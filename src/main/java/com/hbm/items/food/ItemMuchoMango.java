package com.hbm.items.food;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;

import java.util.List;

/**
 * Port of CE's {@code ItemMuchoMango} ({@code mucho_mango}, "The Comically Large Can"). CE's own
 * {@code onFoodEaten} (a plain vanilla {@code Speed} buff) is fully declarative through
 * {@code FoodProperties.Builder#effect} at registration time (see {@link FoodItems}), matching the
 * existing {@link ItemLemon}'s approach - the only reason this needs its own class at all is CE's
 * unusually long 200-tick (10 second) drink duration with the DRINK animation, which the vanilla
 * {@code Item} base class can't express through {@code FoodProperties} alone.
 */
public class ItemMuchoMango extends Item {

    public ItemMuchoMango(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 200;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.literal("The Comically Large Can"));
    }
}
