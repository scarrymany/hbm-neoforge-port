package com.hbm.blockentity.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFillableItem;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import com.hbm.inventory.container.machine.MachineDieselMenu;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.hbm.lib.Library;
import com.hbm.tileentity.IConfigurableMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineDiesel} (block {@code MachineDiesel}, regname
 * {@code machine_diesel}, read in full): a small standalone diesel generator, single block (not
 * dummyable). Per-tick HE yield is {@code FT_Combustible.getCombustionEnergy()/1000 *
 * fuelEfficiency[grade]}, burning exactly 1 mB/tick while {@link #isOn} and not redstone-powered -
 * CE's own static {@code fuelEfficiency} table is reproduced unchanged below.
 * {@code setType(3)} / {@code loadTank(0,1)} Exact CE {@code TileEntityMachineDiesel.java:120-121}.
 * 4-slot layout Exact CE {@code ContainerMachineDiesel.java:38-41}.
 * {@link IConfigurableMachine} Exact CE {@code TileEntityMachineDiesel.java:282-315}
 * ({@code dieselgen}).
 * {@code pollute(BURN, 5F)} every 5t while generating Exact CE {@code :233-234}.
 * Smoke overflow {@code incrementPollution} Exact CE {@code TileEntityMachinePolluting:53-76}.
 * On/off {@code IControlReceiver} Exact CE {@code :323-324}. Audio stay skipped.
 */
public class MachineDieselBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider, IConfigurableMachine, IControlReceiver {

    public static int fuelCap = 16_000;
    public static long maxPower = 50_000L;
    private static final int SLOT_CANISTER = 0;
    private static final int SLOT_EMPTY = 1;
    private static final int SLOT_BATTERY = 2;
    private static final int SLOT_ID = 3;

    public static HashMap<FT_Combustible.FuelGrade, Double> fuelEfficiency = new HashMap<>();

    static {
        fuelEfficiency.put(FT_Combustible.FuelGrade.MEDIUM, 0.5D);
        fuelEfficiency.put(FT_Combustible.FuelGrade.HIGH, 0.75D);
        fuelEfficiency.put(FT_Combustible.FuelGrade.AERO, 0.1D);
    }

    public final FluidTankNTM tank;
    /** CE {@code TileEntityMachinePolluting} buffer 100 from {@code super(4, 100)}. */
    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;
    public boolean isOn;
    public boolean wasOn;
    private long power;

    public MachineDieselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4, true, true);
        tank = new FluidTankNTM(Fluids.DIESEL, fuelCap).withOwner(this);
        this.smoke = new FluidTankNTM(Fluids.SMOKE, 100).withOwner(this);
        this.smokeLeaded = new FluidTankNTM(Fluids.SMOKE_LEADED, 100).withOwner(this);
        this.smokePoison = new FluidTankNTM(Fluids.SMOKE_POISON, 100).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineDiesel");
    }

    public static long getHEFromFuel(FluidType type) {
        if (!type.hasTrait(FT_Combustible.class)) return 0;
        FT_Combustible fuel = type.getTrait(FT_Combustible.class);
        if (fuel.getGrade() == FT_Combustible.FuelGrade.LOW) return 0;
        double efficiency = fuelEfficiency.getOrDefault(fuel.getGrade(), 0D);
        return (long) (fuel.getCombustionEnergy() / 1000D * efficiency);
    }

    public boolean hasAcceptableFuel() {
        return getHEFromFuel(tank.getTankType()) > 0;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        // CE TileEntityMachineDiesel.java:120-121
        tank.setType(SLOT_ID, inventory);
        tank.loadTank(SLOT_CANISTER, SLOT_EMPTY, inventory);

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            this.tryProvide(level, target.getX(), target.getY(), target.getZ(), dir);
            // CE TileEntityMachineDiesel.java:125
            sendSmoke(target, dir);
            this.trySubscribe(tank.getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
        }

        power = Library.chargeItemsFromTE(inventory, SLOT_BATTERY, power, maxPower);

        // CE TileEntityMachineDiesel.java:119 / :222-228
        this.wasOn = false;
        if (isOn && !level.hasNeighborSignal(worldPosition) && hasAcceptableFuel() && tank.getFill() > 0) {
            this.wasOn = true;
            tank.setFill(Math.max(0, tank.getFill() - 1));
            // CE TileEntityMachineDiesel.java:233-234
            if (level.getGameTime() % 5 == 0) {
                pollute(tank.getTankType(), FluidTrait.FluidReleaseType.BURN, 5F);
            }
            power = Math.min(maxPower, power + getHEFromFuel(tank.getTankType()));
        }

        dataChanged();
        networkPackMK2(50);
    }

    /** CE {@code TileEntityMachinePolluting#sendSmoke}. */
    private void sendSmoke(BlockPos pos, Direction dir) {
        if (smoke.getFill() > 0) tryProvide(smoke, level, pos, dir);
        if (smokeLeaded.getFill() > 0) tryProvide(smokeLeaded, level, pos, dir);
        if (smokePoison.getFill() > 0) tryProvide(smokePoison, level, pos, dir);
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

    @Override
    public boolean hasPermission(Player player) {
        return isUseableByPlayer(player);
    }

    /** Exact CE {@code TileEntityMachineDiesel.receiveControl} :323-324. */
    @Override
    public void receiveControl(CompoundTag data) {
        if (data.contains("turnOn")) this.isOn = !this.isOn;
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        if (i == SLOT_CANISTER) {
            if (FluidContainerRegistry.getFluidContent(stack, tank.getTankType()) > 0) return true;
            // Port ItemCanister is IFillableItem, not in FluidContainerRegistry (CE metadata canisters).
            return stack.getItem() instanceof IFillableItem fill && fill.providesFluid(tank.getTankType(), stack);
        }
        if (i == SLOT_BATTERY) return Library.isChargeableBattery(stack);
        // CE :245-248 returns false for slot 3; without this the ID never lands and setType is dead.
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
        return new int[]{SLOT_BATTERY};
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
        return maxPower;
    }

    @Override
    public String getConfigName() {
        return "dieselgen";
    }

    @Override
    public void readIfPresent(JsonObject obj) {
        readConfig(obj);
    }

    @Override
    public void writeConfig(JsonWriter writer) throws IOException {
        writeConfigStatic(writer);
    }

    static void readConfig(JsonObject obj) {
        // CE TileEntityMachineDiesel.java:288-296
        maxPower = IConfigurableMachine.grab(obj, "L:powerCap", maxPower);
        fuelCap = IConfigurableMachine.grab(obj, "I:fuelCap", fuelCap);

        if (obj.has("D[:efficiency")) {
            JsonArray array = obj.get("D[:efficiency").getAsJsonArray();
            for (FT_Combustible.FuelGrade grade : FT_Combustible.FuelGrade.VALUES) {
                fuelEfficiency.put(grade, array.get(grade.ordinal()).getAsDouble());
            }
        }
    }

    static void writeConfigStatic(JsonWriter writer) throws IOException {
        // CE TileEntityMachineDiesel.java:301-314
        writer.name("L:powerCap").value(maxPower);
        writer.name("I:fuelCap").value(fuelCap);

        String info = "Fuel grades in order: ";
        for (FT_Combustible.FuelGrade grade : FT_Combustible.FuelGrade.VALUES) info += grade.name() + " ";
        info = info.trim();
        writer.name("INFO").value(info);

        writer.name("D[:efficiency").beginArray().setIndent("");
        for (FT_Combustible.FuelGrade grade : FT_Combustible.FuelGrade.VALUES) {
            double d = fuelEfficiency.getOrDefault(grade, 0.0D);
            writer.value(d);
        }
        writer.endArray().setIndent("  ");
    }

    /** NeoForge BE has no no-arg ctor. MachineDynConfig Exact CE :44-48. */
    public static final class ConfigDummy implements IConfigurableMachine {
        @Override
        public String getConfigName() {
            return "dieselgen";
        }

        @Override
        public void readIfPresent(JsonObject obj) {
            readConfig(obj);
        }

        @Override
        public void writeConfig(JsonWriter writer) throws IOException {
            writeConfigStatic(writer);
        }
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tank);
    }

    @Override
    public @NotNull List<FluidTankNTM> getSendingTanks() {
        // CE TileEntityMachineDiesel.java:267-269 getSmokeTanks
        return List.of(smoke, smokeLeaded, smokePoison);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        // CE TileEntityMachineDiesel.java:277-278 — fuel tank only
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putLong("powerTime", power);
        tank.writeToNBT(tag, "tank");
        smoke.writeToNBT(tag, "smoke0");
        smokeLeaded.writeToNBT(tag, "smoke1");
        smokePoison.writeToNBT(tag, "smoke2");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isOn = tag.getBoolean("isOn");
        power = tag.getLong("powerTime");
        tank.readFromNBT(tag, "tank");
        smoke.readFromNBT(tag, "smoke0");
        smokeLeaded.readFromNBT(tag, "smoke1");
        smokePoison.readFromNBT(tag, "smoke2");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeBoolean(wasOn);
        buf.writeLong(power);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
        wasOn = buf.readBoolean();
        power = buf.readLong();
        tank.deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineDieselMenu(containerId, playerInventory, this);
    }
}
