package com.hbm.datagen;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.ModItems;
import com.hbm.main.MainRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * English display names for every registered item/block, plus a large first real-content pass of
 * CE's {@code en_us.lang} corpus - ported per docs/phase5/lang_file_and_localization.md's research
 * (CE's real {@code assets/hbm/lang/en_us.lang}, 8,591 lines, read directly; category priority order
 * taken from this task's own brief: achievements first, then item/block display names for the
 * highest-visibility Phase 3/4 content, then GUI titles, then tooltips/misc/death messages).
 *
 * <p><b>Item/block names</b>: {@link #ITEM_NAMES}/{@link #BLOCK_NAMES} are a hand-built, CE-id-keyed
 * lookup (registry path -&gt; CE's real {@code item.<id>.name}/{@code tile.<id>.name} text, joined by
 * grepping this port's own gun/ammo/machine-block registration call sites for their literal registry
 * id strings and cross-checking each one against CE's real lang file - see this task's structured
 * notes for the exact match-rate found per category). Every registered item/block not present in
 * these maps still gets the pre-existing title-case-from-registry-id fallback, so nothing regresses to
 * a raw key. This is <b>not</b> the full 3,444/1,525-entry CE item/tile corpus (that needs a real
 * {@code ./gradlew runData} registry census this sandbox cannot run, per the research report's "Safe
 * to build now vs. blocked" section) - it is a curated, verified subset covering the Sedna gun/ammo
 * roster (161 items), the machine/RBMK/PWR/ICF block family (59 blocks), boss/mob entities (15),
 * potion effects (12), and creative tabs (10).
 *
 * <p><b>Achievements</b>: {@link #addAchievements()} ports all 66 real CE {@code achievement.<id>}/
 * {@code achievement.<id>.desc} pairs (132 lines, CE's real {@code en_us.lang:1-132}) plus the 6
 * {@code hbm.achievement.<id>} duplicates and the {@code hbm.advancement.root} pair that this port's
 * real 65-file advancement JSON set (mirrored from CE's own {@code assets/hbm/advancements/*.json},
 * confirmed by grepping every {@code "translate"} value across all 65 real CE advancement JSON files)
 * actually references - 130 distinct keys total, matching this task's brief almost exactly ("about
 * 132 of them"). c15's advancement JSON port draws from the same 65-file list; this class supplies the
 * lang values those JSONs' {@code title}/{@code description} {@code translate} keys resolve to.
 *
 * <p><b>Container/GUI titles</b>: {@link #addContainerTitles()} ports all 65 {@code container.*} keys
 * already referenced via {@code Component.translatable(...)} across this port's existing 55
 * {@code inventory/gui} {@code Screen} classes (54 matched CE's real key verbatim; the other 11 are
 * documented key-drift - this port's Java call site uses a key CE doesn't have under that exact name -
 * resolved here via a per-machine CE source grep, see the inline comments on each of those 11 for the
 * real CE key/value it was resolved from). Fixing the Java call sites themselves is out of this task's
 * file-ownership scope (those {@code Screen} classes belong to other Phase 5 tasks) - flagged in this
 * task's structured notes instead.
 *
 * <p><b>Everything else</b> ({@link #addEntityNames()}, {@link #addEffectNames()},
 * {@link #addCreativeTabTitles()}, {@link #addMiscKeys()}): every other lang key already referenced
 * from this port's Java via a string-literal {@code Component.translatable(...)} call (found by
 * grepping the whole {@code src/main/java/com/hbm} tree), ported from CE's real matching text where
 * one exists, or - for the handful of keys with zero CE lang-file source - transcribed from CE's real
 * hardcoded-in-Java English text where that exists instead (see {@link #addMiscKeys()}'s javadoc on
 * {@code desc.gun.*} for the one confirmed case), or left unset with the gap documented in this task's
 * notes where truly no CE source exists at all ({@code desc.hand_drill1}).
 *
 * <p><b>Death messages</b>: {@link #addDeathMessages()} now covers all 63 real CE
 * {@code death.attack.<msgId>} base identifiers (was 56/63 - the 7 missing ones flagged by the
 * research report are added at the end of that method, each commented with its CE source line).
 *
 * <p>Real, hand-written CE text for the remaining long tail (the ~5,200-entry item/tile bucket beyond
 * what's listed above, {@code desc.}/{@code hbmfluid.}/{@code hbm.}/{@code trait.rbmk.}/{@code book.}/
 * {@code gun.}/{@code chem.}/{@code hbmmat.}/{@code cannery.}/{@code ammo.}/{@code fluid.}/
 * {@code subtitles.}/{@code rbmk.}/{@code hadron.}/{@code armor.}/{@code pa.}/{@code key.}/
 * {@code radar.}/{@code commands.}/{@code upgrade.}/{@code tool.}/{@code crucible.}/{@code weapon.}/
 * {@code wavelengths.}/{@code warhead.}/{@code satchip.}/{@code armorMod.}/{@code hazard.}/
 * {@code icffuel.}/{@code battery.} and ~40 smaller prefixes, roughly 6,900 lines of CE's 8,591-line
 * corpus) remains a later polish pass - see this task's structured notes for the exact remaining-gap
 * map, matching the research report's own category table.
 */
public class ModLanguageProvider extends LanguageProvider {

    private final Set<String> seenKeys = new HashSet<>();

    public ModLanguageProvider(PackOutput output) {
        super(output, MainRegistry.MODID, "en_us");
    }

    /** BlockItems share {@code block.hbm.*} with their Block — LanguageProvider forbids dups. */
    @Override
    public void add(String key, String value) {
        if (!seenKeys.add(key)) return;
        super.add(key, value);
    }

    @Override
    protected void addTranslations() {
        Map<String, String> itemNames = itemNames();
        Map<String, String> blockNames = blockNames();

        ModItems.ITEMS.getEntries().forEach(holder -> {
            Item item = holder.get();
            String path = holder.getId().getPath();
            String name = itemNames.getOrDefault(path, titleCase(path));
            this.add(item.getDescriptionId(), name);
        });

        ModBlocks.BLOCKS.getEntries().forEach(holder -> {
            Block block = holder.get();
            String path = holder.getId().getPath();
            String name = blockNames.getOrDefault(path, titleCase(path));
            this.add(block.getDescriptionId(), name);
        });

        addAchievements();
        addEntityNames();
        addEffectNames();
        addCreativeTabTitles();
        addContainerTitles();
        addDeathMessages();
        addMiscKeys();
    }

    private static String titleCase(String registryPath) {
        String[] words = registryPath.split("_");
        StringBuilder result = new StringBuilder(registryPath.length());
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return result.toString();
    }

    // ==================== item/block display-name lookup tables ====================

    /**
     * CE's real {@code item.<id>.name} text for this port's Sedna gun/ammo roster
     * ({@code src/main/java/com/hbm/items/weapon/sedna/content/*.java}, registry ids extracted by
     * grepping every {@code registerGun("id", ...)}/{@code registerAmmo("id", ...)} call site) - 161
     * of 163 registered gun/ammo ids matched CE's real lang file exactly by id (guns and most ammo
     * under CE's plain {@code item.<id>.name}; the newer Sedna ammo variants under CE's real
     * {@code item.ammo_standard.<id>.name}/{@code item.ammo_secret.<id>.name} sub-namespace, confirmed
     * by grep). {@code gun_debug}/{@code ammo_debug} (dev-only) are deliberately left to the title-case
     * fallback - CE has no player-facing name for either.
     */
    private static Map<String, String> itemNames() {
        Map<String, String> m = new HashMap<>();

        // ---- guns (item.<id>.name, CE en_us.lang, exact id match) ----
        m.put("coil_tungsten", "Heating Coil");
        m.put("gun_am180", ".22 Submachine Gun");
        m.put("gun_amat", "Anti-Materiel Rifle");
        m.put("gun_amat_penance", "Penance");
        m.put("gun_amat_subtlety", "Subtlety");
        m.put("gun_autoshotgun", "Auto Shotgun");
        m.put("gun_autoshotgun_heretic", "The Heretic");
        m.put("gun_autoshotgun_sexy", "Sexy");
        m.put("gun_autoshotgun_shredder", "Shredder");
        m.put("gun_bolter", "Bolter");
        m.put("gun_carbine", "Carbine");
        m.put("gun_charge_thrower", "Charge Thrower");
        m.put("gun_chemthrower", "Chemthrower");
        m.put("gun_coilgun", "Coilgun");
        m.put("gun_congolake", "Congo Lake");
        m.put("gun_double_barrel", "An Old Classic");
        m.put("gun_double_barrel_sacred_dragon", "Sacred Dragon");
        m.put("gun_drill", "Powered Drill");
        m.put("gun_fatman", "Fat Man");
        m.put("gun_fireext", "Fire Extinguisher");
        m.put("gun_flamer", "Flamethrower");
        m.put("gun_flamer_daybreaker", "Daybreaker");
        m.put("gun_flamer_topaz", "Mister Topaz");
        m.put("gun_flaregun", "Flare Gun");
        m.put("gun_g3", "Assault Rifle");
        m.put("gun_g3_zebra", "Zebra Rifle");
        m.put("gun_greasegun", "Grease Gun");
        m.put("gun_hangman", "Hangman");
        m.put("gun_heavy_revolver", "Heavy Revolver");
        m.put("gun_heavy_revolver_lilmac", "Little Macintosh");
        m.put("gun_heavy_revolver_protege", "Protège");
        m.put("gun_henry", "Lever Action Rifle");
        m.put("gun_henry_lincoln", "Lincoln's Repeater");
        m.put("gun_lag", "Comically Long Pistol");
        m.put("gun_laser_pistol", "Laser Pistol");
        m.put("gun_laser_pistol_morning_glory", "Morning Glory");
        m.put("gun_laser_pistol_pew_pew", "Pew Pew");
        m.put("gun_lasrifle", "Laser Rifle");
        m.put("gun_liberator", "Liberator");
        m.put("gun_light_revolver", "Break-Action Revolver");
        m.put("gun_light_revolver_atlas", "Atlas");
        m.put("gun_light_revolver_dani", "Day And Night");
        m.put("gun_m2", "Ma Deuce");
        m.put("gun_maresleg", "Lever Action Shotgun");
        m.put("gun_maresleg_akimbo", "Lever Action Shotguns");
        m.put("gun_maresleg_broken", "Broken");
        m.put("gun_mas36", "South Star");
        m.put("gun_minigun", "Minigun");
        m.put("gun_minigun_dual", "Dual Miniguns");
        m.put("gun_minigun_lacunae", "Lacunae");
        m.put("gun_missile_launcher", "Missile Launcher");
        m.put("gun_mk108", "Grenade Machinegun");
        m.put("gun_n_i_4_n_i", "N I 4 N I");
        m.put("gun_pa_melee", "Power Armor - Melee Controller");
        m.put("gun_pa_ranged", "Power Armor - Ranged Controller");
        m.put("gun_panzerschreck", "Panzerschreck");
        m.put("gun_pepperbox", "Pepperbox");
        m.put("gun_quadro", "Quad Rocket Launcher");
        m.put("gun_spas12", "SPAS-12");
        m.put("gun_star_f", "Target Pistol");
        m.put("gun_star_f_akimbo", "Target Pistols");
        m.put("gun_stg77", "StG 77");
        m.put("gun_stinger", "FIM-92 Stinger");
        m.put("gun_tau", "Tau Cannon");
        m.put("gun_tesla_cannon", "Tesla Cannon");
        m.put("gun_uzi", "Uzi");
        m.put("gun_uzi_akimbo", "Uzis");

        // ---- ammo: CE's item.ammo_standard.<id>.name / item.ammo_secret.<id>.name ----
        m.put("bmg50_black", ".50 BMG Bypass Round");
        m.put("bmg50_equestrian", ".50 BMG Demolisher");
        m.put("folly_nuke", "Silver Bullet, Nuclear");
        m.put("folly_sm", "Silver Bullet");
        m.put("g12_equestrian", "12 Gauge Railway Spike Shot");
        m.put("m44_equestrian", ".44 Magnum Head-Exploder");
        m.put("p35_800", ".35-800 V9");
        m.put("p35_800_bl", ".35-800 V9 (Black Lightning)");
        m.put("b75", ".75 Bolt");
        m.put("b75_exp", ".75 Bolt (Explosive)");
        m.put("b75_inc", ".75 Bolt (Incendiary)");
        m.put("bmg50_ap", ".50 BMG Round (Armor Piercing)");
        m.put("bmg50_du", ".50 BMG Round (Depleted Uranium)");
        m.put("bmg50_fmj", ".50 BMG Round (Full Metal Jacket)");
        m.put("bmg50_he", ".50 BMG Round (High-Explosive)");
        m.put("bmg50_jhp", ".50 BMG Round (Jacketed Hollow Point)");
        m.put("bmg50_sm", ".50 BMG Round (Starmetal)");
        m.put("bmg50_sp", ".50 BMG Round (Soft Point)");
        m.put("capacitor", "Capacitor (Standard)");
        m.put("capacitor_ir", "Capacitor (Low Wavelength)");
        m.put("capacitor_overcharge", "Capacitor (Overcharge)");
        m.put("coil_ferrouranium", "Coilgun Ferrouranium Ball");
        m.put("ct_hook", "Grappling Hook");
        m.put("ct_mortar", "Demolition Charge");
        m.put("ct_mortar_charge", "Heavy Demolition Charge");
        m.put("flame_balefire", "Flamer Fuel, Balefire");
        m.put("flame_diesel", "Flamer Fuel, Diesel");
        m.put("flame_gas", "Flamer Fuel, Gas");
        m.put("flame_napalm", "Flamer Fuel, Napalm");
        m.put("g10", "10 Gauge Buckshot");
        m.put("g10_du", "10 Gauge Uranium Buckshot");
        m.put("g10_explosive", "10 Gauge Explosive Buckshot");
        m.put("g10_shrapnel", "10 Gauge Shrapnel Buckshot");
        m.put("g10_slug", "10 Gauge Slug");
        m.put("g12", "12 Gauge Buckshot");
        m.put("g12_bp", "12 Gauge Black Powder Buckshot");
        m.put("g12_bp_magnum", "12 Gauge Black Powder Magnum Shell");
        m.put("g12_bp_slug", "12 Gauge Black Powder Slug");
        m.put("g12_explosive", "12 Gauge Explosive Shell");
        m.put("g12_flechette", "12 Gauge Flechette Shell");
        m.put("g12_magnum", "12 Gauge Magnum Shell");
        m.put("g12_phosphorus", "12 Gauge Phosphorus Shell");
        m.put("g12_slug", "12 Gauge Slug");
        m.put("g26_flare", "26mm Signal Flare");
        m.put("g26_flare_supply", "26mm Signal Flare (Supply Airdrop)");
        m.put("g26_flare_weapon", "26mm Signal Flare (Weapon Airdrop)");
        m.put("g40_demo", "40mm Grenade, Demolition");
        m.put("g40_he", "40mm Grenade, High-Explosive");
        m.put("g40_heat", "40mm Grenade, Shaped Charge");
        m.put("g40_inc", "40mm Grenade, Incendiary");
        m.put("g40_phosphorus", "40mm Grenade, White Phosphorus");
        m.put("m357_ap", ".357 Magnum Round (Armor Piercing)");
        m.put("m357_bp", ".357 Magnum Round (Black Powder)");
        m.put("m357_express", ".357 Magnum Round (FMJ Express)");
        m.put("m357_fmj", ".357 Magnum Round (Full Metal Jacket)");
        m.put("m357_jhp", ".357 Magnum Round (Jacketed Hollow Point)");
        m.put("m357_sp", ".357 Magnum Round (Soft Point)");
        m.put("m44_ap", ".44 Magnum Round (Armor Piercing)");
        m.put("m44_bp", ".44 Magnum Round (Black Powder)");
        m.put("m44_express", ".44 Magnum Round (FMJ Express)");
        m.put("m44_fmj", ".44 Magnum Round (Full Metal Jacket)");
        m.put("m44_jhp", ".44 Magnum Round (Jacketed Hollow Point)");
        m.put("m44_sp", ".44 Magnum Round (Soft Point)");
        m.put("p22_ap", ".22 LR Round (Armor Piercing)");
        m.put("p22_fmj", ".22 LR Round (Full Metal Jacket)");
        m.put("p22_jhp", ".22 LR Round (Jacketed Hollow Point)");
        m.put("p22_sp", ".22 LR Round (Soft Point)");
        m.put("p9_ap", "9mm Round (Armor Piercing)");
        m.put("p9_fmj", "9mm Round (Full Metal Jacket)");
        m.put("p9_jhp", "9mm Round (Jacketed Hollow Point)");
        m.put("p9_sp", "9mm Round (Soft Point)");
        m.put("r556_ap", "5.56mm Round (Armor Piercing)");
        m.put("r556_fmj", "5.56mm Round (Full Metal Jacket)");
        m.put("r556_jhp", "5.56mm Round (Jacketed Hollow Point)");
        m.put("r556_sp", "5.56mm Round (Soft Point)");
        m.put("r762_ap", "7.62mm Round (Armor Piercing)");
        m.put("r762_du", "7.62mm Round (Depleted Uranium)");
        m.put("r762_fmj", "7.62mm Round (Full Metal Jacket)");
        m.put("r762_he", "7.62mm Round (High-Explosive)");
        m.put("r762_jhp", "7.62mm Round (Jacketed Hollow Point)");
        m.put("r762_sp", "7.62mm Round (Soft Point)");
        m.put("rocket_demo", "Rocket, Demolition");
        m.put("rocket_he", "Rocket, High-Explosive");
        m.put("rocket_heat", "Rocket, Shaped Charge");
        m.put("rocket_inc", "Rocket, Incendiary");
        m.put("rocket_phosphorus", "Rocket, White Phosphorus");
        m.put("stone", "Ball and Powder");
        m.put("stone_ap", "Flint and Powder");
        m.put("stone_iron", "Iron Ball and Powder");
        m.put("stone_shot", "Shot and Powder");
        m.put("tau_uranium", "Depleted Uranium-235 Box");

        // ---- fire-extinguisher ammo: CE's item.ammo_fireext[_x].name (port shortened id, values real) ----
        m.put("fext_water", "Fire Extinguisher Water Tank");
        m.put("fext_foam", "Fire Extinguisher Foam Tank");
        m.put("fext_sand", "Fire Extinguisher Sand Tank");

        return m;
    }

    /**
     * CE's real {@code tile.<id>.name} text for this port's machine/RBMK/PWR/ICF block family
     * ({@code src/main/java/com/hbm/blocks/machine/**}, registry ids extracted from every
     * {@code registerBlock("id", ...)} call site) - 59 of 60 registered block ids matched CE's real
     * lang file (7 needed a targeted per-block CE grep beyond the exact-id match, commented inline
     * below). {@code silo_hatch_drillgon} has no CE lang-file source at all (a port-specific silo-hatch
     * variant) and is deliberately left to the title-case fallback rather than guessed.
     */
    private static Map<String, String> blockNames() {
        Map<String, String> m = new HashMap<>();

        m.put("capacitor_bus", "Capacitor Bus");
        m.put("corium_block", "Corium");
        m.put("launchpad_soyuz", "Soyuz Launch Pad");
        m.put("machine_assembly_machine", "Assembly Machine");
        m.put("machine_centrifuge", "Centrifuge");
        m.put("machine_chemical_plant", "Chemical Plant");
        m.put("machine_combustion_engine", "Industrial Combustion Engine");
        m.put("machine_crystallizer", "Ore Acidizer");
        m.put("machine_cyclotron", "Cyclotron");
        m.put("machine_diesel", "Diesel Generator");
        m.put("machine_electrolyser", "Electrolysis Machine");
        m.put("machine_gascent", "Gas Centrifuge");
        m.put("machine_icf_press", "ICF Fuel Pellet Maker");
        m.put("machine_industrial_turbine", "Industrial Steam Turbine");
        m.put("machine_large_turbine", "Industrial Steam Turbine");
        m.put("machine_minirtg", "Radio Isotope Cell");
        m.put("machine_mixer", "Industrial Mixer");
        m.put("machine_powerrtg", "PT Isotope Cell");
        m.put("machine_reactor_breeding", "Breeding Reactor");
        m.put("machine_rtg_grey", "RT Generator");
        m.put("machine_shredder", "Shredder");
        m.put("machine_silex", "Laser Isotope Separation Chamber (SILEX)");
        m.put("machine_solar_boiler", "Solar Tower Boiler");
        m.put("machine_steam_engine", "Steam Engine");
        m.put("machine_turbine", "Steam Turbine");
        m.put("machine_turbine_gas", "Combined Cycle Gas Turbine");
        m.put("pwr_block", "PWR");
        m.put("pwr_channel", "PWR Coolant Channel");
        m.put("pwr_control", "PWR Control Rod");
        m.put("pwr_controller", "PWR Controller");
        m.put("pwr_fuelrod", "PWR Fuel Rod");
        m.put("rbmk_absorber", "RBMK Boron Neutron Absorber");
        m.put("rbmk_autoloader", "RBMK Autoloader");
        m.put("rbmk_blank", "RBMK Structural Column");
        m.put("rbmk_boiler", "RBMK Steam Channel");
        m.put("rbmk_console", "RBMK Console");
        m.put("rbmk_control", "RBMK Control Rods");
        m.put("rbmk_control_auto", "RBMK Automatic Control Rods");
        m.put("rbmk_control_mod", "RBMK Moderated Control Rods");
        m.put("rbmk_control_reasim", "RBMK Control Rods (ReaSim)");
        m.put("rbmk_control_reasim_auto", "RBMK Automatic Control Rods (ReaSim)");
        m.put("rbmk_cooler", "RBMK Cooler");
        m.put("rbmk_heater", "RBMK Fluid Heater");
        m.put("rbmk_moderator", "RBMK Graphite Moderator");
        m.put("rbmk_outgasser", "RBMK Irradiation Channel");
        m.put("rbmk_reflector", "RBMK Tungsten Carbide Neutron Reflector");
        m.put("rbmk_rod", "RBMK Fuel Channel");
        m.put("rbmk_rod_mod", "RBMK Moderated Fuel Channel");
        m.put("rbmk_rod_reasim", "RBMK Fuel Channel (ReaSim)");
        m.put("rbmk_rod_reasim_mod", "RBMK Moderated Fuel Channel (ReaSim)");
        m.put("rbmk_storage", "RBMK Storage Column");
        m.put("solar_mirror", "Heliostat Mirror");

        // ---- the 7 that needed a targeted CE grep beyond the exact-id match ----
        m.put("dummy_block_silo_hatch", "Silo Hatch"); // CE tile.silo_hatch.name (dummy passthrough block for the real hatch)
        m.put("machine_fluidtank_basic", "Tank"); // CE tile.machine_fluidtank.name (port's "_basic" suffix disambiguates from upgraded tiers)
        m.put("machine_icf_controller", "ICF Laser Controller"); // CE tile.icf_controller.name
        m.put("machine_icf_reactor", "Inertial Confinement Fusion Reactor (ICF)"); // CE tile.icf.name
        m.put("machine_watz_reactor", "Watz Powerplant"); // CE tile.watz.name
        m.put("rbmk_inlet", "RBMK ReaSim Water Inlet"); // CE tile.rbmk_steam_inlet.name
        m.put("rbmk_outlet", "RBMK ReaSim Steam Outlet"); // CE tile.rbmk_steam_outlet.name

        return m;
    }

    // ==================== achievements (needed by c15's advancement JSON port) ====================

    /**
     * All 66 real CE {@code achievement.<id>}/{@code achievement.<id>.desc} pairs (CE's real
     * {@code en_us.lang:1-132} verbatim), plus the {@code hbm.achievement.<id>} duplicates and the
     * {@code hbm.advancement.root} pair that CE's real 65-file advancement JSON set actually
     * references under that alternate prefix (confirmed by grepping every {@code "translate"} value
     * across all 65 real CE {@code assets/hbm/advancements/*.json} files - 6 of them use
     * {@code hbm.achievement.*} instead of {@code achievement.*} for historical reasons; CE's lang file
     * carries the same English text under both prefixes for 5 of those 6, and a 6th
     * ({@code progress_dfc}) only exists under the {@code hbm.achievement.} prefix at all).
     */
    private void addAchievements() {
        addAch("FOEQ", "Pegasi and Missile Silos", "Send a relay into martian...I mean dunaian orbit.");
        addAch("RBMK", "3.6 Roentegen?", "He's delusional, get him to the infirmary.");
        addAch("RBMKBoom", "It is 15,000.", "What is the cost of lies?");
        addAch("SILEX", "Separation of Isotopes by Laser Exitation", "It's cooler then it sounds, I promise.");
        addAch("ZIRNOXBoom", "CIRNOX", "cope, seethe, mald");
        addAch("acidizer", "Acidic", "oof ow my skin");
        addAch("assembly", "The Factory Grows", "Wait, it's already 1 am?");
        addAch("bismuth", "Bismuth", "Remember when people complained about this for a month? I do.");
        addAch("blastFurnace", "Coal and Iron", "They salvaged a sunken dreadnought for Explorer 1.");
        addAch("bossCreeper", "Bomb On Four Legs", "'There is nuclear creepers? Those are a thing?!'");
        addAch("bossMaskman", "6 Months of mandatory service and all I got was a lousy t-shirt", "Bonk the big boy.");
        addAch("bossMeltdown", "3.6 Roentgen", "More terrible than great, but I take what I can get.");
        addAch("bossUFO", "Ayy Lmao", "Yo, what do we have here? A huge spacecraft pulling up to the blockship?");
        addAch("bossWorm", "Disassembling Balls-O-Tron", "Just a small metal worm.");
        addAch("breeding", "Ironic", "Thank you, god bless you, and god bless the United States of America.");
        addAch("burnerPress", "Under Pressure", "Pressure pushing down on me, on you");
        addAch("c20_5", "Chapter [TWENTY POINT FIVE]", "???");
        addAch("c44", "Chapter 44", "Galvanized! I mean, zinc!");
        addAch("centrifuge", "Centrifugal Force", "centrifugal force is real don't @ me");
        addAch("chemplant", "The Factory Grows Pt. 2", "Now you're thinking with chemicals!");
        addAch("chicagoPile", "The navigator landed in the New World", "\"How were the natives?\" / \"Very friendly.\"");
        addAch("concrete", "Old Reliable", "A Bolshevik's favorite.");
        addAch("desh", "Le Verrier", "\"Come on then, you lot. Places to go!\"");
        addAch("digammaFeel", "SEWAGE-INFUSED GARBAGE WORLD", "My eyes are bleeding");
        addAch("digammaKauaiMoho", "SING, SING ME THE SONG OF THE KAUAI MOHO", "Everything is awful, here's some hot choccy.");
        addAch("digammaKnow", "THE TERROR OF KNOWING", "what this world is about.");
        addAch("digammaSee", "ENTER THE ABYSS", "It's a bit dark, bring a flashlight.");
        addAch("digammaUpOnTop", "ADMIRE ME, ADMIRE MY HOME", "Admire my son, he's my clone.");
        addAch("fiend", "Delinquent", "Be mean.");
        addAch("fiend2", "Delinquent 2: Delinquent Harder", "Be meaner.");
        addAch("freytag", "Freytag", "Herold's life guards");
        addAch("fusion", "Fusion", "A dance of deuterons, tritons, and energy.");
        addAch("gasCent", "The Zippe Style", "Unenriched Uranium hates him!");
        addAch("gofish", "Go Fish", "Nautical Crucifixion");
        addAch("hidden", "Hidden Catalog", "Kill a tainted creeper with a falling boxcar.");
        addAch("horizonsBonus", "Slam Dunk Diarrhea", "honest to god what the hell is wrong with you");
        addAch("horizonsEnd", "The Horizons", "Send Tom home.");
        addAch("horizonsStart", "Apogee", "Send a lad to the moon.");
        addAch("impossible", "Literally impossible", "You can't get this achievement.");
        addAch("inferno", "Operation Cannibal", "\"Turn it to ashes! That's not enough! Drop more bombs!\"");
        addAch("manhattan", "The Manhattan Project", "8:15; August 6th, 1945");
        addAch("meltdown", "Rapid Unscheduled Disassembly", "You got this far, how could you mess this up?");
        addAch("no9", "Old Number Nine", "\"I hope the lead I'm mining will be used in your paint\"");
        addAch("omega12", "Omega-12 Particle Accelerator", "Solve the problem of continued life on this wretched planet.");
        addAch("polymer", "Teflon", "Delicious, delicious microplastics.");
        addAch("potato", "Rogue AI", "You stabbed me! What is WRONG with yo-WOOOAAH");
        addAch("radDeath", "Ouch, Radiation!", "Marie Curie invented the theory of radioactivity, the treatment of radioactivity, and dying of radioactivity.");
        addAch("radPoison", "Yay, Radiation!", "Suffer the effects of radiation poisoning.");
        addAch("radium", "MISTER INCREDIBLE", "YOU'RE FIRED FOR POURING RADIUM IN MY COFFEE!");
        addAch("redBalloons", "99 Red Balloons", "\"This is what we've waited for. This is it, boys, this is war.\"");
        addAch("redroom", "The Other Side", "?");
        addAch("sacrifice", "Sororicide", "Face the fire and live.");
        addAch("schrab", "Island of Stability", "Regardless, I wouldn't look at it for too long.");
        addAch("selenium", "XVIII The Moon", "Yeah.");
        addAch("slimeball", "I should dip my balls in sulfuric acid.", "");
        addAch("someWounds", "Some Wounds Never Heal", "Get ready");
        addAch("soyuz", "Baked Potato", "Become crunchy.");
        addAch("space", "The Final Front-ah forget it", "Fail in every way possible and waste funds worth 90 million dollars.");
        addAch("stratum", "Stratum", "Hit the brakes, Mitts.");
        addAch("sulfuric", "I should not have dipped my balls in sulfuric acid.", "");
        addAch("tantalum", "\"Tantalium\"", "An elusive, yet ever-needed element.");
        addAch("tasteofblood", "The Taste of Blood", "is not part of any testing protocol.");
        addAch("technetium", "Big Man, Pig Man", "It's medicinal, it's medicinal!");
        addAch("watz", "The Power of Element-126", "Fólkvangr fields possibly included.");
        addAch("watzBoom", "Disgusting", "Drain your septic tank next time.");
        addAch("witchtaunter", "Witch Taunter", "Those wacky creatures got nothing on you!");

        // ---- hbm.achievement.* duplicates actually referenced by CE's real advancement JSON set ----
        this.add("hbm.achievement.fiend", "Delinquent");
        this.add("hbm.achievement.fiend.desc", "Be mean.");
        this.add("hbm.achievement.fiend2", "Delinquent 2: Delinquent Harder");
        this.add("hbm.achievement.fiend2.desc", "Be meaner.");
        this.add("hbm.achievement.omega12", "Omega-12 Particle Accelerator");
        this.add("hbm.achievement.omega12.desc", "Solve the problem of continued life on this wretched planet.");
        this.add("hbm.achievement.sacrifice", "Sororicide");
        this.add("hbm.achievement.sacrifice.desc", "Face the fire and live.");
        this.add("hbm.achievement.someWounds", "Some Wounds Never Heal");
        this.add("hbm.achievement.someWounds.desc", "Get ready");
        this.add("hbm.achievement.progress_dfc", "Turning on a Sun");
        this.add("hbm.achievement.progress_dfc.desc", "A stellar campfire");

        // ---- hbm.advancement.root (title/desc of hbm:root.json, the parent of all 65 real advancements) ----
        this.add("hbm.advancement.root", "NTM Community Edition");
        this.add("hbm.advancement.root.desc", "And so the nuclear adventure starts");
    }

    private void addAch(String id, String title, String desc) {
        this.add("achievement." + id, title);
        this.add("achievement." + id + ".desc", desc);
    }

    // ==================== boss/mob entity names (zero fallback exists for EntityType - highest risk) ====================

    /**
     * CE's real {@code entity.<id>.name} text for this port's boss/mob roster (registry ids confirmed
     * exact matches against CE's own unlocalized entity ids - see {@code CreeperVariantEntityTypes},
     * {@code MaskmanEntityTypes}, {@code WormEntityTypes}, {@code Phase4BossEntityTypes2},
     * {@code RadBeastEntityTypes}). Unlike items/blocks, {@code EntityType} has no generative fallback
     * anywhere in this port, so before this method a raw {@code entity.hbm.<id>} key would render
     * literally in the death message / entity nameplate / spawn-egg tooltip the moment any of these
     * bosses spawned - the research report's #1 flagged live risk.
     */
    private void addEntityNames() {
        this.add("entity.hbm.entity_balls_o_tron", "Balls-O-Tron Prime");
        this.add("entity.hbm.entity_balls_o_tron_seg", "Balls-O-Tron Segment");
        this.add("entity.hbm.entity_cyber_crab", "Cyber Crab");
        this.add("entity.hbm.entity_elder_one", "Quackos The Elder One");
        this.add("entity.hbm.entity_fucc_a_ducc", "Duck");
        this.add("entity.hbm.entity_hunter_chopper", "Hunter Chopper");
        this.add("entity.hbm.entity_mask_man", "Mask Man");
        this.add("entity.hbm.entity_mob_gold_creeper", "Golden Creeper");
        this.add("entity.hbm.entity_mob_phosgene_creeper", "Phosgene Creeper");
        this.add("entity.hbm.entity_mob_volatile_creeper", "Volatile Creeper");
        this.add("entity.hbm.entity_nuclear_creeper", "Nuclear Creeper");
        this.add("entity.hbm.entity_ntm_radiation_blaze", "Meltdown Elemental");
        this.add("entity.hbm.entity_ntm_ufo", "Martian Invasion Ship");
        this.add("entity.hbm.entity_taint_crab", "Taint Crab");
        this.add("entity.hbm.entity_tainted_creeper", "Tainted Creeper");
        this.add("entity.hbm.entity_tesla_crab", "Tesla Crab");
    }

    // ==================== potion/status-effect names (zero fallback exists for MobEffect) ====================

    /**
     * CE's real {@code potion.hbm_<id>} status-effect names (CE's 1.12.2 single-{@code Potion}-class
     * dispatch, ported here to this port's 12 real {@link com.hbm.potion.HbmPotionEffects} registry
     * ids - see that class's own javadoc). Like entities, {@code MobEffect} has no generative fallback
     * in this port - the research report's #2 flagged live risk.
     */
    private void addEffectNames() {
        this.add("effect.hbm.taint", "Tainted");
        this.add("effect.hbm.radiation", "Contaminated");
        this.add("effect.hbm.bang", "! ! !");
        this.add("effect.hbm.mutation", "Tainted Heart");
        this.add("effect.hbm.radx", "Rad-X");
        this.add("effect.hbm.lead", "Lead Poisoning");
        this.add("effect.hbm.radaway", "Radaway");
        this.add("effect.hbm.telekinesis", "! ! !");
        this.add("effect.hbm.phosphorus", "Phosphorus Burns");
        this.add("effect.hbm.stability", "Stability");
        this.add("effect.hbm.potionsickness", "Potion Sickness");
        this.add("effect.hbm.death", "Astolfization");
    }

    // ==================== creative tab titles (flagged since Phase 0, docs/phase0/DIGEST.md:405) ====================

    /** CE's real {@code itemGroup.tabX} text (en_us.lang:6230-6240), re-keyed to this port's real
     *  {@code itemGroup.hbm.<path>} key shape ({@code ModCreativeTabs.translationKey}). {@code tabTest}
     *  is CE-dev-only and not ported (this port has no equivalent tab). */
    private void addCreativeTabTitles() {
        this.add("itemGroup.hbm.tab_parts", "NTM Resources and Parts");
        this.add("itemGroup.hbm.tab_control", "NTM Machine Items and Fuel");
        this.add("itemGroup.hbm.tab_template", "NTM Templates");
        this.add("itemGroup.hbm.tab_resource", "NTM Resources");
        this.add("itemGroup.hbm.tab_blocks", "NTM Ores and Blocks");
        this.add("itemGroup.hbm.tab_machine", "NTM Machines");
        this.add("itemGroup.hbm.tab_nuke", "NTM Bombs");
        this.add("itemGroup.hbm.tab_missile", "NTM Missiles and Satellites");
        this.add("itemGroup.hbm.tab_weapon", "NTM Weapons and Turrets");
        this.add("itemGroup.hbm.tab_consumable", "NTM Consumables and Gear");
    }

    // ==================== GUI/container titles ====================

    /**
     * All 65 {@code container.*} keys already referenced via {@code Component.translatable(...)}
     * across this port's existing {@code inventory/gui} {@code Screen} classes (grepped whole-tree).
     * 54 match CE's real {@code container.*} key verbatim (CE en_us.lang:906-1108). The other 11 are
     * documented key-drift - CE's GUI for that machine either used a different container key or (for
     * 3 of them: combustionEngine/pwrController/watz) drew no distinct title text at all in CE's real
     * {@code Gui*.java} (only the shared {@code container.inventory} label) - resolved below via CE's
     * real {@code tile.<id>.name} for that same machine instead, each commented with its CE source.
     * Fixing the Java call sites to use CE's real key strings is out of this task's scope (those
     * {@code Screen} classes belong to other Phase 5 tasks) - flagged in this task's structured notes.
     */
    private void addContainerTitles() {
        this.add("container.battery", "Energy Storage");
        this.add("container.centrifuge", "Centrifuge");
        this.add("container.crystallizer", "Ore Acidizer");
        this.add("container.cyclotron", "Cyclotron");
        this.add("container.fluidtank", "Tank");
        this.add("container.frackingTower", "Hydraulic Fracking Tower");
        this.add("container.gasCentrifuge", "Gas Centrifuge");
        this.add("container.launchPad", "Launch Pad");
        this.add("container.launchPadRusted", "Launch Pad");
        this.add("container.launchpadSoyuz", "Soyuz Launch Pad");
        this.add("container.machineChemicalPlant", "Chemical Plant");
        this.add("container.machineDiesel", "Diesel Generator");
        this.add("container.machineElectrolyser", "Electrolysis Machine");
        this.add("container.machineICF", "ICF");
        this.add("container.machineICFPress", "ICF Fuel Pellet Maker");
        this.add("container.machineLargeTurbine", "Industrial Steam Turbine");
        this.add("container.machineMixer", "Industrial Mixer");
        this.add("container.machineRefinery", "Oil Refinery");
        this.add("container.machineSILEX", "SILEX");
        this.add("container.machineShredder", "Shredder");
        this.add("container.machineTurbine", "Steam Turbine");
        this.add("container.nukeBoy", "Little Boy");
        this.add("container.nukeCustom", "Custom Nuke");
        this.add("container.nukeFleija", "F.L.E.I.J.A.");
        this.add("container.nukeFstbmb", "Balefire Bomb");
        this.add("container.nukeGadget", "The Gadget");
        this.add("container.nukeMan", "Fat Man");
        this.add("container.nukeMike", "Ivy Mike");
        this.add("container.nukeN2", "N² Mine");
        this.add("container.nukePrototype", "The Prototype");
        this.add("container.nukeTsar", "Tsar Bomba");
        this.add("container.oilWell", "Oil Derrick");
        this.add("container.pumpjack", "Pumpjack");
        this.add("container.rbmkAutoloader", "RBMK Autoloader");
        this.add("container.rbmkBoiler", "RBMK Steam Channel");
        this.add("container.rbmkControl", "RBMK Control Rods");
        this.add("container.rbmkControlAuto", "RBMK Automatic Control Rods");
        this.add("container.rbmkHeater", "RBMK Fluid Heater");
        this.add("container.rbmkOutgasser", "RBMK Irradiation Channel");
        this.add("container.rbmkRod", "RBMK Fuel Rod");
        this.add("container.rbmkStorage", "RBMK Storage Column");
        this.add("container.reactorBreeding", "Breeding Reactor");
        this.add("container.rtg", "RT Generator");
        this.add("container.trainTram", "Electric Flat Bed Tram");
        this.add("container.turbinegas", "Combined Cycle Gas Turbine");
        this.add("container.turretChekhov", "Chekhov's Gun");
        this.add("container.turretFriendly", "Mister Friendly");
        this.add("container.turretFritz", "Fritz");
        this.add("container.turretHoward", "Howard");
        this.add("container.turretJeremy", "Jeremy");
        this.add("container.turretMaxwell", "Maxwell");
        this.add("container.turretRichard", "Richard");
        this.add("container.turretSentry", "Brown");
        this.add("container.turretTauon", "Tauon");

        // ---- key-drift: resolved via a per-machine CE grep, not a verbatim container.* match ----
        this.add("container.assemblyMachine", "Assembly Machine (Legacy)"); // CE GUIMachineAssemblyMachine draws tile.machine_assembler.name directly, not a container.* key
        this.add("container.combustionEngine", "Industrial Combustion Engine"); // CE GUICombustionEngine draws no title at all; CE tile.machine_combustion_engine.name
        this.add("container.industrialTurbine", "Industrial Steam Turbine"); // CE tile.machine_industrial_turbine.name (no dedicated CE GUI class - port-added GUI)
        this.add("container.pwrController", "PWR Controller"); // CE GUIPWR draws no title at all; CE tile.pwr_controller.name
        this.add("container.rbmkConsole", "RBMK Console"); // CE tile.rbmk_console.name (CE's GUIRBMKConsole draws no static title text either)
        this.add("container.rbmkCooler", "RBMK Cooler"); // CE tile.rbmk_cooler.name (no dedicated CE GUI class - port-added GUI)
        this.add("container.solarBoiler", "Solar Tower Boiler"); // CE tile.machine_solar_boiler.name (no dedicated CE GUI class - port-added GUI)
        this.add("container.solarMirror", "Heliostat Mirror"); // CE tile.solar_mirror.name (no dedicated CE GUI class - port-added GUI)
        this.add("container.steamEngine", "Steam Engine"); // CE tile.machine_steam_engine.name (no dedicated CE GUI class - port-added GUI)
        this.add("container.trainTramTrailer", "Tram Trailer"); // no CE source found at all (CE grep turned up nothing for a trailer variant) - hand-authored, consistent with container.trainTram="Electric Flat Bed Tram"
        this.add("container.watz", "Watz Powerplant"); // CE GUIWatz draws no title at all; CE tile.watz.name (distinct from achievement.watz, which is unrelated flavor text)
    }

    // ==================== death messages ====================

    private void addDeathMessages() {
        // CE en_us.lang, ported verbatim for every ModDamageTypes msgId with a real CE source string.
        addDeath("acid", "%1$s fell into acid");
        addDeath("acidPlayer", "%1$s was dissolved by %2$s");
        addDeath("ams", "%1$s was bathed in deadly particles that have yet to be named by human science");
        addDeath("amsCore", "%1$s was vaporized in the fire of a singularity");
        addDeath("asbestos", "%1$s is now entitled to financial compensation");
        addDeath("bang", "%1$s was blasted into bite-sized pieces");
        addDeath("blackhole", "%1$s was spaghettified");
        addDeath("blacklung", "%1$s died from black lung disease");
        addDeath("blast", "%1$s was blown away by an explosion shockwave");
        addDeath("blender", "%1$s was chopped in small, bite-sized pieces");
        addDeath("boat", "%1$s was hit by a boat");
        addDeath("boil", "%1$s was boiled alive by %2$s");
        addDeath("boxcar", "%1$s was smushed by a falling boxcar. Oh well");
        addDeath("broadcast", "%1$s got their brain melted");
        addDeath("building", "%1$s was hit by a falling building");
        addDeath("chopperBullet", "%1$s was rekt by %2$s");
        addDeath("cloud", "%1$s melted like a popsicle in the sun");
        addDeath("cmb", "%1$s was fizzeled by %2$s");
        addDeath("crucible", "%1$s was bisected by a blade of pure Argent energy");
        addDeath("digamma", "%1$s stepped into the abyss");
        addDeath("electricity", "%1$s was electrocuted");
        addDeath("electrified", "%1$s was electrified by %2$s");
        addDeath("euthanized", "%1$s was euthanized by %2$s");
        addDeath("euthanizedSelf", "%1$s euthanized himself, what a dork");
        addDeath("euthanizedSelf2", "%1$s wins the Darwin Award");
        addDeath("exhaust", "%1$s was turned into shish kebab by a starting rocket");
        addDeath("flamethrower", "%1$s was cremated by %2$s");
        addDeath("gunGib", "%1$s was blasted into pieces by %2$s");
        addDeath("ice", "%1$s was turned into a popsicle by %2$s");
        addDeath("laser", "%1$s was turned into ash by %2$s");
        addDeath("lead", "%1$s died from lead poisoning");
        addDeath("lunar", "%1$s forgot to charge their vital organs");
        addDeath("meteorite", "%1$s was hit by a falling rock from outer space");
        addDeath("microwave", "%1$s was exploded by microwave radiation");
        addDeath("mku", "%1$s died from unknown causes");
        addDeath("monoxide", "%1$s forgot to change the batteries in their carbon monoxide detector");
        addDeath("mudPoisoning", "%1$s died in poisonous mud");
        addDeath("nuclearBlast", "%1$s was blown away by a nuclear explosion");
        addDeath("overdose", "%1$s overdosed and asphyxiated");
        addDeath("pc", "%1$s was reduced to a puddle in the pink cloud");
        addDeath("plasma", "%1$s was immolated by %2$s");
        addDeath("radiation", "%1$s died from radiation poisoning");
        addDeath("revolverBullet", "%1$s was shot in the head by %2$s");
        addDeath("rubble", "%1$s was squashed by debris");
        addDeath("shrapnel", "%1$s was ragged by a shrapnel");
        addDeath("slicer", "%1$s was cut in half");
        addDeath("spikes", "%1$s got impaled");
        addDeath("subAtomic1", "%1$s's atoms have been destroyed by %2$s");
        addDeath("subAtomic2", "%1$s was QPU-misaligned because %2$s tampered with their de facto speed");
        addDeath("subAtomic3", "%1$s's divergence dropped below 1 percent because of %2$s");
        addDeath("subAtomic4", "%1$s was divided by zero by %2$s");
        addDeath("subAtomic5", "%1$s was nullified by %2$s");
        addDeath("suicide", "%1$s blew their head off");
        addDeath("taint", "%1$s died from flux tumors");
        addDeath("tau", "%1$s was riddeled by %2$s using negatively charged tauons");
        addDeath("tauBlast", "%1$s charged the XVL1456 for too long and was blown into pieces");

        // Sedna generic damage classes: no CE source (DamageSourceSednaNoAttacker/WithAttacker had no
        // fixed id set), text confirmed from the Neo Edition reference's NtmLanguageProvider.
        addDeath("sednaPhysical", "%1$s was shot");
        addDeathPlayer("sednaPhysical", "%1$s was shot by %2$s");
        addDeath("sednaFire", "%1$s was incinerated");
        addDeathPlayer("sednaFire", "%1$s was incinerated by %2$s");
        addDeath("sednaExplosion", "%1$s was blown up.");
        addDeathPlayer("sednaExplosion", "%1$s was blown up by %2$s.");
        addDeath("sednaElectric", "%1$s was fried");
        addDeathPlayer("sednaElectric", "%1$s was fried by %2$s");
        addDeath("sednaLaser", "%1$s was pulverized");
        addDeathPlayer("sednaLaser", "%1$s was pulverized by %2$s");
        addDeath("sednaMicrowave", "%1$s was microwaved");
        addDeathPlayer("sednaMicrowave", "%1$s was microwaved by %2$s");
        addDeath("sednaSubatomic", "%1$s was atomized");
        addDeathPlayer("sednaSubatomic", "%1$s was atomized by %2$s");
        addDeath("sednaOther", "%1$s was killed");
        addDeathPlayer("sednaOther", "%1$s was killed by %2$s.");

        // ---- the 7 real CE msgIds the previous pass missed (research report finding 6) ----
        addDeath("cheater", "%1$s's intestines turned into oats. (???)"); // CE en_us.lang:1170
        this.add("death.attack.flamethrower.item", "%1$s was cremated by %2$s using %3$s"); // CE en_us.lang:1182 (3-arg weapon-named variant)
        addDeath("gluon", "%1$s was deatomized by a stream of concentrated gluons"); // CE en_us.lang:1184
        this.add("death.attack.laser.item", "%1$s was turned into ash by %2$s using %3$s"); // CE en_us.lang:1187
        this.add("death.attack.revolverBullet.item", "%1$s was shot in the head by %2$s using %3$s"); // CE en_us.lang:1201
        addDeath("subAtomic", "%1$s's atoms have been destroyed by %2$s"); // CE en_us.lang:1212 (plain fallback, distinct from subAtomic1-5)
        addDeath("teleporter", "%1$s was teleported into nothingness"); // CE en_us.lang:1217
    }

    private void addDeath(String msgId, String message) {
        this.add("death.attack." + msgId, message);
    }

    private void addDeathPlayer(String msgId, String message) {
        this.add("death.attack." + msgId + ".player", message);
    }

    // ==================== misc keys already referenced from Java (chat/trait/desc/UI/satellite/etc) ====================

    /**
     * Every other lang key this port's Java already calls {@code Component.translatable(...)} on with
     * a string-literal key (grepped whole-tree), ported from CE's real text.
     *
     * <p><b>{@code desc.gun.*}</b> (the Sedna gun quality/damage tooltip lines, referenced from
     * {@code ItemGunBaseNT.java}): the research report flagged these as having "no CE lang-file
     * source" and recommended against hand-authoring them - correct that no lang key exists, but CE's
     * real {@code addInformation(...)} in its own {@code ItemGunBaseNT.java} hardcodes this exact
     * English text directly as Java string literals/concatenation (not through a lang key at all).
     * Transcribed verbatim from that real CE source below, which is a genuine CE-text port, not an
     * invention - see each line's inline comment for the exact CE concatenation it replaces.
     *
     * <p><b>{@code desc.hand_drill1}</b> (also referenced from {@code ItemTooling.java}, alongside
     * {@code desc.screwdriver1}): confirmed CE's real {@code ItemTooling.addInformation(...)} has
     * <i>no</i> hand-drill tooltip line at all (only the screwdriver one) - genuinely zero CE source,
     * left unset here per this project's "don't invent, flag the blocker" convention rather than
     * guessed. Whoever owns the tool-tooltip UX should write real English for it.
     *
     * <p><b>{@code satellite.<name>.name}</b>: this port's 13 {@code saveddata/satellites/Satellite*}
     * classes each reference a distinct key. CE's real satellite-item names live under
     * {@code item.satellite.<ceId>.name} with different id strings (e.g. this port's
     * {@code satellite.rayscan.name} vs. CE's real {@code item.satellite.ray_scan.name}) - resolved
     * below via a per-class CE-behavior match (confirmed by reading each {@code Satellite*.java}
     * class's actual command/behavior, not just its name), each commented with the CE id it matches.
     * {@code satellite.horizons.name} has no matching CE satellite-item entry at all (CE only ever
     * calls it "Horizons" in one chat message, {@code chat.gerald.detonated}) - reused CE's own
     * {@code achievement.horizonsEnd} title text ("The Horizons") as the closest real CE string rather
     * than inventing new text.
     */
    private void addMiscKeys() {
        // ---- chat.* ----
        this.add("chat.addpldata", "Added player data!");
        this.add("chat.crate.needcrowbar", "I'll need a crate opening device to get the loot, smashing the whole thing won't work...");
        this.add("chat.gerald.detonated", "§eHorizons has been activated.");
        this.add("chat.posset", "Position set");
        this.add("chat.wiring.cleared", "§6[Cable] §ePylon position cleared");
        this.add("chat.wiring.connected", "§6[Cable] §eCables Connected");
        this.add("chat.wiring.measure", "§6[Cable] §ePylon distance: %sm");
        this.add("chat.wiring.noself", "§6[Cable] §eIt cant connect it to itself");
        this.add("chat.wiring.notcompatible", "§6[Cable] §eCables have different types");
        this.add("chat.wiring.start", "§6[Cable] §eStart set to %s, %s, %s");
        this.add("chat.wiring.tofar", "§6[Cable] §eDistance is too long %s/%sm");

        // ---- desc.wiring.* / desc.tooltip.hold / desc.permb / desc.screwdriver1 ----
        this.add("desc.permb", "per mB");
        this.add("desc.screwdriver1", "Could be used instead of a fuse...");
        this.add("desc.tooltip.hold", "§8§oHold <§e§o%s§8§o> to display more info");
        this.add("desc.wiring.1", "§eRight-click poles to connect");
        this.add("desc.wiring.2", "§eRight-click any block to show distance");
        this.add("desc.wiring.3", "§eShift-Right-click to set start pole");
        this.add("desc.wiring.4", "§eShift-Right-click any block to clear start pole");
        this.add("desc.wiring.start", "§6Start Pole: %s, %s, %s");
        // desc.hand_drill1: genuinely no CE source - see this method's javadoc, deliberately left unset.

        // ---- desc.gun.* (real CE text, transcribed from CE's hardcoded-in-Java tooltip strings) ----
        this.add("desc.gun.base_damage", "Base Damage: %s"); // CE: "Base Damage: " + FORMAT_DMG.format(dmg)
        this.add("desc.gun.damage_with_ammo", "Damage with current ammo: %s"); // CE: "Damage with current ammo: " + ...
        this.add("desc.gun.condition", "Condition: %s%%"); // CE: "Condition: " + dura + "%"
        this.add("desc.gun.quality.a_side", "Standard Arsenal"); // CE: case A_SIDE -> "Standard Arsenal"
        this.add("desc.gun.quality.b_side", "B-Side"); // CE: case B_SIDE -> "B-Side"
        this.add("desc.gun.quality.legendary", "Legendary Weapon"); // CE: case LEGENDARY -> "Legendary Weapon"
        this.add("desc.gun.quality.special", "Special Weapon"); // CE: case SPECIAL -> "Special Weapon"
        this.add("desc.gun.quality.utility", "Utility"); // CE: case UTILITY -> "Utility"
        this.add("desc.gun.quality.secret", "SECRET"); // CE: case SECRET -> "SECRET"
        this.add("desc.gun.quality.debug", "DEBUG"); // CE: case DEBUG -> "DEBUG"

        // ---- radiation/lung diagnostic tool UI strings ----
        this.add("digamma.playerDigamma", "Digamma exposure:");
        this.add("digamma.playerHealth", "Digamma influence:");
        this.add("digamma.playerRes", "Digamma resistance:");
        this.add("digamma.title", "DIGAMMA DIAGNOSTIC");
        this.add("dosimeter.title", "DOSIMETER");
        this.add("geiger.chunkRad", "Current chunk radiation:");
        this.add("geiger.envRad", "Total environmental radiation:");
        this.add("geiger.playerRad", "Player contamination:");
        this.add("geiger.playerRes", "Player resistance:");
        this.add("geiger.recievedRad", "Total recieved radiation:");
        this.add("geiger.title", "GEIGER COUNTER");
        this.add("geiger.title.dosimeter", "DOSIMETER");
        this.add("lung_scanner.player_asbestos_health", "Lung Health [Asbestos]:");
        this.add("lung_scanner.player_coal_health", "Lung Health [Coal]:");
        this.add("lung_scanner.player_mku", "MKU Test:");
        this.add("lung_scanner.player_mku_duration", "Death in:");
        this.add("lung_scanner.player_total_health", "Lung Health Total:");
        this.add("lung_scanner.title", "Lung Diagnostic");
        this.add("info.asbestos", "My lungs are burning.");
        this.add("info.coaldust", "It's hard to breathe here.");

        // ---- misc GUI/JEI/tile keys ----
        this.add("gui.turretMobFilter", "Turret Mob Filter"); // key-drift: CE's real key is item.turret_mob_filter.name (en_us.lang:6005), not a distinct GUI-title key; port's Java call site not editable from this task, see notes
        this.add("jei.hbm.rbmk_recycling", "RBMK Recycling"); // no distinct CE key (CE's generic JEI category is just jei.recycling="Recycling"); hand-authored, specific to this port's separate RBMK JEI category
        this.add("tile.nospawn", "Mobs cannot spawn on this block!");
        this.add("tile.block_euphemium_cluster.desc", "Balefire nukes have created small amounts of euphemium inside this block");
        this.add("tile.block_schrabidium_cluster.desc", "Balefire nukes create small amounts of euphemium inside this block");
        this.add("book_lore.author", "By %s");

        // ---- standalone item.hbm.* / item.<x> keys referenced directly by Java (not via getDescriptionId()) ----
        this.add("item.hbm.cell_empty", "Empty Cell");
        this.add("item.hbm.cell_full", "%s Cell"); // no CE lang source (CE built this string by concatenation, not a lang key); hand-authored to match CE's real "Empty Cell" naming convention
        this.add("item.hbm.ingot_nikonium", "Nikonium Ingot");
        this.add("item.bedrock_ore.type.actinide.name", "Actinide");
        this.add("item.bedrock_ore.type.crystal.name", "Crystalline");
        this.add("item.bedrock_ore.type.hazard.name", "Hazardous Waste");
        this.add("item.bedrock_ore.type.heavy.name", "Heavy Metal");
        this.add("item.bedrock_ore.type.light.name", "Light Metal");
        this.add("item.bedrock_ore.type.nonmetal.name", "Non-Metal");
        this.add("item.bedrock_ore.type.rare.name", "Rare Earth");
        this.add("item.bedrock_ore.type.schrabidic.name", "Schrabidic");
        this.add("item.record.glass.desc", "? ? ?");
        this.add("item.record.lc.desc", "Valve - Diabolic Adrenaline Guitar/Lambda Core");
        this.add("item.record.ss.desc", "Valve - Sector Sweep");
        this.add("item.record.vc.desc", "Valve - Vortal Combat");

        // ---- satellite UI strings + per-satellite display names ----
        this.add("satellite.cooldown", "Cooldown: %s");
        this.add("satellite.data", "DATA AVAILABLE");
        this.add("satellite.pending", "Measurements pending: %s");
        this.add("satellite.ready", "READY");
        this.add("satellite.sensors", "Sensor Relays installed: %s");
        this.add("satellite.spent", "SPENT");
        this.add("satellite.detector.name", "Wideband Radio Emission Detector Satellite"); // CE item.satellite.detector.name (exact id match)
        this.add("satellite.horizons.name", "The Horizons"); // no matching CE satellite-item entry; reused CE's real achievement.horizonsEnd title text
        this.add("satellite.laser.name", "Orbital Death Ray"); // matches CE item.satellite.death_ray.name by behavior (SatelliteLaser has a charge-timed deathBlast() attack, CE's Orbital Death Ray)
        this.add("satellite.lunar_miner.name", "Lunar Mining Ship"); // CE item.satellite.miner_lunar.name
        this.add("satellite.mapper.name", "Spy Satellite"); // CE item.satellite.spy.name - confirmed by SatelliteMapper.getType()'s own joke string "NOT_A_SPY_SATELLITE_:)" and its CMD_SPOT_PLAYER command
        this.add("satellite.miner.name", "Asteroid Mining Ship"); // CE item.satellite.miner_astro.name (the generic/astro miner, distinct from the lunar-specific satellite above)
        this.add("satellite.precision_laser.name", "Orbital Precision Laser"); // CE item.satellite.precision_laser.name (exact id match)
        this.add("satellite.radar.name", "Radar Satellite"); // CE item.satellite.radar.name (exact id match)
        this.add("satellite.rayscan.name", "Narrowband Emission Scanning Satellite"); // CE item.satellite.ray_scan.name
        this.add("satellite.relay.name", "RoR Relay Satellite"); // CE item.satellite.relay.name (exact id match)
        this.add("satellite.resonator.name", "Xenium Resonator Satellite"); // CE item.satellite.xenium_resonator.name
        this.add("satellite.scanner.name", "Depth Scanning Satellite"); // CE item.satellite.scanner.name (exact id match)
        this.add("satellite.science.name", "Space Laboratory"); // CE item.satellite.science.name (exact id match)

        // ---- material/hazard trait tooltip lines (CE's full trait.* corpus, 148 real lines, en_us.lang:8321-8468) ----
        addRaw(
            "trait.antimatter=Annihilating",
            "trait.antimatterliq=Antimatter",
            "trait.asbestos=Asbestos",
            "trait.balefirebomb=Balefire Bomb",
            "trait.blastres=Blast Resistance: %s",
            "trait.blinding=Blinding",
            "trait.boilable.desc=§bRequires §3%sTU §bper bucket.",
            "trait.boilable=Heatable",
            "trait.breeding=Worth %s operations in breeding reactor",
            "trait.chefficiency=§e[%s] §bEfficiency: %s",
            "trait.cleanroom.desc=Dropped contaminating items do not disappear",
            "trait.cleanroom=Clean",
            "trait.coal=Coal Dust",
            "trait.combustable.avi=Aviation",
            "trait.combustable.desc2=§6Fuel grade: §c%s",
            "trait.combustable.desc=§6Provides §c%sHE §6per bucket.",
            "trait.combustable.gas=Gaseous",
            "trait.combustable.high=High",
            "trait.combustable.low=Low",
            "trait.combustable.medium=Medium",
            "trait.combustable=Combustible",
            "trait.contaminating.radius=Radius: %sm",
            "trait.contaminating=Contaminating Drop",
            "trait.coolable.desc=§cProvides §4%sTU §cper bucket.",
            "trait.coolable=Coolable",
            "trait.corrosiveIron=Strongly Corrosive",
            "trait.corrosivePlastic=Corrosive",
            "trait.cryogenic=Cryogenic / Cold",
            "trait.ctype.heatexch=Coolable",
            "trait.ctype.turbine=Turbine Steam",
            "trait.delicious=Delicious",
            "trait.destroybyexplosion=Can only be destroyed by explosions",
            "trait.dfcFuel.desc=§dPower Output §5%s%%",
            "trait.dfcFuel=DFC Fuel",
            "trait.digamma=Digamma Radiation",
            "trait.drop=Dangerous Drop",
            "trait.explosive=Flammable / Explosive",
            "trait.extremebomb=Extreme Bomb",
            "trait.fallout=Fallout",
            "trait.flammable.desc=§eProvides §6%sTU §eper bucket",
            "trait.flammable=Flammable",
            "trait.fuelefficiency.desc=§e-%s: §c%s%%",
            "trait.fuelefficiency=§eFuel efficiency:",
            "trait.fuelgrade.aero=Aviation",
            "trait.fuelgrade.gas=Gaseous",
            "trait.fuelgrade.high=High",
            "trait.fuelgrade.low=Low",
            "trait.fuelgrade.medium=Medium",
            "trait.furnace=Worth %s operations in nuclear furnace",
            "trait.gaseous=Gaseous",
            "trait.gaseousroom=Gaseous at Room Temperature",
            "trait.haztank=Requires hazardous material tank to hold",
            "trait.heat=Provides %s HEAT",
            "trait.hlParticle=Particle Half-Life: %s",
            "trait.hlPlayer=Player Half-Life: %s",
            "trait.hot=Pyrophoric / Hot",
            "trait.hotfluid=Hot",
            "trait.htype.boiler=Boilable",
            "trait.htype.heatexch=Heatable",
            "trait.htype.icf=ICF Coolant",
            "trait.htype.pwr=PWR Coolant",
            "trait.hydro=Hydroreactive",
            "trait.legendaryweap=§d§l[LEGENDARY WEAPON]",
            "trait.liquid=Liquid",
            "trait.mkuinfected=Infected",
            "trait.modularbomb=Modular Bomb",
            "trait.needhaz=(requires hazmat suit)",
            "trait.nosiphon=Ignored by siphon",
            "trait.nuclearbomb=Nuclear Bomb",
            "trait.nucleargrenade=Nuclear Grenade",
            "trait.pherg=Glyphid Pheromones",
            "trait.pherm=Modified Pheromones",
            "trait.plasma=Plasma",
            "trait.polluburn=§cWhen burned:",
            "trait.polluspill=§aWhen spilled:",
            "trait.polluting=Polluting",
            "trait.ptype.fallout=FALLOUT",
            "trait.ptype.heavymetal=HEAVYMETAL",
            "trait.ptype.poison=POISON",
            "trait.ptype.soot=SOOT",
            "trait.pwrflux.desc=Core flux",
            "trait.pwrflux=PWR Flux Multiplier",
            "trait.radResistance=Radiation resistance: %s",
            "trait.radioactive=Radioactive",
            "trait.radshield=Radiation Shielding",
            "trait.rbmk.coreTemp=Core temp: %s",
            "trait.rbmk.depletion=Depletion: %s",
            "trait.rbmk.diffusion=Diffusion: %s",
            "trait.rbmk.fluxFunc=Flux function: %s",
            "trait.rbmk.funcType=Function type: %s",
            "trait.rbmk.heat=Heat per flux: %s",
            "trait.rbmk.melt=Melting point: %s",
            "trait.rbmk.meltdown=Internal Meltdown: %s",
            "trait.rbmk.neutron.any.x=All non-euclidean shapes",
            "trait.rbmk.neutron.any=All Neutrons",
            "trait.rbmk.neutron.fast.x=Elliptic non-euclidean shapes",
            "trait.rbmk.neutron.fast=Fast Neutrons",
            "trait.rbmk.neutron.slow.x=Hyperbolic non-euclidean shapes",
            "trait.rbmk.neutron.slow=Slow Neutrons",
            "trait.rbmk.skinTemp=Skin temp: %s",
            "trait.rbmk.source=Self-igniting",
            "trait.rbmk.splitsInto=Splits into: %s",
            "trait.rbmk.splitsWith=Splits with: %s",
            "trait.rbmk.xenon=Xenon poison: %s",
            "trait.rbmk.xenonBurn=Xenon burn function: %s",
            "trait.rbmk.xenonGen=Xenon gen function: %s",
            "trait.rbmx.coreTemp=Core entropy: %s",
            "trait.rbmx.depletion=Crustyness: %s",
            "trait.rbmx.diffusion=Flow: %s",
            "trait.rbmx.flux.arch=§6RISKY / NEGATIVE-QUADRATIC",
            "trait.rbmx.flux.euler=§aSAFE / EULER",
            "trait.rbmx.flux.experimental=§fEXPERIMENTAL / SINE SLOPE",
            "trait.rbmx.flux.linear=§cDANGEROUS / LINEAR",
            "trait.rbmx.flux.logten=§eMEDIUM / LOGARITHMIC",
            "trait.rbmx.flux.passive=§2SAFE / PASSIVE",
            "trait.rbmx.flux.quadratic=§4DANGEROUS / QUADRATIC",
            "trait.rbmx.flux.sigmoid=§aSAFE / SIGMOID",
            "trait.rbmx.flux.squrt=§eMEDIUM / SQUARE ROOT",
            "trait.rbmx.fluxFunc=Doom function: %s",
            "trait.rbmx.funcType=Function specification: %s",
            "trait.rbmx.heat=Crust per tick at full power: %s",
            "trait.rbmx.melt=Crush depth: %s",
            "trait.rbmx.meltdown=Reality Breakdown: %s",
            "trait.rbmx.skinTemp=Skin entropy: %s",
            "trait.rbmx.source=Self-combusting",
            "trait.rbmx.splitsInto=Departs to: %s",
            "trait.rbmx.splitsWith=Arrives from: %s",
            "trait.rbmx.xenon=Lead poison: %s",
            "trait.rbmx.xenonBurn=Lead destruction function: %s",
            "trait.rbmx.xenonGen=Lead creation function: %s",
            "trait.reactorrod=Reactor Fuel Rod",
            "trait.rocketGrade.desc=§eProvides §c%s ISP §eper bucket",
            "trait.rocketGrade=Rocket Grade",
            "trait.schrabbomb=Schrabidium Bomb",
            "trait.soliniumbomb=Solinium Bomb",
            "trait.soliniumgrenade=Solinium Grenade",
            "trait.thermalcap=§cThermal capacity: %s TU",
            "trait.thermobomb=Thermonuclear Bomb",
            "trait.thrustPower.desc=§eProvides §c%s N §eof thrust per bucket",
            "trait.thrustPower=Thrust Power",
            "trait.tile.cluster=Drops only when broken by a player",
            "trait.tile.depth=Can only be destroyed by explosions",
            "trait.toxic=Toxicity",
            "trait.toxicfumes=Toxic Fumes",
            "trait.toxin=Toxin",
            "trait.unmineable=Unmineable",
            "trait.unstable=Unstable",
            "trait.viscous=Viscous"
        );
    }

    /** Splits each {@code "key=value"} pair on its first {@code '='} and calls {@link #add(String, String)}. */
    private void addRaw(String... pairs) {
        for (String pair : pairs) {
            int i = pair.indexOf('=');
            this.add(pair.substring(0, i), pair.substring(i + 1));
        }
    }
}
