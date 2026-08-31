package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * CE: {@code EntityProcessorCrossSmooth} - a {@link EntityProcessorCross} variant used by the Sedna
 * weapon system: fixed (non-explosive-formula) damage that falls off linearly with distance, piercing
 * damage-type/resistance support, and confetti-death effects.
 * <p>
 * CE's real {@code attackEntity} routes through {@code com.hbm.items.weapon.sedna.BulletConfig},
 * {@code com.hbm.util.EntityDamageUtil.attackEntityFromNT} (piercing-aware damage bypassing part of
 * the vanilla armor model), and {@code com.hbm.items.weapon.sedna.factory.ConfettiUtil} - none of
 * which are wired into {@link #attackEntity} below yet (the Sedna weapon system's own guns/turrets
 * build their explosive impacts directly on {@code ExplosionVNT} instead, per
 * {@code docs/phase3/gun_framework.md}; this class's own piercing/confetti layer was never needed by
 * any consumer built so far). {@code com.hbm.util.DamageResistanceHandler.DamageClass} <b>does</b> now
 * exist (shipped alongside the Sedna weapon system) and is exposed via {@link #setDamageClass} for a
 * future consumer to select - see that method's own javadoc. {@link #calculateDamage} needs none of
 * the above and is ported exactly; the piercing/confetti/damage-class pieces of {@link #attackEntity}
 * remain a documented forward-reference, falling back to plain vanilla explosion damage in the
 * meantime rather than inventing a piercing model.
 */
public class EntityProcessorCrossSmooth extends EntityProcessorCross {

    protected float fixedDamage;
    protected float pierceDT = 0;
    protected float pierceDR = 0;
    /**
     * CE default for this processor's own damage-source selection ({@code DamageClass.EXPLOSIVE}) -
     * not yet consumed by {@link #attackEntity} below (see that method's own forward-reference note),
     * but exposed now via {@link #setDamageClass} so {@code EntityOrbitalLaser}
     * (docs/phase3/satellites_followup_and_loot_pools.md's forward reference) has a real, non-default
     * damage class to select (e.g. {@code DamageClass.LASER}) once it's built.
     */
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

    @Override
    public void attackEntity(Entity entity, ExplosionVNT source, float amount) {
        if (!entity.isAlive()) return;
        if (source.exploder == entity) amount *= 0.5F;

        // forward reference: com.hbm.items.weapon.sedna.BulletConfig.getDamage(...) /
        // com.hbm.util.EntityDamageUtil.attackEntityFromNT(...) (pierceDT/pierceDR) /
        // com.hbm.items.weapon.sedna.factory.ConfettiUtil.decideConfetti(...) - Sedna weapon package,
        // not created this wave. Falls back to plain vanilla explosion damage (still hits, just
        // without CE's piercing-damage-type/confetti-on-kill layer) until that package lands.
        DamageSource dmg = setExplosionSource(entity.level(), source.compat);
        entity.hurt(dmg, amount);
    }

    @Override
    public float calculateDamage(double distanceScaled, double density, double knockback, float size) {
        return (float) (fixedDamage * (1 - distanceScaled));
    }
}
