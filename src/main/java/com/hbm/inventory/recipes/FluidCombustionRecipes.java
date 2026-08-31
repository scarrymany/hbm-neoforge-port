package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.FluidCombustionRecipes} (92 lines, read in
 * full) - a flat {@code FluidType -> thermal-units-per-1000mB} lookup table, kept as a plain
 * hardcoded Java registration list (no JSON layer - CE's own version has none either, unlike its
 * {@code HeatRecipes}/{@code MixerRecipes} siblings).
 * <p>
 * <b>Ported per {@code mrec-03-silex-misc}</b> (see {@code docs/phase7/mrec_03_silex_misc.md}): all
 * 24 of CE's real {@code FluidType}-keyed {@code addBurnableFluid} calls, verbatim TU values (every
 * fluid already registered in this port - zero fluid blockers, per the research report's dependency
 * check). <b>Deliberately not ported</b>: CE's 14 additional lowercase-string
 * {@code addBurnableFluid(String, int)} calls (e.g. {@code "liquidhydrogen"}, {@code "biodiesel"}) -
 * these target other 1.12 mods' compat fluid-registry names, are self-guarded by
 * {@code if(Fluids.fromName(fluid) != Fluids.NONE)}, and (since CE's own {@link Fluids} constant
 * names are all-caps while every one of these 14 strings is lowercase) almost certainly resolve to
 * {@link Fluids#NONE}/no-op even inside CE itself - the research report judged transcribing them not
 * worth the effort, and this port's own {@link Fluids#fromName(String)} would hit the exact same
 * case-mismatch miss regardless, so the {@code String} overload is kept for API parity but never
 * invoked from {@link #registerDefaults()}.
 * <p>
 * <b>Deliberately low-priority/no gameplay effect yet</b>, same as the research report found for CE
 * itself: a repo-wide grep of CE's own source for {@code FluidCombustionRecipes\.}/
 * {@code getFlameEnergy} finds exactly one real consumer outside the class's own file
 * ({@code GUIMachineGasFlare.java}), and that consumer only uses {@link #hasFuelRecipe} to decide
 * whether to render a cosmetic flame icon - the actual burn/no-burn gameplay logic checks the
 * fluid's {@code FT_Flammable} trait directly, not this table's TU value. This port also has no Gas
 * Flare machine (block/block entity/GUI) built yet at all (confirmed: {@code grep -rli
 * "gasflare|gas_flare"} over this port's source returns zero hits), so porting this table has no
 * observable effect until that machine exists, and even then only drives a cosmetic icon.
 */
public final class FluidCombustionRecipes {

    private static final Map<FluidType, Integer> RESULTING_TU = new LinkedHashMap<>();

    private static boolean registered = false;

    private FluidCombustionRecipes() {
    }

    /** Idempotent, safe to call any number of times - matches this file group's established lazy-registration convention (see {@link MixerRecipes#registerDefaults()}'s own javadoc). */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // for 1000 mb - CE: FluidCombustionRecipes.java:13-46
        addBurnableFluid(Fluids.HYDROGEN, 5);
        addBurnableFluid(Fluids.DEUTERIUM, 5);
        addBurnableFluid(Fluids.TRITIUM, 5);

        addBurnableFluid(Fluids.OIL, 10);
        addBurnableFluid(Fluids.HOTOIL, 10);
        addBurnableFluid(Fluids.CRACKOIL, 10);
        addBurnableFluid(Fluids.HOTCRACKOIL, 10);

        addBurnableFluid(Fluids.GAS, 10);
        addBurnableFluid(Fluids.FISHOIL, 15);
        addBurnableFluid(Fluids.LUBRICANT, 20);
        addBurnableFluid(Fluids.AROMATICS, 25);
        addBurnableFluid(Fluids.PETROLEUM, 25);
        addBurnableFluid(Fluids.BIOGAS, 25);
        addBurnableFluid(Fluids.BITUMEN, 35);
        addBurnableFluid(Fluids.HEAVYOIL, 50);
        addBurnableFluid(Fluids.SMEAR, 50);
        addBurnableFluid(Fluids.ETHANOL, 75);
        addBurnableFluid(Fluids.RECLAIMED, 100);
        addBurnableFluid(Fluids.PETROIL, 125);
        addBurnableFluid(Fluids.NAPHTHA, 125);
        addBurnableFluid(Fluids.HEATINGOIL, 150);
        addBurnableFluid(Fluids.BIOFUEL, 150);
        addBurnableFluid(Fluids.DIESEL, 200);
        addBurnableFluid(Fluids.LIGHTOIL, 200);
        addBurnableFluid(Fluids.KEROSENE, 300);
        addBurnableFluid(Fluids.GASOLINE, 800);

        // CE comment: "why are we registering it twice?.." - BALEFIRE's first (1_000) registration is
        // commented out in CE's own source, only the 10_000 one below actually runs. Transcribed as-is.
        addBurnableFluid(Fluids.UNSATURATEDS, 1_000);
        addBurnableFluid(Fluids.NITAN, 2_000);
        addBurnableFluid(Fluids.BALEFIRE, 10_000);

        // CE's 14 lowercase compat-mod-fluid-name addBurnableFluid(String, int) calls are
        // deliberately not transcribed here - see class javadoc.
    }

    public static int getFlameEnergy(FluidType fluid) {
        registerDefaults();
        Integer heat = RESULTING_TU.get(fluid);
        return heat != null ? heat : 0;
    }

    public static boolean hasFuelRecipe(FluidType fluid) {
        registerDefaults();
        return RESULTING_TU.containsKey(fluid);
    }

    public static void addBurnableFluid(FluidType fluid, int heatPerMilibucket) {
        RESULTING_TU.put(fluid, heatPerMilibucket);
    }

    /** API parity with CE's {@code String}-keyed overload - see class javadoc for why {@link #registerDefaults()} never calls this. */
    public static void addBurnableFluid(String fluid, int heatPerMilibucket) {
        FluidType type = Fluids.fromName(fluid);
        if (type != Fluids.NONE) {
            addBurnableFluid(type, heatPerMilibucket);
        }
    }

    public static void removeBurnableFluid(FluidType fluid) {
        RESULTING_TU.remove(fluid);
    }
}
