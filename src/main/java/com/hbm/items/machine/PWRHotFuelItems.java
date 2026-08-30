package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemPWRFuel.EnumPWRFuel;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Registers the 15 {@link ItemPWRFuelHot} grades ({@code pwr_fuel_hot_<type>}), CE's
 * {@code ModItems.pwr_fuel_hot} byproduct marker (see that class's own javadoc for the exact CE
 * citation). A standalone registration class - not folded into {@code MachineItems}, which this
 * PWR package deliberately does not edit (many Phase 2 areas land in the same wave; see this port's
 * "create your own top-level registration class" convention, already used by
 * {@code PowerGenBlocks}/{@code ProcessingBlocks} for the identical reason) - appends directly to
 * the already-public {@link ModItems#ITEMS} {@code DeferredRegister.Items}, exactly the "any class
 * may append" contract {@code ModBlocks.BLOCKS} relies on elsewhere.
 *
 * <p>Called from {@link com.hbm.blocks.machine.PWRBlocks#registerAll()}, the same call site that
 * wires the rest of this PWR family in - see that class's javadoc for the single
 * {@code ModBlocks.register()} line the orchestrating session needs to add.
 */
public final class PWRHotFuelItems {

    public static final Map<EnumPWRFuel, DeferredItem<Item>> HOT_FUEL = new EnumMap<>(EnumPWRFuel.class);

    private PWRHotFuelItems() {
    }

    public static void registerAll() {
        for (EnumPWRFuel type : EnumPWRFuel.VALUES) {
            String name = "pwr_fuel_hot_" + type.name().toLowerCase(Locale.ROOT);
            DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemPWRFuelHot(type, new Item.Properties()));
            HOT_FUEL.put(type, item);
            CreativeTabContents.add(ModCreativeTabs.CONTROL, item);
        }
    }
}
