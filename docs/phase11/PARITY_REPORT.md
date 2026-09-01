# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors,
`./gradlew build` SUCCESS, jar `build/libs/hbm-0.0.1.jar` **67,078,243 B** (~63.97 MB),
`./gradlew runServer` **Done (5.740s)** on wiped world, **3039 recipes** / 2270 advancements.
No recipe parse errors.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **80.3%** (6240 / 7767) |
| **Unweighted** (mean of category %) | **90.0%** |
| Recipe/loot reachability of port items | **47.1%** (961 / 2042) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **still below ~90%**. Largest remaining hole is still **machine recipes** (67.8%),
then vanilla crafting (64.8%) and blocks (66.4%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2042 | **109.6%** | Phase 10 extract + same machine IDs (casings → Dummyable) |
| Blocks | 1169 | 776 | **66.4%** | same IDs (casings replaced by Dummyable TEs, no new block ids) |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1263 | **64.8%** | CE estimate kept at 1950. +77 leftover JSON this pass |
| Machine recipes | ~2009 | 1362 | **67.8%** | CE denom unchanged. Port: prior 1298 + PyroOven Δ + ArcFurnace 25 + Exposure 4 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **78.5%** / unweighted **89.2%**, machine recipes **1298 / ~2009
(64.6%)**, vanilla **1186 / 1950 (60.8%)**, blocks **776 / 66.4%**.

### Machine recipes: 1298 → 1362 (64.6% → 67.8%)

CE denom stayed **2009**. Port +64. No census-method cheat. Assembler JSON **untouched** (356).

Three Dummyable process families (block + BE + menu + screen + CE recipe table). No stub GUIs.

- **PyroOvenRecipes** ~10 → 45 `RECIPES.add` — CE `PyroOvenRecipes.java:36-124`.
  `machine_pyrooven` Dummyable `{2,0,3,3,2,2}` offset 3 + 5 side extras + roof extra.
  10M HE / 10k base, 2×24k tanks, SPEED/POWER/OVERDRIVE via slot scan. Solid-fuel family
  unblocked (`solid_fuel` / `solid_fuel_bf`). Tar→soot skipped (`powder_ash` unregistered).
  Pollution / audio / particles skipped.
- **ArcFurnaceRecipes** 0 → 25 `.register(new` — CE `ArcFurnaceRecipes.java:41-115`
  (hand-written silica/glass/borax + 12 bedrock sites + material×shape loop).
  `machine_arc_furnace` Dummyable `{4,0,2,2,2,2}` offset 2 + XR `{4,0,3,-2,1,1}` + 6 extras.
  2.5M HE, liquid toggle via `clickMenuButton`, SPEED upgrade, `CrucibleUtil.pourFullStack`.
  `sand_quartz` gated (item unregistered). Vanilla furnace autogen skipped (RecipeManager
  not available at commonSetup). Lid animation / pollution / particles skipped.
- **ExposureChamberRecipes** 0 → 4 `RECIPES.add` — CE `ExposureChamberRecipes.java:54-65`.
  `machine_exposure_chamber` Dummyable `{4,0,2,2,2,2}` offset 2 + XR beam + 5 extras.
  200t / 10k HE, 8 saved particles, SPEED/POWER/OVERDRIVE via slot scan. Cheap path only
  (no expensive-mode DEGENERATE_MATTER). Concrete items, not ore tags (schrabidate INGOT
  autogen missing).

### Vanilla crafting: 1186 → 1263 (60.8% → 64.8%)

+77 leftover shaped/shapeless from CE `CraftingManager` leftovers (`scripts/phase11_wave7_crafts.py`).
Biomass flatten, coils, circuits, particles, pylons/cables, motors, `plate_polymer` flatten,
cloth, cells/canisters, a few tools/weapon parts. Did **not** emit `powder_sawdust` /
`gem_tantalium` / `coil_tungsten` as results. Dropped unregistered ids (radio torch family,
shimmer tools, some blades/rings, …). Assembler skip still **7**. Did not rewrite existing
assembler JSON. runServer recipe count 2962 → **3039** (+77).

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
- PyroOven tar→soot — `powder_ash` unregistered
- PyroOven / arc furnace pollution / audio / particles / lid animation — skipped
- Arc furnace vanilla furnace autogen — RecipeManager not available at commonSetup
- Arc furnace `sand_quartz` → `glass_quartz` — `sand_quartz` unregistered
- Exposure expensive-mode DEGENERATE_MATTER — cheap path only
- Pyro / arc / exposure `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier

## Recipe-graph reachability (cheap)

**47.1%** (961 / 2042). Prior session: 944 / 2042 (46.2%).

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs after pyrooven /
arc furnace / exposure), vanilla crafting 64.8%. Blocks 66.4%. Weighted 80.3% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
