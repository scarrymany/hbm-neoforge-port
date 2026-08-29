package com.hbm.util;

import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Vec3dUtil {

    public static Vec3 rotateRoll(Vec3 vec, float roll) {
        float f = Mth.cos(roll);
        float f1 = Mth.sin(roll);
        double d0 = vec.x * (double) f + vec.y * (double) f1;
        double d1 = vec.y * (double) f - vec.x * (double) f1;
        double d2 = vec.z;
        return new Vec3(d0, d1, d2);
    }

    public static Vec3 lerp(Vec3 vec, Vec3 other, double t) {
        double x = vec.x + (other.x - vec.x) * t;
        double y = vec.y + (other.y - vec.y) * t;
        double z = vec.z + (other.z - vec.z) * t;
        return new Vec3(x, y, z);
    }

    public static Vec3i convertToVec3i(Vec3 vec) {
        return new Vec3i(Mth.floor(vec.x), Mth.floor(vec.y), Mth.floor(vec.z));
    }
}
