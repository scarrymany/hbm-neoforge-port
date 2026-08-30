package com.hbm.blocks.generic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * "Uber" reinforced concrete, ported from CE's {@code BlockUberConcrete}. CE folded 16 curing-stage
 * textures into one {@code PropertyInteger META} block that randomly ticks up through the stages and,
 * on reaching the last one, collapses into rubble ({@code ModBlocks.concrete_super_broken}, spawning
 * a falling-block entity when possible). Per the flattening rule each stage becomes its own registry
 * entry here, chained together via a lazily-resolved {@code nextStage} supplier (resolved only when
 * a tick actually fires, so registration order in {@code ModBlocks} does not matter) - the same
 * technique {@link BlockForgottenBrick} uses for its hole/emptied-hole pair.
 * <p>
 * CE's terminal "collapse into a falling rubble entity" step is not ported: {@code
 * concrete_super_broken} is a separate block outside this survey's Structural/Doors/Glass scope and
 * has not been registered by any area yet. The curing progression itself (stage 0 through 15) is
 * fully faithful; the final stage simply stops advancing until a later phase adds the rubble block
 * and wires its supplier in here - flagged in the port report rather than guessed at.
 */
public class BlockUberConcrete extends Block {

    private final int stage;
    private final Supplier<? extends Block> nextStage;

    public BlockUberConcrete(Properties properties, int stage, Supplier<? extends Block> nextStage) {
        super(properties);
        this.stage = stage;
        this.nextStage = nextStage;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return nextStage != null;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (nextStage == null || random.nextInt(stage + 1) > 0) {
            return;
        }
        level.setBlockAndUpdate(pos, nextStage.get().defaultBlockState());
    }

    public int getStage() {
        return stage;
    }
}
