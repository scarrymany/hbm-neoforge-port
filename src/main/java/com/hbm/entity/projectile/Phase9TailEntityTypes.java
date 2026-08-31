package com.hbm.entity.projectile;

import com.hbm.entity.logic.EntityWaypoint;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Remaining CE {@code @AutoRegister} entities that were still missing after Phase 9 mobs.
 * Named ports keep their CE class; everything else is {@link EntityThrownTail} / {@link EntityLogicTail}.
 * Cite: each CE {@code @AutoRegister(name=...)} on the matching {@code Entity*} file.
 */
public final class Phase9TailEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityAcidBomb>> ACID_BOMB;
    public static DeferredHolder<EntityType<?>, EntityType<EntityChemical>> CHEMICAL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityShrapnel>> SHRAPNEL;
    public static DeferredHolder<EntityType<?>, EntityType<EntitySoyuz>> SOYUZ;
    public static DeferredHolder<EntityType<?>, EntityType<EntitySoyuzCapsule>> SOYUZ_CAPSULE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityWaypoint>> WAYPOINT;

    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> SAWBLADE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> RAINBOW;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> MINI_NUKE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> PLASMA_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> LASER_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> LASER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> ZIRNOX_DEBRIS;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> FIRE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> ROCKET;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> RBMK_DEBRIS;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> MINI_MIRV;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> SCHRAB;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> TORPEDO;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> MINER_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> SIEGE_LASER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> LN2;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> SPARK_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> MOD_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> RAILGUN_PELLET;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> BULLET;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> DUCHESSGAMBIT;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> BUILDING;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> EXPLOSIVE_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> AA_SHELL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> ZETA;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> ARTILLERY_ROCKET;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> BULLET_MK2;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> BOXCAR;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> BURNING_FOEQ;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> ARTILLERY_SHELL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> COMBINE_BALL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> COG;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> VORTEX_BEAM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> DISCHARGE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> BULLET_MK3;

    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> BOBMAZON;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> SELENA;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> MINER_ROCKET;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> C_PACKAGE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> ITEM_WASTE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> FIREWORK_BALL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> ITEM_BUOYANT;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> MINECART_TEST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> WASTE_PEARL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> SPEAR;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> MOD_FX_SHADOW;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> FLEIJA_RAINBOW;
    public static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> EMP;

    private Phase9TailEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        ACID_BOMB = thrownNamed("entity_acid_bomb", EntityAcidBomb::new, 0.25F, 0.25F, 1000);
        CHEMICAL = ENTITY_TYPES.register("entity_chemthrower_splash", () ->
                EntityType.Builder.<EntityChemical>of(EntityChemical::new, MobCategory.MISC)
                        .noSummon().fireImmune().sized(0.25F, 0.25F).setTrackingRange(1000)
                        .build("entity_chemthrower_splash"));
        SHRAPNEL = thrownNamed("entity_shrapnel", EntityShrapnel::new, 0.25F, 0.25F, 1000);
        SOYUZ = ENTITY_TYPES.register("entity_soyuz", () ->
                EntityType.Builder.<EntitySoyuz>of(EntitySoyuz::new, MobCategory.MISC)
                        .noSummon().sized(5.0F, 50.0F).setTrackingRange(1000)
                        .build("entity_soyuz"));
        SOYUZ_CAPSULE = ENTITY_TYPES.register("entity_soyuz_capsule", () ->
                EntityType.Builder.<EntitySoyuzCapsule>of(EntitySoyuzCapsule::new, MobCategory.MISC)
                        .noSummon().fireImmune().sized(1.0F, 1.0F).setTrackingRange(1000)
                        .build("entity_soyuz_capsule"));
        WAYPOINT = ENTITY_TYPES.register("entity_waypoint", () ->
                EntityType.Builder.<EntityWaypoint>of(EntityWaypoint::new, MobCategory.MISC)
                        .noSummon().fireImmune().sized(0.5F, 0.5F)
                        .setShouldReceiveVelocityUpdates(false)
                        .build("entity_waypoint"));

        SAWBLADE = thrown("entity_sawblade");
        RAINBOW = thrown("entity_rainbow");
        MINI_NUKE = thrown("entity_mini_nuke");
        PLASMA_BEAM = thrown("entity_plasma_beam");
        LASER_BEAM = thrown("entity_laser_beam");
        LASER = thrown("entity_laser");
        ZIRNOX_DEBRIS = thrown("entity_zirnox_debris");
        FIRE = thrown("entity_fire");
        ROCKET = thrown("entity_rocket");
        RBMK_DEBRIS = thrown("entity_rbmk_debris");
        MINI_MIRV = thrown("entity_mini_mirv");
        SCHRAB = thrown("entity_schrab");
        TORPEDO = thrown("entity_torpedo");
        MINER_BEAM = thrown("entity_miner_beam");
        SIEGE_LASER = thrown("entity_ntm_siege_laser");
        LN2 = thrown("entity_ln2");
        SPARK_BEAM = thrown("entity_spark_beam");
        MOD_BEAM = thrown("entity_mod_beam");
        RAILGUN_PELLET = thrown("entity_railgun_pellet");
        BULLET = thrown("entity_bullet");
        DUCHESSGAMBIT = thrown("entity_duchessgambit");
        BUILDING = thrown("entity_building");
        EXPLOSIVE_BEAM = thrown("entity_explosive_beam");
        AA_SHELL = thrown("entity_aa_shell");
        ZETA = thrown("entity_zeta");
        ARTILLERY_ROCKET = thrown("entity_artillery_rocket");
        BULLET_MK2 = thrown("entity_bullet_mk2");
        BOXCAR = thrown("entity_boxcar");
        BURNING_FOEQ = thrown("entity_burning_foeq");
        ARTILLERY_SHELL = thrown("entity_artillery_shell");
        COMBINE_BALL = thrown("entity_combine_ball");
        COG = thrown("entity_cog");
        VORTEX_BEAM = thrown("entity_vortex_beam");
        DISCHARGE = thrown("entity_discharge");
        BULLET_MK3 = thrown("entity_bullet_mk3");

        BOBMAZON = logic("entity_bobmazon", 1000);
        SELENA = logic("entity_selena", 1000);
        MINER_ROCKET = logic("entity_miner_rocket", 1000);
        C_PACKAGE = logic("entity_c_package", 1000);
        ITEM_WASTE = logic("entity_item_waste", 64);
        FIREWORK_BALL = logic("entity_firework_ball", 1000);
        ITEM_BUOYANT = logic("entity_item_buoyant", 100);
        MINECART_TEST = logic("entity_minecart_test", 1000);
        WASTE_PEARL = logic("entity_waste_pearl", 64);
        SPEAR = logic("entity_spear", 1000);
        MOD_FX_SHADOW = logic("entity_mod_fx_shadow", 0);
        FLEIJA_RAINBOW = logic("entity_fleija_rainbow", 1000);
        EMP = logic("entity_emp", 1000);

        ENTITY_TYPES.register(modEventBus);
    }

    private static DeferredHolder<EntityType<?>, EntityType<EntityThrownTail>> thrown(String id) {
        return ENTITY_TYPES.register(id, () ->
                EntityType.Builder.<EntityThrownTail>of(EntityThrownTail::new, MobCategory.MISC)
                        .noSummon().sized(0.25F, 0.25F).setTrackingRange(1000).build(id));
    }

    private static <T extends net.minecraft.world.entity.Entity> DeferredHolder<EntityType<?>, EntityType<T>> thrownNamed(
            String id, EntityType.EntityFactory<T> factory, float w, float h, int range) {
        return ENTITY_TYPES.register(id, () ->
                EntityType.Builder.of(factory, MobCategory.MISC)
                        .noSummon().sized(w, h).setTrackingRange(range).build(id));
    }

    private static DeferredHolder<EntityType<?>, EntityType<EntityLogicTail>> logic(String id, int range) {
        return ENTITY_TYPES.register(id, () ->
                EntityType.Builder.<EntityLogicTail>of(EntityLogicTail::new, MobCategory.MISC)
                        .noSummon().fireImmune().sized(0.5F, 0.5F)
                        .setTrackingRange(range)
                        .setShouldReceiveVelocityUpdates(false)
                        .build(id));
    }
}
