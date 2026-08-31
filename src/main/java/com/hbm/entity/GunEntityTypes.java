package com.hbm.entity;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBaseMK4CL;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.entity.projectile.EntityCoin;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the gun-framework ballistics core's three projectile/hitscan
 * entities ({@link EntityBulletBaseMK4}, {@link EntityBulletBaseMK4CL}, {@link EntityBulletBeamBase}
 * - see {@code docs/phase3/gun_framework.md}'s Package A). Follows the exact pattern
 * {@code com.hbm.entity.ConveyorEntityTypes} established (this port's first entity registration,
 * whose own javadoc explicitly leaves "own registry vs. fold into one shared class" open for
 * whoever lands next) - a dedicated {@link DeferredRegister} for this entity family rather than a
 * shared {@code ModEntityTypes}, since no such shared registry exists yet.
 * <p>
 * Sizing (0.5x0.5, matching every {@code setSize(0.5F, 0.5F)} call across CE's 3 constructors) and
 * fire-immunity (CE sets {@code isImmuneToFire = true} at construction for both bullet classes and
 * the beam; 1.21.1 expresses this at the {@link EntityType.Builder} level via {@code fireImmune()}
 * instead of a runtime field) come from CE's {@code @AutoRegister} annotations
 * ({@code entity_bullet_mk4}/{@code trackingRange = 256}, {@code entity_bullet_mk4_cl}/
 * {@code sendVelocityUpdates = false}, {@code entity_beam_mk4}/{@code trackingRange = 256}) cross-
 * checked against Neo Edition's own confirmed-real {@code NtmEntityTypes.BULLET_MK4}/
 * {@code BULLET_BEAM} builder chains (same {@code .noSummon().fireImmune().setTrackingRange(250)
 * .sized(0.5F, 0.5F)} shape - trackingRange kept at CE's own 256 here rather than Neo Edition's 250,
 * since CE is this port's source of truth for values, Neo Edition only for the builder call shape).
 * Neo Edition has no {@code BULLET_MK4CL} entry of its own (it never ported the chunk-loading
 * variant) - registered here from CE's {@code @AutoRegister} directly, same shape as
 * {@code BULLET_MK4} it extends.
 * <p>
 * {@link EntityCoin} (Phase 4, {@code docs/phase4/entities_orbital_and_beam_payloads.md}) joins this
 * family per that report's Key design decisions ("gun-adjacent... either extending GunEntityTypes or
 * standing up a small new registry are both valid" - resolved here in favor of this file, since the
 * coin-flip relay mechanic lives in {@link EntityBulletBeamBase}, this exact class). CE's
 * {@code @AutoRegister(name = "entity_coin", trackingRange = 1000)}; not fire-immune (CE never sets
 * that flag for this entity, unlike the bullets/beam above).
 */
public final class GunEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityBulletBaseMK4>> BULLET_MK4;
    public static DeferredHolder<EntityType<?>, EntityType<EntityBulletBaseMK4CL>> BULLET_MK4CL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityBulletBeamBase>> BULLET_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCoin>> COIN;

    private GunEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BULLET_MK4 = ENTITY_TYPES.register("entity_bullet_mk4", () ->
                EntityType.Builder.<EntityBulletBaseMK4>of(EntityBulletBaseMK4::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(256)
                        .build("entity_bullet_mk4"));

        BULLET_MK4CL = ENTITY_TYPES.register("entity_bullet_mk4_cl", () ->
                EntityType.Builder.<EntityBulletBaseMK4CL>of(EntityBulletBaseMK4CL::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(256)
                        .build("entity_bullet_mk4_cl"));

        BULLET_BEAM = ENTITY_TYPES.register("entity_beam_mk4", () ->
                EntityType.Builder.<EntityBulletBeamBase>of(EntityBulletBeamBase::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(0.5F, 0.5F)
                        .setTrackingRange(256)
                        .build("entity_beam_mk4"));

        COIN = ENTITY_TYPES.register("entity_coin", () ->
                EntityType.Builder.<EntityCoin>of(EntityCoin::new, MobCategory.MISC)
                        .noSummon()
                        .sized(1F, 0.5F)
                        .setTrackingRange(1000)
                        .build("entity_coin"));

        ENTITY_TYPES.register(modEventBus);
    }
}
