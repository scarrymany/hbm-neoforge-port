package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Stub port of CE's {@code com.hbm.items.armor.ArmorNCRPARanged} - the NCR power-armor set's
 * built-in shoulder-mounted rocket launcher, returned by {@link ArmorNCRPA}'s
 * {@link IPAWeaponsProvider#getRangedComponent}.
 *
 * <p><b>Deliberately not ported</b>, same blocker as {@link ArmorNCRPAMelee} (see its javadoc):
 * CE's real {@code fireRocket} needs {@code com.hbm.items.weapon.sedna.mags.MagazineBelt}
 * (ammo-belt bookkeeping) and {@code com.hbm.items.weapon.sedna.factory.XFactoryRocket} (the
 * NCRPA-specific rocket {@code BulletConfig} presets), neither confirmed to exist in this port.
 * Once landed, this should spawn an {@code EntityBulletBaseMK4} (already-ported, per this
 * package's task brief) exactly as CE's body does - the blocker is the magazine/ammo-preset
 * plumbing feeding it, not the projectile entity itself.
 */
public class ArmorNCRPARanged implements IPARanged {

    @Override
    public void clickPrimary(ItemStack stack, Player player) {
        // TODO(gun_state_machine): CE's fireRocket(stack, ctx, true) - steerable rocket. See class javadoc.
    }

    @Override
    public void clickSecondary(ItemStack stack, Player player) {
        // TODO(gun_state_machine): CE's fireRocket(stack, ctx, false) - dumbfire rocket. See class javadoc.
    }
}
