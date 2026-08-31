package com.hbm.entity.mob;

import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.WormMovementHeadNT} (90 lines, read in full)
 * - see {@code docs/phase4/entities_bosses.md}'s worm-boss table. CE's own file comment (preserved in
 * spirit, not verbatim ASCII art): {@code //TODO: clean-room implementation of the movement behavior
 * classes (again)} - CE's own maintainers flag this class as reimplemented/uncertain, not a
 * battle-tested reference. Ported faithfully anyway (the report does not ask for a redesign), but
 * budget extra test coverage per the report's own recommendation.
 * <p>
 * Two-mode waypoint AI: wanders within +-30/+-10/+-30 blocks of {@code spawnPoint} when idle, or homes
 * toward {@code getTarget()} when one exists - with the burrow/surface state machine described in the
 * report: {@code wasNearGround} gates whether the head chases the target's exact position (when "near
 * ground") or is forced toward a fixed Y=10 cruising altitude first (when not), flipping to
 * {@code wasNearGround = true} once it drops below Y=15, and back to {@code false} at a 1-in-80 roll
 * per tick while above {@code surfaceY} (60) and not already inside an opaque block.
 * <p>
 * Pure movement-composition helper, not an {@code Entity} itself - held by reference inside
 * {@link EntityBOTPrimeHead}, called from {@link EntityBOTPrimeHead#customServerAiStep()}. Package-
 * private field access into {@link EntityWormBaseNT} (same package) mirrors CE's own same-package
 * field access (CE's {@code WormMovementHeadNT} directly reads/writes {@code EntityWormBaseNT}'s
 * package-private/protected fields).
 */
public class WormMovementHeadNT {

    private final EntityWormBaseNT user;

    public WormMovementHeadNT(EntityWormBaseNT user) {
        this.user = user;
    }

    public void updateMovement() {
        double deltaX = this.user.waypointX - this.user.getX();
        double deltaY = this.user.waypointY - this.user.getY();
        double deltaZ = this.user.waypointZ - this.user.getZ();
        double deltaSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;

        Vec3 motion = this.user.getDeltaMovement();
        double mx = motion.x;
        double my = motion.y;
        double mz = motion.z;

        if (this.user.courseChangeCooldown-- <= 0) {
            this.user.courseChangeCooldown += this.user.getRandom().nextInt(5) + 2;
            deltaSq = Math.sqrt(deltaSq);

            if (mx * mx + my * my + mz * mz < this.user.maxSpeed) {
                if (!this.user.isCourseTraversable()) {
                    deltaSq *= 8.0D;
                }

                double moveSpeed = Objects.requireNonNull(this.user.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue();
                mx += deltaX / deltaSq * moveSpeed;
                my += deltaY / deltaSq * moveSpeed;
                mz += deltaZ / deltaSq * moveSpeed;
            }
        }

        if (!this.user.isCourseTraversable()) {
            my -= this.user.fallSpeed;
        }

        if (this.user.dmgCooldown > 0) {
            this.user.dmgCooldown -= 1;
        }

        this.user.aggroCooldown -= 1;

        if (this.user.getTarget() != null) {
            if (this.user.aggroCooldown <= 0) {
                this.user.targetedEntity = this.user.getTarget();
                this.user.aggroCooldown = 20;
            }
        } else if (this.user.targetedEntity == null) {
            this.user.waypointX = this.user.spawnPoint.getX() - 30 + this.user.getRandom().nextInt(60);
            this.user.waypointY = this.user.spawnPoint.getY() - 10 + this.user.getRandom().nextInt(20);
            this.user.waypointZ = this.user.spawnPoint.getZ() - 30 + this.user.getRandom().nextInt(60);
        }

        this.user.setDeltaMovement(mx, my, mz);

        // CE: rotationYaw = -(float)-(atan2(motionX, motionZ) * 180/PI) - the double negation cancels
        // out; rotationPitch is genuinely negated. Neither prevRotation field is touched here (unlike
        // EntityBOTPrimeHead#tick's own instant-snap rotation set) - CE leaves interpolation smoothing
        // to whatever the previous tick's rotation was, faithfully preserved.
        float horiz = (float) Math.sqrt(mx * mx + mz * mz);
        float yaw = (float) (Math.atan2(mx, mz) * 180.0D / Math.PI);
        float pitch = (float) -(Math.atan2(my, horiz) * 180.0D / Math.PI);
        this.user.setYRot(yaw);
        this.user.setXRot(pitch);

        if (this.user.targetedEntity != null && this.user.targetedEntity.distanceToSqr(this.user) < this.user.attackRange * this.user.attackRange) {
            if (this.user.wasNearGround || this.user.canFly) {
                this.user.waypointX = this.user.targetedEntity.getX();
                this.user.waypointY = this.user.targetedEntity.getY();
                this.user.waypointZ = this.user.targetedEntity.getZ();

                if (this.user.getRandom().nextInt(80) == 0 && this.user.getY() > this.user.surfaceY && !this.user.isCourseTraversable()) {
                    this.user.wasNearGround = false;
                }
            } else {
                this.user.waypointX = this.user.targetedEntity.getX();
                this.user.waypointY = 10.0D;
                this.user.waypointZ = this.user.targetedEntity.getZ();

                if (this.user.getY() < 15.0D) {
                    this.user.wasNearGround = true;
                }
            }
        }
    }
}
