# Breeding reactor + PWR (pressurized water reactor) triage

Sources read in full: `hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityMachineReactorBreeding.java`,
`.../TileEntityPWRController.java`, `.../pile/TileEntityPileBreedingFuel.java`,
`.../blocks/machine/MachineReactorBreeding.java`, `.../blocks/machine/MachinePWRController.java`,
`.../blocks/machine/BlockPWR.java` (incl. its nested `TileEntityBlockPWR`),
`.../items/machine/ItemBreedingRod.java`, `.../items/machine/ItemPWRFuel.java`,
`.../inventory/recipes/BreederRecipes.java`, `.../blocks/machine/BlockPillarPWR.java`; grepped for every
consumer of `ItemBreedingRod`/`BreedingRodType` and every reference to `BlockPWR`/`TileEntityPWRController`
across CE. Cross-checked against this port's own `docs/phase1/items_machine.md`, `blocks_generic.md`,
`docs/phase0/STATUS.md`, and the three already-written Phase 2 reports this package builds directly on
(`docs/phase2/blockentity_base.md`, `multiblock_framework.md`, `machines_power_generation.md`,
`gui_framework.md`). Confirmed NeoForge 1.21.1 API shapes cross-checked against
`upstream/neo-edition/src/main/java/com/hbm/inventory/{NtmMenuTypes.java,menus/MachineFluidTankMenu.java}`
(menu/screen registration only — Neo Edition has **no** ported PWR controller or breeding-reactor TE at
all, only the item-side `BreedingRodItem`/`FT_PWRModerator`, confirming CE is the sole behavior source
here by necessity, not just by rule).

## Headline finding

These are two structurally unrelated systems that happen to share a source directory and both got
flagged via items_machine.md's Deferred list:

- **The breeding reactor is small and almost entirely already gated on other packages.**
  `TileEntityMachineReactorBreeding` is a 2-slot, single-block-footprint machine (input rod slot,
  output rod slot) driven by a hardcoded `BreederRecipes` item→item table (13 rod-family transmutations
  + 1 special-case sword recipe) and an external "flux" number it reads once per tick from an *adjacent*
  `TileEntityReactorResearch` block (a different, much larger reactor system: the classic graphite-pile
  reactor family under `tileentity/machine/pile/*`, 10 files, not yet researched by any Phase 2 package).
  **`ItemBreedingRod` (already ported, 17 variants across 3 registry items `rod`/`rod_dual`/`rod_quad`)
  is never referenced by this TE at all** — the coupling is entirely through `BreederRecipes`'
  `ComparableStack` lookup table, not a direct import. The breeding reactor's own code is simple; what
  blocks it is (a) the shared machine `BlockEntity` base (`blockentity_base.md`), (b) a JSON-recipe
  home for `BreederRecipes`' 11 entries (`RecipesCommon`, already a known cross-cutting gap), (c) a
  Menu/Screen pair (`gui_framework.md`), and (d) the pile-reactor flux source, which is genuinely new
  scope this report cannot resolve and flags explicitly below.
- **The PWR is the real content here**: a full heat/coolant multiblock with its own bespoke assembly
  mechanism that is **not** built on `BlockDummyable`/`MultiblockHandlerXR` at all (unlike the 150
  classes `multiblock_framework.md` covers) — it physically replaces every structural block in the
  flood-filled volume with one shared proxy block/BE (`BlockPWR`/`TileEntityBlockPWR`) that forwards
  capability queries back to a cached controller. This is a second, independent multiblock idiom CE
  uses, worth calling out so nobody assumes `multiblock_framework.md`'s design covers it.
- **No turbine coupling exists inside the PWR at all.** The PWR's only fluid output is superheated
  coolant (`Fluids.COOLANT_HOT`) pushed out through `pwr_port` positions via the same
  `IFluidStandardTransceiver` pipe-network contract every other fluid machine uses. Any turbine that
  wants to consume it (`TileEntityMachineIndustrialTurbine`, `TileEntityMachineLargeTurbine`, etc. — all
  already triaged in `machines_power_generation.md`) does so exactly like it would consume hot fluid
  from a boiler or RBMK: through pipes, not through any PWR-specific hookup. "Turbine integration" is
  therefore "none, by design" — flagging this explicitly since the task asked to map it.

## Phase-2-safe scope

Nothing needs to wait for another package to be *designed*, but almost everything here needs another
package's *code* to exist before it can compile and run for real. What is safe to land now:

- **`ItemBreedingRod` (17 `BreedingRodType` variants, 3 registry items) — already ported in Phase 1,
  confirmed correct, zero changes needed.** This survey re-confirms `docs/phase1/items_machine.md`'s
  finding: it is a pure marker/NBT-free item (`ItemEnumMulti` over the enum), instantiated three times
  in CE's `ModItems` as `rod`/`rod_dual`/`rod_quad` (single/dual/quad rod stacks, each getting its own
  `flux`-scaled `BreederRecipes.setRecipe` entry — `x1`/`x2`/`x3` flux cost, matching CE's own
  `setRecipe(...)` helper at `BreederRecipes.java:47-51`). No block or TE in CE ever imports
  `ItemBreedingRod` directly (grepped) — every consumer (`BreederRecipes`, `TileEntityNukePrototype`,
  `ContainerNukePrototype`, `ItemStarterKit`, `HazardRegistry`, `ItemPoolsLegacy`, `CraftingManager`,
  `RodRecipes`) reads it through `ModItems.rod*` + the enum ordinal, not through the class itself.
- **`ItemPWRFuel` (15 `EnumPWRFuel` variants) — already ported in Phase 1, confirmed correct.** Purely
  a tooltip/data item (`yield`, `heatEmission`, a `Function` reaction-curve object per variant); the
  controller reads it via `EnumUtil.grabEnumSafely(EnumPWRFuel.VALUES, typeLoaded)` where `typeLoaded`
  is the old metadata index stored as a plain `int` field on the TE, not on the item — nothing about
  the item itself needs to change to support the controller once it exists.
- **`FT_PWRModerator` (fluid trait) — already ported in Phase 1**, read and confirmed 1.21.1-shaped
  (`net.minecraft.ChatFormatting`/`Component`, JSON `serializeJSON`/`deserializeJSON`). This is exactly
  the trait `TileEntityPWRController.update()` reads off `tanks[0].getTankType()` to get the coolant's
  reactivity multiplier — no further work needed on the trait itself.
- **`BlockGenericPWR` (heatex/heatsink/neutron_source/reflector/casing/port, 6 registry blocks) —
  already ported in Phase 1 as plain blocks with no TE**, per `docs/phase1/blocks_generic.md`. Confirmed
  correct against CE: none of these 6 blocks have a tile entity in CE either — they're pure visual/
  identity blocks that `MachinePWRController.assemble()`'s flood-fill recognizes by `Block` identity
  only (`isValidCasing`/`isValidCore`), never by TE state.
- **`BlockPillarPWR` (fuelrod/control/channel, 3 registry blocks) — a small gap Phase 1 missed, but
  trivially Phase-2-safe today.** Not mentioned in `docs/phase1/blocks_generic.md` and not yet present
  in `src/main/java/com/hbm/blocks`. It is the exact same shape as the already-ported `BlockGenericPWR`
  sibling (`extends BlockBakeBase`, no TE, tooltip-only override) — one class, 3 registry entries
  (`pwr_fuelrod`, `pwr_control`, `pwr_channel`). Recommend porting it alongside whichever package picks
  up the remaining `blocks_generic.md`-style visual-block sweep, or inline here since it has zero
  coupling to anything this report defers.
- **`BreederRecipes`' 11-entry recipe table is small, closed, and fully known now** (10
  `setRecipe(inputType, outputType, flux)` calls covering `LITHIUM→TRITIUM`, `CO→CO60`, `RA226→AC227`,
  `TH232→THF`, `U235→NP237`, `NP237→PU238`, `PU238→PU239`, `U238→RGP`, `URANIUM→RGP`, `RGP→WASTE`, each
  auto-expanded x1/x2/x3 for the three rod-count items, plus one special case
  `meteorite_sword_etched→meteorite_sword_bred` at 1000 flux). This is safe to write down as target JSON
  recipe data *now*, for whoever lands `RecipesCommon`'s replacement — it does not need a bespoke recipe
  type, it's a straightforward `(input, output, flux-cost)` triple, structurally identical to a simple
  cooking/smelting-style recipe with an extra numeric field.
- **The PWR's pure-math formulas are fully portable as reference/design material now** (no TE needed to
  write these down, and they should not be re-derived later): heat-capacity scaling
  (`coreHeatCapacityBase + heatsinkCount * (coreHeatCapacityBase/20)`, `heatsinkCount` capped at 80),
  the connection-efficiency curve (`connectinFunc(x) = x/10*(1-e^(-x/300)) + x/150*e^(-x/300)`, i.e. a
  soft-capped output-per-connection function), core/hull heat equalization
  (`heat -= (heat - avg) * coolingApproach`, `coolingApproach = (1-e^(-heatexCount*5/rodCountForCoolant/2))/2`),
  and the 0.999 per-tick heat-decay multiplier. These are pasted verbatim from
  `TileEntityPWRController.java:166-480` in the source list above; no invention needed.

## Deferred scope

### Breeding reactor

- **Shared machine `BlockEntity` base** — `TileEntityMachineReactorBreeding extends TileEntityMachineBase`
  in CE (2-slot inventory, `IGUIProvider`, `IBufPacketReceiver`). Needs `blockentity_base.md`'s
  `MachineBaseBlockEntity` to land first.
- **`BreederRecipes` as JSON data** — needs `RecipesCommon`/`GenericRecipe(s)` (already a known
  cross-cutting gap per `docs/phase0/STATUS.md` and `docs/phase1/STATUS.md`); the data itself is
  captured above so this is purely "needs the loader to exist," not "needs research."
- **Menu/Screen (`ContainerMachineReactorBreeding`/`GUIMachineReactorBreeding`)** — needs
  `gui_framework.md`'s shared base classes + `MenuType<?>` registration convention.
- **The flux source — `TileEntityReactorResearch` and the classic pile-reactor family
  (`com.hbm.tileentity.machine.pile.*`: `TileEntityPileBase`, `PileBaseMK2`, `PileBreedingFuel`,
  `PileControl`, `PileCore`, `PileDeviceBase`, `PileFuel`, `PileLoader`, `PileNeutronDetector`,
  `PileSource`, `PileVent`, plus `ReactorResearch`/`TileEntityReactorResearch` themselves — ~12 files,
  none yet covered by any Phase 2 research package listed in `docs/phase2/`).** This is the single
  largest real gap for the breeding reactor: without it, `getInteractions()` (which scans four
  horizontal neighbors for `ModBlocks.reactor_research` and calls `findCore`/reads `.totalFlux`) has
  nothing to read, so `canProcess()` never returns true and the machine is a dead shell even once its
  own base class exists. `TileEntityPileBreedingFuel` (also read for this report) is a *different*
  consumer of the same pile-reactor system — a graphite-pile fuel-column block that produces
  `block_graphite_tritium` on depletion — and is likewise blocked on the same pile-reactor package,
  not on anything the breeding-reactor machine itself needs. **Recommendation: schedule a dedicated
  "classic pile reactor" Phase 2 research package** (parallel in scope to this report, `RBMK`, and the
  turbine family) before treating the breeding reactor as functionally complete; the machine block
  itself can still be ported and will compile/place fine without it, it just won't ever produce output.
- **`ModBlocks.reactor_research`/`findCore` pattern** — `MachineReactorBreeding` itself is a
  `BlockDummyable` 2-tall casing (`getDimensions() = {2,0,0,0,0,0}`), so it *does* fit
  `multiblock_framework.md`'s standard dummy-block pattern (unlike the PWR, see below) — once that
  package lands, `MachineReactorBreeding` needs no bespoke multiblock code of its own.

### PWR

- **Shared machine `BlockEntity` base** — `TileEntityPWRController extends TileEntityMachineBase`
  (3-slot inventory: slot 0 = fresh fuel rod in, slot 1 = spent/hot fuel rod out, slot 2 unused in the
  read source — `super(3, true, false)` enables both wrapper flags but the constructor comment doesn't
  explain slot 2's purpose; flag as an open question below rather than guessing). Needs
  `blockentity_base.md`'s base class, specifically its `enableFluidWrapper`/`enableEnergyWrapper`
  constructor-flag shape (PWR needs the fluid wrapper for its two tanks, and does **not** need the
  energy wrapper — it has no `IEnergyProviderMK2`/`ConductorMK2` implementation in CE at all; the PWR
  is a heat/steam source only, HE generation happens downstream in a turbine).
- **`FluidTankNTM` (2 tank instances per controller: `tanks[0]` = cold coolant in, 128 000 mB cap,
  `tanks[1]` = hot coolant out, 128 000 mB cap)** — confirmed absent from the port (per
  `blockentity_base.md`/`multiblock_framework.md`'s own findings). The controller depends on
  `FluidTankNTM`'s `setType`/`setTankType`/`getFill`/`setFill`/`getMaxFill`/`getTankType`/
  `serialize`/`deserialize`/`readFromNBT`/`writeToNBT` API surface directly — none of this is
  PWR-specific, it's the same tank class every fluid machine needs. Not re-solved here.
- **`com.hbm.api.fluidmk2.{IFluidReceiverMK2,IFluidProviderMK2,IFluidUserMK2}` and
  `com.hbm.api.fluid.IFluidStandardTransceiver`/`IConnectionAnchors`** — the controller implements
  `IFluidStandardTransceiver` + `IConnectionAnchors` (for `getConPos()`, the port-position list used by
  the pipe network to find valid hookup points) and `BlockPWR.TileEntityBlockPWR` implements
  `IFluidReceiverMK2`. **None of these four interfaces exist in this port yet** (grepped
  `src/main/java/com/hbm` — zero hits for any of the four). This is the TE-side half of the fluid
  pipe-network contract; per the ground rules this port has no world-fluid-block system at all (Phase
  1's own finding, restated in `blockentity_base.md`), but that's a *different* gap — these four
  interfaces are the tank/TE-network side, not world fluid blocks, and PWR genuinely needs them to
  export `COOLANT_HOT`. Flag as a concrete prerequisite for whoever owns
  `com.hbm.inventory.fluid`/`com.hbm.api.fluidmk2` as its own package (same gap `blockentity_base.md`
  already flagged for fluid machines generally — PWR is simply one more, large, consumer of it).
- **Menu/Screen (`ContainerPWR`/`GUIPWR`)** — needs `gui_framework.md`'s shared base + registration
  convention. CE's GUI shows: fuel/hot-fuel slots, two tank gauges, core/hull heat bars, a rod-level
  slider (`IControlReceiver.receiveControl` — already ported in this port, confirmed at
  `src/main/java/com/hbm/interfaces/IControlReceiver.java`, no changes needed there), and flux/output
  readouts.
- **`ModBlocks.corium_block`** — the meltdown failure state (`meltDown()` replaces every fuel-rod
  position with `corium_block` and triggers a vanilla `world.newExplosion(..., 15F, ...)`). Not present
  in this port yet (grepped `src/main/java/com/hbm/blocks`, zero hits). A single new block, low
  complexity, but a real prerequisite for the meltdown path specifically (the rest of the controller
  functions without it — `meltDown()` is the only caller).
- **`SatelliteRayScan.reportEvent(..., RayEvent.INFO_NUCLEAR, 200)`** — called once every 100 ticks
  while fuel is loaded, purely to make the reactor "visible" to CE's orbital-satellite detection system
  (`com.hbm.saveddata.satellites.*`, a full world-simulation/save-data system, clearly Phase 4+ scope
  per PORT_SPEC's own area split). This is a soft dependency: safe to stub as a no-op call (or guard
  behind a null-check on a not-yet-existing registry) without changing any other PWR behavior, and
  should **not** block porting the rest of the controller.
- **`ItemPWRPrinter`'s `serialize`/`deserialize` hook inside the controller's own network sync
  methods** (`TileEntityPWRController.serialize`/`deserialize` branch on an `isPrinting` boolean and,
  if set, delegate the *entire* packet to `ItemPWRPrinter.serialize(world, buf)`/`.deserialize(...)`
  instead of the controller's own state — a client-side "print a construction blueprint" feature that
  hijacks the TE's own sync channel). Already flagged as Deferred in `docs/phase1/items_machine.md`
  (`ItemPWRPrinter` "flood-fills `BlockPWR`/`TileEntityPWRController`/`TileEntityBlockPWR`"), confirmed
  here from the controller side: this is a real, if narrow, coupling — the controller's packet
  format is not fully self-contained, it has an escape hatch for a completely different consumer.
  Recommend porting the controller's normal `serialize`/`deserialize` path first and treating the
  `isPrinting` branch (and `ItemPWRPrinter` itself) as a follow-up once both exist, rather than
  blocking the controller on the printer item.
- **OpenComputers integration** (`li.cil.oc.api.*`, `@Optional.InterfaceList`/`@Optional.Method`,
  `SimpleComponent`/`CompatHandler.OCComponent`) on both TEs — grepped, **zero OpenComputers references
  anywhere in this port yet**, and `machines_power_generation.md` independently flagged the identical
  gap on the turbine family. This is a recurring cross-cutting question, not specific to this package:
  does the port target OpenComputers compatibility at all in a later phase, or is it dropped mod
  support? Not decided here (consistent with that other report), but worth surfacing again since two
  independent Phase 2 surveys have now hit the same undecided call.

## Key design/API decisions

Every shape below was confirmed by reading either this port's own committed code, another already-written
Phase 2 report's confirmed findings, or Neo Edition's real source (API shape only) — never invented.

- **The breeding reactor fits the standard `BlockDummyable` multiblock pattern; the PWR does not, and
  should not be forced into it.** `MachineReactorBreeding extends BlockDummyable` in CE with a trivial
  2-tall `getDimensions()`, matching the 149-class pattern `multiblock_framework.md` already designed
  for — no new multiblock code needed for it. `MachinePWRController`, by contrast, is a plain
  `BlockContainerBakeable`/`BaseEntityBlock` (not a `BlockDummyable` subclass at all in CE) that
  performs its own bespoke recursive flood-fill (`assemble()`/`floodFill()`, read in full above) and,
  on success, **physically replaces every structural block in the volume** with one shared proxy block
  (`ModBlocks.pwr_block`, a `BlockPWR` instance carrying a single `IO_ENABLED` boolean blockstate
  property) whose `TileEntityBlockPWR` stores the *original* replaced blockstate (for restoration on
  disassembly) plus a cached `corePos` back-pointer, and forwards every capability/fluid-network/ROR
  query to the cached `TileEntityPWRController` core when `IO_ENABLED` is true. This is architecturally
  a second, independent multiblock idiom from the one `multiblock_framework.md` designed
  (`MultiblockHandlerXR`/`checkSpace`/`fillSpace`/`META`-encoded dummy blocks) — **do not attempt to
  reuse `BlockDummyable`/`MultiblockHandlerXR` for the PWR**; it needs its own small
  flood-fill-and-replace implementation, ported close to verbatim from `MachinePWRController.assemble()`.
  The `IO_ENABLED` property itself maps directly onto NeoForge 1.21.1's `BooleanProperty`
  (`BlockStateProperties`-style custom property), a straightforward, already-common blockstate idiom
  elsewhere in this port (e.g. any existing `BooleanProperty` usage in `com.hbm.blocks.generic`).
- **`BlockEntityType`/`DeferredRegister` registration** — confirmed pattern, identical to every other
  Phase 2 finding: `ModBlocks.BLOCK_ENTITY_TYPES.register("pwr_controller", () ->
  BlockEntityType.Builder.of(TileEntityPWRController::new, pwrControllerBlock.get()).build(null))`, and
  likewise for `TileEntityBlockPWR` and the breeding reactor's TE, following
  `GenericCrateBlocks.java`'s already-working shape (see `blockentity_base.md` for the full citation).
- **Menu/Screen registration** — confirmed real 1.21.1 shape via Neo Edition's `NtmMenuTypes`
  (`DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID)`, each
  entry `MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory))` where `factory` is an
  `IContainerFactory<T>` — i.e. a `(int id, Inventory inv, FriendlyByteBuf buf) -> T` constructor
  reference) and a concrete example menu (`MachineFluidTankMenu`) whose two-constructor idiom (one
  reading a `BlockPos` off the network buffer to look up the block entity, one taking the block entity
  directly) is exactly the shape `ContainerPWR`/`ContainerMachineReactorBreeding` should follow once
  `gui_framework.md`'s shared base lands — cited here only to confirm the API is real, not to duplicate
  that report's own (much larger) design work.
- **Package naming: this report defers to `multiblock_framework.md`'s explicit resolution
  (`com.hbm.blockentity`), not `blockentity_base.md`'s non-binding recommendation of `com.hbm.tileentity`
  — flagging the inconsistency rather than silently picking one.** Both already-written Phase 2 reports
  address the `docs/phase0/STATUS.md`-flagged package-naming decision, but they land in different
  places: `blockentity_base.md` recommends preserving `com.hbm.tileentity` "for the record but not
  self-authorized," while `multiblock_framework.md` treats the question as **decided** in favor of
  `com.hbm.blockentity` and explicitly instructs downstream packages not to re-litigate it ("Do not let
  any of those packages re-decide the `com.hbm.blockentity` naming call below — it is made here, once,
  for all of Phase 2"). Since this report's own TEs (`TileEntityPWRController`,
  `TileEntityMachineReactorBreeding`, `TileEntityBlockPWR`) are exactly the kind of concrete machine
  content both reports describe as downstream of that call, this report follows
  `multiblock_framework.md`'s resolution (`com.hbm.blockentity.machine.TileEntityPWRController`, etc.)
  as the more explicit and more recently self-declared-final of the two — but flags clearly that
  whoever integrates all Phase 2 research should reconcile these two documents' wording before either
  is treated as implementation-ready, since right now they visibly disagree on the record.
- **HE energy is genuinely out of scope for the PWR itself.** Confirmed by reading the full
  `TileEntityPWRController` class: it implements no `IEnergyHandlerMK2`/`IEnergyProviderMK2`/
  `IEnergyConductorMK2` interface and has no energy-related field at all. The PWR's entire output is
  heat converted to hot coolant fluid; HE generation is exclusively the downstream turbine's job (see
  `machines_power_generation.md`). Do not add an energy capability to the PWR controller — it would be
  new, invented behavior with no CE basis.

## Open questions / risks

- **Pile-reactor / `ReactorResearch` flux source has no owning Phase 2 package yet.** This is the
  single biggest open item from this report: the breeding reactor's entire raison d'être (converting
  neutron flux into transmuted rods) depends on a large, unresearched sibling system. Recommend the
  orchestrating session schedule a dedicated "classic pile reactor" research package (parallel to RBMK,
  turbines, and this report) before Phase 2 implementation treats the breeding reactor as done — it can
  be *ported* now (compiles, places, has a working GUI) but will be inert without that system.
- **Package-naming disagreement between `blockentity_base.md` and `multiblock_framework.md`** (see Key
  design decisions above) needs an actual human/orchestrator sign-off, not another report picking a
  side. This report picked a side (`com.hbm.blockentity`) only to have a concrete answer for its own
  content; that pick should not be read as resolving the disagreement for the rest of Phase 2.
- **`TileEntityPWRController`'s slot 2 has no documented purpose.** The constructor is
  `super(3, true, false)` (3-slot inventory) but only slots 0 (fresh fuel in) and 1 (hot fuel out) are
  ever read or written anywhere in the 726-line file. `isItemValidForSlot`/`canExtractItem`/
  `getAccessibleSlotsFromSide` also only ever mention slots 0 and 1. This could be a vestigial slot from
  an earlier CE revision, or a slot consumed only by `ContainerPWR` (not read for this report — out of
  scope per the task's TE-focus, but whoever ports `ContainerPWR` next should check it directly rather
  than assume slot 2 is dead).
- **OpenComputers compatibility is undecided across two independent Phase 2 surveys now** (this report
  and `machines_power_generation.md` both hit it, on unrelated TE families). Recommend a single
  cross-cutting decision (port a minimal OC shim / drop OC support entirely / defer to a named later
  phase) rather than each package guessing independently.
- **Meltdown behavior (`meltDown()`) is a straightforward vanilla explosion + block replacement and
  carries no special risk of its own**, but it does depend on `ModBlocks.corium_block` existing (not
  yet ported) and implicitly assumes the `rods` list (populated only during `setup()`, i.e. only valid
  while `assembled == true`) is non-empty and non-stale — CE itself has no staleness guard beyond the
  `rods.isEmpty()` early return already in the source. Faithful-port note only, not a new risk to fix.
- **This report's scope was intentionally kept to the two named TE families.** `BlockPillarPWR` (a
  genuine small Phase-1-shaped gap, 3 registry blocks) is flagged above as safe to port immediately but
  was not written here — recommend whoever finishes the `blocks_generic.md`-style visual-block sweep,
  or the implementer of this package, add it as a one-file addition rather than opening a third report
  for three blocks.
