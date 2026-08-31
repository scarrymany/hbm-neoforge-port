package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

/**
 * Registers CE's {@code ModBlocks.fallout} ("ash" carpet, {@link BlockFallout}) - see that class's
 * own javadoc for why this is a distinct registration from CE's similarly-named but unrelated
 * {@code ModBlocks.block_fallout} (a {@code BlockHazardFalling} instance, not this area's concern).
 * Follows the same {@code registerBlock} helper shape as {@link WastelandVirusBlocks}/
 * {@link PlantBlocks} - a plain {@link BlockItem} paired 1:1 with the block, collapsing CE's own
 * separate {@code ModItems.fallout} material-item registration into this port's standard pattern
 * (see {@link BlockFallout}'s javadoc point 2).
 */
public final class FalloutBlocks {

    public static DeferredBlock<BlockFallout> FALLOUT;

    private FalloutBlocks() {
    }

    public static void registerAll() {
        FALLOUT = ModBlocks.BLOCKS.register("fallout", () ->
                new BlockFallout(BlockBehaviour.Properties.of().strength(0.1F).sound(SoundType.GRAVEL).noOcclusion()));
        ModItems.ITEMS.register("fallout", () -> new BlockItem(FALLOUT.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.RESOURCE, FALLOUT);
    }
}
