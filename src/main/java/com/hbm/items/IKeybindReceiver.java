package com.hbm.items;

import com.hbm.handler.HbmKeybinds;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Depends on com.hbm.handler.HbmKeybinds.EnumKeybind, out of this agent's scope; that class does
 * not exist in this tree yet.
 */
public interface IKeybindReceiver {

    boolean canHandleKeybind(Player player, ItemStack stack, HbmKeybinds.EnumKeybind keybind);
    void handleKeybind(Player player, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean state);

    @OnlyIn(Dist.CLIENT)
    default void handleKeybindClient(LocalPlayer player, ItemStack stack, HbmKeybinds.EnumKeybind keybind, boolean state) {}
}
