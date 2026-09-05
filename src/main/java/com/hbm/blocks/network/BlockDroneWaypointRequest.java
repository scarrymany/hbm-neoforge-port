package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.DroneWaypointRequestBlockEntity;
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
 * Logistics drone waypoint block (request-oriented). Ported from CE's drone_waypoint_request block.
 * <p>
 * Phase 4 minimal implementation - see {@link DroneWaypointRequestBlockEntity} for deferred features.
 */
public class BlockDroneWaypointRequest extends Block implements EntityBlock {

    public BlockDroneWaypointRequest(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneWaypointRequestBlockEntity(DroneBlocks.DRONE_WAYPOINT_REQUEST_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DroneBlocks.DRONE_WAYPOINT_REQUEST_BE_TYPE.get() ? ITickableBE.ticker() : null;
    }
}
