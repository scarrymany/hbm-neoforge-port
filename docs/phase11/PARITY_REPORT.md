# Phase 11 parity census (live, 2026-08-31)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg` + Mats autogen + plant/glyph/
bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. This revision: `compileJava` 0 errors. `./gradlew build` /
`runServer` numbers land in a follow-up commit after boot.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **62.8%** (4876 / 7767) |
| **Unweighted** (mean of category %) | **81.1%** |
| Recipe/loot reachability of port items | **41.1%** (785 / 1910) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (33.2%, was 31.5%),
then vanilla crafting (42.2%) and blocks (58.2%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1910 | **102.5%** | Phase 10 extract + flatten extras (`sulfur`/`niter`/7 casings + 4 machine BlockItems) |
| Blocks | 1169 | 680 | **58.2%** | extract + helpers + ammo press / arc welder / soldering / plasma forge |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 823 | **42.2%** | CE estimate kept at 1950. + ammo-press craft (`CraftingManager.java:332`) |
| Machine recipes | ~2009 | 666 | **33.2%** | CE: regex + ~300 pack/unpack + Solidification + PA + now-visible AmmoPress/ArcWelder/Soldering `recipes.add` + PlasmaForge `this.register((PlasmaForgeRecipe`. Port: 150 assembler + 92 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto(Fluids.` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (AmmoPress / ArcWelder / Soldering / PlasmaForge / leftover assembler / chem)

Previous published snapshot: weighted **62.9%** / unweighted **80.7%**, machine recipes **573 / ~1817
(31.5%)**, blocks **676 / 1169 (57.8%)**, items **1897 / 1863 (101.8%)**, reachability **783 / 1897
(41.3%)**.

### Machine recipes: 573 → 666 (31.5% → 33.2%)

CE denom moved **1817 → 2009** because the census now matches AmmoPress/ArcWelder/Soldering
`recipes.add` and PlasmaForge `this.register((PlasmaForgeRecipe` on those classes only. Honest —
global `recipes.add` stays off. Weighted dipped 62.9 → 62.8 because CE grew faster than port
(+192 vs +93); machine **%** still rose.

JSON: assembler **147 → 150** (`fusionplasmaforge` CE `AssemblyMachineRecipes` plasma-forge line,
`explosivelenses1` `:687`, `fleijacharge` `:725`). Slugs lowercase. Shredder 92, breeder 30 unchanged.

Java:

| Class | Now | Notes |
|---|---:|---|
| ChemPlantRecipes | 72 | + `chem.hydrogencoke` (`:46` ANY_COKE), `chem.meatprocessing` (`:235` glyphid_meat→sulfur/niter+SALIENT), `chem.uf6` (`:361` yellowcake+fluorite→sulfur+UF6) |
| AmmoPressRecipes | 61 | CE `:registerDefaults` 9-slot grid. 55 generated + 6 hand (flame_diesel/gas/balefire, tau_uranium, coil_tungsten, coil_ferrouranium — CE `:936+`/`:1024`/`:1038`). `coil_tungsten` is the existing GunEnergy ammo id — no second register |
| ArcWelderRecipes | 13 | CE `ArcWelderRecipes.java`. Dense wires + welded plates + 2× LDE (`part_generic_lde`, FIBER=`ingot_fiberglass`). Skipped missiles/thrusters/unregistered `plate_welded` mats |
| SolderingRecipes | 9 | CE circuit family (no 528/LBSM). Upgrade-template recipes skipped (`upgrade_template` unregistered) |
| PlasmaForgeRecipes | 11 | CE `PlasmaForgeRecipes.java`. Welded plates + HDE + euphemium/DNT plates + `plsm.icfpress`. No PlasmaNetwork — `setInputEnergy` is extra HE on complete |
| others | ~228 | solidifier/PA/PUREX/liquefaction/centrifuge/… unchanged |

### Machine blocks: 676 → 680 (57.8% → 58.2%)

Real TE + menu, no stub GUI. Cubes reuse `block_steel`.

- `machine_ammo_press` — CE `ModBlocks.java:993` / `TileEntityMachineAmmoPress`. 9-slot positional + out. No energy. Auto-match first recipe. Fluid-slot recipes stored, not consumed (no tank).
- `machine_arc_welder` — CE `:1059` / `TileEntityMachineArcWelder`. maxPower 2k (grows `consumption*100`), 3 in + out + battery + 24k tank. Upgrades skipped.
- `machine_soldering_station` — CE `:1061` / `TileEntityMachineSolderingStation`. slots 0–2 toppings / 3–4 pcb / 5 solder + out + battery + 8k tank.
- `fusion_plasma_forge` — CE `ModBlocks.java:1327` / `TileEntityFusionPlasmaForge`. Standalone HE, no PlasmaNetwork. maxPower 10M, 6 in + out + battery + 16k tank.

`@EventBusSubscriber` `bus=MOD` on `WorkshopClientRegistry` / `FusionClientRegistry` / `CommonEvents`.
No `DeferredHolder.get()` in static init. Recipe tables register from `FMLCommonSetupEvent` after
`RegisterEvent`.

### Items that unblocked recipes

CE `ModItems` sulfur / niter dusts (not `crystal_*`). CE `AmmoPressRecipes.java:47–59` `EnumCasingType`
flatten: `casing_small`/`large`/`small_steel`/`large_steel`/`shotshell`/`buckshot`/`buckshot_advanced`.

Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second `coil_tungsten`.

Tags: `hbm:any_smokeless`, `hbm:any_highexplosive`, `hbm:any_coke`, `hbm:any_plastic`,
`hbm:any_hardplastic`.

Reachability **783/1897 → 785/1910 (41.1%)**.

No invented art (machine cubes reuse `block_steel`). Item textures copied from CE where missing.

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
- AmmoPress fluid-slot recipes (FLAME_*) — stored, not consumed (no tank on the press)
- ArcWelder/PlasmaForge missiles, thrusters, ICF laser-component metas, DFC — missing I/O items/blocks
- Soldering `upgrade_template` family — item unregistered
- Assembler leftover doors / oil-chain / `machine_precass` — missing blocks

## Recipe-graph reachability (cheap)

785 / 1910 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 41.1% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: leftover assembler (doors, oil-chain, `machine_precass`), more ArcWelder
missiles once items exist, Soldering upgrades, PlasmaForge ICF/DFC casings. Blocks 58.2%. Vanilla
crafting 42.2%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
