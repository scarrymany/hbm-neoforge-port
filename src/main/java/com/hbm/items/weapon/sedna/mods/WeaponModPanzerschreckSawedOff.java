package com.hbm.items.weapon.sedna.mods;

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
 * {@link WeaponModSawedOff}'s class javadoc for why. CE's fire-application also increments
 * {@code HbmLivingCapability}'s custom {@code fire} burn-timer field (a CE-specific hazard mechanic,
 * distinct from vanilla fire ticks) via {@code props.setFire(props.getFire() + 100)} - that
 * capability field does not exist on this port's {@code HbmLivingAttachment}/{@code HbmLivingProps}
 * (neither ports CE's {@code fire} field - see those classes' own javadocs, which cover CE's other
 * hazard fields but not this one). The real, load-bearing effect - the instant {@code DamageClass.FIRE}
 * hit - is still applied; only the follow-up burn-timer increment is stubbed pending that missing
 * capability field.
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
            // TODO(capability): CE also does props.setFire(props.getFire() + 100) here - see class javadoc.
            EntityDamageUtil.attackEntityFromNT(ctx.entity, BulletConfig.getDamage(ctx.entity, ctx.entity, DamageClass.FIRE), 4F, true, false, 0F, 0F, 0F);
        }
    };
}
