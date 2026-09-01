package com.hbm.world.feature;

import com.hbm.config.GeneralConfig;
import com.hbm.itempool.ItemPool;
import com.hbm.itempool.ItemPoolsSingle;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * CE NITAN powder chests ({@code HbmWorldGen.java:652-686} + {@code generateNitanChest:744-753}).
 * <p>
 * Gate: {@code GeneralConfig.enableNITAN} only — <em>not</em> {@code enableDungeons}.
 * Eight fixed coords at y=250: the four axis points and four corners of
 * {@code ±10000}. Air-only. Vanilla chest, {@code POOL_POWDER} × 29 rolls
 * (CE {@code WeightedRandomChestContentFrom1710.generateChestContents}).
 * No biome filter in CE; this Feature is attached to the overworld oil-meteor
 * modifier (same pipeline as Sellafield).
 */
public class NitanChestFeature extends Feature<NoneFeatureConfiguration> {

    private static final int FLAGS = 2 | 16;
    private static final int ROLLS = 29;
    private static final BlockPos[] SITES = {
            new BlockPos(10000, 250, 10000),
            new BlockPos(0, 250, 10000),
            new BlockPos(-10000, 250, 10000),
            new BlockPos(10000, 250, 0),
            new BlockPos(-10000, 250, 0),
            new BlockPos(10000, 250, -10000),
            new BlockPos(0, 250, -10000),
            new BlockPos(-10000, 250, -10000),
    };

    public NitanChestFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if (!GeneralConfig.ENABLE_NITAN_CHEST_SPAWN.get()) return false;

        WorldGenLevel level = context.level();
        ChunkPos chunk = new ChunkPos(context.origin());
        RandomSource random = context.random();
        boolean placed = false;
        for (BlockPos site : SITES) {
            if (!new ChunkPos(site).equals(chunk)) continue;
            if (generateNitanChest(level, random, site)) {
                placed = true;
            }
        }
        return placed;
    }

    /** CE {@code HbmWorldGen.generateNitanChest} ({@code :744-753}). */
    private static boolean generateNitanChest(WorldGenLevel level, RandomSource random, BlockPos pos) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlock(pos, Blocks.CHEST.defaultBlockState(), FLAGS);
        if (!(level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity chest)) return true;
        ItemPool pool = ItemPool.getPool(ItemPoolsSingle.POOL_POWDER);
        int slots = chest.getContainerSize();
        if (slots <= 0) return true;
        for (int i = 0; i < ROLLS; i++) {
            ItemStack stack = ItemPool.getStack(pool, random);
            if (stack.isEmpty()) continue;
            chest.setItem(random.nextInt(slots), stack);
        }
        return true;
    }
}
