package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for the Arc Welder, ported from CE's {@code com.hbm.inventory.recipes.
 * ArcWelderRecipes} ({@code docs/phase7/mrec_04_arcwelder_misc.md}, 528 lines read in full upstream).
 * CE's own shape is preserved as a plain static list (order-independent {@link AStack} ingredients,
 * an optional single fluid input, one exact-item output, duration + power) - the same
 * "port now, JSON-override later" convention {@link RefineryRecipes}/{@code ChemPlantRecipes}
 * already established, rather than a vanilla {@code Recipe<CraftingInput>}, since this machine has
 * no crafting-table analogue.
 * <p>
 * <b>Scope trim (documented, not silent):</b> CE registers 47 recipes; this class ports the
 * <b>14 that are fully item-ready today</b> per the research report's per-family dependency check -
 * the 3 {@code wire_dense} recipes and the 11 {@code plate_welded} recipes, both families built
 * entirely from {@link MaterialShapes#WIRE}/{@link MaterialShapes#DENSEWIRE}/
 * {@link MaterialShapes#CASTPLATE}/{@link MaterialShapes#WELDEDPLATE} autogen items (all confirmed
 * registered by {@code com.hbm.items.MaterialItemGenerator} for every material used below - see
 * each entry's own inline citation). The remaining 33 entries (parts, missile parts, missiles,
 * satellites) are <b>not</b> ported here - every one needs at least one item that does not exist in
 * this port under any name ({@code motor}, {@code part_generic}, {@code neutron_reflector},
 * {@code thruster_*}, {@code fuel_tank_*}, {@code steel_scaffold}, {@code missile_assembly}, 12
 * {@code warhead_*} variants, {@code sat_base}, 5 {@code sat_head_*} variants), a whole undoubled
 * {@link MaterialShapes#PLATE} shape family this port has not generated for any material (distinct
 * from the {@code CASTPLATE}/{@code WELDEDPLATE} families this class does use), two material
 * identities that don't exist in {@link Mats} at all ({@code WC}/tungsten carbide, {@code FIBER}),
 * or CE's cross-material {@code ANY_X} ore-dict wildcard mechanism (no port-side equivalent - see
 * the research report's Open Questions #1). None of these gaps are guessed at or stubbed here.
 * <p>
 * Ingredients use {@link OreDictStack} against each material's own {@link MaterialShapes#commonTag}
 * (populated by {@code com.hbm.items.datagen.ModItemTagProvider} for every real autogen item),
 * matching CE's own {@code new OreDictStack(X.plateCast(), n)} call shape exactly - a tag match, not
 * a hardcoded exact-item match. Output item ids are derived programmatically via
 * {@link MaterialShapes#buildRegistryName(NTMMaterial)} rather than hand-typed, since several
 * materials' canonical registry name disagrees with their CE field name (e.g. {@link Mats#MAT_CMB}'s
 * real id token is {@code cmbsteel}, not {@code cmb} - {@link Mats#MAT_ALUMINIUM}'s is
 * {@code aluminum}, American spelling) - the same discipline {@code ModRecipeProvider}'s
 * {@code BLOCK_INGOT_SETS} already established for the identical class of naming trap.
 * <p>
 * <b>Not yet built: the Arc Welder block/block entity/GUI itself</b> (confirmed absent by the
 * research report - block, BE, container and screen are all zero for this machine). This class is
 * recipe data only, ready for whichever future pass builds
 * {@code com.hbm.blockentity.machine.MachineArcWelderBlockEntity} to consume via {@link #getRecipe}.
 */
public final class ArcWelderRecipes {

    public static final List<ArcWelderRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private ArcWelderRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // ---- Dense wires (CE ArcWelderRecipes.java lines ~68-84): 100 ticks, 10,000 HE, 8x wireFine ----
        RECIPES.add(new ArcWelderRecipe("arcwelder.wire_dense_copper",
                denseWireOutput(Mats.MAT_COPPER), 100, 10_000L,
                shapeInput(Mats.MAT_COPPER, MaterialShapes.WIRE, 8)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.wire_dense_mingrade",
                denseWireOutput(Mats.MAT_MINGRADE), 100, 10_000L,
                shapeInput(Mats.MAT_MINGRADE, MaterialShapes.WIRE, 8)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.wire_dense_gold",
                denseWireOutput(Mats.MAT_GOLD), 100, 10_000L,
                shapeInput(Mats.MAT_GOLD, MaterialShapes.WIRE, 8)));

        // ---- Welded plates (CE lines ~86-163), progression-gated per CE's own inline comments ----
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_iron", // earlygame
                weldedPlateOutput(Mats.MAT_IRON), 100, 100L,
                shapeInput(Mats.MAT_IRON, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_steel", // high-demand mid-game
                weldedPlateOutput(Mats.MAT_STEEL), 100, 500L,
                shapeInput(Mats.MAT_STEEL, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_copper", // combination oven
                weldedPlateOutput(Mats.MAT_COPPER), 200, 1_000L,
                shapeInput(Mats.MAT_COPPER, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_titanium", // mid-game, combustion engine on LPG
                weldedPlateOutput(Mats.MAT_TITANIUM), 600, 50_000L,
                shapeInput(Mats.MAT_TITANIUM, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_zirconium", // mid-game PWR
                weldedPlateOutput(Mats.MAT_ZIRCONIUM), 600, 10_000L,
                shapeInput(Mats.MAT_ZIRCONIUM, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_aluminium",
                weldedPlateOutput(Mats.MAT_ALUMINIUM), 300, 10_000L,
                shapeInput(Mats.MAT_ALUMINIUM, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_tcalloy", // late-game fusion
                weldedPlateOutput(Mats.MAT_TCALLOY), 1_200, 1_000_000L,
                new FluidStack(Fluids.OXYGEN, 1_000),
                shapeInput(Mats.MAT_TCALLOY, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_cdalloy",
                weldedPlateOutput(Mats.MAT_CDALLOY), 1_200, 1_000_000L,
                new FluidStack(Fluids.OXYGEN, 1_000),
                shapeInput(Mats.MAT_CDALLOY, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_tungsten",
                weldedPlateOutput(Mats.MAT_TUNGSTEN), 1_200, 250_000L,
                new FluidStack(Fluids.OXYGEN, 1_000),
                shapeInput(Mats.MAT_TUNGSTEN, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_cmb",
                weldedPlateOutput(Mats.MAT_CMB), 1_200, 10_000_000L,
                new FluidStack(Fluids.REFORMGAS, 1_000),
                shapeInput(Mats.MAT_CMB, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new ArcWelderRecipe("arcwelder.plate_welded_osmiridium", // pre-DFC
                weldedPlateOutput(Mats.MAT_OSMIRIDIUM), 6_000, 20_000_000L,
                new FluidStack(Fluids.REFORMGAS, 16_000),
                shapeInput(Mats.MAT_OSMIRIDIUM, MaterialShapes.CASTPLATE, 2)));

        // Deliberately not ported (see class javadoc): parts (motor/part_generic/neutron_reflector),
        // missile parts, all 18 missiles, all 5 satellites - every one needs at least one item this
        // port has not registered under any name (see the research report's per-family dependency
        // check for the exact list per entry).
    }

    /** {@code X.wireFine()}/{@code X.plateCast()} - CE's own {@link OreDictStack} ingredient shape. */
    private static AStack shapeInput(NTMMaterial mat, MaterialShapes shape, int count) {
        return new OreDictStack(shape.commonTag(mat), count);
    }

    private static ItemStack denseWireOutput(NTMMaterial mat) {
        return new ItemStack(shapeItem(mat, MaterialShapes.DENSEWIRE));
    }

    private static ItemStack weldedPlateOutput(NTMMaterial mat) {
        return new ItemStack(shapeItem(mat, MaterialShapes.WELDEDPLATE));
    }

    /**
     * Resolve-by-id lookup against the already-populated {@link BuiltInRegistries#ITEM}, matching
     * the pattern already proven safe at runtime by {@code CentrifugeRecipes}/{@code CrucibleRecipes}
     * (this method only ever runs from {@link #register()}, itself only ever called from
     * {@code CommonEvents.commonSetup}'s {@code enqueueWork} - well after every item
     * {@code RegisterEvent} has fired). Throws loudly rather than silently building an unmatchable
     * recipe if a regression ever removes the backing autogen item.
     */
    private static Item shapeItem(NTMMaterial mat, MaterialShapes shape) {
        String id = shape.buildRegistryName(mat);
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id))
                .orElseThrow(() -> new IllegalStateException(
                        "ArcWelderRecipes: item hbm:" + id + " is not registered - check com.hbm.items.MaterialItemGenerator"));
    }

    /**
     * Ported from CE's own {@code ArcWelderRecipes.getRecipe(ItemStack...)} (lines 422-454, read in
     * full): order-independent, greedy per-input match against a mutable copy of the recipe's
     * ingredient list - each input slot is consumed at most once, and a match requires every
     * ingredient to be spent (empty leftover list). Matches items only, exactly like CE (fluid input
     * is checked separately by whichever block entity eventually calls this).
     */
    public static ArcWelderRecipe getRecipe(ItemStack... inputs) {
        register();

        outer:
        for (ArcWelderRecipe recipe : RECIPES) {
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
     * Variable-length {@link AStack} ingredients (order-independent), an optional single fluid
     * input, one deterministic {@link ItemStack} output, duration (ticks) + power (HE/tick) -
     * preserving CE's exact {@code ArcWelderRecipe} inner-class shape.
     */
    public static final class ArcWelderRecipe {
        public final String name;
        public final ItemStack output;
        public final int duration;
        public final long power;
        public final FluidStack fluid;
        public final AStack[] ingredients;

        public ArcWelderRecipe(String name, ItemStack output, int duration, long power, FluidStack fluid, AStack... ingredients) {
            this.name = name;
            this.output = output;
            this.duration = duration;
            this.power = power;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }

        public ArcWelderRecipe(String name, ItemStack output, int duration, long power, AStack... ingredients) {
            this(name, output, duration, power, null, ingredients);
        }
    }
}
