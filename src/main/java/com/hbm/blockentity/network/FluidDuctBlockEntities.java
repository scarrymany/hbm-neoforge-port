package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.FluidDuctBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the {@code FluidDuctBase} family, sibling to
 * {@link FluidDuctBlocks} (that class registers the {@link net.minecraft.world.level.block.Block}s
 * this one's {@code BlockEntityType.Builder.of} calls reference) - mirrors
 * {@code com.hbm.blockentity.machine.StorageBlockEntities}/{@code StorageMachineBlocks}'s own
 * block/block-entity package split.
 *
 * <p>8 distinct {@link BlockEntityType}s for 10 concrete blocks, matching
 * {@code docs/phase2/network_fluid_ducts.md}'s registry table exactly:
 * {@link #STANDARD_TYPE} covers both {@code FluidDuctStandardBlock} and {@code FluidDuctBoxBlock}
 * (CE's own {@code TileEntityPipeBaseNT} pairing for both), and {@link #VALVE_TYPE} covers both
 * {@code FluidValveBlock} and {@code FluidSwitchBlock} (CE's own shared {@code TileEntityFluidValve}).
 */
public final class FluidDuctBlockEntities {

    public static Supplier<BlockEntityType<PipeBaseBlockEntity>> STANDARD_TYPE;
    public static Supplier<BlockEntityType<PipeBaseBlockEntity>> BOX_TYPE;
    public static Supplier<BlockEntityType<PipeExhaustBlockEntity>> BOX_EXHAUST_TYPE;
    public static Supplier<BlockEntityType<FluidCounterValveBlockEntity>> COUNTER_VALVE_TYPE;
    public static Supplier<BlockEntityType<FluidValveBlockEntity>> VALVE_TYPE;
    public static Supplier<BlockEntityType<PipePaintableBlockEntity>> PAINTABLE_TYPE;
    public static Supplier<BlockEntityType<PipeExhaustPaintableBlockEntity>> PAINTABLE_EXHAUST_TYPE;
    public static Supplier<BlockEntityType<PipeGaugeBlockEntity>> GAUGE_TYPE;
    public static Supplier<BlockEntityType<PipeAnchorBlockEntity>> ANCHOR_TYPE;

    private FluidDuctBlockEntities() {
    }

    public static void registerAll() {
        STANDARD_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_duct_standard", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipeBaseBlockEntity(STANDARD_TYPE.get(), pos, state),
                FluidDuctBlocks.DUCT_STANDARD.get()
        ).build(null));

        BOX_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_duct_box", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipeBaseBlockEntity(BOX_TYPE.get(), pos, state),
                FluidDuctBlocks.DUCT_BOX.get()
        ).build(null));

        BOX_EXHAUST_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_duct_box_exhaust", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipeExhaustBlockEntity(BOX_EXHAUST_TYPE.get(), pos, state),
                FluidDuctBlocks.DUCT_BOX_EXHAUST.get(),
                FluidDuctBlocks.DUCT_EXHAUST.get()
        ).build(null));

        COUNTER_VALVE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_counter_valve", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FluidCounterValveBlockEntity(COUNTER_VALVE_TYPE.get(), pos, state),
                FluidDuctBlocks.COUNTER_VALVE.get()
        ).build(null));

        VALVE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_valve", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FluidValveBlockEntity(VALVE_TYPE.get(), pos, state),
                FluidDuctBlocks.VALVE.get(), FluidDuctBlocks.SWITCH.get()
        ).build(null));

        PAINTABLE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_duct_paintable", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipePaintableBlockEntity(PAINTABLE_TYPE.get(), pos, state),
                FluidDuctBlocks.DUCT_PAINTABLE.get()
        ).build(null));

        PAINTABLE_EXHAUST_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_duct_paintable_exhaust", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipeExhaustPaintableBlockEntity(PAINTABLE_EXHAUST_TYPE.get(), pos, state),
                FluidDuctBlocks.DUCT_PAINTABLE_EXHAUST.get()
        ).build(null));

        GAUGE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_duct_gauge", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipeGaugeBlockEntity(GAUGE_TYPE.get(), pos, state),
                FluidDuctBlocks.DUCT_GAUGE.get()
        ).build(null));

        ANCHOR_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("fluid_pipe_anchor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PipeAnchorBlockEntity(ANCHOR_TYPE.get(), pos, state),
                FluidDuctBlocks.PIPE_ANCHOR.get()
        ).build(null));
    }
}
