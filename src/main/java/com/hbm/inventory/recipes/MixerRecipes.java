package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
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
 * {@code RefineryRecipes}' precedent): CE's 40 mixer output-fluid keys (50 {@code MixerRecipe}
 * objects) reference several items/fluids this port hasn't registered yet. Every recipe below is a
 * real CE recipe restricted to the subset whose fluids AND solid input item (where present) are
 * confirmed already registered in this port - see each recipe's inline CE-origin comment.
 * {@link Fluids#LUBRICANT}/{@link Fluids#NITROGLYCERIN}/{@link Fluids#OXYHYDROGEN}/
 * {@link Fluids#FRACKSOL}/{@link Fluids#SOLVENT}/{@link Fluids#BIOFUEL} are deliberately kept as
 * their real CE multi-recipe competing arrays (not flattened to one), directly addressing the
 * research doc's "Open questions" flag on this array being easy to flatten incorrectly: a machine
 * implementer copying this file should treat the multi-recipe-per-fluid array as load-bearing, not an
 * accident of this data set.
 * <p>
 * <b>{@code mrec-03-silex-misc} pass</b> (see {@code docs/phase7/mrec_03_silex_misc.md}): extended
 * this file with 20 more of CE's 29 previously-unported output-fluid keys (25 more
 * {@code MixerRecipe} objects), using {@link PlateCrystalWasteItems#CRYSTAL_SULFUR}/
 * {@link PlateCrystalWasteItems#CRYSTAL_NITER}/{@link PlateCrystalWasteItems#CRYSTAL_FLUORITE} as the
 * standing substitution for CE's plain ore-dictionary sulfur/niter/fluorite dust lookups - the same
 * substitution this port's own {@code SILEXRecipes}/{@code CentrifugeRecipes} already established,
 * which resolves the research report's open question #3 (this class's own previous javadoc called
 * {@code FRACKSOL}'s second recipe blocked on "a sulfur-dust item this port hasn't registered yet" -
 * not true anymore, corrected here). {@code ENDERJUICE}'s CE {@code DIAMOND.dust()} solid input is
 * ported as {@link BilletPowderItems#POWDER_DIAMOND}, not {@link Items#DIAMOND} (the gem) - matching
 * {@link CrystallizerRecipes}'s own already-established {@code DIAMOND.dust()} -&gt;
 * {@code POWDER_DIAMOND} substitution (see that class's {@code POWDER_DIAMOND} entry), a discrepancy
 * from the research report's suggested {@code Items.DIAMOND} substitution.
 * <p>
 * <b>Still blocked</b> (9 keys, unregistered item families, matching the research report):
 * {@code COLLOID} ({@code ModItems.dust}, a generic undifferentiated-dust item), {@code IONGEL}/
 * {@code SCHRABIDIC} ({@code pellet_charged}), {@code FULLERENE}/{@code LYE} ({@code powder_ash}
 * family), {@code PETROIL_LEADED}/{@code GASOLINE_LEADED}/{@code COALGAS_LEADED}
 * ({@code fuel_additive}), {@code BITUMEN} ({@code oil_tar}/{@code EnumTarType}, same gap
 * {@code RefineryRecipes} already documents). <b>Correction to the research report</b>: CE's
 * {@code ALUMINA} key has a real 2-recipe pair, but only the first ({@code F.dust()} -&gt;
 * {@code CRYSTAL_FLUORITE}) is portable - the report claimed CE's second recipe's
 * {@code chunk_ore}/{@code EnumChunkType.CRYOLITE} item is "registered", but only the bare
 * {@code EnumChunkType} enum exists (confirmed against {@code BlockResourceStone}'s and
 * {@code OreBlocks}'s own javadoc, both independently citing {@code chunk_ore} as not yet a real
 * item) - so {@code ALUMINA} below is ported single-recipe, same documented-partial treatment as
 * {@code LUBRICANT}'s CE-3-recipe/ported-2 trim.
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

        // ==================== mrec-03-silex-misc additions ====================
        // See class javadoc "mrec-03-silex-misc pass" for the CRYSTAL_SULFUR/CRYSTAL_NITER/
        // CRYSTAL_FLUORITE substitution rationale and the ALUMINA/ENDERJUICE discrepancy notes.

        // register(Fluids.COOLANT, new MixerRecipe(2_000, 50).setStack1(new FluidStack(Fluids.WATER, 1_800)).setSolid(new RecipesCommon.OreDictStack(KNO.dust())));
        register(Fluids.COOLANT, new MixerRecipe(2_000, 50)
                .setStack1(new FluidStack(Fluids.WATER, 1_800))
                .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get())));

        // register(Fluids.FRACKSOL, <2 competing recipes>) - both ported (see javadoc: sulfur-dust blocker no longer applies)
        register(Fluids.FRACKSOL,
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.SULFURIC_ACID, 900)).setStack2(new FluidStack(Fluids.PETROLEUM, 100)),
                new MixerRecipe(1_000, 20).setStack1(new FluidStack(Fluids.WATER, 1_000)).setStack2(new FluidStack(Fluids.PETROLEUM, 100))
                        .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get())));

        // register(Fluids.ENDERJUICE, new MixerRecipe(100, 100).setStack1(new FluidStack(Fluids.XPJUICE, 500)).setSolid(new RecipesCommon.OreDictStack(DIAMOND.dust()))); - DIAMOND.dust() -> POWDER_DIAMOND, see javadoc
        register(Fluids.ENDERJUICE, new MixerRecipe(100, 100)
                .setStack1(new FluidStack(Fluids.XPJUICE, 500))
                .setSolid(new ComparableStack(BilletPowderItems.POWDER_DIAMOND.get())));

        // register(Fluids.SALIENT, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.SEEDSLURRY, 500)).setStack2(new FluidStack(Fluids.BLOOD, 500)));
        register(Fluids.SALIENT, new MixerRecipe(1_000, 20)
                .setStack1(new FluidStack(Fluids.SEEDSLURRY, 500))
                .setStack2(new FluidStack(Fluids.BLOOD, 500)));

        // register(Fluids.PHOSGENE, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.UNSATURATEDS, 500)).setStack2(new FluidStack(Fluids.CHLORINE, 500)));
        register(Fluids.PHOSGENE, new MixerRecipe(1_000, 20)
                .setStack1(new FluidStack(Fluids.UNSATURATEDS, 500))
                .setStack2(new FluidStack(Fluids.CHLORINE, 500)));

        // register(Fluids.MUSTARDGAS, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.REFORMGAS, 750)).setStack2(new FluidStack(Fluids.CHLORINE, 250)).setSolid(new RecipesCommon.OreDictStack(S.dust())));
        register(Fluids.MUSTARDGAS, new MixerRecipe(1_000, 20)
                .setStack1(new FluidStack(Fluids.REFORMGAS, 750))
                .setStack2(new FluidStack(Fluids.CHLORINE, 250))
                .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get())));

        // register(Fluids.EGG, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.RADIOSOLVENT, 500)).setSolid(new ComparableStack(Items.EGG)));
        register(Fluids.EGG, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.RADIOSOLVENT, 500))
                .setSolid(new ComparableStack(Items.EGG)));

        // register(Fluids.SOLVENT, <4 competing recipes>) - all 4 ported, CE-faithful
        register(Fluids.SOLVENT,
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.NAPHTHA, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.NAPHTHA_CRACK, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.NAPHTHA_DS, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)),
                new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.NAPHTHA_COKER, 500)).setStack2(new FluidStack(Fluids.AROMATICS, 500)));

        // register(Fluids.SULFURIC_ACID, new MixerRecipe(500, 50).setStack1(new FluidStack(Fluids.PEROXIDE, 800)).setSolid(new RecipesCommon.OreDictStack(S.dust())));
        register(Fluids.SULFURIC_ACID, new MixerRecipe(500, 50)
                .setStack1(new FluidStack(Fluids.PEROXIDE, 800))
                .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get())));

        // register(Fluids.NITRIC_ACID, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.SULFURIC_ACID, 500)).setSolid(new RecipesCommon.OreDictStack(KNO.dust())));
        register(Fluids.NITRIC_ACID, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.SULFURIC_ACID, 500))
                .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get())));

        // register(Fluids.RADIOSOLVENT, new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.REFORMGAS, 750)).setStack2(new FluidStack(Fluids.CHLORINE, 250)));
        register(Fluids.RADIOSOLVENT, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.REFORMGAS, 750))
                .setStack2(new FluidStack(Fluids.CHLORINE, 250)));

        // register(Fluids.PETROIL, new MixerRecipe(1_000, 30).setStack1(new FluidStack(Fluids.RECLAIMED, 800)).setStack2(new FluidStack(Fluids.LUBRICANT, 200)));
        register(Fluids.PETROIL, new MixerRecipe(1_000, 30)
                .setStack1(new FluidStack(Fluids.RECLAIMED, 800))
                .setStack2(new FluidStack(Fluids.LUBRICANT, 200)));

        // register(Fluids.BIOFUEL, <2 competing recipes>) - both ported, CE-faithful (note the differing output amounts, 250 vs 200 - not a typo, preserved from CE)
        register(Fluids.BIOFUEL,
                new MixerRecipe(250, 20).setStack1(new FluidStack(Fluids.FISHOIL, 500)).setStack2(new FluidStack(Fluids.WOODOIL, 500)),
                new MixerRecipe(200, 20).setStack1(new FluidStack(Fluids.SUNFLOWEROIL, 500)).setStack2(new FluidStack(Fluids.WOODOIL, 500)));

        // register(Fluids.DIESEL_CRACK_REFORM, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.DIESEL_CRACK, 900)).setStack2(new FluidStack(Fluids.REFORMATE, 100)));
        register(Fluids.DIESEL_CRACK_REFORM, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.DIESEL_CRACK, 900))
                .setStack2(new FluidStack(Fluids.REFORMATE, 100)));

        // register(Fluids.KEROSENE_REFORM, new MixerRecipe(1_000, 50).setStack1(new FluidStack(Fluids.KEROSENE, 900)).setStack2(new FluidStack(Fluids.REFORMATE, 100)));
        register(Fluids.KEROSENE_REFORM, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.KEROSENE, 900))
                .setStack2(new FluidStack(Fluids.REFORMATE, 100)));

        // register(Fluids.CHLOROCALCITE_MIX, new MixerRecipe(1000, 50).setStack1(new FluidStack(Fluids.CHLOROCALCITE_SOLUTION, 500)).setStack2(new FluidStack(Fluids.SULFURIC_ACID, 500)).setSolid(new ComparableStack(ModItems.powder_flux)));
        register(Fluids.CHLOROCALCITE_MIX, new MixerRecipe(1_000, 50)
                .setStack1(new FluidStack(Fluids.CHLOROCALCITE_SOLUTION, 500))
                .setStack2(new FluidStack(Fluids.SULFURIC_ACID, 500))
                .setSolid(new ComparableStack(BilletPowderItems.POWDER_FLUX.get())));

        // register(Fluids.PHEROMONE_M, new MixerRecipe(2000, 10).setStack1(new FluidStack(Fluids.PHEROMONE, 1500)).setStack2(new FluidStack(Fluids.BLOOD, 500)).setSolid(new ComparableStack(ModItems.pill_herbal)));
        register(Fluids.PHEROMONE_M, new MixerRecipe(2_000, 10)
                .setStack1(new FluidStack(Fluids.PHEROMONE, 1_500))
                .setStack2(new FluidStack(Fluids.BLOOD, 500))
                .setSolid(new ComparableStack(hbmItem("pill_herbal"))));

        // register(Fluids.BAUXITE_SOLUTION, new MixerRecipe(300, 80).setStack1(new FluidStack(Fluids.LYE, 50)).setSolid(new ComparableStack(ModBlocks.stone_resource, 1, BAUXITE.ordinal())));
        register(Fluids.BAUXITE_SOLUTION, new MixerRecipe(300, 80)
                .setStack1(new FluidStack(Fluids.LYE, 50))
                .setSolid(new ComparableStack(hbmItem("stone_resource_bauxite"))));

        // register(Fluids.ALUMINA, <2 competing recipes>) - only the first is portable, see class javadoc ALUMINA/chunk_ore note
        register(Fluids.ALUMINA,
                new MixerRecipe(200, 40).setStack1(new FluidStack(Fluids.SODIUM_ALUMINATE, 150))
                        .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 3)));

        // register(Fluids.PERFLUOROMETHYL, new MixerRecipe(1000, 20).setStack1(new FluidStack(Fluids.PETROLEUM, 1000)).setStack2(new FluidStack(Fluids.UNSATURATEDS, 500)).setSolid(new RecipesCommon.OreDictStack(F.dust())));
        register(Fluids.PERFLUOROMETHYL, new MixerRecipe(1_000, 20)
                .setStack1(new FluidStack(Fluids.PETROLEUM, 1_000))
                .setStack2(new FluidStack(Fluids.UNSATURATEDS, 500))
                .setSolid(new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get())));
    }

    /**
     * Resolves one of this port's own items by registry name, matching
     * {@code CrucibleRecipes#blockIcon(String)}'s/{@code CrystallizerRecipes#hbmBlock(String)}'s
     * already-established lazy-lookup pattern (see either method's own javadoc for the full safety
     * reasoning) - safe here only because this method is only ever reachable through
     * {@link #registerDefaults()}, which is itself only ever invoked lazily (see class javadoc "Lazy
     * registration"), long after every item {@code RegisterEvent} has fired. Used for items this
     * port's per-family item classes ({@code FoodItems}, {@code GenericBlocks}) register in a loop
     * without keeping a named field per entry - {@code pill_herbal} and {@code stone_resource_bauxite}
     * below are exactly that shape.
     */
    private static Item hbmItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
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
