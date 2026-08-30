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
 * ({@code docs/phase3/guns_and_ammo.md}'s {@code XFactory40mm}/{@code XFactory75Bolt} families):
 * {@code gun_flaregun}, {@code gun_congolake}, {@code gun_mk108}, {@code gun_bolter} - 4 guns total -
 * plus every real ammo {@code Item} those guns' {@link com.hbm.items.weapon.sedna.BulletConfig}s bind
 * to. Mirrors {@link GunRifleItems}'s exact shape (same per-batch aggregator convention, same
 * {@code registerAmmo}/{@code registerGun} helper pair, same metadata-flattening rationale - see that
 * class's javadoc). Neither ammo family in this batch has any secret/hidden rounds, so
 * {@code registerAmmoHidden} is not needed here.
 */
public final class GunLauncherItems {

    private GunLauncherItems() {
    }

    // ==================== 40mm ammo (8) ====================
    public static final DeferredItem<Item> G26_FLARE = registerAmmo("g26_flare", XFactory40mm.ITEM_G26_FLARE);
    public static final DeferredItem<Item> G26_FLARE_SUPPLY = registerAmmo("g26_flare_supply", XFactory40mm.ITEM_G26_FLARE_SUPPLY);
    public static final DeferredItem<Item> G26_FLARE_WEAPON = registerAmmo("g26_flare_weapon", XFactory40mm.ITEM_G26_FLARE_WEAPON);
    public static final DeferredItem<Item> G40_HE = registerAmmo("g40_he", XFactory40mm.ITEM_G40_HE);
    public static final DeferredItem<Item> G40_HEAT = registerAmmo("g40_heat", XFactory40mm.ITEM_G40_HEAT);
    public static final DeferredItem<Item> G40_DEMO = registerAmmo("g40_demo", XFactory40mm.ITEM_G40_DEMO);
    public static final DeferredItem<Item> G40_INC = registerAmmo("g40_inc", XFactory40mm.ITEM_G40_INC);
    public static final DeferredItem<Item> G40_PHOSPHORUS = registerAmmo("g40_phosphorus", XFactory40mm.ITEM_G40_PHOSPHORUS);

    // ==================== 7.5mm bolt ammo (3) ====================
    public static final DeferredItem<Item> B75 = registerAmmo("b75", XFactory75Bolt.ITEM_B75);
    public static final DeferredItem<Item> B75_INC = registerAmmo("b75_inc", XFactory75Bolt.ITEM_B75_INC);
    public static final DeferredItem<Item> B75_EXP = registerAmmo("b75_exp", XFactory75Bolt.ITEM_B75_EXP);

    // ==================== 40mm guns (3) ====================
    public static final DeferredItem<Item> GUN_FLAREGUN = registerGun("gun_flaregun", XFactory40mm::gun_flaregun);
    public static final DeferredItem<Item> GUN_CONGOLAKE = registerGun("gun_congolake", XFactory40mm::gun_congolake);
    public static final DeferredItem<Item> GUN_MK108 = registerGun("gun_mk108", XFactory40mm::gun_mk108);

    // ==================== 7.5mm bolt gun (1) ====================
    public static final DeferredItem<Item> GUN_BOLTER = registerGun("gun_bolter", XFactory75Bolt::gun_bolter);

    /** No-op beyond forcing this class (and the 2 XFactory* content classes it references) to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    private static DeferredItem<Item> registerAmmo(String name, Item instance) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> instance);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    /**
     * Takes a {@link Supplier}, not an already-constructed {@code Item}: every gun in this batch is
     * built via a static factory method (see e.g. {@link XFactory40mm#gun_flaregun()}), not an eager
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
