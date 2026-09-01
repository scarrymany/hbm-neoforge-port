package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.dummyable.DummyableProcessMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Combination oven / blast furnace / rock mill / annihilator — Dummyable machines.
 * CE {@code furnace_combination}, {@code machine_blast_furnace}, {@code machine_rock_mill},
 * {@code machine_annihilator}.
 */
public final class DummyableProcessBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<FurnaceCombinationBlock> FURNACE_COMBINATION;
    public static DeferredBlock<MachineBlastFurnaceBlock> MACHINE_BLAST_FURNACE;
    public static DeferredBlock<MachineRockMillBlock> MACHINE_ROCK_MILL;
    public static DeferredBlock<MachineAnnihilatorBlock> MACHINE_ANNIHILATOR;

    private DummyableProcessBlocks() {
    }

    public static void registerAll() {
        FURNACE_COMBINATION = registerBlock("furnace_combination", () -> new FurnaceCombinationBlock(MACHINE_PROPS));
        MACHINE_BLAST_FURNACE = registerBlock("machine_blast_furnace", () -> new MachineBlastFurnaceBlock(MACHINE_PROPS));
        MACHINE_ROCK_MILL = registerBlock("machine_rock_mill", () -> new MachineRockMillBlock(MACHINE_PROPS));
        MACHINE_ANNIHILATOR = registerBlock("machine_annihilator", () -> new MachineAnnihilatorBlock(MACHINE_PROPS));
        DummyableProcessBlockEntities.registerAll();
        DummyableProcessMenus.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
