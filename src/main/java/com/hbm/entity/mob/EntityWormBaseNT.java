package com.hbm.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.EntityWormBaseNT} (202 lines, read in full) -
 * see {@code docs/phase4/entities_bosses.md}'s worm-boss table. The shared worm-segment contract:
 * {@link #headID}/{@link #partNum} (plain, unsynced {@code int} fields - every segment resolves its
 * head via a live {@link Level#getEntity(int)} lookup, exactly like CE's {@code world.getEntityByID}).
 * The report flags this live-lookup pattern as worth reconsidering for save/reload robustness (a raw
 * int id is not guaranteed stable across a chunk unload/reload the way a {@code UUID} would be) but
 * does <b>not</b> require a synced-UUID rewrite - ported faithfully here, see this package's own
 * knownGaps for the save-reload caveat.
 * <p>
 * <b>The damage-redirect mechanism</b> ({@link #hurt}): damage dealt to any body segment is redirected
 * to {@link #targetedEntity} (its forward chain-link - for the head-most body segment this is the head
 * itself) unless the attacker is part of the same worm (same {@link #headID}). This - not a separate
 * aggregation step - is the actual mechanism that makes "hit any segment, damage flows toward the
 * head" work (see the research report's Headline finding #2).
 * <p>
 * <b>Naming trap carried over from CE</b> (report's own Open questions): {@link #targetedEntity} means
 * two different things depending on subclass - for {@link EntityBOTPrimeHead} it is the entity being
 * actively attacked (fed from vanilla's own {@code getTarget()} by {@link WormMovementHeadNT}); for
 * {@link EntityBOTPrimeBody} it is the forward chain-link neighbor (fed by
 * {@link WormMovementBodyNT#findEntityToFollow}). Both readings are consistent with the damage-redirect
 * mechanism above (a body segment's "forward link" is exactly what its damage should flow toward), so
 * the field is kept unified rather than split, matching the report's characterization of this as a
 * readability trap rather than two truly independent pieces of state - but the naming is preserved
 * verbatim (not renamed to e.g. {@code chainTarget}) to keep this file's cross-references to CE legible
 * during review.
 */
public abstract class EntityWormBaseNT extends EntityBurrowingNT {

    public static final Predicate<Entity> WORM_SELECTOR = target -> target instanceof EntityWormBaseNT;

    public int aggroCooldown = 0;
    public int courseChangeCooldown = 0;
    public double waypointX;
    public double waypointY;
    public double waypointZ;
    @Nullable
    Entity targetedEntity = null;
    protected boolean canFly = false;
    int dmgCooldown = 0;
    boolean wasNearGround;
    BlockPos spawnPoint = BlockPos.ZERO;
    double attackRange;
    double maxSpeed;
    double fallSpeed;
    double rangeForParts;
    @Nullable
    LivingEntity followed;
    int surfaceY;
    private int headID;
    private int partNum;
    boolean didCheck;
    double bodySpeed;
    double maxBodySpeed;
    double segmentDistance;
    protected double knockbackDivider;

    /** CE's own vanilla {@code recentlyHit} (an {@code EntityLivingBase} field this port's {@link LivingEntity}
     *  does not expose the same way) - reimplemented locally: set to 100 on every successful {@link #hurt},
     *  decremented once per {@link #aiStep()}. Used by {@link EntityBOTPrimeHead}'s self-heal-while-idle
     *  check exactly like CE's {@code this.recentlyHit == 0}. */
    int recentlyHit = 0;

    protected EntityWormBaseNT(EntityType<? extends EntityWormBaseNT> type, Level level) {
        super(type, level);
        this.surfaceY = 60;
    }

    public int getPartNumber() {
        return this.partNum;
    }

    public void setPartNumber(int num) {
        this.partNum = num;
    }

    @Nullable
    public Entity getHead() {
        return this.level().getEntity(this.headID);
    }

    public int getHeadID() {
        return this.headID;
    }

    public void setHeadID(int id) {
        this.headID = id;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)
                || source.is(DamageTypes.DROWN)
                || source.is(DamageTypes.IN_WALL)
                || source.is(DamageTypes.CRAMMING)
                || (source.getDirectEntity() instanceof EntityWormBaseNT attacker && attacker.getHeadID() == this.getHeadID())) {
            return false;
        }

        if (source.getEntity() instanceof LivingEntity attackerLiving) {
            this.setLastHurtByMob(attackerLiving);
        }

        boolean result;
        if (this.getIsHead()) {
            result = super.hurt(source, amount);
        } else {
            Entity head = this.targetedEntity;
            result = head != null ? head.hurt(source, amount) : super.hurt(source, amount);
        }

        if (result) {
            this.recentlyHit = 100;
        }
        return result;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide && this.level().getDifficulty() == Difficulty.PEACEFUL) {
            this.discard();
        }

        if (this.targetedEntity != null && !this.targetedEntity.isAlive()) {
            this.targetedEntity = null;
        }

        if (this.getY() < -10.0D) {
            this.teleportTo(this.getX(), 128.0D, this.getZ());
            this.setDeltaMovement(this.getDeltaMovement().x, 0.0D, this.getDeltaMovement().z);
        } else if (this.getY() < 3.0D) {
            this.setDeltaMovement(this.getDeltaMovement().x, 0.3D, this.getDeltaMovement().z);
        }

        if (this.recentlyHit > 0) {
            this.recentlyHit--;
        }

        if (!this.level().isClientSide && this.tickCount % 5 == 0) {
            attackEntitiesInList(this.level().getEntities(this, this.getBoundingBox().inflate(0.5D), e -> true));
        }
    }

    protected void attackEntitiesInList(List<Entity> targets) {
        for (Entity target : targets) {
            if (target instanceof LivingEntity living && !living.isSpectator()
                    && (!(target instanceof EntityWormBaseNT worm) || worm.getHeadID() != this.getHeadID())) {
                wormAttack(target);
            }
        }
    }

    /** CE: {@code attackEntityAsMob}. Not an override of any vanilla {@code Mob} method - this class's
     *  touch-damage is applied directly from {@link #attackEntitiesInList}, not through vanilla's melee
     *  attack-goal machinery (the worm has no {@code MeleeAttackGoal}). */
    private void wormAttack(Entity target) {
        boolean hit = target.hurt(this.damageSources().mobAttack(this), getAttackStrength(target));

        if (hit) {
            double tx = (this.getBoundingBox().minX + this.getBoundingBox().maxX) / 2.0D;
            double ty = (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D;
            double tz = (this.getBoundingBox().minZ + this.getBoundingBox().maxZ) / 2.0D;
            double deltaX = target.getX() - tx;
            double deltaY = target.getY() - ty;
            double deltaZ = target.getZ() - tz;
            double knockback = this.knockbackDivider * (deltaX * deltaX + deltaZ * deltaZ + deltaY * deltaY + 0.1D);
            target.push(deltaX / knockback, deltaY / knockback, deltaZ / knockback);
        }
    }

    public abstract float getAttackStrength(Entity target);

    @Override
    public void push(double x, double y, double z) {
        // CE: addVelocity is a no-op - the worm cannot be knocked back by explosions/entity collision.
    }

    @Override
    public void knockback(double strength, double x, double z) {
        // CE: no equivalent call exists (faceEntity/addVelocity are the only two no-ops CE names), but
        // vanilla combat knockback bypasses push() entirely in 1.21.1 - overridden here too so "the worm
        // cannot be knocked back" (the report's own characterization of addVelocity's intent) holds for
        // melee-hit knockback as well as explosion/collision knockback.
    }

    protected boolean isCourseTraversable() {
        return this.canFly || this.isInWall();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("wormID", this.getHeadID());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setHeadID(tag.getInt("wormID"));
    }
}
