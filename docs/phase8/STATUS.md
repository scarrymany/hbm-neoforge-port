# Phase 8 — remaining blocks / loot / world-gen structures

Phase 8 target (PORT_SPEC Phase 4 leftovers + recipe-graph gaps 4–5): remaining simple blocks,
structure loot, entity loot datagen, and world-gen structures toward 99% CE parity.

This slice landed code, not another research dump. Gaps below are still open.

## Landed this slice

- **CE `.nbt` assets** — 70 templates copied from CE `assets/hbm/structures/` (skip `test-*`)
  into `data/hbm/structure/` (1.21 datapack path). `StructureManager.java`:55-66 / meteor tree.
- **`StructureType` `hbm:nbt_poi`** + `StructurePieceType` + `NbtPoiStructure` / `NbtPoiPiece`.
  Custom placer reads CE 1.10+ `palette`/`blocks` NBT (not fake `setBlockState` walls).
  1.12→1.21 id remap in `StructureBlockRemap` (vanilla flattenings + port flattened deco/slabs).
- **31 structures + 6 structure_sets** via datagen (`ModStructures` / `ModStructureSets`):
  vertibird / crashed_vertibird / radio_house / meteor_dungeon (core piece at y=32) plus the rest
  of CE `NTMWorldGenerator` single-NBT roster (shacks, ruins A–J, planes, factory, crane, dish,
  lab, forest_*, ocean trio, spire). Heights/offsets from `NTMWorldGenerator.java`:71-242.
  Biome gates in `findGenerationPoint` (sandy/flat/ocean/rain/low). Config gates:
  `enableStructures` / `enableOceanStructures` / `enableRuins`.
- **`BlockWandLoot` / `wand_loot`** — CE `BlockWandLoot.java`:235-400. Paste-time chest +
  `ItemPool` roll (`pool`/`min`/`max` keys). `debugStructures` keeps the marker.
- **Phase 8 block families** (`Phase8Blocks`, CE `ModBlocks.java`:86-210 / 398-405 / 497-504):
  deco metals (titanium/tungsten/lead/aluminium/rusty_steel/red_copper/beryllium/asbestos),
  missing brick/concrete cubes, meteor brick set, 25 stairs + 19 slabs, `block_electrical_scrap`.
  Stairs use `Blocks.STONE` base state — no `DeferredBlock.get()` in static init.
- **Creeper `RegisterSpawnPlacementsEvent`** — gold / phosgene / volatile / tainted / nuclear
  (`ON_GROUND` + `Monster::checkMonsterSpawnRules`).
- Prior slice (still landed): `ItemPools*` + barrels + coated wire + Antenna + `deco_steel`.

## Not this slice (still Phase 8)

- Legacy schematics: `Bunker.java` (1565) / `Radio01`+`Radio02` (~7282). Still no captured `.nbt`
  (do not transliterate `setBlockState` walls).
- Meteor dungeon is **core piece only** — CE 9-pool jigsaw (`wand_jigsaw`, `NTMWorldGenerator.java`:260-373)
  not walked. All meteor `*.nbt` pieces are in the jar for a later walker.
- `EntityLootSubProvider` — still absent (Java `dropCustomDeathLoot` would double-drop).
- Remaining block gap (recount this slice): CE constructor-string ids **1135**, this port
  `registerBlock("literal")` **567** plus ~44 stairs/slabs registered via helpers ≈ **610+**
  distinct ids. Largest leftover families: `machine_*` (117, mostly Phase 2 TE machines),
  `block_*` material cubes (77, overlap with Mats `*_block` autogen), remaining concrete/brick
  colored stairs/slabs (~84), deco props (32), rails/anvils/RBMK leftovers.
- Procedural `BunkerComponents` room graph — not this slice.
- CE `GeneralConfig.enableDungeons` gate — Antenna still per-dim 1-in-N only.

## Verification (this follow-up)

- `./gradlew compileJava` — **0 errors** (73 pre-existing deprecation warnings).
- `./gradlew build` — **SUCCESS.** Jar `build/libs/hbm-0.0.1.jar` **6,428,399** bytes
  (CE `.nbt` templates + generated structure/loot/lang).
- `./gradlew runData` — **SUCCESS.** 3138 generated files (was 2984; +155 this slice:
  31 structures, 6 structure_sets, block loot/lang for new ids).
- `runServer` not re-run this slice (no packet/config/registration-shape change beyond
  `StructureType` + spawn placements). Previous slice: dedicated `Done` on `d7418d2`.

## Explicitly not Phase 8

Phase 9 entities, Phase 10 bulk assets, Phase 11 final parity report.
