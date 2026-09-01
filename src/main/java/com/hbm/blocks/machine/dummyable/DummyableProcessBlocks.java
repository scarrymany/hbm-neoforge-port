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
 * CE furnace_combination / blast / rock mill / annihilator / press / rotary furnace / fraction tower /
 * waste_drum / compressor / coker / catalytic cracker / catalytic reformer / hydrotreater /
 * vacuum distill / radiolysis / flare / epress / pyrooven / arc furnace / exposure /
 * ore slopper / turbofan / radgen / hephaestus / wood burner.
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
    public static DeferredBlock<MachineCompressorBlock> MACHINE_COMPRESSOR;
    public static DeferredBlock<MachineCokerBlock> MACHINE_COKER;
    public static DeferredBlock<MachineCatalyticCrackerBlock> MACHINE_CATALYTIC_CRACKER;
    public static DeferredBlock<MachineCatalyticReformerBlock> MACHINE_CATALYTIC_REFORMER;
    public static DeferredBlock<MachineHydrotreaterBlock> MACHINE_HYDROTREATER;
    public static DeferredBlock<MachineVacuumDistillBlock> MACHINE_VACUUM_DISTILL;
    public static DeferredBlock<MachineRadiolysisBlock> MACHINE_RADIOLYSIS;
    public static DeferredBlock<MachineGasFlareBlock> MACHINE_FLARE;
    public static DeferredBlock<MachineEPressBlock> MACHINE_EPRESS;
    public static DeferredBlock<MachinePyroOvenBlock> MACHINE_PYROOVEN;
    public static DeferredBlock<MachineArcFurnaceBlock> MACHINE_ARC_FURNACE;
    public static DeferredBlock<MachineExposureChamberBlock> MACHINE_EXPOSURE_CHAMBER;
    public static DeferredBlock<MachineOreSlopperBlock> MACHINE_ORE_SLOPPER;
    public static DeferredBlock<MachineTurbofanBlock> MACHINE_TURBOFAN;
    public static DeferredBlock<MachineRadGenBlock> MACHINE_RADGEN;
    public static DeferredBlock<MachineHephaestusBlock> MACHINE_HEPHAESTUS;
    public static DeferredBlock<MachineWoodBurnerBlock> MACHINE_WOOD_BURNER;

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
        MACHINE_COMPRESSOR = registerBlock("machine_compressor", () -> new MachineCompressorBlock(MACHINE_PROPS));
        MACHINE_COKER = registerBlock("machine_coker", () -> new MachineCokerBlock(MACHINE_PROPS));
        MACHINE_CATALYTIC_CRACKER = registerBlock("machine_catalytic_cracker", () -> new MachineCatalyticCrackerBlock(MACHINE_PROPS));
        MACHINE_CATALYTIC_REFORMER = registerBlock("machine_catalytic_reformer", () -> new MachineCatalyticReformerBlock(MACHINE_PROPS));
        MACHINE_HYDROTREATER = registerBlock("machine_hydrotreater", () -> new MachineHydrotreaterBlock(MACHINE_PROPS));
        MACHINE_VACUUM_DISTILL = registerBlock("machine_vacuum_distill", () -> new MachineVacuumDistillBlock(MACHINE_PROPS));
        MACHINE_RADIOLYSIS = registerBlock("machine_radiolysis", () -> new MachineRadiolysisBlock(MACHINE_PROPS));
        MACHINE_FLARE = registerBlock("machine_flare", () -> new MachineGasFlareBlock(MACHINE_PROPS));
        MACHINE_EPRESS = registerBlock("machine_epress", () -> new MachineEPressBlock(MACHINE_PROPS));
        MACHINE_PYROOVEN = registerBlock("machine_pyrooven", () -> new MachinePyroOvenBlock(MACHINE_PROPS));
        MACHINE_ARC_FURNACE = registerBlock("machine_arc_furnace", () -> new MachineArcFurnaceBlock(MACHINE_PROPS));
        MACHINE_EXPOSURE_CHAMBER = registerBlock("machine_exposure_chamber", () -> new MachineExposureChamberBlock(MACHINE_PROPS));
        MACHINE_ORE_SLOPPER = registerBlock("machine_ore_slopper", () -> new MachineOreSlopperBlock(MACHINE_PROPS));
        MACHINE_TURBOFAN = registerBlock("machine_turbofan", () -> new MachineTurbofanBlock(MACHINE_PROPS));
        MACHINE_RADGEN = registerBlock("machine_radgen", () -> new MachineRadGenBlock(MACHINE_PROPS));
        MACHINE_HEPHAESTUS = registerBlock("machine_hephaestus", () -> new MachineHephaestusBlock(MACHINE_PROPS));
        MACHINE_WOOD_BURNER = registerBlock("machine_wood_burner", () -> new MachineWoodBurnerBlock(MACHINE_PROPS));
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
