package com.hbm.items.weapon.sedna.content;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectWeapon;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryRocket} - the 4 rocket launchers,
 * all sharing one {@code LARGE}-casing 5-round ammo template (HE/HEAT/DEMO/INC/PHOSPHORUS). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryRocket} table.
 * <p>
 * <b>{@code Lego.standardExplode} is reimplemented locally, not added to the shared {@code Lego}
 * class</b>: that class's own javadoc explicitly documents {@code standardExplode}/{@code tinyExplode}
 * as out of Package B's scope ("explosive-ammo impact handlers... Package D content"), so
 * {@link #standardExplode} below is this content package's own copy of CE's real
 * {@code Lego.standardExplode(EntityBulletBaseMK4, RayTraceResult, float)} body (confirmed against
 * CE source), built purely on this port's already-landed {@code ExplosionVNT} stack - matching the
 * precedent {@code GrenadeFillingActions} already set for the same situation.
 * <p>
 * <b>Forward references (documented, not silently dropped):</b>
 * <ul>
 *     <li>{@code EntityFireLingering} (the INC/PHOSPHORUS rounds' lingering ground fire) - confirmed
 *     not ported anywhere in this tree, same gap {@code XFactoryEnergy}/{@code GrenadeFillingActions}
 *     already documented; the explosion half of each incendiary round's impact is still real.</li>
 *     <li>{@code gun_stinger}/{@code gun_missile_launcher}'s target lock-on acquisition - CE drives
 *     this through {@code com.hbm.items.weapon.sedna.impl.ItemGunStinger}, a bespoke subclass with its
 *     own tick-based locking-progress state machine that does not exist in this port. Both guns are
 *     registered as plain {@link ItemGunBaseNT}s with a real, self-contained lock-on scan
 *     ({@link #findLockonTarget}, a nearest-entity-in-cone search) substituted for
 *     {@code ItemGunStinger.getLockonTarget} - same observable behavior (aim near a target, it locks,
 *     the fired round homes in via {@link EntityBulletBaseMK4}'s already-ported {@code lockonTarget}
 *     field), without the missing subclass's own multi-tick "locking..." progress readout.</li>
 * </ul>
 */
public final class XFactoryRocket {

    private XFactoryRocket() {
    }

    // ==================== ammo (shared 5-round template) ====================

    public static final Item ITEM_ROCKET_HE = new Item(new Item.Properties());
    public static final Item ITEM_ROCKET_HEAT = new Item(new Item.Properties());
    public static final Item ITEM_ROCKET_DEMO = new Item(new Item.Properties());
    public static final Item ITEM_ROCKET_INC = new Item(new Item.Properties());
    public static final Item ITEM_ROCKET_PHOSPHORUS = new Item(new Item.Properties());

    private static final Consumer<Entity> LAMBDA_ACCELERATE = entity -> {
        if (entity instanceof EntityBulletBaseMK4 bullet && bullet.accel < 7) bullet.accel += 0.4D;
    };

    public static final BulletConfig rocket_he = new BulletConfig("rocket_he").setItem(ITEM_ROCKET_HE)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setOnImpact(XFactoryRocket::explodeHe);
    public static final BulletConfig rocket_heat = new BulletConfig("rocket_heat").setItem(ITEM_ROCKET_HEAT)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.5F).setOnImpact(XFactoryRocket::explodeHeat);
    public static final BulletConfig rocket_demo = new BulletConfig("rocket_demo").setItem(ITEM_ROCKET_DEMO)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.75F).setOnImpact(XFactoryRocket::explodeDemo);
    public static final BulletConfig rocket_inc = new BulletConfig("rocket_inc").setItem(ITEM_ROCKET_INC)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.75F).setOnImpact((bullet, hit) -> explodeIncendiary(bullet, hit, 3F));
    public static final BulletConfig rocket_phosphorus = new BulletConfig("rocket_phosphorus").setItem(ITEM_ROCKET_PHOSPHORUS)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.75F).setOnImpact((bullet, hit) -> explodeIncendiary(bullet, hit, 3F));

    // ==================== guns ====================

    public static ItemGunBaseNT gun_panzerschreck() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(7).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX)
                        .rec(new Receiver(0)
                                .dmg(25F).delay(5).reload(50).jam(40).sound(HBMSoundHandler.rpgShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(rocket_he, rocket_heat, rocket_demo, rocket_inc, rocket_phosphorus))
                                .offset(1, -0.09375, -0.1875D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): ROCKET_HE x3
    }

    public static ItemGunBaseNT gun_stinger() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(7).inspect(40).crosshair(Crosshair.L_BOX_OUTLINE)
                        .rec(new Receiver(0)
                                .dmg(35F).delay(5).reload(50).jam(40).sound(HBMSoundHandler.rpgShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(rocket_he, rocket_heat, rocket_demo, rocket_inc, rocket_phosphorus))
                                .offset(1, -0.09375, -0.1875D)
                                .setupLockonFire())
                        .setupStandardConfiguration()
                        .ps(LAMBDA_STINGER_LOCKON).rs((stack, ctx) -> ItemGunBaseNT.setIsLockedOn(stack, false)));
        // default ammo (not yet wired): ROCKET_HEAT x3
    }

    public static ItemGunBaseNT gun_quadro() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(400).draw(7).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
                        .rec(new Receiver(0)
                                .dmg(40F).spreadHipfire(0F).delay(10).reload(55).jam(40).sound(HBMSoundHandler.rpgShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 4).addConfigs(rocket_he, rocket_heat, rocket_demo, rocket_inc, rocket_phosphorus))
                                .offset(1, -0.09375, -0.1875D)
                                .setupStandardFire())
                        .setupStandardConfiguration());
        // default ammo (not yet wired): ROCKET_HE x4
    }

    public static ItemGunBaseNT gun_missile_launcher() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(500).draw(20).inspect(40).crosshair(Crosshair.L_CIRCUMFLEX).hideCrosshair(false)
                        .rec(new Receiver(0)
                                .dmg(50F).spreadHipfire(0F).delay(5).reload(48).jam(33).sound(HBMSoundHandler.rpgShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(rocket_he, rocket_heat, rocket_demo, rocket_inc, rocket_phosphorus))
                                .offset(1, -0.09375, -0.1875D)
                                .setupStandardFire())
                        .setupStandardConfiguration().pp(LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS));
        // default ammo (not yet wired): ROCKET_HEAT x5
    }

    // ==================== lock-on ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_STINGER_LOCKON = (stack, ctx) -> {
        if (!(ctx.getPlayer() instanceof Player player)) return;
        int target = findLockonTarget(player, 150D, 20D);
        if (target != -1) {
            ItemGunBaseNT.setLockonTarget(stack, target);
            ItemGunBaseNT.setIsLockedOn(stack, true);
        }
    };

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS = (stack, ctx) -> {
        if (ItemGunBaseNT.getIsAiming(stack) && ctx.getPlayer() instanceof Player player) {
            int target = findLockonTarget(player, 150D, 20D);
            if (target != -1) {
                ItemGunBaseNT.setLockonTarget(stack, target);
                ItemGunBaseNT.setIsLockedOn(stack, true);
            }
        }
        Lego.LAMBDA_STANDARD_CLICK_PRIMARY.accept(stack, ctx);
        ItemGunBaseNT.setIsLockedOn(stack, false);
    };

    /**
     * Self-contained replacement for {@code ItemGunStinger.getLockonTarget} (see class javadoc) -
     * nearest {@link LivingEntity} within {@code range} blocks whose direction from the player's eye
     * falls within {@code coneDegrees} of the look vector. Returns the target's entity id, or -1.
     */
    private static int findLockonTarget(Player player, double range, double coneDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double cosThreshold = Math.cos(Math.toRadians(coneDegrees));

        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye.x, eye.y, eye.z, eye.x, eye.y, eye.z).inflate(range))) {
            if (candidate == player || !candidate.isAlive()) continue;
            Vec3 toTarget = candidate.getEyePosition().subtract(eye);
            double dist = toTarget.length();
            if (dist < 0.5 || dist > range) continue;
            if (toTarget.normalize().dot(look) < cosThreshold) continue;
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }

        return best != null ? best.getId() : -1;
    }

    // ==================== impact lambdas ====================

    private static boolean skipSelfHit(EntityBulletBaseMK4 bullet, HitResult hit) {
        return hit instanceof EntityHitResult ehr && bullet.tickCount < 3 && ehr.getEntity() == bullet.getThrower();
    }

    private static void explodeHe(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        standardExplode(bullet, hit, 5F, 1F);
        bullet.discard();
    }

    /** Shaped-charge round: standard blast plus tripled direct-hit damage on the entity actually struck, matching CE's {@code LAMBDA_STANDARD_EXPLODE_HEAT}. */
    private static void explodeHeat(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        standardExplode(bullet, hit, 3.5F, 1F);
        bullet.discard();
        if (hit instanceof EntityHitResult ehr) {
            var source = BulletConfig.getDamage(bullet, bullet.getThrower(), com.hbm.util.DamageResistanceHandler.DamageClass.EXPLOSIVE);
            if (ehr.getEntity() instanceof LivingEntity living) {
                EntityDamageUtil.attackEntityFromNT(living, source, bullet.damage * 3F, true, true, 0.5F, 5F, 0.2F);
            } else {
                EntityDamageUtil.attackEntityFromIgnoreIFrame(ehr.getEntity(), source, bullet.damage * 3F);
            }
        }
    }

    private static void explodeDemo(EntityBulletBaseMK4 bullet, HitResult hit) {
        if (skipSelfHit(bullet, hit)) return;
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, 5F, bullet.getThrower());
        vnt.setBlockAllocator(new BlockAllocatorStandard());
        vnt.setBlockProcessor(new BlockProcessorStandard());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
        bullet.discard();
    }

    /** INC/PHOSPHORUS rounds: standard blast; the lingering ground-fire payload is a documented forward reference (see class javadoc). */
    private static void explodeIncendiary(EntityBulletBaseMK4 bullet, HitResult hit, float range) {
        if (skipSelfHit(bullet, hit)) return;
        standardExplode(bullet, hit, range, 1F);
        bullet.discard();
        // TODO(entity-effect-fire-lingering): CE spawns an EntityFireLingering area-fire puddle plus a
        // 5x5x5 block-ignite scan here - see class javadoc's forward reference.
    }

    /**
     * Port of CE's {@code Lego.standardExplode(EntityBulletBaseMK4, RayTraceResult, float, float)} -
     * see class javadoc for why this lives here rather than in the shared {@code Lego} class.
     */
    private static void standardExplode(EntityBulletBaseMK4 bullet, HitResult hit, float range, float damageMod) {
        Vec3 loc = hit.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), loc.x, loc.y, loc.z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage * damageMod).setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectWeapon(10, 2.5F, 1F));
        vnt.explode();
    }
}
