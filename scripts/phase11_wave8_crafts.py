#!/usr/bin/env python3
"""Leftover vanilla crafts from CE CraftingManager.java:298-509.

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten results.
Does not emit recipes whose result or hbm: ingredient is unregistered.
Does not rewrite assembler JSON. Does not emit vanilla hopper/bucket overwrites.
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

    # CraftingManager.java:298-304
    recs.append(shaped("machine/machine_wood_burner", "hbm:machine_wood_burner", 1,
                       ["PPP", "CFC", "I I"],
                       {"P": item("hbm:plate_steel"), "C": item("hbm:coil_copper"),
                        "I": item("minecraft:iron_ingot"), "F": item("minecraft:furnace")}))
    recs.append(shaped("machine/machine_turbine", "hbm:machine_turbine", 1,
                       ["SMS", "PTP", "SMS"],
                       {"S": item("hbm:ingot_steel"), "T": item("hbm:turbine_titanium"),
                        "M": item("hbm:coil_copper"), "P": tag("hbm:any_plastic")}))
    recs.append(shaped("storage/crate_iron", "hbm:crate_iron", 1,
                       ["PPP", "I I", "III"],
                       {"P": item("hbm:plate_iron"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("storage/crate_steel", "hbm:crate_steel", 1,
                       ["PPP", "I I", "III"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))

    # :326-345
    recs.append(shaped("machine/machine_autocrafter", "hbm:machine_autocrafter", 1,
                       ["SCS", "MWM", "SCS"],
                       {"S": item("hbm:plate_steel"), "C": item("hbm:circuit_vacuum_tube"),
                        "M": item("hbm:motor"), "W": item("minecraft:crafting_table")}))
    recs.append(shaped("machine/machine_funnel", "hbm:machine_funnel", 1,
                       ["S S", "SRS", " S "],
                       {"S": item("hbm:ingot_steel"), "R": item("minecraft:redstone")}))
    recs.append(shaped("machine/machine_waste_drum", "hbm:machine_waste_drum", 1,
                       ["LRL", "BRB", "LRL"],
                       {"L": item("hbm:ingot_lead"), "B": item("minecraft:iron_bars"),
                        "R": item("hbm:rod_quad_empty")}))
    recs.append(shaped("machine/machine_press", "hbm:machine_press", 1,
                       ["IRI", "IPI", "IBI"],
                       {"I": item("minecraft:iron_ingot"), "R": item("minecraft:furnace"),
                        "B": item("minecraft:iron_block"), "P": item("minecraft:piston")}))
    recs.append(shaped("machine/machine_ammo_press", "hbm:machine_ammo_press", 1,
                       ["IPI", "C C", "SSS"],
                       {"I": item("minecraft:iron_ingot"), "P": item("minecraft:piston"),
                        "C": item("hbm:ingot_copper"), "S": item("minecraft:stone")}))
    recs.append(shaped("machine/machine_siren", "hbm:machine_siren", 1,
                       ["SIS", "ICI", "SRS"],
                       {"S": item("hbm:plate_steel"), "I": tag("hbm:any_rubber"),
                        "C": item("hbm:circuit_vacuum_tube"), "R": item("minecraft:redstone")}))
    recs.append(shaped("machine/machine_microwave", "hbm:machine_microwave", 1,
                       ["III", "SGM", "IDI"],
                       {"I": item("hbm:plate_polymer"), "S": item("hbm:plate_steel"),
                        "G": tag("c:glass_panes"), "M": item("hbm:magnetron"),
                        "D": item("hbm:motor")}))
    recs.append(shaped("machine/machine_solar_boiler", "hbm:machine_solar_boiler", 1,
                       ["SHS", "DHD", "SHS"],
                       {"S": item("hbm:ingot_steel"), "H": item("hbm:steel_shell"),
                        "D": item("minecraft:black_dye")}))
    recs.append(shaped("machine/solar_mirror", "hbm:solar_mirror", 3,
                       ["AAA", " B ", "SSS"],
                       {"A": item("hbm:plate_aluminium"), "B": item("hbm:steel_beam"),
                        "S": item("hbm:ingot_steel")}))
    recs.append(shaped("machine/anvil_iron", "hbm:anvil_iron", 1,
                       ["III", " B ", "III"],
                       {"I": item("minecraft:iron_ingot"), "B": item("minecraft:iron_block")}))
    recs.append(shaped("machine/anvil_lead", "hbm:anvil_lead", 1,
                       ["III", " B ", "III"],
                       {"I": item("hbm:ingot_lead"), "B": item("hbm:block_lead")}))
    recs.append(shaped("machine/machine_fraction_tower", "hbm:machine_fraction_tower", 1,
                       ["H", "G", "H"],
                       {"H": item("hbm:steel_plate_welded"), "G": item("hbm:steel_grate")}))
    recs.append(shaped("machine/fraction_spacer", "hbm:fraction_spacer", 1,
                       ["BHB"],
                       {"H": item("hbm:steel_shell"), "B": item("minecraft:iron_bars")}))
    recs.append(shaped("machine/furnace_iron", "hbm:furnace_iron", 1,
                       ["III", "IFI", "BBB"],
                       {"I": item("minecraft:iron_ingot"), "F": item("minecraft:furnace"),
                        "B": item("minecraft:stone_bricks")}))
    recs.append(shaped("machine/machine_mixer", "hbm:machine_mixer", 1,
                       ["PIP", "GCG", "PMP"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_dura_steel"),
                        "G": tag("c:glass_panes"), "C": item("hbm:circuit_vacuum_tube"),
                        "M": item("hbm:motor")}))
    recs.append(shaped("machine/fan", "hbm:fan", 1,
                       ["BPB", "PRP", "BPB"],
                       {"B": item("hbm:steel_bolt"), "P": item("hbm:plate_iron"),
                        "R": item("minecraft:redstone")}))

    # :349-351
    recs.append(shaped("parts/upgrade_muffler", "hbm:upgrade_muffler", 16,
                       ["III", "IWI", "III"],
                       {"I": tag("hbm:any_rubber"), "W": item("minecraft:white_wool")}))
    recs.append(shaped("parts/upgrade_template_analog", "hbm:upgrade_template", 1,
                       ["WIW", "PCP", "WIW"],
                       {"W": item("hbm:copper_wire"), "I": item("hbm:plate_iron"),
                        "C": item("hbm:circuit_analog"), "P": item("hbm:plate_polymer")}))
    recs.append(shaped("parts/upgrade_template_basic", "hbm:upgrade_template", 1,
                       ["WIW", "PCP", "WIW"],
                       {"W": item("hbm:copper_wire"), "I": tag("hbm:any_plastic"),
                        "C": item("hbm:circuit_basic"), "P": item("hbm:plate_polymer")}))

    # :353-358 electrodes
    recs.append(shaped("parts/arc_electrode_graphite_bolt", "hbm:arc_electrode_graphite", 1,
                       ["C", "T", "C"],
                       {"C": item("hbm:ingot_graphite"), "T": item("hbm:steel_bolt")}))
    recs.append(shaped("parts/arc_electrode_lanthanium", "hbm:arc_electrode_lanthanium", 1,
                       ["C", "T", "C"],
                       {"C": item("hbm:ingot_lanthanium"), "T": item("minecraft:brick")}))
    recs.append(shaped("parts/arc_electrode_desh_ti", "hbm:arc_electrode_desh", 1,
                       ["C", "T", "C"],
                       {"C": item("hbm:ingot_desh"), "T": item("hbm:ingot_titanium")}))
    recs.append(shaped("parts/arc_electrode_desh_w", "hbm:arc_electrode_desh", 1,
                       ["C", "T", "C"],
                       {"C": item("hbm:ingot_desh"), "T": item("hbm:ingot_tungsten")}))
    recs.append(shaped("parts/arc_electrode_saturnite", "hbm:arc_electrode_saturnite", 1,
                       ["C", "T", "C"],
                       {"C": item("hbm:ingot_saturnite"), "T": item("hbm:ingot_niobium")}))

    # :360-364 detonators
    recs.append(shaped("tools/detonator", "hbm:detonator", 1,
                       ["C", "S"],
                       {"S": item("hbm:plate_steel"), "C": item("hbm:circuit_basic")}))
    recs.append(shapeless("tools/detonator_multi", "hbm:detonator_multi", 1,
                          ["hbm:detonator", "hbm:circuit_advanced"]))
    recs.append(shapeless("tools/detonator_laser", "hbm:detonator_laser", 1,
                          ["hbm:rangefinder", "hbm:circuit_advanced", "hbm:ingot_rubber",
                           "hbm:gold_dense_wire"]))
    recs.append(shapeless("tools/detonator_deadman", "hbm:detonator_deadman", 1,
                          ["hbm:detonator", "hbm:defuser", "hbm:ducttape"]))
    recs.append(shaped("tools/detonator_de", "hbm:detonator_de", 1,
                       ["T", "D", "T"],
                       {"T": item("minecraft:tnt"), "D": item("hbm:detonator_deadman")}))

    # :374 fuse (overfuse skipped — unregistered dusts)
    recs.append(shapeless("parts/fuse", "hbm:fuse", 1,
                          ["hbm:plate_steel", "hbm:plate_polymer", "hbm:tungsten_wire"]))

    # :378-383 blades
    recs.append(shaped("parts/blades_steel", "hbm:blades_steel", 1,
                       [" P ", "PIP", " P "],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("parts/blades_titanium", "hbm:blades_titanium", 1,
                       [" P ", "PIP", " P "],
                       {"P": item("hbm:plate_titanium"), "I": item("hbm:ingot_titanium")}))
    recs.append(shaped("parts/blades_desh", "hbm:blades_desh", 1,
                       [" P ", "PBP", " P "],
                       {"P": item("hbm:plate_desh"), "B": item("hbm:blades_titanium")}))
    recs.append(shaped("parts/blades_steel_repair", "hbm:blades_steel", 1,
                       ["PIP"],
                       {"P": item("hbm:plate_steel"), "I": item("hbm:blades_steel")}))
    recs.append(shaped("parts/blades_titanium_repair", "hbm:blades_titanium", 1,
                       ["PIP"],
                       {"P": item("hbm:plate_titanium"), "I": item("hbm:blades_titanium")}))

    # :390-399 stamps (brick + nether brick)
    for brick, suffix in (("minecraft:brick", "brick"), ("minecraft:nether_brick", "nether")):
        recs.append(shaped(f"parts/stamp_stone_flat_{suffix}", "hbm:stamp_stone_flat", 1,
                           ["III", "SSS"],
                           {"I": item(brick), "S": tag("c:stones")}))
        recs.append(shaped(f"parts/stamp_iron_flat_{suffix}", "hbm:stamp_iron_flat", 1,
                           ["III", "SSS"],
                           {"I": item(brick), "S": item("minecraft:iron_ingot")}))
        recs.append(shaped(f"parts/stamp_steel_flat_{suffix}", "hbm:stamp_steel_flat", 1,
                           ["III", "SSS"],
                           {"I": item(brick), "S": item("hbm:ingot_steel")}))
        recs.append(shaped(f"parts/stamp_titanium_flat_{suffix}", "hbm:stamp_titanium_flat", 1,
                           ["III", "SSS"],
                           {"I": item(brick), "S": item("hbm:ingot_titanium")}))
        recs.append(shaped(f"parts/stamp_obsidian_flat_{suffix}", "hbm:stamp_obsidian_flat", 1,
                           ["III", "SSS"],
                           {"I": item(brick), "S": item("minecraft:obsidian")}))
        recs.append(shaped(f"parts/stamp_desh_flat_{suffix}", "hbm:stamp_desh_flat", 1,
                           ["BDB", "DSD", "BDB"],
                           {"B": item(brick), "D": item("hbm:ingot_desh"),
                            "S": item("hbm:ingot_ferrouranium")}))

    recs.append(shaped("machine/watz_pump", "hbm:watz_pump", 1,
                       ["MPM", "PCP", "PSP"],
                       {"M": item("hbm:motor_desh"), "P": item("hbm:saturnite_plate_cast"),
                        "C": item("hbm:circuit_bismoid"), "S": item("hbm:pipes_steel")}))

    # :403-428 building
    recs.append(shaped("building/reinforced_stone", "hbm:reinforced_stone", 4,
                       ["FBF", "BFB", "FBF"],
                       {"F": item("minecraft:cobblestone"), "B": item("minecraft:stone")}))
    recs.append(shaped("building/brick_light", "hbm:brick_light", 4,
                       ["FBF", "BFB", "FBF"],
                       {"F": tag("c:fences/wooden"), "B": item("minecraft:bricks")}))
    recs.append(shaped("building/brick_asbestos", "hbm:brick_asbestos", 2,
                       [" A ", "ABA", " A "],
                       {"B": item("hbm:brick_light"), "A": item("hbm:ingot_asbestos")}))
    recs.append(shaped("building/concrete", "hbm:concrete", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:concrete_smooth")}))
    recs.append(shaped("building/concrete_pillar", "hbm:concrete_pillar", 6,
                       ["CBC", "CBC", "CBC"],
                       {"C": item("hbm:concrete_smooth"), "B": item("minecraft:iron_bars")}))
    recs.append(shaped("building/brick_concrete_from_smooth", "hbm:brick_concrete", 4,
                       [" C ", "CBC", " C "],
                       {"C": item("hbm:concrete_smooth"), "B": item("minecraft:clay_ball")}))
    recs.append(shaped("building/brick_concrete_from_block", "hbm:brick_concrete", 4,
                       [" C ", "CBC", " C "],
                       {"C": item("hbm:concrete"), "B": item("minecraft:clay_ball")}))
    recs.append(shaped("building/brick_concrete_mossy", "hbm:brick_concrete_mossy", 8,
                       ["CCC", "CVC", "CCC"],
                       {"C": item("hbm:brick_concrete"), "V": item("minecraft:vine")}))
    recs.append(shaped("building/brick_concrete_cracked", "hbm:brick_concrete_cracked", 6,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:brick_concrete")}))
    recs.append(shaped("building/brick_concrete_broken", "hbm:brick_concrete_broken", 6,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:brick_concrete_cracked")}))
    recs.append(shaped("building/ducrete", "hbm:ducrete", 4,
                       ["DD", "DD"],
                       {"D": item("hbm:ducrete_smooth")}))
    recs.append(shaped("building/brick_obsidian", "hbm:brick_obsidian", 4,
                       ["FBF", "BFB", "FBF"],
                       {"F": item("minecraft:iron_bars"), "B": item("minecraft:obsidian")}))
    recs.append(shaped("building/meteor_polished", "hbm:meteor_polished", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:block_meteor_broken")}))
    recs.append(shaped("building/meteor_pillar", "hbm:meteor_pillar", 2,
                       ["C", "C"],
                       {"C": item("hbm:meteor_polished")}))
    recs.append(shaped("building/meteor_brick", "hbm:meteor_brick", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:meteor_polished")}))
    recs.append(shaped("building/meteor_brick_mossy", "hbm:meteor_brick_mossy", 8,
                       ["CCC", "CVC", "CCC"],
                       {"C": item("hbm:meteor_brick"), "V": item("minecraft:vine")}))
    recs.append(shaped("building/meteor_brick_cracked", "hbm:meteor_brick_cracked", 6,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:meteor_brick")}))
    recs.append(shaped("building/tile_lab", "hbm:tile_lab", 4,
                       ["CBC", "CBC", "CBC"],
                       {"C": item("minecraft:brick"), "B": item("hbm:ingot_asbestos")}))
    recs.append(shaped("building/tile_lab_cracked", "hbm:tile_lab_cracked", 6,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:tile_lab")}))
    recs.append(shaped("building/tile_lab_broken", "hbm:tile_lab_broken", 6,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:tile_lab_cracked")}))
    recs.append(shapeless("building/asphalt_light", "hbm:asphalt_light", 1,
                          ["hbm:asphalt", "minecraft:glowstone_dust"]))
    recs.append(shapeless("building/asphalt_from_light", "hbm:asphalt", 1,
                          ["hbm:asphalt_light"]))

    # :456-470 stone families
    recs.append(shaped("building/gneiss_tile", "hbm:gneiss_tile", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:stone_gneiss")}))
    recs.append(shaped("building/gneiss_brick", "hbm:gneiss_brick", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:gneiss_tile")}))
    recs.append(shapeless("building/gneiss_chiseled", "hbm:gneiss_chiseled", 1,
                          ["hbm:gneiss_tile"]))
    recs.append(shaped("building/depth_brick", "hbm:depth_brick", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:stone_depth")}))
    recs.append(shaped("building/depth_tiles", "hbm:depth_tiles", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:depth_brick")}))
    recs.append(shaped("building/depth_nether_brick", "hbm:depth_nether_brick", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:stone_depth_nether")}))
    recs.append(shaped("building/depth_nether_tiles", "hbm:depth_nether_tiles", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:depth_nether_brick")}))
    recs.append(shaped("building/basalt_polished", "hbm:basalt_polished", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:basalt_smooth")}))
    recs.append(shaped("building/basalt_brick", "hbm:basalt_brick", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:basalt_polished")}))
    recs.append(shaped("building/basalt_tiles", "hbm:basalt_tiles", 4,
                       ["CC", "CC"],
                       {"C": item("hbm:basalt_brick")}))
    recs.append(shapeless("building/lightstone", "hbm:lightstone", 4,
                          ["minecraft:stone", "minecraft:stone", "minecraft:stone",
                           "hbm:powder_limestone"]))

    # :472-486 reinforced
    recs.append(shaped("building/reinforced_brick", "hbm:reinforced_brick", 4,
                       ["FBF", "BFB", "FBF"],
                       {"F": item("minecraft:iron_bars"), "B": item("hbm:brick_concrete")}))
    recs.append(shaped("building/brick_compound", "hbm:brick_compound", 4,
                       ["FBF", "BTB", "FBF"],
                       {"F": item("hbm:steel_bolt"), "B": item("hbm:reinforced_brick"),
                        "T": tag("hbm:any_tar")}))
    recs.append(shaped("building/reinforced_glass", "hbm:reinforced_glass", 4,
                       ["FBF", "BFB", "FBF"],
                       {"F": item("minecraft:iron_bars"), "B": item("minecraft:glass")}))
    recs.append(shaped("building/reinforced_glass_pane", "hbm:reinforced_glass_pane", 16,
                       ["GGG", "GGG"],
                       {"G": item("hbm:reinforced_glass")}))
    recs.append(shaped("building/reinforced_laminate_pane", "hbm:reinforced_laminate_pane", 16,
                       ["LLL", "LLL"],
                       {"L": item("hbm:reinforced_laminate")}))
    recs.append(shaped("building/reinforced_light", "hbm:reinforced_light", 1,
                       ["FFF", "FBF", "FFF"],
                       {"F": item("minecraft:iron_bars"), "B": item("minecraft:glowstone")}))
    recs.append(shaped("building/reinforced_lamp_off", "hbm:reinforced_lamp_off", 1,
                       ["FFF", "FBF", "FFF"],
                       {"F": item("minecraft:iron_bars"), "B": item("minecraft:redstone_lamp")}))
    recs.append(shaped("building/reinforced_sand", "hbm:reinforced_sand", 4,
                       ["FBF", "BFB", "FBF"],
                       {"F": item("minecraft:iron_bars"), "B": item("minecraft:sandstone")}))
    recs.append(shaped("building/lantern", "hbm:lantern", 1,
                       ["PGP", " S ", " S "],
                       {"P": tag("c:glass_panes"), "G": item("minecraft:glowstone_dust"),
                        "S": item("hbm:steel_beam")}))
    recs.append(shaped("building/spotlight_incandescent", "hbm:spotlight_incandescent", 8,
                       ["G", "T", "I"],
                       {"G": tag("c:glass_panes"), "T": item("hbm:tungsten_wire"),
                        "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("building/spotlight_fluoro", "hbm:spotlight_fluoro", 8,
                       ["G", "M", "A"],
                       {"G": tag("c:glass_panes"), "M": item("hbm:ingot_mercury"),
                        "A": item("hbm:plate_aluminium")}))
    recs.append(shaped("building/spotlight_halogen", "hbm:spotlight_halogen", 8,
                       ["G", "B", "S"],
                       {"G": tag("c:glass_panes"), "B": item("hbm:powder_bromine"),
                        "S": item("hbm:plate_steel")}))

    # :488-494 barbed (skip acid/ultradeath fluid-NBT)
    recs.append(shaped("building/barbed_wire", "hbm:barbed_wire", 16,
                       ["AIA", "I I", "AIA"],
                       {"A": item("hbm:steel_wire"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("building/barbed_wire_fire", "hbm:barbed_wire_fire", 8,
                       ["BBB", "BIB", "BBB"],
                       {"B": item("hbm:barbed_wire"), "I": item("minecraft:redstone")}))
    recs.append(shaped("building/barbed_wire_poison", "hbm:barbed_wire_poison", 8,
                       ["BBB", "BIB", "BBB"],
                       {"B": item("hbm:barbed_wire"), "I": item("hbm:powder_poison")}))
    recs.append(shaped("building/barbed_wire_wither", "hbm:barbed_wire_wither", 8,
                       ["BBB", "BIB", "BBB"],
                       {"B": item("hbm:barbed_wire"), "I": item("minecraft:wither_skeleton_skull")}))
    recs.append(shapeless("building/sandbags", "hbm:sandbags", 4,
                          ["hbm:plate_polymer", "tag:c:sands", "tag:c:sands", "tag:c:sands"]))

    # :496-509 steel deco
    recs.append(shaped("building/tape_recorder", "hbm:tape_recorder", 4,
                       ["TST", "SSS"],
                       {"T": item("hbm:ingot_tungsten"), "S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/steel_poles", "hbm:steel_poles", 16,
                       ["S S", "SSS", "S S"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/pole_top", "hbm:pole_top", 1,
                       ["T T", "TRT", "BBB"],
                       {"T": item("hbm:ingot_tungsten"), "B": item("hbm:ingot_beryllium"),
                        "R": item("hbm:ingot_red_copper")}))
    recs.append(shaped("building/pole_satellite_receiver", "hbm:pole_satellite_receiver", 1,
                       ["SS ", "SCR", "SS "],
                       {"S": item("hbm:ingot_steel"), "C": item("hbm:circuit_vacuum_tube"),
                        "R": item("hbm:copper_wire")}))
    recs.append(shaped("building/steel_beam", "hbm:steel_beam", 8,
                       ["S", "S", "S"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/steel_wall", "hbm:steel_wall", 4,
                       ["SSS", "SSS"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shapeless("building/steel_corner", "hbm:steel_corner", 1,
                          ["hbm:steel_wall", "hbm:steel_wall"]))
    recs.append(shaped("building/steel_roof", "hbm:steel_roof", 2,
                       ["SSS"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/steel_scaffold", "hbm:steel_scaffold", 8,
                       ["SSS", " S ", "SSS"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/steel_beam_from_scaffold", "hbm:steel_beam", 8,
                       ["S", "S", "S"],
                       {"S": item("hbm:steel_scaffold")}))
    recs.append(shaped("building/steel_grate", "hbm:steel_grate", 4,
                       ["SS", "SS"],
                       {"S": item("hbm:steel_beam")}))
    recs.append(shaped("building/steel_grate_wide", "hbm:steel_grate_wide", 4,
                       ["SS"],
                       {"S": item("hbm:steel_grate")}))
    recs.append(shaped("building/steel_grate_from_wide", "hbm:steel_grate", 1,
                       ["SS"],
                       {"S": item("hbm:steel_grate_wide")}))
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
        "particle_empty", "particle_hydrogen", "particle_copper", "particle_lead",
        "particle_amat", "particle_aschrab", "particle_dark", "particle_higgs",
        "particle_tachyon", "particle_strange", "particle_sparkticle",
        "circuit_vacuum_tube", "circuit_numitron", "circuit_capacitor",
        "circuit_capacitor_tantalium", "circuit_pcb", "circuit_chip",
        "circuit_chip_bismoid", "circuit_analog", "circuit_basic",
        "circuit_advanced", "circuit_bismoid", "circuit_controller",
        "circuit_controller_chassis",
        "machine_wood_burner",
        "crate_iron", "crate_steel",
        "stamp_stone_flat", "stamp_iron_flat", "stamp_steel_flat",
        "stamp_titanium_flat", "stamp_obsidian_flat", "stamp_desh_flat",
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
