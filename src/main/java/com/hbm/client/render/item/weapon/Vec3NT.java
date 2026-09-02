package com.hbm.client.render.item.weapon;

/**
 * Tiny mutable vector used by CE Sexy/MK108 belt layout
 * ({@code com.hbm.render.ntm.Vec3NT} / CE {@code Vec3NT.rotateAroundZDeg}).
 */
final class Vec3NT {
    double x;
    double y;
    double z;

    Vec3NT(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    void rotateAroundZDeg(double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        double nx = x * cos - y * sin;
        double ny = x * sin + y * cos;
        this.x = nx;
        this.y = ny;
    }
}
