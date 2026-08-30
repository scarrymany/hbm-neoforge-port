package com.hbm.api.fluidmk2;

import com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.lib.DirPos;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.UniNodespace;
import com.hbm.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * If it receives fluid, use this. Fluid-side counterpart to
 * {@link com.hbm.api.energymk2.IEnergyReceiverMK2}.
 *
 * <p>Ported from CE, translating 1.12.2 {@code TileEntity}/{@code World}/{@code ForgeDirection} to
 * {@code BlockEntity}/{@code Level}/{@code Direction} exactly like the already-committed
 * {@code IEnergyReceiverMK2} did for its energy counterpart, including the same two simplifications
 * that file already made relative to CE: no debug-particle branch, and {@link Compat#getBlockEntityStandard}
 * (a forward reference - {@code Compat} doesn't exist in this port yet, same as
 * {@code IEnergyReceiverMK2#trySubscribe} already forward-references it).
 *
 * <p>CE's {@code pullFromForeignHandler} (draining a neighbour's vanilla fluid-handler capability
 * when it isn't an NTM pipe/machine) is kept, since NeoForge's own fluid-handler capability is
 * already used throughout this port (see {@code NTMFluidHandlerWrapper}) - this is a same-mod
 * capability bridge, unlike {@code IEnergyProviderMK2}'s FE bridge (a cross-system HE-to-RF
 * conversion) which that file's own javadoc defers behind a config flag pending confirmed API
 * verification. {@code Level#getCapability(Capabilities.FluidHandler.BLOCK, pos, side)} is verified
 * real per {@code InventoryHelper}'s own provenance comment for the identical
 * {@code Capabilities.ItemHandler.BLOCK} shape.
 */
public interface IFluidReceiverMK2 extends IFluidUserMK2 {

    /** Sends fluid of the desired type and pressure to the receiver, returns the remainder. */
    long transferFluid(FluidType type, int pressure, long amount);

    default long getReceiverSpeed(FluidType type, int pressure) { return 1_000_000_000; }

    /** How much of the given (type, pressure) pair this receiver currently wants. */
    long getDemand(FluidType type, int pressure);

    default int[] getReceivingPressureRange(FluidType type) { return DEFAULT_PRESSURE_RANGE; }

    default void trySubscribe(FluidType type, Level level, DirPos pos) {
        trySubscribe(type, level, pos.getPos(), pos.getDir());
    }

    default void trySubscribe(FluidType type, Level level, BlockPos pos, Direction dir) {
        trySubscribe(type, level, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void trySubscribe(FluidType type, Level level, int x, int y, int z, Direction dir) {

        BlockPos pos = new BlockPos(x, y, z);
        BlockEntity be = Compat.getBlockEntityStandard(level, pos);

        if (be instanceof IFluidConnectorMK2 con) {
            if (!con.canConnect(type, dir.getOpposite())) return;

            GenNode<FluidNetMK2> node = UniNodespace.getNode(level, pos, type.getNetworkProvider());

            if (node != null && node.net != null) {
                node.net.addReceiver(this);
            }
        } else if (be != null && be != this) {
            // Not an NTM pipe - NTM machines never proactively pull from each other (senders push via
            // tryProvide instead), but a foreign block (AE2 fluid interface/bus, any other mod's tank)
            // never pushes into us on its own either, so without an explicit pull here it would just sit
            // there forever. Only pressure-0 fluid is requested: pressure has no representation on the
            // other side of the vanilla capability, see IFluidStandardSenderMK2#pushToForeignHandler.
            pullFromForeignHandler(type, level, pos, dir);
        }
    }

    /**
     * Pulls pressure-0 fluid of the given type from a neighbour that only exposes NeoForge's own
     * fluid-handler capability (AE2 fluid buses/interfaces, or any other mod's tank) - simulate
     * first, then only actually drain (and only actually accept) the amount both sides agree on, so
     * a mismatch between what the target reports and what we can store can't destroy or duplicate
     * fluid.
     */
    default void pullFromForeignHandler(FluidType type, Level level, BlockPos pos, Direction dir) {
        Fluid ff = type.getFF();
        if (ff == null) return;

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir.getOpposite());
        if (handler == null) return;

        long demand = Math.min(this.getDemand(type, 0), this.getReceiverSpeed(type, 0));
        if (demand <= 0) return;
        int want = (int) Math.min(demand, Integer.MAX_VALUE);

        FluidStack simulated = handler.drain(new FluidStack(ff, want), FluidAction.SIMULATE);
        if (simulated.isEmpty()) return;
        FluidStack drained = handler.drain(new FluidStack(ff, simulated.getAmount()), FluidAction.EXECUTE);
        if (drained.isEmpty()) return;
        this.transferFluid(type, 0, drained.getAmount());
    }

    default ConnectionPriority getFluidPriority() {
        return ConnectionPriority.NORMAL;
    }
}
