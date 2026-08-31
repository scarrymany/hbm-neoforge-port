# Phase 9 entity census (CE `@AutoRegister` vs port)

CE named entity `@AutoRegister` (excluding `TileEntity*` / `tileentity_*`): **168**.
Port: **189** (spawn eggs + `entity_cloud_solinium` rename). **Missing list is empty**
(Phase 11 census `scripts/phase11_parity_census.py`).

Leftover projectile / soyuz / waypoint set is registered via `Phase9TailEntityTypes` and
**spawn-wired** this session (not just class stubs).

## Named leftovers (CE file:line) — registered + wired

| CE id | CE cite | Port + spawn site |
|---|---|---|
| `entity_acid_bomb` | `entity/projectile/EntityAcidBomb.java:11` | `EntityAcidBomb` — `EntityGlyphidBombardier.tick` (CE bombardier `onUpdate` 45-105); Blaster inherits + CE bomb overrides |
| `entity_chemthrower_splash` | `entity/projectile/EntityChemical.java:43` | `EntityChemical` (playable subset) — `EntityGlyphidBehemoth.acidAttack` (CE 93-101); unused-offset ctor CE:66 |
| `entity_shrapnel` | `entity/projectile/EntityShrapnel.java:23` | `EntityShrapnel` — `ExplosionLarge.spawnShrapnels` / tracers / shower (CE 73-192) |
| `entity_soyuz` | `entity/missile/EntitySoyuz.java:30` | `EntitySoyuz` — pad `LAUNCHING` is TBI **in CE**; `LaunchpadSoyuzBlockEntity` now accepts flattened `ItemSoyuz` skins |
| `entity_soyuz_capsule` | `entity/missile/EntitySoyuzCapsule.java:17` | `EntitySoyuzCapsule` (drops items; no `soyuz_capsule` block) |
| `entity_waypoint` | `entity/logic/EntityWaypoint.java:22` | `EntityWaypoint` |

`GlyphidHive` is `com.hbm.world.feature.GlyphidHive` (structure), **not** an entity.

## Thrown / logic tails (`EntityThrownTail` / `EntityLogicTail`)

CE ids registered with fallback renderer (hurt+discard or no-clip marker):

`entity_sawblade`, `entity_rainbow`, `entity_mini_nuke`, `entity_plasma_beam`,
`entity_laser_beam`, `entity_laser`, `entity_zirnox_debris`, `entity_fire`,
`entity_rocket`, `entity_rbmk_debris`, `entity_mini_mirv`, `entity_schrab`,
`entity_torpedo`, `entity_miner_beam`, `entity_ntm_siege_laser`, `entity_ln2`,
`entity_spark_beam`, `entity_mod_beam`, `entity_railgun_pellet`, `entity_bullet`,
`entity_duchessgambit`, `entity_building`, `entity_explosive_beam`, `entity_aa_shell`,
`entity_zeta`, `entity_artillery_rocket`, `entity_bullet_mk2`, `entity_boxcar`,
`entity_burning_foeq`, `entity_artillery_shell`, `entity_combine_ball`, `entity_cog`,
`entity_vortex_beam`, `entity_discharge`, `entity_bullet_mk3`,
`entity_bobmazon`, `entity_selena`, `entity_miner_rocket`, `entity_c_package`,
`entity_item_waste`, `entity_firework_ball`, `entity_item_buoyant`,
`entity_minecart_test`, `entity_waste_pearl`, `entity_spear`,
`entity_mod_fx_shadow`, `entity_fleija_rainbow`, `entity_emp`.

## Intentional CE id drift

- CE `entity_clound_solinium` (typo) → port `entity_cloud_solinium` (already registered).

## Not entities

- `tileentity_*` on blocks (`DungeonSpawner`, `BlockEmitter`, volcano core, …) — block entities, not this pass.
