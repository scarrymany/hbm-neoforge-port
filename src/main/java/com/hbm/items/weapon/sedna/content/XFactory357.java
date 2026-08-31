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
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory357} - the .357 ammo family (6
 * {@link BulletConfig}s, {@code SMALL}/no-steel-AP-tier casing) and its 3 revolvers
 * ({@code gun_light_revolver}/{@code _atlas}/{@code _dani}). See
 * {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory357} table.
 * <p>
 * Ammo {@link Item}s/{@link BulletConfig}s are plain eager {@code static final} fields (see
 * {@code XFactory556mm}'s class javadoc for why that's safe) and {@code .setCasing(...)} is omitted
 * (see the same javadoc - no shared casing-item family exists yet). <b>Every gun below is a static
 * method, not an eager field</b> - see {@link XFactoryBlackPowder}'s class javadoc for the full
 * rationale (defers every {@code HBMSoundHandler.xxx.get()} call past mod-construction time, past
 * the point where it would throw); {@link GunPistolItems} calls each method from inside the
 * {@code Supplier} it hands {@code ModItems.ITEMS.register(...)}.
 * <p>
 * {@code gun_light_revolver_dani} is CE's akimbo legendary variant: two independent
 * {@link GunConfig}s sharing one {@link ItemGunBaseNT}, mag indices 0/1, primary press wired to
 * config 0 ({@code pp}) and secondary press wired to config 1 ({@code ps}) - matches CE's own
 * {@code XFactory357.java} exactly, including that both configs use the plain
 * {@link GunStateDecider#LAMBDA_STANDARD_DECIDER} (harmless here since neither receiver is
 * {@code auto(true)}, so the decider's refire-condition predicate is never actually evaluated).
 */
public final class XFactory357 {

    private XFactory357() {
    }

    // ==================== ammo (6) ====================
    // .setCasing(...) intentionally omitted - see XFactory556mm's class javadoc (no shared casing-item
    // family registered anywhere in this port yet). CE's casing type + count preserved per field below.

    /** casing: SMALL x16 */
    public static Item ITEM_M357_BP;
    /** casing: SMALL x8 */
    public static Item ITEM_M357_SP;
    /** casing: SMALL x8 */
    public static Item ITEM_M357_FMJ;
    /** casing: SMALL x8 */
    public static Item ITEM_M357_JHP;
    /** casing: SMALL_STEEL x8 */
    public static Item ITEM_M357_AP;
    /** casing: SMALL x8 */
    public static Item ITEM_M357_EXPRESS;

    public static final BulletConfig m357_bp = new BulletConfig("m357_bp").setItem(() -> ITEM_M357_BP)
            .setDamage(0.75F).setBlackPowder(true);
    public static final BulletConfig m357_sp = new BulletConfig("m357_sp").setItem(() -> ITEM_M357_SP);
    public static final BulletConfig m357_fmj = new BulletConfig("m357_fmj").setItem(() -> ITEM_M357_FMJ)
            .setDamage(0.8F).setThresholdNegation(2F).setArmorPiercing(0.1F);
    public static final BulletConfig m357_jhp = new BulletConfig("m357_jhp").setItem(() -> ITEM_M357_JHP)
            .setDamage(1.5F).setHeadshot(1.5F).setArmorPiercing(-0.25F);
    public static final BulletConfig m357_ap = new BulletConfig("m357_ap").setItem(() -> ITEM_M357_AP)
            .setDoesPenetrate(true).setDamageFalloffByPen(false).setDamage(1.5F).setThresholdNegation(5F)
            .setArmorPiercing(0.15F);
    public static final BulletConfig m357_express = new BulletConfig("m357_express").setItem(() -> ITEM_M357_EXPRESS)
            .setDoesPenetrate(true).setDamage(1.5F).setThresholdNegation(2F).setArmorPiercing(0.1F).setWear(1.5F);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_ATLAS =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(10, (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));
    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_DANI =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil(5, (float) (ctx.getPlayer().getRandom().nextGaussian() * 0.75));

    // ==================== guns (3) ====================
    // Static methods, not eager fields - see class javadoc.

    public static ItemGunBaseNT gun_light_revolver() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.A_SIDE,
                new GunConfig()
                        .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(7.5F).delay(16).reload(55).jam(45).sound(HBMSoundHandler.firePistol.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                                .offset(0.75, -0.0625, -0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_ATLAS))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): M357_SP x12
    }

    public static ItemGunBaseNT gun_light_revolver_atlas() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.B_SIDE,
                new GunConfig()
                        .dura(300).draw(4).inspect(23).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(12.5F).delay(16).reload(55).jam(45).sound(HBMSoundHandler.firePistol.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                                .offset(0.75, -0.0625, -0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_ATLAS))
                        .setupStandardConfiguration());
        // default ammo (not yet wired): M357_JHP x12
    }

    public static ItemGunBaseNT gun_light_revolver_dani() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.LEGENDARY,
                // config 0 - primary click (left revolver)
                new GunConfig()
                        .dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(15F).spreadHipfire(0F).delay(11).reload(55).jam(45).sound(HBMSoundHandler.firePistol.get(), 1.0F, 1.1F)
                                .mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                                .offset(0.75, -0.0625, 0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_DANI))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
                // config 1 - secondary click (right revolver)
                new GunConfig()
                        .dura(30_000).draw(20).inspect(23).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(15F).spreadHipfire(0F).delay(11).reload(55).jam(45).sound(HBMSoundHandler.firePistol.get(), 1.0F, 0.9F)
                                .mag(new MagazineFullReload(1, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap, m357_express))
                                .offset(0.75, -0.0625, -0.3125D)
                                .setupStandardFire().recoil(LAMBDA_RECOIL_DANI))
                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
        // default ammo (not yet wired): M357_EXPRESS x24
    }
}
