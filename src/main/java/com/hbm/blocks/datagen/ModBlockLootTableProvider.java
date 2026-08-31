package com.hbm.blocks.datagen;

import com.hbm.blocks.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Drops every block actually present in {@link ModBlocks#BLOCKS} at datagen time as itself - never
 * a hardcoded id list, so this needs zero coordination with whichever area registers a given block.
 * {@code dropSelf} is correct for the overwhelming majority of Phase 1's "simple blocks" (decorative
 * and storage blocks).
 *
 * <p>{@code com.hbm.blocks.generic.BlockNTMOre}/{@code BlockDepthOre} are ore blocks with a
 * non-{@code dropSelf} runtime drop (their raw-material {@code IOreType} drop function), but they
 * override {@code Block#getDrops(BlockState, LootParams.Builder)}
 * directly and never consult the datapack loot table at all, so the generated {@code dropSelf} entry
 * below is dead in-game for them - it exists purely because {@link BlockLootSubProvider}'s
 * {@link #getKnownBlocks()} validation requires every known block to have *some* generated loot
 * table, on pain of {@link IllegalStateException} at {@code runData} time.
 *
 * <p>Only reachable through a {@link net.minecraft.data.loot.LootTableProvider.SubProviderEntry}
 * factory reference (confirmed real, not an oversight - see {@code com.hbm.datagen.ModDataGenerators}).
 * Constructor is public so {@code ModBlockLootTableProvider::new} is a legal method reference from
 * that other class (javac rejects a {@code protected} ctor used as a cross-package {@code ::new}).
 */
public class ModBlockLootTableProvider extends BlockLootSubProvider {

    public ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        ModBlocks.BLOCKS.getEntries().forEach(holder -> this.dropSelf(holder.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        List<Block> blocks = new ArrayList<>();
        ModBlocks.BLOCKS.getEntries().forEach(holder -> blocks.add(holder.get()));
        return blocks;
    }
}
