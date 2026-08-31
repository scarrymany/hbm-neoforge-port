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
 * {@link EntityType} (+ spawn egg {@link Item}) registration for {@link EntityRADBeast} - see
 * {@code docs/phase4/entities_bosses.md}'s "RAD Beast" section. Follows
 * {@code com.hbm.entity.mob.MaskmanEntityTypes}' own per-family {@link DeferredRegister} + lazy-spawn-egg
 * pattern exactly (kept as its own file for the same collision-avoidance reason that class documents).
 * <p>
 * Registry name ({@code entity_ntm_radiation_blaze}), tracking range (1000), egg colors
 * ({@code 0x303030}/{@code 0x008000}), and fire immunity all come directly from CE's own
 * {@code @AutoRegister(name = "entity_ntm_radiation_blaze", trackingRange = 1000, eggColors =
 * {0x303030, 0x008000})} annotation and constructor body (CE never calls {@code setSize} - this port
 * uses a Zombie-equivalent 0.6x1.95 hitbox as a reasonable default absent a CE-specified value).
 */
public final class RadBeastEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityRADBeast>> RAD_BEAST;

    private RadBeastEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        RAD_BEAST = ENTITY_TYPES.register("entity_ntm_radiation_blaze", () ->
                EntityType.Builder.<EntityRADBeast>of(EntityRADBeast::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .setTrackingRange(1000)
                        .fireImmune()
                        .build("entity_ntm_radiation_blaze"));

        ModItems.ITEMS.register("rad_beast_spawn_egg", () ->
                new LazySpawnEggItem(RAD_BEAST::get, 0x303030, 0x008000, new Item.Properties()));

        ENTITY_TYPES.register(modEventBus);
    }

    /** See {@code MaskmanEntityTypes.LazySpawnEggItem}'s own javadoc - a private, file-local copy. */
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
