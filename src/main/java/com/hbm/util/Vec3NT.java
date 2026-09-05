package com.hbm.util;

import net.minecraft.util.Mth;

/**
 * Simplified mutable vector for Floodlight beam calculation.
 */
public class Vec3NT {
    public double x;
    public double y;
    public double z;

    public Vec3NT(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3NT rotateRollSelf(float roll) {
        double c = Mth.cos(roll);
        double s = Mth.sin(roll);
        double nx = this.x * c + this.y * s;
        double ny = this.y * c - this.x * s;
        this.x = nx;
        this.y = ny;
        return this;
    }

    public Vec3NT rotateYawSelf(float yaw) {
        double c = Mth.cos(yaw);
        double s = Mth.sin(yaw);
        double nx = this.x * c + this.z * s;
        double nz = this.z * c - this.x * s;
        this.x = nx;
        this.z = nz;
        return this;
    }
}
