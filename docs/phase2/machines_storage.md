# Storage machines: mass crates, fluid tanks/barrels, batteries — Phase 2 research

Sources read in full: `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/{BlockStorageCrate,
BlockStorageCrateRadResistant}.java`; `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/
{TileEntityCrate,TileEntityCrateIron,TileEntityCrateSteel,TileEntityCrateTungsten,TileEntityCrateDesh,
TileEntitySafe,TileEntityLockableBase,TileEntityMachineFluidTank,TileEntityMachineBattery}.java`;
`upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/storage/{TileEntityCrateBase,
TileEntityBatteryBase,TileEntityBatterySocket,TileEntityBatteryREDD}.java`;
`upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/{MachineBattery,MachineCapacitor,
MachineCapacitorBus}.java`; `upstream/hbm-ce/src/main/java/com/hbm/blocks/network/
{MachineBatterySocket,MachineBatteryREDD}.java`; `upstream/hbm-ce/src/main/java/com/hbm/inventory/
fluid/tank/FluidTankNTM.java`; `upstream/hbm-ce/src/main/java/com/hbm/api/fluidmk2/*.java`;
`upstream/hbm-ce/src/main/java/com/hbm/blocks/ModBlocks.java` (registration lines for every block
named below); `upstream/hbm-ce/src/main/java/com/hbm/lib/Library.java` (battery/tank helper methods).
This port's own `src/main/java/com/hbm/blocks/generic/GenericCrateBlocks.java`,
`src/main/java/com/hbm/items/machine/{ItemBattery,ItemBatteryPack,ItemBatterySC,
ItemBatteryCreative}.java`, `src/main/java/com/hbm/api/energymk2/{IBatteryItem,IEnergyConductorMK2,
IEnergyProviderMK2,IEnergyReceiverMK2,PowerNetMK2}.java`, `src/main/java/com/hbm/uninos/*.java`,
`src/main/java/com/hbm/api/fluidmk2/IFluidRegisterListener.java`, `src/main/java/com/hbm/blocks/
BlockDummyable.java`, `src/main/java/com/hbm/lib/Library.java`. Cross-referenced against three
already-written sibling Phase 2 research reports found on disk (`docs/phase2/{blockentity_base,
gui_framework,multiblock_framework}.md`) rather than re-deriving their findings — see the "Shared
prerequisites" note below. `docs/phase0/STATUS.md`, `docs/phase1/blocks_generic.md`,
`docs/phase1/items_tool.md` (structural model), `PORT_SPEC.md`. `upstream/neo-edition` was consulted
only for confirmed NeoForge 1.21.1 API shapes (block-entity registration, menu/screen, capability
registration), never for behavior — CE is the sole source of truth for behavior throughout.

## Headline finding

This area splits cleanly into three CE subsystems that PORT_SPEC groups together as "storage
machines" but which sit at very different points on the readiness curve:

- **Mass storage crates** (`BlockStorageCrate`/`BlockStorageCrateRadResistant`, 5 concrete
  `TileEntityCrate` subclasses) are the most portable of the three: single-block, no multiblock
  framework, no fluid network, no OpenComputers coupling. Their real blockers are two **shared**
  Phase 2 prerequisites also needed by dozens of other machines (a block-entity base class, a
  Menu/Screen framework) plus one **storage-local** prerequisite (the lock/pin security item family),
  not anything specific to crates.
- **Mass fluid tanks** are, on inspection, not a simple "TE-backed barrel." CE's only mass-fluid-storage
  block, `TileEntityMachineFluidTank` (`ModBlocks.machine_fluidtank`), is a 5×5 multiblock
  (`BlockDummyable`) fluid-network node (`IFluidStandardTransceiverMK2`/`FluidNode`/`UniNodespace`)
  wired into the control-panel event system, redstone-over-radio, and OpenComputers, on top of a
  fluid-tank data class (`FluidTankNTM`) that does not exist in this port yet, whose two governing
  interfaces (`IFluidReceiverMK2`/`IFluidProviderMK2`) also don't exist yet (the port's
  `com.hbm.api.fluidmk2` package currently has only `IFluidRegisterListener` — no parallel to
  `energymk2`'s `PowerNetMK2`/`Nodespace`/`IEnergyConductorMK2` trio has been built for fluids at all).
  This block is almost entirely blocked scope, not a Phase-2-safe item with a footnote.
- **Batteries/capacitors** split cleanly along the same "single-block vs. multiblock" line as the
  other two systems, and land closer to crates than to the fluid tank: `TileEntityMachineBattery`
  (5 single-block `MachineBattery` grades) and `MachineCapacitor`'s inline `TileEntityCapacitor`
  (5 grades + `capacitor_bus`) depend **only** on already-fully-ported Phase 0 infrastructure
  (`energymk2` API, `UniNodespace`/`Nodespace`, `IRORValueProvider`) plus the same two shared Phase 2
  prerequisites crates need. `MachineBatterySocket`/`MachineBatteryREDD` (the two multiblock "battery
  bank" blocks), by contrast, are `BlockDummyable` multiblocks with `IControlReceiver`/`ICopiable`
  coupling and belong with the fluid tank on the deferred side.

**Shared prerequisites note**: three sibling Phase 2 research reports already exist on disk
(`docs/phase2/blockentity_base.md`, `gui_framework.md`, `multiblock_framework.md`) as of this survey,
each covering exactly the cross-cutting infrastructure this area's blockers point to. This report
does not re-derive their findings — it cites their confirmed conclusions where relevant and defers
to them as the authoritative source for the block-entity base class, Menu/Screen framework, and
multiblock framework respectively. If those packages have not landed by the time storage machines
are implemented, storage machines cannot land either; that is a scheduling fact, not something this
report needs to solve.

## Phase-2-safe scope

### Mass storage crates (2 block classes, 5 tile-entity classes, 8 registry entries)

CE registry entries (from `ModBlocks.java`; all `setCreativeTab(null)` except the 4 crates + safe are
placed on the standard build tab per Phase 1's `GenericCrateBlocks.java` convention — CE itself uses
`MainRegistry.machineTab`):

| Block id | Class | Slots | Notes |
|---|---|---|---|
| `crate_iron` | `TileEntityCrateIron` | 36 | 9×4 grid |
| `crate_steel` | `TileEntityCrateSteel` | 54 | 9×6 grid |
| `crate_tungsten` | `TileEntityCrateTungsten` | 27 | 9×3 grid; **also** `ITickable`+`ILaserable`+
  `IBufPacketReceiver` — a laser-heating mechanic (`heatTimer`/`joules`, particle spawn) layered on
  top of the plain crate shape. Port the plain-crate behavior now; the laser-interaction piece needs
  whatever Phase 2/3 package owns laser weapons (`ItemCrucible`/`DFCRecipes` imports) — flag as a
  narrow deferred sub-feature of an otherwise-safe block, not a reason to defer the whole class. |
| `crate_desh` | `TileEntityCrateDesh` | 104 | 13×8 grid, largest crate |
| `safe` | `TileEntitySafe` | 15 | 5×3 grid, distinct GUI texture (`gui_safe.png`) but otherwise a
  plain `TileEntityCrate` — no extra lock strength or behavior vs. the other four despite the name |

Both block classes (`BlockStorageCrate` normal + `BlockStorageCrateRadResistant`, the latter just
adding an inert-until-Phase-2-radiation `IRadResistantBlock` tooltip/rebuild hook, exactly like the
already-Phase-1-safe `BlockRadResistantPillar`/`BlockNTMGlass` pattern `blocks_generic.md` already
documented) are simple `BlockEntity`-backed containers: `FACING` blockstate property (4-way
horizontal), comparator override via `ItemHandlerHelper.calcRedstoneFromInventory`, and a
loot-list-free plain inventory (no hardcoded loot table wiring in these classes themselves — CE's
loot tables are external JSON, out of this report's code-survey scope but not a blocker).

**What's genuinely self-contained and portable once the two shared prerequisites below land:**

- `TileEntityCrateBase` (36-303 in the file) — `ItemStackHandler` inventory with
  `isItemValidForSlot` rejecting nested crates (`ItemBlockStorageCrate.containsCrate`, a cross-check
  against the crate item itself, not a new system), loot-table fill-on-first-open
  (`fillWithLoot`/`ensureFilled`, driven by vanilla `LootTable`/`LootContext` — both stock NeoForge
  APIs, no port-specific gap), and a `checkLock`-gated `ItemStackHandlerWrapper`-based capability
  exposure (the wrapper class is already ported per `blockentity_base.md`'s dependency list).
- `TileEntityCrate` (the concrete 5-subclass base) — adds the custom-name/GUI-metadata fields
  (columns/rows/pixel offsets/colors/texture `ResourceLocation`, all compile-time constants per
  subclass, zero runtime dependency), the drop-on-break persistent-NBT payload (slot contents +
  accumulated radiation total via `HazardSystem.getTotalRadsFromStack` — `HazardSystem` is already a
  fully Phase-0-ported registry per `docs/phase0/STATUS.md`, not a new dependency), and the
  tooltip-rendering logic in `BlockStorageCrate.addInformation` (slot-count/contents preview,
  entirely stack-NBT/data-driven, no new API needed).
- `TileEntityLockableBase` (the crate's lock/pin state: `lock`/`isLocked`/`lockMod`, `canAccess`,
  `tryPick`) — **the lock mechanism's own state machine is trivial and self-contained**, but every
  path that actually *unlocks* something reads `ItemKeyPin`/`ModItems.key_red`/`ItemTooling`
  (screwdriver-based lock-picking) — none of which exist in this port yet (confirmed by directory
  search: no `Item{Key,Lock,KeyPin,CounterfeitKeys}*` under this port's `com.hbm.items`). This
  matches `docs/phase1/items_tool.md` bucket (c)'s own call ("`ItemKeyPin`/`ItemKey`/`ItemLock`/
  `ItemCounterfeitKeys` — port as one unit" under Phase 2 machine coupling). **A locked crate is a
  real, player-visible CE feature (crates found in loot with a lock puzzle), so shipping crates
  without it is a genuine content gap, not a cosmetic one** — recommend porting the lock/pin item
  family alongside the crate TEs in the same implementation pass, since `TileEntityLockableBase`
  already assumes their existence at three call sites (`canAccess`, `tryPick`, `hasLockPickTools`).

### Batteries and capacitors (single-block only: 2 block classes + `capacitor_bus`, 11 registry entries)

| Block id(s) | Class | Max power (HE) | Notes |
|---|---|---|---|
| `machine_battery_potato` | `MachineBattery` | 10,000 | `setCreativeTab(null)` — non-craftable/debug tier |
| `machine_battery` | `MachineBattery` | 1,000,000 | |
| `machine_lithium_battery` | `MachineBattery` | 50,000,000 | |
| `machine_schrabidium_battery` | `MachineBattery` | 25,000,000,000 | |
| `machine_dineutronium_battery` | `MachineBattery` | 1,000,000,000,000 | |
| `capacitor_copper` | `MachineCapacitor` | 1,000,000 | |
| `capacitor_gold` | `MachineCapacitor` | 5,000,000 | |
| `capacitor_niobium` | `MachineCapacitor` | 25,000,000 | |
| `capacitor_tantalium` | `MachineCapacitor` | 150,000,000 | |
| `capacitor_schrabidate` | `MachineCapacitor` | 50,000,000,000 | |
| `capacitor_bus` | `MachineCapacitorBus` | — | Directional "wire" block that chains capacitor blocks into one virtual bank (`IEnergyConnectorBlock`); no TE of its own, purely a `canConnect` direction filter read by the capacitor's own `update()` bus-walk loop |

Both `MachineBattery` and `MachineCapacitor` are CE-annotated `@Deprecated` (superseded in CE's own
roadmap by the multiblock battery-bank blocks below), **but every instance above is still live,
player-reachable CE content that `ModBlocks`/crafting recipes reference today** — matching this
port's own precedent in `ItemBattery`'s javadoc ("CE marked this `@Deprecated` ... every one of its
~30 registered instances is still real CE content ... ported unchanged in spirit"). Treat these the
same way: port as real Phase 2 content, not as dead weight.

**Why these are portable now, unlike the fluid tank:**

- `TileEntityMachineBattery` and `MachineCapacitor.TileEntityCapacitor` implement
  `IEnergyConductorMK2`/`IEnergyProviderMK2`/`IEnergyReceiverMK2` — **all three interfaces are
  already fully ported** in `src/main/java/com/hbm/api/energymk2/`, unchanged from CE's contract.
- Both use `UniNodespace`/`Nodespace.PowerNode` for the buffer-mode "acts like a cable" behavior —
  **already fully ported** per `docs/phase0/STATUS.md`'s gap-fill pass (confirmed present:
  `src/main/java/com/hbm/uninos/{UniNodespace,GenNode,NodeNet,INetworkProvider}.java`).
  `com.hbm.api.energymk2.Nodespace` (the `PowerNode` holder type) is also already ported.
  There is no fluid-side equivalent of any of this yet, which is the concrete reason the fluid tank
  cannot follow the same path.
- Both implement `IRORValueProvider`/`IRORInteractive` (redstone-over-radio query/control) — **already
  fully ported** (`src/main/java/com/hbm/api/redstoneoverradio/*.java`).
- The battery's item-charging logic (`Library.chargeTEFromItems`/`chargeItemsFromTE`,
  `Library.isChargeableBattery`/`isDischargeableBattery`/`isEmptyBattery`/`isFullBattery`) reads
  `IBatteryItem`-implementing item stacks in its charge/discharge inventory slots. **`IBatteryItem`
  itself is already fully ported** (`com.hbm.api.energymk2.IBatteryItem`, DataComponent-backed charge
  per the port's own convention — see Key design decisions), and its consumers
  (`ItemBattery`/`ItemBatteryPack`/`ItemBatterySC`/`ItemBatteryCreative`) are already ported Phase 1
  content. **The four `Library.*Battery*`/`charge*FromTE`/`*FromItems` helper methods themselves are
  not yet ported** (confirmed absent from this port's `Library.java`, which currently has none of
  `chargeTEFromItems`/`chargeItemsFromTE`/`isDischargeableBattery`/`isChargeableBattery`/
  `isEmptyBattery`/`isFullBattery`) — this is a small, self-contained gap (six static methods,
  ~50 lines in CE, pure `ItemStackHandler`+`IBatteryItem` logic, no new external dependency) that
  this area's implementation pass should close, not something that needs its own research package.
- `MachineCapacitorBus`'s directional bus-chain walk (`update()`'s `while` loop scanning adjacent
  `capacitor_bus` blocks) is self-contained block-state logic, no new API.
- Neither block is `BlockDummyable` — both are plain single-`BlockPos` containers, so neither needs
  the multiblock framework at all.
- OpenComputers integration (`@Optional.Interface`/`SimpleComponent`/`Callback`) appears on both TEs.
  This port has made no OpenComputers integration decision anywhere in the areas surveyed for this
  report (Phase 0/1 STATUS docs don't mention it) — recommend treating it the same way the port
  already treats other 1.12-era soft-dependency mods with no confirmed NeoForge 1.21 build in this
  ecosystem (dropped, per `blocks_generic.md`'s Galacticraft precedent), unless another area has
  already made a different call. Flagged as an open question below rather than resolved here.

**What each still needs from the two shared prerequisites**, same as crates: a `BlockEntity` base
class (`blockentity_base.md`'s `MachineBaseBlockEntity`-equivalent covers the inventory/capability
shape both batteries and crates need almost identically — 4-slot charge/discharge inventory for
`TileEntityMachineBattery`, N-slot loot inventory for crates, both `ItemStackHandler`-backed) and a
Menu/Screen pair (`GUIMachineBattery`/`ContainerMachineBattery` in CE, trivial 4-slot layouts,
covered by `gui_framework.md`'s shared `MenuBase`/`AbstractContainerScreen` design).

## Deferred scope

### Mass fluid tanks — almost entirely blocked, one package's worth of prerequisite work

`TileEntityMachineFluidTank` (`ModBlocks.machine_fluidtank`) cannot be ported as a working block
without **all** of the following landing first:

- **`FluidTankNTM`** (`com.hbm.inventory.fluid.tank.FluidTankNTM`, 504 lines in CE) — the tank data
  class itself. **Confirmed absent from this port** (matches `docs/phase0/STATUS.md`'s own
  compile-error triage list, and independently reconfirmed by both `blockentity_base.md` and
  `multiblock_framework.md`). Notably couples client rendering (`GUIElements`/`GuiInfoContainer`/
  direct `GL11`/`Tessellator` calls) directly into the data class in CE — whoever ports it needs to
  split client rendering out to match this port's client/server separation convention, per
  `blockentity_base.md`'s own flag on this exact point.
- **`com.hbm.api.fluidmk2`'s network trio** — CE has `IFluidReceiverMK2`, `IFluidProviderMK2`,
  `IFluidUserMK2`, `IFluidStandardTransceiverMK2`, `FluidNode`, `FluidNetMK2` forming a fluid-side
  parallel to the already-ported `energymk2` package (`IEnergyProviderMK2`/`IEnergyReceiverMK2`/
  `Nodespace`/`PowerNetMK2`). **This port's `com.hbm.api.fluidmk2` currently has only
  `IFluidRegisterListener`** — none of the network-graph pieces exist. This is a real, undersized gap:
  Phase 0 ported the energy half of this pattern in full but not the fluid half, even though both
  `NTMFluidHandlerWrapper` (already ported, per `blockentity_base.md`) and `TileEntityMachineFluidTank`
  assume it exists. **Recommend this become its own small Phase 2 research/implementation package**
  ("fluid network graph," mirroring whichever package the `energymk2`/`UniNodespace` work landed
  under in Phase 0) rather than folding it into whichever package eventually ports concrete fluid
  machines — dozens of other CE fluid machines beyond the tank need the same trio (this port's own
  `docs/phase0/STATUS.md` build-error list already names `FluidTankNTM` as a known Phase 2 forward
  reference shared across many files, not unique to the tank block).
- **The multiblock framework** (`BlockDummyable`/`MultiblockHandlerXR`) — `machine_fluidtank` is a
  5×5 (`getDimensions()` returns an 8-`DirPos` port ring at ±2 blocks) multiblock. Per
  `multiblock_framework.md`, the port's `BlockDummyable.java` skeleton already exists but doesn't yet
  compile pending `MultiblockHandlerXR`/`IPersistentNBT`.
- **The control-panel event system** (`ControlEvent`/`ControlEventSystem`/`IControllable`) — the
  tank's remote mode-switching (`tank_set_mode`) goes through this network, which `blocks_generic.md`
  already catalogued as an 8-file Phase 2+ system with no port yet.
- **The Menu/Screen framework** — `ContainerMachineFluidTank`/`GUIMachineFluidTank`.
- **Redstone-over-radio's interactive side** (`IRORInteractive.runRORFunction`) — the query side
  (`IRORValueProvider`) is already ported, but this is a small, low-priority addition once the block
  itself exists, not a blocker on its own.
- **OpenComputers** (same open question as batteries, see above).

**Given the size of that dependency list, this report's recommendation is to treat
`TileEntityMachineFluidTank` as out of scope for whichever implementation pass covers the rest of
this report's Phase-2-safe items**, and to schedule it only after the fluid-network-graph package
above lands — likely alongside the broader refinery/boiler/oil-processing machine family that shares
the same `FluidTankNTM`/`fluidmk2` dependency (per `blockentity_base.md`'s note that ~112 CE tile
entities implement some `IFluidStandard*` interface). Porting the crate/battery half of this report's
scope does not require waiting on any of this.

Two smaller adjacent notes, both already flagged by prior research and reconfirmed here rather than
re-litigated:

- **No world-fluid-block system exists in this port** (confirmed by Phase 1's own research, restated
  in the task brief). Not a dependency of anything in this report — CE's fluid tanks are a TE-internal
  storage abstraction (`FluidTankNTM`), entirely separate from any `LiquidBlock`/world fluid
  rendering, matching `blockentity_base.md`'s identical note.
- **No plain "fluid barrel" block distinct from the mass tank exists in CE's storage family.** This
  report's task brief anticipated "fluid barrels/tanks" as parallel content; CE's actual barrel
  family (`BaseBarrel`/`RedBarrel`/`YellowBarrel`) are **item/loot-crate-style single-item-drop
  containers with explosive/radioactive payloads**, not fluid storage — already fully triaged in
  `blocks_generic.md` ("Crates, barrels, loot containers" + the radiation/explosion Phase-2+ rows) and
  out of scope here to avoid duplicating that survey. `machine_fluidtank` is CE's only mass
  *fluid*-storage block.

### Multiblock battery banks — `MachineBatterySocket`, `MachineBatteryREDD`

Both extend `BlockDummyable` (confirmed: 1×1 socket footprint for `MachineBatterySocket`, a larger
9-wide footprint with 6 corner/edge extras for `MachineBatteryREDD`) and their core TEs
(`TileEntityBatterySocket`, `TileEntityBatteryREDD`, both extending the shared
`TileEntityBatteryBase`) additionally require:

- The multiblock framework (same `BlockDummyable`/`MultiblockHandlerXR` dependency as the fluid tank).
- `IControlReceiver` (control-panel event system — same Phase 2+ system `blocks_generic.md` already
  catalogued, not yet ported).
- `ICopiable` (settings copy/paste — paired with `ItemSettingsTool`, itself already flagged Phase 2
  machine-coupling in `items_tool.md` bucket (c)).
- OpenComputers (same open question as the single-block batteries).
- `TileEntityBatterySocket` specifically stores/discharges a *battery item stack* rather than a raw
  HE pool (`syncStack`/`powerFromStack`/`maxPowerFromStack` reading `IBatteryItem` off a held item) —
  self-contained logic once the item API exists (it does, see above), but still gated on the
  multiblock/control-panel dependencies for the block shell itself.

Recommend deferring both alongside the fluid tank, for the same multiblock-framework reason, not
because of any unique blocker of their own.

## Key design/API decisions

Every API shape below was either already committed in this port's own code, or confirmed by a
sibling Phase 2 research report's own citation of real NeoForge 1.21.1 usage — nothing here is
invented for this report.

- **Block-entity base class and registration**: this area should build its crate/battery block
  entities on whatever `MachineBaseBlockEntity`-equivalent `blockentity_base.md` specifies (confirmed
  design: `ItemStackHandler`-backed inventory, `enableFluidWrapper`/`enableEnergyWrapper`
  constructor flags, `saveAdditional`/`loadAdditional` NBT, per-`BlockEntityType`
  `RegisterCapabilitiesEvent` registration) rather than inventing a second base class. Registration
  itself follows the pattern this port already uses in `GenericCrateBlocks.java` (a
  `Supplier<BlockEntityType<...>>` field populated via
  `ModBlocks.BLOCK_ENTITY_TYPES.register(name, () -> BlockEntityType.Builder.of(Ctor::new,
  block.get()).build(null))`), so storage machines add no new registration idiom.
- **Battery item charge storage stays a `DataComponentType<Long>`** (`IBatteryItem.getChargeComponent()`),
  not NBT — already the port's committed convention (`ItemBattery`/`ItemBatteryPack` above), so the
  TE-side charge/discharge logic this area ports (`Library.chargeTEFromItems`/`chargeItemsFromTE`)
  should read/write through `IBatteryItem`'s existing `getCharge`/`setCharge`/`chargeBattery`/
  `dischargeBattery` default methods, not raw `ItemStack` NBT tags the way CE's own `Library` methods
  do internally (CE predates data components entirely).
- **HE energy stays `long`-typed and capability-exposed via the existing wrapper classes**
  (`NTMEnergyCapabilityWrapper`), not NeoForge's own `IEnergyStorage`/FE — confirmed already-committed
  port convention (`docs/phase0/energy.md`, restated in the task's own ground rules) and directly
  relevant here since every battery/capacitor TE in this report implements
  `IEnergyProviderMK2`/`IEnergyReceiverMK2`, the same interfaces the wrapper class already expects.
- **Crate inventory capability exposure should reuse `ItemStackHandlerWrapper`** exactly as CE's own
  `TileEntityCrateBase.getCapability` does — already ported per `blockentity_base.md`'s dependency
  list, no new wrapper needed for the lock-gated (`checkLock(facing)`) capability visibility crates
  need.
- **The lock/pin item family (`ItemKeyPin`/`ItemLock`/`ItemKey`/`ItemCounterfeitKeys`) should be
  ported alongside crate TEs in the same pass**, per this report's own Phase-2-safe-scope note above
  — it is a small, self-contained item family (no block/TE of its own beyond what already exists) and
  crates are the only Phase 2 storage content that needs it functional to be complete (CE's other
  `TileEntityLockableBase` consumers — safes/vaults elsewhere in the mod — are out of this report's
  scope but would benefit from the same pass).

## Open questions / risks

- **`com.hbm.tileentity` vs `com.hbm.blockentity` package naming is still unresolved, and this
  report's own sources disagree with each other.** `docs/phase0/STATUS.md` flagged this as needing an
  explicit call "before Phase 2 block entities land." Both sibling reports read for this survey have
  since taken *different* positions: `blockentity_base.md` recommends **preserving**
  `com.hbm.tileentity` (option A, "offered for the record but not self-authorized"), while
  `multiblock_framework.md`'s own scope table lists `com.hbm.blockentity.IPersistentNBT` as a
  concrete deliverable (option B) on the grounds that the port's already-committed
  `BlockDummyable.java` skeleton calls a Neo-Edition-named method (`restoreData`, not CE's
  `onBlockPlacedBy`) and treats that as license to also adopt Neo Edition's package name. **This
  report takes no side** — every class this area's crate/battery block entities need
  (`IPersistentNBT`, whatever `IGUIProvider`-successor or Menu-only convention lands, the shared base
  class itself) lives under whichever package that decision lands on, so this is a blocking
  dependency for this area too, not just for whoever ports the base class. Flagging again, now from a
  third angle, that this decision needs to be made once, explicitly, before *any* Phase 2 block
  entity (including the crates/batteries in this report) is implemented for real — not resolved
  independently by each package that happens to need it.
- **OpenComputers integration has no confirmed port-wide decision.** `TileEntityMachineBattery`,
  `MachineCapacitor.TileEntityCapacitor`, and `TileEntityBatteryBase` (hence `BatterySocket`/
  `BatteryREDD`) all carry `@Optional.Interface`/`SimpleComponent`/`@Callback` OpenComputers hooks in
  CE. No Phase 0/1 research report surveyed for this task mentions OpenComputers at all, and no
  OpenComputers-adjacent dependency or stub exists anywhere in this port's `build.gradle` or source
  tree as far as this survey's grep coverage reached. Recommend an explicit drop-or-keep decision
  (this report leans "drop," matching the port's precedent of dropping other Forge-1.12-era optional
  mod integrations with no confirmed NeoForge 1.21 build — e.g. Galacticraft in `blocks_generic.md`,
  Baubles in `items_tool.md`) rather than each area silently stripping or silently attempting to port
  `@Optional`-gated OpenComputers code independently.
- **The fluid-network-graph gap (`com.hbm.api.fluidmk2`'s missing trio + `FluidTankNTM`) blocks more
  than this report's own scope.** This report recommends it become its own Phase 2 package (see
  Deferred scope) rather than being solved inline by whichever package first needs a fluid tank —
  but that is a scheduling recommendation for the orchestrating session, not a decision this report
  can make unilaterally. If another concurrent Phase 2 package has already started this work, this
  report's fluid-tank section should be treated as superseded by that package's findings rather than
  re-researched.
- **`TileEntityCrateTungsten`'s laser-heating mechanic** (`ILaserable`, `ItemCrucible`/`DFCRecipes`
  imports) reaches into laser-weapon content this report did not survey in depth. Recommend porting
  the plain-crate shell now and tracking the laser interaction as a small follow-up once whichever
  Phase 2/3 package owns `ItemCrucible`/laser tools lands — not a reason to delay the whole crate
  family, but flagged so it isn't silently forgotten once the crate shell compiles and "looks done."
- **Six missing `Library` battery/charge helper methods** (`chargeTEFromItems`, `chargeItemsFromTE`,
  `isChargeableBattery`, `isDischargeableBattery`, `isEmptyBattery`, `isFullBattery`) are a small gap
  this report expects the implementation pass to close directly (see Phase-2-safe scope) rather than
  treating as a blocker — flagged here only so a reviewer checking "is `Library.java` complete for
  this area" knows the gap is known and intentionally left for implementation, not missed by this
  survey.
- **Recipes are not a dependency of anything in this report's Phase-2-safe scope.** Neither crates nor
  single-block batteries/capacitors consume `RecipesCommon`/`GenericRecipe(s)` (they have no
  processing logic — crates are pure storage, batteries just charge/discharge item stacks). Restating
  this explicitly because the task's ground rules called out `RecipesCommon`/`GenericRecipe(s)` as a
  cross-cutting gap to watch for: this area does not need it, and should not be blocked waiting on it.
