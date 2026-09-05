package com.hbm.items.weapon.sedna.content;

import com.hbm.entity.effect.EntityFireLingering;
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
import com.hbm.items.weapon.sedna.impl.ItemGunStinger;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * INC/PHOS linger is Exact CE {@code XFactoryRocket.java:114-138} via registered
 * {@link EntityFireLingering} (6×2, 300t DIESEL / 600t PHOSPHORUS) plus the 5×5×5
 * adjacent-flammable ignite loop.
 * <p>
 * Stinger lock-on is Exact CE {@code ItemGunStinger.java:36-76} (60-tick progress, ADS + secondary)
 * plus {@code getLockonTarget :87-124}. Missile-launcher ADS primary uses the same scan at
 * {@code 150D}/{@code 20D} ({@code XFactoryRocket.java:220-230}). HUD lock-on bar skipped.
 */
public final class XFactoryRocket {

    private XFactoryRocket() {
    }

    // ==================== ammo (shared 5-round template) ====================

    public static Item ITEM_ROCKET_HE;
    public static Item ITEM_ROCKET_HEAT;
    public static Item ITEM_ROCKET_DEMO;
    public static Item ITEM_ROCKET_INC;
    public static Item ITEM_ROCKET_PHOSPHORUS;

    private static final Consumer<Entity> LAMBDA_ACCELERATE = entity -> {
        if (entity instanceof EntityBulletBaseMK4 bullet && bullet.accel < 7) bullet.accel += 0.4D;
    };

    public static final BulletConfig rocket_he = new BulletConfig("rocket_he").setItem(() -> ITEM_ROCKET_HE)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setOnImpact(XFactoryRocket::explodeHe);
    public static final BulletConfig rocket_heat = new BulletConfig("rocket_heat").setItem(() -> ITEM_ROCKET_HEAT)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.5F).setOnImpact(XFactoryRocket::explodeHeat);
    public static final BulletConfig rocket_demo = new BulletConfig("rocket_demo").setItem(() -> ITEM_ROCKET_DEMO)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.75F).setOnImpact(XFactoryRocket::explodeDemo);
    public static final BulletConfig rocket_inc = new BulletConfig("rocket_inc").setItem(() -> ITEM_ROCKET_INC)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.75F).setOnImpact((bullet, hit) -> spawnFire(bullet, hit, false, 300));
    public static final BulletConfig rocket_phosphorus = new BulletConfig("rocket_phosphorus").setItem(() -> ITEM_ROCKET_PHOSPHORUS)
            .setLife(300).setSelfDamageDelay(10).setVel(0F).setGrav(0).setOnEntityHit(null).setOnRicochet(null).setOnUpdate(LAMBDA_ACCELERATE)
            .setDamage(0.75F).setOnImpact((bullet, hit) -> spawnFire(bullet, hit, true, 600));

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
        return new ItemGunStinger(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(7).inspect(40).crosshair(Crosshair.L_BOX_OUTLINE)
                        .rec(new Receiver(0)
                                .dmg(35F).delay(5).reload(50).jam(40).sound(HBMSoundHandler.rpgShoot.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 1).addConfigs(rocket_he, rocket_heat, rocket_demo, rocket_inc, rocket_phosphorus))
                                .offset(1, -0.09375, -0.1875D)
                                .setupLockonFire())
                        .setupStandardConfiguration()
                        .ps(LAMBDA_STINGER_SECONDARY_PRESS).rs(LAMBDA_STINGER_SECONDARY_RELEASE));
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

    /** Exact CE {@code XFactoryRocket.java:217-218}. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_STINGER_SECONDARY_PRESS =
            (stack, ctx) -> ItemGunStinger.setIsLockingOn(stack, true);
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_STINGER_SECONDARY_RELEASE =
            (stack, ctx) -> ItemGunStinger.setIsLockingOn(stack, false);

    /** Exact CE {@code XFactoryRocket.java:220-230}. */
    public static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_MISSILE_LAUNCHER_PRIMARY_PRESS = (stack, ctx) -> {
        if (ItemGunBaseNT.getIsAiming(stack)) {
            int target = ItemGunStinger.getLockonTarget(ctx.getPlayer(), 150D, 20D);
            if (target != -1) {
                ItemGunBaseNT.setLockonTarget(stack, target);
                ItemGunBaseNT.setIsLockedOn(stack, true);
            }
        }
        Lego.LAMBDA_STANDARD_CLICK_PRIMARY.accept(stack, ctx);
        ItemGunBaseNT.setIsLockedOn(stack, false);
    };

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

    /** Exact CE {@code XFactoryRocket.java:114-138}. */
    public static void spawnFire(EntityBulletBaseMK4 bullet, HitResult mop, boolean phosphorus, int duration) {
        if (mop instanceof EntityHitResult && bullet.tickCount < 3) return;
        Vec3 hit = mop.getLocation();
        Level world = bullet.level();
        standardExplode(bullet, mop, 3F, 1F);
        EntityFireLingering.spawn(world, hit.x, hit.y, hit.z, 6F, 2F,
                phosphorus ? EntityFireLingering.TYPE_PHOSPHORUS : EntityFireLingering.TYPE_DIESEL, duration);
        bullet.discard();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = new BlockPos((int) Math.floor(hit.x) + dx, (int) Math.floor(hit.y) + dy, (int) Math.floor(hit.z) + dz);
                    if (!world.getBlockState(pos).isAir()) continue;
                    for (Direction dir : Direction.values()) {
                        BlockPos adj = pos.relative(dir);
                        BlockState neighbor = world.getBlockState(adj);
                        if (neighbor.isFlammable(world, adj, dir.getOpposite())) {
                            world.setBlock(pos, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }
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
