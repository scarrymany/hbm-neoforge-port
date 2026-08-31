package com.hbm.items.weapon.sedna.content;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory9mm} - the 9mm ammo family (4
 * {@link BulletConfig}s, {@code SMALL}/{@code SMALL_STEEL} casing) and its 4 guns
 * ({@code gun_greasegun}/{@code gun_lag}/{@code gun_uzi}/{@code gun_uzi_akimbo}). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory9mm} table.
 * <p>
 * Ammo {@link Item}s/{@link BulletConfig}s are plain eager {@code static final} fields (see
 * {@code XFactory556mm}'s class javadoc for why that's safe) and {@code .setCasing(...)} is omitted
 * (see the same javadoc - no shared casing-item family exists yet). <b>Every gun below is a static
 * method, not an eager field</b> - see {@link XFactoryBlackPowder}'s class javadoc for the full
 * rationale (defers every {@code HBMSoundHandler.xxx.get()} call past mod-construction time, past
 * the point where it would throw); {@link GunPistolItems} calls each method from inside the
 * {@code Supplier} it hands {@code ModItems.ITEMS.register(...)}.
 * <p>
 * Not ported (weapon-mod-eval-gated, {@code XWeaponModManager} out of scope - see this task's
 * brief): {@code gun_greasegun}'s {@code LAMBDA_NAME_GREASEGUN} "_m3" name mutator, {@code gun_uzi}'s
 * {@code LAMBDA_NAME_UZI} "_richter" name mutator.
 * <p>
 * {@code gun_uzi_akimbo} is CE's akimbo variant: two independent {@link GunConfig}s sharing one
 * {@link ItemGunBaseNT}, mag indices 0/1, primary press wired to config 0 ({@code pp}, standard
 * decider) and secondary press wired to config 1 ({@code ps}, the bespoke
 * {@link #UZI_AKIMBO_SECOND_DECIDER} that polls the secondary button instead of primary for the
 * auto-refire condition - both receivers are {@code auto(true)}, so this distinction is actually
 * load-bearing here, unlike {@code gun_light_revolver_dani}'s non-auto akimbo pair) - matches CE's
 * own {@code LAMBDA_SECOND_UZI} exactly.
 */
public final class XFactory9mm {

    private XFactory9mm() {
    }

    // ==================== ammo (4) ====================
    // .setCasing(...) intentionally omitted - see XFactory556mm's class javadoc (no shared casing-item
    // family registered anywhere in this port yet). CE's casing type + count preserved per field below.

    /** casing: SMALL x12 */
    public static Item ITEM_P9_SP;
    /** casing: SMALL x12 */
    public static Item ITEM_P9_FMJ;
    /** casing: SMALL x12 */
    public static Item ITEM_P9_JHP;
    /** casing: SMALL_STEEL x12 */
    public static Item ITEM_P9_AP;

    public static final BulletConfig p9_sp = new BulletConfig("p9_sp").setItem(() -> ITEM_P9_SP);
    public static final BulletConfig p9_fmj = new BulletConfig("p9_fmj").setItem(() -> ITEM_P9_FMJ)
            .setDamage(0.8F).setThresholdNegation(2F).setArmorPiercing(0.1F);
    public static final BulletConfig p9_jhp = new BulletConfig("p9_jhp").setItem(() -> ITEM_P9_JHP)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig p9_ap = new BulletConfig("p9_ap").setItem(() -> ITEM_P9_AP)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(5F)
            .setArmorPiercing(0.15F);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_GREASEGUN =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(2, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_LAG =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_UZI =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(1, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));

    // ==================== guns (4) ====================
    // Static methods, not eager fields - see class javadoc.

    /** CE also applies an "_m3" name mutator gated on a weapon-mod upgrade - skipped, see class javadoc. Default ammo (not yet wired): P9_SP x30. */
    public static ItemGunBaseNT gun_greasegun() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(3F).delay(4).dry(40).auto(true).spread(0.015F).reload(60).jam(55)
                                .sound(HBMSoundHandler.fireGreaseGun.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
                                .offset(1, -0.0625 * 2.5, -0.25D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_GREASEGUN))
                        .setupStandardConfiguration());
    }

    public static ItemGunBaseNT gun_lag() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(1_700).draw(7).inspect(31).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(25F).delay(4).dry(10).spread(0.005F).reload(53).jam(44)
                                .sound(HBMSoundHandler.firePistol.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 17).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
                                .offset(1, -0.0625 * 2.5, -0.25D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_LAG))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): P9_JHP x17
    }

    /** CE also applies a "_richter" name mutator gated on a weapon-mod upgrade - skipped, see class javadoc. Default ammo (not yet wired): P9_SP x30. */
    public static ItemGunBaseNT gun_uzi() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(3F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50)
                                .sound(HBMSoundHandler.fireUzi.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
                                .offset(1, -0.0625 * 2.5, -0.25D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_UZI))
                        .setupStandardConfiguration());
    }

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> UZI_AKIMBO_SECOND_DECIDER = (stack, ctx) -> {
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState lastState = ItemGunBaseNT.getState(stack, index);
        GunStateDecider.deciderStandardFinishDraw(stack, lastState, index);
        GunStateDecider.deciderStandardClearJam(stack, lastState, index);
        GunStateDecider.deciderStandardReload(stack, ctx, lastState, 0, index);
        GunStateDecider.deciderAutoRefire(stack, ctx, lastState, 0, index,
                () -> ItemGunBaseNT.getSecondary(stack, index) && ItemGunBaseNT.getMode(stack, ctx.configIndex) == 0);
    };

    /** Default ammo (not yet wired): P9_SP x60. */
    public static ItemGunBaseNT gun_uzi_akimbo() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
                // config 0 - primary click (left uzi)
                new GunConfig()
                        .dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(3F).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50)
                                .sound(HBMSoundHandler.fireUzi.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
                                .offset(1, -0.0625 * 2.5, 0.375D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_UZI))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
                // config 1 - secondary click (right uzi)
                new GunConfig()
                        .dura(3_000).draw(15).inspect(31).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(3F).spreadHipfire(0F).delay(2).dry(25).auto(true).spread(0.005F).reload(55).jam(50)
                                .sound(HBMSoundHandler.fireUzi.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(1, 30).addConfigs(p9_sp, p9_fmj, p9_jhp, p9_ap))
                                .offset(1, -0.0625 * 2.5, -0.375D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_UZI))
                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(UZI_AKIMBO_SECOND_DECIDER));
    }
}
