package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.ItemEnums.EnumAshType;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.items.machine.Phase11ProcessItems;
import com.hbm.items.special.ItemWasteLong;
import com.hbm.items.special.ItemWasteShort;
import com.hbm.items.special.SpecialItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.WeightedRandomObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.SILEXRecipes} - laser isotope/element
 * separation (Separation of Isotopes by Laser EXcitation). See
 * {@code docs/phase2/machines_chemical_isotope.md}'s "core isotope-separation formula to preserve
 * exactly" - that formula lives on {@link com.hbm.blockentity.machine.chem.SilexBlockEntity}, not
 * here; this class carries the {@link SILEXRecipe} data shape (material-charge cost, minimum laser
 * wavelength, weighted-random output pool) verbatim.
 * <p>
 * <b>Scope trim</b> (documented, same shape as {@code CentrifugeRecipes}; extended by
 * {@code mrec-03-silex-misc}, see {@code docs/phase7/mrec_03_silex_misc.md}): CE registers 295
 * recipes total. This class now carries all 13 of CE's pre-loop static entries (uranium/plutonium/
 * americium/schrabidium/australium-ore reprocessing, {@code ore_tikite}/{@code crystal_trixite},
 * the lapis-dye breakdown, and the gravel breakdown) plus 12 of CE's 24 post-loop nuclear-waste-
 * reprocessing entries ({@code nuclear_waste_tiny} unblocked the leftover 10) - preserving CE's exact {@code fluidProduced}/{@code fluidConsumed}/
 * {@code laserStrength}/output-weight numbers for every recipe it does carry.
 * {@code ModItems.sulfur} substitutes {@link PlateCrystalWasteItems#CRYSTAL_SULFUR} (same
 * substitution {@code RefineryRecipes} already documented) and {@code ModItems.fluorite} substitutes
 * {@link PlateCrystalWasteItems#CRYSTAL_FLUORITE} (see {@link GasCentrifugeRecipes}'s header for the
 * same substitution). CE's {@code Items.DYE} meta 4 (lapis dye) key is ported as
 * {@link Items#LAPIS_LAZULI} - a previous pass of this file mistakenly keyed that entry on
 * {@link Items#DIAMOND} instead (no substitution comment, unlike every other deliberate one in this
 * file - flagged by {@code mrec_03_silex_misc.md} open question #2 as a bug, corrected here).
 * <p>
 * RBMK pellet keys use restored {@link ComparableStack#meta} = {@code ItemRBMKPellet} stage 0–9
 * (CE {@code SILEXRecipes.java:117-472}). {@code fluid_icon} keys use meta = fluid id
 * ({@code :96-115}, {@code :664}). DRX still skipped — {@code ModItems.undefined} is not registered.
 */
public final class SILEXRecipes {

    public static final Map<ComparableStack, SILEXRecipe> RECIPES = new LinkedHashMap<>();
    private static final Map<Item, Item> TINY_WASTE = new HashMap<>();

    private static boolean registered = false;

    private SILEXRecipes() {
    }

    /**
     * Resolves one of this port's own blocks by registry name, matching
     * {@code CrystallizerRecipes#hbmBlock(String)}'s already-established lazy-lookup pattern (see
     * that method's own javadoc for the full safety reasoning) - safe here only because
     * {@link #register()} itself only ever runs from {@code CommonEvents}/{@code SilexCategory},
     * both strictly after every block {@code RegisterEvent} has fired (see {@code CommonEvents}'s own
     * comment on why the whole recipe-table block it lives in was moved out of
     * {@code MainRegistry}'s constructor).
     */
    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static Item hbmItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static ComparableStack pellet(String id, int meta) {
        return new ComparableStack(hbmItem(id), 1, meta);
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_URANIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 1)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 11));

        // CE: new SILEXRecipe(900, 100, 2) - raw-int overload indexes EnumWavelengths.values()[2] =
        // VISIBLE (confirmed against both trees' identical enum ordinals - NULL=0,IR=1,VISIBLE=2,...).
        // A previous pass of this file mapped every CE `2`-argument entry in this group to IR
        // (ordinal 1) instead - corrected here across INGOT_PU_MIX/INGOT_AM_MIX/INGOT_SCHRARANIUM.
        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_PU_MIX.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 6)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 3));

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_AM_MIX.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM241.get()), 3)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM242.get()), 6));

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_SCHRARANIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get()), 4)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 3)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 2));

        // CE SILEXRecipes.java:50-54 PU.ingot() (dictTranslation dust→ingot already implied)
        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_PLUTONIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU238.get()), 3)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 4)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 2));

        // CE key: Items.DYE meta 4 (lapis dye) - 1.21.1 has no more meta variants, Items.LAPIS_LAZULI
        // is the direct equivalent. A previous pass of this file mis-keyed this entry on
        // Items.DIAMOND instead (see class javadoc, mrec_03_silex_misc.md open question #2).
        RECIPES.put(new ComparableStack(Items.LAPIS_LAZULI),
                new SILEXRecipe(100, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 4), 4)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_ALUMINIUM.get(), 3), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_COBALT.get(), 3), 3));

        RECIPES.put(new ComparableStack(Items.GRAVEL),
                new SILEXRecipe(1000, 250, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(Items.FLINT), 80)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_BORON.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_LITHIUM.get()), 10)
                        .addOut(new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get()), 5));

        // CE SILEXRecipes.java:96-115 fluid_icon DEATH/VITRIOL/REDMUD — meta = fluid id
        RECIPES.put(new ComparableStack(hbmItem("fluid_icon"), 1, Fluids.DEATH.getID()),
                new SILEXRecipe(1000, 1000, 4)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_IMPURE_OSMIRIDIUM.get()), 1));
        RECIPES.put(new ComparableStack(hbmItem("fluid_icon"), 1, Fluids.VITRIOL.getID()),
                new SILEXRecipe(1000, 300, EnumWavelengths.IR)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_BROMINE.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_IODINE.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_IRON.get()), 5)
                        .addOut(new ItemStack(hbmItem("sulfur")), 15));
        RECIPES.put(new ComparableStack(hbmItem("fluid_icon"), 1, Fluids.REDMUD.getID()),
                new SILEXRecipe(300, 50, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_ALUMINIUM.get()), 10)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NEODYMIUM_TINY.get(), 3), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_BORON_TINY.get(), 3), 5)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_IRON.get()), 20)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_TITANIUM.get()), 15)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SODIUM.get()), 10));

        // ==================== mrec-03-silex-misc additions ====================
        // The rest of CE's 13 pre-loop static entries (CE lines 62-87) whose ingredient AND output
        // items are all already registered in this port.

        // CE: recipes.put(new ComparableStack(ModItems.ingot_australium), new SILEXRecipe(900, 100, 2)...) - SILEXRecipes.java:63-66
        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_AUSTRALIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AUSTRALIUM_LESSER.get()), 5)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AUSTRALIUM_GREATER.get()), 1));

        // CE: recipes.put(new ComparableStack(ModItems.crystal_schraranium), ...) - SILEXRecipes.java:68-72
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SCHRARANIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.UV)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get()), 5)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 2)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 2));

        // CE: recipes.put(new ComparableStack(ModBlocks.ore_tikite), ...) - SILEXRecipes.java:74-79
        RECIPES.put(new ComparableStack(hbmBlock("ore_tikite")),
                new SILEXRecipe(900, 100, EnumWavelengths.UV)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_COBALT.get()), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NITAN_MIX.get()), 2));

        // CE: recipes.put(new ComparableStack(ModItems.crystal_trixite), ...) - SILEXRecipes.java:81-87
        RECIPES.put(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get()),
                new SILEXRecipe(1200, 100, EnumWavelengths.UV)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_PLUTONIUM.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_COBALT.get()), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NIOBIUM.get()), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NITAN_MIX.get()), 1)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SPARK_MIX.get()), 1));

        // CE lines 474-646: 2 long U235/U233 + 10 leftover non-depleted (tiny waste now registered).
        // CE: recipes.put(new ComparableStack(ModItems.nuclear_waste_long, 1, URANIUM235.ordinal()), ...) - SILEXRecipes.java:474-479
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLong(ItemWasteLong.WasteClass.URANIUM235).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 20)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 45)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 20)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_TECHNETIUM.get()), 15));

        // CE: recipes.put(new ComparableStack(ModItems.nuclear_waste_long, 1, URANIUM233.ordinal()), ...) - SILEXRecipes.java:501-506
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLong(ItemWasteLong.WasteClass.URANIUM233).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 15)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 25)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 45)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_TECHNETIUM.get()), 15));

        // leftover non-depleted waste — nuclear_waste_tiny now exists (Phase11ProcessItems)
        // CE SILEXRecipes.java:485 / :512 / :528 / :544 / :561 / :576 / :588 / :601 / :616 / :632
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.URANIUM235).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU238.get()), 12)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SR90_TINY.get()), 10)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 10)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 12)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 56));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.URANIUM233).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU238.get()), 4)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SR90_TINY.get()), 12)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 10)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 14)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 60));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.PLUTONIUM239).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 10)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 25)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SR90_TINY.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 6)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 52));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.PLUTONIUM240).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 15)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SR90_TINY.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 7)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 66));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.PLUTONIUM241).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM241.get()), 25)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM242.get()), 35)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_TECHNETIUM.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 7)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 25));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLong(ItemWasteLong.WasteClass.THORIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U233.get()), 40)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 35)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 25));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLong(ItemWasteLong.WasteClass.NEPTUNIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 15)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 40)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 15)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_TECHNETIUM.get()), 15)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 15));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.NEPTUNIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU238.get()), 40)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SR90_TINY.get()), 7)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 8)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 40));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLong(ItemWasteLong.WasteClass.SCHRABIDIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get()), 25)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get()), 18)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_GH336.get()), 16)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_TANTALIUM.get()), 8)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NEODYMIUM_TINY.get()), 8)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 25));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShort(ItemWasteShort.WasteClass.SCHRABIDIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PB209.get()), 7)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), 7)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 5)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 5)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 76));

        // CE SILEXRecipes.java:480-646 depleted keys — I/O now registered
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLongDepleted(ItemWasteLong.WasteClass.URANIUM235).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 65)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 20)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 15));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.URANIUM235).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 10)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 32)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 22)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 5)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 15)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 16));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLongDepleted(ItemWasteLong.WasteClass.URANIUM233).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 60)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 25)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 15));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.URANIUM233).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 12)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 34)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 13)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 2)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 10)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 29));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.PLUTONIUM239).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 16)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 40)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 3)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 39));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.PLUTONIUM240).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 22)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 20)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 17)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 3)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 36));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.PLUTONIUM241).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 60)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 20)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 15)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 5));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLongDepleted(ItemWasteLong.WasteClass.THORIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 35)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 40)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 15)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 10));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLongDepleted(ItemWasteLong.WasteClass.NEPTUNIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 16)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 55)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 20)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 9));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.NEPTUNIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 7)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 29)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 2)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 45)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 17));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteLongDepleted(ItemWasteLong.WasteClass.SCHRABIDIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get()), 20)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get()), 18)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_GH336.get()), 15)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_TANTALIUM.get()), 8)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_NEODYMIUM_TINY.get()), 8)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 31));
        RECIPES.put(new ComparableStack(SpecialItems.nuclearWasteShortDepleted(ItemWasteShort.WasteClass.SCHRABIDIUM).get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 7)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get()), 12)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CERIUM_TINY.get()), 14)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_LANTHANIUM_TINY.get()), 15)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 20)
                        .addOut(new ItemStack(Phase11ProcessItems.NUCLEAR_WASTE_TINY.get()), 32));

        // CE SILEXRecipes.java:648-655
        RECIPES.put(new ComparableStack(hbmBlock("fallout")),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(BilletPowderItems.DUST_TINY.get()), 90)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_CO60.get()), 2)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_SR90_TINY.get()), 3)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_I131_TINY.get()), 1)
                        .addOut(new ItemStack(BilletPowderItems.POWDER_CS137_TINY.get()), 3)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), 1));

        // CE SILEXRecipes.java:664
        RECIPES.put(new ComparableStack(hbmItem("fluid_icon"), 1, Fluids.FULLERENE.getID()),
                new SILEXRecipe(1_000, 1_000, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(BilletPowderItems.powderAsh(EnumAshType.FULLERENE).get()), 1));

        registerPelletLoop();

        for (ItemWasteLong.WasteClass c : ItemWasteLong.WasteClass.VALUES) {
            TINY_WASTE.put(SpecialItems.nuclearWasteLongTiny(c).get(), SpecialItems.nuclearWasteLong(c).get());
            TINY_WASTE.put(SpecialItems.nuclearWasteLongDepletedTiny(c).get(), SpecialItems.nuclearWasteLongDepleted(c).get());
        }
        for (ItemWasteShort.WasteClass c : ItemWasteShort.WasteClass.VALUES) {
            TINY_WASTE.put(SpecialItems.nuclearWasteShortTiny(c).get(), SpecialItems.nuclearWasteShort(c).get());
            TINY_WASTE.put(SpecialItems.nuclearWasteShortDepletedTiny(c).get(), SpecialItems.nuclearWasteShortDepleted(c).get());
        }
    }

    /**
     * CE {@code SILEXRecipes.java:117-472}. Keys are {@code ComparableStack(pellet, 1, stage)}.
     * DRX skipped — {@code ModItems.undefined} is not registered.
     */
    private static void registerPelletLoop() {
        Item xe = BilletPowderItems.POWDER_XE135_TINY.get();
        Item coalTiny = BilletPowderItems.POWDER_COAL_TINY.get();
        Item wasteTiny = Phase11ProcessItems.NUCLEAR_WASTE_TINY.get();
        Item u235l = SpecialItems.nuclearWasteLongTiny(ItemWasteLong.WasteClass.URANIUM235).get();
        Item u235s = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.URANIUM235).get();
        Item u233l = SpecialItems.nuclearWasteLongTiny(ItemWasteLong.WasteClass.URANIUM233).get();
        Item u233s = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.URANIUM233).get();
        Item thL = SpecialItems.nuclearWasteLongTiny(ItemWasteLong.WasteClass.THORIUM).get();
        Item npL = SpecialItems.nuclearWasteLongTiny(ItemWasteLong.WasteClass.NEPTUNIUM).get();
        Item npS = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.NEPTUNIUM).get();
        Item pu239s = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.PLUTONIUM239).get();
        Item pu240s = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.PLUTONIUM240).get();
        Item pu241s = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.PLUTONIUM241).get();
        Item schL = SpecialItems.nuclearWasteLongTiny(ItemWasteLong.WasteClass.SCHRABIDIUM).get();
        Item schS = SpecialItems.nuclearWasteShortTiny(ItemWasteShort.WasteClass.SCHRABIDIUM).get();

        for (int i = 0; i < 5; i++) {
            RECIPES.put(pellet("rbmk_pellet_ueu", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 86 - i * 11)
                    .addOut(i < 2 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 10 + i * 3)
                    .addOut(new ItemStack(u235l), 2 + 3 * i)
                    .addOut(new ItemStack(u235s), 2 + 5 * i));
            RECIPES.put(pellet("rbmk_pellet_ueu", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 86 - i * 11)
                    .addOut(i < 2 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 10 + i * 3)
                    .addOut(new ItemStack(u235l), 2 + 3 * i)
                    .addOut(new ItemStack(u235s), 1 + 5 * i));

            RECIPES.put(pellet("rbmk_pellet_meu", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM_FUEL.get()), 84 - i * 16)
                    .addOut(i < 1 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 4)
                    .addOut(new ItemStack(u235l), 4 + 5 * i)
                    .addOut(new ItemStack(u235s), 6 + 7 * i));
            RECIPES.put(pellet("rbmk_pellet_meu", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM_FUEL.get()), 83 - i * 16)
                    .addOut(i < 1 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 4)
                    .addOut(new ItemStack(u235l), 4 + 5 * i)
                    .addOut(new ItemStack(u235s), 6 + 7 * i));

            RECIPES.put(pellet("rbmk_pellet_heu233", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U233.get()), 90 - i * 20)
                    .addOut(new ItemStack(u233l), 4 + 8 * i)
                    .addOut(new ItemStack(u233s), 6 + 12 * i));
            RECIPES.put(pellet("rbmk_pellet_heu233", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U233.get()), 89 - i * 20)
                    .addOut(new ItemStack(u233l), 4 + 8 * i)
                    .addOut(new ItemStack(u233s), 6 + 12 * i));

            RECIPES.put(pellet("rbmk_pellet_heu235", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 90 - i * 20)
                    .addOut(new ItemStack(u235l), 4 + 8 * i)
                    .addOut(new ItemStack(u235s), 6 + 12 * i));
            RECIPES.put(pellet("rbmk_pellet_heu235", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 89 - i * 20)
                    .addOut(new ItemStack(u235l), 4 + 8 * i)
                    .addOut(new ItemStack(u235s), 6 + 12 * i));

            RECIPES.put(pellet("rbmk_pellet_uzh", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 75)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM_FUEL.get()), 20 - i * 4)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 3 + i * 3)
                    .addOut(new ItemStack(u235l), 1 + i)
                    .addOut(new ItemStack(u235s), 1 + i));
            RECIPES.put(pellet("rbmk_pellet_uzh", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 75)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM_FUEL.get()), 19 - i * 4)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 3 + i * 3)
                    .addOut(new ItemStack(u235l), 1 + i)
                    .addOut(new ItemStack(u235s), 1 + i));

            RECIPES.put(pellet("rbmk_pellet_thmeu", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_THORIUM_FUEL.get()), 84 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U233.get()), 6 + i * 4)
                    .addOut(new ItemStack(thL), 10 + 16 * i));
            RECIPES.put(pellet("rbmk_pellet_thmeu", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_THORIUM_FUEL.get()), 83 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U233.get()), 6 + i * 4)
                    .addOut(new ItemStack(thL), 10 + 16 * i));

            RECIPES.put(pellet("rbmk_pellet_lep", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PLUTONIUM_FUEL.get()), 84 - i * 14)
                    .addOut(i < 1 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 2)
                    .addOut(new ItemStack(pu239s), 7 + 8 * i)
                    .addOut(new ItemStack(pu240s), 3 + 4 * i));
            RECIPES.put(pellet("rbmk_pellet_lep", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PLUTONIUM_FUEL.get()), 83 - i * 14)
                    .addOut(i < 1 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 2)
                    .addOut(new ItemStack(pu239s), 7 + 8 * i)
                    .addOut(new ItemStack(pu240s), 3 + 4 * i));

            RECIPES.put(pellet("rbmk_pellet_mep", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 85 - i * 20)
                    .addOut(new ItemStack(pu239s), 10 + 10 * i)
                    .addOut(new ItemStack(pu240s), 5 + 5 * i));
            RECIPES.put(pellet("rbmk_pellet_mep", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 84 - i * 20)
                    .addOut(new ItemStack(pu239s), 10 + 10 * i)
                    .addOut(new ItemStack(pu240s), 5 + 5 * i));

            RECIPES.put(pellet("rbmk_pellet_hep239", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 85 - i * 20)
                    .addOut(new ItemStack(pu239s), 15 + 20 * i));
            RECIPES.put(pellet("rbmk_pellet_hep239", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 84 - i * 20)
                    .addOut(new ItemStack(pu239s), 15 + 20 * i));

            RECIPES.put(pellet("rbmk_pellet_hep241", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 85 - i * 20)
                    .addOut(new ItemStack(pu241s), 15 + 20 * i));
            RECIPES.put(pellet("rbmk_pellet_hep241", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 84 - i * 20)
                    .addOut(new ItemStack(pu241s), 15 + 20 * i));

            RECIPES.put(pellet("rbmk_pellet_men", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM_FUEL.get()), 84 - i * 14)
                    .addOut(i < 1 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 2)
                    .addOut(new ItemStack(npL), 4 + 5 * i)
                    .addOut(new ItemStack(npS), 6 + 7 * i));
            RECIPES.put(pellet("rbmk_pellet_men", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM_FUEL.get()), 83 - i * 14)
                    .addOut(i < 1 ? new ItemStack(IngotNuggetItems.NUGGET_PU239.get()) : new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 2)
                    .addOut(new ItemStack(npL), 4 + 5 * i)
                    .addOut(new ItemStack(npS), 6 + 7 * i));

            RECIPES.put(pellet("rbmk_pellet_hen", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 4 + 8 * i)
                    .addOut(new ItemStack(npS), 6 + 12 * i));
            RECIPES.put(pellet("rbmk_pellet_hen", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 89 - i * 20)
                    .addOut(new ItemStack(npL), 4 + 8 * i)
                    .addOut(new ItemStack(npS), 6 + 12 * i));

            RECIPES.put(pellet("rbmk_pellet_mox", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_MOX_FUEL.get()), 84 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 4)
                    .addOut(new ItemStack(u235l), 2 + 3 * i)
                    .addOut(new ItemStack(u235s), 3 + 5 * i)
                    .addOut(new ItemStack(pu239s), 5 + 8 * i));
            RECIPES.put(pellet("rbmk_pellet_mox", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_MOX_FUEL.get()), 83 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU_MIX.get()), 6 + i * 4)
                    .addOut(new ItemStack(u235l), 2 + 3 * i)
                    .addOut(new ItemStack(u235s), 3 + 5 * i)
                    .addOut(new ItemStack(pu239s), 5 + 8 * i));

            RECIPES.put(pellet("rbmk_pellet_leaus", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AUSTRALIUM_LESSER.get()), 90 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 6 + 12 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PB209.get()), 4 + 8 * i));
            RECIPES.put(pellet("rbmk_pellet_leaus", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AUSTRALIUM_LESSER.get()), 89 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 6 + 12 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PB209.get()), 4 + 8 * i));

            RECIPES.put(pellet("rbmk_pellet_heaus", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AUSTRALIUM_GREATER.get()), 90 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), 5 + 10 * i)
                    .addOut(new ItemStack(Items.GOLD_NUGGET), 3 + 6 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PB209.get()), 2 + 4 * i));
            RECIPES.put(pellet("rbmk_pellet_heaus", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AUSTRALIUM_GREATER.get()), 89 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), 5 + 10 * i)
                    .addOut(new ItemStack(Items.GOLD_NUGGET), 3 + 6 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PB209.get()), 2 + 4 * i));

            RECIPES.put(pellet("rbmk_pellet_les", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LES.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 2 + 3 * i)
                    .addOut(new ItemStack(npS), 2 + 5 * i)
                    .addOut(new ItemStack(schL), 1 + 2 * i)
                    .addOut(new ItemStack(schS), 1 + 2 * i)
                    .addOut(new ItemStack(coalTiny), 4 + 8 * i));
            RECIPES.put(pellet("rbmk_pellet_les", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LES.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 2 + 3 * i)
                    .addOut(new ItemStack(npS), 2 + 5 * i)
                    .addOut(new ItemStack(schL), 1 + 2 * i)
                    .addOut(new ItemStack(schS), 1 + 2 * i)
                    .addOut(new ItemStack(coalTiny), 4 + 8 * i));

            RECIPES.put(pellet("rbmk_pellet_mes", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM_FUEL.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 1 + 3 * i)
                    .addOut(new ItemStack(npS), 2 + 4 * i)
                    .addOut(new ItemStack(schL), 1 + 3 * i)
                    .addOut(new ItemStack(schS), 2 + 4 * i)
                    .addOut(new ItemStack(coalTiny), 4 + 6 * i));
            RECIPES.put(pellet("rbmk_pellet_mes", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM_FUEL.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 1 + 3 * i)
                    .addOut(new ItemStack(npS), 2 + 4 * i)
                    .addOut(new ItemStack(schL), 1 + 3 * i)
                    .addOut(new ItemStack(schS), 2 + 4 * i)
                    .addOut(new ItemStack(coalTiny), 4 + 6 * i));

            RECIPES.put(pellet("rbmk_pellet_hes", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_HES.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 1 + 2 * i)
                    .addOut(new ItemStack(npS), 1 + 3 * i)
                    .addOut(new ItemStack(schL), 2 + 5 * i)
                    .addOut(new ItemStack(schS), 4 + 6 * i)
                    .addOut(new ItemStack(coalTiny), 2 + 4 * i));
            RECIPES.put(pellet("rbmk_pellet_hes", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_HES.get()), 90 - i * 20)
                    .addOut(new ItemStack(npL), 1 + 2 * i)
                    .addOut(new ItemStack(npS), 1 + 3 * i)
                    .addOut(new ItemStack(schL), 2 + 5 * i)
                    .addOut(new ItemStack(schS), 4 + 6 * i)
                    .addOut(new ItemStack(coalTiny), 2 + 4 * i));

            RECIPES.put(pellet("rbmk_pellet_balefire", i), new SILEXRecipe(400, 100, 3)
                    .addOut(new ItemStack(BilletPowderItems.POWDER_BALEFIRE.get()), 90 - i * 20)
                    .addOut(new ItemStack(wasteTiny), 10 + 20 * i));

            RECIPES.put(pellet("rbmk_pellet_balefire_gold", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), 90 - 20 * i)
                    .addOut(new ItemStack(BilletPowderItems.POWDER_BALEFIRE.get()), 10 + 20 * i));

            RECIPES.put(pellet("rbmk_pellet_flashlead", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AU198.get()), 44 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PB209.get()), 44 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 1 + 6 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get()), 1 + 6 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_GH336.get()), 10 + 8 * i));

            RECIPES.put(pellet("rbmk_pellet_po210be", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_POLONIUM.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BERYLLIUM.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 5 + 10 * i)
                    .addOut(new ItemStack(coalTiny), 5 + 10 * i));

            RECIPES.put(pellet("rbmk_pellet_pu238be", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU238.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BERYLLIUM.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 3 + 5 * i)
                    .addOut(new ItemStack(wasteTiny), 2 + 5 * i)
                    .addOut(new ItemStack(coalTiny), 5 + 10 * i));
            RECIPES.put(pellet("rbmk_pellet_pu238be", i + 5), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(xe), 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU238.get()), 44 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BERYLLIUM.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 3 + 5 * i)
                    .addOut(new ItemStack(wasteTiny), 2 + 5 * i)
                    .addOut(new ItemStack(coalTiny), 5 + 10 * i));

            RECIPES.put(pellet("rbmk_pellet_ra226be", i), new SILEXRecipe(600, 100, 1)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_RA226.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BERYLLIUM.get()), 45 - 10 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_LEAD.get()), 3 + 5 * i)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_POLONIUM.get()), 2 + 5 * i)
                    .addOut(new ItemStack(coalTiny), 5 + 10 * i));

            // TODO(CE: com.hbm.inventory.recipes.SILEXRecipes.java:417-431): DRX pellet
            // outputs ModItems.undefined ×6 — not registered. Do not invent.

            RECIPES.put(pellet("rbmk_pellet_zfb_bismuth", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 50 + i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 150));
            RECIPES.put(pellet("rbmk_pellet_zfb_bismuth", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(xe), 3)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get()), 50 + i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 147));

            RECIPES.put(pellet("rbmk_pellet_zfb_pu241", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 50 + i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 150));
            RECIPES.put(pellet("rbmk_pellet_zfb_pu241", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(xe), 3)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 50 - i * 10)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 50 + i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 147));

            RECIPES.put(pellet("rbmk_pellet_zfb_am_mix", i), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 100 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM_MIX.get()), 50 + i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 150));
            RECIPES.put(pellet("rbmk_pellet_zfb_am_mix", i + 5), new SILEXRecipe(600, 100, 2)
                    .addOut(new ItemStack(xe), 3)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU241.get()), 100 - i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM_MIX.get()), 50 + i * 20)
                    .addOut(new ItemStack(IngotNuggetItems.NUGGET_ZIRCONIUM.get()), 147));
        }
    }

    public static SILEXRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        register();
        ComparableStack key = new ComparableStack(stack).makeSingular();
        SILEXRecipe direct = RECIPES.get(key);
        if (direct != null) return direct;
        // CE SILEXRecipes.java:707-721 tinyWasteTranslation
        Item full = TINY_WASTE.get(stack.getItem());
        if (full == null) return null;
        SILEXRecipe result = getOutput(new ItemStack(full));
        if (result == null) return null;
        int fluidProduced = (result.fluidProduced / 900) * 100;
        SILEXRecipe tiny = new SILEXRecipe(fluidProduced, result.fluidConsumed, result.laserStrength);
        tiny.outputs.addAll(result.outputs);
        return tiny;
    }

    /**
     * Ported verbatim from CE's inner {@code SILEXRecipes.SILEXRecipe} - {@code fluidProduced} is the
     * "material charge" the recipe adds to the machine's internal 0-16000 counter when the input
     * item is consumed, {@code fluidConsumed} is the charge one separation cycle spends,
     * {@code laserStrength} the minimum {@link EnumWavelengths} required, and {@code outputs} a
     * weighted-random pool resolved once per completed cycle (not all outputs deterministically).
     */
    public static final class SILEXRecipe {

        public final int fluidProduced;
        public final int fluidConsumed;
        public final EnumWavelengths laserStrength;
        public final List<WeightedRandomObject> outputs = new ArrayList<>();

        public SILEXRecipe(int fluidProduced, int fluidConsumed, EnumWavelengths laserStrength) {
            this.fluidProduced = fluidProduced;
            this.fluidConsumed = fluidConsumed;
            this.laserStrength = laserStrength;
        }

        /** CE {@code SILEXRecipe(int, int, int)} — indexes {@link EnumWavelengths#values()}. */
        public SILEXRecipe(int fluidProduced, int fluidConsumed, int wavelength) {
            this(fluidProduced, fluidConsumed, EnumWavelengths.values()[wavelength]);
        }

        public SILEXRecipe addOut(ItemStack stack, int weight) {
            outputs.add(new WeightedRandomObject(stack, weight));
            return this;
        }
    }
}
