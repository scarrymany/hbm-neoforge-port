package com.hbm.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * A mutable version of {@link Vec3}, with some extra utilities and supports double precision rotation.
 * Simplified for 1.21.1 using public fields instead of Unsafe.
 *
 * @author mlbv, ported to 1.21.1
 */
public class MutableVec3d extends Vec3 implements Cloneable {
    private static final double DEG2RAD = Math.PI / 180.0;

    public MutableVec3d() {
        super(0.0D, 0.0D, 0.0D);
    }

    public MutableVec3d(double x, double y, double z) {
        super(x, y, z);
    }

    public MutableVec3d(@NotNull Vec3 other) {
        super(other.x, other.y, other.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d set(double x, double y, double z) {
        return new MutableVec3d(x, y, z);
    }

    @Contract(mutates = "this")
    public MutableVec3d set(@NotNull Vec3 v) {
        return new MutableVec3d(v.x, v.y, v.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d setX(double x) {
        return new MutableVec3d(x, this.y, this.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d setY(double y) {
        return new MutableVec3d(this.x, y, this.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d setZ(double z) {
        return new MutableVec3d(this.x, this.y, z);
    }

    @Contract(mutates = "this")
    public MutableVec3d zero() {
        return new MutableVec3d(0.0D, 0.0D, 0.0D);
    }

    @Contract(mutates = "this")
    public MutableVec3d addSelf(double dx, double dy, double dz) {
        return new MutableVec3d(this.x + dx, this.y + dy, this.z + dz);
    }

    @Contract(mutates = "this")
    public MutableVec3d addSelf(@NotNull Vec3 v) {
        return addSelf(v.x, v.y, v.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d subSelf(double dx, double dy, double dz) {
        return new MutableVec3d(this.x - dx, this.y - dy, this.z - dz);
    }

    @Contract(mutates = "this")
    public MutableVec3d subSelf(@NotNull Vec3 v) {
        return subSelf(v.x, v.y, v.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d scaleSelf(double s) {
        return new MutableVec3d(this.x * s, this.y * s, this.z * s);
    }

    @Contract(mutates = "this")
    public MutableVec3d mulAddSelf(double s, @NotNull Vec3 v) {
        return new MutableVec3d(this.x + s * v.x, this.y + s * v.y, this.z + s * v.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d normalizeSelf() {
        double len = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        if (len < 1.0E-4D) return new MutableVec3d(0.0D, 0.0D, 0.0D);
        double inv = 1.0D / len;
        return new MutableVec3d(this.x * inv, this.y * inv, this.z * inv);
    }

    @Contract(mutates = "this")
    public MutableVec3d rotateYawSelf(float yaw) {
        double c = Mth.cos(yaw);
        double s = Mth.sin(yaw);
        double nx = this.x * c + this.z * s;
        double nz = this.z * c - this.x * s;
        return new MutableVec3d(nx, this.y, nz);
    }

    @Contract(mutates = "this")
    public MutableVec3d rotatePitchSelf(float pitch) {
        double c = Mth.cos(pitch);
        double s = Mth.sin(pitch);
        double ny = this.y * c + this.z * s;
        double nz = this.z * c - this.y * s;
        return new MutableVec3d(this.x, ny, nz);
    }

    @Contract(mutates = "this")
    public MutableVec3d rotateRollSelf(float roll) {
        double c = Mth.cos(roll);
        double s = Mth.sin(roll);
        double nx = this.x * c + this.y * s;
        double ny = this.y * c - this.x * s;
        return new MutableVec3d(nx, ny, this.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d rotateYawSelf(double yaw) {
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        double nx = this.x * c + this.z * s;
        double nz = this.z * c - this.x * s;
        return new MutableVec3d(nx, this.y, nz);
    }

    @Contract(mutates = "this")
    public MutableVec3d rotatePitchSelf(double pitch) {
        double c = Math.cos(pitch);
        double s = Math.sin(pitch);
        double ny = this.y * c + this.z * s;
        double nz = this.z * c - this.y * s;
        return new MutableVec3d(this.x, ny, nz);
    }

    @Contract(mutates = "this")
    public MutableVec3d rotateRollSelf(double roll) {
        double c = Math.cos(roll);
        double s = Math.sin(roll);
        double nx = this.x * c + this.y * s;
        double ny = this.y * c - this.x * s;
        return new MutableVec3d(nx, ny, this.z);
    }

    @Contract(mutates = "this")
    public MutableVec3d lerpSelf(Vec3 other, double t) {
        double x = this.x + (other.x - this.x) * t;
        double y = this.y + (other.y - this.y) * t;
        double z = this.z + (other.z - this.z) * t;
        return new MutableVec3d(x, y, z);
    }

    @Override
    @Contract("_, _, _ -> new")
    public @NotNull MutableVec3d add(double x, double y, double z) {
        return new MutableVec3d(this.x + x, this.y + y, this.z + z);
    }

    @Override
    @Contract("_ -> new")
    public @NotNull MutableVec3d add(@NotNull Vec3 vec) {
        return new MutableVec3d(this.x + vec.x, this.y + vec.y, this.z + vec.z);
    }

    @Override
    @Contract("_, _, _ -> new")
    public @NotNull MutableVec3d subtract(double x, double y, double z) {
        return new MutableVec3d(this.x - x, this.y - y, this.z - z);
    }

    @Override
    @Contract("_ -> new")
    public @NotNull MutableVec3d subtract(@NotNull Vec3 vec) {
        return new MutableVec3d(this.x - vec.x, this.y - vec.y, this.z - vec.z);
    }

    @Override
    @Contract("_ -> new")
    public @NotNull MutableVec3d scale(double factor) {
        return new MutableVec3d(this.x * factor, this.y * factor, this.z * factor);
    }

    @Override
    @Contract("-> new")
    public @NotNull MutableVec3d normalize() {
        double len = Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        return (len < 1.0E-4D) ? new MutableVec3d(0.0, 0.0, 0.0) : new MutableVec3d(this.x / len, this.y / len, this.z / len);
    }

    @Override
    @Contract("_ -> new")
    public @NotNull MutableVec3d cross(@NotNull Vec3 vec) {
        return new MutableVec3d(this.y * vec.z - this.z * vec.y, this.z * vec.x - this.x * vec.z, this.x * vec.y - this.y * vec.x);
    }

    @Contract("_ -> new")
    public @NotNull MutableVec3d rotateYaw(float yaw) {
        double c = Mth.cos(yaw);
        double s = Mth.sin(yaw);
        double nx = this.x * c + this.z * s;
        double nz = this.z * c - this.x * s;
        return new MutableVec3d(nx, this.y, nz);
    }

    @Contract("_ -> new")
    public @NotNull MutableVec3d rotatePitch(float pitch) {
        double c = Mth.cos(pitch);
        double s = Mth.sin(pitch);
        double ny = this.y * c + this.z * s;
        double nz = this.z * c - this.y * s;
        return new MutableVec3d(this.x, ny, nz);
    }

    @Contract("_ -> new")
    public @NotNull MutableVec3d rotateRoll(float roll) {
        double c = Mth.cos(roll);
        double s = Mth.sin(roll);
        double nx = this.x * c + this.y * s;
        double ny = this.y * c - this.x * s;
        return new MutableVec3d(nx, ny, this.z);
    }

    @Contract("_ -> new")
    public @NotNull MutableVec3d rotateYaw(double yaw) {
        double c = Math.cos(yaw);
        double s = Math.sin(yaw);
        double nx = this.x * c + this.z * s;
        double nz = this.z * c - this.x * s;
        return new MutableVec3d(nx, this.y, nz);
    }

    @Contract("_ -> new")
    public @NotNull MutableVec3d rotatePitch(double pitch) {
        double c = Math.cos(pitch);
        double s = Math.sin(pitch);
        double ny = this.y * c + this.z * s;
        double nz = this.z * c - this.y * s;
        return new MutableVec3d(this.x, ny, nz);
    }

    @Contract("_ -> new")
    public @NotNull MutableVec3d rotateRoll(double roll) {
        double c = Math.cos(roll);
        double s = Math.sin(roll);
        double nx = this.x * c + this.y * s;
        double ny = this.y * c - this.x * s;
        return new MutableVec3d(nx, ny, this.z);
    }

    @Contract("_, _ -> new")
    public @NotNull MutableVec3d lerp(@NotNull Vec3 other, double t) {
        double x = this.x + (other.x - this.x) * t;
        double y = this.y + (other.y - this.y) * t;
        double z = this.z + (other.z - this.z) * t;
        return new MutableVec3d(x, y, z);
    }

    @Contract("-> new")
    public @NotNull Vec3 toImmutable() {
        return new Vec3(this.x, this.y, this.z);
    }

    @Override
    @Contract("-> new")
    public @NotNull MutableVec3d clone() {
        try {
            return (MutableVec3d) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }
}
