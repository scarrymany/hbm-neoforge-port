package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.util.Tuple;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.RefineryRecipes} (137 lines, read in full) - a
 * bespoke, refinery-only recipe shape (see {@code docs/phase2/oil_production_chain.md}'s Key
 * design/API decisions): a {@code LinkedHashMap<FluidType, Tuple.Quintet<FluidStack, FluidStack,
 * FluidStack, FluidStack, ItemStack>>} populated once by {@link #registerRefinery()}, keyed by
 * *input* fluid type, producing up to 4 fixed-percentage output fluids plus one optional item
 * byproduct. CE never made this data-driven/moddable (no JEI-exposed "recipe system" the way
 * {@code RecipesCommon}/{@code GenericRecipe} are elsewhere in the mod) - per the research report's
 * own recommendation, this stays a literal hardcoded Java registration list rather than inventing a
 * new {@code Recipe<?>}/datagen shape for a single consumer, the lower-risk option that does not
 * block on the recipe/datagen cross-cutting work landing first.
 *
 * <p><b>Not ported</b>: CE's {@code getRefineryRecipe()}/{@code getVacuumRecipe()} (JEI-display-table
 * builders keyed by {@code ItemFluidIcon.make(...)}, an item this port has not ported) - JEI/REI
 * integration is out of this task's boundary per the research report's Deferred scope #8. The
 * {@link #vacuum} data map itself is still ported (cheap, and {@code TileEntityMachineRefinery} never
 * actually reads it either in CE - no vacuum-refinery block/TE exists in either codebase - so
 * {@link #getVacuum(FluidType)} is provided for a future consumer, matching CE's own dead-until-used
 * accessor).</p>
 *
 * <p><b>Item byproduct substitutions</b> (documented, not silent): CE's {@code ModItems.sulfur} and
 * {@code ModItems.oil_tar} (with {@code ItemEnums.EnumTarType}) are not registered items in this port
 * yet (confirmed absent - {@code com.hbm.blocks.OreBlocks}'s own javadoc already names both as
 * "not yet ported by any Phase 1 items area"). The {@code HOTOIL} recipe's sulfur byproduct substitutes
 * this port's already-registered {@link PlateCrystalWasteItems#CRYSTAL_SULFUR} (the closest existing
 * equivalent item); the three cracking recipes' {@code oil_tar} byproducts are left as
 * {@link ItemStack#EMPTY} (no item output) until that item exists - <b>TODO(items-followup)</b>: swap
 * in a real {@code oil_tar} item once whichever items-area registers it, at the three call sites
 * marked below.</p>
 */
public final class RefineryRecipes {

    // fractions in percent
    public static final int OIL_FRAC_HEAVY = 50;
    public static final int OIL_FRAC_NAPH = 25;
    public static final int OIL_FRAC_LIGHT = 15;
    public static final int OIL_FRAC_PETRO = 10;
    public static final int CRACK_FRAC_NAPH = 40;
    public static final int CRACK_FRAC_LIGHT = 30;
    public static final int CRACK_FRAC_AROMA = 15;
    public static final int CRACK_FRAC_UNSAT = 15;

    public static final int OILDS_FRAC_HEAVY = 30;
    public static final int OILDS_FRAC_NAPH = 35;
    public static final int OILDS_FRAC_LIGHT = 20;
    public static final int OILDS_FRAC_UNSAT = 15;
    public static final int CRACKDS_FRAC_NAPH = 35;
    public static final int CRACKDS_FRAC_LIGHT = 35;
    public static final int CRACKDS_FRAC_AROMA = 15;
    public static final int CRACKDS_FRAC_UNSAT = 15;

    public static final int VAC_FRAC_HEAVY = 40;
    public static final int VAC_FRAC_REFORM = 25;
    public static final int VAC_FRAC_LIGHT = 20;
    public static final int VAC_FRAC_SOUR = 15;

    private static final Map<FluidType, Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack>> refinery = new LinkedHashMap<>();
    private static final Map<FluidType, Tuple.Quartet<FluidStack, FluidStack, FluidStack, FluidStack>> vacuum = new LinkedHashMap<>();

    private static boolean registered = false;

    private RefineryRecipes() {
    }

    public static synchronized void registerRefinery() {
        if (registered) return;
        registered = true;

        refinery.put(Fluids.HOTOIL, new Tuple.Quintet<>(
                new FluidStack(Fluids.HEAVYOIL, OIL_FRAC_HEAVY),
                new FluidStack(Fluids.NAPHTHA, OIL_FRAC_NAPH),
                new FluidStack(Fluids.LIGHTOIL, OIL_FRAC_LIGHT),
                new FluidStack(Fluids.PETROLEUM, OIL_FRAC_PETRO),
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get())
        ));
        refinery.put(Fluids.HOTCRACKOIL, new Tuple.Quintet<>(
                new FluidStack(Fluids.NAPHTHA_CRACK, CRACK_FRAC_NAPH),
                new FluidStack(Fluids.LIGHTOIL_CRACK, CRACK_FRAC_LIGHT),
                new FluidStack(Fluids.AROMATICS, CRACK_FRAC_AROMA),
                new FluidStack(Fluids.UNSATURATEDS, CRACK_FRAC_UNSAT),
                ItemStack.EMPTY // TODO(items-followup): CE's oil_tar/EnumTarType.CRACK, item not yet registered
        ));
        refinery.put(Fluids.HOTOIL_DS, new Tuple.Quintet<>(
                new FluidStack(Fluids.HEAVYOIL, OILDS_FRAC_HEAVY),
                new FluidStack(Fluids.NAPHTHA_DS, OILDS_FRAC_NAPH),
                new FluidStack(Fluids.LIGHTOIL_DS, OILDS_FRAC_LIGHT),
                new FluidStack(Fluids.UNSATURATEDS, OILDS_FRAC_UNSAT),
                ItemStack.EMPTY // TODO(items-followup): CE's oil_tar/EnumTarType.PARAFFIN, item not yet registered
        ));
        refinery.put(Fluids.HOTCRACKOIL_DS, new Tuple.Quintet<>(
                new FluidStack(Fluids.NAPHTHA_DS, CRACKDS_FRAC_NAPH),
                new FluidStack(Fluids.LIGHTOIL_DS, CRACKDS_FRAC_LIGHT),
                new FluidStack(Fluids.AROMATICS, CRACKDS_FRAC_AROMA),
                new FluidStack(Fluids.UNSATURATEDS, CRACKDS_FRAC_UNSAT),
                ItemStack.EMPTY // TODO(items-followup): CE's oil_tar/EnumTarType.PARAFFIN, item not yet registered
        ));

        vacuum.put(Fluids.OIL, new Tuple.Quartet<>(
                new FluidStack(Fluids.HEAVYOIL_VACUUM, VAC_FRAC_HEAVY),
                new FluidStack(Fluids.REFORMATE, VAC_FRAC_REFORM),
                new FluidStack(Fluids.LIGHTOIL_VACUUM, VAC_FRAC_LIGHT),
                new FluidStack(Fluids.SOURGAS, VAC_FRAC_SOUR)
        ));
        vacuum.put(Fluids.OIL_DS, new Tuple.Quartet<>(
                new FluidStack(Fluids.HEAVYOIL_VACUUM, VAC_FRAC_HEAVY),
                new FluidStack(Fluids.REFORMATE, VAC_FRAC_REFORM),
                new FluidStack(Fluids.LIGHTOIL_VACUUM, VAC_FRAC_LIGHT),
                new FluidStack(Fluids.REFORMGAS, VAC_FRAC_SOUR)
        ));
    }

    public static Tuple.Quintet<FluidStack, FluidStack, FluidStack, FluidStack, ItemStack> getRefinery(FluidType oil) {
        return refinery.get(oil);
    }

    public static Tuple.Quartet<FluidStack, FluidStack, FluidStack, FluidStack> getVacuum(FluidType oil) {
        return vacuum.get(oil);
    }
}
