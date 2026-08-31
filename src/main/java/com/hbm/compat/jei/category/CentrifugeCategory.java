package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.recipes.chem.CentrifugeRecipes;
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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the (item) centrifuge - ore/crystal washing, {@link CentrifugeRecipes}'s flat
 * {@code Map<AStack, ItemStack[]>} (up to 4 outputs, real CE quantities). Directly adaptable from
 * neo-edition's real, compiling {@code CentrifugeRecipeHandler} - same 1-in/up-to-4-out shape (per
 * {@code docs/phase5/jei_integration.md}'s table).
 *
 * <p>Not to be confused with {@link GasCentrifugeCategory} (the real isotope-separation cascade
 * machine) - see {@link CentrifugeRecipes}'s own class javadoc for the CE naming distinction this
 * port preserves.
 */
public class CentrifugeCategory implements IRecipeCategory<CentrifugeCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "centrifuge", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CentrifugeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("centrifuge"), 0, 0, 176, 86)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_centrifuge"));
    }

    public static List<Entry> buildRecipes() {
        CentrifugeRecipes.register();
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<AStack, ItemStack[]> e : CentrifugeRecipes.RECIPES.entrySet()) {
            list.add(new Entry(e.getKey(), e.getValue()));
        }
        return list;
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.centrifuge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Entry recipe, IFocusGroup focuses) {
        builder.addInputSlot(36, 50)
                .setStandardSlotBackground()
                .addItemStacks(recipe.input().extractForJEI());

        ItemStack[] outputs = recipe.outputs();
        for (int i = 0; i < outputs.length && i < 4; i++) {
            if (outputs[i] == null || outputs[i].isEmpty()) continue;
            builder.addOutputSlot(63 + i * 20, 50)
                    .setStandardSlotBackground()
                    .addItemStack(outputs[i].copy());
        }
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }

    public record Entry(AStack input, ItemStack[] outputs) {
    }
}
