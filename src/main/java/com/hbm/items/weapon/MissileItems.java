package com.hbm.items.weapon;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.weapon.ItemMissile.FuelType;
import com.hbm.items.weapon.ItemMissile.PartSize;
import com.hbm.items.weapon.ItemMissile.Rarity;
import com.hbm.items.weapon.ItemMissile.WarheadType;
import com.hbm.items.weapon.ItemMissileStandard.MissileFormFactor;
import com.hbm.items.weapon.ItemMissileStandard.MissileFuel;
import com.hbm.items.weapon.ItemMissileStandard.MissileTier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.function.Supplier;

/**
 * Registers {@code missile_custom} ({@link ItemCustomMissile}), the 28 real launchable
 * {@link ItemMissileStandard} presets, and the 64 structurally-distinct {@code mp_*} parts
 * ({@link ItemMissile}) - i.e. every real, non-cosmetic-clone missile item CE has (see
 * {@code docs/phase3/missile_framework.md}'s headline finding: {@code mp_*} is 125 items total,
 * but 61 of those are pure cosmetic {@code .copy(name)} reskins feeding {@code ItemLootCrate}'s
 * rarity-weighted roll tables - {@code ItemLootCrate} does not exist in this port yet, Phase-3-
 * blocked, so those 61 reskins are deferred to whichever pass lands that loot-crate system, not
 * silently dropped forever).
 * <p>
 * Every field name/value below is copied verbatim from CE's real {@code ModItems.java} (grep-
 * confirmed exact match, including the odd registry-name-vs-field-name mismatch on the 5
 * {@code mp_chip_*} items - CE registers them as {@code "mp_c_1".."mp_c_5"}, not {@code
 * "mp_chip_1"} etc., preserved here exactly since registry names are player-visible/save-
 * compatible identifiers).
 */
public final class MissileItems {

    // --- missile_custom -----------------------------------------------------------------------

    public static DeferredItem<ItemCustomMissile> MISSILE_CUSTOM;

    // --- the 28 real launchable ItemMissileStandard presets ------------------------------------

    public static DeferredItem<ItemMissileStandard> MISSILE_GENERIC;
    public static DeferredItem<ItemMissileStandard> MISSILE_STRONG;
    public static DeferredItem<ItemMissileStandard> MISSILE_BURST;
    public static DeferredItem<ItemMissileStandard> MISSILE_INCENDIARY;
    public static DeferredItem<ItemMissileStandard> MISSILE_INCENDIARY_STRONG;
    public static DeferredItem<ItemMissileStandard> MISSILE_INFERNO;
    public static DeferredItem<ItemMissileStandard> MISSILE_CLUSTER;
    public static DeferredItem<ItemMissileStandard> MISSILE_CLUSTER_STRONG;
    public static DeferredItem<ItemMissileStandard> MISSILE_RAIN;
    public static DeferredItem<ItemMissileStandard> MISSILE_BUSTER;
    public static DeferredItem<ItemMissileStandard> MISSILE_BUSTER_STRONG;
    public static DeferredItem<ItemMissileStandard> MISSILE_DRILL;
    public static DeferredItem<ItemMissileStandard> MISSILE_N2;
    public static DeferredItem<ItemMissileStandard> MISSILE_NUCLEAR;
    public static DeferredItem<ItemMissileStandard> MISSILE_NUCLEAR_CLUSTER;
    public static DeferredItem<ItemMissileStandard> MISSILE_VOLCANO;
    public static DeferredItem<ItemMissileStandard> MISSILE_SHUTTLE;
    public static DeferredItem<ItemMissileStandard> MISSILE_DOOMSDAY;
    public static DeferredItem<ItemMissileStandard> MISSILE_DOOMSDAY_RUSTED;
    public static DeferredItem<ItemMissileStandard> MISSILE_TAINT;
    public static DeferredItem<ItemMissileStandard> MISSILE_MICRO;
    public static DeferredItem<ItemMissileStandard> MISSILE_BHOLE;
    public static DeferredItem<ItemMissileStandard> MISSILE_SCHRABIDIUM;
    public static DeferredItem<ItemMissileStandard> MISSILE_EMP;
    public static DeferredItem<ItemMissileStandard> MISSILE_EMP_STRONG;
    public static DeferredItem<ItemMissileStandard> MISSILE_ANTI_BALLISTIC;
    public static DeferredItem<ItemMissileStandard> MISSILE_DECOY;
    public static DeferredItem<ItemMissileStandard> MISSILE_STEALTH;

    // --- mp_* parts: 5 chips -------------------------------------------------------------------

    public static DeferredItem<ItemMissile> MP_CHIP_1;
    public static DeferredItem<ItemMissile> MP_CHIP_2;
    public static DeferredItem<ItemMissile> MP_CHIP_3;
    public static DeferredItem<ItemMissile> MP_CHIP_4;
    public static DeferredItem<ItemMissile> MP_CHIP_5;

    // --- mp_* parts: 17 warheads (the 2 further .copy() cosmetic reskins are deferred) ----------

    public static DeferredItem<ItemMissile> MP_WARHEAD_10_HE;
    public static DeferredItem<ItemMissile> MP_WARHEAD_10_INCENDIARY;
    public static DeferredItem<ItemMissile> MP_WARHEAD_10_BUSTER;
    public static DeferredItem<ItemMissile> MP_WARHEAD_10_NUCLEAR;
    public static DeferredItem<ItemMissile> MP_WARHEAD_10_NUCLEAR_LARGE;
    public static DeferredItem<ItemMissile> MP_WARHEAD_10_TAINT;
    public static DeferredItem<ItemMissile> MP_WARHEAD_10_CLOUD;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_HE;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_INCENDIARY;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_NUCLEAR;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_THERMO;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_MIRV;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_BOXCAR;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_N2;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_BALEFIRE;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_VOLCANO;
    public static DeferredItem<ItemMissile> MP_WARHEAD_15_TURBINE;

    // --- mp_* parts: 15 fuselages ----------------------------------------------------------------

    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_KEROSENE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_SOLID;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_XENON;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_LONG_KEROSENE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_LONG_SOLID;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_15_KEROSENE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_15_SOLID;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_15_HYDROGEN;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_10_15_BALEFIRE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_15_KEROSENE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_15_SOLID;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_15_HYDROGEN;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_15_BALEFIRE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_15_20_KEROSENE;
    public static DeferredItem<ItemMissile> MP_FUSELAGE_15_20_SOLID;

    // --- mp_* parts: 7 stability (fins) ----------------------------------------------------------

    public static DeferredItem<ItemMissile> MP_STABILITY_10_FLAT;
    public static DeferredItem<ItemMissile> MP_STABILITY_10_CRUISE;
    public static DeferredItem<ItemMissile> MP_STABILITY_10_SPACE;
    public static DeferredItem<ItemMissile> MP_STABILITY_15_FLAT;
    public static DeferredItem<ItemMissile> MP_STABILITY_15_THIN;
    public static DeferredItem<ItemMissile> MP_STABILITY_15_SOYUZ;
    public static DeferredItem<ItemMissile> MP_STABILITY_20_FLAT;

    // --- mp_* parts: 20 thrusters -----------------------------------------------------------------

    public static DeferredItem<ItemMissile> MP_THRUSTER_10_KEROSENE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_10_SOLID;
    public static DeferredItem<ItemMissile> MP_THRUSTER_10_XENON;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_KEROSENE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_KEROSENE_DUAL;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_KEROSENE_TRIPLE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_SOLID;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_SOLID_HEXDECUPLE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_HYDROGEN;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_HYDROGEN_DUAL;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_BALEFIRE_SHORT;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_BALEFIRE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_BALEFIRE_LARGE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_15_BALEFIRE_LARGE_RAD;
    public static DeferredItem<ItemMissile> MP_THRUSTER_20_KEROSENE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_20_KEROSENE_DUAL;
    public static DeferredItem<ItemMissile> MP_THRUSTER_20_KEROSENE_TRIPLE;
    public static DeferredItem<ItemMissile> MP_THRUSTER_20_SOLID;
    public static DeferredItem<ItemMissile> MP_THRUSTER_20_SOLID_MULTI;
    public static DeferredItem<ItemMissile> MP_THRUSTER_20_SOLID_MULTIER;

    private MissileItems() {
    }

    public static void registerAll() {
        MISSILE_CUSTOM = item("missile_custom", () -> new ItemCustomMissile(new Item.Properties().stacksTo(1)));

        MISSILE_GENERIC = standard("missile_generic", MissileFormFactor.V2, MissileTier.TIER1);
        MISSILE_STRONG = standard("missile_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
        MISSILE_BURST = standard("missile_burst", MissileFormFactor.HUGE, MissileTier.TIER3);
        MISSILE_INCENDIARY = standard("missile_incendiary", MissileFormFactor.V2, MissileTier.TIER1);
        MISSILE_INCENDIARY_STRONG = standard("missile_incendiary_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
        MISSILE_INFERNO = standard("missile_inferno", MissileFormFactor.HUGE, MissileTier.TIER3);
        MISSILE_CLUSTER = standard("missile_cluster", MissileFormFactor.V2, MissileTier.TIER1);
        MISSILE_CLUSTER_STRONG = standard("missile_cluster_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
        MISSILE_RAIN = standard("missile_rain", MissileFormFactor.HUGE, MissileTier.TIER3);
        MISSILE_BUSTER = standard("missile_buster", MissileFormFactor.V2, MissileTier.TIER1);
        MISSILE_BUSTER_STRONG = standard("missile_buster_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
        MISSILE_DRILL = standard("missile_drill", MissileFormFactor.HUGE, MissileTier.TIER3);
        MISSILE_N2 = standard("missile_n2", MissileFormFactor.HUGE, MissileTier.TIER3);
        MISSILE_NUCLEAR = standard("missile_nuclear", MissileFormFactor.ATLAS, MissileTier.TIER4);
        MISSILE_NUCLEAR_CLUSTER = standard("missile_nuclear_cluster", MissileFormFactor.ATLAS, MissileTier.TIER4);
        MISSILE_VOLCANO = standard("missile_volcano", MissileFormFactor.ATLAS, MissileTier.TIER4);
        MISSILE_SHUTTLE = itemStandard("missile_shuttle", () ->
                new ItemMissileStandard(new Item.Properties(), MissileFormFactor.OTHER, MissileTier.TIER3, MissileFuel.KEROSENE_PEROXIDE));
        MISSILE_DOOMSDAY = standard("missile_doomsday", MissileFormFactor.ATLAS, MissileTier.TIER4);
        MISSILE_DOOMSDAY_RUSTED = itemStandard("missile_doomsday_rusted", () ->
                new ItemMissileStandard(new Item.Properties(), MissileFormFactor.ATLAS, MissileTier.TIER4).notLaunchable());
        MISSILE_TAINT = standard("missile_taint", MissileFormFactor.MICRO, MissileTier.TIER0);
        MISSILE_MICRO = standard("missile_micro", MissileFormFactor.MICRO, MissileTier.TIER0);
        MISSILE_BHOLE = standard("missile_bhole", MissileFormFactor.MICRO, MissileTier.TIER0);
        MISSILE_SCHRABIDIUM = standard("missile_schrabidium", MissileFormFactor.MICRO, MissileTier.TIER0);
        MISSILE_EMP = standard("missile_emp", MissileFormFactor.MICRO, MissileTier.TIER0);
        MISSILE_EMP_STRONG = standard("missile_emp_strong", MissileFormFactor.STRONG, MissileTier.TIER2);
        MISSILE_ANTI_BALLISTIC = standard("missile_anti_ballistic", MissileFormFactor.ABM, MissileTier.TIER1);
        MISSILE_DECOY = standard("missile_decoy", MissileFormFactor.V2, MissileTier.TIER1);
        MISSILE_STEALTH = standard("missile_stealth", MissileFormFactor.STRONG, MissileTier.TIER1);

        MP_CHIP_1 = part("mp_c_1", () -> new ItemMissile(new Item.Properties()).makeChip(0.1F));
        MP_CHIP_2 = part("mp_c_2", () -> new ItemMissile(new Item.Properties()).makeChip(0.05F));
        MP_CHIP_3 = part("mp_c_3", () -> new ItemMissile(new Item.Properties()).makeChip(0.01F));
        MP_CHIP_4 = part("mp_c_4", () -> new ItemMissile(new Item.Properties()).makeChip(0.005F));
        MP_CHIP_5 = part("mp_c_5", () -> new ItemMissile(new Item.Properties()).makeChip(0.0F));

        MP_WARHEAD_10_HE = part("mp_warhead_10_he", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.HE, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
        MP_WARHEAD_10_INCENDIARY = part("mp_warhead_10_incendiary", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.INC, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
        MP_WARHEAD_10_BUSTER = part("mp_warhead_10_buster", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.BUSTER, 15F, 1.5F, PartSize.SIZE_10).setHealth(5F));
        MP_WARHEAD_10_NUCLEAR = part("mp_warhead_10_nuclear", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.NUCLEAR, 35F, 1.5F, PartSize.SIZE_10).setTitle("Tater Tot").setHealth(10F));
        MP_WARHEAD_10_NUCLEAR_LARGE = part("mp_warhead_10_nuclear_large", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.NUCLEAR, 75F, 2.5F, PartSize.SIZE_10).setTitle("Chernobyl Boris").setHealth(15F));
        MP_WARHEAD_10_TAINT = part("mp_warhead_10_taint", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.TAINT, 15F, 1.5F, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.UNCOMMON)
                        .setWittyText("Eat my taint! Bureaucracy is dead and we killed it!"));
        MP_WARHEAD_10_CLOUD = part("mp_warhead_10_cloud", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.CLOUD, 15F, 1.5F, PartSize.SIZE_10).setHealth(20F).setRarity(Rarity.RARE));
        MP_WARHEAD_15_HE = part("mp_warhead_15_he", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.HE, 50F, 2.5F, PartSize.SIZE_15).setHealth(10F));
        MP_WARHEAD_15_INCENDIARY = part("mp_warhead_15_incendiary", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.INC, 35F, 2.5F, PartSize.SIZE_15).setHealth(10F));
        MP_WARHEAD_15_NUCLEAR = part("mp_warhead_15_nuclear", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.NUCLEAR, 125F, 5F, PartSize.SIZE_15).setTitle("Auntie Bertha").setHealth(15F));
        MP_WARHEAD_15_THERMO = part("mp_warhead_15_thermo", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.TX, 250F, 6.5F, PartSize.SIZE_15).setHealth(25F).setRarity(Rarity.RARE));
        MP_WARHEAD_15_MIRV = part("mp_warhead_15_mirv", () ->
                // 70F = CE BombConfig.mirvRadius default. Live blast uses EntityMIRV + MIRV_RADIUS.get().
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.MIRV, 70F, 7.0F, PartSize.SIZE_15)
                        .setRarity(Rarity.LEGENDARY).setAuthor("Seven").setHealth(20F).setWittyText("I wanna know, have you ever seen the rain?"));
        MP_WARHEAD_15_BOXCAR = part("mp_warhead_15_boxcar", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.TX, 500F, 7.5F, PartSize.SIZE_15).setWittyText("?!?!").setHealth(35F).setRarity(Rarity.LEGENDARY));
        MP_WARHEAD_15_N2 = part("mp_warhead_15_n2", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.N2, 100F, 5F, PartSize.SIZE_15).setWittyText("[screams geometrically]").setHealth(20F).setRarity(Rarity.RARE));
        MP_WARHEAD_15_BALEFIRE = part("mp_warhead_15_balefire", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.BALEFIRE, 100F, 7.5F, PartSize.SIZE_15)
                        .setRarity(Rarity.LEGENDARY).setAuthor("VT-6/24").setHealth(15F).setWittyText("Hightower, never forgetti."));
        MP_WARHEAD_15_VOLCANO = part("mp_warhead_15_volcano", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.VOLCANO, 10F, 6.5F, PartSize.SIZE_15).setHealth(25F).setRarity(Rarity.LEGENDARY));
        MP_WARHEAD_15_TURBINE = part("mp_warhead_15_turbine", () ->
                new ItemMissile(new Item.Properties()).makeWarhead(WarheadType.TURBINE, 200F, 5F, PartSize.SIZE_15).setHealth(250F)
                        .setRarity(Rarity.SEWS_CLOTHES_AND_SUCKS_HORSE_COCK));

        MP_FUSELAGE_10_KEROSENE = part("mp_fuselage_10_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.KEROSENE, 2500F, 1000, PartSize.SIZE_10, PartSize.SIZE_10).setAuthor("Hoboy").setHealth(20F));
        MP_FUSELAGE_10_SOLID = part("mp_fuselage_10_solid", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.SOLID, 2500F, 1000, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(25F));
        MP_FUSELAGE_10_XENON = part("mp_fuselage_10_xenon", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.XENON, 5000F, 1000, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(20F));
        MP_FUSELAGE_10_LONG_KEROSENE = part("mp_fuselage_10_long_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.KEROSENE, 5000F, 1000, PartSize.SIZE_10, PartSize.SIZE_10).setAuthor("Hoboy").setHealth(30F));
        MP_FUSELAGE_10_LONG_SOLID = part("mp_fuselage_10_long_solid", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.SOLID, 5000F, 1000, PartSize.SIZE_10, PartSize.SIZE_10).setHealth(35F));
        MP_FUSELAGE_10_15_KEROSENE = part("mp_fuselage_10_15_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.KEROSENE, 10000F, 1000, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
        MP_FUSELAGE_10_15_SOLID = part("mp_fuselage_10_15_solid", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.SOLID, 10000F, 1000, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
        MP_FUSELAGE_10_15_HYDROGEN = part("mp_fuselage_10_15_hydrogen", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.HYDROGEN, 10000F, 1000, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
        MP_FUSELAGE_10_15_BALEFIRE = part("mp_fuselage_10_15_balefire", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.BALEFIRE, 10000F, 1000, PartSize.SIZE_10, PartSize.SIZE_15).setHealth(40F));
        MP_FUSELAGE_15_KEROSENE = part("mp_fuselage_15_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.KEROSENE, 15000F, 1000, PartSize.SIZE_15, PartSize.SIZE_15).setAuthor("Hoboy").setHealth(50F));
        MP_FUSELAGE_15_SOLID = part("mp_fuselage_15_solid", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.SOLID, 15000F, 1000, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(60F));
        MP_FUSELAGE_15_HYDROGEN = part("mp_fuselage_15_hydrogen", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.HYDROGEN, 15000F, 1000, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(50F));
        MP_FUSELAGE_15_BALEFIRE = part("mp_fuselage_15_balefire", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.BALEFIRE, 15000F, 1000, PartSize.SIZE_15, PartSize.SIZE_15).setHealth(75F));
        MP_FUSELAGE_15_20_KEROSENE = part("mp_fuselage_15_20_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.KEROSENE, 20000F, 1000, PartSize.SIZE_15, PartSize.SIZE_20).setAuthor("Hoboy").setHealth(70F));
        MP_FUSELAGE_15_20_SOLID = part("mp_fuselage_15_20_solid", () ->
                new ItemMissile(new Item.Properties()).makeFuselage(FuelType.SOLID, 20000F, 1000, PartSize.SIZE_15, PartSize.SIZE_20).setHealth(70F));

        MP_STABILITY_10_FLAT = part("mp_stability_10_flat", () -> new ItemMissile(new Item.Properties()).makeStability(0.5F, PartSize.SIZE_10).setHealth(10F));
        MP_STABILITY_10_CRUISE = part("mp_stability_10_cruise", () -> new ItemMissile(new Item.Properties()).makeStability(0.25F, PartSize.SIZE_10).setHealth(5F));
        MP_STABILITY_10_SPACE = part("mp_stability_10_space", () ->
                new ItemMissile(new Item.Properties()).makeStability(0.35F, PartSize.SIZE_10).setHealth(5F).setRarity(Rarity.COMMON)
                        .setWittyText("Standing there alone, the ship is waiting / All systems are go, are you sure?"));
        MP_STABILITY_15_FLAT = part("mp_stability_15_flat", () -> new ItemMissile(new Item.Properties()).makeStability(0.5F, PartSize.SIZE_15).setHealth(10F));
        MP_STABILITY_15_THIN = part("mp_stability_15_thin", () -> new ItemMissile(new Item.Properties()).makeStability(0.35F, PartSize.SIZE_15).setHealth(5F));
        MP_STABILITY_15_SOYUZ = part("mp_stability_15_soyuz", () ->
                new ItemMissile(new Item.Properties()).makeStability(0.25F, PartSize.SIZE_15).setHealth(15F).setRarity(Rarity.COMMON).setWittyText("Союз!"));
        MP_STABILITY_20_FLAT = part("mp_stability_20_flat", () -> new ItemMissile(new Item.Properties()).makeStability(0.5F, PartSize.SIZE_20));

        MP_THRUSTER_10_KEROSENE = part("mp_thruster_10_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 1.5F, PartSize.SIZE_10).setHealth(10F));
        MP_THRUSTER_10_SOLID = part("mp_thruster_10_solid", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.SOLID, 1F, 1.5F, PartSize.SIZE_10).setHealth(15F));
        MP_THRUSTER_10_XENON = part("mp_thruster_10_xenon", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.XENON, 1F, 1.5F, PartSize.SIZE_10).setHealth(5F));
        MP_THRUSTER_15_KEROSENE = part("mp_thruster_15_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 7.5F, PartSize.SIZE_15).setHealth(15F));
        MP_THRUSTER_15_KEROSENE_DUAL = part("mp_thruster_15_kerosene_dual", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 6.5F, PartSize.SIZE_15).setHealth(15F));
        MP_THRUSTER_15_KEROSENE_TRIPLE = part("mp_thruster_15_kerosene_triple", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 5F, PartSize.SIZE_15).setHealth(15F));
        MP_THRUSTER_15_SOLID = part("mp_thruster_15_solid", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.SOLID, 1F, 5F, PartSize.SIZE_15).setHealth(20F));
        MP_THRUSTER_15_SOLID_HEXDECUPLE = part("mp_thruster_15_solid_hexdecuple", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.SOLID, 1F, 7F, PartSize.SIZE_15).setHealth(25F).setRarity(Rarity.UNCOMMON));
        MP_THRUSTER_15_HYDROGEN = part("mp_thruster_15_hydrogen", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.HYDROGEN, 1F, 7.5F, PartSize.SIZE_15).setHealth(20F));
        MP_THRUSTER_15_HYDROGEN_DUAL = part("mp_thruster_15_hydrogen_dual", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.HYDROGEN, 1F, 5.0F, PartSize.SIZE_15).setHealth(15F));
        MP_THRUSTER_15_BALEFIRE_SHORT = part("mp_thruster_15_balefire_short", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.BALEFIRE, 1F, 5F, PartSize.SIZE_15).setHealth(25F));
        MP_THRUSTER_15_BALEFIRE = part("mp_thruster_15_balefire", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.BALEFIRE, 1F, 6.5F, PartSize.SIZE_15).setHealth(25F));
        MP_THRUSTER_15_BALEFIRE_LARGE = part("mp_thruster_15_balefire_large", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.BALEFIRE, 1F, 7.0F, PartSize.SIZE_15).setHealth(35F));
        MP_THRUSTER_15_BALEFIRE_LARGE_RAD = part("mp_thruster_15_balefire_large_rad", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.BALEFIRE, 1F, 7.5F, PartSize.SIZE_15).setAuthor("The Master").setHealth(35F).setRarity(Rarity.UNCOMMON));
        MP_THRUSTER_20_KEROSENE = part("mp_thruster_20_kerosene", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
        MP_THRUSTER_20_KEROSENE_DUAL = part("mp_thruster_20_kerosene_dual", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
        MP_THRUSTER_20_KEROSENE_TRIPLE = part("mp_thruster_20_kerosene_triple", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.KEROSENE, 1F, 100F, PartSize.SIZE_20).setHealth(30F));
        MP_THRUSTER_20_SOLID = part("mp_thruster_20_solid", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F)
                        .setWittyText("It's basically just a big hole at the end of the fuel tank."));
        MP_THRUSTER_20_SOLID_MULTI = part("mp_thruster_20_solid_multi", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F));
        MP_THRUSTER_20_SOLID_MULTIER = part("mp_thruster_20_solid_multier", () ->
                new ItemMissile(new Item.Properties()).makeThruster(FuelType.SOLID, 1F, 100F, PartSize.SIZE_20).setHealth(35F).setWittyText("Did I miscount? Hope not."));
    }

    private static DeferredItem<ItemMissile> part(String name, Supplier<ItemMissile> factory) {
        DeferredItem<ItemMissile> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, item);
        return item;
    }

    private static DeferredItem<ItemMissileStandard> standard(String name, MissileFormFactor form, MissileTier tier) {
        return itemStandard(name, () -> new ItemMissileStandard(new Item.Properties(), form, tier));
    }

    private static DeferredItem<ItemMissileStandard> itemStandard(String name, Supplier<ItemMissileStandard> factory) {
        DeferredItem<ItemMissileStandard> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, item);
        return item;
    }

    private static <T extends Item> DeferredItem<T> item(String name, Supplier<T> factory) {
        DeferredItem<T> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.MISSILE, item);
        return item;
    }
}
