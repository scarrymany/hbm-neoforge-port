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
 * ({@code docs/phase3/guns_and_ammo.md}'s {@code XFactory12ga}/{@code XFactory10ga} families):
 * {@code gun_maresleg}(+akimbo/broken variants), {@code gun_liberator}, {@code gun_spas12},
 * {@code gun_autoshotgun}(+shredder/sexy variants), {@code gun_double_barrel}(+sacred_dragon
 * variant), {@code gun_autoshotgun_heretic} - 11 guns total - plus every real ammo {@code Item} those
 * guns' {@link com.hbm.items.weapon.sedna.BulletConfig}s bind to (the 12 shredder/sub laser-beam
 * submunition configs reuse their originals' items, see {@link XFactory12ga#makeShredderConfig}'s
 * javadoc - no new items needed for those). Mirrors {@link GunRifleItems}'s exact shape (same
 * per-batch aggregator convention, same {@code registerAmmo}/{@code registerAmmoHidden}/
 * {@code registerGun} helper trio, same metadata-flattening rationale - see that class's javadoc).
 * <p>
 * {@code g12_equestrian} (CE's shared {@code EnumAmmoSecret.G12_EQUESTRIAN}, backing both
 * {@code g12_equestrian_bj} and {@code _tkr}) is registered as a real item but deliberately left out
 * of the creative tab, matching {@code GunRifleItems}'s identical treatment of
 * {@code bmg50_black}/{@code bmg50_equestrian}.
 */
public final class GunShotgunItems {

    private GunShotgunItems() {
    }

    // ==================== 12ga ammo (9 regular + 1 secret) ====================
    public static final DeferredItem<Item> G12_BP = registerAmmo("g12_bp", () -> { if (XFactory12ga.ITEM_G12_BP == null) XFactory12ga.ITEM_G12_BP = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_BP; });
    public static final DeferredItem<Item> G12_BP_MAGNUM = registerAmmo("g12_bp_magnum", () -> { if (XFactory12ga.ITEM_G12_BP_MAGNUM == null) XFactory12ga.ITEM_G12_BP_MAGNUM = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_BP_MAGNUM; });
    public static final DeferredItem<Item> G12_BP_SLUG = registerAmmo("g12_bp_slug", () -> { if (XFactory12ga.ITEM_G12_BP_SLUG == null) XFactory12ga.ITEM_G12_BP_SLUG = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_BP_SLUG; });
    public static final DeferredItem<Item> G12 = registerAmmo("g12", () -> { if (XFactory12ga.ITEM_G12 == null) XFactory12ga.ITEM_G12 = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12; });
    public static final DeferredItem<Item> G12_SLUG = registerAmmo("g12_slug", () -> { if (XFactory12ga.ITEM_G12_SLUG == null) XFactory12ga.ITEM_G12_SLUG = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_SLUG; });
    public static final DeferredItem<Item> G12_FLECHETTE = registerAmmo("g12_flechette", () -> { if (XFactory12ga.ITEM_G12_FLECHETTE == null) XFactory12ga.ITEM_G12_FLECHETTE = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_FLECHETTE; });
    public static final DeferredItem<Item> G12_MAGNUM = registerAmmo("g12_magnum", () -> { if (XFactory12ga.ITEM_G12_MAGNUM == null) XFactory12ga.ITEM_G12_MAGNUM = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_MAGNUM; });
    public static final DeferredItem<Item> G12_EXPLOSIVE = registerAmmo("g12_explosive", () -> { if (XFactory12ga.ITEM_G12_EXPLOSIVE == null) XFactory12ga.ITEM_G12_EXPLOSIVE = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_EXPLOSIVE; });
    public static final DeferredItem<Item> G12_PHOSPHORUS = registerAmmo("g12_phosphorus", () -> { if (XFactory12ga.ITEM_G12_PHOSPHORUS == null) XFactory12ga.ITEM_G12_PHOSPHORUS = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_PHOSPHORUS; });
    /** Secret round - hidden from the creative tab, see class javadoc. Backs both g12_equestrian_bj and _tkr. */
    public static final DeferredItem<Item> G12_EQUESTRIAN = registerAmmoHidden("g12_equestrian", () -> { if (XFactory12ga.ITEM_G12_EQUESTRIAN == null) XFactory12ga.ITEM_G12_EQUESTRIAN = new Item(new Item.Properties()); return XFactory12ga.ITEM_G12_EQUESTRIAN; });

    // ==================== 10ga ammo (5) ====================
    public static final DeferredItem<Item> G10 = registerAmmo("g10", () -> { if (XFactory10ga.ITEM_G10 == null) XFactory10ga.ITEM_G10 = new Item(new Item.Properties()); return XFactory10ga.ITEM_G10; });
    public static final DeferredItem<Item> G10_SHRAPNEL = registerAmmo("g10_shrapnel", () -> { if (XFactory10ga.ITEM_G10_SHRAPNEL == null) XFactory10ga.ITEM_G10_SHRAPNEL = new Item(new Item.Properties()); return XFactory10ga.ITEM_G10_SHRAPNEL; });
    public static final DeferredItem<Item> G10_DU = registerAmmo("g10_du", () -> { if (XFactory10ga.ITEM_G10_DU == null) XFactory10ga.ITEM_G10_DU = new Item(new Item.Properties()); return XFactory10ga.ITEM_G10_DU; });
    public static final DeferredItem<Item> G10_SLUG = registerAmmo("g10_slug", () -> { if (XFactory10ga.ITEM_G10_SLUG == null) XFactory10ga.ITEM_G10_SLUG = new Item(new Item.Properties()); return XFactory10ga.ITEM_G10_SLUG; });
    public static final DeferredItem<Item> G10_EXPLOSIVE = registerAmmo("g10_explosive", () -> { if (XFactory10ga.ITEM_G10_EXPLOSIVE == null) XFactory10ga.ITEM_G10_EXPLOSIVE = new Item(new Item.Properties()); return XFactory10ga.ITEM_G10_EXPLOSIVE; });

    // ==================== 12ga guns (8) ====================
    public static final DeferredItem<Item> GUN_MARESLEG = registerGun("gun_maresleg", XFactory12ga::gun_maresleg);
    public static final DeferredItem<Item> GUN_MARESLEG_AKIMBO = registerGun("gun_maresleg_akimbo", XFactory12ga::gun_maresleg_akimbo);
    public static final DeferredItem<Item> GUN_MARESLEG_BROKEN = registerGun("gun_maresleg_broken", XFactory12ga::gun_maresleg_broken);
    public static final DeferredItem<Item> GUN_LIBERATOR = registerGun("gun_liberator", XFactory12ga::gun_liberator);
    public static final DeferredItem<Item> GUN_SPAS12 = registerGun("gun_spas12", XFactory12ga::gun_spas12);
    public static final DeferredItem<Item> GUN_AUTOSHOTGUN = registerGun("gun_autoshotgun", XFactory12ga::gun_autoshotgun);
    public static final DeferredItem<Item> GUN_AUTOSHOTGUN_SHREDDER = registerGun("gun_autoshotgun_shredder", XFactory12ga::gun_autoshotgun_shredder);
    public static final DeferredItem<Item> GUN_AUTOSHOTGUN_SEXY = registerGun("gun_autoshotgun_sexy", XFactory12ga::gun_autoshotgun_sexy);

    // ==================== 10ga guns (3) ====================
    public static final DeferredItem<Item> GUN_DOUBLE_BARREL = registerGun("gun_double_barrel", XFactory10ga::gun_double_barrel);
    public static final DeferredItem<Item> GUN_DOUBLE_BARREL_SACRED_DRAGON = registerGun("gun_double_barrel_sacred_dragon", XFactory10ga::gun_double_barrel_sacred_dragon);
    /** Debug-quality; its actual default ammo is G10 (10ga), not 12ga, despite the "autoshotgun" name - see XFactory10ga's class javadoc. */
    public static final DeferredItem<Item> GUN_AUTOSHOTGUN_HERETIC = registerGun("gun_autoshotgun_heretic", XFactory10ga::gun_autoshotgun_heretic);

    /** No-op beyond forcing this class (and the 2 XFactory* content classes it references) to load before {@code ModItems.ITEMS.register(modEventBus)}. */
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
     * Takes a {@link Supplier}, not an already-constructed {@code Item}: every gun in this batch is
     * built via a static factory method (see e.g. {@link XFactory12ga#gun_maresleg()}), not an eager
     * {@code static final} field, specifically so constructing it (which resolves a SoundEvent
     * {@code DeferredHolder} via {@code Receiver.sound(...).get()}) is deferred until
     * {@code RegisterEvent(ITEM)} actually fires, not evaluated at this class's own load time.
     */
    private static DeferredItem<Item> registerGun(String name, Supplier<? extends ItemGunBaseNT> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }
}
