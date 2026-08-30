package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.container.machine.MachineDieselMenu;
import com.hbm.lib.Library;
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
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code TileEntityMachineDiesel} (block {@code MachineDiesel}, regname
 * {@code machine_diesel}, read in full): a small standalone diesel generator, single block (not
 * dummyable). Per-tick HE yield is {@code FT_Combustible.getCombustionEnergy()/1000 *
 * fuelEfficiency[grade]}, burning exactly 1 mB/tick while {@link #isOn} and not redstone-powered -
 * CE's own static {@code fuelEfficiency} table is reproduced unchanged below.
 * <p>
 * <b>Scope trim vs. CE</b> (documented, not silent): CE's slot 0 fills the tank from a held fluid
 * container item via {@code FluidContainerRegistry}/{@code tank.loadTank} - that registry class is
 * referenced by several already-shipped Phase 0/1 capability files
 * ({@code NTMFluidCapabilityHandler}, {@code NTMFluidContainerWrapper}, {@code ItemCanister},
 * {@code ItemFluidTank}/{@code V2}) but does not exist anywhere in this port (confirmed by search) -
 * a real, pre-existing compile-blocking gap, not one this pass introduces. Building it is a
 * cross-cutting item-fluid-container project outside this power-generation pass's scope (see
 * {@code docs/phase2/machines_power_generation.md}'s own deferred-scope framing for the analogous
 * pollution gap). This class therefore has no item-fill slot at all: fuel arrives purely over the
 * fluid network ({@link IFluidStandardReceiverMK2}, exactly like every other tank-only NTM machine's
 * pipe input) - CE's slot 1 (empty-container output) is dropped along with it. The battery-charging
 * slot (CE's slot 2) is kept, using the storage-machines package's now-shipped
 * {@link Library#chargeItemsFromTE}/{@link Library#isBattery} (CE's own {@code Library} helpers,
 * ported by that concurrent pass).
 */
public class MachineDieselBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardReceiverMK2, ITickableBE, MenuProvider {

    public static final int FUEL_CAP = 16_000;
    public static final long MAX_POWER = 50_000L;
    private static final int BATTERY_SLOT = 0;

    private static final Map<FT_Combustible.FuelGrade, Double> FUEL_EFFICIENCY = new EnumMap<>(FT_Combustible.FuelGrade.class);

    static {
        FUEL_EFFICIENCY.put(FT_Combustible.FuelGrade.MEDIUM, 0.5D);
        FUEL_EFFICIENCY.put(FT_Combustible.FuelGrade.HIGH, 0.75D);
        FUEL_EFFICIENCY.put(FT_Combustible.FuelGrade.AERO, 0.1D);
    }

    public final FluidTankNTM tank;
    public boolean isOn;
    private long power;

    public MachineDieselBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1, true, true);
        tank = new FluidTankNTM(Fluids.DIESEL, FUEL_CAP).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineDiesel");
    }

    public static long getHEFromFuel(FluidType type) {
        if (!type.hasTrait(FT_Combustible.class)) return 0;
        FT_Combustible fuel = type.getTrait(FT_Combustible.class);
        if (fuel.getGrade() == FT_Combustible.FuelGrade.LOW) return 0;
        double efficiency = FUEL_EFFICIENCY.getOrDefault(fuel.getGrade(), 0D);
        return (long) (fuel.getCombustionEnergy() / 1000D * efficiency);
    }

    public boolean hasAcceptableFuel() {
        return getHEFromFuel(tank.getTankType()) > 0;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            this.tryProvide(level, target.getX(), target.getY(), target.getZ(), dir);
            this.trySubscribe(tank.getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
        }

        power = Library.chargeItemsFromTE(inventory, BATTERY_SLOT, power, MAX_POWER);

        if (isOn && !level.hasNeighborSignal(worldPosition) && hasAcceptableFuel() && tank.getFill() > 0) {
            tank.setFill(Math.max(0, tank.getFill() - 1));
            power = Math.min(MAX_POWER, power + getHEFromFuel(tank.getTankType()));
        }

        dataChanged();
        networkPackMK2(50);
    }

    public void setOn(boolean on) {
        this.isOn = on;
        dataChanged();
        setChanged();
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        return i == BATTERY_SLOT && Library.isBattery(stack);
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
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tank);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isOn", isOn);
        tag.putLong("powerTime", power);
        tank.writeToNBT(tag, "tank");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isOn = tag.getBoolean("isOn");
        power = tag.getLong("powerTime");
        tank.readFromNBT(tag, "tank");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeBoolean(isOn);
        buf.writeLong(power);
        tank.serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        isOn = buf.readBoolean();
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
