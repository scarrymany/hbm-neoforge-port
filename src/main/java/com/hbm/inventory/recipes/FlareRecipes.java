package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Gaseous_ART;

/**
 * CE has no flare recipe map — {@code TileEntityMachineGasFlare} is trait-driven
 * (vent {@code FT_Gaseous}/{@code FT_Gaseous_ART}, burn {@code FT_Flammable}).
 * This class is the documented lookup, not a census inflate.
 */
public final class FlareRecipes {

    private FlareRecipes() {
    }

    public static void register() {
        // no map — CE TileEntityMachineGasFlare.java:150-201
    }

    public static boolean canVent(FluidType type) {
        return type.hasTrait(FT_Gaseous.class) || type.hasTrait(FT_Gaseous_ART.class);
    }

    public static boolean canBurn(FluidType type) {
        return type.hasTrait(FT_Flammable.class);
    }

    public static long burnEnergyPerMb(FluidType type) {
        if (!type.hasTrait(FT_Flammable.class)) return 0;
        return type.getTrait(FT_Flammable.class).getHeatEnergy() / 1_000L;
    }
}
