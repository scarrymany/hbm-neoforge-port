package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.ConveyorBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for this conveyor/crane-splitter family, sibling to
 * {@link com.hbm.blocks.network.ConveyorBlocks} (that class registers the {@code Block} this one's
 * {@code BlockEntityType.Builder.of} call references) - matches the established
 * {@code PowerGenBlocks}/{@code PowerGenBlockEntities} split.
 * <p>
 * Called from {@code ConveyorBlocks#registerAll()}, not registered independently.
 */
public final class ConveyorBlockEntities {

    public static Supplier<BlockEntityType<CraneSplitterBlockEntity>> CRANE_SPLITTER;

    private ConveyorBlockEntities() {
    }

    public static void registerAll() {
        CRANE_SPLITTER = ModBlocks.BLOCK_ENTITY_TYPES.register("crane_splitter", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CraneSplitterBlockEntity(CRANE_SPLITTER.get(), pos, state),
                ConveyorBlocks.CRANE_SPLITTER.get()
        ).build(null));
    }
}
