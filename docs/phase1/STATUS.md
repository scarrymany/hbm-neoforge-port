# Phase 1 status

Phase 1 (content mass: items and simple blocks) ran across two sessions. The first launched a
15-area implement wave that was interrupted mid-flight (most areas got their IMPLEMENT stage done,
but REVIEW/FIX never ran) - see `HANDOFF.md` for that session's own account. This session picked up
from there: an 11-agent audit wave compared every area's disk state against its research report and
CE source, a 14-agent implement wave closed every gap the audit found, and a 9-agent adversarial
review wave (plus a fix pass) re-read the highest-risk new code against CE a second time. Full
per-area detail lives in `docs/phase1/*.md` (research reports) and the git log (`git log --oneline`
from `1cdc8e9` through `4852589` covers this session's work item-by-item, one commit per area/fix).

## What's registered and working (by area)

All 15 areas from `HANDOFF.md`'s table are now implemented, wired into `ModItems.register()`/
`ModBlocks.register()`, and reviewed at least once against CE:

- **datagen_framework** - confirmed correct and registry-driven (iterates live `ModItems.ITEMS`/
  `ModBlocks.BLOCKS`, not a hardcoded list); this session added `ICustomItemModelRegister` on the
  tool-ability item hierarchy and `ICustomBlockModelRegister` on the pillar/stairs/slab block
  classes so they get real (non-flat, non-cube-all) models, plus vanilla tool-tag seeding.
- **creative_tabs_infra** - unchanged, still the most trustworthy Phase 1 area from the first
  session.
- **items_tool** - bucket (a)'s full ~46-file Phase-1-safe scope now exists: the ability framework
  (`com.hbm.handler.ability.*`) was already correct; this session ported the ~26 remaining
  standalone/container/detector/GUI-shell files. Baubles/Curios items (`ItemDosimeter` and
  siblings) ship as plain held items reading real data off `HbmLivingAttachment` - see "Open
  decisions" below. Buckets (b)/(c)/(d) (melee/military, machine-coupling, dev tooling) remain
  correctly out of Phase 1 scope per the research report.
- **moditems_autogen** - unchanged, trustworthy.
- **modblocks_generative** - the ore/cluster/depth family (section 1a) was already complete; this
  session added the ~57-material storage-block family (section 1b, `MaterialBlockGenerator.java`)
  that was missing entirely.
- **items_ingot_nugget** - reviewed for the first time; found and fixed a silent tooltip-loss bug
  (12 items with real CE flavor text registered as plain `Item`) and wired `ItemHot` into 3 fields
  that were losing their heat-glow behavior.
- **items_billet_powder** - reviewed for the first time; found and fixed a real gameplay bug
  (fertilizer's bonemeal success/consumption semantics were backwards from CE) plus two dead-code
  duplications.
- **items_plate_crystal_waste** - unchanged, trustworthy.
- **items_food_gear** - gear was source-complete but never wired in (one missing line in
  `ModItems.register()` - fixed); food didn't exist at all - `FoodItems.java` and 13 supporting
  classes now cover the full Phase-1-safe scope (7 clean items, all of `ItemLemon`'s catalog, 4
  flattened metadata-multi classes, `ItemEnergy`/`ItemPill`/`ItemCanteen` with TODO-marked
  potion-sickness branches).
- **items_special** - `ItemSoyuz`/`ItemTrain` shells added; `SpecialItems.registerAll()` was
  source-complete but never called (fixed); `ItemDepletedFuel` wired into `PlateCrystalWasteItems`'s
  16 waste fields.
- **items_machine** - all 43 Phase-1-safe classes existed and were correct, but
  `MachineItems.registerAll()`/`MachineDataComponents.register()` were never called (fixed) and
  `ItemPistons` was never registered even after that fix (fixed).
- **blocks_generic_\*** (3 merged sub-areas) - the compile-blocking gap: `ModBlocks.java` referenced
  `GenericBlocks.registerAll()`, which didn't exist. Now does, plus `PlantBlocks`,
  `GenericCrateBlocks`, and `GenericDecoBlocks` cover the plant/fallout, crate/loot, and decorative
  sub-families respectively, and the ~27 files missing from the original 95-file scope are ported.
- **blocks/gas** - confirmed missing entirely at handoff; the 3 Phase-1-safe files
  (`BlockGasBase`/`Flammable`/`Explosive`) are now ported.
- **hazard/tab wiring** - `HazardRegistry.registerContaminatingDrops()` was an empty stub; now binds
  12 dust tags. `items_special`'s demon-core and nuclear-waste bindings, and `items_machine`'s
  entire fuel/rod/pellet family (breeding rod, Zirnox, PWR fuel, pile rod MK2, RTG pellet, Watz
  pellet, RBMK pellet), had zero hazard data despite being fully registered - now bound. Tab wiring
  (`CreativeTabContents.add`) was confirmed present across every area.

## Real bugs found and fixed beyond the areas' own review stages

- **Dead-code registration gap, recurring pattern**: `GearItems`, `MachineItems`+
  `MachineDataComponents`, `SpecialItems`+`SpecialItemComponents`, and `OreBlocks` were all
  source-complete but never invoked from `ModItems.register()`/`ModBlocks.register()` - Java only
  runs a class's static initializers on first active use, so none of their `DeferredItem`/
  `DeferredBlock` fields ever registered. This was the single highest-leverage finding of the
  session's audit wave; fixed by wiring all four in directly.
- **Duplicate registration, would have hard-crashed at mod load**: `ToolItems.java` registered 4
  door items under the same ids `GenericBlocks.registerDoors()`'s real `BlockModDoor` blocks
  auto-register via their `BlockItem`s - a `DeferredRegister.Items` duplicate-name collision. Caught
  by the review wave, fixed by removing the dead `ToolItems` stubs.
- **Content duplication (not a crash, but two registry entries for one material)**: `GenericDecoBlocks`
  hand-registered `block_bismuth`/`_tantalium`/`_niobium`/`_lanthanium`/`_zirconium` and later
  (separately) `block_lithium`/`_boron`/`_lead` under CE's legacy ids, duplicating what
  `MaterialBlockGenerator` produces under the port's suffix-first convention for the same 8
  materials (all `Mats.java`-tagged for `MaterialShapes.BLOCK` autogen). Fixed both times the
  review wave caught it.
- **Wrong import package**: all 13 `com.hbm.hazard.type.*` classes imported `com.hbm.util.I18nUtil`
  instead of the real `com.hbm.util.i18n.I18nUtil` - a Phase 0-era bug, unrelated to this session's
  own work, caught by a static compile-triage pass and fixed.
- `ItemPipette.tryEmpty()` returned the leftover (undrained) amount instead of the amount actually
  drained - inverted from both CE's real behavior and the documented `IFillableItem` contract.

## Known gaps intentionally deferred to later phases

These are expected forward references, not regressions - the owning phase resolves them:

- `com.hbm.blocks.BlockDummyable` imports `com.hbm.handler.MultiblockHandlerXR` and
  `com.hbm.tileentity.IPersistentNBT`, neither of which exists - the Phase 2 multiblock framework's
  job. Confirmed by this session's static triage to be unchanged since Phase 0 and not referenced
  anywhere in Phase 1's own new code.
- ~~`com.hbm.items.machine.ItemBlueprints`/`ItemBlueprintFolder` import
  `com.hbm.inventory.recipes.loader.GenericRecipe(s)`, which doesn't exist~~ - **closed in Phase 2**
  (`docs/phase2/items_tool_machine_coupling_and_recipe_system.md` Part B): `com.hbm.inventory.RecipesCommon`
  (the `AStack`/`ComparableStack`/`NbtComparableStack`/`OreDictStack`/`MetaBlock` comparison-key
  hierarchy - also unblocks `com.hbm.api.block.IToolable` and `com.hbm.hazard.HazardSystem`, both of
  which already referenced it uncompiled) and a deliberately minimal
  `com.hbm.inventory.recipes.loader.GenericRecipe(s)` compile shim (pool bookkeeping +
  `getLocalizedName()` only - the full CE method-chaining machine-recipe loader is NOT ported, see
  that class's own header) are both now in the tree. `ItemBlueprints`/`ItemBlueprintFolder` compile
  against them. The real per-machine JSON `Recipe<?>` conversion PORT_SPEC.md's ground rule calls for
  is separate follow-up work, scaffolded (not yet populated) in
  `com.hbm.inventory.recipes.{HbmRecipes,HbmSimpleRecipe}`.
- The 11 classes repeatedly referenced across this session's research as "needs a cross-cutting
  system that doesn't exist yet" remain correctly absent and are not half-referenced by any Phase 1
  code: `ContaminationUtil`, `ArmorUtil`, `HbmPotion`, `HbmLivingProps`, `PollutionHandler`,
  `ChunkRadiationManager`, `ConsumableHandler`, `IFillableItem`, `ItemTooling`,
  `MultiblockHandlerXR`, `IPersistentNBT`. Every Phase 1 branch that would call into one of these is
  marked with an explicit inline TODO naming the exact CE call it replaces (per the project's
  no-silent-drop rule), not silently dropped or faked.
- `MaterialBlockGenerator`'s `radium_block` loses CE's `.makeBeaconable()` trait (CE's one material
  with both contact-radiation-hazard and beacon-base behavior at once) - this port's
  single-behavior-class-per-material design has no composition mechanism for that yet; documented
  in the class javadoc as a follow-up for whoever designs a trait/interface-composable block-behavior
  system, not fixed here.
- Several items across `items_tool`/`items_special` ship as registered shells with documented
  TODO/stub `use()`/`useOn()` overrides pending blocks or systems that don't exist yet (`ModBlocks.
  ntm_dirt`, a placeable door block instance, `ConsumableHandler`, `PollutionHandler`, the rail/rocket
  subsystem for `ItemSoyuz`/`ItemTrain`, etc.) - each documents its exact blocker inline.
- A Menu/Screen framework (`AbstractContainerMenu`/`Screen`, replacing CE's `IGUIProvider`) does not
  exist yet. 8 items across `items_tool`/`items_special` (`ItemGuideBook`, `ItemAmmoBag` and
  siblings, `ItemBook` and siblings) ship as registered shells with no menu-opening interaction,
  following the pattern `ItemBook` already established in the first session. Building this
  framework is recommended as an early Phase 2 task since machines need it too.

## Open decisions made this session (not previously settled)

- **Baubles/Curios**: no Curios API dependency exists in `build.gradle`, and CE's own Baubles usage
  was an optional soft dependency (`@Optional.InterfaceList`). Decision: port `ItemDosimeter`,
  `ItemGeigerCounter`, `ItemDigammaDiagnostic`, `ItemLungDiagnostic` as plain held items for Phase 1
  (functional parity over accessory-slot parity), reading real data off the existing
  `HbmLivingAttachment` capability rather than stubbing. Revisit Curios-slot integration later if the
  project adds that dependency.
- **Food tier naming**: CE's `ItemAppleSchrabidium`/`ItemTemFlakes` have no per-tier names, only
  damage values 0/1/2. Decision: `_low`/`_mid`/`_high` suffixes.
- **`ItemFlask`**: de-generified to one plain item (CE's `ItemEnumMulti` only ever used its single
  `SHIELD` enum value), wired to the existing `HbmPlayerAttachment` shield methods.
- **Material storage-block family naming**: follows the project's already-established suffix-first
  convention (`titanium_block`, not `block_titanium`) for consistency with every other
  `Mats x MaterialShapes` item/block family, per Phase 0's precedent.

## On "gradlew build green" as a per-phase gate

Unlike the first session, **this sandbox cannot run `gradlew` at all** - the org's egress policy
blocks `maven.neoforged.net` (confirmed via `curl`/the agent-proxy status endpoint), so the NeoForge
toolchain can never resolve here, not even to reproduce Phase 0's own documented ~100-error
baseline. Every area in this session was verified by static means instead: reading every constructor
call against the real target class, checking every import resolves to a real file, grepping for
registry-id collisions, and (for the highest-risk new code) a dedicated adversarial review wave. This
is inherently weaker than a real compiler and already caught fewer things than review-by-humans
would (see "real bugs found" above for what static review DID catch) - the next session or CI run
with real network access to `maven.neoforged.net` should run `gradlew compileJava` as the first
order of business and treat any error not explained by this file or `docs/phase0/STATUS.md` as a
real regression to fix immediately, rather than assuming it's yet another expected forward
reference.
