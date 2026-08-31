package com.hbm.inventory.recipes.machine;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.BilletPowderItems;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recipe data for the DFC (laser transmutation) system, ported from CE's
 * {@code com.hbm.inventory.recipes.DFCRecipes} (126 ln, read in full - see
 * {@code docs/phase7/mrec_05_purex_misc.md}). CE's real shape is not even part of its
 * {@code GenericRecipes}/{@code SerializableRecipe} JSON-loader framework at all (no {@code extends}
 * clause, no {@code readRecipe}/{@code writeRecipe}/{@code getFileName}) - a plain
 * {@code LinkedHashMap<ComparableStack, Object[]>} populated by one {@code setRecipe(long, ItemStack,
 * ItemStack)} call, looked up by {@code TileEntityCrateTungsten.addEnergy} once accumulated laser
 * energy exceeds the required flux. This class reproduces that exact API shape
 * ({@link #setRecipe}/{@link #getRequiredFlux}/{@link #getOutput}) 1:1, matching CE's own naming so
 * the port's {@code CrateBlockEntity} (see that class's {@code addEnergy} override) reads identically
 * to CE's consumer.
 *
 * <p><b>Full catalog: CE has exactly 1 real entry</b> ({@code billet_polonium} -&gt;
 * {@code billet_yharonite}, 10,000,000 flux), and both items are already registered in this port
 * ({@link BilletPowderItems#BILLET_POLONIUM}/{@link BilletPowderItems#BILLET_YHARONITE}) - fully
 * item-ready, nothing blocked.</p>
 *
 * <p><b>Consumer status</b>: the real gap this task closes is {@code CrateBlockEntity}'s missing
 * {@code ILaserable}/{@code addEnergy} behavior (now wired - see that class), <em>not</em> this recipe
 * data. CE's actual laser <em>source</em> that would call {@code addEnergy} on a placed
 * {@code crate_tungsten} - {@code TileEntityCoreEmitter}, the {@code dfc_emitter}/{@code dfc_core}
 * casing-block family's tile entity - has no port-side equivalent yet (those blocks remain plain,
 * tile-entity-less decorative blocks in this port per the research report); building that emitter is
 * out of this task's scope (a separate DFC-core machine system, not named in this task's file list).
 * {@code addEnergy} is nonetheless fully wired and correct today for whatever future laser source (or
 * a debug/creative call) invokes it.</p>
 */
public final class DFCRecipes {

    private static final Map<ComparableStack, Object[]> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private DFCRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        setRecipe(10_000_000L,
                new ItemStack(BilletPowderItems.BILLET_POLONIUM.get()),
                new ItemStack(BilletPowderItems.BILLET_YHARONITE.get()));
    }

    public static void setRecipe(long requiredFlux, ItemStack in, ItemStack out) {
        RECIPES.put(new ComparableStack(in), new Object[]{requiredFlux, out});
    }

    /** Returns -1 if the stack has no registered DFC recipe, matching CE's own sentinel. */
    public static long getRequiredFlux(ItemStack stack) {
        register();
        if (stack == null || stack.isEmpty()) return -1;

        ComparableStack comp = new ComparableStack(stack).makeSingular();
        Object[] entry = RECIPES.get(comp);
        return entry != null ? (long) entry[0] : -1;
    }

    /** Returns {@link ItemStack#EMPTY} if the stack has no registered DFC recipe, matching CE's own sentinel. */
    public static ItemStack getOutput(ItemStack stack) {
        register();
        if (stack == null || stack.isEmpty()) return ItemStack.EMPTY;

        ComparableStack comp = new ComparableStack(stack).makeSingular();
        Object[] entry = RECIPES.get(comp);
        return entry != null ? (ItemStack) entry[1] : ItemStack.EMPTY;
    }
}
