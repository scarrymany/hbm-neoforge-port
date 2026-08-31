package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.IntStream;

/**
 * Group C's substrate pass, ported from CE's {@code SchistStratum} (74 lines, read in full): a
 * wavy horizontal band of {@code stone_gneiss} centered at y=30, painted before any of the eight
 * gneiss ore veins ({@link EllipsoidOreFeature} instances targeting {@code stone_gneiss}) can find
 * anything to place into - dimension 0 only, no config gate at all in CE (always on), preserved
 * that way here.
 * <p>
 * CE lazily built its single {@code NoiseGeneratorPerlin} from whichever chunk's own
 * {@code DecorateBiomeEvent.Pre}-supplied {@code Random} happened to fire first - an order-dependent
 * bootstrap quirk that has no clean equivalent once re-expressed as a stateless, per-chunk-invoked
 * {@code Feature} (there is no single persistent "first call" moment any more). This port instead
 * seeds the noise field once per world from {@code level.getSeed()} (cached by seed, so multiple
 * worlds in one JVM session - integration tests, a dedicated server switching worlds - don't
 * collide), which is at least as faithful to CE's evident intent (a stratum shape that is
 * consistent across a given world's own chunks, not literally fixed across every CE world the way
 * {@link BedrockOreFeature}'s density scan deliberately is) while being fully deterministic and
 * order-independent. Uses the same {@link PerlinNoise} API this port's own
 * {@code ItemBedrockOreBase.getOreLevel} already relies on for the equivalent 1.12
 * {@code NoiseGeneratorPerlin} substitution.
 */
public class GneissStratumFeature extends Feature<NoneFeatureConfiguration> {

    private static final double SCALE = 0.01D;
    private static final double THRESHOLD = 5.0D;
    private static final double THICKNESS = 8.0D;
    private static final int CENTER_Y = 30;

    private static final ConcurrentMap<Long, PerlinNoise> NOISE_CACHE = new ConcurrentHashMap<>();

    public GneissStratumFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        if (!OreShapeUtil.dimension(level).equals(Level.OVERWORLD)) return false;

        Block gneiss = OreShapeUtil.block("stone_gneiss");
        if (gneiss == null) return false;
        BlockState gneissState = gneiss.defaultBlockState();

        long seed = OreShapeUtil.seed(level);
        PerlinNoise noise = NOISE_CACHE.computeIfAbsent(seed,
                s -> PerlinNoise.create(RandomSource.create(s), IntStream.rangeClosed(-3, 0)));

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        int minY = Math.max(level.getMinBuildHeight() + 1, 1);
        int maxY = level.getMaxBuildHeight() - 1;

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = chunkMinX; x < chunkMinX + 16; x++) {
            for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
                double n = noise.getValue(x * SCALE, 0, z * SCALE);
                if (n <= THRESHOLD) continue;

                double range = (n - THRESHOLD) * (THICKNESS * 0.5 - 1);
                if (range > THICKNESS * 0.5) range = THICKNESS - range;
                if (range <= 0) continue;

                int r = (int) range;
                int yStart = Math.max(minY, CENTER_Y - r);
                int yEnd = Math.min(maxY, CENTER_Y + r);

                for (int y = yStart; y <= yEnd; y++) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.STONE)) {
                        level.setBlock(pos, gneissState, 2 | 16);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }
}
