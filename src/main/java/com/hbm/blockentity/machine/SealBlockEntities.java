package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.SealBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class SealBlockEntities {

    public static Supplier<BlockEntityType<SealHatchBlockEntity>> SEAL_HATCH;

    private SealBlockEntities() {
    }

    public static void registerAll() {
        // CE TileMappings: "tileentity_seal_lid"
        SEAL_HATCH = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_seal_lid", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SealHatchBlockEntity(SEAL_HATCH.get(), pos, state),
                SealBlocks.SEAL_HATCH.get()
        ).build(null));
    }
}
