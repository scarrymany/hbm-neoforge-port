# Phase 10 glue — texture aliases + lang

Census is Java `register`/`reg`/`stair`/`slab` + Mats autogen + plant/glyph loops +
bedrock-ore 6×26 grid. Lang keys are **not** ids (that polluted v2 to 4544/1433).

## Texture miss-rate (registered id → playable model)

| | items | blocks |
|---|---:|---:|
| Registered (Java+autogen+loops) | **1771** | **579** |
| Playable miss **before** v3 remaps | 320 (18.1%) | 96 (16.6%) |
| Unresolved **after** remaps | **164 (9.3%)** | **96 (16.6%)** |
| Prior datagen-lang dump (legacy) | 2353 / 32.5% after v1 | 849 / 38.8% after v1 |

Playable = item model JSON exists *and* its texture resolves, or blockstate + block tex.

Leftovers (`docs/phase10/LEFTOVER_MISSES.md`): Mats `BLOCK` cubes with no CE png
(26), TESR/machine/duct (29+18), debug ammo, remaining no-file ids. Powered armor
now aliases to existing `plate_armor_{ajr,fau,hev,lunar,dnt,titanium}`. No art invented.

## Remap rules (JSON → existing png)

- `{mat}_{shape}` → CE template (`wire_fine`, `shell`, `pipe`, `plate_cast`, …) or
  dedicated `wire_copper` / `ingot_steel`
- `{mat}_block` → `block_{mat}` (`titanium_block` → `block_titanium`)
- `aluminum` ↔ `aluminium`
- flatten `_0`…`_9` → parent stem
- `achievement_icon_acid` → `achievement_icon.acid`
- `b75` → `ammo_standard.b75`; `battery_sc_am241` → `battery_sc.am241`
- `bedrock_ore_new_*` → `bedrock_ore_*`
- `basalt_ore_gem` → `ore_basalt.gem`; `block_meteor_ore_X` → `ore_meteor.X`
- 1.12-only `meta=`/`taintage=` blockstates get a `""` default variant

## Lang

| locale | CE `.lang` keys | ported JSON keys |
|---|---:|---:|
| en_us | 8591 | **9269** (`item.X.name`→`item.hbm.X`, `tile.X.name`→`block.hbm.X`, plus autogen `%s` compose) |
| ru_ru | 8424 | **8854** |
| uk_ua | 5054 | **5144** |

Static files: `src/main/resources/assets/hbm/lang/{en_us,ru_ru,uk_ua}.json`.
`ModLanguageProvider` loads the static en_us first so `runData` cannot title-case-stomp.

`item.hbm.ingot_steel` = "Steel Ingot" (CE). Autogen e.g. `item.wire_fine.name=%s Wire` +
`hbmmat.aluminum` → `item.hbm.aluminum_wire` = "Aluminium Wire".

## Verified

- `./gradlew compileJava` — 0 errors
- `./gradlew build` — SUCCESS
- jar `hbm-0.0.1.jar` **66,225,207** bytes (**63.16 MB**)
- `./gradlew runServer` — **Done (5.842s)** on wiped world. OilSpot still logs
  `setBlock in a far chunk` for `hbm:oil_bubble` (worldgen thread) but does **not**
  deadlock; spawn 2% → 51% → Done.
