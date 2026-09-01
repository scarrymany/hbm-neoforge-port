#!/usr/bin/env python3
"""Table-driven leftover CE vanilla crafts (PowderRecipes / ConsumableRecipes / CraftingManager).

CE cites:
  PowderRecipes.java:25 / :29-30 / :40-41 / :72
  ConsumableRecipes.java:73 / :77 / :130
  CraftingManager.java:646 / :650 / :660-661 / :691-692
"""
from __future__ import annotations

import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "src" / "main" / "resources" / "data" / "hbm" / "recipe" / "ce_craft"


def shapeless(path: str, result: str, count: int, ings: list[str | dict]) -> dict:
    ingredients = []
    for i in ings:
        if isinstance(i, dict):
            ingredients.append(i)
        elif i.startswith("tag:"):
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


def shaped(path: str, result: str, count: int, pattern: list[str], key: dict) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
        "_path": path,
    }


def item(iid: str) -> dict:
    return {"item": iid}


def tag(tid: str) -> dict:
    return {"tag": tid}


def recipes() -> list[dict]:
    return [
        # PowderRecipes.java:25
        shapeless("powder/ballistite", "hbm:ballistite", 3,
                  ["minecraft:gunpowder", "hbm:niter", "minecraft:sugar"]),
        # PowderRecipes.java:29
        shapeless("powder/powder_semtex_mix", "hbm:powder_semtex_mix", 3,
                  ["hbm:solid_fuel", "hbm:cordite", "hbm:niter"]),
        # PowderRecipes.java:30
        shapeless("powder/powder_semtex_mix_ballistite", "hbm:powder_semtex_mix", 1,
                  ["hbm:solid_fuel", "hbm:ballistite", "hbm:niter"]),
        # PowderRecipes.java:40
        shapeless("powder/gunpowder_from_niter", "minecraft:gunpowder", 3,
                  ["hbm:sulfur", "hbm:niter", "minecraft:coal"]),
        # PowderRecipes.java:41
        shapeless("powder/gunpowder_from_niter_charcoal", "minecraft:gunpowder", 3,
                  ["hbm:sulfur", "hbm:niter", "minecraft:charcoal"]),
        # PowderRecipes.java:72
        shapeless("powder/powder_fertilizer", "hbm:powder_fertilizer", 4,
                  ["hbm:powder_calcium", "hbm:powder_fire", "hbm:niter", "hbm:sulfur"]),
        # ConsumableRecipes.java:73
        shapeless("consumable/can_smart", "hbm:can_smart", 1,
                  ["hbm:can_empty", "minecraft:potion", "minecraft:sugar", "hbm:niter"]),
        # ConsumableRecipes.java:77
        shapeless("consumable/can_overcharge", "hbm:can_overcharge", 1,
                  ["hbm:can_empty", "minecraft:potion", "minecraft:sugar", "hbm:sulfur"]),
        # ConsumableRecipes.java:130 (non-LBSM)
        shapeless("consumable/xanax", "hbm:xanax", 1,
                  ["hbm:powder_coal", "hbm:niter", "hbm:powder_bromine"]),
        # CraftingManager.java:646
        shaped("parts/photo_panel", "hbm:photo_panel", 1,
               [" G ", "IPI", " C "],
               {
                   "G": tag("c:glass_panes"),
                   "I": item("hbm:plate_polymer"),
                   "P": item("hbm:powder_quartz"),
                   "C": item("hbm:circuit_pcb"),
               }),
        # CraftingManager.java:650
        shaped("parts/sat_chip", "hbm:sat_chip", 1,
               ["WWW", "CIC", "WWW"],
               {
                   "W": item("hbm:mingrade_wire"),
                   "C": item("hbm:circuit_advanced"),
                   "I": tag("hbm:any_plastic"),
               }),
        # CraftingManager.java:660
        shaped("machine/machine_transformer", "hbm:machine_transformer", 1,
               ["SCS", "MDM", "SCS"],
               {
                   "S": item("minecraft:iron_ingot"),
                   "D": item("hbm:ingot_red_copper"),
                   "M": item("hbm:coil_copper"),
                   "C": item("hbm:circuit_capacitor"),
               }),
        # CraftingManager.java:661
        shaped("machine/machine_transformer_dnt", "hbm:machine_transformer_dnt", 1,
               ["SDS", "MCM", "MCM"],
               {
                   "S": item("hbm:ingot_starmetal"),
                   "D": item("hbm:ingot_desh"),
                   "M": item("hbm:magnetizedtungsten_dense_wire"),
                   "C": item("hbm:circuit_bismoid"),
               }),
        # CraftingManager.java:691-692
        shapeless("machine/sliding_blast_door_legacy", "hbm:sliding_blast_door_legacy", 1,
                  ["hbm:sliding_blast_door"]),
        shapeless("machine/sliding_blast_door_from_legacy", "hbm:sliding_blast_door", 1,
                  ["hbm:sliding_blast_door_legacy"]),
    ]


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    written = skipped = 0
    for rec in recipes():
        rel = rec.pop("_path")
        path = OUT / f"{rel}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            skipped += 1
            continue
        path.write_text(json.dumps(rec, indent=2) + "\n")
        written += 1
    print(f"ce_craft written={written} skipped_exists={skipped}")


if __name__ == "__main__":
    main()
