package com.hbm.inventory.recipes.crafting;

import com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes;
import com.hbm.items.machine.ItemRBMKPellet;
import com.hbm.items.machine.ItemRBMKRod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Port of CE's {@code com.hbm.crafting.handlers.RBMKFuelCraftingHandler} (107 lines, read in full;
 * see {@code docs/phase7/crafting_dynamic_handlers.md} catalog entry 6) - recycle one cool, spent
 * RBMK fuel rod into 8 pellets whose damage-value "stage" is derived from the rod's live
 * enrichment/poison state. Works in the player's personal 2x2 grid too, not just a real crafting
 * table (CE: {@code canFit(width, height) = width >= 1 && height >= 1}).
 * <p>
 * <b>Stitches together two already-committed-but-previously-unwired pieces</b> (per the research
 * report's "Already covered" finding 2): {@link RBMKFuelRecipes#getRecyclingOutput} (pellet
 * resolution) and {@link RBMKFuelRecipes#computeStage} (the enrichment/poison stage math, byte-for-
 * byte CE's own formula) existed with zero call sites anywhere in this port before this class - and
 * {@code getRecyclingOutput} itself never applied the stage it could have computed. This class is
 * the first real caller of both, and is the one that actually calls
 * {@link ItemRBMKPellet#setStage} on the result - see {@link #assemble}.
 * <p>
 * <b>CE quirk preserved, not silently fixed</b>: CE's own {@code matches()} does <em>not</em> check
 * {@code enrichment > 0.99} (only {@code getCraftingResult()} does) - so a still-fresh, unburned rod
 * passes {@code matches()} (heat is low, pellet resolves) but then {@code getCraftingResult()}
 * silently returns {@code ItemStack.EMPTY} anyway. This class reproduces that exact asymmetry
 * ({@link #matches} omits the enrichment check, {@link #assemble} includes it) rather than
 * "fixing" {@code matches()} to reject fresh rods up front - the net play behavior (nothing crafts)
 * is identical either way, and this keeps the two methods a faithful line-for-line translation of
 * CE's real, if slightly redundant, logic.
 */
public final class RBMKFuelRecycleRecipe implements CraftingRecipe {

    /** CE: rods stop being "cool enough" at 50 degrees on either the hull or the core reading. */
    private static final double MAX_SAFE_HEAT = 50D;

    public static final RBMKFuelRecycleRecipe INSTANCE = new RBMKFuelRecycleRecipe();

    private RBMKFuelRecycleRecipe() {
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack rodStack = CraftingInputs.onlyNonEmptyStack(input);
        if (rodStack == null) return false;
        if (!(rodStack.getItem() instanceof ItemRBMKRod)) return false;
        if (isTooHot(rodStack)) return false;
        // "non-null pellet" (CE: ItemRBMKRod.pellet != null) - this port's ItemRBMKRod.pellet field
        // is never populated (see RBMKFuelRecipes' own javadoc); resolvability of the recycling
        // output via registry-name lookup is the equivalent live check.
        return !RBMKFuelRecipes.getRecyclingOutput(rodStack).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack rodStack = CraftingInputs.onlyNonEmptyStack(input);
        if (rodStack == null || !(rodStack.getItem() instanceof ItemRBMKRod)) return ItemStack.EMPTY;
        if (ItemRBMKRod.getEnrichment(rodStack) > 0.99D) return ItemStack.EMPTY;
        if (isTooHot(rodStack)) return ItemStack.EMPTY;

        ItemStack output = RBMKFuelRecipes.getRecyclingOutput(rodStack);
        if (output.isEmpty()) return ItemStack.EMPTY;

        output = output.copy();
        ItemRBMKPellet.setStage(output, RBMKFuelRecipes.computeStage(rodStack));
        return output;
    }

    private static boolean isTooHot(ItemStack rodStack) {
        return ItemRBMKRod.getHullHeat(rodStack) >= MAX_SAFE_HEAT || ItemRBMKRod.getCoreHeat(rodStack) >= MAX_SAFE_HEAT;
    }

    /** CE: {@code canFit(width, height) = width >= 1 && height >= 1} - any real grid, including 2x2. */
    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    /** CE: {@code getRecipeOutput() = ItemStack.EMPTY} - fully dynamic, output depends on the live rod state. */
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public String getGroup() {
        return "";
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.create();
    }

    @Override
    public RecipeType<?> getType() {
        return RecipeType.CRAFTING;
    }

    @Override
    public RecipeSerializer<RBMKFuelRecycleRecipe> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public CraftingBookCategory category() {
        return CraftingBookCategory.MISC;
    }

    /** No per-instance configurable data - see {@code GrenadeCraftingRecipe.Serializer}'s javadoc for the singleton-codec rationale. */
    public static final class Serializer implements RecipeSerializer<RBMKFuelRecycleRecipe> {

        public static final Serializer INSTANCE = new Serializer();

        private static final MapCodec<RBMKFuelRecycleRecipe> CODEC = MapCodec.unit(RBMKFuelRecycleRecipe.INSTANCE);
        private static final StreamCodec<RegistryFriendlyByteBuf, RBMKFuelRecycleRecipe> STREAM_CODEC =
                StreamCodec.unit(RBMKFuelRecycleRecipe.INSTANCE);

        @Override
        public MapCodec<RBMKFuelRecycleRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, RBMKFuelRecycleRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
