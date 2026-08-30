package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;

/**
 * CE: {@code EntityProcessorCrossSmooth} - a {@link EntityProcessorCross} variant used by the Sedna
 * weapon system: fixed (non-explosive-formula) damage that falls off linearly with distance, piercing
 * damage-type/resistance support, and confetti-death effects.
 * <p>
 * CE's real {@code attackEntity} routes through {@code com.hbm.items.weapon.sedna.BulletConfig},
 * {@code com.hbm.util.EntityDamageUtil.attackEntityFromNT} (piercing-aware damage bypassing part of
 * the vanilla armor model), {@code com.hbm.items.weapon.sedna.factory.ConfettiUtil}, and
 * {@code com.hbm.util.DamageResistanceHandler.DamageClass} - none of which exist in this port yet
 * (the Sedna weapon system and its damage-resistance/piercing model are a separate Phase 3 "guns"
 * package, not created this wave; confirmed via Neo Edition's own parallel port assuming the same
 * not-yet-built classes). {@link #calculateDamage} needs none of them and is ported exactly; the
 * piercing/confetti pieces of {@link #attackEntity} are left as a documented forward-reference,
 * falling back to plain vanilla damage in the meantime rather than inventing a piercing model.
 */
public class EntityProcessorCrossSmooth extends EntityProcessorCross {

    protected float fixedDamage;
    protected float pierceDT = 0;
    protected float pierceDR = 0;

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
