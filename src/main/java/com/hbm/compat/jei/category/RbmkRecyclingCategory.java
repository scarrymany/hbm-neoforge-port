package com.hbm.compat.jei.category;

import com.hbm.compat.jei.JeiUtil;
import com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes;
import com.hbm.items.machine.rbmk.RBMKRods;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for RBMK spent-fuel-rod recycling ({@link RBMKFuelRecipes#getRecyclingOutput}) -
 * per {@code docs/phase5/jei_integration.md}'s table, this was flagged as the single hardest
 * category in this port's inventory: {@link RBMKFuelRecipes} is deliberately "not a keyed table at
 * all" ({@code getRecyclingOutput}/{@code computeStage} are pure functions over a <i>live</i> rod
 * stack's current data-component state - see that class's own javadoc for why a static NBT-keyed
 * table would never match a post-burn stack), so there is no collection to hand
 * {@code registration.addRecipes(...)} directly.
 *
 * <p><b>Synthetic enumerator</b> (the new glue code the research report predicted this category
 * would need): {@link #buildRecipes()} builds one representative fresh, unburned example
 * {@link ItemStack} per registered {@link com.hbm.items.machine.ItemRBMKRod} (every
 * {@code RBMKRods} field except the debug-only {@code TEST}/{@code rbmk_fuel_test}, which CE itself
 * never wired into its own recycling table either - {@code RBMKRods}' own javadoc documents this
 * exact CE quirk, preserved here by the same omission) and pairs it with
 * {@link RBMKFuelRecipes#getRecyclingOutput}'s real result.
 *
 * <p><b>Simpler than the research report anticipated, once read against this port's actual code</b>:
 * that report worried a synthetic enumerator would need to cover CE's real 0-9
 * {@link RBMKFuelRecipes#computeStage} depletion/xenon-poison buckets (CE's own real
 * {@code RBMKFuelRecipes.addRod} generated 5 enrichment buckets x 2 xenon states per rod, purely for
 * JEI display). Reading {@link RBMKFuelRecipes#getRecyclingOutput} in full shows this port's actual
 * ported implementation does not branch on {@code computeStage} at all - it returns a fixed
 * {@code 8x} the rod's paired pellet regardless of the input stack's live enrichment/poison state
 * (the pellet's own stage-variant rendering, if any, is a concern of
 * {@code com.hbm.items.machine.ItemRBMKPellet}, not of which pellet type the recycler returns). One
 * representative example stack per rod is therefore already a complete, non-lossy JEI
 * representation of this port's actual live conversion function, not a simplification of it - worth
 * flagging for whoever next touches {@link RBMKFuelRecipes} in case that flattening was accidental
 * rather than intended (see this task's own structured-output notes).
 *
 * <p><b>No JEI catalyst registered</b> (deliberately, not an oversight): grepping this whole port for
 * {@code getRecyclingOutput} shows {@link RBMKFuelRecipes} has zero real consumers today - no RBMK
 * fuel-reprocessing machine block/menu/block-entity exists in this port yet. This category still
 * registers (so the recipe data itself is inspectable in JEI once a rod's own item entry is looked
 * up), it just has no machine item to bind as a catalyst - see this task's notes for the follow-up
 * once that machine lands.
 */
public class RbmkRecyclingCategory implements IRecipeCategory<RbmkRecyclingCategory.Recycling> {

    public static final RecipeType<Recycling> RECIPE_TYPE =
            RecipeType.create(MainRegistry.MODID, "rbmk_recycling", Recycling.class);

    private final IDrawable background;
    private final IDrawable icon;

    public RbmkRecyclingCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.drawableBuilder(JeiUtil.jeiTexture("rbmk_recycling"), 0, 0, 120, 50)
                .setTextureSize(256, 256).build();
        this.icon = guiHelper.createDrawableItemLike(JeiUtil.hbmItem("rbmk_fuel_ueu"));
    }

    public static List<Recycling> buildRecipes() {
        List<DeferredItem<Item>> rods = List.of(
                RBMKRods.UEU, RBMKRods.MEU, RBMKRods.HEU233, RBMKRods.HEU235, RBMKRods.UZH, RBMKRods.THMEU,
                RBMKRods.LEP, RBMKRods.MEP, RBMKRods.HEP239, RBMKRods.HEP241,
                RBMKRods.LEA, RBMKRods.MEA, RBMKRods.HEA241, RBMKRods.HEA242,
                RBMKRods.MEN, RBMKRods.HEN, RBMKRods.MOX,
                RBMKRods.LES, RBMKRods.MES, RBMKRods.HES,
                RBMKRods.LEAUS, RBMKRods.HEAUS,
                RBMKRods.RA226BE, RBMKRods.PO210BE, RBMKRods.PU238BE,
                RBMKRods.BALEFIRE_GOLD, RBMKRods.FLASHLEAD,
                RBMKRods.ZFB_BISMUTH, RBMKRods.ZFB_PU241, RBMKRods.ZFB_AM_MIX,
                RBMKRods.BALEFIRE, RBMKRods.DRX
                // RBMKRods.TEST deliberately excluded - debug-only, no pellet, CE itself never wired
                // it into recycling either (see class javadoc).
        );

        List<Recycling> list = new ArrayList<>();
        for (DeferredItem<Item> rod : rods) {
            ItemStack input = new ItemStack(rod.get());
            ItemStack output = RBMKFuelRecipes.getRecyclingOutput(input);
            if (!output.isEmpty()) list.add(new Recycling(input, output));
        }
        return list;
    }

    @Override
    public RecipeType<Recycling> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.hbm.rbmk_recycling");
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
    public void setRecipe(IRecipeLayoutBuilder builder, Recycling recipe, IFocusGroup focuses) {
        builder.addInputSlot(15, 15)
                .setStandardSlotBackground()
                .addItemStack(recipe.input().copy());

        builder.addOutputSlot(85, 15)
                .setStandardSlotBackground()
                .addItemStack(recipe.output().copy());
    }

    @Override
    public void draw(Recycling recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
    }

    public record Recycling(ItemStack input, ItemStack output) {
    }
}
