#!/usr/bin/env python3
"""Emit leftover CE assembler recipes: remaining named rows + fluid pack/unpack loop.

CE AssemblyMachineRecipes.java:1088-1097 (emptypackage already present;
pack/unpack per Fluids.getInNiceOrder() skipping hasNoContainer / NONE).

Does not overwrite existing assembler JSON. Does not invent recipes.
Skip list stays the genuine missing-id set (≤7) unless a new unresolved id appears.
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / "scripts"))
from phase11_machine_parts import (  # noqa: E402
    CE_ASS,
    DATA,
    known_ids,
    parse_all_recipes,
    write_assembler,
)

OUT = DATA / "recipe" / "assembler"
FLUIDS_JAVA = REPO / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "fluid" / "Fluids.java"

# Remaining assembler skips (PARITY_REPORT). Fluid-dict four now have JSON.
SKIP7 = {
    "ass.nitra",  # ChanceOutput — AssemblerRecipe is single ItemStack
    "ass.digimemer",  # commented Mekanism in CE
    "ass.50bmgbypass",  # black_diamond is ItemModHealth
}


def container_fluids() -> list[str]:
    text = FLUIDS_JAVA.read_text(errors="replace")
    names: list[str] = []
    for m in re.finditer(r'new FluidType\(\s*"([A-Z][A-Z0-9_]*)"', text):
        name = m.group(1)
        end = text.find(";", m.start())
        stmt = text[m.start() : end if end != -1 else m.start() + 400]
        if name == "NONE":
            continue
        if "NOCON" in stmt:
            continue
        names.append(name)
    return names


def write_pack_unpack() -> tuple[int, int]:
    """CE AssemblyMachineRecipes.java:1090-1097."""
    written = skipped = 0
    if "fluid_pack_empty" not in known_ids() or "fluid_pack_full" not in known_ids():
        print("drop pack/unpack: fluid_pack_empty/full unregistered")
        return 0, 0
    OUT.mkdir(parents=True, exist_ok=True)
    for name in container_fluids():
        slug = name.lower()
        pack = {
            "type": "hbm:assembler",
            "inputs": [{"item": {"item": "hbm:fluid_pack_empty"}, "count": 1}],
            "output": {"id": "hbm:fluid_pack_full", "count": 1},
            "duration": 40,
            "power": 100,
            "input_fluids": [{"type": name, "fill": 32000}],
        }
        unpack = {
            "type": "hbm:assembler",
            "inputs": [{"item": {"item": "hbm:fluid_pack_full"}, "count": 1}],
            "output": {"id": "hbm:fluid_pack_empty", "count": 1},
            "duration": 40,
            "power": 100,
            "output_fluids": [{"type": name, "fill": 32000}],
        }
        for slug_name, payload in ((f"package_{slug}", pack), (f"unpackage_{slug}", unpack)):
            path = OUT / f"{slug_name}.json"
            if path.exists():
                skipped += 1
                continue
            path.write_text(json.dumps(payload, indent=2) + "\n")
            written += 1
    return written, skipped


def list_missing_named() -> None:
    text = CE_ASS.read_text(errors="replace")
    recs = parse_all_recipes(text)
    existing = {p.stem for p in OUT.glob("*.json")}
    missing = []
    for r in recs:
        slug = r["name"].replace("ass.", "").lower()
        if slug in existing:
            continue
        missing.append(r["name"])
    print(f"named CE assembler={len(recs)} json={len(existing)} named_missing={len(missing)}")
    for n in missing[:40]:
        print(f"  missing {n}")
    if len(missing) > 40:
        print(f"  ... +{len(missing) - 40} more")


def main() -> None:
    list_missing_named()
    kn = known_ids()
    ok, skip, reasons = write_assembler(kn)
    print(f"named write_assembler written={ok} skipped={skip} reasons={reasons}")
    pw, ps = write_pack_unpack()
    print(f"pack/unpack written={pw} skipped_exists={ps} fluids={len(container_fluids())}")
    print(f"assembler json now={len(list(OUT.glob('*.json')))}")


if __name__ == "__main__":
    main()
