package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Registers every Phase-1-safe {@code com.hbm.items.machine} item (see
 * {@code docs/phase1/items_machine.md}). Mirrors the shape of {@code BilletPowderItems}/
 * {@code PlateCrystalWasteItems}: a plain registration class whose {@code registerAll()} is called
 * from {@link ModItems#register}, exposing {@code DeferredItem}/{@code Map} fields other classes
 * (hazard bindings, cross-package references such as {@link ItemBlueprintFolder}) read from.
 * <p>
 * Naming note: several CE classes here backed multiple metadata variants with a single registry
 * field (e.g. {@code arc_electrode} for all 4 grades, {@code gear_large} for both gear grades,
 * {@code pile_rod} for all 9 {@code EnumPileRod} states). Post-flattening each variant needs its
 * own distinct id; where CE's single field name doesn't already disambiguate, this class derives
 * one from the variant's enum name (documented per section below) rather than inventing an
 * unrelated id.
 */
public final class MachineItems {

    private MachineItems() {
    }

    // ==================== fields other areas/hazard bindings reference ====================

    public static DeferredItem<Item> BLUEPRINTS;

    public static final Map<ItemArcElectrode.EnumElectrodeType, DeferredItem<Item>> ARC_ELECTRODES = new EnumMap<>(ItemArcElectrode.EnumElectrodeType.class);
    public static final Map<ItemZirnoxRod.EnumZirnoxType, DeferredItem<Item>> ZIRNOX_RODS = new EnumMap<>(ItemZirnoxRod.EnumZirnoxType.class);
    public static final Map<ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted, DeferredItem<Item>> ZIRNOX_RODS_DEPLETED = new EnumMap<>(ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.class);
    public static final Map<ItemPileRodMK2.EnumPileRod, DeferredItem<Item>> PILE_RODS_MK2 = new EnumMap<>(ItemPileRodMK2.EnumPileRod.class);
    public static final Map<ItemPWRFuel.EnumPWRFuel, DeferredItem<Item>> PWR_FUEL = new EnumMap<>(ItemPWRFuel.EnumPWRFuel.class);
    public static final Map<ItemWatzPellet.EnumWatzType, DeferredItem<Item>> WATZ_PELLET = new EnumMap<>(ItemWatzPellet.EnumWatzType.class);
    /**
     * Depleted counterpart to {@link #WATZ_PELLET}, exposed for the Phase 2 Watz reactor block
     * entity ({@code docs/phase2/machine_fusion_watz.md}) to swap a spent pellet stack into once its
     * yield reaches zero (CE's {@code ItemStack(ModItems.watz_pellet_depleted, 1, meta)} swap) - the
     * depleted item was already registered here in Phase 1 but its {@code DeferredItem} reference was
     * previously discarded since nothing consumed it yet.
     */
    public static final Map<ItemWatzPellet.EnumWatzType, DeferredItem<Item>> WATZ_PELLET_DEPLETED = new EnumMap<>(ItemWatzPellet.EnumWatzType.class);
    public static final Map<ItemBreedingRod.BreedingRodType, DeferredItem<Item>> BREEDING_ROD_SINGLE = new EnumMap<>(ItemBreedingRod.BreedingRodType.class);
    public static final Map<ItemBreedingRod.BreedingRodType, DeferredItem<Item>> BREEDING_ROD_DUAL = new EnumMap<>(ItemBreedingRod.BreedingRodType.class);
    public static final Map<ItemBreedingRod.BreedingRodType, DeferredItem<Item>> BREEDING_ROD_QUAD = new EnumMap<>(ItemBreedingRod.BreedingRodType.class);
    public static final Map<ItemPistons.EnumPistonType, DeferredItem<Item>> PISTONS = new EnumMap<>(ItemPistons.EnumPistonType.class);

    public static DeferredItem<Item> PLATE_FUEL_U233, PLATE_FUEL_U235, PLATE_FUEL_MOX, PLATE_FUEL_PU239, PLATE_FUEL_SA326, PLATE_FUEL_RA226BE, PLATE_FUEL_PU238BE;
    public static DeferredItem<Item> PELLET_RTG, PELLET_RTG_RADIUM, PELLET_RTG_WEAK, PELLET_RTG_STRONTIUM, PELLET_RTG_COBALT, PELLET_RTG_ACTINIUM,
            PELLET_RTG_POLONIUM, PELLET_RTG_LEAD, PELLET_RTG_GOLD, PELLET_RTG_AMERICIUM, PELLET_RTG_BALEFIRE;

    public static void registerAll() {
        registerArcElectrodes();
        registerBatteries();
        registerBlades();
        registerBlueprints();
        registerBreedingRods();
        registerCapacitorAndCassette();
        registerCatalysts();
        registerChemicalDye();
        registerDrillbits();
        registerFelCrystals();
        registerFluidContainers();
        registerGears();
        registerIcfPellet();
        registerLens();
        registerMachineUpgrades();
        registerMold();
        registerPaCoils();
        registerPileRods();
        registerPistons();
        registerPlateFuel();
        registerPwrFuel();
        registerRbmkPellets();
        registerRtgPellets();
        registerSatChipsAndSatellites();
        registerScraps();
        registerStamps();
        registerTurretItems();
        registerWatzPellets();
        registerZirnoxRods();
    }

    // ==================== ItemArcElectrode / ItemArcElectrodeBurnt ====================
    // CE: single "arc_electrode"/"arc_electrode_burnt" field, 4 metadata grades each.

    private static void registerArcElectrodes() {
        for (ItemArcElectrode.EnumElectrodeType type : ItemArcElectrode.EnumElectrodeType.VALUES) {
            String suffix = lower(type.name());
            DeferredItem<Item> electrode = reg("arc_electrode_" + suffix, () -> new ItemArcElectrode(type, props().stacksTo(1)));
            DeferredItem<Item> burnt = reg("arc_electrode_burnt_" + suffix, () -> new ItemArcElectrodeBurnt(type, props()));
            ARC_ELECTRODES.put(type, electrode);
            tab(ModCreativeTabs.CONTROL, electrode);
            tab(ModCreativeTabs.CONTROL, burnt);
        }
    }

    // ==================== ItemBattery / ItemBatteryCreative / ItemBatteryPack / ItemBatterySC ====================

    private static void registerBatteries() {
        registerPlainBattery("battery_generic", 5000, 100, 100, ModCreativeTabs.CONTROL);
        registerPlainBattery("battery_red_cell", 15000, 100, 100, null);
        registerPlainBattery("battery_red_cell_6", 15000L * 6, 100, 100, null);
        registerPlainBattery("battery_red_cell_24", 15000L * 24, 100, 100, null);
        registerPlainBattery("battery_advanced", 20000, 500, 500, null);
        registerPlainBattery("battery_advanced_cell", 60000, 500, 500, null);
        registerPlainBattery("battery_advanced_cell_4", 60000L * 4, 500, 500, null);
        registerPlainBattery("battery_advanced_cell_12", 60000L * 12, 500, 500, null);
        registerPlainBattery("battery_lithium", 250000, 1000, 1000, null);
        registerPlainBattery("battery_lithium_cell", 750000, 1000, 1000, null);
        registerPlainBattery("battery_lithium_cell_3", 750000L * 3, 1000, 1000, null);
        registerPlainBattery("battery_lithium_cell_6", 750000L * 6, 1000, 1000, null);
        tab(ModCreativeTabs.CONTROL, reg("battery_schrabidium",
                () -> new ItemBattery(1000000, 5000, 5000, Rarity.RARE, false, props())));
        registerPlainBattery("battery_schrabidium_cell", 3000000, 15000, 15000, null);
        registerPlainBattery("battery_schrabidium_cell_2", 3000000L * 2, 30000, 30000, null);
        registerPlainBattery("battery_schrabidium_cell_4", 3000000L * 4, 60000, 60000, null);
        registerPlainBattery("battery_spark_cell_6", 100000000L * 6, 2000000, 2000000, null);
        registerPlainBattery("battery_spark_cell_25", 100000000L * 25, 2000000, 2000000, null);
        registerPlainBattery("battery_spark_cell_100", 1000000000L * 10, 1000000L * 5, 1000000L * 5, null);
        registerPlainBattery("battery_spark_cell_1000", 1000000000L * 100, 10000000L * 5, 10000000L * 5, null);
        registerPlainBattery("battery_spark_cell_2500", 1000000000L * 250, 100000000L * 5, 100000000L * 5, null);
        registerPlainBattery("cube_power", 1000000000000000000L, 1000000000000000L, 1000000000000000L, ModCreativeTabs.CONTROL);
        registerPlainBattery("battery_spark_cell_10000", 1000000000L * 1000, 1000000000L * 5, 1000000000L * 5, null);
        registerPlainBattery("battery_spark_cell_power", 1000000000L * 100000, 1000000000L * 500, 1000000000L * 500, null);
        tab(ModCreativeTabs.CONTROL, reg("battery_potato", () -> new ItemBattery(100, 0, 100, props())));
        tab(ModCreativeTabs.CONTROL, reg("energy_core",
                () -> new ItemBattery(10000000, 0, 1000, Rarity.UNCOMMON, true, props())));
        registerPlainBattery("memory", Long.MAX_VALUE / 100L, 100000000000000L, 100000000000000L, null);

        tab(ModCreativeTabs.CONTROL, reg("battery_creative", ItemBatteryCreative::new));

        for (ItemBatteryPack.EnumBatteryPack type : ItemBatteryPack.EnumBatteryPack.VALUES) {
            tab(ModCreativeTabs.CONTROL, reg(lower(type.name()) + "_pack", () -> new ItemBatteryPack(type, props().stacksTo(1))));
        }
        for (ItemBatterySC.EnumBatterySC type : ItemBatterySC.EnumBatterySC.VALUES) {
            tab(ModCreativeTabs.CONTROL, reg("battery_sc_" + lower(type.name()), () -> new ItemBatterySC(type, props().stacksTo(1))));
        }
    }

    private static void registerPlainBattery(String name, long maxCharge, long chargeRate, long dischargeRate, ResourceKey<CreativeModeTab> tab) {
        DeferredItem<Item> item = reg(name, () -> new ItemBattery(maxCharge, chargeRate, dischargeRate, props()));
        if (tab != null) tab(tab, item);
    }

    // ==================== ItemBlades ====================
    // CE: blades_steel(400 dura)/blades_titanium(500 dura)/blades_desh(unbreakable, stacksTo(1), CONTROL tab).
    // blade_titanium/blade_tungsten in CE are plain ItemBase, out of this area's scope.

    private static void registerBlades() {
        reg("blades_steel", () -> new ItemBlades(props().durability(400)));
        reg("blades_titanium", () -> new ItemBlades(props().durability(500)));
        tab(ModCreativeTabs.CONTROL, reg("blades_desh", () -> new ItemBlades(props().stacksTo(1))));
    }

    // ==================== ItemBlueprints / ItemBlueprintFolder ====================

    private static void registerBlueprints() {
        BLUEPRINTS = reg("blueprints", () -> new ItemBlueprints(props().stacksTo(1)));
        tab(ModCreativeTabs.TEMPLATE, BLUEPRINTS);

        for (ItemBlueprintFolder.Kind kind : ItemBlueprintFolder.Kind.values()) {
            tab(ModCreativeTabs.TEMPLATE, reg("blueprint_folder_" + lower(kind.name()), () -> new ItemBlueprintFolder(kind, props())));
        }
    }

    // ==================== ItemBreedingRod ====================
    // CE: rod/rod_dual/rod_quad, each a single field with 17 BreedingRodType metadata variants.

    private static void registerBreedingRods() {
        for (ItemBreedingRod.BreedingRodType type : ItemBreedingRod.BreedingRodType.VALUES) {
            String suffix = lower(type.name());
            DeferredItem<Item> single = reg("rod_" + suffix, () -> new ItemBreedingRod(ItemBreedingRod.Multiplicity.SINGLE, type, props()));
            DeferredItem<Item> dual = reg("rod_dual_" + suffix, () -> new ItemBreedingRod(ItemBreedingRod.Multiplicity.DUAL, type, props()));
            DeferredItem<Item> quad = reg("rod_quad_" + suffix, () -> new ItemBreedingRod(ItemBreedingRod.Multiplicity.QUAD, type, props()));
            BREEDING_ROD_SINGLE.put(type, single);
            BREEDING_ROD_DUAL.put(type, dual);
            BREEDING_ROD_QUAD.put(type, quad);
            tab(ModCreativeTabs.CONTROL, single);
            tab(ModCreativeTabs.CONTROL, dual);
            tab(ModCreativeTabs.CONTROL, quad);
        }
    }

    // ==================== ItemCapacitor / ItemCassette ====================

    private static void registerCapacitorAndCassette() {
        tab(ModCreativeTabs.CONTROL, reg("redcoil_capacitor", () -> new ItemCapacitor(10, props().stacksTo(1))));
        tab(ModCreativeTabs.TEMPLATE, reg("siren_track", () -> new ItemCassette(props())));
    }

    // ==================== ItemCatalyst ====================

    private static void registerCatalysts() {
        registerCatalyst("ams_catalyst_iron", 0xFF7E22, 10, 0.50F, 1.50F, 1.50F);
        registerCatalyst("ams_catalyst_copper", 0xAADE29, 100, 0.60F, 1.20F, 0.60F);
        registerCatalyst("ams_catalyst_aluminium", 0xCCCCCC, 250, 0.70F, 0.85F, 0.85F);
        registerCatalyst("ams_catalyst_lithium", 0xFF2727, 500, 0.80F, 0.75F, 1.15F);
        registerCatalyst("ams_catalyst_beryllium", 0x97978B, 1000, 0.90F, 1.15F, 0.75F);
        registerCatalyst("ams_catalyst_tungsten", 0xF5FF48, 5000, 1.00F, 1.00F, 0.95F);
        registerCatalyst("ams_catalyst_cobalt", 0x789BBE, 10000, 1.02F, 0.95F, 1.00F);
        registerCatalyst("ams_catalyst_niobium", 0x3BF1B6, 25000, 1.05F, 1.15F, 1.00F);
        registerCatalyst("ams_catalyst_cerium", 0x1D3FFF, 50000, 1.05F, 1.00F, 1.15F);
        registerCatalyst("ams_catalyst_thorium", 0x653B22, 100000, 1.10F, 0.95F, 1.20F);
        registerCatalyst("ams_catalyst_strontium", 0xDD0D35, 200000, 1.15F, 0.90F, 1.30F);
        registerCatalyst("ams_catalyst_caesium", 0x6400FF, 400000, 1.20F, 0.85F, 1.40F);
        registerCatalyst("ams_catalyst_schrabidium", 0x32FFFF, 600000, 1.30F, 0.70F, 1.25F);
        registerCatalyst("ams_catalyst_euphemium", 0xFF9CD2, 800000, 1.50F, 1.25F, 0.70F);
        registerCatalyst("ams_catalyst_dineutronium", 0x334077, 1000000, 2.00F, 1.50F, 2.00F);
    }

    private static void registerCatalyst(String name, int color, long powerAbs, float powerMod, float heatMod, float fuelMod) {
        tab(ModCreativeTabs.CONTROL, reg(name, () -> new ItemCatalyst(color, powerAbs, powerMod, heatMod, fuelMod, props().stacksTo(1))));
    }

    // ==================== ItemChemicalDye ====================
    // CE also has a "crayon" base item via a different class (ItemCrayon), out of this area's scope.

    private static void registerChemicalDye() {
        for (ItemChemicalDye.EnumChemDye dye : ItemChemicalDye.EnumChemDye.VALUES) {
            tab(ModCreativeTabs.PARTS, reg("chemical_dye_" + lower(dye.name()), () -> new ItemChemicalDye(dye, props())));
        }
    }

    // ==================== ItemDrillbit ====================

    private static void registerDrillbits() {
        for (com.hbm.items.ItemEnums.EnumDrillType type : com.hbm.items.ItemEnums.EnumDrillType.VALUES) {
            tab(ModCreativeTabs.CONTROL, reg("drillbit_" + lower(type.name()), () -> new ItemDrillbit(type, props())));
        }
    }

    // ==================== ItemFELCrystal ====================
    // CE: one class instantiation per wavelength; EnumWavelengths.NULL is an internal-only default, not registered.

    private static void registerFelCrystals() {
        registerFelCrystal("laser_crystal_co2", ItemFELCrystal.EnumWavelengths.IR);
        registerFelCrystal("laser_crystal_bismuth", ItemFELCrystal.EnumWavelengths.VISIBLE);
        registerFelCrystal("laser_crystal_cmb", ItemFELCrystal.EnumWavelengths.UV);
        registerFelCrystal("laser_crystal_bale", ItemFELCrystal.EnumWavelengths.GAMMA);
        registerFelCrystal("laser_crystal_digamma", ItemFELCrystal.EnumWavelengths.DRX);
    }

    private static void registerFelCrystal(String name, ItemFELCrystal.EnumWavelengths wavelength) {
        tab(ModCreativeTabs.CONTROL, reg(name, () -> new ItemFELCrystal(wavelength, props())));
    }

    // ==================== ItemFluidTank / ItemFluidTankV2 / ItemFluidIcon ====================
    // CE's fluid_tank_empty/fluid_barrel_empty/fluid_pack_empty companions use ItemBakedBase, out of this area's scope.

    private static void registerFluidContainers() {
        tab(ModCreativeTabs.CONTROL, reg("fluid_tank_full", () -> new ItemFluidTank(1000, props())));
        tab(ModCreativeTabs.CONTROL, reg("fluid_tank_lead_full", () -> new ItemFluidTank(1000, props())));
        tab(ModCreativeTabs.CONTROL, reg("fluid_barrel_full", () -> new ItemFluidTank(16000, props())));
        tab(ModCreativeTabs.CONTROL, reg("fluid_pack_full", () -> new ItemFluidTank(32000, props())));

        tab(ModCreativeTabs.CONTROL, reg("fluid_tank_v2", () -> new ItemFluidTankV2(1000, props())));
        tab(ModCreativeTabs.CONTROL, reg("fluid_tank_lead_v2", () -> new ItemFluidTankV2(1000, props())));
        tab(ModCreativeTabs.CONTROL, reg("fluid_barrel_v2", () -> new ItemFluidTankV2(16000, props())));

        reg("fluid_icon", ItemFluidIcon::new); // CE: setCreativeTab(null), pure GUI helper, never shown in creative
    }

    // ==================== ItemGear ====================
    // CE: single "gear_large" field, 2 metadata grades (index 0 = bronze, 1 = steel) - split into two ids.

    private static void registerGears() {
        tab(ModCreativeTabs.PARTS, reg("gear_bronze", () -> new ItemGear(ItemGear.GearType.BRONZE, props())));
        tab(ModCreativeTabs.PARTS, reg("gear_steel", () -> new ItemGear(ItemGear.GearType.STEEL, props())));
    }

    // ==================== ItemICFPellet ====================

    private static void registerIcfPellet() {
        tab(ModCreativeTabs.CONTROL, reg("icf_pellet", ItemICFPellet::new));
    }

    // ==================== ItemLens ====================

    private static void registerLens() {
        tab(ModCreativeTabs.CONTROL, reg("ams_lens", () -> new ItemLens(60L * 60 * 60 * 20 * 100, props().stacksTo(1))));
    }

    // ==================== ItemMachineUpgrade ====================

    private static void registerMachineUpgrades() {
        registerUpgrade("upgrade_speed_1", ItemMachineUpgrade.UpgradeType.SPEED, 1, 1, "desc.upgrade1");
        registerUpgrade("upgrade_speed_2", ItemMachineUpgrade.UpgradeType.SPEED, 2, 2, "desc.upgrade1");
        registerUpgrade("upgrade_speed_3", ItemMachineUpgrade.UpgradeType.SPEED, 3, 3, "desc.upgrade1");
        registerUpgrade("upgrade_effect_1", ItemMachineUpgrade.UpgradeType.EFFECT, 1, 0, "desc.upgrade2");
        registerUpgrade("upgrade_effect_2", ItemMachineUpgrade.UpgradeType.EFFECT, 2, 0, "desc.upgrade2");
        registerUpgrade("upgrade_effect_3", ItemMachineUpgrade.UpgradeType.EFFECT, 3, 0, "desc.upgrade2");
        registerUpgrade("upgrade_power_1", ItemMachineUpgrade.UpgradeType.POWER, 1, 0, "desc.upgrade3");
        registerUpgrade("upgrade_power_2", ItemMachineUpgrade.UpgradeType.POWER, 2, 0, "desc.upgrade3");
        registerUpgrade("upgrade_power_3", ItemMachineUpgrade.UpgradeType.POWER, 3, 0, "desc.upgrade3");
        registerUpgrade("upgrade_fortune_1", ItemMachineUpgrade.UpgradeType.FORTUNE, 1, 0, "desc.upgrade4");
        registerUpgrade("upgrade_fortune_2", ItemMachineUpgrade.UpgradeType.FORTUNE, 2, 0, "desc.upgrade4");
        registerUpgrade("upgrade_fortune_3", ItemMachineUpgrade.UpgradeType.FORTUNE, 3, 0, "desc.upgrade4");
        registerUpgrade("upgrade_afterburn_1", ItemMachineUpgrade.UpgradeType.AFTERBURN, 1, 0, "desc.upgrade5");
        registerUpgrade("upgrade_afterburn_2", ItemMachineUpgrade.UpgradeType.AFTERBURN, 2, 0, "desc.upgrade5");
        registerUpgrade("upgrade_afterburn_3", ItemMachineUpgrade.UpgradeType.AFTERBURN, 3, 0, "desc.upgrade5");
        registerUpgradeStack("upgrade_radius", "desc.upgrade7", "desc.upgraderd", "desc.upgradestack");
        registerUpgradeStack("upgrade_health", "desc.upgrade8", "desc.upgradeht", "desc.upgradestack");
        registerUpgrade("upgrade_overdrive_1", ItemMachineUpgrade.UpgradeType.OVERDRIVE, 1, 4, "desc.upgrade6");
        registerUpgrade("upgrade_overdrive_2", ItemMachineUpgrade.UpgradeType.OVERDRIVE, 2, 6, "desc.upgrade6");
        registerUpgrade("upgrade_overdrive_3", ItemMachineUpgrade.UpgradeType.OVERDRIVE, 3, 8, "desc.upgrade6");
        registerUpgrade("upgrade_smelter", ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, "desc.upgrade9", "desc.upgrade12");
        registerUpgrade("upgrade_shredder", ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, "desc.upgrade9", "desc.upgrade13");
        registerUpgrade("upgrade_centrifuge", ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, "desc.upgrade9", "desc.upgrade21");
        registerUpgrade("upgrade_crystallizer", ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, "desc.upgrade9", "desc.upgrade14");
        registerUpgrade("upgrade_nullifier", ItemMachineUpgrade.UpgradeType.NULLIFIER, 1, 0, "desc.upgrade10", "desc.upgrade19");
        registerUpgrade("upgrade_screm", ItemMachineUpgrade.UpgradeType.SCREAM, 1, 10, "desc.upgrade9", "desc.upgrade15", "desc.upgrade16", "desc.upgrade17");
        registerUpgrade("upgrade_gc_speed", ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, "desc.upgradegc");
        registerUpgrade("upgrade_5g", ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, "desc.upgrade5g");
        registerUpgrade("upgrade_ejector_1", ItemMachineUpgrade.UpgradeType.SPECIAL, 1, 0, "desc.upgradeej");
        registerUpgrade("upgrade_ejector_2", ItemMachineUpgrade.UpgradeType.SPECIAL, 2, 0, "desc.upgradeej");
        registerUpgrade("upgrade_ejector_3", ItemMachineUpgrade.UpgradeType.SPECIAL, 3, 0, "desc.upgradeej");
        registerUpgrade("upgrade_stack_1", ItemMachineUpgrade.UpgradeType.SPECIAL, 1, 0, "desc.upgradestk");
        registerUpgrade("upgrade_stack_2", ItemMachineUpgrade.UpgradeType.SPECIAL, 1, 0, "desc.upgradestk");
        registerUpgrade("upgrade_stack_3", ItemMachineUpgrade.UpgradeType.SPECIAL, 1, 0, "desc.upgradestk");
    }

    private static void registerUpgrade(String name, ItemMachineUpgrade.UpgradeType type, int tier, int speed, String... descKeys) {
        tab(ModCreativeTabs.CONTROL, reg(name, () -> new ItemMachineUpgrade(type, tier, speed, props().stacksTo(1), descKeys)));
    }

    private static void registerUpgradeStack(String name, String... descKeys) {
        tab(ModCreativeTabs.CONTROL, reg(name, () -> new ItemMachineUpgrade(ItemMachineUpgrade.UpgradeType.SPECIAL, 0, 0, props().stacksTo(16), descKeys)));
    }

    // ==================== ItemMold ====================

    private static void registerMold() {
        tab(ModCreativeTabs.TEMPLATE, reg("mold", ItemMold::new));
    }

    // ==================== ItemPACoil ====================

    private static void registerPaCoils() {
        // CE: single "pa_coil" field, 4 metadata grades.
        for (ItemPACoil.EnumCoilType type : ItemPACoil.EnumCoilType.VALUES) {
            tab(ModCreativeTabs.CONTROL, reg("pa_coil_" + lower(type.name()), () -> new ItemPACoil(type, props())));
        }
    }

    // ==================== ItemPileRod / ItemPileRodMK2 ====================

    private static void registerPileRods() {
        for (String name : new String[] {"uranium", "pu239", "plutonium", "source", "boron", "lithium", "detector"}) {
            tab(ModCreativeTabs.CONTROL, reg("pile_rod_" + name, () -> new ItemPileRod(props())));
        }

        for (ItemPileRodMK2.EnumPileRod type : ItemPileRodMK2.EnumPileRod.VALUES) {
            DeferredItem<Item> item = reg("pile_rod_mk2_" + lower(type.name()), () -> new ItemPileRodMK2(type, props()));
            PILE_RODS_MK2.put(type, item);
            tab(ModCreativeTabs.CONTROL, item);
        }
    }

    // ==================== ItemPistons ====================
    // CE: single "piston_set" field (ItemEnumMulti), 4 metadata grades.

    private static void registerPistons() {
        for (ItemPistons.EnumPistonType type : ItemPistons.EnumPistonType.VALUES) {
            DeferredItem<Item> item = reg("piston_set_" + lower(type.name()), () -> new ItemPistons(type, props().stacksTo(1)));
            PISTONS.put(type, item);
            tab(ModCreativeTabs.CONTROL, item);
        }
    }

    // ==================== ItemPlateFuel ====================

    private static void registerPlateFuel() {
        PLATE_FUEL_U233 = registerPlateFuel("plate_fuel_u233", 2200000, ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 50);
        PLATE_FUEL_U235 = registerPlateFuel("plate_fuel_u235", 2200000, ItemPlateFuel.FunctionEnum.SQUARE_ROOT, 40);
        PLATE_FUEL_MOX = registerPlateFuel("plate_fuel_mox", 2400000, ItemPlateFuel.FunctionEnum.LOGARITHM, 50);
        PLATE_FUEL_PU239 = registerPlateFuel("plate_fuel_pu239", 2000000, ItemPlateFuel.FunctionEnum.NEGATIVE_QUADRATIC, 50);
        PLATE_FUEL_SA326 = registerPlateFuel("plate_fuel_sa326", 2000000, ItemPlateFuel.FunctionEnum.LINEAR, 80);
        PLATE_FUEL_RA226BE = registerPlateFuel("plate_fuel_ra226be", 1300000, ItemPlateFuel.FunctionEnum.PASSIVE, 30);
        PLATE_FUEL_PU238BE = registerPlateFuel("plate_fuel_pu238be", 1000000, ItemPlateFuel.FunctionEnum.PASSIVE, 50);
    }

    private static DeferredItem<Item> registerPlateFuel(String name, int lifeTime, ItemPlateFuel.FunctionEnum function, int reactivity) {
        DeferredItem<Item> item = reg(name, () -> new ItemPlateFuel(lifeTime, function, reactivity, props().stacksTo(1)));
        tab(ModCreativeTabs.CONTROL, item);
        return item;
    }

    // ==================== ItemPWRFuel ====================
    // CE also registers pwr_fuel_hot/pwr_fuel_depleted as plain ItemEnumMulti byproduct markers
    // sharing this same enum - out of this class's own scope (not an ItemPWRFuel instance), so not
    // duplicated here; only the fresh fuel item (the actual ItemPWRFuel class) is registered.

    private static void registerPwrFuel() {
        for (ItemPWRFuel.EnumPWRFuel type : ItemPWRFuel.EnumPWRFuel.VALUES) {
            DeferredItem<Item> item = reg("pwr_fuel_" + lower(type.name()), () -> new ItemPWRFuel(type, props()));
            PWR_FUEL.put(type, item);
            tab(ModCreativeTabs.CONTROL, item);
        }
    }

    // ==================== ItemRBMKPellet ====================

    private static void registerRbmkPellets() {
        registerRbmkPellet("rbmk_pellet_zfb_bismuth", "Zirconium Fast Breeder - LEU/HEP-241 -> Bi");
        registerRbmkPellet("rbmk_pellet_zfb_pu241", "Zirconium Fast Breeder - HEU-235/HEP-240 -> Pu241");
        registerRbmkPellet("rbmk_pellet_zfb_am_mix", "Zirconium Fast Breeder - HEP-241 -> HEA");
        registerRbmkPellet("rbmk_pellet_ueu", "Unenriched Uranium");
        registerRbmkPellet("rbmk_pellet_meu", "Medium Enriched Uranium-235");
        registerRbmkPellet("rbmk_pellet_heu233", "Highly Enriched Uranium-233");
        registerRbmkPellet("rbmk_pellet_heu235", "Highly Enriched Uranium-235");
        registerRbmkPellet("rbmk_pellet_uzh", "Uranium Zirconium Hydride");
        registerRbmkPellet("rbmk_pellet_thmeu", "Thorium with MEU Driver Fuel");
        registerRbmkPellet("rbmk_pellet_lep", "Low Enriched Plutonium-239");
        registerRbmkPellet("rbmk_pellet_mep", "Medium Enriched Plutonium-239");
        registerRbmkPellet("rbmk_pellet_hep239", "Highly Enriched Plutonium-239");
        registerRbmkPellet("rbmk_pellet_hep241", "Highly Enriched Plutonium-241");
        registerRbmkPellet("rbmk_pellet_lea", "Low Enriched Americium-242");
        registerRbmkPellet("rbmk_pellet_mea", "Medium Enriched Americium-242");
        registerRbmkPellet("rbmk_pellet_hea241", "Highly Enriched Americium-241");
        registerRbmkPellet("rbmk_pellet_hea242", "Highly Enriched Americium-242");
        registerRbmkPellet("rbmk_pellet_men", "Medium Enriched Neptunium-237");
        registerRbmkPellet("rbmk_pellet_hen", "Highly Enriched Neptunium-237");
        registerRbmkPellet("rbmk_pellet_mox", "Mixed LEU & LEP Oxide");
        registerRbmkPellet("rbmk_pellet_les", "Low Enriched Schrabidium-326");
        registerRbmkPellet("rbmk_pellet_mes", "Medium Enriched Schrabidium-326");
        registerRbmkPellet("rbmk_pellet_hes", "Highly Enriched Schrabidium-326");
        registerRbmkPellet("rbmk_pellet_leaus", "Low Enriched Australium (Tasmanite)");
        registerRbmkPellet("rbmk_pellet_heaus", "Highly Enriched Australium (Ayerite)");
        registerRbmkPellet("rbmk_pellet_po210be", "Polonium-210 & Beryllium Neutron Source");
        registerRbmkPellet("rbmk_pellet_ra226be", "Radium-226 & Beryllium Neutron Source");
        registerRbmkPellet("rbmk_pellet_pu238be", "Plutonium-238 & Beryllium Neutron Source");
        registerRbmkPellet("rbmk_pellet_balefire_gold", "Antihydrogen in a Magnetized Gold-198 Lattice");
        registerRbmkPellet("rbmk_pellet_flashlead", "Antihydrogen confined by a Magnetized Gold-198 & Lead-209 Lattice");
        registerRbmkPellet("rbmk_pellet_balefire", "Draconic Flames");
        registerRbmkPellet("rbmk_pellet_drx", net.minecraft.ChatFormatting.OBFUSCATED + "can't you hear, can't you hear the thunder?");
    }

    private static void registerRbmkPellet(String name, String fullName) {
        tab(ModCreativeTabs.CONTROL, reg(name, () -> new ItemRBMKPellet(fullName, props())));
    }

    // ==================== ItemRTGPellet ====================
    // Each decays into its own pellet_rtg_depleted_<material> byproduct (a plain Item, one per
    // EnumDepletedRTGMaterial - CE's ItemEnums.EnumDepletedRTGMaterial, already ported).

    private static final Map<com.hbm.items.ItemEnums.EnumDepletedRTGMaterial, DeferredItem<Item>> RTG_DEPLETED = new EnumMap<>(com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.class);

    private static void registerRtgPellets() {
        for (com.hbm.items.ItemEnums.EnumDepletedRTGMaterial mat : com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.VALUES) {
            RTG_DEPLETED.put(mat, tab(ModCreativeTabs.CONTROL, reg("pellet_rtg_depleted_" + lower(mat.name()), () -> new Item(props()))));
        }

        PELLET_RTG = registerRtgPellet("pellet_rtg", 10, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.LEAD, 768252000L, 2);
        PELLET_RTG_RADIUM = registerRtgPellet("pellet_rtg_radium", 3, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.LEAD, 14016000000L, 1);
        PELLET_RTG_WEAK = registerRtgPellet("pellet_rtg_weak", 5, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.LEAD, 876000000L, 2);
        PELLET_RTG_STRONTIUM = registerRtgPellet("pellet_rtg_strontium", 12, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.ZIRCONIUM, 252200400L, 2);
        PELLET_RTG_COBALT = registerRtgPellet("pellet_rtg_cobalt", 16, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.ZIRCONIUM, 46176588L, 2);
        PELLET_RTG_ACTINIUM = registerRtgPellet("pellet_rtg_actinium", 20, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.LEAD, 190705200L, 2);
        PELLET_RTG_AMERICIUM = registerRtgPellet("pellet_rtg_americium", 25, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.NEPTUNIUM, 3786072000L, 2);
        PELLET_RTG_POLONIUM = registerRtgPellet("pellet_rtg_polonium", 50, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.LEAD, 3321024L, 3);
        PELLET_RTG_GOLD = registerRtgPellet("pellet_rtg_gold", 200, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.MERCURY, 64728L, 4);
        PELLET_RTG_LEAD = registerRtgPellet("pellet_rtg_lead", 600, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.BISMUTH, 3253L, 6);
        PELLET_RTG_BALEFIRE = registerRtgPellet("pellet_rtg_balefire", 6000, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial.NEPTUNIUM, 1000L, 12);
    }

    private static DeferredItem<Item> registerRtgPellet(String name, int heat, com.hbm.items.ItemEnums.EnumDepletedRTGMaterial decaysInto, long halflife, int halflifes) {
        Supplier<? extends Item> decayItem = RTG_DEPLETED.get(decaysInto);
        DeferredItem<Item> item = reg(name, () -> new ItemRTGPellet(heat, decayItem, halflife, halflifes, props()));
        tab(ModCreativeTabs.CONTROL, item);
        return item;
    }

    // ==================== ItemSatChip / ItemSatellite ====================

    private static void registerSatChipsAndSatellites() {
        registerSatChip("sat_mapper", "satchip.mapper", null);
        registerSatChip("sat_scanner", "satchip.scanner", null);
        registerSatChip("sat_radar", "satchip.radar", null);
        registerSatChip("sat_laser", "satchip.laser", null);
        registerSatChip("sat_foeq", "satchip.foeq", null);
        registerSatChip("sat_resonator", "satchip.resonator", null);
        registerSatChip("sat_miner", "satchip.miner", null);
        registerSatChip("sat_lunar_miner", "satchip.lunar_miner", null);
        registerSatChip("sat_gerald", "satchip.gerald", ModCreativeTabs.MISSILE);
        registerSatChip("sat_chip", "satchip.generic", ModCreativeTabs.MISSILE);
        registerSatChip("sat_relay", "satchip.foeq", ModCreativeTabs.MISSILE);

        for (ItemSatellite.EnumSatType type : ItemSatellite.EnumSatType.VALUES) {
            tab(ModCreativeTabs.MISSILE, reg("satellite_" + lower(type.name()), () -> new ItemSatellite(type, props().stacksTo(1))));
        }
    }

    private static void registerSatChip(String name, String descKey, ResourceKey<CreativeModeTab> tab) {
        DeferredItem<Item> item = reg(name, () -> new ItemSatChip(descKey, props().stacksTo(1)));
        if (tab != null) tab(tab, item);
    }

    // ==================== ItemScraps ====================
    // CE: single "scraps" field, one metadata variant per smeltable/additive NTMMaterial.

    private static void registerScraps() {
        for (NTMMaterial material : Mats.orderedList) {
            if (!ItemScraps.isScrappable(material)) continue;
            tab(ModCreativeTabs.PARTS, reg("scraps_" + material.getRegistryName(), () -> new ItemScraps(material, props())));
        }
    }

    // ==================== ItemStamp / ItemStampBook ====================

    private static void registerStamps() {
        registerStamp("stamp_stone_flat", 32, ItemStamp.StampType.FLAT);
        registerStamp("stamp_stone_plate", 32, ItemStamp.StampType.PLATE);
        registerStamp("stamp_stone_wire", 32, ItemStamp.StampType.WIRE);
        registerStamp("stamp_stone_circuit", 32, ItemStamp.StampType.CIRCUIT);
        registerStamp("stamp_iron_flat", 64, ItemStamp.StampType.FLAT);
        registerStamp("stamp_iron_plate", 64, ItemStamp.StampType.PLATE);
        registerStamp("stamp_iron_wire", 64, ItemStamp.StampType.WIRE);
        registerStamp("stamp_iron_circuit", 64, ItemStamp.StampType.CIRCUIT);
        registerStamp("stamp_steel_flat", 192, ItemStamp.StampType.FLAT);
        registerStamp("stamp_steel_plate", 192, ItemStamp.StampType.PLATE);
        registerStamp("stamp_steel_wire", 192, ItemStamp.StampType.WIRE);
        registerStamp("stamp_steel_circuit", 192, ItemStamp.StampType.CIRCUIT);
        registerStamp("stamp_titanium_flat", 256, ItemStamp.StampType.FLAT);
        registerStamp("stamp_titanium_plate", 256, ItemStamp.StampType.PLATE);
        registerStamp("stamp_titanium_wire", 256, ItemStamp.StampType.WIRE);
        registerStamp("stamp_titanium_circuit", 256, ItemStamp.StampType.CIRCUIT);
        registerStamp("stamp_obsidian_flat", 512, ItemStamp.StampType.FLAT);
        registerStamp("stamp_obsidian_plate", 512, ItemStamp.StampType.PLATE);
        registerStamp("stamp_obsidian_wire", 512, ItemStamp.StampType.WIRE);
        registerStamp("stamp_obsidian_circuit", 512, ItemStamp.StampType.CIRCUIT);
        registerStamp("stamp_desh_flat", 0, ItemStamp.StampType.FLAT);
        registerStamp("stamp_desh_plate", 0, ItemStamp.StampType.PLATE);
        registerStamp("stamp_desh_wire", 0, ItemStamp.StampType.WIRE);
        registerStamp("stamp_desh_circuit", 0, ItemStamp.StampType.CIRCUIT);
        registerStamp("stamp_desh_357", 0, ItemStamp.StampType.C357);
        registerStamp("stamp_desh_44", 0, ItemStamp.StampType.C44);
        registerStamp("stamp_desh_9", 0, ItemStamp.StampType.C9);
        registerStamp("stamp_desh_50", 0, ItemStamp.StampType.C50);
        registerStamp("stamp_357", 1000, ItemStamp.StampType.C357);
        registerStamp("stamp_44", 1000, ItemStamp.StampType.C44);
        registerStamp("stamp_9", 1000, ItemStamp.StampType.C9);
        registerStamp("stamp_50", 1000, ItemStamp.StampType.C50);

        for (int i = 0; i < 8; i++) {
            ItemStamp.StampType type = ItemStamp.StampType.values()[ItemStamp.StampType.PRINTING1.ordinal() + i];
            // CE: stamp_book is hidden from creative (setCreativeTab(null)) despite ItemStamp's own
            // constructor defaulting to CONTROL - not added to any CreativeTabContents tab here.
            reg("stamp_book_" + lower(type.name()), () -> new ItemStampBook(type, props().stacksTo(1)));
        }
    }

    private static void registerStamp(String name, int durability, ItemStamp.StampType type) {
        Item.Properties properties = props().stacksTo(1);
        if (durability > 0) properties = properties.durability(durability);
        Item.Properties finalProperties = properties;
        tab(ModCreativeTabs.CONTROL, reg(name, () -> new ItemStamp(type, finalProperties)));
    }

    // ==================== ItemTurretBiometry / ItemTurretChip ====================

    private static void registerTurretItems() {
        tab(ModCreativeTabs.WEAPON, reg("turret_chip", () -> new ItemTurretChip(props().stacksTo(1))));
        tab(ModCreativeTabs.WEAPON, reg("turret_biometry", () -> new ItemTurretBiometry(props().stacksTo(1))));
    }

    // ==================== ItemWatzPellet ====================

    private static void registerWatzPellets() {
        for (ItemWatzPellet.EnumWatzType type : ItemWatzPellet.EnumWatzType.VALUES) {
            String suffix = lower(type.name());
            DeferredItem<Item> fresh = reg("watz_pellet_" + suffix, () -> new ItemWatzPellet(type, false, props()));
            DeferredItem<Item> depleted = reg("watz_pellet_depleted_" + suffix, () -> new ItemWatzPellet(type, true, props()));
            WATZ_PELLET.put(type, fresh);
            WATZ_PELLET_DEPLETED.put(type, depleted);
            tab(ModCreativeTabs.CONTROL, fresh);
        }
    }

    // ==================== ItemZirnoxRod / ItemZirnoxRodDepleted ====================

    private static void registerZirnoxRods() {
        for (ItemZirnoxRod.EnumZirnoxType type : ItemZirnoxRod.EnumZirnoxType.VALUES) {
            DeferredItem<Item> item = reg("rod_zirnox_" + lower(type.name()), () -> new ItemZirnoxRod(type, props()));
            ZIRNOX_RODS.put(type, item);
            tab(ModCreativeTabs.CONTROL, item);
        }
        for (ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted type : ItemZirnoxRodDepleted.EnumZirnoxTypeDepleted.VALUES) {
            DeferredItem<Item> item = reg("rod_zirnox_depleted_" + lower(type.name()), () -> new ItemZirnoxRodDepleted(type, props()));
            ZIRNOX_RODS_DEPLETED.put(type, item);
            tab(ModCreativeTabs.CONTROL, item);
        }
    }

    // ==================== shared helpers ====================

    private static Item.Properties props() {
        return new Item.Properties();
    }

    private static DeferredItem<Item> reg(String name, Supplier<? extends Item> factory) {
        return ModItems.ITEMS.register(name, factory);
    }

    private static DeferredItem<Item> tab(ResourceKey<CreativeModeTab> tab, DeferredItem<Item> item) {
        CreativeTabContents.add(tab, item);
        return item;
    }

    private static String lower(String enumName) {
        return enumName.toLowerCase(Locale.ROOT);
    }
}
