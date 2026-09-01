package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple.Triplet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code ReformingRecipes.java}:23-68. 100 mB in + 20k HE + catalyst → 3 fluid outs.
 * Census: {@code recipes.put}.
 */
public final class ReformingRecipes {

    public static final Map<FluidType, Triplet<FluidStack, FluidStack, FluidStack>> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private ReformingRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE ReformingRecipes.java:24-68
        recipes.put(Fluids.HEATINGOIL, trip(Fluids.NAPHTHA, 50, Fluids.PETROLEUM, 15, Fluids.HYDROGEN, 10));
        recipes.put(Fluids.NAPHTHA, trip(Fluids.REFORMATE, 50, Fluids.PETROLEUM, 15, Fluids.HYDROGEN, 10));
        recipes.put(Fluids.NAPHTHA_CRACK, trip(Fluids.REFORMATE, 50, Fluids.AROMATICS, 10, Fluids.HYDROGEN, 5));
        recipes.put(Fluids.NAPHTHA_COKER, trip(Fluids.REFORMATE, 50, Fluids.REFORMGAS, 10, Fluids.HYDROGEN, 5));
        recipes.put(Fluids.LIGHTOIL, trip(Fluids.AROMATICS, 50, Fluids.REFORMGAS, 10, Fluids.HYDROGEN, 15));
        recipes.put(Fluids.LIGHTOIL_CRACK, trip(Fluids.AROMATICS, 50, Fluids.REFORMGAS, 5, Fluids.HYDROGEN, 20));
        recipes.put(Fluids.PETROLEUM, trip(Fluids.UNSATURATEDS, 85, Fluids.REFORMGAS, 10, Fluids.HYDROGEN, 5));
        recipes.put(Fluids.SOURGAS, trip(Fluids.SULFURIC_ACID, 75, Fluids.PETROLEUM, 10, Fluids.HYDROGEN, 15));
        recipes.put(Fluids.CHOLESTEROL, trip(Fluids.ESTRADIOL, 50, Fluids.REFORMGAS, 35, Fluids.HYDROGEN, 15));
    }

    public static Triplet<FluidStack, FluidStack, FluidStack> getOutput(FluidType type) {
        register();
        return recipes.get(type);
    }

    private static Triplet<FluidStack, FluidStack, FluidStack> trip(FluidType a, int na, FluidType b, int nb, FluidType c, int nc) {
        return new Triplet<>(new FluidStack(a, na), new FluidStack(b, nb), new FluidStack(c, nc));
    }
}
