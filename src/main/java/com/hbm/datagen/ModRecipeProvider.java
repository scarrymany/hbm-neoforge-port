package com.hbm.datagen;

import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.inventory.recipes.crafting.ContainerUpgradeRecipe;
import com.hbm.inventory.recipes.crafting.GrenadeCraftingRecipe;
import com.hbm.inventory.recipes.crafting.RBMKFuelRecycleRecipe;
import com.hbm.inventory.recipes.crafting.ScrapSplitRecipe;
import com.hbm.items.special.SpecialItemComponents;
import com.hbm.main.MainRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.CompoundIngredient;

import java.util.concurrent.CompletableFuture;

/**
 * Ports the highest-value slice of CE's vanilla-crafting-table recipe corpus
 * ({@code upstream/hbm-ce/src/main/java/com/hbm/main/CraftingManager.java} (1,602 lines) +
 * {@code upstream/hbm-ce/src/main/java/com/hbm/crafting/*.java} (9 files, 2,085 lines)), whose scale
 * (~1,900-2,000+ individual recipe registrations) and complete absence from this port (before this
 * class, exactly 1 vanilla crafting recipe existed anywhere: {@code data/hbm/recipe/mech_key.json})
 * was surveyed and confirmed by {@code docs/phase5/advancement_and_recipe_datagen_assets.md} Part 2.
 * That report explicitly scoped a first implement pass to CE's {@code ToolRecipes}, {@code ArmorRecipes}
 * and {@code MineralRecipes} (its own table gives the exact per-class counts) - this class originally
 * covered exactly that slice.
 * <p>
 * A second pass ({@code docs/phase7/crafting_tools_armor_smelting.md}, Phase 7) added {@link #toolRecipes}'s
 * and {@link #armorRecipes}'s remaining ready-to-port gap (the entries the first pass's own item-
 * registry audit left out only because of missing items, re-audited fresh rather than re-quoted - see
 * each addition's inline citation for exactly what changed) plus a full new {@link #smeltingRecipes},
 * porting CE's {@code SmeltingRecipes.java} (previously completely untouched by this port) as plain
 * vanilla {@code minecraft:smelting} recipes via {@link SimpleCookingRecipeBuilder}. This class
 * documents below precisely which entries within all four CE classes could and could not be ported,
 * matching the original pass's own honest-scoping discipline rather than silently truncating.
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
 *     assembly, scrap sorting) needed genuine new {@code RecipeType}/{@code RecipeSerializer}/
 *     {@code Recipe<CraftingInput>} Java classes, not plain shaped/shapeless JSON - out of this
 *     class's own scope, but landed by a later pass
 *     ({@code docs/phase7/crafting_dynamic_handlers.md}) as {@code com.hbm.inventory.recipes.crafting.*}
 *     (grenade assembly, RBMK spent-fuel recycling, scrap splitting, and 3 of 5
 *     {@code ContainerUpgradeCraftingHandler} tiers - see {@link #dynamicHandlerRecipes} for the
 *     minimal type-only JSON stub each one still needs). Cargo shells, MKU ammo, fluid duct
 *     retyping, and the 2 mass-storage container-upgrade tiers remain unported - blocked on missing
 *     items/blocks (or, for fluid duct retyping, an architecture mismatch) per that report's
 *     dependency check, not on the recipe-class mechanism itself.</li>
 *     <li>CE's {@code CraftingManager.addCrafting()} (the ~1,200-line dispatcher method's own inline
 *     recipes, distinct from the 8 named sub-registrar classes) and CE's {@code WeaponRecipes}/
 *     {@code ConsumableRecipes} - not reached in this pass; every ingredient/result item they
 *     reference (circuits, ammo components, etc.) would need the same per-recipe registry-name audit
 *     this class's covered areas already received. {@code PowderRecipes}/{@code ExclusiveRecipes} were
 *     reached by a later pass ({@code docs/phase7/crafting_minerals_powder_exclusive.md}) - see
 *     {@link #powderRecipes} and the "Part 6: ExclusiveRecipes" comment block (all 6
 *     {@code ExclusiveRecipes} entries turned out blocked on missing items, not ported).</li>
 *     <li>CE's block&harr;ingot 3x3 compression grid (the back half of {@code MineralRecipes.java}, CE
 *     lines ~228-395) - the original pass's claim that "this port's material storage-block items...
 *     were not found under any name this class could confirm exists" was corrected by a later pass
 *     ({@code docs/phase7/crafting_minerals_powder_exclusive.md}): {@code com.hbm.blocks.
 *     MaterialBlockGenerator} does generate one storage block per {@link Mats} material tagged
 *     {@link MaterialShapes#BLOCK}, under a suffix-first id ({@code titanium_block}, not CE's
 *     {@code block_titanium}) the original pass's grep never tried. {@link #mineralRecipes} now covers
 *     this grid too, via {@link #BLOCK_INGOT_SETS} (id derived programmatically, never guessed - see
 *     that field's own javadoc) plus {@link #BLOCK_INGOT_LEGACY} for the handful of targets registered
 *     under a hand-kept legacy {@code block_x} id instead.</li>
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
        smeltingRecipes(output);
        powderRecipes(output);
        rodRecipes(output);
        weaponRecipes(output);
        consumableRecipes(output);
        craftingManagerRecipes(output);
        dynamicHandlerRecipes(output);
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

        // ================================================================================================
        // docs/phase7/crafting_tools_armor_smelting.md's "ready to port now" ToolRecipes gap slice -
        // every ingredient AND result item individually re-confirmed registered while writing this
        // block (not re-quoted from the report on faith - see the 3 corrections below).
        // ================================================================================================

        // ToolRecipes.java:85: crowbar, "II"," I"," I", I=STEEL.ingot().
        Item ingotSteelForCrowbar = item("ingot_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("crowbar"))
                .pattern("II")
                .pattern(" I")
                .pattern(" I")
                .define('I', ingotSteelForCrowbar)
                .unlockedBy("has_ingot", has(ingotSteelForCrowbar))
                .save(output, id("tool/crowbar"));

        // ToolRecipes.java:86: bottle_opener, "S","P", S=STEEL.plate(), P=KEY_PLANKS (vanilla planks tag).
        Item plateSteelForTools = item("plate_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("bottle_opener"))
                .pattern("S")
                .pattern("P")
                .define('S', plateSteelForTools)
                .define('P', ItemTags.PLANKS)
                .unlockedBy("has_plate", has(plateSteelForTools))
                .save(output, id("tool/bottle_opener"));

        // ToolRecipes.java:99: ullapool_caber, "ITI"," S "," S ", I=IRON.plate(), T=Blocks.TNT, S=KEY_STICK.
        Item plateIronForTools = item("plate_iron");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("ullapool_caber"))
                .pattern("ITI")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', plateIronForTools)
                .define('T', Items.TNT)
                .define('S', Items.STICK)
                .unlockedBy("has_plate", has(plateIronForTools))
                .save(output, id("tool/ullapool_caber"));

        // ToolRecipes.java:94: wood_gavel, "SWS"," R "," R ", S=KEY_SLAB, W=KEY_LOG, R=KEY_STICK
        // (both vanilla wood tags - independently confirmed by docs/phase7/mrec_06_soldering_misc.md's
        // own KEY_LOG cross-check, not just assumed here).
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("wood_gavel"))
                .pattern("SWS")
                .pattern(" R ")
                .pattern(" R ")
                .define('S', ItemTags.WOODEN_SLABS)
                .define('W', ItemTags.LOGS)
                .define('R', Items.STICK)
                .unlockedBy("has_stick", has(Items.STICK))
                .save(output, id("tool/wood_gavel"));

        // ToolRecipes.java:151: bobmazon, shapeless [Items.BOOK, Items.GOLD_NUGGET, Items.STRING, KEY_BLUE].
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("bobmazon"))
                .requires(Items.BOOK)
                .requires(Items.GOLD_NUGGET)
                .requires(Items.STRING)
                .requires(Items.BLUE_DYE)
                .unlockedBy("has_book", has(Items.BOOK))
                .save(output, id("tool/bobmazon"));

        // ToolRecipes.java:132: mirror_tool, " A "," IA","I  ", A=AL.ingot(), I=IRON.ingot() (vanilla).
        Item ingotAluminium = item("ingot_aluminium");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("mirror_tool"))
                .pattern(" A ")
                .pattern(" IA")
                .pattern("I  ")
                .define('A', ingotAluminium)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_aluminium_ingot", has(ingotAluminium))
                .save(output, id("tool/mirror_tool"));

        // ToolRecipes.java:133: rbmk_tool, " A "," IA","I  ", A=PB.ingot(), I=IRON.ingot() (vanilla).
        Item ingotLeadForTools = item("ingot_lead");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("rbmk_tool"))
                .pattern(" A ")
                .pattern(" IA")
                .pattern("I  ")
                .define('A', ingotLeadForTools)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_lead_ingot", has(ingotLeadForTools))
                .save(output, id("tool/rbmk_tool"));

        // ToolRecipes.java:118: coltan_tool, "ACA","CXC","ACA", A=CU.ingot(), C=CINNABAR.crystal(), X=Items.COMPASS.
        Item ingotCopperForTools = item("ingot_copper");
        Item crystalCinnabar = item("crystal_cinnabar");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("coltan_tool"))
                .pattern("ACA")
                .pattern("CXC")
                .pattern("ACA")
                .define('A', ingotCopperForTools)
                .define('C', crystalCinnabar)
                .define('X', Items.COMPASS)
                .unlockedBy("has_crystal", has(crystalCinnabar))
                .save(output, id("tool/coltan_tool"));

        // ToolRecipes.java:139: screwdriver, "  I"," I ","S  ", S=STEEL.ingot(), I=IRON.ingot() (vanilla).
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("screwdriver"))
                .pattern("  I")
                .pattern(" I ")
                .pattern("S  ")
                .define('S', ingotSteelForCrowbar)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_ingot", has(ingotSteelForCrowbar))
                .save(output, id("tool/screwdriver"));

        // ToolRecipes.java:137: toolbox, "CCC","CIC", C=CU.plate(), I=IRON.ingot() (vanilla).
        Item plateCopper = item("plate_copper");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("toolbox"))
                .pattern("CCC")
                .pattern("CIC")
                .define('C', plateCopper)
                .define('I', Items.IRON_INGOT)
                .unlockedBy("has_plate", has(plateCopper))
                .save(output, id("tool/toolbox"));

        // ToolRecipes.java:144: chemistry_set_boron, "GIG","GCG", G=ModBlocks.glass_boron, I=STEEL.ingot(), C=CO.ingot().
        Item glassBoron = item("glass_boron");
        Item ingotCobaltForTools = item("ingot_cobalt");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("chemistry_set_boron"))
                .pattern("GIG")
                .pattern("GCG")
                .define('G', glassBoron)
                .define('I', ingotSteelForCrowbar)
                .define('C', ingotCobaltForTools)
                .unlockedBy("has_glass_boron", has(glassBoron))
                .save(output, id("tool/chemistry_set_boron"));

        // ToolRecipes.java:134: power_net_tool, "WRW"," I "," B ", W=MINGRADE.wireFine(), R=REDSTONE.dust()
        // (vanilla), I=IRON.ingot() (vanilla), B=ItemBatteryPack.EnumBatteryPack.BATTERY_LEAD.stack().
        // CORRECTION to the research report: CE's ModItems.battery_lead maps to registry id
        // "battery_lead_pack" in this port, not "battery_lead" - MachineItems.java:147 builds every
        // EnumBatteryPack's id as lower(type.name()) + "_pack", and BATTERY_LEAD's enum constant name
        // (not its "battery_lead" texture-path constructor argument the report's grep matched on) is
        // what actually feeds that id.
        Item batteryLeadPack = item("battery_lead_pack");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("power_net_tool"))
                .pattern("WRW")
                .pattern(" I ")
                .pattern(" B ")
                .define('W', MaterialShapes.WIRE.commonTag(Mats.MAT_MINGRADE))
                .define('R', Items.REDSTONE)
                .define('I', Items.IRON_INGOT)
                .define('B', batteryLeadPack)
                .unlockedBy("has_battery", has(batteryLeadPack))
                .save(output, id("tool/power_net_tool"));

        // ToolRecipes.java:141: hand_drill, " D","S "," S", D=DURA.ingot(), S=KEY_STICK. MAT_DURA has
        // no plain INGOT autogen shape (only BOLT/DUST/PLATE/CASTPLATE/PIPE/BLOCK/barrels/receivers/
        // GRIP, per Mats.java's own setAutogen list) - "ingot_dura_steel" is a distinct, directly
        // hand-registered item (IngotNuggetItems.java:103), used here by direct id like the rest of
        // this file's non-shape ingots rather than a MaterialShapes tag.
        Item ingotDuraSteel = item("ingot_dura_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("hand_drill"))
                .pattern(" D")
                .pattern("S ")
                .pattern(" S")
                .define('D', ingotDuraSteel)
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ingotDuraSteel))
                .save(output, id("tool/hand_drill"));

        // ToolRecipes.java:104: designator_range, shapeless [rangefinder, designator, ANY_PLASTIC.ingot()].
        // Judgment call flagged by the research report as "every ingredient is a real item even though
        // rangefinder/designator have no craft path of their own yet" - included, since a valid
        // crafting ingredient only needs to exist as an item (same reasoning ModRecipeProvider's own
        // armorRecipes() already applies to bj_plate_jetpack/ajr_helmet as ingredients). CE's sibling
        // designator_arty_range is NOT included here (correction to the report): its own result item
        // does not exist under any name in this port (0 grep hits for "designator_arty_range"), so
        // unlike designator_range it fails the "every ingredient AND output already registered" bar
        // outright, not just a "no craft path yet" judgment call.
        Item rangefinder = item("rangefinder");
        Item designator = item("designator");
        Item ingotPolymerForTools = item("ingot_polymer");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.TOOLS, item("designator_range"))
                .requires(rangefinder)
                .requires(designator)
                .requires(ingotPolymerForTools)
                .unlockedBy("has_rangefinder", has(rangefinder))
                .save(output, id("tool/designator_range"));

        // ToolRecipes.java:107 linker, "I I","ICI","GGG", I=IRON.plate(), G=GOLD.plate(), C=circuit ADVANCED.
        Item plateIronForLinker = item("plate_iron");
        Item plateGoldForLinker = item("plate_gold");
        Item circuitAdvanced = item("circuit_advanced");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("linker"))
                .pattern("I I")
                .pattern("ICI")
                .pattern("GGG")
                .define('I', plateIronForLinker)
                .define('G', plateGoldForLinker)
                .define('C', circuitAdvanced)
                .unlockedBy("has_circuit", has(circuitAdvanced))
                .save(output, id("tool/linker"));

        // Deliberately not ported (see class javadoc for the full reasoning):
        // - elec_sword/_pickaxe/_axe/_shovel, centri_stick, smashing_hammer, chainsaw, matchstick,
        //   carts, lead_gavel, pipe_lead, designator/designator_manual/designator_arty_range and the
        //   rest of the detector/utility bucket (oil_detector, turret_chip, survey_scanner,
        //   geiger_counter, dosimeter, digamma_diagnostic, pollution_detector, ore_density_scanner,
        //   defuser, reacher, sat_designator, sat_relay, settings_tool, pipette*, siphon, boat_rubber,
        //   analysis_tool, screwdriver_desh, hand_drill_desh, chemistry_set, blowtorch*, boltgun,
        //   rebar_placer) - each needs at least one of: this port's still-unbuilt circuit-component
        //   family, a "motor"/"canister_empty"/"piston_selenium"/"ducttape"/"tank_steel" item that
        //   does not exist under any name this class could confirm, or a plain resource item (sulfur,
        //   dust, block_steel/block_tungsten) likewise not found registered anywhere.
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

    /**
     * {name, ingot id, nugget id} - docs/phase7/crafting_minerals_powder_exclusive.md's "direct
     * ingot&harr;nugget 1:9 shortcut" family: CE's {@code MineralRecipes.java} hand-writes a plain
     * {@code add1To9Pair(ingot, nugget)} shortcut for 39 materials <em>in addition to</em> the
     * billet-mediated ingot&harr;billet&harr;nugget path {@link #BILLET_SETS}/{@link #billetSet}
     * already covers for 35 of those same 39 - a genuine, deliberate CE redundancy (a player who
     * doesn't want to touch the billet step at all), not a bug or a duplicate to dedupe. Only 4 of
     * the 39 materials here (arsenic, americium_fuel, euphemium, lead) have no billet family at all
     * in CE. {@link #onePair} is reused verbatim with a distinct {@code "mineral/shortcut_"} id
     * namespace so these never collide with {@link #billetSet}'s
     * {@code "mineral/<name>_billet_from_nugget"}-style ids for the same 35 overlapping materials.
     * <p>
     * <b>Correction to the research report:</b> the report marked {@code ingot_arsenic}/
     * {@code nugget_arsenic} (MineralRecipes.java:83) blocked, citing "Mats.MAT_ARSENIC's NUGGET
     * autogen isn't wired to any generator". Re-checked directly against
     * {@code com.hbm.items.IngotNuggetItems} source (not the report's own citation): both
     * {@code INGOT_ARSENIC} (line 127) and {@code NUGGET_ARSENIC} (line 306) are real, independently
     * hand-registered items there - CE's own field-per-item pattern this class's javadoc already
     * describes, unrelated to whether {@code Mats.MAT_ARSENIC} happens to carry a matching autogen
     * shape. Arsenic is included below as ready, not skipped.
     */
    private static final String[][] DIRECT_INGOT_NUGGET_SHORTCUTS = {
            {"technetium", "ingot_technetium", "nugget_technetium"},
            {"co60", "ingot_co60", "nugget_co60"},
            {"sr90", "ingot_sr90", "nugget_sr90"},
            {"au198", "ingot_au198", "nugget_au198"},
            {"pb209", "ingot_pb209", "nugget_pb209"},
            {"ra226", "ingot_ra226", "nugget_ra226"},
            {"actinium", "ingot_actinium", "nugget_actinium"},
            {"arsenic", "ingot_arsenic", "nugget_arsenic"},
            {"pu241", "ingot_pu241", "nugget_pu241"},
            {"am241", "ingot_am241", "nugget_am241"},
            {"am242", "ingot_am242", "nugget_am242"},
            {"am_mix", "ingot_am_mix", "nugget_am_mix"},
            {"americium_fuel", "ingot_americium_fuel", "nugget_americium_fuel"},
            {"gh336", "ingot_gh336", "nugget_gh336"},
            {"neptunium_fuel", "ingot_neptunium_fuel", "nugget_neptunium_fuel"},
            {"plutonium", "ingot_plutonium", "nugget_plutonium"},
            {"pu238", "ingot_pu238", "nugget_pu238"},
            {"pu239", "ingot_pu239", "nugget_pu239"},
            {"pu240", "ingot_pu240", "nugget_pu240"},
            {"th232", "ingot_th232", "nugget_th232"},
            {"uranium", "ingot_uranium", "nugget_uranium"},
            {"u233", "ingot_u233", "nugget_u233"},
            {"u235", "ingot_u235", "nugget_u235"},
            {"u238", "ingot_u238", "nugget_u238"},
            {"neptunium", "ingot_neptunium", "nugget_neptunium"},
            {"polonium", "ingot_polonium", "nugget_polonium"},
            {"lead", "ingot_lead", "nugget_lead"},
            {"beryllium", "ingot_beryllium", "nugget_beryllium"},
            {"schrabidium", "ingot_schrabidium", "nugget_schrabidium"},
            {"uranium_fuel", "ingot_uranium_fuel", "nugget_uranium_fuel"},
            {"thorium_fuel", "ingot_thorium_fuel", "nugget_thorium_fuel"},
            {"plutonium_fuel", "ingot_plutonium_fuel", "nugget_plutonium_fuel"},
            {"mox_fuel", "ingot_mox_fuel", "nugget_mox_fuel"},
            {"schrabidium_fuel", "ingot_schrabidium_fuel", "nugget_schrabidium_fuel"},
            {"hes", "ingot_hes", "nugget_hes"},
            {"les", "ingot_les", "nugget_les"},
            {"australium", "ingot_australium", "nugget_australium"},
            {"solinium", "ingot_solinium", "nugget_solinium"},
            {"euphemium", "ingot_euphemium", "nugget_euphemium"},
    };

    /**
     * {@link Mats} material constant, ingot registry id} - the block&harr;ingot 3x3 compression grid,
     * CE's {@code MineralRecipes.java} lines 35-54/228-324 plus the block leg of {@code
     * addMineralSet} (niobium/zirconium/bismuth/tantalium/dineutronium - lines 61-65, previously
     * dropped by {@link #MINERAL_SETS}, closed here instead). Per the research report's own
     * recommendation, the block id is derived <em>programmatically</em> via
     * {@link MaterialShapes#buildRegistryName(NTMMaterial)} - the exact method
     * {@code com.hbm.blocks.MaterialBlockGenerator} itself calls to register these blocks - rather
     * than guessed from CE's prefix-first {@code block_x} field name, since
     * {@link NTMMaterial#getRegistryName()} (the material's first alias, lowercased) frequently
     * disagrees with both CE's field name and this port's ingot id (e.g. {@code Mats.MAT_U233}'s
     * real block id is {@code uranium233_block}, {@code Mats.MAT_DESH}'s is
     * {@code workersalloy_block}, {@code Mats.MAT_ALUMINIUM}'s is {@code aluminum_block} - American
     * spelling, vs. this table's British-spelled {@code ingot_aluminium}) - confirmed individually
     * for every row below by reading {@code Mats.java} in full and cross-checking each material's
     * {@code setAutogen(...)} call for {@link MaterialShapes#BLOCK}, not assumed. {@code Mats.MAT_SLAG}
     * doubles as CE's {@code block_slag}/{@code ingot_raw}(meta {@code MAT_SLAG.id}) pair
     * (MineralRecipes.java:55): since {@code MAT_SLAG} also carries {@link MaterialShapes#INGOT}
     * autogen (one of {@code com.hbm.items.MaterialItemGenerator}'s 17 generated shapes), both legs
     * resolve cleanly to real generated items ({@code slag_block}/{@code slag_ingot}) with no need
     * for CE's metadata-on-a-shared-item trick.
     */
    private static final Object[][] BLOCK_INGOT_SETS = {
            {Mats.MAT_ALUMINIUM, "ingot_aluminium"},
            {Mats.MAT_BORON, "ingot_boron"},
            {Mats.MAT_SCHRARANIUM, "ingot_schraranium"},
            {Mats.MAT_LANTHANIUM, "ingot_lanthanium"},
            {Mats.MAT_RADIUM, "ingot_ra226"},
            {Mats.MAT_SCHRABIDATE, "ingot_schrabidate"},
            {Mats.MAT_SATURN, "ingot_saturnite"},
            {Mats.MAT_SLAG, MaterialShapes.INGOT.buildRegistryName(Mats.MAT_SLAG)},
            {Mats.MAT_URANIUM, "ingot_uranium"},
            {Mats.MAT_U233, "ingot_u233"},
            {Mats.MAT_U235, "ingot_u235"},
            {Mats.MAT_U238, "ingot_u238"},
            {Mats.MAT_THORIUM, "ingot_th232"},
            {Mats.MAT_LEAD, "ingot_lead"},
            {Mats.MAT_TITANIUM, "ingot_titanium"},
            {Mats.MAT_COPPER, "ingot_copper"},
            {Mats.MAT_TUNGSTEN, "ingot_tungsten"},
            {Mats.MAT_BERYLLIUM, "ingot_beryllium"},
            {Mats.MAT_SCHRABIDIUM, "ingot_schrabidium"},
            {Mats.MAT_MAGTUNG, "ingot_magnetized_tungsten"},
            {Mats.MAT_DESH, "ingot_desh"},
            {Mats.MAT_DURA, "ingot_dura_steel"},
            {Mats.MAT_STAR, "ingot_starmetal"},
            {Mats.MAT_NEPTUNIUM, "ingot_neptunium"},
            {Mats.MAT_POLONIUM, "ingot_polonium"},
            {Mats.MAT_PLUTONIUM, "ingot_plutonium"},
            {Mats.MAT_PU238, "ingot_pu238"},
            {Mats.MAT_PU239, "ingot_pu239"},
            {Mats.MAT_PU240, "ingot_pu240"},
            {Mats.MAT_SOLINIUM, "ingot_solinium"},
            {Mats.MAT_ASBESTOS, "ingot_asbestos"},
            {Mats.MAT_COBALT, "ingot_cobalt"},
            {Mats.MAT_STEEL, "ingot_steel"},
            {Mats.MAT_NIOBIUM, "ingot_niobium"},
            {Mats.MAT_ZIRCONIUM, "ingot_zirconium"},
            {Mats.MAT_BISMUTH, "ingot_bismuth"},
            {Mats.MAT_TANTALIUM, "ingot_tantalium"},
            {Mats.MAT_DNT, "ingot_dineutronium"},
    };

    /**
     * {block id, ingot/plate/fragment id} - the handful of CE block-grid targets whose port block is
     * hand-registered under CE's legacy {@code block_x} id ({@code GenericDecoBlocks}/
     * {@code GenericBlocks}/{@code WastelandVirusBlocks}) rather than generated by
     * {@code MaterialBlockGenerator} off a {@link Mats} constant (either because the material has no
     * {@link Mats} entry at all - australium, coltan - or the entry exists but was never tagged
     * {@link MaterialShapes#BLOCK} - actinium, cadmium, polymer, bakelite, rubber, fiberglass). All
     * 10 confirmed by direct {@code registerBlock("block_x", ...)} call-site grep against those three
     * classes, each paired with the exact ingredient CE's own {@code add1To9Pair}/compression-grid
     * call for that block uses.
     */
    private static final String[][] BLOCK_INGOT_LEGACY = {
            {"block_actinium", "ingot_actinium"},
            {"block_australium", "ingot_australium"},
            {"block_bakelite", "ingot_bakelite"},
            {"block_cadmium", "ingot_cadmium"},
            {"block_coltan", "fragment_coltan"},
            {"block_fiberglass", "ingot_fiberglass"},
            {"block_insulator", "plate_polymer"},
            {"block_polymer", "ingot_polymer"},
            {"block_rubber", "ingot_rubber"},
            {"block_waste", "nuclear_waste"},
    };

    /**
     * {billet output id, count, ingredient ids...(count 1 each, repeat an id for >1)} - CE's 9 fuel-
     * blend billet-alloy outputs (MineralRecipes.java:153-178), primary item-based recipe only (each
     * output's 2 additional ore-dict-vararg alternates are correctly dropped, same as
     * {@link #BILLET_SETS}). {@code billet_mox_fuel} has no pure-concrete-item CE recipe at all - both
     * of CE's calls (lines 174-175) mix a concrete item with an {@code OreDictManager.DictFrame}
     * accessor ({@code PU239.billet()}/{@code PU239.nugget()}); since every {@code DictFrame} accessor
     * in this file resolves (in CE's own single-mod environment) to exactly CE's own concrete item for
     * that material - confirmed by reading {@code DictFrame}'s method bodies, every one returns a bare
     * ore-dict {@code String}, not an item reference, so the accessor is purely a compatibility layer
     * over CE's real item - {@code PU239.billet()} is translated to {@code billet_pu239} below, the
     * same substitution this table's other rows make implicitly by using concrete items directly.
     */
    private static final Object[][] FUEL_BLEND_BILLETS = {
            {"billet_thorium_fuel", 6, new Object[]{"billet_th232", 5, "billet_u233", 1}},
            {"billet_uranium_fuel", 6, new Object[]{"billet_u238", 5, "billet_u235", 1}},
            {"billet_plutonium_fuel", 3, new Object[]{"billet_u238", 2, "billet_pu_mix", 1}},
            {"billet_americium_fuel", 3, new Object[]{"billet_u238", 2, "billet_am_mix", 1}},
            {"billet_pu_mix", 3, new Object[]{"billet_pu239", 2, "billet_pu240", 1}},
            {"billet_am_mix", 3, new Object[]{"billet_am241", 1, "billet_am242", 2}},
            {"billet_neptunium_fuel", 3, new Object[]{"billet_u238", 2, "billet_neptunium", 1}},
            {"billet_mox_fuel", 3, new Object[]{"billet_uranium_fuel", 2, "billet_pu239", 1}},
            {"billet_schrabidium_fuel", 3, new Object[]{"billet_schrabidium", 1, "billet_neptunium", 1, "billet_beryllium", 1}},
    };

    /**
     * {precursor billet id, be-alloy output id} - CE's 3 Be-alloy billet families
     * (MineralRecipes.java:180-188): the item-based x2/x6 blend recipes only (the ore-dict-nugget x1
     * blends, lines 180-182, are correctly dropped - no item-based equivalent exists for those).
     */
    private static final String[][] BE_ALLOY_BILLETS = {
            {"billet_polonium", "billet_po210be"},
            {"billet_pu238", "billet_pu238be"},
            {"billet_ra226", "billet_ra226be"},
    };

    /**
     * {pellet id, billet id, billet count} - CE's 10-entry RTG pellet family
     * (MineralRecipes.java:202-211): {@code 3x billet + 1x plate_iron -> 1 pellet}, uniform shape
     * except {@code pellet_rtg_weak} (2 different billets, 1 each - handled separately below since it
     * does not fit this table's uniform 3-of-one-billet shape).
     */
    private static final String[][] RTG_PELLETS = {
            {"pellet_rtg", "billet_pu238"},
            {"pellet_rtg_radium", "billet_ra226"},
            {"pellet_rtg_strontium", "billet_sr90"},
            {"pellet_rtg_cobalt", "billet_co60"},
            {"pellet_rtg_actinium", "billet_actinium"},
            {"pellet_rtg_polonium", "billet_polonium"},
            {"pellet_rtg_lead", "billet_pb209"},
            {"pellet_rtg_gold", "billet_au198"},
            {"pellet_rtg_americium", "billet_am241"},
    };

    /**
     * {depleted pellet id, output id, output count} - CE's RTG-depleted "recycling" recipes
     * (MineralRecipes.java:213-218), 5 of CE's 6 (nickel is ore-dict-conditional, correctly dropped).
     * In CE these match an {@code EnumDepletedRTGMaterial} metadata subtype; this port already
     * flattens {@code pellet_rtg_depleted} into 6 separate {@code pellet_rtg_depleted_<material>}
     * items ({@code MachineItems.java}'s {@code RTG_DEPLETED} loop), so each row is a plain
     * single-item shapeless recipe with no component/meta matching needed. CE's own field name
     * {@code ModItems.ingot_mercury} is, per {@code IngotNuggetItems}' own javadoc, registered under
     * the real id {@code nugget_mercury} (a CE field-name/registry-id mismatch), not
     * {@code ingot_mercury} - used correctly below rather than guessed from the field name.
     */
    private static final String[][] RTG_DEPLETED_RECYCLING = {
            {"pellet_rtg_depleted_bismuth", "billet_bismuth", "3"},
            {"pellet_rtg_depleted_lead", "ingot_lead", "2"},
            {"pellet_rtg_depleted_mercury", "nugget_mercury", "2"},
            {"pellet_rtg_depleted_neptunium", "billet_neptunium", "3"},
            {"pellet_rtg_depleted_zirconium", "billet_zirconium", "3"},
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
        for (String[] row : DIRECT_INGOT_NUGGET_SHORTCUTS) {
            onePair(output, "mineral/shortcut_" + row[0], item(row[1]), item(row[2]));
        }
        for (Object[] row : BLOCK_INGOT_SETS) {
            NTMMaterial mat = (NTMMaterial) row[0];
            String ingotId = (String) row[1];
            onePair(output, "mineral/block_" + mat.getRegistryName(), item(MaterialShapes.BLOCK.buildRegistryName(mat)), item(ingotId));
        }
        for (String[] row : BLOCK_INGOT_LEGACY) {
            onePair(output, "mineral/block_" + row[0].substring("block_".length()), item(row[0]), item(row[1]));
        }
        fuelBlendBillets(output);
        beAlloyBillets(output);
        rtgPellets(output);
        rtgDepletedRecycling(output);
        zfbBillets(output);
        eggBalefireFamily(output);
        mineralOneOffs(output);
    }

    /** {@link #FUEL_BLEND_BILLETS}: {@code count x (ingredientId, ingredientCount)...} -> outputId. */
    private void fuelBlendBillets(RecipeOutput output) {
        for (Object[] row : FUEL_BLEND_BILLETS) {
            String outputId = (String) row[0];
            int count = (Integer) row[1];
            Object[] ingredients = (Object[]) row[2];
            ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item(outputId), count);
            Item unlockOn = null;
            for (int i = 0; i < ingredients.length; i += 2) {
                Item ingredient = item((String) ingredients[i]);
                int ingredientCount = (Integer) ingredients[i + 1];
                for (int n = 0; n < ingredientCount; n++) {
                    builder.requires(ingredient);
                }
                unlockOn = ingredient;
            }
            builder.unlockedBy("has_ingredient", has(unlockOn)).save(output, id("mineral/fuelblend_" + outputId));
        }
    }

    /** {@link #BE_ALLOY_BILLETS}: {@code precursor x2 + billet_beryllium x2 -> output x2} and {@code precursor x3 + billet_beryllium x3 -> output x6}. */
    private void beAlloyBillets(RecipeOutput output) {
        Item billetBeryllium = item("billet_beryllium");
        for (String[] row : BE_ALLOY_BILLETS) {
            Item precursor = item(row[0]);
            Item result = item(row[1]);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 2)
                    .requires(precursor)
                    .requires(billetBeryllium)
                    .unlockedBy("has_precursor", has(precursor))
                    .save(output, id("mineral/bealloy_" + row[1] + "_small"));
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 6)
                    .requires(precursor).requires(precursor).requires(precursor)
                    .requires(billetBeryllium).requires(billetBeryllium).requires(billetBeryllium)
                    .unlockedBy("has_precursor", has(precursor))
                    .save(output, id("mineral/bealloy_" + row[1] + "_large"));
        }
    }

    /** {@link #RTG_PELLETS}: {@code billet x3 + plate_iron -> pellet}, plus pellet_rtg_weak's mixed-billet variant. */
    private void rtgPellets(RecipeOutput output) {
        Item plateIron = item("plate_iron");
        for (String[] row : RTG_PELLETS) {
            Item billet = item(row[1]);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item(row[0]))
                    .requires(billet).requires(billet).requires(billet)
                    .requires(plateIron)
                    .unlockedBy("has_billet", has(billet))
                    .save(output, id("mineral/rtg_" + row[0]));
        }
        // MineralRecipes.java:204: pellet_rtg_weak <- billet_u238 x2 + billet_pu238 x1 + plate_iron.
        Item billetU238 = item("billet_u238");
        Item billetPu238 = item("billet_pu238");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("pellet_rtg_weak"))
                .requires(billetU238).requires(billetU238)
                .requires(billetPu238)
                .requires(plateIron)
                .unlockedBy("has_billet", has(billetU238))
                .save(output, id("mineral/rtg_pellet_rtg_weak"));
    }

    /** {@link #RTG_DEPLETED_RECYCLING}: {@code depleted pellet -> output}, one-way (matches CE - no reverse). */
    private void rtgDepletedRecycling(RecipeOutput output) {
        for (String[] row : RTG_DEPLETED_RECYCLING) {
            Item depleted = item(row[0]);
            Item result = item(row[1]);
            int count = Integer.parseInt(row[2]);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, count)
                    .requires(depleted)
                    .unlockedBy("has_depleted", has(depleted))
                    .save(output, id("mineral/rtg_recycle_" + row[0]));
        }
    }

    /**
     * MineralRecipes.java:190-195: the ZFB billet family. 2 of CE's 3 outputs are ready -
     * {@code billet_zfb_bismuth}/{@code billet_zfb_pu241}, both nugget-tier (x1) and billet-tier (x6)
     * recipes, all ingredients real registered items. The 3rd, {@code billet_zfb_am_mix}, is NOT
     * ported: its recipe needs {@code AMRG} ("AmericiumRG" per {@code OreDictManager.java:121}, a
     * distinct material from {@code am_mix}/{@code Mats.MAT_RGA}), which has no registered nugget or
     * billet item under any name in this port (confirmed by grep - only am241/am242/am_mix exist).
     * <b>Correction to the research report</b>, which marked the whole ZFB family blocked ("billet_zfb_
     * bismuth output not registered") - {@code BilletPowderItems.java} lines 117-119 show all 3 ZFB
     * billet items (bismuth/pu241/am_mix) are in fact already registered; only the am_mix recipe's
     * ingredient (AMRG) is actually missing, not the output.
     */
    private void zfbBillets(RecipeOutput output) {
        Item nuggetZirconium = item("nugget_zirconium");
        Item billetZirconium = item("billet_zirconium");
        Item zfbBismuth = item("billet_zfb_bismuth");
        Item zfbPu241 = item("billet_zfb_pu241");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, zfbBismuth)
                .requires(nuggetZirconium).requires(nuggetZirconium).requires(nuggetZirconium)
                .requires(item("nugget_uranium"))
                .requires(item("nugget_pu241"))
                .requires(item("nugget_bismuth"))
                .unlockedBy("has_nugget", has(nuggetZirconium))
                .save(output, id("mineral/zfb_bismuth_nugget"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, zfbBismuth, 6)
                .requires(billetZirconium).requires(billetZirconium).requires(billetZirconium)
                .requires(item("billet_uranium"))
                .requires(item("billet_pu241"))
                .requires(item("billet_bismuth"))
                .unlockedBy("has_billet", has(billetZirconium))
                .save(output, id("mineral/zfb_bismuth_billet"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, zfbPu241)
                .requires(nuggetZirconium).requires(nuggetZirconium).requires(nuggetZirconium)
                .requires(item("nugget_u235"))
                .requires(item("nugget_pu240"))
                .requires(item("nugget_pu241"))
                .unlockedBy("has_nugget", has(nuggetZirconium))
                .save(output, id("mineral/zfb_pu241_nugget"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, zfbPu241, 6)
                .requires(billetZirconium).requires(billetZirconium).requires(billetZirconium)
                .requires(item("billet_u235"))
                .requires(item("billet_pu240"))
                .requires(item("billet_pu241"))
                .unlockedBy("has_billet", has(billetZirconium))
                .save(output, id("mineral/zfb_pu241_billet"));
    }

    /**
     * MineralRecipes.java:394-395/403-404: the egg-balefire family, 4 recipes, every item already
     * real - a pure oversight-class gap per the research report.
     */
    private void eggBalefireFamily(RecipeOutput output) {
        onePair(output, "mineral/egg_balefire", item("egg_balefire"), item("egg_balefire_shard"));

        // MineralRecipes.java:403: egg_balefire_shard <- 4x powder_balefire ("##","##").
        Item powderBalefire = item("powder_balefire");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("egg_balefire_shard"))
                .pattern("##")
                .pattern("##")
                .define('#', powderBalefire)
                .unlockedBy("has_powder", has(powderBalefire))
                .save(output, id("mineral/egg_balefire_shard_from_powder"));

        // MineralRecipes.java:404: add9To1(cell_balefire, egg_balefire_shard) - 9:1 one-way only, no reverse.
        Item eggBalefireShard = item("egg_balefire_shard");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("cell_balefire"))
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', eggBalefireShard)
                .unlockedBy("has_shard", has(eggBalefireShard))
                .save(output, id("mineral/cell_balefire_from_shard"));
    }

    /**
     * The remaining self-contained, non-table-shaped {@code MineralRecipes.java} entries whose every
     * item is already real:
     * <ul>
     *     <li>MineralRecipes.java:245: {@code block_schrabidium_cluster}, a special 3x3 shaped recipe
     *     mixing 3 different ingots (compress-only, no CE reverse).</li>
     *     <li>MineralRecipes.java:252: {@code block_meteor_cobble}, 4x {@code fragment_meteorite} in a
     *     2x2 pattern (compress-only, no CE reverse - a deliberate one-way debris sink).</li>
     *     <li>MineralRecipes.java:253: {@code block_meteor_broken}, 9x {@code fragment_meteorite} in a
     *     3x3 pattern (compress-only, no CE reverse, same reasoning).</li>
     * </ul>
     */
    private void mineralOneOffs(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("block_schrabidium_cluster"))
                .pattern("#S#")
                .pattern("SXS")
                .pattern("#S#")
                .define('#', item("ingot_schrabidium"))
                .define('S', item("ingot_starmetal"))
                .define('X', item("ingot_schrabidate"))
                .unlockedBy("has_ingot", has(item("ingot_schrabidium")))
                .save(output, id("mineral/block_schrabidium_cluster"));

        // CE MineralRecipes.java: block_euphemium 3x3 compress/decompress
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, item("block_euphemium"))
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', item("ingot_euphemium"))
                .unlockedBy("has_ingot", has(item("ingot_euphemium")))
                .save(output, id("mineral/block_euphemium_many_from_one"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("ingot_euphemium"), 9)
                .requires(item("block_euphemium"))
                .unlockedBy("has_block", has(item("block_euphemium")))
                .save(output, id("mineral/block_euphemium_one_from_many"));

        Item fragmentMeteorite = item("fragment_meteorite");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("block_meteor_cobble"))
                .pattern("##")
                .pattern("##")
                .define('#', fragmentMeteorite)
                .unlockedBy("has_fragment", has(fragmentMeteorite))
                .save(output, id("mineral/block_meteor_cobble"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("block_meteor_broken"))
                .pattern("###")
                .pattern("###")
                .pattern("###")
                .define('#', fragmentMeteorite)
                .unlockedBy("has_fragment", has(fragmentMeteorite))
                .save(output, id("mineral/block_meteor_broken"));
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

        // ArmorRecipes.java:22-25: titanium_helmet/_plate/_legs/_boots (addHelmet/addChest/addLegs/
        // addBoots TI.ingot()). Standard vanilla armor patterns.
        Item ingotTitanium = item("ingot_titanium");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("titanium_helmet"))
                .pattern("XXX").pattern("X X")
                .define('X', ingotTitanium)
                .unlockedBy("has_ingot", has(ingotTitanium))
                .save(output, id("armor/titanium_helmet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("titanium_plate"))
                .pattern("X X").pattern("XXX").pattern("XXX")
                .define('X', ingotTitanium)
                .unlockedBy("has_ingot", has(ingotTitanium))
                .save(output, id("armor/titanium_plate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("titanium_legs"))
                .pattern("XXX").pattern("X X").pattern("X X")
                .define('X', ingotTitanium)
                .unlockedBy("has_ingot", has(ingotTitanium))
                .save(output, id("armor/titanium_legs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("titanium_boots"))
                .pattern("X X").pattern("X X")
                .define('X', ingotTitanium)
                .unlockedBy("has_ingot", has(ingotTitanium))
                .save(output, id("armor/titanium_boots"));

        // ArmorRecipes.java:163: mask_of_infamy, 7x plate_iron.
        Item plateIron = item("plate_iron");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("mask_of_infamy"))
                .pattern("III").pattern("III").pattern(" I ")
                .define('I', plateIron)
                .unlockedBy("has_plate", has(plateIron))
                .save(output, id("armor/mask_of_infamy"));

        // ArmorRecipes.java:158-162: masks. mask_rag craft lives in ce_craft (rag_damp is now a
        // real ItemBase). mask_piss output is ArmorModel helmet, not registered. ashglasses skipped
        // (output unregistered). goggles is ce_craft/armor/goggles.json.
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

        // ArmorRecipes.java:95-98: rpa_helmet/_plate/_legs/_boots. All use TIER2 parts_legendary.
        Item plateKevlar = item("plate_kevlar");
        Item plateArmorAjr = item("plate_armor_ajr");
        Item partsLegendaryTier2 = item("parts_legendary_tier2");
        Item motorDesh = item("motor_desh");
        Item gasMaskFilterCombo = item("gas_mask_filter_combo");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("rpa_helmet"))
                .pattern("KPK").pattern("PLP").pattern(" F ")
                .define('K', plateKevlar)
                .define('P', plateArmorAjr)
                .define('L', partsLegendaryTier2)
                .define('F', gasMaskFilterCombo)
                .unlockedBy("has_parts_legendary", has(partsLegendaryTier2))
                .save(output, id("armor/rpa_helmet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("rpa_plate"))
                .pattern("P P").pattern("MLM").pattern("PKP")
                .define('P', plateArmorAjr)
                .define('M', motorDesh)
                .define('L', partsLegendaryTier2)
                .define('K', plateKevlar)
                .unlockedBy("has_parts_legendary", has(partsLegendaryTier2))
                .save(output, id("armor/rpa_plate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("rpa_legs"))
                .pattern("MPM").pattern("KLK").pattern("P P")
                .define('M', motorDesh)
                .define('P', plateArmorAjr)
                .define('K', plateKevlar)
                .define('L', partsLegendaryTier2)
                .unlockedBy("has_parts_legendary", has(partsLegendaryTier2))
                .save(output, id("armor/rpa_legs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("rpa_boots"))
                .pattern("KLK").pattern("P P")
                .define('K', plateKevlar)
                .define('L', partsLegendaryTier2)
                .define('P', plateArmorAjr)
                .unlockedBy("has_parts_legendary", has(partsLegendaryTier2))
                .save(output, id("armor/rpa_boots"));

        // ArmorRecipes.java:70-73: ajro_* (dyed ajr_*). ajr_* itself has no CE recipe in this pass
        // either (needs titanium_helmet/_plate/_legs/_boots, none of which exist in this port - see
        // class javadoc) but is, again, a real registered item, valid as an ingredient here.
        ajro(output, "ajro_helmet", "ajr_helmet");
        ajro(output, "ajro_plate", "ajr_plate");
        ajro(output, "ajro_legs", "ajr_legs");
        ajro(output, "ajro_boots", "ajr_boots");

        // ArmorRecipes.java:143-146: hazmat_paa_helmet/_plate/_legs/_boots, E=ModItems.plate_paa,
        // I=KEY_ANYPANE, P=IRON.plate(). New finding from docs/phase7/crafting_tools_armor_smelting.md,
        // not in ModRecipeProvider before this class.
        //
        // CORRECTION to the research report: the report also marked CE's 20-recipe hazmat/asbestos
        // family (hazmat_helmet/_plate/_legs/_boots + _red/_grey variants, asbestos_helmet/_plate/
        // _legs/_boots - ArmorRecipes.java:127-142) as ready, citing "hazmat_cloth/_red/_grey and
        // asbestos_cloth all confirmed real registered items". Re-checked here: those 4 strings only
        // ever appear as the *tag-id suffix* argument to MaterialRegistry.repairTag(String) (e.g.
        // repairTag("hazmat_cloth") builds the TAG id hbm:repair/hazmat_cloth, used only for tool-
        // repair matching), never as an actual `ModItems.ITEMS.register(...)` call - repo-wide grep
        // for "cloth" under com.hbm.items finds zero item registrations. So hazmat_cloth/_red/_grey/
        // asbestos_cloth are NOT real items in this port (unlike hazmat_paa's own ingredient,
        // ModItems.plate_paa, which the report also cited and which IS independently confirmed real -
        // PlateCrystalWasteItems.java:153). Those 20 recipes stay blocked until a future items pass
        // adds the missing cloth item family; only the 4 hazmat_paa recipes are actually portable now.
        Item platePaa = item("plate_paa");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("hazmat_paa_helmet"))
                .pattern("EEE")
                .pattern("IEI")
                .pattern(" P ")
                .define('E', platePaa)
                .define('I', GLASS_PANES)
                .define('P', plateIron)
                .unlockedBy("has_plate_paa", has(platePaa))
                .save(output, id("armor/hazmat_paa_helmet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("hazmat_paa_plate"))
                .pattern("E E")
                .pattern("EEE")
                .pattern("EEE")
                .define('E', platePaa)
                .unlockedBy("has_plate_paa", has(platePaa))
                .save(output, id("armor/hazmat_paa_plate"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("hazmat_paa_legs"))
                .pattern("EEE")
                .pattern("E E")
                .pattern("E E")
                .define('E', platePaa)
                .unlockedBy("has_plate_paa", has(platePaa))
                .save(output, id("armor/hazmat_paa_legs"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("hazmat_paa_boots"))
                .pattern("E E")
                .pattern("E E")
                .define('E', platePaa)
                .unlockedBy("has_plate_paa", has(platePaa))
                .save(output, id("armor/hazmat_paa_boots"));
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
    // Part 4: SmeltingRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/SmeltingRecipes.java
    // ================================================================================================

    /**
     * {ore/powder/etc input id, output id, output count, experience} - the uniform-shape majority of
     * CE's {@code SmeltingRecipes.AddSmeltingRec()} (132 call sites -> 141 individual recipes): every
     * metal-ore/meteor-ore/gneiss-ore -> ingot pair, every {@code powder_x -> ingot_x} pair (41 in CE,
     * 40 here - see the lithium correction below), the 4 {@code arc_electrode_burnt_*} -> ingot pairs,
     * australium, schraranium, 3 gravel/waste entries and the mineral-crystal -> base-resource family.
     * An input or output id prefixed {@code "minecraft:"} resolves against the vanilla registry
     * instead of {@code hbm:} (see {@link #resolveItem(String)}) - CE smelts several ores/crystals
     * directly into vanilla iron/gold ingots, redstone, diamond, gravel and cobblestone with no hbm
     * equivalent item of their own.
     * <p>
     * Every id below was individually re-confirmed registered in this port while writing this table
     * (not re-quoted from the research report on faith) - three real corrections came out of that
     * re-check, all to entries the report marked ready:
     * <ul>
     *     <li>{@code ore_gneiss_lithium -> ModItems.lithium} (SmeltingRecipes.java:53),
     *     {@code powder_lithium -> ModItems.lithium} (line 84) and {@code crystal_lithium ->
     *     ModItems.lithium} (line 154): CE's {@code ModItems.lithium} is a bare resource item,
     *     distinct from {@code powder_lithium}/{@code powder_lithium_tiny} (which do exist here, see
     *     {@link #ONE_TO_NINE_PAIRS}) - a plain {@code hbm:lithium} item does not exist anywhere in
     *     this port (0 grep hits outside of {@code pile_rod_lithium}, an unrelated control-rod item).
     *     Same root cause the report itself already correctly identified for
     *     {@code crystal_sulfur/_niter/_fluorite/_cinnabar} - lithium needed the identical treatment
     *     but was missed. All 3 are skipped here, not guessed around.</li>
     * </ul>
     * See the class javadoc's "Explicitly not attempted" list additions (this task) for why the
     * remaining ~27 CE entries and the count>1-shaped subset are handled the way they are.
     */
    private static final String[][] SMELTING_SIMPLE = {
            // Misc (SmeltingRecipes.java:22)
            {"glyphid_meat", "glyphid_meat_grilled", "1", "1.0"},

            // Metal ores -> ingots (SmeltingRecipes.java:24-40). ore_aluminium -> chunk_ore skipped -
            // ModItems.chunk_ore (any EnumChunkType) is not registered anywhere in this port.
            {"ore_thorium", "ingot_th232", "1", "3.0"},
            {"ore_uranium", "ingot_uranium", "1", "6.0"},
            {"ore_uranium_scorched", "ingot_uranium", "1", "6.0"},
            {"ore_nether_uranium", "ingot_uranium", "1", "12.0"},
            {"ore_nether_uranium_scorched", "ingot_uranium", "1", "12.0"},
            {"ore_nether_plutonium", "ingot_plutonium", "1", "24.0"},
            {"ore_titanium", "ingot_titanium", "1", "3.0"},
            {"ore_copper", "ingot_copper", "1", "2.5"},
            {"ore_tungsten", "ingot_tungsten", "1", "6.0"},
            {"ore_nether_tungsten", "ingot_tungsten", "1", "12.0"},
            {"ore_lead", "ingot_lead", "1", "3.0"},
            {"ore_beryllium", "ingot_beryllium", "1", "2.0"},
            {"ore_schrabidium", "ingot_schrabidium", "1", "128.0"},
            {"ore_nether_schrabidium", "ingot_schrabidium", "1", "256.0"},
            {"ore_cobalt", "ingot_cobalt", "1", "2.0"},
            {"ore_nether_cobalt", "ingot_cobalt", "1", "2.0"},

            // Meteor ore (SmeltingRecipes.java:42-46; ready subset only - block_meteor_ore_aluminium/
            // _rareearth both need the same unregistered chunk_ore family as ore_aluminium above).
            // This port flattens CE's ore_meteor+EnumMeteorType into 5 per-variant blocks
            // (GenericBlocks.java's registerMeteorOre()).
            {"block_meteor_ore_iron", "minecraft:iron_ingot", "16", "10.0"},
            {"block_meteor_ore_copper", "ingot_copper", "16", "10.0"},
            {"block_meteor_ore_cobalt", "ingot_cobalt", "4", "10.0"},

            // Gneiss ore (SmeltingRecipes.java:48-54; minus ore_gneiss_lithium, see class javadoc).
            {"ore_gneiss_iron", "minecraft:iron_ingot", "1", "5.0"},
            {"ore_gneiss_gold", "minecraft:gold_ingot", "1", "5.0"},
            {"ore_gneiss_uranium", "ingot_uranium", "1", "12.0"},
            {"ore_gneiss_uranium_scorched", "ingot_uranium", "1", "12.0"},
            {"ore_gneiss_copper", "ingot_copper", "1", "5.0"},
            {"ore_gneiss_schrabidium", "ingot_schrabidium", "1", "256.0"},

            // Australium (SmeltingRecipes.java:56-57)
            {"ore_australium", "nugget_australium", "1", "2.5"},
            {"powder_australium", "ingot_australium", "1", "5.0"},

            // Powder -> ingot (SmeltingRecipes.java:63-103; minus powder_lithium, see class javadoc).
            // 40 of CE's 41 powder_x -> ingot_x calls - the single largest ready-now block in the file.
            {"powder_lead", "ingot_lead", "1", "1.0"},
            {"powder_neptunium", "ingot_neptunium", "1", "1.0"},
            {"powder_polonium", "ingot_polonium", "1", "1.0"},
            {"powder_schrabidium", "ingot_schrabidium", "1", "5.0"},
            {"powder_schrabidate", "ingot_schrabidate", "1", "5.0"},
            {"powder_euphemium", "ingot_euphemium", "1", "10.0"},
            {"powder_aluminium", "ingot_aluminium", "1", "1.0"},
            {"powder_beryllium", "ingot_beryllium", "1", "1.0"},
            {"powder_copper", "ingot_copper", "1", "1.0"},
            {"powder_gold", "minecraft:gold_ingot", "1", "1.0"},
            {"powder_iron", "minecraft:iron_ingot", "1", "1.0"},
            {"powder_titanium", "ingot_titanium", "1", "1.0"},
            {"powder_cobalt", "ingot_cobalt", "1", "1.0"},
            {"powder_tungsten", "ingot_tungsten", "1", "1.0"},
            {"powder_uranium", "ingot_uranium", "1", "1.0"},
            {"powder_thorium", "ingot_th232", "1", "1.0"},
            {"powder_plutonium", "ingot_plutonium", "1", "1.0"},
            {"powder_combine_steel", "ingot_combine_steel", "1", "1.0"},
            {"powder_magnetized_tungsten", "ingot_magnetized_tungsten", "1", "1.0"},
            {"powder_red_copper", "ingot_red_copper", "1", "1.0"},
            {"powder_steel", "ingot_steel", "1", "1.0"},
            {"powder_dura_steel", "ingot_dura_steel", "1", "1.0"},
            {"powder_polymer", "ingot_polymer", "1", "1.0"},
            {"powder_bakelite", "ingot_bakelite", "1", "1.0"},
            {"powder_lanthanium", "ingot_lanthanium", "1", "1.0"},
            {"powder_actinium", "ingot_actinium", "1", "1.0"},
            {"powder_boron", "ingot_boron", "1", "1.0"},
            {"powder_desh", "ingot_desh", "1", "1.0"},
            {"powder_dineutronium", "ingot_dineutronium", "1", "5.0"},
            {"powder_asbestos", "ingot_asbestos", "1", "1.0"},
            {"powder_zirconium", "ingot_zirconium", "1", "1.0"},
            {"powder_tcalloy", "ingot_tcalloy", "1", "1.0"},
            {"powder_au198", "ingot_au198", "1", "1.0"},
            {"powder_sr90", "ingot_sr90", "1", "1.0"},
            {"powder_ra226", "ingot_ra226", "1", "1.0"},
            {"powder_tantalium", "ingot_tantalium", "1", "1.0"},
            {"powder_niobium", "ingot_niobium", "1", "1.0"},
            {"powder_bismuth", "ingot_bismuth", "1", "1.0"},
            {"powder_calcium", "ingot_calcium", "1", "1.0"},
            {"powder_cadmium", "ingot_cadmium", "1", "1.0"},

            // Arc electrode burnt -> ingot (SmeltingRecipes.java:106-109). This port flattens CE's
            // single arc_electrode_burnt field (4 metadata grades) into 4 per-variant ids
            // (MachineItems.java's registerArcElectrodes()).
            {"arc_electrode_burnt_graphite", "ingot_graphite", "1", "3.0"},
            {"arc_electrode_burnt_lanthanium", "ingot_lanthanium", "1", "3.0"},
            {"arc_electrode_burnt_desh", "ingot_desh", "1", "3.0"},
            {"arc_electrode_burnt_saturnite", "ingot_saturnite", "1", "3.0"},

            // Schraranium (SmeltingRecipes.java:131)
            {"ingot_schraranium", "nugget_schrabidium", "1", "2.0"},

            // Gravel/waste (SmeltingRecipes.java:119, 124-125; gravel_obsidian/gravel_diamond/
            // sand_uranium/_polonium/_boron/_lead/ash_digamma/basalt all need blocks not registered
            // anywhere in this port).
            {"minecraft:gravel", "minecraft:cobblestone", "1", "0.0"},
            {"waste_trinitite", "glass_trinitite", "1", "0.25"},
            {"waste_trinitite_red", "glass_trinitite", "1", "0.25"},

            // Mineral crystals -> base resource (SmeltingRecipes.java:134-159; minus crystal_sulfur/
            // _niter/_fluorite/_cinnabar - the plain sulfur/niter/fluorite/cinnabar resource items are
            // not registered, distinct from crystal_x themselves and the ore_x world-gen blocks, both
            // of which do exist - and crystal_lithium, see class javadoc).
            {"crystal_iron", "minecraft:iron_ingot", "2", "2.0"},
            {"crystal_gold", "minecraft:gold_ingot", "2", "2.0"},
            {"crystal_redstone", "minecraft:redstone", "6", "2.0"},
            {"crystal_diamond", "minecraft:diamond", "2", "2.0"},
            {"crystal_uranium", "ingot_uranium", "2", "2.0"},
            {"crystal_thorium", "ingot_th232", "2", "2.0"},
            {"crystal_plutonium", "ingot_plutonium", "2", "2.0"},
            {"crystal_titanium", "ingot_titanium", "2", "2.0"},
            {"crystal_copper", "ingot_copper", "2", "2.0"},
            {"crystal_tungsten", "ingot_tungsten", "2", "2.0"},
            {"crystal_aluminium", "ingot_aluminium", "2", "2.0"},
            {"crystal_beryllium", "ingot_beryllium", "2", "2.0"},
            {"crystal_lead", "ingot_lead", "2", "2.0"},
            {"crystal_schraranium", "nugget_schrabidium", "2", "2.0"},
            {"crystal_schrabidium", "ingot_schrabidium", "2", "2.0"},
            {"crystal_rare", "powder_desh_mix", "1", "2.0"},
            {"crystal_phosphorus", "powder_fire", "6", "2.0"},
            {"crystal_cobalt", "ingot_cobalt", "2", "2.0"},
            {"crystal_starmetal", "ingot_starmetal", "2", "2.0"},
            {"crystal_trixite", "ingot_plutonium", "4", "2.0"},
            {"crystal_osmiridium", "ingot_osmiridium", "1", "2.0"},
    };

    /**
     * CE's {@code scrap_plastic} (SmeltingRecipes.java:167) is a single {@code ItemStack(scrap_plastic,
     * 1, OreDictionary.WILDCARD_VALUE)} wildcard-metadata smelting recipe - one recipe matching all of
     * CE's plastic-scrap metadata variants at once. This port flattens that single item into 23 plain
     * {@code plastic_scrap_<type>} ids (see {@code SpecialItems.ScrapType}, {@code SpecialItems.java}),
     * so the single CE call site expands into 23 discrete recipes here, all -> {@code ingot_polymer}.
     */
    private static final String[] PLASTIC_SCRAP_TYPES = {
            "board_blank", "board_transistor", "board_converter",
            "bridge_north", "bridge_south", "bridge_io", "bridge_bus", "bridge_chipset", "bridge_cmos", "bridge_bios",
            "cpu_register", "cpu_clock", "cpu_logic", "cpu_cache", "cpu_ext", "cpu_socket",
            "mem_socket", "mem_16k_a", "mem_16k_b", "mem_16k_c", "mem_16k_d",
            "card_board", "card_processor",
    };

    private void smeltingRecipes(RecipeOutput output) {
        for (String[] row : SMELTING_SIMPLE) {
            Item input = resolveItem(row[0]);
            Item resultItem = resolveItem(row[1]);
            int count = Integer.parseInt(row[2]);
            float xp = Float.parseFloat(row[3]);
            String pathSafeId = row[0].replace(':', '_');
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(input), RecipeCategory.MISC, new ItemStack(resultItem, count), xp, 200)
                    .unlockedBy("has_input", has(input))
                    .save(output, id("smelting/" + pathSafeId));
        }

        Item ingotPolymer = item("ingot_polymer");
        for (String scrapType : PLASTIC_SCRAP_TYPES) {
            Item scrap = item("plastic_scrap_" + scrapType);
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(scrap), RecipeCategory.MISC, new ItemStack(ingotPolymer, 1), 0.1F, 200)
                    .unlockedBy("has_scrap", has(scrap))
                    .save(output, id("smelting/plastic_scrap_" + scrapType));
        }

        // SmeltingRecipes.java:161-163: ItemHot.heatUp self-smelts. Only the 3 entries whose item is
        // actually ItemHot-backed (and so actually consumes the resulting hbm:heat component,
        // ItemHot.java's inventoryTick) are ported - blade_meteorite/meteorite_sword (lines 164-165)
        // need items this port doesn't have at all (see class javadoc), and the 10-entry
        // ingot_steel_dusted loop (lines 169-170) is deliberately NOT ported here even though every
        // id it needs already exists: IngotNuggetItems.java's own javadoc documents
        // ingot_steel_dusted as a plain Item with "heat mechanic deferred... no ItemHotDusted port
        // exists yet" - tagging a component that item class doesn't read or decay would be inert data,
        // not a working port of CE's mechanic, so it is left for whichever future pass ports
        // ItemHotDusted alongside it (a correction to the research report, which counted this loop as
        // ready).
        //
        // NeoForge extends SimpleCookingRecipeBuilder.smelting(...) with an ItemStack-result overload
        // (net/minecraft/data/recipes/SimpleCookingRecipeBuilder.java.patch, "Neo: add stack result
        // support") specifically so a recipe result can carry data components - used here instead of
        // hand-authoring raw recipe JSON to attach hbm:heat, avoiding exactly the JSON-schema guess
        // this task's own ground rules warn against.
        heatSelfSmelt(output, "ingot_chainsteel", 100);
        heatSelfSmelt(output, "ingot_meteorite", 200);
        heatSelfSmelt(output, "ingot_meteorite_forged", 200);
    }

    private static void heatSelfSmelt(RecipeOutput output, String path, int maxHeat) {
        Item i = item(path);
        ItemStack result = new ItemStack(i, 1);
        result.set(SpecialItemComponents.HEAT.get(), maxHeat);
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(i), RecipeCategory.MISC, result, 0.0F, 200)
                .unlockedBy("has_input", has(i))
                .save(output, id("smelting/" + path + "_heat_up"));
    }

    // ================================================================================================
    // Part 5: PowderRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/PowderRecipes.java
    // (docs/phase7/crafting_minerals_powder_exclusive.md - previously completely untouched by this port)
    // ================================================================================================

    /**
     * Every {@code OreDictManager.DictFrame} accessor CE calls throughout {@code PowderRecipes.java}
     * (e.g. {@code S.dust()}, {@code KNO.dust()}, {@code IRON.dust()}) returns a bare ore-dict
     * {@code String}, never a concrete item reference (confirmed by reading every {@code DictFrame}
     * shape-accessor method body in {@code OreDictManager.java} - all return {@code String}) - so, as
     * with {@link #FUEL_BLEND_BILLETS}, every ingredient below is CE's own concrete
     * {@code powder_<material>} item for that accessor's underlying material (matched by
     * {@code OreDictManager.java}'s own {@code DictFrame} name against
     * {@code com.hbm.items.BilletPowderItems}' real registered ids), not a guess. Two CE ore-dict
     * materials genuinely have no registered dust item anywhere in this port and are skipped
     * accordingly, not guessed around: {@code S} ("Sulfur") and {@code KNO} ("Saltpeter") - this
     * blocks the entire Gunpowder cluster and 2 of 7 Flux-tier recipes, a correction to the research
     * report, which marked Gunpowder ready on the strength of a {@link MaterialShapes#DUST} common
     * tag existing in principle; since {@link MaterialShapes#DUST} is not one of
     * {@code com.hbm.items.MaterialItemGenerator}'s 17 auto-generated shapes, that tag is only ever
     * populated by {@code com.hbm.items.datagen.ModItemTagProvider#addLegacyMaterialTags()} finding a
     * real {@code powder_<material>} item to add to it - and no such item exists for sulfur or
     * saltpeter, so the tag would resolve empty at runtime (a permanently uncraftable recipe), not
     * missing at datagen time. Using the concrete {@code item(String)} lookup here (which throws
     * immediately if wrong) rather than a tag-based {@link Ingredient} avoids exactly that silent
     * failure mode for every other row.
     */
    private void powderRecipes(RecipeOutput output) {
        explosivesAndOther(output);
        powderBlends(output);
        powderFlux(output);
        dyeBlends(output);

        // Not ported, and why (every case individually re-checked against this port's real item
        // registrations, not assumed from the research report):
        // - Explosives: ballistite/ball_dynamite/ball_tnt/solid_fuel/cordite (whole conventional-
        //   explosives sub-family, confirmed absent anywhere in this port) and powder_semtex_mix
        //   (both recipes need solid_fuel/cordite/ballistite plus KNO.dust(), all missing). ball_tnt
        //   and ingot_c4 additionally need Fluids.AROMATICS/UNSATURATEDS as a full-1000mB-container
        //   crafting ingredient - NeoForge/vanilla recipes have no native "any filled fluid container"
        //   Ingredient; would need a custom Ingredient built on this port's FluidContainerRegistry, out
        //   of this task's plain-recipe scope even where the output item exists (ingot_c4).
        // - Other: powder_bakelite needs the same fluid-container Ingredient (Fluids.AROMATICS +
        //   Fluids.PETROLEUM), same reasoning.
        // - Gunpowder: all 4 calls blocked, see this method's own javadoc (S/KNO dust missing).
        // - Metal powders (ItemScraps.create(MaterialStack, ...)): this port's ItemScraps is a
        //   one-item-per-material family (scrap_<material>) carrying its amount/liquid state as
        //   DataComponents (com.hbm.items.machine.ItemScraps.create(ItemStack, int, boolean)), not
        //   CE's single polymetadata item - a recipe result needs to carry those components, which is
        //   past what a plain ShapedRecipeBuilder/ShapelessRecipeBuilder result expresses; flagged by
        //   the research report as unverified and confirmed here to need real custom-result plumbing,
        //   not a missing-item block - left for a future pass, not guessed at.
        // - Fertilizer: both recipes blocked - the primary needs KNO.dust()+S.dust() (both missing);
        //   the ANY_ASH.any() alternate is a pure ore-dict wildcard with no port equivalent, correctly
        //   out of scope regardless (matches this port's own dropped-ore-dict-vararg precedent).
        // - LBSM-gated cluster (powder_red_copper/powder_dura_steel x4/ingot_firebrick,
        //   config-gated behind GeneralConfig.enableLBSM && enableLBSMSimpleCrafting): CE's own
        //   default has this off, matching the established precedent this class already set for the
        //   LBSM-gated starmetal/cobalt_decorated tool branch - skipped as faithful to CE's real
        //   default, not a scope cut.
        // - Crayon (16-color loop): ModItems.crayon/ItemCrayon confirmed absent anywhere in this port
        //   (2 separate code comments elsewhere already name this gap).
    }

    /**
     * PowderRecipes.java:31-33/36: the 2 ready Explosives-cluster entries plus {@code Other}'s
     * {@code ingot_steel_dusted}. CE's {@code new ItemStack(ModItems.ingot_steel_dusted, 1)} has no
     * explicit metadata, i.e. damage/purity 0 - this port's flattened equivalent is
     * {@code ingot_steel_dusted_0} (index 0 of {@code IngotNuggetItems.INGOT_STEEL_DUSTED}'s 10-entry
     * series), the natural translation of CE's implicit default rather than a guess.
     */
    private void explosivesAndOther(RecipeOutput output) {
        // PowderRecipes.java:32: "clay uncrafting because placing and breaking it isn't worth anyone's
        // time" - CE's own comment. Purely vanilla, no port items involved.
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, Items.CLAY_BALL, 4)
                .requires(Items.CLAY)
                .unlockedBy("has_clay", has(Items.CLAY))
                .save(output, id("powder/clay_ball_from_clay"));

        // PowderRecipes.java:33: powder_cement x4 <- LIMESTONE.dust() + 3x CLAY_BALL.
        shapelessBlend(output, "powder/powder_cement", item("powder_cement"), 4,
                item("powder_limestone"), Items.CLAY_BALL, Items.CLAY_BALL, Items.CLAY_BALL);

        // PowderRecipes.java:36: ingot_steel_dusted_0 <- STEEL.ingot() + COAL.dust().
        shapelessBlend(output, "powder/ingot_steel_dusted", item("ingot_steel_dusted_0"), 1,
                item("ingot_steel"), item("powder_coal"));
    }

    /**
     * PowderRecipes.java:46-55: the Blends cluster, 9 CE calls, all 9 confirmed ready - every output
     * and ingredient item independently confirmed registered while writing this method. "dustGlowstone"
     * and "dustQuartz"/{@code NETHERQUARTZ} are CE ore-dict tags with no dedicated hbm item of their
     * own in CE either - they resolve to vanilla {@link Items#GLOWSTONE_DUST}/{@link Items#QUARTZ},
     * used directly rather than guessing at a nonexistent {@code powder_glowstone}/{@code powder_quartz}
     * hbm item.
     */
    private void powderBlends(RecipeOutput output) {
        Item powderPower = item("powder_power");
        shapelessBlend(output, "powder/powder_power", powderPower, 3,
                Items.GLOWSTONE_DUST, item("powder_diamond"), item("powder_magnetized_tungsten"));

        shapelessBlend(output, "powder/powder_nitan_mix_a", item("powder_nitan_mix"), 6,
                item("powder_neptunium"), item("powder_iodine"), item("powder_thorium"),
                item("powder_astatine"), item("powder_neodymium"), item("powder_caesium"));
        shapelessBlend(output, "powder/powder_nitan_mix_b", item("powder_nitan_mix"), 6,
                item("powder_strontium"), item("powder_cobalt"), item("powder_bromine"),
                item("powder_tennessine"), item("powder_niobium"), item("powder_cerium"));

        shapelessBlend(output, "powder/powder_spark_mix", item("powder_spark_mix"), 3,
                item("powder_desh"), item("powder_euphemium"), powderPower);

        shapelessBlend(output, "powder/powder_meteorite", item("powder_meteorite"), 4,
                item("powder_iron"), item("powder_copper"), item("powder_lithium"), Items.QUARTZ);

        Item powderIron = item("powder_iron");
        shapelessBlend(output, "powder/powder_thermite", item("powder_thermite"), 4,
                powderIron, powderIron, powderIron, item("powder_aluminium"));

        Item boronTiny = item("powder_boron_tiny");
        Item lanthaniumTiny = item("powder_lanthanium_tiny");
        shapelessBlend(output, "powder/powder_desh_mix_tiny", item("powder_desh_mix"), 1,
                boronTiny, boronTiny, lanthaniumTiny, lanthaniumTiny,
                item("powder_cerium_tiny"), item("powder_cobalt_tiny"), item("powder_lithium_tiny"),
                item("powder_neodymium_tiny"), item("powder_niobium_tiny"));
        Item boron = item("powder_boron");
        Item lanthanium = item("powder_lanthanium");
        shapelessBlend(output, "powder/powder_desh_mix", item("powder_desh_mix"), 9,
                boron, boron, lanthanium, lanthanium,
                item("powder_cerium"), item("powder_cobalt"), item("powder_lithium"),
                item("powder_neodymium"), item("powder_niobium"));

        Item nuggetMercury = item("nugget_mercury");
        shapelessBlend(output, "powder/powder_desh_ready", item("powder_desh_ready"), 1,
                item("powder_desh_mix"), nuggetMercury, nuggetMercury, item("powder_coal"));
    }

    /**
     * PowderRecipes.java:64-70: the Flux cluster, 5 of CE's 7 tiers ready (F/fluorite and PB+S/lead+
     * sulfur blocked - no {@code powder_fluorite}/{@code powder_sulfur} item exists anywhere in this
     * port). {@code KEY_SAND} (CE's ore-dict sand tag) has no confirmed common-tag equivalent in this
     * port - used directly as vanilla {@link Items#SAND} rather than a guessed tag, matching this
     * class's established "no ore-dict system" simplification.
     */
    private void powderFlux(RecipeOutput output) {
        Item flux = item("powder_flux");
        shapelessBlend(output, "powder/powder_flux_charcoal", flux, 1, Items.CHARCOAL, Items.SAND);
        shapelessBlend(output, "powder/powder_flux_coal", flux, 2, item("powder_coal"), Items.SAND);
        shapelessBlend(output, "powder/powder_flux_limestone", flux, 12, item("powder_limestone"), Items.SAND);
        shapelessBlend(output, "powder/powder_flux_calcium", flux, 12, item("powder_calcium"), Items.SAND);
        shapelessBlend(output, "powder/powder_flux_borax", flux, 16, item("powder_borax"), Items.SAND);
    }

    /**
     * {result color, color A, color B} - PowderRecipes.java:85-95's dye-blend cluster: 11 shapeless
     * 2-color->2 blends (this task's own line-by-line read of the real CE source counts 11 call sites,
     * lines 85 through 95 inclusive - a correction to the research report's "10"). This port already
     * flattens {@code chemical_dye} into 16 separate {@code chemical_dye_<color>} items
     * ({@code MachineItems.java}'s {@code "chemical_dye_" + lower(dye.name())} registration loop,
     * confirmed by direct read), so every id below is that exact naming, not guessed.
     */
    private static final String[][] DYE_BLENDS = {
            {"gray", "black", "white"},
            {"silver", "gray", "white"},
            {"orange", "red", "yellow"},
            {"lime", "green", "white"},
            {"cyan", "blue", "green"},
            {"purple", "red", "blue"},
            {"brown", "orange", "black"},
            {"magenta", "pink", "purple"},
            {"lightblue", "blue", "white"},
            {"pink", "red", "white"},
            {"green", "blue", "yellow"},
    };

    private void dyeBlends(RecipeOutput output) {
        for (String[] row : DYE_BLENDS) {
            Item result = item("chemical_dye_" + row[0]);
            Item a = item("chemical_dye_" + row[1]);
            Item b = item("chemical_dye_" + row[2]);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, 2)
                    .requires(a)
                    .requires(b)
                    .unlockedBy("has_dye", has(a))
                    .save(output, id("powder/dye_" + row[0]));
        }
    }

    /**
     * Generic shapeless-blend helper: {@code result x count <- ingredients...} (repeat an entry for a
     * count &gt;1 of that ingredient, e.g. {@code powderIron, powderIron, powderIron} for a 3x
     * requirement) - accepts a mix of {@link Item} (for vanilla ingredients) and already-resolved
     * {@link Item} results from {@link #item(String)} calls, used throughout {@link #powderBlends}/
     * {@link #powderFlux}/{@link #explosivesAndOther} to avoid re-typing the same
     * {@code ShapelessRecipeBuilder...unlockedBy(...).save(...)} boilerplate for each of CE's many
     * fixed-ingredient-list blend recipes.
     */
    private static void shapelessBlend(RecipeOutput output, String path, Item result, int count, Item... ingredients) {
        ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, result, count);
        for (Item ingredient : ingredients) {
            builder.requires(ingredient);
        }
        builder.unlockedBy("has_ingredient", has(ingredients[0])).save(output, id(path));
    }

    // ================================================================================================
    // Part 6: ExclusiveRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/ExclusiveRecipes.java
    // ================================================================================================
    //
    // Not ported: all 6 of CE's entries are blocked, each independently re-checked against this port's
    // real item/block registrations rather than the research report's own readiness verdicts (which
    // marked 3 of these 6 "ready" - corrected here):
    // - hazmat block <-> hazmat_cloth (2 recipes): "hazmat_cloth" is confirmed NOT a real registered
    //   item anywhere in this port - the only place that string appears in source is as the tag-id
    //   suffix argument to MaterialRegistry.repairTag("hazmat_cloth") (builds the tag id
    //   hbm:repair/hazmat_cloth for tool-repair matching only), never a ModItems.ITEMS.register(...)
    //   call. The "hazmat" block itself does exist (GenericBlocks.java:489), but with no real
    //   hazmat_cloth item to pair it with, both directions are blocked. (This is the same root-cause
    //   correction docs/phase7/crafting_tools_armor_smelting.md's own hazmat/asbestos-armor finding
    //   already made elsewhere in this class - re-confirmed independently here.)
    // - block_niter_reinforced <- ingot_tcalloy + concrete + saltpeter_block (1 recipe): "concrete" as
    //   a bare block id is confirmed NOT registered anywhere in this port (GenericBlocks.java has many
    //   concrete_<color>/concrete_pillar/concrete_super_N/concrete_ext_<type>/brick_concrete variants,
    //   but no plain "concrete" - resolves the research report's own open question on this point).
    // - red_wire_sealed <- red_wire_coated + brick_compound: neither red_wire_sealed nor
    //   red_wire_coated is registered anywhere in this port (brick_compound alone does exist).
    // - fluid_duct_solid <- ingot_steel + plate_aluminium + ducttape (and fluid_duct_solid_sealed,
    //   transitively): fluid_duct_solid and ducttape both confirmed absent.

    // ================================================================================================
    // Part 7: RodRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/RodRecipes.java
    // ================================================================================================
    // docs/phase7/crafting_weapon_rod_consumable.md assignment. Ported here (NOT into
    // com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes, which is a different mechanism - see
    // that class's own javadoc and this class's "Explicitly not attempted" note above, now narrowed to
    // exclude RodRecipes since this section closes that gap).
    //
    // Corrections to docs/phase7/crafting_weapon_rod_consumable.md, found re-deriving every id against
    // this port's real item-registration source rather than trusting the report's own verdicts:
    // - The report's RBMK-fill table names the HEP239 output "rbmk_fuel_hep239" (CE's Java *field*
    //   name, ModItems.rbmk_fuel_hep239). CE's own ItemRBMKRod constructor takes a separate *registry*
    //   name argument, and for this one entry it disagrees with the field name: CE's real registry id
    //   is "rbmk_fuel_hep" (upstream/hbm-ce/.../ModItems.java:2111), which is exactly what this port's
    //   RBMKRods.java also registers (RBMKRods.java:107, "rbmk_fuel_hep"). Used correctly below.
    // - The report placed "9 of 9 ZIRNOX-rod fill recipes" under its "Ready now" heading while its own
    //   prose on the same line says they are "blocked... on the single missing rod_zirnox_empty" - a
    //   contradiction in the report itself. Independently re-confirmed here: {@code rod_zirnox_empty}
    //   is genuinely not registered anywhere in this port (repo-wide grep, zero hits outside a comment
    //   in FluidContainerRegistry.java) - every {@code addZIRNOXRod} fill recipe and the ZIRNOX-empty
    //   housing craft itself are correctly BLOCKED, not ported. (This does not affect the *depleted*
    //   ZIRNOX-rod-to-waste recipes below, a separate CE recipe family that needs no empty housing.)
    // - The report counted the ZIRNOX LITHIUM_FUEL recipe and {@code pile_rod_lithium} as ready because
    //   {@code LI.ingot()} "exists". Re-checked: only {@code powder_lithium}/{@code powder_lithium_tiny}
    //   exist for lithium in this port - a plain {@code ingot_lithium} item is not registered anywhere
    //   (confirmed by the same grep {@link #smeltingRecipes}'s own javadoc already used to drop CE's
    //   lithium smelting entries). Both recipes are BLOCKED here, not ported (ZIRNOX LITHIUM_FUEL is
    //   additionally blocked on {@code rod_zirnox_empty} anyway; {@code pile_rod_lithium} is blocked on
    //   {@code ingot_lithium} alone).
    // - A genuinely new addition the report never enumerated at all: RodRecipes.java's 9
    //   "Zirnox Fuel" depleted-rod-to-waste conversions (RodRecipes.java:36-44, e.g.
    //   {@code rod_zirnox_depleted_natural_uranium_fuel -> waste_natural_uranium x2}) were skipped by
    //   the report's own §3.2 catalog entirely (it only covered the *fresh*-rod {@code addZIRNOXRod}
    //   family). Independently verified here: both the {@code rod_zirnox_depleted_<type>} ingredients
    //   (flattened per-type by {@code MachineItems.registerZirnoxRods()}, distinct from the never-
    //   registered bare {@code rod_zirnox_depleted}) and every {@code waste_<type>} output
    //   ({@code PlateCrystalWasteItems.java}) are real - added below as {@link #ZIRNOX_DEPLETED_TO_WASTE}.
    // - {@code SA326}/{@code SA327} are not two schrabidium isotopes as the report's phrasing implied -
    //   CE's own {@code OreDictManager} defines {@code SA326 = "Schrabidium"} and
    //   {@code SA327 = "Solinium"} (two different materials sharing this recipe corpus's naming
    //   pattern) - resolved here to {@code ingot_schrabidium}/{@code billet_solinium} respectively,
    //   both confirmed real, not flagged as any kind of gap.
    //
    // Every id below (all 32 rbmk_fuel_* outputs + rbmk_fuel_empty, all 15 pwr_fuel_* outputs, all 10
    // watz_pellet_* outputs, 3 of 5 pile_rod_* outputs, icf_pellet_empty, all 9 rod_zirnox_depleted_*
    // ingredients + their waste_* outputs, and every billet/nugget/plate/ingot ingredient) was
    // individually confirmed registered in this port's real item source (BilletPowderItems,
    // IngotNuggetItems, PlateCrystalWasteItems, RBMKItems, RBMKRods, MachineItems) before being used
    // here, via {@link #item(String)} - never guessed from the CE field name.

    /**
     * {output rod id, billet/precursor id} - RodRecipes.java:95-125's {@code addRBMKRod} calls (31 of
     * them; the 32nd, {@code rbmk_fuel_drx}, has a different 2-ingredient shape and is a direct call
     * below). Every fill recipe is shapeless: {@code rbmk_fuel_empty + billet x8 -> output}.
     */
    private static final String[][] RBMK_ROD_FILLS = {
            {"rbmk_fuel_ueu", "billet_uranium"},
            {"rbmk_fuel_meu", "billet_uranium_fuel"},
            {"rbmk_fuel_heu233", "billet_u233"},
            {"rbmk_fuel_heu235", "billet_u235"},
            {"rbmk_fuel_uzh", "billet_uzh"},
            {"rbmk_fuel_thmeu", "billet_thorium_fuel"},
            {"rbmk_fuel_mox", "billet_mox_fuel"},
            {"rbmk_fuel_lep", "billet_plutonium_fuel"},
            {"rbmk_fuel_mep", "billet_pu_mix"},
            {"rbmk_fuel_hep", "billet_pu239"},
            {"rbmk_fuel_hep241", "billet_pu241"},
            {"rbmk_fuel_lea", "billet_americium_fuel"},
            {"rbmk_fuel_mea", "billet_am_mix"},
            {"rbmk_fuel_hea241", "billet_am241"},
            {"rbmk_fuel_hea242", "billet_am242"},
            {"rbmk_fuel_men", "billet_neptunium_fuel"},
            {"rbmk_fuel_hen", "billet_neptunium"},
            {"rbmk_fuel_po210be", "billet_po210be"},
            {"rbmk_fuel_ra226be", "billet_ra226be"},
            {"rbmk_fuel_pu238be", "billet_pu238be"},
            {"rbmk_fuel_leaus", "billet_australium_lesser"},
            {"rbmk_fuel_heaus", "billet_australium_greater"},
            {"rbmk_fuel_balefire", "egg_balefire_shard"},
            {"rbmk_fuel_les", "billet_les"},
            {"rbmk_fuel_mes", "billet_schrabidium_fuel"},
            {"rbmk_fuel_hes", "billet_hes"},
            {"rbmk_fuel_balefire_gold", "billet_balefire_gold"},
            {"rbmk_fuel_flashlead", "billet_flashlead"},
            {"rbmk_fuel_zfb_bismuth", "billet_zfb_bismuth"},
            {"rbmk_fuel_zfb_pu241", "billet_zfb_pu241"},
            {"rbmk_fuel_zfb_am_mix", "billet_zfb_am_mix"},
    };

    /**
     * {watz_pellet_ suffix, ingot/nugget id} - RodRecipes.java:128-137's {@code addPellet} calls (all
     * 10; the GT6-only NQD/NQR pair from {@code registerInit()} is dropped, see class javadoc). Every
     * recipe is shaped {@code " I ","IGI"," I "} (I=ingot, G=ingot_graphite).
     */
    private static final String[][] WATZ_PELLET_SET = {
            {"schrabidium", "ingot_schrabidium"},
            {"hes", "ingot_hes"},
            {"mes", "ingot_schrabidium_fuel"},
            {"les", "ingot_les"},
            {"hen", "ingot_neptunium"},
            {"meu", "ingot_uranium_fuel"},
            {"mep", "ingot_pu_mix"},
            {"lead", "ingot_lead"},
            {"boron", "ingot_boron"},
            {"du", "ingot_u238"},
    };

    /**
     * {pwr_fuel_ suffix, billet id} - 13 of RodRecipes.java:140-152's {@code pwr_fuel} calls sharing the
     * uniform shaped pattern {@code "F","I","F"} (F=billet, I=plate_polymer); the 2 remaining
     * (BFB_AM_MIX/BFB_PU241, a larger nugget-bordered pattern) are direct calls below.
     */
    private static final String[][] PWR_FUEL_SET = {
            {"meu", "billet_uranium_fuel"},
            {"heu233", "billet_u233"},
            {"heu235", "billet_u235"},
            {"men", "billet_neptunium_fuel"},
            {"hen237", "billet_neptunium"},
            {"mox", "billet_mox_fuel"},
            {"mep", "billet_pu_mix"},
            {"hep239", "billet_pu239"},
            {"hep241", "billet_pu241"},
            {"mea", "billet_am_mix"},
            {"hea242", "billet_am242"},
            {"hes326", "billet_schrabidium"},
            {"hes327", "billet_solinium"},
    };

    /**
     * {rod_zirnox_depleted_ ingredient suffix, waste output id} - RodRecipes.java:36-44 (see the class
     * javadoc's discrepancy note - not covered by the research report's own §3.2 catalog at all).
     * Every recipe is shapeless, 1 depleted rod -> 2 waste (CE's own {@code new ItemStack(waste, 2, 1)}
     * - the metadata argument is a vestige of CE's own non-flattened waste item and carries no
     * distinct behavior in {@code ItemDepletedFuel}, so it is dropped here, matching this port's
     * already-flattened, single-variant {@code waste_<type>} items).
     */
    private static final String[][] ZIRNOX_DEPLETED_TO_WASTE = {
            {"rod_zirnox_depleted_natural_uranium_fuel", "waste_natural_uranium"},
            {"rod_zirnox_depleted_uranium_fuel", "waste_uranium"},
            {"rod_zirnox_depleted_thorium_fuel", "waste_thorium"},
            {"rod_zirnox_depleted_mox_fuel", "waste_mox"},
            {"rod_zirnox_depleted_plutonium_fuel", "waste_plutonium"},
            {"rod_zirnox_depleted_u233_fuel", "waste_u233"},
            {"rod_zirnox_depleted_u235_fuel", "waste_u235"},
            {"rod_zirnox_depleted_les_fuel", "waste_schrabidium"},
            {"rod_zirnox_depleted_zfb_mox_fuel", "waste_zfb_mox"},
    };

    private void rodRecipes(RecipeOutput output) {
        // RodRecipes.java:94-125: rbmk_fuel_empty + billet x8 -> output, all shapeless.
        Item rbmkFuelEmpty = item("rbmk_fuel_empty");
        for (String[] row : RBMK_ROD_FILLS) {
            Item billet = item(row[1]);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item(row[0]))
                    .requires(rbmkFuelEmpty)
                    .requires(billet, 8)
                    .unlockedBy("has_empty", has(rbmkFuelEmpty))
                    .save(output, id("rod/" + row[0]));
        }
        // RodRecipes.java:126: rbmk_fuel_drx <- rbmk_fuel_balefire + particle_digamma, shapeless.
        Item rbmkFuelBalefire = item("rbmk_fuel_balefire");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("rbmk_fuel_drx"))
                .requires(rbmkFuelBalefire)
                .requires(item("particle_digamma"))
                .unlockedBy("has_balefire", has(rbmkFuelBalefire))
                .save(output, id("rod/rbmk_fuel_drx"));

        // RodRecipes.java:265-269: addPellet, shaped " I ","IGI"," I ".
        Item ingotGraphite = item("ingot_graphite");
        for (String[] row : WATZ_PELLET_SET) {
            Item ingot = item(row[1]);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("watz_pellet_" + row[0]))
                    .pattern(" I ")
                    .pattern("IGI")
                    .pattern(" I ")
                    .define('I', ingot)
                    .define('G', ingotGraphite)
                    .unlockedBy("has_ingot", has(ingot))
                    .save(output, id("rod/watz_pellet_" + row[0]));
        }

        // RodRecipes.java:140-152: pwr_fuel, shaped "F","I","F".
        Item platePolymer = item("plate_polymer");
        for (String[] row : PWR_FUEL_SET) {
            Item billet = item(row[1]);
            ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("pwr_fuel_" + row[0]))
                    .pattern("F")
                    .pattern("I")
                    .pattern("F")
                    .define('F', billet)
                    .define('I', platePolymer)
                    .unlockedBy("has_billet", has(billet))
                    .save(output, id("rod/pwr_fuel_" + row[0]));
        }
        // RodRecipes.java:153-154: BFB_AM_MIX/BFB_PU241, shaped "NFN","NIN","NBN" (nugget-bordered).
        Item billetBismuth = item("billet_bismuth");
        Item billetAmMix = item("billet_am_mix");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("pwr_fuel_bfb_am_mix"))
                .pattern("NFN")
                .pattern("NIN")
                .pattern("NBN")
                .define('F', billetAmMix)
                .define('I', platePolymer)
                .define('B', billetBismuth)
                .define('N', item("nugget_plutonium_fuel"))
                .unlockedBy("has_billet", has(billetAmMix))
                .save(output, id("rod/pwr_fuel_bfb_am_mix"));
        Item billetPu241 = item("billet_pu241");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("pwr_fuel_bfb_pu241"))
                .pattern("NFN")
                .pattern("NIN")
                .pattern("NBN")
                .define('F', billetPu241)
                .define('I', platePolymer)
                .define('B', billetBismuth)
                .define('N', item("nugget_uranium_fuel"))
                .unlockedBy("has_billet", has(billetPu241))
                .save(output, id("rod/pwr_fuel_bfb_pu241"));

        // RodRecipes.java:88-90: pile fuel (3 of 5 - pile_rod_lithium/_detector blocked, see class
        // javadoc / #4 discrepancy notes).
        Item plateIron = item("plate_iron");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("pile_rod_uranium"))
                .pattern(" U ")
                .pattern("PUP")
                .pattern(" U ")
                .define('P', plateIron)
                .define('U', item("billet_uranium"))
                .unlockedBy("has_plate", has(plateIron))
                .save(output, id("rod/pile_rod_uranium"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("pile_rod_source"))
                .pattern(" U ")
                .pattern("PUP")
                .pattern(" U ")
                .define('P', plateIron)
                .define('U', item("billet_ra226be"))
                .unlockedBy("has_plate", has(plateIron))
                .save(output, id("rod/pile_rod_source"));
        Item ingotBoron = item("ingot_boron");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("pile_rod_boron"))
                .pattern(" B ")
                .pattern(" W ")
                .pattern(" B ")
                .define('B', ingotBoron)
                .define('W', ItemTags.PLANKS)
                .unlockedBy("has_ingot", has(ingotBoron))
                .save(output, id("rod/pile_rod_boron"));

        // RodRecipes.java:156: icf_pellet_empty, shaped "ZLZ","L L","ZLZ" (Z=zirconium wire, L=lead
        // wire - both MaterialItemGenerator-autogenerated tags, same confirmed-tagged pattern as
        // ToolRecipes' tungstenBolt/durasteelBolt).
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("icf_pellet_empty"))
                .pattern("ZLZ")
                .pattern("L L")
                .pattern("ZLZ")
                .define('Z', MaterialShapes.WIRE.commonTag(Mats.MAT_ZIRCONIUM))
                .define('L', MaterialShapes.WIRE.commonTag(Mats.MAT_LEAD))
                .unlockedBy("has_wire", has(MaterialShapes.WIRE.commonTag(Mats.MAT_ZIRCONIUM)))
                .save(output, id("rod/icf_pellet_empty"));

        // RodRecipes.java:36-44 (see class javadoc's discrepancy note): 9 shapeless depleted-rod ->
        // waste conversions, waste x2 per depleted rod. Not in the research report's own catalog.
        for (String[] row : ZIRNOX_DEPLETED_TO_WASTE) {
            Item depleted = item(row[0]);
            ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item(row[1]), 2)
                    .requires(depleted)
                    .unlockedBy("has_depleted", has(depleted))
                    .save(output, id("rod/" + row[0] + "_to_waste"));
        }

        // Deliberately not ported (see class javadoc for the full reasoning):
        // - All 9 addZIRNOXRod fresh-rod fill recipes + the rod_zirnox_empty housing craft + the
        //   LITHIUM_FUEL/ZFB_MOX_FUEL direct calls: rod_zirnox_empty is not a registered item anywhere
        //   in this port (correcting the research report's contradictory "ready now" placement).
        // - All 99 breeding-rod (addBreedingRod/LEAD/LITHIUM/TRITIUM) recipes: rod_empty/
        //   rod_dual_empty/rod_quad_empty are not registered anywhere - a bootstrapping gap (the
        //   housing-craft recipe's own output is the missing item), matching the research report.
        // - pile_rod_lithium / syringe_empty / mike_deut deuterium pack: empty-cell match lives in
        //   EmptyCellCraftingRecipe (vanilla {"item":"hbm:cell"} also matches filled).
        //   pile_rod_detector is ce_craft/rods JSON (circuit_vacuum_tube + motor + ingot_boron).
        // - The 3 TRITIUM breeding-rod "unload to filled cell" recipes (RodRecipes.java:61-63): the
        //   result is a hbm:cell carrying a fluid-id data component
        //   (com.hbm.items.special.ItemCell.getFullCell), which a plain ShapedRecipeBuilder/
        //   ShapelessRecipeBuilder result cannot express - the same "recipe result needs to carry data
        //   components, past what this builder expresses" gap {@link #powderRecipes}'s own javadoc
        //   already identifies for ItemScraps, not guessed at here either. (Moot regardless: the rod
        //   ingredient itself, rod_tritium, is unreachable without rod_empty - see above.)
        // - RodRecipes.registerInit()'s 2 OreDictionary.doesOreNameExist(...)-gated GT6 pellets (NQD,
        //   NQR): no NeoForge ore-dictionary equivalent, matching this class's established policy.

    }

    // ================================================================================================
    // Part 8: WeaponRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/WeaponRecipes.java
    // ================================================================================================
    // docs/phase7/crafting_weapon_rod_consumable.md assignment. The overwhelming majority of this CE
    // file (weapon mods table, SEDNA Parts, SEDNA Ammo, Secrets, Missiles, missile fins/warhead/chips,
    // Turrets, most Guns-misc, Ammo assemblies, 240mm/Artillery Shells, DGK Belts, Fire-ext tanks, every
    // Grenade section, Sticks/Blocks of explosives, Mines, most Nuke parts) is blocked on this port's
    // still-missing circuit/ducttape/motor/piston_selenium/safety_fuse/ball_x/steel_scaffold family -
    // matching the research report's overall picture. What follows corrects and narrows that report on
    // several specific, independently re-checked points:
    // - CE's entire ammo corpus (ammo_standard, ammo_shell, ammo_arty, ammo_dgk, ammo_fireext,
    //   ammo_secret, casing, item_secret) is NOT just "differently named" as the report's §6.1 risk
    //   suggested - com.hbm.items.weapon.ItemAmmo.java's own class javadoc states outright "Nothing in
    //   this port constructs an ItemAmmo yet"; ammo_shell/ammo_arty/ammo_secret have no registration
    //   site at all, and ammo_fireext's 3 XFactoryTool constants (ITEM_FEXT_WATER/_FOAM/_SAND) are bare
    //   `new Item(...)` never passed to a DeferredRegister, so they carry no registry name either. This
    //   means 240mm Shells, Artillery Shells, DGK Belts, Fire-ext tanks, SEDNA Ammo and Secrets are
    //   ALL blocked in full - a stronger, corrected finding than the report's per-section "mixed"/
    //   "likely ready" verdicts for these exact sections.
    // - CE's "Missile fuselages" section (10 recipes) is blocked in full: its *precursor* items
    //   (mp_fuselage_10_kerosene, etc.) do exist as the report flagged needing confirmation, but the
    //   recipes' actual *output* items (mp_fuselage_*_insulation/_metal/_desh) do not exist under any
    //   name in this port's MissileItems.java - the report's "R, contingent on the precursor" framing
    //   checked the wrong side of these recipes.
    // - CE's SEDNA Guns (43) and weapon_mod_special (16 of CE's ~29, the rest already flattened into
    //   more this-port-specific ids - see WeaponModItems.java) sections turned out far more portable
    //   than the report's "mixed per-recipe" hedge suggested, once every CE `X.shape()` call was
    //   resolved against real registered ids: legacy shapes (plate_x/ingot_x) were checked directly
    //   against PlateCrystalWasteItems/IngotNuggetItems source; MaterialItemGenerator-autogenerated
    //   shapes (light/heavy barrel+receiver, gun_mechanism, stock, grip, bolt, castplate, shell, pipe,
    //   wire, dense_wire) were resolved via MaterialShapes#commonTag exactly like ToolRecipes'/
    //   armorRecipes' own already-established precedent (tungstenBolt, durasteelBolt, titaniumCastPlate
    //   etc.) - not the grip/stock legacy-ingot's ".ingot()" call, which is a separate hand-registered
    //   family that does NOT exist for MAT_HARDPLASTIC (Polycarbonate) specifically (only MAT_PVC's
    //   does), a distinction the recipes below preserve. CE's "Any" DictGroup ingredients (ANY_PLASTIC
    //   = Polymer+Bakelite, ANY_RESISTANTALLOY = TcAlloy+CdAlloy) have no grouped-tag equivalent in this
    //   port, so they are rebuilt as a plain multi-item {@code Ingredient.of(...)} union of whichever
    //   group members this port actually has real items for (a strict subset of CE's real matching set
    //   when one member - e.g. ANY_RUBBER's Latex, ANY_HARDPLASTIC's Polycarbonate ingot - does not
    //   exist here; never an invented item).
    // - "crucible" (WeaponRecipes.java:334): CE's metadata-3 melee weapon ("The Crucible" sword,
    //   WeaponTiers.CRUCIBLE), already ported to this port's id "crucible"
    //   (com.hbm.items.weapon.WeaponMeleeItems.CRUCIBLE) - explicitly NOT the smelting-Crucible machine
    //   that is Phase 7's other, much larger workstream. Confirmed here per the research report's own
    //   flagged disambiguation.
    // - Custom nuke rods (WeaponRecipes.java:323-330): re-checked individually rather than left
    //   "unverified" as the report did - custom_tnt/custom_hydro/custom_amat/custom_euph are blocked
    //   (ANY_HIGHEXPLOSIVE has no port equivalent; ItemCell.getFullCell(fluid) is a data-component
    //   recipe result/ingredient this builder cannot express, the same gap noted above and in
    //   #powderRecipes' own javadoc; custom_euph's own output item does not exist) but custom_nuke,
    //   custom_dirty, custom_schrab and custom_sol use only concrete, confirmed-real ingredients and
    //   are ported below - a real addition the report did not surface.

    private void weaponRecipes(RecipeOutput output) {
        // ---- SEDNA Guns (WeaponRecipes.java:61-109). 36 of CE's 43 recipes (35 distinct outputs -
        // gun_charge_thrower has 2 alternate recipes, matching CE). Not ported: gun_stinger/gun_quadro/
        // gun_missile_launcher/gun_tesla_cannon/gun_tau/gun_lasrifle/gun_pa_melee/gun_pa_ranged (circuit
        // family), gun_minigun (motor_desh), gun_stg77 (gem_diamond, a legacy GEM-shape item not
        // registered under any name), gun_double_barrel_sacred_dragon (item_secret), gun_drill
        // (piston_selenium). ----
        Item ironIngot = Items.IRON_INGOT;
        Item ingotCopper = item("ingot_copper");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_pepperbox"))
                .pattern("IIW").pattern("  C")
                .define('I', ironIngot).define('W', ItemTags.PLANKS).define('C', ingotCopper)
                .unlockedBy("has_ingot", has(ironIngot)).save(output, id("weapon/gun_pepperbox"));

        TagKey<Item> steelLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_STEEL);
        TagKey<Item> steelLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_STEEL);
        TagKey<Item> steelHeavyBarrel = MaterialShapes.HEAVYBARREL.commonTag(Mats.MAT_STEEL);
        TagKey<Item> steelBolt = MaterialShapes.BOLT.commonTag(Mats.MAT_STEEL);
        TagKey<Item> steelGrip = MaterialShapes.GRIP.commonTag(Mats.MAT_STEEL);
        TagKey<Item> steelCastplate = MaterialShapes.CASTPLATE.commonTag(Mats.MAT_STEEL);
        TagKey<Item> gunmetalMechanism = MaterialShapes.MECHANISM.commonTag(Mats.MAT_GUNMETAL);
        TagKey<Item> gunmetalLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_GUNMETAL);
        TagKey<Item> woodGrip = MaterialShapes.GRIP.commonTag(Mats.MAT_WOOD);
        TagKey<Item> woodStock = MaterialShapes.STOCK.commonTag(Mats.MAT_WOOD);
        Item plateGunmetal = item("plate_gunmetal");
        TagKey<Item> weaponsteelMechanism = MaterialShapes.MECHANISM.commonTag(Mats.MAT_WEAPONSTEEL);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_light_revolver"))
                .pattern("BRM").pattern("  G")
                .define('B', steelLightBarrel).define('R', steelLightReceiver).define('M', gunmetalMechanism).define('G', woodGrip)
                .unlockedBy("has_barrel", has(steelLightBarrel)).save(output, id("weapon/gun_light_revolver"));
        Item gunLightRevolver = item("gun_light_revolver");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_light_revolver_atlas"))
                .pattern(" M ").pattern("MAM").pattern(" M ")
                .define('M', weaponsteelMechanism).define('A', gunLightRevolver)
                .unlockedBy("has_gun", has(gunLightRevolver)).save(output, id("weapon/gun_light_revolver_atlas"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_henry"))
                .pattern("BRP").pattern("BMS")
                .define('B', steelLightBarrel).define('R', gunmetalLightReceiver).define('M', gunmetalMechanism)
                .define('S', woodStock).define('P', plateGunmetal)
                .unlockedBy("has_barrel", has(steelLightBarrel)).save(output, id("weapon/gun_henry"));
        Item gunHenry = item("gun_henry");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_henry_lincoln"))
                .pattern(" M ").pattern("PGP").pattern(" M ")
                .define('M', weaponsteelMechanism).define('P', MaterialShapes.CASTPLATE.commonTag(Mats.MAT_GOLD)).define('G', gunHenry)
                .unlockedBy("has_gun", has(gunHenry)).save(output, id("weapon/gun_henry_lincoln"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_greasegun"))
                .pattern("BRS").pattern("SMG")
                .define('B', steelLightBarrel).define('R', steelLightReceiver).define('S', steelBolt)
                .define('M', gunmetalMechanism).define('G', steelGrip)
                .unlockedBy("has_barrel", has(steelLightBarrel)).save(output, id("weapon/gun_greasegun"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_maresleg"))
                .pattern("BRM").pattern("BGS")
                .define('B', steelLightBarrel).define('R', steelLightReceiver).define('M', gunmetalMechanism)
                .define('G', steelBolt).define('S', woodStock)
                .unlockedBy("has_barrel", has(steelLightBarrel)).save(output, id("weapon/gun_maresleg"));
        Item gunMaresleg = item("gun_maresleg");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_maresleg_akimbo"))
                .pattern("SMS")
                .define('S', gunMaresleg).define('M', weaponsteelMechanism)
                .unlockedBy("has_gun", has(gunMaresleg)).save(output, id("weapon/gun_maresleg_akimbo"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_flaregun"))
                .pattern("BRM").pattern("  G")
                .define('B', steelHeavyBarrel).define('R', steelLightReceiver).define('M', gunmetalMechanism).define('G', steelGrip)
                .unlockedBy("has_barrel", has(steelHeavyBarrel)).save(output, id("weapon/gun_flaregun"));

        TagKey<Item> duraLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_DURA);
        TagKey<Item> duraLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_DURA);
        TagKey<Item> duraHeavyBarrel = MaterialShapes.HEAVYBARREL.commonTag(Mats.MAT_DURA);
        TagKey<Item> duraHeavyReceiver = MaterialShapes.HEAVYRECEIVER.commonTag(Mats.MAT_DURA);
        TagKey<Item> duraGrip = MaterialShapes.GRIP.commonTag(Mats.MAT_DURA);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_am180"))
                .pattern("BBR").pattern("GMS")
                .define('B', duraLightBarrel).define('R', duraLightReceiver).define('M', gunmetalMechanism)
                .define('G', woodGrip).define('S', woodStock)
                .unlockedBy("has_barrel", has(duraLightBarrel)).save(output, id("weapon/gun_am180"));

        TagKey<Item> weaponsteelLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_WEAPONSTEEL);
        TagKey<Item> weaponsteelLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_WEAPONSTEEL);
        TagKey<Item> weaponsteelHeavyBarrel = MaterialShapes.HEAVYBARREL.commonTag(Mats.MAT_WEAPONSTEEL);
        TagKey<Item> weaponsteelHeavyReceiver = MaterialShapes.HEAVYRECEIVER.commonTag(Mats.MAT_WEAPONSTEEL);
        TagKey<Item> weaponsteelShell = MaterialShapes.SHELL.commonTag(Mats.MAT_WEAPONSTEEL);
        Item ingotPolymer = item("ingot_polymer");
        Item ingotBakelite = item("ingot_bakelite");
        Ingredient anyPlasticGrip = CompoundIngredient.of(
                Ingredient.of(MaterialShapes.GRIP.commonTag(Mats.MAT_POLYMER)),
                Ingredient.of(MaterialShapes.GRIP.commonTag(Mats.MAT_BAKELITE)));
        Ingredient anyPlasticStock = CompoundIngredient.of(
                Ingredient.of(MaterialShapes.STOCK.commonTag(Mats.MAT_POLYMER)),
                Ingredient.of(MaterialShapes.STOCK.commonTag(Mats.MAT_BAKELITE)));
        Ingredient anyPlasticIngot = Ingredient.of(ingotPolymer, ingotBakelite);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_star_f"))
                .pattern("BRM").pattern("  G")
                .define('B', weaponsteelLightBarrel).define('R', weaponsteelLightReceiver)
                .define('M', weaponsteelMechanism).define('G', anyPlasticGrip)
                .unlockedBy("has_barrel", has(weaponsteelLightBarrel)).save(output, id("weapon/gun_star_f"));
        Item gunStarF = item("gun_star_f");
        TagKey<Item> bigmtMechanism = MaterialShapes.MECHANISM.commonTag(Mats.MAT_SATURN);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_star_f_akimbo"))
                .pattern("UMU")
                .define('U', gunStarF).define('M', bigmtMechanism)
                .unlockedBy("has_gun", has(gunStarF)).save(output, id("weapon/gun_star_f_akimbo"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_liberator"))
                .pattern("BB ").pattern("BBM").pattern("G G")
                .define('B', duraLightBarrel).define('M', gunmetalMechanism).define('G', woodGrip)
                .unlockedBy("has_barrel", has(duraLightBarrel)).save(output, id("weapon/gun_liberator"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_congolake"))
                .pattern("BM ").pattern("BRS").pattern("G  ")
                .define('B', duraHeavyBarrel).define('M', gunmetalMechanism).define('R', duraLightReceiver)
                .define('S', woodStock).define('G', woodGrip)
                .unlockedBy("has_barrel", has(duraHeavyBarrel)).save(output, id("weapon/gun_congolake"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_mk108"))
                .pattern(" GG").pattern("BRM").pattern(" D ")
                .define('G', anyPlasticGrip).define('B', weaponsteelHeavyBarrel).define('R', weaponsteelHeavyReceiver)
                .define('M', weaponsteelMechanism).define('D', weaponsteelShell)
                .unlockedBy("has_barrel", has(weaponsteelHeavyBarrel)).save(output, id("weapon/gun_mk108"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_flamer"))
                .pattern(" MG").pattern("BBR").pattern(" GM")
                .define('M', gunmetalMechanism).define('G', duraGrip).define('B', duraHeavyBarrel).define('R', duraHeavyReceiver)
                .unlockedBy("has_barrel", has(duraHeavyBarrel)).save(output, id("weapon/gun_flamer"));
        Item gunFlamer = item("gun_flamer");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_flamer_topaz"))
                .pattern(" M ").pattern("MFM").pattern(" M ")
                .define('M', weaponsteelMechanism).define('F', gunFlamer)
                .unlockedBy("has_gun", has(gunFlamer)).save(output, id("weapon/gun_flamer_topaz"));

        // MAT_DESH's real registry name is "workersalloy" (Mats.java: n("WorkersAlloy")) - resolved
        // programmatically via commonTag/buildRegistryName below rather than guessed as "desh_*",
        // exactly matching this class's own {@link #BLOCK_INGOT_SETS} precedent for the same material.
        TagKey<Item> deshLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_DESH);
        TagKey<Item> deshLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_DESH);
        TagKey<Item> deshHeavyBarrel = MaterialShapes.HEAVYBARREL.commonTag(Mats.MAT_DESH);
        TagKey<Item> deshStock = MaterialShapes.STOCK.commonTag(Mats.MAT_DESH);
        TagKey<Item> deshGrip = MaterialShapes.GRIP.commonTag(Mats.MAT_DESH);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_heavy_revolver"))
                .pattern("BRM").pattern("  G")
                .define('B', deshLightBarrel).define('R', deshLightReceiver).define('M', gunmetalMechanism).define('G', woodGrip)
                .unlockedBy("has_barrel", has(deshLightBarrel)).save(output, id("weapon/gun_heavy_revolver"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_carbine"))
                .pattern("BRM").pattern("G S")
                .define('B', deshLightBarrel).define('R', deshLightReceiver).define('M', gunmetalMechanism)
                .define('G', woodGrip).define('S', woodStock)
                .unlockedBy("has_barrel", has(deshLightBarrel)).save(output, id("weapon/gun_carbine"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_uzi"))
                .pattern("BRS").pattern(" GM")
                .define('B', deshLightBarrel).define('R', deshLightReceiver).define('S', anyPlasticStock)
                .define('G', anyPlasticGrip).define('M', gunmetalMechanism)
                .unlockedBy("has_barrel", has(deshLightBarrel)).save(output, id("weapon/gun_uzi"));
        Item gunUzi = item("gun_uzi");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_uzi_akimbo"))
                .pattern("UMU")
                .define('U', gunUzi).define('M', weaponsteelMechanism)
                .unlockedBy("has_gun", has(gunUzi)).save(output, id("weapon/gun_uzi_akimbo"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_spas12"))
                .pattern("BRM").pattern("BGS")
                .define('B', deshLightBarrel).define('R', deshLightReceiver).define('M', gunmetalMechanism)
                .define('G', anyPlasticGrip).define('S', deshStock)
                .unlockedBy("has_barrel", has(deshLightBarrel)).save(output, id("weapon/gun_spas12"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_panzerschreck"))
                .pattern("BBB").pattern("PGM")
                .define('B', deshHeavyBarrel).define('P', steelCastplate).define('G', deshGrip).define('M', gunmetalMechanism)
                .unlockedBy("has_barrel", has(deshHeavyBarrel)).save(output, id("weapon/gun_panzerschreck"));

        TagKey<Item> rubberGrip = MaterialShapes.GRIP.commonTag(Mats.MAT_RUBBER);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_g3"))
                .pattern("BRM").pattern("WGS")
                .define('B', weaponsteelLightBarrel).define('R', weaponsteelLightReceiver).define('M', weaponsteelMechanism)
                .define('W', woodGrip).define('G', rubberGrip).define('S', woodStock)
                .unlockedBy("has_barrel", has(weaponsteelLightBarrel)).save(output, id("weapon/gun_g3"));
        Item gunG3 = item("gun_g3");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_g3_zebra"))
                .pattern(" M ").pattern("MPM").pattern(" M ")
                .define('M', bigmtMechanism).define('P', gunG3)
                .unlockedBy("has_gun", has(gunG3)).save(output, id("weapon/gun_g3_zebra"));

        TagKey<Item> rubberPipe = MaterialShapes.PIPE.commonTag(Mats.MAT_RUBBER);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_chemthrower"))
                .pattern("MHW").pattern("PSS")
                .define('M', weaponsteelMechanism).define('H', rubberPipe).define('W', item("wrench"))
                .define('P', weaponsteelHeavyBarrel).define('S', weaponsteelShell)
                .unlockedBy("has_barrel", has(weaponsteelHeavyBarrel)).save(output, id("weapon/gun_chemthrower"));

        TagKey<Item> ferroHeavyBarrel = MaterialShapes.HEAVYBARREL.commonTag(Mats.MAT_FERRO);
        TagKey<Item> ferroHeavyReceiver = MaterialShapes.HEAVYRECEIVER.commonTag(Mats.MAT_FERRO);
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_amat"))
                .pattern(" C ").pattern("BRS").pattern(" MG")
                .define('G', woodGrip).define('B', ferroHeavyBarrel).define('R', ferroHeavyReceiver)
                .define('M', weaponsteelMechanism).define('C', item("weapon_mod_special_scope")).define('S', woodStock)
                .unlockedBy("has_barrel", has(ferroHeavyBarrel)).save(output, id("weapon/gun_amat"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_m2"))
                .pattern("  G").pattern("BRM").pattern("  G")
                .define('G', woodGrip).define('B', ferroHeavyBarrel).define('R', ferroHeavyReceiver).define('M', weaponsteelMechanism)
                .unlockedBy("has_barrel", has(ferroHeavyBarrel)).save(output, id("weapon/gun_m2"));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_autoshotgun"))
                .pattern("BRM").pattern("G G")
                .define('B', ferroHeavyBarrel).define('R', ferroHeavyReceiver).define('M', weaponsteelMechanism).define('G', anyPlasticGrip)
                .unlockedBy("has_barrel", has(ferroHeavyBarrel)).save(output, id("weapon/gun_autoshotgun"));
        Item gunAutoshotgun = item("gun_autoshotgun");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_autoshotgun_shredder"))
                .pattern(" M ").pattern("MAM").pattern(" M ")
                .define('M', bigmtMechanism).define('A', gunAutoshotgun)
                .unlockedBy("has_gun", has(gunAutoshotgun)).save(output, id("weapon/gun_autoshotgun_shredder"));

        TagKey<Item> tcalloyLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_TCALLOY);
        TagKey<Item> cdalloyLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_CDALLOY);
        TagKey<Item> tcalloyLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_TCALLOY);
        TagKey<Item> cdalloyLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_CDALLOY);
        Ingredient anyResistantAlloyLightBarrel = CompoundIngredient.of(
                Ingredient.of(tcalloyLightBarrel), Ingredient.of(cdalloyLightBarrel));
        Ingredient anyResistantAlloyLightReceiver = CompoundIngredient.of(
                Ingredient.of(tcalloyLightReceiver), Ingredient.of(cdalloyLightReceiver));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_lag"))
                .pattern("BRM").pattern("  G")
                .define('B', anyResistantAlloyLightBarrel).define('R', anyResistantAlloyLightReceiver)
                .define('M', weaponsteelMechanism).define('G', anyPlasticGrip)
                .unlockedBy("has_mechanism", has(weaponsteelMechanism)).save(output, id("weapon/gun_lag"));

        Item crystalRedstone = item("crystal_redstone");
        TagKey<Item> bigmtLightReceiver = MaterialShapes.LIGHTRECEIVER.commonTag(Mats.MAT_SATURN);
        Item ingotPvc = item("ingot_pvc");
        Ingredient anyHardplasticGrip = CompoundIngredient.of(
                Ingredient.of(MaterialShapes.GRIP.commonTag(Mats.MAT_HARDPLASTIC)),
                Ingredient.of(MaterialShapes.GRIP.commonTag(Mats.MAT_PVC)));
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_laser_pistol"))
                .pattern("CRM").pattern("GG ")
                .define('C', crystalRedstone).define('R', bigmtLightReceiver).define('M', bigmtMechanism).define('G', anyHardplasticGrip)
                .unlockedBy("has_crystal", has(crystalRedstone)).save(output, id("weapon/gun_laser_pistol"));
        Item gunLaserPistol = item("gun_laser_pistol");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_laser_pistol_pew_pew"))
                .pattern(" M ").pattern("MPM").pattern(" M ")
                .define('M', bigmtMechanism).define('P', gunLaserPistol)
                .unlockedBy("has_gun", has(gunLaserPistol)).save(output, id("weapon/gun_laser_pistol_pew_pew"));

        TagKey<Item> bigmtHeavyBarrel = MaterialShapes.HEAVYBARREL.commonTag(Mats.MAT_SATURN);
        TagKey<Item> bigmtHeavyReceiver = MaterialShapes.HEAVYRECEIVER.commonTag(Mats.MAT_SATURN);
        TagKey<Item> bigmtShell = MaterialShapes.SHELL.commonTag(Mats.MAT_SATURN);
        Item plateSaturnite = item("plate_saturnite");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_fatman"))
                .pattern("PPP").pattern("BSR").pattern("G M")
                .define('P', plateSaturnite).define('B', bigmtHeavyBarrel).define('S', bigmtShell)
                .define('R', bigmtHeavyReceiver).define('G', anyHardplasticGrip).define('M', bigmtMechanism)
                .unlockedBy("has_plate", has(plateSaturnite)).save(output, id("weapon/gun_fatman"));

        Item plateSteel = item("plate_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_charge_thrower"))
                .pattern("MMM").pattern("BBL").pattern("GG ")
                .define('M', gunmetalMechanism).define('B', steelHeavyBarrel).define('G', steelGrip).define('L', Items.LEATHER)
                .unlockedBy("has_barrel", has(steelHeavyBarrel)).save(output, id("weapon/gun_charge_thrower_leather"));
        Item ingotRubber = item("ingot_rubber");
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("gun_charge_thrower"))
                .pattern("MMM").pattern("BBL").pattern("GG ")
                .define('M', gunmetalMechanism).define('B', steelHeavyBarrel).define('G', steelGrip).define('L', ingotRubber)
                .unlockedBy("has_barrel", has(steelHeavyBarrel)).save(output, id("weapon/gun_charge_thrower_rubber"));

        // ---- Missile thruster (WeaponRecipes.java:187). ----
        Item mpThrusterLarge = item("mp_thruster_15_balefire_large");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("mp_thruster_15_balefire_large_rad"))
                .pattern("CCC").pattern("CTC").pattern("CCC")
                .define('C', MaterialShapes.CASTPLATE.commonTag(Mats.MAT_COPPER)).define('T', mpThrusterLarge)
                .unlockedBy("has_thruster", has(mpThrusterLarge)).save(output, id("weapon/mp_thruster_15_balefire_large_rad"));

        // ---- weapon_mod_special (WeaponRecipes.java:137-163). 18 of CE's 29 - LAS_SHOTGUN/
        // LAS_CAPACITOR/LAS_AUTO (circuit) and ENGINE_DIESEL/ENGINE_AVIATION/ENGINE_TURBO/CANISTERS
        // (piston_selenium/canister_empty) not ported. Every weapon_mod_generic recipe (18, a separate
        // CE section) needs ducttape and is not ported either. ----
        Item ingotDuraSteel = item("ingot_dura_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_silencer"))
                .pattern("P").pattern("B").pattern("P")
                .define('P', anyPlasticIngot).define('B', steelLightBarrel)
                .unlockedBy("has_ingot", has(ingotPolymer)).save(output, id("weapon/mod_special_silencer"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_scope"))
                .pattern("SPS").pattern("G G").pattern("SPS")
                .define('P', anyPlasticIngot).define('S', plateSteel).define('G', GLASS_PANES)
                .unlockedBy("has_ingot", has(ingotPolymer)).save(output, id("weapon/mod_special_scope"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_saw"))
                .pattern("BBS").pattern("BHS")
                .define('B', steelBolt).define('S', Items.STICK).define('H', item("plate_dura_steel"))
                .unlockedBy("has_bolt", has(steelBolt)).save(output, id("weapon/mod_special_saw"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_speedloader"))
                .pattern(" B ").pattern("BSB").pattern(" B ")
                .define('B', steelBolt).define('S', item("plate_weaponsteel"))
                .unlockedBy("has_bolt", has(steelBolt)).save(output, id("weapon/mod_special_speedloader"));
        Item ingotWeaponsteel = item("ingot_weaponsteel");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_slowdown"))
                .pattern(" I ").pattern(" M ").pattern("I I")
                .define('I', ingotWeaponsteel).define('M', weaponsteelMechanism)
                .unlockedBy("has_ingot", has(ingotWeaponsteel)).save(output, id("weapon/mod_special_slowdown"));
        TagKey<Item> goldDenseWireTag = MaterialShapes.DENSEWIRE.commonTag(Mats.MAT_GOLD);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_speedup"))
                .pattern("PIP").pattern("WWW").pattern("PIP")
                .define('P', item("plate_weaponsteel")).define('I', item("ingot_gunmetal")).define('W', goldDenseWireTag)
                .unlockedBy("has_wire", has(goldDenseWireTag)).save(output, id("weapon/mod_special_speedup"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_greasegun"))
                .pattern("BRM").pattern("P G")
                .define('B', weaponsteelLightBarrel).define('R', weaponsteelLightReceiver).define('M', weaponsteelMechanism)
                .define('P', item("plate_dura_steel")).define('G', anyPlasticGrip)
                .unlockedBy("has_barrel", has(weaponsteelLightBarrel)).save(output, id("weapon/mod_special_greasegun"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_choke"))
                .pattern("P").pattern("B").pattern("P")
                .define('P', item("plate_weaponsteel")).define('B', duraLightBarrel)
                .unlockedBy("has_barrel", has(duraLightBarrel)).save(output, id("weapon/mod_special_choke"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_furniture_green"))
                .pattern("PDS").pattern("  G")
                .define('P', anyPlasticIngot).define('D', Items.GREEN_DYE).define('S', anyPlasticStock).define('G', anyPlasticGrip)
                .unlockedBy("has_ingot", has(ingotPolymer)).save(output, id("weapon/mod_special_furniture_green"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_furniture_black"))
                .pattern("PDS").pattern("  G")
                .define('P', anyPlasticIngot).define('D', Items.BLACK_DYE).define('S', anyPlasticStock).define('G', anyPlasticGrip)
                .unlockedBy("has_ingot", has(ingotPolymer)).save(output, id("weapon/mod_special_furniture_black"));
        TagKey<Item> bigmtLightBarrel = MaterialShapes.LIGHTBARREL.commonTag(Mats.MAT_SATURN);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_skin_saturnite"))
                .pattern("BRM").pattern(" P ")
                .define('B', bigmtLightBarrel).define('R', bigmtLightReceiver).define('M', bigmtMechanism).define('P', plateSaturnite)
                .unlockedBy("has_plate", has(plateSaturnite)).save(output, id("weapon/mod_special_skin_saturnite"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_stack_mag"))
                .pattern("P P").pattern("P P").pattern("PMP")
                .define('P', item("plate_weaponsteel")).define('M', bigmtMechanism)
                .unlockedBy("has_mechanism", has(bigmtMechanism)).save(output, id("weapon/mod_special_stack_mag"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_bayonet"))
                .pattern("  P").pattern("BBB")
                .define('P', item("plate_weaponsteel")).define('B', steelBolt)
                .unlockedBy("has_bolt", has(steelBolt)).save(output, id("weapon/mod_special_bayonet"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_drill_hss"))
                .pattern(" IP").pattern("IIM").pattern(" IP")
                .define('I', ingotDuraSteel).define('P', anyPlasticIngot).define('M', gunmetalMechanism)
                .unlockedBy("has_ingot", has(ingotDuraSteel)).save(output, id("weapon/mod_special_drill_hss"));
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_drill_weaponsteel"))
                .pattern(" IP").pattern("IIM").pattern(" IP")
                .define('I', ingotWeaponsteel).define('P', ingotRubber).define('M', gunmetalMechanism)
                .unlockedBy("has_ingot", has(ingotWeaponsteel)).save(output, id("weapon/mod_special_drill_weaponsteel"));
        Item ingotTcalloy = item("ingot_tcalloy");
        Item ingotCdalloy = item("ingot_cdalloy");
        Ingredient anyResistantAlloyIngot = Ingredient.of(ingotTcalloy, ingotCdalloy);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_drill_tcalloy"))
                .pattern(" IP").pattern("IIM").pattern(" IP")
                .define('I', anyResistantAlloyIngot).define('P', ingotRubber).define('M', weaponsteelMechanism)
                .unlockedBy("has_ingot", has(ingotTcalloy)).save(output, id("weapon/mod_special_drill_tcalloy"));
        Item ingotSaturnite = item("ingot_saturnite");
        Ingredient anyHardplasticIngot = Ingredient.of(ingotPvc);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_drill_saturnite"))
                .pattern(" IP").pattern("IIM").pattern(" IP")
                .define('I', ingotSaturnite).define('P', anyHardplasticIngot).define('M', weaponsteelMechanism)
                .unlockedBy("has_ingot", has(ingotSaturnite)).save(output, id("weapon/mod_special_drill_saturnite"));
        Item capacitorGoldPack = item("capacitor_gold_pack");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_engine_electric"))
                .pattern("DSD").pattern("PPP").pattern("DSD")
                .define('D', anyPlasticIngot).define('P', goldDenseWireTag).define('S', capacitorGoldPack)
                .unlockedBy("has_battery", has(capacitorGoldPack)).save(output, id("weapon/mod_special_engine_electric"));
        Item niobiumBlock = item("niobium_block");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_magnet"))
                .pattern("RGR").pattern("GBG").pattern("RGR")
                .define('R', ingotRubber).define('G', goldDenseWireTag).define('B', niobiumBlock)
                .unlockedBy("has_block", has(niobiumBlock)).save(output, id("weapon/mod_special_magnet"));
        Item steelGrate = item("steel_grate");
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, item("weapon_mod_special_sifter"))
                .pattern("IGI").pattern("IGI")
                .define('I', ingotDuraSteel).define('G', steelGrate)
                .unlockedBy("has_grate", has(steelGrate)).save(output, id("weapon/mod_special_sifter"));

        // ---- Custom nuke rods (WeaponRecipes.java:324, 327-329). ----
        Item plateCopper = item("plate_copper");
        Item plateLead = item("plate_lead");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("custom_nuke"))
                .pattern(" C ").pattern("LUL").pattern("LUL")
                .define('C', plateCopper).define('L', plateLead).define('U', item("ingot_u235"))
                .unlockedBy("has_plate", has(plateCopper)).save(output, id("weapon/custom_nuke"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("custom_dirty"))
                .pattern(" C ").pattern("WLW").pattern("WLW")
                .define('C', plateCopper).define('L', plateLead).define('W', item("nuclear_waste"))
                .unlockedBy("has_plate", has(plateCopper)).save(output, id("weapon/custom_dirty"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("custom_schrab"))
                .pattern(" C ").pattern("LUL").pattern("LUL")
                .define('C', plateCopper).define('L', plateLead).define('U', item("ingot_schrabidium"))
                .unlockedBy("has_plate", has(plateCopper)).save(output, id("weapon/custom_schrab"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("custom_sol"))
                .pattern(" C ").pattern("LUL").pattern("LUL")
                .define('C', plateCopper).define('L', plateLead).define('U', item("ingot_solinium"))
                .unlockedBy("has_plate", has(plateCopper)).save(output, id("weapon/custom_sol"));

        // ---- Misc (WeaponRecipes.java:334): "crucible" is CE's melee weapon, NOT the smelting
        // Crucible machine - see class javadoc disambiguation. ----
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, item("crucible"))
                .pattern("MEM").pattern("YDY").pattern("YCY")
                .define('M', item("ingot_meteorite_forged")).define('E', item("ingot_euphemium"))
                .define('Y', item("billet_yharonite")).define('D', item("demon_core_closed")).define('C', item("ingot_chainsteel"))
                .unlockedBy("has_demon_core", has(item("demon_core_closed"))).save(output, id("weapon/crucible"));
    }

    // ================================================================================================
    // Part 9: ConsumableRecipes - CE upstream/hbm-ce/src/main/java/com/hbm/crafting/ConsumableRecipes.java
    // ================================================================================================
    // docs/phase7/crafting_weapon_rod_consumable.md assignment. Corrections to the research report,
    // found re-deriving every id against this port's real item source rather than trusting its verdicts:
    // - can_smart (report: "R"): needs KNO.dust() (Saltpeter) - re-checked, no powder_saltpeter/
    //   powder_kno item is registered anywhere in this port (the same missing material this class's own
    //   #powderRecipes javadoc already found blocks CE's gunpowder/fertilizer recipes). BLOCKED, not
    //   the report's "ready" verdict.
    // - can_mrsugar/can_overcharge (report: "R"): need F.dust()/S.dust() (fluorine/sulfur powder) -
    //   neither is registered anywhere in this port either (same root cause as pill_iodine/plan_c/radx
    //   below). BLOCKED.
    // - can_creature (report: implicitly "R" as part of the 5-of-6 verdict): needs
    //   {@code Fluids.DIESEL.getDict(1000)}, a "matches any full 1000mB diesel container" ore-dict-style
    //   ingredient with no plain-Ingredient equivalent in this port (same class of gap as this class's
    //   own #powderRecipes javadoc already flags for the AROMATICS/UNSATURATEDS-container recipes).
    //   BLOCKED, not guessed at with an invented Ingredient.
    // - bottle_quantum (report: "R"): needs {@code ModItems.trinitite}, confirmed NOT registered
    //   anywhere in this port (distinct from the real {@code glass_trinitite}/{@code waste_trinitite}
    //   this class's own {@link #SMELTING_SIMPLE} table already uses). BLOCKED - and bottle_rad
    //   transitively with it, since it is shapeless off bottle_quantum.
    // - loops/loop_stew (report: "R"/"R"): loops needs {@code ModItems.flame_pony}, not registered
    //   anywhere in this port. BLOCKED (loop_stew transitively, needing loops).
    // - protection_charm (report: "R" alongside meteor_charm): needs {@code DIAMOND.gem()} -
    //   {@code Mats.MAT_DIAMOND}'s own {@code setAutogen(...)} call carries only {@code FRAGMENT}, no
    //   {@code GEM}; no {@code gem_diamond} item exists under any name in this port (repo-wide grep,
    //   zero hits). BLOCKED - correcting the report, which paired it with meteor_charm as equally ready.
    //   meteor_charm itself (VOLCANIC.gem(), a different, real {@code gem_volcanic} item) is unaffected
    //   and is ported below.
    // - Every other "R" verdict below (apple_euphemium, glowing_stew, balefire_scrambled/_and_ham,
    //   med_ipecac/_ptsd, coffee/_radium, ingot_smore, marshmallow, peas, can_empty, can_luna,
    //   mucho_mango, canteen_vodka, the 7-recipe Soda family minus bottle_quantum/_rad, five_htp, fmn,
    //   siox, meteor_charm) was independently re-confirmed here, not re-quoted.

    private void consumableRecipes(RecipeOutput output) {
        // ---- Food (ConsumableRecipes.java:46-63). ----
        Item nuggetEuphemium = item("nugget_euphemium");
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, item("apple_euphemium"))
                .pattern("EEE").pattern("EAE").pattern("EEE")
                .define('E', nuggetEuphemium).define('A', Items.APPLE)
                .unlockedBy("has_nugget", has(nuggetEuphemium)).save(output, id("consumable/apple_euphemium"));
        Item mush = item("mush");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("glowing_stew"))
                .requires(Items.BOWL).requires(mush).requires(mush)
                .unlockedBy("has_mush", has(mush)).save(output, id("consumable/glowing_stew"));
        Item eggBalefire = item("egg_balefire");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("balefire_scrambled"))
                .requires(Items.BOWL).requires(eggBalefire)
                .unlockedBy("has_egg", has(eggBalefire)).save(output, id("consumable/balefire_scrambled"));
        Item balefireScrambled = item("balefire_scrambled");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("balefire_and_ham"))
                .requires(balefireScrambled).requires(Items.COOKED_BEEF)
                .unlockedBy("has_scrambled", has(balefireScrambled)).save(output, id("consumable/balefire_and_ham"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("med_ipecac"))
                .requires(Items.GLASS_BOTTLE).requires(Items.NETHER_WART)
                .unlockedBy("has_wart", has(Items.NETHER_WART)).save(output, id("consumable/med_ipecac"));
        Item medIpecac = item("med_ipecac");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("med_ptsd"))
                .requires(medIpecac)
                .unlockedBy("has_ipecac", has(medIpecac)).save(output, id("consumable/med_ptsd"));
        Item powderCoal = item("powder_coal");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("coffee"))
                .requires(powderCoal).requires(Items.MILK_BUCKET).requires(Items.POTION).requires(Items.SUGAR)
                .unlockedBy("has_powder", has(powderCoal)).save(output, id("consumable/coffee"));
        Item coffee = item("coffee");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("coffee_radium"))
                .requires(coffee).requires(item("nugget_ra226"))
                .unlockedBy("has_coffee", has(coffee)).save(output, id("consumable/coffee_radium"));
        // CE's `new ItemStack(Items.DYE, 1, 3)` is old (pre-1.13) dye metadata 3 = Cocoa Beans/brown
        // dye - Items.COCOA_BEANS is the modern equivalent (no standalone "brown_dye" item exists;
        // Cocoa Beans itself fills that role post-flattening).
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("ingot_smore"))
                .requires(Items.WHEAT).requires(item("marshmallow_roasted")).requires(Items.COCOA_BEANS)
                .unlockedBy("has_wheat", has(Items.WHEAT)).save(output, id("consumable/ingot_smore"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("marshmallow"))
                .requires(Items.STICK).requires(Items.SUGAR).requires(Items.WHEAT_SEEDS)
                .unlockedBy("has_stick", has(Items.STICK)).save(output, id("consumable/marshmallow"));

        // ---- Peas (ConsumableRecipes.java:69). ----
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, item("peas"))
                .pattern(" S ").pattern("SNS").pattern(" S ")
                .define('S', Items.WHEAT_SEEDS).define('N', Items.GOLD_NUGGET)
                .unlockedBy("has_seeds", has(Items.WHEAT_SEEDS)).save(output, id("consumable/peas"));

        // ---- Cans (ConsumableRecipes.java:72-79). Only can_empty/can_luna/mucho_mango are ready - see
        // class javadoc for why can_smart/_creature/_redbomb/_mrsugar/_overcharge are all blocked. ----
        Item plateAluminium = item("plate_aluminium");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("can_empty"))
                .pattern("P").pattern("P")
                .define('P', plateAluminium)
                .unlockedBy("has_plate", has(plateAluminium)).save(output, id("consumable/can_empty"));
        Item canEmpty = item("can_empty");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, item("can_luna"))
                .requires(canEmpty).requires(Items.POTION).requires(Items.SUGAR).requires(item("powder_meteorite_tiny"))
                .unlockedBy("has_can", has(canEmpty)).save(output, id("consumable/can_luna"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("mucho_mango"))
                .requires(Items.POTION).requires(Items.SUGAR).requires(Items.SUGAR).requires(Items.ORANGE_DYE)
                .unlockedBy("has_potion", has(Items.POTION)).save(output, id("consumable/mucho_mango"));

        // ---- Canteens (ConsumableRecipes.java:82). ----
        ShapedRecipeBuilder.shaped(RecipeCategory.FOOD, item("canteen_vodka"))
                .pattern("O").pattern("P")
                .define('O', Items.POTATO).define('P', item("plate_steel"))
                .unlockedBy("has_potato", has(Items.POTATO)).save(output, id("consumable/canteen_vodka"));

        // ---- Soda (ConsumableRecipes.java:85-93). 7 of 9 - bottle_quantum/bottle_rad blocked
        // (trinitite missing, see class javadoc). ----
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("bottle_empty"), 6)
                .pattern(" G ").pattern("G G").pattern("GGG")
                .define('G', GLASS_PANES)
                .unlockedBy("has_pane", has(GLASS_PANES)).save(output, id("consumable/bottle_empty"));
        Item bottleEmpty = item("bottle_empty");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("bottle_nuka"))
                .requires(bottleEmpty).requires(Items.POTION).requires(Items.SUGAR).requires(powderCoal)
                .unlockedBy("has_bottle", has(bottleEmpty)).save(output, id("consumable/bottle_nuka"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("bottle_cherry"))
                .requires(bottleEmpty).requires(Items.POTION).requires(Items.SUGAR).requires(Items.REDSTONE)
                .unlockedBy("has_bottle", has(bottleEmpty)).save(output, id("consumable/bottle_cherry"));
        Item bottleNuka = item("bottle_nuka");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("bottle_sparkle"))
                .requires(bottleNuka).requires(Items.CARROT).requires(Items.GOLD_NUGGET)
                .unlockedBy("has_bottle", has(bottleNuka)).save(output, id("consumable/bottle_sparkle"));
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("bottle2_empty"), 6)
                .pattern(" G ").pattern("G G").pattern("G G")
                .define('G', GLASS_PANES)
                .unlockedBy("has_pane", has(GLASS_PANES)).save(output, id("consumable/bottle2_empty"));
        Item bottle2Empty = item("bottle2_empty");
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("bottle2_korl"))
                .requires(bottle2Empty).requires(Items.POTION).requires(Items.SUGAR).requires(item("powder_copper"))
                .unlockedBy("has_bottle", has(bottle2Empty)).save(output, id("consumable/bottle2_korl"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("bottle2_fritz"))
                .requires(bottle2Empty).requires(Items.POTION).requires(Items.SUGAR).requires(item("powder_tungsten"))
                .unlockedBy("has_bottle", has(bottle2Empty)).save(output, id("consumable/bottle2_fritz"));

        // ---- Medicine (ConsumableRecipes.java:120-130). Only fmn/five_htp/siox are ready - pill_iodine/
        // plan_c/radx all need F.dust() (fluorine powder), not registered anywhere in this port; cigarette/
        // crackpipe output items don't exist. GeneralConfig.enableLBSM defaults false, so the default
        // ("else") branch is the one ported, matching this class's own established policy for CE's LBSM
        // config gate (see #powderRecipes and the class javadoc's ToolRecipes precedent). ----
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("fmn"))
                .requires(powderCoal).requires(item("powder_polonium")).requires(item("powder_strontium"))
                .unlockedBy("has_powder", has(powderCoal)).save(output, id("consumable/fmn"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("five_htp"))
                .requires(powderCoal).requires(item("powder_euphemium")).requires(item("canteen_vodka"))
                .unlockedBy("has_powder", has(powderCoal)).save(output, id("consumable/five_htp"));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, item("siox"), 8)
                .requires(powderCoal).requires(item("powder_asbestos")).requires(item("nugget_bismuth"))
                .unlockedBy("has_powder", has(powderCoal)).save(output, id("consumable/siox"));

        // ---- Special Mods (ConsumableRecipes.java:204-205). Only meteor_charm is ready -
        // protection_charm needs DIAMOND.gem() (gem_diamond, not a registered item - see class
        // javadoc). ----
        Item fragmentMeteorite = item("fragment_meteorite");
        Item gemVolcanic = item("gem_volcanic");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("meteor_charm"))
                .pattern(" M ").pattern("MDM").pattern(" M ")
                .define('M', fragmentMeteorite).define('D', gemVolcanic)
                .unlockedBy("has_fragment", has(fragmentMeteorite)).save(output, id("consumable/meteor_charm"));
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

    /**
     * Like {@link #item(String)}, but a {@code "minecraft:"}-prefixed id resolves against the vanilla
     * namespace instead of {@code hbm:} - used only by {@link #smeltingRecipes} for the handful of CE
     * {@code SmeltingRecipes.java} entries that smelt directly into a vanilla item (iron/gold ingot,
     * redstone, diamond, gravel, cobblestone) with no hbm-namespaced equivalent of their own.
     */
    private static Item resolveItem(String id) {
        ResourceLocation rl = id.startsWith("minecraft:")
                ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id);
        Item found = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (found == null) {
            throw new IllegalStateException("ModRecipeProvider: item " + rl
                    + " referenced by a smelting recipe is not registered - check the owning item-registration class");
        }
        return found;
    }

    // ================================================================================================
    // Part 8a: CraftingManager misc recipes - basic container/machine-component recipes
    // ================================================================================================
    /**
     * Ports high-value obtainability-hole-closing recipes from CE's
     * {@code CraftingManager.addCrafting()} whose inputs and outputs are all confirmed registered.
     * Focus: fluid containers (canister_empty, gas_empty), machine components (coil_copper,
     * coil_gold), basic tools/items that close reachability gaps. Circuits/motors/advanced items
     * still blocked (not registered).
     */
    private void craftingManagerRecipes(RecipeOutput output) {
        // ---- Fluid containers (CraftingManager.java:177-178). ----
        // canister_empty = "S ","AA","AA", S=STEEL.plate(), A=AL.plate()
        Item plateSteel = item("plate_steel");
        Item plateAluminium = item("plate_aluminium");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("canister_empty"), 2)
                .pattern("S ").pattern("AA").pattern("AA")
                .define('S', plateSteel).define('A', plateAluminium)
                .unlockedBy("has_plate", has(plateSteel))
                .save(output, id("container/canister_empty"));

        // gas_empty = "S ","AA","AA", A=STEEL.plate(), S=CU.plate()
        Item plateCopper = item("plate_copper");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("gas_empty"), 2)
                .pattern("S ").pattern("AA").pattern("AA")
                .define('A', plateSteel).define('S', plateCopper)
                .unlockedBy("has_plate", has(plateSteel))
                .save(output, id("container/gas_empty"));

        // ---- Coils (CraftingManager.java:205-208). ----
        // coil_copper = "WWW","WIW","WWW", W=MINGRADE.wireFine(), I=IRON.ingot()
        TagKey<Item> mingradeWireTag = MaterialShapes.WIRE.commonTag(Mats.MAT_MINGRADE);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("coil_copper"))
                .pattern("WWW").pattern("WIW").pattern("WWW")
                .define('W', mingradeWireTag).define('I', Items.IRON_INGOT)
                .unlockedBy("has_wire", has(mingradeWireTag))
                .save(output, id("component/coil_copper_iron"));
        // Alternate with STEEL.ingot()
        Item ingotSteel = item("ingot_steel");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("coil_copper"))
                .pattern("WWW").pattern("WIW").pattern("WWW")
                .define('W', mingradeWireTag).define('I', ingotSteel)
                .unlockedBy("has_wire", has(mingradeWireTag))
                .save(output, id("component/coil_copper_steel"));

        // coil_gold = "WWW","WIW","WWW", W=GOLD.wireFine(), I=IRON.ingot()
        TagKey<Item> goldWireTag = MaterialShapes.WIRE.commonTag(Mats.MAT_GOLD);
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("coil_gold"))
                .pattern("WWW").pattern("WIW").pattern("WWW")
                .define('W', goldWireTag).define('I', Items.IRON_INGOT)
                .unlockedBy("has_wire", has(goldWireTag))
                .save(output, id("component/coil_gold_iron"));
        // Alternate with STEEL.ingot()
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("coil_gold"))
                .pattern("WWW").pattern("WIW").pattern("WWW")
                .define('W', goldWireTag).define('I', ingotSteel)
                .unlockedBy("has_wire", has(goldWireTag))
                .save(output, id("component/coil_gold_steel"));

        // coil_copper_torus (CraftingManager.java:210) = " C ","CPC"," C ", P=IRON.plate(), C=coil_copper
        Item coilCopper = item("coil_copper");
        Item plateIron = item("plate_iron");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("coil_copper_torus"), 2)
                .pattern(" C ").pattern("CPC").pattern(" C ")
                .define('C', coilCopper).define('P', plateIron)
                .unlockedBy("has_coil", has(coilCopper))
                .save(output, id("component/coil_copper_torus"));

        // ---- Lighting machines (CraftingManager.java:486). ----
        // floodlight = "CSC","TST","G G", C=circuit_capacitor, S=STEEL.plate(), T=coil_tungsten, G=KEY_ANYPANE
        Item circuitCapacitor = item("circuit_capacitor");
        Item coilTungsten = item("coil_tungsten");
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, item("floodlight"), 2)
                .pattern("CSC").pattern("TST").pattern("G G")
                .define('C', circuitCapacitor)
                .define('S', plateSteel)
                .define('T', coilTungsten)
                .define('G', GLASS_PANES)
                .unlockedBy("has_circuit", has(circuitCapacitor))
                .save(output, id("machine/floodlight"));

        // ---- Satellite machines (CraftingManager.java:648). ----
        // machine_tape_drive = "PPP","CCC","PPP", P=ANY_PLASTIC.ingot(), C=circuit_pcb
        Item ingotPolymer = item("ingot_polymer");
        Item circuitPcb = item("circuit_pcb");
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, item("machine_tape_drive"))
                .pattern("PPP").pattern("CCC").pattern("PPP")
                .define('P', ingotPolymer)
                .define('C', circuitPcb)
                .unlockedBy("has_circuit", has(circuitPcb))
                .save(output, id("machine/tape_drive"));
    }

    // ================================================================================================
    // Part 9: dynamic crafting-table handlers - docs/phase7/crafting_dynamic_handlers.md
    // ================================================================================================
    // CE's com.hbm.crafting.handlers.* family (see this class's own "Explicitly not attempted"
    // javadoc paragraph, now updated). Every real matching/assembling logic lives in the Java
    // Recipe<CraftingInput> classes themselves (com.hbm.inventory.recipes.crafting.*, each a fixed
    // singleton with a trivial MapCodec.unit(...) serializer - no JSON-configurable fields exist for
    // any of these, exactly like CE's own one-Java-object-per-recipe model). What RecipeManager
    // actually needs on top of that Java class is a minimal type-only JSON entry under
    // data/hbm/recipe/<id>.json to put a RecipeHolder into its datapack-driven recipe map at all -
    // this is the same role SpecialRecipeBuilder plays for vanilla's own RepairItemRecipe/
    // BannerDuplicateRecipe/etc, but this method calls RecipeOutput's own low-level #accept(id,
    // Recipe<?>, AdvancementHolder) directly instead of that (unconfirmed-in-this-checkout) builder
    // class, since #accept's exact signature was independently confirmed real via this repo's local
    // NeoForge source checkout (RecipeProvider.java.patch/RecipeOutput.java.patch - see
    // docs/phase7/crafting_dynamic_handlers.md for how that was verified without network access).
    private void dynamicHandlerRecipes(RecipeOutput output) {
        output.accept(id("grenade_crafting"), GrenadeCraftingRecipe.INSTANCE, null);
        output.accept(id("rbmk_fuel_recycle"), RBMKFuelRecycleRecipe.INSTANCE, null);
        output.accept(id("scrap_split"), ScrapSplitRecipe.INSTANCE, null);
        // Only 3 of CE's 5 ContainerUpgradeCraftingHandler tiers are item-ready today - the 2
        // mass_storage tiers are skipped (unregistered circuit_* family), see
        // ContainerUpgradeRecipe's own class javadoc and this task's stillBlocked report output.
        output.accept(id("container_upgrade_crate_desh"), ContainerUpgradeRecipe.CRATE_DESH, null);
        output.accept(id("container_upgrade_crate_tungsten"), ContainerUpgradeRecipe.CRATE_TUNGSTEN, null);
        output.accept(id("container_upgrade_safe"), ContainerUpgradeRecipe.SAFE, null);
    }
}
