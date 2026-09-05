package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.food.FoodDataComponents;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * CE {@code AnvilSmithingCyanideRecipe}: poison food via {@code plan_c} or {@code pill_red}.
 * Sets NBT {@code ntmCyanide} or {@code ntmRedPill}. Port: use {@code FoodDataComponents.CYANIDE} / {@code RED_PILL}.
 */
public class AnvilSmithingCyanideRecipe extends AnvilSmithingRecipe {

    private static Item planC() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "plan_c"));
    }

    private static Item pillRed() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, "pill_red"));
    }

    public AnvilSmithingCyanideRecipe() {
        super(1, new ItemStack(Items.BREAD), new ComparableStack(Items.BREAD), new ComparableStack(planC()));
    }

    @Override
    public boolean matches(ItemStack left, ItemStack right) {
        return (doesStackMatch(right, this.right) || right.getItem() == pillRed())
                && left.getFoodProperties(null) != null;
    }

    @Override
    public int matchesInt(ItemStack left, ItemStack right) {
        return matches(left, right) ? 0 : -1;
    }

    @Override
    public ItemStack getOutput(ItemStack left, ItemStack right) {
        ItemStack out = left.copyWithCount(1);
        if (right.getItem() == pillRed()) {
            out.set(FoodDataComponents.RED_PILL.get(), true);
        } else {
            out.set(FoodDataComponents.CYANIDE.get(), true);
        }
        return out;
    }
}
