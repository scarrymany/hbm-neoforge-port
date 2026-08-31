package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.CrystallizerRecipes;
import com.hbm.inventory.recipes.CrystallizerRecipes.CrystallizerRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the crystallizer ({@link CrystallizerRecipes}, a bespoke
 * {@code Map<Pair<ComparableStack, FluidType>, CrystallizerRecipe>}, not a vanilla
 * {@code Recipe<?>} - see that class's own javadoc for why). 1 item input + 1 required acid/reagent
 * fluid input + 1 item output + a {@code productivity} free-output chance (per
 * {@code docs/phase5/jei_integration.md}'s table: "1 item input slot + 1 ItemFluidIcon acid slot +
 * 1 output slot + a productivity-% tooltip/label").
 *
 * <p>{@link CrystallizerRecipes#getAllRecipes()} (added by this task, see that method's own
 * javadoc) supplies the full recipe set; this category wraps each {@code Map.Entry} in a small
 * {@link Entry} record since {@link CrystallizerRecipe} itself does not carry its own input
 * item/acid-type key.
 */
public class CrystallizerCategory implements IRecipeCategory<CrystallizerCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "crystallizer", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CrystallizerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("crystallizer"), 0, 0, 140, 60)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_crystallizer"));
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Tuple.Pair<ComparableStack, FluidType>, CrystallizerRecipe> e : CrystallizerRecipes.getAllRecipes().entrySet()) {
            list.add(new Entry(e.getKey().getKey(), e.getKey().getValue(), e.getValue()));
        }
        return list;
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.crystallizer");
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
        builder.addInputSlot(15, 20)
                .setStandardSlotBackground()
                .addItemStacks(recipe.input().extractForJEI());

        builder.addInputSlot(40, 20)
                .setStandardSlotBackground()
                .addItemStack(JeiUtil.fluidIcon(recipe.acidType(), recipe.recipe().acidAmount));

        builder.addOutputSlot(100, 20)
                .setStandardSlotBackground()
                .addItemStack(recipe.recipe().output.copy());
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        int pct = Math.round(recipe.recipe().productivity * 100F);
        if (pct > 0) {
            guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    Component.literal("+" + pct + "%").withStyle(ChatFormatting.GREEN),
                    65, 5, 0x404040, false);
        }
    }

    public record Entry(ComparableStack input, FluidType acidType, CrystallizerRecipe recipe) {
    }
}
