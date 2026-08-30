# Missile launch infrastructure: pads, silos, designators, satellites — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/bomb/{LaunchPad,LaunchPadLarge,LaunchPadRusted}.java`
  (98/110/92 lines) and `upstream/hbm-ce/src/main/java/com/hbm/tileentity/bomb/
  {TileEntityLaunchPadBase,TileEntityLaunchPad,TileEntityLaunchPadLarge,TileEntityLaunchPadRusted}.java`
  (568/162/322/235 lines) — the three player-built launch pad multiblocks and their shared base.
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/{BlockSiloHatch,DummyBlockSiloHatch,
  LaunchpadSoyuz}.java` (237/209/130 lines) and `upstream/hbm-ce/src/main/java/com/hbm/tileentity/
  machine/{TileEntitySiloHatch,TileEntityLaunchpadSoyuz}.java` (256/435 lines) — the blast-door silo
  hatch and the Soyuz crewed-launch multiblock.
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/{ItemDesignator,ItemDesignatorManual,
  ItemDesignatorRange,ItemDesignatorArtyRange,ItemSatDesignator,ItemSatInterface}.java` (91/86/97/89/
  52/83 lines), plus `com.hbm.api.item.IDesignatorItem` (CE and this port's own already-committed
  version), `com.hbm.items.machine.ItemSatChip` (the shared base of the two satellite items).
- `upstream/hbm-ce/src/main/java/com/hbm/saveddata/satellites/**/*.java` — all 15 files (`Satellite`,
  `SatelliteSavedData`, and the 13 concrete satellite behaviors: `Detector`, `Horizons`, `Laser`,
  `LunarMiner`, `Mapper`, `Miner`, `PrecisionLaser`, `Radar`, `RayScan`, `Relay`, `Resonator`,
  `Scanner`, `Science` — read in full, `Miner`/`Laser` detailed below as representative shapes, the
  rest surveyed for their `Interfaces`/command-dispatch shape only since their payload behavior
  (asteroid mining loot tables, radar entity lists, xenium resonator effects) is downstream content
  outside this report's launch/targeting-protocol scope).
- `upstream/hbm-ce/src/main/java/com/hbm/entity/missile/EntityMissileBaseNT.java` (420 lines, full) —
  the actual in-flight targeting/ballistics logic every launched missile runs, read to answer "how
  does a launched missile find its target" directly rather than inferring it from the launch pad side.
  `EntityMissileTier0/4.java`, `EntityMissileAntiBallistic.java`, `EntitySoyuz.java` (171/156/301/208
  lines) surveyed by signature only — concrete subclass line counts, not read function-by-function,
  since their differences from the base class are cosmetic (explosion payload, debris list) and
  already tracked by the sibling `explosion_engine.md`/`bomb_blocks_and_detonators.md` reports.
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemMissileStandard.java` (104 lines, full) —
  the missile item class every launch pad's slot-0 validity check and fuel/tier logic reads off of.
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/container/{ContainerLaunchPadLarge,
  ContainerLaunchPadRusted,ContainerLaunchpadSoyuz}.java` (109/83/47 lines, signature + slot-layout
  level, not the full GUI render code) and `upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/
  GUIScreenDesignator.java` (213 lines, full — the one client-only, containerless designator GUI,
  detailed below since its shape is unusual and load-bearing for a design decision).
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/component/SiloComponent.java` (header/class
  declaration + package imports read, not the full 1,408-line block-placement body) — confirmed this
  is a 1.12 legacy world-gen `StructureComponent` (`net.minecraft.world.gen.structure.template.
  TemplateManager`-based), i.e. the thing that places a `launch_pad_rusted` + `silo_hatch_drillgon`
  pair into freshly generated terrain; out of this report's scope, flagged under Deferred scope.
- `docs/phase1/items_tool.md` bucket (b) (the section that named these six designator/sat files and
  `ItemRadarLinker` as "military C2 equipment" Phase 3 scope — read first, per this task's own
  instruction), `docs/phase1/items_special.md`'s Deferred section (grep-checked for
  designator/silo/satellite/missile mentions — found none beyond `ItemLootCrate`'s `ItemMissile`
  reference and `ItemStarterKit`'s giveaway-list mention, both about missile *items*, not this
  report's launch-infrastructure scope) and `docs/phase1/items_food_gear.md`'s Deferred section
  (same grep, same result — no launch-pad/designator/satellite content named there).
  `docs/phase2/rbmk_reactor.md` (structural model this report follows, and the source of the
  "`com.hbm.entity` package is entirely absent" finding this report re-confirms and narrows).
  `docs/phase3/bomb_blocks_and_detonators.md` (already read `LaunchPad`/`LaunchPadLarge`/
  `LaunchPadRusted` as `IBomb` implementors and explicitly deferred their designator/missile payload
  to "a separate 'military C2 equipment' work package" — that is this report; its `ModContext.
  DETONATOR_CONTEXT` and `IBomb`/damage-type/data-component/entity-registration findings are treated
  as authoritative and not re-derived) and `docs/phase3/explosion_engine.md` (already scoped
  `ExplosionLarge.spawnMissileDebris`/`EntityNukeExplosionMK5` as the missile-warhead detonation path,
  also not re-derived).
- This port's own already-committed code (confirmed real, not inferred): `com.hbm.api.item.
  IDesignatorItem` (already ported, NeoForge-shaped), `com.hbm.interfaces.IBomb` (already ported,
  3-arg `explode`, `LAUNCHED` return code already present in the enum), `com.hbm.blocks.BlockDummyable`
  + `com.hbm.handler.MultiblockHandlerXR` (25 existing `extends BlockDummyable` subclasses),
  `com.hbm.blockentity.{LoadedBaseBlockEntity,MachineBaseBlockEntity}` (47 existing subclasses),
  `com.hbm.api.energymk2.*` / `com.hbm.api.fluidmk2.*` (both fully populated, including
  `IEnergyReceiverMK2`, `IFluidStandardReceiverMK2` — confirming CE's launch-pad power/fluid interfaces
  have real ported equivalents), `com.hbm.inventory.fluid.tank.FluidTankNTM` (confirmed to now exist —
  it did not yet at RBMK-report time), `com.hbm.packet.HbmNetwork` (one payload registered so far,
  `BufPacket`), `com.hbm.inventory.container.MenuBase` / `com.hbm.inventory.gui.GuiInfoContainer`,
  `com.hbm.damage.ModDamageTypes` (`NUCLEAR_BLAST` already present), `com.hbm.saveddata.TomSaveData`
  (the one existing `SavedData` precedent — read in full as the API-shape model for
  `SatelliteSavedData`'s port), `com.hbm.items.tool.ToolDataComponents` (the one existing
  `DataComponentType` registration precedent), `com.hbm.entity.ConveyorEntityTypes` (the one existing
  `EntityType` registration precedent), `com.hbm.interfaces.IDummy` (ported, empty marker), and
  `com.hbm.blocks/blockentity` package trees (grepped for any existing missile/designator/satellite
  content — found none; this is greenfield Phase 3 scope, confirmed empty).

## Headline finding

"Launch pads, silos, and designators" turns out to be four almost entirely independent systems that
happen to hand off to each other through two narrow, already-ported interfaces
(`IDesignatorItem`, `IBomb`) — not one multiblock with some items bolted on. Understanding the
handoff points is the actual research payoff here:

1. **A launch pad's own logic never computes a target.** `TileEntityLaunchPadBase.launchFromDesignator()`
   only knows how to (a) check `IDesignatorItem.isReady(...)` on whatever is in inventory slot 1, and
   (b) read `IDesignatorItem.getCoords(...)` off it to get an `(x, z)` pair. Every actual "how do you
   pick a target" decision — walk-and-click, ray-trace-and-click, GUI text entry, satellite panel
   click — lives entirely inside the *designator item* classes, which are otherwise unrelated to each
   other beyond implementing the same 2-method interface. This is why the interface is trivial (2
   methods, both already ported) and the six designator/sat classes are all under 100 lines each: the
   protocol is deliberately minimal.
2. **Three unrelated launch mechanisms share the name "launch pad."** `TileEntityLaunchPad` (small,
   single missile, instant-ish launch via `IEnergyReceiverMK2`+`IFluidStandardReceiver` fuel) and
   `TileEntityLaunchPadLarge` (adds a multi-stage erector/lift animation state machine before it will
   accept `canLaunch()`) both go through the shared `TileEntityLaunchPadBase.launchFromDesignator()`/
   `launchToCoordinate()`/`launchToEntity()` trio and the shared `missiles` factory map. **`TileEntityLaunchPadRusted`
   does not extend `TileEntityLaunchPadBase` at all** — it is a standalone `TileEntityMachineBase`
   with its own hand-rolled `launch()` method, a completely different unlock condition (`launch_code`
   + `launch_key` items physically present, not power/fuel), and it hardcodes a single missile type
   (`EntityMissileTier4.EntityMissileDoomsdayRusted`) rather than consulting the `missiles` map. Anyone
   implementing "the launch pad" as one class hierarchy will misparent this one.
3. **The silo hatch is not a launch pad at all — it's a blast door.** `BlockSiloHatch`/
   `TileEntitySiloHatch` is a large sliding/opening `IDoor`-family door (T-flip-flop redstone,
   lock/key security via `TileEntityLockableBase`, a 100-tick open/close animation that places and
   removes a ring of `TileEntityDummy` blocks around a point 3 blocks in front of itself) with **no
   missile-targeting logic whatsoever**. It exists purely as world-gen dressing for
   `launch_pad_rusted` (see `SiloComponent`, Deferred scope) and as a themed blast door players can
   build standalone. Confusingly, only one of the three registered "silo hatch" blocks
   (`silo_hatch_drillgon`) actually uses the `BlockSiloHatch`/`TileEntitySiloHatch` classes surveyed
   here — the other two (`silo_hatch`, `silo_hatch_large`) are registered as `BlockDoorGeneric`
   instances (a different, already-`docs/phase1/blocks_generic.md`-scoped generic door system) that
   merely share the texture/name theme. Do not assume all three "silo hatch" registry names route
   through this report's `TileEntitySiloHatch` class.
4. **The Soyuz complex is a fourth, unrelated launch mechanism again**, and its own multiblock
   (`LaunchpadSoyuz`) is one `BlockDummyable` core driving a 20-array-slot inventory, an 8-float
   crane/strut/carriage animation state machine (`updateStates()`'s 5-state `SoyuzStatus` enum), and
   — per its own `Deferred scope` note below — an **incomplete `LAUNCHING` state** (CE's own code:
   `// TBI: countdown, retracting the struts, launch`, i.e. the Soyuz complex has never actually fired
   a rocket in CE itself). It still validates designator/sat-chip/lander slots via `IDesignatorItem`/
   `ISatChip`, so it belongs in this report's scope, but "port the launch" for this one specifically
   means "port up to the point CE itself stops," not further.
5. **A launched missile's targeting is pure ballistics toward a fixed point chosen at spawn time, not
   guidance.** `EntityMissileBaseNT`'s constructor computes a unit vector from `(startX,startZ)` to
   `(targetX,targetZ)` once, and `onUpdate()` re-applies that same fixed-direction acceleration every
   tick (`accelXZ`/`decelY`, a parabolic lob, not a homing correction toward a moving point) — the
   missile does not re-read the designator, the target coordinate, or anything satellite-related after
   launch. **`EntityMissileAntiBallistic` is the sole exception**: it stores a live `Entity tracking`
   reference (set in `TileEntityLaunchPadBase.launchToEntity()`) and is presumably the only missile
   type with real intercept guidance (not read function-by-function here — out of scope, it belongs to
   the same `com.hbm.entity.missile` prerequisite package as every other concrete missile). So "how a
   launched missile finds its target" has two different correct answers depending on missile type, and
   the interesting logic (the ballistic trajectory shared by ~26 of ~27 missile types) lives entirely
   in the abstract base class, not per-missile.
6. **Satellites are a third, independent addressing scheme layered on top of designators, not a
   replacement for them.** A satellite is identified by an arbitrary integer "frequency" chosen by the
   player when it's launched (`Satellite.orbit(world, id, freq, x, y, z)`, called from wherever a
   `satellite`-type payload reaches orbit — not itself in this report's file set, likely the missile
   warhead/payload-delivery logic), stored in one `SatelliteSavedData` (a per-world `Int2ObjectOpenHashMap<Satellite>`
   keyed by that frequency) rather than per-chunk or per-block state. `ItemSatDesignator`/
   `ItemSatInterface` (both extend `ItemSatChip`, whose only real state is a `freq` int) look up a
   satellite by matching frequency and delegate through one of two dispatch shapes on the abstract
   `Satellite` class: `Interfaces.SAT_COORD` (`onCoordAction`, e.g. teleport-style remotes) or
   `Interfaces.SAT_PANEL` (`onClick`, e.g. the laser's `deathBlast`). A satellite's actual payload
   behavior (asteroid mining cargo rolls, radar entity scanning, the laser's `EntityDeathBlast`) is
   downstream content this report does not need to port to establish the addressing/dispatch protocol
   itself — the protocol is 13 subclasses picking one of two `Interfaces` values plus an
   `onCommand(String...)` text-command dispatch (`RTTYSystem`-adjacent, shared with redstone-over-radio,
   itself out of scope) used by non-GUI callers.

## Phase-3-safe scope

All class/line counts below are from the CE files actually read.

### Designator items (6 classes, ~500 lines total) — fully portable now, zero blockers

| Class | Lines | What it actually does | Real dependency |
|---|---|---|---|
| `ItemDesignator` | 91 | Right-click a block (not a `LaunchPad`) → stores that block's `(x, z)` as `xCoord`/`zCoord` on the stack. No range check, no line-of-sight requirement beyond vanilla's own reach. | None beyond `IDesignatorItem`. |
| `ItemDesignatorRange` | 97 | Same storage shape, but the target comes from a 300-block ray-trace (`Library.rayTrace(player, 300, 1)`) rather than a direct block click — a "laser designator," usable without touching the target block. | `Library.rayTrace` (vanilla ray-trace helper; confirm this port's `com.hbm.lib.Library` equivalent, or use `Player`'s own pick helpers directly). |
| `ItemDesignatorManual` | 86 | Stores the same `xCoord`/`zCoord`, but they're set via a **client-only, containerless GUI** (`GUIScreenDesignator`, detailed below) with text-entry fields, not a world click at all. | The GUI framework decision below — this is the one designator that needs a `Screen`, and specifically needs one with **no paired `Menu`/`AbstractContainerMenu`**. |
| `ItemDesignatorArtyRange` | 89 | **Not an `IDesignatorItem` at all** (it's a plain `ItemBase`) and has nothing to do with launch pads — it links to `TileEntityTurretBaseArtillery` (an artillery turret, `com.hbm.tileentity.turret`, entirely unported — confirmed absent from this port) via `onItemUse` and calls `arty.enqueueTarget(...)` via `onItemRightClick`. Included in this survey only because the task named it explicitly; it is **turret-targeting content, not launch-pad content**, and has zero real dependency on anything else in this report. Recommend porting it alongside whichever package eventually owns `com.hbm.tileentity.turret`, not alongside the launch pad work here. | `TileEntityTurretBaseArtillery` (turret package, unported, out of this report's scope). |
| `ItemSatDesignator` | 52 | `extends ItemSatChip`. Right-click ray-traces 300 blocks, looks up the satellite at this item's stored frequency via `SatelliteSavedData.getSatFromFreq`, and dispatches to `onCoordAction` or `onClick` depending on that satellite's `Interfaces` value. | `SatelliteSavedData`/`Satellite` (this report's own scope, see below). |
| `ItemSatInterface` | 83 | `extends ItemSatChip`, `implements IGUIProvider`. Right-click opens a **second containerless GUI** (`GUIScreenSatInterface`/`GUIScreenSatCoord`, chosen by `this == ModItems.sat_interface`), and its `onUpdate` hook pushes a `SatPanelPacket` to the holding player every 2 ticks while the item is the active hotbar item — i.e. **the satellite panel is a live-streamed one-way sync**, not a request/response GUI open. | The same client-screen decision as `ItemDesignatorManual`, plus a real `CustomPacketPayload` (this one genuinely needs one — see Key design/API decisions). |

`ItemSatChip` (the shared base, `ItemBakedBase implements ISatChip`) itself is trivial — one `freq`
field, exposed via `ISatChip.getFreq(stack)` (not read in this survey; a 1-line accessor per its use
sites) and a tooltip. `IDesignatorItem` (2 methods: `isReady`, `getCoords`) is **already ported** in
this port's `com.hbm.api.item.IDesignatorItem`, already in the correct NeoForge shape
(`Level`/`ItemStack`/`Vec3` — no `int y` even needed since none of these six items use it beyond
padding). No new interface work needed here at all.

### Launch pad multiblocks (3 classes + shared base, ~1,287 lines total)

| Class | Lines | Portability |
|---|---|---|
| `LaunchPad` (block) | 98 | `BlockDummyable` subclass, `getDimensions() = {0,0,1,1,1,1}` (single dummy ring, `getOffset()=1`), `implements IBomb` (routes detonator-triggered launches through `explode()` → `TileEntityLaunchPad.launchFromDesignator()`). Trivially portable against this port's already-real `BlockDummyable`/`MultiblockHandlerXR`. |
| `TileEntityLaunchPadBase` (abstract) | 568 | **The actual centerpiece.** Owns the `missiles` static factory map (`Object2ReferenceOpenHashMap<ComparableStack, MissileFactory>`, ~26 entries mapping a missile `ItemStack` to an `EntityMissileBaseNT`-producing lambda — registered once via `registerLaunchables()`), the two `FluidTankNTM` fuel tanks + `IEnergyReceiverMK2` power buffer, redstone-edge-triggered auto-launch (`redstonePower > 0 && prevRedstonePower <= 0`), and the three launch entry points detailed in the Headline finding (`launchFromDesignator`/`launchToCoordinate`/`launchToEntity`, all funneling into `instantiateMissile`+`finalizeLaunch`). Also implements `IRadarCommandReceiver` (2-method interface, itself unported — see Deferred scope) and the OpenComputers `SimpleComponent` bridge (5 `@Callback` methods — CE-1.12-only integration, not a NeoForge concern, drop or stub per this port's own OpenComputers-equivalent policy, not decided in this survey). |
| `TileEntityLaunchPad` | 162 | Small pad: `isReadyForLaunch() = delay <= 0` (a 100-tick post-launch cooldown), `getLaunchOffset() = 1D`. Client-side smoke-particle spawn on missile-entity-detected-above. Fully portable once the base class and `HbmEffectNT`/particle system exist (particle system status not re-derived here — Phase 2/4 concern per prior docs). |
| `TileEntityLaunchPadLarge` | 322 | Adds the erector/lift animation state machine (`erected`/`readyToLoad`/`scheduleErect`, `lift ∈ [0,1]`, `erector ∈ [0°,90°]`, form-factor-dependent speed halving for `ATLAS`/`HUGE` missiles) gating `isReadyForLaunch()`. This is pure per-tick float interpolation, no external dependency beyond the base class and (client-side only) `AudioWrapper` looped-sound handles for the lift/erector motors. |

`TileEntityLaunchPadRusted` (235 lines) is **not** a `TileEntityLaunchPadBase` subclass (see Headline
finding #2) — it is a standalone `TileEntityMachineBase implements IGUIProvider, IControlReceiver,
ITickable` with a 4-slot inventory (missile-result / `launch_code` / `launch_key` / designator), a
hardcoded single missile type, and a `receiveControl(NBTTagCompound)` hook (`IControlReceiver`,
already ported in this port as `com.hbm.interfaces.IControlReceiver` — confirmed real) used to
externally trigger "release" (spawn the result missile item into slot 0) from whatever placed
`ModItems.missile_doomsday_rusted` there in the first place — likely tied to `SiloComponent`
world-gen loot delivery, not read further here since it's a world-gen integration detail.

### Silo hatch (2 classes, 446 lines) — a blast door, ported alongside the multiblock/lock-security
work this port already has, not new logic

`BlockSiloHatch`/`TileEntitySiloHatch` is a **hand-rolled** dummy-placement multiblock — it does
**not** use `BlockDummyable`/`MultiblockHandlerXR` at all. It's a plain `BlockContainer` whose
`onBlockPlacedBy` manually loops a 7×7-minus-corners area 3 blocks in front of itself and calls
`TileEntitySiloHatch.placeDummy(BlockPos)` for each cell, which sets `ModBlocks.dummy_block_silo_hatch`
and stashes a `target` `BlockPos` back-reference on the resulting `TileEntityDummy`. The door itself
extends `TileEntityLockableBase` (this port's existing lock-security base — confirmed real, used
elsewhere already) for `ItemLock`/`ItemKey` access control, and its 100-tick open/close animation
(`DoorState.OPENING`/`CLOSING`) additionally spawns/despawns a **second**, smaller 3×3 ring of
`TileEntityDummy` blocks around the point 3 blocks past *its own* dummy plane once the door starts
moving — i.e. two independent uses of the same dummy-block trick at two different distances. This
needs `com.hbm.tileentity.machine.TileEntityDummy` (64 lines, CE) ported as its own small
`BlockEntity` — **confirmed not covered by the existing `BlockDummyable` "master/dummy" framework**,
since that framework's dummy blocks are an internal implementation detail of `BlockDummyable` itself,
not a general-purpose "place a dummy block with a back-reference" primitive any block can reuse. The
door-state redstone T-flip-flop, `RadiationSystemNT.markSectionForRebuild`/`markSectionsForRebuild`
calls (confirmed real system in this port per Phase 1/2 radiation work), and the
`IAnimatedDoor`/`IDoor` interfaces are otherwise the same shape as every other door in this mod's
existing lock/door family — no new door abstraction needed.

### Soyuz launch complex (2 classes, 565 lines) — portable to the exact point CE itself stops

`LaunchpadSoyuz` is a large `BlockDummyable` multiblock (`getAllDimensions()` returns **19** separate
`int[]` shape entries via `MultiblockHandlerXR.checkSpace`/`fillSpace` — by far the most elaborate
dimension list of anything surveyed in this report, confirming this is CE's most visually ambitious
launch structure). `TileEntityLaunchpadSoyuz` (435 lines) drives an 8-element parallel-array animation
system (`positions`/`prevPositions`/`speed`/`target`/`syncPositions`, indices named `INDEX_STRUT1..5`/
`CARRIAGE`/`ROTOR`/`TILT`) through a 5-state `SoyuzStatus` enum (`ABSENT → LOADING → FUELING → IDLE →
LAUNCHING`) driven entirely by `updateStates()`'s hand-written state transitions — mechanically
identical in shape to `TileEntityLaunchPadLarge`'s erector/lift state machine, just with 8 axes
instead of 2. Validates `IDesignatorItem` (slot 1) and `ISatChip` (slot 2, gated by `!cargoMode`)
exactly like the other launch pads. **Confirmed dead end**: the `LAUNCHING` branch of `updateStates()`
only retracts the carriage/rotor back down — CE's own inline comment reads `// TBI: countdown,
retracting the struts, launch` (to-be-implemented), meaning **CE itself has never wired an actual
missile-spawn call for the Soyuz complex**. Porting this multiblock and its full animation state
machine faithfully means porting exactly this incompleteness too — do not "finish" the launch
sequence CE never finished, unless the project explicitly wants to go beyond CE parity here.

### Satellite system (`com.hbm.saveddata.satellites`, 15 classes, 1,534 lines)

| Class | Lines | Shape |
|---|---|---|
| `Satellite` (abstract) | 266 | The registry (`satellites`/`itemToClass`/`metaToClass`, populated once by `register()`), the `create(int id)`/`orbit(...)` factory pair, the `Interfaces`/`InterfaceActions`/`CoordActions` enums that drive designator dispatch (see Headline finding #6), the `onCommand`/`onCommandTarget`/`onCommandImpl` text-protocol dispatch (used by non-item callers, e.g. redstone-over-radio — out of this report's scope), and the abstract `getColor()`/`onClick`/`onCoordAction`/`onOrbit` hooks each subclass implements. Pure logic, zero missing dependencies beyond `RTTYSystem.broadcast` (a chat/log broadcast call in `orbit()`, cosmetic, easily stubbed) and `SatPanelPacket` (needs porting as a real payload, see below). |
| `SatelliteSavedData` | 91 | One `Int2ObjectOpenHashMap<Satellite>` keyed by frequency, persisted via `WorldSavedData` (CE) → **maps directly onto this port's already-real `SavedData` pattern** (see `TomSaveData`, read in full as the model). `getData(World)`'s lazy-create-if-absent shape is exactly `SavedData.Factory<T>` + `ServerLevel#getDataStorage().computeIfAbsent(factory, key)`, already proven in this port. |
| `SatelliteDetector`/`Horizons`/`Mapper`/`Miner`/`LunarMiner`/`PrecisionLaser`/`RayScan`/`Relay`/`Resonator`/`Scanner`/`Science` (11 classes) | 24–155 each | Concrete payload behaviors (asteroid-mining loot rolls via `WeightedRandomObject`/`ItemPoolsSatellite`, radar entity-list queries, "Xenium resonator" effects, etc.) not detailed function-by-function here — each is a downstream content package (loot tables, world-scan behavior) layered on the addressing protocol this report establishes, and can be ported independently/incrementally once `Satellite`/`SatelliteSavedData` exist. `SatelliteLunarMiner` (24 lines) and `SatelliteScanner` (29 lines) are near-trivial stubs; `SatelliteHorizons`/`SatelliteRayScan` (107/155 lines) are the largest of the eleven. |
| `SatelliteLaser` | 94 | Read in full as a representative `SAT_PANEL` example: a 5-minute (`CHARGE_TIME = 5*60*20` ticks) cooldown gate around spawning an `EntityDeathBlast` (`com.hbm.entity.logic`, unported, see Deferred scope) at a clicked/commanded coordinate. Confirms the `onClick`/`onCommandImpl` dual-entry-point pattern every `SAT_PANEL` satellite follows. |
| `SatelliteMiner` | 70 | Read in full as a representative `Interfaces.NONE` example (no designator interaction at all — payload arrives passively via `getCargo()`/`ItemPoolsSatellite`, consumed by whatever the "asteroid miner" delivery mechanism is, not itself in this file). |

### Missile in-flight targeting (`EntityMissileBaseNT`, 420 lines) — read to close the loop, not itself
buildable yet (see Deferred scope), but its *design* is fully understood

The constructor computes `accelXZ = decelY = 1/|targetXZ - startXZ|` once; `onUpdate()` re-derives a
normalized `(targetX-startX, targetZ-startZ)` vector every tick and applies it as constant horizontal
acceleration while `motionY` still exceeds an ascending/descending threshold, i.e. a fixed parabolic
lob toward a point chosen at spawn time — see Headline finding #5 for the full explanation and why
this means "does a missile home in on its target" has different answers per missile type. `readEntityFromNBT`/
`writeEntityToNBT`'s field list (`moX/Y/Z`, `poX/Y/Z`, `decel`, `accel`, `tX/Z`, `sX/Z`, `veloc`) is
the complete state a ported entity needs to persist. `IRadarDetectableNT` (already ported in this
port, confirmed real, `getBlipLevel()`/`getTranslationKey()` switch on `ItemMissileStandard.MissileTier`)
and `IChunkLoader` (CE's `ForgeChunkManager`-based force-loading of the missile's flight path — **not**
yet ported, and NeoForge 1.21.1's own forced-chunk API differs from 1.12's ticket system; flagged
under Open questions, not solved here) are the entity's two interface dependencies beyond the base
`EntityThrowableInterp` class itself (see Deferred scope — the whole `com.hbm.entity.missile` package
is a separate prerequisite this report does not include).

### Missile item shape (`ItemMissileStandard`, 104 lines) — trivially portable, needed by every launch
pad's slot-0 validity check

A plain `ItemBase` subclass with three `enum` fields fixed at construction (`MissileFormFactor`
picking a default `MissileFuel`, `MissileTier`, and an overridable `fuelCap`/`launchable` pair) — no
logic beyond a tooltip. `TileEntityLaunchPadBase.isMissileValid`/`setFuel`/`getGaugeState` all read
these three enums directly. This class itself is not this report's scope to fully catalogue (the
~26-entry `ModItems` missile registry, one per concrete missile type, is weapon-item content, likely
its own work package), but launch pad logic cannot compile without it existing, so it is flagged here
as a hard, narrow, easy prerequisite — port the class and enough concrete instances to exercise the
`missiles` factory map, even if the full registry lands incrementally alongside the weapon-item
package.

## Deferred scope

- **`com.hbm.entity.missile` (the entire package, ~16 files) — the dominant blocker for anything
  past "a missile leaves the pad."** `EntityMissileBaseNT` extends `EntityThrowableInterp`
  (`com.hbm.entity.projectile`, unported — confirmed the whole `com.hbm.entity` tree beyond this
  port's own `com.hbm.entity.item`/`ConveyorEntityTypes` conveyor-object family is absent, matching
  `docs/phase2/rbmk_reactor.md`'s and `docs/phase3/bomb_blocks_and_detonators.md`'s independent
  findings on the same package). Every concrete missile (`EntityMissileTier0` through `Tier4`,
  `EntityMissileAntiBallistic`, `EntityMissileCustom`, `EntityMissileShuttle`, `EntityMissileStealth`,
  `EntityMIRV`, `EntityMinerRocket`, `EntityBobmazon`, `EntityBombletSelena`, `EntitySoyuz`+
  `EntitySoyuzCapsule`) is a separate class needing its own explosion-payload wiring (already tracked
  by `docs/phase3/explosion_engine.md`'s `ExplosionLarge`/`EntityNukeExplosionMK5` scope, not
  re-derived here). Recommend this become its own named work package
  (`com.hbm.entity.missile` core-entity prerequisite), following the `ConveyorEntityTypes`
  `DeferredRegister<EntityType<?>>` pattern already proven in this port — narrow scope: the base class
  + enough concrete missiles to exercise every `TileEntityLaunchPadBase.missiles` map entry and the
  `EntityMissileDoomsdayRusted` hardcoded reference, not necessarily all ~26 on day one.
- **`com.hbm.tileentity.turret` (entire package, confirmed absent)** — blocks `ItemDesignatorArtyRange`
  fully (see Phase-3-safe scope table; this item has zero real overlap with launch pads and should be
  scheduled with whichever package eventually owns turrets, not with this one).
- **`IRadarCommandReceiver`** (`com.hbm.tileentity`, CE — a trivial 2-method interface,
  `sendCommandPosition`/`sendCommandEntity`) is implemented by `TileEntityLaunchPadBase` but not yet
  ported in this port. Its only real consumer is `TileEntityMachineRadarScreen` (a radar console TE,
  unported, `com.hbm.tileentity.machine`) sending remote fire commands over the radar-command network
  — narrow, easy interface to port alongside this report's own work (it costs nothing to add even
  before the radar console exists, since `TileEntityLaunchPadBase` already needs to implement it to
  compile), but the actual "fire this launch pad from a radar screen" *feature* waits on the radar
  console TE, which is out of scope here.
- **`com.hbm.util.TrackerUtil`** (`setTrackingRange`/`getTrackerEntry`, used by
  `TileEntityLaunchPadBase.finalizeLaunch` and `EntityMissileBaseNT.onUpdate`'s rotation-encoding
  hack) — a 1.12-era entity-tracker internals reach-around with no confirmed 1.21.1 NeoForge
  equivalent researched in this survey; flagged under Open questions, likely resolves to plain
  `EntityType.Builder.setTrackingRange(...)` (already used by `ConveyorEntityTypes`) making the
  runtime override unnecessary, but that call belongs to whoever implements the missile entity
  package, not this report.
- **`ForgeChunkManager`/`IChunkLoader`** (missile flight-path chunk force-loading) — 1.12 Forge's
  ticket-based chunk-loading API has no direct 1.21.1 NeoForge equivalent researched in this survey
  (NeoForge's forced-chunk API is `ServerChunkCache`/`TicketType`-based and was not read here, being
  out of this report's file set). Flagged as a real open item for the missile-entity package, not
  solved here.
- **`SiloComponent`/world-gen structure placement** (1,408-line legacy `StructureComponent`,
  `net.minecraft.world.gen.structure.template.TemplateManager`-based) — places `launch_pad_rusted` +
  `silo_hatch_drillgon` + loot into freshly generated terrain using 1.12's structure-piece API, which
  has no direct 1.21.1 equivalent (modern Minecraft structure generation is entirely
  `StructureTemplate`/jigsaw-based). This is squarely Phase 4 world-gen scope per this port's own
  established phase boundaries (`docs/phase0/STATUS.md` places world-gen features later), not
  something this report's launch-pad/designator/satellite scope should absorb. The launch pad and
  silo hatch *blocks* are fully player-placeable/portable without this dependency — only the
  world-generated pre-built silo structure needs it.
- **`com.hbm.entity.logic.EntityDeathBlast`** (`SatelliteLaser`'s payload) and similar per-satellite
  payload entities (asteroid-mining delivery, `EntityMinerRocket`) — each concrete satellite's actual
  effect, not the addressing/dispatch protocol. Port incrementally per satellite once each payload
  entity exists; does not block `Satellite`/`SatelliteSavedData`/the designator items themselves.
- **`RTTYSystem.broadcast`** (`Satellite.orbit`'s chat/log announcement) — a redstone-over-radio
  broadcast call, cosmetic to the addressing protocol itself; stub or defer to whichever package owns
  `com.hbm.tileentity.network.RTTYSystem` (not surveyed here).
- **OpenComputers `SimpleComponent`/`@Callback` integration** on `TileEntityLaunchPadBase`/
  `TileEntityLaunchPad` (5+4 methods) — a CE-1.12-only third-party mod integration
  (`li.cil.oc.api.*`, `@Optional.InterfaceList`). This port has made no general OpenComputers-parity
  decision anywhere in Phase 0-2's committed work (grepped, found none); recommend dropping this
  integration entirely for the port rather than treating it as launch-pad-specific scope, but that is
  a mod-wide call, not one this report should make unilaterally — flagged for whoever owns
  cross-cutting third-party-mod-integration decisions.
- **`ContainerLaunchPadLarge`/`ContainerLaunchPadRusted`/`ContainerLaunchpadSoyuz` and their paired
  `GUILaunchPad*`/`GUILaunchpadSoyuz` screens** — signature-level only in this survey (slot layout,
  not full render code). Straightforward `MenuBase`/`GuiInfoContainer` ports once the block entities
  exist, following the exact pattern `docs/phase2/gui_framework.md` and every Phase 2 machine already
  established; not detailed further here since no new GUI-framework question arises from them beyond
  the containerless-screen question the designator/sat items already raise (see Key design/API
  decisions).

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior, this port's own committed code — and,
narrowly, cross-checking `ConveyorEntityTypes`'s own javadoc pointer to Neo Edition's `NtmEntityTypes`
for the entity-registration call shape — for NeoForge 1.21.1 API shape; no API below is invented):

- **Designator coordinate storage is a Data Component, following the exact pattern `docs/phase3/
  bomb_blocks_and_detonators.md` already established for `ItemDetonator`'s `x`/`y`/`z` NBT ints.**
  `ItemDesignator`/`ItemDesignatorRange`/`ItemDesignatorManual` all store the identical shape
  (`xCoord`/`zCoord` ints, CE's own `onCreated` always zero-initializes both). This port's
  `com.hbm.items.tool.ToolDataComponents` (confirmed real: `DeferredRegister<DataComponentType<?>>`
  via `Registries.DATA_COMPONENT_TYPE`, `DataComponentType.<T>builder().persistent(codec).
  networkSynchronized(streamCodec).build()`) is the established, already-registered home for this —
  since every designator item lives in the same `com.hbm.items.tool` package this class already
  serves. Recommend either two more `DataComponentType<Integer>` entries (mirroring the existing
  `TOOL_FUEL`/`TOOL_CHARGE`/`COLTAN_X`/`COLTAN_Z` shape exactly) or one `DataComponentType<BlockPos>`
  (vanilla `BlockPos` already ships its own `Codec`/`StreamCodec`, so no custom codec is needed
  either way) with `y` simply left at 0 — either is a direct, zero-invention translation of CE's
  NBT shape; the choice is stylistic, not a blocker.
- **`ItemDesignatorManual` and `ItemSatInterface`/`ItemSatDesignator`'s coord variant both need a
  `Screen` with no paired `Menu` — confirmed by reading `GUIScreenDesignator.java` in full and both
  items' `provideContainer` overrides, which literally `return null`.** CE opens these via
  `FMLNetworkHandler.openGui(...)` (which still round-trips through the container-open protocol even
  though the container is `null`) purely to get a client-side `GuiScreen` instantiated; the screen
  itself owns two `GuiTextField`s and, on its own "Save" button click, sends a raw client→server
  packet (`ItemDesignatorPacket(x, z)`) that writes the values back onto the *currently held* item —
  there is no inventory-slot interaction anywhere in this flow. **The correct 1.21.1/NeoForge shape
  for this is not `player.openMenu(...)`** (which requires a `MenuProvider` and therefore an
  `AbstractContainerMenu`) **but a direct client-side `Minecraft.getInstance().setScreen(new
  ...Screen())` call from the item's `use()` override when `level.isClientSide()`**, exactly
  mirroring what CE's round-trip achieves without CE's now-obsolete container-open machinery. The
  "Save" action becomes a small new `CustomPacketPayload` (see below) rather than
  `ItemDesignatorPacket`'s old `SimpleImpl` shape. This is a real, load-bearing design decision this
  report surfaces cleanly (three of the six designator/sat items need this shape) rather than letting
  each item's implementer independently rediscover it.
- **`ItemSatInterface`'s live panel stream (`SatPanelPacket`, sent every 2 ticks while held) is the
  one genuinely new payload this report's scope needs**, registered through this port's already-real
  `com.hbm.packet.HbmNetwork`/`RegisterPayloadHandlersEvent` (currently one payload, `BufPacket`, from
  Phase 2 — confirmed real, read in full). Follow that exact pattern: a `record SatPanelPayload(...)
  implements CustomPacketPayload` with a `Type`+`StreamCodec` pair, registered via
  `registrar.playToClient(...)` in `HbmNetwork`, carrying whatever subset of `Satellite` state the
  panel screen needs to render (frequency, type, cooldown/charge state, map/radar data per its
  `InterfaceActions` flags) — CE's version sends the whole `Satellite` object via a hand-rolled
  `ByteBuf` `serialize`, which does not map onto a 1.21 `StreamCodec` one-to-one and needs a
  deliberate field list, not a blind object dump. The "every 2 ticks while item is the active
  hotbar slot" cadence itself (`onUpdate`/`isSelected`) maps directly onto `Item#inventoryTick` (or
  `#onUseTick` is not the right hook — `inventoryTick(ItemStack, Level, Entity, int, boolean)`'s
  `isSelected` boolean parameter is the exact 1.21.1 equivalent CE's own parameter list already
  mirrors).
- **`SatelliteSavedData` maps directly onto this port's already-proven `SavedData` pattern** — read
  `com.hbm.saveddata.TomSaveData` in full as the model: `SavedData.Factory<T>` (static factory +
  `load(CompoundTag, HolderLookup.Provider)` + `save(CompoundTag, HolderLookup.Provider)` override),
  `ServerLevel#getDataStorage().computeIfAbsent(factory, key)` for the lazy-create-once-per-world
  lookup CE's `getData(World)` performs by hand. No new persistence pattern needed; this is a
  same-shape, larger-payload sibling of `TomSaveData` (one `Int2ObjectOpenHashMap<Satellite>`
  serialized as an indexed flat list, exactly like CE's own `writeToNBT`/`readFromNBT` shape — no
  `Codec`-based map serialization needed, a manual indexed-loop read/write is the direct translation
  and matches what `TomSaveData` itself does for its own simpler fields).
- **The launch pad multiblocks need zero new multiblock-framework work.** `LaunchPad`
  (`getDimensions={0,0,1,1,1,1}`) and `LaunchpadSoyuz` (19-entry `getAllDimensions()` via repeated
  `MultiblockHandlerXR.checkSpace`/`fillSpace` calls) are both directly expressible against this
  port's already-real `BlockDummyable`/`MultiblockHandlerXR` (25 existing subclasses, confirmed by
  `docs/phase2/multiblock_framework.md` and re-confirmed here by grep) — no bespoke shape-description
  work is this report's to do.
- **The silo hatch's dummy-placement is a different, smaller primitive than `BlockDummyable`'s own
  master/dummy mechanism, and needs its own tiny `BlockEntity` port** (`TileEntityDummy`, 64 lines CE
  — a `BlockEntity` holding one `target: BlockPos` back-reference plus a one-shot "mark dirty and
  self-destruct if the target block stopped being an `IMultiBlock`" tick check). This port's
  `com.hbm.interfaces.IDummy` marker interface is already ported (empty, as in CE) but the concrete
  `TileEntityDummy` class it marks is not — confirmed by grep. This is a genuinely small, standalone
  port (no framework decision needed, just write the class), but worth flagging explicitly since it's
  easy to assume "dummy blocks" are already fully covered by the Phase 2 multiblock-framework work
  when in fact that framework's dummy blocks are a different, internal-only mechanism.
- **`ModContext.DETONATOR_CONTEXT`** (a `ThreadLocal<Entity>`, CE, confirmed **not yet ported** by
  grep) is a real, small, shared prerequisite between this report and `docs/phase3/
  bomb_blocks_and_detonators.md`: `TileEntityLaunchPadBase.finalizeLaunch`/`TileEntityLaunchPadLarge`
  read it to attribute a detonator-triggered launch's `IThrowable.setThrower(...)` to whichever entity
  fired the detonator. Per the bomb-blocks report's own flag, this is "the one place this section's
  protocol and the launch-pad package's own eventual survey overlap" — confirmed here from the
  launch-pad side too. Port this one 5-line class (`com.hbm.main.ModContext`, one `ThreadLocal<Entity>`
  field) once, in whichever of the two work packages lands first; do not let both re-derive it
  independently.
- **Entity registration, when the missile-entity package lands, has one confirmed real precedent to
  follow**: `com.hbm.entity.ConveyorEntityTypes` (`DeferredRegister<EntityType<?>>` via
  `BuiltInRegistries.ENTITY_TYPE`, `EntityType.Builder.<T>of(ctor, MobCategory).noSummon().sized(w,h)
  .setTrackingRange(n).build(name)`). `EntityMissileBaseNT.getSize(1.5F, 1.5F)` (CE) is the sizing
  value to carry over; `TrackerUtil.setTrackingRange(world, missile, 500)`'s runtime override (called
  at spawn time in `finalizeLaunch`) likely becomes unnecessary once `.setTrackingRange(500)` is set
  declaratively on the `EntityType.Builder` itself — flagged as a probable simplification, not
  confirmed here since it belongs to the missile-entity package's own implementation, not this report.
- **Damage types: no new `DamageType` entries needed for anything in this report's own scope.**
  Missile warhead detonation damage is already covered by `com.hbm.damage.ModDamageTypes`'s existing
  `NUCLEAR_BLAST`/`BLAST`/`SHRAPNEL` entries (confirmed real, per `docs/phase3/
  bomb_blocks_and_detonators.md`'s own finding, re-confirmed here) — nothing in the launch pad/
  designator/satellite protocol itself deals damage.
- **`IBomb`'s `LAUNCHED` return code already exists** in this port's committed `IBomb.java`
  (`BombReturnCode.LAUNCHED`, javadoc'd in CE itself as "success for launching missiles") — confirming
  this interface was already anticipated to need launch-pad support before this report was written;
  `LaunchPad.explode()`/`BlockSiloHatch.explode()` (detonator-triggered launch / hatch-toggle-via-
  detonator) need no interface changes, just the concrete implementations this report scopes.

## Open questions / risks

- **`com.hbm.entity.missile`'s absence is the load-bearing gap.** Every launch pad can be built,
  fueled, and told to fire — but nothing actually flies until that package exists. This report
  deliberately treats it as a sibling prerequisite (recommended as its own work package, following
  `ConveyorEntityTypes`'s proven registration shape) rather than folding ~16 files of entity/ballistics
  code into "launch infrastructure" scope; whoever schedules Phase 3 work should sequence that package
  either just before or in parallel with this one, since neither is independently satisfying to a
  player without the other.
- **Chunk force-loading along a missile's flight path (`ForgeChunkManager`/`IChunkLoader`) has no
  confirmed 1.21.1 NeoForge answer in this survey.** 1.12's ticket API is gone; NeoForge 1.21.1 has a
  different forced-chunk mechanism, but this report did not open it (out of file-set scope). Flag
  explicitly for whoever implements the missile entity: a missile flying over unloaded chunks with no
  force-loading will either desync or get frozen by chunk unloading, and CE's original behavior (force
  every chunk under the flight path) needs a deliberate, confirmed-API replacement, not a guess.
- **`TrackerUtil.setTrackingRange`'s exact necessity is unconfirmed.** It may be entirely subsumed by
  `EntityType.Builder.setTrackingRange(...)` (as `ConveyorEntityTypes` already does declaratively) —
  or CE's runtime per-instance override might exist for a reason not visible from this report's file
  set (e.g. different tiers wanting different ranges from one `EntityType`). Whoever ports the missile
  entity package should verify which is true rather than assuming the declarative builder value is a
  strict superset of what CE's runtime call achieves.
- **The Soyuz complex's unfinished `LAUNCHING` state is a genuine CE-parity ambiguity, not a research
  gap.** Should the port (a) preserve CE's exact incompleteness (players can fuel and ready a Soyuz
  rocket but it can never actually launch, matching upstream today), or (b) finish the launch sequence
  CE itself never wrote, using the same `EntityMissileBaseNT`-family machinery every other launch pad
  uses once that package exists? This is a product decision, not something this report can resolve —
  flagged so it's made deliberately rather than silently inherited or silently "fixed."
- **The three "silo hatch" registry names splitting across two unrelated block classes
  (`BlockSiloHatch` vs. `BlockDoorGeneric`) is a real naming trap for implementation-time greps.**
  Anyone searching this port's future code for "silo hatch" work by class name alone will miss that
  `silo_hatch`/`silo_hatch_large` are generic doors (Phase 1 scope, per `docs/phase1/blocks_generic.md`)
  while only `silo_hatch_drillgon` is this report's `TileEntitySiloHatch` multiblock-blast-door logic.
  Flagged explicitly so the two pieces of work aren't accidentally merged or one silently dropped
  assuming the other already covers it.
- **OpenComputers integration policy is undecided mod-wide**, not just for launch pads — this report
  found no existing committed decision anywhere in Phase 0-2 to point to. Recommend whoever makes that
  call do so once, centrally, rather than each package (this one included) independently guessing.
- **`ItemDesignatorArtyRange`'s inclusion in the task's file list, despite having no real relationship
  to launch pads/silos, suggests the turret-targeting system may not yet have its own named survey.**
  Flagged so it isn't lost — this report explicitly does not claim it as launch-pad scope, but also
  does not want it to fall through a gap between this report and a future turret-package survey that
  might not think to look for it under `items/tool`.
