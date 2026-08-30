package com.hbm.blocks.datagen;

import com.hbm.blocks.ModBlocks;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Tags every block actually present in {@link ModBlocks#BLOCKS} at datagen time as
 * {@link BlockTags#MINEABLE_WITH_PICKAXE} - never a hardcoded id list. This is a deliberately broad
 * default: Phase 1's "simple blocks" scope (ores, decorative stone/metal blocks, storage blocks) is
 * overwhelmingly pickaxe-minable, and vanilla's fallback for an untagged block is hand-breaking
 * only, which is wrong for essentially all of it. A future block that genuinely needs a different
 * (or no) mining-tool tag should be excluded here once such a block exists - none does yet in this
 * port.
 */
public class ModBlockTagProvider extends BlockTagsProvider {

    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, MainRegistry.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxeMineable = this.tag(BlockTags.MINEABLE_WITH_PICKAXE);
        ModBlocks.BLOCKS.getEntries().forEach(holder -> pickaxeMineable.add(holder.get()));
    }
}
