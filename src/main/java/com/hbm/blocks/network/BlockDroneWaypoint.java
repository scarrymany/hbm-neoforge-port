package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.DroneWaypointBlockEntity;
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
 * Transport drone waypoint block. Ported from CE's {@code BlockDroneWaypoint}.
 * <p>
 * Phase 4 minimal implementation - see {@link DroneWaypointBlockEntity} for deferred features.
 * TODO(CE: BlockDroneWaypoint interaction): Add offset configuration via right-click/shift-click,
 * DroneLinker tool integration.
 */
public class BlockDroneWaypoint extends Block implements EntityBlock {

    public BlockDroneWaypoint(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneWaypointBlockEntity(DroneBlocks.DRONE_WAYPOINT_BE_TYPE.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == DroneBlocks.DRONE_WAYPOINT_BE_TYPE.get() ? ITickableBE.ticker() : null;
    }
}
