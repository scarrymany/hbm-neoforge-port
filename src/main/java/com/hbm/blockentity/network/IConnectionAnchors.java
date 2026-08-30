package com.hbm.blockentity.network;

import com.hbm.lib.DirPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Ported from CE's {@code com.hbm.tileentity.IConnectionAnchors} ({@code TileEntity}/{@code World}
 * -&gt; {@code BlockEntity}/{@code Level}). {@link #notifyAnchors} is called after a duct's
 * {@code FluidType} changes ({@code PipeBaseBlockEntity#setFluidType}) so any wrench-linked
 * {@code PipeAnchorBlockEntity} whose connection set doesn't include the changed block as a direct
 * face-neighbor still gets a neighbor-changed style notification.
 *
 * <p>CE's version single-targets each {@link DirPos} via {@code World#neighborChanged(BlockPos, Block,
 * BlockPos)} (a per-position notify) when the source implements this interface, falling back to a
 * blanket 6-neighbor notify otherwise. That single-target overload could not be independently
 * confirmed against real 1.21.1 usage anywhere in this repo (only its blanket-neighbor sibling,
 * {@code Level#updateNeighborsAt}, is confirmed - used by Neo Edition's own {@code GeigerBlockEntity}/
 * {@code RadioTorchReceiverBlockEntity}), and this path is dead code for this pass regardless (nothing
 * yet implements {@link #getConPos()} - the wrench/anchor-linking item itself is out of scope, see
 * {@code docs/phase2/network_fluid_ducts.md}'s Deferred scope). Rather than guess at an unconfirmed
 * signature for a currently-unreachable branch, both branches use the confirmed blanket notify; a
 * future pass wiring the wrench can restore the single-target path once that call shape is confirmed.
 */
public interface IConnectionAnchors {

    DirPos[] getConPos();

    static void notifyAnchors(BlockEntity be) {
        if (be == null) return;
        Level level = be.getLevel();
        if (level == null || level.isClientSide) return;
        Block source = be.getBlockState().getBlock();
        BlockPos from = be.getBlockPos();
        level.updateNeighborsAt(from, source);
    }
}
