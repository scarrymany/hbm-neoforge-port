package com.hbm.blocks.machine;

import com.hbm.blockentity.machine.SealBlockEntities;
import com.hbm.blocks.BlockBase;
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
 * CE {@code ModBlocks.java:884-886}. {@code seal_frame} stays BlockBase (CE has no TE).
 * {@code seal_controller} is {@link BlockSeal} (no TE). {@code seal_hatch} + {@code TileEntityHatch}.
 */
public final class SealBlocks {

    private static final BlockBehaviour.Properties SEAL_PROPS =
            BlockBehaviour.Properties.of().strength(10.0F, 100.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    private static final BlockBehaviour.Properties HATCH_PROPS =
            BlockBehaviour.Properties.of().strength(-1.0F, Float.POSITIVE_INFINITY).sound(SoundType.METAL);

    public static DeferredBlock<BlockBase> SEAL_FRAME;
    public static DeferredBlock<BlockSeal> SEAL_CONTROLLER;
    public static DeferredBlock<BlockHatch> SEAL_HATCH;

    private SealBlocks() {
    }

    public static void registerAll() {
        SEAL_FRAME = registerBlock("seal_frame", () -> new BlockBase(SEAL_PROPS));
        SEAL_CONTROLLER = registerBlock("seal_controller", () -> new BlockSeal(SEAL_PROPS));
        // CE creative tab null — BlockItem still registered for /give and pick-block.
        SEAL_HATCH = registerBlockNoTab("seal_hatch", () -> new BlockHatch(HATCH_PROPS));
        SealBlockEntities.registerAll();
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
