package com.hbm.items.tool;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemToolBox} ({@code toolbox}): a 24-slot toolbox that can
 * either swap its three rows in/out of the player's hotbar (plain right-click) or open its own
 * inventory screen (sneak + right-click), backed by CE's
 * {@code InventoryToolBox}/{@code ContainerToolBox}/{@code GUIToolBox}. Follows the
 * {@link com.hbm.items.special.ItemBook} shell pattern - see that class's javadoc - since no generic
 * item-owned-inventory Menu/Screen framework exists in this port yet. Both interactions ({@code
 * moveRows}'s hotbar swap and the sneak-click GUI open) are deferred together: the hotbar swap
 * itself does not depend on the GUI framework, but it does depend on the same not-yet-decided
 * item-owned-{@code ItemStackHandler} data-component shape the GUI needs, so splitting it out
 * separately would mean re-touching this class twice.
 */
public class ItemToolBox extends Item {

    public ItemToolBox(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Click with the toolbox to swap hotbars in/out of the toolbox.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Shift-click with the toolbox to open the toolbox.").withStyle(ChatFormatting.GRAY));
    }

    // Hotbar-swap and menu-opening interactions deferred - see class javadoc. No use() override
    // until a MenuProvider/Screen equivalent of CE's ContainerToolBox/GUIToolBox (and its backing
    // item-owned inventory data component) exists.
}
