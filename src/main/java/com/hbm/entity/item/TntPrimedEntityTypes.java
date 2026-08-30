package com.hbm.entity.item;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@link EntityTNTPrimedBase} - its own {@link DeferredRegister},
 * following the same per-family pattern already established by {@code com.hbm.entity.
 * ConveyorEntityTypes}/{@code com.hbm.entity.GunEntityTypes}/{@code com.hbm.entity.projectile.
 * FallingNukeEntityTypes} (no shared {@code ModEntityTypes} registry exists yet). CE's
 * {@code @AutoRegister(name = "entity_ntm_tnt_primed", trackingRange = 256)} supplies the id/tracking
 * range; sizing (0.98x0.98) comes from CE's own {@code this.setSize(0.98F, 0.98F)} call in the
 * no-arg constructor.
 */
public final class TntPrimedEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityTNTPrimedBase>> TNT_PRIMED;

    private TntPrimedEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        TNT_PRIMED = ENTITY_TYPES.register("entity_ntm_tnt_primed", () ->
                EntityType.Builder.<EntityTNTPrimedBase>of(EntityTNTPrimedBase::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.98F)
                        .setTrackingRange(256)
                        .build("entity_ntm_tnt_primed"));

        ENTITY_TYPES.register(modEventBus);
    }
}
