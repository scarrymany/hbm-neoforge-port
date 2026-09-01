package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE {@code com.hbm.world.Satellite} dish ({@code Satellite.java}:22-2574) — not satlink.
 * <p>
 * Gates: {@code enableDungeons}. Chance {@code CompatibilityConfig.satelliteStructure} default
 * overworld {@code 500} (CE {@code 0:500}, {@code 03.07_satelliteSpawn}). Biome:
 * {@code temp < 1F || temp > 1.8F} only ({@code HbmWorldGen.java}:373-374). Four corners
 * {@code (0,0) (24,0) (24,30) (0,30)} plus sandstone ({@code Satellite.java}:38-48).
 * Chests: {@code POOL_GENERIC}×8, {@code POOL_ANTENNA}×8, {@code POOL_EXPENSIVE}×12 ×2.
 */
public class SatelliteFeature extends Feature<NoneFeatureConfiguration> {

    public SatelliteFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.satelliteStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        float temp = level.getBiome(new BlockPos(x, origin.getY(), z)).value().getBaseTemperature();
        if (!(temp < 1.0F || temp > 1.8F)) return false;

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;

        BlockPos spawn = new BlockPos(x, y, z);
        if (!CeStructureSpawn.locationIsValidSpawn(level, spawn, true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(24, 0, 0), true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(24, 0, 30), true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(0, 0, 30), true)) {
            return false;
        }

        CeSchematicPlacer.place(level, spawn, random, "satellite");
        return true;
    }
}
