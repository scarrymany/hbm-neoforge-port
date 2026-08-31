package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * CE {@code ModItems.pwr_fuel_depleted} ({@code ItemEnumMulti} byproduct marker, cited from
 * {@code MachineItems} / {@code PWRHotFuelItems}). Flattened so PUREX PWR recycle recipes
 * ({@code PUREXRecipes.java:247-359}) can address a real registry id.
 */
public final class PWRDepletedFuelItems {

    public static final Map<EnumPWRFuel, DeferredItem<Item>> DEPLETED = new EnumMap<>(EnumPWRFuel.class);

    private PWRDepletedFuelItems() {
    }

    public static void registerAll() {
        for (EnumPWRFuel type : EnumPWRFuel.VALUES) {
            String name = "pwr_fuel_depleted_" + type.name().toLowerCase(Locale.ROOT);
            DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
            DEPLETED.put(type, item);
            CreativeTabContents.add(ModCreativeTabs.CONTROL, item);
        }
    }
}
