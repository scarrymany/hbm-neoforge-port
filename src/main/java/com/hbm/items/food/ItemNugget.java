package com.hbm.items.food;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Port of CE's {@code ItemNugget}: a thin {@code ItemFood} wrapper with only one real CE instance
 * ({@code gun_moist_nugget} - see {@link FoodItems}), tooltip joke only, no {@code onFoodEaten}
 * override. Since there is only ever one instance, the tooltip is a fixed literal rather than a
 * registry-path switch (unlike {@link ItemEnergy}/{@link ItemPill}, which have many instances).
 */
public class ItemNugget extends Item {

    public ItemNugget(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("A Mosin-Na...no wait, it's"));
        tooltip.add(Component.literal("just a moist nugget."));
    }
}
