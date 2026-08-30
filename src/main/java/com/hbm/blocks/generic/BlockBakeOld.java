package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "Automatic transform" base class, ported from CE's {@code BlockBakeOld}. CE's role was to
 * auto-retexture a legacy block at bake time; the port's datagen ground rule replaces that (see the
 * {@code BlockBakeBase} family note in the research report), so what survives here is the real
 * content behavior: scheduling a tick on placement and running an arbitrary transform on it. CE's
 * only concrete instance ({@code ModBlocks.absorber}) is itself {@code @Deprecated} and depends on
 * the Phase-2 {@code BlockAbsorber}/radiation-block family, so no instance is registered from this
 * area - this class is left as reusable infrastructure for whichever phase needs it.
 */
public class BlockBakeOld extends Block {

    private final OnTick onTick;

    public BlockBakeOld(Properties properties, OnTick onTick) {
        super(properties);
        this.onTick = onTick;
    }

    public BlockBakeOld(Properties properties, BlockState replacement) {
        this(properties, (level, pos, state) -> level.setBlockAndUpdate(pos, replacement));
    }

    @FunctionalInterface
    public interface OnTick {
        void tick(Level level, BlockPos pos, BlockState state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        level.scheduleTick(pos, this, 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        onTick.tick(level, pos, state);
    }
}
