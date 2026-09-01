# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,611,238** B (~63.53 MB), `./gradlew runServer` **Done (5.093s)** on a
wiped world (2584 recipes, was 2446). Spawn 2% → 51% → Done. No duplicate ids, no leftover `tag:` JSON
parse errors. `hbm:oil_bubble` still expected to log `setBlock in a far chunk` (no deadlock) on longer
worlds; this wipe finished before it showed up.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **68.6%** (5327 / 7767) |
| **Unweighted** (mean of category %) | **84.3%** |
| Recipe/loot reachability of port items | **43.5%** (873 / 2009) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (44.8%, was 39.1%),
then vanilla crafting (44.2%) and blocks (64.9%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2009 | **107.8%** | Phase 10 extract + `parts`/`parts1` + flatten extras |
| Blocks | 1169 | 759 | **64.9%** | extract + helpers + leftover assembler casings |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 861 | **44.2%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 901 | **44.8%** | CE denom unchanged. Port: 305 assembler + 98 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (leftover assembler casings + crafts)

Previous published snapshot: weighted **65.8%** / unweighted **82.7%**, machine recipes **786 / ~2009
(39.1%)**, blocks **724 / 1169 (61.9%)**, items **1963 / 1863 (105.4%)**, vanilla **838 / 1950 (43.0%)**,
reachability **821 / 1963 (41.8%)**.

### Machine recipes: 786 → 901 (39.1% → 44.8%)

CE denom stayed **2009**. Port +115 (109 assembler JSON + 6 shredder JSON). No census-method cheat.

JSON: assembler **196 → 305**. Leftover skip dump **169 → 58** (`scripts/phase11_machine_parts.py`).
Gating casings/items registered first, then JSON. Existing assembler JSON **not overwritten**
(circuit-count flatten is lossy). Slugs lowercase. Tags still `{"item":{"tag":"hbm:…"}}`.
`circuit, N` counts kept. `fluid_barrel_full` + `Fluids.X.getID()` stay items (satelliterelay
`circuit_basic` **24**). Shredder **92 → 98** (`crystal_fluorite`→fluorite, dirt/sand/dust,
crystal_niter/sulfur). Breeder 30 unchanged. Machine JSON total 433.

Java (unchanged this pass):

| Class | Now | Notes |
|---|---:|---|
| ArcWelderRecipes | 47 | CE complete |
| SolderingRecipes | 26 | CE complete (no 528/LBSM) |
| PlasmaForgeRecipes | 35 | CE complete |
| AmmoPressRecipes | 60 | Did not re-add a second `coil_tungsten` |
| others | ~304 | chem/solidifier/PA/PUREX/liquefaction/centrifuge/… unchanged |

### Vanilla crafting: 838 → 861 (43.0% → 44.2%)

Table-driven leftover CE crafts in `data/hbm/recipe/ce_craft/` (not ModRecipeProvider Java).

- PowderRecipes.java:66 — `powder_flux` from `fluorite` (`F.dust()`)
- ConsumableRecipes.java:76 / :96-119 / :134 / :151-157 — `can_mrsugar`, `can_redbomb`,
  syringe family, `med_bag`, `radx`, `pill_iodine`, `plan_c`, cladding family
- Prior wave leftovers (ballistite / semtex / fertilizer / cans / xanax / transformers / sat_chip)
  unchanged

Skipped fluids / chem-set / ItemScraps / LBSM. Did not overwrite generated `powder/` files.

### Blocks: 724 → 759 (61.9% → 64.9%)

Casings only (no new TE) in `Phase11CasingBlocks`. Cubes reuse `block_steel` when CE models
are missing. Existing CE blockstates (arc furnace / supercomputer / satlink) left alone.

- CE `ModBlocks.java:1232` / `AssemblyMachineRecipes.java:248` — `machine_supercomputer`
- `:1219` / `:258` — `machine_arc_furnace`
- `:1086-1087` / `:320-323` — `machine_compressor`, `machine_compressor_compact`
- `:1186` / `:362` — `machine_teleporter` (+ `entanglement_kit`)
- `:1238` / `:367` — `machine_satlink`
- Plus leftover assembler outputs: epress / ore_slopper / mining_laser / forcefield /
  strand_caster / assembly+chemical factory / turbofan / hephaestus / chungus / radgen /
  pyrooven / fluidtank / bigasstank / exposure_chamber / reactor_research / reactor_zirnox /
  seal_frame+controller / vitrified_barrel / struct_torus_core / fusion klystron/collector/
  breeder/boiler/mhdt/coupler / watz_element / watz_cooler

`machine_fluidtank` ≠ existing `machine_fluidtank_basic`.

### Items that unblocked recipes

- CE `ModItems.java:1303` / `AssemblyMachineRecipes.java:820` + `:1015` — `thruster_nuclear`
  (unblocks `satelliterelay`)
- `:2727` / `:362` — `entanglement_kit`
- `:2536` — `tank_steel`
- `:1281` / `:1289` — `pellet_buckshot`, `pellet_cluster`
- `:2530-2532` — `seg_10` / `seg_15` / `seg_20`
- `:1134` — `fluorite` (bare CE item; **not** a second `powder_fluorite` / `crystal_fluorite`)
- `:1296` — `ducttape`
- `:1173` — `rod_empty`
- `:117-126` — syringe family + `med_bag`
- `:191-197` — cladding via `ItemModCladding`
- `dysfunctional_reactor` (protoreactor still skipped — needs `rod_quad_empty`)

Resolver: MAT `TA`/`LI`/`NP237`/`SA327`, `ANY_TAR`→`tag:hbm:any_tar`, mechanism shape,
PA-coil flatten, KEY dyes / planks / pane, SAT_TYPE extras. `known_ids()` now sees
`fuel|consumeFx|consume|cladding|casing|part|standard`.

Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second `coil_tungsten`.

Reachability **821/1963 → 873/2009 (43.5%)**.

No invented art.

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure, not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself**
- Projectile tails = fallback renderer
- Assembler expensive-mode `inputItemsEx` legs — dropped (same as prior assembler ports)
- PA recipe `#10` SBD.ingot() — no schrabidate INGOT autogen
- Texture misses with no CE file — documented, no invented art
- ElectrolyserMetal → foundry. Not stubbed.
- PUREX chance-output / ICF / vitrification / naquadria — missing I/O
- Full Albion beam physics — detector runs the recipe table locally
- AmmoPress fluid-slot recipes (FLAME_*) — stored, not consumed (no tank on the press)
- PlasmaForge late-game recipes with 11–12 item stacks — counted, TE still 6 slots
- Assembler leftover skip **58** (`rod_quad_empty`, `block_cap` flatten, `machine_condenser_powered` /
  `machine_orbus`, pile_rod enum / `pile_brick`, `ANY_BISMOIDBRONZE`, nuke fins, `nuke_solinium` /
  `nuke_fstbmb`, `LI.ingot()` no INGOT autogen, HIMARS/ammo flatten, `missile_shuttle`/`soyuz`,
  `fluid_pack_empty`, chance-output `ass.nitra`, `ass.digimemer`)
- Older leftover assembler rows may still have circuit flatten ×1 (new files this pass are correct)

## Recipe-graph reachability (cheap)

873 / 2009 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 43.5% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: leftover assembler (~58 skipped — protoreactor / block_cap / hpcondenser /
orbus / pile rods / nuke fins / …), vanilla crafting 44.2%. Blocks 64.9%. Weighted 68.6% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
