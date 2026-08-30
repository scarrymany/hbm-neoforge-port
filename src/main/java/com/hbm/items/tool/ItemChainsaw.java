package com.hbm.items.tool;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.packet.toclient.GunAnimationPayload;
import com.hbm.weapon.anim.ToolAnimationType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

/**
 * Concrete axe-type fueled tool. Ported from CE's {@code com.hbm.items.tool.ItemChainsaw}.
 *
 * <p>CE's chainsaw additionally implements {@code IAnimatedItem} to drive a custom bone/bus swing
 * animation ({@code BusAnimation}/{@code HbmAnimations}) via a server-side {@code onEntitySwing}
 * trigger into {@code HbmEffectNT.Anim} (CE {@code ItemChainsaw.java:30-42}). This port's Phase 3
 * weapons package substitutes the already-built {@link ToolAnimationType}/
 * {@link GunAnimationPayload} wire path for that trigger (see
 * {@code docs/phase3/weapon_animation_hooks.md}'s explicit recommendation, since CE's
 * {@code HbmEffectNT} generic effect-dispatch table is not part of this port) - the *trigger* is
 * therefore real; only the client-side per-slot animation-state array and renderer that would
 * actually sample {@link ToolAnimationType#SWING} back out are Phase 5 scope (same as the gun
 * framework's own {@link com.hbm.weapon.anim.GunAnimationType}), so today this still visibly swings
 * with the plain vanilla item animation - the wire trigger fires correctly, nothing consumes it yet.
 */
public class ItemChainsaw extends ItemToolAbilityFueled {

    public ItemChainsaw(Properties properties, Tier tier, int maxFuel, int consumption, int fillRate, FluidType... acceptedFuels) {
        super(properties, tier, ToolRole.AXE, maxFuel, consumption, fillRate, acceptedFuels);
    }

    /**
     * CE: {@code onEntitySwing} (server-side only, guarded by {@code EntityPlayerMP} and "not
     * already out of fuel"). Assumed real 1.21.1 {@code IItemExtension} hook shape
     * ({@code onEntitySwing(ItemStack, LivingEntity)}, stack first) - see
     * {@code com.hbm.items.weapon.ItemSwordCutter#onEntitySwing}'s javadoc for the same
     * not-independently-verified-against-a-real-jar caveat (no cached NeoForge dependency in this
     * sandbox to check against).
     */
    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        if (entity instanceof ServerPlayer serverPlayer && canOperate(stack)) {
            GunAnimationPayload.triggerGunAnimation(serverPlayer, stack, InteractionHand.MAIN_HAND, ToolAnimationType.SWING, t -> true);
        }
        return false;
    }
}
