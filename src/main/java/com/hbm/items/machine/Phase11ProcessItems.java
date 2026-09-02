package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ItemModCladding;
import com.hbm.items.food.ItemLemon;
import com.hbm.items.special.ItemConsumable;
import com.hbm.items.special.ItemFuel;
import com.hbm.items.special.ItemHot;
import com.hbm.items.weapon.ItemArtyShell;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Cheap CE items that unblock leftover chemplant / mixer / PUREX / liquefactor / solidifier / PA.
 * Citations: {@code ModItems.java:393} {@code fuel_additive}, {@code :1231-1232} biomass,
 * {@code :1287} {@code pellet_charged}, {@code :1325} {@code oil_tar}/{@code EnumTarType},
 * {@code :1330-1333} {@code solid_fuel*}, {@code :1155} {@code dust}, {@code :1234}/{:1237}
 * cordite/ball_tnt, {@code :2314+} {@code particle_*}, {@code :943} {@code bio_wafer},
 * {@code :1842} {@code upgrade_template}, {@code :2461}/{@code :2492-2535} missile parts,
 * {@code :1151-1154} scrap family, {@code :1310} {@code pipes_steel},
 * {@code :1994-2000} debris_* (ShredderRecipes.java:208/:347/:405-410),
 * {@code sawblade}/{@code mold_base}/{@code deuterium_filter}/{@code egg_glyphid}/
 * {@code flame_pony}/{@code blade_titanium}/{@code blade_tungsten}/{@code blade_meteorite}
 * (Anvil leftover I/O). {@code lignite} CE {@code ItemFuel} 1200 ({@code ModItems.java:1339}).
 * {@code wings_*} live in {@code JetpackItems} as {@code WingsMurk}.
 */
public final class Phase11ProcessItems {

    public static DeferredItem<Item> PELLET_CHARGED;
    public static DeferredItem<Item> BIOMASS;
    public static DeferredItem<Item> BIOMASS_COMPRESSED;
    public static DeferredItem<Item> BIO_WAFER;
    public static DeferredItem<Item> FUEL_ADDITIVE_ANTIKNOCK;
    public static DeferredItem<Item> FUEL_ADDITIVE_DEICER;
    public static DeferredItem<Item> NUCLEAR_WASTE_TINY;
    public static DeferredItem<Item> NUCLEAR_WASTE_VITRIFIED;
    public static DeferredItem<Item> DUST;
    public static DeferredItem<Item> SOLID_FUEL;
    public static DeferredItem<Item> SOLID_FUEL_BF;
    public static DeferredItem<Item> CORDITE;
    public static DeferredItem<Item> BALL_TNT;
    public static DeferredItem<Item> BALL_DYNAMITE;
    public static DeferredItem<Item> BALL_TATB;
    public static DeferredItem<Item> ROCKET_FUEL;
    public static DeferredItem<Item> CANISTER_NAPALM;
    public static DeferredItem<Item> PART_LITHIUM;
    public static DeferredItem<Item> PART_BERYLLIUM;
    public static DeferredItem<Item> PART_CARBON;
    public static DeferredItem<Item> PART_COPPER;
    public static DeferredItem<Item> PART_PLUTONIUM;
    public static DeferredItem<Item> SOLINIUM_IGNITER;
    public static DeferredItem<Item> SOLINIUM_PROPELLANT;

    private Phase11ProcessItems() {
    }

    public static void registerAll() {
        PELLET_CHARGED = parts("pellet_charged");
        BIOMASS = fuel("biomass", 20);
        BIOMASS_COMPRESSED = fuel("biomass_compressed", 800);
        BIO_WAFER = food("bio_wafer", 8, 8F);
        FUEL_ADDITIVE_ANTIKNOCK = control("fuel_additive_antiknock");
        FUEL_ADDITIVE_DEICER = control("fuel_additive_deicer");
        NUCLEAR_WASTE_TINY = nuke("nuclear_waste_tiny");
        NUCLEAR_WASTE_VITRIFIED = nuke("nuclear_waste_vitrified");
        DUST = parts("dust");
        // CE ModItems.java:1151-1154 — ShredderRecipes.java:208 / :352 / :360 / :401
        parts("scrap");
        parts("scrap_nuclear");
        parts("scrap_oil");
        hidden("scrap_plastic");
        // CE ModItems.java:1310 — ShredderRecipes.java:347
        parts("pipes_steel");
        // CE ModItems.java:1994-2000 — ShredderRecipes.java:405-410
        control("debris_concrete");
        control("debris_shrapnel");
        control("debris_exchanger");
        control("debris_element");
        control("debris_metal");
        control("debris_graphite");
        SOLID_FUEL = fuel("solid_fuel", 3200);
        // CE ModItems.java:1291 ItemFuel 4800 — CrystallizerRecipes.java:99
        fuel("coal_infernal", 4800);
        SOLID_FUEL_BF = parts("solid_fuel_bf");
        CORDITE = parts("cordite");
        BALL_TNT = parts("ball_tnt");
        BALL_DYNAMITE = parts("ball_dynamite");
        BALL_TATB = parts("ball_tatb");
        ROCKET_FUEL = fuel("rocket_fuel", 6400);
        CANISTER_NAPALM = parts("canister_napalm");
        PART_LITHIUM = parts("part_lithium");
        PART_BERYLLIUM = parts("part_beryllium");
        PART_CARBON = parts("part_carbon");
        PART_COPPER = parts("part_copper");
        PART_PLUTONIUM = parts("part_plutonium");
        // CE ModItems.java sulfur / niter dusts (not crystal_*). Gates chem.uf6 / meatprocessing.
        parts("sulfur");
        parts("niter");
        // CE ModItems.java:1339 ItemFuel 1200 — OreEnum.LIGNITE drop
        fuel("lignite", 1200);
        // CE EnumCasingType flatten — AmmoPressRecipes.java:47-59
        parts("casing_small");
        parts("casing_large");
        parts("casing_small_steel");
        parts("casing_large_steel");
        parts("casing_shotshell");
        parts("casing_buckshot");
        parts("casing_buckshot_advanced");
        // CE ModItems.assembly_nuke — AmmoPressRecipes.java:1061
        parts("assembly_nuke");

        for (ItemEnums.EnumTarType type : ItemEnums.EnumTarType.VALUES) {
            parts("oil_tar_" + type.name().toLowerCase());
        }
        // CE ModItems.java:1273 ItemEnumMulti chunk_ore / EnumChunkType
        for (ItemEnums.EnumChunkType type : ItemEnums.EnumChunkType.VALUES) {
            parts("chunk_ore_" + type.name().toLowerCase());
        }

        String[] particles = {
                "particle_empty", "particle_hydrogen", "particle_copper", "particle_lead",
                "particle_amat", "particle_aschrab", "particle_dark", "particle_higgs",
                "particle_tachyon", "particle_strange", "particle_sparkticle"
        };
        for (String id : particles) {
            control(id);
        }

        // CE ModItems.java:1842 — SolderingRecipes.java:192-282 upgrade-template family
        parts1("upgrade_template");
        // CE ModItems.java:1861 / ArcWelderRecipes.java:59-65
        parts("neutron_reflector");
        // CE ModItems.java:2461 / 2492-2535 — ArcWelderRecipes.java:166-215 missile parts
        parts1("missile_assembly");
        parts("thruster_small");
        parts("thruster_medium");
        parts("thruster_large");
        parts("fuel_tank_small");
        parts("fuel_tank_medium");
        parts("fuel_tank_large");
        // CE ModItems.java:2492-2508 — ArcWelderRecipes.java:217-350 finished-missile inputs
        parts("warhead_generic_small");
        parts("warhead_incendiary_small");
        parts("warhead_cluster_small");
        parts("warhead_buster_small");
        parts("warhead_generic_medium");
        parts("warhead_incendiary_medium");
        parts("warhead_cluster_medium");
        parts("warhead_buster_medium");
        parts("warhead_generic_large");
        parts("warhead_incendiary_large");
        parts("warhead_cluster_large");
        parts("warhead_buster_large");
        parts("warhead_nuclear");
        parts("warhead_mirv");
        parts("warhead_volcano");

        // CE ModItems.java:1302 / :2525-2529 — ArcWelderRecipes.java:366-400 satellite heads.
        // Finished sats already live as sat_mapper/scanner/radar/laser/resonator (MachineItems).
        parts("sat_base");
        parts("sat_head_mapper");
        parts("sat_head_scanner");
        parts("sat_head_radar");
        parts("sat_head_laser");
        parts("sat_head_resonator");
        // CE ModItems.java:1301 — AssemblyMachineRecipes.java:969 sat_base input
        parts("photo_panel");
        // CE ModItems.java (ballistite) — PowderRecipes.java:25 leftover vanilla craft
        parts("ballistite");

        // CE ModItems.java:1303 / AssemblyMachineRecipes.java:820 thrusternerva + :1015 satelliterelay
        parts("thruster_nuclear");
        // CE ModItems.java:2727 / AssemblyMachineRecipes.java:362 teleporter input
        parts("entanglement_kit");
        // CE ModItems.java:2536 / AssemblyMachineRecipes.java yellowbarrel
        parts("tank_steel");
        // CE ModItems.java:1281 / :1289 — cluster/buckshot leftover assembler
        parts("pellet_buckshot");
        parts("pellet_cluster");
        // CE ModItems.java:2530-2532 — mp_* assembler inputs
        parts("seg_10");
        parts("seg_15");
        parts("seg_20");
        // CE ModItems.java:1134 — F.dust() / leftover Powder+Consumable crafts
        parts("fluorite");
        // CE ModItems.java:1296 — cladding crafts
        parts("ducttape");
        // CE AnvilRecipes leftover I/O — assets already in tree (tex/model/lang).
        parts("sawblade");
        parts("mold_base");
        parts("deuterium_filter");
        // CE ModItems.java:1297 ItemBase partsTab — deuterium_filter + gas_mask_filter_* crafts
        parts("catalyst_clay");
        // CE ModItems.java:1304 ItemBakedBase partsTab + CraftingManager.java:831
        parts("safety_fuse");
        // CE ModItems.java:239 ItemModSensor — I/O + assets; no invented sensor HUD
        consume("gas_tester");
        // CE ModItems.java:284 ItemCustomLore max16 controlTab — energy_core shapeless
        control16("fuse");
        // CE ModItems.java:1843 ItemEnumMulti parts_legendary / EnumLegendaryType (TIER1/TIER2/TIER3) - ArmorRecipes.java:95-98 RPA armor
        parts1("parts_legendary_tier1");
        parts1("parts_legendary_tier2");
        parts1("parts_legendary_tier3");
        // CE ModItems.java:399 ItemBase controlTab — t51/ajr/liquidator plate crafts
        control("gas_empty");
        // CE ModItems.java:1305 / :1307 ItemBase; :887 ItemHot(200). Mold/hot recipes stay cited.
        parts("blade_titanium");
        parts("blade_tungsten");
        partsHot("blade_meteorite", 200);
        consume("egg_glyphid");
        parts("flame_pony");
        // CE ModItems.java:1173 — syringe_metal_empty input
        control("rod_empty");
        // CE dysfunctional_reactor — ass.protoreactor / ninadidnothingwrong
        parts("dysfunctional_reactor");

        // CE ModItems.java:117-126 syringes. ConsumableRecipes.java:96-114
        consume("syringe_empty");
        consumeFx("syringe_antidote");
        consumeFx("syringe_poison");
        consumeFx("syringe_awesome");
        consume("syringe_metal_empty");
        consumeFx("syringe_metal_stimpak");
        consumeFx("syringe_metal_medx");
        consumeFx("syringe_metal_psycho");
        consumeFx("syringe_metal_super");
        consumeFx("syringe_taint");
        consumeFx("med_bag");

        // CE ModItems.java:191-197 ItemModCladding / ConsumableRecipes.java:151-157
        cladding("cladding_paint", 0.025);
        cladding("cladding_rubber", 0.05);
        cladding("cladding_lead", 0.1);
        cladding("cladding_desh", 0.2);
        cladding("cladding_ghiorsium", 0.5);
        cladding("cladding_iron", 0.0);
        cladding("cladding_obsidian", 0.0);

        // CE ModItems.java:1175 / AssemblyMachineRecipes ass.protoreactor
        control("rod_quad_empty");
        control("rod_dual_empty");
        // CE ModItems.java:1135 — LI.ingot() (OreDictManager P_WHITE/LI frames; not ingot_lithium)
        parts("lithium");
        // CE ModItems.java:2515-2519 nuke fins
        parts("fins_flat");
        parts("fins_small_steel");
        parts("fins_big_steel");
        parts("fins_tri_steel");
        parts("fins_quad_titanium");
        // CE ModItems.java:2521 / ass.gadget
        parts("pedestal_steel");
        // CE ModItems.java:2397-2398 / ass.solinium*
        SOLINIUM_IGNITER = nuke1("solinium_igniter");
        SOLINIUM_PROPELLANT = nuke1("solinium_propellant");
        // CE ModItems.java:417 / ass.emptypackage
        control("fluid_pack_empty");
        // CE ModItems.java:2490 / ass.lander
        parts1("missile_soyuz_lander");
        // CE ItemAmmoHIMARS.RocketType / AssemblyMachineRecipes.java:767-781
        weapon1("ammo_himars_small");
        weapon1("ammo_himars_small_he");
        weapon1("ammo_himars_small_wp");
        weapon1("ammo_himars_small_tb");
        weapon1("ammo_himars_small_mini_nuke");
        weapon1("ammo_himars_small_lava");
        weapon1("ammo_himars_large");
        weapon1("ammo_himars_large_tb");
        // CE ItemAmmoArty metas 0–11 / WeaponRecipes.java:240-248
        weaponArty("ammo_arty_normal");
        weaponArty("ammo_arty_classic");
        weaponArty("ammo_arty_he");
        weaponArty("ammo_arty_mini_nuke");
        weaponArty("ammo_arty_nuke");
        weaponArty("ammo_arty_phosphorus");
        weaponArty("ammo_arty_mini_nuke_multi");
        weaponArty("ammo_arty_phosphorus_multi");
        weaponArty("ammo_arty_cargo");
        weaponArty("ammo_arty_chlorine");
        weaponArty("ammo_arty_phosgene");
        weaponArty("ammo_arty_mustard");
        // CE ModItems.java:1360 / ass.capfritz
        consume("cap_fritz");
        // CE ItemEnums.EnumSecretType / ass.50bmgbypass — CE tab=null
        hidden("item_secret_selenium_steel");

        // CE ModItems.java:1241-1245 consumable items for legendary gun crafts (PedestalRecipes.java)
        // CE cite: PedestalRecipes.java:71 (BONE.grip), :109 (morning_glory), :119 (wild_p/card_aos/card_qos)
        consume("morning_glory");
        parts("bone_grip");
        consume("wild_p");
        consume("card_aos");
        consume("card_qos");
        // CE ModItems.java:1172 ItemBase partsTab — CE PedestalRecipes.java:57/:117 (barbed_wire recipe)
        // bolt_spike×16 (CE has bolt_spike commented out in ItemBoltgun.java:58 with //FIXME)
        // Register as parts item with TODO cite
        // TODO(CE: PedestalRecipes.java:57/117): bolt_spike not fully implemented in CE itself (commented FIXME)
        parts("bolt_spike");

        // CE ModItems.java:1323 coke flatten + :1211 catalytic_converter (reformer/hydrotreater slot).
        fuel("coke_coal", 3200);
        fuel("coke_lignite", 3200);
        fuel("coke_petroleum", 3200);
        parts1("catalytic_converter");
        // CE ModItems.java:775-777 ItemBakedBase weaponTab (default stack 64).
        weapon("stick_tnt");
        weapon("stick_semtex");
        weapon("stick_c4");
    }

    private static DeferredItem<Item> parts(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    private static DeferredItem<Item> partsHot(String name, int maxHeat) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemHot(new Item.Properties(), maxHeat));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    private static DeferredItem<Item> parts1(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties().stacksTo(1)));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    private static DeferredItem<Item> control(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, item);
        return item;
    }

    private static DeferredItem<Item> control16(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties().stacksTo(16)));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, item);
        return item;
    }

    private static DeferredItem<Item> nuke(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.NUKE, item);
        return item;
    }

    private static DeferredItem<Item> fuel(String name, int burn) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemFuel(new Item.Properties(), burn));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        FoodProperties food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemLemon(new Item.Properties().food(food)));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    private static DeferredItem<Item> consume(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, item);
        return item;
    }

    private static DeferredItem<Item> consumeFx(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemConsumable(new Item.Properties(), "syringe_awesome".equals(name)));
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, item);
        return item;
    }

    private static DeferredItem<Item> cladding(String name, double rad) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemModCladding(new Item.Properties(), rad));
        CreativeTabContents.add(ModCreativeTabs.CONSUMABLE, item);
        return item;
    }

    private static DeferredItem<Item> nuke1(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties().stacksTo(1)));
        CreativeTabContents.add(ModCreativeTabs.NUKE, item);
        return item;
    }

    private static DeferredItem<Item> weapon(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    private static DeferredItem<Item> weapon1(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties().stacksTo(1)));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    private static DeferredItem<Item> weaponArty(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemArtyShell(new Item.Properties().stacksTo(1)));
        CreativeTabContents.add(ModCreativeTabs.WEAPON, item);
        return item;
    }

    private static DeferredItem<Item> hidden(String name) {
        return ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
    }
}
