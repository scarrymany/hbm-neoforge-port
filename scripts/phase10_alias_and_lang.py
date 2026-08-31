#!/usr/bin/env python3
"""Phase 10 glue: alias models/blockstates onto existing CE pngs + port lang.

Does not invent art. Prefer pointing JSON at a real CE file over copying pngs.
"""
from __future__ import annotations

import json
import re
import shutil
from collections import Counter, defaultdict
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
JAVA = REPO / "src" / "main" / "java" / "com" / "hbm"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "hbm"
GEN_LANG = REPO / "src" / "generated" / "resources" / "assets" / "hbm" / "lang"
CE_LANG = REPO / "upstream" / "hbm-ce" / "src" / "main" / "resources" / "assets" / "hbm" / "lang"
MATS = REPO / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "material" / "Mats.java"

# MaterialItemGenerator AUTOGEN_SHAPES → CE template png + lang format key
# port id = "{mat}_{shape_token}"
SHAPE_ALIASES: list[tuple[str, str, str]] = [
    # suffix on port id, preferred dedicated prefix, fallback template stem
    ("_ore_fragment", "bedrock_ore_fragment", "bedrock_ore_fragment"),
    ("_plate_sextuple", "plate_welded", "plate_welded"),
    ("_plate_triple", "plate_cast", "plate_cast"),
    ("_dense_wire", "wire_dense", "wire_dense"),
    ("_light_barrel", "part_barrel_light", "part_barrel_light"),
    ("_heavy_barrel", "part_barrel_heavy", "part_barrel_heavy"),
    ("_light_receiver", "part_receiver_light", "part_receiver_light"),
    ("_heavy_receiver", "part_receiver_heavy", "part_receiver_heavy"),
    ("_gun_mechanism", "part_mechanism", "part_mechanism"),
    ("_raw_ingot", "ingot_raw", "ingot_raw"),  # unused, safety
    ("_ingot", "ingot", "ingot_raw"),
    ("_wire", "wire", "wire_fine"),
    ("_shell", "shell", "shell"),
    ("_pipe", "pipe", "pipe"),
    ("_bolt", "bolt", "bolt"),
    ("_stock", "part_stock", "part_stock"),
    ("_grip", "part_grip", "part_grip"),
    ("_block", "block", None),  # storage cubes: block_{mat}
]

SHAPE_LANG = {
    "_ore_fragment": "item.bedrock_ore_fragment.name",
    "_plate_sextuple": "item.plate_welded.name",
    "_plate_triple": "item.plate_cast.name",
    "_dense_wire": "item.wire_dense.name",
    "_light_barrel": "item.part_barrel_light.name",
    "_heavy_barrel": "item.part_barrel_heavy.name",
    "_light_receiver": "item.part_receiver_light.name",
    "_heavy_receiver": "item.part_receiver_heavy.name",
    "_gun_mechanism": "item.part_mechanism.name",
    "_ingot": "item.ingot_raw.name",
    "_wire": "item.wire_fine.name",
    "_shell": "item.shell.name",
    "_pipe": "item.pipe.name",
    "_bolt": "item.boltntm.name",
    "_stock": "item.part_stock.name",
    "_grip": "item.part_grip.name",
}

SPELL = {
    "aluminum": "aluminium",
    "aluminium": "aluminum",
    "weaponsteel": "weapon_steel",
    "dura": "dura_steel",
    "mingrade": "red_copper",
    "magtung": "magnetized_tungsten",
    "cmb": "combine_steel",
    "star": "starmetal",
    "dnt": "dineutronium",
    "ivory": "bone",
    "bbronze": "bismuth_bronze",
    "abronze": "arsenic_bronze",
    "hardplastic": "pc",
    "saturn": "saturnite",
    "ferro": "ferrouranium",
}


def item_tex_exists(stem: str) -> bool:
    return (ASSETS / "textures" / "item" / f"{stem}.png").is_file()


def block_tex_exists(stem: str) -> bool:
    return (ASSETS / "textures" / "block" / f"{stem}.png").is_file()


def item_model_exists(stem: str) -> bool:
    return (ASSETS / "models" / "item" / f"{stem}.json").is_file() or (
        REPO / "src" / "generated" / "resources" / "assets" / "hbm" / "models" / "item" / f"{stem}.json"
    ).is_file()


def blockstate_exists(stem: str) -> bool:
    return (ASSETS / "blockstates" / f"{stem}.json").is_file()


def block_model_exists(stem: str) -> bool:
    return (ASSETS / "models" / "block" / f"{stem}.json").is_file()


def alt_spells(token: str) -> list[str]:
    out = [token]
    if token in SPELL:
        out.append(SPELL[token])
    # uranium233 → u233
    m = re.fullmatch(r"uranium(\d+)", token)
    if m:
        out.append("u" + m.group(1))
    m = re.fullmatch(r"u(\d+)", token)
    if m:
        out.append("uranium" + m.group(1))
    return out


def extract_java_ids() -> tuple[set[str], set[str]]:
    items: set[str] = set()
    blocks: set[str] = set()
    ident = r"([a-z][a-z0-9_]*)"
    item_helpers = re.compile(
        r'(?:ITEMS\.register|registerIngot|registerNugget|registerLoreIngot|registerItem|'
        r'registerGun|registerAmmo|registerBlockItem)\(\s*"' + ident + r'"'
    )
    any_register = re.compile(r'\.register\(\s*"' + ident + r'"')
    block_helpers = re.compile(
        r'(?:BLOCKS\.register|registerBlock)\(\s*"' + ident + r'"'
    )
    for p in JAVA.rglob("*.java"):
        text = p.read_text(errors="ignore")
        rel = p.as_posix()
        if "/items/" in rel or p.name == "ModItems.java":
            items.update(item_helpers.findall(text))
            items.update(any_register.findall(text))
            items.update(re.findall(r'(?<![A-Za-z])register\(\s*"' + ident + r'"', text))
        if "/blocks/" in rel or p.name in {"ModBlocks.java", "OreBlocks.java", "MaterialBlockGenerator.java"}:
            blocks.update(block_helpers.findall(text))
            blocks.update(any_register.findall(text))
            items.update(re.findall(r'ModItems\.ITEMS\.register\(\s*"' + ident + r'"', text))
    items.update(_autogen_item_ids())
    blocks.update(_autogen_block_ids())
    legal = re.compile(r"^[a-z][a-z0-9_]*$")
    items = {x for x in items if legal.fullmatch(x) and not x.endswith("_")}
    blocks = {x for x in blocks if legal.fullmatch(x) and not x.endswith("_")}
    # block items share ids
    items |= blocks
    return items, blocks


SHAPE_TOKENS = {
    "SHELL": "shell",
    "PIPE": "pipe",
    "INGOT": "ingot",
    "CASTPLATE": "plate_triple",
    "WELDEDPLATE": "plate_sextuple",
    "HEAVY_COMPONENT": "heavy_component",
    "WIRE": "wire",
    "DENSEWIRE": "dense_wire",
    "BOLT": "bolt",
    "LIGHTBARREL": "light_barrel",
    "HEAVYBARREL": "heavy_barrel",
    "LIGHTRECEIVER": "light_receiver",
    "HEAVYRECEIVER": "heavy_receiver",
    "MECHANISM": "gun_mechanism",
    "STOCK": "stock",
    "GRIP": "grip",
    "FRAGMENT": "ore_fragment",
    "BLOCK": "block",
}

ITEM_AUTOGEN_SHAPES = {
    "SHELL", "PIPE", "INGOT", "CASTPLATE", "WELDEDPLATE", "HEAVY_COMPONENT",
    "WIRE", "DENSEWIRE", "BOLT", "LIGHTBARREL", "HEAVYBARREL", "LIGHTRECEIVER",
    "HEAVYRECEIVER", "MECHANISM", "STOCK", "GRIP", "FRAGMENT",
}


def _mats_autogen() -> list[tuple[str, set[str]]]:
    text = MATS.read_text()
    rows = []
    for m in re.finditer(
        r"n\(\s*\"([^\"]+)\"[^\)]*\)(.*?)(?:\.n\(\)|;)",
        text,
        flags=re.S,
    ):
        canon = m.group(1).lower()
        tail = m.group(2)
        ag = re.search(r"setAutogen\(([^)]*)\)", tail)
        shapes: set[str] = set()
        if ag:
            shapes = {s.strip() for s in ag.group(1).split(",") if s.strip() in SHAPE_TOKENS}
        rows.append((canon, shapes))
    return rows


def _autogen_item_ids() -> set[str]:
    out = set()
    for mat, shapes in _mats_autogen():
        for sh in shapes:
            if sh in ITEM_AUTOGEN_SHAPES:
                out.add(f"{mat}_{SHAPE_TOKENS[sh]}")
    return out


def _autogen_block_ids() -> set[str]:
    return {f"{mat}_{SHAPE_TOKENS['BLOCK']}" for mat, shapes in _mats_autogen() if "BLOCK" in shapes}


def parse_mats() -> dict[str, list[str]]:
    """canonical registry name → all alias names lowercased."""
    text = MATS.read_text()
    out: dict[str, list[str]] = {}
    for m in re.finditer(r"n\(\s*((?:\"[^\"]+\"\s*,\s*)*\"[^\"]+\")\s*\)", text):
        names = re.findall(r"\"([^\"]+)\"", m.group(1))
        if not names:
            continue
        canon = names[0].lower()
        aliases = [n.lower() for n in names]
        out.setdefault(canon, [])
        for a in aliases:
            if a not in out[canon]:
                out[canon].append(a)
    return out


def resolve_item_tex(item_id: str) -> str | None:
    """Return textures/item/<stem> without .png, or None."""
    if item_tex_exists(item_id):
        return item_id
    # flatten suffix _0 .. _99
    m = re.fullmatch(r"(.+)_(\d{1,2})", item_id)
    if m and item_tex_exists(m.group(1)):
        return m.group(1)
    # strip last _token and retry (u238m2_elements → u238m2 / hs-elements later)
    if "_" in item_id:
        parent = item_id.rsplit("_", 1)[0]
        if item_tex_exists(parent):
            return parent
    # prefix/suffix swap: steel_ingot → ingot_steel
    if "_" in item_id:
        a, b = item_id.split("_", 1)
        swapped = f"{b}_{a}"
        if item_tex_exists(swapped):
            return swapped
        # three-part: aluminum_plate_triple already handled by shape table
    # known flatten reskins
    special = {
        "ingot_u238m2_elements": "hs-elements",
        "ingot_u238m2_arsenic": "hs-arsenic",
        "ingot_u238m2_vault": "hs-vault",
    }
    if item_id in special and item_tex_exists(special[item_id]):
        return special[item_id]
    # shape aliases
    for suffix, dedicated_prefix, template in SHAPE_ALIASES:
        if not item_id.endswith(suffix):
            continue
        mat = item_id[: -len(suffix)]
        for tok in alt_spells(mat):
            cand = f"{dedicated_prefix}_{tok}" if dedicated_prefix not in ("ingot", "wire", "block", "shell", "pipe", "bolt") else f"{dedicated_prefix}_{tok}"
            if item_tex_exists(cand):
                return cand
            # CE prefix style: wire_copper, ingot_steel, bolt_tungsten
            if dedicated_prefix in ("wire", "ingot", "bolt", "pipe", "shell"):
                if item_tex_exists(f"{dedicated_prefix}_{tok}"):
                    return f"{dedicated_prefix}_{tok}"
        if template and item_tex_exists(template):
            return template
    # aluminium/aluminum whole-id
    for a, b in (("aluminum", "aluminium"), ("aluminium", "aluminum")):
        if a in item_id:
            alt = item_id.replace(a, b)
            if item_tex_exists(alt):
                return alt
    # CE dotted stems: achievement_icon.acid, battery_sc.am241, ammo_standard.b75
    for prefix in ("achievement_icon_", "ammo_standard_", "ammo_secret_", "battery_sc_", "part_generic_"):
        if item_id.startswith(prefix):
            cand = prefix[:-1] + "." + item_id[len(prefix) :]
            if item_tex_exists(cand):
                return cand
    for pref in ("ammo_standard.", "ammo_secret."):
        if item_tex_exists(pref + item_id):
            return pref + item_id
    if item_id.startswith("bedrock_ore_new_"):
        cand = "bedrock_ore_" + item_id[len("bedrock_ore_new_") :]
        if item_tex_exists(cand):
            return cand
    # last-underscore → dot (battery_sc_am241 already covered; generic fallback)
    if item_id.count("_") >= 1:
        dotted = item_id[::-1].replace("_", ".", 1)[::-1]
        if item_tex_exists(dotted):
            return dotted
    return None


def resolve_block_tex(block_id: str) -> str | None:
    if block_tex_exists(block_id):
        return block_id
    m = re.fullmatch(r"(.+)_(\d{1,2})", block_id)
    if m and block_tex_exists(m.group(1)):
        return m.group(1)
    if block_id.endswith("_block"):
        mat = block_id[: -len("_block")]
        for tok in alt_spells(mat):
            if block_tex_exists(f"block_{tok}"):
                return f"block_{tok}"
    if block_id.startswith("block_"):
        mat = block_id[len("block_") :]
        for tok in alt_spells(mat):
            if block_tex_exists(f"{tok}_block"):
                return f"{tok}_block"
    if "_" in block_id:
        parent = block_id.rsplit("_", 1)[0]
        if block_tex_exists(parent):
            return parent
    for a, b in (("aluminum", "aluminium"), ("aluminium", "aluminum")):
        if a in block_id:
            alt = block_id.replace(a, b)
            if block_tex_exists(alt):
                return alt
    if block_id.startswith("block_") and block_tex_exists(block_id[6:]):
        return block_id[6:]
    if block_tex_exists("block_" + block_id):
        return "block_" + block_id
    if block_id.startswith("basalt_ore_"):
        kind = block_id[len("basalt_ore_") :]
        for cand in (f"ore_basalt.{kind}", f"basalt_{kind}"):
            if block_tex_exists(cand):
                return cand
    if "meteor_ore" in block_id:
        kind = block_id.split("meteor_ore_", 1)[-1] if "meteor_ore_" in block_id else ""
        if kind and block_tex_exists(f"ore_meteor.{kind}"):
            return f"ore_meteor.{kind}"
    for suffix in ("_off", "_on", "_side", "_top"):
        if block_tex_exists(block_id + suffix):
            return block_id + suffix
    return None


def write_item_model(item_id: str, tex: str) -> None:
    dest = ASSETS / "models" / "item" / f"{item_id}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(
        json.dumps(
            {"parent": "minecraft:item/generated", "textures": {"layer0": f"hbm:item/{tex}"}},
            indent=2,
        )
        + "\n"
    )


def write_block_assets(block_id: str, tex: str) -> None:
    model = {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": f"hbm:block/{tex}"},
    }
    mpath = ASSETS / "models" / "block" / f"{block_id}.json"
    mpath.parent.mkdir(parents=True, exist_ok=True)
    if not mpath.is_file():
        mpath.write_text(json.dumps(model, indent=2) + "\n")
    bpath = ASSETS / "blockstates" / f"{block_id}.json"
    if not bpath.is_file():
        bpath.write_text(
            json.dumps({"variants": {"": {"model": f"hbm:block/{block_id}"}}}, indent=2) + "\n"
        )
    ipath = ASSETS / "models" / "item" / f"{block_id}.json"
    if not ipath.is_file():
        ipath.write_text(json.dumps({"parent": f"hbm:block/{block_id}"}, indent=2) + "\n")


def fix_blockstates() -> int:
    """Ensure every copied CE blockstate has a '' variant so property-less 1.21 blocks render."""
    fixed = 0
    bs_dir = ASSETS / "blockstates"
    if not bs_dir.is_dir():
        return 0
    for p in bs_dir.glob("*.json"):
        try:
            data = json.loads(p.read_text())
        except json.JSONDecodeError:
            continue
        variants = data.get("variants")
        if not isinstance(variants, dict) or not variants:
            continue
        if "" in variants:
            continue
        first = next(iter(variants.values()))
        # drop 1.12-only property bags that 1.21 will ignore, keep a default
        variants[""] = first
        # if ALL keys look like 1.12 meta leftovers, replace with just ""
        leftover_props = ("meta=", "taintage=", "variant=", "rot=", "height=", "covered=")
        if all(any(tok in k for tok in leftover_props) or k == "inventory" for k in list(variants) if k != ""):
            data["variants"] = {"": first}
        p.write_text(json.dumps(data, indent=2) + "\n")
        fixed += 1
    return fixed


def parse_lang(path: Path) -> dict[str, str]:
    out: dict[str, str] = {}
    for raw in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = raw.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k] = v
    return out


def port_lang_keys(ce: dict[str, str]) -> dict[str, str]:
    out: dict[str, str] = {}
    for k, v in ce.items():
        if k.startswith("item.") and k.endswith(".name"):
            mid = k[len("item.") : -len(".name")]
            # item.ammo_standard.bmg50_ap.name → item.hbm.bmg50_ap AND keep namespaced
            if mid.startswith("ammo_standard.") or mid.startswith("ammo_secret."):
                short = mid.split(".", 1)[1]
                out[f"item.hbm.{short}"] = v
                out[f"item.hbm.{mid}"] = v
            out[f"item.hbm.{mid}"] = v
            continue
        if k.startswith("item.") and k.endswith(".desc"):
            mid = k[len("item.") : -len(".desc")]
            out[f"item.hbm.{mid}.desc"] = v
            out[k] = v  # some Java still uses old key
            continue
        if k.startswith("tile.") and k.endswith(".name"):
            mid = k[len("tile.") : -len(".name")]
            out[f"block.hbm.{mid}"] = v
            continue
        if k.startswith("tile.") and k.endswith(".desc"):
            mid = k[len("tile.") : -len(".desc")]
            out[f"block.hbm.{mid}.desc"] = v
            out[k] = v
            continue
        if k.startswith("entity.") and k.endswith(".name"):
            mid = k[len("entity.") : -len(".name")]
            # 1.21 entity.hbm.<id>
            out[f"entity.hbm.{mid}"] = v
            out[k] = v
            continue
        # everything else: keep verbatim (achievement, container, trait, hbmmat, ...)
        out[k] = v
    return out


def compose_autogen_names(items: set[str], ce: dict[str, str], mats: dict[str, list[str]]) -> dict[str, str]:
    hbmmat = {k[len("hbmmat.") :]: v for k, v in ce.items() if k.startswith("hbmmat.")}
    extra: dict[str, str] = {}
    for item_id in items:
        key = f"item.hbm.{item_id}"
        for suffix, lang_key in SHAPE_LANG.items():
            if not item_id.endswith(suffix):
                continue
            mat = item_id[: -len(suffix)]
            fmt = ce.get(lang_key)
            if not fmt or "%s" not in fmt:
                continue
            mat_name = None
            for tok in [mat, *alt_spells(mat)]:
                if tok in hbmmat:
                    mat_name = hbmmat[tok]
                    break
                # mats aliases
                for canon, aliases in mats.items():
                    if tok == canon or tok in aliases:
                        mat_name = hbmmat.get(canon) or hbmmat.get(aliases[0] if aliases else "")
                        if mat_name:
                            break
                if mat_name:
                    break
            if mat_name:
                extra[key] = fmt.replace("%s", mat_name, 1)
            break
    return extra


def write_json(path: Path, data: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def main() -> int:
    items, blocks = extract_java_ids()
    mats = parse_mats()

    before = {"item_tex": 0, "item_model": 0, "block_tex": 0, "blockstate": 0}
    after_src: dict[str, int] = {}

    item_miss_before = []
    item_miss_after = []
    block_miss_before = []
    block_miss_after = []
    aliased_items = 0
    aliased_blocks = 0

    for i in sorted(items):
        tex_hit = item_tex_exists(i)
        model_hit = item_model_exists(i)
        if tex_hit:
            before["item_tex"] += 1
        if model_hit:
            before["item_model"] += 1
        if i in blocks:
            playable = blockstate_exists(i) or block_tex_exists(i) or bool(resolve_block_tex(i))
        else:
            playable = tex_hit and model_hit
        if not playable:
            item_miss_before.append(i)
        resolved = i if tex_hit else resolve_item_tex(i)
        if resolved:
            if not model_hit or (not tex_hit):
                write_item_model(i, resolved)
                aliased_items += 1
        elif i in blocks:
            btex = resolve_block_tex(i)
            if btex:
                write_block_assets(i, btex)
                aliased_blocks += 1
            else:
                item_miss_after.append(i)
        else:
            item_miss_after.append(i)

    for b in sorted(blocks):
        tex_hit = block_tex_exists(b)
        bs_hit = blockstate_exists(b)
        if tex_hit:
            before["block_tex"] += 1
        if bs_hit:
            before["blockstate"] += 1
        if not (tex_hit and bs_hit):
            block_miss_before.append(b)
        resolved = b if tex_hit else resolve_block_tex(b)
        if resolved:
            if not tex_hit or not bs_hit or not block_model_exists(b):
                write_block_assets(b, resolved)
                aliased_blocks += 1
        else:
            # still try blockstate '' fix later
            if not tex_hit:
                block_miss_after.append(b)

    bs_fixed = fix_blockstates()

    # recount after
    item_tex_after = sum(
        1
        for i in items
        if item_tex_exists(i) or resolve_item_tex(i) or (i in blocks and resolve_block_tex(i))
    )
    item_model_after = sum(1 for i in items if item_model_exists(i) or (i in blocks and blockstate_exists(i)))
    block_tex_after = sum(1 for b in blocks if block_tex_exists(b) or resolve_block_tex(b))
    blockstate_after = sum(1 for b in blocks if blockstate_exists(b))

    # ---- lang ----
    locales = ["en_us", "ru_ru", "uk_ua"]
    lang_stats = {}
    for loc in locales:
        src = CE_LANG / f"{loc}.lang"
        if not src.is_file():
            continue
        ce = parse_lang(src)
        ported = port_lang_keys(ce)
        if loc == "en_us":
            ported.update(compose_autogen_names(items, ce, mats))
            # flatten dusted etc.: copy parent name
            for i in items:
                k = f"item.hbm.{i}"
                if k in ported:
                    continue
                if re.fullmatch(r".+_\d{1,2}", i):
                    parent = i.rsplit("_", 1)[0]
                    if f"item.hbm.{parent}" in ported:
                        ported[k] = ported[f"item.hbm.{parent}"]
                if "_" in i:
                    parent = i.rsplit("_", 1)[0]
                    if f"item.hbm.{parent}" in ported and k not in ported:
                        ported[k] = ported[f"item.hbm.{parent}"]
            for b in blocks:
                k = f"block.hbm.{b}"
                if k in ported:
                    continue
                if "_" in b:
                    parent = b.rsplit("_", 1)[0]
                    if f"block.hbm.{parent}" in ported:
                        ported[k] = ported[f"block.hbm.{parent}"]
        dest = ASSETS / "lang" / f"{loc}.json"
        write_json(dest, ported)
        if loc == "en_us":
            write_json(GEN_LANG / "en_us.json", ported)
        named_items = sum(1 for i in items if f"item.hbm.{i}" in ported)
        named_blocks = sum(1 for b in blocks if f"block.hbm.{b}" in ported)
        lang_stats[loc] = {
            "ce_keys": len(ce),
            "ported_keys": len(ported),
            "item_ids_named": named_items,
            "block_ids_named": named_blocks,
        }

    report = {
        "registered_items": len(items),
        "registered_blocks": len(blocks),
        "before": {
            "item_exact_tex": before["item_tex"],
            "item_exact_model": before["item_model"],
            "item_miss_playable": len(item_miss_before),
            "block_exact_tex": before["block_tex"],
            "block_exact_blockstate": before["blockstate"],
            "block_miss_playable": len(block_miss_before),
        },
        "after": {
            "item_resolvable_tex": item_tex_after,
            "item_models": item_model_after,
            "item_unresolved": len(item_miss_after),
            "block_resolvable_tex": block_tex_after,
            "block_blockstates": blockstate_after,
            "block_unresolved_tex": len(block_miss_after),
            "aliased_item_models": aliased_items,
            "aliased_block_assets": aliased_blocks,
            "blockstates_default_variant": bs_fixed,
        },
        "miss_rate": {
            "item_before": round(len(item_miss_before) / max(len(items), 1), 4),
            "item_after": round(len(item_miss_after) / max(len(items), 1), 4),
            "block_before": round(len(block_miss_before) / max(len(blocks), 1), 4),
            "block_after": round(len(block_miss_after) / max(len(blocks), 1), 4),
        },
        "item_unresolved_sample": item_miss_after[:40],
        "block_unresolved_sample": block_miss_after[:40],
        "lang": lang_stats,
    }
    out = REPO / "docs" / "phase10" / "GLUE_CENSUS.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    write_json(out, report)
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
