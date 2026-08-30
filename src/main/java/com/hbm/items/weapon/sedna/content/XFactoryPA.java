package com.hbm.items.weapon.sedna.content;

import com.hbm.items.armor.IPAMelee;
import com.hbm.items.armor.IPARanged;
import com.hbm.items.armor.IPAWeaponsProvider;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import net.minecraft.world.item.Item;

/**
 * Port of CE's {@code com.hbm.items.weapon.sedna.factory.XFactoryPA} - {@code gun_pa_melee}/
 * {@code gun_pa_ranged}, pure dispatch shells with no intrinsic {@link Receiver}/damage/ammo of
 * their own: every click just forwards to whatever {@link IPAMelee}/{@link IPARanged} component the
 * worn powered-armor chestplate supplies via {@link IPAWeaponsProvider} (already-ported interfaces
 * per this task's brief). See {@code docs/phase3/guns_and_ammo.md}'s {@code XFactoryPA} note.
 * <p>
 * CE's own {@code Receiver}s for both guns carry no {@code .dmg}/{@code .mag}/{@code .sound} calls at
 * all (confirmed by reading {@code XFactoryPA.java} in full) - the bare {@code new Receiver(0)} below
 * matches that exactly; neither gun ever calls {@code getCanFire}/{@code getOnFire} since their
 * {@code pp}/{@code ps} slots go straight to the melee/ranged dispatch lambdas below, never through
 * {@code Lego.clickReceiver}.
 */
public final class XFactoryPA {

    private XFactoryPA() {
    }

    public static ItemGunBaseNT gun_pa_melee() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .draw(10).crosshair(Crosshair.NONE)
                        .rec(new Receiver(0))
                        .pp(LAMBDA_CLICK_MELEE_PRIMARY).ps(LAMBDA_CLICK_MELEE_SECONDARY)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }

    public static ItemGunBaseNT gun_pa_ranged() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .draw(0).crosshair(Crosshair.CROSS)
                        .rec(new Receiver(0))
                        .pp(LAMBDA_CLICK_RANGED_PRIMARY).ps(LAMBDA_CLICK_RANGED_SECONDARY)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }

    private static final java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx.getPlayer());
    };
    private static final java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_CLICK_MELEE_SECONDARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx.getPlayer());
    };
    private static final java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx.getPlayer());
    };
    private static final java.util.function.BiConsumer<net.minecraft.world.item.ItemStack, ItemGunBaseNT.LambdaContext> LAMBDA_CLICK_RANGED_SECONDARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx.getPlayer());
    };
}
