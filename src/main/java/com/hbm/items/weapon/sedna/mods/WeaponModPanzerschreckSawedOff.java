package com.hbm.items.weapon.sedna.mods;

import com.hbm.capability.HbmLivingAttachment;
import com.hbm.capability.ModAttachments;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.Lego;
import com.hbm.util.DamageResistanceHandler.DamageClass;
import com.hbm.util.EntityDamageUtil;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Port of CE's {@code WeaponModPanzerschreckSawedOff} (51 lines) - removes the panzerschreck's blast
 * shield: faster draw, but firing burns the shooter.
 * <p>
 * CE's {@code GunConfig.FUN_ANIMNATIONS} branch (a shieldless-draw animation) is dropped - see
 * {@link WeaponModSawedOff}'s class javadoc for why. CE's {@code props.setFire(props.getFire() + 100)}
 * burn-timer increment IS wired below, via {@link HbmLivingAttachment#getFire()}/{@code setFire(int)}
 * (that field does exist on this port's attachment - see {@code XFactory40mm}'s {@code LAMBDA_STANDARD_IGNITE}
 * for the identical direct-attachment-access precedent), re-synced via {@code entity.setData(...)} per
 * that class's documented mutation contract.
 */
public class WeaponModPanzerschreckSawedOff extends WeaponModBase {

    public WeaponModPanzerschreckSawedOff(String id) {
        super(id, "SHIELD");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, GunConfig.I_DRAWDURATION)) return cast(5, base);
        if (Objects.equals(key, Receiver.CON_ONFIRE)) return cast(LAMBDA_FIRE, base);
        return base;
    }

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_FIRE = (stack, ctx) -> {
        Lego.LAMBDA_STANDARD_FIRE.accept(stack, ctx);
        if (ctx.entity != null) {
            HbmLivingAttachment data = HbmLivingAttachment.getData(ctx.entity);
            data.setFire(data.getFire() + 100);
            ctx.entity.setData(ModAttachments.LIVING_ATTACHMENT, data);
            EntityDamageUtil.attackEntityFromNT(ctx.entity, BulletConfig.getDamage(ctx.entity, ctx.entity, DamageClass.FIRE), 4F, true, false, 0F, 0F, 0F);
        }
    };
}
