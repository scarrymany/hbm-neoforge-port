#!/usr/bin/env python3
"""Leftover vanilla crafts from CE CraftingManager / *Recipes.java.

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten results.
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


def smelt(path: str, result: str, ingredient: str, exp: float = 1.0) -> dict:
    return {
        "type": "minecraft:smelting",
        "category": "misc",
        "cookingtime": 200,
        "experience": exp,
        "ingredient": {"item": ingredient},
        "result": {"id": result, "count": 1},
        "_path": path,
    }


def item(iid: str) -> dict:
    return {"item": iid}


def recipes() -> list[dict]:
    recs: list[dict] = []

    # CraftingManager.java:456-510 building / steel deco
    recs.append(shaped("building/reinforced_glass", "hbm:reinforced_glass", 4,
                       ["GPG", "P P", "GPG"],
                       {"G": item("minecraft:glass"), "P": item("hbm:plate_steel")}))
    recs.append(shaped("building/reinforced_light", "hbm:reinforced_lamp_off", 1,
                       [" G ", "GLG", " G "],
                       {"G": item("hbm:reinforced_glass"), "L": item("minecraft:redstone_lamp")}))
    recs.append(shaped("building/steel_beam", "hbm:steel_beam", 8,
                       ["S", "S", "S"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/steel_scaffold", "hbm:steel_scaffold", 8,
                       ["S S", " S ", "S S"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/steel_grate", "hbm:steel_grate", 8,
                       ["SS", "SS"],
                       {"S": item("hbm:steel_beam")}))
    recs.append(shaped("building/steel_roof", "hbm:steel_roof", 4,
                       ["SS", "PP"],
                       {"S": item("hbm:ingot_steel"), "P": item("hbm:plate_steel")}))
    recs.append(shaped("building/sandbags", "hbm:sandbags", 4,
                       ["WW", "SS", "WW"],
                       {"W": item("minecraft:white_wool"), "S": item("minecraft:sand")}))
    recs.append(shaped("building/barbed_wire", "hbm:barbed_wire", 16,
                       ["I I", " I ", "I I"],
                       {"I": item("minecraft:iron_ingot")}))
    recs.append(shaped("building/barbed_wire_fire", "hbm:barbed_wire_fire", 8,
                       ["B B", " B ", "B B"],
                       {"B": item("hbm:barbed_wire")}))
    recs.append(shaped("building/depth_nether_brick", "hbm:depth_nether_brick", 4,
                       ["##", "##"], {"#": item("hbm:stone_depth_nether")}))
    recs.append(shaped("building/depth_nether_tiles", "hbm:depth_nether_tiles", 4,
                       ["##", "##"], {"#": item("hbm:depth_nether_brick")}))

    # CraftingManager.java:191-202 biomass (sawdust as ingredient only)
    recs.append(shapeless("consumable/biomass_sapling", "hbm:biomass", 1,
                          ["tag:c:saplings", "tag:c:saplings", "tag:c:saplings",
                           "tag:c:saplings", "hbm:powder_sawdust"]))
    recs.append(shapeless("consumable/biomass_leaves", "hbm:biomass", 1,
                          ["tag:c:leaves", "tag:c:leaves", "tag:c:leaves",
                           "tag:c:leaves", "tag:c:leaves", "tag:c:leaves",
                           "tag:c:leaves", "tag:c:leaves", "hbm:powder_sawdust"]))
    recs.append(shapeless("consumable/biomass_sugar", "hbm:biomass", 2,
                          ["minecraft:sugar_cane", "minecraft:sugar_cane", "minecraft:sugar_cane",
                           "minecraft:sugar_cane", "hbm:powder_sawdust"]))

    # CraftingManager.java:182-189 wire 3x3 → ingot
    for mat, wire, ingot in (
        ("copper", "copper_wire", "ingot_copper"),
        ("gold", "gold_wire", "minecraft:gold_ingot"),
        ("aluminium", "aluminum_wire", "ingot_aluminium"),
        ("tungsten", "tungsten_wire", "ingot_tungsten"),
        ("steel", "steel_wire", "ingot_steel"),
        ("mingrade", "mingrade_wire", "ingot_red_copper"),
        ("magnetized", "magnetizedtungsten_wire", "ingot_magnetized_tungsten"),
        ("schrabidium", "schrabidium_wire", "ingot_schrabidium"),
    ):
        out = ingot if ingot.startswith("minecraft:") else f"hbm:{ingot}"
        recs.append(shaped(f"parts/{mat}_ingot_from_wire", out, 1,
                           ["###", "###", "###"], {"#": item(f"hbm:{wire}")}))

    # CraftingManager.java:224-234 missile / turbine parts
    recs.append(shaped("weapon/fins_small_steel", "hbm:fins_small_steel", 1,
                       [" P ", "PSP", " P "],
                       {"P": item("hbm:plate_steel"), "S": item("hbm:ingot_steel")}))
    recs.append(shaped("weapon/fins_quad_titanium", "hbm:fins_quad_titanium", 1,
                       [" P ", "P P", " P "],
                       {"P": item("hbm:plate_titanium")}))
    recs.append(shaped("weapon/sphere_steel", "hbm:sphere_steel", 1,
                       [" P ", "P P", " P "],
                       {"P": item("hbm:plate_steel")}))
    # CraftingManager.java:205-212 coils (skip coil_tungsten)
    recs.append(shaped("parts/coil_gold", "hbm:coil_gold", 1,
                       ["WWW", "W W", "WWW"],
                       {"W": item("hbm:gold_wire")}))
    recs.append(shaped("parts/coil_gold_torus", "hbm:coil_gold_torus", 2,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:coil_gold")}))
    recs.append(shaped("parts/coil_copper_torus", "hbm:coil_copper_torus", 2,
                       [" C ", "C C", " C "],
                       {"C": item("hbm:coil_copper")}))
    recs.append(shaped("parts/coil_magnetized_tungsten", "hbm:coil_magnetized_tungsten", 1,
                       ["WWW", "W W", "WWW"],
                       {"W": item("hbm:magnetizedtungsten_wire")}))

    # CraftingManager.java:876-890 ladders / deco
    recs.append(shaped("building/ladder_red", "hbm:ladder_red", 3,
                       ["S S", "SSS", "S S"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/ladder_gold", "hbm:ladder_gold", 3,
                       ["S S", "SSS", "S S"],
                       {"S": item("minecraft:gold_ingot")}))
    recs.append(shaped("building/ladder_copper", "hbm:ladder_copper", 3,
                       ["S S", "SSS", "S S"],
                       {"S": item("hbm:ingot_copper")}))
    recs.append(shaped("building/ladder_titanium", "hbm:ladder_titanium", 3,
                       ["S S", "SSS", "S S"],
                       {"S": item("hbm:ingot_titanium")}))
    recs.append(shaped("building/trapdoor_steel", "hbm:trapdoor_steel", 2,
                       ["PPP", "PPP"],
                       {"P": item("hbm:plate_steel")}))
    recs.append(shaped("building/deco_steel", "hbm:deco_steel", 8,
                       ["SSS", "S S", "SSS"],
                       {"S": item("hbm:ingot_steel")}))
    recs.append(shaped("building/deco_titanium", "hbm:deco_titanium", 8,
                       ["SSS", "S S", "SSS"],
                       {"S": item("hbm:ingot_titanium")}))
    recs.append(shaped("building/deco_red_copper", "hbm:deco_red_copper", 8,
                       ["SSS", "S S", "SSS"],
                       {"S": item("hbm:ingot_red_copper")}))

    # SmeltingRecipes.java:117-129 leftovers
    recs.append(smelt("smelting/powder_iron", "minecraft:iron_ingot", "hbm:powder_iron"))
    recs.append(smelt("smelting/powder_gold", "minecraft:gold_ingot", "hbm:powder_gold"))
    recs.append(smelt("smelting/powder_copper", "hbm:ingot_copper", "hbm:powder_copper"))
    recs.append(smelt("smelting/powder_titanium", "hbm:ingot_titanium", "hbm:powder_titanium"))
    recs.append(smelt("smelting/powder_tungsten", "hbm:ingot_tungsten", "hbm:powder_tungsten"))
    recs.append(smelt("smelting/powder_aluminium", "hbm:ingot_aluminium", "hbm:powder_aluminium"))
    recs.append(smelt("smelting/powder_lead", "hbm:ingot_lead", "hbm:powder_lead"))
    recs.append(smelt("smelting/powder_steel", "hbm:ingot_steel", "hbm:powder_steel"))

    # PowderRecipes.java:36-81 non-fluid blends
    recs.append(shapeless("powder/powder_steel", "hbm:powder_steel", 1,
                          ["hbm:powder_iron", "hbm:powder_coal"]))
    recs.append(shapeless("powder/powder_red_copper", "hbm:powder_red_copper", 2,
                          ["hbm:powder_copper", "minecraft:redstone"]))
    recs.append(shapeless("powder/powder_dura_steel", "hbm:powder_dura_steel", 2,
                          ["hbm:powder_steel", "hbm:powder_tungsten"]))

    # ConsumableRecipes bottles (no fluid)
    recs.append(shapeless("consumable/canteen_13", "hbm:canteen_13", 1,
                          ["hbm:canteen_vodka", "minecraft:redstone"]))
    recs.append(shaped("consumable/cap_nuka", "hbm:cap_nuka", 8,
                       [" I ", "I I"],
                       {"I": item("hbm:plate_aluminium")}))

    return recs


def _known_ids() -> set[str]:
    import sys
    sys.path.insert(0, str(REPO / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, blocks = extract_all_ids()
    known = items | blocks
    known.update({
        "steel_beam", "steel_scaffold", "steel_grate", "steel_roof", "rebar", "sandbags",
        "barbed_wire", "barbed_wire_fire", "reinforced_glass", "reinforced_lamp_off",
        "stone_gneiss", "gneiss_brick", "gneiss_tile", "stone_depth_nether",
        "depth_nether_brick", "depth_nether_tiles", "biomass",
        "copper_wire", "gold_wire", "aluminum_wire", "tungsten_wire", "steel_wire",
        "mingrade_wire", "magnetizedtungsten_wire", "schrabidium_wire",
        "fins_small_steel", "fins_quad_titanium", "sphere_steel", "turbine_titanium",
        "blade_titanium", "coil_gold", "coil_gold_torus", "coil_copper_torus",
        "coil_advanced_torus", "coil_advanced", "coil_magnetized_tungsten",
        "ladder_red", "ladder_gold", "ladder_copper", "ladder_titanium", "trapdoor_steel",
        "deco_steel", "deco_titanium", "deco_red_copper",
        "lightstone", "lightstone_tiles", "lightstone_bricks",
        "powder_iron", "powder_gold", "powder_copper", "powder_titanium", "powder_tungsten",
        "powder_aluminium", "powder_lead", "powder_steel", "powder_red_copper",
        "powder_advanced_alloy", "powder_dura_steel", "powder_desh_mix", "powder_rare",
        "nugget_mercury", "geiger_counter", "circuit_aluminium", "canteen_13",
        "canteen_vodka", "cap_nuka", "plate_aluminium", "plate_steel", "plate_titanium",
        "powder_coal", "powder_sawdust",
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
    print(f"ce_craft wave6 written={written} skipped_exists={skipped} dropped_missing={dropped}")


if __name__ == "__main__":
    main()
