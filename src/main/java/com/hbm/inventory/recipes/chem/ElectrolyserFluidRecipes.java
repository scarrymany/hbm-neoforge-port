package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.ElectrolyserFluidRecipes} - the fluid-electrolysis
 * half of the Electrolyser ({@code docs/phase2/machines_chemical_isotope.md}'s Electrolyser section).
 * Every recipe's {@code amount}/output amounts/duration are CE's exact numbers.
 * <p>
 * <b>Phase 7 mrec-02-assembly-misc pass</b>: {@code VITRIOL}'s second byproduct
 * ({@code ingot_mercury} in CE - CE's own field name for what its registry actually calls
 * {@code nugget_mercury}, see {@link IngotNuggetItems#NUGGET_MERCURY}'s javadoc for that CE-side
 * field/id mismatch) is now wired in - the earlier scope-trim TODO is resolved, that item is
 * registered. {@code REDMUD} (CE line 30) is a new entry this pass, ready with zero blockers (all 3
 * byproduct powders and both fluids already registered). Still not ported: {@code SLOP} (CE line 29,
 * needs CE's bare {@code niter}/{@code sulfur} items - distinct from the already-ported
 * {@code crystal_niter}/{@code crystal_sulfur} family - neither registered in this port yet) and
 * {@code ALUMINA}'s second byproduct {@code fluorite} (CE's bare item, distinct from the already-ported
 * {@code crystal_fluorite}, not registered yet either).
 * <p>
 * The ore/crystal electrolysis half ({@code ElectrolyserMetalRecipes}) is not ported this pass - see
 * {@code com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity}'s javadoc: it requires
 * {@code com.hbm.util.CrucibleUtil}'s foundry/casting system, not ported anywhere in this port yet, a
 * real Phase 2/4 boundary dependency flagged by the research doc itself.
 */
public final class ElectrolyserFluidRecipes {

    public static final Map<FluidType, ElectrolysisRecipe> RECIPES = new HashMap<>();

    private static boolean registered = false;

    private ElectrolyserFluidRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        RECIPES.put(Fluids.WATER, new ElectrolysisRecipe(2_000, new FluidStack(Fluids.HYDROGEN, 200), new FluidStack(Fluids.OXYGEN, 200), 10));
        RECIPES.put(Fluids.HEAVYWATER, new ElectrolysisRecipe(2_000, new FluidStack(Fluids.DEUTERIUM, 200), new FluidStack(Fluids.OXYGEN, 200), 10));
        RECIPES.put(Fluids.VITRIOL, new ElectrolysisRecipe(1_000, new FluidStack(Fluids.SULFURIC_ACID, 500), new FluidStack(Fluids.CHLORINE, 500), 20,
                new ItemStack(BilletPowderItems.POWDER_IRON.get()), new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get())));
        RECIPES.put(Fluids.REDMUD, new ElectrolysisRecipe(450, new FluidStack(Fluids.MERCURY, 150), new FluidStack(Fluids.LYE, 50), 20,
                new ItemStack(BilletPowderItems.POWDER_TITANIUM.get(), 3), new ItemStack(BilletPowderItems.POWDER_IRON.get(), 3), new ItemStack(BilletPowderItems.POWDER_ALUMINIUM.get(), 2)));
        RECIPES.put(Fluids.POTASSIUM_CHLORIDE, new ElectrolysisRecipe(250, new FluidStack(Fluids.CHLORINE, 125), new FluidStack(Fluids.NONE, 0), 20));
        RECIPES.put(Fluids.CALCIUM_CHLORIDE, new ElectrolysisRecipe(250, new FluidStack(Fluids.CHLORINE, 125), new FluidStack(Fluids.CALCIUM_SOLUTION, 125), 20));

        RECIPES.put(Fluids.ALUMINA, new ElectrolysisRecipe(200, new FluidStack(Fluids.CARBONDIOXIDE, 100), new FluidStack(Fluids.NONE, 0), 40,
                new ItemStack(BilletPowderItems.POWDER_ALUMINIUM.get(), 7)));
    }

    public static ElectrolysisRecipe getRecipe(FluidType type) {
        return type == null ? null : RECIPES.get(type);
    }

    public static final class ElectrolysisRecipe {
        public final int amount;
        public final FluidStack output1;
        public final FluidStack output2;
        public final int duration;
        public final ItemStack[] byproduct;

        public ElectrolysisRecipe(int amount, FluidStack output1, FluidStack output2, int duration, ItemStack... byproduct) {
            this.amount = amount;
            this.output1 = output1;
            this.output2 = output2;
            this.duration = duration;
            this.byproduct = byproduct;
        }
    }
}
