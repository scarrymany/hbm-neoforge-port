package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.RefineryRecipes;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple;

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
 * JEI category for the refinery ({@link RefineryRecipes}, keyed by <i>input</i> {@link FluidType})
 * - 1 input fluid -&gt; up to 4 fixed-percentage output fluids + 1 optional item byproduct. Directly
 * adapted from neo-edition's real, compiling {@code RefineryRecipeHandler}, which solves this exact
 * shape against its own structurally identical {@code RefineryRecipes.getRecipes()} map (per
 * {@code docs/phase5/jei_integration.md}'s table: "near-directly adaptable... swapping
 * {@code FluidIconItem.make} for this port's own {@code ItemFluidIcon.make}") -
 * {@link RefineryRecipes#getAllRefinery()} (added by this task) supplies the data.
 *
 * <p>The x10 output-amount scaling in {@link #setRecipe} is carried over from neo-edition's own
 * real {@code RefineryRecipeHandler.RefineryRecipe#output1Scaled()} et al. - both ports store their
 * refinery fractions as small percent-style numbers ({@link RefineryRecipes#OIL_FRAC_HEAVY} = 50,
 * etc; this port's own javadoc calls them "fractions in percent"), and neo-edition already worked
 * out that displaying the raw fraction reads as a confusingly tiny mB amount in a JEI fluid-slot
 * tooltip - this is a display-only convention (does not change this port's own live refinery output
 * math, which reads {@link RefineryRecipes#getRefinery} directly, unscaled) carried over because it
 * addresses the same data shape, not copied blindly.</p>
 */
public class RefineryCategory implements IRecipeCategory<RefineryCategory.Entry> {

    public static final RecipeType<Entry> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "refinery", Entry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public RefineryCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("refinery"), 0, 0, 174, 84)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_refinery"));
    }

    public static List<Entry> buildRecipes() {
        List<Entry> list = new ArrayList<>();
        for (Map.Entry<FluidType, Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack>> e : RefineryRecipes.getAllRefinery().entrySet()) {
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
        return Component.translatable("container.machineRefinery");
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
        Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack> outputs = entry.outputs();

        builder.addInputSlot(52, 34)
                .setStandardSlotBackground()
                .addItemStack(JeiUtil.fluidIcon(entry.input(), 1000));

        builder.addOutputSlot(115, 16).setStandardSlotBackground().addItemStack(JeiUtil.fluidIcon(scaled(outputs.getX())));
        builder.addOutputSlot(133, 25).setStandardSlotBackground().addItemStack(JeiUtil.fluidIcon(scaled(outputs.getY())));
        builder.addOutputSlot(115, 34).setStandardSlotBackground().addItemStack(JeiUtil.fluidIcon(scaled(outputs.getV())));
        builder.addOutputSlot(133, 43).setStandardSlotBackground().addItemStack(JeiUtil.fluidIcon(scaled(outputs.getW())));

        ItemStack byproduct = outputs.getZ();
        if (byproduct != null && !byproduct.isEmpty()) {
            builder.addOutputSlot(115, 52)
                    .setStandardSlotBackground()
                    .addItemStack(byproduct.copy());
        }
    }

    private static FluidStack scaled(FluidStack stack) {
        if (stack == null) return null;
        return new FluidStack(stack.type, stack.fill * 10, stack.pressure);
    }

    @Override
    public void draw(Entry entry, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }

    public record Entry(FluidType input, Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack> outputs) {
    }
}
