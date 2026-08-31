#!/usr/bin/env python3
"""Copy CE textures + emit models/blockstates/assembler JSON/lang for wave-3 machines."""
from __future__ import annotations

import json
import shutil
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
CE = REPO / "upstream" / "hbm-ce" / "src" / "main" / "resources" / "assets" / "hbm"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "hbm"
DATA = REPO / "src" / "main" / "resources" / "data" / "hbm"
LANG = ASSETS / "lang" / "en_us.json"

ITEMS = [
    "dust", "solid_fuel", "solid_fuel_bf", "cordite", "ball_tnt", "ball_dynamite",
    "ball_tatb", "rocket_fuel", "powder_sawdust", "gem_tantalium", "canister_napalm",
    "bio_wafer", "part_lithium", "part_beryllium", "part_carbon", "part_copper",
    "part_plutonium", "particle_empty", "particle_hydrogen", "particle_copper",
    "particle_lead", "particle_amat", "particle_aschrab", "particle_dark",
    "particle_higgs", "particle_tachyon", "particle_strange", "particle_sparkticle",
]
TAR = ["crude", "crack", "coal", "wood", "wax", "paraffin"]
BLOCKS = [
    "machine_solidifier", "machine_fel", "machine_excavator",
    "pa_beamline", "pa_rfc", "pa_quadrupole", "pa_dipole", "pa_source", "pa_detector",
]


def copy_tex(src_name: str, dest_stem: str) -> None:
    src = CE / "textures" / "items" / src_name
    dest = ASSETS / "textures" / "item" / f"{dest_stem}.png"
    dest.parent.mkdir(parents=True, exist_ok=True)
    if src.exists() and not dest.exists():
        shutil.copy2(src, dest)


def item_model(name: str, tex: str | None = None) -> None:
    p = ASSETS / "models" / "item" / f"{name}.json"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"hbm:item/{tex or name}"},
    }, indent=2) + "\n")


def cube_block(name: str) -> None:
    (ASSETS / "models" / "block").mkdir(parents=True, exist_ok=True)
    (ASSETS / "models" / "item").mkdir(parents=True, exist_ok=True)
    (ASSETS / "blockstates").mkdir(parents=True, exist_ok=True)
    (ASSETS / "models" / "block" / f"{name}.json").write_text(json.dumps({
        "parent": "minecraft:block/cube_all",
        "textures": {"all": "hbm:block/block_steel"},
    }, indent=2) + "\n")
    (ASSETS / "models" / "item" / f"{name}.json").write_text(json.dumps({
        "parent": f"hbm:block/{name}",
    }, indent=2) + "\n")
    if name == "machine_fel":
        variants = {f"facing={d}": {"model": f"hbm:block/{name}", "y": y}
                    for d, y in (("north", 0), ("south", 180), ("west", 270), ("east", 90))}
        (ASSETS / "blockstates" / f"{name}.json").write_text(json.dumps({"variants": variants}, indent=2) + "\n")
    else:
        (ASSETS / "blockstates" / f"{name}.json").write_text(json.dumps({
            "variants": {"": {"model": f"hbm:block/{name}"}}
        }, indent=2) + "\n")


def assembler(slug: str, inputs: list[tuple], output: str, duration: int, power: int = 100, out_count: int = 1) -> None:
    ents = []
    for kind, id_, count in inputs:
        if kind == "tag":
            ents.append({"item": {"tag": id_}, "count": count})
        else:
            ents.append({"item": {"item": id_}, "count": count})
    p = DATA / "recipe" / "assembler" / f"{slug}.json"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({
        "type": "hbm:assembler",
        "inputs": ents,
        "output": {"id": output, "count": out_count},
        "duration": duration,
        "power": power,
    }, indent=2) + "\n")


def main() -> None:
    for name in ITEMS:
        copy_tex(f"{name}.png", name)
        item_model(name)
    for t in TAR:
        dest = f"oil_tar_{t}"
        copy_tex(f"oil_tar.{t}.png", dest)
        item_model(dest)
    for b in BLOCKS:
        cube_block(b)

    tag = DATA / "tags" / "item" / "any_tar.json"
    tag.parent.mkdir(parents=True, exist_ok=True)
    tag.write_text(json.dumps({
        "replace": False,
        "values": [f"hbm:oil_tar_{t}" for t in TAR],
    }, indent=2) + "\n")

    # CE AssemblyMachineRecipes.java:314-336, 317-318, 329-336, 462-484, 151-160
    assembler("liquefactor", [
        ("item", "hbm:steel_shell", 4),
        ("item", "hbm:plate_copper", 12),
        ("tag", "hbm:any_tar", 4),
        ("item", "hbm:circuit_capacitor", 12),
        ("item", "hbm:coil_tungsten", 8),
    ], "hbm:machine_liquefactor", 200)
    assembler("solidifier", [
        ("item", "hbm:steel_shell", 4),
        ("item", "hbm:plate_aluminium", 12),
        ("item", "hbm:ingot_polymer", 4),
        ("item", "hbm:circuit_capacitor", 12),
        ("item", "hbm:coil_copper", 4),
    ], "hbm:machine_solidifier", 200)
    assembler("fel", [
        ("item", "hbm:battery_lithium_pack", 1),
        ("item", "hbm:gold_dense_wire", 64),
        ("item", "hbm:steel_plate_triple", 12),
        ("item", "hbm:ingot_polymer", 16),
        ("item", "hbm:part_generic_glass_polarized", 4),
        ("item", "hbm:circuit_capacitor", 16),
        ("item", "hbm:circuit_basic", 4),
    ], "hbm:machine_fel", 400)
    assembler("excavator", [
        ("item", "minecraft:stone_bricks", 8),
        ("item", "hbm:ingot_steel", 8),
        ("item", "minecraft:iron_ingot", 8),
        ("item", "hbm:motor", 2),
        ("item", "hbm:circuit_analog", 1),
    ], "hbm:machine_excavator", 200)
    assembler("beamline", [
        ("item", "hbm:steel_plate_triple", 8),
        ("item", "hbm:plate_copper", 16),
        ("item", "hbm:gold_dense_wire", 4),
    ], "hbm:pa_beamline", 200)
    assembler("rfc", [
        ("item", "hbm:pa_beamline", 3),
        ("item", "hbm:steel_plate_triple", 16),
        ("item", "hbm:plate_copper", 64),
        ("item", "hbm:ingot_pc", 16),
        ("item", "hbm:magnetron", 16),
    ], "hbm:pa_rfc", 400)
    assembler("quadrupole", [
        ("item", "hbm:pa_beamline", 1),
        ("item", "hbm:steel_plate_triple", 16),
        ("item", "hbm:ingot_pc", 16),
        ("item", "hbm:circuit_bismoid", 1),
    ], "hbm:pa_quadrupole", 400)
    assembler("dipole", [
        ("item", "hbm:pa_beamline", 2),
        ("item", "hbm:steel_plate_triple", 16),
        ("item", "hbm:ingot_pc", 32),
        ("item", "hbm:circuit_bismoid", 4),
    ], "hbm:pa_dipole", 400)
    assembler("source", [
        ("item", "hbm:pa_beamline", 3),
        ("item", "hbm:steel_plate_triple", 16),
        ("item", "hbm:ingot_pc", 16),
        ("item", "hbm:magnetron", 16),
        ("item", "hbm:circuit_quantum", 1),
    ], "hbm:pa_source", 400)
    assembler("detector", [
        ("item", "hbm:pa_beamline", 3),
        ("item", "hbm:steel_plate_triple", 24),
        ("item", "hbm:gold_dense_wire", 16),
        ("item", "hbm:ingot_pc", 16),
        ("item", "hbm:circuit_quantum", 4),
    ], "hbm:pa_detector", 400)
    assembler("partlith", [("item", "hbm:powder_lithium", 1)], "hbm:part_lithium", 40, out_count=8)
    assembler("partberyl", [("item", "hbm:powder_beryllium", 1)], "hbm:part_beryllium", 40, out_count=8)
    assembler("partcoal", [("item", "hbm:powder_coal", 1)], "hbm:part_carbon", 40, out_count=8)
    assembler("partcop", [("item", "hbm:powder_copper", 1)], "hbm:part_copper", 40, out_count=8)
    assembler("partplut", [("item", "hbm:powder_plutonium", 1)], "hbm:part_plutonium", 40, out_count=8)

    lang = json.loads(LANG.read_text())
    extras = {
        "item.hbm.oil_tar_crude": "Oil Tar",
        "item.hbm.oil_tar_crack": "Crack Oil Tar",
        "item.hbm.oil_tar_coal": "Coal Tar",
        "item.hbm.oil_tar_wood": "Wood Tar",
        "item.hbm.oil_tar_wax": "Chlorinated Petroleum Wax",
        "item.hbm.oil_tar_paraffin": "Paraffin Wax",
        "container.machineSolidifier": "Industrial Solidification Machine",
        "container.machineExcavator": "Large Mining Drill",
        "container.paDetector": "Particle Detector",
        "container.paRFC": "RF Cavity",
        "container.paQuadrupole": "Quadrupole Magnet",
        "container.paDipole": "Dipole Magnet",
        "container.paSource": "Particle Source",
        "container.paBeamline": "Beamline",
    }
    lang.update(extras)
    LANG.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n")
    print("wave3 assets ok")


if __name__ == "__main__":
    main()
