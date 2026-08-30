package com.hbm.items.tool;

import com.hbm.items.ItemBase;
import net.minecraft.world.item.ItemStack;

/**
 * Generic base for tools that degrade one use per craft (dies, stamps, ...) instead of being
 * consumed outright - each craft returns a damaged copy of itself as the "crafting remainder"
 * until it finally wears out. Ported from CE's {@code com.hbm.items.tool.ItemCraftingDegradation}.
 *
 * <p>No concrete die/stamp subclass is registered by this area (none fall under this agent's
 * Phase-1-safe scope), but the class itself is part of the {@code items/tool} package this agent
 * owns, so it is ported here ready for whichever recipe/press content ends up extending it.
 */
public class ItemCraftingDegradation extends ItemBase {

    public ItemCraftingDegradation(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copy();

        if (remainder.isDamageableItem() && remainder.getMaxDamage() > 0) {
            remainder.setDamageValue(remainder.getDamageValue() + 1);

            if (remainder.getDamageValue() >= remainder.getMaxDamage()) {
                return ItemStack.EMPTY;
            }
        }

        return remainder;
    }
}
