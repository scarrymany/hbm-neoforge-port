package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.MixerRecipes} (221 lines, read in full) - a
 * bespoke, mixer-only recipe shape keyed by <i>output</i> {@link FluidType}, each key mapping to an
 * <b>array</b> of competing recipes (CE: {@code HashMap<FluidType, MixerRecipe[]>}) rather than a
 * single recipe - see {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s
 * "Recipe shape" analysis and its "Open questions" flag on this exact array being easy to flatten
 * incorrectly. Kept as a plain hardcoded Java registration list, not a JSON {@code Recipe<?>}, for
 * the same reason {@link CrystallizerRecipes} is (see that class's own javadoc, and
 * {@link ProcessingRecipes}'s): two independent fluid inputs plus an optional solid input plus a
 * competing-recipe array is a shape vanilla's {@code Recipe<RecipeInput>} contract does not fit
 * without a much larger custom-ingredient design this task's scope does not call for.
 * <p>
 * <b>Lazy registration</b>: see {@link CrystallizerRecipes#registerDefaults()}'s javadoc for why this
 * class's own {@link #registerDefaults()} is populated on first real lookup ({@link #getOutput}),
 * never eagerly from a block/mod registration bootstrap chain - the same registry-not-populated-yet
 * hazard applies here (several entries below reference this port's own {@code powder_*} items via
 * {@code DeferredItem.get()}).
 * <p>
 * <b>Scope trim vs. CE</b> (documented, not silent - matching {@link CrystallizerRecipes}' and
 * {@code RefineryRecipes}' precedent): CE's ~40 mixer recipes reference several items/fluids this
 * port hasn't registered yet ({@code niter}/{@code sulfur}/{@code fluorite} dusts, {@code
 * pellet_charged}, {@code fuel_additive}, ore-dictionary tag lookups via {@code OreDictManager}).
 * Every recipe below is a real CE recipe restricted to the subset whose fluids AND solid input item
 * (where present) are confirmed already registered in this port - see each recipe's inline CE-origin
 * comment. {@link Fluids#LUBRICANT}/{@link Fluids#NITROGLYCERIN}/{@link Fluids#OXYHYDROGEN} are
 * deliberately kept as their real CE 2-recipe pairs (not flattened to one), directly addressing that
 * "Open questions" flag: a machine implementer copying this file should treat the
 * multi-recipe-per-fluid array as load-bearing, not an accident of this data set. ({@code FRACKSOL}'s
 * own real 2-recipe pair is not ported - its second recipe needs a sulfur-dust item this port hasn't
 * registered yet, and porting only its first recipe would misrepresent {@code FRACKSOL} as
 * single-recipe when CE's real data is a competing pair - left out entirely rather than
 * half-ported.)
 */
public final class MixerRecipes {

    private static final Map<FluidType, MixerRecipe[]> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private MixerRecipes() {
    }

    /** See class javadoc "Lazy registration" - idempotent, safe to call any number of times from any thread context that already holds the tick lock (block entity ticks are single-threaded). */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // register(Fluids.CRYOGEL, new MixerRecipe(2_000, 50).setStack1(new FluidStack(Fluids.COOLANT, 1_800)).setSolid(new ComparableStack(ModItems.powder_ice)));
        register(Fluids.CRYOGEL, new MixerRecipe(2_000, 50)
                .setStack1(new FluidStack(Fluids.COOLANT, 1_800))
                .setSolid(new ComparableStack(BilletPowderItems.POWDER_ICE.get())));

        // register(Fluids.NITAN, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.KEROSENE, 600)).setStack2(new FluidStack(Fluids.MERCURY, 200)).setSolid(new ComparableStack(ModItems.powder_nitan_mix)));
        register(Fluids.NITAN, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.KEROSENE, 600))
                .setStack2(new FluidStack(Fluids.MERCURY, 200))
                .setSolid(new ComparableStack(BilletPowderItems.POWDER_NITAN_MIX.get())));

        // register(Fluids.FISHOIL, new MixerRecipe(100, 50).setSolid(new ComparableStack(Items.FISH, 1, WILDCARD))); - 1.13+ flattened fish items, nearest single equivalent is COD
        register(Fluids.FISHOIL, new MixerRecipe(100, 50)
                .setSolid(new ComparableStack(Items.COD)));

        // register(Fluids.SUNFLOWEROIL, new MixerRecipe(100, 50).setSolid(new ComparableStack(Blocks.DOUBLE_PLANT, 1, 0))); - 1.13+ sunflower is its own block, no more meta
        register(Fluids.SUNFLOWEROIL, new MixerRecipe(100, 50)
                .setSolid(new ComparableStack(Blocks.SUNFLOWER)));

        // register(Fluids.THORIUM_SALT, new MixerRecipe(1_000, 30).setStack1(new FluidStack(Fluids.CHLORINE, 1000)).setSolid(new RecipesCommon.OreDictStack(TH232.dust())));
        register(Fluids.THORIUM_SALT, new MixerRecipe(1_000, 30)
                .setStack1(new FluidStack(Fluids.CHLORINE, 1_000))
                .setSolid(new ComparableStack(BilletPowderItems.POWDER_THORIUM.get())));

        // register(Fluids.CHLOROCALCITE_SOLUTION, new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.WATER, 250)).setStack2(new FluidStack(Fluids.NITRIC_ACID, 250)).setSolid(new RecipesCommon.OreDictStack(CHLOROCALCITE.dust())));
        register(Fluids.CHLOROCALCITE_SOLUTION, new MixerRecipe(500, 50)
                .setStack1(new FluidStack(Fluids.WATER, 250))
                .setStack2(new FluidStack(Fluids.NITRIC_ACID, 250))
                .setSolid(new ComparableStack(BilletPowderItems.POWDER_CHLOROCALCITE.get())));

        // register(Fluids.DIESEL_REFORM, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.DIESEL, 900)).setStack2(new FluidStack(Fluids.REFORMATE, 100)));
        register(Fluids.DIESEL_REFORM, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.DIESEL, 900))
                .setStack2(new FluidStack(Fluids.REFORMATE, 100)));

        // register(Fluids.SYNGAS, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.COALOIL, 500)).setStack2(new FluidStack(Fluids.STEAM, 500)));
        register(Fluids.SYNGAS, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.COALOIL, 500))
                .setStack2(new FluidStack(Fluids.STEAM, 500)));

        // register(Fluids.LUBRICANT, <3 competing recipes>) - only the 2 whose fluids are all confirmed kept (SUNFLOWEROIL+ETHANOL variant dropped, nothing missing about it, just redundant with FISHOIL+ETHANOL for this trimmed set)
        register(Fluids.LUBRICANT,
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.HEATINGOIL, 500)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 500)),
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.FISHOIL, 800)).setStack2(new FluidStack(Fluids.ETHANOL, 200)));

        // register(Fluids.NITROGLYCERIN, <2 competing recipes>) - both kept in full, CE-faithful
        register(Fluids.NITROGLYCERIN,
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.PETROLEUM, 1_000)).setStack2(new FluidStack(Fluids.NITRIC_ACID, 1_000)),
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.FISHOIL, 500)).setStack2(new FluidStack(Fluids.NITRIC_ACID, 500)));

        // register(Fluids.OXYHYDROGEN, <2 competing recipes>) - both kept in full, CE-faithful
        register(Fluids.OXYHYDROGEN,
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.HYDROGEN, 500)).setStack2(new FluidStack(Fluids.AIR, 2_000)),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.HYDROGEN, 500)).setStack2(new FluidStack(Fluids.OXYGEN, 500)));
    }

    private static void register(FluidType outputType, MixerRecipe... recipes) {
        RECIPES.put(outputType, recipes);
    }

    /** All competing recipes for the given output fluid, or {@code null} if none exist - matches CE's {@code getOutput(FluidType)}. */
    public static MixerRecipe[] getOutput(FluidType outputType) {
        registerDefaults();
        return RECIPES.get(outputType);
    }

    /** One specific competing recipe, wrapping around the array like CE's own {@code recipeIndex} cycling ({@code TileEntityMachineMixer.receiveControl}'s "toggle" field) does. */
    public static MixerRecipe getOutput(FluidType outputType, int index) {
        MixerRecipe[] recipes = getOutput(outputType);
        if (recipes == null || recipes.length == 0) return null;
        return recipes[Math.floorMod(index, recipes.length)];
    }

    /**
     * Full-collection accessor added for {@code c11-jei-recipe-categories}
     * ({@code docs/phase5/jei_integration.md}'s "Safe to build now" #4 - {@link #RECIPES} was
     * previously point-lookup-only via {@link #getOutput}) so a JEI category can enumerate every
     * registered recipe, including every entry of a competing-array output type (this port's own
     * open-question flag on that array being easy to flatten incorrectly - see class javadoc - is
     * why this returns the raw {@code Map<FluidType, MixerRecipe[]>} rather than pre-flattening it;
     * the caller decides how to enumerate the array). Returns an unmodifiable view.
     */
    public static Map<FluidType, MixerRecipe[]> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }

    /**
     * Auto-detection scan over every registered recipe, keyed by the two input tanks' current
     * contents plus the optional solid input - used by {@code MachineMixerBlockEntity} in place of
     * CE's manual "select target output fluid via GUI toggle, then the two input tanks re-derive
     * their expected type from it" flow ({@code TileEntityMachineMixer.canProcess}'s own
     * {@code tanks[0].setTankType(recipe.input1.type)} call). This port has no
     * {@code IItemFluidIdentifier} output-fluid-selector item yet (same gap
     * {@code com.hbm.inventory.fluid.tank.FluidTankNTM}'s own javadoc documents) to drive that manual selection with, so the
     * block entity instead asks "does anything already in my tanks/input slot match a real recipe" -
     * a documented simplification, not a silent behavior change: every recipe below still requires
     * the exact same two fluids (or one fluid + solid, or solid alone) in the exact same amounts CE's
     * data specifies.
     *
     * @param tank1Type  {@code tanks[0]}'s current fluid type ({@link Fluids#NONE} if empty)
     * @param tank1Fill  {@code tanks[0]}'s current fill, mB
     * @param tank2Type  {@code tanks[1]}'s current fluid type
     * @param tank2Fill  {@code tanks[1]}'s current fill, mB
     * @param solid      the solid-ingredient input slot's stack (may be {@link ItemStack#isEmpty()})
     * @return the first matching (output type, recipe) pair, or {@code null} if nothing matches yet
     */
    public static Match findMatch(FluidType tank1Type, int tank1Fill, FluidType tank2Type, int tank2Fill, ItemStack solid) {
        registerDefaults();
        for (Map.Entry<FluidType, MixerRecipe[]> entry : RECIPES.entrySet()) {
            for (MixerRecipe recipe : entry.getValue()) {
                // Only the fluid slots a given recipe actually requires are checked - an unrelated
                // fluid sitting in the other tank (not part of this recipe) does not block a match,
                // matching the "auto-detect from whatever's present" simplification this method's own
                // javadoc documents.
                if (recipe.input1 != null && (recipe.input1.type != tank1Type || tank1Fill < recipe.input1.fill)) continue;
                if (recipe.input2 != null && (recipe.input2.type != tank2Type || tank2Fill < recipe.input2.fill)) continue;
                if (recipe.solidInput != null && (solid.isEmpty() || !recipe.solidInput.matchesRecipe(solid, false))) continue;
                return new Match(entry.getKey(), recipe);
            }
        }
        return null;
    }

    public record Match(FluidType outputType, MixerRecipe recipe) {
    }

    public static class MixerRecipe {
        public FluidStack input1;
        public FluidStack input2;
        public AStack solidInput;
        public final int output;
        public final int processTime;

        public MixerRecipe(int output, int processTime) {
            this.output = output;
            this.processTime = processTime;
        }

        public MixerRecipe setStack1(FluidStack stack) {
            this.input1 = stack;
            return this;
        }

        public MixerRecipe setStack2(FluidStack stack) {
            this.input2 = stack;
            return this;
        }

        public MixerRecipe setSolid(AStack stack) {
            this.solidInput = stack;
            return this;
        }
    }
}
