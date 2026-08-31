package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Group B's hardcoded australium "treasure zone", ported from CE's {@code HbmWorldGen.generateOres}
 * overworld branch: an unconditional {@code rand.nextInt(4)} attempts per chunk, each a 50-block
 * ellipsoid vein at a random point in {@code y in [15,30)}, but only actually placed inside the
 * fixed absolute region {@code x in [-450,-350], z in [-450,-350]} - a single, deliberate one-off
 * "treasure zone" at fixed world coordinates, not a per-chunk-random feature. This is why
 * {@code CompatibilityConfig.australiumSpawn()}'s dim-0 default is 0 (see {@link EllipsoidOreFeature}
 * for the ordinary, effectively-disabled-by-default australium vein) - CE's real overworld
 * australium source is this fixed zone. Both are ported exactly as found, per the research report's
 * own recommendation.
 */
public class AustraliumTreasureFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MIN_X = -450;
    private static final int MAX_X = -350;
    private static final int MIN_Z = -450;
    private static final int MAX_Z = -350;

    public AustraliumTreasureFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!OreShapeUtil.dimension(level).equals(Level.OVERWORLD)) return false;

        Block australium = OreShapeUtil.block("ore_australium");
        if (australium == null) return false;

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        int attempts = random.nextInt(4);

        boolean placedAny = false;
        for (int k = 0; k < attempts; k++) {
            int randPosX = chunkMinX + random.nextInt(16);
            int randPosY = random.nextInt(15) + 15;
            int randPosZ = chunkMinZ + random.nextInt(16);

            if (randPosX <= MAX_X && randPosX >= MIN_X && randPosZ <= MAX_Z && randPosZ >= MIN_Z) {
                placedAny |= OreShapeUtil.placeEllipsoidVein(level, random, randPosX, randPosY, randPosZ,
                        50, australium.defaultBlockState(), Blocks.STONE);
            }
        }
        return placedAny;
    }
}
