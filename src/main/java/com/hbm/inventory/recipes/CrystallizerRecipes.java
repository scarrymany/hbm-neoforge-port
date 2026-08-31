package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import com.hbm.main.MainRegistry;
import com.hbm.util.Tuple;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.hbm.items.special.BedrockOreGrade.*;

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

    /** Item-registry counterpart to {@link #hbmBlock(String)} - same lazy-safety rationale, used
     * below to resolve {@code chemical_dye_*} (registered per-color by {@code MachineItems}, which
     * exposes no public {@code EnumChemDye -> DeferredItem} map to reference directly) and
     * {@code ingot_schraranium} (a {@code register(...)} call in {@code IngotNuggetItems}, not one of
     * that class's {@code registerIngot(...)}-family helpers, so no uniformly-named field lookup is
     * needed - this is simply the same cross-package-reference idiom already established by
     * {@code RockMillRecipes#resolveItem}/{@code ChemPlantRecipes} for the identical situation). */
    private static Item hbmItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /** {@code ItemBedrockOreNew.make(grade, type)} equivalent for a recipe *input* - CE's dense
     * (type, grade) cross product is this port's {@link BedrockOreItems#get}, see that class's own
     * javadoc for the 6x26=156 flattening rationale. */
    private static ComparableStack bIn(BedrockOreType type, BedrockOreGrade grade) {
        return new ComparableStack(BedrockOreItems.get(type, grade).get());
    }

    /** Same lookup as {@link #bIn}, wrapped as an output {@link ItemStack} instead. */
    private static ItemStack bOut(BedrockOreType type, BedrockOreGrade grade) {
        return new ItemStack(BedrockOreItems.get(type, grade).get());
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

        // ==================== mrec-10-crystallizer-misc additions ====================
        // Ported per docs/phase7/mrec_10_crystallizer_misc.md's "ready to port now" catalog (CE
        // CrystallizerRecipes.java, upstream/hbm-ce, lines 56-219 read in full). CE origin cited per
        // entry/group below; entries the report marked blocked (missing item/block) are NOT ported -
        // see that file's stillBlocked-equivalent notes and this task's own final report.
        final int mixingTime = 20;

        // ---- ore -> crystal, remaining flat entries (CE registerDefaults(), the COAL/U/S/KNO/AL/F/
        // BE/SA326/oreRareEarth/oreCinnabar/ore_nether_fire/ore_tikite/SRN call sites) ----
        // registerRecipe(COAL.ore(), new CrystallizerRecipe(ModItems.crystal_coal, baseTime).prod(0.05F));
        register(new ComparableStack(Blocks.COAL_ORE), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_COAL.get()), baseTime, 0.05F));
        // registerRecipe(U.ore(), new CrystallizerRecipe(ModItems.crystal_uranium, baseTime).prod(0.05F), sulfur);
        // NOTE: this port's ore_uranium is a BlockOutgas (radioactive-gas variant of BlockNTMOre), but
        // it is still the real registered block under that id - CE's U.ore() resolves to the same block.
        register(new ComparableStack(hbmBlock("ore_uranium")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_URANIUM.get()), baseTime, 0.05F));
        // registerRecipe(S.ore(), new CrystallizerRecipe(ModItems.crystal_sulfur, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_sulfur")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get()), baseTime, 0.05F));
        // registerRecipe(KNO.ore(), new CrystallizerRecipe(ModItems.crystal_niter, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_niter")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_NITER.get()), baseTime, 0.05F));
        // registerRecipe(AL.ore(), new CrystallizerRecipe(ModItems.crystal_aluminium, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_aluminium")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_ALUMINIUM.get()), baseTime, 0.05F));
        // registerRecipe(F.ore(), new CrystallizerRecipe(ModItems.crystal_fluorite, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_fluorite")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get()), baseTime, 0.05F));
        // registerRecipe(BE.ore(), new CrystallizerRecipe(ModItems.crystal_beryllium, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_beryllium")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_BERYLLIUM.get()), baseTime, 0.05F));
        // registerRecipe(SA326.ore(), new CrystallizerRecipe(ModItems.crystal_schrabidium, baseTime).prod(0.05F), sulfur);
        register(new ComparableStack(hbmBlock("ore_schrabidium")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_SCHRABIDIUM.get()), baseTime, 0.05F));
        // registerRecipe("oreRareEarth", new CrystallizerRecipe(ModItems.crystal_rare, baseTime).prod(0.05F), sulfur);
        // ore-dict key substituted with the concrete block it names in this port (no ore-dict system
        // here - see class javadoc), matching the already-established substitution pattern.
        register(new ComparableStack(hbmBlock("ore_rare")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_RARE.get()), baseTime, 0.05F));
        // registerRecipe("oreCinnabar", new CrystallizerRecipe(ModItems.crystal_cinnabar, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_cinnabar")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_CINNABAR.get()), baseTime, 0.05F));
        // registerRecipe(new ComparableStack(ModBlocks.ore_nether_fire), new CrystallizerRecipe(ModItems.crystal_phosphorus, baseTime).prod(0.05F));
        register(new ComparableStack(hbmBlock("ore_nether_fire")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_PHOSPHORUS.get()), baseTime, 0.05F));
        // registerRecipe(new ComparableStack(ModBlocks.ore_tikite), new CrystallizerRecipe(ModItems.crystal_trixite, baseTime).prod(0.05F), sulfur);
        register(new ComparableStack(hbmBlock("ore_tikite")), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_TRIXITE.get()), baseTime, 0.05F));
        // registerRecipe(SRN.ingot(), new CrystallizerRecipe(ModItems.crystal_schraranium, baseTime).prod(0.05F));
        // ingot_schraranium is IngotNuggetItems.INGOT_SCHRARANIUM (a bespoke register(...) call, not
        // one of that class's registerIngot(...) helpers - still a real, confirmed item).
        register(new ComparableStack(IngotNuggetItems.INGOT_SCHRARANIUM.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_SCHRARANIUM.get()), baseTime, 0.05F));

        // ---- misc item/utility transmutations (CE registerDefaults(), the KEY_SAND/SI/BORAX/DYE-15/
        // BONE/powder_meteorite/powder_impure_osmiridium/KEY_SAND-to-clay call sites) ----
        // registerRecipe(KEY_SAND, new CrystallizerRecipe(ModItems.ingot_fiberglass, utilityTime).prod(0.15F));
        // KEY_SAND is CE's ore-dict "sand" key, substituted with vanilla sand directly (see class javadoc).
        register(new ComparableStack(Blocks.SAND), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(IngotNuggetItems.INGOT_FIBERGLASS.get()), utilityTime, 0.15F));
        // registerRecipe(SI.ingot(), new CrystallizerRecipe(new ItemStack(Items.QUARTZ, 2), utilityTime).prod(0.1F), new FluidStack(Fluids.OXYGEN, 250));
        register(new ComparableStack(IngotNuggetItems.INGOT_SILICON.get()), Fluids.OXYGEN, 250,
                new CrystallizerRecipe(new ItemStack(Items.QUARTZ, 2), utilityTime, 0.1F));
        // registerRecipe(BORAX.dust(), new CrystallizerRecipe(new ItemStack(ModItems.powder_boron_tiny, 3), baseTime).prod(0.25F), sulfur);
        register(new ComparableStack(BilletPowderItems.POWDER_BORAX.get()), Fluids.SULFURIC_ACID, 500,
                new CrystallizerRecipe(new ItemStack(BilletPowderItems.POWDER_BORON_TINY.get(), 3), baseTime, 0.25F));
        // registerRecipe(new ComparableStack(Items.DYE, 1, 15), new CrystallizerRecipe(new ItemStack(Items.SLIME_BALL, 4), mixingTime), new FluidStack(Fluids.SULFURIC_ACID, 250));
        // 1.21: dye color 15 (white) is its own item, Items.WHITE_DYE.
        register(new ComparableStack(Items.WHITE_DYE), Fluids.SULFURIC_ACID, 250,
                new CrystallizerRecipe(new ItemStack(Items.SLIME_BALL, 4), mixingTime, 0F));
        // registerRecipe(new ComparableStack(Items.BONE), new CrystallizerRecipe(new ItemStack(Items.SLIME_BALL, 16), mixingTime), new FluidStack(Fluids.SULFURIC_ACID, 1_000));
        register(new ComparableStack(Items.BONE), Fluids.SULFURIC_ACID, 1_000,
                new CrystallizerRecipe(new ItemStack(Items.SLIME_BALL, 16), mixingTime, 0F));
        // registerRecipe(new ComparableStack(ModItems.powder_meteorite), new CrystallizerRecipe(ModItems.fragment_meteorite, utilityTime));
        register(new ComparableStack(BilletPowderItems.POWDER_METEORITE.get()), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get()), utilityTime, 0F));
        // registerRecipe(new ComparableStack(ModItems.powder_impure_osmiridium), new CrystallizerRecipe(ModItems.crystal_osmiridium, baseTime), new FluidStack(Fluids.SCHRABIDIC, 1_000));
        register(new ComparableStack(BilletPowderItems.POWDER_IMPURE_OSMIRIDIUM.get()), Fluids.SCHRABIDIC, 1_000,
                new CrystallizerRecipe(new ItemStack(PlateCrystalWasteItems.CRYSTAL_OSMIRIDIUM.get()), baseTime, 0F));
        // registerRecipe(KEY_SAND, new CrystallizerRecipe(Blocks.CLAY, 20), new FluidStack(Fluids.COLLOID, 1_000));
        register(new ComparableStack(Blocks.SAND), Fluids.COLLOID, 1_000,
                new CrystallizerRecipe(new ItemStack(Blocks.CLAY), mixingTime, 0F));

        // registerRecipe(new ComparableStack(ModBlocks.stone_gneiss), new CrystallizerRecipe(ModItems.powder_lithium, utilityTime).prod(0.25F));
        // CORRECTION vs. this task's own research report (docs/phase7/mrec_10_crystallizer_misc.md
        // marked this "Blocked"): stone_gneiss IS a real registered block in this port
        // (com.hbm.blocks.OreBlocks#registerBlock("stone_gneiss", ...), confirmed by direct read and
        // by RockMillRecipes' own already-working resolveItem("stone_gneiss") reference) - ready.
        register(new ComparableStack(hbmBlock("stone_gneiss")), Fluids.PEROXIDE, DEFAULT_ACID_AMOUNT,
                new CrystallizerRecipe(new ItemStack(BilletPowderItems.POWDER_LITHIUM.get()), utilityTime, 0.25F));

        // registerRecipe(CD.dust(), new CrystallizerRecipe(new ItemStack(ModItems.ingot_rubber, 16), utilityTime), new FluidStack(Fluids.FISHOIL, 4_000));
        register(new ComparableStack(BilletPowderItems.POWDER_CADMIUM.get()), Fluids.FISHOIL, 4_000,
                new CrystallizerRecipe(new ItemStack(IngotNuggetItems.INGOT_RUBBER.get(), 16), utilityTime, 0F));

        // ---- dye loop (CE lines 178-186): 6 dust materials x 3 reagent fluids -> chemical_dye colors ----
        // chemical_dye_* items are registered per-color by MachineItems#registerChemicalDye (no public
        // EnumChemDye -> DeferredItem map exposed), resolved here the same way hbmItem resolves any
        // other cross-package registry id.
        for (FluidType dyeReagent : new FluidType[]{Fluids.WOODOIL, Fluids.FISHOIL, Fluids.LIGHTOIL}) {
            register(new ComparableStack(BilletPowderItems.POWDER_COAL.get()), dyeReagent, 100,
                    new CrystallizerRecipe(new ItemStack(hbmItem("chemical_dye_black"), 4), mixingTime, 0.15F));
            register(new ComparableStack(BilletPowderItems.POWDER_TITANIUM.get()), dyeReagent, 100,
                    new CrystallizerRecipe(new ItemStack(hbmItem("chemical_dye_white"), 4), mixingTime, 0.15F));
            register(new ComparableStack(BilletPowderItems.POWDER_IRON.get()), dyeReagent, 100,
                    new CrystallizerRecipe(new ItemStack(hbmItem("chemical_dye_red"), 4), mixingTime, 0.15F));
            register(new ComparableStack(BilletPowderItems.POWDER_TUNGSTEN.get()), dyeReagent, 100,
                    new CrystallizerRecipe(new ItemStack(hbmItem("chemical_dye_yellow"), 4), mixingTime, 0.15F));
            register(new ComparableStack(BilletPowderItems.POWDER_COPPER.get()), dyeReagent, 100,
                    new CrystallizerRecipe(new ItemStack(hbmItem("chemical_dye_green"), 4), mixingTime, 0.15F));
            register(new ComparableStack(BilletPowderItems.POWDER_COBALT.get()), dyeReagent, 100,
                    new CrystallizerRecipe(new ItemStack(hbmItem("chemical_dye_blue"), 4), mixingTime, 0.15F));
        }

        // ---- bedrock-ore washing/roasting/centrifuging chain (CE lines 122-176) ----
        // Verbatim transcription of CE's nested loop: ItemBedrockOreNew.make(grade, type) ->
        // BedrockOreItems.get(type, grade).get() is the only substitution (see bIn/bOut above). 37
        // recipes per BedrockOreType x 6 types = 222 runtime recipes - by far the largest sub-pattern
        // in this class; see docs/phase7/mrec_10_crystallizer_misc.md for the full derivation.
        final int bedrock = 200;
        final int washing = 100;
        for (BedrockOreType type : BedrockOreType.VALUES) {
            register(bIn(type, BASE), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, BASE_WASHED), washing, 0F));
            register(bIn(type, BASE_ROASTED), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, BASE_WASHED), washing, 0F));

            register(bIn(type, PRIMARY), Fluids.SULFURIC_ACID, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SULFURIC), bedrock, 0F));
            register(bIn(type, PRIMARY_ROASTED), Fluids.SULFURIC_ACID, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SULFURIC), bedrock, 0F));

            register(bIn(type, PRIMARY), Fluids.SOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SOLVENT), bedrock, 0F));
            register(bIn(type, PRIMARY_ROASTED), Fluids.SOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SOLVENT), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSULFURIC), Fluids.SOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SOLVENT), bedrock, 0F));

            register(bIn(type, PRIMARY), Fluids.RADIOSOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_RAD), bedrock, 0F));
            register(bIn(type, PRIMARY_ROASTED), Fluids.RADIOSOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_RAD), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSULFURIC), Fluids.RADIOSOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_RAD), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSOLVENT), Fluids.RADIOSOLVENT, 250, new CrystallizerRecipe(bOut(type, PRIMARY_RAD), bedrock, 0F));

            final int sulf = 4;
            register(bIn(type, SULFURIC_BYPRODUCT), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, SULFURIC_WASHED), washing, sulf, 0F));
            register(bIn(type, SULFURIC_ROASTED), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, SULFURIC_WASHED), washing, sulf, 0F));
            register(bIn(type, SULFURIC_ARC), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, SULFURIC_WASHED), washing, sulf, 0F));

            final int solv = 4;
            register(bIn(type, SOLVENT_BYPRODUCT), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, SOLVENT_WASHED), washing, solv, 0F));
            register(bIn(type, SOLVENT_ROASTED), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, SOLVENT_WASHED), washing, solv, 0F));
            register(bIn(type, SOLVENT_ARC), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, SOLVENT_WASHED), washing, solv, 0F));

            final int rad = 4;
            register(bIn(type, RAD_BYPRODUCT), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, RAD_WASHED), washing, rad, 0F));
            register(bIn(type, RAD_ROASTED), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, RAD_WASHED), washing, rad, 0F));
            register(bIn(type, RAD_ARC), Fluids.WATER, 250, new CrystallizerRecipe(bOut(type, RAD_WASHED), washing, rad, 0F));

            register(bIn(type, PRIMARY), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_ROASTED), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_SULFURIC), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSULFURIC), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_SOLVENT), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSOLVENT), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_RAD), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));
            register(bIn(type, PRIMARY_NORAD), Fluids.HYDROGEN, 250, new CrystallizerRecipe(bOut(type, PRIMARY_FIRST), bedrock, 0F));

            register(bIn(type, PRIMARY), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_ROASTED), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_SULFURIC), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSULFURIC), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_SOLVENT), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_NOSOLVENT), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_RAD), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));
            register(bIn(type, PRIMARY_NORAD), Fluids.CHLORINE, 250, new CrystallizerRecipe(bOut(type, PRIMARY_SECOND), bedrock, 0F));

            register(bIn(type, CRUMBS), Fluids.SLOP, 1000, new CrystallizerRecipe(bOut(type, BASE), bedrock, 64, 0F));
        }
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
