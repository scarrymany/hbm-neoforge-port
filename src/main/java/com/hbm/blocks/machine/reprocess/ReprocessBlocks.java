package com.hbm.blocks.machine.reprocess;

import com.hbm.blockentity.machine.reprocess.ReprocessBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.reprocess.ReprocessMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * PUREX + liquefactor family. CE {@code machine_purex} / {@code machine_liquefactor}.
 * Real TE + menu (auto-detect recipes, no stub GUI).
 */
public final class ReprocessBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<PurexBlock> MACHINE_PUREX;
    public static DeferredBlock<LiquefactorBlock> MACHINE_LIQUEFACTOR;

    private ReprocessBlocks() {
    }

    public static void registerAll() {
        MACHINE_PUREX = registerBlock("machine_purex", () -> new PurexBlock(MACHINE_PROPS));
        MACHINE_LIQUEFACTOR = registerBlock("machine_liquefactor", () -> new LiquefactorBlock(MACHINE_PROPS));
        ReprocessBlockEntities.registerAll();
        ReprocessMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
