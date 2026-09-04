package com.hbm.blockentity.machine;

import com.hbm.api.energymk2.IEnergyProviderMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.container.machine.MachineTurbineMenu;
import com.hbm.inventory.fluid.trait.FT_Coolable;
import com.hbm.items.machine.IItemFluidIdentifier;
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

import java.util.List;

/**
 * Ported from CE's {@code TileEntityMachineTurbine} (block {@code MachineTurbine}, regname
 * {@code machine_turbine}, read in full): small standalone single-block turbine, 85% of
 * {@link FT_Coolable}'s TURBINE efficiency, with a 6000-heat-equivalent per-tick operation cap
 * ({@code cap = 6000 / trait.amountReq}) that CE's larger turbines don't have.
 * {@code setType(0,1)} / {@code loadTank(2,3)} / {@code unloadTank(5,6)} Exact CE
 * {@code TileEntityMachineTurbine.java:129-155}. 7-slot layout Exact CE
 * {@code ContainerMachineTurbine.java:36-45}.
 */
public class MachineTurbineBlockEntity extends MachineBaseBlockEntity
        implements IEnergyProviderMK2, IFluidStandardTransceiverMK2, ITickableBE, MenuProvider {

    public static final long MAX_POWER = 1_000_000L;
    private static final double EFFICIENCY = 0.85D;
    private static final int OPS_HEAT_CAP = 6_000;
    private static final int SLOT_ID = 0;
    private static final int SLOT_ID_OUT = 1;
    private static final int SLOT_LOAD = 2;
    private static final int SLOT_LOAD_OUT = 3;
    private static final int SLOT_BATTERY = 4;
    private static final int SLOT_UNLOAD = 5;
    private static final int SLOT_UNLOAD_OUT = 6;

    public final FluidTankNTM[] tanks;
    private long power;

    public MachineTurbineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 7, true, true);
        tanks = new FluidTankNTM[]{
                new FluidTankNTM(Fluids.STEAM, 64_000).withOwner(this),
                new FluidTankNTM(Fluids.SPENTSTEAM, 128_000).withOwner(this)
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTurbine");
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack stack) {
        // CE TileEntityMachineTurbine.java:80-88
        if (i == SLOT_ID) return stack.getItem() instanceof IItemFluidIdentifier;
        if (i == SLOT_BATTERY) return Library.isBattery(stack);
        return true;
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            this.tryProvide(level, target.getX(), target.getY(), target.getZ(), dir);
        }

        // CE TileEntityMachineTurbine.java:129-131
        tanks[0].setType(SLOT_ID, SLOT_ID_OUT, inventory);
        tanks[0].loadTank(SLOT_LOAD, SLOT_LOAD_OUT, inventory);
        power = Library.chargeItemsFromTE(inventory, SLOT_BATTERY, power, MAX_POWER);

        FluidType in = tanks[0].getTankType();
        boolean valid = false;
        if (in.hasTrait(FT_Coolable.class)) {
            FT_Coolable trait = in.getTrait(FT_Coolable.class);
            double eff = trait.getEfficiency(FT_Coolable.CoolingType.TURBINE) * EFFICIENCY;
            if (eff > 0) {
                tanks[1].setTankType(trait.coolsTo);
                int inputOps = tanks[0].getFill() / trait.amountReq;
                int outputOps = (tanks[1].getMaxFill() - tanks[1].getFill()) / trait.amountProduced;
                int cap = OPS_HEAT_CAP / trait.amountReq;
                int ops = Math.min(inputOps, Math.min(outputOps, cap));
                tanks[0].setFill(tanks[0].getFill() - ops * trait.amountReq);
                tanks[1].setFill(tanks[1].getFill() + ops * trait.amountProduced);
                power += (long) (ops * trait.heatEnergy * eff);
                valid = true;
            }
        }
        if (!valid) tanks[1].setTankType(Fluids.NONE);
        if (power > MAX_POWER) power = MAX_POWER;

        for (Direction dir : Direction.values()) {
            BlockPos target = worldPosition.relative(dir);
            this.tryProvide(tanks[1], level, target, dir);
            this.trySubscribe(tanks[0].getTankType(), level, target.getX(), target.getY(), target.getZ(), dir);
        }

        // CE TileEntityMachineTurbine.java:155
        tanks[1].unloadTank(SLOT_UNLOAD, SLOT_UNLOAD_OUT, inventory);

        dataChanged();
        networkPackMK2(50);
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
    public List<FluidTankNTM> getSendingTanks() {
        return List.of(tanks[1]);
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of(tanks[0]);
    }

    @Override
    public List<FluidTankNTM> getAllTanks() {
        return List.of(tanks);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tanks[0].writeToNBT(tag, "water");
        tanks[1].writeToNBT(tag, "steam");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        tanks[0].readFromNBT(tag, "water");
        tanks[1].readFromNBT(tag, "steam");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeLong(power);
        tanks[0].serialize(buf);
        tanks[1].serialize(buf);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        power = buf.readLong();
        tanks[0].deserialize(buf);
        tanks[1].deserialize(buf);
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new MachineTurbineMenu(containerId, playerInventory, this);
    }
}
