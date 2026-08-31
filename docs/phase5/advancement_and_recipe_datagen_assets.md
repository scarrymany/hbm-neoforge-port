# Phase 5 Research: Advancement & Recipe Datagen/Assets

**Area:** `advancement_and_recipe_datagen_assets`
**Scope (per task brief):** (1) Research-only scoping of CE's real advancement JSON tree and the
1.12.2→1.21.1 datapack-advancement schema changes a future implement-wave needs, for
`com.hbm.main.AdvancementManager`'s ~65 currently-null fields. (2) Spot-check whether this port's
`src/main/java/com/hbm/datagen/` already produces every model/blockstate/loot-table/recipe JSON
PORT_SPEC.md promises, and characterize the scale of any gap found. **This report does not author
any advancement or recipe JSON itself** — that is explicitly out of scope per the task brief.

## Method / what was actually read

- `upstream/hbm-ce/src/main/java/com/hbm/main/AdvancementManager.java` (195 lines, full read)
- `upstream/hbm-ce/src/main/resources/assets/hbm/advancements/*.json` — all 65 files, most read in
  full; representative samples quoted below for every distinct trigger/predicate shape found
- `upstream/hbm-ce/src/main/java/com/hbm/datagen/AdvGen.java` (536 lines) and
  `upstream/hbm-ce/src/main/java/com/hbm/datagen/dsl/AdvancementDSL.java` (908 lines, partial read
  of `Templates`) — CE's own (disabled-by-default) Java→JSON advancement generator
- `upstream/hbm-ce/src/main/java/com/hbm/main/CraftingManager.java` (1,602 lines) +
  `upstream/hbm-ce/src/main/java/com/hbm/crafting/*.java` (9 files, 2,085 lines) +
  `upstream/hbm-ce/src/main/java/com/hbm/crafting/handlers/*.java` (7 files, 634 lines) — CE's full
  vanilla-crafting-recipe corpus
- `upstream/hbm-ce/src/main/resources/assets/hbm/lang/en_us.lang` (8,591 lines) — spot-checked for
  the advancement title/desc keys the JSON tree references
- This port's `src/main/java/com/hbm/main/AdvancementManager.java` (292 lines, full read),
  `src/main/java/com/hbm/datagen/ModDataGenerators.java` (107 lines, full read),
  `src/main/java/com/hbm/{items,blocks}/datagen/*.java` (all 4 provider classes, full read),
  `src/main/resources/data/hbm/recipe/**` (88 JSON files, enumerated + `type` field audited)
- `upstream/neo-edition/src/main/resources/data/hbmsntm/advancement/**` (root.json + representative
  recipe-unlock JSONs) and `upstream/neo-edition/src/main/java/com/hbm/items/{special,food}/*.java`
  (grepped for `CriteriaTriggers`) — used **only** to confirm real, compiling 1.21.1 datapack-JSON
  shapes and Java API surface, never for design/content, per this project's standing rule
- `docs/phase4/STATUS.md`, `docs/phase1/datagen_framework.md`, `docs/phase1/DIGEST_REMAINDER.md`,
  `docs/phase4/entities_bosses.md`, `docs/phase4/satellites_followup_and_loot_pools.md` for existing
  handoff context this report builds on
- Cross-checked against sibling Phase 5 reports already on disk (`lang_file_and_localization.md`,
  `boss_and_vehicle_entity_renderers.md`, `jei_integration.md`, `gui_screens_survey_weapons_storage_special.md`,
  `renderer_framework_and_obj_models.md`) to avoid duplicating their already-completed findings and to
  correctly attribute cross-cutting gaps (texture assets, lang) to their real owners

---

## Part 1: Advancements

### 1.1 Headline: the task's own framing is right, with one important correction

The task brief says CE's advancement tree lives at `assets/hbm/advancements/*.json` — **confirmed
exactly right**, and this is *not* a mistake on CE's part: in 1.12.2, the JSON advancement system
(new in 1.12) was still resource-pack-adjacent, loaded the same way recipes were (`assets/<modid>/`),
not datapack-adjacent — the `data/` folder convention did not exist until 1.13. So CE's own path is
correct 1.12.2 usage, not something to "fix" when reading it.

The **one correction**: the task brief's suggested target path, `data/hbm/advancement/*.json`
(singular `advancement`), is the *right* 1.21.1 path, but this is worth stating with hard evidence
rather than assumption, because Mojang renamed several datapack folders from plural to singular in a
1.21 pre-release (`recipes`→`recipe`, `advancements`→`advancement`, `loot_tables`→`loot_table`,
`structures`→`structure`, etc.) — a detail easy to get wrong by analogy with older tutorials. This is
**confirmed directly from a real, compiling NeoForge 1.21.1 mod's own checked-in resources**, not
assumed: `upstream/neo-edition/src/main/resources/data/hbmsntm/advancement/root.json` and
`.../recipe/*.json` and `.../loot_table/*.json` all use the singular form already. This port's own
`src/main/resources/data/hbm/recipe/` (see Part 2) already uses the singular form too, so the
convention is already established in this codebase — a future advancement-porting agent should create
`src/main/resources/data/hbm/advancement/*.json`, matching that existing precedent exactly.

### 1.2 Exact scale: 65 files, 65 Java fields, 65 `load()` calls — a clean 1:1 map

```
find .../hbm-ce/.../assets/hbm/advancements -name '*.json' | wc -l        → 65
grep -c 'public static Advancement '  AdvancementManager.java (CE)        → 65
grep -c 'public static AdvancementHolder ' AdvancementManager.java (port) → 65
grep -c '= load('                     AdvancementManager.java (CE)        → 65
```
The task's "~65" is exact, not approximate. Both CE's and this port's `AdvancementManager` classes
already declare all 65 fields by name (`achSacrifice` … `root`) and both already call `load(...)`
for each one — **the Java side of this class needs zero further work**; only the 65 JSON files (plus
their lang keys, see 1.6) are missing. The port's own class (`src/main/java/com/hbm/main/
AdvancementManager.java:1-292`) already documents this exact gap in its own javadoc and already
degrades to a safe no-op (crash-fix from a prior Phase 4 review pass, confirmed) rather than crashing
on startup — this was previously fixed, is not a new problem, and needs no further Java change to
receive the JSON once it exists.

### 1.3 Trigger/criteria shapes actually used — narrower than 65 files suggests

Across all 65 files there are only **3 distinct trigger ids** and a single-root, cleanly-parented
tree (only `root.json` itself has no `"parent"`; every other file's parent chain resolves back to
`hbm:root`):

| Trigger | Criteria count (of 76 total across 65 files) | Compiling 1.21.1 example found? |
|---|---|---|
| `minecraft:impossible` | 38 | **Not found** in Neo Edition. Well-established Mojang-mapping knowledge (vanilla's own `net.minecraft.advancements.critereon.ImpossibleTrigger`, used by several vanilla root advancements) — **flagged as unverified against a real jar or compiling example in this sandbox.** |
| `minecraft:inventory_changed` | 37 | **Confirmed real and compiling** — `upstream/neo-edition/.../data/hbmsntm/advancement/recipes/misc/egg_balefire.json:1-25` and 15 sibling files use it, and `.../advancement/root.json` uses `minecraft:tick` for its own root (a different but also-confirmed trigger, not used by CE). |
| `minecraft:consume_item` | 1 (only `achradium.json`) | **Confirmed real and compiling**, and its Java-side trigger API is confirmed too: `net.minecraft.advancements.CriteriaTriggers.CONSUME_ITEM.trigger(ServerPlayer, ItemStack)`, called from 4 real, compiling classes — `upstream/neo-edition/.../items/{special/CigaretteItem.java:49, food/DrinkItem.java:187, food/EnergyItem.java:35, food/ConserveItem.java:133}`. |

Files with more than one criterion (`achredballoons.json` has 8, in a single OR-group "obtain any of
these missiles"; `achbreeding.json`/`achconcrete.json`/`achschrab.json`/`achselenium.json` have 2
each) are all still `inventory_changed`-only internally — no file mixes trigger types.

### 1.4 The real 1.12.2→1.21.1 predicate-schema differences (with side-by-side proof)

Sampled directly, CE 1.12.2 (`achburnerpress.json`) vs. confirmed-compiling 1.21.1
(`upstream/neo-edition/.../advancement/recipes/misc/egg_balefire.json`):

| Field | CE 1.12.2 shape | 1.21.1 shape (confirmed via Neo Edition) |
|---|---|---|
| Display icon | `"icon": {"item": "hbm:machine_press"}` | `"icon": {"count": 1, "id": "hbmsntm:crashed_bomb_conventional"}` — key renamed `item`→`id` |
| Item predicate (criteria conditions) | `"items": [{"item": "hbm:machine_press"}]` | `"items": [{"items": "hbmsntm:egg_balefire_shard"}]` — key renamed `item`→`items` (the *inner* predicate key), and it accepts either a single id string or a tag/list |
| Metadata/subtype (`"data": N`) | 11 of 65 files use `"data": N` inside an `item`/`icon` object (1.12.2 damage-value subtype selector — see 1.5) | **No equivalent exists.** 1.21.1 has no metadata subtypes; every CE `data:N` variant is (by this port's own Phase 0-4 convention) a *separately registered item id*. |
| Fluid-bucket NBT icon | `achwatzboom.json`: `"icon": {"item": "forge:bucketfilled", "nbt": "{FluidName:\"mud_fluid\",Amount:1000}"}` | No `nbt`-string predicates exist in 1.21.1 at all (NBT-as-string predicates were removed in the 1.20.5 data-component rework). Must resolve to whatever concrete item id this port uses for "a bucket of mud" (or, more likely, drop the fluid-specificity and just reference a real registered item — see 1.5). |
| `sends_telemetry_event` | Not present in CE (1.12.2 predates it) | Present in the confirmed real Neo Edition `root.json` (`"sends_telemetry_event": true`) — new-in-1.21.1-era field, optional but real; a future porter should decide a value (or omit — defaults exist) rather than guess it's required. |
| `requirements` shape (AND-of-OR) | Identical shape confirmed both sides: `[["a","b"]]` = OR, `[["a"],["b"]]` = AND. No change needed. | Same. |
| `rewards.experience` / `rewards.recipes` | Both present in CE (`achsacrifice.json` grants 70 XP; `root.json` grants 7 XP) | Confirmed unchanged shape (not independently re-verified against Neo Edition, since Neo Edition's sampled files don't use `rewards.experience`, but this is standard vanilla `AdvancementRewards` schema, unchanged across the version range — low risk). |

### 1.5 The real content-authoring burden: resolving CE's item-metadata subtypes

11 of 65 files (`achc20_5`, `achslimeball`, `achsulfuric`, `achtob`, `achwatzboom`, `bobhidden`,
`digammafeel`, `digammakauaimoho`, `digammaknow`, `digammasee`, `digammaupontop`) use a `"data": N`
metadata-subtype icon or predicate. Two concrete examples, read in full:

- `achc20_5.json`: icon `{"item": "hbm:achievement_icon", "data": 9}`, predicate
  `{"item": "hbm:canned_conserve", "data": 5}` — CE's `ItemConserve.EnumFoodType.JIZZ` variant
  (confirmed cross-referenced against `AdvGen.java:75`, which builds the same achievement's icon via
  `new ItemStack(ModItems.canned_conserve, 1, ItemConserve.EnumFoodType.JIZZ.ordinal())`).
- `achtob.json`: icon `{"item": "hbm:fluid_icon", "data": 34}` — a specific `Fluids` enum entry
  rendered through CE's internal fluid-icon-item convention (confirmed cross-referenced against
  `AdvGen.java:41`: `new ItemStack(ModItems.fluid_icon, 1, Fluids.ASCHRAB.getID())`).

**This is real per-file lookup work for a future implementer, not mechanical find-replace**: each of
these 11 files' subtype index must be traced to (a) CE's `EnumAchievementType`/`Fluids`/food-enum
ordinal it represents, then (b) whatever concrete item id this port's own Phase 0-4 registries
already gave that specific variant (per this port's established "1.12 metadata subtype → separate
1.21.1 item id" convention, confirmed used throughout Phase 0-4). None of the 11 target items were
independently verified as already-registered in this port by this report — that check is left to the
implement-wave, but is expected to succeed given Phase 0-4's stated completeness.

### 1.6 CE's own advancement JSON generator exists — useful, but stale

`upstream/hbm-ce/src/main/java/com/hbm/datagen/AdvGen.java` (536 lines) is CE's own semi-automated
Java→JSON generator, built on `AdvancementDSL` (908 lines, a real templated JSON-builder with
`Templates.{impossible, obtainAnyItem, obtainAnyItemStack, killEntity, enterDimension, enterBlock, ...}`
factory methods — confirmed by direct read of `AdvancementDSL.java:173-300`). It is **disabled by
default** (`private static final boolean doGen = false;` — a dev-only "flip this flag, run the client
once, commit the resulting JSON" workflow, per its own comment).

This is a genuinely useful cross-reference for a future implement-wave: its `batch.add(...)` calls
name the exact `ModItems`/`Fluids`/`EnumAchievementType` Java constant behind 50 of the 65 files'
icons and predicates, which is far less error-prone to read than re-deriving intent from a bare item
id string. **But it is stale, confirmed by direct diff**: only 50 of the 65 real, checked-in JSON ids
are referenced by any `batch.add(...)` call (`achfiend`, `achfiend2`, `achomega12`, `achraddeath`,
`achradpoison`, `achsacrifice`, `achsomewounds`, `bosscreeper`, `bossmaskman`, `bossmeltdown`,
`bossufo`, `bossworm`, `horizonsbonus`, `horizonsend`, `horizonsstart`, `root` are absent from
`AdvGen.java` entirely — hand-maintained/legacy JSON never migrated into the generator), and the
generator itself references a dangling id, `"bobchemistry"`, with **no corresponding JSON file
anywhere in the real tree** — clear evidence the generator has drifted from what CE actually ships.
**Recommendation for the implement-wave: treat the 65 checked-in `assets/hbm/advancements/*.json`
files as the sole ground truth (per this project's own CE-is-truth rule), and use `AdvGen.java`/
`AdvancementDSL.java` only as a secondary cross-reference for the 50 files it does cover — never as a
primary source, and never assume it's exhaustive or current.**

### 1.7 Lang keys: a real, separate, already-scoped dependency

The advancement JSON files reference `"translate"` keys inconsistently — some use the un-prefixed
legacy form (`achievement.burnerPress`), others use a namespaced form
(`hbm.achievement.sacrifice`, `hbm.advancement.root`). Both forms genuinely exist side-by-side in
CE's real `en_us.lang` with identical text for at least the sampled pair (`achievement.sacrifice` /
`hbm.achievement.sacrifice`, lines 103-104 and 2245-2246) — **this is not a CE bug to "fix" during
porting; a future porter must copy whichever exact key string each specific JSON file uses**, not
normalize the two forms.

This port's `ModLanguageProvider` (`src/main/java/com/hbm/datagen/ModLanguageProvider.java:1-149`,
full read) currently emits **zero** `achievement.*`/`hbm.achievement.*`/`hbm.advancement.*` keys —
it only emits auto-title-cased item/block names and CE's death-message corpus. This exact gap is
**already independently scoped by the sibling report** `docs/phase5/lang_file_and_localization.md`
(its own table, line 177: `achievement.` category, 132 keys, 0% ported) — this report defers to that
one for the full lang-extraction plan rather than duplicating it, and simply flags the **dependency**:
porting the 65 advancement JSONs and porting their ~132 lang keys are two halves of one deliverable,
likely best done by the same implement-wave pass or in tight coordination, since an advancement JSON
referencing a missing lang key doesn't crash (vanilla falls back to showing the raw key string) but
silently looks broken in the advancement-screen UI.

### 1.8 Java-side wiring status: what already works vs. what's still a loose end

`grantAchievement`/`hasAdvancement` (`AdvancementManager.java:275-291`, port) are already fully built,
null-tolerant, and already have **9 real call sites wired** across earlier phases (confirmed by
grep, all pre-existing, none touched by this report):

| Field | Call site | Status once JSON lands |
|---|---|---|
| `bossWorm` | `entity/mob/EntityBOTPrimeHead.java:170` | Works immediately, zero further Java change |
| `bossMaskman` | `entity/mob/EntityMaskMan.java:173` | Works immediately |
| `bossMeltdown` | `entity/mob/EntityRADBeast.java:232` | Works immediately |
| `bossCreeper` | `entity/mob/EntityCreeperNuclear.java:200` | Works immediately |
| `bossUFO` | `entity/mob/EntityUFO.java:384` | Works immediately |
| `horizonsStart`, `horizonsEnd` | `saveddata/satellites/SatelliteHorizons.java:54,106` | Works immediately |
| `digammaSee`, `digammaFeel`, `digammaKnow` | `capability/HbmLivingProps.java:174-176` | Works immediately |

The other ~56 fields have no explicit `grantAchievement` call anywhere in this port today. Most of
these need **no Java call at all**: the 37 `inventory_changed` + 1 `consume_item` criteria (1.3
above) are vanilla-auto-firing triggers that grant themselves the moment a player's inventory (or
consumption) matches the JSON predicate, once the JSON exists — this is precisely why CE itself never
calls `grantAchievement` for e.g. `achburnerpress`/`achpotato`/`achslimeball` anywhere in its own
source (confirmed: none of those 3 field names appear as a `grantAchievement(...)` argument anywhere
in `upstream/hbm-ce` outside `AdvancementManager.java` itself). The 38 `impossible`-triggered fields
*do* each need an explicit `grantAchievement` call from wherever the equivalent CE gameplay moment
lives — most of those CE call sites are in game systems this port has not yet built/ported (verified
only for the 9 above; the remaining ~29 `impossible` fields' CE call sites were not individually
traced in this report — that mapping is real remaining work for the implement-wave, scoped but not
executed here).

**One concrete open loose end found**: `achradium`'s `consume_item` trigger needs
`CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack)` called from wherever this port's
`coffee_radium` item (registered via `com.hbm.items.food.ItemEnergy`, confirmed via grep) finishes
being drunk. This port has **zero** references to `CriteriaTriggers` anywhere today (confirmed by
repo-wide grep) — whether this fires "for free" via vanilla's stock `LivingEntity#eat()`/
`finishUsingItem` pipeline (if `ItemEnergy` uses the standard food/drink completion path) or needs an
explicit added call (as Neo Edition's own `DrinkItem`/`EnergyItem`/`ConserveItem` needed, all of which
override consumption behavior and each added the explicit call) was **not traced to a conclusion in
this report** — flagged as an open question for whoever ports this one file.

### 1.9 API-shape verification status (carried forward, not newly resolved)

The port's own `AdvancementManager.java` javadoc (lines 27-75) already documents the
`Advancement`→`AdvancementHolder`, `AdvancementManager.getAdvancement`→`ServerAdvancementManager.get`,
`getProgress`→`getOrStartProgress`, `getRemaningCriteria`→`getRemainingCriteria`,
`grantCriterion`→`award` renames as "well-established Mojang-mapping knowledge for 1.20.2+, not
independently verified against a real compiled jar." **This report re-confirms that status is
unchanged**: a repo-wide grep of `upstream/neo-edition` for `AdvancementHolder`,
`ServerAdvancementManager`, `PlayerAdvancements`, and `getAdvancements()` returns **zero hits** —
Neo Edition genuinely has no Java code touching the advancement API at all, only datapack JSON. This
remains the single largest unverified-API risk in this whole area (see Key Risks).

---

## Part 2: Datagen coverage audit

### 2.1 What's actually in `src/main/java/com/hbm/datagen/` today

Only 2 files live directly in that package (`ModDataGenerators.java`, 107 lines;
`ModLanguageProvider.java`, 149 lines); the other providers live in their owning packages
(`com.hbm.items.datagen.ModItemModelProvider`, `com.hbm.blocks.datagen.{ModBlockStateProvider,
ModBlockTagProvider, ModBlockLootTableProvider}`, `com.hbm.items.datagen.ModItemTagProvider`). All 7
providers are wired into the single `GatherDataEvent` subscriber
(`ModDataGenerators.gatherData`, confirmed, full read).

### 2.2 Model, blockstate, loot-table, and lang coverage: **structurally complete, not a gap**

This is the report's central correction to the task's own framing: the task asked to "spot-check...
does every Phase 0-4 registered block have a real blockstate+model provider entry, not just a
registration." **The answer is: this cannot be missing by construction**, because all four relevant
providers iterate the *live* `DeferredRegister` at datagen time rather than a hardcoded id list —
confirmed by direct read of each:

- `ModItemModelProvider.registerModels()` (`items/datagen/ModItemModelProvider.java:29-40`):
  `ModItems.ITEMS.getEntries().forEach(...)` → every item gets `basicItem(...)` by default, or its
  own `ICustomItemModelRegister.registerItemModel(...)` if it implements that opt-out interface.
- `ModBlockStateProvider.registerStatesAndModels()` (`blocks/datagen/ModBlockStateProvider.java:31-42`):
  same pattern over `ModBlocks.BLOCKS`, default `simpleBlockWithItem(cubeAll(...))`, opt-out via
  `ICustomBlockModelRegister`.
- `ModBlockLootTableProvider.generate()`/`getKnownBlocks()`
  (`blocks/datagen/ModBlockLootTableProvider.java:36-45`): same pattern, default `dropSelf(...)`.
- `ModLanguageProvider.addTranslations()` (`datagen/ModLanguageProvider.java:34-46`): same pattern,
  default a title-cased-from-registry-id fallback string.

**There is no possible "registered but has no model/blockstate/loot/lang JSON" state in this port's
current architecture** — every one of the ~32 `ModBlocks.BLOCKS.register*(...)` call sites and ~208
`ModItems.ITEMS.register*(...)` call sites (confirmed by grep; each call site can register more than
one item/block, e.g. an enum-driven loop, so these are lower bounds on real content count, not the
count itself) automatically gets *some* valid, non-crashing generated JSON the moment `runData` runs.
**This is good, deliberate design that Phase 1 already got right** (confirmed against
`docs/phase1/datagen_framework.md`'s own recommendation to introduce the opt-out-interface pattern
"even if no Phase 1 item/block uses them yet" — that recommendation was followed).

**What this exhaustiveness does *not* mean** — and this is the real substance behind PORT_SPEC's
promise, not a false all-clear:

- Only **5 block classes** and **2 item classes** in the entire port implement
  `ICustomBlockModelRegister`/`ICustomItemModelRegister` (confirmed by grep). Every other registered
  block/item — almost certainly the overwhelming majority of this mod's machine blocks, multiblock
  parts, and complex items — falls through to the generic cube-all block model / flat single-layer
  item icon default. That default is *valid, buildable JSON*, but it is very unlikely to be CE's real
  visual design for e.g. a reactor casing or a directional machine front. **This is not a datagen
  coverage bug — it is exactly the rendering-content work other Phase 5 areas
  (`renderer_framework_and_obj_models.md`, `armor_humanoidmodel_rendering.md`,
  `boss_and_vehicle_entity_renderers.md`, the GUI-screen surveys) are already independently scoped to
  do.** Framed correctly: the *pipe* is complete; almost none of the *content* flowing through it yet
  matches CE.
- Lang entries are a generic fallback, not CE's real names — already covered by
  `lang_file_and_localization.md` (see 1.7).
- **Zero PNG texture files exist anywhere in this port's `src/main/resources`** (`find ...
  -name '*.png' | wc -l` → `0`, vs. CE's real **6,965** PNGs under
  `assets/hbm/textures/**`) — independently confirmed by at least four other Phase 5 reports already
  on disk (`boss_and_vehicle_entity_renderers.md:61,503`, `gui_screens_survey_weapons_storage_special.md:42`,
  `jei_integration.md:74`, `lang_file_and_localization.md:59`). This report adds one more independent
  confirmation, **not a new finding**, but it matters for correctly scoping "is the datagen promise
  met": even a perfectly-generated `cubeAll` blockstate JSON will render as the missing-texture
  checkerboard in-game with zero texture files present. **No single Phase 5 area currently claims
  ownership of "bulk-migrate CE's 6,965 texture/model asset files into this port's resource tree"** —
  every report that notices the gap (including this one) treats it as someone else's problem. This is
  worth flagging to whoever synthesizes Phase 5's research wave as a possible ownership gap, not
  something this report can resolve on its own.

### 2.3 The real, large, honestly-scoped gap: vanilla crafting recipes

**This is the headline finding of Part 2.** CE registers essentially its entire vanilla-crafting-table
recipe corpus through **Java code**, not JSON, via `com.hbm.main.CraftingManager.init()`
(`CraftingManager.java:67-90`, confirmed), which dispatches to 8 sub-registrar classes:

| Class | Lines | Representative helper calls |
|---|---|---|
| `com.hbm.main.CraftingManager` (dispatcher + `addCrafting()`) | 1,602 | `GameRegistry.addShapedRecipe` ×2, `new ShapedOreRecipe`/`ShapelessOreRecipe` ×3 (small, hand-special-cased recipes) |
| `com.hbm.crafting.ToolRecipes` | (part of 2,085 combined) | `addSword/addPickaxe/addAxe/addShovel/addHoe` ×10 each, `CraftingManager.addRecipeAuto(...)` |
| `com.hbm.crafting.ArmorRecipes` | " | `addHelmet/addChest/addLegs/addBoots` ×7 each, `addArmor` ×5 |
| `com.hbm.crafting.WeaponRecipes` | " | `addRecipeAuto`-style calls |
| `com.hbm.crafting.MineralRecipes` | " | `addBillet` ×52, `addMineralSet` ×9 |
| `com.hbm.crafting.RodRecipes` | " | `addRBMKRod` ×34, `addBreedingRod` ×18, `addZIRNOXRod` ×13, `addPellet` ×14, plus billet/unload variants |
| `com.hbm.crafting.ConsumableRecipes` | " | `addRecipeAuto`/`addShapelessAuto`-style calls |
| `com.hbm.crafting.PowderRecipes` | " | `addRecipeAuto`/`addShapelessAuto`-style calls |
| `com.hbm.crafting.ExclusiveRecipes` | " | Special/one-off recipes |
| (8 files above, combined) | **2,085** | — |
| `com.hbm.crafting.handlers.*` (7 classes: `CargoShellCraftingHandler`, `ContainerUpgradeCraftingHandler`, `FluidDuctRetypeHandler`, `GrenadeCraftingHandler`, `MKUCraftingHandler`, `RBMKFuelCraftingHandler`, `ScrapsCraftingHandler`) | 634 | Custom dynamic `IRecipe` subclasses — NBT-aware/predicate-matching recipes, not representable as plain shaped/shapeless JSON at all |

Raw call-site counts across `CraftingManager.java` + the 8 `com.hbm.crafting/*.java` sub-registrars
(grep, `addX(` patterns):

```
1332  addRecipeAuto(          464  addShapelessAuto(        134  addSmelting(
  56  addRecipeAutoOreShapeless(    52  addBillet(               34  addRBMKRod(
  25  addSlabStair(                18  addBreedingRod(          14  addPellet(
  13  addZIRNOXRod(                10  addSword/Shovel/Pickaxe/Hoe/Axe (each)
   9  addMineralSet(                8  addBreedingRodUnload(     8  addBreedingRodLoad(
   7  addLegs/addHelmet/addChest (each)   6  addTool(              6  addBoots(
   5  addArmor(              ... (smaller convenience wrappers omitted)
```

Summing just the three largest buckets (`addRecipeAuto` + `addShapelessAuto` + `addSmelting`) alone
already totals **1,930** individual recipe registrations, before counting the dozens of smaller
specialized helpers layered on top. **CE's real vanilla-crafting-recipe corpus is on the order of
~1,900-2,000+ distinct recipes.** This report does not claim an exact final count (some convenience
wrappers like `addSword` internally call the counted helpers rather than adding to the total, and
this was not traced line-by-line) — the point is scale, not precision: it is roughly **two orders of
magnitude larger than the 65-file advancement gap**, and almost two orders of magnitude larger than
what this port currently ships.

**What this port has today, for comparison** (`src/main/resources/data/hbm/recipe/**`, 88 files
total, `type` field audited across all of them):

```
44  "type": "hbm:shredder"
30  "type": "hbm:breeder"
13  "type": "hbm:assembler"
 1  "type": "minecraft:crafting_shaped"
```

**Exactly one** vanilla crafting-table recipe exists anywhere in this port
(`src/main/resources/data/hbm/recipe/mech_key.json`) — and it's not part of a systematic pass; `git
log` attributes it to the Phase 4 content wave (`docs/phase4/entities_bosses.md`), added ad hoc
because a specific boss-key item needed *a* recipe to be reachable, not because recipe-porting has
started. **This port has zero classes anywhere resembling CE's `CraftingManager`/`ToolRecipes`/etc.,
and zero `ModRecipeProvider`/`extends net.minecraft.data.recipes.RecipeProvider` class of any kind**
(confirmed by repo-wide grep for both).

**Correction to the task's framing here too**: this is not a "spot-check reveals a gap" finding in
the sense of something overlooked — it was **explicitly and correctly scoped out from the very start
of the project**. `docs/phase1/datagen_framework.md` section 4.6 (written at Phase 1, before any
content existed to have recipes) already says: *"`ModRecipeProvider`... is confirmed real but is its
own large area (CE's crafting-recipe corpus) — out of scope for this report, only noted so
`ModDataGenerators`'s design... has a slot for it."* `ModDataGenerators.java`'s own class javadoc
(confirmed, full read) still says the same thing today: *"Slots deliberately left out because they
are out of this area's scope... a fluid tag provider..., a sound definitions provider..., and a
recipe provider (its own large content area)."* No Phase 2, 3, or 4 STATUS.md ever claimed this area
(confirmed by grep across all `docs/phase{1,2,3,4}/*.md`) — **this is a clean, consistently-named,
never-silently-dropped gap that has simply been waiting for its own dedicated pass since Phase 1**,
exactly like the advancement gap. It is real and it is large, but it is not a surprise.

### 2.4 What a future recipe-datagen pass can build on (already real, already compiling)

- `com.hbm.inventory.material.Mats`/`MaterialShapes`/`NTMMaterial` (confirmed real, `Mats.java` uses
  `Map<TagKey<Item>, MaterialStack>` internally) — a modern `TagKey<Item>`-based material/common-tag
  abstraction already exists and is exactly the kind of thing CE's `STEEL.ingot()`/`DURA.bolt()`
  ore-dictionary-shortcut style calls (seen throughout `ToolRecipes.java`) would need to map onto.
- `com.hbm.items.datagen.ModItemTagProvider` (confirmed, partial read) already derives
  `c:<tagFolder>/<material>`-style common item tags for every material×shape combination
  automatically at datagen time (same exhaustive-iteration pattern as 2.2) — directly reusable
  ingredient-tag infrastructure for a future recipe provider.
- Real, compiling custom `RecipeSerializer<>`/`RecipeType<>` implementations already exist in this
  port from earlier phases — `com.hbm.inventory.recipes.AssemblerRecipe`,
  `com.hbm.inventory.recipes.machine.BreederRecipe` (confirmed via grep) — proving out the general
  1.21.1 custom-recipe pattern a future implementer would need for the 7 dynamic
  `com.hbm.crafting.handlers.*` CE classes (2.3 table), even though those need the
  `CraftingRecipe`/`RecipeInput` (crafting-table) variant specifically, not the machine-recipe variant
  these two examples use.
- CE's `com.hbm.crafting.handlers.RBMKFuelCraftingHandler` should not be confused with this port's
  already-real `com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes` (confirmed both exist, are
  different things): the port's class is the RBMK reactor's own internal fuel-rod recipe registry
  (already built, Phase 2 scope), while CE's `handlers` class is a *crafting-table* recipe for
  assembling a fresh fuel rod from raw materials — the latter is still unbuilt.

---

## Safe to build now vs. blocked

**Safe to build now** (all prerequisite systems — item/block registries, material/tag
infrastructure, the `AdvancementManager`/`GatherDataEvent` plumbing — are already complete per
Phase 0-4's own STATUS.md and this report's own reading):

1. Writing all 65 `data/hbm/advancement/*.json` files, 1:1 against CE's real tree, using this
   report's schema-diff table (1.4) and the 11-file metadata-subtype resolution list (1.5) as a
   checklist. Zero Java changes required for the 9 already-wired boss/satellite/radiation fields;
   zero Java changes required for the 38 `inventory_changed`/`consume_item`-triggered fields either
   (they self-fire from vanilla once the JSON exists), *except* possibly `achradium`'s
   `CriteriaTriggers.CONSUME_ITEM` call (1.8, unresolved — small, isolated).
2. A `ModRecipeProvider` (or hand-authored JSON, since this port already mixes both approaches for
   its 87 existing machine recipes) covering CE's ~1,900+ vanilla crafting/smelting recipe corpus.
   All referenced items/materials are already registered (Phase 0-4 complete); the tag/material
   abstraction (2.4) already exists in modern `TagKey<Item>` form.
3. The 7 dynamic `com.hbm.crafting.handlers.*` custom crafting-table recipes are buildable now too,
   though they need genuine new Java (custom `RecipeType`/`RecipeSerializer`/`Recipe<CraftingInput>`
   classes, not just JSON) — this port's own `AssemblerRecipe`/`BreederRecipe` prove the pattern
   compiles in this codebase already, lowering (not eliminating) the risk.

**Blocked / explicitly out of this report's scope:**

1. Actually authoring the 65 advancement JSON files and ~1,900+ recipe JSON/Java definitions
   themselves — per the task brief, this report is research/scoping only.
2. Texture/model PNG asset migration (0 of CE's real 6,965 present) — not owned by this area, and
   apparently not clearly owned by *any* single Phase 5 area yet (2.2) — flagged for whoever
   synthesizes the Phase 5 research wave.
3. Real (non-generic-fallback) item/block display names and the 132 `achievement.*`-family lang
   keys — both already scoped by `docs/phase5/lang_file_and_localization.md`; this report only names
   the dependency, doesn't duplicate that work.
4. Verifying `AdvancementHolder`/`ServerAdvancementManager`/`PlayerAdvancements`'s exact 1.21.1 API
   shape against a real jar or GitHub source read — still unverified (1.9), same status as Phase 4
   left it; this sandbox cannot run `./gradlew` or reach a real jar.

---

## Key risks

1. **`AdvancementHolder`/`ServerAdvancementManager`/`PlayerAdvancements` API shape is still 100%
   unverified against a real compiled jar or any compiling example anywhere in either upstream repo**
   (re-confirmed by this report's own repo-wide grep of Neo Edition, zero hits). If any of the
   documented renames in `AdvancementManager.java`'s javadoc is subtly wrong (e.g. a different method
   name or return type on `PlayerAdvancements`), the 65 JSON files could compile and load fine while
   `grantAchievement`/`hasAdvancement` silently fail or throw at runtime — exactly the failure mode
   the prior crash-fix already had to patch once. Recommend a targeted GitHub-source read (the
   WebFetch technique `docs/phase4/STATUS.md` already used successfully 3 times) before or during the
   implement pass, specifically for these 3 classes.
2. **CE's own `AdvGen.java`/`AdvancementDSL.java` is stale** (1.6) — a future implementer who treats
   it as authoritative rather than cross-reference-only would silently miss the 15 files it doesn't
   cover and could chase a dangling `"bobchemistry"` reference that doesn't exist.
3. **The recipe corpus (~1,900+ entries) is large enough that "port it all in one pass" may not be
   realistic** — this report deliberately does not recommend a tiering/sequencing strategy (out of
   its research-only scope), but flags that whoever plans the implement wave should decide this
   explicitly rather than assume it's a single bounded task like the 65-file advancement tree.
4. **The `"data": N` metadata-subtype resolution (1.5) is per-file manual lookup work**, not
   mechanical — a wrong guess produces a valid-JSON, wrong-icon/wrong-predicate advancement that
   would not be caught by any compiler or datapack-load validation, only by visual/functional QA
   against CE.
5. **Zero texture/model assets (2.2) undermine the practical value of "datagen coverage is
   complete"** — even a perfect JSON pass renders as missing-texture placeholders today. Not this
   area's blocker to clear, but material context for anyone reading this report's "safe to build now"
   claims as "will look right once built" — it will not, until a separate asset-migration pass lands.

## Open questions

1. Does `com.hbm.items.food.ItemEnergy` (this port's `coffee_radium` item class) go through vanilla's
   stock `LivingEntity#eat()`/`finishUsingItem` completion path (which would auto-fire
   `CriteriaTriggers.CONSUME_ITEM` for free), or does it override consumption behavior custom (as Neo
   Edition's analogous `EnergyItem`/`DrinkItem`/`ConserveItem` classes do, each needing an explicit
   added call)? Not traced to a conclusion in this report — small in scope (1 of 65 files) but a real
   loose end.
2. Should the 7 dynamic `com.hbm.crafting.handlers.*` CE recipes (2.3) be ported in the same
   implement-wave pass as the ~1,900 static shaped/shapeless/smelting recipes, or split off as their
   own smaller follow-up given they need net-new custom `Recipe`/`RecipeSerializer` Java classes
   rather than pure JSON authoring? This report characterizes the split but doesn't decide it.
3. Who owns bulk texture/model asset migration (2.2)? At least 5 Phase 5 reports (including this one)
   have now independently hit and named the same "0 of 6,965 CE PNGs present" fact without any of
   them claiming ownership of fixing it — worth explicit resolution when Phase 5's research wave is
   synthesized, since it blocks the *visible* payoff of essentially every other Phase 5 area's work,
   not just this one.
4. Is a value needed for advancement JSON's newer `sends_telemetry_event` field (1.4), or is omitting
   it (defaulting per vanilla) acceptable for a non-Mojang datapack? Low-risk either way, but worth a
   one-line decision before the implement pass writes 65 files with some particular choice baked in.
