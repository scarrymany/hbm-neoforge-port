#!/usr/bin/env python3
"""Phase 11 live CE-vs-port registry census. Prints JSON to stdout."""
from __future__ import annotations

import json
import os
import re
from collections import defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CE = ROOT / "upstream" / "hbm-ce" / "src" / "main"
PORT_JAVA = ROOT / "src" / "main" / "java" / "com" / "hbm"
PORT_RES = ROOT / "src" / "main" / "resources"
PORT_GEN = ROOT / "src" / "generated" / "resources"


def read(p: Path) -> str:
    return p.read_text(encoding="utf-8", errors="replace")


def java_files(root: Path) -> list[Path]:
    if not root.exists():
        return []
    return sorted(root.rglob("*.java"))


def extract_quoted(pattern: str, text: str) -> list[str]:
    return re.findall(pattern, text)


def port_register_ids(pattern: str, roots: list[Path]) -> set[str]:
    ids: set[str] = set()
    for root in roots:
        for p in java_files(root):
            ids.update(extract_quoted(pattern, read(p)))
    return ids


# ---------------------------------------------------------------------------
# Entities
# ---------------------------------------------------------------------------

def ce_entity_ids() -> dict[str, str]:
    """CE @AutoRegister(name=...) on entity classes, excluding tileentity/render."""
    out: dict[str, str] = {}
    ent = CE / "java" / "com" / "hbm" / "entity"
    if not ent.exists():
        return out
    for p in java_files(ent):
        rel = str(p.relative_to(CE / "java"))
        if "/tileentity" in rel.replace("\\", "/"):
            continue
        text = read(p)
        for i, m in enumerate(re.finditer(r'@AutoRegister\(\s*name\s*=\s*"([^"]+)"', text)):
            name = m.group(1)
            line = text[: m.start()].count("\n") + 1
            out[name] = f"{rel}:{line}"
    return out


def port_entity_ids() -> set[str]:
    ids: set[str] = set()
    for p in java_files(PORT_JAVA / "entity"):
        text = read(p)
        ids.update(re.findall(r'\.register\(\s*"([^"]+)"', text))
        # Phase9TailEntityTypes.thrown("id") / thrownNamed / logic
        ids.update(re.findall(r'(?:thrownNamed|thrown|logic)\(\s*"([^"]+)"', text))
    # missile_shuttle is registered as that id
    return ids


# ---------------------------------------------------------------------------
# Items / blocks
# ---------------------------------------------------------------------------

def port_item_ids() -> set[str]:
    import sys
    sys.path.insert(0, str(ROOT / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    items, _blocks = extract_all_ids()
    extra = {
        "item_expensive_steel_plating", "item_expensive_heavy_frame", "item_expensive_circuit",
        "item_expensive_lead_plating", "item_expensive_ferro_plating", "item_expensive_computer",
        "item_expensive_bronze_tubes", "item_expensive_plastic", "item_expensive_gold_dust",
        "item_expensive_degenerate_matter",
        "part_generic_piston_pneumatic", "part_generic_piston_hydraulic", "part_generic_piston_electric",
        "part_generic_lde", "part_generic_hde", "part_generic_glass_polarized",
        "battery_redstone_pack", "battery_lead_pack", "battery_lithium_pack",
        "battery_sodium_pack", "battery_schrabidium_pack", "battery_quantum_pack",
        "capacitor_copper_pack", "capacitor_gold_pack", "capacitor_niobium_pack",
        "capacitor_tantalum_pack", "capacitor_bismuth_pack", "capacitor_spark_pack",
        "pellet_charged", "biomass", "biomass_compressed", "bio_wafer",
        "fuel_additive_antiknock", "fuel_additive_deicer",
        "nuclear_waste_tiny", "nuclear_waste_vitrified",
        "dust", "solid_fuel", "solid_fuel_bf", "cordite", "ball_tnt",
        "ball_dynamite", "ball_tatb", "rocket_fuel", "powder_sawdust",
        "gem_tantalium", "canister_napalm",
        "part_lithium", "part_beryllium", "part_carbon", "part_copper", "part_plutonium",
        "oil_tar_crude", "oil_tar_crack", "oil_tar_coal", "oil_tar_wood", "oil_tar_wax", "oil_tar_paraffin",
        "particle_empty", "particle_hydrogen", "particle_copper", "particle_lead",
        "particle_amat", "particle_aschrab", "particle_dark", "particle_higgs",
        "particle_tachyon", "particle_strange", "particle_sparkticle",
        "sulfur", "niter",
        "casing_small", "casing_large", "casing_small_steel", "casing_large_steel",
        "casing_shotshell", "casing_buckshot", "casing_buckshot_advanced",
        "upgrade_template", "neutron_reflector", "missile_assembly",
        "thruster_small", "thruster_medium", "thruster_large",
        "fuel_tank_small", "fuel_tank_medium", "fuel_tank_large",
        "warhead_generic_small", "warhead_incendiary_small", "warhead_cluster_small", "warhead_buster_small",
        "warhead_generic_medium", "warhead_incendiary_medium", "warhead_cluster_medium", "warhead_buster_medium",
        "warhead_generic_large", "warhead_incendiary_large", "warhead_cluster_large", "warhead_buster_large",
        "warhead_nuclear", "warhead_mirv", "warhead_volcano",
        "sat_base", "sat_head_mapper", "sat_head_scanner", "sat_head_radar",
        "sat_head_laser", "sat_head_resonator", "photo_panel", "ballistite",
    }
    for t in ("meu", "heu233", "heu235", "men", "hen237", "mox", "mep", "hep239", "hep241",
              "mea", "hea242", "hes326", "hes327", "bfb_am_mix", "bfb_pu241"):
        extra.add(f"pwr_fuel_depleted_{t}")
    return set(items) | extra


def port_block_ids() -> set[str]:
    import sys
    sys.path.insert(0, str(ROOT / "scripts"))
    from phase10_remap_v3 import extract_all_ids
    _items, blocks = extract_all_ids()
    extra = set()
    helper = re.compile(
        r'(?:registerBlock|registerMachine|registerResource|registerCasing|ore|outgas|stair|slab)\(\s*"([a-z][a-z0-9_]*)"'
    )
    for p in java_files(PORT_JAVA / "blocks"):
        extra.update(helper.findall(read(p)))
    return set(blocks) | extra


def ce_string_ids(path: Path) -> set[str]:
    """Quoted registry-style ids from CE 1.12 constructors (new ItemBase("id"), etc.)."""
    if not path.exists():
        return set()
    text = read(path)
    ids = set(re.findall(r'new\s+\w+\s*\(\s*"([a-z][a-z0-9_.]*)"', text))
    ids |= set(re.findall(r'setTranslationKey\(\s*"([a-z][a-z0-9_.]*)"', text))
    return {i.replace(".", "_") for i in ids if not i.startswith("minecraft")}


def ce_item_fields() -> int:
    p = CE / "java" / "com" / "hbm" / "items" / "ModItems.java"
    if not p.exists():
        return 0
    text = read(p)
    # public static final Item / ItemXxx fields
    return len(re.findall(r'public\s+static\s+final\s+\w+\s+\w+\s*=\s*new\s+', text))


def ce_block_fields() -> int:
    p = CE / "java" / "com" / "hbm" / "blocks" / "ModBlocks.java"
    if not p.exists():
        return 0
    text = read(p)
    n = len(re.findall(r'public\s+static\s+final\s+\w+\s+\w+\s*=\s*new\s+', text))
    # loop families
    if "concrete_colored_stairs" in text:
        n += 16
    if "concrete_colored_ext_stairs" in text:
        n += 8
    return n


def ce_item_variants() -> int:
    """Flattened-equivalent: ItemEnumMulti fields * enum size, plus plain items."""
    mod = CE / "java" / "com" / "hbm" / "items" / "ModItems.java"
    if not mod.exists():
        return 0
    text = read(mod)
    # count ItemEnumMulti / similar multiplex constructors
    enum_multi = len(re.findall(r'new\s+ItemEnumMulti', text))
    # fallback: field count is the 1.12 collapsed count
    fields = ce_item_fields()
    # scan enum files referenced
    extra = 0
    enums_dir = CE / "java" / "com" / "hbm" / "items"
    enum_sizes: dict[str, int] = {}
    for p in java_files(enums_dir):
        t = read(p)
        m = re.search(r'enum\s+(\w+)', t)
        if not m:
            continue
        # crude: count ENUM_CONST,
        consts = re.findall(r'^\s+([A-Z][A-Z0-9_]+)\s*[,;(]', t, flags=re.M)
        if consts:
            enum_sizes[m.group(1)] = len(consts)
    # ItemEnumMulti<>(EnumX.class) or similar
    for m in re.finditer(r'ItemEnumMulti\s*\([^)]*?(\w+)\.class', text):
        extra += enum_sizes.get(m.group(1), 0)
    # if we can't expand, report fields + extra where found
    return fields + extra


# ---------------------------------------------------------------------------
# Fluids / sounds
# ---------------------------------------------------------------------------

def count_fluid_fields(path: Path) -> int:
    if not path.exists():
        return 0
    text = read(path)
    return len(re.findall(r'public\s+static\s+(?:final\s+)?FluidType\s+\w+', text))


def count_sound_fields(path: Path) -> int:
    if not path.exists():
        return 0
    text = read(path)
    a = len(re.findall(r'public\s+static\s+(?:final\s+)?SoundEvent\s+\w+', text))
    b = len(re.findall(r'DeferredHolder<SoundEvent,\s*SoundEvent>', text))
    return max(a, b)


# ---------------------------------------------------------------------------
# Recipes
# ---------------------------------------------------------------------------

VANILLA_TYPES = {
    "minecraft:crafting_shaped",
    "minecraft:crafting_shapeless",
    "minecraft:smelting",
    "minecraft:blasting",
    "minecraft:smoking",
    "minecraft:campfire_cooking",
    "minecraft:stonecutting",
    "minecraft:smithing_transform",
    "minecraft:smithing_trim",
}

MACHINE_TYPES_PREFIX = "hbm:"


def collect_recipe_json() -> list[tuple[Path, dict]]:
    found: list[tuple[Path, dict]] = []
    for base in (PORT_RES, PORT_GEN):
        rec = base / "data" / "hbm" / "recipe"
        if not rec.exists():
            continue
        for p in rec.rglob("*.json"):
            try:
                found.append((p, json.loads(p.read_text(encoding="utf-8"))))
            except Exception:
                pass
    return found


def java_machine_recipe_counts() -> dict[str, int]:
    counts: dict[str, int] = {}
    recipes_root = PORT_JAVA / "inventory" / "recipes"
    if not recipes_root.exists():
        return counts
    for p in java_files(recipes_root):
        text = read(p)
        name = p.stem
        n = 0
        n += len(re.findall(r'RECIPES\.add\(', text))
        n += len(re.findall(r'recipes\.put\(', text))
        n += len(re.findall(r'RECIPES\.put\(', text))
        n += len(re.findall(r'\.register\(\s*new\s+', text))
        n += len(re.findall(r'register\(\s*"crucible\.', text))
        n += len(re.findall(r'register\(\s*new\s+ItemStack', text))
        # SolidificationRecipes: helper RECIPES.put is one site; count live SF-auto calls too.
        if name == "SolidificationRecipes":
            n += len(re.findall(r'registerSFAuto\(Fluids\.', text))
        if n:
            counts[name] = n
    return counts


def ce_machine_recipe_counts() -> dict[str, int]:
    counts: dict[str, int] = {}
    recipes_root = CE / "java" / "com" / "hbm" / "inventory" / "recipes"
    if not recipes_root.exists():
        return counts
    for p in java_files(recipes_root):
        text = read(p)
        n = 0
        n += len(re.findall(r'this\.register\(\s*new\s+GenericRecipe', text))
        n += len(re.findall(r'this\.register\(\s*\(PUREXRecipe\)', text))
        n += len(re.findall(r'recipes\.put\(', text))
        n += len(re.findall(r'RECIPES\.add\(', text))
        if p.stem == "SolidificationRecipes":
            n += len(re.findall(r'registerRecipe\([A-Z_]+,', text))
            n += len(re.findall(r'registerSFAuto\([A-Z_]+', text))
        if p.stem == "ParticleAcceleratorRecipes":
            n += len(re.findall(r'recipes\.add\(', text))
        if p.stem in ("AmmoPressRecipes", "ArcWelderRecipes", "SolderingRecipes"):
            n += len(re.findall(r'recipes\.add\(', text))
        if p.stem == "PlasmaForgeRecipes":
            n += len(re.findall(r'this\.register\(\(PlasmaForgeRecipe\)', text))
        n += len(re.findall(r'registerDefaults', text)) and 0
        n += len(re.findall(r'\.register\(\s*new\s+', text))
        n += len(re.findall(r'addRecipe\(', text))
        if n:
            counts[p.stem] = n
    return counts


def ce_crafting_calls() -> dict[str, int]:
    out: dict[str, int] = {}
    crafting = CE / "java" / "com" / "hbm"
    files = []
    cm = crafting / "main" / "CraftingManager.java"
    if cm.exists():
        files.append(cm)
    cr = crafting / "crafting"
    if cr.exists():
        files.extend(java_files(cr))
    total_auto = 0
    total_shapeless = 0
    total_slab = 0
    total_9 = 0
    for p in files:
        text = read(p)
        total_auto += len(re.findall(r'addRecipeAuto\s*\(', text))
        total_shapeless += len(re.findall(r'addShapelessAuto\s*\(', text))
        total_slab += len(re.findall(r'addSlabStair\s*\(', text))
        total_9 += len(re.findall(r'add(?:9To1|1To9)\s*\(', text))
    out["addRecipeAuto"] = total_auto
    out["addShapelessAuto"] = total_shapeless
    out["addSlabStair"] = total_slab
    out["add9or1"] = total_9
    # loops multiply some of these; flag as call-sites not expanded
    return out


# ---------------------------------------------------------------------------
# Advancements
# ---------------------------------------------------------------------------

def count_advancements(root: Path) -> int:
    n = 0
    for base in (root / "data" / "hbm" / "advancement", root / "data" / "hbm" / "advancements"):
        if base.exists():
            n += sum(1 for p in base.rglob("*.json") if "recipes/" not in str(p).replace("\\", "/"))
    return n


def ce_advancements() -> int:
    # 1.12 lives under assets or data
    n = 0
    for base in (
        CE / "resources" / "assets" / "hbm" / "advancements",
        CE / "resources" / "data" / "hbm" / "advancements",
    ):
        if base.exists():
            n += sum(1 for _ in base.rglob("*.json"))
    return n


# ---------------------------------------------------------------------------
# Reachability (cheap)
# ---------------------------------------------------------------------------

def recipe_outputs(recipes: list[tuple[Path, dict]]) -> set[str]:
    outs: set[str] = set()
    for _, data in recipes:
        res = data.get("result") or data.get("output")
        if isinstance(res, dict):
            iid = res.get("id") or res.get("item")
            if iid:
                outs.add(str(iid))
        elif isinstance(res, str):
            outs.add(res)
    return outs


WASTE_KIND = {
    "nuclearWasteLong": "nuclear_waste_long",
    "nuclearWasteLongTiny": "nuclear_waste_long_tiny",
    "nuclearWasteLongDepleted": "nuclear_waste_long_depleted",
    "nuclearWasteLongDepletedTiny": "nuclear_waste_long_depleted_tiny",
    "nuclearWasteShort": "nuclear_waste_short",
    "nuclearWasteShortTiny": "nuclear_waste_short_tiny",
    "nuclearWasteShortDepleted": "nuclear_waste_short_depleted",
    "nuclearWasteShortDepletedTiny": "nuclear_waste_short_depleted_tiny",
}

# Mats.getRegistryName() = names[0].lower — only materials used as ElectrolyserMetal outputs.
MAT_SCRAP = {
    "IRON": "iron",
    "GOLD": "gold",
    "URANIUM": "uranium",
    "THORIUM": "thorium232",
    "PLUTONIUM": "plutonium",
    "TITANIUM": "titanium",
    "COPPER": "copper",
    "TUNGSTEN": "tungsten",
    "ALUMINIUM": "aluminum",
    "BERYLLIUM": "beryllium",
    "LEAD": "lead",
    "SCHRABIDIUM": "schrabidium",
    "ZIRCONIUM": "zirconium",
    "BORON": "boron",
    "LITHIUM": "lithium",
    "DURA": "durasteel",
    "COBALT": "cobalt",
    "RADIUM": "radium226",
    "POLONIUM": "polonium210",
}


def machine_table_outputs() -> set[str]:
    """Item ids produced by Java machine tables (not JSON). Outputs only — no input keys."""
    outs: set[str] = set()
    recipes_root = PORT_JAVA / "inventory" / "recipes"
    if not recipes_root.exists():
        return outs
    item_ctor = re.compile(
        r'new ItemStack\(\s*(?:'
        r'(?:IngotNuggetItems|BilletPowderItems|PlateCrystalWasteItems|Phase11ProcessItems)\.([A-Z][A-Z0-9_]+)'
        r'|(?:item|hbmItem|stack)\("([a-z0-9_]+)"'
        r'|SpecialItems\.(nuclearWaste\w+)\(ItemWaste\w+\.WasteClass\.([A-Z0-9_]+)\)'
        r'|drive\(EnumDriveType\.([A-Z0-9_]+)\)'
        r'|BilletPowderItems\.powderAsh\(EnumAshType\.([A-Z]+)\)'
        r')'
    )
    stack_helper = re.compile(r'\bstack\("([a-z0-9_]+)"')
    add_out_item = re.compile(
        r'\.addOut\(\s*(?:i\s*<\s*\d+\s*\?\s*)?new ItemStack\(\s*'
        r'(?:IngotNuggetItems|BilletPowderItems|PlateCrystalWasteItems|Phase11ProcessItems)\.([A-Z][A-Z0-9_]+)'
    )
    for p in java_files(recipes_root):
        text = read(p)
        for m in item_ctor.finditer(text):
            field, lit, waste_kind, waste_cls, drive, ash = m.groups()
            if field:
                outs.add("hbm:" + field.lower())
            if lit:
                outs.add("hbm:" + lit)
            if waste_kind and waste_cls:
                prefix = WASTE_KIND.get(waste_kind, waste_kind)
                outs.add(f"hbm:{prefix}_{waste_cls.lower()}")
            if drive:
                outs.add("hbm:drive_" + drive.lower())
            if ash:
                outs.add("hbm:powder_ash_" + ash.lower())
        for sid in stack_helper.findall(text):
            outs.add("hbm:" + sid)
        for field in add_out_item.findall(text):
            outs.add("hbm:" + field.lower())
        # Crystallizer ore(...) passes PlateCrystalWasteItems.CRYSTAL_* without new ItemStack(Field).
        for field in re.findall(
            r'(?:IngotNuggetItems|BilletPowderItems|PlateCrystalWasteItems|Phase11ProcessItems)\.([A-Z][A-Z0-9_]+)',
            text,
        ):
            outs.add("hbm:" + field.lower())
        if p.stem == "WasteDrumRecipes":
            # stack("pwr_fuel_depleted_" + slug) — concat, not a literal.
            for slug in (
                "meu", "heu233", "heu235", "men", "hen237", "mox", "mep", "hep239", "hep241",
                "mea", "hea242", "hes326", "hes327", "bfb_am_mix", "bfb_pu241",
            ):
                outs.add("hbm:pwr_fuel_depleted_" + slug)
        if p.stem == "ElectrolyserMetalRecipes":
            for mat in re.findall(r'Mats\.MAT_([A-Z0-9]+)', text):
                name = MAT_SCRAP.get(mat)
                if name:
                    outs.add("hbm:scraps_" + name)
        if p.stem == "SuperComputerRecipes":
            for dt in re.findall(r'drive\(EnumDriveType\.([A-Z0-9_]+)\)', text):
                outs.add("hbm:drive_" + dt.lower())
            for folder in re.findall(r'item\("(blueprint_folder_[a-z_]+)"\)', text):
                outs.add("hbm:" + folder)
    return outs


def loot_outputs() -> set[str]:
    outs: set[str] = set()
    for base in (PORT_RES, PORT_GEN):
        loot = base / "data" / "hbm" / "loot_table"
        if not loot.exists():
            loot = base / "data" / "hbm" / "loot_tables"
        if not loot.exists():
            continue
        for p in loot.rglob("*.json"):
            text = p.read_text(encoding="utf-8", errors="replace")
            outs.update(re.findall(r'"name"\s*:\s*"(hbm:[^"]+)"', text))
            outs.update(re.findall(r'"item"\s*:\s*"(hbm:[^"]+)"', text))
    # Live Java ItemPools (structures / satellite miners / vending). These fire in-world;
    # census was JSON-only and treated the pools as invisible.
    itempool = PORT_JAVA / "itempool"
    if itempool.exists():
        add_id = re.compile(
            r'(?:addHbm|ItemPoolLookups\.add)\(\s*\w+\s*,\s*"([a-z0-9_]+)"'
        )
        for p in java_files(itempool):
            text = read(p)
            for sid in add_id.findall(text):
                outs.add("hbm:" + sid)
    return outs


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def pct(port: float, ce: float) -> float:
    if ce <= 0:
        return 0.0
    return 100.0 * port / ce


def main() -> None:
    ce_ents = ce_entity_ids()
    port_ents = port_entity_ids()
    missing_ents = sorted(set(ce_ents) - port_ents)
    extra_ents = sorted(port_ents - set(ce_ents))
    # intentional rename
    if "entity_clound_solinium" in missing_ents and "entity_cloud_solinium" in port_ents:
        missing_ents.remove("entity_clound_solinium")

    port_items = port_item_ids()
    port_blocks = port_block_ids()
    # BlockItems are typically same id as blocks; unique items = item regs + block regs
    # Many register() hits are items; subtract nothing — overcount is possible.
    # Prefer: item register ids that aren't exclusively blocks... keep both.

    ce_items_collapsed = ce_item_fields()
    ce_items_expanded = ce_item_variants()
    ce_item_strings = 0
    ce_moditems = CE / "java" / "com" / "hbm" / "items" / "ModItems.java"
    if ce_moditems.exists():
        ce_item_strings = len(ce_string_ids(ce_moditems))
    ce_blocks = ce_block_fields()
    ce_block_strings = 0
    ce_modblocks = CE / "java" / "com" / "hbm" / "blocks" / "ModBlocks.java"
    if ce_modblocks.exists():
        ce_block_strings = len(ce_string_ids(ce_modblocks))

    ce_fluids = count_fluid_fields(CE / "java" / "com" / "hbm" / "inventory" / "fluid" / "Fluids.java")
    port_fluids = count_fluid_fields(PORT_JAVA / "inventory" / "fluid" / "Fluids.java")

    ce_sounds = count_sound_fields(CE / "java" / "com" / "hbm" / "lib" / "HBMSoundHandler.java")
    port_sounds = count_sound_fields(PORT_JAVA / "lib" / "HBMSoundHandler.java")

    recipes = collect_recipe_json()
    by_type: dict[str, int] = defaultdict(int)
    for _, data in recipes:
        by_type[str(data.get("type", "?"))] += 1
    vanilla_json = sum(v for k, v in by_type.items() if k in VANILLA_TYPES or k.startswith("minecraft:crafting"))
    # also hbm custom crafting serializers
    hbm_crafting = sum(v for k, v in by_type.items() if k.startswith("hbm:") and k.split(":")[-1] in {
        "grenade_crafting", "rbmk_fuel_recycle", "scrap_split",
        "container_upgrade_crate_desh", "container_upgrade_crate_tungsten",
        "container_upgrade_safe",
    })
    machine_json = sum(v for k, v in by_type.items() if k.startswith("hbm:") and k not in {
        "hbm:grenade_crafting", "hbm:rbmk_fuel_recycle", "hbm:scrap_split",
        "hbm:container_upgrade_crate_desh", "hbm:container_upgrade_crate_tungsten",
        "hbm:container_upgrade_safe",
    } and not k.startswith("minecraft:"))
    # leftover minecraft types already in vanilla_json
    other_json = sum(v for k, v in by_type.items() if k not in VANILLA_TYPES and not k.startswith("hbm:"))

    java_mach = java_machine_recipe_counts()
    ce_mach = ce_machine_recipe_counts()
    ce_craft = ce_crafting_calls()

    # CE crafting estimate: call sites + loop expansion lower bound
    ce_craft_calls = sum(ce_craft.values())
    # Phase 6 used 1900-2000 from CraftingManager + 9 handlers. Keep that as CE
    # ceiling and use call-site * 1.6 as a cheap lower bound (loops).
    ce_craft_est = 1950
    ce_machine_est = sum(ce_mach.values())
    # AssemblyMachineRecipes fluid pack loop ~2 * fluid types with containers
    if "AssemblyMachineRecipes" in ce_mach:
        ce_machine_est += 300  # documented ~320 pack/unpack in phase7 research

    port_vanilla = vanilla_json + hbm_crafting
    port_machine = machine_json + sum(java_mach.values())

    port_adv = count_advancements(PORT_GEN) + count_advancements(PORT_RES)
    ce_adv = ce_advancements() or 65

    # Phase 10 extract_all_ids: Java register/reg + Mats autogen + plant/glyph/bedrock loops.
    # Same denominator as docs/phase10/LEFTOVER_MISSES.md (1771/579 before this session's parts).
    port_items_n = len(port_items)
    port_blocks_n = len(port_blocks)

    outs = recipe_outputs(recipes) | loot_outputs() | machine_table_outputs()
    reachable = {i for i in port_items if f"hbm:{i}" in outs or i in {o.split(":")[-1] for o in outs}}
    reach_pct = pct(len(reachable), max(1, port_items_n))

    categories = {
        "items": {
            "ce_collapsed": ce_items_collapsed,
            "ce_expanded_approx": ce_items_expanded,
            "ce_string_ctors": ce_item_strings,
            # Flattened-equivalent: max(enum-expanded fields, string ctors). Lang keys are NOT used.
            "ce_used": max(ce_items_expanded, ce_item_strings, ce_items_collapsed),
            "port": port_items_n,
            "method": "phase10 extract_all_ids (Java register/reg + Mats autogen + loops)",
        },
        "blocks": {
            "ce": max(ce_blocks, ce_block_strings),
            "ce_fields": ce_blocks,
            "ce_string_ctors": ce_block_strings,
            "port": port_blocks_n,
            "method": "phase10 extract_all_ids + ore()/stair/slab helpers",
        },
        "fluids": {"ce": ce_fluids, "port": port_fluids},
        "entities": {"ce": len(ce_ents), "port": len(port_ents), "missing": missing_ents, "extra": extra_ents},
        "sounds": {"ce": ce_sounds, "port": port_sounds},
        "vanilla_crafting": {
            "ce": ce_craft_est,
            "ce_call_sites": ce_craft,
            "port": port_vanilla,
            "by_type": {k: v for k, v in sorted(by_type.items()) if k in VANILLA_TYPES or k.startswith("minecraft:")},
        },
        "machine_recipes": {
            "ce_regex_plus_packloop": ce_machine_est,
            "ce_by_class": ce_mach,
            "port_json": machine_json,
            "port_java": java_mach,
            "port": port_machine,
            "by_type": {k: v for k, v in sorted(by_type.items()) if k.startswith("hbm:")},
        },
        "advancements": {"ce": ce_adv, "port": port_adv},
        "reachability": {
            "port_items": port_items_n,
            "reachable_via_recipe_or_loot": len(reachable),
            "includes_machine_tables": True,
            "pct": round(reach_pct, 1),
        },
    }

    # Items: compare flattened port ids to CE flattened-approx (enum-expanded + fields).
    # Do NOT use lang-key counts (CE lang was copied wholesale and overcounts).
    ce_item_used = categories["items"]["ce_used"]
    rows = [
        ("items", ce_item_used, port_items_n),
        ("blocks", categories["blocks"]["ce"], port_blocks_n),
        ("fluids", ce_fluids, port_fluids),
        ("entities", len(ce_ents), len(port_ents)),
        ("sounds", ce_sounds, port_sounds),
        ("vanilla_crafting", ce_craft_est, port_vanilla),
        ("machine_recipes", ce_machine_est, port_machine),
        ("advancements", ce_adv, port_adv),
    ]
    ce_sum = sum(r[1] for r in rows)
    port_sum = sum(r[2] for r in rows)
    unweighted = sum(pct(r[2], r[1]) for r in rows) / len(rows)
    weighted = pct(port_sum, ce_sum)

    report = {
        "weighted_pct": round(weighted, 1),
        "unweighted_pct": round(unweighted, 1),
        "ce_sum": ce_sum,
        "port_sum": port_sum,
        "rows": [
            {
                "cat": n,
                "ce": c,
                "port": p,
                "pct": round(pct(p, c), 1),
            }
            for n, c, p in rows
        ],
        "detail": categories,
        "recipe_json_total": len(recipes),
    }
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
