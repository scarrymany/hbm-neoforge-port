package com.hbm.blockentity.machine.rbmk;

import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * "ReaSim" (realistic-simulation) fuel rod variant - wider flux spread (8 directions instead of 4,
 * at reduced per-stream quantity) from a single random starting rotation. Ported from CE's
 * {@code TileEntityRBMKRodReaSim} (58 lines, {@code extends TileEntityRBMKRod}). Delegates to the
 * sibling {@code rbmk_core_logic} package's ready-made
 * {@link RBMKNeutronHandler#spreadFluxReaSim} (see {@link RBMKRodBlockEntity#spreadFlux}'s own
 * javadoc for why this package doesn't reimplement the node-cache/geometry itself) and marks itself
 * as the ReaSim variant via {@link #isReaSimVariant()} so that method's own {@code checkNode}
 * cache-eviction logic (in {@code RBMKNeutronHandler.RBMKNeutronNode}) takes the wide-diamond path
 * instead of the ordinary 4-cardinal one.
 */
public class RBMKRodReaSimBlockEntity extends RBMKRodBlockEntity {

    private BlockPos reasimNodePos;

    public RBMKRodReaSimBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    // implements IRBMKFluxReceiver.isReaSimVariant() - com.hbm.api.rbmk.IRBMKFluxReceiver, default false
    @Override
    public boolean isReaSimVariant() {
        return true;
    }

    @Override
    protected void spreadFlux(double flux, double ratio) {
        if (reasimNodePos == null) reasimNodePos = worldPosition.immutable();
        RandomSource random = level.getRandom();
        RBMKNeutronHandler.spreadFluxReaSim(this, reasimNodePos, flux, ratio, random);
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.FUEL_SIM;
    }
}
