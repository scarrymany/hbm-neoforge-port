# Phase 10 leftover misses (strict texture/model)

Registry vs CE png/json. Copied **only** missing CE assets. No invented art.
Data-component register() strings (`heat`, `gun_states`, …) are not items and are excluded.

- Census: items **2634**, blocks **978**
- Previous leftover wave: items **20**, blocks **48** (census 2616/960)
- Origin before this wave (cm_* family registered, no models): leftover would be items **36**, blocks **64**
- This wave AFTER: items **20** (0.76%), blocks **48** (4.91%)
- Closed this wave: 16 `cm_{block,sheet,tank,port}_{steel,desh,bismoid_bronze,resistant}`

Strict playable = model/blockstate exists **and** every referenced texture file exists
(or is vanilla `minecraft:`).

## This wave (CE aliases / same-class textures only)

- `rbmk_display` → CE `blocks/rbmk/rbmk_display` (`TODO(CE:RBMKMiniPanelBase.java:145)` same sprite as blank)
- `crane_extractor` → CE cube `crane_ejector` (`TODO(CE:ModBlocks.java:1113)` `new CraneExtractor(..., "crane_ejector")`)
- `cm_*` family → CE `cm_{block,sheet,tank,port}_{steel,alloy,desh,tcalloy}` (`TODO(CE:BlockEnums.java:40-44)` STEEL/ALLOY/DESH/TCALLOY; port `bismoid_bronze`=ALLOY, `resistant`=TCALLOY)

Turret **world** cubes skipped: CE has no cube png for chekhov/howard/… Inventory already CE `block_steel`.
TESR machine world cubes skipped: inventory already wired from `textures/models/*`.
`cargo_elevator` / `red_connector_super` world cubes skipped: CE TESR/OBJ, no unused cube png.

## Still leftover — true CE-missing

Do **not** invent cubes.

### Mats BLOCK autogen — CE never shipped a storage cube (`TODO(CE:Mats.java)`) (8)

- `americium241_block` `americium242_block` `americiumrg_block`
- `carbon_block` — not `block_graphite`
- `ghiorsium336_block` `neodymium_block` `plutonium241_block` `technetium99_block`

### Items without a CE inventory png (10)

- `balls_spawner` / `balls_spawner_spent` — port ids; CE is `brick_jungle_circle`
- `barrel` — generic id; CE only has colored `barrel_*`
- `counterfeit_keys` `dyatlov` `lock` — `lock` is port-extra; CE has `padlock`
- `gear_bronze` / `gear_steel` — CE only `gear_large.png`
- `jetpack_glider` — armor layer only, no inventory png (craft exists)
- `wiring_tool` — not `wiring_red_copper` (different item)

### Blocks still TESR / no cube (~48)

World leftover = mats above + TESR machines (inventory may already be wired) + nuke casings without a CE **block** png + turrets (no CE cube) + `balls_spawner*` `barrel` `pile_device_*` + `cargo_elevator` `red_connector_super`.

#### Item leftover ids (20)

`americium241_block` `americium242_block` `americiumrg_block` `balls_spawner` `balls_spawner_spent` `barrel` `carbon_block` `cargo_elevator` `counterfeit_keys` `dyatlov` `gear_bronze` `gear_steel` `ghiorsium336_block` `jetpack_glider` `lock` `neodymium_block` `plutonium241_block` `red_connector_super` `technetium99_block` `wiring_tool`

#### Block leftover ids (48)

mats above + `balls_spawner` `balls_spawner_spent` `barrel` `cargo_elevator` `red_connector_super` + TESR machines (`machine_assembly_machine` `machine_chemical_plant` `machine_combustion_engine` `machine_crystallizer` `machine_electrolyser` `machine_fraction_tower` `machine_gascent` `machine_icf_reactor` `machine_industrial_turbine` `machine_large_turbine` `machine_minirtg` `machine_mixer` `machine_powerrtg` `machine_silex` `machine_solar_boiler` `machine_steam_engine` `machine_turbine_gas` `machine_watz_reactor` `machine_wood_burner`) + nuke casings (`nuke_balefire` `nuke_custom` `nuke_fleija` `nuke_gadget` `nuke_man` `nuke_tsar`) + `pile_device_1` `pile_device_2` + turrets (`turret_chekhov` `turret_friendly` `turret_fritz` `turret_howard` `turret_jeremy` `turret_maxwell` `turret_richard` `turret_tauon`)
