#!/usr/bin/env python3
"""Emit item models + lang aliases + coil/motor crafting + assembler recipes.

CE cites:
  ModItems.java:396 / 1230 / 1280 / 1298-1341 / 1847 / 1871-1873 / 2520
  CraftingManager.java:205-221 (coil/motor craft)
  AssemblyMachineRecipes.java (all GenericRecipe sections)
"""
from __future__ import annotations

import json
import re
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "hbm"
DATA = REPO / "src" / "main" / "resources" / "data" / "hbm"
CE_ASS = REPO / "upstream" / "hbm-ce" / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "recipes" / "AssemblyMachineRecipes.java"
LANG = ASSETS / "lang" / "en_us.json"
GEN_LANG = REPO / "src" / "generated" / "resources" / "assets" / "hbm" / "lang" / "en_us.json"

CIRCUITS = [
    "vacuum_tube", "capacitor", "capacitor_tantalium", "pcb", "silicon", "chip",
    "chip_bismoid", "analog", "basic", "advanced", "capacitor_board", "bismoid",
    "controller_chassis", "controller", "controller_advanced", "quantum",
    "chip_quantum", "controller_quantum", "atomic_clock", "numitron",
]

PARTS = [
    "motor", "motor_desh", "motor_bismuth",
    "coil_copper", "coil_copper_torus", "coil_tungsten", "coil_gold",
    "coil_gold_torus", "coil_magnetized_tungsten",
    "centrifuge_element", "thermo_element", "rtg_unit", "drill_titanium",
    "canister_empty", "turbine_titanium", "turbine_tungsten", "magnetron",
    "crt_display", "sphere_steel", "flywheel_beryllium", "reactor_core",
    "hazmat_cloth", "hazmat_cloth_red", "hazmat_cloth_grey", "asbestos_cloth",
    "filter_coal",
]

# CE DictFrame symbol → candidate material tokens (autogen + hand-coded)
MAT: dict[str, list[str]] = {
    "STEEL": ["steel"],
    "CU": ["copper"],
    "TI": ["titanium"],
    "AL": ["aluminium", "aluminum"],
    "PB": ["lead"],
    "GOLD": ["gold"],
    "IRON": ["iron"],
    "W": ["tungsten"],
    "DESH": ["desh", "workersalloy"],
    "DURA": ["dura_steel", "durasteel"],
    "NB": ["niobium"],
    "RUBBER": ["rubber"],
    "MINGRADE": ["mingrade", "red_copper"],
    "MAGTUNG": ["magnetized_tungsten", "magnetizedtungsten"],
    "ZR": ["zirconium"],
    "ANY_PLASTIC": ["polymer", "bakelite"],
    "ANY_HARDPLASTIC": ["pc", "polycarbonate", "pvc"],
    "ANY_RUBBER": ["rubber"],
    "ANY_RESISTANTALLOY": ["tcalloy", "cdalloy"],
    "ANY_CONCRETE": ["concrete_smooth"],
    "ANY_TAR": [],
    "ANY_BISMOID": ["bismuth"],
    "ANY_BISMOIDBRONZE": ["bismuthbronze"],
    "B": ["boron"],
    "BI": ["bismuth"],
    "BE": ["beryllium"],
    "FERRO": ["ferrouranium"],
    "STAR": ["starmetal"],
    "BIGMT": ["saturnite"],
    "DIAMOND": ["diamond"],
    "SA326": ["schrabidium"],
    "CMB": ["combine_steel", "cmbsteel", "cmb"],
    "GUNMETAL": ["gunmetal"],
    "WEAPONSTEEL": ["weaponsteel"],
    "BSCCO": ["bscco"],
    "ASBESTOS": ["asbestos"],
    "COAL": ["coal"],
    "NETHERQUARTZ": ["netherquartz", "quartz"],
    "SI": ["silicon"],
    "SR": ["strontium"],
    "CS137": ["caesium137", "cs137"],
    "U238": ["uranium238", "u238"],
    "KEY_RED": [],
}

SHAPE_CANDIDATES = {
    "ingot": ["ingot_{m}", "{m}_ingot"],
    "plate": ["plate_{m}", "{m}_plate"],
    "plateCast": ["{m}_plate_triple", "plate_cast_{m}", "plate_{m}"],
    "plateWelded": ["{m}_plate_sextuple", "plate_welded_{m}", "plate_{m}"],
    "plate528": ["plate_{m}", "{m}_plate", "{m}_plate_triple"],
    "pipe": ["{m}_pipe", "pipe_{m}"],
    "shell": ["{m}_shell", "shell_{m}"],
    "wireFine": ["{m}_wire", "wire_{m}", "wire_fine_{m}"],
    "wireDense": ["{m}_dense_wire", "wire_dense_{m}", "{m}_wire"],
    "bolt": ["{m}_bolt", "bolt_{m}"],
    "dust": ["powder_{m}", "{m}_dust", "dust_{m}"],
    "nugget": ["nugget_{m}", "{m}_nugget"],
    "billet": ["billet_{m}", "{m}_billet"],
    "any": ["{m}"],
}

ITEM_MAP = {
    "ModItems.motor": "motor",
    "ModItems.motor_desh": "motor_desh",
    "ModItems.motor_bismuth": "motor_bismuth",
    "ModItems.coil_tungsten": "coil_tungsten",
    "ModItems.coil_copper": "coil_copper",
    "ModItems.coil_copper_torus": "coil_copper_torus",
    "ModItems.coil_gold_torus": "coil_gold_torus",
    "ModItems.coil_gold": "coil_gold",
    "ModItems.coil_magnetized_tungsten": "coil_magnetized_tungsten",
    "ModItems.plate_polymer": "plate_polymer",
    "ModItems.plate_desh": "plate_desh",
    "ModItems.plate_iron": "plate_iron",
    "ModItems.plate_gold": "plate_gold",
    "ModItems.plate_titanium": "plate_titanium",
    "ModItems.plate_aluminium": "plate_aluminium",
    "ModItems.plate_steel": "plate_steel",
    "ModItems.plate_lead": "plate_lead",
    "ModItems.plate_copper": "plate_copper",
    "ModItems.plate_schrabidium": "plate_schrabidium",
    "ModItems.plate_combine_steel": "plate_combine_steel",
    "ModItems.plate_gunmetal": "plate_gunmetal",
    "ModItems.plate_weaponsteel": "plate_weaponsteel",
    "ModItems.plate_saturnite": "plate_saturnite",
    "ModItems.plate_dura_steel": "plate_dura_steel",
    "ModItems.plate_mixed": "plate_mixed",
    "ModItems.plate_dalekanium": "plate_dalekanium",
    "ModItems.plate_bismuth": "plate_bismuth",
    "ModItems.centrifuge_element": "centrifuge_element",
    "ModItems.thermo_element": "thermo_element",
    "ModItems.rtg_unit": "rtg_unit",
    "ModItems.drill_titanium": "drill_titanium",
    "ModItems.ingot_firebrick": "ingot_firebrick",
    "ModItems.ingot_cft": "ingot_cft",
    "ModItems.sphere_steel": "sphere_steel",
    "ModItems.magnetron": "magnetron",
    "ModItems.crt_display": "crt_display",
    "ModItems.turbine_tungsten": "turbine_tungsten",
    "ModItems.turbine_titanium": "turbine_titanium",
    "ModItems.flywheel_beryllium": "flywheel_beryllium",
    "ModItems.reactor_core": "reactor_core",
    "ModItems.canister_empty": "canister_empty",
    "ModItems.hazmat_cloth": "hazmat_cloth",
    "ModItems.asbestos_cloth": "asbestos_cloth",
    "ModItems.filter_coal": "filter_coal",
    "ModItems.nugget_bismuth": "nugget_bismuth",
    "ModBlocks.machine_turbinegas": "machine_turbine_gas",
    "ModBlocks.machine_rtg_grey": "machine_rtg_grey",
    "ModBlocks.machine_assembly_machine": "machine_assembly_machine",
    "ModBlocks.concrete_smooth": "concrete_smooth",
    "ModBlocks.steel_scaffold": "steel_scaffold",
    "ModBlocks.glass_quartz": "glass_quartz",
    "ModBlocks.block_meteor": "block_meteor",
}

CIRCUIT_ENUM = {
    "VACUUM_TUBE": "circuit_vacuum_tube",
    "CAPACITOR": "circuit_capacitor",
    "CAPACITOR_TANTALIUM": "circuit_capacitor_tantalium",
    "PCB": "circuit_pcb",
    "SILICON": "circuit_silicon",
    "CHIP": "circuit_chip",
    "CHIP_BISMOID": "circuit_chip_bismoid",
    "ANALOG": "circuit_analog",
    "BASIC": "circuit_basic",
    "ADVANCED": "circuit_advanced",
    "CAPACITOR_BOARD": "circuit_capacitor_board",
    "BISMOID": "circuit_bismoid",
    "CONTROLLER_CHASSIS": "circuit_controller_chassis",
    "CONTROLLER": "circuit_controller",
    "CONTROLLER_ADVANCED": "circuit_controller_advanced",
    "QUANTUM": "circuit_quantum",
    "CHIP_QUANTUM": "circuit_chip_quantum",
    "CONTROLLER_QUANTUM": "circuit_controller_quantum",
    "ATOMIC_CLOCK": "circuit_atomic_clock",
    "NUMITRON": "circuit_numitron",
}

VANILLA_ITEMS = {
    "DIAMOND": "minecraft:diamond",
    "STRING": "minecraft:string",
    "PAPER": "minecraft:paper",
    "IRON_INGOT": "minecraft:iron_ingot",
    "GOLD_INGOT": "minecraft:gold_ingot",
    "REDSTONE": "minecraft:redstone",
}

VANILLA_BLOCKS = {
    "STONEBRICK": "minecraft:stone_bricks",
    "FURNACE": "minecraft:furnace",
    "HOPPER": "minecraft:hopper",
    "CRAFTING_TABLE": "minecraft:crafting_table",
    "IRON_BLOCK": "minecraft:iron_block",
    "GOLD_BLOCK": "minecraft:gold_block",
}


def known_ids() -> set[str]:
    import sys
    sys.path.insert(0, str(REPO / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, blocks = extract_all_ids()
    extra = set()
    java = REPO / "src" / "main" / "java" / "com" / "hbm"
    helper = re.compile(
        r'(?:registerParts|registerIngot|registerNugget|registerLoreIngot|registerItem|'
        r'registerBlock|reg|registerPowder|registerFuelPowder)\(\s*"([a-z][a-z0-9_]*)"'
    )
    for p in java.rglob("*.java"):
        extra.update(helper.findall(p.read_text(errors="ignore")))
    return items | blocks | extra | set(PARTS) | {f"circuit_{c}" for c in CIRCUITS}


def write_models() -> None:
    models = ASSETS / "models" / "item"
    models.mkdir(parents=True, exist_ok=True)
    tex_root = ASSETS / "textures" / "item"
    for c in CIRCUITS:
        dotted = f"circuit.{c}"
        tex = dotted if (tex_root / f"{dotted}.png").exists() else f"circuit_{c}"
        (models / f"circuit_{c}.json").write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"hbm:item/{tex}"},
        }, indent=2) + "\n")
    for p in PARTS:
        path = models / f"{p}.json"
        if path.exists():
            continue
        tex = p
        if not (tex_root / f"{p}.png").exists():
            alt = f"{p}_alt"
            if (tex_root / f"{alt}.png").exists():
                tex = alt
            else:
                continue
        path.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": f"hbm:item/{tex}"},
        }, indent=2) + "\n")
    # magnetron only ships magnetron_alt.png
    mag = models / "magnetron.json"
    if not mag.exists() and (tex_root / "magnetron_alt.png").exists():
        mag.write_text(json.dumps({
            "parent": "minecraft:item/generated",
            "textures": {"layer0": "hbm:item/magnetron_alt"},
        }, indent=2) + "\n")


def patch_lang() -> None:
    for path in (LANG, GEN_LANG):
        if not path.exists():
            continue
        data = json.loads(path.read_text())
        for c in CIRCUITS:
            src = f"item.hbm.circuit.{c}"
            dst = f"item.hbm.circuit_{c}"
            if src in data and dst not in data:
                data[dst] = data[src]
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def shaped(path: str, result: str, count: int, pattern: list[str], keys: dict, unlocked: str) -> None:
    p = DATA / "recipe" / path
    p.parent.mkdir(parents=True, exist_ok=True)
    key = {}
    for k, v in keys.items():
        if v.startswith("tag:"):
            key[k] = {"tag": v[4:]}
        else:
            key[k] = {"item": v if ":" in v else f"hbm:{v}"}
    out = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": pattern,
        "key": key,
        "result": {"id": result if ":" in result else f"hbm:{result}", "count": count},
    }
    p.write_text(json.dumps(out, indent=2) + "\n")


def write_crafting() -> None:
    # CE CraftingManager.java:205-221
    shaped("parts/coil_copper.json", "coil_copper", 1,
           ["WWW", "WIW", "WWW"],
           {"W": "mingrade_wire", "I": "minecraft:iron_ingot"},
           "mingrade_wire")
    shaped("parts/coil_copper_from_steel.json", "coil_copper", 1,
           ["WWW", "WIW", "WWW"],
           {"W": "mingrade_wire", "I": "ingot_steel"},
           "mingrade_wire")
    shaped("parts/coil_copper_torus.json", "coil_copper_torus", 2,
           [" C ", "CPC", " C "],
           {"C": "coil_copper", "P": "plate_iron"},
           "coil_copper")
    shaped("parts/coil_copper_torus_steel.json", "coil_copper_torus", 2,
           [" C ", "CPC", " C "],
           {"C": "coil_copper", "P": "plate_steel"},
           "coil_copper")
    shaped("parts/coil_tungsten.json", "coil_tungsten", 1,
           ["WWW", "WIW", "WWW"],
           {"W": "tungsten_wire", "I": "minecraft:iron_ingot"},
           "tungsten_wire")
    shaped("parts/coil_tungsten_steel.json", "coil_tungsten", 1,
           ["WWW", "WIW", "WWW"],
           {"W": "tungsten_wire", "I": "ingot_steel"},
           "tungsten_wire")
    shaped("parts/motor.json", "motor", 2,
           [" R ", "ICI", "ITI"],
           {"R": "mingrade_wire", "T": "coil_copper_torus", "I": "plate_iron", "C": "coil_copper"},
           "coil_copper")
    shaped("parts/motor_steel.json", "motor", 2,
           [" R ", "ICI", " T "],
           {"R": "mingrade_wire", "T": "coil_copper_torus", "I": "plate_steel", "C": "coil_copper"},
           "coil_copper")
    shaped("parts/motor_desh.json", "motor_desh", 1,
           ["PCP", "DMD", "PCP"],
           {"P": "ingot_polymer", "C": "gold_dense_wire", "D": "ingot_desh", "M": "motor"},
           "motor")


def _first_known(cands: list[str], known: set[str]) -> str | None:
    for c in cands:
        if c in known:
            return c
    return None


def resolve_ore(mat: str, shape: str, n: int, known: set[str]) -> tuple[str, int] | None:
    if mat == "KEY_RED":
        return "minecraft:redstone", n
    if mat == "ANY_CONCRETE":
        if "concrete_smooth" in known:
            return "hbm:concrete_smooth", n
        return None
    tokens = MAT.get(mat)
    if tokens is None:
        tokens = [mat.lower()]
    tmpls = SHAPE_CANDIDATES.get(shape, ["{m}_" + shape, shape + "_{m}"])
    cands: list[str] = []
    for tok in tokens:
        for tmpl in tmpls:
            cands.append(tmpl.format(m=tok))
    if mat == "MINGRADE" and shape == "wireFine":
        cands.extend(["mingrade_wire", "red_copper_wire"])
    if mat == "IRON" and shape == "ingot":
        return "minecraft:iron_ingot", n
    if mat == "GOLD" and shape == "ingot":
        return "minecraft:gold_ingot", n
    if mat == "DIAMOND" and shape == "dust":
        cands.extend(["powder_diamond", "diamond_dust"])
    hit = _first_known(cands, known)
    return (f"hbm:{hit}", n) if hit else None


def resolve_named(src: str, name: str, n: int, known: set[str]) -> tuple[str, int] | None:
    if src == "Blocks":
        vid = VANILLA_BLOCKS.get(name)
        return (vid, n) if vid else None
    if src == "Items":
        vid = VANILLA_ITEMS.get(name)
        return (vid, n) if vid else None
    key = f"{src}.{name}"
    mapped = ITEM_MAP.get(key, name)
    if mapped.startswith("minecraft:"):
        return mapped, n
    if mapped in known:
        return f"hbm:{mapped}", n
    # CE machine_turbinegas vs port machine_turbine_gas already in ITEM_MAP
    snake = name.lower()
    if snake in known:
        return f"hbm:{snake}", n
    return None


def resolve_flatten(item_name: str, enum_name: str, n: int, known: set[str]) -> tuple[str, int] | None:
    en = enum_name.lower()
    if item_name == "circuit":
        cid = CIRCUIT_ENUM.get(enum_name)
        if cid and cid in known:
            return f"hbm:{cid}", n
        cand = f"circuit_{en}"
        return (f"hbm:{cand}", n) if cand in known else None
    if item_name == "drillbit":
        cand = f"drillbit_{en}"
        return (f"hbm:{cand}", n) if cand in known else None
    if item_name == "piston_set":
        cand = f"piston_set_{en}"
        return (f"hbm:{cand}", n) if cand in known else None
    if item_name == "battery_pack":
        cand = f"{en}_pack"
        return (f"hbm:{cand}", n) if cand in known else None
    # item_expensive / part_generic deliberately unregistered
    return None


def resolve_stack(expr: str, known: set[str]) -> tuple[str, int] | None:
    expr = expr.strip()
    expr = expr.replace("RecipesCommon.", "")

    if "Fluids." in expr or "getDict(" in expr or "inputFluids" in expr:
        return None
    if "OreDictManager.getReflector" in expr:
        for cand in ("neutron_reflector", "plate_paa"):
            if cand in known:
                return f"hbm:{cand}", 1
        return None

    m = re.search(r"EnumCircuitType\.(\w+)", expr)
    if m and ("circuit" in expr or "DictFrame.fromOne" in expr):
        n = 1
        nm = re.search(r",\s*(\d+)\s*\)\s*$", expr)
        if nm:
            n = int(nm.group(1))
        cid = CIRCUIT_ENUM.get(m.group(1))
        if cid and cid in known:
            return f"hbm:{cid}", n
        return None

    m = re.match(
        r"new (?:ItemStack|ComparableStack)\((ModBlocks|ModItems|Blocks|Items)\.(\w+)\s*,\s*(\d+)\s*,\s*(?:[\w.]+\.)?(\w+)(?:\.ordinal\(\))?\)",
        expr,
    )
    if m:
        src, name, n, en = m.group(1), m.group(2), int(m.group(3)), m.group(4)
        if src == "ModItems":
            flat = resolve_flatten(name, en, n, known)
            if flat:
                return flat
        return None

    m = re.match(
        r"new (?:ItemStack|ComparableStack)\((ModBlocks|ModItems|Blocks|Items)\.(\w+)(?:,\s*(\d+))?\)",
        expr,
    )
    if m:
        return resolve_named(m.group(1), m.group(2), int(m.group(3) or 1), known)

    m = re.match(r"new OreDictStack\((\w+)\.(\w+)\(\)(?:,\s*(\d+))?\)", expr)
    if m:
        return resolve_ore(m.group(1), m.group(2), int(m.group(3) or 1), known)

    m = re.match(r"new ComparableStack\((ModItems|ModBlocks)\.(\w+)(?:,\s*(\d+))?\)", expr)
    if m:
        return resolve_named(m.group(1), m.group(2), int(m.group(3) or 1), known)

    return None


def _take_call_args(s: str, start: int) -> tuple[str, int] | None:
    if start >= len(s) or s[start] != "(":
        return None
    depth = 0
    for i in range(start, len(s)):
        if s[i] == "(":
            depth += 1
        elif s[i] == ")":
            depth -= 1
            if depth == 0:
                return s[start + 1:i], i + 1
    return None


def parse_all_recipes(text: str) -> list[dict]:
    recipes = []
    for rm in re.finditer(
        r'this\.register\(new GenericRecipe\("([^"]+)"\)\.setup(?:Named)?\((\d[\d_]*)\s*,\s*(\d[\d_]*)\)',
        text,
    ):
        name = rm.group(1)
        dur = int(rm.group(2).replace("_", ""))
        pow_ = int(rm.group(3).replace("_", ""))
        rest = text[rm.end():]
        endm = re.search(r"this\.register\(new GenericRecipe", rest)
        chunk = rest[: endm.start()] if endm else rest[:4000]
        if ".inputFluids(" in chunk.split(".outputItems(")[0] if False else False:
            pass
        # skip fluid-gated recipes — this port's AssemblerRecipe is items-only
        if re.search(r"\.inputFluids\(", chunk):
            # still allow if inputFluids is the only extra and we want items-only? No — incomplete.
            recipes.append({"name": name, "duration": dur, "power": pow_, "out": None, "inn": None, "skip": "fluids"})
            continue
        out = inn = None
        pos = 0
        while pos < len(chunk):
            om = re.search(r"\.(outputItems|inputItems)\(", chunk[pos:])
            if not om or (out and inn):
                break
            abs_par = pos + om.end() - 1
            taken = _take_call_args(chunk, abs_par)
            if not taken:
                break
            body, nxti = taken
            if om.group(1) == "outputItems":
                out = body
            elif om.group(1) == "inputItems" and inn is None:
                inn = body
            pos = nxti
        if out and inn:
            recipes.append({"name": name, "duration": dur, "power": pow_, "out": out, "inn": inn, "skip": None})
        else:
            recipes.append({"name": name, "duration": dur, "power": pow_, "out": out, "inn": inn, "skip": "parse"})
    return recipes


def split_args(s: str) -> list[str]:
    args, depth, cur = [], 0, []
    for ch in s:
        if ch == "(":
            depth += 1
            cur.append(ch)
        elif ch == ")":
            depth -= 1
            cur.append(ch)
        elif ch == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if cur:
        args.append("".join(cur).strip())
    return [a for a in args if a]


def write_assembler(known: set[str]) -> tuple[int, int, dict[str, int]]:
    if not CE_ASS.exists():
        return 0, 0, {}
    text = CE_ASS.read_text(errors="replace")
    recs = parse_all_recipes(text)
    out_dir = DATA / "recipe" / "assembler"
    out_dir.mkdir(parents=True, exist_ok=True)
    ok = skip = 0
    reasons: dict[str, int] = {}
    for r in recs:
        if r.get("skip"):
            reasons[r["skip"]] = reasons.get(r["skip"], 0) + 1
            skip += 1
            continue
        out_args = split_args(r["out"])
        in_args = split_args(r["inn"])
        outs = [resolve_stack(a, known) for a in out_args]
        inns = [resolve_stack(a, known) for a in in_args]
        if any(x is None for x in outs + inns) or not outs or not inns:
            fail = next((a for a, x in zip(out_args + in_args, outs + inns) if x is None), "?")
            key = "unresolved"
            if "ModBlocks." in fail:
                key = "missing_block"
            elif "item_expensive" in fail or "part_generic" in fail:
                key = "unregistered_part"
            elif "OreDictStack" in fail or "ANY_TAR" in fail or "Fluids." in fail:
                key = "ore_or_fluid"
            reasons[key] = reasons.get(key, 0) + 1
            if reasons.get("_samples", 0) < 25:
                reasons["_samples"] = reasons.get("_samples", 0) + 1
                print(f"  SKIP {r['name']}: {fail[:140]}")
            skip += 1
            continue
        oid, oc = outs[0]
        inputs = [{"item": {"item": iid}, "count": c} for iid, c in inns]
        slug = r["name"].replace("ass.", "")
        path = out_dir / f"{slug}.json"
        path.write_text(json.dumps({
            "type": "hbm:assembler",
            "inputs": inputs,
            "output": {"id": oid, "count": oc},
            "duration": r["duration"],
            "power": r["power"],
        }, indent=2) + "\n")
        ok += 1
    return ok, skip, reasons


def main() -> None:
    write_models()
    patch_lang()
    write_crafting()
    known = known_ids()
    ok, skip, reasons = write_assembler(known)
    print(f"assembler ALL written={ok} skipped={skip} known={len(known)} reasons={reasons}")


if __name__ == "__main__":
    main()
