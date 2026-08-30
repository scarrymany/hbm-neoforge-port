package com.hbm.api.fluidmk2;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * {@link IFluidReceiverMK2} with a standard tank-array(-turned-{@link List})-backed implementation
 * of transfer/demand. Ported unchanged from CE aside from {@code FluidTankNTM[]} -&gt;
 * {@code List<FluidTankNTM>} (see {@link IFluidUserMK2}'s javadoc). Named directly in this port's
 * already-committed {@code NTMFluidHandlerWrapper} javadoc ("every existing implementer either
 * overrides this properly (IFluidStandardReceiverMK2, scanning its own getReceivingTanks())...") -
 * that reference is what this class fulfils.
 *
 * @author hbm
 */
public interface IFluidStandardReceiverMK2 extends IFluidReceiverMK2 {

    @NotNull List<FluidTankNTM> getReceivingTanks();

    @Override
    default long getDemand(FluidType type, int pressure) {
        long amount = 0;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) amount += (tank.getMaxFill() - tank.getFill());
        }
        return amount;
    }

    @Override
    default long transferFluid(FluidType type, int pressure, long amount) {
        int tanks = 0;
        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) tanks++;
        }
        if (tanks > 1) {
            int firstRound = (int) Math.floor((double) amount / (double) tanks);
            for (FluidTankNTM tank : getReceivingTanks()) {
                if (tank.getTankType() == type && tank.getPressure() == pressure) {
                    int toAdd = Math.min(firstRound, tank.getMaxFill() - tank.getFill());
                    tank.setFill(tank.getFill() + toAdd);
                    amount -= toAdd;
                }
            }
        }
        if (amount > 0) for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getTankType() == type && tank.getPressure() == pressure) {
                int toAdd = (int) Math.min(amount, tank.getMaxFill() - tank.getFill());
                tank.setFill(tank.getFill() + toAdd);
                amount -= toAdd;
            }
        }
        return amount;
    }

    @Override
    default int[] getReceivingPressureRange(FluidType type) {
        int lowest = HIGHEST_VALID_PRESSURE;
        int highest = 0;

        for (FluidTankNTM tank : getReceivingTanks()) {
            if (tank.getTankType() == type) {
                if (tank.getPressure() < lowest) lowest = tank.getPressure();
                if (tank.getPressure() > highest) highest = tank.getPressure();
            }
        }

        return lowest <= highest ? new int[] {lowest, highest} : DEFAULT_PRESSURE_RANGE;
    }

    @Override
    default long getReceiverSpeed(FluidType type, int pressure) {
        return 1_000_000_000;
    }
}
