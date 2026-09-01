# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors
(`build` / `runServer` pending after this snapshot).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **78.5%** (6099 / 7767) |
| **Unweighted** (mean of category %) | **89.2%** |
| Recipe/loot reachability of port items | **46.2%** (944 / 2042) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (64.6%),
then vanilla crafting (60.8%) and blocks (66.4%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2042 | **109.6%** | Phase 10 extract + same machine IDs (casings → Dummyable) |
| Blocks | 1169 | 776 | **66.4%** | same IDs (casings replaced by Dummyable TEs, no new block ids) |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1186 | **60.8%** | CE estimate kept at 1950. No new leftover JSON this pass |
| Machine recipes | ~2009 | 1298 | **64.6%** | CE denom unchanged. Port: prior 1283 + VacuumDistill 2 + Radiolysis 13 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **78.3%** / unweighted **89.1%**, machine recipes **1283 / ~2009
(63.9%)**, vanilla **1186 / 1950 (60.8%)**, blocks **776 / 66.4%**.

### Machine recipes: 1283 → 1298 (63.9% → 64.6%)

CE denom stayed **2009**. Port +15. No census-method cheat. Assembler JSON **untouched** (356).

Four Dummyable oil/chem families (block + BE + menu + screen + CE recipe table). No stub GUIs.

- **VacuumDistillRecipes** 0 → 2 `recipes.put` — CE `RefineryRecipes.java:116-127`
  (`vacuum.put`, census-invisible). `machine_vacuum_distill` Dummyable `{8,0,1,1,1,1}` offset 1
  + 4 back-corner extras. 10k HE / 100 mB, input @ PU2, 5 tanks. Canister unload skipped.
- **RadiolysisRecipes** 0 → 13 `recipes.put` — CE `RadiolysisRecipes.java:50,60` (WATER +
  `putAll(CrackingRecipes)`). `machine_radiolysis` Dummyable `{2,0,1,1,1,1}` offset 1 + 4
  core-side extras. RTG heat ×10 HE, crack 100 mB when heat&gt;100. Sterilize/`ntmContagion` skipped.
- **FlareRecipes** — CE has **no recipe map** (`TileEntityMachineGasFlare.java:150-201` is
  trait-driven). `machine_flare` Dummyable `{11,0,1,1,1,1}` offset 1 + 4 core-side extras.
  Vent 50 mB/t / burn 10 mB/t, valve+ignition via `clickMenuButton`. Pollution/particles/tilt skipped.
- **EPress** — shared `PressRecipes` (already counted). `machine_epress` Dummyable `{2,0,0,0,0,0}`
  offset 0. 100 HE/t, 200 progress, SPEED upgrade via slot scan (`UpgradeManagerNT` not ported).

### Vanilla crafting: unchanged 1186 / 60.8%

No leftover JSON this pass. Did **not** emit `powder_sawdust` / `gem_tantalium` / `coil_tungsten`.
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
- Vacuum distill canister unload — FluidTankNTM item-canister subsystem not ported
- Radiolysis sterilize (`ntmContagion` NBT) — not ported; slots exist
- Flare pollution / particles / entity fire / tilt — skipped
- Flare / EPress `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier instead

## Recipe-graph reachability (cheap)

**46.2%** (944 / 2042). Prior session: 941 / 2039 (46.2%).

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs: pyrooven /
arc furnace / exposure / …), vanilla crafting 60.8%. Blocks 66.4%. Weighted 78.5% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
