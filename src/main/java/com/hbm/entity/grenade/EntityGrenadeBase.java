package com.hbm.entity.grenade;

import com.hbm.entity.projectile.EntityThrowableNT;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Port of CE's {@code com.hbm.entity.grenade.EntityGrenadeBase} (106 lines, abstract) - the legacy
 * hand-thrown-projectile family's shared throw-launch physics and "hit anything, explode
 * immediately" impact dispatch. Used directly by {@link EntityGrenadeImpactGeneric} (CE's
 * {@code fuse == -1} path - unreachable by any currently-registered {@code ModItems} entry, but kept
 * for registry/parity per {@code docs/phase3/grenades.md}) and {@link EntityDisperserCanister}.
 * <p>
 * {@link EntityGrenadeBouncyGeneric} also extends this class in this port (CE instead gives it a
 * wholly separate {@code EntityGrenadeBouncyBase} hierarchy, forking vanilla's own
 * {@code Entity#move}/collision code to get continuous ground-rolling bounce physics) - a deliberate
 * simplification the research report explicitly sanctions ("re-derive the *intent* - invert/damp
 * motion on collision - against 1.21.1's actual movement API, not transliterate the 1.12 AABB-sweep
 * mechanics"). {@link EntityGrenadeBouncyGeneric} overrides {@link #onImpact} to bounce instead of
 * detonating immediately, reusing the already-ported {@link EntityThrowableNT} ballistic tick loop
 * rather than a hand-rolled collision sweep - the same technique this port's own
 * {@code EntityGrenadeUniversal} already uses for the modern grenade's bounce.
 */
public abstract class EntityGrenadeBase extends EntityThrowableNT {

    protected EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level level) {
        super(type, level);
    }

    protected EntityGrenadeBase(EntityType<? extends EntityGrenadeBase> type, Level level, LivingEntity thrower, InteractionHand hand) {
        super(type, level);
        this.setOwner(thrower);

        this.moveTo(thrower.getX(), thrower.getY() + thrower.getEyeHeight(), thrower.getZ(), thrower.getYRot(), thrower.getXRot());

        double sideSign = hand == InteractionHand.MAIN_HAND ? -1D : 1D;
        double yawRad = Math.toRadians(this.getYRot());
        this.setPos(
                this.getX() + sideSign * Math.cos(yawRad) * 0.16D,
                this.getY() - 0.1D,
                this.getZ() + sideSign * Math.sin(yawRad) * 0.16D);

        float velocity = 0.4F;
        double pitchRad = Math.toRadians(this.getXRot());
        double mx = -Math.sin(yawRad) * Math.cos(pitchRad) * velocity;
        double mz = Math.cos(yawRad) * Math.cos(pitchRad) * velocity;
        double my = -Math.sin(pitchRad) * velocity;
        this.shoot(mx, my, mz, 1.5F, 1.0F);
    }

    public int getTimer() {
        return this.ticksInAir + this.ticksInGround;
    }

    @Override
    protected void onImpact(HitResult mop) {
        if (mop instanceof EntityHitResult ehr) {
            Entity hit = ehr.getEntity();
            // CE: attackEntityFrom(DamageSource.causeThrownDamage(this, thrower), 0) - 0-damage,
            // hit-detection bookkeeping only, not real damage. level().damageSources().thrown(...) is
            // the confirmed 1.21.1 equivalent source family for a thrown-projectile hit.
            hit.hurt(this.level().damageSources().thrown(this, this.getThrower()), 0F);
        }

        this.explode();
    }

    public abstract void explode();
}
