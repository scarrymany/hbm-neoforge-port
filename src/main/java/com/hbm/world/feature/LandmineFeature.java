package com.hbm.world.feature;

import com.hbm.blockentity.bomb.LandmineBlockEntity;
import com.hbm.blocks.bomb.BombBlocks;
import com.hbm.config.CompatibilityConfig;
import com.hbm.config.GeneralConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE AP landmine scatter ({@code HbmWorldGen.java:386-404}).
 * <p>
 * Gates: {@code enableDungeons} + {@code enableMines}. Chance
 * {@code CompatibilityConfig.minefreq} default overworld {@code 64} (CE {@code 0:64},
 * {@code 03.15_landmineSpawn}). Random X/Z in chunk, {@code y = getHeight}, below must
 * be {@code isSideSolid(UP)} → {@code isFaceSturdy(UP)}. Places {@code mine_ap} flags
 * {@code 2|16}, {@code TileEntityLandmine.waitingForPlayer = true}.
 * <p>
 * No biome gate in CE. 528 bosnia HE mines are extra and not this feature
 * TODO(CE: HbmWorldGen.java:411-421).
 */
public class LandmineFeature extends Feature<NoneFeatureConfiguration> {

    private static final int FLAGS = 2 | 16;

    public LandmineFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_DUNGEON_SPAWN.get() || !GeneralConfig.ENABLE_LANDMINE_SPAWN.get()) {
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.minefreq(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        int x = (origin.getX() & ~15) + random.nextInt(16);
        int z = (origin.getZ() & ~15) + random.nextInt(16);
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        BlockPos below = pos.below();
        BlockState ground = level.getBlockState(below);
        if (!ground.isFaceSturdy(level, below, Direction.UP)) return false;

        level.setBlock(pos, BombBlocks.MINE_AP.get().defaultBlockState(), FLAGS);
        if (level.getBlockEntity(pos) instanceof LandmineBlockEntity te) {
            te.waitingForPlayer = true;
            te.setChanged();
        }
        return true;
    }
}
