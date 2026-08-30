package com.hbm.items.weapon.sedna.content;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.item.Item;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.GunFactory} itself - just {@code gun_debug}
 * (DEBUG quality) plus its bespoke, non-{@code ammo_standard} {@code ammo_debug} round. See
 * {@code docs/phase3/guns_and_ammo.md}'s "{@code GunFactory} itself" note.
 * <p>
 * CE's {@code ammo_debug} is a plain {@code ItemBakedBase} (a bare renderable item, no special
 * behavior) sharing the .44's icon (CE: {@code new ItemBakedBase("ammo_debug", "ammo_45")}) - not an
 * {@code EnumAmmo} member. CE's {@code .setCasing(CASING44.clone().register("DEBUG0"))} spawn-effect
 * binding is omitted per {@link BulletConfig}'s own class javadoc (client-rendering particle config,
 * out of this package's scope).
 */
public final class XFactoryDebug {

    private XFactoryDebug() {
    }

    public static final Item ITEM_AMMO_DEBUG = new Item(new Item.Properties());

    public static final BulletConfig ammo_debug = new BulletConfig("ammo_debug").setItem(ITEM_AMMO_DEBUG)
            .setSpread(0.01F).setRicochetAngle(45);

    public static ItemGunBaseNT gun_debug() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.DEBUG,
                new GunConfig()
                        .dura(600F).draw(15).inspect(23).crosshair(Crosshair.L_CLASSIC)
                        .rec(new Receiver(0)
                                .dmg(10F).delay(14).reload(46).jam(23).sound(HBMSoundHandler.shoot44.get(), 1.0F, 1.0F)
                                .mag(new MagazineFullReload(0, 12).addConfigs(ammo_debug))
                                .offset(0.75, -0.0625, -0.3125D)
                                .canFire(Lego.LAMBDA_STANDARD_CAN_FIRE).fire(Lego.LAMBDA_STANDARD_FIRE))
                        .setupStandardConfiguration());
    }
}
