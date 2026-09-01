package com.hbm.items.special;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemHolotapeImage.EnumHoloImage;
import com.hbm.items.special.ItemSiegeCoin.SiegeTier;
import com.hbm.items.special.ItemWasteLong.WasteClass;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Registration for docs/phase1/items_special.md's P1/P1-flatten lists (excluding
 * {@code ItemBedrockOreNew}, owned by a separate area - see {@code BedrockOreItems} in this same
 * package). See each item class's own javadoc for CE-to-port behavior notes; this file is
 * registration and creative-tab wiring only.
 * <p>
 * Deliberately not registered here (see individual class javadocs for why):
 * <ul>
 *     <li>{@code ItemConsumable}, {@code ItemCustomLore} (base, beyond {@code demon_core_closed}),
 *     {@code ItemSimpleConsumable}, {@code ItemHot}, {@code ItemNuclearWaste} (base) - reusable
 *     building classes with no field of their own inside this package's CE scope; CE's hundreds of
 *     concrete instances of these classes live in other Phase 1 areas' own item families.</li>
 *     <li>{@code ItemDepletedFuel} - all 16 CE fields it backs are registered by
 *     {@code com.hbm.items.PlateCrystalWasteItems} (a different concurrent Phase 1 area's file),
 *     using this class directly; not duplicated here to avoid double-registering those ids.</li>
 * </ul>
 */
public final class SpecialItems {

    private SpecialItems() {
    }

    /** No-op body; referencing this class forces the static initializers below to run. */
    public static void registerAll() {
    }

    public static final DeferredItem<com.hbm.items.BrokenItem> BROKEN_ITEM = register("broken_item",
            () -> new com.hbm.items.BrokenItem(new Item.Properties()));

    // ==================== ItemAMSCore (4 instances) ====================

    public static final DeferredItem<ItemAMSCore> AMS_CORE_SING = register("ams_core_sing",
            () -> new ItemAMSCore(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    500, 0.8F, 1.5F, java.util.List.of(
                            Component.literal("A modified undefined state of spacetime used to aid in"),
                            Component.literal("inter-gluon fusion and spacetime annihilation. Yes, this"),
                            Component.literal("destroys the universe itself, slowly but steadily, but at"),
                            Component.literal("least you can power your toaster with this, so it's all good."))));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONTROL, AMS_CORE_SING);
    }

    public static final DeferredItem<ItemAMSCore> AMS_CORE_WORMHOLE = register("ams_core_wormhole",
            () -> new ItemAMSCore(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    650, 1.5F, 0.8F, java.util.List.of(
                            Component.literal("A cloud of billions of nano-wormholes which deliberately"),
                            Component.literal("fail at tunneling matter from another dimension, rather it"),
                            Component.literal("converts all that matter into pure energy. That dimension"),
                            Component.literal("probably sucked, anyways."))));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONTROL, AMS_CORE_WORMHOLE);
    }

    public static final DeferredItem<ItemAMSCore> AMS_CORE_EYEOFHARMONY = register("ams_core_eyeofharmony",
            () -> new ItemAMSCore(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),
                    800, 1.5F, 2.0F, java.util.List.of(
                            Component.literal("A star collapsing in on itself, mere nanoseconds away from"),
                            Component.literal("being turned into a black hole, frozen in time."))));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONTROL, AMS_CORE_EYEOFHARMONY);
    }

    public static final DeferredItem<ItemAMSCore> AMS_CORE_THINGY = register("ams_core_thingy",
            () -> new ItemAMSCore(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC),
                    2500, 0.7F, 0.7F, java.util.List.of(
                            Component.literal("It's a small metal thing. I dunno where it's from or what"),
                            Component.literal("it does. If it weren't for the fact that I can stuff this"),
                            Component.literal("into some great big laser reactor thing, I'd probably bring"),
                            Component.literal("it back to where it belongs. In the trash."))));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, AMS_CORE_THINGY);
    }

    // ==================== ItemCell (1 instance, fluid identity via data component) ============

    public static final DeferredItem<ItemCell> CELL = register("cell", () -> new ItemCell(new Item.Properties()));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONTROL, CELL);
    }

    // ==================== ItemDemonCore / demon_core_closed (Pattern F, see HazardRegistry) =====

    public static final DeferredItem<ItemDemonCore> DEMON_CORE_OPEN =
            register("demon_core_open", () -> new ItemDemonCore(new Item.Properties()));
    public static final DeferredItem<ItemCustomLore> DEMON_CORE_CLOSED =
            register("demon_core_closed", () -> new ItemCustomLore(new Item.Properties()));
    static {
        CreativeTabContents.add(ModCreativeTabs.NUKE, DEMON_CORE_OPEN);
        CreativeTabContents.add(ModCreativeTabs.NUKE, DEMON_CORE_CLOSED);
    }

    // ==================== ItemDigamma (1 instance) ====================

    public static final DeferredItem<ItemDigamma> PARTICLE_DIGAMMA =
            register("particle_digamma", () -> new ItemDigamma(new Item.Properties(), 60));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONTROL, PARTICLE_DIGAMMA);
    }

    // ==================== ItemGlitch (1 instance, registration only) ====================

    public static final DeferredItem<ItemGlitch> GLITCH =
            register("glitch", () -> new ItemGlitch(new Item.Properties().stacksTo(1).durability(1)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, GLITCH);
    }

    // ==================== ItemModRecord (4 instances) ====================
    // CE placed all four under vanilla CreativeTabs.MISC, which has no equivalent hook in this
    // port's CreativeTabContents design (that mechanism only flushes this mod's own 10 tabs) - left
    // out of every tab, same net visibility as CE's records had outside of MISC.

    public static final DeferredItem<ItemModRecord> RECORD_LC = register("record_lc",
            () -> new ItemModRecord(new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm.main.MainRegistry.MODID, "lc"))),
                    "lc"));
    public static final DeferredItem<ItemModRecord> RECORD_SS = register("record_ss",
            () -> new ItemModRecord(new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm.main.MainRegistry.MODID, "ss"))),
                    "ss"));
    public static final DeferredItem<ItemModRecord> RECORD_VC = register("record_vc",
            () -> new ItemModRecord(new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm.main.MainRegistry.MODID, "vc"))),
                    "vc"));
    public static final DeferredItem<ItemModRecord> RECORD_GLASS = register("record_glass",
            () -> new ItemModRecord(new Item.Properties().stacksTo(1)
                    .jukeboxPlayable(net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.JUKEBOX_SONG,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(com.hbm.main.MainRegistry.MODID, "glass"))),
                    "glass"));

    // ==================== ItemPolaroid (1 instance) ====================

    public static final DeferredItem<ItemPolaroid> POLAROID =
            register("polaroid", () -> new ItemPolaroid(new Item.Properties().stacksTo(1)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, POLAROID);
    }

    // ==================== ItemRag (2 instances) ====================

    public static final DeferredItem<ItemRag> RAG =
            register("rag", () -> new ItemRag(new Item.Properties(), "rag_damp", "rag_piss"));
    static {
        CreativeTabContents.add(ModCreativeTabs.PARTS, RAG);
    }

    // mask_rag: CE places this under vanilla CreativeTabs.COMBAT - same "no hook for vanilla tabs"
    // situation as the records above, left out of every tab.
    public static final DeferredItem<ItemRag> MASK_RAG =
            register("mask_rag", () -> new ItemRag(new Item.Properties().stacksTo(1), "mask_damp", "mask_piss"));

    // ==================== ItemStarterKit (24 instances, registration only) ====================

    public static final DeferredItem<ItemStarterKit> STEALTH_BOY = registerKit("stealth_boy", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> EUPHEMIUM_KIT = registerKit("euphemium_kit", null);
    public static final DeferredItem<ItemStarterKit> NUKE_ELECTRIC_KIT = registerKit("nuke_electric_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> GRENADE_KIT = registerKit("grenade_kit", ModCreativeTabs.WEAPON);
    public static final DeferredItem<ItemStarterKit> GADGET_KIT = registerKit("gadget_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> BOY_KIT = registerKit("boy_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> MAN_KIT = registerKit("man_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> MIKE_KIT = registerKit("mike_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> TSAR_KIT = registerKit("tsar_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> PROTOTYPE_KIT = registerKit("prototype_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> FLEIJA_KIT = registerKit("fleija_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> SOLINIUM_KIT = registerKit("solinium_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> BALEFIRE_KIT = registerKit("balefire_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> MULTI_KIT = registerKit("multi_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> CUSTOM_KIT = registerKit("custom_kit", ModCreativeTabs.NUKE);
    public static final DeferredItem<ItemStarterKit> MISSILE_KIT = registerKit("missile_kit", ModCreativeTabs.MISSILE);
    public static final DeferredItem<ItemStarterKit> T45_KIT = registerKit("t45_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> HAZMAT_KIT = registerKit("hazmat_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> HAZMAT_RED_KIT = registerKit("hazmat_red_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> HAZMAT_GREY_KIT = registerKit("hazmat_grey_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> NUKE_STARTER_KIT = registerKit("nuke_starter_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> NUKE_ADVANCED_KIT = registerKit("nuke_advanced_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> NUKE_COMMERCIALLY_KIT = registerKit("nuke_commercially_kit", ModCreativeTabs.CONSUMABLE);
    public static final DeferredItem<ItemStarterKit> LETTER = registerKit("letter", ModCreativeTabs.CONSUMABLE);

    private static DeferredItem<ItemStarterKit> registerKit(String name, ResourceKey<CreativeModeTab> tab) {
        DeferredItem<ItemStarterKit> item = register(name, () -> new ItemStarterKit(new Item.Properties().stacksTo(1)));
        if (tab != null) {
            CreativeTabContents.add(tab, item);
        }
        return item;
    }

    // ==================== ItemBook / ItemBookLore / ItemClayTablet (shells) ====================

    public static final DeferredItem<ItemBook> BOOK_OF = register("book_of_", () -> new ItemBook(new Item.Properties().stacksTo(1)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, BOOK_OF);
    }

    // book_lore, clay_tablet: CE sets setCreativeTab(null) - hidden from every tab, same here.
    public static final DeferredItem<ItemBookLore> BOOK_LORE = register("book_lore", () -> new ItemBookLore(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ItemClayTablet> CLAY_TABLET = register("clay_tablet", () -> new ItemClayTablet(new Item.Properties().stacksTo(1)));

    // ==================== ItemHolotapeImage (18 flattened variants) ====================
    // CE sets setCreativeTab(null) on holotape_image - hidden from every tab, same here.

    private static final java.util.Map<EnumHoloImage, DeferredItem<ItemHolotapeImage>> HOLOTAPE_IMAGES = new java.util.EnumMap<>(EnumHoloImage.class);

    static {
        for (EnumHoloImage image : EnumHoloImage.VALUES) {
            String id = "holotape_image_" + image.name().toLowerCase(Locale.ROOT);
            DeferredItem<ItemHolotapeImage> item = register(id,
                    () -> new ItemHolotapeImage(new Item.Properties().stacksTo(1), image));
            HOLOTAPE_IMAGES.put(image, item);
        }
    }

    public static DeferredItem<ItemHolotapeImage> holotapeImage(EnumHoloImage image) {
        return HOLOTAPE_IMAGES.get(image);
    }

    // ==================== ItemPlasticScrap (21 flattened variants, plain items) ==================
    // CE's ItemPlasticScrap is pure ItemEnumMulti multiplexing (no addInformation/hasEffect of its
    // own) - post-flattening every variant is a plain Item, no dedicated class needed.
    // CE sets setCreativeTab(null) - hidden from every tab, same here.

    private static final java.util.Map<ScrapType, DeferredItem<Item>> PLASTIC_SCRAP = new java.util.EnumMap<>(ScrapType.class);

    static {
        for (ScrapType type : ScrapType.VALUES) {
            String id = "plastic_scrap_" + type.name().toLowerCase(Locale.ROOT);
            DeferredItem<Item> item = register(id, () -> new Item(new Item.Properties()));
            PLASTIC_SCRAP.put(type, item);
        }
    }

    public static DeferredItem<Item> plasticScrap(ScrapType type) {
        return PLASTIC_SCRAP.get(type);
    }

    /**
     * Mirrors CE's {@code ItemPlasticScrap.ScrapType} (21 constants: general circuit-board
     * components), used only for this flattened item family's registry-id suffixes.
     */
    public enum ScrapType {
        BOARD_BLANK, BOARD_TRANSISTOR, BOARD_CONVERTER,
        BRIDGE_NORTH, BRIDGE_SOUTH, BRIDGE_IO, BRIDGE_BUS, BRIDGE_CHIPSET, BRIDGE_CMOS, BRIDGE_BIOS,
        CPU_REGISTER, CPU_CLOCK, CPU_LOGIC, CPU_CACHE, CPU_EXT, CPU_SOCKET,
        MEM_SOCKET, MEM_16K_A, MEM_16K_B, MEM_16K_C, MEM_16K_D,
        CARD_BOARD, CARD_PROCESSOR;

        public static final ScrapType[] VALUES = values();
    }

    // ==================== ItemWasteLong / ItemWasteShort (CE 4 fields × class, flattened) ====================
    // CE: nuclear_waste_long / _tiny / _depleted / _depleted_tiny (5 WasteClass)
    //     nuclear_waste_short / _tiny / _depleted / _depleted_tiny (8 WasteClass)
    // StorageDrum + SILEX I/O. Texture is the CE family png; class is tooltip-only.

    private static final java.util.Map<WasteClass, DeferredItem<ItemWasteLong>> NUCLEAR_WASTE_LONG = new java.util.EnumMap<>(WasteClass.class);
    private static final java.util.Map<WasteClass, DeferredItem<ItemWasteLong>> NUCLEAR_WASTE_LONG_TINY = new java.util.EnumMap<>(WasteClass.class);
    private static final java.util.Map<WasteClass, DeferredItem<ItemWasteLong>> NUCLEAR_WASTE_LONG_DEPLETED = new java.util.EnumMap<>(WasteClass.class);
    private static final java.util.Map<WasteClass, DeferredItem<ItemWasteLong>> NUCLEAR_WASTE_LONG_DEPLETED_TINY = new java.util.EnumMap<>(WasteClass.class);

    static {
        for (WasteClass wasteClass : WasteClass.VALUES) {
            String suffix = wasteClass.name().toLowerCase(Locale.ROOT);
            NUCLEAR_WASTE_LONG.put(wasteClass, registerParts("nuclear_waste_long_" + suffix,
                    () -> new ItemWasteLong(new Item.Properties(), wasteClass, ItemWasteLong.WasteForm.BASE)));
            NUCLEAR_WASTE_LONG_TINY.put(wasteClass, registerParts("nuclear_waste_long_tiny_" + suffix,
                    () -> new ItemWasteLong(new Item.Properties(), wasteClass, ItemWasteLong.WasteForm.TINY)));
            NUCLEAR_WASTE_LONG_DEPLETED.put(wasteClass, registerParts("nuclear_waste_long_depleted_" + suffix,
                    () -> new ItemWasteLong(new Item.Properties(), wasteClass, ItemWasteLong.WasteForm.DEPLETED)));
            NUCLEAR_WASTE_LONG_DEPLETED_TINY.put(wasteClass, registerParts("nuclear_waste_long_depleted_tiny_" + suffix,
                    () -> new ItemWasteLong(new Item.Properties(), wasteClass, ItemWasteLong.WasteForm.DEPLETED_TINY)));
        }
    }

    public static DeferredItem<ItemWasteLong> nuclearWasteLong(WasteClass wasteClass) {
        return NUCLEAR_WASTE_LONG.get(wasteClass);
    }

    public static DeferredItem<ItemWasteLong> nuclearWasteLongTiny(WasteClass wasteClass) {
        return NUCLEAR_WASTE_LONG_TINY.get(wasteClass);
    }

    public static DeferredItem<ItemWasteLong> nuclearWasteLongDepleted(WasteClass wasteClass) {
        return NUCLEAR_WASTE_LONG_DEPLETED.get(wasteClass);
    }

    public static DeferredItem<ItemWasteLong> nuclearWasteLongDepletedTiny(WasteClass wasteClass) {
        return NUCLEAR_WASTE_LONG_DEPLETED_TINY.get(wasteClass);
    }

    private static final java.util.Map<ItemWasteShort.WasteClass, DeferredItem<ItemWasteShort>> NUCLEAR_WASTE_SHORT =
            new java.util.EnumMap<>(ItemWasteShort.WasteClass.class);
    private static final java.util.Map<ItemWasteShort.WasteClass, DeferredItem<ItemWasteShort>> NUCLEAR_WASTE_SHORT_TINY =
            new java.util.EnumMap<>(ItemWasteShort.WasteClass.class);
    private static final java.util.Map<ItemWasteShort.WasteClass, DeferredItem<ItemWasteShort>> NUCLEAR_WASTE_SHORT_DEPLETED =
            new java.util.EnumMap<>(ItemWasteShort.WasteClass.class);
    private static final java.util.Map<ItemWasteShort.WasteClass, DeferredItem<ItemWasteShort>> NUCLEAR_WASTE_SHORT_DEPLETED_TINY =
            new java.util.EnumMap<>(ItemWasteShort.WasteClass.class);

    static {
        for (ItemWasteShort.WasteClass wasteClass : ItemWasteShort.WasteClass.VALUES) {
            String suffix = wasteClass.name().toLowerCase(Locale.ROOT);
            NUCLEAR_WASTE_SHORT.put(wasteClass, registerParts("nuclear_waste_short_" + suffix,
                    () -> new ItemWasteShort(new Item.Properties(), wasteClass, ItemWasteShort.WasteForm.BASE)));
            NUCLEAR_WASTE_SHORT_TINY.put(wasteClass, registerParts("nuclear_waste_short_tiny_" + suffix,
                    () -> new ItemWasteShort(new Item.Properties(), wasteClass, ItemWasteShort.WasteForm.TINY)));
            NUCLEAR_WASTE_SHORT_DEPLETED.put(wasteClass, registerParts("nuclear_waste_short_depleted_" + suffix,
                    () -> new ItemWasteShort(new Item.Properties(), wasteClass, ItemWasteShort.WasteForm.DEPLETED)));
            NUCLEAR_WASTE_SHORT_DEPLETED_TINY.put(wasteClass, registerParts("nuclear_waste_short_depleted_tiny_" + suffix,
                    () -> new ItemWasteShort(new Item.Properties(), wasteClass, ItemWasteShort.WasteForm.DEPLETED_TINY)));
        }
    }

    public static DeferredItem<ItemWasteShort> nuclearWasteShort(ItemWasteShort.WasteClass wasteClass) {
        return NUCLEAR_WASTE_SHORT.get(wasteClass);
    }

    public static DeferredItem<ItemWasteShort> nuclearWasteShortTiny(ItemWasteShort.WasteClass wasteClass) {
        return NUCLEAR_WASTE_SHORT_TINY.get(wasteClass);
    }

    public static DeferredItem<ItemWasteShort> nuclearWasteShortDepleted(ItemWasteShort.WasteClass wasteClass) {
        return NUCLEAR_WASTE_SHORT_DEPLETED.get(wasteClass);
    }

    public static DeferredItem<ItemWasteShort> nuclearWasteShortDepletedTiny(ItemWasteShort.WasteClass wasteClass) {
        return NUCLEAR_WASTE_SHORT_DEPLETED_TINY.get(wasteClass);
    }

    // ==================== ItemSiegeCoin (9 flattened variants) ====================

    private static final java.util.Map<SiegeTier, DeferredItem<ItemSiegeCoin>> SIEGE_COINS = new java.util.EnumMap<>(SiegeTier.class);

    static {
        for (SiegeTier tier : SiegeTier.VALUES) {
            String id = "coin_siege_" + tier.name().toLowerCase(Locale.ROOT);
            DeferredItem<ItemSiegeCoin> item = register(id,
                    () -> new ItemSiegeCoin(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON), tier.ordinal()));
            CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, item);
            SIEGE_COINS.put(tier, item);
        }
    }

    public static DeferredItem<ItemSiegeCoin> siegeCoin(SiegeTier tier) {
        return SIEGE_COINS.get(tier);
    }

    // ==================== ItemChopper (4 instances) ====================

    public static final DeferredItem<ItemChopper> SPAWN_CHOPPER =
            register("chopper", () -> new ItemChopper(new Item.Properties().stacksTo(1), ItemChopper.SpawnMob.CHOPPER));
    public static final DeferredItem<ItemChopper> SPAWN_WORM =
            register("spawn_worm", () -> new ItemChopper(new Item.Properties().stacksTo(1), ItemChopper.SpawnMob.WORM));
    public static final DeferredItem<ItemChopper> SPAWN_UFO =
            register("spawn_ufo", () -> new ItemChopper(new Item.Properties().stacksTo(1), ItemChopper.SpawnMob.UFO));
    public static final DeferredItem<ItemChopper> SPAWN_DUCK =
            register("spawn_duck", () -> new ItemChopper(new Item.Properties().stacksTo(16), ItemChopper.SpawnMob.DUCK));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, SPAWN_CHOPPER);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, SPAWN_WORM);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, SPAWN_UFO);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, SPAWN_DUCK);
    }

    // ==================== BOTPrime worm boss loot/key (docs/phase4/entities_bosses.md) ==========
    // coin_worm: a plain ItemCustomLore trophy, same pattern as the already-ported ItemSiegeCoin
    // instances above. mech_key: CE's worm-spawner key (BlockBallsSpawner's single consumable), a
    // plain crafted item - CE's own recipe (main/CraftingManager.java:792) needs ModItems.coin_maskman
    // (MaskMan's own trophy - a different, not-yet-ported boss per this same report's own scope split)
    // and a bare "key" item that does not exist in this port; substituted below with a recipe built
    // entirely from items already registered in this port (see data/hbm/recipe/mech_key.json) - a
    // deliberate simplification, not a missing dependency, per this package's own task brief ("this is
    // not the recipe system's focus").

    public static final DeferredItem<ItemCustomLore> COIN_WORM =
            register("coin_worm", () -> new ItemCustomLore(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    public static final DeferredItem<Item> MECH_KEY =
            register("mech_key", () -> new Item(new Item.Properties().stacksTo(1)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COIN_WORM);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, MECH_KEY);
    }

    // ==================== MaskMan boss loot (docs/phase4/entities_bosses.md) ====================
    // coin_maskman: CE ModItems.java:1419, ItemCustomLore, UNCOMMON, consumableTab - same pattern as
    // coin_worm/the ItemSiegeCoin family above.
    // gas_mask_filter_combo: CE ModItems.java:181, `new ItemFilter("gas_mask_filter_combo", 24000)
    // .setMaxStackSize(1).setCreativeTab(MainRegistry.consumableTab)`. Ported here as a minimal plain
    // Item (durability only) purely so EntityMaskMan's death loot can pre-install a real filter stack
    // via the already-real ArmorUtil.installGasMaskFilter. CE's full ItemFilter mechanic (player
    // right-click swap onto a worn IGasMask helmet, with an ArmorModHandler mod-slot cross-check) is a
    // wider "armor items/attachments" scope already flagged as a deferred TODO in ArmorUtil.java
    // (register(), ~lines 104-109) and is NOT reproduced here - this item is otherwise inert.

    public static final DeferredItem<ItemCustomLore> COIN_MASKMAN =
            register("coin_maskman", () -> new ItemCustomLore(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<Item> GAS_MASK_FILTER_COMBO =
            register("gas_mask_filter_combo", () -> new Item(new Item.Properties().stacksTo(1).durability(24000)));
    public static final DeferredItem<com.hbm.items.armor.ItemModV1> V1 =
            register("v1", () -> new com.hbm.items.armor.ItemModV1(new Item.Properties().stacksTo(1)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COIN_MASKMAN);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, GAS_MASK_FILTER_COMBO);
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, V1);
    }

    // ==================== RAD Beast leader loot (docs/phase4/entities_bosses.md RAD Beast section /
    // entities_creeper_variants.md / pollution_system.md's EntityEffectHandler cross-references) ======
    // coin_radiation: CE ModItems.java:1421, `new ItemCustomLore("coin_radiation").setRarity(UNCOMMON)
    // .setCreativeTab(MainRegistry.consumableTab)` - same pattern as coin_worm/coin_maskman above; held
    // in the off-hand-equivalent (mainhand) slot and dropped only by EntityRADBeast's "leader" variant.

    public static final DeferredItem<ItemCustomLore> COIN_RADIATION =
            register("coin_radiation", () -> new ItemCustomLore(new Item.Properties().rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COIN_RADIATION);
    }

    // ==================== ItemSoyuz (3 flattened SoyuzSkinType variants) ====================
    // CE's single "missile_soyuz" field (ItemEnumMulti, 3 metadata skins) flattens into one registry
    // id per skin, matching this file's other ItemEnumMulti ports (ItemSiegeCoin, ItemWasteLong).
    // CE placed missile_soyuz under MainRegistry.missileTab with stacksTo(1); rarity is baked in per
    // skin (see ItemSoyuz's javadoc) since getRarity has no per-instance override left in modern Item.

    public static final DeferredItem<ItemSoyuz> MISSILE_SOYUZ_NORMAL = register("missile_soyuz_normal",
            () -> new ItemSoyuz(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.COMMON),
                    ItemSoyuz.SoyuzSkinType.NORMAL));
    public static final DeferredItem<ItemSoyuz> MISSILE_SOYUZ_LUNAR = register("missile_soyuz_lunar",
            () -> new ItemSoyuz(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE),
                    ItemSoyuz.SoyuzSkinType.LUNAR));
    public static final DeferredItem<ItemSoyuz> MISSILE_SOYUZ_POST_WAR = register("missile_soyuz_post_war",
            () -> new ItemSoyuz(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC),
                    ItemSoyuz.SoyuzSkinType.POST_WAR));
    static {
        CreativeTabContents.add(ModCreativeTabs.MISSILE, MISSILE_SOYUZ_NORMAL);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, MISSILE_SOYUZ_LUNAR);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, MISSILE_SOYUZ_POST_WAR);
    }

    // ==================== ItemTrain (2 flattened EnumTrainType variants) ====================
    // CE's single "train" field (ItemEnumMulti, 2 metadata rail-car types) flattens into one registry
    // id per type, same treatment as ItemSoyuz above. CE sets setCreativeTab(null) on "train" - hidden
    // from every tab, same here.

    public static final DeferredItem<ItemTrain> TRAIN_CARGO_TRAM = register("train_cargo_tram",
            () -> new ItemTrain(new Item.Properties().stacksTo(1), ItemTrain.EnumTrainType.CARGO_TRAM));
    public static final DeferredItem<ItemTrain> TRAIN_CARGO_TRAM_TRAILER = register("train_cargo_tram_trailer",
            () -> new ItemTrain(new Item.Properties().stacksTo(1), ItemTrain.EnumTrainType.CARGO_TRAM_TRAILER));

    // ==================== UFO boss loot (docs/phase4/entities_bosses.md UFO row) ==================
    // coin_ufo: same ItemCustomLore trophy pattern as coin_worm/coin_maskman/coin_radiation above.

    public static final DeferredItem<ItemCustomLore> COIN_UFO =
            register("coin_ufo", () -> new ItemCustomLore(new Item.Properties().rarity(net.minecraft.world.item.Rarity.RARE)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, COIN_UFO);
    }

    // ==================== Hunter Chopper wreckage loot (docs/phase4/entities_bosses.md Hunter
    // Chopper row) ============================================================================
    // 6 plain flavor items with no gameplay use beyond being a drop - CE's own ModItems.java entries
    // are bare `new ItemBase(name)` calls under partsTab; ported here as plain Items under the PARTS
    // tab, matching this file's own established `registerParts` helper for exactly this shape.
    // combine_scrap/wire_fine (also named in EntityHunterChopper's own loot table) are NOT registered
    // here - neither exists anywhere in this port yet (a Phase 1/2 generic-materials gap, not owned by
    // this boss/mob package) and registering a shared generic material from this file risks a
    // duplicate-id collision with whichever concurrent sibling package does own it.

    public static final DeferredItem<Item> CHOPPER_HEAD = registerParts("chopper_head", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHOPPER_TORSO = registerParts("chopper_torso", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHOPPER_WING = registerParts("chopper_wing", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHOPPER_TAIL = registerParts("chopper_tail", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHOPPER_GUN = registerParts("chopper_gun", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> CHOPPER_BLADES = registerParts("chopper_blades", () -> new Item(new Item.Properties()));

    // ==================== ItemPeas (docs/phase4/entities_bosses.md Quackos row) ===================
    // EntityQuackos's sole removal path - see com.hbm.items.tool.ItemPeas's own javadoc.

    public static final DeferredItem<com.hbm.items.tool.ItemPeas> PEAS =
            register("peas", () -> new com.hbm.items.tool.ItemPeas(new Item.Properties().stacksTo(64)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, PEAS);
    }

    // CE ModItems.java:106 ItemTeleLink("linker").setMaxStackSize(1).setCreativeTab(consumableTab)
    public static final DeferredItem<ItemTeleLink> LINKER =
            register("linker", () -> new ItemTeleLink(new Item.Properties().stacksTo(1)));
    static {
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, LINKER);
    }

    // ==================== helpers ====================

    private static <T extends Item> DeferredItem<T> register(String name, Supplier<T> factory) {
        return ModItems.ITEMS.register(name, factory);
    }

    private static <T extends Item> DeferredItem<T> registerParts(String name, Supplier<T> factory) {
        DeferredItem<T> item = register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }
}
