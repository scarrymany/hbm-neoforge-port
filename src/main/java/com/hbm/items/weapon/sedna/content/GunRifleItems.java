package com.hbm.items.weapon.sedna.content;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registered {@code Item}s for this batch of the Sedna gun roster
 * ({@code docs/phase3/guns_and_ammo.md}'s {@code XFactory556mm}/{@code XFactory762mm}/
 * {@code XFactory50} families): {@code gun_g3}/{@code gun_g3_zebra}/{@code gun_stg77},
 * {@code gun_carbine}/{@code gun_minigun}/{@code gun_minigun_lacunae}/{@code gun_minigun_dual}/
 * {@code gun_mas36}, and {@code gun_amat}(+2 variants)/{@code gun_m2} - plus every real ammo
 * {@code Item} those guns' {@link com.hbm.items.weapon.sedna.BulletConfig}s bind to.
 * <p>
 * <b>Ammo is flattened into real, distinct {@code DeferredItem}s, one per CE {@code EnumAmmo}
 * constant this batch owns - not a shared metadata multi-item.</b> CE's own {@code ammo_standard} is
 * a single {@code ItemEnumMulti<GunFactory.EnumAmmo>} (101 constants, metadata = ordinal), and this
 * port's general convention flattens {@code ItemEnumMulti} families into one distinct registry entry
 * per variant (see {@code com.hbm.items.special.SpecialItems}'s {@code ItemHolotapeImage}/
 * {@code ItemPlasticScrap} precedent). For ammo specifically that convention is doubly required, not
 * just preferred: {@link com.hbm.items.weapon.sedna.BulletConfig#setItem(Item)} needs a real,
 * concrete {@code Item} so a gun's magazine can scan the firer's inventory for matching stacks
 * ({@code IMagazine}'s reload machinery matches by {@code ComparableStack}, i.e. by {@code Item}
 * identity) - a single shared metadata item would work too in principle, but 101 real items is both
 * this port's established default for exactly this shape of content <i>and</i> the only option this
 * task's own instructions authorize choosing without an explicit metadata-precedent match. Each
 * item below is named after its lowercased CE {@code EnumAmmo} constant (e.g. {@code hbm:r556_sp}).
 * <p>
 * {@code bmg50_black}/{@code bmg50_equestrian} (CE's {@code EnumAmmoSecret}) are registered as real
 * items too (a player can still legitimately hold/fire them) but are deliberately left out of the
 * creative tab, matching CE's own {@code ammo_secret} having a {@code null} creative tab.
 * <p>
 * CAPACITOR/CAPACITOR_OVERCHARGE/CAPACITOR_IR (consumed conceptually by {@code gun_minigun_lacunae})
 * are NOT registered here - see {@link XFactory762mm}'s class javadoc for why (a cross-package shared
 * ammo family also owned by the not-in-this-batch {@code XFactoryEnergy} guns).
 */
public final class GunRifleItems {

    private GunRifleItems() {
    }

    // ==================== 5.56mm ammo (4) ====================
    public static final DeferredItem<Item> R556_SP = registerAmmo("r556_sp", XFactory556mm.ITEM_R556_SP);
    public static final DeferredItem<Item> R556_FMJ = registerAmmo("r556_fmj", XFactory556mm.ITEM_R556_FMJ);
    public static final DeferredItem<Item> R556_JHP = registerAmmo("r556_jhp", XFactory556mm.ITEM_R556_JHP);
    public static final DeferredItem<Item> R556_AP = registerAmmo("r556_ap", XFactory556mm.ITEM_R556_AP);

    // ==================== 7.62mm ammo (6) ====================
    public static final DeferredItem<Item> R762_SP = registerAmmo("r762_sp", XFactory762mm.ITEM_R762_SP);
    public static final DeferredItem<Item> R762_FMJ = registerAmmo("r762_fmj", XFactory762mm.ITEM_R762_FMJ);
    public static final DeferredItem<Item> R762_JHP = registerAmmo("r762_jhp", XFactory762mm.ITEM_R762_JHP);
    public static final DeferredItem<Item> R762_AP = registerAmmo("r762_ap", XFactory762mm.ITEM_R762_AP);
    public static final DeferredItem<Item> R762_DU = registerAmmo("r762_du", XFactory762mm.ITEM_R762_DU);
    public static final DeferredItem<Item> R762_HE = registerAmmo("r762_he", XFactory762mm.ITEM_R762_HE);

    // ==================== .50 BMG ammo (7 regular + 2 secret) ====================
    public static final DeferredItem<Item> BMG50_SP = registerAmmo("bmg50_sp", XFactory50.ITEM_BMG50_SP);
    public static final DeferredItem<Item> BMG50_FMJ = registerAmmo("bmg50_fmj", XFactory50.ITEM_BMG50_FMJ);
    public static final DeferredItem<Item> BMG50_JHP = registerAmmo("bmg50_jhp", XFactory50.ITEM_BMG50_JHP);
    public static final DeferredItem<Item> BMG50_AP = registerAmmo("bmg50_ap", XFactory50.ITEM_BMG50_AP);
    public static final DeferredItem<Item> BMG50_DU = registerAmmo("bmg50_du", XFactory50.ITEM_BMG50_DU);
    public static final DeferredItem<Item> BMG50_HE = registerAmmo("bmg50_he", XFactory50.ITEM_BMG50_HE);
    public static final DeferredItem<Item> BMG50_SM = registerAmmo("bmg50_sm", XFactory50.ITEM_BMG50_SM);
    /** Secret round - hidden from the creative tab, see class javadoc. */
    public static final DeferredItem<Item> BMG50_BLACK = registerAmmoHidden("bmg50_black", XFactory50.ITEM_BMG50_BLACK);
    /** Secret round - hidden from the creative tab, see class javadoc. */
    public static final DeferredItem<Item> BMG50_EQUESTRIAN = registerAmmoHidden("bmg50_equestrian", XFactory50.ITEM_BMG50_EQUESTRIAN);

    // ==================== 5.56mm guns (3) ====================
    public static final DeferredItem<Item> GUN_G3 = registerGun("gun_g3", XFactory556mm::gun_g3);
    public static final DeferredItem<Item> GUN_G3_ZEBRA = registerGun("gun_g3_zebra", XFactory556mm::gun_g3_zebra);
    public static final DeferredItem<Item> GUN_STG77 = registerGun("gun_stg77", XFactory556mm::gun_stg77);

    // ==================== 7.62mm guns (5) ====================
    public static final DeferredItem<Item> GUN_CARBINE = registerGun("gun_carbine", XFactory762mm::gun_carbine);
    public static final DeferredItem<Item> GUN_MINIGUN = registerGun("gun_minigun", XFactory762mm::gun_minigun);
    public static final DeferredItem<Item> GUN_MINIGUN_LACUNAE = registerGun("gun_minigun_lacunae", XFactory762mm::gun_minigun_lacunae);
    public static final DeferredItem<Item> GUN_MINIGUN_DUAL = registerGun("gun_minigun_dual", XFactory762mm::gun_minigun_dual);
    public static final DeferredItem<Item> GUN_MAS36 = registerGun("gun_mas36", XFactory762mm::gun_mas36);

    // ==================== .50 BMG guns (4) ====================
    public static final DeferredItem<Item> GUN_AMAT = registerGun("gun_amat", XFactory50::gun_amat);
    public static final DeferredItem<Item> GUN_AMAT_SUBTLETY = registerGun("gun_amat_subtlety", XFactory50::gun_amat_subtlety);
    public static final DeferredItem<Item> GUN_AMAT_PENANCE = registerGun("gun_amat_penance", XFactory50::gun_amat_penance);
    public static final DeferredItem<Item> GUN_M2 = registerGun("gun_m2", XFactory50::gun_m2);

    /** No-op beyond forcing this class (and the 3 XFactory* content classes it references) to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    private static DeferredItem<Item> registerAmmo(String name, Item instance) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> instance);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    /** Registered as a real item (matching CE's EnumAmmoSecret being a real ItemStack variant) but not added to any creative tab. */
    private static DeferredItem<Item> registerAmmoHidden(String name, Item instance) {
        return ModItems.ITEMS.register(name, () -> instance);
    }

    /**
     * Takes a {@link Supplier}, not an already-constructed {@code Item}: every gun in this batch is
     * built via a static factory method (see e.g. {@link XFactory556mm#gun_g3()}), not an eager
     * {@code static final} field, specifically so constructing it (which resolves a SoundEvent
     * {@code DeferredHolder} via {@code Receiver.sound(...).get()}) is deferred until
     * {@code RegisterEvent(ITEM)} actually fires, not evaluated at this class's own load time.
     */
    private static DeferredItem<Item> registerGun(String name, Supplier<Item> instance) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, instance);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }
}
