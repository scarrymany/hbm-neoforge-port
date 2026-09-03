package com.hbm.blocks.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.blockentity.network.DroneWaypointBlockEntity;
import com.hbm.blockentity.network.NetworkBlockEntities;
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
 * Minimal test waypoint block for drone navigation. See {@link DroneWaypointBlockEntity} javadoc for
 * scope - this is temporary infrastructure to prove drone movement works, not the full CE logistics
 * network.
 */
public class BlockDroneWaypoint extends Block implements EntityBlock {

    public BlockDroneWaypoint(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DroneWaypointBlockEntity(NetworkBlockEntities.DRONE_WAYPOINT.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return type == NetworkBlockEntities.DRONE_WAYPOINT.get() ? ITickableBE.ticker() : null;
    }
}
