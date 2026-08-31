package com.hbm.world.feature;

import com.hbm.config.CompatibilityConfig;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Ported from CE's {@code com.hbm.world.feature.BedrockOilDeposit} (80 lines, full read), triggered
 * in CE by {@code HbmWorldGen.generateBedrockOil} (lines 312-319) - a deep, tap-only (never depletes)
 * oil reserve baked into the bedrock layer. See
 * docs/phase4/worldgen_oil_and_meteor_dungeons.md Part 1.
 * <p>
 * CE's diamond region is anchored at the literal bedrock floor {@code y=0..4} - 1.12's Overworld has
 * one perfectly flat bedrock layer there. Modern 1.21.1's Overworld floor instead sits near
 * {@code level.getMinBuildHeight()} (-64) with a partially noise-randomized bedrock/stone boundary,
 * not one flat layer; this substitutes {@code level.getMinBuildHeight() + dy} for CE's literal
 * {@code dy} - the same adaptation neo-edition's own confirmed-real {@code BedrockOilDepositFeature}
 * already makes.
 * <p>
 * {@code stone_porous}'s companion vein (CE also calls
 * {@code DungeonToolbox.generateOre(..., ModBlocks.stone_porous)} from the same site) is skipped -
 * {@code stone_porous}/{@code BlockPorous} is not registered anywhere in this port yet (confirmed by
 * repo-wide grep); see this package's own knownGaps rather than blocking on it.
 */
public class BedrockOilDepositFeature extends Feature<NoneFeatureConfiguration> {

    private static final int DXZ_LIMIT = 4;
    private static final int MAX_DY = 4;
    private static final int MANHATTAN_LIMIT = 6;
    private static final int OIL_SPOT_RADIUS = 5;
    private static final int OIL_SPOT_COUNT = 50;

    public BedrockOilDepositFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        ResourceKey<Level> dimension = OreShapeUtil.dimension(level);
        int rate = CompatibilityConfig.forDimension(CompatibilityConfig.bedrockOilSpawn(), dimension);
        if (rate <= 0 || random.nextInt(rate) != 0) return false;

        Block oreBedrockOil = OreShapeUtil.block("ore_bedrock_oil");
        if (oreBedrockOil == null) return false;
        var oreState = oreBedrockOil.defaultBlockState();

        int centerX = origin.getX();
        int centerZ = origin.getZ();
        int minY = level.getMinBuildHeight();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int dx = -DXZ_LIMIT; dx <= DXZ_LIMIT; dx++) {
            for (int dy = 0; dy <= MAX_DY; dy++) {
                for (int dz = -DXZ_LIMIT; dz <= DXZ_LIMIT; dz++) {
                    if (Math.abs(dx) + dy + Math.abs(dz) <= MANHATTAN_LIMIT) {
                        pos.set(centerX + dx, minY + dy, centerZ + dz);
                        if (level.getBlockState(pos).is(Blocks.BEDROCK)) {
                            level.setBlock(pos, oreState, 2 | 16);
                            placed = true;
                        }
                    }
                }
            }
        }

        if (placed) {
            // Pass WorldGenLevel, not ServerLevel: Level#getHeight during Feature#place deadlocks
            // via ServerChunkCache.getChunk (CE OilSpot.java uses World.getHeight which is fine on 1.12).
            OilSpot.generateOilSpot(level, centerX, centerZ, OIL_SPOT_RADIUS, OIL_SPOT_COUNT, true);
        }
        return placed;
    }
}
