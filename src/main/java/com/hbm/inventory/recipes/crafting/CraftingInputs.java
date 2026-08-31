package com.hbm.inventory.recipes.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;

/**
 * Tiny shared helper for the "exactly one non-empty stack anywhere in the grid" match shape both
 * {@link RBMKFuelRecycleRecipe} and {@link ScrapSplitRecipe} need - CE's own
 * {@code RBMKFuelCraftingHandler#hasExactlyOneStack}/{@code getFirstStack} and
 * {@code ScrapsCraftingHandler#matches}' inline equivalent, generalized off CE's hardcoded 3x3
 * {@code getStackInRowAndColumn} double loop onto {@link CraftingInput#size()}/{@code getItem(int)}
 * (slot order is irrelevant to either recipe - both only care "is there exactly one occupied slot",
 * not which one) so the same logic keeps working in the player's 2x2 personal-inventory grid too,
 * matching CE's own {@code canFit(width, height): width >= 1 && height >= 1} (RBMK) / {@code width *
 * height >= 1} (Scraps) - neither is restricted to a real crafting table.
 */
final class CraftingInputs {

    private CraftingInputs() {
    }

    /** @return the single non-empty stack in {@code input}, or {@code null} if there are zero or 2+. */
    static ItemStack onlyNonEmptyStack(CraftingInput input) {
        ItemStack found = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (found != null) return null;
            found = stack;
        }
        return found;
    }
}
