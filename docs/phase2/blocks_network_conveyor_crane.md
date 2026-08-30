# blocks/network conveyor + crane/inserter system triage (8 conveyor blocks + `CraneSplitter`/`DummyBlockCraneSplitter`, plus 9 crane blocks and the `entity.item` moving-object sub-package)

Source: `hbm-ce/src/main/java/com/hbm/blocks/network/{BlockConveyor*, Crane*, DummyBlockCraneSplitter}.java`,
`hbm-ce/src/main/java/com/hbm/entity/item/{EntityMovingConveyorObject,EntityMovingItem,EntityMovingPackage}.java`,
plus read-only cross-checks of `hbm-ce/src/main/java/com/hbm/tileentity/network/TileEntityCrane*.java`,
this port's `com.hbm.api.conveyor.*` / `com.hbm.api.block.IToolable`, `docs/phase1/blocks_network_rail.md`,
and `docs/phase0/STATUS.md`.

This survey follows up directly on `docs/phase1/blocks_network_rail.md`'s recommendation: porting
`EntityMovingItem`/`EntityMovingConveyorObject` first, then the 8 plain conveyor blocks, was flagged as
"the lightest-weight slice within Phase 2." This report confirms that call, extends it to cover
`CraneSplitter` (also named in that file's `BlockDummyable` list), and separately scopes the 9
`BlockCraneBase`-derived crane/inserter blocks that sit one layer up in real dependency weight.

## Headline finding

There are two genuinely different tiers of difficulty hiding under one package-folder heading:

1. **Conveyors (8 `Block` subclasses, no block entity) + the 3-class `entity.item` moving-object
   sub-package.** This is exactly as small and self-contained as Phase 1's research predicted. Zero
   `BlockContainer`/`TileEntity` coupling. The only real blocker was `EntityMovingItem`, which is a
   plain vanilla-`Entity` subclass (`SynchedEntityData`-equivalent, `readAdditionalSaveData`/
   `addAdditionalSaveData`-equivalent) with no capability, no GUI, no multiblock. **This is genuinely
   the first slice to port in Phase 2's logistics wave**, ahead of everything else in the package.
2. **Crane/inserter blocks (9 `BlockCraneBase` subclasses + `CraneSplitter`'s `BlockDummyable`
   pair).** These looked deceptively similar (also implement `IEnterableBlock`, also move items) but
   every single one needs a real block entity **and**, for 6 of the 9, a dedicated
   `AbstractContainerMenu`+`Screen` pair — a framework this port does not have yet at all (confirmed
   below). These are Phase-2-safe in principle but gated on two shared prerequisites landing first:
   the block-entity base/package-naming decision, and the menu/screen framework.

## Phase-2-safe scope (portable now, once `EntityMovingItem`/`EntityMovingConveyorObject` land)

### Step 0 prerequisite: `com.hbm.entity.item` moving-object sub-package (3 classes, ported together)

- `EntityMovingConveyorObject` (abstract, `extends Entity`) — the shared `onUpdate` tick loop: reads
  the block under the entity, calls `IConveyorBelt.canItemStay`/`getTravelLocation` (already-ported
  Phase 0 interfaces, see below) to compute motion, detects `IEnterableBlock` on move, and does an
  every-20-tick "cram" check (`ServerConfig.CONVEYOR_CRAM_MAX`, an `ExplosionVNT` if too many
  entities stack — depends on `com.hbm.explosion.vanillant.*`, confirmed present in Phase 0/1 scope
  per STATUS.md's gap list, not re-verified in this pass but should be checked at implement time).
  Declares two abstract hooks: `enterBlock(IEnterableBlock, BlockPos, Direction)` and
  `onLeaveConveyor()`.
- `EntityMovingItem` (`extends EntityMovingConveyorObject implements IConveyorItem`) — the one
  conveyor blocks actually spawn. Holds one `ItemStack` via a synced data value
  (`EntityDataManager`/`DataParameter<ItemStack>` in CE — this is `SynchedEntityData`/
  `EntityDataAccessor<ItemStack>` in 1.21.1; `DataSerializers.ITEM_STACK` maps to
  `EntityDataSerializers.ITEM_STACK`). NBT round-trip is two fields (`Item`, `schedule`). On
  `attackEntityFrom`/death it either respawns a vanilla dropped item (`EntityItem` -> `ItemEntity`)
  or hands itself to an `IEnterableBlock`.
- `EntityMovingPackage` (`extends EntityMovingConveyorObject implements IConveyorPackage`) — **not
  named in the task prompt but a real sibling dependency**: `IConveyorPackage`/`IEnterableBlock`
  (already-ported Phase 0 interfaces) are only ever produced/consumed by the crane boxer/unboxer/
  router blocks (`TileEntityCraneBoxer` constructs one, `CraneRouter.java:123` spawns one). It carries
  `ItemStack[]` instead of a single stack. Structurally as simple as `EntityMovingItem`. Recommend
  porting all three `entity.item` classes as one unit even though only conveyors need
  `EntityMovingItem` directly — `EntityMovingPackage` is needed the moment any crane block is
  implemented, and it shares 100% of its abstract base with `EntityMovingItem`, so there is no reason
  to split the sub-package across two work sessions.
- Both `AutoRegister(name = "entity_c_item"/"entity_c_package", trackingRange = 1000)` — check
  whether this port's `com.hbm.interfaces.AutoRegister` annotation is already wired to an
  `EntityType<?>` `DeferredRegister` processor (Phase 0 scope) or whether entity registration needs
  manual `DeferredRegister<EntityType<?>>` entries here; this survey did not re-verify `AutoRegister`'s
  processor, only that the annotation exists in CE source on both classes.
- No renderer is required to make these functional server-side; a Phase 5 concern per PORT_SPEC.md,
  not a blocker for the entity classes themselves compiling and ticking.

### 8 conveyor blocks — confirmed zero block-entity coupling, ready once step 0 lands

All `extends Block` directly (`BlockContainer`/`TileEntity` reference count is exactly 0 across all 8
files, re-verified in this pass by full read, not just grep):

| Class | Extends | Notes |
|---|---|---|
| `BlockConveyorBase` (abstract) | `Block implements IConveyorBelt, IToolable` | Owns `FACING` blockstate property, the `getTravelLocation`/`getClosestSnappingPosition`/`getTravelDirection` math, `onEntityCollision` (converts a colliding vanilla item entity into `EntityMovingItem`), and the shared bounding box (`0-1, 0-0.25, 0-1`, non-full/non-opaque cube). |
| `BlockConveyorBendable` | `BlockConveyorBase` | Adds a `CURVE` enum property (`STRAIGHT`/`LEFT`/`RIGHT`, `PropertyEnum` -> `EnumProperty` in 1.21.1) with left/right output-direction and travel-direction math for curved belts. |
| `BlockConveyor` | `BlockConveyorBendable` | The plain visible conveyor; drops `ItemConveyorWand` (metadata 0) and cycles STRAIGHT->LEFT->RIGHT->lift-swap on sneak-screwdriver. |
| `BlockConveyorChute` | `BlockConveyorBase` | Vertical drop-chute; adds a 3-state `TYPE` int property (bottom/middle/input) recomputed on `neighborChanged` by scanning the block below/at the facing offset for `IConveyorBelt`/`IEnterableBlock`. |
| `BlockConveyorLift` | `BlockConveyorChute` | Vertical conveyor lift; reuses chute's `TYPE` property semantics but for stacking lift segments, adds a shorter top-segment AABB. |
| `BlockConveyorDouble`, `BlockConveyorExpress`, `BlockConveyorTriple` | `BlockConveyorBendable` | Cosmetic/throughput variants (double/triple-wide snapping math, 3x speed multiplier for express); each drops a distinct `ItemConveyorWand` damage value. **Metadata note**: these three plus the base `conveyor` are 4 distinct registry blocks that share one `ItemConveyorWand` item differentiated by damage value in CE (`getPickBlock`/`getItemDropped` return `ItemStack(ModItems.conveyor_wand, 1, N)` for N=0..3) — this is exactly the pre-1.13 item-damage-variant pattern the port's flattening ground rule targets on the **item** side; flag for whoever owns `ItemConveyorWand` in the items research area, not resolved here since this survey is block-scoped. |

All 8 implement `IConveyorBelt` (directly or via `BlockConveyorBase`) and `IToolable` (screwdriver
rotates/cycles curve or lift/chute type) — **both interfaces are already ported in this repo**
(`com.hbm.api.conveyor.IConveyorBelt`, `com.hbm.api.block.IToolable`, see "Key design/API decisions"
below for the exact signatures found). No new interface work is needed on that front; conveyor blocks
just need to implement the existing contract.

None of the 8 need a menu/screen, a capability provider, or HE energy. Blockstate properties
(`FACING` via `DirectionProperty`, `CURVE`/`TYPE` via `EnumProperty`/`IntegerProperty`) are a live
NeoForge 1.21.1 mechanism — no flattening concern on the block side (confirmed by
`docs/phase1/blocks_network_rail.md`'s same finding, re-confirmed here by direct read of all 8
files).

### `CraneSplitter` + `DummyBlockCraneSplitter` — portable once the Phase 2 multiblock framework lands (not a conveyor-tier item, despite implementing `IConveyorBelt`)

`CraneSplitter extends BlockDummyable implements IConveyorBelt, IEnterableBlock, IToolable, ...` —
structurally it is a conveyor-belt-protocol implementer (same `getTravelLocation`/
`getClosestSnappingPosition` shape as `BlockConveyorBase`, and it directly spawns
`EntityMovingItem`), but it is **not** in the "8 lightweight conveyor blocks" tier: it is a 2-wide
`BlockDummyable` multiblock (`getDimensions() = {0,0,0,0,0,1}`) whose core-vs-dummy meta split
(`meta >= 12` = core / `meta >= 6` = dummy-proxy / `meta < 6` = not-yet-formed) is driven by
`MultiblockHandlerXR.fillSpace` and a `TileEntityCraneSplitter` block entity (ratio-based
1-input/2-output item splitter, holds `leftRatio`/`rightRatio` bytes, no GUI — screwdriver
directly adjusts the ratio and there's a HUD overlay via `ILookOverlay.printGeneric`, not a
menu). `DummyBlockCraneSplitter extends BlockContainer implements IDummy` is its companion proxy
block (`TileEntityDummy`, renders invisible, forwards `breakBlock` to the real structure origin).

This confirms and narrows Phase 1's finding: `CraneSplitter` needs
`com.hbm.handler.MultiblockHandlerXR` (Phase 2 multiblock framework, not yet ported per
`docs/phase0/STATUS.md`'s gap list) plus one block entity (`TileEntityCraneSplitter`) and its dummy
companion's `TileEntityDummy`. It does **not** need a menu/screen. Recommend sequencing it right after
the multiblock framework lands, not bundled with the 8 plain conveyors (which need neither
multiblock nor block entity).

## Deferred scope (needs another package/prerequisite first)

### The 9 `BlockCraneBase`-derived crane/inserter blocks — need block entities; 6 of 9 also need a menu/screen framework this port does not have

All 9 confirmed by direct read (`extends`/`implements`/`createNewTileEntity` grep across every file
in `blocks/network/Crane*.java`):

| Class | Extends | Companion TE (CE) | Has own GUI in CE? |
|---|---|---|---|
| `CraneBoxer` | `BlockCraneBase implements IEnterableBlock` | `TileEntityCraneBoxer` | Yes — `ContainerCraneBoxer`/`GUICraneBoxer` |
| `CraneExtractor` | `BlockCraneBase` | `TileEntityCraneExtractor` | Yes — `ContainerCraneExtractor`/`GUICraneExtractor` |
| `CraneGrabber` | `BlockCraneBase` | `TileEntityCraneGrabber` | Yes — `ContainerCraneGrabber`/`GUICraneGrabber` |
| `CraneInserter` | `BlockCraneBase implements IEnterableBlock` | `TileEntityCraneInserter` | Yes — `ContainerCraneInserter`/`GUICraneInserter` |
| `CraneUnboxer` | `BlockCraneBase implements IEnterableBlock` | `TileEntityCraneUnboxer` | Yes — `ContainerCraneUnboxer`/`GUICraneUnboxer` |
| `CraneRouter` | `BlockContainer implements IEnterableBlock, ITooltipProvider` (its own base, not `BlockCraneBase`) | inline `TileEntityCraneRouter`-equivalent (declared `createNewTileEntity` directly) | Yes — `ContainerCraneRouter`/`GUICraneRouter` |
| `CranePartitioner` | `BlockContainer implements IConveyorBelt, IEnterableBlock, ITooltipProvider, IDynamicModels` (its own base) | **inner class** `TileEntityCranePartitioner extends TileEntityMachineBase implements ITickable` | Not confirmed in this pass — no `ContainerCranePartitioner`/`GUICranePartitioner` file found alongside the other 6; treat as open until re-checked at implement time. |
| `CraneGrabber`/`CraneExtractor` (re-listed above) | — | — | — |

(`CraneRouter` and `CranePartitioner` do not extend `BlockCraneBase` at all — they extend
`BlockContainer` directly with their own facing/property handling. `BlockCraneBase` itself
`extends BlockContainer implements IToolable, ITooltipProvider, IDynamicModels, IBlockSideRotation`
and is abstract — `createNewTileEntity` returns `TileEntityCraneBase`, and it owns an
`ExtendedBlockState`-based `OUTPUT_OVERRIDE` unlisted property plus a large set of
`@SideOnly(Side.CLIENT) TextureAtlasSprite` fields and a custom `CraneBakedModel`/`StateMapperBase` —
1.12-era manual model-baking machinery with no direct NeoForge 1.21.1 equivalent; this becomes a
datagen/blockstate-JSON + `BakedModel`/`RenderType` concern for whichever Phase 5 area owns custom
block models, not something to port line-for-line.)

**Confirmed blockers, both real gaps in this port today (re-verified in this pass, not assumed from
Phase 1):**

1. **No `BlockEntity` base class or convention exists yet.** `grep -r "extends BlockEntity"` across
   `src/main/java/com/hbm` returns zero hits, and there is no `com.hbm.tileentity` or
   `com.hbm.blockentity` package at all in this port yet. This is not this survey's area to resolve —
   `docs/phase0/STATUS.md`'s "Open decisions" section already flags exactly this
   (`IPersistentNBT` under CE's `com.hbm.tileentity` vs. Neo Edition's renamed `com.hbm.blockentity`)
   as needing "one explicit decision before Phase 2 block entities land." Every crane block in this
   package needs that decision made first. Neo Edition's reference (content/behavior not
   authoritative, but package/API shape is) uses `com.hbm.blockentity.**`, e.g.
   `com.hbm.blockentity.machine.GeigerBlockEntity extends BlockEntity`, constructed as
   `super(NtmBlockEntityTypes.GEIGER_COUNTER.get(), pos, blockState)` and registered via
   `DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID)`
   with `BlockEntityType.Builder.of(Ctor::new, blockSupplier.get()).build(null)` per entry — cited
   here purely as a confirmed NeoForge 1.21.1 API shape, not as a decision this report is making on
   the port's behalf.
2. **No `AbstractContainerMenu`+`Screen` framework exists yet in this port at all.** Confirmed by
   `grep -rl "AbstractContainerMenu\|MenuType"` across `src/main/java/com/hbm` returning zero hits.
   `docs/phase1/items_tool.md` already flagged this gap from the item side (bag/container items
   needing "a generic item-owned inventory/GUI pattern"); this report reconfirms it independently
   from the block side. 6 of the 9 crane/inserter blocks (`CraneBoxer`, `CraneExtractor`,
   `CraneGrabber`, `CraneInserter`, `CraneUnboxer`, `CraneRouter`) each have their own dedicated
   `Container*`/`GUI*` pair in CE (not a shared generic GUI — verified by listing
   `com.hbm.inventory.{container,gui}` for exact filenames), so this is 6 distinct menu/screen classes
   to write, not one reusable pattern. `CranePartitioner`'s GUI status is unconfirmed (see table
   above). This is a shared Phase 2 prerequisite affecting far more than this package — recommend
   whoever sequences Phase 2's work order stands up the menu/screen framework as its own early
   work item, citing Neo Edition's confirmed shape as reference only:
   `DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID)`,
   entries via a `reg(name, Ctor::new)` helper producing `DeferredHolder<MenuType<?>, MenuType<X>>`.
3. **`BlockCraneBase.onScrew` casts `world.getTileEntity(pos)` to `TileEntityCraneBase`** and calls
   `craneTileEntity.setOutputOverride(side)` / `.setInput(side)` — i.e. the block-entity API surface
   for input/output-side configuration is itself part of what needs porting alongside the block; this
   is not just a rendering/GUI concern, the core interaction (screwdriver sets input/output side) is
   block-entity-mediated.
4. No HE energy (`PowerNetMK2`/`IEnergyConductorMK2`/`IEnergyProviderMK2`) or fluid-tank
   (`com.hbm.inventory.fluid.*`) coupling was found in any crane/inserter block or its TE (re-checked
   by grep across `tileentity/network/TileEntityCrane*.java` for `energymk2`/`PowerNetMK2` — zero
   hits). This sub-area does not need those two already-ported Phase 0 systems at all, so nothing to
   flag there beyond confirming the negative.
5. No world-fluid-block or `com.hbm.inventory.RecipesCommon`/JSON-recipe dependency exists in this
   sub-area either — crane/inserter and conveyor blocks move `ItemStack`s directly via entities, they
   don't consume machine recipes. `IToolable.ToolType` (already ported) does reference
   `com.hbm.inventory.RecipesCommon.ComparableStack` in its `getType(ItemStack)` lookup helper, but
   that helper isn't called anywhere in the conveyor/crane block code read in this pass — it's a
   latent compile dependency the interface already carries from Phase 0, not something this area's
   blocks add.

## Key design/API decisions (confirmed NeoForge 1.21.1 shapes)

All of the following were found by reading real code in this port or in `upstream/neo-edition`, not
invented:

- **`IConveyorBelt`, `IEnterableBlock`, `IConveyorItem`, `IConveyorPackage` are already ported** at
  `com.hbm.api.conveyor.*` in this port, using modern types throughout: `Level` (not `World`),
  `net.minecraft.core.BlockPos`, `net.minecraft.core.Direction` (not `EnumFacing`), `net.minecraft.world.phys.Vec3`
  (not `Vec3d`), `net.minecraft.world.item.ItemStack`. Signatures match CE 1:1 modulo the type
  renames — e.g. `Vec3 getClosestSnappingPosition(Level world, BlockPos pos, Vec3 itemPos)`. Conveyor
  and crane blocks can implement this contract directly with no interface changes needed.
- **`IToolable` is already ported** at `com.hbm.api.block.IToolable`, using
  `net.minecraft.world.InteractionHand` (not `EnumHand`) and keeping the CE `(x, y, z, ...)`
  int-triple `onScrew` signature plus a convenience `default onScrew(Level, Player, BlockPos, ...)`
  overload not present in CE — a port-side addition, worth keeping for new block-entity-mediated
  `onScrew` implementations (crane blocks) since it avoids re-deriving `BlockPos` from three ints.
  Note its `ToolType.getType(ItemStack)` helper already references `com.hbm.inventory.RecipesCommon`
  (confirmed missing per `docs/phase1/STATUS.md`'s gap list) — a latent compile dependency, not
  something newly introduced by this area.
- **`BlockEntity` registration** (confirmed via Neo Edition, API-shape-only citation):
  `DeferredRegister<BlockEntityType<?>> = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID)`,
  each entry `register("name", () -> BlockEntityType.Builder.of(Ctor::new, blockSupplier.get()).build(null))`,
  and the `BlockEntity` subclass constructor takes `(BlockPos pos, BlockState blockState)` and calls
  `super(TYPE.get(), pos, blockState)`.
- **Menu registration** (confirmed via Neo Edition, API-shape-only citation):
  `DeferredRegister<MenuType<?>> = DeferredRegister.create(Registries.MENU, MODID)`, entries via a
  `reg(name, Ctor::new)` helper wrapping `IContainerFactory`/`IMenuTypeExtension`, producing
  `DeferredHolder<MenuType<?>, MenuType<X>>`. This port has **zero** existing menu/screen code to
  build on — the first crane block ported effectively also stands up this pattern for the rest of
  Phase 2 (and Phase 3+), so budget for that shared cost once, not per-block.
- **`EntityDataManager`/`DataParameter<ItemStack>` (CE 1.12) maps to `SynchedEntityData`/
  `EntityDataAccessor<ItemStack>`** in 1.21.1 (`EntityDataSerializers.ITEM_STACK` for the serializer).
  Not independently re-confirmed against this port's own entity code in this pass (this port has no
  ported entities yet to cross-check against) — flagging as the expected 1.21.1 shape based on
  well-known Mojang-mapping renames, not verified against a live example the way the BlockEntity/Menu
  shapes above were.
- **Blockstate properties**: CE's `PropertyDirection`/`PropertyEnum`/`PropertyInteger` map to
  `DirectionProperty`/`EnumProperty<T>`/`IntegerProperty` in 1.21.1 (`BlockStateProperties.FACING` or
  a custom `DirectionProperty.create(...)` for the horizontal-only `FACING` case, matching CE's
  `BlockHorizontal.FACING`). Confirmed as the standard modern mechanism, not independently
  cross-checked against a ported example in this pass since no blocks are ported yet in this port
  (Phase 1 content mass is still pending per `docs/phase0/STATUS.md`).

## Open questions / risks

- **Block-entity package-naming decision (`com.hbm.tileentity` vs `com.hbm.blockentity`) is a hard
  prerequisite for every crane block in this area**, and is explicitly called out as unresolved in
  `docs/phase0/STATUS.md`. This report does not resolve it — flagging again because this is the first
  Phase 2 sub-area whose implementation is actually blocked on it (the conveyor/`EntityMovingItem`
  slice is not blocked on it at all, which is exactly why it should go first).
- **Menu/screen framework does not exist in this port at all.** 6 of 9 crane blocks need it
  (`CraneBoxer`/`Extractor`/`Grabber`/`Inserter`/`Unboxer`/`Router`, each with its own distinct
  `Container`+`GUI` pair in CE — not a shared generic one). `CranePartitioner`'s GUI status is
  unconfirmed (no `Container`/`GUI` file found for it in this pass, but its TE implements
  `ITickable` and extends `TileEntityMachineBase`, both suggesting active machine behavior that may
  still need a GUI for configuration — recommend a direct check of `CranePartitioner`'s TE and any
  GUI-related interface it implements before scheduling it). Recommend the menu/screen framework be
  stood up as an explicit early Phase 2 work item (as Phase 1's `items_tool.md` also anticipated from
  the item-container side) rather than being organically discovered mid-crane-block-porting.
- **`CraneRouter` and `CranePartitioner` do not extend `BlockCraneBase`** — they're independent
  `BlockContainer` subclasses with their own facing-property and rendering setup. Don't assume all 9
  "crane-family" blocks share one base-class port; `BlockCraneBase`'s 7 subclasses
  (`Boxer`/`Extractor`/`Grabber`/`Inserter`/`Unboxer`, plus 2 more not directly named in this report's
  file list check — re-verify the full `BlockCraneBase` subclass count at implement time since this
  survey read `CraneBoxer/Extractor/Grabber/Inserter/Partitioner/Router/Unboxer` — 7 files — of which
  5 extend `BlockCraneBase` and 2 (`Router`, `Partitioner`) do not) need one porting approach; the
  other 2 need their own.
- **`BlockCraneBase`'s manual model-baking machinery** (`CraneBakedModel`, `StateMapperBase`,
  `ExtendedBlockState`/`IUnlistedProperty<Direction> OUTPUT_OVERRIDE` for a runtime-computed output
  arrow render) has no direct 1.21.1 equivalent and needs a real rendering-side design decision
  (custom `BakedModel` + `RenderType`, or a simpler blockstate-driven approach that drops the
  unlisted-property indirection) — flagging as a Phase 5 client-side risk that this block-scoped
  report cannot resolve, but that whoever implements these blocks' block-state/model JSON via
  datagen should be aware doesn't have a template to copy from CE's baked-model Java class directly.
- **`ItemConveyorWand`'s 4-damage-value metadata split** (feeding `BlockConveyor`/`Double`/
  `Express`/`Triple`'s `getPickBlock`/`getItemDropped`) needs 4 distinct registry items post-1.13
  flattening — this is an item-side gap for whichever Phase 1/2 area owns `ItemConveyorWand`
  (referenced in `docs/phase1/items_tool.md`'s Phase-2-machine-coupling table), not resolved here.
- **`AutoRegister` annotation processing for entities** (`@AutoRegister(name=..., trackingRange=...)`
  on both `EntityMovingItem` and `EntityMovingPackage`) was not re-verified against this port's
  current `com.hbm.interfaces.AutoRegister` implementation/processor in this pass — confirm whether
  it already drives `EntityType<?>` `DeferredRegister` entries automatically, or whether manual
  registration is needed for these two (three, counting `EntityMovingConveyorObject`'s abstract
  non-registered base) classes.
- **`ExplosionVNT`/`ServerConfig.CONVEYOR_CRAM_MAX`/`CONVEYOR_CRAM_EXPLODE`** (the conveyor
  "cram" anti-lag/explosion check in `EntityMovingConveyorObject.onUpdate`) depend on
  `com.hbm.explosion.vanillant.*` and this port's config system — both are believed present per
  Phase 0/1 scope but were not independently re-verified as ported in this pass; a quick existence
  check before implementing `EntityMovingConveyorObject` is cheap insurance.
- **`CranePartitioner`'s inner-class TE pattern** (`TileEntityCranePartitioner` declared as a
  `public static class` *inside* `CranePartitioner.java` rather than its own file under
  `tileentity/network/`) is structurally different from every other crane block's TE (all separate
  top-level classes) — worth deciding at implement time whether the port keeps that inline nesting
  or normalizes it to a standalone `BlockEntity` class file alongside the others.
