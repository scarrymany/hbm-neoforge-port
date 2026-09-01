# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors,
`./gradlew build` SUCCESS, jar `build/libs/hbm-0.0.1.jar` **67,229,301 B** (~64.11 MB),
`./gradlew runServer` **Done (5.204s)** on wiped world, **3145 recipes** / 2270 advancements.
No recipe parse errors.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **82.5%** (6407 / 7767) |
| **Unweighted** (mean of category %) | **91.2%** |
| Recipe/loot reachability of port items | **48.2%** (987 / 2049) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **still below ~90%**. Largest remaining hole is still **machine recipes** (70.1%),
then vanilla crafting (70.2%) and blocks (67.0%). Assembler JSON hole is the 90% blocker.

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2049 | **110.0%** | Phase 10 extract + 6 new BlockItems |
| Blocks | 1169 | 783 | **67.0%** | +6 Dummyable (`furnace_iron` / `furnace_steel` / `heater_firebox` / `heater_oven` / `heater_oilburner` / `machine_sawmill`) |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1369 | **70.2%** | CE estimate kept at 1950. +35 leftover JSON this pass |
| Machine recipes | ~2009 | 1409 | **70.1%** | CE denom unchanged. Port: prior 1405 + sawmill 4 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **81.8%** / unweighted **90.9%**, machine recipes **1405 / ~2009
(69.9%)**, vanilla **1334 / 1950 (68.4%)**, blocks **777 / 66.5%**.

### Machine recipes: 1405 → 1409 (69.9% → 70.1%)

CE denom stayed **2009**. Port +4. No census-method cheat. Assembler JSON **untouched** (356).

Six Dummyable process families (block + BE + menu + screen). Only sawmill adds a counted
`RECIPES.add` table. No stub GUIs.

- **Iron furnace** — CE `FurnaceIron` `ModBlocks.java:1251`, `TileEntityFurnaceIron.java:61-116`.
  Dummyable `{1,0,1,0,1,0}` offset 0. Vanilla `RecipeType.SMELTING`, baseTime 160, SPEED upgrade
  slot scan. Pollution / particles skipped.
- **Steel furnace** — CE `FurnaceSteel` `:1252`, `TileEntityFurnaceSteel.java:59-111`.
  Dummyable `{1,0,1,1,1,1}` offset 1. 3-lane heat smelter, processTime 40_000, maxHeat 100_000,
  diffusion 0.05 from `IHeatSource` below. Ore bonus / pollution / particles skipped.
- **Firebox** — CE `HeaterFirebox` `:1245`, `TileEntityFireboxBase.java:50-113`.
  Dummyable `{0,0,1,1,1,1}` offset 1. 2 fuel slots, baseHeat 100, maxHeat 100_000. Ashpit /
  pollution / door anim skipped. `ModuleBurnTime` fuel-class mods → vanilla burn time.
- **Oven** — CE `HeaterOven` `:1246`, `TileEntityHeaterOven.java:26-75`. Same dims. Firebox with
  baseHeat 500, timeMult 0.125, maxHeat 500_000, plus 50% pull from `IHeatSource` below.
- **Oilburner** — CE `HeaterOilburner` `:1247`, `TileEntityHeaterOilburner.java:59-103`.
  Dummyable `{1,0,1,1,1,1}` offset 1 + 5 extras. `FT_Flammable` tank, setting mB/t, maxHeat 100_000.
  Canister `loadTank` / pollution skipped.
- **Sawmill** — CE `MachineSawmill` `:1257`, `TileEntitySawmill.java:279-337` / `getRecipes()`
  `:327-337`. Dummyable `{1,0,1,1,1,1}` offset 1 + 4 extras. **4 `RECIPES.add`**
  (logs/planks/stickWood/saplings). CE overlay-only → port adds a live 3-slot menu (not a stub).
  Blade / entity shred / overspeed skipped (`sawblade` unregistered → blade assumed).
  `powder_sawdust` is a machine byproduct (registered); ban is vanilla-craft *results* only.

Skipped this wave (still Dummyable, not landed): `heater_electric` / `heater_heatex` /
Stirling ×3 / StorageDrum (unregistered depleted waste) / SuperComputer XR / Autosaw.
Heaters / steel furnace / sawmill CE crafts are **anvil**, not vanilla (only `furnace_iron`
`CraftingManager.java:343` is table-craft).

### Vanilla crafting: 1334 → 1369 (68.4% → 70.2%)

+35 leftover shaped/shapeless from CE `CraftingManager.java:343` + `:526-728` (reg2)
(`scripts/phase11_wave9_crafts.py`). `furnace_iron` (dropped last wave as unregistered),
rails, book_guide, powders, dets/charges/emp, gun kits, doors, wood barrier, records,
fluid ducts/valves, segs, geiger, containment box, casing bags. Did **not** emit
`powder_sawdust` / `gem_tantalium` / `coil_tungsten` as results. Dropped unregistered ids
(sat_dock / flame_* / solid_fuel_presto_* / hev_battery / igniter / key / padlock / tanks /
sat_chip / …). Assembler skip still **7**. Did not rewrite existing assembler JSON.
runServer recipe count 3110 → **3145** (+35).

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
- Slopper animation / entity shred — skipped
- Turbofan pollution / particles — skipped
- Radgen depleted/tiny leftovers — items unregistered
- Hephaestus `volcanic_lava_block` — unregistered; CE overlay-only (port has a real menu)
- Wood burner `powder_ash` — unregistered; `loadTank` canister path not ported
- Wood burner / slopper / turbofan `UpgradeManagerNT` — slot scan by type/tier instead
- Iron / steel furnace pollution + particles; steel ore bonus
- Firebox `ModuleBurnTime` fuel-class mods + ashpit + door anim
- Oilburner `loadTank` / pollution
- Sawmill blade / entity shred / overspeed (`sawblade` unregistered → blade assumed)
- Heaters / steel furnace / sawmill CE crafts are **anvil**, not vanilla
- `heater_electric` / `heater_heatex` / Stirling ×3 / StorageDrum / SuperComputer / Autosaw — not this wave

## Recipe-graph reachability (cheap)

**48.2%** (987 / 2049). Prior session: 972 / 2043 (47.6%).

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs after this heat/furnace
wave), vanilla crafting 70.2%. Blocks 67.0%. Weighted **82.5% ≠ 90%**.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
