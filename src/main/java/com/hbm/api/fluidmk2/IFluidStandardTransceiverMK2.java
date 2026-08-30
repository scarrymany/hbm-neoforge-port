package com.hbm.api.fluidmk2;

/**
 * A single tile that both sends and receives fluid over the network (e.g. CE's
 * {@code TileEntityMachineFluidTank}). Ported unchanged from CE - a pure marker union of the two
 * standard interfaces, no members of its own.
 */
public interface IFluidStandardTransceiverMK2 extends IFluidStandardSenderMK2, IFluidStandardReceiverMK2 { }
