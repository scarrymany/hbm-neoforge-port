package com.hbm.entity.item;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the logistics-drone family ({@link EntityDeliveryDrone},
 * {@link EntityRequestDrone}) - {@link EntityDroneBase} itself is never registered directly, matching
 * CE (only its two concrete {@code @AutoRegister} subclasses are). Follows the same per-family
 * {@link DeferredRegister} pattern as this package's own sibling {@code TntPrimedEntityTypes}/
 * {@code ParachuteCrateEntityTypes}.
 * <p>
 * CE's {@code @AutoRegister(name = "entity_delivery_drone"/"entity_request_drone",
 * sendVelocityUpdates = false)} supplies the registration name; neither annotation sets an explicit
 * {@code trackingRange}, so both fall back to {@code @AutoRegister}'s own documented default of 250
 * (read from CE's own annotation declaration in {@code com.hbm.interfaces.AutoRegister}).
 * {@code sendVelocityUpdates = false} has no {@link EntityType.Builder} equivalent found anywhere in
 * this port (the same already-documented gap as {@code EntityFireLingering}/
 * {@code docs/phase3/grenades.md}) - a non-blocking Phase 5 bandwidth concern, not dropped behavior.
 * Sizing (0.75x0.75) comes from CE's shared {@code EntityDroneBase.this.setSize(0.75F, 0.75F)} call in
 * its no-arg constructor, which neither concrete subclass overrides (confirmed by reading both CE
 * files in full).
 */
public final class DroneEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityDeliveryDrone>> DELIVERY_DRONE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityRequestDrone>> REQUEST_DRONE;

    private DroneEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        DELIVERY_DRONE = ENTITY_TYPES.register("entity_delivery_drone", () ->
                EntityType.Builder.<EntityDeliveryDrone>of(EntityDeliveryDrone::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.75F, 0.75F)
                        .setTrackingRange(250)
                        .build("entity_delivery_drone"));

        REQUEST_DRONE = ENTITY_TYPES.register("entity_request_drone", () ->
                EntityType.Builder.<EntityRequestDrone>of(EntityRequestDrone::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.75F, 0.75F)
                        .setTrackingRange(250)
                        .build("entity_request_drone"));

        ENTITY_TYPES.register(modEventBus);
    }
}
