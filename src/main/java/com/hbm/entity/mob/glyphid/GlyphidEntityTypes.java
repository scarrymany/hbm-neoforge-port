package com.hbm.entity.mob.glyphid;

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
 * CE glyphid family. Names from {@code @AutoRegister}. Egg colors from CE annotations.
 * Must run before {@code ModItems.register}.
 */
public final class GlyphidEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MainRegistry.MODID);

    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphid>> GLYPHID;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidBombardier>> BOMBARDIER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidBlaster>> BLASTER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidBrawler>> BRAWLER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidBehemoth>> BEHEMOTH;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidBrenda>> BRENDA;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidDigger>> DIGGER;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidNuclear>> NUCLEAR;
    public static DeferredHolder<EntityType<?>, EntityType<EntityGlyphidScout>> SCOUT;

    private GlyphidEntityTypes() {
    }

    public static void register(IEventBus modEventBus) {
        GLYPHID = ENTITY_TYPES.register("entity_glyphid", () ->
                EntityType.Builder.<EntityGlyphid>of(EntityGlyphid::new, MobCategory.MONSTER)
                        .sized(1.75F, 1.0F).build("entity_glyphid"));
        BOMBARDIER = ENTITY_TYPES.register("entity_glyphid_bombardier", () ->
                EntityType.Builder.<EntityGlyphidBombardier>of(EntityGlyphidBombardier::new, MobCategory.MONSTER)
                        .sized(1.75F, 1.0F).build("entity_glyphid_bombardier"));
        BLASTER = ENTITY_TYPES.register("entity_glyphid_blaster", () ->
                EntityType.Builder.<EntityGlyphidBlaster>of(EntityGlyphidBlaster::new, MobCategory.MONSTER)
                        .sized(2.0F, 1.125F).build("entity_glyphid_blaster"));
        BRAWLER = ENTITY_TYPES.register("entity_glyphid_brawler", () ->
                EntityType.Builder.<EntityGlyphidBrawler>of(EntityGlyphidBrawler::new, MobCategory.MONSTER)
                        .sized(2.0F, 1.125F).build("entity_glyphid_brawler"));
        BEHEMOTH = ENTITY_TYPES.register("entity_glyphid_behemoth", () ->
                EntityType.Builder.<EntityGlyphidBehemoth>of(EntityGlyphidBehemoth::new, MobCategory.MONSTER)
                        .sized(2.5F, 1.5F).build("entity_glyphid_behemoth"));
        BRENDA = ENTITY_TYPES.register("entity_glyphid_brenda", () ->
                EntityType.Builder.<EntityGlyphidBrenda>of(EntityGlyphidBrenda::new, MobCategory.MONSTER)
                        .sized(2.5F, 1.75F).fireImmune().build("entity_glyphid_brenda"));
        DIGGER = ENTITY_TYPES.register("entity_glyphid_digger", () ->
                EntityType.Builder.<EntityGlyphidDigger>of(EntityGlyphidDigger::new, MobCategory.MONSTER)
                        .sized(1.75F, 1.0F).build("entity_glyphid_digger"));
        NUCLEAR = ENTITY_TYPES.register("entity_glyphid_nuclear", () ->
                EntityType.Builder.<EntityGlyphidNuclear>of(EntityGlyphidNuclear::new, MobCategory.MONSTER)
                        .sized(2.5F, 1.75F).fireImmune().build("entity_glyphid_nuclear"));
        SCOUT = ENTITY_TYPES.register("entity_glyphid_scout", () ->
                EntityType.Builder.<EntityGlyphidScout>of(EntityGlyphidScout::new, MobCategory.MONSTER)
                        .sized(1.25F, 0.75F).build("entity_glyphid_scout"));

        egg("glyphid_spawn_egg", GLYPHID::get, 0x724A21, 0xD2BB72);
        egg("glyphid_bombardier_spawn_egg", BOMBARDIER::get, 0xDDD919, 0xDBB79D);
        egg("glyphid_blaster_spawn_egg", BLASTER::get, 0xD83737, 0xDBB79D);
        egg("glyphid_brawler_spawn_egg", BRAWLER::get, 0x273038, 0xD2BB72);
        egg("glyphid_behemoth_spawn_egg", BEHEMOTH::get, 0x267F00, 0xD2BB72);
        egg("glyphid_brenda_spawn_egg", BRENDA::get, 0x4FC0C0, 0xA0A0A0);
        egg("glyphid_digger_spawn_egg", DIGGER::get, 0x273038, 0x724A21);
        egg("glyphid_nuclear_spawn_egg", NUCLEAR::get, 0x267F00, 0xA0A0A0);
        egg("glyphid_scout_spawn_egg", SCOUT::get, 0x273038, 0xB9E36B);

        ENTITY_TYPES.register(modEventBus);
    }

    private static void egg(String name, java.util.function.Supplier<EntityType<?>> type, int a, int b) {
        ModItems.ITEMS.register(name, () -> new LazySpawnEggItem(type, a, b, new Item.Properties()));
    }
}
