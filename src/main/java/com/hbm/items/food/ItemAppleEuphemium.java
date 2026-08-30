package com.hbm.items.food;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Port of CE's {@code ItemAppleEuphemium} ({@code apple_euphemium}): a golden-apple-style
 * infinite-duration buff apple. CE's {@code onFoodEaten} (plain vanilla {@code Resistance}/
 * {@code FireResistance}/{@code Saturation} at max duration/amplifier) is fully declarative through
 * {@code FoodProperties.Builder#effect} at registration time (see {@link FoodItems}), matching the
 * existing {@link ItemLemon}'s approach. The only reason this needs its own class is CE's
 * unconditional {@code hasEffect() -> true} override (an enchantment glint with no enchantments
 * present), which has no {@code FoodProperties} equivalent.
 */
public class ItemAppleEuphemium extends Item {

    public ItemAppleEuphemium(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
