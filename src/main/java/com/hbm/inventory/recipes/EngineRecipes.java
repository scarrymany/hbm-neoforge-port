package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code EngineRecipes.java}:16-39. Census: {@code recipes.put}. Turbofan TE still gates on
 * {@code FT_Combustible.FuelGrade.AERO} (same as CE {@code TileEntityMachineTurbofan}:182-185).
 */
public final class EngineRecipes {

    public static final Map<FluidType, Long> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private EngineRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        // CE EngineRecipes.java:16-39 — 1000 mB energy, skip lowercase-mod compat
        recipes.put(Fluids.HYDROGEN, 10_000L);
        recipes.put(Fluids.DEUTERIUM, 10_000L);
        recipes.put(Fluids.TRITIUM, 10_000L);
        recipes.put(Fluids.HEAVYOIL, 100_000L);
        recipes.put(Fluids.RECLAIMED, 200_000L);
        recipes.put(Fluids.PETROIL, 300_000L);
        recipes.put(Fluids.NAPHTHA, 200_000L);
        recipes.put(Fluids.DIESEL, 500_000L);
        recipes.put(Fluids.LIGHTOIL, 500_000L);
        recipes.put(Fluids.KEROSENE, 1_250_000L);
        recipes.put(Fluids.KEROSENE_REFORM, 1_750_000L);
        recipes.put(Fluids.BIOGAS, 500_000L);
        recipes.put(Fluids.BIOFUEL, 400_000L);
        recipes.put(Fluids.NITAN, 5_000_000L);
        recipes.put(Fluids.BALEFIRE, 2_500_000L);
        recipes.put(Fluids.GASOLINE, 1_000_000L);
        recipes.put(Fluids.ETHANOL, 200_000L);
        recipes.put(Fluids.FISHOIL, 50_000L);
        recipes.put(Fluids.SUNFLOWEROIL, 80_000L);
        recipes.put(Fluids.GAS, 100_000L);
        recipes.put(Fluids.PETROLEUM, 300_000L);
        recipes.put(Fluids.AROMATICS, 150_000L);
        recipes.put(Fluids.UNSATURATEDS, 250_000L);
    }

    public static long getEnergy(FluidType type) {
        register();
        return recipes.getOrDefault(type, 0L);
    }
}
