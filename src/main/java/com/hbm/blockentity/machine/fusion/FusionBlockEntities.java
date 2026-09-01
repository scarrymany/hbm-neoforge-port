package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.fusion.FusionBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class FusionBlockEntities {

    public static Supplier<BlockEntityType<IcfReactorBlockEntity>> ICF_REACTOR;
    public static Supplier<BlockEntityType<IcfControllerBlockEntity>> ICF_CONTROLLER;
    public static Supplier<BlockEntityType<IcfPressBlockEntity>> ICF_PRESS;
    public static Supplier<BlockEntityType<WatzReactorBlockEntity>> WATZ_REACTOR;
    public static Supplier<BlockEntityType<PlasmaForgeBlockEntity>> FUSION_PLASMA_FORGE;

    public static Supplier<BlockEntityType<FusionTorusBlockEntity>> FUSION_TORUS;
    public static Supplier<BlockEntityType<FusionKlystronBlockEntity>> FUSION_KLYSTRON;
    public static Supplier<BlockEntityType<FusionKlystronCreativeBlockEntity>> FUSION_KLYSTRON_CREATIVE;
    public static Supplier<BlockEntityType<FusionCollectorBlockEntity>> FUSION_COLLECTOR;
    public static Supplier<BlockEntityType<FusionBreederBlockEntity>> FUSION_BREEDER;
    public static Supplier<BlockEntityType<FusionBoilerBlockEntity>> FUSION_BOILER;
    public static Supplier<BlockEntityType<FusionMHDTBlockEntity>> FUSION_MHDT;
    public static Supplier<BlockEntityType<FusionCouplerBlockEntity>> FUSION_COUPLER;
    public static Supplier<BlockEntityType<FusionTorusStructBlockEntity>> STRUCT_TORUS_CORE;

    private FusionBlockEntities() {
    }

    public static void registerAll() {
        ICF_REACTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_icf_reactor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new IcfReactorBlockEntity(ICF_REACTOR.get(), pos, state),
                FusionBlocks.ICF_REACTOR.get()
        ).build(null));
        ICF_CONTROLLER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_icf_controller", () -> BlockEntityType.Builder.of(
                (pos, state) -> new IcfControllerBlockEntity(ICF_CONTROLLER.get(), pos, state),
                FusionBlocks.ICF_CONTROLLER.get()
        ).build(null));
        ICF_PRESS = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_icf_press", () -> BlockEntityType.Builder.of(
                (pos, state) -> new IcfPressBlockEntity(ICF_PRESS.get(), pos, state),
                FusionBlocks.ICF_PRESS.get()
        ).build(null));
        WATZ_REACTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_watz_reactor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new WatzReactorBlockEntity(WATZ_REACTOR.get(), pos, state),
                FusionBlocks.WATZ_REACTOR.get()
        ).build(null));
        FUSION_PLASMA_FORGE = ModBlocks.BLOCK_ENTITY_TYPES.register("fusion_plasma_forge", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PlasmaForgeBlockEntity(FUSION_PLASMA_FORGE.get(), pos, state),
                FusionBlocks.FUSION_PLASMA_FORGE.get()
        ).build(null));

        FUSION_TORUS = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_torus", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionTorusBlockEntity(FUSION_TORUS.get(), pos, state),
                FusionBlocks.FUSION_TORUS.get()
        ).build(null));
        FUSION_KLYSTRON = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_klystron", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionKlystronBlockEntity(FUSION_KLYSTRON.get(), pos, state),
                FusionBlocks.FUSION_KLYSTRON.get()
        ).build(null));
        FUSION_KLYSTRON_CREATIVE = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_klystron_creative", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionKlystronCreativeBlockEntity(FUSION_KLYSTRON_CREATIVE.get(), pos, state),
                FusionBlocks.FUSION_KLYSTRON_CREATIVE.get()
        ).build(null));
        FUSION_COLLECTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_collector", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionCollectorBlockEntity(FUSION_COLLECTOR.get(), pos, state),
                FusionBlocks.FUSION_COLLECTOR.get()
        ).build(null));
        FUSION_BREEDER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_breeder", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionBreederBlockEntity(FUSION_BREEDER.get(), pos, state),
                FusionBlocks.FUSION_BREEDER.get()
        ).build(null));
        FUSION_BOILER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_boiler", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionBoilerBlockEntity(FUSION_BOILER.get(), pos, state),
                FusionBlocks.FUSION_BOILER.get()
        ).build(null));
        FUSION_MHDT = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_mhdt", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionMHDTBlockEntity(FUSION_MHDT.get(), pos, state),
                FusionBlocks.FUSION_MHDT.get()
        ).build(null));
        FUSION_COUPLER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_coupler", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionCouplerBlockEntity(FUSION_COUPLER.get(), pos, state),
                FusionBlocks.FUSION_COUPLER.get()
        ).build(null));
        STRUCT_TORUS_CORE = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_fusion_torus_struct", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FusionTorusStructBlockEntity(STRUCT_TORUS_CORE.get(), pos, state),
                FusionBlocks.STRUCT_TORUS_CORE.get()
        ).build(null));
    }
}
