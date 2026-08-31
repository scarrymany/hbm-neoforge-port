package com.hbm.entity.effect;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for CE's gravity-well/singularity entity family
 * ({@link EntityBlackHole}/{@link EntityVortex}/{@link EntityRagingVortex}/{@link EntityQuasar}),
 * per docs/phase4/entities_vortex_gravity_wells.md's Key design/API decisions. A dedicated
 * {@link DeferredRegister} (not folded into the sibling {@link EffectEntityTypes}, to avoid a
 * same-wave merge conflict with whatever else touches that file this pass - see this report's own
 * recommendation), following the exact same per-family template.
 * <p>
 * All 4 classes register as fully independent {@link EntityType}s (CE's own
 * {@code @AutoRegister(name = "entity_black_hole"/"entity_vortex"/"entity_raging_vortex"/
 * "entity_digamma_quasar", trackingRange = 1000)}) - <b>not</b> folded into a single class the way
 * Neo Edition's {@code NtmEntityTypes} mistakenly does for {@code BLACK_HOLE}/{@code DIGAMMA_QUASAR}
 * (both constructing its one {@code BlackHole} class); CE keeps {@code EntityQuasar} a distinct class
 * with its own registry name, and this port's own "one CE class = one port class" convention follows
 * suit for save-file entity-type-identifier stability. {@code fireImmune()} replaces CE's per-instance
 * {@code this.isImmuneToFire = true} field (set in the constructor of all 4 CE classes); sizing is a
 * nominal 1x1 hitbox for all 4 (CE never gives this family a custom {@code setSize} - the "size" that
 * matters for gameplay is the synced {@code SIZE} data accessor, not the AABB hitbox).
 */
public final class GravityWellEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityBlackHole>> BLACK_HOLE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityVortex>> VORTEX;
    public static DeferredHolder<EntityType<?>, EntityType<EntityRagingVortex>> RAGING_VORTEX;
    public static DeferredHolder<EntityType<?>, EntityType<EntityQuasar>> QUASAR;

    private GravityWellEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BLACK_HOLE = ENTITY_TYPES.register("entity_black_hole", () ->
                EntityType.Builder.<EntityBlackHole>of(EntityBlackHole::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(1F, 1F)
                        .setTrackingRange(1000)
                        .build("entity_black_hole"));

        VORTEX = ENTITY_TYPES.register("entity_vortex", () ->
                EntityType.Builder.<EntityVortex>of(EntityVortex::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(1F, 1F)
                        .setTrackingRange(1000)
                        .build("entity_vortex"));

        RAGING_VORTEX = ENTITY_TYPES.register("entity_raging_vortex", () ->
                EntityType.Builder.<EntityRagingVortex>of(EntityRagingVortex::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(1F, 1F)
                        .setTrackingRange(1000)
                        .build("entity_raging_vortex"));

        QUASAR = ENTITY_TYPES.register("entity_digamma_quasar", () ->
                EntityType.Builder.<EntityQuasar>of(EntityQuasar::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(1F, 1F)
                        .setTrackingRange(1000)
                        .build("entity_digamma_quasar"));

        ENTITY_TYPES.register(modEventBus);
    }
}
