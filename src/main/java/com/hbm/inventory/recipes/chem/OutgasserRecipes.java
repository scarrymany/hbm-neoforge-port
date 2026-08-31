package com.hbm.inventory.recipes.chem;

import com.hbm.blocks.generic.PlantBlocks;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for CE's {@code com.hbm.inventory.recipes.OutgasserRecipes} - ported per
 * {@code docs/phase7/mrec_08_chemplant_misc.md}'s catalog and item/registry dependency check.
 * <p>
 * <b>Naming trap, resolved by the research report</b>: despite the file name, this is <i>not</i> the
 * RBMK Outgasser machine ({@code com.hbm.blocks.machine.rbmk.RBMKOutgasser}, a separate CE system,
 * out of this task's scope and also unported). CE's {@code OutgasserRecipes.getOutput(...)} is only
 * called from {@code TileEntityFusionBreeder} - this is the recipe table for the <b>Fusion Breeder</b>
 * machine's slot-1 item-irradiation input (registered as {@code ModBlocks.fusion_breeder} /
 * {@code MachineFusionBreeder} in CE). This port has zero trace of a Fusion Breeder block/block
 * entity/menu/screen - building that whole machine (a multi-fluid transceiver with fusion-power-
 * network integration, per the research report "likely larger than the recipe class itself") is out
 * of this task's scope (a machine-recipe-data task, not a from-scratch-machine task). This class is
 * recipe data only, following the report's recommended shape (small data class - single {@link AStack}
 * input, nullable item output, nullable fluid output, no duration/power field - CE's Fusion Breeder
 * ticks at a fixed machine rate, not a per-recipe one, confirmed by CE's own {@code OutgasserRecipe}
 * inner class carrying neither field), ready for whichever future pass builds the Fusion Breeder to
 * consume via {@link #getOutput}.
 * <p>
 * <b>18 of CE's 22 raw {@code recipes.put(} call sites are the real entry count</b> (the naive grep
 * tally double-counts, see the research report's Open Questions #1 for the full accounting - trusted
 * here without re-deriving). <b>12 of those 18 are ported</b>; 6 are not:
 * <ul>
 *     <li><b>{@code LI.ingot()} -&gt; 1,000mB tritium</b> - CE's lithium <i>ingot</i> shape has no
 *     port-side equivalent ({@link com.hbm.inventory.material.Mats#MAT_LITHIUM} only autogens
 *     {@code FRAGMENT}/{@code DUST}/{@code BLOCK}, no {@code INGOT}, and no hand-registered
 *     {@code ingot_lithium} item exists either - confirmed absent). The other 3 lithium shapes
 *     (block/dust/dust-tiny) are fine and are ported below.</li>
 *     <li><b>The 3 coal -&gt; {@code oil_tar[COAL]} entries and the 2 {@code oil_tar}-keyed input
 *     entries (5 total)</b> - {@code ModItems.oil_tar} (with {@code ItemEnums.EnumTarType}) is
 *     confirmed absent from this port, corroborated by {@code RefineryRecipes}/
 *     {@code SolidificationRecipes}/{@code CombinationRecipes}/{@code PyroOvenRecipes}, which each
 *     independently document the exact same gap (not a new finding specific to this file).</li>
 * </ul>
 */
public final class OutgasserRecipes {

    public static final List<OutgasserRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private OutgasserRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        /* lithium to tritium (LI.ingot() NOT PORTED - no ingot_lithium/lithium_ingot item exists) */
        RECIPES.add(new OutgasserRecipe(new ComparableStack(resolveItem("lithium_block"), 1),
                null, new FluidStack(Fluids.TRITIUM, 10_000)));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(BilletPowderItems.POWDER_LITHIUM.get(), 1),
                null, new FluidStack(Fluids.TRITIUM, 1_000)));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(BilletPowderItems.POWDER_LITHIUM_TINY.get(), 1),
                null, new FluidStack(Fluids.TRITIUM, 100)));

        /* gold to gold-198 */
        RECIPES.add(new OutgasserRecipe(new ComparableStack(Items.GOLD_INGOT, 1),
                new ItemStack(IngotNuggetItems.INGOT_AU198.get(), 1), null));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(Items.GOLD_NUGGET, 1),
                new ItemStack(IngotNuggetItems.NUGGET_AU198.get(), 1), null));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(BilletPowderItems.POWDER_GOLD.get(), 1),
                new ItemStack(BilletPowderItems.POWDER_AU198.get(), 1), null));

        /* thorium to thorium fuel */
        RECIPES.add(new OutgasserRecipe(new ComparableStack(IngotNuggetItems.INGOT_TH232.get(), 1),
                new ItemStack(IngotNuggetItems.INGOT_THORIUM_FUEL.get(), 1), null));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(IngotNuggetItems.NUGGET_TH232.get(), 1),
                new ItemStack(IngotNuggetItems.NUGGET_THORIUM_FUEL.get(), 1), null));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(BilletPowderItems.BILLET_TH232.get(), 1),
                new ItemStack(BilletPowderItems.BILLET_THORIUM_FUEL.get(), 1), null));

        /* mushrooms to glowing mushrooms */
        RECIPES.add(new OutgasserRecipe(new ComparableStack(Items.BROWN_MUSHROOM, 1),
                new ItemStack(PlantBlocks.MUSH.get().asItem(), 1), null));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(Items.RED_MUSHROOM, 1),
                new ItemStack(PlantBlocks.MUSH.get().asItem(), 1), null));
        RECIPES.add(new OutgasserRecipe(new ComparableStack(Items.MUSHROOM_STEW, 1),
                new ItemStack(resolveItem("glowing_stew"), 1), null));

        // NOT PORTED (5 entries, ModItems.oil_tar confirmed absent - see class javadoc): coal (gem/
        // dust/block) -> oil_tar[COAL] x1/x1/x9 + SYNGAS 50/50/500, and the 2 oil_tar-keyed inputs
        // (oil_tar[COAL] -> COALOIL 100, oil_tar[WAX] -> RADIOSOLVENT 100).
    }

    /**
     * Ported from CE's {@code OutgasserRecipes.getRecipe}: exact match first, then a linear scan for
     * the first applicable {@link AStack} (tag membership) - same lookup order CE's own
     * ore-dict-then-exact fallback used, adapted to this port's tag-based {@link OreDictStack}.
     */
    public static OutgasserRecipe getOutput(ItemStack input) {
        register();
        if (input == null || input.isEmpty()) return null;

        for (OutgasserRecipe recipe : RECIPES) {
            if (recipe.input.matchesRecipe(input, true)) return recipe;
        }
        return null;
    }

    /** Same lazy-lookup-by-path idiom as {@code PUREXRecipes#resolveItem}/{@code FluidContainerRegistry#resolveItem}. */
    private static Item resolveItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * One {@link AStack} input, a nullable item output, a nullable fluid output - preserving CE's
     * exact {@code OutgasserRecipe} inner-class shape (no duration/power field - see class javadoc).
     */
    public static final class OutgasserRecipe {
        public final AStack input;
        public final ItemStack itemOutput;
        public final FluidStack fluidOutput;

        public OutgasserRecipe(AStack input, ItemStack itemOutput, FluidStack fluidOutput) {
            this.input = input;
            this.itemOutput = itemOutput;
            this.fluidOutput = fluidOutput;
        }
    }
}
