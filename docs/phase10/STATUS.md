# Phase 10 — bulk CE assets (textures / models / sounds)

CE is content truth. This slice copies the 1.12 asset corpus into the 1.21.1
`assets/hbm/...` layout. No invented art. Neo-edition used only as a path hint
(`textures/block`, `textures/item`, snake_case sound event ids).

## Census (CE vs this port, after copy)

| Kind | CE (`upstream/hbm-ce`) | Port before | Port after |
|---|---:|---:|---:|
| `*.png` | 6967 | 2 | **6967** |
| `textures/blocks` → `textures/block` | 1860 | 0 | **1860** |
| `textures/items` → `textures/item` | 3189 | 0 | **3189** |
| `textures/gui` | 493 | 0 | **493** |
| `textures/models` | 1076 | 0 | **1079** (incl. sidecar png) |
| `textures/armor` | 133 | 0 | **133** |
| `textures/entity*` | 83 | 0 | **83** |
| `textures/particle` | 37 | 0 | **37** |
| `*.ogg` | 548 | 0 | **548** |
| `sounds.json` keys | 392 | 0 | **392** |
| …of which match `HBMSoundHandler` | 379 | — | **379 / 379** |
| blockstates | 932 | 0 | **932** (converted) |
| `models/block`+`models/item` json | 4084 | 0 | **4084** (converted) |
| `*.obj` | 634 | 0 | **634** |
| `*.mtl` | 57 | 0 | **57** |
| `*.mcmeta` | 190 | 0 | **190** |
| assets tree size | 139M | ~2M | **~135M** |

13 `sounds.json` keys have no `HBMSoundHandler` field (same CE orphans as
`docs/phase5/sound_wiring_and_assets.md`: vault scrape/thud, dieselOperate,
missileAssembly, a few reload/fm.*). They are still shipped under snake_case
keys so Cassette/`getOrCreate` can resolve them.

## Mapping applied

- `textures/blocks/**` → `textures/block/**`, `textures/items/**` → `textures/item/**`
- Model texture keys `hbm:blocks/x` → `hbm:block/x`, `hbm:items/x` → `hbm:item/x`
- Blockstate `variants.normal` → `""`; 1.12 implicit `hbm:foo` model → `hbm:block/foo`
- 1.12 slab `half=*,variant=*` → `type=*`
- Parents: `item/generated` → `minecraft:item/generated`, `block/half_slab` →
  `minecraft:block/slab`, `block/upper_slab` → `minecraft:block/slab_top`
- `sounds.json` keys snake_cased to match `HBMSoundHandler.reg(...)` (ogg paths
  stay CE filenames — already lowercase)
- MTL `map_Kd hbm:blocks/` → `hbm:block/`
- Existing `models/weapons/animations/*.json` (12) not overwritten

## Deliberately not copied

| CE tree | why |
|---|---|
| `lang/*.lang` | 1.12 format; port already has generated `en_us.json` |
| `advancements/` | 1.12 location/schema; 1.21 lives under `data/` |
| `manual/`, `disks/`, `optifine/` | not textures/sounds; later slice |
| CE Java | never vendor |

## Code nits this slice

- Datagen skips a block/item when a CE-converted blockstate / item model already
  exists (`--existing src/main/resources`), so `runData` cannot cube-all-shadow
  custom CE models.
- RBMK BER texture paths `textures/blocks/rbmk/` → `textures/block/rbmk/`.

## Leftovers (not this slice)

- Flatten/autogen registry ids that don't match CE texture filenames
  (`steel_ingot` vs `ingot_steel`) — no invented aliases.
- 1.12 `meta=` blockstates vs 1.21 flattened blocks: unused or log-noisy, not pink
  for the `normal`/facing majority.
- Entity loot datagen (Phase 8 leftover).
- IRailNTM implementors still absent in CE.

Repro: `python3 scripts/phase10_copy_ce_assets.py` (idempotent; needs
`upstream/hbm-ce`).
