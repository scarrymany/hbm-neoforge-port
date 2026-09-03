package com.hbm.blocks.network;

import com.hbm.blockentity.network.NetworkBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;

import java.util.function.Supplier;

/**
 * Drone logistics / network infrastructure blocks, matching {@link ConveyorBlocks} pattern.
 * <p>
 * Minimal test infrastructure for drone navigation - full CE network (IDroneLinkable, dock/provider/
 * requester) deferred. See {@link com.hbm.blockentity.network.DroneWaypointBlockEntity} javadoc.
 */
public final class NetworkBlocks {

    public static DeferredBlock<BlockDroneWaypoint> DRONE_WAYPOINT;

    private NetworkBlocks() {
    }

    public static void registerAll() {
        DRONE_WAYPOINT = registerBlock("drone_waypoint", () -> new BlockDroneWaypoint(
                BlockBehaviour.Properties.of()
                        .strength(5.0F, 30.0F)
                        .sound(SoundType.METAL)
                        .requiresCorrectToolForDrops()
        ));
        ModItems.ITEMS.register("drone_waypoint", () -> new BlockItem(DRONE_WAYPOINT.get(), new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.MACHINE, DRONE_WAYPOINT);

        NetworkBlockEntities.registerAll();
    }

    private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> factory) {
        return ModBlocks.BLOCKS.register(name, factory);
    }
}
