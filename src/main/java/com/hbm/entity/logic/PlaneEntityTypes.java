package com.hbm.entity.logic;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the scripted-aircraft family ({@link EntityC130}, {@link
 * EntityBomber}), Phase 4 ({@code docs/phase4/entities_vehicles_aircraft.md} /
 * {@code entities_orbital_and_beam_payloads.md}), following the same per-family {@link
 * DeferredRegister} pattern as {@code com.hbm.entity.logic.NukeEntityTypes}. CE's {@code
 * @AutoRegister(name = "...", trackingRange = 1000)} annotations on both classes supply the ids/
 * tracking ranges; both constructors call {@code setSize(8.0F, 4.0F)}, carried over verbatim as
 * {@code .sized(8F, 4F)}.
 */
public final class PlaneEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityC130>> C130;
    public static DeferredHolder<EntityType<?>, EntityType<EntityBomber>> BOMBER;

    private PlaneEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        C130 = ENTITY_TYPES.register("entity_c130", () ->
                EntityType.Builder.<EntityC130>of(EntityC130::new, MobCategory.MISC)
                        .noSummon()
                        .sized(8F, 4F)
                        .setTrackingRange(1000)
                        .build("entity_c130"));

        BOMBER = ENTITY_TYPES.register("entity_bomber", () ->
                EntityType.Builder.<EntityBomber>of(EntityBomber::new, MobCategory.MISC)
                        .noSummon()
                        .sized(8F, 4F)
                        .setTrackingRange(1000)
                        .build("entity_bomber"));

        ENTITY_TYPES.register(modEventBus);
    }
}
