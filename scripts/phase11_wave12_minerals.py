#!/usr/bin/env python3
"""CE MineralRecipes.java leftover: add1To9Pair / addMineralSet / addBillet + explicit 9-pack.

Does not overwrite existing JSON. Skips powder_sawdust / gem_tantalium / coil_tungsten results.
Does not emit oredict-only, SameMeta, or unregistered ids. Does not rewrite assembler JSON.
"""
from __future__ import annotations

import json
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
CE = REPO / "upstream" / "hbm-ce" / "src" / "main" / "java" / "com" / "hbm" / "crafting" / "MineralRecipes.java"
OUT = REPO / "src" / "main" / "resources" / "data" / "hbm" / "recipe" / "ce_craft"

SKIP_RESULTS = {"powder_sawdust", "gem_tantalium", "coil_tungsten"}
FIELD = re.compile(r"(?:ModItems|ModBlocks)\.([a-z0-9_]+)")


def shaped(path: str, result: str, count: int, pattern: list[str], key: dict) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result, "count": count},
        "_path": path,
    }


def shapeless(path: str, result: str, count: int, ings: list[str]) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": i} for i in ings],
        "result": {"id": result, "count": count},
        "_path": path,
    }


def pair_1_9(one: str, nine: str) -> list[dict]:
    """CE add1To9Pair: 1×one → 9×nine (shapeless); 9×nine → 1×one (3×3)."""
    return [
        shapeless(f"mineral/{nine}_from_{one}", f"hbm:{nine}", 9, [f"hbm:{one}"]),
        shaped(f"mineral/{one}_from_{nine}", f"hbm:{one}", 1,
               ["###", "###", "###"], {"#": {"item": f"hbm:{nine}"}}),
    ]


def recipes() -> list[dict]:
    text = CE.read_text(encoding="utf-8", errors="replace")
    recs: list[dict] = []

    for m in re.finditer(
            r"add1To9Pair\(\s*(?:ModItems|ModBlocks)\.([a-z0-9_]+)\s*,\s*ModItems\.([a-z0-9_]+)\s*\)",
            text):
        recs.extend(pair_1_9(m.group(1), m.group(2)))

    for m in re.finditer(
            r"addMineralSet\(\s*ModItems\.([a-z0-9_]+)\s*,\s*ModItems\.([a-z0-9_]+)\s*,\s*ModBlocks\.([a-z0-9_]+)\s*\)",
            text):
        nug, ing, blk = m.group(1), m.group(2), m.group(3)
        recs.extend(pair_1_9(ing, nug))
        recs.extend(pair_1_9(blk, ing))

    # addBillet(billet, ingot, nugget[, ore...])  OR  addBillet(billet, nugget[, ore...])
    for m in re.finditer(r"addBillet\(\s*([^;]+?)\)\s*;", text):
        fields = FIELD.findall(m.group(1))
        if len(fields) >= 3:
            billet, ingot, nugget = fields[0], fields[1], fields[2]
            recs.append(shaped(f"mineral/{billet}_from_{nugget}", f"hbm:{billet}", 1,
                               ["###", "###"], {"#": {"item": f"hbm:{nugget}"}}))
            recs.append(shapeless(f"mineral/{nugget}_from_{billet}", f"hbm:{nugget}", 6,
                                  [f"hbm:{billet}"]))
            recs.append(shapeless(f"mineral/{ingot}_from_{billet}", f"hbm:{ingot}", 2,
                                  [f"hbm:{billet}", f"hbm:{billet}", f"hbm:{billet}"]))
            recs.append(shaped(f"mineral/{billet}_from_{ingot}", f"hbm:{billet}", 3,
                               ["##"], {"#": {"item": f"hbm:{ingot}"}}))
        elif len(fields) == 2:
            billet, nugget = fields[0], fields[1]
            recs.append(shaped(f"mineral/{billet}_from_{nugget}", f"hbm:{billet}", 1,
                               ["###", "###"], {"#": {"item": f"hbm:{nugget}"}}))
            recs.append(shapeless(f"mineral/{nugget}_from_{billet}", f"hbm:{nugget}", 6,
                                  [f"hbm:{billet}"]))

    # Explicit 3×3 compress: result 1 of A from 9× B
    for m in re.finditer(
            r'addRecipeAuto\(\s*new ItemStack\(\s*(?:Item\.getItemFromBlock\()?(?:ModItems|ModBlocks)\.([a-z0-9_]+)\)?\s*,\s*1\s*\)\s*,\s*"###"\s*,\s*"###"\s*,\s*"###"\s*,\s*\'#\'\s*,\s*(?:ModItems|ModBlocks)\.([a-z0-9_]+)',
            text):
        recs.append(shaped(f"mineral/{m.group(1)}_from_{m.group(2)}_9", f"hbm:{m.group(1)}", 1,
                           ["###", "###", "###"], {"#": {"item": f"hbm:{m.group(2)}"}}))

    # Explicit 1→9 decompress: result 9 of A from 1× B  (pattern "#")
    for m in re.finditer(
            r'addRecipeAuto\(\s*new ItemStack\(\s*(?:ModItems|ModBlocks)\.([a-z0-9_]+)\s*,\s*9\s*\)\s*,\s*"#"\s*,\s*\'#\'\s*,\s*(?:Item\.getItemFromBlock\()?(?:ModItems|ModBlocks)\.([a-z0-9_]+)',
            text):
        recs.append(shapeless(f"mineral/{m.group(1)}_from_{m.group(2)}_unpack", f"hbm:{m.group(1)}", 9,
                              [f"hbm:{m.group(2)}"]))

    # 2×2 scrap / nitra
    recs.append(shaped("mineral/block_scrap_from_scrap", "hbm:block_scrap", 1,
                       ["##", "##"], {"#": {"item": "hbm:scrap"}}))
    recs.append(shaped("mineral/block_meteor_cobble", "hbm:block_meteor_cobble", 1,
                       ["##", "##"], {"#": {"item": "hbm:fragment_meteorite"}}))
    recs.append(shaped("mineral/nitra_from_small", "hbm:nitra", 1,
                       ["##", "##"], {"#": {"item": "hbm:nitra_small"}}))
    recs.append(shapeless("mineral/nitra_small_from_nitra", "hbm:nitra_small", 4, ["hbm:nitra"]))

    # fuel mix leftovers that are item-only (no ore strings)
    recs.append(shapeless("mineral/billet_thorium_fuel_mix", "hbm:billet_thorium_fuel", 6,
                          ["hbm:billet_th232"] * 5 + ["hbm:billet_u233"]))
    recs.append(shapeless("mineral/billet_uranium_fuel_mix", "hbm:billet_uranium_fuel", 6,
                          ["hbm:billet_u238"] * 5 + ["hbm:billet_u235"]))
    recs.append(shapeless("mineral/billet_plutonium_fuel_mix", "hbm:billet_plutonium_fuel", 3,
                          ["hbm:billet_u238", "hbm:billet_u238", "hbm:billet_pu_mix"]))
    recs.append(shapeless("mineral/billet_pu_mix_mix", "hbm:billet_pu_mix", 3,
                          ["hbm:billet_pu239", "hbm:billet_pu239", "hbm:billet_pu240"]))
    recs.append(shapeless("mineral/billet_americium_fuel_mix", "hbm:billet_americium_fuel", 3,
                          ["hbm:billet_u238", "hbm:billet_u238", "hbm:billet_am_mix"]))
    recs.append(shapeless("mineral/billet_am_mix_mix", "hbm:billet_am_mix", 3,
                          ["hbm:billet_am241", "hbm:billet_am242", "hbm:billet_am242"]))
    recs.append(shapeless("mineral/billet_neptunium_fuel_mix", "hbm:billet_neptunium_fuel", 3,
                          ["hbm:billet_u238", "hbm:billet_u238", "hbm:billet_neptunium"]))
    recs.append(shapeless("mineral/billet_schrabidium_fuel_mix", "hbm:billet_schrabidium_fuel", 3,
                          ["hbm:billet_schrabidium", "hbm:billet_neptunium", "hbm:billet_beryllium"]))
    recs.append(shapeless("mineral/billet_uranium_unmix", "hbm:billet_uranium", 2,
                          ["hbm:billet_uranium_fuel", "hbm:billet_u238"]))

    return recs


def _known_ids() -> set[str]:
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
        r'registerIngot\(\s*"([a-z0-9_]+)"',
        r'registerNugget\(\s*"([a-z0-9_]+)"',
        r'registerPowder\(\s*"([a-z0-9_]+)"',
        r'registerLoreIngot\(\s*"([a-z0-9_]+)"',
        r'registerLoreNugget\(\s*"([a-z0-9_]+)"',
        r'parts(?:1)?\(\s*"([a-z0-9_]+)"',
    ]
    for p in java.rglob("*.java"):
        t = p.read_text(errors="ignore")
        for pat in pats:
            known.update(re.findall(pat, t))
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
            out.extend(_refs(v) if not (isinstance(v, str) and ":" in v) else [v])
    return out


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    known = _known_ids()
    written = skipped = dropped = 0
    seen_paths: set[str] = set()
    for rec in recipes():
        rel = rec.pop("_path")
        if rel in seen_paths:
            continue
        seen_paths.add(rel)
        slug = rec["result"]["id"].split(":", 1)[-1]
        if slug in SKIP_RESULTS:
            dropped += 1
            continue
        missing = []
        for ref in _refs(rec):
            if ref.startswith("minecraft:") or ref.startswith("c:"):
                continue
            s = ref.split(":", 1)[-1] if ref.startswith("hbm:") else ref
            if s not in known:
                missing.append(ref)
        if missing:
            dropped += 1
            continue
        path = OUT / f"{rel}.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.exists():
            skipped += 1
            continue
        path.write_text(json.dumps(rec, indent=2) + "\n")
        written += 1
    print(f"mineral written={written} skipped_exists={skipped} dropped={dropped} unique={len(seen_paths)}")


if __name__ == "__main__":
    main()
