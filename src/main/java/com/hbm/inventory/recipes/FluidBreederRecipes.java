package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.FluidBreederRecipes} (70 lines, read in full) -
 * per {@code docs/phase7/mrec_10_crystallizer_misc.md}'s catalog (all 3 entries confirmed ready as
 * *data*: every referenced {@link FluidType} already exists in {@link Fluids}). Recipe shape: input
 * fluid type -> (input amount, output {@link FluidStack}) - a
 * {@code Map<FluidType, Tuple.Pair<Integer, FluidStack>>}, using this port's already-existing
 * {@link Tuple.Pair} (the same shape {@link CrystallizerRecipes} itself already keys its own map on).
 * <p>
 * <b>Special case - not reachable by any machine in this port, and deliberately not wired to one</b>
 * (per the research report's explicit recommendation): CE's sole consumer is
 * {@code TileEntityFusionBreeder}, the breeder blanket of the hot-fusion tokamak reactor
 * ({@code ModBlocks.fusion_breeder}, part of CE's {@code tileentity.machine.fusion.**}). This port's
 * own {@code com.hbm.blocks.machine.fusion.FusionBlocks} javadoc already documents that only the
 * separate ICF/Watz cold-fusion family was ported so far, and explicitly excludes the hot-fusion
 * tokamak (a structurally distinct, much larger system) as a future pass. Landing this 3-entry data
 * table now costs nothing and leaves nothing for that future hot-fusion pass to re-derive, but a
 * fusion-breeder block/block-entity is well outside this task's "port recipe data" scope - do not
 * treat this class's existence as implying {@code fusion_breeder} is buildable yet.
 */
public final class FluidBreederRecipes {

    private static final Map<FluidType, Tuple.Pair<Integer, FluidStack>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private FluidBreederRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // register(new FluidStack(Fluids.GAS, 1_000), new FluidStack(Fluids.SYNGAS, 1_000));
        register(new FluidStack(Fluids.GAS, 1_000), new FluidStack(Fluids.SYNGAS, 1_000));
        // register(new FluidStack(Fluids.LIGHTOIL, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
        register(new FluidStack(Fluids.LIGHTOIL, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
        // register(new FluidStack(Fluids.LIGHTOIL_CRACK, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
        register(new FluidStack(Fluids.LIGHTOIL_CRACK, 1_000), new FluidStack(Fluids.REFORMGAS, 1_000));
    }

    private static void register(FluidStack input, FluidStack output) {
        RECIPES.put(input.type, new Tuple.Pair<>(input.fill, output));
    }

    public static Tuple.Pair<Integer, FluidStack> getOutput(FluidType type) {
        registerDefaults();
        return RECIPES.get(type);
    }

    /** Full-collection accessor for a future JEI category, matching {@link CrystallizerRecipes#getAllRecipes()}'s own precedent. */
    public static Map<FluidType, Tuple.Pair<Integer, FluidStack>> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }
}
