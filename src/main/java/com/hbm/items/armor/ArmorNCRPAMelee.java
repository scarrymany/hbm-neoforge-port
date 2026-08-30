package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Stub port of CE's {@code com.hbm.items.armor.ArmorNCRPAMelee} - the NCR power-armor set's
 * built-in "swing the whole arm" melee weapon component, returned by {@link ArmorNCRPA}'s
 * {@link IPAWeaponsProvider#getMeleeComponent}.
 *
 * <p><b>Deliberately not ported</b> per this package's task brief item 3 ("stub the actual
 * melee/ranged component implementations with a documented TODO if they need the not-yet-ported
 * gun_state_machine package"): CE's real body ({@code clickPrimary}/{@code clickSecondary}/
 * {@code orchestra}) drives a raycast-and-damage swing sequence timed off
 * {@code ItemGunBaseNT.getLastAnim}/{@code getAnimTimer} (the gun state machine's per-tick timer
 * bookkeeping) and calls into {@code com.hbm.items.weapon.sedna.factory.XFactoryPA}/
 * {@code ConfettiUtil} - a swing-choreography helper library not confirmed to exist anywhere in
 * this port (distinct from the confirmed-landed ballistics core named in this package's task
 * brief: {@code BulletConfig}/{@code EntityBulletBaseMK4}/{@code EntityThrowableNT}). Wiring this
 * for real needs that gun-state-machine timer plumbing to land first; until then both hooks are
 * documented no-ops rather than a guessed reimplementation.
 */
public class ArmorNCRPAMelee implements IPAMelee {

    @Override
    public void clickPrimary(ItemStack stack, Player player) {
        // TODO(gun_state_machine): CE's XFactoryPA.doSwing(stack, ctx, GunAnimation.CYCLE, 25) +
        // orchestra()'s timed raycast-and-damage swing. See class javadoc.
    }

    @Override
    public void clickSecondary(ItemStack stack, Player player) {
        // TODO(gun_state_machine): CE's XFactoryPA.doSwing(stack, ctx, GunAnimation.ALT_CYCLE, 30)
        // heavy-sweep variant. See class javadoc.
    }
}
