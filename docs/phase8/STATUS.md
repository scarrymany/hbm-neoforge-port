# Phase 8 — remaining blocks / loot / world-gen structures

Phase 8 target (PORT_SPEC Phase 4 leftovers + recipe-graph gaps 4–5): remaining simple blocks,
structure loot, entity loot datagen, and world-gen structures toward 99% CE parity.

This slice landed code, not another research dump. Gaps below are still open.

## Landed this slice

- `ItemPoolsLegacy` — CE `itempool/ItemPoolsLegacy.java` (238 lines). All 7 pool names verbatim
  (`POOL_GENERIC` / `POOL_ANTENNA` / `POOL_EXPENSIVE` / `POOL_NUKE_TRASH` / `POOL_NUKE_MISC` /
  `POOL_VERTIBIRD` / `POOL_SPACESHIP`). Wired from `CommonEvents` FMLCommonSetup after RegisterEvent.
  Meta-flattened CE entries remapped to discrete items or skipped (circuits/casings/rods/grenade
  shells/fusion_core/scrap) via `BuiltInRegistries` lookup.
- `deco_steel` + `steel_scaffold` — CE `ModBlocks` entries missing from the original deco table;
  Antenna schematic + `POOL_ANTENNA` need both.
- `AntennaFeature` — CE `world/Antenna.java` (238 lines) as a `Feature<NoneFeatureConfiguration>`
  on the existing oil/meteor pipeline. 1-in-750 overworld (`CompatibilityConfig.antennaStructure`).
  Chest: 8 rolls of `POOL_ANTENNA`, EAST-facing, matching CE line 53.
- `ModDataGenerators` — merged Ore + OilMeteor + creeper `RegistrySetBuilder` bootstraps. Two
  `.add(CONFIGURED_FEATURE, …)` calls on one builder overwrite; ores would have vanished at runData.
- `red_barrel` / `pink_barrel` / `lox_barrel` / `taint_barrel` / `yellow_barrel` — CE
  `RedBarrel`/`YellowBarrel` (ModBlocks.java:751-756). Fire-chain 2.5F (red/pink), idle rad tick
  5/75 + detonation waste 35 + rad 35/1500 (yellow). LOX freezer + taint scatter + `toxic_block`
  1-in-3 replace still missing (`ExplosionThermo` / `BlockTaint` / toxic fluid not ported).
- `red_wire_coated` — CE `WireCoated` (full-cube `TileEntityCableBaseNT`).
- `ItemPoolsLegacy` remaps CE `block_*` → port autogen `*_block` (`block_tungsten` → `tungsten_block`).
- `ItemPoolsComponent` / `ItemPoolsSingle` / `ItemPoolsRedRoom` / `ItemPoolsPile` /
  `ItemPoolsVendingMachine` — CE pool-name strings verbatim (SHA `e5ed5ec` / `a23316ca` /
  `293649fc` / `4c61be97` / `565968ad`). Wired from `CommonEvents` FMLCommonSetup.
  Meta remaps: circuits, stamp_book PRINTING1–8, deco_computer IBM_300PL, blueprint_folder
  base/discover/secret. Grenade-shell / `item_secret` / material-meta bolt-shell skipped.

## Not this slice (still Phase 8)

- Legacy schematics: `Bunker.java` (1565), `Radio01`+`Radio02` (~7282). Capture as `.nbt`, do not
  transliterate `setBlockState` walls. See `docs/phase4/worldgen_structures_bunkers_stations.md`.
- Modern `.nbt` structures: vertibird / crashed_vertibird / radio_house / meteor_dungeon jigsaw.
  Assets live under CE `assets/hbm/structures/`; need `StructureType` + `structure_set` + block-id remap.
- `EntityLootSubProvider` — still absent. Existing mobs already drop via `dropCustomDeathLoot`
  (creeper variants, Mask Man, crabs, RAD Beast). Adding datapack tables on top would double drops.
  Convert Java drops → loot tables when Phase 9 lands more entities.
- Remaining ~500 missing block ids (PARITY_REPORT 642 / ~1165).
- CE `GeneralConfig.enableDungeons` gate — not ported; Antenna uses only the per-dim 1-in-N roll.
- Crater biome `EnumProxy` grass tint — `ModCraterBiomes` needs `META-INF/enumextensions.json`;
  bootstrap uses `GrassColorModifier.NONE` so runData can emit biome JSON.

## Verification (this follow-up)

- `./gradlew compileJava` — green after config + Dist packet fixes.
- `./gradlew runData` — **SUCCESS** (previous slice; 2984 files). Generated tree committed.
- `./gradlew runServer` — **SUCCESS.** Dedicated-server `"Done (0.592s)!"` on `d7418d2`.
  Fixed this turn: `defineInList` `List.of().contains(null)` NPE (`f387aee`); `@OnlyIn`
  `handleClient` DistCleaner NSME (`d7418d2` → `ClientPackets`). Non-fatal: 3 creeper variants
  missing `RegisterSpawnPlacementsEvent` (gold/phosgene/volatile). `runClient` skipped (no display).

## Explicitly not Phase 8

Phase 9 entities, Phase 10 bulk assets, Phase 11 final parity report.
