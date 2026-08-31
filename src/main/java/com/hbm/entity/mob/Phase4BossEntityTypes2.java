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
 * {@link EntityType} (+ spawn egg {@link Item}) registration for this task's package: the UFO boss,
 * Hunter Chopper, and the Cyber Crab mob family, plus {@link EntityDuck}/{@link EntityQuackos} (the
 * duck-mutation pseudo-boss and its own prerequisite base mob) - see {@code docs/phase4/
 * entities_bosses.md} and {@code docs/phase4/entities_vehicles_aircraft.md}. Follows the exact
 * per-family {@link DeferredRegister} + lazy-spawn-egg pattern {@code MaskmanEntityTypes}/
 * {@code WormEntityTypes}/{@code CreeperVariantEntityTypes} already established. Named
 * {@code Phase4BossEntityTypes2} (not folded into any of those 3 sibling files, and not named
 * {@code UFOEntityTypes}/{@code BossEntityTypes}) specifically to avoid a same-file collision with
 * sibling boss/mob-content agents editing this same package concurrently in this wave, per this
 * task's own instruction - this file covers 7 entities, which is a lot for one registry class, but
 * splitting further would only multiply collision surface without reducing it.
 * <p>
 * Registry names are CE's own {@code @AutoRegister(name = ...)} strings verbatim
 * ({@code entity_ntm_ufo}/{@code entity_hunter_chopper}/{@code entity_cyber_crab}/
 * {@code entity_taint_crab}/{@code entity_tesla_crab}/{@code entity_fucc_a_ducc}/{@code
 * entity_elder_one}). Hitboxes: UFO 15x4, Hunter Chopper 8.25x3 (both from CE's own {@code setSize}
 * calls), Cyber Crab 0.75x0.35, Taint Crab 1.25x1.25, Tesla Crab 0.75x1.25, Duck 0.4x0.7 (vanilla
 * Chicken's own size, since CE's {@code EntityDuck} never overrides it), Quackos 7.5x17.5 (CE's own
 * {@code 0.3F*25, 0.7F*25} scale-up, hardcoded directly rather than derived from Duck's real size,
 * matching CE's own literal override call).
 * <p>
 * <b>UFO has no spawn egg</b> - CE's own {@code @AutoRegister} for {@code EntityUFO} carries no
 * {@code eggColors} (unlike every other entity here), matching the design that its only spawn path is
 * {@code ItemChopper}'s {@code spawn_ufo} variant - see {@link EntityUFO}'s own javadoc.
 */
public final class Phase4BossEntityTypes2 {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityUFO>> UFO;
    public static DeferredHolder<EntityType<?>, EntityType<EntityHunterChopper>> HUNTER_CHOPPER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityCyberCrab>> CYBER_CRAB;
    public static DeferredHolder<EntityType<?>, EntityType<EntityTaintCrab>> TAINT_CRAB;
    public static DeferredHolder<EntityType<?>, EntityType<EntityTeslaCrab>> TESLA_CRAB;
    public static DeferredHolder<EntityType<?>, EntityType<EntityDuck>> DUCK;
    public static DeferredHolder<EntityType<?>, EntityType<EntityQuackos>> QUACKOS;

    private Phase4BossEntityTypes2() {
    }

    public static void register(IEventBus modEventBus) {
        UFO = ENTITY_TYPES.register("entity_ntm_ufo", () ->
                EntityType.Builder.<EntityUFO>of(EntityUFO::new, MobCategory.MONSTER)
                        .sized(15.0F, 4.0F)
                        .fireImmune()
                        .setTrackingRange(1000)
                        .build("entity_ntm_ufo"));

        HUNTER_CHOPPER = ENTITY_TYPES.register("entity_hunter_chopper", () ->
                EntityType.Builder.<EntityHunterChopper>of(EntityHunterChopper::new, MobCategory.MONSTER)
                        .sized(8.25F, 3.0F)
                        .fireImmune()
                        .setTrackingRange(1000)
                        .build("entity_hunter_chopper"));

        CYBER_CRAB = ENTITY_TYPES.register("entity_cyber_crab", () ->
                EntityType.Builder.<EntityCyberCrab>of(EntityCyberCrab::new, MobCategory.MONSTER)
                        .sized(0.75F, 0.35F)
                        .build("entity_cyber_crab"));

        TAINT_CRAB = ENTITY_TYPES.register("entity_taint_crab", () ->
                EntityType.Builder.<EntityTaintCrab>of(EntityTaintCrab::new, MobCategory.MONSTER)
                        .sized(1.25F, 1.25F)
                        .build("entity_taint_crab"));

        TESLA_CRAB = ENTITY_TYPES.register("entity_tesla_crab", () ->
                EntityType.Builder.<EntityTeslaCrab>of(EntityTeslaCrab::new, MobCategory.MONSTER)
                        .sized(0.75F, 1.25F)
                        .build("entity_tesla_crab"));

        DUCK = ENTITY_TYPES.register("entity_fucc_a_ducc", () ->
                EntityType.Builder.<EntityDuck>of(EntityDuck::new, MobCategory.CREATURE)
                        .sized(0.4F, 0.7F)
                        .setTrackingRange(1000)
                        .build("entity_fucc_a_ducc"));

        QUACKOS = ENTITY_TYPES.register("entity_elder_one", () ->
                EntityType.Builder.<EntityQuackos>of(EntityQuackos::new, MobCategory.CREATURE)
                        .sized(7.5F, 17.5F)
                        .setTrackingRange(1000)
                        .build("entity_elder_one"));

        ModItems.ITEMS.register("hunter_chopper_spawn_egg", () ->
                new LazySpawnEggItem(HUNTER_CHOPPER::get, 0x000020, 0x2D2D72, new Item.Properties()));
        ModItems.ITEMS.register("cyber_crab_spawn_egg", () ->
                new LazySpawnEggItem(CYBER_CRAB::get, 0xAAAAAA, 0x444444, new Item.Properties()));
        ModItems.ITEMS.register("taint_crab_spawn_egg", () ->
                new LazySpawnEggItem(TAINT_CRAB::get, 0xAAAAAA, 0xFF00FF, new Item.Properties()));
        ModItems.ITEMS.register("tesla_crab_spawn_egg", () ->
                new LazySpawnEggItem(TESLA_CRAB::get, 0xAAAAAA, 0x440000, new Item.Properties()));
        ModItems.ITEMS.register("duck_spawn_egg", () ->
                new LazySpawnEggItem(DUCK::get, 0xd0d0d0, 0xFFBF00, new Item.Properties()));
        ModItems.ITEMS.register("quackos_spawn_egg", () ->
                new LazySpawnEggItem(QUACKOS::get, 0xd0d0d0, 0xFFBF00, new Item.Properties()));

        ENTITY_TYPES.register(modEventBus);
    }

    /** See {@code MaskmanEntityTypes.LazySpawnEggItem}'s own javadoc for why this exists instead of a
     *  plain {@code new SpawnEggItem(...)} - a private, file-local copy rather than a shared one to
     *  keep this file collision-free from that one, per this class's own javadoc. */
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
