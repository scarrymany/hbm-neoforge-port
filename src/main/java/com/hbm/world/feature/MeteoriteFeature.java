package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.hbm.world.MeteoriteGenerator;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ported from CE's {@code com.hbm.world.MeteoriteStructure} (50 lines, full read), triggered in CE by
 * {@code HbmWorldGen.generateAStructure} (lines 303-310, called at line 380 with
 * {@code CompatibilityConfig.meteoriteSpawn}) - the ambient, passive "just a mineable rock formation"
 * meteor world-gen feature. Not a dungeon: no loot, no mobs, no tile entity. See
 * docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 2a.
 * <p>
 * All of the actual block-shape logic lives in the shared, reusable {@link MeteoriteGenerator} (this
 * feature always calls it with {@code safe=false, allowSpecials=false, damagingImpact=false}, matching
 * CE's own {@code new Meteorite().generate(world, rand, x, y, z, false, false, false)} call exactly).
 * {@code OilMeteorPlacedFeatures} supplies one candidate surface position per chunk via
 * {@code HeightmapPlacement.onHeightmap(WORLD_SURFACE_WG)}; this method then reproduces CE's own
 * {@code y = world.getHeight(x,z) - rand.nextInt(10)} drop and ground-check exactly.
 */
public class MeteoriteFeature extends Feature<NoneFeatureConfiguration> {

    private static final int MAX_SURFACE_DROP = 10;

    public MeteoriteFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.meteoriteSpawn(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        int x = origin.getX();
        int z = origin.getZ();
        int y = origin.getY() - random.nextInt(MAX_SURFACE_DROP);
        if (y <= 1) return false;

        BlockPos ground = new BlockPos(x, y - 2, z);
        BlockState state = level.getBlockState(ground);
        if (state.isAir() || !state.getFluidState().isEmpty()) return false;

        MeteoriteGenerator.generate(level, random, x, y, z, false, false, false);
        return true;
    }
}
