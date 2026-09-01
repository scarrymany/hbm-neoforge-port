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
 * CE {@code com.hbm.world.Spaceship} + {@code Spaceship2} ({@code Spaceship.java}:27-1107).
 * <p>
 * Gates: {@code enableDungeons}. Chance {@code CompatibilityConfig.spaceshipStructure} default
 * overworld {@code 1000} (CE {@code 0:1000}, {@code 03.12_spaceshipSpawn}). No biome filter
 * ({@code HbmWorldGen.java}:377). Four corners {@code (0,0) (12,0) (0,23) (12,23)} plus sandstone
 * ({@code Spaceship.java}:50-61). {@code y += 1} before placements ({@code Spaceship.java}:86).
 * Loot: {@code POOL_SPACESHIP}×12 ×4 + {@code POOL_EXPENSIVE}×12 with 1/10 {@code gun_vortex}.
 */
public class SpaceshipFeature extends Feature<NoneFeatureConfiguration> {

    public SpaceshipFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.spaceshipStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;

        BlockPos spawn = new BlockPos(x, y, z);
        if (!CeStructureSpawn.locationIsValidSpawn(level, spawn, true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(12, 0, 0), true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(0, 0, 23), true)
                || !CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(12, 0, 23), true)) {
            return false;
        }

        // CE Spaceship.java:86 y += 1 before generate_r0 / Spaceship2.
        CeSchematicPlacer.place(level, spawn.above(), random, "spaceship");
        return true;
    }
}
