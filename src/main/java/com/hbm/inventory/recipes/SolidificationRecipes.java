package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.blocks.machine.OilChainBlocks;
import com.hbm.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Recipe data for the Solidifier machine, ported from CE's
 * {@code com.hbm.inventory.recipes.SolidificationRecipes} (195 ln, read in full - see
 * {@code docs/phase7/mrec_05_purex_misc.md}). CE's real shape is a single-input-fluid-type-keyed
 * {@code HashMap<FluidType, Tuple.Pair<Integer, ItemStack>>} ({@code SerializableRecipe}, JSON-backed);
 * this class reproduces that exact shape as a plain static table, matching {@link RefineryRecipes}
 * (this class's direct analogue - both are single-fluid-type-keyed, no chance outputs, no multi-input
 * complexity) rather than inventing new recipe-loader machinery for one more consumer.
 *
 * <p><b>No {@code MachineSolidifier} block/block-entity exists in this port yet</b> (confirmed absent) -
 * this class is pure recipe data for whichever future task builds that machine, exposed via
 * {@link #getAll()} (defensive-lazy registration, same pattern as
 * {@link RefineryRecipes#getAllRefinery()} - no eager bootstrap call needs wiring into any shared
 * aggregator file today).</p>
 *
 * <p><b>Scope: only CE's fully item-ready entries are ported</b> (per this task's ground rules - do
 * not stub missing items). Of CE's 47 distinct {@code FluidType}-keyed entries, exactly <b>9</b> have
 * every output item already registered in this port - the direct, non-{@code oil_tar}/
 * {@code solid_fuel*}/{@code biomass}/{@code bio_wafer} entries below. The other 38 are <b>not</b>
 * ported here:</p>
 * <ul>
 *   <li><b>{@code oil_tar} (+ {@code ItemEnums.EnumTarType})</b> - not a registered item in this port
 *   yet (corroborated by {@code RefineryRecipes.java}'s own TODO). Blocks all 9 {@code oil_tar}-keyed
 *   entries (OIL/CRACKOIL/COALOIL/HEAVYOIL/HEAVYOIL_VACUUM/BITUMEN/COALCREOSOTE/WOODOIL/LUBRICANT).</li>
 *   <li><b>{@code solid_fuel}/{@code solid_fuel_bf}</b> - not found anywhere in this port. Blocks all
 *   27 {@code registerSFAuto}-generated entries (26 use {@code solid_fuel}, the {@code BALEFIRE}
 *   override uses {@code solid_fuel_bf}). Per the research report: those 27 {@code mB} values are
 *   <i>computed</i> by CE's {@code registerSFAuto} formula from each fluid's
 *   {@code FT_Flammable#getHeatEnergy()} trait, not pre-authored constants - if/when {@code solid_fuel}/
 *   {@code solid_fuel_bf} land, port {@code registerSFAuto} itself (reproduced in CE's own source as:
 *   {@code mB = tuPerSF * 1000 * 1.25 / fluid.getTrait(FT_Flammable.class).getHeatEnergy()}, rounded
 *   down to a clean multiple of 10/100/1000 by magnitude) as a Java method here, not 27 hand-transcribed
 *   numbers, so it stays correct if a fluid's heat-energy trait value ever changes.</li>
 *   <li><b>{@code biomass_compressed}, {@code bio_wafer}</b> - not registered (named as a known,
 *   deliberate deferral in {@code FoodItems.java}'s own comment). Blocks the BIOGAS and SALIENT
 *   entries.</li>
 * </ul>
 *
 * <p><b>Field-name quirk carried over correctly</b>: CE's {@code MERCURY} entry outputs
 * {@code ModItems.ingot_mercury} - but that CE field's real registry id is {@code "nugget_mercury"}
 * (a CE field-name/id mismatch this port's own {@link IngotNuggetItems} already documents), so this
 * class outputs {@link IngotNuggetItems#NUGGET_MERCURY}, not a nonexistent {@code ingot_mercury}.</p>
 */
public final class SolidificationRecipes {

    private static final Map<FluidType, Tuple.Pair<Integer, ItemStack>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private SolidificationRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        put(Fluids.WATER, 1000, new ItemStack(Blocks.ICE));
        put(Fluids.LAVA, 1000, new ItemStack(Blocks.OBSIDIAN));
        put(Fluids.MERCURY, 125, new ItemStack(IngotNuggetItems.NUGGET_MERCURY.get()));
        put(Fluids.ENDERJUICE, 100, new ItemStack(Items.ENDER_PEARL));
        put(Fluids.WATZ, 1000, new ItemStack(IngotNuggetItems.INGOT_MUD.get()));
        put(Fluids.REDMUD, 450, new ItemStack(Items.IRON_INGOT));
        put(Fluids.SODIUM, 100, new ItemStack(BilletPowderItems.POWDER_SODIUM.get()));
        put(Fluids.LEAD, 100, new ItemStack(IngotNuggetItems.INGOT_LEAD.get()));
        put(Fluids.SLOP, 250, new ItemStack(OilChainBlocks.ORE_OIL_SAND.get()));
    }

    private static void put(FluidType type, int mB, ItemStack output) {
        RECIPES.put(type, new Tuple.Pair<>(mB, output));
    }

    public static Tuple.Pair<Integer, ItemStack> getOutput(FluidType type) {
        register();
        return RECIPES.get(type);
    }

    /**
     * Full-collection accessor, defensively calling {@link #register()} first - same pattern as
     * {@link RefineryRecipes#getAllRefinery()}.
     */
    public static Map<FluidType, Tuple.Pair<Integer, ItemStack>> getAll() {
        register();
        return java.util.Collections.unmodifiableMap(RECIPES);
    }
}
