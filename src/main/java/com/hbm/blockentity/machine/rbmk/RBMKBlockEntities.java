package com.hbm.blockentity.machine.rbmk;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.rbmk.RBMKBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for every RBMK column/console/pipe-stub block entity - sibling
 * to {@link com.hbm.blocks.machine.rbmk.RBMKBlocks} (that class registers the {@code Block}s this
 * one's {@code BlockEntityType.Builder.of} calls reference), following the identical split
 * {@code PWRBlockEntities}/{@code PWRBlocks} already established for this port's other Phase 2
 * multiblock family. Called from {@link RBMKBlocks#registerAll()}, not registered independently.
 */
public final class RBMKBlockEntities {

    public static Supplier<BlockEntityType<RBMKRodBlockEntity>> ROD;
    public static Supplier<BlockEntityType<RBMKRodReaSimBlockEntity>> ROD_REASIM;
    public static Supplier<BlockEntityType<RBMKModeratorBlockEntity>> MODERATOR;
    public static Supplier<BlockEntityType<RBMKAbsorberBlockEntity>> ABSORBER;
    public static Supplier<BlockEntityType<RBMKReflectorBlockEntity>> REFLECTOR;
    public static Supplier<BlockEntityType<RBMKBlankBlockEntity>> BLANK;
    public static Supplier<BlockEntityType<RBMKControlManualBlockEntity>> CONTROL_MANUAL;
    public static Supplier<BlockEntityType<RBMKControlAutoBlockEntity>> CONTROL_AUTO;
    public static Supplier<BlockEntityType<RBMKBoilerBlockEntity>> BOILER;
    public static Supplier<BlockEntityType<RBMKOutgasserBlockEntity>> OUTGASSER;
    public static Supplier<BlockEntityType<RBMKCoolerBlockEntity>> COOLER;
    public static Supplier<BlockEntityType<RBMKHeaterBlockEntity>> HEATER;
    public static Supplier<BlockEntityType<RBMKStorageBlockEntity>> STORAGE;
    public static Supplier<BlockEntityType<RBMKInletBlockEntity>> INLET;
    public static Supplier<BlockEntityType<RBMKOutletBlockEntity>> OUTLET;
    public static Supplier<BlockEntityType<RBMKAutoloaderBlockEntity>> AUTOLOADER;
    public static Supplier<BlockEntityType<RBMKConsoleBlockEntity>> CONSOLE;

    // Mini-panels
    public static Supplier<BlockEntityType<RBMKNumitronBlockEntity>> RBMK_NUMITRON;
    public static Supplier<BlockEntityType<RBMKTerminalBlockEntity>> RBMK_TERMINAL;

    private RBMKBlockEntities() {
    }

    public static void registerAll() {
        ROD = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_rod", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKRodBlockEntity(ROD.get(), pos, state),
                RBMKBlocks.ROD.get(), RBMKBlocks.ROD_MOD.get()
        ).build(null));

        ROD_REASIM = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_rod_reasim", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKRodReaSimBlockEntity(ROD_REASIM.get(), pos, state),
                RBMKBlocks.ROD_REASIM.get(), RBMKBlocks.ROD_REASIM_MOD.get()
        ).build(null));

        MODERATOR = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_moderator", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKModeratorBlockEntity(MODERATOR.get(), pos, state),
                RBMKBlocks.MODERATOR.get()
        ).build(null));

        ABSORBER = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_absorber", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKAbsorberBlockEntity(ABSORBER.get(), pos, state),
                RBMKBlocks.ABSORBER.get()
        ).build(null));

        REFLECTOR = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_reflector", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKReflectorBlockEntity(REFLECTOR.get(), pos, state),
                RBMKBlocks.REFLECTOR.get()
        ).build(null));

        BLANK = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_blank", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKBlankBlockEntity(BLANK.get(), pos, state),
                RBMKBlocks.BLANK.get()
        ).build(null));

        CONTROL_MANUAL = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_control_manual", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKControlManualBlockEntity(CONTROL_MANUAL.get(), pos, state),
                RBMKBlocks.CONTROL.get(), RBMKBlocks.CONTROL_MOD.get(), RBMKBlocks.CONTROL_REASIM.get()
        ).build(null));

        CONTROL_AUTO = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_control_auto", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKControlAutoBlockEntity(CONTROL_AUTO.get(), pos, state),
                RBMKBlocks.CONTROL_AUTO.get(), RBMKBlocks.CONTROL_REASIM_AUTO.get()
        ).build(null));

        BOILER = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_boiler", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKBoilerBlockEntity(BOILER.get(), pos, state),
                RBMKBlocks.BOILER.get()
        ).build(null));

        OUTGASSER = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_outgasser", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKOutgasserBlockEntity(OUTGASSER.get(), pos, state),
                RBMKBlocks.OUTGASSER.get()
        ).build(null));

        COOLER = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_cooler", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKCoolerBlockEntity(COOLER.get(), pos, state),
                RBMKBlocks.COOLER.get()
        ).build(null));

        HEATER = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_heater", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKHeaterBlockEntity(HEATER.get(), pos, state),
                RBMKBlocks.HEATER.get()
        ).build(null));

        STORAGE = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_storage", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKStorageBlockEntity(STORAGE.get(), pos, state),
                RBMKBlocks.STORAGE.get()
        ).build(null));

        INLET = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_inlet", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKInletBlockEntity(INLET.get(), pos, state),
                RBMKBlocks.INLET.get()
        ).build(null));

        OUTLET = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_outlet", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKOutletBlockEntity(OUTLET.get(), pos, state),
                RBMKBlocks.OUTLET.get()
        ).build(null));

        AUTOLOADER = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_autoloader", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKAutoloaderBlockEntity(AUTOLOADER.get(), pos, state),
                RBMKBlocks.AUTOLOADER.get()
        ).build(null));

        CONSOLE = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_console", () -> BlockEntityType.Builder.of(
                (pos, state) -> new RBMKConsoleBlockEntity(CONSOLE.get(), pos, state),
                RBMKBlocks.CONSOLE.get()
        ).build(null));

        // Mini-panels
        RBMK_NUMITRON = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_numitron", () -> BlockEntityType.Builder.of(
                RBMKNumitronBlockEntity::new,
                RBMKBlocks.NUMITRON.get()
        ).build(null));

        RBMK_TERMINAL = ModBlocks.BLOCK_ENTITY_TYPES.register("rbmk_terminal", () -> BlockEntityType.Builder.of(
                RBMKTerminalBlockEntity::new,
                RBMKBlocks.TERMINAL.get()
        ).build(null));
    }
}
