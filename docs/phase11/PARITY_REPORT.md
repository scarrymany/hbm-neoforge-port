# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,637,534** B (~63.56 MB), `./gradlew runServer` **Done (5.727s)** on a
wiped world (2642 recipes, was 2584). Spawn 2% → 51% → Done. No duplicate ids, no leftover `tag:` JSON
parse errors.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **69.7%** (5411 / 7767) |
| **Unweighted** (mean of category %) | **84.9%** |
| Recipe/loot reachability of port items | **43.7%** (885 / 2026) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (47.5%, was 44.8%),
then vanilla crafting (44.4%) and blocks (65.7%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2026 | **108.7%** | Phase 10 extract + `parts`/`parts1` + flatten extras |
| Blocks | 1169 | 768 | **65.7%** | extract + leftover assembler casings |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 866 | **44.4%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 954 | **47.5%** | CE denom unchanged. Port: 356 assembler + 100 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session (leftover assembler skip 58 → 7)

Previous published snapshot: weighted **68.6%** / unweighted **84.3%**, machine recipes **901 / ~2009
(44.8%)**, blocks **759 / 1169 (64.9%)**, items **2009 / 1863 (107.8%)**, vanilla **861 / 1950 (44.2%)**,
reachability **873 / 2009 (43.5%)**.

### Machine recipes: 901 → 954 (44.8% → 47.5%)

CE denom stayed **2009**. Port +53 (51 assembler JSON + 2 shredder). No census-method cheat.

JSON: assembler **305 → 356**. Leftover skip dump **58 → 7** (`scripts/phase11_machine_parts.py`).
Gating casings/items registered first, then JSON. Existing assembler JSON **not overwritten**.
Slugs lowercase. Tags `{"item":{"tag":"hbm:…"}}`. `circuit, N` counts kept
(`gadget` `circuit_controller` **3**, `himarssmall` `circuit_basic` **6**).
`fluid_barrel_full` / `fluid_pack_full` / `canister_full` + `Fluids.X.getID()` stay items.
Shredder **98 → 100** (crate_iron / crate_steel). Breeder 30 unchanged. Machine JSON total 486.

Java (unchanged this pass): ArcWelder 47, Soldering 26, PlasmaForge 35, AmmoPress 60.

### Vanilla crafting: 861 → 866 (44.2% → 44.4%)

- CraftingManager.java:647 — `machine_satlinker` (≠ `machine_satlink`)
- PowderRecipes.java:32 / :64 / :67 / :69 — clay uncraft, charcoal/lead/calcium flux

### Blocks: 759 → 768 (64.9% → 65.7%)

Casings only (no new TE) in `Phase11CasingBlocks`. Cubes reuse `block_steel` when CE models
are missing.

- CE `ModBlocks.java:1076` — `machine_condenser_powered` (ass.hpcondenser still skipped: `Fluids.LUBRICANT.getDict`)
- `:803` — `machine_orbus`
- `:664` — `pile_brick`
- `:706` / `:711` — `nuke_solinium`, `nuke_fstbmb`
- `:821-822` — `turret_arty`, `turret_himars` (out of TurretBlocks TE scope)
- `:789` — `barrel_steel`
- `:958` — `machine_satlinker`

### Items that unblocked recipes

- CE `ModItems.java:1175` — `rod_quad_empty` (ass.protoreactor)
- `:1135` — `lithium` (`LI.ingot()`, not a second `ingot_lithium` autogen)
- `:2515-2519` — `fins_flat` / `fins_small_steel` / `fins_big_steel` / `fins_tri_steel` / `fins_quad_titanium`
- `:2521` — `pedestal_steel`
- `:2397-2398` — `solinium_igniter` / `solinium_propellant`
- `:417` — `fluid_pack_empty`
- `:2490` — `missile_soyuz_lander`
- `ItemAmmoHIMARS.RocketType` — `ammo_himars_{small,small_he,small_wp,small_tb,small_mini_nuke,small_lava,large,large_tb}`
- `ItemAmmoArty` meta 0/9/10/11 — `ammo_arty_{normal,chlorine,phosgene,mustard}`
- `:1360` — `cap_fritz`

Resolver: `block_cap_*` (already flattened in GenericBlocks), `pile_rod_mk2_*`,
`ANY_BISMOIDBRONZE`→`ingot_bismuth_bronze`, `GRAPHITE`/`TH232`/`P_WHITE`, KEY_BLUE/BROWN,
`missile_soyuz`→`missile_soyuz_normal`, `canister_full`→`canister_fuel`, battery_sc / casing flatten.

Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second `coil_tungsten`.

Reachability **873/2009 → 885/2026 (43.7%)**.

No invented art.

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
- PlasmaForge late-game recipes with 11–12 item stacks — counted, TE still 6 slots
- Assembler leftover skip **7**: `Fluids.X.getDict` ore stacks (hpcondenser / himars TB ×2 / mpw10taint),
  `ass.nitra` ChanceOutput, `ass.digimemer` (commented Mekanism in CE), `ass.50bmgbypass`
  (`black_diamond` is `ItemModHealth`, not a dummy)
- Older leftover assembler rows may still have circuit flatten ×1 (new files this pass are correct)

## Recipe-graph reachability (cheap)

885 / 2026 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 43.7% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes (chemplant / shredder / leftover getDict assembler), vanilla crafting 44.4%.
Blocks 65.7%. Weighted 69.7% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
