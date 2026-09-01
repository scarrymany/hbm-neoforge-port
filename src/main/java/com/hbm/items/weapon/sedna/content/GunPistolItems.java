package com.hbm.items.weapon.sedna.content;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registered {@code Item}s for this batch of the Sedna gun roster
 * ({@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryBlackPowder}/{@code XFactory357}/
 * {@code XFactory44}/{@code XFactory9mm}/{@code XFactory22lr} families): {@code gun_pepperbox},
 * {@code gun_light_revolver}(+2 variants), {@code gun_henry}(+1 variant),
 * {@code gun_heavy_revolver}(+2 variants), {@code gun_hangman}, {@code gun_greasegun},
 * {@code gun_lag}, {@code gun_uzi}(+akimbo variant), {@code gun_am180}, {@code gun_star_f}(+akimbo
 * variant) - 17 guns total - plus every real ammo {@code Item} those guns'
 * {@link com.hbm.items.weapon.sedna.BulletConfig}s bind to. Mirrors
 * {@link GunRifleItems}'s exact shape (same per-batch aggregator convention, same
 * {@code registerAmmo}/{@code registerAmmoHidden}/{@code registerGun} helper trio) - read that
 * class's javadoc for the full rationale (metadata-flattening precedent, why ammo must be real
 * distinct items for {@code IMagazine} inventory-scan reloading to work at all).
 * <p>
 * {@code m44_equestrian} (CE's {@code EnumAmmoSecret.M44_EQUESTRIAN}) is registered as a real item
 * (a player can still legitimately hold/fire it) but deliberately left out of the creative tab,
 * matching CE's own {@code ammo_secret} having a {@code null} creative tab - and matching
 * {@code GunRifleItems}'s identical treatment of {@code bmg50_black}/{@code bmg50_equestrian}.
 * <p>
 * <b>One structural difference from {@code GunRifleItems}, not a copy-paste miss.</b> That sibling
 * class wraps each already-built {@code XFactory556mm.gun_g3}-style field directly - fine for ammo
 * (plain, non-registry-touching {@code Item}/{@code BulletConfig} construction) but, for the guns
 * specifically, forces {@code HBMSoundHandler.xxx.get()} to resolve the moment {@code GunRifleItems}
 * is class-loaded, i.e. during {@code ModItems.register(modEventBus)} at mod-construction time -
 * strictly before any {@code RegisterEvent} has fired for any registry, sound included. Every one of
 * this batch's guns is instead exposed as a static <i>method</i> on its {@code XFactory*} class (e.g.
 * {@code XFactoryBlackPowder.gun_pepperbox()}), called from inside the {@code Supplier} below - see
 * {@code XFactoryBlackPowder}'s class javadoc for the full rationale, and this task's structured
 * output for a flag recommending the same fix for {@code GunRifleItems}/{@code XFactory556mm}/
 * {@code XFactory762mm}/{@code XFactory50} once the review wave reaches them.
 */
public final class GunPistolItems {

    private GunPistolItems() {
    }

    // ==================== black powder ammo (4) ====================
    public static final DeferredItem<Item> STONE = registerAmmo("stone", () -> { if (XFactoryBlackPowder.ITEM_STONE == null) XFactoryBlackPowder.ITEM_STONE = new Item(new Item.Properties()); return XFactoryBlackPowder.ITEM_STONE; });
    public static final DeferredItem<Item> STONE_AP = registerAmmo("stone_ap", () -> { if (XFactoryBlackPowder.ITEM_STONE_AP == null) XFactoryBlackPowder.ITEM_STONE_AP = new Item(new Item.Properties()); return XFactoryBlackPowder.ITEM_STONE_AP; });
    public static final DeferredItem<Item> STONE_IRON = registerAmmo("stone_iron", () -> { if (XFactoryBlackPowder.ITEM_STONE_IRON == null) XFactoryBlackPowder.ITEM_STONE_IRON = new Item(new Item.Properties()); return XFactoryBlackPowder.ITEM_STONE_IRON; });
    public static final DeferredItem<Item> STONE_SHOT = registerAmmo("stone_shot", () -> { if (XFactoryBlackPowder.ITEM_STONE_SHOT == null) XFactoryBlackPowder.ITEM_STONE_SHOT = new Item(new Item.Properties()); return XFactoryBlackPowder.ITEM_STONE_SHOT; });

    // ==================== .357 ammo (6) ====================
    public static final DeferredItem<Item> M357_BP = registerAmmo("m357_bp", () -> { if (XFactory357.ITEM_M357_BP == null) XFactory357.ITEM_M357_BP = new Item(new Item.Properties()); return XFactory357.ITEM_M357_BP; });
    public static final DeferredItem<Item> M357_SP = registerAmmo("m357_sp", () -> { if (XFactory357.ITEM_M357_SP == null) XFactory357.ITEM_M357_SP = new Item(new Item.Properties()); return XFactory357.ITEM_M357_SP; });
    public static final DeferredItem<Item> M357_FMJ = registerAmmo("m357_fmj", () -> { if (XFactory357.ITEM_M357_FMJ == null) XFactory357.ITEM_M357_FMJ = new Item(new Item.Properties()); return XFactory357.ITEM_M357_FMJ; });
    public static final DeferredItem<Item> M357_JHP = registerAmmo("m357_jhp", () -> { if (XFactory357.ITEM_M357_JHP == null) XFactory357.ITEM_M357_JHP = new Item(new Item.Properties()); return XFactory357.ITEM_M357_JHP; });
    public static final DeferredItem<Item> M357_AP = registerAmmo("m357_ap", () -> { if (XFactory357.ITEM_M357_AP == null) XFactory357.ITEM_M357_AP = new Item(new Item.Properties()); return XFactory357.ITEM_M357_AP; });
    public static final DeferredItem<Item> M357_EXPRESS = registerAmmo("m357_express", () -> { if (XFactory357.ITEM_M357_EXPRESS == null) XFactory357.ITEM_M357_EXPRESS = new Item(new Item.Properties()); return XFactory357.ITEM_M357_EXPRESS; });

    // ==================== .44 ammo (6 + 1 secret) ====================
    public static final DeferredItem<Item> M44_BP = registerAmmo("m44_bp", () -> { if (XFactory44.ITEM_M44_BP == null) XFactory44.ITEM_M44_BP = new Item(new Item.Properties()); return XFactory44.ITEM_M44_BP; });
    public static final DeferredItem<Item> M44_SP = registerAmmo("m44_sp", () -> { if (XFactory44.ITEM_M44_SP == null) XFactory44.ITEM_M44_SP = new Item(new Item.Properties()); return XFactory44.ITEM_M44_SP; });
    public static final DeferredItem<Item> M44_FMJ = registerAmmo("m44_fmj", () -> { if (XFactory44.ITEM_M44_FMJ == null) XFactory44.ITEM_M44_FMJ = new Item(new Item.Properties()); return XFactory44.ITEM_M44_FMJ; });
    public static final DeferredItem<Item> M44_JHP = registerAmmo("m44_jhp", () -> { if (XFactory44.ITEM_M44_JHP == null) XFactory44.ITEM_M44_JHP = new Item(new Item.Properties()); return XFactory44.ITEM_M44_JHP; });
    public static final DeferredItem<Item> M44_AP = registerAmmo("m44_ap", () -> { if (XFactory44.ITEM_M44_AP == null) XFactory44.ITEM_M44_AP = new Item(new Item.Properties()); return XFactory44.ITEM_M44_AP; });
    public static final DeferredItem<Item> M44_EXPRESS = registerAmmo("m44_express", () -> { if (XFactory44.ITEM_M44_EXPRESS == null) XFactory44.ITEM_M44_EXPRESS = new Item(new Item.Properties()); return XFactory44.ITEM_M44_EXPRESS; });
    /** Secret round - hidden from the creative tab, see class javadoc. Backs both m44_equestrian_pip and _mn7. */
    public static final DeferredItem<Item> M44_EQUESTRIAN = registerAmmoHidden("m44_equestrian", () -> { if (XFactory44.ITEM_M44_EQUESTRIAN == null) XFactory44.ITEM_M44_EQUESTRIAN = new Item(new Item.Properties()); return XFactory44.ITEM_M44_EQUESTRIAN; });

    // ==================== 9mm ammo (4) ====================
    public static final DeferredItem<Item> P9_SP = registerAmmo("p9_sp", () -> { if (XFactory9mm.ITEM_P9_SP == null) XFactory9mm.ITEM_P9_SP = new Item(new Item.Properties()); return XFactory9mm.ITEM_P9_SP; });
    public static final DeferredItem<Item> P9_FMJ = registerAmmo("p9_fmj", () -> { if (XFactory9mm.ITEM_P9_FMJ == null) XFactory9mm.ITEM_P9_FMJ = new Item(new Item.Properties()); return XFactory9mm.ITEM_P9_FMJ; });
    public static final DeferredItem<Item> P9_JHP = registerAmmo("p9_jhp", () -> { if (XFactory9mm.ITEM_P9_JHP == null) XFactory9mm.ITEM_P9_JHP = new Item(new Item.Properties()); return XFactory9mm.ITEM_P9_JHP; });
    public static final DeferredItem<Item> P9_AP = registerAmmo("p9_ap", () -> { if (XFactory9mm.ITEM_P9_AP == null) XFactory9mm.ITEM_P9_AP = new Item(new Item.Properties()); return XFactory9mm.ITEM_P9_AP; });

    // ==================== .45 ACP ammo (CE XFactory45 / AmmoPressRecipes.java:305-364) ====================
    public static final DeferredItem<Item> P45_SP = registerAmmo("p45_sp", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> P45_FMJ = registerAmmo("p45_fmj", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> P45_JHP = registerAmmo("p45_jhp", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> P45_AP = registerAmmo("p45_ap", () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> P45_DU = registerAmmo("p45_du", () -> new Item(new Item.Properties()));

    // ==================== .22LR ammo (4) ====================
    public static final DeferredItem<Item> P22_SP = registerAmmo("p22_sp", () -> { if (XFactory22lr.ITEM_P22_SP == null) XFactory22lr.ITEM_P22_SP = new Item(new Item.Properties()); return XFactory22lr.ITEM_P22_SP; });
    public static final DeferredItem<Item> P22_FMJ = registerAmmo("p22_fmj", () -> { if (XFactory22lr.ITEM_P22_FMJ == null) XFactory22lr.ITEM_P22_FMJ = new Item(new Item.Properties()); return XFactory22lr.ITEM_P22_FMJ; });
    public static final DeferredItem<Item> P22_JHP = registerAmmo("p22_jhp", () -> { if (XFactory22lr.ITEM_P22_JHP == null) XFactory22lr.ITEM_P22_JHP = new Item(new Item.Properties()); return XFactory22lr.ITEM_P22_JHP; });
    public static final DeferredItem<Item> P22_AP = registerAmmo("p22_ap", () -> { if (XFactory22lr.ITEM_P22_AP == null) XFactory22lr.ITEM_P22_AP = new Item(new Item.Properties()); return XFactory22lr.ITEM_P22_AP; });

    // ==================== black powder gun (1) ====================
    public static final DeferredItem<Item> GUN_PEPPERBOX = registerGun("gun_pepperbox", XFactoryBlackPowder::gun_pepperbox);

    // ==================== .357 guns (3) ====================
    public static final DeferredItem<Item> GUN_LIGHT_REVOLVER = registerGun("gun_light_revolver", XFactory357::gun_light_revolver);
    public static final DeferredItem<Item> GUN_LIGHT_REVOLVER_ATLAS = registerGun("gun_light_revolver_atlas", XFactory357::gun_light_revolver_atlas);
    public static final DeferredItem<Item> GUN_LIGHT_REVOLVER_DANI = registerGun("gun_light_revolver_dani", XFactory357::gun_light_revolver_dani);

    // ==================== .44 guns (6) ====================
    public static final DeferredItem<Item> GUN_HENRY = registerGun("gun_henry", XFactory44::gun_henry);
    public static final DeferredItem<Item> GUN_HENRY_LINCOLN = registerGun("gun_henry_lincoln", XFactory44::gun_henry_lincoln);
    public static final DeferredItem<Item> GUN_HEAVY_REVOLVER = registerGun("gun_heavy_revolver", XFactory44::gun_heavy_revolver);
    public static final DeferredItem<Item> GUN_HEAVY_REVOLVER_LILMAC = registerGun("gun_heavy_revolver_lilmac", XFactory44::gun_heavy_revolver_lilmac);
    public static final DeferredItem<Item> GUN_HEAVY_REVOLVER_PROTEGE = registerGun("gun_heavy_revolver_protege", XFactory44::gun_heavy_revolver_protege);
    public static final DeferredItem<Item> GUN_HANGMAN = registerGun("gun_hangman", XFactory44::gun_hangman);

    // ==================== 9mm guns (4) ====================
    public static final DeferredItem<Item> GUN_GREASEGUN = registerGun("gun_greasegun", XFactory9mm::gun_greasegun);
    public static final DeferredItem<Item> GUN_LAG = registerGun("gun_lag", XFactory9mm::gun_lag);
    public static final DeferredItem<Item> GUN_UZI = registerGun("gun_uzi", XFactory9mm::gun_uzi);
    public static final DeferredItem<Item> GUN_UZI_AKIMBO = registerGun("gun_uzi_akimbo", XFactory9mm::gun_uzi_akimbo);

    // ==================== .22LR guns (3) ====================
    public static final DeferredItem<Item> GUN_AM180 = registerGun("gun_am180", XFactory22lr::gun_am180);
    public static final DeferredItem<Item> GUN_STAR_F = registerGun("gun_star_f", XFactory22lr::gun_star_f);
    public static final DeferredItem<Item> GUN_STAR_F_AKIMBO = registerGun("gun_star_f_akimbo", XFactory22lr::gun_star_f_akimbo);

    /** No-op beyond forcing this class (and the 5 XFactory* content classes it references) to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    private static DeferredItem<Item> registerAmmo(String name, java.util.function.Supplier<Item> instance) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, instance);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    /** Registered as a real item (matching CE's EnumAmmoSecret being a real ItemStack variant) but not added to any creative tab. */
    private static DeferredItem<Item> registerAmmoHidden(String name, java.util.function.Supplier<Item> instance) {
        return ModItems.ITEMS.register(name, instance);
    }

    /**
     * Unlike {@link #registerAmmo}, this takes a {@code Supplier} (a {@code XFactoryXxx::gun_yyy}
     * method reference), not an already-built instance - see this class's javadoc for why: it must
     * stay lazy so the gun's {@code Receiver.sound(HBMSoundHandler.xxx.get(), ...)} call only
     * resolves at {@code RegisterEvent} time, not at mod-construction time.
     */
    private static DeferredItem<Item> registerGun(String name, Supplier<? extends ItemGunBaseNT> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }
}
