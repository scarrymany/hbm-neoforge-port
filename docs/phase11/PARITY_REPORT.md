# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors (build /
`runServer` pending after this snapshot).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **78.3%** (6081 / 7767) |
| **Unweighted** (mean of category %) | **89.1%** |
| Recipe/loot reachability of port items | **46.2%** (941 / 2039) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (63.9%),
then vanilla crafting (60.8%) and blocks (66.4%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2039 | **109.4%** | Phase 10 extract + coke flatten + catalytic_converter |
| Blocks | 1169 | 776 | **66.4%** | same IDs (casings replaced by Dummyable TEs, no new block ids) |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1186 | **60.8%** | CE estimate kept at 1950. +7 leftover (catalytic_converter + coke 1↔9) |
| Machine recipes | ~2009 | 1283 | **63.9%** | CE denom unchanged. Port: prior 1218 + Compressor 5 + Coker 33 + Cracking 12 + Reforming 9 + Hydrotreating 6 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **77.3%** / unweighted **88.6%**, machine recipes **1218 / ~2009
(60.6%)**, vanilla **1179 / 1950 (60.5%)**, blocks **776 / 66.4%**.

### Machine recipes: 1218 → 1283 (60.6% → 63.9%)

CE denom stayed **2009**. Port +65. No census-method cheat. Assembler JSON **untouched** (356).

Five Dummyable oil/chem families (block + BE + menu + screen + CE recipe table). No stub GUIs.

- **CompressorRecipes** 0 → 5 `recipes.put` — CE `CompressorRecipes.java:24-32`.
  `machine_compressor` Dummyable `{2,0,1,2,1,1}` offset 2 + extras. 2×16k tanks, 100k HE,
  generic +1 PU fallback, PU buttons 0–5 via `clickMenuButton`. Compact stays a casing.
- **CokerRecipes** 0 → 33 `recipes.put` — CE `CokerRecipes.java:30-68` (24×`registerAuto` +
  `registerSFAuto` woodoil + 8 explicit). `machine_coker` Dummyable `{22,0,1,1,1,1}` offset 1.
  Heat pull from `IHeatSource` below, 20k TU/cycle. Coke flatten `coke_petroleum` (not a block).
- **CrackingRecipes** 0 → 12 `recipes.put` — CE `CrackingRecipes.java:41-53` (CE used
  `cracking.put`, census-invisible; port uses `recipes.put`). `machine_catalytic_cracker`
  Dummyable `{0,0,3,3,2,3}` offset 3. Real 5-tank + fluid-id menu (CE was overlay only).
- **ReformingRecipes** 0 → 9 `recipes.put` — CE `ReformingRecipes.java:23-68`.
  `machine_catalytic_reformer` Dummyable `{2,0,1,1,2,2}` offset 1. 20k HE / 100 mB +
  `catalytic_converter`. Canister `loadTank`/`unloadTank` skipped (subsystem not ported).
- **HydrotreatingRecipes** 0 → 6 `recipes.put` — CE `HydrotreatingRecipes.java:23-59`.
  `machine_hydrotreater` Dummyable `{6,0,1,1,1,1}` offset 1. H₂@P1 + 20k HE + catalyst.
  Same canister skip as reformer.

### Vanilla crafting: 1179 → 1186 (60.5% → 60.8%)

+1 `catalytic_converter` (CE `CraftingManager.java:775`, polymer/cobalt/bismuth stand-in for
`ANY_HARDPLASTIC`/`CO.dust()`/`ANY_BISMOID`). +6 coke 1↔9 (CE `MineralRecipes.java:57-58`).
Did **not** emit `powder_sawdust` / `gem_tantalium` / `coil_tungsten` as results.

Assembler skip still **7**. Did not rewrite existing assembler JSON.

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
- Assembler leftover skip **7**: `Fluids.X.getDict` ore stacks (hpcondenser / himars TB ×2 / mpw10taint),
  `ass.nitra` ChanceOutput, `ass.digimemer` (commented Mekanism in CE), `ass.50bmgbypass`
  (`black_diamond` is `ItemModHealth`, not a dummy)
- Combination coke/briquette/powder_ash item flatten — put sites present, AIR filtered
- Blast `meteorite_sword_*` — items missing, row dropped after register
- RockMill AE2 module / blueprint cycling — auto-detect instead
- Annihilator 528 gate — table registered unconditionally (same CE lines)
- SILEX depleted waste / RBMK pellet loop — items missing
- Press meteorite sword / briquette / page_of — AIR filtered
- Rotary `ModuleBurnTime` heat mods — vanilla burn/2
- WasteDrum RBMK rod heat path — `ItemRBMKRod.updateHeat` not in this port
- Fraction CE chat inspect — replaced with a real tank menu (not a stub)
- Compressor upgrades (`UpgradeManagerNT`) — slots exist, levels unused (manager not ported)
- Compressor compact — still a cube casing (assembler output only)
- Cracker CE overlay — replaced with a real tank menu (not a stub)
- Reformer / hydrotreater canister `loadTank`/`unloadTank` — FluidTankNTM item-canister subsystem
  not ported (same skip as diesel / crystallizer / PWR)
- Coker pollution soot increment — PollutionHandler tick not wired on this TE

## Recipe-graph reachability (cheap)

**46.2%** (941 / 2039). Prior session: 936 / 2034 (46.0%). +4 leftover items + leftover craft outputs.

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs: vacuum distill /
radiolysis / flare / epress / …), vanilla crafting 60.8%. Blocks 66.4%. Weighted 78.3% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
