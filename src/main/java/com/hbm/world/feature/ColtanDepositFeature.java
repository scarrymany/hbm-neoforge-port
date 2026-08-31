package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.function.BooleanSupplier;

/**
 * Group B's coltan rich deposit, ported from CE's {@code HbmWorldGen.generateOres} overworld
 * branch (gated by {@code GeneralConfig.enable528ColtanDeposit}): a Gaussian-scattered hotspot
 * centered at {@code (1500*N(0,1), 1500*N(0,1))} from a {@code world.getSeed()+5}-seeded RNG, 2
 * outer passes x 5 concentric rings ({@code colRange/r} for r=1..5), each rolling one 4-block
 * {@code ore_coltan} vein if the current chunk's random sample point falls inside that ring - a
 * genuinely different mechanic from every other entry in the report's tables (fixed-seed-derived
 * world-coordinate hotspot, not per-chunk-independent).
 * <p>
 * CE recomputes {@code colX}/{@code colZ} fresh every single chunk (a brand new
 * {@code new Random(seed+5)} each call, not cached) - cheap (one RNG construction plus two
 * {@code nextGaussian} calls) and reproduced verbatim here rather than cached, to stay behaviorally
 * identical.
 */
public class ColtanDepositFeature extends Feature<NoneFeatureConfiguration> {

    private static final int COL_RANGE = 750;
    private static final int OUTER_PASSES = 2;
    private static final int RINGS = 5;

    private final BooleanSupplier enabledSupplier;

    public ColtanDepositFeature(Codec<NoneFeatureConfiguration> codec, BooleanSupplier enabledSupplier) {
        super(codec);
        this.enabledSupplier = enabledSupplier;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!enabledSupplier.getAsBoolean()) return false;

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!OreShapeUtil.dimension(level).equals(Level.OVERWORLD)) return false;

        Block coltan = OreShapeUtil.block("ore_coltan");
        if (coltan == null) return false;

        long seed = OreShapeUtil.seed(level);
        RandomSource colRand = RandomSource.create(seed + 5L);
        int colX = (int) (colRand.nextGaussian() * 1500);
        int colZ = (int) (colRand.nextGaussian() * 1500);

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());

        boolean placedAny = false;
        for (int k = 0; k < OUTER_PASSES; k++) {
            for (int r = 1; r <= RINGS; r++) {
                int randPosX = chunkMinX + random.nextInt(16);
                int randPosY = random.nextInt(25) + 15;
                int randPosZ = chunkMinZ + random.nextInt(16);
                int range = COL_RANGE / r;

                if (randPosX <= colX + range && randPosX >= colX - range
                        && randPosZ <= colZ + range && randPosZ >= colZ - range) {
                    placedAny |= OreShapeUtil.placeEllipsoidVein(level, random, randPosX, randPosY, randPosZ,
                            4, coltan.defaultBlockState(), Blocks.STONE);
                }
            }
        }
        return placedAny;
    }
}
