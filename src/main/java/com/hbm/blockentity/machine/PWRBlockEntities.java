package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.PWRBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for this PWR/breeding-reactor family, sibling to
 * {@link PWRBlocks} (that class registers the {@code Block}s this one's
 * {@code BlockEntityType.Builder.of} calls reference) - see {@code PowerGenBlocks}/
 * {@code PowerGenBlockEntities} for the identical split this follows.
 *
 * <p>Called from {@link PWRBlocks#registerAll()}, not registered independently - see this task's
 * wiring notes for the single call site ({@code ModBlocks.register()}) this whole family needs.
 */
public final class PWRBlockEntities {

    public static Supplier<BlockEntityType<PWRControllerBlockEntity>> PWR_CONTROLLER;
    public static Supplier<BlockEntityType<PWRProxyBlockEntity>> PWR_PROXY;
    public static Supplier<BlockEntityType<MachineReactorBreedingBlockEntity>> REACTOR_BREEDING;

    private PWRBlockEntities() {
    }

    public static void registerAll() {
        PWR_CONTROLLER = ModBlocks.BLOCK_ENTITY_TYPES.register("pwr_controller", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PWRControllerBlockEntity(PWR_CONTROLLER.get(), pos, state),
                PWRBlocks.PWR_CONTROLLER.get()
        ).build(null));

        PWR_PROXY = ModBlocks.BLOCK_ENTITY_TYPES.register("pwr_block", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PWRProxyBlockEntity(PWR_PROXY.get(), pos, state),
                PWRBlocks.PWR_PROXY.get()
        ).build(null));

        REACTOR_BREEDING = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_reactor_breeding", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineReactorBreedingBlockEntity(REACTOR_BREEDING.get(), pos, state),
                PWRBlocks.REACTOR_BREEDING.get()
        ).build(null));
    }
}
