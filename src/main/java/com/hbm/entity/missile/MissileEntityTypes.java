package com.hbm.entity.missile;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for every missile entity in this package, following {@code
 * com.hbm.entity.ConveyorEntityTypes}'s confirmed-real pattern (its own {@code
 * DeferredRegister<EntityType<?>>}, one per entity family rather than a shared central registry).
 * Registry names and {@code trackingRange = 1000} match CE's own per-entity
 * {@code @AutoRegister(name, trackingRange)} values 1:1 (grep-confirmed against every class this
 * file registers).
 * <p>
 * Sizes: CE only ever calls {@code setSize} explicitly for {@link EntityMissileAntiBallistic}
 * (1x8) - every {@link EntityMissileBaseNT} subclass's targeted-spawn constructor sets 1.5x1.5 (this
 * port bakes that into the registration instead, since {@link EntityMissileBaseNT#initTrajectory}
 * no longer doubles as a constructor - see that class's "Simplification" javadoc note).
 * {@link EntityMIRV} (extends vanilla {@code Projectile} directly, CE never sizes it) gets a small
 * nominal 0.5x0.5 footprint, matching {@code com.hbm.entity.logic.NukeEntityTypes}'s own precedent
 * for CE entities with no explicit size call.
 */
public final class MissileEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileCustom>> CUSTOM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileAntiBallistic>> ANTI_BALLISTIC;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMIRV>> MIRV;

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier0.EntityMissileTest>> TEST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier0.EntityMissileMicro>> MICRO;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier0.EntityMissileSchrabidium>> SCHRABIDIUM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier0.EntityMissileBHole>> BHOLE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier0.EntityMissileTaint>> TAINT;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier0.EntityMissileEMP>> EMP;

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier1.EntityMissileGeneric>> GENERIC;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier1.EntityMissileDecoy>> DECOY;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier1.EntityMissileIncendiary>> INCENDIARY;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier1.EntityMissileCluster>> CLUSTER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier1.EntityMissileBunkerBuster>> BUNKER_BUSTER;

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier2.EntityMissileStrong>> STRONG;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier2.EntityMissileIncendiaryStrong>> INCENDIARY_STRONG;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier2.EntityMissileClusterStrong>> CLUSTER_STRONG;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier2.EntityMissileBusterStrong>> BUSTER_STRONG;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier2.EntityMissileEMPStrong>> EMP_STRONG;

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier3.EntityMissileBurst>> BURST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier3.EntityMissileInferno>> INFERNO;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier3.EntityMissileRain>> RAIN;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier3.EntityMissileDrill>> DRILL;

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier4.EntityMissileNuclear>> NUCLEAR;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier4.EntityMissileMirv>> NUCLEAR_MIRV;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier4.EntityMissileVolcano>> VOLCANO;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier4.EntityMissileDoomsday>> DOOMSDAY;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier4.EntityMissileDoomsdayRusted>> DOOMSDAY_RUSTED;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileTier4.EntityMissileN2>> N2;

    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileStealth>> STEALTH;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMissileShuttle>> SHUTTLE;

    private MissileEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        CUSTOM = ENTITY_TYPES.register("entity_custom_missile", () ->
                EntityType.Builder.<EntityMissileCustom>of(EntityMissileCustom::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_custom_missile"));
        ANTI_BALLISTIC = ENTITY_TYPES.register("entity_missile_ab", () ->
                EntityType.Builder.<EntityMissileAntiBallistic>of(EntityMissileAntiBallistic::new, MobCategory.MISC)
                        .noSummon().sized(1F, 8F).setTrackingRange(1000).build("entity_missile_ab"));
        MIRV = ENTITY_TYPES.register("entity_mirvlet", () ->
                EntityType.Builder.<EntityMIRV>of(EntityMIRV::new, MobCategory.MISC)
                        .noSummon().sized(0.5F, 0.5F).setTrackingRange(1000).build("entity_mirvlet"));

        TEST = ENTITY_TYPES.register("entity_missile_test_mk2", () ->
                EntityType.Builder.<EntityMissileTier0.EntityMissileTest>of(EntityMissileTier0.EntityMissileTest::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_test_mk2"));
        MICRO = ENTITY_TYPES.register("entity_missile_micro", () ->
                EntityType.Builder.<EntityMissileTier0.EntityMissileMicro>of(EntityMissileTier0.EntityMissileMicro::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_micro"));
        SCHRABIDIUM = ENTITY_TYPES.register("entity_missile_schrab", () ->
                EntityType.Builder.<EntityMissileTier0.EntityMissileSchrabidium>of(EntityMissileTier0.EntityMissileSchrabidium::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_schrab"));
        BHOLE = ENTITY_TYPES.register("entity_missile_bhole", () ->
                EntityType.Builder.<EntityMissileTier0.EntityMissileBHole>of(EntityMissileTier0.EntityMissileBHole::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_bhole"));
        TAINT = ENTITY_TYPES.register("entity_missile_taint", () ->
                EntityType.Builder.<EntityMissileTier0.EntityMissileTaint>of(EntityMissileTier0.EntityMissileTaint::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_taint"));
        EMP = ENTITY_TYPES.register("entity_missile_emp", () ->
                EntityType.Builder.<EntityMissileTier0.EntityMissileEMP>of(EntityMissileTier0.EntityMissileEMP::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_emp"));

        GENERIC = ENTITY_TYPES.register("entity_missile_generic", () ->
                EntityType.Builder.<EntityMissileTier1.EntityMissileGeneric>of(EntityMissileTier1.EntityMissileGeneric::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_generic"));
        DECOY = ENTITY_TYPES.register("entity_missile_decoy", () ->
                EntityType.Builder.<EntityMissileTier1.EntityMissileDecoy>of(EntityMissileTier1.EntityMissileDecoy::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_decoy"));
        INCENDIARY = ENTITY_TYPES.register("entity_missile_incendiary", () ->
                EntityType.Builder.<EntityMissileTier1.EntityMissileIncendiary>of(EntityMissileTier1.EntityMissileIncendiary::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_incendiary"));
        CLUSTER = ENTITY_TYPES.register("entity_missile_cluster", () ->
                EntityType.Builder.<EntityMissileTier1.EntityMissileCluster>of(EntityMissileTier1.EntityMissileCluster::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_cluster"));
        BUNKER_BUSTER = ENTITY_TYPES.register("entity_missile_bunker_buster", () ->
                EntityType.Builder.<EntityMissileTier1.EntityMissileBunkerBuster>of(EntityMissileTier1.EntityMissileBunkerBuster::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_bunker_buster"));

        STRONG = ENTITY_TYPES.register("entity_missile_strong", () ->
                EntityType.Builder.<EntityMissileTier2.EntityMissileStrong>of(EntityMissileTier2.EntityMissileStrong::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_strong"));
        INCENDIARY_STRONG = ENTITY_TYPES.register("entity_missile_incendiary_strong", () ->
                EntityType.Builder.<EntityMissileTier2.EntityMissileIncendiaryStrong>of(EntityMissileTier2.EntityMissileIncendiaryStrong::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_incendiary_strong"));
        CLUSTER_STRONG = ENTITY_TYPES.register("entity_missile_cluster_strong", () ->
                EntityType.Builder.<EntityMissileTier2.EntityMissileClusterStrong>of(EntityMissileTier2.EntityMissileClusterStrong::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_cluster_strong"));
        BUSTER_STRONG = ENTITY_TYPES.register("entity_missile_buster_strong", () ->
                EntityType.Builder.<EntityMissileTier2.EntityMissileBusterStrong>of(EntityMissileTier2.EntityMissileBusterStrong::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_buster_strong"));
        EMP_STRONG = ENTITY_TYPES.register("entity_missile_emp_strong", () ->
                EntityType.Builder.<EntityMissileTier2.EntityMissileEMPStrong>of(EntityMissileTier2.EntityMissileEMPStrong::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_emp_strong"));

        BURST = ENTITY_TYPES.register("entity_missile_burst", () ->
                EntityType.Builder.<EntityMissileTier3.EntityMissileBurst>of(EntityMissileTier3.EntityMissileBurst::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_burst"));
        INFERNO = ENTITY_TYPES.register("entity_missile_inferno", () ->
                EntityType.Builder.<EntityMissileTier3.EntityMissileInferno>of(EntityMissileTier3.EntityMissileInferno::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_inferno"));
        RAIN = ENTITY_TYPES.register("entity_missile_rain", () ->
                EntityType.Builder.<EntityMissileTier3.EntityMissileRain>of(EntityMissileTier3.EntityMissileRain::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_rain"));
        DRILL = ENTITY_TYPES.register("entity_missile_drill", () ->
                EntityType.Builder.<EntityMissileTier3.EntityMissileDrill>of(EntityMissileTier3.EntityMissileDrill::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_drill"));

        NUCLEAR = ENTITY_TYPES.register("entity_missile_nuclear", () ->
                EntityType.Builder.<EntityMissileTier4.EntityMissileNuclear>of(EntityMissileTier4.EntityMissileNuclear::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_nuclear"));
        NUCLEAR_MIRV = ENTITY_TYPES.register("entity_missile_mirv", () ->
                EntityType.Builder.<EntityMissileTier4.EntityMissileMirv>of(EntityMissileTier4.EntityMissileMirv::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_mirv"));
        VOLCANO = ENTITY_TYPES.register("entity_missile_volcano", () ->
                EntityType.Builder.<EntityMissileTier4.EntityMissileVolcano>of(EntityMissileTier4.EntityMissileVolcano::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_volcano"));
        DOOMSDAY = ENTITY_TYPES.register("entity_missile_doomsday", () ->
                EntityType.Builder.<EntityMissileTier4.EntityMissileDoomsday>of(EntityMissileTier4.EntityMissileDoomsday::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_doomsday"));
        DOOMSDAY_RUSTED = ENTITY_TYPES.register("entity_missile_doomsday_rusted", () ->
                EntityType.Builder.<EntityMissileTier4.EntityMissileDoomsdayRusted>of(EntityMissileTier4.EntityMissileDoomsdayRusted::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_doomsday_rusted"));
        N2 = ENTITY_TYPES.register("entity_missile_n2", () ->
                EntityType.Builder.<EntityMissileTier4.EntityMissileN2>of(EntityMissileTier4.EntityMissileN2::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_n2"));

        STEALTH = ENTITY_TYPES.register("entity_missile_stealth", () ->
                EntityType.Builder.<EntityMissileStealth>of(EntityMissileStealth::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("entity_missile_stealth"));
        SHUTTLE = ENTITY_TYPES.register("missile_shuttle", () ->
                EntityType.Builder.<EntityMissileShuttle>of(EntityMissileShuttle::new, MobCategory.MISC)
                        .noSummon().sized(1.5F, 1.5F).setTrackingRange(1000).build("missile_shuttle"));

        ENTITY_TYPES.register(modEventBus);
    }
}
