package com.hbm.blockentity.machine.dummyable;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.dummyable.DummyableProcessBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class DummyableProcessBlockEntities {

    public static Supplier<BlockEntityType<FurnaceCombinationBlockEntity>> FURNACE_COMBINATION;
    public static Supplier<BlockEntityType<MachineBlastFurnaceBlockEntity>> MACHINE_BLAST_FURNACE;
    public static Supplier<BlockEntityType<MachineRockMillBlockEntity>> MACHINE_ROCK_MILL;
    public static Supplier<BlockEntityType<MachineAnnihilatorBlockEntity>> MACHINE_ANNIHILATOR;

    private DummyableProcessBlockEntities() {
    }

    public static void registerAll() {
        FURNACE_COMBINATION = ModBlocks.BLOCK_ENTITY_TYPES.register("furnace_combination", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FurnaceCombinationBlockEntity(FURNACE_COMBINATION.get(), pos, state),
                DummyableProcessBlocks.FURNACE_COMBINATION.get()
        ).build(null));
        MACHINE_BLAST_FURNACE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_blast_furnace", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineBlastFurnaceBlockEntity(MACHINE_BLAST_FURNACE.get(), pos, state),
                DummyableProcessBlocks.MACHINE_BLAST_FURNACE.get()
        ).build(null));
        MACHINE_ROCK_MILL = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_rock_mill", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRockMillBlockEntity(MACHINE_ROCK_MILL.get(), pos, state),
                DummyableProcessBlocks.MACHINE_ROCK_MILL.get()
        ).build(null));
        MACHINE_ANNIHILATOR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_annihilator", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineAnnihilatorBlockEntity(MACHINE_ANNIHILATOR.get(), pos, state),
                DummyableProcessBlocks.MACHINE_ANNIHILATOR.get()
        ).build(null));
    }
}
