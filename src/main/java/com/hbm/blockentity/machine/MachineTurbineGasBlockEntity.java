package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.machine.MachineTurbineGasMenu;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.DirPos;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.lib.Library;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineTurbineGas} (block {@code MachineTurbineGas}, regname
 * {@code machine_turbine_gas}, read in full): the real "gas generator" - a self-contained
 * gas-combustion steam turbine in one multiblock. Burns {@code tanks[0]} (a {@link FT_Combustible}
 * GAS-grade fluid, per-fluid max-consumption rate in {@link #FUEL_MAX_CONS}) to boil
 * {@code tanks[2]} (water) into {@code tanks[3]} (hot steam) while separately producing HE directly
 * off the fuel via an RPM/temperature/throttle state machine ({@link #startup}/{@link #run}/
 * {@link #shutdown}, CE's own numeric tuning constants reproduced unchanged).
 * Slot 1 fluid-ID is Exact CE {@code TileEntityMachineTurbineGas.java:109-114}: manual
 * {@link IItemFluidIdentifier#getType} then {@code tanks[0].setTankType} only when the fluid is
 * {@link FT_Combustible} {@code FuelGrade.GAS}. Not {@code setType} — CE does not call it here.
 * Slot 1 @ 36,17 Exact CE {@code ContainerMachineTurbineGas.java:28}.
 * <p>
 * {@code incrementPollution(SOOT, SOOT_PER_SECOND*3)} Exact CE {@code :352}
 * (skip OXYHYDROGEN). No OpenComputers. ROR: CE {@code TileEntityMachineTurbineGas.java:716-783}.
 * {@code gui_turbinegas.png} is not in this tree — do not invent it.
 */
public class MachineTurbineGasBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IRORValueProvider, IRORInteractive {

    public static final long MAX_POWER = 1_000_000L;
    public static final int BATTERY_SLOT = 0;
    public static final int SLOT_ID = 1;
    private static final int RPM_IDLE = 10;
    private static final int TEMP_IDLE = 300;

    private static final Map<FluidType, Double> FUEL_MAX_CONS = new HashMap<>();

    static {
        FUEL_MAX_CONS.put(Fluids.GAS, 50D);
        FUEL_MAX_CONS.put(Fluids.SYNGAS, 10D);
        FUEL_MAX_CONS.put(Fluids.OXYHYDROGEN, 100D);
        FUEL_MAX_CONS.put(Fluids.REFORMGAS, 5D);
    }

    public final FluidTankNTM[] tanks;
    public long power;
    public int rpm;
    public int temp = 20;
    public int powerSliderPos;
    public int throttle;
    public boolean autoMode;
    /** 0 = offline, -1 = startup, 1 = online. */
    public int state;
    public int counter;
    public int instantPowerOutput;

    private int rpmLast;
    private int tempLast;
    private double fuelToConsume;

    public MachineTurbineGasBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, true);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.GAS, 100_000).withOwner(this),
                new FluidTankNTM(Fluids.LUBRICANT, 16_000).withOwner(this),
                new FluidTankNTM(Fluids.WATER, 16_000).withOwner(this),
                new FluidTankNTM(Fluids.HOTSTEAM, 160_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.turbinegas");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == BATTERY_SLOT) return Library.isBattery(stack);
        if (i == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        return false;
    }

    private Direction coreDirection() {
        BlockState st = getBlockState();
        return st.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(st.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    public boolean hasAcceptableFuel() {
        return tanks[0].getTankType().hasTrait(FT_Combustible.class)
                && tanks[0].getTankType().getTrait(FT_Combustible.class).getGrade() == FT_Combustible.FuelGrade.GAS;
    }

    private void stopIfNotReady() {
        if (tanks[0].getFill() == 0 || tanks[1].getFill() == 0 || !hasAcceptableFuel()) {
            state = 0;
        }
    }

    private void startup() {
        counter++;

        if (counter <= 20) rpm = 5 * counter;
        else if (counter <= 40) rpm = 100 - 5 * (counter - 20);
        else if (counter > 50) {
            rpm = RPM_IDLE * (counter - 50) / 530;
            temp = TEMP_IDLE * (counter - 50) / 530;
        }

        // CE: TileEntityMachineTurbineGas.startup() - one-shot ignition sound at the exact tick the
        // rpm gauge's 0-100-0 sweep finishes and idle ramp-up begins.
        if (counter == 50) {
            level.playSound(null, worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ(),
                    HBMSoundHandler.turbinegasStartup.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        if (counter >= 580) {
            counter = 225;
            state = 1;
        }
    }

    private void shutdown() {
        autoMode = false;
        instantPowerOutput = 0;
        if (powerSliderPos > 0) powerSliderPos--;

        if (rpm <= 10 && counter > 0) {
            if (counter == 225) {
                // CE: TileEntityMachineTurbineGas.shutdown() - one-shot spin-down sound the instant
                // the cooldown ramp begins.
                level.playSound(null, worldPosition.getX(), worldPosition.getY() + 2, worldPosition.getZ(),
                        HBMSoundHandler.turbinegasShutdown.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                rpmLast = rpm;
                tempLast = temp;
            }
            counter--;
            rpm = rpmLast * counter / 225;
            temp = tempLast * counter / 225;
        } else if (rpm > 11) {
            counter = 42069;
            rpm--;
        } else if (rpm == 11) {
            counter = 225;
            rpm--;
        }
    }

    /** Dynamically scales a (hopefully) sensible burn heat from the fuel's combustion energy, 300C-800C. */
    private static int fluidBurnTemp(FluidType type) {
        double energy = type.hasTrait(FT_Combustible.class) ? type.getTrait(FT_Combustible.class).getCombustionEnergy() : 0;
        return (int) Math.floor(800D - Math.pow(Math.E, -energy / 100_000D) * 300D);
    }

    private void run() {
        if ((int) (throttle * 0.9) > rpm - RPM_IDLE) {
            if (level.getGameTime() % 5 == 0) rpm++;
        } else if ((int) (throttle * 0.9) < rpm - RPM_IDLE) {
            if (level.getGameTime() % 2 == 0) rpm--;
        }

        int maxTemp = fluidBurnTemp(tanks[0].getTankType());
        int tempTarget = throttle * 5 * (maxTemp - TEMP_IDLE) / 500;
        if (tempTarget > temp - TEMP_IDLE) {
            if (level.getGameTime() % 2 == 0) temp++;
        } else if (tempTarget < temp - TEMP_IDLE) {
            if (level.getGameTime() % 2 == 0) temp--;
        }

        double consMax = FUEL_MAX_CONS.getOrDefault(tanks[0].getTankType(), 5D);
        // CE TileEntityMachineTurbineGas.java:352
        if (level.getGameTime() % 20 == 0 && tanks[0].getTankType() != Fluids.OXYHYDROGEN) {
            PollutionHandler.incrementPollution(level, worldPosition, PollutionHandler.PollutionType.SOOT,
                    PollutionHandler.SOOT_PER_SECOND * 3);
        }
        makePower(consMax);
    }

    private void makePower(double consMax) {
        double consumption = consMax * 0.05D + consMax * throttle / 100D;
        fuelToConsume += consumption;

        int wholeFuel = (int) Math.floor(fuelToConsume);
        tanks[0].setFill(tanks[0].getFill() - wholeFuel);
        fuelToConsume -= wholeFuel;

        if (level.getGameTime() % 10 == 0) tanks[1].setFill(tanks[1].getFill() - 1);

        if (tanks[0].getFill() < 0) {
            tanks[0].setFill(0);
            state = 0;
        }
        if (tanks[1].getFill() < 0) {
            tanks[1].setFill(0);
            state = 0;
        }

        long energy = tanks[0].getTankType().hasTrait(FT_Combustible.class)
                ? tanks[0].getTankType().getTrait(FT_Combustible.class).getCombustionEnergy() / 1000L : 0;
        int rpmEff = rpm - RPM_IDLE;
        double target = consMax * energy * rpmEff / 90D;

        if (instantPowerOutput < target) {
            instantPowerOutput += Math.random() * 0.005 * consMax * energy;
            if (instantPowerOutput > target) instantPowerOutput = (int) target;
        } else if (instantPowerOutput > target) {
            instantPowerOutput -= Math.random() * 0.011 * consMax * energy;
            if (instantPowerOutput < target) instantPowerOutput = (int) target;
        }
        power += instantPowerOutput;

        double waterToBoil = consMax * energy * (temp - TEMP_IDLE) / 220_000D;
        int heatCycles = (int) Math.floor(waterToBoil);
        int waterCycles = tanks[2].getFill();
        int steamCycles = (tanks[3].getMaxFill() - tanks[3].getFill()) / 10;
        int cycles = Math.min(heatCycles, Math.min(waterCycles, steamCycles));

        tanks[2].setFill(tanks[2].getFill() - cycles);
        tanks[3].setFill(tanks[3].getFill() + cycles * 10);
    }

    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x - dir.getStepZ() * 5, y + 1, z + dir.getStepX() * 5, rot),
                new DirPos(x - dir.getStepX() * 2 + rot.getStepX(), y, z - dir.getStepZ() * 2 + rot.getStepZ(), dir.getOpposite()),
                new DirPos(x + dir.getStepX() * 2 + rot.getStepX(), y, z + dir.getStepZ() * 2 + rot.getStepZ(), dir),
                new DirPos(x - dir.getStepX() * 2 - rot.getStepX() * 4, y, z - dir.getStepZ() * 2 - rot.getStepZ() * 4, dir.getOpposite()),
                new DirPos(x + dir.getStepX() * 2 - rot.getStepX() * 4, y, z + dir.getStepZ() * 2 - rot.getStepZ() * 4, dir),
                new DirPos(x + dir.getStepZ() * 6, y + 1, z - dir.getStepX() * 6, rot.getOpposite())
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        throttle = powerSliderPos * 100 / 60;

        // CE TileEntityMachineTurbineGas.java:109-114 — GAS-grade identifier only, not setType.
        ItemStack idStack = inventory.getStackInSlot(SLOT_ID);
        if (!idStack.isEmpty() && idStack.getItem() instanceof IItemFluidIdentifier identifier) {
            FluidType fluid = identifier.getType(level, worldPosition, idStack);
            if (fluid.hasTrait(FT_Combustible.class)
                    && fluid.getTrait(FT_Combustible.class).getGrade() == FT_Combustible.FuelGrade.GAS) {
                tanks[0].setTankType(fluid);
            }
        }

        if (autoMode) {
            int target = 60 - (int) (60 * power / MAX_POWER);
            if (target > powerSliderPos) powerSliderPos++;
            else if (target < powerSliderPos) powerSliderPos--;
        }

        switch (state) {
            case 0 -> shutdown();
            case -1 -> {
                stopIfNotReady();
                startup();
            }
            case 1 -> {
                stopIfNotReady();
                run();
            }
            default -> {
            }
        }

        // CE: TileEntityMachineTurbineGas's client-side branch - continuous AudioWrapper loop
        // (HBMSoundHandler.turbinegasRunning, 20-tick keepAlive, pitch ramped by rpm) while
        // rpm >= 10 and not mid-startup. No looped-block-audio bridge ported yet (see
        // ChemPlantBlockEntity's identical note); substituted with a periodic broadcast every 20
        // ticks, pitch approximating CE's 0.55 + 0.1 * rpm/10 ramp.
        if (rpm >= 10 && state != -1 && level.getGameTime() % 20 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.turbinegasRunning.get(), SoundSource.BLOCKS, 2F, (float) (0.55D + 0.1D * rpm / 10D));
        }

        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);

        power = Library.chargeItemsFromTE(inventory, BATTERY_SLOT, power, MAX_POWER);
        BlockPos powerTarget = worldPosition.offset(-rot.getStepZ() * 5, 1, rot.getStepX() * 5);
        this.tryProvide(level, powerTarget.getX(), powerTarget.getY(), powerTarget.getZ(), rot);
        if (power > MAX_POWER) power = MAX_POWER;

        for (int i = 0; i < 2; i++) {
            BlockPos a = worldPosition.offset(-dir.getStepX() * 2 + rot.getStepX(), 0, -dir.getStepZ() * 2 + rot.getStepZ());
            BlockPos b = worldPosition.offset(dir.getStepX() * 2 + rot.getStepX(), 0, dir.getStepZ() * 2 + rot.getStepZ());
            this.trySubscribe(tanks[i].getTankType(), level, a.getX(), a.getY(), a.getZ(), dir.getOpposite());
            this.trySubscribe(tanks[i].getTankType(), level, b.getX(), b.getY(), b.getZ(), dir);
        }
        BlockPos waterA = worldPosition.offset(-dir.getStepX() * 2 - rot.getStepX() * 4, 0, -dir.getStepZ() * 2 - rot.getStepZ() * 4);
        BlockPos waterB = worldPosition.offset(dir.getStepX() * 2 - rot.getStepX() * 4, 0, dir.getStepZ() * 2 - rot.getStepZ() * 4);
        this.trySubscribe(tanks[2].getTankType(), level, waterA.getX(), waterA.getY(), waterA.getZ(), dir.getOpposite());
        this.trySubscribe(tanks[2].getTankType(), level, waterB.getX(), waterB.getY(), waterB.getZ(), dir);

        BlockPos steamOut = worldPosition.offset(dir.getStepZ() * 6, 1, -dir.getStepX() * 6);
        this.tryProvide(tanks[3], level, steamOut, rot.getOpposite());

        dataChanged();
        networkPackMK2(150);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long i) {
        power = i;
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0], tanks[1], tanks[2]);
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[3]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    public void setSliderPos(int pos) {
        this.powerSliderPos = Math.max(0, Math.min(60, pos));
        dataChanged();
        setChanged();
    }

    public void setAutoMode(boolean auto) {
        this.autoMode = auto;
        dataChanged();
        setChanged();
    }

    public void setRunning(boolean running) {
        if (running && state == 0) state = -1;
        if (!running && state == 1) state = 0;
        dataChanged();
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tanks[0].writeToNBT(tag, "gas");
        tanks[1].writeToNBT(tag, "lube");
        tanks[2].writeToNBT(tag, "water");
        tanks[3].writeToNBT(tag, "densesteam");
        tag.putBoolean("automode", autoMode);
        tag.putLong("power", power);
        tag.putInt("state", state);
        tag.putInt("rpm", rpm);
        tag.putInt("temperature", temp);
        tag.putInt("slidPos", powerSliderPos);
        tag.putInt("instPwr", instantPowerOutput);
        tag.putInt("counter", counter);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tanks[0].readFromNBT(tag, "gas");
        tanks[1].readFromNBT(tag, "lube");
        tanks[2].readFromNBT(tag, "water");
        tanks[3].readFromNBT(tag, "densesteam");
        autoMode = tag.getBoolean("automode");
        power = tag.getLong("power");
        state = tag.getInt("state");
        rpm = tag.getInt("rpm");
        temp = tag.getInt("temperature");
        powerSliderPos = tag.getInt("slidPos");
        instantPowerOutput = tag.getInt("instPwr");
        counter = tag.getInt("counter");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        buf.writeInt(rpm);
        buf.writeInt(temp);
        buf.writeInt(state);
        buf.writeBoolean(autoMode);
        buf.writeInt(throttle);
        buf.writeInt(powerSliderPos);
        buf.writeInt(counter);
        buf.writeInt(instantPowerOutput);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
        tanks[2].serialize(buf);
        tanks[3].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        rpm = buf.readInt();
        temp = buf.readInt();
        state = buf.readInt();
        autoMode = buf.readBoolean();
        throttle = buf.readInt();
        powerSliderPos = buf.readInt();
        counter = buf.readInt();
        instantPowerOutput = buf.readInt();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
        tanks[2].deserialize(buf);
        tanks[3].deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineTurbineGasMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :716-732
        return new String[]{
                PREFIX_VALUE + "turbinepercent",
                PREFIX_VALUE + "turbinespeed",
                PREFIX_VALUE + "output",
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "automode",
                PREFIX_VALUE + "temp",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "lubricant",
                PREFIX_VALUE + "water",
                PREFIX_VALUE + "steam",
                PREFIX_FUNCTION + "setauto" + NAME_SEPARATOR + "auto",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "percent",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :736-748
        if ((PREFIX_VALUE + "turbinepercent").equals(name)) return "" + (int) (this.powerSliderPos * 100D / 60D);
        if ((PREFIX_VALUE + "turbinespeed").equals(name)) return "" + this.rpm;
        if ((PREFIX_VALUE + "output").equals(name)) return "" + (this.instantPowerOutput * 20);
        if ((PREFIX_VALUE + "state").equals(name)) return "" + this.state;
        if ((PREFIX_VALUE + "automode").equals(name)) return "" + (this.autoMode ? 1 : 0);
        if ((PREFIX_VALUE + "temp").equals(name)) return "" + this.temp;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + this.power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tanks[0].getFill();
        if ((PREFIX_VALUE + "lubricant").equals(name)) return "" + tanks[1].getFill();
        if ((PREFIX_VALUE + "water").equals(name)) return "" + tanks[2].getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + tanks[3].getFill();
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :752-783
        if ((PREFIX_FUNCTION + "setauto").equals(name) && params.length > 0) {
            try {
                this.autoMode = Integer.parseInt(params[0]) == 1;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            try {
                int percent = Integer.parseInt(params[0]);
                if (percent < 0) percent = 0;
                if (percent > 100) percent = 100;
                this.powerSliderPos = percent * 60 / 100;
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            try {
                int newState = Integer.parseInt(params[0]);
                if (newState == 1) {
                    if (this.state == 0) this.state = -1;
                } else if (newState == 0) {
                    if (this.state == 1) this.state = 0;
                }
                setChanged();
            } catch (NumberFormatException ignored) {
            }
            return null;
        }
        return null;
    }
}
