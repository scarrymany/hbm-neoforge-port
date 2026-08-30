package com.hbm.items.armor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from CE's {@code com.hbm.items.armor.IPAMelee} - the "power armor supplies a built-in
 * melee weapon" contract {@link IPAWeaponsProvider#getMeleeComponent} hands back while a matching
 * powered-armor set is worn (CE: {@code ArmorNCRPAMelee}/{@code ArmorRPAMelee}).
 *
 * <p><b>Simplified relative to CE</b> (documented, not silently dropped): CE's real interface also
 * carries {@code setupFirstPerson}/{@code renderFirstPerson}/{@code playAnim}/{@code orchestra}, all
 * of which exist purely to drive CE's own 1.12 {@code BusAnimationSedna} GL-immediate-mode
 * first-person weapon-arm rendering - a Phase 5 (Client &amp; UX) concern per
 * {@code docs/phase3/armor_equippable_framework.md}'s Deferred scope ("All client-side rendering").
 * Only the two server-authoritative click hooks are ported now; a Phase 5 pass can widen this
 * interface with a confirmed 1.21.1 first-person-rendering hook once one exists, without touching
 * the click contract below.
 */
public interface IPAMelee {

    /** CE: {@code IPAMelee#clickPrimary(ItemStack, ItemGunBaseNT.LambdaContext)}. */
    void clickPrimary(ItemStack stack, Player player);

    /** CE: {@code IPAMelee#clickSecondary(ItemStack, ItemGunBaseNT.LambdaContext)}. */
    void clickSecondary(ItemStack stack, Player player);
}
