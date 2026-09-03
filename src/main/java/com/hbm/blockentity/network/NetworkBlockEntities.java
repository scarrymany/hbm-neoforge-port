package com.hbm.blockentity.network;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.network.NetworkBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

/**
 * {@link BlockEntityType} registration for drone logistics / network infrastructure,
 * matching {@link ConveyorBlockEntities} pattern.
 * <p>
 * Called from {@code NetworkBlocks#registerAll()}, not registered independently.
 */
public final class NetworkBlockEntities {

    public static Supplier<BlockEntityType<DroneWaypointBlockEntity>> DRONE_WAYPOINT;

    private NetworkBlockEntities() {
    }

    public static void registerAll() {
        DRONE_WAYPOINT = ModBlocks.BLOCK_ENTITY_TYPES.register("drone_waypoint", () -> BlockEntityType.Builder.of(
                (pos, state) -> new DroneWaypointBlockEntity(DRONE_WAYPOINT.get(), pos, state),
                NetworkBlocks.DRONE_WAYPOINT.get()
        ).build(null));
    }
}
