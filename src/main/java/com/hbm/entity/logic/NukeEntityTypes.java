package com.hbm.entity.logic;

import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the nuke-tier logic-driver entities ({@link
 * EntityNukeExplosionMK5}, {@link EntityNukeExplosionMK3}, {@link EntityBalefire}) plus {@link
 * EntityFalloutRain} (Phase 4, {@code docs/phase4/fallout_rain_and_effects.md}) - following the
 * one confirmed, real registration precedent in this port, {@code com.hbm.entity.
 * ConveyorEntityTypes} (its own {@code DeferredRegister<EntityType<?>>} on {@code
 * BuiltInRegistries.ENTITY_TYPE}, one {@code DeferredRegister} per entity family rather than a
 * shared central registry - see that class's own javadoc for why). CE's
 * {@code @AutoRegister(name = "...", trackingRange = 1000)} annotations on each of these classes
 * supply the ids/tracking ranges below; CE never overrides mk3/mk5/balefire's default entity size
 * (no {@code setSize} call in any of the 3), so a small nominal, non-colliding size is picked for
 * those instead of guessing at an intended visual footprint for entities that render nothing of
 * their own (Phase 5 scope) and exist purely as tick-driven world-mutation logic. {@code
 * EntityFalloutRain} is the one real exception - CE's own constructor does call
 * {@code setSize(4.0F, 20.0F)}, a real value carried over verbatim as {@code .sized(4F, 20F)}.
 */
public final class NukeEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityNukeExplosionMK5>> NUKE_MK5;
    public static DeferredHolder<EntityType<?>, EntityType<EntityNukeExplosionMK3>> NUKE_MK3;
    public static DeferredHolder<EntityType<?>, EntityType<EntityBalefire>> BALEFIRE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityFalloutRain>> FALLOUT_RAIN;

    private NukeEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        NUKE_MK5 = ENTITY_TYPES.register("entity_nuke_mk5", () ->
                EntityType.Builder.<EntityNukeExplosionMK5>of(EntityNukeExplosionMK5::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_nuke_mk5"));

        NUKE_MK3 = ENTITY_TYPES.register("entity_nuke_mk3", () ->
                EntityType.Builder.<EntityNukeExplosionMK3>of(EntityNukeExplosionMK3::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_nuke_mk3"));

        BALEFIRE = ENTITY_TYPES.register("entity_balefire", () ->
                EntityType.Builder.<EntityBalefire>of(EntityBalefire::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_balefire"));

        FALLOUT_RAIN = ENTITY_TYPES.register("entity_fallout_rain", () ->
                EntityType.Builder.<EntityFalloutRain>of(EntityFalloutRain::new, MobCategory.MISC)
                        .noSummon()
                        .sized(4.0F, 20.0F)
                        .setTrackingRange(1000)
                        .build("entity_fallout_rain"));

        ENTITY_TYPES.register(modEventBus);
    }
}
