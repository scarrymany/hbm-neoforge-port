package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.CrucibleBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the Crucible, sibling to {@link CrucibleBlocks} (that
 * class registers the {@code Block} this {@code BlockEntityType.Builder.of} call references) -
 * mirrors {@code ProcessingBlockEntities}/{@code ProcessingBlocks}' established split for exactly
 * this situation.
 * <p>
 * Called from {@link CrucibleBlocks#registerAll()}, not registered independently - see this task's
 * wiring notes for the single call site ({@code ModBlocks.register()}) this whole family needs.
 */
public final class CrucibleBlockEntities {

    public static Supplier<BlockEntityType<MachineCrucibleBlockEntity>> MACHINE_CRUCIBLE;

    private CrucibleBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_CRUCIBLE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_crucible", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCrucibleBlockEntity(MACHINE_CRUCIBLE.get(), pos, state),
                CrucibleBlocks.MACHINE_CRUCIBLE.get()
        ).build(null));
    }
}
