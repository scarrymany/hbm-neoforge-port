# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,541,112** B (~63.46 MB), `./gradlew runServer` **Done (5.620s)** on a
wiped world (2446 recipes, was 2417). Spawn 2% → 51% → Done. No duplicate ids, no leftover `tag:` JSON
parse errors. `hbm:oil_bubble` still logs `setBlock in a far chunk` (no deadlock).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **65.8%** (5108 / 7767) |
| **Unweighted** (mean of category %) | **82.7%** |
| Recipe/loot reachability of port items | **41.8%** (821 / 1963) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (39.1%, was 38.0%),
then vanilla crafting (43.0%) and blocks (61.9%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1963 | **105.4%** | Phase 10 extract + `parts`/`parts1` + flatten extras |
| Blocks | 1169 | 724 | **61.9%** | extract + helpers + fusion/precass/transformer casings |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 838 | **43.0%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 786 | **39.1%** | CE denom unchanged. Port: 196 assembler + 92 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (sats + PlasmaForge leftovers + leftover assembler/vanilla)

Previous published snapshot: weighted **65.0%** / unweighted **82.3%**, machine recipes **763 / ~2009
(38.0%)**, blocks **714 / 1169 (61.1%)**, items **1949 / 1863 (104.6%)**, vanilla **823 / 1950 (42.2%)**,
reachability **806 / 1949 (41.4%)**.

### Machine recipes: 763 → 786 (38.0% → 39.1%)

CE denom stayed **2009**. Port +23 (14 assembler JSON + 9 Java). No census-method cheat.

JSON: assembler **182 → 196**. New leftovers (`AssemblyMachineRecipes.java:245` `ass.precass`;
`:569-578` fusion component flatten 0/2/3; `:683` `ass.exobomb` → existing `therm_exo`;
`:793-798` warheadinc; `:969-1009` sat_base + 5 sat heads). Slugs lowercase. Existing assembler
JSON not overwritten. Circuit-count flatten fixed (`circuit, N, EnumCircuitType`) for new files.
`fluid_barrel_full` + `Fluids.X.getID()` no longer early-returns as fluid. Tags still
`{"item":{"tag":"hbm:…"}}`. Shredder 92, breeder 30 unchanged. Machine JSON total 318.

Java:

| Class | Now | Notes |
|---|---:|---|
| ArcWelderRecipes | 47 | CE complete. `:366-400` EnumSatType flatten → existing `sat_mapper`/`scanner`/`radar`/`laser`/`resonator` |
| SolderingRecipes | 26 | CE complete (no 528/LBSM). Unchanged |
| PlasmaForgeRecipes | 35 | CE complete. `:98` fusionvessel → `fusion_torus` + `fusion_component_{0,2,3}`; `:161` schrabhammer → `schrabidium_hammer` + `schrabidium_block` (suffix-first, not `block_schrabidium`); `:176` fensusan → `machine_battery_redd`; `:202` gerald → `sat_gerald`. Multi-input (11–12 stacks) counted; TE still 6 slots |
| AmmoPressRecipes | 60 | Unchanged. Did not re-add a second `coil_tungsten` |
| others | ~304 | chem/solidifier/PA/PUREX/liquefaction/centrifuge/… unchanged |

### Vanilla crafting: 823 → 838 (42.2% → 43.0%)

Table-driven leftover CE crafts in `data/hbm/recipe/ce_craft/` (not ModRecipeProvider Java).
`scripts/phase11_leftover_craft.py`.

- PowderRecipes.java:25 / :29-30 / :40-41 / :72 — `ballistite`, `powder_semtex_mix` (both legs),
  gunpowder from niter+sulfur, `powder_fertilizer` (niter/sulfur now exist)
- ConsumableRecipes.java:73 / :77 / :130 — `can_smart`, `can_overcharge`, `xanax` (non-LBSM)
- CraftingManager.java:646 / :650 / :660-661 / :691-692 — `photo_panel`, `sat_chip`,
  `machine_transformer`, `machine_transformer_dnt`, sliding-blast-door legacy convert

Skipped fluids / chem-set / ItemScraps / LBSM. Did not overwrite generated `powder/` files.

### Blocks: 714 → 724 (61.1% → 61.9%)

Casings only (no new TE). Cubes reuse `block_steel` except transformers (existing cube_bottom_top).

- Fusion — CE `ModBlocks.java:1318-1319` / `PlasmaForgeRecipes.java:98`: `fusion_torus`,
  `fusion_component_0/2/3` (BlockFusionComponent metas 0/2/3)
- Precass — CE `:1057` / `AssemblyMachineRecipes.java:245`: `machine_precass`
- FEnSU — CE `:970` / PlasmaForge `:176`: `machine_battery_redd`
- Transformers — CE `:979-982` BlockBase: `machine_transformer`, `_20`, `_dnt`, `_dnt_20`

### Items that unblocked recipes

CE `ModItems.java:1302` / `:2525-2529` — `sat_base`, `sat_head_{mapper,scanner,radar,laser,resonator}`.
`:1301` `photo_panel`. PowderRecipes.java:25 `ballistite`. `any_smokeless` tag now includes ballistite.

Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second `coil_tungsten`.

Reachability **806/1949 → 821/1963 (41.8%)**.

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
- Assembler `satelliterelay` — `thruster_nuclear` missing
- ~169 leftover assembler still skipped (missing blocks / ore-or-fluid / unresolved)
- Older leftover assembler rows may still have circuit flatten ×1 (new files this pass are correct)

## Recipe-graph reachability (cheap)

821 / 1963 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 41.8% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: leftover assembler (~169 skipped — `machine_arc_furnace` / `machine_supercomputer`
/ compressors / satlink / teleporter / …), vanilla crafting 43.0%. Blocks 61.9%. Weighted 65.8% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
