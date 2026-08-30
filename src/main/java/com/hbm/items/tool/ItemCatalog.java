package com.hbm.items.tool;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code com.hbm.items.tool.ItemCatalog} ({@code bobmazon}/{@code bobmazon_hidden}): an
 * in-game "Bobmazon" mail-order shop screen. Right-clicking opened {@code GUIScreenBobmazon} against
 * offers resolved by {@code BobmazonOfferFactory} from the held stack. Follows the
 * {@link com.hbm.items.special.ItemBook} shell pattern - see that class's javadoc - since neither the
 * menu framework nor {@code BobmazonOfferFactory} exist in this port yet.
 */
public class ItemCatalog extends Item {

    public ItemCatalog(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("Bobmazon Prime - mail order catalog."));
    }

    // Menu-opening interaction deferred - see class javadoc. No use() override until a
    // MenuProvider/Screen equivalent of CE's GUIScreenBobmazon (and BobmazonOfferFactory) exists.
}
