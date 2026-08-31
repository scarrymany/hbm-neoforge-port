package com.hbm.blocks.bomb;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.bomb.LaunchPadLargeBlockEntity;
import com.hbm.blocks.BlockDummyable;
import com.hbm.interfaces.IBomb;
import com.hbm.main.ModContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import com.hbm.blockentity.bomb.BombBlockEntities;

/**
 * Ported from CE's {@code com.hbm.blocks.bomb.LaunchPadLarge} (110 lines, read in full) - the
 * large erector pad. {@code getDimensions() = {0,0,4,4,4,4}}, offset 4.
 */
public class LaunchPadLarge extends BlockDummyable implements IBomb {

    public LaunchPadLarge(Properties properties) {
        super(properties);
        this.bounding.add(new AABB(-4.5D, 0D, -4.5D, 4.5D, 1D, -0.5D));
        this.bounding.add(new AABB(-4.5D, 0D, 0.5D, 4.5D, 1D, 4.5D));
        this.bounding.add(new AABB(-4.5D, 0.875D, -0.5D, 4.5D, 1D, 0.5D));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new LaunchPadLargeBlockEntity(BombBlockEntities.LAUNCH_PAD_LARGE.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == BombBlockEntities.LAUNCH_PAD_LARGE.get() ? ITickableBE.ticker() : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this.standardOpenBehavior(level, pos, player);
    }

    @Override
    public int[] getDimensions() {
        return new int[]{0, 0, 4, 4, 4, 4};
    }

    @Override
    public int getOffset() {
        return 4;
    }

    @Override
    public BombReturnCode explode(Level level, BlockPos pos, Entity detonator) {
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof LaunchPadLargeBlockEntity launchPadLarge) {
                ModContext.DETONATOR_CONTEXT.set(detonator);
                try {
                    return launchPadLarge.launchFromDesignator();
                } finally {
                    ModContext.DETONATOR_CONTEXT.remove();
                }
            }
        }
        return BombReturnCode.UNDEFINED;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        if (!level.isClientSide()) {
            BlockPos corePos = findCore(level, pos);
            if (corePos != null && level.getBlockEntity(corePos) instanceof LaunchPadLargeBlockEntity launchpad) {
                launchpad.updateRedstonePower(pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    @Override
    protected void fillSpace(Level level, BlockPos placedPos, Direction dir, int placementOffset) {
        super.fillSpace(level, placedPos, dir, placementOffset);

        BlockPos core = placedPos.relative(dir, placementOffset);
        this.makeExtra(level, core.offset(4, 0, 2));
        this.makeExtra(level, core.offset(4, 0, -2));
        this.makeExtra(level, core.offset(-4, 0, 2));
        this.makeExtra(level, core.offset(-4, 0, -2));
        this.makeExtra(level, core.offset(2, 0, 4));
        this.makeExtra(level, core.offset(-2, 0, 4));
        this.makeExtra(level, core.offset(2, 0, -4));
        this.makeExtra(level, core.offset(-2, 0, -4));
    }
}
