package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.DroneDockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Drone logistics dock block. Ported from CE's {@code BlockDroneDock}.
 * <p>
 * Phase 4 minimal implementation - see {@link DroneDockBlockEntity} for deferred features.
 * TODO(CE: BlockDroneDock interaction): Add GUI opening, inventory interaction, configuration.
 */
public class BlockDroneDock extends Block implements EntityBlock {

    public BlockDroneDock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneDockBlockEntity(DroneBlocks.DRONE_DOCK_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DroneBlocks.DRONE_DOCK_BE_TYPE.get() ? ITickableBE.ticker() : null;
    }
}
