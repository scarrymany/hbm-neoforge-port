#!/usr/bin/env python3
"""Leftover vanilla crafts from CE CraftingManager.java:724-1090 (post-wave9).

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten results.
Does not emit recipes whose result or hbm: ingredient is unregistered.
Does not rewrite assembler JSON. Skips fluid-NBT, OreDictionary.WILDCARD, LBSM-gated, commented.
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

    # :724 — fluid_duct_neo wildcard skipped
    # :730 obj_tester unregistered
    # :732 fence_metal already in ce_craft/building/fence_metal.json
    # :736-742 sands / trinitite — sands + trinitite unregistered

    # :767-768 barrels (plastic/tcalloy/antimatter unregistered)
    recs.append(shaped("machine/barrel_steel", "hbm:barrel_steel", 1,
                       ["IPI", "I I", "IPI"],
                       {"I": item("hbm:plate_steel"), "P": item("hbm:ingot_steel")}))

    # :774 energy_core (fusion_core/fuse unregistered)
    recs.append(shaped("parts/catalytic_converter", "hbm:catalytic_converter", 1,
                       ["PCP", "PBP", "PCP"],
                       {"P": tag("hbm:any_hardplastic"), "C": item("hbm:powder_cobalt"),
                        "B": item("hbm:ingot_bismuth")}))

    # :777-790 upgrades (crystallizer uses fluid-NBT barrel — skip)
    recs.append(shaped("upgrade/upgrade_nullifier", "hbm:upgrade_nullifier", 1,
                       ["SPS", "PUP", "SPS"],
                       {"S": item("hbm:plate_steel"), "P": item("hbm:powder_fire"),
                        "U": item("hbm:upgrade_template")}))
    recs.append(shaped("upgrade/upgrade_smelter", "hbm:upgrade_smelter", 1,
                       ["PHP", "CUC", "DTD"],
                       {"P": item("hbm:plate_copper"), "H": item("minecraft:hopper"),
                        "C": item("hbm:coil_tungsten"), "U": item("hbm:upgrade_template"),
                        "D": item("hbm:coil_copper"), "T": item("hbm:machine_transformer")}))
    recs.append(shaped("upgrade/upgrade_shredder", "hbm:upgrade_shredder", 1,
                       ["PHP", "CUC", "DTD"],
                       {"P": item("hbm:motor"), "H": item("minecraft:hopper"),
                        "C": item("hbm:blades_titanium"), "U": item("hbm:upgrade_smelter"),
                        "D": item("hbm:plate_titanium"), "T": item("hbm:machine_transformer")}))
    recs.append(shaped("upgrade/upgrade_centrifuge", "hbm:upgrade_centrifuge", 1,
                       ["PHP", "PUP", "DTD"],
                       {"P": item("hbm:centrifuge_element"), "H": item("minecraft:hopper"),
                        "U": item("hbm:upgrade_shredder"), "D": tag("hbm:any_plastic"),
                        "T": item("hbm:machine_transformer")}))
    recs.append(shaped("upgrade/upgrade_screm", "hbm:upgrade_screm", 1,
                       ["SUS", "SCS", "SUS"],
                       {"S": item("hbm:plate_steel"), "U": item("hbm:upgrade_template"),
                        "C": item("hbm:crystal_xen")}))
    recs.append(shaped("upgrade/upgrade_gc_speed", "hbm:upgrade_gc_speed", 1,
                       ["GNG", "RUR", "GMG"],
                       {"R": item("hbm:ingot_rubber"), "M": item("hbm:motor"),
                        "G": item("hbm:coil_gold"), "N": item("hbm:ingot_niobium"),
                        "U": item("hbm:upgrade_template")}))
    recs.append(shaped("upgrade/upgrade_stack_1", "hbm:upgrade_stack_1", 1,
                       [" C ", "PUP", " C "],
                       {"C": item("hbm:circuit_vacuum_tube"),
                        "P": item("hbm:part_generic_piston_pneumatic"),
                        "U": item("hbm:upgrade_template")}))
    recs.append(shaped("upgrade/upgrade_stack_2", "hbm:upgrade_stack_2", 1,
                       [" C ", "PUP", " C "],
                       {"C": item("hbm:circuit_capacitor"),
                        "P": item("hbm:part_generic_piston_hydraulic"),
                        "U": item("hbm:upgrade_stack_1")}))
    recs.append(shaped("upgrade/upgrade_stack_3", "hbm:upgrade_stack_3", 1,
                       [" C ", "PUP", " C "],
                       {"C": item("hbm:circuit_chip"),
                        "P": item("hbm:part_generic_piston_electric"),
                        "U": item("hbm:upgrade_stack_2")}))
    recs.append(shaped("upgrade/upgrade_ejector_1", "hbm:upgrade_ejector_1", 1,
                       [" C ", "PUP", " C "],
                       {"C": item("hbm:plate_copper"), "P": item("hbm:motor"),
                        "U": item("hbm:upgrade_template")}))
    recs.append(shaped("upgrade/upgrade_ejector_2", "hbm:upgrade_ejector_2", 1,
                       [" C ", "PUP", " C "],
                       {"C": item("hbm:plate_gold"), "P": item("hbm:motor"),
                        "U": item("hbm:upgrade_ejector_1")}))
    recs.append(shaped("upgrade/upgrade_ejector_3", "hbm:upgrade_ejector_3", 1,
                       [" C ", "PUP", " C "],
                       {"C": item("hbm:plate_saturnite"), "P": item("hbm:motor"),
                        "U": item("hbm:upgrade_ejector_2")}))

    # :792-793
    recs.append(shaped("tool/mech_key", "hbm:mech_key", 1,
                       ["MCM", "MKM", "MMM"],
                       {"M": item("hbm:ingot_meteorite_forged"), "C": item("hbm:coin_maskman"),
                        "K": item("hbm:key")}))
    recs.append(shaped("consumable/spawn_ufo", "hbm:spawn_ufo", 1,
                       ["MMM", "DCD", "MMM"],
                       {"M": item("hbm:ingot_meteorite"), "D": item("hbm:ingot_dineutronium"),
                        "C": item("hbm:coin_worm")}))

    # :831-835
    recs.append(shaped("parts/rbmk_lid", "hbm:rbmk_lid", 4,
                       ["PPP", "CCC", "PPP"],
                       {"P": item("hbm:plate_steel"), "C": item("hbm:concrete_asbestos")}))
    recs.append(shaped("parts/rbmk_lid_glass_a", "hbm:rbmk_lid_glass", 4,
                       ["LLL", "BBB", "P P"],
                       {"P": item("hbm:plate_steel"), "L": item("hbm:glass_lead"),
                        "B": item("hbm:glass_boron")}))
    recs.append(shaped("parts/rbmk_lid_glass_b", "hbm:rbmk_lid_glass", 4,
                       ["BBB", "LLL", "P P"],
                       {"P": item("hbm:plate_steel"), "L": item("hbm:glass_lead"),
                        "B": item("hbm:glass_boron")}))

    # :841-858 RBMK columns (!enable528 branch)
    recs.append(shaped("machine/rbmk_moderator", "hbm:rbmk_moderator", 1,
                       [" G ", "GRG", " G "],
                       {"G": item("hbm:block_graphite"), "R": item("hbm:rbmk_blank")}))
    recs.append(shaped("machine/rbmk_absorber", "hbm:rbmk_absorber", 1,
                       ["GGG", "GRG", "GGG"],
                       {"G": item("hbm:ingot_boron"), "R": item("hbm:rbmk_blank")}))
    recs.append(shaped("machine/rbmk_reflector", "hbm:rbmk_reflector", 1,
                       ["GGG", "GRG", "GGG"],
                       {"G": item("hbm:neutron_reflector"), "R": item("hbm:rbmk_blank")}))
    recs.append(shaped("machine/rbmk_control", "hbm:rbmk_control", 1,
                       [" B ", "GRG", " B "],
                       {"G": item("hbm:ingot_graphite"), "B": item("hbm:motor"),
                        "R": item("hbm:rbmk_absorber")}))
    recs.append(shaped("machine/rbmk_control_mod", "hbm:rbmk_control_mod", 1,
                       ["BGB", "GRG", "BGB"],
                       {"G": item("hbm:block_graphite"), "R": item("hbm:rbmk_control"),
                        "B": item("hbm:nugget_bismuth")}))
    recs.append(shaped("machine/rbmk_control_auto", "hbm:rbmk_control_auto", 1,
                       ["C", "R", "D"],
                       {"C": item("hbm:circuit_advanced"), "R": item("hbm:rbmk_control"),
                        "D": item("hbm:crt_display")}))
    recs.append(shaped("machine/rbmk_rod_reasim", "hbm:rbmk_rod_reasim", 1,
                       ["ZCZ", "ZRZ", "ZCZ"],
                       {"C": item("hbm:steel_shell"), "R": item("hbm:rbmk_blank"),
                        "Z": item("hbm:ingot_zirconium")}))
    recs.append(shaped("machine/rbmk_rod_reasim_mod", "hbm:rbmk_rod_reasim_mod", 1,
                       ["BGB", "GRG", "BGB"],
                       {"G": item("hbm:block_graphite"), "R": item("hbm:rbmk_rod_reasim"),
                        "B": item("hbm:ingot_saturnite")}))
    recs.append(shaped("machine/rbmk_outgasser", "hbm:rbmk_outgasser", 1,
                       ["GHG", "GRG", "GTG"],
                       {"G": item("hbm:steel_grate"), "H": item("minecraft:hopper"),
                        "T": item("hbm:tank_steel"), "R": item("hbm:rbmk_blank")}))
    recs.append(shaped("machine/rbmk_storage", "hbm:rbmk_storage", 1,
                       ["C", "R", "C"],
                       {"C": item("hbm:crate_steel"), "R": item("hbm:rbmk_blank")}))

    # :876-883 deco RBMK
    recs.append(shaped("building/deco_rbmk", "hbm:deco_rbmk", 8,
                       ["R"],
                       {"R": item("hbm:rbmk_blank")}))
    recs.append(shaped("building/deco_rbmk_smooth", "hbm:deco_rbmk_smooth", 1,
                       ["R"],
                       {"R": item("hbm:deco_rbmk")}))
    recs.append(shaped("building/deco_rbmk_panel", "hbm:deco_rbmk_panel", 1,
                       ["P", "R"],
                       {"P": item("hbm:plate_steel"), "R": item("hbm:deco_rbmk")}))
    recs.append(shaped("building/deco_rbmk_smooth_panel", "hbm:deco_rbmk_smooth_panel", 1,
                       ["P", "R"],
                       {"P": item("hbm:plate_steel"), "R": item("hbm:deco_rbmk_smooth")}))
    recs.append(shaped("building/rbmk_blank_from_deco", "hbm:rbmk_blank", 1,
                       ["RRR", "R R", "RRR"],
                       {"R": item("hbm:deco_rbmk")}))
    recs.append(shaped("building/rbmk_blank_from_smooth", "hbm:rbmk_blank", 1,
                       ["RRR", "R R", "RRR"],
                       {"R": item("hbm:deco_rbmk_smooth")}))

    # :890
    recs.append(shapeless("building/trapdoor_steel", "hbm:trapdoor_steel", 1,
                          ["minecraft:oak_trapdoor", "hbm:ingot_steel"]))

    # :892 Dummyable storage drum now registered
    recs.append(shaped("machine/machine_storage_drum", "hbm:machine_storage_drum", 1,
                       ["LLL", "L#L", "LLL"],
                       {"L": item("hbm:plate_lead"), "#": item("hbm:tank_steel")}))

    # :932 rag
    recs.append(shaped("consumable/rag", "hbm:rag", 4,
                       ["SW", "WS"],
                       {"S": item("minecraft:string"), "W": item("minecraft:white_wool")}))

    # :1010-1023 foundry / firebrick
    recs.append(shaped("parts/gear_large_iron", "hbm:gear_large", 1,
                       ["III", "ICI", "III"],
                       {"I": item("hbm:plate_iron"), "C": item("hbm:ingot_copper")}))
    recs.append(shaped("parts/sawblade", "hbm:sawblade", 1,
                       ["III", "ICI", "III"],
                       {"I": item("hbm:plate_steel"), "C": item("minecraft:iron_ingot")}))
    recs.append(shaped("building/brick_fire", "hbm:brick_fire", 1,
                       ["BB", "BB"],
                       {"B": item("hbm:ingot_firebrick")}))
    recs.append(shapeless("parts/ingot_firebrick_from_block", "hbm:ingot_firebrick", 4,
                          ["hbm:brick_fire"]))
    recs.append(shaped("parts/mold_base", "hbm:mold_base", 1,
                       [" B ", "BIB", " B "],
                       {"B": item("hbm:ingot_firebrick"), "I": item("minecraft:iron_ingot")}))

    # :1072-1074 Mats BOLT autogen (4 mats)
    recs.append(shaped("parts/tungsten_bolt", "hbm:tungsten_bolt", 16,
                       ["#", "#"],
                       {"#": item("hbm:ingot_tungsten")}))
    recs.append(shaped("parts/lead_bolt", "hbm:lead_bolt", 16,
                       ["#", "#"],
                       {"#": item("hbm:ingot_lead")}))
    recs.append(shaped("parts/steel_bolt_from_ingot", "hbm:steel_bolt", 16,
                       ["#", "#"],
                       {"#": item("hbm:ingot_steel")}))
    recs.append(shaped("parts/durasteel_bolt", "hbm:durasteel_bolt", 16,
                       ["#", "#"],
                       {"#": item("hbm:ingot_dura_steel")}))

    # :1081-1087 !enable528 RBMK leftover
    recs.append(shaped("tool/reactor_sensor", "hbm:reactor_sensor", 1,
                       ["WPW", "CMC", "PPP"],
                       {"W": item("hbm:tungsten_wire"), "P": item("hbm:plate_lead"),
                        "C": item("hbm:circuit_basic"), "M": item("hbm:magnetron")}))
    recs.append(shaped("machine/rbmk_console", "hbm:rbmk_console", 1,
                       ["BBB", "DGD", "DCD"],
                       {"B": item("hbm:ingot_boron"), "D": item("hbm:deco_rbmk"),
                        "G": tag("c:glass_panes"), "C": item("hbm:circuit_analog")}))
    recs.append(shaped("machine/rbmk_rod", "hbm:rbmk_rod", 1,
                       ["C", "R", "C"],
                       {"C": item("hbm:steel_shell"), "R": item("hbm:rbmk_blank")}))
    recs.append(shaped("machine/rbmk_rod_mod", "hbm:rbmk_rod_mod", 1,
                       ["BGB", "GRG", "BGB"],
                       {"G": item("hbm:block_graphite"), "R": item("hbm:rbmk_rod"),
                        "B": item("hbm:nugget_bismuth")}))
    recs.append(shaped("machine/rbmk_boiler", "hbm:rbmk_boiler", 1,
                       ["CPC", "CRC", "CPC"],
                       {"C": item("hbm:copper_pipe"), "P": item("hbm:copper_shell"),
                        "R": item("hbm:rbmk_blank")}))
    recs.append(shaped("machine/rbmk_heater", "hbm:rbmk_heater", 1,
                       ["CIC", "PRP", "CIC"],
                       {"C": item("hbm:copper_pipe"), "P": item("hbm:steel_shell"),
                        "R": item("hbm:rbmk_blank"), "I": tag("hbm:any_plastic")}))
    recs.append(shaped("machine/rbmk_cooler", "hbm:rbmk_cooler", 1,
                       ["IGI", "GCG", "IGI"],
                       {"C": item("hbm:rbmk_blank"), "I": item("hbm:plate_polymer"),
                        "G": item("hbm:steel_grate")}))

    recs.append(shaped("tool/radar_linker", "hbm:radar_linker", 1,
                       ["S", "C", "P"],
                       {"S": item("hbm:crt_display"), "C": item("hbm:circuit_basic"),
                        "P": item("hbm:plate_steel")}))

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
        r'registerParts\(\s*"([a-z0-9_]+)"',
        r'fuel\(\s*"([a-z0-9_]+)"',
        r'control\(\s*"([a-z0-9_]+)"',
        r'nuke\(\s*"([a-z0-9_]+)"',
        r'food\(\s*"([a-z0-9_]+)"',
        r'registerIngot\(\s*"([a-z0-9_]+)"',
        r'registerNugget\(\s*"([a-z0-9_]+)"',
        r'registerPowder\(\s*"([a-z0-9_]+)"',
        r'registerLoreIngot\(\s*"([a-z0-9_]+)"',
        r'registerLoreNugget\(\s*"([a-z0-9_]+)"',
        r'registerStamp\(\s*"([a-z0-9_]+)"',
        r'registerCrate\([^,]+,\s*"([a-z0-9_]+)"',
    ]
    for p in java.rglob("*.java"):
        text = p.read_text(errors="ignore")
        for pat in pats:
            known.update(re.findall(pat, text))
    known.update({
        "furnace_iron", "furnace_steel", "heater_firebox", "heater_oven",
        "heater_oilburner", "machine_sawmill", "heater_electric", "heater_heatex",
        "machine_stirling", "machine_storage_drum", "machine_supercomputer",
        "machine_autosaw",
        "circuit_vacuum_tube", "circuit_advanced", "circuit_basic",
        "circuit_pcb", "circuit_chip", "circuit_capacitor", "circuit_bismoid",
        "circuit_analog", "circuit_numitron",
        "part_generic_piston_pneumatic", "part_generic_piston_hydraulic",
        "part_generic_piston_electric",
        "gold_wire", "schrabidium_wire", "steel_bolt", "copper_wire",
        "red_copper_wire", "tungsten_wire", "tungsten_bolt", "lead_bolt",
        "durasteel_bolt", "steel_shell", "copper_shell", "copper_pipe",
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
