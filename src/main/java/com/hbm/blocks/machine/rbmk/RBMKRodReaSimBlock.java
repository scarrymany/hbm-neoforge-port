package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.blockentity.machine.rbmk.RBMKRodReaSimBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** ReaSim fuel rod channel column. Ported from CE's {@code RBMKRodReaSim} block class. */
public class RBMKRodReaSimBlock extends RBMKRodBlock {

    public RBMKRodReaSimBlock(Properties properties, boolean moderated) {
        super(properties, moderated);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new RBMKRodReaSimBlockEntity(RBMKBlockEntities.ROD_REASIM.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RBMKBlockEntities.ROD_REASIM.get() ? ITickableBE.ticker() : null;
    }
}
