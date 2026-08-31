package com.hbm.entity.mob;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.EntityBOTPrimeBody} (115 lines, read in full)
 * - see {@code docs/phase4/entities_bosses.md}'s worm-boss table.
 * <p>
 * <b>{@link #getAttackStrength}</b> - 75% of the target's <em>current</em> health (a real formula, not
 * a fixed value: a body-segment touch always leaves the victim at 25% HP), matching CE exactly.
 * <p>
 * <b>Self-destruct-on-orphan</b> ({@link #customServerAiStep()}): once {@link #didCheck} flips true
 * (set by {@link WormMovementBodyNT#findEntityToFollow} on its first / every-60-tick chain-relink
 * scan), a segment whose forward chain-link ({@link #targetedEntity}) has died or gone missing bleeds
 * out at 1999 HP/tick; a segment whose {@link #followed} (the worm's <em>head</em> specifically - see
 * {@link WormMovementBodyNT}'s own javadoc for why every segment's orphan-check is keyed on the head,
 * not its immediate neighbor) is dead or out of {@code rangeForParts} additionally rolls a 1-in-60
 * chance per tick to self-detonate (damage-only, {@code Level.ExplosionInteraction.NONE}) - this is
 * the mechanism that makes the practical fight "damage the head; the body unravels afterward" per the
 * research report's Headline finding #2.
 * <p>
 * CE's own {@code updateAITasks()} additionally re-invokes {@code targetTasks.onUpdateTasks()} a
 * second time inside the method body, even though vanilla's own {@code EntityLiving.onUpdate()} (this
 * port's {@code Mob#aiStep()}) already ticks the target selector once immediately before calling this
 * method - a literal redundant double-tick in CE's own source with no observable behavioral difference
 * (goal evaluation is idempotent within a single tick), not reproduced here.
 */
public class EntityBOTPrimeBody extends EntityBOTPrimeBase {

    private static final EntityDataAccessor<Boolean> DATA_SHIELD =
            SynchedEntityData.defineId(EntityBOTPrimeBody.class, EntityDataSerializers.BOOLEAN);

    private final WormMovementBodyNT movement = new WormMovementBodyNT(this);

    public EntityBOTPrimeBody(EntityType<? extends EntityBOTPrimeBody> type, Level level) {
        super(type, level);
        this.bodySpeed = 0.6D;
        this.rangeForParts = 70.0D;
        this.segmentDistance = 3.5D;
        this.maxBodySpeed = 1.4D;
        this.targetSelector.addGoal(1, new EntityAINearestAttackableTargetNT(this, Player.class, 0, false, false, this.selector, 128.0D));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        // CE registers this DataParameter but this survey found no consumer anywhere in the classes
        // read - likely client-side rendering (Phase 5, out of this package's scope). Kept as inert
        // synced state for shape-fidelity with CE rather than dropped outright.
        builder.define(DATA_SHIELD, false);
    }

    @Override
    public float getAttackStrength(Entity target) {
        if (target instanceof LivingEntity living) {
            return living.getHealth() * 0.75F;
        }
        return 100F;
    }

    /** CE: {@code isPotionApplicable} - body segments are immune to every potion/mob effect. */
    @Override
    public boolean canBeAffected(MobEffectInstance effectInstance) {
        return false;
    }

    @Override
    protected void customServerAiStep() {
        this.movement.updateMovement();

        if (this.didCheck) {
            if (this.targetedEntity == null || !this.targetedEntity.isAlive()) {
                this.setHealth(this.getHealth() - 1999.0F);
            }
            if ((this.followed == null || !this.followed.isAlive()) && this.random.nextInt(60) == 0) {
                this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, false, Level.ExplosionInteraction.NONE);
            }
        }

        LivingEntity attackTarget = this.getTarget();
        if (this.followed != null && this.followed.isAlive() && attackTarget != null) {
            if (hasLineOfSight(attackTarget)) {
                this.attackCounter += 1;
                if (this.attackCounter == 10) {
                    laserAttack(attackTarget, false);
                    this.attackCounter = -20;
                }
            } else if (this.attackCounter > 0) {
                this.attackCounter -= 1;
            }
        } else if (this.attackCounter > 0) {
            this.attackCounter -= 1;
        }

        updateFacingTowardChainLink();
    }

    @Override
    public void tick() {
        super.tick();
        updateFacingTowardChainLink();
    }

    /** CE duplicates this exact snippet in both {@code updateAITasks()} and {@code onUpdate()}; folded
     *  into one helper here since both call sites compute the same thing from the same fields. */
    private void updateFacingTowardChainLink() {
        Entity target = this.targetedEntity;
        if (target == null) return;

        double dx = target.getX() - this.getX();
        double dy = target.getY() - this.getY();
        double dz = target.getZ() - this.getZ();
        float horiz = (float) Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.atan2(dx, dz) * 180.0D / Math.PI);
        float pitch = (float) (Math.atan2(dy, horiz) * 180.0D / Math.PI);

        this.setYRot(yaw);
        this.yRotO = yaw;
        this.setXRot(pitch);
        this.xRotO = pitch;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("partID", this.getPartNumber());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setPartNumber(tag.getInt("partID"));
    }
}
