package com.hbm.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import javax.annotation.Nullable;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Group A ("N attempts per chunk" ellipsoid ore veins) <b>and</b> Group C's per-ore gneiss-vein
 * pass, per docs/phase4/ore_veins_and_bedrock_ores.md - both are the exact same
 * {@code DungeonToolbox.generateOre}/{@code WorldGenMinableNonCascade} shape in CE, differing only
 * in which block gets targeted (plain stone/netherrack/end stone for Group A, {@code stone_gneiss}
 * for Group C's eight gneiss ores), so one Feature class covers both, parametrized by
 * {@code targetSupplier}.
 * <p>
 * {@code veinCountFn} is re-evaluated fresh on every {@link #place}, so a server operator's live
 * edit to a {@code CompatibilityConfig}/{@code GeneralConfig} spawn-rate value takes effect without
 * regenerating datapack JSON, per the report's Key design decision (fork A). {@code targetSupplier}
 * is a plain {@link Supplier} rather than a by-name lookup because every real CE target is a vanilla
 * block ({@code Blocks.STONE}/{@code NETHERRACK}/{@code END_STONE}, always safe to reference
 * directly - vanilla's own registry bootstraps long before any mod code runs) except
 * {@code stone_gneiss}, whose supplier lazily resolves via {@link OreShapeUtil#block}.
 */
public class EllipsoidOreFeature extends Feature<NoneFeatureConfiguration> {

    private final ToIntFunction<ResourceKey<Level>> veinCountFn;
    private final String oreBlockName;
    private final int amount;
    private final int minHeight;
    private final int variance;
    @Nullable
    private final Supplier<Block> targetSupplier;

    public EllipsoidOreFeature(Codec<NoneFeatureConfiguration> codec, ToIntFunction<ResourceKey<Level>> veinCountFn,
                                String oreBlockName, int amount, int minHeight, int variance,
                                @Nullable Supplier<Block> targetSupplier) {
        super(codec);
        this.veinCountFn = veinCountFn;
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

        int veinCount = veinCountFn.applyAsInt(dim);
        if (veinCount <= 0) return false;

        Block ore = OreShapeUtil.block(oreBlockName);
        if (ore == null) return false;
        Block target = targetSupplier != null ? targetSupplier.get() : Blocks.STONE;
        if (target == null) return false;

        int chunkMinX = OreShapeUtil.chunkOrigin(origin.getX());
        int chunkMinZ = OreShapeUtil.chunkOrigin(origin.getZ());
        BlockState oreState = ore.defaultBlockState();

        boolean placedAny = false;
        for (int i = 0; i < veinCount; i++) {
            int x = chunkMinX + random.nextInt(16);
            int y = minHeight + (variance > 0 ? random.nextInt(variance) : 0);
            int z = chunkMinZ + random.nextInt(16);
            placedAny |= OreShapeUtil.placeEllipsoidVein(level, random, x, y, z, amount, oreState, target);
        }
        return placedAny;
    }
}
