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
        if (isValidColumn(level, airPos, sandstone)) return true;
        // Spaceship 13×24 / satellite 25×31 corners leave the WorldGenRegion.
        // CE IWorldGenerator saw populated neighbors; Feature decoration does not.
        var server = level.getLevel();
        if (!server.hasChunk(airPos.getX() >> 4, airPos.getZ() >> 4)) return false;
        return isValidColumn(server, airPos, sandstone);
    }

    private static boolean isValidColumn(BlockGetter level, BlockPos airPos, boolean sandstone) {
        if (!level.getBlockState(airPos).isAir()) return false;
        BlockState ground = level.getBlockState(airPos.below());
        if (isValidSpawnBlock(ground, sandstone)) return true;
        BlockState below = level.getBlockState(airPos.below(2));
        if (ground.is(Blocks.SNOW) && isValidSpawnBlock(below, sandstone)) return true;
        return (ground.is(BlockTags.REPLACEABLE) || ground.is(BlockTags.SMALL_FLOWERS) || ground.is(BlockTags.SAPLINGS))
                && isValidSpawnBlock(below, sandstone);
    }

    public static boolean isValidSpawnBlock(BlockState state, boolean sandstone) {
        boolean base = state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(BlockTags.DIRT)
                || state.is(Blocks.STONE) || state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
        if (base) return true;
        return sandstone && (state.is(Blocks.SANDSTONE) || state.is(Blocks.RED_SANDSTONE));
    }
}
