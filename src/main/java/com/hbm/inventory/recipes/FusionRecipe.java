package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.loader.GenericRecipe;

/** CE {@code FusionRecipe} — ignition / plasma / flux extras on GenericRecipe. */
public class FusionRecipe extends GenericRecipe {

    public long ignitionTemp;
    public long outputTemp;
    public double neutronFlux;
    public float r = 1F;
    public float g = 0.2F;
    public float b = 0.6F;

    public FusionRecipe(String name) {
        super(name);
    }

    public FusionRecipe setInputEnergy(long ignitionTemp) {
        this.ignitionTemp = ignitionTemp;
        return this;
    }

    public FusionRecipe setOutputEnergy(long outputTemp) {
        this.outputTemp = outputTemp;
        return this;
    }

    public FusionRecipe setOutputFlux(double neutronFlux) {
        this.neutronFlux = neutronFlux;
        return this;
    }

    public FusionRecipe setRGB(float r, float g, float b) {
        this.r = r;
        this.g = g;
        this.b = b;
        return this;
    }
}
