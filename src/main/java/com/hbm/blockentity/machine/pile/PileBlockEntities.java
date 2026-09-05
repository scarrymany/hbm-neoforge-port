package com.hbm.blockentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.PileBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class PileBlockEntities {

    public static Supplier<BlockEntityType<PileCoreBlockEntity>> PILE_CORE;
    public static Supplier<BlockEntityType<PileBaseBlockEntity>> PILE_BASE;
    public static Supplier<BlockEntityType<PileLoaderBlockEntity>> PILE_LOADER;
    public static Supplier<BlockEntityType<PileVentBlockEntity>> PILE_VENT;
    public static Supplier<BlockEntityType<PileControlBlockEntity>> PILE_CONTROL;

    private PileBlockEntities() {
    }

    public static void registerAll() {
        // CE AutoRegister TileEntityPileCore / TileEntityPileBaseMK2 — no TileMappings ids.
        PILE_CORE = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_pile_core", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PileCoreBlockEntity(PILE_CORE.get(), pos, state),
                PileBlocks.PILE_BLOCK.get()
        ).build(null));
        PILE_BASE = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_pile_base_mk2", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PileBaseBlockEntity(PILE_BASE.get(), pos, state),
                PileBlocks.PILE_BLOCK.get()
        ).build(null));
        // CE AutoRegister TileEntityPileLoader / Vent / Control.
        PILE_LOADER = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_pile_loader", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PileLoaderBlockEntity(PILE_LOADER.get(), pos, state),
                PileBlocks.PILE_DEVICE.get()
        ).build(null));
        PILE_VENT = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_pile_vent", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PileVentBlockEntity(PILE_VENT.get(), pos, state),
                PileBlocks.PILE_DEVICE.get()
        ).build(null));
        PILE_CONTROL = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_pile_control", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PileControlBlockEntity(PILE_CONTROL.get(), pos, state),
                PileBlocks.PILE_DEVICE.get()
        ).build(null));
    }
}
