package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CE {@code ArcWelderRecipes.java}. Generated from CE registerDefaults.
 */
public final class ArcWelderRecipes {

    public static final List<ArcWelderRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private ArcWelderRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("motor"), 2), 100, 400L, new ComparableStack(item("plate_steel"), 2), new ComparableStack(item("mingrade_dense_wire"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("part_generic_lde"), 1), 200, 5000L, new ComparableStack(item("plate_aluminium"), 4), new ComparableStack(item("ingot_fiberglass"), 4), OreDictStack.ofHbmTag("any_hardplastic", 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("part_generic_lde"), 1), 200, 10000L, new ComparableStack(item("plate_titanium"), 2), new ComparableStack(item("ingot_fiberglass"), 4), OreDictStack.ofHbmTag("any_hardplastic", 1)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("copper_dense_wire"), 1), 100, 10000L, new ComparableStack(item("copper_wire"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("mingrade_dense_wire"), 1), 100, 10000L, new ComparableStack(item("mingrade_wire"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("gold_dense_wire"), 1), 100, 10000L, new ComparableStack(item("gold_wire"), 8)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("iron_plate_sextuple"), 1), 100, 100L, new ComparableStack(item("iron_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("steel_plate_sextuple"), 1), 100, 500L, new ComparableStack(item("steel_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("copper_plate_sextuple"), 1), 200, 1000L, new ComparableStack(item("copper_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("titanium_plate_sextuple"), 1), 600, 50000L, new ComparableStack(item("titanium_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("zirconium_plate_sextuple"), 1), 600, 10000L, new ComparableStack(item("zirconium_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("tcalloy_plate_sextuple"), 1), 1200, 1000000L, new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("tcalloy_plate_triple"), 2)));
        RECIPES.add(new ArcWelderRecipe(new ItemStack(item("tungsten_plate_sextuple"), 1), 1200, 250000L, new FluidStack(Fluids.OXYGEN, 1000), new ComparableStack(item("tungsten_plate_triple"), 2)));
    }

    private static Item item(String id) {
        if (id.contains(":")) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static synchronized void register() {
        registerDefaults();
    }

    public static ArcWelderRecipe getRecipe(ItemStack... inputs) {
        registerDefaults();
        outer:
        for (ArcWelderRecipe recipe : RECIPES) {
            List<AStack> left = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack in : inputs) {
                if (in.isEmpty()) continue;
                boolean hit = false;
                for (int i = 0; i < left.size(); i++) {
                    AStack key = left.get(i);
                    if (key.matchesRecipe(in, true) && in.getCount() >= key.count()) {
                        left.remove(i);
                        hit = true;
                        break;
                    }
                }
                if (!hit) continue outer;
            }
            if (left.isEmpty()) return recipe;
        }
        return null;
    }

    public static class ArcWelderRecipe {
        public final AStack[] ingredients;
        public final FluidStack fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }
    }
}
