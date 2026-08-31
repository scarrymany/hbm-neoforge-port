package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CrystallizerRecipes} (385 lines, read in full) -
 * a bespoke, crystallizer-only recipe shape keyed by (input item, required acid/reagent fluid type),
 * per {@code docs/phase2/machines_shredder_assembler_crystallizer_mixer.md}'s "Recipe shape" analysis
 * ("its own bespoke class, not {@code GenericRecipe}"). Kept as a plain hardcoded Java registration
 * list rather than a JSON {@code Recipe<?>} for the same reason
 * {@code com.hbm.inventory.recipes.RefineryRecipes} (the concurrent oil-production-chain pass) stayed
 * bespoke: the real shape (a required-{@link FluidType} map key, a {@code productivity}/free-output
 * chance, a per-recipe input-count requirement) doesn't fit vanilla's {@code Recipe<RecipeInput>}
 * contract without a much larger custom-ingredient design this task's scope does not call for - see
 * {@link ProcessingRecipes}'s own javadoc for the fuller rationale, which applies identically here.
 * <p>
 * <b>CE's {@code ComparableStack} used {@code meta} for damage-value variants (e.g. wool color) -
 * this port's simplified {@link ComparableStack} has no {@code meta} field (see that class's own
 * header), so recipes keyed on a CE meta value collapse onto the plain item - not applicable to any
 * entry actually ported below, since every one already keys on a distinct real item in this port.</b>
 * <p>
 * <b>Scope trim vs. CE</b> (documented, not silent, matching this port's established precedent for
 * partial CE-data ports - see {@code RefineryRecipes}'s own header): CE's ~50-entry
 * {@code registerDefaults()} references dozens of items/blocks this port has not registered yet
 * (ore-dictionary-keyed entries via {@code OreDictManager}, {@code ItemBedrockOreNew}'s ~20-recipe
 * washing/roasting chain, {@code ItemChemicalDye}, {@code ModItems.oil_tar}/{@code pellet_charged}/
 * {@code chunk_ore}, several not-yet-ported blocks). Every recipe below is a real CE recipe (same
 * input, output, duration, acid type/amount, productivity) restricted to the subset whose input AND
 * output items are confirmed already registered in this port - see each recipe's inline comment for
 * its CE-recipe origin. The rest are a real, out-of-scope gap for whoever ports those item families
 * next, not silently dropped: nothing here invents a substitute item the way some earlier Phase 2
 * passes had to (e.g. {@code RefineryRecipes}' sulfur substitution) - recipes needing a missing item
 * are simply not ported yet.
 */
public final class CrystallizerRecipes {

    /** Default acid amount CE's own 2-arg {@code registerRecipe(input, recipe)} overload applies when no fluid is specified explicitly (always {@link Fluids#PEROXIDE}). */
    private static final int DEFAULT_ACID_AMOUNT = 500;

    private static final Map<Tuple.Pair<ComparableStack, FluidType>, CrystallizerRecipe> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CrystallizerRecipes() {
    }

    /**
     * Resolves one of this port's own blocks by registry name, exactly like
     * {@code OilDrillBaseBlockEntity#resolve(String)}: {@code com.hbm.blocks.OreBlocks}'s {@code ore(...)}
     * helper does not keep a named field per ore (confirmed - only a bare {@code registerAll()} loop
     * exists), so the only way to reference e.g. {@code ore_titanium} from outside that class is a
     * registry-name lookup. Safe here because {@link #registerDefaults()} only ever runs lazily (see
     * that method's own javadoc), long after {@link BuiltInRegistries#BLOCK} is fully populated - never
     * eagerly from a static initializer or a mod-construction-time registration call, when it would
     * resolve to {@code Blocks.AIR}/{@code null} instead (the registry is not populated yet at that point).
     */
    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * Populates {@link #RECIPES}. <b>Deliberately lazy</b>, not called from any block/item/mod
     * registration path: unlike {@link ProcessingRecipes} (JSON {@code Recipe<?>} data, resolved by
     * {@code RecipeManager} long after all registries are frozen, so registration-time references are
     * safe there), this class holds plain Java references to real {@link Block}/{@link ItemStack}
     * objects built directly from {@code DeferredItem.get()}/{@link BuiltInRegistries} lookups - both
     * of which throw/return a placeholder if called before NeoForge's {@code RegisterEvent} has fired
     * for the relevant registry, which happens strictly *after* every mod's constructor (where
     * {@code ModBlocks.register(modEventBus)}/{@code ModItems.register(modEventBus)} run) returns.
     * Calling this eagerly from a {@code registerAll()}-style bootstrap chained off block/mod
     * registration (the pattern {@code RefineryRecipes}, a concurrent sibling pass, uses) would silently
     * build every {@link ComparableStack} key around a null/placeholder item. Instead, {@link #getOutput}
     * below calls this itself on first use (idempotent, {@code synchronized} guarded) - by the time any
     * crystallizer block entity actually ticks in a running world, every registry is long since frozen.
     */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        final int baseTime = 600;
        final int utilityTime = 100;

        // ---- ore -> crystal (CE: IRON.ore()/GOLD.ore()/DIAMOND.ore()/REDSTONE.ore()/LAPIS.ore(), default PEROXIDE 500mB) ----
        register(new ComparableStack(Blocks.IRON_ORE), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_IRON.get()), baseTime, 0.05F));
        register(new ComparableStack(Blocks.GOLD_ORE), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_GOLD.get()), baseTime, 0.05F));
        register(new ComparableStack(Blocks.DIAMOND_ORE), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_DIAMOND.get()), baseTime, 0.05F));
        register(new ComparableStack(Blocks.REDSTONE_ORE), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_REDSTONE.get()), baseTime, 0.05F));
        register(new ComparableStack(Blocks.LAPIS_ORE), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_LAPIS.get()), baseTime, 0.05F));

        // ---- ore -> crystal requiring SULFURIC_ACID (CE: W.ore()/TI.ore()/SA326.ore()/CO.ore(), sulfur=500mB) ----
        register(new ComparableStack(hbmBlock("ore_titanium")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_TITANIUM.get()), baseTime, 0.05F));
        register(new ComparableStack(hbmBlock("ore_tungsten")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_TUNGSTEN.get()), baseTime, 0.05F));
        register(new ComparableStack(hbmBlock("ore_thorium")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_THORIUM.get()), baseTime, 0.05F));
        register(new ComparableStack(hbmBlock("ore_cobalt")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_COBALT.get()), baseTime, 0.05F));

        // ---- ore -> crystal, default acid (CE: CU.ore()/PB.ore()) ----
        register(new ComparableStack(hbmBlock("ore_copper")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_COPPER.get()), baseTime, 0.05F));
        register(new ComparableStack(hbmBlock("ore_lead")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_LEAD.get()), baseTime, 0.05F));

        // ---- misc item transmutations (CE lines ~78, 96-114) ----
        // registerRecipe(new ComparableStack(ModItems.powder_calcium), new CrystallizerRecipe(new ItemStack(ModItems.powder_cement, 8), utilityTime).prod(0.1F), new FluidStack(Fluids.REDMUD, 75));
        register(new ComparableStack(BilletPowderItems.POWDER_CALCIUM.get()), Fluids.REDMUD, 75,
                new CrystallizerRecipe(new ItemStack(BilletPowderItems.POWDER_CEMENT.get(), 8), utilityTime, 0.1F));
        // registerRecipe(new ComparableStack(Items.ROTTEN_FLESH), new CrystallizerRecipe(Items.LEATHER, utilityTime).prod(0.25F));
        register(new ComparableStack(Items.ROTTEN_FLESH), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(Items.LEATHER), utilityTime, 0.25F));
        // registerRecipe(DIAMOND.dust(), new CrystallizerRecipe(Items.DIAMOND, utilityTime)); - this port's nearest confirmed dust equivalent is powder_diamond
        register(new ComparableStack(BilletPowderItems.POWDER_DIAMOND.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(Items.DIAMOND), utilityTime, 0F));
        // registerRecipe(EMERALD.dust(), new CrystallizerRecipe(Items.EMERALD, utilityTime));
        register(new ComparableStack(BilletPowderItems.POWDER_EMERALD.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(Items.EMERALD), utilityTime, 0F));
        // registerRecipe(LAPIS.dust(), new CrystallizerRecipe(new ItemStack(Items.DYE, 1, 4), utilityTime)); - 1.21 lapis dye is its own item
        register(new ComparableStack(BilletPowderItems.POWDER_LAPIS.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(Items.LAPIS_LAZULI), utilityTime, 0F));
        // registerRecipe(new ComparableStack(ModItems.powder_semtex_mix), new CrystallizerRecipe(ModItems.ingot_semtex, baseTime));
        register(new ComparableStack(BilletPowderItems.POWDER_SEMTEX_MIX.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(IngotNuggetItems.INGOT_SEMTEX.get()), baseTime, 0F));
        // registerRecipe(new ComparableStack(ModItems.powder_desh_ready), new CrystallizerRecipe(ModItems.ingot_desh, baseTime));
        register(new ComparableStack(BilletPowderItems.POWDER_DESH_READY.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(IngotNuggetItems.INGOT_DESH.get()), baseTime, 0F));
    }

    private static void register(ComparableStack input, FluidType acidType, int acidAmount, CrystallizerRecipe recipe) {
        input.makeSingular();
        recipe.acidAmount = acidAmount;
        RECIPES.put(new Tuple.Pair<>(input, acidType), recipe);
    }

    /**
     * Matches CE's {@code getOutput(ItemStack, FluidType)}: exact-item lookup only (this port's
     * {@link ComparableStack} has no ore-dict/wildcard-meta fallback to try after a miss - see class
     * javadoc). Returns {@code null} on no match, same as CE (crystallizer recipes have a real
     * "no recipe" state, unlike the shredder's always-something-comes-out fallback).
     */
    public static CrystallizerRecipe getOutput(ItemStack stack, FluidType acidType) {
        registerDefaults();
        if (stack == null || stack.isEmpty()) return null;
        ComparableStack comp = new ComparableStack(stack).makeSingular();
        return RECIPES.get(new Tuple.Pair<>(comp, acidType));
    }

    /**
     * Full-collection accessor added for {@code c11-jei-recipe-categories}
     * ({@code docs/phase5/jei_integration.md}'s "Safe to build now" #4 - {@link #RECIPES} was
     * previously point-lookup-only via {@link #getOutput}) so a JEI category can enumerate every
     * registered recipe. Returns an unmodifiable view over the live, lazily-populated map - callers
     * must not assume the returned map is non-empty before {@link #registerDefaults()} has run at
     * least once (this method runs it itself, same as {@link #getOutput}).
     */
    public static Map<Tuple.Pair<ComparableStack, FluidType>, CrystallizerRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }

    public static class CrystallizerRecipe {
        public final ItemStack output;
        public final int duration;
        public final int itemAmount;
        public int acidAmount = DEFAULT_ACID_AMOUNT;
        public final float productivity;

        public CrystallizerRecipe(ItemStack output, int duration, float productivity) {
            this(output, duration, 1, productivity);
        }

        public CrystallizerRecipe(ItemStack output, int duration, int itemAmount, float productivity) {
            this.output = output;
            this.duration = duration;
            this.itemAmount = itemAmount;
            this.productivity = productivity;
        }
    }
}
