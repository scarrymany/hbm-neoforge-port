package com.hbm.entity.item;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** CE: {@code EntityBoatRubber} {@code @AutoRegister(name = "entity_rubber_boat", trackingRange = 250)}. */
public final class BoatEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityBoatRubber>> BOAT_RUBBER;

    private BoatEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BOAT_RUBBER = ENTITY_TYPES.register("entity_rubber_boat", () ->
                EntityType.Builder.<EntityBoatRubber>of(EntityBoatRubber::new, MobCategory.MISC)
                        .sized(1.375F, 0.5625F)
                        .setTrackingRange(250)
                        .setShouldReceiveVelocityUpdates(false)
                        .build("entity_rubber_boat"));
        ENTITY_TYPES.register(modEventBus);
    }
}
