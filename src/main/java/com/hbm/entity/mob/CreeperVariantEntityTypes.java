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
 * {@link EntityType} (+ spawn egg {@link Item}) registration for this package's 5 CE creeper
 * variants - see {@code docs/phase4/entities_creeper_variants.md}. Follows the exact
 * {@code com.hbm.entity.GunEntityTypes}/{@code com.hbm.entity.effect.EffectEntityTypes} per-family
 * {@link DeferredRegister} pattern (no shared "mob entity types" registry exists yet in this port).
 * Registry names are CE's own {@code @AutoRegister(name = ...)} strings verbatim, matching this
 * port's established convention of keeping CE's ids where sensible. Hitbox is 0.6x1.7 for all 5,
 * matching vanilla {@code Creeper} exactly (confirmed additionally by Neo Edition's own
 * {@code CreeperNuclear} registration using the identical size) - none of CE's 4 simple variants (or
 * Nuclear) override {@code setSize}.
 * <p>
 * <b>Spawn eggs</b> ({@link #registerSpawnEggs()}): this port's first, per the research report (no
 * {@code SpawnEggItem}/{@code MobCategory.MONSTER} precedent exists anywhere in this port or Neo
 * Edition to consult). CE's exact hex color pairs are carried over verbatim from its
 * {@code @AutoRegister} annotations. A plain vanilla {@code new SpawnEggItem(entityType, primary,
 * secondary, properties)} call cannot be used directly here: at the point this class's
 * {@link #register(IEventBus)} method runs (early in {@code MainRegistry}'s constructor, alongside
 * every sibling {@code *EntityTypes.register(modEventBus)} call - see this task's wiring snippet for
 * {@code MainRegistry.java}), none of this file's own {@link EntityType} {@link DeferredHolder}s are
 * resolvable yet (their {@code RegisterEvent} has not fired) - calling {@code .get()} on one here
 * would throw {@code IllegalStateException} (the exact recurring bug pattern this whole port has hit
 * repeatedly with eager {@code DeferredHolder.get()} field/constructor access). {@link LazySpawnEggItem}
 * (below) sidesteps this the same way a lazy static factory method would: it satisfies vanilla
 * {@code SpawnEggItem}'s own constructor with a harmless placeholder type at construction time, then
 * overrides {@link SpawnEggItem#getType(ItemStack)} - the method vanilla's own spawn-egg use/
 * interaction logic already dispatches through - to lazily resolve the real registered
 * {@link EntityType} only when the egg is actually used, by which point every registry has long since
 * finished firing its {@code RegisterEvent}. This sandbox has no compiled 1.21.1 jar to confirm
 * {@code getType(ItemStack)}'s exact override point against; if it has since been renamed/removed,
 * this one file (not the mobs themselves, which are already fully functional via {@code /summon})
 * would need adjusting - see {@code docs/phase4/entities_creeper_variants.md}'s "Open questions".
 */
public final class CreeperVariantEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityCreeperGold>> CREEPER_GOLD;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCreeperVolatile>> CREEPER_VOLATILE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCreeperPhosgene>> CREEPER_PHOSGENE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCreeperTainted>> CREEPER_TAINTED;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCreeperNuclear>> CREEPER_NUCLEAR;

    private CreeperVariantEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        CREEPER_GOLD = ENTITY_TYPES.register("entity_mob_gold_creeper", () ->
                EntityType.Builder.<EntityCreeperGold>of(EntityCreeperGold::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.7F)
                        .setTrackingRange(80)
                        .build("entity_mob_gold_creeper"));

        CREEPER_VOLATILE = ENTITY_TYPES.register("entity_mob_volatile_creeper", () ->
                EntityType.Builder.<EntityCreeperVolatile>of(EntityCreeperVolatile::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.7F)
                        .setTrackingRange(80)
                        .build("entity_mob_volatile_creeper"));

        CREEPER_PHOSGENE = ENTITY_TYPES.register("entity_mob_phosgene_creeper", () ->
                EntityType.Builder.<EntityCreeperPhosgene>of(EntityCreeperPhosgene::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.7F)
                        .setTrackingRange(80)
                        .build("entity_mob_phosgene_creeper"));

        CREEPER_TAINTED = ENTITY_TYPES.register("entity_tainted_creeper", () ->
                EntityType.Builder.<EntityCreeperTainted>of(EntityCreeperTainted::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.7F)
                        .setTrackingRange(80)
                        .build("entity_tainted_creeper"));

        CREEPER_NUCLEAR = ENTITY_TYPES.register("entity_nuclear_creeper", () ->
                EntityType.Builder.<EntityCreeperNuclear>of(EntityCreeperNuclear::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.7F)
                        .setTrackingRange(80)
                        .build("entity_nuclear_creeper"));

        registerSpawnEggs();

        ENTITY_TYPES.register(modEventBus);
    }

    private static void registerSpawnEggs() {
        ModItems.ITEMS.register("gold_creeper_spawn_egg", () ->
                new LazySpawnEggItem(CREEPER_GOLD::get, 0xECC136, 0x9E8B3E, new Item.Properties()));
        ModItems.ITEMS.register("volatile_creeper_spawn_egg", () ->
                new LazySpawnEggItem(CREEPER_VOLATILE::get, 0x000020, 0x2D2D72, new Item.Properties()));
        ModItems.ITEMS.register("phosgene_creeper_spawn_egg", () ->
                new LazySpawnEggItem(CREEPER_PHOSGENE::get, 0xE3D398, 0xB8A06B, new Item.Properties()));
        ModItems.ITEMS.register("tainted_creeper_spawn_egg", () ->
                new LazySpawnEggItem(CREEPER_TAINTED::get, 0x813b9b, 0xd71fdd, new Item.Properties()));
        ModItems.ITEMS.register("nuclear_creeper_spawn_egg", () ->
                new LazySpawnEggItem(CREEPER_NUCLEAR::get, 0x204131, 0x75CE00, new Item.Properties()));
    }

    /** See this class's own javadoc for why this exists instead of a plain {@code new SpawnEggItem(...)}. */
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
