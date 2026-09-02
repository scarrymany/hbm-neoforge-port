# Phase 10 leftover misses (strict texture/model)

Registry vs CE png/json. Copied **only** missing CE assets. No invented art.
Data-component / block-entity `register()` strings (`heat`, `gun_states`, `tileentity_cyber_crab`, …) are not items and are excluded.

- Census: items **2575**, blocks **944**
- BEFORE (strict, this wave): items **129**, blocks **197**
- AFTER: items **61**, blocks **74**
- Fixed this wave: items **68**, blocks **123**

Strict playable = model/blockstate exists **and** every referenced texture file exists
(or is vanilla `minecraft:`). `builtin/entity` guns count as playable.

## What this wave wired (CE files only)

- Dotted flatten: `coke_petroleum` → `coke.petroleum.png`, `stone_resource_asbestos` → `stone_resource.asbestos.png`
- Mats `{mat}_block` → CE `block_{ce_stem}` (`cmbsteel`→`block_combine_steel`, `workersalloy`→`block_desh`, `lanthanum`→`block_lanthanium`, isotopes `pu238`/`ra226`/…)
- Vanilla cubes CE already uses: `gold_block`, `emerald_block`
- Numbered CE variants: `xanax`→`xanax_2`, `polaroid`→`polaroid_1`, `glitch`→`glitch_1`
- Same-object 3D skins for **inventory**: `machine_silex`→`textures/models/machines/silex.png`, nukes→`models/bombs/{lilboy,gadget,…}`
- OBJ/TESR blockstates that already had a cube+png (`anvil_*`) rewritten onto the cube
- Stairs from the same CE stone (`lightstone.bricks` / `lightstone.tile`)

## Why leftover (true CE-missing or TESR)

Do **not** invent purple-black replacements. TESR/OBJ machines keep world TESR;
inventory may already use the same-object `textures/models/*` skin.

### Items (61)

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
