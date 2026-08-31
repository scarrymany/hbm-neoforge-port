package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.MixerRecipes;
import com.hbm.inventory.recipes.MixerRecipes.MixerRecipe;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the mixer ({@link MixerRecipes}, keyed by <i>output</i> {@link FluidType}, each
 * key mapping to a competing-recipe array) - up to 2 optional input fluids + 1 optional solid input
 * -&gt; the keyed output fluid amount. The competing-array shape is not itself a JEI problem (per
 * {@code docs/phase5/jei_integration.md}'s table: "JEI is perfectly happy to register multiple
 * distinct recipe instances that share the same output, it just lists them as separate recipe
 * pages") - {@link MixerRecipes#getAllRecipes()} (added by this task) is flattened one array entry
 * at a time into individual {@link Entry} rows below, exactly that many separate JEI recipe pages.
 * The optional-slot handling (any subset of the 2 fluids/1 solid may be absent) is the real
 * per-recipe complexity, not the array flattening.
 */
public class MixerCategory implements IRecipeCategory<MixerCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "mixer", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MixerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("mixer"), 0, 0, 150, 70)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_mixer"));
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<FluidType, MixerRecipe[]> e : MixerRecipes.getAllRecipes().entrySet()) {
            for (MixerRecipe recipe : e.getValue()) {
                list.add(new Entry(e.getKey(), recipe));
            }
        }
        return list;
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.machineMixer");
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
        MixerRecipe r = recipe.recipe();

        if (r.input1 != null) {
            builder.addInputSlot(15, 15)
                    .setStandardSlotBackground()
                    .addItemStack(JeiUtil.fluidIcon(r.input1));
        }
        if (r.input2 != null) {
            builder.addInputSlot(15, 45)
                    .setStandardSlotBackground()
                    .addItemStack(JeiUtil.fluidIcon(r.input2));
        }
        if (r.solidInput != null) {
            builder.addInputSlot(40, 30)
                    .setStandardSlotBackground()
                    .addItemStacks(r.solidInput.extractForJEI());
        }

        builder.addOutputSlot(100, 30)
                .setStandardSlotBackground()
                .addItemStack(JeiUtil.fluidIcon(recipe.outputType(), r.output));
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }

    public record Entry(FluidType outputType, MixerRecipe recipe) {
    }
}
