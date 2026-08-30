package com.hbm.blockentity.network;

import com.hbm.blocks.network.FluidValveBlock;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Redstone-gated duct valve, ported from CE's {@code com.hbm.tileentity.network.TileEntityFluidValve}
 * - shared by both {@code FluidValve} (right-click toggled) and {@code FluidSwitch} (redstone-driven)
 * in CE, exactly like this port's {@link FluidValveBlock}/{@code FluidSwitchBlock} share this one
 * block-entity class and its {@link FluidValveBlock#ACTIVE} property. {@link #shouldCreateNode()}
 * reads that property directly off the live {@link #getBlockState()} instead of CE's own cached
 * {@code getBlockMetadata()} - 1.21's block entity keeps its owning block's current state in sync
 * automatically, so there is no equivalent metadata cache to invalidate.
 */
public class FluidValveBlockEntity extends PipeBaseBlockEntity {

    public FluidValveBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public boolean shouldCreateNode() {
        return getBlockState().getValue(FluidValveBlock.ACTIVE);
    }

    /** Called by the block after its {@link FluidValveBlock#ACTIVE} state flips. */
    public void updateState() {
        if (level == null || level.isClientSide) return;

        if (!getBlockState().getValue(FluidValveBlock.ACTIVE)) {
            if (this.node != null) {
                UniNodespace.destroyNode(level, node);
                this.node = null;
            }
        } else {
            // Turning on: drop the (possibly stale/absent) node reference so the next tick's
            // updateEntity() re-attaches or creates one - mirrors CE's own "must recreate/reattach the
            // node when switching on, otherwise update() never ticks again" comment.
            this.node = null;
            setChanged();
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
