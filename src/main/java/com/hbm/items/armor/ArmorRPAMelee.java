package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Stub port of CE's {@code com.hbm.items.armor.ArmorRPAMelee} - the Remnant power-armor set's
 * built-in melee weapon component, returned by {@link ArmorRPA}'s
 * {@link IPAWeaponsProvider#getMeleeComponent}. Same shape and same blocker as
 * {@link ArmorNCRPAMelee} (see its javadoc) - CE's real body is a raycast-and-damage swing
 * sequence driven by the same not-yet-confirmed {@code XFactoryPA}/{@code ConfettiUtil} helpers.
 */
public class ArmorRPAMelee implements IPAMelee {

    @Override
    public void clickPrimary(ItemStack stack, Player player) {
        // TODO(gun_state_machine): CE's XFactoryPA.doSwing(stack, ctx, GunAnimation.CYCLE, 14) +
        // orchestra()'s continuous-refire timed swing. See ArmorNCRPAMelee's javadoc for the blocker.
    }

    @Override
    public void clickSecondary(ItemStack stack, Player player) {
        // TODO(gun_state_machine): CE's XFactoryPA.doSwing(stack, ctx, GunAnimation.ALT_CYCLE, 20)
        // heavy-slap variant. See ArmorNCRPAMelee's javadoc for the blocker.
    }
}
