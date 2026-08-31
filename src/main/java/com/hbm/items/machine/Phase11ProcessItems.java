package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ModItems;
import com.hbm.items.special.ItemFuel;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Cheap CE items that unblock leftover chemplant / mixer / PUREX / liquefactor recipes.
 * Citations: {@code ModItems.java:393} {@code fuel_additive}, {@code :1231-1232} biomass,
 * {@code :1287} {@code pellet_charged}; waste companions already have models/lang in this port.
 */
public final class Phase11ProcessItems {

    public static DeferredItem<Item> PELLET_CHARGED;
    public static DeferredItem<Item> BIOMASS;
    public static DeferredItem<Item> BIOMASS_COMPRESSED;
    public static DeferredItem<Item> FUEL_ADDITIVE_ANTIKNOCK;
    public static DeferredItem<Item> FUEL_ADDITIVE_DEICER;
    public static DeferredItem<Item> NUCLEAR_WASTE_TINY;
    public static DeferredItem<Item> NUCLEAR_WASTE_VITRIFIED;

    private Phase11ProcessItems() {
    }

    public static void registerAll() {
        PELLET_CHARGED = parts("pellet_charged");
        BIOMASS = fuel("biomass", 20);
        BIOMASS_COMPRESSED = fuel("biomass_compressed", 800);
        FUEL_ADDITIVE_ANTIKNOCK = control("fuel_additive_antiknock");
        FUEL_ADDITIVE_DEICER = control("fuel_additive_deicer");
        NUCLEAR_WASTE_TINY = nuke("nuclear_waste_tiny");
        NUCLEAR_WASTE_VITRIFIED = nuke("nuclear_waste_vitrified");
    }

    private static DeferredItem<Item> parts(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }

    private static DeferredItem<Item> control(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.CONTROL, item);
        return item;
    }

    private static DeferredItem<Item> nuke(String name) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemBase(new Item.Properties()));
        CreativeTabContents.add(ModCreativeTabs.NUKE, item);
        return item;
    }

    private static DeferredItem<Item> fuel(String name, int burn) {
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemFuel(new Item.Properties(), burn));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }
}
