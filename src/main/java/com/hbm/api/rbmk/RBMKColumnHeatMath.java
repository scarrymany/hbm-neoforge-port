package com.hbm.api.rbmk;

/**
 * Pure heat-diffusion/passive-cooling math for the RBMK column grid, extracted from CE's
 * {@code TileEntityRBMKBase#moveHeat()}/{@code #passiveCooling(int)}/{@code #coolPassively(int)}
 * (read in full - see docs/phase2/rbmk_reactor.md). CE's own version gathers the four cardinal
 * neighbor tile entities and mutates their {@code heat} fields directly inline with the
 * world/neighbor-cache lookups; this class only does the arithmetic, operating on plain
 * {@code double}s so it is directly unit-testable with no {@code BlockEntity}/world access at all.
 * The column-blocks package's column base class is expected to gather neighbor heats (its own
 * neighbor-cache concern, out of this package's scope) and call these methods with the results.
 */
public final class RBMKColumnHeatMath {

    private RBMKColumnHeatMath() {
    }

    /**
     * CE: {@code TileEntityRBMKBase#passiveCooling(int)}. Interpolates between an "inner" minimum
     * (surrounded by neighbors, well-insulated) and an "outer" maximum (fully isolated, radiates
     * freely) heat-per-tick loss.
     *
     * @param neighbors    number of present orthogonal neighbors, clamped internally to {@code [0;4]}
     * @param coolingInner {@link RBMKDials#getPassiveCoolingInner}
     * @param coolingOuter {@link RBMKDials#getPassiveCooling}
     */
    public static double passiveCooling(int neighbors, double coolingInner, double coolingOuter) {
        int clamped = Math.max(0, Math.min(neighbors, 4));
        return coolingInner + (coolingOuter - coolingInner) * ((4 - clamped) / 4D);
    }

    /**
     * CE: the final step of {@code TileEntityRBMKBase#coolPassively(int)} - subtracts the passive
     * cooling amount and floors at ambient temperature (20°C).
     */
    public static double applyPassiveCooling(double heat, double coolingAmount) {
        double result = heat - coolingAmount;
        return Math.max(result, 20D);
    }

    /**
     * CE: {@code TileEntityRBMKBase#moveHeat()}'s equalization step - NOT a literal
     * conduction/diffusion PDE, but an exponential move-toward-the-group-average: every member
     * (including "self", which CE always includes in its own average) moves {@code stepSize}
     * of the way toward the plain average of the whole group's current heats, every tick.
     *
     * @param heats    every member's current heat, in any order; the returned array preserves that order
     * @param stepSize {@link RBMKDials#getColumnHeatFlow}, {@code [0;1]} (0 = no movement, 1 = instantly snap to the average)
     * @return a new array, one equalized heat value per input entry, same order
     */
    public static double[] equalizeHeat(double[] heats, double stepSize) {
        if (heats.length == 0) {
            return heats;
        }
        double total = 0;
        for (double h : heats) {
            total += h;
        }
        double target = total / heats.length;

        double[] out = new double[heats.length];
        for (int i = 0; i < heats.length; i++) {
            out[i] = heats[i] + (target - heats[i]) * stepSize;
        }
        return out;
    }
}
