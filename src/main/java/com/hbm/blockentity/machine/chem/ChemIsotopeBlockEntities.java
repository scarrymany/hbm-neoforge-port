package com.hbm.blockentity.machine.chem;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.chem.ChemIsotopeBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the chemical-plant/centrifuge/gas-centrifuge/cyclotron/
 * SILEX/electrolyser machine family ({@code docs/phase2/machines_chemical_isotope.md}), sibling to
 * {@link ChemIsotopeBlocks} (that class registers the {@code Block}s this one's
 * {@code BlockEntityType.Builder.of} calls reference) - see {@code PowerGenBlockEntities}/
 * {@code PowerGenBlocks} for the precedent this split follows.
 * <p>
 * Called from {@link ChemIsotopeBlocks#registerAll()}, not registered independently.
 */
public final class ChemIsotopeBlockEntities {

    public static Supplier<BlockEntityType<CentrifugeBlockEntity>> CENTRIFUGE;
    public static Supplier<BlockEntityType<GasCentrifugeBlockEntity>> GAS_CENTRIFUGE;
    public static Supplier<BlockEntityType<SilexBlockEntity>> SILEX;
    public static Supplier<BlockEntityType<CyclotronBlockEntity>> CYCLOTRON;
    public static Supplier<BlockEntityType<ChemPlantBlockEntity>> CHEM_PLANT;
    public static Supplier<BlockEntityType<ElectrolyserBlockEntity>> ELECTROLYSER;

    private ChemIsotopeBlockEntities() {
    }

    public static void registerAll() {
        CENTRIFUGE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_centrifuge", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CentrifugeBlockEntity(CENTRIFUGE.get(), pos, state),
                ChemIsotopeBlocks.CENTRIFUGE.get()
        ).build(null));

        GAS_CENTRIFUGE = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_gascent", () -> BlockEntityType.Builder.of(
                (pos, state) -> new GasCentrifugeBlockEntity(GAS_CENTRIFUGE.get(), pos, state),
                ChemIsotopeBlocks.GAS_CENTRIFUGE.get()
        ).build(null));

        SILEX = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_silex", () -> BlockEntityType.Builder.of(
                (pos, state) -> new SilexBlockEntity(SILEX.get(), pos, state),
                ChemIsotopeBlocks.SILEX.get()
        ).build(null));

        CYCLOTRON = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_cyclotron", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CyclotronBlockEntity(CYCLOTRON.get(), pos, state),
                ChemIsotopeBlocks.CYCLOTRON.get()
        ).build(null));

        CHEM_PLANT = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_chemical_plant", () -> BlockEntityType.Builder.of(
                (pos, state) -> new ChemPlantBlockEntity(CHEM_PLANT.get(), pos, state),
                ChemIsotopeBlocks.CHEM_PLANT.get()
        ).build(null));

        ELECTROLYSER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_electrolyser", () -> BlockEntityType.Builder.of(
                (pos, state) -> new ElectrolyserBlockEntity(ELECTROLYSER.get(), pos, state),
                ChemIsotopeBlocks.ELECTROLYSER.get()
        ).build(null));
    }
}
