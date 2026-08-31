package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recipe data for the Catalytic Reformer, ported from CE's {@code com.hbm.inventory.recipes.
 * ReformingRecipes} ({@code docs/phase7/mrec_04_arcwelder_misc.md}, 126 lines read in full
 * upstream) - CE's simplest recipe shape of the four covered by that report: a flat
 * {@code HashMap<FluidType, Tuple.Triplet<FluidStack, FluidStack, FluidStack>>}, no items, no
 * chance, no power field at all (CE's own class carries none). 1 fluid input (1,000 mB, implicit -
 * the map key itself represents "some amount", scaled x10 at display/use time per CE's own
 * {@code getRecipes()}) -&gt; fixed 3 fluid outputs.
 * <p>
 * <b>All 9 of CE's entries are ported here, near-verbatim</b> - every one of the 15 distinct
 * {@link FluidType}s this file references (including {@code UNSATURATEDS}, independently
 * re-verified against {@link Fluids} while writing this class, closing the research report's own
 * flagged follow-up) is already registered 1:1 in this port's {@link Fluids}. This class's only real
 * blocker is the Catalytic Reformer block/block entity/GUI itself not existing yet (confirmed
 * absent by the research report) - once that lands, {@link #getOutput(FluidType)} is ready to use
 * as-is.
 */
public final class ReformingRecipes {

    public static final Map<FluidType, Tuple.Triplet<FluidStack, FluidStack, FluidStack>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private ReformingRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        RECIPES.put(Fluids.HEATINGOIL, new Tuple.Triplet<>(
                new FluidStack(Fluids.NAPHTHA, 50),
                new FluidStack(Fluids.PETROLEUM, 15),
                new FluidStack(Fluids.HYDROGEN, 10)));
        RECIPES.put(Fluids.NAPHTHA, new Tuple.Triplet<>(
                new FluidStack(Fluids.REFORMATE, 50),
                new FluidStack(Fluids.PETROLEUM, 15),
                new FluidStack(Fluids.HYDROGEN, 10)));
        RECIPES.put(Fluids.NAPHTHA_CRACK, new Tuple.Triplet<>(
                new FluidStack(Fluids.REFORMATE, 50),
                new FluidStack(Fluids.AROMATICS, 10),
                new FluidStack(Fluids.HYDROGEN, 5)));
        RECIPES.put(Fluids.NAPHTHA_COKER, new Tuple.Triplet<>(
                new FluidStack(Fluids.REFORMATE, 50),
                new FluidStack(Fluids.REFORMGAS, 10),
                new FluidStack(Fluids.HYDROGEN, 5)));
        RECIPES.put(Fluids.LIGHTOIL, new Tuple.Triplet<>(
                new FluidStack(Fluids.AROMATICS, 50),
                new FluidStack(Fluids.REFORMGAS, 10),
                new FluidStack(Fluids.HYDROGEN, 15)));
        RECIPES.put(Fluids.LIGHTOIL_CRACK, new Tuple.Triplet<>(
                new FluidStack(Fluids.AROMATICS, 50),
                new FluidStack(Fluids.REFORMGAS, 5),
                new FluidStack(Fluids.HYDROGEN, 20)));
        RECIPES.put(Fluids.PETROLEUM, new Tuple.Triplet<>(
                new FluidStack(Fluids.UNSATURATEDS, 85),
                new FluidStack(Fluids.REFORMGAS, 10),
                new FluidStack(Fluids.HYDROGEN, 5)));
        RECIPES.put(Fluids.SOURGAS, new Tuple.Triplet<>(
                new FluidStack(Fluids.SULFURIC_ACID, 75),
                new FluidStack(Fluids.PETROLEUM, 10),
                new FluidStack(Fluids.HYDROGEN, 15)));
        RECIPES.put(Fluids.CHOLESTEROL, new Tuple.Triplet<>(
                new FluidStack(Fluids.ESTRADIOL, 50),
                new FluidStack(Fluids.REFORMGAS, 35),
                new FluidStack(Fluids.HYDROGEN, 15)));
    }

    public static Tuple.Triplet<FluidStack, FluidStack, FluidStack> getOutput(FluidType type) {
        register();
        return RECIPES.get(type);
    }
}
