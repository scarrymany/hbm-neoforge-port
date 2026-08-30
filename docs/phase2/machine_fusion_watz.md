# fusion / ICF / Watz reactor systems triage

Source: `hbm-ce/src/main/java/com/hbm/tileentity/machine/**` — the ICF (inertial confinement
fusion) family, the Watz (isotropic liquid-fuel breeder) family, and the `fusion/` subpackage
(hot-fusion tokamak). 17 files read in full: 4 ICF, 2 Watz, 1 sibling struct helper
(`TileEntityFusionTorusStruct`), and the 10 files under `tileentity/machine/fusion/` (9 concrete
TE classes + 1 interface). Cross-checked against Phase 1's `docs/phase1/items_machine.md` (already
ported `ItemICFPellet`, `ItemWatzPellet`, `ItemFELCrystal`) and against the current on-disk state of
the port (`src/main/java/com/hbm/**`).

## Headline finding

These are three **structurally distinct** multiblock systems that happen to share a directory and
a fuel-item vocabulary. None of the three has *any* block, block entity, GUI/menu, or blocks-side
registration in the port yet — Phase 1 only ported the fuel-item classes (`ItemICFPellet`,
`ItemWatzPellet`, `ItemFELCrystal`) as standalone data-over-`ItemStack` items. This package is
**100% net-new Phase 2 work** on the block/TE/GUI side, layered on top of infrastructure gaps that
are already known (fluid tanks, `RecipesCommon`) plus two gaps this survey newly confirms are
missing in full: the legacy (non-MK2) `com.hbm.api.fluid.*` interface family, and the
`com.hbm.uninos.networkproviders.*` package (only the generic `uninos` base — `NodeNet`,
`UniNodespace`, `GenNode`, `INetworkProvider` — exists in the port; `KlystronNetwork` and
`PlasmaNetwork`, which the fusion torus is built around, do not).

- **ICF** (`TileEntityICF`, `TileEntityICFController`, `TileEntityICFStruct`, `TileEntityICFPress`)
  is a laser-driven cold-fusion reactor: a controller block fires a "laser" (a `long` power value,
  not a rendered raycast weapon) down a line of `icf_component` structure blocks at a reactor core,
  which reacts `ItemICFPellet` fuel under laser power to produce heat, which in turn heats a
  `FluidTankNTM` pair (`SODIUM` → `SODIUM_HOT`) via the `FT_Heatable` fluid trait and slowly fills a
  `STELLAR_FLUX` byproduct tank. `TileEntityICFPress` is a separate, non-multiblock machine that
  *crafts* `ItemICFPellet` stacks from two fuel inputs (fluid or material-shape ingot) plus an
  optional muon-catalyst item.
- **Watz** (`TileEntityWatz`, `TileEntityWatzStruct`) is a vertically-stacked (3-block-tall
  segments) liquid/pellet breeder reactor: each segment holds 24 `ItemWatzPellet` slots, segments
  share three `FluidTankNTM` pools (coolant cold/hot, `WATZ` mud/waste) top-to-bottom every tick,
  and the reaction math (base flux, burn function, absorb function, heat emission, mud byproduct)
  is entirely keyed off `ItemWatzPellet.EnumWatzType` — confirmed **12** variants
  (`SCHRABIDIUM, HES, MES, LES, HEN, MEU, MEP, LEAD, BORON, DU, NQD, NQR`), not 9-10 as a rough
  estimate might suggest; Phase 1 already registered all 12 × 2 (fresh/depleted) = 24 items. Mud
  overflow triggers a violent meltdown (`EntityShrapnel` spray + `mud_block`/`watz_element`/
  `watz_cooler`/`watz_casing` column destruction + an advancement grant), which is real gameplay
  behavior, not a stub.
- **Fusion torus (hot fusion / tokamak)** is the most structurally complex of the three: the core
  `TileEntityFusionTorus` does not draw ignition/output power through `PowerNetMK2` at all for its
  plasma output — it uses two dedicated `com.hbm.uninos.networkproviders` node networks
  (`KlystronNetwork` for ignition energy in, `PlasmaNetwork` for plasma energy out), each a
  `GenNode`-based graph distinct from `PowerNetMK2`/`IEnergyConductorMK2`. Runs a `FusionRecipe`
  (`extends GenericRecipe`) through a `ModuleMachineFusion` helper (`com.hbm.modules.machine`, also
  entirely unported). Output intensity is inversely scaled by connected-receiver count
  (`getOuputIntensity`: 100%/125%/150%/175% split across 1/2/3/4+ simultaneous `IFusionPowerReceiver`
  hookups) — a real balance mechanic, not incidental. Six device types plug into this network as
  `IFusionPowerReceiver` implementors: `TileEntityFusionBoiler` (plasma+water → superheated steam),
  `TileEntityFusionBreeder` (plasma → bred fluid/material output, largest fusion file at 398 lines),
  `TileEntityFusionMHDT` (the actual plasma→HE energy generator, via `IEnergyProviderMK2`),
  `TileEntityFusionPlasmaForge` (plasma-powered forge with an inner `ForgeArm` class, largest single
  file in this survey at 609 lines), `TileEntityFusionCoupler` (short relay/coupler, 117 lines), and
  `TileEntityFusionCollector` (76 lines — its only job is registering as a `PlasmaNetwork` receiver
  to raise the torus's bonus-speed multiplier; no other logic). `TileEntityFusionKlystron` /
  `TileEntityFusionKlystronCreative` are the ignition-energy sources (battery + compressed-air fuel,
  GUI-configurable output target, survival vs. creative-unlimited). `TileEntityFusionTorusStruct`
  is the multiblock-assembly dummy-block validator, keyed off a `MachineFusionTorus.layout[][][]`
  int-array pattern rather than the flat offset lists ICF/Watz structs use.
- **"Hadron" grep result**: no dedicated Hadron Collider TE exists in this directory. The only
  "Hadron" hit is `HbmEffectNT.Hadron`, a particle-effect enum value `TileEntityICF` fires as a
  visual when a pellet reacts — cosmetic, not a distinct machine.
- **Item cross-check**: `ItemICFPellet` (ported, data-component-based) is the correct and only fuel
  item `TileEntityICF`/`TileEntityICFPress` consume; `ItemWatzPellet` (ported, 24 registry entries)
  is the correct and only fuel item `TileEntityWatz` consumes. **`ItemFELCrystal` is a false lead
  for this area** — grepping its consumers in CE shows its real users are `TileEntityFEL` (Free
  Electron Laser) and `TileEntitySILEX` (laser isotope separation), both siblings of this package
  but **not part of it** — see the SILEX/processing_b boundary note below. Two small item-side gaps
  surfaced that ICF needs but Phase 1 didn't port: `ModItems.icf_pellet_empty` (the press's input
  item) and `ModItems.particle_muon` (the press's catalyst item, consumed 1:1 to grant a pellet's
  muon-catalyzed bonus) — neither exists anywhere in the port yet.

## SILEX / laser-isotope-separation boundary (flag only, not ported here)

`TileEntitySILEX` and `TileEntityFEL` both live directly in `com.hbm.tileentity.machine` (siblings
of the ICF/Watz files, not inside the `fusion/` subpackage) and both consume `ItemFELCrystal`
(already ported) plus `ItemLens` (also already ported per Phase 1's items_machine.md). They are
**not** part of the fusion/ICF/Watz multiblocks — SILEX is a laser-driven uranium/isotope enrichment
line that reads `SILEXRecipes`, and FEL is the laser source SILEX depends on. Whichever Phase 2
package owns general processing/enrichment machinery (referred to in the task brief as
"processing_b") should own both `TileEntitySILEX` and `TileEntityFEL` together, since FEL has no
purpose without something to shine its laser at. This report deliberately stops at flagging the
boundary — porting either class is out of scope here.

## Phase-2-safe scope

Nothing in this area can be *fully* implemented without the shared blockers listed below, but the
following design/analysis work is safe to lock in now, independent of any other Phase 2 package:

- **Multiblock shape data** for all three structures can be transcribed now from CE source with no
  external dependency: ICF's flat per-offset `cbarp` checks in `TileEntityICFStruct`, Watz's flat
  per-offset `cbr` checks in `TileEntityWatzStruct`, and the fusion torus's
  `MachineFusionTorus.layout[5][15][15]` int-array pattern read by `TileEntityFusionTorusStruct`.
  These are pure data once the underlying block IDs exist; encoding them (e.g. as a datagen-friendly
  structure descriptor) does not require the block-entity framework to exist first.
- **Reaction/math ports** are pure functions over data already in hand: `ItemWatzPellet`'s
  burn/absorb/heatDiv `Function` objects (already ported, Phase 1), `ItemICFPellet.react`/
  `getFusingDifficulty`/`getMaxDepletion` (already ported, Phase 1), and `TileEntityFusionTorus`'s
  `getOuputIntensity`/`getSpeedScaled` static helpers (trivial, no TE state) can all be unit-tested
  or otherwise validated in isolation before any block entity exists.
- **The two missing ICF item companions** (`icf_pellet_empty`, `particle_muon`) are plain,
  self-contained items in the same shape as the already-ported `ItemICFPellet` — no reason not to
  land them alongside whatever package is doing general item cleanup, ahead of `TileEntityICFPress`
  itself.
- **`IFusionPowerReceiver`** is a two-method interface with zero implementation weight
  (`receivesFusionPower()`, `receiveFusionPower(long, double, float, float, float)`) — safe to
  declare in `com.hbm.tileentity.fusion` (or wherever the package-naming decision below lands) as
  soon as that package exists, ahead of porting any of its six implementors.

Everything else — every concrete `TileEntity*` class in this survey — requires at minimum a working
`BlockEntity` base class and registration path (see Open Questions), and in most cases also the
fluid-tank, energy-network, and recipe infrastructure detailed below. None of the 17 files surveyed
can be ported to a compiling, functional state today.

## Deferred scope

Everything below blocks on infrastructure that belongs to another package or an explicit
cross-cutting decision — listed so whoever picks this area up next doesn't rediscover it:

1. **Block-entity base framework (blocks every TE in this survey, and every other Phase 2 machine)**.
   The port has zero `com.hbm.tileentity` or `com.hbm.blockentity` content today — no
   `TileEntityLoadedBase`/`TileEntityMachineBase`/`TileEntityTickingBase` equivalents, no
   `IPersistentNBT`, no `IGUIProvider`/`IConfigurableMachine`/`IConnectionAnchors`/`IFluidCopiable`.
   Every CE class in this survey extends or implements at least one of these. This is squarely the
   package-naming decision `docs/phase0/STATUS.md` flags under "Open decisions" (CE's
   `com.hbm.tileentity` vs. Neo Edition's `com.hbm.blockentity`) — **not re-litigated here**, just
   confirmed as a hard blocker for this area specifically, and confirmed via a fresh read that Neo
   Edition (`upstream/neo-edition/src/main/java/com/hbm/blockentity/...`) did choose
   `com.hbm.blockentity`, for whatever weight that's worth as a data point (Neo Edition is
   content-unreliable per project ground rules, but its package/API *shape* choices are fair game).
2. **Fluid abstraction gap, two layers deep**. `com.hbm.inventory.fluid.tank.FluidTankNTM` — the
   concrete tank class every TE in this survey instantiates (`new FluidTankNTM(Fluids.X, size)`) —
   does not exist in the port (confirmed again here; already flagged by Phase 0/1). Beyond that,
   this survey found a **second, previously-uncatalogued gap**: the legacy non-MK2
   `com.hbm.api.fluid.*` interface family (`IFluidStandardTransceiver`, `IFluidStandardReceiver`,
   `IFluidStandardSender`, `IFluidConnector`, `IFluidConnectorBlock`) that ICF/Watz/`ICFPress` all
   implement is entirely absent from the port — only the *MK2* fluid API partially exists
   (`com.hbm.api.fluidmk2` in the port has just `IFluidRegisterListener`; CE's fluidmk2 package has
   14 files including `IFluidStandardTransceiverMK2`/`IFluidStandardReceiverMK2`, which the fusion
   subpackage's Boiler/Breeder/MHDT/PlasmaForge need). Whichever package owns "port the fluid tank
   system" needs to know both API generations are required, not just one.
3. **`com.hbm.uninos.networkproviders.*` — net-new, not previously flagged anywhere.** The port's
   `com.hbm.uninos` has the generic node-graph base (`NodeNet`, `UniNodespace`, `GenNode`,
   `INetworkProvider`) from earlier phases, but none of CE's four concrete network providers
   (`KlystronNetwork`, `PlasmaNetwork`, `PneumaticNetwork`, `RebarNetwork`) exist yet. The fusion
   torus, klystron, collector, boiler, and breeder are unusable without `KlystronNetwork` and
   `PlasmaNetwork` specifically — this is a hard prerequisite that belongs to whoever generalizes
   the `uninos` layer, not something to reimplement inline in this package.
4. **`com.hbm.modules.machine.ModuleMachineFusion`** (and its sibling `com.hbm.modules.*` helper
   classes) — the processing-loop abstraction `TileEntityFusionTorus` delegates almost all of its
   recipe-matching/progress/consumption logic to — has no port-side equivalent (`com.hbm.modules`
   doesn't exist in the port at all). This is a reusable machine-processing pattern likely shared
   with other Phase 2 machines (CE's `modules/machine` also has `ModuleMachineChemplant`,
   `ModuleMachineAssembler`, etc.) — recommend whoever designs the generic "processing machine"
   shape for Phase 2 treats `ModuleMachineFusion` as one required consumer, not something fusion
   reinvents standalone.
5. **Recipes**: `FusionRecipe extends GenericRecipe`, and `GenericRecipe`/`RecipesCommon` are the
   already-known cross-cutting gap (`docs/phase1/STATUS.md`). This area's specific need from that
   system: a `FusionRecipe`-shaped JSON `Recipe<?>` with `ignitionTemp` (long), `outputTemp` (long),
   `neutronFlux` (double), an RGB triple for plasma-effect tinting, plus the inherited
   input-fluid/input-item/output/duration/power fields from `GenericRecipe`. Not solving this here —
   flagging the exact shape needed so the recipe-infrastructure owner has a concrete consumer to
   design against.
6. **GUI/menu framework**: every TE in this survey except the four pure-struct-dummy classes
   (`*Struct`) and `TileEntityFusionCollector`/`TileEntityFusionCoupler` implements `IGUIProvider`
   and pairs with a CE `Container*`/`GUI*` class (`ContainerICF`/`GUIICF`,
   `ContainerFusionTorus`/`GUIFusionTorus`, `ContainerWatz`/`GUIWatz`, etc.). Confirmed (again) that
   no `AbstractContainerMenu`/`Screen` framework exists anywhere in the port yet — this is a shared
   Phase 2 prerequisite, not something to build bespoke per-machine here.
7. **OpenComputers integration** (`li.cil.oc.api.*`, `@Optional.Method(modid = "opencomputers")`) on
   nearly every TE in this survey — a soft/optional-mod integration layer. Not a blocker (the
   `@Optional` annotations mean CE itself treats it as best-effort), but flagging that it's present
   throughout and someone needs to decide whether NeoForge-era OpenComputers/vanilla-equivalent
   support is in scope at all for this port, or whether it's dropped entirely.
8. **SILEX/FEL** — see the dedicated boundary note above; explicitly not this package's job.

## Key design/API decisions

Confirmed by reading real code already in this repository (CE for behavior, the port's own Phase
0/1 code and `upstream/neo-edition` for NeoForge 1.21.1 API *shape* only — never for content):

- **Registration pattern is already established and should be reused verbatim.** The port's
  `com.hbm.items.machine.MachineItems` (Phase 1) registers items via
  `DeferredRegister`/`DeferredItem<Item>` with a small `reg("name", Ctor::new)` helper and a
  `tab(CreativeTab, ...)` wrapper. `upstream/neo-edition`'s `NtmBlockEntityTypes.java` confirms the
  equivalent block-entity-side shape actually compiles against NeoForge 1.21.1:
  `DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID)`
  with entries built via `BlockEntityType.Builder.of(SupplierCtor::new, block...).build(null)`, and a
  block-entity constructor of the standard NeoForge shape
  `MachineCentrifugeBlockEntity(BlockPos pos, BlockState state)` calling
  `super(NtmBlockEntityTypes.MACHINE_CENTRIFUGE.get(), pos, state, <inventorySize>)`. Whichever base
  class this port adopts for machine block entities should follow that same three/four-argument
  constructor shape.
- **Data components are the confirmed NBT replacement, but only for item-carried state.**
  `com.hbm.items.machine.MachineDataComponents` (already in the port, Phase 1) shows the real
  pattern: `DeferredHolder<DataComponentType<?>, DataComponentType<T>> FOO = register("foo",
  Codec.LONG, ByteBufCodecs.VAR_LONG)`. This applies to `ItemICFPellet`'s depletion/fuel-selection
  state and `ItemWatzPellet`'s yield (both already ported this way). It does **not** apply to
  block-entity-internal state (tank contents, heat, klystron/plasma energy, structure-assembly
  flags) — those stay as `CompoundTag` read/write on the block entity itself, same as CE's
  `readFromNBT`/`writeToNBT` pattern, just renamed to NeoForge's `loadAdditional`/`saveAdditional`.
  Don't over-apply the data-component pattern to TE state; it's an item-stack mechanism.
- **Capability exposure has an established wrapper pattern to extend, not reinvent.** The port
  already has `com.hbm.capability.NTMEnergyCapabilityWrapper`,
  `NTMCableEnergyCapabilityWrapper`, `NTMBatteryEnergyWrapper`, `NTMFluidHandlerWrapper`,
  `NTMFluidContainerWrapper`, and a central `ModCapabilities` registration point from earlier
  phases. CE's `TileEntityICFController.getCapability()` exposing `NTMEnergyCapabilityWrapper` for
  `CapabilityEnergy.ENERGY` (a *vanilla-Forge*-facing bridge, not the HE-native path) is the same
  shape this port's `NTMEnergyCapabilityWrapper` already exists to serve — new fusion/ICF/Watz block
  entities should register through `ModCapabilities`/`RegisterCapabilitiesEvent`, not invent a new
  capability-exposure mechanism.
- **HE energy stays non-NeoForge, per ground rule, and this area confirms why that matters twice
  over.** `IEnergyReceiverMK2`, `IEnergyProviderMK2`, `PowerNetMK2` already exist in the port
  (`com.hbm.api.energymk2`) and cover `TileEntityICFController`, `TileEntityFusionKlystron`, and
  `TileEntityFusionMHDT`'s battery/power-net side. But the fusion torus's actual plasma/ignition
  distribution is **not** `PowerNetMK2` at all — it's the separate `uninos` node-graph system
  (`KlystronNetwork`/`PlasmaNetwork`, gap #3 above). Don't assume "custom HE energy" fully covers
  this package's networking needs; two distinct in-house network systems are in play here.
- **Multiblock assembly follows the existing `BlockDummyable`/`BlockDummyableMBB` pattern**, both of
  which already exist in the port (`src/main/java/com/hbm/blocks/BlockDummyable.java`,
  `BlockDummyableMBB.java`) from earlier phases, even though no concrete multiblock content has
  used them yet. All three struct-check classes in this survey (`TileEntityICFStruct`,
  `TileEntityWatzStruct`, `TileEntityFusionTorusStruct`) end their check by calling
  `BlockDummyable.safeRem = true/false` around a `fillSpace(...)` call — that's the real mechanism
  to preserve, and it's already present on the port's block side, just never yet driven by a real
  multiblock TE.

## Open questions / risks

- **Package-naming decision is not optional for this area** — it blocks all 17 files, not just a
  convenience choice. `docs/phase0/STATUS.md` already asks for a call between CE's
  `com.hbm.tileentity` and Neo Edition's `com.hbm.blockentity` "before Phase 2 block entities land";
  this survey is a concrete instance of that landing. Recommend resolving it before implementation
  starts on *any* Phase 2 machine package, since every one of them will hit the same fork.
- **Two energy/network systems, one package.** The fusion torus needs both `PowerNetMK2` (battery
  charging on Klystron/MHDT) *and* the `uninos` KlystronNetwork/PlasmaNetwork graphs
  simultaneously. Confirm the `uninos` generalization work (gap #3) is scheduled somewhere before
  fusion-torus implementation starts, since it can't be stubbed convincingly — the torus's core
  power-sharing mechanic (`getOuputIntensity` receiver-count scaling) depends on it directly.
  Should `uninos/networkproviders` be a Phase 2 machines subtask, or does it belong to whichever
  package owns general power/pipe networking? Not decided here.
- **`ModuleMachineFusion` reuse question**: is the generic `com.hbm.modules.machine.ModuleMachineBase`
  processing-loop abstraction going to be ported once, generically, for all `modules/machine/*`
  consumers (chemplant, assembler, PUREX, etc. — all Phase 2 machines elsewhere), or does each
  consuming package (this one included) port its own copy of just the pieces it needs? Porting
  fusion's copy in isolation risks divergence from whatever the "real" processing-machine
  abstraction ends up looking like once more of Phase 2 lands.
- **OpenComputers/computer-integration scope**: every TE in this survey carries a full
  `SimpleComponent`/`@Callback` OpenComputers bridge. No decision found anywhere in Phase 0/1 docs
  about whether external-mod computer integration is in scope for this port at all. Worth an
  explicit yes/no before anyone spends time porting (or deliberately stripping) this from 15 of the
  17 files.
- **Item-side completeness for ICF**: confirmed `icf_pellet_empty` and `particle_muon` are missing
  items (needed only by `TileEntityICFPress`, not by `TileEntityICF` itself) — small, but will block
  compiling the press machine specifically if not caught before that point.
- **Watz meltdown VFX/entity dependency**: `TileEntityWatz`'s explosion path spawns
  `EntityShrapnel` (with a `setWatz(true)` flag) and calls `AdvancementManager.grantAchievement`.
  Neither of those was checked for port-readiness as part of this survey — worth a quick
  cross-check with whoever owns entities/advancements before Watz implementation, since a meltdown
  that silently no-ops the shrapnel spray would be a visible parity regression, not a stub anyone
  would notice quickly.
- **Render/audio coupling not covered here**: several TEs (`TileEntityFusionTorus`,
  `TileEntityFusionKlystron`) drive looped positional audio (`AudioWrapper`,
  `MainRegistry.proxy.getLoopedSound`) and client-side rotation animation state (`magnet`/`fan`
  angle + speed) directly from tick logic. This is gameplay-adjacent (the audio pitch/volume reflect
  live reactor throughput) rather than pure rendering, so it likely needs to travel with the TE port
  rather than being deferred wholesale to a client/UX phase — flagging for whoever scopes that
  phase boundary, not resolving it here.
