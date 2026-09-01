package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.HashMap;
import java.util.Map;

/** CE {@code FluidBreederRecipes} — 3 fluid→fluid rows for fusion breeder. */
public final class FluidBreederRecipes {

    public static final Map<FluidType, Tuple.Pair<Integer, FluidStack>> RECIPES = new HashMap<>();

    private static boolean registered;

    private FluidBreederRecipes() {
    }

    public static void register() {
        if (registered) return;
        registered = true;
        put(new FluidStack(Fluids.GAS, 1_000), new FluidStack(Fluids.SYNGAS, 1_000));
        put(new FluidStack(Fluids.LIGHTOIL, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
        put(new FluidStack(Fluids.LIGHTOIL_CRACK, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
    }

    private static void put(FluidStack input, FluidStack output) {
        RECIPES.put(input.type, new Tuple.Pair<>(input.fill, output));
    }

    public static Tuple.Pair<Integer, FluidStack> getOutput(FluidType type) {
        register();
        return RECIPES.get(type);
    }
}
