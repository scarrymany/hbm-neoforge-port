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
        # PowderRecipes.java:66 F.dust()
        shapeless("powder/powder_flux_fluorite", "hbm:powder_flux", 4,
                  ["hbm:fluorite", "minecraft:sand"]),
        # ConsumableRecipes.java:76
        shapeless("consumable/can_mrsugar", "hbm:can_mrsugar", 1,
                  ["hbm:can_empty", "minecraft:potion", "minecraft:sugar", "hbm:fluorite"]),
        # ConsumableRecipes.java:96
        shaped("consumable/syringe_empty", "hbm:syringe_empty", 6,
               ["P", "C", "B"],
               {
                   "P": item("hbm:plate_iron"),
                   "C": item("hbm:cell"),
                   "B": item("minecraft:iron_bars"),
               }),
        # ConsumableRecipes.java:97
        shaped("consumable/syringe_antidote", "hbm:syringe_antidote", 6,
               ["SSS", "PMP", "SSS"],
               {
                   "S": item("hbm:syringe_empty"),
                   "P": item("minecraft:pumpkin_seeds"),
                   "M": item("minecraft:milk_bucket"),
               }),
        # ConsumableRecipes.java:103
        shaped("consumable/syringe_awesome", "hbm:syringe_awesome", 1,
               ["SPS", "NCN", "SPS"],
               {
                   "C": item("hbm:syringe_empty"),
                   "S": item("hbm:sulfur"),
                   "P": item("hbm:nugget_pu239"),
                   "N": item("hbm:nugget_pu238"),
               }),
        # ConsumableRecipes.java:101
        shaped("consumable/syringe_poison", "hbm:syringe_poison", 1,
               ["SLS", "LCL", "SLS"],
               {
                   "C": item("hbm:syringe_empty"),
                   "S": item("minecraft:spider_eye"),
                   "L": item("hbm:powder_lead"),
               }),
        # ConsumableRecipes.java:105
        shaped("consumable/syringe_metal_empty", "hbm:syringe_metal_empty", 6,
               ["P", "C", "B"],
               {
                   "P": item("hbm:plate_iron"),
                   "C": item("hbm:rod_empty"),
                   "B": item("minecraft:iron_bars"),
               }),
        # ConsumableRecipes.java:106
        shaped("consumable/syringe_metal_stimpak", "hbm:syringe_metal_stimpak", 1,
               [" N ", "NSN", " N "],
               {
                   "N": item("minecraft:nether_wart"),
                   "S": item("hbm:syringe_metal_empty"),
               }),
        # ConsumableRecipes.java:108
        shaped("consumable/syringe_metal_medx", "hbm:syringe_metal_medx", 1,
               [" N ", "NSN", " N "],
               {
                   "N": item("minecraft:quartz"),
                   "S": item("hbm:syringe_metal_empty"),
               }),
        # ConsumableRecipes.java:109
        shaped("consumable/syringe_metal_psycho", "hbm:syringe_metal_psycho", 1,
               [" N ", "NSN", " N "],
               {
                   "N": item("minecraft:glowstone_dust"),
                   "S": item("hbm:syringe_metal_empty"),
               }),
        # ConsumableRecipes.java:110
        shaped("consumable/syringe_metal_super", "hbm:syringe_metal_super", 1,
               [" N ", "PSP", "L L"],
               {
                   "N": item("hbm:bottle_nuka"),
                   "P": item("hbm:plate_steel"),
                   "S": item("hbm:syringe_metal_stimpak"),
                   "L": item("minecraft:leather"),
               }),
        # ConsumableRecipes.java:117
        shaped("consumable/pill_iodine", "hbm:pill_iodine", 8,
               ["IF"],
               {
                   "I": item("hbm:powder_iodine"),
                   "F": item("hbm:fluorite"),
               }),
        # ConsumableRecipes.java:118
        shaped("consumable/plan_c", "hbm:plan_c", 1,
               ["PFP"],
               {
                   "P": item("hbm:powder_poison"),
                   "F": item("hbm:fluorite"),
               }),
        # ConsumableRecipes.java:119
        shapeless("consumable/radx", "hbm:radx", 1,
                  ["hbm:powder_coal", "hbm:powder_coal", "hbm:fluorite"]),
        # ConsumableRecipes.java:134
        shaped("consumable/med_bag", "hbm:med_bag", 1,
               ["LLL", "SIS", "LLL"],
               {
                   "L": item("minecraft:leather"),
                   "S": item("hbm:syringe_metal_stimpak"),
                   "I": item("hbm:syringe_antidote"),
               }),
        # ConsumableRecipes.java:151
        shapeless("consumable/cladding_paint", "hbm:cladding_paint", 1,
                  ["hbm:nugget_lead", "hbm:nugget_lead", "hbm:nugget_lead", "hbm:nugget_lead",
                   "minecraft:clay_ball", "minecraft:glass_bottle"]),
        # ConsumableRecipes.java:152
        shaped("consumable/cladding_rubber", "hbm:cladding_rubber", 1,
               ["RCR", "CDC", "RCR"],
               {
                   "R": item("hbm:ingot_rubber"),
                   "C": item("hbm:powder_coal"),
                   "D": item("hbm:ducttape"),
               }),
        # ConsumableRecipes.java:153
        shaped("consumable/cladding_lead", "hbm:cladding_lead", 1,
               ["DPD", "PRP", "DPD"],
               {
                   "R": item("hbm:cladding_rubber"),
                   "P": item("hbm:plate_lead"),
                   "D": item("hbm:ducttape"),
               }),
        # ConsumableRecipes.java:154
        shaped("consumable/cladding_desh", "hbm:cladding_desh", 1,
               ["DPD", "PRP", "DPD"],
               {
                   "R": item("hbm:cladding_lead"),
                   "P": item("hbm:plate_desh"),
                   "D": item("hbm:ducttape"),
               }),
        # ConsumableRecipes.java:155
        shaped("consumable/cladding_ghiorsium", "hbm:cladding_ghiorsium", 1,
               ["DPD", "PRP", "DPD"],
               {
                   "R": item("hbm:cladding_desh"),
                   "P": item("hbm:ingot_gh336"),
                   "D": item("hbm:ducttape"),
               }),
        # ConsumableRecipes.java:156
        shaped("consumable/cladding_obsidian", "hbm:cladding_obsidian", 1,
               ["OOO", "PDP", "OOO"],
               {
                   "O": item("minecraft:obsidian"),
                   "P": item("hbm:plate_steel"),
                   "D": item("hbm:ducttape"),
               }),
        # ConsumableRecipes.java:157
        shaped("consumable/cladding_iron", "hbm:cladding_iron", 1,
               ["OOO", "PDP", "OOO"],
               {
                   "O": item("hbm:plate_iron"),
                   "P": item("hbm:plate_polymer"),
                   "D": item("hbm:ducttape"),
               }),
        # ConsumableRecipes.java:73 leftover can_redbomb now that pellet_cluster exists
        shapeless("consumable/can_redbomb", "hbm:can_redbomb", 1,
                  ["hbm:can_empty", "minecraft:potion", "minecraft:sugar", "hbm:pellet_cluster"]),
        # CraftingManager.java:647 satlinker (block not registered this pass — skip)
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
