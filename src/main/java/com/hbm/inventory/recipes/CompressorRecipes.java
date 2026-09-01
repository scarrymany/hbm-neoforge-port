package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code CompressorRecipes.java}:24-32. Key = (fluid, PU).
 * Census: {@code recipes.put}.
 */
public final class CompressorRecipes {

    public static final Map<Pair<FluidType, Integer>, CompressorRecipe> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private CompressorRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE CompressorRecipes.java:25-31
        recipes.put(new Pair<>(Fluids.PETROLEUM, 0), new CompressorRecipe(2_000, new FluidStack(Fluids.PETROLEUM, 2_000, 1), 20));
        recipes.put(new Pair<>(Fluids.PETROLEUM, 1), new CompressorRecipe(2_000, new FluidStack(Fluids.LPG, 1_000, 0), 20));
        recipes.put(new Pair<>(Fluids.BLOOD, 3), new CompressorRecipe(1_000, new FluidStack(Fluids.HEAVYOIL, 250, 0), 200));
        recipes.put(new Pair<>(Fluids.PERFLUOROMETHYL, 0), new CompressorRecipe(1_000, new FluidStack(Fluids.PERFLUOROMETHYL, 1_000, 1), 50));
        recipes.put(new Pair<>(Fluids.PERFLUOROMETHYL, 1), new CompressorRecipe(1_000, new FluidStack(Fluids.PERFLUOROMETHYL_COLD, 1_000, 0), 50));
    }

    public static CompressorRecipe getRecipe(FluidType type, int pressure) {
        register();
        return recipes.get(new Pair<>(type, pressure));
    }

    public static final class CompressorRecipe {
        public final FluidStack output;
        public final int inputAmount;
        public final int duration;

        public CompressorRecipe(int input, FluidStack output, int duration) {
            this.output = output;
            this.inputAmount = input;
            this.duration = duration;
        }
    }
}
