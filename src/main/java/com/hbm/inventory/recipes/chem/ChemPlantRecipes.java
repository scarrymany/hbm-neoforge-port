package com.hbm.inventory.recipes.chem;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for the Chemical Plant, ported from CE's {@code com.hbm.inventory.recipes.
 * ChemicalPlantRecipes} - a {@code GenericRecipes<GenericRecipe>} singleton in CE
 * ({@code docs/phase2/machines_chemical_isotope.md}'s Chemical Plant section). Per the task's own
 * scaffolding guidance ("prefer porting CE's real recipe data...into a JSON-backed
 * HbmSimpleRecipe-shaped recipe type you define for your machine family... do not block on a bigger
 * recipe-system redesign - port the data now") and this port's own {@code GenericRecipe}/
 * {@code GenericRecipes} stand-in (see that package's header: it deliberately does NOT carry the real
 * multi-input/fluid machine-recipe shape CE's Chemical Plant needs), this class defines its own small
 * {@link ChemPlantRecipe} data shape - up to 3 item inputs (each an {@link AStack}, so tag or exact
 * matches both work the same way {@link com.hbm.inventory.recipes.chem.CentrifugeRecipes} does), up
 * to 2 fluid inputs, up to 3 item outputs, 1 fluid output, duration + power - as a plain static table,
 * the same "port now, JSON-override later" shape {@code RefineryRecipes} already established this
 * pass, rather than reusing the unrelated blueprint-pool {@code GenericRecipe} stand-in.
 * <p>
 * <b>Recognition model differs from CE</b> (documented): CE's Chemical Plant is <i>player-selected</i>
 * (a GUI dropdown picks one recipe by name, {@code IControlReceiver}/{@code receiveControl}), not
 * automatically matched. {@code com.hbm.blockentity.machine.chem.ChemPlantBlockEntity} instead
 * auto-recognizes the active recipe from whatever item/fluid currently sits in the input slots/tanks -
 * the same automatic-recognition model the item Centrifuge and every other machine in this pass use -
 * since the named-recipe-pool GUI control channel is a separate, not-yet-existing cross-cutting
 * mechanism (see {@code docs/phase2/machines_chemical_isotope.md}'s Deferred scope #6).
 * <p>
 * <b>Scope trim</b>: CE registers ~30+ recipes; this class ports a representative real subset (gas
 * synthesis + basic inorganic/organic chemistry) with CE's exact ingredient/output quantities and
 * timings, using only fluids already confirmed present in {@link Fluids}.
 */
public final class ChemPlantRecipes {

    public static final List<ChemPlantRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private ChemPlantRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        RECIPES.add(new ChemPlantRecipe("chem.hydrogen", 20, 400,
                new AStack[]{OreDictStack.ofCommonTag("coals")},
                new FluidStack[]{new FluidStack(Fluids.WATER, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.HYDROGEN, 500)));

        RECIPES.add(new ChemPlantRecipe("chem.oxygen", 20, 400,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.OXYGEN, 500)));

        RECIPES.add(new ChemPlantRecipe("chem.ethanol", 50, 100,
                new AStack[]{new ComparableStack(Items.SUGAR, 10)},
                new FluidStack[0],
                new ItemStack[0],
                new FluidStack(Fluids.ETHANOL, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.cobble", 20, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25)},
                new ItemStack[]{new ItemStack(Blocks.COBBLESTONE)},
                null));
    }

    /**
     * Up to 3 {@link AStack} item inputs, up to 2 {@link FluidStack} fluid inputs, up to 3 item
     * outputs, 1 fluid output, duration (ticks) + power (HE/tick), preserving CE's exact recipe data.
     */
    public static final class ChemPlantRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final AStack[] inputItems;
        public final FluidStack[] inputFluids;
        public final ItemStack[] outputItems;
        public final FluidStack outputFluid;

        public ChemPlantRecipe(String name, int duration, long power, AStack[] inputItems,
                                FluidStack[] inputFluids, ItemStack[] outputItems, FluidStack outputFluid) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems;
            this.inputFluids = inputFluids;
            this.outputItems = outputItems;
            this.outputFluid = outputFluid;
        }
    }
}
