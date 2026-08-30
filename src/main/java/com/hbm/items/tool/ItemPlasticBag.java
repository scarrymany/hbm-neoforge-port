package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemPlasticBag} ({@code plastic_bag}): a single-slot bag
 * viewable via right-click, backed by CE's {@code InventoryPlasticBag}/{@code ContainerPlasticBag}/
 * {@code GUIPlasticBag}. Follows the {@link com.hbm.items.special.ItemBook} shell pattern - see that
 * class's javadoc - since no generic item-owned-inventory Menu/Screen framework exists in this port
 * yet.
 */
public class ItemPlasticBag extends Item {

    public ItemPlasticBag(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Holds a single item, keeping it safe and dry."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use() override until a
    // MenuProvider/Screen equivalent of CE's ContainerPlasticBag/GUIPlasticBag exists.
}
