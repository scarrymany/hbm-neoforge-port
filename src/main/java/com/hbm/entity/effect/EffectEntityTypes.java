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
 */
public final class EffectEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityNukeTorex>> TOREX;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCloudFleija>> CLOUD_FLEIJA;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCloudSolinium>> CLOUD_SOLINIUM;
    public static DeferredHolder<EntityType<?>, EntityType<EntityEMPBlast>> EMP_BLAST;

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

        ENTITY_TYPES.register(modEventBus);
    }
}
