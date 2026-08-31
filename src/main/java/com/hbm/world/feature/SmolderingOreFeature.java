package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Group B's netherrack "smoldering ore" scatter, ported from CE's {@code HbmWorldGen.generateOres}
 * nether branch (not an ellipsoid vein at all): unconditional, up to 30 attempts per chunk, each
 * picking a random {@code (x,z)} and depth {@code d in [16,112)}, scanning the 5-block column
 * {@code [d-5,d]} for the first air-above-netherrack transition and replacing that one netherrack
 * block with {@code ore_nether_smoldering}. No config gate in CE at all - preserved that way here;
 * scoped to the Nether purely by which {@code AddFeaturesBiomeModifier} biome-tag group registers it.
 */
public class SmolderingOreFeature extends Feature<NoneFeatureConfiguration> {

    private static final int ATTEMPTS = 30;

    public SmolderingOreFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!OreShapeUtil.dimension(level).equals(Level.NETHER)) return false;

        Block smoldering = OreShapeUtil.block("ore_nether_smoldering");
        if (smoldering == null) return false;
        BlockState smolderingState = smoldering.defaultBlockState();

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = 0; k < ATTEMPTS; k++) {
            int x = chunkMinX + random.nextInt(16);
            int z = chunkMinZ + random.nextInt(16);
            int d = 16 + random.nextInt(96);

            for (int y = d - 5; y <= d; y++) {
                pos.set(x, y + 1, z);
                if (level.getBlockState(pos).isAir()) {
                    pos.set(x, y, z);
                    if (level.getBlockState(pos).is(Blocks.NETHERRACK)) {
                        level.setBlock(pos, smolderingState, 2 | 16);
                        placedAny = true;
                    }
                }
            }
        }
        return placedAny;
    }
}
