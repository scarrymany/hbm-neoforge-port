package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes;
import com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes.ElectrolysisRecipe;
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
 * JEI category for the electrolyser's fluid-electrolysis half ({@link ElectrolyserFluidRecipes}, a
 * bespoke {@code Map<FluidType, ElectrolysisRecipe>}) - 1 required input fluid (a full
 * {@code amount}, not per-tick) -&gt; 2 output fluids + optional item byproducts (per
 * {@code docs/phase5/jei_integration.md}'s table: "1 ItemFluidIcon input slot + 2 ItemFluidIcon
 * output slots + up to N byproduct item slots - fixed slot count, no vanilla shape fits an
 * all-fluid layout"). The ore/crystal electrolysis half ({@code ElectrolyserMetalRecipes}) is not
 * ported in this port yet ({@link ElectrolyserFluidRecipes}'s own javadoc, lines 18-22) - nothing to
 * build a second category for there.
 */
public class ElectrolyserCategory implements IRecipeCategory<ElectrolyserCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "electrolyser", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ElectrolyserCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("electrolyser"), 0, 0, 150, 70)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_electrolyser"));
    }

    public static List<Entry> buildRecipes() {
        ElectrolyserFluidRecipes.register();
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<FluidType, ElectrolysisRecipe> e : ElectrolyserFluidRecipes.RECIPES.entrySet()) {
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
        return Component.translatable("container.machineElectrolyser");
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
        builder.addInputSlot(15, 30)
                .setStandardSlotBackground()
                .addItemStack(JeiUtil.fluidIcon(recipe.input(), recipe.recipe().amount));

        builder.addOutputSlot(80, 15)
                .setStandardSlotBackground()
                .addItemStack(JeiUtil.fluidIcon(recipe.recipe().output1));

        builder.addOutputSlot(80, 45)
                .setStandardSlotBackground()
                .addItemStack(JeiUtil.fluidIcon(recipe.recipe().output2));

        ItemStack[] byproduct = recipe.recipe().byproduct;
        if (byproduct != null) {
            for (int i = 0; i < byproduct.length && i < 2; i++) {
                if (byproduct[i] == null || byproduct[i].isEmpty()) continue;
                builder.addOutputSlot(115, 15 + i * 30)
                        .setStandardSlotBackground()
                        .addItemStack(byproduct[i].copy());
            }
        }
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }

    public record Entry(FluidType input, ElectrolysisRecipe recipe) {
    }
}
