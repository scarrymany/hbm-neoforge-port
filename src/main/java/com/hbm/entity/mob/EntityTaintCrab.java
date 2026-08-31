package com.hbm.entity.mob;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.items.weapon.sedna.content.XFactory762mm;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import com.hbm.potion.HbmPotionEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/**
 * Direct port of CE's {@code com.hbm.entity.mob.EntityTaintCrab} (extends {@link EntityCyberCrab}, 84
 * lines, read in full) - see {@code docs/phase4/entities_vehicles_aircraft.md}'s crab-family row. 1.25x1.25
 * hitbox, 25 HP, same 0.5 speed, a much shorter/wider ranged burst (CE: {@code (0.5D, 5, 5, 50.0F)}
 * vs. the base crab's {@code (0.5D, 60, 80, 15.0F)}).
 * <p>
 * <b>Modern Sedna weapon, not the legacy config</b> - confirmed by the research report's Headline
 * finding #1: CE fires {@code EntityBulletBaseMK4} via {@code XFactory762mm.r762_fmj} directly, not
 * {@code EntityBullet}/{@code GunNPCFactory}. Reproduced here with this port's own already-real
 * {@link EntityBulletBaseMK4}/{@link XFactory762mm#r762_fmj} - zero missing dependency.
 * <p>
 * <b>Taint aura</b> ({@link #tick()}): every tick, every non-{@link EntityCyberCrab} {@link LivingEntity}
 * within a 5-block AABB grow gets {@link HbmPotionEffects#TAINT} for 30 ticks - CE's own
 * {@code onLivingUpdate}, preserved exactly.
 * <p>
 * <b>Tesla-arc zap not reproduced</b> - CE's {@code onLivingUpdate} also calls
 * {@code TileEntityTesla.zap(world, posX, posY+1.25, posZ, 10, this)} to arc-damage nearby entities.
 * {@code TileEntityTesla} does not exist anywhere in this port yet (confirmed by repo-wide grep - the
 * Tesla coil machine block entity is unported Phase 2/3 content, not owned by this mob package). This
 * is a genuine, documented forward-reference gap (see this task's knownGaps), not a silent drop - the
 * taint-aura half of the same method (a separate, self-contained mechanic) is fully reproduced above.
 * <p>
 * <b>{@code coil_copper}/{@code coil_magnetized_tungsten} drops not reproduced</b> - neither item is
 * registered anywhere in this port yet (confirmed by grep; a Phase 1/2 items-scope gap, matching the
 * same "document and skip" precedent {@code EntityMissileTier0} already established for its own
 * unregistered {@code wire_fine} drop, rather than risk a duplicate-registration collision with a
 * concurrent sibling items package by registering a shared generic material here).
 */
public class EntityTaintCrab extends EntityCyberCrab {

    public EntityTaintCrab(EntityType<? extends EntityCyberCrab> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    /** CE: {@code applyEntityAttributes} - {@code MAX_HEALTH = 25}, {@code MOVEMENT_SPEED = 0.5}. */
    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 25.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected RangedAttackGoal createRangedAttackGoal() {
        return new RangedAttackGoal(this, 0.5D, 5, 5, 50.0F);
    }

    @Override
    protected float explosionRadius() {
        return 3.0F;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5))) {
                if (!(e instanceof EntityCyberCrab)) {
                    e.addEffect(new MobEffectInstance(HbmPotionEffects.TAINT, 30));
                }
            }
        }
        // TileEntityTesla.zap(...) not reproduced - see class javadoc.
    }

    /**
     * CE: {@code new EntityBulletBaseMK4(this, XFactory762mm.r762_fmj, 10F, 0F, 0F, 0F, 0F)} - the
     * "standard guns" gun-fired constructor, fired straight along the crab's own look vector (not the
     * mob-aim-at-target constructor {@link EntityCyberCrab} uses for its tau bullet - CE's own source
     * uses the shooter-facing overload here, not a target-aimed one, despite this being a "ranged
     * attack" callback; preserved exactly, not corrected to aim at target).
     */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.level().isClientSide) return;

        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(this, XFactory762mm.r762_fmj, 10F, 0F, 0D, 0D, 0D);

        CompoundTag data = new CompoundTag();
        data.putString("mode", "flame");
        data.putDouble("mX", bullet.getDeltaMovement().x * 0.3);
        data.putDouble("mY", bullet.getDeltaMovement().y * 0.3);
        data.putDouble("mZ", bullet.getDeltaMovement().z * 0.3);
        HbmEffect.sendPacket(this.level(), HbmEffect.VANILLA, bullet.getX(), bullet.getY(), bullet.getZ(), 50, data);

        this.level().addFreshEntity(bullet);
        this.playSound(HBMSoundHandler.sawShoot.get(), 1.0F, 0.5F);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        // coil_copper / coil_magnetized_tungsten not registered in this port yet - see class javadoc.
    }
}
