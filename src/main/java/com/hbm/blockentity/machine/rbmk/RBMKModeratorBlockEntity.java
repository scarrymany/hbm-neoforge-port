package com.hbm.blockentity.machine.rbmk;

import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Graphite moderator column - a trivial pass-through: all the actual moderation physics
 * (multiplying a passing {@code NeutronStream}'s {@code fluxRatio} by {@code 1 - moderatorEfficiency})
 * lives in the forward-referenced {@code RBMKNeutronHandler.RBMKNeutronStream.runStreamInteraction},
 * not here - see {@code package-info.java}. Ported from CE's {@code TileEntityRBMKModerator} (33 lines).
 */
public class RBMKModeratorBlockEntity extends RBMKBaseBlockEntity {

    public RBMKModeratorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onMelt(int reduce) {
        int count = 2 + this.level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris("GRAPHITE");
        super.onMelt(reduce);
    }

    // CE contract: TileEntityRBMKBase.getRBMKType() -> RBMKNeutronHandler.RBMKType
    @Override
    public RBMKNeutronHandler.RBMKType getRBMKType() {
        return RBMKNeutronHandler.RBMKType.MODERATOR;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.MODERATOR;
    }
}
