# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Verified this session: `compileJava` 0 errors, `./gradlew build`
SUCCESS, jar `hbm-0.0.1.jar` **66,753,140** B (~63.66 MB), `./gradlew runServer` **Done (6.816s)** on a
wiped world (2905 recipes, was 2642). Spawn 2% → 51% → Done. No recipe parse errors.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **74.4%** (5781 / 7767) |
| **Unweighted** (mean of category %) | **87.2%** |
| Recipe/loot reachability of port items | **45.4%** (919 / 2026) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **well below ~90%**. Largest remaining hole is still **machine recipes** (52.8%),
then vanilla crafting (57.9%) and blocks (65.7%).

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2026 | **108.7%** | Phase 10 extract + `parts`/`parts1` + flatten extras |
| Blocks | 1169 | 768 | **65.7%** | extract + leftover assembler casings |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1129 | **57.9%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 1061 | **52.8%** | CE denom unchanged. Port: 356 assembler + 100 shredder + 30 breeder JSON + Java `RECIPES.add`/`RECIPES.put`/`registerSFAuto` |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this session

Previous published snapshot: weighted **69.7%** / unweighted **84.9%**, machine recipes **954 / ~2009
(47.5%)**, vanilla **866 / 1950 (44.4%)**, reachability **885 / 2026 (43.7%)**.

### Machine recipes: 954 → 1061 (47.5% → 52.8%)

CE denom stayed **2009**. Port +107. No census-method cheat. Assembler JSON **untouched** (356).

- **CentrifugeRecipes** 29 → 69 — leftover statics (lignite/Ti/quartz/W/SA326/rare/Th/Be/F/tikite/
  euphemium cluster/nether fire/Co/tektite/blaze/schraranium + crystals S/F/P/trixite/Li/starmetal)
  plus the 16-site bedrock loop. CE `CentrifugeRecipes.java:220-241` and leftover `:59/:89/:95/:101/:125/:131/:149/:155/:161/:173/:185/:191/:197/:203/:256/:258/:267-285`. `put()` skips AIR outputs (missing fragments don't invent items).
- **OutgasserRecipes** 0 → 20 — CE `OutgasserRecipes.java:35-60`. Shared RBMK outgasser / fusion-breeder table. Lithium block is `lithium_block` (Mats), not CE `block_lithium`. `billet_th232` (not a `BILLET_THORIUM` holder). RBMK outgasser BE rewired 1→2 slots + `MenuProvider` / `RBMKOutgasserMenu` / screen (CE `TileEntityRBMKOutgasser`:150-187). No stub GUI.
- **LemegetonRecipes** 0 → 37 — CE `LemegetonRecipes.java:20-65`. `book_lemegeton.use()` opens `LemegetonMenu`. `ingot_latex` missing → that one row dropped at register via AIR filter.
- **SILEXRecipes** 19 → 29 — leftover 10 non-depleted waste rows. CE `SILEXRecipes.java:485,512,528,544,561,576,588,601,616,632`. Unblocked by `nuclear_waste_tiny`. Skip depleted (`dust_tiny` + `*_depleted`), pellet loop, `fluid_icon`.

Java now also: ChemPlant 72, AmmoPress 60, PUREX 51, Solidification 49, ArcWelder 47, PlasmaForge 35, Liquefaction 31, Soldering 26 (unchanged this pass).

### Vanilla crafting: 866 → 1129 (44.4% → 57.9%)

+263 `ce_craft` JSON (`scripts/phase11_wave5_crafts.py`). No overwrite of existing JSON.

- `CraftingManager.java:97-121` — `addSlabStair` ×23 families ×5
- `:111` / `:1222-1234` — `addSlabStairColConcrete` ×16 (silver stairs/slabs; parent `concrete_light_gray`)
- `:112` / `:1237-1242` — `addStairColorExt` ×8
- ExclusiveRecipes.java:22-23, ArmorRecipes.java:139-142 asbestos, Tool/Mineral/Weapon/Smelting leftovers, CraftingManager leftover swords/asphalt/depth/basalt/charges/spikes/fence/rbmk_absorber/rag/firebrick

### Assembler leftover skip still 7

`Fluids.X.getDict` (hpcondenser / himars TB ×2 / mpw10taint), `ass.nitra` ChanceOutput,
`ass.digimemer` (commented Mekanism in CE), `ass.50bmgbypass` (`black_diamond` is `ItemModHealth`).
Did **not** re-register `powder_sawdust`, `gem_tantalium`, or a second `coil_tungsten`.

Reachability **885/2026 → 919/2026 (45.4%)**.

No invented art. No new Dummyable machines this pass (combination/blast/rockmill/annihilator still absent).

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
- CombinationOven / BlastFurnace / RockMill / Annihilator — full Dummyable machines, not started
- SILEX depleted waste / RBMK pellet loop — items missing

## Recipe-graph reachability (cheap)

919 / 2026 port item ids appear as a recipe `result`/`output` or loot `item`/`name`. Not a survival
walk from dirt. Treat 45.4% as a ceiling-ish lower bound on “registered but dead.”

## Next single gap (not this session)

Still machine recipes (combination/blast/rockmill/annihilator + leftover getDict assembler),
vanilla crafting 57.9%. Blocks 65.7%. Weighted 74.4% ≠ 90%.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
