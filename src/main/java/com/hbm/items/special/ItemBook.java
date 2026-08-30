package com.hbm.items.special;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemBook} ({@code book_of_}): a simple flavor tooltip item whose right-click
 * opened a fixed lore book GUI ({@code ContainerBook}/{@code GUIBook}). Per
 * docs/phase1/items_special.md finding 3, no {@code AbstractContainerMenu}/{@code Screen} framework
 * has been ported yet - registers the item shell now (tooltip included, faithful to CE) with the
 * menu-opening interaction left as an explicit, documented gap rather than a fake/partial
 * implementation.
 */
public class ItemBook extends Item {

    public ItemBook(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Edition 4, gold lined pages"));
    }

    // Menu-opening interaction deferred - see class javadoc. No use()/useOn() override until a
    // MenuProvider/Screen equivalent of CE's ContainerBook/GUIBook exists.
}
