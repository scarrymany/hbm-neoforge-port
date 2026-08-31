# Phase 10 glue — texture aliases + lang

Datagen ID set from the last `runData` lang dump (2353 items / 849 blocks),
measured before aliases vs after pointing JSON at existing CE pngs.

## Texture miss-rate (registered id → playable model)

| | items | blocks |
|---|---:|---:|
| Registered (datagen census) | 2353 | 849 |
| Exact-path png before | 1146 (48.7%) | 373 (44.0%) |
| Playable miss **before** | **1588 (67.5%)** | **541 (63.7%)** |
| Unresolved **after** aliases | **764 (32.5%)** | **329 (38.8%)** |
| Alias models written | 824 item + later remaps | 255 block + later remaps |
| Blockstates given `""` default | — | 338 |

Playable = item model JSON exists *and* its texture resolves, or blockstate + block tex.

Remaining misses are mostly: powered-armor sets without inventory png (AJR/BJ),
Mats `BLOCK` autogen with no CE cube (`americium241_block`), debug ammo, TESR-only
machines. No art invented — those stay missing until a real CE file exists.

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
- jar `hbm-0.0.1.jar` **66,011,309** bytes (**62.95 MB**)
- `./gradlew runServer` — **Done (5.777s)** on wiped world. OilSpot still logs
  `setBlock in a far chunk` for `hbm:oil_bubble` (worldgen thread) but does **not**
  deadlock; spawn 2% → 51% → Done.
