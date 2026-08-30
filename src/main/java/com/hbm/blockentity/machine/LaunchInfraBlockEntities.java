package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.LaunchInfraBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for {@link LaunchInfraBlocks}'s three block-entity-owning
 * blocks, mirroring {@code com.hbm.blockentity.bomb.BombBlockEntities}'s established pattern.
 * Called from {@link LaunchInfraBlocks#registerAll()}, not registered independently.
 */
public final class LaunchInfraBlockEntities {

    public static Supplier<BlockEntityType<SiloHatchBlockEntity>> SILO_HATCH;
    public static Supplier<BlockEntityType<DummyBlockEntity>> DUMMY_SILO_HATCH;
    public static Supplier<BlockEntityType<LaunchpadSoyuzBlockEntity>> LAUNCHPAD_SOYUZ;

    private LaunchInfraBlockEntities() {
    }

    public static void registerAll() {
        SILO_HATCH = ModBlocks.BLOCK_ENTITY_TYPES.register("silo_hatch", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SiloHatchBlockEntity(SILO_HATCH.get(), pos, state),
                LaunchInfraBlocks.SILO_HATCH.get()
        ).build(null));

        DUMMY_SILO_HATCH = ModBlocks.BLOCK_ENTITY_TYPES.register("dummy_silo_hatch", () -> BlockEntityType.Builder.of(
                (pos, state) -> new DummyBlockEntity(DUMMY_SILO_HATCH.get(), pos, state),
                LaunchInfraBlocks.DUMMY_BLOCK_SILO_HATCH.get()
        ).build(null));

        LAUNCHPAD_SOYUZ = ModBlocks.BLOCK_ENTITY_TYPES.register("launchpad_soyuz", () -> BlockEntityType.Builder.of(
                (pos, state) -> new LaunchpadSoyuzBlockEntity(LAUNCHPAD_SOYUZ.get(), pos, state),
                LaunchInfraBlocks.LAUNCHPAD_SOYUZ.get()
        ).build(null));
    }
}
