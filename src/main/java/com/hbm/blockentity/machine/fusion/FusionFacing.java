package com.hbm.blockentity.machine.fusion;

import com.hbm.blocks.BlockDummyable;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** CE {@code getBlockMetadata() - 10} → port Dummyable offset. */
public final class FusionFacing {

    private FusionFacing() {
    }

    public static Direction of(BlockEntity be) {
        return of(be.getBlockState());
    }

    public static Direction of(BlockState state) {
        int meta = state.hasProperty(BlockDummyable.META) ? state.getValue(BlockDummyable.META) : BlockDummyable.offset;
        int idx = meta - BlockDummyable.offset;
        if (idx < 0 || idx > 5) idx = 2;
        return Direction.from3DDataValue(idx);
    }
}
