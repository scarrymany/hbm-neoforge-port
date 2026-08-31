package com.hbm.entity.projectile;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.util.BobMathUtil;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * The 10 real, live pre-Sedna mob/boss ammo definitions - CE's {@code com.hbm.handler.guncfg.
 * GunNPCFactory}'s 8 methods (the only genuinely-live content in that ~1,000-line package, per
 * {@code docs/phase4/entities_legacy_bullet_system.md}'s Headline finding #2's exhaustive registry
 * trace) plus Hunter Chopper's and the Cyber Crab family's own {@code EntityBullet}-based ammo (per
 * that same report's own table and {@code docs/phase4/entities_bosses.md}'s "legacy bullet system"
 * table), all retargeted onto this port's already-shipped Sedna {@link BulletConfig}/
 * {@link EntityBulletBaseMK4} framework rather than porting a second, parallel ballistics entity -
 * the executive decision both reports converge on (see the legacy-bullet-system report's Headline
 * finding #5 and Key design/API decisions).
 * <p>
 * This class does <b>not</b> build any of the mobs/bosses that fire these - MaskMan, the worm boss
 * (BOTPrime head/body), the UFO boss, Hunter Chopper, and the Cyber Crab family are separate
 * content-wave work (see {@code docs/phase4/entities_bosses.md}). It exists purely so those consumers
 * have real {@link BulletConfig} instances and {@link EntityBulletBaseMK4}'s new "aim at a target"
 * constructor to call - e.g. {@code new EntityBulletBaseMK4(level, LegacyMobBulletConfigs.WORM_LASER,
 * this, target, 1.0F, 0.05F)}.
 * <p>
 * <b>Key porting decisions, stated rather than silently absorbed</b> (see the report's own "Key
 * design/API decisions" and "Open questions" for the full reasoning):
 * <ul>
 *     <li><b>{@code doesPenetrate} is mapped to Sedna's non-penetrating (single-nearest-hit) path for
 *     every config here</b>, regardless of CE's own per-config flag value - naively reusing Sedna's
 *     same-named flag would change CE's "passes through a target, may hit a second one on a later
 *     tick" semantics into "hits every entity in the same swept box in one tick", a real behavioral
 *     difference the report flags explicitly rather than accepting silently.</li>
 *     <li><b>{@link BulletConfig#headshotMult} is neutralized ({@code setHeadshot(1.0F)}) on every
 *     config here</b> - CE's legacy ballistics never had a headshot-multiplier mechanic, so leaving
 *     Sedna's own default (1.25x) in place would be an unintended buff.</li>
 *     <li><b>{@code UFO_ROCKET}'s mid-flight homing reuses {@link EntityBulletBaseMK4#lockonTarget}</b>
 *     (the same already-shipped gradual-turn mechanism {@code TurretRichardBlockEntity} and
 *     player-locked-on rockets already use) rather than reimplementing CE's own instant
 *     snap-to-target-direction steering - a deliberate adaptation consistent with this port's existing
 *     homing pattern, not a duplicate ballistics mechanism.</li>
 *     <li><b>{@code MASKMAN_BULLET}'s {@code leadChance = 15} (CE: 15% chance to inflict
 *     {@code HbmPotion.lead} on hit) is a documented forward reference, not silently dropped</b> -
 *     {@code HbmPotion} does not exist in this port yet (confirmed by the report's own Deferred scope,
 *     which names this exact gap and its owning package: {@code docs/phase4/hbm_potion_system.md}).</li>
 *     <li><b>Cosmetic-only legacy fields with no gameplay effect - {@code style}/{@code trail} (visual
 *     bullet appearance), {@code vPFX} (particle trail), and the client-side particle bursts in
 *     {@code getMaskmanMeteor}'s {@code bUpdate} / {@code getRocketUFOConfig}'s {@code bImpact} - are
 *     not ported</b>, matching the same deferred-particle pattern this port's own
 *     {@code XFactoryEnergy}/{@code XFactoryRocket} Sedna content already established (Phase 5 client
 *     scope; {@code HbmEffectNT}/{@code AuxParticlePacketNT} don't exist in this port).</li>
 * </ul>
 */
public final class LegacyMobBulletConfigs {

    private LegacyMobBulletConfigs() {
    }

    // ============================================================================================
    // shared lambdas
    // ============================================================================================

    /**
     * For configs with CE {@code doesRicochet = false}: a block hit simply ends the bullet's flight
     * (matching CE's own non-ricocheting {@code EntityBulletBase} behavior) rather than Sedna's default
     * bounce-or-discard-by-angle lambda.
     */
    private static final BiConsumer<EntityBulletBaseMK4, BlockHitResult> DISCARD_ON_BLOCK_HIT = (bullet, bhr) -> {
        bullet.setPos(bhr.getLocation());
        bullet.discard();
    };

    private static final double UFO_HOMING_RANGE = 100D;
    private static final double UFO_HOMING_CONE_ANGLE = 90D;

    // ============================================================================================
    // MaskMan (docs/phase4/entities_bosses.md) - CE's GunNPCFactory.getMaskman*()
    // ============================================================================================

    /** CE: {@code getMaskmanOrb()} - a slow, gravity-free orb that fans out {@link #MASKMAN_BOLT} shots at every nearby player every 10 ticks, then pops in a small blast on eventual impact. */
    public static final BulletConfig MASKMAN_ORB = new BulletConfig("legacy_maskman_orb")
            .setVel(0.25F).setSpread(0F).setLife(60).setGrav(0).setDamageRange(100F, 100F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET).setDoesPenetrate(false)
            .setOnEntityHit(null).setOnRicochet(null)
            .setOnImpact((bullet, hit) -> {
                spawnMobExplosion(bullet, 1.5F, true);
                bullet.discard();
            })
            .setOnUpdate(LegacyMobBulletConfigs::maskmanOrbUpdate);

    /** CE: {@code getMaskmanBolt()} - the sub-munition {@link #MASKMAN_ORB} fans out; also a plain ricocheting bullet in its own right. */
    public static final BulletConfig MASKMAN_BOLT = new BulletConfig("legacy_maskman_bolt")
            .setVel(5.0F).setSpread(0F).setLife(100).setDamageRange(15F, 20F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET)
            .setRicochetAngle(5F).setDoesPenetrate(false);

    /** CE: {@code getMaskmanBullet()} - MaskMan's minigun stream ({@code EntityAIMaskmanMinigun}). */
    public static final BulletConfig MASKMAN_BULLET = new BulletConfig("legacy_maskman_bullet")
            .setVel(5.0F).setSpread(0F).setLife(100).setDamageRange(5F, 10F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET)
            .setRicochetAngle(5F).setDoesPenetrate(false);
    // TODO(phase4-hbm-potion-system): CE's leadChance = 15 (15% chance to inflict HbmPotion.lead on a
    // successful hit) is not wired - HbmPotion doesn't exist in this port yet. See class javadoc.

    /** CE: {@code getMaskmanTracer()} - a plain ricocheting bullet that additionally drops a {@link #MASKMAN_METEOR} wherever it strikes a block. */
    public static final BulletConfig MASKMAN_TRACER = new BulletConfig("legacy_maskman_tracer")
            .setVel(5.0F).setSpread(0F).setLife(100).setDamageRange(15F, 20F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET)
            .setRicochetAngle(5F).setDoesPenetrate(false)
            .setOnImpact(LegacyMobBulletConfigs::maskmanTracerImpact);

    /** CE: {@code getMaskmanRocket()} - a lobbed, non-ricocheting grenade-style round; {@code blockDamage = false} in CE, so no terrain destruction. Spread (0.005F) is inherited from {@code standardGrenadeConfig()} - CE's own {@code getMaskmanRocket()} never touches it. */
    public static final BulletConfig MASKMAN_ROCKET = new BulletConfig("legacy_maskman_rocket")
            .setVel(1.0F).setSpread(0.005F).setLife(300).setGrav(0.1F).setDamageRange(15F, 20F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET).setDoesPenetrate(false)
            .setOnEntityHit(null).setOnRicochet(null)
            .setOnImpact((bullet, hit) -> {
                spawnMobExplosion(bullet, 5.0F, false);
                bullet.discard();
            });

    /** CE: {@code getMaskmanMeteor()} - {@link #MASKMAN_TRACER}'s falling sub-munition; terrain-destroying (CE leaves {@code blockDamage} at its default {@code true}) and ignites whatever it directly strikes ({@code incendiary = 3}). Spread (0.005F) is inherited from {@code standardGrenadeConfig()} - CE's own {@code getMaskmanMeteor()} never touches it. */
    public static final BulletConfig MASKMAN_METEOR = new BulletConfig("legacy_maskman_meteor")
            .setVel(1.0F).setSpread(0.005F).setLife(300).setGrav(0.1F).setDamageRange(20F, 30F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET).setDoesPenetrate(false)
            .setOnEntityHit(null).setOnRicochet(null)
            .setOnImpact((bullet, hit) -> {
                spawnMobExplosion(bullet, 2.5F, true);
                if (hit instanceof EntityHitResult ehr) ehr.getEntity().igniteForSeconds(3);
                bullet.discard();
            });
    // TODO(phase5-particles): CE's own bUpdate (a client-side 5-particle Flame trail) is purely
    // decorative and not ported - see class javadoc's deferred-particle note.

    // ============================================================================================
    // Worm boss / BOTPrime head+body (docs/phase4/entities_bosses.md) - also fired identically by the
    // UFO boss's laser attack (WORM_LASER; CE reuses this one ammo definition across both bosses).
    // ============================================================================================

    /** CE: {@code getWormBolt()} - BOTPrime body's single-shot laser. */
    public static final BulletConfig WORM_BOLT = new BulletConfig("legacy_worm_bolt")
            .setVel(5.0F).setSpread(0F).setLife(60).setDamageRange(15F, 25F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET)
            .setDoesPenetrate(false).setOnRicochet(DISCARD_ON_BLOCK_HIT);

    /** CE: {@code getWormHeadBolt()} - BOTPrime head's 5-shot staggered volley; also the UFO boss's laser attack. */
    public static final BulletConfig WORM_LASER = new BulletConfig("legacy_worm_laser")
            .setVel(5.0F).setSpread(0F).setLife(100).setDamageRange(35F, 60F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET)
            .setDoesPenetrate(false).setOnRicochet(DISCARD_ON_BLOCK_HIT);

    // ============================================================================================
    // UFO boss (docs/phase4/entities_bosses.md) - CE's GunRocketFactory.getRocketConfig() base,
    // customized by GunNPCFactory.getRocketUFOConfig()'s homing bUpdate + radiation/sound bImpact.
    // ============================================================================================

    /**
     * CE: {@code getRocketUFOConfig()} - direct hit-damage (10-15, via the standard entity-hit lambda)
     * plus a radiation/sound area effect on any impact ({@link #ufoRocketImpact}), and mid-flight
     * homing toward the nearest visible {@link LivingEntity} within a 90-degree cone
     * ({@link #ufoRocketUpdate}). Unlike the MaskMan rocket/meteor/orb above, CE never disables
     * ricochet for this config (inherited {@code true} from {@code standardRocketConfig()}, never
     * overridden) - kept as Sedna's default ricochet lambda here, matching CE exactly.
     */
    public static final BulletConfig UFO_ROCKET = new BulletConfig("legacy_ufo_rocket")
            .setVel(2.0F).setSpread(0.005F).setLife(300).setGrav(0.005F).setDamageRange(10F, 15F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.REVOLVER_BULLET)
            .setRicochetAngle(10F).setDoesPenetrate(false)
            .setOnImpact(LegacyMobBulletConfigs::ufoRocketImpact)
            .setOnUpdate(LegacyMobBulletConfigs::ufoRocketUpdate);

    // ============================================================================================
    // Hunter Chopper / Cyber Crab family (docs/phase4/entities_bosses.md,
    // docs/phase4/entities_legacy_bullet_system.md) - CE's EntityBullet ("MK1"), not GunNPCFactory.
    // Both live consumers always fire with the mob itself as shooter, so EntityBullet's own
    // vanilla-arrow-derived "stuck in ground / player pickup" branch is already dead-in-practice for
    // both (see the legacy-bullet-system report's Key design decisions) - dropped entirely here in
    // favor of Sedna's own non-penetrating "discard on block hit" handling, zero observable change.
    // ============================================================================================

    /** CE: {@code new EntityBullet(world, this, 3.0F, 35, 45, false, "chopper", MAIN_HAND)} - Hunter Chopper's plain minigun-style bullet. Not tau, not critical (chopper explicitly excludes itself from the critical/no-drag flag).
     *  <p>Review-pass fix: the {@code (35, 45)} constructor args are that overload's {@code dmgMin}/
     *  {@code dmgMax} params, but CE's real {@code EntityBullet(World, EntityLivingBase, float, int, int,
     *  boolean, String, EnumHand)} constructor never reads them anywhere in its body (confirmed by a full
     *  read - dead parameters) - the actual damage comes from the call site's very next line,
     *  {@code entityarrow.setDamage(3 + rand.nextInt(5))}, i.e. a flat 3-7, not 35-45 (a ~6-10x
     *  overtuned hit was previously baked in here). Corrected to match the real applied damage. */
    public static final BulletConfig CHOPPER_BULLET = new BulletConfig("legacy_chopper_bullet")
            .setVel(3.0F).setSpread(0F).setLife(100).setDamageRange(3F, 7F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.CHOPPER_BULLET)
            .setDoesPenetrate(false).setOnRicochet(DISCARD_ON_BLOCK_HIT);

    /** CE: {@code EntityCyberCrab.attackEntityWithRangedAttack} - {@code Critical=true, Tau=true}, flat 2 damage. "Critical" freezes drag/gravity in CE's arrow-fork {@code EntityBullet}; mapped here to plain zero gravity, since {@link EntityBulletBaseMK4} already has no separate air-drag model to freeze. */
    public static final BulletConfig TAU_BULLET = new BulletConfig("legacy_tau_bullet")
            .setVel(3.0F).setSpread(0F).setLife(100).setGrav(0).setDamageRange(2F, 2F)
            .setHeadshot(1.0F).setupDamageClass(DamageClass.TAU)
            .setDoesPenetrate(false).setOnRicochet(DISCARD_ON_BLOCK_HIT);

    // ============================================================================================
    // impact / update lambdas
    // ============================================================================================

    /**
     * CE: {@code getMaskmanOrb()}'s {@code bUpdate} - every 10 ticks (on the 6th, matching CE's
     * {@code ticksExisted % 10 == 5}), fan out one {@link #MASKMAN_BOLT} at every {@link Player} within
     * 50 blocks, aimed directly at that player's eye position.
     */
    private static void maskmanOrbUpdate(Entity entity) {
        if (!(entity instanceof EntityBulletBaseMK4 bullet)) return;
        Level level = bullet.level();
        if (level.isClientSide) return;
        if (bullet.tickCount % 10 != 5) return;

        Vec3 pos = bullet.position();
        for (Player player : level.getEntitiesOfClass(Player.class, bullet.getBoundingBox().inflate(50D))) {
            Vec3 dir = new Vec3(
                    player.getX() - pos.x,
                    (player.getY() + player.getEyeHeight()) - pos.y,
                    player.getZ() - pos.z
            ).normalize();

            EntityBulletBaseMK4 bolt = new EntityBulletBaseMK4(level);
            bolt.setBulletConfig(MASKMAN_BOLT);
            bolt.damage = MASKMAN_BOLT.rollDamage(level.random);
            bolt.setOwner(bullet.getThrower());
            bolt.setPos(pos);
            bolt.shoot(dir.x, dir.y, dir.z, 0.5F, 0.05F);
            level.addFreshEntity(bolt);
        }
    }

    /**
     * CE: {@code getMaskmanTracer()}'s {@code bImpact} - on a block hit only (matching CE's
     * {@code IBulletImpactBehavior.behaveBlockHit} naming/intent), drop a {@link #MASKMAN_METEOR} from
     * 30-40 blocks straight up, falling at a flat rate. Does not discard the tracer itself - it keeps
     * ricocheting/flying normally afterward via the default onRicochet/onEntityHit lambdas.
     */
    private static void maskmanTracerImpact(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (!(hit instanceof BlockHitResult)) return;
        Level level = bullet.level();
        if (level.isClientSide) return;

        Vec3 pos = bullet.position();
        double y = pos.y + 30 + level.random.nextInt(10);

        EntityBulletBaseMK4 meteor = new EntityBulletBaseMK4(level);
        meteor.setBulletConfig(MASKMAN_METEOR);
        meteor.damage = MASKMAN_METEOR.rollDamage(level.random);
        meteor.setOwner(bullet.getThrower());
        meteor.moveTo(pos.x, y, pos.z, 0, 0);
        meteor.setDeltaMovement(0, -1D, 0);
        level.addFreshEntity(meteor);
    }

    /**
     * CE: {@code getRocketUFOConfig()}'s {@code bImpact} - a radiation/sound area effect with no direct
     * entity damage of its own (the rocket's 10-15 direct-hit damage comes from the standard
     * onEntityHit lambda instead, which runs independently of this). Does not discard the bullet - the
     * caller (either the framework, for a real collision, or {@link #ufoRocketUpdate}'s proximity fuse)
     * is responsible for that.
     */
    private static void ufoRocketImpact(EntityBulletBaseMK4 bullet, HitResult hit) {
        Level level = bullet.level();
        Vec3 pos = bullet.position();

        level.playSound(null, pos.x, pos.y, pos.z, HBMSoundHandler.ufoBlast.get(), SoundSource.HOSTILE, 5.0F, 0.9F + level.random.nextFloat() * 0.2F);
        // NOTE: SoundEvents.FIREWORK_ROCKET_BLAST is well-established Mojang-mapping knowledge for 1.21.1
        // (matching CE's SoundEvents.ENTITY_FIREWORK_BLAST), NOT verified against a real client jar in
        // this sandbox (no network access to fetch one) - flagged as a risk, see the implementing
        // report's own returned knownGaps.
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 5.0F, 0.5F);
        ContaminationUtil.radiate(level, pos.x, pos.y, pos.z, 50, 0, 0, 500);
        // TODO(phase5-particles): CE also broadcasts a 3-shot PlasmaBlast particle burst here (client
        // VFX) - HbmEffectNT/AuxParticlePacketNT don't exist in this port yet, matching this port's own
        // XFactoryEnergy/XFactoryRocket deferred-particle precedent (see class javadoc).
    }

    /**
     * CE: {@code getRocketUFOConfig()}'s {@code bUpdate} - (re)selects the nearest visible
     * {@link LivingEntity} within a 90-degree cone of the rocket's current heading, out to 100 blocks,
     * whenever the current target is gone or dead, storing it in {@link EntityBulletBaseMK4#lockonTarget}
     * for that class's own already-shipped gradual-homing tick logic to steer toward (see class javadoc
     * for why this doesn't reimplement CE's own instant-snap steering). Detonates
     * ({@link #ufoRocketImpact}) once within 5 blocks of the locked target, matching CE's proximity fuse.
     */
    private static void ufoRocketUpdate(Entity entity) {
        if (!(entity instanceof EntityBulletBaseMK4 bullet)) return;
        Level level = bullet.level();
        if (level.isClientSide) return;

        if (bullet.lockonTarget == null || !bullet.lockonTarget.isAlive()) {
            chooseUfoRocketTarget(bullet);
        }

        Entity target = bullet.lockonTarget;
        if (target != null && bullet.position().distanceTo(target.position()) < 5D) {
            ufoRocketImpact(bullet, null);
            bullet.discard();
        }
    }

    private static void chooseUfoRocketTarget(EntityBulletBaseMK4 bullet) {
        Level level = bullet.level();
        Vec3 origin = bullet.position();
        Vec3 motion = bullet.getDeltaMovement();

        LivingEntity best = null;
        double bestAngle = UFO_HOMING_CONE_ANGLE;

        for (LivingEntity candidate : level.getEntitiesOfClass(LivingEntity.class, bullet.getBoundingBox().inflate(UFO_HOMING_RANGE))) {
            if (!candidate.isAlive() || candidate == bullet.getThrower()) continue;

            Vec3 targetPoint = new Vec3(candidate.getX(), candidate.getY() + candidate.getBbHeight() / 2D, candidate.getZ());
            double distSq = origin.distanceToSqr(targetPoint);
            if (distSq >= UFO_HOMING_RANGE * UFO_HOMING_RANGE) continue;

            HitResult trace = level.clip(new ClipContext(origin, targetPoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
            if (trace.getType() != HitResult.Type.MISS) continue;

            double angle = BobMathUtil.getCrossAngle(motion, targetPoint.subtract(origin));
            if (angle < bestAngle) {
                best = candidate;
                bestAngle = angle;
            }
        }

        if (best != null) bullet.lockonTarget = best;
    }

    /**
     * Shared explosion helper for the MaskMan orb/rocket/meteor's {@code explosive} field - built on
     * this port's already-landed {@code ExplosionVNT} stack, matching the precedent
     * {@code XFactoryRocket.standardExplode} already set for the same "explosive Sedna ammo" situation.
     *
     * @param blockDamage whether to also destroy terrain (CE: {@code BulletConfiguration.blockDamage})
     */
    private static void spawnMobExplosion(EntityBulletBaseMK4 bullet, float range, boolean blockDamage) {
        Vec3 pos = bullet.position();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), pos.x, pos.y, pos.z, range, bullet.getThrower());
        if (blockDamage) {
            vnt.setBlockAllocator(new BlockAllocatorStandard());
            vnt.setBlockProcessor(new BlockProcessorStandard());
        }
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }
}
