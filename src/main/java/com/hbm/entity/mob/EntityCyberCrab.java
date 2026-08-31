package com.hbm.entity.mob;

import com.hbm.damage.ModDamageTypes;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.LegacyMobBulletConfigs;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.lib.HBMSoundHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityCyberCrab} (extends {@code EntityMob}, ~100
 * lines, read in full) - see {@code docs/phase4/entities_vehicles_aircraft.md}'s Headline finding #1
 * (confirmed <b>not</b> a vehicle - an ordinary ground mob with a legacy ranged attack) and
 * {@code docs/phase4/entities_bosses.md}'s legacy-bullet-system table (the tau bullet consumer).
 * 0.75x0.35 hitbox, 4 HP, 0.5 movement speed - a small, fragile robotic crab.
 * <p>
 * <b>Water/fire self-damage</b> ({@link #tick()}): CE's {@code onUpdate} deals 10 flat damage per
 * tick while wet/burning (a robot shorting out) - preserved via a plain generic {@code hurt} call.
 * <p>
 * <b>Death explosion</b>: CE's own {@code onUpdate} has a redundant {@code getHealth() <= 0 ->
 * setDead() + createExplosion} check that only ever fires through the normal damage/death pipeline in
 * a modern engine (unlike 1.12, where an explicit follow-up check was sometimes needed) - reproduced
 * here via {@link #die(DamageSource)} instead, the same "CE's onUpdate self-destruct becomes a die()
 * override" simplification {@code EntityCreeperNuclear}/{@code EntityBOTPrimeHead} already established
 * in this port, not a behavior change (0.1F radius, no block damage, matching CE's {@code EntityMob}
 * regular crab value - {@link EntityTaintCrab} overrides to CE's 3F).
 * <p>
 * <b>Tau immunity</b> ({@link #hurt}): CE's {@code ModDamageSource.getIsTau(source) -> return false}
 * maps directly onto {@link ModDamageTypes#TAU} (the same key {@link LegacyMobBulletConfigs#TAU_BULLET}
 * itself deals, per that class's {@code DamageClass.TAU -> ModDamageTypes.TAU} mapping) - the crab is
 * immune to its own weapon.
 */
public class EntityCyberCrab extends Monster implements RangedAttackMob, IRadiationImmune {

    /** CE: the {@code EntityAINearestAttackableTarget} exclusion predicate - excludes the whole crab
     *  family plus any {@link Creeper} (including {@code EntityCreeperNuclear}, a {@link Creeper}
     *  subclass in this port per {@code docs/phase4/entities_creeper_variants.md}). */
    protected static final Predicate<LivingEntity> TARGET_EXCLUSION =
            e -> !(e instanceof EntityCyberCrab || e instanceof Creeper);

    public EntityCyberCrab(EntityType<? extends EntityCyberCrab> type, Level level) {
        super(type, level);
    }

    /** CE: {@code applyEntityAttributes} - {@code MAX_HEALTH = 4}, {@code MOVEMENT_SPEED = 0.5}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    /** CE: {@code isAIDisabled() { return false; }} */
    @Override
    public boolean isNoAi() {
        return false;
    }

    @Override
    protected void registerGoals() {
        // CE: EntityAIPanic is added for the base crab and EntityTeslaCrab, but not EntityTaintCrab
        // (checked via `!(this instanceof EntityTaintCrab)` at construction time in CE - safe here too,
        // since the runtime class is already fixed before any constructor body runs).
        if (!(this instanceof EntityTaintCrab)) {
            this.goalSelector.addGoal(0, new PanicGoal(this, 0.75D));
        }
        this.goalSelector.addGoal(1, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(4, this.createRangedAttackGoal());
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 3, true, false, null));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 3, true, false, TARGET_EXCLUSION));
    }

    /** CE: {@code arrowAttack()} - overridden by {@link EntityTaintCrab} to a much shorter, more
     *  frequent burst (CE: {@code (this, 0.5D, 5, 5, 50.0F)}). */
    protected RangedAttackGoal createRangedAttackGoal() {
        return new RangedAttackGoal(this, 0.5D, 60, 80, 15.0F);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(ModDamageTypes.TAU)) return false;
        return super.hurt(source, amount);
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.isAlive()
                && (this.isInWaterRainOrBubble() || this.isOnFire())) {
            this.hurt(this.damageSources().generic(), 10.0F);
        }
    }

    /** CE: {@code onUpdate}'s explicit death-explosion check, moved to {@link #die} - see class javadoc. */
    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.explosionRadius(), Level.ExplosionInteraction.NONE);
        }
    }

    /** CE: 0.1F for the base crab/{@link EntityTeslaCrab}, 3F for {@link EntityTaintCrab}. */
    protected float explosionRadius() {
        return 0.1F;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return HBMSoundHandler.cybercrab.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return HBMSoundHandler.cybercrab.get();
    }

    /**
     * CE: {@code attackEntityWithRangedAttack} - {@link LegacyMobBulletConfigs#TAU_BULLET}
     * (critical/tau/flat-2-damage in CE terms - already baked into that config's own
     * {@code setGrav(0).setDamageRange(2F, 2F)}), via {@link EntityBulletBaseMK4}'s mob-aim constructor.
     */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level().isClientSide) return;

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this.level(), LegacyMobBulletConfigs.TAU_BULLET, this, target, 1.6F, 0.05F);
        this.level().addFreshEntity(bullet);
        this.playSound(HBMSoundHandler.sawShoot.get(), 1.0F, 2.0F);
    }
}
