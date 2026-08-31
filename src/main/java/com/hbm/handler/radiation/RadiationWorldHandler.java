package com.hbm.handler.radiation;

import com.hbm.blocks.generic.PlantBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Ported from CE's {@code com.hbm.handler.radiation.RadiationWorldHandler} (109 lines, read in
 * full) - the world-destruction block-decay sweep {@link RadiationSystemNT} drives once a pocket's
 * density exceeds the "queue for destruction" threshold. CE actually carries <em>two</em>
 * {@code decayBlock} overloads: a 3-arg one that is the one every live call site in CE's own
 * {@code RadiationSystemNT.handleWorldDestruction} calls (confirmed by grep - both call sites use the
 * 3-arg form), and a {@code @Deprecated} 4-arg {@code isLegacy}-flagged sibling with extra
 * mycelium/sand/trinitite branches that is <b>never called anywhere in CE</b> (confirmed dead code,
 * kept there only for compatibility with some external caller CE itself no longer uses). Only the
 * live 3-arg method is ported here - reproducing dead code would be inventing behavior CE itself does
 * not exercise.
 * <p>
 * <b>1.12-to-1.21 vanilla-block mapping notes</b> (this class's only real porting decisions, since the
 * target blocks/logic are otherwise a direct line-for-line port):
 * <ul>
 *     <li>{@code BlockDoublePlant}/{@code EnumBlockHalf} -> vanilla's {@link DoublePlantBlock}/
 *     {@link DoubleBlockHalf} - the same double-tall-plant idiom (sunflower/lilac/rose bush/peony/
 *     double tallgrass/double fern), unchanged in shape since 1.13. Well-established Mojang-mapping
 *     knowledge; not independently confirmed against a real compiled jar in this sandbox (no other
 *     file in this port references it yet - this port's own double-tall plants use a bespoke
 *     paired-block scheme instead, see {@code com.hbm.blocks.generic.BlockTallPlant}'s javadoc).</li>
 *     <li>{@code Blocks.GRASS} (1.12's grass block) -> {@link Blocks#GRASS_BLOCK}.</li>
 *     <li>{@code Blocks.TALLGRASS} (1.12's single block covering dead-shrub/grass/fern via metadata)
 *     -> matched here via {@link Blocks#SHORT_GRASS}/{@link Blocks#FERN}, the two metadata variants
 *     with a direct 1:1 modern-block successor.</li>
 *     <li>{@code Material.LEAVES} (1.12's material-based leaf check, matching any leaf block from any
 *     mod) -> {@link BlockTags#LEAVES}, the modern data-driven equivalent covering the same "any leaf
 *     block" membership.</li>
 * </ul>
 */
final class RadiationWorldHandler {

    private RadiationWorldHandler() {
    }

    static void decayBlock(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();

        if (block instanceof DoublePlantBlock) {
            DoubleBlockHalf half;
            try {
                half = state.getValue(DoublePlantBlock.HALF);
            } catch (Exception ignored) {
                return;
            }
            BlockPos lowerPos = half == DoubleBlockHalf.LOWER ? pos : pos.below();
            BlockPos upperPos = half == DoubleBlockHalf.LOWER ? pos.above() : pos;
            level.setBlock(upperPos, Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(lowerPos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }

        if (state.is(Blocks.GRASS_BLOCK)) {
            level.setBlock(pos, PlantBlocks.WASTE_EARTH.get().defaultBlockState(), 2);
            return;
        }

        if (state.is(Blocks.SHORT_GRASS) || state.is(Blocks.FERN)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return;
        }

        if (state.is(BlockTags.LEAVES) && block != PlantBlocks.WASTE_LEAVES.get()) {
            if (level.getRandom().nextInt(7) <= 5) {
                level.setBlock(pos, PlantBlocks.WASTE_LEAVES.get().defaultBlockState(), 2);
            } else {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
    }
}
