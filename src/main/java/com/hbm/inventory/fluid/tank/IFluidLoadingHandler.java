package com.hbm.inventory.fluid.tank;

import net.neoforged.neoforge.items.IItemHandler;

/** CE {@code com.hbm.inventory.fluid.tank.IFluidLoadingHandler}. */
public interface IFluidLoadingHandler {

    boolean fillItem(IItemHandler slots, int in, int out, FluidTankNTM tank);

    boolean emptyItem(IItemHandler slots, int in, int out, FluidTankNTM tank);
}
