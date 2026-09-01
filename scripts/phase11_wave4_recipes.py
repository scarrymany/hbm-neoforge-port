#!/usr/bin/env python3
"""Generate AmmoPress / ArcWelder / Soldering / PlasmaForge Java tables + leftover assembler JSON."""
from __future__ import annotations

import json
import re
import shutil
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / "scripts"))
from phase11_machine_parts import (  # noqa: E402
    CE_ASS, DATA, MAT, SHAPE_CANDIDATES, known_ids, parse_all_recipes, resolve_fluid,
    resolve_stack, split_args, write_assembler,
)

CE_REC = REPO / "upstream" / "hbm-ce" / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "recipes"
PORT_REC = REPO / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "recipes"
CE_ASSETS = REPO / "upstream" / "hbm-ce" / "src" / "main" / "resources" / "assets" / "hbm"
ASSETS = REPO / "src" / "main" / "resources" / "assets" / "hbm"

MATS_MAP = {
    "MAT_STEEL": "steel", "MAT_IRON": "iron", "MAT_COPPER": "copper", "MAT_TITANIUM": "titanium",
    "MAT_ALUMINIUM": "aluminum", "MAT_ALUMINUM": "aluminum", "MAT_ZIRCONIUM": "zirconium",
    "MAT_TCALLOY": "tcalloy", "MAT_MINGRADE": "mingrade", "MAT_GOLD": "gold",
    "MAT_TUNGSTEN": "tungsten", "MAT_DESH": "desh", "MAT_DURA": "dura_steel",
    "MAT_CMB": "cmbsteel", "MAT_BSCCO": "bscco", "MAT_SCHRABIDIUM": "schrabidium",
    "MAT_CDALLOY": "cdalloy", "MAT_OSMIRIDIUM": "osmiridium",
}

EXTRA_KNOWN = {
    "sulfur", "niter",
    "casing_small", "casing_large", "casing_small_steel", "casing_large_steel",
    "casing_shotshell", "casing_buckshot", "casing_buckshot_advanced",
    "machine_ammo_press", "machine_arc_welder", "machine_soldering_station", "fusion_plasma_forge",
    "upgrade_template", "upgrade_speed_1", "upgrade_speed_2", "upgrade_speed_3",
    "upgrade_effect_1", "upgrade_effect_2", "upgrade_effect_3",
    "upgrade_power_1", "upgrade_power_2", "upgrade_power_3",
    "upgrade_fortune_1", "upgrade_fortune_2", "upgrade_fortune_3",
    "upgrade_afterburn_1", "upgrade_afterburn_2", "upgrade_afterburn_3",
    "upgrade_radius", "upgrade_health", "upgrade_overdrive_1",
    "early_explosive_lenses", "fleija_propellant", "det_cord", "ingot_fiberglass",
    "neutron_reflector", "missile_assembly",
    "thruster_small", "thruster_medium", "thruster_large",
    "fuel_tank_small", "fuel_tank_medium", "fuel_tank_large",
    "warhead_generic_small", "warhead_incendiary_small", "warhead_cluster_small", "warhead_buster_small",
    "warhead_generic_medium", "warhead_incendiary_medium", "warhead_cluster_medium", "warhead_buster_medium",
    "warhead_generic_large", "warhead_incendiary_large", "warhead_cluster_large", "warhead_buster_large",
    "warhead_nuclear", "warhead_mirv", "warhead_volcano",
    "missile_anti_ballistic", "missile_generic", "missile_incendiary", "missile_cluster",
    "missile_buster", "missile_decoy", "missile_strong", "missile_incendiary_strong",
    "missile_cluster_strong", "missile_buster_strong", "missile_emp_strong",
    "missile_burst", "missile_inferno", "missile_rain", "missile_drill",
    "missile_nuclear", "missile_nuclear_cluster", "missile_volcano",
    "neodymium_dense_wire", "schrabidate_dense_wire", "aluminum_plate_sextuple",
    "machine_flare", "machine_catalytic_cracker", "machine_coker", "machine_vacuum_distill",
    "machine_catalytic_reformer", "machine_hydrotreater", "machine_radiolysis",
    "icf_laser_component_casing", "icf_laser_component_port", "icf_laser_component_cell",
    "icf_laser_component_emitter", "icf_laser_component_capacitor", "icf_laser_component_turbo",
    "icf_component_0", "icf_component_1", "icf_component_3", "struct_icf_core",
    "dfc_core", "dfc_emitter", "dfc_receiver", "dfc_injector", "dfc_stabilizer",
    "sliding_blast_door_legacy", "large_vehicle_door", "water_door", "qe_containment",
    "qe_sliding_door", "round_airlock_door", "secure_access_door", "sliding_seal_door",
    "cargo_door", "silo_hatch", "silo_hatch_large", "transition_seal", "emp_bomb",
    "ingot_euphemium", "ingot_dineutronium", "powder_astatine", "gem_volcanic",
    "ingot_osmiridium", "ingot_pc", "ingot_pvc", "ingot_bakelite", "ingot_polymer",
}


def known() -> set[str]:
    return known_ids() | EXTRA_KNOWN


def resolve_more(expr: str, kn: set[str]) -> tuple[str, int] | None:
    expr = expr.strip().replace("RecipesCommon.", "")
    m = re.search(r"DictFrame\.fromOne\(ModItems\.ammo_standard,\s*EnumAmmo\.(\w+)(?:,\s*(\d+))?\)", expr)
    if m:
        slug = m.group(1).lower()
        n = int(m.group(2) or 1)
        return (f"hbm:{slug}", n) if slug in kn else None
    m = re.search(r"new ComparableStack\(ModItems\.casing,\s*(\d+),\s*EnumCasingType\.(\w+)", expr)
    if m:
        n = int(m.group(1))
        cmap = {
            "SMALL": "casing_small", "LARGE": "casing_large",
            "SMALL_STEEL": "casing_small_steel", "LARGE_STEEL": "casing_large_steel",
            "SHOTSHELL": "casing_shotshell", "BUCKSHOT": "casing_buckshot",
            "BUCKSHOT_ADVANCED": "casing_buckshot_advanced",
        }
        cid = cmap.get(m.group(2))
        return (f"hbm:{cid}", n) if cid and cid in kn else None
    m = re.search(r"new ItemStack\(ModItems\.(plate_welded|wire_dense),\s*(\d+),\s*Mats\.(MAT_\w+)\.id\)", expr)
    if m:
        kind, n, mat = m.group(1), int(m.group(2)), MATS_MAP.get(m.group(3))
        if not mat:
            return None
        if kind == "plate_welded":
            cands = [f"{mat}_plate_sextuple", f"plate_welded_{mat}", f"{mat}_plate"]
        else:
            cands = [f"{mat}_dense_wire", f"wire_dense_{mat}", f"{mat}_wire"]
        for c in cands:
            if c in kn:
                return f"hbm:{c}", n
        return None
    m = re.search(r"new OreDictStack\(ANY_SMOKELESS\.dust\(\)(?:,\s*(\d+))?\)", expr)
    if m:
        return "tag:hbm:any_smokeless", int(m.group(1) or 1)
    m = re.search(r"new OreDictStack\(ANY_HIGHEXPLOSIVE\.ingot\(\)(?:,\s*(\d+))?\)", expr)
    if m:
        return "tag:hbm:any_highexplosive", int(m.group(1) or 1)
    m = re.search(r"new ComparableStack\(Items\.GUNPOWDER(?:,\s*(\d+))?\)", expr)
    if m:
        return "minecraft:gunpowder", int(m.group(1) or 1)
    m = re.search(r"new OreDictStack\((\w+)\.(\w+)\(\)\.copy\((\d+)\)\)", expr)
    if m:
        from phase11_machine_parts import resolve_ore
        return resolve_ore(m.group(1), m.group(2), int(m.group(3)), kn)
    m = re.search(r"ModBlocks\.icf_laser_component\s*,\s*(\d+)\s*,\s*EnumICFPart\.(\w+)", expr)
    if m:
        from phase11_machine_parts import ICF_LASER_ENUM
        cid = ICF_LASER_ENUM.get(m.group(2))
        return (f"hbm:{cid}", int(m.group(1))) if cid and cid in kn else None
    m = re.search(r"ModBlocks\.icf_component\s*,\s*(\d+)\s*,\s*(\d+)", expr)
    if m:
        from phase11_machine_parts import ICF_COMPONENT_META
        cid = ICF_COMPONENT_META.get(m.group(2))
        return (f"hbm:{cid}", int(m.group(1))) if cid and cid in kn else None
    hit = resolve_stack(expr, kn)
    if hit:
        return hit
    m = re.match(r"(\w+)\.copy\((\d+)\)", expr)
    if m:
        return None  # handled via named locals in ammo press
    return None


def java_stack(hit: tuple[str, int]) -> str:
    iid, n = hit
    if iid.startswith("tag:"):
        tag = iid[4:]
        path = tag.split(":", 1)[-1]
        ns = tag.split(":")[0]
        if ns == "hbm":
            return f"OreDictStack.ofHbmTag(\"{path}\", {n})"
        return f"new OreDictStack(net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath(\"{ns}\", \"{path}\")), {n})"
    if iid.startswith("minecraft:"):
        path = iid.split(":")[-1].upper()
        vanilla = {
            "GUNPOWDER": "Items.GUNPOWDER",
            "IRON_INGOT": "Items.IRON_INGOT",
            "GOLD_INGOT": "Items.GOLD_INGOT",
            "DIAMOND": "Items.DIAMOND",
            "REDSTONE": "Items.REDSTONE",
            "PISTON": "Blocks.PISTON.asItem()",
            "STONE": "Blocks.STONE.asItem()",
        }
        if iid == "minecraft:gunpowder":
            return f"new ComparableStack(Items.GUNPOWDER, {n})" if n != 1 else "new ComparableStack(Items.GUNPOWDER)"
        return f"new ComparableStack(item(\"{iid}\"), {n})"
    path = iid.split(":")[-1]
    return f"new ComparableStack(item(\"{path}\"), {n})"


def java_out(hit: tuple[str, int]) -> str:
    iid, n = hit
    if iid.startswith("minecraft:"):
        return f"new ItemStack(Items.{iid.split(':')[-1].upper()}, {n})"
    return f"new ItemStack(item(\"{iid.split(':')[-1]}\"), {n})"


HEADER = """package com.hbm.inventory.recipes;

import com.hbm.inventory.RecipesCommon.AStack;
import com.hbm.inventory.RecipesCommon.ComparableStack;
import com.hbm.inventory.RecipesCommon.OreDictStack;
import com.hbm.inventory.fluid.FluidStack;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.main.MainRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

"""


def item_helpers() -> str:
    return """
    private static Item item(String id) {
        if (id.contains(":")) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(MainRegistry.MODID, id));
    }

    public static synchronized void register() {
        registerDefaults();
    }
"""


def gen_ammo(kn: set[str]) -> int:
    text = (CE_REC / "AmmoPressRecipes.java").read_text(errors="replace")
    # locals
    locals_map = {
        "lead": ("PB", "ingot", 1),
        "nugget": ("PB", "nugget", 1),
        "flechette": ("PB", "bolt", 1),
        "steel": ("STEEL", "ingot", 1),
        "wSteel": ("WEAPONSTEEL", "ingot", 1),
        "copper": ("CU", "ingot", 1),
        "plastic": ("ANY_PLASTIC", "ingot", 1),
        "uranium": ("U238", "ingot", 1),
        "ferro": ("FERRO", "ingot", 1),
        "nb": ("NB", "ingot", 1),
        "smokeless": None,  # tag
        "he": None,
        "wp": ("P_WHITE", "ingot", 1),
        "rp": ("P_RED", "dust", 1),
        "pipe": ("STEEL", "pipe", 1),
        "smokeful": "gunpowder",
        "rocket": "rocket_fuel",
        "cSmall": "casing_small",
        "cBig": "casing_large",
        "sSmall": "casing_small_steel",
        "sBig": "casing_large_steel",
        "bpShell": "casing_shotshell",
        "pShell": "casing_buckshot",
        "sShell": "casing_buckshot_advanced",
    }
    from phase11_machine_parts import resolve_ore
    resolved_locals: dict[str, str] = {}
    for name, spec in locals_map.items():
        if spec is None:
            continue
        if isinstance(spec, str):
            if spec == "gunpowder":
                resolved_locals[name] = "new ComparableStack(Items.GUNPOWDER)"
            elif spec in kn:
                resolved_locals[name] = f"new ComparableStack(item(\"{spec}\"))"
            continue
        hit = resolve_ore(spec[0], spec[1], spec[2], kn)
        if hit:
            resolved_locals[name] = java_stack(hit)
    resolved_locals["smokeless"] = "OreDictStack.ofHbmTag(\"any_smokeless\", 1)"
    resolved_locals["he"] = "OreDictStack.ofHbmTag(\"any_highexplosive\", 1)"

    body = []
    ok = 0
    for m in re.finditer(r"recipes\.add\(\s*new AmmoPressRecipe\(\s*(.*?)\s*\)\s*\)\s*;", text, re.S):
        args = split_args(m.group(1))
        if len(args) < 2:
            continue
        out = resolve_more(args[0], kn)
        if not out:
            continue
        slots = []
        good = True
        for a in args[1:10]:
            a = a.strip()
            if a == "null":
                slots.append("null")
                continue
            cm = re.match(r"(\w+)(?:\.copy\((\d+)\))?", a)
            if cm and cm.group(1) in resolved_locals:
                base = resolved_locals[cm.group(1)]
                if cm.group(2):
                    slots.append(f"(({base}).copy({cm.group(2)}))")
                else:
                    slots.append(base)
                continue
            hit = resolve_more(a, kn)
            if hit:
                slots.append(java_stack(hit))
                continue
            good = False
            break
        if not good or len(slots) != 9:
            continue
        body.append(f"        RECIPES.add(new AmmoPressRecipe({java_out(out)},\n                {', '.join(slots)}));")
        ok += 1

    java = HEADER + """/**
 * CE {@code AmmoPressRecipes.java}: 9-slot positional grid. Generated from CE registerDefaults.
 */
public final class AmmoPressRecipes {

    public static final List<AmmoPressRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private AmmoPressRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
""" + "\n".join(body) + """
    }
""" + item_helpers() + """
    public static List<AmmoPressRecipe> getAllRecipes() {
        registerDefaults();
        return java.util.Collections.unmodifiableList(RECIPES);
    }

    public static class AmmoPressRecipe {
        public final ItemStack output;
        public final Object[] slots;

        public AmmoPressRecipe(ItemStack output, Object... slots) {
            this.output = output;
            this.slots = slots;
        }
    }
}
"""
    (PORT_REC / "AmmoPressRecipes.java").write_text(java)
    return ok


def gen_arc(kn: set[str]) -> int:
    text = (CE_REC / "ArcWelderRecipes.java").read_text(errors="replace")
    body = []
    ok = 0
    for m in re.finditer(r"recipes\.add\(\s*new ArcWelderRecipe\(\s*(.*?)\s*\)\s*\)\s*;", text, re.S):
        args = split_args(m.group(1))
        if len(args) < 3:
            continue
        out = resolve_more(args[0], kn)
        if not out:
            continue
        dur = args[1].replace("_", "")
        cons = args[2].replace("_", "").replace("L", "")
        rest = args[3:]
        fluid = None
        ings = []
        good = True
        for a in rest:
            if "Fluids." in a:
                fluid = resolve_fluid(a)
                if fluid is None:
                    good = False
                continue
            hit = resolve_more(a, kn)
            if not hit:
                good = False
                break
            ings.append(java_stack(hit))
        if not good or not ings:
            continue
        fl = f"new FluidStack(Fluids.{fluid['type']}, {fluid['fill']}), " if fluid else ""
        body.append(
            f"        RECIPES.add(new ArcWelderRecipe({java_out(out)}, {dur}, {cons}L, {fl}{', '.join(ings)}));"
        )
        ok += 1
    java = HEADER + """/**
 * CE {@code ArcWelderRecipes.java}. Generated from CE registerDefaults.
 */
public final class ArcWelderRecipes {

    public static final List<ArcWelderRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private ArcWelderRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
""" + "\n".join(body) + """
    }
""" + item_helpers() + """
    public static ArcWelderRecipe getRecipe(ItemStack... inputs) {
        registerDefaults();
        outer:
        for (ArcWelderRecipe recipe : RECIPES) {
            List<AStack> left = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack in : inputs) {
                if (in.isEmpty()) continue;
                boolean hit = false;
                for (int i = 0; i < left.size(); i++) {
                    AStack key = left.get(i);
                    if (key.matchesRecipe(in, true) && in.getCount() >= key.count()) {
                        left.remove(i);
                        hit = true;
                        break;
                    }
                }
                if (!hit) continue outer;
            }
            if (left.isEmpty()) return recipe;
        }
        return null;
    }

    public static class ArcWelderRecipe {
        public final AStack[] ingredients;
        public final FluidStack fluid;
        public final ItemStack output;
        public final int duration;
        public final long consumption;

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, AStack... ingredients) {
            this(output, duration, consumption, null, ingredients);
        }

        public ArcWelderRecipe(ItemStack output, int duration, long consumption, FluidStack fluid, AStack... ingredients) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }
    }
}
"""
    (PORT_REC / "ArcWelderRecipes.java").write_text(java)
    return ok


def gen_solder(kn: set[str]) -> int:
    text = (CE_REC / "SolderingRecipes.java").read_text(errors="replace")
    # no-528 / no-LBSM: take the harder (non-lbsm) count
    text = re.sub(r"lbsm\s*\?\s*(\d+)\s*:\s*(\d+)", r"\2", text)
    body = []
    ok = 0
    # flatten arrays in constructor
    for m in re.finditer(r"recipes\.add\(\s*new SolderingRecipe\(\s*(.*?)\s*\)\s*\)\s*;", text, re.S):
        raw = m.group(1)
        # crude: extract output, dur, cons, optional fluid, then three AStack[] 
        args = split_args(raw)
        if len(args) < 6:
            continue
        out = resolve_more(args[0], kn)
        if not out:
            continue
        dur = args[1].replace("_", "")
        cons = args[2].replace("_", "")
        idx = 3
        fluid = None
        if "Fluids." in args[3]:
            fluid = resolve_fluid(args[3])
            idx = 4
        groups = []
        good = True
        for g in args[idx:idx + 3]:
            inner = g.strip()
            if inner.startswith("new AStack[]"):
                inner = inner[inner.find("{") + 1: inner.rfind("}")]
            stacks = []
            for a in split_args(inner):
                if not a.strip():
                    continue
                hit = resolve_more(a, kn)
                if not hit:
                    good = False
                    break
                stacks.append(java_stack(hit))
            groups.append(stacks)
            if not good:
                break
        if not good or len(groups) != 3:
            continue
        fl = f"new FluidStack(Fluids.{fluid['type']}, {fluid['fill']}), " if fluid else ""
        def arr(xs):
            return "new AStack[]{" + ", ".join(xs) + "}"
        body.append(
            f"        RECIPES.add(new SolderingRecipe({java_out(out)}, {dur}, {cons}, {fl}"
            f"{arr(groups[0])}, {arr(groups[1])}, {arr(groups[2])}));"
        )
        ok += 1
    # CE SolderingRecipes.java:192-282 upgrade_template family (parser misses multiline AStack[])
    if "upgrade_template" in kn:
        templates = [
            ("upgrade_speed_1", "powder_red_copper"),
            ("upgrade_effect_1", "powder_emerald"),
            ("upgrade_power_1", "powder_gold"),
            ("upgrade_fortune_1", "powder_niobium"),
            ("upgrade_afterburn_1", "powder_tungsten"),
        ]
        def arr(xs):
            return "new AStack[]{" + ", ".join(xs) + "}"
        for out_id, dust in templates:
            if out_id not in kn or dust not in kn:
                continue
            body.append(
                f"        RECIPES.add(new SolderingRecipe(new ItemStack(item(\"{out_id}\"), 1), 200, 1000, "
                f"{arr(['new ComparableStack(item(\"circuit_vacuum_tube\"), 4)', 'new ComparableStack(item(\"circuit_capacitor\"), 1)'])}, "
                f"{arr(['new ComparableStack(item(\"upgrade_template\"), 1)', f'new ComparableStack(item(\"{dust}\"), 4)'])}, "
                f"new AStack[]{{}}));"
            )
            ok += 1
        if "upgrade_radius" in kn:
            body.append(
                f"        RECIPES.add(new SolderingRecipe(new ItemStack(item(\"upgrade_radius\"), 1), 200, 1000, "
                f"{arr(['new ComparableStack(item(\"circuit_chip\"), 4)', 'new ComparableStack(item(\"circuit_capacitor\"), 4)'])}, "
                f"{arr(['new ComparableStack(item(\"upgrade_template\"), 1)', 'new ComparableStack(item(\"minecraft:glowstone_dust\"), 4)'])}, "
                f"new AStack[]{{}}));"
            )
            ok += 1
        if "upgrade_health" in kn and "powder_lithium" in kn:
            body.append(
                f"        RECIPES.add(new SolderingRecipe(new ItemStack(item(\"upgrade_health\"), 1), 200, 1000, "
                f"{arr(['new ComparableStack(item(\"circuit_chip\"), 4)', 'new ComparableStack(item(\"circuit_capacitor\"), 4)'])}, "
                f"{arr(['new ComparableStack(item(\"upgrade_template\"), 1)', 'new ComparableStack(item(\"powder_lithium\"), 4)'])}, "
                f"new AStack[]{{}}));"
            )
            ok += 1
    # CE SolderingRecipes.java:284-294 addFirstUpgrade / addSecondUpgrade (no528 default)
    upgrades = [
        ("upgrade_speed_1", "upgrade_speed_2"),
        ("upgrade_effect_1", "upgrade_effect_2"),
        ("upgrade_power_1", "upgrade_power_2"),
        ("upgrade_fortune_1", "upgrade_fortune_2"),
        ("upgrade_afterburn_1", "upgrade_afterburn_2"),
    ]
    seconds = [
        ("upgrade_speed_2", "upgrade_speed_3"),
        ("upgrade_effect_2", "upgrade_effect_3"),
        ("upgrade_power_2", "upgrade_power_3"),
        ("upgrade_fortune_2", "upgrade_fortune_3"),
        ("upgrade_afterburn_2", "upgrade_afterburn_3"),
    ]
    def arr(xs):
        return "new AStack[]{" + ", ".join(xs) + "}"
    for lo, hi in upgrades:
        if lo not in kn or hi not in kn:
            continue
        plastic = resolve_more("new OreDictStack(ANY_PLASTIC.ingot(), 4)", kn)
        if not plastic:
            continue
        body.append(
            f"        RECIPES.add(new SolderingRecipe(new ItemStack(item(\"{hi}\"), 1), 300, 10000, "
            f"{arr(['new ComparableStack(item(\"circuit_chip\"), 8)', 'new ComparableStack(item(\"circuit_capacitor\"), 4)'])}, "
            f"{arr([f'new ComparableStack(item(\"{lo}\"), 1)', java_stack(plastic)])}, "
            f"new AStack[]{{}}));"
        )
        ok += 1
    for lo, hi in seconds:
        if lo not in kn or hi not in kn:
            continue
        rubber = resolve_more("new OreDictStack(RUBBER.ingot(), 4)", kn)
        if not rubber:
            continue
        body.append(
            f"        RECIPES.add(new SolderingRecipe(new ItemStack(item(\"{hi}\"), 1), 400, 25000, "
            f"new FluidStack(Fluids.SOLVENT, 500), "
            f"{arr(['new ComparableStack(item(\"circuit_chip\"), 16)', 'new ComparableStack(item(\"circuit_capacitor\"), 16)'])}, "
            f"{arr([f'new ComparableStack(item(\"{lo}\"), 1)', java_stack(rubber)])}, "
            f"new AStack[]{{}}));"
        )
        ok += 1
    java = HEADER + """/**
 * CE {@code SolderingRecipes.java}. Generated from CE registerDefaults (no 528/LBSM forks).
 */
public final class SolderingRecipes {

    public static final List<SolderingRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private SolderingRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
""" + "\n".join(body) + """
    }
""" + item_helpers() + """
    public static SolderingRecipe getRecipe(ItemStack[] inputs) {
        registerDefaults();
        for (SolderingRecipe recipe : RECIPES) {
            if (matches(new ItemStack[]{inputs[0], inputs[1], inputs[2]}, recipe.toppings)
                    && matches(new ItemStack[]{inputs[3], inputs[4]}, recipe.pcb)
                    && matches(new ItemStack[]{inputs[5]}, recipe.solder)) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matches(ItemStack[] stacks, AStack[] keys) {
        boolean[] used = new boolean[stacks.length];
        for (AStack key : keys) {
            boolean found = false;
            for (int i = 0; i < stacks.length; i++) {
                if (used[i]) continue;
                if (key.matchesRecipe(stacks[i], false)) {
                    used[i] = true;
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public static class SolderingRecipe {
        public final ItemStack output;
        public final int duration;
        public final long consumption;
        public final FluidStack fluid;
        public final AStack[] toppings;
        public final AStack[] pcb;
        public final AStack[] solder;

        public SolderingRecipe(ItemStack output, int duration, long consumption,
                               AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this(output, duration, consumption, null, toppings, pcb, solder);
        }

        public SolderingRecipe(ItemStack output, int duration, long consumption, FluidStack fluid,
                               AStack[] toppings, AStack[] pcb, AStack[] solder) {
            this.output = output;
            this.duration = duration;
            this.consumption = consumption;
            this.fluid = fluid;
            this.toppings = toppings;
            this.pcb = pcb;
            this.solder = solder;
        }
    }
}
"""
    (PORT_REC / "SolderingRecipes.java").write_text(java)
    return ok


def gen_plasma(kn: set[str]) -> int:
    text = (CE_REC / "PlasmaForgeRecipes.java").read_text(errors="replace")
    body = []
    ok = 0
    for m in re.finditer(
        r'this\.register\(\(PlasmaForgeRecipe\) new PlasmaForgeRecipe\("([^"]+)"\)'
        r'\.setInputEnergy\((\d[\d_]*)\)\.setup\((\d[\d_]*)\s*,\s*(\d[\d_]*L?)\)',
        text,
    ):
        name, energy, dur, power = m.group(1), m.group(2).replace("_", ""), m.group(3).replace("_", ""), m.group(4).replace("_", "").replace("L", "")
        rest = text[m.end():]
        endm = re.search(r"this\.register\(\(PlasmaForgeRecipe\)", rest)
        chunk = rest[: endm.start()] if endm else rest[:2500]
        out = inn = infl = None
        pos = 0
        while pos < len(chunk):
            om = re.search(r"\.(outputItems|inputItems|inputFluids)\(", chunk[pos:])
            if not om:
                break
            abs_par = pos + om.end() - 1
            depth = 0
            taken = None
            for i in range(abs_par, len(chunk)):
                if chunk[i] == "(":
                    depth += 1
                elif chunk[i] == ")":
                    depth -= 1
                    if depth == 0:
                        taken = chunk[abs_par + 1:i]
                        pos = i + 1
                        break
            if taken is None:
                break
            if om.group(1) == "outputItems" and out is None:
                out = taken
            elif om.group(1) == "inputItems" and inn is None:
                inn = taken
            elif om.group(1) == "inputFluids" and infl is None:
                infl = taken
        if not out or not inn:
            continue
        outs = [resolve_more(a, kn) for a in split_args(out)]
        inns = [resolve_more(a, kn) for a in split_args(inn)]
        if any(x is None for x in outs + inns) or not outs or not inns:
            continue
        fl = "null"
        if infl:
            f = resolve_fluid(infl)
            if f:
                fl = f"new FluidStack(Fluids.{f['type']}, {f['fill']})"
        ings = ", ".join(java_stack(x) for x in inns)
        body.append(
            f"        RECIPES.add(new PlasmaForgeRecipe(\"{name}\", {energy}L, {dur}, {power}L, {java_out(outs[0])}, {fl}, {ings}));"
        )
        ok += 1
    java = HEADER + """/**
 * CE {@code PlasmaForgeRecipes.java}. Generated from CE registerDefaults.
 * {@code setInputEnergy} is ignition HE on complete (no PlasmaNetwork).
 */
public final class PlasmaForgeRecipes {

    public static final List<PlasmaForgeRecipe> RECIPES = new ArrayList<>();
    private static boolean registered = false;

    private PlasmaForgeRecipes() {
    }

    public static synchronized void registerDefaults() {
        if (registered) return;
        registered = true;
""" + "\n".join(body) + """
    }
""" + item_helpers() + """
    public static PlasmaForgeRecipe getRecipe(ItemStack... inputs) {
        registerDefaults();
        outer:
        for (PlasmaForgeRecipe recipe : RECIPES) {
            List<AStack> left = new ArrayList<>(Arrays.asList(recipe.ingredients));
            for (ItemStack in : inputs) {
                if (in.isEmpty()) continue;
                boolean hit = false;
                for (int i = 0; i < left.size(); i++) {
                    AStack key = left.get(i);
                    if (key.matchesRecipe(in, true) && in.getCount() >= key.count()) {
                        left.remove(i);
                        hit = true;
                        break;
                    }
                }
                if (!hit) continue outer;
            }
            if (left.isEmpty()) return recipe;
        }
        return null;
    }

    public static class PlasmaForgeRecipe {
        public final String name;
        public final long inputEnergy;
        public final int duration;
        public final long power;
        public final ItemStack output;
        public final FluidStack fluid;
        public final AStack[] ingredients;

        public PlasmaForgeRecipe(String name, long inputEnergy, int duration, long power,
                                 ItemStack output, FluidStack fluid, AStack... ingredients) {
            this.name = name;
            this.inputEnergy = inputEnergy;
            this.duration = duration;
            this.power = power;
            this.output = output;
            this.fluid = fluid;
            this.ingredients = ingredients;
        }
    }
}
"""
    (PORT_REC / "PlasmaForgeRecipes.java").write_text(java)
    return ok


def assets() -> None:
    items = [
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
    ]
    tex_src = CE_ASSETS / "textures" / "items"
    tex_dst = ASSETS / "textures" / "item"
    tex_dst.mkdir(parents=True, exist_ok=True)
    models = ASSETS / "models" / "item"
    models.mkdir(parents=True, exist_ok=True)
    aliases = {
        "casing_small": ["casing.small.png", "casing_small.png"],
        "casing_large": ["casing.large.png", "casing_large.png"],
        "casing_small_steel": ["casing.small_steel.png", "casing_small_steel.png"],
        "casing_large_steel": ["casing.large_steel.png", "casing_large_steel.png"],
        "casing_shotshell": ["casing.shotshell.png", "casing_shotshell.png"],
        "casing_buckshot": ["casing.buckshot.png", "casing_buckshot.png"],
        "casing_buckshot_advanced": ["casing.buckshot_advanced.png", "casing_buckshot_advanced.png"],
        "sulfur": ["sulfur.png"],
        "niter": ["niter.png"],
    }
    for name in items:
        dest = tex_dst / f"{name}.png"
        if not dest.exists():
            for cand in aliases.get(name, [f"{name}.png"]):
                src = tex_src / cand
                if src.exists():
                    shutil.copy2(src, dest)
                    break
        if not (models / f"{name}.json").exists():
            (models / f"{name}.json").write_text(json.dumps({
                "parent": "minecraft:item/generated",
                "textures": {"layer0": f"hbm:item/{name}"},
            }, indent=2) + "\n")
    for blk in (
        "machine_ammo_press", "machine_arc_welder", "machine_soldering_station", "fusion_plasma_forge",
        "machine_flare", "machine_catalytic_cracker", "machine_coker", "machine_vacuum_distill",
        "machine_catalytic_reformer", "machine_hydrotreater", "machine_radiolysis",
        "icf_laser_component_casing", "icf_laser_component_port", "icf_laser_component_cell",
        "icf_laser_component_emitter", "icf_laser_component_capacitor", "icf_laser_component_turbo",
        "icf_component_0", "icf_component_1", "icf_component_3", "struct_icf_core",
        "dfc_core", "dfc_emitter", "dfc_receiver", "dfc_injector", "dfc_stabilizer",
        "sliding_blast_door_legacy", "large_vehicle_door", "water_door", "qe_containment",
        "qe_sliding_door", "round_airlock_door", "secure_access_door", "sliding_seal_door",
        "cargo_door", "silo_hatch", "silo_hatch_large", "transition_seal",
    ):
        (ASSETS / "models" / "block").mkdir(parents=True, exist_ok=True)
        (ASSETS / "blockstates").mkdir(parents=True, exist_ok=True)
        (ASSETS / "models" / "block" / f"{blk}.json").write_text(json.dumps({
            "parent": "minecraft:block/cube_all",
            "textures": {"all": "hbm:block/block_steel"},
        }, indent=2) + "\n")
        (models / f"{blk}.json").write_text(json.dumps({"parent": f"hbm:block/{blk}"}, indent=2) + "\n")
        (ASSETS / "blockstates" / f"{blk}.json").write_text(json.dumps({
            "variants": {"": {"model": f"hbm:block/{blk}"}}
        }, indent=2) + "\n")
    lang = ASSETS / "lang" / "en_us.json"
    gen = REPO / "src" / "generated" / "resources" / "assets" / "hbm" / "lang" / "en_us.json"
    extra = {
        "item.hbm.sulfur": "Sulfur",
        "item.hbm.niter": "Niter",
        "item.hbm.casing_small": "Small Casing",
        "item.hbm.casing_large": "Large Casing",
        "item.hbm.casing_small_steel": "Small Steel Casing",
        "item.hbm.casing_large_steel": "Large Steel Casing",
        "item.hbm.casing_shotshell": "Shotshell Casing",
        "item.hbm.casing_buckshot": "Buckshot Casing",
        "item.hbm.casing_buckshot_advanced": "Advanced Buckshot Casing",
        "block.hbm.machine_ammo_press": "Ammo Press",
        "block.hbm.machine_arc_welder": "Arc Welder",
        "block.hbm.machine_soldering_station": "Soldering Station",
        "block.hbm.fusion_plasma_forge": "Plasma Heater / Forge",
        "container.machineAmmoPress": "Ammo Press",
        "container.machineArcWelder": "Arc Welder",
        "container.machineSolderingStation": "Soldering Station",
        "container.machinePlasmaForge": "Plasma Forge",
        "item.hbm.upgrade_template": "Machine Upgrade Template",
        "item.hbm.neutron_reflector": "Neutron Reflector",
        "block.hbm.icf_laser_component_casing": "ICF Laser Casing",
        "block.hbm.icf_laser_component_port": "ICF Laser Port",
        "block.hbm.icf_laser_component_cell": "ICF Laser Cell",
        "block.hbm.icf_laser_component_emitter": "ICF Laser Flash Tube",
        "block.hbm.icf_laser_component_capacitor": "ICF Laser Capacitor",
        "block.hbm.icf_laser_component_turbo": "ICF Laser Turbocharger",
        "block.hbm.sliding_blast_door_legacy": "Sliding Blast Door (Legacy)",
    }
    for path in (lang, gen):
        if not path.exists():
            continue
        data = json.loads(path.read_text())
        for k, v in extra.items():
            data.setdefault(k, v)
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n")


def ammo_press_craft() -> None:
    # CE CraftingManager.java:332
    p = DATA / "recipe" / "machine_ammo_press.json"
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps({
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["IPI", "C C", "SSS"],
        "key": {
            "I": {"item": "minecraft:iron_ingot"},
            "P": {"item": "minecraft:piston"},
            "C": {"item": "hbm:ingot_copper"},
            "S": {"item": "minecraft:stone"},
        },
        "result": {"id": "hbm:machine_ammo_press", "count": 1},
    }, indent=2) + "\n")


def main() -> None:
    kn = known()
    assets()
    ammo_press_craft()
    a = gen_ammo(kn)
    w = gen_arc(kn)
    s = gen_solder(kn)
    p = gen_plasma(kn)
    ok, skip, reasons = write_assembler(kn)
    print(f"ammo={a} welder={w} solder={s} plasma={p} assembler written={ok} skipped={skip} {reasons}")


if __name__ == "__main__":
    main()
