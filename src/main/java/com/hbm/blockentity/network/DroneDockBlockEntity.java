package com.hbm.blockentity.network;

import com.hbm.api.drone.IDroneLinkable;
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
 * Drone logistics dock - spawn point and home base for delivery drones. Ported (simplified for Phase 4
 * vertical slice) from CE's {@code TileEntityDroneDock} (~300 lines).
 * <p>
 * <b>Phase 4 minimal implementation:</b> Sets nearby drones' target to this dock's position (home/spawn
 * behavior). Full CE features deferred:
 * - Inventory transfer to/from drone cargo (18 slots + fluid tank)
 * - Refueling system (kerosene consumption tracking)
 * - Network path start point (link to waypoints/providers/requesters)
 * - 5-chunk radius scanning for linkable blocks
 * - GUI for configuration
 * <p>
 * TODO(CE: TileEntityDroneDock full port): Add inventory management, refuel logic, network scanning,
 * GUI (GUIDroneDock). See CE TileEntityDroneDock.java for complete behavior.
 */
public class DroneDockBlockEntity extends BlockEntity implements ITickableBE, IDroneLinkable {

    private static final double SEARCH_RADIUS = 32.0;
    private static final int UPDATE_INTERVAL = 20;
    private int tickCounter = 0;

    public DroneDockBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void updateEntity() {
        Level level = this.getLevel();
        if (level == null || level.isClientSide()) return;

        tickCounter++;
        if (tickCounter < UPDATE_INTERVAL) return;
        tickCounter = 0;

        BlockPos dockPos = this.getBlockPos();
        AABB searchBox = new AABB(dockPos).inflate(SEARCH_RADIUS);
        List<EntityDroneBase> drones = level.getEntitiesOfClass(EntityDroneBase.class, searchBox);

        for (EntityDroneBase drone : drones) {
            // Set dock as target - CE behavior: drones return to dock when idle or for refuel
            // (full logic checks fuel level, inventory state, etc. - simplified here)
            drone.setTarget(dockPos.getX() + 0.5, dockPos.getY() + 1.5, dockPos.getZ() + 0.5);
        }
    }
}
