package com.hbm.items;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.food.ItemLemon;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.special.ItemFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.BonemealEvent;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Ports CE's hardcoded {@code billet_}/{@code powder_} resource item fields (upstream hbm-ce
 * {@code ModItems.java}, lines ~888-942 and ~1009-1129/1293-1294; see
 * {@code docs/phase1/moditems_generative.md} section 3).
 * <p>
 * CE backs almost every one of these with {@code ItemBakedBase} or {@code ItemCustomLore}. Reading
 * both classes end to end shows their only behavior beyond a plain {@code Item} is: (a) a 1.12
 * custom-model-loading path ({@code ItemBakedBase}), entirely superseded by 1.21's item model/
 * datagen pipeline, and (b) an optional tooltip line pulled from a {@code <key>.desc} lang entry
 * plus a handful of {@code this == ModItems.<specific other item>} special cases for glint/rarity
 * and a scrambling display name, none of which target any {@code billet_}/{@code powder_} field
 * (verified by reading {@code ItemCustomLore.java} in full: the special-cased items are runes,
 * coins and {@code undefined}, none of them resources in this family). No lang file carries a
 * {@code .desc} entry for any item in this family yet either, so the lore lookup would be a no-op
 * today regardless. Every plain-behavior entry below is therefore registered as a bare {@link Item}
 * via {@link #registerBillet(String)}/{@link #registerPowder(String)}, matching
 * {@link MaterialItemGenerator}'s precedent of not carrying CE's 1.12 rendering base classes
 * forward. {@code ItemCustomLore}/{@code ItemBakedBase} themselves are not ported as named classes
 * here: both live in CE's {@code com.hbm.items.special} package, which {@code docs/phase1/
 * items_special.md} claims as its own area's scope - creating a competing class of either name
 * would duplicate that area's work.
 * <p>
 * {@code powder_lignite}, {@code powder_coal}, {@code powder_coal_tiny} and {@code powder_fire} keep
 * CE's {@code ItemFuel} furnace-burn-time behavior faithfully via the named
 * {@link com.hbm.items.special.ItemFuel} class, constructed directly by
 * {@link #registerFuelPowder(String, int)} - that class landed in a parallel {@code items_special.md}
 * implementation wave, so no local anonymous subclass is needed for it any more.
 * {@code powder_fertilizer} keeps CE's {@code ItemFertilizer} area-bonemeal behavior via a local
 * anonymous {@link Item} subclass built by {@link #registerFertilizerPowder}, since CE's
 * {@code ItemFertilizer} (CE package {@code com.hbm.items.tool}) - claimed by
 * {@code docs/phase1/items_tool.md} ("bonemeal-alternative for IGrowable crops, fires vanilla
 * BonemealEvent") - has not landed as a named class as of this writing. Once that area lands its
 * canonical class, {@link #registerFertilizerPowder} can be pointed at it in place of the local
 * anonymous subclass with no registry-id, hazard, or creative-tab change - this file only avoids
 * introducing a second, competing implementation of that concept in the meantime.
 * {@link #registerFertilizerPowder}/{@link #fertilize} reproduce CE's 3x3x3
 * {@code ItemFertilizer.onItemUse} via the confirmed 1.21 {@link BonemealableBlock}/
 * {@link BonemealEvent} APIs, including CE's actual per-block success semantics: a valid target
 * counts as success (consumes the item, plays the growth particle) even when its
 * force-only-on-the-clicked-block chance roll fails - only the actual growth call
 * ({@code performBonemeal}) is gated by that roll (see {@link #fertilize}'s own javadoc for the
 * exact CE-to-1.21 API mapping).
 * <p>
 * {@code powder_cement} was CE's one {@code ItemLemon} (basic food, nutrition 2 / saturation 0.5, no
 * special tooltip or effect for this particular item) - it is now constructed directly via the named
 * {@link com.hbm.items.food.ItemLemon} class (see that class's own javadoc, which names
 * {@code powder_cement} as one of its two intended consumers outside the food-consumables catalog),
 * carrying a {@link FoodProperties} component built by {@link #registerCementPowder} rather than a
 * separate 1.12-style food-item class.
 * <p>
 * {@code powder_ash} ({@code ItemEnumMulti<EnumAshType>}) is flattened here as
 * {@code powder_ash_wood}/{@code _coal}/{@code _misc}/{@code _fly}/{@code _soot}/{@code _fullerene}.
 * {@code dust_tiny}, {@code bottle_mercury}, {@code cinnabar} (CE {@code ModItems.cinnabar}, the
 * CINNABAR.gem() ore-dict target) are registered here for StorageDrum / SILEX / SuperComputer I/O.
 */
public final class BilletPowderItems {

    private BilletPowderItems() {
    }

    // ==================== billet_ (55 fields, all plain resources in CE) ====================

    public static final DeferredItem<Item> BILLET_COBALT = registerBillet("billet_cobalt");
    public static final DeferredItem<Item> BILLET_SILICON = registerBillet("billet_silicon");
    public static final DeferredItem<Item> BILLET_TH232 = registerBillet("billet_th232");
    public static final DeferredItem<Item> BILLET_URANIUM = registerBillet("billet_uranium");
    public static final DeferredItem<Item> BILLET_U233 = registerBillet("billet_u233");
    public static final DeferredItem<Item> BILLET_U235 = registerBillet("billet_u235");
    public static final DeferredItem<Item> BILLET_U238 = registerBillet("billet_u238");
    public static final DeferredItem<Item> BILLET_UZH = registerBillet("billet_uzh");
    public static final DeferredItem<Item> BILLET_PLUTONIUM = registerBillet("billet_plutonium");
    public static final DeferredItem<Item> BILLET_PU238 = registerBillet("billet_pu238");
    public static final DeferredItem<Item> BILLET_PU239 = registerBillet("billet_pu239");
    public static final DeferredItem<Item> BILLET_PU240 = registerBillet("billet_pu240");
    public static final DeferredItem<Item> BILLET_PU241 = registerBillet("billet_pu241");
    public static final DeferredItem<Item> BILLET_PU_MIX = registerBillet("billet_pu_mix");
    public static final DeferredItem<Item> BILLET_AM241 = registerBillet("billet_am241");
    public static final DeferredItem<Item> BILLET_AM242 = registerBillet("billet_am242");
    public static final DeferredItem<Item> BILLET_AM_MIX = registerBillet("billet_am_mix");
    public static final DeferredItem<Item> BILLET_NEPTUNIUM = registerBillet("billet_neptunium");
    public static final DeferredItem<Item> BILLET_POLONIUM = registerBillet("billet_polonium");
    public static final DeferredItem<Item> BILLET_TECHNETIUM = registerBillet("billet_technetium");
    public static final DeferredItem<Item> BILLET_CO60 = registerBillet("billet_co60");
    public static final DeferredItem<Item> BILLET_SR90 = registerBillet("billet_sr90");
    public static final DeferredItem<Item> BILLET_AU198 = registerBillet("billet_au198");
    public static final DeferredItem<Item> BILLET_PB209 = registerBillet("billet_pb209");
    public static final DeferredItem<Item> BILLET_RA226 = registerBillet("billet_ra226");
    public static final DeferredItem<Item> BILLET_ACTINIUM = registerBillet("billet_actinium");
    public static final DeferredItem<Item> BILLET_GH336 = registerBillet("billet_gh336");
    public static final DeferredItem<Item> BILLET_BERYLLIUM = registerBillet("billet_beryllium");
    public static final DeferredItem<Item> BILLET_BISMUTH = registerBillet("billet_bismuth");
    public static final DeferredItem<Item> BILLET_ZIRCONIUM = registerBillet("billet_zirconium");
    public static final DeferredItem<Item> BILLET_ZFB_BISMUTH = registerBillet("billet_zfb_bismuth");
    public static final DeferredItem<Item> BILLET_ZFB_PU241 = registerBillet("billet_zfb_pu241");
    public static final DeferredItem<Item> BILLET_ZFB_AM_MIX = registerBillet("billet_zfb_am_mix");
    public static final DeferredItem<Item> BILLET_SCHRABIDIUM = registerBillet("billet_schrabidium");
    public static final DeferredItem<Item> BILLET_SOLINIUM = registerBillet("billet_solinium");
    public static final DeferredItem<Item> BILLET_THORIUM_FUEL = registerBillet("billet_thorium_fuel");
    public static final DeferredItem<Item> BILLET_URANIUM_FUEL = registerBillet("billet_uranium_fuel");
    public static final DeferredItem<Item> BILLET_MOX_FUEL = registerBillet("billet_mox_fuel");
    public static final DeferredItem<Item> BILLET_PLUTONIUM_FUEL = registerBillet("billet_plutonium_fuel");
    public static final DeferredItem<Item> BILLET_NEPTUNIUM_FUEL = registerBillet("billet_neptunium_fuel");
    public static final DeferredItem<Item> BILLET_AMERICIUM_FUEL = registerBillet("billet_americium_fuel");
    public static final DeferredItem<Item> BILLET_LES = registerBillet("billet_les");
    public static final DeferredItem<Item> BILLET_SCHRABIDIUM_FUEL = registerBillet("billet_schrabidium_fuel");
    public static final DeferredItem<Item> BILLET_HES = registerBillet("billet_hes");
    public static final DeferredItem<Item> BILLET_PO210BE = registerBillet("billet_po210be");
    public static final DeferredItem<Item> BILLET_RA226BE = registerBillet("billet_ra226be");
    public static final DeferredItem<Item> BILLET_PU238BE = registerBillet("billet_pu238be");
    public static final DeferredItem<Item> BILLET_AUSTRALIUM = registerBillet("billet_australium");
    public static final DeferredItem<Item> BILLET_AUSTRALIUM_LESSER = registerBillet("billet_australium_lesser");
    public static final DeferredItem<Item> BILLET_AUSTRALIUM_GREATER = registerBillet("billet_australium_greater");
    public static final DeferredItem<Item> BILLET_UNOBTAINIUM = registerBillet("billet_unobtainium");
    public static final DeferredItem<Item> BILLET_YHARONITE = registerBillet("billet_yharonite");
    public static final DeferredItem<Item> BILLET_BALEFIRE_GOLD = registerBillet("billet_balefire_gold");
    public static final DeferredItem<Item> BILLET_FLASHLEAD = registerBillet("billet_flashlead");
    public static final DeferredItem<Item> BILLET_NUCLEAR_WASTE = registerBillet("billet_nuclear_waste");

    // ==================== powder_ (120 registered here; powder_ash excluded, see class javadoc) ====================

    public static final DeferredItem<Item> POWDER_IRON = registerPowder("powder_iron");
    public static final DeferredItem<Item> POWDER_GOLD = registerPowder("powder_gold");
    public static final DeferredItem<Item> POWDER_DIAMOND = registerPowder("powder_diamond");
    public static final DeferredItem<Item> POWDER_EMERALD = registerPowder("powder_emerald");
    public static final DeferredItem<Item> POWDER_LAPIS = registerPowder("powder_lapis");
    public static final DeferredItem<Item> POWDER_TITANIUM = registerPowder("powder_titanium");
    public static final DeferredItem<Item> POWDER_TUNGSTEN = registerPowder("powder_tungsten");
    public static final DeferredItem<Item> POWDER_SODIUM = registerPowder("powder_sodium");
    public static final DeferredItem<Item> POWDER_CHLOROCALCITE = registerPowder("powder_chlorocalcite");
    public static final DeferredItem<Item> POWDER_MOLYSITE = registerPowder("powder_molysite");
    public static final DeferredItem<Item> POWDER_COPPER = registerPowder("powder_copper");
    public static final DeferredItem<Item> POWDER_BERYLLIUM = registerPowder("powder_beryllium");
    public static final DeferredItem<Item> POWDER_ALUMINIUM = registerPowder("powder_aluminium");
    public static final DeferredItem<Item> POWDER_LEAD = registerPowder("powder_lead");
    public static final DeferredItem<Item> POWDER_COMBINE_STEEL = registerPowder("powder_combine_steel");
    public static final DeferredItem<Item> POWDER_TCALLOY = registerPowder("powder_tcalloy");
    public static final DeferredItem<Item> POWDER_CDALLOY = registerPowder("powder_cdalloy");
    public static final DeferredItem<Item> POWDER_MAGNETIZED_TUNGSTEN = registerPowder("powder_magnetized_tungsten");
    public static final DeferredItem<Item> POWDER_CHLOROPHYTE = registerPowder("powder_chlorophyte");
    public static final DeferredItem<Item> POWDER_RED_COPPER = registerPowder("powder_red_copper");
    public static final DeferredItem<Item> POWDER_STEEL = registerPowder("powder_steel");
    public static final DeferredItem<Item> POWDER_STEEL_TINY = registerPowder("powder_steel_tiny");
    public static final DeferredItem<Item> POWDER_LITHIUM = registerPowder("powder_lithium");
    public static final DeferredItem<Item> POWDER_LITHIUM_TINY = registerPowder("powder_lithium_tiny");
    public static final DeferredItem<Item> POWDER_QUARTZ = registerPowder("powder_quartz");
    // Note: powder_fluorite/powder_sulfur do NOT exist as discrete items in CE — CE uses MaterialShapes
    // autogen F.dust()/S.dust() (MAT_FLUORITE/MAT_SULFUR .setAutogen(DUST)). This port now generates
    // fluorite_dust/sulfur_dust via MaterialShapes DUST autogen (MaterialItemGenerator).
    public static final DeferredItem<Item> POWDER_BORAX = registerPowder("powder_borax");
    public static final DeferredItem<Item> POWDER_DURA_STEEL = registerPowder("powder_dura_steel");
    public static final DeferredItem<Item> POWDER_POLYMER = registerPowder("powder_polymer");
    public static final DeferredItem<Item> POWDER_BAKELITE = registerPowder("powder_bakelite");
    public static final DeferredItem<Item> POWDER_LANTHANIUM = registerPowder("powder_lanthanium");
    public static final DeferredItem<Item> POWDER_LANTHANIUM_TINY = registerPowder("powder_lanthanium_tiny");
    public static final DeferredItem<Item> POWDER_ACTINIUM = registerPowder("powder_actinium");
    public static final DeferredItem<Item> POWDER_ACTINIUM_TINY = registerPowder("powder_actinium_tiny");
    public static final DeferredItem<Item> POWDER_BORON = registerPowder("powder_boron");
    public static final DeferredItem<Item> POWDER_BORON_TINY = registerPowder("powder_boron_tiny");
    public static final DeferredItem<Item> POWDER_SEMTEX_MIX = registerPowder("powder_semtex_mix");
    public static final DeferredItem<Item> POWDER_DESH = registerPowder("powder_desh");
    public static final DeferredItem<Item> POWDER_ZIRCONIUM = registerPowder("powder_zirconium");
    public static final DeferredItem<Item> POWDER_LIGNITE = registerFuelPowder("powder_lignite", 1200);
    public static final DeferredItem<Item> POWDER_ASBESTOS = registerPowder("powder_asbestos");
    public static final DeferredItem<Item> POWDER_CADMIUM = registerPowder("powder_cadmium");
    public static final DeferredItem<Item> POWDER_BISMUTH = registerPowder("powder_bismuth");
    public static final DeferredItem<Item> POWDER_COAL = registerFuelPowder("powder_coal", 1600);
    public static final DeferredItem<Item> POWDER_COAL_TINY = registerFuelPowder("powder_coal_tiny", 160);
    public static final DeferredItem<Item> POWDER_YELLOWCAKE = registerPowder("powder_yellowcake");
    public static final DeferredItem<Item> POWDER_THORIUM = registerPowder("powder_thorium");
    public static final DeferredItem<Item> POWDER_URANIUM = registerPowder("powder_uranium");
    public static final DeferredItem<Item> POWDER_PLUTONIUM = registerPowder("powder_plutonium");
    public static final DeferredItem<Item> POWDER_NEPTUNIUM = registerPowder("powder_neptunium");
    public static final DeferredItem<Item> POWDER_POLONIUM = registerPowder("powder_polonium");
    public static final DeferredItem<Item> POWDER_SCHRABIDIUM = registerPowder("powder_schrabidium");
    public static final DeferredItem<Item> POWDER_SCHRABIDATE = registerPowder("powder_schrabidate");
    public static final DeferredItem<Item> POWDER_EUPHEMIUM = registerPowder("powder_euphemium");
    public static final DeferredItem<Item> POWDER_DINEUTRONIUM = registerPowder("powder_dineutronium");
    public static final DeferredItem<Item> POWDER_IODINE = registerPowder("powder_iodine");
    public static final DeferredItem<Item> POWDER_IODINE_TINY = registerPowder("powder_iodine_tiny");
    public static final DeferredItem<Item> POWDER_ASTATINE = registerPowder("powder_astatine");
    public static final DeferredItem<Item> POWDER_NEODYMIUM = registerPowder("powder_neodymium");
    public static final DeferredItem<Item> POWDER_NEODYMIUM_TINY = registerPowder("powder_neodymium_tiny");
    public static final DeferredItem<Item> POWDER_CAESIUM = registerPowder("powder_caesium");
    public static final DeferredItem<Item> POWDER_REIIUM = registerPowder("powder_reiium");
    public static final DeferredItem<Item> POWDER_WEIDANIUM = registerPowder("powder_weidanium");
    public static final DeferredItem<Item> POWDER_AUSTRALIUM = registerPowder("powder_australium");
    public static final DeferredItem<Item> POWDER_VERTICIUM = registerPowder("powder_verticium");
    public static final DeferredItem<Item> POWDER_UNOBTAINIUM = registerPowder("powder_unobtainium");
    public static final DeferredItem<Item> POWDER_DAFFERGON = registerPowder("powder_daffergon");
    public static final DeferredItem<Item> POWDER_STRONTIUM = registerPowder("powder_strontium");
    public static final DeferredItem<Item> POWDER_COBALT = registerPowder("powder_cobalt");
    public static final DeferredItem<Item> POWDER_COBALT_TINY = registerPowder("powder_cobalt_tiny");
    public static final DeferredItem<Item> POWDER_BROMINE = registerPowder("powder_bromine");
    public static final DeferredItem<Item> POWDER_NIOBIUM = registerPowder("powder_niobium");
    public static final DeferredItem<Item> POWDER_NIOBIUM_TINY = registerPowder("powder_niobium_tiny");
    public static final DeferredItem<Item> POWDER_TANTALIUM = registerPowder("powder_tantalium");
    public static final DeferredItem<Item> POWDER_TENNESSINE = registerPowder("powder_tennessine");
    public static final DeferredItem<Item> POWDER_CERIUM = registerPowder("powder_cerium");
    public static final DeferredItem<Item> POWDER_CERIUM_TINY = registerPowder("powder_cerium_tiny");
    public static final DeferredItem<Item> POWDER_CALCIUM = registerPowder("powder_calcium");
    public static final DeferredItem<Item> POWDER_ICE = registerPowder("powder_ice");
    public static final DeferredItem<Item> POWDER_LIMESTONE = registerPowder("powder_limestone");
    public static final DeferredItem<Item> POWDER_DESH_MIX = registerPowder("powder_desh_mix");
    public static final DeferredItem<Item> POWDER_DESH_READY = registerPowder("powder_desh_ready");
    public static final DeferredItem<Item> POWDER_NITAN_MIX = registerPowder("powder_nitan_mix");
    public static final DeferredItem<Item> POWDER_SPARK_MIX = registerPowder("powder_spark_mix");
    public static final DeferredItem<Item> POWDER_FIRE = registerFuelPowder("powder_fire", 6400);
    public static final DeferredItem<Item> POWDER_METEORITE = registerPowder("powder_meteorite");
    public static final DeferredItem<Item> POWDER_METEORITE_TINY = registerPowder("powder_meteorite_tiny");
    public static final DeferredItem<Item> POWDER_FLUX = registerPowder("powder_flux");
    public static final DeferredItem<Item> POWDER_FERTILIZER = registerFertilizerPowder("powder_fertilizer");
    public static final DeferredItem<Item> DUST_TINY = registerResource("dust_tiny");
    public static final DeferredItem<Item> BOTTLE_MERCURY = registerResource("bottle_mercury");
    /** CE {@code ModItems.cinnabar} — CINNABAR.gem() / CINNABAR.crystal() ore-dict target. */
    public static final DeferredItem<Item> CINNABAR = registerResource("cinnabar");
    public static final java.util.Map<EnumAshType, DeferredItem<Item>> POWDER_ASH = new java.util.EnumMap<>(EnumAshType.class);
    static {
        for (EnumAshType type : EnumAshType.VALUES) {
            EnumAshType ash = type;
            String id = "powder_ash_" + ash.name().toLowerCase(java.util.Locale.ROOT);
            DeferredItem<Item> item = ModItems.ITEMS.register(id, () -> new Item(new Item.Properties()) {
                @Override
                public String getDescriptionId() {
                    return "item.hbm.powder_ash." + ash.name().toLowerCase(java.util.Locale.ROOT);
                }
            });
            CreativeTabContents.add(ModCreativeTabs.PARTS, item);
            POWDER_ASH.put(type, item);
        }
    }
    public static DeferredItem<Item> powderAsh(EnumAshType type) {
        return POWDER_ASH.get(type);
    }
    // CE's ItemCustomLore("powder_power", "powder_energy_alt") texture-path override is a datagen
    // concern (see docs/phase1/moditems_generative.md section 1 notes on .aot() overrides), not a
    // registration-id one; the registry id below stays "powder_power" either way.
    public static final DeferredItem<Item> POWDER_TEKTITE = registerPowder("powder_tektite");
    public static final DeferredItem<Item> POWDER_PALEOGENITE_TINY = registerPowder("powder_paleogenite_tiny");
    public static final DeferredItem<Item> POWDER_PALEOGENITE = registerPowder("powder_paleogenite");
    public static final DeferredItem<Item> POWDER_IMPURE_OSMIRIDIUM = registerPowder("powder_impure_osmiridium");
    public static final DeferredItem<Item> POWDER_OSMIRIDIUM = registerPowder("powder_osmiridium");
    public static final DeferredItem<Item> POWDER_MAGIC = registerPowder("powder_magic");
    public static final DeferredItem<Item> POWDER_CLOUD = registerPowder("powder_cloud");
    public static final DeferredItem<Item> POWDER_BALEFIRE = registerPowder("powder_balefire");
    public static final DeferredItem<Item> POWDER_SAWDUST = registerPowder("powder_sawdust");
    public static final DeferredItem<Item> POWDER_COLTAN_ORE = registerPowder("powder_coltan_ore");
    public static final DeferredItem<Item> POWDER_COLTAN = registerPowder("powder_coltan");
    public static final DeferredItem<Item> POWDER_POISON = registerPowder("powder_poison");
    public static final DeferredItem<Item> POWDER_THERMITE = registerPowder("powder_thermite");
    public static final DeferredItem<Item> POWDER_POWER = registerPowder("powder_power");
    public static final DeferredItem<Item> POWDER_CO60 = registerPowder("powder_co60");
    public static final DeferredItem<Item> POWDER_CO60_TINY = registerPowder("powder_co60_tiny");
    public static final DeferredItem<Item> POWDER_SR90 = registerPowder("powder_sr90");
    public static final DeferredItem<Item> POWDER_SR90_TINY = registerPowder("powder_sr90_tiny");
    public static final DeferredItem<Item> POWDER_AT209 = registerPowder("powder_at209");
    public static final DeferredItem<Item> POWDER_AT209_TINY = registerPowder("powder_at209_tiny");
    public static final DeferredItem<Item> POWDER_PB209 = registerPowder("powder_pb209");
    public static final DeferredItem<Item> POWDER_PB209_TINY = registerPowder("powder_pb209_tiny");
    public static final DeferredItem<Item> POWDER_I131 = registerPowder("powder_i131");
    public static final DeferredItem<Item> POWDER_I131_TINY = registerPowder("powder_i131_tiny");
    public static final DeferredItem<Item> POWDER_CS137 = registerPowder("powder_cs137");
    public static final DeferredItem<Item> POWDER_CS137_TINY = registerPowder("powder_cs137_tiny");
    public static final DeferredItem<Item> POWDER_XE135 = registerPowder("powder_xe135");
    public static final DeferredItem<Item> POWDER_XE135_TINY = registerPowder("powder_xe135_tiny");
    public static final DeferredItem<Item> POWDER_AU198 = registerPowder("powder_au198");
    public static final DeferredItem<Item> POWDER_AU198_TINY = registerPowder("powder_au198_tiny");
    public static final DeferredItem<Item> POWDER_RA226 = registerPowder("powder_ra226");

    // CE's ItemLemon(2, 0.5F, false, "powder_cement"): plain food, no special tooltip/effect for
    // this item in ItemLemon.java's path-switch branches (see that class - powder_cement falls
    // through its default case). CE's isWolfFood=false has no direct component equivalent (wolf
    // food preference is a #minecraft:wolf_food item tag in 1.21); omitted rather than guessed at,
    // since leaving a resource item off that tag is the tag's default state anyway.
    public static final DeferredItem<Item> POWDER_CEMENT = registerCementPowder();

    /** No-op beyond forcing this class to load before {@code ModItems.ITEMS.register(modEventBus)}. */
    public static void registerAll() {
    }

    private static DeferredItem<Item> registerBillet(String name) {
        return registerResource(name);
    }

    private static DeferredItem<Item> registerPowder(String name) {
        return registerResource(name);
    }

    private static DeferredItem<Item> registerResource(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new Item(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    /**
     * CE's {@code ItemFuel}: a fixed furnace burn time, now backed by the named
     * {@link ItemFuel} class from {@code com.hbm.items.special} (see the class javadoc).
     */
    private static DeferredItem<Item> registerFuelPowder(String name, int burnTime) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemFuel(new Item.Properties(), burnTime));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    /**
     * CE's {@code ItemFertilizer}: right-clicking a block applies bonemeal-equivalent growth to
     * every block in the 3x3x3 area centered on it, consuming one item if anything grew. CE posted
     * Forge's {@code BonemealEvent} per candidate block so other mods could veto/observe the
     * action; the 1.21 equivalent is {@link BonemealEvent} in
     * {@code net.neoforged.neoforge.event.entity.player} (confirmed against NeoForge 1.21.x
     * source: constructor {@code BonemealEvent(Player, Level, BlockPos, BlockState, ItemStack)},
     * plus {@code isValidBonemealTarget()} and {@code isSuccessful()}/{@code setSuccessful(boolean)}
     * on top of the inherited cancellable state). CE's separate {@code useFertillizer(ItemStack,
     * World, int, int, int)} static entry point for automated (dispenser-driven) use is not
     * reproduced: no dispenser behavior wiring exists yet in the port. See the class javadoc for
     * why this is an anonymous subclass rather than a named {@code ItemFertilizer} class.
     */
    private static DeferredItem<Item> registerFertilizerPowder(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new Item(new Item.Properties()) {
            @Override
            public InteractionResult useOn(UseOnContext context) {
                Level level = context.getLevel();
                Player player = context.getPlayer();
                BlockPos center = context.getClickedPos();
                ItemStack stack = context.getItemInHand();

                if (player != null && !player.mayUseItemAt(center, context.getClickedFace(), stack)) {
                    return InteractionResult.FAIL;
                }

                boolean didSomething = false;
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            BlockPos pos = center.offset(x, y, z);
                            boolean force = x == 0 && y == 0 && z == 0;
                            if (fertilize(level, player, stack, pos, force)) {
                                didSomething = true;
                                level.levelEvent(2005, pos, 0);
                            }
                        }
                    }
                }

                if (didSomething && (player == null || !player.getAbilities().instabuild)) {
                    stack.shrink(1);
                }

                return didSomething ? InteractionResult.SUCCESS : InteractionResult.PASS;
            }
        });
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    /**
     * Applies bonemeal-equivalent growth to a single candidate block, matching CE's
     * {@code ItemFertilizer.fertilize(..., force)} - including its actual (if arguably surprising)
     * success semantics: CE's {@code canGrow(...)} check (here {@link BonemealEvent#isValidBonemealTarget()})
     * alone decides success/consumption/particle for a position; CE's {@code force ||
     * canUseBonemeal(...)} chance roll (here {@link BonemealableBlock#isBonemealSuccess}) only gates
     * whether the actual growth call ({@code growable.grow(...)}, here
     * {@link BonemealableBlock#performBonemeal}) fires - a valid target that fails its own chance
     * roll still counts as success. CE only bypassed the chance roll itself, never the validity
     * check, on the exact clicked block ({@code force = true}); the other 26 positions in the 3x3x3
     * area still roll for the actual growth. This mirrors
     * {@link com.hbm.blocks.generic.BlockNTMFlower}/{@link com.hbm.blocks.generic.BlockTallPlant}'s
     * own {@code isValidBonemealTarget} + {@code isBonemealSuccess} + {@code performBonemeal} call
     * shape.
     */
    private static boolean fertilize(Level level, Player player, ItemStack stack, BlockPos pos, boolean force) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        BlockState state = level.getBlockState(pos);

        BonemealEvent event = new BonemealEvent(player, level, pos, state, stack);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return event.isSuccessful();
        }
        if (!event.isValidBonemealTarget()) {
            return false;
        }

        RandomSource random = level.getRandom();
        BonemealableBlock growable = (BonemealableBlock) state.getBlock();
        if (force || growable.isBonemealSuccess(level, random, pos, state)) {
            growable.performBonemeal(serverLevel, random, pos, state);
        }
        return true;
    }

    private static DeferredItem<Item> registerCementPowder() {
        FoodProperties food = new FoodProperties.Builder().nutrition(2).saturationModifier(0.5F).build();
        DeferredItem<Item> item = ModItems.ITEMS.register("powder_cement", () -> new ItemLemon(new Item.Properties().food(food)));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }
}
