#!/usr/bin/env python3
"""Phase 10: copy CE 1.12 assets into this port's 1.21.1 resource layout.

CE is content truth. Byte-for-byte for binaries. JSON is rewritten only where 1.12
paths/parents actually break 1.21 (textures/blocks→block, textures/items→item,
blockstate model refs, `normal` variant, a handful of dead parents).

Does NOT vendor CE Java. Does NOT touch advancements/lang/manual/disks/optifine.
"""
from __future__ import annotations

import json
import re
import shutil
import sys
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
CE = REPO / "upstream" / "hbm-ce" / "src" / "main" / "resources" / "assets" / "hbm"
DST = REPO / "src" / "main" / "resources" / "assets" / "hbm"
PORT_SOUNDS_JAVA = REPO / "src" / "main" / "java" / "com" / "hbm" / "lib" / "HBMSoundHandler.java"

SKIP_TOP = {
    "advancements",
    "lang",
    "manual",
    "disks",
    "optifine",
}

# Already-ported JSON we must not clobber (weapon anims landed earlier).
PRESERVE_EXISTING_SUFFIXES = (
    "models/weapons/animations/",
)

PARENT_REMAP = {
    "block/half_slab": "minecraft:block/slab",
    "block/upper_slab": "minecraft:block/slab_top",
    "item/generated": "minecraft:item/generated",
    "item/handheld": "minecraft:item/handheld",
    "item/handheld_rod": "minecraft:item/handheld_rod",
    "builtin/generated": "minecraft:item/generated",
}

REG_RE = re.compile(r'reg\("([^"]+)"\)')


def snake_ce_id(s: str) -> str:
    """Same transform HBMSoundHandler used: '_' before each original uppercase, then lower."""
    out: list[str] = []
    for i, c in enumerate(s):
        if c.isupper() and i > 0 and s[i - 1] not in "._-/":
            out.append("_")
        out.append(c.lower())
    return "".join(out)


def remap_texture_path(value: str) -> str:
    if not isinstance(value, str) or value.startswith("#"):
        return value
    repl = (
        ("hbm:blocks/", "hbm:block/"),
        ("hbm:items/", "hbm:item/"),
        ("minecraft:blocks/", "minecraft:block/"),
        ("minecraft:items/", "minecraft:item/"),
        ("hbm:blocks\\", "hbm:block/"),
        ("hbm:items\\", "hbm:item/"),
    )
    for a, b in repl:
        if value.startswith(a) or a in value:
            value = value.replace(a, b)
    # un-namespaced 1.12 leftovers
    if value.startswith("blocks/"):
        value = "block/" + value[len("blocks/") :]
    if value.startswith("items/"):
        value = "item/" + value[len("items/") :]
    return value


def remap_model_ref(value: str) -> str:
    if not isinstance(value, str):
        return value
    if value == "forge:fluid":
        return "neoforge:fluid"
    value = remap_texture_path(value)
    if ":" in value:
        ns, path = value.split(":", 1)
        if "/" not in path:
            # 1.12 implicit models/block/
            return f"{ns}:block/{path}"
        if path.startswith("blocks/"):
            return f"{ns}:block/{path[len('blocks/'):]}"
        return f"{ns}:{path}"
    if "/" not in value:
        return f"minecraft:block/{value}"
    return value


def remap_parent(value: str) -> str:
    if not isinstance(value, str):
        return value
    if value in PARENT_REMAP:
        return PARENT_REMAP[value]
    if value.startswith("block/") and ":" not in value:
        return "minecraft:" + value
    if value.startswith("item/") and ":" not in value:
        return "minecraft:" + value
    return remap_model_ref(value) if value.startswith("hbm:") or value.startswith("minecraft:") else value


def rewrite_json_obj(obj):
    if isinstance(obj, dict):
        out = {}
        for k, v in obj.items():
            if k == "parent" and isinstance(v, str):
                out[k] = remap_parent(v)
            elif k == "model" and isinstance(v, str):
                out[k] = remap_model_ref(v)
            elif k == "textures" and isinstance(v, dict):
                out[k] = {tk: remap_texture_path(tv) if isinstance(tv, str) else rewrite_json_obj(tv) for tk, tv in v.items()}
            elif k == "particle" and isinstance(v, str):
                out[k] = remap_texture_path(v)
            else:
                out[k] = rewrite_json_obj(v)
        # blockstate: 1.12 `normal` → 1.21 empty variant; drop inventory (now item models)
        if "variants" in out and isinstance(out["variants"], dict):
            variants = {}
            for vk, vv in out["variants"].items():
                if vk == "inventory":
                    continue
                if vk == "normal":
                    variants[""] = vv
                    continue
                # 1.12 slabs: half=*,variant=* → 1.21 type=*
                if "half=" in vk and "variant=" in vk and vk.count("=") == 2:
                    props = dict(p.split("=", 1) for p in vk.split(","))
                    half = props.get("half")
                    if half in ("bottom", "top"):
                        variants[f"type={half}"] = vv
                        continue
                variants[vk] = vv
            out["variants"] = variants
        return out
    if isinstance(obj, list):
        return [rewrite_json_obj(v) for v in obj]
    if isinstance(obj, str):
        return remap_texture_path(obj)
    return obj


def load_json_lenient(text: str):
    text = text.strip()
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        # concatenated objects (CE foundry_channel.json) — take the first
        decoder = json.JSONDecoder()
        obj, _ = decoder.raw_decode(text)
        return obj


def dest_texture_rel(rel: Path) -> Path:
    parts = list(rel.parts)
    if parts[:2] == ["textures", "blocks"]:
        parts[1] = "block"
    elif parts[:2] == ["textures", "items"]:
        parts[1] = "item"
    return Path(*parts)


def should_preserve(dest: Path) -> bool:
    try:
        rel = dest.relative_to(DST).as_posix()
    except ValueError:
        return False
    return any(rel.startswith(p) for p in PRESERVE_EXISTING_SUFFIXES) and dest.exists()


def copy_file(src: Path, dest: Path, copied: Counter, skipped: Counter, kind: str) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if should_preserve(dest):
        skipped[kind] += 1
        return
    shutil.copy2(src, dest)
    copied[kind] += 1


def rewrite_mtl(text: str) -> str:
    return text.replace("hbm:blocks/", "hbm:block/").replace("hbm:items/", "hbm:item/")


def port_sound_ids() -> set[str]:
    text = PORT_SOUNDS_JAVA.read_text()
    return set(REG_RE.findall(text))


def convert_sounds_json(copied: Counter) -> dict:
    ce = json.loads((CE / "sounds.json").read_text())
    ids = port_sound_ids()
    out = {}
    mapped = 0
    orphan = 0
    mismatch = []
    for key, val in ce.items():
        new_key = snake_ce_id(key)
        if new_key not in ids:
            # some CE keys are already lower and match as-is
            if key in ids:
                new_key = key
            else:
                orphan += 1
                mismatch.append((key, new_key))
        else:
            mapped += 1
        entry = rewrite_json_obj(val)
        # 1.21 still accepts category; keep CE subtitle + sounds file refs (already lowercase)
        out[new_key] = entry
    dest = DST / "sounds.json"
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(out, indent=2, ensure_ascii=False) + "\n")
    copied["sounds.json"] += 1
    return {
        "ce_keys": len(ce),
        "out_keys": len(out),
        "mapped_to_handler": mapped,
        "keys_not_in_handler": orphan,
        "handler_ids": len(ids),
        "mismatch_sample": mismatch[:12],
        "handler_missing_from_json": sorted(ids - set(out))[:20],
        "handler_missing_count": len(ids - set(out)),
    }


def convert_json_file(src: Path, dest: Path, copied: Counter, skipped: Counter, kind: str) -> None:
    if should_preserve(dest):
        skipped[kind] += 1
        return
    try:
        obj = load_json_lenient(src.read_text(encoding="utf-8"))
    except Exception as exc:
        print(f"WARN skip bad json {src}: {exc}", file=sys.stderr)
        skipped["bad_json"] += 1
        return
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_text(json.dumps(rewrite_json_obj(obj), indent=2, ensure_ascii=False) + "\n")
    copied[kind] += 1


def census(root: Path) -> dict[str, int]:
    counts: dict[str, int] = Counter()
    if not root.exists():
        return counts
    for p in root.rglob("*"):
        if not p.is_file():
            continue
        suf = p.suffix.lower()
        counts[suf or "<none>"] += 1
        rel = p.relative_to(root).as_posix()
        if rel.startswith("textures/block/") or rel.startswith("textures/blocks/"):
            counts["tex_block"] += 1
        elif rel.startswith("textures/item/") or rel.startswith("textures/items/"):
            counts["tex_item"] += 1
        elif rel.startswith("textures/gui/"):
            counts["tex_gui"] += 1
        elif rel.startswith("textures/entity"):
            counts["tex_entity"] += 1
        elif rel.startswith("textures/armor/"):
            counts["tex_armor"] += 1
        elif rel.startswith("textures/particle"):
            counts["tex_particle"] += 1
        elif rel.startswith("textures/models/"):
            counts["tex_models"] += 1
        elif rel.startswith("sounds/") and suf == ".ogg":
            counts["ogg"] += 1
    counts["png"] = sum(1 for p in root.rglob("*.png") if p.is_file())
    counts["ogg_all"] = sum(1 for p in root.rglob("*.ogg") if p.is_file())
    counts["json"] = sum(1 for p in root.rglob("*.json") if p.is_file())
    counts["obj"] = sum(1 for p in root.rglob("*.obj") if p.is_file())
    counts["mcmeta"] = sum(1 for p in root.rglob("*.mcmeta") if p.is_file())
    return counts


def main() -> int:
    if not CE.is_dir():
        print(f"CE assets missing: {CE}", file=sys.stderr)
        return 2
    DST.mkdir(parents=True, exist_ok=True)

    before = census(DST)
    copied: Counter = Counter()
    skipped: Counter = Counter()

    for src in CE.rglob("*"):
        if not src.is_file():
            continue
        rel = src.relative_to(CE)
        top = rel.parts[0] if rel.parts else ""
        if top in SKIP_TOP:
            skipped[f"skip_{top}"] += 1
            continue

        # sounds.json handled separately (id remap)
        if rel.as_posix() == "sounds.json":
            continue

        suf = src.suffix.lower()

        if rel.parts[:1] == ("textures",):
            dest = DST / dest_texture_rel(rel)
            copy_file(src, dest, copied, skipped, "texture")
            continue

        if rel.parts[:1] == ("sounds",) or suf == ".ogg":
            dest = DST / rel
            copy_file(src, dest, copied, skipped, "ogg")
            continue

        if rel.parts[:1] == ("blockstates",) and suf == ".json":
            convert_json_file(src, DST / rel, copied, skipped, "blockstate")
            continue

        if rel.parts[:2] in (("models", "block"), ("models", "item")) and suf == ".json":
            convert_json_file(src, DST / rel, copied, skipped, "model_json")
            continue

        if suf == ".json":
            # other json (weapon anims, particles, etc.) — convert texture keys if any
            convert_json_file(src, DST / rel, copied, skipped, "other_json")
            continue

        if suf == ".mtl":
            dest = DST / rel
            if should_preserve(dest):
                skipped["mtl"] += 1
                continue
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_text(rewrite_mtl(src.read_text(encoding="utf-8", errors="replace")))
            copied["mtl"] += 1
            continue

        # obj / dae / png-in-models / shaders / leftover binaries
        dest = DST / rel
        copy_file(src, dest, copied, skipped, suf.lstrip(".") or "other")

    sound_stats = convert_sounds_json(copied)
    after = census(DST)

    report = {
        "ce_root": str(CE),
        "dst_root": str(DST),
        "copied": dict(copied),
        "skipped": dict(skipped),
        "port_before": dict(before),
        "port_after": dict(after),
        "sounds": sound_stats,
    }
    out_dir = REPO / "docs" / "phase10"
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / "CENSUS.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
