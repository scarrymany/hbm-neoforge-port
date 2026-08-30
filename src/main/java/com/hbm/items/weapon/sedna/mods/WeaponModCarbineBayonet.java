package com.hbm.items.weapon.sedna.mods;

import com.hbm.items.weapon.sedna.GunConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.ItemGunBaseNT.LambdaContext;
import com.hbm.weapon.anim.GunAnimationType;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Port of CE's {@code WeaponModCarbineBayonet} (81 lines) - a fixed bayonet for the carbine: a slower
 * secondary-press inspect/stab animation instead of aiming.
 * <p>
 * <b>Dropped branches, documented rather than silent.</b> CE's {@code GunConfig.FUN_ANIMNATIONS} (a
 * bespoke stab animation, {@code BusAnimationSedna}) and {@code GunConfig.CON_ORCHESTRA} (falls back
 * to {@code Orchestras.ORCHESTRA_CARBINE}) both need unported Phase 5 rendering infrastructure - see
 * {@link WeaponModSawedOff}'s class javadoc for the animation half, and
 * {@code com.hbm.items.weapon.sedna.factory.Orchestras} simply does not exist in this port at all
 * (its own report flags it as 1,694 lines of per-gun reload sound-cue timing tables, Phase 5 scope).
 * CE's mid-stab entity/block hit-and-damage raytrace ({@code EntityDamageUtil.getMouseOver}) is also
 * dropped for the same reason that method doesn't exist in this port's {@code EntityDamageUtil}
 * (that class's own javadoc names it as deferred melee/interaction-scope raycasting).
 * <p>
 * The one real, wireable branch - forcing an inspect animation on secondary-press instead of the
 * standard toggle-aim - is kept, inlining the same tiny lambda body
 * {@code XFactory44.HANGMAN_INSPECT_ON_SECONDARY} already uses (CE calls this lambda
 * {@code SMACK_A_FUCKER} and reuses the identical instance across both bayonet mods and
 * {@code gun_hangman}; that field is {@code private} on this port's {@code XFactory44} and this
 * package is scoped separately from the concurrently-edited {@code content} package, so the 6-line
 * body is duplicated here rather than exposing it).
 */
public class WeaponModCarbineBayonet extends WeaponModBase {

    public WeaponModCarbineBayonet(String id) {
        super(id, "BAYONET");
    }

    @Override
    public <T> T eval(T base, ItemStack gun, String key, Object parent) {
        if (Objects.equals(key, GunConfig.I_INSPECTDURATION)) return cast(30, base);
        if (Objects.equals(key, GunConfig.CON_ONPRESSSECONDARY)) return cast(INSPECT_ON_SECONDARY, base);
        if (Objects.equals(key, GunConfig.I_INSPECTCANCEL)) return cast(false, base);
        return base;
    }

    /** Mirrors {@code XFactory44}'s private {@code HANGMAN_INSPECT_ON_SECONDARY} - see class javadoc. */
    public static final BiConsumer<ItemStack, LambdaContext> INSPECT_ON_SECONDARY = (stack, ctx) -> {
        if (ItemGunBaseNT.getState(stack, ctx.configIndex) == ItemGunBaseNT.GunState.IDLE
                || ItemGunBaseNT.getLastAnim(stack, ctx.configIndex) == GunAnimationType.CYCLE.ordinal()) {
            ItemGunBaseNT.setState(stack, ctx.configIndex, ItemGunBaseNT.GunState.DRAWING);
            ItemGunBaseNT.setTimer(stack, ctx.configIndex, ctx.config.getInspectDuration(stack));
            ItemGunBaseNT.playAnimation(ctx.getPlayer(), stack, GunAnimationType.INSPECT, ctx.configIndex);
        }
    };
}
