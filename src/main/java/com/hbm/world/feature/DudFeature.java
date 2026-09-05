package com.hbm.world.feature;

import com.hbm.blocks.bomb.CrashedBombBlock.EnumDudType;
import com.hbm.blocks.bomb.NukeCasingBlocks;
import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE {@code com.hbm.world.Dud} ({@code Dud.java}:15-44).
 * <p>
 * Gates: {@code enableDungeons}. Chance {@code CompatibilityConfig.dudStructure} default
 * overworld {@code 500} (CE {@code 0:500}, {@code 03.11_dudSpawn}). No biome filter
 * ({@code HbmWorldGen.java}:379). Spawn: grass/dirt/stone/sand <b>or sandstone</b>
 * ({@code Dud.java}:30-32). Places one of the four flattened {@code crashed_bomb_*}
 * ids ({@code EnumDudType.VALUES}) flags {@code 2|16}.
 */
public class DudFeature extends Feature<NoneFeatureConfiguration> {

    private static final int FLAGS = 2 | 16;

    public DudFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get()) return false;

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.dudStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return false;

        BlockPos pos = new BlockPos(x, y, z);
        if (!CeStructureSpawn.locationIsValidSpawn(level, pos, true)) return false;

        EnumDudType[] types = EnumDudType.values();
        EnumDudType type = types[random.nextInt(types.length)];
        level.setBlock(pos, dudBlock(type).defaultBlockState(), FLAGS);
        return true;
    }

    private static Block dudBlock(EnumDudType type) {
        return switch (type) {
            case BALEFIRE -> NukeCasingBlocks.CRASHED_BOMB_BALEFIRE.get();
            case CONVENTIONAL -> NukeCasingBlocks.CRASHED_BOMB_CONVENTIONAL.get();
            case NUKE -> NukeCasingBlocks.CRASHED_BOMB_NUKE.get();
            case SALTED -> NukeCasingBlocks.CRASHED_BOMB_SALTED.get();
        };
    }
}
