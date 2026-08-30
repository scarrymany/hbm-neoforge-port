package com.hbm.blockentity.machine.rbmk;

import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Boron absorber column - the flux absorption (heat generation + {@code fluxQuantity} reduction by
 * {@code absorberEfficiency}) lives in {@code RBMKNeutronHandler.RBMKNeutronStream.runStreamInteraction}
 * (forward reference, see {@code package-info.java}), not here. Ported from CE's
 * {@code TileEntityRBMKAbsorber} (33 lines).
 */
public class RBMKAbsorberBlockEntity extends RBMKBaseBlockEntity {

    public RBMKAbsorberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onMelt(int reduce) {
        int count = 1 + this.level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris("BLANK");
        super.onMelt(reduce);
    }

    @Override
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.ABSORBER;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.ABSORBER;
    }
}
