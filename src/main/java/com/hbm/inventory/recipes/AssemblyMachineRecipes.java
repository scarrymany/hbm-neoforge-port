package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.inventory.recipes.loader.GenericRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CE {@code AssemblyMachineRecipes.INSTANCE} — GenericRecipes view over JSON
 * {@link AssemblerRecipe} holders. Selection key is {@code RecipeHolder#id()}
 * ({@code hbm:<path>}), not invented {@code ass.*} names.
 */
public final class AssemblyMachineRecipes {

    public static final GenericRecipes INSTANCE = new GenericRecipes();

    private AssemblyMachineRecipes() {
    }

    public static void rebuild(RecipeManager manager) {
        INSTANCE.recipeNameMap.clear();
        INSTANCE.recipeOrderedList.clear();
        List<RecipeHolder<AssemblerRecipe>> holders =
                new ArrayList<>(manager.getAllRecipesFor(ProcessingRecipes.ASSEMBLER_TYPE.get()));
        holders.sort(Comparator.comparing(holder -> holder.id().toString()));
        for (RecipeHolder<AssemblerRecipe> holder : holders) {
            GenericRecipe generic = wrap(holder);
            INSTANCE.recipeNameMap.put(generic.getInternalName(), generic);
            INSTANCE.recipeOrderedList.add(generic);
        }
    }

    @Nullable
    public static AssemblerRecipe byName(Level level, String name) {
        if (level == null || name == null || name.isEmpty() || "null".equals(name)) return null;
        for (RecipeHolder<AssemblerRecipe> holder : level.getRecipeManager()
                .getAllRecipesFor(ProcessingRecipes.ASSEMBLER_TYPE.get())) {
            if (holder.id().toString().equals(name)) return holder.value();
        }
        return null;
    }

    private static GenericRecipe wrap(RecipeHolder<AssemblerRecipe> holder) {
        AssemblerRecipe recipe = holder.value();
        GenericRecipe generic = new GenericRecipe(holder.id().toString())
                .setDuration(recipe.getDuration())
                .setPower(recipe.getPower());
        List<AStack> items = new ArrayList<>();
        for (AssemblerRecipe.Entry entry : recipe.getInputEntries()) {
            ItemStack[] options = entry.ingredient().getItems();
            if (options.length == 0) continue;
            items.add(new ComparableStack(options[0].getItem(), entry.count()));
        }
        if (!items.isEmpty()) generic.inputItem = items.toArray(AStack[]::new);
        if (!recipe.getInputFluids().isEmpty()) {
            generic.inputFluid = recipe.getInputFluids().toArray(FluidStack[]::new);
        }
        ItemStack output = recipe.getResultItem(null);
        if (!output.isEmpty()) {
            generic.setIcon(output.copy());
            generic.outputItems(output.copy());
        }
        if (!recipe.getOutputFluids().isEmpty()) {
            generic.outputFluids(recipe.getOutputFluids().toArray(FluidStack[]::new));
        }
        return generic;
    }
}
