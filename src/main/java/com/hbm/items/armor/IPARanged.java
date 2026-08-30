package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from CE's {@code com.hbm.items.armor.IPARanged} - the "power armor supplies a built-in
 * ranged weapon" contract {@link IPAWeaponsProvider#getRangedComponent} hands back while a matching
 * powered-armor set is worn (CE: {@code ArmorNCRPARanged}, a shoulder-mounted rocket launcher).
 * Ported verbatim (CE's own interface was already this small - just the two click hooks, no
 * rendering methods to simplify away).
 */
public interface IPARanged {

    /** CE: {@code IPARanged#clickPrimary(ItemStack, ItemGunBaseNT.LambdaContext)}. */
    void clickPrimary(ItemStack stack, Player player);

    /** CE: {@code IPARanged#clickSecondary(ItemStack, ItemGunBaseNT.LambdaContext)}. */
    void clickSecondary(ItemStack stack, Player player);
}
