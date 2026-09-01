#!/usr/bin/env python3
"""Leftover CE vanilla crafts: ToolRecipes / ArmorRecipes / ConsumableRecipes.

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten.
Does not emit unregistered I/O. Does not rewrite assembler JSON. No invented recipes.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "src/main/resources/data/hbm/recipe/ce_craft"
SKIP_RESULTS = {"powder_sawdust", "gem_tantalium", "coil_tungsten"}


def shaped(path, result, count, pattern, key):
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
        "_path": path,
    }


def shapeless(path, result, count, ings):
    ingredients = []
    for i in ings:
        if isinstance(i, dict):
            ingredients.append(i)
        elif isinstance(i, str) and i.startswith("tag:"):
            ingredients.append({"tag": i[4:]})
        else:
            ingredients.append({"item": i})
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": ingredients,
        "result": {"id": result, "count": count},
        "_path": path,
    }


def item(iid):
    return {"item": iid}


def tag(tid):
    return {"tag": tid}


def recipes():
    recs = []

    # --- ToolRecipes.java leftover (datagen skipped: circuits/motors now exist) ---
    # :85 crowbar already generated
    recs.append(shaped("tool/oil_detector", "hbm:oil_detector", 1,
                       ["W I", "WCI", "PPP"],
                       {"W": item("hbm:gold_wire"), "I": item("hbm:ingot_copper"),
                        "C": item("hbm:circuit_analog"), "P": item("hbm:plate_steel")}))
    recs.append(shaped("tool/turret_chip", "hbm:turret_chip", 1,
                       ["WWW", "CPC", "WWW"],
                       {"W": item("hbm:gold_wire"), "P": tag("hbm:any_plastic"),
                        "C": item("hbm:circuit_advanced")}))
    recs.append(shaped("tool/survey_scanner", "hbm:survey_scanner", 1,
                       ["SWS", " G ", "PCP"],
                       {"W": item("hbm:gold_wire"), "P": tag("hbm:any_plastic"),
                        "C": item("hbm:circuit_advanced"), "S": item("hbm:plate_steel"),
                        "G": item("minecraft:gold_ingot")}))
    recs.append(shaped("tool/dosimeter", "hbm:dosimeter", 1,
                       ["WGW", "WCW", "WBW"],
                       {"W": tag("minecraft:planks"), "G": tag("c:glass_panes"),
                        "C": item("hbm:circuit_vacuum_tube"), "B": item("hbm:ingot_beryllium")}))
    recs.append(shaped("tool/pollution_detector", "hbm:pollution_detector", 1,
                       ["SFS", "SCS", " S "],
                       {"S": item("hbm:plate_steel"), "F": item("hbm:filter_coal"),
                        "C": item("hbm:circuit_vacuum_tube")}))
    recs.append(shaped("tool/ore_density_scanner", "hbm:ore_density_scanner", 1,
                       ["VVV", "CSC", "GGG"],
                       {"V": item("hbm:circuit_vacuum_tube"), "C": item("hbm:circuit_capacitor"),
                        "S": item("hbm:circuit_controller_chassis"), "G": item("hbm:plate_gold")}))
    recs.append(shaped("tool/defuser", "hbm:defuser", 1,
                       [" PS", "P P", " P "],
                       {"P": tag("hbm:any_plastic"), "S": item("hbm:plate_steel")}))
    recs.append(shaped("tool/settings_tool", "hbm:settings_tool", 1,
                       [" P ", "PCP", "III"],
                       {"P": item("hbm:plate_iron"), "C": item("hbm:circuit_analog"),
                        "I": item("hbm:plate_polymer")}))
    recs.append(shaped("tool/pipette_laboratory", "hbm:pipette_laboratory", 1,
                       ["  C", " R ", "P  "],
                       {"C": item("hbm:circuit_chip"), "R": item("hbm:ingot_rubber"),
                        "P": item("hbm:pipette_boron")}))
    recs.append(shaped("tool/blowtorch", "hbm:blowtorch", 1,
                       ["CC ", " I ", "CCC"],
                       {"C": item("hbm:plate_copper"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("tool/boltgun", "hbm:boltgun", 1,
                       ["DPS", " RD", " D "],
                       {"D": item("hbm:ingot_dura_steel"),
                        "P": item("hbm:part_generic_piston_pneumatic"),
                        "R": item("hbm:ingot_rubber"), "S": item("hbm:steel_shell")}))
    recs.append(shaped("tool/rangefinder", "hbm:rangefinder", 1,
                       ["GRC", "  S"],
                       {"G": tag("c:glass_panes"), "R": item("minecraft:redstone"),
                        "C": item("hbm:circuit_basic"), "S": item("hbm:plate_steel")}))
    recs.append(shapeless("tool/geiger_block", "hbm:geiger", 1, ["hbm:geiger_counter"]))
    recs.append(shapeless("tool/digamma_diagnostic", "hbm:digamma_diagnostic", 1,
                          ["hbm:geiger_counter", "hbm:billet_polonium", "hbm:ingot_asbestos"]))
    recs.append(shaped("tool/reacher", "hbm:reacher", 1,
                       ["BIB", "P P", "B B"],
                       {"B": item("hbm:tungsten_bolt"), "I": item("hbm:ingot_tungsten"),
                        "P": tag("hbm:any_rubber")}))
    recs.append(shaped("tool/siphon", "hbm:siphon", 1,
                       [" GR", " GR", " G "],
                       {"G": tag("c:glass_blocks"), "R": tag("hbm:any_rubber")}))
    recs.append(shaped("tool/analysis_tool", "hbm:analysis_tool", 1,
                       ["  G", " S ", "S  "],
                       {"G": tag("c:glass_panes"), "S": item("hbm:ingot_steel")}))
    recs.append(shaped("tool/screwdriver_desh", "hbm:screwdriver_desh", 1,
                       ["  I", " I ", "S  "],
                       {"S": tag("hbm:any_plastic"), "I": item("hbm:ingot_desh")}))
    recs.append(shaped("tool/hand_drill_desh", "hbm:hand_drill_desh", 1,
                       [" D", "S ", " S"],
                       {"D": item("hbm:ingot_desh"), "S": tag("hbm:any_plastic")}))
    recs.append(shaped("tool/chemistry_set", "hbm:chemistry_set", 1,
                       ["GIG", "GCG"],
                       {"G": tag("c:glass_blocks"), "I": item("minecraft:iron_ingot"),
                        "C": item("hbm:ingot_copper")}))
    recs.append(shaped("tool/acetylene_torch", "hbm:acetylene_torch", 1,
                       ["SS ", " PS", " T "],
                       {"S": item("hbm:plate_steel"), "P": tag("hbm:any_plastic"),
                        "T": item("hbm:tank_steel")}))
    recs.append(shaped("tool/cmb_sword", "hbm:cmb_sword", 1,
                       ["X", "X", "#"],
                       {"X": item("hbm:ingot_combine_steel"), "#": item("minecraft:stick")}))
    recs.append(shaped("tool/alloy_sword", "hbm:alloy_sword", 1,
                       ["X", "X", "#"],
                       {"X": item("hbm:ingot_advanced_alloy"), "#": item("minecraft:stick")}))
    recs.append(shaped("tool/schrabidium_hoe", "hbm:schrabidium_hoe", 1,
                       ["IW", " S", " S"],
                       {"I": item("hbm:ingot_schrabidium"), "W": item("hbm:desh_hoe"),
                        "S": tag("hbm:any_plastic")}))
    recs.append(shaped("tool/schrabidium_sword", "hbm:schrabidium_sword", 1,
                       ["I", "W", "S"],
                       {"I": item("hbm:block_schrabidium"), "W": item("hbm:desh_sword"),
                        "S": tag("hbm:any_plastic")}))
    recs.append(shaped("tool/starmetal_sword", "hbm:starmetal_sword", 1,
                       [" I ", " B ", "ISI"],
                       {"I": item("hbm:ingot_starmetal"), "S": item("hbm:ring_starmetal"),
                        "B": item("hbm:cobalt_decorated_sword")}))
    recs.append(shaped("tool/matchstick_red", "hbm:matchstick", 24,
                       ["I", "S"],
                       {"I": item("hbm:powder_fire"), "S": item("minecraft:stick")}))

    # --- ArmorRecipes.java leftover (only sets whose I/O exists) ---
    recs.append(shaped("armor/dieselsuit_helmet", "hbm:dieselsuit_helmet", 1,
                       ["W W", "W W", "SCS"],
                       {"W": item("minecraft:red_wool"), "S": item("hbm:ingot_steel"),
                        "C": item("hbm:circuit_analog")}))
    recs.append(shaped("armor/dieselsuit_plate", "hbm:dieselsuit_plate", 1,
                       ["W W", "CDC", "SWS"],
                       {"W": item("minecraft:red_wool"), "S": item("hbm:ingot_steel"),
                        "C": item("hbm:circuit_analog"), "D": item("hbm:machine_diesel")}))
    recs.append(shaped("armor/dieselsuit_legs", "hbm:dieselsuit_legs", 1,
                       ["M M", "S S", "W W"],
                       {"W": item("minecraft:red_wool"), "S": item("hbm:ingot_steel"),
                        "M": item("hbm:motor")}))
    recs.append(shaped("armor/dieselsuit_boots", "hbm:dieselsuit_boots", 1,
                       ["W W", "S S"],
                       {"W": item("minecraft:red_wool"), "S": item("hbm:ingot_steel")}))
    recs.append(shaped("armor/envsuit_helmet", "hbm:envsuit_helmet", 1,
                       ["TCT", "TGT", "RRR"],
                       {"T": item("hbm:plate_titanium"), "C": item("hbm:circuit_chip"),
                        "G": tag("c:glass_panes"), "R": item("hbm:ingot_rubber")}))
    recs.append(shaped("armor/envsuit_plate", "hbm:envsuit_plate", 1,
                       ["T T", "TCT", "RRR"],
                       {"T": item("hbm:plate_titanium"), "C": item("hbm:plate_cast_titanium"),
                        "R": item("hbm:ingot_rubber")}))
    recs.append(shaped("armor/envsuit_legs", "hbm:envsuit_legs", 1,
                       ["TCT", "R R", "T T"],
                       {"T": item("hbm:plate_titanium"), "C": item("hbm:plate_cast_titanium"),
                        "R": item("hbm:ingot_rubber")}))
    recs.append(shaped("armor/envsuit_boots", "hbm:envsuit_boots", 1,
                       ["R R", "T T"],
                       {"T": item("hbm:plate_titanium"), "R": item("hbm:ingot_rubber")}))
    recs.append(shaped("armor/euphemium_helmet", "hbm:euphemium_helmet", 1,
                       ["EEE", "E E"],
                       {"E": item("hbm:plate_euphemium")}))
    recs.append(shaped("armor/euphemium_legs", "hbm:euphemium_legs", 1,
                       ["EEE", "E E", "E E"],
                       {"E": item("hbm:plate_euphemium")}))
    recs.append(shaped("armor/euphemium_boots", "hbm:euphemium_boots", 1,
                       ["E E", "E E"],
                       {"E": item("hbm:plate_euphemium")}))
    recs.append(shaped("armor/euphemium_plate", "hbm:euphemium_plate", 1,
                       ["EWE", "EEE", "EEE"],
                       {"E": item("hbm:plate_euphemium"), "W": item("hbm:watch")}))
    recs.append(shaped("armor/bismuth_helmet", "hbm:bismuth_helmet", 1,
                       ["GPP", "P  ", "FPP"],
                       {"G": item("minecraft:gold_ingot"), "P": item("hbm:plate_bismuth"),
                        "F": item("hbm:rag")}))
    recs.append(shaped("armor/bismuth_boots", "hbm:bismuth_boots", 1,
                       ["W W", "P P"],
                       {"W": item("hbm:gold_wire"), "P": item("hbm:plate_bismuth")}))
    recs.append(shaped("armor/bismuth_legs", "hbm:bismuth_legs", 1,
                       ["FSF", "   ", "FSF"],
                       {"F": item("hbm:rag"), "S": item("hbm:ring_starmetal")}))
    recs.append(shaped("armor/bismuth_plate", "hbm:bismuth_plate", 1,
                       ["RWR", "PCP", "SFS"],
                       {"R": item("hbm:crystal_rare"), "W": item("hbm:gold_wire"),
                        "P": item("hbm:plate_bismuth"), "C": item("hbm:laser_crystal_bismuth"),
                        "S": item("hbm:ring_starmetal"), "F": item("hbm:rag")}))
    recs.append(shaped("armor/jetpack_fly", "hbm:jetpack_fly", 1,
                       ["ACA", "TLT", "D D"],
                       {"A": item("hbm:plate_aluminium"), "C": item("hbm:circuit_basic"),
                        "T": item("hbm:tank_steel"), "L": item("minecraft:leather"),
                        "D": item("hbm:thruster_small")}))
    recs.append(shaped("armor/jetpack_break", "hbm:jetpack_break", 1,
                       ["ICI", "TJT", "I I"],
                       {"C": item("hbm:circuit_basic"), "T": item("hbm:ingot_dura_steel"),
                        "J": item("hbm:jetpack_fly"), "I": item("hbm:plate_polymer")}))
    recs.append(shaped("armor/jetpack_vector", "hbm:jetpack_vector", 1,
                       ["TCT", "MJM", "B B"],
                       {"C": item("hbm:circuit_advanced"), "T": item("hbm:tank_steel"),
                        "J": item("hbm:jetpack_break"), "M": item("hbm:motor"),
                        "B": item("hbm:durasteel_bolt")}))
    recs.append(shaped("armor/jetpack_boost", "hbm:jetpack_boost", 1,
                       ["PCP", "DJD", "PAP"],
                       {"C": item("hbm:circuit_advanced"), "P": item("hbm:plate_saturnite"),
                        "D": item("hbm:ingot_desh"), "J": item("hbm:jetpack_vector"),
                        "A": item("hbm:plate_cast_copper")}))
    recs.append(shaped("armor/steamsuit_helmet", "hbm:steamsuit_helmet", 1,
                       ["DCD", "CXC", " F "],
                       {"D": item("hbm:ingot_desh"), "C": item("hbm:plate_copper"),
                        "X": item("hbm:steel_helmet"), "F": item("hbm:gas_mask_filter")}))
    recs.append(shaped("armor/steamsuit_plate", "hbm:steamsuit_plate", 1,
                       ["C C", "DXD", "CFC"],
                       {"D": item("hbm:ingot_desh"), "C": item("hbm:plate_copper"),
                        "X": item("hbm:steel_plate"), "F": item("hbm:tank_steel")}))
    recs.append(shaped("armor/steamsuit_legs", "hbm:steamsuit_legs", 1,
                       ["CCC", "DXD", "C C"],
                       {"D": item("hbm:ingot_desh"), "C": item("hbm:plate_copper"),
                        "X": item("hbm:steel_legs")}))
    recs.append(shaped("armor/steamsuit_boots", "hbm:steamsuit_boots", 1,
                       ["C C", "DXD"],
                       {"D": item("hbm:ingot_desh"), "C": item("hbm:plate_copper"),
                        "X": item("hbm:steel_boots")}))
    recs.append(shaped("armor/goggles", "hbm:goggles", 1,
                       ["P P", "GPG"],
                       {"G": tag("c:glass_panes"), "P": item("hbm:plate_steel")}))
    recs.append(shaped("armor/gas_mask", "hbm:gas_mask", 1,
                       ["PPP", "GPG", " F "],
                       {"G": tag("c:glass_panes"), "P": item("hbm:plate_steel"),
                        "F": item("hbm:plate_iron")}))
    recs.append(shaped("armor/gas_mask_m65", "hbm:gas_mask_m65", 1,
                       ["PPP", "GPG", " F "],
                       {"G": tag("c:glass_panes"), "P": tag("hbm:any_rubber"),
                        "F": item("hbm:plate_iron")}))
    recs.append(shaped("armor/gas_mask_olde", "hbm:gas_mask_olde", 1,
                       ["PPP", "GPG", " F "],
                       {"G": tag("c:glass_panes"), "P": item("minecraft:leather"),
                        "F": item("minecraft:iron_ingot")}))
    recs.append(shaped("armor/gas_mask_mono", "hbm:gas_mask_mono", 1,
                       [" P ", "PPP", " F "],
                       {"P": tag("hbm:any_rubber"), "F": item("hbm:plate_iron")}))
    recs.append(shaped("armor/mask_of_infamy", "hbm:mask_of_infamy", 1,
                       ["III", "III", " I "],
                       {"I": item("hbm:plate_iron")}))
    recs.append(shaped("armor/ashglasses", "hbm:ashglasses", 1,
                       ["I I", "GPG"],
                       {"I": tag("hbm:any_rubber"), "G": item("hbm:glass_ash"),
                        "P": tag("hbm:any_plastic")}))
    recs.append(shaped("armor/mask_rag", "hbm:mask_rag", 1,
                       ["RRR"], {"R": item("hbm:rag_damp")}))
    recs.append(shaped("armor/mask_piss", "hbm:mask_piss", 1,
                       ["RRR"], {"R": item("hbm:rag_piss")}))
    recs.append(shaped("armor/night_vision", "hbm:night_vision", 1,
                       ["P P", "GCG"],
                       {"P": tag("hbm:any_plastic"), "G": tag("c:glass_blocks"),
                        "C": item("hbm:circuit_basic")}))
    recs.append(shaped("armor/stealth_boy", "hbm:stealth_boy", 1,
                       [" B", "LI", "LC"],
                       {"B": item("minecraft:stone_button"), "L": item("minecraft:leather"),
                        "I": item("hbm:ingot_steel"), "C": item("hbm:circuit_basic")}))
    recs.append(shaped("armor/gas_mask_filter", "hbm:gas_mask_filter", 1,
                       ["I", "F"],
                       {"F": item("hbm:filter_coal"), "I": item("hbm:plate_iron")}))
    recs.append(shaped("armor/gas_mask_filter_combo", "hbm:gas_mask_filter_combo", 1,
                       ["ZCZ", "CFC", "ZCZ"],
                       {"Z": item("hbm:ingot_zirconium"), "C": item("hbm:catalyst_clay"),
                        "F": item("hbm:gas_mask_filter")}))
    recs.append(shaped("armor/gas_mask_filter_mono", "hbm:gas_mask_filter_mono", 1,
                       ["ZZZ", "ZCZ", "ZZZ"],
                       {"Z": item("hbm:nugget_zirconium"), "C": item("hbm:catalyst_clay")}))
    recs.append(shaped("armor/gas_mask_filter_rag", "hbm:gas_mask_filter_rag", 1,
                       ["I", "F"],
                       {"F": item("hbm:rag_damp"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("armor/gas_mask_filter_piss", "hbm:gas_mask_filter_piss", 1,
                       ["I", "F"],
                       {"F": item("hbm:rag_piss"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("armor/machine_armor_table", "hbm:machine_armor_table", 1,
                       ["PPP", "TCT", "TST"],
                       {"P": item("hbm:plate_steel"), "T": item("hbm:ingot_tungsten"),
                        "C": item("minecraft:crafting_table"), "S": item("hbm:block_steel")}))
    recs.append(shaped("armor/zirconium_legs", "hbm:zirconium_legs", 1,
                       ["EEE", "E E", "E E"],
                       {"E": item("hbm:ingot_zirconium")}))
    recs.append(shaped("armor/dnt_helmet", "hbm:dnt_helmet", 1,
                       ["EEE", "EE "],
                       {"E": item("hbm:ingot_dineutronium")}))
    recs.append(shaped("armor/dnt_plate", "hbm:dnt_plate", 1,
                       ["EE ", "EEE", "EEE"],
                       {"E": item("hbm:ingot_dineutronium")}))
    recs.append(shaped("armor/dnt_legs", "hbm:dnt_legs", 1,
                       ["EE ", "EEE", "E E"],
                       {"E": item("hbm:ingot_dineutronium")}))
    recs.append(shaped("armor/dnt_boots", "hbm:dnt_boots", 1,
                       ["  E", "E  ", "E E"],
                       {"E": item("hbm:ingot_dineutronium")}))
    recs.append(shaped("armor/robes_boots", "hbm:robes_boots", 1,
                       ["R R", "P P"],
                       {"R": item("hbm:rag"), "P": tag("hbm:any_rubber")}))
    recs.append(shaped("armor/steel_helmet", "hbm:steel_helmet", 1,
                       ["XXX", "X X"], {"X": item("hbm:ingot_steel")}))
    recs.append(shaped("armor/steel_plate", "hbm:steel_plate", 1,
                       ["X X", "XXX", "XXX"], {"X": item("hbm:ingot_steel")}))
    recs.append(shaped("armor/steel_legs", "hbm:steel_legs", 1,
                       ["XXX", "X X", "X X"], {"X": item("hbm:ingot_steel")}))
    recs.append(shaped("armor/steel_boots", "hbm:steel_boots", 1,
                       ["X X", "X X"], {"X": item("hbm:ingot_steel")}))
    recs.append(shaped("armor/titanium_helmet", "hbm:titanium_helmet", 1,
                       ["XXX", "X X"], {"X": item("hbm:ingot_titanium")}))
    recs.append(shaped("armor/titanium_plate", "hbm:titanium_plate", 1,
                       ["X X", "XXX", "XXX"], {"X": item("hbm:ingot_titanium")}))
    recs.append(shaped("armor/titanium_legs", "hbm:titanium_legs", 1,
                       ["XXX", "X X", "X X"], {"X": item("hbm:ingot_titanium")}))
    recs.append(shaped("armor/titanium_boots", "hbm:titanium_boots", 1,
                       ["X X", "X X"], {"X": item("hbm:ingot_titanium")}))
    recs.append(shaped("armor/security_helmet", "hbm:security_helmet", 1,
                       ["SSS", "IGI"],
                       {"S": item("hbm:plate_steel"), "I": tag("hbm:any_rubber"),
                        "G": tag("c:glass_panes")}))
    recs.append(shaped("armor/security_boots", "hbm:security_boots", 1,
                       ["P P", "I I"],
                       {"P": item("hbm:plate_steel"), "I": tag("hbm:any_rubber")}))
    recs.append(shaped("armor/hazmat_helmet", "hbm:hazmat_helmet", 1,
                       ["EEE", "EIE", " P "],
                       {"E": item("hbm:hazmat_cloth"), "I": tag("c:glass_panes"),
                        "P": item("hbm:plate_iron")}))
    recs.append(shaped("armor/hazmat_plate", "hbm:hazmat_plate", 1,
                       ["E E", "EEE", "EEE"], {"E": item("hbm:hazmat_cloth")}))
    recs.append(shaped("armor/hazmat_legs", "hbm:hazmat_legs", 1,
                       ["EEE", "E E", "E E"], {"E": item("hbm:hazmat_cloth")}))
    recs.append(shaped("armor/hazmat_boots", "hbm:hazmat_boots", 1,
                       ["E E", "E E"], {"E": item("hbm:hazmat_cloth")}))
    recs.append(shaped("armor/hazmat_helmet_red", "hbm:hazmat_helmet_red", 1,
                       ["EEE", "IEI", "EFE"],
                       {"E": item("hbm:hazmat_cloth_red"), "I": tag("c:glass_panes"),
                        "F": item("hbm:plate_iron")}))
    recs.append(shaped("armor/hazmat_plate_red", "hbm:hazmat_plate_red", 1,
                       ["E E", "EEE", "EEE"], {"E": item("hbm:hazmat_cloth_red")}))
    recs.append(shaped("armor/hazmat_legs_red", "hbm:hazmat_legs_red", 1,
                       ["EEE", "E E", "E E"], {"E": item("hbm:hazmat_cloth_red")}))
    recs.append(shaped("armor/hazmat_boots_red", "hbm:hazmat_boots_red", 1,
                       ["E E", "E E"], {"E": item("hbm:hazmat_cloth_red")}))
    recs.append(shaped("armor/hazmat_helmet_grey", "hbm:hazmat_helmet_grey", 1,
                       ["EEE", "IEI", "EFE"],
                       {"E": item("hbm:hazmat_cloth_grey"), "I": tag("c:glass_panes"),
                        "F": item("hbm:plate_iron")}))
    recs.append(shaped("armor/hazmat_plate_grey", "hbm:hazmat_plate_grey", 1,
                       ["E E", "EEE", "EEE"], {"E": item("hbm:hazmat_cloth_grey")}))
    recs.append(shaped("armor/hazmat_legs_grey", "hbm:hazmat_legs_grey", 1,
                       ["EEE", "E E", "E E"], {"E": item("hbm:hazmat_cloth_grey")}))
    recs.append(shaped("armor/hazmat_boots_grey", "hbm:hazmat_boots_grey", 1,
                       ["E E", "E E"], {"E": item("hbm:hazmat_cloth_grey")}))
    recs.append(shaped("armor/asbestos_helmet", "hbm:asbestos_helmet", 1,
                       ["EEE", "EIE"],
                       {"E": item("hbm:asbestos_cloth"), "I": item("hbm:plate_gold")}))
    recs.append(shaped("armor/asbestos_plate", "hbm:asbestos_plate", 1,
                       ["E E", "EEE", "EEE"], {"E": item("hbm:asbestos_cloth")}))
    recs.append(shaped("armor/asbestos_legs", "hbm:asbestos_legs", 1,
                       ["EEE", "E E", "E E"], {"E": item("hbm:asbestos_cloth")}))
    recs.append(shaped("armor/asbestos_boots", "hbm:asbestos_boots", 1,
                       ["E E", "E E"], {"E": item("hbm:asbestos_cloth")}))
    recs.append(shaped("armor/hazmat_paa_helmet", "hbm:hazmat_paa_helmet", 1,
                       ["EEE", "IEI", " P "],
                       {"E": item("hbm:plate_paa"), "I": tag("c:glass_panes"),
                        "P": item("hbm:plate_iron")}))
    recs.append(shaped("armor/hazmat_paa_plate", "hbm:hazmat_paa_plate", 1,
                       ["E E", "EEE", "EEE"], {"E": item("hbm:plate_paa")}))
    recs.append(shaped("armor/hazmat_paa_legs", "hbm:hazmat_paa_legs", 1,
                       ["EEE", "E E", "E E"], {"E": item("hbm:plate_paa")}))
    recs.append(shaped("armor/hazmat_paa_boots", "hbm:hazmat_paa_boots", 1,
                       ["E E", "E E"], {"E": item("hbm:plate_paa")}))
    recs.append(shaped("armor/robes_helmet", "hbm:robes_helmet", 1,
                       ["XXX", "X X"], {"X": item("hbm:rag")}))
    recs.append(shaped("armor/robes_plate", "hbm:robes_plate", 1,
                       ["X X", "XXX", "XXX"], {"X": item("hbm:rag")}))
    recs.append(shaped("armor/robes_legs", "hbm:robes_legs", 1,
                       ["XXX", "X X", "X X"], {"X": item("hbm:rag")}))

    # --- ConsumableRecipes.java leftover ---
    recs.append(shaped("consumable/can_empty", "hbm:can_empty", 1,
                       ["P", "P"], {"P": item("hbm:plate_aluminium")}))
    recs.append(shapeless("consumable/can_redbomb", "hbm:can_redbomb", 1,
                          ["hbm:can_empty", "minecraft:potion", "minecraft:sugar", "hbm:pellet_cluster"]))
    recs.append(shapeless("consumable/can_luna", "hbm:can_luna", 1,
                          ["hbm:can_empty", "minecraft:potion", "minecraft:sugar", "hbm:powder_meteorite_tiny"]))
    recs.append(shapeless("consumable/can_creature_skip_fluid", "hbm:can_creature", 1,
                          ["hbm:can_empty", "minecraft:potion", "minecraft:sugar"]))  # fluid-NBT skipped — drop if we require exact
    # don't emit can_creature without diesel — remove below
    recs.pop()
    recs.append(shapeless("consumable/mucho_mango", "hbm:mucho_mango", 1,
                          ["minecraft:potion", "minecraft:sugar", "minecraft:sugar", "tag:c:dyes/orange"]))
    recs.append(shaped("consumable/canteen_vodka", "hbm:canteen_vodka", 1,
                       ["O", "P"],
                       {"O": item("minecraft:potato"), "P": item("hbm:plate_steel")}))
    recs.append(shaped("consumable/bottle_empty", "hbm:bottle_empty", 6,
                       [" G ", "G G", "GGG"],
                       {"G": tag("c:glass_panes")}))
    recs.append(shapeless("consumable/bottle_nuka", "hbm:bottle_nuka", 1,
                          ["hbm:bottle_empty", "minecraft:potion", "minecraft:sugar", "hbm:powder_coal"]))
    recs.append(shapeless("consumable/bottle_cherry", "hbm:bottle_cherry", 1,
                          ["hbm:bottle_empty", "minecraft:potion", "minecraft:sugar", "minecraft:redstone"]))
    recs.append(shapeless("consumable/bottle_quantum", "hbm:bottle_quantum", 1,
                          ["hbm:bottle_empty", "minecraft:potion", "minecraft:sugar", "hbm:trinitite"]))
    recs.append(shapeless("consumable/bottle_sparkle", "hbm:bottle_sparkle", 1,
                          ["hbm:bottle_nuka", "minecraft:carrot", "minecraft:gold_nugget"]))
    recs.append(shapeless("consumable/bottle_rad", "hbm:bottle_rad", 1,
                          ["hbm:bottle_quantum", "minecraft:carrot", "minecraft:gold_nugget"]))
    recs.append(shaped("consumable/bottle2_empty", "hbm:bottle2_empty", 6,
                       [" G ", "G G", "G G"],
                       {"G": tag("c:glass_panes")}))
    recs.append(shapeless("consumable/bottle2_korl", "hbm:bottle2_korl", 1,
                          ["hbm:bottle2_empty", "minecraft:potion", "minecraft:sugar", "hbm:powder_copper"]))
    recs.append(shapeless("consumable/bottle2_fritz", "hbm:bottle2_fritz", 1,
                          ["hbm:bottle2_empty", "minecraft:potion", "minecraft:sugar", "hbm:powder_tungsten"]))
    recs.append(shapeless("consumable/coffee", "hbm:coffee", 1,
                          ["hbm:powder_coal", "minecraft:milk_bucket", "minecraft:potion", "minecraft:sugar"]))
    recs.append(shapeless("consumable/coffee_radium", "hbm:coffee_radium", 1,
                          ["hbm:coffee", "hbm:nugget_ra226"]))
    recs.append(shapeless("consumable/med_ipecac", "hbm:med_ipecac", 1,
                          ["minecraft:glass_bottle", "minecraft:nether_wart"]))
    recs.append(shapeless("consumable/med_ptsd", "hbm:med_ptsd", 1, ["hbm:med_ipecac"]))
    recs.append(shapeless("consumable/pancake_diamond", "hbm:pancake", 1,
                          ["minecraft:redstone", "hbm:powder_diamond", "minecraft:wheat",
                           "hbm:steel_bolt", "hbm:copper_wire", "hbm:plate_steel"]))
    recs.append(shapeless("consumable/pancake_emerald", "hbm:pancake", 1,
                          ["minecraft:redstone", "hbm:powder_emerald", "minecraft:wheat",
                           "hbm:steel_bolt", "hbm:copper_wire", "hbm:plate_steel"]))
    recs.append(shapeless("consumable/marshmallow", "hbm:marshmallow", 1,
                          ["minecraft:stick", "minecraft:sugar", "minecraft:wheat_seeds"]))
    recs.append(shapeless("consumable/quesadilla", "hbm:quesadilla", 3,
                          ["hbm:cheese", "hbm:cheese", "minecraft:bread"]))
    recs.append(shaped("consumable/peas", "hbm:peas", 1,
                       [" S ", "SNS", " S "],
                       {"S": item("minecraft:wheat_seeds"), "N": item("minecraft:gold_nugget")}))
    recs.append(shapeless("consumable/glowing_stew", "hbm:glowing_stew", 1,
                          ["minecraft:bowl", "hbm:mush", "hbm:mush"]))
    recs.append(shapeless("consumable/balefire_scrambled", "hbm:balefire_scrambled", 1,
                          ["minecraft:bowl", "hbm:egg_balefire"]))
    recs.append(shapeless("consumable/balefire_and_ham", "hbm:balefire_and_ham", 1,
                          ["hbm:balefire_scrambled", "minecraft:cooked_beef"]))
    recs.append(shaped("consumable/apple_euphemium", "hbm:apple_euphemium", 1,
                       ["EEE", "EAE", "EEE"],
                       {"E": item("hbm:nugget_euphemium"), "A": item("minecraft:apple")}))
    recs.append(shaped("consumable/cotton_candy", "hbm:cotton_candy", 2,
                       [" S ", "SPS", " H "],
                       {"P": item("hbm:nugget_pu239"), "S": item("minecraft:sugar"),
                        "H": item("minecraft:stick")}))
    recs.append(shaped("consumable/schnitzel_vegan", "hbm:schnitzel_vegan", 3,
                       ["RWR", "WPW", "RWR"],
                       {"W": item("hbm:nuclear_waste"), "R": item("minecraft:sugar_cane"),
                        "P": item("minecraft:pumpkin_seeds")}))
    recs.append(shapeless("consumable/siox", "hbm:siox", 8,
                          ["hbm:powder_coal", "hbm:powder_asbestos", "hbm:nugget_bismuth"]))
    recs.append(shapeless("consumable/crackpipe", "hbm:crackpipe", 1, ["hbm:catalytic_converter"]))
    recs.append(shapeless("consumable/fmn", "hbm:fmn", 1,
                          ["hbm:powder_coal", "hbm:powder_polonium", "hbm:powder_sr90"]))
    recs.append(shapeless("consumable/five_htp", "hbm:five_htp", 1,
                          ["hbm:powder_coal", "hbm:powder_euphemium", "hbm:canteen_vodka"]))
    recs.append(shaped("consumable/iv_empty", "hbm:iv_empty", 4,
                       ["S", "I", "S"],
                       {"S": tag("hbm:any_rubber"), "I": item("hbm:plate_iron")}))
    recs.append(shapeless("consumable/iv_xp_empty", "hbm:iv_xp_empty", 1,
                          ["hbm:iv_empty", "hbm:powder_magic"]))
    recs.append(shaped("consumable/cladding_obsidian", "hbm:cladding_obsidian", 1,
                       ["OOO", "PDP", "OOO"],
                       {"O": item("minecraft:obsidian"), "P": item("hbm:plate_steel"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/cladding_iron", "hbm:cladding_iron", 1,
                       ["OOO", "PDP", "OOO"],
                       {"O": item("hbm:plate_iron"), "P": item("hbm:plate_polymer"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/cladding_desh", "hbm:cladding_desh", 1,
                       ["DPD", "PRP", "DPD"],
                       {"R": item("hbm:cladding_lead"), "P": item("hbm:plate_desh"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/cladding_ghiorsium", "hbm:cladding_ghiorsium", 1,
                       ["DPD", "PRP", "DPD"],
                       {"R": item("hbm:cladding_desh"), "P": item("hbm:ingot_gh336"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/insert_steel", "hbm:insert_steel", 1,
                       ["DPD", "PSP", "DPD"],
                       {"D": item("hbm:ducttape"), "P": item("hbm:plate_iron"),
                        "S": item("hbm:block_steel")}))
    recs.append(shaped("consumable/insert_era", "hbm:insert_era", 1,
                       ["DPD", "PSP", "DPD"],
                       {"D": item("hbm:ducttape"), "P": item("hbm:plate_iron"),
                        "S": item("hbm:ingot_semtex")}))
    recs.append(shaped("consumable/insert_kevlar", "hbm:insert_kevlar", 1,
                       ["KIK", "IDI", "KIK"],
                       {"K": item("hbm:plate_kevlar"), "I": tag("hbm:any_rubber"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/insert_sapi", "hbm:insert_sapi", 1,
                       ["PKP", "DPD", "PKP"],
                       {"P": tag("hbm:any_plastic"), "K": item("hbm:insert_kevlar"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/servo_set", "hbm:servo_set", 1,
                       ["MBM", "PBP", "MBM"],
                       {"M": item("hbm:motor"), "B": item("hbm:steel_bolt"),
                        "P": item("hbm:plate_iron")}))
    recs.append(shaped("consumable/servo_set_desh", "hbm:servo_set_desh", 1,
                       ["MBM", "PSP", "MBM"],
                       {"M": item("hbm:motor_desh"), "B": item("hbm:durasteel_bolt"),
                        "P": item("hbm:plate_desh"), "S": item("hbm:servo_set")}))
    recs.append(shaped("consumable/attachment_mask", "hbm:attachment_mask", 1,
                       ["DID", "IGI", " F "],
                       {"D": item("hbm:ducttape"), "I": tag("hbm:any_rubber"),
                        "G": tag("c:glass_panes"), "F": item("hbm:plate_iron")}))
    recs.append(shaped("consumable/attachment_mask_mono", "hbm:attachment_mask_mono", 1,
                       [" D ", "DID", " F "],
                       {"D": item("hbm:ducttape"), "I": tag("hbm:any_rubber"),
                        "F": item("hbm:plate_iron")}))
    recs.append(shaped("consumable/pads_rubber", "hbm:pads_rubber", 1,
                       ["P P", "IDI", "P P"],
                       {"P": tag("hbm:any_rubber"), "I": item("hbm:plate_iron"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/pads_slime", "hbm:pads_slime", 1,
                       ["SPS", "DSD", "SPS"],
                       {"S": item("minecraft:slime_ball"), "P": item("hbm:pads_rubber"),
                        "D": item("hbm:ducttape")}))
    recs.append(shaped("consumable/pads_static", "hbm:pads_static", 1,
                       ["CDC", "ISI", "CDC"],
                       {"C": item("hbm:ingot_copper"), "D": item("hbm:ducttape"),
                        "I": tag("hbm:any_rubber"), "S": item("hbm:pads_slime")}))
    recs.append(shaped("consumable/horseshoe_magnet", "hbm:horseshoe_magnet", 1,
                       ["L L", "I I", "ILI"],
                       {"L": item("hbm:lodestone"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("consumable/protection_charm", "hbm:protection_charm", 1,
                       [" M ", "MDM", " M "],
                       {"M": item("hbm:fragment_meteorite"), "D": item("minecraft:diamond")}))
    recs.append(shaped("consumable/meteor_charm", "hbm:meteor_charm", 1,
                       [" M ", "MDM", " M "],
                       {"M": item("hbm:fragment_meteorite"), "D": item("hbm:gem_volcanic")}))
    recs.append(shaped("consumable/gas_tester", "hbm:gas_tester", 1,
                       ["G", "C", "I"],
                       {"G": item("hbm:plate_gold"), "C": item("hbm:circuit_vacuum_tube"),
                        "I": item("hbm:plate_iron")}))
    recs.append(shaped("consumable/neutrino_lens", "hbm:neutrino_lens", 1,
                       ["PSP", "SCS", "PSP"],
                       {"P": tag("hbm:any_plastic"), "S": item("hbm:ingot_starmetal"),
                        "C": item("hbm:circuit_bismoid")}))
    recs.append(shaped("weapon/machine_weapon_table", "hbm:machine_weapon_table", 1,
                       ["PPP", "TCT", "TST"],
                       {"P": item("hbm:plate_gunmetal"), "T": item("hbm:ingot_steel"),
                        "C": item("minecraft:crafting_table"), "S": item("hbm:block_steel")}))

    # WeaponRecipes.java:43-54 SEDNA wood/polymer grips
    recs.append(shaped("weapon/part_stock_wood", "hbm:part_stock_wood", 1,
                       ["WWW", "  W"], {"W": tag("minecraft:planks")}))
    recs.append(shaped("weapon/part_grip_wood", "hbm:part_grip_wood", 1,
                       ["W ", " W", " W"], {"W": tag("minecraft:planks")}))
    recs.append(shaped("weapon/part_stock_polymer", "hbm:part_stock_polymer", 1,
                       ["WWW", "  W"], {"W": item("hbm:ingot_polymer")}))
    recs.append(shaped("weapon/part_grip_polymer", "hbm:part_grip_polymer", 1,
                       ["W ", " W", " W"], {"W": item("hbm:ingot_polymer")}))
    recs.append(shaped("weapon/part_stock_bakelite", "hbm:part_stock_bakelite", 1,
                       ["WWW", "  W"], {"W": item("hbm:ingot_bakelite")}))
    recs.append(shaped("weapon/part_grip_bakelite", "hbm:part_grip_bakelite", 1,
                       ["W ", " W", " W"], {"W": item("hbm:ingot_bakelite")}))
    recs.append(shaped("weapon/part_grip_rubber", "hbm:part_grip_rubber", 1,
                       ["W ", " W", " W"], {"W": item("hbm:ingot_rubber")}))
    recs.append(shaped("weapon/part_grip_ivory", "hbm:part_grip_ivory", 1,
                       ["W ", " W", " W"], {"W": item("minecraft:bone")}))
    recs.append(shaped("weapon/casing_shotshell", "hbm:casing_shotshell", 2,
                       ["P", "C"],
                       {"P": item("hbm:plate_gunmetal"), "C": item("hbm:casing_large")}))
    recs.append(shaped("weapon/casing_buckshot", "hbm:casing_buckshot", 2,
                       ["P", "C"],
                       {"P": tag("hbm:any_plastic"), "C": item("hbm:casing_large")}))
    recs.append(shaped("weapon/casing_buckshot_advanced", "hbm:casing_buckshot_advanced", 2,
                       ["P", "C"],
                       {"P": tag("hbm:any_plastic"), "C": item("hbm:casing_large_steel")}))

    # ExclusiveRecipes.java leftovers
    recs.append(shaped("building/block_niter_reinforced", "hbm:block_niter_reinforced", 1,
                       ["TCT", "CNC", "TCT"],
                       {"T": item("hbm:ingot_tcalloy"), "C": item("hbm:concrete"),
                        "N": item("hbm:block_niter")}))
    recs.append(shapeless("building/red_wire_sealed", "hbm:red_wire_sealed", 1,
                          ["hbm:red_wire_coated", "hbm:brick_compound"]))
    recs.append(shaped("building/fluid_duct_solid", "hbm:fluid_duct_solid", 8,
                       ["SAS", "ADA", "SAS"],
                       {"S": item("hbm:ingot_steel"), "A": item("hbm:plate_aluminium"),
                        "D": item("hbm:ducttape")}))
    recs.append(shapeless("building/fluid_duct_solid_sealed", "hbm:fluid_duct_solid_sealed", 1,
                          ["hbm:fluid_duct_solid", "hbm:brick_compound"]))

    return recs


def _known_ids():
    import sys
    sys.path.insert(0, str(REPO / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, blocks = extract_all_ids()
    known = items | blocks
    java = REPO / "src/main/java"
    pats = [
        r'register(?:Block|Item|Billet|Powder|FuelPowder|RtgPellet|Resource|Ingot|Nugget|LoreIngot|LoreNugget|Can|Bottle|PlainEnergy|Soup|Parts|Waste|PlateFuel)?\(\s*"([a-z0-9_]+)"',
        r'reg\(\s*"([a-z0-9_]+)"',
        r'ITEMS\.register\(\s*"([a-z0-9_]+)"',
        r'parts(?:1)?\(\s*"([a-z0-9_]+)"',
    ]
    for p in java.rglob("*.java"):
        t = p.read_text(errors="ignore")
        for pat in pats:
            known.update(re.findall(pat, t))
    known.update({
        "circuit_vacuum_tube", "circuit_capacitor", "circuit_basic", "circuit_advanced",
        "circuit_chip", "circuit_analog", "circuit_pcb", "circuit_controller",
        "circuit_controller_chassis", "circuit_bismoid", "circuit_quantum",
        "gold_wire", "steel_bolt", "durasteel_bolt", "tungsten_bolt",
        "steel_shell", "copper_pipe", "copper_wire",
        "part_generic_piston_pneumatic", "part_generic_piston_hydraulic",
        "part_generic_piston_electric",
        "pile_rod_uranium", "pile_rod_source", "pile_rod_boron", "pile_rod_lithium",
        "pile_rod_detector", "pile_rod_pu239", "pile_rod_plutonium",
        "geiger", "machine_diesel",
    })
    return known


def _refs(obj):
    out = []
    if isinstance(obj, dict):
        if "item" in obj and isinstance(obj["item"], str):
            out.append(obj["item"])
        if "id" in obj and isinstance(obj["id"], str):
            out.append(obj["id"])
        for v in obj.values():
            out.extend(_refs(v))
    elif isinstance(obj, list):
        for v in obj:
            out.extend(_refs(v) if not (isinstance(v, str) and ":" in v) else [v])
    return out


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    known = _known_ids()
    written = skipped = dropped = 0
    for rec in recipes():
        rel = rec.pop("_path")
        slug = rec["result"]["id"].split(":", 1)[-1]
        if slug in SKIP_RESULTS:
            dropped += 1
            print("skip-banned", rel)
            continue
        missing = []
        for ref in _refs(rec):
            if ref.startswith(("minecraft:", "c:", "neoforge:", "tag:")):
                continue
            s = ref.split(":", 1)[-1] if ref.startswith("hbm:") else ref
            if s not in known:
                missing.append(ref)
        if missing:
            dropped += 1
            print("drop", rel, missing)
            continue
        path = OUT / f"{rel}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            skipped += 1
            continue
        path.write_text(json.dumps(rec, indent=2) + "\n")
        written += 1
    print(f"wave13 written={written} skipped_exists={skipped} dropped={dropped}")


if __name__ == "__main__":
    main()
