package com.hbm.inventory.recipes.machine;

import com.hbm.inventory.recipes.HbmRecipes;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * {@link RecipeType}/{@link RecipeSerializer} registration for {@link BreederRecipe} - see that
 * class's own javadoc for the JSON shape and CE citation. Appends directly to
 * {@link HbmRecipes#RECIPE_TYPES}/{@link HbmRecipes#RECIPE_SERIALIZERS} rather than owning a second
 * {@code DeferredRegister}, the exact convention {@code com.hbm.inventory.recipes.ProcessingRecipes}
 * already established for a second real {@code HbmSimpleRecipe}-family consumer (see that class's own
 * javadoc for the full rationale - avoids a second {@code register(modEventBus)} call site that would
 * need wiring into the shared {@code MainRegistry}, a file this task was told not to race on editing).
 *
 * <p>{@link #bootstrap()} only needs to run (forcing this class's static initializers) before
 * {@code HbmRecipes.register(modEventBus)} fires - guaranteed by {@link com.hbm.blocks.machine.PWRBlocks#registerAll()}
 * calling it, itself reached from {@code ModBlocks.register()} strictly before
 * {@code HbmRecipes.register(modEventBus)} in {@code MainRegistry}'s constructor (see this task's
 * wiring notes), the identical call-order guarantee {@code ProcessingBlocks}/{@code ProcessingRecipes}
 * already rely on.
 */
public final class BreederRecipes {

    private BreederRecipes() {
    }

    public static final DeferredHolder<RecipeType<?>, RecipeType<BreederRecipe>> BREEDER_TYPE =
            HbmRecipes.RECIPE_TYPES.register("breeder", () -> new RecipeType<BreederRecipe>() {
                @Override
                public String toString() {
                    return "hbm:breeder";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BreederRecipe>> BREEDER_SERIALIZER =
            HbmRecipes.RECIPE_SERIALIZERS.register("breeder", () -> BreederRecipe.Serializer.INSTANCE);

    /** No-op - see class javadoc. Forces this class's static fields (and therefore the registrations above) to run. */
    public static void bootstrap() {
    }
}
