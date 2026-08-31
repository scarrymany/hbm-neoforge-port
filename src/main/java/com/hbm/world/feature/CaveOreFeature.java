package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.synth.PerlinNoise;

import javax.annotation.Nullable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BooleanSupplier;
import java.util.stream.IntStream;

/**
 * Group E's {@code OreCave} family, ported from CE's {@code OreCave} (209 lines, read in full): a
 * 2D-noise-driven cave-cavity-boundary decoration - for each XZ column, a noise value above
 * {@code threshold} defines a vertical range around {@code yLevel}; any exposed stone face touching
 * air within that range becomes {@code ore}, and (if {@code stalagmiteTypeSuffix} is set) unrelated
 * nearby air pockets get a chance at a matching {@code stalactite_*}/{@code stalagmite_*} decoration.
 * <p>
 * <b>Deliberate, flagged scope narrowing</b>: CE's sulfur variant additionally carves a paired
 * sulfuric-acid fluid pocket (see CE's {@code fluid}/{@code canGenFluid} branch) when a cavity
 * touches open air. This port has no placeable world fluid block for sulfuric acid yet - a
 * repo-wide grep confirms zero {@code LiquidBlock}/{@code FlowingFluid} subclasses exist anywhere in
 * this port (this mod's own {@code FluidType}/{@code FluidStack} system is an internal
 * tank/pipe-only abstraction, not a placed-in-world block) - so that secondary acid-pool sub-feature
 * is not implemented; see this package's own known-gaps note. The core mechanic (ore-vein
 * replacement plus stalactite/stalagmite decoration) is fully implemented and matches CE's
 * {@code shouldGen} branch exactly.
 */
public class CaveOreFeature extends Feature<NoneFeatureConfiguration> {

    private static final double SCALE = 0.01D;
    private static final ConcurrentMap<Long, PerlinNoise> NOISE_CACHE = new ConcurrentHashMap<>();

    private final int id;
    private final double threshold;
    private final int rangeMult;
    private final int maxRange;
    private final int yLevel;
    private final String oreBlockName;
    @Nullable
    private final String stalagmiteTypeSuffix;
    private final BooleanSupplier enabledSupplier;

    public CaveOreFeature(Codec<NoneFeatureConfiguration> codec, int id, double threshold, int rangeMult,
                           int maxRange, int yLevel, String oreBlockName, @Nullable String stalagmiteTypeSuffix,
                           BooleanSupplier enabledSupplier) {
        super(codec);
        this.id = id;
        this.threshold = threshold;
        this.rangeMult = rangeMult;
        this.maxRange = maxRange;
        this.yLevel = yLevel;
        this.oreBlockName = oreBlockName;
        this.stalagmiteTypeSuffix = stalagmiteTypeSuffix;
        this.enabledSupplier = enabledSupplier;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!enabledSupplier.getAsBoolean()) return false;

        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        if (!OreShapeUtil.dimension(level).equals(Level.OVERWORLD)) return false;

        Block ore = OreShapeUtil.block(oreBlockName);
        if (ore == null) return false;
        BlockState oreState = ore.defaultBlockState();

        Block stalactite = stalagmiteTypeSuffix != null ? OreShapeUtil.block("stalactite_" + stalagmiteTypeSuffix) : null;
        Block stalagmite = stalagmiteTypeSuffix != null ? OreShapeUtil.block("stalagmite_" + stalagmiteTypeSuffix) : null;
        boolean canDecorateSpikes = stalactite != null && stalagmite != null;

        long seed = OreShapeUtil.seed(level);
        PerlinNoise noise = NOISE_CACHE.computeIfAbsent(seed * 31L + id,
                k -> PerlinNoise.create(RandomSource.create(seed + id * 31L + yLevel), IntStream.rangeClosed(-1, 0)));

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        int minBuild = level.getMinBuildHeight();
        int maxBuild = level.getMaxBuildHeight() - 1;

        boolean placedAny = false;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = chunkMinX; x < chunkMinX + 16; x++) {
            for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
                double n = noise.getValue(x * SCALE, 0, z * SCALE);
                if (n <= threshold) continue;

                int range = (int) ((n - threshold) * rangeMult);
                if (range > maxRange) range = (maxRange * 2) - range;
                if (range < 0) continue;

                int yMin = Math.max(minBuild, yLevel - range);
                int yMax = Math.min(maxBuild, yLevel + range);

                for (int y = yMin; y <= yMax; y++) {
                    pos.set(x, y, z);
                    BlockState genState = level.getBlockState(pos);

                    if (genState.is(Blocks.STONE)) {
                        boolean shouldGen = false;
                        for (Direction dir : Direction.values()) {
                            BlockState neighborState = level.getBlockState(pos.relative(dir));
                            if (neighborState.isAir() || (stalagmite != null && neighborState.is(stalagmite))) {
                                shouldGen = true;
                                break;
                            }
                        }
                        if (shouldGen) {
                            level.setBlock(pos, oreState, 2 | 16);
                            placedAny = true;
                        }
                        continue;
                    }

                    if (canDecorateSpikes && (genState.isAir() || !genState.isSolidRender(level, pos))
                            && genState.getFluidState().isEmpty() && random.nextInt(5) == 0) {
                        BlockPos immutable = pos.immutable();
                        BlockState stalactiteState = stalactite.defaultBlockState();
                        BlockState stalagmiteState = stalagmite.defaultBlockState();
                        if (stalactiteState.canSurvive(level, immutable)) {
                            level.setBlock(immutable, stalactiteState, 2 | 16);
                            placedAny = true;
                        } else if (stalagmiteState.canSurvive(level, immutable)) {
                            level.setBlock(immutable, stalagmiteState, 2 | 16);
                            placedAny = true;
                        }
                    }
                }
            }
        }
        return placedAny;
    }
}
