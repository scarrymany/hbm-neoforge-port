package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.RadiolysisRecipes} (67 lines, read in full;
 * {@code docs/phase7/mrec_11_centrifuge_misc.md}) - a flat
 * {@code Map<FluidType, Tuple.Pair<FluidStack, FluidStack>>} with exactly 1 literal entry
 * ({@code WATER -> {80 PEROXIDE, 20 HYDROGEN}}) plus every entry from {@link CrackingRecipes} folded
 * in wholesale: CE's own {@code registerRadiolysis()} {@code putAll()}s
 * {@code CrackingRecipes.getCrackingRecipes()} and throws {@code IllegalStateException} if that map
 * is empty at that point ("Either the load order is broken or cracking recipes have been removed!") -
 * a hard cross-class dependency, reproduced verbatim below via {@link CrackingRecipes#getAllCracking()}
 * (already ported in full, all 11 CE entries, by a separate Phase 7 pass -
 * {@code docs/phase7/mrec_07_shredder_misc.md} - confirmed present and non-empty at this class's own
 * {@link #register()} time). Total: 12 entries (1 + 11), all fluid-only, nothing item/fluid-blocked -
 * every referenced {@link FluidType} confirmed registered in {@link Fluids}.
 * <p>
 * <b>Not yet built: the Radiolysis machine block/block-entity/GUI</b> (0% built, confirmed by
 * repo-wide grep). This class is recipe data only, ready for whichever future pass builds a consumer,
 * matching {@code ArcWelderRecipes}'/{@code CrackingRecipes}' own already-established "data ahead of
 * machine" convention. It has zero call sites today.
 */
public final class RadiolysisRecipes {

    public static final Map<FluidType, Tuple.Pair<FluidStack, FluidStack>> RADIOLYSIS = new LinkedHashMap<>();

    private static boolean registered = false;

    private RadiolysisRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE: radiolysis.put(Fluids.WATER, new Pair<>(new FluidStack(80, Fluids.PEROXIDE), new FluidStack(20, Fluids.HYDROGEN)))
        RADIOLYSIS.put(Fluids.WATER, new Tuple.Pair<>(
                new FluidStack(Fluids.PEROXIDE, 80), new FluidStack(Fluids.HYDROGEN, 20)));

        // CE: "automatically add cracking recipes to the radiolysis recipe list - we want the numbers
        // and types to stay consistent anyway and this will save us a lot of headache later on" -
        // reproduced verbatim, including CE's own empty-map load-order guard.
        Map<FluidType, Tuple.Pair<FluidStack, FluidStack>> cracking = CrackingRecipes.getAllCracking();
        if (cracking.isEmpty()) {
            throw new IllegalStateException(
                    "RadiolysisRecipes: CrackingRecipes.getAllCracking() returned an empty map while "
                            + "registering the radiolysis recipes - either the load order is broken or "
                            + "cracking recipes have been removed!");
        }
        RADIOLYSIS.putAll(cracking);
    }

    /** Ported from CE's {@code RadiolysisRecipes.getRadiolysis}. */
    public static Tuple.Pair<FluidStack, FluidStack> getRadiolysis(FluidType input) {
        register();
        return RADIOLYSIS.get(input);
    }
}
