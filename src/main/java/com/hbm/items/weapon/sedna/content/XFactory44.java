package com.hbm.items.weapon.sedna.content;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.items.weapon.sedna.mags.MagazineSingleReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory44} - the .44 ammo family (6
 * {@link BulletConfig}s plus 2 secret "equestrian" novelty rounds) and its 6 guns
 * ({@code gun_henry}/{@code _lincoln}, {@code gun_heavy_revolver}/{@code _lilmac}/{@code _protege},
 * {@code gun_hangman}). See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory44} table.
 * <p>
 * Ammo {@link Item}s/{@link BulletConfig}s are plain eager {@code static final} fields (see
 * {@code XFactory556mm}'s class javadoc for why that's safe) and {@code .setCasing(...)} is omitted
 * (see the same javadoc - no shared casing-item family exists yet). <b>Every gun below is a static
 * method, not an eager field</b> - see {@link XFactoryBlackPowder}'s class javadoc for the full
 * rationale (defers every {@code HBMSoundHandler.xxx.get()} call past mod-construction time, past
 * the point where it would throw); {@link GunPistolItems} calls each method from inside the
 * {@code Supplier} it hands {@code ModItems.ITEMS.register(...)}.
 * <p>
 * Not ported (weapon-mod-eval-gated, {@code XWeaponModManager} out of scope - see this task's brief):
 * {@code gun_heavy_revolver}'s {@code LAMBDA_NAME_NOPIP} "_scoped" name mutator,
 * {@code gun_heavy_revolver_lilmac}'s {@code .scopeTexture(...)} call ({@link GunConfig} has no such
 * setter yet either way - client-rendering-only slot).
 * <p>
 * {@code m44_equestrian_pip}/{@code _mn7}'s CE {@code onImpact} spawns {@code EntityBoxcar}/
 * {@code EntityTorpedo} (an "MLP" easter egg per CE's own source comments) - neither entity exists in
 * this port yet (grepped, confirmed absent; vehicle/joke-entity content, not this package's scope).
 * Left with no {@code onImpact} so the 0-damage novelty round still fires/consumes correctly; wire the
 * real spawn once those entities land.
 */
public final class XFactory44 {

    private XFactory44() {
    }

    // ==================== ammo (6 + 2 secret) ====================
    // .setCasing(...) intentionally omitted - see XFactory556mm's class javadoc (no shared casing-item
    // family registered anywhere in this port yet). CE's casing type + count preserved per field below.

    /** casing: SMALL x12 */
    public static Item ITEM_M44_BP;
    /** casing: SMALL x6 */
    public static Item ITEM_M44_SP;
    /** casing: SMALL x6 */
    public static Item ITEM_M44_FMJ;
    /** casing: SMALL x6 */
    public static Item ITEM_M44_JHP;
    /** casing: SMALL_STEEL x6 */
    public static Item ITEM_M44_AP;
    /** casing: SMALL x6 */
    public static Item ITEM_M44_EXPRESS;
    /** CE's ammo_secret M44_EQUESTRIAN constant - one shared item, two BulletConfigs (pip/mn7 impact effects). Hidden from the creative tab, matching CE. */
    public static Item ITEM_M44_EQUESTRIAN;

    public static final BulletConfig m44_bp = new BulletConfig("m44_bp").setItem(() -> ITEM_M44_BP)
            .setDamage(0.75F).setBlackPowder(true);
    public static final BulletConfig m44_sp = new BulletConfig("m44_sp").setItem(() -> ITEM_M44_SP);
    public static final BulletConfig m44_fmj = new BulletConfig("m44_fmj").setItem(() -> ITEM_M44_FMJ)
            .setDamage(0.8F).setThresholdNegation(3F).setArmorPiercing(0.1F);
    public static final BulletConfig m44_jhp = new BulletConfig("m44_jhp").setItem(() -> ITEM_M44_JHP)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig m44_ap = new BulletConfig("m44_ap").setItem(() -> ITEM_M44_AP)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(7.5F)
            .setArmorPiercing(0.15F);
    public static final BulletConfig m44_express = new BulletConfig("m44_express").setItem(() -> ITEM_M44_EXPRESS)
            .setDoesPenetrate(true).setDamage(1.5F).setThresholdNegation(3F).setArmorPiercing(0.1F).setWear(1.5F);

    // TODO(phase3/phase4-entities): see class javadoc - EntityBoxcar/EntityTorpedo don't exist yet.
    public static final BulletConfig m44_equestrian_pip = new BulletConfig("m44_equestrian_pip").setItem(() -> ITEM_M44_EQUESTRIAN)
            .setDamage(0F);
    public static final BulletConfig m44_equestrian_mn7 = new BulletConfig("m44_equestrian_mn7").setItem(() -> ITEM_M44_EQUESTRIAN)
            .setDamage(0F);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_HENRY =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian()));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_NOPIP =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_HANGMAN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian()));

    // ==================== guns (6) ====================
    // Static methods, not eager fields - see class javadoc.

    public static ItemGunBaseNT gun_henry() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(10F).delay(20).reload(25, 11, 14, 8).jam(45)
                                .sound(HBMSoundHandler.fireRifle.get(), 1.0F, 1.0F)
                                .mag(new MagazineSingleReload(0, 14).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
                                .offset(0.75, -0.0625, -0.1875D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_HENRY))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): M44_SP x14
    }

    public static ItemGunBaseNT gun_henry_lincoln() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
                new GunConfig()
                        .dura(300).draw(15).inspect(23).reloadSequential(true).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(20F).spreadHipfire(0F).delay(20).reload(25, 11, 14, 8).jam(45)
                                .sound(HBMSoundHandler.fireRifle.get(), 1.0F, 1.25F)
                                .mag(new MagazineSingleReload(0, 14).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
                                .offset(0.75, -0.0625, -0.1875D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_HENRY))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): M44_JHP x14
    }

    /** CE also applies a "_scoped" name mutator gated on a weapon-mod upgrade - skipped, see class javadoc. Default ammo (not yet wired): M44_SP x12. */
    public static ItemGunBaseNT gun_heavy_revolver() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(600).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                        .rec(new Receiver(0)
                                .dmg(15F).delay(14).reload(46).jam(23).sound(HBMSoundHandler.shoot44.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
                                .offset(0.75, -0.0625, -0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_NOPIP))
                        .setupStandardConfiguration());
    }

    /** CE also sets a scope texture (client-rendering slot, not ported onto GunConfig) - skipped, see class javadoc. Default ammo (not yet wired): M44_JHP x12. */
    public static ItemGunBaseNT gun_heavy_revolver_lilmac() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
                new GunConfig()
                        .dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                        .rec(new Receiver(0)
                                .dmg(30F).delay(14).reload(46).jam(23).sound(HBMSoundHandler.shoot44.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(m44_equestrian_pip, m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
                                .offset(0.75, -0.0625, -0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_NOPIP))
                        .setupStandardConfiguration());
    }

    public static ItemGunBaseNT gun_heavy_revolver_protege() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
                new GunConfig()
                        .dura(31_000).draw(10).inspect(23).crosshair(Crosshair.L_CLASSIC)
                        .rec(new Receiver(0)
                                .dmg(30F).delay(14).reload(46).jam(23).sound(HBMSoundHandler.shoot44.get(), 1.0F, 0.8F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(m44_equestrian_mn7, m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
                                .offset(0.75, -0.0625, -0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_NOPIP))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): M44_JHP x12
    }

    /**
     * Port of CE's {@code SMACK_A_FUCKER} secondary-press lambda for {@code gun_hangman} - forces an
     * inspect animation on right-click whenever idle or mid-cycle-animation. Not weapon-mod related
     * (unlike the name-mutator/scope-texture skips above), so it is ported. {@code ItemGunBaseNT}'s
     * {@code getLastAnim} returns a raw ordinal (see that class's javadoc), compared against
     * {@link GunAnimationType#CYCLE}'s ordinal since CE compares against its own {@code GunAnimation.CYCLE}.
     */
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> HANGMAN_INSPECT_ON_SECONDARY = (stack, ctx) -> {
        if (ItemGunBaseNT.getState(stack, ctx.configIndex) == ItemGunBaseNT.GunState.IDLE
                || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimationType.CYCLE.ordinal()) {
            ItemGunBaseNT.setState(stack, ctx.configIndex, ItemGunBaseNT.GunState.DRAWING);
            ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.INSPECT, ctx.configIndex);
        }
    };

    /** Default ammo (not yet wired): M44_FMJ x16. */
    public static ItemGunBaseNT gun_hangman() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
                new GunConfig()
                        .dura(600).draw(10).inspect(31).inspectCancel(false).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(25F).delay(10).reload(46).jam(23).sound(HBMSoundHandler.shoot44.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 8).addConfigs(m44_bp, m44_sp, m44_fmj, m44_jhp, m44_ap, m44_express))
                                .offset(1, -0.0625 * 2.5, -0.25D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_HANGMAN))
                        .setupStandardConfiguration().ps(HANGMAN_INSPECT_ON_SECONDARY));
    }
}
