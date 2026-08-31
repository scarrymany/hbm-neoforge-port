package com.hbm.inventory.recipes.machine;

import com.hbm.inventory.recipes.HbmRecipes;
import com.hbm.inventory.recipes.HbmSimpleRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link RecipeType}/{@link RecipeSerializer} registration for CE's
 * {@code com.hbm.inventory.recipes.LemegetonRecipes} (the {@code book_lemegeton} transmutation
 * table) - ported per {@code docs/phase7/mrec_08_chemplant_misc.md}'s catalog and item/registry
 * dependency check.
 * <p>
 * <b>Cleanest of this task's 4 files</b>: functionally identical in shape to
 * {@link HbmSimpleRecipe} (one {@code Ingredient} in, one fixed {@code ItemStack} out, no
 * duration/power used) - reused directly rather than a bespoke {@code LemegetonRecipe} class,
 * exactly the "second/third real consumer" precedent {@code ProcessingRecipes}
 * ({@code SHREDDER_TYPE}) and {@code BreederRecipes} already established for this same class (see
 * {@link HbmSimpleRecipe}'s own javadoc "Bug fix" section for why a distinct
 * {@code RecipeType}/{@code RecipeSerializer} pair per machine family is required, not just a
 * distinct JSON folder). No extra field needed at all (unlike {@code BreederRecipe}'s {@code flux}),
 * so this class only needs the registration, not a new {@code Recipe<?>} subclass.
 * <p>
 * <b>All 37 of CE's entries are ported</b> (37 JSON files under {@code data/hbm/recipe/lemegeton/}) -
 * confirmed zero item-registry blockers by tracing every input/output (many entries chain into each
 * other: e.g. {@code ingot_uranium} is simultaneously the output of the Th-232 entry and the input of
 * the next entry in the transmutation ladder). Appends directly to
 * {@link HbmRecipes#RECIPE_TYPES}/{@link HbmRecipes#RECIPE_SERIALIZERS} rather than owning a second
 * {@code DeferredRegister}, the same convention {@code ProcessingRecipes}/{@code BreederRecipes}
 * already established (avoids a second {@code register(modEventBus)} call site that would need
 * wiring into the shared {@code MainRegistry}, a file this task was told not to race on editing).
 * <p>
 * <b>{@link #bootstrap()}</b> only needs to run (forcing this class's static initializers) before
 * {@code HbmRecipes.register(modEventBus)} fires - see {@code BreederRecipes}'s own javadoc for the
 * identical call-order guarantee this follows. Not yet wired into any aggregator (that edit is
 * reported as a {@code wiringSnippets} entry per this task's ground rules) - until a coordinator adds
 * the {@code LemegetonRecipes.bootstrap();} call, this class's fields still register correctly the
 * moment any other code first references this class (JVM class-init-on-first-use), just not
 * necessarily before {@code HbmRecipes.register(modEventBus)} - so the wiring snippet matters for
 * correctness, not merely tidiness.
 * <p>
 * <b>No consuming Menu/Screen yet</b> (documented, out of this recipe-data task's scope): CE's
 * {@code book_lemegeton} item opens {@code GUILemegeton}/{@code ContainerLemegeton} (a screenless-BE
 * menu - just the player's own inventory plus 2 extra slots, no block/tile entity backing it at all).
 * This port has no precedent yet for an item-triggered (non-block-entity) {@link
 * net.minecraft.world.inventory.AbstractContainerMenu} - every {@code Menu} class in this port today
 * is opened from a block entity right-click (see {@code ItemBookLemegeton}'s own javadoc). Building
 * that Menu/Screen and wiring {@code ItemBookLemegeton#use()} to open it is a UI-infrastructure
 * addition, not recipe-data porting, and is left for a future pass - this class's 37 JSON recipes are
 * ready for it to consume via {@code RecipeManager.getRecipeFor(LEMEGETON_TYPE, ...)} the moment it
 * exists.
 */
public final class LemegetonRecipes {

    private LemegetonRecipes() {
    }

    public static final DeferredHolder<RecipeType<?>, RecipeType<HbmSimpleRecipe>> LEMEGETON_TYPE =
            HbmRecipes.RECIPE_TYPES.register("lemegeton", () -> new RecipeType<HbmSimpleRecipe>() {
                @Override
                public String toString() {
                    return "hbm:lemegeton";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HbmSimpleRecipe>> LEMEGETON_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("lemegeton", () -> new HbmSimpleRecipe.Serializer(LEMEGETON_TYPE));

    /** No-op - see class javadoc. Forces this class's static fields (and therefore the registrations above) to run. */
    public static void bootstrap() {
    }
}
