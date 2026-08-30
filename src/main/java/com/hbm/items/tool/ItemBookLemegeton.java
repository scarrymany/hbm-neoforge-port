package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemBookLemegeton} ({@code book_lemegeton}): an
 * occult/ritual grimoire whose right-click opened {@code GUILemegeton}/{@code ContainerLemegeton}.
 * Follows the {@link com.hbm.items.special.ItemBook} shell pattern - see that class's javadoc -
 * since no Menu/Screen framework exists in this port yet.
 */
public class ItemBookLemegeton extends Item {

    public ItemBookLemegeton(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Ars Goetia - the Lesser Key of Solomon."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use() override until a
    // MenuProvider/Screen equivalent of CE's GUILemegeton/ContainerLemegeton exists.
}
