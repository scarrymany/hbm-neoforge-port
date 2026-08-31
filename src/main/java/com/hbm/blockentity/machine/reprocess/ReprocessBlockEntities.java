package com.hbm.blockentity.machine.reprocess;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.reprocess.ReprocessBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class ReprocessBlockEntities {

    public static Supplier<BlockEntityType<PurexBlockEntity>> MACHINE_PUREX;
    public static Supplier<BlockEntityType<LiquefactorBlockEntity>> MACHINE_LIQUEFACTOR;

    private ReprocessBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_PUREX = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_purex", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PurexBlockEntity(MACHINE_PUREX.get(), pos, state),
                ReprocessBlocks.MACHINE_PUREX.get()
        ).build(null));
        MACHINE_LIQUEFACTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_liquefactor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new LiquefactorBlockEntity(MACHINE_LIQUEFACTOR.get(), pos, state),
                ReprocessBlocks.MACHINE_LIQUEFACTOR.get()
        ).build(null));
    }
}
