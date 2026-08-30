package com.hbm.items.armor;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Ported from CE's {@code com.hbm.items.armor.IPAWeaponsProvider} - implemented by power-armor
 * chestplates ({@code ArmorNCRPA}, {@code ArmorRPA}) that supply their own built-in melee/ranged
 * "weapon" component instead of (or alongside) whatever the player is actually holding, gated on
 * the full matching set being worn and powered ({@code ArmorFSB#hasFSBArmorIgnoreCharge}).
 *
 * <p>CE's two {@code static ... ClientProxy} helpers ({@code getMeleeComponentClient}/
 * {@code getRangedComponentClient}, which resolve {@code MainRegistry.proxy.me()} - a client-only
 * "get the local player" accessor) are dropped: this port has not confirmed an equivalent
 * client-side accessor, and every real call site can resolve its own {@link Player} reference
 * directly (e.g. {@code Minecraft.getInstance().player} client-side, or the event's player
 * server-side) and call {@link #getMeleeComponentCommon}/{@link #getRangedComponentCommon}
 * directly - functionally identical, no behavior lost.
 */
public interface IPAWeaponsProvider {

    IPAMelee getMeleeComponent(Player entity);

    static IPAMelee getMeleeComponentCommon(Player player) {
        if (player == null) return null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof IPAWeaponsProvider provider) {
            return provider.getMeleeComponent(player);
        }
        return null;
    }

    IPARanged getRangedComponent(Player entity);

    static IPARanged getRangedComponentCommon(Player player) {
        if (player == null) return null;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!chest.isEmpty() && chest.getItem() instanceof IPAWeaponsProvider provider) {
            return provider.getRangedComponent(player);
        }
        return null;
    }
}
