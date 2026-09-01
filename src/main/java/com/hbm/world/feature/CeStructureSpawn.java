package com.hbm.world.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * CE {@code AbstractPhasedStructure.locationIsValidSpawn} ({@code AbstractPhasedStructure.java:110-128})
 * plus the sandstone OR used by Dud/Barrel/Bunker.
 */
public final class CeStructureSpawn {

    private CeStructureSpawn() {
    }

    public static boolean locationIsValidSpawn(WorldGenLevel level, BlockPos airPos, boolean sandstone) {
        return locationIsValidSpawn(level, airPos, sandstone, false);
    }

    /**
     * {@code sandstone} = Dud/Barrel/Bunker/Spaceship/Satellite extra.
     * {@code terracotta} = DesertAtom {@code HARDENED_CLAY}/{@code STAINED_HARDENED_CLAY}
     * ({@code DesertAtom001.java}:73-75).
     */
    public static boolean locationIsValidSpawn(WorldGenLevel level, BlockPos airPos, boolean sandstone, boolean terracotta) {
        // Only the WorldGenRegion. ServerLevel.getBlockState during FEATURES
        // pulls neighbor protochunks and cascades at forced 1/1.
        if (!level.hasChunk(airPos.getX() >> 4, airPos.getZ() >> 4)) return false;
        return isValidColumn(level, airPos, sandstone, terracotta);
    }

    private static boolean isValidColumn(BlockGetter level, BlockPos airPos, boolean sandstone, boolean terracotta) {
        if (!level.getBlockState(airPos).isAir()) return false;
        BlockState ground = level.getBlockState(airPos.below());
        if (isValidSpawnBlock(ground, sandstone, terracotta)) return true;
        BlockState below = level.getBlockState(airPos.below(2));
        if (ground.is(Blocks.SNOW) && isValidSpawnBlock(below, sandstone, terracotta)) return true;
        return (ground.is(BlockTags.REPLACEABLE) || ground.is(BlockTags.SMALL_FLOWERS) || ground.is(BlockTags.SAPLINGS))
                && isValidSpawnBlock(below, sandstone, terracotta);
    }

    public static boolean isValidSpawnBlock(BlockState state, boolean sandstone) {
        return isValidSpawnBlock(state, sandstone, false);
    }

    public static boolean isValidSpawnBlock(BlockState state, boolean sandstone, boolean terracotta) {
        boolean base = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(BlockTags.DIRT)
                || state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
        if (base) return true;
        if (sandstone && (state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE))) return true;
        return terracotta && (state.is(Blocks.TERRACOTTA) || state.is(BlockTags.TERRACOTTA));
    }
}
