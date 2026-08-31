package com.hbm.blocks.generic;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Ported from CE's {@code BlockNTMDirt extends net.minecraft.block.BlockDirt}: a purely visual
 * dirt reskin (registered by CE with {@code setCreativeTab(null)}, i.e. not obtainable from the
 * creative menu). Vanilla's 1.12 {@code BlockDirt} carried a dirt/coarse-dirt/podzol metadata
 * variant enum that CE never actually used here (only ever placed at meta 0). Modern vanilla has
 * no dedicated {@code DirtBlock}/{@code CoarseDirtBlock}/{@code PodzolBlock} classes either -
 * {@code Blocks.DIRT} and {@code Blocks.COARSE_DIRT} are plain {@link Block}s in Mojang mappings
 * (only the snow-overlay-aware members of the dirt family, grass and mycelium, get a dedicated
 * {@code SnowyDirtBlock} subclass) - so extending plain {@link Block} directly, exactly like this
 * package's sibling {@link BlockDirt}, is the faithful equivalent of CE's single always-meta-0
 * usage.
 */
public class BlockNTMDirt extends Block {

    public BlockNTMDirt(Properties properties) {
        super(properties);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Blocks.DIRT));
    }
}
