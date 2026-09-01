#!/usr/bin/env python3
"""CE CraftingManager addSlabStair / addSlabStairColConcrete / addStairColorExt + leftover crafts.

Cites:
  CraftingManager.java:97-121 (addSlabStair → 5 recipes each)
  CraftingManager.java:111 / :1222-1234 (addSlabStairColConcrete ×16)
  CraftingManager.java:112 / :1237-1242 (addStairColorExt ×8)
  ExclusiveRecipes.java:22-23, ArmorRecipes.java:139-142, ToolRecipes.java leftovers,
  MineralRecipes / WeaponRecipes / SmeltingRecipes / CraftingManager leftovers.
Does not overwrite existing JSON.
"""
from __future__ import annotations

import json
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
OUT = REPO / "src" / "main" / "resources" / "data" / "hbm" / "recipe" / "ce_craft"


def shaped(path: str, result: str, count: int, pattern: list[str], key: dict) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
        "_path": path,
    }


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


def tag(tid: str) -> dict:
    return {"tag": tid}


def nid(path: str) -> str:
    return path if path.startswith("minecraft:") else f"hbm:{path}"


def add_slab_stair(recs: list, slug: str, block: str, slab: str, stair: str, cite: str) -> None:
    """CraftingManager.java:1214-1219 helper expansion."""
    recs.append(shaped(f"building/{slug}_slab_from_block", nid(slab), 6,
                       ["###"], {"#": item(nid(block))}))
    recs.append(shaped(f"building/{slug}_stairs_from_block", nid(stair), 8,
                       ["#  ", "## ", "###"], {"#": item(nid(block))}))
    recs.append(shapeless(f"building/{slug}_block_from_stairs", nid(block), 3,
                          [nid(stair)] * 4))
    recs.append(shaped(f"building/{slug}_stairs_from_slab", nid(stair), 4,
                       ["#  ", "## ", "###"], {"#": item(nid(slab))}))
    recs.append(shapeless(f"building/{slug}_block_from_slab", nid(block), 1,
                          [nid(slab), nid(slab)]))


def recipes() -> list[dict]:
    recs: list[dict] = []

    # CraftingManager.java:97-121 — addSlabStair (skip pink_planks :135)
    families = [
        ("reinforced_brick", "reinforced_brick", "reinforced_brick_slab", "reinforced_brick_stairs", "97"),
        ("reinforced_sand", "reinforced_sand", "reinforced_sand_slab", "reinforced_sand_stairs", "98"),
        ("reinforced_stone", "reinforced_stone", "reinforced_stone_slab", "reinforced_stone_stairs", "99"),
        ("brick_concrete", "brick_concrete", "brick_concrete_slab", "brick_concrete_stairs", "100"),
        ("brick_concrete_mossy", "brick_concrete_mossy", "brick_concrete_mossy_slab", "brick_concrete_mossy_stairs", "101"),
        ("brick_concrete_cracked", "brick_concrete_cracked", "brick_concrete_cracked_slab", "brick_concrete_cracked_stairs", "102"),
        ("brick_concrete_broken", "brick_concrete_broken", "brick_concrete_broken_slab", "brick_concrete_broken_stairs", "103"),
        ("brick_compound", "brick_compound", "brick_compound_slab", "brick_compound_stairs", "104"),
        ("brick_asbestos", "brick_asbestos", "brick_asbestos_slab", "brick_asbestos_stairs", "105"),
        ("brick_light", "brick_light", "brick_light_slab", "brick_light_stairs", "106"),
        ("brick_obsidian", "brick_obsidian", "brick_obsidian_slab", "brick_obsidian_stairs", "107"),
        ("cmb_brick_reinforced", "cmb_brick_reinforced", "cmb_brick_reinforced_slab", "cmb_brick_reinforced_stairs", "108"),
        ("concrete", "concrete", "concrete_slab", "concrete_stairs", "109"),
        ("concrete_smooth", "concrete_smooth", "concrete_smooth_slab", "concrete_smooth_stairs", "110"),
        ("concrete_asbestos", "concrete_asbestos", "concrete_asbestos_slab", "concrete_asbestos_stairs", "113"),
        ("ducrete_smooth", "ducrete_smooth", "ducrete_smooth_slab", "ducrete_smooth_stairs", "114"),
        ("ducrete", "ducrete", "ducrete_slab", "ducrete_stairs", "115"),
        ("ducrete_brick", "ducrete_brick", "ducrete_brick_slab", "ducrete_brick_stairs", "116"),
        ("ducrete_reinforced", "ducrete_reinforced", "ducrete_reinforced_slab", "ducrete_reinforced_stairs", "117"),
        ("tile_lab", "tile_lab", "tile_lab_slab", "tile_lab_stairs", "118"),
        ("tile_lab_cracked", "tile_lab_cracked", "tile_lab_cracked_slab", "tile_lab_cracked_stairs", "119"),
        ("tile_lab_broken", "tile_lab_broken", "tile_lab_broken_slab", "tile_lab_broken_stairs", "120"),
        ("brick_fire", "brick_fire", "brick_fire_slab", "brick_fire_stairs", "121"),
    ]
    for slug, block, slab, stair, line in families:
        add_slab_stair(recs, slug, block, slab, stair, line)

    # CraftingManager.java:111 — addSlabStairColConcrete. DyeColor LIGHT_GRAY → concrete_light_gray,
    # stairs/slabs keep CE silver path (Phase8Blocks.java:163-173).
    colors = [
        ("white", "concrete_white"),
        ("orange", "concrete_orange"),
        ("magenta", "concrete_magenta"),
        ("light_blue", "concrete_light_blue"),
        ("yellow", "concrete_yellow"),
        ("lime", "concrete_lime"),
        ("pink", "concrete_pink"),
        ("gray", "concrete_gray"),
        ("silver", "concrete_light_gray"),
        ("cyan", "concrete_cyan"),
        ("purple", "concrete_purple"),
        ("blue", "concrete_blue"),
        ("brown", "concrete_brown"),
        ("green", "concrete_green"),
        ("red", "concrete_red"),
        ("black", "concrete_black"),
    ]
    for color, block in colors:
        add_slab_stair(
            recs, f"concrete_{color}",
            block, f"concrete_{color}_slab", f"concrete_colored_stairs_{color}",
            "111",
        )

    # CraftingManager.java:112 / :1237-1242 — addStairColorExt (stair×8 + block×3 only)
    for ext in ("machine", "machine_stripe", "indigo", "purple", "pink", "hazard", "sand", "bronze"):
        block = f"concrete_ext_{ext}"
        stair = f"concrete_colored_ext_stairs_{ext}"
        recs.append(shaped(f"building/concrete_ext_{ext}_stairs_from_block", nid(stair), 8,
                           ["#  ", "## ", "###"], {"#": item(nid(block))}))
        recs.append(shapeless(f"building/concrete_ext_{ext}_block_from_stairs", nid(block), 3,
                              [nid(stair)] * 4))

    # ExclusiveRecipes.java:22-23
    recs.append(shaped("exclusive/hazmat", "hbm:hazmat", 8,
                       ["###", "# #", "###"], {"#": item("hbm:hazmat_cloth")}))
    recs.append(shaped("exclusive/hazmat_cloth_from_hazmat", "hbm:hazmat_cloth", 1,
                       ["#"], {"#": item("hbm:hazmat")}))

    # ArmorRecipes.java:139-142 asbestos
    recs.append(shaped("armor/asbestos_helmet", "hbm:asbestos_helmet", 1,
                       ["EEE", "EIE"],
                       {"E": item("hbm:asbestos_cloth"), "I": item("hbm:plate_gold")}))
    recs.append(shaped("armor/asbestos_plate", "hbm:asbestos_plate", 1,
                       ["E E", "EEE", "EEE"],
                       {"E": item("hbm:asbestos_cloth")}))
    recs.append(shaped("armor/asbestos_legs", "hbm:asbestos_legs", 1,
                       ["EEE", "E E", "E E"],
                       {"E": item("hbm:asbestos_cloth")}))
    recs.append(shaped("armor/asbestos_boots", "hbm:asbestos_boots", 1,
                       ["E E", "E E"],
                       {"E": item("hbm:asbestos_cloth")}))

    # ToolRecipes.java leftovers
    recs.append(shaped("tool/matchstick", "hbm:matchstick", 16,
                       ["I", "S"],
                       {"I": item("hbm:sulfur"), "S": item("minecraft:stick")}))
    recs.append(shapeless("tool/geiger_from_counter", "hbm:geiger_counter", 1,
                          ["hbm:geiger_counter"]))  # skip if same id — geiger block?
    recs.append(shaped("tool/reacher", "hbm:reacher", 1,
                       ["BIB", "P P", "B B"],
                       {"B": item("hbm:tungsten_bolt"), "I": item("hbm:ingot_tungsten"),
                        "P": item("hbm:ingot_rubber")}))
    recs.append(shaped("tool/pipette", "hbm:pipette", 1,
                       ["  R", " G ", "G  "],
                       {"R": item("hbm:ingot_rubber"), "G": item("minecraft:glass")}))
    recs.append(shaped("tool/pipette_boron", "hbm:pipette_boron", 1,
                       ["  R", " G ", "G  "],
                       {"R": item("hbm:ingot_rubber"), "G": item("hbm:glass_boron")}))
    recs.append(shaped("tool/boat_rubber", "hbm:boat_rubber", 1,
                       ["L L", "LLL"],
                       {"L": item("hbm:ingot_rubber")}))
    recs.append(shaped("tool/analysis_tool", "hbm:analysis_tool", 1,
                       ["G", "I"],
                       {"G": tag("c:glass_panes"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("tool/screwdriver_desh", "hbm:screwdriver_desh", 1,
                       ["P", "D"],
                       {"P": item("hbm:ingot_polymer"), "D": item("hbm:ingot_desh")}))
    recs.append(shaped("tool/hand_drill_desh", "hbm:hand_drill_desh", 1,
                       ["D", "P"],
                       {"D": item("hbm:ingot_desh"), "P": item("hbm:ingot_polymer")}))
    recs.append(shaped("tool/chemistry_set", "hbm:chemistry_set", 1,
                       ["GIG", "GCG", "III"],
                       {"G": item("minecraft:glass"), "I": item("minecraft:iron_ingot"),
                        "C": item("hbm:ingot_copper")}))

    # MineralRecipes leftovers
    recs.append(shapeless("mineral/ingot_cdalloy_from_block", "hbm:ingot_cdalloy", 9,
                          ["hbm:block_cdalloy"]))
    recs.append(shaped("mineral/block_cdalloy", "hbm:block_cdalloy", 1,
                       ["###", "###", "###"], {"#": item("hbm:ingot_cdalloy")}))
    recs.append(shapeless("mineral/fallout_from_block", "hbm:fallout", 9,
                          ["hbm:block_fallout"]))
    recs.append(shaped("mineral/block_fallout", "hbm:block_fallout", 1,
                       ["###", "###", "###"], {"#": item("hbm:fallout")}))
    recs.append(shaped("mineral/fallout_layer", "hbm:fallout", 2,
                       ["##"], {"#": item("hbm:fallout")}))
    for fuel in ("uranium", "mox", "plutonium", "thorium", "schrabidium"):
        recs.append(shaped(f"mineral/block_{fuel}_fuel", f"hbm:block_{fuel}_fuel", 1,
                           ["###", "###", "###"], {"#": item(f"hbm:ingot_{fuel}_fuel")}))
    recs.append(shaped("mineral/block_white_phosphorus", "hbm:block_white_phosphorus", 1,
                       ["###", "###", "###"], {"#": item("hbm:ingot_phosphorus")}))
    recs.append(shapeless("mineral/ingot_phosphorus_from_block", "hbm:ingot_phosphorus", 9,
                          ["hbm:block_white_phosphorus"]))
    recs.append(shapeless("mineral/nuclear_waste_tiny_from_waste", "hbm:nuclear_waste_tiny", 9,
                          ["hbm:nuclear_waste"]))

    # WeaponRecipes leftovers
    recs.append(shapeless("weapon/stick_dynamite_fishing", "hbm:stick_dynamite_fishing", 1,
                          ["hbm:stick_dynamite", "hbm:stick_dynamite", "hbm:stick_dynamite",
                           "minecraft:paper", {"tag": "hbm:any_tar"}]))
    recs.append(shapeless("weapon/mine_shrap", "hbm:mine_shrap", 1,
                          ["hbm:mine_ap", "hbm:pellet_buckshot"]))

    # SmeltingRecipes leftovers
    recs.append(smelt("smelting/ore_gneiss_lithium", "hbm:lithium", "hbm:ore_gneiss_lithium"))
    recs.append(smelt("smelting/gravel_obsidian", "minecraft:obsidian", "hbm:gravel_obsidian"))
    recs.append(smelt("smelting/ash_digamma", "hbm:glass_ash", "hbm:ash_digamma"))
    recs.append(smelt("smelting/basalt_smooth", "hbm:basalt_smooth", "hbm:basalt"))

    # CraftingManager leftover (not assembler)
    recs.append(shaped("gear/redstone_sword", "hbm:redstone_sword", 1,
                       ["R", "R", "S"],
                       {"R": item("minecraft:redstone_block"), "S": item("minecraft:stick")}))
    recs.append(shaped("gear/big_sword", "hbm:big_sword", 1,
                       ["QIQ", "QIQ", "GSG"],
                       {"Q": item("minecraft:quartz_block"), "I": item("minecraft:iron_ingot"),
                        "G": item("minecraft:gold_ingot"), "S": item("minecraft:stick")}))
    recs.append(shapeless("building/asphalt_light", "hbm:asphalt_light", 1,
                          ["hbm:asphalt", "minecraft:glowstone_dust"]))
    recs.append(shapeless("building/asphalt_from_light", "hbm:asphalt", 1,
                          ["hbm:asphalt_light"]))
    recs.append(shaped("building/depth_brick", "hbm:depth_brick", 4,
                       ["##", "##"], {"#": item("hbm:stone_depth")}))
    recs.append(shaped("building/depth_tiles", "hbm:depth_tiles", 4,
                       ["##", "##"], {"#": item("hbm:depth_brick")}))
    recs.append(shaped("building/basalt_polished", "hbm:basalt_polished", 4,
                       ["##", "##"], {"#": item("hbm:basalt_smooth")}))
    recs.append(shaped("building/basalt_brick", "hbm:basalt_brick", 4,
                       ["##", "##"], {"#": item("hbm:basalt_polished")}))
    recs.append(shaped("building/basalt_tiles", "hbm:basalt_tiles", 4,
                       ["##", "##"], {"#": item("hbm:basalt_brick")}))
    recs.append(shaped("weapon/det_cord", "hbm:det_cord", 4,
                       [" P ", "PGP", " P "],
                       {"P": item("minecraft:paper"), "G": item("minecraft:gunpowder")}))
    recs.append(shapeless("weapon/charge_dynamite", "hbm:charge_dynamite", 1,
                          ["hbm:stick_dynamite", "hbm:stick_dynamite", "hbm:stick_dynamite",
                           "hbm:ducttape"]))
    recs.append(shaped("weapon/charge_miner", "hbm:charge_miner", 1,
                       [" F ", "FCF", " F "],
                       {"F": item("minecraft:flint"), "C": item("hbm:charge_dynamite")}))
    recs.append(shaped("building/spikes", "hbm:spikes", 4,
                       ["B", "I"],
                       {"B": item("hbm:steel_bolt"), "I": item("hbm:ingot_steel")}))
    recs.append(shaped("building/fence_metal", "hbm:fence_metal", 6,
                       ["BIB", "BIB"],
                       {"B": item("minecraft:iron_bars"), "I": item("minecraft:iron_ingot")}))
    recs.append(shaped("rbmk/rbmk_absorber", "hbm:rbmk_absorber", 1,
                       ["BBB", "BLB", "BBB"],
                       {"B": item("hbm:ingot_boron"), "L": item("hbm:rbmk_blank")}))
    recs.append(shapeless("consumable/rag", "hbm:rag", 4,
                          ["minecraft:string", "minecraft:white_wool"]))
    recs.append(shapeless("building/ingot_firebrick_from_brick", "hbm:ingot_firebrick", 4,
                          ["hbm:brick_fire"]))
    recs.append(shaped("building/brick_fire_from_ingot", "hbm:brick_fire", 1,
                       ["##", "##"], {"#": item("hbm:ingot_firebrick")}))

    return recs


def _known_ids() -> set[str]:
    import sys
    sys.path.insert(0, str(REPO / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, blocks = extract_all_ids()
    known = items | blocks
    # loop-concatenated ids extract_all_ids cannot see (Phase8Blocks / GenericBlocks)
    colors = [
        "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "silver", "cyan", "purple", "blue", "brown", "green", "red", "black",
    ]
    for c in colors:
        known.add(f"concrete_{c}")
        known.add(f"concrete_{c}_slab")
        known.add(f"concrete_colored_stairs_{c}")
    known.add("concrete_light_gray")  # DyeColor.LIGHT_GRAY; stairs/slabs stay silver
    for ext in ("machine", "machine_stripe", "indigo", "purple", "pink", "hazard", "sand", "bronze"):
        known.add(f"concrete_ext_{ext}")
        known.add(f"concrete_colored_ext_stairs_{ext}")
    known.update({"nuclear_waste_tiny", "nuclear_waste_vitrified", "plate_gold", "ore_gneiss_lithium"})
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
    # geiger_from_counter is a no-op identity — drop it
    for rec in recipes():
        rel = rec.pop("_path")
        if rel.endswith("geiger_from_counter"):
            continue
        missing = []
        for ref in _refs(rec):
            if ref.startswith("minecraft:") or ref.startswith("c:") or ref.startswith("neoforge:"):
                continue
            if ref.startswith("tag:"):
                continue
            slug = ref.split(":", 1)[-1] if ref.startswith("hbm:") else ref
            if slug not in known:
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
    print(f"ce_craft wave5 written={written} skipped_exists={skipped} dropped_missing={dropped}")


if __name__ == "__main__":
    main()
