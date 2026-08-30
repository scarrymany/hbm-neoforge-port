package com.hbm.blockentity.machine.rbmk;

import com.hbm.handler.neutron.RBMKNeutronHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Reflector column - bounces a stream back into its originating rod
 * ({@code RBMKNeutronHandler.RBMKNeutronStream.runStreamInteraction}, forward reference, see
 * {@code package-info.java}), not here. Ported from CE's {@code TileEntityRBMKReflector} (32 lines).
 */
public class RBMKReflectorBlockEntity extends RBMKBaseBlockEntity {

    public RBMKReflectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
        return RBMKNeutronHandler.RBMKType.REFLECTOR;
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.REFLECTOR;
    }
}
