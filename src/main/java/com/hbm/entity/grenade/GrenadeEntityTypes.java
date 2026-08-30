package com.hbm.entity.grenade;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@code com.hbm.entity.grenade}, following
 * {@code com.hbm.entity.ConveyorEntityTypes}/{@code com.hbm.entity.logic.NukeEntityTypes}'s
 * already-committed per-family {@link DeferredRegister} pattern (see
 * {@code docs/phase3/grenades.md}'s "Key design/API decisions" for the sizes/tracking ranges below,
 * ported from each entity's CE {@code @AutoRegister} annotation).
 * <p>
 * {@code EntityGrenadeBase} (the legacy family's abstract base) is never spawned directly, matching
 * CE, so it has no {@link EntityType} of its own - only its two concrete subclasses
 * ({@link EntityGrenadeBouncyGeneric}, {@link EntityDisperserCanister}) and
 * {@link EntityGrenadeImpactGeneric} are registered.
 */
public final class GrenadeEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityGrenadeUniversal>> GRENADE_UNIVERSAL;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGrenadeBouncyGeneric>> GRENADE_BOUNCY_GENERIC;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGrenadeImpactGeneric>> GRENADE_IMPACT_GENERIC;
    public static DeferredHolder<EntityType<?>, EntityType<EntityDisperserCanister>> DISPERSER_CANISTER;

    private GrenadeEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        GRENADE_UNIVERSAL = ENTITY_TYPES.register("entity_grenade_universal", () ->
                EntityType.Builder.<EntityGrenadeUniversal>of(EntityGrenadeUniversal::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.25F, 0.25F)
                        .setTrackingRange(64)
                        .build("entity_grenade_universal"));

        GRENADE_BOUNCY_GENERIC = ENTITY_TYPES.register("entity_grenade_bouncy_generic", () ->
                EntityType.Builder.<EntityGrenadeBouncyGeneric>of(EntityGrenadeBouncyGeneric::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.25F, 0.25F)
                        .build("entity_grenade_bouncy_generic"));

        GRENADE_IMPACT_GENERIC = ENTITY_TYPES.register("entity_grenade_impact_generic", () ->
                EntityType.Builder.<EntityGrenadeImpactGeneric>of(EntityGrenadeImpactGeneric::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.25F, 0.25F)
                        .build("entity_grenade_impact_generic"));

        DISPERSER_CANISTER = ENTITY_TYPES.register("entity_disperser", () ->
                EntityType.Builder.<EntityDisperserCanister>of(EntityDisperserCanister::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.25F, 0.25F)
                        .build("entity_disperser"));

        ENTITY_TYPES.register(modEventBus);
    }
}
