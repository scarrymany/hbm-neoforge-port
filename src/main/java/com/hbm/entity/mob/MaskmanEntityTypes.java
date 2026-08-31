package com.hbm.entity.mob;

import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.Nullable;
import java.util.function.Supplier;

/**
 * {@link EntityType} (+ spawn egg {@link Item}) registration for {@link EntityMaskMan} - see
 * {@code docs/phase4/entities_bosses.md}. Follows {@code com.hbm.entity.mob.CreeperVariantEntityTypes}'
 * own per-family {@link DeferredRegister} + lazy-spawn-egg pattern exactly (see that class's own
 * javadoc for why a plain {@code new SpawnEggItem(entityType, ...)} can't be used directly at this
 * registration point). Kept as its own file - not folded into {@code CreeperVariantEntityTypes} -
 * specifically to avoid a same-file collision with sibling boss/mob-content agents editing that file
 * concurrently in this same wave.
 * <p>
 * Registry name ({@code entity_mask_man}) and hitbox (2x5, CE: {@code this.setSize(2F, 5F)}) and
 * tracking range (1000) and fire immunity all come directly from CE's own
 * {@code @AutoRegister(name = "entity_mask_man", trackingRange = 1000, eggColors = {0x818572,
 * 0xC7C1B7})} annotation and constructor body.
 */
public final class MaskmanEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityMaskMan>> MASK_MAN;

    private MaskmanEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        MASK_MAN = ENTITY_TYPES.register("entity_mask_man", () ->
                EntityType.Builder.<EntityMaskMan>of(EntityMaskMan::new, MobCategory.MONSTER)
                        .sized(2.0F, 5.0F)
                        .setTrackingRange(1000)
                        .fireImmune()
                        .build("entity_mask_man"));

        ModItems.ITEMS.register("mask_man_spawn_egg", () ->
                new LazySpawnEggItem(MASK_MAN::get, 0x818572, 0xC7C1B7, new Item.Properties()));

        ENTITY_TYPES.register(modEventBus);
    }

    /** See {@code CreeperVariantEntityTypes.LazySpawnEggItem}'s own javadoc for why this exists
     *  instead of a plain {@code new SpawnEggItem(...)} - a private, file-local copy rather than a
     *  shared one to keep this file collision-free from that one, per this class's own javadoc. */
    private static final class LazySpawnEggItem extends SpawnEggItem {

        private final Supplier<EntityType<?>> realType;

        LazySpawnEggItem(Supplier<EntityType<?>> realType, int primaryColor, int secondaryColor, Item.Properties properties) {
            // Placeholder only - never actually spawns a pig, see getType(ItemStack) below.
            super(EntityType.PIG, primaryColor, secondaryColor, properties);
            this.realType = realType;
        }

        @Override
        public EntityType<?> getType(@Nullable ItemStack stack) {
            return this.realType.get();
        }
    }
}
