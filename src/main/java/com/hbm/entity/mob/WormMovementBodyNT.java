package com.hbm.entity.mob;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.WormMovementBodyNT} (70 lines, read in full)
 * - see {@code docs/phase4/entities_bosses.md}'s worm-boss table. "Follow the entity ahead of you in
 * the chain" logic: every 60 ticks (or on tick 1), re-resolves {@link EntityWormBaseNT#followed}/
 * {@code targetedEntity} by scanning every {@link EntityWormBaseNT} within {@code rangeForParts} (70
 * blocks) for the segment whose {@code partNumber == thisPartNumber - 1} (or the head, for segment 0)
 * - this scan-and-relink, not a stored reference, is what lets the chain self-heal if a segment
 * despawns/reloads out of order.
 * <p>
 * Speed is clamped to {@code min(distanceToTarget - segmentDistance, maxBodySpeed)} - the chain has
 * slack: a segment does not move at all once within {@code segmentDistance * 0.895} (~3.13 blocks) of
 * its forward link, so the worm can compress/bunch up around a stationary head rather than always
 * maintaining exact string-of-pearls spacing.
 * <p>
 * CE calls {@code faceEntity(targetedEntity, 180, 180)} here, which resolves to
 * {@link EntityWormBaseNT}'s own no-op override on this hierarchy (the worm's facing is driven
 * entirely by this movement helper's own rotation math elsewhere, e.g.
 * {@link EntityBOTPrimeBody#tick()}) - omitted here rather than calling a guaranteed no-op, with zero
 * observable behavior difference.
 */
public class WormMovementBodyNT {

    private static final double TARGETING_RANGE = 128.0D;

    private final EntityWormBaseNT user;

    public WormMovementBodyNT(EntityWormBaseNT user) {
        this.user = user;
    }

    public void updateMovement() {
        if (this.user.targetedEntity != null
                && this.user.targetedEntity.distanceToSqr(this.user) < TARGETING_RANGE * TARGETING_RANGE) {
            this.user.waypointX = this.user.targetedEntity.getX();
            this.user.waypointY = this.user.targetedEntity.getY();
            this.user.waypointZ = this.user.targetedEntity.getZ();
        }

        if ((this.user.tickCount % 60 == 0 || this.user.tickCount == 1)
                && (this.user.targetedEntity == null || this.user.followed == null)) {
            findEntityToFollow(this.user.level().getEntitiesOfClass(EntityWormBaseNT.class,
                    this.user.getBoundingBox().inflate(this.user.rangeForParts), EntityWormBaseNT.WORM_SELECTOR));
        }

        double deltaX = this.user.waypointX - this.user.getX();
        double deltaY = this.user.waypointY - this.user.getY();
        double deltaZ = this.user.waypointZ - this.user.getZ();
        double deltaDist = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        this.user.bodySpeed = Math.max(0.0D, Math.min(deltaDist - this.user.segmentDistance, this.user.maxBodySpeed));

        if (deltaDist < this.user.segmentDistance * 0.895D) {
            Vec3 motion = this.user.getDeltaMovement();
            this.user.setDeltaMovement(motion.scale(0.8D));
        } else {
            this.user.setDeltaMovement(
                    deltaX / deltaDist * this.user.bodySpeed,
                    deltaY / deltaDist * this.user.bodySpeed,
                    deltaZ / deltaDist * this.user.bodySpeed);
        }
    }

    private void findEntityToFollow(List<EntityWormBaseNT> segments) {
        for (EntityWormBaseNT segment : segments) {
            if (segment.getHeadID() != this.user.getHeadID()) continue;

            if (segment.getIsHead()) {
                if (this.user.getPartNumber() == 0) {
                    this.user.targetedEntity = segment;
                }
                this.user.followed = segment;
            } else if (segment.getPartNumber() == this.user.getPartNumber() - 1) {
                this.user.targetedEntity = segment;
            }
        }
        this.user.didCheck = true;
    }
}
