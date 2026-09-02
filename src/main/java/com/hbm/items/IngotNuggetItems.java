package com.hbm.items;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.food.ItemLemon;
import com.hbm.items.special.ItemCustomLore;
import com.hbm.items.special.ItemFuel;
import com.hbm.items.special.ItemHot;
import com.hbm.items.special.ItemSchraranium;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Ports CE's ~174 hardcoded {@code ingot_}/{@code nugget_} resource item fields
 * (see {@code docs/phase1/moditems_generative.md} section 3). CE hand-wrote every one of these as
 * an individual {@code public static final Item} field rather than through a generation loop - the
 * material/shape sets across ingot/nugget/billet/powder are too irregular (mismatched materials,
 * inconsistent shape coverage, armor plates and fuel materials mixed in) for a loop keyed off
 * {@code Mats.orderedList} to reproduce faithfully. This class keeps that same one-field-per-item
 * shape, just as {@code DeferredItem} registrations instead of CE's construction-as-registration
 * pattern, matching the Neo Edition reference's own {@code NtmItems} ({@code registerNugget(...)}
 * helper confirmed at its line 1037).
 *
 * <p><b>Skipped by design</b> (different Phase 1 areas, per the research report): {@code ingot_raw}
 * ({@code ItemAutogen}-backed, section 1) and {@code ingot_metal} ({@code ItemEnumMulti}-backed,
 * section 2).
 *
 * <p><b>Metadata flattening</b> (post-1.13 has no item metadata, so every CE metadata-multi field
 * becomes N distinct registry entries):
 * <ul>
 *     <li>{@code ingot_steel_dusted} (CE: {@code ItemHotDusted}, damage 0-9 = forging purity level,
 *     confirmed still-live via {@code AnvilRecipes}'s purity-combining smithing recipes and
 *     {@code SmeltingRecipes}) becomes {@code ingot_steel_dusted_0} .. {@code ingot_steel_dusted_9}.</li>
 *     <li>{@code ingot_u238m2} (CE: {@code ItemUnstable}, damage 1-3 = inert "ELEMENTS"/"ARSENIC"/
 *     "VAULT" reskins, confirmed still-live via {@code MagicRecipes} and {@code ItemMS}'s loot list)
 *     becomes {@code ingot_u238m2}, {@code ingot_u238m2_elements}, {@code ingot_u238m2_arsenic},
 *     {@code ingot_u238m2_vault}. {@code nugget_u238m2} shares the same {@code ItemUnstable} class
 *     (so technically also carries damage 1-3 in CE) but has zero references to those metas anywhere
 *     in CE's codebase - kept as a single item rather than inventing three unused variants with no
 *     evidence of real content.</li>
 * </ul>
 *
 * <p><b>Deferred behavior</b> (registered as plain items; see the ported classes' javadoc and
 * {@code docs/phase1/items_ingot_nugget_notes.md} for the full reasoning):
 * <ul>
 *     <li>{@code ItemHot}'s heat-countdown mechanic is ported for {@code ingot_chainsteel} (maxHeat
 *     100), {@code ingot_meteorite} and {@code ingot_meteorite_forged} (maxHeat 200 each) - constants
 *     confirmed against CE's {@code ModItems.java} lines 884-886 - now that
 *     {@code SpecialItemComponents.HEAT} is registered on the mod event bus (see
 *     {@code ModItems.register()}). {@code ItemHotDusted}'s parallel per-purity-level heat mechanic
 *     ({@code ingot_steel_dusted_*}) is still deferred: no {@code ItemHotDusted} port exists yet, and
 *     porting it would mean either 10 near-duplicate heat-tracking classes or a shared refactor of
 *     {@code ItemHot} itself, which belongs to whichever area owns {@code com.hbm.items.special}, not
 *     this file. The purely cosmetic baked-model heat-glow overlay is also not reproduced for any of
 *     these (1.12 vertex-color rendering hack with no 1.21 model-json equivalent, a resource-pack
 *     concern for a later phase).</li>
 *     <li>{@code ItemUnstable}'s nuclear-detonation timer ({@code ingot_u238m2}, {@code nugget_u238m2},
 *     {@code ingot_electronium} - not the inert {@code _elements}/{@code _arsenic}/{@code _vault}
 *     reskins) no longer needs a bespoke {@code Item} subclass here: Phase 3's
 *     {@code docs/phase3/scattered_military_items.md} wires it via the already-real
 *     {@code com.hbm.hazard.type.HazardTypeUnstable} parametric hazard strategy instead (see
 *     {@code com.hbm.hazard.HazardRegistry#registerItems()}'s own binding block) - these three
 *     fields stay plain {@link Item}s here; the decay/detonation behavior is bound externally.</li>
 *     <li>{@code ItemCustomLore}'s "polaroid ID 11" easter-egg branch (the {@code .desc.P11} lines on
 *     {@code ingot_lanthanium}, {@code ingot_neptunium}, {@code ingot_tantalium} and
 *     {@code nugget_tantalium}) is not reproduced: it depends on {@code MainRegistry.polaroidID}, an
 *     unported mechanic. The port's {@code ItemCustomLore} (unlike CE's) only ever renders the plain
 *     {@code .desc} line, which is exactly the fallback CE itself used whenever {@code polaroidID != 11}
 *     - so these four items already show their non-Easter-egg CE tooltip faithfully.</li>
 * </ul>
 */
public final class IngotNuggetItems {

    private static final int FUEL_BURN_TIME = 1600;
    private static final int STEEL_DUSTED_VARIANT_COUNT = 10;

    // ===== ingot_ family (CE ModItems.java ~L780-886) =====

    public static final DeferredItem<Item> INGOT_STEEL = registerIngot("ingot_steel");
    public static final DeferredItem<Item> INGOT_TITANIUM = registerIngot("ingot_titanium");
    public static final DeferredItem<Item> INGOT_COPPER = registerIngot("ingot_copper");
    public static final DeferredItem<Item> INGOT_RED_COPPER = registerIngot("ingot_red_copper");
    public static final DeferredItem<Item> INGOT_TUNGSTEN = registerIngot("ingot_tungsten");
    public static final DeferredItem<Item> INGOT_ALUMINIUM = registerIngot("ingot_aluminium");
    public static final DeferredItem<Item> INGOT_BERYLLIUM = registerIngot("ingot_beryllium");
    public static final DeferredItem<Item> INGOT_LEAD = registerIngot("ingot_lead");

    // ItemCustomLore: CE's lang file has a real item.ingot_asbestos.desc flavor-text entry; the
    // port renders it under the modern item.hbm.ingot_asbestos.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_ASBESTOS = registerLoreIngot("ingot_asbestos");

    public static final DeferredItem<Item> INGOT_MAGNETIZED_TUNGSTEN = registerIngot("ingot_magnetized_tungsten");

    // ItemCustomLore: CE's lang file has a real item.ingot_combine_steel.desc flavor-text entry;
    // the port renders it under the modern item.hbm.ingot_combine_steel.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_COMBINE_STEEL = registerLoreIngot("ingot_combine_steel");
    public static final DeferredItem<Item> INGOT_DURA_STEEL = registerIngot("ingot_dura_steel");
    public static final DeferredItem<Item> INGOT_TECHNETIUM = registerIngot("ingot_technetium");
    public static final DeferredItem<Item> INGOT_TCALLOY = registerIngot("ingot_tcalloy");
    public static final DeferredItem<Item> INGOT_TUNGSTEN_CARBIDE = registerIngot("ingot_tungsten_carbide");
    public static final DeferredItem<Item> INGOT_CDALLOY = registerIngot("ingot_cdalloy");
    public static final DeferredItem<Item> INGOT_POLYMER = registerIngot("ingot_polymer");
    public static final DeferredItem<Item> INGOT_BAKELITE = registerIngot("ingot_bakelite");
    public static final DeferredItem<Item> INGOT_RUBBER = registerIngot("ingot_rubber");
    public static final DeferredItem<Item> INGOT_BIORUBBER = registerIngot("ingot_biorubber");
    public static final DeferredItem<Item> INGOT_PC = registerIngot("ingot_pc");
    public static final DeferredItem<Item> INGOT_PVC = registerIngot("ingot_pvc");
    public static final DeferredItem<Item> INGOT_DESH = registerIngot("ingot_desh");
    public static final DeferredItem<Item> INGOT_SATURNITE = registerIngot("ingot_saturnite");
    public static final DeferredItem<Item> INGOT_FERROURANIUM = registerIngot("ingot_ferrouranium");
    public static final DeferredItem<Item> INGOT_STARMETAL = registerIngot("ingot_starmetal");
    public static final DeferredItem<Item> INGOT_OSMIRIDIUM = registerIngot("ingot_osmiridium");

    // ItemCustomLore: CE's lang file has a real item.ingot_euphemium.desc flavor-text entry; the
    // port renders it under the modern item.hbm.ingot_euphemium.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_EUPHEMIUM = registerLoreIngot("ingot_euphemium");

    public static final DeferredItem<Item> INGOT_DINEUTRONIUM = registerIngot("ingot_dineutronium");
    public static final DeferredItem<Item> INGOT_CADMIUM = registerIngot("ingot_cadmium");
    public static final DeferredItem<Item> INGOT_BISMUTH = registerIngot("ingot_bismuth");
    public static final DeferredItem<Item> INGOT_ARSENIC = registerIngot("ingot_arsenic");
    public static final DeferredItem<Item> INGOT_ZIRCONIUM = registerIngot("ingot_zirconium");
    public static final DeferredItem<Item> INGOT_BISMUTH_BRONZE = registerIngot("ingot_bismuth_bronze");
    public static final DeferredItem<Item> INGOT_ARSENIC_BRONZE = registerIngot("ingot_arsenic_bronze");
    public static final DeferredItem<Item> INGOT_BSCCO = registerIngot("ingot_bscco");
    public static final DeferredItem<Item> INGOT_CALCIUM = registerIngot("ingot_calcium");
    public static final DeferredItem<Item> INGOT_SILICON = registerIngot("ingot_silicon");
    public static final DeferredItem<Item> INGOT_GUNMETAL = registerIngot("ingot_gunmetal");
    public static final DeferredItem<Item> INGOT_WEAPONSTEEL = registerIngot("ingot_weaponsteel");
    public static final DeferredItem<Item> INGOT_CFT = registerIngot("ingot_cft");
    public static final DeferredItem<Item> INGOT_TH232 = registerIngot("ingot_th232");
    public static final DeferredItem<Item> INGOT_URANIUM = registerIngot("ingot_uranium");
    public static final DeferredItem<Item> INGOT_U233 = registerIngot("ingot_u233");
    public static final DeferredItem<Item> INGOT_U235 = registerIngot("ingot_u235");
    public static final DeferredItem<Item> INGOT_U238 = registerIngot("ingot_u238");

    // ItemUnstable, decay/explosion timer bound via HazardRegistry (see class javadoc). Damage 1-3
    // flattened per confirmed MagicRecipes/ItemMS usage.
    public static final DeferredItem<Item> INGOT_U238M2 = registerIngot("ingot_u238m2");
    public static final DeferredItem<Item> INGOT_U238M2_ELEMENTS = registerIngot("ingot_u238m2_elements");
    public static final DeferredItem<Item> INGOT_U238M2_ARSENIC = registerIngot("ingot_u238m2_arsenic");
    public static final DeferredItem<Item> INGOT_U238M2_VAULT = registerIngot("ingot_u238m2_vault");

    public static final DeferredItem<Item> INGOT_PLUTONIUM = registerIngot("ingot_plutonium");
    public static final DeferredItem<Item> INGOT_PU238 = registerIngot("ingot_pu238");
    public static final DeferredItem<Item> INGOT_PU239 = registerIngot("ingot_pu239");
    public static final DeferredItem<Item> INGOT_PU240 = registerIngot("ingot_pu240");
    public static final DeferredItem<Item> INGOT_PU241 = registerIngot("ingot_pu241");
    public static final DeferredItem<Item> INGOT_PU_MIX = registerIngot("ingot_pu_mix");
    public static final DeferredItem<Item> INGOT_AM241 = registerIngot("ingot_am241");
    public static final DeferredItem<Item> INGOT_AM242 = registerIngot("ingot_am242");
    public static final DeferredItem<Item> INGOT_AM_MIX = registerIngot("ingot_am_mix");

    // ItemSchraranium, cosmetic LBSM-compat name/tooltip swap (see class javadoc).
    public static final DeferredItem<Item> INGOT_SCHRARANIUM = register("ingot_schraranium", () -> new ItemSchraranium(new Item.Properties()));

    public static final DeferredItem<Item> INGOT_SCHRABIDIUM = registerIngot("ingot_schrabidium");
    public static final DeferredItem<Item> INGOT_SCHRABIDATE = registerIngot("ingot_schrabidate");
    public static final DeferredItem<Item> INGOT_SOLINIUM = registerIngot("ingot_solinium");
    public static final DeferredItem<Item> INGOT_MUD = registerIngot("ingot_mud");
    public static final DeferredItem<Item> INGOT_THORIUM_FUEL = registerIngot("ingot_thorium_fuel");
    public static final DeferredItem<Item> INGOT_URANIUM_FUEL = registerIngot("ingot_uranium_fuel");
    public static final DeferredItem<Item> INGOT_MOX_FUEL = registerIngot("ingot_mox_fuel");
    public static final DeferredItem<Item> INGOT_PLUTONIUM_FUEL = registerIngot("ingot_plutonium_fuel");
    public static final DeferredItem<Item> INGOT_NEPTUNIUM_FUEL = registerIngot("ingot_neptunium_fuel");
    public static final DeferredItem<Item> INGOT_AMERICIUM_FUEL = registerIngot("ingot_americium_fuel");
    public static final DeferredItem<Item> INGOT_LES = registerIngot("ingot_les");
    public static final DeferredItem<Item> INGOT_SCHRABIDIUM_FUEL = registerIngot("ingot_schrabidium_fuel");
    public static final DeferredItem<Item> INGOT_HES = registerIngot("ingot_hes");
    // ItemCustomLore: CE's lang file has a real item.ingot_neptunium.desc flavor-text entry (plus
    // a .desc.P11 easter-egg line, deferred - see class javadoc); the port renders the former
    // under the modern item.hbm.ingot_neptunium.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_NEPTUNIUM = registerLoreIngot("ingot_neptunium");
    public static final DeferredItem<Item> INGOT_TENNESSINE = registerIngot("ingot_tennessine");
    public static final DeferredItem<Item> INGOT_POLONIUM = registerIngot("ingot_polonium");
    public static final DeferredItem<Item> INGOT_PHOSPHORUS = registerIngot("ingot_phosphorus");
    public static final DeferredItem<Item> INGOT_BORON = registerIngot("ingot_boron");

    // ItemFuel: furnace burn time only, no per-item tooltip branch applies to this area's fields.
    public static final DeferredItem<Item> INGOT_GRAPHITE = register("ingot_graphite", () -> new ItemFuel(new Item.Properties(), FUEL_BURN_TIME));

    // ItemCustomLore: CE's lang file has a real item.ingot_fiberglass.desc flavor-text entry; the
    // port renders it under the modern item.hbm.ingot_fiberglass.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_FIBERGLASS = registerLoreIngot("ingot_fiberglass");

    // ItemFoodBase in CE, but ingot_smore has no per-item onFoodEaten/tooltip branch, so a plain
    // Item with food properties is fully faithful without needing a dedicated class.
    public static final DeferredItem<Item> INGOT_SMORE = register("ingot_smore",
            () -> new Item(new Item.Properties().food(new FoodProperties.Builder().nutrition(10).saturationModifier(20F).build())));

    public static final DeferredItem<Item> INGOT_NIOBIUM = registerIngot("ingot_niobium");
    public static final DeferredItem<Item> INGOT_ACTINIUM = registerIngot("ingot_actinium");
    public static final DeferredItem<Item> INGOT_BROMINE = registerIngot("ingot_bromine");
    public static final DeferredItem<Item> INGOT_CAESIUM = registerIngot("ingot_caesium");
    public static final DeferredItem<Item> INGOT_CERIUM = registerIngot("ingot_cerium");
    // ItemCustomLore: CE's lang file has a real item.ingot_lanthanium.desc flavor-text entry (plus
    // a .desc.P11 easter-egg line, deferred - see class javadoc); the port renders the former
    // under the modern item.hbm.ingot_lanthanium.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_LANTHANIUM = registerLoreIngot("ingot_lanthanium");

    // ItemCustomLore: CE's lang file has a real item.ingot_tantalium.desc flavor-text entry (plus
    // a .desc.P11 easter-egg line, deferred - see class javadoc); the port renders the former
    // under the modern item.hbm.ingot_tantalium.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_TANTALIUM = registerLoreIngot("ingot_tantalium");
    public static final DeferredItem<Item> INGOT_ASTATINE = registerIngot("ingot_astatine");
    public static final DeferredItem<Item> INGOT_FIREBRICK = registerIngot("ingot_firebrick");
    public static final DeferredItem<Item> INGOT_COBALT = registerIngot("ingot_cobalt");
    public static final DeferredItem<Item> INGOT_CO60 = registerIngot("ingot_co60");
    public static final DeferredItem<Item> INGOT_SR90 = registerIngot("ingot_sr90");
    public static final DeferredItem<Item> INGOT_IODINE = registerIngot("ingot_iodine");
    public static final DeferredItem<Item> INGOT_I131 = registerIngot("ingot_i131");
    public static final DeferredItem<Item> INGOT_AU198 = registerIngot("ingot_au198");
    public static final DeferredItem<Item> INGOT_PB209 = registerIngot("ingot_pb209");
    public static final DeferredItem<Item> INGOT_RA226 = registerIngot("ingot_ra226");

    // ItemCustomLore: CE's lang file has a real item.ingot_gh336.desc flavor-text entry; the port
    // renders it under the modern item.hbm.ingot_gh336.desc key (see class javadoc).
    public static final DeferredItem<Item> INGOT_GH336 =
            register("ingot_gh336", () -> new ItemCustomLore(new Item.Properties().rarity(Rarity.EPIC)));

    // ItemUnstable, decay/explosion timer bound via HazardRegistry (see class javadoc). No confirmed
    // use of damage 1-3 anywhere in CE for this field, unlike ingot_u238m2 - kept as a single item.
    public static final DeferredItem<Item> INGOT_ELECTRONIUM = registerIngot("ingot_electronium");

    public static final DeferredItem<Item> INGOT_REIIUM = registerIngot("ingot_reiium");
    public static final DeferredItem<Item> INGOT_WEIDANIUM = registerIngot("ingot_weidanium");
    public static final DeferredItem<Item> INGOT_AUSTRALIUM = registerIngot("ingot_australium");
    public static final DeferredItem<Item> INGOT_VERTICIUM = registerIngot("ingot_verticium");
    public static final DeferredItem<Item> INGOT_UNOBTAINIUM = registerIngot("ingot_unobtainium");
    public static final DeferredItem<Item> INGOT_DAFFERGON = registerIngot("ingot_daffergon");

    // ItemHotDusted ported as 10 separate ItemHot instances with decreasing maxHeat (200 - purity*10).
    // CE AnvilRecipes.java:76-80 confirms 10 hot-smithing recipes for purity progression 0→1..8→9 + chainsteel.
    public static final List<DeferredItem<Item>> INGOT_STEEL_DUSTED = registerHotDustedSeries("ingot_steel_dusted", STEEL_DUSTED_VARIANT_COUNT);

    // ItemHot, heat-glow behavior ported. maxHeat constants confirmed against CE's ModItems.java
    // lines 884-886 (new ItemHot(100, "ingot_chainsteel") / new ItemHot(200, "ingot_meteorite") /
    // new ItemHot(200, "ingot_meteorite_forged")).
    public static final DeferredItem<Item> INGOT_CHAINSTEEL =
            register("ingot_chainsteel", () -> new ItemHot(new Item.Properties(), 100));
    public static final DeferredItem<Item> INGOT_METEORITE =
            register("ingot_meteorite", () -> new ItemHot(new Item.Properties(), 200));
    public static final DeferredItem<Item> INGOT_METEORITE_FORGED =
            register("ingot_meteorite_forged", () -> new ItemHot(new Item.Properties(), 200));

    // ===== scattered ingot_ entries (CE ModItems.java ~L1005, L1136-1137) =====

    // CE's field is literally named "ingot_mercury" but its registry name string is
    // "nugget_mercury" - there is no real "ingot_mercury" id in CE at all, only this one (a
    // seemingly unintentional CE naming mismatch between field name and registry id). Preserved
    // verbatim per the "keep hbm:<name> ids where legal" rule; named for the real id here instead
    // of CE's misleading field name.
    public static final DeferredItem<Item> NUGGET_MERCURY = registerNugget("nugget_mercury");

    // ItemLemon, scoped tooltip only (see class javadoc); no onFoodEaten side effect in CE.
    public static final DeferredItem<Item> INGOT_SEMTEX = register("ingot_semtex",
            () -> new ItemLemon(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(5F).build())));

    public static final DeferredItem<Item> INGOT_C4 = register("ingot_c4", () -> new ItemFuel(new Item.Properties(), FUEL_BURN_TIME));

    // ===== nugget_ family (CE ModItems.java ~L944-1006) =====

    public static final DeferredItem<Item> NUGGET_URANIUM = registerNugget("nugget_uranium");
    public static final DeferredItem<Item> NUGGET_U233 = registerNugget("nugget_u233");
    public static final DeferredItem<Item> NUGGET_U235 = registerNugget("nugget_u235");
    public static final DeferredItem<Item> NUGGET_U238 = registerNugget("nugget_u238");

    // ItemUnstable, decay/explosion timer bound via HazardRegistry (see class javadoc). No confirmed
    // use of damage 1-3 anywhere in CE for this field - kept as a single item, unlike ingot_u238m2.
    public static final DeferredItem<Item> NUGGET_U238M2 = registerNugget("nugget_u238m2");

    public static final DeferredItem<Item> NUGGET_PLUTONIUM = registerNugget("nugget_plutonium");
    public static final DeferredItem<Item> NUGGET_PU238 = registerNugget("nugget_pu238");
    public static final DeferredItem<Item> NUGGET_PU239 = registerNugget("nugget_pu239");
    public static final DeferredItem<Item> NUGGET_PU240 = registerNugget("nugget_pu240");
    public static final DeferredItem<Item> NUGGET_TH232 = registerNugget("nugget_th232");
    public static final DeferredItem<Item> NUGGET_PU241 = registerNugget("nugget_pu241");
    public static final DeferredItem<Item> NUGGET_PU_MIX = registerNugget("nugget_pu_mix");
    public static final DeferredItem<Item> NUGGET_AM241 = registerNugget("nugget_am241");
    public static final DeferredItem<Item> NUGGET_AM242 = registerNugget("nugget_am242");
    public static final DeferredItem<Item> NUGGET_AM_MIX = registerNugget("nugget_am_mix");
    public static final DeferredItem<Item> NUGGET_TECHNETIUM = registerNugget("nugget_technetium");
    public static final DeferredItem<Item> NUGGET_NEPTUNIUM = registerNugget("nugget_neptunium");
    public static final DeferredItem<Item> NUGGET_POLONIUM = registerNugget("nugget_polonium");
    public static final DeferredItem<Item> NUGGET_THORIUM_FUEL = registerNugget("nugget_thorium_fuel");
    public static final DeferredItem<Item> NUGGET_URANIUM_FUEL = registerNugget("nugget_uranium_fuel");
    // ItemCustomLore: CE's lang file has a real item.nugget_mox_fuel.desc flavor-text entry; the
    // port renders it under the modern item.hbm.nugget_mox_fuel.desc key (see class javadoc).
    public static final DeferredItem<Item> NUGGET_MOX_FUEL = registerLoreNugget("nugget_mox_fuel");
    public static final DeferredItem<Item> NUGGET_PLUTONIUM_FUEL = registerNugget("nugget_plutonium_fuel");
    public static final DeferredItem<Item> NUGGET_NEPTUNIUM_FUEL = registerNugget("nugget_neptunium_fuel");
    public static final DeferredItem<Item> NUGGET_AMERICIUM_FUEL = registerNugget("nugget_americium_fuel");
    public static final DeferredItem<Item> NUGGET_LES = registerNugget("nugget_les");
    public static final DeferredItem<Item> NUGGET_SCHRABIDIUM_FUEL = registerNugget("nugget_schrabidium_fuel");
    public static final DeferredItem<Item> NUGGET_HES = registerNugget("nugget_hes");
    public static final DeferredItem<Item> NUGGET_LEAD = registerNugget("nugget_lead");
    public static final DeferredItem<Item> NUGGET_BERYLLIUM = registerNugget("nugget_beryllium");
    public static final DeferredItem<Item> NUGGET_CADMIUM = registerNugget("nugget_cadmium");
    public static final DeferredItem<Item> NUGGET_BISMUTH = registerNugget("nugget_bismuth");
    public static final DeferredItem<Item> NUGGET_ARSENIC = registerNugget("nugget_arsenic");
    public static final DeferredItem<Item> NUGGET_ZIRCONIUM = registerNugget("nugget_zirconium");
    // ItemCustomLore: CE's lang file has a real item.nugget_tantalium.desc flavor-text entry (plus
    // a .desc.P11 easter-egg line, deferred - see class javadoc); the port renders the former
    // under the modern item.hbm.nugget_tantalium.desc key (see class javadoc).
    public static final DeferredItem<Item> NUGGET_TANTALIUM = registerLoreNugget("nugget_tantalium");
    public static final DeferredItem<Item> NUGGET_DESH = registerNugget("nugget_desh");
    public static final DeferredItem<Item> NUGGET_OSMIRIDIUM = registerNugget("nugget_osmiridium");
    public static final DeferredItem<Item> NUGGET_SCHRABIDIUM = registerNugget("nugget_schrabidium");
    public static final DeferredItem<Item> NUGGET_SOLINIUM = registerNugget("nugget_solinium");

    // ItemCustomLore: CE's lang file has a real item.nugget_euphemium.desc flavor-text entry; the
    // port renders it under the modern item.hbm.nugget_euphemium.desc key (see class javadoc).
    public static final DeferredItem<Item> NUGGET_EUPHEMIUM = registerLoreNugget("nugget_euphemium");

    public static final DeferredItem<Item> NUGGET_DINEUTRONIUM = registerNugget("nugget_dineutronium");
    public static final DeferredItem<Item> NUGGET_NIOBIUM = registerNugget("nugget_niobium");
    public static final DeferredItem<Item> NUGGET_SILICON = registerNugget("nugget_silicon");
    public static final DeferredItem<Item> NUGGET_ACTINIUM = registerNugget("nugget_actinium");
    public static final DeferredItem<Item> NUGGET_COBALT = registerNugget("nugget_cobalt");
    public static final DeferredItem<Item> NUGGET_CO60 = registerNugget("nugget_co60");
    public static final DeferredItem<Item> NUGGET_STRONTIUM = registerNugget("nugget_strontium");
    public static final DeferredItem<Item> NUGGET_SR90 = registerNugget("nugget_sr90");
    public static final DeferredItem<Item> NUGGET_PB209 = registerNugget("nugget_pb209");
    // ItemCustomLore: CE's lang file has a real item.nugget_gh336.desc flavor-text entry; the port
    // renders it under the modern item.hbm.nugget_gh336.desc key (see class javadoc).
    public static final DeferredItem<Item> NUGGET_GH336 = registerLoreNugget("nugget_gh336");
    public static final DeferredItem<Item> NUGGET_AU198 = registerNugget("nugget_au198");
    public static final DeferredItem<Item> NUGGET_RA226 = registerNugget("nugget_ra226");
    public static final DeferredItem<Item> NUGGET_REIIUM = registerNugget("nugget_reiium");
    public static final DeferredItem<Item> NUGGET_WEIDANIUM = registerNugget("nugget_weidanium");
    public static final DeferredItem<Item> NUGGET_AUSTRALIUM = registerNugget("nugget_australium");
    public static final DeferredItem<Item> NUGGET_AUSTRALIUM_LESSER = registerNugget("nugget_australium_lesser");
    public static final DeferredItem<Item> NUGGET_AUSTRALIUM_GREATER = registerNugget("nugget_australium_greater");
    public static final DeferredItem<Item> NUGGET_VERTICIUM = registerNugget("nugget_verticium");
    public static final DeferredItem<Item> NUGGET_UNOBTAINIUM = registerNugget("nugget_unobtainium");
    public static final DeferredItem<Item> NUGGET_UNOBTAINIUM_LESSER = registerNugget("nugget_unobtainium_lesser");
    public static final DeferredItem<Item> NUGGET_UNOBTAINIUM_GREATER = registerNugget("nugget_unobtainium_greater");
    public static final DeferredItem<Item> NUGGET_DAFFERGON = registerNugget("nugget_daffergon");

    // CE's field is named "nugget_mercury" but its registry name string is "nugget_mercury_tiny"
    // (see NUGGET_MERCURY above for the matching "ingot_mercury" field / "nugget_mercury" id case).
    public static final DeferredItem<Item> NUGGET_MERCURY_TINY = registerNugget("nugget_mercury_tiny");

    private IngotNuggetItems() {
    }

    /**
     * No-op body: every registration above happens via static field initializers, so merely
     * invoking this method (an active use of the class, per JLS 12.4.1) is enough to trigger them.
     * Kept as a method purely so {@code ModItems.register()} can call into this area the same way
     * it calls {@code MaterialItemGenerator.registerAll()}.
     */
    public static void registerAll() {
    }

    private static DeferredItem<Item> registerIngot(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    private static DeferredItem<Item> registerNugget(String name) {
        return register(name, () -> new Item(new Item.Properties()));
    }

    /**
     * Same as {@link #registerIngot(String)}, but backed by {@link ItemCustomLore} for the ingot
     * fields that carry a real {@code item.<name>.desc} flavor-text lang entry in CE (see class
     * javadoc) - a plain {@link Item} would silently drop that tooltip line. Note {@link
     * ItemCustomLore} looks the line up under the modern namespaced {@code item.hbm.<name>.desc}
     * key, not CE's literal {@code item.<name>.desc} key - the port's own lang file must be written
     * with the {@code hbm} segment, not copied verbatim from CE's.
     */
    private static DeferredItem<Item> registerLoreIngot(String name) {
        return register(name, () -> new ItemCustomLore(new Item.Properties()));
    }

    /**
     * Same as {@link #registerNugget(String)}, but backed by {@link ItemCustomLore} for the nugget
     * fields that carry a real {@code item.<name>.desc} flavor-text lang entry in CE (see class
     * javadoc) - a plain {@link Item} would silently drop that tooltip line. Note {@link
     * ItemCustomLore} looks the line up under the modern namespaced {@code item.hbm.<name>.desc}
     * key, not CE's literal {@code item.<name>.desc} key - the port's own lang file must be written
     * with the {@code hbm} segment, not copied verbatim from CE's.
     */
    private static DeferredItem<Item> registerLoreNugget(String name) {
        return register(name, () -> new ItemCustomLore(new Item.Properties()));
    }

    private static List<DeferredItem<Item>> registerIngotSeries(String baseName, int count) {
        List<DeferredItem<Item>> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            items.add(registerIngot(baseName + "_" + i));
        }
        return List.copyOf(items);
    }

    /** CE ItemHotDusted ported as N ItemHot instances with maxHeat = 200 - purity*10. */
    private static List<DeferredItem<Item>> registerHotDustedSeries(String baseName, int count) {
        List<DeferredItem<Item>> items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            final int purity = i;
            int maxHeat = 200 - purity * 10;
            items.add(register(baseName + "_" + i, () -> new ItemHot(new Item.Properties(), maxHeat)));
        }
        return List.copyOf(items);
    }

    private static DeferredItem<Item> register(String name, Supplier<Item> factory) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, factory);
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }
}
