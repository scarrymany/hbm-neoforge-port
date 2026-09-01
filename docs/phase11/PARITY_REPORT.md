# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,829,607** B (~63.73 MB), `./gradlew runServer` **Done (5.769s)** on a
wiped world (2905 recipes). Spawn 2% → 51% → Done. No recipe parse errors (only expected first-boot
`server.properties` missing).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **75.4%** (5860 / 7767) |
| **Unweighted** (mean of category %) | **87.7%** |
| Recipe/loot reachability of port items | **45.5%** (923 / 2030) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (56.3%),
then vanilla crafting (57.9%) and blocks (66.0%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2030 | **109.0%** | Phase 10 extract + `parts`/`parts1` + flatten extras + 4 new machine BlockItems |
| Blocks | 1169 | 772 | **66.0%** | extract + leftover assembler casings + 4 Dummyable machines |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1129 | **57.9%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 1132 | **56.3%** | CE denom unchanged. Port: prior 1061 + Combination 28 + Blast NT 15 register + RockMill 9 register + Annihilator 15 put |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **74.4%** / unweighted **87.2%**, machine recipes **1061 / ~2009
(52.8%)**, vanilla **1129 / 1950 (57.9%)**, blocks **768 / 65.7%**.

### Machine recipes: 1061 → 1132 (52.8% → 56.3%)

CE denom stayed **2009**. Port +71. No census-method cheat. Assembler JSON **untouched** (356).

Four new Dummyable families (block + BE + menu + screen + CE recipe table). No stub GUIs.

- **CombinationRecipes** 0 → 28 — CE `CombinationRecipes.java:86-134`. `furnace_combination` Dummyable
  `{1,0,1,1,1,1}` offset 1. Heat pull from `IHeatSource` below (CE `TileEntityFurnaceCombination`).
  AIR coke/ash/briquette rows dropped after register; put sites remain.
- **BlastFurnaceRecipesNT** 0 → 15 `register(new` — CE `BlastFurnaceRecipesNT.java:33-82` (NT machine,
  not deprecated DiFurnace). `machine_blast_furnace` Dummyable `{6,0,1,1,1,1}` offset 1 + extras.
  Fuel = vanilla burn time. Airblast/flue tanks. Slag AIR outputs stripped, steel rows kept.
- **RockMillRecipes** 0 → 9 `register(new` — CE `RockMillRecipes.java:39-121`. Weighted
  `ChanceOutput` pool (weights sum 100). `machine_rock_mill` Dummyable `{2,0,2,2,2,2}` offset 2.
  Auto-detect first match (no AE2 / blueprint module).
- **AnnihilatorRecipes** 0 → 15 `recipes.put` — CE `AnnihilatorRecipes.java:61-76`. CE gates on
  `enable528`; port registers the same put sites unconditionally so the machine pays out.
  `machine_annihilator` Dummyable + extra tower + `AnnihilatorSavedData`.

Assembler skip still **7**. Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second
`coil_tungsten`. Did not rewrite existing assembler JSON.

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

## Recipe-graph reachability (cheap)

**45.5%** (923 / 2030). Prior session: 919 / 2026 (45.4%). +4 = the new Dummyable BlockItems
via `dropSelf` loot (combination / blast / rock mill / annihilator).

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs), vanilla crafting
57.9%. Blocks 66.0%. Weighted 75.4% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
