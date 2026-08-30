package com.hbm.entity.projectile;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@link EntityFallingNuke} ({@code NukeCustom}'s air-dropped
 * mode) - its own {@link DeferredRegister}, following the same per-family pattern as {@code
 * com.hbm.entity.ConveyorEntityTypes}/{@code com.hbm.entity.logic.NukeEntityTypes}/{@code
 * com.hbm.entity.effect.EffectEntityTypes}. CE's {@code @AutoRegister(name = "entity_falling_bomb",
 * trackingRange = 1000)} supplies the id/tracking range; CE never calls {@code setSize} for this
 * class, so a nominal falling-block-sized hitbox is used instead of guessing at an intended one.
 */
public final class FallingNukeEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityFallingNuke>> FALLING_NUKE;

    private FallingNukeEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        FALLING_NUKE = ENTITY_TYPES.register("entity_falling_bomb", () ->
                EntityType.Builder.<EntityFallingNuke>of(EntityFallingNuke::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.98F)
                        .setTrackingRange(1000)
                        .build("entity_falling_bomb"));

        ENTITY_TYPES.register(modEventBus);
    }
}
