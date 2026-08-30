package com.hbm.blockentity.machine;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.ProcessingBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the shredder/assembler/crystallizer/mixer family,
 * sibling to {@link ProcessingBlocks} (that class registers the {@code Block}s these
 * {@code BlockEntityType.Builder.of} calls reference) - see {@code PowerGenBlockEntities}/
 * {@code PowerGenBlocks} for the established precedent this split follows exactly.
 * <p>
 * Called from {@link ProcessingBlocks#registerAll()}, not registered independently - see this
 * task's wiring notes for the single call site ({@code ModBlocks.register()}) this whole family
 * needs.
 */
public final class ProcessingBlockEntities {

    public static Supplier<BlockEntityType<MachineShredderBlockEntity>> MACHINE_SHREDDER;
    public static Supplier<BlockEntityType<MachineAssemblyMachineBlockEntity>> MACHINE_ASSEMBLER;
    public static Supplier<BlockEntityType<MachineCrystallizerBlockEntity>> MACHINE_CRYSTALLIZER;
    public static Supplier<BlockEntityType<MachineMixerBlockEntity>> MACHINE_MIXER;

    private ProcessingBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_SHREDDER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_shredder", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineShredderBlockEntity(MACHINE_SHREDDER.get(), pos, state),
                ProcessingBlocks.MACHINE_SHREDDER.get()
        ).build(null));

        MACHINE_ASSEMBLER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_assembly_machine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineAssemblyMachineBlockEntity(MACHINE_ASSEMBLER.get(), pos, state),
                ProcessingBlocks.MACHINE_ASSEMBLER.get()
        ).build(null));

        MACHINE_CRYSTALLIZER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_crystallizer", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineCrystallizerBlockEntity(MACHINE_CRYSTALLIZER.get(), pos, state),
                ProcessingBlocks.MACHINE_CRYSTALLIZER.get()
        ).build(null));

        MACHINE_MIXER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_mixer", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineMixerBlockEntity(MACHINE_MIXER.get(), pos, state),
                ProcessingBlocks.MACHINE_MIXER.get()
        ).build(null));
    }
}
