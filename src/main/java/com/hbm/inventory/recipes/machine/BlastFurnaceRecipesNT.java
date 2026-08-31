package com.hbm.inventory.recipes.machine;

import com.hbm.inventory.recipes.HbmRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link RecipeType}/{@link RecipeSerializer} registration for {@link BlastFurnaceRecipeNT} - see
 * that class's own javadoc for the JSON shape, CE citation, and the "NT" naming-collision rationale
 * ({@code com.hbm.inventory.recipes.BlastFurnaceRecipes}, a sibling task's port of CE's unrelated,
 * older, {@code @Deprecated} Di-Furnace recipe class, already owns the bare "BlastFurnaceRecipes"
 * name one package up). Appends directly to {@link HbmRecipes#RECIPE_TYPES}/
 * {@link HbmRecipes#RECIPE_SERIALIZERS} rather than owning a second {@code DeferredRegister}, the
 * exact convention {@link BreederRecipes} already established (see that class's own javadoc for the
 * full rationale - avoids a second {@code register(modEventBus)} call site that would need wiring
 * into the shared {@code MainRegistry}, a file this task was told not to race on editing).
 *
 * <p>{@link #bootstrap()} only needs to run (forcing this class's static initializers) before
 * {@code HbmRecipes.register(modEventBus)} fires in {@code MainRegistry}'s constructor - unlike
 * {@link BreederRecipes} (piggybacked on {@code PWRBlocks.registerAll()}, an already-existing real
 * machine's block registration), no Blast Furnace block/block-entity exists in this port yet to hang
 * this call off of (see {@link BlastFurnaceRecipeNT}'s own javadoc), so this task reports the
 * {@code MainRegistry} call site as a {@code wiringSnippets} entry instead of guessing at one.
 */
public final class BlastFurnaceRecipesNT {

    private BlastFurnaceRecipesNT() {
    }

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastFurnaceRecipeNT>> BLAST_FURNACE_TYPE =
            HbmRecipes.RECIPE_TYPES.register("blast_furnace_nt", () -> new RecipeType<BlastFurnaceRecipeNT>() {
                @Override
                public String toString() {
                    return "hbm:blast_furnace_nt";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastFurnaceRecipeNT>> BLAST_FURNACE_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("blast_furnace_nt", () -> BlastFurnaceRecipeNT.Serializer.INSTANCE);

    /** No-op - see class javadoc. Forces this class's static fields (and therefore the registrations above) to run. */
    public static void bootstrap() {
    }
}
