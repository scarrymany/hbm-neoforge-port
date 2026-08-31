package com.hbm.items.weapon.sedna.content;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.ModAttachments;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.entity.projectile.EntityBulletBeamBase;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorBalefire;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.radiation.ChunkRadiationManager;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.particle.HbmEffect;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryEnergy} (Tesla/laser sidearms) plus
 * {@code gun_fatman} - which CE itself actually defines inside {@code XFactoryCatapult.java} (a
 * misfiling, same pattern Phase 1 already flagged for {@code gun_b92}) but this port's roster groups
 * with the energy-weapon family per {@code docs/phase3/guns_and_ammo.md}. See that report's
 * {@code XFactoryEnergy} table for the full stat card.
 * <p>
 * <b>Forward references (documented, not silently dropped):</b>
 * <ul>
 *     <li>{@code EntityFireLingering} (the laser-IR round's lingering ground fire /
 *     {@code igniteAround}-style "ignite an adjacent flammable block" branch) - confirmed not ported
 *     anywhere in this tree, matching {@code GrenadeFillingActions}'s identical documented gap for
 *     the same CE class (Phase 5 client-VFX / a future incendiary-effects pass). The direct
 *     "set the entity on fire" half of the same lambda <b>is</b> ported (via
 *     {@link HbmLivingAttachment}, which is real and already carries CE's exact {@code fire}/
 *     {@code getFire}/{@code setFire} field).</li>
 *     <li>{@code com.hbm.saveddata.satellites.SatelliteDetector} (the nuke rounds' satellite-ping
 *     calls) - confirmed not ported anywhere in this tree, matching {@code GrenadeFillingActions}'s
 *     identical documented gap for the same class. {@code ChunkRadiationManager}'s own
 *     {@code incrementRad} half of these same CE call sites (Phase 4) is wired below, via the local
 *     {@code incrementRad} helper.</li>
 *     <li>{@code EntityProcessorCrossSmooth#setDamageClass(DamageClass)} does not exist on this port's
 *     {@code EntityProcessorCrossSmooth} - same confirmed gap {@code GrenadeFillingActions} already
 *     documented; every {@code ExplosionVNT} blast below falls back to the processor's own plain
 *     explosion damage source.</li>
 * </ul>
 * Everything else (the {@code ExplosionVNT} blasts, {@link EntityNukeExplosionMK5}/{@link EntityNukeTorex}
 * spawns, the lightning-fanout sub-beam) is a real, fully-wired port against this port's own already-
 * landed ballistics/explosion/nuke-entity infrastructure.
 */
public final class XFactoryEnergy {

    private XFactoryEnergy() {
    }

    // ==================== ammo ====================
    // No discrete casing item family exists yet for beam/energy rounds (see XFactory357's class
    // javadoc for the same omission on cased ammo) - CE's own energy rounds consume ingot_polymer as
    // a raw-material "capacitor" cost via .setCasing(...), not tracked here for the same reason.

    public static Item ITEM_CAPACITOR;
    public static Item ITEM_CAPACITOR_OVERCHARGE;
    public static Item ITEM_CAPACITOR_IR;

    public static final BulletConfig energy_tesla = new BulletConfig("energy_tesla").setItem(() -> ITEM_CAPACITOR)
            .setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0F).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
            .setOnBeamImpact(XFactoryEnergy::lightningHit);
    public static final BulletConfig energy_tesla_overcharge = new BulletConfig("energy_tesla_overcharge").setItem(() -> ITEM_CAPACITOR_OVERCHARGE)
            .setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0F).setLife(5).setRenderRotations(false).setDoesPenetrate(true)
            .setDamage(1.5F).setOnBeamImpact(XFactoryEnergy::lightningHit);
    public static final BulletConfig energy_tesla_ir = new BulletConfig("energy_tesla_ir").setItem(() -> ITEM_CAPACITOR_IR)
            .setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0F).setLife(5).setRenderRotations(false)
            .setDamage(0.8F).setOnBeamImpact(XFactoryEnergy::lightningSplit);
    public static final BulletConfig energy_tesla_ir_sub = new BulletConfig("energy_tesla_ir_sub").setItem(() -> ITEM_CAPACITOR_IR)
            .setupDamageClass(DamageClass.ELECTRIC).setBeam().setSpread(0F).setLife(3).setWear(3F).setRenderRotations(false).setDoesPenetrate(true)
            .setDamage(0.5F).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);

    public static final BulletConfig energy_las = new BulletConfig("energy_las").setItem(() -> ITEM_CAPACITOR)
            .setupDamageClass(DamageClass.LASER).setBeam().setSpread(0F).setLife(5).setRenderRotations(false).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    public static final BulletConfig energy_las_overcharge = new BulletConfig("energy_las_overcharge").setItem(() -> ITEM_CAPACITOR_OVERCHARGE)
            .setupDamageClass(DamageClass.LASER).setBeam().setSpread(0F).setLife(5).setRenderRotations(false).setDoesPenetrate(true).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    public static final BulletConfig energy_las_ir = new BulletConfig("energy_las_ir").setItem(() -> ITEM_CAPACITOR_IR)
            .setupDamageClass(DamageClass.FIRE).setBeam().setSpread(0F).setLife(5).setRenderRotations(false).setOnBeamImpact(XFactoryEnergy::irHit);

    public static final BulletConfig energy_emerald = energy_las.clone("energy_emerald").setArmorPiercing(0.5F).setThresholdNegation(10F);
    public static final BulletConfig energy_emerald_overcharge = energy_las_overcharge.clone("energy_emerald_overcharge").setArmorPiercing(0.5F).setThresholdNegation(15F);
    public static final BulletConfig energy_emerald_ir = energy_las_ir.clone("energy_emerald_ir").setArmorPiercing(0.5F).setThresholdNegation(10F);

    // gun_fatman's mini-nuke rounds - no cased ammo item at all, matches CE's EnumAmmo.NUKE_* having
    // none either (report: "no cased-ammo item at all").
    public static final BulletConfig nuke_standard = new BulletConfig("nuke_standard").setLife(300).setVel(3F).setGrav(0.025).setOnImpact(XFactoryEnergy::nukeStandard);
    public static final BulletConfig nuke_demo = new BulletConfig("nuke_demo").setLife(300).setVel(3F).setGrav(0.025).setOnImpact(XFactoryEnergy::nukeDemo);
    public static final BulletConfig nuke_high = new BulletConfig("nuke_high").setLife(300).setVel(3F).setGrav(0.025).setOnImpact(XFactoryEnergy::nukeHigh);
    public static final BulletConfig nuke_tots = new BulletConfig("nuke_tots").setProjectiles(8).setLife(300).setVel(3F).setGrav(0.025).setSpread(0.1F).setDamage(0.35F).setOnImpact(XFactoryEnergy::nukeTinyTot);
    public static final BulletConfig nuke_hive = new BulletConfig("nuke_hive").setProjectiles(12).setLife(300).setVel(1F).setGrav(0.025).setSpread(0.15F).setDamage(0.25F).setOnImpact(XFactoryEnergy::nukeHive);
    public static final BulletConfig nuke_balefire = new BulletConfig("nuke_balefire").setDamage(2.5F).setLife(300).setVel(3F).setGrav(0.025).setOnImpact(XFactoryEnergy::nukeBalefire);

    // ==================== guns ====================

    public static ItemGunBaseNT gun_tesla_cannon() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(2_000).draw(10).inspect(33).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(35F).delay(20).reload(44).jam(19).sound(HBMSoundHandler.fireTesla.get(), 1.0F, 1.0F)
                                .mag(new MagazineBelt().addConfigs(energy_tesla, energy_tesla_overcharge, energy_tesla_ir))
                                .offset(0.75, 0, -0.375).offsetScoped(0.75, 0, -0.25)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): CAPACITOR x15
    }

    public static ItemGunBaseNT gun_laser_pistol() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(25F).delay(5).spread(1F).spreadHipfire(1F).reload(45).jam(37).sound(HBMSoundHandler.fireLaserPistol.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 30).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
                                .offset(0.75, -0.09375, -0.1875)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): CAPACITOR x15
    }

    public static ItemGunBaseNT gun_laser_pistol_pew_pew() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
                new GunConfig()
                        .dura(500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(30F).rounds(5).delay(10).spread(0.25F).spreadHipfire(1F).reload(45).jam(37).sound(HBMSoundHandler.fireLaserPistol.get(), 1.0F, 0.8F)
                                .mag(new MagazineFullReload(0, 10).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
                                .offset(0.75, -0.09375, -0.1875)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): CAPACITOR_OVERCHARGE x10
    }

    public static ItemGunBaseNT gun_laser_pistol_morning_glory() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
                new GunConfig()
                        .dura(1_500).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(20F).delay(7).spread(0F).spreadHipfire(0.5F).reload(45).jam(37).sound(HBMSoundHandler.fireLaserPistol.get(), 1.0F, 1.1F)
                                .mag(new MagazineFullReload(0, 20).addConfigs(energy_emerald, energy_emerald_overcharge, energy_emerald_ir))
                                .offset(0.75, -0.09375, -0.1875)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): CAPACITOR_OVERCHARGE x20
    }

    public static ItemGunBaseNT gun_lasrifle() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(2_000).draw(10).inspect(26).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(50F).delay(8).reload(44).jam(36).sound(HBMSoundHandler.fireLaser.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 24).addConfigs(energy_las, energy_las_overcharge, energy_las_ir))
                                .offset(0.75, -0.09375, -0.1875)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): CAPACITOR x24
    }

    public static ItemGunBaseNT gun_fatman() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(20).inspect(30).reloadChangeType(true).hideCrosshair(false).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0)
                                .dmg(100F).spreadHipfire(0F).delay(10).reload(57).jam(40).sound(HBMSoundHandler.fireFatman.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(nuke_standard, nuke_demo, nuke_high, nuke_tots, nuke_hive, nuke_balefire))
                                .offset(1, -0.09375, -0.1875D).offsetScoped(1, -0.09375, -0.125D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired, and setDefaultAmmoExpensive - the item-container ammo-grant
        // system halves normal grants but never grants "expensive" default ammo at all, per the
        // report's ItemAmmoContainer finding): NUKE_STANDARD x1
    }

    // ==================== impact lambdas ====================

    /** Port of CE's {@code XFactoryEnergy.LAMBDA_LIGHTNING_HIT} - small area blast + slowness/mining-fatigue on the direct hit. */
    private static void lightningHit(EntityBulletBeamBase beam, HitResult hit) {
        Vec3 loc = resolveImpactPoint(hit);

        ExplosionVNT vnt = new ExplosionVNT(beam.level(), loc.x, loc.y, loc.z, 2F, beam.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, beam.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();

        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 9));
            living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60, 9));
        }

        // CE (upstream/hbm-ce/.../factory/XFactoryEnergy.java:79-93): an extra sound pair plus a
        // 3-shot PlasmaBlast fan (pitch -60/0/60, shared random yaw, blue r=0.5,g=0.5,b=1.0, scale=2,
        // radius 100) on top of the standard beam-hit blast above.
        beam.level().playSound(null, loc.x, loc.y, loc.z, HBMSoundHandler.ufoBlast.get(), net.minecraft.sounds.SoundSource.PLAYERS, 5.0F, 0.9F + beam.level().getRandom().nextFloat() * 0.2F);
        beam.level().playSound(null, loc.x, loc.y, loc.z, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_BLAST, net.minecraft.sounds.SoundSource.PLAYERS, 5.0F, 0.5F);

        float sharedYaw = beam.level().getRandom().nextFloat() * 180F;
        for (int i = 0; i < 3; i++) {
            CompoundTag data = new CompoundTag();
            data.putFloat("r", 0.5F);
            data.putFloat("g", 0.5F);
            data.putFloat("b", 1.0F);
            data.putFloat("pitch", -60F + 60F * i);
            data.putFloat("yaw", sharedYaw);
            data.putFloat("scale", 2F);
            HbmEffect.sendPacket(beam.level(), HbmEffect.PLASMA_BLAST, loc.x, loc.y, loc.z, 100, data);
        }
    }

    /** Port of CE's {@code LAMBDA_LIGHTNING_SPLIT} - the same hit, then a fan-out of short sub-beams toward every nearby living entity. */
    private static void lightningSplit(EntityBulletBeamBase beam, HitResult hit) {
        lightningHit(beam, hit);
        if (!(hit instanceof EntityHitResult ehr)) return;

        Vec3 loc = ehr.getLocation();
        double range = 20;
        Level level = beam.level();
        List<LivingEntity> potentialTargets = level.getEntitiesOfClass(LivingEntity.class, new AABB(loc.x, loc.y, loc.z, loc.x, loc.y, loc.z).inflate(range));

        for (LivingEntity target : potentialTargets) {
            if (target == beam.getThrower() || target == ehr.getEntity()) continue;

            Vec3 delta = new Vec3(target.getX() - loc.x, target.getY() + target.getBbHeight() / 2D - loc.y, target.getZ() - loc.z);
            if (delta.length() > range) continue;

            EntityBulletBeamBase sub = new EntityBulletBeamBase(level, energy_tesla_ir_sub, beam.damage);
            sub.thrower = beam.getThrower();
            sub.setPos(loc);
            sub.setRotationsFromVector(delta);
            sub.performHitscanExternal(delta.length());
            level.addFreshEntity(sub);
        }
    }

    /** Port of CE's {@code LAMBDA_IR_HIT} - standard beam damage, then sets a hit living entity on fire (block-ignite branch is a documented forward reference, see class javadoc). */
    private static void irHit(EntityBulletBeamBase beam, HitResult hit) {
        BulletConfig.LAMBDA_STANDARD_BEAM_HIT.accept(beam, hit);

        if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity living) {
            HbmLivingAttachment props = HbmLivingAttachment.getData(living);
            if (props.getFire() < 100) {
                props.setFire(100);
                living.setData(ModAttachments.LIVING_ATTACHMENT, props);
            }
        }
        // TODO(entity-effect-fire-lingering): CE also ignites an adjacent flammable block / spawns an
        // EntityFireLingering ground-fire puddle on a block hit - see class javadoc's forward reference.
    }

    private static Vec3 resolveImpactPoint(HitResult hit) {
        if (hit instanceof BlockHitResult bhr) {
            var dir = bhr.getDirection();
            return bhr.getLocation().add(dir.getStepX() * 0.5, dir.getStepY() * 0.5, dir.getStepZ() * 0.5);
        }
        return hit.getLocation();
    }

    // ==================== gun_fatman nuke-round impact lambdas ====================
    // SatelliteDetector calls are still dropped (com.hbm.saveddata.satellites.SatelliteDetector is a
    // separate, not-yet-ported system - documented forward reference, see class javadoc); incrementRad
    // is now wired against Phase 4's real com.hbm.handler.radiation.ChunkRadiationManager.

    private static void nukeStandard(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 10);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        incrementRad(bullet.level(), loc.x, loc.y, loc.z, 1F);
        spawnMush(bullet, loc);
    }

    private static void nukeDemo(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 10);
        vnt.setBlockAllocator(new BlockAllocatorStandard(64));
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        incrementRad(bullet.level(), loc.x, loc.y, loc.z, 1.5F);
        spawnMush(bullet, loc);
    }

    private static void nukeHigh(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        bullet.level().addFreshEntity(EntityNukeExplosionMK5.statFac(bullet.level(), 35, loc.x, loc.y, loc.z));
        spawnMush(bullet, loc);
    }

    private static void nukeBalefire(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 10);
        vnt.setBlockAllocator(new BlockAllocatorStandard(64));
        vnt.setBlockProcessor(new BlockProcessorStandard().withBlockEffect(new BlockMutatorBalefire()));
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        incrementRad(bullet.level(), loc.x, loc.y, loc.z, 1.5F);
        bullet.level().playSound(null, loc.x, loc.y + 0.5, loc.z, HBMSoundHandler.mukeExplosion.get(), net.minecraft.sounds.SoundSource.HOSTILE, 15.0F, 1.0F);
        HbmEffect.sendPacket(bullet.level(), HbmEffect.MUKE, loc.x, loc.y + 0.5, loc.z, 250, null);
    }

    private static void nukeTinyTot(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 5);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(2, bullet.damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.explode();
        incrementRad(bullet.level(), loc.x, loc.y, loc.z, 0.25F);
        EntityNukeTorex.statFac(bullet.level(), loc.x, loc.y + 0.5, loc.z, 0.25F);
    }

    /**
     * CE: {@code XFactoryCatapult.incrementRad(World, double, double, double, float)} - a 5x5
     * chunk-cross ambient-radiation bump around a nuke round's impact point, one
     * {@code ChunkRadiationManager.proxy.incrementRad} call per chunk in the (Manhattan-distance &lt; 4)
     * diamond, falling off as {@code 50/(|i|+|j|+1)} scaled by {@code mult}.
     */
    private static void incrementRad(Level level, double x, double y, double z, float mult) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (Math.abs(i) + Math.abs(j) < 4) {
                    BlockPos pos = BlockPos.containing(x + i * 16, y, z + j * 16);
                    ChunkRadiationManager.proxy.incrementRad(level, pos, (50F / (Math.abs(i) + Math.abs(j) + 1)) * mult);
                }
            }
        }
    }

    private static void nukeHive(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        bullet.discard();
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 5);
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage).withRangeMod(1.5F));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }

    private static void spawnMush(EntityBulletBaseMK4 bullet, Vec3 loc) {
        EntityNukeTorex.statFac(bullet.level(), loc.x, loc.y + 0.5, loc.z, 0.4F);
    }

    private static boolean skipSelfHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        return hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower();
    }
}
