package com.hbm.world.feature;

import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ported from CE's {@code com.hbm.world.OilSandBubble} (89 lines, full read), triggered in CE by
 * {@code HbmWorldGen}'s inline desert-biome roll (lines 624-632) - a shallow desert-surface
 * {@code ore_oil_sand} patch, mined and liquefied via the chemical-plant recipe chain rather than the
 * derrick/pumpjack BFS mechanic. See docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 1.
 * <p>
 * CE hardcodes this roll's odds as a bare {@code rand.nextInt(600)} with <b>no</b>
 * {@code CompatibilityConfig} entry at all (confirmed by direct read of {@code HbmWorldGen.java}) -
 * preserved here as the same hardcoded constant, not a config gap to fill in.
 * <p>
 * CE's biome gate is {@code !biome.canRain() && getDefaultTemperature() >= 1.8F}; substituted with
 * {@code getPrecipitationAt(pos) == Biome.Precipitation.NONE}, the same confirmed-real substitution
 * {@link OilBubbleFeature} uses for its own hot/dry check (no direct "canRain" analog exists on the
 * modern datapack-driven {@code Biome}, and no rainfall/downfall accessor was confirmed reachable in
 * this sandbox).
 */
public class OilSandBubbleFeature extends Feature<NoneFeatureConfiguration> {

    private static final int SPAWN_CHANCE = 600;
    private static final int MIN_RADIUS = 15;
    private static final int RADIUS_VARIATION = 31;

    public OilSandBubbleFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get()) return false;

        Biome biome = level.getBiome(origin).value();
        boolean hotAndDry = biome.getBaseTemperature() >= 1.8F && biome.getPrecipitationAt(origin) == Biome.Precipitation.NONE;
        if (!hotAndDry) return false;
        if (random.nextInt(SPAWN_CHANCE) != 0) return false;

        Block oreOilSand = OreShapeUtil.block("ore_oil_sand");
        if (oreOilSand == null) return false;

        int radius = MIN_RADIUS + random.nextInt(RADIUS_VARIATION);
        spawnOilSand(level, random, origin.getX(), origin.getY(), origin.getZ(), radius, oreOilSand);
        return true;
    }

    private static void spawnOilSand(WorldGenLevel level, RandomSource random, int x, int y, int z, int radius, Block target) {
        int r2 = radius * radius;
        int r22 = r2 / 2;
        BlockState oreState = target.defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int xx = -radius; xx < radius; xx++) {
            int xPos = xx + x;
            int xx2 = xx * xx;
            for (int yy = -radius; yy < radius; yy++) {
                int yPos = yy + y;
                int yy2 = xx2 + yy * yy * 3;
                for (int zz = -radius; zz < radius; zz++) {
                    int zPos = zz + z;
                    int distSq = yy2 + zz * zz;
                    // CE's own extra random-widening term, distinct from OilBubble's plain ellipsoid.
                    if (distSq < r22 + random.nextInt(Math.max(1, r22 / 3))) {
                        pos.set(xPos, yPos, zPos);
                        if (level.getBlockState(pos).is(Blocks.SAND)) {
                            level.setBlock(pos, oreState, 2 | 16);
                        }
                    }
                }
            }
        }
    }
}
