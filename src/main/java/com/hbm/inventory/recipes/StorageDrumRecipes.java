package com.hbm.inventory.recipes;

import com.hbm.config.VersatileConfig;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.IngotNuggetItems;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recipe data for the Storage Drum, ported from CE's {@code com.hbm.inventory.recipes.
 * StorageDrumRecipes} (78 ln, read in full - {@code docs/phase7/mrec_06_soldering_misc.md}). CE's
 * real shape is a single-item-key {@code ComparableStack -> ItemStack} lookup (plus a parallel
 * {@code ComparableStack -> int[]{chance, wasteLiquid, wasteGas}} table) consulted by
 * {@code TileEntityStorageDrum.update()} - a <b>passive per-tick random-chance decay roll</b>
 * ({@code world.rand.nextInt(chance)==0}) for every occupied slot, <i>not</i> a player-invoked craft:
 * no GUI "craft" button, no progress bar, no recipe selection at all. Architecturally this is a
 * static lookup table consulted by passive world/block-entity logic (the same category as
 * {@code MatDistribution}), not a {@code RecipeType} the way a craftable machine recipe is - so, per
 * the research report's own recommendation, this stays a plain static Java data class (same shape as
 * {@link WasteDrumRecipes}, CE's closest sibling passive-decay table already ported this same pass):
 * no JSON, no {@code Recipe<?>}/{@code RecipeSerializer} at all, since JSON-loading a mechanic that
 * gains nothing from being data-pack-overridable the way a craftable recipe would just adds Codec
 * complexity for no benefit.
 *
 * <p><b>No {@code MachineStorageDrum} block/block-entity exists in this port yet either</b> (confirmed
 * absent by the research pass) - this class is pure recipe data for whichever future task builds that
 * block entity to consume (via {@link #getOutput(ItemStack)}/{@link #getWaste(ItemStack)}, mirroring
 * CE's own accessor pair), matching {@link WasteDrumRecipes}'s "recipe data only, ready for whichever
 * future pass builds the block entity" precedent.
 *
 * <p><b>Scope: only CE's fully item-ready entries are ported</b> (per this task's ground rules - do
 * not stub missing items). Of CE's 28 real entries, exactly <b>1</b> is item-ready today:
 * {@code nugget_au198 -> nugget_mercury} (CE's {@code recipeOutputs} field is literally named
 * {@code ingot_mercury}, but that field's own registry-id string is {@code "nugget_mercury"} - a
 * known CE field-name/registry-id mismatch this port's own {@link IngotNuggetItems#NUGGET_MERCURY}
 * javadoc already documents; both {@link IngotNuggetItems#NUGGET_AU198} and
 * {@link IngotNuggetItems#NUGGET_MERCURY} are confirmed registered). The other 27 are <b>not</b>
 * ported: <b>26</b> loop-generated entries (5 {@code ItemWasteLong.WasteClass} x 2 (normal/tiny) +
 * 8 {@code ItemWasteShort.WasteClass} x 2 (normal/tiny)) are blocked on the {@code _tiny}/
 * {@code _depleted}/{@code _depleted_tiny} nuclear-waste item variants - this port's own
 * {@code SpecialItems.java} already documents flattening only the base {@code nuclear_waste_long}/
 * {@code nuclear_waste_short} fields, leaving those six sibling families as "a distinct open
 * question" not yet resolved by any area. The remaining standalone entry,
 * {@code ingot_au198 -> bottle_mercury}, is blocked on {@code bottle_mercury} - this port's own
 * {@code FluidContainerRegistry.java} already documents that neither {@code bottle_mercury} nor a
 * real {@code ingot_mercury} exist.
 *
 * <p>Whoever picks up the 26 blocked loop entries later should extend {@code SpecialItems.java}'s
 * existing {@code EnumMap}-over-{@code WasteClass} pattern with the 3 missing sibling families x 2
 * (long/short), then reproduce CE's exact loop shape: {@code for (WasteClass w : VALUES) {
 * addRecipe(long[i], longDepleted[i], baseChance, w.liquid, w.gas); addRecipe(longTiny[i],
 * longDepletedTiny[i], baseChance/10, w.liquid/10, w.gas/10); }} (and the short-family equivalent),
 * table-keyed on {@code WasteClass}'s own {@code .liquid}/{@code .gas} fields exactly as CE does -
 * see the research report's "Generating pattern" note for the full shape.</p>
 */
public final class StorageDrumRecipes {

    public static final Map<ComparableStack, ItemStack> RECIPE_OUTPUTS = new LinkedHashMap<>();
    public static final Map<ComparableStack, int[]> RECIPE_WASTES = new LinkedHashMap<>();

    private static boolean registered = false;

    private StorageDrumRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE line 57: addRecipe(nugget_au198, ingot_mercury[=nugget_mercury id], (int)(shortChance*0.001), 50, 50)
        addRecipe(new ComparableStack(IngotNuggetItems.NUGGET_AU198.get()),
                new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get()),
                (int) (VersatileConfig.getShortDecayChance() * 0.001), 50, 50);

        // Not ported: 26 nuclear_waste_{long,short}{,_tiny} loop entries (missing _depleted/_tiny
        // item variants) and ingot_au198 -> bottle_mercury (missing bottle_mercury item) - see class
        // javadoc.
    }

    private static void addRecipe(ComparableStack input, ItemStack output, int chance, int wasteLiquid, int wasteGas) {
        RECIPE_OUTPUTS.put(input, output);
        RECIPE_WASTES.put(input, new int[]{chance, wasteLiquid, wasteGas});
    }

    /**
     * Ported from CE's own {@code StorageDrumRecipes.getOutput(ItemStack)}. CE's real lookup is a
     * direct {@code HashMap.get(new ComparableStack(stack))} - which, since CE's own
     * {@code ComparableStack.equals} folds {@code stacksize} into the comparison, only matches a
     * queried stack whose count is exactly the registered recipe's (1 here). Matched here via
     * {@code matchesRecipe(stack, true)} (ignoring count) instead - the same linear-scan, item-identity
     * -only lookup {@link WasteDrumRecipes#getOutput} (this port's closest sibling passive-decay
     * table) already established - so a >1-count stack in a drum slot still resolves correctly
     * regardless of exactly how a future {@code MachineStorageDrum} block entity presents it.
     */
    public static ItemStack getOutput(ItemStack stack) {
        register();
        if (stack == null || stack.isEmpty()) return null;
        for (Map.Entry<ComparableStack, ItemStack> entry : RECIPE_OUTPUTS.entrySet()) {
            if (entry.getKey().matchesRecipe(stack, true)) {
                return entry.getValue().copy();
            }
        }
        return null;
    }

    /**
     * Ported from CE's own {@code StorageDrumRecipes.getWaste(ItemStack)}: {chance, wasteLiquid,
     * wasteGas}. Same item-identity-only lookup as {@link #getOutput(ItemStack)} above.
     */
    public static int[] getWaste(ItemStack stack) {
        register();
        if (stack == null || stack.isEmpty()) return null;
        for (Map.Entry<ComparableStack, int[]> entry : RECIPE_WASTES.entrySet()) {
            if (entry.getKey().matchesRecipe(stack, true)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
