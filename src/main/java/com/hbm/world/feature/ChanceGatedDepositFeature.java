package com.hbm.world.feature;

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

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Group B ("1-in-N chunks" chance-gated single deposits), per
 * docs/phase4/ore_veins_and_bedrock_ores.md: {@code gas_flammable}/{@code gas_explosive} bubbles
 * and {@code ore_alexandrite}. Reuses the exact same {@link OreShapeUtil#placeEllipsoidVein} shape
 * as {@link EllipsoidOreFeature}, but is preceded by a single {@code random.nextInt(N) == 0} roll
 * (CE: {@code if (rand.nextInt(dimSpawn) == 0) DungeonToolbox.generateOre(..., 1, amount, ...)})
 * instead of looping {@code N} independent attempts. {@code chanceFn} returning {@code <= 0} means
 * "disabled" (folds in CE's own {@code GeneralConfig.enableFlammableGas}/{@code enableExplosiveGas}
 * boolean gates alongside the numeric spawn-rate map).
 */
public class ChanceGatedDepositFeature extends Feature<NoneFeatureConfiguration> {

    private final ToIntFunction<ResourceKey<Level>> chanceFn;
    private final String oreBlockName;
    private final int amount;
    private final int minHeight;
    private final int variance;
    @Nullable
    private final Supplier<Block> targetSupplier;

    public ChanceGatedDepositFeature(Codec<NoneFeatureConfiguration> codec, ToIntFunction<ResourceKey<Level>> chanceFn,
                                      String oreBlockName, int amount, int minHeight, int variance,
                                      @Nullable Supplier<Block> targetSupplier) {
        super(codec);
        this.chanceFn = chanceFn;
        this.oreBlockName = oreBlockName;
        this.amount = amount;
        this.minHeight = minHeight;
        this.variance = variance;
        this.targetSupplier = targetSupplier;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        ResourceKey<Level> dim = OreShapeUtil.dimension(level);

        int chance = chanceFn.applyAsInt(dim);
        if (chance <= 0 || random.nextInt(chance) != 0) return false;

        Block ore = OreShapeUtil.block(oreBlockName);
        if (ore == null) return false;
        Block target = targetSupplier != null ? targetSupplier.get() : Blocks.STONE;
        if (target == null) return false;

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        int x = chunkMinX + random.nextInt(16);
        int y = minHeight + (variance > 0 ? random.nextInt(variance) : 0);
        int z = chunkMinZ + random.nextInt(16);

        return OreShapeUtil.placeEllipsoidVein(level, random, x, y, z, amount, ore.defaultBlockState(), target);
    }
}
