package com.hbm.blockentity.turret;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.turret.TurretBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for this turret package's 13 CE block entities,
 * sibling to {@link TurretBlocks}
 * (that class registers the {@code Block}s this one's {@code BlockEntityType.Builder.of} calls
 * reference) - matching the {@code PowerGenBlocks}/{@code PowerGenBlockEntities} split precedent.
 * Called from {@link TurretBlocks#registerAll()}, not registered independently.
 */
public final class TurretBlockEntities {

    public static Supplier<BlockEntityType<TurretSentryBlockEntity>> SENTRY;
    public static Supplier<BlockEntityType<TurretSentryDamagedBlockEntity>> SENTRY_DAMAGED;
    public static Supplier<BlockEntityType<TurretChekhovBlockEntity>> CHEKHOV;
    public static Supplier<BlockEntityType<TurretFriendlyBlockEntity>> FRIENDLY;
    public static Supplier<BlockEntityType<TurretRichardBlockEntity>> RICHARD;
    public static Supplier<BlockEntityType<TurretJeremyBlockEntity>> JEREMY;
    public static Supplier<BlockEntityType<TurretHowardBlockEntity>> HOWARD;
    public static Supplier<BlockEntityType<TurretHowardDamagedBlockEntity>> HOWARD_DAMAGED;
    public static Supplier<BlockEntityType<TurretFritzBlockEntity>> FRITZ;
    public static Supplier<BlockEntityType<TurretMaxwellBlockEntity>> MAXWELL;
    public static Supplier<BlockEntityType<TurretTauonBlockEntity>> TAUON;
    public static Supplier<BlockEntityType<TurretArtyBlockEntity>> ARTY;
    public static Supplier<BlockEntityType<TurretHIMARSBlockEntity>> HIMARS;

    private TurretBlockEntities() {
    }

    public static void registerAll() {
        SENTRY = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_sentry", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretSentryBlockEntity(SENTRY.get(), pos, state),
                TurretBlocks.TURRET_SENTRY.get()
        ).build(null));

        SENTRY_DAMAGED = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_sentry_damaged", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretSentryDamagedBlockEntity(SENTRY_DAMAGED.get(), pos, state),
                TurretBlocks.TURRET_SENTRY_DAMAGED.get()
        ).build(null));

        CHEKHOV = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_chekhov", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretChekhovBlockEntity(CHEKHOV.get(), pos, state),
                TurretBlocks.TURRET_CHEKHOV.get()
        ).build(null));

        FRIENDLY = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_friendly", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretFriendlyBlockEntity(FRIENDLY.get(), pos, state),
                TurretBlocks.TURRET_FRIENDLY.get()
        ).build(null));

        RICHARD = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_richard", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretRichardBlockEntity(RICHARD.get(), pos, state),
                TurretBlocks.TURRET_RICHARD.get()
        ).build(null));

        JEREMY = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_jeremy", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretJeremyBlockEntity(JEREMY.get(), pos, state),
                TurretBlocks.TURRET_JEREMY.get()
        ).build(null));

        HOWARD = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_howard", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretHowardBlockEntity(HOWARD.get(), pos, state),
                TurretBlocks.TURRET_HOWARD.get()
        ).build(null));

        HOWARD_DAMAGED = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_howard_damaged", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretHowardDamagedBlockEntity(HOWARD_DAMAGED.get(), pos, state),
                TurretBlocks.TURRET_HOWARD_DAMAGED.get()
        ).build(null));

        FRITZ = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_fritz", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretFritzBlockEntity(FRITZ.get(), pos, state),
                TurretBlocks.TURRET_FRITZ.get()
        ).build(null));

        MAXWELL = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_maxwell", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretMaxwellBlockEntity(MAXWELL.get(), pos, state),
                TurretBlocks.TURRET_MAXWELL.get()
        ).build(null));

        TAUON = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_tauon", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretTauonBlockEntity(TAUON.get(), pos, state),
                TurretBlocks.TURRET_TAUON.get()
        ).build(null));

        ARTY = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_arty", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretArtyBlockEntity(ARTY.get(), pos, state),
                TurretBlocks.TURRET_ARTY.get()
        ).build(null));

        HIMARS = ModBlocks.BLOCK_ENTITY_TYPES.register("turret_himars", () -> BlockEntityType.Builder.of(
                (pos, state) -> new TurretHIMARSBlockEntity(HIMARS.get(), pos, state),
                TurretBlocks.TURRET_HIMARS.get()
        ).build(null));
    }
}
