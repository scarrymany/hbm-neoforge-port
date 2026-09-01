#!/usr/bin/env python3
"""CE leftover vanilla crafts whose I/O is already registered.

CraftingManager / ConsumableRecipes / SmeltingRecipes only. No invented rows.
Does not overwrite existing JSON. Skips banned results and fluid-meta crafts.
"""
from __future__ import annotations

import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "src/main/resources/data/hbm/recipe/ce_craft"


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
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": i} for i in ings],
        "result": {"id": result, "count": count},
        "_path": path,
    }


def smelting(path, inp, result, count):
    return {
        "type": "minecraft:smelting",
        "category": "misc",
        "ingredient": {"item": inp},
        "result": {"id": result, "count": count},
        "experience": 0.1,
        "cookingtime": 200,
        "_path": path,
    }


def item(iid):
    return {"item": iid}


def recipes():
    recs = []
    # CraftingManager.java:129 / :132
    recs.append(shaped("parts/lightstone_tile_stairs", "hbm:lightstone_tile_stairs", 4,
                       ["#  ", "## ", "###"], {"#": item("hbm:lightstone_tile_slab")}))
    recs.append(shaped("parts/lightstone_bricks_stairs", "hbm:lightstone_bricks_stairs", 4,
                       ["#  ", "## ", "###"], {"#": item("hbm:lightstone_bricks_slab")}))
    # :371
    recs.append(shaped("parts/black_hole", "hbm:black_hole", 1,
                       ["SSS", "SCS", "SSS"],
                       {"C": item("hbm:singularity"), "S": item("hbm:crystal_xen")}))
    # :418
    recs.append(shaped("parts/meteor_polished", "hbm:meteor_polished", 4,
                       ["CC", "CC"], {"C": item("hbm:block_meteor_broken")}))
    # :456-457
    recs.append(shaped("parts/gneiss_tile", "hbm:gneiss_tile", 4,
                       ["CC", "CC"], {"C": item("hbm:stone_gneiss")}))
    recs.append(shaped("parts/gneiss_brick", "hbm:gneiss_brick", 4,
                       ["CC", "CC"], {"C": item("hbm:gneiss_tile")}))
    # :477
    recs.append(shaped("parts/reinforced_light", "hbm:reinforced_light", 1,
                       ["FFF", "FBF", "FFF"],
                       {"F": item("minecraft:iron_bars"), "B": item("minecraft:glowstone")}))
    # :493
    recs.append(shaped("parts/barbed_wire_ultradeath", "hbm:barbed_wire_ultradeath", 4,
                       ["BCB", "CIC", "BCB"],
                       {"B": item("hbm:barbed_wire"), "C": item("hbm:powder_yellowcake"),
                        "I": item("hbm:nuclear_waste")}))
    # :640 / :641 (two patterns, same result)
    recs.append(shaped("parts/singularity_spark", "hbm:singularity_spark", 1,
                       ["XAX", "BCB", "XAX"],
                       {"X": item("hbm:plate_dineutronium"), "A": item("hbm:singularity_counter_resonant"),
                        "B": item("hbm:singularity_super_heated"), "C": item("hbm:black_hole")}))
    recs.append(shaped("parts/singularity_spark_alt", "hbm:singularity_spark", 1,
                       ["XBX", "ACA", "XBX"],
                       {"X": item("hbm:plate_dineutronium"), "A": item("hbm:singularity_counter_resonant"),
                        "B": item("hbm:singularity_super_heated"), "C": item("hbm:black_hole")}))
    # :643
    recs.append(shaped("parts/ams_core_wormhole", "hbm:ams_core_wormhole", 1,
                       ["DPD", "PSP", "DPD"],
                       {"D": item("hbm:plate_dineutronium"), "P": item("hbm:powder_spark_mix"),
                        "S": item("hbm:singularity")}))
    # :683
    recs.append(shaped("parts/pink_planks", "hbm:pink_planks", 4,
                       ["W"], {"W": item("hbm:pink_log")}))
    # :750
    recs.append(shaped("parts/ams_lens", "hbm:ams_lens", 1,
                       ["PDP", "GDG", "PDP"],
                       {"P": item("hbm:plate_dineutronium"), "G": item("hbm:reinforced_glass"),
                        "D": item("minecraft:diamond_block")}))
    # :880-881
    recs.append(shaped("parts/deco_rbmk_panel_slab", "hbm:deco_rbmk_panel_slab", 8,
                       ["R"], {"R": item("hbm:deco_rbmk_panel")}))
    recs.append(shaped("parts/deco_rbmk_smooth_panel_slab", "hbm:deco_rbmk_smooth_panel_slab", 8,
                       ["R"], {"R": item("hbm:deco_rbmk_smooth_panel")}))
    # :900-902
    recs.append(shaped("parts/deco_pipe_quad", "hbm:deco_pipe_quad", 4,
                       ["PP", "PP"], {"P": item("hbm:deco_pipe")}))
    recs.append(shaped("parts/deco_pipe_framed_from_pipe", "hbm:deco_pipe_framed", 8,
                       ["PPP", "PCP", "PPP"],
                       {"P": item("hbm:deco_pipe"), "C": item("minecraft:iron_bars")}))
    recs.append(shaped("parts/deco_pipe_framed_from_rim", "hbm:deco_pipe_framed", 8,
                       ["PPP", "PCP", "PPP"],
                       {"P": item("hbm:deco_pipe_rim"), "C": item("minecraft:iron_bars")}))
    # shapeless :458 / :773 / :895-897 / :1033 / :1040
    recs.append(shapeless("parts/gneiss_chiseled_from_gneiss_tile", "hbm:gneiss_chiseled", 1,
                         ["hbm:gneiss_tile"]))
    recs.append(shapeless("parts/fusion_heater_from_fusion_hatch", "hbm:fusion_heater", 1,
                         ["hbm:fusion_hatch"]))
    recs.append(shapeless("parts/deco_pipe_from_deco_pipe_rim", "hbm:deco_pipe", 1,
                         ["hbm:deco_pipe_rim"]))
    recs.append(shapeless("parts/deco_pipe_from_deco_pipe_framed", "hbm:deco_pipe", 1,
                         ["hbm:deco_pipe_framed"]))
    recs.append(shapeless("parts/deco_pipe_from_deco_pipe_quad", "hbm:deco_pipe", 1,
                         ["hbm:deco_pipe_quad"]))
    recs.append(shapeless("parts/upgrade_5g", "hbm:upgrade_5g", 1,
                         ["hbm:upgrade_template", "hbm:gem_alexandrite"]))
    recs.append(shapeless("parts/cordite", "hbm:cordite", 3,
                         ["hbm:ballistite", "minecraft:gunpowder", "minecraft:white_wool"]))
    # ConsumableRecipes.java:59
    recs.append(shapeless("consumable/loop_stew", "hbm:loop_stew", 1,
                         ["hbm:loops", "hbm:can_smart", "minecraft:bowl"]))
    # SmeltingRecipes.java:142 / :143 / :147
    recs.append(smelting("smelting/sulfur_from_crystal_sulfur", "hbm:crystal_sulfur", "hbm:sulfur", 6))
    recs.append(smelting("smelting/niter_from_crystal_niter", "hbm:crystal_niter", "hbm:niter", 6))
    recs.append(smelting("smelting/fluorite_from_crystal_fluorite", "hbm:crystal_fluorite", "hbm:fluorite", 6))
    return recs


def main():
    wrote = 0
    skipped = 0
    for rec in recipes():
        rel = rec.pop("_path")
        path = OUT / f"{rel}.json"
        if path.exists():
            skipped += 1
            continue
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(rec, indent=2) + "\n")
        wrote += 1
    print(f"wave14 wrote {wrote}, skipped existing {skipped}")


if __name__ == "__main__":
    main()
