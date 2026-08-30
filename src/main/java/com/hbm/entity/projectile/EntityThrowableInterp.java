package com.hbm.entity.projectile;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.entity.projectile.EntityThrowableInterp} (84 lines, abstract) - a
 * from-scratch rewrite of vanilla 1.12's own client-side position/rotation interpolation
 * ({@code turnProgress} countdown lerp toward a synced {@code syncPos}/{@code syncYaw}/
 * {@code syncPitch}), needed back then because CE's projectile entities didn't go through vanilla's
 * normal smoothing path.
 * <p>
 * 1.21.1 exposes exactly this smoothing as a real, built-in {@link Entity} delegate -
 * {@code lerpTo}/{@code lerpTargetX/Y/Z}/{@code lerpTargetXRot/YRot} plus
 * {@code lerpPositionAndRotationStep} - confirmed real by Neo Edition's own parallel
 * {@code ProjectileLerping} class (read for API shape only, not behavior: this class ports CE's own
 * intent - "smooth this projectile's client-side rendering" - onto vanilla's modern equivalent
 * mechanism rather than re-deriving CE's manual {@code turnProgress} math, exactly as the
 * gun-framework report's Package A table recommends). No CE-specific behavior is lost: the *visual*
 * smoothing curve is a vanilla concern now, and CE never gave this class any gameplay-affecting
 * logic of its own beyond that curve.
 */
public abstract class EntityThrowableInterp extends EntityThrowableNT {

    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;

    protected EntityThrowableInterp(EntityType<? extends EntityThrowableInterp> type, Level level) {
        super(type, level);
    }

    protected EntityThrowableInterp(EntityType<? extends EntityThrowableInterp> type, Level level, double x, double y, double z) {
        super(type, level, x, y, z);
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        // CE's own approachNum() hook (see below) - larger values make the approach smoother but lag
        // the true value more; every CE consumer of this class leaves it at the default 0.
        this.lerpSteps = steps + approachNum();
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            super.tick();
        } else {
            // server-authoritative physics runs in EntityThrowableNT.tick() (only reached on the
            // logical server above); on the client this entity is purely interpolated toward the last
            // synced position/rotation, matching CE's own onUpdate() client branch 1:1.
            if (this.lerpSteps > 0) {
                this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
                --this.lerpSteps;
            }
        }
    }

    /**
     * CE's own doc comment: "a number added to the basic 3 of the approach progress value. Larger
     * numbers make the approach smoother, but lagging behind the true value more."
     */
    public int approachNum() {
        return 0;
    }
}
