package com.hbm.inventory.recipes;

import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Ported from CE's {@code com.hbm.inventory.recipes.CokerRecipes} (159 lines, read in full;
 * {@code docs/phase7/mrec_11_centrifuge_misc.md}) - the Coker's recipe table: a
 * {@code Map<FluidType, Tuple.Triplet<Integer, ItemStack, FluidStack>>} keyed by input fluid type,
 * each value = {fluid quantity consumed, item byproduct (nullable), fluid byproduct (nullable)}.
 * CE's {@code registerDefaults()} makes 33 literal calls through 3 helper overloads
 * ({@code registerAuto}/{@code registerSFAuto}/{@code registerRecipe}, all delegating to the plain
 * {@code registerRecipe(FluidType, int, ItemStack, FluidStack)} kept here) - same shape as this
 * port's own {@link RefineryRecipes} precedent (a bespoke, non-JSON, hand-registered Java map; CE
 * never made this data-driven either).
 * <p>
 * <b>Scope trim (documented, not silent): 6 of CE's 33 entries are ported.</b> The 6 are CE's
 * {@code registerRecipe(...)} direct calls whose fluids and item output are all confirmed registered
 * in this port: {@code WATZ}, {@code REDMUD}, {@code CALCIUM_SOLUTION}, {@code SOURGAS} (item output
 * substituted, see below), {@code SLOP}, {@code VITRIOL}. <b>Not ported</b> (27 of 33): the 24
 * {@code registerAuto(fluid, outputType)} entries, the 1 {@code registerSFAuto(WOODOIL, ...)} entry,
 * and the {@code BITUMEN}/{@code LUBRICANT} {@code registerRecipe(...)} entries - every one of these
 * 26 needs {@code ModItems.coke} (CE's {@code ItemEnumMulti<EnumCokeType>}, 3 variants COAL/LIGNITE/
 * PETROLEUM) as fuel input or item output; the enum exists ({@code items/ItemEnums.java}) but no coke
 * item is registered under any name in this port. {@code SOURGAS}'s item output substitutes
 * {@link PlateCrystalWasteItems#CRYSTAL_SULFUR} for CE's plain {@code ModItems.sulfur}, the same
 * already-established convention {@code SILEXRecipes}/{@code CentrifugeRecipes}/{@code RefineryRecipes}/
 * {@code MixerRecipes} all use for this exact gap (see each class's own javadoc).
 * <p>
 * <b>Not yet built: the Coker block/block-entity/GUI/menu itself</b> (confirmed absent by the research
 * report - block, BE, container and screen are all zero for this machine, same as its {@code coke}
 * item dependency). This class is recipe data only, ready for whichever future pass builds
 * {@code com.hbm.blockentity.machine.oil.MachineCokerBlockEntity} (or similarly named) to consume via
 * {@link #getOutput(FluidType)} - matching {@code ArcWelderRecipes}'/{@code CrackingRecipes}' own
 * already-established "data ahead of machine" convention. It has zero call sites today.
 */
public final class CokerRecipes {

    public static final Map<FluidType, Tuple.Triplet<Integer, ItemStack, FluidStack>> RECIPES = new LinkedHashMap<>();

    private static boolean registered = false;

    private CokerRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        // CE: registerRecipe(WATZ, 4_000, new ItemStack(ModItems.ingot_mud, 4), null)
        registerRecipe(Fluids.WATZ, 4_000, new ItemStack(IngotNuggetItems.INGOT_MUD.get(), 4), null);

        // CE: registerRecipe(REDMUD, 450, new ItemStack(Items.IRON_INGOT, 1), new FluidStack(MERCURY, 50))
        registerRecipe(Fluids.REDMUD, 450, new ItemStack(Items.IRON_INGOT, 1), new FluidStack(Fluids.MERCURY, 50));

        // CE: registerRecipe(CALCIUM_SOLUTION, 125, new ItemStack(ModItems.powder_calcium), new FluidStack(SPENTSTEAM, 100))
        registerRecipe(Fluids.CALCIUM_SOLUTION, 125,
                new ItemStack(BilletPowderItems.POWDER_CALCIUM.get()), new FluidStack(Fluids.SPENTSTEAM, 100));

        // CE: registerRecipe(SOURGAS, 1_000, new ItemStack(ModItems.sulfur), new FluidStack(GAS_COKER, 150))
        // - only cokable gas to extract sulfur content (CE's own comment); sulfur substituted per
        // class javadoc's "Scope trim" note.
        registerRecipe(Fluids.SOURGAS, 1_000,
                new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get()), new FluidStack(Fluids.GAS_COKER, 150));

        // CE: registerRecipe(SLOP, 1000, new ItemStack(ModItems.powder_limestone), new FluidStack(COLLOID, 250))
        registerRecipe(Fluids.SLOP, 1000,
                new ItemStack(BilletPowderItems.POWDER_LIMESTONE.get()), new FluidStack(Fluids.COLLOID, 250));

        // CE: registerRecipe(VITRIOL, 4000, new ItemStack(ModItems.powder_iron), new FluidStack(SULFURIC_ACID, 500))
        registerRecipe(Fluids.VITRIOL, 4000,
                new ItemStack(BilletPowderItems.POWDER_IRON.get()), new FluidStack(Fluids.SULFURIC_ACID, 500));

        // Deliberately not ported (see class javadoc): the 24 registerAuto(...) coke-fueled entries,
        // the 1 registerSFAuto(WOODOIL, ...) entry, and the BITUMEN/LUBRICANT registerRecipe(...)
        // entries - all 26 need ModItems.coke, not registered under any name in this port.
    }

    /** CE's {@code registerRecipe(FluidType, int, ItemStack, FluidStack)} - the common delegation target. */
    public static void registerRecipe(FluidType type, int quantity, ItemStack output, FluidStack byproduct) {
        RECIPES.put(type, new Tuple.Triplet<>(quantity, output, byproduct));
    }

    /** Ported from CE's {@code CokerRecipes.getOutput}. */
    public static Tuple.Triplet<Integer, ItemStack, FluidStack> getOutput(FluidType type) {
        register();
        return RECIPES.get(type);
    }
}
