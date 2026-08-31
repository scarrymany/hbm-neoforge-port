package com.hbm.entity.cart;

import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the reskinned-vanilla-minecart family ({@code
 * com.hbm.entity.cart}, Phase 4 {@code docs/phase4/entities_vehicles_aircraft.md}), following the
 * same per-family {@link DeferredRegister} pattern as {@code com.hbm.entity.ConveyorEntityTypes}/
 * {@code com.hbm.entity.logic.NukeEntityTypes}. Sized {@code (0.98F, 0.7F)} - vanilla's own real
 * minecart hitbox dimensions (CE never overrides cart size in any of these 5 classes, confirmed by
 * reading every file in full). None of these are {@code .noSummon()}-exempted in the sense of being
 * player-summonable via vanilla commands right now - matching every other registry in this port,
 * {@code .noSummon()} is set uniformly since there is no placement item wired up yet (see {@code
 * com.hbm.items.tool.CartItems}'s own javadoc for that real, separate gap).
 */
public final class CartEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityMinecartOre>> CART_ORE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMinecartPowder>> CART_POWDER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMinecartSemtex>> CART_SEMTEX;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMinecartCrate>> CART_CRATE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityMinecartDestroyer>> CART_DESTROYER;

    private CartEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        CART_ORE = ENTITY_TYPES.register("entity_ntm_cart_ore", () ->
                EntityType.Builder.<EntityMinecartOre>of(EntityMinecartOre::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.7F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cart_ore"));

        CART_POWDER = ENTITY_TYPES.register("entity_ntm_cart_powder", () ->
                EntityType.Builder.<EntityMinecartPowder>of(EntityMinecartPowder::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.7F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cart_powder"));

        CART_SEMTEX = ENTITY_TYPES.register("entity_ntm_cart_semtex", () ->
                EntityType.Builder.<EntityMinecartSemtex>of(EntityMinecartSemtex::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.7F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cart_semtex"));

        CART_CRATE = ENTITY_TYPES.register("entity_ntm_cart_crate", () ->
                EntityType.Builder.<EntityMinecartCrate>of(EntityMinecartCrate::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.7F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cart_crate"));

        CART_DESTROYER = ENTITY_TYPES.register("entity_ntm_cart_destroyer", () ->
                EntityType.Builder.<EntityMinecartDestroyer>of(EntityMinecartDestroyer::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.98F, 0.7F)
                        .setTrackingRange(250)
                        .build("entity_ntm_cart_destroyer"));

        ENTITY_TYPES.register(modEventBus);
    }
}
