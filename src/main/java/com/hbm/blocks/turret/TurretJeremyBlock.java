package com.hbm.blocks.turret;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.blockentity.turret.TurretJeremyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Ported from CE's {@code TurretJeremy} - thin {@link TurretBaseNTBlock} casing. */
public class TurretJeremyBlock extends TurretBaseNTBlock {

    public TurretJeremyBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new TurretJeremyBlockEntity(TurretBlockEntities.JEREMY.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TurretBlockEntities.JEREMY.get() ? ITickableBE.ticker() : null;
    }
}
