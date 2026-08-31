package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.recipes.machine.BreederRecipe;
import com.hbm.inventory.recipes.machine.BreederRecipes;
import com.hbm.main.MainRegistry;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/**
 * JEI category for the breeding reactor's rod transmutation table ({@link BreederRecipe} fed
 * through {@link BreederRecipes#BREEDER_TYPE}) - 1 {@code Ingredient} in, 1 {@code ItemStack} out,
 * plus a flux-cost {@code int} JEI has no built-in slot for (per
 * {@code docs/phase5/jei_integration.md}'s table: "same shape as Shredder, plus one drawn text
 * overlay for the flux number - no vanilla category renders that for free, but it's a single
 * {@code guiGraphics.drawString} call"). Data source is real JSON-datapack recipes
 * ({@code data/hbm/recipe/breeder/*.json}, 30 files) - see {@link JeiUtil#vanillaRecipes}'s javadoc
 * for the shared {@code RecipeManager} timing caveat.
 */
public class BreederCategory implements IRecipeCategory<BreederRecipe> {

    public static final RecipeType<BreederRecipe> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "breeder", BreederRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public BreederCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("breeder"), 0, 0, 140, 60)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_reactor_breeding"));
    }

    public static List<BreederRecipe> buildRecipes() {
        return JeiUtil.vanillaRecipes(BreederRecipes.BREEDER_TYPE.get());
    }

    @Override
    public RecipeType<BreederRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.reactorBreeding");
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
    public void setRecipe(IRecipeLayoutBuilder builder, BreederRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(30, 20)
                .setStandardSlotBackground()
                .addItemStacks(Arrays.asList(recipe.getInput().getItems()));

        builder.addOutputSlot(100, 20)
                .setStandardSlotBackground()
                .addItemStack(recipe.getResultItem(null).copy());
    }

    /** Draws the flux cost as a text overlay - no vanilla JEI slot shows a bare numeric requirement, see class javadoc. */
    @Override
    public void draw(BreederRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                Component.literal(recipe.getFlux() + " flux").withStyle(ChatFormatting.GOLD),
                60, 5, 0x404040, false);
    }
}
