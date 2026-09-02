package com.hbm.blockentity.bomb;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BombBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for {@code docs/phase3/bomb_blocks_and_detonators.md}
 * Section A's two block-entity-owning families ({@link ChargeBlockEntity}/
 * {@link LandmineBlockEntity}), sibling to {@link BombBlocks} exactly like this port's established
 * {@code EnergyNetworkBlockEntities}/{@code EnergyNetworkBlocks} pattern. Called from
 * {@link BombBlocks#registerAll()}, not registered independently.
 */
public final class BombBlockEntities {

    public static Supplier<BlockEntityType<ChargeBlockEntity>> CHARGE;
    public static Supplier<BlockEntityType<LandmineBlockEntity>> LANDMINE;

    /** Phase 3 ({@code missile_launch_infra}): small pad, {@link LaunchPadBlockEntity}. */
    public static Supplier<BlockEntityType<LaunchPadBlockEntity>> LAUNCH_PAD;
    /** Phase 3 ({@code missile_launch_infra}): large erector pad, {@link LaunchPadLargeBlockEntity}. */
    public static Supplier<BlockEntityType<LaunchPadLargeBlockEntity>> LAUNCH_PAD_LARGE;
    /** Phase 3 ({@code missile_launch_infra}): standalone rusted pad, {@link LaunchPadRustedBlockEntity} - not a {@link LaunchPadBaseBlockEntity} subclass, per the research report's explicit warning. */
    public static Supplier<BlockEntityType<LaunchPadRustedBlockEntity>> LAUNCH_PAD_RUSTED;

    public static Supplier<BlockEntityType<BombMultiBlockEntity>> BOMB_MULTI;

    private BombBlockEntities() {
    }

    public static void registerAll() {
        CHARGE = ModBlocks.BLOCK_ENTITY_TYPES.register("charge", () -> BlockEntityType.Builder.of(
                (pos, state) -> new ChargeBlockEntity(CHARGE.get(), pos, state),
                BombBlocks.CHARGE_C4.get(), BombBlocks.CHARGE_SEMTEX.get(), BombBlocks.CHARGE_DYNAMITE.get(), BombBlocks.CHARGE_MINER.get()
        ).build(null));

        LANDMINE = ModBlocks.BLOCK_ENTITY_TYPES.register("landmine", () -> BlockEntityType.Builder.of(
                (pos, state) -> new LandmineBlockEntity(LANDMINE.get(), pos, state),
                BombBlocks.MINE_AP.get(), BombBlocks.MINE_HE.get(), BombBlocks.MINE_SHRAP.get(), BombBlocks.MINE_FAT.get(), BombBlocks.MINE_NAVAL.get()
        ).build(null));

        LAUNCH_PAD = ModBlocks.BLOCK_ENTITY_TYPES.register("launch_pad", () -> BlockEntityType.Builder.of(
                (pos, state) -> new LaunchPadBlockEntity(LAUNCH_PAD.get(), pos, state),
                BombBlocks.LAUNCH_PAD.get()
        ).build(null));

        LAUNCH_PAD_LARGE = ModBlocks.BLOCK_ENTITY_TYPES.register("launch_pad_large", () -> BlockEntityType.Builder.of(
                (pos, state) -> new LaunchPadLargeBlockEntity(LAUNCH_PAD_LARGE.get(), pos, state),
                BombBlocks.LAUNCH_PAD_LARGE.get()
        ).build(null));

        LAUNCH_PAD_RUSTED = ModBlocks.BLOCK_ENTITY_TYPES.register("launch_pad_rusted", () -> BlockEntityType.Builder.of(
                (pos, state) -> new LaunchPadRustedBlockEntity(LAUNCH_PAD_RUSTED.get(), pos, state),
                BombBlocks.LAUNCH_PAD_RUSTED.get()
        ).build(null));

        BOMB_MULTI = ModBlocks.BLOCK_ENTITY_TYPES.register("bomb_multi", () -> BlockEntityType.Builder.of(
                (pos, state) -> new BombMultiBlockEntity(BOMB_MULTI.get(), pos, state),
                BombBlocks.BOMB_MULTI.get()
        ).build(null));
    }
}
