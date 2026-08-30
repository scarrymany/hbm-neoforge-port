package com.hbm.api.fluidmk2;

import com.hbm.api.tile.ILoadedTile;
import com.hbm.inventory.fluid.tank.FluidTankNTM;

import java.util.List;

/**
 * Fluid-side counterpart to {@link com.hbm.api.energymk2.IEnergyHandlerMK2}: the common ancestor of
 * {@link IFluidReceiverMK2} and {@link IFluidProviderMK2}, DO NOT implement directly.
 *
 * <p>{@link #getAllTanks()} returns a {@link List} rather than CE's {@code FluidTankNTM[]} - this
 * port's own already-committed {@link com.hbm.capability.NTMFluidHandlerWrapper} fixes that shape
 * ({@code user.getAllTanks().size()}/{@code .get(index)}, and a plain for-each over
 * {@code provider.getAllTanks()}), matching the List-based convention the rest of this port uses
 * for tank collections. Pure data-shape change - the CE math this trio implements is unaffected.
 */
public interface IFluidUserMK2 extends IFluidConnectorMK2, ILoadedTile {

    int HIGHEST_VALID_PRESSURE = 5;
    int[] DEFAULT_PRESSURE_RANGE = new int[] {0, 0};

    List<FluidTankNTM> getAllTanks();
}
