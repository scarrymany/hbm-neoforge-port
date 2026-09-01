package com.hbm.blocks.machine.workshop;

import com.hbm.blockentity.machine.workshop.WorkshopBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.workshop.WorkshopMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Ammo press + arc welder + soldering station. CE {@code machine_ammo_press} ({@code ModBlocks.java:993}),
 * {@code machine_arc_welder} ({@code :1059}), {@code machine_soldering_station} ({@code :1061}).
 * Real TE + menu (auto-detect recipes, no stub GUI).
 */
public final class WorkshopBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 10.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<WorkshopBlock> MACHINE_AMMO_PRESS;
    public static DeferredBlock<WorkshopBlock> MACHINE_ARC_WELDER;
    public static DeferredBlock<WorkshopBlock> MACHINE_SOLDERING_STATION;

    private WorkshopBlocks() {
    }

    public static void registerAll() {
        MACHINE_AMMO_PRESS = registerBlock("machine_ammo_press",
                () -> new WorkshopBlock(MACHINE_PROPS, WorkshopBlock.Kind.AMMO_PRESS));
        MACHINE_ARC_WELDER = registerBlock("machine_arc_welder",
                () -> new WorkshopBlock(MACHINE_PROPS, WorkshopBlock.Kind.ARC_WELDER));
        MACHINE_SOLDERING_STATION = registerBlock("machine_soldering_station",
                () -> new WorkshopBlock(MACHINE_PROPS, WorkshopBlock.Kind.SOLDERING));
        WorkshopBlockEntities.registerAll();
        WorkshopMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
