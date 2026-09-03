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
 * Logistics drone waypoint - request-oriented navigation node (different from transport waypoint).
 * Ported (simplified for Phase 4 vertical slice) from CE's drone_waypoint_request block.
 * <p>
 * <b>Phase 4 minimal implementation:</b> Same as DroneWaypointBlockEntity but for logistics drones.
 * CE uses this for item request/provide network routing (different visual/behavior from transport waypoint).
 * Full CE features deferred:
 * - Network path integration with requester/provider blocks
 * - 5-chunk radius link detection
 * - Priority/filter configuration
 * <p>
 * TODO(CE: drone_waypoint_request full port): Add requester/provider linking, network priority system.
 */
public class DroneWaypointRequestBlockEntity extends BlockEntity implements ITickableBE, IDroneLinkable {

    private static final double SEARCH_RADIUS = 32.0;
    private static final int UPDATE_INTERVAL = 20;
    private int tickCounter = 0;

    private int offsetX = 0;
    private int offsetY = 0;
    private int offsetZ = 0;

    public DroneWaypointRequestBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
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
