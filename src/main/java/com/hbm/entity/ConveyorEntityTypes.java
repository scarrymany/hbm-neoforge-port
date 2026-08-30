package com.hbm.entity;

import com.hbm.entity.item.EntityMovingItem;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * {@link EntityType} registration for the conveyor-belt moving-object entities
 * (see {@code docs/phase2/blocks_network_conveyor_crane.md}). This is the first entity registered
 * anywhere in this port - no shared {@code com.hbm.entity.ModEntityTypes}-style central registry
 * existed yet (unlike {@code ModBlocks}/{@code ModItems}), so this family owns its own
 * {@link DeferredRegister} rather than adding one to either of those two files, which are being
 * edited by many other Phase 2 agents in this same wave. A later area introducing many more entity
 * families is free to either keep following this same per-family pattern or fold every
 * {@code DeferredRegister<EntityType<?>>} into one shared class the way block/item registration
 * eventually did - both are valid NeoForge shapes (multiple {@code DeferredRegister}s targeting the
 * same underlying registry coexist without conflict), and that call is left for whoever lands next.
 * <p>
 * {@code EntityMovingConveyorObject} itself is abstract and never spawned directly, so it has no
 * {@link EntityType} of its own - matching CE, which only {@code @AutoRegister}s the concrete
 * {@code EntityMovingItem}/{@code EntityMovingPackage} subclasses. {@code EntityMovingPackage} is not
 * ported in this pass (only crane blocks - out of this task's scope - ever spawn it; see the research
 * report's "Deferred scope" section), so only {@link EntityMovingItem} is registered here.
 * <p>
 * Sizing/tracking values are ported directly from CE's
 * {@code @AutoRegister(name = "entity_c_item", trackingRange = 1000)} annotation and the
 * constructor's {@code this.setSize(0.375F, 0.375F)} call, translated to the 1.21.1
 * {@code EntityType.Builder} call shape confirmed by Neo Edition's own {@code NtmEntityTypes}
 * (e.g. its {@code TOM}/{@code BULLET_BEAM} entries use this exact
 * {@code .sized(...).setTrackingRange(...).build(name)} pattern).
 */
public final class ConveyorEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityMovingItem>> MOVING_ITEM;

    private ConveyorEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MOVING_ITEM = ENTITY_TYPES.register("entity_c_item", () ->
                EntityType.Builder.<EntityMovingItem>of(EntityMovingItem::new, MobCategory.MISC)
                        .noSummon()
                        .sized(0.375F, 0.375F)
                        .setTrackingRange(1000)
                        .build("entity_c_item"));

        ENTITY_TYPES.register(modEventBus);
    }
}
