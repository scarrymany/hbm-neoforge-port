package com.hbm.items.weapon.sedna.content;

import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.mags.MagazineFullReload;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactory75Bolt} - the 7.5mm bolt-action ammo
 * family (3 {@link BulletConfig}s, no casing item registered in CE at all - confirmed by reading the
 * source, this caliber genuinely has no {@code .setCasing(...)} call on any of its 3 configs, unlike
 * every other family in this batch which merely lacks a *port-side* casing item) and its single gun,
 * {@code gun_bolter}. See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactory75Bolt} table;
 * cross-checked against a full read of CE's real {@code XFactory75Bolt.java} (63 lines, read in full).
 * <p>
 * See {@link XFactory556mm}'s class javadoc for why every ammo/{@code BulletConfig} field here is a
 * plain eager {@code static final} and why {@code .smoke(...)}/{@code .anim(...)}/
 * {@code .orchestra(...)}/{@code setDefaultAmmo(...)} are omitted, and why the gun below is a static
 * METHOD rather than a field (deferring the {@code Receiver.sound(...).get()} SoundEvent
 * {@code DeferredHolder} resolution until {@code RegisterEvent(ITEM)} time, via
 * {@link GunLauncherItems}'s method-reference {@code Supplier}).
 * <p>
 * <b>{@code b75_inc}/{@code b75_exp} have no incendiary/explosive on-hit effect whatsoever</b>, despite
 * their names - confirmed by reading the source directly rather than trusting the naming convention
 * every other caliber family in this roster follows (e.g. {@code r762_he} really does explode,
 * {@code m357_express} really does penetrate). CE's real {@code b75_inc}/{@code b75_exp} are plain
 * damage/armor-piercing multiplier tweaks with no {@code .setOnImpact(...)}/{@code .setBlackPowder(...)}
 * call at all - ported exactly as-is, not "fixed" to match their names.
 */
public final class XFactory75Bolt {

    private XFactory75Bolt() {
    }

    // ==================== ammo (3) ====================
    // No .setCasing(...) call exists on any of these 3 configs in CE's real source either - this is
    // not this port's usual "casing item family not registered yet" omission, it's CE's own genuine
    // behavior for this caliber (see class javadoc).

    public static final Item ITEM_B75 = new Item(new Item.Properties());
    public static final Item ITEM_B75_INC = new Item(new Item.Properties());
    public static final Item ITEM_B75_EXP = new Item(new Item.Properties());

    public static final BulletConfig b75 = new BulletConfig("b75").setItem(ITEM_B75);
    public static final BulletConfig b75_inc = new BulletConfig("b75_inc").setItem(ITEM_B75_INC)
            .setDamage(0.8F).setArmorPiercing(0.1F);
    public static final BulletConfig b75_exp = new BulletConfig("b75_exp").setItem(ITEM_B75_EXP)
            .setDamage(1.5F).setArmorPiercing(-0.25F);

    // ==================== recoil (see XFactory556mm's javadoc - not currently wired anywhere, kept for 1:1 parity) ====================

    private static final BiConsumer<ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_RECOIL_BOLT =
            (stack, ctx) -> ItemGunBaseNT.setupRecoil((float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5), (float) (ctx.getPlayer().getRandom().nextGaussian() * 1.5));

    // ==================== gun (1) ====================

    public static ItemGunBaseNT gun_bolter() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.SPECIAL,
            new GunConfig()
                    .dura(3_000).draw(20).inspect(31).crosshair(Crosshair.L_CIRCLE)
                    .rec(new Receiver(0)
                            .dmg(15F).delay(2).auto(true).spread(0.005F).reload(40).jam(55).sound(HBMSoundHandler.fireBlackPowder.get(), 1.0F, 1.0F)
                            .mag(new MagazineFullReload(0, 30).addConfigs(b75, b75_inc, b75_exp))
                            .offset(1, -0.0625 * 2.5, -0.25D)
                            .setupStandardFire().recoil(LAMBDA_RECOIL_BOLT))
                    .setupStandardConfiguration()
            // default ammo (not yet wired): B75 x15
        );
    }
}
