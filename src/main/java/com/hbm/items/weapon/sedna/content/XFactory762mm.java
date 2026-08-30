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
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory762mm} - the 7.62mm ammo family (6
 * {@link BulletConfig}s, including the explosive {@code r762_he}) plus the 3 "lacunae" laser-beam
 * energy configs {@code gun_minigun_lacunae} reuses, and the family's 5 guns
 * ({@code gun_carbine}/{@code gun_minigun}/{@code gun_minigun_lacunae}/{@code gun_minigun_dual}/
 * {@code gun_mas36}). See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory762mm} table.
 * <p>
 * See {@link XFactory556mm}'s class javadoc for why every field here is a plain eager
 * {@code static final} (not deferred into a supplier) and why {@code .setCasing(...)} is omitted.
 * <p>
 * <b>CAPACITOR/CAPACITOR_OVERCHARGE/CAPACITOR_IR ammo items - a genuinely missing CROSS-PACKAGE
 * dependency, not invented here.</b> CE's {@code energy_lacunae}/{@code _overcharge}/{@code _ir}
 * bind to the same {@code GunFactory.EnumAmmo.CAPACITOR}/{@code CAPACITOR_OVERCHARGE}/
 * {@code CAPACITOR_IR} constants that CE's {@code XFactoryEnergy} guns ({@code gun_tesla_cannon}/
 * {@code gun_laser_pistol}/{@code gun_lasrifle}, not in this batch's roster) also consume - a shared
 * ammo family owned by whichever Phase 3 package ports {@code XFactoryEnergy}, not this one. Minting
 * 3 new "Capacitor" items here would risk a duplicate-{@code DeferredItem} registration race against
 * that concurrent/future package. {@code .setItem(...)} is therefore left uncalled on the 3
 * {@code energy_lacunae*} configs below (verified safe: every magazine/reload code path already
 * null-checks {@code BulletConfig.getAmmo()} before using it, so an ammo-less config simply can never
 * be inventory-reloaded yet, rather than crashing) - confirmed real dependency,
 * {@code com.hbm.items.IngotNuggetItems.INGOT_POLYMER}, is the eventual Ammo Press crafting input
 * per CE's own {@code setCasing(new ItemStack(ModItems.ingot_polymer, 2), 160)} call, for whoever
 * wires the shared Capacitor item + Ammo Press recipe later.
 */
public final class XFactory762mm {

    private XFactory762mm() {
    }

    // ==================== ammo ====================
    // .setCasing(...) omitted for the 6 cased rounds - see class javadoc (shared casing-item family
    // not registered anywhere yet). CE's exact casing (SMALL/SMALL_STEEL x6 for every round in this
    // family) is preserved in each field's comment for whoever wires that family + Ammo Press later.

    /** casing: SMALL x6 */
    public static final Item ITEM_R762_SP = new Item(new Item.Properties());
    /** casing: SMALL x6 */
    public static final Item ITEM_R762_FMJ = new Item(new Item.Properties());
    /** casing: SMALL x6 */
    public static final Item ITEM_R762_JHP = new Item(new Item.Properties());
    /** casing: SMALL_STEEL x6 */
    public static final Item ITEM_R762_AP = new Item(new Item.Properties());
    /** casing: SMALL_STEEL x6 */
    public static final Item ITEM_R762_DU = new Item(new Item.Properties());
    /** casing: SMALL_STEEL x6 */
    public static final Item ITEM_R762_HE = new Item(new Item.Properties());

    /**
     * Reimplementation of CE's {@code Lego.tinyExplode(bullet, mop, range, 1F)} - this port's own
     * {@code Lego.java} explicitly defers {@code tinyExplode}/{@code standardExplode} to "whichever
     * package wires an ammo's onImpact to an explosion" (see that class's javadoc), matching the
     * precedent already set by {@code com.hbm.items.weapon.grenade.GrenadeFillingActions}'s own
     * identical local reimplementation (see that class's javadoc for the same rationale: avoid
     * touching the shared {@code Lego.java} other concurrent "guns" packages may be editing this
     * wave). Skips CE's block-facing impact-offset nuance (0.25 blocks along the hit normal) as an
     * explicitly documented simplification, matching {@code GrenadeFillingActions.pelletTinyExplode}'s
     * own precedent of exploding directly at the hit location.
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

    public static final BulletConfig r762_sp = new BulletConfig("r762_sp").setItem(ITEM_R762_SP);
    public static final BulletConfig r762_fmj = new BulletConfig("r762_fmj").setItem(ITEM_R762_FMJ)
            .setDamage(0.8F).setThresholdNegation(5F).setArmorPiercing(0.1F);
    public static final BulletConfig r762_jhp = new BulletConfig("r762_jhp").setItem(ITEM_R762_JHP)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig r762_ap = new BulletConfig("r762_ap").setItem(ITEM_R762_AP)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(12.5F).setArmorPiercing(0.15F);
    public static final BulletConfig r762_du = new BulletConfig("r762_du").setItem(ITEM_R762_DU)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(2.5F).setThresholdNegation(15F).setArmorPiercing(0.25F);
    public static final BulletConfig r762_he = new BulletConfig("r762_he").setItem(ITEM_R762_HE)
            .setWear(3F).setDamage(2F).setOnImpact(LAMBDA_TINY_EXPLODE);

    // energy_lacunae/_overcharge/_ir - beam ammo, no setItem(...) yet, see class javadoc.
    public static final BulletConfig energy_lacunae = new BulletConfig("energy_lacunae")
            .setupDamageClass(DamageClass.LASER).setBeam().setReloadCount(40).setSpread(0.0F).setLife(5)
            .setRenderRotations(false).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    public static final BulletConfig energy_lacunae_overcharge = new BulletConfig("energy_lacunae_overcharge")
            .setupDamageClass(DamageClass.LASER).setBeam().setReloadCount(40).setSpread(0.0F).setLife(5)
            .setRenderRotations(false).setDoesPenetrate(true).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    /**
     * CE's {@code onImpactBeam} here is {@code XFactoryEnergy.LAMBDA_IR_HIT} (an incendiary-ignite
     * beam-hit variant), not the plain standard beam hit - {@code XFactoryEnergy} is a different,
     * out-of-scope Phase 3 package (see class javadoc), so this falls back to the standard beam-hit
     * lambda (still deals full damage, just without the ignite-on-hit VFX/effect) until that package
     * lands and this can reference the real lambda.
     */
    public static final BulletConfig energy_lacunae_ir = new BulletConfig("energy_lacunae_ir")
            .setupDamageClass(DamageClass.FIRE).setBeam().setReloadCount(40).setSpread(0.0F).setLife(5)
            .setRenderRotations(false).setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_CARBINE =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_MINIGUN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_LACUNAE = (stack, ctx) -> { };

    // ==================== guns ====================

    // Static METHODS, not fields: see XFactory556mm's identical class javadoc note - constructing an
    // ItemGunBaseNT touches Receiver.sound(...).get() (SoundEvent DeferredHolder resolution), which
    // must not happen at class-load time (before RegisterEvent(SOUND_EVENT) has fired).
    public static ItemGunBaseNT gun_carbine() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(3_000).draw(10).inspect(31).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(15F).delay(5).dry(15).spread(0.0F).reload(30, 0, 15, 0).jam(60)
                            .sound(HBMSoundHandler.fireBlackPowder.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 14).addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_CARBINE))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): R762_SP x14
        );
    }

    public static ItemGunBaseNT gun_minigun() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(6F).delay(1).auto(true).dry(15).spread(0.01F)
                            .sound(HBMSoundHandler.calShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineBelt().addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MINIGUN))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): R762_FMJ x30
        );
    }

    public static ItemGunBaseNT gun_minigun_lacunae() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
            new GunConfig()
                    .dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(12F).delay(1).auto(true).dry(15).reload(15).spread(0.01F)
                            .sound(HBMSoundHandler.fireLaserGatling.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 200).addConfigs(energy_lacunae, energy_lacunae_overcharge, energy_lacunae_ir))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_LACUNAE))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): CAPACITOR x15 (moot until the shared Capacitor item lands, see class javadoc)
        );
    }

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_SECOND_MINIGUN = (stack, ctx) -> {
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState lastState = ItemGunBaseNT.getState(stack, index);
        GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
        GunStateDecider.deciderStandardClearJam(stack, lastState, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0);
    };

    public static ItemGunBaseNT gun_minigun_dual() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.DEBUG,
            // config 0 - primary click (left barrel)
            new GunConfig()
                    .dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(6F).delay(1).auto(true).dry(15).spread(0.01F)
                            .sound(HBMSoundHandler.calShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineBelt().addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
                            .offset(1, -0.0625 * 2.5, 0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MINIGUN))
                    .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
            // config 1 - secondary click (right barrel) - matches CE exactly: secondary press routes
            // to the PRIMARY-click fire handler, and its own decider's refire condition checks the
            // SECONDARY button, not primary.
            new GunConfig()
                    .dura(50_000).draw(20).inspect(20).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(6F).delay(1).auto(true).dry(15).spread(0.01F)
                            .sound(HBMSoundHandler.calShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineBelt().addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_MINIGUN))
                    .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                    .decider(LAMBDA_SECOND_MINIGUN)
            // default ammo (not yet wired): R762_SP x50
        );
    }

    public static ItemGunBaseNT gun_mas36() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
            new GunConfig()
                    .dura(5_000).draw(20).inspect(31).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(30F).delay(25).dry(25).spread(0.0F).reload(43).jam(43)
                            .sound(HBMSoundHandler.fireRifleHeavy.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 7).addConfigs(r762_sp, r762_fmj, r762_jhp, r762_ap, r762_du, r762_he))
                            .offset(1, -0.0625 * 1.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_CARBINE))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): R762_AP x14
        );
    }
}
