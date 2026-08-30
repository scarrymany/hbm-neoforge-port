# Energy cable/pylon network (`blocks/network/energy` + `tileentity/network/energy`)

Source: `hbm-ce/src/main/java/com/hbm/blocks/network/energy/**` (15 files) and
`hbm-ce/src/main/java/com/hbm/tileentity/network/energy/**` (8 files), plus two inner-class TEs
(`CableDiode.TileEntityDiode`, `BlockCableGauge.TileEntityCableGauge`) and one sibling TE outside
that exact directory (`com.hbm.tileentity.network.TileEntityCablePaintable`, paired with
`BlockCablePaintable`). Builds directly on Phase 0's `com.hbm.api.energymk2.*` /
`com.hbm.uninos.*` (already read in full: `PowerNetMK2`, `IEnergyConductorMK2`,
`IEnergyReceiverMK2`, `IEnergyProviderMK2`, `IEnergyConnectorMK2`, `IEnergyConnectorBlock`,
`IEnergyHandlerMK2`, `Nodespace`/`Nodespace.PowerNode`, `GenNode`, `NodeNet`, `UniNodespace`,
`INetworkProvider`) and the two capability wrappers (`NTMEnergyCapabilityWrapper`,
`NTMCableEnergyCapabilityWrapper`). Cross-references `docs/phase1/blocks_network_rail.md`
(the 76-file `blocks/network` triage that placed this whole package in Phase 2) and
`docs/phase0/STATUS.md`'s open-decisions list.

## Headline finding: the network-graph API is not new work, it's already a 1:1 port

CE's own source tree (the thing being ported, not a plan for one) already imports
`com.hbm.api.energymk2.*` / `com.hbm.uninos.*` in every file this report covers — CE had already
done its own "MK2"/UNINOS energy-graph rewrite before this NeoForge port started. Phase 0 ported
that API + graph engine faithfully (`TileEntity`→`BlockEntity`, `ForgeDirection`→`Direction`,
Forge `IEnergyStorage`→NeoForge `IEnergyStorage`, `IBlockAccess`→`BlockGetter`), and, per its own
`DIGEST.md`, generic-signature-verified it against `PowerNetMK2` in a dedicated gap-fill pass. It
is feature-complete and already **more capable than CE's original graph maintenance**: CE's
`UniNodespace.PerTypeNodeManager.removeNode` recomputes the graph incrementally via BFS connected
components (`splitNetIfNecessary`/`collectComponent`) instead of invalidating the whole net on any
node removal — a real behavioral improvement over the base game already locked in, not something
this phase should second-guess or redesign.

**Consequence for this area's scope**: porting `BlockCable`/`CableDiode`/`PylonLarge`/
`Substation`/`PowerCableBox` (and their package-mates) is a **mechanical TileEntity→BlockEntity
transcription against a stable, already-verified API surface**, not new graph-engine design. Every
node-registration/connection-discovery/load-balancing call these TEs make already has a working,
reviewed NeoForge-side implementation to call into unchanged:

- **Node registration**: `IEnergyConductorMK2.createNode()` (default method, already ported) builds
  a `Nodespace.PowerNode` at the TE's own `BlockPos` with 6 `DirPos` connection stubs (one per
  `Direction`). A conductor TE's tick method calls `Nodespace.getNode(level, pos)` — if absent/
  expired, `this.createNode()` then `Nodespace.createNode(level, node)`. On removal,
  `Nodespace.destroyNode(level, pos)`. All three are static passthroughs to
  `UniNodespace`/`PerTypeNodeManager`, already implemented.
- **Connection discovery**: `UniNodespace.PerTypeNodeManager.checkNodeConnection` walks each node's
  `connections` (`DirPos[]`), looks up the node at that position, and checks it reciprocates
  (`checkConnection`: does the neighbor have a `DirPos` pointing back at *this* position, opposite
  direction). Reciprocal connections join nets (`connectToNode` — smaller net absorbed into larger
  via `joinNetworks`/`joinLink`); no valid neighbor spawns a fresh net via the `INetworkProvider`
  supplier (`Nodespace.THE_POWER_PROVIDER = PowerNetMK2::new`). Already implemented, unchanged from
  what CE's TEs already call.
- **Load balancing**: `PowerNetMK2.updateInternal()` (the full per-tick net update, called from
  `UniNodespace`'s tick driver) buckets receivers by `IEnergyReceiverMK2.ConnectionPriority`
  (`LOWEST`..`HIGHEST`), fills highest priority first, and within a priority bucket splits
  `weightedShare`-proportional to each receiver's demand with a rotating cursor
  (`updateReceiverRemainderCursor`/`updateProviderRemainderCursor`) so fairness rotates
  tick-to-tick instead of always favoring list-order-first receivers. `sendPowerDiode`/
  `extractPowerDiode` are the narrower diode-only paths (`CableDiode`'s TE and the FE-bridge
  capability wrappers call these directly, bypassing a full net update). All already implemented
  and already what CE's own TEs call.

This means the review bar for Phase 2 implementation of this area is **"does the ported TE call
the existing API the same way CE's TE does"**, not "does the ported TE reinvent network semantics."

## Phase-2-safe scope

Every file below is blocked only on (a) a `BlockEntity` base-class decision (see Key design
decisions) and (b) for two files, the multiblock framework — nothing in this list needs another
Phase 2 *package* (fluid ducts, pneumo-tubes, conveyors, etc.) to land first, and nothing needs a
GUI/menu framework except where flagged. 14 blocks register from 13 concrete block classes (one
class, `PylonRedWire`, is instantiated twice: `red_pylon`, `red_pylon_steel_small`) plus the
abstract `PylonBase` and `BlockCablePaintable` — 15 files, matching the task's file count.

| Class | Registry id(s) | TE | Coupling | Portable now? |
|---|---|---|---|---|
| `BlockCable` | `red_cable` | `TileEntityCableBaseNT` | plain 6-way conductor, dynamic connection-mask block state | Yes |
| `BlockCableClassic` | `red_cable_classic` | `TileEntityCableBaseNT` (inherited) | trivial `BlockCable` subclass, different texture | Yes |
| `PowerCableBox` | `red_cable_box` | `TileEntityCableBaseNT` | `Block implements ITileEntityProvider` (not `BlockContainer`), 5-meta cable-thickness variant, same connection-mask logic as `BlockCable` (duplicated `resolveMask`) | Yes |
| `WireCoated` | `red_wire_coated` | `TileEntityCableBaseNT` | plain conductor variant | Yes |
| `WireCoatedRadResistant` | `red_wire_sealed` | `TileEntityCableBaseNT` (inherited) | `WireCoated` subclass + `IRadResistantBlock` (radiation-package interface, out of this area's scope but a real dependency — confirm it's ported before this compiles) | Yes, pending `IRadResistantBlock` |
| `CableSwitch` | `cable_switch` | `TileEntityCableSwitch` | `TileEntityCableBaseNT` subclass; meta 0/1 = off/on gates `shouldCreateNode()` | Yes |
| `CableDetector` | `cable_detector` | `TileEntityCableSwitch` (shared) | same TE as `CableSwitch` | Yes |
| `PowerDetector` | `machine_detector` | `TileEntityMachineDetector` | not read in full (outside the 5 named classes); shares the `TileEntityCableBaseNT` lineage per `tileentity/network/energy` inventory | Yes (verify on implementation) |
| `CableDiode` | `cable_diode` | `CableDiode.TileEntityDiode` (inner class) | `IEnergyReceiverMK2` + `IControlReceiver` (**already ported**, `com.hbm.interfaces.IControlReceiver`) + `IGUIProvider` (**not ported — no menu framework yet**, see Deferred scope) | Partially — TE energy/control logic yes, GUI no |
| `BlockCableGauge` | `red_cable_gauge` | `BlockCableGauge.TileEntityCableGauge` (inner, extends `TileEntityCableBaseNT`) | additionally implements OpenComputers' `SimpleComponent` + CE's `IRORValueProvider` | Yes for the energy TE; **drop the OpenComputers integration** (see Open questions) |
| `BlockCablePaintable` | `red_cable_paintable` | `TileEntityCablePaintable` (not read in full; sibling TE outside `tileentity/network/energy`) | `extends BlockBakeBase implements IToolable, ITooltipProvider, IFacade` — paint-color state, cross-references the `items.util` paint-tool research area per `docs/phase1/blocks_network_rail.md`'s own flag | Needs cross-check with whichever area owns `IFacade`/paint tools before scheduling |
| `PylonBase` | (abstract, not registered) | — | shared tooltip/`onBlockActivated` (dye-to-set-color) base for `PylonRedWire` | N/A |
| `PylonRedWire` | `red_pylon`, `red_pylon_steel_small` | `TileEntityPylon` | `BlockContainer` (**not** `BlockDummyable` — single-block pylon, no multiblock framework needed) | Yes |
| `PylonLarge` | `red_pylon_large` | `TileEntityPylonLarge` | `extends BlockDummyable` | **Blocked** — needs `MultiblockHandlerXR` + `IPersistentNBT` |
| `Substation` | `substation` | `TileEntitySubstation` (core) / `TileEntityProxyConductor` (fill positions) | `extends BlockDummyable` | **Blocked** — same as `PylonLarge` |

Companion TEs in `tileentity/network/energy` (8 files): `TileEntityCableBaseNT` (the conductor
root — every cable/wire/box TE either is one or extends one), `TileEntityPylonBase` (abstract:
pylon-to-pylon wire linking — `addConnection`/`disconnectAll`/static `canConnect(first, second)`
distance+type check via `getMaxWireLength()`/`getConnectionPoint()`, dye-based `setColor`),
`TileEntityPylon` (single-block pylon, `ConnectionType.SINGLE`, pairs with `PylonRedWire`),
`TileEntityPylonLarge` (`ConnectionType.QUAD`, 4 mount points, rotates by `BlockDummyable` meta),
`TileEntityPylonMedium` (sibling multiblock pylon tier — lives under plain `blocks/network`, not
`blocks/network/energy`, per `docs/phase1/blocks_network_rail.md`'s inventory; not one of this
report's named classes but shares `TileEntityPylonBase` and the same `BlockDummyable` blocker),
`TileEntitySubstation` (`ConnectionType.QUAD`, overrides `createNode()` to register 5 `BlockPos`
positions — the core plus 4 corner fill blocks — as one logical node), `TileEntityCableSwitch`
(redstone on/off gate on node creation), `TileEntityMachineDetector` (paired with
`PowerDetector`, not read in full).

**Pylon-to-pylon wire linking is genuinely part of this area's scope, not the items package**:
`TileEntityPylonBase.addConnection`/`disconnectAll`/`canConnect` are called from
`com.hbm.items.tool.ItemWiring` (confirmed via grep — `ItemWiring` right-clicks a source pylon
then a target pylon, calls `TileEntityPylonBase.canConnect(thisPylon, targetPylon)` to validate
type-match/distance/self-connect, then `addConnection`). `docs/phase1/items_tool.md` already
flagged `ItemWiring` as a "Phase 2 machine-coupling item" without naming this exact TE method —
this report closes that loop: whichever Phase 2 work-package ports the cable/pylon TEs must also
port `ItemWiring` (and `ItemWrench`, which also touches `addConnection` per the same grep) in the
same slice, or pylons will be placeable but permanently unlinkable.

## Deferred scope

- **`PylonLarge` and `Substation` (both `extends BlockDummyable`)**: blocked on
  `com.hbm.handler.MultiblockHandlerXR` (space-check/fill-space multiblock placement) and
  `com.hbm.tileentity.IPersistentNBT` (core-block NBT restore on placement,
  `BlockDummyable.java:237` — `IPersistentNBT.restoreData(level, corePos, stack)`). Confirmed:
  neither class exists anywhere in the port yet (`grep` came up empty for both). `BlockDummyable`
  itself **is already ported** (`src/main/java/com/hbm/blocks/BlockDummyable.java`, 481 lines,
  faithfully preserves CE's packed-meta rotation/orphan/core encoding per its own header comment)
  — it is only the two referenced classes that are missing. This is exactly the gap
  `docs/phase0/STATUS.md` calls out under "Known gaps intentionally deferred to later phases," and
  it blocks every `BlockDummyable` machine in Phase 2, not just these two. Do not re-solve it in
  this area's work package; treat it as a shared Phase 2 prerequisite (likely its own first slice).
- **`CableDiode`'s GUI** (`IGUIProvider provideGUI` → `GUIDiode`, a limit/priority-setting screen
  sent via `IControlReceiver.receiveControl`): needs an `AbstractContainerMenu`+`Screen` pair.
  Confirmed absent — no `AbstractContainerMenu` reference anywhere in `src/main/java/com/hbm`, and
  `com.hbm.tileentity.IGUIProvider` itself doesn't exist in the port. Matches
  `docs/phase1/blocks_network_rail.md`'s own flag that 5 files in the wider `blocks/network`
  package need a GUI/menu framework as a shared prerequisite. The TE's energy-side logic
  (`transferPower`, `getReceiverSpeed`, priority/limit fields, NBT read/write) has no menu
  dependency and can port now; only `provideGUI`/`provideContainer` wait on that framework.
- **`BlockCableGauge`'s OpenComputers integration** (`TileEntityCableGauge implements
  SimpleComponent`, plus CE's own `IRORValueProvider`): OpenComputers has no NeoForge 1.21.1
  release and no compat layer exists anywhere in this port's scope (not mentioned in any Phase
  0/1 doc). Recommend dropping this integration outright rather than deferring it — flag for an
  explicit call, don't silently port dead code against a mod that can't load.
- **`BlockCablePaintable`'s paint/facade mechanic** (`IFacade`, color state, `IToolable`
  wrench interaction): shares state/behavior with whichever area owns `com.hbm.items.util` paint
  tools (screwdriver/paint can) — `docs/phase1/blocks_network_rail.md` flagged this exact
  cross-reference for the sibling `FluidDuctPaintable`/`PneumoTubePaintableBlock` classes without
  resolving it. Still open; confirm with that area whether the paint color is TE NBT (this area's
  problem) before scheduling.
- **`WireCoatedRadResistant`'s `IRadResistantBlock`**: a radiation-package interface
  (`com.hbm.handler.radiation.*` is Phase 4 per `docs/phase0/STATUS.md`, but this specific marker
  interface may be a lighter Phase-0/1-adjacent contract — not independently verified in this
  pass; confirm it's ported, or stub it, before this one block compiles).
- **Forge/NeoForge Energy (FE) bridging** (`TileEntityCableBaseNT.refreshFENeighbors`/
  `handleFETransfers`, gated by `GeneralConfig.autoCableConversion` — **confirmed present** in the
  port's config, `defineInRange`d at `GeneralConfig.java:310-314`): the underlying capability
  wrappers (`NTMEnergyCapabilityWrapper`, `NTMCableEnergyCapabilityWrapper`) are already ported and
  compile against NeoForge's `IEnergyStorage`. Porting this half of `TileEntityCableBaseNT` is
  capability-*registration* work (a `RegisterCapabilitiesEvent` handler exposing
  `NTMCableEnergyCapabilityWrapper`/`NTMEnergyCapabilityWrapper` for the relevant block entity
  types) — confirmed possible, not confirmed *how this port's convention wires it up* (no existing
  `RegisterCapabilitiesEvent` usage found anywhere in `src/main/java/com/hbm` to copy from). Flag as
  an open question, not a blocker: the HE-only path works with zero capability registration.

## Key design/API decisions

- **`BlockEntity` tick lifecycle (confirmed)**: NeoForge's `BlockEntity` has no `ITickable`
  equivalent. Ticking is wired through `Block`/`BaseEntityBlock` overriding
  `<T extends BlockEntity> BlockEntityTicker<T> getTicker(Level, BlockState, BlockEntityType<T>)`
  and returning a ticker lambda — confirmed already in this port's own code
  (`com.hbm.blocks.generic.DecoBlockAlt.getTicker`, returns `null` off the client-tick guard,
  otherwise a lambda calling `pulse.tick(lvl, pos)`) and independently confirmed against the
  Neo Edition reference's own convention (`com.hbm.blockentity.network.CableBaseBlockEntity`
  implements a local marker interface `com.hbm.blockentity.ITickable` with an `updateEntity()`
  method, dispatched by whatever ticker wiring that project uses — Neo Edition is read here only
  for the shape, not copied for content). Recommend: `TileEntityCableBaseNT.update()` (and every
  other `ITickable` TE in this area) becomes a plain method invoked from a `getTicker()` lambda on
  the owning `Block`, guarded `level.isClientSide` exactly as CE's own `if (!world.isRemote)` guard
  already does.
- **Removal hook (confirmed)**: CE's `TileEntityLoadedBase`/`TileEntityCableBaseNT` override
  1.12's `invalidate()` to call `Nodespace.destroyNode(world, pos)`. NeoForge's equivalent,
  confirmed via the same Neo Edition `CableBaseBlockEntity` reference, is overriding
  `setRemoved()` (call `super.setRemoved()` first, then run the node-teardown). Every TE in this
  area that currently overrides `invalidate()` needs that one-line rename plus the `world`→`level`
  field rename that comes with the `BlockEntity` base class generally.
- **`BlockEntityType` registration (confirmed, already established in this port)**: follow
  `com.hbm.blocks.generic.GenericCrateBlocks`'s existing pattern exactly — a `DeferredRegister<
  BlockEntityType<?>>`-backed `Supplier<BlockEntityType<T>>` field per TE class, built via
  `BlockEntityType.Builder.of(Constructor::new, blockSupplier.get()).build(null)`. CE's
  `@AutoRegister`-annotation reflection scan (seen on every TE this report reads, e.g.
  `TileEntityCableBaseNT`, `TileEntityPylonLarge`, `TileEntitySubstation`,
  `TileEntityProxyConductor`) has no NeoForge equivalent and should **not** be ported — it's
  exactly the manual-registration pattern the `DeferredRegister` convention already replaces.
- **Dynamic connection-mask block state (confirmed portable, no forward dependency)**: CE's
  `BlockCable`/`PowerCableBox` recompute a 6-bit "which side has a matching neighbor" mask
  per-block via `IExtendedBlockState`/`IUnlistedProperty<Boolean>` (a 1.12 Forge-only mechanism,
  already flagged as having no NeoForge equivalent by `docs/phase1/blocks_network_rail.md`'s
  `SimpleUnlistedProperty` note). The replacement is ordinary NeoForge blockstate mechanics: six
  `BooleanProperty`s (`POS_X`/`NEG_X`/... — CE's own property names, keep them) added to
  `BlockStateDefinition`, recomputed in `updateShape`/on neighbor-changed and pushed via
  `level.setBlock` with the updated state, exactly mirroring what
  `TileEntityCableBaseNT.invalidateConnectionCache()` + `BlockCable.getActualState` already do
  conceptually (cache the mask on the TE, recompute lazily, invalidate on neighbor change) — this
  needs no new design, just a mechanical swap from "unlisted property computed at render time" to
  "listed property computed at neighbor-changed time," which is strictly easier in 1.21.
- **`com.hbm.util.Compat` is a confirmed, real, already-referenced gap**: Phase 0's own
  `IEnergyReceiverMK2.trySubscribe` (already ported, already in the tree) calls
  `Compat.getBlockEntityStandard(level, pos)`, and CE's `CableDiode.TileEntityDiode.transferPower`
  calls the CE-side equivalent `Compat.getTileStandard(...)`. `com.hbm.util.Compat` does not exist
  anywhere in the port yet (only one other file, `HazardTransformerRadiationME`, references it —
  both are forward references, not regressions). This is a small utility class (likely just a
  null/unloaded-chunk-safe `getBlockEntity` wrapper judging by call sites) but it is a real,
  concrete prerequisite for this area to compile, not a hypothetical one.

## Open questions / risks

- **`IPersistentNBT` package-naming decision** (already flagged by `docs/phase0/STATUS.md` as
  needing an explicit call "before Phase 2 block entities land," and directly relevant here):
  CE has it under `com.hbm.tileentity`; the Neo Edition reference renamed the entire tile-entity
  layer to `com.hbm.blockentity`. **This port has already made a de-facto choice**: the already-
  ported `BlockDummyable.java` imports `com.hbm.tileentity.IPersistentNBT` (not
  `com.hbm.blockentity.IPersistentNBT`), and no `com.hbm.blockentity` package exists anywhere in
  `src/main/java/com/hbm`. If that import is intentional, this area's TEs should land under
  `com.hbm.tileentity.network.energy` (matching CE's path exactly, consistent with the port's
  general "preserve package layout where legal" rule) rather than adopting Neo Edition's renamed
  layout. Flagging explicitly per the task's instruction rather than silently assuming — someone
  should confirm this is a deliberate choice and not just an unedited forward reference copied
  from CE source during Phase 0's interface pass.
- **No `com.hbm.tileentity.TileEntityLoadedBase` equivalent exists yet**, and it is a much bigger
  shared dependency than this area alone: every TE this report reads extends it (directly or via
  `TileEntityCableBaseNT`/`TileEntityPylonBase`), and CE's version bundles several cross-cutting
  concerns beyond `ILoadedTile` (already ported as a bare marker interface) —
  custom `ByteBuf`-based delta sync (`serialize`/`deserialize`/`serializeInitial`/
  `deserializeInitial`/`networkPackNT`/`networkPackMK2`, backed by `com.hbm.packet.toclient.BufPacket`
  + `com.hbm.handler.threading.PacketThreading`, both unconfirmed in this port), the
  muffled/tilted "machine gravity" mechanic (`checkTilt`, gated by `GeneralConfig.enableMachineGravity`/
  `enable528MachineGravity` — both presumably already-ported config flags, not verified here), and
  the update-tag/data-packet plumbing NeoForge's `BlockEntity` reshapes anyway
  (`getUpdateTag`/`getUpdatePacket` are different methods with different signatures in 1.21 —
  confirmed by `BlockLoot.LootBlockEntity` in this port's own `blocks/generic` package, which
  already implements `getUpdateTag(HolderLookup.Provider)` and
  `getUpdatePacket()` returning `ClientboundBlockEntityDataPacket.create(this)`). **Recommendation**:
  treat porting a `TileEntityLoadedBase`-equivalent base `BlockEntity` class as a shared Phase 2
  prerequisite decided once, not something this area's implementation should invent unilaterally —
  it will be reused by every other `tileentity/*` package Phase 2 touches.
- **Menu/GUI framework absence confirms `docs/phase1/blocks_network_rail.md`'s own finding**: no
  `AbstractContainerMenu` exists anywhere in this port yet. `CableDiode` is the only one of the
  five named classes that needs one (for its limit/priority screen); everything else in this
  report's Phase-2-safe list needs zero GUI work.
- **`RegisterCapabilitiesEvent` wiring pattern is unconfirmed**: the FE-bridge capability wrapper
  classes exist and compile standalone, but no code anywhere in `src/main/java/com/hbm` actually
  registers a capability provider for a block entity type yet (no `RegisterCapabilitiesEvent`
  subscriber found). Whoever implements `TileEntityCableBaseNT`'s FE-bridge half will be the first
  area to establish that pattern in this port — not a blocker for the HE-only network graph, but
  worth flagging so it isn't invented three different ways across Phase 2's several packages that
  all want an FE bridge (fluid ducts, pneumo-tubes likely have the same need per
  `docs/phase1/blocks_network_rail.md`'s broader `blocks/network` survey).
- **`TileEntityMachineDetector`, `TileEntityCablePaintable`, and `PowerDetector`'s exact behavior
  were not read in full** in this pass (out of the five explicitly named classes) — the table
  above places them by inheritance/coupling evidence (`createNewTileEntity` call sites, `extends`
  chains) but a later implementation pass should read them directly rather than relying on this
  report's inference for their internals.
- **This sandbox cannot run `gradlew`** (network access to `maven.neoforged.net` is blocked) — all
  findings above are from direct source reading, not compiler verification. Nothing about that
  affects this being a read-only research task; noting it only so a later implementation phase
  doesn't expect a green build check from this report.
