package com.hbm.items.weapon.grenade;

import com.hbm.entity.effect.EntityCloudFleija;
import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorFire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Detonation behavior for every {@link EnumGrenadeFilling} value, ported from CE's
 * {@code com.hbm.items.weapon.grenade.ItemGrenadeFilling} (339 lines). Kept as a standalone class
 * (rather than nested statics inside {@link EnumGrenadeFilling} itself) purely for file-length
 * readability - see {@link EnumGrenadeFuze}'s javadoc for why every enum constant below references
 * one of these via a plain method handle rather than a shared {@code public static final} lambda
 * field.
 * <p>
 * <b>Forward references (documented, not silently dropped) - each one is called out again at its
 * exact call site below:</b>
 * <ul>
 *     <li>{@code com.hbm.entity.effect.EntityFireLingering} (INC/WP's lingering-fire payload) is
 *     confirmed not ported anywhere in this tree. The WP/energy-filling {@code AuxParticlePacketNT}
 *     VFX broadcasts ({@link #explodeWp}'s {@link com.hbm.particle.HbmEffect#HAZE} x3,
 *     {@link #explodeStandardEnergy}'s {@link com.hbm.particle.HbmEffect#PLASMA_BLAST} x3,
 *     {@link #spawnMush}'s {@link com.hbm.particle.HbmEffect#MUKE}) are now wired - see
 *     {@code docs/phase5/particle_engine_and_generic_vfx.md}.</li>
 *     <li>{@code igniteAround}'s "ignite a flammable-adjacent air block" branch - CE's
 *     {@code Block#isFlammable(IBlockAccess,BlockPos,EnumFacing)} has no single confirmed 1.21.1
 *     replacement; dropped rather than guessed at, matching
 *     {@code com.hbm.explosion.ExplosionNukeGeneric#vaporDest}'s identical documented gap for the
 *     same CE API.</li>
 *     <li>{@code EntityProcessorCrossSmooth#setDamageClass(DamageClass)} does not exist on this
 *     port's {@code EntityProcessorCrossSmooth} yet (the {@code DamageClass}-to-{@code DamageType}
 *     mapping {@code docs/phase3/gun_framework.md} already flagged as missing a
 *     {@code SEDNA_PLASMA}-family entry) - EMP/PLASMA fall back to the processor's own plain
 *     explosion-damage source until that lands.</li>
 *     <li>{@code com.hbm.saveddata.satellites.SatelliteDetector} ({@code spawnMush}'s satellite ping) -
 *     confirmed not ported anywhere in this tree (missile-infra scope, per
 *     {@code docs/phase3/explosion_engine.md}'s identical finding for {@code EntityFalloutRain}).
 *     {@code com.hbm.handler.radiation.ChunkRadiationManager} (NUCLEAR/NUCLEAR_DEMO's
 *     {@code incrementRad}) is now real (Phase 4) and wired below.</li>
 * </ul>
 * Every other call below (the {@code ExplosionVNT} blasts themselves, the CLUSTER/CLUSTER_HEAVY/LASER/
 * FRAG_SLEEVE submunitions, SCHRAB's Fleija spawn) is a real, fully-wired port - none of Phase 3's own
 * already-landed dependencies (vanillant explosions, the ballistics core, the nuke-tier entity
 * families) are missing.
 */
public final class GrenadeFillingActions {

    private GrenadeFillingActions() {
    }

    // ==================== shared submunition BulletConfigs ====================
    // CE declares these as instance-assigned static fields inside ItemGrenadeFilling's constructor
    // (fragile: correctness depends on that Item being constructed before any filling lambda ever
    // runs). This port instead makes them plain eagerly-initialized static finals, independent of any
    // Item's construction lifecycle - see BulletConfig's own class javadoc for this port's explicit-id
    // registration scheme, which these ids feed into.

    public static final BulletConfig FRAGMENTATION = new BulletConfig("grenade_fragment")
            .setLife(3).setThresholdNegation(5F).setRicochetAngle(90).setRicochetCount(2);

    public static final BulletConfig PELLET = new BulletConfig("grenade_cluster_pellet")
            .setLife(100).setGrav(0.04F).setVel(1.5F).setOnImpact(GrenadeFillingActions::pelletTinyExplode);

    public static final BulletConfig PELLET_HEAVY = new BulletConfig("grenade_cluster_pellet_heavy")
            .setLife(100).setGrav(0.04F).setVel(1.5F).setOnImpact(GrenadeFillingActions::pelletExplode);

    public static final BulletConfig LASER_BEAM = new BulletConfig("grenade_laser_beam")
            .setBeam().setupDamageClass(DamageClass.LASER).setLife(3).setRenderRotations(false)
            .setThresholdNegation(10F).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);

    // ==================== per-filling detonation ====================

    static void explodePowder(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 5F, 10F, 5F, 0F);
    }

    static void explodeHe(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 7.5F, 25F, 10F, 0.1F);
    }

    static void explodeDemo(EntityGrenadeUniversal grenade) {
        ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), 5F, grenade.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, 10F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    static void explodeInc(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 3F, 10F);
        // forward reference: EntityFireLingering(6x2 area, 200t, TYPE_DIESEL) - see class javadoc.
        // forward reference: igniteAround(...,2) - see class javadoc.
    }

    static void explodeWp(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 3F, 10F);
        // forward reference: EntityFireLingering(6x2 area, 600t, TYPE_PHOSPHORUS) - see class javadoc.
        // forward reference: igniteAround(...,3) - see class javadoc.

        Level level = grenade.level();
        for (int i = 0; i < 3; i++) {
            double hx = grenade.getX() + level.getRandom().nextGaussian() * 4;
            double hz = grenade.getZ() + level.getRandom().nextGaussian() * 4;
            HbmEffect.sendPacket(level, HbmEffect.HAZE, hx, grenade.getY(), hz, 150, null);
        }
    }

    static void explodeCluster(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
        int frags = grenade.getShell() == EnumGrenadeShell.FRAG ? 37 : 30; // 30 * 1.25
        spawnPellets(grenade, PELLET, 15F, frags, 0.5D, 0.75D, 0.5D);
    }

    static void explodeClusterHeavy(EntityGrenadeUniversal grenade) {
        standardExplode(grenade, 7.5F, 15F, 10F, 0.1F);
        spawnPellets(grenade, PELLET_HEAVY, 30F, 15, 0.5D, 1.25D, 0.5D);
    }

    private static void spawnPellets(EntityGrenadeUniversal grenade, BulletConfig config, float damage, int count, double sx, double sy, double sz) {
        Level level = grenade.level();
        for (int i = 0; i < count; i++) {
            float yaw = level.getRandom().nextFloat() * 2F * (float) Math.PI;
            float pitch = (level.getRandom().nextFloat() * 0.5F + 0.5F) * (float) Math.PI;
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, config, damage, 0F, yaw, pitch);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05, grenade.getZ());
            Vec3 m = bullet.getDeltaMovement();
            bullet.setDeltaMovement(m.x * sx, m.y * sy, m.z * sz);
            level.addFreshEntity(bullet);
        }
    }

    static void explodeEmp(EntityGrenadeUniversal grenade) {
        // CE: explodeStandardEnergy(grenade, 30F, 3F, DamageClass.ELECTRIC, 0.5F, 0.5F, 1F, 3F) - a
        // pale blue burst.
        explodeStandardEnergy(grenade, 30F, 3F, 0.5F, 0.5F, 1F, 3F);
    }

    static void explodePlasma(EntityGrenadeUniversal grenade) {
        // CE's own code comment: "TODO: unique effect because this sucks" - CE itself flags this
        // filling's VFX as a placeholder; not worth over-polishing beyond parity here either.
        // CE: explodeStandardEnergy(grenade, 50F, 5F, DamageClass.PLASMA, 0.5F, 1F, 0.5F, 4F) - a
        // pale green burst.
        explodeStandardEnergy(grenade, 50F, 5F, 0.5F, 1F, 0.5F, 4F);
    }

    private static void explodeStandardEnergy(EntityGrenadeUniversal grenade, float damage, float range, float r, float g, float b, float scale) {
        Level level = grenade.level();
        ExplosionVNT vnt = new ExplosionVNT(level, grenade.getX(), grenade.getY(), grenade.getZ(), range, grenade.getThrower());
        // forward reference: EntityProcessorCrossSmooth#setDamageClass(...) - see class javadoc.
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        level.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(),
                HBMSoundHandler.ufoBlast, SoundSource.HOSTILE, 5.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(),
                SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 5.0F, 0.5F);

        float yaw = level.getRandom().nextFloat() * 180F;
        for (int i = 0; i < 3; i++) {
            CompoundTag data = new CompoundTag();
            data.putFloat("r", r);
            data.putFloat("g", g);
            data.putFloat("b", b);
            data.putFloat("pitch", -60F + 60F * i);
            data.putFloat("yaw", yaw);
            data.putFloat("scale", scale);
            HbmEffect.sendPacket(level, HbmEffect.PLASMA_BLAST, grenade.getX(), grenade.getY() + 0.125, grenade.getZ(), 100, data);
        }
    }

    static void explodeLaser(EntityGrenadeUniversal grenade) {
        tinyExplode(grenade, 2F, 5F);

        Level level = grenade.level();
        double x = grenade.getX();
        double y = grenade.getY() + 0.125;
        double z = grenade.getZ();
        double range = 15;

        List<LivingEntity> potentialTargets = new ArrayList<>(
                level.getEntitiesOfClass(LivingEntity.class, new AABB(x, y, z, x, y, z).inflate(range)));
        Collections.shuffle(potentialTargets);

        for (LivingEntity target : potentialTargets) {
            if (target == grenade.getThrower()) continue;

            Vec3 delta = new Vec3(target.getX() - x, target.getY() + target.getBbHeight() / 2D - y, target.getZ() - z);
            if (delta.length() > range) continue;

            EntityBulletBeamBase sub = new EntityBulletBeamBase(level, LASER_BEAM, 30F);
            sub.thrower = grenade.getThrower();
            sub.setPos(x, y, z);
            sub.setRotationsFromVector(delta);
            sub.performHitscanExternal(delta.length());
            level.addFreshEntity(sub);
        }
    }

    static void explodeNuclear(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        ExplosionVNT vnt = new ExplosionVNT(level, grenade.getX(), grenade.getY(), grenade.getZ(), 10F);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 100).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();

        incrementRad(level, grenade.getX(), grenade.getY(), grenade.getZ(), 1F);
        spawnMush(grenade);
    }

    static void explodeNuclearDemo(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        ExplosionVNT vnt = new ExplosionVNT(level, grenade.getX(), grenade.getY(), grenade.getZ(), 10F);
        vnt.setBlockAllocator(new BlockAllocatorStandard(64));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorFire()));
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, 50).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();

        incrementRad(level, grenade.getX(), grenade.getY(), grenade.getZ(), 1.5F);
        spawnMush(grenade);
    }

    /**
     * CE: {@code XFactoryCatapult.incrementRad(World, double, double, double, float)} - the same
     * 5x5 chunk-cross ambient-radiation bump {@link com.hbm.items.weapon.sedna.content.XFactoryEnergy}
     * uses for its own nuke rounds, applied here to NUCLEAR/NUCLEAR_DEMO grenade fillings.
     */
    private static void incrementRad(Level level, double x, double y, double z, float mult) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) < 4) {
                    pos.set((int) Math.floor(x + i * 16), (int) Math.floor(y), (int) Math.floor(z + j * 16));
                    ChunkRadiationManager.proxy.incrementRad(level, pos, (50F / (Math.abs(i) + Math.abs(j) + 1)) * mult);
                }
            }
        }
    }

    private static void spawnMush(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        // forward reference: SatelliteDetector.reportEvent(...) - see class javadoc. The sound below is real.
        level.playSound(null, grenade.getX(), grenade.getY(), grenade.getZ(),
                HBMSoundHandler.mukeExplosion, SoundSource.HOSTILE, 15.0F, 1.0F);

        // CE's own rare "balefire" cosmetic flag (upstream/hbm-ce/.../ItemGrenadeFilling.java:265-266)
        // - the random 1-in-100 half only; MainRegistry.polaroidID isn't confirmed ported.
        CompoundTag data = new CompoundTag();
        if (level.getRandom().nextInt(100) == 0) {
            data.putBoolean("balefire", true);
        }
        HbmEffect.sendPacket(level, HbmEffect.MUKE, grenade.getX(), grenade.getY() + 0.5, grenade.getZ(), 250, data);
    }

    static void explodeSchrab(EntityGrenadeUniversal grenade) {
        Level level = grenade.level();
        double x = grenade.getX();
        double y = grenade.getY();
        double z = grenade.getZ();

        EntityNukeExplosionMK3 ex = EntityNukeExplosionMK3.statFacFleija(level, x, y, z, 20);
        ex.setDetonator(grenade.getThrower());
        if (!ex.isRemoved()) {
            level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE, SoundSource.HOSTILE, 100.0F,
                    level.getRandom().nextFloat() * 0.1F + 0.9F);
            level.addFreshEntity(ex);
            level.addFreshEntity(EntityCloudFleija.create(level, x, y, z, 20));
        }
    }

    // ==================== shared submunition impact lambdas ====================
    // CE routes these through com.hbm.items.weapon.sedna.factory.Lego.tinyExplode/standardExplode -
    // this port's own Lego.java explicitly defers those two helpers to "whichever package wires an
    // ammo's onImpact to an explosion" (see that class's javadoc), i.e. this package. Reimplemented
    // here rather than added to the shared Lego.java, to avoid touching a file other Phase 3 "guns"
    // agents may be concurrently editing in this same wave.

    private static void pelletTinyExplode(EntityBulletBaseMK4 bullet, HitResult mop) {
        if (bullet.tickCount < 2) return;
        Vec3 hit = mop.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 1.5F, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent)
                .setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
        bullet.discard();
    }

    private static void pelletExplode(EntityBulletBaseMK4 bullet, HitResult mop) {
        if (bullet.tickCount < 2) return;
        Vec3 hit = mop.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, 5F, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.discard();
    }

    // ==================== shared ExplosionVNT presets ====================

    static void standardExplode(EntityGrenadeUniversal grenade, float range, float damage) {
        standardExplode(grenade, range, damage, 0F, 0F);
    }

    static void standardExplode(EntityGrenadeUniversal grenade, float range, float damage, float pierceDT, float pierceDR) {
        ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), range, grenade.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, damage).setupPiercing(pierceDT, pierceDR));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    static void tinyExplode(EntityGrenadeUniversal grenade, float range, float damage) {
        ExplosionVNT vnt = new ExplosionVNT(grenade.level(), grenade.getX(), grenade.getY(), grenade.getZ(), range, grenade.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(0.5, damage).setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
    }

    /**
     * Shared by {@link EnumGrenadeExtra#FRAG_SLEEVE}. {@code frags} is a {@code float} and the loop
     * condition below compares {@code int < float} directly (not truncating {@code frags} to an
     * {@code int} first) - matching CE's own exact loop shape bit-for-bit: with the FRAG shell's 1.5x
     * bonus, 25 frags becomes 37.5, and {@code i < 37.5F} is still true at {@code i == 37}, so this
     * spawns 38 pellets, not 37 - a real CE quirk of comparing an {@code int} loop counter against a
     * {@code float} bound, preserved rather than "corrected".
     */
    static void standardFragmentation(EntityGrenadeUniversal grenade, float frags) {
        if (grenade.getShell() == EnumGrenadeShell.FRAG) frags *= 1.5F;
        Level level = grenade.level();
        for (int i = 0; i < frags; i++) {
            float yaw = level.getRandom().nextFloat() * 2F * (float) Math.PI;
            float pitch = (level.getRandom().nextFloat() - 0.5F) * 2F * (float) Math.PI;
            EntityBulletBaseMK4 bullet = new EntityBulletBaseMK4(level, FRAGMENTATION, 10F, 0F, yaw, pitch);
            bullet.setPos(grenade.getX(), grenade.getY() + 0.05, grenade.getZ());
            level.addFreshEntity(bullet);
        }
    }
}
