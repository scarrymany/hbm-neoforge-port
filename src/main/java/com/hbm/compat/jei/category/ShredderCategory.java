package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.recipes.HbmSimpleRecipe;
import com.hbm.inventory.recipes.ProcessingRecipes;
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

/**
 * JEI category for the shredder ({@link HbmSimpleRecipe} fed through
 * {@link ProcessingRecipes#SHREDDER_TYPE}) - 1 {@code Ingredient} in, 1 {@code ItemStack} out, the
 * simplest shape in this port's whole recipe inventory (per
 * {@code docs/phase5/jei_integration.md}'s per-machine table: "textbook single-in/single-out
 * category, same shape JEI ships examples for"). Data source is real JSON-datapack recipes
 * ({@code data/hbm/recipe/shredder/*.json}, 44 files) fed via {@link JeiUtil#vanillaRecipes} - see
 * that method's own javadoc for the {@link net.minecraft.world.item.crafting.RecipeManager} timing
 * caveat that applies to this category (and {@link AssemblerCategory}/{@link BreederCategory},
 * this port's only 3 vanilla-{@code Recipe<?>}-backed JEI categories).
 *
 * <p>Slot layout/background geometry mirrors neo-edition's real, compiling
 * {@code ShredderRecipeHandler} 1:1 (background crop {@code (5,11,166,65)} of a
 * {@code gui_nei_shredder.png}, input at {@code (38,23)}, output at {@code (128,23)}) - that file's
 * numbers are themselves derived from CE's real shipped asset, not invented, so reusing them here
 * means this category's layout will already be correct the moment the sibling GUI-asset-porting
 * task lands the real {@code textures/gui/jei/gui_nei_shredder.png}. This port's own blade-slot
 * decoration (CE/neo-edition both draw 2 rotating "blade" ingredient slots either side of the
 * shredder's drum) is intentionally NOT reproduced - this port has not ported a blade-item/upgrade
 * system for the shredder (grepped, no equivalent), so those 2 slots are simply omitted rather than
 * populated with nothing.
 *
 * <p>{@link HbmSimpleRecipe#getResultItem(net.minecraft.core.HolderLookup.Provider)} is called with
 * a {@code null} registries argument below - safe because that method's real implementation
 * ({@code return this.output;}) never dereferences its parameter (confirmed by reading
 * {@code HbmSimpleRecipe.java} in full); this is the only public accessor that class exposes for
 * its output field (a {@code protected} field, inaccessible from this package).
 */
public class ShredderCategory implements IRecipeCategory<HbmSimpleRecipe> {

    public static final RecipeType<HbmSimpleRecipe> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "shredder", HbmSimpleRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public ShredderCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("shredder"), 5, 11, 166, 65)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_shredder"));
    }

    public static java.util.List<HbmSimpleRecipe> buildRecipes() {
        return JeiUtil.vanillaRecipes(ProcessingRecipes.SHREDDER_TYPE.get());
    }

    @Override
    public RecipeType<HbmSimpleRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.machineShredder");
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
    public void setRecipe(IRecipeLayoutBuilder builder, HbmSimpleRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(38, 23)
                .setStandardSlotBackground()
                .addItemStacks(java.util.Arrays.asList(recipe.getInput().getItems()));

        builder.addOutputSlot(128, 23)
                .setStandardSlotBackground()
                .addItemStack(recipe.getResultItem(null).copy());
    }

    @Override
    public void draw(HbmSimpleRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }
}
