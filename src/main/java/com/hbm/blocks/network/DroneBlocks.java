package com.hbm.blocks.network;

import com.hbm.blockentity.network.DroneCrateProviderBlockEntity;
import com.hbm.blockentity.network.DroneCrateRequesterBlockEntity;
import com.hbm.blockentity.network.DroneDockBlockEntity;
import com.hbm.blockentity.network.DroneWaypointBlockEntity;
import com.hbm.blockentity.network.DroneWaypointRequestBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Drone logistics infrastructure blocks. Ported from CE's drone network (BlockDroneDock, BlockDroneWaypoint).
 */
public final class DroneBlocks {

    public static DeferredBlock<? extends Block> DRONE_DOCK;
    public static Supplier<BlockEntityType<DroneDockBlockEntity>> DRONE_DOCK_BE_TYPE;

    public static DeferredBlock<? extends Block> DRONE_WAYPOINT;
    public static Supplier<BlockEntityType<DroneWaypointBlockEntity>> DRONE_WAYPOINT_BE_TYPE;

    public static DeferredBlock<? extends Block> DRONE_WAYPOINT_REQUEST;
    public static Supplier<BlockEntityType<DroneWaypointRequestBlockEntity>> DRONE_WAYPOINT_REQUEST_BE_TYPE;

    public static DeferredBlock<? extends Block> DRONE_CRATE_PROVIDER;
    public static Supplier<BlockEntityType<DroneCrateProviderBlockEntity>> DRONE_CRATE_PROVIDER_BE_TYPE;

    public static DeferredBlock<? extends Block> DRONE_CRATE_REQUESTER;
    public static Supplier<BlockEntityType<DroneCrateRequesterBlockEntity>> DRONE_CRATE_REQUESTER_BE_TYPE;

    private DroneBlocks() {
    }

    public static void registerAll() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .strength(5.0F, 30.0F)
                .sound(SoundType.METAL)
                .requiresCorrectToolForDrops();

        // drone_dock
        DRONE_DOCK = ModBlocks.BLOCKS.register("drone_dock", () -> new BlockDroneDock(props));
        ModItems.ITEMS.register("drone_dock", () -> new BlockItem(DRONE_DOCK.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, DRONE_DOCK);

        DRONE_DOCK_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("drone_dock", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DroneDockBlockEntity(DRONE_DOCK_BE_TYPE.get(), pos, state),
                        DRONE_DOCK.get()
                ).build(null));

        // drone_waypoint (transport variant)
        DRONE_WAYPOINT = ModBlocks.BLOCKS.register("drone_waypoint", () -> new BlockDroneWaypoint(props));
        ModItems.ITEMS.register("drone_waypoint", () -> new BlockItem(DRONE_WAYPOINT.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, DRONE_WAYPOINT);

        DRONE_WAYPOINT_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("drone_waypoint", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DroneWaypointBlockEntity(DRONE_WAYPOINT_BE_TYPE.get(), pos, state),
                        DRONE_WAYPOINT.get()
                ).build(null));

        // drone_waypoint_request (logistics variant)
        DRONE_WAYPOINT_REQUEST = ModBlocks.BLOCKS.register("drone_waypoint_request", () -> new BlockDroneWaypointRequest(props));
        ModItems.ITEMS.register("drone_waypoint_request", () -> new BlockItem(DRONE_WAYPOINT_REQUEST.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, DRONE_WAYPOINT_REQUEST);

        DRONE_WAYPOINT_REQUEST_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("drone_waypoint_request", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DroneWaypointRequestBlockEntity(DRONE_WAYPOINT_REQUEST_BE_TYPE.get(), pos, state),
                        DRONE_WAYPOINT_REQUEST.get()
                ).build(null));

        // drone_crate_provider (CE ModBlocks.java:1127 - DroneDock-style block with provider inventory)
        DRONE_CRATE_PROVIDER = ModBlocks.BLOCKS.register("drone_crate_provider", () -> new BlockDroneCrateProvider(props));
        ModItems.ITEMS.register("drone_crate_provider", () -> new BlockItem(DRONE_CRATE_PROVIDER.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, DRONE_CRATE_PROVIDER);

        DRONE_CRATE_PROVIDER_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("drone_crate_provider", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DroneCrateProviderBlockEntity(DRONE_CRATE_PROVIDER_BE_TYPE.get(), pos, state),
                        DRONE_CRATE_PROVIDER.get()
                ).build(null));

        // drone_crate_requester (CE ModBlocks.java:1128 - DroneDock-style block with requester inventory + filters)
        DRONE_CRATE_REQUESTER = ModBlocks.BLOCKS.register("drone_crate_requester", () -> new BlockDroneCrateRequester(props));
        ModItems.ITEMS.register("drone_crate_requester", () -> new BlockItem(DRONE_CRATE_REQUESTER.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, DRONE_CRATE_REQUESTER);

        DRONE_CRATE_REQUESTER_BE_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("drone_crate_requester", () ->
                BlockEntityType.Builder.of(
                        (pos, state) -> new DroneCrateRequesterBlockEntity(DRONE_CRATE_REQUESTER_BE_TYPE.get(), pos, state),
                        DRONE_CRATE_REQUESTER.get()
                ).build(null));
    }
}
