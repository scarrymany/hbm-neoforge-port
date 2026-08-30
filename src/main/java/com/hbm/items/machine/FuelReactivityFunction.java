package com.hbm.items.machine;

import net.minecraft.ChatFormatting;

import java.util.Locale;

/**
 * Reactivity-curve descriptor used by {@link ItemPWRFuel} and {@link ItemWatzPellet} tooltips. A
 * package-local reproduction of the handful of {@code com.hbm.util.Function} subclasses those two
 * item classes actually use - {@code Function} itself is a general-purpose util class outside this
 * area's package scope (see {@link MachineMathUtil} for the same reasoning applied to
 * {@code BobMathUtil}).
 */
abstract class FuelReactivityFunction {

    private double div = 1D;
    private double off = 0D;

    abstract double effonix(double x);

    abstract String getLabelForFuel();

    abstract String getDangerFromFuel();

    FuelReactivityFunction withDiv(double div) {
        this.div = div;
        return this;
    }

    FuelReactivityFunction withOff(double off) {
        this.off = off;
        return this;
    }

    double getX(double x) {
        return x / div + off;
    }

    String getXName(boolean brackets) {
        String x = "x";
        if (div != 1D) x += " / " + String.format(Locale.US, "%,.1f", div);
        if (off != 0D) x += " + " + String.format(Locale.US, "%,.1f", off);
        return x;
    }

    static class Logarithmic extends FuelReactivityFunction {
        private final double level;

        Logarithmic(double level) {
            this.level = level;
            this.withOff(1D);
        }

        @Override
        double effonix(double x) {
            return Math.log10(getX(x)) * level;
        }

        @Override
        String getLabelForFuel() {
            return "log10(" + getXName(false) + ") * " + String.format(Locale.US, "%,.1f", level);
        }

        @Override
        String getDangerFromFuel() {
            return ChatFormatting.YELLOW + "MEDIUM / LOGARITHMIC";
        }
    }

    static class Sqrt extends FuelReactivityFunction {
        private final double level;

        Sqrt(double level) {
            this.level = level;
        }

        @Override
        double effonix(double x) {
            return MachineMathUtil.sqrt(getX(x)) * level;
        }

        @Override
        String getLabelForFuel() {
            return "sqrt(" + getXName(false) + ") * " + String.format(Locale.US, "%,.3f", level);
        }

        @Override
        String getDangerFromFuel() {
            return ChatFormatting.YELLOW + "MEDIUM / SQUARE ROOT";
        }
    }

    static class SqrtFalling extends Sqrt {
        SqrtFalling(double fallFactor) {
            super(1D / fallFactor);
            this.withOff(fallFactor * fallFactor);
        }
    }

    static class Linear extends FuelReactivityFunction {
        private final double level;

        Linear(double level) {
            this.level = level;
        }

        @Override
        double effonix(double x) {
            return getX(x) * level;
        }

        @Override
        String getLabelForFuel() {
            return getXName(true) + " * " + String.format(Locale.US, "%,.1f", level);
        }

        @Override
        String getDangerFromFuel() {
            return ChatFormatting.RED + "DANGEROUS / LINEAR";
        }
    }

    static class Quadratic extends FuelReactivityFunction {
        private final double level;
        private final double vOff;

        Quadratic(double level, double vOff) {
            this.level = level;
            this.vOff = vOff;
        }

        @Override
        double effonix(double x) {
            return getX(x) * getX(x) * level + vOff;
        }

        @Override
        String getLabelForFuel() {
            return getXName(true) + "² * " + String.format(Locale.US, "%,.1f", level)
                    + (vOff != 0 ? (" + " + String.format(Locale.US, "%,.1f", vOff)) : "");
        }

        @Override
        String getDangerFromFuel() {
            return ChatFormatting.RED + "DANGEROUS / QUADRATIC";
        }
    }
}
