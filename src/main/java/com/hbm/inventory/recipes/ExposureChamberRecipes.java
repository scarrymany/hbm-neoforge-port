package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * CE {@code ExposureChamberRecipes.java}:54-65. Census: {@code RECIPES.add}.
 * Cheap path (no expensive-mode DEGENERATE_MATTER): sparkticle + SBD.ingot → dineutronium.
 */
public final class ExposureChamberRecipes {

    public static final List<ExposureChamberRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private ExposureChamberRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE ExposureChamberRecipes.java:55-56 Higgs + U.ingot → schraranium
        RECIPES.add(new ExposureChamberRecipe(cs("particle_higgs"), cs("ingot_uranium"), stack("ingot_schraranium")));
        // CE ExposureChamberRecipes.java:57-58 Higgs + U238.ingot → schrabidium
        RECIPES.add(new ExposureChamberRecipe(cs("particle_higgs"), cs("ingot_u238"), stack("ingot_schrabidium")));
        // CE ExposureChamberRecipes.java:59-60 dark + PU.ingot → euphemium
        RECIPES.add(new ExposureChamberRecipe(cs("particle_dark"), cs("ingot_plutonium"), stack("ingot_euphemium")));
        // CE ExposureChamberRecipes.java:64 cheap branch — sparkticle + SBD.ingot → dineutronium
        RECIPES.add(new ExposureChamberRecipe(cs("particle_sparkticle"), cs("ingot_schrabidate"), stack("ingot_dineutronium")));
    }

    public static ExposureChamberRecipe getRecipe(ItemStack particle, ItemStack input) {
        register();
        for (ExposureChamberRecipe recipe : RECIPES) {
            if (recipe.particle.matchesRecipe(particle, true) && recipe.ingredient.matchesRecipe(input, true)) {
                return recipe;
            }
        }
        return null;
    }

    private static ComparableStack cs(String id) {
        return new ComparableStack(item(id));
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i);
    }

    public static final class ExposureChamberRecipe {
        public final AStack particle;
        public final AStack ingredient;
        public final ItemStack output;

        public ExposureChamberRecipe(AStack particle, AStack ingredient, ItemStack output) {
            this.particle = particle;
            this.ingredient = ingredient;
            this.output = output;
        }
    }
}
