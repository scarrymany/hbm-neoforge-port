package com.hbm.api.energymk2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * If it sends energy, use this.
 * <p>
 * Note: CE also bridges leftover power into Forge Energy on non-HBM neighbors here. That bridge
 * is intentionally deferred to a later phase behind a config flag - see the class javadoc on
 * {@link PowerNetMK2} for the rationale. Only the native HE paths (network conductor, direct
 * receiver) are ported for Phase 0.
 */
public interface IEnergyProviderMK2 extends IEnergyHandlerMK2 {

    /**
     * Uses up available power, default implementation has no sanity checking, make sure that the
     * requested power is lequal to the current power.
     *
     * @param power The amount of power to use. Ensure this value is less than or equal to the current power.
     */
    default void usePower(long power) {
        this.setPower(this.getPower() - power);
    }

    /**
     * Retrieves the maximum speed at which the energy provider can send energy.
     * By default, this method returns the maximum power capacity of the provider.
     */
    default long getProviderSpeed() {
        return this.getMaxPower();
    }

    /**
     * Attempts to provide energy to a target block entity at specific coordinates.
     * It checks for HBM's native energy interfaces: a conductor joining this provider onto its
     * network, or a receiver willing to accept power directly (bypassing the network entirely).
     *
     * @param level The level.
     * @param x     The x-coordinate of the <b>target block entity</b> (the potential receiver).
     * @param y     The y-coordinate of the <b>target block entity</b>.
     * @param z     The z-coordinate of the <b>target block entity</b>.
     * @param dir   The {@link Direction} from this provider to the target block entity.
     */
    default void tryProvide(Level level, int x, int y, int z, Direction dir) {
        BlockPos targetPos = new BlockPos(x, y, z);
        BlockEntity targetBe = level.getBlockEntity(targetPos);

        if (targetBe == null) return;

        if (targetBe instanceof IEnergyConductorMK2 con) {
            if (con.canConnect(dir.getOpposite())) {
                Nodespace.PowerNode node = Nodespace.getNode(level, targetPos);
                if (node != null && node.net != null) {
                    node.net.addProvider(this);
                }
            }
        }

        if (targetBe instanceof IEnergyReceiverMK2 rec && targetBe != this) {
            if (rec.canConnect(dir.getOpposite()) && rec.allowDirectProvision()) {
                long canProvide = Math.min(this.getPower(), this.getProviderSpeed());
                long canReceive = Math.min(rec.getMaxPower() - rec.getPower(), rec.getReceiverSpeed());
                long toTransfer = Math.min(canProvide, canReceive);

                if (toTransfer > 0) {
                    long rejected = rec.transferPower(toTransfer, false);
                    long accepted = toTransfer - rejected;
                    if (accepted > 0) this.usePower(accepted);
                }
            }
        }
    }

    default void tryProvide(Level level, BlockPos pos, Direction dir) {
        tryProvide(level, pos.getX(), pos.getY(), pos.getZ(), dir);
    }
}
