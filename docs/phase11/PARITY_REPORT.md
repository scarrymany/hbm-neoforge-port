# Phase 11 parity census (live, 2026-08-31)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg` + Mats autogen + plant/glyph/
bedrock loops, plus this session's flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build` SUCCESS, jar
`hbm-0.0.1.jar` **66,311,278** B (~63.24 MB), `./gradlew runServer` **Done (5.014s)**
on a wiped world (2359 recipes after lowercase assembler path fix). `hbm:oil_bubble`
still logs `setBlock in a far chunk` (no deadlock).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **60.5%** (4508 / 7453) |
| **Unweighted** (mean of category %) | **79.1%** |
| Recipe/loot reachability of port items | **41.9%** (766 / 1830) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (23.2%, was 13.4%),
then vanilla crafting (42.2%) and blocks (56.9%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1830 | **98.2%** | Phase 10 extract + expensive/part_generic/battery-pack flatten |
| Blocks | 1169 | 665 | **56.9%** | extract + `ore()`/`stair`/`slab`/`registerMachine`/`registerResource` |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 822 | **42.2%** | CE estimate kept at 1950. Port: 370 shaped + 323 shapeless + 123 smelting + 6 hbm crafting serializers |
| Machine recipes | ~1695 | 394 | **23.2%** | CE: regex + ~300 pack/unpack. Port: 125 assembler + 92 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`. New expensive/part_generic items use CE
dotted pngs (`item_expensive.steel_plating.png`, `part_generic.piston_pneumatic.png`, …).

## What changed this session (machine-recipe + missing-machine-block hole)

Previous published snapshot: weighted **57.6%** / unweighted **77.5%**, machine recipes **227 / ~1695
(13.4%)**, blocks **649 / 1169 (55.5%)**, items **1796 / 1863 (96.4%)**, reachability **746 / 1796
(41.5%)**.

### Machine recipes: 227 → 394 (13.4% → 23.2%)

JSON: assembler **110 → 125** (identical slug-dupes from two emitter naming schemes dropped; CE alts kept), shredder **44 → 92**, breeder 30 (unchanged).

Java (census now also matches `RECIPES.put`, the port's actual map-insert; previous regex only hit
lowercase `recipes.put` / `RECIPES.add`, so centrifuge / SILEX / electrolyser were undercounted):

| Class | Now | Notes |
|---|---:|---|
| ChemPlantRecipes | 53 | CE `ChemicalPlantRecipes.java:41–391`. Was 4. Skipped biomass / fuel_additive / explosives / `pellet_charged` / generic `dust` / `canister_napalm` / sulfur item |
| CentrifugeRecipes | 29 | + CE `:267-285` crystals (thorium/Pu/Ti/W/Be/schraranium/schrabidium/rare/cobalt) |
| SILEXRecipes | 19 | + CE `:50-54` `PU.ingot()` |
| ElectrolyserFluidRecipes | 7 | already present; newly counted |
| others | 39 | ammo/crucible/pyro/magic/mixer/heat/cyclotron/crystallizer |

Assembler fluids are a real CE path (`AssemblyMachineRecipes.java:78-96` — sulfuric / lubricant /
perfluoromethyl). `AssemblerRecipe` now has optional `input_fluids`/`output_fluids`; the BE consumes
and retargets empty tanks. `coil_tungsten` is still the coilgun ammo item — no second id.

Skipped assembler (not invented): unregistered machines (PUREX, excavator, FEL, PA, …),
`pellet_charged`, `part_lithium` family, extra airlock doors, `KEY_GREEN` ore-dict.

### Machine blocks: 649 → 665 (55.5% → 56.9%)

New this session (casings + real TE where logic existed):

- `machine_radar` / `machine_radar_large` — CE `ModBlocks.java:1193-1194`, TE
  `TileEntityMachineRadarNT.java:85-87` (100k HE, 500/t, range 1000) /
  `TileEntityMachineRadarLarge.java:16` (range 3000). Ping + `IRadarDetectableNT` scan + comparator.
  Map GUI / satellite link / jammer **not** ported.
- `vault_door` / `blast_door` / `fire_door` / `sliding_blast_door` — CE custom multiblock TEs; this
  port registers **casings** (`BlockBase`) so assembler can output them. Not vanilla `DoorBlock`.
  Full open/close TE later.

Census helper now also sees `registerMachine` / `registerResource` (oil well / pumpjack / fracker /
refinery + oil-chain resources were already registered, previously invisible to the block extract).

Fracker was already `OilChainBlocks.MACHINE_FRACKING_TOWER`. Not re-stubbed.

### `item_expensive` / `part_generic` / reachability

CE `ModItems.java:2800` / `ItemEnums.EnumExpensiveType` (10) and `:2798` / `EnumPartType` (6),
flattened to `item_expensive_<enum>` / `part_generic_<enum>`. Unlocks assembler expensive-part chain
(`AssemblyMachineRecipes.java:74-97`) and `chem.polarized`. Reachability **746/1796 → 766/1830
(41.9%)** — more registered items, so the ratio barely moves; absolute reachable +20.

No orphan items without a CE recipe.

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure, not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself**
- Projectile tails = fallback renderer
- Assembler recipes whose output block/item is unregistered — not emitted
- Texture misses with no CE file — documented, no invented art
- Radar map GUI / extra airlock door TEs / PUREX / press / excavator — not stubbed

## Recipe-graph reachability (cheap)

766 / 1830 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Machine Java recipes are only partly in the graph (JSON-backed + loot). Treat 41.9%
as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: remaining assembler sections need missing **machine blocks** (PUREX, excavator,
FEL, particle accelerator, extra doors) or unregistered parts (`pellet_charged`, `part_lithium`).
Chemplant leftovers need explosives / biomass / `fuel_additive`. Do not emit recipes for
unregistered machines. Blocks still 56.9%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
