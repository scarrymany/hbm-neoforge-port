package com.hbm.items.machine;

import com.hbm.creativetabs.CreativeTabContents;
import com.hbm.creativetabs.ModCreativeTabs;
import com.hbm.items.ItemBase;
import com.hbm.items.ItemEnums;
import com.hbm.items.ModItems;
import com.hbm.items.food.ItemLemon;
import com.hbm.items.special.ItemFuel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * Cheap CE items that unblock leftover chemplant / mixer / PUREX / liquefactor / solidifier / PA.
 * Citations: {@code ModItems.java:393} {@code fuel_additive}, {@code :1231-1232} biomass,
 * {@code :1287} {@code pellet_charged}, {@code :1325} {@code oil_tar}/{@code EnumTarType},
 * {@code :1330-1333} {@code solid_fuel*}, {@code :1155} {@code dust}, {@code :1234}/{:1237}
 * cordite/ball_tnt, {@code :2314+} {@code particle_*}, {@code :943} {@code bio_wafer}.
 */
public final class Phase11ProcessItems {

    public static DeferredItem<Item> PELLET_CHARGED;
    public static DeferredItem<Item> BIOMASS;
    public static DeferredItem<Item> BIOMASS_COMPRESSED;
    public static DeferredItem<Item> BIO_WAFER;
    public static DeferredItem<Item> FUEL_ADDITIVE_ANTIKNOCK;
    public static DeferredItem<Item> FUEL_ADDITIVE_DEICER;
    public static DeferredItem<Item> NUCLEAR_WASTE_TINY;
    public static DeferredItem<Item> NUCLEAR_WASTE_VITRIFIED;
    public static DeferredItem<Item> DUST;
    public static DeferredItem<Item> SOLID_FUEL;
    public static DeferredItem<Item> SOLID_FUEL_BF;
    public static DeferredItem<Item> CORDITE;
    public static DeferredItem<Item> BALL_TNT;
    public static DeferredItem<Item> BALL_DYNAMITE;
    public static DeferredItem<Item> BALL_TATB;
    public static DeferredItem<Item> ROCKET_FUEL;
    public static DeferredItem<Item> POWDER_SAWDUST;
    public static DeferredItem<Item> GEM_TANTALIUM;
    public static DeferredItem<Item> CANISTER_NAPALM;
    public static DeferredItem<Item> PART_LITHIUM;
    public static DeferredItem<Item> PART_BERYLLIUM;
    public static DeferredItem<Item> PART_CARBON;
    public static DeferredItem<Item> PART_COPPER;
    public static DeferredItem<Item> PART_PLUTONIUM;

    private Phase11ProcessItems() {
    }

    public static void registerAll() {
        PELLET_CHARGED = parts("pellet_charged");
        BIOMASS = fuel("biomass", 20);
        BIOMASS_COMPRESSED = fuel("biomass_compressed", 800);
        BIO_WAFER = food("bio_wafer", 8, 8F);
        FUEL_ADDITIVE_ANTIKNOCK = control("fuel_additive_antiknock");
        FUEL_ADDITIVE_DEICER = control("fuel_additive_deicer");
        NUCLEAR_WASTE_TINY = nuke("nuclear_waste_tiny");
        NUCLEAR_WASTE_VITRIFIED = nuke("nuclear_waste_vitrified");
        DUST = parts("dust");
        SOLID_FUEL = fuel("solid_fuel", 3200);
        SOLID_FUEL_BF = parts("solid_fuel_bf");
        CORDITE = parts("cordite");
        BALL_TNT = parts("ball_tnt");
        BALL_DYNAMITE = parts("ball_dynamite");
        BALL_TATB = parts("ball_tatb");
        ROCKET_FUEL = fuel("rocket_fuel", 6400);
        POWDER_SAWDUST = parts("powder_sawdust");
        GEM_TANTALIUM = parts("gem_tantalium");
        CANISTER_NAPALM = parts("canister_napalm");
        PART_LITHIUM = parts("part_lithium");
        PART_BERYLLIUM = parts("part_beryllium");
        PART_CARBON = parts("part_carbon");
        PART_COPPER = parts("part_copper");
        PART_PLUTONIUM = parts("part_plutonium");

        for (ItemEnums.EnumTarType type : ItemEnums.EnumTarType.VALUES) {
            parts("oil_tar_" + type.name().toLowerCase());
        }

        String[] particles = {
                "particle_empty", "particle_hydrogen", "particle_copper", "particle_lead",
                "particle_amat", "particle_aschrab", "particle_dark", "particle_higgs",
                "particle_tachyon", "particle_strange", "particle_sparkticle"
        };
        for (String id : particles) {
            control(id);
        }
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

    private static DeferredItem<Item> food(String name, int nutrition, float saturation) {
        FoodProperties food = new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build();
        DeferredItem<Item> item = ModItems.ITEMS.register(name, () -> new ItemLemon(new Item.Properties().food(food)));
        CreativeTabContents.add(ModCreativeTabs.PARTS, item);
        return item;
    }
}
