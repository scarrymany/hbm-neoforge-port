# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,896,525** B (~63.80 MB), `./gradlew runServer` **Done (5.216s)** on a
wiped world (2955 recipes, was 2905). Spawn 2% → 51% → Done. No recipe parse errors.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **77.3%** (6004 / 7767) |
| **Unweighted** (mean of category %) | **88.6%** |
| Recipe/loot reachability of port items | **46.0%** (936 / 2034) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (60.6%),
then vanilla crafting (60.5%) and blocks (66.4%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2034 | **109.2%** | Phase 10 extract + 4 new machine BlockItems |
| Blocks | 1169 | 776 | **66.4%** | extract + 4 new machines (press / rotary / fraction / waste drum) |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1179 | **60.5%** | CE estimate kept at 1950. +50 leftover CraftingManager / powder / smelting JSON |
| Machine recipes | ~2009 | 1218 | **60.6%** | CE denom unchanged. Port: prior 1132 + Press 38 + Rotary 12 + Fraction 19 + WasteDrum 17 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **75.4%** / unweighted **87.7%**, machine recipes **1132 / ~2009
(56.3%)**, vanilla **1129 / 1950 (57.9%)**, blocks **772 / 66.0%**.

### Machine recipes: 1132 → 1218 (56.3% → 60.6%)

CE denom stayed **2009**. Port +86. No census-method cheat. Assembler JSON **untouched** (356).

Four families (block + BE + menu + screen + CE recipe table). No stub GUIs.

- **PressRecipes** 0 → 38 — CE `PressRecipes.java:56-105`. `machine_press` Dummyable `{2,0,0,0,0,0}`
  offset 0. Stamp + fuel + input. AIR meteorite/briquette/page_of rows dropped after register; put
  sites remain. Wire autogen one put per WIRE-shaped mat.
- **RotaryFurnaceRecipes** 0 → 12 `RECIPES.add` — CE `RotaryFurnaceRecipes.java:30-47`.
  `machine_rotary_furnace` Dummyable `{4,0,1,1,2,2}` offset 1 + extras. Steam + process tanks,
  crucible pour via `CrucibleUtil.pourFullStack`.
- **WasteDrumRecipes** 0 → 17 `recipes.put` — CE `WasteDrumRecipes.java:32-50` (16 waste + PWR loop
  site). `machine_waste_drum` single-block, water-adjacency cooling. Not Dummyable (CE isn't).
- **FractionRecipes** 0 → 19 `recipes.put` — CE `FractionRecipes.java:23-42` (CE used
  `fractions.put`, census-invisible; port uses `recipes.put`). `machine_fraction_tower` Dummyable
  `{2,0,1,1,1,1}` offset 1. Real tank + fluid-id menu (CE was chat inspect).

### Vanilla crafting: 1129 → 1179 (57.9% → 60.5%)

+50 leftover `ce_craft/` JSON from `CraftingManager` / `PowderRecipes` / `SmeltingRecipes`
(`scripts/phase11_wave6_crafts.py`). 11 JSON dropped after first boot (unregistered ids:
gneiss/lightstone/rebar/blade/coil_advanced/geiger/desh_mix). Did **not** emit
`powder_sawdust` / `gem_tantalium` / `coil_tungsten` as results.

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

## Recipe-graph reachability (cheap)

**46.0%** (936 / 2034). Prior session: 923 / 2030 (45.5%). +4 BlockItems + leftover craft outputs.

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs: compressor / coker /
cracker / reformer / hydrotreater), vanilla crafting 60.5%. Blocks 66.4%. Weighted 77.3% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
