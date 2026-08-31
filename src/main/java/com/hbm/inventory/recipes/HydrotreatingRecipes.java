package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.HydrotreatingRecipes} (118 lines, read in full) -
 * per {@code docs/phase7/mrec_10_crystallizer_misc.md}'s catalog (all 6 entries confirmed ready: pure
 * fluid-to-fluid, every referenced {@link FluidType} already exists in {@link Fluids}, zero item
 * dependencies). Recipe shape: input fluid type -> (hydrogen consumed, primary desulfurized output,
 * {@link Fluids#SOURGAS} byproduct) - a {@code Map<FluidType, Tuple.Triplet<FluidStack, FluidStack,
 * FluidStack>>}, using this port's already-existing {@link Tuple.Triplet} (same shape CE itself uses).
 * Kept as a plain hardcoded Java registration list for the same reason every sibling bespoke recipe
 * class in this package stays that way - see {@link RefineryRecipes}'s own header for the fuller
 * rationale (NeoForge 1.21.1 has no first-party fluid-to-multi-fluid {@code RecipeType}).
 * <p>
 * <b>No machine exists yet to consume this data</b> (confirmed absent: zero matches for "hydrotreat"
 * anywhere in this port) - same "recipe data now, machine later" sequencing as {@link
 * LiquefactionRecipes}, following the same {@code RockMillRecipes}-established precedent; see that
 * class's own header for the fuller rationale. The Hydrotreater block+block-entity would reuse this
 * port's {@code MachineRefineryBlockEntity}/oil-chain package shape once built.
 * <p>
 * <b>Fluid amounts</b>: CE's own {@code getRecipes()} (its JEI-table builder) multiplies every amount
 * below by 10 purely for its icon-stack display - the values registered here are CE's real raw
 * per-tick {@code registerDefaults()} amounts (hydrogen 5 or 10, output1 90/80, output2/SOURGAS
 * 15/30), not the x10 display figures, per this task's own research report's explicit correction.
 */
public final class HydrotreatingRecipes {

    private static final Map<FluidType, Tuple.Triplet<FluidStack, FluidStack, FluidStack>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private HydrotreatingRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // registerRecipe: recipes.put(Fluids.OIL, new Tuple.Triplet<>(new FluidStack(HYDROGEN, 5, 1), new FluidStack(OIL_DS, 90), new FluidStack(SOURGAS, 15)));
        RECIPES.put(Fluids.OIL, new Tuple.Triplet<>(
                new FluidStack(Fluids.HYDROGEN, 5, 1),
                new FluidStack(Fluids.OIL_DS, 90),
                new FluidStack(Fluids.SOURGAS, 15)
        ));
        RECIPES.put(Fluids.CRACKOIL, new Tuple.Triplet<>(
                new FluidStack(Fluids.HYDROGEN, 5, 1),
                new FluidStack(Fluids.CRACKOIL_DS, 90),
                new FluidStack(Fluids.SOURGAS, 15)
        ));
        RECIPES.put(Fluids.GAS, new Tuple.Triplet<>(
                new FluidStack(Fluids.HYDROGEN, 5, 1),
                new FluidStack(Fluids.PETROLEUM, 80),
                new FluidStack(Fluids.SOURGAS, 15)
        ));
        RECIPES.put(Fluids.DIESEL_CRACK, new Tuple.Triplet<>(
                new FluidStack(Fluids.HYDROGEN, 10, 1),
                new FluidStack(Fluids.DIESEL, 80),
                new FluidStack(Fluids.SOURGAS, 30)
        ));
        RECIPES.put(Fluids.DIESEL_CRACK_REFORM, new Tuple.Triplet<>(
                new FluidStack(Fluids.HYDROGEN, 10, 1),
                new FluidStack(Fluids.DIESEL_REFORM, 80),
                new FluidStack(Fluids.SOURGAS, 30)
        ));
        RECIPES.put(Fluids.COALOIL, new Tuple.Triplet<>(
                new FluidStack(Fluids.HYDROGEN, 10, 1),
                new FluidStack(Fluids.COALGAS, 80),
                new FluidStack(Fluids.SOURGAS, 15)
        ));
    }

    public static Tuple.Triplet<FluidStack, FluidStack, FluidStack> getOutput(FluidType type) {
        registerDefaults();
        return RECIPES.get(type);
    }

    /** Full-collection accessor for a future JEI category, matching {@link CrystallizerRecipes#getAllRecipes()}'s own precedent. */
    public static Map<FluidType, Tuple.Triplet<FluidStack, FluidStack, FluidStack>> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }
}
