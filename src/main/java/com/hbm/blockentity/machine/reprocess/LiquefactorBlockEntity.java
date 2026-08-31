package com.hbm.blockentity.machine.reprocess;

import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardTransceiverMK2;
import com.hbm.blockentity.IPersistentNBT;
import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.MachineBaseBlockEntity;
import com.hbm.inventory.container.machine.reprocess.LiquefactorMenu;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.recipes.LiquefactionRecipes;
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
 * CE {@code TileEntityMachineLiquefactor.java}: maxPower 100_000, usageBase 250, processTimeBase 60,
 * tank 24_000. Upgrades not ported — base numbers only.
 */
public class LiquefactorBlockEntity extends MachineBaseBlockEntity
        implements IEnergyReceiverMK2, IFluidStandardTransceiverMK2, ITickableBE, IPersistentNBT, MenuProvider {

    private static final int SLOT_IN = 0;
    private static final int SLOT_BATTERY = 1;

    public static final long MAX_POWER = 100_000L;
    public static final int USAGE = 250;
    public static final int PROCESS_TIME = 60;
    public static final int TANK_CAPACITY = 24_000;

    public final FluidTankNTM tank;
    public long power;
    public int progress;
    public boolean isProcessing;

    public LiquefactorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 2, true, true);
        tank = new FluidTankNTM(Fluids.NONE, TANK_CAPACITY).withOwner(this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineLiquefactor");
    }

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        if (slot == SLOT_BATTERY) return Library.isBattery(stack);
        return slot == SLOT_IN && LiquefactionRecipes.getOutput(stack) != null;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int amount) {
        return false;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(Direction side) {
        return new int[]{SLOT_IN};
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
            if (tank.getFill() > 0) tryProvide(tank, level, dp);
        }

        ItemStack in = inventory.getStackInSlot(SLOT_IN);
        FluidStack out = LiquefactionRecipes.getOutput(in);
        if (out == null) {
            progress = 0;
            isProcessing = false;
        } else {
            boolean space = (tank.getTankType() == Fluids.NONE || tank.getTankType() == out.type)
                    && tank.getFill() + out.fill <= tank.getMaxFill();
            if (power >= USAGE && space) {
                isProcessing = true;
                progress++;
                power -= USAGE;
                if (progress >= PROCESS_TIME) {
                    inventory.extractItem(SLOT_IN, 1, false);
                    if (tank.getTankType() == Fluids.NONE) tank.setTankType(out.type);
                    tank.setFill(tank.getFill() + out.fill);
                    progress = 0;
                }
            } else {
                isProcessing = false;
            }
        }

        dataChanged();
        networkPackMK2(100);
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
        return 0;
    }

    @Override
    public long transferFluid(FluidType type, int pressure, long amount) {
        return amount;
    }

    @Override
    public List<FluidTankNTM> getReceivingTanks() {
        return List.of();
    }

    @Override
    public List<FluidTankNTM> getSendingTanks() {
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
        return new LiquefactorMenu(containerId, playerInventory, this);
    }
}
