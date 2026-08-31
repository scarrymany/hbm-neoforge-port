package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.recipes.AssemblerRecipe;
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

import java.util.Arrays;
import java.util.List;

/**
 * JEI category for the assembler ({@link AssemblerRecipe} fed through
 * {@link ProcessingRecipes#ASSEMBLER_TYPE}) - up to 12 unordered {@code (Ingredient, count)} inputs
 * in, 1 {@code ItemStack} out (per {@code docs/phase5/jei_integration.md}'s table: "12-slot input
 * grid (a loop, not per-slot code) + 1 output slot"). Data source is real JSON-datapack recipes
 * ({@code data/hbm/recipe/assembler/*.json}, 13 files) - see {@link JeiUtil#vanillaRecipes}'s own
 * javadoc for the {@code RecipeManager} timing caveat shared by this port's 3 vanilla-{@code
 * Recipe<?>}-backed categories.
 *
 * <p>12-slot grid coordinates copied from neo-edition's real, compiling
 * {@code AssemblyMachineRecipeHandler.INPUT_COORDS} (a 4x3 grid at {@code x∈{12,30,48,66}, y∈{6,24,
 * 42}}) - that shape (not the fluid slots that class also draws) is the part directly applicable
 * here, since this port's {@link AssemblerRecipe} deliberately has no fluid input/output (see that
 * class's own javadoc's "Deliberately not the full GenericRecipe shape" section) - only the item
 * grid + 1 output slot are drawn.
 *
 * <p>A recipe's {@link AssemblerRecipe.Entry#count()} (how many of that ingredient the recipe
 * needs) has no dedicated JEI slot-quantity rendering hook in this shape (JEI shows "which items
 * satisfy this slot", not "how many" beyond the stack's own {@code ItemStack} count) - each input
 * {@code ItemStack} shown is built with its real {@link AssemblerRecipe.Entry#count()} baked into
 * the stack size, matching how a player would actually need to supply it.
 */
public class AssemblerCategory implements IRecipeCategory<AssemblerRecipe> {

    public static final RecipeType<AssemblerRecipe> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "assembler", AssemblerRecipe.class);

    private static final int[][] INPUT_COORDS = {
            {12, 6}, {30, 6}, {48, 6}, {66, 6},
            {12, 24}, {30, 24}, {48, 24}, {66, 24},
            {12, 42}, {30, 42}, {48, 42}, {66, 42}
    };

    private final IDrawable background;
    private final IDrawable icon;

    public AssemblerCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("assembler"), 5, 11, 166, 65)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_assembly_machine"));
    }

    public static List<AssemblerRecipe> buildRecipes() {
        return JeiUtil.vanillaRecipes(ProcessingRecipes.ASSEMBLER_TYPE.get());
    }

    @Override
    public RecipeType<AssemblerRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.assemblyMachine");
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
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblerRecipe recipe, IFocusGroup focuses) {
        List<AssemblerRecipe.Entry> entries = recipe.getInputEntries();
        int limit = Math.min(entries.size(), INPUT_COORDS.length);

        for (int i = 0; i < limit; i++) {
            AssemblerRecipe.Entry entry = entries.get(i);
            List<net.minecraft.world.item.ItemStack> stacks = Arrays.stream(entry.ingredient().getItems())
                    .map(s -> {
                        net.minecraft.world.item.ItemStack copy = s.copy();
                        copy.setCount(Math.max(1, entry.count()));
                        return copy;
                    })
                    .toList();

            builder.addInputSlot(INPUT_COORDS[i][0], INPUT_COORDS[i][1])
                    .setStandardSlotBackground()
                    .addItemStacks(stacks);
        }

        builder.addOutputSlot(134, 24)
                .setStandardSlotBackground()
                .addItemStack(recipe.getResultItem(null).copy());
    }

    @Override
    public void draw(AssemblerRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }
}
