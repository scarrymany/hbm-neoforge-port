# Phase 8 — remaining blocks / loot / world-gen structures

Phase 8 target (PORT_SPEC Phase 4 leftovers + recipe-graph gaps 4–5): remaining simple blocks,
structure loot, entity loot datagen, and world-gen structures toward 99% CE parity.

This slice landed code, not another research dump. Gaps below are still open.

## Landed this slice

- **Bunker + Radio Java generators** — extracted CE `setBlockState` / `placeDoorWithoutCheck`
  tables (not fake walls) into `data/hbm/schematic/{bunker,radio}.json`.
  - Bunker: 1438 placements, 16-id palette, 18 specials. CE `Bunker.java`:67 `y += 1`,
    11×9×15 AIR clear at :69-78, loot at :274-296 / :781 (`POOL_EXPENSIVE` 6+rand(2),
    4× `POOL_GENERIC` ×8, `POOL_ANTENNA` ×12), geiger at :277, iron doors via
    `Library.placeDoorWithoutCheck` :1119. Spawn 4 corners (`Bunker.java`:30-45).
    Dispatch `HbmWorldGen.java`:378, default `"0:1000"`.
  - Radio: 7148 placements, 22-id palette, 15 specials (`Radio01.java` + `Radio02.java`
    via `part2.generate_r00` at :5099). `Library.getRandomConcrete` (`Library.java`:1108)
    rolled live. Biome `temp >= 0.8F && rainfall > 0.7F` (`HbmWorldGen.java`:361-362) →
    jungle/swamp proxy. Spawn check `+(5,0,15)` (`Radio01.java`:30-31). Default 1-in-1000.
  - Config: `bunkerStructure` / `radioStructure` on `CompatibilityConfig` (same
    `defineListAllowEmpty` + `spawnMap` as antenna). Wired through the oil/meteor
    Feature → Configured → Placed → BiomeModifier pipeline.
- **`hbm:geiger` facing casing** — CE `ModBlocks.java`:857 / `GeigerCounter.java`:28.
  TE/click-to-read still Phase 2; bunker only needs the block.
- **Largest leftover non-machine family** (`Phase8Blocks`):
  16 dye-color stairs (`concrete_colored_stairs_*`, CE `ModBlocks.java`:153-166, 1.12
  `silver` kept) + 16 color slabs (`:212-227`) + 8 ext stairs (`:168-181`) + 4 ducrete
  slabs (`:229-232`) + 3 tile_lab slabs (`:233-235`) + jungle/dungeon cubes
  (`:407-434` / `:643-644`, plain `BlockBase` only) + `brick_fire_stairs` + geiger.
  **+56 ids.**
- Prior slices still landed: 70 CE `.nbt` + `StructureType hbm:nbt_poi` + 31/6
  structures/sets + wand_loot + deco/brick/meteor stairs/slabs + creeper placements
  + ItemPools* + Antenna.

## Census (this slice, recount)

| | count | notes |
|---|---|---|
| CE constructor-string ids | **~1135** | 1.12 meta blocks count as 1 (`concrete_colored`) |
| CE + array factory stairs | **~1159** | `makeConcreteStairs` / `makeConcreteExtStairs` |
| This port registered ids | **~666+** | previous ~610 + 56 this slice |
| `machine_*` still missing | **~117** | Phase 2 TEs — skip |
| `*_double_slab` | **~42** | intentional 1.21 flatten (SlabBlock owns DOUBLE) |
| Remaining non-machine (excl. double_slab + machine) | **still large** | `block_*` cubes (overlap Mats `*_block`), ores, rails, anvils, leftover jungle specials (trap/glyph/circle/hazard), deco_crt/emitter |

Do **not** start Phase 9/10/11 — non-machine gap is smaller but not honestly small.

## Not this slice (still Phase 8)

- Meteor dungeon is **core piece only** — CE 9-pool jigsaw (`wand_jigsaw`,
  `NTMWorldGenerator.java`:260-373) not walked. All meteor `*.nbt` pieces are in the jar.
- `EntityLootSubProvider` — still absent (Java `dropCustomDeathLoot` would double-drop).
  CE has **no** datapack `loot_tables/`; structure loot is ItemPool (already wired).
- Procedural `BunkerComponents` room graph — not this slice.
- CE `GeneralConfig.enableDungeons` gate — Features still per-dim 1-in-N only.
- Jungle specials (`brick_jungle_trap` / glyph / circle / lava / ooze / mystic / fragile).

## Verification (this follow-up)

- `./gradlew compileJava` — **0 errors** (pre-existing deprecation warnings).
- `./gradlew build` / `runData` / `runServer` — recorded after this STATUS is committed.

## Explicitly not Phase 8

Phase 9 entities, Phase 10 bulk assets, Phase 11 final parity report.
