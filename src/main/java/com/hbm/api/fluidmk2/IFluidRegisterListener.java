package com.hbm.api.fluidmk2;

public interface IFluidRegisterListener {

    /**
     * Called when the fluid registry initializes all fluids. Use a FluidType constructor variant
     * that registers itself (e.g. the custom/foreign setup constructors) to add new instances.
     */
    void onFluidsLoad();
}
