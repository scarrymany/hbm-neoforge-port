package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.util.RandomSource;

import java.util.Collections;
import java.util.List;

/**
 * Jungle-ruin brick that collapses as soon as something touches it, and cascades to any
 * like-typed neighbors shortly after. Ported from CE's {@code FragileBrick}; CE's
 * {@code getItemDropped} returning {@code null} (never drops) is preserved via an explicit empty
 * {@link #getDrops} override.
 */
public class FragileBrick extends BlockBase {

    private static final int NEIGHBOR_SCHEDULE_MIN_TICKS = 8;
    private static final int NEIGHBOR_SCHEDULE_RANDOM_TICKS = 4;

    public FragileBrick(Properties properties) {
        super(properties);
    }

    @Override
    protected List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return Collections.emptyList();
    }

    @Override
    protected void tick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) {
        level.destroyBlock(pos, false);
        notifyNeighbors(level, pos);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide()) {
            level.destroyBlock(pos, false);
            notifyNeighbors(level, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            notifyNeighbors(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void notifyNeighbors(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction direction : Direction.values()) {
            BlockPos next = pos.relative(direction);
            if (level.getBlockState(next).is(this)) {
                level.scheduleTick(next, this, NEIGHBOR_SCHEDULE_MIN_TICKS + level.getRandom().nextInt(NEIGHBOR_SCHEDULE_RANDOM_TICKS));
            }
        }
    }
}
