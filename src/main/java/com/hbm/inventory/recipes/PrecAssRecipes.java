package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for the Precision Assembler, ported from CE's {@code com.hbm.inventory.recipes.
 * PrecAssRecipes} ({@code docs/phase7/mrec_04_arcwelder_misc.md}, 209 lines read in full upstream).
 * CE's real shape is {@code GenericRecipes<GenericRecipe>} (up to 9 item inputs, 1 fluid input, up
 * to 9 chance-weighted item outputs, 1 fluid output, duration + power) - this port's own
 * {@code com.hbm.inventory.recipes.loader.GenericRecipe} is explicitly documented as a minimal
 * pool-metadata stand-in that does NOT carry that shape (see that class's header), so - per the
 * research report's recommendation and following {@code com.hbm.inventory.recipes.chem.
 * ChemPlantRecipes}'s own precedent for exactly this situation - this class defines its own small
 * {@link PrecAssRecipe} data shape rather than extending the stand-in.
 * <p>
 * <b>Scope trim (documented, not silent): only 1 of CE's 22 authored recipes is ported here.</b>
 * <ul>
 *     <li><b>All 20 recipes gated behind {@code GeneralConfig.enable528()}</b> (the circuit chip/
 *     controller/upgrade-tier chain, plus their auto-generated {@code .recycle} companions) are
 *     blocked - every one references CE's {@code circuit[EnumCircuitType]} item family
 *     ({@code SILICON}/{@code CHIP}/{@code CHIP_BISMOID}/.../{@code CONTROLLER_QUANTUM}), which does
 *     not exist anywhere in this port under any name (confirmed: zero hits for
 *     {@code "circuit"}/{@code EnumCircuitType}/{@code ItemCircuit}). Every recycle companion is
 *     additionally blocked on {@code ModItems.BROKEN_ITEM} - {@link com.hbm.items.BrokenItem} itself
 *     is a real, 1.21-ready class, but the backing registered item its {@code make()} needs does not
 *     exist yet (confirmed: zero hits for {@code "BROKEN_ITEM"} in {@code ModItems.java}).</li>
 *     <li><b>{@code precass.beigeprints}</b> (CE's other ungated recipe) is blocked on
 *     {@code CINNABAR.gem()} - this port has not generated a {@code cinnabar_gem} item under any
 *     name (a whole {@code MaterialShapes.GEM} shape-family gap, not specific to cinnabar).</li>
 *     <li><b>{@code precass.blueprints}</b> (ported below) has no such blocker: every ingredient
 *     (vanilla paper/blue dye/pufferfish) and its output ({@code blueprint_folder_base}, CE metadata
 *     0 - confirmed against CE's own {@code ItemBlueprintFolder#onItemRightClick}, whose
 *     {@code meta == 0} branch maps to {@code POOL_PREFIX_ALT}, exactly this port's
 *     {@code ItemBlueprintFolder.Kind.BASE}) are already real, registered items.</li>
 * </ul>
 * <p>
 * <b>Not yet built: the Precision Assembler block/block entity/GUI itself</b> (confirmed absent by
 * the research report). This class is recipe data only. Once the circuit-item family and
 * {@code ModItems.BROKEN_ITEM} land, whoever extends this class should also replicate CE's
 * {@code registerPair()} recycle-generation as a helper method (auto-derive a {@code <name>.recycle}
 * companion recipe from a main recipe's own ingredient list, at CE's documented reclaim%) rather
 * than hand-authoring 20 more entries - see the research report's "Recommended implementation shape"
 * section for the exact mechanic.
 */
public final class PrecAssRecipes {

    public static final List<PrecAssRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private PrecAssRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE PrecAssRecipes.java: "int min = 1_200;" then .setup(5 * min, 20_000L) - ungated
        // this.register() call (not behind GeneralConfig.enable528()), no .recycle companion.
        // KEY_BLUE -> Items.BLUE_DYE, matching this port's own established substitution
        // (com.hbm.datagen.ModRecipeProvider's "bobmazon" recipe uses the identical mapping).
        RECIPES.add(new PrecAssRecipe("precass.blueprints", 6_000, 20_000L,
                new AStack[]{
                        new ComparableStack(Items.PAPER, 16),
                        new ComparableStack(Items.BLUE_DYE, 16),
                        new ComparableStack(Items.PUFFERFISH, 4)
                },
                null,
                new ChanceOutput[]{
                        new ChanceOutput(new ItemStack(blueprintFolderBase(), 1), 10),
                        new ChanceOutput(new ItemStack(Items.PAPER, 16), 90)
                }));
    }

    private static Item blueprintFolderBase() {
        return item("blueprint_folder_base");
    }

    /**
     * Resolve-by-id lookup against the already-populated {@link BuiltInRegistries#ITEM}, matching
     * the pattern already proven safe at runtime by {@code CentrifugeRecipes}/{@code CrucibleRecipes}
     * ({@link #register()} only ever runs from {@code CommonEvents.commonSetup}'s
     * {@code enqueueWork}, well after every item {@code RegisterEvent} has fired).
     */
    private static Item item(String path) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path))
                .orElseThrow(() -> new IllegalStateException(
                        "PrecAssRecipes: item hbm:" + path + " is not registered - check com.hbm.items.machine.MachineItems"));
    }

    /** Up to 9 item inputs (weighted success/failure), matching CE's {@code ChanceOutput} shape. */
    public static final class ChanceOutput {
        public final ItemStack stack;
        public final int weight;

        public ChanceOutput(ItemStack stack, int weight) {
            this.stack = stack;
            this.weight = weight;
        }
    }

    /**
     * Up to 9 {@link AStack} item inputs, 1 optional {@link FluidStack} input, chance-weighted item
     * outputs (via {@link ChanceOutput}, CE's {@code ChanceOutputMulti} equivalent), duration
     * (ticks) + power (HE/tick) - preserving CE's real {@code GenericRecipe} shape for this machine.
     */
    public static final class PrecAssRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final AStack[] inputItems;
        public final FluidStack inputFluid;
        public final ChanceOutput[] outputs;

        public PrecAssRecipe(String name, int duration, long power, AStack[] inputItems, FluidStack inputFluid, ChanceOutput[] outputs) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems;
            this.inputFluid = inputFluid;
            this.outputs = outputs;
        }
    }
}
