package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE {@code com.hbm.world.DesertAtom001}+{@code 002}+{@code 003} ({@code DesertAtom001.java}:23-80).
 * <p>
 * Gates: {@code enableDungeons}. Chance {@code CompatibilityConfig.atomStructure} default overworld
 * {@code 500} (CE {@code 0:500}, {@code 03.03_atomSpawn}). Biome:
 * {@code !canRain() && temp>=2F} ({@code HbmWorldGen.java}:367-368) — 1.21
 * {@code !hasPrecipitation()} + {@code getBaseTemperature()>=2}. One height/spawn point at
 * offset {@code (20,0,16)} plus sandstone + terracotta ({@code DesertAtom001.java}:63-81).
 * FEATURES write-radius 0 clips overflow cells (cite, no ServerLevel cascade).
 */
public class DesertAtomFeature extends Feature<NoneFeatureConfiguration> {

    public DesertAtomFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.atomStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        Holder<Biome> biome = level.getBiome(new BlockPos(x, origin.getY(), z));
        if (biome.value().hasPrecipitation() || biome.value().getBaseTemperature() < 2.0F) {
            return false;
        }

        int hx = x + 20;
        int hz = z + 16;
        if (!level.hasChunk(hx >> 4, hz >> 4)) return false;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hx, hz);
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;

        BlockPos spawn = new BlockPos(x, y, z);
        if (!CeStructureSpawn.locationIsValidSpawn(level, spawn.offset(20, 0, 16), true, true)) {
            return false;
        }

        CeSchematicPlacer.place(level, spawn, random, "desert_atom");
        return true;
    }
}
