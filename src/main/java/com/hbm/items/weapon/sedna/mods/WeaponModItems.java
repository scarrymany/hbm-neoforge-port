package com.hbm.items.weapon.sedna.mods;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registered {@code Item}s for CE's {@code weapon_mod_generic}/{@code weapon_mod_special}/
 * {@code weapon_mod_caliber} {@code ItemEnumMulti} attachment families (55 flattened variants total -
 * see {@link ModGeneric}/{@link ModSpecial}/{@link ModCaliber}'s own javadocs for each family's exact
 * CE enum source). Mirrors {@code com.hbm.items.special.SpecialItems}'s
 * {@code ItemPlasticScrap}/{@code EnumMap<ScrapType, DeferredItem<Item>>} flattening precedent 1:1
 * (per this task's explicit instruction to match that approach) - each variant is a plain,
 * class-less {@code Item} (CE's {@code ItemEnumMulti} itself has no per-variant logic beyond display
 * name/icon; the actual gameplay effect lives entirely in the {@link IWeaponMod} instance
 * {@link XWeaponModManager}'s {@code init()} associates with each variant's {@code ItemStack}, not on
 * the item class).
 * <p>
 * CE's {@code weapon_mod_test} family (10 debug-only variants: {@code FIRERATE}/{@code DAMAGE}/
 * {@code MULTI}/7x {@code OVERRIDE_*}) is deliberately <b>not</b> ported - see
 * {@code XWeaponModManager}'s class javadoc for why (matches this task's explicit instruction to skip
 * the 3 {@code WeaponModTest*} debug mod classes; the debug items that would only ever install those
 * classes follow the same exclusion).
 * <p>
 * <b>No install mechanic wired here.</b> CE installs these exclusively through
 * {@code ContainerWeaponTable} (a dedicated weapon-table block/menu, confirmed by grepping every real
 * {@code XWeaponModManager.install}/{@code isApplicable} call site in CE - there is no other caller
 * anywhere in CE's source). That GUI is explicitly out of this package's scope per
 * {@code docs/phase3/gun_framework.md} ("that screen is Package C/D territory... an ordinary
 * block-opened container menu... should use MenuBase/GuiInfoContainer... no new GUI framework
 * needed"). This class + {@link XWeaponModManager#install}/{@code uninstall}/{@code isApplicable} are
 * the complete API surface a future weapon-table block/menu needs to call - none of it is invented
 * here, all of it is a straight port of CE's own manager methods.
 */
public final class WeaponModItems {

    private WeaponModItems() {
    }

    // ==================== weapon_mod_generic (18 variants) ====================

    private static final Map<ModGeneric, DeferredItem<Item>> GENERIC = new EnumMap<>(ModGeneric.class);

    static {
        for (ModGeneric type : ModGeneric.VALUES) {
            GENERIC.put(type, register("weapon_mod_generic_" + type.name().toLowerCase(Locale.ROOT)));
        }
    }

    public static DeferredItem<Item> generic(ModGeneric type) {
        return GENERIC.get(type);
    }

    /** Mirrors CE's {@code GunFactory.EnumModGeneric} (18 constants: material-tier damage/durability kits keyed to which guns use that tier's material). */
    public enum ModGeneric {
        IRON_DAMAGE, IRON_DURA,
        STEEL_DAMAGE, STEEL_DURA,
        DURA_DAMAGE, DURA_DURA,
        DESH_DAMAGE, DESH_DURA,
        WSTEEL_DAMAGE, WSTEEL_DURA,
        FERRO_DAMAGE, FERRO_DURA,
        TCALLOY_DAMAGE, TCALLOY_DURA,
        BIGMT_DAMAGE, BIGMT_DURA,
        BRONZE_DAMAGE, BRONZE_DURA;

        public static final ModGeneric[] VALUES = values();
    }

    // ==================== weapon_mod_special (29 variants) ====================

    private static final Map<ModSpecial, DeferredItem<Item>> SPECIAL = new EnumMap<>(ModSpecial.class);

    static {
        for (ModSpecial type : ModSpecial.VALUES) {
            SPECIAL.put(type, register("weapon_mod_special_" + type.name().toLowerCase(Locale.ROOT)));
        }
    }

    public static DeferredItem<Item> special(ModSpecial type) {
        return SPECIAL.get(type);
    }

    /** Mirrors CE's {@code GunFactory.EnumModSpecial} (29 constants: every named/unique weapon attachment). */
    public enum ModSpecial {
        SILENCER, SCOPE, SAW, GREASEGUN, SLOWDOWN,
        SPEEDUP, CHOKE, SPEEDLOADER,
        FURNITURE_GREEN, FURNITURE_BLACK, BAYONET,
        STACK_MAG, SKIN_SATURNITE, LAS_SHOTGUN,
        LAS_CAPACITOR, LAS_AUTO,
        NICKEL, DOUBLOONS,
        DRILL_HSS, DRILL_WEAPONSTEEL, DRILL_TCALLOY, DRILL_SATURNITE,
        ENGINE_DIESEL, ENGINE_AVIATION, ENGINE_ELECTRIC, ENGINE_TURBO,
        MAGNET, SIFTER, CANISTERS;

        public static final ModSpecial[] VALUES = values();
    }

    // ==================== weapon_mod_caliber (8 variants) ====================

    private static final Map<ModCaliber, DeferredItem<Item>> CALIBER = new EnumMap<>(ModCaliber.class);

    static {
        for (ModCaliber type : ModCaliber.VALUES) {
            CALIBER.put(type, register("weapon_mod_caliber_" + type.name().toLowerCase(Locale.ROOT)));
        }
    }

    public static DeferredItem<Item> caliber(ModCaliber type) {
        return CALIBER.get(type);
    }

    /** Mirrors CE's {@code GunFactory.EnumModCaliber} (8 constants: caliber-conversion kit families). */
    public enum ModCaliber {
        P9, P45, P22, M357, M44, R556, R762, BMG50;

        public static final ModCaliber[] VALUES = values();
    }

    /** No-op beyond forcing this class's 3 static blocks to run before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    private static DeferredItem<Item> register(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new Item(new Item.Properties().stacksTo(1)));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }
}
