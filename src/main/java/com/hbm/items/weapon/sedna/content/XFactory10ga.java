package com.hbm.items.weapon.sedna.content;

import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.EntityProcessorCrossSmooth;
import com.hbm.explosion.vanillant.standard.ExplosionEffectTiny;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory10ga} - the 10ga ammo family (5
 * {@link BulletConfig}s, {@code BUCKSHOT_ADVANCED} casing exclusively) and its 3 guns
 * ({@code gun_double_barrel}, {@code gun_double_barrel_sacred_dragon}, and - despite the "auto_shotgun"
 * name and despite being grouped under the 12ga family in this task's roster brief -
 * {@code gun_autoshotgun_heretic}, which CE's real source constructs inside {@code XFactory10ga.init()}
 * because it actually consumes 10ga ammo (G10 default, this file's mag), not 12ga; mirrored here
 * exactly rather than in {@link XFactory12ga} to match CE's real file structure). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory10ga} table; cross-checked against a full read
 * of CE's real {@code XFactory10ga.java}.
 * <p>
 * See {@link XFactory556mm}'s class javadoc for why every ammo/{@code BulletConfig} field here is a
 * plain eager {@code static final} and why {@code .setCasing(...)}/{@code .smoke(...)}/
 * {@code .anim(...)}/{@code .orchestra(...)}/{@code setDefaultAmmo(...)} are all omitted.
 * <p>
 * <b>Guns are static METHODS, not fields</b> - see {@link XFactory556mm}'s javadoc on
 * {@code gun_g3()} for why: constructing an {@code ItemGunBaseNT} here resolves a SoundEvent
 * {@code DeferredHolder} via {@code Receiver.sound(...).get()}, which throws
 * {@code IllegalStateException} if evaluated eagerly at class-load time (before
 * {@code RegisterEvent(SOUND_EVENT)} has fired). {@link GunShotgunItems} wraps each of these in a
 * method-reference {@code Supplier} for {@code DeferredRegister}.
 */
public final class XFactory10ga {

    private XFactory10ga() {
    }

    // ==================== ammo (5) ====================
    // .setCasing(...) intentionally omitted - see class javadoc. CE's exact casing
    // (BUCKSHOT_ADVANCED x4 for every round in this family) is preserved below for whoever wires
    // that family + Ammo Press.

    /** casing: BUCKSHOT_ADVANCED x4 */
    public static Item ITEM_G10;
    /** casing: BUCKSHOT_ADVANCED x4 */
    public static Item ITEM_G10_SHRAPNEL;
    /** casing: BUCKSHOT_ADVANCED x4 */
    public static Item ITEM_G10_DU;
    /** casing: BUCKSHOT_ADVANCED x4 */
    public static Item ITEM_G10_SLUG;
    /** casing: BUCKSHOT_ADVANCED x4 */
    public static Item ITEM_G10_EXPLOSIVE;

    private static final float BUCKSHOT_SPREAD = 0.035F;

    /**
     * Reimplementation of CE's {@code Lego.tinyExplode(bullet, mop, range)} at range 1.5F - the exact
     * same shared lambda CE's {@code r762_he} (see {@code XFactory762mm.tinyExplode}'s javadoc for why
     * this is duplicated locally per family rather than added to the shared {@code Lego.java}) and
     * this file's {@code g10_explosive} both bind to in real CE via a static import. Skips CE's
     * block-facing impact-offset nuance, matching that same precedent's documented simplification.
     */
    private static void tinyExplode(EntityBulletBaseMK4 bullet, HitResult hr, float range) {
        Vec3 hit = hr.getLocation();
        ExplosionVNT vnt = new ExplosionVNT(bullet.level(), hit.x, hit.y, hit.z, range, bullet.getThrower());
        vnt.setEntityProcessor(new EntityProcessorCrossSmooth(1, bullet.damage)
                .setupPiercing(bullet.config.armorThresholdNegation, bullet.config.armorPiercingPercent)
                .setKnockback(0.25D));
        vnt.setPlayerProcessor(new PlayerProcessorStandard());
        vnt.setSFX(new ExplosionEffectTiny());
        vnt.explode();
    }

    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_TINY_EXPLODE = (bullet, hr) -> {
        if (hr instanceof EntityHitResult ehr) {
            Entity hitEntity = ehr.getEntity();
            if (bullet.tickCount < 3 && hitEntity == bullet.getThrower()) return;
        }
        tinyExplode(bullet, hr, 1.5F);
        bullet.discard();
    };

    public static final BulletConfig g10 = new BulletConfig("g10").setItem(() -> ITEM_G10)
            .setProjectiles(10).setDamage(1F / 10F).setSpread(BUCKSHOT_SPREAD).setRicochetAngle(15F).setThresholdNegation(5F);
    public static final BulletConfig g10_shrapnel = new BulletConfig("g10_shrapnel").setItem(() -> ITEM_G10_SHRAPNEL)
            .setProjectiles(10).setDamage(1F / 10F).setSpread(BUCKSHOT_SPREAD).setRicochetAngle(90F).setRicochetCount(15).setThresholdNegation(5F);
    public static final BulletConfig g10_du = new BulletConfig("g10_du").setItem(() -> ITEM_G10_DU)
            .setProjectiles(10).setDamage(1F / 4F).setSpread(BUCKSHOT_SPREAD).setRicochetAngle(15F).setThresholdNegation(10F).setArmorPiercing(0.2F)
            .setDoesPenetrate(true).setDamageFalloffByPen(false);
    public static final BulletConfig g10_slug = new BulletConfig("g10_slug").setItem(() -> ITEM_G10_SLUG)
            .setRicochetAngle(15F).setThresholdNegation(10F).setArmorPiercing(0.1F).setDoesPenetrate(true);
    public static final BulletConfig g10_explosive = new BulletConfig("g10_explosive").setItem(() -> ITEM_G10_EXPLOSIVE)
            .setWear(3F).setProjectiles(10).setDamage(1F / 4F).setSpread(BUCKSHOT_SPREAD).setOnImpact(LAMBDA_TINY_EXPLODE);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_DOUBLE_BARREL =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    /**
     * Port of CE's {@code LAMBDA_DOUBLE_SECONDARY} - fires the second barrel on RMB, an identical copy
     * of {@link Lego#clickReceiver}'s IDLE-fire branch bound to the secondary press slot instead of
     * primary (CE defines this as its own bespoke lambda rather than reusing {@code clickReceiver}
     * directly, so this mirrors that exactly rather than trying to call {@code clickReceiver} with a
     * receiver index {@code Lego} doesn't expose a public overload for at press-time).
     */
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_DOUBLE_SECONDARY = (stack, ctx) -> {
        LivingEntity entity = ctx.entity;
        Receiver rec = ctx.config.getReceivers(stack)[0];
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == ItemGunBaseNT.GunState.IDLE) {
            if (rec.getCanFire(stack).apply(stack, ctx)) {
                rec.getOnFire(stack).accept(stack, ctx);
                if (rec.getFireSound(stack) != null) {
                    entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), rec.getFireSound(stack), SoundSource.PLAYERS, rec.getFireVolume(stack), rec.getFirePitch(stack));
                }
                ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.COOLDOWN);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterFire(stack));
            } else if (rec.getDoesDryFire(stack)) {
                ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.CYCLE_DRY, index);
                ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.DRAWING);
                ItemGunBaseNT.setTimer(stack, index, rec.getDelayAfterDryFire(stack));
            }
        }
        if (state == ItemGunBaseNT.GunState.RELOADING) {
            ItemGunBaseNT.setReloadCancel(stack, true);
        }
    };

    // ==================== guns (3) ====================

    public static ItemGunBaseNT gun_double_barrel() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SPECIAL,
            new GunConfig()
                    .dura(1_000).draw(10).inspect(39).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(30F).rounds(2).delay(10).reload(41).reloadOnEmpty(true).sound(HBMSoundHandler.fireShotgun.get(), 1.0F, 0.9F)
                            .mag(new MagazineFullReload(0, 2).addConfigs(g10, g10_shrapnel, g10_du, g10_slug, g10_explosive))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_DOUBLE_BARREL))
                    .setupStandardConfiguration().ps(LAMBDA_DOUBLE_SECONDARY)
            // default ammo (not yet wired): G10 x6
        );
    }

    public static ItemGunBaseNT gun_double_barrel_sacred_dragon() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
            new GunConfig()
                    .dura(6_000).draw(10).inspect(39).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(45F).spreadAmmo(1.35F).rounds(2).delay(10).reload(41).reloadOnEmpty(true).sound(HBMSoundHandler.fireShotgun.get(), 1.0F, 0.9F)
                            .mag(new MagazineFullReload(0, 2).addConfigs(g10, g10_shrapnel, g10_du, g10_slug, g10_explosive))
                            .offset(0.75, -0.0625, -0.1875D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_DOUBLE_BARREL))
                    .setupStandardConfiguration().ps(LAMBDA_DOUBLE_SECONDARY)
            // default ammo (not yet wired): G10_DU x6
        );
    }

    /**
     * DEBUG-quality gun; CE never calls {@code .dura(...)} for this one either (0 durability, matching
     * this port's {@link GunConfig#durability_DNA} default), and fires via
     * {@link Lego#LAMBDA_NOWEAR_FIRE} (never accrues wear) exactly like {@code gun_autoshotgun_heretic}'s
     * 12ga-family debug siblings.
     */
    public static ItemGunBaseNT gun_autoshotgun_heretic() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.DEBUG,
            new GunConfig()
                    .draw(20).inspect(65).reloadSequential(true).inspectCancel(false).crosshair(Crosshair.L_CIRCLE).hideCrosshair(false)
                    .rec(new Receiver(0)
                            .dmg(100F).delay(3).auto(true).dryfireAfterAuto(true).reload(110).jam(19).sound(HBMSoundHandler.fireShotgunAuto.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 250).addConfigs(g10, g10_shrapnel, g10_du, g10_slug, g10_explosive))
                            .offset(0.75, -0.125, -0.25D)
                            .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE).recoil(XFactory12ga.LAMBDA_RECOIL_SEXY))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): G10 x50
        );
    }
}
