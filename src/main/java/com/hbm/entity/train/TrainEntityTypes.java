package com.hbm.entity.train;

import com.hbm.main.MainRegistry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the rail/train vehicle system
 * ({@code docs/phase4/entities_vehicles_aircraft.md}'s rail/train table), following the same
 * per-family {@link DeferredRegister} pattern as {@code com.hbm.entity.GunEntityTypes}/
 * {@code com.hbm.entity.effect.EffectEntityTypes}. Sizes/tracking ranges are ported from each class's
 * CE {@code @AutoRegister} (all 4: {@code trackingRange = 250}) - {@link #BOUNDING_DUMMY}'s
 * {@code sized(1F, 1F)} is only the pre-{@link EntityRailCarBase.BoundingBoxDummyEntity#setSize}
 * fallback (see that class's javadoc); its real per-instance size is synced data, not the registered
 * default.
 * <p>
 * Also hosts the {@link ServerTickEvent.Pre} subscriber that actually drives
 * {@link EntityRailCarBase#updateMotion(ServerLevel)} every server tick - see that method's own
 * javadoc for why this is necessary (CE's own equivalent pass is unreachable dead code upstream) and
 * {@code docs/phase4/pollution_system.md}-adjacent precedent ({@code PollutionHandler}'s own
 * confirmed-real {@code ServerTickEvent.Pre}/{@code getAllLevels()} pattern, mirrored here). Default
 * {@code @EventBusSubscriber} bus (GAME) is correct for {@code ServerTickEvent} - it is not an
 * {@code IModBusEvent}.
 */
@EventBusSubscriber(modid = MainRegistry.MODID)
public final class TrainEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityRailCarBase.BoundingBoxDummyEntity>> BOUNDING_DUMMY;
    public static DeferredHolder<EntityType<?>, EntityType<EntityRailCarRidable.SeatDummyEntity>> TRAIN_SEAT;
    public static DeferredHolder<EntityType<?>, EntityType<TrainCargoTram>> CARGO_TRAM;
    public static DeferredHolder<EntityType<?>, EntityType<TrainCargoTramTrailer>> CARGO_TRAM_TRAILER;

    private TrainEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BOUNDING_DUMMY = ENTITY_TYPES.register("entity_ntm_bounding_dummy", () ->
                EntityType.Builder.<EntityRailCarBase.BoundingBoxDummyEntity>of(EntityRailCarBase.BoundingBoxDummyEntity::new, MobCategory.MISC)
                        .noSummon()
                        .sized(1F, 1F)
                        .setTrackingRange(250)
                        .build("entity_ntm_bounding_dummy"));

        TRAIN_SEAT = ENTITY_TYPES.register("entity_ntm_train_seat", () ->
                EntityType.Builder.<EntityRailCarRidable.SeatDummyEntity>of(EntityRailCarRidable.SeatDummyEntity::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.1F)
                        .setTrackingRange(250)
                        .build("entity_ntm_train_seat"));

        CARGO_TRAM = ENTITY_TYPES.register("entity_ntm_cargo_tram", () ->
                EntityType.Builder.<TrainCargoTram>of(TrainCargoTram::new, MobCategory.MISC)
                        .noSummon()
                        .sized(5F, 2F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cargo_tram"));

        CARGO_TRAM_TRAILER = ENTITY_TYPES.register("entity_ntm_cargo_tram_trailer", () ->
                EntityType.Builder.<TrainCargoTramTrailer>of(TrainCargoTramTrailer::new, MobCategory.MISC)
                        .noSummon()
                        .sized(5F, 2F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cargo_tram_trailer"));

        ENTITY_TYPES.register(modEventBus);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            EntityRailCarBase.updateMotion(level);
        }
    }
}
