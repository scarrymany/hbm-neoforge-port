# Phase 10 leftover misses (strict texture/model)

Registry vs CE png/json. Copied **only** missing CE assets. No invented art.
Data-component register() strings (`heat`, `gun_states`, …) are not items and are excluded.

- Census: items **2589**, blocks **949**
- Previous leftover wave (origin): items **25**, blocks **49**
- This wave AFTER: items **18**, blocks **46**
- Closed this wave: items **7** + `rbmk_display_blank`, blocks **3** + `rbmk_display_blank`

Strict playable = model/blockstate exists **and** every referenced texture file exists
(or is vanilla `minecraft:`).

## This wave (CE aliases / same-class textures only)

- `ammo_debug` → CE `ammo_45` (`TODO(CE:GunFactory.java)` `ItemBakedBase("ammo_debug", "ammo_45")`)
- `fmn` → CE `tablet` (`TODO(CE:ModItems.java:143)` `ItemPill(..., "tablet")`)
- `fext_water` / `fext_sand` → CE `ammo_fireext` metas 0/2 (`TODO(CE:XFactoryTool.java)`)
- `concrete_light_gray` → CE `concrete_silver` (1.12 silver = light gray; conflict markers removed)
- `sellafield_bedrock` → CE `sellafield_slaked` (`TODO(CE:BlockSellafieldSlaked.java:51-56)` same class)
- `skeleton_holder` → CE `dirt_dead` (`TODO(CE:ModBlocks.java:519)` `cubeAll("dirt_dead")`)
- `rbmk_display_blank` → CE `blocks/rbmk/rbmk_display` (`TODO(CE:RBMKMiniPanelBase.java:145)`)

Turret **world** cubes skipped: CE has no cube png for chekhov/howard/… Inventory already CE `block_steel`.
TESR machine world cubes skipped: inventory already wired from `textures/models/*`.

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

### Blocks still TESR / no cube (~46)

World leftover = mats above + TESR machines (inventory may already be wired) + nuke casings without a CE **block** png + turrets (no CE cube) + `balls_spawner*` `barrel` `pile_device_*`.

#### Item leftover ids (18)

`americium241_block` `americium242_block` `americiumrg_block` `balls_spawner` `balls_spawner_spent` `barrel` `carbon_block` `counterfeit_keys` `dyatlov` `gear_bronze` `gear_steel` `ghiorsium336_block` `jetpack_glider` `lock` `neodymium_block` `plutonium241_block` `technetium99_block` `wiring_tool`

#### Block leftover ids (46)

mats above + `balls_spawner` `balls_spawner_spent` `barrel` + TESR machines (`machine_assembly_machine` `machine_chemical_plant` `machine_combustion_engine` `machine_crystallizer` `machine_electrolyser` `machine_fraction_tower` `machine_gascent` `machine_icf_reactor` `machine_industrial_turbine` `machine_large_turbine` `machine_minirtg` `machine_mixer` `machine_powerrtg` `machine_silex` `machine_solar_boiler` `machine_steam_engine` `machine_turbine_gas` `machine_watz_reactor` `machine_wood_burner`) + nuke casings (`nuke_balefire` `nuke_custom` `nuke_fleija` `nuke_gadget` `nuke_man` `nuke_tsar`) + `pile_device_1` `pile_device_2` + turrets (`turret_chekhov` `turret_friendly` `turret_fritz` `turret_howard` `turret_jeremy` `turret_maxwell` `turret_richard` `turret_tauon`)
