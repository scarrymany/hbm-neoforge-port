package com.hbm.blocks.machine.rbmk;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.machine.rbmk.RBMKBlockEntities;
import com.hbm.blockentity.machine.rbmk.RBMKReflectorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Reflector column. Ported from CE's {@code RBMKReflector} block class. */
public class RBMKReflectorBlock extends RBMKBaseBlock {

    public RBMKReflectorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new RBMKReflectorBlockEntity(RBMKBlockEntities.REFLECTOR.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == RBMKBlockEntities.REFLECTOR.get() ? ITickableBE.ticker() : null;
    }
}
