# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,526,731** B (~63.45 MB), `./gradlew runServer` **Done (5.525s)** on a
wiped world (2417 recipes). Spawn 2% → 51% → Done. No duplicate ids, no leftover `tag:` JSON parse
errors. `hbm:oil_bubble` still logs `setBlock in a far chunk` (no deadlock).

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **65.0%** (5046 / 7767) |
| **Unweighted** (mean of category %) | **82.3%** |
| Recipe/loot reachability of port items | **41.4%** (806 / 1949) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (38.0%, was 33.2%),
then vanilla crafting (42.2%) and blocks (61.1%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1949 | **104.6%** | Phase 10 extract + `parts`/`parts1` + flatten extras |
| Blocks | 1169 | 714 | **61.1%** | extract + helpers + leftover doors / oil-chain cubes / ICF+DFC casings |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 823 | **42.2%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 763 | **38.0%** | CE denom unchanged. Port: 182 assembler + 92 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (gate items + leftover assembler + ICF/DFC)

Previous published snapshot: weighted **62.8%** / unweighted **81.1%**, machine recipes **666 / ~2009
(33.2%)**, blocks **680 / 1169 (58.2%)**, items **1910 / 1863 (102.5%)**, reachability **785 / 1910
(41.1%)**.

### Machine recipes: 666 → 763 (33.2% → 38.0%)

CE denom stayed **2009**. Port +97 (assembler JSON + Java tables). No census-method cheat.

JSON: assembler **150 → 182** leftover doors / oil-chain / warheads / cyclotron (`AssemblyMachineRecipes.java:189-214`
doors, `:281-309` oil, `:459` cyclotron, `:811-819` warheads). Slugs lowercase. Existing assembler
JSON not overwritten (generator circuit-count flatten is lossy). Tags emit as
`{"item":{"tag":"hbm:any_plastic"}}` — first boot dropped 6 files that used `"item":"tag:hbm:…"`.
Shredder 92, breeder 30 unchanged. Machine JSON total 304.

Java:

| Class | Now | Notes |
|---|---:|---|
| ArcWelderRecipes | 42 | CE `ArcWelderRecipes.java:59-65` neutron_reflector; `:166-215` thrusters/tanks; `:217-364` 18 missiles. 5 satellites still blocked (`sat_base` / `sat_head_*`) |
| SolderingRecipes | 26 | CE complete (no 528/LBSM). `upgrade_template` family `:192-282` (MINGRADE.dust → `powder_red_copper`). Glowstone = `Items.GLOWSTONE_DUST`. + 5+5 first/second upgrades `:284-329` |
| PlasmaForgeRecipes | 31 | CE `PlasmaForgeRecipes.java:113-237` ICF laser flatten + component metas 0/1/3 + DFC five. Skipped fusionvessel / schrabhammer / fensusan / gerald. `icf_controller` → existing `machine_icf_controller` |
| AmmoPressRecipes | 60 | 55 generated + 5 hand (`flame_diesel`/`gas`/`balefire`, `tau_uranium`, `coil_tungsten` — CE `:936+`/`:1024`/`:1038`). Did not re-add a second `coil_ferrouranium` (already generated) |
| others | ~304 | chem/solidifier/PA/PUREX/liquefaction/centrifuge/… unchanged |

### Blocks: 680 → 714 (58.2% → 61.1%)

Casings only (no new TE) unless noted. Cubes reuse `block_steel`.

- Doors — CE `AssemblyMachineRecipes.java:189-214`: `sliding_blast_door_legacy`, `large_vehicle_door`,
  `water_door`, `qe_containment`, `qe_sliding_door`, `round_airlock_door`, `secure_access_door`,
  `sliding_seal_door`, `cargo_door`, `silo_hatch`, `silo_hatch_large`, `transition_seal`
- Oil leftovers — CE `:281-309`: `machine_flare`, `machine_catalytic_cracker`, `machine_coker`,
  `machine_vacuum_distill`, `machine_catalytic_reformer`, `machine_hydrotreater`, `machine_radiolysis`
- ICF/DFC — CE `ModBlocks.java:1331-1348` / `PlasmaForgeRecipes.java:113-237` / `EnumICFPart`
  CASING/PORT/CELL/EMITTER/CAPACITOR/TURBO: `icf_laser_component_*`, `icf_component_0/1/3`,
  `struct_icf_core`, `dfc_{core,emitter,receiver,injector,stabilizer}`

ICF TE already existed. Full laser/DFC multiblock later. Extra machine casings with existing TEs
already had blocks — nothing cheap left there.

### Items that unblocked recipes

CE `ModItems.java:1842` `upgrade_template`; `:1861` `neutron_reflector`; `:2461` / `:2492-2535`
missile parts (`missile_assembly`, thruster/tank s/m/l, 12 conventional warheads + nuclear/mirv/volcano).

Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second `coil_tungsten`.

Reachability **785/1910 → 806/1949 (41.4%)**. Item count rose because `parts`/`parts1` now hit
`extract_all_ids` plus the new BlockItems.

No invented art (machine cubes reuse `block_steel`).

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure, not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself**
- Projectile tails = fallback renderer
- Assembler expensive-mode `inputItemsEx` legs — dropped (same as prior assembler ports)
- PA recipe `#10` SBD.ingot() — no schrabidate INGOT autogen
- Texture misses with no CE file — documented, no invented art
- ElectrolyserMetal → foundry. Not stubbed.
- PUREX chance-output / ICF / vitrification / naquadria — missing I/O
- Full Albion beam physics — detector runs the recipe table locally
- AmmoPress fluid-slot recipes (FLAME_*) — stored, not consumed (no tank on the press)
- ArcWelder 5 satellites (`sat_base` / `sat_head_*`) — items missing
- PlasmaForge fusionvessel / schrabhammer / fensusan / gerald — items missing
- Assembler `machine_precass` — still missing
- Circuit counts on some leftover assembler rows still flatten to ×1 unless hand-fixed

## Recipe-graph reachability (cheap)

806 / 1949 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 41.4% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes: leftover assembler (`machine_precass` / more skipped 180-ish), ArcWelder
satellites, PlasmaForge late-game tools, vanilla crafting 42.2%. Blocks 61.1%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
