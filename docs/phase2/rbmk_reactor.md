# RBMK reactor multiblock — Phase 2 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/rbmk/**/*.java` (34 files, ~8,900 lines
  — every column-type tile entity, both interfaces, `RBMKDials`, `RBMKColumn`)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/rbmk/**/*.java` (34 files, ~2,130 lines —
  `RBMKBase` and every concrete block subclass, surveyed by signature/override grep after the two
  files most load-bearing for multiblock shape and meltdown triggering — `RBMKBase.java`,
  `RBMKRod.java` — were read in full)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/neutron/*.java` (7 files — `NeutronStream`,
  `NeutronNode`, `NeutronNodeWorld`, `NeutronHandler`, `RBMKNeutronHandler`, full; `PileNeutronHandler`
  not read, out of scope — Chicago-pile content, not RBMK)
- `upstream/hbm-ce/src/main/java/com/hbm/items/machine/{ItemRBMKRod,ItemRBMKPellet}.java` (full)
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemDyatlov.java` (full — the only other
  `meltdown()` call site besides the two inside the RBMK package itself)
- `upstream/hbm-ce/processor/src/main/java/com/hbm/interfaces/AutoRegister.java` +
  `AutoRegisterProcessor.java` (to resolve what `@AutoRegister` — present on ~15 RBMK tile-entity
  classes — actually does)
- This port's own `src/main/java/com/hbm/{items/machine/ItemRBMKPellet.java,
  hazard/modifier/HazardModifierRBMK{Hot,Radiation}.java, hazard/HazardRegistry.java,
  config/GeneralConfig.java, blocks/generic/BlockRBMKSlab.java}`, plus repo-wide greps for
  `com.hbm.tileentity`/`com.hbm.blockentity`, `AbstractContainerMenu`, `com.hbm.api.fluid*`,
  `com.hbm.inventory.control_panel`, `com.hbm.entity`, `com.hbm.particle`, `GameRules`,
  `EntityRBMKDebris`, `pribris`/`corium_block`
- `docs/phase0/STATUS.md`, `docs/phase1/{items_tool.md (structural model), items_machine.md,
  DIGEST_REMAINDER.md, hazard_bindings_plan.md}`, and the three sibling Phase 2 reports already on
  disk: `docs/phase2/{blockentity_base.md, multiblock_framework.md, gui_framework.md}` — all three
  are load-bearing prerequisites for RBMK and are treated as authoritative, not re-derived, below.

## Headline finding

PORT_SPEC.md is right to flag this as one of the deepest logic packages, but the *shape* of the
depth is narrower than "big multiblock" suggests, and two premises in this task's own framing need
correcting before the split makes sense:

1. **The multiblock structure validation is not RBMK's problem to solve.** `RBMKBase extends
   BlockDummyable` and its `getDimensions(World)` returns `{RBMKDials.getColumnHeight(world), 0, 0,
   0, 0, 0}` — a pure 1×1×N vertical column (N = a gamerule-controlled 2–16, default 4), no
   horizontal footprint at all. `docs/phase2/multiblock_framework.md` already names `RBMKBase`
   explicitly as one of the abstract intermediate bases among its 149-file `extends BlockDummyable`
   survey. Every RBMK column block is a `BlockDummyable` dummy/core pair exactly like every other
   multiblock machine in the mod; RBMK contributes nothing new to *how* multiblocks are validated,
   placed, rotated, or orphan-cascaded. **This package's real dependency on the multiblock package is
   total but shallow**: it needs `MultiblockHandlerXR`/`IPersistentNBT`/`BlockDummyable` to exist (per
   that report's own Phase-2-safe scope), and in return it owns zero multiblock-framework logic of
   its own.
2. **The actual depth is in three custom subsystems that are genuinely this package's own**: (a) a
   bespoke neutron-flux propagation engine (`com.hbm.handler.neutron`, 7 classes, a node-graph +
   per-world-tick stream-cast simulation, not reused by anything outside RBMK/Chicago-pile content),
   (b) `ItemRBMKRod`'s reactivity/xenon/heat math (9 burn-curve shapes × 5 depletion-curve shapes,
   entirely static `double` functions over `ItemStack` NBT), and (c) `TileEntityRBMKBase`'s
   heat-diffusion/meltdown/debris-conversion state machine. These three are exactly the "pure-logic
   core" this task asked for fullest attention on, and they are detailed function-by-function below.
3. **Correcting the task's premise on `ItemRBMKRod`/`ItemRBMKPellet`**: only `ItemRBMKPellet` is
   actually ported (it's genuinely standalone — tooltip/creative-tab logic, zero RBMK-package
   imports, already sitting in `src/main/java/com/hbm/items/machine/ItemRBMKPellet.java`).
   **`ItemRBMKRod` does not exist in the port at all.** `docs/phase1/items_machine.md` explicitly
   chose option (a) — "defer the whole class alongside RBMK" — over splitting out its pure-NBT math,
   and no file was ever written. This is not a hypothetical gap: **two Phase-1 hazard-modifier
   classes already on disk import and call it today** —
   `src/main/java/com/hbm/hazard/modifier/HazardModifierRBMKRadiation.java` and
   `HazardModifierRBMKHot.java` both `import com.hbm.items.machine.ItemRBMKRod` and call
   `ItemRBMKRod.getEnrichment/getPoisonLevel/getHullHeat`, and `HazardModifierRBMKRadiation` is
   actively wired into `HazardRegistry.registerRBMK(...)` at `HazardRegistry.java:669`, which is
   itself called from live pellet-registration code (`HazardRegistry.java` comment at line 584-591
   already documents this exact forward reference by name). **This is a real, already-present compile
   break in the port**, in the same documented-forward-reference style as every other Phase 0/1 gap —
   just flagging that this Phase 2 package's very first, easiest, most test-friendly deliverable
   (`ItemRBMKRod`) is also unblocking code that already exists.
4. **`RBMKNeutronHandler` reaches directly into `ChunkRadiationManager`, a Phase 4 system**, in three
   places (`irradiateFromFlux` ×2, the unlidded-stream-target branch of `runStreamInteraction`), and
   `TileEntityRBMKRod.update()` calls it a fourth time directly for un-lidded core irradiation.
   `docs/phase0/STATUS.md` lists `com.hbm.handler.radiation.ChunkRadiationManager` under "Phase 4
   world/simulation systems," not Phase 2. This is a real cross-phase coupling worth flagging loudly
   (see Deferred scope / Open questions) — it is small in code size (4 call sites) but means the flux
   engine cannot be *fully* wired without either a Phase 4 dependency or a Phase-2-local no-op stub.

## Suggested Phase 2 work-package split

This package is too large for one work package — 34 tile-entity files + 32 block files + the neutron
engine + the item math, spanning three genuinely different dependency profiles. Recommended 3-way
split, in build order:

### Package A — Core reactor logic (do this first; this is PORT_SPEC's "deepest logic," and the
future unit-test target)
`RBMKDials`, `RBMKColumn` (+ nested column-data classes), `IRBMKFluxReceiver` (+ `NType`),
`IRBMKLoadable`, `ItemRBMKRod`, the full `com.hbm.handler.neutron` package, `TileEntityRBMKBase`,
`TileEntityRBMKActiveBase`, `TileEntityRBMKRod`, `TileEntityRBMKModerator`, `TileEntityRBMKAbsorber`,
`TileEntityRBMKReflector`, `TileEntityRBMKBlank`, `TileEntityRBMKControl`(+`Manual`+`Auto`),
`RBMKBase` (block) and the non-fluid concrete column blocks (`RBMKRod`, `RBMKRodReaSim`,
`RBMKModerator`, `RBMKAbsorber`, `RBMKReflector`, `RBMKBlank`, `RBMKControl`+`Auto`). This is the
entire flux/heat/meltdown simulation and is buildable against only the two already-scoped sibling
packages (multiblock framework, block-entity base) plus small stubs (see Deferred scope). It has
**no dependency on fluid tanks, recipes, or the GUI framework** — every class in this bucket either
has no player-facing GUI (moderator/absorber/reflector/blank) or its GUI can be stubbed/deferred
without touching the simulation math (rod/control loading UI).

### Package B — Fluid-handling columns and control-panel peripherals (sequence after Package A, and
after the fluid-tank + GUI-framework prerequisites land)
`TileEntityRBMKBoiler`, `TileEntityRBMKOutgasser`, `TileEntityRBMKCooler`, `TileEntityRBMKHeater`,
`TileEntityRBMKInlet`, `TileEntityRBMKOutlet`, `TileEntityRBMKAutoloader`, `TileEntityRBMKStorage`,
`TileEntityRBMKConsole`, `TileEntityRBMKTerminal`, `TileEntityRBMKKeyPad`, `TileEntityRBMKLever`,
`TileEntityRBMKIndicator`, `TileEntityRBMKDisplay`, `TileEntityRBMKGauge`, `TileEntityRBMKNumitron`,
`TileEntityRBMKGraph`, `TileEntityRBMKCraneConsole`, plus their paired blocks and
`RBMKMiniPanelBase`/`RBMKPipedBase`. Every one of these needs at least one of: `FluidTankNTM` (does
not exist anywhere in the port), the Menu/Screen GUI framework (confirmed absent by
`docs/phase2/gui_framework.md`), `OutgasserRecipes`/`RecipesCommon` (confirmed absent), or the full
`com.hbm.inventory.control_panel` network (29 CE files, 5,349 lines, zero ported — see Deferred
scope). None of that blocks Package A; Package A does not need Package B's column types to be
present in the world for the flux engine or meltdown state machine to be correct.

### Package C — Meltdown-and-fallout integration (sequence whenever Phase 4's radiation/particle/
entity infrastructure lands, even if that's after other Phase 2 work)
Wiring `TileEntityRBMKBase.meltdown()`/`onMelt()`/`onOverheat()` to real byproducts: `EntityRBMKDebris`
(does not exist — `com.hbm.entity` package is entirely absent from the port), the four
`RBMKDebris*`/`pribris*` blocks (not ported), `ModBlocks.corium_block` (a **world-placed fluid
block** — `CoriumFinite extends BlockFluidClassic`-family — not ported; see Open questions, this is
the one place RBMK collides with Phase 1's "no world-fluid-block system exists yet" finding),
`EntitySpear` (digamma event), `AdvancementManager.achRBMKBoom` (not ported),
`ChunkRadiationManager` (Phase 4, see Headline finding #4), and the particle/packet VFX
(`HbmEffectNT.RBMKMush`/`RBMKSteam`, `AuxParticlePacketNT`, `PacketThreading`). Package A can and
should still implement the *math* of `onMelt`/`standardMelt`/`meltdown()` (which columns convert to
which debris tier, the BFS flood-fill of connected columns, the min-distance-from-edge reduce
factor) with the byproduct block/entity references left as documented forward references, exactly
like every other cross-phase gap in this port — Package C is what makes a meltdown actually visible
and radioactive in the world.

## Phase-2-safe scope (Package A detail)

All class/line counts below are from the CE files actually read.

| Class | Lines | Portability |
|---|---|---|
| `IRBMKFluxReceiver` (+ nested `NType` enum: `FAST`/`SLOW`/`ANY`) | 19 | Zero real dependency beyond `NeutronStream`'s own type — trivially portable once that exists. **This is the exact interface/enum shape `ItemRBMKRod`/`TileEntityRBMKRod` need**, confirming the task's premise that reading it would answer the API-shape question. |
| `IRBMKLoadable` | 33 | Zero dependency (`ItemStack` only). Trivially portable. |
| `RBMKDials` | 387 | Static `World → double/int/boolean` accessor over ~26 world gamerules (`dialPassiveCooling`, `dialColumnHeight`, `dialReactivityMod`, `dialFluxRange`, `dialModeratorEfficiency`, `dialDisableXenon`, etc., full enum `RBMKKeys` in file). Every method is a pure clamp/parse over a string gamerule read — genuinely the most unit-test-friendly file in the package **except** that every accessor takes a live `World` (see Open questions on `GameRules` API shape). |
| `RBMKColumn` (+ `StandardColumn`/`FuelColumn`/`BoilerColumn`/`ControlColumn`/`CoolerColumn`/`OutgasserColumn`/`HeaterColumn`, `ColumnType` enum) | 373 | Pure DTO + `ByteBuf` (de)serialization for the console's remote display of column state. `ColumnType` enum (14 values) is the canonical list of "what a column can be." No TE coupling beyond the enum. |
| `ItemRBMKRod` | 599 | **The centerpiece of the pure-logic core** (detailed below). Only two forward references: `RBMKDials` (gamerule reads) and `IRBMKFluxReceiver.NType`. Once those exist, this class is 100% portable and unblocks the two hazard modifiers already on disk (see Headline finding #3). |
| `com.hbm.handler.neutron.*` (`NeutronStream`, `NeutronNode`, `NeutronNodeWorld`, `NeutronHandler`, `RBMKNeutronHandler`) | ~900 combined | The flux-propagation engine (detailed below). Depends on `TileEntityRBMKBase`'s abstract surface (`getRBMKType()`, `hasLid()`, `isModerated()`), `RBMKBase` (block, only for an `instanceof` check and a *world block-opacity* query — not multiblock validation), and 4 `ChunkRadiationManager` call sites (Phase 4, stub-able). |
| `TileEntityRBMKBase` (abstract) | 710 | Heat diffusion, passive cooling, meltdown state machine (detailed below). Real forward references beyond the multiblock/block-entity-base packages: `ModBlocks.pribris*`/`corium_block` (Package C), `EntityRBMKDebris`/`EntitySpear` (Package C), `FluidNetMK2`/`IFluidReceiverMK2` (overpressure-only, `fluidmk2` package, not yet ported anywhere), `ControlEventSystem`/`IControllable`/`DataValue` (`com.hbm.inventory.control_panel`, see Deferred scope — used unconditionally in `validate()`/`invalidate()`, not optional), `AdvancementManager`, `HBMSoundHandler` (already exists), particle/packet plumbing. |
| `TileEntityRBMKActiveBase` (abstract) | 16 | Trivial — one `isUseableByPlayer` distance check. |
| `TileEntityRBMKRod` | 579 | The fuel-rod flux-receive/burn/spread cycle (detailed below). |
| `TileEntityRBMKRodReaSim` | 58 | `extends TileEntityRBMKRod` — the "ReaSim" (realistic-simulation) variant with a wider flux range and multi-neutron output; same shape, different constants. |
| `TileEntityRBMKModerator` / `Absorber` / `Reflector` | 33 / 33 / 32 | Trivial pass-through columns — each is just `onMelt()` (debris type/count) + `getRBMKType()` + `getConsoleType()` overrides on top of the base. All physics for these three lives in `RBMKNeutronHandler.RBMKNeutronStream.runStreamInteraction` (moderation multiplies `fluxRatio`, absorption multiplies `fluxQuantity` + adds heat, reflection bounces the stream back into the originating rod), not in these classes themselves. |
| `TileEntityRBMKBlank` | 26 | Trivial no-op column (a structural filler block in the reactor grid). |
| `TileEntityRBMKControl` (abstract) | 256 | Control-rod extraction-level movement (`level`→`targetLevel` at fixed `speed`, modified by `RBMKDials.getControlSpeed`), REASIM-powered variants via `IEnergyReceiverMK2` (already ported in `com.hbm.api.energymk2`, confirmed real usage here). |
| `TileEntityRBMKControlManual` | 279 | Adds the operator-facing `RBMKColor` enum (`RED`/`YELLOW`/`GREEN`/`BLUE`/`PURPLE`, nested here in CE — many other files reference it as `TileEntityRBMKControlManual.RBMKColor`, recommend keeping that nesting to avoid unnecessary churn) and the **power-surge formula** on rod withdrawal (detailed below). |
| `TileEntityRBMKControlAuto` | 196 | Heat-setpoint auto-control: three interpolation shapes (`LINEAR`, `QUAD_UP`, `QUAD_DOWN`) mapping `heat ∈ [heatLower, heatUpper]` to a target extraction level. Pure math, no fluid/GUI-framework coupling beyond its own settings container (`Container`/`GuiScreen`, deferrable). |

### The neutron flux engine (`com.hbm.handler.neutron`) — read in full

- **`NeutronStream`** (abstract, 79 lines): a directional flux packet with `fluxQuantity` (double,
  "how much"), `fluxRatio` (double `[0,1]`, "how fast/slow" — 1 = all fast, 0 = all slow — this is
  the fast/slow neutron split, not a percentage of something else), an origin `NeutronNode`, and a
  `Vec3d` direction. Its constructor **self-registers** into
  `NeutronNodeWorld.getOrAddWorld(world).addStream(this)` — streams are create-and-forget, consumed
  once per world tick by `NeutronHandler.onServerTick()`. `getBlocks(int range)` walks an
  `Iterator<BlockPos>` outward along the vector one block at a time (rounds `0.5 + vector*i` to an
  int — this is how a diagonal-looking direction still walks an axis-aligned column of blocks,
  though in practice RBMK only ever uses the 4 cardinal `Vec3d`s).
- **`RBMKNeutronHandler`** (421 lines) is where the actual per-column-type interaction logic lives —
  not in the individual TE classes:
  - `RBMKType` enum: `ROD`, `MODERATOR`, `CONTROL_ROD`, `REFLECTOR`, `ABSORBER`, `OUTGASSER`, `OTHER`
    (`OTHER` = "don't bother with neutron calculations on this, it can't change anything" — cooler,
    heater, boiler, storage, blank all fall through to this default from `TileEntityRBMKBase`).
  - `RBMKNeutronNode` wraps a `TileEntityRBMKBase` + cached `hasLid`/`type` for the node-cache
    (`checkNode` decides whether a node can be safely evicted from the 20-tick cache-refresh pass in
    `NeutronNodeWorld.cleanNodes()` — a rod with no fuel or zero last flux, or no fuel rod anywhere
    within `fluxRange` of a non-rod column, gets uncached).
  - **`RBMKNeutronStream.runStreamInteraction`** is the actual per-tick physics dispatch, walking
    outward from the origin rod along one cardinal direction up to `fluxRange` blocks (a world
    gamerule, default 5): for each target column it (a) irradiates the surrounding chunk via
    `ChunkRadiationManager` if the column has no lid, (b) applies moderation
    (`fluxRatio *= (1 - moderatorEfficiency)`) if the target is a moderator *or* the target column
    itself reports `isModerated()` (fuel/control columns can themselves be built with a moderated
    block variant — see `RBMKRod.moderated`/`RBMKControl` equivalents), (c) dispatches by
    `RBMKType`: `ROD` → `rod.receiveFlux(this)` and the stream terminates; `OUTGASSER` → same,
    conditional on `canProcess()`; `CONTROL_ROD` → multiplies `fluxQuantity` by `rod.getMult()`
    (0 fully blocks, continues the loop otherwise) or terminates if `level <= 0`; `REFLECTOR` →
    re-moderates by however many moderation events already happened along this path, then bounces
    the (possibly `reflectorEfficiency`-reduced) stream straight back into the *originating* rod;
    `ABSORBER` → adds `RBMKDials.getAbsorberHeatConversion(world) * fluxQuantity` heat to the
    absorber column and reduces `fluxQuantity` by `absorberEfficiency`. If the stream reaches
    `fluxRange` with no rod ever catching it, it's not silently dropped — it irradiates whatever
    block is at the final position (a raw "wasted flux escaped into the world" radiation event), and
    a special case (`#1933` regression fix, comment preserved in CE) re-checks whether the block
    *directly after* a control rod is itself a real RBMK column before falling back to a bare
    world-position irradiation.
  - `getHits(BlockPos)` counts opaque-cube blocks stacked through the column's full height (comment
    preserved verbatim in CE: "total count of bugs fixed attributed to this function: 14" — this is
    a genuinely fragile piece of world-shape-dependent logic, worth extra scrutiny/tests when
    ported) — used to partially attenuate a stream that hits a non-RBMK obstruction instead of a
    proper column.
  - Static fields (`moderatorEfficiency`, `reflectorEfficiency`, `absorberEfficiency`,
    `columnHeight`, `fluxRange`) are refreshed once per server tick in `NeutronHandler.onServerTick()`
    from the current world's gamerules and shared mutable state across the whole handler class — **not
    thread-safe by construction, and not per-world** (a bug already present in CE if multiple worlds
    have different dial values and run in the same tick loop; CE's own comment flags per-world
    parallelism as a TODO). Worth deciding explicitly whether the port preserves this exact
    (slightly buggy) shared-static-state shape or fixes it, since "preserve CE behavior" and "this is
    a bug" are in tension here — flagged, not resolved, in Open questions.
- **`NeutronNodeWorld`**: one `StreamWorld` (stream list + node cache) per `World`, GC'd when empty.
  `runStreamInteractions` just iterates and calls each stream's `runStreamInteraction`, then
  `removeAllStreams()` — the whole cache is consumed once per tick and rebuilt as rods re-spread flux
  in their own `update()`.

### `ItemRBMKRod`'s reactivity/xenon/heat math — read in full, this is the unit-test target

Every method below is a pure function over `(World, ItemStack)` or plain doubles — `World` is only
used for `RBMKDials` gamerule multipliers, never for anything that isn't trivially fakeable. This is
exactly the "pure-logic core" a later unit-testing phase will want:

- **`burn(World, ItemStack, inFlux)` → outFlux** (the whole per-tick fission cycle, in exact order):
  1. `inFlux += selfRate` (self-igniting fuels, e.g. spontaneous-fission sources, `selfRate = 0` for
     normal fuel).
  2. If xenon enabled (`RBMKDials.getXenon`): burn off `xenonBurnFunc(inFlux) = inFlux² / xBurn`
     (quadratic — burns off faster at high flux), then attenuate `inFlux *= (1 - poisonLevel)`
     (poison directly reduces effective flux — the actual poisoning effect), then generate
     `xenonGenFunc(inFlux) = inFlux * xGen` (linear, using the *already-attenuated* flux), clamp
     `xenon ∈ [0, 100]`.
  3. Compute a heat-coefficient reactivity multiplier `mult` (starts at 1; if `heatCoeffStart != 0`
     and core heat has crossed it, `mult` ramps down via a half-sine
     `sin((prog·π + π)/2)` from 1 to a minimum as `coreHeat` moves from `heatCoeffStart` to
     `heatCoeffStart + heatCoeffLength` — this is a "this fuel type de-rates itself at high
     temperature" self-limiting curve, used by some fuels, off by default).
  4. `outFlux = reactivityFunc(inFlux, enrichment * mult) * RBMKDials.getReactivityMod(world)`.
  5. If depletion enabled: `yield -= inFlux` (clamped ≥ 0) — **note depletion consumes the
     *pre-poison* `inFlux` from step 1, not the post-poison value from step 2 or the `outFlux` from
     step 4** — an easy-to-invert-by-accident detail worth a dedicated unit test.
  6. `coreHeat += outFlux * heat` (the `heat` field is "°C generated per unit of outFlux," a
     per-fuel-type constant), then `rectify()`s (clamp `[20, 1_000_000]`, NaN→20) and stores it.
- **`reactivityFunc(in, enrichment)`**: `flux = in * reactivityModByEnrichment(enrichment)`, then one
  of **9** shapes selected by `EnumBurnFunc` (`PASSIVE`, `LOG_TEN`, `PLATEU`, `ARCH`, `SIGMOID`,
  `SQUARE_ROOT`, `LINEAR`, `QUADRATIC`, `EXPERIMENTAL` — exact formulas preserved in the file, e.g.
  `LOG_TEN → log10(flux+1) * 0.5 * reactivity`, `SIGMOID → reactivity / (1 + e^(-(flux-50)/10))`).
  Each fuel type (across the port's ~30+ fuel-rod registry entries, not read in this survey) picks
  one shape + a `reactivity` endpoint constant.
- **`reactivityModByEnrichment(enrichment)`**: one of **5** shapes selected by `EnumDepleteFunc`
  (`LINEAR`, `STATIC`, `BOOSTED_SLOPE`, `RAISING_SLOPE`, `GENTLE_SLOPE`) — the non-linear ones are
  literally "breeding" curves that produce **more** reactivity than raw enrichment would suggest at
  partial depletion (e.g. `BOOSTED_SLOPE`'s comment: "maximum of 132% at 64% depletion") — this is
  intentional CE game-design (breeder fuels get *better* partway through their life), not a bug, and
  must survive the port exactly.
- **`updateHeat`**: core↔hull heat equalization, `mid = (core - hull)/2`, both move toward each other
  by `mid * diffusion * RBMKDials.getFuelDiffusionMod(world) * mod`. **`provideHeat`**: hull→column
  heat transfer, **with an inline meltdown short-circuit** — if `hullHeat > meltingPoint`, core/hull/
  column heat are instantly averaged three ways and the delta is returned as component heat (this
  happens *inside the item's own logic*, before `TileEntityRBMKRod.update()`'s own
  `heat > maxHeat()` check ever fires — there are effectively two independent overheat thresholds in
  play: the fuel item's own `meltingPoint` (per fuel type, e.g. 1000°C default) which triggers this
  internal equalization, and the column's fixed `maxHeat() = 1500°C` in `TileEntityRBMKBase` which
  triggers the real `meltdown()`/`onMelt()` call in `TileEntityRBMKRod.update()`). Getting this
  two-threshold interaction right (not collapsing it into one check) is important for parity.
- Getters/setters (`getYield`/`setYield`, `getPoison`/`setPoison`, `getCoreHeat`/`setCoreHeat`,
  `getHullHeat`/`setHullHeat`, `getEnrichment = yield/maxYield`, `getPoisonLevel = poison/100`) are
  all plain NBT double reads/writes with lazy-init defaults (`setNBTDefaults` on first touch) — direct
  Data Component migration candidates, already flagged as such in `docs/phase1/items_machine.md`'s
  NBT-key list (`yield`/`xenon`/`core`/`hull`).

### `TileEntityRBMKBase`'s heat/meltdown state machine — read in full

- **`moveHeat()`** (called every server tick from `update()`): sums this column's heat (+ ReaSim
  water/steam if that dial is on) with its 4 cardinal `TileEntityRBMKBase` neighbors (a
  per-instance 4-slot lazy neighbor cache, invalidated on `isInvalid()`), computes the group average,
  and moves every member (including self) a `RBMKDials.getColumnHeatFlow(world)` fraction of the way
  toward that average — i.e. **exponential heat equalization across the horizontal grid**, not a
  literal conduction/diffusion PDE. Then `coolPassively(neighborCount)` subtracts a passive-cooling
  constant that scales *down* as more neighbors are present (`passiveCooling(int neighbors)`
  interpolates between an "inner" minimum (surrounded, well-insulated) and "outer" maximum (isolated,
  radiates freely) — `min + (max-min) * (4 - clamp(neighbors,0,4))/4`), floored at 20°C (ambient).
- **`onOverheat()`**: a currently-dead-simple fallback (places lava in the 4 blocks above the core) —
  grep found no live caller of this method anywhere in CE outside its own declaration; likely legacy/
  unused code path, worth double-checking at implementation time rather than assuming it's load-bearing.
- **`meltdown()`** (`!world.isRemote` only, the real event): BFS flood-fills every orthogonally-
  connected `TileEntityRBMKBase` from the trigger column (`getFF`, iterative with a 50,000-node
  safety cap to avoid a server freeze on a pathological world-edited mega-structure), computes the
  bounding box of the affected footprint, then for every column computes `minDist` = its distance to
  the *nearest edge* of that bounding box and calls `onMelt(minDist + 1)` — **columns near the edge
  of the meltdown convert to fewer debris layers than columns in the center**, which is the actual
  "meltdown crater" shape logic. Separately handles: extra-radiating corium-adjacency upgrades (a
  1-in-3 chance per adjacent debris block within a 3×3×3 to upgrade `pribris`/`pribris_burning` to
  `pribris_radiating`, or `pribris_digamma` if `RBMKBase.digamma` was set), an optional
  "overpressure" pass (gamerule-gated) that vaporizes/explodes every fluid pipe and receiver
  transitively connected to any boiler caught in the meltdown, particle/sound/achievement side
  effects, and a `EntitySpear` "digamma" projectile spawn for the special digamma-fuel case.
- **`standardMelt(reduce)`** / **`TileEntityRBMKRod.onMelt(reduce)`**: `reduce` (clamped to
  `[1, columnHeight]`, with a 1-in-3 chance to bump it by one more) is "how many blocks from the top
  of the column convert to plain rubble (`pribris`) vs. air," with the single block just below that
  cutoff becoming `pribris_burning` if more than one layer converts. **Fuel-rod columns are the
  special case**: if the rod's item is a real `ItemRBMKRod`, the whole column becomes
  `corium_block` (not `pribris`) up to the cutoff, the digamma flag is set if the specific waste-fuel
  item was loaded, and 1–columnHeight `DebrisType.FUEL` entities are spawned as flying loot.
  Moderated columns additionally spawn 2-3 `DebrisType.GRAPHITE` entities regardless of fuel-rod
  status. This is the byte-for-byte behavior Package C needs to preserve once its byproduct blocks
  exist; Package A can and should port this method's *decision logic* now with the block/entity
  targets as documented forward references.

### Control-rod surge math (`TileEntityRBMKControlManual.getMult()`)

```java
if (targetLevel < startingLevel && |level - targetLevel| > 0.01) {
    surge = sin(pow(1 - level, 15) * PI) * (startingLevel - targetLevel) * RBMKDials.getSurgeMod(world);
}
return level + surge;
```
This is the Chernobyl-reference "positive void/scram coefficient" effect: **withdrawing** a control
rod that was previously inserted (`targetLevel < startingLevel`) produces a transient *extra* flux
multiplier spike (`surge`, on top of `level` itself) that only matters while `level` is still close
to its old (higher) value — `pow(1-level, 15)` makes the `sin` argument swing from near-0 to near-π
almost entirely in the last few percent of `level`'s travel, so the surge appears as a sharp pulse
right as the rod starts moving, then vanishes. This is one of the highest-value functions to
unit-test given the project's own framing (RBMK meltdown realism is a headline feature of this mod).

## Deferred scope

Real dependencies of *concrete* RBMK content, not of Package A's core simulation:

- **`com.hbm.inventory.fluid.tank.FluidTankNTM`** — confirmed absent (also flagged by
  `docs/phase2/blockentity_base.md`). Needed by `TileEntityRBMKBoiler`, `Outgasser`, `Cooler`,
  `Heater`. Package B blocker.
- **`com.hbm.api.fluid.{IFluidStandardReceiver,Sender,Transceiver}`** (legacy fluid-network
  interfaces) — confirmed **completely absent** from the port (this is a different, older interface
  family than `com.hbm.api.fluidmk2`, which itself only has `IFluidRegisterListener` ported so far).
  `TileEntityRBMKBoiler`/`Outgasser`/`Inlet`/`Outlet` all implement one of these three. Package B
  blocker, and worth flagging to whoever owns the fluid-network prerequisite package generally (not
  RBMK-specific — this interface family is used by ~112 CE tile entities per
  `docs/phase2/blockentity_base.md`'s own count).
- **`com.hbm.api.fluidmk2.{FluidNode,FluidNetMK2,IFluidReceiverMK2}`** — confirmed absent except the
  one interface already ported. Needed only for the *overpressure* branch of `meltdown()` (pipe/
  receiver destruction) — the core meltdown state machine (byproduct conversion) does not need this.
  Package C, narrow scope.
- **`com.hbm.inventory.recipes.OutgasserRecipes` / `com.hbm.inventory.RecipesCommon`** — confirmed
  absent (already flagged cross-cutting in `docs/phase0/STATUS.md`/`docs/phase1/STATUS.md`).
  `TileEntityRBMKOutgasser.canProcess()`/`process()` are hardcoded against this lookup. Package B
  blocker, not re-solved here per the task's own instruction.
- **The Menu/Screen GUI framework** — confirmed absent (`docs/phase2/gui_framework.md`, zero real
  `AbstractContainerMenu`/`extends Screen` hits repo-wide). Every peripheral/panel TE and every
  fluid-handling column TE implements CE's `IGUIProvider` (`provideContainer`+`provideGUI`) for a
  player-facing GUI (`ContainerRBMKRod`/`GUIRBMKRod`, `ContainerRBMKBoiler`/`GUIRBMKBoiler`, etc. —
  not read in this survey, out of RBMK's own scope, they belong to whichever package builds the GUI
  framework). **Package A's fuel-rod/control-rod loading UIs also need this** — flagged as a real
  Package A dependency too, not just Package B, though the *simulation* logic in Package A does not
  depend on it (a rod can be "loaded" via `IRBMKLoadable.load()` called by non-GUI code, e.g. an
  autoloader or a crane, with zero GUI involved).
- **`com.hbm.inventory.control_panel.*`** (29 CE files, 5,349 lines: `ControlEventSystem`,
  `IControllable`, `IControlReceiver`, `DataValue`/`DataValueFloat`/`DataValueString`, `ControlEvent`,
  etc.) — **confirmed entirely absent from the port.** This is the single largest concrete gap this
  survey found. It is not optional glue: `TileEntityRBMKBase.validate()`/`invalidate()`
  unconditionally call `ControlEventSystem.get(world).addControllable(this)`/`removeControllable(this)`,
  and `getQueryData()` returns `Map<String, DataValue>` — meaning **every single RBMK column TE**,
  including the trivial ones (moderator/absorber/reflector/blank), cannot compile as CE wrote it
  without this package existing. Recommend Package A ship with a minimal no-op
  `IControllable`/`ControlEventSystem` stub (satisfying the interface contract, `addControllable`/
  `removeControllable` as no-ops, `getQueryData` wired but unconsumed) so the simulation compiles and
  runs correctly standalone, and let Package B (which owns the panel TEs that are the *actual*
  consumers of this network — `IControlReceiver`, redstone-over-radio-style remote monitoring/control)
  do the real port of this package. This is a design call, not a re-derivation of the multiblock
  report's job — flagging it because it changes Package A's shape (needs a stub) if not decided.
- **World-fluid blocks** — per Phase 1's own research (referenced by this task's instructions), this
  port has no world-placed fluid-block system at all. **RBMK's `corium_block` is the one place in
  this survey where that gap becomes directly relevant**: CE's `corium_block` is a
  `CoriumFinite`(`extends BlockFluidClassic`-family) world fluid block that meltdown literally paints
  into the world. This is Package C scope, but it means Package C cannot be "just" an RBMK task —
  it needs whichever package first builds a world-fluid-block story to exist first, or needs its own
  narrow one-off decision (e.g. render corium as a solid/static block instead of a flowing fluid for
  now) — flagged as a real design fork, not resolved here.
- **`ChunkRadiationManager`** (Phase 4 per `docs/phase0/STATUS.md`) — 4 call sites inside the neutron
  engine + fuel-rod update loop (Headline finding #4). Recommend Package A stub this the same way as
  `ControlEventSystem` (a no-op or simple accumulator that a Phase 4 package later replaces) so the
  flux math is fully testable without waiting on Phase 4's real radiation simulation.
- **`com.hbm.entity.projectile.EntityRBMKDebris`, `com.hbm.entity.effect.EntitySpear`** — the entire
  `com.hbm.entity` package tree is absent from the port (confirmed by grep — no directory exists).
  Package C.
- **`com.hbm.main.AdvancementManager`, `com.hbm.particle.*`, `com.hbm.util.ParticleUtil`,
  `com.hbm.util.ContaminationUtil`** — all confirmed absent. `ContaminationUtil` is used by
  `TileEntityRBMKOutgasser.receiveFlux` (activates radiation contamination on an inserted item when
  the outgasser has nothing to process). Package C (advancement/particle) / Package B
  (`ContaminationUtil`, a narrower single-call dependency, might land sooner as part of the general
  hazard/contamination system rather than waiting on all of Package C).
- **The 149-file `BlockDummyable`/`MultiblockHandlerXR`/`IPersistentNBT` prerequisite** — already
  fully scoped by `docs/phase2/multiblock_framework.md`. Not re-derived here; RBMK simply consumes it
  once it lands (see Headline finding #1). Do not let this package re-decide the
  `com.hbm.tileentity` vs `com.hbm.blockentity` package-naming call — that report already makes it
  once for all of Phase 2.
- **A base "machine" `BlockEntity` class** — already fully scoped by `docs/phase2/blockentity_base.md`.
  RBMK's column TEs map onto that report's `LoadedBaseBlockEntity`/`MachineBaseBlockEntity` split
  cleanly (moderator/absorber/reflector/blank/rod/control need only the loaded-base tier plus a tick
  hook; boiler/outgasser/cooler/heater/autoloader/storage need the full inventoried-machine tier).
  Not re-derived here.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior, this port's own committed code and
the sibling Phase 2 reports for NeoForge API shape — no NeoForge API is invented below):

- **`@AutoRegister` on ~15 RBMK tile-entity classes is CE-only build tooling, not something to
  port.** It's a compile-time annotation processor living in a *separate Gradle module*
  (`hbm-ce/processor/`, `com.hbm.processor.AutoRegisterProcessor`, `@SupportedAnnotationTypes`
  targeting `com.hbm.interfaces.AutoRegister`) that generates Forge-1.12
  `GameRegistry.registerTileEntity(...)` calls from the annotation at CE's own compile time. It has
  no NeoForge equivalent need — this port's real registration mechanism
  (`ModBlocks.BLOCK_ENTITY_TYPES.register(...)`, confirmed live and already used four times per
  `docs/phase2/blockentity_base.md`) is manual and explicit per `BlockEntityType`. Do not port the
  annotation, the processor module, or try to preserve `@AutoRegister` on the ported classes — just
  write the explicit registration call each concrete RBMK TE needs, exactly like the four existing
  ad hoc block entities and the pattern `blockentity_base.md` specifies.
- **`RBMKColor`'s CE nesting inside `TileEntityRBMKControlManual`** should be preserved as-is
  (referenced elsewhere as `TileEntityRBMKControlManual.RBMKColor`) — purely a naming-stability call,
  not an API-shape one, flagged so it isn't "fixed" into a top-level enum unnecessarily during
  the port.
- **`BlockEntityType`/`DeferredRegister` registration, NBT via `saveAdditional`/`loadAdditional`,
  per-`BlockEntityType` capability registration, and the `ITickableBE`-marker-interface ticking
  convention** are all already confirmed and specified by `docs/phase2/blockentity_base.md` — RBMK's
  column TEs should follow that report's shapes exactly rather than this package re-deriving them.
  Concretely: `TileEntityRBMKBase`'s abstract tier maps onto that report's
  `LoadedBaseBlockEntity`-equivalent (it is not inventoried at the base-class level — only
  `TileEntityRBMKSlottedBase` subclasses, e.g. the rod, are); `TileEntityRBMKSlottedBase`'s
  `RBMKSlottedItemStackHandler` (an `ItemStackHandler` inner class with per-slot
  `canInsertItem`/`canExtractItem`/`isItemValidForSlot` hooks and an "unchecked" bypass pair used
  internally by e.g. the outgasser's own recipe-output insertion) maps onto that report's
  `MachineBaseBlockEntity`-equivalent inventory story directly — no new inventory abstraction needed.
- **Multiblock shape input is a single integer**: `RBMKBase.getDimensions(World)` returns
  `{RBMKDials.getColumnHeight(world) /* = gamerule value - 1 */, 0, 0, 0, 0, 0}` — confirmed this is
  the *only* RBMK-specific parameter the multiblock framework's `checkSpace`/`fillSpace` (per
  `docs/phase2/multiblock_framework.md`'s already-confirmed signatures) needs from this package.
  RBMK does not need bespoke multiblock code of its own.

## Open questions / risks

- **`com.hbm.tileentity` vs `com.hbm.blockentity` package naming.** This affects RBMK directly (all
  34 tile-entity files) exactly as much as every other Phase 2 package with block entities.
  `docs/phase0/STATUS.md` flagged it as needing resolution "before Phase 2 block entities land";
  `docs/phase2/multiblock_framework.md` and `blockentity_base.md` both independently re-flag it and
  both recommend (A) preserve `com.hbm.tileentity`. This report adds no new information on the
  question itself, just confirms RBMK is one more (large) package waiting on the same unresolved
  call — **the highest-priority blocking decision across all of Phase 2's research so far, now
  flagged identically by three independent surveys.**
- **`RBMKDials`'s `World`-gamerule-backed design has zero precedent anywhere in this port or in Neo
  Edition.** A repo-wide grep for `GameRules`/`RegisterGameRulesEvent` in this port's `src/` returns
  nothing, and Neo Edition has no `RBMKDials` file to cross-check either. CE's shape is
  1.12-vanilla-stringly-typed (`world.getGameRules().getString(key)`, hand-parsed to
  double/int/boolean per accessor). NeoForge 1.21.1's real `GameRules` API is typed
  (`GameRules.Key<T>`/`GameRules.register(...)`) and — not confirmed by any file in this repo, so not
  asserted as fact here — may not even ship a built-in floating-point gamerule value type the way
  CE's ~15 double-valued dials would need, meaning a straight port could require a custom
  `GameRules.Type<DoubleValue>`. **Real alternative worth raising explicitly**: this port already has
  a fully-wired TOML config system (`GeneralConfig`, confirmed in Phase 0) with zero new
  infrastructure needed, and every one of RBMK's ~26 dials is a server-operator tunable in both CE's
  design and in spirit — nothing in the gameplay requires them to be *per-world* gamerules rather
  than *per-server* config values. Recommend whoever implements Package A make this call explicitly
  (gamerules for CE fidelity + eventual per-world `/gamerule` command support, vs. TOML config for
  zero new infrastructure and consistency with the rest of the port) rather than it being decided
  implicitly by whichever is easier to write first.
- **`RBMKNeutronHandler`'s shared mutable static state (`moderatorEfficiency`/`fluxRange`/etc.) is not
  per-world**, and CE's own in-file comment flags "TODO: per-world parallelism" as unaddressed. If two
  worlds with different dial values are simulated in the same tick (or if per-world parallelism is
  ever added), this is a latent correctness bug already present in CE. Decide explicitly whether to
  preserve this exact (buggy-but-authentic) shape or fix it — either is defensible, but it should be
  a stated decision given this package's own emphasis on future unit-testing (a per-world-correct
  version is straightforwardly more testable in isolation).
- **`onOverheat()` appears to have no live caller anywhere in CE** (grep found only its own
  declaration in `TileEntityRBMKBase`). Worth a second check at implementation time before assuming
  it needs porting as active behavior versus dead/legacy code.
- **Two independent overheat thresholds interact** (`ItemRBMKRod.meltingPoint`'s inline
  `provideHeat` equalization vs. `TileEntityRBMKBase.maxHeat() = 1500°C`'s real `meltdown()` trigger
  in `TileEntityRBMKRod.update()`) — flagged above in the pure-logic section as an easy place to
  accidentally collapse two thresholds into one during the port; call out explicitly in Package A's
  own review pass.
- **`getHits()`'s "14 bugs fixed" comment** (verbatim in CE) is this survey's strongest signal that
  the block-opacity-counting logic along a stream's path is unusually fragile/history-laden. Flag
  for extra test coverage once the pure-logic core is unit-testable, rather than assuming a
  straightforward one-time port is sufficient.
- **Fast/slow neutron-type semantics are easy to invert.** `NType.SLOW`/`FAST`/`ANY` (in
  `IRBMKFluxReceiver`) describes what a *fuel* splits with; `fluxRatio` on a `NeutronStream`
  describes what fraction of the *current stream* is fast (1.0) vs slow (0.0); and
  `TileEntityRBMKRod.fluxFromType(NType)` computes an *efficiency-weighted* input flux from those two
  independent axes (`SLOW` fuel: full efficiency on slow flux, 50% on fast; `FAST` fuel: full on
  fast, 30% on slow; `ANY`: no weighting at all). These three related-but-distinct concepts sharing
  similar names is a real risk for subtle sign/axis-swap bugs during the port — worth a dedicated,
  explicit unit test matrix (3 fuel types × flux ratios spanning `[0,1]`) rather than relying on
  general test coverage to catch it.
