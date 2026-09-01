package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple.Pair;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code CrackingRecipes.java}:41-53. 100 mB in + 200 mB steam → two outs + 2 mB spentsteam.
 * Census: {@code recipes.put} (CE used {@code cracking.put}).
 */
public final class CrackingRecipes {

    public static final Map<FluidType, Pair<FluidStack, FluidStack>> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private CrackingRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE CrackingRecipes.java:42-53
        recipes.put(Fluids.OIL, pair(Fluids.CRACKOIL, 80, Fluids.PETROLEUM, 20));
        recipes.put(Fluids.BITUMEN, pair(Fluids.OIL, 80, Fluids.AROMATICS, 20));
        recipes.put(Fluids.SMEAR, pair(Fluids.NAPHTHA, 60, Fluids.PETROLEUM, 40));
        recipes.put(Fluids.GAS, pair(Fluids.PETROLEUM, 30, Fluids.UNSATURATEDS, 20));
        recipes.put(Fluids.DIESEL, pair(Fluids.KEROSENE, 40, Fluids.PETROLEUM, 30));
        recipes.put(Fluids.DIESEL_CRACK, pair(Fluids.KEROSENE, 40, Fluids.PETROLEUM, 30));
        recipes.put(Fluids.KEROSENE, pair(Fluids.PETROLEUM, 60, Fluids.NONE, 0));
        recipes.put(Fluids.WOODOIL, pair(Fluids.HEATINGOIL, 40, Fluids.AROMATICS, 10));
        recipes.put(Fluids.XYLENE, pair(Fluids.AROMATICS, 80, Fluids.PETROLEUM, 20));
        recipes.put(Fluids.HEATINGOIL_VACUUM, pair(Fluids.HEATINGOIL, 80, Fluids.REFORMGAS, 20));
        recipes.put(Fluids.REFORMATE, pair(Fluids.UNSATURATEDS, 40, Fluids.REFORMGAS, 60));
        recipes.put(Fluids.BIOGAS, pair(Fluids.PETROLEUM, 20, Fluids.AROMATICS, 20));
    }

    public static Pair<FluidStack, FluidStack> getCracking(FluidType oil) {
        register();
        return recipes.get(oil);
    }

    private static Pair<FluidStack, FluidStack> pair(FluidType a, int na, FluidType b, int nb) {
        return new Pair<>(new FluidStack(a, na), new FluidStack(b, nb));
    }
}
