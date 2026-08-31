package com.hbm.inventory.recipes.machine;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.IngotNuggetItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.bomb.NukeCasingItems;
import com.hbm.items.machine.ItemPileRodMK2.EnumPileRod;
import com.hbm.items.machine.ItemWatzPellet.EnumWatzType;
import com.hbm.items.machine.MachineItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Recipe data for the PUREX (Plutonium/URanium EXtraction) reprocessing machine, ported from CE's
 * {@code com.hbm.inventory.recipes.PUREXRecipes} (518 ln, read in full - see
 * {@code docs/phase7/mrec_05_purex_misc.md}). CE's real shape is a {@code GenericRecipes<PUREXRecipe>}
 * singleton (3 item inputs, 3 fluid inputs, 6 item outputs, 1 fluid output, duration/power, an
 * "auto switch" group string, a name-wrapper localization key) backed by a JSON-override file - but
 * this port has no shared multi-io/chance-output {@code Recipe<?>} infrastructure yet (that research
 * doc's own recommendation names {@code HbmMachineRecipe}, which {@code docs/phase2/
 * items_tool_machine_coupling_and_recipe_system.md} designs but nothing has built), so this class
 * follows the same "port now, JSON-override later" plain-static-table shape {@code RefineryRecipes}/
 * {@code com.hbm.inventory.recipes.chem.ChemPlantRecipes} already established for exactly this
 * situation, rather than inventing a second competing recipe-loader framework.
 *
 * <p><b>No {@code MachinePUREX} block/block-entity exists in this port yet either</b> (confirmed
 * absent by the research pass) - this class is pure recipe data for whichever future task builds
 * that machine to consume (via {@link #getAll()}, mirroring {@code RefineryRecipes#getAllRefinery()}'s
 * defensive-lazy-registration accessor pattern, so no eager bootstrap call needs wiring into any
 * shared aggregator file today).</p>
 *
 * <p><b>Scope: only CE's fully item-ready entries are ported</b> (per this task's ground rules - do
 * not stub missing items). Of CE's 58 real recipes, exactly <b>18</b> have every input/output item
 * already registered in this port: {@code purex.uzh}, {@code purex.flashlead}, the 3 CP-1 pile
 * recipes ({@code purex.pilepu}/{@code pilethorium}/{@code pilepu239}), the 10 unconditional Watz
 * recipes ({@code purex.watzschrab}/{@code watzhes}/{@code watzmes}/{@code watzles}/{@code watzhen}/
 * {@code watzmeu}/{@code watzmep}/{@code watzlead}/{@code watzboron}/{@code watzdu}),
 * {@code purex.schraranium}, and <b>2 recipes this task's own re-check found item-ready that the
 * research report's blanket "nuclear_waste_tiny blocks all 7 Plate Fuel recipes" claim missed</b> -
 * {@code purex.platepu238be} and {@code purex.platera226be} (see the discrepancy note below). The
 * other 40 are <b>not</b> ported here - each is blocked on one or more of these missing port-side
 * items (grepped absent, corroborated by 3 independently-committed files' own javadocs - see the
 * research report's "Item/registry dependency check"):
 * <ul>
 *   <li><b>{@code pwr_fuel_depleted} (all 15 {@code EnumPWRFuel} grades)</b> - this port only
 *   registers the fresh {@code pwr_fuel_<name>} grade ({@link MachineItems#PWR_FUEL}), not CE's
 *   separate depleted/spent marker. Blocks all 17 PWR-family recipes ({@code purex.pwrmeu} through
 *   {@code purex.pwrbfpu241}) plus {@code purex.schrabpwr}/{@code purex.schrabmen}.</li>
 *   <li><b>{@code nuclear_waste_tiny}</b> - blocks all 9 ZIRNOX recipes, 5 of the 7 Plate Fuel recipes
 *   ({@code purex.platemox}/{@code platepu239}/{@code platesa326}/{@code plateu233}/{@code plateu235} -
 *   <em>not</em> {@code platepu238be}/{@code platera226be}, whose output lists never included it - see
 *   discrepancy note below), {@code purex.thoriumsalt}, and {@code purex.schrabzirnox} (16 recipes).</li>
 *   <li><b>{@code nuclear_waste_vitrified}</b> - blocks all 3 Vitrification recipes
 *   ({@code purex.vitliquid}/{@code vitgaseous}/{@code vitsolid}).</li>
 *   <li><b>{@code sand_lead} (block)</b> - CE's {@code BlockFallingBase(Material.SAND, "sand_lead", ...)},
 *   distinct from the already-ported {@code ore_oil_sand}. Also blocks the 3 Vitrification recipes.</li>
 *   <li><b>{@code pellet_charged}</b> - blocks {@code purex.flashgold} and {@code purex.icf}.</li>
 *   <li><b>{@code icf_pellet_depleted}</b> - a "depleted" state distinct from this port's already-ported
 *   {@code icf_pellet}/{@code icf_pellet_empty} (no such state exists on {@code ItemICFPellet} today).
 *   Blocks {@code purex.icf} (also blocked by {@code pellet_charged} above).</li>
 *   <li><b>A port-side ore-dict-alias equivalent for "nuggetNaquadria"</b> - this port has zero
 *   Naquadria material anywhere, and the ore-dict alias-matching mechanism CE's guard depends on was
 *   deliberately dropped (see {@code Mats.java}'s own javadoc). {@code purex.watznaqadah}/
 *   {@code purex.watznaqadria} are themselves conditional even in CE (only fire if a
 *   Naquadria-integration mod is loaded), so this is treated as N/A rather than a real blocker.</li>
 * </ul>
 * {@code purex.flashgold} is additionally excluded even though its other ingredients are all present,
 * since it needs {@code pellet_charged} too.
 *
 * <p><b>Discrepancy from {@code docs/phase7/mrec_05_purex_misc.md}</b>: that report's "Ready-to-port
 * tally for PUREXRecipes" section states 16/58 and groups all 7 Plate Fuel recipes under the
 * {@code nuclear_waste_tiny} blocker. Re-reading CE's actual output lists for {@code purex.platepu238be}
 * (outputs {@code nugget_beryllium}/{@code nugget_pu238}/{@code powder_coal_tiny}/{@code nugget_lead})
 * and {@code purex.platera226be} (outputs {@code nugget_beryllium}/{@code nugget_polonium}/
 * {@code powder_coal_tiny}/{@code nugget_lead}) against CE source directly (not just the report's own
 * summary table) shows neither one's output list ever included {@code nuclear_waste_tiny} - and both
 * their input items ({@code waste_plate_pu238be}/{@code waste_plate_ra226be}) and every output item are
 * independently confirmed already registered in this port. Ported here as 2 additional ready recipes
 * per this task's own ground rules ("use your own judgment and note the discrepancy").</p>
 *
 * <p><b>Ingredient model note</b>: CE keys 2 of these 18 recipes' item inputs via
 * {@code OreDictStack}/{@code OreDictManager} isotope frames ({@code ZR.billet()} for
 * {@code purex.uzh}, {@code PB209.billet()} for {@code purex.flashlead}). This port has no tag
 * generation for custom material shapes yet (no {@code c:billets/*} tags exist in
 * {@code src/main/resources/data}), so those become plain {@link ComparableStack} exact-item matches
 * against this port's single already-registered billet item instead of a tag - a documented,
 * lower-risk substitution (same single item CE's tag would have resolved to on a vanilla+HBM-only
 * install anyway), not a silent behavior change.</p>
 *
 * <p><b>{@code pile_rod_plutonium}/{@code pile_rod_pu239}</b> have no dedicated static field anywhere
 * in this port ({@link MachineItems#registerPileRods()} registers them in a bare string loop with the
 * {@code DeferredItem} return discarded) - resolved here via {@link #resolveItem(String)}, the same
 * lazy {@code BuiltInRegistries.ITEM.get(ResourceLocation)}-by-path idiom already used elsewhere in
 * this port (e.g. {@code FluidContainerRegistry#resolveItem}) rather than adding a new public field to
 * a shared items file this task does not own.</p>
 */
public final class PUREXRecipes {

    public static final List<PUREXRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private PUREXRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        long pilePower = 100;
        long platePower = 1_500;
        long watzPower = 10_000;
        String autoPile = "autoswitch.pile";
        String autoPlate = "autoswitch.plate";
        String autoWatz = "autoswitch.watz";
        String recycle = "purex.recycle";

        RECIPES.add(new PUREXRecipe("purex.uzh", 600, 1_000, null, null,
                new AStack[]{
                        new ComparableStack(BilletPowderItems.BILLET_URANIUM_FUEL.get()),
                        new ComparableStack(BilletPowderItems.BILLET_ZIRCONIUM.get(), 3)
                },
                new FluidStack[]{new FluidStack(Fluids.NITRIC_ACID, 1000), new FluidStack(Fluids.HYDROGEN, 4000)},
                new ItemStack[]{new ItemStack(BilletPowderItems.BILLET_UZH.get(), 4)},
                null));

        RECIPES.add(new PUREXRecipe("purex.flashlead", 600, 1_000, null, null,
                new AStack[]{
                        new ComparableStack(BilletPowderItems.BILLET_PB209.get()),
                        new ComparableStack(BilletPowderItems.BILLET_BALEFIRE_GOLD.get())
                },
                new FluidStack[]{new FluidStack(Fluids.AMAT, 1_000)},
                new ItemStack[]{new ItemStack(BilletPowderItems.BILLET_FLASHLEAD.get(), 1)},
                null));

        // CP-1 pile
        RECIPES.add(new PUREXRecipe("purex.pilepu", 40, pilePower, recycle, autoPile,
                new AStack[]{new ComparableStack(resolveItem("pile_rod_plutonium"))},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 100)},
                new ItemStack[]{
                        new ItemStack(BilletPowderItems.BILLET_PU_MIX.get(), 2),
                        new ItemStack(BilletPowderItems.BILLET_URANIUM.get(), 1),
                        new ItemStack(PlateCrystalWasteItems.PLATE_IRON.get(), 2)
                },
                null));

        RECIPES.add(new PUREXRecipe("purex.pilethorium", 40, pilePower, recycle, autoPile,
                new AStack[]{new ComparableStack(MachineItems.PILE_RODS_MK2.get(EnumPileRod.THORIUM_FUEL).get())},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 100)},
                new ItemStack[]{
                        new ItemStack(BilletPowderItems.BILLET_THORIUM_FUEL.get(), 2),
                        new ItemStack(BilletPowderItems.BILLET_NUCLEAR_WASTE.get(), 1)
                },
                null));

        RECIPES.add(new PUREXRecipe("purex.pilepu239", 40, pilePower, recycle, autoPile,
                new AStack[]{new ComparableStack(resolveItem("pile_rod_pu239"))},
                new FluidStack[]{new FluidStack(Fluids.SULFURIC_ACID, 100)},
                new ItemStack[]{
                        new ItemStack(BilletPowderItems.BILLET_PU239.get(), 1),
                        new ItemStack(BilletPowderItems.BILLET_PU_MIX.get(), 1),
                        new ItemStack(BilletPowderItems.BILLET_URANIUM.get(), 1),
                        new ItemStack(PlateCrystalWasteItems.PLATE_IRON.get(), 2)
                },
                null));

        // Plate Fuel (2 of 7 - the other 5 need nuclear_waste_tiny, see class javadoc discrepancy note)
        RECIPES.add(new PUREXRecipe("purex.platepu238be", 100, platePower, recycle, autoPlate,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.WASTE_PLATE_PU238BE.get())},
                new FluidStack[]{new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250)},
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_BERYLLIUM.get(), 1),
                        new ItemStack(IngotNuggetItems.NUGGET_PU238.get(), 1),
                        new ItemStack(BilletPowderItems.POWDER_COAL_TINY.get(), 2),
                        new ItemStack(IngotNuggetItems.NUGGET_LEAD.get(), 2)
                },
                null));

        RECIPES.add(new PUREXRecipe("purex.platera226be", 100, platePower, recycle, autoPlate,
                new AStack[]{new ComparableStack(PlateCrystalWasteItems.WASTE_PLATE_RA226BE.get())},
                new FluidStack[]{new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250)},
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_BERYLLIUM.get(), 2),
                        new ItemStack(IngotNuggetItems.NUGGET_POLONIUM.get(), 2),
                        new ItemStack(BilletPowderItems.POWDER_COAL_TINY.get(), 1),
                        new ItemStack(IngotNuggetItems.NUGGET_LEAD.get(), 1)
                },
                null));

        // Watz (10 of 12 - watznaqadah/watznaqadria excluded, see class javadoc)
        RECIPES.add(watz("purex.watzschrab", EnumWatzType.SCHRABIDIUM, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 15),
                        new ItemStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get(), 3),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzhes", EnumWatzType.HES, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 17),
                        new ItemStack(IngotNuggetItems.NUGGET_EUPHEMIUM.get(), 1),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzmes", EnumWatzType.MES, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 12),
                        new ItemStack(IngotNuggetItems.NUGGET_TANTALIUM.get(), 6),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzles", EnumWatzType.LES, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_SOLINIUM.get(), 9),
                        new ItemStack(IngotNuggetItems.NUGGET_TANTALIUM.get(), 9),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzhen", EnumWatzType.HEN, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_PU239.get(), 12),
                        new ItemStack(IngotNuggetItems.NUGGET_TECHNETIUM.get(), 6),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzmeu", EnumWatzType.MEU, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_PU239.get(), 12),
                        new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get(), 6),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzmep", EnumWatzType.MEP, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_PU241.get(), 12),
                        new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get(), 6),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzlead", EnumWatzType.LEAD, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_LEAD.get(), 6),
                        new ItemStack(IngotNuggetItems.NUGGET_BISMUTH.get(), 12),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzboron", EnumWatzType.BORON, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(BilletPowderItems.POWDER_COAL_TINY.get(), 12),
                        new ItemStack(IngotNuggetItems.NUGGET_CO60.get(), 6),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        RECIPES.add(watz("purex.watzdu", EnumWatzType.DU, recycle, autoWatz, watzPower,
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_POLONIUM.get(), 12),
                        new ItemStack(IngotNuggetItems.NUGGET_PU238.get(), 6),
                        new ItemStack(NukeCasingItems.NUCLEAR_WASTE.get(), 2)
                }));

        // Schrabidium
        RECIPES.add(new PUREXRecipe("purex.schraranium", 200, 1_000, "purex.schrab", null,
                new AStack[]{new ComparableStack(IngotNuggetItems.INGOT_SCHRARANIUM.get())},
                new FluidStack[]{new FluidStack(Fluids.KEROSENE, 2_000), new FluidStack(Fluids.NITRIC_ACID, 1_000)},
                new ItemStack[]{
                        new ItemStack(IngotNuggetItems.NUGGET_SCHRABIDIUM.get(), 3),
                        new ItemStack(IngotNuggetItems.NUGGET_URANIUM.get(), 3),
                        new ItemStack(IngotNuggetItems.NUGGET_NEPTUNIUM.get(), 2)
                },
                null));
    }

    /**
     * Shared shape for the 10 unconditional Watz recipes: all use the same K500+N250 input-fluid
     * pair, the same {@code "purex.recycle"} name-wrapper / {@code "autoswitch.watz"} group, output
     * {@code WATZ} fluid 1000mB, and a {@code watz_pellet_depleted[type]} single item input - only the
     * item outputs differ (CE's own {@code registerDefaults()} repeats this shape 12 times verbatim;
     * see the class javadoc for why 2 of the 12 are excluded here).
     */
    private static PUREXRecipe watz(String name, EnumWatzType type, String nameWrapper, String group,
                                     long power, ItemStack[] outputs) {
        return new PUREXRecipe(name, 60, power, nameWrapper, group,
                new AStack[]{new ComparableStack(MachineItems.WATZ_PELLET_DEPLETED.get(type).get())},
                new FluidStack[]{new FluidStack(Fluids.KEROSENE, 500), new FluidStack(Fluids.NITRIC_ACID, 250)},
                outputs,
                new FluidStack(Fluids.WATZ, 1_000));
    }

    /** Same lazy-lookup-by-path idiom as {@code FluidContainerRegistry#resolveItem}. */
    private static Item resolveItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path));
    }

    /**
     * Full-collection accessor, defensively calling {@link #register()} first (idempotent) so any
     * future consumer (a {@code MachinePUREX} block entity, a JEI category) can call this safely
     * without needing a separate eager-bootstrap call wired into a shared aggregator file - the same
     * pattern {@code RefineryRecipes#getAllRefinery()} already established.
     */
    public static List<PUREXRecipe> getAll() {
        register();
        return Collections.unmodifiableList(RECIPES);
    }

    /**
     * Up to 3 {@link AStack} item inputs, up to 3 {@link FluidStack} fluid inputs, up to 6 item
     * outputs, 1 optional fluid output, duration (ticks) + power (HE/tick), plus CE's optional
     * name-wrapper localization key and auto-switch group string (both nullable - only used by a
     * subset of CE's recipes), preserving CE's exact recipe data and metadata shape.
     */
    public static final class PUREXRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final String nameWrapper;
        public final String group;
        public final AStack[] inputItems;
        public final FluidStack[] inputFluids;
        public final ItemStack[] outputItems;
        public final FluidStack outputFluid;

        public PUREXRecipe(String name, int duration, long power, String nameWrapper, String group,
                            AStack[] inputItems, FluidStack[] inputFluids, ItemStack[] outputItems, FluidStack outputFluid) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.nameWrapper = nameWrapper;
            this.group = group;
            this.inputItems = inputItems;
            this.inputFluids = inputFluids;
            this.outputItems = outputItems;
            this.outputFluid = outputFluid;
        }
    }
}
