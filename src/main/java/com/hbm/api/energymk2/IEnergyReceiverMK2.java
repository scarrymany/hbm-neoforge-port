package com.hbm.api.energymk2;

import com.hbm.lib.DirPos;
import com.hbm.util.Compat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * If it receives energy, use this.
 */
public interface IEnergyReceiverMK2 extends IEnergyHandlerMK2 {

    /**
     * Transfers a specified amount of energy to this receiver.
     * If the receiver has enough capacity, all the energy is absorbed.
     * Otherwise, it absorbs as much as it can and returns the excess energy.
     *
     * @param power    The amount of energy to transfer.
     * @param simulate If true, the transfer is simulated and no energy is actually transferred.
     * @return The amount of energy that could not be absorbed (excess energy), or 0 if all energy was absorbed.
     */
    default long transferPower(long power, boolean simulate) {
        if (power + this.getPower() <= this.getMaxPower()) {
            if (!simulate) this.setPower(power + this.getPower());
            return 0;
        }
        long capacity = this.getMaxPower() - this.getPower();
        long overshoot = power - capacity;
        if (!simulate) this.setPower(this.getMaxPower());
        return overshoot;
    }

    /**
     * Retrieves the maximum speed at which this energy receiver can accept energy.
     * By default, it returns the maximum power capacity of the receiver.
     */
    default long getReceiverSpeed() {
        return this.getMaxPower();
    }

    /** Whether a provider can provide power by touching the block (i.e. via proxies), bypassing the need for a network entirely */
    default boolean allowDirectProvision() {
        return true;
    }

    default void trySubscribe(Level level, DirPos pos) {
        trySubscribe(level, pos.getPos(), pos.getDir());
    }

    default void trySubscribe(Level level, BlockPos pos, Direction dir) {
        trySubscribe(level, pos.getX(), pos.getY(), pos.getZ(), dir);
    }

    default void trySubscribe(Level level, int x, int y, int z, Direction dir) {
        BlockEntity be = Compat.getBlockEntityStandard(level, new BlockPos(x, y, z));

        if (be instanceof IEnergyConductorMK2 con) {
            if (!con.canConnect(dir.getOpposite())) return;

            Nodespace.PowerNode node = Nodespace.getNode(level, new BlockPos(x, y, z));

            if (node != null && node.net != null) {
                node.net.addReceiver(this);
            }
        }
    }

    default void tryUnsubscribe(Level level, int x, int y, int z) {
        BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));

        if (be instanceof IEnergyConductorMK2 con) {
            Nodespace.PowerNode node = con.createNode();

            if (node != null && node.net != null) {
                node.net.removeReceiver(this);
            }
        }
    }

    default ConnectionPriority getPriority() {
        return ConnectionPriority.NORMAL;
    }

    /**
     * More is better-er.
     */
    enum ConnectionPriority {
        LOWEST,
        LOW,
        NORMAL,
        HIGH,
        HIGHEST;

        public static final ConnectionPriority[] VALUES = values();
    }
}
