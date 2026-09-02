# Phase 10 leftover misses (strict texture/model)

Registry vs CE png/json. Copied **only** missing CE assets. No invented art.
Data-component / block-entity `register()` strings are excluded.

- Census: items **2575**, blocks **944**
- Previous wave (accepted): items **129 → 61**, blocks **197 → 74**
- This wave BEFORE: items **61**, blocks **74**
- This wave AFTER: items **24**, blocks **48**
- Percents: items **2.4% → 0.93%**, blocks **7.8% → 5.1%**

Strict playable = model/blockstate exists **and** every referenced texture file exists
(or is vanilla `minecraft:`).

## This wave (CE files only)

- Railings → CE `pipe_side.png` (`TODO(CE:models/block/railing.mtl)` `map_Kd`)
- `deco_pipe*` → CE `pipe_top*` / `pipe_side*` / `pipe_frame` (replaced the accidental `deco_steel` cubes)
- Launch pads → CE `textures/models/launchpad/{silo,silo_rusted,pad}.png`
- Statues → CE `textures/models/misc/modelstatue.png`
- `rbmk_inlet`/`outlet` → CE `rbmk_steam_inlet`/`rbmk_steam_outlet` cubes
- `red_cable_box` → CE `red_cable_icon`
- `pole_satellite_receiver` → CE `deco_satellite_receiver`
- `filing_cabinet` → CE `models/file_cabinet_steel.png`
- `plant_reeds` → CE `reeds_top` cross; `leaves_layer` → CE `waste_leaves`
- `mold` → CE `mold_base.png`
- Carts → CE `cart.crate` / `cart.destroyer` plus layered `cart.{wood,steel}` + `cart_overlay.*`
- Crashed bombs → CE `models/bombs/dud_{nuke,balefire,conventional,salted}.png`
- TESR machine **inventory**: `centrifuge_gas`, `icf`, `turbine`, `turbinegas`, `watz`, `rtg_cell_flipped`, `rtg_polonium`

Turret **world** cubes skipped: CE has no cube png for chekhov/howard/… (inventory already CE `block_steel`).

## Still leftover — true CE-missing

Do **not** invent cubes.

### Mats BLOCK autogen — CE never shipped a storage cube (`TODO(CE:Mats.java)`) (8)

- `americium241_block` `americium242_block` `americiumrg_block`
- `carbon_block` — not `block_graphite`
- `ghiorsium336_block` `neodymium_block` `plutonium241_block` `technetium99_block`

### Items without a CE inventory png (16)

- `ammo_debug` — debug ammo, no `items/ammo_debug.png`
- `balls_spawner` / `balls_spawner_spent`
- `barrel` — generic id; CE only has colored `barrel_*`
- `counterfeit_keys` `dyatlov` `fmn` `lock`
- `fext_sand` / `fext_water`
- `gear_bronze` / `gear_steel` — CE only `gear_large.png`
- `jetpack_glider` — armor layer only, no inventory png (craft exists)
- `sellafield_bedrock` — CE has `sellafield_0..4` / slaked, no bedrock stem
- `skeleton_holder` — OBJ only
- `wiring_tool` — not `wiring_red_copper` (different item)

### Blocks still TESR / no cube (48)

World leftover = mats above + TESR machines (inventory may already be wired) + nuke casings without a CE **block** png + turrets (no CE cube) + `balls_spawner*` `barrel` `pile_device_*` `sellafield_bedrock` `skeleton_holder`.

#### Item leftover ids (24)

`americium241_block` `americium242_block` `americiumrg_block` `ammo_debug` `balls_spawner` `balls_spawner_spent` `barrel` `carbon_block` `counterfeit_keys` `dyatlov` `fext_sand` `fext_water` `fmn` `gear_bronze` `gear_steel` `ghiorsium336_block` `jetpack_glider` `lock` `neodymium_block` `plutonium241_block` `sellafield_bedrock` `skeleton_holder` `technetium99_block` `wiring_tool`

#### Block leftover ids (48)

`americium241_block` `americium242_block` `americiumrg_block` `balls_spawner` `balls_spawner_spent` `barrel` `carbon_block` `ghiorsium336_block` `machine_assembly_machine` `machine_chemical_plant` `machine_combustion_engine` `machine_crystallizer` `machine_electrolyser` `machine_fraction_tower` `machine_gascent` `machine_icf_reactor` `machine_industrial_turbine` `machine_large_turbine` `machine_minirtg` `machine_mixer` `machine_powerrtg` `machine_silex` `machine_solar_boiler` `machine_steam_engine` `machine_turbine_gas` `machine_watz_reactor` `machine_wood_burner` `neodymium_block` `nuke_balefire` `nuke_custom` `nuke_fleija` `nuke_gadget` `nuke_man` `nuke_tsar` `pile_device_1` `pile_device_2` `plutonium241_block` `sellafield_bedrock` `skeleton_holder` `technetium99_block` `turret_chekhov` `turret_friendly` `turret_fritz` `turret_howard` `turret_jeremy` `turret_maxwell` `turret_richard` `turret_tauon`
