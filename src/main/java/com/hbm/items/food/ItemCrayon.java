package com.hbm.items.food;

import com.hbm.items.ItemBase;
import com.hbm.items.machine.ItemChemicalDye;

/**
 * Port of CE's {@code ItemCrayon}: flattened from CE's single metadata-multi item (16
 * {@code ItemChemicalDye.EnumChemDye} damage variants sharing one {@code ItemStack}) into 16 distinct
 * registry entries, {@code hbm:crayon_<color>} (see {@link FoodItems}), matching
 * {@code ItemChemicalDye}'s own flattening precedent in {@code com.hbm.items.machine.MachineItems}
 * exactly (each (base item, color) pair becomes its own item with its own texture; the enum value
 * itself is no longer a render-time tint source).
 * <p>
 * CE has no {@code onFoodEaten} override at all for this item (just an always-edible nutrition value),
 * so the only reason for a dedicated class rather than a plain {@code Item} is keeping the color
 * around for lore/tooltip lookups, mirroring {@link ItemChemicalDye}'s own {@code dye} field.
 */
public class ItemCrayon extends ItemBase {

    private final ItemChemicalDye.EnumChemDye color;

    public ItemCrayon(ItemChemicalDye.EnumChemDye color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public ItemChemicalDye.EnumChemDye getColor() {
        return color;
    }
}
