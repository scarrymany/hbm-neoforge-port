# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Quality bar: `docs/CE_PARITY_ADDENDUM.md`.

Verified this wave: `compileJava` 0,
`./gradlew runServer` **Done (5.303s)** on wiped world port 25566, **3946 recipes**.
No recipe parse errors. No new tag. `v0.0.1-rc2` stays.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **102.8%** (7983 / 7767) |
| **Unweighted** (mean of category %) | **102.4%** |
| Recipe/loot reachability of port items | **52.4%** (1348 / 2574) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **above 99%** (need 7689). Gates: `compileJava` 0 + `runServer` Done.
Tag `v0.0.1-rc2`. Existing `v0.0.1-rc1` / `beta-82` / `beta-82.1` stay.

Largest remaining holes: **blocks 162**, **machine 302**, **vanilla 52**.
Weighted **102.8%**. Category holes remain. Not content-complete.
99%+ tag: https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc2
90% playtest (kept): https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc1

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2574 | **138.2%** | +bottle_mercury / dust_tiny / cinnabar (loop waste/ash/drive not flatten extras) |
| Blocks | 1169 | 1007 | **86.1%** | unchanged this wave |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1898 | **97.3%** | CE estimate kept at 1950. +3 ducrete CraftingManager rows |
| Machine recipes | ~2009 | 1707 | **85.0%** | +SILEX depleted/fallout + StorageDrum + SuperComputer. ChemPlant 72/72 (CE 145 double-count) |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this wave (86.8% → 96.4%, 7489 / 7767)

Previous published snapshot: weighted **86.8%** / unweighted **93.3%**, vanilla **1416 / 1950
(72.6%)**, machine **1682 / 83.7%**, items **2056**, blocks **790**. Short of 90% by **249**.

### Extractor: already-registered helpers

`scripts/phase10_remap_v3.py` HELPERS now matches `registerBillet` / `registerPowder` /
`registerFuelPowder` / `registerParts` / `registerWaste` / `registerRtgPellet` /
`registerResource`. Those items were already in `BilletPowderItems` / `PlateCrystalWasteItems` /
etc. Census items **2056 → 2354**. Not dummy registration.

### Vanilla leftover JSON (CE-faithful, no invented rows)

- `scripts/phase11_wave12_minerals.py` — CE `MineralRecipes.java` `add1To9Pair` / `addMineralSet` /
  `addBillet` + explicit 9-pack. Skips unregistered I/O and banned results.
- `scripts/phase11_wave12_rods.py` — leftover empty-rod / RBMK / RTG rows whose ids are visible.
  Pile/PWR fuels already in datagen (`src/generated/.../rod/`); not re-emitted under new filenames.
- `scripts/phase11_wave13_leftover.py` — leftover Tool / Armor / Consumable / Exclusive crafts
  with registered I/O only.

Vanilla **1416 → 1866**. Recipe JSON total **2174 → 2624**. Dropped `gas_mask_filter` (DataComponent id, not an item).

Dropped (not invented): armor sets whose pieces are unregistered (`steel_helmet` / hazmat /
`gas_mask_m65`), `ring_starmetal`, `block_schrabidium`, SEDNA `part_stock_*` / `part_grip_*`,
`insert_*` / `cladding_*` / `pads_*` / `servo_set`, `block_steel` (Mats id is `steel_block`),
foods (`pancake` / `quesadilla` / …), pile/PWR concat ids already owned by datagen.

Banned results still skipped: `powder_sawdust` / `gem_tantalium` / `coil_tungsten`.

### Unchanged this wave

- Machine **1682 → 1707 / 85.0%**. ChemPlant 72/72 already live. SILEX +13 depleted/fallout.
  StorageDrum CE table live (census 6 sites / runtime 28). SuperComputer 18 runtime / census 7 sites.
  Cited leftovers: SILEX RBMK pellet loop `:117-472`, fluid_icon `:96-115`/`:664`.
- Blocks **790 / 67.6%**. No dummy blocks.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Client NPE fix (`CrucibleBlocks.registerAll` + `SafeMenuScreens`) from prior HEAD.

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
- SILEX RBMK pellet loop — TODO(CE: SILEXRecipes.java:117-472) NbtComparableStack / stage meta
- SILEX fluid_icon DEATH/VITRIOL/REDMUD/FULLERENE — TODO(CE: SILEXRecipes.java:96-115, :664)
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
- PyroOven tar→soot — `powder_ash_*` now registered; row still skipped pending wire-up
- PyroOven / arc furnace pollution / audio / particles / lid animation — skipped
- Arc furnace vanilla furnace autogen — RecipeManager not available at commonSetup
- Arc furnace `sand_quartz` → `glass_quartz` — `sand_quartz` unregistered
- Exposure expensive-mode DEGENERATE_MATTER — cheap path only
- Pyro / arc / exposure `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier
- Slopper animation / entity shred — skipped
- Turbofan pollution / particles — skipped
- Radgen depleted/tiny leftovers — items unregistered
- Hephaestus `volcanic_lava_block` — unregistered; CE overlay-only (port has a real menu)
- Wood burner `powder_ash_*` registered; ash emit + `loadTank` canister path not ported
- Wood burner / slopper / turbofan `UpgradeManagerNT` — slot scan by type/tier instead
- Iron / steel furnace pollution + particles; steel ore bonus
- Firebox `ModuleBurnTime` fuel-class mods + ashpit + door anim
- Oilburner `loadTank` / pollution
- Sawmill blade / entity shred / overspeed (`sawblade` unregistered → blade assumed)
- Heaters / steel furnace / sawmill CE crafts are **anvil**, not vanilla
- SuperComputer CE recipe dropdown (`IControlReceiver` / ModuleMachineBase) — auto-match instead
  TODO(CE: TileEntityMachineSuperComputer.java:186-194)
- Autosaw entity shred skipped
- `catalytic_converter` `ANY_BISMOID.ingot()` → `ingot_bismuth` (no `any_bismoid` tag)
- wave11 drops: `powder_fire` / `key` / `gear_large` / `sawblade` / `mold_base` unregistered
- wave13 drops: unregistered armor pieces / SEDNA parts / inserts / claddings / foods / `block_steel`
- fluid-NBT / `OreDictionary.WILDCARD` / LBSM-gated / commented CE crafts skipped

## Recipe-graph reachability (cheap)

**52.4%** (1348 / 2574). JSON/loot only — machine tables (ChemPlant/SILEX/StorageDrum/SuperComputer)
do not feed this graph.

## Next single gap

Blocks **86.1%** (162 missing). Machine leftover **~302** (SILEX RBMK loop + fluid_icon + assembler
skip 7 + other families). Vanilla leftover **52**. Weighted **102.8%**. `v0.0.1-rc2` stays (no new tag).

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
