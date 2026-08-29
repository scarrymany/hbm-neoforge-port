package com.hbm.items;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IEquipReceiver {

    default void onEquip(Player player, InteractionHand hand) {}
    default void onEquip(Player player, ItemStack stack) {}
}
