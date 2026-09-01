package com.hbm.blocks.machine.fusion;

import com.hbm.blockentity.machine.fusion.FusionBlockEntities;
import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.container.machine.fusion.FusionMenus;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * ICF/Watz + live tokamak (CE {@code MachineFusion*}).
 */
public final class FusionBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<IcfReactorBlock> ICF_REACTOR;
    public static DeferredBlock<IcfControllerBlock> ICF_CONTROLLER;
    public static DeferredBlock<IcfPressBlock> ICF_PRESS;
    public static DeferredBlock<WatzReactorBlock> WATZ_REACTOR;
    public static DeferredBlock<PlasmaForgeBlock> FUSION_PLASMA_FORGE;

    public static DeferredBlock<MachineFusionTorusBlock> FUSION_TORUS;
    public static DeferredBlock<MachineFusionKlystronBlock> FUSION_KLYSTRON;
    public static DeferredBlock<MachineFusionKlystronCreativeBlock> FUSION_KLYSTRON_CREATIVE;
    public static DeferredBlock<MachineFusionCollectorBlock> FUSION_COLLECTOR;
    public static DeferredBlock<MachineFusionBreederBlock> FUSION_BREEDER;
    public static DeferredBlock<MachineFusionBoilerBlock> FUSION_BOILER;
    public static DeferredBlock<MachineFusionMHDTBlock> FUSION_MHDT;
    public static DeferredBlock<MachineFusionCouplerBlock> FUSION_COUPLER;
    public static DeferredBlock<BlockFusionTorusStruct> STRUCT_TORUS_CORE;

    private FusionBlocks() {
    }

    public static void registerAll() {
        ICF_REACTOR = registerBlock("machine_icf_reactor", () -> new IcfReactorBlock(MACHINE_PROPS));
        ICF_CONTROLLER = registerBlock("machine_icf_controller", () -> new IcfControllerBlock(MACHINE_PROPS));
        ICF_PRESS = registerBlock("machine_icf_press", () -> new IcfPressBlock(MACHINE_PROPS));
        WATZ_REACTOR = registerBlock("machine_watz_reactor", () -> new WatzReactorBlock(MACHINE_PROPS));
        FUSION_PLASMA_FORGE = registerBlock("fusion_plasma_forge", () -> new PlasmaForgeBlock(MACHINE_PROPS));

        FUSION_TORUS = registerBlock("fusion_torus", () -> new MachineFusionTorusBlock(MACHINE_PROPS));
        FUSION_KLYSTRON = registerBlock("fusion_klystron", () -> new MachineFusionKlystronBlock(MACHINE_PROPS));
        FUSION_KLYSTRON_CREATIVE = registerBlock("fusion_klystron_creative", () -> new MachineFusionKlystronCreativeBlock(MACHINE_PROPS));
        FUSION_COLLECTOR = registerBlock("fusion_collector", () -> new MachineFusionCollectorBlock(MACHINE_PROPS));
        FUSION_BREEDER = registerBlock("fusion_breeder", () -> new MachineFusionBreederBlock(MACHINE_PROPS));
        FUSION_BOILER = registerBlock("fusion_boiler", () -> new MachineFusionBoilerBlock(MACHINE_PROPS));
        FUSION_MHDT = registerBlock("fusion_mhdt", () -> new MachineFusionMHDTBlock(MACHINE_PROPS));
        FUSION_COUPLER = registerBlock("fusion_coupler", () -> new MachineFusionCouplerBlock(MACHINE_PROPS));
        STRUCT_TORUS_CORE = registerBlock("struct_torus_core", () -> new BlockFusionTorusStruct(MACHINE_PROPS));

        registerBlock("fusion_component_0", () -> new FusionComponentBlock(MACHINE_PROPS));
        registerCasing("fusion_component_1");
        registerCasing("fusion_component_2");
        registerCasing("fusion_component_3");

        registerCasing("icf_laser_component_casing");
        registerCasing("icf_laser_component_port");
        registerCasing("icf_laser_component_cell");
        registerCasing("icf_laser_component_emitter");
        registerCasing("icf_laser_component_capacitor");
        registerCasing("icf_laser_component_turbo");
        registerCasing("icf_component_0");
        registerCasing("icf_component_1");
        registerCasing("icf_component_3");
        registerCasing("struct_icf_core");
        registerCasing("dfc_core");
        registerCasing("dfc_emitter");
        registerCasing("dfc_receiver");
        registerCasing("dfc_injector");
        registerCasing("dfc_stabilizer");

        FusionBlockEntities.registerAll();
        FusionMenus.registerAll();
    }

    private static DeferredBlock<BlockBase> registerCasing(String name) {
        return registerBlock(name, () -> new BlockBase(MACHINE_PROPS));
    }

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        DeferredBlock<T> block = ModBlocks.BLOCKS.register(name, factory);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, block);
        return block;
    }
}
