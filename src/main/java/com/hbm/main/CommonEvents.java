package com.hbm.main;

import com.hbm.handler.ArmorUtil;
import com.hbm.hazard.HazardRegistry;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * Mod-bus common setup. {@code bus = Bus.MOD} is required: {@link FMLCommonSetupEvent} implements
 * {@code net.neoforged.fml.event.IModBusEvent} and only ever fires on the mod bus - confirmed against
 * FancyModLoader's {@code EventBusSubscriber} javadoc, which states {@code bus()} defaults to
 * {@code Bus.GAME} and does not auto-detect {@code IModBusEvent}. The game-bus per-entity tick
 * dispatch that used to live in this class was split out to {@link CommonTickEvents} for exactly this
 * reason - a single {@code @EventBusSubscriber} class can only subscribe to one bus.
 */
@EventBusSubscriber(modid = MainRegistry.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CommonEvents {

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            HazardRegistry.registerTrafos();
            HazardRegistry.registerItems();
            HazardRegistry.registerContaminatingDrops();
            // Flushes com.hbm.items.gear.ArmorFSB#setHazardClass's accumulated self-registrations
            // into com.hbm.util.ArmorRegistry - confirmed real call-site timing via Neo Edition's
            // own CommonEvents.commonSetup, which calls ArmorUtil.register() from this exact event.
            ArmorUtil.register();
        });
    }
}
