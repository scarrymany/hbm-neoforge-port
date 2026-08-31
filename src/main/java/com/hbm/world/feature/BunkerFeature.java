package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Port of CE {@code com.hbm.world.Bunker} (1565 lines). Schematic is the extracted
 * {@code setBlockState} table ({@code Bunker.java}:80-1565) plus the 11×9×15 AIR clear at
 * {@code Bunker.java}:69-78. Spawn: four corners on grass/dirt/stone/sand/sandstone
 * ({@code Bunker.java}:30-45 / {@code isValidSpawnBlock}). No biome gate
 * ({@code HbmWorldGen.java}:378). {@code CompatibilityConfig.bunkerStructure} default 1-in-1000.
 * <p>
 * CE {@code generate_r0} does {@code y += 1} ({@code Bunker.java}:67) before every placement;
 * that +1 is baked into the JSON dy so this Feature's heightmap origin matches
 * {@code PhasedStructureGenerator}'s height snap.
 */
public class BunkerFeature extends Feature<NoneFeatureConfiguration> {

    public BunkerFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.bunkerStructure(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        if (!validCorner(level, origin)
                || !validCorner(level, origin.offset(3, 0, 0))
                || !validCorner(level, origin.offset(3, 0, 5))
                || !validCorner(level, origin.offset(0, 0, 5))) {
            return false;
        }

        // CE Bunker.java:69-78 after y+=1 → relative to surface: (i, j-24, k)
        for (int i = 0; i < 11; i++) {
            for (int j = 0; j < 9; j++) {
                for (int k = 0; k < 15; k++) {
                    level.setBlock(origin.offset(i, j - 24, k), Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        CeSchematicPlacer.place(level, origin, random, "bunker");
        return true;
    }

    /** CE {@code locationIsValidSpawn} + Bunker {@code isValidSpawnBlock} (adds sandstone). */
    private static boolean validCorner(WorldGenLevel level, BlockPos airPos) {
        BlockState above = level.getBlockState(airPos);
        if (!above.isAir()) return false;
        BlockState ground = level.getBlockState(airPos.below());
        return isValidSpawnBlock(ground) || ground.is(Blocks.SANDSTONE);
    }

    private static boolean isValidSpawnBlock(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(BlockTags.DIRT)
                || state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND)
                || state.is(Blocks.SANDSTONE);
    }
}
