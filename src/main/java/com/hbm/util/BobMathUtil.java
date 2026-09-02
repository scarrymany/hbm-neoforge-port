package com.hbm.util;

import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Partial port of CE's {@code com.hbm.util.BobMathUtil} - a large grab-bag math-helper class.
 * Only the pure functions the gun-framework packages actually call are ported here
 * ({@link #getCrossAngle}, used by {@code BulletConfig}'s standard ricochet lambda to test a
 * glancing-blow angle; {@link #interp}, used by {@code EntityBulletBaseMK4}'s lockon-homing turn-rate
 * ramp; {@link #min(int, int, int)}/{@link #getShortNumber}/{@link #getBlink}, added by the held-
 * weapon state-machine package for {@code MagazineSingleTypeBase}'s reload-amount clamping,
 * {@code MagazineEnergy}'s HUD text, and {@code ItemGunBaseNT}'s SECRET/DEBUG tooltip blink
 * respectively). Whichever future package needs another CE {@code BobMathUtil} member should add it
 * here rather than re-deriving it elsewhere - this is a per-package partial port, not a claim that
 * the rest of CE's {@code BobMathUtil} doesn't exist/isn't needed.
 */
public class BobMathUtil {

    /**
     * Angle (0-90 degrees) between two vectors, folded so a head-on hit reads as 90 and a
     * perfectly grazing hit reads as 0 - CE's own "how glancing was this hit" test, ported verbatim
     * (including the {@code >= 180} fold, which in practice never triggers for two normalized
     * vectors' dot-product-derived angle since {@code acos} only ever returns 0-180, but is kept
     * exactly as CE has it rather than "cleaned up" into a no-op removal).
     */
    public static double getCrossAngle(Vec3 vel, Vec3 rel) {
        Vec3 v = vel.normalize();
        Vec3 r = rel.normalize();

        double angle = Math.toDegrees(Math.acos(v.dot(r)));

        if (angle >= 180) angle -= 180;

        return angle;
    }

    /** Linear interpolation between x and y at fraction `interp` (0 = x, 1 = y). */
    public static double interp(double x, double y, float interp) {
        return x + (y - x) * interp;
    }

    /** CE {@code BobMathUtil.partialTick} — lerp used by Sexy/MK108 belt shell placement. */
    public static double partialTick(double a, double b, double t) {
        return a + (b - a) * t;
    }

    /** Smallest of three ints - CE's own overload used by {@code MagazineSingleTypeBase.standardReload}'s reload-amount clamp. */
    public static int min(int a, int b, int c) {
        return Math.min(a, Math.min(b, c));
    }

    /** CE {@code BobMathUtil.min(double...)} — torus power/fuel factor. */
    public static double min(double... values) {
        double m = values[0];
        for (int i = 1; i < values.length; i++) m = Math.min(m, values[i]);
        return m;
    }

    /** Abbreviated large-number formatting (1.5k/2.3M/...), matching CE's own helper used by ammo/energy HUD text. */
    public static String getShortNumber(long number) {
        if (number < 1000) return String.valueOf(number);
        if (number < 1_000_000) return String.format(Locale.US, "%.2fk", number / 1000.0);
        if (number < 1_000_000_000) return String.format(Locale.US, "%.2fM", number / 1_000_000.0);
        if (number < 1_000_000_000_000L) return String.format(Locale.US, "%.2fG", number / 1_000_000_000.0);
        return String.format(Locale.US, "%.2fT", number / 1_000_000_000_000.0);
    }

    /** Half-second on/off blink, used by CE's SECRET/DEBUG weapon-quality tooltip flash. */
    public static boolean getBlink() {
        return (System.currentTimeMillis() / 500L) % 2 == 0;
    }
}
