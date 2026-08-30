package com.hbm.blocks.network.energy;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.energy.PylonBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Ported from CE's {@code com.hbm.blocks.network.energy.PylonRedWire} (read in full) - a plain
 * {@link net.minecraft.world.level.block.BaseEntityBlock}, not {@link com.hbm.blocks.BlockDummyable}:
 * a single-block pylon needs no multiblock footprint. Registered twice (matching CE's own
 * {@code red_pylon}/{@code red_pylon_steel_small} instantiation of the identical class) by
 * {@link EnergyNetworkBlocks#registerAll()}.
 */
public class PylonRedWireBlock extends PylonBaseBlock {

    public PylonRedWireBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PylonBlockEntity(EnergyNetworkBlockEntities.PYLON.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == EnergyNetworkBlockEntities.PYLON.get() ? ITickableBE.ticker() : null;
    }
}
