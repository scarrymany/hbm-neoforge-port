# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors,
`./gradlew build` SUCCESS, jar `build/libs/hbm-0.0.1.jar` **67,160,858 B** (~64.05 MB),
`./gradlew runServer` **Done (8.015s)** on wiped world, **3110 recipes** / 2270 advancements.
No recipe parse errors.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **81.8%** (6356 / 7767) |
| **Unweighted** (mean of category %) | **90.9%** |
| Recipe/loot reachability of port items | **47.6%** (972 / 2043) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **still below ~90%**. Largest remaining hole is still **machine recipes** (69.9%),
then vanilla crafting (68.4%) and blocks (66.5%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2043 | **109.7%** | Phase 10 extract + wood burner BlockItem |
| Blocks | 1169 | 777 | **66.5%** | +1 `machine_wood_burner`; 4 casings → Dummyable TEs |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1334 | **68.4%** | CE estimate kept at 1950. +71 leftover JSON this pass |
| Machine recipes | ~2009 | 1405 | **69.9%** | CE denom unchanged. Port: prior 1362 + slopper 6 + engine 23 + radgen 14 |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **80.3%** / unweighted **90.0%**, machine recipes **1362 / ~2009
(67.8%)**, vanilla **1263 / 1950 (64.8%)**, blocks **776 / 66.4%**.

### Machine recipes: 1362 → 1405 (67.8% → 69.9%)

CE denom stayed **2009**. Port +43. No census-method cheat. Assembler JSON **untouched** (356).

Five Dummyable process families (block + BE + menu + screen + CE recipe table). No stub GUIs.

- **OreSlopperRecipes** 0 → 6 `RECIPES.add` — CE `TileEntityMachineOreSlopper.java:149-197`
  + JEI `OreSlopperHandler`. `machine_ore_slopper` Dummyable `{3,0,3,3,1,1}` offset 3 + 8 extras.
  100k HE, water→slop 16k, SPEED/EFFECT via slot scan, `ItemBedrockOreBase` → BASE grades.
  Animation / entity shred skipped.
- **EngineRecipes** 0 → 23 `recipes.put` — CE `EngineRecipes.java:16-39`.
  `machine_turbofan` Dummyable `{2,0,1,1,3,3}` offset 1. TE still gates on
  `FT_Combustible.FuelGrade.AERO` (CE `TileEntityMachineTurbofan`:182-185). AFTERBURN slot scan.
  Pollution / particles skipped.
- **RadGenRecipes** 0 → 14 `recipes.put` — CE `TileEntityMachineRadGen.java:236-251`.
  `machine_radgen` Dummyable `{2,0,3,2,1,1}` offset 2 + 3 extras. 8 short + 5 long waste
  + `gem_rad`→diamond. Depleted/tiny leftovers skipped (unregistered).
- **Hephaestus** — CE `TileEntityMachineHephaestus.java:132-182` (`FT_Heatable` HEATEXCHANGER).
  Named table `HeatRecipes` already counted. Dummyable `{11,0,1,1,1,1}` offset 1 + 8 extras.
  CE overlay-only → port adds a live ID+tank+heat menu (not a stub). `volcanic_lava_block` skipped
  (unregistered); lava / magma / `ore_volcano` heat scan kept.
- **Wood burner** — CE `TileEntityMachineWoodBurner.java:72-136`. Dummyable `{1,0,1,0,1,0}`
  offset 0 + 2 extras. **New** block id `machine_wood_burner`. Vanilla `RecipeType.SMELTING`
  burn time + optional `FT_Flammable` tank. Ash (`powder_ash`) skipped. On/oil via `clickMenuButton`.

### Vanilla crafting: 1263 → 1334 (64.8% → 68.4%)

+71 leftover shaped/shapeless from CE `CraftingManager.java:298-509`
(`scripts/phase11_wave8_crafts.py`). Wood burner / turbine / crates / press / ammo press / mixer /
solar / anvil / detonators / blades / stamps / building (reinforced/brick/concrete/meteor/tile/steel
deco) / barbed wire. Did **not** emit `powder_sawdust` / `gem_tantalium` / `coil_tungsten` as
results. Dropped unregistered ids (siren / microwave / furnace_iron / electrodes / fuse / gneiss /
spotlights / …). Assembler skip still **7**. Did not rewrite existing assembler JSON.
runServer recipe count 3039 → **3110** (+71).

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

## Recipe-graph reachability (cheap)

**47.6%** (972 / 2043). Prior session: 961 / 2042 (47.1%).

## Next single gap (not this session)

Still machine recipes (leftover getDict assembler + remaining Dummyable TEs after slopper /
turbofan / radgen / hephaestus / wood burner), vanilla crafting 68.4%. Blocks 66.5%.
Weighted 81.8% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
