package com.hbm.blocks.generic;

import com.hbm.blocks.BlockBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

/**
 * Ported from CE's {@code BlockHydroreactive} ({@code block_lithium}): violently explodes and
 * destroys itself the moment it touches water, whether the water arrived first (neighbor update) or
 * the block was placed next to existing water. CE's rain-triggered smoke-particle
 * {@code randomDisplayTick} is purely a client-side cosmetic and is not reproduced here.
 * <p>
 * {@code neighborChanged}'s trailing parameter is {@link BlockPos} (the neighbor's position), not
 * {@code Orientation} - confirmed against this exact toolchain by the Neo Edition reference, where
 * every override of this method (14+ call sites across {@code GasBaseBlock}, {@code LayeringBlock},
 * {@code DummyableBlock}, etc.) uses the {@code BlockPos fromPos} form. Likewise {@link Level#explode}
 * takes {@code (Entity, double, double, double, float, ExplosionInteraction)} with no trailing
 * {@code boolean} - confirmed against {@code Meteor}, {@code TNTBlock} and {@code SemtexBlock} in
 * that same reference.
 */
public class BlockHydroreactive extends BlockBase {

    private static final float EXPLOSION_POWER = 15.0F;

    public BlockHydroreactive(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, fromPos, movedByPiston);
        reactIfTouchingWater(level, pos);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        reactIfTouchingWater(level, pos);
    }

    private void reactIfTouchingWater(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (level.getFluidState(pos.relative(dir)).is(Fluids.WATER)) {
                level.removeBlock(pos, false);
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EXPLOSION_POWER, Level.ExplosionInteraction.BLOCK);
                return;
            }
        }
    }
}
