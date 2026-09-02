# Phase 10 leftover misses (strict texture/model)

Registry vs CE png/json. Copied **only** missing CE assets. No invented art.
Data-component register() strings (`heat`, `gun_states`, …) are not items and are excluded.

- Census: items **2575**, blocks **944**
- BEFORE (strict, this wave): items **61**, blocks **74**
- AFTER: items **25**, blocks **49**
- Fixed this wave: items **36**, blocks **25**

## Why leftover (true CE-missing or TESR)

These ids have no CE inventory/block cube. Do **not** invent purple-black replacements.
TESR/OBJ machines keep world TESR; inventory may use the same-object `textures/models/*` skin when CE has one.

### Items

#### CE has no inventory/block png for this id (25)

- `americium241_block`
- `americium242_block`
- `americiumrg_block`
- `ammo_debug`
- `balls_spawner`
- `balls_spawner_spent`
- `barrel`
- `carbon_block`
- `concrete_light_gray`
- `counterfeit_keys`
- `dyatlov`
- `fext_sand`
- `fext_water`
- `fmn`
- `gear_bronze`
- `gear_steel`
- `ghiorsium336_block`
- `jetpack_glider`
- `lock`
- `neodymium_block`
- `plutonium241_block`
- `sellafield_bedrock`
- `skeleton_holder`
- `technetium99_block`
- `wiring_tool`

### Blocks

#### CE has no block png/blockstate cube for this id (49)

- `americium241_block`
- `americium242_block`
- `americiumrg_block`
- `balls_spawner`
- `balls_spawner_spent`
- `barrel`
- `carbon_block`
- `concrete_light_gray`
- `ghiorsium336_block`
- `machine_assembly_machine`
- `machine_chemical_plant`
- `machine_combustion_engine`
- `machine_crystallizer`
- `machine_electrolyser`
- `machine_fraction_tower`
- `machine_gascent`
- `machine_icf_reactor`
- `machine_industrial_turbine`
- `machine_large_turbine`
- `machine_minirtg`
- `machine_mixer`
- `machine_powerrtg`
- `machine_silex`
- `machine_solar_boiler`
- `machine_steam_engine`
- `machine_turbine_gas`
- `machine_watz_reactor`
- `machine_wood_burner`
- `neodymium_block`
- `nuke_balefire`
- `nuke_custom`
- `nuke_fleija`
- `nuke_gadget`
- `nuke_man`
- `nuke_tsar`
- `pile_device_1`
- `pile_device_2`
- `plutonium241_block`
- `sellafield_bedrock`
- `skeleton_holder`
- `technetium99_block`
- `turret_chekhov`
- `turret_friendly`
- `turret_fritz`
- `turret_howard`
- `turret_jeremy`
- `turret_maxwell`
- `turret_richard`
- `turret_tauon`

