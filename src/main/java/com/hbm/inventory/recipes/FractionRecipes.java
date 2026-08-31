package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.FractionRecipes} (104 lines, read in full;
 * see {@code docs/phase7/mrec_14_annihilator_misc.md}) - the Fraction Tower's recipe table: a
 * {@code Map<FluidType, Tuple.Pair<FluidStack, FluidStack>>} keyed by input fluid, each entry a
 * fixed-percentage 2-output split (CE's own {@code getComment()}: "Inputs are always 100mB, set
 * output quantities accordingly"). Structurally a near-twin of this port's own
 * {@link RefineryRecipes} (same lazy-{@code register()}-with-guard shape, same
 * {@code Map<FluidType, Tuple.*<...>>} idiom, same rationale for staying a bespoke Java table
 * rather than a JSON {@code Recipe<?>} - CE never made this data-driven in any way a datapack
 * schema would improve on; its {@code readRecipe}/{@code writeRecipe} exist only for CE's own
 * generic in-game recipe-override tool, not for player-facing datapacks).
 *
 * <p>All 18 of CE's default entries are ported verbatim below - no scope trim, unlike most other
 * Phase 7 recipe files: the research report confirmed all 40 distinct {@link FluidType} names this
 * class references are already registered under the same names in {@link Fluids}, so this is a
 * 100%, zero-blocker port (the single healthiest of the three files this task covers).
 *
 * <p><b>Not yet built: the Fraction Tower block/block-entity itself</b> (confirmed absent by the
 * research report - {@code MachineFractionTower}/{@code FractionSpacer}/
 * {@code TileEntityMachineFractionTower} are all zero in this port). This class is recipe data
 * only, ready for whichever future pass builds that machine to consume via {@link #getFractions}.
 */
public final class FractionRecipes {

    private static final Map<FluidType, Tuple.Pair<FluidStack, FluidStack>> FRACTIONS = new LinkedHashMap<>();

    private static boolean registered = false;

    private FractionRecipes() {
    }

    public static synchronized void registerFractions() {
        if (registered) return;
        registered = true;

        FRACTIONS.put(Fluids.HEAVYOIL, new Tuple.Pair<>(new FluidStack(Fluids.BITUMEN, 30), new FluidStack(Fluids.SMEAR, 70)));
        FRACTIONS.put(Fluids.HEAVYOIL_VACUUM, new Tuple.Pair<>(new FluidStack(Fluids.SMEAR, 40), new FluidStack(Fluids.HEATINGOIL_VACUUM, 60)));
        FRACTIONS.put(Fluids.SMEAR, new Tuple.Pair<>(new FluidStack(Fluids.HEATINGOIL, 60), new FluidStack(Fluids.LUBRICANT, 40)));
        FRACTIONS.put(Fluids.NAPHTHA, new Tuple.Pair<>(new FluidStack(Fluids.HEATINGOIL, 40), new FluidStack(Fluids.DIESEL, 60)));
        FRACTIONS.put(Fluids.NAPHTHA_DS, new Tuple.Pair<>(new FluidStack(Fluids.XYLENE, 60), new FluidStack(Fluids.DIESEL_REFORM, 40)));
        FRACTIONS.put(Fluids.NAPHTHA_CRACK, new Tuple.Pair<>(new FluidStack(Fluids.HEATINGOIL, 30), new FluidStack(Fluids.DIESEL_CRACK, 70)));
        FRACTIONS.put(Fluids.LIGHTOIL, new Tuple.Pair<>(new FluidStack(Fluids.DIESEL, 40), new FluidStack(Fluids.KEROSENE, 60)));
        FRACTIONS.put(Fluids.LIGHTOIL_DS, new Tuple.Pair<>(new FluidStack(Fluids.DIESEL_REFORM, 60), new FluidStack(Fluids.KEROSENE_REFORM, 40)));
        FRACTIONS.put(Fluids.LIGHTOIL_CRACK, new Tuple.Pair<>(new FluidStack(Fluids.KEROSENE, 70), new FluidStack(Fluids.PETROLEUM, 30)));
        FRACTIONS.put(Fluids.COALOIL, new Tuple.Pair<>(new FluidStack(Fluids.COALGAS, 30), new FluidStack(Fluids.OIL, 70)));
        FRACTIONS.put(Fluids.COALCREOSOTE, new Tuple.Pair<>(new FluidStack(Fluids.COALOIL, 10), new FluidStack(Fluids.BITUMEN, 90)));
        FRACTIONS.put(Fluids.REFORMATE, new Tuple.Pair<>(new FluidStack(Fluids.AROMATICS, 40), new FluidStack(Fluids.XYLENE, 60)));
        FRACTIONS.put(Fluids.LIGHTOIL_VACUUM, new Tuple.Pair<>(new FluidStack(Fluids.KEROSENE, 70), new FluidStack(Fluids.REFORMGAS, 30)));
        FRACTIONS.put(Fluids.EGG, new Tuple.Pair<>(new FluidStack(Fluids.CHOLESTEROL, 50), new FluidStack(Fluids.RADIOSOLVENT, 50)));
        FRACTIONS.put(Fluids.OIL_COKER, new Tuple.Pair<>(new FluidStack(Fluids.CRACKOIL, 30), new FluidStack(Fluids.HEATINGOIL, 70)));
        FRACTIONS.put(Fluids.NAPHTHA_COKER, new Tuple.Pair<>(new FluidStack(Fluids.NAPHTHA_CRACK, 75), new FluidStack(Fluids.LIGHTOIL_CRACK, 25)));
        FRACTIONS.put(Fluids.GAS_COKER, new Tuple.Pair<>(new FluidStack(Fluids.AROMATICS, 25), new FluidStack(Fluids.CARBONDIOXIDE, 75)));
        FRACTIONS.put(Fluids.CHLOROCALCITE_MIX, new Tuple.Pair<>(new FluidStack(Fluids.CHLOROCALCITE_CLEANED, 50), new FluidStack(Fluids.COLLOID, 50)));
        FRACTIONS.put(Fluids.BAUXITE_SOLUTION, new Tuple.Pair<>(new FluidStack(Fluids.REDMUD, 50), new FluidStack(Fluids.SODIUM_ALUMINATE, 50)));
    }

    /** Inputs are always 100mB; output quantities (in mB) are set accordingly (matches CE's own {@code getComment()}). */
    public static Tuple.Pair<FluidStack, FluidStack> getFractions(FluidType oil) {
        registerFractions();
        return FRACTIONS.get(oil);
    }

    /**
     * Full-collection accessor for a future JEI category / Fraction Tower block entity, matching
     * {@link RefineryRecipes#getAllRefinery()}'s established pattern (defensive lazy registration,
     * unmodifiable view).
     */
    public static Map<FluidType, Tuple.Pair<FluidStack, FluidStack>> getAllFractions() {
        registerFractions();
        return Collections.unmodifiableMap(FRACTIONS);
    }
}
