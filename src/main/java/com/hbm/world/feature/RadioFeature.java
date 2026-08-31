package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.common.Tags;

/**
 * Port of CE {@code Radio01}+{@code Radio02} (~7282 lines, split for javac method-size).
 * Schematic is the extracted {@code setBlockState} table including {@code Library.getRandomConcrete}
 * ({@code Radio01.java}:43, 56+) and the {@code part2.generate_r00} continuation
 * ({@code Radio01.java}:5099). Biome gate: {@code temp >= 0.8F && rainfall > 0.7F}
 * ({@code HbmWorldGen.java}:361-362) — 1.21 rainfall proxy is jungle/swamp. Spawn check at
 * {@code +(5,0,15)} ({@code Radio01.java}:30-31). Config default 1-in-1000.
 */
public class RadioFeature extends Feature<NoneFeatureConfiguration> {

    public RadioFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.radioStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        var biome = level.getBiome(origin);
        if (biome.value().getBaseTemperature() < 0.8F) return false;
        if (!(biome.is(BiomeTags.IS_JUNGLE) || biome.is(Tags.Biomes.IS_SWAMP))) return false;

        BlockPos check = origin.offset(5, 0, 15);
        BlockState above = level.getBlockState(check);
        if (!above.isAir()) return false;
        BlockState ground = level.getBlockState(check.below());
        if (!(ground.is(Blocks.GRASS_BLOCK) || ground.is(Blocks.DIRT) || ground.is(BlockTags.DIRT)
                || ground.is(Blocks.STONE) || ground.is(Blocks.SAND))) {
            return false;
        }

        CeSchematicPlacer.place(level, origin, random, "radio");
        return true;
    }
}
