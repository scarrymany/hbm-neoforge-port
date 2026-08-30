package com.hbm.items.weapon.legacy;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Shared ballistics for CE's legendary legacy charge-weapon pair {@code gun_b92}/{@code gun_b93},
 * ported per this task's explicit instruction ("port them if the required entity classes
 * {@code EntityExplosiveBeam}/{@code EntityModBeam} exist or can be built simply on vanilla
 * Projectile"). Neither CE entity class exists anywhere in this port; rather than inventing two new
 * bespoke {@code EntityType}s, both weapons reuse the already-ported, already-registered
 * {@link EntityBulletBaseMK4} (constructed directly, bypassing the Sedna {@code Receiver}/
 * {@code GunConfig} state machine entirely - these are hold-right-click bow-style items, not
 * {@code ItemGunBaseNT}s) with a dedicated {@link BulletConfig} whose {@code onImpact} reproduces
 * CE's "explode on impact" behavior via this port's real {@code ExplosionVNT} stack. Same visible
 * behavior (a projectile that flies out and explodes where it lands); no new entity/network
 * registration needed.
 */
final class LegacyChargeWeapons {

    private LegacyChargeWeapons() {
    }

    static final BulletConfig b92_beam = new BulletConfig("b92_beam")
            .setupDamageClass(DamageClass.EXPLOSIVE).setVel(1.5F).setGrav(0).setLife(200)
            .setOnImpact(LegacyChargeWeapons::explode);
    static final BulletConfig b93_beam = new BulletConfig("b93_beam")
            .setupDamageClass(DamageClass.EXPLOSIVE).setVel(1.5F).setGrav(0).setLife(200)
            .setOnImpact(LegacyChargeWeapons::explode);

    /** Spawns one charge-round with the given fixed damage (CE: {@code dmgMin}-{@code dmgMax} 16-28 for b92, mode-scaled for b93) and per-shot angular divergence. */
    static void fireBeam(LivingEntity shooter, BulletConfig config, float damage, float divergence) {
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(shooter, config, damage, divergence, 0, 0, 0.5);
        shooter.level().addFreshEntity(bullet);
    }

    private static void explode(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower()) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 3F, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(6, 2F, 1F));
        vnt.explode();
    }

    static final Item.Properties LEGENDARY_PROPS = new Item.Properties().stacksTo(1);
}
