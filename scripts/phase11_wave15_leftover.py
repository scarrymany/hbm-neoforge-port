#!/usr/bin/env python3
"""CE leftover vanilla crafts + Zirnox depleted → hot waste.

CraftingManager / ConsumableRecipes / MineralRecipes / SmeltingRecipes / RodRecipes.
No invented rows. No fluid-meta crafts (cell/tank/barrel fill). Does not overwrite.
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


def smelting(path, inp, result, count, exp=0.1):
    return {
        "type": "minecraft:smelting",
        "category": "misc",
        "ingredient": {"item": inp},
        "result": {"id": result, "count": count},
        "experience": exp,
        "cookingtime": 200,
        "_path": path,
    }


def item(iid):
    return {"item": iid}


def recipes():
    recs = []
    # ConsumableRecipes.java:58
    recs.append(shapeless("consumable/loops", "hbm:loops", 1,
                          ["hbm:flame_pony", "minecraft:wheat", "minecraft:sugar"]))
    # MineralRecipes.java:273 / :392
    recs.append(shaped("mineral/block_red_phosphorus", "hbm:block_red_phosphorus", 1,
                       ["###", "###", "###"], {"#": item("hbm:powder_fire")}))
    recs.append(shaped("mineral/bottle_mercury", "hbm:bottle_mercury", 1,
                       ["###", "#B#", "###"],
                       {"#": item("hbm:ingot_mercury"), "B": item("minecraft:glass_bottle")}))
    # CraftingManager.java:939-940 — port has a single book_guide_book (CE meta split)
    recs.append(shapeless("parts/book_guide_from_potato", "hbm:book_guide_book", 1,
                          ["minecraft:book", "minecraft:potato"]))
    recs.append(shapeless("parts/book_guide_from_iron", "hbm:book_guide_book", 1,
                          ["minecraft:book", "minecraft:iron_ingot"]))
    # CraftingManager.java:1050-1051
    recs.append(shapeless("parts/ore_uranium_from_sellafield_scorched", "hbm:ore_uranium", 1,
                          ["hbm:ore_sellafield_uranium_scorched", "minecraft:water_bucket"]))
    recs.append(shaped("parts/ore_uranium_from_sellafield_scorched_8", "hbm:ore_uranium", 8,
                       ["OOO", "OBO", "OOO"],
                       {"O": item("hbm:ore_sellafield_uranium_scorched"),
                        "B": item("minecraft:water_bucket")}))
    # SmeltingRecipes.java:122-123 / :126-127 / :158
    recs.append(smelting("smelting/glass_uranium_from_sand_uranium",
                         "hbm:sand_uranium", "hbm:glass_uranium", 1, 0.25))
    recs.append(smelting("smelting/glass_polonium_from_sand_polonium",
                         "hbm:sand_polonium", "hbm:glass_polonium", 1, 0.75))
    recs.append(smelting("smelting/glass_boron_from_sand_boron",
                         "hbm:sand_boron", "hbm:glass_boron", 1, 0.25))
    recs.append(smelting("smelting/glass_lead_from_sand_lead",
                         "hbm:sand_lead", "hbm:glass_lead", 1, 0.25))
    recs.append(smelting("smelting/cinnabar_from_crystal_cinnabar",
                         "hbm:crystal_cinnabar", "hbm:cinnabar", 4, 2.0))
    # RodRecipes.java:36-44 — CE waste_* meta 1 = port waste_*_hot
    rods = [
        ("natural_uranium_fuel", "waste_natural_uranium_hot"),
        ("uranium_fuel", "waste_uranium_hot"),
        ("thorium_fuel", "waste_thorium_hot"),
        ("mox_fuel", "waste_mox_hot"),
        ("plutonium_fuel", "waste_plutonium_hot"),
        ("u233_fuel", "waste_u233_hot"),
        ("u235_fuel", "waste_u235_hot"),
        ("les_fuel", "waste_schrabidium_hot"),
        ("zfb_mox_fuel", "waste_zfb_mox_hot"),
    ]
    for rod, waste in rods:
        recs.append(shapeless(f"rods/{waste}_from_zirnox", f"hbm:{waste}", 2,
                              [f"hbm:rod_zirnox_depleted_{rod}"]))
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
    print(f"wave15 wrote {wrote}, skipped existing {skipped}")


if __name__ == "__main__":
    main()
