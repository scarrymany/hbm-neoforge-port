# Phase 11 parity census (live, 2026-08-31)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg` + Mats autogen + plant/glyph/
bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors (build +
`runServer` recorded after this snapshot). `hbm:oil_bubble` still logs `setBlock in a far chunk`
(no deadlock).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **62.9%** (4765 / 7575) |
| **Unweighted** (mean of category %) | **80.7%** |
| Recipe/loot reachability of port items | **41.3%** (783 / 1897) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (31.5%, was 27.6%),
then vanilla crafting (42.2%) and blocks (57.8%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1897 | **101.8%** | Phase 10 extract + flatten extras (CE meta families now split: `oil_tar_*`, `particle_*`) |
| Blocks | 1169 | 676 | **57.8%** | extract + helpers + solidifier/FEL/excavator/6 PA parts |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 822 | **42.2%** | CE estimate kept at 1950 |
| Machine recipes | ~1817 | 573 | **31.5%** | CE: regex + ~300 pack/unpack + PUREX + now-visible Solidification `registerRecipe`/`registerSFAuto` + PA `recipes.add`. Port: 147 assembler + 92 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto(Fluids.` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (excavator / FEL / PA / ANY_TAR / chem leftovers / solidifier)

Previous published snapshot: weighted **61.5%** / unweighted **79.8%**, machine recipes **483 / ~1753
(27.6%)**, blocks **667 / 1169 (57.1%)**, items **1854 / 1863 (99.5%)**, reachability **768 / 1854
(41.4%)**.

### Machine recipes: 483 → 573 (27.6% → 31.5%)

CE denom moved **1753 → 1817** because the census now matches Solidification `registerRecipe(FLUID,`
/`registerSFAuto(FLUID` (`SolidificationRecipes.java:63–115`) and PA `recipes.add`
(`ParticleAcceleratorRecipes.java:65–88`). Honest, not a silent inflate. Global `recipes.add` was
**not** turned on (would pull AmmoPress/ArcWelder/etc. that this pass did not port).

JSON: assembler **132 → 147** (`liquefactor`/`solidifier`/`fel`/`excavator`/`beamline`/`rfc`/
`quadrupole`/`dipole`/`source`/`detector`/`partlith`/`partberyl`/`partcoal`/`partcop`/`partplut`).
Slugs lowercase. Shredder 92, breeder 30 unchanged.

Java:

| Class | Now | Notes |
|---|---:|---|
| SolidificationRecipes | 49 | CE `:63–115`. `RECIPES.put` + `registerSFAuto(Fluids.` formula (`:119-134`). BALEFIRE put twice matching CE `:85` then `:115` |
| ChemPlantRecipes | 69 | + tarsand `:102`, tel `:107`, biosolidfuel `:246`, biooilsolidfuel `:250`, coltancleaning `:293`, coltancrystal `:304`, cordite `:310`, rocketfuel `:315`, dynamite `:320`, tnt `:324`, tatb `:329`, napalm `:339`. Still skipped: glyphid meat `:235`, `chem.uf6` `:361` |
| LiquefactionRecipes | 31 | + 4 tar keys (`oil_tar_coal/wood/wax/paraffin`) |
| ParticleAcceleratorRecipes | 10 | CE `:65–88`. Skipped `:84-86` SBD.ingot() (no schrabidate INGOT). Chicken→`nugget` food item |
| PUREXRecipes | 51 | unchanged |
| others | ~94 | centrifuge/SILEX/electrolyser/ammo/crucible/pyro/… unchanged |

`coil_tungsten` is still the coilgun ammo item — no second id.

### Machine blocks: 667 → 676 (57.1% → 57.8%)

- `machine_solidifier` — CE `TileEntityMachineSolidifier.java:46-48` (100k HE, 250/t, 60t, 24k tank). Fluid→item. Real menu.
- `machine_fel` — CE `TileEntityFEL.java:55-56` (2e9 HE, 1000×4^ordinal). Crystal + battery. Sets `SilexBlockEntity.setLaserMode`.
- `machine_excavator` — CE `TileEntityMachineExcavator.java:70-71` (10M HE, 10k/block). Drillbit silk/vein/fortune. 9 outputs.
- `pa_beamline` / `pa_rfc` / `pa_quadrupole` / `pa_dipole` / `pa_source` / `pa_detector` — CE Albion parts (`AssemblyMachineRecipes.java:462-484`). Detector is the recipe consumer (`TileEntityPADetector.java:45`, 100k HE, momentum gate). Beamline has no GUI (none in CE). Other parts: coil + battery.

### Items that unblocked recipes

CE `ModItems.java:1325` `oil_tar`/`EnumTarType` flatten, `:1330-1333` `solid_fuel`/`solid_fuel_bf`,
`:1155` `dust`, `:1234`/`:1237` cordite/`ball_tnt`, `:943` `bio_wafer`, `:2314+` `particle_*`,
cyclotron `part_*` (`:151-160` assembler). `ANY_TAR` is tag `hbm:any_tar`.

Reachability **768/1854 → 783/1897 (41.3%)**.

No invented art (machine cubes reuse `block_steel`). Item textures copied from CE.

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
- Full Albion beam physics (source→RFC→quad/dipole→detector particle transit) — detector runs the recipe table locally with HE→momentum

## Recipe-graph reachability (cheap)

783 / 1897 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 41.3% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: leftover assembler (extra doors, more oil-chain), chem `uf6`/`meatprocessing`,
AmmoPress/ArcWelder/Soldering Java tables, PlasmaForge. Blocks still 57.8% — more `machine_*`
casings. Vanilla crafting 42.2%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
