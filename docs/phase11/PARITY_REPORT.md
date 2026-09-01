# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors,
jar `build/libs/hbm-0.0.1.jar` **67,390,382 B** (~64.27 MB) tagged `beta-82.1`,
`./gradlew runServer` **Done (6.716s)** on wiped world, **3464 recipes** / 2270 advancements.
No recipe parse errors. `runClient` passed `RegisterMenuScreensEvent` (reached `gui.png-atlas`).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **86.8%** (6741 / 7767) |
| **Unweighted** (mean of category %) | **93.3%** |
| Recipe/loot reachability of port items | **49.1%** (1010 / 2056) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **still below ~90%** (need 6990, short **249**). Largest remaining hole is
**vanilla crafting** (72.6%), then blocks (67.6%). Assembler named leftover is closed
(skip 7 + pack/unpack).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2056 | **110.4%** | Phase 10 extract + Dummyable BlockItems |
| Blocks | 1169 | 790 | **67.6%** | +7 Dummyable/1×1 (`heater_electric` / `heater_heatex` / Stirling ×3 / `machine_storage_drum` / `machine_autosaw`; SuperComputer was a casing) |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1416 | **72.6%** | CE estimate kept at 1950. +47 leftover JSON this pass |
| Machine recipes | ~2009 | 1682 | **83.7%** | CE denom unchanged. Port: prior 1409 + 272 pack/unpack +1 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **82.5%** / unweighted **91.2%**, machine recipes **1409 / ~2009
(70.1%)**, vanilla **1369 / 1950 (70.2%)**, blocks **783 / 67.0%**.

### Client crash: `RegisterMenuScreensEvent` NPE

`CrucibleMenus.MACHINE_CRUCIBLE` (`CrucibleMenus.java:20`) is assigned only in
`CrucibleMenus.registerAll()` (`:26-28`), which is only called from
`CrucibleBlocks.registerAll()` (`CrucibleBlocks.java:46`). `ModBlocks.register()` never called
`CrucibleBlocks.registerAll()`, so the field stayed null. Dedicated server never hits
`RegisterMenuScreensEvent`; XMCL client NPE'd on `MACHINE_CRUCIBLE.get()`.

Fix: `ModBlocks.java:89` now calls `CrucibleBlocks.registerAll()`. New
`SafeMenuScreens.bind` skips a null holder instead of taking down the whole client. Every
`RegisterMenuScreensEvent` handler uses it. Grep `event.register(.*\.get()` in `*Registry.java`
is empty.

### Machine recipes: 1409 → 1682 (70.1% → 83.7%)

CE denom stayed **2009**. Port +273. No census-method cheat. Assembler JSON **356 → 628**.

CE `AssemblyMachineRecipes.java:1088-1097` pack/unpack loop (`Fluids.getInNiceOrder()`, skip
`NONE` + `hasNoContainer`/`NOCON`): +272 JSON `package_<fluid>` / `unpackage_<fluid>`
(`scripts/phase11_wave10_assembler.py`). Named leftover is exactly skip **7**.

### Dummyables (live GUI+BE, not stubs)

CE dims from `ModBlocks.java` ~1245–1257:

- **heater_electric** — `{0,0,1,2,1,1}` offset 2. HE in, heat out.
- **heater_heatex** — `{0,0,1,1,1,1}` offset 1 + 4 extras. `FT_Coolable` HEATEXCHANGER.
- **Stirling ×3** — `{1,0,1,1,1,1}` offset 1 + 4 extras. Shared BE.
- **StorageDrum** — 1×1, 24 hexagonal slots. Waste recipes empty (outputs unregistered).
- **SuperComputer** — Dummyable `{5,0,3,3,3,3}` offset 8 (was a casing). Drive recipes not invented.
- **Autosaw** — 1×1. `WOODOIL` tank, `BlockTags.LOGS` harvest.

### Vanilla crafting: 1369 → 1416 (70.2% → 72.6%)

+47 leftover from CE `CraftingManager.java:724-1090` (`scripts/phase11_wave11_crafts.py`).
Barrels / upgrades / RBMK columns / deco_rbmk / storage_drum / firebrick / Mats BOLT ×4.
Did **not** emit banned results. Dropped unregistered (`powder_fire` / `key` / `gear_large` /
`sawblade` / `mold_base`). Did not rewrite assembler JSON. Did not invent anvil→table.
runServer recipe count 3145 → **3464** (+272 pack +47 vanilla).

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
- StorageDrum waste table empty (depleted-waste outputs unregistered)
- SuperComputer drive/`EnumDriveType` recipes not invented
- Autosaw entity shred skipped
- `catalytic_converter` `ANY_BISMOID.ingot()` → `ingot_bismuth` (no `any_bismoid` tag)
- wave11 drops: `powder_fire` / `key` / `gear_large` / `sawblade` / `mold_base` unregistered
- fluid-NBT / `OreDictionary.WILDCARD` / LBSM-gated / commented CE crafts skipped

## Recipe-graph reachability (cheap)

**49.1%** (1010 / 2056). Prior published: 987 / 2049 (48.2%).

## Next single gap (not 90%)

Vanilla leftover after `:1090` + unregistered-result crafts. Blocks 67.6%. Remaining machine
hole ~327 (other Java tables, not assembler named rows). Weighted **86.8% ≠ 90%**.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
