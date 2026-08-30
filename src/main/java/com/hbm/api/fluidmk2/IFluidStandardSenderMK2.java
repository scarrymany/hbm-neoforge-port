package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.uninos.GenNode;
import com.hbm.uninos.UniNodespace;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@link IFluidProviderMK2} with a standard tank-array(-turned-{@link List})-backed implementation
 * of fluid provision/removal, plus the {@code tryProvide} push logic. Ported from CE, translating
 * 1.12.2 types the same way {@link IFluidReceiverMK2} does - see that interface's javadoc for the
 * rationale behind keeping the vanilla-fluid-handler bridge ({@link #pushToForeignHandler}) while
 * dropping CE's debug-particle branch.
 *
 * @author hbm
 */
public interface IFluidStandardSenderMK2 extends IFluidProviderMK2 {

    default void tryProvide(FluidTankNTM tank, Level level, DirPos pos) { tryProvide(tank.getTankType(), tank.getPressure(), level, pos.getPos(), pos.getDir()); }
    default void tryProvide(FluidType type, Level level, DirPos pos) { tryProvide(type, 0, level, pos.getPos(), pos.getDir()); }
    default void tryProvide(FluidType type, int pressure, Level level, DirPos pos) { tryProvide(type, pressure, level, pos.getPos(), pos.getDir()); }

    default void tryProvide(FluidTankNTM tank, Level level, BlockPos pos, Direction dir) { tryProvide(tank.getTankType(), tank.getPressure(), level, pos, dir); }
    default void tryProvide(FluidType type, Level level, BlockPos pos, Direction dir) { tryProvide(type, 0, level, pos, dir); }

    default void tryProvide(FluidType type, int pressure, Level level, BlockPos pos, Direction dir) {
        tryProvide(type, pressure, level, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void tryProvide(FluidType type, int pressure, Level level, int x, int y, int z, Direction dir) {

        BlockPos targetPos = new BlockPos(x, y, z);
        BlockEntity te = level.getBlockEntity(targetPos);

        if (te instanceof IFluidConnectorMK2 con) {
            if (con.canConnect(type, dir.getOpposite())) {

                GenNode<FluidNetMK2> node = UniNodespace.getNode(level, targetPos, type.getNetworkProvider());

                if (node != null && node.net != null) {
                    node.net.addProvider(this);
                }
            }
        }

        if (te != this && te instanceof IFluidReceiverMK2 rec) {
            if (rec.canConnect(type, dir.getOpposite())) {
                long provides = Math.min(this.getFluidAvailable(type, pressure), this.getProviderSpeed(type, pressure));
                long receives = Math.min(rec.getDemand(type, pressure), rec.getReceiverSpeed(type, pressure));
                long toTransfer = Math.min(provides, receives);
                toTransfer -= rec.transferFluid(type, pressure, toTransfer);
                this.useUpFluid(type, pressure, toTransfer);
            }
        } else if (te != null && te != this && !(te instanceof IFluidConnectorMK2)) {
            // Neither an NTM pipe nor an NTM machine - fall back to NeoForge's own fluid-handler
            // capability so this can still push into AE2 fluid buses/interfaces, or any other mod's
            // tank, sitting on the opposite face. Only pressure-0 fluid is offered outward: a foreign
            // IFluidHandler has no way to represent NTM's pressure concept.
            if (pressure == 0) pushToForeignHandler(type, level, targetPos, dir);
        }
    }

    /**
     * Pushes pressure-0 fluid of the given type into a neighbour that only exposes NeoForge's own
     * fluid-handler capability (AE2 fluid buses/interfaces, or any other mod's tank) - simulate
     * first, then commit only the amount the target actually accepted, so a target that reports more
     * room than it really has can't destroy fluid.
     */
    default void pushToForeignHandler(FluidType type, Level level, BlockPos pos, Direction dir) {
        Fluid ff = type.getFF();
        if (ff == null) return;

        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, dir.getOpposite());
        if (handler == null) return;

        long available = Math.min(this.getFluidAvailable(type, 0), this.getProviderSpeed(type, 0));
        if (available <= 0) return;
        int offer = (int) Math.min(available, Integer.MAX_VALUE);

        int canAccept = handler.fill(new FluidStack(ff, offer), FluidAction.SIMULATE);
        if (canAccept <= 0) return;
        int filled = handler.fill(new FluidStack(ff, canAccept), FluidAction.EXECUTE);
        if (filled > 0) this.useUpFluid(type, 0, filled);
    }

    @NotNull List<FluidTankNTM> getSendingTanks();

    @Override
    default long getFluidAvailable(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) amount += tank.getFill();
        }
        return amount;
    }

    @Override
    default void useUpFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (FluidTankNTM tank : getSendingTanks()) {
                if (tank.getTankType() == type && tank.getPressure() == pressure) {
                    int toRem = Math.min(firstRound, tank.getFill());
                    tank.setFill(tank.getFill() - toRem);
                    amount -= toRem;
                }
            }
        }
        if (amount > 0) for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) {
                int toRem = (int) Math.min(amount, tank.getFill());
                tank.setFill(tank.getFill() - toRem);
                amount -= toRem;
            }
        }
    }

    @Override
    default int[] getProvidingPressureRange(FluidType type) {
        int lowest = HIGHEST_VALID_PRESSURE;
        int highest = 0;

        for (FluidTankNTM tank : getSendingTanks()) {
            if (tank.getTankType() == type) {
                if (tank.getPressure() < lowest) lowest = tank.getPressure();
                if (tank.getPressure() > highest) highest = tank.getPressure();
            }
        }

        return lowest <= highest ? new int[] {lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }

    @Override
    default long getProviderSpeed(FluidType type, int pressure) {
        return 1_000_000_000;
    }
}
