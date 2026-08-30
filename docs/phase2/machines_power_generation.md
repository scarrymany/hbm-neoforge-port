# Power generation machines triage (burner/steam/diesel engines, RTGs, turbines, solar)

Source: `hbm-ce/src/main/java/com/hbm/tileentity/machine/**` (grep for `Engine|RTG|Turbine|Generator|Solar`)
paired with `hbm-ce/src/main/java/com/hbm/blocks/machine/**`, plus every class those TEs reach into
directly (`TileEntityMachineBase`/`TileEntityLoadedBase`/`TileEntityMachinePolluting`,
`RTGUtil`/`ItemRTGPellet`, `IPersistentNBT`, the already-ported `com.hbm.api.energymk2.*` and
`com.hbm.capability.*` classes, `BlockDummyable`/`BlockDummyableMBB`).

File count: 15 tile-entity classes (`TileEntityMachineSteamEngine`, `TileEntityMachineCombustionEngine`,
`TileEntityMachineDiesel`, `TileEntityMachineRTG`, `TileEntityMachineMiniRTG`, `TileEntityDiFurnaceRTG`,
`TileEntityRtgFurnace`, `TileEntityTurbineBase` (abstract), `TileEntityMachineTurbine`,
`TileEntityMachineLargeTurbine`, `TileEntityMachineIndustrialTurbine`, `TileEntityMachineTurbineGas`,
`TileEntitySolarBoiler`, `TileEntitySolarMirror`, `TileEntityMachineIGenerator`) + 15 paired block
classes (`MachineSteamEngine`, `MachineCombustionEngine`, `MachineDiesel`, `MachineRTG`,
`MachineMiniRTG`, `MachineDiFurnaceRTG`, `MachineRtgFurnace`, `MachineTurbine`, `MachineLargeTurbine`,
`MachineIndustrialTurbine`, `MachineTurbineGas`, `MachineSolarBoiler`, `SolarMirror`, `MachineIGenerator`,
`MachineGenerator`) = 30 core files, plus ~15 directly-coupled support files read in full to verify
behavior (`TileEntityMachineBase`, `TileEntityLoadedBase`, `TileEntityMachinePolluting`, `RTGUtil`,
`ItemRTGPellet`, `IPersistentNBT`, `BlockDummyable`, `BlockDummyableMBB`, the 9 `com.hbm.api.energymk2.*`
interfaces, `NTMEnergyCapabilityWrapper`, `NTMFluidHandlerWrapper`) that already exist in this port's
tree and pin down the confirmed API shapes below.

## Headline finding

"Solar panels" in the task brief does not exist as CE content: there is no photovoltaic HE-generating
block anywhere in `com.hbm`. CE's only "solar" power tech is a **thermal** array —
`TileEntitySolarBoiler` (produces **steam**, not HE) heated by `TileEntitySolarMirror` (a
sun-tracking heliostat that pumps `heatInput` into one specific boiler by direct coordinate
reference, no HE, no fluid). The boiler's steam then has to be piped into a turbine (this same
family) to actually become HE. Plan the "solar" deliverable as boiler+mirror, not a panel block.

Similarly "diesel/gas generators" is two unrelated CE systems, not one: `TileEntityMachineDiesel`
(small single-block diesel HE generator, registry name `machine_diesel`) and
`TileEntityMachineTurbineGas` (a self-contained gas-combustion **steam turbine**, i.e. gas is burned
to boil water to spin a turbine internally, not burned directly into HE). `TileEntityMachineIGenerator`
("Industrial Generator") is a red herring for both: it's a `BlockDummyable` multiblock whose TE
`update()` body is empty and whose block's only real behavior is a "memorial" look-overlay easter egg
(`"In memory of all that we have lost"`) — CE ships it as a non-functional decorative husk, not a
generator. Do not budget it as real generator content; port it (if at all) as inert decoration.

Every generator in this family that actually produces power is `IEnergyProviderMK2`-only — none of
them are also `IEnergyReceiverMK2`/`IEnergyConductorMK2` — and every one already uses the exact
`tryProvide`/`trySubscribe` push model this port's Phase 0 `PowerNetMK2` implements, not a
hypothetical different energy API. Two members of the "RTG" name cluster
(`TileEntityDiFurnaceRTG`, `TileEntityRtgFurnace`) are **not HE generators at all** — they're
smelting/blast-furnace machines that burn RTG pellets as a heat-rate multiplier for vanilla/CE
smelting recipes, with zero `IEnergyProviderMK2` involvement. Bucket them as processing machines
that happen to share fuel with the RTG family, not as generators.

## Class inventory by family

### Burner/steam engines
| Class | Shape | Notes |
|---|---|---|
| `TileEntityMachineSteamEngine` (block `MachineSteamEngine`, regname `machine_steam_engine`) | `BlockDummyable` multiblock (`getDimensions()` = `{1,0,5,1,1,1}`, `getOffset()`=1), core TE extends `TileEntityLoadedBase` directly (not `TileEntityMachineBase` — **no item inventory, no GUI, not `IGUIProvider`**) | Pure fluid->HE converter: tank 0 = `Fluids.STEAM` (2000 cap), tank 1 = `Fluids.SPENTSTEAM` (20 cap). Reads `FT_Coolable` trait off the input fluid type to compute `ops`/`heatEnergy`/efficiency (0.85 static), converts to `powerBuffer`, `tryProvide`s it out 3 fixed connector offsets computed from `ForgeDirection` + `BlockDummyable.offset`-encoded rotation. Rotor angle (`rotor`/`lastRotor`/`acceleration`) is pure client animation state, synced via a custom `serialize`/`deserialize` (not vanilla NBT) plus one-shot `serializeInitial`. Also implements `IConfigurableMachine` (`steam_engine` config: `steamCap`, `ldsCap`, `efficiency`) and `IConnectionAnchors`/`IFluidCopiable`. |
| `TileEntityMachineCombustionEngine` (block `MachineCombustionEngine`, regname `machine_combustion_engine`) | `BlockDummyable` multiblock, TE extends `TileEntityMachinePolluting` (5 slots, 50 buffer, both wrappers on) | Diesel-burning HE engine with a real inventory + GUI (`ContainerCombustionEngine`/`GUICombustionEngine`). Slot 2 must hold `ModItems.piston_set`; `ItemPistons.EnumPistonType.eff[FT_Combustible.FuelGrade]` sets the burn efficiency multiplier, `setting` (0-30, redstone/GUI-controlled) is the throttle. Burns `tank` (24 000 cap, starts as `Fluids.DIESEL`) via `FT_Combustible`, calls `super.pollute(...)` every 5 ticks (Phase-4 `PollutionHandler` dependency, see Deferred scope). Also wired for OpenComputers (`SimpleComponent`/`CompatHandler.OCComponent`) and Redstone-over-Radio (`IRORValueProvider`/`IRORInteractive`) — both optional-mod integrations, not core to the port. |
| `TileEntityMachineDiesel` (block `MachineDiesel`, regname `machine_diesel`) | **Single-block** `BlockMachineBase` (not dummyable), TE extends `TileEntityMachinePolluting` (4 slots, 100 buffer) | Small standalone diesel generator with GUI (`ContainerMachineDiesel`/`GUIMachineDiesel`). `tank` (16 000 cap `Fluids.DIESEL`) burns 1 mB/tick when `isOn` (toggled via `IControlReceiver.receiveControl`'s `"turnOn"` key) and not redstone-powered; per-tick HE yield comes from `FT_Combustible.getCombustionEnergy()/1000 * fuelEfficiency[grade]` (static `HashMap<FuelGrade,Double>`, config-overridable via `IConfigurableMachine`/`"dieselgen"`). Slot 2 charges held batteries via `Library.chargeItemsFromTE`. Implements `IFluidCopiable` (paste-settings) and iterates all 6 `ForgeDirection`s for `tryProvide`/pollution-smoke/subscribe (no fixed multiblock connector offsets, unlike the engines above). |

### RTGs
| Class | Shape | Notes |
|---|---|---|
| `TileEntityMachineRTG` (block `MachineRTG`, regname `machine_rtg_grey`) | Single-block `BlockContainer`, TE extends `TileEntityLoadedBase` directly | 15-slot **raw `ItemStackHandler`** (not the `TileEntityMachineBase` inventory convention) that only accepts `ItemRTGPellet`s, no extraction override left enabled (commented out in CE — pellets are permanent once inserted except via decay). `RTGUtil.updateRTGs(inventory, allSlots)` sums pellet heat every tick into `heat` (cap 6000), then `power += heat*5`, capped at `maxPower` (1 000 000). Has GUI (`ContainerMachineRTG`/`GUIMachineRTG`). Exposes both `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY` (the raw `ItemStackHandler`) and `CapabilityEnergy.ENERGY` (via `NTMEnergyCapabilityWrapper`, already ported in this repo) directly off the TE's own 1.12 `getCapability`. |
| `TileEntityMachineMiniRTG` (block `MachineMiniRTG`, regname `machine_minirtg`, **and** `MachineMiniRTG` again under regname `rtg_polonium` -> `ModBlocks.machine_powerrtg`) | Single-block `BlockContainer`, TE extends `TileEntityLoadedBase` | **No inventory, no GUI, no fuel item at all** — one class backs two distinct registry blocks and the TE's `update()`/`getMaxPower()` branch on `this.getBlockType() == ModBlocks.machine_powerrtg` to pick between two hardcoded flat rates: `machine_minirtg` = +70 HE/tick, cap 10 000; `machine_powerrtg` ("polonium RTG") = +2500 HE/tick, cap 50 000. Both are decorative black-box generators — free power, no fuel to manage, no config hook (`IConfigurableMachine` not implemented). |
| `TileEntityDiFurnaceRTG` (block `MachineDiFurnaceRTG`, regnames `machine_difurnace_rtg_off`/`machine_difurnace_rtg_on`) | Single-block `BlockContainer` pair (lit/unlit are **two separate `Block` instances**, not a blockstate property), TE extends `TileEntityMachineBase` (9 slots) | **Not an `IEnergyProviderMK2`.** RTG pellets in slots 3-8 (`RTGUtil.updateRTGs`) produce `rtgPower` (cap 6000) used purely as a smelting-speed multiplier against `BlastFurnaceRecipes.getRequiredCounts(in0, in1)` — a hardcoded 2-input custom recipe table (`com.hbm.inventory.recipes.BlastFurnaceRecipes`), not vanilla smelting. Swaps between the two block instances via its own `updateBlockState(boolean, World, BlockPos)` static helper when `isProcessing()`/`canProcess()` transitions. Has GUI (`ContainerDiFurnaceRTG`/`GUIDiFurnaceRTG`). |
| `TileEntityRtgFurnace` (block `MachineRtgFurnace`, regnames `machine_rtg_furnace_off`/`machine_rtg_furnace_on`) | Single-block `BlockContainer` pair (same lit/unlit two-instance pattern), TE extends `TileEntityMachineBase` (5 slots) | Also **not** an `IEnergyProviderMK2`. RTG pellets in slots 1-3 drive `heat` which accelerates **vanilla** `FurnaceRecipes.instance().getSmeltingResult(...)` (ordinary smelting, not a custom recipe table — the only processing machine in this whole family that needs zero new `Recipe<?>` type). Has GUI (`ContainerRtgFurnace`/`GUIRtgFurnace`). |

### Turbines
| Class | Shape | Notes |
|---|---|---|
| `TileEntityTurbineBase` (abstract, no direct block) | n/a | Shared base for `TileEntityMachineIndustrialTurbine` (and, per its own doc comment, intended for future multiblock turbines). Implements the whole `IEnergyProviderMK2`+`IFluidStandardTransceiverMK2` tick loop once: reads `FT_Coolable` off `tanks[0]`, computes `ops` bounded by `consumptionPercent()` (abstract, subclass-supplied), calls the abstract `generatePower(long, int)` hook, then `tryProvide`s over `getPowerPos()` and fluid-transfers/subscribes over `getConPos()` (both abstract — subclasses supply the multiblock's fixed connector geometry). Also owns the shared `onLeverPull()` steam-densification state machine (STEAM -> HOTSTEAM -> SUPERHOTSTEAM -> ULTRAHOTSTEAM -> back to STEAM, resizing tank capacity x10/x1000 each step when `doesResizeCompressor()` is true) and `IRORValueProvider`. |
| `TileEntityMachineTurbine` (block `MachineTurbine`, regname `machine_turbine`) | Single-block `BlockContainer`, TE extends `TileEntityLoadedBase` directly (does **not** use `TileEntityTurbineBase`) | Small standalone turbine, 7-slot raw `ItemStackHandler` (slot 0 = fluid-identifier item to retype the tank, slot 4 = battery charging, slots 5/6 = spent-steam container fill/drain) + GUI (`ContainerMachineTurbine`/`GUIMachineTurbine`). Steam (64 000 cap) -> spent steam (128 000 cap) at 85% of `FT_Coolable` efficiency, capped at `maxPower` 1 000 000. Carries dead migration code (`converted`/`tanks`/`tankTypes` old-`FluidTank` conversion path via `FFUtils`) from an even older CE fluid system — **do not port the migration shim**, only the steady-state `tanksNew`/`FluidTankNTM` path; note this same dead-migration pattern repeats in `TileEntityMachineLargeTurbine` below. |
| `TileEntityMachineLargeTurbine` (block `MachineLargeTurbine`, regname `machine_large_turbine`) | `BlockDummyable` multiblock, TE extends `TileEntityMachineBase` (7 slots, both wrappers on) | 100% `FT_Coolable` efficiency (no multiplier), huge tanks (512 000 / 10 240 000), `maxPower` 100 000 000. Has fan rotor animation (`rotor`/`fanAcceleration`) and looped audio with a randomized per-instance desync offset. Same dead old-`FluidTank` migration shim as `TileEntityMachineTurbine` — skip porting it. |
| `TileEntityMachineIndustrialTurbine` (block `MachineIndustrialTurbine`, regname `machine_industrial_turbine`) | `BlockDummyable` multiblock, TE **extends `TileEntityTurbineBase`** | The one turbine that actually uses the shared base. Adds a flywheel spin-up model (`flywheel_energy`/`spin`/`FLYWHEEL_MAX_ENERGY`) so output ramps rather than snapping to target, `consumptionPercent()`=0.2 (consumes at most 20% of input tank per tick), `doesResizeCompressor()`=true, and `IConfigurableMachine` (`steamturbineIndustrialMk2`: `inputTankSize`, `outputTankSize`, `efficiency`). |
| `TileEntityMachineTurbineGas` (block `MachineTurbineGas`, regname `machine_turbine_gas`) | `BlockDummyable` multiblock, TE extends `TileEntityMachineBase` (2 slots) | The real "gas generator": self-contained gas-combustion **steam** turbine in one TE — burns `tanks[0]` (gas family fluids, per-fluid max-consumption table in static `fuelMaxCons`) to boil `tanks[2]` (water) into `tanks[3]` (hot steam) with an RPM/temp/throttle state machine (`autoMode`, `powerSliderPos`, startup/shutdown `counter`), then feeds HE off that internally — **does call `PollutionHandler`/`PollutionHandler.PollutionType` directly** (imports it, Phase-4 dependency, see below). Has GUI (`ContainerMachineTurbineGas`/`GUIMachineTurbineGas`), OpenComputers, and `IRORValueProvider`/`IRORInteractive` like the combustion engine. |

### Solar
| Class | Shape | Notes |
|---|---|---|
| `TileEntitySolarBoiler` (block `MachineSolarBoiler`, regname `machine_solar_boiler`) | `BlockDummyable` multiblock (dimensions `{2,0,1,1,1,1}`), TE extends `TileEntityLoadedBase` directly (**no inventory, no GUI**) | Converts accumulated `heat` (fed externally by a paired `TileEntitySolarMirror`, capped `maxHeat`=320 000) into `Fluids.STEAM` at a fixed 1 heat : 100 mB : 1 water ratio, with a 0.999 per-tick heat decay. `IFluidStandardTransceiver` only — **produces no HE directly**, must be piped to a turbine. |
| `TileEntitySolarMirror` (block `SolarMirror`, regname `solar_mirror`) | Plain `BlockContainer` (not dummyable — mirrors are independently placed and each just points at one target), TE extends `TileEntityTickingBase` | Tracks a fixed target position (`tX`/`tY`/`tZ`, set via `setTarget`, presumably by the Phase-2/3 "mirror tool" item flagged in `items_tool.md`'s bucket (c)). Every tick, checks real sky light (`world.getLightFor(EnumSkyBlock.SKY, pos) - skylightSubtracted - 11`, `canSeeSky`) and, if lit, adds that sun value directly into the target `TileEntitySolarBoiler.heatInput` by looking up the TE at `(tX, tY-1, tZ)` — a **direct cross-TE field write**, not a capability or the HE network. No HE, no fluids on the mirror itself. |

### Non-functional / not-a-generator
| Class | Notes |
|---|---|
| `TileEntityMachineIGenerator` + `MachineIGenerator` (regname not in the grep list above — registered separately; block is `BlockDummyable` with `getDimensions()` = all zeros and an empty-body `update()`) | Decorative memorial easter egg only (`ILookOverlay` text "In memory of all that we have lost"). No power, no inventory, no GUI. Do not budget as a real generator; port as inert multiblock decoration or drop, at the implementer's discretion. |
| `MachineGenerator` (regname not applicable — plain `Block`, no paired TE at all) | Zero-logic decorative block (drops an advanced circuit item on break). Not part of the power-generation system despite the name; likely a leftover/legacy block. |

## Phase-2-safe scope

Everything above **except** the four items called out in "Deferred scope" is portable now as far as
its own logic goes, *given* the shared Phase 2 prerequisites this port doesn't have yet (see Key
design decisions): a `BlockEntity` base-class layer equivalent to `TileEntityLoadedBase`/
`TileEntityMachineBase`/`TileEntityTickingBase`, `FluidTankNTM`, and (for every machine with a GUI)
an `AbstractContainerMenu`+`Screen` pair. That's 11 of the 15 TE/block pairs with real, useful
behavior: steam engine, combustion engine, diesel generator, RTG, mini RTG (both variants), small
turbine, large turbine, industrial turbine, gas turbine, solar boiler + solar mirror. All of them
are single-network-role `IEnergyProviderMK2` producers (or, for solar boiler/mirror, pure
`IFluidStandardTransceiverMK2` fluid producers) — none need `IEnergyReceiverMK2`/
`IEnergyConductorMK2` behavior, so there's no risk of them needing conductor/network-graph pieces
beyond what Phase 0 already ported.

`TileEntityDiFurnaceRTG` and `TileEntityRtgFurnace` are also safe on the RTG-fuel side (RTGUtil,
`ItemRTGPellet` decay), but `TileEntityDiFurnaceRTG`'s recipe lookup (`BlastFurnaceRecipes`) needs
the cross-cutting `RecipesCommon`/JSON-recipe gap resolved first (see Deferred scope) —
`TileEntityRtgFurnace` has no such gap since it uses vanilla smelting recipes directly via whatever
this port's `RecipeManager` access ends up being.

The two non-functional stubs (`MachineIGenerator`/`MachineGenerator`) are trivially safe (there's
nothing to break) but carry zero gameplay value — treat as filler, not part of Phase 2's real
power-generation deliverable.

## Deferred scope

1. **Combustion engine's and diesel generator's pollution mechanic** — both extend
   `TileEntityMachinePolluting`, whose `pollute(...)` methods call
   `PollutionHandler.incrementPollution(world, pos, type, amount)` directly. `PollutionHandler` is
   explicitly Phase 4 (world/simulation) per `docs/phase0/STATUS.md`'s deferred-gaps list. The
   generator logic itself (fuel burn -> HE) does not depend on pollution to function — the safe path
   is to port `TileEntityMachinePolluting`'s tank bookkeeping (`smoke`/`smoke_leaded`/`smoke_poison`
   `FluidTankNTM`s, `getSmokeTanks()`, NBT read/write) now, but stub `pollute(...)` as a no-op (or
   route it through a small interface `PollutionHandler` can implement later) until Phase 4 lands the
   real system. Do not block the whole engine on this.
2. **Gas turbine's pollution call** — `TileEntityMachineTurbineGas` imports and calls
   `com.hbm.handler.pollution.PollutionHandler`/`PollutionHandler.PollutionType` directly (not via
   `TileEntityMachinePolluting` — it extends plain `TileEntityMachineBase`). Same Phase 4 dependency
   and same recommended stub-it-out approach.
3. **`TileEntityDiFurnaceRTG`'s recipe lookup** needs `com.hbm.inventory.recipes.BlastFurnaceRecipes`
   (a hardcoded 2-item-input custom recipe table) converted to a JSON `Recipe<?>` type. This is the
   same cross-cutting gap `docs/phase1/STATUS.md` already flags for `com.hbm.inventory.RecipesCommon`
   and `com.hbm.inventory.recipes.loader.GenericRecipe(s)` — don't re-solve it in this area; just note
   that `TileEntityDiFurnaceRTG` is blocked on whichever package ends up owning that conversion.
4. **Every GUI-bearing machine in this family** (combustion engine, diesel generator, RTG,
   di-furnace RTG, RTG furnace, small/large/industrial/gas turbine — 8 of the 15) needs an
   `AbstractContainerMenu`+`Screen` pair before its GUI is usable. Confirmed by grep: this port has
   **zero** `AbstractContainerMenu`/`Screen` subclasses anywhere yet (matches Phase 1's own finding).
   This is a shared cross-cutting prerequisite, not specific to power generation — whichever Phase 2
   package lands first should stand up the base menu/slot-sync pattern the rest can reuse.

## Key design/API decisions

- **HE energy API is confirmed unchanged from Phase 0's ported shape.** Every real generator calls
  the exact same push-model helpers already in this repo: `IEnergyProviderMK2.tryProvide(Level, int,
  int, int, Direction)` (or the `BlockPos` overload) to hand power to a neighbor conductor/receiver,
  and (for fluid producers) `IFluidReceiverMK2/ProviderMK2.trySubscribe(...)`/`tryProvide(FluidTankNTM,
  ...)`. No generator here needs `IEnergyConductorMK2` or `IEnergyReceiverMK2` — they're all pure
  sources. `PowerNetMK2`, `Nodespace.PowerNode`, and the `IEnergyHandlerMK2`/`IEnergyConnectorMK2`
  contracts are all already ported and need no changes for this family.
- **The HE<->NeoForge-Energy capability bridge already exists at the per-block level and should be
  reused as-is.** `com.hbm.capability.NTMEnergyCapabilityWrapper` (already ported, wraps any
  `IEnergyHandlerMK2` block entity as NeoForge's `net.neoforged.neoforge.energy.IEnergyStorage`,
  gated by `GeneralConfig.conversionRateHeToRF`) is exactly the class CE's own generators use for
  their old `CapabilityEnergy.ENERGY` exposure (`TileEntityMachineRTG`, `TileEntityMachineMiniRTG`,
  `TileEntityMachineSteamEngine`, `TileEntityMachineTurbine` all did this in CE). This port's
  `PowerNetMK2` javadoc calls the network-level FE bridge "deferred behind a config flag" — that's
  the *network-wide* leftover-power siphon in `tryProvide`, which is genuinely not ported yet.
  The *per-block* capability wrapper is a different, already-finished piece: every Phase 2 generator
  block entity should register `Capabilities.EnergyStorage.BLOCK -> new
  NTMEnergyCapabilityWrapper(this)` for parity with CE's FE exposure, independent of whether the
  network-wide bridge ever lands. **Not independently verified against a NeoForge jar in this
  sandbox** (network to maven.neoforged.net is blocked, no local NeoForge jar found) — the item-level
  analogue (`event.registerItem(Capabilities.EnergyStorage.ITEM, ...)`) is already live in
  `com.hbm.capability.ModCapabilities`, and `registerBlockEntity`/`Capabilities.*.BLOCK` is inferred
  by direct symmetry with that confirmed-working call, not read off a jar — confirm the exact method
  name at implementation time.
- **Fluid handling: `com.hbm.inventory.fluid.tank.FluidTankNTM` does not exist in this port yet**,
  even though it's already imported by ported code (`FluidType`, `NTMFluidHandlerWrapper`) — this
  matches the gap `docs/phase0/STATUS.md`'s build-verification section already names. Every
  fluid-handling generator in this family (steam engine, combustion engine, diesel generator, all
  four turbines, solar boiler) is built directly on `FluidTankNTM[]` arrays and CE's own
  `IFluidStandardTransceiver`/`IFluidStandardTransceiverMK2`/`IFluidStandardSenderMK2` interfaces —
  **not** any NeoForge world-fluid-block system (confirmed: none of these TEs place or read fluid
  blocks in the world; it's all tank-to-tank `FluidStack` transfer over the HBM API). Porting
  `FluidTankNTM` (plus the already-referenced `IFluidReceiverMK2`/`IFluidProviderMK2`/`IFluidUserMK2`
  under `com.hbm.api.fluidmk2`, which also don't exist yet — only `IFluidRegisterListener` does) is a
  hard prerequisite for every generator in this family except the two RTGs and the two RTG-furnaces.
- **`com.hbm.tileentity.IPersistentNBT` package placement is a live, not just theoretical,
  inconsistency.** `docs/phase0/STATUS.md` flags the `com.hbm.tileentity` vs `com.hbm.blockentity`
  naming decision as needing an explicit call "before Phase 2 block entities land" — but
  `com.hbm.blocks.BlockDummyable` (already ported, used by 8 of this family's 15 block classes:
  steam/combustion engines, large/industrial turbines, gas turbine, solar boiler, IGenerator, and
  transitively by `BlockDummyableMBB`) **already imports `com.hbm.tileentity.IPersistentNBT`** and
  calls `IPersistentNBT.restoreData(level, corePos, stack)` from `setPlacedBy`. That file does not
  exist yet (confirmed by search), so the decision is *already made* by the one file that depends on
  it — `com.hbm.tileentity.IPersistentNBT`, matching CE's original path, not a renamed
  `com.hbm.blockentity` package. Whoever ports the interface should place it there to match, unless a
  deliberate decision is made to rename it and fix up `BlockDummyable`'s import in the same change.
  No other file in the port yet references `com.hbm.tileentity.*`, so the blast radius of getting
  this wrong is currently limited to that one import.
- **No `BlockEntity` base-class layer exists yet at all** — confirmed by search: no
  `com.hbm.tileentity.*` or `com.hbm.blockentity.*` `BlockEntity` subclass exists anywhere in this
  port. `TileEntityLoadedBase` (network-tick/dirty-tracking base, ~288 lines in CE),
  `TileEntityMachineBase` (adds the checked `ItemStackHandler` inventory + slot-access contract, 4
  constructor overloads taking `scount`/`slotlimit`/wrapper-enable flags, ~385 lines),
  `TileEntityMachinePolluting` (adds the 3 smoke tanks), and `TileEntityTickingBase` (13-line
  ticking-only base used by `TileEntitySolarMirror`) are all real CE base classes this family's TEs
  build on and none of them are ported. This is the single biggest shared Phase 2 prerequisite for
  this area (bigger than the GUI framework gap) — recommend standing these up once, early, rather
  than each machine area inventing its own.
- **On/off block-instance pairs, not blockstate properties.** CE encodes the lit/unlit state of
  `TileEntityDiFurnaceRTG` and `TileEntityRtgFurnace` as **two separate registered `Block`s**
  (`machine_difurnace_rtg_off`/`_on`, `machine_rtg_furnace_off`/`_on`), swapped in-place via a static
  `updateBlockState(boolean, World, BlockPos)` helper on the block class (`Block.getStateFromMeta`
  substitution + `world.setBlockState`). This is portable as-is (two `DeferredBlock`s + a swap
  helper) and does not need a `BooleanProperty`/blockstate-property redesign, though a property would
  be the more idiomatic NeoForge shape if the port wants to consolidate the pair into one block later.
- **`BlockDummyable`'s multiblock metadata encoding is already ported and locked in.** The 0-15
  `IntegerProperty META` (0-5 dummy-to-core direction, 6-11 same with "extra" flag, 12-15 core's own
  rotation) is preserved bit-for-bit from CE's `ForgeDirection`-based metadata, per that file's own
  javadoc. Every multiblock generator here (steam/combustion engines, large/industrial turbines, gas
  turbine, solar boiler) can reuse `findCore`/`getDimensions`/`getOffset`/`fillSpace` unchanged; none
  of them need `BlockDummyableMBB`'s arbitrary-AABB footprint variant (all use plain rectangular
  `int[6]` dimensions). `BlockDummyableMBB` itself still needs `com.hbm.handler.MultiblockBBHandler`
  (not ported, referenced only by that one file) but no generator in this family uses it.
- **Two turbines carry dead migration code that should not be ported.** `TileEntityMachineTurbine`
  and `TileEntityMachineLargeTurbine` both still contain CE's one-time `FluidTank[]`/`Fluid[]` ->
  `FluidTankNTM[]` conversion shim (`converted` flag, `FFUtils.serializeTankArray`/
  `deserializeTankArray`) left over from an earlier CE fluid-system migration that has nothing to do
  with this 1.12->1.21 port. Port only the steady-state `tanksNew`/`FluidTankNTM` code path.
- **RTG pellet fuel/decay is item-side state, not TE state.** `ItemRTGPellet` stores remaining
  lifespan on the *pellet stack's* NBT tag (`PELLET_DEPLETION`, a `long`), decremented once per tick
  the pellet contributes heat (`ItemRTGPellet.decay`/`handleDecay`, called from
  `RTGUtil.updateRTGs`). Decayed-out pellets are swapped for `instance.getDecayItem()` in place. This
  is pure `ItemStack` NBT -> straightforward Data Component candidate for whichever Phase 1/2 item
  area owns `ItemRTGPellet` (not itself in the `machine`-TE scope this report covers, but every RTG
  generator here depends on it).
- **NBT key inventory** (for the safe-scope generators, so the eventual port keeps read/write
  symmetry): steam engine — `powerBuffer`(long), `acceleration`(float), tanks `"s"`/`"w"`; combustion
  engine — `setting`(int), `power`(long), `isOn`(bool), `tank`, `tenth`(int); diesel generator —
  `isOn`(bool), `powerTime`(long, note the NBT key differs from the field name `power`),
  `powerCap`(long), `tank`; RTG — `heat`(int), `power`(long), `inventory` (serialized
  `ItemStackHandler` compound); mini RTG — none (fully stateless besides `power`, which CE doesn't
  even persist to NBT — only synced over the network buffer); turbines (base) —
  `water`/`steam` tanks, `power`(long); industrial turbine adds `lastPowerTarget`/`flywheel_energy`/
  `maxPower`(all long)/`spin`(double); solar boiler — `heat`(int), `tank0`/`tank1`; solar mirror —
  `targetX`/`targetY`/`targetZ`(int).

## Open questions / risks

- **Block-entity base-class + `IPersistentNBT` package layout must be settled before any of this
  family's TEs are written**, not just before Phase 2 "starts" abstractly — `BlockDummyable` already
  hard-codes the `com.hbm.tileentity` answer via a currently-dangling import, so treat that as the
  default unless a maintainer overrides it explicitly (see Key design decisions above).
- **`FluidTankNTM` and the `com.hbm.api.fluidmk2` receiver/provider/user interfaces are a bigger
  blocker than they first appear.** They're not specific to this family, but this family is unusually
  dense with fluid-handling machines (8 of 15 real generators move fluid), so whichever area ports
  `FluidTankNTM` should be sequenced early relative to this one, or this area should treat porting a
  minimal `FluidTankNTM` as its own first work item.
- **GUI/menu framework**: confirmed absent (zero `AbstractContainerMenu`/`Screen` in this port).
  8 of 15 generators need one. Recommend whichever Phase 2 package lands first builds the reusable
  pattern (slot sync, a generic "progress bar"/"tank" widget base) rather than each machine
  reinventing it — CE's own `GuiInfoContainer` (used by the combustion engine, a `GuiScreen` subclass
  with a built-in scrollable info panel) suggests at least one shared GUI base is worth porting once.
- **Optional-mod integration should probably be dropped, not ported.** `TileEntityMachineCombustionEngine`,
  `TileEntityMachineTurbine`, `TileEntityMachineLargeTurbine`, `TileEntityMachineIndustrialTurbine`,
  and `TileEntityMachineTurbineGas` all carry OpenComputers (`li.cil.oc.api.*`, `@Optional.InterfaceList`)
  integration, and the two combustion/gas-turbine engines also carry Redstone-over-Radio
  (`com.hbm.api.redstoneoverradio.*`) integration. Neither mod has a confirmed NeoForge 1.21 build;
  recommend explicitly deciding to drop both integrations for this port rather than silently carrying
  the interfaces forward unimplemented — flagging here since it touches 5 of this family's classes.
- **RTG-furnace/di-furnace-RTG classification**: don't miscount `TileEntityDiFurnaceRTG`/
  `TileEntityRtgFurnace` as part of the "power generation" deliverable's HE output — they produce
  zero HE. If Phase 2's scope document buckets by directory (`tileentity/machine`) rather than by
  function, these two will show up in this family by name only; they belong with whatever bucket
  owns RTG-fuel-accelerated smelting, which may or may not be this same package depending on how
  Phase 2 work gets split.
- **`TileEntityMachineIGenerator`/`MachineGenerator` are safe to deprioritize entirely** — confirmed
  non-functional in CE itself (not a porting gap, CE ships it this way deliberately as an easter egg).
  Don't spend Phase 2 budget making it "work" since it never did.
- **Mini RTG's two-variant-from-one-class pattern** (`getBlockType() == ModBlocks.machine_powerrtg`
  branch) needs a decision: port as one `BlockEntity` class checking `getBlockState().getBlock()`
  identity against two `DeferredBlock`s (matching CE exactly), or split into two TE classes/a shared
  config value. Either works; flagging only so it isn't silently "fixed" into something CE didn't do.
