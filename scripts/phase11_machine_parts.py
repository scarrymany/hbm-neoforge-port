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

EXPENSIVE = [
    "item_expensive_steel_plating", "item_expensive_heavy_frame", "item_expensive_circuit",
    "item_expensive_lead_plating", "item_expensive_ferro_plating", "item_expensive_computer",
    "item_expensive_bronze_tubes", "item_expensive_plastic", "item_expensive_gold_dust",
    "item_expensive_degenerate_matter",
]
PART_GENERIC = [
    "part_generic_piston_pneumatic", "part_generic_piston_hydraulic", "part_generic_piston_electric",
    "part_generic_lde", "part_generic_hde", "part_generic_glass_polarized",
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
    "AL": ["aluminum", "aluminium"],
    "ND": ["neodymium"],
    "SBD": ["schrabidate"],
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
    "FIBER": ["fiberglass"],
    "EUPH": ["euphemium"],
    "DNT": ["dineutronium"],
    "AT": ["astatine"],
    "VOLCANIC": ["volcanic"],
    "OSMIRIDIUM": ["osmiridium"],
    "CDALLOY": ["cdalloy"],
    "ANY_HIGHEXPLOSIVE": [],
    "ANY_SMOKELESS": [],
    "ANY_RUBBER": ["rubber"],
    "ANY_RESISTANTALLOY": ["tcalloy", "cdalloy"],
    "WC": ["tungsten_carbide"],
    "ANY_CONCRETE": ["concrete_smooth"],
    "ANY_TAR": [],
    "ANY_PLASTICEXPLOSIVE": [],
    "ANY_BISMOID": ["bismuth"],
    "ANY_BISMOIDBRONZE": ["bismuthbronze"],
    "TA": ["tantalium", "tantalum"],
    "LI": ["lithium"],
    "NP237": ["neptunium"],
    "SA327": ["solinium"],
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
    "U": ["uranium"],
    "PU": ["plutonium"],
    "SR": ["strontium"],
    "CS137": ["caesium137", "cs137"],
    "U238": ["uranium238", "u238"],
    "KNO": ["niter"],
    "S": ["sulfur"],
    "I": ["iodine"],
    "F": ["fluorite"],
    "BR": ["bromine"],
    "CA": ["calcium"],
    "P_RED": ["fire"],
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
    "mechanism": ["{m}_gun_mechanism", "{m}_mechanism", "gun_mechanism_{m}"],
    "dust": ["powder_{m}", "{m}_dust", "dust_{m}"],
    "gem": ["gem_{m}", "{m}_gem"],
    "nugget": ["nugget_{m}", "{m}_nugget"],
    "billet": ["billet_{m}", "{m}_billet"],
    "block": ["{m}_block", "block_{m}"],
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
    "ModItems.upgrade_speed_1": "upgrade_speed_1",
    "ModItems.upgrade_speed_2": "upgrade_speed_2",
    "ModItems.upgrade_speed_3": "upgrade_speed_3",
    "ModItems.upgrade_overdrive_1": "upgrade_overdrive_1",
    "ModItems.early_explosive_lenses": "early_explosive_lenses",
    "ModItems.fleija_propellant": "fleija_propellant",
    "ModBlocks.det_cord": "det_cord",
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
    "ModBlocks.vault_door": "vault_door",
    "ModBlocks.blast_door": "blast_door",
    "ModBlocks.fire_door": "fire_door",
    "ModBlocks.sliding_blast_door": "sliding_blast_door",
    "ModBlocks.sliding_blast_door_legacy": "sliding_blast_door_legacy",
    "ModBlocks.large_vehicle_door": "large_vehicle_door",
    "ModBlocks.water_door": "water_door",
    "ModBlocks.qe_containment": "qe_containment",
    "ModBlocks.qe_sliding_door": "qe_sliding_door",
    "ModBlocks.round_airlock_door": "round_airlock_door",
    "ModBlocks.secure_access_door": "secure_access_door",
    "ModBlocks.sliding_seal_door": "sliding_seal_door",
    "ModBlocks.cargo_door": "cargo_door",
    "ModBlocks.silo_hatch": "silo_hatch",
    "ModBlocks.silo_hatch_large": "silo_hatch_large",
    "ModBlocks.transition_seal": "transition_seal",
    "ModBlocks.machine_flare": "machine_flare",
    "ModBlocks.machine_catalytic_cracker": "machine_catalytic_cracker",
    "ModBlocks.machine_coker": "machine_coker",
    "ModBlocks.machine_vacuum_distill": "machine_vacuum_distill",
    "ModBlocks.machine_catalytic_reformer": "machine_catalytic_reformer",
    "ModBlocks.machine_hydrotreater": "machine_hydrotreater",
    "ModBlocks.machine_radiolysis": "machine_radiolysis",
    "ModBlocks.icf_controller": "machine_icf_controller",
    "ModBlocks.struct_icf_core": "struct_icf_core",
    "ModBlocks.dfc_core": "dfc_core",
    "ModBlocks.dfc_emitter": "dfc_emitter",
    "ModBlocks.dfc_receiver": "dfc_receiver",
    "ModBlocks.dfc_injector": "dfc_injector",
    "ModBlocks.dfc_stabilizer": "dfc_stabilizer",
    "ModBlocks.emp_bomb": "emp_bomb",
    "ModItems.upgrade_template": "upgrade_template",
    "ModItems.neutron_reflector": "neutron_reflector",
    "ModItems.missile_assembly": "missile_assembly",
    "ModItems.thruster_small": "thruster_small",
    "ModItems.thruster_medium": "thruster_medium",
    "ModItems.thruster_large": "thruster_large",
    "ModItems.fuel_tank_small": "fuel_tank_small",
    "ModItems.fuel_tank_medium": "fuel_tank_medium",
    "ModItems.fuel_tank_large": "fuel_tank_large",
    "ModItems.warhead_generic_small": "warhead_generic_small",
    "ModItems.warhead_incendiary_small": "warhead_incendiary_small",
    "ModItems.warhead_cluster_small": "warhead_cluster_small",
    "ModItems.warhead_buster_small": "warhead_buster_small",
    "ModItems.warhead_generic_medium": "warhead_generic_medium",
    "ModItems.warhead_incendiary_medium": "warhead_incendiary_medium",
    "ModItems.warhead_cluster_medium": "warhead_cluster_medium",
    "ModItems.warhead_buster_medium": "warhead_buster_medium",
    "ModItems.warhead_generic_large": "warhead_generic_large",
    "ModItems.warhead_incendiary_large": "warhead_incendiary_large",
    "ModItems.warhead_cluster_large": "warhead_cluster_large",
    "ModItems.warhead_buster_large": "warhead_buster_large",
    "ModItems.warhead_nuclear": "warhead_nuclear",
    "ModItems.warhead_mirv": "warhead_mirv",
    "ModItems.warhead_volcano": "warhead_volcano",
    "ModBlocks.machine_radar": "machine_radar",
    "ModBlocks.machine_radar_large": "machine_radar_large",
    "ModBlocks.machine_purex": "machine_purex",
    "ModBlocks.machine_liquefactor": "machine_liquefactor",
    "ModItems.pellet_charged": "pellet_charged",
    "ModItems.sat_base": "sat_base",
    "ModItems.sat_head_mapper": "sat_head_mapper",
    "ModItems.sat_head_scanner": "sat_head_scanner",
    "ModItems.sat_head_radar": "sat_head_radar",
    "ModItems.sat_head_laser": "sat_head_laser",
    "ModItems.sat_head_resonator": "sat_head_resonator",
    "ModItems.photo_panel": "photo_panel",
    "ModItems.ballistite": "ballistite",
    "ModItems.thruster_nuclear": "thruster_nuclear",
    "ModItems.entanglement_kit": "entanglement_kit",
    "ModItems.tank_steel": "tank_steel",
    "ModItems.pellet_buckshot": "pellet_buckshot",
    "ModItems.pellet_cluster": "pellet_cluster",
    "ModItems.seg_10": "seg_10",
    "ModItems.seg_15": "seg_15",
    "ModItems.seg_20": "seg_20",
    "ModItems.fluorite": "fluorite",
    "ModItems.ducttape": "ducttape",
    "ModItems.rod_empty": "rod_empty",
    "ModItems.dysfunctional_reactor": "dysfunctional_reactor",
    "ModItems.spawn_chopper": "chopper",
    "ModItems.rocket_fuel": "rocket_fuel",
    "ModItems.missile_stealth": "missile_stealth",
    "ModItems.mp_thruster_10_kerosene": "mp_thruster_10_kerosene",
    "ModItems.mp_thruster_10_solid": "mp_thruster_10_solid",
    "ModItems.mp_thruster_10_xenon": "mp_thruster_10_xenon",
    "ModItems.mp_thruster_15_kerosene": "mp_thruster_15_kerosene",
    "ModItems.mp_thruster_15_kerosene_dual": "mp_thruster_15_kerosene_dual",
    "ModItems.mp_thruster_15_kerosene_triple": "mp_thruster_15_kerosene_triple",
    "ModItems.mp_thruster_15_solid": "mp_thruster_15_solid",
    "ModItems.mp_thruster_15_solid_hexdecuple": "mp_thruster_15_solid_hexdecuple",
    "ModItems.mp_thruster_15_hydrogen": "mp_thruster_15_hydrogen",
    "ModItems.mp_thruster_15_hydrogen_dual": "mp_thruster_15_hydrogen_dual",
    "ModItems.mp_thruster_15_balefire_short": "mp_thruster_15_balefire_short",
    "ModItems.mp_thruster_15_balefire": "mp_thruster_15_balefire",
    "ModItems.mp_thruster_15_balefire_large": "mp_thruster_15_balefire_large",
    "ModItems.mp_thruster_20_kerosene": "mp_thruster_20_kerosene",
    "ModItems.mp_thruster_20_kerosene_dual": "mp_thruster_20_kerosene_dual",
    "ModItems.mp_thruster_20_kerosene_triple": "mp_thruster_20_kerosene_triple",
    "ModItems.mp_thruster_20_solid": "mp_thruster_20_solid",
    "ModItems.mp_thruster_20_solid_multi": "mp_thruster_20_solid_multi",
    "ModItems.mp_thruster_20_solid_multier": "mp_thruster_20_solid_multier",
    "ModItems.mp_fuselage_10_kerosene": "mp_fuselage_10_kerosene",
    "ModItems.mp_fuselage_10_long_kerosene": "mp_fuselage_10_long_kerosene",
    "ModItems.mp_fuselage_10_solid": "mp_fuselage_10_solid",
    "ModItems.mp_fuselage_10_long_solid": "mp_fuselage_10_long_solid",
    "ModItems.mp_fuselage_10_xenon": "mp_fuselage_10_xenon",
    "ModItems.mp_fuselage_10_15_kerosene": "mp_fuselage_10_15_kerosene",
    "ModItems.mp_fuselage_10_15_solid": "mp_fuselage_10_15_solid",
    "ModItems.mp_fuselage_10_15_hydrogen": "mp_fuselage_10_15_hydrogen",
    "ModItems.mp_fuselage_10_15_balefire": "mp_fuselage_10_15_balefire",
    "ModItems.mp_fuselage_15_kerosene": "mp_fuselage_15_kerosene",
    "ModItems.mp_fuselage_15_solid": "mp_fuselage_15_solid",
    "ModItems.mp_fuselage_15_hydrogen": "mp_fuselage_15_hydrogen",
    "ModItems.mp_fuselage_15_20_kerosene": "mp_fuselage_15_20_kerosene",
    "ModItems.mp_fuselage_15_20_solid": "mp_fuselage_15_20_solid",
    "ModItems.mp_warhead_10_he": "mp_warhead_10_he",
    "ModItems.mp_warhead_10_incendiary": "mp_warhead_10_incendiary",
    "ModItems.mp_warhead_10_buster": "mp_warhead_10_buster",
    "ModItems.mp_warhead_10_nuclear": "mp_warhead_10_nuclear",
    "ModItems.mp_warhead_10_nuclear_large": "mp_warhead_10_nuclear_large",
    "ModItems.mp_warhead_10_taint": "mp_warhead_10_taint",
    "ModItems.mp_warhead_10_cloud": "mp_warhead_10_cloud",
    "ModItems.mp_warhead_15_he": "mp_warhead_15_he",
    "ModItems.mp_warhead_15_incendiary": "mp_warhead_15_incendiary",
    "ModItems.mp_warhead_15_nuclear": "mp_warhead_15_nuclear",
    "ModItems.mp_warhead_15_n2": "mp_warhead_15_n2",
    "ModItems.mp_warhead_15_balefire": "mp_warhead_15_balefire",
    "ModBlocks.machine_supercomputer": "machine_supercomputer",
    "ModBlocks.machine_arc_furnace": "machine_arc_furnace",
    "ModBlocks.machine_compressor": "machine_compressor",
    "ModBlocks.machine_compressor_compact": "machine_compressor_compact",
    "ModBlocks.machine_epress": "machine_epress",
    "ModBlocks.machine_ore_slopper": "machine_ore_slopper",
    "ModBlocks.machine_mining_laser": "machine_mining_laser",
    "ModBlocks.machine_teleporter": "machine_teleporter",
    "ModBlocks.machine_satlink": "machine_satlink",
    "ModBlocks.machine_forcefield": "machine_forcefield",
    "ModBlocks.machine_strand_caster": "machine_strand_caster",
    "ModBlocks.machine_assembly_factory": "machine_assembly_factory",
    "ModBlocks.machine_chemical_factory": "machine_chemical_factory",
    "ModBlocks.machine_turbofan": "machine_turbofan",
    "ModBlocks.machine_hephaestus": "machine_hephaestus",
    "ModBlocks.machine_chungus": "machine_chungus",
    "ModBlocks.machine_radgen": "machine_radgen",
    "ModBlocks.machine_pyrooven": "machine_pyrooven",
    "ModBlocks.machine_fluidtank": "machine_fluidtank",
    "ModBlocks.machine_bigasstank": "machine_bigasstank",
    "ModBlocks.machine_exposure_chamber": "machine_exposure_chamber",
    "ModBlocks.reactor_research": "reactor_research",
    "ModBlocks.reactor_zirnox": "reactor_zirnox",
    "ModBlocks.seal_frame": "seal_frame",
    "ModBlocks.seal_controller": "seal_controller",
    "ModBlocks.vitrified_barrel": "vitrified_barrel",
    "ModBlocks.struct_torus_core": "struct_torus_core",
    "ModBlocks.fusion_klystron": "fusion_klystron",
    "ModBlocks.fusion_collector": "fusion_collector",
    "ModBlocks.fusion_breeder": "fusion_breeder",
    "ModBlocks.fusion_boiler": "fusion_boiler",
    "ModBlocks.fusion_mhdt": "fusion_mhdt",
    "ModBlocks.fusion_coupler": "fusion_coupler",
    "ModBlocks.watz_element": "watz_element",
    "ModBlocks.watz_cooler": "watz_cooler",
    "ModItems.sat_gerald": "sat_gerald",
    "ModItems.sat_chip": "sat_chip",
    "ModItems.schrabidium_hammer": "schrabidium_hammer",
    "ModItems.fluid_barrel_full": "fluid_barrel_full",
    "ModBlocks.fusion_torus": "fusion_torus",
    "ModBlocks.machine_precass": "machine_precass",
    "ModBlocks.machine_battery_redd": "machine_battery_redd",
    "ModBlocks.machine_transformer": "machine_transformer",
    "ModBlocks.machine_transformer_20": "machine_transformer_20",
    "ModBlocks.machine_transformer_dnt": "machine_transformer_dnt",
    "ModBlocks.machine_transformer_dnt_20": "machine_transformer_dnt_20",
    "ModItems.biomass": "biomass",
    "ModItems.biomass_compressed": "biomass_compressed",
    "ModItems.nuclear_waste_tiny": "nuclear_waste_tiny",
    "ModItems.nuclear_waste_vitrified": "nuclear_waste_vitrified",
    "ModBlocks.machine_fracking_tower": "machine_fracking_tower",
    "ModBlocks.machine_well": "machine_well",
    "ModBlocks.machine_pumpjack": "machine_pumpjack",
    "ModBlocks.machine_refinery": "machine_refinery",
    "ModBlocks.block_meteor": "block_meteor",
    "ModBlocks.deco_steel": "deco_steel",
    "ModBlocks.asphalt": "asphalt",
    "ModBlocks.reinforced_laminate": "reinforced_laminate",
}

EXPENSIVE_ENUM = {
    "STEEL_PLATING": "item_expensive_steel_plating",
    "HEAVY_FRAME": "item_expensive_heavy_frame",
    "CIRCUIT": "item_expensive_circuit",
    "LEAD_PLATING": "item_expensive_lead_plating",
    "FERRO_PLATING": "item_expensive_ferro_plating",
    "COMPUTER": "item_expensive_computer",
    "BRONZE_TUBES": "item_expensive_bronze_tubes",
    "PLASTIC": "item_expensive_plastic",
    "GOLD_DUST": "item_expensive_gold_dust",
    "DEGENERATE_MATTER": "item_expensive_degenerate_matter",
}

PART_GENERIC_ENUM = {
    "PISTON_PNEUMATIC": "part_generic_piston_pneumatic",
    "PISTON_HYDRAULIC": "part_generic_piston_hydraulic",
    "PISTON_ELECTRIC": "part_generic_piston_electric",
    "LDE": "part_generic_lde",
    "HDE": "part_generic_hde",
    "GLASS_POLARIZED": "part_generic_glass_polarized",
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
    "GLASS": "minecraft:glass",
}

ICF_LASER_ENUM = {
    "CASING": "icf_laser_component_casing",
    "PORT": "icf_laser_component_port",
    "CELL": "icf_laser_component_cell",
    "EMITTER": "icf_laser_component_emitter",
    "CAPACITOR": "icf_laser_component_capacitor",
    "TURBO": "icf_laser_component_turbo",
}

ICF_COMPONENT_META = {
    "0": "icf_component_0",
    "1": "icf_component_1",
    "3": "icf_component_3",
}

FUSION_COMPONENT_META = {
    "0": "fusion_component_0",
    "2": "fusion_component_2",
    "3": "fusion_component_3",
}

SAT_TYPE = {
    "SPY": "sat_mapper",
    "SCANNER": "sat_scanner",
    "RADAR": "sat_radar",
    "DEATH_RAY": "sat_laser",
    "XENIUM_RESONATOR": "sat_resonator",
    "RELAY": "sat_relay",
    "MINER_ASTRO": "sat_miner",
    "MINER_LUNAR": "sat_lunar_miner",
    "DETECTOR": "satellite_detector",
    "PRECISION_LASER": "satellite_precision_laser",
    "RAY_SCAN": "satellite_ray_scan",
    "SCIENCE": "satellite_science",
    "SCIENCE_SENSOR": "satellite_science_sensor",
}

KEY_DYES = {
    "KEY_RED": "minecraft:red_dye",
    "KEY_YELLOW": "minecraft:yellow_dye",
    "KEY_GREEN": "minecraft:green_dye",
    "KEY_BLACK": "minecraft:black_dye",
    "KEY_WHITE": "minecraft:white_dye",
    "KEY_GRAY": "minecraft:gray_dye",
    "KEY_GREY": "minecraft:gray_dye",
    "KEY_BLACK": "minecraft:black_dye",
    "KEY_ORANGE": "minecraft:orange_dye",
    "KEY_LIGHTGRAY": "minecraft:light_gray_dye",
    "KEY_LIGHTGREY": "minecraft:light_gray_dye",
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
        r'registerBlock|registerMachine|registerResource|registerBillet|reg1|'
        r'registerPowder|registerFuelPowder|registerUpgrade|registerCasing|ore|reg|parts1|parts|'
        r'fuel|consumeFx|consume|cladding|casing|part|standard)\(\s*"([a-z][a-z0-9_]*)"'
    )
    for p in java.rglob("*.java"):
        extra.update(helper.findall(p.read_text(errors="ignore")))
    packs = {
        "battery_redstone_pack", "battery_lead_pack", "battery_lithium_pack",
        "battery_sodium_pack", "battery_schrabidium_pack", "battery_quantum_pack",
        "capacitor_copper_pack", "capacitor_gold_pack", "capacitor_niobium_pack",
        "capacitor_tantalum_pack", "capacitor_bismuth_pack", "capacitor_spark_pack",
    }
    computed = {
        "pellet_charged", "biomass", "biomass_compressed",
        "fuel_additive_antiknock", "fuel_additive_deicer",
        "nuclear_waste_tiny", "nuclear_waste_vitrified",
        "machine_purex", "machine_liquefactor",
        "rocket_fuel", "chopper", "sat_relay", "sat_miner", "sat_lunar_miner",
        "satellite_detector", "satellite_precision_laser", "satellite_ray_scan",
        "satellite_science", "satellite_science_sensor",
        "pa_coil_gold", "pa_coil_niobium", "pa_coil_bscco", "pa_coil_chlorophyte",
    }
    for d in ("steel", "steel_diamond", "hss", "hss_diamond", "desh", "desh_diamond",
              "tcalloy", "tcalloy_diamond", "ferro", "ferro_diamond"):
        computed.add(f"drillbit_{d}")
    for p in ("steel", "dura", "desh", "starmetal"):
        computed.add(f"piston_set_{p}")
    for r in ("uranium", "pu239", "plutonium", "source", "boron", "lithium", "detector"):
        computed.add(f"pile_rod_{r}")
    for t in ("meu", "heu233", "heu235", "men", "hen237", "mox", "mep", "hep239", "hep241",
              "mea", "hea242", "hes326", "hes327", "bfb_am_mix", "bfb_pu241"):
        computed.add(f"pwr_fuel_depleted_{t}")
    return items | blocks | extra | packs | computed | set(PARTS) | set(EXPENSIVE) | set(PART_GENERIC) | {f"circuit_{c}" for c in CIRCUITS}


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
    dotted_models = {
        "item_expensive_steel_plating": "item_expensive.steel_plating",
        "item_expensive_heavy_frame": "item_expensive.heavy_frame",
        "item_expensive_circuit": "item_expensive.circuit",
        "item_expensive_lead_plating": "item_expensive.lead_plating",
        "item_expensive_ferro_plating": "item_expensive.ferro_plating",
        "item_expensive_computer": "item_expensive.computer",
        "item_expensive_bronze_tubes": "item_expensive.bronze_tubes",
        "item_expensive_plastic": "item_expensive.plastic",
        "item_expensive_gold_dust": "item_expensive.gold_dust",
        "item_expensive_degenerate_matter": "item_expensive.degenerate_matter",
        "part_generic_piston_pneumatic": "part_generic.piston_pneumatic",
        "part_generic_piston_hydraulic": "part_generic.piston_hydraulic",
        "part_generic_piston_electric": "part_generic.piston_electric",
        "part_generic_lde": "part_generic.lde",
        "part_generic_hde": "part_generic.hde",
        "part_generic_glass_polarized": "part_generic.glass_polarized",
    }
    for pid, tex in dotted_models.items():
        path = models / f"{pid}.json"
        if (tex_root / f"{tex}.png").exists():
            path.write_text(json.dumps({
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
        aliases = {
            "item.hbm.item_expensive_steel_plating": "Bolted Steel Plating",
            "item.hbm.item_expensive_heavy_frame": "Heavy Framework",
            "item.hbm.item_expensive_circuit": "Extensive Circuit Board",
            "item.hbm.item_expensive_lead_plating": "Radiation Resistant Plating",
            "item.hbm.item_expensive_ferro_plating": "Reinforced Ferrouranium Panels",
            "item.hbm.item_expensive_computer": "Mainframe",
            "item.hbm.item_expensive_bronze_tubes": "Bronze Structural Elements",
            "item.hbm.item_expensive_plastic": "Plastic Panels",
            "item.hbm.item_expensive_gold_dust": "Gold Dust (Expensive)",
            "item.hbm.item_expensive_degenerate_matter": "Degenerate Matter",
            "item.hbm.part_generic_piston_pneumatic": "Pneumatic Piston",
            "item.hbm.part_generic_piston_hydraulic": "Hydraulic Piston",
            "item.hbm.part_generic_piston_electric": "Electric Piston",
            "item.hbm.part_generic_lde": "Low-Density Element",
            "item.hbm.part_generic_hde": "Heavy Duty Element",
            "item.hbm.part_generic_glass_polarized": "Polarized Lens",
            "container.machineRadar": "Radar",
            "container.machineRadarLarge": "Large Radar",
            "block.hbm.vault_door": "Vault Door",
            "block.hbm.blast_door": "Blast Door",
            "block.hbm.fire_door": "Fire Door",
            "block.hbm.sliding_blast_door": "Sliding Blast Door",
            "block.hbm.machine_radar": "Radar",
            "block.hbm.machine_radar_large": "Large Radar",
            "block.hbm.machine_purex": "PUREX Reprocessing Plant",
            "block.hbm.machine_liquefactor": "Liquefaction Machine",
            "container.machinePUREX": "PUREX Reprocessing Plant",
            "container.machineLiquefactor": "Liquefaction Machine",
            "item.hbm.pellet_charged": "Charged Pellet",
            "item.hbm.biomass": "Biomass",
            "item.hbm.biomass_compressed": "Compressed Biomass",
            "item.hbm.fuel_additive_antiknock": "Tetraethyllead Antiknock Agent",
            "item.hbm.fuel_additive_deicer": "Deicer",
            "item.hbm.nuclear_waste_tiny": "Tiny Pile of Nuclear Waste",
            "item.hbm.nuclear_waste_vitrified": "Vitrified Nuclear Waste",
            "item.hbm.pwr_fuel_depleted_meu": "Depleted PWR Fuel (MEU)",
            "item.hbm.pwr_fuel_depleted_heu233": "Depleted PWR Fuel (HEU-233)",
            "item.hbm.pwr_fuel_depleted_heu235": "Depleted PWR Fuel (HEU-235)",
            "item.hbm.pwr_fuel_depleted_men": "Depleted PWR Fuel (MEN)",
            "item.hbm.pwr_fuel_depleted_hen237": "Depleted PWR Fuel (HEN-237)",
            "item.hbm.pwr_fuel_depleted_mox": "Depleted PWR Fuel (MOX)",
            "item.hbm.pwr_fuel_depleted_mep": "Depleted PWR Fuel (MEP)",
            "item.hbm.pwr_fuel_depleted_hep239": "Depleted PWR Fuel (HEP-239)",
            "item.hbm.pwr_fuel_depleted_hep241": "Depleted PWR Fuel (HEP-241)",
            "item.hbm.pwr_fuel_depleted_mea": "Depleted PWR Fuel (MEA)",
            "item.hbm.pwr_fuel_depleted_hea242": "Depleted PWR Fuel (HEA-242)",
            "item.hbm.pwr_fuel_depleted_hes326": "Depleted PWR Fuel (HES-326)",
            "item.hbm.pwr_fuel_depleted_hes327": "Depleted PWR Fuel (HES-327)",
            "item.hbm.pwr_fuel_depleted_bfb_am_mix": "Depleted PWR Fuel (BFB Am-Mix)",
            "item.hbm.pwr_fuel_depleted_bfb_pu241": "Depleted PWR Fuel (BFB Pu-241)",
        }
        for k, v in aliases.items():
            if k not in data:
                data[k] = v
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
    if mat in KEY_DYES:
        return KEY_DYES[mat], n
    if mat == "ANY_CONCRETE":
        if "concrete_smooth" in known:
            return "hbm:concrete_smooth", n
        return None
    if mat == "ANY_PLASTIC":
        return "tag:hbm:any_plastic", n
    if mat == "ANY_HARDPLASTIC":
        return "tag:hbm:any_hardplastic", n
    if mat == "ANY_HIGHEXPLOSIVE":
        return "tag:hbm:any_highexplosive", n
    if mat == "ANY_SMOKELESS":
        return "tag:hbm:any_smokeless", n
    if mat == "ANY_TAR":
        return "tag:hbm:any_tar", n
    if mat == "ANY_PLASTICEXPLOSIVE":
        return "tag:hbm:any_highexplosive", n
    if mat == "KEY_PLANKS":
        return "tag:minecraft:planks", n
    if mat == "KEY_ANYPANE":
        return "tag:c:glass_panes", n
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
    if mat == "S" and shape == "dust":
        cands.extend(["sulfur", "powder_sulfur"])
    if mat == "KNO" and shape == "dust":
        cands.extend(["niter", "powder_niter"])
    if mat == "NETHERQUARTZ" and shape == "dust":
        cands.extend(["powder_quartz", "powder_nether_quartz"])
    if mat == "COAL" and shape == "gem":
        return "minecraft:coal", n
    if mat == "P_RED" and shape == "dust":
        cands.extend(["powder_fire", "powder_red_phosphorus"])
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
        cand = f"{en.lower()}_pack"
        return (f"hbm:{cand}", n) if cand in known else None
    if item_name == "item_expensive":
        cid = EXPENSIVE_ENUM.get(enum_name)
        if cid and cid in known:
            return f"hbm:{cid}", n
        cand = f"item_expensive_{enum_name.lower()}"
        return (f"hbm:{cand}", n) if cand in known else None
    if item_name == "part_generic":
        cid = PART_GENERIC_ENUM.get(enum_name)
        if cid and cid in known:
            return f"hbm:{cid}", n
        cand = f"part_generic_{enum_name.lower()}"
        return (f"hbm:{cand}", n) if cand in known else None
    return None


def resolve_stack(expr: str, known: set[str]) -> tuple[str, int] | None:
    expr = expr.strip()
    expr = expr.replace("RecipesCommon.", "")

    m = re.search(
        r"DictFrame\.fromOne\(ModItems\.(item_expensive|part_generic|circuit|drillbit|piston_set|battery_pack),\s*(?:[\w.]+\.)?(\w+)(?:,\s*(\d+))?\)",
        expr,
    )
    if m:
        return resolve_flatten(m.group(1), m.group(2), int(m.group(3) or 1), known)

    m = re.match(
        r"new (?:ItemStack|ComparableStack)\(ModItems\.(fluid_barrel_full|fluid_tank_full|cell)\s*,\s*(\d+)\s*,\s*Fluids\.\w+\.getID\(\)\)",
        expr,
    )
    if m:
        name, n = m.group(1), int(m.group(2))
        return (f"hbm:{name}", n) if name in known else None
    if "Fluids." in expr or "getDict(" in expr or "inputFluids" in expr:
        return None
    if "OreDictManager.getReflector" in expr:
        n = 1
        nm = re.search(r",\s*(\d+)\s*\)\s*$", expr)
        if nm:
            n = int(nm.group(1))
        for cand in ("neutron_reflector", "plate_paa"):
            if cand in known:
                return f"hbm:{cand}", n
        return None

    m = re.search(r"EnumCircuitType\.(\w+)", expr)
    if m and ("circuit" in expr or "DictFrame.fromOne" in expr):
        n = 1
        nm = re.search(r"circuit\s*,\s*(\d+)\s*,", expr)
        if nm:
            n = int(nm.group(1))
        else:
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
        if src == "ModBlocks" and name == "icf_laser_component":
            cid = ICF_LASER_ENUM.get(en)
            return (f"hbm:{cid}", n) if cid and cid in known else None
        if src == "ModBlocks" and name == "icf_component":
            cid = ICF_COMPONENT_META.get(en)
            return (f"hbm:{cid}", n) if cid and cid in known else None
        if src == "ModBlocks" and name == "fusion_component":
            cid = FUSION_COMPONENT_META.get(en)
            return (f"hbm:{cid}", n) if cid and cid in known else None
        if src == "ModItems" and name == "satellite":
            cid = SAT_TYPE.get(en)
            return (f"hbm:{cid}", n) if cid and cid in known else None
        if src == "ModItems" and name == "pa_coil":
            cid = f"pa_coil_{en.lower()}"
            return (f"hbm:{cid}", n) if cid in known else None
        if src == "ModItems" and name == "pile_rod":
            for cand in (f"pile_rod_{en.lower()}", f"pile_rod_mk2_{en.lower()}"):
                if cand in known:
                    return f"hbm:{cand}", n
            return None
        return None

    m = re.match(
        r"new (?:ItemStack|ComparableStack)\(ModItems\.(fluid_barrel_full|fluid_tank_full|cell)\s*,\s*(\d+)\s*,\s*Fluids\.\w+\.getID\(\)\)",
        expr,
    )
    if m:
        name, n = m.group(1), int(m.group(2))
        return (f"hbm:{name}", n) if name in known else None

    m = re.match(
        r"new (?:ItemStack|ComparableStack)\((ModBlocks|ModItems|Blocks|Items)\.(\w+)(?:,\s*(\d+))?\)",
        expr,
    )
    if m:
        return resolve_named(m.group(1), m.group(2), int(m.group(3) or 1), known)

    m = re.match(r"new OreDictStack\((\w+)\.(\w+)\(\)(?:,\s*(\d+))?\)", expr)
    if m:
        return resolve_ore(m.group(1), m.group(2), int(m.group(3) or 1), known)

    m = re.match(r'new OreDictStack\("dustGlowstone"(?:,\s*(\d+))?\)', expr)
    if m:
        return "minecraft:glowstone_dust", int(m.group(1) or 1)

    m = re.match(r"new OreDictStack\((KEY_\w+)(?:,\s*(\d+))?\)", expr)
    if m:
        key = m.group(1)
        n = int(m.group(2) or 1)
        if key in KEY_DYES:
            return KEY_DYES[key], n
        if key == "KEY_PLANKS":
            return "tag:minecraft:planks", n
        if key == "KEY_ANYPANE":
            return "tag:c:glass_panes", n

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
        out = inn = infl = outfl = None
        pos = 0
        while pos < len(chunk):
            om = re.search(r"\.(outputItems|inputItems|inputFluids|outputFluids)\(", chunk[pos:])
            if not om:
                break
            abs_par = pos + om.end() - 1
            taken = _take_call_args(chunk, abs_par)
            if not taken:
                break
            body, nxti = taken
            kind = om.group(1)
            if kind == "outputItems" and out is None:
                out = body
            elif kind == "inputItems" and inn is None:
                inn = body
            elif kind == "inputFluids" and infl is None:
                infl = body
            elif kind == "outputFluids" and outfl is None:
                outfl = body
            pos = nxti
        if out and inn:
            recipes.append({"name": name, "duration": dur, "power": pow_, "out": out, "inn": inn,
                            "infl": infl, "outfl": outfl, "skip": None})
        else:
            recipes.append({"name": name, "duration": dur, "power": pow_, "out": out, "inn": inn,
                            "infl": infl, "outfl": outfl, "skip": "parse"})
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


def resolve_fluid(expr: str) -> dict | None:
    m = re.search(r"Fluids\.(\w+)\s*,\s*(\d[\d_]*)", expr)
    if not m:
        return None
    return {"type": m.group(1), "fill": int(m.group(2).replace("_", ""))}


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
        inputs = []
        for iid, c in inns:
            if iid.startswith("tag:"):
                inputs.append({"item": {"tag": iid[4:]}, "count": c})
            else:
                inputs.append({"item": {"item": iid}, "count": c})
        payload = {
            "type": "hbm:assembler",
            "inputs": inputs,
            "output": {"id": oid, "count": oc},
            "duration": r["duration"],
            "power": r["power"],
        }
        if r.get("infl"):
            fargs = split_args(r["infl"])
            fluids = [resolve_fluid(a) for a in fargs]
            if any(x is None for x in fluids):
                reasons["fluid_parse"] = reasons.get("fluid_parse", 0) + 1
                skip += 1
                continue
            payload["input_fluids"] = fluids
        if r.get("outfl"):
            fargs = split_args(r["outfl"])
            fluids = [resolve_fluid(a) for a in fargs]
            if any(x is None for x in fluids):
                reasons["fluid_parse"] = reasons.get("fluid_parse", 0) + 1
                skip += 1
                continue
            payload["output_fluids"] = fluids
        slug = r["name"].replace("ass.", "").lower()
        path = out_dir / f"{slug}.json"
        # Never overwrite a hand-tuned leftover; generator circuit-count flatten is lossy.
        if path.exists():
            reasons["exists"] = reasons.get("exists", 0) + 1
            ok += 1
            continue
        path.write_text(json.dumps(payload, indent=2) + "\n")
        ok += 1
    return ok, skip, reasons


def write_shredder(known: set[str]) -> int:
    """CE ShredderRecipes.java setRecipe(Items/Blocks/ModItems, new ItemStack(...))."""
    ce = REPO / "upstream" / "hbm-ce" / "src" / "main" / "java" / "com" / "hbm" / "inventory" / "recipes" / "ShredderRecipes.java"
    if not ce.exists():
        return 0
    text = ce.read_text(errors="replace")
    out_dir = DATA / "recipe" / "shredder"
    out_dir.mkdir(parents=True, exist_ok=True)
    vanilla_in = {
        "IRON_INGOT": "minecraft:iron_ingot", "GOLD_INGOT": "minecraft:gold_ingot",
        "DIAMOND": "minecraft:diamond", "EMERALD": "minecraft:emerald",
        "COAL": "minecraft:coal", "REDSTONE": "minecraft:redstone",
        "QUARTZ": "minecraft:quartz", "BONE": "minecraft:bone",
        "BLAZE_ROD": "minecraft:blaze_rod", "BLAZE_POWDER": "minecraft:blaze_powder",
        "ENDER_PEARL": "minecraft:ender_pearl", "SLIME_BALL": "minecraft:slime_ball",
        "LEATHER": "minecraft:leather", "STRING": "minecraft:string",
        "FEATHER": "minecraft:feather", "GUNPOWDER": "minecraft:gunpowder",
        "SUGAR": "minecraft:sugar", "WHEAT": "minecraft:wheat",
        "BREAD": "minecraft:bread", "PAPER": "minecraft:paper",
        "BOOK": "minecraft:book", "GLASS_BOTTLE": "minecraft:glass_bottle",
        "NETHER_STAR": "minecraft:nether_star", "PRISMARINE_SHARD": "minecraft:prismarine_shard",
        "PRISMARINE_CRYSTALS": "minecraft:prismarine_crystals",
        "CHORUS_FRUIT": "minecraft:chorus_fruit", "POPPED_CHORUS_FRUIT": "minecraft:popped_chorus_fruit",
        "SHULKER_SHELL": "minecraft:shulker_shell", "TOTEM_OF_UNDYING": "minecraft:totem_of_undying",
    }
    vanilla_block = {
        "DIRT": "minecraft:dirt", "GRASS": "minecraft:grass_block",
        "SAND": "minecraft:sand", "GRAVEL": "minecraft:gravel",
        "COBBLESTONE": "minecraft:cobblestone", "STONE": "minecraft:stone",
        "NETHERRACK": "minecraft:netherrack", "SOUL_SAND": "minecraft:soul_sand",
        "GLOWSTONE": "minecraft:glowstone", "OBSIDIAN": "minecraft:obsidian",
        "ICE": "minecraft:ice", "PACKED_ICE": "minecraft:packed_ice",
        "CLAY": "minecraft:clay", "BRICK_BLOCK": "minecraft:bricks",
        "SANDSTONE": "minecraft:sandstone", "END_STONE": "minecraft:end_stone",
        "PRISMARINE": "minecraft:prismarine", "SEA_LANTERN": "minecraft:sea_lantern",
        "MAGMA": "minecraft:magma_block", "NETHER_WART_BLOCK": "minecraft:nether_wart_block",
        "RED_NETHER_BRICK": "minecraft:red_nether_bricks",
        "BONE_BLOCK": "minecraft:bone_block", "CONCRETE": None,
        "WOOL": "minecraft:white_wool", "LOG": "minecraft:oak_log",
        "PLANKS": "minecraft:oak_planks", "LEAVES": "minecraft:oak_leaves",
    }
    n = 0
    for m in re.finditer(
        r"setRecipe\(\s*(Items|Blocks|ModItems|ModBlocks)\.(\w+)\s*,\s*new ItemStack\(\s*(Items|Blocks|ModItems|ModBlocks)\.(\w+)(?:,\s*(\d+))?",
        text,
    ):
        src, sname, dst, dname, cnt = m.group(1), m.group(2), m.group(3), m.group(4), int(m.group(5) or 1)
        inn = None
        out = None
        if src == "Items":
            inn = vanilla_in.get(sname)
        elif src == "Blocks":
            inn = vanilla_block.get(sname)
        else:
            snake = sname.lower()
            inn = f"hbm:{snake}" if snake in known else None
        if dst == "Items":
            out = vanilla_in.get(dname)
        elif dst == "Blocks":
            out = vanilla_block.get(dname)
        else:
            snake = dname.lower()
            out = f"hbm:{snake}" if snake in known else None
        if not inn or not out:
            continue
        slug = inn.split(":")[-1]
        path = out_dir / f"{slug}.json"
        if path.exists():
            continue
        path.write_text(json.dumps({
            "type": "hbm:shredder",
            "input": {"item": inn},
            "output": {"id": out, "count": cnt},
            "duration": 60,
        }, indent=2) + "\n")
        n += 1
    return n


def main() -> None:
    write_models()
    patch_lang()
    write_crafting()
    known = known_ids()
    ok, skip, reasons = write_assembler(known)
    sh = write_shredder(known)
    print(f"assembler ALL written={ok} skipped={skip} known={len(known)} reasons={reasons}")
    print(f"shredder extra written={sh}")


if __name__ == "__main__":
    main()
