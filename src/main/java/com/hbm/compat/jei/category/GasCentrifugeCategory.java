package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes;
import com.hbm.inventory.recipes.chem.GasCentrifugeRecipes.PseudoFluidType;
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
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * JEI category for the gas centrifuge's real isotope-enrichment cascade
 * ({@link GasCentrifugeRecipes} - not the item {@link CentrifugeCategory}, a different real CE
 * machine, see that class's own naming-distinction note). Per
 * {@code docs/phase5/jei_integration.md}'s table, this data is "not item/output-keyed at all - a
 * stateful isotope-enrichment cascade": a real feed {@link FluidType} converts 1:1 into a
 * {@link PseudoFluidType}, which then decays through a fixed chain
 * ({@code NUF6→LEUF6→MEUF6→HEUF6}, {@code PF6} terminal, {@code MUD→MUD_HEAVY}), each stage
 * consuming/producing an internal fluid-charge amount and optionally dropping byproduct items,
 * gated by an {@code isHighSpeed} flag on the terminal enrichment stage.
 *
 * <p><b>Synthetic enumerator</b> (the genuinely new glue code this category needed, per that
 * report's "Blocked" section - not an external blocker, just real new code, same as CE's own real
 * plugin needed: {@code JeiRecipes.GasCentrifugeRecipe}, {@code JeiRecipes.java:104-141}):
 * {@link #buildRecipes()} flattens the cascade into one synthetic {@link Stage} row per transition -
 * one "real feed fluid converts into the starting pseudo-fluid" row per
 * {@link GasCentrifugeRecipes#FLUID_CONVERSIONS} entry, then one row per link in that pseudo-fluid's
 * decay chain (walking {@link PseudoFluidType#getOutputType()} until the {@code NONE} terminal).
 *
 * <p>Since a {@link PseudoFluidType} is a wholly synthetic, in-memory-only concept with no backing
 * {@link FluidType}/item (see that class's own javadoc), a cascade-stage row has no real ingredient
 * to put in a fluid-icon slot the way every other fluid-bearing category in this package can - only
 * the very first row (the real feed conversion) gets an input {@code ItemFluidIcon} slot; every
 * cascade-stage row instead draws its pseudo-fluid names/consumed-produced numbers/high-speed flag
 * as text in {@link #draw}, matching CE's own real plugin's custom high-speed icon overlay
 * (cited for "this needs a custom text/icon overlay, not a plain slot" only, not copied).
 */
public class GasCentrifugeCategory implements IRecipeCategory<GasCentrifugeCategory.Stage> {

    public static final RecipeType<Stage> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "gas_centrifuge", Stage.class);

    private final IDrawable background;
    private final IDrawable icon;

    public GasCentrifugeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("gascentrifuge"), 0, 0, 150, 70)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmBlockItem("machine_gascent"));
    }

    public static List<Stage> buildRecipes() {
        GasCentrifugeRecipes.register();
        List<Stage> list = new ArrayList<>();

        for (Map.Entry<FluidType, PseudoFluidType> e : GasCentrifugeRecipes.FLUID_CONVERSIONS.entrySet()) {
            FluidType feed = e.getKey();
            PseudoFluidType start = e.getValue();

            // Row 0: real feed fluid -> starting pseudo-fluid, 1:1, instantaneous - not a "cascade
            // stage" with its own consumed/produced numbers, just the entry point into the chain.
            list.add(new Stage(feed, null, start, 0, 0, new ItemStack[0], false));

            // Walk the decay chain until the NONE terminal (safety-capped - see class javadoc, the
            // real data is a short fixed chain, this guards only against a future data mistake
            // introducing a cycle).
            PseudoFluidType current = start;
            for (int guard = 0; guard < 16 && current != null && current != PseudoFluidType.NONE; guard++) {
                PseudoFluidType output = current.getOutputType();
                ItemStack[] items = current.getOutput();
                list.add(new Stage(null, current, output,
                        current.getFluidConsumed(), current.getFluidProduced(),
                        items == null ? new ItemStack[0] : items, current.getIfHighSpeed()));
                current = output;
            }
        }
        return list;
    }

    @Override
    public RecipeType<Stage> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("container.gasCentrifuge");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Stage stage, IFocusGroup focuses) {
        if (stage.realFeed() != null) {
            builder.addInputSlot(15, 30)
                    .setStandardSlotBackground()
                    .addItemStack(JeiUtil.fluidIcon(stage.realFeed(), 1000));
        }

        ItemStack[] items = stage.items();
        for (int i = 0; i < items.length && i < 3; i++) {
            if (items[i] == null || items[i].isEmpty()) continue;
            builder.addOutputSlot(90 + i * 18, 30)
                    .setStandardSlotBackground()
                    .addItemStack(items[i].copy());
        }
    }

    @Override
    public void draw(Stage stage, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = net.minecraft.client.Minecraft.getInstance().font;

        String from = stage.inputPseudo() != null ? stage.inputPseudo().name : stage.realFeed().getName();
        String to = stage.outputPseudo() != null ? stage.outputPseudo().name : "?";

        guiGraphics.drawString(font, Component.literal(from + " -> " + to), 5, 5, 0x404040, false);

        if (stage.inputPseudo() != null) {
            guiGraphics.drawString(font,
                    Component.literal(stage.consumed() + "/" + stage.produced() + " charge").withStyle(ChatFormatting.GRAY),
                    5, 15, 0x404040, false);
        }
        if (stage.highSpeed()) {
            guiGraphics.drawString(font,
                    Component.literal("High-Speed only").withStyle(ChatFormatting.RED),
                    5, 25, 0x404040, false);
        }
    }

    /**
     * One cascade transition. {@code realFeed} is set only for the synthetic "feed conversion" row
     * ({@code inputPseudo}/{@code outputPseudo} otherwise carry the transition); {@code items} is
     * this stage's byproduct drop (may be empty).
     */
    public record Stage(FluidType realFeed, PseudoFluidType inputPseudo, PseudoFluidType outputPseudo,
                         int consumed, int produced, ItemStack[] items, boolean highSpeed) {
    }
}
