package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple.Triplet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code HydrotreatingRecipes.java}:23-59. 100 mB in + H₂@P1 + 20k HE + catalyst.
 * Census: {@code recipes.put}.
 */
public final class HydrotreatingRecipes {

    public static final Map<FluidType, Triplet<FluidStack, FluidStack, FluidStack>> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private HydrotreatingRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE HydrotreatingRecipes.java:25-59
        recipes.put(Fluids.OIL, trip(Fluids.HYDROGEN, 5, 1, Fluids.OIL_DS, 90, Fluids.SOURGAS, 15));
        recipes.put(Fluids.CRACKOIL, trip(Fluids.HYDROGEN, 5, 1, Fluids.CRACKOIL_DS, 90, Fluids.SOURGAS, 15));
        recipes.put(Fluids.GAS, trip(Fluids.HYDROGEN, 5, 1, Fluids.PETROLEUM, 80, Fluids.SOURGAS, 15));
        recipes.put(Fluids.DIESEL_CRACK, trip(Fluids.HYDROGEN, 10, 1, Fluids.DIESEL, 80, Fluids.SOURGAS, 30));
        recipes.put(Fluids.DIESEL_CRACK_REFORM, trip(Fluids.HYDROGEN, 10, 1, Fluids.DIESEL_REFORM, 80, Fluids.SOURGAS, 30));
        recipes.put(Fluids.COALOIL, trip(Fluids.HYDROGEN, 10, 1, Fluids.COALGAS, 80, Fluids.SOURGAS, 15));
    }

    public static Triplet<FluidStack, FluidStack, FluidStack> getOutput(FluidType type) {
        register();
        return recipes.get(type);
    }

    private static Triplet<FluidStack, FluidStack, FluidStack> trip(
            FluidType h2, int h2n, int h2p, FluidType a, int na, FluidType b, int nb) {
        return new Triplet<>(new FluidStack(h2, h2n, h2p), new FluidStack(a, na), new FluidStack(b, nb));
    }
}
