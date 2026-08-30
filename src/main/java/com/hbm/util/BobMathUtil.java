package com.hbm.util;

import net.minecraft.world.phys.Vec3;

/**
 * Partial port of CE's {@code com.hbm.util.BobMathUtil} - a large grab-bag math-helper class.
 * Only the two pure functions the gun-framework ballistics core actually calls are ported here
 * ({@link #getCrossAngle}, used by {@code BulletConfig}'s standard ricochet lambda to test a
 * glancing-blow angle; {@link #interp}, used by {@code EntityBulletBaseMK4}'s lockon-homing turn-rate
 * ramp). Whichever future package needs another CE {@code BobMathUtil} member should add it here
 * rather than re-deriving it elsewhere - this is a per-package partial port, not a claim that the
 * rest of CE's {@code BobMathUtil} doesn't exist/isn't needed.
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
}
