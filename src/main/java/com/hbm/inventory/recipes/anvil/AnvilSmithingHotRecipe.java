package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.items.special.ItemHot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** CE {@code AnvilSmithingHotRecipe}: ItemHot inputs need heat ≥ 0.5; output heat is averaged. */
public class AnvilSmithingHotRecipe extends AnvilSmithingRecipe {

    public AnvilSmithingHotRecipe(int tier, ItemStack out, AStack left, AStack right) {
        super(tier, out, left, right);
    }

    @Override
    public boolean doesStackMatch(ItemStack input, AStack recipe) {
        if (input != null && !input.isEmpty() && input.getItem() instanceof ItemHot) {
            if (ItemHot.getHeat(input) < 0.5D) return false;
        }
        return super.doesStackMatch(input, recipe);
    }

    @Override
    public ItemStack getOutput(ItemStack left, ItemStack right) {
        if (left.getItem() instanceof ItemHot && right.getItem() instanceof ItemHot
                && output.getItem() instanceof ItemHot) {
            double h1 = ItemHot.getHeat(left);
            double h2 = ItemHot.getHeat(right);
            ItemStack out = output.copy();
            ItemHot.heatUp(out, (h1 + h2) / 2D);
            return out;
        }
        return output.copy();
    }

    @Override
    public List<ItemStack> getLeft() {
        return heatDemo(super.getLeft());
    }

    @Override
    public List<ItemStack> getRight() {
        return heatDemo(super.getRight());
    }

    private static List<ItemStack> heatDemo(List<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            ItemStack copy = stack.copy();
            if (copy.getItem() instanceof ItemHot) ItemHot.heatUp(copy, 1.0);
            out.add(copy);
        }
        return out;
    }
}
