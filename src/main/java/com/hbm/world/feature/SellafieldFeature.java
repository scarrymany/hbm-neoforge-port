package com.hbm.world.feature;

import com.hbm.blocks.generic.BlockSellafield;
import com.hbm.blocks.generic.WastelandVirusBlocks;
import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE {@code Sellafield} crater ({@code Sellafield.java:59-96}) triggered by
 * {@code HbmWorldGen.generateSellafieldPool} ({@code HbmWorldGen.java:321-334},
 * biome gate {@code :382-384}).
 * <p>
 * CE numbers: {@code radfreq} default {@code 0:5000}; radius {@code rand(15)+10},
 * 1-in-50 {@code r=50}; depth {@code r*0.35}. Rings are {@code sellafield} meta 4→0
 * then {@code sellafield_slaked}; core meta 5. TE never created in CE
 * ({@code Sellafield.java:149-155}).
 * <p>
 * {@code AbstractPhasedStructure} chunk-wait is not ported —
 * TODO(CE: Sellafield.java:20-45). Columns in missing chunks are skipped
 * ({@code hasChunk}, same as {@link OilSpot}).
 * <p>
 * 1.12 {@code TempCategory.WARM} is {@code getDefaultTemperature() >= 1.0} and not
 * ocean. No invented biomes — filter in {@link #place} only.
 */
public class SellafieldFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_RADIUS = 10;
    private static final int RADIUS_VARIATION = 15;
    private static final int GIANT_CHANCE = 50;
    private static final int GIANT_RADIUS = 50;
    private static final double DEPTH_SCALE = 0.35D;
    private static final int PLACE_DEPTH = 3;
    private static final int FLAGS = 2 | 16;

    public SellafieldFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get() || !GeneralConfig.ENABLE_RAD_HOTSPOT_SPAWN.get()) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.radfreq(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        var biome = level.getBiome(origin);
        if (biome.is(BiomeTags.IS_OCEAN)) return false;
        if (biome.value().getBaseTemperature() < 1.0F) return false;

        double radius = random.nextInt(RADIUS_VARIATION) + MIN_RADIUS;
        if (random.nextInt(GIANT_CHANCE) == 0) {
            radius = GIANT_RADIUS;
        }
        generate(level, random, origin.getX(), origin.getZ(), radius, radius * DEPTH_SCALE);
        return true;
    }

    private static double depthFunc(double x, double rad, double depth) {
        return -Math.pow(x, 2) / Math.pow(rad, 2) * depth + depth;
    }

    private static void generate(WorldGenLevel level, RandomSource rand, int x, int z, double radius, double depth) {
        int iRad = (int) Math.round(radius);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int a = -iRad - 5; a <= iRad + 5; a++) {
            for (int b = -iRad - 5; b <= iRad + 5; b++) {
                int cx = x + a;
                int cz = z + b;
                if (!level.hasChunk(cx >> 4, cz >> 4)) continue;

                double r = Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2));
                if (r - rand.nextInt(3) > radius) continue;

                int dep = (int) depthFunc(r, radius, depth);
                dig(level, pos, cx, cz, dep);

                BlockState ring;
                if (r + rand.nextInt(3) <= radius / 6D) {
                    ring = sellafield(4);
                } else if (r - rand.nextInt(3) <= radius / 6D * 2D) {
                    ring = sellafield(3);
                } else if (r - rand.nextInt(3) <= radius / 6D * 3D) {
                    ring = sellafield(2);
                } else if (r - rand.nextInt(3) <= radius / 6D * 4D) {
                    ring = sellafield(1);
                } else if (r - rand.nextInt(3) <= radius / 6D * 5D) {
                    ring = sellafield(0);
                } else {
                    ring = WastelandVirusBlocks.SELLAFIELD_SLAKED.get().defaultBlockState();
                }
                place(level, pos, cx, cz, PLACE_DEPTH, ring);
            }
        }
        placeCore(level, pos, x, z);
    }

    private static BlockState sellafield(int meta) {
        return WastelandVirusBlocks.SELLAFIELD.get().defaultBlockState().setValue(BlockSellafield.LEVEL, meta);
    }

    private static int surfaceY(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
    }

    private static void dig(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int depth) {
        int y = surfaceY(level, x, z);
        if (y < depth * 2) return;
        for (int i = 0; i < depth; i++) {
            pos.set(x, y - i, z);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
    }

    private static void place(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z, int depth, BlockState block) {
        int y = surfaceY(level, x, z);
        for (int i = 0; i < depth; i++) {
            pos.set(x, y - i, z);
            level.setBlock(pos, block, FLAGS);
        }
    }

    private static void placeCore(WorldGenLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
        if (!level.hasChunk(x >> 4, z >> 4)) return;
        pos.set(x, surfaceY(level, x, z), z);
        level.setBlock(pos, sellafield(5), FLAGS);
        // TODO(CE: Sellafield.java:149-155) TileEntitySellafield never created in CE.
    }
}
