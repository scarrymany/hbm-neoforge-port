package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.HeatRecipes} (129 lines, read in full) - a
 * simple fluid-to-fluid "boil" (cold -&gt; hot) / "cool" (hot -&gt; cold) conversion table, keyed by
 * the <i>input</i> {@link FluidType} of each direction (CE: two separate
 * {@code HashMap<FluidType, HeatRecipe>} fields, {@code boilRecipes}/{@code coolRecipes}). Dropped
 * CE's {@code SerializableRecipe} JSON-datapack base (this port has no active consumer of that
 * loader layer for its bespoke machine-recipe classes - see {@link MixerRecipes}'s own javadoc for
 * the same simplification, same precedent) - kept as a plain hardcoded Java registration list.
 * <p>
 * <b>Ported per {@code mrec-03-silex-misc}</b> (see {@code docs/phase7/mrec_03_silex_misc.md}): all
 * 7 of CE's {@code addBoilAndCoolRecipe}/{@code addCoolRecipe} calls, verbatim (every fluid and heat
 * value already registered in this port - zero item/fluid blockers, per the research report's
 * dependency check). <b>Deliberately low-priority/no gameplay effect yet</b>, same as the research
 * report found for CE itself: a repo-wide grep of CE's own source for {@code HeatRecipes\.} finds no
 * consumer besides the class's own file and a Groovy/KubeJS-style scripting integration binding (not
 * game logic, and not in this port's scope anywhere) - CE's real boiler
 * ({@code TileEntityHeatBoiler.java}) hardcodes {@code WATER}-&gt;{@code STEAM} directly rather than
 * consulting this table, and this port's own {@code SolarBoilerBlockEntity} already replicates that
 * exact hardcoded behavior. This class exists for data parity and any future boiler/heat-exchanger
 * block entity that wants a real lookup table instead of a hardcoded pair - nothing in this port
 * currently calls {@link #getBoilRecipe}/{@link #getCoolRecipe}.
 */
public final class HeatRecipes {

    private static final Map<FluidType, HeatRecipe> BOIL_RECIPES = new LinkedHashMap<>();
    private static final Map<FluidType, HeatRecipe> COOL_RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private HeatRecipes() {
    }

    /** Idempotent, safe to call any number of times - matches {@link MixerRecipes#registerDefaults()}'s own lazy-registration convention, even though nothing here resolves a {@code DeferredItem}/{@code DeferredBlock} (only plain {@link Fluids} constants), for consistency with this file group's established pattern. */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // CE: addBoilAndCoolRecipe(new FluidStack(Fluids.WATER, 1), new FluidStack(Fluids.STEAM, 100), 100); - HeatRecipes.java:34
        addBoilAndCoolRecipe(new FluidStack(Fluids.WATER, 1), new FluidStack(Fluids.STEAM, 100), 100);
        // CE: addCoolRecipe(new FluidStack(Fluids.STEAM, 100), new FluidStack(Fluids.SPENTSTEAM, 1), 100); - cool-only, no reverse boil path in CE either - HeatRecipes.java:35
        addCoolRecipe(new FluidStack(Fluids.STEAM, 100), new FluidStack(Fluids.SPENTSTEAM, 1), 100);

        addBoilAndCoolRecipe(new FluidStack(Fluids.STEAM, 10), new FluidStack(Fluids.HOTSTEAM, 1), 15);
        addBoilAndCoolRecipe(new FluidStack(Fluids.HOTSTEAM, 10), new FluidStack(Fluids.SUPERHOTSTEAM, 1), 30);
        addBoilAndCoolRecipe(new FluidStack(Fluids.SUPERHOTSTEAM, 10), new FluidStack(Fluids.ULTRAHOTSTEAM, 1), 120);
        addBoilAndCoolRecipe(new FluidStack(Fluids.OIL, 1), new FluidStack(Fluids.HOTOIL, 1), 300);
        addBoilAndCoolRecipe(new FluidStack(Fluids.CRACKOIL, 1), new FluidStack(Fluids.HOTCRACKOIL, 1), 300);
        addBoilAndCoolRecipe(new FluidStack(Fluids.COOLANT, 1), new FluidStack(Fluids.COOLANT_HOT, 1), 500);
    }

    public static void addBoilAndCoolRecipe(FluidStack cold, FluidStack hot, int heat) {
        addBoilRecipe(cold, hot, heat);
        addCoolRecipe(hot, cold, heat);
    }

    public static void addBoilRecipe(FluidStack cold, FluidStack hot, int heat) {
        BOIL_RECIPES.put(cold.type, new HeatRecipe(cold, hot, heat));
    }

    public static void addCoolRecipe(FluidStack hot, FluidStack cold, int heat) {
        COOL_RECIPES.put(hot.type, new HeatRecipe(hot, cold, heat));
    }

    public static HeatRecipe getBoilRecipe(FluidType fluid) {
        registerDefaults();
        return BOIL_RECIPES.get(fluid);
    }

    public static HeatRecipe getCoolRecipe(FluidType fluid) {
        registerDefaults();
        return COOL_RECIPES.get(fluid);
    }

    public static boolean hasBoilRecipe(FluidType fluid) {
        registerDefaults();
        return BOIL_RECIPES.containsKey(fluid);
    }

    public static boolean hasCoolRecipe(FluidType fluid) {
        registerDefaults();
        return COOL_RECIPES.containsKey(fluid);
    }

    /** Ported verbatim from CE's inner {@code HeatRecipes.HeatRecipe}: {@code input}/{@code output} carry both the fluid type and the exact per-cycle amount consumed/produced, {@code heat} the heat-unit cost of one cycle. */
    public static final class HeatRecipe {
        public final FluidStack input;
        public final FluidStack output;
        public final int heat;

        public HeatRecipe(FluidStack input, FluidStack output, int heat) {
            this.input = input;
            this.output = output;
            this.heat = heat;
        }
    }
}
