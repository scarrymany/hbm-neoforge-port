package com.hbm.entity.effect;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for this pass's {@code com.hbm.entity.effect} nuke/detonation
 * companion VFX entities - {@link EntityNukeTorex}, {@link EntityCloudFleija}, {@link
 * EntityCloudSolinium}, {@link EntityEMPBlast} - following the same per-family {@link
 * DeferredRegister} pattern as {@code com.hbm.entity.ConveyorEntityTypes}/{@code
 * com.hbm.entity.logic.NukeEntityTypes}. Sizes/tracking ranges are ported from each class's CE
 * {@code @AutoRegister}/constructor {@code setSize} call (see each entity class's own javadoc for
 * the one deliberate exception - {@code EntityCloudFleija}/{@code EntityCloudSolinium}'s
 * never-actually-called size-(1,4) constructor overload is not registered).
 * <p>
 * CE's registry name for the Solinium cloud has a typo ({@code "entity_clound_solinium"}); this
 * port uses the corrected spelling ({@code "entity_cloud_solinium"}) since this is a brand-new
 * registry with no existing save data to stay byte-compatible with.
 * <p>
 * {@link EntityFireLingering}/{@link EntityMist} (Phase 4,
 * {@code docs/phase4/entities_orbital_and_beam_payloads.md}) join this same family/file per that
 * report's Key design decisions - both are real area-denial/area-effect gameplay entities, not VFX,
 * but share this package and this exact registration shape. Neither entity relies on its registered
 * hitbox for real collision (both build their own per-tick scan {@code AABB} from synced width/
 * height fields - see each class's javadoc), so the sizes below are nominal defaults matching each
 * entity's typical real-world call-site area, not a hard gameplay constraint.
 */
public final class EffectEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityNukeTorex>> TOREX;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCloudFleija>> CLOUD_FLEIJA;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCloudSolinium>> CLOUD_SOLINIUM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityEMPBlast>> EMP_BLAST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityFireLingering>> FIRE_LINGERING;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMist>> MIST;

    private EffectEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        TOREX = ENTITY_TYPES.register("entity_effect_torex", () ->
                EntityType.Builder.<EntityNukeTorex>of(EntityNukeTorex::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(20F, 40F)
                        .setTrackingRange(1000)
                        .build("entity_effect_torex"));

        CLOUD_FLEIJA = ENTITY_TYPES.register("entity_cloud_fleija", () ->
                EntityType.Builder.<EntityCloudFleija>of(EntityCloudFleija::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(20F, 40F)
                        .setTrackingRange(1000)
                        .build("entity_cloud_fleija"));

        CLOUD_SOLINIUM = ENTITY_TYPES.register("entity_cloud_solinium", () ->
                EntityType.Builder.<EntityCloudSolinium>of(EntityCloudSolinium::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(20F, 40F)
                        .setTrackingRange(1000)
                        .build("entity_cloud_solinium"));

        EMP_BLAST = ENTITY_TYPES.register("entity_emp_blast", () ->
                EntityType.Builder.<EntityEMPBlast>of(EntityEMPBlast::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(1.5F, 1.5F)
                        .setTrackingRange(1000)
                        .build("entity_emp_blast"));

        // CE's @AutoRegister(name = "entity_fire_lingering", sendVelocityUpdates = false) - fire
        // immune (it IS fire) and nominal-sized per this class's javadoc.
        FIRE_LINGERING = ENTITY_TYPES.register("entity_fire_lingering", () ->
                EntityType.Builder.<EntityFireLingering>of(EntityFireLingering::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(6F, 2F)
                        .setTrackingRange(256)
                        .build("entity_fire_lingering"));

        // CE's @AutoRegister(name = "entity_mist", trackingRange = 1000).
        MIST = ENTITY_TYPES.register("entity_mist", () ->
                EntityType.Builder.<EntityMist>of(EntityMist::new, MobCategory.MISC)
                        .noSummon()
                        .sized(10F, 5F)
                        .setTrackingRange(1000)
                        .build("entity_mist"));

        ENTITY_TYPES.register(modEventBus);
    }
}
