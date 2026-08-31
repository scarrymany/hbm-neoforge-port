package com.hbm.entity.mob;

import com.hbm.entity.LazySpawnEggItem;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Remaining CE mobs vs this port (Phase 9). Registry names are CE {@code @AutoRegister} ids.
 * Must run before {@code ModItems.register} — spawn eggs land on {@code ModItems.ITEMS}.
 */
public final class Phase9MobEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityGlowingOne>> GLOWING_ONE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGhost>> GHOST;
    public static DeferredHolder<EntityType<?>, EntityType<EntityFBI>> FBI;
    public static DeferredHolder<EntityType<?>, EntityType<EntityFBIDrone>> FBI_DRONE;
    public static DeferredHolder<EntityType<?>, EntityType<EntityUndeadSoldier>> UNDEAD_SOLDIER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityPigeon>> PIGEON;
    public static DeferredHolder<EntityType<?>, EntityType<EntityPlasticBag>> PLASTIC_BAG;
    public static DeferredHolder<EntityType<?>, EntityType<EntityParasiteMaggot>> PARASITE_MAGGOT;
    public static DeferredHolder<EntityType<?>, EntityType<EntityBlockSpider>> BLOCK_SPIDER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityDummy>> DUMMY;

    private Phase9MobEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        GLOWING_ONE = ENTITY_TYPES.register("entity_glowing_one", () ->
                EntityType.Builder.<EntityGlowingOne>of(EntityGlowingOne::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .setTrackingRange(1000)
                        .build("entity_glowing_one"));

        GHOST = ENTITY_TYPES.register("entity_ntm_ghost", () ->
                EntityType.Builder.<EntityGhost>of(EntityGhost::new, MobCategory.CREATURE)
                        .sized(0.6F, 1.8F)
                        .setTrackingRange(1000)
                        .build("entity_ntm_ghost"));

        FBI = ENTITY_TYPES.register("entity_ntm_fbi", () ->
                EntityType.Builder.<EntityFBI>of(EntityFBI::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.8F)
                        .fireImmune()
                        .setTrackingRange(1000)
                        .build("entity_ntm_fbi"));

        FBI_DRONE = ENTITY_TYPES.register("entity_ntm_fbi_drone", () ->
                EntityType.Builder.<EntityFBIDrone>of(EntityFBIDrone::new, MobCategory.MONSTER)
                        .sized(1.0F, 0.5F)
                        .fireImmune()
                        .setTrackingRange(80)
                        .setUpdateInterval(3)
                        .build("entity_ntm_fbi_drone"));

        UNDEAD_SOLDIER = ENTITY_TYPES.register("entity_ntm_undead_soldier", () ->
                EntityType.Builder.<EntityUndeadSoldier>of(EntityUndeadSoldier::new, MobCategory.MONSTER)
                        .sized(0.6F, 1.95F)
                        .setTrackingRange(1000)
                        .build("entity_ntm_undead_soldier"));

        PIGEON = ENTITY_TYPES.register("entity_pigeon", () ->
                EntityType.Builder.<EntityPigeon>of(EntityPigeon::new, MobCategory.CREATURE)
                        .sized(0.5F, 1.0F)
                        .setTrackingRange(80)
                        .setUpdateInterval(3)
                        .build("entity_pigeon"));

        PLASTIC_BAG = ENTITY_TYPES.register("entity_plastic_bag", () ->
                EntityType.Builder.<EntityPlasticBag>of(EntityPlasticBag::new, MobCategory.WATER_CREATURE)
                        .sized(0.45F, 0.45F)
                        .setTrackingRange(64)
                        .build("entity_plastic_bag"));

        PARASITE_MAGGOT = ENTITY_TYPES.register("entity_parasite_maggot", () ->
                EntityType.Builder.<EntityParasiteMaggot>of(EntityParasiteMaggot::new, MobCategory.MONSTER)
                        .sized(0.3F, 0.7F)
                        .build("entity_parasite_maggot"));

        BLOCK_SPIDER = ENTITY_TYPES.register("entity_taintcrawler", () ->
                EntityType.Builder.<EntityBlockSpider>of(EntityBlockSpider::new, MobCategory.MONSTER)
                        .sized(0.95F, 1.25F)
                        .setTrackingRange(1000)
                        .build("entity_taintcrawler"));

        DUMMY = ENTITY_TYPES.register("entity_ntm_test_dummy", () ->
                EntityType.Builder.<EntityDummy>of(EntityDummy::new, MobCategory.MISC)
                        .sized(0.6F, 1.8F)
                        .setTrackingRange(80)
                        .setUpdateInterval(3)
                        .build("entity_ntm_test_dummy"));

        ModItems.ITEMS.register("glowing_one_spawn_egg", () ->
                new LazySpawnEggItem(GLOWING_ONE::get, 0x00FF00, 0x303030, new Item.Properties()));
        ModItems.ITEMS.register("fbi_spawn_egg", () ->
                new LazySpawnEggItem(FBI::get, 0x008000, 0x404040, new Item.Properties()));
        ModItems.ITEMS.register("fbi_drone_spawn_egg", () ->
                new LazySpawnEggItem(FBI_DRONE::get, 0x008000, 0x404040, new Item.Properties()));
        ModItems.ITEMS.register("undead_soldier_spawn_egg", () ->
                new LazySpawnEggItem(UNDEAD_SOLDIER::get, 0x749F30, 0x6C5B44, new Item.Properties()));
        ModItems.ITEMS.register("pigeon_spawn_egg", () ->
                new LazySpawnEggItem(PIGEON::get, 0xC8C9CD, 0x858894, new Item.Properties()));
        ModItems.ITEMS.register("parasite_maggot_spawn_egg", () ->
                new LazySpawnEggItem(PARASITE_MAGGOT::get, 0xd0d0d0, 0x808080, new Item.Properties()));
        ModItems.ITEMS.register("taintcrawler_spawn_egg", () ->
                new LazySpawnEggItem(BLOCK_SPIDER::get, 0x220022, 0x00FF00, new Item.Properties()));
        ModItems.ITEMS.register("test_dummy_spawn_egg", () ->
                new LazySpawnEggItem(DUMMY::get, 0xffffff, 0x000000, new Item.Properties()));

        ENTITY_TYPES.register(modEventBus);
    }
}
