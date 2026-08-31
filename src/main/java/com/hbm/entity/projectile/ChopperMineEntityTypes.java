package com.hbm.entity.projectile;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for {@link EntityChopperMine} - {@link com.hbm.entity.mob.
 * EntityHunterChopper}'s dropped proximity mine (see {@code docs/phase4/entities_bosses.md}'s Hunter
 * Chopper row). Its own {@link DeferredRegister}, following the same per-family pattern as
 * {@code RubbleEntityTypes}/{@code FallingNukeEntityTypes} in this same package (no shared
 * {@code ModEntityTypes} registry exists yet). CE's own {@code @AutoRegister(name =
 * "entity_chopper_mine", trackingRange = 1000)} and {@code setSize(12, 12)} supply the id/tracking
 * range/hitbox.
 */
public final class ChopperMineEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityChopperMine>> CHOPPER_MINE;

    private ChopperMineEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        CHOPPER_MINE = ENTITY_TYPES.register("entity_chopper_mine", () ->
                EntityType.Builder.<EntityChopperMine>of(EntityChopperMine::new, MobCategory.MISC)
                        .noSummon()
                        .fireImmune()
                        .sized(12.0F, 12.0F)
                        .setTrackingRange(1000)
                        .build("entity_chopper_mine"));

        ENTITY_TYPES.register(modEventBus);
    }
}
