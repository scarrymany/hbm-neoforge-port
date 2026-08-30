package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.blockentity.machine.rbmk.RBMKModeratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Graphite moderator column. Ported from CE's {@code RBMKModerator} block class. */
public class RBMKModeratorBlock extends RBMKBaseBlock {

    public RBMKModeratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new RBMKModeratorBlockEntity(RBMKBlockEntities.MODERATOR.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RBMKBlockEntities.MODERATOR.get() ? ITickableBE.ticker() : null;
    }
}
