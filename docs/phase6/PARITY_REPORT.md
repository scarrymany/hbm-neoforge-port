# Parity report — CE vs. this port

Phase 6 (`sy1-parity-report`). Answers PORT_SPEC.md §Phase 6's first deliverable: "dump CE
registries... diff against 1.21.1 registries; target >=99% by count with an explicit, justified
exclusion list." Covers the 8 categories PORT_SPEC.md names: items, blocks, fluids, entities, sounds,
vanilla-crafting recipes, machine recipes, advancements.

## 0. Verification status — read this first

**Nothing in this document is compile-verified or live-verified.** This sandbox cannot run
`./gradlew` (the build's own dependency resolution hits `maven.neoforged.net`/`maven.blamejared.com`,
both returning HTTP 403 from this session's egress proxy — a network-policy denial, confirmed at task
start) and cannot launch a Minecraft client/server or a live CE 1.12.2 dev instance to dump its
registries programmatically, so the "dump CE registries... diff" instruction in PORT_SPEC.md's literal
form (a runtime JSON dump + automated diff) is **not possible in this environment**. Every number below
instead comes from **static reading of source** — CE's real Java under `upstream/hbm-ce/src/main/java`
and its resource JSON, and this port's Java under `src/main/java/com/hbm` and its resource JSON —
plus small Python scripts written for this task that mechanically count field declarations, method
calls, and JSON files. This is the same static-reading methodology this project has used for every
prior phase's STATUS.md and for this same wave's `ca3` recipe-graph/reachability audit
(`docs/phase6/recipe_graph_audit.md`), reused here rather than re-derived from scratch wherever `ca3`
already did the counting work for a category this report also needs (items, blocks).

Two source notes specific to this task:

- This task's prompt expected to build on a `ca1` "CE registry census" and `ca2` "this-port registry
  census" from the same Stage 1 wave. **Neither was found on disk** — `docs/phase6/` contained only
  `ca3`'s recipe-graph audit, and no `ca1`/`ca2` scratch files, task-output JSON, or code comments were
  located anywhere under this session's scratchpad or task-output directories (searched explicitly).
  Every count in this report was therefore **re-derived directly from source** by this task, following
  the same methodology PORT_SPEC.md and the sibling tasks' own prompts describe, except where `ca3`'s
  already-published item/block counts could be reused directly (cited by section below).
- Every count carries the same two-tier confidence `ca3` established: **exact** where a loop bound is a
  literal integer or a directly-read enum's constant count, and a **regex/call-site-count
  approximation** everywhere else (field-declaration counts, `.register(`/`.put(`/`.add(` call-site
  tallies). Approximations are flagged inline; treat every non-exact figure as ±5-10%, not a precise
  count. Nothing here should be read as more precise than the confidence tier states.

## 1. Top-line summary

**Overall registry-count parity: ≈54% (count-weighted), ≈68% (category-average) — both far below
PORT_SPEC.md's ≥99% target.** The gap is not close, is not attributable to a small justified-exclusion
list, and is not evenly spread: two categories (advancements, sounds-as-registered-ids) are at or near
100%, three (fluids, items, entities) are in the 70-98% range, and the two largest-by-content-volume
categories — **vanilla-crafting recipes (≈13%) and machine recipes (≈8%)** — are the overwhelming
reason the weighted total is as low as it is. This matches what every prior phase's STATUS.md already
disclosed piecemeal (Phase 5 STATUS.md: "CE's real corpus is ~1,900+ recipes... this port now has
261"); this report is the first place those disclosures are rolled into one count-based percentage.

**A more important caveat than the count percentage itself:** count-based registry parity (this report)
and *functional* parity (can a player actually obtain/use the content) are different questions with
very different answers here. `ca3`'s companion recipe-graph audit (`docs/phase6/recipe_graph_audit.md`,
same wave) found that of this port's ~2,982 registered items, only **≈12.4% are reachable by any
in-game path today** (crafting, machine recipe, loot, world-gen, or advancement reward) — the other
≈87.6% are registered but currently unobtainable in survival, mostly because the machine- and
crafting-recipe corpora this report also finds thin are the *same* recipes that would make those items
reachable. **The count percentages in this report and the ≈12.4% reachability figure are not
in tension — they are measuring the same underlying shortfall from two different angles**, and a
reader should weight the reachability number more heavily than any of this report's per-category
percentages when judging whether the mod is actually playable end-to-end.

## 2. Per-category table

| Category | CE count | This port | % (port/CE) | Confidence |
|---|---:|---:|---:|---|
| Items (registered Item ids, hand + block-derived) | ≈3,154 | 2,982 | **94.5%** | Approx both sides — see §3.1 |
| Blocks (registered Block ids) | ≈1,165 | 642 | **55.1%** | Approx both sides — see §3.2 |
| Fluids (registered FluidType entries) | 162 | ≈158 | **97.5%** | Near-exact both sides — see §3.3 |
| Entities (registered entity types) | ≈159 | 117 | **73.6%** | Approx both sides — see §3.4 |
| Sounds (registered SoundEvent ids) | 381 | 379 | **99.5%**\* | Near-exact both sides — see §3.5 |
| Vanilla-crafting recipes | ≈1,900-2,000 | ≈260 | **≈13.3%** | Approx CE, near-exact port — see §3.6 |
| Machine recipes | ≈1,718 | 138 | **≈8.0%** | Rough CE (regex), near-exact port — see §3.7 |
| Advancements | 65 | 65 | **100%** | Exact both sides — see §3.8 |
| **Weighted total** (Σport / ΣCE) | **8,754** | **4,741** | **≈54.2%** | — |
| **Category average** (unweighted) | — | — | **≈67.7%** | — |

\* Sounds: 99.5% is the **registered-SoundEvent-id** percentage only. The actual playable audio layer
(the `.ogg` files and `sounds.json` entries those ids point to) is a *separate, much worse* number —
see §3.5. This is the single most misleading-if-read-alone cell in this table.

**Both headline percentages are far below the ≥99% target.** The weighted figure (≈54.2%, summing raw
counts across all 8 categories then dividing) is the more decision-relevant of the two because it
reflects each category's real share of CE's total registered surface — the two recipe categories alone
account for 3,668 of CE's ≈8,754 counted entries (42%) and are the two worst-performing categories, so
they pull the weighted average down hard. The unweighted category-average (≈67.7%) treats a 65-entry
category (advancements) as equally important as a ~2,000-entry one (vanilla recipes), which is a less
meaningful summary of "how much of CE's total content surface exists here" but is included because
PORT_SPEC.md's target is stated as a single, category-agnostic bar and a reader may want the simpler
number too. Neither reading clears 99%, or comes remotely close.

## 3. Methodology and evidence, per category

### 3.1 Items

**This port: 2,982** (2,340 hand items + 642 block-derived `BlockItem`s), taken directly from `ca3`'s
recipe-graph audit (`docs/phase6/recipe_graph_audit.md` §1/§0), which computed this with a documented
exact/approximate split (exact for ~14 loop-governed files whose enum/array bound was read directly,
regex-extraction spot-checked for the remaining ~50) — reused here rather than re-derived, per this
task's own instruction to prefer `ca3`'s work where available.

**CE: ≈3,154.** `ModItems.java`'s `public static final Item...`-typed field count is **1,993**
(script-counted: regex over field declarations, excluding 9 `ToolMaterial` and 2 non-`Item` helper
fields; cross-checked with a looser regex that gave 2,003 — within the ±5-10% approximation band). All
of these are constructed via `new ItemXyz(...)`, and `ItemBase`'s constructor (and ~30 other item base
classes') calls `ModItems.ALL_ITEMS.add(this)`, so `ALL_ITEMS.size()` at runtime should equal this
field count (confirmed no other file in the codebase constructs these classes outside `ModItems.java`
for registration purposes). Block-derived items: CE's `ModItems.registerItems()` walks
`ModBlocks.ALL_BLOCKS` and creates one `ItemBlock` per block **except** the 4 `BlockModDoor` instances
(explicitly skipped, 0 items) and the 31 classes implementing `ICustomBlockItem` (register their own
item(s) via a class-specific `registerItem()` override — assumed ≈1-per-block on average, not
individually re-verified for all 31 given this task's time budget). CE blocks ≈1,165 (§3.2) minus 4
minus a small, unquantified ICustomBlockItem adjustment ≈ **≈1,161** block-derived items. 1,993 + 1,161
= **≈3,154**.

**A methodology caveat that matters more than the raw percentage for this category specifically:** CE
is a Minecraft 1.12.2 mod and makes heavy use of the era's metadata-sub-item convention — one
registered `Item` object (`ItemEnumMulti<E>` or a hand-written equivalent) represents many in-game
variants via item damage/NBT, each with its own name/texture/recipe role. This port's 1.21.1 convention
(matching every other 1.21 mod) instead registers one distinct `Item` id per variant — the same
convention `ca3`'s audit calls out by name ("13 `EnumAchievementType` ordinals flattened... per this
port's metadata-to-id convention"). This task found **24 direct `ItemEnumMulti<>` fields in CE's
`ModItems.java` alone**, backed by enums ranging from 2 members (`EnumFuelAdditive`) to 73
(`EnumSecretType`) — a spot-check summing the 21 of those 24 enums this task could size directly
totals **≈346 variants represented by those 24 CE `Item` objects alone** — plus **27 more classes**
elsewhere in CE that extend `ItemEnumMulti` (`ItemAmmo`, `ItemHoloTape`, `ItemBatterySC`,
`ItemBreedingRod`, etc.), not individually sized given this task's budget. **Net effect: CE's raw
"registered Item object" count of ≈3,154 structurally *understates* CE's true obtainable-item surface
by at least several hundred, likely more**, because a large minority of those 3,154 objects are
themselves multi-variant containers. This means the 94.5% figure in the table above is **not a
trustworthy signal that this port is near item parity with CE** — it is closer to an artifact of
comparing a 1.21-flattened count against a 1.12-collapsed one. `ca3`'s reachability finding (≈12.4% of
this port's 2,982 registered items are obtainable by any path) is the more reliable read on this
category's real state; see §1.

### 3.2 Blocks

**This port: 642.** Not independently re-derived by this report — reused from `ca3`'s "BlockItem half
of `ModBlocks.BLOCKS`" figure (`recipe_graph_audit.md` §1), on the confirmed basis (checked directly by
this task, see below) that this port's own block-registration convention pairs exactly one `BlockItem`
with every registered `Block` in a single helper call (`ModItems.ITEMS.register(name, () -> new
BlockItem(block.get(), ...))` sits inside the same `registerBlock(...)` helper as the `BLOCKS.register`
call, in all 26 files this task checked that call block registration) — so "BlockItem count" and "Block
count" are the same number in this port by construction, with no exceptions found.

**CE: ≈1,165.** `ModBlocks.java` has **1,141** `public static final Block`-typed field declarations
(script-counted, same method as §3.1), plus two loop-generated `Block[]` families read directly:
`concrete_colored_stairs` (16, one per `EnumDyeColor`) and `concrete_colored_ext_stairs` (8, one per
`BlockConcreteColoredExt.EnumConcreteType`, enum read directly). 1,141 + 16 + 8 = 1,165. Same
`ALL_BLOCKS`-via-constructor pattern as items (`BlockBase` and other base classes call
`ModBlocks.ALL_BLOCKS.add(this)`), same confidence tier as the item field count.

**55.1% is a real, large gap**, not primarily a counting-convention artifact the way items is — CE does
have some block-metadata families too (this port's own `GenericBlocks.java` already flattens at least
one, `concrete_colored_ext_stairs`'s 8-variant `EnumConcreteType`, into loop-registered separate port
blocks), but nowhere near at the scale that would explain a 45-point gap. This is consistent with §5's
acknowledged gaps: most of CE's ~1,165 blocks are content families this port has not yet reached
(decorative/structural block variants, the full material-storage-block family, most machine-casing
blocks that don't yet have a block entity behind them).

### 3.3 Fluids

**CE: 162.** `com/hbm/inventory/fluid/Fluids.java`, `public static final FluidType` field count,
script-counted directly (158 use `new FluidType(name, ...)` inline; the rest use a slightly different
constructor overload — not individually re-verified, low risk given the small remainder).

**This port: ≈158.** This port carries its own `com/hbm/inventory/fluid/Fluids.java` and
`FluidType.java` — **a near-verbatim structural port of CE's own custom fluid-type abstraction**,
rather than mapping onto NeoForge's native `Fluid`/`FluidType` registry (matching PORT_SPEC.md §2's
framing that CE's 100+ custom fluids mostly have no world block and don't need to be real NeoForge
fluids). Field count: 158 `public static FluidType` declarations (no `final`, since these are assigned
later rather than at declaration). Of those, 157 are directly assigned via `= new FluidType(...)`
(script-confirmed); the 158th (`ACID`) is declared but not matched by that exact assignment pattern —
likely assigned via a different call shape, not independently confirmed. This is the port's **healthiest
non-trivial category** — CE's fluid *definitions* (name, color, traits, temperature) appear to have been
carried over close to 1:1, which is a real, verifiable win worth calling out explicitly rather than
letting it get lost under the recipe-category headline numbers.

### 3.4 Entities

**CE: ≈159.** No central "ModEntities" registry file exists in CE 1.12.2 — CE uses a build-time
annotation/codegen system (`com.hbm.main.AutoRegistry` + a `GeneratedHBMRegistrar` class generated by
CE's `processor/` module, which is All-Rights-Reserved per PORT_SPEC.md §4.5 and was not read).
Counted instead via `grep` for `public class Entity...` declarations under `com/hbm/entity/` (159
concrete, i.e. non-`abstract`, matches out of 200 total `.java` files in that package tree; 23 further
files are `public abstract class Entity...` base classes, correctly excluded from a "registered type"
count). This is a naming-convention-based count (CE's own convention names essentially every entity
class with an `Entity` prefix) rather than a verified registration-call count, so treat as approximate.
One thing this task specifically checked and ruled out as a false alarm: Phase 4 STATUS.md's mention of
"the full 75-entity BOTPrime worm chain" does **not** mean 75 distinct registered entity *types* — CE's
`com/hbm/entity/mob/botprime/` package has only 4 real entity classes (`EntityBOTPrimeHead`,
`EntityBOTPrimeBody`, `EntityBOTPrimeBase`, `EntityWormBaseNT`); "75" refers to runtime body-segment
*instances* of the same few classes, not registered types, and this port's
`entity/mob/WormEntityTypes.java` registering 3 types for that family is consistent with CE's real
class count, not a gap.

**This port: 117.** Counted via literal `ENTITY_TYPES.register(` call-site count across
`src/main/java/com/hbm` (excluding the 106+2 separate `BLOCK_ENTITY_TYPES.register(` calls, a different
registry this report does not cover — those are block entities/tile entities, not PORT_SPEC.md's
"entities" category). This port's convention registers one entity type per literal `.register(` call
(no loop-based entity-type families found), so this count should be close to exact for this port's
side.

### 3.5 Sounds

**Registered-id comparison: CE 381, this port 379 (99.5%).** CE: `com/hbm/lib/HBMSoundHandler.java`,
381 `public static SoundEvent` field declarations — not `final`: CE declares each field bare
(`public static SoundEvent fel;`, line 14) and assigns it later via `register(...)` inside a
`public static void init()` method (line 412), not at the declaration site (an earlier draft of
this report said `final` here; corrected on review — the count of 381 itself was already right).
CE's `sounds.json` itself has 392 entries — a handful of CE sound *files* have no matching Java
`SoundEvent` field, or map many-to-one; not reconciled further. This port: `com/hbm/lib/HBMSoundHandler.java`, 379
`DeferredHolder<SoundEvent, SoundEvent>` field declarations — the class's own javadoc documents that
every CE sound-path string (mixed-case, invalid under 1.21's `ResourceLocation` rules) was mechanically
converted to lowercase snake_case, and explains 3 sounds' `GunConfiguration` wiring was dropped pending
that class's own port. 379/381 is a genuinely strong result for the **id registration** half of this
category.

**The other half of "sounds" is not close to parity, and the id percentage above should not be read as
representing it.** This port's `src/main/resources/assets/hbm/` has **zero** `.ogg` files and **no**
`sounds.json` at all (confirmed: `find ... -iname "*.ogg"` returns 0 hits; no `sounds` directory or
`sounds.json` exists under `assets/hbm`). Every one of the 379 registered `SoundEvent` ids currently
points at nothing playable — registering the id is necessary but not sufficient for a sound to actually
play in-game. This is a real, load-bearing, **0/392** gap on the asset side of this category, distinct
from (and much worse than) the 99.5% id-registration figure, and is part of the same "bulk asset
migration was never claimed by any Phase 5 area" gap Phase 5 STATUS.md already flagged for textures/
models (§5 below).

### 3.6 Vanilla-crafting recipes

**This port: ≈260** (this task's own count, cross-validated against Phase 5 STATUS.md's independently
published "261" from when that phase closed — the 1-recipe difference is noise-level, consistent with
either a minor edit since or a rounding difference in how a table row was counted). Computed directly
from `com/hbm/datagen/ModRecipeProvider.java`: 25 tool-family recipes (4 materials × 5 tool types via a
table-driven loop, read directly, + `dwarven_pickaxe` + 2 "super" recipes + 2 more) + 21 armor-family
recipes (named individually) + 214 mineral-conversion-cluster recipes, computed exactly from the file's
own tables (`BILLET_SETS` 40 rows × 4 recipes/row via `billetSet()`'s body = 160; `BILLET_NUGGET_ONLY` 2
× 2 = 4; `MINERAL_SETS` 6 × 2 = 12; `ONE_TO_NINE_PAIRS` 19 × 2 = 38; 160+4+12+38 = 214). 25+21+214 = 260.

**CE: ≈1,900-2,000.** Not independently re-derived to a precise figure by this task — this is the same
estimate `ca3`'s audit and Phase 5 STATUS.md both already converged on independently, each from reading
CE's real crafting-recipe source directly (`com/hbm/main/CraftingManager.java`, 1,602 lines, plus the 9
`com.hbm.crafting.*` handler classes, 2,085 combined lines). This task's own spot-check of
`CraftingManager.java` is consistent with that range without adding precision: 603 literal
`addRecipeAuto(` call sites + 218 `addShapelessAuto(` + 24 `addSlabStair(` (each likely emitting more
than one recipe) + 7 `add9To1(`/7 `add1To9(` (each inside one of 13 `for` loops iterating a material
table, so each literal call site represents many runtime recipes, not one) — a call-site tally alone
already exceeds 850 before accounting for any loop multiplication, which is consistent with, not
contradictory to, the ~1,900-2,000 estimate. This report uses the midpoint (1,950) for the percentage
column; the true figure could reasonably be anywhere in that range, giving **≈13.0-13.7%** rather than
one precise number — reported here as ≈13.3%, i.e. this port has roughly **one recipe for every seven
or eight CE has**.

### 3.7 Machine recipes

**This port: 138** — 87 JSON recipe files (`data/hbm/recipe/{shredder,assembler,breeder}`: 44+13+30)
plus 51 recipe registrations across the 9 bespoke Java recipe-data classes this port ported
(`CrystallizerRecipes` 2, `MixerRecipes` 1, `RefineryRecipes` 6, `CentrifugeRecipes` 20,
`ChemPlantRecipes` 4, `CyclotronRecipes` 1, `GasCentrifugeRecipes` 4, `SILEXRecipes` 7,
`ElectrolyserFluidRecipes` 6 — script-counted via `.put(`/`.register(`/`.add(` call sites per file, each
file small enough that call-site count ≈ recipe count with low risk of loop-multiplication error).

**CE: ≈1,718** (rough, regex-based — see caveat below). CE's `com/hbm/inventory/recipes/` package has
**72 recipe-data classes**, not 9 — this port has ported the data for only the 9 machine types named
above, plus shredder/assembler/breeder as JSON. The other ≈63 CE recipe classes cover entire machine
types this port has either not built yet or has built without any recipe data behind them yet:
`BlastFurnaceRecipes`/`BlastFurnaceRecipesNT`, `ArcFurnaceRecipes`, `ArcWelderRecipes`, `CokerRecipes`,
`CombinationRecipes`, `CompressorRecipes`, `CrackingRecipes`, `CrucibleRecipes` (the Crucible itself
isn't ported at all — `ca3`'s audit's root cause #1), `CustomMachineRecipes`, `DFCRecipes`,
`ElectrolyserMetalRecipes`, `EngineRecipes`, `ExposureChamberRecipes`, `FluidBreederRecipes`,
`FluidCombustionRecipes`, `FractionRecipes`, `FusionRecipesLegacy`, `HeatRecipes`,
`HydrotreatingRecipes`, `LemegetonRecipes`, `LiquefactionRecipes`, `MagicRecipes`,
`NuclearTransmutationRecipes`, `OutgasserRecipes`, `PUREXRecipes`, `ParticleAcceleratorRecipes`,
`PedestalRecipes`, `PlasmaForgeRecipes`, `PrecAssRecipes`, `PressRecipes`, `PyroOvenRecipes`,
`RBMKFuelRecipes`, `RadiolysisRecipes`, `ReformingRecipes`, `RockMillRecipes`, `RotaryFurnaceRecipes`,
`SolderingRecipes`, `SolidificationRecipes`, `StorageDrumRecipes`, `SuperComputerRecipes`,
`WasteDrumRecipes`, `AnvilRecipes` (241 entries alone — the single largest CE recipe class),
`AmmoPressRecipes` (92), `AnnihilatorRecipes`, plus the `anvil/` and `loader/` sub-packages. A script
summing `.put(`/`.register(`/`.add(` call sites across all 72 classes (excluding one 0-entry file) gives
**1,718** — this is a **rougher approximation than most other categories' CE-side figures**: it is a
single-hop, non-loop-corrected call-site count (the same class of caveat `ca3`'s audit applied to its
own machine-recipe extraction) so it can both overcount (an unrelated `.add(`/`.put(` call swept in by
the regex) and undercount (a call site inside a `for` loop iterating a material table, counted once
per source line rather than once per runtime iteration, the same pattern already confirmed for
`CraftingManager.java` in §3.6). Treat 1,718 as an order-of-magnitude anchor, not a precise count — the
true figure is plausibly anywhere from ~1,400 to ~2,500 given that uncertainty, which would move the
8.0% result to somewhere in a **≈5.5-10%** band. Under any reading in that band the conclusion is the
same: **this port's machine-recipe coverage is the weakest of all 8 categories**, driven by whole
machine types (not just individual recipes within an already-covered type) having zero recipe data
ported.

### 3.8 Advancements

**CE: 65, this port: 65 — 100%.** Both counted by literal `find ... -name "*.json"` under each repo's
`advancements`/`advancement` resource directory (CE: `assets/hbm/advancements`; this port:
`data/hbm/advancement`, the 1.21 datapack path convention). Exact count match on both sides. This report
does not re-verify that each of the 65 port-side advancement JSON files is *behaviorally* faithful to
its CE counterpart (correct triggers, correct parent/child tree, correct icon item) — only that the
file count matches; a behavioral spot-check was out of this task's scope and is a reasonable follow-up
for whoever picks up the "acknowledged gaps" list in §5.

## 4. Exclusion list

PORT_SPEC.md asks for an "explicit, justified exclusion list (<=1%: dead/deprecated content, 1.12-only
mod integrations like GregTech)." This task looked specifically for content in either of PORT_SPEC.md's
named exclusion shapes and found very little that qualifies — **consistent with this project's own
established pattern of not forcing gaps into a rationalized bucket.** The full list:

1. **CE's `weapon_mod_test` debug family (≈10 items, `WeaponModItems.java`).** Named in that CE class's
   own javadoc (per `ca3`'s audit, §2 exclusion list) as a debug-only test family. **Not ported by this
   port at all** — it does not appear in this port's registered-item census, so it is neither
   "reachable" nor a counted gap in `ca3`'s audit either. **Justified exclusion**: debug/test content
   with no player-facing purpose in CE itself. Scale: ≈10 items out of a ≈3,154-item CE census
   (<0.32%).
2. **CE's 13-item `AchievementIconItems` family + `battery_creative` + `rbmk_fuel_test` (≈15 items
   total).** CE's own source constructs these with `.setCreativeTab(null)` (confirmed by `ca3`'s audit,
   citing exact CE line numbers) — genuinely non-obtainable, decorative/debug-only items in CE itself.
   **These are already ported 1:1 by this port** (all 15 exist in the port's own item census), so they
   are **not a source of any count gap** — listed here only for completeness/transparency, since
   PORT_SPEC.md asks the exclusion list to be reviewed, and a reviewer should be able to see that this
   category was checked and found to be a non-issue on the count side.
3. **1.12-only mod integrations (PORT_SPEC.md's own named example, "GregTech"): checked, and found
   *not* to apply.** CE's `com/hbm/util/Compat.java` does reference GregTech, AE2, and OpenComputers by
   modid, but every reference this task found is **behavioral** (e.g. `tryRegisterHazmat("gregtech",
   "gt.armor.hazmat.radiation.head", ...)` — CE reacting to *foreign* mods' items by string id to grant
   them hazmat protection) — **CE does not register any of its own items/blocks/recipes specifically
   gated behind another 1.12-only mod being present**, as far as this task's read of `Compat.java` and a
   repo-wide `Loader.isModLoaded`/`@Optional` grep (116 hits, spot-checked) found. **This report cannot
   claim this exclusion category — it does not appear to exist in CE's real source, despite being
   PORT_SPEC.md's own illustrative example.** Stated explicitly rather than silently omitted, so a
   reviewer knows this was checked and not just assumed away.

**Total justified exclusions: ≈10 items, <0.2% of the combined 8-category CE count (8,754).** This is
comfortably inside PORT_SPEC.md's "<=1%" ceiling — but, per this task's brief, that is because there is
very little legitimate exclusion content to find, **not** because it was stretched to explain the ≈46
percentage points (weighted) or ≈32 percentage points (unweighted) of shortfall against the 99% target.
Essentially the entire gap belongs in §5 instead.

## 5. Acknowledged open gaps (the honest majority)

These are **not** exclusions — they are real, currently-unclosed parity gaps, each already substantially
documented elsewhere in this project's own history (cited per item below) rather than newly discovered
by this report. Ordered roughly by estimated impact on the weighted percentage:

1. **CE's Crucible/`MatDistribution` smelting-and-casting system is not ported at all.** Root cause #1
   in `ca3`'s recipe-graph audit — CE's `CrucibleRecipes.java` (24 entries, part of §3.7's 72-class
   count) has no ported equivalent, and this is the primary reason `MaterialItemGenerator` (188 items),
   `IngotNuggetItems` (184), `BilletPowderItems` (176), `PlateCrystalWasteItems` (107) and
   `MaterialBlockGenerator` (57 blocks) are mostly unreachable even where the items/blocks themselves
   are registered. Scale: the single largest lever on both the items and blocks percentages, and
   indirectly on machine recipes (Crucible's 24 CE recipes are 100% unported).
2. **CE's real crafting-recipe corpus is ~85-87% unported** (§3.6): `ModRecipeProvider` covers only
   `ToolRecipes`/`ArmorRecipes`/(part of) `MineralRecipes`; CE's `RodRecipes`, `WeaponRecipes`,
   `ConsumableRecipes`, `PowderRecipes`, `ExclusiveRecipes`, and the 7
   `com.hbm.crafting.handlers.*` dynamic-recipe classes are explicitly out of that class's own scope
   per its javadoc (cited by `ca3`). Scale: ≈1,650-1,750 recipes, the single largest raw-count gap in
   this entire report.
3. **CE's machine-recipe corpus is ported for only 9 of 72 recipe-data classes** (§3.7) — entire machine
   types (blast furnace, arc furnace/welder, coker, anvil (241 CE recipes alone), ammo press (92),
   PUREX, plasma forge, cracking/reforming/hydrotreating, super computer, and ~50 more) have zero
   recipe data in this port regardless of whether the machine block/block-entity itself exists yet.
   Scale: ≈1,500-1,600 recipes, the second-largest raw-count gap.
4. **Block↔ingot 3×3 compression grid is explicitly not attempted** (`ModRecipeProvider`'s own
   javadoc, cited by `ca3`) — blocks CE's material storage-block family (57 blocks) in both directions.
5. **No mob/entity loot table datagen exists** (`ca3`'s root cause #4) — zero
   `LootContextParamSets.ENTITY` references anywhere in this port, zero `EntityLootSubProvider`. Any CE
   item whose only acquisition path is a mob kill is currently unobtainable regardless of whether the
   mob entity itself is ported.
6. **No world-gen structures exist** (`ca3`'s root cause #5; also Phase 4 STATUS.md's own "world gen
   structures" scope note) — bunkers, radio stations, crashed vertibird, meteor dungeons: none of these
   are placed, so any CE item gated behind structure loot is unobtainable. (Ambient ore/meteorite
   *placement*, a different mechanism, is real and working — see §3.2/§3.4's healthier findings.)
7. **Sound assets (`.ogg` files + `sounds.json`) are 0% ported** (§3.5) despite SoundEvent *id*
   registration being ≈99.5% complete — every registered sound currently points at nothing playable.
8. **Bulk texture/model/asset migration is essentially unstarted**, already disclosed in Phase 5
   STATUS.md's own "Known gaps" section: "CE ships ~6,965 PNG/OBJ assets; this port now has a handful
   (2 overlay PNGs, 12 gun animation JSONs)... no Phase 5 area claimed ownership of the bulk copy." This
   is not one of PORT_SPEC.md's 8 named registry categories, so it has no row in §2's table, but it
   compounds every category above — a registered item/block/entity with no texture renders as a
   missing-texture checkerboard even where its registry entry and recipe are both complete.
9. **Entities: ≈26% of CE's ≈159 registered entity types have no port-side equivalent** (§3.4) — not
   independently root-caused per-entity by this report (that would duplicate Phase 3/4's own research
   reports); Phase 4 STATUS.md's "Known gaps" section already names several specific unowned families
   (the legacy artillery/rocket entity family, the Glyphid mob family, the FBI/FBI-drone raid family).
10. **The project does not currently compile end-to-end**, per Phase 5 STATUS.md's own disclosure ("22
    pre-existing dangling imports... means the project will not compile end-to-end today even with every
    Phase 5 gap above closed") and confirmed by this same wave's compile-triage commits already on this
    branch (`1747b7e`, in progress as of this report). This does not change any count in §2 (registry
    census counts source text, not compiled output), but it means **none of this report's percentages
    can currently be double-checked by an actual build** even if network egress were available — a
    second, independent reason (beyond the proxy block) that every number here is static-reading-only.

## 6. What would move the number

In descending order of estimated leverage on the weighted total (§1's ≈54.2%), consistent with `ca3`'s
own recommendations (repeated here for a reader who only reads this report, not both):

1. Port a minimal Crucible + `MatDistribution` — unblocks the single largest connected cluster of
   unreachable items/blocks (gap #1 above).
2. Extend `ModRecipeProvider` to CE's remaining crafting-recipe classes (gap #2) — the single largest
   raw recipe-count gap.
3. Port recipe data for CE's remaining ≈63 machine-recipe classes, prioritized by whichever machine
   block entities Phase 2 already built without recipe data behind them (gap #3).
4. Land the block↔ingot compression grid (gap #4) — small, mechanical, high count-per-effort once (1)
   exists.
5. A minimal `EntityLootSubProvider` (gap #5) and the sound/texture asset copy (gaps #7/#8) — lower
   count-per-effort than 1-4 but currently hard zeros, and (for sounds/textures specifically) blockers
   for *verifying* everything else visually/audibly once a client can finally be launched.

## 7. Companion documents

- `docs/phase6/recipe_graph_audit.md` (`ca3`, same wave) — item-level reachability audit; the ≈12.4%
  figure cited in §1 comes from there. Read together with this report, not instead of it: this report
  answers "how much of CE's registry surface exists here by count," `ca3` answers "of what exists, how
  much can a player actually reach."
- `docs/phase0/STATUS.md` through `docs/phase5/STATUS.md` — each phase's own honestly-reported scope
  and gaps; every "already-documented" citation in §3/§5 above points back to one of these.
