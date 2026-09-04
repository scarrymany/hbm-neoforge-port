package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Exact CE {@code EntityProcessorCrossSmooth.java}: fixed linear falloff, {@code setDamageClass},
 * pierce via {@link EntityDamageUtil#attackEntityFromNT}. {@code ConfettiUtil.decideConfetti} stays
 * skipped (VFX).
 */
public class EntityProcessorCrossSmooth extends EntityProcessorCross {

    protected float fixedDamage;
    protected float pierceDT = 0;
    protected float pierceDR = 0;
    /** CE field {@code clazz}, default {@code EXPLOSIVE}. */
    protected DamageClass dmgClass = DamageClass.EXPLOSIVE;

    public EntityProcessorCrossSmooth(double nodeDist, float fixedDamage) {
        super(nodeDist);
        this.fixedDamage = fixedDamage;
        this.setAllowSelfDamage();
    }

    public EntityProcessorCrossSmooth setupPiercing(float pierceDT, float pierceDR) {
        this.pierceDT = pierceDT;
        this.pierceDR = pierceDR;
        return this;
    }

    public EntityProcessorCrossSmooth setDamageClass(DamageClass dmgClass) {
        this.dmgClass = dmgClass;
        return this;
    }

    /** Exact CE {@code EntityProcessorCrossSmooth.java:38-47}. Confetti skip. */
    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!entity.isAlive()) return;
        if (source.exploder == entity) amount *= 0.5F;
        LivingEntity shooter = source.exploder instanceof LivingEntity living ? living : null;
        DamageSource dmg = BulletConfig.getDamage(entity.level(), null, shooter, dmgClass);
        if (!(entity instanceof LivingEntity living)) {
            entity.hurt(dmg, amount);
        } else {
            EntityDamageUtil.attackEntityFromNT(living, dmg, amount, true, false, 0F, pierceDT, pierceDR);
        }
    }

    @Override
    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) (fixedDamage * (1 - distanceScaled));
    }
}
