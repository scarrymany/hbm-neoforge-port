package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.PyroOvenRecipes} (228 lines, read in full) - a
 * bespoke, pyrolytic-oven-only recipe shape ({@code duration}, optional fluid/item input, optional
 * fluid/item output). Kept as a plain hardcoded Java registration list rather than a JSON
 * {@code Recipe<?>}, matching {@link RefineryRecipes}/{@link CrystallizerRecipes}/
 * {@link MixerRecipes}'s established precedent: this port's own {@code AssemblerRecipe} javadoc
 * documents that {@link FluidStack} has no {@code Codec}/{@code StreamCodec} yet, and nearly every
 * recipe here needs a fluid input and/or output, so a JSON-datagen shape genuinely cannot be built
 * today without that cross-cutting prerequisite landing first (out of this task's scope per
 * {@code docs/phase7/mrec_01_ammopress_misc.md}'s "Recommended implementation shape").
 * <p>
 * <b>Lazy registration</b>: see {@link CrystallizerRecipes#registerDefaults()}'s javadoc for why
 * {@link #registerDefaults()} populates {@link #RECIPES} on first real lookup rather than eagerly
 * from a block/mod registration bootstrap chain - the same registry-not-populated-yet hazard
 * applies here (every entry below resolves a {@code DeferredItem.get()} for its item ingredient
 * and/or output).
 * <p>
 * <b>No port-side machine block/block-entity exists for the Pyrolytic Oven yet</b> (confirmed by
 * the research report's exhaustive grep) - this class is recipe <i>data</i> only, provided for
 * whichever future pass builds the actual machine, matching this port's established "recipe data
 * can land ahead of its machine" precedent (see {@link RefineryRecipes}'s own header on the same
 * point, and {@code docs/phase7/mrec_01_ammopress_misc.md}'s open question #6).
 * <p>
 * <b>Scope trim vs. CE (documented, not silent)</b> - CE's real file has 71 total recipes across
 * three families; this port implements the subset whose full item dependency chain is already
 * registered, per the research report's dependency check (with two corrections found while writing
 * this class, see below):
 * <ul>
 *     <li><b>Solid-fuel family (27 of CE's 71, all 26 {@code registerSFAuto(fluid)} one-liners plus
 *     the 1 {@code BALEFIRE} three-arg call) - NOT ported, correcting the research report.</b> The
 *     report's dependency check listed "{@code solid_fuel} item" as already registered/ready; a
 *     direct grep of this port's {@code items/} tree at implementation time found zero hits for a
 *     {@code solid_fuel} item anywhere (the only reference anywhere in this port is
 *     {@code ModRecipeProvider}'s own comment naming {@code solid_fuel} as one of several
 *     <i>not-yet-registered</i> conventional-explosives items). Since {@code solid_fuel} (and its
 *     balefire sibling {@code solid_fuel_bf}, separately confirmed absent by the report itself) is
 *     the sole *output* of all 27 solid-fuel recipes, none of them are ready-to-port - this is a
 *     genuine, not-yet-closed item gap, not silently dropped.</li>
 *     <li><b>Bedrock-ore roasting (30 of 71) - fully ported</b> as CE's own
 *     {@code for(BedrockOreType : VALUES)} loop, one iteration per {@link BedrockOreType}, 5
 *     {@code BedrockOreGrade} pairs per iteration (identical structure to CE's real loop, not
 *     hand-transcribed). {@link BedrockOreItems#get(BedrockOreType, BedrockOreGrade)} is this
 *     port's flattened-item equivalent of CE's {@code ItemBedrockOreNew.make(grade, type)}.</li>
 *     <li><b>Misc reaction chemistry (14 of 71) - 9 ported, 5 not</b>. Ported: both {@code STEAM}-
 *     from-coal syngas recipes (coal <i>gem</i> = vanilla {@code Items.COAL}, coal <i>dust</i> =
 *     {@link BilletPowderItems#POWDER_COAL} - confirmed against CE's own
 *     {@code OreDictManager}: {@code COAL.gem(Items.COAL)...dust(powder_coal)}), both
 *     {@code HYDROGEN}-from-coal heavy-oil recipes, both {@code HEAVYOIL}-from-coal coalgas
 *     recipes, the pure-fluid {@code GAS_COKER}->{@code REFORMGAS} and {@code GAS}->
 *     {@code HYDROGEN}+{@code ingot_graphite} recipes, and (a further correction to the report,
 *     which flagged this as an unresolved open question) the tungsten-carbide recipe
 *     ({@code SYNGAS} + tungsten dust -> {@code SPENTSTEAM} + {@code ingot_tungsten_carbide}):
 *     tungsten dust is registered as {@link BilletPowderItems#POWDER_TUNGSTEN} (confirmed against
 *     CE's {@code OreDictManager}: {@code W...dust(powder_tungsten)...}), so this recipe is ready
 *     after all. NOT ported: the 3 "coke gem" variants of the coal recipes above (CE's
 *     {@code ANY_COKE.gem()} resolves to CE's standalone {@code coke} item, which this port has not
 *     registered as a loose item - only the block form {@code block_coke_<type>} exists here), the
 *     biomass->syngas recipe ({@code biomass} item confirmed absent), and the any-tar->soot recipe
 *     ({@code oil_tar} input and {@code powder_ash}/{@code EnumAshType.SOOT} output both confirmed
 *     absent, same gap {@link RefineryRecipes} documents for {@code oil_tar}).</li>
 * </ul>
 * <b>Fluid-as-ingredient positions</b> below are plain {@link FluidStack}s, exactly like CE's own
 * shape (CE's {@code PyroOvenRecipe.in(FluidStack)}/{@code .out(FluidStack)}) - no simplification
 * needed here (unlike {@code AmmoPressRecipes}, which has to improvise a fluid-slot representation
 * for a machine that historically only ever accepted item-form fluid containers).
 */
public final class PyroOvenRecipes {

    private static final List<PyroOvenRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private PyroOvenRecipes() {
    }

    /** See class javadoc "Lazy registration". Idempotent, safe to call repeatedly. */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // ---- bedrock-ore roasting (CE: for(BedrockOreType type : VALUES) { 5 recipes.add() } - identical loop shape ----
        for (BedrockOreType type : BedrockOreType.VALUES) {
            roast(type, BedrockOreGrade.BASE, BedrockOreGrade.BASE_ROASTED);
            roast(type, BedrockOreGrade.PRIMARY, BedrockOreGrade.PRIMARY_ROASTED);
            roast(type, BedrockOreGrade.SULFURIC_BYPRODUCT, BedrockOreGrade.SULFURIC_ROASTED);
            roast(type, BedrockOreGrade.SOLVENT_BYPRODUCT, BedrockOreGrade.SOLVENT_ROASTED);
            roast(type, BedrockOreGrade.RAD_BYPRODUCT, BedrockOreGrade.RAD_ROASTED);
        }

        // ---- misc reaction chemistry (CE lines ~75-124 of PyroOvenRecipes.registerDefaults()) ----

        // syngas from coal (CE: STEAM 500 + COAL.gem()/.dust() -> SYNGAS 1000, duration 100)
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 500)).in(new ComparableStack(Items.COAL))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.STEAM, 500)).in(new ComparableStack(BilletPowderItems.POWDER_COAL.get()))
                .out(new FluidStack(Fluids.SYNGAS, 1_000)));

        // tungsten carbide from tungsten dust (CE: SYNGAS 2000 + W.dust() -> SPENTSTEAM 1000 + ingot_tungsten_carbide, duration 300)
        RECIPES.add(new PyroOvenRecipe(300)
                .in(new FluidStack(Fluids.SYNGAS, 2_000)).in(new ComparableStack(BilletPowderItems.POWDER_TUNGSTEN.get()))
                .out(new FluidStack(Fluids.SPENTSTEAM, 1_000)).out(new ItemStack(IngotNuggetItems.INGOT_TUNGSTEN_CARBIDE.get())));

        // heavyoil from coal (CE: HYDROGEN 500 + COAL.gem()/.dust() -> HEAVYOIL 1000, duration 100)
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 500)).in(new ComparableStack(Items.COAL))
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));
        RECIPES.add(new PyroOvenRecipe(100)
                .in(new FluidStack(Fluids.HYDROGEN, 500)).in(new ComparableStack(BilletPowderItems.POWDER_COAL.get()))
                .out(new FluidStack(Fluids.HEAVYOIL, 1_000)));

        // coalgas from coal (CE: HEAVYOIL 500 + COAL.gem()/.dust() -> COALGAS 1000, duration 50)
        RECIPES.add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(new ComparableStack(Items.COAL))
                .out(new FluidStack(Fluids.COALGAS, 1_000)));
        RECIPES.add(new PyroOvenRecipe(50)
                .in(new FluidStack(Fluids.HEAVYOIL, 500)).in(new ComparableStack(BilletPowderItems.POWDER_COAL.get()))
                .out(new FluidStack(Fluids.COALGAS, 1_000)));

        // refgas from coker gas (CE: GAS_COKER 4000 -> REFORMGAS 100, duration 60)
        RECIPES.add(new PyroOvenRecipe(60)
                .in(new FluidStack(Fluids.GAS_COKER, 4_000))
                .out(new FluidStack(Fluids.REFORMGAS, 100)));

        // hydrogen and graphite from natgas (CE: GAS 12000 -> HYDROGEN 8000 + ingot_graphite, duration 60)
        RECIPES.add(new PyroOvenRecipe(60)
                .in(new FluidStack(Fluids.GAS, 12_000))
                .out(new FluidStack(Fluids.HYDROGEN, 8_000)).out(new ItemStack(IngotNuggetItems.INGOT_GRAPHITE.get())));
    }

    private static void roast(BedrockOreType type, BedrockOreGrade rawGrade, BedrockOreGrade roastedGrade) {
        RECIPES.add(new PyroOvenRecipe(10)
                .in(new ComparableStack(BedrockOreItems.get(type, rawGrade).get()))
                .out(new FluidStack(Fluids.VITRIOL, 50))
                .out(new ItemStack(BedrockOreItems.get(type, roastedGrade).get())));
    }

    /** Full-collection accessor, matching {@link RefineryRecipes#getAllRefinery()}/{@link CrystallizerRecipes#getAllRecipes()}'s established shape for a future JEI category / machine block entity. */
    public static List<PyroOvenRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    public static class PyroOvenRecipe {
        public FluidStack inputFluid;
        public AStack inputItem;
        public FluidStack outputFluid;
        public ItemStack outputItem;
        public final int duration;

        public PyroOvenRecipe(int duration) {
            this.duration = duration;
        }

        public PyroOvenRecipe in(FluidStack stack) { this.inputFluid = stack; return this; }
        public PyroOvenRecipe in(AStack stack) { this.inputItem = stack; return this; }
        public PyroOvenRecipe out(FluidStack stack) { this.outputFluid = stack; return this; }
        public PyroOvenRecipe out(ItemStack stack) { this.outputItem = stack; return this; }
    }
}
