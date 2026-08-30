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

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory35800} - {@code gun_aberrator} +
 * its akimbo legendary variant {@code gun_aberrator_eott} (SECRET-tier ultra-high-damage beam
 * sidearms). See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory35800} table.
 * <p>
 * {@code p35800_bl}'s "apply black fire" branch is dropped for a documented reason that is not this
 * port's own gap: CE's own {@code LAMBDA_BLACK_IMPACT} has that call commented out in upstream
 * source ({@code // props.setBlackFire(...)}) - it is dead code in CE 1.12.2 itself, not a casualty
 * of this port. The lingering-ground-fire half of the same lambda ({@code EntityFireLingering}) is a
 * genuine port gap - see {@code XFactoryEnergy}'s class javadoc for the same confirmed-missing
 * dependency. {@code p35800}/{@code p35800_bl}'s {@code SpentCasing} spawn-effect binding is omitted
 * per {@code BulletConfig}'s own class javadoc (pure client-rendering particle config, Phase 5 scope).
 * <p>
 * {@code gun_aberrator_eott}'s dual-{@code GunConfig} akimbo wiring (primary click -> mag index 0,
 * secondary click -> mag index 1, both sharing one {@link ItemGunBaseNT}) matches
 * {@code XFactory357.gun_light_revolver_dani()}'s already-landed pattern exactly.
 */
public final class XFactory35800 {

    private XFactory35800() {
    }

    // ==================== ammo ====================

    public static final Item ITEM_P35800 = new Item(new Item.Properties());
    public static final Item ITEM_P35800_BL = new Item(new Item.Properties());

    public static final BulletConfig p35800 = new BulletConfig("p35800").setItem(ITEM_P35800)
            .setArmorPiercing(0.5F).setThresholdNegation(50F).setBeam().setSpread(0F).setLife(3).setRenderRotations(false)
            .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);
    public static final BulletConfig p35800_bl = new BulletConfig("p35800_bl").setItem(ITEM_P35800_BL)
            .setArmorPiercing(0.5F).setThresholdNegation(50F).setBeam().setSpread(0F).setLife(3).setRenderRotations(false)
            .setOnBeamImpact(BulletConfig.LAMBDA_STANDARD_BEAM_HIT);

    // ==================== guns ====================

    public static ItemGunBaseNT gun_aberrator() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SECRET,
                new GunConfig()
                        .dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(100F).delay(13).dry(21).reload(51).sound(HBMSoundHandler.fireAberrator.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 5).addConfigs(p35800))
                                .offset(0.75, -0.09375, -0.1875)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE))
                        .setupStandardConfiguration());
    }

    public static ItemGunBaseNT gun_aberrator_eott() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SECRET,
                new GunConfig()
                        .dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(100F).spreadHipfire(0F).delay(13).dry(21).reload(51).sound(HBMSoundHandler.fireAberrator.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 5).addConfigs(p35800))
                                .offset(0.75, -0.09375, 0.1875)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE))
                        .pp(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER),
                new GunConfig()
                        .dura(2_000).draw(10).inspect(26).crosshair(Crosshair.CIRCLE)
                        .rec(new Receiver(0)
                                .dmg(100F).spreadHipfire(0F).delay(13).dry(21).reload(51).sound(HBMSoundHandler.fireAberrator.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(1, 5).addConfigs(p35800))
                                .offset(0.75, -0.09375, -0.1875)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_NOWEAR_FIRE))
                        .ps(Lego.LAMBDA_STANDARD_CLICK_PRIMARY).pr(Lego.LAMBDA_STANDARD_RELOAD)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }
}
