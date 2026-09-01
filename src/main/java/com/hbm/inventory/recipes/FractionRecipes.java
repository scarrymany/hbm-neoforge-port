package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code FractionRecipes.java}:23-42. 100 mB in → two fluid outs.
 * Census: {@code recipes.put} (CE used {@code fractions.put}, which the census misses).
 */
public final class FractionRecipes {

    public static final Map<FluidType, Pair<FluidStack, FluidStack>> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private FractionRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE FractionRecipes.java:24-42
        recipes.put(Fluids.HEAVYOIL, pair(Fluids.BITUMEN, 30, Fluids.SMEAR, 70));
        recipes.put(Fluids.HEAVYOIL_VACUUM, pair(Fluids.SMEAR, 40, Fluids.HEATINGOIL_VACUUM, 60));
        recipes.put(Fluids.SMEAR, pair(Fluids.HEATINGOIL, 60, Fluids.LUBRICANT, 40));
        recipes.put(Fluids.NAPHTHA, pair(Fluids.HEATINGOIL, 40, Fluids.DIESEL, 60));
        recipes.put(Fluids.NAPHTHA_DS, pair(Fluids.XYLENE, 60, Fluids.DIESEL_REFORM, 40));
        recipes.put(Fluids.NAPHTHA_CRACK, pair(Fluids.HEATINGOIL, 30, Fluids.DIESEL_CRACK, 70));
        recipes.put(Fluids.LIGHTOIL, pair(Fluids.DIESEL, 40, Fluids.KEROSENE, 60));
        recipes.put(Fluids.LIGHTOIL_DS, pair(Fluids.DIESEL_REFORM, 60, Fluids.KEROSENE_REFORM, 40));
        recipes.put(Fluids.LIGHTOIL_CRACK, pair(Fluids.KEROSENE, 70, Fluids.PETROLEUM, 30));
        recipes.put(Fluids.COALOIL, pair(Fluids.COALGAS, 30, Fluids.OIL, 70));
        recipes.put(Fluids.COALCREOSOTE, pair(Fluids.COALOIL, 10, Fluids.BITUMEN, 90));
        recipes.put(Fluids.REFORMATE, pair(Fluids.AROMATICS, 40, Fluids.XYLENE, 60));
        recipes.put(Fluids.LIGHTOIL_VACUUM, pair(Fluids.KEROSENE, 70, Fluids.REFORMGAS, 30));
        recipes.put(Fluids.EGG, pair(Fluids.CHOLESTEROL, 50, Fluids.RADIOSOLVENT, 50));
        recipes.put(Fluids.OIL_COKER, pair(Fluids.CRACKOIL, 30, Fluids.HEATINGOIL, 70));
        recipes.put(Fluids.NAPHTHA_COKER, pair(Fluids.NAPHTHA_CRACK, 75, Fluids.LIGHTOIL_CRACK, 25));
        recipes.put(Fluids.GAS_COKER, pair(Fluids.AROMATICS, 25, Fluids.CARBONDIOXIDE, 75));
        recipes.put(Fluids.CHLOROCALCITE_MIX, pair(Fluids.CHLOROCALCITE_CLEANED, 50, Fluids.COLLOID, 50));
        recipes.put(Fluids.BAUXITE_SOLUTION, pair(Fluids.REDMUD, 50, Fluids.SODIUM_ALUMINATE, 50));
    }

    public static Pair<FluidStack, FluidStack> getFractions(FluidType oil) {
        register();
        return recipes.get(oil);
    }

    private static Pair<FluidStack, FluidStack> pair(FluidType a, int na, FluidType b, int nb) {
        return new Pair<>(new FluidStack(a, na), new FluidStack(b, nb));
    }
}
