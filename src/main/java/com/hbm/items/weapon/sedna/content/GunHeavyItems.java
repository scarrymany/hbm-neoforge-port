package com.hbm.items.weapon.sedna.content;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registered {@code Item}s for the heavy-weapon/utility batch of the Sedna gun roster
 * ({@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryRocket}/{@code XFactoryTool}/
 * {@code XFactoryFlamer}/{@code XFactoryDrill}/{@code XFactoryPA} families, plus {@code GunFactory}
 * itself's {@code gun_debug}): {@code gun_panzerschreck}, {@code gun_stinger}, {@code gun_quadro},
 * {@code gun_missile_launcher}, {@code gun_fireext}, {@code gun_charge_thrower},
 * {@code gun_flamer}(+2 variants), {@code gun_chemthrower}, {@code gun_drill}, {@code gun_pa_melee},
 * {@code gun_pa_ranged}, {@code gun_debug} - 13 guns total - plus every ammo {@code Item} those guns'
 * {@code BulletConfig}s bind to. Mirrors {@code GunPistolItems}/{@code GunEnergyItems}'s exact
 * per-batch aggregator shape.
 * <p>
 * {@code gun_debug}'s {@code ammo_debug} is a bespoke {@code Item}, not part of {@code ammo_standard}
 * (matches {@code XFactoryDebug}'s own javadoc) - still registered/creative-tabbed the same way every
 * other ammo item in this batch is, since it is a real holdable/craftable-adjacent item either way.
 */
public final class GunHeavyItems {

    private GunHeavyItems() {
    }

    // ==================== rocket ammo (5) ====================
    public static final DeferredItem<Item> ROCKET_HE = registerAmmo("rocket_he", () -> { if (XFactoryRocket.ITEM_ROCKET_HE == null) XFactoryRocket.ITEM_ROCKET_HE = new Item(new Item.Properties()); return XFactoryRocket.ITEM_ROCKET_HE; });
    public static final DeferredItem<Item> ROCKET_HEAT = registerAmmo("rocket_heat", () -> { if (XFactoryRocket.ITEM_ROCKET_HEAT == null) XFactoryRocket.ITEM_ROCKET_HEAT = new Item(new Item.Properties()); return XFactoryRocket.ITEM_ROCKET_HEAT; });
    public static final DeferredItem<Item> ROCKET_DEMO = registerAmmo("rocket_demo", () -> { if (XFactoryRocket.ITEM_ROCKET_DEMO == null) XFactoryRocket.ITEM_ROCKET_DEMO = new Item(new Item.Properties()); return XFactoryRocket.ITEM_ROCKET_DEMO; });
    public static final DeferredItem<Item> ROCKET_INC = registerAmmo("rocket_inc", () -> { if (XFactoryRocket.ITEM_ROCKET_INC == null) XFactoryRocket.ITEM_ROCKET_INC = new Item(new Item.Properties()); return XFactoryRocket.ITEM_ROCKET_INC; });
    public static final DeferredItem<Item> ROCKET_PHOSPHORUS = registerAmmo("rocket_phosphorus", () -> { if (XFactoryRocket.ITEM_ROCKET_PHOSPHORUS == null) XFactoryRocket.ITEM_ROCKET_PHOSPHORUS = new Item(new Item.Properties()); return XFactoryRocket.ITEM_ROCKET_PHOSPHORUS; });

    // ==================== tool ammo (6) ====================
    public static final DeferredItem<Item> FEXT_WATER = registerAmmo("fext_water", () -> { if (XFactoryTool.ITEM_FEXT_WATER == null) XFactoryTool.ITEM_FEXT_WATER = new Item(new Item.Properties()); return XFactoryTool.ITEM_FEXT_WATER; });
    public static final DeferredItem<Item> FEXT_FOAM = registerAmmo("fext_foam", () -> { if (XFactoryTool.ITEM_FEXT_FOAM == null) XFactoryTool.ITEM_FEXT_FOAM = new Item(new Item.Properties()); return XFactoryTool.ITEM_FEXT_FOAM; });
    public static final DeferredItem<Item> FEXT_SAND = registerAmmo("fext_sand", () -> { if (XFactoryTool.ITEM_FEXT_SAND == null) XFactoryTool.ITEM_FEXT_SAND = new Item(new Item.Properties()); return XFactoryTool.ITEM_FEXT_SAND; });
    public static final DeferredItem<Item> CT_HOOK = registerAmmo("ct_hook", () -> { if (XFactoryTool.ITEM_CT_HOOK == null) XFactoryTool.ITEM_CT_HOOK = new Item(new Item.Properties()); return XFactoryTool.ITEM_CT_HOOK; });
    public static final DeferredItem<Item> CT_MORTAR = registerAmmo("ct_mortar", () -> { if (XFactoryTool.ITEM_CT_MORTAR == null) XFactoryTool.ITEM_CT_MORTAR = new Item(new Item.Properties()); return XFactoryTool.ITEM_CT_MORTAR; });
    public static final DeferredItem<Item> CT_MORTAR_CHARGE = registerAmmo("ct_mortar_charge", () -> { if (XFactoryTool.ITEM_CT_MORTAR_CHARGE == null) XFactoryTool.ITEM_CT_MORTAR_CHARGE = new Item(new Item.Properties()); return XFactoryTool.ITEM_CT_MORTAR_CHARGE; });

    // ==================== flamer ammo (4) ====================
    public static final DeferredItem<Item> FLAME_DIESEL = registerAmmo("flame_diesel", () -> { if (XFactoryFlamer.ITEM_FLAME_DIESEL == null) XFactoryFlamer.ITEM_FLAME_DIESEL = new Item(new Item.Properties()); return XFactoryFlamer.ITEM_FLAME_DIESEL; });
    public static final DeferredItem<Item> FLAME_GAS = registerAmmo("flame_gas", () -> { if (XFactoryFlamer.ITEM_FLAME_GAS == null) XFactoryFlamer.ITEM_FLAME_GAS = new Item(new Item.Properties()); return XFactoryFlamer.ITEM_FLAME_GAS; });
    public static final DeferredItem<Item> FLAME_NAPALM = registerAmmo("flame_napalm", () -> { if (XFactoryFlamer.ITEM_FLAME_NAPALM == null) XFactoryFlamer.ITEM_FLAME_NAPALM = new Item(new Item.Properties()); return XFactoryFlamer.ITEM_FLAME_NAPALM; });
    public static final DeferredItem<Item> FLAME_BALEFIRE = registerAmmo("flame_balefire", () -> { if (XFactoryFlamer.ITEM_FLAME_BALEFIRE == null) XFactoryFlamer.ITEM_FLAME_BALEFIRE = new Item(new Item.Properties()); return XFactoryFlamer.ITEM_FLAME_BALEFIRE; });

    // ==================== debug ammo (1, not part of ammo_standard) ====================
    public static final DeferredItem<Item> AMMO_DEBUG = registerAmmo("ammo_debug", () -> { if (XFactoryDebug.ITEM_AMMO_DEBUG == null) XFactoryDebug.ITEM_AMMO_DEBUG = new Item(new Item.Properties()); return XFactoryDebug.ITEM_AMMO_DEBUG; });

    // ==================== rocket guns (4) ====================
    public static final DeferredItem<Item> GUN_PANZERSCHRECK = registerGun("gun_panzerschreck", XFactoryRocket::gun_panzerschreck);
    public static final DeferredItem<Item> GUN_STINGER = registerGun("gun_stinger", XFactoryRocket::gun_stinger);
    public static final DeferredItem<Item> GUN_QUADRO = registerGun("gun_quadro", XFactoryRocket::gun_quadro);
    public static final DeferredItem<Item> GUN_MISSILE_LAUNCHER = registerGun("gun_missile_launcher", XFactoryRocket::gun_missile_launcher);

    // ==================== tool guns (2) ====================
    public static final DeferredItem<Item> GUN_FIREEXT = registerGun("gun_fireext", XFactoryTool::gun_fireext);
    public static final DeferredItem<Item> GUN_CHARGE_THROWER = registerGun("gun_charge_thrower", XFactoryTool::gun_charge_thrower);

    // ==================== flamer guns (4) ====================
    public static final DeferredItem<Item> GUN_FLAMER = registerGun("gun_flamer", XFactoryFlamer::gun_flamer);
    public static final DeferredItem<Item> GUN_FLAMER_TOPAZ = registerGun("gun_flamer_topaz", XFactoryFlamer::gun_flamer_topaz);
    public static final DeferredItem<Item> GUN_FLAMER_DAYBREAKER = registerGun("gun_flamer_daybreaker", XFactoryFlamer::gun_flamer_daybreaker);
    public static final DeferredItem<Item> GUN_CHEMTHROWER = registerGun("gun_chemthrower", XFactoryFlamer::gun_chemthrower);

    // ==================== drill gun (1) ====================
    public static final DeferredItem<Item> GUN_DRILL = registerGun("gun_drill", XFactoryDrill::gun_drill);

    // ==================== power-armor dispatch guns (2) ====================
    public static final DeferredItem<Item> GUN_PA_MELEE = registerGun("gun_pa_melee", XFactoryPA::gun_pa_melee);
    public static final DeferredItem<Item> GUN_PA_RANGED = registerGun("gun_pa_ranged", XFactoryPA::gun_pa_ranged);

    // ==================== debug gun (1) ====================
    public static final DeferredItem<Item> GUN_DEBUG = registerGun("gun_debug", XFactoryDebug::gun_debug);

    public static void registerAll() {
    }

    private static DeferredItem<Item> registerAmmo(String name, java.util.function.Supplier<Item> instance) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, instance);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    private static DeferredItem<Item> registerGun(String name, Supplier<? extends ItemGunBaseNT> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }
}
