package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * CE {@code AnvilSmithingRenameRecipe}: applies name-tag's custom name to any item in left slot.
 * CE {@code :40} uses raw NBT escape {@code \\&} → {@code §}; 1.21 supports {@code Component} style codes directly.
 */
public class AnvilSmithingRenameRecipe extends AnvilSmithingRecipe {

    public AnvilSmithingRenameRecipe() {
        super(1, new ItemStack(Items.IRON_SWORD), new ComparableStack(Items.IRON_SWORD), new ComparableStack(Items.NAME_TAG, 1));
    }

    @Override
    public boolean matches(ItemStack left, ItemStack right) {
        return doesStackMatch(right, this.right) && getDisplayName(right) != null;
    }

    @Override
    public int matchesInt(ItemStack left, ItemStack right) {
        return matches(left, right) ? 0 : -1;
    }

    @Override
    public ItemStack getOutput(ItemStack left, ItemStack right) {
        ItemStack out = left.copyWithCount(1);
        String name = getDisplayName(right);
        if (name != null) {
            name = name.replace("\\&", "§");
            Component displayName = Component.literal("§r" + name).withStyle(ChatFormatting.RESET);
            out.set(DataComponents.CUSTOM_NAME, displayName);
        }
        return out;
    }

    @Override
    public int amountConsumed(int index, boolean mirrored) {
        if (index == 0) return mirrored ? 0 : left.stacksize;
        if (index == 1) return mirrored ? left.stacksize : 0;
        return 0;
    }

    private String getDisplayName(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null ? name.getString() : null;
    }
}
