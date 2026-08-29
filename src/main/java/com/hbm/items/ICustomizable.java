package com.hbm.items;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ICustomizable {

    void customize(Player player, ItemStack stack, String... args);
}
