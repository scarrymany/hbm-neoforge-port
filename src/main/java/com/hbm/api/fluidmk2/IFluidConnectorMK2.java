package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.Direction;

/**
 * Fluid-side counterpart to {@link com.hbm.api.energymk2.IEnergyConnectorMK2}. Ported unchanged
 * from CE (dir is per-fluid-type rather than a single blanket answer, since a pipe segment can be
 * whitelisted/filtered to specific fluids).
 */
public interface IFluidConnectorMK2 {

    /**
     * Whether the given side can be connected to.
     * dir refers to the side of this block, not the connecting block doing the check.
     */
    default boolean canConnect(FluidType type, Direction dir) {
        return dir != null;
    }
}
