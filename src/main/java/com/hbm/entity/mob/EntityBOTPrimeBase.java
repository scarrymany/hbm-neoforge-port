package com.hbm.entity.mob;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.Predicate;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.botprime.EntityBOTPrimeBase} (95 lines, read in full)
 * - see {@code docs/phase4/entities_bosses.md}'s worm-boss table. Shared by {@link EntityBOTPrimeHead}
 * and {@link EntityBOTPrimeBody}: 15,000 HP <b>per segment</b> (not split across the worm - see the
 * research report's Headline finding #2, faithfully preserved here, not "fixed" into a shared pool),
 * full knockback resistance, fire-immune, {@code noPhysics = true} (CE: {@code noClip}), never
 * despawns, AI never disabled.
 * <p>
 * {@link #laserAttack(Entity, boolean)} is the shared attack entry point both Head and Body call,
 * firing through this port's already-shipped Sedna-retarget classes
 * ({@link LegacyMobBulletConfigs#WORM_LASER}/{@link LegacyMobBulletConfigs#WORM_BOLT} via
 * {@link EntityBulletBaseMK4}'s target-aim constructor) rather than CE's own legacy
 * {@code EntityBulletBase} - the executive decision {@code docs/phase4/entities_legacy_bullet_system.md}
 * and this report's own Key design/API decisions converge on.
 */
public abstract class EntityBOTPrimeBase extends EntityWormBaseNT {

    public int attackCounter = 0;

    protected final Predicate<Entity> selector =
            ent -> !(ent instanceof EntityWormBaseNT w && w.getHeadID() == EntityBOTPrimeBase.this.getHeadID());

    protected EntityBOTPrimeBase(EntityType<? extends EntityBOTPrimeBase> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.dragInAir = 0.995F;
        this.dragInGround = 0.98F;
        this.knockbackDivider = 1.0D;
    }

    /** CE: {@code applyEntityAttributes} - {@code MAX_HEALTH = 15000}, {@code KNOCKBACK_RESISTANCE = 1.0}.
     *  {@code FOLLOW_RANGE} is bumped well past vanilla's 16-block default since the worm's own targeting
     *  ({@link EntityAINearestAttackableTargetNT}) searches out to 128 blocks independently of this
     *  attribute - kept generous so nothing else reading {@code Attributes.FOLLOW_RANGE} clips it short. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 15000.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D);
    }

    /** CE: {@code canEntityBeSeen} - a raw block raytrace ignoring vanilla's own LOS caching. */
    @Override
    public boolean hasLineOfSight(Entity entity) {
        Vec3 from = new Vec3(this.getX(), this.getY() + this.getEyeHeight(), this.getZ());
        Vec3 to = new Vec3(entity.getX(), entity.getY() + entity.getEyeHeight(), entity.getZ());
        HitResult trace = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return trace.getType() == HitResult.Type.MISS;
    }

    @Override
    public boolean isNoAi() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HBMSoundHandler.bombDet.get();
    }

    /**
     * CE: {@code laserAttack(Entity, boolean)}. {@code head=true} fires 5 staggered
     * {@link LegacyMobBulletConfigs#WORM_LASER} shots at {@code i*0.05F} deviation increments (CE's
     * {@code GunNPCFactory.getWormHeadBolt()}, 35-60 damage); {@code head=false} fires a single
     * {@link LegacyMobBulletConfigs#WORM_BOLT} shot (CE's {@code getWormBolt()}, 15-25 damage).
     */
    protected void laserAttack(Entity target, boolean head) {
        if (!(target instanceof LivingEntity living)) return;

        Level level = this.level();
        if (level.isClientSide) return;

        if (head) {
            for (int i = 0; i < 5; i++) {
                EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.WORM_LASER, this, living, 1.0F, i * 0.05F);
                level.addFreshEntity(bullet);
            }
            this.playSound(HBMSoundHandler.ballsLaser.get(), 5.0F, 0.75F);
        } else {
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.WORM_BOLT, this, living, 0.5F, 0.125F);
            level.addFreshEntity(bullet);
            this.playSound(HBMSoundHandler.ballsLaser.get(), 5.0F, 1.0F);
        }
    }
}
