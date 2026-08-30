package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemAmmoBag} ({@code ammo_bag}/{@code ammo_bag_infinite}):
 * a bag with its own 8-slot ammunition inventory, owned by the item stack itself (not a placed
 * block) - CE's {@code InventoryAmmoBag} backed a {@code ContainerAmmoBag}/{@code GUIAmmoBag} pair
 * opened on right-click. Follows the {@link com.hbm.items.special.ItemBook} shell pattern - see that
 * class's javadoc - since no generic item-owned-inventory Menu/Screen framework exists in this port
 * yet. The bag's own inventory contents (CE: NBT under the stack's {@code "Inventory"} tag) are not
 * yet representable either, pending that same framework deciding the data-component shape for an
 * item-carried {@code ItemStackHandler}.
 */
public class ItemAmmoBag extends Item {

    public ItemAmmoBag(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Holds a small reserve of ammunition."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use() override until a
    // MenuProvider/Screen equivalent of CE's ContainerAmmoBag/GUIAmmoBag exists.
}
