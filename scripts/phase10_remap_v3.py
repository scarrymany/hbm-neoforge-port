#!/usr/bin/env python3
"""Census registered ids (Java + autogen only) and remap leftovers onto existing CE pngs.

Does not invent art. Does not union lang keys (those are not registry ids).
"""
from __future__ import annotations

import json
import re
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from phase10_alias_and_lang import (  # type: ignore
    ASSETS,
    REPO,
    JAVA,
    extract_java_ids,
    item_tex_exists,
    block_tex_exists,
    resolve_item_tex,
    resolve_block_tex,
    write_item_model,
    write_block_assets,
    item_model_exists,
    blockstate_exists,
    _autogen_item_ids,
    _autogen_block_ids,
)

LEGAL = re.compile(r"^[a-z][a-z0-9_]*$")
HELPERS = re.compile(
    r'(?:ITEMS\.register|BLOCKS\.register|registerIngot|registerNugget|registerLoreIngot|'
    r'registerItem|registerGun|registerAmmo(?:Hidden)?|registerBlockItem|registerBlock|'
    r'registerUpgrade(?:Stack)?|registerPlainBattery|registerFelCrystal|'
    r'registerBillet|registerPowder|registerFuelPowder|registerParts|registerWaste|'
    r'registerRtgPellet|registerResource|'
    r'registerUpgrade|tab\([^,]+,\s*reg|'
    r'\breg|parts1|parts)\(\s*"([a-z][a-z0-9_]*)"'
)
ANY_REG = re.compile(r'\.register\(\s*"([a-z][a-z0-9_]*)"')
BARE_REG = re.compile(r'(?<![A-Za-z])register\(\s*"([a-z][a-z0-9_]*)"')


def extract_all_ids() -> tuple[set[str], set[str]]:
    items, blocks = extract_java_ids()
    # items/ + blocks/ helpers only (not sounds / menus / particles)
    for p in JAVA.rglob("*.java"):
        rel = p.as_posix()
        in_items = "/items/" in rel or p.name == "ModItems.java"
        in_blocks = "/blocks/" in rel or p.name in {"ModBlocks.java", "OreBlocks.java", "MaterialBlockGenerator.java"}
        in_eggs = "/entity/" in rel
        if not (in_items or in_blocks or in_eggs):
            continue
        text = p.read_text(errors="ignore")
        found = set(HELPERS.findall(text)) | set(ANY_REG.findall(text)) | set(BARE_REG.findall(text))
        found |= set(re.findall(r'(?:stair|slab)\(\s*"([a-z][a-z0-9_]*)"', text))
        found = {x for x in found if LEGAL.fullmatch(x) and not x.endswith("_")}
        if in_eggs:
            found = {x for x in found if "spawn_egg" in x or x.startswith("entity_")}
            items |= {x for x in found if "spawn_egg" in x}
            continue
        if in_blocks:
            blocks |= found
            items |= found
        if in_items:
            items |= found
    # deco_pipe_* loop (string array in GenericBlocks.registerPipes — already registered)
    pipes = JAVA / "blocks" / "generic" / "GenericBlocks.java"
    if pipes.exists():
        for name in re.findall(r'"(deco_pipe[a-z0-9_]*)"', pipes.read_text(errors="ignore")):
            blocks.add(name)
            items.add(name)
    # deco_crt_ / deco_toaster_ / deco_computer_ concatenated from BlockEnums
    enums_deco = (JAVA / "blocks" / "BlockEnums.java").read_text(errors="ignore") if (JAVA / "blocks" / "BlockEnums.java").exists() else ""
    for pref, enum_name in (
        ("deco_crt_", "DecoCRTEnum"),
        ("deco_toaster_", "DecoToasterEnum"),
        ("deco_computer_", "DecoComputerEnum"),
    ):
        m = re.search(rf"enum {enum_name} \{{(.*?)\}}", enums_deco, flags=re.S)
        if not m:
            continue
        for n in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", m.group(1)):
            if n in {"VALUES"}:
                continue
            bid = pref + n.lower()
            blocks.add(bid)
            items.add(bid)
    # concrete_* / concrete_ext_* / concrete_super_* / scaffold_* loops (already registered)
    dyes = ("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")
    for d in dyes:
        blocks.add(f"concrete_{d}")
        items.add(f"concrete_{d}")
    for t in ("machine", "machine_stripe", "indigo", "purple", "pink", "hazard", "sand", "bronze"):
        blocks.add(f"concrete_ext_{t}")
        items.add(f"concrete_ext_{t}")
    for i in range(16):
        blocks.add(f"concrete_super_{i}")
        items.add(f"concrete_super_{i}")
    for s in ("scaffold_steel", "scaffold_red", "scaffold_white", "scaffold_yellow"):
        blocks.add(s)
        items.add(s)
    # GenericBlocks metadata loops already registered via registerBlock("prefix_" + enum)
    for pref, enum_name, src in (
        ("platemetal_", "PlatemetalType", enums_deco),
        ("stone_resource_", "EnumStoneType", enums_deco),
        ("lightstone_", "LightstoneType", enums_deco),
        ("block_cap_", "EnumBlockCapType", enums_deco),
        ("block_meteor_ore_", "EnumMeteorType", enums_deco),
    ):
        m = re.search(rf"enum {enum_name} \{{(.*?)\}}", src, flags=re.S)
        if not m:
            continue
        for n in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", m.group(1)):
            if n in {"VALUES"}:
                continue
            bid = pref + n.lower()
            blocks.add(bid)
            items.add(bid)
    for n in re.findall(r"\b([A-Z][A-Z0-9_]+)\b",
                        re.search(r"enum EnumStalagmiteType \{(.*?)\}", enums_deco, flags=re.S).group(1)
                        if re.search(r"enum EnumStalagmiteType \{(.*?)\}", enums_deco, flags=re.S) else ""):
        if n in {"VALUES"}:
            continue
        s = n.lower()
        blocks.add(f"stalagmite_{s}")
        blocks.add(f"stalactite_{s}")
        items.add(f"stalagmite_{s}")
        items.add(f"stalactite_{s}")
    coke_src = (JAVA / "items" / "ItemEnums.java").read_text(errors="ignore") if (JAVA / "items" / "ItemEnums.java").exists() else ""
    m = re.search(r"enum EnumCokeType \{(.*?)\}", coke_src, flags=re.S)
    if m:
        for n in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", m.group(1)):
            if n in {"VALUES"}:
                continue
            bid = "block_coke_" + n.lower()
            blocks.add(bid)
            items.add(bid)
    for name in (
        "ladder_sturdy", "ladder_iron", "ladder_gold", "ladder_aluminium", "ladder_copper",
        "ladder_titanium", "ladder_lead", "ladder_cobalt", "ladder_steel", "ladder_tungsten",
        "ladder_red", "ladder_red_top",
    ):
        blocks.add(name)
        items.add(name)
    # PlantBlocks / glyph loops
    enums = (JAVA / "blocks" / "PlantEnums.java").read_text(errors="ignore")
    for pref, enum_name in (
        ("plant_dead_", "EnumDeadPlantType"),
        ("plant_flower_", "EnumFlowerPlantType"),
        ("plant_tall_", "EnumTallPlantType"),
    ):
        m = re.search(rf"enum {enum_name} \{{(.*?)\}}", enums, flags=re.S)
        if not m:
            continue
        for n in re.findall(r"\b([A-Z][A-Z0-9_]+)\b", m.group(1)):
            if n in {"VALUES"}:
                continue
            bid = pref + n.lower()
            blocks.add(bid)
            items.add(bid)
    for i in range(16):
        blocks.add(f"brick_jungle_glyph_{i}")
        items.add(f"brick_jungle_glyph_{i}")
    for bid in ("glyphid_base", "glyphid_base_infested", "glyphid_base_rad"):
        blocks.add(bid)
        items.add(bid)
    # BedrockOreItems: grade x type (156)
    grades = re.findall(
        r"^\s+([A-Z][A-Z0-9_]+)\(",
        (JAVA / "items" / "special" / "BedrockOreGrade.java").read_text(errors="ignore"),
        flags=re.M,
    )
    types = re.findall(
        r'^\s+[A-Z_]+$$[^"]+"(\w+)"',
        (JAVA / "items" / "special" / "BedrockOreType.java").read_text(errors="ignore"),
        flags=re.M,
    )
    # fallback suffixes from ctor 3rd string arg
    if not types:
        types = ["light", "heavy", "rare", "actinide", "nonmetal", "crystal"]
    for g in grades:
        if g in {"VALUES"}:
            continue
        for t in types:
            items.add(f"bedrock_ore_new_{g.lower()}_{t}")
    items.add("bedrock_ore_base")
    items |= _autogen_item_ids()
    blocks |= _autogen_block_ids()
    items |= blocks
    return items, blocks


def all_png(kind: str) -> set[str]:
    root = ASSETS / "textures" / kind
    out: set[str] = set()
    if not root.is_dir():
        return out
    for p in root.rglob("*.png"):
        rel = p.relative_to(root).as_posix()[:-4]
        out.add(rel)
        out.add(p.stem)
    return out


ITEM_PNG = all_png("item")
BLOCK_PNG = all_png("block")


def extra_item(iid: str) -> str | None:
    if item_tex_exists(iid):
        return iid
    if "spawn_egg" in iid:
        return "__SPAWN_EGG__"
    # anvils share one CE inventory icon
    if iid.startswith("anvil_"):
        for cand in ("anvil_steel", "anvil_iron", "anvil", "machine_anvil"):
            if item_tex_exists(cand):
                return cand
    # himars / stinger / luna ammo families
    if iid.startswith("ammo_himars"):
        for cand in ("ammo_himars", "himars", "ammo_standard.himars", "rocket_generic"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("ammo_stinger"):
        for cand in ("ammo_stinger", "stinger", "rocket_stinger", "ammo_rocket"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("ammo_luna"):
        for cand in ("ammo_luna", "gun_lunatic", "ammo_standard.luna"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("ammo_762"):
        for cand in ("ammo_standard.r762", "r762", "ammo_762"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("ammo_") and item_tex_exists("ammo_standard"):
        rest = iid[len("ammo_") :]
        for cand in (f"ammo_standard.{rest}", f"ammo_secret.{rest}", rest, f"ammo_{rest.split('_')[0]}"):
            if item_tex_exists(cand):
                return cand
    # apples / books / cells / carts
    if iid.startswith("apple_"):
        for cand in ("apple_lead", "apple_schrabidium", "apple_euphemium", iid.rsplit("_", 1)[0]):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("book_"):
        for cand in (iid, "book_secret", "book_guide", "book"):
            if item_tex_exists(cand):
                return cand
    if iid in {"cell", "cell_fluid_id"} or iid.startswith("cell_"):
        for cand in ("cell_empty", "cell_full", "cell"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("canister_"):
        for cand in ("canister_empty", "canister_full", "canister_fuel"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("cart_ntm_") or iid.startswith("cart_"):
        for cand in ("minecart_chest", "cart", "minecart"):
            if item_tex_exists(cand):
                return cand
    # armor pieces → plate_armor / layer
    m = re.fullmatch(r"(.+?)_(helmet|plate|legs|boots|jetpack|chest)", iid)
    if m:
        base = m.group(1)
        for cand in (
            f"plate_armor_{base}",
            f"armor_{base}",
            f"{base}_helmet",
            f"{base}_chest",
            f"{base}_1",
            f"armor_{base}_1",
        ):
            if item_tex_exists(cand):
                return cand
        # CE only ships a few plate_armor_* icons — reuse nearest existing set
        for token, plate in (
            ("ajr", "plate_armor_ajr"),
            ("ajro", "plate_armor_ajr"),
            ("fau", "plate_armor_fau"),
            ("hev", "plate_armor_hev"),
            ("bj", "plate_armor_ajr"),
            ("t45", "plate_armor_hev"),
            ("lunar", "plate_armor_lunar"),
            ("dnt", "plate_armor_dnt"),
        ):
            if token in base.split("_") or base == token:
                if item_tex_exists(plate):
                    return plate
        if item_tex_exists("plate_armor_titanium"):
            return "plate_armor_titanium"
    # trains
    if iid.startswith("train_"):
        for cand in (iid, "train_cargo_tram", "tram"):
            if item_tex_exists(cand):
                return cand
    # mp thruster / fuselage templates
    if iid.startswith("mp_"):
        parts = iid.split("_")
        for n in range(len(parts), 2, -1):
            cand = "_".join(parts[:n])
            if item_tex_exists(cand):
                return cand
        for cand in ("mp_thruster_10", "mp_fuselage_10", "mp_warhead_10"):
            if item_tex_exists(cand) and iid.split("_")[1] == cand.split("_")[1]:
                return cand
    # rod / battery families
    if iid.startswith("rod_"):
        for cand in (iid, "rod_empty", "rod_uranium", "rod"):
            if item_tex_exists(cand):
                return cand
    if iid.startswith("battery_"):
        for cand in (iid, "battery_generic", "battery_red_cell", "battery"):
            if item_tex_exists(cand):
                return cand
    # circuit / stamp / upgrade templates
    for pref, tmpl in (
        ("circuit_", "circuit_aluminium"),
        ("stamp_", "stamp_stone_flat"),
        ("upgrade_", "upgrade_template"),
        ("cap_", "cap_aluminium"),
        ("powder_", "powder_iron"),
        ("billet_", "billet_iron"),
        ("nugget_", "nugget_iron"),
        ("crystal_", "crystal_iron"),
        ("plate_", "plate_iron"),
        ("wire_", "wire_copper"),
        ("ingot_", "ingot_steel"),
    ):
        if iid.startswith(pref) and item_tex_exists(tmpl):
            # only if no more-specific png (already checked exact)
            dedicated = pref + iid[len(pref) :]
            if item_tex_exists(dedicated):
                return dedicated
            # dotted CE
            dotted = pref[:-1] + "." + iid[len(pref) :]
            if item_tex_exists(dotted):
                return dotted
            return tmpl
    # longest existing prefix
    cur = iid
    while "_" in cur:
        cur = cur.rsplit("_", 1)[0]
        if item_tex_exists(cur):
            return cur
    # token rotation
    parts = iid.split("_")
    if 2 <= len(parts) <= 5:
        for i in range(1, len(parts)):
            swapped = "_".join(parts[i:] + parts[:i])
            if item_tex_exists(swapped):
                return swapped
            dotted = ".".join(parts[i:] + parts[:i])
            if item_tex_exists(dotted):
                return dotted
    return None


def extra_block(bid: str) -> str | None:
    if block_tex_exists(bid):
        return bid
    for suf in ("_stairs", "_slab", "_double_slab", "_wall", "_fence", "_fence_gate", "_layer"):
        if bid.endswith(suf):
            parent = bid[: -len(suf)]
            for cand in (parent, "block_" + parent, parent + "_side"):
                if block_tex_exists(cand):
                    return cand
    for suf in ("_side", "_top", "_front", "_bottom", "_on", "_off", "_base", "_inner"):
        if block_tex_exists(bid + suf):
            return bid + suf
    if bid.startswith("machine_"):
        rest = bid[len("machine_") :]
        for cand in (f"machine_{rest}_side", f"{rest}_side", bid + "_side", "machine_assembler_side"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("waste_"):
        for cand in ("waste_earth", "waste_dirt", "dirt_dead", "block_waste"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("brick_"):
        for cand in (bid, "brick_concrete", "concrete_brick", "brick"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("reinforced_"):
        rest = bid[len("reinforced_") :]
        for cand in (bid, rest, f"brick_{rest}", f"block_{rest}"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("fluid_") or bid.startswith("pipe_"):
        for cand in ("fluid_duct_neo", "fluid_duct", "pipe_gauge"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("red_pylon"):
        for cand in ("red_pylon", "pylon_red_wire", "red_cable"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("deco_"):
        rest = bid[5:]
        for cand in (rest, f"block_{rest}", bid + "_side", "deco_steel"):
            if block_tex_exists(cand):
                return cand
    if bid.startswith("foam") or bid.endswith("_foam"):
        for cand in ("foam", "block_foam", "foam_layer"):
            if block_tex_exists(cand):
                return cand
    cur = bid
    while "_" in cur:
        cur = cur.rsplit("_", 1)[0]
        if block_tex_exists(cur):
            return cur
        if block_tex_exists("block_" + cur):
            return "block_" + cur
    parts = bid.split("_")
    if 2 <= len(parts) <= 5:
        for i in range(1, len(parts)):
            swapped = "_".join(parts[i:] + parts[:i])
            if block_tex_exists(swapped):
                return swapped
    return None


def playable_item(iid: str) -> bool:
    for root in (ASSETS, REPO / "src" / "generated" / "resources" / "assets" / "hbm"):
        p = root / "models" / "item" / f"{iid}.json"
        if not p.is_file():
            continue
        try:
            data = json.loads(p.read_text())
        except Exception:
            continue
        parent = data.get("parent") or ""
        if parent in ("minecraft:item/template_spawn_egg", "minecraft:builtin/entity"):
            return True
        texs = data.get("textures") or {}
        layer = texs.get("layer0") or texs.get("all") or texs.get("particle")
        if layer:
            stem = layer.split(":")[-1]
            if "/" in stem:
                folder, name = stem.split("/", 1)
                if (ASSETS / "textures" / folder / f"{name}.png").is_file():
                    return True
            if item_tex_exists(stem.split("/")[-1]):
                return True
        # parent is another hbm/minecraft model
        if parent.startswith("hbm:item/") or parent.startswith("hbm:block/"):
            return True
        if parent.startswith("minecraft:item/") or parent.startswith("minecraft:block/"):
            return True
    return item_tex_exists(iid) or item_model_exists(iid) and item_tex_exists(iid)


def playable_block(bid: str) -> bool:
    if blockstate_exists(bid):
        try:
            data = json.loads((ASSETS / "blockstates" / f"{bid}.json").read_text())
        except Exception:
            data = {}
        if data.get("multipart") or data.get("variants"):
            return True
    if (ASSETS / "models" / "block" / f"{bid}.json").is_file():
        return True
    return block_tex_exists(bid)


def leftover_why_item(iid: str) -> str:
    if re.search(r"_(helmet|plate|legs|boots|jetpack)$", iid):
        return "powered-armor: CE has 3D/layer pngs only, no inventory icon"
    if iid.endswith("_block") or iid.startswith("block_"):
        return "Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)"
    if "ammo_debug" in iid:
        return "debug ammo, no CE inventory png"
    if iid.startswith("machine_") or iid.startswith("rbmk_") or iid.startswith("ams_"):
        return "TESR/machine: no inventory png in CE"
    if "spawn_egg" in iid:
        return "spawn egg (should have been aliased to template)"
    if iid.startswith("anvil_"):
        return "anvil family: no CE per-material inventory png"
    return "no CE item png under any remap of existing files"


def leftover_why_block(bid: str) -> str:
    if bid.endswith("_block") or bid.startswith("block_"):
        return "autogen/storage cube: CE has no cube png for this mat"
    if bid.startswith("machine_") or bid.startswith("dummy_") or bid.startswith("fluid_"):
        return "TESR/duct: no cube png"
    if bid.startswith("deco_") or "crashed" in bid:
        return "TESR/deco: no cube png"
    return "no CE block png under any remap of existing files"


def write_spawn_egg(iid: str) -> None:
    dest = ASSETS / "models" / "item" / f"{iid}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps({"parent": "minecraft:item/template_spawn_egg"}, indent=2) + "\n")


def main() -> None:
    items, blocks = extract_all_ids()
    before_i = [i for i in items if not playable_item(i)]
    before_b = [b for b in blocks if not playable_block(b)]
    new_i = new_b = 0
    after_i: list[str] = []
    after_b: list[str] = []

    for iid in sorted(items):
        if playable_item(iid):
            continue
        t = resolve_item_tex(iid) or extra_item(iid)
        if t == "__SPAWN_EGG__":
            write_spawn_egg(iid)
            new_i += 1
            continue
        if t:
            write_item_model(iid, t)
            new_i += 1
            continue
        after_i.append(iid)

    for bid in sorted(blocks):
        if playable_block(bid):
            continue
        t = resolve_block_tex(bid) or extra_block(bid)
        if t:
            write_block_assets(bid, t)
            new_b += 1
            continue
        after_b.append(bid)

    ni, nb = len(items), len(blocks)
    leftover = {
        "source": "java_register_plus_autogen",
        "registered_items": ni,
        "registered_blocks": nb,
        "item_miss_before": len(before_i),
        "block_miss_before": len(before_b),
        "item_miss_after": len(after_i),
        "block_miss_after": len(after_b),
        "item_miss_pct": round(100 * len(after_i) / ni, 1) if ni else 0,
        "block_miss_pct": round(100 * len(after_b) / nb, 1) if nb else 0,
        "new_item_aliases": new_i,
        "new_block_aliases": new_b,
        "item_leftover_cats": dict(Counter(leftover_why_item(i) for i in after_i)),
        "block_leftover_cats": dict(Counter(leftover_why_block(b) for b in after_b)),
        "item_leftover": {i: leftover_why_item(i) for i in after_i},
        "block_leftover": {b: leftover_why_block(b) for b in after_b},
    }
    out = REPO / "docs" / "phase10" / "REMAP_V3.json"
    out.write_text(json.dumps(leftover, indent=2) + "\n")
    md = REPO / "docs" / "phase10" / "LEFTOVER_MISSES.md"
    lines = [
        "# Leftover texture misses (no CE file)",
        "",
        f"Census: **{ni} items** / **{nb} blocks** (Java `register`/`reg` + Mats autogen; not lang keys).",
        f"After remaps: items **{len(after_i)} ({leftover['item_miss_pct']}%)**, "
        f"blocks **{len(after_b)} ({leftover['block_miss_pct']}%)**.",
        "",
        "## Item leftover categories",
        "",
    ]
    for k, v in leftover["item_leftover_cats"].items():
        lines.append(f"- {v} — {k}")
    lines += ["", "## Block leftover categories", ""]
    for k, v in leftover["block_leftover_cats"].items():
        lines.append(f"- {v} — {k}")
    lines += ["", "## Items (id → why)", ""]
    for i in after_i:
        lines.append(f"- `{i}` — {leftover_why_item(i)}")
    lines += ["", "## Blocks (id → why)", ""]
    for b in after_b:
        lines.append(f"- `{b}` — {leftover_why_block(b)}")
    lines.append("")
    md.write_text("\n".join(lines))
    print(f"items {ni} miss {len(before_i)}->{len(after_i)} ({leftover['item_miss_pct']}%)")
    print(f"blocks {nb} miss {len(before_b)}->{len(after_b)} ({leftover['block_miss_pct']}%)")
    print(f"wrote {new_i} item aliases, {new_b} block aliases")
    print("wrote", out, md)


if __name__ == "__main__":
    main()
