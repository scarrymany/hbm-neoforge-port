package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;

/**
 * If it sends fluid, use this. Fluid-side counterpart to {@link com.hbm.api.energymk2.IEnergyProviderMK2}.
 * Ported unchanged from CE - see {@link IFluidStandardSenderMK2} for the tank-array-backed default
 * implementation of this contract, and {@link FluidNetMK2} for how a network drains providers.
 */
public interface IFluidProviderMK2 extends IFluidUserMK2 {

    /** Uses up available fluid; no sanity checking, the caller must ensure amount &lt;= what's available. */
    void useUpFluid(FluidType type, int pressure, long amount);

    default long getProviderSpeed(FluidType type, int pressure) { return 1_000_000_000; }

    /** How much of the given (type, pressure) pair this provider currently has to give. */
    long getFluidAvailable(FluidType type, int pressure);

    default int[] getProvidingPressureRange(FluidType type) { return DEFAULT_PRESSURE_RANGE; }
}
