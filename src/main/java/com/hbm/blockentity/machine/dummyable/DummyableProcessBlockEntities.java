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
    public static Supplier<BlockEntityType<MachineCompressorBlockEntity>> MACHINE_COMPRESSOR;
    public static Supplier<BlockEntityType<MachineCokerBlockEntity>> MACHINE_COKER;
    public static Supplier<BlockEntityType<MachineCatalyticCrackerBlockEntity>> MACHINE_CATALYTIC_CRACKER;
    public static Supplier<BlockEntityType<MachineCatalyticReformerBlockEntity>> MACHINE_CATALYTIC_REFORMER;
    public static Supplier<BlockEntityType<MachineHydrotreaterBlockEntity>> MACHINE_HYDROTREATER;
    public static Supplier<BlockEntityType<MachineVacuumDistillBlockEntity>> MACHINE_VACUUM_DISTILL;
    public static Supplier<BlockEntityType<MachineRadiolysisBlockEntity>> MACHINE_RADIOLYSIS;
    public static Supplier<BlockEntityType<MachineGasFlareBlockEntity>> MACHINE_FLARE;
    public static Supplier<BlockEntityType<MachineEPressBlockEntity>> MACHINE_EPRESS;
    public static Supplier<BlockEntityType<MachinePyroOvenBlockEntity>> MACHINE_PYROOVEN;
    public static Supplier<BlockEntityType<MachineArcFurnaceBlockEntity>> MACHINE_ARC_FURNACE;
    public static Supplier<BlockEntityType<MachineExposureChamberBlockEntity>> MACHINE_EXPOSURE_CHAMBER;

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
        MACHINE_COMPRESSOR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_compressor", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCompressorBlockEntity(MACHINE_COMPRESSOR.get(), pos, state),
                DummyableProcessBlocks.MACHINE_COMPRESSOR.get()
        ).build(null));
        MACHINE_COKER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_coker", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCokerBlockEntity(MACHINE_COKER.get(), pos, state),
                DummyableProcessBlocks.MACHINE_COKER.get()
        ).build(null));
        MACHINE_CATALYTIC_CRACKER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_catalytic_cracker", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCatalyticCrackerBlockEntity(MACHINE_CATALYTIC_CRACKER.get(), pos, state),
                DummyableProcessBlocks.MACHINE_CATALYTIC_CRACKER.get()
        ).build(null));
        MACHINE_CATALYTIC_REFORMER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_catalytic_reformer", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCatalyticReformerBlockEntity(MACHINE_CATALYTIC_REFORMER.get(), pos, state),
                DummyableProcessBlocks.MACHINE_CATALYTIC_REFORMER.get()
        ).build(null));
        MACHINE_HYDROTREATER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_hydrotreater", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineHydrotreaterBlockEntity(MACHINE_HYDROTREATER.get(), pos, state),
                DummyableProcessBlocks.MACHINE_HYDROTREATER.get()
        ).build(null));
        MACHINE_VACUUM_DISTILL = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_vacuum_distill", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineVacuumDistillBlockEntity(MACHINE_VACUUM_DISTILL.get(), pos, state),
                DummyableProcessBlocks.MACHINE_VACUUM_DISTILL.get()
        ).build(null));
        MACHINE_RADIOLYSIS = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_radiolysis", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRadiolysisBlockEntity(MACHINE_RADIOLYSIS.get(), pos, state),
                DummyableProcessBlocks.MACHINE_RADIOLYSIS.get()
        ).build(null));
        MACHINE_FLARE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_flare", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineGasFlareBlockEntity(MACHINE_FLARE.get(), pos, state),
                DummyableProcessBlocks.MACHINE_FLARE.get()
        ).build(null));
        MACHINE_EPRESS = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_epress", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineEPressBlockEntity(MACHINE_EPRESS.get(), pos, state),
                DummyableProcessBlocks.MACHINE_EPRESS.get()
        ).build(null));
        MACHINE_PYROOVEN = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_pyrooven", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachinePyroOvenBlockEntity(MACHINE_PYROOVEN.get(), pos, state),
                DummyableProcessBlocks.MACHINE_PYROOVEN.get()
        ).build(null));
        MACHINE_ARC_FURNACE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_arc_furnace", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineArcFurnaceBlockEntity(MACHINE_ARC_FURNACE.get(), pos, state),
                DummyableProcessBlocks.MACHINE_ARC_FURNACE.get()
        ).build(null));
        MACHINE_EXPOSURE_CHAMBER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_exposure_chamber", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineExposureChamberBlockEntity(MACHINE_EXPOSURE_CHAMBER.get(), pos, state),
                DummyableProcessBlocks.MACHINE_EXPOSURE_CHAMBER.get()
        ).build(null));
    }
}
