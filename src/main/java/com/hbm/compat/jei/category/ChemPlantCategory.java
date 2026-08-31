package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes;
import com.hbm.inventory.recipes.chem.ChemPlantRecipes.ChemPlantRecipe;
import com.hbm.main.MainRegistry;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * JEI category for the chemical plant ({@link ChemPlantRecipe}, this port's own bespoke shape - up
 * to 3 {@link AStack} item inputs, up to 2 {@link FluidStack} fluid inputs, up to 3 item outputs, 1
 * fluid output). The most I/O-diverse shape in this port's whole recipe inventory (per
 * {@code docs/phase5/jei_integration.md}'s table) - directly adaptable from neo-edition's real,
 * compiling {@code ChemicalPlantRecipeHandler}, which implements exactly this shape (loop-based
 * item/fluid slot placement) against its own structurally near-identical {@code GenericRecipe}.
 * {@link ChemPlantRecipes#RECIPES} is already {@code public}, no wrapper needed.
 */
public class ChemPlantCategory implements IRecipeCategory<ChemPlantRecipe> {

    public static final RecipeType<ChemPlantRecipe> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "chemical_plant", ChemPlantRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ChemPlantCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("chemplant"), 0, 0, 176, 141)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_chemical_plant"));
    }

    public static List<ChemPlantRecipe> buildRecipes() {
        ChemPlantRecipes.register();
        return ChemPlantRecipes.RECIPES;
    }

    @Override
    public RecipeType<ChemPlantRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.machineChemicalPlant");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ChemPlantRecipe recipe, IFocusGroup focuses) {
        if (recipe.inputItems != null) {
            for (int i = 0; i < Math.min(recipe.inputItems.length, 3); i++) {
                builder.addInputSlot(8 + i * 18, 99)
                        .setStandardSlotBackground()
                        .addItemStacks(recipe.inputItems[i].extractForJEI());
            }
        }

        if (recipe.inputFluids != null) {
            for (int i = 0; i < Math.min(recipe.inputFluids.length, 2); i++) {
                builder.addInputSlot(8 + i * 18, 54)
                        .setStandardSlotBackground()
                        .addItemStack(JeiUtil.fluidIcon(recipe.inputFluids[i]));
            }
        }

        if (recipe.outputItems != null) {
            for (int i = 0; i < Math.min(recipe.outputItems.length, 3); i++) {
                if (recipe.outputItems[i] == null || recipe.outputItems[i].isEmpty()) continue;
                builder.addOutputSlot(80 + i * 18, 99)
                        .setOutputSlotBackground()
                        .addItemStack(recipe.outputItems[i].copy());
            }
        }

        if (recipe.outputFluid != null) {
            builder.addOutputSlot(80, 54)
                    .setOutputSlotBackground()
                    .addItemStack(JeiUtil.fluidIcon(recipe.outputFluid));
        }
    }

    @Override
    public void draw(ChemPlantRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }
}
