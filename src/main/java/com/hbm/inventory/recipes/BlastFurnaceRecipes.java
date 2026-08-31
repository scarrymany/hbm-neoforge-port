package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.BlastFurnaceRecipes} (397 lines, read in full
 * upstream; see {@code docs/phase7/mrec_09_blastfurnace_misc.md}). Despite the class name, CE's
 * {@code @Deprecated} {@code BlastFurnaceRecipes} backs the <b>older</b> two-slot Di-Furnace pair
 * ({@code TileEntityDiFurnace}/{@code TileEntityDiFurnaceRTG}), not CE's newer, unrelated
 * {@code BlastFurnaceRecipesNT}/{@code TileEntityMachineBlastFurnace} (that class is a separate,
 * not-yet-researched task - see the research report's Open Questions #1). Neither the Di-Furnace
 * block nor its block entity exist in this port yet; this class is recipe data only, ready for
 * whichever future pass builds {@code TileEntityDiFurnace}'s NeoForge equivalent to consume via
 * {@link #getItemPower}/{@link #getOutput}, matching {@code ArcWelderRecipes}' own "recipe data
 * ready, block/BE not built yet" precedent.
 * <p>
 * <b>Lazy registration</b>: see {@link CrystallizerRecipes#registerDefaults()}'s javadoc for why
 * {@link #registerDefaults()} only runs on first real lookup, never eagerly from a registration
 * bootstrap - the same hazard applies here (every entry below resolves a real {@code DeferredItem}).
 * <p>
 * <b>CE's "DictFrame wildcard" inputs</b> (a bare material reference passed to CE's
 * {@code addRecipe}, expanding to any of that material's ingot/plate/gem/dust forms - CE's
 * {@code getRecipeStacks}, lines 184-202) are reproduced with a small local {@link AnyStack} union
 * type below, listing only the concrete forms independently confirmed to exist in this port for
 * each material (not a blind 4-shape tag loop - several materials here have no plate/gem form
 * registered at all, and a couple, like coal, are matched by their plain vanilla item rather than
 * any per-material shape family). See each {@code *Any()} helper's inline citation.
 * <p>
 * <b>Discrepancies from the research report</b>, found while cross-checking real CE source
 * ({@code upstream/hbm-ce/.../BlastFurnaceRecipes.java} and {@code OreDictManager.java}) against
 * this port's actual registered items - corrected here, not silently:
 * <ul>
 *     <li><b>{@code neutron_reflector} does not exist in this port under any name</b> (confirmed by
 *     a repo-wide grep - the only hit is {@code ArcWelderRecipes}' own "not registered" citation).
 *     The report's summary line claimed recipes "#1-8, #10-14" were ready, which would include
 *     recipes #7 and #8 (both output {@code neutron_reflector}) - both are actually blocked. The
 *     report's own itemized "Blocked" section only flagged #8 (via {@code ANY_COKE}) and missed #7
 *     entirely; #7 is dropped here too.</li>
 *     <li><b>{@code ANY_COKE} is not a uniformly missing family.</b> Reading CE's real
 *     {@code getRecipeStacks} confirms the bare-{@code ANY_COKE} "wildcard" form used in item
 *     recipes #2/#4/#8 only ever tries {@code .ingot()/.plate()/.gem()/.dust()} - never
 *     {@code .block()} - and CE's own {@code OreDictManager} only ever populates {@code ANY_COKE}'s
 *     {@code .gem()} (the loose {@code coke} item, absent from this port) and {@code .block()}
 *     (CE's {@code block_coke}, present here as {@code block_coke_coal}/{@code _lignite}/
 *     {@code _petroleum} - confirmed real registered items, {@code GenericBlocks.java:538-541}).
 *     So the wildcard recipes (#2/#4/#8) stay blocked (only {@code .gem()} would ever satisfy them,
 *     and that item doesn't exist), but the <b>explicit</b> {@code ANY_COKE.block()} fuel entry
 *     (CE fuel #12, 4000 power) is portable now - registered below as three fuel entries, one per
 *     coke-block variant, matching CE's own {@code fromAll(block_coke, EnumCokeType.VALUES)}
 *     multi-variant registration under one ore-dict key.</li>
 *     <li><b>{@code "gemCharcoal"}</b> (report: "not individually re-verified") resolves to vanilla
 *     {@link Items#CHARCOAL} - the same substitution this port's own {@code CombinationRecipes}
 *     already established for CE's charcoal concept. Ported as ready.</li>
 * </ul>
 * <p>
 * <b>Scope trim vs. CE</b> (documented, not silent, per this task's own ground rules): of CE's 21
 * fuel entries, 9 are ready here (11 {@link #addFuel} calls - {@code ANY_COKE.block()} expands to
 * 3). Not ported: {@code LIGNITE.gem()}/{@code .block()} (CE's {@code lignite} item and any lignite
 * block form don't exist in this port - {@code OreBlocks.java}'s own javadoc lists {@code lignite}
 * among items "not yet ported by any Phase 1 items area"), {@code briquette}, {@code "blockCharcoal"}
 * (no charcoal block item), {@code "fuelCoke"}/{@code ANY_COKE.gem()} (loose {@code coke} item
 * missing), the vanilla-coal-damage-1 entry (not applicable - 1.21 flattened away 1.12 metadata
 * variants, and it would just duplicate the plain-coal entry above it), {@code INFERNAL} (no
 * "infernal coal" item/block - CE's own {@code OreDictManager} never actually populates
 * {@code INFERNAL}'s {@code .gem()}/{@code .block()} with a real item either, a pre-existing CE
 * oversight, not a porting gap this port introduced), and {@code solid_fuel}/{@code _presto}/
 * {@code _presto_triplet} (missing item family). Of CE's 16 item recipes (15 unconditional + 1
 * config-gated), 9 are ready: #1, #3, #5, #6, #10, #11, #12, #13, #14. Not ported: #2/#4/#8
 * ({@code ANY_COKE} wildcard), #7 ({@code neutron_reflector}, see discrepancy note above), #9/#16
 * (canister family - {@code ItemCanister.java} registers zero concrete canister items yet), #15
 * ({@code meteorite_sword_hardened}/{@code _alloyed}, also already {@code hiddenRecipes}-flagged in
 * CE so low-priority regardless).
 * <p>
 * <b>One judgment call</b> not directly citable to a specific CE ore-dict registration: CE's fuel
 * entry {@code COAL.block()} (2000 power) has no {@code .block(...)} setter call anywhere in real
 * CE's {@code OreDictManager.java} (only {@code .gem()}/{@code .dustSmall()}/{@code .dust()} are
 * set for {@code COAL}) - i.e. this specific fuel entry appears to already be dead/unreachable in
 * upstream CE itself. Ported here anyway against vanilla {@link Blocks#COAL_BLOCK} (obviously the
 * intended real-world equivalent, not an invented substitute) rather than skipped, since the input
 * item trivially exists and the mapping carries essentially no ambiguity.
 */
public final class BlastFurnaceRecipes {

    private static final Map<AStack, Integer> FUELS = new LinkedHashMap<>();
    private static final List<BlastFurnaceRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private BlastFurnaceRecipes() {
    }

    /** See class javadoc "Lazy registration". Idempotent, safe to call from any lookup entry point. */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        registerFuels();
        registerRecipes();
    }

    private static void registerFuels() {
        // CE: COAL.gem() -> vanilla coal
        addFuel(new ComparableStack(Items.COAL), 200);
        // CE: COAL.dust() -> powder_coal
        addFuel(new ComparableStack(BilletPowderItems.POWDER_COAL.get()), 220);
        // CE: COAL.block() -> see class javadoc "One judgment call"
        addFuel(new ComparableStack(Blocks.COAL_BLOCK), 2000);
        // CE: LIGNITE.dust() -> powder_lignite (LIGNITE.gem()/.block() not portable, see class javadoc)
        addFuel(new ComparableStack(BilletPowderItems.POWDER_LIGNITE.get()), 150);
        // CE: "gemCharcoal" -> vanilla charcoal, see class javadoc
        addFuel(new ComparableStack(Items.CHARCOAL), 150);
        // CE: ANY_COKE.block() -> this port's 3 coke-block variants, see class javadoc
        addFuel(new ComparableStack(hbmItem("block_coke_coal")), 4000);
        addFuel(new ComparableStack(hbmItem("block_coke_lignite")), 4000);
        addFuel(new ComparableStack(hbmItem("block_coke_petroleum")), 4000);
        addFuel(new ComparableStack(Items.LAVA_BUCKET), 12800);
        addFuel(new ComparableStack(Items.BLAZE_ROD), 1000);
        addFuel(new ComparableStack(Items.BLAZE_POWDER), 300);

        // Not ported (see class javadoc "Scope trim"): LIGNITE.gem()/.block(), briquette,
        // "blockCharcoal", "fuelCoke"/ANY_COKE.gem(), vanilla-coal-damage-1 (N/A in 1.21),
        // INFERNAL.gem()/.block(), solid_fuel/_presto/_presto_triplet.
    }

    private static void registerRecipes() {
        // #1: IRON(any) + COAL(any) -> ingot_steel x1
        addRecipe(ironAny(), coalAny(), new ItemStack(IngotNuggetItems.INGOT_STEEL.get(), 1));
        // #3: IRON.ore() + COAL(any) -> ingot_steel x2
        addRecipe(new ComparableStack(Blocks.IRON_ORE), coalAny(), new ItemStack(IngotNuggetItems.INGOT_STEEL.get(), 2));
        // #5: IRON.ore() + powder_flux -> ingot_steel x3
        addRecipe(new ComparableStack(Blocks.IRON_ORE), new ComparableStack(BilletPowderItems.POWDER_FLUX.get()),
                new ItemStack(IngotNuggetItems.INGOT_STEEL.get(), 3));
        // #6: CU(any) + REDSTONE(any) -> ingot_red_copper x2
        addRecipe(copperAny(), redstoneAny(), new ItemStack(IngotNuggetItems.INGOT_RED_COPPER.get(), 2));
        // #10: W(any) + nugget_schrabidium -> ingot_magnetized_tungsten x1
        addRecipe(tungstenAny(), new ComparableStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get()),
                new ItemStack(IngotNuggetItems.INGOT_MAGNETIZED_TUNGSTEN.get(), 1));
        // #11: STEEL(any) + nugget_technetium -> ingot_tcalloy x1
        addRecipe(steelAny(), new ComparableStack(IngotNuggetItems.NUGGET_TECHNETIUM.get()),
                new ItemStack(IngotNuggetItems.INGOT_TCALLOY.get(), 1));
        // #12: plate_gold + plate_mixed -> plate_paa x2
        addRecipe(new ComparableStack(PlateCrystalWasteItems.PLATE_GOLD.get()), new ComparableStack(PlateCrystalWasteItems.PLATE_MIXED.get()),
                new ItemStack(PlateCrystalWasteItems.PLATE_PAA.get(), 2));
        // #13: BIGMT/Saturnite(any) + ingot_meteorite -> ingot_starmetal x2
        addRecipe(saturniteAny(), new ComparableStack(IngotNuggetItems.INGOT_METEORITE.get()),
                new ItemStack(IngotNuggetItems.INGOT_STARMETAL.get(), 2));
        // #14: CO/Cobalt(any) + powder_meteorite -> ingot_meteorite x1
        addRecipe(cobaltAny(), new ComparableStack(BilletPowderItems.POWDER_METEORITE.get()),
                new ItemStack(IngotNuggetItems.INGOT_METEORITE.get(), 1));

        // Not ported (see class javadoc "Scope trim"): #2/#4/#8 (ANY_COKE wildcard), #7
        // (neutron_reflector), #9/#16 (canister family), #15 (meteorite sword chain).
    }

    // ==================== CE DictFrame-wildcard reproductions ====================
    // See class javadoc "CE's DictFrame wildcard inputs" - each helper lists only the concrete
    // ingot/plate/gem/dust forms independently confirmed to exist in this port for that material.

    private static AStack ironAny() {
        return new AnyStack(
                new ComparableStack(Items.IRON_INGOT),
                new ComparableStack(BilletPowderItems.POWDER_IRON.get()),
                new ComparableStack(PlateCrystalWasteItems.PLATE_IRON.get()));
    }

    private static AStack coalAny() {
        return new AnyStack(
                new ComparableStack(Items.COAL),
                new ComparableStack(BilletPowderItems.POWDER_COAL.get()));
    }

    private static AStack copperAny() {
        return new AnyStack(
                new ComparableStack(Items.COPPER_INGOT),
                new ComparableStack(BilletPowderItems.POWDER_COPPER.get()),
                new ComparableStack(PlateCrystalWasteItems.PLATE_COPPER.get()));
    }

    private static AStack redstoneAny() {
        return new AnyStack(new ComparableStack(Items.REDSTONE));
    }

    private static AStack tungstenAny() {
        return new AnyStack(
                new ComparableStack(IngotNuggetItems.INGOT_TUNGSTEN.get()),
                new ComparableStack(BilletPowderItems.POWDER_TUNGSTEN.get()));
    }

    private static AStack steelAny() {
        return new AnyStack(
                new ComparableStack(IngotNuggetItems.INGOT_STEEL.get()),
                new ComparableStack(BilletPowderItems.POWDER_STEEL.get()),
                new ComparableStack(PlateCrystalWasteItems.PLATE_STEEL.get()));
    }

    private static AStack saturniteAny() {
        return new AnyStack(
                new ComparableStack(IngotNuggetItems.INGOT_SATURNITE.get()),
                new ComparableStack(PlateCrystalWasteItems.PLATE_SATURNITE.get()));
    }

    private static AStack cobaltAny() {
        return new AnyStack(
                new ComparableStack(IngotNuggetItems.INGOT_COBALT.get()),
                new ComparableStack(BilletPowderItems.POWDER_COBALT.get()));
    }

    /**
     * Resolves one of this port's own items by registry name - matches
     * {@code MixerRecipes#hbmItem(String)}'s already-established lazy-lookup pattern (see that
     * method's own javadoc for the full safety reasoning). Safe here only because this is only ever
     * reachable through {@link #registerFuels()}, itself only ever invoked lazily.
     */
    private static Item hbmItem(String path) {
        return BuiltInRegistries.ITEM.getValue(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    private static void addFuel(AStack fuel, int power) {
        FUELS.put(fuel, power);
    }

    private static void addRecipe(AStack input1, AStack input2, ItemStack output) {
        RECIPES.add(new BlastFurnaceRecipe(input1, input2, output));
    }

    /** Matches CE's {@code getItemPower(ItemStack)}: exact/tag/union match against {@link #FUELS}, 0 if nothing matches. */
    public static int getItemPower(ItemStack stack) {
        registerDefaults();
        if (stack == null || stack.isEmpty()) return 0;
        for (Map.Entry<AStack, Integer> entry : FUELS.entrySet()) {
            if (entry.getKey().matchesRecipe(stack, true)) return entry.getValue();
        }
        return 0;
    }

    /**
     * Matches CE's {@code getOutput(ItemStack, ItemStack)}: the two Di-Furnace input slots are
     * checked against both ingredient orderings (CE's real {@code TileEntityDiFurnace} slot-order
     * requirement was not read - out of this task's recipe-data-only scope, see the research
     * report's "Recommended implementation shape" #3 - so both orderings are accepted here as the
     * safer, strictly-more-permissive default; a future machine pass can tighten this if CE turns
     * out to require a fixed slot order). Returns {@link ItemStack#EMPTY} if nothing matches.
     */
    public static ItemStack getOutput(ItemStack in1, ItemStack in2) {
        registerDefaults();
        for (BlastFurnaceRecipe recipe : RECIPES) {
            if (recipe.input1.matchesRecipe(in1, true) && recipe.input2.matchesRecipe(in2, true)) {
                return recipe.output.copy();
            }
            if (recipe.input1.matchesRecipe(in2, true) && recipe.input2.matchesRecipe(in1, true)) {
                return recipe.output.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Full-collection accessor, matching {@code CrystallizerRecipes#getAllRecipes()}'s established
     * JEI-enumeration precedent ({@code docs/phase5/jei_integration.md}'s "Safe to build now" #4).
     */
    public static List<BlastFurnaceRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    public static Map<AStack, Integer> getAllFuels() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(FUELS);
    }

    public static final class BlastFurnaceRecipe {
        public final AStack input1;
        public final AStack input2;
        public final ItemStack output;

        public BlastFurnaceRecipe(AStack input1, AStack input2, ItemStack output) {
            this.input1 = input1;
            this.input2 = input2;
            this.output = output;
        }
    }

    /**
     * Reproduces CE's DictFrame wildcard match: matches if ANY of the given concrete candidate
     * {@link AStack}s matches. Not a general-purpose addition to {@link com.hbm.inventory.RecipesCommon}
     * - kept local to this file (this task's own file, not a shared one) since no other recipe class
     * in this port currently needs a union match; a future pass touching {@code RecipesCommon.java}
     * itself is free to promote this if the same need recurs elsewhere.
     */
    private static final class AnyStack extends AStack {
        private final AStack[] options;

        AnyStack(AStack... options) {
            this.options = options;
            this.stacksize = 1;
        }

        @Override
        public boolean matchesRecipe(ItemStack stack, boolean ignoreSize) {
            for (AStack option : options) {
                if (option.matchesRecipe(stack, ignoreSize)) return true;
            }
            return false;
        }

        @Override
        public AStack copy() {
            AnyStack copy = new AnyStack(options);
            copy.stacksize = stacksize;
            return copy;
        }

        @Override
        public AStack copy(int stacksize) {
            AnyStack copy = new AnyStack(options);
            copy.stacksize = stacksize;
            return copy;
        }

        @Override
        public ItemStack getStack() {
            return options.length > 0 ? options[0].getStack() : ItemStack.EMPTY;
        }

        @Override
        public List<ItemStack> getStackList() {
            List<ItemStack> list = new ArrayList<>();
            for (AStack option : options) list.addAll(option.getStackList());
            return list;
        }

        @Override
        public List<ItemStack> extractForJEI() {
            return getStackList();
        }

        @Override
        public int compareTo(AStack other) {
            return 0;
        }
    }
}
