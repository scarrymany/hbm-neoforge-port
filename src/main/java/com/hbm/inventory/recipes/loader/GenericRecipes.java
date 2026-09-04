package com.hbm.inventory.recipes.loader;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal compile-time stand-in for CE's {@code com.hbm.inventory.recipes.loader.GenericRecipes<T>}.
 * <p>
 * CE's real {@code GenericRecipes} is an abstract, generic {@code SerializableRecipe} subclass —
 * the actual per-machine recipe registry/JSON-loader for the 9 "generic" machine recipe classes
 * (see {@link GenericRecipe}'s header for the full list and why porting that machinery is out of
 * scope for this task). This port has no {@code SerializableRecipe} loader at all yet, and per
 * PORT_SPEC.md's ground rule ("hardcoded recipes -> JSON {@code Recipe<?>} types with serializers")
 * future machine recipes are expected to use the vanilla {@code Recipe<?>}/{@code RecipeSerializer}
 * scaffolding (see {@code com.hbm.inventory.recipes.HbmRecipes}/{@code HbmSimpleRecipe}) instead of
 * this Java-object loader, not extend it — so this class does not attempt the generic
 * {@code <T extends GenericRecipe>} registry shape at all.
 * <p>
 * What it does keep, because two already-committed items read it directly today
 * ({@code com.hbm.items.machine.ItemBlueprints}/{@code ItemBlueprintFolder}): the blueprint-pool
 * bookkeeping — pool name prefixes and the two static indices CE's {@code addToPool}/
 * {@code clearPools} maintain. Once real machine recipes exist as JSON {@code Recipe<?>}s, the
 * research report's own design (Part B, point 3) expects these two maps to become a derived index
 * rebuilt from a {@code RecipesUpdatedEvent} listener rather than hand-populated via
 * {@link #addToPool} — that migration is not done here, {@link #addToPool}/{@link #clearPools} are
 * kept only so any future stopgap population code (or tests) has the same call shape CE had.
 */
public final class GenericRecipes {

    public final Map<String, GenericRecipe> recipeNameMap = new LinkedHashMap<>();
    public final List<GenericRecipe> recipeOrderedList = new ArrayList<>();
    public final Map<String, List<GenericRecipe>> autoSwitchGroups = new HashMap<>();

    public GenericRecipes() {
    }

    /** Alternate recipes, i.e. obtainable otherwise. */
    public static final String POOL_PREFIX_ALT = "alt.";
    /** Discoverable recipes, i.e. not obtainable otherwise. */
    public static final String POOL_PREFIX_DISCOVER = "discover.";
    /** Secret recipes, self-explanatory. */
    public static final String POOL_PREFIX_SECRET = "secret.";
    /** 528 greyprints. */
    public static final String POOL_PREFIX_528 = "528.";

    /** Blueprint pool name to list of recipe (internal) names that are part of this pool. */
    public static final Map<String, List<String>> blueprintPools = new HashMap<>();
    /** Internal name to recipe map for all recipes that are part of a pool, for lookup. */
    public static final Map<String, GenericRecipe> pooledBlueprints = new HashMap<>();

    /** Adds a recipe to a blueprint pool (i.e. a blueprint item's roll list). */
    public static void addToPool(String pool, GenericRecipe recipe) {
        blueprintPools.computeIfAbsent(pool, k -> new ArrayList<>()).add(recipe.getInternalName());
        pooledBlueprints.put(recipe.getInternalName(), recipe);
    }

    public static void clearPools() {
        blueprintPools.clear();
        pooledBlueprints.clear();
    }

    public interface IOutput {
        ItemStack collapse();
        ItemStack getSingle();
        boolean possibleMultiOutput();
    }

    public static final class ChanceOutput implements IOutput {
        public final ItemStack stack;

        public ChanceOutput(ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack collapse() {
            return stack.copy();
        }

        @Override
        public ItemStack getSingle() {
            return stack;
        }

        @Override
        public boolean possibleMultiOutput() {
            return false;
        }
    }
}
