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
 * CE {@code com.hbm.world.Barrel} ({@code Barrel.java}:24-398).
 * <p>
 * Gates: {@code enableDungeons}. Chance {@code CompatibilityConfig.barrelStructure} default
 * overworld {@code 5000} (CE {@code 0:5000}, {@code 03.13_barrelSpawn}). Biome:
 * {@code getBaseTemperature() > 1.8F} only ({@code HbmWorldGen.java}:370-371) — no invented
 * biome tags. Four corners {@code (0,0) (4,0) (4,6) (0,6)}, Y = min height of those
 * ({@code Barrel.java}:35-57). Schematic loot {@code crate_steel} + {@code POOL_EXPENSIVE}×16.
 */
public class BarrelFeature extends Feature<NoneFeatureConfiguration> {

    public BarrelFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.barrelStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        if (level.getBiome(origin).value().getBaseTemperature() <= 1.8F) return false;

        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);

        int y0 = height(level, x, z);
        int y1 = height(level, x + 4, z);
        int y2 = height(level, x + 4, z + 6);
        int y3 = height(level, x, z + 6);
        int y = Math.min(Math.min(y0, y1), Math.min(y2, y3));
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;

        BlockPos spawn = new BlockPos(x, y, z);
        if (!CeStructureSpawn.locationIsValidSpawn(level, spawn, true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(4, 0, 0), true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(4, 0, 6), true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(0, 0, 6), true)) {
            return false;
        }

        CeSchematicPlacer.place(level, spawn, random, "barrel");
        return true;
    }

    private static int height(WorldGenLevel level, int x, int z) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
    }
}
