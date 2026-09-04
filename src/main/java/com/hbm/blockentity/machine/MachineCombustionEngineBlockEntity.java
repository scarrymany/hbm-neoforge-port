package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.container.machine.MachineCombustionEngineMenu;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.items.machine.ItemPistons;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineCombustionEngine} (block {@code MachineCombustionEngine},
 * regname {@code machine_combustion_engine}, read in full): a multiblock diesel-burning HE engine
 * with a real inventory + GUI. Slot 2 must hold an {@link ItemPistons} (this port's four registered
 * {@code piston_set_*} items replace CE's single item + 4 metadata grades - see
 * {@link ItemPistons.EnumPistonType#eff}); {@link #setting} (0-30, redstone/GUI-controlled) is the
 * throttle. {@code fill}/{@code tenth} preserve CE's tenth-of-a-millibucket burn-rate precision
 * exactly (burning fractional mB/tick at low throttle would otherwise round to zero forever).
 * {@code loadTank(0,1)} / {@code setType(4)} Exact CE {@code :96-99}. 5-slot layout Exact CE
 * {@code ContainerCombustionEngine.java:37-41}.
 * {@code pollute(BURN, toBurn*0.5F)} every 5t Exact CE {@code :126-127}.
 * Smoke overflow {@code incrementPollution} Exact CE {@code TileEntityMachinePolluting:53-76}.
 * No OpenComputers. ROR: CE {@code TileEntityMachineCombustionEngine.java:538-584}. Piston is slot 2
 * / {@link ItemPistons} instance (CE slot 2 / {@code piston_set} meta). Audio loop stay skipped.
 */
public class MachineCombustionEngineBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider,
        IRORValueProvider, IRORInteractive {

    public static final long MAX_POWER = 2_500_000L;
    private static final int SLOT_CANISTER = 0;
    private static final int SLOT_EMPTY = 1;
    private static final int SLOT_PISTON = 2;
    private static final int SLOT_BATTERY = 3;
    private static final int SLOT_ID = 4;

    public final FluidTankNTM tank;
    /** CE {@code TileEntityMachinePolluting} buffer 50 from {@code super(5, 50)}. */
    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;
    public boolean isOn;
    public int setting;
    private long power;
    private int tenth;

    public MachineCombustionEngineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 5, true, true);
        tank = new FluidTankNTM(Fluids.DIESEL, 24_000).withOwner(this);
        this.smoke = new FluidTankNTM(Fluids.SMOKE, 50).withOwner(this);
        this.smokeLeaded = new FluidTankNTM(Fluids.SMOKE_LEADED, 50).withOwner(this);
        this.smokePoison = new FluidTankNTM(Fluids.SMOKE_POISON, 50).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.combustionEngine");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == SLOT_CANISTER) {
            if (FluidContainerRegistry.getFluidContent(stack, tank.getTankType()) > 0) return true;
            return stack.getItem() instanceof IFillableItem fill && fill.providesFluid(tank.getTankType(), stack);
        }
        if (i == SLOT_PISTON) return stack.getItem() instanceof ItemPistons;
        if (i == SLOT_BATTERY) return Library.isChargeableBattery(stack);
        // CE has no isItemValid; without this the ID never lands and setType is dead.
        if (i == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        return false;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        if (slot == SLOT_EMPTY) return true;
        if (slot == SLOT_BATTERY && stack.getItem() instanceof IBatteryItem bat) {
            return bat.getCharge(stack) == bat.getMaxCharge(stack);
        }
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        if (side == Direction.DOWN) return new int[]{SLOT_EMPTY, SLOT_BATTERY};
        if (side == Direction.UP) return new int[]{SLOT_CANISTER};
        return new int[]{SLOT_PISTON, SLOT_BATTERY};
    }

    private Direction coreDirection() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockDummyable.META)
                ? Direction.from3DDataValue(state.getValue(BlockDummyable.META) - BlockDummyable.offset)
                : Direction.NORTH;
    }

    public DirPos[] getConPos() {
        Direction dir = coreDirection();
        Direction rot = dir.getClockWise(Direction.Axis.Y);
        int x = worldPosition.getX();
        int y = worldPosition.getY();
        int z = worldPosition.getZ();

        return new DirPos[]{
                new DirPos(x + dir.getStepX() + rot.getStepX(), y, z + dir.getStepZ() + rot.getStepZ(), dir),
                new DirPos(x + dir.getStepX() - rot.getStepX(), y, z + dir.getStepZ() - rot.getStepZ(), dir),
                new DirPos(x - dir.getStepX() * 2 + rot.getStepX(), y, z - dir.getStepZ() * 2 + rot.getStepZ(), dir.getOpposite()),
                new DirPos(x - dir.getStepX() * 2 - rot.getStepX(), y, z - dir.getStepZ() * 2 - rot.getStepZ(), dir.getOpposite())
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityMachineCombustionEngine.java:96-99
        this.tank.loadTank(SLOT_CANISTER, SLOT_EMPTY, inventory);
        if (this.tank.setType(SLOT_ID, inventory)) {
            this.tenth = 0;
        }

        int fill = tank.getFill() * 10 + tenth;
        ItemStack pistonStack = inventory.getStackInSlot(SLOT_PISTON);

        boolean wasOn = false;
        if (isOn && setting > 0 && pistonStack.getItem() instanceof ItemPistons piston
                && fill > 0 && tank.getTankType().hasTrait(FT_Combustible.class)) {
            FT_Combustible trait = tank.getTankType().getTrait(FT_Combustible.class);
            double eff = piston.getType().eff[trait.getGrade().ordinal()];

            if (eff > 0) {
                int speed = setting * 2;
                int toBurn = Math.min(fill, speed);
                power += (long) (toBurn * (trait.getCombustionEnergy() / 10_000D) * eff);
                fill -= toBurn;
                // CE TileEntityMachineCombustionEngine.java:126-127
                if (level.getGameTime() % 5 == 0 && toBurn > 0) {
                    pollute(tank.getTankType(), FluidTrait.FluidReleaseType.BURN, toBurn * 0.5F);
                }
                wasOn = toBurn > 0;

                tank.setFill(fill / 10);
                tenth = fill % 10;
            }
        }

        power = Library.chargeItemsFromTE(inventory, SLOT_BATTERY, power, MAX_POWER);

        for (DirPos dirPos : getConPos()) {
            BlockPos p = dirPos.getPos();
            this.tryProvide(level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
            this.trySubscribe(tank.getTankType(), level, p.getX(), p.getY(), p.getZ(), dirPos.getDir());
            // CE TileEntityMachineCombustionEngine.java:158-162
            sendSmoke(dirPos);
        }

        if (power > MAX_POWER) power = MAX_POWER;

        // CE: TileEntityMachineDiesel.getLoopedSound() - continuous AudioWrapper loop
        // (HBMSoundHandler.engine, 10-tick keepAlive) while burning fuel. No looped-block-audio
        // bridge ported yet (see ChemPlantBlockEntity's identical note); substituted with a periodic
        // broadcast every 10 ticks while actively combusting.
        if (wasOn && level.getGameTime() % 10 == 0) {
            level.playSound(null, worldPosition, HBMSoundHandler.engine.get(), SoundSource.BLOCKS, 1F, 1.0F);
        }

        if (wasOn) dataChanged();
        networkPackMK2(50);
    }

    /** CE {@code TileEntityMachinePolluting#sendSmoke}. */
    private void sendSmoke(DirPos pos) {
        if (smoke.getFill() > 0) tryProvide(smoke, level, pos);
        if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, pos);
        if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, pos);
    }

    /**
     * Exact CE {@code TileEntityMachinePolluting#pollute(FluidType, FluidReleaseType, float)}
     * {@code :53-76}. Fire-extinguish sound stay skipped.
     */
    public void pollute(FluidType type, FluidTrait.FluidReleaseType release, float amount) {
        FT_Polluting trait = type.getTrait(FT_Polluting.class);
        if (trait == null) return;
        if (release == FluidTrait.FluidReleaseType.VOID) return;

        HashMap<PollutionHandler.PollutionType, Float> map = release == FluidTrait.FluidReleaseType.BURN
                ? trait.burnMap : trait.releaseMap;

        for (Map.Entry<PollutionHandler.PollutionType, Float> entry : map.entrySet()) {
            FluidTankNTM dest = entry.getKey() == PollutionHandler.PollutionType.SOOT ? smoke
                    : entry.getKey() == PollutionHandler.PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;
            int fluidAmount = (int) Math.ceil(entry.getValue() * amount * 100);
            dest.setFill(dest.getFill() + fluidAmount);
            if (dest.getFill() > dest.getMaxFill()) {
                int overflow = dest.getFill() - dest.getMaxFill();
                dest.setFill(dest.getMaxFill());
                PollutionHandler.incrementPollution(level, worldPosition, entry.getKey(), overflow / 100F);
            }
        }
    }

    public void setOn(boolean on) {
        this.isOn = on;
        dataChanged();
        setChanged();
    }

    public void setThrottle(int value) {
        this.setting = Math.max(0, Math.min(30, value));
        dataChanged();
        setChanged();
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
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        // CE TileEntityMachineCombustionEngine.java:354-355 getSmokeTanks
        return List.of(smoke, smokeLeaded, smokePoison);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        // CE TileEntityMachineCombustionEngine.java:344-345 — fuel tank only
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("setting", setting);
        tag.putLong("power", power);
        tag.putBoolean("isOn", isOn);
        tag.putInt("tenth", tenth);
        tank.writeToNBT(tag, "tank");
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setting = tag.getInt("setting");
        power = tag.getLong("power");
        isOn = tag.getBoolean("isOn");
        tenth = tag.getInt("tenth");
        tank.readFromNBT(tag, "tank");
        smoke.readFromNBT(tag, "smoke0");
        smokeLeaded.readFromNBT(tag, "smoke1");
        smokePoison.readFromNBT(tag, "smoke2");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeInt(setting);
        buf.writeLong(power);
        buf.writeBoolean(isOn);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        setting = buf.readInt();
        power = buf.readLong();
        isOn = buf.readBoolean();
        tank.deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineCombustionEngineMenu(containerId, playerInventory, this);
    }

    @Override
    public String[] getFunctionInfo() {
        // CE :538-547
        return new String[]{
                PREFIX_VALUE + "state",
                PREFIX_VALUE + "throttle",
                PREFIX_VALUE + "power",
                PREFIX_VALUE + "fuel",
                PREFIX_VALUE + "efficiency",
                PREFIX_FUNCTION + "setstate" + NAME_SEPARATOR + "state",
                PREFIX_FUNCTION + "setthrottle" + NAME_SEPARATOR + "throttle"
        };
    }

    @Override
    public String provideRORValue(String name) {
        // CE :551-568 — piston slot 2 / ItemPistons instance (CE slot 2 / piston_set meta)
        if ((PREFIX_VALUE + "state").equals(name)) return "" + (isOn ? 1 : 0);
        if ((PREFIX_VALUE + "throttle").equals(name)) return "" + setting;
        if ((PREFIX_VALUE + "power").equals(name)) return "" + power;
        if ((PREFIX_VALUE + "fuel").equals(name)) return "" + tank.getFill();
        if ((PREFIX_VALUE + "efficiency").equals(name)) {
            ItemStack stack = inventory.getStackInSlot(SLOT_PISTON);
            if (!stack.isEmpty()
                    && stack.getItem() instanceof ItemPistons piston
                    && tank.getTankType().hasTrait(FT_Combustible.class)) {
                FT_Combustible trait = tank.getTankType().getTrait(FT_Combustible.class);
                return "" + (int) Math.round(piston.getType().eff[trait.getGrade().ordinal()] * 100);
            }
            return "0";
        }
        return null;
    }

    @Override
    public String runRORFunction(String name, String[] params) {
        // CE :572-583
        if ((PREFIX_FUNCTION + "setstate").equals(name) && params.length > 0) {
            this.isOn = IRORInteractive.parseInt(params[0], 0, 1) == 1;
            setChanged();
            return null;
        }
        if ((PREFIX_FUNCTION + "setthrottle").equals(name) && params.length > 0) {
            this.setting = IRORInteractive.parseInt(params[0], 0, 30);
            setChanged();
            return null;
        }
        return null;
    }
}
