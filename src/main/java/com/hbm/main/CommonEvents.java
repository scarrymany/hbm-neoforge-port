package com.hbm.main;

import com.hbm.blockentity.bomb.LaunchPadBaseBlockEntity;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.HazmatRegistry;
import com.hbm.hazard.HazardRegistry;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.saveddata.satellites.Satellite;
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
            // CE: FMLPreInitializationEvent-time HazmatRegistry.registerHazmats() call. This port
            // splits out just the initDefault() half (registerHazmats()'s Gson config-file
            // persistence is not ported - see HazmatRegistry's own javadoc); currently a no-op
            // beyond flushing HazmatRegistry.external, since nothing populates that list yet.
            HazmatRegistry.initDefault();
            // Package C (weapon-mod eval chain) - must run after every Item/BulletConfig in
            // com.hbm.items.weapon.sedna.** has registered (RegisterEvent has already fully fired by
            // the time enqueueWork's Runnable executes), see XWeaponModManager's own class javadoc.
            XWeaponModManager.init();
            // Phase 3 (missile_launch_infra) - must run after every MissileItems/MissileEntityTypes
            // DeferredHolder has registered, matching XWeaponModManager's own timing reasoning above.
            LaunchPadBaseBlockEntity.registerLaunchables();
            // Phase 3 (missile_launch_infra) - populates com.hbm.saveddata.satellites.Satellite's
            // fixed, order-sensitive registry (see that class's own javadoc on why order matters).
            Satellite.register();
        });
    }
}
