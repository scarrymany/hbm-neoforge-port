# Phase 11 parity census (live, 2026-08-31)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted,
recipe reachability ~12%) was taken **before** Phase 7 Crucible/recipes, Phase 8 blocks/structures,
Phase 9–10 entities/textures, and this session's machine-parts + assembler chunk. Do not quote Phase 6
as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg` + Mats autogen + plant/glyph/
bedrock loops; **not** lang keys). Recipe JSON counted from `src/main/resources` + `src/generated`.
`./gradlew` verification is recorded in the PR after this file lands.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **57.6%** (4291 / 7453) |
| **Unweighted** (mean of category %) | **77.5%** |
| Recipe/loot reachability of port items | **41.5%** (746 / 1796) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. The single largest remaining hole is **machine recipes** (13.4%),
then vanilla crafting (42.2%) and blocks (55.5%). Items/fluids/sounds/advancements/entities are at
or above CE count.

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 1796 | **96.4%** | CE: `ModItems` field ctors. Port: Phase 10 extract (+ circuits/motors/coils/turbines this session) |
| Blocks | 1169 | 649 | **55.5%** | CE: `ModBlocks` fields + color-stair loops. Port: extract_all_ids + `ore()`/`stair`/`slab` |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/` (no tileentity). Port extras = spawn eggs + `entity_cloud_solinium` vs CE typo `entity_clound_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 822 | **42.2%** | CE: `addRecipeAuto` 1332 + shapeless 464 + slab/stair 25 + 9or1 43, estimate kept at 1950. Port: 370 shaped + 323 shapeless + 123 smelting + 6 hbm crafting serializers |
| Machine recipes | ~1695 | 227 | **13.4%** | CE: regex over recipe classes + ~300 pack/unpack allowance. Port: 110 assembler JSON + 44 shredder + 30 breeder + Java `RECIPES.add` (chemplant/pyro/silex/ammo/magic) |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`. New machine-part items this session all
have CE pngs.

## What changed since Phase 6 (do not mix the numbers)

Phase 6 used a larger item denominator (~3154/2982, closer to lang-key union) and counted recipes
before assembler/chemplant/crucible waves. Live Phase 11 uses the same Java-id extract as the
texture leftover census. Reachability jumped ~12% → **41.5%** because (a) the item denominator is
honest, (b) loot + vanilla crafts + this session's assembler JSON actually output those ids.

## This session's gap chunk (machine parts + assembler)

Largest remaining hole after leftover entities: **assembler/machine reachability**.

Registered (CE `ModItems.java` 396 / 1230 / 1280 / 1298–1341 / 1847 / 1871–1873 / 2520):

- Flattened `circuit_*` (all `EnumCircuitType`)
- `motor` / `motor_desh` / `motor_bismuth`
- `coil_copper` / `_torus` / `coil_tungsten` / `coil_gold` / `_torus` / `coil_magnetized_tungsten`
- `centrifuge_element` / `thermo_element` / `rtg_unit` / `drill_titanium`
- `canister_empty` / `turbine_titanium` / `turbine_tungsten` / `magnetron` / `crt_display`
- `sphere_steel` / `flywheel_beryllium` / `reactor_core`
- `hazmat_cloth` (+ red/grey) / `asbestos_cloth` / `filter_coal`

Vanilla crafts: coil/motor family (`CraftingManager.java:205-221`).

Assembler JSON: every CE `AssemblyMachineRecipes` `GenericRecipe` whose **inputs and output already
exist** in this port (110 files). Skipped, not invented:

- missing machine **blocks** (purex, fracker, excavator, radar, …)
- `item_expensive` / `part_generic` (unregistered)
- fluid-gated recipes (`AssemblerRecipe` is items-only)
- ore-dict shapes with no port item (`U238.billet`, `ANY_TAR`, …)

Unlocks now craftable on already-registered machines: shredder, assembler, chemplant, centrifuge,
gascent, acidizer, electrolyzer, RTG, diesel, combustion engine, industrial turbine, gas turbine,
SILEX, refinery, plus circuit chips / plates / PWR parts / bomb parts that resolved.

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block; CE double slabs are not mirrored)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure (`com.hbm.world.feature.GlyphidHive`), not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself** — no invented launch spawn
- Projectile tails = fallback renderer (hurt+discard / marker)
- Assembler recipes whose output block/item is unregistered — not emitted
- Texture misses with no CE file — documented, no invented art

## Recipe-graph reachability (cheap)

746 / 1796 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. This is **not**
a full survival walk from dirt: it is “has any JSON path.” Machine Java recipes (chemplant, crucible)
are only partly included (JSON-backed ones + loot). Treat 41.5% as a **ceiling-ish lower bound** on
“registered but dead” items, not a playthrough completion %.

## Next single gap (not this session)

Still machine recipes: remaining assembler sections need missing **blocks** (oil/gas, radar, doors)
or `item_expensive` flatten, plus chemplant/crucible JSON coverage vs CE call-site volume. Do not
emit recipes for unregistered machines.

## Entities (Phase 9 leftovers)

All six leftover classes were already registered at HEAD `51fc030b`. This session **spawn-wired** them
and confirmed no other CE `@AutoRegister` entity is missing. Cites: `docs/phase9/ENTITY_CENSUS.md`.
