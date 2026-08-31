package com.hbm.inventory.recipes.chem;

import com.hbm.config.GeneralConfig;
import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.machine.ItemArcElectrode;
import com.hbm.items.machine.MachineItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for the Chemical Plant, ported from CE's {@code com.hbm.inventory.recipes.
 * ChemicalPlantRecipes} - a {@code GenericRecipes<GenericRecipe>} singleton in CE
 * ({@code docs/phase2/machines_chemical_isotope.md}'s Chemical Plant section). Per the task's own
 * scaffolding guidance ("prefer porting CE's real recipe data...into a JSON-backed
 * HbmSimpleRecipe-shaped recipe type you define for your machine family... do not block on a bigger
 * recipe-system redesign - port the data now") and this port's own {@code GenericRecipe}/
 * {@code GenericRecipes} stand-in (see that package's header: it deliberately does NOT carry the real
 * multi-input/fluid machine-recipe shape CE's Chemical Plant needs), this class defines its own small
 * {@link ChemPlantRecipe} data shape - up to 3 item inputs (each an {@link AStack}, so tag or exact
 * matches both work the same way {@link com.hbm.inventory.recipes.chem.CentrifugeRecipes} does), up
 * to 2-3 fluid inputs, up to 3 item outputs, up to 2 fluid outputs, duration + power - as a plain
 * static table, the same "port now, JSON-override later" shape {@code RefineryRecipes} already
 * established this pass, rather than reusing the unrelated blueprint-pool {@code GenericRecipe}
 * stand-in.
 * <p>
 * <b>Recognition model differs from CE</b> (documented): CE's Chemical Plant is <i>player-selected</i>
 * (a GUI dropdown picks one recipe by name, {@code IControlReceiver}/{@code receiveControl}), not
 * automatically matched. {@code com.hbm.blockentity.machine.chem.ChemPlantBlockEntity} instead
 * auto-recognizes the active recipe from whatever item/fluid currently sits in the input slots/tanks -
 * the same automatic-recognition model the item Centrifuge and every other machine in this pass use -
 * since the named-recipe-pool GUI control channel is a separate, not-yet-existing cross-cutting
 * mechanism (see {@code docs/phase2/machines_chemical_isotope.md}'s Deferred scope #6). CE's own
 * per-recipe "pool" tags ({@code .setPools(...)}, a progression/discovery-unlock grouping consumed by
 * that same GUI dropdown) are dropped globally for the same reason - there is no pool-gated UI here
 * for them to feed.
 * <p>
 * <b>{@code mrec-08-chemplant-misc} pass</b> (see {@code docs/phase7/mrec_08_chemplant_misc.md}):
 * extended this file from CE's first 4 entries to 51 of CE's 72 (47 new). The research report's own
 * "confirmed present" item list turned out to be wrong on several items when independently
 * re-checked against the live tree (not merely guessed at differently) - {@code moon_turf},
 * {@code oil_tar}, {@code fuel_additive}, {@code pellet_charged}, {@code ModItems.dust} (a generic
 * undifferentiated-dust item, distinct from the {@link MaterialShapes#DUST} shape token),
 * {@code ball_tnt}, {@code ball_dynamite}, {@code canister_full}/{@code canister_empty}/
 * {@code canister_napalm}, and bare {@code sulfur}/{@code niter}/{@code fluorite} items are all
 * confirmed absent (grepped for real registration call sites, not doc mentions) - these block 21 of
 * CE's 72 entries, catalogued in the "NOT PORTED" block at the end of {@link #register()}. The bare
 * {@code sulfur}/{@code niter}/{@code fluorite} case is not a new gap this file discovered on its own:
 * {@code CentrifugeRecipes}/{@code SILEXRecipes}/{@code GasCentrifugeRecipes}/{@code MixerRecipes} all
 * already independently established the same standing substitution this file reuses -
 * {@link PlateCrystalWasteItems#CRYSTAL_SULFUR}/{@link PlateCrystalWasteItems#CRYSTAL_NITER}/
 * {@link PlateCrystalWasteItems#CRYSTAL_FLUORITE} in place of CE's {@code S.dust()}/{@code KNO.dust()}/
 * {@code F.dust()}.
 * <p>
 * <b>OR-match gap</b>: CE's cross-material {@code DictGroup} wildcards ({@code ANY_PLASTIC},
 * {@code ANY_HARDPLASTIC}, {@code ANY_BISMOIDBRONZE} - "matches any material in this group") have no
 * port-side {@link AStack} equivalent (confirmed absent by {@code ArcWelderRecipes}'s own javadoc).
 * Rather than silently narrow CE's real accepted-input set down to one material or drop those 3
 * recipes outright, {@link AnyOfStack} below is a small local OR-match combinator (not touching the
 * shared {@code RecipesCommon} class other tasks also depend on) that preserves CE's actual semantics.
 * <p>
 * <b>Known BE gap, not fixed by this pass</b>: {@code ChemPlantBlockEntity}'s 3 input tanks are
 * hardcoded to exactly {@code WATER}/{@code AIR}/{@code LAVA} (see that class's constructor javadoc -
 * a deliberate, documented, pre-existing limitation, not something this pass introduced). Every new
 * entry below whose {@code inputFluids} needs a type outside that fixed set (the large majority - the
 * battery/solid/acid/coltan/nuclear-processing families in particular) is therefore data-complete and
 * JEI-visible but not yet fully reachable in actual gameplay until a future pass gives the Chemical
 * Plant flexible/retypeable input tanks (matching CE's own dropped-item-identifier retyping mechanic,
 * which this port has not built). This mirrors the same "port the data now, the mechanism catches up
 * later" precedent {@code ArcWelderRecipes}/{@code RockMillRecipes} already establish for machines
 * with recipe data but no consuming block/BE at all yet - this machine at least exists and already
 * correctly processes every entry using only {@code WATER}/{@code AIR}/{@code LAVA} (or no fluid
 * input at all).
 */
public final class ChemPlantRecipes {

    public static final List<ChemPlantRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private ChemPlantRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        /// REGULAR FLUIDS ///

        RECIPES.add(new ChemPlantRecipe("chem.hydrogen", 20, 400,
                new AStack[]{OreDictStack.ofCommonTag("coals")},
                new FluidStack[]{new FluidStack(Fluids.WATER, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.HYDROGEN, 500)));

        // CE ANY_COKE.gem() - a 3-member DictGroup (block_coke_coal/lignite/petroleum), no single
        // common tag exists for it in this port (see AnyOfStack javadoc) - OR-matched here.
        RECIPES.add(new ChemPlantRecipe("chem.hydrogencoke", 20, 400,
                new AStack[]{anyResolvedItem(1, "block_coke_coal", "block_coke_lignite", "block_coke_petroleum")},
                new FluidStack[]{new FluidStack(Fluids.WATER, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.HYDROGEN, 500)));

        RECIPES.add(new ChemPlantRecipe("chem.oxygen", 20, 400,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.OXYGEN, 500)));

        RECIPES.add(new ChemPlantRecipe("chem.xenon", 300, 1_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 16_000)},
                new ItemStack[0],
                new FluidStack(Fluids.XENON, 50)));

        RECIPES.add(new ChemPlantRecipe("chem.xenonoxy", 20, 1_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000), new FluidStack(Fluids.OXYGEN, 250)},
                new ItemStack[0],
                new FluidStack(Fluids.XENON, 50)));

        // NOT PORTED: chem.helium3 (ModBlocks.moon_turf x8 input) - moon_turf confirmed absent
        // anywhere in this port (see class javadoc).

        RECIPES.add(new ChemPlantRecipe("chem.co2", 60, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.GAS, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.CARBONDIOXIDE, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.perfluoromethyl", 20, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.PETROLEUM, 1_000), new FluidStack(Fluids.UNSATURATEDS, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.PERFLUOROMETHYL, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.cccentrifuge", 200, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.CHLOROCALCITE_CLEANED, 500), new FluidStack(Fluids.SULFURIC_ACID, 8_000)},
                new ItemStack[0],
                new FluidStack(Fluids.POTASSIUM_CHLORIDE, 250), new FluidStack(Fluids.CALCIUM_CHLORIDE, 250)));

        /// OILS ///

        RECIPES.add(new ChemPlantRecipe("chem.ethanol", 50, 100,
                new AStack[]{new ComparableStack(Items.SUGAR, 10)},
                new FluidStack[0],
                new ItemStack[0],
                new FluidStack(Fluids.ETHANOL, 1_000)));

        // NOT PORTED: chem.biogas (ModItems.biomass x16 input) - biomass confirmed absent.

        RECIPES.add(new ChemPlantRecipe("chem.biofuel", 60, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.BIOGAS, 1_500), new FluidStack(Fluids.ETHANOL, 250)},
                new ItemStack[0],
                new FluidStack(Fluids.BIOFUEL, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.reoil", 40, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.SMEAR, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.RECLAIMED, 800)));

        RECIPES.add(new ChemPlantRecipe("chem.gasoline", 40, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.NAPHTHA, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.GASOLINE, 800)));

        // NOT PORTED: chem.tarsand, chem.tel - both need CE's ANY_TAR (oil_tar/EnumTarType), and
        // oil_tar is confirmed absent from this port (corroborated by RefineryRecipes/
        // SolidificationRecipes/CombinationRecipes/PyroOvenRecipes, which each independently document
        // the same gap). chem.tel additionally needs fuel_additive (also absent).
        // NOT PORTED: chem.deicer (fuel_additive output, confirmed absent).

        /// THE CONC AND ASPHALE ///

        RECIPES.add(new ChemPlantRecipe("chem.cobble", 20, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25)},
                new ItemStack[]{new ItemStack(Blocks.COBBLESTONE)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.stone", 60, 500,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 25), new FluidStack(Fluids.AIR, 4_000)},
                new ItemStack[]{new ItemStack(Blocks.STONE)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.obsidian", 60, 500,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000), new FluidStack(Fluids.LAVA, 500), new FluidStack(Fluids.AIR, 4_000)},
                new ItemStack[]{new ItemStack(Blocks.OBSIDIAN)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.aggregate", 320, 500,
                new AStack[]{new ComparableStack(Blocks.COBBLESTONE.asItem(), 16)},
                new FluidStack[0],
                new ItemStack[]{new ItemStack(Blocks.GRAVEL, 8), new ItemStack(Blocks.SAND, 8)},
                null));

        // NOT PORTED: chem.concrete, chem.concreteasbestos - both output ModBlocks.concrete_smooth/
        // concrete_asbestos, confirmed absent (only concrete_pillar/super_*/<color>/ext_* variants
        // exist, not the plain "smooth"/"asbestos" bases CE's chem plant needs).

        RECIPES.add(new ChemPlantRecipe("chem.ducrete", 150, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CEMENT.get(), 4),
                        new ComparableStack(IngotNuggetItems.INGOT_FERROURANIUM.get(), 1),
                        new ComparableStack(Blocks.SAND.asItem(), 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[]{new ItemStack(resolveItem("ducrete_smooth"), 8)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.liquidconk", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CEMENT.get(), 1),
                        new ComparableStack(Blocks.GRAVEL.asItem(), 8),
                        new ComparableStack(Blocks.SAND.asItem(), 8)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[0],
                new FluidStack(Fluids.CONCRETE, 16_000)));

        RECIPES.add(new ChemPlantRecipe("chem.asphalt", 100, 100,
                new AStack[]{new ComparableStack(Blocks.GRAVEL.asItem(), 2), new ComparableStack(Blocks.SAND.asItem(), 6)},
                new FluidStack[]{new FluidStack(Fluids.BITUMEN, 1_000)},
                new ItemStack[]{new ItemStack(resolveItem("asphalt"), 16)},
                null));

        /// BATTERIES ///

        RECIPES.add(new ChemPlantRecipe("chem.batterylead", 100, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.PLATE_STEEL.get(), 4),
                        new ComparableStack(IngotNuggetItems.INGOT_LEAD.get(), 4)},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 8_000)},
                new ItemStack[]{new ItemStack(resolveItem("battery_lead_pack"), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.batterylithium", 100, 1_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_LITHIUM.get(), 12),
                        new ComparableStack(BilletPowderItems.POWDER_COBALT.get(), 8),
                        anyItem(4, IngotNuggetItems.INGOT_POLYMER, IngotNuggetItems.INGOT_BAKELITE)},
                new FluidStack[]{new FluidStack(Fluids.OXYGEN, 2_000)},
                new ItemStack[]{new ItemStack(resolveItem("battery_lithium_pack"), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.batterysodium", 100, 10_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_SODIUM.get(), 24),
                        new ComparableStack(BilletPowderItems.POWDER_IRON.get(), 24),
                        anyItem(12, IngotNuggetItems.INGOT_PC, IngotNuggetItems.INGOT_PVC)},
                new FluidStack[0],
                new ItemStack[]{new ItemStack(resolveItem("battery_sodium_pack"), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.batteryschrabidium", 100, 25_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 24),
                        anyTag(8, MaterialShapes.CASTPLATE.commonTag(Mats.MAT_BBRONZE), MaterialShapes.CASTPLATE.commonTag(Mats.MAT_ABRONZE))},
                new FluidStack[]{new FluidStack(Fluids.HELIUM4, 8_000)},
                new ItemStack[]{new ItemStack(resolveItem("battery_schrabidium_pack"), 1)},
                null));

        // NOT PORTED: chem.batteryquantum (ModItems.pellet_charged x32 input, confirmed absent).

        /// SOLIDS ///

        RECIPES.add(new ChemPlantRecipe("chem.desh", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_DESH_MIX.get(), 1)},
                simpleChemistry()
                        ? new FluidStack[]{new FluidStack(Fluids.LIGHTOIL, 200)}
                        : new FluidStack[]{new FluidStack(Fluids.LIGHTOIL, 200), new FluidStack(Fluids.MERCURY, 200)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_DESH.get(), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.deshcracked", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_DESH_MIX.get(), 1)},
                simpleChemistry()
                        ? new FluidStack[]{new FluidStack(Fluids.LIGHTOIL_CRACK, 500)}
                        : new FluidStack[]{new FluidStack(Fluids.LIGHTOIL_CRACK, 500, 1), new FluidStack(Fluids.MERCURY, 100)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_DESH.get(), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.polymer", 100, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_COAL.get(), 2),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.PETROLEUM, 1_000, pressurized() ? 1 : 0)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_POLYMER.get(), 4)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.bakelite", 100, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AROMATICS, 500, pressurized() ? 1 : 0),
                        new FluidStack(Fluids.PETROLEUM, 500, pressurized() ? 1 : 0)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_BAKELITE.get(), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.rubber", 100, 200,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.UNSATURATEDS, 500, pressurized() ? 2 : 0)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_RUBBER.get(), 2)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.hardplastic", 100, 1_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.XYLENE, 500, pressurized() ? 2 : 0),
                        new FluidStack(Fluids.PHOSGENE, 500, pressurized() ? 2 : 0)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_PC.get(), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.pvc", 100, 1_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_CADMIUM.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.UNSATURATEDS, 250, pressurized() ? 2 : 0),
                        new FluidStack(Fluids.CHLORINE, 250, pressurized() ? 2 : 0)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_PVC.get(), 2)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.kevlar", 60, 300,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AROMATICS, 200), new FluidStack(Fluids.NITRIC_ACID, 100),
                        new FluidStack(pressurized() ? Fluids.PHOSGENE : Fluids.CHLORINE, 100)},
                new ItemStack[]{new ItemStack(PlateCrystalWasteItems.PLATE_KEVLAR.get(), 4)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.meth", 60, 300,
                new AStack[]{new ComparableStack(Items.WHEAT, 1), new ComparableStack(Items.COCOA_BEANS, 2)},
                new FluidStack[]{new FluidStack(Fluids.LUBRICANT, 400), new FluidStack(Fluids.PEROXIDE, 500)},
                new ItemStack[]{new ItemStack(resolveItem("chocolate"), 4)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.epearl", 100, 300,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_DIAMOND.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.XPJUICE, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.ENDERJUICE, 100)));

        RECIPES.add(new ChemPlantRecipe("chem.meatprocessing", 200, 200,
                new AStack[]{new ComparableStack(resolveItem("glyphid_meat"), 3)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[]{new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 4),
                        new ItemStack(PlateCrystalWasteItems.CRYSTAL_NITER.get(), 3)},
                new FluidStack(Fluids.SALIENT, 250)));

        // NOT PORTED: chem.rustysteel (ModBlocks.deco_steel/deco_rusty_steel, both confirmed absent).
        // NOT PORTED: chem.biosolidfuel, chem.biooilsolidfuel (ModItems.biomass_compressed input and
        // ModItems.solid_fuel output, both confirmed absent).

        RECIPES.add(new ChemPlantRecipe("chem.oilelectrodes", 600, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.HEATINGOIL, 4_000)},
                new ItemStack[]{new ItemStack(MachineItems.ARC_ELECTRODES.get(ItemArcElectrode.EnumElectrodeType.GRAPHITE).get(), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.lubeelectrodes", 600, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.LUBRICANT, 8_000)},
                new ItemStack[]{new ItemStack(MachineItems.ARC_ELECTRODES.get(ItemArcElectrode.EnumElectrodeType.GRAPHITE).get(), 1)},
                null));

        /// ACIDS ///

        RECIPES.add(new ChemPlantRecipe("chem.peroxide", 50, 100,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.PEROXIDE, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.sulfuricacid", 50, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 1_000), new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.SULFURIC_ACID, 2_000)));

        RECIPES.add(new ChemPlantRecipe("chem.nitricacid", 50, 100,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.NITRIC_ACID, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.birkeland", 200, 5_000,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.AIR, 8_000), new FluidStack(Fluids.WATER, 2_000)},
                new ItemStack[0],
                new FluidStack(Fluids.NITRIC_ACID, 1_000)));

        // NOT PORTED: chem.schrabidic (ModItems.pellet_charged x1 input, confirmed absent).

        RECIPES.add(new ChemPlantRecipe("chem.schrabidate", 150, 5_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_IRON.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.SCHRABIDIC, 250)},
                new ItemStack[]{new ItemStack(BilletPowderItems.POWDER_SCHRABIDATE.get(), 1)},
                null));

        /// COLTAN ///

        // NOT PORTED: chem.coltancleaning (outputs ModItems.dust, a generic undifferentiated-dust
        // item confirmed absent - distinct from the MaterialShapes DUST shape token).

        RECIPES.add(new ChemPlantRecipe("chem.coltanpain", 120, 100,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_COLTAN.get(), 1),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.GAS, 1_000), new FluidStack(Fluids.OXYGEN, 500)},
                new ItemStack[0],
                new FluidStack(Fluids.PAIN, 1_000)));

        // NOT PORTED: chem.coltancrystal (also outputs ModItems.dust x3, confirmed absent).

        /// EXPLOSIVES ///

        // NOT PORTED: chem.cordite (output ModItems.cordite, confirmed absent).
        // NOT PORTED: chem.rocketfuel (ModItems.solid_fuel input and ModItems.rocket_fuel output,
        // both confirmed absent).
        // NOT PORTED: chem.dynamite (output ModItems.ball_dynamite, confirmed absent).
        // NOT PORTED: chem.tnt (output ModItems.ball_tnt, confirmed absent).
        // NOT PORTED: chem.tatb (input ModItems.ball_tnt and output ModItems.ball_tatb, both absent).

        RECIPES.add(new ChemPlantRecipe("chem.c4", 100, 1_000,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.CRYSTAL_NITER.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.UNSATURATEDS, 500, pressurized() ? 1 : 0)},
                new ItemStack[]{new ItemStack(IngotNuggetItems.INGOT_C4.get(), 4)},
                null));

        // NOT PORTED: chem.napalm (ModItems.canister_empty input and ModItems.canister_napalm
        // output, both confirmed absent - only the data-component-based ItemCanister exists in this
        // port, see ItemCanister's own javadoc).

        /// GLASS ///

        RECIPES.add(new ChemPlantRecipe("chem.laminate", 20, 100,
                new AStack[]{new OreDictStack(ANY_GLASS_BLOCK, 1),
                        new OreDictStack(MaterialShapes.BOLT.commonTag(Mats.MAT_STEEL), 4)},
                new FluidStack[]{new FluidStack(Fluids.XYLENE, 50), new FluidStack(Fluids.PHOSGENE, 50)},
                new ItemStack[]{new ItemStack(resolveItem("reinforced_laminate"), 1)},
                null));

        // NOT PORTED: chem.polarized (output ModItems.part_generic[GLASS_POLARIZED] - ItemEnums.
        // EnumPartType exists as a bare enum but no part_generic item family is registered from it).

        /// NUCLEAR PROCESSING ///

        RECIPES.add(new ChemPlantRecipe("chem.yellowcake", 250, 500,
                new AStack[]{new ComparableStack(BilletPowderItems.BILLET_URANIUM.get(), 2),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 2)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 500)},
                new ItemStack[]{new ItemStack(BilletPowderItems.POWDER_YELLOWCAKE.get(), 1)},
                null));

        RECIPES.add(new ChemPlantRecipe("chem.uf6", 100, 500,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_YELLOWCAKE.get(), 1),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 4)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[]{new ItemStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 2)},
                new FluidStack(Fluids.UF6, 1_200)));

        RECIPES.add(new ChemPlantRecipe("chem.puf6", 200, 500,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_PLUTONIUM.get(), 1),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 3)},
                new FluidStack[]{new FluidStack(Fluids.WATER, 1_000)},
                new ItemStack[0],
                new FluidStack(Fluids.PUF6, 900)));

        RECIPES.add(new ChemPlantRecipe("chem.sas3", 200, 5_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_SCHRABIDIUM.get(), 1),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_SULFUR.get(), 2)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 2_000)},
                new ItemStack[0],
                new FluidStack(Fluids.SAS3, 1_000)));

        RECIPES.add(new ChemPlantRecipe("chem.balefire", 100, 10_000,
                new AStack[]{new ComparableStack(NukeCasingItems.EGG_BALEFIRE_SHARD.get(), 1)},
                new FluidStack[]{new FluidStack(Fluids.KEROSENE, 6_000)},
                new ItemStack[]{new ItemStack(BilletPowderItems.POWDER_BALEFIRE.get(), 1)},
                new FluidStack(Fluids.BALEFIRE, 8_000)));

        RECIPES.add(new ChemPlantRecipe("chem.dhc", 400, 500,
                new AStack[0],
                new FluidStack[]{new FluidStack(Fluids.DEUTERIUM, 500), new FluidStack(Fluids.REFORMGAS, 250), new FluidStack(Fluids.SYNGAS, 250)},
                new ItemStack[0],
                new FluidStack(Fluids.DHC, 500)));

        /// OSMIRIDIUM ///

        RECIPES.add(new ChemPlantRecipe("chem.osmiridiumdeath", 240, 1_000,
                new AStack[]{new ComparableStack(BilletPowderItems.POWDER_PALEOGENITE.get(), 1),
                        new ComparableStack(PlateCrystalWasteItems.CRYSTAL_FLUORITE.get(), 8),
                        new ComparableStack(IngotNuggetItems.NUGGET_BISMUTH.get(), 4)},
                new FluidStack[]{new FluidStack(Fluids.PEROXIDE, 1_000, 5)},
                new ItemStack[0],
                new FluidStack(Fluids.DEATH, 1_000, 0)));
    }

    /** CE: {@code GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleChemsitry} [sic]. */
    private static boolean simpleChemistry() {
        return GeneralConfig.enableLBSM() && GeneralConfig.LBSM_RECIPE_SIMPLE_CHEMISTRY.get();
    }

    /** CE: {@code GeneralConfig.enable528PressurizedRecipes}. */
    private static boolean pressurized() {
        return GeneralConfig.X528_ENABLE_PRESSURIZED_RECIPES.get();
    }

    /**
     * CE's {@code KEY_ANYGLASS} (any full glass block, vanilla or modded). NeoForge's real
     * common-tag convention equivalent is {@code c:glass_blocks} - hand-built via
     * {@link ItemTags#create}, the same "don't assume an unverified {@code Tags.Items} field name"
     * discipline {@code ModRecipeProvider}'s own {@code GLASS_PANES} constant already established for
     * the sibling {@code c:glass_panes} tag.
     */
    private static final TagKey<Item> ANY_GLASS_BLOCK =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "glass_blocks"));

    /**
     * Resolve-by-id lookup against the already-populated {@link BuiltInRegistries#ITEM}, matching
     * the pattern already proven safe at runtime by {@code PUREXRecipes#resolveItem}/
     * {@code FluidContainerRegistry#resolveItem} (this method only ever runs from {@link #register()},
     * itself only ever called from {@code CommonEvents.commonSetup}'s {@code enqueueWork} - well
     * after every item {@code RegisterEvent} has fired) - used for items registered without a named
     * static field (loop-registered battery packs, block-paired items).
     */
    private static Item resolveItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /** {@link #resolveItem(String)} OR-combined via {@link AnyOfStack} - see that class's javadoc. */
    private static AStack anyResolvedItem(int count, String... paths) {
        AStack[] options = new AStack[paths.length];
        for (int i = 0; i < paths.length; i++) options[i] = new ComparableStack(resolveItem(paths[i]), count);
        return new AnyOfStack(options);
    }

    /** OR-combined exact-item match via {@link AnyOfStack} - see that class's javadoc. */
    @SafeVarargs
    private static AStack anyItem(int count, java.util.function.Supplier<Item>... items) {
        AStack[] options = new AStack[items.length];
        for (int i = 0; i < items.length; i++) options[i] = new ComparableStack(items[i].get(), count);
        return new AnyOfStack(options);
    }

    /** OR-combined tag match via {@link AnyOfStack} - see that class's javadoc. */
    @SafeVarargs
    private static AStack anyTag(int count, TagKey<Item>... tags) {
        AStack[] options = new AStack[tags.length];
        for (int i = 0; i < tags.length; i++) options[i] = new OreDictStack(tags[i], count);
        return new AnyOfStack(options);
    }

    /**
     * OR-match combinator for CE's cross-material {@code DictGroup} wildcards (e.g.
     * {@code ANY_PLASTIC} = polymer OR bakelite ingot, {@code ANY_BISMOIDBRONZE} = bismuth-bronze OR
     * arsenic-bronze cast-plate) - this port's {@link AStack} hierarchy has no such union type
     * (confirmed absent by {@code ArcWelderRecipes}'s own javadoc: "CE's cross-material {@code ANY_X}
     * ore-dict wildcard mechanism - no port-side equivalent"). A small, self-contained local addition
     * (not touching the shared {@code RecipesCommon} class other tasks also depend on) that preserves
     * CE's real accepted-input set rather than silently narrowing it to one material.
     */
    private static final class AnyOfStack extends AStack {
        private final AStack[] options;

        AnyOfStack(AStack... options) {
            this.options = options;
            this.stacksize = options[0].count();
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
            AStack[] copies = new AStack[options.length];
            for (int i = 0; i < options.length; i++) copies[i] = options[i].copy();
            return new AnyOfStack(copies);
        }

        @Override
        public AStack copy(int stacksize) {
            AStack[] copies = new AStack[options.length];
            for (int i = 0; i < options.length; i++) copies[i] = options[i].copy(stacksize);
            return new AnyOfStack(copies);
        }

        @Override
        public ItemStack getStack() {
            return options[0].getStack();
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

    /**
     * Up to 3 {@link AStack} item inputs, up to 3 {@link FluidStack} fluid inputs, up to 3 item
     * outputs, up to 2 fluid outputs (widened from a single {@code outputFluid} field during the
     * {@code mrec-08-chemplant-misc} pass - CE's {@code chem.cccentrifuge} is the one entry that
     * needs 2 simultaneous output fluids), duration (ticks) + power (HE/tick), preserving CE's exact
     * recipe data.
     */
    public static final class ChemPlantRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final AStack[] inputItems;
        public final FluidStack[] inputFluids;
        public final ItemStack[] outputItems;
        public final FluidStack[] outputFluids;

        public ChemPlantRecipe(String name, int duration, long power, AStack[] inputItems,
                                FluidStack[] inputFluids, ItemStack[] outputItems, FluidStack... outputFluids) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.inputItems = inputItems;
            this.inputFluids = inputFluids;
            this.outputItems = outputItems;
            this.outputFluids = outputFluids == null ? new FluidStack[0] : outputFluids;
        }
    }
}
