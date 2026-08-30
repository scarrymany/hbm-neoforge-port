package com.hbm.blockentity.machine.oil;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.OilChainBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for the oil production chain (derrick, pumpjack, fracking
 * tower, refinery), sibling to {@link OilChainBlocks} - see that class's own javadoc for why the two
 * are split across files (matching this port's established {@code PowerGenBlocks}/
 * {@code PowerGenBlockEntities} split). Called from {@link OilChainBlocks#registerAll()}, not
 * registered independently.
 *
 * <p>Only the derrick's non-core dummy positions get {@code null} (no block entity, matching CE's own
 * asymmetry - see that block's javadoc); the pumpjack/fracking tower/refinery's dummy positions also
 * get {@code null} here, a deliberate simplification vs. CE's {@code TileEntityProxyCombo}, not an
 * asymmetry this port introduced on top of CE's - see {@code OilDrillBaseBlockEntity}'s "shell now"
 * TODO.</p>
 */
public final class OilChainBlockEntities {

    public static Supplier<BlockEntityType<MachineOilWellBlockEntity>> MACHINE_OIL_WELL;
    public static Supplier<BlockEntityType<MachinePumpjackBlockEntity>> MACHINE_PUMPJACK;
    public static Supplier<BlockEntityType<MachineFrackingTowerBlockEntity>> MACHINE_FRACKING_TOWER;
    public static Supplier<BlockEntityType<MachineRefineryBlockEntity>> MACHINE_REFINERY;

    private OilChainBlockEntities() {
    }

    public static void registerAll() {
        MACHINE_OIL_WELL = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_oil_well", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineOilWellBlockEntity(MACHINE_OIL_WELL.get(), pos, state),
                OilChainBlocks.MACHINE_OIL_WELL.get()
        ).build(null));

        MACHINE_PUMPJACK = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_pumpjack", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachinePumpjackBlockEntity(MACHINE_PUMPJACK.get(), pos, state),
                OilChainBlocks.MACHINE_PUMPJACK.get()
        ).build(null));

        MACHINE_FRACKING_TOWER = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_fracking_tower", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineFrackingTowerBlockEntity(MACHINE_FRACKING_TOWER.get(), pos, state),
                OilChainBlocks.MACHINE_FRACKING_TOWER.get()
        ).build(null));

        MACHINE_REFINERY = ModBlocks.BLOCK_ENTITY_TYPES.register("machine_refinery", () -> BlockEntityType.Builder.of(
                (pos, state) -> new MachineRefineryBlockEntity(MACHINE_REFINERY.get(), pos, state),
                OilChainBlocks.MACHINE_REFINERY.get()
        ).build(null));
    }
}
