package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.weapon.grenade.GrenadeItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.LiquefactionRecipes} (168 lines, read in full) -
 * per {@code docs/phase7/mrec_10_crystallizer_misc.md}'s catalog and item/registry dependency check.
 * A simpler shape than {@link CrystallizerRecipes}: one input item, one output {@link FluidStack}, no
 * acid/reagent key dimension. Kept as a plain hardcoded Java registration list for the same reason
 * every sibling bespoke recipe class in this package stays that way (no vanilla {@code RecipeType}
 * represents an item-to-fluid conversion at all) - see {@link RefineryRecipes}'s own header for the
 * fuller rationale.
 * <p>
 * <b>No machine exists yet to consume this data</b> (confirmed absent: zero matches for "liquefact"
 * anywhere in this port's block/block-entity trees) - the Liquefactor block+block-entity is a
 * materially larger "new machine" task the research report explicitly recommends sequencing
 * separately (its own design pass following {@code MachineRefineryBlockEntity}'s established oil-chain
 * shape), not bundled into "port the recipe data" here. Landing the data now (matching the identical,
 * already-established precedent {@code RockMillRecipes} set for its own machine-less recipe class) costs
 * nothing and leaves nothing for a future machine pass to re-derive.
 * <p>
 * <b>Corrections vs. CE's 1.12.2 references</b> (documented, not silent):
 * <ul>
 * <li>CE's {@code Blocks.SNOW} is the 1.12.2 *solid, compacted* snow block - the 1.13 flattening
 * swapped the "snow"/"snow_layer" ids between the thin decorative layer and the solid block, so the
 * correct modern equivalent is {@link Blocks#SNOW_BLOCK}, not {@link Blocks#SNOW} (which is now the
 * thin layer CE called {@code snow_layer}).</li>
 * <li>CE's {@code Items.MELON} is the 1.12.2 *edible slice* - in 1.13+ that item was renamed
 * {@code melon_slice} ({@link Items#MELON_SLICE}); modern {@link Items#MELON} is the whole melon
 * block's item form instead (not edible, a different CE concept entirely).</li>
 * </ul>
 * <b>Ore-dict/wildcard fan-out</b> (documented, not silent - this port's {@link ComparableStack} has
 * no ore-dict-tag or wildcard-meta matching, see that class's own header): CE's three wildcard/tag
 * keys are each expanded into their concrete modern-item equivalents rather than dropped:
 * {@code KEY_LOG} (ore-dict {@code "logWood"}) -> the 6 classic overworld logs; {@code Items.FISH}
 * (1.12.2 {@code RAW_FISH} meta 0-3) -> {@link Items#COD}/{@link Items#SALMON}/
 * {@link Items#TROPICAL_FISH}/{@link Items#PUFFERFISH}; {@code Blocks.TALLGRASS} meta 1/2 ->
 * {@link Blocks#SHORT_GRASS}/{@link Blocks#FERN}. This is a real, deliberate one-CE-entry-to-many-
 * port-entries expansion, not new content - every resulting entry is still exactly CE's recipe
 * (same output fluid/amount) applied to one of the concrete items CE's single wildcard entry matched.
 * <p>
 * <b>Scope trim vs. CE</b>: {@code KEY_OIL_TAR}/{@code KEY_CRACK_TAR}/{@code KEY_COAL_TAR} (CE's
 * {@code oil_tar} family, confirmed absent from this port - see {@link RefineryRecipes}'s own header)
 * and {@code ModItems.biomass} (confirmed absent) are not ported; CE's {@code COAL.gem()}/
 * {@code LIGNITE.gem()} variants collapse onto this port's single dust-shape item for each material
 * ({@link BilletPowderItems#POWDER_COAL}/{@link BilletPowderItems#POWDER_LIGNITE} - no separate "gem"
 * shape exists here), so only one entry per material is registered rather than CE's two near-duplicate
 * ones. CE's {@code ItemFood} fallback ({@code getOutput}'s tail branch, converting any unmatched food
 * item into {@link Fluids#SALIENT} scaled by its nutrition/saturation) is ported using this port's
 * {@link FoodProperties} data component in place of 1.12.2's {@code ItemFood} methods.
 */
public final class LiquefactionRecipes {

    private static final Map<ComparableStack, FluidStack> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private LiquefactionRecipes() {
    }

    /** Same lazy-registration rationale as {@link CrystallizerRecipes#registerDefaults()} - avoids
     * resolving {@code DeferredItem.get()}/registry lookups before {@code RegisterEvent} has fired. */
    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;

        // ---- oil-processing group (CE lines 33-44) ----
        // COAL.gem()/COAL.dust() and LIGNITE.gem()/LIGNITE.dust() each collapse onto this port's
        // single dust-shape item (see class javadoc).
        put(new ComparableStack(BilletPowderItems.POWDER_COAL.get()), Fluids.COALOIL, 250);
        put(new ComparableStack(BilletPowderItems.POWDER_LIGNITE.get()), Fluids.COALOIL, 150);
        // KEY_OIL_TAR/KEY_CRACK_TAR/KEY_COAL_TAR: blocked, oil_tar family not registered (see javadoc).

        // KEY_LOG (CE's ore-dict "logWood") - expanded to the 6 classic overworld logs (see javadoc).
        for (Item log : new Item[]{Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG}) {
            put(new ComparableStack(log), Fluids.MUG, 100);
        }

        put(new ComparableStack(BilletPowderItems.POWDER_SODIUM.get()), Fluids.SODIUM, 100);
        put(new ComparableStack(IngotNuggetItems.INGOT_LEAD.get()), Fluids.LEAD, 100);
        put(new ComparableStack(BilletPowderItems.POWDER_LEAD.get()), Fluids.LEAD, 100);
        put(new ComparableStack(hbmBlock("lead_block")), Fluids.LEAD, 900);

        // ---- general utility group (CE lines 46-55) ----
        put(new ComparableStack(Blocks.NETHERRACK), Fluids.LAVA, 250);
        put(new ComparableStack(Blocks.COBBLESTONE), Fluids.LAVA, 250);
        put(new ComparableStack(Blocks.STONE), Fluids.LAVA, 250);
        put(new ComparableStack(Blocks.OBSIDIAN), Fluids.LAVA, 500);
        put(new ComparableStack(Items.SNOWBALL), Fluids.WATER, 125);
        // CE's Blocks.SNOW (1.12.2 solid compacted block) = modern Blocks.SNOW_BLOCK, see javadoc.
        put(new ComparableStack(Blocks.SNOW_BLOCK), Fluids.WATER, 500);
        put(new ComparableStack(Blocks.ICE), Fluids.WATER, 1000);
        put(new ComparableStack(Blocks.PACKED_ICE), Fluids.WATER, 1000);
        put(new ComparableStack(Items.ENDER_PEARL), Fluids.ENDERJUICE, 100);
        put(new ComparableStack(hbmBlock("ore_oil_sand")), Fluids.BITUMEN, 100);

        put(new ComparableStack(Items.SUGAR), Fluids.ETHANOL, 100);
        // CE's Items.MELON (1.12.2 edible slice) = modern Items.MELON_SLICE, see javadoc.
        put(new ComparableStack(Items.MELON_SLICE), Fluids.ETHANOL, 100);
        // CE's ModBlocks.plant_flower meta 3/4 = EnumFlowerPlantType.MUSTARD_WILLOW_1/NIGHTSHADE
        // (index-matched against com.hbm.blocks.PlantEnums's verbatim-ported enum), registered by
        // PlantBlocks as "plant_flower_mustard_willow_1"/"plant_flower_nightshade".
        put(new ComparableStack(hbmBlock("plant_flower_mustard_willow_1")), Fluids.ETHANOL, 100);
        put(new ComparableStack(hbmBlock("plant_flower_nightshade")), Fluids.ETHANOL, 50);
        // ModItems.biomass: blocked, not registered anywhere in this port.
        put(new ComparableStack(GrenadeItems.GLYPHID_GLAND_EMPTY.get()), Fluids.BIOGAS, 2000);
        // Items.FISH wildcard (CE's 1.12.2 RAW_FISH meta 0-3) - expanded to the 4 concrete fish items.
        for (Item fish : new Item[]{Items.COD, Items.SALMON, Items.TROPICAL_FISH, Items.PUFFERFISH}) {
            put(new ComparableStack(fish), Fluids.FISHOIL, 100);
        }
        put(new ComparableStack(Blocks.SUNFLOWER), Fluids.SUNFLOWEROIL, 100);

        put(new ComparableStack(Items.WHEAT_SEEDS), Fluids.SEEDSLURRY, 50);
        // CE's Blocks.TALLGRASS meta 1/2 (grass/fern) = modern Blocks.SHORT_GRASS/Blocks.FERN.
        put(new ComparableStack(Blocks.SHORT_GRASS), Fluids.SEEDSLURRY, 100);
        put(new ComparableStack(Blocks.FERN), Fluids.SEEDSLURRY, 100);
        put(new ComparableStack(Blocks.VINE), Fluids.SEEDSLURRY, 100);
    }

    private static void put(ComparableStack input, FluidType type, int amount) {
        input.makeSingular();
        RECIPES.put(input, new FluidStack(type, amount));
    }

    private static Block hbmBlock(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * Matches CE's {@code getOutput(ItemStack)}: exact-item lookup, falling back (like CE's own
     * {@code instanceof ItemFood} tail branch) to a nutrition/saturation-scaled {@link Fluids#SALIENT}
     * stack for any food item with no explicit recipe, via this port's {@link FoodProperties} data
     * component in place of CE's 1.12.2 {@code ItemFood} accessor methods.
     */
    public static FluidStack getOutput(ItemStack stack) {
        registerDefaults();
        if (stack == null || stack.isEmpty()) return null;
        ComparableStack comp = new ComparableStack(stack).makeSingular();
        FluidStack match = RECIPES.get(comp);
        if (match != null) return match;

        FoodProperties food = stack.get(DataComponents.FOOD);
        if (food != null) {
            return new FluidStack(Fluids.SALIENT, (int) (food.nutrition() * food.saturation() * 20));
        }
        return null;
    }

    /** Full-collection accessor for a future JEI category, matching {@link CrystallizerRecipes#getAllRecipes()}'s own precedent. */
    public static Map<ComparableStack, FluidStack> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }
}
