package com.hbm.api.rbmk;

import net.minecraft.server.level.ServerLevel;

/**
 * Pure meltdown-<b>trigger</b> threshold logic, extracted from CE's
 * {@code TileEntityRBMKRod#update()}. Does NOT perform a meltdown - see
 * {@link IRBMKMeltdownHandler}'s javadoc for why, and what does.
 * <p>
 * <b>Two independent overheat thresholds exist in CE and must not be collapsed into one</b> (see
 * docs/phase2/rbmk_reactor.md's "Two independent overheat thresholds interact" open question):
 * <ol>
 *   <li>The fuel item's own {@code ItemRBMKRod#meltingPoint} (per fuel type, default 1000°C).
 *   Crossing it only triggers an internal core/hull/column heat equalization inside
 *   {@code ItemRBMKRod#provideHeat} (ported unchanged in this package's {@code ItemRBMKRod}) - it
 *   does NOT by itself trigger a meltdown event, and has nothing to do with this class.</li>
 *   <li>The column's own {@link IRBMKColumn#maxHeat()} (hardcoded {@code 1500D} in CE for every
 *   column type). THIS is the real meltdown trigger - checked once per tick by the column's own
 *   tick loop (owned by the column-blocks package) via {@link #checkAndFire}.</li>
 * </ol>
 */
public final class RBMKMeltdownTrigger {

    private RBMKMeltdownTrigger() {
    }

    /**
     * CE: {@code this.heat > this.maxHeat()}, the condition guarding the {@code meltdown()} call
     * inside {@code TileEntityRBMKRod#update()} - the only place in CE that ever calls
     * {@code meltdown()} from normal gameplay (the other call site, {@code ItemDyatlov}, is a
     * player-triggered debug/cheat tool, out of this package's scope).
     */
    public static boolean isOverheated(IRBMKColumn column) {
        return column.getHeat() > column.maxHeat();
    }

    /**
     * Checks the trigger condition and, if crossed and meltdowns are not globally disabled
     * ({@link RBMKDials#getMeltdownsDisabled}), invokes {@code handler}.
     * <p>
     * <b>Caller contract</b> (preserved from CE's own call site): when this returns {@code true} -
     * regardless of whether {@code handler} actually fired - the calling column must discard this
     * tick's flux/heat output exactly as CE's {@code TileEntityRBMKRod#update()} does
     * ({@code lastFluxRatio = lastFluxQuantity = fluxQuantity = 0;} and return without spreading
     * flux this tick). When meltdowns ARE globally disabled, CE substitutes a purely cosmetic gas-
     * flame particle effect ({@code ParticleUtil.spawnGasFlame}) in place of calling
     * {@code meltdown()} - that effect is a world/rendering concern out of this package's scope, so
     * {@code handler} is simply not invoked in that case; the caller may add its own idle-effect
     * substitute if desired.
     *
     * @return {@code true} if the column is overheated, whether or not {@code handler} fired
     */
    public static boolean checkAndFire(ServerLevel level, IRBMKColumn column, IRBMKMeltdownHandler handler) {
        if (!isOverheated(column)) {
            return false;
        }
        if (!RBMKDials.getMeltdownsDisabled(level) && handler != null) {
            handler.onMeltdownTriggered(level, column.getRbmkPos(), column);
        }
        return true;
    }
}
