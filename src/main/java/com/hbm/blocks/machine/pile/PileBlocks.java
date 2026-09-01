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
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * CE {@code ModBlocks.java:664-666}. {@code pile_brick} stays in the machine tab;
 * {@code pile_block} creative tab is null (formed in-world only).
 * {@code pile_device} is one block + 3 flattened items (CE metas 0/1/2).
 */
public final class PileBlocks {

    public static DeferredBlock<BlockPileBrick> PILE_BRICK;
    public static DeferredBlock<BlockPile> PILE_BLOCK;
    public static DeferredBlock<BlockPileDevice> PILE_DEVICE;
    public static DeferredItem<Item> PILE_DEVICE_LOADER;
    public static DeferredItem<Item> PILE_DEVICE_VENT;
    public static DeferredItem<Item> PILE_DEVICE_CONTROL;

    private PileBlocks() {
    }

    public static void registerAll() {
        PILE_BRICK = registerBlock("pile_brick", () -> new BlockPileBrick(
                BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
        // CE ModBlocks.java:666 hardness 15 / resistance 10, tab null.
        PILE_BLOCK = registerBlockNoTab("pile_block", () -> new BlockPile(
                BlockBehaviour.Properties.of().strength(15.0F, 10.0F).sound(SoundType.METAL)
                        .requiresCorrectToolForDrops().noLootTable()));
        // CE ModBlocks.java:665 hardness 5 / 10, machine tab. IBlockMulti metas 0/1/2.
        PILE_DEVICE = ModBlocks.BLOCKS.register("pile_device", () -> new BlockPileDevice(
                BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL)
                        .requiresCorrectToolForDrops().noOcclusion()));
        PILE_DEVICE_LOADER = ModItems.ITEMS.register("pile_device",
                () -> new PileDeviceItem(PILE_DEVICE.get(), BlockPileDevice.ITEM_META_LOADER, new Item.Properties()));
        PILE_DEVICE_VENT = ModItems.ITEMS.register("pile_device_1",
                () -> new PileDeviceItem(PILE_DEVICE.get(), BlockPileDevice.ITEM_META_VENT, new Item.Properties()));
        PILE_DEVICE_CONTROL = ModItems.ITEMS.register("pile_device_2",
                () -> new PileDeviceItem(PILE_DEVICE.get(), BlockPileDevice.ITEM_META_CONTROL, new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, PILE_DEVICE_LOADER);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, PILE_DEVICE_VENT);
        CreativeTabContents.add(ModCreativeTabs.MACHINE, PILE_DEVICE_CONTROL);
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
