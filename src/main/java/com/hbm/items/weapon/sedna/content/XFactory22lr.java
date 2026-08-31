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
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory22lr} - the .22LR ammo family (4
 * {@link BulletConfig}s, {@code SMALL}/{@code SMALL_STEEL} casing, zero knockback on every round)
 * and its 3 guns ({@code gun_am180}/{@code gun_star_f}/{@code gun_star_f_akimbo}). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory22lr} table.
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
 * brief): {@code gun_am180}/{@code gun_star_f}'s shared {@code LAMBDA_NAME_AM180} "_silenced" name
 * mutator.
 * <p>
 * {@code gun_star_f_akimbo} is CE's akimbo variant: two independent {@link GunConfig}s sharing one
 * {@link ItemGunBaseNT}, mag indices 0/1, primary press wired to config 0 ({@code pp}) and secondary
 * press wired to config 1 ({@code ps}) - both configs use the plain
 * {@link GunStateDecider#LAMBDA_STANDARD_DECIDER} (matches CE exactly; harmless since neither
 * receiver is {@code auto(true)}).
 */
public final class XFactory22lr {

    private XFactory22lr() {
    }

    // ==================== ammo (4) ====================
    // .setCasing(...) intentionally omitted - see XFactory556mm's class javadoc (no shared casing-item
    // family registered anywhere in this port yet). CE's casing type + count preserved per field below.

    /** casing: SMALL x24 */
    public static Item ITEM_P22_SP;
    /** casing: SMALL x24 */
    public static Item ITEM_P22_FMJ;
    /** casing: SMALL x24 */
    public static Item ITEM_P22_JHP;
    /** casing: SMALL_STEEL x24 */
    public static Item ITEM_P22_AP;

    public static final BulletConfig p22_sp = new BulletConfig("p22_sp").setItem(() -> ITEM_P22_SP).setKnockback(0F);
    public static final BulletConfig p22_fmj = new BulletConfig("p22_fmj").setItem(() -> ITEM_P22_FMJ).setKnockback(0F)
            .setDamage(0.8F).setThresholdNegation(1F).setArmorPiercing(0.1F);
    public static final BulletConfig p22_jhp = new BulletConfig("p22_jhp").setItem(() -> ITEM_P22_JHP).setKnockback(0F)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig p22_ap = new BulletConfig("p22_ap").setItem(() -> ITEM_P22_AP).setKnockback(0F)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(2.5F)
            .setArmorPiercing(0.15F);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_AM180 =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25), (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.25));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_STAR_F =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(2.5F, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.5));

    // ==================== guns (3) ====================
    // Static methods, not eager fields - see class javadoc.

    /** CE also applies a "_silenced" name mutator gated on a weapon-mod upgrade - skipped, see class javadoc. Default ammo (not yet wired): P22_SP x35. */
    public static ItemGunBaseNT gun_am180() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(177 * 25).draw(15).inspect(38).crosshair(Crosshair.L_CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(2F).delay(1).dry(10).auto(true).spread(0.02F).reload(66).jam(30)
                                .sound(HBMSoundHandler.fireGreaseGun.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 177).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
                                .offset(1, -0.0625 * 1.5, -0.1875D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_AM180))
                        .setupStandardConfiguration());
    }

    /** CE also applies a "_silenced" name mutator gated on a weapon-mod upgrade - skipped, see class javadoc. Default ammo (not yet wired): P22_SP x15. */
    public static ItemGunBaseNT gun_star_f() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(12.5F).delay(5).dry(17).spread(0.01F).reload(40).jam(32)
                                .sound(HBMSoundHandler.firePistolLight.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 15).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
                                .offset(1, -0.0625 * 1.5, -0.1875D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_STAR_F))
                        .setupStandardConfiguration());
    }

    /** Default ammo (not yet wired): P22_SP x30. */
    public static ItemGunBaseNT gun_star_f_akimbo() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
                // config 0 - primary click (left pistol)
                new GunConfig()
                        .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(12.5F).delay(5).dry(17).spread(0.01F).reload(40).jam(32)
                                .sound(HBMSoundHandler.firePistolLight.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 15).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
                                .offset(1, -0.0625 * 1.5, 0.25D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_STAR_F))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
                // config 1 - secondary click (right pistol)
                new GunConfig()
                        .dura(15 * 25).draw(15).inspect(38).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(12.5F).delay(5).dry(17).spread(0.01F).reload(40).jam(32)
                                .sound(HBMSoundHandler.firePistolLight.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(1, 15).addConfigs(p22_sp, p22_fmj, p22_jhp, p22_ap))
                                .offset(1, -0.0625 * 1.5, -0.25D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_STAR_F))
                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }
}
