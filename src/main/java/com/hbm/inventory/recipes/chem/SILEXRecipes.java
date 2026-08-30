package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.machine.ItemFELCrystal.EnumWavelengths;
import com.hbm.util.WeightedRandomObject;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
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
 * <b>Scope trim</b> (documented, same shape as {@code CentrifugeRecipes}): CE registers ~140
 * recipes, the overwhelming majority keyed against RBMK spent-fuel-pellet items and waste-processing
 * items not yet ported in this pass. This class ports a representative real subset - uranium/
 * plutonium/americium/schrabidium-ore reprocessing plus the gravel breakdown recipe - preserving
 * CE's exact {@code fluidProduced}/{@code fluidConsumed}/{@code laserStrength}/output-weight numbers
 * for every recipe it does carry. {@code ModItems.sulfur} substitutes
 * {@link PlateCrystalWasteItems#CRYSTAL_SULFUR} (not yet a plain item in this port, same substitution
 * {@code RefineryRecipes} already documented) and {@code ModItems.fluorite} substitutes
 * {@link PlateCrystalWasteItems#CRYSTAL_FLUORITE} (see {@link GasCentrifugeRecipes}'s header for the
 * same substitution).
 */
public final class SILEXRecipes {

    public static final Map<ComparableStack, SILEXRecipe> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private SILEXRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_URANIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.VISIBLE)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U235.get()), 1)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_U238.get()), 11));

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_PU_MIX.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU239.get()), 6)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_PU240.get()), 3));

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_AM_MIX.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM241.get()), 3)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_AM242.get()), 6));

        RECIPES.put(new ComparableStack(IngotNuggetItems.INGOT_SCHRARANIUM.get()),
                new SILEXRecipe(900, 100, EnumWavelengths.IR)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get()), 4)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get()), 3)
                        .addOut(new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get()), 2));

        RECIPES.put(new ComparableStack(Items.DIAMOND),
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
    }

    public static SILEXRecipe getOutput(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        return RECIPES.get(new ComparableStack(stack).makeSingular());
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
