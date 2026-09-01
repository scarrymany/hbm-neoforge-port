#!/usr/bin/env python3
"""Leftover vanilla crafts from CE CraftingManager.java:343 + :526-700 (reg2).

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten results.
Does not emit recipes whose result or hbm: ingredient is unregistered.
Does not rewrite assembler JSON.
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

    # CraftingManager.java:343 — was dropped last wave (furnace_iron unregistered)
    recs.append(shaped("machine/furnace_iron", "hbm:furnace_iron", 1,
                       ["III", "IFI", "BBB"],
                       {"I": item("minecraft:iron_ingot"), "F": item("minecraft:furnace"),
                        "B": item("minecraft:stone_bricks")}))

    # :526-533
    recs.append(shaped("machine/sat_dock", "hbm:sat_dock", 1,
                       ["SSS", "PCP"],
                       {"S": item("hbm:ingot_steel"), "P": tag("hbm:any_plastic"),
                        "C": item("hbm:crate_iron")}))
    recs.append(shaped("building/book_guide", "hbm:book_guide", 1,
                       ["IBI", "LBL", "IBI"],
                       {"B": item("minecraft:book"), "I": tag("c:dyes/black"),
                        "L": tag("c:dyes/blue")}))
    recs.append(shaped("rail/rail_narrow", "hbm:rail_narrow", 64,
                       ["S S", "S S", "S S"],
                       {"S": item("hbm:steel_beam")}))
    recs.append(shaped("rail/rail_highspeed", "hbm:rail_highspeed", 16,
                       ["S S", "SIS", "S S"],
                       {"S": item("hbm:ingot_steel"), "I": item("hbm:plate_iron")}))
    recs.append(shaped("rail/rail_booster", "hbm:rail_booster", 6,
                       ["S S", "CIC", "SRS"],
                       {"S": item("hbm:ingot_steel"), "I": item("hbm:plate_iron"),
                        "R": item("hbm:ingot_red_copper"), "C": item("hbm:coil_copper")}))

    # :536-538
    recs.append(shapeless("powder/powder_ice", "hbm:powder_ice", 4,
                          ["minecraft:snowball", "hbm:niter", "minecraft:redstone"]))
    recs.append(shapeless("powder/powder_poison", "hbm:powder_poison", 4,
                          ["minecraft:spider_eye", "minecraft:redstone", "minecraft:quartz"]))

    # :540-543
    recs.append(shaped("consumable/flame_pony", "hbm:flame_pony", 1,
                       [" O ", "DPD", " O "],
                       {"D": tag("c:dyes/pink"), "O": tag("c:dyes/yellow"),
                        "P": item("minecraft:paper")}))
    recs.append(shaped("consumable/flame_politics", "hbm:flame_politics", 1,
                       [" I ", "IPI", " I "],
                       {"P": item("minecraft:paper"), "I": tag("c:dyes/black")}))
    recs.append(shaped("consumable/flame_opinion", "hbm:flame_opinion", 1,
                       [" R ", "RPR", " R "],
                       {"P": item("minecraft:paper"), "R": tag("c:dyes/red")}))

    # :545-548
    recs.append(shaped("fuel/solid_fuel_presto", "hbm:solid_fuel_presto", 1,
                       [" P ", "SRS", " P "],
                       {"P": item("minecraft:paper"), "S": item("hbm:solid_fuel"),
                        "R": item("minecraft:redstone")}))
    recs.append(shapeless("fuel/solid_fuel_presto_triplet", "hbm:solid_fuel_presto_triplet", 1,
                          ["hbm:solid_fuel_presto", "hbm:solid_fuel_presto",
                           "hbm:solid_fuel_presto", "hbm:ball_dynamite"]))
    recs.append(shaped("fuel/solid_fuel_presto_bf", "hbm:solid_fuel_presto_bf", 1,
                       [" P ", "SRS", " P "],
                       {"P": item("minecraft:paper"), "S": item("hbm:solid_fuel_bf"),
                        "R": item("minecraft:redstone")}))
    recs.append(shapeless("fuel/solid_fuel_presto_triplet_bf", "hbm:solid_fuel_presto_triplet_bf", 1,
                          ["hbm:solid_fuel_presto_bf", "hbm:solid_fuel_presto_bf",
                           "hbm:solid_fuel_presto_bf", "hbm:ingot_c4"]))

    # :551-560
    recs.append(shaped("bomb/flame_war", "hbm:flame_war", 1,
                       ["WHW", "CTP", "WOW"],
                       {"W": tag("minecraft:planks"), "T": item("minecraft:tnt"),
                        "H": item("hbm:flame_pony"), "C": item("hbm:flame_conspiracy"),
                        "P": item("hbm:flame_politics"), "O": item("hbm:flame_opinion")}))
    recs.append(shaped("bomb/det_cord", "hbm:det_cord", 4,
                       [" P ", "PGP", " P "],
                       {"P": item("minecraft:paper"), "G": item("minecraft:gunpowder")}))
    recs.append(shaped("bomb/det_charge", "hbm:det_charge", 1,
                       ["PDP", "DTD", "PDP"],
                       {"P": item("hbm:plate_steel"), "D": item("hbm:det_cord"),
                        "T": item("hbm:ingot_c4")}))
    recs.append(shaped("bomb/det_miner_dynamite", "hbm:det_miner", 4,
                       ["FFF", "ITI", "ITI"],
                       {"F": item("minecraft:flint"), "I": item("hbm:plate_iron"),
                        "T": item("hbm:ball_dynamite")}))
    recs.append(shaped("bomb/det_miner_plastic", "hbm:det_miner", 12,
                       ["FFF", "ITI", "ITI"],
                       {"F": item("minecraft:flint"), "I": item("hbm:plate_steel"),
                        "T": item("hbm:ingot_c4")}))
    recs.append(shaped("bomb/emp_bomb", "hbm:emp_bomb", 1,
                       ["LML", "LCL", "LML"],
                       {"L": item("hbm:plate_lead"), "M": item("hbm:magnetron"),
                        "C": item("hbm:circuit_advanced")}))
    recs.append(shapeless("bomb/charge_dynamite", "hbm:charge_dynamite", 1,
                          ["hbm:stick_dynamite", "hbm:stick_dynamite",
                           "hbm:stick_dynamite", "hbm:ducttape"]))
    recs.append(shaped("bomb/charge_miner", "hbm:charge_miner", 1,
                       [" F ", "FCF", " F "],
                       {"F": item("minecraft:flint"), "C": item("hbm:charge_dynamite")}))
    recs.append(shapeless("bomb/charge_semtex", "hbm:charge_semtex", 1,
                          ["hbm:stick_semtex", "hbm:stick_semtex",
                           "hbm:stick_semtex", "hbm:ducttape"]))
    recs.append(shapeless("bomb/charge_c4", "hbm:charge_c4", 1,
                          ["hbm:stick_c4", "hbm:stick_c4", "hbm:stick_c4", "hbm:ducttape"]))

    # :562-565
    recs.append(shaped("armor/hev_battery", "hbm:hev_battery", 4,
                       [" W ", "IEI", "ICI"],
                       {"W": item("hbm:gold_wire"), "I": item("hbm:plate_polymer"),
                        "E": item("minecraft:redstone"), "C": item("hbm:powder_cobalt")}))
    recs.append(shaped("armor/hev_battery_alt", "hbm:hev_battery", 4,
                       [" W ", "ICI", "IEI"],
                       {"W": item("hbm:gold_wire"), "I": item("hbm:plate_polymer"),
                        "E": item("minecraft:redstone"), "C": item("hbm:powder_cobalt")}))

    # :570
    recs.append(shaped("parts/wiring_red_copper", "hbm:wiring_red_copper", 1,
                       ["PPP", "PIP", "PPP"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))

    # :573-574
    recs.append(shapeless("tool/gun_kit_1", "hbm:gun_kit_1", 1,
                          ["tag:hbm:any_rubber", "tag:c:ingots/iron"]))
    recs.append(shapeless("tool/gun_kit_2", "hbm:gun_kit_2", 1,
                          ["hbm:gun_kit_1", "hbm:wrench", "hbm:ducttape"]))

    # :576-578
    recs.append(shaped("tool/igniter", "hbm:igniter", 1,
                       [" W", "SC", "CE"],
                       {"S": item("hbm:plate_steel"), "W": item("hbm:schrabidium_wire"),
                        "C": item("hbm:circuit_advanced"), "E": item("hbm:ingot_euphemium")}))
    recs.append(shaped("tool/key", "hbm:key", 1,
                       ["  B", " B ", "P  "],
                       {"P": item("hbm:plate_steel"), "B": item("hbm:steel_bolt")}))

    # :510 leftover from wave8 (rebar)
    recs.append(shaped("building/rebar", "hbm:rebar", 8,
                       ["BB", "BB"],
                       {"B": item("hbm:steel_bolt")}))

    # :516 wood barrier (skip wood_structure meta)
    recs.append(shaped("building/wood_barrier", "hbm:wood_barrier", 8,
                       ["SFS", "SFS"],
                       {"S": tag("minecraft:wooden_slabs"), "F": tag("c:fences/wooden")}))

    # :579-586 keys / padlocks (skip key_kit screwdriver + key_red late-game)
    recs.append(shaped("tool/pin", "hbm:pin", 1,
                       ["W ", " W", " W"],
                       {"W": item("hbm:copper_wire")}))
    recs.append(shaped("tool/padlock_rusty", "hbm:padlock_rusty", 1,
                       ["I", "B", "I"],
                       {"I": item("minecraft:iron_ingot"), "B": item("hbm:steel_bolt")}))
    recs.append(shaped("tool/padlock", "hbm:padlock", 1,
                       [" P ", "PBP", "PPP"],
                       {"P": item("hbm:plate_steel"), "B": item("hbm:steel_bolt")}))
    recs.append(shaped("tool/padlock_reinforced", "hbm:padlock_reinforced", 1,
                       [" P ", "PBP", "PDP"],
                       {"P": item("hbm:plate_dura_steel"), "D": item("hbm:plate_desh"),
                        "B": item("hbm:dura_steel_bolt")}))

    # :588-592 records / polaroid
    recs.append(shaped("consumable/record_lc", "hbm:record_lc", 1,
                       [" S ", "SDS", " S "],
                       {"S": tag("hbm:any_plastic"), "D": item("hbm:powder_lapis")}))
    recs.append(shaped("consumable/record_ss", "hbm:record_ss", 1,
                       [" S ", "SDS", " S "],
                       {"S": tag("hbm:any_plastic"), "D": item("hbm:powder_red_copper")}))
    recs.append(shaped("consumable/record_vc", "hbm:record_vc", 1,
                       [" S ", "SDS", " S "],
                       {"S": tag("hbm:any_plastic"), "D": item("hbm:powder_combine_steel")}))
    recs.append(shaped("consumable/polaroid", "hbm:polaroid", 1,
                       [" C ", "RPY", " B "],
                       {"B": item("hbm:powder_lapis"), "C": item("hbm:powder_coal"),
                        "R": item("hbm:powder_red_copper"), "Y": item("hbm:powder_gold"),
                        "P": item("minecraft:paper")}))

    # :598-611 fluid ducts / valves (skip fluid-NBT / meta neo ducts)
    recs.append(shaped("network/fluid_duct_paintable", "hbm:fluid_duct_paintable", 8,
                       ["SAS", "A A", "SAS"],
                       {"S": item("hbm:ingot_steel"), "A": item("hbm:plate_aluminium")}))
    recs.append(shaped("network/fluid_duct_paintable_block_exhaust",
                       "hbm:fluid_duct_paintable_block_exhaust", 8,
                       ["SAS", "A A", "SAS"],
                       {"S": item("minecraft:iron_ingot"), "A": item("hbm:plate_polymer")}))
    recs.append(shapeless("network/fluid_duct_gauge", "hbm:fluid_duct_gauge", 1,
                          ["hbm:fluid_duct_paintable", "hbm:ingot_steel", "hbm:circuit_basic"]))
    recs.append(shaped("network/fluid_valve", "hbm:fluid_valve", 1,
                       ["S", "W"],
                       {"S": item("minecraft:lever"), "W": item("hbm:fluid_duct_paintable")}))
    recs.append(shaped("network/fluid_switch", "hbm:fluid_switch", 1,
                       ["S", "W"],
                       {"S": item("minecraft:redstone"), "W": item("hbm:fluid_duct_paintable")}))
    recs.append(shaped("network/fluid_counter_valve", "hbm:fluid_counter_valve", 1,
                       ["S", "W"],
                       {"S": item("hbm:circuit_chip"), "W": item("hbm:fluid_switch")}))
    recs.append(shaped("network/fluid_pump", "hbm:fluid_pump", 1,
                       [" S ", "PGP", "IMI"],
                       {"S": item("hbm:steel_shell"), "P": item("hbm:steel_pipe"),
                        "G": item("hbm:ingot_graphite"), "I": item("hbm:ingot_steel"),
                        "M": item("hbm:motor")}))
    recs.append(shaped("network/pneumatic_tube", "hbm:pneumatic_tube", 8,
                       ["CRC"],
                       {"C": item("hbm:copper_plate_cast"), "R": tag("hbm:any_rubber")}))
    recs.append(shaped("network/pneumatic_tube_welded", "hbm:pneumatic_tube", 24,
                       ["CRC"],
                       {"C": item("hbm:copper_plate_welded"), "R": tag("hbm:any_rubber")}))
    recs.append(shaped("network/pneumatic_tube_paintable", "hbm:pneumatic_tube_paintable", 4,
                       ["SAS", "A A", "SAS"],
                       {"S": item("hbm:plate_steel"), "A": item("hbm:pneumatic_tube")}))
    recs.append(shaped("network/pipe_anchor", "hbm:pipe_anchor", 2,
                       ["P", "P", "S"],
                       {"P": item("hbm:steel_pipe"), "S": item("hbm:ingot_steel")}))

    # :613-617 tanks / folder (skip fluid-NBT pellet_antimatter / cell)
    recs.append(shaped("tool/template_folder", "hbm:template_folder", 1,
                       ["LPL", "BPB", "LPL"],
                       {"P": item("minecraft:paper"), "L": tag("c:dyes"), "B": tag("c:dyes")}))
    recs.append(shaped("tool/fluid_tank_empty", "hbm:fluid_tank_empty", 8,
                       ["121", "1G1", "121"],
                       {"1": item("hbm:plate_aluminium"), "2": item("hbm:plate_iron"),
                        "G": tag("c:glass_panes")}))
    recs.append(shaped("tool/fluid_tank_lead_empty", "hbm:fluid_tank_lead_empty", 4,
                       ["LUL", "LTL", "LUL"],
                       {"L": item("hbm:plate_lead"), "U": item("hbm:billet_uranium_238"),
                        "T": item("hbm:fluid_tank_empty")}))
    recs.append(shaped("tool/fluid_barrel_empty", "hbm:fluid_barrel_empty", 2,
                       ["121", "1G1", "121"],
                       {"1": item("hbm:plate_steel"), "2": item("hbm:plate_aluminium"),
                        "G": tag("c:glass_panes")}))

    # :637-650
    recs.append(shaped("parts/piston_selenium", "hbm:piston_selenium", 1,
                       ["SSS", "STS", " D "],
                       {"S": item("hbm:plate_steel"), "T": item("hbm:ingot_tungsten"),
                        "D": item("hbm:dura_steel_bolt")}))
    recs.append(shapeless("parts/catalyst_clay", "hbm:catalyst_clay", 1,
                          ["hbm:powder_iron", "minecraft:clay_ball"]))
    recs.append(shaped("parts/photo_panel", "hbm:photo_panel", 1,
                       [" G ", "IPI", " C "],
                       {"G": tag("c:glass_panes"), "I": item("hbm:plate_polymer"),
                        "P": item("hbm:powder_quartz"), "C": item("hbm:circuit_pcb")}))
    recs.append(shaped("machine/machine_satlinker", "hbm:machine_satlinker", 1,
                       ["PSP", "SCS", "PSP"],
                       {"P": item("hbm:plate_steel"), "S": item("hbm:ingot_starmetal"),
                        "C": item("hbm:sat_chip")}))
    recs.append(shaped("machine/machine_tape_drive", "hbm:machine_tape_drive", 1,
                       ["PPP", "CCC", "PPP"],
                       {"P": tag("hbm:any_plastic"), "C": item("hbm:circuit_pcb")}))
    recs.append(shaped("machine/machine_keyforge", "hbm:machine_keyforge", 1,
                       ["PCP", "WSW", "WSW"],
                       {"P": item("hbm:plate_steel"), "S": item("hbm:ingot_tungsten"),
                        "C": item("hbm:padlock"), "W": tag("minecraft:planks")}))
    recs.append(shaped("parts/sat_chip", "hbm:sat_chip", 1,
                       ["WWW", "CIC", "WWW"],
                       {"W": item("hbm:red_copper_wire"), "C": item("hbm:circuit_advanced"),
                        "I": tag("hbm:any_plastic")}))

    # :657-665
    recs.append(shapeless("tool/geiger_counter", "hbm:geiger_counter", 1,
                          ["hbm:geiger"]))
    recs.append(shaped("tool/sat_interface", "hbm:sat_interface", 1,
                       ["ISI", "PCP", "PAP"],
                       {"I": item("hbm:ingot_steel"), "S": item("hbm:ingot_starmetal"),
                        "P": item("hbm:plate_polymer"), "C": item("hbm:sat_chip"),
                        "A": item("hbm:circuit_advanced")}))
    recs.append(shaped("tool/sat_coord", "hbm:sat_coord", 1,
                       ["SII", "SCA", "SPP"],
                       {"I": item("hbm:ingot_steel"), "S": item("hbm:ingot_starmetal"),
                        "P": item("hbm:plate_polymer"), "C": item("hbm:sat_chip"),
                        "A": item("hbm:circuit_advanced")}))
    recs.append(shaped("machine/machine_transformer", "hbm:machine_transformer", 1,
                       ["SCS", "MDM", "SCS"],
                       {"S": item("minecraft:iron_ingot"), "D": item("hbm:ingot_red_copper"),
                        "M": item("hbm:coil_copper"), "C": item("hbm:circuit_capacitor")}))
    recs.append(shaped("machine/machine_transformer_dnt", "hbm:machine_transformer_dnt", 1,
                       ["SDS", "MCM", "MCM"],
                       {"S": item("hbm:ingot_starmetal"), "D": item("hbm:ingot_desh"),
                        "M": item("hbm:magnetized_tungsten_dense_wire"),
                        "C": item("hbm:circuit_bismoid")}))
    recs.append(shaped("machine/radiobox", "hbm:radiobox", 1,
                       ["PLP", "PSP", "PLP"],
                       {"P": item("hbm:plate_steel"), "S": item("hbm:ring_starmetal"),
                        "L": item("hbm:plate_dura_steel")}))
    recs.append(shaped("machine/radiorec", "hbm:radiorec", 1,
                       ["  W", "PCP", "PIP"],
                       {"W": item("hbm:copper_wire"), "P": item("hbm:plate_steel"),
                        "C": item("hbm:circuit_vacuum_tube"), "I": tag("hbm:any_plastic")}))

    # :664-675
    recs.append(shaped("armor/jackt", "hbm:jackt", 1,
                       ["S S", "LIL", "LIL"],
                       {"S": item("hbm:plate_steel"), "L": item("minecraft:leather"),
                        "I": tag("hbm:any_rubber")}))
    recs.append(shaped("armor/jackt2", "hbm:jackt2", 1,
                       ["S S", "LIL", "III"],
                       {"S": item("hbm:plate_steel"), "L": item("minecraft:leather"),
                        "I": tag("hbm:any_rubber")}))
    recs.append(shaped("machine/vent_chlorine", "hbm:vent_chlorine", 1,
                       ["IGI", "ICI", "IDI"],
                       {"I": item("hbm:plate_iron"), "G": item("minecraft:iron_bars"),
                        "C": item("hbm:pellet_gas"), "D": item("minecraft:dispenser")}))
    recs.append(shaped("machine/vent_chlorine_seal", "hbm:vent_chlorine_seal", 1,
                       ["ISI", "SCS", "ISI"],
                       {"I": item("hbm:ingot_saturnite"), "S": item("hbm:ingot_starmetal"),
                        "C": item("hbm:chlorine_pinwheel")}))
    recs.append(shaped("building/spikes", "hbm:spikes", 4,
                       ["BBB", "BBB", "TTT"],
                       {"B": item("hbm:steel_bolt"), "T": item("hbm:ingot_steel")}))
    recs.append(shaped("machine/machine_controller", "hbm:machine_controller", 1,
                       ["TDT", "DCD", "TDT"],
                       {"T": item("hbm:ingot_saturnite"), "D": item("hbm:crt_display"),
                        "C": item("hbm:circuit_advanced")}))
    recs.append(shaped("tool/containment_box", "hbm:containment_box", 1,
                       ["LUL", "UCU", "LUL"],
                       {"L": item("hbm:plate_lead"), "U": item("hbm:ingot_ferrouranium"),
                        "C": item("hbm:crate_steel")}))
    recs.append(shaped("tool/casing_bag_leather", "hbm:casing_bag", 1,
                       [" L ", "LGL", " L "],
                       {"L": item("minecraft:leather"), "G": item("hbm:plate_gunmetal")}))
    recs.append(shaped("tool/casing_bag_rubber", "hbm:casing_bag", 1,
                       [" L ", "LGL", " L "],
                       {"L": tag("hbm:any_rubber"), "G": item("hbm:plate_gunmetal")}))
    recs.append(shaped("tool/ammo_bag_leather", "hbm:ammo_bag", 1,
                       ["LLL", "MGM", "LLL"],
                       {"L": item("minecraft:leather"), "G": item("hbm:plate_weaponsteel"),
                        "M": item("hbm:mechanism_weaponsteel")}))
    recs.append(shaped("tool/ammo_bag_rubber", "hbm:ammo_bag", 1,
                       ["LLL", "MGM", "LLL"],
                       {"L": tag("hbm:any_rubber"), "G": item("hbm:plate_weaponsteel"),
                        "M": item("hbm:mechanism_weaponsteel")}))

    # :683-690 pink wood / doors
    recs.append(shaped("building/pink_planks", "hbm:pink_planks", 4,
                       ["W"],
                       {"W": item("hbm:pink_log")}))
    recs.append(shaped("building/pink_slab", "hbm:pink_slab", 6,
                       ["WWW"],
                       {"W": item("hbm:pink_planks")}))
    recs.append(shaped("building/pink_stairs", "hbm:pink_stairs", 6,
                       ["W  ", "WW ", "WWW"],
                       {"W": item("hbm:pink_planks")}))
    recs.append(shaped("building/door_metal", "hbm:door_metal", 1,
                       ["II", "SS", "II"],
                       {"I": item("hbm:plate_iron"), "S": item("hbm:plate_steel")}))
    recs.append(shaped("building/door_office", "hbm:door_office", 1,
                       ["II", "SS", "II"],
                       {"I": tag("minecraft:planks"), "S": item("hbm:plate_iron")}))
    recs.append(shaped("building/door_bunker", "hbm:door_bunker", 1,
                       ["II", "SS", "II"],
                       {"I": item("hbm:plate_steel"), "S": item("hbm:plate_lead")}))

    # :722-728 missile / segs
    recs.append(shaped("machine/machine_missile_assembly", "hbm:machine_missile_assembly", 1,
                       ["PWP", "SSS", "CCC"],
                       {"P": item("hbm:pedestal_steel"), "W": item("hbm:wrench"),
                        "S": item("hbm:plate_steel"), "C": item("hbm:steel_scaffold")}))
    recs.append(shaped("machine/struct_launcher", "hbm:struct_launcher", 8,
                       ["PPP", "SDS", "CCC"],
                       {"P": item("hbm:plate_steel"), "S": item("hbm:steel_scaffold"),
                        "D": item("hbm:steel_pipe"), "C": tag("hbm:any_concrete")}))
    recs.append(shaped("parts/seg_10", "hbm:seg_10", 1,
                       ["P", "S", "B"],
                       {"P": item("hbm:plate_aluminium"), "S": item("hbm:steel_scaffold"),
                        "B": item("hbm:steel_beam")}))
    recs.append(shaped("parts/seg_15", "hbm:seg_15", 1,
                       ["PP", "SS", "BB"],
                       {"P": item("hbm:plate_titanium"), "S": item("hbm:steel_scaffold"),
                        "B": item("hbm:steel_beam")}))
    recs.append(shaped("parts/seg_20", "hbm:seg_20", 1,
                       ["PGP", "SSS", "BBB"],
                       {"P": item("hbm:plate_steel"), "G": item("hbm:plate_gold"),
                        "S": item("hbm:steel_scaffold"), "B": item("hbm:steel_beam")}))

    # :538 leftover pellet_gas (no fluid NBT)
    recs.append(shapeless("consumable/pellet_gas", "hbm:pellet_gas", 2,
                          ["minecraft:water_bucket", "minecraft:glowstone_dust",
                           "hbm:plate_steel"]))

    # :564-565 hev item ↔ block
    recs.append(shapeless("armor/hev_battery_from_block", "hbm:hev_battery", 1,
                          ["hbm:hev_battery_block"]))
    recs.append(shapeless("building/hev_battery_block", "hbm:hev_battery_block", 1,
                          ["hbm:hev_battery"]))

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
        r'registerStamp\(\s*"([a-z0-9_]+)"',
        r'registerCrate\([^,]+,\s*"([a-z0-9_]+)"',
    ]
    for p in java.rglob("*.java"):
        text = p.read_text(errors="ignore")
        for pat in pats:
            known.update(re.findall(pat, text))
    known.update({
        "furnace_iron", "furnace_steel", "heater_firebox", "heater_oven",
        "heater_oilburner", "machine_sawmill",
        "circuit_vacuum_tube", "circuit_advanced", "circuit_basic",
        "circuit_pcb", "circuit_chip", "circuit_capacitor", "circuit_bismoid",
        "gold_wire", "schrabidium_wire", "steel_bolt", "copper_wire",
        "red_copper_wire",
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
            if ref.startswith("tag:"):
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
