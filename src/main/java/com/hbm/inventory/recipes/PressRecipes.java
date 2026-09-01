package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemStamp;
import com.hbm.items.machine.ItemStamp.StampType;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code PressRecipes.java}:56-105. Stamp + ingredient → output.
 * Each {@code recipes.put} is a census site. AIR outputs dropped after register.
 */
public final class PressRecipes {

    public static final Map<Pair<AStack, StampType>, ItemStack> recipes = new LinkedHashMap<>();

    private static boolean registered = false;

    private PressRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE PressRecipes.java:58-70 FLAT
        recipes.put(pair(OreDictStack.ofCommonTag("dusts/quartz"), StampType.FLAT), new ItemStack(Items.QUARTZ));
        recipes.put(pair(OreDictStack.ofCommonTag("dusts/lapis"), StampType.FLAT), new ItemStack(Items.LAPIS_LAZULI));
        recipes.put(pair(OreDictStack.ofCommonTag("dusts/diamond"), StampType.FLAT), new ItemStack(Items.DIAMOND));
        recipes.put(pair(OreDictStack.ofCommonTag("dusts/emerald"), StampType.FLAT), new ItemStack(Items.EMERALD));
        recipes.put(pair(new ComparableStack(item("biomass")), StampType.FLAT), stack("biomass_compressed"));
        recipes.put(pair(OreDictStack.ofHbmTag("any_coke", 1), StampType.FLAT), stack("ingot_graphite"));
        recipes.put(pair(new ComparableStack(item("meteorite_sword_reforged")), StampType.FLAT), stack("meteorite_sword_hardened"));
        recipes.put(pair(new ComparableStack(Blocks.JUNGLE_LOG), StampType.FLAT), stack("ball_resin"));
        recipes.put(pair(OreDictStack.ofCommonTag("dusts/coal"), StampType.FLAT), stack("briquette_coal"));
        recipes.put(pair(OreDictStack.ofCommonTag("dusts/lignite"), StampType.FLAT), stack("briquette_lignite"));
        recipes.put(pair(new ComparableStack(item("powder_sawdust")), StampType.FLAT), stack("briquette_wood"));

        // :71-83 PLATE
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/iron"), StampType.PLATE), stack("plate_iron"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/gold"), StampType.PLATE), stack("plate_gold"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/titanium"), StampType.PLATE), stack("plate_titanium"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/aluminum"), StampType.PLATE), stack("plate_aluminium"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/steel"), StampType.PLATE), stack("plate_steel"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/lead"), StampType.PLATE), stack("plate_lead"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/copper"), StampType.PLATE), stack("plate_copper"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/schrabidium"), StampType.PLATE), stack("plate_schrabidium"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/cmb_steel"), StampType.PLATE), stack("plate_combine_steel"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/gunmetal"), StampType.PLATE), stack("plate_gunmetal"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/weaponsteel"), StampType.PLATE), stack("plate_weaponsteel"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/saturnite"), StampType.PLATE), stack("plate_saturnite"));
        recipes.put(pair(OreDictStack.ofCommonTag("ingots/dura_steel"), StampType.PLATE), stack("plate_dura_steel"));

        // :85-88 casings
        recipes.put(pair(OreDictStack.ofCommonTag("plates/gunmetal"), StampType.C9), stack("casing_small", 4));
        recipes.put(pair(OreDictStack.ofCommonTag("plates/gunmetal"), StampType.C50), stack("casing_large", 2));
        recipes.put(pair(OreDictStack.ofCommonTag("plates/weaponsteel"), StampType.C9), stack("casing_small_steel", 4));
        recipes.put(pair(OreDictStack.ofCommonTag("plates/weaponsteel"), StampType.C50), stack("casing_large_steel", 2));

        // :90-94 wire autogen — one put per WIRE-shaped mat
        for (NTMMaterial mat : Mats.orderedList) {
            if (!mat.getAutogen().contains(MaterialShapes.WIRE)) continue;
            Item wire = item(MaterialShapes.WIRE.buildRegistryName(mat));
            if (wire == Items.AIR) continue;
            recipes.put(pair(OreDictStack.ofCommonTag("ingots/" + mat.getRegistryName()), StampType.WIRE), new ItemStack(wire, 8));
        }

        // :96 circuit
        recipes.put(pair(OreDictStack.ofCommonTag("billets/silicon"), StampType.CIRCUIT), stack("circuit_silicon"));

        // :98-105 printing book
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING1), stack("page_of_page1"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING2), stack("page_of_page2"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING3), stack("page_of_page3"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING4), stack("page_of_page4"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING5), stack("page_of_page5"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING6), stack("page_of_page6"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING7), stack("page_of_page7"));
        recipes.put(pair(new ComparableStack(Items.PAPER), StampType.PRINTING8), stack("page_of_page8"));

        recipes.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().getItem() == Items.AIR);
    }

    public static ItemStack getOutput(ItemStack ingredient, ItemStack stamp) {
        if (ingredient == null || ingredient.isEmpty() || stamp == null || stamp.isEmpty()) return ItemStack.EMPTY;
        if (!(stamp.getItem() instanceof ItemStamp itemStamp)) return ItemStack.EMPTY;
        StampType type = itemStamp.getStampType();
        register();
        for (Map.Entry<Pair<AStack, StampType>, ItemStack> e : recipes.entrySet()) {
            if (e.getKey().getValue() == type && e.getKey().getKey().matchesRecipe(ingredient, true)) {
                return e.getValue().copy();
            }
        }
        return ItemStack.EMPTY;
    }

    private static Pair<AStack, StampType> pair(AStack in, StampType type) {
        return new Pair<>(in, type);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id) {
        return stack(id, 1);
    }

    private static ItemStack stack(String id, int n) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }
}
