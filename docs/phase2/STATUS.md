# Phase 2 status

Phase 2 (Machines) followed the same research -> implement -> review -> fix methodology as Phase 0/1,
scaled up for the phase's own size: a 15-agent research wave (`docs/phase2/*.md`), a 5-agent
foundation implement wave (multiblock framework, block-entity base, fluid tank, recipe scaffolding,
Menu/Screen GUI framework) plus its own review/fix pass, then a 13-agent concrete-machine implement
wave (power generation, storage, 3 logistics packages, oil chain, 2 processing packages, breeding
reactor + PWR, fusion + Watz, RBMK split into core-logic + column-blocks, machine-coupling items)
plus its own 8-agent review/fix pass. Full per-package detail lives in `docs/phase2/*.md` (research)
and the git log (`git log --oneline` from `45da3ed` through `5a30356` covers this phase area-by-area).

## What's implemented (by package)

**Foundation** (lands first, everything else builds on it):
- `com.hbm.handler.MultiblockHandlerXR`/`MultiblockBBHandler` + `com.hbm.blockentity.IPersistentNBT` -
  unblocked the already-written `BlockDummyable`/`BlockDummyableMBB` multiblock-casing framework.
  Also resolved the Phase 0-flagged `com.hbm.tileentity` vs `com.hbm.blockentity` package-naming
  decision: **`com.hbm.blockentity`**, matching Neo Edition's real, confirmed choice.
- `com.hbm.blockentity.LoadedBaseBlockEntity`/`MachineBaseBlockEntity` - the base class hierarchy
  every one of Phase 2's ~90 concrete block entities extends, wired to the already-ported
  `NTMEnergyCapabilityWrapper`/`NTMFluidHandlerWrapper`/`ItemStackHandlerWrapper`/
  `CapabilityContextProvider` capability plumbing.
- `com.hbm.inventory.fluid.tank.FluidTankNTM` + the `com.hbm.api.fluidmk2` network trio
  (`IFluidReceiverMK2`/`ProviderMK2`/`UserMK2`/`TransceiverMK2`/`FluidNode`/`FluidNetMK2`) -
  mirroring the already-ported `energymk2` package's design.
- `com.hbm.inventory.container.MenuBase`/`ModMenuTypes` + `com.hbm.inventory.gui.GuiInfoContainer` -
  the Menu/Screen GUI framework every machine with a player-facing inventory now uses.
- `com.hbm.inventory.RecipesCommon` (comparison-key hierarchy) + `com.hbm.inventory.recipes`
  (JSON `Recipe<?>`/`RecipeSerializer` scaffolding) - unblocked `ItemBlueprints`/`ItemBlueprintFolder`
  (a real, previously-undocumented compile break) and gave every processing machine a shared
  recipe-registration shape to build on.

**Concrete machine content**:
- **Power generation**: burner/steam engine, full + mini RTG, diesel/combustion generators,
  small/large/industrial turbines, gas turbine, solar boiler+mirror pair.
- **Storage**: TE-backed mass storage crate, fluid tank, and battery blocks (distinct from Phase 1's
  plain non-TE `GenericCrateBlocks`).
- **Logistics**: energy cable/pylon network (built on Phase 0's `PowerNetMK2`/`IEnergyConductorMK2`),
  fluid duct network (built on the new `fluidmk2` trio), item conveyors/crane inserters (ported
  `EntityMovingItem`/`EntityMovingConveyorObject` first, as the research report recommended).
- **Oil chain**: derrick, pumpjack, refinery (real cracking recipe), fracking. The derrick/pumpjack's
  oil-deposit world-gen dependency is Phase 4 scope - stubbed with a documented TODO.
- **Processing**: shredder, assembler, crystallizer, mixer, chemical plant, centrifuge, gas
  centrifuge, cyclotron, SILEX/laser isotope separation, electrolyser - all with real CE recipe data
  ported as JSON, not stubs.
- **Reactors**: breeding reactor + PWR (consuming Phase 1's `ItemBreedingRod`/`ItemPWRFuel`), fusion
  reactor + Watz reactor (consuming Phase 1's `ItemICFPellet`/`ItemFELCrystal`/`ItemWatzPellet`).
- **RBMK**: the full reactor - neutron-flux/xenon-poisoning/heat math and every column type (fuel
  rod, control rod x3, moderator, reflector, absorber, coolant, boiler, outgasser, heater, inlet/
  outlet, storage, autoloader, console), each column its own independent `BlockDummyable` structure
  per CE's real design. PORT_SPEC.md's own risk note asked for this to be built as an
  independently-unit-testable pure-logic core (`com.hbm.api.rbmk`, `com.hbm.handler.neutron`)
  separable from any `BlockEntity` - done.
- **Machine-coupling items**: the ~19 tool items Phase 1 deferred pending their target systems
  (screwdriver/wrench/blowtorch/analyzer, lock/key family, drone logistics, RBMK console tools,
  power-net/conveyor/mirror tools), now that those systems exist.

## Real bugs found and fixed beyond each wave's own review stage

- **Registration-wiring dead code, recurring across phases**: `PowerGenCapabilities`/
  `PWRCapabilities`'s `@EventBusSubscriber` annotations omitted `bus = Bus.MOD` - since
  `RegisterCapabilitiesEvent` only ever fires on a mod's own mod-event-bus (confirmed against
  FancyModLoader's pinned-version source, not assumed), these classes' capability registrations
  never actually ran. None of 9 power-gen or 3 PWR block entities' item/fluid/energy capabilities
  were reachable from hoppers/pipes/cables despite every other layer of wiring being correct - the
  same "looks wired but isn't" pattern Phase 1 hit repeatedly with `registerAll()` calls, one layer
  deeper this time (an annotation attribute, not a missing method call).
- **Confirmed compile-breaking bug in the RBMK core**: `RBMKNeutronHandler.java` - the single most
  central class in the whole RBMK simulation, the per-tick flux-stream dispatcher - had three live
  calls into `com.hbm.handler.radiation.ChunkRadiationManager` (Phase 4 scope, does not exist in this
  port). A sibling class had already correctly deferred its own analogous CE call site; this one
  hadn't. Fixed with matching forward-reference comments.
- **RBMK cross-package reconciliation**: the core-logic and column-blocks packages ran concurrently
  and only discovered each other's real API partway through. The column-blocks package proactively
  read the core-logic package's landed code and reconciled its own earlier forward-reference guesses
  against it (deleted its own duplicate `ItemRBMKRod`/`IRBMKLoadable`, fixed `Level`->`ServerLevel`
  typing, switched to the real `RBMKNeutronHandler` helpers) - verified this actually landed correctly
  on disk (not just claimed) before trusting it. A follow-up review then found and fixed: a rotation-
  sign bug in the ReaSim flux-spread trigonometry (mirrored instead of matching CE's clockwise
  convention), an unconditional `checkMeltdown()` call duplicated onto every non-rod column type
  (CE only checks from the fuel rod), a duplicated heat-to-extraction interpolation formula that had
  drifted from the shared pure-function version, and a network-sync asymmetry in
  `RBMKRodBlockEntity` (deserialize wrote into the wrong fields, so client-side flux readouts would
  always show stale/zero data).
- **`HbmSimpleRecipe` design bug**: `getType()`/`getSerializer()` were hardcoded to the demo
  registration's own `RecipeType`/`RecipeSerializer` regardless of which serializer actually decoded
  a given recipe - silently breaking the class's own documented "multiple machines can reuse this
  shape with their own type/serializer pair" contract, since `RecipeManager` buckets recipes by
  `getType()`. Fixed before any real machine hit it (caught during the processing-machines pass,
  which was the first real second consumer).
- Several narrow, well-justified cross-file additions to already-committed Phase 1 files, each adding
  exactly one new public forwarding method rather than widening an intentionally-narrow type: `ItemPWRFuel.EnumPWRFuel#reactivity(double)`,
  `ItemWatzPellet#computeBurn/computeAbsorb`, and exposing `MachineItems.WATZ_PELLET_DEPLETED` (registered in Phase 1 but
  never given an accessible field since nothing consumed it yet).
- `ItemRBMKPellet.rectify(ItemStack)` and `com.hbm.util.Compat.getBlockEntityStandard` - two
  pre-existing compile breaks in already-committed Phase 0/1 code (forward-referenced by
  `HazardModifierRBMKRadiation` and `IEnergyReceiverMK2`/`IFluidReceiverMK2` respectively, never
  implemented) - found and fixed opportunistically by Phase 2 agents working adjacent code.

## Known gaps intentionally deferred to later phases

- RBMK's actual meltdown byproduct conversion (pribris blocks, corium, `EntityRBMKDebris` - the
  entity doesn't exist in this port yet) - Phase 3/4 scope, the BFS/reduce-factor decision logic
  calling it is already fully implemented and waiting.
- OpenComputers integration and redstone-over-radio wiring on the RBMK rod/control classes (the
  latter is a live system elsewhere in this port, e.g. `PWRControllerBlockEntity` - just not yet
  extended to RBMK).
- The oil derrick/pumpjack's extraction mechanic is stubbed pending Phase 4's oil-deposit world-gen.
- `com.hbm.inventory.FluidContainerRegistry` - referenced by 5 already-shipped capability files but
  does not exist anywhere in the tree; Phase 2's new machines were deliberately designed to avoid
  depending on it (fluids move over the `fluidmk2` pipe network / vanilla `IFluidHandler.ITEM`
  capability instead of whole-container item swapping) - a real, pre-existing gap for whoever
  eventually owns item-side fluid containers as a dedicated area.
- A real per-machine JSON recipe-vs-Java-loader design tension the research flagged (Neo Edition kept
  CE's Java `GenericRecipe` loader verbatim instead of migrating to JSON) is *not* re-litigated here;
  Phase 2 built the JSON scaffolding per PORT_SPEC.md's explicit ground rule and used it for every
  machine ported this phase, but a `com.hbm.inventory.recipes.loader.GenericRecipe(s)` compile shim
  also exists for `ItemBlueprints`/`ItemBlueprintFolder`'s narrower pool-bookkeeping need - the two
  are not the same mechanism and should not be conflated by a future reader.
- Texture/model assets: every Phase 2 GUI renders with plain filled panels (this port has no
  `assets/hbm/textures` tree yet, a pre-existing gap from before Phase 2) - a real, tracked follow-up
  once asset porting starts, not a Phase 2 defect.
- A fidelity pass on the RBMK boiler/outgasser/cooler/heater's per-tick heat/fluid conversion-rate
  constants (approximated rather than fully read from CE's ~1400 combined lines, given this wave's
  time budget) - documented per-class as a follow-up, not silently presented as exact.

## On "gradlew build green" as a per-phase gate

Unchanged from Phase 1: **this sandbox cannot run `gradlew` at all** (org egress policy blocks
`maven.neoforged.net`). Every package in Phase 2 was verified by static means - reading every
constructor/method signature against its real target class, checking every import resolves, and
(for the two implement waves) a dedicated adversarial review pass against CE source. This caught a
meaningful number of real bugs (see above) but is not a substitute for a real compiler; the next
session or CI run with real network access should run `gradlew compileJava` as the first order of
business and treat any error not explained by this file or `docs/phase0/STATUS.md`/
`docs/phase1/STATUS.md` as a real regression, not another expected forward reference.
