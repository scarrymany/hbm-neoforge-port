package com.hbm.entity.mob;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the BOTPrime worm boss's 2 entities
 * ({@link EntityBOTPrimeHead}/{@link EntityBOTPrimeBody}) - see
 * {@code docs/phase4/entities_bosses.md}'s worm-boss table. Follows the exact per-family
 * {@link DeferredRegister} pattern {@code com.hbm.entity.GunEntityTypes}/
 * {@code com.hbm.entity.mob.CreeperVariantEntityTypes} already established (no shared "mob entity
 * types" registry exists yet in this port). Registry names are CE's own {@code @AutoRegister(name =
 * ...)} strings verbatim ({@code entity_balls_o_tron}/{@code entity_balls_o_tron_seg}). Sizing (Head
 * 3x3, Body 2x2) matches CE's own {@code setSize} calls in each class's constructor - this port sets
 * size at the {@link EntityType.Builder} level instead of at runtime, since 1.21.1 entities have one
 * fixed size per registered type rather than CE's per-instance mutable size.
 * <p>
 * Named {@code WormEntityTypes} rather than a shared {@code BossEntityTypes} to avoid a same-file
 * collision with any sibling boss package landing in this same wave (MaskMan/UFO/Hunter Chopper are
 * explicitly out of this package's scope per its own task brief).
 */
public final class WormEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityBOTPrimeHead>> BOTPRIME_HEAD;
    public static DeferredHolder<EntityType<?>, EntityType<EntityBOTPrimeBody>> BOTPRIME_BODY;

    private WormEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        BOTPRIME_HEAD = ENTITY_TYPES.register("entity_balls_o_tron", () ->
                EntityType.Builder.<EntityBOTPrimeHead>of(EntityBOTPrimeHead::new, MobCategory.MONSTER)
                        .sized(3.0F, 3.0F)
                        .fireImmune()
                        .setTrackingRange(1000)
                        .build("entity_balls_o_tron"));

        BOTPRIME_BODY = ENTITY_TYPES.register("entity_balls_o_tron_seg", () ->
                EntityType.Builder.<EntityBOTPrimeBody>of(EntityBOTPrimeBody::new, MobCategory.MONSTER)
                        .sized(2.0F, 2.0F)
                        .fireImmune()
                        .setTrackingRange(1000)
                        .build("entity_balls_o_tron_seg"));

        ENTITY_TYPES.register(modEventBus);
    }
}
