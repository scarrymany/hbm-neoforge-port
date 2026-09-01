package com.hbm.blockentity.machine.pile;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.pile.PileBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class PileBlockEntities {

    public static Supplier<BlockEntityType<PileCoreBlockEntity>> PILE_CORE;
    public static Supplier<BlockEntityType<PileBaseBlockEntity>> PILE_BASE;

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
    }
}
