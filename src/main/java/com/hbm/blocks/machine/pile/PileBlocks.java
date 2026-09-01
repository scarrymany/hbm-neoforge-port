package com.hbm.blocks.machine.pile;

import com.hbm.blockentity.machine.pile.PileBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * CE {@code ModBlocks.java:664-666}. {@code pile_brick} stays in the machine tab;
 * {@code pile_block} creative tab is null (formed in-world only).
 */
public final class PileBlocks {

    public static DeferredBlock<BlockPileBrick> PILE_BRICK;
    public static DeferredBlock<BlockPile> PILE_BLOCK;

    private PileBlocks() {
    }

    public static void registerAll() {
        PILE_BRICK = registerBlock("pile_brick", () -> new BlockPileBrick(
                BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        // CE ModBlocks.java:666 hardness 15 / resistance 10, tab null.
        PILE_BLOCK = registerBlockNoTab("pile_block", () -> new BlockPile(
                BlockBehaviour.Properties.of().strength(15.0F, 10.0F).sound(SoundType.METAL)
                        .requiresCorrectToolForDrops().noLootTable()));
        PileBlockEntities.registerAll();
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = registerBlockNoTab(name, factory);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }

    private static <T extends Block> DeferredBlock<T> registerBlockNoTab(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
