package com.hbm.datagen;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

/**
 * Ports the highest-value slice of CE's vanilla-crafting-table recipe corpus
 * ({@code upstream/hbm-ce/src/main/java/com/hbm/main/CraftingManager.java} (1,602 lines) +
 * {@code upstream/hbm-ce/src/main/java/com/hbm/crafting/*.java} (9 files, 2,085 lines)), whose scale
 * (~1,900-2,000+ individual recipe registrations) and complete absence from this port (before this
 * class, exactly 1 vanilla crafting recipe existed anywhere: {@code data/hbm/recipe/mech_key.json})
 * was surveyed and confirmed by {@code docs/phase5/advancement_and_recipe_datagen_assets.md} Part 2.
 * That report explicitly scoped a first implement pass to CE's {@code ToolRecipes}, {@code ArmorRecipes}
 * and {@code MineralRecipes} (its own table gives the exact per-class counts) - this class covers
 * exactly that slice, plus nothing else, and documents below precisely which entries within those
 * three CE classes could and could not be ported, matching that report's own honest-scoping
 * discipline rather than silently truncating.
 *
 * <h2>Why so much of CE's ~1,900-recipe corpus is still out of reach</h2>
 * CE's recipes are Java calls against CE's own {@code ModItems} fields, so every recipe this class
 * ports first needs BOTH sides (every ingredient and every result) to already be a real registered
 * item in this port. Two structural facts made that a much smaller intersection than expected, found
 * only by direct registry-name auditing while writing this class (not assumed):
 * <ol>
 *     <li>This port kept CE's exact legacy resource-item ids ({@code billet_x}, {@code powder_x},
 *     {@code ingot_x}, {@code nugget_x}, {@code plate_x} - see {@code com.hbm.items.BilletPowderItems},
 *     {@code com.hbm.items.IngotNuggetItems}, {@code com.hbm.items.PlateCrystalWasteItems}) almost
 *     completely intact, one field for one field - this is what makes {@link #mineralRecipes} as
 *     large as it is. But CE's <em>material-tiered mining tool and armor</em> items were ported far
 *     more selectively (Phase 1/3 content waves): this port has mining tools for steel/titanium/
 *     cobalt/desh but not CMB (no {@code ingot_cmb}/{@code ingot_cmbsteel} item exists at all), and
 *     has swords for every material CE has one for (via {@code com.hbm.items.weapon.WeaponMeleeItems})
 *     but genuinely has <b>no armor items at all</b> for CE's basic steel/titanium/cmb/cobalt/
 *     starmetal/robes/security/dnt/zirconium tiers (confirmed by repo-wide grep for
 *     {@code "steel_helmet"}/{@code "titanium_plate"}/etc. - zero hits anywhere in
 *     {@code src/main/java}) - only the special/powered-armor sets from Phase 3
 *     ({@code com.hbm.items.armor.SpecialArmorItems}/{@code PoweredArmorItems}) exist. This is a real
 *     Phase 0-4 items-registration gap, not something this recipe-datagen task can or should fix
 *     (out of this task's file scope per the wave's ground rules) - it is the single largest reason
 *     {@link #armorRecipes} is so much smaller than {@link #toolRecipes}/{@link #mineralRecipes}.</li>
 *     <li>CE's electronics family ({@code EnumCircuitType.BASIC/ANALOG/ADVANCED/CHIP/QUANTUM} circuit
 *     items) and several other pervasively-referenced Phase-2-adjacent items ({@code motor},
 *     {@code motor_desh}, {@code tank_steel}, {@code thruster_small}, {@code cladding_lead},
 *     {@code machine_diesel} as a craftable item, {@code watch}) do not exist in this port at all yet
 *     (confirmed by repo-wide grep, zero hits for any of them). Every CE recipe that consumes a
 *     circuit component or one of those items as an ingredient is unreachable until whichever future
 *     phase ports that family - this blocks the majority of CE's powered-armor recipes (t51, ajr,
 *     hev, bj, rpa, jetpacks, most of dieselsuit/envsuit) even though the armor pieces themselves are
 *     already real, registered items with no recipe to reach them.</li>
 * </ol>
 * Every ingredient/result id referenced below was individually confirmed present in this port's own
 * item-registration source (not CE's) before being used, via {@link #item(String)} - a resolve-by-id
 * lookup against {@link BuiltInRegistries#ITEM} (the same pattern already proven safe in this
 * codebase by {@code com.hbm.items.datagen.ModItemTagProvider}, since {@code GatherDataEvent} fires
 * only after every {@code RegisterEvent} has already populated the registries) rather than importing
 * a dozen item-registration classes and trusting every constant name was guessed correctly. Any id
 * that turns out to still be wrong throws a clear {@link IllegalStateException} naming the missing id
 * at {@code runData} time instead of silently emitting a broken recipe JSON.
 *
 * <h2>Explicitly not attempted (per the research report and this task's own scope)</h2>
 * <ul>
 *     <li>CE's {@code RodRecipes} (RBMK/breeding/ZIRNOX fuel-rod crafting) - a different mechanism,
 *     already served by this port's own RBMK-internal recipe system
 *     ({@code com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes}, Phase 2 scope).</li>
 *     <li>The 7 {@code com.hbm.crafting.handlers.*} CE classes (dynamic NBT/predicate-matching
 *     recipes: cargo shells, container upgrades, fluid duct retyping, grenades, MKU ammo, RBMK fuel
 *     assembly, scrap sorting) - these need genuine new {@code RecipeType}/{@code RecipeSerializer}/
 *     {@code Recipe<CraftingInput>} Java classes, not plain shaped/shapeless JSON, and are explicitly
 *     scoped by the research report as a separate, harder follow-up.</li>
 *     <li>CE's {@code CraftingManager.addCrafting()} (the ~1,200-line dispatcher method's own inline
 *     recipes, distinct from the 8 named sub-registrar classes) and CE's {@code WeaponRecipes}/
 *     {@code ConsumableRecipes}/{@code PowderRecipes}/{@code ExclusiveRecipes} - not reached in this
 *     pass; every ingredient/result item they reference (circuits, {@code hazmat_cloth}, ammo
 *     components, etc.) would need the same per-recipe registry-name audit this class's three covered
 *     areas already received, and this task's time budget did not extend to it. A future pass should
 *     repeat this class's {@link #item(String)}-first verification discipline for those four classes
 *     rather than assume CE's field names carried over unchanged.</li>
 *     <li>CE's block&harr;ingot 3x3 compression grid (the back half of {@code MineralRecipes.java},
 *     CE lines ~228-395: {@code Item.getItemFromBlock(ModBlocks.block_x)} both directions) - this
 *     port's material storage-block items ({@code block_steel}, {@code block_titanium}, etc.) were
 *     not found under any name this class could confirm exists (repeated grep, zero hits); rather
 *     than guess a naming convention for another area's block registrations, this whole leg is
 *     skipped. {@link #mineralRecipes} covers only the addBillet/addMineralSet/add1To9Pair families,
 *     which never touch a block.</li>
 *     <li>CE's ore-dictionary compatibility legs (every {@code String... ore} vararg on
 *     {@code addBillet}, e.g. {@code SR90.all(MaterialShapes.NUGGET)}) - Forge's ore dictionary does
 *     not exist in NeoForge 1.21.1; these varargs only ever added cross-mod-compatibility alternate
 *     recipes in CE, never anything this port's own items need, so they are dropped without
 *     replacement.</li>
 * </ul>
 */
public class ModRecipeProvider extends RecipeProvider {

    /**
     * CE's {@code KEY_ANYPANE} (an {@code OreDictManager} wrapper matching any glass pane, vanilla or
     * modded). NeoForge's real common-tag convention equivalent is {@code c:glass_panes} - built by
     * hand via {@link ItemTags#create} (same pattern {@code ModItemTagProvider} already uses for
     * tags this port could not verify as a named constant against a real jar in this sandbox) rather
     * than assumed as a {@code net.neoforged.neoforge.common.Tags.Items} field, since that class's
     * exact field name was not independently confirmed here.
     */
    private static final TagKey<Item> GLASS_PANES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "glass_panes"));

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        toolRecipes(output);
        mineralRecipes(output);
        armorRecipes(output);
    }

    // ================================================================================================
    // Part 1: ToolRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/ToolRecipes.java
    // ================================================================================================

    /**
     * {name, ingot registry id}. CE's "Regular tools" bucket (ToolRecipes.java:32-56) covers steel,
     * titanium, cobalt, cmb and desh; CMB is dropped here - no {@code ingot_cmb}/{@code ingot_cmbsteel}
     * item exists anywhere in this port (confirmed by grep), so {@code cmb_sword/_pickaxe/_axe/
     * _shovel/_hoe} (all 5 already real registered items) have no ingredient to craft them from and
     * are left unreachable by this pass.
     */
    private static final String[][] TOOL_MATERIALS = {
            {"steel", "ingot_steel"},
            {"titanium", "ingot_titanium"},
            {"cobalt", "ingot_cobalt"},
            {"desh", "ingot_desh"},
    };

    private void toolRecipes(RecipeOutput output) {
        // ToolRecipes.java:32-56 (addSword/addPickaxe/addAxe/addShovel/addHoe over 5 ingots -> 4 here).
        for (String[] mat : TOOL_MATERIALS) {
            String name = mat[0];
            Item ingot = item(mat[1]);
            sword(output, ingot, item(name + "_sword"), "tool/" + name + "_sword");
            pickaxe(output, ingot, item(name + "_pickaxe"), "tool/" + name + "_pickaxe");
            axe(output, ingot, item(name + "_axe"), "tool/" + name + "_axe");
            shovel(output, ingot, item(name + "_shovel"), "tool/" + name + "_shovel");
            hoe(output, ingot, item(name + "_hoe"), "tool/" + name + "_hoe");
        }

        // ToolRecipes.java:65: dwarven_pickaxe, "CIC"," S "," S ".
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("dwarven_pickaxe"))
                .pattern("CIC")
                .pattern(" S ")
                .pattern(" S ")
                .define('C', item("ingot_copper"))
                .define('I', Items.IRON_INGOT)
                .define('S', Items.STICK)
                .unlockedBy("has_copper_ingot", has(item("ingot_copper")))
                .save(output, id("tool/dwarven_pickaxe"));

        // ToolRecipes.java:68-79: super pickaxes/axes. W.bolt() -> the c:bolts/tungsten common tag
        // (MaterialShapes.BOLT is autogenerated for Mats.MAT_TUNGSTEN - confirmed via
        // com.hbm.items.MaterialItemGenerator's AUTOGEN_SHAPES list and MAT_TUNGSTEN's own
        // setAutogen(...) call, so "tungsten_bolt" really is a registered, correctly-tagged item).
        Ingredient tungstenBolt = Ingredient.of(MaterialShapes.BOLT.commonTag(Mats.MAT_TUNGSTEN));
        superPickaxeOrAxe(output, "bismuth_pickaxe", item("ingot_bismuth"), item("starmetal_pickaxe"), tungstenBolt, "tool/bismuth_pickaxe");
        superPickaxeOrAxe(output, "bismuth_axe", item("ingot_bismuth"), item("starmetal_axe"), tungstenBolt, "tool/bismuth_axe");
        superPickaxeOrAxe(output, "volcanic_pickaxe", item("gem_volcanic"), item("starmetal_pickaxe"), tungstenBolt, "tool/volcanic_pickaxe");
        superPickaxeOrAxe(output, "volcanic_axe", item("gem_volcanic"), item("starmetal_axe"), tungstenBolt, "tool/volcanic_axe");

        // ToolRecipes.java:70-71/77-78: chlorophyte tools each have 2 alternate CE recipes (built from
        // either the bismuth_* or volcanic_* precursor) - both ported as separate recipe ids, matching
        // vanilla datagen's normal "multiple recipes, same result" pattern. DURA.bolt() -> the
        // c:bolts/durasteel common tag (same autogen confirmation as tungsten above, for Mats.MAT_DURA).
        Ingredient durasteelBolt = Ingredient.of(MaterialShapes.BOLT.commonTag(Mats.MAT_DURA));
        chlorophyteTool(output, "chlorophyte_pickaxe", "bismuth_pickaxe", durasteelBolt, "tool/chlorophyte_pickaxe_from_bismuth");
        chlorophyteTool(output, "chlorophyte_pickaxe", "volcanic_pickaxe", durasteelBolt, "tool/chlorophyte_pickaxe_from_volcanic");
        chlorophyteTool(output, "chlorophyte_axe", "bismuth_axe", durasteelBolt, "tool/chlorophyte_axe_from_bismuth");
        chlorophyteTool(output, "chlorophyte_axe", "volcanic_axe", durasteelBolt, "tool/chlorophyte_axe_from_volcanic");

        // Deliberately not ported (see class javadoc for the full reasoning):
        // - elec_sword/_pickaxe/_axe/_shovel, centri_stick, smashing_hammer, chainsaw, matchstick,
        //   crowbar, bottle_opener, carts, gavels and the detector/utility bucket (rangefinder,
        //   designator, linker, oil_detector, turret_chip, survey_scanner, geiger_counter, dosimeter,
        //   digamma_diagnostic, pollution_detector, coltan_tool, mirror_tool, rbmk_tool,
        //   power_net_tool, analysis_tool, toolbox, screwdriver*, hand_drill*, chemistry_set*,
        //   blowtorch*, boltgun, rebar_placer, bobmazon) - each needs at least one of: this port's
        //   still-unbuilt circuit-component family, a "motor"/"canister_empty"/"piston_selenium" item
        //   that does not exist under any name this class could confirm, or a plain resource item
        //   (sulfur, dust, block_steel/block_tungsten) likewise not found registered anywhere.
        // - starmetal_sword/_pickaxe/_axe/_shovel/_hoe, schrabidium_sword/_pickaxe/_axe/_shovel/_hoe,
        //   cobalt_decorated_sword/_pickaxe/_axe/_shovel/_hoe - CE gates the simple version of these
        //   behind GeneralConfig.enableLBSM, which defaults to false
        //   (upstream/hbm-ce/src/main/java/com/hbm/config/GeneralConfig.java:107,274). The real CE
        //   default (the "else" branch, ToolRecipes.java:184-193) crafts starmetal_* FROM
        //   cobalt_decorated_* - but cobalt_decorated_* has no recipe at all in that same default
        //   branch (it only gets one under the LBSM-enabled branch), making the whole chain
        //   uncraftable in CE's own out-of-the-box config. Skipping this branch entirely is therefore
        //   the most faithful choice, not a scope cut: it reproduces CE's real default behavior
        //   (no crafting path) rather than picking one config side to hard-code.
        // - schrabidium_sword/etc. also transitively need ModItems.ring_starmetal and
        //   ModBlocks.block_schrabidium, neither of which exists in this port under any name found.
    }

    private static void sword(RecipeOutput output, Item ingot, Item result, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, result)
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ingot))
                .save(output, id(path));
    }

    private static void pickaxe(RecipeOutput output, Item ingot, Item result, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ingot))
                .save(output, id(path));
    }

    private static void axe(RecipeOutput output, Item ingot, Item result, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("II")
                .pattern("IS")
                .pattern(" S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ingot))
                .save(output, id(path));
    }

    private static void shovel(RecipeOutput output, Item ingot, Item result, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("I")
                .pattern("S")
                .pattern("S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ingot))
                .save(output, id(path));
    }

    private static void hoe(RecipeOutput output, Item ingot, Item result, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, result)
                .pattern("II")
                .pattern(" S")
                .pattern(" S")
                .define('I', ingot)
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ingot))
                .save(output, id(path));
    }

    /** ToolRecipes.java:68-79 pattern: " BM","BPB","TB ". */
    private static void superPickaxeOrAxe(RecipeOutput output, String resultId, Item b, Item precursor, Ingredient tungstenBolt, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item(resultId))
                .pattern(" BM")
                .pattern("BPB")
                .pattern("TB ")
                .define('B', b)
                .define('M', item("ingot_meteorite"))
                .define('P', precursor)
                .define('T', tungstenBolt)
                .unlockedBy("has_precursor", has(precursor))
                .save(output, id(path));
    }

    /** ToolRecipes.java:70-71/77-78 pattern: " SD","APS","FA ". */
    private static void chlorophyteTool(RecipeOutput output, String resultId, String precursorId, Ingredient durasteelBolt, String path) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item(resultId))
                .pattern(" SD")
                .pattern("APS")
                .pattern("FA ")
                .define('S', item("blades_steel"))
                .define('D', item("powder_chlorophyte"))
                .define('A', item("ingot_fiberglass"))
                .define('P', item(precursorId))
                .define('F', durasteelBolt)
                .unlockedBy("has_precursor", has(item(precursorId)))
                .save(output, id(path));
    }

    // ================================================================================================
    // Part 2: MineralRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/MineralRecipes.java
    // ================================================================================================

    /**
     * {name, billet id, ingot id, nugget id} - the 39 (of CE's 52) {@code addBillet(billet, ingot,
     * nugget, ...)} calls whose billet/ingot/nugget triple all resolve to real items in this port
     * (verified individually against {@code com.hbm.items.BilletPowderItems}/{@code IngotNuggetItems}
     * source, not assumed from CE's field names). The ore-dict compatibility vararg every CE call
     * also carries is dropped (see class javadoc). The 13 CE calls not reproduced here:
     * {@code billet_uzh} (no matching ingot/nugget triple in CE's own call - CE itself only ever
     * calls the 2-arg form for it, and that form is not in this port's covered set either since no
     * {@code nugget_uzh} exists), and the block-touching/ore-dict-only tail of the file, which
     * {@link #mineralRecipes} does not reach at all (see class javadoc).
     */
    private static final String[][] BILLET_SETS = {
            {"cobalt", "billet_cobalt", "ingot_cobalt", "nugget_cobalt"},
            {"co60", "billet_co60", "ingot_co60", "nugget_co60"},
            {"sr90", "billet_sr90", "ingot_sr90", "nugget_sr90"},
            {"uranium", "billet_uranium", "ingot_uranium", "nugget_uranium"},
            {"u233", "billet_u233", "ingot_u233", "nugget_u233"},
            {"u235", "billet_u235", "ingot_u235", "nugget_u235"},
            {"u238", "billet_u238", "ingot_u238", "nugget_u238"},
            {"th232", "billet_th232", "ingot_th232", "nugget_th232"},
            {"plutonium", "billet_plutonium", "ingot_plutonium", "nugget_plutonium"},
            {"pu238", "billet_pu238", "ingot_pu238", "nugget_pu238"},
            {"pu239", "billet_pu239", "ingot_pu239", "nugget_pu239"},
            {"pu240", "billet_pu240", "ingot_pu240", "nugget_pu240"},
            {"pu241", "billet_pu241", "ingot_pu241", "nugget_pu241"},
            {"pu_mix", "billet_pu_mix", "ingot_pu_mix", "nugget_pu_mix"},
            {"am241", "billet_am241", "ingot_am241", "nugget_am241"},
            {"am242", "billet_am242", "ingot_am242", "nugget_am242"},
            {"am_mix", "billet_am_mix", "ingot_am_mix", "nugget_am_mix"},
            {"neptunium", "billet_neptunium", "ingot_neptunium", "nugget_neptunium"},
            {"polonium", "billet_polonium", "ingot_polonium", "nugget_polonium"},
            {"technetium", "billet_technetium", "ingot_technetium", "nugget_technetium"},
            {"au198", "billet_au198", "ingot_au198", "nugget_au198"},
            {"pb209", "billet_pb209", "ingot_pb209", "nugget_pb209"},
            {"ra226", "billet_ra226", "ingot_ra226", "nugget_ra226"},
            {"actinium", "billet_actinium", "ingot_actinium", "nugget_actinium"},
            {"schrabidium", "billet_schrabidium", "ingot_schrabidium", "nugget_schrabidium"},
            {"solinium", "billet_solinium", "ingot_solinium", "nugget_solinium"},
            {"gh336", "billet_gh336", "ingot_gh336", "nugget_gh336"},
            {"uranium_fuel", "billet_uranium_fuel", "ingot_uranium_fuel", "nugget_uranium_fuel"},
            {"thorium_fuel", "billet_thorium_fuel", "ingot_thorium_fuel", "nugget_thorium_fuel"},
            {"plutonium_fuel", "billet_plutonium_fuel", "ingot_plutonium_fuel", "nugget_plutonium_fuel"},
            {"neptunium_fuel", "billet_neptunium_fuel", "ingot_neptunium_fuel", "nugget_neptunium_fuel"},
            {"mox_fuel", "billet_mox_fuel", "ingot_mox_fuel", "nugget_mox_fuel"},
            {"les", "billet_les", "ingot_les", "nugget_les"},
            {"schrabidium_fuel", "billet_schrabidium_fuel", "ingot_schrabidium_fuel", "nugget_schrabidium_fuel"},
            {"hes", "billet_hes", "ingot_hes", "nugget_hes"},
            {"australium", "billet_australium", "ingot_australium", "nugget_australium"},
            {"beryllium", "billet_beryllium", "ingot_beryllium", "nugget_beryllium"},
            {"zirconium", "billet_zirconium", "ingot_zirconium", "nugget_zirconium"},
            {"bismuth", "billet_bismuth", "ingot_bismuth", "nugget_bismuth"},
            {"silicon", "billet_silicon", "ingot_silicon", "nugget_silicon"},
    };

    /** {name, billet id, nugget id} - CE's 2-arg {@code addBillet(billet, nugget)} calls (no ingot leg). */
    private static final String[][] BILLET_NUGGET_ONLY = {
            {"australium_greater", "billet_australium_greater", "nugget_australium_greater"},
            {"australium_lesser", "billet_australium_lesser", "nugget_australium_lesser"},
    };

    /**
     * {name, nugget id, ingot id} - 6 of CE's {@code addMineralSet(nugget, ingot, block)} calls (the
     * nugget&harr;ingot leg only; the block leg is dropped, see class javadoc). The 7th call CE makes,
     * for {@code nuclear_waste_vitrified}/{@code _tiny}, is skipped - neither item was found
     * registered anywhere in this port.
     */
    private static final String[][] MINERAL_SETS = {
            {"niobium", "nugget_niobium", "ingot_niobium"},
            {"bismuth", "nugget_bismuth", "ingot_bismuth"},
            {"tantalium", "nugget_tantalium", "ingot_tantalium"},
            {"zirconium", "nugget_zirconium", "ingot_zirconium"},
            {"dineutronium", "nugget_dineutronium", "ingot_dineutronium"},
            {"pu_mix", "nugget_pu_mix", "ingot_pu_mix"},
    };

    /**
     * {name, "one" id, "many"(x9) id} - CE's {@code add1To9Pair(one, nine)} family: 16 tiny/regular
     * powder pairs (MineralRecipes.java:70-74/370-387) plus 3 further 1-to-9 pairs CE writes as
     * standalone {@code addRecipeAuto}/{@code addShapelessAuto} calls with the identical shape
     * (mercury nugget/nugget_tiny - MineralRecipes.java:33, noting CE's own field-name-vs-registry-id
     * mismatch documented in {@code IngotNuggetItems}; silicon ingot/nugget - line 68; osmiridium
     * ingot/nugget - line 401).
     */
    private static final String[][] ONE_TO_NINE_PAIRS = {
            {"boron", "powder_boron", "powder_boron_tiny"},
            {"sr90", "powder_sr90", "powder_sr90_tiny"},
            {"xe135", "powder_xe135", "powder_xe135_tiny"},
            {"cs137", "powder_cs137", "powder_cs137_tiny"},
            {"i131", "powder_i131", "powder_i131_tiny"},
            {"coal", "powder_coal", "powder_coal_tiny"},
            {"steel", "powder_steel", "powder_steel_tiny"},
            {"lithium", "powder_lithium", "powder_lithium_tiny"},
            {"cobalt", "powder_cobalt", "powder_cobalt_tiny"},
            {"neodymium", "powder_neodymium", "powder_neodymium_tiny"},
            {"niobium", "powder_niobium", "powder_niobium_tiny"},
            {"cerium", "powder_cerium", "powder_cerium_tiny"},
            {"lanthanium", "powder_lanthanium", "powder_lanthanium_tiny"},
            {"actinium", "powder_actinium", "powder_actinium_tiny"},
            {"meteorite", "powder_meteorite", "powder_meteorite_tiny"},
            {"paleogenite", "powder_paleogenite", "powder_paleogenite_tiny"},
            {"mercury", "nugget_mercury", "nugget_mercury_tiny"},
            {"silicon", "ingot_silicon", "nugget_silicon"},
            {"osmiridium", "ingot_osmiridium", "nugget_osmiridium"},
    };

    private void mineralRecipes(RecipeOutput output) {
        for (String[] row : BILLET_SETS) {
            billetSet(output, row[0], item(row[1]), item(row[2]), item(row[3]));
        }
        for (String[] row : BILLET_NUGGET_ONLY) {
            billetNuggetOnly(output, row[0], item(row[1]), item(row[2]));
        }
        for (String[] row : MINERAL_SETS) {
            onePair(output, "mineral/" + row[0], item(row[1]), item(row[2]));
        }
        for (String[] row : ONE_TO_NINE_PAIRS) {
            onePair(output, "mineral/" + row[0] + "_pair", item(row[1]), item(row[2]));
        }
    }

    /**
     * CE {@code CraftingManager.addBillet(Item billet, Item ingot, Item nugget)}
     * (CraftingManager.java:1257 wraps the pair below plus {@code addBilletToIngot}) /
     * {@code MineralRecipes.addBillet(Item, Item, Item)} (MineralRecipes.java:504-508, identical
     * body): 6 nugget &harr; 1 billet, 3 billet &harr; 2 ingot.
     */
    private static void billetSet(RecipeOutput output, String name, Item billet, Item ingot, Item nugget) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, billet)
                .pattern("###")
                .pattern("###")
                .define('#', nugget)
                .unlockedBy("has_nugget", has(nugget))
                .save(output, id("mineral/" + name + "_billet_from_nugget"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget, 6)
                .requires(billet)
                .unlockedBy("has_billet", has(billet))
                .save(output, id("mineral/" + name + "_nugget_from_billet"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ingot, 2)
                .requires(billet)
                .requires(billet)
                .requires(billet)
                .unlockedBy("has_billet", has(billet))
                .save(output, id("mineral/" + name + "_ingot_from_billet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, billet, 3)
                .pattern("##")
                .define('#', ingot)
                .unlockedBy("has_ingot", has(ingot))
                .save(output, id("mineral/" + name + "_billet_from_ingot"));
    }

    /** CE {@code addBillet(Item billet, Item nugget)} (MineralRecipes.java:499-502): 6 nugget &harr; 1 billet only, no ingot leg. */
    private static void billetNuggetOnly(RecipeOutput output, String name, Item billet, Item nugget) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, billet)
                .pattern("###")
                .pattern("###")
                .define('#', nugget)
                .unlockedBy("has_nugget", has(nugget))
                .save(output, id("mineral/" + name + "_billet_from_nugget"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, nugget, 6)
                .requires(billet)
                .unlockedBy("has_billet", has(billet))
                .save(output, id("mineral/" + name + "_nugget_from_billet"));
    }

    /**
     * CE {@code CraftingManager}/{@code MineralRecipes}' shared {@code add1To9Pair(one, nine)} /
     * {@code addMineralSet}'s nugget&harr;ingot leg: 1 "one" &harr; 9 "many", both directions.
     */
    private static void onePair(RecipeOutput output, String path, Item one, Item many) {
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, many, 9)
                .requires(one)
                .unlockedBy("has_one", has(one))
                .save(output, id(path + "_many_from_one"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, one)
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', many)
                .unlockedBy("has_many", has(many))
                .save(output, id(path + "_one_from_many"));
    }

    // ================================================================================================
    // Part 3: ArmorRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/ArmorRecipes.java
    // ================================================================================================

    /**
     * The small, heavily-filtered slice of CE's ~90 {@code ArmorRecipes.java} entries whose result
     * item and every ingredient are both real, registered items in this port - see the class javadoc
     * for exactly why the great majority of CE's armor recipe corpus is blocked (missing base armor
     * tiers, missing circuit family, missing motor/tank_steel/thruster_small/cladding_lead/watch).
     */
    private void armorRecipes(RecipeOutput output) {
        // ArmorRecipes.java:115-118 (euphemium). euphemium_plate skipped - needs ModItems.watch,
        // not registered anywhere in this port.
        Item plateEuphemium = item("plate_euphemium");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("euphemium_helmet"))
                .pattern("EEE").pattern("E E")
                .define('E', plateEuphemium)
                .unlockedBy("has_plate", has(plateEuphemium))
                .save(output, id("armor/euphemium_helmet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("euphemium_legs"))
                .pattern("EEE").pattern("E E").pattern("E E")
                .define('E', plateEuphemium)
                .unlockedBy("has_plate", has(plateEuphemium))
                .save(output, id("armor/euphemium_legs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("euphemium_boots"))
                .pattern("E E").pattern("E E")
                .define('E', plateEuphemium)
                .unlockedBy("has_plate", has(plateEuphemium))
                .save(output, id("armor/euphemium_boots"));

        // ArmorRecipes.java:163: mask_of_infamy, 7x plate_iron.
        Item plateIron = item("plate_iron");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("mask_of_infamy"))
                .pattern("III").pattern("III").pattern(" I ")
                .define('I', plateIron)
                .unlockedBy("has_plate", has(plateIron))
                .save(output, id("armor/mask_of_infamy"));

        // ArmorRecipes.java:158-162: masks. mask_rag/mask_piss (line 165-166) are NOT reproduced -
        // ModItems.rag_damp/rag_piss in CE are not separate items, only ItemRag *state names* on the
        // single "rag" item (confirmed by reading com.hbm.items.special.SpecialItems.RAG's own
        // constructor call); matching one specific rag state as a plain shaped-recipe ingredient would
        // need a component-predicate custom ingredient, the same "handlers"-class complexity this
        // task's scope explicitly excludes (see class javadoc). goggles/ashglasses are skipped too -
        // neither output item was found registered under any name in this port.
        Item plateSteel = item("plate_steel");
        Item ingotRubber = item("ingot_rubber");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gas_mask"))
                .pattern("PPP").pattern("GPG").pattern(" F ")
                .define('P', plateSteel)
                .define('G', GLASS_PANES)
                .define('F', plateIron)
                .unlockedBy("has_plate", has(plateSteel))
                .save(output, id("armor/gas_mask"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gas_mask_m65"))
                .pattern("PPP").pattern("GPG").pattern(" F ")
                .define('P', ingotRubber)
                .define('G', GLASS_PANES)
                .define('F', plateIron)
                .unlockedBy("has_rubber", has(ingotRubber))
                .save(output, id("armor/gas_mask_m65"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gas_mask_olde"))
                .pattern("PPP").pattern("GPG").pattern(" F ")
                .define('P', Items.LEATHER)
                .define('G', GLASS_PANES)
                .define('F', Items.IRON_INGOT)
                .unlockedBy("has_leather", has(Items.LEATHER))
                .save(output, id("armor/gas_mask_olde"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gas_mask_mono"))
                .pattern(" P ").pattern("PPP").pattern(" F ")
                .define('P', ingotRubber)
                .define('F', plateIron)
                .unlockedBy("has_rubber", has(ingotRubber))
                .save(output, id("armor/gas_mask_mono"));

        // ArmorRecipes.java:102: dieselsuit_boots only ("W W","S S"; helmet/plate/legs all need the
        // still-unbuilt circuit family or ModItems.motor, see class javadoc). CE's metadata-14 wool is
        // RED (EnumDyeColor ordinal 14), not black - Items.RED_WOOL is the direct 1.21 equivalent.
        Item ingotSteel = item("ingot_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("dieselsuit_boots"))
                .pattern("W W").pattern("S S")
                .define('W', Items.RED_WOOL)
                .define('S', ingotSteel)
                .unlockedBy("has_steel_ingot", has(ingotSteel))
                .save(output, id("armor/dieselsuit_boots"));

        // ArmorRecipes.java:104-106: envsuit_plate/_legs/_boots (helmet needs circuit CHIP, skipped).
        // TI.plateCast() -> the c:plates_triple/titanium common tag (MaterialShapes.CASTPLATE is
        // autogenerated for Mats.MAT_TITANIUM - confirmed the same way as the tool tags above).
        Item plateTitanium = item("plate_titanium");
        Ingredient titaniumCastPlate = Ingredient.of(MaterialShapes.CASTPLATE.commonTag(Mats.MAT_TITANIUM));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("envsuit_plate"))
                .pattern("T T").pattern("TCT").pattern("RRR")
                .define('T', plateTitanium)
                .define('C', titaniumCastPlate)
                .define('R', ingotRubber)
                .unlockedBy("has_titanium_plate", has(plateTitanium))
                .save(output, id("armor/envsuit_plate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("envsuit_legs"))
                .pattern("TCT").pattern("R R").pattern("T T")
                .define('T', plateTitanium)
                .define('C', titaniumCastPlate)
                .define('R', ingotRubber)
                .unlockedBy("has_titanium_plate", has(plateTitanium))
                .save(output, id("armor/envsuit_legs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("envsuit_boots"))
                .pattern("R R").pattern("T T")
                .define('T', plateTitanium)
                .define('R', ingotRubber)
                .unlockedBy("has_titanium_plate", has(plateTitanium))
                .save(output, id("armor/envsuit_boots"));

        // ArmorRecipes.java:109/112: bismuth_helmet/_boots (plate/legs need ModItems.ring_starmetal,
        // not registered anywhere in this port - skipped).
        Item plateBismuth = item("plate_bismuth");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("bismuth_helmet"))
                .pattern("GPP").pattern("P  ").pattern("FPP")
                .define('G', Items.GOLD_INGOT)
                .define('P', plateBismuth)
                .define('F', item("rag"))
                .unlockedBy("has_plate", has(plateBismuth))
                .save(output, id("armor/bismuth_helmet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("bismuth_boots"))
                .pattern("W W").pattern("P P")
                .define('W', Ingredient.of(MaterialShapes.WIRE.commonTag(Mats.MAT_GOLD)))
                .define('P', plateBismuth)
                .unlockedBy("has_plate", has(plateBismuth))
                .save(output, id("armor/bismuth_boots"));

        // ArmorRecipes.java:88-90: dns_plate/_legs/_boots (helmet needs circuit QUANTUM, skipped).
        // The bj_plate_jetpack/bj_legs/bj_boots ingredients are real registered items even though this
        // pass could not port a recipe to craft them (see class javadoc) - a valid crafting ingredient
        // only needs to exist as an item, not to itself be reachable via crafting yet.
        Item plateArmorDnt = item("plate_armor_dnt");
        Item ingotChainsteel = item("ingot_chainsteel");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("dns_plate"))
                .pattern("PCP").pattern("PBP").pattern("PSP")
                .define('P', plateArmorDnt)
                .define('C', item("singularity_spark"))
                .define('B', item("bj_plate_jetpack"))
                .define('S', ingotChainsteel)
                .unlockedBy("has_plate", has(plateArmorDnt))
                .save(output, id("armor/dns_plate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("dns_legs"))
                .pattern("PCP").pattern("PBP").pattern("PSP")
                .define('P', plateArmorDnt)
                .define('C', item("coin_worm"))
                .define('B', item("bj_legs"))
                .define('S', ingotChainsteel)
                .unlockedBy("has_plate", has(plateArmorDnt))
                .save(output, id("armor/dns_legs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("dns_boots"))
                .pattern("PCP").pattern("PBP").pattern("PSP")
                .define('P', plateArmorDnt)
                .define('C', item("demon_core_closed"))
                .define('B', item("bj_boots"))
                .define('S', ingotChainsteel)
                .unlockedBy("has_plate", has(plateArmorDnt))
                .save(output, id("armor/dns_boots"));

        // ArmorRecipes.java:70-73: ajro_* (dyed ajr_*). ajr_* itself has no CE recipe in this pass
        // either (needs titanium_helmet/_plate/_legs/_boots, none of which exist in this port - see
        // class javadoc) but is, again, a real registered item, valid as an ingredient here.
        ajro(output, "ajro_helmet", "ajr_helmet");
        ajro(output, "ajro_plate", "ajr_plate");
        ajro(output, "ajro_legs", "ajr_legs");
        ajro(output, "ajro_boots", "ajr_boots");
    }

    private static void ajro(RecipeOutput output, String resultId, String ajrId) {
        Item ajr = item(ajrId);
        ShapelessRecipeBuilder.shapeless(RecipeCategory.COMBAT, item(resultId))
                .requires(ajr)
                .requires(Items.RED_DYE)
                .requires(Items.BLACK_DYE)
                .unlockedBy("has_ajr", has(ajr))
                .save(output, id("armor/" + resultId));
    }

    // ================================================================================================
    // Shared helpers
    // ================================================================================================

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
    }

    /**
     * Resolve-by-id lookup against the already-populated {@link BuiltInRegistries#ITEM} registry,
     * mirroring the pattern already proven safe in this codebase by
     * {@code com.hbm.items.datagen.ModItemTagProvider} - {@code GatherDataEvent} (which drives this
     * whole provider) only ever fires after every mod's {@code RegisterEvent} has already run, so
     * every real item id is present here regardless of which class registered it or what its Java
     * constant is (or isn't) named. Throws loudly, naming the missing id, rather than silently
     * building a broken recipe around a null ingredient - every id passed here was individually
     * confirmed against this port's own item-registration source while writing this class (see the
     * class javadoc), so a failure here means either a genuine regression in the owning item class or
     * a mistake in this class that a reviewer needs to see, not something to degrade past quietly.
     */
    private static Item item(String path) {
        ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, path);
        Item found = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (found == null) {
            throw new IllegalStateException("ModRecipeProvider: item hbm:" + path
                    + " referenced by a recipe is not registered - check the owning item-registration class");
        }
        return found;
    }
}
