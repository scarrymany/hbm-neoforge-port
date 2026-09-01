#!/usr/bin/env python3
"""CE RodRecipes.java leftover: empty rods, pile rods, RBMK/PWR fuels (item-only, no DictFrame)."""
from __future__ import annotations

import json
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "src" / "main" / "resources" / "data" / "hbm" / "recipe" / "ce_craft"
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
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": i} for i in ings],
        "result": {"id": result, "count": count},
        "_path": path,
    }


def recipes():
    recs = []
    # RodRecipes.java:47-52
    recs.append(shaped("rods/rod_empty", "hbm:rod_empty", 16,
                       ["SSS", "L L", "SSS"],
                       {"S": {"item": "hbm:plate_steel"}, "L": {"item": "hbm:plate_lead"}}))
    recs.append(shapeless("rods/rod_empty_from_dual", "hbm:rod_empty", 2, ["hbm:rod_dual_empty"]))
    recs.append(shapeless("rods/rod_dual_empty", "hbm:rod_dual_empty", 1,
                          ["hbm:rod_empty", "hbm:rod_empty"]))
    recs.append(shapeless("rods/rod_empty_from_quad", "hbm:rod_empty", 4, ["hbm:rod_quad_empty"]))
    recs.append(shapeless("rods/rod_quad_empty", "hbm:rod_quad_empty", 1,
                          ["hbm:rod_empty", "hbm:rod_empty", "hbm:rod_empty", "hbm:rod_empty"]))
    recs.append(shapeless("rods/rod_quad_empty_from_dual", "hbm:rod_quad_empty", 1,
                          ["hbm:rod_dual_empty", "hbm:rod_dual_empty"]))

    # :88-92 pile
    recs.append(shaped("rods/pile_rod_uranium", "hbm:pile_rod_uranium", 1,
                       [" U ", "PUP", " U "],
                       {"P": {"item": "hbm:plate_iron"}, "U": {"item": "hbm:billet_uranium"}}))
    recs.append(shaped("rods/pile_rod_source", "hbm:pile_rod_source", 1,
                       [" U ", "PUP", " U "],
                       {"P": {"item": "hbm:plate_iron"}, "U": {"item": "hbm:billet_ra226be"}}))
    recs.append(shaped("rods/pile_rod_boron", "hbm:pile_rod_boron", 1,
                       [" B ", " W ", " B "],
                       {"B": {"item": "hbm:ingot_boron"}, "W": {"tag": "minecraft:planks"}}))
    recs.append(shapeless("rods/pile_rod_lithium", "hbm:pile_rod_lithium", 1,
                          ["hbm:cell", "hbm:lithium"]))
    recs.append(shaped("rods/pile_rod_detector", "hbm:pile_rod_detector", 1,
                       [" B ", "CM ", " B "],
                       {"B": {"item": "hbm:ingot_boron"}, "C": {"item": "hbm:circuit_vacuum_tube"},
                        "M": {"item": "hbm:motor"}}))

    recs.append(shaped("rods/rbmk_fuel_empty", "hbm:rbmk_fuel_empty", 1,
                       ["ZRZ", "Z Z", "ZRZ"],
                       {"Z": {"item": "hbm:ingot_zirconium"}, "R": {"item": "hbm:rod_quad_empty"}}))

    # addRBMKRod(ModItems.billet, ModItems.rbmk_fuel) — 8 billets + empty
    text = (REPO / "upstream/hbm-ce/src/main/java/com/hbm/crafting/RodRecipes.java").read_text()
    for m in re.finditer(
            r"addRBMKRod\(\s*ModItems\.([a-z0-9_]+)\s*,\s*ModItems\.([a-z0-9_]+)\s*\)", text):
        billet, fuel = m.group(1), m.group(2)
        recs.append(shapeless(f"rods/{fuel}", f"hbm:{fuel}", 1,
                              ["hbm:rbmk_fuel_empty"] + [f"hbm:{billet}"] * 8))

    # PWR fuels with explicit ModItems.billet_*
    pwr = {
        "MEU": "billet_uranium_fuel",
        "MEN": "billet_neptunium_fuel",
        "MOX": "billet_mox_fuel",
        "MEP": "billet_pu_mix",
        "MEA": "billet_am_mix",
    }
    for enum, billet in pwr.items():
        slug = "pwr_fuel_" + enum.lower()
        recs.append(shaped(f"rods/{slug}", f"hbm:{slug}", 1,
                           ["F", "I", "F"],
                           {"F": {"item": f"hbm:{billet}"}, "I": {"item": "hbm:plate_polymer"}}))

    # MineralRecipes RTG pellets (item-only)
    for fuel, billet in [
        ("pellet_rtg", "billet_pu238"),
        ("pellet_rtg_radium", "billet_ra226"),
        ("pellet_rtg_strontium", "billet_sr90"),
        ("pellet_rtg_cobalt", "billet_co60"),
        ("pellet_rtg_actinium", "billet_actinium"),
        ("pellet_rtg_polonium", "billet_polonium"),
        ("pellet_rtg_lead", "billet_pb209"),
        ("pellet_rtg_gold", "billet_au198"),
        ("pellet_rtg_americium", "billet_am241"),
    ]:
        recs.append(shapeless(f"mineral/{fuel}", f"hbm:{fuel}", 1,
                              [f"hbm:{billet}", f"hbm:{billet}", f"hbm:{billet}", "hbm:plate_iron"]))
    recs.append(shapeless("mineral/pellet_rtg_weak", "hbm:pellet_rtg_weak", 1,
                          ["hbm:billet_u238", "hbm:billet_u238", "hbm:billet_pu238", "hbm:plate_iron"]))

    recs.append(shapeless("rods/rbmk_fuel_drx", "hbm:rbmk_fuel_drx", 1,
                          ["hbm:rbmk_fuel_balefire", "hbm:particle_digamma"]))
    return recs


def _known_ids():
    import sys
    sys.path.insert(0, str(REPO / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, blocks = extract_all_ids()
    known = items | blocks
    java = REPO / "src" / "main" / "java"
    pats = [
        r'register(?:Block|Item)?\(\s*"([a-z0-9_]+)"',
        r'reg\(\s*"([a-z0-9_]+)"',
        r'registerParts\(\s*"([a-z0-9_]+)"',
        r'control\(\s*"([a-z0-9_]+)"',
        r'parts(?:1)?\(\s*"([a-z0-9_]+)"',
        r'rod\(\s*"([a-z0-9_]+)"',
    ]
    for p in java.rglob("*.java"):
        t = p.read_text(errors="ignore")
        for pat in pats:
            known.update(re.findall(pat, t))
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
            if ref.startswith("minecraft:") or ref.startswith("c:") or ref.startswith("tag:"):
                continue
            if isinstance(rec.get("key"), dict):
                pass
            s = ref.split(":", 1)[-1] if ref.startswith("hbm:") else ref
            if s not in known:
                missing.append(ref)
        # tags in key
        for v in (rec.get("key") or {}).values():
            if isinstance(v, dict) and "tag" in v:
                continue
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
    print(f"rods written={written} skipped_exists={skipped} dropped={dropped}")


if __name__ == "__main__":
    main()
