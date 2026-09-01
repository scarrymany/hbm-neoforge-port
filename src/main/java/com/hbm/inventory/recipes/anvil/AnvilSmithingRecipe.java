package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** CE {@code AnvilSmithingRecipe.java}:9-75. */
public class AnvilSmithingRecipe {

    public final int tier;
    public final ItemStack output;
    public final AStack left;
    public final AStack right;
    private boolean shapeless;

    public AnvilSmithingRecipe(int tier, ItemStack out, AStack left, AStack right) {
        this.tier = tier;
        this.output = out;
        this.left = left;
        this.right = right;
    }

    public AnvilSmithingRecipe makeShapeless() {
        this.shapeless = true;
        return this;
    }

    public boolean matches(ItemStack left, ItemStack right) {
        return matchesInt(left, right) != -1;
    }

    public int matchesInt(ItemStack left, ItemStack right) {
        if (doesStackMatch(left, this.left) && doesStackMatch(right, this.right)) return 0;
        if (shapeless) {
            return doesStackMatch(right, this.left) && doesStackMatch(left, this.right) ? 1 : -1;
        }
        return -1;
    }

    public boolean doesStackMatch(ItemStack input, AStack recipe) {
        return recipe != null && recipe.matchesRecipe(input, false);
    }

    public List<ItemStack> getLeft() {
        return left.getStackList();
    }

    public List<ItemStack> getRight() {
        return right.getStackList();
    }

    public ItemStack getSimpleOutput() {
        return output.copy();
    }

    public ItemStack getOutput(ItemStack left, ItemStack right) {
        return getSimpleOutput();
    }

    public int amountConsumed(int index, boolean mirrored) {
        if (index == 0) return mirrored ? right.stacksize : left.stacksize;
        if (index == 1) return mirrored ? left.stacksize : right.stacksize;
        return 0;
    }
}
