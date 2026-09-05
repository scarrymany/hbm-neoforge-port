#!/usr/bin/env python3
"""Strict registry vs CE asset audit. Copy only missing CE files; rewrite broken models.

Does NOT invent textures. Does NOT alias unrelated templates.
Does NOT overwrite builtin/entity gun models.
"""
from __future__ import annotations

import json
import re
import shutil
import sys
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from phase10_remap_v3 import extract_all_ids  # noqa: E402

CE = ROOT / "upstream" / "hbm-ce" / "src" / "main" / "resources" / "assets" / "hbm"
PORT = ROOT / "src" / "main" / "resources" / "assets" / "hbm"
JAVA = ROOT / "src" / "main" / "java" / "com" / "hbm"

CE_ITEM_TEX = CE / "textures" / "items"
CE_BLOCK_TEX = CE / "textures" / "blocks"
CE_ITEM_MODEL = CE / "models" / "item"
CE_BLOCK_MODEL = CE / "models" / "block"
CE_BLOCKSTATE = CE / "blockstates"

PORT_ITEM_TEX = PORT / "textures" / "item"
PORT_BLOCK_TEX = PORT / "textures" / "block"
PORT_ITEM_MODEL = PORT / "models" / "item"
PORT_BLOCK_MODEL = PORT / "models" / "block"
PORT_BLOCKSTATE = PORT / "blockstates"
PORT_MODEL_TEX = PORT / "textures" / "models"

CE_MODEL_TEX_ROOTS = [
    CE / "textures" / "models" / "bombs",
    CE / "textures" / "models" / "machines",
    CE / "textures" / "models" / "projectiles",
    CE / "textures" / "models" / "weapons",
]

COPIED: list[str] = []
REWIRED: list[str] = []
SKIP_GUN_MODELS: set[str] = set()

# Comments in ModItems / BasicArmorItems, not real registry ids.
COMMENT_JUNK = {"id", "x"}

# Mats autogen {mat}_block → CE storage-cube stem (same material, CE spelling).
MAT_BLOCK_CE: dict[str, str] = {
    "cmbsteel": "block_combine_steel",
    "magnetizedtungsten": "block_magnetized_tungsten",
    "durasteel": "block_dura_steel",
    "lanthanum": "block_lanthanium",
    "tantalum": "block_tantalium",
    "saltpeter": "block_niter",
    "redphosphorus": "block_red_phosphorus",
    "neptunium237": "block_neptunium",
    "plutonium238": "block_pu238",
    "plutonium239": "block_pu239",
    "plutonium240": "block_pu240",
    "plutoniumrg": "block_pu_mix",
    "polonium210": "block_polonium",
    "radium226": "block_ra226",
    "thorium232": "block_thorium",
    "workersalloy": "block_desh",
}

# Vanilla cubes CE itself uses (see block_au198 → gold_block).
VANILLA_BLOCK: dict[str, str] = {
    "gold_block": "minecraft:block/gold_block",
    "emerald_block": "minecraft:block/emerald_block",
    "iron_block": "minecraft:block/iron_block",
}

# Same-object CE bomb/machine skins (not invented).
NUKE_CE: dict[str, str] = {
    "nuke_boy": "lilboy",
    "nuke_gadget": "gadget",
    "nuke_fleija": "fleija",
    "nuke_tsar": "tsar",
    "nuke_man": "fatman",
    "nuke_mike": "ivymike",
    "nuke_prototype": "prototype",
}

# Same-object deco / misc remaps onto existing CE pngs.
EXPLICIT_CE: dict[str, tuple[str, str]] = {
    # id → (ce_rel_under textures/, layer0)
    "vitrified_barrel": ("blocks/barrel_vitrified.png", "hbm:block/barrel_vitrified"),
    "flask_infusion": ("items/flask_infusion.empty.png", "hbm:item/flask_infusion.empty"),
    "tape_recorder": ("blocks/deco_tape_recorder_flipped.png", "hbm:block/deco_tape_recorder_flipped"),
    "pole_top": ("blocks/deco_pole_top_flipped.png", "hbm:block/deco_pole_top_flipped"),
    "dungeon_chain": ("blocks/chain.png", "hbm:block/chain"),
    "gun_vortex": ("models/weapons/vortex.png", "hbm:models/weapons/vortex"),
}


def load_skip_guns() -> None:
    guns = PORT / "models" / "item" / "sedna" / "guns"
    if not guns.is_dir():
        return
    for p in guns.glob("*.json"):
        SKIP_GUN_MODELS.add(p.stem)


def data_component_ids() -> set[str]:
    ids: set[str] = set()
    for p in JAVA.rglob("*.java"):
        text = p.read_text(errors="ignore")
        if "DATA_COMPONENT_TYPES" not in text and "DataComponentType" not in p.name:
            continue
        ids.update(re.findall(r'register\(\s*"([a-z][a-z0-9_]*)"', text))
    return ids


def block_entity_only_ids() -> set[str]:
    """BLOCK_ENTITY_TYPES.register names that are not also BLOCKS.register."""
    be: set[str] = set()
    blocks_reg: set[str] = set()
    for p in JAVA.rglob("*.java"):
        text = p.read_text(errors="ignore")
        if "BLOCK_ENTITY_TYPES.register" in text:
            be.update(re.findall(r'BLOCK_ENTITY_TYPES\.register\(\s*"([a-z][a-z0-9_]*)"', text))
        if "BLOCKS.register" in text or "registerBlock" in text:
            blocks_reg.update(re.findall(r'(?:BLOCKS\.register|registerBlock(?:Item|NoTab)?)\(\s*"([a-z][a-z0-9_]*)"', text))
    return be - blocks_reg


def registry_ids() -> tuple[set[str], set[str]]:
    items, blocks = extract_all_ids()
    junk = data_component_ids() | COMMENT_JUNK | block_entity_only_ids()
    items -= junk
    blocks -= junk
    return items, blocks


def resolve_tex_ref(ref: str) -> Path | None:
    """Map a model texture ref to a port png path. Vanilla refs return a sentinel Path."""
    ref = str(ref)
    if ref.startswith("#"):
        return None
    if ref.startswith("minecraft:"):
        return Path("__vanilla__")
    if ":" not in ref:
        if ref.startswith("block/") or ref.startswith("item/"):
            return Path("__vanilla__")
        # 1.12 leftover: blocks/foo
        if ref.startswith("blocks/"):
            return PORT_BLOCK_TEX / (ref[len("blocks/") :] + ".png")
        if ref.startswith("items/"):
            return PORT_ITEM_TEX / (ref[len("items/") :] + ".png")
        return None
    ns, rest = ref.split(":", 1)
    if ns != "hbm":
        return Path("__vanilla__") if ns == "minecraft" else None
    parts = rest.split("/")
    if not parts:
        return None
    if parts[0] in {"item", "items"}:
        return PORT / "textures" / "item" / ("/".join(parts[1:]) + ".png")
    if parts[0] in {"block", "blocks"}:
        return PORT / "textures" / "block" / ("/".join(parts[1:]) + ".png")
    if parts[0] == "models":
        return PORT / "textures" / "models" / ("/".join(parts[1:]) + ".png")
    return PORT / "textures" / Path(rest + ".png")


def tex_ok(ref: str) -> bool:
    p = resolve_tex_ref(ref)
    if p is None:
        return False
    if p.name == "__vanilla__":
        return True
    return p.is_file()


def model_textures_ok(model_path: Path, seen: set[Path] | None = None) -> bool:
    if seen is None:
        seen = set()
    if model_path in seen or not model_path.is_file():
        return False
    if model_path.suffix == ".obj":
        return False
    seen.add(model_path)
    try:
        data = json.loads(model_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return False
    parent = str(data.get("parent") or "")
    if parent == "builtin/entity":
        return True
    if parent.endswith("template_spawn_egg"):
        return True
    texs = data.get("textures") or {}
    if texs:
        if all(tex_ok(str(v)) for v in texs.values()):
            return True
        return False
    if parent.startswith("hbm:"):
        rest = parent.split(":", 1)[1]
        if rest.startswith("item/"):
            return model_textures_ok(PORT_ITEM_MODEL / f"{rest[5:]}.json", seen)
        if rest.startswith("block/"):
            return model_textures_ok(PORT_BLOCK_MODEL / f"{rest[6:]}.json", seen)
    return False


def item_playable(iid: str) -> bool:
    model = PORT_ITEM_MODEL / f"{iid}.json"
    if not model.is_file():
        return False
    return model_textures_ok(model)


def iter_blockstate_models(data: dict) -> list[str]:
    models: list[str] = []
    variants = data.get("variants") or {}
    if isinstance(variants, dict):
        for v in variants.values():
            if isinstance(v, dict) and v.get("model"):
                models.append(str(v["model"]))
            elif isinstance(v, list):
                for e in v:
                    if isinstance(e, dict) and e.get("model"):
                        models.append(str(e["model"]))
    mpart = data.get("multipart") or []
    if isinstance(mpart, list):
        for e in mpart:
            apply = e.get("apply") if isinstance(e, dict) else None
            if isinstance(apply, dict) and apply.get("model"):
                models.append(str(apply["model"]))
            elif isinstance(apply, list):
                for a in apply:
                    if isinstance(a, dict) and a.get("model"):
                        models.append(a["model"])
    defaults = data.get("defaults") or {}
    if isinstance(defaults, dict) and defaults.get("model"):
        models.append(str(defaults["model"]))
    return models


def model_path_from_ref(m: str) -> Path | None:
    if m.endswith(".obj"):
        return None
    rest = m.split(":", 1)[-1] if ":" in m else m
    if rest.startswith("block/"):
        return PORT_BLOCK_MODEL / f"{rest[6:]}.json"
    if rest.startswith("item/"):
        return PORT_ITEM_MODEL / f"{rest[5:]}.json"
    return PORT_BLOCK_MODEL / f"{rest}.json"


def block_playable(bid: str) -> bool:
    bs = PORT_BLOCKSTATE / f"{bid}.json"
    if not bs.is_file():
        return False
    try:
        data = json.loads(bs.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return False
    models = iter_blockstate_models(data)
    if not models:
        return False
    for m in models:
        mp = model_path_from_ref(m)
        if mp and model_textures_ok(mp):
            return True
    return False


def index_pngs(root: Path) -> dict[str, Path]:
    out: dict[str, Path] = {}
    if not root.is_dir():
        return out
    for p in root.rglob("*.png"):
        rel = p.relative_to(root).as_posix()[:-4]
        out[rel] = p
        out[p.stem] = p
        out[p.stem.lower()] = p
    return out


def flatten_candidates(name: str) -> list[str]:
    cands = [name]
    if "_" in name:
        cands.append(name.replace("_", ".", 1))
        cands.append(name.replace("_", "."))
        last = name.rsplit("_", 1)
        cands.append(last[0] + "." + last[1])
    if name.endswith("_block"):
        mat = name[: -len("_block")]
        cands.append(f"block_{mat}")
        cands.append(f"block.{mat}")
        if mat in MAT_BLOCK_CE:
            cands.append(MAT_BLOCK_CE[mat])
    if name.startswith("block_"):
        cands.append(name[6:] + "_block")
    if name.startswith("machine_"):
        cands.append(name[len("machine_") :])
    if name.startswith("stone_resource_"):
        cands.append("stone_resource." + name[len("stone_resource_") :])
    if name.startswith("lightstone_"):
        rest = name[len("lightstone_") :]
        cands.append("lightstone." + rest.replace("_stairs", "").replace("_slab", ""))
    for n in (1, 2, 3, 4, 5):
        cands.append(f"{name}_{n}")
    return list(dict.fromkeys(cands))


def find_ce_png(name: str, *indexes: dict[str, Path]) -> Path | None:
    for idx in indexes:
        for c in flatten_candidates(name):
            if c in idx:
                return idx[c]
    return None


def copy_png(src: Path, dest: Path) -> bool:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.is_file():
        return False
    shutil.copy2(src, dest)
    COPIED.append(f"{src.relative_to(CE) if src.is_relative_to(CE) else src} -> {dest.relative_to(PORT)}")
    return True


def is_gun_model(path: Path) -> bool:
    if path.stem in SKIP_GUN_MODELS:
        return True
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (json.JSONDecodeError, OSError):
        return False
    return data.get("parent") == "builtin/entity"


def write_item_model(iid: str, layer0: str) -> None:
    dest = PORT_ITEM_MODEL / f"{iid}.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.is_file() and is_gun_model(dest):
        return
    dest.write_text(
        json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": layer0}}, indent=2)
        + "\n",
        encoding="utf-8",
    )
    REWIRED.append(f"item:{iid}")


def write_block_cube(bid: str, tex: str) -> None:
    PORT_BLOCK_MODEL.mkdir(parents=True, exist_ok=True)
    PORT_BLOCKSTATE.mkdir(parents=True, exist_ok=True)
    (PORT_BLOCK_MODEL / f"{bid}.json").write_text(
        json.dumps({"parent": "minecraft:block/cube_all", "textures": {"all": tex}}, indent=2) + "\n",
        encoding="utf-8",
    )
    (PORT_BLOCKSTATE / f"{bid}.json").write_text(
        json.dumps({"variants": {"": {"model": f"hbm:block/{bid}"}}}, indent=2) + "\n",
        encoding="utf-8",
    )
    item = PORT_ITEM_MODEL / f"{bid}.json"
    if not item.is_file() or not model_textures_ok(item):
        # parent the item to the cube
        item.parent.mkdir(parents=True, exist_ok=True)
        if not (item.is_file() and is_gun_model(item)):
            item.write_text(
                json.dumps({"parent": f"hbm:block/{bid}"}, indent=2) + "\n",
                encoding="utf-8",
            )
            REWIRED.append(f"item:{bid}")
    REWIRED.append(f"block:{bid}")


def remap_tex_ref(tv: str) -> str:
    tv = str(tv)
    if tv.startswith("hbm:blocks/"):
        return "hbm:block/" + tv[len("hbm:blocks/") :]
    if tv.startswith("hbm:items/"):
        return "hbm:item/" + tv[len("hbm:items/") :]
    if tv.startswith("blocks/"):
        if tv.startswith("blocks/gold_block") or tv.startswith("blocks/iron_block") or tv.startswith("blocks/"):
            # vanilla 1.12 blocks/foo → minecraft:block/foo if not hbm-prefixed
            stem = tv[len("blocks/") :]
            if (CE_BLOCK_TEX / f"{stem}.png").is_file() or (PORT_BLOCK_TEX / f"{stem}.png").is_file():
                return "hbm:block/" + stem
            return "minecraft:block/" + stem
    if tv.startswith("items/"):
        return "hbm:item/" + tv[len("items/") :]
    if tv.startswith("minecraft:blocks/"):
        return "minecraft:block/" + tv[len("minecraft:blocks/") :]
    if tv.startswith("minecraft:items/"):
        return "minecraft:item/" + tv[len("minecraft:items/") :]
    if tv.startswith("hbm:") and "/" not in tv.split(":", 1)[1]:
        return "hbm:block/" + tv.split(":", 1)[1]
    return tv


def copy_ce_png_for_ref(ref: str) -> bool:
    """If port png for this hbm ref is missing, copy from CE."""
    p = resolve_tex_ref(ref)
    if p is None or p.name == "__vanilla__" or p.is_file():
        return False
    # try CE mirrors
    rest = ref.split(":", 1)[-1] if ":" in ref else ref
    rest = rest.replace("blocks/", "").replace("block/", "").replace("items/", "").replace("item/", "")
    for src_root, dest_root in (
        (CE_BLOCK_TEX, PORT_BLOCK_TEX),
        (CE_ITEM_TEX, PORT_ITEM_TEX),
    ):
        src = src_root / f"{rest}.png"
        if src.is_file():
            return copy_png(src, dest_root / f"{rest}.png")
        # also try flatten
        src2 = src_root / f"{rest.replace('_', '.', 1)}.png"
        if src2.is_file():
            return copy_png(src2, dest_root / f"{src2.stem}.png")
    return False


def import_ce_block_model(name: str) -> bool:
    src_m = CE_BLOCK_MODEL / f"{name}.json"
    if not src_m.is_file():
        return False
    try:
        md = json.loads(src_m.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return False
    parent = str(md.get("parent") or "")
    if parent.startswith("block/"):
        md["parent"] = "minecraft:" + parent
    texs = md.get("textures") or {}
    for tk, tv in list(texs.items()):
        texs[tk] = remap_tex_ref(str(tv))
        copy_ce_png_for_ref(texs[tk])
    md["textures"] = texs
    dest_m = PORT_BLOCK_MODEL / f"{name}.json"
    dest_m.parent.mkdir(parents=True, exist_ok=True)
    dest_m.write_text(json.dumps(md, indent=2) + "\n", encoding="utf-8")
    COPIED.append(f"blockmodel {name}")
    return model_textures_ok(dest_m)


def import_ce_item_model(name: str) -> bool:
    if name in SKIP_GUN_MODELS:
        return False
    src = CE_ITEM_MODEL / f"{name}.json"
    dest = PORT_ITEM_MODEL / f"{name}.json"
    if dest.is_file() and is_gun_model(dest):
        return False
    if not src.is_file():
        return False
    try:
        md = json.loads(src.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return False
    parent = str(md.get("parent") or "")
    if parent.startswith("item/"):
        md["parent"] = "minecraft:" + parent
    if parent.startswith("hbm:block/") or parent.startswith("hbm:blocks/"):
        md["parent"] = "hbm:block/" + parent.split("/")[-1]
    texs = md.get("textures") or {}
    for tk, tv in list(texs.items()):
        texs[tk] = remap_tex_ref(str(tv))
        copy_ce_png_for_ref(texs[tk])
    if texs:
        md["textures"] = texs
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(md, indent=2) + "\n", encoding="utf-8")
    COPIED.append(f"itemmodel {name}")
    return model_textures_ok(dest)


def rewrite_obj_blockstate_to_cube(bid: str) -> bool:
    """If blockstate is forge OBJ/TESR but a cube model+png exist, point at the cube."""
    bs = PORT_BLOCKSTATE / f"{bid}.json"
    cube = PORT_BLOCK_MODEL / f"{bid}.json"
    if cube.is_file() and model_textures_ok(cube):
        bs.parent.mkdir(parents=True, exist_ok=True)
        bs.write_text(
            json.dumps({"variants": {"": {"model": f"hbm:block/{bid}"}}}, indent=2) + "\n",
            encoding="utf-8",
        )
        REWIRED.append(f"blockstate:{bid}")
        return True
    return False


def layer0_from_png(src: Path) -> str | None:
    try:
        if src.is_relative_to(CE_ITEM_TEX) or src.is_relative_to(PORT_ITEM_TEX):
            rel = src.stem if src.parent in {CE_ITEM_TEX, PORT_ITEM_TEX} else src.relative_to(
                CE_ITEM_TEX if src.is_relative_to(CE_ITEM_TEX) else PORT_ITEM_TEX
            ).as_posix()[:-4]
            dest = PORT_ITEM_TEX / f"{rel}.png"
            if src.is_relative_to(CE):
                copy_png(src, dest)
            return f"hbm:item/{rel}"
        if src.is_relative_to(CE_BLOCK_TEX) or src.is_relative_to(PORT_BLOCK_TEX):
            rel = src.relative_to(
                CE_BLOCK_TEX if src.is_relative_to(CE_BLOCK_TEX) else PORT_BLOCK_TEX
            ).as_posix()[:-4]
            dest = PORT_BLOCK_TEX / f"{rel}.png"
            if src.is_relative_to(CE):
                copy_png(src, dest)
            return f"hbm:block/{rel}"
        for root in CE_MODEL_TEX_ROOTS:
            if src.is_relative_to(root):
                rel = src.relative_to(CE / "textures" / "models").as_posix()[:-4]
                dest = PORT_MODEL_TEX / f"{rel}.png"
                copy_png(src, dest)
                return f"hbm:models/{rel}"
        if src.is_relative_to(PORT_MODEL_TEX):
            rel = src.relative_to(PORT_MODEL_TEX).as_posix()[:-4]
            return f"hbm:models/{rel}"
    except (ValueError, AttributeError):
        pass
    return None


def port_flatten_png(name: str) -> Path | None:
    for c in flatten_candidates(name):
        for folder in (PORT_ITEM_TEX, PORT_BLOCK_TEX):
            p = folder / f"{c}.png"
            if p.is_file():
                return p
    return None


def apply_explicit_or_special(iid: str) -> bool:
    """Same-object CE remaps that flatten() cannot see."""
    if iid in VANILLA_BLOCK:
        write_block_cube(iid, VANILLA_BLOCK[iid])
        return True
    if iid in EXPLICIT_CE:
        rel, layer = EXPLICIT_CE[iid]
        src = CE / "textures" / rel
        if src.is_file():
            dest = resolve_tex_ref(layer)
            if dest is not None and dest.name != "__vanilla__":
                copy_png(src, dest)
            write_item_model(iid, layer)
            if iid in {"vitrified_barrel", "tape_recorder", "pole_top", "dungeon_chain"}:
                write_block_cube(iid, layer)
            return True
    if iid in NUKE_CE:
        stem = NUKE_CE[iid]
        for folder, layer_pref, dest_root in (
            ("blocks", "hbm:block/", PORT_BLOCK_TEX),
            ("models/bombs", "hbm:models/bombs/", PORT_MODEL_TEX / "bombs"),
        ):
            src = CE / "textures" / folder / f"{stem}.png"
            if src.is_file():
                dest = dest_root / f"{stem}.png"
                copy_png(src, dest)
                layer = layer_pref + stem
                write_item_model(iid, layer)
                if folder == "blocks":
                    write_block_cube(iid, layer)
                return True
        return False
    if iid.startswith("machine_"):
        stem = iid[len("machine_") :]
        src = CE / "textures" / "models" / "machines" / f"{stem}.png"
        if src.is_file():
            dest = PORT_MODEL_TEX / "machines" / f"{stem}.png"
            copy_png(src, dest)
            write_item_model(iid, f"hbm:models/machines/{stem}")
            return True
    if iid.endswith("_stairs"):
        base = iid[: -len("_stairs")]
        # lightstone_bricks_stairs → lightstone.bricks
        tex = None
        for c in flatten_candidates(base):
            p = PORT_BLOCK_TEX / f"{c}.png"
            src = CE_BLOCK_TEX / f"{c}.png"
            if p.is_file():
                tex = f"hbm:block/{c}"
                break
            if src.is_file():
                copy_png(src, PORT_BLOCK_TEX / f"{c}.png")
                tex = f"hbm:block/{c}"
                break
        if tex:
            PORT_BLOCK_MODEL.mkdir(parents=True, exist_ok=True)
            (PORT_BLOCK_MODEL / f"{iid}.json").write_text(
                json.dumps(
                    {
                        "parent": "minecraft:block/stairs",
                        "textures": {"bottom": tex, "top": tex, "side": tex},
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )
            (PORT_BLOCKSTATE / f"{iid}.json").write_text(
                json.dumps({"variants": {"": {"model": f"hbm:block/{iid}"}}}, indent=2) + "\n",
                encoding="utf-8",
            )
            write_item_model(iid, tex)
            REWIRED.append(f"stairs:{iid}")
            return True
    if iid.startswith("turret_"):
        # CE inventory icons for turrets are block_steel (see turret_chekhov.json).
        src = CE_BLOCK_TEX / "block_steel.png"
        if src.is_file():
            copy_png(src, PORT_BLOCK_TEX / "block_steel.png")
            write_item_model(iid, "hbm:block/block_steel")
            return True
    if iid.startswith("rbmk_"):
        # CE rbmk textures live under textures/blocks/rbmk/
        for cand in (iid, iid + "_top", iid.replace("rbmk_", "rbmk/", 1), f"rbmk/{iid}_top"):
            src = CE_BLOCK_TEX / f"{cand}.png"
            if src.is_file():
                rel = Path(cand).as_posix()
                dest = PORT_BLOCK_TEX / f"{rel}.png"
                copy_png(src, dest)
                layer = f"hbm:block/{rel}"
                write_block_cube(iid, layer)
                return True
    return False


def fix_item(iid: str, ce_items: dict[str, Path], ce_models: dict[str, Path]) -> bool:
    if item_playable(iid):
        return False
    dest = PORT_ITEM_MODEL / f"{iid}.json"
    if dest.is_file() and is_gun_model(dest):
        return True  # already entity-rendered
    if apply_explicit_or_special(iid) and item_playable(iid):
        return True
    # 1) import CE item model
    if import_ce_item_model(iid) and item_playable(iid):
        return True
    # 2) parent to playable block
    if (PORT_BLOCK_MODEL / f"{iid}.json").is_file() and model_textures_ok(PORT_BLOCK_MODEL / f"{iid}.json"):
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(json.dumps({"parent": f"hbm:block/{iid}"}, indent=2) + "\n", encoding="utf-8")
        REWIRED.append(f"item:{iid}")
        return True
    # 3) rewrite layer0 to CE/port flatten png
    src = find_ce_png(iid, ce_items, ce_models) or port_flatten_png(iid)
    if src is None:
        return False
    layer = layer0_from_png(src)
    if not layer:
        return False
    write_item_model(iid, layer)
    return item_playable(iid)


def fix_block(bid: str, ce_blocks: dict[str, Path], ce_models: dict[str, Path]) -> bool:
    if block_playable(bid):
        return False
    if apply_explicit_or_special(bid) and block_playable(bid):
        return True
    # 1) rewrite OBJ/TESR blockstate onto existing playable cube
    if rewrite_obj_blockstate_to_cube(bid) and block_playable(bid):
        return True
    # 2) import CE block model
    if import_ce_block_model(bid) and model_textures_ok(PORT_BLOCK_MODEL / f"{bid}.json"):
        rewrite_obj_blockstate_to_cube(bid)
        if not (PORT_BLOCKSTATE / f"{bid}.json").is_file() or not block_playable(bid):
            (PORT_BLOCKSTATE / f"{bid}.json").write_text(
                json.dumps({"variants": {"": {"model": f"hbm:block/{bid}"}}}, indent=2) + "\n",
                encoding="utf-8",
            )
            REWIRED.append(f"blockstate:{bid}")
        if not item_playable(bid):
            fix_item(bid, {}, {})
        return block_playable(bid)
    # 3) copy CE blockstate only if it remaps to a JSON model we can import
    src_bs = CE_BLOCKSTATE / f"{bid}.json"
    if src_bs.is_file():
        try:
            data = json.loads(src_bs.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            data = {}
        models = iter_blockstate_models(data)
        imported = False
        for m in models:
            name = m.split(":")[-1].split("/")[-1]
            if name.endswith(".obj"):
                continue
            if import_ce_block_model(name):
                imported = True
        if imported and model_textures_ok(PORT_BLOCK_MODEL / f"{bid}.json"):
            (PORT_BLOCKSTATE / f"{bid}.json").write_text(
                json.dumps({"variants": {"": {"model": f"hbm:block/{bid}"}}}, indent=2) + "\n",
                encoding="utf-8",
            )
            REWIRED.append(f"blockstate:{bid}")
            return True
    # 4) cube from CE/port block png (same-id art only)
    src = find_ce_png(bid, ce_blocks) or port_flatten_png(bid)
    if src is not None:
        layer = layer0_from_png(src)
        if layer:
            write_block_cube(bid, layer)
            return block_playable(bid)
    # 5) TESR 3D skin → item icon only, leave world blockstate
    src3 = find_ce_png(bid, ce_models)
    if src3 is not None:
        layer = layer0_from_png(src3)
        if layer and not item_playable(bid):
            write_item_model(bid, layer)
        return False
    return False


def copy_missing_lang(items: set[str], blocks: set[str]) -> int:
    """Copy CE display names for leftover ids only (en_us)."""
    ce_en = CE / "lang" / "en_US.lang"
    port_en = PORT / "lang" / "en_us.json"
    if not ce_en.is_file() or not port_en.is_file():
        return 0
    try:
        port = json.loads(port_en.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return 0
    ce_map: dict[str, str] = {}
    for line in ce_en.read_text(encoding="utf-8", errors="ignore").splitlines():
        if "=" not in line or line.startswith("#"):
            continue
        k, v = line.split("=", 1)
        ce_map[k.strip()] = v.strip()
    added = 0
    for iid in items:
        key = f"item.hbm.{iid}"
        if key in port:
            continue
        for ck in (f"item.{iid}.name", f"item.{iid}", f"tile.{iid}.name"):
            if ck in ce_map:
                port[key] = ce_map[ck]
                added += 1
                break
    for bid in blocks:
        key = f"block.hbm.{bid}"
        if key in port:
            continue
        for ck in (f"tile.{bid}.name", f"tile.{bid}", f"item.{bid}.name"):
            if ck in ce_map:
                port[key] = ce_map[ck]
                added += 1
                break
    if added:
        port_en.write_text(json.dumps(port, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        COPIED.append(f"lang keys +{added}")
    return added


def classify_leftover(iid: str, kind: str, ce_items, ce_blocks, ce_models) -> str:
    if kind == "item":
        if find_ce_png(iid, ce_items, ce_models):
            return "CE png exists but model still unresolved"
        if (CE_ITEM_MODEL / f"{iid}.json").is_file():
            return "CE item model exists but textures unresolved"
        for root in CE_MODEL_TEX_ROOTS:
            if list(root.glob(f"{iid}*.png")):
                return f"CE 3D-only ({root.name}) — no inventory png"
        return "CE has no inventory/block png for this id"
    if find_ce_png(iid, ce_blocks, ce_models):
        return "CE png exists but blockstate/model still unresolved"
    if (CE_BLOCKSTATE / f"{iid}.json").is_file():
        return "CE blockstate exists (TESR/OBJ) but no cube/block png"
    if (CE_BLOCK_MODEL / f"{iid}.json").is_file():
        return "CE block model exists but textures unresolved"
    return "CE has no block png/blockstate cube for this id"


def main() -> int:
    load_skip_guns()
    items, blocks = registry_ids()
    print(f"registry items={len(items)} blocks={len(blocks)}")

    before_item_miss = sorted(i for i in items if not item_playable(i))
    before_block_miss = sorted(b for b in blocks if not block_playable(b))
    # Preserve first-wave baseline if we already started fixing this session.
    prev = ROOT / "docs" / "phase10" / "TEXTURE_MISS_AUDIT.json"
    orig_before_item = len(before_item_miss)
    orig_before_block = len(before_block_miss)
    if prev.is_file():
        try:
            old = json.loads(prev.read_text(encoding="utf-8"))
            orig_before_item = int(old.get("orig_before_item_miss") or old.get("before_item_miss") or orig_before_item)
            orig_before_block = int(old.get("orig_before_block_miss") or old.get("before_block_miss") or orig_before_block)
        except json.JSONDecodeError:
            pass
    print(f"BEFORE item miss={len(before_item_miss)}/{len(items)} (wave0={orig_before_item})")
    print(f"BEFORE block miss={len(before_block_miss)}/{len(blocks)} (wave0={orig_before_block})")

    ce_items = index_pngs(CE_ITEM_TEX)
    ce_blocks = index_pngs(CE_BLOCK_TEX)
    ce_models: dict[str, Path] = {}
    for root in CE_MODEL_TEX_ROOTS:
        ce_models.update(index_pngs(root))

    # copy missing textures already referenced by existing models of registered ids
    for iid in list(items):
        for mp in (PORT_ITEM_MODEL / f"{iid}.json", PORT_BLOCK_MODEL / f"{iid}.json"):
            if not mp.is_file():
                continue
            try:
                data = json.loads(mp.read_text(encoding="utf-8"))
            except json.JSONDecodeError:
                continue
            for tv in (data.get("textures") or {}).values():
                copy_ce_png_for_ref(str(tv))

    fixed_items = 0
    for iid in before_item_miss:
        if fix_item(iid, ce_items, ce_models):
            fixed_items += 1
    fixed_blocks = 0
    for bid in before_block_miss:
        if fix_block(bid, ce_blocks, ce_models):
            fixed_blocks += 1
    # second pass: items that parent to newly-fixed blocks
    for iid in list(items):
        if not item_playable(iid) and fix_item(iid, ce_items, ce_models):
            fixed_items += 1

    after_item_miss = sorted(i for i in items if not item_playable(i))
    after_block_miss = sorted(b for b in blocks if not block_playable(b))
    print(f"AFTER item miss={len(after_item_miss)}/{len(items)} fixed={fixed_items}")
    print(f"AFTER block miss={len(after_block_miss)}/{len(blocks)} fixed={fixed_blocks}")
    print(f"copied={len(COPIED)} rewired={len(REWIRED)}")

    copy_missing_lang(set(after_item_miss), set(after_block_miss))

    leftover_path = ROOT / "docs" / "phase10" / "LEFTOVER_MISSES.md"
    leftover_path.parent.mkdir(parents=True, exist_ok=True)
    item_reasons = defaultdict(list)
    for iid in after_item_miss:
        item_reasons[classify_leftover(iid, "item", ce_items, ce_blocks, ce_models)].append(iid)
    block_reasons = defaultdict(list)
    for bid in after_block_miss:
        block_reasons[classify_leftover(bid, "block", ce_items, ce_blocks, ce_models)].append(bid)

    lines = [
        "# Phase 10 leftover misses (strict texture/model)",
        "",
        "Registry vs CE png/json. Copied **only** missing CE assets. No invented art.",
        "Data-component register() strings (`heat`, `gun_states`, …) are not items and are excluded.",
        "",
        f"- Census: items **{len(items)}**, blocks **{len(blocks)}**",
        f"- BEFORE (strict, this wave): items **{orig_before_item}**, blocks **{orig_before_block}**",
        f"- AFTER: items **{len(after_item_miss)}**, blocks **{len(after_block_miss)}**",
        f"- Fixed this wave: items **{orig_before_item - len(after_item_miss)}**, blocks **{orig_before_block - len(after_block_miss)}**",
        "",
        "## Why leftover (true CE-missing or TESR)",
        "",
        "These ids have no CE inventory/block cube. Do **not** invent purple-black replacements.",
        "TESR/OBJ machines keep world TESR; inventory may use the same-object `textures/models/*` skin when CE has one.",
        "",
        "### Items",
        "",
    ]
    for reason, ids in sorted(item_reasons.items(), key=lambda x: (-len(x[1]), x[0])):
        lines.append(f"#### {reason} ({len(ids)})")
        lines.append("")
        for i in ids:
            lines.append(f"- `{i}`")
        lines.append("")
    lines += ["### Blocks", ""]
    for reason, ids in sorted(block_reasons.items(), key=lambda x: (-len(x[1]), x[0])):
        lines.append(f"#### {reason} ({len(ids)})")
        lines.append("")
        for i in ids:
            lines.append(f"- `{i}`")
        lines.append("")
    leftover_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    report = {
        "census_items": len(items),
        "census_blocks": len(blocks),
        "orig_before_item_miss": orig_before_item,
        "orig_before_block_miss": orig_before_block,
        "before_item_miss": orig_before_item,
        "before_block_miss": orig_before_block,
        "after_item_miss": len(after_item_miss),
        "after_block_miss": len(after_block_miss),
        "fixed_items": orig_before_item - len(after_item_miss),
        "fixed_blocks": orig_before_block - len(after_block_miss),
        "copied": len(COPIED),
        "rewired": len(REWIRED),
        "after_item_ids": after_item_miss,
        "after_block_ids": after_block_miss,
    }
    (ROOT / "docs" / "phase10" / "TEXTURE_MISS_AUDIT.json").write_text(
        json.dumps(report, indent=2) + "\n", encoding="utf-8"
    )
    print("wrote", leftover_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
