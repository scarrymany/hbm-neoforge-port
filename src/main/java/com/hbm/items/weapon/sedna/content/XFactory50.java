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
import com.hbm.items.weapon.sedna.mags.MagazineBelt;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory50} - the .50 BMG ammo family (7
 * regular {@link BulletConfig}s plus the 2 secret/easter-egg rounds {@code bmg50_black}/
 * {@code bmg50_equestrian}) and its 4 guns ({@code gun_amat}/{@code gun_amat_subtlety}/
 * {@code gun_amat_penance}/{@code gun_m2}). See {@code docs/phase3/guns_and_ammo.md}'s
 * {@code XFactory50} table.
 * <p>
 * See {@link XFactory556mm}'s class javadoc for why every field here is a plain eager
 * {@code static final} and why {@code .setCasing(...)} is omitted. {@code bmg50_black}/
 * {@code bmg50_equestrian} are still registered as real, giveable items (matching CE's own
 * {@code EnumAmmoSecret} constants, which are real {@code ItemStack} variants) but are deliberately
 * left out of the creative-mode tab in {@link GunRifleItems} - matching CE's own
 * {@code ammo_secret} being hidden from creative search (per {@code docs/phase3/guns_and_ammo.md}'s
 * "The full EnumAmmo roster" section).
 */
public final class XFactory50 {

    private XFactory50() {
    }

    // ==================== ammo ====================
    // .setCasing(...) omitted - see XFactory556mm's javadoc (shared casing-item family not
    // registered anywhere yet). CE's exact casing (LARGE x12 for the 6 base tiers, LARGE_STEEL x6
    // for bmg50_sm) is preserved in each field's comment for whoever wires that family + Ammo Press.

    /** casing: LARGE x12 */
    public static Item ITEM_BMG50_SP;
    /** casing: LARGE x12 */
    public static Item ITEM_BMG50_FMJ;
    /** casing: LARGE x12 */
    public static Item ITEM_BMG50_JHP;
    /** casing: LARGE_STEEL x12 */
    public static Item ITEM_BMG50_AP;
    /** casing: LARGE_STEEL x12 */
    public static Item ITEM_BMG50_DU;
    /** casing: LARGE_STEEL x12 */
    public static Item ITEM_BMG50_HE;
    /** casing: LARGE_STEEL x6 */
    public static Item ITEM_BMG50_SM;
    /** secret round - EnumAmmoSecret.BMG50_BLACK, hidden from creative tab, see class javadoc. */
    public static Item ITEM_BMG50_BLACK;
    /** secret round - EnumAmmoSecret.BMG50_EQUESTRIAN, hidden from creative tab, see class javadoc. */
    public static Item ITEM_BMG50_EQUESTRIAN;

    /**
     * Reimplementation of CE's {@code Lego.tinyExplode(bullet, mop, range, 1F)} - see
     * {@code XFactory762mm.tinyExplode}'s javadoc for why this is duplicated locally per family
     * rather than added to the shared {@code Lego.java} (matches
     * {@code com.hbm.items.weapon.grenade.GrenadeFillingActions}'s own established precedent).
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

    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_STANDARD_EXPLODE = (bullet, hr) -> {
        if (hr instanceof EntityHitResult ehr) {
            Entity hitEntity = ehr.getEntity();
            if (bullet.tickCount < 3 && hitEntity == bullet.getThrower()) return;
        }
        tinyExplode(bullet, hr, 2F);
        bullet.discard();
    };

    /**
     * CE spawns {@code com.hbm.entity.projectile.EntityBuilding} ("silver", an easter-egg
     * structure-spawn joke entity) 50 blocks above the impact point here. {@code EntityBuilding}
     * does not exist anywhere in this port yet (not ported by any landed Phase 3 package) - stubbed
     * to a plain bullet-discard rather than inventing spawn behavior for an entity this ammo-content
     * package does not own. TODO(phase3-easter-eggs): wire the real spawn once EntityBuilding lands.
     */
    private static final BiConsumer<EntityBulletBaseMK4, HitResult> LAMBDA_BUILDING_STUB = (bullet, hr) -> bullet.discard();

    public static final BulletConfig bmg50_sp = new BulletConfig("bmg50_sp").setItem(() -> ITEM_BMG50_SP);
    public static final BulletConfig bmg50_fmj = new BulletConfig("bmg50_fmj").setItem(() -> ITEM_BMG50_FMJ)
            .setDamage(0.8F).setThresholdNegation(7F).setArmorPiercing(0.1F);
    public static final BulletConfig bmg50_jhp = new BulletConfig("bmg50_jhp").setItem(() -> ITEM_BMG50_JHP)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig bmg50_ap = new BulletConfig("bmg50_ap").setItem(() -> ITEM_BMG50_AP)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(17.5F).setArmorPiercing(0.15F);
    public static final BulletConfig bmg50_du = new BulletConfig("bmg50_du").setItem(() -> ITEM_BMG50_DU)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(2.5F).setThresholdNegation(21F).setArmorPiercing(0.25F);
    public static final BulletConfig bmg50_he = new BulletConfig("bmg50_he").setItem(() -> ITEM_BMG50_HE)
            .setWear(3F).setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.75F).setOnImpact(LAMBDA_STANDARD_EXPLODE);
    public static final BulletConfig bmg50_sm = new BulletConfig("bmg50_sm").setItem(() -> ITEM_BMG50_SM)
            .setWear(10F).setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(2.5F).setThresholdNegation(30F).setArmorPiercing(0.35F);
    public static final BulletConfig bmg50_black = new BulletConfig("bmg50_black").setItem(() -> ITEM_BMG50_BLACK)
            .setWear(5F).setDoesPenetrate(true).setDamageFalloffByPen(false).setSpectral(true).setDamage(1.5F).setHeadshot(3F).setThresholdNegation(30F).setArmorPiercing(0.35F);
    public static final BulletConfig bmg50_equestrian = new BulletConfig("bmg50_equestrian").setItem(() -> ITEM_BMG50_EQUESTRIAN)
            .setDamage(0F).setOnImpact(LAMBDA_BUILDING_STUB);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_AMAT =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(12.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_M2 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));

    // ==================== guns ====================

    // Static METHODS, not fields: see XFactory556mm's identical class javadoc note - constructing an
    // ItemGunBaseNT touches Receiver.sound(...).get() (SoundEvent DeferredHolder resolution), which
    // must not happen at class-load time (before RegisterEvent(SOUND_EVENT) has fired).
    public static ItemGunBaseNT gun_amat() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(350).draw(20).inspect(50).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(30F).delay(25).dry(25).spreadHipfire(0.05F).reload(51).jam(43)
                            .sound(HBMSoundHandler.fireAmat.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 7).addConfigs(bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm, bmg50_he))
                            .offset(1, -0.0625 * 1.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_AMAT))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): BMG50_SP x7
        );
    }

    public static ItemGunBaseNT gun_amat_subtlety() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
            new GunConfig()
                    .dura(1_000).draw(20).inspect(50).crosshair(Crosshair.CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(50F).delay(25).dry(25).spreadHipfire(0.05F).reload(51).jam(43)
                            .sound(HBMSoundHandler.fireAmat.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 7).addConfigs(bmg50_equestrian, bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm, bmg50_he))
                            .offset(1, -0.0625 * 1.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_AMAT))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): BMG50_JHP x7
        );
    }

    public static ItemGunBaseNT gun_amat_penance() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
            new GunConfig()
                    .dura(5_000).draw(20).inspect(50).crosshair(Crosshair.CIRCLE).thermalSights(true)
                    .rec(new Receiver(0)
                            .dmg(45F).delay(25).dry(25).spreadHipfire(0F).reload(51).jam(43)
                            .sound(HBMSoundHandler.silencerShoot.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 7).addConfigs(bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_sm, bmg50_he, bmg50_black))
                            .offset(1, -0.0625 * 1.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_AMAT))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): BMG50_JHP x7
        );
    }

    public static ItemGunBaseNT gun_m2() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
            new GunConfig()
                    .dura(3_000).draw(10).inspect(31).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(7.5F).delay(2).dry(10).auto(true).spread(0.005F)
                            .sound(HBMSoundHandler.chekhov_fire.get(), 1.0F, 1.0F)
                            .mag(new MagazineBelt().addConfigs(bmg50_sp, bmg50_fmj, bmg50_jhp, bmg50_ap, bmg50_du, bmg50_he))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_M2))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): BMG50_FMJ x25
        );
    }
}
