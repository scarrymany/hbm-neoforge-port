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
        // machine_compressor / machine_compressor_compact are DummyableProcessBlocks (live TE).
        // machine_epress is DummyableProcessBlocks (full TE).
        // machine_ore_slopper is DummyableProcessBlocks (full TE).
        // machine_mining_laser is DummyableProcessBlocks (live TE).
        // machine_teleporter / machine_satlink / machine_forcefield / machine_strand_caster /
        // machine_chungus are DummyableProcessBlocks (live TE).
        // machine_assembly_factory / machine_chemical_factory are DummyableProcessBlocks (live TE).
        // machine_turbofan / machine_hephaestus / machine_radgen are DummyableProcessBlocks (full TEs).
        // machine_pyrooven / machine_exposure_chamber are DummyableProcessBlocks (full TEs).
        // machine_fluidtank / machine_bigasstank are DummyableProcessBlocks (live TE).
        // reactor_research / reactor_zirnox are DummyableProcessBlocks (live TE).
        // seal_frame / seal_controller / seal_hatch are SealBlocks (live controller + hatch TE).
        // vitrified_barrel is GenericCrateBlocks (YellowBarrel, idle rad 0.5/5).
        // TODO(CE: MachineFusionTorus.java:1): fusion_* + struct_torus_core stay BlockBase.
        // Blocked by KlystronNetwork/PlasmaNetwork/ModuleMachineFusion. Do not stub.
        // CE fusion_core is BlockBase.
        // watz_element / watz_cooler are CE BlockBase. machine_watz_reactor already live (FusionBlocks).
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
        // machine_orbus is DummyableProcessBlocks (live TE).
        // pile_brick / pile_block are PileBlocks (live conversion + PileChannel sim).
        // nuke_solinium / nuke_fstbmb are NukeCasingBlocks (live TE).
        // turret_arty / turret_himars are TurretBlocks (live Dummyable TE).
        // barrel_steel is DummyableProcessBlocks (live TE).
        // machine_satlinker is DummyableProcessBlocks (live TE, missile tab). ≠ machine_satlink.
        // pump_steam / pump_electric / machine_thresher / chimney_* / bm_power_box
        // are DummyableProcessBlocks (live TE). fluid_duct_exhaust is FluidDuctBlocks.
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
