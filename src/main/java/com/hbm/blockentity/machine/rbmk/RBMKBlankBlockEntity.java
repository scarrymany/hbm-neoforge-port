package com.hbm.blockentity.machine.rbmk;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Structural filler column - a no-op for flux purposes (default {@code getRBMKType() == OTHER} from
 * the forward-referenced base class). Ported from CE's {@code TileEntityRBMKBlank} (26 lines).
 */
public class RBMKBlankBlockEntity extends RBMKBaseBlockEntity {

    public RBMKBlankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onMelt(int reduce) {
        int count = 1 + this.level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris("BLANK");
        super.onMelt(reduce);
    }

    @Override
    public RBMKColumn.ColumnType getConsoleType() {
        return RBMKColumn.ColumnType.BLANK;
    }
}
