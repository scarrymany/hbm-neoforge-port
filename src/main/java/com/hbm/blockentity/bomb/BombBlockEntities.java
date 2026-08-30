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
    }
}
