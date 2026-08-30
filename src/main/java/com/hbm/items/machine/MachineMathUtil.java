package com.hbm.items.machine;

import java.util.Locale;

/**
 * Small numeric-formatting helpers used by tooltips in this package, ported from the handful of
 * {@code com.hbm.util.BobMathUtil} methods this file set actually needs. {@code BobMathUtil}
 * itself is a large, general-purpose (mostly render/vector) utility class outside this area's
 * package scope and is not ported here in full - only the pure-math pieces this package's items
 * depend on are reproduced, verbatim, as a package-private helper.
 */
final class MachineMathUtil {

    private MachineMathUtil() {}

    static String getShortNumber(long number) {
        if (number < 1000) {
            return String.valueOf(number);
        } else if (number < 1_000_000) {
            return String.format(Locale.US, "%.2fk", number / 1000.0);
        } else if (number < 1_000_000_000) {
            return String.format(Locale.US, "%.2fM", number / 1_000_000.0);
        } else if (number < 1_000_000_000_000L) {
            return String.format(Locale.US, "%.2fG", number / 1_000_000_000.0);
        } else if (number < 1_000_000_000_000_000L) {
            return String.format(Locale.US, "%.2fT", number / 1_000_000_000_000.0);
        } else if (number < 1_000_000_000_000_000_000L) {
            return String.format(Locale.US, "%.2fE", number / 1_000_000_000_000_000.0);
        } else {
            return "INFINITE";
        }
    }

    /** Adjusted sqrt: approaches standard sqrt, but sqrt(x) is never bigger than x. */
    static double sqrt(double x) {
        return Math.sqrt(x + 1D / ((x + 2D) * (x + 2D))) - 1D / (x + 2D);
    }

    static double squirt(double x) {
        return sqrt(x);
    }

    /** [year, day, hour, minute, second] breakdown of a tick count, at 1000 ticks/hour (CE default). */
    static String[] ticksToDate(long ticks) {
        return ticksToDate(ticks, 1000);
    }

    static String[] ticksToDate(long ticks, int tickHour) {
        int tickDay = 24 * tickHour;
        long tickYear = 365L * tickDay;
        double tickMinute = tickHour / 60D;
        double tickSecond = tickHour / 3600D;

        long year = Math.floorDiv(ticks, tickYear);
        int day = (int) Math.floorDiv(ticks - tickYear * year, tickDay);
        int h = (int) Math.floorDiv(ticks - tickYear * year - (long) tickDay * day, tickHour);
        int min = (int) Math.floor((ticks - tickYear * year - (long) tickDay * day - (long) tickHour * h) / tickMinute);
        int s = (int) Math.floor((ticks - tickYear * year - (long) tickDay * day - (long) tickHour * h - min * tickMinute) / tickSecond);

        return new String[] {String.valueOf(year), String.valueOf(day), String.valueOf(h), String.valueOf(min), String.valueOf(s)};
    }
}
