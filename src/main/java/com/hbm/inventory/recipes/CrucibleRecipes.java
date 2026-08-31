package com.hbm.inventory.recipes;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.items.IngotNuggetItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CrucibleRecipes} (275 lines, read in full) -
 * the 13 real (non-GregTech-gated) {@code registerDefaults()} entries only; see the "Scope trims"
 * note below for what's deliberately left out.
 * <p>
 * Follows this port's established "plain Java {@code Map<String, Recipe>} + lazy
 * {@code registerDefaults()}" convention for a {@code GenericRecipe}-shaped machine that doesn't fit
 * vanilla's {@code Recipe<RecipeInput>} contract (same shape as {@link MixerRecipes}/
 * {@code CrystallizerRecipes}/{@code RefineryRecipes} - each with its own javadoc making the same
 * "doesn't fit Recipe&lt;RecipeInput&gt;" case: a pool of {@link Mats.MaterialStack} entries
 * converted at a rate, not a one-shot ingredient-pattern craft), not a JSON-loader/
 * {@code GenericRecipes<T>} subclass. Registration is lazy (see {@link #registerDefaults()}) so the
 * item-dependent icons below (resolved via live {@code DeferredItem#get()} calls) never race
 * registry population - the same "lazy registration" precedent {@code MixerRecipes}/
 * {@code CrystallizerRecipes} already established.
 * <p>
 * <b>Scope trims vs. CE</b> (documented, not silent - matching sibling recipe classes' own
 * precedent): CE's external {@code hbmCrucible.json} recipe-override file
 * ({@code readRecipe}/{@code writeRecipe}) is a data-driven-tuning nicety, not required for
 * functional parity - not ported. CE's JEI-only {@code registerMoldsForJEI()}/
 * {@code getMoldRecipes()} (a cross-product of every smeltable material x every Foundry mold shape)
 * is display data for the separate, unported Foundry mold-casting block family, not consumed by the
 * Crucible block entity itself - not ported. CE's {@code getSmeltingRecipes()} (a JEI-only "which
 * items melt into a crucible" index) is likewise JEI-only display plumbing - not ported (no JEI
 * integration exists in this port yet for any machine).
 * <p>
 * <b>Entry count vs. this task's assignment brief</b>: a direct read of CE's
 * {@code registerDefaults()} finds 13 real, active {@code CrucibleRecipe} registrations, not the
 * ~24 the assignment brief estimated (confirmed by this task's own research report, "Scope
 * confirmed" section) - 4 more calls are commented out in CE's own source (GregTech-6 compat,
 * {@code crucible.steelWrought}/{@code steelPig}/{@code steelMeteoric}, out of scope per
 * PORT_SPEC.md's GregTech exclusion) and are not ported here either.
 */
public final class CrucibleRecipes {

    private static final Map<String, CrucibleRecipe> RECIPES = new LinkedHashMap<>();
    private static boolean registered = false;

    private CrucibleRecipes() {
    }

    /** See class javadoc "Lazy registration" - idempotent, safe to call any number of times from any thread context that already holds the tick lock (block entity ticks are single-threaded). */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        int n = MaterialShapes.NUGGET.q(1);
        int i = MaterialShapes.INGOT.q(1);

        register("crucible.steel", 20, new ItemStack(IngotNuggetItems.INGOT_STEEL.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_IRON, n * 2), new Mats.MaterialStack(Mats.MAT_CARBON, n * 3), new Mats.MaterialStack(Mats.MAT_FLUX, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_STEEL, n * 2)});

        register("crucible.hematite", 6, blockIcon("stone_resource_hematite"),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_HEMATITE, i * 2), new Mats.MaterialStack(Mats.MAT_FLUX, n * 2)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_IRON, i), new Mats.MaterialStack(Mats.MAT_SLAG, n * 3)});

        register("crucible.malachite", 6, blockIcon("stone_resource_malachite"),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_MALACHITE, i * 2), new Mats.MaterialStack(Mats.MAT_FLUX, n * 2)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_COPPER, i), new Mats.MaterialStack(Mats.MAT_SLAG, n * 3)});

        register("crucible.redcopper", 2, new ItemStack(IngotNuggetItems.INGOT_RED_COPPER.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_COPPER, n), new Mats.MaterialStack(Mats.MAT_REDSTONE, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_MINGRADE, n * 2)});

        register("crucible.hss", 9, new ItemStack(IngotNuggetItems.INGOT_DURA_STEEL.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_STEEL, n * 5), new Mats.MaterialStack(Mats.MAT_TUNGSTEN, n * 3), new Mats.MaterialStack(Mats.MAT_COBALT, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_DURA, n * 9)});

        register("crucible.ferro", 3, new ItemStack(IngotNuggetItems.INGOT_FERROURANIUM.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_STEEL, n * 2), new Mats.MaterialStack(Mats.MAT_U238, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_FERRO, n * 3)});

        register("crucible.tcalloy", 9, new ItemStack(IngotNuggetItems.INGOT_TCALLOY.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_STEEL, n * 8), new Mats.MaterialStack(Mats.MAT_TECHNETIUM, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_TCALLOY, i)});

        register("crucible.cdalloy", 9, new ItemStack(IngotNuggetItems.INGOT_CDALLOY.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_STEEL, n * 8), new Mats.MaterialStack(Mats.MAT_CADMIUM, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_CDALLOY, i)});

        register("crucible.bbronze", 9, new ItemStack(IngotNuggetItems.INGOT_BISMUTH_BRONZE.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_COPPER, n * 8), new Mats.MaterialStack(Mats.MAT_BISMUTH, n), new Mats.MaterialStack(Mats.MAT_FLUX, n * 3)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_BBRONZE, i), new Mats.MaterialStack(Mats.MAT_SLAG, n * 3)});

        register("crucible.abronze", 9, new ItemStack(IngotNuggetItems.INGOT_ARSENIC_BRONZE.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_COPPER, n * 8), new Mats.MaterialStack(Mats.MAT_ARSENIC, n), new Mats.MaterialStack(Mats.MAT_FLUX, n * 3)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_ABRONZE, i), new Mats.MaterialStack(Mats.MAT_SLAG, n * 3)});

        register("crucible.cmb", 3, new ItemStack(IngotNuggetItems.INGOT_COMBINE_STEEL.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_MAGTUNG, n * 6), new Mats.MaterialStack(Mats.MAT_MUD, n * 3)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_CMB, i)});

        register("crucible.magtung", 3, new ItemStack(IngotNuggetItems.INGOT_MAGNETIZED_TUNGSTEN.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_TUNGSTEN, i), new Mats.MaterialStack(Mats.MAT_SCHRABIDIUM, n)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_MAGTUNG, i)});

        register("crucible.bscco", 3, new ItemStack(IngotNuggetItems.INGOT_BSCCO.get()),
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_BISMUTH, n * 2), new Mats.MaterialStack(Mats.MAT_STRONTIUM, n * 2), new Mats.MaterialStack(Mats.MAT_CALCIUM, n * 2), new Mats.MaterialStack(Mats.MAT_COPPER, n * 3)},
                new Mats.MaterialStack[]{new Mats.MaterialStack(Mats.MAT_BSCCO, i)});
    }

    private static void register(String name, int frequency, ItemStack icon, Mats.MaterialStack[] input, Mats.MaterialStack[] output) {
        RECIPES.put(name, new CrucibleRecipe(name, frequency, icon, input, output));
    }

    /** Resolves a registered block's item form by registry id, matching {@code ItemMold.MoldEntry.getOutput}'s already-established lazy-lookup pattern (see that class's own javadoc) - safe here only because this whole method runs from lazily-invoked {@link #registerDefaults()}, never at class-load time. */
    private static ItemStack blockIcon(String itemId) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, itemId))
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }

    /** The named recipe, or {@code null} if unknown/unset - matches CE's {@code recipeNameMap.get(name)} lookup. */
    public static CrucibleRecipe getRecipe(String name) {
        registerDefaults();
        return RECIPES.get(name);
    }

    /** Every registered recipe's internal name, in registration order - used by the minimal in-GUI recipe cycler (see {@code MachineCrucibleScreen}). */
    public static List<String> getRecipeNames() {
        registerDefaults();
        return List.copyOf(RECIPES.keySet());
    }

    public static Map<String, CrucibleRecipe> getAllRecipes() {
        registerDefaults();
        return Collections.unmodifiableMap(RECIPES);
    }
}
