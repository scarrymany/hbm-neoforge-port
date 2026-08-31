package com.hbm.items.weapon.sedna.content;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registered {@code Item}s for the energy/beam-weapon batch of the Sedna gun roster
 * ({@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryAccelerator}/{@code XFactoryEnergy}/
 * {@code XFactoryFolly}/{@code XFactory35800} families): {@code gun_tau}, {@code gun_coilgun},
 * {@code gun_n_i_4_n_i}, {@code gun_tesla_cannon}, {@code gun_laser_pistol}(+2 variants),
 * {@code gun_lasrifle}, {@code gun_fatman}, {@code gun_folly}, {@code gun_aberrator}(+akimbo
 * variant) - 11 guns total - plus every ammo {@code Item} those guns' {@code BulletConfig}s bind to.
 * Mirrors {@code GunPistolItems}'s exact per-batch aggregator shape (same lazy-{@code Supplier}
 * gun-registration convention - every gun below is a static factory <i>method</i> on its
 * {@code XFactory*} class, not an eager field, for the same "sound field resolves before any
 * {@code RegisterEvent}" hazard that class's javadoc documents).
 * <p>
 * {@code folly_sm}/{@code folly_nuke} back CE's hidden {@code EnumAmmoSecret} (no creative tab),
 * matching {@code GunPistolItems}/{@code GunRifleItems}'s identical {@code registerAmmoHidden}
 * treatment of other secret rounds.
 */
public final class GunEnergyItems {

    private GunEnergyItems() {
    }

    // ==================== accelerator ammo ====================
    public static final DeferredItem<Item> TAU_URANIUM = registerAmmo("tau_uranium", () -> { if (XFactoryAccelerator.ITEM_TAU_URANIUM == null) XFactoryAccelerator.ITEM_TAU_URANIUM = new Item(new Item.Properties()); return XFactoryAccelerator.ITEM_TAU_URANIUM; });
    public static final DeferredItem<Item> COIL_TUNGSTEN = registerAmmo("coil_tungsten", () -> { if (XFactoryAccelerator.ITEM_COIL_TUNGSTEN == null) XFactoryAccelerator.ITEM_COIL_TUNGSTEN = new Item(new Item.Properties()); return XFactoryAccelerator.ITEM_COIL_TUNGSTEN; });
    public static final DeferredItem<Item> COIL_FERROURANIUM = registerAmmo("coil_ferrouranium", () -> { if (XFactoryAccelerator.ITEM_COIL_FERROURANIUM == null) XFactoryAccelerator.ITEM_COIL_FERROURANIUM = new Item(new Item.Properties()); return XFactoryAccelerator.ITEM_COIL_FERROURANIUM; });

    // ==================== energy ammo ====================
    public static final DeferredItem<Item> CAPACITOR = registerAmmo("capacitor", () -> { if (XFactoryEnergy.ITEM_CAPACITOR == null) XFactoryEnergy.ITEM_CAPACITOR = new Item(new Item.Properties()); return XFactoryEnergy.ITEM_CAPACITOR; });
    public static final DeferredItem<Item> CAPACITOR_OVERCHARGE = registerAmmo("capacitor_overcharge", () -> { if (XFactoryEnergy.ITEM_CAPACITOR_OVERCHARGE == null) XFactoryEnergy.ITEM_CAPACITOR_OVERCHARGE = new Item(new Item.Properties()); return XFactoryEnergy.ITEM_CAPACITOR_OVERCHARGE; });
    public static final DeferredItem<Item> CAPACITOR_IR = registerAmmo("capacitor_ir", () -> { if (XFactoryEnergy.ITEM_CAPACITOR_IR == null) XFactoryEnergy.ITEM_CAPACITOR_IR = new Item(new Item.Properties()); return XFactoryEnergy.ITEM_CAPACITOR_IR; });

    // ==================== folly ammo (secret, 2) ====================
    public static final DeferredItem<Item> FOLLY_SM = registerAmmoHidden("folly_sm", () -> { if (XFactoryFolly.ITEM_FOLLY_SM == null) XFactoryFolly.ITEM_FOLLY_SM = new Item(new Item.Properties()); return XFactoryFolly.ITEM_FOLLY_SM; });
    public static final DeferredItem<Item> FOLLY_NUKE = registerAmmoHidden("folly_nuke", () -> { if (XFactoryFolly.ITEM_FOLLY_NUKE == null) XFactoryFolly.ITEM_FOLLY_NUKE = new Item(new Item.Properties()); return XFactoryFolly.ITEM_FOLLY_NUKE; });

    // ==================== 35800 ammo (secret) ====================
    public static final DeferredItem<Item> P35800 = registerAmmoHidden("p35_800", () -> { if (XFactory35800.ITEM_P35800 == null) XFactory35800.ITEM_P35800 = new Item(new Item.Properties()); return XFactory35800.ITEM_P35800; });
    public static final DeferredItem<Item> P35800_BL = registerAmmoHidden("p35_800_bl", () -> { if (XFactory35800.ITEM_P35800_BL == null) XFactory35800.ITEM_P35800_BL = new Item(new Item.Properties()); return XFactory35800.ITEM_P35800_BL; });

    // ==================== accelerator guns (3) ====================
    public static final DeferredItem<Item> GUN_TAU = registerGun("gun_tau", XFactoryAccelerator::gun_tau);
    public static final DeferredItem<Item> GUN_COILGUN = registerGun("gun_coilgun", XFactoryAccelerator::gun_coilgun);
    public static final DeferredItem<Item> GUN_N_I_4_N_I = registerGun("gun_n_i_4_n_i", XFactoryAccelerator::gun_n_i_4_n_i);

    // ==================== energy guns (5, incl. gun_fatman per this port's roster grouping) ====================
    public static final DeferredItem<Item> GUN_TESLA_CANNON = registerGun("gun_tesla_cannon", XFactoryEnergy::gun_tesla_cannon);
    public static final DeferredItem<Item> GUN_LASER_PISTOL = registerGun("gun_laser_pistol", XFactoryEnergy::gun_laser_pistol);
    public static final DeferredItem<Item> GUN_LASER_PISTOL_PEW_PEW = registerGun("gun_laser_pistol_pew_pew", XFactoryEnergy::gun_laser_pistol_pew_pew);
    public static final DeferredItem<Item> GUN_LASER_PISTOL_MORNING_GLORY = registerGun("gun_laser_pistol_morning_glory", XFactoryEnergy::gun_laser_pistol_morning_glory);
    public static final DeferredItem<Item> GUN_LASRIFLE = registerGun("gun_lasrifle", XFactoryEnergy::gun_lasrifle);
    public static final DeferredItem<Item> GUN_FATMAN = registerGun("gun_fatman", XFactoryEnergy::gun_fatman);

    // ==================== folly gun (SECRET, 1) ====================
    public static final DeferredItem<Item> GUN_FOLLY = registerGunHidden("gun_folly", XFactoryFolly::gun_folly);

    // ==================== 35800 guns (SECRET, 2) ====================
    public static final DeferredItem<Item> GUN_ABERRATOR = registerGunHidden("gun_aberrator", XFactory35800::gun_aberrator);
    public static final DeferredItem<Item> GUN_ABERRATOR_EOTT = registerGunHidden("gun_aberrator_eott", XFactory35800::gun_aberrator_eott);

    public static void registerAll() {
    }

    private static DeferredItem<Item> registerAmmo(String name, java.util.function.Supplier<Item> instance) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, instance);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    private static DeferredItem<Item> registerAmmoHidden(String name, java.util.function.Supplier<Item> instance) {
        return ModItems.ITEMS.register(name, instance);
    }

    private static DeferredItem<Item> registerGun(String name, Supplier<? extends ItemGunBaseNT> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    /** SECRET-quality guns are still real items (a player can hold/fire them) but excluded from the creative tab, matching CE's {@code ammo_secret}/secret-gun treatment. */
    private static DeferredItem<Item> registerGunHidden(String name, Supplier<? extends ItemGunBaseNT> factory) {
        return ModItems.ITEMS.register(name, factory);
    }
}
