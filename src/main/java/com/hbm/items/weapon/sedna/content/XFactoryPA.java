package com.hbm.items.weapon.sedna.content;

import com.hbm.items.armor.IPAMelee;
import com.hbm.items.armor.IPARanged;
import com.hbm.items.armor.IPAWeaponsProvider;
import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.WeaponQuality;
import com.hbm.items.weapon.sedna.Receiver;
import com.hbm.items.weapon.sedna.factory.GunStateDecider;
import com.hbm.render.misc.RenderScreenOverlay.Crosshair;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiConsumer;

/**
 * Exact CE {@code XFactoryPA}: {@code gun_pa_melee}/{@code gun_pa_ranged} dispatch to the worn
 * {@link IPAWeaponsProvider} component. Melee orchestra + {@link #doSwing} are Exact CE
 * ({@code XFactoryPA.java:43-74}). Ranged still Player-stub — {@code rocket_ncrpa*} unregistered.
 * {@code LAMBDA_MELEE_ANIMS} / first-person stay skipped.
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
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER)
                        .orchestra(ORCHESTRA));
    }

    public static ItemGunBaseNT gun_pa_ranged() {
        return new ItemGunBaseNT(new Item.Properties(), WeaponQuality.UTILITY,
                new GunConfig()
                        .draw(0).crosshair(Crosshair.CROSS)
                        .rec(new Receiver(0))
                        .pp(LAMBDA_CLICK_RANGED_PRIMARY).ps(LAMBDA_CLICK_RANGED_SECONDARY)
                        .decider(GunStateDecider.LAMBDA_STANDARD_DECIDER));
    }

    public static final BiConsumer<ItemStack, LambdaContext> ORCHESTRA = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.orchestra(stack, ctx);
    };

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_PRIMARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx);
    };
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_MELEE_SECONDARY = (stack, ctx) -> {
        IPAMelee component = IPAWeaponsProvider.getMeleeComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx);
    };

    /** Exact CE {@code XFactoryPA.doSwing} ({@code XFactoryPA.java:63-74}). */
    public static void doSwing(ItemStack stack, LambdaContext ctx, GunAnimationType anim, int cooldown) {
        Player player = ctx.getPlayer();
        int index = ctx.configIndex;
        ItemGunBaseNT.GunState state = ItemGunBaseNT.getState(stack, index);

        if (state == ItemGunBaseNT.GunState.IDLE) {
            ItemGunBaseNT.playAnimation(player, stack, anim, ctx.configIndex);
            ItemGunBaseNT.setState(stack, index, ItemGunBaseNT.GunState.COOLDOWN);
            ItemGunBaseNT.setTimer(stack, index, cooldown);
        }
    }

    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_PRIMARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickPrimary(stack, ctx.getPlayer());
    };
    public static final BiConsumer<ItemStack, LambdaContext> LAMBDA_CLICK_RANGED_SECONDARY = (stack, ctx) -> {
        IPARanged component = IPAWeaponsProvider.getRangedComponentCommon(ctx.getPlayer());
        if (component != null) component.clickSecondary(stack, ctx.getPlayer());
    };
}
