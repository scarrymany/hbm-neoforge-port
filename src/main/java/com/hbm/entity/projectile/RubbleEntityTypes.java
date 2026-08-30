package com.hbm.entity.projectile;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@link EntityRubble} - its own {@link DeferredRegister},
 * following the same per-family pattern already established by {@code com.hbm.entity.
 * ConveyorEntityTypes}/{@code com.hbm.entity.GunEntityTypes}/{@code com.hbm.entity.projectile.
 * FallingNukeEntityTypes} (no shared {@code ModEntityTypes} registry exists yet). CE's
 * {@code @AutoRegister(name = "entity_rubble", trackingRange = 1000)} supplies the id/tracking range;
 * sizing (0.25x0.25) comes from CE's own base class {@code EntityThrowableNT}'s
 * {@code setSize(0.25F, 0.25F)} call, which {@code EntityRubble} never overrides.
 */
public final class RubbleEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityRubble>> RUBBLE;

    private RubbleEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        RUBBLE = ENTITY_TYPES.register("entity_rubble", () ->
                EntityType.Builder.<EntityRubble>of(EntityRubble::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.25F, 0.25F)
                        .setTrackingRange(1000)
                        .build("entity_rubble"));

        ENTITY_TYPES.register(modEventBus);
    }
}
