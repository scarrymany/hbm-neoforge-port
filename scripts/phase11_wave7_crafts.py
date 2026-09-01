#!/usr/bin/env python3
"""Leftover vanilla crafts from CE CraftingManager.java:137-290 / later leftovers.

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten results.
Does not emit recipes whose result or hbm: ingredient is unregistered.
"""
from __future__ import annotations

import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "src" / "main" / "resources" / "data" / "hbm" / "recipe" / "ce_craft"

SKIP_RESULTS = {"powder_sawdust", "gem_tantalium", "coil_tungsten"}


def shaped(path: str, result: str, count: int, pattern: list[str], key: dict) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
        "_path": path,
    }


def shapeless(path: str, result: str, count: int, ings: list) -> dict:
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


def item(iid: str) -> dict:
    return {"item": iid}


def tag(tid: str) -> dict:
    return {"tag": tid}


def recipes() -> list[dict]:
    recs: list[dict] = []

    # CraftingManager.java:137-138
    recs.append(shaped("tools/redstone_sword", "hbm:redstone_sword", 1,
                       ["R", "R", "S"],
                       {"R": item("minecraft:redstone_block"), "S": item("minecraft:stick")}))
    recs.append(shaped("tools/big_sword", "hbm:big_sword", 1,
                       ["QIQ", "QIQ", "GSG"],
                       {"G": item("minecraft:gold_ingot"), "S": item("minecraft:stick"),
                        "I": item("minecraft:iron_ingot"), "Q": item("minecraft:quartz")}))
    # :140
    recs.append(shapeless("machine/machine_industrial_turbine_from_large",
                          "hbm:machine_industrial_turbine", 1,
                          ["hbm:machine_large_turbine"]))
    # :143-147 cloth / spike / pipes
    recs.append(shaped("parts/hazmat_cloth_red", "hbm:hazmat_cloth_red", 1,
                       ["C", "R", "C"],
                       {"C": item("hbm:hazmat_cloth"), "R": item("minecraft:redstone")}))
    recs.append(shaped("parts/hazmat_cloth_grey", "hbm:hazmat_cloth_grey", 1,
                       [" P ", "ICI", " L "],
                       {"C": item("hbm:hazmat_cloth_red"), "P": item("hbm:plate_iron"),
                        "L": item("hbm:plate_lead"), "I": item("hbm:ingot_rubber")}))
    recs.append(shaped("parts/asbestos_cloth", "hbm:asbestos_cloth", 8,
                       ["SCS", "CPC", "SCS"],
                       {"S": item("minecraft:string"), "P": item("hbm:powder_bromine"),
                        "C": item("minecraft:white_wool")}))
    recs.append(shaped("parts/bolt_spike", "hbm:bolt_spike", 2,
                       ["BB", "B ", "B "],
                       {"B": item("hbm:steel_bolt")}))
    recs.append(shaped("parts/pipes_steel", "hbm:pipes_steel", 1,
                       ["B", "B", "B"],
                       {"B": item("hbm:block_steel")}))
    # :148-154 plate_polymer
    recs.append(shaped("parts/plate_polymer_plastic", "hbm:plate_polymer", 8,
                       ["DD"], {"D": tag("hbm:any_plastic")}))
    recs.append(shaped("parts/plate_polymer_rubber", "hbm:plate_polymer", 8,
                       ["DD"], {"D": item("hbm:ingot_rubber")}))
    recs.append(shaped("parts/plate_polymer_fiberglass", "hbm:plate_polymer", 16,
                       ["DD"], {"D": item("hbm:ingot_fiberglass")}))
    recs.append(shaped("parts/plate_polymer_asbestos", "hbm:plate_polymer", 16,
                       ["DD"], {"D": item("hbm:ingot_asbestos")}))
    recs.append(shaped("parts/plate_polymer_wool", "hbm:plate_polymer", 4,
                       ["SWS"],
                       {"S": item("minecraft:string"), "W": item("minecraft:white_wool")}))
    recs.append(shaped("parts/plate_polymer_brick", "hbm:plate_polymer", 4,
                       ["BB"], {"B": item("minecraft:brick")}))
    recs.append(shaped("parts/plate_polymer_nether_brick", "hbm:plate_polymer", 4,
                       ["BB"], {"B": item("minecraft:nether_brick")}))
    # :156-167 circuits
    recs.append(shaped("parts/circuit_vacuum_tube_tungsten", "hbm:circuit_vacuum_tube", 1,
                       ["G", "W", "I"],
                       {"G": tag("c:glass_panes"), "W": item("hbm:tungsten_wire"),
                        "I": item("hbm:plate_polymer")}))
    recs.append(shaped("parts/circuit_vacuum_tube_carbon", "hbm:circuit_vacuum_tube", 1,
                       ["G", "W", "I"],
                       {"G": tag("c:glass_panes"), "W": item("hbm:carbon_wire"),
                        "I": item("hbm:plate_polymer")}))
    recs.append(shaped("parts/circuit_numitron", "hbm:circuit_numitron", 3,
                       ["G", "W", "I"],
                       {"G": tag("c:glass_panes"), "W": item("hbm:coil_advanced"),
                        "I": item("hbm:plate_copper")}))
    recs.append(shaped("parts/circuit_capacitor_al", "hbm:circuit_capacitor", 1,
                       ["I", "N", "W"],
                       {"I": item("hbm:plate_polymer"), "N": item("hbm:nugget_niobium"),
                        "W": item("hbm:aluminum_wire")}))
    recs.append(shaped("parts/circuit_capacitor_cu", "hbm:circuit_capacitor", 1,
                       ["I", "N", "W"],
                       {"I": item("hbm:plate_polymer"), "N": item("hbm:nugget_niobium"),
                        "W": item("hbm:copper_wire")}))
    recs.append(shaped("parts/circuit_capacitor_dust_al", "hbm:circuit_capacitor", 2,
                       ["IAI", "W W"],
                       {"I": item("hbm:plate_polymer"), "A": item("hbm:powder_aluminium"),
                        "W": item("hbm:aluminum_wire")}))
    recs.append(shaped("parts/circuit_capacitor_dust_cu", "hbm:circuit_capacitor", 2,
                       ["IAI", "W W"],
                       {"I": item("hbm:plate_polymer"), "A": item("hbm:powder_aluminium"),
                        "W": item("hbm:copper_wire")}))
    recs.append(shaped("parts/circuit_pcb_copper", "hbm:circuit_pcb", 1,
                       ["I", "P"],
                       {"I": item("hbm:plate_polymer"), "P": item("hbm:plate_copper")}))
    recs.append(shaped("parts/circuit_pcb_gold", "hbm:circuit_pcb", 4,
                       ["I", "P"],
                       {"I": item("hbm:plate_polymer"), "P": item("hbm:plate_gold")}))
    recs.append(shaped("parts/circuit_controller_chassis", "hbm:circuit_controller_chassis", 1,
                       ["PPP", "CBB", "PPP"],
                       {"P": tag("hbm:any_plastic"), "C": item("hbm:crt_display"),
                        "B": item("hbm:circuit_pcb")}))
    # :169-177
    recs.append(shaped("parts/crt_display", "hbm:crt_display", 4,
                       [" A ", "SGS", " T "],
                       {"A": item("hbm:powder_aluminium"), "S": item("hbm:plate_steel"),
                        "G": tag("c:glass_panes"), "T": item("hbm:circuit_vacuum_tube")}))
    recs.append(shaped("parts/cell", "hbm:cell", 6,
                       [" S ", "G G", " S "],
                       {"S": item("hbm:plate_steel"), "G": tag("c:glass_panes")}))
    recs.append(shaped("parts/particle_empty", "hbm:particle_empty", 2,
                       ["STS", "G G", "STS"],
                       {"S": item("hbm:lead_plate_triple"), "T": item("hbm:coil_gold"),
                        "G": tag("c:glass_panes")}))
    recs.append(shapeless("parts/particle_copper", "hbm:particle_copper", 1,
                          ["hbm:particle_empty", "hbm:powder_copper", "hbm:pellet_charged"]))
    recs.append(shapeless("parts/particle_lead", "hbm:particle_lead", 1,
                          ["hbm:particle_empty", "hbm:powder_lead", "hbm:pellet_charged"]))
    recs.append(shaped("parts/canister_empty", "hbm:canister_empty", 2,
                       ["S ", "AA", "AA"],
                       {"S": item("hbm:plate_steel"), "A": item("hbm:plate_aluminium")}))
    recs.append(shaped("parts/gas_empty", "hbm:gas_empty", 2,
                       ["S ", "AA", "AA"],
                       {"A": item("hbm:plate_steel"), "S": item("hbm:plate_copper")}))
    recs.append(shapeless("building/block_waste_painted", "hbm:block_waste_painted", 1,
                          ["tag:c:dyes/yellow", "hbm:block_waste"]))
    # :191-202 biomass leftovers (no powder_sawdust RESULT)
    recs.append(shapeless("consumable/biomass_apple", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:apple", "minecraft:apple", "minecraft:apple"]))
    recs.append(shapeless("consumable/biomass_reeds", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:sugar_cane", "minecraft:sugar_cane", "minecraft:sugar_cane"]))
    recs.append(shapeless("consumable/biomass_flesh", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:rotten_flesh", "minecraft:rotten_flesh", "minecraft:rotten_flesh"]))
    recs.append(shapeless("consumable/biomass_carrot", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:carrot", "minecraft:carrot", "minecraft:carrot",
                           "minecraft:carrot", "minecraft:carrot", "minecraft:carrot"]))
    recs.append(shapeless("consumable/biomass_potato", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:potato", "minecraft:potato", "minecraft:potato",
                           "minecraft:potato", "minecraft:potato", "minecraft:potato"]))
    recs.append(shapeless("consumable/biomass_pumpkin", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:pumpkin"]))
    recs.append(shapeless("consumable/biomass_melon", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:melon"]))
    recs.append(shapeless("consumable/biomass_cactus", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:cactus", "minecraft:cactus", "minecraft:cactus"]))
    recs.append(shapeless("consumable/biomass_wheat", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "minecraft:wheat", "minecraft:wheat", "minecraft:wheat",
                           "minecraft:wheat", "minecraft:wheat", "minecraft:wheat"]))
    # :205-222 coils / tank / motor (coil_tungsten RESULT banned)
    recs.append(shaped("parts/coil_copper_iron", "hbm:coil_copper", 1,
                       ["WWW", "WIW", "WWW"],
                       {"W": item("hbm:mingrade_wire"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("parts/coil_copper_steel", "hbm:coil_copper", 1,
                       ["WWW", "WIW", "WWW"],
                       {"W": item("hbm:mingrade_wire"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("parts/coil_gold_iron", "hbm:coil_gold", 1,
                       ["WWW", "WIW", "WWW"],
                       {"W": item("hbm:gold_wire"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("parts/coil_gold_steel", "hbm:coil_gold", 1,
                       ["WWW", "WIW", "WWW"],
                       {"W": item("hbm:gold_wire"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("parts/coil_copper_torus_iron", "hbm:coil_copper_torus", 2,
                       [" C ", "CPC", " C "],
                       {"P": item("hbm:plate_iron"), "C": item("hbm:coil_copper")}))
    recs.append(shaped("parts/coil_gold_torus_iron", "hbm:coil_gold_torus", 2,
                       [" C ", "CPC", " C "],
                       {"P": item("hbm:plate_iron"), "C": item("hbm:coil_gold")}))
    recs.append(shaped("parts/coil_copper_torus_steel", "hbm:coil_copper_torus", 2,
                       [" C ", "CPC", " C "],
                       {"P": item("hbm:plate_steel"), "C": item("hbm:coil_copper")}))
    recs.append(shaped("parts/coil_gold_torus_steel", "hbm:coil_gold_torus", 2,
                       [" C ", "CPC", " C "],
                       {"P": item("hbm:plate_steel"), "C": item("hbm:coil_gold")}))
    recs.append(shaped("parts/coil_magnetized_tungsten_iron", "hbm:coil_magnetized_tungsten", 1,
                       ["WWW", "WIW", "WWW"],
                       {"W": item("hbm:magnetizedtungsten_wire"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("parts/coil_magnetized_tungsten_steel", "hbm:coil_magnetized_tungsten", 1,
                       ["WWW", "WIW", "WWW"],
                       {"W": item("hbm:magnetizedtungsten_wire"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("parts/tank_steel", "hbm:tank_steel", 2,
                       ["STS", "S S", "STS"],
                       {"S": item("hbm:plate_steel"), "T": item("hbm:plate_titanium")}))
    recs.append(shaped("parts/motor_iron", "hbm:motor", 2,
                       [" R ", "ICI", "ITI"],
                       {"R": item("hbm:mingrade_wire"), "T": item("hbm:coil_copper_torus"),
                        "I": item("hbm:plate_iron"), "C": item("hbm:coil_copper")}))
    recs.append(shaped("parts/motor_steel", "hbm:motor", 2,
                       [" R ", "ICI", " T "],
                       {"R": item("hbm:mingrade_wire"), "T": item("hbm:coil_copper_torus"),
                        "I": item("hbm:plate_steel"), "C": item("hbm:coil_copper")}))
    recs.append(shaped("parts/motor_desh", "hbm:motor_desh", 1,
                       ["PCP", "DMD", "PCP"],
                       {"P": tag("hbm:any_plastic"), "C": item("hbm:gold_dense_wire"),
                        "D": item("hbm:ingot_desh"), "M": item("hbm:motor")}))
    # :224-243 missile / turbine / food
    recs.append(shaped("weapon/fins_flat", "hbm:fins_flat", 1,
                       ["IP", "PP", "IP"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("weapon/fins_small_steel_ce", "hbm:fins_small_steel", 1,
                       [" PP", "PII", " PP"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("weapon/fins_big_steel", "hbm:fins_big_steel", 1,
                       [" PI", "III", " PI"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("weapon/fins_tri_steel", "hbm:fins_tri_steel", 1,
                       [" PI", "IIB", " PI"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel"),
                        "B": item("hbm:block_steel")}))
    recs.append(shaped("weapon/sphere_steel_ce", "hbm:sphere_steel", 1,
                       ["PIP", "I I", "PIP"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("weapon/pedestal_steel", "hbm:pedestal_steel", 1,
                       ["P P", "P P", "III"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("parts/blade_titanium", "hbm:blade_titanium", 2,
                       ["TP", "TP", "TT"],
                       {"P": item("hbm:plate_titanium"), "T": item("hbm:ingot_titanium")}))
    recs.append(shaped("parts/turbine_titanium", "hbm:turbine_titanium", 1,
                       ["BBB", "BSB", "BBB"],
                       {"B": item("hbm:blade_titanium"), "S": item("hbm:ingot_steel")}))
    recs.append(shapeless("consumable/definitelyfood_seeds", "hbm:definitelyfood", 4,
                          ["hbm:ingot_rubber", "minecraft:wheat", "minecraft:rotten_flesh",
                           "minecraft:wheat_seeds", "minecraft:wheat_seeds", "minecraft:wheat_seeds"]))
    recs.append(shapeless("consumable/definitelyfood_sapling", "hbm:definitelyfood", 4,
                          ["hbm:ingot_rubber", "minecraft:wheat", "minecraft:rotten_flesh",
                           "tag:c:saplings"]))
    recs.append(shaped("parts/turbine_tungsten", "hbm:turbine_tungsten", 1,
                       ["BBB", "BSB", "BBB"],
                       {"B": item("hbm:blade_tungsten"), "S": item("hbm:ingot_dura_steel")}))
    recs.append(shaped("parts/ring_starmetal", "hbm:ring_starmetal", 1,
                       [" S ", "S S", " S "],
                       {"S": item("hbm:ingot_starmetal")}))
    # :253-267 tools / ducttape / radio
    recs.append(shaped("parts/paper_from_sawdust", "minecraft:paper", 3,
                       ["SSS"], {"S": item("hbm:powder_sawdust")}))
    recs.append(shaped("tools/wrench", "hbm:wrench", 1,
                       [" S ", " IS", "I  "],
                       {"S": item("hbm:ingot_steel"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("tools/wrench_flipped", "hbm:wrench_flipped", 1,
                       ["S", "D", "W"],
                       {"S": item("minecraft:iron_sword"), "D": item("hbm:ducttape"),
                        "W": item("hbm:wrench")}))
    recs.append(shapeless("tools/cbt_device", "hbm:cbt_device", 1,
                          ["hbm:steel_bolt", "hbm:wrench"]))
    recs.append(shaped("parts/ducttape", "hbm:ducttape", 4,
                       ["F", "P", "S"],
                       {"F": item("minecraft:string"), "S": item("minecraft:slime_ball"),
                        "P": item("minecraft:paper")}))
    recs.append(shaped("network/radio_torch_sender", "hbm:radio_torch_sender", 4,
                       ["G", "R", "I"],
                       {"G": item("minecraft:glowstone_dust"), "R": item("minecraft:redstone_torch"),
                        "I": item("minecraft:quartz")}))
    recs.append(shaped("network/radio_torch_receiver", "hbm:radio_torch_receiver", 4,
                       ["G", "R", "I"],
                       {"G": item("minecraft:glowstone_dust"), "R": item("minecraft:redstone_torch"),
                        "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("network/radio_torch_logic", "hbm:radio_torch_logic", 4,
                       ["G", "R", "I"],
                       {"G": item("minecraft:glowstone_dust"), "R": item("minecraft:redstone_torch"),
                        "I": item("hbm:circuit_chip")}))
    recs.append(shaped("network/radio_torch_counter", "hbm:radio_torch_counter", 4,
                       ["G", "R", "I"],
                       {"G": item("minecraft:glowstone_dust"), "R": item("minecraft:redstone_torch"),
                        "I": item("hbm:circuit_vacuum_tube")}))
    recs.append(shaped("network/radio_torch_reader", "hbm:radio_torch_reader", 4,
                       [" G ", "IRI"],
                       {"G": item("minecraft:glowstone_dust"), "R": item("minecraft:redstone_torch"),
                        "I": item("hbm:circuit_vacuum_tube")}))
    recs.append(shaped("network/radio_torch_controller", "hbm:radio_torch_controller", 4,
                       [" G ", "IRI"],
                       {"G": item("minecraft:glowstone_dust"), "R": item("minecraft:redstone_torch"),
                        "I": item("hbm:circuit_chip")}))
    recs.append(shaped("network/radio_telex", "hbm:radio_telex", 2,
                       ["SCR", "W#W", "WWW"],
                       {"S": item("hbm:radio_torch_sender"), "C": item("hbm:crt_display"),
                        "R": item("hbm:radio_torch_receiver"), "W": tag("minecraft:planks"),
                        "#": item("hbm:circuit_analog")}))
    # :277-291 cables
    recs.append(shaped("network/red_wire_coated", "hbm:red_wire_coated", 16,
                       ["WRW", "RIR", "WRW"],
                       {"W": item("hbm:plate_polymer"), "I": item("hbm:ingot_red_copper"),
                        "R": item("hbm:mingrade_wire")}))
    recs.append(shaped("network/red_cable_paintable", "hbm:red_cable_paintable", 16,
                       ["WRW", "RIR", "WRW"],
                       {"W": item("hbm:plate_steel"), "I": item("hbm:ingot_red_copper"),
                        "R": item("hbm:mingrade_wire")}))
    recs.append(shaped("network/cable_switch", "hbm:cable_switch", 1,
                       ["S", "W"],
                       {"S": item("minecraft:lever"), "W": item("hbm:red_wire_coated")}))
    recs.append(shaped("network/cable_detector", "hbm:cable_detector", 1,
                       ["S", "W"],
                       {"S": item("minecraft:redstone"), "W": item("hbm:red_wire_coated")}))
    recs.append(shaped("network/red_cable", "hbm:red_cable", 16,
                       [" W ", "RRR", " W "],
                       {"W": item("hbm:plate_polymer"), "R": item("hbm:mingrade_wire")}))
    recs.append(shapeless("network/red_cable_classic", "hbm:red_cable_classic", 1,
                          ["hbm:red_cable"]))
    recs.append(shapeless("network/red_cable_from_classic", "hbm:red_cable", 1,
                          ["hbm:red_cable_classic"]))
    recs.append(shapeless("network/red_cable_gauge", "hbm:red_cable_gauge", 1,
                          ["hbm:red_wire_coated", "hbm:ingot_steel", "hbm:circuit_basic"]))
    recs.append(shaped("network/red_connector", "hbm:red_connector", 4,
                       ["C", "I", "S"],
                       {"C": item("hbm:coil_copper"), "I": item("hbm:plate_polymer"),
                        "S": item("hbm:ingot_steel")}))
    recs.append(shaped("network/red_pylon", "hbm:red_pylon", 4,
                       ["CWC", "PWP", " T "],
                       {"C": item("hbm:coil_copper"), "W": tag("minecraft:planks"),
                        "P": item("hbm:plate_polymer"), "T": item("hbm:red_wire_coated")}))
    # CraftingManager leftover machine casings / tools
    recs.append(shaped("machine/machine_electric_furnace", "hbm:machine_electric_furnace_off", 1,
                       ["BBB", "WFW", "RRR"],
                       {"B": item("hbm:ingot_beryllium"), "R": item("hbm:coil_advanced"),
                        "W": item("hbm:copper_plate_triple"), "F": item("minecraft:furnace")}))
    recs.append(shaped("network/machine_detector", "hbm:machine_detector", 1,
                       ["IRI", "CTC", "IRI"],
                       {"I": item("hbm:plate_polymer"), "R": item("minecraft:redstone"),
                        "C": item("hbm:mingrade_wire"), "T": item("hbm:coil_advanced")}))
    recs.append(shaped("parts/deuterium_filter", "hbm:deuterium_filter", 1,
                       ["TST", "SCS", "TST"],
                       {"T": item("hbm:ingot_saturnite"), "S": item("hbm:sulfur"),
                        "C": item("hbm:catalyst_clay")}))
    recs.append(shaped("parts/flywheel_beryllium", "hbm:flywheel_beryllium", 1,
                       ["IBI", "BTB", "IBI"],
                       {"B": item("hbm:block_beryllium"), "I": item("hbm:iron_plate_triple"),
                        "T": item("hbm:dura_steel_pipe")}))
    recs.append(shapeless("consumable/pill_herbal", "hbm:pill_herbal", 1,
                          ["hbm:powder_coal", "minecraft:poisonous_potato",
                           "minecraft:nether_wart"]))
    recs.append(shaped("parts/plant_rope", "hbm:plant_item_rope", 1,
                       ["S", "S", "S"],
                       {"S": item("minecraft:string")}))
    recs.append(shapeless("parts/string_from_hemp", "minecraft:string", 3,
                          ["hbm:plant_flower_hemp"]))
    recs.append(shaped("weapon/shimmer_handle", "hbm:shimmer_handle", 1,
                       ["GP", "GP", "GP"],
                       {"G": item("hbm:plate_gold"), "P": tag("hbm:any_plastic")}))
    recs.append(shaped("weapon/shimmer_sledge", "hbm:shimmer_sledge", 1,
                       ["H", "G", "G"],
                       {"G": item("hbm:shimmer_handle"), "H": item("hbm:shimmer_head")}))
    recs.append(shaped("weapon/shimmer_axe", "hbm:shimmer_axe", 1,
                       ["H", "G", "G"],
                       {"G": item("hbm:shimmer_handle"), "H": item("hbm:shimmer_axe_head")}))
    recs.append(shaped("weapon/shimmer_head", "hbm:shimmer_head", 1,
                       ["SSS", "DTD", "SSS"],
                       {"S": item("hbm:ingot_steel"), "D": item("hbm:block_desh"),
                        "T": item("hbm:block_tungsten")}))
    recs.append(shaped("weapon/shimmer_axe_head", "hbm:shimmer_axe_head", 1,
                       ["PII", "PBB", "PII"],
                       {"P": item("hbm:plate_steel"), "B": item("hbm:block_desh"),
                        "I": item("hbm:ingot_tungsten")}))
    recs.append(shaped("parts/motor_bismuth", "hbm:motor_bismuth", 1,
                       ["BCB", "SDS", "BCB"],
                       {"B": item("hbm:nugget_bismuth"), "C": item("hbm:neodymium_dense_wire"),
                        "S": item("hbm:steel_plate_triple"), "D": item("hbm:ingot_dura_steel")}))
    recs.append(shaped("network/red_pylon_medium_wood", "hbm:red_pylon_medium_wood", 2,
                       ["CCW", "IIW", "  S"],
                       {"C": item("hbm:coil_copper"), "W": tag("minecraft:planks"),
                        "I": item("hbm:plate_polymer"), "S": tag("c:cobblestones")}))
    recs.append(shaped("network/red_connector_super", "hbm:red_connector_super", 2,
                       ["CCC", "III", " S "],
                       {"C": item("hbm:coil_copper"), "I": item("hbm:plate_polymer"),
                        "S": item("hbm:ingot_saturnite")}))
    recs.append(shaped("network/red_pylon_steel_small", "hbm:red_pylon_steel_small", 4,
                       ["CWC", "PWP", " S "],
                       {"C": item("hbm:coil_copper"), "W": item("hbm:steel_pipe"),
                        "P": item("hbm:plate_polymer"), "S": tag("c:cobblestones")}))
    recs.append(shaped("network/cable_diode", "hbm:cable_diode", 1,
                       [" Q ", "CAC", " Q "],
                       {"Q": item("hbm:nugget_silicon"), "C": item("hbm:red_cable"),
                        "A": item("hbm:ingot_aluminium")}))
    recs.append(shaped("network/radio_autocal", "hbm:radio_autocal", 1,
                       ["TAR", "PAP", "PAP"],
                       {"T": item("hbm:radio_torch_sender"), "R": item("hbm:radio_torch_receiver"),
                        "A": item("hbm:circuit_analog"), "P": item("hbm:copper_plate_triple")}))
    recs.append(shaped("parts/graphite_ingot_from_wire", "hbm:ingot_graphite", 1,
                       ["###", "###", "###"],
                       {"#": item("hbm:carbon_wire")}))
    recs.append(shaped("parts/schrabidium_ingot_from_wire", "hbm:ingot_schrabidium", 1,
                       ["###", "###", "###"],
                       {"#": item("hbm:schrabidium_wire")}))
    recs.append(shaped("parts/magnetized_tungsten_ingot_from_wire", "hbm:ingot_magnetized_tungsten", 1,
                       ["###", "###", "###"],
                       {"#": item("hbm:magnetizedtungsten_wire")}))
    recs.append(shapeless("consumable/biomass_sugar_only", "hbm:biomass", 4,
                          ["minecraft:sugar", "hbm:powder_sawdust", "hbm:powder_sawdust",
                           "hbm:powder_sawdust", "hbm:powder_sawdust", "hbm:powder_sawdust"]))
    return recs


def _known_ids() -> set[str]:
    import re
    import sys
    sys.path.insert(0, str(REPO / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, blocks = extract_all_ids()
    known = items | blocks
    java = REPO / "src" / "main" / "java"
    pats = [
        r'register(?:Block|Item)?\(\s*"([a-z0-9_]+)"',
        r'reg\(\s*"([a-z0-9_]+)"',
        r'parts(?:1)?\(\s*"([a-z0-9_]+)"',
        r'fuel\(\s*"([a-z0-9_]+)"',
        r'control\(\s*"([a-z0-9_]+)"',
        r'nuke\(\s*"([a-z0-9_]+)"',
        r'food\(\s*"([a-z0-9_]+)"',
        r'registerIngot\(\s*"([a-z0-9_]+)"',
        r'registerNugget\(\s*"([a-z0-9_]+)"',
        r'registerPowder\(\s*"([a-z0-9_]+)"',
        r'registerLoreIngot\(\s*"([a-z0-9_]+)"',
        r'registerLoreNugget\(\s*"([a-z0-9_]+)"',
        r'registerParts\(\s*"([a-z0-9_]+)"',
    ]
    for p in java.rglob("*.java"):
        text = p.read_text(errors="ignore")
        for pat in pats:
            known.update(re.findall(pat, text))
    # Phase11ProcessItems particle loop + MachineItems circuit flatten
    known.update({
        "particle_empty", "particle_hydrogen", "particle_copper", "particle_lead",
        "particle_amat", "particle_aschrab", "particle_dark", "particle_higgs",
        "particle_tachyon", "particle_strange", "particle_sparkticle",
        "circuit_vacuum_tube", "circuit_numitron", "circuit_capacitor",
        "circuit_capacitor_tantalium", "circuit_pcb", "circuit_chip",
        "circuit_chip_bismoid", "circuit_analog", "circuit_basic",
        "circuit_advanced", "circuit_bismoid", "circuit_controller",
        "circuit_controller_chassis",
    })
    return known


def _refs(obj) -> list[str]:
    out: list[str] = []
    if isinstance(obj, dict):
        if "item" in obj and isinstance(obj["item"], str):
            out.append(obj["item"])
        if "id" in obj and isinstance(obj["id"], str):
            out.append(obj["id"])
        for v in obj.values():
            out.extend(_refs(v))
    elif isinstance(obj, list):
        for v in obj:
            if isinstance(v, str) and ":" in v:
                out.append(v)
            else:
                out.extend(_refs(v))
    return out


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    known = _known_ids()
    written = skipped = dropped = 0
    for rec in recipes():
        rel = rec.pop("_path")
        result = rec["result"]["id"]
        slug = result.split(":", 1)[-1]
        if slug in SKIP_RESULTS:
            dropped += 1
            print(f"skip-banned {rel}")
            continue
        missing = []
        for ref in _refs(rec):
            if ref.startswith("minecraft:") or ref.startswith("c:") or ref.startswith("neoforge:"):
                continue
            s = ref.split(":", 1)[-1] if ref.startswith("hbm:") else ref
            if s not in known:
                missing.append(ref)
        if missing:
            dropped += 1
            print(f"drop {rel} missing={missing}")
            continue
        path = OUT / f"{rel}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            skipped += 1
            continue
        path.write_text(json.dumps(rec, indent=2) + "\n")
        written += 1
    print(f"ce_craft written={written} skipped_exists={skipped} dropped={dropped}")


if __name__ == "__main__":
    main()
