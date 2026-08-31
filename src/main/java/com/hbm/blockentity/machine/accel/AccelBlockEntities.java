package com.hbm.blockentity.machine.accel;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.accel.AccelBlocks;
import com.hbm.blocks.machine.accel.PaPartBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public final class AccelBlockEntities {

    public static Supplier<BlockEntityType<FelBlockEntity>> MACHINE_FEL;
    public static Supplier<BlockEntityType<ExcavatorBlockEntity>> MACHINE_EXCAVATOR;
    public static Supplier<BlockEntityType<PaPartBlockEntity>> PA_BEAMLINE;
    public static Supplier<BlockEntityType<PaPartBlockEntity>> PA_RFC;
    public static Supplier<BlockEntityType<PaPartBlockEntity>> PA_QUADRUPOLE;
    public static Supplier<BlockEntityType<PaPartBlockEntity>> PA_DIPOLE;
    public static Supplier<BlockEntityType<PaPartBlockEntity>> PA_SOURCE;
    public static Supplier<BlockEntityType<PaDetectorBlockEntity>> PA_DETECTOR;

    private AccelBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_FEL = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_fel", () -> BlockEntityType.Builder.of(
                (pos, state) -> new FelBlockEntity(MACHINE_FEL.get(), pos, state),
                AccelBlocks.MACHINE_FEL.get()
        ).build(null));
        MACHINE_EXCAVATOR = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_excavator", () -> BlockEntityType.Builder.of(
                (pos, state) -> new ExcavatorBlockEntity(MACHINE_EXCAVATOR.get(), pos, state),
                AccelBlocks.MACHINE_EXCAVATOR.get()
        ).build(null));
        PA_BEAMLINE = part("pa_beamline", PaPartBlock.Kind.BEAMLINE, () -> AccelBlocks.PA_BEAMLINE.get());
        PA_RFC = part("pa_rfc", PaPartBlock.Kind.RFC, () -> AccelBlocks.PA_RFC.get());
        PA_QUADRUPOLE = part("pa_quadrupole", PaPartBlock.Kind.QUADRUPOLE, () -> AccelBlocks.PA_QUADRUPOLE.get());
        PA_DIPOLE = part("pa_dipole", PaPartBlock.Kind.DIPOLE, () -> AccelBlocks.PA_DIPOLE.get());
        PA_SOURCE = part("pa_source", PaPartBlock.Kind.SOURCE, () -> AccelBlocks.PA_SOURCE.get());
        PA_DETECTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("pa_detector", () -> BlockEntityType.Builder.of(
                (pos, state) -> new PaDetectorBlockEntity(PA_DETECTOR.get(), pos, state),
                AccelBlocks.PA_DETECTOR.get()
        ).build(null));
    }

    public static Supplier<BlockEntityType<PaPartBlockEntity>> typeFor(PaPartBlock.Kind kind) {
        return switch (kind) {
            case BEAMLINE -> PA_BEAMLINE;
            case RFC -> PA_RFC;
            case QUADRUPOLE -> PA_QUADRUPOLE;
            case DIPOLE -> PA_DIPOLE;
            case SOURCE -> PA_SOURCE;
        };
    }

    private static Supplier<BlockEntityType<PaPartBlockEntity>> part(String id, PaPartBlock.Kind kind,
            Supplier<net.minecraft.world.level.block.Block> block) {
        return ModBlocks.BLOCK_ENTITY_TYPES.register(id, () -> BlockEntityType.Builder.of(
                (pos, state) -> new PaPartBlockEntity(typeFor(kind).get(), pos, state, kind),
                block.get()
        ).build(null));
    }
}
