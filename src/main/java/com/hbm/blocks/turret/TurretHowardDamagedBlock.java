package com.hbm.blocks.turret;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.turret.TurretBlockEntities;
import com.hbm.blockentity.turret.TurretHowardDamagedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code TurretHowardDamaged} - a ruins/loot "damaged" variant casing. CE extends
 * {@link com.hbm.blocks.BlockDummyable} directly rather than {@code TurretBaseNT}, but with the
 * exact same dimensions/offset/bounding box - {@link TurretBaseNTBlock} is reused here rather than
 * duplicating that shape a second time. Unlike every other concrete turret block, CE's dummy-half
 * ({@code meta < 12}) branch returns {@code null} (no proxy tile entity at all) rather than a
 * {@code TileEntityProxyCombo} - reproduced here by simply never registering a block entity for the
 * non-core half either (this port's dummy halves carry no block entity in the first place, so this
 * is a no-op distinction, kept here only as a documented parity note).
 */
public class TurretHowardDamagedBlock extends TurretBaseNTBlock {

    public TurretHowardDamagedBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(META) >= 12 ? new TurretHowardDamagedBlockEntity(TurretBlockEntities.HOWARD_DAMAGED.get(), pos, state) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == TurretBlockEntities.HOWARD_DAMAGED.get() ? ITickableBE.ticker() : null;
    }
}
