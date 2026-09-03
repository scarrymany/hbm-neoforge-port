package com.hbm.api.drone;

import net.minecraft.core.BlockPos;

/**
 * Interface for blocks that participate in CE's drone logistics network. Ported from CE's
 * {@code com.hbm.tileentity.network.IDroneLinkable}.
 * <p>
 * Minimal port for Phase 4 drone infrastructure - full network routing/provider/requester logic
 * deferred. This interface establishes the API contract; concrete implementations in
 * TileEntityDroneDock and TileEntityDroneWaypoint provide basic position/offset behavior for
 * EntityDeliveryDrone navigation.
 * <p>
 * TODO(CE: full drone logistics): Port CE's complete network features:
 * - TileEntityMachineProvider / TileEntityMachineRequester (item request/provide system)
 * - Full path chaining via waypoint links (up to 10 waypoints per CE)
 * - 5-chunk radius network scanning
 * - DroneLinker tool for path configuration
 * - GUIDrone* screens for filters/priorities
 */
public interface IDroneLinkable {

    /**
     * Returns the position this linkable block contributes to the drone network. For docks, this is
     * the spawn/home position. For waypoints, this is the navigation target (with offset applied).
     */
    BlockPos getBlockPos();

    /**
     * CE's waypoint offset system - allows adjusting the target position relative to the block.
     * Returns {0,0,0} for non-waypoint blocks (docks, providers, requesters).
     */
    default int[] getOffset() {
        return new int[]{0, 0, 0};
    }
}
