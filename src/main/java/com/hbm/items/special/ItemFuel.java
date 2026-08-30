package com.hbm.items.special;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * Port of CE's {@code ItemFuel} (com.hbm.items.special.ItemFuel): a plain item with a fixed
 * furnace burn time.
 * <p>
 * CE's version also carried two hardcoded {@code addInformation} easter-egg tooltip branches for
 * {@code ModItems.dust} and {@code ModItems.powder_fire} - neither of those fields belongs to this
 * area (they are {@code powder_}-family items owned by a different Phase 1 area), so they are not
 * reproduced here. This class only needs to cover this area's two {@code ItemFuel}-backed fields,
 * {@code ingot_graphite} and {@code ingot_c4}, neither of which had a tooltip branch in CE.
 */
public class ItemFuel extends Item {

    private final int burnTime;

    public ItemFuel(Properties properties, int burnTime) {
        super(properties);
        this.burnTime = burnTime;
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        return burnTime;
    }
}
