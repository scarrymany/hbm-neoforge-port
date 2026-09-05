package com.hbm.blockentity.network;

import com.hbm.api.drone.IDroneLinkable;
import com.hbm.blockentity.ITickableBE;
import com.hbm.entity.item.EntityDroneBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Transport drone waypoint - navigation node for drone pathfinding. Ported (simplified for Phase 4
 * vertical slice) from CE's {@code TileEntityDroneWaypoint} (~150 lines).
 * <p>
 * <b>Phase 4 minimal implementation:</b> Sets nearby drones' target to this waypoint's position + offset.
 * CE allows right-click/shift-click to adjust offset (not ported yet). Full CE features deferred:
 * - Offset configuration via right-click interaction
 * - Path chaining to next waypoint (up to 10 waypoints per CE)
 * - DroneLinker tool integration for path setup
 * - Link persistence to dock/provider/requester
 * <p>
 * TODO(CE: TileEntityDroneWaypoint full port): Add offset interaction, path chaining, linker tool
 * integration. See CE TileEntityDroneWaypoint.java + BlockDroneWaypoint.java for complete behavior.
 */
public class DroneWaypointBlockEntity extends BlockEntity implements ITickableBE, IDroneLinkable {

    private static final double SEARCH_RADIUS = 32.0;
    private static final int UPDATE_INTERVAL = 20;
    private int tickCounter = 0;

    // CE offset system - allows adjusting waypoint target position
    private int offsetX = 0;
    private int offsetY = 0;
    private int offsetZ = 0;

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
            // Set waypoint + offset as target - CE behavior: drones follow waypoint chain
            // (full logic checks next waypoint in path, handles 10-waypoint limit, etc. - simplified here)
            double targetX = waypointPos.getX() + 0.5 + offsetX;
            double targetY = waypointPos.getY() + 1.0 + offsetY;
            double targetZ = waypointPos.getZ() + 0.5 + offsetZ;
            drone.setTarget(targetX, targetY, targetZ);
        }
    }

    @Override
    public int[] getOffset() {
        return new int[]{offsetX, offsetY, offsetZ};
    }

    public void setOffset(int x, int y, int z) {
        this.offsetX = x;
        this.offsetY = y;
        this.offsetZ = z;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("offsetX", offsetX);
        tag.putInt("offsetY", offsetY);
        tag.putInt("offsetZ", offsetZ);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        offsetX = tag.getInt("offsetX");
        offsetY = tag.getInt("offsetY");
        offsetZ = tag.getInt("offsetZ");
    }
}
