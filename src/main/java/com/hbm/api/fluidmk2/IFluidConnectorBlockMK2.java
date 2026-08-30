package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;

/**
 * Interface for all blocks that should visually/logically connect to fluid ducts without having an
 * {@link IFluidConnectorMK2} block entity (a plain {@code Block}, checked for a fixed input/output
 * port on a machine that has no per-side-configurable block entity state). Fluid-side counterpart to
 * {@link com.hbm.api.energymk2.IEnergyConnectorBlock}, whose own javadoc documents the same "used
 * for rendering only" caveat CE's version carries; here it additionally gates real connectivity via
 * {@link com.hbm.lib.Library#canConnectFluid} (CE's own dual block-then-block-entity check), so it is
 * not purely cosmetic on the fluid side the way the energy one is.
 *
 * <p>Ported from CE's {@code com.hbm.api.fluidmk2.IFluidConnectorBlockMK2}, translating 1.12.2
 * {@code IBlockAccess}/{@code int x,y,z}/{@code ForgeDirection} to {@code BlockGetter}/
 * {@code BlockPos}/{@code Direction} exactly like {@link com.hbm.api.energymk2.IEnergyConnectorBlock}
 * did for its own energy counterpart (confirmed real shape, already committed and used by
 * {@code CapacitorBusBlock}).
 */
public interface IFluidConnectorBlockMK2 {

    /** {@code dir} is the face that is connected to, the direction going outwards from the block. */
    boolean canConnect(FluidType type, BlockGetter level, BlockPos pos, Direction dir);
}
