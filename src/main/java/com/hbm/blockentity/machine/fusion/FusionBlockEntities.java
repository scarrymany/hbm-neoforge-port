package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.fusion.FusionBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the ICF/Watz fusion-reactor family
 * ({@code docs/phase2/machine_fusion_watz.md}), sibling to {@link FusionBlocks} (that class
 * registers the {@code Block}s this one's {@code BlockEntityType.Builder.of} calls reference) - see
 * {@code ChemIsotopeBlockEntities}/{@code ChemIsotopeBlocks} for the precedent this split follows.
 * <p>
 * Called from {@link FusionBlocks#registerAll()}, not registered independently.
 */
public final class FusionBlockEntities {

    public static Supplier<BlockEntityType<IcfReactorBlockEntity>> ICF_REACTOR;
    public static Supplier<BlockEntityType<IcfControllerBlockEntity>> ICF_CONTROLLER;
    public static Supplier<BlockEntityType<IcfPressBlockEntity>> ICF_PRESS;
    public static Supplier<BlockEntityType<WatzReactorBlockEntity>> WATZ_REACTOR;
    public static Supplier<BlockEntityType<PlasmaForgeBlockEntity>> FUSION_PLASMA_FORGE;

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
    }
}
