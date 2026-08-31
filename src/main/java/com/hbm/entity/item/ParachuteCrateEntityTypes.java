package com.hbm.entity.item;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@link EntityParachuteCrate} (Phase 4,
 * {@code docs/phase4/entities_vehicles_aircraft.md} / {@code entities_orbital_and_beam_payloads.md}),
 * following the same per-family {@link DeferredRegister} pattern as
 * {@code com.hbm.entity.item.TntPrimedEntityTypes}. CE's {@code @AutoRegister(name =
 * "entity_parachute_crate", trackingRange = 1000)} supplies the id/tracking range; CE never overrides
 * this entity's size (no {@code setSize} call anywhere in the class), so a small nominal box is used.
 */
public final class ParachuteCrateEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityParachuteCrate>> PARACHUTE_CRATE;

    private ParachuteCrateEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        PARACHUTE_CRATE = ENTITY_TYPES.register("entity_parachute_crate", () ->
                EntityType.Builder.<EntityParachuteCrate>of(EntityParachuteCrate::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(1.0F, 1.0F)
                        .setTrackingRange(1000)
                        .build("entity_parachute_crate"));

        ENTITY_TYPES.register(modEventBus);
    }
}
