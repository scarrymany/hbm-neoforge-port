# Triage: `blocks/network` (76 files) and `blocks/rail` (13 files)

Research area key: `blocks_network_rail`. Read-only survey, no port files written.

## Verdict up front

Both phase assignments guessed in the task prompt are **confirmed, with one correction**:

- `blocks/network` -> **Phase 2** ("Machines" / logistics), exactly as PORT_SPEC.md section 2 already
  states verbatim: *"logistics (cables + energy net, fluid ducts, item conveyors/crane inserters)"*.
  This is not a guess to validate, it's already the explicit spec text.
- `blocks/rail` -> **not actually placed anywhere in PORT_SPEC.md's phase list.** It is neither
  Phase 1 nor cleanly Phase 4. See "Rail: the real complication" below - this needs an explicit
  decision, not just a rubber stamp of "Phase 4".
- **No Phase-1-safe block hides in either package.** Every single file in both packages requires
  either a companion `TileEntity`/block-entity, the Phase 2 multiblock framework (`BlockDummyable`
  + `MultiblockHandlerXR`), or a custom `Entity` subclass that doesn't exist in the port yet. See
  the one near-miss (conveyors) discussed below, and why it still doesn't qualify.

## `blocks/network` (76 files) - confirmed Phase 2

### Structural evidence

Grepped every file's superclass/interfaces and `TileEntity` reference count. Breakdown:

| Coupling pattern | Count (approx) | Examples |
|---|---|---|
| `extends BlockContainer` / `BlockContainerBakeable` / `BlockBakeBase` (needs a `TileEntity`) | ~55 | `PneumoTube`, `FluidDuctBase` and its 8 subclasses, `energy/BlockCable`, `energy/CableDiode`, `DroneCrate`, `DroneDock`, `RadioTorchBase` hierarchy |
| `extends BlockDummyable` (Phase 2 multiblock framework: `MultiblockHandlerXR`) | 8 | `MachineBatteryREDD`, `MachineBatterySocket`, `PylonMedium`, `RadioAUTOCAL`, `RadioTelex`, `energy/PylonLarge`, `energy/Substation`, `CraneSplitter` |
| `extends Block` directly, no TE, but coupled to a custom `Entity` (`EntityMovingItem`) | 8 | `BlockConveyor`, `BlockConveyorBase/Bendable/Chute/Double/Express/Lift/Triple` |
| Not a block at all (interface / render-property helper) | 2 | `IBlockFluidDuct` (marker interface consumed by `FluidDuctBase`), `SimpleUnlistedProperty<T>` (implements Forge's `IUnlistedProperty<T>` - a Forge-1.12 blockstate mechanism with no NeoForge 1.21 equivalent; this is a rendering utility class, not a registrable block, and should not be counted toward the 76-block figure at all) |
| `extends BlockContainer implements ITileEntityProvider` explicitly | 1 | `energy/PowerCableBox` |

Companion evidence: `com.hbm.tileentity.network.**` has **54 TileEntity classes** already sitting in
CE, one per (or shared across a couple of) `BlockContainer`/`BlockDummyable` block in this package -
none of those TEs exist in the port yet (Phase 0's STATUS.md gap list already calls out
`com.hbm.tileentity.*` broadly, and `MultiblockHandlerXR`/`MultiblockBBHandler` specifically, as
Phase 2 work).

Five files (`FluidPump`, `RadioTorchController`, `RadioTorchCounter`, `RadioTorchLogic`,
`RadioTorchReader`, plus `energy/CableDiode`) additionally implement `IGUIProvider`, meaning they
need a Phase 2 menu/screen, not just a block-entity.

Several files (`FluidDuctBase`, `energy/BlockCableGauge`, `energy/CableDiode`) directly reference
`uninos`/`PowerNetMK2`/`IEnergyConnectorBlock` - the HE energy network graph classes Phase 0 already
delivered (`com.hbm.uninos.*`), confirming these blocks are literal consumers of a Phase 0 API that
was built specifically for this package. That is strong independent confirmation this package is
"the next layer up" from Phase 0, i.e. Phase 2, not Phase 1.

### The one near-miss: conveyors

`BlockConveyorBase` (and its 7 concrete subclasses `BlockConveyor`, `BlockConveyorBendable`,
`BlockConveyorChute`, `BlockConveyorDouble`, `BlockConveyorExpress`, `BlockConveyorLift`,
`BlockConveyorTriple`) are the structurally simplest blocks in the package: `extends Block` directly
(no `BlockContainer`, no `TileEntity`, `grep -c TileEntity` = 0), just a directional block with
`onEntityCollision` logic and a `FACING`/`TYPE` blockstate property. On pure block-class shape they
would look Phase-1-safe.

They are not, for one real reason: `onEntityCollision` converts a vanilla `EntityItem` into a custom
`com.hbm.entity.item.EntityMovingItem` and spawns it into the world. That entity class does not
exist in the port yet, is not part of Phase 0's delivered scope, and entity registration/rendering
is explicitly a later concern (custom entities are called out under PORT_SPEC.md's Phase 4 bucket,
and any entity needs at minimum an `EntityType<T>` registration plus - eventually - a renderer,
which is Phase 5 territory). Pulling `EntityMovingItem` forward into Phase 1 just to unblock 8
conveyor blocks would violate Phase 1's stated scope ("items and simple blocks" - no entity
registration). Recommendation: leave conveyors in Phase 2 exactly as PORT_SPEC.md already says, but
note for the Phase 2 work-package split that conveyors are the lightest-weight item in the package
(no multiblock, no menu, no capability network) and a good first slice within that phase's wave -
right after `EntityMovingItem`/`EntityMovingConveyorObject` are ported alongside it.

The two related classes `IConveyorBelt` and `IToolable` (the interfaces conveyors implement) are
**already ported** in Phase 0 (`com.hbm.api.conveyor.IConveyorBelt`, `com.hbm.api.block.IToolable`
both exist under the port's `src/main/java/com/hbm/api`), so that part of the dependency graph is
already closed.

### Note on the item-metadata-flattening ground rule

The port's "flattening" concern (CE items using `ItemStack` metadata to represent N material
variants, which must become N distinct registry entries post-1.13) **does not apply to this
package**. Everything here is a `Block`, and block-state properties (`PropertyDirection FACING`,
`BlockConveyorChute.TYPE` etc.) are a live, supported mechanism in modern Minecraft/NeoForge
blockstates (`DirectionProperty`, `IntegerProperty`, ...) - they are not the pre-1.13 item-damage-value
mechanism the ground rules are warning about. Checked `ModBlocks.java` in CE directly: network
package classes are instantiated close to 1:1 with their file (e.g. `red_cable`, `red_wire_coated`,
`red_pylon_large`, `substation`, `conveyor`, `crane_extractor`, `crane_boxer`, `fluid_duct_neo`,
`pneumatic_tube` - one registry entry per constructor call), i.e. there is no hidden per-material
block explosion analogous to the item-side `ItemAutogen` pattern to plan around here.

### NBT -> Data Components note

Did not find `ItemStack` NBT usage of note in this package (it's almost entirely `Block`/`TileEntity`
logic, not item logic) - any NBT-bearing items tied to these blocks (paint cans for the `Paintable`
variants, wrenches/screwdrivers for `IToolable`) belong to the items-package research areas, not
here. Flagging for cross-reference: `FluidDuctPaintable`, `FluidDuctPaintableBlockExhaust`,
`energy/BlockCablePaintable`, `PneumoTubePaintableBlock`, and `BlockOpenComputersCablePaintable` all
implement `IFacade`/paint mechanics that store color state - confirm with whichever area covers
`com.hbm.items.util` paint tools whether that's TE NBT (stays a TE data problem, not a component
problem) or ItemStack NBT (would need componentization).

## `blocks/rail` (13 files) - phase assignment is a genuine open question

### Structural evidence

All 13 files were read (small package, full depth):

| File | Superclass | Notes |
|---|---|---|
| `IRailNTM.java` | interface | rail-traversal contract (`Vec3d`/`BlockPos` based), consumed by `EntityRailCarBase` for movement, not just by blocks |
| `BlockRailWaypointSystem.java` | `BlockDummyable implements IRailNTM` | abstract base; imports `com.hbm.entity.train.EntityRailCarBase` directly |
| `RailNarrowCurve/Straight`, `RailStandardCurveBase/Wide7/Wide9/StraightShort/Straight/Buffer/Ramp` | `BlockDummyable implements IRailNTM` (or extend `RailStandardCurveBase`) | pure track geometry, still multiblock-dummy-based |
| `RailStandardSwitch`, `RailStandardSwitchFlipped` | `extends BlockRailWaypointSystem` | additionally reference `com.hbm.tileentity.rail.TileEntityRailSwitch` and `ModItems` (a switch-lever item) |

Every single rail block extends `BlockDummyable` - CE's multiblock-structure base class. None are
plain single-block placements. `BlockDummyable` itself is a Phase 0 class (already in the port), but
its *handler* (`MultiblockHandlerXR`) is explicitly listed in Phase 0's STATUS.md as a deferred
Phase 2 gap. So structurally, every rail block is blocked on the Phase 2 multiblock framework
regardless of anything else.

On top of that, rail blocks only make sense functionally alongside a **train entity subsystem** that
does not exist in the port and is not mentioned in Phase 0's scope at all:

```
com.hbm.entity.train/
  EntityRailCarBase.java
  EntityRailCarCargo.java
  EntityRailCarElectric.java
  EntityRailCarRidable.java
  TrainCargoTram.java
  TrainCargoTramTrailer.java
```

`BlockRailWaypointSystem` imports `EntityRailCarBase` directly (rail geometry queries feed the
train's pathing), and `RailStandardSwitch`/`RailStandardSwitchFlipped` need
`com.hbm.tileentity.rail.TileEntityRailSwitch` (a real block-entity, for switch state + redstone
control), which itself is presumably driven by the train entities passing over it.

### Why "Phase 4" is not a clean fit either

PORT_SPEC.md's Phase 4 bucket ("World & simulation systems") mentions custom entities only in
passing - *"custom entities (creepers variants, bosses, projectiles, vehicles if present in CE
2.5.0.5)"* - grouped with world-gen structures, radiation, and pollution. Trains are a real
"vehicles" hit for that parenthetical, so Phase 4 is defensible on the entity side. But the rail
*blocks* themselves are structural/multiblock content, which is a Phase 2 concern (multiblock
framework), not a Phase 4 one. Rail is a vertical feature that straddles:

- Phase 2 dependency: `BlockDummyable` multiblock framework must exist first (blocks can't function,
  can't even be meaningfully datagenned for their multi-part shapes, without it).
- Phase 4-shaped dependency: the entity/train subsystem (movement, rendering hooks it will need in
  Phase 5, redstone-driven switch TE).

**Recommendation:** do not silently fold rail into either Phase 1 or a generic "Phase 4 leftover"
bucket. Treat `blocks/rail` + `entity/train` + `tileentity/rail` as one dedicated work package that
starts no earlier than the Phase 2 multiblock framework lands, and flag it explicitly in the Phase
2-vs-4 planning pass so it isn't dropped between two phase boundaries. This is exactly the kind of
gap PORT_SPEC.md's own phase list doesn't resolve for you - it needs an explicit call, not an
inferred one.

### Phase-1-safe candidates in `blocks/rail`

**None.** Every file needs `BlockDummyable`'s full multiblock machinery at minimum, and most need
the train entity subsystem too. There is no purely-decorative rail-adjacent block in this package -
even the plain track-geometry pieces (`RailNarrowStraight`, `RailStandardStraight`, etc.) are
multiblock-dummy blocks, not simple single-block placements, because CE renders multi-block-long
rail segments as one logical dummyable structure.

## Summary for the Phase 1 work-package breakdown

- Do not schedule any file from `blocks/network` or `blocks/rail` into Phase 1. Zero exceptions.
- `blocks/network` (74 real blocks + 1 interface + 1 render-utility class, not 76 registrable
  blocks) stays exactly where PORT_SPEC.md already puts it: Phase 2 logistics. Its own internal
  ordering hint for the Phase 2 wave: conveyors (8 files, needs `EntityMovingItem` ported alongside)
  are the lightest dependency; cranes/pneumo-tubes/fluid-ducts/energy-cables (the `BlockContainer`
  majority, ~55 files) need their 54 companion TileEntities from `com.hbm.tileentity.network`; the 8
  `BlockDummyable` files (pylons, substation, radio towers, battery blocks) need the multiblock
  framework; 5 files additionally need a GUI/menu.
- `blocks/rail` (13 files) needs an explicit phase-ownership decision that PORT_SPEC.md does not
  currently make - recommend a dedicated package spanning `blocks/rail` + `entity/train` +
  `tileentity/rail`, gated on the Phase 2 multiblock framework, most naturally scheduled alongside
  or just after Phase 4's custom-entity/vehicle work rather than assumed to be pure Phase 4
  leftovers.
