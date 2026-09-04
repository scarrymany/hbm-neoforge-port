package com.hbm.inventory.recipes;

import com.hbm.inventory.recipes.chem.ChemPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes.ChemPlantRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import org.jetbrains.annotations.Nullable;

/**
 * CE {@code ChemicalPlantRecipes.INSTANCE} — GenericRecipes view over the already-ported
 * {@link ChemPlantRecipes} table. No new recipe rows; names/duration/power/IO are 1:1.
 */
public final class ChemicalPlantRecipes {

    public static final GenericRecipes INSTANCE = new GenericRecipes();

    private ChemicalPlantRecipes() {
    }

    public static void rebuild() {
        INSTANCE.recipeNameMap.clear();
        INSTANCE.recipeOrderedList.clear();
        for (ChemPlantRecipe recipe : ChemPlantRecipes.RECIPES) {
            GenericRecipe generic = new GenericRecipe(recipe.name)
                    .setDuration(recipe.duration)
                    .setPower(recipe.power)
                    .setNamed();
            if (recipe.inputItems.length > 0) generic.inputItem = recipe.inputItems;
            if (recipe.inputFluids.length > 0) generic.inputFluid = recipe.inputFluids;
            if (recipe.outputItems.length > 0) generic.outputItems(recipe.outputItems);
            if (recipe.outputFluids.length > 0) generic.outputFluids(recipe.outputFluids);
            INSTANCE.recipeNameMap.put(recipe.name, generic);
            INSTANCE.recipeOrderedList.add(generic);
        }
    }

    @Nullable
    public static ChemPlantRecipe byName(String name) {
        if (name == null || name.isEmpty() || "null".equals(name)) return null;
        for (ChemPlantRecipe recipe : ChemPlantRecipes.RECIPES) {
            if (recipe.name.equals(name)) return recipe;
        }
        return null;
    }
}
