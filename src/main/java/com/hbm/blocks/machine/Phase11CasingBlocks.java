package com.hbm.blocks.machine;

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
 * Leftover assembler output casings (no TE). Unblocks AssemblyMachineRecipes leftovers
 * named in {@code docs/phase11/PARITY_REPORT.md} (arc furnace / supercomputer / compressors /
 * satlink / teleporter / …).
 * <p>
 * CE: {@code ModBlocks.java:1086-1087} compressor pair, {@code :1186} teleporter,
 * {@code :1219} arc furnace, {@code :1232} supercomputer, {@code :1238} satlink.
 */
public final class Phase11CasingBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    private Phase11CasingBlocks() {
    }

    public static void registerAll() {
        // machine_supercomputer is DummyableProcessBlocks (full TE).
        // machine_arc_furnace is DummyableProcessBlocks (full TE).
        // CE ModBlocks.java:1086-1087 / AssemblyMachineRecipes.java:320-323
        // machine_compressor is DummyableProcessBlocks (full TE). Compact stays a casing.
        registerBlock("machine_compressor_compact", () -> new BlockBase(MACHINE_PROPS));
        // machine_epress is DummyableProcessBlocks (full TE).
        // machine_ore_slopper is DummyableProcessBlocks (full TE).
        registerBlock("machine_mining_laser", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:1186 / AssemblyMachineRecipes.java:362
        registerBlock("machine_teleporter", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:1238 / AssemblyMachineRecipes.java:367
        registerBlock("machine_satlink", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("machine_forcefield", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("machine_strand_caster", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("machine_assembly_factory", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("machine_chemical_factory", () -> new BlockBase(MACHINE_PROPS));
        // machine_turbofan / machine_hephaestus / machine_radgen are DummyableProcessBlocks (full TEs).
        registerBlock("machine_chungus", () -> new BlockBase(MACHINE_PROPS));
        // machine_pyrooven / machine_exposure_chamber are DummyableProcessBlocks (full TEs).
        registerBlock("machine_fluidtank", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("machine_bigasstank", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("reactor_research", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("reactor_zirnox", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("seal_frame", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("seal_controller", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("vitrified_barrel", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("struct_torus_core", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("fusion_klystron", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("fusion_collector", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("fusion_breeder", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("fusion_boiler", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("fusion_mhdt", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("fusion_coupler", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("watz_element", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("watz_cooler", () -> new BlockBase(MACHINE_PROPS));
        // machine_condenser_powered is DummyableProcessBlocks (live TE).
        // CE ModBlocks.java:803 / ass.orbus
        registerBlock("machine_orbus", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:664 / ass.pileblock
        registerBlock("pile_brick", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:706 / :711 — leftover nuke casings (no TE)
        registerBlock("nuke_solinium", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("nuke_fstbmb", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:821-822 — Arty/HIMARS out of TurretBlocks TE scope
        registerBlock("turret_arty", () -> new BlockBase(MACHINE_PROPS));
        registerBlock("turret_himars", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:789 / ass.fritz
        registerBlock("barrel_steel", () -> new BlockBase(MACHINE_PROPS));
        // CE ModBlocks.java:958 / CraftingManager.java:647 (vanilla satlinker, ≠ machine_satlink)
        registerBlock("machine_satlinker", () -> new BlockBase(MACHINE_PROPS));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
