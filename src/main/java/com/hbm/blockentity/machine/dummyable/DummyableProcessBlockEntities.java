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
    public static Supplier<BlockEntityType<MachinePressBlockEntity>> MACHINE_PRESS;
    public static Supplier<BlockEntityType<MachineRotaryFurnaceBlockEntity>> MACHINE_ROTARY_FURNACE;
    public static Supplier<BlockEntityType<MachineFractionTowerBlockEntity>> MACHINE_FRACTION_TOWER;
    public static Supplier<BlockEntityType<WasteDrumBlockEntity>> WASTE_DRUM;

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
        MACHINE_PRESS = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_press", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachinePressBlockEntity(MACHINE_PRESS.get(), pos, state),
                DummyableProcessBlocks.MACHINE_PRESS.get()
        ).build(null));
        MACHINE_ROTARY_FURNACE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_rotary_furnace", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRotaryFurnaceBlockEntity(MACHINE_ROTARY_FURNACE.get(), pos, state),
                DummyableProcessBlocks.MACHINE_ROTARY_FURNACE.get()
        ).build(null));
        MACHINE_FRACTION_TOWER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_fraction_tower", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineFractionTowerBlockEntity(MACHINE_FRACTION_TOWER.get(), pos, state),
                DummyableProcessBlocks.MACHINE_FRACTION_TOWER.get()
        ).build(null));
        WASTE_DRUM = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_waste_drum", () -> BlockEntityType.Builder.of(
                (pos, state) -> new WasteDrumBlockEntity(WASTE_DRUM.get(), pos, state),
                DummyableProcessBlocks.WASTE_DRUM.get()
        ).build(null));
    }
}
