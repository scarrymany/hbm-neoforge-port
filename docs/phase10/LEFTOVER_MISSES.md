# Phase 10 leftover misses (strict texture/model)

<<<<<<< HEAD
Registry vs CE png/json. Copied **only** missing CE assets. No invented art.
Data-component / block-entity `register()` strings (`heat`, `gun_states`, `tileentity_cyber_crab`, …) are not items and are excluded.
=======
Census: **2565 items** / **938 blocks** (Java `register`/`reg` + Mats autogen; not lang keys).
After remaps: items **237 (9.2%)**, blocks **114 (12.2%)**.
>>>>>>> 44cd9dff (Close LEFTOVER_MISSES: mold/cart_ntm item models (CE assets exist).)

- Census: items **2575**, blocks **944**
- BEFORE (strict, this wave): items **129**, blocks **197**
- AFTER: items **61**, blocks **74**
- Fixed this wave: items **68**, blocks **123**

<<<<<<< HEAD
Strict playable = model/blockstate exists **and** every referenced texture file exists
(or is vanilla `minecraft:`). `builtin/entity` guns count as playable.
=======
- 31 — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- 1 — debug ammo, no CE inventory png
- 175 — no CE item png under any remap of existing files
- 30 — TESR/machine: no inventory png in CE
>>>>>>> 44cd9dff (Close LEFTOVER_MISSES: mold/cart_ntm item models (CE assets exist).)

## What this wave wired (CE files only)

<<<<<<< HEAD
- Dotted flatten: `coke_petroleum` → `coke.petroleum.png`, `stone_resource_asbestos` → `stone_resource.asbestos.png`
- Mats `{mat}_block` → CE `block_{ce_stem}` (`cmbsteel`→`block_combine_steel`, `workersalloy`→`block_desh`, `lanthanum`→`block_lanthanium`, isotopes `pu238`/`ra226`/…)
- Vanilla cubes CE already uses: `gold_block`, `emerald_block`
- Numbered CE variants: `xanax`→`xanax_2`, `polaroid`→`polaroid_1`, `glitch`→`glitch_1`
- Same-object 3D skins for **inventory**: `machine_silex`→`textures/models/machines/silex.png`, nukes→`models/bombs/{lilboy,gadget,…}`
- OBJ/TESR blockstates that already had a cube+png (`anvil_*`) rewritten onto the cube
- Stairs from the same CE stone (`lightstone.bricks` / `lightstone.tile`)
=======
- 26 — autogen/storage cube: CE has no cube png for this mat
- 66 — no CE block png under any remap of existing files
- 4 — TESR/deco: no cube png
- 18 — TESR/duct: no cube png
>>>>>>> 44cd9dff (Close LEFTOVER_MISSES: mold/cart_ntm item models (CE assets exist).)

## Why leftover (true CE-missing or TESR)

<<<<<<< HEAD
Do **not** invent purple-black replacements. TESR/OBJ machines keep world TESR;
inventory may already use the same-object `textures/models/*` skin.
=======
- `americium241_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `americium242_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `americiumrg_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `ammo_debug` — debug ammo, no CE inventory png
- `armor_charge` — no CE item png under any remap of existing files
- `armor_fuel` — no CE item png under any remap of existing files
- `balls_spawner` — no CE item png under any remap of existing files
- `balls_spawner_spent` — no CE item png under any remap of existing files
- `barrel` — no CE item png under any remap of existing files
- `block_meteor_ore_aluminium` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `block_meteor_ore_cobalt` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `block_meteor_ore_copper` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `block_meteor_ore_iron` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `block_meteor_ore_rareearth` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `blueprint_pool` — no CE item png under any remap of existing files
- `boltgun` — no CE item png under any remap of existing files
- `canteen_cooldown` — no CE item png under any remap of existing files
- `carbon_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `charge` — no CE item png under any remap of existing files
- `cmbsteel_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `coltan_x` — no CE item png under any remap of existing files
- `coltan_z` — no CE item png under any remap of existing files
- `concrete_ext_bronze` — no CE item png under any remap of existing files
- `concrete_ext_hazard` — no CE item png under any remap of existing files
- `concrete_ext_indigo` — no CE item png under any remap of existing files
- `concrete_ext_machine` — no CE item png under any remap of existing files
- `concrete_ext_machine_stripe` — no CE item png under any remap of existing files
- `concrete_ext_pink` — no CE item png under any remap of existing files
- `concrete_ext_purple` — no CE item png under any remap of existing files
- `concrete_ext_sand` — no CE item png under any remap of existing files
- `concrete_light_gray` — no CE item png under any remap of existing files
- `conveyor_wand` — no CE item png under any remap of existing files
- `conveyor_wand_double` — no CE item png under any remap of existing files
- `conveyor_wand_express` — no CE item png under any remap of existing files
- `conveyor_wand_triple` — no CE item png under any remap of existing files
- `counterfeit_keys` — no CE item png under any remap of existing files
- `crane_splitter` — no CE item png under any remap of existing files
- `crashed_bomb_balefire` — no CE item png under any remap of existing files
- `crashed_bomb_conventional` — no CE item png under any remap of existing files
- `crashed_bomb_nuke` — no CE item png under any remap of existing files
- `crashed_bomb_salted` — no CE item png under any remap of existing files
- `deco_computer_ibm_300pl` — no CE item png under any remap of existing files
- `deco_crt_blinking` — no CE item png under any remap of existing files
- `deco_crt_broken` — no CE item png under any remap of existing files
- `deco_crt_bsod` — no CE item png under any remap of existing files
- `deco_crt_clean` — no CE item png under any remap of existing files
- `deco_pipe_framed_green` — no CE item png under any remap of existing files
- `deco_pipe_framed_green_rusted` — no CE item png under any remap of existing files
- `deco_pipe_framed_marked` — no CE item png under any remap of existing files
- `deco_pipe_framed_red` — no CE item png under any remap of existing files
- `deco_pipe_framed_rusted` — no CE item png under any remap of existing files
- `deco_pipe_green` — no CE item png under any remap of existing files
- `deco_pipe_green_rusted` — no CE item png under any remap of existing files
- `deco_pipe_marked` — no CE item png under any remap of existing files
- `deco_pipe_quad` — no CE item png under any remap of existing files
- `deco_pipe_quad_green` — no CE item png under any remap of existing files
- `deco_pipe_quad_green_rusted` — no CE item png under any remap of existing files
- `deco_pipe_quad_marked` — no CE item png under any remap of existing files
- `deco_pipe_quad_red` — no CE item png under any remap of existing files
- `deco_pipe_quad_rusted` — no CE item png under any remap of existing files
- `deco_pipe_red` — no CE item png under any remap of existing files
- `deco_pipe_rim` — no CE item png under any remap of existing files
- `deco_pipe_rim_green` — no CE item png under any remap of existing files
- `deco_pipe_rim_green_rusted` — no CE item png under any remap of existing files
- `deco_pipe_rim_marked` — no CE item png under any remap of existing files
- `deco_pipe_rim_red` — no CE item png under any remap of existing files
- `deco_pipe_rim_rusted` — no CE item png under any remap of existing files
- `deco_pipe_rusted` — no CE item png under any remap of existing files
- `deco_toaster_iron` — no CE item png under any remap of existing files
- `deco_toaster_steel` — no CE item png under any remap of existing files
- `deco_toaster_wood` — no CE item png under any remap of existing files
- `drone_patrol_chunkloading` — no CE item png under any remap of existing files
- `drone_patrol_express` — no CE item png under any remap of existing files
- `drone_patrol_express_chunkloading` — no CE item png under any remap of existing files
- `durasteel_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `dyatlov` — no CE item png under any remap of existing files
- `emerald_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `fext_foam` — no CE item png under any remap of existing files
- `fext_sand` — no CE item png under any remap of existing files
- `fext_water` — no CE item png under any remap of existing files
- `flask_infusion` — no CE item png under any remap of existing files
- `fluid_amount` — no CE item png under any remap of existing files
- `fluid_id` — no CE item png under any remap of existing files
- `fluid_id_multi` — no CE item png under any remap of existing files
- `fluid_pressure` — no CE item png under any remap of existing files
- `fluid_siphon` — no CE item png under any remap of existing files
- `fmn` — no CE item png under any remap of existing files
- `fuel_rod_life` — no CE item png under any remap of existing files
- `gear_bronze` — no CE item png under any remap of existing files
- `gear_steel` — no CE item png under any remap of existing files
- `ghiorsium336_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `glitch` — no CE item png under any remap of existing files
- `gold_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `grenade_equipped` — no CE item png under any remap of existing files
- `grenade_loadout` — no CE item png under any remap of existing files
- `gun_autoshotgun` — no CE item png under any remap of existing files
- `gun_charge_thrower` — no CE item png under any remap of existing files
- `gun_debug` — no CE item png under any remap of existing files
- `gun_lockon_target` — no CE item png under any remap of existing files
- `gun_states` — no CE item png under any remap of existing files
- `heat` — no CE item png under any remap of existing files
- `icf_muon` — no CE item png under any remap of existing files
- `icf_type1` — no CE item png under any remap of existing files
- `icf_type2` — no CE item png under any remap of existing files
- `jetpack_fuel` — no CE item png under any remap of existing files
- `jetpack_glider` — no CE item png under any remap of existing files
- `jetpack_glider_tank` — no CE item png under any remap of existing files
- `lanthanum_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `launch_pad_large` — no CE item png under any remap of existing files
- `launch_pad_rusted` — no CE item png under any remap of existing files
- `leaves_layer` — no CE item png under any remap of existing files
- `lens_damage` — no CE item png under any remap of existing files
- `lightstone_bricks` — no CE item png under any remap of existing files
- `lightstone_bricks_chiseled` — no CE item png under any remap of existing files
- `lightstone_bricks_stairs` — no CE item png under any remap of existing files
- `lightstone_chiseled` — no CE item png under any remap of existing files
- `lightstone_tile` — no CE item png under any remap of existing files
- `lightstone_tile_stairs` — no CE item png under any remap of existing files
- `lightstone_unrefined` — no CE item png under any remap of existing files
- `lock` — no CE item png under any remap of existing files
- `lox_barrel` — no CE item png under any remap of existing files
- `machine_assembly_machine` — TESR/machine: no inventory png in CE
- `machine_chemical_plant` — TESR/machine: no inventory png in CE
- `machine_combustion_engine` — TESR/machine: no inventory png in CE
- `machine_crystallizer` — TESR/machine: no inventory png in CE
- `machine_cyclotron` — TESR/machine: no inventory png in CE
- `machine_electrolyser` — TESR/machine: no inventory png in CE
- `machine_fraction_tower` — TESR/machine: no inventory png in CE
- `machine_gascent` — TESR/machine: no inventory png in CE
- `machine_icf_controller` — TESR/machine: no inventory png in CE
- `machine_icf_reactor` — TESR/machine: no inventory png in CE
- `machine_industrial_turbine` — TESR/machine: no inventory png in CE
- `machine_large_turbine` — TESR/machine: no inventory png in CE
- `machine_mixer` — TESR/machine: no inventory png in CE
- `machine_silex` — TESR/machine: no inventory png in CE
- `machine_solar_boiler` — TESR/machine: no inventory png in CE
- `machine_steam_engine` — TESR/machine: no inventory png in CE
- `machine_turbine_gas` — TESR/machine: no inventory png in CE
- `machine_watz_reactor` — TESR/machine: no inventory png in CE
- `machine_wood_burner` — TESR/machine: no inventory png in CE
- `mag_states` — no CE item png under any remap of existing files
- `magnetizedtungsten_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `mold_id` — no CE item png under any remap of existing files
- `multi_detonator_pos` — no CE item png under any remap of existing files
- `neodymium_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `neptunium237_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `neutron_activation` — no CE item png under any remap of existing files
- `nuke_boy` — no CE item png under any remap of existing files
- `nuke_fleija` — no CE item png under any remap of existing files
- `nuke_gadget` — no CE item png under any remap of existing files
- `nuke_man` — no CE item png under any remap of existing files
- `nuke_mike` — no CE item png under any remap of existing files
- `nuke_prototype` — no CE item png under any remap of existing files
- `nuke_tsar` — no CE item png under any remap of existing files
- `pager_channel` — no CE item png under any remap of existing files
- `pile_rod_depletion` — no CE item png under any remap of existing files
- `pink_barrel` — no CE item png under any remap of existing files
- `plant_reeds` — no CE item png under any remap of existing files
- `platemetal_base` — no CE item png under any remap of existing files
- `platemetal_black` — no CE item png under any remap of existing files
- `platemetal_blue` — no CE item png under any remap of existing files
- `platemetal_cyan` — no CE item png under any remap of existing files
- `platemetal_green` — no CE item png under any remap of existing files
- `platemetal_light_blue` — no CE item png under any remap of existing files
- `platemetal_light_gray` — no CE item png under any remap of existing files
- `platemetal_lime` — no CE item png under any remap of existing files
- `platemetal_magenta` — no CE item png under any remap of existing files
- `platemetal_orange` — no CE item png under any remap of existing files
- `platemetal_pink` — no CE item png under any remap of existing files
- `platemetal_purple` — no CE item png under any remap of existing files
- `platemetal_red` — no CE item png under any remap of existing files
- `platemetal_white` — no CE item png under any remap of existing files
- `platemetal_yellow` — no CE item png under any remap of existing files
- `plutonium238_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `plutonium239_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `plutonium240_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `plutonium241_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `plutoniumrg_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `polaroid` — no CE item png under any remap of existing files
- `pole_top` — no CE item png under any remap of existing files
- `polonium210_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `radium226_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `rbmk_absorber` — TESR/machine: no inventory png in CE
- `rbmk_control_reasim` — TESR/machine: no inventory png in CE
- `rbmk_control_reasim_auto` — TESR/machine: no inventory png in CE
- `rbmk_heater` — TESR/machine: no inventory png in CE
- `rbmk_inlet` — TESR/machine: no inventory png in CE
- `rbmk_outlet` — TESR/machine: no inventory png in CE
- `rbmk_pellet_stage` — TESR/machine: no inventory png in CE
- `rbmk_rod_core_heat` — TESR/machine: no inventory png in CE
- `rbmk_rod_hull_heat` — TESR/machine: no inventory png in CE
- `rbmk_rod_xenon` — TESR/machine: no inventory png in CE
- `rbmk_rod_yield` — TESR/machine: no inventory png in CE
- `red_cable_box` — no CE item png under any remap of existing files
- `redphosphorus_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `rtg_pellet_depletion` — no CE item png under any remap of existing files
- `saltpeter_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `sat_freq` — no CE item png under any remap of existing files
- `sellafield` — no CE item png under any remap of existing files
- `sellafield_bedrock` — no CE item png under any remap of existing files
- `skeleton_holder` — no CE item png under any remap of existing files
- `stalactite_asbestos` — no CE item png under any remap of existing files
- `stalactite_sulfur` — no CE item png under any remap of existing files
- `stalagmite_asbestos` — no CE item png under any remap of existing files
- `stalagmite_sulfur` — no CE item png under any remap of existing files
- `statue_elb` — no CE item png under any remap of existing files
- `statue_elb_f` — no CE item png under any remap of existing files
- `statue_elb_g` — no CE item png under any remap of existing files
- `statue_elb_w` — no CE item png under any remap of existing files
- `stone_biome_desert` — no CE item png under any remap of existing files
- `stone_biome_woodland` — no CE item png under any remap of existing files
- `stone_resource_asbestos` — no CE item png under any remap of existing files
- `stone_resource_bauxite` — no CE item png under any remap of existing files
- `stone_resource_hematite` — no CE item png under any remap of existing files
- `stone_resource_limestone` — no CE item png under any remap of existing files
- `stone_resource_malachite` — no CE item png under any remap of existing files
- `stone_resource_sulfur` — no CE item png under any remap of existing files
- `tantalum_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `technetium99_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `thorium232_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `tileentity_cyber_crab` — no CE item png under any remap of existing files
- `tnt_ntm` — no CE item png under any remap of existing files
- `tool_charge` — no CE item png under any remap of existing files
- `tool_fuel` — no CE item png under any remap of existing files
- `turret_names` — no CE item png under any remap of existing files
- `waste_natural_uranium_hot` — no CE item png under any remap of existing files
- `waste_u233_hot` — no CE item png under any remap of existing files
- `waste_u235_hot` — no CE item png under any remap of existing files
- `watz_yield` — no CE item png under any remap of existing files
- `weapon_melee_equipped` — no CE item png under any remap of existing files
- `weapon_mod_lists` — no CE item png under any remap of existing files
- `wiring_tool` — no CE item png under any remap of existing files
- `workersalloy_block` — Mats BLOCK autogen: no CE cube/item png (CE used prefix block_<mat>)
- `wrapped_item` — no CE item png under any remap of existing files
- `x` — no CE item png under any remap of existing files
- `xanax` — no CE item png under any remap of existing files
- `zirnox_rod_life` — no CE item png under any remap of existing files
>>>>>>> 44cd9dff (Close LEFTOVER_MISSES: mold/cart_ntm item models (CE assets exist).)

### Items (61)

<<<<<<< HEAD
#### Mats BLOCK autogen — CE has no storage cube (`TODO(CE:Mats.java)`) (8)

- `americium241_block` — no `block_am241` / `block_americium` in CE
- `americium242_block`
- `americiumrg_block`
- `carbon_block` — `MAT_CARBON` autogen; CE has `block_graphite`, different mat
- `ghiorsium336_block`
- `neodymium_block`
- `plutonium241_block` — CE has `block_pu238/239/240`, no `pu241`
- `technetium99_block`

#### TESR / OBJ — CE item model points at a missing cube png (11)

- `deco_pipe_framed` — `TODO(CE:models/item/deco_pipe_framed.json)` parent `hbm:block/deco_pipe_framed` (no JSON cube)
- `filing_cabinet` — `TODO(CE:models/item/filing_cabinet.json)` `layer0=hbm:blocks/filing_cabinet` (png absent in CE)
- `launch_pad` — `TODO(CE:models/item/launch_pad.json)` `layer0=hbm:blocks/launch_pad` (png absent)
- `machine_minirtg` — `TODO(CE:models/block/machine_minirtg.json)` `all=hbm:blocks/machine_minirtg` (png absent)
- `machine_powerrtg` — same, `machine_powerrtg.png` absent
- `railing_bend` / `railing_normal` / `railing_end_*` (6) — parent CE block models, no cube png

#### TESR machines without a same-id `models/machines/*.png` (5)

- `machine_gascent`
- `machine_icf_reactor`
- `machine_large_turbine` — not the same as `industrial_turbine`
- `machine_turbine_gas`
- `machine_watz_reactor`

#### No CE inventory png at all (37)

- `ammo_debug` — debug ammo
- `balls_spawner` / `balls_spawner_spent`
- `barrel` — generic id; CE only has colored `barrel_*`
- `cart_ntm_crate` / `cart_ntm_destroyer` / `cart_ntm_ore` / `cart_ntm_powder` / `cart_ntm_semtex`
- `counterfeit_keys`
- `crashed_bomb_balefire` / `crashed_bomb_conventional` / `crashed_bomb_nuke` / `crashed_bomb_salted`
- `dyatlov` / `fmn` / `jetpack_glider` / `lock` / `wiring_tool`
- `fext_sand` / `fext_water`
- `gear_bronze` / `gear_steel`
- `launch_pad_large` / `launch_pad_rusted` — GUI only in CE
- `leaves_layer` / `plant_reeds`
- `mold` — CE has `mold_*.png` shapes, no generic `mold.png`
- `rbmk_inlet` / `rbmk_outlet` — no CE rbmk inlet/outlet png
- `red_cable_box`
- `sellafield_bedrock` — CE has `sellafield_0..4` / slaked, no bedrock stem
- `skeleton_holder`
- `statue_elb` / `statue_elb_f` / `statue_elb_g` / `statue_elb_w`

### Blocks (74)

World leftover = same TESR/missing-cube set as items, plus:

- TESR machines whose **inventory** is already wired (`machine_silex`, `machine_chemical_plant`, …) — world stays TESR (`TODO(CE:TESR)`), no invented cube
- Nuke casings without a CE **block** png (`nuke_gadget`/`fleija`/`man`/`tsar`/`custom`/`balefire`) — item uses `textures/models/bombs/*`; `nuke_boy`/`mike`/`prototype` have CE `blocks/{lilboy,ivymike,prototype}.png` and are playable
- Turrets (`turret_chekhov` …) — CE inventory icon is `block_steel` (`TODO(CE:models/item/turret_chekhov.json)`); world is TESR
- `deco_pipe` / `pile_device_1` / `pile_device_2` / `pole_satellite_receiver` / `nuke_custom`

#### Block leftover ids

- `americium241_block` `americium242_block` `americiumrg_block` `balls_spawner` `balls_spawner_spent` `barrel` `carbon_block` `crashed_bomb_*` `deco_pipe` `deco_pipe_framed` `filing_cabinet` `ghiorsium336_block` `launch_pad` `launch_pad_large` `launch_pad_rusted` `leaves_layer` `machine_assembly_machine` `machine_chemical_plant` `machine_combustion_engine` `machine_crystallizer` `machine_electrolyser` `machine_fraction_tower` `machine_gascent` `machine_icf_reactor` `machine_industrial_turbine` `machine_large_turbine` `machine_minirtg` `machine_mixer` `machine_powerrtg` `machine_silex` `machine_solar_boiler` `machine_steam_engine` `machine_turbine_gas` `machine_watz_reactor` `machine_wood_burner` `neodymium_block` `nuke_balefire` `nuke_custom` `nuke_fleija` `nuke_gadget` `nuke_man` `nuke_tsar` `pile_device_1` `pile_device_2` `plant_reeds` `plutonium241_block` `pole_satellite_receiver` `railing_*` `rbmk_inlet` `rbmk_outlet` `red_cable_box` `sellafield_bedrock` `skeleton_holder` `statue_elb*` `technetium99_block` `turret_*`
=======
- `americium241_block` — autogen/storage cube: CE has no cube png for this mat
- `americium242_block` — autogen/storage cube: CE has no cube png for this mat
- `americiumrg_block` — autogen/storage cube: CE has no cube png for this mat
- `balls_spawner` — no CE block png under any remap of existing files
- `balls_spawner_spent` — no CE block png under any remap of existing files
- `barrel` — no CE block png under any remap of existing files
- `carbon_block` — autogen/storage cube: CE has no cube png for this mat
- `cmbsteel_block` — autogen/storage cube: CE has no cube png for this mat
- `crashed_bomb_balefire` — TESR/deco: no cube png
- `crashed_bomb_conventional` — TESR/deco: no cube png
- `crashed_bomb_nuke` — TESR/deco: no cube png
- `crashed_bomb_salted` — TESR/deco: no cube png
- `durasteel_block` — autogen/storage cube: CE has no cube png for this mat
- `emerald_block` — autogen/storage cube: CE has no cube png for this mat
- `ghiorsium336_block` — autogen/storage cube: CE has no cube png for this mat
- `gold_block` — autogen/storage cube: CE has no cube png for this mat
- `lanthanum_block` — autogen/storage cube: CE has no cube png for this mat
- `launch_pad` — no CE block png under any remap of existing files
- `launch_pad_large` — no CE block png under any remap of existing files
- `launch_pad_rusted` — no CE block png under any remap of existing files
- `leaves_layer` — no CE block png under any remap of existing files
- `lightstone_bricks` — no CE block png under any remap of existing files
- `lightstone_bricks_chiseled` — no CE block png under any remap of existing files
- `lightstone_bricks_stairs` — no CE block png under any remap of existing files
- `lightstone_chiseled` — no CE block png under any remap of existing files
- `lightstone_tile` — no CE block png under any remap of existing files
- `lightstone_tile_stairs` — no CE block png under any remap of existing files
- `lightstone_unrefined` — no CE block png under any remap of existing files
- `machine_assembly_machine` — TESR/duct: no cube png
- `machine_chemical_plant` — TESR/duct: no cube png
- `machine_combustion_engine` — TESR/duct: no cube png
- `machine_crystallizer` — TESR/duct: no cube png
- `machine_cyclotron` — TESR/duct: no cube png
- `machine_electrolyser` — TESR/duct: no cube png
- `machine_gascent` — TESR/duct: no cube png
- `machine_icf_controller` — TESR/duct: no cube png
- `machine_icf_reactor` — TESR/duct: no cube png
- `machine_industrial_turbine` — TESR/duct: no cube png
- `machine_large_turbine` — TESR/duct: no cube png
- `machine_mixer` — TESR/duct: no cube png
- `machine_silex` — TESR/duct: no cube png
- `machine_solar_boiler` — TESR/duct: no cube png
- `machine_steam_engine` — TESR/duct: no cube png
- `machine_turbine_gas` — TESR/duct: no cube png
- `machine_watz_reactor` — TESR/duct: no cube png
- `machine_wood_burner` — TESR/duct: no cube png
- `magnetizedtungsten_block` — autogen/storage cube: CE has no cube png for this mat
- `neodymium_block` — autogen/storage cube: CE has no cube png for this mat
- `neptunium237_block` — autogen/storage cube: CE has no cube png for this mat
- `nuke_balefire` — no CE block png under any remap of existing files
- `pile_device_1` — no CE block png under any remap of existing files
- `pile_device_2` — no CE block png under any remap of existing files
- `plant_reeds` — no CE block png under any remap of existing files
- `plutonium238_block` — autogen/storage cube: CE has no cube png for this mat
- `plutonium239_block` — autogen/storage cube: CE has no cube png for this mat
- `plutonium240_block` — autogen/storage cube: CE has no cube png for this mat
- `plutonium241_block` — autogen/storage cube: CE has no cube png for this mat
- `plutoniumrg_block` — autogen/storage cube: CE has no cube png for this mat
- `polonium210_block` — autogen/storage cube: CE has no cube png for this mat
- `radium226_block` — autogen/storage cube: CE has no cube png for this mat
- `rbmk_absorber` — no CE block png under any remap of existing files
- `rbmk_blank` — no CE block png under any remap of existing files
- `rbmk_boiler` — no CE block png under any remap of existing files
- `rbmk_console` — no CE block png under any remap of existing files
- `rbmk_control` — no CE block png under any remap of existing files
- `rbmk_control_auto` — no CE block png under any remap of existing files
- `rbmk_control_mod` — no CE block png under any remap of existing files
- `rbmk_control_reasim` — no CE block png under any remap of existing files
- `rbmk_control_reasim_auto` — no CE block png under any remap of existing files
- `rbmk_cooler` — no CE block png under any remap of existing files
- `rbmk_heater` — no CE block png under any remap of existing files
- `rbmk_inlet` — no CE block png under any remap of existing files
- `rbmk_outgasser` — no CE block png under any remap of existing files
- `rbmk_outlet` — no CE block png under any remap of existing files
- `rbmk_reflector` — no CE block png under any remap of existing files
- `rbmk_rod` — no CE block png under any remap of existing files
- `rbmk_rod_mod` — no CE block png under any remap of existing files
- `rbmk_rod_reasim` — no CE block png under any remap of existing files
- `rbmk_rod_reasim_mod` — no CE block png under any remap of existing files
- `rbmk_storage` — no CE block png under any remap of existing files
- `red_cable_box` — no CE block png under any remap of existing files
- `redphosphorus_block` — autogen/storage cube: CE has no cube png for this mat
- `saltpeter_block` — autogen/storage cube: CE has no cube png for this mat
- `sellafield` — no CE block png under any remap of existing files
- `sellafield_bedrock` — no CE block png under any remap of existing files
- `skeleton_holder` — no CE block png under any remap of existing files
- `stalactite_asbestos` — no CE block png under any remap of existing files
- `stalactite_sulfur` — no CE block png under any remap of existing files
- `stalagmite_asbestos` — no CE block png under any remap of existing files
- `stalagmite_sulfur` — no CE block png under any remap of existing files
- `statue_elb` — no CE block png under any remap of existing files
- `statue_elb_f` — no CE block png under any remap of existing files
- `statue_elb_g` — no CE block png under any remap of existing files
- `statue_elb_w` — no CE block png under any remap of existing files
- `stone_resource_asbestos` — no CE block png under any remap of existing files
- `stone_resource_bauxite` — no CE block png under any remap of existing files
- `stone_resource_hematite` — no CE block png under any remap of existing files
- `stone_resource_limestone` — no CE block png under any remap of existing files
- `stone_resource_malachite` — no CE block png under any remap of existing files
- `stone_resource_sulfur` — no CE block png under any remap of existing files
- `tantalum_block` — autogen/storage cube: CE has no cube png for this mat
- `technetium99_block` — autogen/storage cube: CE has no cube png for this mat
- `thorium232_block` — autogen/storage cube: CE has no cube png for this mat
- `tileentity_cyber_crab` — no CE block png under any remap of existing files
- `tnt_ntm` — no CE block png under any remap of existing files
- `turret_chekhov` — no CE block png under any remap of existing files
- `turret_friendly` — no CE block png under any remap of existing files
- `turret_fritz` — no CE block png under any remap of existing files
- `turret_howard` — no CE block png under any remap of existing files
- `turret_jeremy` — no CE block png under any remap of existing files
- `turret_maxwell` — no CE block png under any remap of existing files
- `turret_richard` — no CE block png under any remap of existing files
- `turret_tauon` — no CE block png under any remap of existing files
- `workersalloy_block` — autogen/storage cube: CE has no cube png for this mat
>>>>>>> 44cd9dff (Close LEFTOVER_MISSES: mold/cart_ntm item models (CE assets exist).)
