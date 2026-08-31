package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.util.Tuple;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CrackingRecipes} (124 lines, read in full) - the
 * Catalytic Cracker's recipe table: a {@code Map<FluidType, Tuple.Pair<FluidStack, FluidStack>>}
 * keyed by *input* fluid type (always consumed at a fixed 100mB per CE's own {@code getComment()}),
 * producing up to 2 fixed-percentage output fluids (a single-output recipe, e.g. {@code KEROSENE},
 * uses {@link Fluids#NONE} at 0mB for the unused second slot - matching CE's own convention exactly
 * rather than inventing a separate 1-output variant). Per {@code docs/phase7/mrec_07_shredder_misc.md}
 * ("CrackingRecipes" section) this is architecturally identical to this port's already-committed
 * {@link RefineryRecipes} (a {@code FluidType}-keyed hardcoded Java map, not a vanilla {@code
 * Recipe<?>}/JSON shape - see that class's own javadoc for the full rationale, which applies
 * verbatim here) except simpler: 2 fluid outputs and no item byproduct, vs. Refinery's 4 outputs +
 * 1 item. Every one of CE's 12 {@code registerDefaults()} entries is reproduced verbatim below -
 * the research report's dependency check confirmed all 22 distinct {@link FluidType} names this
 * class references already exist in this port's {@link Fluids} (individually re-confirmed here too,
 * not re-quoted on faith).
 * <p>
 * <b>Not yet consumed by any machine - flagged plainly, not silently left dangling.</b> CE's
 * consumer, {@code com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker}, is a distinct
 * machine from the already-ported Refinery ({@code TileEntityMachineRefinery}/{@code
 * MachineRefineryBlockEntity}) - easy to conflate since both deal with "cracking"-named fluids, but
 * this port has no Catalytic Cracker block, block entity, menu, screen, or JEI category at all yet
 * (confirmed: {@code blockentity/machine/oil/} contains only {@code OilWell}/{@code Pumpjack}/{@code
 * OilDrillBase}/{@code FrackingTower}/{@code Refinery}). Building that machine (block + block entity
 * + menu/screen + JEI category, plus the {@code ModBlocks}/{@code ModBlockEntities}/{@code
 * ClientProxy} wiring it needs) is out of this recipe-focused task's scope - it needs real new
 * capability/container-sync code this sandbox cannot compile-check, and would touch several of the
 * shared aggregator files this wave's ground rules say not to edit directly. This class exists so
 * the recipe *data* is ready and faithful the moment a future task builds that machine and calls
 * {@link #getCracking(FluidType)} from its tick method (the same shape {@code
 * MachineRefineryBlockEntity} already reads {@link RefineryRecipes#getRefinery(FluidType)} from) -
 * it has zero call sites today, matching this class's own honest "not yet wired to a consumer"
 * precedent already established by e.g. {@code MixerRecipes}/{@code CrystallizerRecipes} at various
 * points in this port's history.
 */
public final class CrackingRecipes {

    // cracking fractions in percent - field names/values kept identical to CE's own constants
    public static final int OIL_CRACK_OIL = 80;
    public static final int OIL_CRACK_PETRO = 20;
    public static final int BITUMEN_CRACK_OIL = 80;
    public static final int BITUMEN_CRACK_AROMA = 20;
    public static final int SMEAR_CRACK_NAPHT = 60;
    public static final int SMEAR_CRACK_PETRO = 40;
    public static final int GAS_CRACK_PETRO = 30;
    public static final int GAS_CRACK_UNSAT = 20;
    public static final int DIESEL_CRACK_KERO = 40;
    public static final int DIESEL_CRACK_PETRO = 30;
    public static final int KERO_CRACK_PETRO = 60;
    public static final int WOOD_CRACK_AROMA = 10;
    public static final int WOOD_CRACK_HEAT = 40;
    public static final int XYL_CRACK_AROMA = 80;
    public static final int XYL_CRACK_PETRO = 20;

    private static final Map<FluidType, Tuple.Pair<FluidStack, FluidStack>> CRACKING = new LinkedHashMap<>();

    private static boolean registered = false;

    private CrackingRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        CRACKING.put(Fluids.OIL, new Tuple.Pair<>(
                new FluidStack(Fluids.CRACKOIL, OIL_CRACK_OIL),
                new FluidStack(Fluids.PETROLEUM, OIL_CRACK_PETRO)));
        CRACKING.put(Fluids.BITUMEN, new Tuple.Pair<>(
                new FluidStack(Fluids.OIL, BITUMEN_CRACK_OIL),
                new FluidStack(Fluids.AROMATICS, BITUMEN_CRACK_AROMA)));
        CRACKING.put(Fluids.SMEAR, new Tuple.Pair<>(
                new FluidStack(Fluids.NAPHTHA, SMEAR_CRACK_NAPHT),
                new FluidStack(Fluids.PETROLEUM, SMEAR_CRACK_PETRO)));
        CRACKING.put(Fluids.GAS, new Tuple.Pair<>(
                new FluidStack(Fluids.PETROLEUM, GAS_CRACK_PETRO),
                new FluidStack(Fluids.UNSATURATEDS, GAS_CRACK_UNSAT)));
        CRACKING.put(Fluids.DIESEL, new Tuple.Pair<>(
                new FluidStack(Fluids.KEROSENE, DIESEL_CRACK_KERO),
                new FluidStack(Fluids.PETROLEUM, DIESEL_CRACK_PETRO)));
        CRACKING.put(Fluids.DIESEL_CRACK, new Tuple.Pair<>(
                new FluidStack(Fluids.KEROSENE, DIESEL_CRACK_KERO),
                new FluidStack(Fluids.PETROLEUM, DIESEL_CRACK_PETRO)));
        CRACKING.put(Fluids.KEROSENE, new Tuple.Pair<>(
                new FluidStack(Fluids.PETROLEUM, KERO_CRACK_PETRO),
                new FluidStack(Fluids.NONE, 0)));
        CRACKING.put(Fluids.WOODOIL, new Tuple.Pair<>(
                new FluidStack(Fluids.HEATINGOIL, WOOD_CRACK_HEAT),
                new FluidStack(Fluids.AROMATICS, WOOD_CRACK_AROMA)));
        CRACKING.put(Fluids.XYLENE, new Tuple.Pair<>(
                new FluidStack(Fluids.AROMATICS, XYL_CRACK_AROMA),
                new FluidStack(Fluids.PETROLEUM, XYL_CRACK_PETRO)));
        CRACKING.put(Fluids.HEATINGOIL_VACUUM, new Tuple.Pair<>(
                new FluidStack(Fluids.HEATINGOIL, 80),
                new FluidStack(Fluids.REFORMGAS, 20)));
        CRACKING.put(Fluids.REFORMATE, new Tuple.Pair<>(
                new FluidStack(Fluids.UNSATURATEDS, 40),
                new FluidStack(Fluids.REFORMGAS, 60)));
        CRACKING.put(Fluids.BIOGAS, new Tuple.Pair<>(
                new FluidStack(Fluids.PETROLEUM, 20),
                new FluidStack(Fluids.AROMATICS, 20)));
    }

    /** Ported from CE's {@code CrackingRecipes.getCracking} - input is always consumed at 100mB. */
    public static Tuple.Pair<FluidStack, FluidStack> getCracking(FluidType input) {
        register();
        return CRACKING.get(input);
    }

    /** Full-collection accessor for a future JEI category, matching {@link RefineryRecipes#getAllRefinery()}'s precedent. */
    public static Map<FluidType, Tuple.Pair<FluidStack, FluidStack>> getAllCracking() {
        register();
        return java.util.Collections.unmodifiableMap(CRACKING);
    }
}
