package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.RotaryFurnaceRecipes} (177 lines, read in full
 * upstream; see {@code docs/phase7/mrec_09_blastfurnace_misc.md}). Preserves CE's own shape: a flat
 * list of {@code RotaryFurnaceRecipe} entries, order-independent {@link AStack} item ingredients, an
 * optional single fluid input, a steam cost, a duration, and (unlike every other recipe class in
 * this package) an output expressed as {@link MaterialStack} - a material identity + quanta amount,
 * not a fixed {@link ItemStack} - matching CE's own {@code MaterialStack output} field exactly.
 * <b>The real "what item comes out" resolution lives in whichever block entity eventually consumes
 * this class</b> (CE's {@code TileEntityMachineRotaryFurnace}, not read - out of this task's
 * recipe-data-only scope, see the research report's Open Questions #5); this class only carries the
 * material+amount abstraction, same as CE.
 * <p>
 * <b>Lazy registration</b>: see {@link CrystallizerRecipes#registerDefaults()}'s javadoc - the same
 * registry-not-populated-yet hazard applies here, so {@link #registerDefaults()} only runs on first
 * real lookup.
 * <p>
 * <b>Not yet built: the Rotary Furnace block/block entity itself</b> (confirmed absent - CE's real
 * machine, {@code MachineRotaryFurnace}, is a multiblock, {@code extends BlockDummyable} - a
 * materially bigger lift than a plain single-block machine, per the research report's Open
 * Questions #6). This class is recipe data only, ready for whichever future pass builds it.
 * <p>
 * <b>Scope trim vs. CE / discrepancy from the research report</b> (documented, not silent): of CE's
 * 12 recipes, <b>7</b> are ready here - #1, #3, #6, #7, #8, #11, #12. The report's "Ready now" bullet
 * claimed "10 of 12 (all except the 2 {@code MAT_SATURN} recipes)", but its own item catalog (and its
 * own separately-flagged {@code ANY_COKE} blocker) both show recipes #2, #4 and #5 also key on
 * {@code ANY_COKE.gem()} (CE's loose {@code coke} item, confirmed absent from this port - see
 * {@link BlastFurnaceRecipes}'s own javadoc for the same family) - the "10 of 12" summary line
 * simply didn't propagate that blocker to this file's own entries. Also not ported: #9/#10 (the 2
 * {@code MAT_SATURN} recipes, blocked on {@code powder_durasteel} - confirmed absent from
 * {@code BilletPowderItems.java} despite {@code Mats.MAT_DURA} declaring the {@code DUST} shape).
 */
public final class RotaryFurnaceRecipes {

    public static final List<RotaryFurnaceRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private RotaryFurnaceRecipes() {
    }

    /** See class javadoc "Lazy registration". */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // #1 (CE line 32): MAT_STEEL x1 ingot, 100 ticks, 100 steam - iron ingot + coal
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.q(1)), 100, 100,
                new ComparableStack(Items.IRON_INGOT), new ComparableStack(Items.COAL)));

        // #3 (CE line 35): MAT_STEEL x2 ingot, 200 ticks, 25 steam - 9x iron ore fragment + coal
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_STEEL, MaterialShapes.INGOT.q(2)), 200, 25,
                new ComparableStack(hbmItem("iron_ore_fragment"), 9), new ComparableStack(Items.COAL)));

        // #6 (CE line 39): MAT_DESH x1 ingot, 100 ticks, 200 steam, 100mB LIGHTOIL - powder_desh_ready
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_DESH, MaterialShapes.INGOT.q(1)), 100, 200,
                new FluidStack(Fluids.LIGHTOIL, 100),
                new ComparableStack(BilletPowderItems.POWDER_DESH_READY.get())));

        // #7 (CE line 41): MAT_GUNMETAL x4 ingot, 200 ticks, 100 steam - 3x copper ingot + 1x aluminium ingot
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_GUNMETAL, MaterialShapes.INGOT.q(4)), 200, 100,
                new ComparableStack(Items.COPPER_INGOT, 3), new ComparableStack(IngotNuggetItems.INGOT_ALUMINIUM.get(), 1)));

        // #8 (CE line 42): MAT_WEAPONSTEEL x1 ingot, 200 ticks, 400 steam, 100mB GAS_COKER - steel ingot + 2x powder_flux
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_WEAPONSTEEL, MaterialShapes.INGOT.q(1)), 200, 400,
                new FluidStack(Fluids.GAS_COKER, 100),
                new ComparableStack(IngotNuggetItems.INGOT_STEEL.get(), 1), new ComparableStack(BilletPowderItems.POWDER_FLUX.get(), 2)));

        // #11 (CE line 45): MAT_ALUMINIUM x2 ingot, 100 ticks, 400 steam, 150mB SODIUM_ALUMINATE - no item inputs
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT.q(2)), 100, 400,
                new FluidStack(Fluids.SODIUM_ALUMINATE, 150)));

        // #12 (CE line 46): MAT_ALUMINIUM x3 ingot, 40 ticks, 200 steam, 150mB SODIUM_ALUMINATE - 2x powder_flux
        RECIPES.add(new RotaryFurnaceRecipe(
                new MaterialStack(Mats.MAT_ALUMINIUM, MaterialShapes.INGOT.q(3)), 40, 200,
                new FluidStack(Fluids.SODIUM_ALUMINATE, 150),
                new ComparableStack(BilletPowderItems.POWDER_FLUX.get(), 2)));

        // Not ported (see class javadoc "Scope trim"): #2/#4/#5 (ANY_COKE.gem(), missing coke item),
        // #9/#10 (MAT_SATURN, blocked on missing powder_durasteel).
    }

    /**
     * Resolves one of this port's own items by registry name - matches
     * {@code CrystallizerRecipes#hbmBlock(String)}'s/{@code MixerRecipes#hbmItem(String)}'s
     * already-established lazy-lookup pattern. Used for {@code iron_ore_fragment}, a
     * {@code MaterialItemGenerator}-autogen item (material {@code iron} x shape
     * {@link MaterialShapes#FRAGMENT}) with no hand-declared field to reference directly.
     */
    private static Item hbmItem(String path) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * Ported from CE's own {@code RotaryFurnaceRecipes.getRecipe(ItemStack...)}: order-independent,
     * greedy per-input match against a mutable copy of the recipe's ingredient list, matching
     * {@code ArcWelderRecipes#getRecipe}'s already-established algorithm shape exactly (same
     * variable-length {@link AStack} ingredient design). Fluid input (if any) is checked separately
     * by whichever block entity eventually calls this, same as CE and same as {@code ArcWelderRecipes}.
     */
    public static RotaryFurnaceRecipe getRecipe(ItemStack... inputs) {
        registerDefaults();

        outer:
        for (RotaryFurnaceRecipe recipe : RECIPES) {
            List<AStack> recipeList = new ArrayList<>(List.of(recipe.ingredients));

            for (ItemStack inputStack : inputs) {
                if (inputStack == null || inputStack.isEmpty()) continue;

                boolean hasMatch = false;
                for (AStack recipeStack : recipeList) {
                    if (recipeStack.matchesRecipe(inputStack, true) && inputStack.getCount() >= recipeStack.count()) {
                        hasMatch = true;
                        recipeList.remove(recipeStack);
                        break;
                    }
                }

                if (!hasMatch) continue outer;
            }

            if (recipeList.isEmpty()) return recipe;
        }

        return null;
    }

    /**
     * Full-collection accessor, matching {@code CrystallizerRecipes#getAllRecipes()}'s established
     * JEI-enumeration precedent.
     */
    public static List<RotaryFurnaceRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    /**
     * Variable-length {@link AStack} ingredients (order-independent), an optional single fluid
     * input, a {@link MaterialStack} output (not a fixed {@link ItemStack} - see class javadoc),
     * steam cost + duration (ticks) - preserving CE's exact {@code RotaryFurnaceRecipe} shape.
     */
    public static final class RotaryFurnaceRecipe {
        public final MaterialStack output;
        public final int duration;
        public final int steam;
        public final FluidStack fluid;
        public final AStack[] ingredients;

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, FluidStack fluid, AStack... ingredients) {
            this.output = output;
            this.duration = duration;
            this.steam = steam;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }

        public RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, AStack... ingredients) {
            this(output, duration, steam, null, ingredients);
        }
    }
}
