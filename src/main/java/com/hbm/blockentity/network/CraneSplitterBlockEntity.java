package com.hbm.blockentity.network;

import com.hbm.blockentity.LoadedBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ported from CE's {@code com.hbm.tileentity.network.TileEntityCraneSplitter} (read in full). Holds
 * the ratio-based 1-input/2-output item split state for {@code CraneSplitterBlock}'s core position.
 * Extends {@link LoadedBaseBlockEntity} (CE's {@code TileEntityLoadedBase}) - this block has no
 * inventory of its own (items pass straight through as {@code EntityMovingItem}s, never sitting in a
 * slot), so {@code MachineBaseBlockEntity}'s {@code ItemStackHandler} plumbing would be pure dead
 * weight here.
 * <p>
 * Not implementing {@link com.hbm.blockentity.IPersistentNBT}: {@code leftRatio}/{@code rightRatio}
 * are a player-tunable configuration value (screwdriver-adjusted), not "contents" a stack needs to
 * carry through a break/place cycle - the same judgment call this port already made for
 * {@code PylonMediumBlockEntity}'s color setting, the closest existing precedent for a simple
 * multiblock with player-configurable-but-not-persistent-across-breaks state.
 */
public class CraneSplitterBlockEntity extends LoadedBaseBlockEntity {

    /** false: left belt is preferred, true: right belt is preferred. */
    private boolean position;
    /** count until position swaps. */
    private byte remaining;

    public byte leftRatio = 1;
    public byte rightRatio = 1;

    public CraneSplitterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Splits the input stack into two, based on the current ratio and internal round-robin state. */
    public ItemStack[] splitStack(ItemStack stack) {
        int left = 0;
        int right = 0;
        int count = stack.getCount();

        if (remaining <= 0) remaining = position ? rightRatio : leftRatio;

        while (count > 0) {
            int toExtract = Math.min(remaining, count);

            remaining -= (byte) toExtract;
            count -= toExtract;
            if (position) right += toExtract; else left += toExtract;

            if (remaining <= 0) {
                position = !position;
                remaining = position ? rightRatio : leftRatio;
            }
        }

        ItemStack leftStack = stack.copy();
        ItemStack rightStack = stack.copy();
        leftStack.setCount(left);
        rightStack.setCount(right);

        this.setChanged();
        return new ItemStack[]{leftStack, rightStack};
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        position = tag.getBoolean("pos");
        remaining = tag.getByte("count");

        // Make sure existing conveyors are initialised with ratios
        leftRatio = (byte) Math.max(tag.getByte("left"), 1);
        rightRatio = (byte) Math.max(tag.getByte("right"), 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.putBoolean("pos", position);
        tag.putByte("count", remaining);

        tag.putByte("left", leftRatio);
        tag.putByte("right", rightRatio);
    }

    @Override
    public void serialize(RegistryFriendlyByteBuf buf) {
        super.serialize(buf);
        buf.writeByte(leftRatio);
        buf.writeByte(rightRatio);
    }

    @Override
    public void deserialize(RegistryFriendlyByteBuf buf) {
        super.deserialize(buf);
        leftRatio = buf.readByte();
        rightRatio = buf.readByte();
    }
}
