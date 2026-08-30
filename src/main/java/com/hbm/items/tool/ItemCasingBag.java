package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemCasingBag} ({@code casing_bag}): a bag that
 * automatically collects spent shell casings (CE: {@code pushCasing}, called from weapon-fire code
 * not owned by this area) into its own 15-slot inventory, viewable via right-click. Follows the
 * {@link com.hbm.items.special.ItemBook} shell pattern - see that class's javadoc - since no generic
 * item-owned-inventory Menu/Screen framework exists in this port yet. {@code pushCasing}'s caller
 * (weapon-fire code) is Phase 3 scope regardless.
 */
public class ItemCasingBag extends Item {

    public ItemCasingBag(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Automatically collects spent shell casings."));
    }

    // Menu-opening interaction and casing-collection deferred - see class javadoc. No use()
    // override until a MenuProvider/Screen equivalent of CE's ContainerCasingBag/GUICasingBag
    // exists.
}
