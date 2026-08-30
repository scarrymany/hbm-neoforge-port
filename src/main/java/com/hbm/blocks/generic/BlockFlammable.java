package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Generic configurable-flammability block base, ported from CE's {@code BlockFlammable}
 * ({@code BlockMeta} subclass with a per-instance encouragement/flammability pair). Method
 * signatures confirmed against the Neo Edition reference's own equivalent {@code FlammableBlock}
 * (same shape, same override signatures) rather than guessed at.
 * <p>
 * <b>No concrete instance is registered from this area.</b> CE itself never constructs a
 * {@code BlockFlammable} anywhere in its own {@code ModBlocks} (confirmed by searching the upstream
 * CE source tree) - it exists in CE only as reusable infrastructure for a fuel/combustible block a
 * future content pass might add. Kept compiled here for the same reason and matching the same
 * precedent as {@link BlockOreMeta}/{@link BlockGenericSlab}.
 */
public class BlockFlammable extends Block {

    protected final int encouragement;
    protected final int flammability;

    public BlockFlammable(Properties properties, int encouragement, int flammability) {
        super(properties);
        this.encouragement = encouragement;
        this.flammability = flammability;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return flammability;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return encouragement;
    }

    public boolean shouldIgnite(Level level, BlockPos pos) {
        if (flammability == 0) {
            return false;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.FIRE)) {
                return true;
            }
        }
        return false;
    }
}
