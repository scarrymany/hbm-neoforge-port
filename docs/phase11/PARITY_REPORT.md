# Phase 11 parity census (live, 2026-08-31)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg` + Mats autogen + plant/glyph/
bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build` SUCCESS, jar
`hbm-0.0.1.jar` **66,354,746** B (~63.28 MB), `./gradlew runServer` **Done (5.684s)**
on a wiped world (2366 recipes). `hbm:oil_bubble` still logs `setBlock in a far chunk` (no deadlock).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **61.5%** (4623 / 7511) |
| **Unweighted** (mean of category %) | **79.8%** |
| Recipe/loot reachability of port items | **41.4%** (768 / 1854) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (27.6%, was 23.2%),
then vanilla crafting (42.2%) and blocks (57.1%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1854 | **99.5%** | Phase 10 extract + pellet_charged/biomass/fuel_additive/pwr_fuel_depleted flatten |
| Blocks | 1169 | 667 | **57.1%** | extract + `ore()`/`stair`/`slab`/`registerMachine`/`registerResource` + PUREX/liquefactor |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 822 | **42.2%** | CE estimate kept at 1950. Port: 370 shaped + 323 shapeless + 123 smelting + 6 hbm crafting serializers |
| Machine recipes | ~1753 | 483 | **27.6%** | CE: regex + ~300 pack/unpack + now-visible PUREX (`this.register((PUREXRecipe)`). Port: 132 assembler + 92 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (PUREX / liquefactor / leftover chem)

Previous published snapshot: weighted **60.5%** / unweighted **79.1%**, machine recipes **394 / ~1695
(23.2%)**, blocks **665 / 1169 (56.9%)**, items **1830 / 1863 (98.2%)**, reachability **766 / 1830
(41.9%)**.

### Machine recipes: 394 → 483 (23.2% → 27.6%)

CE denom moved **1695 → 1753** because the census now matches `this.register((PUREXRecipe)`
(`PUREXRecipes.java` was invisible — 58 CE recipes). Honest, not a silent inflate.

JSON: assembler **125 → 132** (computed `known_ids` for drillbit/piston flatten; CE slugs only;
identical-payload leftovers dropped). Shredder 92, breeder 30 unchanged. `ass.liquefactor` still
blocked on `ANY_TAR` (`AssemblyMachineRecipes.java:314-316`). `ass.purex` landed
(`AssemblyMachineRecipes.java:242`).

Java:

| Class | Now | Notes |
|---|---:|---|
| PUREXRecipes | 51 | CE `PUREXRecipes.java:66–517`. `RECIPES.add` per recipe. Skipped: chance `thoriumsalt` `:362`, ICF `:467` (no `icf_pellet_depleted`), vitrification `:477-486` (no `sand_lead`), naquadria-guarded watz `:443-465` |
| LiquefactionRecipes | 27 | CE `LiquefactionRecipes.java:32–69`. `RECIPES.put`. Skipped tar keys / lignite gem / glyphid gland / plant_flower / `block_lead` / food→SALIENT |
| ChemPlantRecipes | 57 | + `chem.biogas` `:85`, `chem.deicer` `:112`, `chem.schrabidic` `:282`, `chem.batteryquantum` `:176`. Still skipped: ANY_TAR/`solid_fuel`/explosives/`dust`/glyphid/`chem.uf6` |
| MixerRecipes | 1 | helper still one `RECIPES.put`; runtime gained IONGEL/SCHRABIDIC/*LEADED (`MixerRecipes.java:48/:62/:83-85`) |
| others | 91 | centrifuge/SILEX/electrolyser/ammo/crucible/pyro/… unchanged |

`coil_tungsten` is still the coilgun ammo item — no second id.

### Machine blocks: 665 → 667 (56.9% → 57.1%)

- `machine_purex` — CE `ModBlocks` / `TileEntityMachinePUREX.java:56` (1M HE floor, recipe.power×100,
  3×24k in / 1×24k out). Auto-detect bag-of-items+fluids, 3 in / 6 out / battery. Real menu, not a stub.
- `machine_liquefactor` — CE `TileEntityMachineLiquefactor.java:46-48` (100k HE, 250/t, 60t, 24k tank).
  Item→fluid. Upgrades not ported (base numbers only).

### Items that unblocked recipes

CE `ModItems.java:1287` `pellet_charged`, `:1231-1232` biomass / biomass_compressed (`ItemFuel` 20/800),
`:393` `fuel_additive` flatten (ANTIKNOCK/DEICER), `nuclear_waste_tiny` / `nuclear_waste_vitrified`
(models+lang already in-tree), `pwr_fuel_depleted_<enum>` (15, companion to `PWRHotFuelItems`).

Reachability **766/1830 → 768/1854 (41.4%)** — more registered items, ratio barely moves.

No orphan items without a CE recipe. No invented art (machine cubes reuse `block_steel`, same as radar).

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure, not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself**
- Projectile tails = fallback renderer
- Assembler recipes whose output block/item is unregistered — not emitted (`ass.liquefactor` / excavator / FEL / PA)
- Texture misses with no CE file — documented, no invented art
- ElectrolyserMetal → foundry. Not stubbed.
- PUREX chance-output / ICF / vitrification / naquadria — missing I/O

## Recipe-graph reachability (cheap)

768 / 1854 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Machine Java recipes are only partly in the graph (JSON-backed + loot). Treat 41.4%
as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: leftover assembler needs excavator / FEL / PA / extra doors / `part_lithium`
/ `ANY_TAR` (`oil_tar`). Chemplant leftovers need explosives / `solid_fuel` / generic `dust`.
Solidifier is the liquefactor sibling (CE `SolidificationRecipes`, 47 entries) — no machine yet.
Blocks still 57.1%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
