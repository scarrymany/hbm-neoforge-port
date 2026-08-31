package com.hbm.items.weapon.legacy;

import com.hbm.config.BombConfig;
import com.hbm.entity.effect.EntityBlackHole;
import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.effect.EntityRagingVortex;
import com.hbm.entity.effect.EntityVortex;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
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
 * {@code ItemGunBaseNT}s) with dedicated {@link BulletConfig}s whose {@code onImpact} reproduces
 * CE's real explosion behavior via already-ported explosion entities/helpers.
 * <p>
 * <b>{@code gun_b92}'s explosion is a single fixed tier</b> - CE's real {@code EntityExplosiveBeam#explode()}
 * always spawns the exact same fixed-radius antimatter-tier detonation regardless of how many charges
 * were fired (charge count only controls how many beams are fired, each with increasing divergence -
 * see {@link ItemGunB92#releaseUsing}). This is ported 1:1 via {@link EntityNukeExplosionMK3#statFacFleija}
 * (destructionRange 10) - the exact same factory {@link ItemGunB92#selfDetonate} already uses for the
 * gun's 11th-charge self-detonation, just at a smaller radius - plus a decorative
 * {@link EntityCloudFleija} (this port's substitute for CE's {@code EntityCloudFleijaRainbow}, which
 * does not exist here; see that class's own javadoc).
 * <p>
 * <b>{@code gun_b93}'s explosion genuinely escalates with charge level</b> - CE's real
 * {@code EntityModBeam#explode()} branches on an int {@code mode} field (0-9, = charge power - 1)
 * into 10 completely different explosion types, from a small vanilla-style blast at mode 0 up to a
 * full {@code EntityNukeExplosionMK5} gadget-tier nuclear detonation at max charge (mode 9) - this is
 * the gun's entire "legendary" identity, not a cosmetic detail, so it is ported as a real 10-tier
 * escalation rather than the single flattened explosion an earlier draft of this file used.
 * {@link EntityBulletBaseMK4} has no spare per-shot field to carry an arbitrary int like CE's bespoke
 * {@code EntityModBeam.mode} (adding one would mean touching that shared, heavily-reused entity class
 * outside this package's scope), so the 10 tiers are instead 10 distinct {@link BulletConfig}s - one
 * per mode, each with its own {@code onImpact} - selected by {@link #b93Beam(int)} at fire time. CE's
 * modes 4/5 ({@link EntityVortex}, sizes 1F/2.5F), 6/7 ({@link EntityRagingVortex}, sizes 2.5F/5F), and
 * 8 ({@link EntityBlackHole}, size 2F) are now wired directly per
 * docs/phase4/entities_vortex_gravity_wells.md's Headline finding 5 (that report's own exact
 * mode-by-mode parameters, read from real CE {@code EntityModBeam#explode()} source), now that this
 * port has that entity family. Mode 9 (CE's real final {@code else} branch, hit only at max charge) is
 * a genuine gadget-tier nuke, exactly matching CE.
 */
final class LegacyChargeWeapons {

    private LegacyChargeWeapons() {
    }

    static final BulletConfig b92_beam = new BulletConfig("b92_beam")
            .setupDamageClass(DamageClass.EXPLOSIVE).setVel(1.5F).setGrav(0).setLife(200)
            .setOnImpact(LegacyChargeWeapons::explodeB92);

    /** 10 mode-tier configs for {@code gun_b93} - see class javadoc. Index == CE's {@code mode} (0-9). */
    private static final BulletConfig[] B93_BEAMS = new BulletConfig[10];

    static {
        for (int i = 0; i < B93_BEAMS.length; i++) {
            int mode = i;
            B93_BEAMS[i] = new BulletConfig("b93_beam_" + mode)
                    .setupDamageClass(DamageClass.EXPLOSIVE).setVel(1.5F).setGrav(0).setLife(200)
                    .setOnImpact((bullet, hit) -> explodeB93(bullet, hit, mode));
        }
    }

    /** The {@code gun_b93} config for a given charge {@code mode} (0-9), clamped defensively - see {@link ItemGunB93#releaseUsing}'s call site. */
    static BulletConfig b93Beam(int mode) {
        return B93_BEAMS[Math.max(0, Math.min(mode, B93_BEAMS.length - 1))];
    }

    /** Spawns one charge-round with the given fixed damage (CE: {@code dmgMin}-{@code dmgMax} 16-28 for b92, mode-scaled for b93) and per-shot angular divergence. */
    static void fireBeam(LivingEntity shooter, BulletConfig config, float damage, float divergence) {
        EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(shooter, config, damage, divergence, 0, 0, 0.5);
        shooter.level().addFreshEntity(bullet);
    }

    /** CE's {@code EntityExplosiveBeam#explode()} - see class javadoc. */
    private static void explodeB92(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower()) return;
        bullet.discard();

        Level level = bullet.level();
        if (level.isClientSide()) return;
        Vec3 loc = hit.getLocation();

        level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);
        level.addFreshEntity(EntityNukeExplosionMK3.statFacFleija(level, loc.x, loc.y, loc.z, 10));
        level.addFreshEntity(EntityCloudFleija.create(level, loc.x, loc.y, loc.z, 100));
    }

    /** CE's {@code EntityModBeam#explode()}'s 10-tier {@code mode} branch - see class javadoc for the mode-by-mode mapping. */
    private static void explodeB93(EntityBulletBaseMK4 bullet, HitResult hit, int mode) {
        if (hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower()) return;
        bullet.discard();

        Level level = bullet.level();
        if (level.isClientSide()) return;
        Vec3 loc = hit.getLocation();
        LivingEntity thrower = bullet.getThrower();

        switch (mode) {
            case 0 -> ExplosionLarge.explode(level, thrower, loc.x, loc.y, loc.z, 5F, true, false, false);
            case 1 -> ExplosionLarge.explodeFire(level, thrower, loc.x, loc.y, loc.z, 10F, true, false, false);
            case 2 -> {
                level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);
                level.addFreshEntity(EntityNukeExplosionMK3.statFacFleija(level, loc.x, loc.y, loc.z, 10));
                level.addFreshEntity(EntityCloudFleija.create(level, loc.x, loc.y, loc.z, 100));
            }
            case 3 -> {
                level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);
                level.addFreshEntity(EntityNukeExplosionMK3.statFacFleija(level, loc.x, loc.y, loc.z, 20));
                level.addFreshEntity(EntityCloudFleija.create(level, loc.x, loc.y, loc.z, 100));
            }
            // Modes 4-8: CE's real EntityModBeam#explode() spawns the gravity-well entity itself at
            // the beam's impact position, with the same explosion-sound cue as every other mode but
            // no accompanying cloud - see class javadoc for the size-per-mode table.
            case 4 -> spawnGravityWell(level, new EntityVortex(level, 1F), loc);
            case 5 -> spawnGravityWell(level, new EntityVortex(level, 2.5F), loc);
            case 6 -> spawnGravityWell(level, new EntityRagingVortex(level, 2.5F), loc);
            case 7 -> spawnGravityWell(level, new EntityRagingVortex(level, 5F), loc);
            case 8 -> spawnGravityWell(level, new EntityBlackHole(level, 2F), loc);
            // Mode 9 (max charge) and CE's own catch-all default: a real gadget-tier nuke.
            default -> {
                level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);
                level.addFreshEntity(EntityNukeExplosionMK5.statFac(level, BombConfig.GADGET_RADIUS.get(), loc.x, loc.y, loc.z).setDetonator(thrower));
                if (BombConfig.ENABLE_NUKE_CLOUDS.get()) {
                    EntityNukeTorex.statFac(level, loc.x, loc.y, loc.z, BombConfig.GADGET_RADIUS.get());
                }
            }
        }
    }

    /** CE's real per-mode {@code EntityVortex}/{@code EntityRagingVortex}/{@code EntityBlackHole} spawn (modes 4-8) - see {@link #explodeB93}. */
    private static void spawnGravityWell(Level level, EntityBlackHole well, Vec3 loc) {
        level.playSound(null, loc.x, loc.y, loc.z, SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 100.0F, level.random.nextFloat() * 0.1F + 0.9F);
        well.setPos(loc.x, loc.y, loc.z);
        level.addFreshEntity(well);
    }

    static final Item.Properties LEGENDARY_PROPS = new Item.Properties().stacksTo(1);
}
