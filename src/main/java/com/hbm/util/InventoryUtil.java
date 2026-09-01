package com.hbm.util;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.anvil.AnvilRecipes.AnvilOutput;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * CE {@code InventoryUtil.doesPlayerHaveAStacks}/{@code giveChanceStacksToPlayer}
 * ({@code InventoryUtil.java}:215-288). Anvil construction pulls from the player inventory.
 */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    public static boolean doesPlayerHaveAStacks(Player player, List<AStack> stacks, boolean shouldRemove) {
        Inventory inv = player.getInventory();
        ItemStack[] copy = new ItemStack[inv.items.size()];
        AStack[] input = new AStack[stacks.size()];
        for (int i = 0; i < input.length; i++) {
            input[i] = stacks.get(i).copy();
        }
        for (int i = 0; i < inv.items.size(); i++) {
            copy[i] = inv.items.get(i).copy();
        }
        for (int i = 0; i < input.length; i++) {
            AStack stack = input[i];
            if (stack == null || stack.count() <= 0) {
                input[i] = null;
                continue;
            }
            for (int j = 0; j < copy.length; j++) {
                ItemStack slot = copy[j];
                if (slot.isEmpty() || !stack.matchesRecipe(slot, true)) continue;
                int size = Math.min(stack.count(), slot.getCount());
                stack.setCount(stack.count() - size);
                slot.shrink(size);
                if (stack.count() <= 0) {
                    input[i] = null;
                    break;
                }
            }
        }
        for (AStack stack : input) {
            if (stack != null) return false;
        }
        if (shouldRemove) {
            for (int i = 0; i < inv.items.size(); i++) {
                inv.items.set(i, copy[i].isEmpty() ? ItemStack.EMPTY : copy[i]);
            }
        }
        return true;
    }

    public static void giveChanceStacksToPlayer(Player player, List<AnvilOutput> stacks) {
        for (AnvilOutput out : stacks) {
            if (out.chance < 1.0F && player.getRandom().nextFloat() >= out.chance) continue;
            ItemStack give = out.stack.copy();
            if (!player.getInventory().add(give)) {
                player.drop(give, false);
            }
        }
    }
}
