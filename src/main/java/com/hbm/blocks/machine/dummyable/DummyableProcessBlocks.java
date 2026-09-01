package com.hbm.blocks.machine.dummyable;

import com.hbm.blockentity.machine.dummyable.DummyableProcessBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.WasteDrumBlock;
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
 * Dummyable process machines + waste drum.
 * CE furnace_combination / blast / rock mill / annihilator / press / rotary furnace / fraction tower / waste_drum.
 */
public final class DummyableProcessBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<FurnaceCombinationBlock> FURNACE_COMBINATION;
    public static DeferredBlock<MachineBlastFurnaceBlock> MACHINE_BLAST_FURNACE;
    public static DeferredBlock<MachineRockMillBlock> MACHINE_ROCK_MILL;
    public static DeferredBlock<MachineAnnihilatorBlock> MACHINE_ANNIHILATOR;
    public static DeferredBlock<MachinePressBlock> MACHINE_PRESS;
    public static DeferredBlock<MachineRotaryFurnaceBlock> MACHINE_ROTARY_FURNACE;
    public static DeferredBlock<MachineFractionTowerBlock> MACHINE_FRACTION_TOWER;
    public static DeferredBlock<WasteDrumBlock> WASTE_DRUM;

    private DummyableProcessBlocks() {
    }

    public static void registerAll() {
        FURNACE_COMBINATION = registerBlock("furnace_combination", () -> new FurnaceCombinationBlock(MACHINE_PROPS));
        MACHINE_BLAST_FURNACE = registerBlock("machine_blast_furnace", () -> new MachineBlastFurnaceBlock(MACHINE_PROPS));
        MACHINE_ROCK_MILL = registerBlock("machine_rock_mill", () -> new MachineRockMillBlock(MACHINE_PROPS));
        MACHINE_ANNIHILATOR = registerBlock("machine_annihilator", () -> new MachineAnnihilatorBlock(MACHINE_PROPS));
        MACHINE_PRESS = registerBlock("machine_press", () -> new MachinePressBlock(MACHINE_PROPS));
        MACHINE_ROTARY_FURNACE = registerBlock("machine_rotary_furnace", () -> new MachineRotaryFurnaceBlock(MACHINE_PROPS));
        MACHINE_FRACTION_TOWER = registerBlock("machine_fraction_tower", () -> new MachineFractionTowerBlock(MACHINE_PROPS));
        WASTE_DRUM = registerBlock("machine_waste_drum", () -> new WasteDrumBlock(MACHINE_PROPS));
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
