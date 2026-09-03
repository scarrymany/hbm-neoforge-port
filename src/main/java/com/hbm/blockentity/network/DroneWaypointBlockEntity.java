package com.hbm.blockentity.network;

import com.hbm.blockentity.ITickableBE;
import com.hbm.entity.item.EntityDroneBase;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Minimal test waypoint for drone navigation. Sets targets for nearby drones to make them fly to this
 * block's position. Full CE logistics network (IDroneLinkable, dock/provider/requester) deferred - this
 * is just enough to prove EntityDeliveryDrone movement works after spawn.
 * <p>
 * TODO(CE: TileEntityDroneWaypoint + network system): Port full CE drone logistics once basic movement
 * is confirmed working. CE system includes:
 * - IDroneLinkable interface for dock/waypoint/provider/requester blocks
 * - TileEntityDroneDock: spawn point, refuel, inventory transfer
 * - TileEntityDroneWaypoint: navigation node
 * - TileEntityMachineRequester: request items from network
 * - TileEntityMachineProvider: provide items to network
 * - GUIDrone* screens for configuration
 * This minimal test just makes drones fly to a target, skipping the full routing/inventory logic.
 */
public class DroneWaypointBlockEntity extends BlockEntity implements ITickableBE {

    private static final double SEARCH_RADIUS = 32.0;
    private static final int UPDATE_INTERVAL = 20; // Update every second
    private int tickCounter = 0;

    public DroneWaypointBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) return;
        tickCounter = 0;

        BlockPos waypointPos = this.getBlockPos();
        AABB searchBox = new AABB(waypointPos).inflate(SEARCH_RADIUS);
        List<EntityDroneBase> drones = level.getEntitiesOfClass(EntityDroneBase.class, searchBox);

        for (EntityDroneBase drone : drones) {
            // Set this waypoint as the drone's target (center of block, 0.5 block above)
            drone.setTarget(waypointPos.getX() + 0.5, waypointPos.getY() + 1.0, waypointPos.getZ() + 0.5);
        }
    }
}
