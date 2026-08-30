package com.hbm.blocks.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.bomb.LaunchPadRustedBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IBomb;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.LaunchPadRusted} (92 lines, read in full) - the
 * world-gen "silo" pad. Same {@code BlockDummyable} shape as {@link LaunchPad} ({@code
 * getDimensions()={0,0,1,1,1,1}}, offset 1), but its block entity ({@link
 * LaunchPadRustedBlockEntity}) is a standalone, non-{@code LaunchPadBaseBlockEntity} class - see
 * that class's javadoc. {@code getItemDropped} returns air, matching CE exactly (this block is
 * meant to be a permanent, non-recoverable part of the generated silo structure).
 */
public class LaunchPadRusted extends BlockDummyable implements IBomb {

    public LaunchPadRusted(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-1.5D, 0D, -1.5D, -0.5D, 1D, -0.5D));
        this.bounding.add(new AABB(0.5D, 0D, -1.5D, 1.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-1.5D, 0D, 0.5D, -0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(0.5D, 0D, 0.5D, 1.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-0.5D, 0.5D, -1.5D, 0.5D, 1D, 1.5D));
        this.bounding.add(new AABB(-1.5D, 0.5D, -0.5D, 1.5D, 1D, 0.5D));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new LaunchPadRustedBlockEntity(BombBlockEntities.LAUNCH_PAD_RUSTED.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BombBlockEntities.LAUNCH_PAD_RUSTED.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this.standardOpenBehavior(level, pos, player);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 1, 1, 1, 1};
    }

    @Override
    public int getOffset() {
        return 1;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof LaunchPadRustedBlockEntity entity) {
                return entity.launch();
            }
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof LaunchPadRustedBlockEntity launchpad) {
                launchpad.updateRedstonePower(pos);
            }
        }
    }
}
