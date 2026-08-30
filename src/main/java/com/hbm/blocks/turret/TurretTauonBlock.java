package com.hbm.blocks.turret;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.blockentity.turret.TurretTauonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Ported from CE's {@code TurretTauon} - thin {@link TurretBaseNTBlock} casing. */
public class TurretTauonBlock extends TurretBaseNTBlock {

    public TurretTauonBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new TurretTauonBlockEntity(TurretBlockEntities.TAUON.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TurretBlockEntities.TAUON.get() ? ITickableBE.ticker() : null;
    }
}
