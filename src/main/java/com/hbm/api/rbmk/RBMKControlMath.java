package com.hbm.api.rbmk;

/**
 * Pure control-rod math extracted from CE's {@code TileEntityRBMKControlManual}/
 * {@code TileEntityRBMKControlAuto} (both read in full - see docs/phase2/rbmk_reactor.md).
 */
public final class RBMKControlMath {

    private RBMKControlMath() {
    }

    /**
     * CE: {@code TileEntityRBMKControlManual#getMult()}'s inline surge math, extracted as a pure
     * function - docs/phase2/rbmk_reactor.md calls this out by name as "one of the highest-value
     * functions to unit test given the project's own framing".
     * <p>
     * Withdrawing a control rod that was previously more inserted ({@code targetLevel < startingLevel})
     * produces a transient EXTRA flux multiplier on top of {@code level} itself, active only while
     * {@code level} is still close to its old (higher) value - the Chernobyl-reference positive
     * void/scram-coefficient effect. {@code pow(1 - level, 15)} makes the {@code sin} argument
     * swing from near-0 to near-pi almost entirely within the last few percent of the rod's outward
     * travel, so the surge appears as a sharp pulse right as withdrawal starts and vanishes almost
     * immediately after.
     *
     * @param level         current raw extraction level {@code [0;1]}
     * @param startingLevel the level the rod was at when the current withdrawal/insertion command was issued
     * @param targetLevel   the level the rod is now moving toward
     * @param surgeMod      {@link RBMKDials#getSurgeMod}
     * @return the effective flux multiplier: {@code level} plus the transient surge, if any
     */
    public static double getEffectiveMult(double level, double startingLevel, double targetLevel, double surgeMod) {
        double surge = 0D;
        if (targetLevel < startingLevel && Math.abs(level - targetLevel) > 0.01D) {
            surge = Math.sin(Math.pow(1D - level, 15) * Math.PI) * (startingLevel - targetLevel) * surgeMod;
        }
        return level + surge;
    }

    /** CE: {@code TileEntityRBMKControlAuto.RBMKFunction}, extracted here since the interpolation math it selects is now a pure shared function (see {@link #autoLevel}). */
    public enum AutoFunction {
        LINEAR,
        QUAD_UP,
        QUAD_DOWN
    }

    /**
     * CE: {@code TileEntityRBMKControlAuto#update()}'s three heat-setpoint interpolation shapes,
     * extracted as one pure function. Maps {@code heat} within {@code [heatLower;heatUpper]}
     * (order-independent - CE takes {@code min}/{@code max} of the two bounds first) onto
     * {@code [levelLower;levelUpper]}; outside that range the result clamps to whichever endpoint
     * is closer, matching CE's own explicit lower/upper-bound branches exactly.
     */
    public static double autoLevel(double heat, double heatLower, double heatUpper,
                                    double levelLower, double levelUpper, AutoFunction function) {
        double lowerBound = Math.min(heatLower, heatUpper);
        double upperBound = Math.max(heatLower, heatUpper);

        if (heat < lowerBound) {
            return levelLower;
        }
        if (heat > upperBound) {
            return levelUpper;
        }

        return switch (function) {
            case LINEAR -> (heat - heatLower) * ((levelUpper - levelLower) / (heatUpper - heatLower)) + levelLower;
            case QUAD_UP -> Math.pow((heat - heatLower) / (heatUpper - heatLower), 2) * (levelUpper - levelLower) + levelLower;
            case QUAD_DOWN -> Math.pow((heat - heatUpper) / (heatLower - heatUpper), 2) * (levelLower - levelUpper) + levelUpper;
        };
    }
}
