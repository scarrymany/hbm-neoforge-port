package com.hbm.inventory.recipes.machine;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.special.BedrockOreGrade;
import com.hbm.items.special.BedrockOreItems;
import com.hbm.items.special.BedrockOreType;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recipe data for the Combination Furnace, ported from CE's {@code com.hbm.inventory.recipes.
 * CombinationRecipes} (192 ln, read in full - {@code docs/phase7/mrec_06_soldering_misc.md}). CE's
 * real shape is a {@code HashMap<Object, Pair<ItemStack, FluidStack>>} keyed by either a
 * {@code ComparableStack} or a legacy ore-dict alias string, single-item-key lookup, up to one
 * item output and one fluid output (both independently nullable) - no duration/power field of its
 * own, since CE's consuming block entity ({@code TileEntityFurnaceCombination}, confirmed absent in
 * this port) is a heat-accumulator machine whose processing speed is a machine-level constant, not
 * per-recipe data. Follows the same "port now, JSON-override later" plain-static-table shape
 * {@code RefineryRecipes}/{@code com.hbm.inventory.recipes.chem.ChemPlantRecipes}/{@link PUREXRecipes}
 * already established, keyed on this port's {@link AStack} hierarchy (linear-scan {@code matchesRecipe}
 * lookup, same as {@code com.hbm.inventory.recipes.chem.CentrifugeRecipes}) so both exact-item
 * ({@link ComparableStack}) and tag ({@link OreDictStack}) keys work the same way CE's dual
 * {@code ComparableStack}/ore-dict-string keying did.
 *
 * <p><b>No {@code MachineFurnaceCombination} block/block-entity exists in this port yet either</b>
 * (confirmed absent by the research pass) - this class is pure recipe data for whichever future task
 * builds that machine to consume (via {@link #getOutput(ItemStack)}/{@link #getAll()}, the same
 * defensive-lazy-registration accessor pattern {@code RefineryRecipes#getAllRefinery()}/
 * {@link PUREXRecipes#getAll()} already establish), so no eager bootstrap call needs wiring into any
 * shared aggregator file today.</p>
 *
 * <p><b>Scope: only CE's fully item-ready entries are ported</b> (per this task's ground rules - do
 * not stub missing items). Of CE's 53 real entries (23 individually-written + a 6-type x 5-grade-pair
 * bedrock-roasting loop), <b>38</b> are ported here: <b>8</b> of the 23 individual entries (CE lines
 * 96, 97, 100, 103, 104, 106, 120, 121) plus <b>all 30</b> bedrock-roasting loop entries (CE lines
 * 123-134) - the single largest "ready" finding for this class, since {@code BedrockOreType}/
 * {@code BedrockOreGrade}/{@link BedrockOreItems} are all verified fully registered (156-item grid).
 * The other 15 individual entries are <b>not</b> ported:
 * each is blocked on one or more of these missing port-side items (grepped absent, corroborated by
 * this port's own existing code comments - see the research report's "Item/registry dependency
 * check"):
 * <ul>
 *   <li><b>{@code coke}</b> (hand item; only a block form, {@code BlockCoke}, exists) - blocks the
 *   coal/lignite carbonization entries (CE lines 86-94) and the 4 {@code oil_tar} entries (111-118,
 *   also blocked by {@code oil_tar} itself, see below).</li>
 *   <li><b>{@code briquette}</b> (hand item, 0 hits) - blocks the coal/lignite/wood briquette entries
 *   (CE lines 88-89, 93-94, 108-109).</li>
 *   <li><b>{@code sulfur}</b> (hand item; only ore/world-gen block references exist) - blocks the
 *   cinnabar and glowstone-dust entries (CE lines 98, 99).</li>
 *   <li><b>{@code chunk_ore}</b> ({@code EnumChunkType.CRYOLITE}) - blocks the cryolite entry (CE
 *   line 101-102); this port's own {@code BlockResourceStone.java} already flags this exact gap.</li>
 *   <li><b>{@code powder_ash}</b> ({@code EnumAshType.WOOD}) - blocks the sapling entry (CE line
 *   107); {@code BilletPowderItems.java} and {@code EntityMissileStealth.java} both carry explicit
 *   {@code TODO(items-followup)} comments for this exact gap.</li>
 *   <li><b>{@code oil_tar}</b> ({@code EnumTarType}, all 4 grades) - blocks CE lines 111-118;
 *   {@code RefineryRecipes.java} (this port's own already-ported Refinery recipe file) already
 *   carries 3 live {@code TODO(items-followup)} comments for exactly this item.</li>
 * </ul>
 *
 * <p><b>Discrepancy from {@code docs/phase7/mrec_06_soldering_misc.md}</b>: that report tallies 37
 * ready entries and leaves CE line 103 ({@code NA.dust()} -&gt; null item, Sodium 100mB fluid) as an
 * open question ("could not confirm whether this resolves to the same {@code powder_sodium} item or
 * a separate one without reading CE's full {@code OreDictManager.java}"). Reading CE's
 * {@code OreDictManager.java:288} directly resolves it: {@code NA} is declared as
 * {@code new DictFrame("Sodium")} - the same generic-name-keyed frame CE's {@code SODIUM} constant
 * would be if one existed (it doesn't; {@code NA} <i>is</i> CE's only "elemental sodium" dict frame),
 * so {@code NA.dust()} resolves to the identical {@code dustSodium}-family item this port already
 * flattened as {@link BilletPowderItems#POWDER_SODIUM}. Ported here as a 38th ready entry (input
 * {@code POWDER_SODIUM}, no item output, {@code Fluids.SODIUM} 100mB) per this task's own ground
 * rules ("use your own judgment and note the discrepancy").</p>
 *
 * <p><b>{@code KEY_LOG}</b> (CE's ore-dict wildcard {@code "logWood"}) is ported via vanilla
 * {@link ItemTags#LOGS} directly ({@link OreDictStack} takes any {@code TagKey<Item>}, not just the
 * {@code c:} common-tag namespace {@link OreDictStack#ofCommonTag} wraps) - a strictly better-typed
 * 1.21 equivalent needing no new port-side item registration, matching CE's "any log" semantics
 * exactly. CE's output for this entry is {@code new ItemStack(Items.COAL, 1, 1)} (metadata 1 =
 * charcoal in 1.12's single-item coal/charcoal split); this port's flattened equivalent is vanilla
 * {@link Items#CHARCOAL}.</p>
 */
public final class CombinationRecipes {

    public static final Map<AStack, CombinationRecipe> RECIPES = new LinkedHashMap<>();

    /** {@code (type, grade)} pairs CE's bedrock-roasting loop transitions, in CE's own iteration order. */
    private static final BedrockOreGrade[][] ROAST_GRADE_PAIRS = {
            {BedrockOreGrade.BASE, BedrockOreGrade.BASE_ROASTED},
            {BedrockOreGrade.PRIMARY, BedrockOreGrade.PRIMARY_ROASTED},
            {BedrockOreGrade.SULFURIC_BYPRODUCT, BedrockOreGrade.SULFURIC_ROASTED},
            {BedrockOreGrade.SOLVENT_BYPRODUCT, BedrockOreGrade.SOLVENT_ROASTED},
            {BedrockOreGrade.RAD_BYPRODUCT, BedrockOreGrade.RAD_ROASTED},
    };

    private static boolean registered = false;

    private CombinationRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // -- individual entries (8 of CE's 23; the other 15 are blocked, see class javadoc) --

        addRecipe(new ComparableStack(BilletPowderItems.POWDER_CHLOROCALCITE.get()),
                new ItemStack(BilletPowderItems.POWDER_CALCIUM.get()), new FluidStack(Fluids.CHLORINE, 250));

        addRecipe(new ComparableStack(BilletPowderItems.POWDER_MOLYSITE.get()),
                new ItemStack(Items.IRON_INGOT), new FluidStack(Fluids.CHLORINE, 250));

        addRecipe(new ComparableStack(PlateCrystalWasteItems.GEM_SODALITE.get()),
                new ItemStack(BilletPowderItems.POWDER_SODIUM.get()), new FluidStack(Fluids.CHLORINE, 100));

        // CE's NA.dust() -> Sodium fluid, no item output; see class javadoc discrepancy note.
        addRecipe(new ComparableStack(BilletPowderItems.POWDER_SODIUM.get()),
                null, new FluidStack(Fluids.SODIUM, 100));

        addRecipe(new ComparableStack(BilletPowderItems.POWDER_LIMESTONE.get()),
                new ItemStack(BilletPowderItems.POWDER_CALCIUM.get()), new FluidStack(Fluids.CARBONDIOXIDE, 50));

        addRecipe(new OreDictStack(ItemTags.LOGS),
                new ItemStack(Items.CHARCOAL), new FluidStack(Fluids.WOODOIL, 250));

        addRecipe(new ComparableStack(Items.SUGAR_CANE),
                new ItemStack(Items.SUGAR, 2), new FluidStack(Fluids.ETHANOL, 50));

        addRecipe(new ComparableStack(Items.CLAY),
                new ItemStack(Items.BRICKS), null);

        // -- bedrock-roasting loop: 6 BedrockOreType x 5 grade pairs = 30 entries, all ready --

        for (BedrockOreType type : BedrockOreType.VALUES) {
            for (BedrockOreGrade[] pair : ROAST_GRADE_PAIRS) {
                addRecipe(new ComparableStack(BedrockOreItems.get(type, pair[0]).get()),
                        new ItemStack(BedrockOreItems.get(type, pair[1]).get()),
                        new FluidStack(Fluids.VITRIOL, 50));
            }
        }
    }

    private static void addRecipe(AStack input, ItemStack output, FluidStack outputFluid) {
        RECIPES.put(input, new CombinationRecipe(output, outputFluid));
    }

    /**
     * Ported from CE's own {@code CombinationRecipes.getOutput(ItemStack)}: exact match first is not
     * distinguished here the way CE's two-pass exact-then-ore-dict lookup was, since this port's
     * {@link AStack} hierarchy already unifies both key kinds behind one {@code matchesRecipe} call -
     * same linear-scan-first-match order {@code CentrifugeRecipes#getOutput} already established.
     */
    public static CombinationRecipe getOutput(ItemStack stack) {
        register();
        if (stack == null || stack.isEmpty()) return null;

        for (Map.Entry<AStack, CombinationRecipe> entry : RECIPES.entrySet()) {
            if (entry.getKey().matchesRecipe(stack, true)) {
                CombinationRecipe recipe = entry.getValue();
                return new CombinationRecipe(
                        recipe.output == null ? null : recipe.output.copy(),
                        recipe.outputFluid);
            }
        }
        return null;
    }

    /**
     * Full-collection accessor, defensively calling {@link #register()} first (idempotent) so any
     * future consumer (a {@code MachineFurnaceCombination} block entity, a JEI category) can call
     * this safely without needing a separate eager-bootstrap call wired into a shared aggregator
     * file - the same pattern {@code RefineryRecipes#getAllRefinery()}/{@link PUREXRecipes#getAll()}
     * already establish.
     */
    public static Map<AStack, CombinationRecipe> getAll() {
        register();
        return Collections.unmodifiableMap(RECIPES);
    }

    /**
     * Single item output (nullable) + single fluid output (nullable), matching CE's own
     * {@code Pair<ItemStack, FluidStack>} exactly - at least one of the two is always non-null for
     * every entry actually registered above.
     */
    public static final class CombinationRecipe {
        public final ItemStack output;
        public final FluidStack outputFluid;

        public CombinationRecipe(ItemStack output, FluidStack outputFluid) {
            this.output = output;
            this.outputFluid = outputFluid;
        }
    }
}
