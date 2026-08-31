package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.BilletPowderItems;
import com.hbm.items.PlateCrystalWasteItems;
import com.hbm.items.gear.GearItems;
import com.hbm.items.special.SpecialItems;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Recipe data for the Plasma Forge, ported from CE's {@code com.hbm.inventory.recipes.
 * PlasmaForgeRecipes} ({@code docs/phase7/mrec_15_plasmaforge_misc.md}, CE's real 265-line source
 * read in full upstream). CE's own shape ({@code extends GenericRecipes<PlasmaForgeRecipe>}, a
 * JSON-config-with-Java-defaults loader this port has no equivalent for - see
 * {@code com.hbm.inventory.recipes.loader.GenericRecipes}'s own javadoc) is translated into the same
 * "port now, JSON-override later" plain-static-table shape {@link com.hbm.inventory.recipes.chem.ChemPlantRecipes}/
 * {@link ArcWelderRecipes} already established: up to 12 order-independent {@link AStack} item
 * inputs (CE's {@code inputItemLimit()==12}), one optional {@link FluidStack} fluid input (CE's
 * {@code inputFluidLimit()==1}), one deterministic {@link ItemStack} output, duration (ticks) +
 * power (HE/tick) + a third field CE calls {@code ignitionTemp} (set via {@code setInputEnergy(...)},
 * printed as {@code TU/t} in CE's own recipe tooltip/NEI code) - preserved here as
 * {@link PlasmaForgeRecipe#heatDemand}, the Plasma Forge's own required plasma-heat throughput,
 * distinct from the {@code power} HE/tick field.
 * <p>
 * <b>Scope trim (documented, not silent, per the research report's item/registry dependency
 * check, independently re-verified line-by-line against both CE's real source and the live tree for
 * this pass): CE registers 35 entries; this class ports the 12 that are fully item-ready today - the
 * entire "weld" sub-family (CE ids {@code plsm.weldiron} through {@code plsm.weldosmiridium}, 11
 * entries) plus {@code plsm.schrabhammer}.</b>
 * Every one of the 11 weld-family materials ({@code IRON, STEEL, COPPER, TITANIUM, ZIRCONIUM,
 * ALUMINIUM, TCALLOY, CDALLOY, TUNGSTEN, CMB, OSMIRIDIUM}) already carries both
 * {@link MaterialShapes#CASTPLATE} (input shape) and {@link MaterialShapes#WELDEDPLATE} (output
 * shape) in its {@code setAutogen(...)} list in {@link Mats} (re-verified directly against the live
 * {@code Mats.java} for this pass, not merely trusted from the research report - the report's own
 * prose undercounted this family as "10 entries", but its table lists all 11 correctly and every one
 * of the 11 materials, OSMIRIDIUM included, is confirmed to have both shapes). The two fluids the
 * family needs ({@link Fluids#OXYGEN}, {@link Fluids#REFORMGAS}) are both confirmed present. This
 * exact same 11-recipe family is also separately ported (different machine, different CE balance
 * numbers - not a duplicate) by {@link ArcWelderRecipes} for the (not yet built) Arc Welder; CE
 * genuinely has both machines craft {@code plate_welded} from {@code plate_triple} at different
 * tiers, so both are kept.
 * <p>
 * <b>{@code plsm.schrabhammer} - a correction found while implementing, not flagged as ready by the
 * research report:</b> every one of its 11 item inputs/output is registered. CE's own
 * {@code OreDictManager} defines {@code SA326 = new DictFrame("Schrabidium")} - CE's alias for the
 * Schrabidium material, not a distinct isotope - and {@link Mats#MAT_SCHRABIDIUM} carries
 * {@code BLOCK} autogen, which {@code MaterialBlockGenerator} (a sibling generator to
 * {@code MaterialItemGenerator}, covering the {@code BLOCK}/{@code NUGGET}/{@code DUST}/etc. shapes
 * {@code MaterialItemGenerator} itself explicitly does not) registers as {@code schrabidium_block}
 * and {@code ModItemTagProvider} tags under {@link MaterialShapes#BLOCK}'s common tag exactly like
 * any other autogen shape - the research report's "ready" analysis stopped at
 * {@code MaterialItemGenerator}'s own 17-shape list and may have read {@code BLOCK} as ungenerated
 * entirely. {@code GearItems.SCHRABIDIUM_HAMMER}, {@code BilletPowderItems.BILLET_YHARONITE},
 * {@code SpecialItems.COIN_UFO} and {@code PlateCrystalWasteItems.FRAGMENT_METEORITE} are all
 * separately confirmed registered.
 * <p>
 * <b>{@code ass.fensusan} - checked and confirmed still blocked, for a different reason than the
 * report's per-family buckets suggest:</b> every one of its item inputs is actually registered
 * (including via an {@code ANY_BISMOIDBRONZE} OR-match, both {@code MAT_BBRONZE}/{@code MAT_ABRONZE}
 * having {@code CASTPLATE}) - but its <i>output</i>, CE's real {@code ModBlocks.machine_battery_redd},
 * is confirmed absent under any name anywhere in this port. Not ported for that reason alone.
 * <p>
 * <b>The remaining 22 entries are NOT ported, cited exactly (per the research report's dependency
 * check, independently spot-checked against the live tree for this pass):</b>
 * <ul>
 *     <li>{@code plsm.plateeuphemium}, and the {@code AT.dust()} leg of {@code plsm.hde}: need
 *     {@code Mats.MAT_EUPHEMIUM}/{@code MAT_ASTATINE}/{@code MAT_VOLCANIC} (CE's {@code EUPH}/
 *     {@code AT}/{@code VOLCANIC} materials) - none of the three exists as a {@code MAT_*} constant
 *     anywhere in this port's {@code Mats.java} (re-confirmed by direct grep for this pass).</li>
 *     <li>{@code plsm.hde}, {@code plsm.gerald}: need {@code part_generic[HDE]} ({@code EnumPartType}
 *     exists in {@code ItemEnums} but no backing item family is registered - confirmed absent).</li>
 *     <li>{@code plsm.icfcell/icfemitter/icfcapacitor/icfturbo/icfcasing/icfport/icfcontroller/
 *     icfscaffold/icfvessel/icfstructural/icfcore}: need {@code icf_laser_component} (6-variant
 *     metadata family), {@code icf_component} (metadata family), {@code struct_icf_core} - none
 *     exists under any name (confirmed absent by grep). {@code plsm.icfcontroller}'s output,
 *     CE's real {@code icf_controller}, is a genuinely different registry id from this port's
 *     already-built {@code machine_icf_controller} ({@code IcfControllerBlock}, a documented
 *     simplification of a related-but-distinct CE mechanic) - not the same item, confirmed by
 *     reading CE's own {@code ModBlocks.java} (real id {@code icf_controller}, no {@code machine_}
 *     prefix) side by side with this port's {@code FusionBlocks.java}.</li>
 *     <li>{@code plsm.icfpress}: needs {@code ModItems.motor}, confirmed not registered anywhere in
 *     this port (only a {@code SoundEvent} named {@code block.motor} exists) - note that unlike
 *     {@code icf_controller} above, this recipe's <i>output</i>, CE's real {@code machine_icf_press},
 *     genuinely <b>is</b> already built in this port under the identical id (see
 *     {@code IcfPressBlock}'s own javadoc: "Ported from CE's {@code MachineICFPress}"), so this
 *     recipe is blocked purely on the missing {@code motor} input, not on the output.</li>
 *     <li>{@code plsm.fusionvessel}: needs {@code fusion_torus}/{@code fusion_component} (metadata
 *     family) and {@code circuit[QUANTUM]} - both confirmed absent.</li>
 *     <li>{@code plsm.gerald}: needs {@code circuit}/{@code item_expensive} (backing items for both
 *     {@code EnumCircuitType}/{@code EnumExpensiveType} confirmed absent, enums themselves exist bare
 *     in {@code ItemEnums}), {@code det_nuke} as an item ingredient, and {@code part_generic[HDE]}
 *     again.</li>
 *     <li>{@code plsm.dfccore/dfcemitter/dfcreceiver/dfcinjector/dfcstabilizer}: need
 *     {@code circuit[CONTROLLER_QUANTUM]}/{@code circuit[CONTROLLER_ADVANCED]} (same backing-item
 *     gap) and CE's real {@code dfc_core}/{@code dfc_emitter}/{@code dfc_receiver}/
 *     {@code dfc_injector}/{@code dfc_stabilizer} blocks, none of which are registered under any name
 *     in this port yet (confirmed absent by grep - only {@code DFCRecipes.java}'s own javadoc
 *     mentions these ids, as a forward-looking TODO, not a real registration).</li>
 * </ul>
 * <p>
 * <b>Also dropped, matching this port's established precedent for CE progression/UX layers riding on
 * top of recipe data (see {@code ChemPlantRecipes}'s {@code .setPools(...)} treatment):</b> CE's
 * "expensive mode" alternate-ingredient leg ({@code inputItemsEx}, gated by
 * {@code GeneralConfig.enableExpensiveMode}, present on 24 of the 35 entries - none of which survive
 * into this class's 12 anyway, {@code plsm.schrabhammer} included, which never had one in CE) and the
 * blueprint-pool/auto-switch-group metadata
 * ({@code setPools}/{@code setPools528}/{@code setGroup}) are not carried over - no pool-gated
 * blueprint-roll or GUI material-dropdown system exists in this port for them to feed.
 * <p>
 * <b>No machine to attach this recipe data to yet</b> (confirmed by the research report: no
 * {@code fusion_plasma_forge} block, block entity, menu or screen exists anywhere in this port).
 * This class is recipe data only, following this port's own precedent
 * ({@code CentrifugeRecipes}/{@code ChemPlantRecipes}/{@code ArcWelderRecipes} were all ported ahead
 * of or alongside their block entities) - ready for whichever future pass builds the Plasma Forge
 * block entity to consume via {@link #getRecipe}.
 */
public final class PlasmaForgeRecipes {

    public static final List<PlasmaForgeRecipe> RECIPES = new ArrayList<>();

    private static boolean registered = false;

    private PlasmaForgeRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;

        String autoPlate = "autoswitch.weldPlates";

        // ---- Welded plates (CE PlasmaForgeRecipes.java plsm.weldiron..plsm.weldosmiridium,
        // lines 59-96): every entry's ignitionTemp (500,000 TU/t) is identical, duration/power/fluid
        // vary per material - exact CE values, group tag kept only as a documentation note (no
        // GUI dropdown exists yet to consume it, see class javadoc). ----

        RECIPES.add(new PlasmaForgeRecipe("plsm.weldiron", 50, 100L, 500_000L,
                weldedPlateOutput(Mats.MAT_IRON), null, autoPlate,
                shapeInput(Mats.MAT_IRON, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldsteel", 50, 500L, 500_000L,
                weldedPlateOutput(Mats.MAT_STEEL), null, autoPlate,
                shapeInput(Mats.MAT_STEEL, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldcopper", 50, 1_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_COPPER), null, autoPlate,
                shapeInput(Mats.MAT_COPPER, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldtitanium", 300, 50_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_TITANIUM), null, autoPlate,
                shapeInput(Mats.MAT_TITANIUM, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldzirconium", 300, 10_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_ZIRCONIUM), null, autoPlate,
                shapeInput(Mats.MAT_ZIRCONIUM, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldaluminium", 150, 10_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_ALUMINIUM), null, autoPlate,
                shapeInput(Mats.MAT_ALUMINIUM, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldtcalloy", 600, 1_000_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_TCALLOY), new FluidStack(Fluids.OXYGEN, 1_000), autoPlate,
                shapeInput(Mats.MAT_TCALLOY, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldcdalloy", 600, 1_000_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_CDALLOY), new FluidStack(Fluids.OXYGEN, 1_000), autoPlate,
                shapeInput(Mats.MAT_CDALLOY, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldtungsten", 600, 250_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_TUNGSTEN), new FluidStack(Fluids.OXYGEN, 1_000), autoPlate,
                shapeInput(Mats.MAT_TUNGSTEN, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldcmb", 600, 10_000_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_CMB), new FluidStack(Fluids.REFORMGAS, 1_000), autoPlate,
                shapeInput(Mats.MAT_CMB, MaterialShapes.CASTPLATE, 2)));
        RECIPES.add(new PlasmaForgeRecipe("plsm.weldosmiridium", 3_000, 50_000_000L, 500_000L,
                weldedPlateOutput(Mats.MAT_OSMIRIDIUM), new FluidStack(Fluids.REFORMGAS, 16_000), autoPlate,
                shapeInput(Mats.MAT_OSMIRIDIUM, MaterialShapes.CASTPLATE, 2)));

        // ---- plsm.schrabhammer (CE PlasmaForgeRecipes.java lines 161-174) - exact CE values, no
        // fluid, no auto-switch group. SA326 == Schrabidium (see class javadoc). 11 item inputs,
        // within CE's own inputItemLimit()==12. ----
        RECIPES.add(new PlasmaForgeRecipe("plsm.schrabhammer", 6_000, 10_000_000L, 25_000_000L,
                new ItemStack(GearItems.SCHRABIDIUM_HAMMER.get()), null, null,
                shapeInput(Mats.MAT_SCHRABIDIUM, MaterialShapes.BLOCK, 35),
                new ComparableStack(BilletPowderItems.BILLET_YHARONITE.get(), 64),
                new ComparableStack(BilletPowderItems.BILLET_YHARONITE.get(), 64),
                new ComparableStack(SpecialItems.COIN_UFO.get(), 1),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64),
                new ComparableStack(PlateCrystalWasteItems.FRAGMENT_METEORITE.get(), 64)));

        // Deliberately not ported (see class javadoc): plateeuphemium, platednt (needs EUPH/AT/VOLCANIC
        // materials, and DNT/DESH both lack an INGOT autogen shape in this port regardless), hde, all
        // 11 ICF laser-fusion entries, fusionvessel, fensusan (every input is ready, but its output
        // machine_battery_redd is not registered anywhere in this port), gerald, and all 5 DFC entries.
    }

    /** {@code X.plateCast()} - CE's own {@link OreDictStack} ingredient shape. */
    private static AStack shapeInput(NTMMaterial mat, MaterialShapes shape, int count) {
        return new OreDictStack(shape.commonTag(mat), count);
    }

    private static ItemStack weldedPlateOutput(NTMMaterial mat) {
        return new ItemStack(shapeItem(mat, MaterialShapes.WELDEDPLATE));
    }

    /**
     * Resolve-by-id lookup against the already-populated {@link BuiltInRegistries#ITEM}, matching
     * the pattern already proven safe at runtime by {@code ArcWelderRecipes}/{@code ChemPlantRecipes}
     * (this method only ever runs from {@link #register()}, itself only ever called well after every
     * item {@code RegisterEvent} has fired). Throws loudly rather than silently building an
     * unmatchable recipe if a regression ever removes the backing autogen item.
     */
    private static Item shapeItem(NTMMaterial mat, MaterialShapes shape) {
        String id = shape.buildRegistryName(mat);
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id))
                .orElseThrow(() -> new IllegalStateException(
                        "PlasmaForgeRecipes: item hbm:" + id + " is not registered - check com.hbm.items.MaterialItemGenerator"));
    }

    /**
     * Order-independent, greedy per-input match against a mutable copy of the recipe's ingredient
     * list (each input slot consumed at most once, every ingredient must be spent), matching CE's
     * own {@code GenericRecipes.getRecipeFor}/{@code ArcWelderRecipes.getRecipe} shape. Fluid input
     * (if any) is checked separately by the caller against its own tank, the same split this port's
     * other multi-input machine recipe classes (e.g. {@code ArcWelderRecipes}) already use.
     */
    public static PlasmaForgeRecipe getRecipe(ItemStack... inputs) {
        register();

        outer:
        for (PlasmaForgeRecipe recipe : RECIPES) {
            List<AStack> recipeList = new ArrayList<>(List.of(recipe.inputItems));

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
     * Up to 12 order-independent {@link AStack} item inputs (CE's {@code inputItemLimit()==12}), one
     * optional {@link FluidStack} fluid input (CE's {@code inputFluidLimit()==1}), one deterministic
     * {@link ItemStack} output, duration (ticks) + power (HE/tick) + {@link #heatDemand} (TU/tick,
     * CE's {@code ignitionTemp}/{@code setInputEnergy}), preserving CE's exact recipe data.
     * {@link #autoSwitchGroup} is kept only as a documentation note (see class javadoc - no GUI
     * dropdown mechanic consumes it yet).
     */
    public static final class PlasmaForgeRecipe {
        public final String name;
        public final int duration;
        public final long power;
        public final long heatDemand;
        public final ItemStack output;
        public final FluidStack inputFluid;
        public final String autoSwitchGroup;
        public final AStack[] inputItems;

        public PlasmaForgeRecipe(String name, int duration, long power, long heatDemand, ItemStack output,
                                  FluidStack inputFluid, String autoSwitchGroup, AStack... inputItems) {
            this.name = name;
            this.duration = duration;
            this.power = power;
            this.heatDemand = heatDemand;
            this.output = output;
            this.inputFluid = inputFluid;
            this.autoSwitchGroup = autoSwitchGroup;
            this.inputItems = inputItems;
        }
    }
}
