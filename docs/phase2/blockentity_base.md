# Block-entity (TileEntity) base framework — Phase 2 prerequisite research

Sources:
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/TileEntityLoadedBase.java`,
  `TileEntityMachineBase.java` (CE's own two-tier base hierarchy — CE is already a modernized fork
  running Forge capabilities/`ItemStackHandler`, not raw 1.7-style `ISidedInventory`)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/**/*.java` (387 `TileEntity*.java` files total,
  271 of them under `tileentity/machine/`) surveyed for shared behavior
- `upstream/hbm-ce/src/main/java/com/hbm/api/{block,fluid,fluidmk2}/*.java` (`IToolable`,
  `IFluidStandardReceiver`/`Transceiver`, `IFluidReceiverMK2`/`ProviderMK2`/`UserMK2`)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/fluid/tank/FluidTankNTM.java`
- This port's `src/main/java/com/hbm/api/energymk2/*.java`, `com/hbm/capability/*.java`,
  `com/hbm/api/block/IToolable.java`, `com/hbm/api/tile/{ILoadedTile,IWorldRenameable}.java`,
  `com/hbm/lib/{CapabilityContextProvider,ItemStackHandlerWrapper}.java`,
  `com/hbm/blocks/ModBlocks.java`, `com/hbm/blocks/BlockDummyable.java`, and the four already-ported
  ad hoc block entities in `com/hbm/blocks/generic/{BlockLoot,BlockSkeletonHolder,DecoBlockAlt,
  BlockSupplyCrate}.java`
- `upstream/neo-edition/src/main/java/com/hbm/blockentity/{LoadedBaseBlockEntity,
  MachineBaseBlockEntity,NtmBlockEntityTypes}.java` and `com/hbm/blocks/machine/MachinePressBlock.java`
  (cross-checked for confirmed NeoForge 1.21.1 API shape only — CE remains the sole source of truth
  for behavior)
- `docs/phase0/STATUS.md`, `docs/phase1/STATUS.md`, `docs/phase1/items_tool.md` (structural model)

## Headline finding

The task's framing ("none exist in the port yet") is half right and worth correcting precisely,
because the corrected picture changes the scope of this package:

- **No shared base class, and no "machine" (ticking, inventoried, energy/fluid-capable) block
  entity exists.** This is the real gap and the actual subject of this report.
- **Four content block entities already exist**, added during Phase 1 for blocks that needed a
  small amount of container/tick state: `BlockLoot.LootBlockEntity`,
  `BlockSkeletonHolder.SkeletonHolderBlockEntity`, `DecoBlockAlt.StatuePulseBlockEntity`,
  `BlockSupplyCrate.SupplyCrateBlockEntity`. Each is a package-private static nested class that
  `extends BlockEntity` **directly** — no shared base, no `com.hbm.tileentity`/`com.hbm.blockentity`
  package, registered ad hoc as a `Supplier<BlockEntityType<...>>` field on its own block-registration
  class via `ModBlocks.BLOCK_ENTITY_TYPES.register(...)`. This is not a mistake to fix retroactively
  (they're simple enough not to need the machine hierarchy this report designs), but it does mean
  Phase 2's base-class design is not landing in a vacuum — it establishes the pattern these four
  informal one-offs never had to.
- **The registration plumbing already exists and is empty, waiting.** `ModBlocks.java` already
  declares `public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
  DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MainRegistry.MODID);`, registered in
  `ModBlocks.register(modEventBus)` right after `BLOCKS.register(modEventBus)`. Phase 0's own
  javadoc on that field says explicitly: "Any area registering a block entity should add its
  `BLOCK_ENTITY_TYPES.register(...)` calls the same way `BLOCKS.register(...)` calls already work."
  Confirmed pattern (already used by the four ad hoc block entities above, not just seen in Neo
  Edition):
  ```java
  DeferredBlock<MyMachineBlock> block = registerBlock("my_machine", () -> new MyMachineBlock(...));
  MY_MACHINE_ENTITY_TYPE = ModBlocks.BLOCK_ENTITY_TYPES.register("my_machine",
          () -> BlockEntityType.Builder.of(MyMachineBlockEntity::new, block.get()).build(null));
  ```
- **This port's own energy/fluid API already assumes a BlockEntity-shaped machine.** Phase 0 ported
  `com.hbm.api.energymk2.{IEnergyHandlerMK2,IEnergyConductorMK2,IEnergyReceiverMK2,
  IEnergyProviderMK2}` and `com.hbm.capability.{NTMEnergyCapabilityWrapper,NTMFluidHandlerWrapper}`
  *before* any block entity existed to implement them — every one of these classes does
  `(BlockEntity) this` or takes a `BlockEntity handler` constructor argument. This is a real,
  already-committed constraint on the base class design, not a free choice: whatever the shared
  machine base is, it must be a `BlockEntity` subclass that can additionally implement these
  marker/handler interfaces, because the wrapper classes that expose them to NeoForge capabilities
  already exist and already expect that shape.

## Phase-2-safe scope

### CE's shared TileEntity behavior (what every machine needs)

CE's own hierarchy is exactly two tiers, and it is *already* capability-based (this fork runs
`ItemStackHandler`/`IItemHandlerModifiable`, not 1.7-style `ISidedInventory`), which maps far more
directly onto NeoForge 1.21.1 than a stock-1.12.2 TileEntity tree would:

- **`TileEntityLoadedBase extends TileEntity implements ILoadedTile, IBufPacketReceiver`** (288
  lines) — the universal base. Owns: `isLoaded`/`onLoad`/`onChunkUnload` tracking; `muffled`/
  `tilted` fields with NBT round-trip (`readFromNBT`/`writeToNBT` calling `super` first); a
  `ByteBuf`-based sync pair (`serialize`/`deserialize`, `serializeInitial`/`deserializeInitial` for
  the one-time chunk-load payload via `getUpdateTag`/`handleUpdateTag`); two throttled sync-packet
  senders (`networkPackNT` — hash-dedup'd, `networkPackMK2` — dirty-flag-gated via `dataChanged()`);
  and the `checkTilt`/`TiltType` machine-gravity wobble effect with three fixed floor-shape helpers
  (`standardFloor3x3/5x5/7x7`). **80 classes extend this directly** (non-inventoried, non-ticking
  or custom-ticking TEs — proxies, doors, keypads, etc.).
- **`TileEntityMachineBase extends TileEntityLoadedBase implements IWorldRenameable`** (385 lines) —
  the inventoried-machine base. Owns: an `ItemStackHandler inventory` built via an overridable
  `getNewInventory(scount, slotlimit)` factory (auto-`markDirty()` on content change) plus
  `resizeInventory`; a `getCheckedInventory()` wrapper for Container/GUI use that re-validates
  through `isItemValidForSlot`; the full `getCapability`/`hasCapability` override exposing
  `CapabilityItemHandler`/`CapabilityFluidHandler`/`CapabilityEnergy` gated by two constructor-time
  booleans (`enablefluidWrapper`, `enableEnergyWrapper`); **per-accessor-position capability-wrapper
  caching** (`fluidWrapperCache`, `itemWrapperCache` keyed by `(facing, accessorPos)`) so a
  multiblock's dummy ports each get a stable wrapper identity instead of a fresh one per query
  (documented in-file as required for AE2-style external cache-by-identity consumers);
  `getAccessibleSlotsFromSide`/`canInsertItem`/`canExtractItem` hooks; custom-name plumbing
  (`getName`/`getDisplayName`/`hasCustomName`/`setCustomName`, abstract `getDefaultName()`);
  `countMufflers()`/`getVolume(int)` (adjacent `ModBlocks.muffler` silencing); and
  `writeToNBT`/`readFromNBT` chaining that serializes the whole inventory under one `"inventory"`
  NBT key. **111 classes extend this directly**; more reach it transitively through intermediate
  bases (`TileEntityPneumaticMachineBase`, pylon/turret/RBMK-family bases, etc.) that this report
  does not need to enumerate — they all still bottom out at these same two classes.
- A concrete example (`TileEntityMachinePress`, read in full) shows the full idiom in one file:
  `@AutoRegister` class annotation, constructor calling `super(slotCount)`, `implements ITickable,
  IGUIProvider`; `update()` doing all game logic server-side only (`if (!world.isRemote)`) and
  calling `this.networkPackNT(50)` at the end; `serialize`/`deserialize` overrides that call `super`
  first then add their own fields; `readFromNBT`/`writeToNBT` overrides doing the same, including a
  defensive `resizeInventory` if a saved inventory came back smaller than expected; per-slot
  `isItemValidForSlot`/`canExtractItem`/`getAccessibleSlotsFromSide` overrides; and
  `provideContainer`/`provideGUI` (`IGUIProvider`) returning a paired `Container`/`GuiScreen`.

### Interface counts (repo-wide grep, CE source)

| Interface / behavior | Count | Notes |
|---|---|---|
| `extends TileEntityMachineBase` (direct) | 111 | Inventoried machines |
| `extends TileEntityLoadedBase` (direct) | 80 | Non-inventoried or custom-inventory TEs |
| `implements ITickable` (under `tileentity/`) | 228 | Most machines and non-machines alike tick |
| `implements IToolable` | 71 | Screwdriver/wrench/hand-drill/torch/bolt/defuser right-click interactions |
| `implements IFluidStandard{Receiver,Sender,Transceiver}` | 112 | Legacy (CE-marked `@Deprecated`) fluid-network interface, still the majority fluid contract in practice |
| `implements IEnergyConductorMK2` | 9 | HE cable/conductor nodes specifically (most machines are `IEnergyReceiverMK2`/`ProviderMK2` instead, not counted separately here) |
| Total `TileEntity*.java` files (repo-wide) | 387 | 271 under `tileentity/machine/` alone |

### Already-ported dependencies this base class can build directly on

Nothing below needs to be invented by this package — it already exists in `src/main/java/com/hbm`,
ported ahead of any block entity to consume it:

- `com.hbm.api.tile.ILoadedTile`, `IWorldRenameable` — direct CE-shape ports, ready to implement.
- `com.hbm.api.block.IToolable` — ported (`BlockPos`-overload added, same `ToolType` enum, same
  `RecipesCommon.ComparableStack`-keyed lookup — itself a forward reference to the not-yet-ported
  `RecipesCommon`, consistent with the rest of the port).
- `com.hbm.api.energymk2.{IEnergyHandlerMK2,IEnergyConductorMK2,IEnergyReceiverMK2,
  IEnergyProviderMK2}` — ported, already `BlockEntity`-shaped (see Headline finding).
- `com.hbm.capability.{NTMEnergyCapabilityWrapper,NTMFluidHandlerWrapper,
  NTMCableEnergyCapabilityWrapper}` — ported, each wraps a `BlockEntity` (plus an optional accessor
  `BlockPos`) as the matching NeoForge capability (`IEnergyStorage`, `IFluidHandler`). These are the
  server-side of the exact same accessor-aware caching CE's `TileEntityMachineBase.getCapability`
  does — the base class's job is to *call* these from `getCapability`/register them via
  `RegisterCapabilitiesEvent`, not to reimplement their logic.
- `com.hbm.lib.CapabilityContextProvider` — the port's replacement for CE's
  `IConditionalInvAccess`/accessor-position threading, already used by both wrapper classes above via
  a `ThreadLocal<BlockPos>` push/pop pair. The base class's `getCapability` should push the accessor
  position the same way CE's does, then let the wrapper read it back via
  `CapabilityContextProvider.getAccessor(this.pos)`.
- `com.hbm.lib.ItemStackHandlerWrapper` — ported, same per-accessor slot-filtering wrapper CE's
  `TileEntityMachineBase.getCapability` builds for the item-handler capability.
- `com.hbm.capability.ModCapabilities.register(RegisterCapabilitiesEvent)` — already wired into the
  mod's `RegisterCapabilitiesEvent` handler, currently registering only two **item** capabilities
  (`Capabilities.EnergyStorage.ITEM` for `IBatteryItem`, `Capabilities.FluidHandler.ITEM` for fluid
  containers). This is the exact spot where **block-entity-side** capability registration
  (`event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MY_TYPE.get(), (be, side) -> ...)`
  per concrete `BlockEntityType`) needs to be added once machine block entities exist — see Key
  design decisions below.
- `ModBlocks.BLOCKS` / `ModBlocks.BLOCK_ENTITY_TYPES` (`DeferredRegister.Blocks` /
  `DeferredRegister<BlockEntityType<?>>`) — both already exist and are already used by four
  registrations (see Headline finding).
- `com.hbm.blocks.BlockDummyable` — the multiblock dummy-block base is already ported (`extends
  BaseEntityBlock implements ICustomBlockHighlight, ICopiable, INBTBlockTransformable`), including
  `findCore`/`findCoreBlockEntity`/orphan-cascade/placement/rotation logic. It already calls
  `level.getBlockEntity(corePos) instanceof MenuProvider`/`ICopiable` for its
  `standardOpenBehavior`/`getSettings`/`pasteSettings`/`infoForDisplay` hooks — i.e. it already
  expects the *core* dummy's block entity to be a real `BlockEntity` implementing those interfaces,
  which is exactly the shape this report's base class produces.

### What Phase 2's base-class package should actually build

Given the above, the *net new* work for this package is genuinely small and mechanical, not a
redesign:

1. A `LoadedBaseBlockEntity`-equivalent (`extends BlockEntity implements ILoadedTile`) porting CE's
   `TileEntityLoadedBase`: `muffled`/`tilted` fields + `saveAdditional`/`loadAdditional`, the
   `networkPackNT`/`networkPackMK2` sync senders rebuilt on NeoForge's packet API, and the
   `checkTilt`/floor-helper machine-gravity effect (config-gated, cosmetic — low risk, straightforward
   port; Neo Edition's version, read for API shape only, needlessly confirms every call maps
   1:1 — `world.markChunkDirty` → `setChanged()`, `SPacketUpdateTileEntity` → the modern
   `getUpdateTag`/`handleUpdateTag`/custom-payload path, etc.).
2. A `MachineBaseBlockEntity`-equivalent (`extends` the above) porting CE's `TileEntityMachineBase`:
   `ItemStackHandler`-backed inventory (constructor-configurable slot count/limit, matching CE's
   `(scount, slotlimit, enableFluidWrapper, enableEnergyWrapper)` overload set), the checked-inventory
   wrapper, the accessor-position-aware `getCapability`/`hasCapability` pair built on the already-ported
   wrapper classes above, `getAccessibleSlotsFromSide`/`canInsertItem`/`canExtractItem`/
   `isItemValidForSlot` hooks, custom-name plumbing, and inventory NBT round-trip via
   `saveAdditional`/`loadAdditional` (`ItemStackHandler.serializeNBT()`/`deserializeNBT()`, same as
   CE — no `ContainerHelper`/`NonNullList` needed, since this port follows CE's `ItemStackHandler`
   convention rather than vanilla `WorldlyContainer`, see Key design decisions).
3. Wiring one `RegisterCapabilitiesEvent`-time helper (in `ModCapabilities` or a sibling class) that,
   for every registered machine `BlockEntityType`, calls `event.registerBlockEntity(...)` for
   whichever of `Capabilities.ItemHandler.BLOCK` / `Capabilities.EnergyStorage.BLOCK` /
   `Capabilities.FluidHandler.BLOCK` the machine's flags enable — the generic-registration
   equivalent of CE's per-instance `hasCapability` checks, done once per `BlockEntityType` instead
   of per-TE (NeoForge's `RegisterCapabilitiesEvent` is inherently type-indexed, not
   instance-indexed, unlike CE's Forge-1.12 `getCapability` override — this is the one genuine
   API-shape difference to design around, not just port).
4. A shared ticking convention: a `com.hbm.tileentity.ITickableBE`-style marker interface with an
   `updateEntity()` (or `tick()`) method that the base class's owning `Block` subclasses call from
   `EntityBlock#getTicker` via `BaseEntityBlock.createTickerHelper(type, expectedType, (lvl, pos,
   st, be) -> be.updateEntity())` — confirmed idiom (see Key design decisions). This lives on the
   BE side as a marker interface (so the base class itself does not have to be abstract-ticking for
   the ~80 non-ticking `TileEntityLoadedBase`-only subclasses), matched by a **shared static helper**
   on whichever new common `Block` base machine blocks extend, so each concrete machine block doesn't
   hand-roll its own `getTicker` boilerplate.
5. `BlockEntityType` registration follows the pattern already established by
   `GenericCrateBlocks.java` (Phase-2-safe, zero new API to learn) — one field + one
   `ModBlocks.BLOCK_ENTITY_TYPES.register(...)` call per concrete machine.

None of the above requires FluidTankNTM, RecipesCommon, or a Menu/Screen framework to exist —
the base class hierarchy, its NBT contract, its capability-registration hook, and its ticking
convention are all self-contained and can be built and reviewed against CE now. What *cannot* be
fully wired without those (see Deferred scope) is any *concrete* machine that needs a fluid tank, a
JSON-recipe lookup, or a player-facing inventory GUI — i.e. nearly all ~111+ concrete machines, but
not the base class itself.

## Deferred scope

These are real dependencies of *concrete* machine block entities, not of the base-class package
itself. Building the base class now and leaving these hooks unconnected until their owning package
lands is the correct sequencing — re-stating PORT_SPEC's own phase boundary, not inventing a new one:

- **Fluid tank abstraction — `com.hbm.inventory.fluid.tank.FluidTankNTM`** (504 lines in CE:
  `IFluidHandler`/`IFluidTank`/`Cloneable`, tracks `FluidType`+amount+capacity+pressure, owns its own
  GUI-gauge rendering hooks and a pluggable `IFluidLoadingHandler` chain for item-fluid interactions).
  **Does not exist in this port yet.** Neither do its two governing interfaces in the port's own
  `com.hbm.api.fluidmk2` package — CE has `IFluidReceiverMK2`, `IFluidProviderMK2`, `IFluidUserMK2`
  there; the port currently has only `IFluidRegisterListener`. This is not a surprise gap: Phase 0's
  own `NTMFluidHandlerWrapper` (already ported) imports all three and `FluidTankNTM` as forward
  references, exactly like every other documented Phase 0 gap. **Owning phase/package**: whichever
  Phase 2 package covers `com.hbm.inventory.fluid`/`com.hbm.api.fluidmk2` (not yet run as its own
  research area per this survey — flagging it as a concrete prerequisite for any fluid-handling
  machine, likely a large fraction of the ~112 `IFluidStandard*` implementors). The base
  `MachineBaseBlockEntity` needs no fluid-specific code itself — it only needs the
  `enableFluidWrapper` flag and the `getCapability` call-through to `NTMFluidHandlerWrapper`, both of
  which are structurally ready today; only the *tank storage itself* is missing.
- **Recipe system — `com.hbm.inventory.RecipesCommon` / `com.hbm.inventory.recipes.loader.
  GenericRecipe(s)`.** Already flagged in `docs/phase0/STATUS.md` and `docs/phase1/STATUS.md` as a
  known cross-cutting gap (also the reason `MachineItems.ItemBlueprints`/`ItemBlueprintFolder`
  don't fully compile yet). Every concrete processing machine (press, furnace-combination,
  assembly, refinery, etc. — the majority of the 271 `tileentity/machine` files) calls into a
  CE-hardcoded recipe list (`PressRecipes.getOutput(...)` in the example read above) that needs a
  JSON `Recipe<?>`-based replacement before that machine's `canProcess()`/tick logic can be ported
  for real. **Not re-solved here** per the task's own instruction — noted purely as "this is what
  concrete machine tick logic depends on," same category as the fluid gap.
- **Menu/Screen framework (`AbstractContainerMenu`+`Screen` pairs, replacing CE's
  `IGUIProvider`).** Confirmed absent by grep (`AbstractContainerMenu`/`extends Screen`: zero real
  hits in `src/main/java/com/hbm`). `docs/phase1/STATUS.md` already recommended building this as an
  early Phase 2 task since "machines need it too" — this report reconfirms that from the machine
  side: every one of CE's ~111 `TileEntityMachineBase` subclasses implements `IGUIProvider`
  (`provideContainer`+`provideGUI`), and the base class's `getCheckedInventory()` method exists
  *specifically* so a `Container`/Menu class has a safe wrapper to bind slots to. The base
  `BlockEntity` class this report designs does not need the Menu framework to exist (it can expose
  `MenuProvider`/`getDisplayName`/a `getCheckedInventory()`-equivalent today), but essentially every
  *concrete* machine block entity needs it to actually be playable. Recommend this be built as its
  own shared Phase 2 package, landing alongside or just before the first concrete machine that
  needs a GUI.
- **World-fluid blocks.** Per Phase 1's own research (referenced by this task's instructions), this
  port has no world-placed fluid-block system at all. This block-entity base package doesn't need
  one either — CE's fluid *tanks* (`FluidTankNTM`, above) are a tile-entity-internal storage
  abstraction, entirely separate from any `LiquidBlock`/world fluid rendering. Flagging only because
  the task asked to check: **not a dependency of this package**, the fluid gap that matters here is
  the tank class, not world fluids.
- **Multiblock framework (`MultiblockHandlerXR`/`MultiblockBBHandler`) and `IPersistentNBT`.**
  Already deferred from Phase 0/1 per `docs/phase0/STATUS.md`'s own list. `BlockDummyable` (already
  ported) already imports and calls `com.hbm.tileentity.IPersistentNBT.restoreData(...)` and
  `com.hbm.handler.MultiblockHandlerXR.{checkSpace,fillSpace}` as forward references — i.e. every
  multiblock *core* block entity this base-class package's consumers eventually write will need
  those two to exist before a multiblock machine (as opposed to a single-block machine) is fully
  wired. Single-block machines (a large fraction of the 111+ direct `TileEntityMachineBase`
  subclasses — e.g. `TileEntityMachinePress` itself is a single block, no dummies) have no
  dependency on either. See also the package-naming decision immediately below, which is entangled
  with `IPersistentNBT`'s own package.

## Key design/API decisions

Every API shape below was confirmed by reading either this port's own already-committed code or
Neo Edition's real source, not invented:

- **`BlockEntity` construction and registration** (confirmed by this port's own
  `GenericCrateBlocks.java`, independently cross-checked against Neo Edition's `NtmBlockEntityTypes`
  — identical shape in both):
  ```java
  public MyMachineBlockEntity(BlockPos pos, BlockState state) {
      super(ModBlockEntityTypes.MY_MACHINE.get(), pos, state);
  }
  // registration:
  ModBlocks.BLOCK_ENTITY_TYPES.register("my_machine",
          () -> BlockEntityType.Builder.of(MyMachineBlockEntity::new, myMachineBlock.get()).build(null));
  ```
  The `.build(null)` (no explicit `DataFixer` type) matches every existing registration in both the
  port and Neo Edition — there is no evidence any block entity in this codebase needs a datafixer
  argument.
- **NBT persistence maps to `saveAdditional`/`loadAdditional`, not `readFromNBT`/`writeToNBT`.**
  Confirmed by Neo Edition's `LoadedBaseBlockEntity`/`MachineBaseBlockEntity`:
  ```java
  @Override
  protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.saveAdditional(tag, registries);
      tag.putBoolean("muffled", muffled);
      // ... own fields
  }
  @Override
  protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
      super.loadAdditional(tag, registries);
      this.muffled = tag.getBoolean("muffled");
      // ... own fields
  }
  ```
  This is a direct structural match for CE's `super` chaining, just renamed and given a
  `HolderLookup.Provider` (needed for `ItemStack`/`Component`-bearing NBT, not for plain
  primitives). **DataComponents are not a replacement for this** — they're an orthogonal mechanism
  for *item-stack* data, not block-entity persistence. This port's own already-ported
  `BlockSupplyCrate` shows exactly where the two meet: the crate's `BlockEntity` still persists its
  contents via plain `CompoundTag` (`saveContents()`/`loadContents()`), but when the block is broken
  and drops as an item, that same NBT is carried across via
  `drop.set(DataComponents.CUSTOM_DATA, CustomData.of(saved))` on the dropped `ItemStack`, and
  restored via `stack.get(DataComponents.CUSTOM_DATA)` in `setPlacedBy`. **Recommendation**:
  machine internal state (progress/burnTime/speed/inventory/energy/fluid) stays plain
  `saveAdditional`/`loadAdditional` NBT — there is no player-facing item round-trip need for most
  machines (they're usually mined and placed empty, matching CE). Only machines whose CE behavior
  explicitly preserves contents across break/replace (rare — CE's `BlockSupplyCrate` pattern, already
  handled) need the `CustomData` component bridge, and that pattern is already established, not
  something this package needs to invent.
- **Capability registration is per-`BlockEntityType`, not per-instance override.** CE's
  `TileEntityMachineBase.getCapability`/`hasCapability` overrides are evaluated per query, per
  instance. NeoForge 1.21.1's `RegisterCapabilitiesEvent` (confirmed already in use in this port's
  own `ModCapabilities.register`, currently item-only) is type-indexed instead:
  ```java
  event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, MY_MACHINE_TYPE.get(),
          (be, side) -> be.enableEnergyWrapper ? new NTMEnergyCapabilityWrapper(be, /* accessor */ null) : null);
  ```
  registered once per `BlockEntityType` at mod-init, not per `getCapability` call. This is the one
  place where the port's design genuinely differs in *shape* from CE's, even though the
  *behavior* (accessor-aware wrapper caching via `CapabilityContextProvider`) carries over
  unchanged. Recommend the base class still exposes `enableFluidWrapper`/`enableEnergyWrapper`
  instance flags (set at construction, mirroring CE) so a single `registerBlockEntity` callback
  registered against the base type (or a small number of per-machine-family `BlockEntityType`s) can
  branch on them, rather than needing one `RegisterCapabilitiesEvent` call per concrete machine
  class.
- **Ticking**: `BlockEntityTicker` is supplied by the `Block`, not the `BlockEntity`, via
  `EntityBlock#getTicker(Level, BlockState, BlockEntityType<T>)`. Confirmed idiom, read directly
  from this port's neighborhood (Neo Edition's `MachinePressBlock`, cross-checked shape only):
  ```java
  @Override
  public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
      return (lvl, pos, st, be) -> { if (be instanceof ITickableBE tickable) tickable.updateEntity(); };
  }
  ```
  or the equivalent `BaseEntityBlock.createTickerHelper(type, expectedType, BlockEntity::tick)`
  vanilla helper. Recommend a small shared `ITickableBE` marker interface (CE's `ITickable.update()`
  renamed to avoid colliding with vanilla's own `net.minecraft.world.level.BlockEntityTicker`
  naming) plus one static helper method on whichever common machine-block base class this port ends
  up with, so each concrete machine block's `getTicker` is one line, not hand-rolled per block.
- **Inventory stays `ItemStackHandler`-based, not vanilla `WorldlyContainer`.** Neo Edition's own
  `MachineBaseBlockEntity` (read for API-shape cross-check) chose `implements WorldlyContainer,
  Nameable, MenuProvider` with a raw `NonNullList<ItemStack> slots` — a legitimate NeoForge pattern,
  but a different one from CE's already-capability-based `ItemStackHandler`. Since this port's
  ground rules make CE the sole source of truth for behavior and Neo Edition cross-check is for API
  shape only, and since CE's own `TileEntityMachineBase` is *already* `ItemStackHandler`-based (not
  a straight 1.12-vanilla-`IInventory` port), **recommend following CE's shape directly**
  (`ItemStackHandler` + `IItemHandlerModifiable` capability exposure), not Neo Edition's
  `WorldlyContainer` re-implementation. This also keeps the base class consistent with the
  already-ported `ItemStackHandlerWrapper` (built for exactly an `ItemStackHandler`-backed
  inventory) and avoids a second inventory abstraction existing in the port for no behavioral gain.

## Open questions / risks

- **`com.hbm.tileentity` vs `com.hbm.blockentity` package naming — unresolved, and now more
  concretely blocking than Phase 0 flagged it.** `docs/phase0/STATUS.md` already named this as an
  open decision needing an explicit call "before Phase 2 block entities land," because
  `com.hbm.tileentity.IPersistentNBT` is referenced (as a forward reference) from
  `BlockDummyable.java`, which is already committed to disk. This survey confirms the stakes are
  larger than that one interface: CE's entire TileEntity hierarchy this report is designing a port
  for (`TileEntityLoadedBase`, `TileEntityMachineBase`, and by extension every concrete machine)
  lives under `com.hbm.tileentity` in CE, and Neo Edition renamed that *entire* tree to
  `com.hbm.blockentity` (confirmed: `LoadedBaseBlockEntity`, `MachineBaseBlockEntity`,
  `NtmBlockEntityTypes` all live there). Two real options, not a false binary:
  - **(A) Preserve `com.hbm.tileentity`** — matches PORT_SPEC's "preserve com.hbm.* package layout
    ... where legal" default, and matches what's *already* committed (`BlockDummyable`'s forward
    reference, `CapabilityContextProvider`'s own javadoc referencing "1.7 com.hbm.tileentity
    IConditionalInvAccess"). Downside: a package literally named `tileentity` containing classes
    that `extends BlockEntity` (CE's own 1.12 name for the concept, not NeoForge's) reads as
    confusing/stale to anyone who knows modern Minecraft naming, and is exactly what Neo Edition's
    authors evidently minded enough to rename.
  - **(B) Adopt `com.hbm.blockentity`** (Neo Edition's choice) — clearer to new contributors,
    matches the class name it actually extends. Downside: breaks the "preserve package layout"
    default for a fairly large, deeply-cross-referenced package (271+ files under CE's
    `tileentity/machine` alone, plus every `IToolable`/`ICopiable`/etc. consumer that currently
    imports from `com.hbm.tileentity` — the port's own `IToolable`/`ILoadedTile`/`IWorldRenameable`
    already live under `com.hbm.api.*`, not `com.hbm.tileentity`, for what it's worth, so the actual
    collision surface is narrower than "271 files" suggests, but `IPersistentNBT` and
    `IGUIProvider`-equivalent and the base classes themselves are still all in question).
  - **This report's recommendation, offered for the record but not self-authorized**: **(A)**,
    preserve `com.hbm.tileentity`, on the grounds that (1) it's the PORT_SPEC default, (2) it's
    already the path one committed file (`BlockDummyable`) forward-references, and (3) the
    "tileentity" name is cosmetic, not load-bearing — but this is exactly the kind of call
    `docs/phase0/STATUS.md` said needs an explicit sign-off before landing, not something to
    silently resolve inside a research report. **Flagging as the single highest-priority open
    decision blocking this package's implementation.**
- **Per-`BlockEntityType` capability registration doesn't obviously scale to ~100+ machine
  classes.** If every concrete machine needs its own `event.registerBlockEntity(...)` call (one per
  capability it supports), `ModCapabilities.register` could grow to hundreds of lines. A registry-
  driven approach (iterate `ModBlocks.BLOCK_ENTITY_TYPES` the same way `ModCapabilities` already
  iterates `BuiltInRegistries.ITEM.stream()` for item capabilities, filtering by `instanceof
  IEnergyHandlerMK2`/etc.) may not be possible for block entities the same way, since
  `RegisterCapabilitiesEvent.registerBlockEntity` takes a `BlockEntityType` up front — it cannot
  filter a live instance stream the way item registration does (items exist as static registry
  singletons at that point; block entities do not, they're per-world-position). This needs a design
  decision by whoever implements this package: one `registerBlockEntity` call per family (e.g. "all
  energy-consuming machines share one call against a common intermediate `BlockEntityType`-agnostic
  helper, invoked once per concrete type") versus a generated/looped registration — not resolved
  here, flagged as a real implementation-time decision.
- **`FluidTankNTM`'s CE implementation is not a clean tile-entity-only class.** The 504-line file
  read for this report pulls in `com.hbm.inventory.gui.element.GUIElements`,
  `com.hbm.inventory.gui.GuiInfoContainer`, and direct `GL11`/`Tessellator` rendering calls — i.e.
  CE couples tank *rendering* directly into the tank *data* class. Whoever ports `FluidTankNTM`
  will need to split client rendering out (matching this port's general client/server separation
  convention, not confirmed here since that package wasn't read in depth) — flagging so the base
  `MachineBaseBlockEntity`'s `enableFluidWrapper` hook isn't designed assuming a same-shaped
  `FluidTankNTM` port lands unchanged.
- **The four existing ad hoc block entities are not retrofitted onto this base, and this report does
  not recommend retrofitting them.** `BlockLoot`/`BlockSkeletonHolder`/`DecoBlockAlt`/
  `BlockSupplyCrate` are simple enough (no inventory-capability exposure, no energy/fluid, no
  `IGUIProvider`) that forcing them onto a `MachineBaseBlockEntity` they don't need would add
  ceremony, not value. Flagging only so a future reviewer doesn't assume these four are Phase 2
  scope creep or a missed base-class application — they predate this package and were a reasonable
  Phase 1 call for their scope.
- **`checkTilt`'s machine-gravity effect is config-gated and cosmetic** (`GeneralConfig.
  enableMachineGravity`/`enable528MachineGravity`) but every concrete machine that wants it must
  implement `getFloorCount()`/`getFloorPosFromIndex(int)` correctly for its own footprint — this is
  per-machine data the base class cannot supply generically (CE's own base class returns `0`/`null`
  defaults, i.e. "off" until a subclass opts in). Low risk, just noting it's a per-concrete-machine
  task, not something the base class resolves once for everyone.
