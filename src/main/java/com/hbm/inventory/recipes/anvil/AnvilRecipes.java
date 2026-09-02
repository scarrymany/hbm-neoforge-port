package com.hbm.inventory.recipes.anvil;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.machine.ItemMold;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CE {@code AnvilRecipes.java}. Smithing {@code :59-131} + construction {@code :140+}.
 * Rows whose I/O is AIR are skipped — no fake ids. Shell/pipe/stamp/recycle rows land when
 * the flattened autogen / already-registered I/O exists.
 * Ported: 18 {@link AnvilSmithingHotRecipe} (dusted steel purity chain 0→9→chainsteel + meteorite + cobalt decoration + meteorite_sword_reforged) + 18
 * {@link AnvilSmithingMold} + {@link AnvilSmithingCyanideRecipe} + {@link AnvilSmithingRenameRecipe}
 * + 9 mold-construction rows ({@code :626-635}).
 */
public final class AnvilRecipes {

    public static final List<AnvilSmithingRecipe> SMITHING = new ArrayList<>();
    public static final List<AnvilConstructionRecipe> CONSTRUCTION = new ArrayList<>();

    private static boolean registered;

    private AnvilRecipes() {
    }

    public static synchronized void register() {
        if (registered) return;
        registered = true;
        registerSmithing();
        registerConstruction();
    }

    public static List<AnvilSmithingRecipe> getSmithing() {
        register();
        return SMITHING;
    }

    public static List<AnvilConstructionRecipe> getConstruction() {
        register();
        return CONSTRUCTION;
    }

    /**
     * CE {@code :59-131} anvil upgrades + gunmetal + hot (I/O that exists) + dusted steel purity
     * progression + chainsteel + mold smithing + cyanide + rename.
     * {@code :93} wings_murk and {@code :94} flask_infusion are live (flask flattened SHIELD).
     */
    private static void registerSmithing() {
        String[] bases = {"anvil_iron", "anvil_lead"};
        for (String base : bases) {
            smith(1, stack("anvil_steel"), cmp(base), tag("ingots/steel", 10));
            smith(1, stack("anvil_desh"), cmp(base), tag("ingots/desh", 10));
            smith(1, stack("anvil_saturnite"), cmp(base), tag("ingots/saturnite", 10));
            smith(1, stack("anvil_ferrouranium"), cmp(base), cmp("ingot_ferrouranium", 10));
            smith(1, stack("anvil_bismuth_bronze"), cmp(base), tag("ingots/bismuth_bronze", 10));
            smith(1, stack("anvil_arsenic_bronze"), cmp(base), tag("ingots/arsenic_bronze", 10));
            smith(1, stack("anvil_schrabidate"), cmp(base), tag("ingots/schrabidate", 10));
            smith(1, stack("anvil_dnt"), cmp(base), tag("ingots/dineutronium", 10));
            smith(1, stack("anvil_osmiridium"), cmp(base), tag("ingots/osmiridium", 10));
        }
        smith(1, stack("ingot_gunmetal"), tag("ingots/copper"), tag("ingots/aluminum"));

        // CE :76-80 — dusted steel purity progression (0→1, 1→2, ..., 8→9) + chainsteel
        for (int i = 0; i < 9; i++) {
            smithHot(3, "ingot_steel_dusted_" + i, "ingot_steel_dusted_" + i, "ingot_steel_dusted_" + (i + 1));
        }
        smithHot(3, "ingot_steel_dusted_9", "ingot_steel_dusted_9", "ingot_chainsteel");

        // CE :93 — regular smithing, not hot
        smith(1916169, stack("wings_murk"), cmp("wings_limp"), cmp("particle_tachyon"));
        // CE :94 — ItemFlask flattened to single SHIELD item
        smith(4, stack("flask_infusion"), cmp("gem_alexandrite"), cmp("bottle_nuka"));

        // CE :82-91 — meteorite hot + cobalt decoration. Skip cyanide/rename and sword/dusted.
        smithHot(3, "ingot_meteorite", "ingot_meteorite", "ingot_meteorite_forged");
        smithHot(3, "ingot_meteorite_forged", "ingot_meteorite_forged", "blade_meteorite");
        smithHot(3, "meteorite_sword_seared", "ingot_meteorite_forged", "meteorite_sword_reforged");
        smithHot(3, "cobalt_sword", "ingot_meteorite", "cobalt_decorated_sword");
        smithHot(3, "cobalt_pickaxe", "ingot_meteorite", "cobalt_decorated_pickaxe");
        smithHot(3, "cobalt_axe", "ingot_meteorite", "cobalt_decorated_axe");
        smithHot(3, "cobalt_shovel", "ingot_meteorite", "cobalt_decorated_shovel");
        smithHot(3, "cobalt_hoe", "ingot_meteorite", "cobalt_decorated_hoe");

        // CE :98-127 — mold smithing. Output is mold + MOLD_ID, not a flatten item.
        moldSmith(0, tag("nuggets/gold"), "nugget", 1);
        moldSmith(1, tag("billets/uranium"), "billet", 1);
        moldSmith(2, tag("ingots/iron"), "ingot", 1);
        moldSmith(3, tag("plates/iron"), "plate", 1);
        moldSmith(19, tag("plates_triple/iron"), "plateTriple", 1);
        moldSmith(15, tag("plates_triple/iron", 3), "plateTriple", 3);
        moldSmith(4, tag("wires/copper"), "wireFine", 1);
        moldSmith(5, cmp("blade_titanium"), new ItemStack[]{stack("blade_titanium"), stack("blade_tungsten")});
        moldSmith(6, cmp("blades_steel"), new ItemStack[]{stack("blades_steel"), stack("blades_titanium")});
        moldSmith(7, cmp("stamp_iron_flat"), new ItemStack[]{
                stack("stamp_stone_flat"),
                stack("stamp_iron_flat"),
                stack("stamp_steel_flat"),
                stack("stamp_titanium_flat"),
                stack("stamp_obsidian_flat")
        });
        moldSmith(8, tag("shells/steel"), "shell", 1);
        moldSmith(9, tag("pipes/steel"), "ntmpipe", 1);
        moldSmith(10, tag("ingots/iron", 9), "ingot", 9);
        moldSmith(11, tag("plates/iron", 9), "plate", 9);
        moldSmith(12, tag("storage_blocks/iron"), "block", 1);
        moldSmith(13, cmp("pipes_steel"), new ItemStack[]{stack("pipes_steel")});
        moldSmith(20, tag("dense_wires/red_copper"), "wireDense", 1);
        moldSmith(21, tag("dense_wires/red_copper", 9), "wireDense", 9);

        SMITHING.add(new AnvilSmithingCyanideRecipe());
        SMITHING.add(new AnvilSmithingRenameRecipe());
    }

    private static void registerConstruction() {
        // CE :142-154 plates (Press already stamps these; anvil is the CE second path)
        plate("ingots/iron", "plate_iron");
        plate("ingots/gold", "plate_gold");
        plate("ingots/titanium", "plate_titanium");
        plate("ingots/aluminum", "plate_aluminium");
        plate("ingots/steel", "plate_steel");
        plate("ingots/lead", "plate_lead");
        plate("ingots/copper", "plate_copper");
        plate("ingots/gunmetal", "plate_gunmetal");
        plate("ingots/weaponsteel", "plate_weaponsteel");
        plate("ingots/saturnite", "plate_saturnite");
        plate("ingots/dura_steel", "plate_dura_steel");
        plate("ingots/schrabidium", "plate_schrabidium");
        plate("ingots/cmb_steel", "plate_combine_steel");

        // CE :156-160 wire autogen — same Mats WIRE set Press uses
        for (NTMMaterial mat : Mats.orderedList) {
            if (!mat.getAutogen().contains(MaterialShapes.WIRE)) continue;
            Item wire = item(MaterialShapes.WIRE.buildRegistryName(mat));
            if (wire == Items.AIR) continue;
            construct(4, new ItemStack(wire, 8), tag("ingots/" + mat.getRegistryName()));
        }

        // CE :162-166 dust → gem
        construct(3, new ItemStack(Items.COAL), tag("dusts/coal"));
        construct(3, new ItemStack(Items.QUARTZ), tag("dusts/quartz"));
        construct(3, new ItemStack(Items.LAPIS_LAZULI), tag("dusts/lapis"));
        construct(3, new ItemStack(Items.DIAMOND), tag("dusts/diamond"));
        construct(3, new ItemStack(Items.EMERALD), tag("dusts/emerald"));

        registerConstructionRecipes();
    }

    /** CE {@code registerConstructionRecipes} :176-566 — machines / deco / coils / armor / fuel plates. */
    private static void registerConstructionRecipes() {
        // :182-188 deco cubes
        construct(1, stack("deco_aluminium", 4), tag("ingots/aluminum"));
        construct(1, stack("deco_beryllium", 4), tag("ingots/beryllium"));
        construct(1, stack("deco_lead", 4), tag("ingots/lead"));
        construct(1, stack("deco_red_copper", 4), tag("ingots/red_copper"));
        construct(1, stack("deco_steel", 4), tag("ingots/steel"));
        construct(1, stack("deco_titanium", 4), tag("ingots/titanium"));
        construct(1, stack("deco_tungsten", 4), tag("ingots/tungsten"));
        construct(1, stack("deco_asbestos", 4), cmp("ingot_asbestos"));

        // :205-217 coils / motors
        construct(1, stack("coil_copper_torus"), cmp("coil_copper", 2));
        construct(1, stack("coil_gold_torus"), cmp("coil_gold", 2));
        construct(1, stack("motor", 2), tag("plates/iron", 2), cmp("coil_copper"), cmp("coil_copper_torus"));
        Item goldDense = item(MaterialShapes.DENSEWIRE.buildRegistryName(Mats.MAT_GOLD));
        if (goldDense != Items.AIR) {
            construct(3, stack("motor_desh"), cmp("motor"), cmp("ingot_polymer", 2), tag("ingots/desh", 2),
                    new ComparableStack(goldDense));
        }

        // :219-225 blast furnace
        construct(1, stack("machine_blast_furnace"),
                new ComparableStack(Blocks.STONE_BRICKS, 4),
                cmp("ingot_firebrick", 32),
                tag("plates/copper", 8));

        // :237-243 assembly machine (cheap vacuum-tube path, not expensive-mode analog)
        construct(2, stack("machine_assembly_machine"),
                tag("ingots/steel", 8),
                tag("plates/copper", 4),
                cmp("motor", 2),
                cmp("circuit_vacuum_tube", 4));

        // :262-267 heater_firebox
        construct(2, stack("heater_firebox"),
                new ComparableStack(Blocks.FURNACE),
                tag("plates/steel", 8),
                tag("ingots/copper", 8));

        // :269-274 heater_oven
        construct(2, stack("heater_oven"),
                cmp("ingot_firebrick", 16),
                tag("plates/steel", 4),
                tag("ingots/copper", 8));

        // :276-281 ashpit
        construct(2, stack("machine_ashpit"),
                new ComparableStack(Blocks.STONE, 8),
                tag("plates/steel", 2),
                tag("ingots/iron", 4));

        // :283-289 heater_oilburner
        construct(2, stack("heater_oilburner"),
                cmp("tank_steel", 4),
                tag("pipes/steel", 3),
                tag("ingots/titanium", 12),
                tag("ingots/copper", 8));

        // :291-298 heater_electric
        construct(3, stack("heater_electric"),
                cmp("plate_polymer", 4),
                tag("ingots/copper", 8),
                tag("plates/steel", 8),
                cmp("coil_tungsten", 8),
                cmp("circuit_basic"));

        // :300-307 heater_heatex
        construct(3, stack("heater_heatex"),
                cmp("ingot_rubber", 4),
                tag("ingots/copper", 16),
                tag("plates/steel", 16),
                tag("pipes/steel", 3));

        // :309-315 furnace_steel
        construct(2, stack("furnace_steel"),
                new ComparableStack(Blocks.STONE_BRICKS, 16),
                tag("ingots/iron", 4),
                tag("plates/steel", 16),
                tag("ingots/copper", 8),
                cmp("steel_grate", 16));

        // :317-323 furnace_combination
        construct(2, stack("furnace_combination"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                new OreDictStack(ItemTags.LOGS, 16),
                tag("cast_plates/copper", 2),
                new ComparableStack(Items.BRICK, 16));

        // :325-331 machine_rotary_furnace
        construct(2, stack("machine_rotary_furnace"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                cmp("ingot_firebrick", 16),
                tag("ingots/iron", 4),
                tag("plates/copper", 8));

        // :333-340 machine_stirling
        construct(2, stack("machine_stirling"),
                new OreDictStack(ItemTags.PLANKS, 16),
                tag("plates/steel", 6),
                tag("ingots/copper", 8),
                cmp("coil_copper", 4),
                cmp("gear_large_iron"));

        // :342-349 machine_stirling_steel
        construct(2, stack("machine_stirling_steel"),
                tag("plates/steel", 16),
                tag("ingots/beryllium", 6),
                tag("ingots/copper", 8),
                cmp("coil_gold", 16),
                cmp("gear_large_steel"));

        // :351-358 machine_steam_engine
        construct(2, stack("machine_steam_engine"),
                cmp("reinforced_stone", 16),
                tag("plates/steel", 12),
                tag("shells/steel", 2),
                cmp("coil_copper", 4),
                cmp("gear_large_iron"));

        // :360-367 machine_sawmill
        construct(2, stack("machine_sawmill"),
                new OreDictStack(ItemTags.PLANKS, 16),
                tag("plates/steel", 6),
                tag("ingots/copper", 8),
                tag("ingots/iron", 4),
                cmp("sawblade"));

        // :369-374 machine_crucible
        construct(2, stack("machine_crucible"),
                cmp("ingot_firebrick", 20),
                tag("ingots/copper", 8),
                tag("plates/steel", 8));

        // :376-381 machine_boiler
        construct(2, stack("machine_boiler"),
                tag("ingots/steel", 4),
                tag("plates/copper", 16),
                cmp("plate_polymer", 8));

        // :383-389 machine_soldering_station
        construct(2, stack("machine_soldering_station"),
                tag("cast_plates/steel", 2),
                cmp("coil_copper", 4),
                tag("bolts/tungsten", 4),
                cmp("circuit_vacuum_tube", 2));

        // :391-397 machine_arc_welder
        construct(2, stack("machine_arc_welder"),
                tag("cast_plates/steel", 4),
                tag("ingots/tungsten", 8),
                cmp("machine_transformer"),
                cmp("arc_electrode", 2));

        // :399-404 machine_industrial_boiler
        construct(3, stack("machine_industrial_boiler"),
                tag("cast_plates/steel", 8),
                tag("ingots/copper", 8),
                cmp("plate_polymer", 4));

        // :406-410 machine_transformer
        construct(2, stack("machine_transformer"),
                tag("plates/steel", 4),
                tag("ingots/iron", 12),
                cmp("coil_copper", 4),
                cmp("coil_gold", 2));

        // :406-413 machine_autosaw
        construct(2, stack("machine_autosaw"),
                tag("plates/steel", 8),
                tag("ingots/iron", 12),
                tag("ingots/copper", 2),
                cmp("circuit_vacuum_tube", 2),
                cmp("sawblade"));

        // :415-421 machine_thresher
        construct(2, stack("machine_thresher"),
                tag("plates/steel", 8),
                tag("ingots/iron", 12),
                tag("ingots/copper", 2),
                cmp("circuit_vacuum_tube"));

        // :423-428 machine_tower_small
        construct(3, stack("machine_tower_small"),
                cmp("brick_concrete", 64),
                new ComparableStack(Blocks.IRON_BARS, 128),
                cmp("machine_condenser", 4));

        // :429-435 machine_tower_large
        construct(4, stack("machine_tower_large"),
                cmp("concrete_smooth", 128),
                cmp("steel_scaffold", 32),
                cmp("machine_condenser", 16),
                tag("pipes/steel", 8));

        // :437-442 wings_limp
        construct(2, stack("wings_limp"),
                new ComparableStack(Items.BONE, 16),
                new ComparableStack(Items.LEATHER, 4),
                new ComparableStack(Items.FEATHER, 24));

        // :444-451 machine_deuterium_extractor
        construct(2, stack("machine_deuterium_extractor"),
                cmp("sulfur", 12),
                tag("shells/steel", 4),
                tag("cast_plates/copper", 6),
                cmp("circuit_basic", 2));

        // :453-462 machine_deuterium_tower (skip SOURGAS fluid requirement for now - CE :460)
        // TODO(CE :453-462): machine_deuterium_tower requires Fluids.SOURGAS.getDict(1_000) × 8

        // :464-471 red_pylon_large
        construct(2, stack("red_pylon_large"),
                cmp("concrete_smooth", 2),
                cmp("steel_scaffold", 8),
                cmp("plate_polymer", 8),
                cmp("coil_copper", 4));

        // :473-480 substation
        construct(2, stack("substation", 2),
                cmp("concrete_smooth", 8),
                tag("ingots/steel", 8),
                cmp("plate_polymer", 12),
                cmp("coil_copper", 8));

        // :482-488 chimney_brick
        construct(2, stack("chimney_brick"),
                tag("plates/steel", 4),
                new ComparableStack(Blocks.BRICKS, 16),
                cmp("steel_grate", 2));

        // :490-497 bm_power_box (uses wire_dense metadata Mats.MAT_MINGRADE)
        Item mingradeDense = item(MaterialShapes.DENSEWIRE.buildRegistryName(Mats.MAT_MINGRADE));
        if (mingradeDense != Items.AIR) {
            construct(5, stack("bm_power_box"),
                    cmp("steel_wall", 2),
                    tag("dusts/redstone", 4),
                    new ComparableStack(Blocks.LEVER, 2),
                    new ComparableStack(mingradeDense, 3));
        }

        // :499-506 chimney_industrial
        construct(3, stack("chimney_industrial"),
                tag("plates/steel", 16),
                cmp("concrete_smooth", 64),
                cmp("steel_grate", 4),
                cmp("filter_coal", 4));

        // :508-513 yellow_barrel
        construct(3, stack("yellow_barrel"),
                cmp("tank_steel"),
                tag("plates/lead", 2),
                cmp("nuclear_waste", 10));

        // :514-519 vitrified_barrel
        construct(3, stack("vitrified_barrel"),
                cmp("tank_steel"),
                tag("plates/lead", 2),
                cmp("nuclear_waste_vitrified", 10));

        // :291-298 heater_electric
        construct(3, stack("heater_electric"),
                cmp("ingot_polymer", 4),
                tag("ingots/copper", 8),
                tag("plates/steel", 8),
                cmp("coil_tungsten", 8),
                cmp("circuit_basic"));

        // :300-306 heater_heatex
        construct(3, stack("heater_heatex"),
                cmp("ingot_rubber", 4),
                tag("ingots/copper", 16),
                tag("plates/steel", 16));

        // :308-315 furnace_steel
        construct(2, stack("furnace_steel"),
                new ComparableStack(Blocks.STONE_BRICKS, 16),
                tag("ingots/iron", 4),
                tag("plates/steel", 16),
                tag("ingots/copper", 8),
                cmp("steel_grate", 16));

        // :317-323 furnace_combination
        construct(2, stack("furnace_combination"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                new ComparableStack(Items.OAK_LOG, 16),
                tag("plates/copper", 2),
                new ComparableStack(Items.BRICK, 16));

        // :325-331 rotary furnace
        construct(2, stack("machine_rotary_furnace"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                cmp("ingot_firebrick", 16),
                tag("ingots/iron", 4),
                tag("plates/copper", 8));

        // :333-340 stirling
        construct(2, stack("machine_stirling"),
                new ComparableStack(Items.OAK_PLANKS, 16),
                tag("plates/steel", 6),
                tag("ingots/copper", 8),
                cmp("coil_copper", 4),
                cmp("gear_bronze"));

        // :342-349 stirling steel
        construct(2, stack("machine_stirling_steel"),
                tag("plates/steel", 16),
                tag("ingots/beryllium", 6),
                tag("ingots/copper", 8),
                cmp("coil_gold", 16),
                cmp("gear_steel"));

        // :369-374 crucible
        construct(2, stack("machine_crucible"),
                cmp("ingot_firebrick", 20),
                tag("ingots/copper", 8),
                tag("plates/steel", 8));

        // :376-381 CE machine_boiler → port id heat_boiler (registerLeftoverMachines)

        // :383-389 soldering
        construct(2, stack("machine_soldering_station"),
                tag("plates/steel", 2),
                cmp("coil_copper", 4),
                cmp("circuit_vacuum_tube", 2));

        // :391-397 arc welder
        construct(2, stack("machine_arc_welder"),
                tag("plates/steel", 4),
                tag("ingots/tungsten", 8),
                cmp("arc_electrode", 2));

        // :399-404 industrial boiler
        construct(3, stack("machine_industrial_boiler"),
                tag("plates/steel", 8),
                tag("ingots/copper", 8),
                cmp("ingot_polymer", 4));

        // :528-551 armor / desh / bismuth plates
        construct(3, stack("plate_desh", 4),
                tag("ingots/desh", 4),
                tag("dusts/polymer", 2),
                tag("ingots/dura_steel"));
        construct(4, stack("plate_bismuth"),
                cmp("nugget_bismuth", 2),
                tag("billets/uranium238", 2),
                tag("dusts/niobium"));
        construct(2, stack("plate_armor_titanium"),
                tag("plates/titanium", 2),
                tag("ingots/steel"));
        construct(3, stack("plate_armor_ajr", 2),
                tag("plates/iron", 6),
                tag("ingots/niobium"),
                cmp("plate_armor_titanium"));
        construct(4, stack("plate_armor_hev"),
                tag("plates/dura_steel", 4),
                cmp("plate_armor_titanium"),
                tag("wires/tungsten", 8));
        construct(4, stack("plate_armor_lunar"),
                tag("ingots/starmetal"),
                tag("wires/magnetized_tungsten", 8));

        // :560-566 fuel plates
        construct(4, stack("plate_fuel_u233"), cmp("ingot_u233"));
        construct(4, stack("plate_fuel_u235"), cmp("ingot_u235"));
        construct(4, stack("plate_fuel_mox"), cmp("ingot_mox_fuel"));
        construct(4, stack("plate_fuel_pu239"), cmp("ingot_pu239"));
        construct(4, stack("plate_fuel_sa326"), cmp("ingot_schrabidium"));
        construct(4, stack("plate_fuel_ra226be"), cmp("billet_ra226be"));
        construct(4, stack("plate_fuel_pu238be"), cmp("billet_pu238be"));

        // :508-519 barrels
        construct(3, stack("yellow_barrel"),
                cmp("tank_steel"),
                tag("plates/lead", 2),
                cmp("nuclear_waste", 10));
        construct(3, stack("vitrified_barrel"),
                cmp("tank_steel"),
                tag("plates/lead", 2),
                cmp("nuclear_waste_vitrified", 10));

        registerShellsPipes();
        registerLeftoverMachines();
        registerConstructionStamps();
        registerConstructionRecycling();
    }

    /** CE {@code :194-203} Mats SHELL/PIPE autogen — ids are {@code {mat}_{shell|pipe}}. */
    private static void registerShellsPipes() {
        construct(1, stack("titanium_shell"), tag("plates/titanium", 4));
        construct(1, stack("copper_shell"), tag("plates/copper", 4));
        construct(1, stack("aluminum_shell"), tag("plates/aluminum", 4));
        construct(1, stack("steel_shell"), tag("plates/steel", 4));
        construct(1, stack("weaponsteel_shell"), tag("plates/weaponsteel", 4));
        construct(1, stack("saturnite_shell"), tag("plates/saturnite", 4));

        construct(1, stack("iron_pipe"), tag("ingots/iron", 3));
        construct(1, stack("copper_pipe"), tag("plates/copper", 3));
        construct(1, stack("aluminum_pipe"), tag("plates/aluminum", 3));
        construct(1, stack("lead_pipe"), tag("plates/lead", 3));
        construct(1, stack("steel_pipe"), tag("plates/steel", 3));
        construct(1, stack("durasteel_pipe"), tag("plates/dura_steel", 3));
        construct(1, stack("rubber_pipe"), tag("ingots/rubber", 3));
    }

    /** CE construction leftovers whose I/O is now registered (heat_boiler = CE {@code machine_boiler} field). */
    private static void registerLeftoverMachines() {
        // :229-235 CE machine_rockmill → port id machine_rock_mill
        construct(2, stack("machine_rock_mill"),
                new ComparableStack(Blocks.STONE, 16),
                tag("plates/steel", 4),
                cmp("copper_pipe"),
                cmp("motor"));
        // :253-260 pumps
        construct(2, stack("pump_steam"),
                new ComparableStack(Blocks.COBBLESTONE, 8),
                new ComparableStack(Items.OAK_PLANKS, 16),
                tag("plates/copper", 8),
                cmp("lead_pipe", 2));
        construct(3, stack("pump_electric"),
                new ComparableStack(Blocks.STONE_BRICKS, 8),
                tag("plates/steel", 16),
                cmp("lead_pipe", 4),
                cmp("motor", 2),
                cmp("circuit_vacuum_tube", 4));
        // :376-381 CE machine_boiler field registers as heat_boiler
        construct(2, stack("heat_boiler"),
                tag("ingots/steel", 4),
                tag("plates/copper", 16),
                cmp("plate_polymer", 8));
        // :351-358 steam engine
        construct(2, stack("machine_steam_engine"),
                cmp("reinforced_stone", 16),
                tag("plates/steel", 12),
                cmp("steel_shell", 2),
                cmp("coil_copper", 4),
                cmp("gear_bronze"));
        // :360-367 / :406-413 saws — sawblade now registered
        construct(2, stack("machine_sawmill"),
                new ComparableStack(Items.OAK_PLANKS, 16),
                tag("plates/steel", 6),
                tag("ingots/copper", 8),
                tag("ingots/iron", 4),
                cmp("sawblade"));
        construct(2, stack("machine_autosaw"),
                tag("plates/steel", 4),
                tag("ingots/iron", 12),
                tag("ingots/copper", 2),
                cmp("circuit_vacuum_tube", 2),
                cmp("sawblade"));
        construct(2, stack("machine_thresher"),
                tag("plates/steel", 8),
                tag("ingots/iron", 12),
                tag("ingots/copper", 2),
                cmp("circuit_vacuum_tube"));
        // :423-435 towers
        construct(3, stack("machine_tower_small"),
                cmp("brick_concrete", 64),
                new ComparableStack(Blocks.IRON_BARS, 128),
                cmp("machine_condenser", 4));
        construct(4, stack("machine_tower_large"),
                cmp("concrete_smooth", 128),
                cmp("steel_scaffold", 32),
                cmp("machine_condenser", 16),
                cmp("steel_pipe", 8));
        // :437-442
        construct(2, stack("wings_limp"),
                new ComparableStack(Items.BONE, 16),
                new ComparableStack(Items.LEATHER, 4),
                new ComparableStack(Items.FEATHER, 24));
        // :444-451
        construct(2, stack("machine_deuterium_extractor"),
                cmp("sulfur", 12),
                cmp("steel_shell", 4),
                tag("plates/copper", 6),
                cmp("circuit_basic", 2));
        // :453-462 machine_deuterium_tower — Fluids.SOURGAS.getDict fluid AStack.
        // Port AStack has no fluid. TODO(CE: AnvilRecipes.java:453-462)
        // :552-558
        construct(5, stack("missile_doomsday"),
                cmp("missile_doomsday_rusted"),
                OreDictStack.ofHbmTag("any_hardplastic", 8),
                cmp("aluminum_plate_sextuple", 2),
                cmp("billet_pu239", 3));
        // :464-480 pylons
        construct(2, stack("red_pylon_large"),
                cmp("concrete", 2),
                cmp("steel_scaffold", 8),
                cmp("plate_polymer", 8),
                cmp("coil_copper", 4));
        construct(2, stack("substation", 2),
                cmp("concrete", 8),
                tag("ingots/steel", 8),
                cmp("plate_polymer", 12),
                cmp("coil_copper", 8));
        // :482-506 chimneys
        construct(2, stack("chimney_brick"),
                tag("plates/steel", 4),
                new ComparableStack(Blocks.BRICKS, 16),
                cmp("steel_grate", 2));
        construct(3, stack("chimney_industrial"),
                tag("plates/steel", 16),
                cmp("concrete", 64),
                cmp("steel_grate", 4),
                cmp("filter_coal", 4));
        // :568-582 ducts / cable (one flatten each, not CE meta 0-14)
        construct(2, stack("fluid_duct_box"), tag("plates/iron"));
        construct(2, stack("fluid_duct_exhaust", 8), tag("plates/iron"), cmp("plate_polymer"));
        construct(2, stack("red_cable_box", 16), tag("ingots/red_copper"), cmp("plate_polymer"));
        // :612-616 one cassette (port flattened siren_track, not per-track meta)
        construct(2, stack("siren_track"), tag("plates/steel"), cmp("plate_polymer"));
        // :178-180 annihilator
        construct(2, stack("machine_annihilator"),
                new ComparableStack(Blocks.STONE_BRICKS, 16),
                cmp("ingot_firebrick", 16),
                tag("ingots/iron", 8),
                tag("ingots/copper", 8));
        // :190-192
        construct(1916169, stack("depth_dnt"), tag("ingots/dineutronium", 4), cmp("depth_brick"));
        // :490-497
        construct(5, stack("bm_power_box"),
                cmp("steel_wall", 2),
                new ComparableStack(Items.REDSTONE, 4),
                new ComparableStack(Items.LEVER, 2),
                cmp("mingrade_dense_wire", 3));
        // :521-526
        construct(3, stack("demon_core_open"), cmp("man_core"), tag("ingots/beryllium", 4), cmp("screwdriver"));
        // :547-551 — inputs exist; meteorite_forged is ItemHot (not anvil-hot recipe)
        construct(6, stack("plate_armor_fau"),
                cmp("ingot_meteorite_forged", 4),
                tag("ingots/desh"),
                cmp("billet_yharonite"));
        construct(7, stack("plate_armor_dnt"),
                cmp("plate_dineutronium", 4),
                cmp("particle_sparkticle"),
                cmp("plate_armor_fau", 6));
    }

    /** CE {@code registerConstructionStamps} :585-609 + ammo stamps :621-624. */
    private static void registerConstructionStamps() {
        construct(1, stack("stamp_stone_plate"), cmp("stamp_stone_flat"));
        construct(1, stack("stamp_stone_wire"), cmp("stamp_stone_flat"));
        construct(1, stack("stamp_stone_circuit"), cmp("stamp_stone_flat"));
        construct(1, stack("stamp_iron_plate"), cmp("stamp_iron_flat"));
        construct(1, stack("stamp_iron_wire"), cmp("stamp_iron_flat"));
        construct(1, stack("stamp_iron_circuit"), cmp("stamp_iron_flat"));
        construct(2, stack("stamp_steel_plate"), cmp("stamp_steel_flat"));
        construct(2, stack("stamp_steel_wire"), cmp("stamp_steel_flat"));
        construct(2, stack("stamp_steel_circuit"), cmp("stamp_steel_flat"));
        construct(2, stack("stamp_titanium_plate"), cmp("stamp_titanium_flat"));
        construct(2, stack("stamp_titanium_wire"), cmp("stamp_titanium_flat"));
        construct(2, stack("stamp_titanium_circuit"), cmp("stamp_titanium_flat"));
        construct(2, stack("stamp_obsidian_plate"), cmp("stamp_obsidian_flat"));
        construct(2, stack("stamp_obsidian_wire"), cmp("stamp_obsidian_flat"));
        construct(2, stack("stamp_obsidian_circuit"), cmp("stamp_obsidian_flat"));
        construct(3, stack("stamp_desh_plate"), cmp("stamp_desh_flat"));
        construct(3, stack("stamp_desh_wire"), cmp("stamp_desh_flat"));
        construct(3, stack("stamp_desh_circuit"), cmp("stamp_desh_flat"));
        construct(2, stack("stamp_9"), cmp("stamp_iron_flat"), tag("ingots/gunmetal", 2));
        construct(2, stack("stamp_50"), cmp("stamp_iron_flat"), tag("ingots/gunmetal", 2));
        construct(4, stack("stamp_desh_9"), cmp("stamp_desh_flat"), tag("ingots/weaponsteel", 4));
        construct(4, stack("stamp_desh_50"), cmp("stamp_desh_flat"), tag("ingots/weaponsteel", 4));

        // CE :626-635 — mold construction (output = mold with MOLD_ID).
        // 16/17 are CE construction-only ids (c9/c50), not in ItemMold.MOLDS foundry list.
        construct(1, moldStack(16), cmp("mold_base"), tag("ingots/iron", 2));
        construct(1, moldStack(17), cmp("mold_base"), tag("ingots/iron", 2));
        construct(2, moldStack(22), cmp("mold_base"), tag("ingots/steel", 4));
        construct(2, moldStack(23), cmp("mold_base"), tag("ingots/steel", 4));
        construct(2, moldStack(24), cmp("mold_base"), tag("ingots/steel", 4));
        construct(2, moldStack(25), cmp("mold_base"), tag("ingots/steel", 4));
        construct(2, moldStack(26), cmp("mold_base"), tag("ingots/steel", 4));
        construct(2, moldStack(27), cmp("mold_base"), tag("ingots/steel", 4));
        construct(2, moldStack(28), cmp("mold_base"), tag("ingots/steel", 4));
    }

    /** CE {@code registerConstructionRecycling} :640+ — only rows whose I/O exists. */
    private static void registerConstructionRecycling() {
        recycle(1, cmp("deco_titanium", 4), out("ingot_titanium"));
        recycle(1, cmp("deco_red_copper", 4), out("ingot_red_copper"));
        recycle(1, cmp("deco_tungsten", 4), out("ingot_tungsten"));
        recycle(1, cmp("deco_aluminium", 4), out("ingot_aluminium"));
        recycle(1, cmp("deco_steel", 4), out("ingot_steel"));
        recycle(1, cmp("deco_rusty_steel", 8), out("ingot_steel"));
        recycle(1, cmp("deco_lead", 4), out("ingot_lead"));
        recycle(1, cmp("deco_beryllium", 4), out("ingot_beryllium"));
        recycle(1, cmp("deco_asbestos", 4), out("ingot_asbestos"));

        recycle(2, cmp("heater_firebox"), out("plate_steel", 8), out("ingot_copper", 6));
        recycle(2, cmp("heater_oven"), out("ingot_firebrick", 16), out("ingot_copper", 8));
        recycle(2, cmp("machine_stirling"), out("plate_steel", 6), out("ingot_copper", 8), out("coil_copper", 4), out("gear_bronze"));
        recycle(2, cmp("machine_stirling_steel"), out("plate_steel", 16), out("ingot_beryllium", 6), out("ingot_copper", 8), out("coil_gold", 16), out("gear_steel"));
        recycle(2, cmp("gear_steel"), out("plate_steel", 8), out("ingot_titanium"));
        recycle(2, cmp("gear_bronze"), out("plate_iron", 8), out("ingot_copper"));
        recycle(2, cmp("fluid_duct_box"), out("plate_iron"));
        recycle(2, cmp("fluid_duct_exhaust", 8), out("plate_iron"), out("plate_polymer"));
        recycle(2, cmp("red_cable_box", 16), out("ingot_red_copper"), out("plate_polymer"));

        // CE :640-654 chunk_ore RARE — port flatten chunk_ore_rare
        recycle(2, cmp("chunk_ore_rare"),
                out("fragment_boron"),
                out("fragment_boron", 1, 0.5F),
                out("fragment_lanthanium", 1, 0.1F),
                out("fragment_cobalt"),
                out("fragment_cobalt", 1, 0.5F),
                out("fragment_cerium", 1, 0.1F),
                out("fragment_neodymium", 1, 0.5F),
                out("fragment_niobium", 1, 0.5F));

        recycle(2, cmp("tape_recorder"), out("ingot_steel"), out("ingot_tungsten", 1, 0.25F));
        recycle(2, cmp("pole_top"),
                out("ingot_tungsten", 3),
                out("ingot_red_copper"),
                out("ingot_beryllium", 2),
                out("ingot_beryllium", 1, 0.5F));
        recycle(2, cmp("pole_satellite_receiver"),
                out("ingot_steel", 3),
                out("ingot_steel", 2, 0.5F),
                out("circuit_vacuum_tube", 1, 0.5F),
                out("mingrade_wire"));
        recycle(1, cmp("filing_cabinet"),
                out("plate_steel", 2),
                out("plate_steel", 2, 0.5F),
                out("plate_polymer", 2, 0.25F),
                out("scrap"));

        recycle(2, cmp("deco_computer_ibm_300pl"),
                out("crt_display"),
                out("scrap", 3),
                out("copper_wire", 4),
                out("circuit_pcb", 2),
                out("circuit_vacuum_tube", 1, 0.5F),
                out("circuit_capacitor", 1, 0.75F),
                out("circuit_capacitor", 1, 0.5F),
                out("circuit_analog", 1, 0.1F));
        for (String crt : new String[]{"deco_crt_clean", "deco_crt_broken", "deco_crt_blinking", "deco_crt_bsod"}) {
            recycle(2, cmp(crt),
                    out("crt_display"),
                    out("scrap", 2),
                    out("copper_wire", 2),
                    out("gold_wire", 2, 0.25F),
                    out("circuit_vacuum_tube", 1, 0.25F));
        }
        recycle(2, cmp("deco_toaster_iron"),
                out("plate_iron", 3),
                out("scrap"),
                out("coil_tungsten"),
                vanilla(Items.BREAD, 1, 0.5F),
                out("fusion_core", 1, 0.01F));
        recycle(2, cmp("deco_toaster_steel"),
                out("plate_steel", 3),
                out("scrap"),
                out("coil_tungsten", 2),
                vanilla(Items.BREAD, 1, 0.5F),
                out("battery_sc_ra226", 1, 0.1F),
                out("fusion_core", 1, 0.05F));
        recycle(2, cmp("deco_toaster_wood"),
                out("powder_sawdust", 4),
                out("scrap"),
                out("coil_tungsten", 4),
                vanilla(Items.BREAD, 1, 0.5F),
                out("fusion_core", 1, 0.5F),
                out("fusion_core", 1, 0.5F),
                out("gem_alexandrite", 1, 0.25F),
                out("flame_pony", 1, 0.01F));

        recycle(2, cmp("pile_rod_uranium"), out("billet_uranium", 3), out("plate_iron", 2));
        recycle(2, cmp("pile_rod_source"), out("billet_ra226be", 3), out("plate_iron", 2));
        recycle(2, cmp("pile_rod_boron"), out("ingot_boron", 2), vanilla(Items.STICK, 2, 1F));
        recycle(2, cmp("pile_rod_detector"), out("ingot_boron", 2), out("motor"), out("circuit_vacuum_tube"));
        recycle(2, cmp("pile_rod_lithium"), out("lithium"), out("cell"));
        recycle(2, cmp("pile_rod_plutonium"), out("billet_pu_mix", 2), out("billet_uranium"), out("plate_iron", 2));
        recycle(2, cmp("pile_rod_pu239"), out("billet_pu239"), out("billet_pu_mix"), out("billet_uranium"), out("plate_iron", 2));

        // CE :880-895 pile_rod meta EnumPileRod — port flatten pile_rod_mk2_*
        construct(2, stack("pile_rod_mk2_ra226be"), cmp("billet_ra226be", 3));
        construct(2, stack("pile_rod_mk2_po210be"), cmp("billet_po210be", 3));
        construct(2, stack("pile_rod_mk2_zr"), cmp("billet_zirconium", 3));
        construct(2, stack("pile_rod_mk2_nu"), cmp("billet_uranium", 3));

        recycle(4, cmp("rbmk_moderator"), out("rbmk_blank"), out("block_graphite", 4));
        recycle(4, cmp("rbmk_absorber"), out("rbmk_blank"), out("ingot_boron", 8));
        recycle(4, cmp("rbmk_reflector"), out("rbmk_blank"), out("neutron_reflector", 8));
        recycle(4, cmp("rbmk_control"), out("rbmk_absorber"), out("ingot_graphite", 2), out("motor", 2));
        recycle(4, cmp("rbmk_control_mod"), out("rbmk_control"), out("block_graphite", 4), out("nugget_bismuth", 4));
        recycle(4, cmp("rbmk_control_auto"), out("rbmk_control"), out("circuit_advanced"), out("crt_display"));
        recycle(4, cmp("rbmk_rod_reasim"), out("rbmk_blank"), out("ingot_zirconium", 4), out("steel_shell", 2));
        recycle(4, cmp("rbmk_rod_reasim_mod"), out("rbmk_rod_reasim"), out("block_graphite", 4), out("ingot_tcalloy", 4));
        recycle(4, cmp("rbmk_outgasser"), out("rbmk_blank"), out("steel_grate", 6), out("tank_steel"), vanilla(Items.HOPPER, 1, 1F));
        recycle(4, cmp("rbmk_storage"), out("rbmk_blank"), out("crate_steel", 2));
        recycle(4, cmp("rbmk_rod"), out("rbmk_blank"), out("steel_shell", 2));
        recycle(4, cmp("rbmk_rod_mod"), out("rbmk_rod"), out("block_graphite", 4), out("nugget_bismuth", 4));
        recycle(4, cmp("rbmk_boiler"), out("rbmk_blank"), out("copper_pipe", 6), out("copper_shell", 2));
        recycle(4, cmp("rbmk_cooler"), out("rbmk_blank"), out("steel_grate", 4), out("plate_polymer", 4));
        recycle(4, cmp("reactor_research"),
                out("ingot_steel", 8),
                out("ingot_tcalloy", 4),
                out("motor_desh", 2),
                out("ingot_boron", 5),
                out("plate_lead", 8),
                out("crt_display", 3),
                out("circuit_basic"),
                out("circuit_basic", 1, 0.5F));

        recycle(3, cmp("machine_turbine"), out("turbine_titanium"), out("coil_copper", 2), out("ingot_steel", 4));
        recycle(3, cmp("yellow_barrel"), out("tank_steel"), out("plate_lead", 2), out("nuclear_waste", 10));
        recycle(3, cmp("vitrified_barrel"), out("tank_steel"), out("plate_lead", 2), out("nuclear_waste_vitrified", 10));
        recycle(1, cmp("egg_glyphid"),
                out("glyphid_meat", 2),
                out("glyphid_meat", 1, 0.5F),
                vanilla(Items.BONE, 1, 0.75F),
                vanilla(Items.EXPERIENCE_BOTTLE, 1, 0.5F));
        recycle(1, cmp("fusion_heater"),
                out("steel_pipe", 4),
                out("copper_pipe", 2),
                out("circuit_analog", 1, 0.5F));
        recycle(1, cmp("fusion_hatch"),
                out("steel_pipe", 4),
                out("copper_pipe", 4),
                out("circuit_analog", 1, 0.75F));
    }

    private static void plate(String ingotTag, String out) {
        construct(3, stack(out), tag(ingotTag));
    }

    private static void smith(int tier, ItemStack out, AStack left, AStack right) {
        if (empty(out) || empty(left) || empty(right)) return;
        SMITHING.add(new AnvilSmithingRecipe(tier, out, left, right));
    }

    private static void smithHot(int tier, String left, String right, String out) {
        ItemStack o = stack(out);
        AStack l = cmp(left);
        AStack r = cmp(right);
        if (empty(o) || empty(l) || empty(r)) return;
        SMITHING.add(new AnvilSmithingHotRecipe(tier, o, l, r));
    }

    private static void moldSmith(int meta, AStack demo, String prefix, int prefixCount) {
        if (item("mold") == Items.AIR || item("mold_base") == Items.AIR) return;
        SMITHING.add(new AnvilSmithingMold(meta, demo, prefix, prefixCount));
    }

    private static void moldSmith(int meta, AStack demo, ItemStack[] matches) {
        if (item("mold") == Items.AIR || item("mold_base") == Items.AIR) return;
        boolean any = false;
        for (ItemStack s : matches) {
            if (!empty(s)) any = true;
        }
        if (!any) return;
        SMITHING.add(new AnvilSmithingMold(meta, demo, matches));
    }

    private static ItemStack moldStack(int id) {
        Item mold = item("mold");
        if (mold == Items.AIR) return ItemStack.EMPTY;
        ItemStack stack = new ItemStack(mold);
        ItemMold.setMoldId(stack, id);
        return stack;
    }

    private static void construct(int tier, ItemStack out, AStack... in) {
        if (empty(out)) return;
        for (AStack stack : in) {
            if (empty(stack)) return;
        }
        CONSTRUCTION.add(new AnvilConstructionRecipe(in, new AnvilOutput(out)).setTier(tier));
    }

    private static void recycle(int tier, AStack in, AnvilOutput... outs) {
        if (empty(in)) return;
        for (AnvilOutput o : outs) {
            if (o == null || empty(o.stack)) return;
        }
        CONSTRUCTION.add(new AnvilConstructionRecipe(in, outs).setTier(tier));
    }

    private static AnvilOutput out(String id) {
        return new AnvilOutput(stack(id));
    }

    private static AnvilOutput out(String id, int n) {
        return new AnvilOutput(stack(id, n));
    }

    private static AnvilOutput out(String id, int n, float chance) {
        return new AnvilOutput(stack(id, n), chance);
    }

    private static AnvilOutput vanilla(Item item, int n, float chance) {
        return new AnvilOutput(new ItemStack(item, n), chance);
    }

    private static boolean empty(ItemStack stack) {
        return stack == null || stack.isEmpty() || stack.getItem() == Items.AIR;
    }

    private static boolean empty(AStack stack) {
        if (stack == null) return true;
        if (stack instanceof ComparableStack cmp) {
            return cmp.item == null || cmp.item == Items.AIR;
        }
        return false;
    }

    private static OreDictStack tag(String path) {
        return OreDictStack.ofCommonTag(path);
    }

    private static OreDictStack tag(String path, int n) {
        return OreDictStack.ofCommonTag(path, n);
    }

    private static ComparableStack cmp(String id) {
        return new ComparableStack(item(id));
    }

    private static ComparableStack cmp(String id, int n) {
        return new ComparableStack(item(id), n);
    }

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    private static ItemStack stack(String id) {
        return stack(id, 1);
    }

    private static ItemStack stack(String id, int n) {
        Item i = item(id);
        return i == Items.AIR ? ItemStack.EMPTY : new ItemStack(i, n);
    }

    public static final class AnvilConstructionRecipe {
        public final List<AStack> input = new ArrayList<>();
        public final List<AnvilOutput> output = new ArrayList<>();
        public int tierLower;
        public int tierUpper = -1;
        public OverlayType overlay = OverlayType.NONE;

        public AnvilConstructionRecipe(AStack input, AnvilOutput output) {
            this.input.add(input);
            this.output.add(output);
            this.overlay = OverlayType.SMITHING;
        }

        public AnvilConstructionRecipe(AStack[] input, AnvilOutput output) {
            Collections.addAll(this.input, input);
            this.output.add(output);
            this.overlay = OverlayType.CONSTRUCTION;
        }

        public AnvilConstructionRecipe(AStack input, AnvilOutput[] output) {
            this.input.add(input);
            Collections.addAll(this.output, output);
            this.overlay = OverlayType.RECYCLING;
        }

        public AnvilConstructionRecipe setTier(int tier) {
            this.tierLower = tier;
            return this;
        }

        public AnvilConstructionRecipe setTierRange(int lower, int upper) {
            this.tierLower = lower;
            this.tierUpper = upper;
            return this;
        }

        public boolean isTierValid(int tier) {
            if (this.tierUpper == -1) return tier >= this.tierLower;
            return tier >= this.tierLower && tier <= this.tierUpper;
        }

        public AnvilConstructionRecipe setOverlay(OverlayType overlay) {
            this.overlay = overlay;
            return this;
        }

        public ItemStack getDisplay() {
            if (overlay == OverlayType.RECYCLING) {
                for (AStack stack : input) {
                    if (stack instanceof ComparableStack cmp) return cmp.getStack();
                }
            }
            return output.isEmpty() ? ItemStack.EMPTY : output.get(0).stack.copy();
        }
    }

    public static final class AnvilOutput {
        public final ItemStack stack;
        public final float chance;

        public AnvilOutput(ItemStack stack) {
            this(stack, 1F);
        }

        public AnvilOutput(ItemStack stack, float chance) {
            this.stack = stack;
            this.chance = chance;
        }
    }

    public enum OverlayType {
        NONE,
        CONSTRUCTION,
        RECYCLING,
        SMITHING
    }
}
