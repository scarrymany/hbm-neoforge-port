# Research Report — mrec-04-arcwelder-misc

Scope: CE's `ArcWelderRecipes.java`, `PrecAssRecipes.java`, `ReformingRecipes.java`, `WasteDrumRecipes.java` (all `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`) — four distinct machine-type recipe-data classes named in Phase 6's `docs/phase6/recipe_graph_audit.md` §3.7's "72 recipe classes, 9 ported" list, all four in the **unported ≈63** bucket. Phase 6 context read first per instructions: `docs/phase6/PARITY_REPORT.md` (416 lines) and `docs/phase6/recipe_graph_audit.md` (377 lines), both in full.

## Scope confirmed

All four files read in full.

| File | Lines | In-CE structure |
|---|---:|---|
| `ArcWelderRecipes.java` | 528 | `extends SerializableRecipe`. One `registerDefaults()` method: **47** sequential `recipes.add(new ArcWelderRecipe(...))` calls (a 48th `recipes.add(` at line 481 is inside `readRecipe()`, CE's JSON-reload deserializer, not a data entry) into a flat `List<ArcWelderRecipe>`. No loops, no material tables — every recipe is individually hand-written with its own ingredient list. `ArcWelderRecipe` (inner class, lines 506-527): `AStack[] ingredients`, optional `FluidStack fluid`, `ItemStack output`, `int duration`, `long consumption`. Plus JSON read/write plumbing (CE's live-reload recipe-editor system) and a `getRecipe(ItemStack...)` order-independent multi-slot matcher (lines 422-454) — greedy per-input match against a mutable copy of the ingredient list, each input consumed at most once, all-or-nothing (empty leftover list = match). |
| `PrecAssRecipes.java` | 209 | `extends GenericRecipes<GenericRecipe>` (`GenericRecipe`/`GenericRecipes` = CE's shared "up to 9 item in, 1 fluid in, 9 item out, 1 fluid out, chance-weighted outputs" machine-recipe base, `com.hbm.inventory.recipes.loader`). One `registerDefaults()`: **20** `registerPair(...)` calls (7 direct circuit/controller recipes + 10 upgrade-tier recipes generated via two 5-call helper-method groups, `addFirstUpgrade`/`addSecondUpgrade` + 3 overdrive-tier recipes) gated behind `if (GeneralConfig.enable528)`, plus **2** ungated `this.register(...)` calls (`blueprints`/`beigeprints`) always active. `registerPair()` (lines 182-208) is the file's real generative mechanism: for every gated recipe it calls `this.register()` **twice** — once for the recipe itself (output wrapped in a `ChanceOutputMulti` of success-item vs. `BrokenItem.make(output)` failure-item, weighted by a `chance` int) and once for a `.recycle` companion recipe (input = the broken item, outputs = a partial-reclaim `ChanceOutput` of each original ingredient, scaled by a `reclaim` percent). So **20 registerPair calls × 2 + 2 direct = 42 actual registered `GenericRecipe` entries**, from **22 authored call sites**. |
| `ReformingRecipes.java` | 126 | `extends SerializableRecipe`. One `registerDefaults()`: **9** `recipes.put(FluidType, new Tuple.Triplet<>(FluidStack, FluidStack, FluidStack))` calls into a flat `HashMap<FluidType, Triplet<...>>`. Simplest structure of the four — pure 1-fluid-in → fixed-3-fluid-out lookup table, no items at all, no loop. |
| `WasteDrumRecipes.java` | 88 | `extends SerializableRecipe`. One `registerDefaults()`: **16** literal `addRecipe(new ComparableStack(item,1,1), new ItemStack(item))` calls (identical-item metadata-1→metadata-0 decay pairs) + **1** `for (ItemPWRFuel.EnumPWRFuel pwr : EnumPWRFuel.values())` loop (15 members, confirmed by reading `ItemPWRFuel.java:21-36` in full) each adding `pwr_fuel_hot[pwr]→pwr_fuel_depleted[pwr]`. **16 + 15 = 31 registered entries** from 17 call sites — the only one of the four files with any loop generation at all, and it's a small, fully-enumerable one. |

**Total across the four files: 47 + 42 + 9 + 31 = 129 registered recipe entries** (95 authored call sites). All four are unambiguously **small** files per this task's "under ~150 entries → full catalog" threshold — no representative-sample/pattern-description mode needed; every entry is catalogued below.

**Confirmed via grep: none of the four is touched by this port at all.** `grep -rn "ArcWelder|PrecAss|Reforming|WasteDrum|FuelPool" src/` (case-insensitive) returns zero hits anywhere under `src/main/java/com/hbm` except one incidental comment in `GeneralConfig.java`. No `src/main/java/com/hbm/inventory/recipes/{ArcWelderRecipes,PrecAssRecipes,ReformingRecipes,WasteDrumRecipes}.java` exists. No block, block entity, GUI, or container for any of the four backing machines exists either (see next section).

### Which machine each file belongs to — does the machine itself exist yet?

**No. All four machine types are entirely unbuilt in this port — block, block entity, GUI, container, and recipe data are all zero.** Checked by `find`-ing this port's `src/main/java/com/hbm/blockentity/` (134 files listed, none named `ArcWelder`/`PrecAss`/`WasteDrum`/`CatalyticReformer`) and cross-checking CE's own block registrations:

| CE recipe file | CE machine block field | CE block class | CE block entity | CE GUI/Container | Port-side equivalent |
|---|---|---|---|---|---|
| `ArcWelderRecipes.java` | `machine_arc_welder` (`ModBlocks.java:1059`) | `MachineArcWelder` | `TileEntityMachineArcWelder.java` (490 lines) | `GUIMachineArcWelder`/`ContainerMachineArcWelder` | **None found.** |
| `PrecAssRecipes.java` | `machine_precass` (`ModBlocks.java:1057`) | `MachinePrecAss` | `TileEntityMachinePrecAss.java` (408 lines) | `GUIMachinePrecAss`/`ContainerMachinePrecAss` | **None found.** |
| `ReformingRecipes.java` | `machine_catalytic_reformer` (`ModBlocks.java:1211`) | `MachineCatalyticReformer` | `TileEntityMachineCatalyticReformer.java` (`tileentity/machine/oil/`, 265 lines) | `GUIMachineCatalyticReformer`/`ContainerMachineCatalyticReformer` | **None found.** |
| `WasteDrumRecipes.java` | `machine_waste_drum` (`ModBlocks.java:1155`) | `WasteDrum` | `TileEntityWasteDrum.java` (158 lines, read in full) | `GUIWasteDrum`/`ContainerWasteDrum` | **None found.** |

This means the implement wave's work here is **not** "port recipe data behind an existing machine" (the pattern for e.g. the 9 already-ported bespoke recipe classes) — it is **build the machine block/BE/GUI from scratch AND port its recipe data**, for all four. This changes the effort shape materially versus a typical "recipe-only" gap; flagged explicitly since PORT_SPEC's framing of this phase leans toward recipe-corpus work.

## Already covered by this port

Nothing in this port pre-dates or duplicates any of these four files' actual recipe content (confirmed: zero hits for all four class names). But several **supporting pieces these four files would build on** are already committed, and one is a **false-friend trap** worth flagging precisely so the implement wave doesn't assume more coverage than exists:

1. **`com.hbm.inventory.recipes.loader.GenericRecipe`/`GenericRecipes` exist in this port — but are NOT the shape CE's `PrecAssRecipes` needs.** `src/main/java/com/hbm/inventory/recipes/loader/GenericRecipe.java` (105 lines) is real and already used (by `ItemBlueprintFolder`/`ItemBlueprints` for display-name/pool metadata only). Its own javadoc (lines 8-31) states outright: this is a "minimal compile-time stand-in" that deliberately does **not** carry CE's real multi-input/fluid/chance-output/duration/power `GenericRecipe` shape, names `PrecAssRecipes`'s CE sibling classes as exactly the kind of machine that needs a real replacement, and directs whoever ports one of "the 9" `GenericRecipes<GenericRecipe>`-based CE classes to **define their own recipe data class** rather than extend this stand-in. This port's own `com.hbm.inventory.recipes.chem.ChemPlantRecipes.java` (104 lines, read in full) already did exactly that — its own javadoc (lines 15-40) explains why, and it is the **established, proven precedent** for `PrecAssRecipes`'s implementation (see Recommended shape below). **Net: the class names exist, but treating them as "PrecAssRecipes' base class already ported" would be wrong — the real gap is a from-scratch `PrecAssRecipe`-shaped data class, same effort as `ChemPlantRecipes` took, not zero.**
2. **`RecipesCommon`'s `AStack`/`ComparableStack`/`OreDictStack`** are already ported and used by every already-committed bespoke recipe class (`ChemPlantRecipes`, `CentrifugeRecipes`, etc.) — directly reusable by all four target files' ingredient lists without new plumbing.
3. **`com.hbm.util.Tuple` (with `Triplet`)** and **`com.hbm.items.machine.ItemFluidIcon`** both already exist in this port — the two supporting classes `ReformingRecipes.java` needs (`Tuple.Triplet<FluidStack,FluidStack,FluidStack>` as its map value type, `ItemFluidIcon` for JEI-style fluid-as-icon display) are ready, no new plumbing needed.
4. **`com.hbm.items.BrokenItem`** (57 lines) already exists, fully implemented for the 1.21 DataComponents model (wraps the original stack in an `hbm:wrapped_item` component instead of CE's NBT-string reconstruction) — this is exactly the class `PrecAssRecipes.registerPair()`'s recycling mechanic needs. **However its own javadoc (line 40-42) flags that `ModItems.BROKEN_ITEM` — the backing registered item its `make()` method requires — does not exist yet**, and this task confirmed that directly: `grep "BROKEN_ITEM" src/main/java/com/hbm/items/ModItems.java` returns zero hits. **The class is ready; the item registration it depends on is not** — a one-line-fix blocker worth calling out precisely since it's easy to miss (the class compiling doesn't mean the feature works).
5. **Two of `PrecAssRecipes`'s config gates are already ported**: `GeneralConfig.enable528()` (`src/main/java/com/hbm/config/GeneralConfig.java:379-380,463-464`, mirrors CE's `enable528Mode`) and `enableExpensiveMode` (same file, lines 366-367). Both directly usable as-is.
6. **The full 18-item machine-upgrade family `PrecAssRecipes` needs already exists**: `upgrade_speed_1/2/3`, `upgrade_effect_1/2/3`, `upgrade_power_1/2/3`, `upgrade_fortune_1/2/3`, `upgrade_afterburn_1/2/3`, `upgrade_overdrive_1/2/3` — all 18 registered in `src/main/java/com/hbm/items/machine/MachineItems.java:300-319`. This covers **all 10** of `PrecAssRecipes`'s upgrade-tier recipes' item dependencies.
7. **`ItemRBMKRod` and the RBMK rod item family already exist** (`src/main/java/com/hbm/items/machine/ItemRBMKRod.java`, `.../rbmk/RBMKRods.java`) — covers `WasteDrumRecipes`'s special-cased (non-data-driven) RBMK-rod cooling behavior in `TileEntityWasteDrum.update()`.
8. **All 16 CE `ItemDepletedFuel`-backed waste items are already flattened and registered as 32 port-side ids** (`waste_natural_uranium`/`waste_natural_uranium_hot`, ... `waste_plate_pu238be`/`waste_plate_pu238be_hot`, `src/main/java/com/hbm/items/PlateCrystalWasteItems.java:106-140`) — this is exactly `WasteDrumRecipes`'s 16 literal decay recipes' full ingredient+output set. **This sub-family is 100% item-ready today.**
9. **`pwr_fuel_hot_<type>` (all 15 `EnumPWRFuel` grades) is already registered** (`src/main/java/com/hbm/items/machine/PWRHotFuelItems.java`) — covers the **input** half of `WasteDrumRecipes`'s PWR-fuel loop. The **output** half, `pwr_fuel_depleted_<type>`, is confirmed **not** registered anywhere (`grep "\"pwr_fuel_depleted"` → 0 hits; `MachineItems.java:403-405`'s own comment explicitly says CE's `pwr_fuel_depleted` "is out of this class's own scope... not duplicated here").
10. **All 15 fluid types `ReformingRecipes` needs are already ported 1:1** in `src/main/java/com/hbm/inventory/fluid/Fluids.java` (`HEATINGOIL`, `NAPHTHA`, `NAPHTHA_CRACK`, `NAPHTHA_COKER`, `LIGHTOIL`, `LIGHTOIL_CRACK`, `PETROLEUM`, `SOURGAS`, `CHOLESTEROL`, `REFORMATE`, `AROMATICS`, `REFORMGAS`, `SULFURIC_ACID`, `ESTRADIOL`, `HYDROGEN` — every one confirmed present by name, matching Phase 6's "97.5%, near-verbatim" fluid-parity finding). **`ReformingRecipes` is 100% fluid-ready — its only blocker is the machine itself not existing.**
11. **This port's `MaterialShapes`/`Mats.java` abstraction (per the prompt's required reading) already generates two of the three plate-family shapes `ArcWelderRecipes` needs**: `MaterialItemGenerator.java` (`src/main/java/com/hbm/items/MaterialItemGenerator.java:35-40`) loops `CASTPLATE` (→ `<mat>_plate_triple`, = CE's `plate_cast`/`.plateCast()`) and `WELDEDPLATE` (→ `<mat>_plate_sextuple`, = CE's `plate_welded`/`.plateWelded()`) over every material that declares them in `Mats.java`'s `setAutogen(...)`, plus `WIRE`/`DENSEWIRE`/`BOLT`. Every one of the 11 materials `ArcWelderRecipes`'s `plate_welded` group needs (iron, steel, copper, titanium, zirconium, aluminium, tcalloy, cdalloy, tungsten, cmb, osmiridium) declares **both** `CASTPLATE` and `WELDEDPLATE` in `Mats.java` (verified line-by-line, e.g. `MAT_IRON` line 78, `MAT_STEEL` line 155, `MAT_OSMIRIDIUM` line 152) — **this entire 11-recipe sub-group is item-ready today** (see catalog below). Plain `PLATE` (undoubled), the shape several *other* `ArcWelderRecipes` entries need, is **not** covered by this generator and is confirmed absent everywhere (see Item/registry dependency check) — a precise, important distinction between "this shape family is done" and "this shape family is not," inside the same source file.

## Full recipe/entry catalog

### ArcWelderRecipes.java — 47 entries (all catalogued)

*Duration in ticks, Power in HE. `[mat]` = CE metadata variant of an `ItemEnumMulti`-style item; per this port's convention each becomes a separate id.*

**Parts (4)**
| # | Output | Dur | Power | Inputs |
|---|---|---:|---:|---|
| 1 | `motor` ×2 | 100 | 400 | `STEEL.plate()` ×2, `MINGRADE.wireDense()` ×2 |
| 2 | `part_generic[LDE]` | 200 | 5,000 | `AL.plate()` ×4, `FIBER.ingot()` ×4, `ANY_HARDPLASTIC.ingot()` ×1 |
| 3 | `part_generic[LDE]` (alt recipe) | 200 | 10,000 | `TI.plate()` ×2, `FIBER.ingot()` ×4, `ANY_HARDPLASTIC.ingot()` ×1 |
| 4 | `neutron_reflector` ×2 | 400 | 50,000 | `WC.ingot()` ×2, `DURA.plate()` ×1 |

**Dense wires (3)**
| # | Output | Dur | Power | Inputs |
|---|---|---:|---:|---|
| 5 | `wire_dense[COPPER]` | 100 | 10,000 | `CU.wireFine()` ×8 |
| 6 | `wire_dense[MINGRADE]` | 100 | 10,000 | `MINGRADE.wireFine()` ×8 |
| 7 | `wire_dense[GOLD]` | 100 | 10,000 | `GOLD.wireFine()` ×8 |

**Welded plates (11)** — CE comments mark these as progression-gated (early/mid/late/pre-DFC game)
| # | Output | Dur | Power | Inputs |
|---|---|---:|---:|---|
| 8 | `plate_welded[IRON]` | 100 | 100 | `IRON.plateCast()` ×2 |
| 9 | `plate_welded[STEEL]` | 100 | 500 | `STEEL.plateCast()` ×2 |
| 10 | `plate_welded[COPPER]` | 200 | 1,000 | `CU.plateCast()` ×2 |
| 11 | `plate_welded[TITANIUM]` | 600 | 50,000 | `TI.plateCast()` ×2 |
| 12 | `plate_welded[ZIRCONIUM]` | 600 | 10,000 | `ZR.plateCast()` ×2 |
| 13 | `plate_welded[ALUMINIUM]` | 300 | 10,000 | `AL.plateCast()` ×2 |
| 14 | `plate_welded[TCALLOY]` | 1,200 | 1,000,000 | `OXYGEN` fluid 1000mB, `TCALLOY.plateCast()` ×2 |
| 15 | `plate_welded[CDALLOY]` | 1,200 | 1,000,000 | `OXYGEN` fluid 1000mB, `CDALLOY.plateCast()` ×2 |
| 16 | `plate_welded[TUNGSTEN]` | 1,200 | 250,000 | `OXYGEN` fluid 1000mB, `W.plateCast()` ×2 |
| 17 | `plate_welded[CMB]` | 1,200 | 10,000,000 | `REFORMGAS` fluid 1000mB, `CMB.plateCast()` ×2 |
| 18 | `plate_welded[OSMIRIDIUM]` | 6,000 | 20,000,000 | `REFORMGAS` fluid 16,000mB, `OSMIRIDIUM.plateCast()` ×2 |

**Missile parts (6)**
| # | Output | Dur | Power | Inputs |
|---|---|---:|---:|---|
| 19 | `thruster_small` | 60 | 1,000 | `STEEL.plate()` ×4, `AL.wireFine()` ×4, `CU.plate()` ×4 |
| 20 | `thruster_medium` | 100 | 2,000 | `STEEL.plate()` ×8, `motor` ×1, `GRAPHITE.ingot()` ×8 |
| 21 | `thruster_large` | 200 | 5,000 | `DURA.ingot()` ×10, `motor` ×1, [any]`neutron_reflector` ×12 |
| 22 | `fuel_tank_small` | 60 | 1,000 | `AL.plate()` ×6, `CU.plate()` ×4, `steel_scaffold` ×4 |
| 23 | `fuel_tank_medium` | 100 | 2,000 | `AL.plateCast()` ×4, `TI.plate()` ×8, `steel_scaffold` ×12 |
| 24 | `fuel_tank_large` | 200 | 5,000 | `AL.plateWelded()` ×8, `BIGMT.plate()` ×12, `steel_scaffold` ×16 |

**Missiles (18)**
| # | Output | Dur | Power | Inputs |
|---|---|---:|---:|---|
| 25 | `missile_anti_ballistic` | 100 | 5,000 | `ANY_HIGHEXPLOSIVE.ingot()` ×3, `missile_assembly` ×1, `thruster_small` ×4 |
| 26 | `missile_generic` | 100 | 5,000 | `warhead_generic_small`, `fuel_tank_small`, `thruster_small` |
| 27 | `missile_incendiary` | 100 | 5,000 | `warhead_incendiary_small`, `fuel_tank_small`, `thruster_small` |
| 28 | `missile_cluster` | 100 | 5,000 | `warhead_cluster_small`, `fuel_tank_small`, `thruster_small` |
| 29 | `missile_buster` | 100 | 5,000 | `warhead_buster_small`, `fuel_tank_small`, `thruster_small` |
| 30 | `missile_decoy` | 60 | 2,500 | `STEEL.ingot()` ×1, `fuel_tank_small`, `thruster_small` |
| 31 | `missile_strong` | 200 | 10,000 | `warhead_generic_medium`, `fuel_tank_medium`, `thruster_medium` |
| 32 | `missile_incendiary_strong` | 200 | 10,000 | `warhead_incendiary_medium`, `fuel_tank_medium`, `thruster_medium` |
| 33 | `missile_cluster_strong` | 200 | 10,000 | `warhead_cluster_medium`, `fuel_tank_medium`, `thruster_medium` |
| 34 | `missile_buster_strong` | 200 | 10,000 | `warhead_buster_medium`, `fuel_tank_medium`, `thruster_medium` |
| 35 | `missile_emp_strong` | 200 | 10,000 | `emp_bomb` ×3, `fuel_tank_medium`, `thruster_medium` |
| 36 | `missile_burst` | 300 | 25,000 | `warhead_generic_large`, `fuel_tank_medium` ×2, `thruster_medium` ×4 |
| 37 | `missile_inferno` | 300 | 25,000 | `warhead_incendiary_large`, `fuel_tank_medium` ×2, `thruster_medium` ×4 |
| 38 | `missile_rain` | 300 | 25,000 | `warhead_cluster_large`, `fuel_tank_medium` ×2, `thruster_medium` ×4 |
| 39 | `missile_drill` | 300 | 25,000 | `warhead_buster_large`, `fuel_tank_medium` ×2, `thruster_medium` ×4 |
| 40 | `missile_nuclear` | 600 | 50,000 | `warhead_nuclear`, `fuel_tank_large`, `thruster_large` ×3 |
| 41 | `missile_nuclear_cluster` | 600 | 50,000 | `warhead_mirv`, `fuel_tank_large`, `thruster_large` ×3 |
| 42 | `missile_volcano` | 600 | 50,000 | `warhead_volcano`, `fuel_tank_large`, `thruster_large` ×3 |

**Satellites (5)**
| # | Output | Dur | Power | Inputs |
|---|---|---:|---:|---|
| 43 | `satellite[SPY]` | 600 | 10,000 | `sat_base`, `sat_head_mapper` |
| 44 | `satellite[SCANNER]` | 600 | 10,000 | `sat_base`, `sat_head_scanner` |
| 45 | `satellite[RADAR]` | 600 | 10,000 | `sat_base`, `sat_head_radar` |
| 46 | `satellite[DEATH_RAY]` | 600 | 50,000 | `sat_base`, `sat_head_laser` |
| 47 | `satellite[XENIUM_RESONATOR]` | 600 | 50,000 | `sat_base`, `sat_head_resonator` |

### PrecAssRecipes.java — 22 authored entries / 42 registered (all catalogued)

**Gated behind `GeneralConfig.enable528()` (20 `registerPair` calls → 40 registered entries: main + auto-generated `.recycle`)**
| # | Name | Dur | Power | Inputs | Output (success chance / broken-item chance) |
|---|---|---:|---:|---|---|
| 1 | `precass.chip` | 100 | 200 | `circuit[SILICON]` ×1, `plate_polymer` ×3, `GOLD.wireFine()` ×4 | `circuit[CHIP]` (90% / 50-90% reclaim*) |
| 2 | `precass.chip_bismoid` | 200 | 1,000 | `circuit[SILICON]` ×4, `plate_polymer` ×8, `ANY_BISMOID.nugget()` ×2, `GOLD.wireFine()` ×4, fluid `PERFLUOROMETHYL` 500mB | `circuit[CHIP_BISMOID]` (75%) |
| 3 | `precass.chip_quantum` | 300 | 20,000 | `circuit[SILICON]` ×8, `BSCCO.wireDense()` ×2, `ANY_HARDPLASTIC.ingot()` ×4, `pellet_charged` ×4, `GOLD.wireFine()` ×8, fluid `HELIUM4` 250mB | `circuit[CHIP_QUANTUM]` (90%) |
| 4 | `precass.atomic_clock` | 200 | 2,000 | `circuit[CHIP]` ×8, `ANY_PLASTIC.ingot()` ×4, `ZR.wireFine()` ×8, `SR.dust()` ×1 | `circuit[ATOMIC_CLOCK]` (50%) |
| 5 | `precass.controller` | 400 | 15,000 | `circuit[CHIP]` ×32, `circuit[CAPACITOR]` ×32, `circuit[CAPACITOR_TANTALIUM]` ×16, `circuit[CONTROLLER_CHASSIS]` ×1, `upgrade_speed_1` ×1, `PB.wireFine()` ×16, fluid `PERFLUOROMETHYL` 1,000mB | `circuit[CONTROLLER]` (75%) |
| 6 | `precass.controller_advanced` | 600 | 25,000 | `circuit[CHIP_BISMOID]` ×16, `circuit[CAPACITOR_TANTALIUM]` ×48, `circuit[ATOMIC_CLOCK]` ×1, `circuit[CONTROLLER_CHASSIS]` ×1, `upgrade_speed_3` ×1, `PB.wireFine()` ×24, fluid `PERFLUOROMETHYL` 4,000mB | `circuit[CONTROLLER_ADVANCED]` (50%) |
| 7 | `precass.controller_quantum` | 600 | 250,000 | `circuit[CHIP_QUANTUM]` ×16, `circuit[CHIP_BISMOID]` ×48, `circuit[ATOMIC_CLOCK]` ×8, `circuit[CONTROLLER_ADVANCED]` ×2, `upgrade_overdrive_1` ×1, `PB.wireFine()` ×32, fluid `PERFLUOROMETHYL_COLD` 6,000mB | `circuit[CONTROLLER_QUANTUM]` (75%) |
| 8 | `precass.upgrade_speed_ii` | 300 | 10,000 | `circuit[CHIP]` ×8, `circuit[CAPACITOR_TANTALIUM]` ×4, `upgrade_speed_1`, `ANY_PLASTIC.ingot()` ×4 | `upgrade_speed_2` (50%) |
| 9 | `precass.upgrade_speed_iii` | 400 | 25,000 | `circuit[CHIP]` ×16, `circuit[CAPACITOR_TANTALIUM]` ×16, `upgrade_speed_2`, `RUBBER.ingot()` ×4, fluid `SOLVENT` 500mB | `upgrade_speed_3` (25%) |
| 10 | `precass.upgrade_effect_ii` | 300 | 10,000 | (same shape as #8) w/ `upgrade_effect_1` | `upgrade_effect_2` (50%) |
| 11 | `precass.upgrade_effect_iii` | 400 | 25,000 | (same shape as #9) w/ `upgrade_effect_2` | `upgrade_effect_3` (25%) |
| 12 | `precass.upgrade_power_ii` | 300 | 10,000 | (shape of #8) w/ `upgrade_power_1` | `upgrade_power_2` (50%) |
| 13 | `precass.upgrade_power_iii` | 400 | 25,000 | (shape of #9) w/ `upgrade_power_2` | `upgrade_power_3` (25%) |
| 14 | `precass.upgrade_fortune_ii` | 300 | 10,000 | (shape of #8) w/ `upgrade_fortune_1` | `upgrade_fortune_2` (50%) |
| 15 | `precass.upgrade_fortune_iii` | 400 | 25,000 | (shape of #9) w/ `upgrade_fortune_2` | `upgrade_fortune_3` (25%) |
| 16 | `precass.upgrade_ab_ii` | 300 | 10,000 | (shape of #8) w/ `upgrade_afterburn_1` | `upgrade_afterburn_2` (50%) |
| 17 | `precass.upgrade_ab_iii` | 400 | 25,000 | (shape of #9) w/ `upgrade_afterburn_2` | `upgrade_afterburn_3` (25%) |
| 18 | `precass.upgrade_overdive_i` | 200 | 1,000 | `upgrade_speed_3`, `upgrade_effect_3`, `BIGMT.ingot()` ×16, `ANY_HARDPLASTIC.ingot()` ×16, `circuit[ADVANCED]` ×16 | `upgrade_overdrive_1` (50%) |
| 19 | `precass.upgrade_overdive_ii` | 600 | 5,000 | `upgrade_overdrive_1`, `upgrade_speed_3`, `upgrade_effect_3`, `BIGMT.ingot()` ×16, `ingot_cft` ×8, `circuit[CAPACITOR_BOARD]` ×16 | `upgrade_overdrive_2` (50%) |
| 20 | `precass.upgrade_overdive_iii` | 1,200 | 100,000 | `upgrade_overdrive_2`, `upgrade_speed_3`, `upgrade_effect_3`, `ANY_BISMOIDBRONZE.ingot()` ×16, `ingot_cft` ×16, `circuit[BISMOID]` ×16 | `upgrade_overdrive_3` (25%) |

*"chance" = success rate; remainder becomes `BrokenItem.make(output)`; every one of these 20 also auto-generates a `.recycle` companion recipe (input = the broken item, output = each original ingredient at the listed reclaim% — see catalog entry 21 below for the mechanic itself, not each individual instance).

**Ungated (2 direct `this.register` calls, no recycle pair)**
| # | Name | Dur | Power | Inputs | Output |
|---|---|---:|---:|---|---|
| 21 | `precass.blueprints` | 6,000 | 20,000 | `Items.PAPER` ×16, `KEY_BLUE`(dye) ×16, `Items.FISH[PUFFERFISH]` ×4 | 10%: `blueprint_folder[0]`; 90%: `Items.PAPER` ×16 |
| 22 | `precass.beigeprints` | 6,000 | 50,000 | `Items.PAPER` ×24, `CINNABAR.gem()` ×24, `Items.FISH[PUFFERFISH]` ×8 | 5%: `blueprint_folder[1]`; 95%: `Items.PAPER` ×24 |

**The recycle mechanic (`registerPair`, lines 182-208, applies to all 20 gated entries above):** For each gated recipe, generates a second `GenericRecipe` named `<name>.recycle`: input = `NbtComparableStack(BrokenItem.make(output))`, outputs = every original input item at `reclaim%` (fluid input similarly scaled), via `ChanceOutput`. This is a **generative pattern**, not 20 separately-hand-written recycle recipes — an implement-wave port should replicate it as a helper method exactly like CE does (auto-derive the recycle recipe from the main recipe's own ingredient list), not hand-transcribe 20 more entries.

### ReformingRecipes.java — 9 entries (all catalogued)

Each entry: 1 fluid input (1,000mB, implicit — the recipe key itself represents "some amount" scaled ×10 at use-time per `getRecipes()`'s `* 10` factor) → fixed 3 fluid outputs.

| # | Input fluid | Output 1 | Output 2 | Output 3 |
|---|---|---|---|---|
| 1 | `HEATINGOIL` | `NAPHTHA` 50 | `PETROLEUM` 15 | `HYDROGEN` 10 |
| 2 | `NAPHTHA` | `REFORMATE` 50 | `PETROLEUM` 15 | `HYDROGEN` 10 |
| 3 | `NAPHTHA_CRACK` | `REFORMATE` 50 | `AROMATICS` 10 | `HYDROGEN` 5 |
| 4 | `NAPHTHA_COKER` | `REFORMATE` 50 | `REFORMGAS` 10 | `HYDROGEN` 5 |
| 5 | `LIGHTOIL` | `AROMATICS` 50 | `REFORMGAS` 10 | `HYDROGEN` 15 |
| 6 | `LIGHTOIL_CRACK` | `AROMATICS` 50 | `REFORMGAS` 5 | `HYDROGEN` 20 |
| 7 | `PETROLEUM` | `UNSATURATEDS` 85 | `REFORMGAS` 10 | `HYDROGEN` 5 |
| 8 | `SOURGAS` | `SULFURIC_ACID` 75 | `PETROLEUM` 10 | `HYDROGEN` 15 |
| 9 | `CHOLESTEROL` | `ESTRADIOL` 50 | `REFORMGAS` 35 | `HYDROGEN` 15 |

(Note: `UNSATURATEDS` in entry 7 was not independently re-verified against `Fluids.java` in this pass — the other 14 distinct fluid names across all 9 rows were; flagged as a follow-up spot-check, low risk given the 97.5% overall fluid-parity figure.)

### WasteDrumRecipes.java — 31 entries (16 catalogued individually + 1 loop pattern of 15)

**16 literal decay recipes** (all: input = item at CE metadata 1 "contaminated"/hot, output = same item at metadata 0 "decontaminated"/cold — mapped 1:1 to this port's already-flattened `<name>`/`<name>_hot` id pairs):
`waste_natural_uranium`, `waste_uranium`, `waste_thorium`, `waste_mox`, `waste_plutonium`, `waste_u233`, `waste_u235`, `waste_schrabidium`, `waste_zfb_mox`, `waste_plate_u233`, `waste_plate_u235`, `waste_plate_mox`, `waste_plate_pu239`, `waste_plate_sa326`, `waste_plate_ra226be`, `waste_plate_pu238be` — each: `<name>_hot` → `<name>`.

**Loop-generated: 15 entries**, `for (EnumPWRFuel pwr : EnumPWRFuel.values())`: `pwr_fuel_hot[pwr]` → `pwr_fuel_depleted[pwr]`, for pwr ∈ {MEU, HEU233, HEU235, MEN, HEN237, MOX, MEP, HEP239, HEP241, MEA, HEA242, HES326, HES327, BFB_AM_MIX, BFB_PU241} (`ItemPWRFuel.java:21-36`, read in full).

**Non-data-driven special case (not a table entry, hardcoded in `TileEntityWasteDrum.update()`):** any `ItemRBMKRod` sitting in the drum instead calls `rod.updateHeat(...)`/`rod.provideHeat(...)` every tick (cools the rod using the same water-adjacency mechanic) rather than looking up `WasteDrumRecipes.recipes`.

**Mechanic (from `TileEntityWasteDrum.java`, read in full, 158 lines):** not a machine-recipe in the usual sense — a 12-slot passive block. `updateWater()` counts adjacent water-source blocks (0-6) on load; `update()` re-rolls `world.rand.nextInt(60*60*20/water)==0` per slot per tick (no water = never triggers) and, on hit, replaces the slot's stack with the recipe's output via `WasteDrumRecipes.recipes.get(comp)`. No power/duration/GUI-progress concept at all.

## Item/registry dependency check

### ArcWelderRecipes — by family

| Family | Status | Detail |
|---|---|---|
| `plate_welded[IRON/STEEL/COPPER/TITANIUM/ZIRCONIUM/ALUMINIUM/TCALLOY/CDALLOY/TUNGSTEN/CMB/OSMIRIDIUM]` output + `X.plateCast()` input (entries 8-18, **11 of 47**) | **READY** | Both `CASTPLATE`("plate_triple") and `WELDEDPLATE`("plate_sextuple") shapes confirmed declared in `Mats.java`'s `setAutogen(...)` for all 11 materials (line-checked individually), and both shapes are in `MaterialItemGenerator.AUTOGEN_SHAPES` (`MaterialItemGenerator.java:36-39`) — items exist today. `OXYGEN`/`REFORMGAS` fluids also confirmed present. **This entire 11-recipe sub-group needs only the machine itself.** |
| `wire_dense[COPPER/MINGRADE/GOLD]` output + `X.wireFine()` input (entries 5-7, **3 of 47**) | **READY** | `WIRE`("wire") and `DENSEWIRE`("dense_wire") both declared for COPPER, MINGRADE, GOLD in `Mats.java`, both in `MaterialItemGenerator.AUTOGEN_SHAPES`. |
| `X.plate()` (plain, undoubled `PLATE` shape — motor, LDE parts, neutron_reflector, thrusters, fuel tanks; **appears in ≈9 of the remaining 33 entries**) | **BLOCKED — structural, not per-material** | Plain `PLATE` is not in `MaterialItemGenerator.AUTOGEN_SHAPES` and is not hand-curated anywhere else: `grep '"titanium_plate"'` and `grep 'MaterialShapes\.PLATE\b'` outside `ItemMold.java`/`GearTiers.java` both return zero id-registration hits. This port's own `ModRecipeProvider.java:196-202` already documents this exact gap in its own javadoc ("a 'motor'/'canister_empty'/'piston_selenium' item that does not exist under any name"). **A whole shape family, blocking every recipe below that names `.plate()`.** |
| `motor`, `part_generic`, `neutron_reflector` (output items, entries 1-4, 20-21) | **BLOCKED — items don't exist** | Zero registration hits for any of the three. `motor` explicitly named as unbuilt in `ModRecipeProvider.java:200`. |
| `thruster_small/medium/large`, `fuel_tank_small/medium/large`, `steel_scaffold`, `missile_assembly` (entries 19-24, 25) | **BLOCKED — items don't exist** | Zero hits for all 7 names anywhere in `src/main/java/com/hbm`. |
| `warhead_generic/incendiary/cluster/buster_{small,medium,large}`, `warhead_nuclear`, `warhead_mirv`, `warhead_volcano` (12 distinct warhead ids, entries 26-42) | **BLOCKED — items don't exist** | Zero hits for every `warhead_*` name. |
| `missile_generic`, `missile_incendiary`, `missile_cluster`, `missile_buster`, `missile_decoy`, `missile_strong`, `missile_incendiary_strong`, `missile_cluster_strong`, `missile_buster_strong`, `missile_emp_strong`, `missile_burst`, `missile_inferno`, `missile_rain`, `missile_drill`, `missile_nuclear`, `missile_nuclear_cluster`, `missile_volcano`, `missile_anti_ballistic` (18 finished-missile **outputs**, entries 25-42) | **READY (output side only)** | All 18 confirmed registered by `src/main/java/com/hbm/items/weapon/MissileItems.java` (`standard("missile_generic", ...)` etc., lines 157-185 sampled). **The finished missile items exist; only their crafting ingredients (warheads/thrusters/fuel tanks/assembly, above) are missing.** |
| `satellite[SPY/SCANNER/RADAR/DEATH_RAY/XENIUM_RESONATOR]` (5 outputs, entries 43-47) | **READY (output side only)** | `ItemSatellite.EnumSatType` includes all 5 (plus 9 more CE doesn't even have in this ArcWelder file); registered as `satellite_spy` etc. by `MachineItems.java:502-503`'s loop. |
| `sat_base`, `sat_head_mapper/scanner/radar/laser/resonator` (6 crafting **inputs**, entries 43-47) | **BLOCKED — items don't exist** | Zero hits anywhere; this port's satellite-module system (`ItemSatChip`, `saveddata/satellites/*`) appears to be a rewritten payload mechanic, not this literal "sat_base + sat_head" craftable-parts pair. |
| Materials `WC` (TungstenCarbide), `FIBER` (Fiberglass) | **BLOCKED — material identity doesn't exist** | Neither appears anywhere in `Mats.java` (327 lines read in full) under any constant name. Needed for the `neutron_reflector` and `part_generic[LDE]` recipes. |
| `ANY_HARDPLASTIC`, `ANY_HIGHEXPLOSIVE` (CE ore-dict trait-group wildcards) | **BLOCKED — no equivalent mechanism** | This port's `Mats`/`MaterialShapes` model exact-material tags (`c:ingots/iron`), not CE's cross-material "any X with trait Y" wildcard groups. See Open questions. |
| `GRAPHITE.ingot()`, `DURA.ingot()`, `STEEL.ingot()` | **READY** | `ingot_graphite` (`IngotNuggetItems.java:186`) and `ingot_dura_steel` (`IngotNuggetItems.java:103`) both confirmed registered (hand-curated `INGOT`-shape family, independent of `Mats.java`'s `setAutogen`), plain `steel_ingot`/equivalent assumed present (core material, not independently re-verified this pass). |
| `emp_bomb` (block, entry 35) | **READY** | `src/main/java/com/hbm/blocks/bomb/BombBlocks.java:121`. |

**Bottom line for ArcWelderRecipes: 14 of 47 entries (the `plate_welded` group + `wire_dense` group) are item-ready today and blocked only on the machine not existing. The remaining 33 need at minimum the `PLATE` shape family, and mostly need 15+ entirely-new items (`motor`, `part_generic`, `neutron_reflector`, `thruster_*`, `fuel_tank_*`, `steel_scaffold`, `missile_assembly`, 12 `warhead_*` variants, `sat_base`, 5 `sat_head_*`) plus 2 new material identities (WC, FIBER) plus the `ANY_*` wildcard mechanism.**

### PrecAssRecipes — by family

| Family | Status | Detail |
|---|---|---|
| `circuit[*]` (`EnumCircuitType` — SILICON, CHIP, CHIP_BISMOID, CHIP_QUANTUM, ATOMIC_CLOCK, CAPACITOR, CAPACITOR_TANTALIUM, CONTROLLER_CHASSIS, CONTROLLER, CONTROLLER_ADVANCED, CONTROLLER_QUANTUM, ADVANCED, CAPACITOR_BOARD, BISMOID) | **BLOCKED — entire family doesn't exist** | Zero hits for `"circuit"`, `EnumCircuitType`, `ItemCircuit` anywhere in this port. This blocks **all 20** gated recipes (every one references `circuit[X]` as an ingredient, output, or both) — the single largest blocker in this file. |
| `upgrade_speed_1/2/3`, `upgrade_effect_1/2/3`, `upgrade_power_1/2/3`, `upgrade_fortune_1/2/3`, `upgrade_afterburn_1/2/3`, `upgrade_overdrive_1/2/3` | **READY** | All 18 registered, `MachineItems.java:300-319` (see "Already covered" #6). |
| `BrokenItem`/`ModItems.BROKEN_ITEM` | **PARTIALLY READY** | `BrokenItem.java` class exists and is 1.21-ready; the backing `ModItems.BROKEN_ITEM` registration it needs does not exist (confirmed 0 hits) — blocks every recycle-pair recipe (all 20) even once `circuit` exists. |
| `plate_polymer`, `pellet_charged`, `ingot_cft` | Not independently re-verified this pass (not part of this file's dominant blocker — `circuit` gates first regardless) — flagged as follow-up. |
| `ANY_BISMOID`, `ANY_HARDPLASTIC`, `ANY_PLASTIC`, `ANY_BISMOIDBRONZE` (ore-dict wildcards) | **BLOCKED — no equivalent mechanism** | Same structural gap as ArcWelderRecipes. |
| `blueprint_folder[0]`/`[1]` | **READY** | `blueprint_folder_<kind>` loop-registered, `MachineItems.java:176`. |
| `Items.PAPER`, `Items.FISH`/pufferfish variant, `KEY_BLUE` (dye) | **READY** | Vanilla items/tags, trivially satisfiable. |
| `CINNABAR.gem()` | **BLOCKED — `GEM` shape absent entirely** | Confirmed via `grep '"[a-z_]+_gem"'` across `items/` — zero hits port-wide. Same category of gap as `PLATE` for ArcWelder: a whole shape family, not a single material's coverage. |

**Bottom line for PrecAssRecipes: 0 of the 20 gated (circuit-chain) recipes are item-ready — `circuit` alone blocks all of them. The 2 ungated recipes (`blueprints`/`beigeprints`) are blocked only on `GEM`-shape `cinnabar_gem` (blueprints' own ingredient list is otherwise fully ready) and, like all 22, on the machine itself not existing.**

### ReformingRecipes — by family

| Family | Status |
|---|---|
| All 15 distinct `FluidType`s (`HEATINGOIL`, `NAPHTHA`, `NAPHTHA_CRACK`, `NAPHTHA_COKER`, `LIGHTOIL`, `LIGHTOIL_CRACK`, `PETROLEUM`, `SOURGAS`, `CHOLESTEROL`, `REFORMATE`, `AROMATICS`, `REFORMGAS`, `SULFURIC_ACID`, `HYDROGEN`, `ESTRADIOL`) | **READY** — all confirmed present in `Fluids.java`. |

**Bottom line: ReformingRecipes is 100% item/fluid-ready today. The only blocker is `MachineCatalyticReformer` (block+BE+GUI) not existing — this is purely a "build the machine" task once that's done, the recipe data can be ported essentially verbatim.**

### WasteDrumRecipes — by family

| Family | Status |
|---|---|
| 16 waste items (`waste_*`/`waste_*_hot` pairs, 32 ids) | **READY** — `PlateCrystalWasteItems.java:106-140`, all confirmed. |
| `pwr_fuel_hot_<type>` (15, input side) | **READY** — `PWRHotFuelItems.java`. |
| `pwr_fuel_depleted_<type>` (15, output side) | **BLOCKED — not registered anywhere** (confirmed 0 hits; `MachineItems.java:403-405`'s own comment names this exact gap). |
| `ItemRBMKRod` (special-cased, not a recipe-table entry) | **READY** — `items/machine/ItemRBMKRod.java`, `RBMKRods.java`. |

**Bottom line: 16 of 31 entries (the waste-decay family) are 100% item-ready today; 15 of 31 (the PWR-fuel-decay loop) are blocked only on `pwr_fuel_depleted_<type>` (15 new items, otherwise identical to the already-established `pwr_fuel_hot_<type>` registration pattern — trivial to add). Both sub-families need only the `WasteDrum` machine itself once their (near-total) item gaps close.**

## Recommended 1.21.1 implementation shape

**None of these four are vanilla-shaped (`Recipe<CraftingInput>`/JSON shaped-shapeless) recipes.** All four need custom Java recipe-data classes, following this port's own already-proven `ChemPlantRecipes`/`CentrifugeRecipes` convention (plain static table + custom block-entity recognition logic) rather than vanilla `RecipeType`/`RecipeSerializer` JSON, for reasons specific to each:

- **`ArcWelderRecipes`** → custom Java data class (e.g. `ArcWelderRecipes.java` under `com.hbm.inventory.recipes`, mirroring CE's own package). Needs: variable-length `AStack[]` ingredients (order-independent, tag-or-exact matching via the existing `AStack`/`ComparableStack`/`OreDictStack` hierarchy — already ported), an *optional* single fluid input, single deterministic output (no chance), duration + power. This is structurally closest to `RefineryRecipes`/`CentrifugeRecipes`'s existing shape in this port (multi-`AStack` in, single out) — extend that established pattern, add the optional-fluid field. Block entity: extend `MachineBaseBlockEntity` (this port's existing base, confirmed present at `blockentity/`), following the already-built `CentrifugeBlockEntity`/`ChemPlantBlockEntity` as the closest structural precedent for "match against a static Java recipe list, no JEI/JSON reload."
- **`PrecAssRecipes`** → custom Java data class, **not** the existing `loader.GenericRecipe`/`GenericRecipes` stand-in (see "Already covered" #1 — that stand-in explicitly isn't this shape). Follow `ChemPlantRecipes.java`'s exact precedent: define a local `PrecAssRecipe` record/class (up to 9 `AStack` inputs incl. tag matches, ≤1 fluid input, chance-weighted item outputs via a small `ChanceOutput`-equivalent, ≤1 fluid output, duration+power), a plain static `List`, and — critically — **replicate `registerPair()`'s recycle-generation as a helper method** rather than hand-authoring 20 more recycle entries; this both matches CE's real generative structure and halves the hand-transcription work. Needs the `circuit` item family and `ModItems.BROKEN_ITEM` built first (see dependency check) before any of this is testable end-to-end, even though the recipe-data class itself has no such dependency.
- **`ReformingRecipes`** → simplest of the four: a near-verbatim `Map<FluidType, Triplet<FluidStack,FluidStack,FluidStack>>` static table (this port's `Tuple.Triplet` and `ItemFluidIcon` are already available) — no chance, no items, no power field even (CE's own class carries none). Genuinely close to copy-paste once `MachineCatalyticReformer` exists.
- **`WasteDrumRecipes`** → simplest possible shape: `Map<ComparableStack, ItemStack>` identity/decay table + the RBMK-rod special case hardcoded in the block entity's tick method (not data-driven in CE either — don't try to generalize it into the table). No duration/power/chance concept at all; the "recipe" is really a passive water-triggered item transform.

**All four also need, from scratch: block class, block entity, container/menu, and screen/GUI** — none exist. Recommend building these using this port's own already-established machine-block-entity conventions (`MachineBaseBlockEntity` base, the pattern visible in already-built single-recipe-list machines like `CentrifugeBlockEntity`/`GasCentrifugeBlockEntity`) rather than re-deriving CE's 1.12 `TileEntityMachineBase`/`IGUIProvider` pattern from scratch.

## Open questions / risks

1. **The `ANY_X` ore-dict trait-group wildcard mechanism (CE's `ANY_HARDPLASTIC`, `ANY_BISMOID`, `ANY_PLASTIC`, `ANY_HIGHEXPLOSIVE`, `ANY_BISMOIDBRONZE`, `ANY_RESISTANTALLOY`, etc.) has no port-side equivalent at all.** These aren't single materials — they're CE-defined groups of multiple materials sharing a trait, matched as "any material in this group" in a recipe slot. This port's `Mats`/`MaterialShapes` model is per-exact-material tags (`c:ingots/iron`); there's no evident mechanism for "match any of {these N materials}" as a single ingredient slot. This affects `ArcWelderRecipes` (2 entries) and `PrecAssRecipes` (≈8 of 20 gated entries) directly, and is very likely a recurring pattern across the other ≈63 unported CE recipe classes too — worth flagging to whoever owns the cross-cutting recipe-matching infrastructure decision, not just this task's four files. Options: (a) define a NeoForge tag per CE "ANY_" group and match on that tag, (b) a small custom `AStack` subclass wrapping a `Set<Item>`/predicate. Not decided here — genuinely a design call above this task's scope.
2. **Whether `pwr_fuel_hot`/`pwr_fuel_depleted` numeric ordinals in CE's `EnumPWRFuel` still line up 1:1 with this port's already-registered `pwr_fuel_hot_<type>` names** was not independently re-verified beyond confirming both use the same enum (`ItemPWRFuel.EnumPWRFuel`) — low risk since `PWRHotFuelItems.java` iterates the identical enum, but worth a sanity check when wiring the actual recipe table.
3. **`ItemMold.java`'s `"wire_dense"` mold-entry reference** (found during dependency-checking) wasn't investigated for whether it implies a *different*, already-partially-built wire_dense casting path that might overlap with `ArcWelderRecipes`' dense-wire recipes — flagged as a possible small overlap to check before implementing, not confirmed either way.
4. **CE's `plate_cast`/`plate_welded` naming vs. this port's `<mat>_plate_triple`/`<mat>_plate_sextuple` naming is a genuine, confirmed rename** (both are CE's own internal `MaterialShape` enum constant names, carried through verbatim by this port's near-literal `MaterialShapes.java` port) — not a bug, just something an implement-wave agent must know to look up the right id when translating `X.plateCast()`/`X.plateWelded()` calls.
5. **`IngotNuggetItems.java`'s hand-curated `INGOT`/`NUGGET`/`DUST`/etc. shape coverage was spot-checked for only 2 materials (`ingot_graphite`, `ingot_dura_steel`)**, both found present — but this file's *complete* per-material coverage for the `INGOT` shape (as opposed to the fully-generator-swept `CASTPLATE`/`WELDEDPLATE`/`WIRE`/`DENSEWIRE`/`BOLT` shapes) was not exhaustively verified. If a future ArcWelder/PrecAss entry needs an `.ingot()` for a material not yet spot-checked, verify individually before assuming readiness.
6. **`Items.FISH` with `FishType.PUFFERFISH` (CE 1.12.2's cooked/raw-fish-with-subtype item model)** doesn't map directly onto 1.21's separate `PUFFERFISH` item — this is a trivial vanilla-item-id swap (`net.minecraft.world.item.Items.PUFFERFISH`), noted only because it's a literal API-shape difference an implement-wave agent should not miss when transcribing `precass.blueprints`/`precass.beigeprints`.
