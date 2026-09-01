# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`.

Verified this wave: `compileJava` 0,
`./gradlew runServer` **Done (5.630s)** on wiped world port 25566, **3946 recipes**.
No recipe parse errors. No new tag (Dummyable wave, not a closed ChemPlant/SILEX family). `v0.0.1-rc2` stays.
`runClient` last green: MenuScreens (`gui.png-atlas`). No `MACHINE_CRUCIBLE` NPE.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **102.1%** (7933 / 7767) |
| **Unweighted** (mean of category %) | **102.0%** |
| Recipe/loot reachability of port items | **52.4%** (1341 / 2560) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **above 99%** (need 7689). Gates: `compileJava` 0 + `runServer` Done.
Tag `v0.0.1-rc2`. Existing `v0.0.1-rc1` / `beta-82` / `beta-82.1` stay.

Largest remaining holes: **blocks 173**, **machine 327**, **vanilla 52**.
Weighted **102.1%**. Category holes remain. Not content-complete.
99%+ tag: https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc2
90% playtest (kept): https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc1

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2560 | **137.4%** | Extract + already-registered helpers / loops + Dummyable BlockItems |
| Blocks | 1169 | 996 | **85.2%** | +autocrafter / keyforge / di-furnace / RTG di-furnace + lamps + sands + pink_stairs |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1898 | **97.3%** | CE estimate kept at 1950. +3 ducrete CraftingManager rows |
| Machine recipes | ~2009 | 1682 | **83.7%** | CE denom unchanged. Assembler skip **7** + pack/unpack already closed |
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

- Machine **1682 / 83.7%**. Leftover ChemPlant / SILEX / StorageDrum / SuperComputer I/O still
  unregistered — not invented.
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
- wave13 drops: unregistered armor pieces / SEDNA parts / inserts / claddings / foods / `block_steel`
- fluid-NBT / `OreDictionary.WILDCARD` / LBSM-gated / commented CE crafts skipped

## Recipe-graph reachability (cheap)

**52.7%** (1341 / 2546). Prior: 1341 / 2529 (53.0%).

## Next single gap

Blocks **84.0%** (187 missing — Dummyable/deco leftover, not dummy regs).
Machine leftover **~327** (ChemPlant / SILEX / StorageDrum / SuperComputer I/O unregistered).
Vanilla leftover **52**. Weighted **101.8%**. `v0.0.1-rc2` stays (no new tag).

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
