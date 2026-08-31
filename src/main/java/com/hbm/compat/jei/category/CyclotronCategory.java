package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.chem.CyclotronRecipes;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple.Pair;

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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the cyclotron ({@link CyclotronRecipes}, this port's own bespoke shape) - 2
 * distinct inputs (a catalyst {@link ComparableStack} + a target {@link AStack}) -&gt; 1
 * {@code ItemStack} out + an antimatter-mB yield {@code int} (a non-item numeric side-output, per
 * {@code docs/phase5/jei_integration.md}'s table: "2 side-by-side input slots + 1 output slot + a
 * drawn antimatter-yield number - no vanilla-shaped category has a slot for a bare int"). CE's own
 * real {@code JeiRecipes.CyclotronRecipe} ({@code JeiRecipes.java:64-81}) confirms this exact 2-in/
 * 1-out layout (cited for layout only, per this project's CE-citation rules - not for numbers,
 * which come from this port's own already-ported {@link CyclotronRecipes} data).
 */
public class CyclotronCategory implements IRecipeCategory<CyclotronCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "cyclotron", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CyclotronCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("cyclotron"), 0, 0, 140, 60)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_cyclotron"));
    }

    public static List<Entry> buildRecipes() {
        CyclotronRecipes.register();
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<Pair<ComparableStack, AStack>, Pair<ItemStack, Integer>> e : CyclotronRecipes.RECIPES.entrySet()) {
            list.add(new Entry(e.getKey().getKey(), e.getKey().getValue(), e.getValue().getKey(), e.getValue().getValue()));
        }
        return list;
    }

    @Override
    public RecipeType<Entry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.cyclotron");
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
                .addItemStacks(recipe.catalyst().extractForJEI());

        builder.addInputSlot(40, 20)
                .setStandardSlotBackground()
                .addItemStacks(recipe.target().extractForJEI());

        builder.addOutputSlot(100, 20)
                .setStandardSlotBackground()
                .addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(Entry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                Component.literal(recipe.antimatterMb() + "mB AM").withStyle(ChatFormatting.DARK_PURPLE),
                65, 5, 0x404040, false);
    }

    public record Entry(ComparableStack catalyst, AStack target, ItemStack output, int antimatterMb) {
    }
}
