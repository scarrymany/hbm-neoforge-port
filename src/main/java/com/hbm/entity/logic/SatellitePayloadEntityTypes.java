package com.hbm.entity.logic;

import com.hbm.entity.effect.EntityCloudTom;
import com.hbm.entity.projectile.EntityTom;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the 5 satellite-payload entities researched by
 * {@code docs/phase4/satellites_followup_and_loot_pools.md} - {@link EntityDeathBlast}
 * ({@code SatelliteLaser}'s payload), {@link EntityOrbitalLaser} ({@code SatellitePrecisionLaser}'s
 * payload), and {@link EntityTom}/{@link EntityTomBlast}/{@link EntityCloudTom} (the 3-entity
 * {@code SatelliteHorizons} "gerald" payload chain). Following {@code NukeEntityTypes}' own
 * precedent of one cross-package {@code DeferredRegister<EntityType<?>>} per content family rather
 * than a shared central registry (that class registers {@code com.hbm.entity.effect.
 * EntityFalloutRain} alongside its own {@code com.hbm.entity.logic} siblings for the exact same
 * "one family, several home packages" reason) - {@link EntityTom} (a
 * {@code com.hbm.entity.projectile} vanilla-throwable subclass) and {@link EntityCloudTom} (a
 * {@code com.hbm.entity.effect} cosmetic entity) are registered here alongside their
 * {@code com.hbm.entity.logic} siblings rather than split into 3 near-empty per-package files.
 * <p>
 * <b>Two registration-name corrections versus this package's own task brief</b> (CE's real
 * {@code @AutoRegister} annotations are the ground truth per this task's own rules, not a paraphrase
 * of them): CE's real {@link EntityTomBlast} annotation is
 * {@code @AutoRegister(name = "entity_tom_bust", trackingRange = 1000)} - not
 * {@code "entity_tom_blast"} - and CE's real {@link EntityCloudTom} annotation is
 * {@code @AutoRegister(name = "entity_moonstone_blast", trackingRange = 1000)} - not
 * {@code "entity_cloud_tom"}. Both confirmed by direct reads of CE's own source files; the real CE
 * names are used below rather than the paraphrased ones.
 * <p>
 * Sizing: {@link EntityDeathBlast}/{@link EntityOrbitalLaser}/{@link EntityTomBlast} call no
 * {@code setSize} anywhere in CE (all 3 are pure logic-driver {@code Entity} subclasses rendering
 * nothing of their own), so each gets {@code NukeEntityTypes}' own established nominal
 * non-colliding {@code .sized(0.5F, 0.5F)}. {@link EntityTom} extends vanilla 1.12
 * {@code EntityThrowable}, whose own base constructor calls {@code setSize(0.25F, 0.25F)} (CE never
 * overrides it) - matching this port's own {@code EntityRubble} precedent for the identical base
 * class. {@link EntityCloudTom} has two CE constructors with two different sizes
 * ({@code (World)}: 1x4; {@code (World, int maxAge)}: 20x40) - only the second is ever actually
 * called by any real CE spawn site ({@code EntityTom.onUpdate()}'s own spawn), so - matching this
 * port's own {@code EntityFalloutRain}/{@code EntityCloudFleija} precedent for the identical
 * "CE has an unused constructor with a different size" situation - only the real, used size is
 * registered.
 */
public final class SatellitePayloadEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityDeathBlast>> DEATH_BLAST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityOrbitalLaser>> ORBITAL_LASER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityTom>> TOM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityTomBlast>> TOM_BLAST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCloudTom>> CLOUD_TOM;

    private SatellitePayloadEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        DEATH_BLAST = ENTITY_TYPES.register("entity_laser_blast", () ->
                EntityType.Builder.<EntityDeathBlast>of(EntityDeathBlast::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_laser_blast"));

        ORBITAL_LASER = ENTITY_TYPES.register("entity_orbital_laser", () ->
                EntityType.Builder.<EntityOrbitalLaser>of(EntityOrbitalLaser::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_orbital_laser"));

        TOM = ENTITY_TYPES.register("entity_tom_the_moonstone", () ->
                EntityType.Builder.<EntityTom>of(EntityTom::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.25F, 0.25F)
                        .setTrackingRange(1000)
                        .build("entity_tom_the_moonstone"));

        // CE real name: "entity_tom_bust" (see class javadoc - not "entity_tom_blast").
        TOM_BLAST = ENTITY_TYPES.register("entity_tom_bust", () ->
                EntityType.Builder.<EntityTomBlast>of(EntityTomBlast::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_tom_bust"));

        // CE real name: "entity_moonstone_blast" (see class javadoc - not "entity_cloud_tom").
        CLOUD_TOM = ENTITY_TYPES.register("entity_moonstone_blast", () ->
                EntityType.Builder.<EntityCloudTom>of(EntityCloudTom::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(20.0F, 40.0F)
                        .setTrackingRange(1000)
                        .build("entity_moonstone_blast"));

        ENTITY_TYPES.register(modEventBus);
    }
}
