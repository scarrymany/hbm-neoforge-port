package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.recipes.chem.SILEXRecipes;
import com.hbm.inventory.recipes.chem.SILEXRecipes.SILEXRecipe;
import com.hbm.main.MainRegistry;
import com.hbm.util.WeightedRandomObject;

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
 * JEI category for SILEX laser isotope/element separation ({@link SILEXRecipes}, a bespoke
 * {@code Map<ComparableStack, SILEXRecipe>}) - 1 item in -&gt; a <b>weighted-random</b> pool of
 * possible outputs, gated by a minimum laser wavelength, plus a material-charge produced/consumed
 * pair (per {@code docs/phase5/jei_integration.md}'s table). JEI's per-slot {@code addItemStacks}
 * already natively cycles a list of alternative outputs in one slot - CE's real weight skew (e.g.
 * 11:1 U238:U235, see {@link SILEXRecipes}'s own registered data) is cosmetically flattened to
 * equal-likelihood cycling in this JEI display only, never in the live drop table
 * ({@code SilexBlockEntity} still reads {@link SILEXRecipe#outputs}' real weights directly).
 *
 * <p><b>Open question, not resolved here</b> (see the research report's own "Open questions" #1):
 * CE's real plugin splits this into 3 separate categories by wavelength tier
 * ({@code SILEXVisibleRecipeHandler}/{@code SILEXIrRecipeHandler}/{@code SILEXGammaRecipeHandler}).
 * This port instead uses <b>one</b> category with the wavelength requirement drawn as a text label
 * per recipe row - a display-only simplification, not a behavior deviation (every recipe's real
 * minimum-wavelength gate is unchanged), chosen to keep this task's first pass at 13 working
 * categories rather than 15; splitting into CE's exact 3-way layout later is a pure JEI-registration
 * change if a future pass wants CE's exact visual split.
 */
public class SilexCategory implements IRecipeCategory<SilexCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "silex", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public SilexCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("silex"), 0, 0, 150, 70)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_silex"));
    }

    public static List<Entry> buildRecipes() {
        SILEXRecipes.register();
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<ComparableStack, SILEXRecipe> e : SILEXRecipes.RECIPES.entrySet()) {
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
        return Component.translatable("container.machineSILEX");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
        builder.addInputSlot(20, 30)
                .setStandardSlotBackground()
                .addItemStacks(entry.input().extractForJEI());

        List<ItemStack> outputs = new ArrayList<>();
        for (WeightedRandomObject weighted : entry.recipe().outputs) {
            ItemStack stack = weighted.asStack();
            if (stack != null && !stack.isEmpty()) outputs.add(stack);
        }

        builder.addOutputSlot(100, 30)
                .setStandardSlotBackground()
                .addItemStacks(outputs);
    }

    @Override
    public void draw(Entry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        SILEXRecipe recipe = entry.recipe();
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                Component.literal(recipe.laserStrength.name() + " min").withStyle(ChatFormatting.LIGHT_PURPLE),
                55, 5, 0x404040, false);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                Component.literal(recipe.fluidConsumed + "/" + recipe.fluidProduced + " charge").withStyle(ChatFormatting.GRAY),
                55, 15, 0x404040, false);
    }

    public record Entry(ComparableStack input, SILEXRecipe recipe) {
    }
}
