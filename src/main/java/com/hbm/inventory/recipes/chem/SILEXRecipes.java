package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.BilletPowderItems;
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
 * <b>Still not ported, and why</b> (see the research report's "Item/registry dependency check" for
 * the full citation trail):
 * <ul>
 *   <li>The entire 255-entry RBMK-pellet reprocessing loop (CE lines 117-472) - every one of its
 *   entries outputs {@code nuclear_waste_long_tiny}/{@code nuclear_waste_short_tiny}, neither of
 *   which is registered in this port ({@link SpecialItems} only carries the base
 *   {@code nuclear_waste_long}/{@code nuclear_waste_short} families, see that class's own
 *   "task-scoped families" comment). The loop's keys would also need this class's
 *   {@link #RECIPES} map widened from {@code ComparableStack} to {@code AStack} (to hold an
 *   {@code NbtComparableStack} keyed on the pellet's burnup-stage data component) - not attempted
 *   here since nothing below needs it.</li>
 *   <li>12 of the 24 post-loop waste-reprocessing entries key on
 *   {@code nuclear_waste_long_depleted}/{@code nuclear_waste_short_depleted}, also not registered.</li>
 *   <li>Depleted waste + {@code fallout} + {@code dust_tiny} are registered this wave.</li>
 *   <li>{@code fluid_icon}(DEATH/VITRIOL/REDMUD) - every ingredient/output item these 3 entries need
 *   <i>is</i> already registered, but reaching them at runtime needs a real fluid-tank-direct
 *   reprocessing path {@link com.hbm.blockentity.machine.chem.SilexBlockEntity} does not have yet
 *   (its own javadoc documents this gap), plus a key-collision-safe {@code NbtComparableStack}
 *   design (three different fluids all synthesize the same base {@code fluid_icon} {@link Item}, so
 *   a plain {@link ComparableStack} key cannot tell them apart, and a naive exact-data-component
 *   {@code NbtComparableStack} key would also need to ignore the tank's current fill amount, which
 *   is not an established pattern anywhere in this codebase yet) - genuine machine-logic design work,
 *   not a pure data addition, so deliberately left as a follow-up rather than guessed here.</li>
 *   <li>The DRX pellet's 6-output "mystery box" joke recipe needs {@code ModItems.undefined}
 *   (CE's {@code ItemCustomLore} troll placeholder), not registered - low priority, part of the
 *   blocked RBMK loop anyway.</li>
 * </ul>
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

        // TODO(CE: com.hbm.inventory.recipes.SILEXRecipes.java:117-472): RBMK pellet loop keys
        // pellet meta 0-4 / 5-9 (xenon). Port ComparableStack dropped meta; MachineDataComponents.RBMK_PELLET_STAGE
        // exists but shipping one recipe for all stages is unfaithful. Do not invent. Blocked by NbtComparableStack.
        // TODO(CE: com.hbm.inventory.recipes.SILEXRecipes.java:96-115): fluid_icon DEATH/VITRIOL/REDMUD —
        // same fluid_icon item, need NbtComparableStack ignoring fill + SILEX tank-direct path. Do not invent.
        // TODO(CE: com.hbm.inventory.recipes.SILEXRecipes.java:664): fluid_icon FULLERENE → powder_ash FULLERENE.
        // powder_ash_fullerene is registered; blocked by the same fluid_icon key. Do not invent.

        for (ItemWasteLong.WasteClass c : ItemWasteLong.WasteClass.VALUES) {
            TINY_WASTE.put(SpecialItems.nuclearWasteLongTiny(c).get(), SpecialItems.nuclearWasteLong(c).get());
            TINY_WASTE.put(SpecialItems.nuclearWasteLongDepletedTiny(c).get(), SpecialItems.nuclearWasteLongDepleted(c).get());
        }
        for (ItemWasteShort.WasteClass c : ItemWasteShort.WasteClass.VALUES) {
            TINY_WASTE.put(SpecialItems.nuclearWasteShortTiny(c).get(), SpecialItems.nuclearWasteShort(c).get());
            TINY_WASTE.put(SpecialItems.nuclearWasteShortDepletedTiny(c).get(), SpecialItems.nuclearWasteShortDepleted(c).get());
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

        public SILEXRecipe addOut(ItemStack stack, int weight) {
            outputs.add(new WeightedRandomObject(stack, weight));
            return this;
        }
    }
}
