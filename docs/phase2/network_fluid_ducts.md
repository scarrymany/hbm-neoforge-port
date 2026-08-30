# `FluidDuctBase` + pipe-network TileEntities (blocks/network fluid family)

Sources:
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/network/{FluidDuctBase,FluidDuctStandard,
  FluidCounterValve,FluidDuctPaintableBlockExhaust,FluidDuctBox,FluidDuctBoxExhaust,FluidPipeAnchor,
  FluidSwitch,FluidDuctPaintable,FluidValve,FluidDuctGauge,IBlockFluidDuct}.java` (full read of the
  base + `IBlockFluidDuct`; superclass/`createNewTileEntity` read on every subclass)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/network/{TileEntityPipeBaseNT,
  TileEntityPipelineBase,TileEntityFluidValve,TileEntityFluidCounterValve,TileEntityPipeAnchor,
  ICachedPipeConnections}.java` (full or targeted reads)
- `upstream/hbm-ce/src/main/java/com/hbm/api/fluidmk2/*.java` (all 12 files: `FluidNetMK2`,
  `FluidNode`, `IFluidUserMK2`, `IFluidConnectorMK2`, `IFluidConnectorBlockMK2`, `IFluidPipeMK2`,
  `IFluidReceiverMK2`, `IFluidProviderMK2`, `IFluidStandardReceiverMK2`, `IFluidStandardSenderMK2`,
  `IFluidStandardTransceiverMK2`, `IFillableItem`, `IFluidRegisterListener`)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/fluid/tank/FluidTankNTM.java` (full read, 504
  lines)
- `upstream/hbm-ce/src/main/java/com/hbm/uninos/{UniNodespace,GenNode}.java`,
  `com/hbm/tileentity/{IFluidCopiable,IConnectionAnchors}.java`, `com/hbm/lib/Library.java`
  (`canConnectFluid`)
- This port's `src/main/java/com/hbm/uninos/{NodeNet,GenNode,UniNodespace,INetworkProvider}.java`,
  `com/hbm/api/energymk2/{PowerNetMK2,Nodespace,IEnergyReceiverMK2}.java` (already-ported generic
  graph engine + its one other live consumer, HE energy, read for structural comparison),
  `com/hbm/inventory/fluid/{FluidType,FluidStack,Fluids}.java`, `com/hbm/api/fluidmk2/
  IFluidRegisterListener.java`, `com/hbm/capability/{ModCapabilities,NTMFluidHandlerWrapper,
  NTMFluidCapabilityHandler}.java`
- `docs/phase1/blocks_network_rail.md` (Phase 1's own triage of this package, read first per task
  instructions), `docs/phase1/items_tool.md` (structural example, and source of `ItemWrench`/
  `ItemFluidIDMulti` cross-references), `docs/phase1/items_food_gear.md` (`JetpackGlider`'s
  `FluidTankNTM` NBT dependency), `docs/phase0/STATUS.md` (open decisions section),
  `docs/phase2/{blockentity_base,gui_framework}.md` (both already-written Phase 2 prerequisite
  research this report builds on rather than re-deriving)
- `upstream/neo-edition` grepped for `Capabilities.FluidHandler`/`getCapability`/
  `BlockCapabilityCache` (API-shape cross-check only — see Open questions, this came back empty)

## Headline finding

The task's framing needs one correction and one addition, in the same spirit as
`blockentity_base.md`'s own headline finding:

- **`FluidDuctBase` has 9 direct subclasses, not ~8, plus one second-level subclass** —
  `FluidDuctStandard`, `FluidCounterValve`, `FluidDuctPaintableBlockExhaust`, `FluidDuctBox`,
  `FluidPipeAnchor`, `FluidSwitch`, `FluidDuctPaintable`, `FluidValve`, `FluidDuctGauge` all
  `extend FluidDuctBase` directly; `FluidDuctBoxExhaust extends FluidDuctBox` (one level deeper).
  **10 concrete registrable blocks total** in the family, paired with **8 distinct TileEntity
  classes** (some TEs are shared by two blocks): `TileEntityPipeBaseNT` (default, used by
  `FluidDuctStandard` and `FluidDuctBox`), `TileEntityFluidValve` (used by both `FluidValve` *and*
  `FluidSwitch`), `TileEntityFluidCounterValve`, `TileEntityPipeExhaustPaintable`,
  `TileEntityPipePaintable`, `TileEntityPipeGauge`, `TileEntityPipeExhaust`
  (`FluidDuctBoxExhaust`), and `TileEntityPipeAnchor extends TileEntityPipelineBase` (an
  intermediate abstract TE class, not itself instantiated, that `FluidPipeAnchor` uses via the
  newer `createTileEntity(World, IBlockState)` override rather than `FluidDuctBase`'s
  `createNewTileEntity(World, int)` default). None of the 10 blocks need `BlockDummyable` or a
  GUI/menu (confirmed: neither `TileEntityFluidValve` nor `TileEntityFluidCounterValve` implements
  `IGUIProvider` — the 5-file `IGUIProvider` list `blocks_network_rail.md` already found in this
  package, `FluidPump`/`RadioTorch*`, is entirely outside the `FluidDuctBase` family).
- **The flood-fill / connection-discovery algorithm this task asked to map is not new code to
  write.** It already exists, already ported, already exercised by a second live consumer. CE's
  `FluidNetMK2 extends NodeNet<IFluidReceiverMK2, IFluidProviderMK2, FluidNode, FluidNetMK2>` is a
  thin, fluid-specific subclass of the exact same generic graph engine HE energy already uses
  (`PowerNetMK2 extends NodeNet<IEnergyReceiverMK2, IEnergyProviderMK2, Nodespace.PowerNode,
  PowerNetMK2>`, already ported in this port's `com.hbm.api.energymk2` package). The generic engine
  itself — `com.hbm.uninos.{NodeNet, GenNode, UniNodespace, INetworkProvider}` — is **already
  ported in this port**, byte-for-byte structurally identical to CE's version (diffed directly: the
  only changes are `TileEntity`→`BlockEntity` and `isInvalid()`→`isRemoved()`). The incremental
  BFS/connected-components logic that answers "did placing/breaking this pipe join or split a
  network" lives entirely in `UniNodespace.PerTypeNodeManager.{checkNodeConnection, connectToNode,
  splitNetIfNecessary, collectComponent}` (already ported) — porting `FluidNetMK2`/`FluidNode`
  themselves is a mechanical ~200-line task of plugging fluid-specific types into an engine that
  already works, not an algorithm-design task.
- **This port's own already-committed code already forward-references every fluid-network class
  this package needs to deliver**, exactly the same pattern `blockentity_base.md` found for the
  energy side. `com.hbm.inventory.fluid.FluidType.java` (ported, Phase 0) line 180-184 already has
  `protected INetworkProvider<FluidNetMK2> NETWORK_PROVIDER = () -> new FluidNetMK2(this);` and
  `getNetworkProvider()` returning it — `FluidNetMK2` does not exist in the port yet.
  `com.hbm.capability.NTMFluidHandlerWrapper` and `NTMFluidCapabilityHandler` (both ported, Phase
  0) import and use `IFluidReceiverMK2`, `IFluidProviderMK2`, `IFluidUserMK2`, and
  `FluidTankNTM` — none of the four exist in the port yet either. The port's `com.hbm.api.fluidmk2`
  package currently holds exactly one of CE's 12 files (`IFluidRegisterListener`). This is not a
  new discovery so much as independent confirmation from a second angle (this task's own sources)
  of the same gap `blockentity_base.md` already flagged from the block-entity angle: **the fluid
  tank/network API layer is a real, load-bearing, currently-missing prerequisite that multiple
  already-ported classes are already waiting on**, not a nice-to-have.
- **CE's own design already has a foreign-mod capability bridge, in both directions**, and it is
  not this port's job to invent one — only to re-target it at NeoForge's capability lookup API
  instead of Forge 1.12's per-`TileEntity` `getCapability` override. `IFluidReceiverMK2
  .pullFromForeignHandler` and `IFluidStandardSenderMK2.pushToForeignHandler` (both default
  methods, both read in full) already do exactly this: when a pipe's neighbor is not an
  `IFluidConnectorMK2`/`IFluidReceiverMK2` (i.e. not one of HBM's own pipes/machines), CE falls
  back to simulate-then-commit `fill`/`drain` calls against the neighbor's vanilla Forge
  `IFluidHandler` capability, at pressure 0 only (a plain `FluidStack` has no pressure field).
  This is the exact shape of "capability bridge" the task asked about — it is CE content to port,
  not a novel design this package must invent, though the capability *lookup* call itself needs a
  confirmed NeoForge 1.21.1 replacement (see Open questions — this is the one piece that could not
  be confirmed against real usage anywhere in this repo).

## Phase-2-safe scope

### The `FluidDuctBase` block family (10 blocks, all `BlockContainer`-shaped, no multiblock, no GUI)

| CE class | Superclass | Paired TileEntity | Notes |
|---|---|---|---|
| `FluidDuctBase` (abstract) | `BlockContainer implements IAnalyzable, IBlockFluidDuct` | `TileEntityPipeBaseNT` (default `createNewTileEntity`) | Owns `changeTypeRecursively` (the *other* flood fill, see below) and `getDebugInfo` (reads `FluidNetMK2.links/receiverEntries/providerEntries/fluidTracker` for `ItemAnalyzer`) |
| `FluidDuctStandard` | `FluidDuctBase` | `TileEntityPipeBaseNT` | Plain connector duct |
| `FluidDuctBox` | `FluidDuctBase` | `TileEntityPipeBaseNT` | Larger-footprint duct variant |
| `FluidDuctBoxExhaust` | `FluidDuctBox` (2nd-level) | `TileEntityPipeExhaust extends TileEntity implements IFluidPipeMK2, ITickable, ICachedPipeConnections` — **does not extend `TileEntityPipeBaseNT`**, ticks independently | Exhaust/vent variant |
| `FluidCounterValve` | `FluidDuctBase` | `TileEntityFluidCounterValve extends TileEntityPipeBaseNT implements ITickable, IRORValueProvider, IRORInteractive, SimpleComponent, CompatHandler.OCComponent` | Also implements OpenComputers integration (mod-integration concern, separable/deferrable) |
| `FluidValve` | `FluidDuctBase` | `TileEntityFluidValve extends TileEntityPipeBaseNT` | Redstone-gated on/off; `shouldCreateNode()` returns `getBlockMetadata() == 1` |
| `FluidSwitch` | `FluidDuctBase` | `TileEntityFluidValve` (**same TE class as `FluidValve`**) | |
| `FluidDuctPaintable` | `FluidDuctBase implements IToolable, ILookOverlay, IDynamicModels, ITooltipProvider, IFacade` | `TileEntityPipePaintable` | Colorable via paint tool (`IFacade`) |
| `FluidDuctPaintableBlockExhaust` | `FluidDuctBase` (same interface set as above) | `TileEntityPipeExhaustPaintable` | Paintable exhaust variant |
| `FluidDuctGauge` | `FluidDuctBase implements ILookOverlay, ITooltipProvider, IDynamicModels, INBTBlockTransformable` | `TileEntityPipeGauge` | Read-only fill-level display duct |
| `FluidPipeAnchor` | `FluidDuctBase implements ITooltipProvider, ILookOverlay, IDynamicModels, ICustomBlockItem, IBlockSpecialPlacementAABB` | `TileEntityPipeAnchor extends TileEntityPipelineBase extends TileEntityPipeBaseNT` (via the newer `createTileEntity(World, IBlockState)` override, not the default) | Long-distance wrench-linked anchor — see connection model below |

All 10 are simple `BlockContainer`s (no `BlockDummyable`), matching `blocks_network_rail.md`'s
structural table exactly. None implement `IGUIProvider`. None need the Menu/Screen framework
`docs/phase2/gui_framework.md` designs. **This is genuinely one of the lighter-weight Phase 2
sub-packages once its two real prerequisites (below) land.**

### The flood-fill / connection-discovery algorithm — two distinct mechanisms, both already covered

CE runs two separate "flood fill"-shaped mechanisms for fluid ducts, and it matters that they stay
distinct in the port:

1. **Logical network graph (drives actual fluid transfer)** — `IFluidPipeMK2.createNode(FluidType)`
   (a default method) builds a `FluidNode` (a `GenNode<FluidNetMK2>`) with 6 `DirPos` connections,
   one per `EnumFacing`/`Direction`, each just "the position one block over in that direction, no
   side-check" (`Library.POS_X`/`NEG_X`/etc.). `UniNodespace.createNode`/`.getNode` register it into
   a `World`-scoped `PerTypeNodeManager<IFluidReceiverMK2, IFluidProviderMK2, FluidNode,
   FluidNetMK2>` — **one manager per `INetworkProvider`, and each `FluidType` has its own
   `INetworkProvider<FluidNetMK2>` instance** (`FluidType.NETWORK_PROVIDER = () -> new
   FluidNetMK2(this)`), so water pipes and steam pipes never share a network graph even if
   physically adjacent — the graph is partitioned by fluid type, not just by block adjacency. The
   actual connectivity check/merge/split logic (`checkNodeConnection`, `connectToNode`,
   `splitNetIfNecessary`/`collectComponent` — an incremental connected-components BFS run only on
   the neighborhood of a changed node, not a full-graph rescan) is 100% generic, already ported,
   and requires zero fluid-specific code to reuse. `TileEntityPipeBaseNT.update()` (read in full)
   shows the TE side of this is equally thin: on the first server tick after load, if it has no
   valid net, ask `UniNodespace.getNode` for an existing node at its own position or create one and
   register it — that's the entire per-tile contribution.
   - `FluidPipeAnchor`/`TileEntityPipelineBase` is the one exception to pure 6-directional adjacency:
     its `createNode` builds connections from an explicit `List<int[]> connected` populated by
     `addConnection(x,y,z)` (called by `ItemWrench`, per `docs/phase1/items_tool.md`'s bucket-(c)
     cross-reference) using `ForgeDirection.UNKNOWN` as the direction — i.e. two distant anchors can
     be wrench-linked into the same net without being face-adjacent at all. The generic engine
     handles this transparently since `checkConnection`'s side-check is skippable
     (`skipSideCheck`/`UNKNOWN` handling already present in the ported `UniNodespace`).
2. **Visual/type-propagation flood fill (cosmetic, bounded, separate from the graph above)** —
   `IBlockFluidDuct.changeTypeRecursively(World, BlockPos, FluidType prevType, FluidType type, int
   loopsRemaining)`, implemented once in `FluidDuctBase` and called recursively into each of the 6
   neighbors that are also `IBlockFluidDuct` instances, decrementing a loop budget (CE caller in
   `TileEntityPipeBaseNT.pasteSettings` passes `64`). This is *not* the network graph — it exists so
   that right-clicking one empty duct with a wrench (holding Ctrl) re-paints an entire connected run
   of not-yet-typed ducts to match, without needing to wait for the graph to form. `Library
   .canConnectFluid(IBlockAccess, pos, dir, FluidType)` (used both here, indirectly, via
   `computeConnectionMask`, and directly by `TileEntityPipeBaseNT`'s render-only
   `cachedConnectionMask`) is the per-neighbor adjacency test for *this* mechanism: checks
   `IFluidConnectorBlockMK2.canConnect` (block-level, e.g. a machine's fixed input) or
   `IFluidConnectorMK2.canConnect` (TE-level) on the neighbor, entirely independent of whether a
   `FluidNetMK2` node exists yet.

Both mechanisms are Phase-2-safe to port as-is: neither has a Phase 3/4/5 dependency, and the
generic engine both graph types plug into already compiles in this port today (modulo the
`fluidmk2` interfaces themselves, next section).

### The `com.hbm.api.fluidmk2` interface layer + `FluidTankNTM` (the real prerequisite, claimed here)

The task calls `com.hbm.inventory.fluid.*` "already-ported" — true for the fluid *type registry*
(`FluidType`, `FluidStack`, `Fluids`, the `FT_*` trait classes, all present in
`src/main/java/com/hbm/inventory/fluid`), **false for the fluid *tank* abstraction**
(`com.hbm.inventory.fluid.tank.FluidTankNTM`, 0 files ported) and **false for the network-facing
API layer** (`com.hbm.api.fluidmk2`, 1 of 12 CE files ported). Both `blockentity_base.md` and
`gui_framework.md` already flagged this gap from their own angles and explicitly declined to claim
it ("not yet run as its own research area" / "this GUI package does not re-solve that gap"). This
report claims it, because `FluidDuctBase`'s entire TE family cannot compile without it — it is the
direct, load-bearing, blocking dependency of this specific package, not a generic cross-cutting
concern like `RecipesCommon`.

What needs porting, all mechanical (types and control flow only, no design decisions beyond the
one flagged in Open questions):

- `com.hbm.inventory.fluid.tank.FluidTankNTM` (504 lines in CE) — strip the legacy
  `IFluidHandler`/`IFluidTank` interface implementation (superseded by `NTMFluidHandlerWrapper`,
  already ported, which wraps a *block entity's* tanks as the modern capability rather than the
  tank itself implementing it) and the raw `GL11`/`Tessellator` rendering methods
  (`renderTank`/`renderTankInfo` — client-side concern, flagged separately by
  `gui_framework.md`'s Open questions as needing a `GuiGraphics`/`RenderSystem` rewrite once a
  concrete machine needs it). What must survive: the plain data fields and accessors
  (`getTankType`/`setTankType`, `getFill`/`setFill`, `getMaxFill`, `getPressure`/`withPressure`,
  `conform(FluidStack)`, `changeTankSize`), the NBT/`ByteBuf` (de)serialization pair
  (`writeToNBT`/`readFromNBT`, `serialize`/`deserialize`), and the `fill(FluidType, int, boolean)`
  simulate-or-commit method — this is exactly the surface `NTMFluidHandlerWrapper`'s own javadoc
  already assumes ("mirroring NeoForge's own `IFluidTank` convention... once the fluid/inventory
  area ports it").
- `com.hbm.api.fluidmk2.{IFluidUserMK2, IFluidConnectorMK2, IFluidConnectorBlockMK2, IFluidPipeMK2,
  IFluidReceiverMK2, IFluidProviderMK2, IFluidStandardReceiverMK2, IFluidStandardSenderMK2,
  IFluidStandardTransceiverMK2, FluidNode, FluidNetMK2}` — all 10 remaining files. Every one of
  these is a direct-port, default-method-heavy interface with no rendering/client code and no
  forward dependency on anything *else* missing except each other, `FluidTankNTM`, and the
  already-ported `com.hbm.uninos`/`com.hbm.api.energymk2.IEnergyReceiverMK2.ConnectionPriority`
  (fluid priority reuses HE's own priority enum — confirmed:
  `IFluidReceiverMK2.getFluidPriority()` returns `IEnergyReceiverMK2.ConnectionPriority`, already
  ported). `IFillableItem` (item-side fluid interface, used by canisters/jetpacks per
  `docs/phase1/items_food_gear.md`'s `JetpackGlider` note) is item-scoped and can be ported
  alongside trivially, though its consumers are a different package's concern.
- `com.hbm.tileentity.{IFluidCopiable, IConnectionAnchors}` and
  `com.hbm.tileentity.network.ICachedPipeConnections` — small interfaces `TileEntityPipeBaseNT`
  needs. `IConnectionAnchors.notifyAnchors(TileEntity)` is a static helper CE's fluid-type-change
  path calls after `setType`; ports mechanically once the block-entity base class
  (`docs/phase2/blockentity_base.md`) lands, since its signature needs to move from `TileEntity` to
  `BlockEntity`.

None of this needs `RecipesCommon`, the multiblock framework, or the Menu/Screen framework. It
needs exactly one thing from elsewhere in Phase 2: the block-entity base class package
(`docs/phase2/blockentity_base.md`'s `LoadedBaseBlockEntity`), since `TileEntityPipeBaseNT extends
TileEntityLoadedBase` in CE and every TE in this family is `ITickable` (server-tick-driven node
creation/connection maintenance).

### Capability interop with the port's NeoForge fluid-handler bridge (already exists, one direction wired)

This port already has exactly the wrapper class CE's own design implies is needed:
`com.hbm.capability.NTMFluidHandlerWrapper implements IFluidHandler` (ported, Phase 0) wraps a
`BlockEntity`'s `IFluidReceiverMK2`/`IFluidProviderMK2`/`IFluidUserMK2` surface as NeoForge's
modern `IFluidHandler`, with pressure-aware fill/drain logic ported from CE's own
`NTMFluidHandlerWrapper` (the class exists in CE too — this is not a new invention, confirmed by
its own javadoc: "Ported from CE's `NTMFluidHandlerWrapper`"). It is currently constructed with
`(BlockEntity handler, @Nullable BlockPos accessor)` and is **fully ready to be registered** the
moment a fluid duct/machine `BlockEntityType` exists — but nothing calls
`RegisterCapabilitiesEvent#registerBlockEntity(Capabilities.FluidHandler.BLOCK, ...)` with it yet
(`ModCapabilities.register` currently only wires the **item**-side `Capabilities.FluidHandler.ITEM`
for fluid-container items). This is exactly the same "registration hook exists, block entity to
plug into it doesn't yet" situation `blockentity_base.md` already found for the energy side —
wiring the `registerBlockEntity` call for `Capabilities.FluidHandler.BLOCK` against each fluid-duct
`BlockEntityType` is real Phase-2-safe work for whoever implements this package, once the block
entity itself exists.

## Deferred scope

- **`FluidTankNTM`'s client rendering** (`renderTank`/`renderTankInfo`, raw `GL11`/`Tessellator`)
  — already flagged by `gui_framework.md`'s Open questions as needing a `GuiGraphics`/
  `RenderSystem` rewrite. Not this package's blocker (fluid ducts have no GUI at all), but the
  *data* half of `FluidTankNTM` this package needs and the *rendering* half `gui_framework.md`
  will eventually need are the same class in CE — whoever ports `FluidTankNTM` should split them,
  per that report's own recommendation, and this package only needs the data half.
- **Block-entity base class / `com.hbm.tileentity` vs `com.hbm.blockentity` package-naming
  decision** — this package's every TE (`TileEntityPipeBaseNT` and its 7 sibling/subclasses) is
  blocked on `docs/phase2/blockentity_base.md`'s open decision, exactly as that report already
  flags for every Phase 2 block entity. Not re-litigated here; this package is simply one more
  concrete consumer waiting on the same call. `TileEntityPipeBaseNT extends TileEntityLoadedBase`
  specifically (the lighter of the two bases that report designs — fluid ducts are not
  inventoried/`ItemStackHandler`-owning, so they need `LoadedBaseBlockEntity`, not
  `MachineBaseBlockEntity`).
- **`IToolable`/`ItemWrench` machine-coupling** (`docs/phase1/items_tool.md` bucket (c)): the wrench
  is what calls `TileEntityPipelineBase.addConnection` for anchor-linking and what drives
  `TileEntityPipeBaseNT.pasteSettings`'s Ctrl-click type-flood-fill. The item itself is a separate
  package's scope (already tracked there); this package only needs to expose the TE-side methods
  the wrench calls (`addConnection`, `pasteSettings`, `getType`/`setType`) — all of which are inside
  the CE classes already read above and require no additional design.
- **`ItemAnalyzer`/`IAnalyzable.getDebugInfo`** (`docs/phase1/items_tool.md` bucket (c),
  `ItemAnalyzer`): `FluidDuctBase.getDebugInfo` (already read above) is this package's side of that
  contract and needs no additional work beyond the fluid-network classes already in scope here; the
  item itself is a different package's job.
- **OpenComputers integration** (`TileEntityFluidCounterValve implements SimpleComponent,
  CompatHandler.OCComponent`): a third-party mod-integration surface, `@Optional`-annotated in CE.
  Recommend deferring/stubbing per this port's general mod-integration posture (not otherwise
  established in any prior Phase 0/1/2 report read for this survey — flagging as a small
  unaddressed integration-policy question, not specific to this package).
- **`FluidPump` and the `energy/` sibling package** (`BlockCableGauge`, `CableDiode`, etc.) —
  `blocks_network_rail.md` already scoped these separately (the `IGUIProvider`-bearing and
  `BlockDummyable`-based files in `blocks/network`); out of scope for this `FluidDuctBase`-focused
  report.
- **World-fluid blocks**: per Phase 1's own research (re-confirmed here), this port has no
  world-placed fluid-block system, and nothing in the `FluidDuctBase` family needs one — CE's fluid
  ducts move fluid entirely through the `FluidNetMK2` graph and `FluidTankNTM` tank objects inside
  TEs, never through a `LiquidBlock` in the world. Not a dependency of this package.
- **Datagen for connection-aware pipe models**: `FluidDuctStandard`/`FluidDuctBox`/`FluidPipeAnchor`
  implement `IDynamicModels`/`ICustomBlockItem`/`IBlockSpecialPlacementAABB`, and
  `FluidDuctPaintable`/`FluidDuctGauge` bake their own `IBakedModel` inner classes
  (`FluidDuctPaintableModel`, `FluidDuctGaugeModel`) that presumably read the render-only
  `cachedConnectionMask`/`SimpleUnlistedProperty` state to choose which pipe-arm quads to emit.
  `blocks_network_rail.md` already flagged `SimpleUnlistedProperty` (Forge 1.12's
  `IUnlistedProperty`) as having no NeoForge 1.21 equivalent. This package can land the block +
  block-entity + network logic with a placeholder/uniform static model now (satisfying "logic
  first" Phase 2 sequencing) and defer the real connection-aware baked-model work to Phase 5
  (Client & UX), consistent with how `blocks_network_rail.md` treated dynamic model baking
  elsewhere in this same package.

## Key design/API decisions

- **The generic graph engine (`com.hbm.uninos.*`) needs no NeoForge-specific redesign for fluid —
  it already ported.** Confirmed by direct diff against CE: the only two changes anywhere in
  `NodeNet.java` between CE and this port are `TileEntity`→`BlockEntity` and
  `isInvalid()`→`isRemoved()`. `FluidNetMK2`/`FluidNode` should be ported the same way HE's
  `PowerNetMK2`/`Nodespace.PowerNode` already were: same generic-parameter shape
  (`NodeNet<IFluidReceiverMK2, IFluidProviderMK2, FluidNode, FluidNetMK2>`), same
  `INetworkProvider<FluidNetMK2>` functional-interface pattern (`FluidType.NETWORK_PROVIDER`, one
  instance per registered fluid type, mirroring `Nodespace.THE_POWER_PROVIDER`'s single static
  instance — the fluid case needs *N* provider instances, one per `FluidType`, not one static
  singleton, since networks must not cross fluid types; this is already exactly how CE's own
  `FluidType.getNetworkProvider()` works, nothing to invent).
- **NBT persistence and sync for these TEs follow `blockentity_base.md`'s confirmed
  `saveAdditional`/`loadAdditional` + `getUpdateTag`/`getUpdatePacket` contract**, not a
  fluid-specific mechanism — `TileEntityPipeBaseNT.serializeInitial`/`deserializeInitial`
  (CE's own initial-sync-payload pair, pushing the duct's `FluidType` on chunk load so client-side
  connection-mask rendering is correct immediately) maps directly onto that already-confirmed
  pattern; no new sync mechanism needs designing for this package.
- **Fluid priority reuses `IEnergyReceiverMK2.ConnectionPriority` directly** (confirmed:
  `IFluidReceiverMK2.getFluidPriority()`'s return type is that HE enum, already ported) — this is
  CE's own choice to share one priority enum across both network types, not something to
  reinvent or "properly" split into a fluid-specific enum during the port.
- **Per-fluid-type network partitioning is load-bearing and must not be simplified.** Because
  `UniNodespace`'s `PerTypeNodeManager` is keyed by `INetworkProvider<N>` identity
  (`Reference2ObjectOpenHashMap<INetworkProvider<?>, PerTypeNodeManager<...>>`, reference-equality
  keyed) and each `FluidType` instance owns its own `NETWORK_PROVIDER` lambda instance, water and
  lava pipes standing directly adjacent never merge into one `FluidNetMK2`, even though they share
  the exact same `IFluidPipeMK2`/`TileEntityPipeBaseNT` Java class. This is CE's actual behavior
  (confirmed by reading `UniNodespace.getManagerFor`, keyed on the `INetworkProvider` reference)
  and must be preserved exactly, not "optimized" into a single shared network keyed by fluid type
  as a secondary field.

## Open questions / risks

- **The foreign-capability bridge direction (pipe → neighbor's vanilla `IFluidHandler`) cannot be
  confirmed against real 1.21.1 usage anywhere in this repo, and should not be guessed at
  implementation time without independent verification.** CE's `pullFromForeignHandler`/
  `pushToForeignHandler` call `TileEntity#hasCapability`/`#getCapability(CapabilityFluidHandler
  .FLUID_HANDLER_CAPABILITY, side)` — a per-instance Forge 1.12 API NeoForge 1.21.1 does not have
  (capabilities are looked up centrally, not queried on the object). `upstream/neo-edition` — the
  only reference this task's ground rules permit for API-shape confirmation — has **zero** hits for
  `Capabilities.FluidHandler`, `getCapability`, or `BlockCapabilityCache` anywhere in its ~981 Java
  files, i.e. Neo Edition never implemented this foreign-mod interop at all, so there is no real
  usage anywhere available to this survey to confirm the block-position capability-lookup call
  shape against (the general NeoForge 1.21 pattern is a `Level#getCapability(BlockCapability<T,
  C>, BlockPos, C context)` static-style lookup via the same `Capabilities` holder class this
  port's own `ModCapabilities.java` already uses for the *item*-side `Capabilities.FluidHandler
  .ITEM` — the `.BLOCK` sibling constant is a reasonable expectation given that pairing, but is
  **not independently confirmed by any usage in this repository**, so flagging rather than writing
  it into "Key design decisions" as settled). Whoever implements this package should verify the
  exact `Capabilities.FluidHandler.BLOCK`/`BlockCapabilityCache` call shape against real NeoForge
  1.21.1 source or documentation before writing `pullFromForeignHandler`'s replacement — this is
  the one piece of this whole area this report could not ground in observed usage.
- **The NeoForge block-entity-side fluid-handler capability is not yet wired to anything**
  (`ModCapabilities.register` has no `registerBlockEntity(Capabilities.FluidHandler.BLOCK, ...)`
  call for any type) — not a blocker for this package's own logic, but means a vanilla hopper/other
  mod's tank sitting next to a fluid duct's *end point* (as opposed to the duct pushing/pulling
  outward via CE's own `pullFromForeignHandler`/`pushToForeignHandler`) cannot yet interact with it
  either, until this package (or `blockentity_base.md`'s package) adds that registration for the
  fluid-duct/machine `BlockEntityType`s once they're registered.
- **`FluidTankNTM` ownership**: this report claims porting `FluidTankNTM` + the 10 remaining
  `com.hbm.api.fluidmk2` files as in-scope for this package, since `FluidDuctBase`'s own TEs cannot
  compile without them. If a separate Phase 2 work-package split assigns fluid tanks to a different
  package (e.g. bundled with "storage: fluid barrels/tanks" per `PORT_SPEC.md`'s own Phase 2
  wording), that package and this one need to agree on who ports `FluidTankNTM` first — it is a
  genuine shared prerequisite between "logistics (fluid ducts)" and "storage (fluid barrels/tanks)"
  per `PORT_SPEC.md`'s own Phase 2 line, and porting it twice would be wasted work. Flagging for
  whoever does the Phase 2 work-package split, not resolving unilaterally here.
- **`TileEntityPipeExhaust` and `TileEntityFluidCounterValve` tick independently of
  `TileEntityPipeBaseNT.update()`'s node-creation gate** (`TileEntityPipeExhaust extends
  TileEntity` directly, implementing `ITickable`/`IFluidPipeMK2` itself rather than inheriting the
  base's `update()`) — worth double-checking at implementation time whether `TileEntityPipeExhaust`
  duplicates the same node-lifecycle logic inline (not fully read in this survey beyond its class
  declaration) rather than assuming it behaves identically to the shared base.
- **`IFacade`/paint-tool interop** (`FluidDuctPaintable`, `FluidDuctPaintableBlockExhaust`) —
  `blocks_network_rail.md` already flagged this cross-package (paint cans live in
  `com.hbm.items.util`) and asked whoever covers that area to confirm whether the color state is TE
  NBT (this package's problem) or `ItemStack` NBT (a componentization problem elsewhere). Not
  resolved by this survey; repeating the flag since this report is the first to actually name the
  two concrete TE classes involved (`TileEntityPipePaintable`, `TileEntityPipeExhaustPaintable`,
  neither read in depth here — out of scope beyond confirming their existence and pairing).
