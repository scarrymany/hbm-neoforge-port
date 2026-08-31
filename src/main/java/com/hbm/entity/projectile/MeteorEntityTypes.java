package com.hbm.entity.projectile;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@link EntityMeteor} - its own {@link DeferredRegister},
 * following the same per-family pattern as {@code com.hbm.entity.projectile.RubbleEntityTypes}/
 * {@code FallingNukeEntityTypes} (no shared {@code ModEntityTypes} registry exists yet). CE's
 * {@code @AutoRegister(name = "entity_meteor", trackingRange = 1000)} supplies the id/tracking range;
 * {@code sized(4F, 4F)} comes from CE's own constructor {@code this.setSize(4F, 4F)} call;
 * {@code fireImmune()} comes from CE's {@code this.isImmuneToFire = true}.
 */
public final class MeteorEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityMeteor>> METEOR;

    private MeteorEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        METEOR = ENTITY_TYPES.register("entity_meteor", () ->
                EntityType.Builder.<EntityMeteor>of(EntityMeteor::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(4F, 4F)
                        .setTrackingRange(1000)
                        .build("entity_meteor"));

        ENTITY_TYPES.register(modEventBus);
    }
}
