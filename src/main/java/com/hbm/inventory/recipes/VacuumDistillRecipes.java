package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple.Quartet;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code RefineryRecipes.java}:116-127 ({@code vacuum.put}). Vacuum distill table.
 * Census: {@code recipes.put} (CE used {@code vacuum.put}, census-invisible).
 */
public final class VacuumDistillRecipes {

    public static final Map<FluidType, Quartet<FluidStack, FluidStack, FluidStack, FluidStack>> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private VacuumDistillRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE RefineryRecipes.java:116-121
        recipes.put(Fluids.OIL, new Quartet<>(
                new FluidStack(Fluids.HEAVYOIL_VACUUM, 40),
                new FluidStack(Fluids.REFORMATE, 25),
                new FluidStack(Fluids.LIGHTOIL_VACUUM, 20),
                new FluidStack(Fluids.SOURGAS, 15)
        ));
        // CE RefineryRecipes.java:122-127
        recipes.put(Fluids.OIL_DS, new Quartet<>(
                new FluidStack(Fluids.HEAVYOIL_VACUUM, 40),
                new FluidStack(Fluids.REFORMATE, 25),
                new FluidStack(Fluids.LIGHTOIL_VACUUM, 20),
                new FluidStack(Fluids.REFORMGAS, 15)
        ));
    }

    public static Quartet<FluidStack, FluidStack, FluidStack, FluidStack> getOutput(FluidType type) {
        register();
        return recipes.get(type);
    }
}
