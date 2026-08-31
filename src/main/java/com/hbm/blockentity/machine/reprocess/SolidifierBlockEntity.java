package com.hbm.blockentity.machine.reprocess;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.reprocess.SolidifierMenu;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.SolidificationRecipes;
import com.hbm.lib.DirPos;
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
 * CE {@code TileEntityMachineSolidifier.java}: maxPower 100_000, usageBase 250, processTimeBase 60,
 * tank 24_000. Upgrades not ported — base numbers only.
 */
public class SolidifierBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardReceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int SLOT_OUT = 0;
    private static final int SLOT_BATTERY = 1;

    public static final long MAX_POWER = 100_000L;
    public static final int USAGE = 250;
    public static final int PROCESS_TIME = 60;
    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM tank;
    public long power;
    public int progress;
    public boolean isProcessing;

    public SolidifierBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, true);
        tank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineSolidifier");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot == SLOT_BATTERY && Library.isBattery(stack);
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return slot == SLOT_OUT;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SLOT_OUT};
    }

    public int getProgressScaled(int i) {
        return (progress * i) / PROCESS_TIME;
    }

    public DirPos[] getConPos() {
        BlockPos p = worldPosition;
        return new DirPos[]{
                new DirPos(p.getX() + 1, p.getY(), p.getZ(), Direction.EAST),
                new DirPos(p.getX() - 1, p.getY(), p.getZ(), Direction.WEST),
                new DirPos(p.getX(), p.getY(), p.getZ() + 1, Direction.SOUTH),
                new DirPos(p.getX(), p.getY(), p.getZ() - 1, Direction.NORTH)
        };
    }

    @Override
    public void updateEntity() {
        if (level == null || level.isClientSide) return;

        power = Library.chargeTEFromItems(inventory, SLOT_BATTERY, power, MAX_POWER);

        for (DirPos dp : getConPos()) {
            trySubscribe(level, dp);
            if (tank.getTankType() != Fluids.NONE) {
                trySubscribe(tank.getTankType(), level, dp.getPos(), dp.getDir());
            }
        }

        if (canProcess()) {
            isProcessing = true;
            power -= USAGE;
            progress++;
            if (progress >= PROCESS_TIME) {
                SolidificationRecipes.Output out = SolidificationRecipes.getOutput(tank.getTankType());
                tank.setFill(tank.getFill() - out.amount());
                ItemStack have = inventory.getStackInSlot(SLOT_OUT);
                if (have.isEmpty()) {
                    inventory.setStackInSlot(SLOT_OUT, out.stack().copy());
                } else {
                    have.grow(out.stack().getCount());
                }
                progress = 0;
            }
        } else {
            isProcessing = false;
            progress = 0;
        }

        dataChanged();
        networkPackMK2(100);
    }

    private boolean canProcess() {
        if (power < USAGE) return false;
        SolidificationRecipes.Output out = SolidificationRecipes.getOutput(tank.getTankType());
        if (out == null) return false;
        if (out.amount() > tank.getFill()) return false;
        ItemStack have = inventory.getStackInSlot(SLOT_OUT);
        if (have.isEmpty()) return true;
        return ItemStack.isSameItemSameComponents(have, out.stack())
                && have.getCount() + out.stack().getCount() <= have.getMaxStackSize();
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
    public long getDemand(FluidType type, int pressure) {
        if (tank.getPressure() != pressure) return 0;
        if (tank.getTankType() == type || (tank.getTankType() == Fluids.NONE && tank.getFill() == 0)) {
            return tank.getMaxFill() - tank.getFill();
        }
        return 0;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        if (tank.getPressure() != pressure) return amount;
        if (tank.getTankType() == Fluids.NONE && tank.getFill() == 0) tank.setTankType(type);
        if (tank.getTankType() != type) return amount;
        int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
        tank.setFill(tank.getFill() + toAdd);
        return amount - toAdd;
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
    public boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("power", power);
        tag.putInt("progress", progress);
        tank.writeToNBT(tag, "t");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        power = tag.getLong("power");
        progress = tag.getInt("progress");
        tank.readFromNBT(tag, "t");
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        tank.serialize(buf);
        buf.writeLong(power);
        buf.writeBoolean(isProcessing);
        buf.writeInt(progress);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        tank.deserialize(buf);
        power = buf.readLong();
        isProcessing = buf.readBoolean();
        progress = buf.readInt();
    }

    @Override
    public void writeNBT(CompoundTag nbt) {
        tank.writeToNBT(nbt, "nt");
    }

    @Override
    public void readNBT(CompoundTag nbt) {
        tank.readFromNBT(nbt, "nt");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new SolidifierMenu(containerId, playerInventory, this);
    }
}
