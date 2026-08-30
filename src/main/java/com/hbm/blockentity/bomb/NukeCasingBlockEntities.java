package com.hbm.blockentity.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.CrashedBombBlock;
import com.hbm.blocks.bomb.NukeCasingBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the 9 concrete nuke casings + {@code NukeCustom} +
 * {@code CrashedBomb} (see {@code docs/phase3/bomb_blocks_and_detonators.md} Section B), sibling to
 * {@link NukeCasingBlocks} exactly like {@code PowerGenBlockEntities}/{@code PowerGenBlocks} - that
 * class's {@code BlockEntityType.Builder.of} calls reference the {@code DeferredBlock} fields
 * registered there. Called from {@link NukeCasingBlocks#registerAll()}, not registered
 * independently - see this task's wiring notes for the one call site ({@code ModBlocks.register()})
 * this whole family needs.
 * <p>
 * {@link #CRASHED_BOMB} is one shared {@code BlockEntityType} valid for all 4
 * {@link CrashedBombBlock.EnumDudType} block variants (matching {@code PowerGenBlockEntities.MACHINE_MINI_RTG}'s
 * own two-block-per-type precedent) - each variant's block entity still carries its own baked-in
 * dud type, passed at construction, not read off the shared type.
 */
public final class NukeCasingBlockEntities {

    public static Supplier<BlockEntityType<NukeBoyBlockEntity>> NUKE_BOY;
    public static Supplier<BlockEntityType<NukeGadgetBlockEntity>> NUKE_GADGET;
    public static Supplier<BlockEntityType<NukeManBlockEntity>> NUKE_MAN;
    public static Supplier<BlockEntityType<NukeMikeBlockEntity>> NUKE_MIKE;
    public static Supplier<BlockEntityType<NukeTsarBlockEntity>> NUKE_TSAR;
    public static Supplier<BlockEntityType<NukeN2BlockEntity>> NUKE_N2;
    public static Supplier<BlockEntityType<NukePrototypeBlockEntity>> NUKE_PROTOTYPE;
    public static Supplier<BlockEntityType<NukeFleijaBlockEntity>> NUKE_FLEIJA;
    public static Supplier<BlockEntityType<NukeBalefireBlockEntity>> NUKE_BALEFIRE;
    public static Supplier<BlockEntityType<NukeCustomBlockEntity>> NUKE_CUSTOM;
    public static Supplier<BlockEntityType<CrashedBombBlockEntity>> CRASHED_BOMB;

    private NukeCasingBlockEntities() {
    }

    public static void registerAll() {
        NUKE_BOY = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_boy", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeBoyBlockEntity(NUKE_BOY.get(), pos, state),
                NukeCasingBlocks.NUKE_BOY.get()
        ).build(null));

        NUKE_GADGET = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_gadget", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeGadgetBlockEntity(NUKE_GADGET.get(), pos, state),
                NukeCasingBlocks.NUKE_GADGET.get()
        ).build(null));

        NUKE_MAN = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_man", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeManBlockEntity(NUKE_MAN.get(), pos, state),
                NukeCasingBlocks.NUKE_MAN.get()
        ).build(null));

        NUKE_MIKE = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_mike", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeMikeBlockEntity(NUKE_MIKE.get(), pos, state),
                NukeCasingBlocks.NUKE_MIKE.get()
        ).build(null));

        NUKE_TSAR = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_tsar", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeTsarBlockEntity(NUKE_TSAR.get(), pos, state),
                NukeCasingBlocks.NUKE_TSAR.get()
        ).build(null));

        NUKE_N2 = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_n2", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeN2BlockEntity(NUKE_N2.get(), pos, state),
                NukeCasingBlocks.NUKE_N2.get()
        ).build(null));

        NUKE_PROTOTYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_prototype", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukePrototypeBlockEntity(NUKE_PROTOTYPE.get(), pos, state),
                NukeCasingBlocks.NUKE_PROTOTYPE.get()
        ).build(null));

        NUKE_FLEIJA = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_fleija", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeFleijaBlockEntity(NUKE_FLEIJA.get(), pos, state),
                NukeCasingBlocks.NUKE_FLEIJA.get()
        ).build(null));

        NUKE_BALEFIRE = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_balefire", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeBalefireBlockEntity(NUKE_BALEFIRE.get(), pos, state),
                NukeCasingBlocks.NUKE_BALEFIRE.get()
        ).build(null));

        NUKE_CUSTOM = ModBlocks.BLOCK_ENTITY_TYPES.register("nuke_custom", () -> BlockEntityType.Builder.of(
                (pos, state) -> new NukeCustomBlockEntity(NUKE_CUSTOM.get(), pos, state),
                NukeCasingBlocks.NUKE_CUSTOM.get()
        ).build(null));

        CRASHED_BOMB = ModBlocks.BLOCK_ENTITY_TYPES.register("crashed_bomb", () -> BlockEntityType.Builder.of(
                (pos, state) -> new CrashedBombBlockEntity(CRASHED_BOMB.get(), pos, state,
                        ((CrashedBombBlock) state.getBlock()).getDudType()),
                NukeCasingBlocks.CRASHED_BOMB_BALEFIRE.get(), NukeCasingBlocks.CRASHED_BOMB_CONVENTIONAL.get(),
                NukeCasingBlocks.CRASHED_BOMB_NUKE.get(), NukeCasingBlocks.CRASHED_BOMB_SALTED.get()
        ).build(null));
    }
}
