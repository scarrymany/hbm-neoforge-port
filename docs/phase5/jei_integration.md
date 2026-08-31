# Phase 5 research: JEI integration

**Area:** `jei_integration` — PORT_SPEC.md's client/UX line item "JEI integration for every recipe
type" (`PORT_SPEC.md:48`). This report inventories every machine-recipe type this port's Phases 0-3
actually registered, classifies each against the real modern JEI extension API (confirmed against a
genuinely compiling reference, not guessed), and states plainly what is safe to build today versus
what is blocked and on whom.

## Method

- Read every file under this port's own `src/main/java/com/hbm/inventory/recipes/**` in full (14 files,
  listed with line counts in the table below) — the actual, current recipe-registration surface this
  report has to build JEI categories against.
- Read this port's `build.gradle`/`settings.gradle`/`gradle.properties` directly — no assumption about
  dependency state.
- Read `upstream/hbm-ce/src/main/java/com/hbm/handler/jei/*.java` (40 files, 5,684 lines total, `wc -l`
  counted) — CE's own real, shipped JEI plugin. This is CE's real behavioral/design intent (which
  categories exist, what each one's catalyst/slot layout communicates to the player) and is treated as
  source-of-truth for *that*, per this project's ground rules — but its API (`mezz.jei.api.recipe.
  IRecipeWrapper`, `IIngredients`, `IIngredientType`) is the **1.12.2-era JEI 4.x API**, structurally
  unrelated to the modern API a NeoForge 1.21.1 build will compile against. Code from this package is
  cited only for *what a category needs to show*, never copied for *how to register it*.
- Read `upstream/neo-edition/src/main/java/com/hbm/handler/jei/*.java` (18 files) and its
  `build.gradle`/`gradle.properties` — a genuinely real, modern JEI plugin already written against
  **JEI `19.25.0.325` for Minecraft `1.21.1`/NeoForge** (`upstream/neo-edition/gradle.properties:27`),
  the exact Minecraft/NeoForge version this port targets (`neo_version=21.1.228` in both repos' own
  `gradle.properties`, confirmed identical). Per this project's standing rule this is used **only** to
  confirm the real modern JEI API's class/method shapes (interface names, method signatures, the
  registration call sequence) — never as a source of which recipes exist, what a category's numbers
  should be, or layout/visual design. Every design/behavior claim in this report instead cites this
  port's own or CE's real recipe data.
- Grepped this whole port for `jei`/`JEI` case-insensitively to find already-existing forward
  references (config fields, javadoc scope notes) before assuming anything was untouched.

**Unverified-against-a-real-build flag, stated once up front so it isn't repeated after every claim**:
this sandbox cannot run `./gradlew` (blocked network policy) or launch a client, so nothing about JEI
in this report is screenshot- or compile-verified. The modern-JEI API shapes below are cross-checked
against `upstream/neo-edition`'s own source, which itself is *also* unverified by a real build in this
sandbox — it is simply the best available "this at least looks like real, intentional, versioned-API
code" reference, consistent with how prior phases (e.g. `docs/phase4/STATUS.md`'s own closing
paragraph) have treated Neo Edition. Anywhere this report states a JEI interface/method name, read it as
"well-established from a real, version-pinned neo-edition source file, not jar-verified."

## Headline findings

1. **This port has no JEI dependency at all today.** `build.gradle` (91 lines, read in full) declares
   zero dependencies of any kind beyond the `net.neoforged.moddev` plugin itself — no `dependencies {}`
   block exists. `settings.gradle` (13 lines) declares only `gradlePluginPortal()` and
   `https://maven.neoforged.net/releases` as repositories. Neither the JEI Maven host nor a
   `mezz.jei:jei-*` coordinate appears anywhere in this port. This is the area's most basic gap, exactly
   as this task's brief predicted, and it blocks every other finding below from actually compiling.
2. **This port already has 11 distinct machine-recipe registries from Phases 0-3** (not CE's ~60 — see
   "Recipe families CE has that this port doesn't" below for the rest), split 3-vanilla/8-bespoke. Full
   per-type breakdown in the table below.
3. **This port's fluids are not real NeoForge `Fluid`/`FluidStack`** — `com.hbm.inventory.fluid.
   FluidStack`/`FluidType` (confirmed: `src/main/java/com/hbm/inventory/fluid/FluidStack.java:9`,
   `FluidType.java:30`) are a wholly bespoke non-vanilla system, same as CE's own. This means JEI's
   native fluid-ingredient rendering (built for real `Fluid`s) is architecturally the wrong tool here —
   and both CE's real plugin and neo-edition's real plugin independently solved this the same way:
   represent a fluid recipe slot as an ordinary **item** slot holding a special "fluid icon" stack (CE:
   `ItemFluidIcon.make(...)`; neo-edition: its own `FluidIconItem.make(...)`, e.g.
   `upstream/neo-edition/.../RefineryRecipeHandler.java:77`). **This port already has the exact
   equivalent, already registered**: `com.hbm.items.machine.ItemFluidIcon` (registered as
   `hbm:fluid_icon` at `src/main/java/com/hbm/items/machine/MachineItems.java:274`), with static
   factories `make(Item, FluidStack)` / `make(Item, FluidType, int)` / `make(Item, FluidType, int, int)`
   (`ItemFluidIcon.java:55-67`) matching the exact call shape neo-edition's real plugin uses. No new
   ingredient-type registration is needed for any fluid-bearing category in this port — every one of
   them can use plain item slots.
   - **Correction to this port's own docs**: `RefineryRecipes.java`'s class javadoc (line 26) still says
     CE's JEI display builders use "`ItemFluidIcon.make(...)`, an item this port has not ported." That
     is now stale — `ItemFluidIcon` exists and is registered (see above); whoever wrote that javadoc
     line predates a later items pass that added it. Worth a one-line javadoc fix alongside whatever
     implements this area.
4. **This port has zero GUI texture assets of any kind** (independently confirmed both by this report's
   own `find src/main/resources -iname '*.png'` → 1 unrelated file, and by the sibling Phase 5 report
   `docs/phase5/gui_screens_survey_machines_processing.md`'s headline finding #1: "Zero real CE GUI
   textures exist anywhere in this port's resources today," against CE's real 493). Every real JEI
   category example in neo-edition builds its `background`/`icon` `IDrawable` by cropping a real machine
   GUI PNG (e.g. `guiHelper.drawableBuilder(..."textures/gui/jei/gui_nei_shredder.png", 5, 11, 166,
   65)...build()`, `ShredderRecipeHandler.java:31-35`). Since none of those PNGs exist in this port yet,
   a JEI category's *visual chrome* (the background frame image, the machine icon) is blocked on that
   sibling GUI-asset-porting area's work landing — but this is a soft/visual block, not a hard one: a
   category can ship today with a flat-fill placeholder background (the same interim convention
   `GuiInfoContainer` already uses for every real screen per that sibling report's finding #1) and be
   reskinned later without touching any registration or slot-matching code.
5. **CE's own real JEI plugin (`com.hbm.handler.jei.*`, 40 files) confirms which of this port's already-
   built recipe shapes are and aren't "standard-shaped"** — CE itself needed 3 separate SILEX categories
   split by wavelength tier (`SILEXVisibleRecipeHandler`/`SILEXIrRecipeHandler`/
   `SILEXGammaRecipeHandler`), a custom "N. G. Cents" + high-speed-icon overlay for the gas centrifuge
   (`JeiRecipes.java:104-140`), and treated RBMK fuel recycling as synthetic display-only rows
   (`RBMKFuelRecipeHandler`) distinct from the live conversion function — all signals this report's own
   independent read of this port's code (see table) arrived at the same conclusions on, not contradicted
   design guesses.
6. **Dead config plumbing already exists, ready for a real plugin to read**:
   `GeneralConfig.ENABLE_JEI` (`GeneralConfig.java:81-284`, comment: "Enables JEI compatibility. [CE:
   1.28_enableJei]") and `ClientConfig.JEI_HIDE_SECRETS` (`ClientConfig.java:41,94`, "Hides secret/hidden
   items from JEI"). Both are defined, both have zero readers anywhere in this port today (grepped) —
   forward references from an earlier phase with nothing yet to gate.
7. **This port is missing several recipe families the task brief and CE both name**: Anvil (no
   `AnvilRecipes`/`AnvilSmithingRecipe`/anvil block/menu anywhere in this port — the whole machine does
   not exist yet), Blast Furnace (same — no `BlastFurnaceRecipes`/`BlastFurnaceRecipesNT` port), and the
   Crucible/foundry casting system (only a marker interface, `com.hbm.api.block.ICrucibleAcceptor`
   exists — no real recipes; `ElectrolyserFluidRecipes.java:20-22`'s own javadoc already names this exact
   gap for its metal-electrolysis half). These are real, out-of-scope-for-*this*-report gaps: you cannot
   build a JEI category for a machine whose server-side recipe data doesn't exist. They're blocked on
   whichever future phase ports those machines, not on JEI research or this task.

## This port's real recipe registries today (Phases 0-3), by machine

Every file below is under `src/main/java/com/hbm/inventory/recipes/` (or `recipes/chem/`,
`recipes/machine/`, `recipes/machine/rbmk/`) unless noted, read in full.

| Machine | Class(es) (lines) | Real I/O shape | Recipe kind | JEI category classification |
|---|---|---|---|---|
| **Shredder** | `HbmSimpleRecipe.java` (190) via `ProcessingRecipes.java` (76), `SHREDDER_TYPE` | 1 `Ingredient` in → 1 `ItemStack` out, optional duration | Real vanilla `Recipe<SingleRecipeInput>`/`RecipeType`/`RecipeSerializer`, JSON-backed. **44 real recipe JSON files already exist** (`data/hbm/recipe/shredder/*.json`) | **Standard.** Textbook single-in/single-out category, same shape JEI ships examples for. Near-zero custom code; `RecipeManager.getAllRecipesFor(SHREDDER_TYPE)` feeds it directly. |
| **Assembler** | `AssemblerRecipe.java` (219), `ASSEMBLER_TYPE` | Up to 12 unordered `(Ingredient, count)` entries in → 1 `ItemStack` out, duration+power | Real vanilla `Recipe<AssemblerRecipe.Input>` (custom `RecipeInput`), JSON-backed. **13 real recipe JSONs exist** (`data/hbm/recipe/assembler/*.json`) | **Custom category, low complexity.** Needs a 12-slot input grid (a loop, not per-slot code) + 1 output slot. Neo-edition's real, compiling `AssemblyMachineRecipeHandler.java` (111 lines) already demonstrates this exact multi-slot-loop pattern for its own (structurally similar) `GenericRecipe`. |
| **Breeder** | `BreederRecipe.java` (128)/`BreederRecipes.java` (44), `BREEDER_TYPE` | 1 `Ingredient` in → 1 `ItemStack` out + a `flux` cost int | Real vanilla `Recipe<SingleRecipeInput>`, JSON-backed. **30 real recipe JSONs exist** (`data/hbm/recipe/breeder/*.json`) | **Standard + one label.** Same shape as Shredder, plus one drawn text overlay for the flux number (`BreederRecipe.getFlux()`, line 59) — no vanilla category renders that for free, but it's a single `guiGraphics.drawString` call in `draw(...)`. |
| **Crystallizer** | `CrystallizerRecipes.java` (189) | 1 item (or tag) + a required `FluidType`+amount in → 1 `ItemStack` out + a `productivity` chance float | Bespoke `Map<Pair<ComparableStack,FluidType>, CrystallizerRecipe>`, hardcoded Java, **not** a vanilla `Recipe<?>` (deliberate — see the class's own javadoc citing `docs/phase2/machines_shredder_assembler_crystallizer_mixer.md`'s "own bespoke class" call) | **Custom category, moderate.** 1 item input slot + 1 `ItemFluidIcon` "acid" slot + 1 output slot + a productivity-% tooltip/label. `RECIPES` is `private` (line 56) with only a point-lookup accessor (`getOutput(ItemStack, FluidType)`, line 163) — **needs one new `public static` full-collection getter added** for a JEI plugin to iterate; trivial, ~3 lines. |
| **Centrifuge** (item, not gas) | `CentrifugeRecipes.java` (181) | 1 `AStack` (item or `c:` tag) in → up to 4 `ItemStack` outputs | Bespoke `public static final Map<AStack, ItemStack[]>` (line 34, already public) | **Standard-shaped, low complexity.** 1 input + up to 4 outputs — exactly what neo-edition's own real, compiling `CentrifugeRecipeHandler.java` (79 lines) already implements end-to-end for the structurally-identical CE Centrifuge. Directly adaptable. |
| **Gas Centrifuge** | `GasCentrifugeRecipes.java` (145) | **Not item/output-keyed at all** — a stateful isotope-enrichment *cascade*: a real feed `FluidType` (`FLUID_CONVERSIONS` map, line 129) converts 1:1 into a `PseudoFluidType`, which then decays through a fixed same-class chain (`NUF6→LEUF6→MEUF6→HEUF6`, `PF6` terminal, `MUD→MUD_HEAVY`) consuming/producing an internal fluid-charge amount per stage, each stage optionally dropping byproduct `ItemStack[]`s, gated by an `isHighSpeed` flag | Bespoke, in-memory `PseudoFluidType` state-machine class, not a keyed recipe table at all | **Genuinely custom, moderate-high.** No simple "input→output" mapping exists to iterate — a JEI-facing wrapper must flatten the chain into one synthetic per-stage-transition recipe object (feed amount, produced amount, byproducts, high-speed flag), the exact same flattening CE's own real plugin already does (`JeiRecipes.GasCentrifugeRecipe`, `JeiRecipes.java:104-141`, complete with a custom high-speed icon overlay via `drawInfo`). This is real, non-trivial glue code, not boilerplate. |
| **Cyclotron** | `CyclotronRecipes.java` (84) | 2 distinct inputs — a catalyst item (`ComparableStack`) + a target ore/dust (`AStack`, usually a tag) — → 1 `ItemStack` out + an antimatter-mB yield `int` (a non-item numeric side-output) | Bespoke `public static final Map<Pair<ComparableStack,AStack>, Pair<ItemStack,Integer>>` (line 32, already public) | **Custom category, moderate.** 2 side-by-side input slots + 1 output slot + a drawn antimatter-yield number (no vanilla-shaped category has a slot for a bare int). CE's own real `JeiRecipes.CyclotronRecipe` (`JeiRecipes.java:64-81`) is the same 2-in/1-out shape, confirming the layout, not the numbers. |
| **SILEX** | `SILEXRecipes.java` (115) | 1 item in → a **weighted-random** pool of possible `ItemStack` outputs (`WeightedRandomObject`, not "all of," not fixed), gated by a minimum `EnumWavelengths` tier, plus a material-charge produced/consumed pair | Bespoke `public static final Map<ComparableStack, SILEXRecipe>` (line 38, already public) | **Custom category, moderate.** JEI's per-slot `addItemStacks(list)` already natively cycles a list of alternative outputs in one slot (~1s rotation) — covering the "N possible outputs" shape acceptably, though JEI has no weighting concept, so CE's real weight skew (e.g. 11:1 U238:U235) is cosmetically flattened to equal-likelihood cycling in JEI's display only, not in the live drop table. The wavelength gate needs its own icon/label. CE itself split this into 3 separate categories by wavelength tier (`SILEXVisibleRecipeHandler`/`SILEXIrRecipeHandler`/`SILEXGammaRecipeHandler`) rather than one category with a label — worth deciding which this port follows (open question below). |
| **Electrolyser** (fluid half) | `ElectrolyserFluidRecipes.java` (68) | 1 input fluid (a full required `amount`, not per-tick) → 2 output fluids + optional item byproducts | Bespoke `public static final Map<FluidType, ElectrolysisRecipe>` (line 26, already public) | **Custom category, moderate but structurally fixed.** 1 `ItemFluidIcon` input slot + 2 `ItemFluidIcon` output slots + up to N byproduct item slots — fixed slot count, no vanilla shape fits an all-fluid layout. Note: the *metal* half (ore/crystal electrolysis) is explicitly not ported yet (class javadoc, lines 18-22) — nothing to build JEI for there. |
| **Mixer** | `MixerRecipes.java` (204) | Keyed by **output** `FluidType`, each key mapping to an **array of competing recipes** (up to 2 optional input fluids + 1 optional solid input → the keyed output fluid amount) | Bespoke `private static final Map<FluidType, MixerRecipe[]>` (line 51) | **Custom category, moderate** — the competing-array shape is *not* actually a JEI problem (a common misconception this report explicitly checked): JEI is perfectly happy to register multiple distinct recipe instances that share the same output, it just lists them as separate recipe pages. `RECIPES` is `private` with only `getOutput(FluidType[,int])`/`findMatch(...)` point-lookup accessors (no full-listing getter) — **needs one new accessor added** (flatten `RECIPES.values()`'s arrays into one list) for JEI registration. The optional-slot handling (2 fluids, 1 solid, any subset may be null) is the real per-recipe complexity, not the array. |
| **Refinery** | `RefineryRecipes.java` (132) | Keyed by **input** `FluidType` → up to 4 fixed-percentage output fluids + 1 optional item byproduct (`refinery` map), plus a separate, never-actually-consumed `vacuum` map of the same shape | Bespoke `private static final Map<FluidType, Tuple.Quintet<...>>` (lines 69-70) | **Custom category, low-to-moderate — de-risked by a working reference.** `RECIPES`/`vacuum` are `private` with only point-lookup `getRefinery(FluidType)`/`getVacuum(FluidType)` (no full-listing getter — needs one added, same as Mixer/Crystallizer). Neo-edition's real, compiling `RefineryRecipeHandler.java` (129 lines, read in full) already solves this **exact same 1-in/4-out+byproduct shape** end to end against its own structurally-identical `RefineryRecipes.getRecipes()` map — near-directly adaptable onto this port's own `getRefinery(...)` map, swapping `FluidIconItem.make` for this port's own `ItemFluidIcon.make`. This port's own `RefineryRecipes.java` javadoc (lines 25-31) already explicitly deferred "JEI/REI integration" to "out of this task's boundary" — this report is that boundary's owner now. |
| **Chemical Plant** | `ChemPlantRecipes.java` (105) | Up to 3 `AStack` item inputs + up to 2 `FluidStack` fluid inputs → up to 3 `ItemStack` outputs + 1 `FluidStack` output, duration+power | Bespoke `public static final List<ChemPlantRecipe>` (line 44, already public — a `List`, not even a `Map`, simplest accessor of the bespoke set) | **Custom category, moderate — de-risked by a working reference.** The most I/O-diverse shape in this port (3 items in + 2 fluids in + 3 items out + 1 fluid out). Neo-edition's real, compiling `ChemicalPlantRecipeHandler.java` (79 lines) already implements exactly this shape against its own (structurally near-identical) `GenericRecipe` — loop-based slot placement for both item and fluid sides, directly adaptable. |
| **RBMK fuel recycling** | `RBMKFuelRecipes.java` (68), package `recipes/machine/rbmk` | **Not a keyed table at all** — `getRecyclingOutput(ItemStack)` and `computeStage(ItemStack)` are pure functions over a *live* rod stack's current enrichment/xenon-poison data-component state (the class's own javadoc explains why: a static NBT-keyed table would never match a post-burn stack) | Pure static logic, deliberately not registered as any `Recipe`/`RecipeType` (documented decision, lines 16-27) | **Genuinely the hardest one — needs new code, not just a registration wrapper.** There is no collection to hand `registerRecipes`. A JEI-facing wrapper needs a synthetic enumerator: for every registered `ItemRBMKRod`, generate representative example stacks across the 0-9 `computeStage` bucket range (matching CE's own real precedent — this class's own javadoc already namechecks CE's `RBMKFuelRecipes.addRod`, which existed *for exactly this display purpose*, "not as the live conversion path") — then pair each with `getRecyclingOutput`'s result. This enumerator does not exist anywhere in this port or in neo-edition today; it is new code this area would have to write, the single largest net-new piece of glue in this whole report. |

**Excluded from the table on purpose**: `com.hbm.inventory.recipes.loader.GenericRecipe`/`GenericRecipes`
(`loader/GenericRecipe.java` 106 lines, `loader/GenericRecipes.java` 60 lines) — this is **not** a
machine recipe type. It is a blueprint-*pool* bookkeeping stand-in (`ItemBlueprints`/
`ItemBlueprintFolder`'s tooltip pool-name index), explicitly documented in its own class javadoc as
deliberately not carrying CE's real multi-input/fluid machine-recipe shape. Nothing for JEI to show here
— it names pools, it doesn't produce items.

## The real modern JEI API shape (confirmed via neo-edition's compiling source)

`upstream/neo-edition/src/main/java/com/hbm/handler/jei/NtmJeiPlugin.java` (234 lines, read in full) is
a complete `@JeiPlugin`-annotated `IModPlugin` implementation against JEI `19.25.0.325`/NeoForge
`21.1.228`/Minecraft `1.21.1` — the same NeoForge/Minecraft pin this port itself uses
(`gradle.properties:19` in both repos: `neo_version=21.1.228`). Confirmed real method surface (present
and called with real project types, not stubs):

- `IModPlugin.getPluginUid()`, `registerCategories(IRecipeCategoryRegistration)`,
  `registerRecipes(IRecipeRegistration)`, `registerRecipeCatalysts(IRecipeCatalystRegistration)`,
  `registerRecipeTransferHandlers(IRecipeTransferRegistration)`,
  `registerGuiHandlers(IGuiHandlerRegistration)`, `registerItemSubtypes(ISubtypeRegistration)`,
  `registerExtraIngredients(IExtraIngredientRegistration)` — every one of these 8 hooks is implemented
  with real calls (`NtmJeiPlugin.java:53-232`), not left as no-ops.
- `IRecipeCategoryRegistration.getJeiHelpers().getGuiHelper()` then `addRecipeCategories(...)` (line
  54-70) — categories are plain constructor calls, no `DeferredRegister`-style indirection.
- `IRecipeRegistration.addRecipes(RecipeType<T> type, List<T> recipes)` (lines 74-88) — **critically,
  JEI's own `RecipeType<T>` here is JEI's own key type, built via `RecipeType.create(modid, name,
  Class<T>)`** (e.g. `ShredderRecipeHandler.java:19-23`) — it is completely independent of vanilla's
  `net.minecraft.world.item.crafting.RecipeType`. This confirms the earlier assumption in this report's
  table: a JEI category's backing `T` **does not need to be a vanilla `Recipe<?>` at all** — CE's own
  `GenericRecipe` and this port's own bespoke `CrystallizerRecipe`/`ChemPlantRecipe`/etc. plain-Java
  classes are equally valid JEI recipe types, registered by handing JEI a `List<T>` built from whatever
  in-memory structure already holds the data (a `Map.values()`, a flattened array-of-arrays, a synthetic
  enumerator — see the RBMK row above).
- `IRecipeCategory<T>` interface: `getRecipeType()`, `getTitle()` (returns `Component`),
  `getBackground()`/`getIcon()` (both `IDrawable`), `setRecipe(IRecipeLayoutBuilder, T, IFocusGroup)`,
  `draw(T, IRecipeSlotsView, GuiGraphics, double mouseX, double mouseY)` — confirmed identically
  implemented across every one of the 15 category classes read (`ShredderRecipeHandler`,
  `AssemblyMachineRecipeHandler`, `CentrifugeRecipeHandler`, `ChemicalPlantRecipeHandler`,
  `RefineryRecipeHandler`, `AnvilRecipeHandler`, `AnvilConstructionRecipeHandler`).
- `IGuiHelper.drawableBuilder(ResourceLocation, u, v, w, h).setTextureSize(256, 256).build()` for a
  cropped background, `.createDrawableItemLike(Item)` for an icon (every category constructor above).
- `IRecipeLayoutBuilder.addInputSlot(x, y)` / `.addOutputSlot(x, y)`, chained with
  `.setStandardSlotBackground()` / `.setOutputSlotBackground()`, then `.addItemStack(stack)` /
  `.addItemStacks(List<ItemStack>)` to populate it (every category's `setRecipe`). Also seen:
  `.addRichTooltipCallback((slotView, tooltip) -> ...)` for a per-slot dynamic tooltip
  (`AnvilConstructionRecipeHandler.java:82-87`, used for a drop-chance %), `.setShapeless(x, y)` and
  `.createFocusLink(slotA, slotB)` (`AnvilRecipeHandler.java:62-70`, linking two independently-searchable
  input slots as one logical ingredient set).
- `IRecipeCatalystRegistration.addRecipeCatalyst(ItemStack/Item, RecipeType<?>)` — registers "this block
  opens/produces recipes of this category" for JEI's catalyst display (`NtmJeiPlugin.java:91-151`, one
  call per machine block).
- `IGuiHandlerRegistration.addRecipeClickArea(ScreenClass, x, y, w, h, RecipeType<?>)` — wires a
  clickable region of a real machine's own `Screen` straight to "show me recipes of this type"
  (`NtmJeiPlugin.java:161-165`).
- `ISubtypeRegistration.registerSubtypeInterpreter(Item, ISubtypeInterpreter)` — used to tell JEI two
  stacks of the same `Item` with different data components/NBT are meaningfully different ingredients
  (`NtmJeiPlugin.java:169-218`, a 20+ item ignore-meta list plus a dedicated `BatterySubtypeInterpreter`)
  — directly relevant to this port's own data-component-heavy items (RBMK rods, battery packs) if this
  port wants JEI to distinguish charge states/enrichment the way neo-edition already does.
- `IExtraIngredientRegistration.addExtraItemStacks(List<ItemStack>)` — used to inject every fluid's
  `FluidIconItem` representation into JEI's searchable ingredient list even though nothing ever crafts
  one directly (`NtmJeiPlugin.java:221-232`) — the same technique this port's own `ItemFluidIcon` would
  need for fluids to be independently searchable/bookmarkable in JEI, not just visible inside a recipe.

None of this is exotic or version-fragile-sounding API surface — it reads as the same general JEI 9.x+
category-registration shape that has been stable across many Minecraft versions, which is some
independent (if soft) corroboration beyond "one neo-edition file says so."

## Recipe families CE has that this port doesn't (yet) — real gaps, not this report's job

Confirmed by listing `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/*.java` (~60 files) against
what this port's own package actually contains (14 files). Not exhaustively re-derived here (that's a
future items/machines-phase job, not this report's), but the ones the task brief explicitly named or CE's
own JEI plugin (`com.hbm.handler.jei.*`) builds a whole category for, that this port has *nothing* for:

- **Anvil** (`AnvilRecipes`/anvil block+menu) — the entire machine, not just its recipes, is unbuilt in
  this port. CE's/neo-edition's real anvil JEI story is unusually rich (2 categories — construction and
  smithing — 4 visual overlay layouts, weighted/chance output tooltips, tier-gated catalyst lists,
  `createFocusLink` for its 2-ingredient smithing slot) — worth flagging now so whichever future phase
  builds the Anvil machine knows its JEI category will be one of the most bespoke in the whole mod, not
  a quick add-on afterward.
- **Blast Furnace** (`BlastFurnaceRecipes`/`BlastFurnaceRecipesNT`) — same story, machine unbuilt.
- **Crucible / foundry casting** — only `com.hbm.api.block.ICrucibleAcceptor` (a marker interface)
  exists; no recipe data. `ElectrolyserFluidRecipes.java`'s own javadoc (lines 18-22) already names this
  exact dependency for its own metal-electrolysis half.
- Everything else CE's `com.hbm.handler.jei` package builds a category for and this port hasn't touched
  at all: Press, Soldering Station, Arc Welder, Furnace Combination, Boiler, PUREX, Pyro Oven, Reforming,
  Cracking, (non-legacy) Fusion, Rock Mill, Rotary Furnace, Compressor, Radiolysis, Super Computer,
  Plasma Forge, Outgasser, Storage/Waste Drum, DFC, Lemegeton, Solidification, RTG (a fuel-info display,
  not a real recipe) — none of these machines exist in this port yet (grepped for each class name,
  zero hits beyond the two repos). Not a JEI gap; a machines/items gap for a future phase.

## Safe to build now

1. **Add the JEI dependency.** Nothing below this line compiles without it. Exact coordinates, cross-
   checked against neo-edition's own real, version-pinned declaration
   (`upstream/neo-edition/build.gradle:130-132`, `gradle.properties:27`):
   ```gradle
   repositories {
       maven { url = 'https://maven.blamejared.com' } // hosts mezz.jei artifacts
   }
   dependencies {
       compileOnly "mezz.jei:jei-1.21.1-neoforge-api:19.25.0.325"
       runtimeOnly "mezz.jei:jei-1.21.1-neoforge:19.25.0.325"
   }
   ```
   Unverified: this sandbox cannot resolve `maven.blamejared.com` or confirm `19.25.0.325` is still
   current — whoever has real build access should confirm both before relying on this pin.
2. **Shredder, Assembler, Breeder** — all 3 are real vanilla `Recipe<?>` types with real JSON data
   already loaded (44+13+30 files). These are the lowest-risk, highest-immediate-value categories: no
   accessor plumbing needed, `RecipeManager.getAllRecipesFor(TYPE)` feeds JEI directly, and two of the
   three (Shredder, Breeder) are single-in/single-out, the simplest category shape JEI has.
3. **Centrifuge, Chemical Plant, Cyclotron, Electrolyser (fluid), SILEX** — all 5 already expose their
   backing collection as a `public static` field/method, so a JEI plugin module can read them with zero
   new production code beyond the category class itself.
4. **Crystallizer, Mixer, Refinery** — same as above, plus one small, low-risk addition each: a
   `public static` full-collection getter (their current accessors are point-lookups only). ~3 one-line
   methods total.
5. **Fluid rendering in every category** — reuse this port's own already-registered `ItemFluidIcon`
   (`hbm:fluid_icon`) exactly as neo-edition's real plugin uses its own equivalent. No new ingredient
   type, no new item.
6. **Wire `GeneralConfig.ENABLE_JEI`/`ClientConfig.JEI_HIDE_SECRETS`** into the new plugin's
   `registerCategories`/`registerItemSubtypes` (both fields already exist and are currently dead code).

## Blocked (named blocker, named owner)

- **Anvil / Blast Furnace / Crucible-foundry JEI categories** — blocked on those machines existing at
  all. Owner: whichever future phase ports those machine families (not Phase 5 client/UX by itself;
  these are server-side recipe/block-entity gaps first).
- **RBMK fuel recycling category** — blocked on writing a new synthetic-example-row enumerator (no
  collection exists to register today); this is real new code this area would produce itself, not an
  external blocker, but it's the one row in this report that isn't "wrap an existing collection."
- **Gas Centrifuge category's real content** — same: needs the cascade-flattening glue code described
  in the table before it has anything to register, though the state machine itself (`PseudoFluidType`)
  is already fully built and correct (Phase 2 work, confirmed).
- **Category background/icon textures** — soft-blocked on the sibling `gui_screens_survey_machines_
  processing.md` area's GUI-PNG asset port (0 of CE's real 493 exist in this port today). Categories can
  ship with flat-fill placeholders in the meantime (matching this port's own existing screen convention)
  and be reskinned later without touching registration/matching code — not a hard blocker on shipping
  JEI support, just on it looking finished.
- **JEI's own Maven reachability** — this sandbox cannot confirm `maven.blamejared.com` resolves under
  this project's network policy (only `maven.neoforged.net` is explicitly named as blocked in this
  project's ground rules; blamejared.com's status is unconfirmed either way). Whoever next has real
  `./gradlew` access should confirm this first, before any other work in this area.

## Known risks

- **JEI version drift.** `19.25.0.325` is neo-edition's pinned version as of whenever that repo was last
  updated, not independently confirmed as "current" for `1.21.1`/NeoForge `21.1.228` today. JEI ships
  frequent point releases; whoever adds the dependency should check for a newer `19.25.x` build for this
  exact Minecraft version before pinning.
- **`RecipeType.create(...)`'s generic-class-token requirement** means every bespoke recipe class
  (`CrystallizerRecipe`, `ChemPlantRecipe`, etc.) needs to stay a concrete, JEI-visible class — none of
  today's bespoke classes are `private` nested classes with an inaccessible constructor, so this should
  be a non-issue, but it's worth a build-time check once compilation is possible.
- **SILEX's weighted-random flattening to equal-likelihood JEI display** is a real, if minor,
  information loss versus CE's actual weighted drop table (see table row) — worth a decision (documented
  as an open question below) rather than an accidental silent simplification.
- **The 3 `private`-map bespoke classes' new getters** (Crystallizer/Mixer/Refinery) are trivial, but
  each is a real production-code edit to an already-shipped, already-reviewed Phase 2 file — should go
  through the same review step as any other cross-cutting addition, not be waved through as "just
  research."

## Open questions

1. **Does this port want CE's real 3-way SILEX category split (by wavelength tier) or one category +
   a wavelength-icon overlay?** Both are equally valid against the real JEI API; this is a design call
   this report deliberately does not make, since CE (the source of truth) made the 3-way choice but a
   1-category design is not a behavior deviation, only a display one.
2. **Should HBM fluids get real `IIngredientType<FluidStack>` registration (CE's original 1.12.2-era
   approach, a `FluidNTM` custom ingredient type) instead of the item-slot workaround** both CE's and
   neo-edition's real plugins independently settled on? The item-slot approach is lower-risk and already
   has a real, working, version-matched precedent (neo-edition) plus an already-registered item
   (`ItemFluidIcon`) in this port — this report recommends it over inventing a new custom ingredient type
   for the fluid-adjacent categories, but flags the alternative exists.
3. **Should this port's own not-yet-existing anvil/blast-furnace/crucible items also get a JEI-catalyst
   registration reserved for them the moment they land**, or is that purely the responsibility of
   whichever future phase adds those machines? This report has no view either way; noted so it isn't
   silently forgotten.
4. **Item-subtype interpreters** (`ISubtypeRegistration`) — should this port's own RBMK rods/battery
   packs/etc. get the same "distinguish by data component, not just Item" treatment neo-edition's real
   plugin already applies? Not required for the recipe categories in this report's table to function
   correctly, but affects whether JEI's bookmark/search treats e.g. two different-enrichment rods as the
   same ingredient (probably undesirable) — a real design decision for whoever implements this area.
