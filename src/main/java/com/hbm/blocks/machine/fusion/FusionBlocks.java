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
 * Block + {@code BlockItem} registration for Phase 2's ICF/Watz fusion-reactor family - see
 * {@code docs/phase2/machine_fusion_watz.md}. Mirrors {@code ChemIsotopeBlocks}' shape (block-entity
 * registration in the sibling {@link FusionBlockEntities} class, {@link FusionMenus}' {@code MenuType}s
 * triggered from here too) - wiring this family into the game needs exactly one call from
 * {@code ModBlocks.register()}, no other shared file needs a direct edit.
 * <p>
 * <b>Not ported this pass</b>: the hot-fusion tokamak (CE's {@code tileentity/machine/fusion/**},
 * {@code TileEntityFusionTorus} and its six {@code IFusionPowerReceiver} devices) - see this
 * package's own follow-up notes for why (it is built around {@code com.hbm.uninos.networkproviders}'
 * {@code KlystronNetwork}/{@code PlasmaNetwork}, neither of which exist in this port, plus the
 * unported {@code com.hbm.modules.machine.ModuleMachineFusion} processing-loop abstraction - a
 * structurally distinct, much larger system from the ICF/Watz pair this task named). SILEX/FEL
 * (laser isotope separation) are explicitly out of scope per the survey doc's own boundary note and
 * already own their package ({@code com.hbm.blocks.machine.chem}) - this family stays entirely
 * separate from it.
 */
public final class FusionBlocks {

    private static final BlockBehaviour.Properties MACHINE_PROPS =
            BlockBehaviour.Properties.of().strength(5.0F, 15.0F).sound(SoundType.METAL).requiresCorrectToolForDrops();

    public static DeferredBlock<IcfReactorBlock> ICF_REACTOR;
    public static DeferredBlock<IcfControllerBlock> ICF_CONTROLLER;
    public static DeferredBlock<IcfPressBlock> ICF_PRESS;
    public static DeferredBlock<WatzReactorBlock> WATZ_REACTOR;
    public static DeferredBlock<PlasmaForgeBlock> FUSION_PLASMA_FORGE;

    private FusionBlocks() {
    }

    public static void registerAll() {
        ICF_REACTOR = registerBlock("machine_icf_reactor", () -> new IcfReactorBlock(MACHINE_PROPS));
        ICF_CONTROLLER = registerBlock("machine_icf_controller", () -> new IcfControllerBlock(MACHINE_PROPS));
        ICF_PRESS = registerBlock("machine_icf_press", () -> new IcfPressBlock(MACHINE_PROPS));
        WATZ_REACTOR = registerBlock("machine_watz_reactor", () -> new WatzReactorBlock(MACHINE_PROPS));
        FUSION_PLASMA_FORGE = registerBlock("fusion_plasma_forge", () -> new PlasmaForgeBlock(MACHINE_PROPS));

        // CE ModBlocks.java:1331-1348 / PlasmaForgeRecipes.java:113-237.
        // Flattened ICF laser metas (EnumICFPart) + ICF component metas 0/1/3 + DFC casings.
        // Placeable cubes (ICF TE already exists); full laser/DFC multiblock later.
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
