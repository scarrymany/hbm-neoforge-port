package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.GenericBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * CE {@code tileentity_ntm_door} / {@code tileentity_blast_door}. Called from
 * {@link GenericBlocks#registerDoors()} after the door blocks exist.
 */
public final class DoorGenericBlockEntities {

    public static Supplier<BlockEntityType<DoorGenericBlockEntity>> DOOR_GENERIC;
    public static Supplier<BlockEntityType<BlastDoorBlockEntity>> BLAST_DOOR;
    public static Supplier<BlockEntityType<DummyBlockEntity>> DUMMY_BLAST;
    public static Supplier<BlockEntityType<SlidingBlastDoorBlockEntity>> SLIDING_BLAST_DOOR;
    public static Supplier<BlockEntityType<SlidingBlastDoorKeypadBlockEntity>> SLIDING_BLAST_KEYPAD;
    public static Supplier<BlockEntityType<KeypadTestBlockEntity>> KEYPAD_TEST;

    private DoorGenericBlockEntities() {
    }

    public static void registerAll() {
        DOOR_GENERIC = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_ntm_door", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DoorGenericBlockEntity(DOOR_GENERIC.get(), pos, state),
                        GenericBlocks.VAULT_DOOR.get(),
                        GenericBlocks.FIRE_DOOR.get(),
                        GenericBlocks.SLIDING_BLAST_DOOR.get(),
                        GenericBlocks.LARGE_VEHICLE_DOOR.get(),
                        GenericBlocks.WATER_DOOR.get(),
                        GenericBlocks.QE_CONTAINMENT.get(),
                        GenericBlocks.QE_SLIDING_DOOR.get(),
                        GenericBlocks.ROUND_AIRLOCK_DOOR.get(),
                        GenericBlocks.SECURE_ACCESS_DOOR.get(),
                        GenericBlocks.SLIDING_SEAL_DOOR.get(),
                        GenericBlocks.CARGO_DOOR.get(),
                        GenericBlocks.SILO_HATCH.get(),
                        GenericBlocks.SILO_HATCH_LARGE.get(),
                        GenericBlocks.TRANSITION_SEAL.get()
                ).build(null));

        BLAST_DOOR = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_blast_door", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new BlastDoorBlockEntity(BLAST_DOOR.get(), pos, state),
                        GenericBlocks.BLAST_DOOR.get()
                ).build(null));

        DUMMY_BLAST = ModBlocks.BLOCK_ENTITY_TYPES.register("dummy_blast", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DummyBlockEntity(DUMMY_BLAST.get(), pos, state),
                        GenericBlocks.DUMMY_BLOCK_BLAST.get()
                ).build(null));

        SLIDING_BLAST_DOOR = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_sliding_blast_door", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new SlidingBlastDoorBlockEntity(SLIDING_BLAST_DOOR.get(), pos, state),
                        GenericBlocks.SLIDING_BLAST_DOOR_LEGACY.get(),
                        GenericBlocks.SLIDING_BLAST_DOOR_2.get()
                ).build(null));

        SLIDING_BLAST_KEYPAD = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_sliding_blast_door_keypad", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new SlidingBlastDoorKeypadBlockEntity(SLIDING_BLAST_KEYPAD.get(), pos, state),
                        GenericBlocks.SLIDING_BLAST_DOOR_KEYPAD.get()
                ).build(null));

        KEYPAD_TEST = ModBlocks.BLOCK_ENTITY_TYPES.register("tileentity_keypad_test", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new KeypadTestBlockEntity(KEYPAD_TEST.get(), pos, state),
                        GenericBlocks.KEYPAD_TEST.get()
                ).build(null));
    }
}
