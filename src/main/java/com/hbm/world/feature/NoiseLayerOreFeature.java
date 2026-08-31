package com.hbm.world.feature;

import com.mojang.serialization.Codec;
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
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/**
 * Group E's {@code OreLayer3D} family, ported from CE's {@code OreLayer3D} (137 lines, read in
 * full): a genuine 3D noise volume replacing plain stone in {@code y in [6,64]} with a
 * {@code BlockResourceStone} state wherever three independent 2D Perlin fields multiply above a
 * threshold. CE's own "pseudo-3D" cache trick is preserved exactly: {@code cacheX} only varies with
 * (z, y) and is shared across all 16 x-columns of the chunk, {@code cacheZ} only varies with (x, y),
 * {@code cacheY} only varies with (x, z) - each collapses one axis, and the product of the three
 * approximates a real 3D field cheaply. This is a deliberate CE optimization, not a bug, and is kept
 * verbatim.
 * <p>
 * CE additionally offsets its scan by {@code +8} into the next chunk (vanilla's classic
 * decoration-centering trick for reducing visible chunk seams under Forge's now-gone
 * {@code DecorateBiomeEvent}). Since every chunk here is decorated by its own independent
 * {@code Feature} invocation covering its own {@code [chunkMinX, chunkMinX+16)} range, dropping that
 * offset still tiles the entire world exactly once with no gaps or overlaps - a safe simplification
 * for the modern per-chunk Feature architecture.
 * <p>
 * {@code id} salts the three noise seeds exactly like CE's own {@code counter++}-assigned instance
 * id, so the three registered instances (hematite/bauxite/malachite) get distinct fields even though
 * all three share this same class.
 */
public class NoiseLayerOreFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_Y = 6;
    private static final int MAX_Y = 64;

    private static final ConcurrentMap<Long, PerlinNoise[]> NOISE_CACHE = new ConcurrentHashMap<>();

    private final int id;
    private final double scaleH;
    private final double scaleV;
    private final double threshold;
    private final String oreBlockName;
    private final BooleanSupplier enabledSupplier;

    public NoiseLayerOreFeature(Codec<NoneFeatureConfiguration> codec, int id, double scaleH, double scaleV,
                                 double threshold, String oreBlockName, BooleanSupplier enabledSupplier) {
        super(codec);
        this.id = id;
        this.scaleH = scaleH;
        this.scaleV = scaleV;
        this.threshold = threshold;
        this.oreBlockName = oreBlockName;
        this.enabledSupplier = enabledSupplier;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!enabledSupplier.getAsBoolean()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        if (!OreShapeUtil.dimension(level).equals(Level.OVERWORLD)) return false;

        Block ore = OreShapeUtil.block(oreBlockName);
        if (ore == null) return false;
        BlockState oreState = ore.defaultBlockState();

        long seed = OreShapeUtil.seed(level);
        PerlinNoise[] noises = NOISE_CACHE.computeIfAbsent(seed * 31L + id, k -> new PerlinNoise[]{
                PerlinNoise.create(RandomSource.create(seed + 101L + id), IntStream.rangeClosed(-3, 0)),
                PerlinNoise.create(RandomSource.create(seed + 102L + id), IntStream.rangeClosed(-3, 0)),
                PerlinNoise.create(RandomSource.create(seed + 103L + id), IntStream.rangeClosed(-3, 0))
        });
        PerlinNoise noiseX = noises[0];
        PerlinNoise noiseY = noises[1];
        PerlinNoise noiseZ = noises[2];

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        int minY = Math.max(MIN_Y, level.getMinBuildHeight());
        int maxY = Math.min(MAX_Y, level.getMaxBuildHeight() - 1);
        int height = maxY - minY + 1;
        if (height <= 0) return false;

        double[][] cacheX = new double[16][height]; // f(z, y)
        double[][] cacheZ = new double[16][height]; // f(x, y)
        double[][] cacheY = new double[16][16];      // f(x, z)

        for (int zOff = 0; zOff < 16; zOff++) {
            int worldZ = chunkMinZ + zOff;
            for (int yi = 0; yi < height; yi++) {
                int y = maxY - yi;
                cacheX[zOff][yi] = noiseX.getValue(y * scaleV, 0, worldZ * scaleH);
            }
        }
        for (int xOff = 0; xOff < 16; xOff++) {
            int worldX = chunkMinX + xOff;
            for (int yi = 0; yi < height; yi++) {
                int y = maxY - yi;
                cacheZ[xOff][yi] = noiseZ.getValue(worldX * scaleH, 0, y * scaleV);
            }
        }
        for (int xOff = 0; xOff < 16; xOff++) {
            int worldX = chunkMinX + xOff;
            for (int zOff = 0; zOff < 16; zOff++) {
                int worldZ = chunkMinZ + zOff;
                cacheY[xOff][zOff] = noiseY.getValue(worldX * scaleH, 0, worldZ * scaleH);
            }
        }

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int xOff = 0; xOff < 16; xOff++) {
            int worldX = chunkMinX + xOff;
            for (int zOff = 0; zOff < 16; zOff++) {
                int worldZ = chunkMinZ + zOff;
                double nY = cacheY[xOff][zOff];

                for (int yi = 0; yi < height; yi++) {
                    int y = maxY - yi;
                    double nX = cacheX[zOff][yi];
                    double nZ = cacheZ[xOff][yi];
                    if (nX * nY * nZ <= threshold) continue;

                    pos.set(worldX, y, worldZ);
                    if (level.getBlockState(pos).is(Blocks.STONE)) {
                        level.setBlock(pos, oreState, 2 | 16);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }
}
