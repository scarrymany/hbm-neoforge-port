package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CE {@code LemegetonRecipes.java}:20-65 — 37 material-upgrade conversions for the Book of Lemegeton.
 */
public final class LemegetonRecipes {

    public static final Map<AStack, ItemStack> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private LemegetonRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE LemegetonRecipes.java:20-65 — each RECIPES.put is a census site
        RECIPES.put(OreDictStack.ofCommonTag("ingots/iron"), stack("ingot_steel"));
        RECIPES.put(new ComparableStack(item("ingot_steel")), stack("ingot_dura_steel"));
        RECIPES.put(new ComparableStack(item("ingot_dura_steel")), stack("ingot_tcalloy"));
        RECIPES.put(new ComparableStack(item("ingot_tcalloy")), stack("ingot_combine_steel"));
        RECIPES.put(new ComparableStack(item("ingot_combine_steel")), stack("ingot_dineutronium"));

        RECIPES.put(new ComparableStack(item("ingot_titanium")), stack("ingot_saturnite"));
        RECIPES.put(new ComparableStack(item("ingot_saturnite")), stack("ingot_starmetal"));

        RECIPES.put(OreDictStack.ofCommonTag("ingots/copper"), stack("ingot_red_copper"));
        RECIPES.put(new ComparableStack(item("ingot_red_copper")), stack("ingot_desh"));
        RECIPES.put(new ComparableStack(item("ingot_desh")), stack("ingot_bscco"));

        RECIPES.put(new ComparableStack(item("ingot_lead")), new ItemStack(Items.GOLD_INGOT));
        RECIPES.put(new ComparableStack(Items.GOLD_INGOT), stack("ingot_bismuth"));
        RECIPES.put(new ComparableStack(item("ingot_bismuth")), stack("ingot_osmiridium"));

        RECIPES.put(new ComparableStack(item("ingot_th232")), stack("ingot_uranium"));
        RECIPES.put(new ComparableStack(item("ingot_uranium")), stack("ingot_u238"));
        RECIPES.put(new ComparableStack(item("ingot_u238")), stack("ingot_u235"));
        RECIPES.put(new ComparableStack(item("ingot_u235")), stack("ingot_plutonium"));
        RECIPES.put(new ComparableStack(item("ingot_plutonium")), stack("ingot_pu238"));
        RECIPES.put(new ComparableStack(item("ingot_pu238")), stack("ingot_pu239"));
        RECIPES.put(new ComparableStack(item("ingot_pu239")), stack("ingot_pu240"));
        RECIPES.put(new ComparableStack(item("ingot_pu240")), stack("ingot_pu241"));
        RECIPES.put(new ComparableStack(item("ingot_pu241")), stack("ingot_am241"));
        RECIPES.put(new ComparableStack(item("ingot_am241")), stack("ingot_am242"));

        RECIPES.put(new ComparableStack(item("ingot_ra226")), stack("ingot_polonium"));
        RECIPES.put(new ComparableStack(item("ingot_polonium")), stack("ingot_technetium"));

        RECIPES.put(new ComparableStack(item("ingot_polymer")), stack("ingot_pc"));
        RECIPES.put(new ComparableStack(item("ingot_bakelite")), stack("ingot_pvc"));
        RECIPES.put(new ComparableStack(item("ingot_latex")), stack("ingot_rubber"));

        RECIPES.put(new ComparableStack(Items.COAL), stack("ingot_graphite"));
        RECIPES.put(new ComparableStack(item("ingot_graphite")), new ItemStack(Items.DIAMOND));
        RECIPES.put(new ComparableStack(Items.DIAMOND), stack("ingot_cft"));

        RECIPES.put(new ComparableStack(item("fluorite")), stack("gem_sodalite"));
        RECIPES.put(new ComparableStack(item("gem_sodalite")), stack("gem_volcanic"));
        RECIPES.put(new ComparableStack(item("gem_volcanic")), stack("gem_rad"));
        RECIPES.put(new ComparableStack(item("gem_rad")), stack("gem_alexandrite"));

        RECIPES.put(new ComparableStack(Blocks.SAND), stack("ingot_fiberglass"));
        RECIPES.put(new ComparableStack(item("ingot_fiberglass")), stack("ingot_asbestos"));

        RECIPES.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty()
                || e.getValue().getItem() == Items.AIR);
    }

    public static ItemStack getRecipe(ItemStack ingredient) {
        register();
        if (ingredient == null || ingredient.isEmpty()) return ItemStack.EMPTY;
        for (Map.Entry<AStack, ItemStack> entry : RECIPES.entrySet()) {
            if (entry.getKey().matchesRecipe(ingredient, true)) return entry.getValue().copy();
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack stack(String id) {
        Item resolved = item(id);
        return resolved == Items.AIR ? ItemStack.EMPTY : new ItemStack(resolved);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }
}
