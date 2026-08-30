# CE vehicle/aircraft entities — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/{EntityUFO,EntityHunterChopper,EntityCyberCrab,
  EntityTaintCrab,EntityTeslaCrab,EntityUFOBase,EntityFBIDrone,IFlyingCreature}.java` (8 files, 1,438
  lines) and `.../entity/mob/EntityFBI.java` (header/class-declaration read, ~40 of 196 lines, to
  confirm it is an ordinary ground mob and not vehicle-shaped)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/train/*.java` (6 files, 1,689 lines — the entire
  rail/train vehicle system: `EntityRailCarBase`, `EntityRailCarCargo`, `EntityRailCarElectric`,
  `EntityRailCarRidable`, `TrainCargoTram`, `TrainCargoTramTrailer`)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/rail/IRailNTM.java` (48 lines, full — the
  block-side rail-traversal contract `EntityRailCarBase` calls into every tick)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/cart/*.java` (7 files, 779 lines — the entire
  NTM-reskinned-vanilla-minecart family: `EntityMinecartNTM`, `EntityMinecartContainerBase`,
  `EntityMinecartCrate`, `EntityMinecartDestroyer`, `EntityMinecartOre`, `EntityMinecartPowder`,
  `EntityMinecartSemtex`)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/item/{EntityBoatRubber,EntityDroneBase,
  EntityDeliveryDrone,EntityRequestDrone,EntityParachuteCrate,EntityMinecartTest}.java` (6 files, 845
  lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/logic/{EntityPlaneBase,EntityBomber,EntityC130}.java`
  (3 files, 674 lines — the scripted-aircraft family)
- `upstream/hbm-ce/src/main/java/com/hbm/itempool/ItemPoolsC130.java` (59 lines, header + first pool
  read in full, remaining 2 pools signature-skimmed — confirms the loot table `EntityC130` needs)
- Full directory listing of `upstream/hbm-ce/src/main/java/com/hbm/entity/**` (11 packages) plus
  targeted `wc -l`/`grep` passes across all of them, to confirm the complete real inventory of
  vehicle-shaped entities rather than assuming the task's own 3 named classes are exhaustive
- This port's own `src/main/java/com/hbm/{items/special/ItemTrain.java, items/tool/{ItemDrone,
  ItemDroneLinker}.java, entity/logic/{IChunkLoader,EntityExplosionChunkloading}.java, config/
  CompatibilityConfig.java, inventory/container/MenuBase.java, blockentity/turret/
  TurretBaseBlockEntity.java, items/weapon/sedna/content/XFactory40mm.java, blocks/generic/
  BlockSupplyCrate.java, api/entity/IRadarDetectableNT.java}` (read in full or targeted, all
  already-committed and already-compiling) and `docs/{phase1/{blocks_network_rail.md,
  DIGEST_REMAINDER.md, items_tool.md, items_special.md}, phase2/items_tool_machine_coupling_and_
  recipe_system.md, phase3/{gun_framework.md, turret_system.md}, phase4/{entities_bosses.md,
  satellites_followup_and_loot_pools.md}}` (read in full or targeted to their relevant sections)
- `upstream/neo-edition/src/main/java/com/hbm/entity/logic/{PlaneBase,Bomber}.java` (2 files, 389
  lines, read in full) — **the only vehicle/aircraft classes Neo Edition has ported**; a repo-wide
  search for rail/cart/UFO/chopper/drone/boat-named classes under `upstream/neo-edition/src/main/
  java/com/hbm/entity/` found nothing else. Cross-referenced for confirmed NeoForge 1.21.1 API shape
  only, per this task's ground rules — every behavioral claim below is sourced from CE.

## Headline finding

This task's own framing needs four corrections, in the same spirit RBMK's and the gun-framework's
own reports had to correct their tasks' framing:

1. **One of the three named "vehicle" entities isn't a vehicle at all.** `EntityCyberCrab extends
   EntityMob` (read in full) — it is an ordinary 0.75×0.35 ground-walking mob using stock
   `EntityAIWanderAvoidWater`/`EntityAIPanic` pathfinding, `IRangedAttackMob`'s standard
   `EntityAIAttackRanged` goal, and vanilla `EntityMob`'s ground AI. It has **no travel/motion
   override, no rider, no flight, nothing "vehicle-shaped" about its movement at all** — the only
   things distinguishing it from a generic hostile mob are a legacy-`EntityBullet` ranged attack
   (already the sibling report's territory per this task's own framing) and an on-death explosion.
   Its two subclasses (`EntityTaintCrab`, `EntityTeslaCrab`, both read) are equally ordinary —
   `EntityTaintCrab` even fires the *modern* Sedna gun framework (`EntityBulletBaseMK4` via
   `XFactory762mm`), not the legacy bullet class. This report gives the crab family a one-line
   mention below and defers all three to whichever Phase 4 area covers general hostile-mob AI; they
   do not belong in a vehicle/aircraft survey at all.
2. **For the two named entities that *are* vehicle-shaped (UFO, Hunter Chopper), the "sibling
   report" this task describes as covering their "bullet-firing behavior" actually already exists
   and covers far more than bullets.** `docs/phase4/entities_bosses.md` (read in full) is a complete,
   already-written Phase 4 report that already documents both entities' health/attributes, full
   attack-pattern tables, death/crash sequences, loot drops, and spawn mechanisms — the task's framing
   ("a sibling report covers their bullet-firing behavior in depth") undersells it. **This report's
   real, non-overlapping job for UFO/Chopper is narrow: their flight/hover movement model and
   confirming neither has a rider.** Duplicating `entities_bosses.md`'s attack/health/loot tables here
   would be redundant, not thorough — this report cross-references it instead (see the UFO/Chopper
   row below) and adds only what that report did not cover: the actual `motionX/Y/Z` mechanics that
   make them fly.
3. **The real "vehicles" PORT_SPEC.md's Phase 4 parenthetical points to are a much larger, mostly
   *unclaimed* surface than 3 mob classes** — a full directory sweep of `com.hbm.entity/**` (11
   packages) turns up five genuinely distinct, genuinely vehicle-shaped systems, all still fully
   unported and none previously scoped by any Phase 1–3 report in more than a passing mention:
   - A **from-scratch custom rail/train system** (`com.hbm.entity.train`, 6 files, 1,689 lines) with
     its own non-vanilla locomotion physics, coupling/consist logic, and a rideable multi-seat engine
     car — already flagged as a genuine phase-ownership gap by `docs/phase1/DIGEST_REMAINDER.md`,
     which explicitly recommends scheduling it "alongside or after Phase 4's custom-entity/vehicle
     work" (i.e., work exactly like this report). This is, by a wide margin, the deepest and most
     novel vehicle-mechanics content in the mod — deeper than either UFO or Hunter Chopper.
   - A **scripted-aircraft family** (`com.hbm.entity.logic.{EntityPlaneBase,EntityBomber,EntityC130}`,
     3 files, 674 lines) — non-rideable, chunk-loading, straight-line-flight NPC planes used for
     air-strike and supply-drop events. **This is not a hypothetical gap**: `EntityC130` is already a
     named, documented forward-reference TODO inside this port's own committed, compiling Phase 3 gun
     code (`src/main/java/com/hbm/items/weapon/sedna/content/XFactory40mm.java`'s class javadoc and
     inline TODO at line 181), and `EntityBomber` is separately named as a forward reference inside
     this port's own committed turret-targeting code
     (`src/main/java/com/hbm/blockentity/turret/TurretBaseBlockEntity.java:656-660`). Both are exactly
     the kind of "already named, in-repo forward reference" `docs/phase4/entities_bosses.md` found for
     its own five bosses — not this report's own inference.
   - A **reskinned-vanilla-minecart family** (`com.hbm.entity.cart`, 7 files, 779 lines) — thin
     `EntityMinecart` subclasses with custom loot-drop/GUI/appearance behavior and **zero custom
     movement physics** (confirmed by reading every file: none override `moveMinecartOnRail`,
     `travel`, or any vanilla minecart motion hook). **A real, concrete correction to Phase 1's own
     research here**: `docs/phase1/items_tool.md`'s line 71 flags `EntityMinecartSemtex` as "an
     explosive cart" needing "Phase 3 explosives content" — but the actual `EntityMinecartSemtex`
     class (read in full, 40 lines) has no explosion logic whatsoever, no fuze, no `killMinecart`
     override; it is a purely cosmetic texture-swap crafted from a `semtex` block, functionally
     identical to `EntityMinecartOre`. (The minecart that *does* explode-on-overflow is
     `EntityMinecartCrate`, for an unrelated reason: its `killMinecart` triggers a 2-block TNT-style
     blast if its serialized inventory NBT exceeds 6,000 bytes — a data-safety valve, not a themed
     "explosive cart" mechanic.) This report corrects the record so a future implementer doesn't port
     a fuze/detonation system that doesn't exist in CE.
   - A **trivial vanilla-`Boat` reskin** (`com.hbm.entity.item.EntityBoatRubber`, 26 lines) — a
     one-field override of `getItemBoat()` on top of vanilla `EntityBoat`. Zero novel mechanics.
   - An **unclaimed logistics-drone entity family** (`com.hbm.entity.item.{EntityDroneBase,
     EntityDeliveryDrone,EntityRequestDrone}` + `EntityParachuteCrate`, 4 files, 793 lines) — small
     flying "vehicles" with their own straight-line homing-flight model, distinct from every other
     flight model in this survey. **Also not a hypothetical gap**: this port's own already-committed
     `src/main/java/com/hbm/items/tool/{ItemDrone,ItemDroneLinker}.java` carry explicit doc-comment
     TODOs naming `EntityDroneBase`/`EntityDeliveryDrone` as the exact missing dependency, quoting
     `docs/phase2/items_tool_machine_coupling_and_recipe_system.md`'s own recommendation for "a
     dedicated 'drone logistics' Phase 2 research package" — which, per a directory listing of
     `docs/phase2/`, was never actually written. This report picks up the **entity movement** half of
     that gap (squarely "vehicle" content) and explicitly hands the **block/GUI network** half
     (`TileEntityDroneDock`/`Waypoint`/`Requester`/`Provider`, `IDroneLinkable`, 4 GUI/Container pairs)
     back to that still-unclaimed Phase 2 package in Deferred scope below, rather than silently
     absorbing it or silently dropping it a second time.
4. **None of this survey's genuinely-custom flyers use a vanilla `travel()` override, and the one
   entity that most looks like it needs vehicle physics (the train) uses none of Minecraft's motion
   fields at all.** Every hand-rolled flyer (`EntityUFO`, `EntityHunterChopper`, `EntityUFOBase`/
   `EntityFBIDrone`, `EntityDroneBase`'s family) computes a target-seeking `motionX/Y/Z` vector
   directly inside `updateAITasks()`/`onUpdate()` and then either lets the inherited `EntityFlying`
   travel loop apply it (UFO/Chopper/UFOBase family) or calls `move(MoverType.SELF, ...)` manually
   every tick (drone family) — there is no per-entity `travel(float,float,float)` override anywhere
   in this survey. `EntityPlaneBase` doesn't even extend `EntityFlying` — it extends bare `Entity`
   and manually adds its motion to its own position once a tick, with no drag/gravity at all while
   healthy. And `EntityRailCarBase` (the deepest, most algorithmically complex entity in this whole
   report) extends bare `Entity` too and **never uses `motionX/Y/Z`/`move()` for its on-rail
   locomotion at all** — it queries `IRailNTM.getTravelLocation(...)` block-by-block from its rail
   neighbors and calls `setPosition(...)` directly; motion fields only appear in its de-railed
   "coast to a stop off the tracks" fallback path. Whoever implements any of these should not go
   looking for a `travel()` override pattern to reuse across this report's entities — there isn't
   one, by CE's own design, in five out of six of them.

## Phase-4-safe scope

### UFO and Hunter Chopper — flight-physics supplement to `docs/phase4/entities_bosses.md`

Both are already fully scoped for health/attributes/attack-pattern/death-sequence/loot/spawn by
`docs/phase4/entities_bosses.md` (see Headline finding #2) — do not re-derive those from this table.
This report's own contribution is the movement model and the rider question:

| Entity | Movement model (read in full from CE) | Rider/passenger |
|---|---|---|
| `EntityUFO` (`extends EntityFlying`) | **Full reset-then-recompute every tick.** `updateAITasks()` zeroes `motionX/Y/Z` unconditionally at the top of every tick, then — only while `courseChangeCooldown > 0` (a 40–60-tick timer restarted whenever a fresh waypoint is picked from `setWaypoint`) — recomputes a unit vector toward the waypoint scaled by a flat speed (5 blocks/tick if chasing a player, 2 otherwise) and re-assigns it to `motionX/Y/Z`, gated by the same `isCourseTraversable` block-sweep check `EntityHunterChopper`/`EntityUFOBase` all independently reimplement (4 near-identical copies of this exact AABB-sweep-and-bail helper exist across this survey — a real duplication worth collapsing into one shared static helper at port time, not preserving 4×). Once the countdown expires or the UFO arrives within 5 blocks, it goes to a dead stop and **hovers in place, doing nothing**, until the next 50-tick target-rescan cycle picks a fresh waypoint 35 blocks past the target. This is a "dart to a spot, then hang motionless" hover model, not smooth continuous flight. | **None.** No `processInteract`/`canBeRidden` override anywhere in the class — confirmed not rideable. |
| `EntityHunterChopper` (`extends EntityFlying`) | **Accumulating impulse, not reset-then-set.** Unlike UFO, `onUpdate()` **adds** a small (`0.1D`-scaled) unit-vector impulse toward the waypoint into `motionX/Y/Z` via `+=` once every `courseChangeCooldown` (2–6 ticks) rather than overwriting it — meaning the chopper actually carries momentum/drag between impulses (via whatever residual friction `EntityFlying`'s inherited travel loop applies), a materially different feel from UFO's harsh stop-start hover despite sharing a superclass. Waypoints are picked within a 16-block gaussian jitter of either the current target or (if none) the chopper's own current position, re-picked whenever within 1 block or beyond 60 blocks of the current one. A separate yaw/pitch **banking** calculation (`atan2` of the motion vector, clamped so `rotationPitch` never sits inside a `30°–330°` dead zone — a rendering-model workaround, not gameplay) drives visible nose-up/down/turn attitude independent of the actual motion math. On entering its crash state (`isDying`), gravity switches on manually (`motionY -= 0.08` every tick, since `EntityFlying` itself applies none) and horizontal speed is forcibly maintained above 1.8 blocks/tick by scaling `motionX/Z` — a deliberate "can't just plummet straight down, must glide/spin while dying" effect. CE's own top-of-file comment (preserved by `entities_bosses.md`) flags this whole class as unusually fragile; that applies to this movement code too. | **None.** No passenger logic anywhere in the class. |

### The crab family — confirmed not vehicles (one-line each, per Headline finding #1)

| Entity | Confirmed reality |
|---|---|
| `EntityCyberCrab` (`extends EntityMob`, 111 lines) | Ordinary ground mob, stock AI goals, legacy `EntityBullet` ranged attack, explodes on death. No vehicle content. |
| `EntityTaintCrab` (`extends EntityCyberCrab`, 84 lines) | Same, but fires the **modern** Sedna `EntityBulletBaseMK4` via `XFactory762mm` instead of the legacy bullet — a detail worth flagging to whoever owns "hostile mob AI," since it means this one non-boss mob is a live consumer of Package A from `docs/phase3/gun_framework.md`, not the legacy system. |
| `EntityTeslaCrab` (`extends EntityCyberCrab`, 44 lines) | Same base, taller hitbox (0.75×1.25), otherwise unremarkable. |

None of the three are covered further here — deferred whole to general hostile-mob-AI research (see Deferred scope).

### The rail/train vehicle system (`com.hbm.entity.train`, 6 files, 1,689 lines — the deepest content in this report)

| Class | Lines | What it actually does |
|---|---|---|
| `EntityRailCarBase` (abstract, `extends Entity`) | 782, read in full | **Not a `motionX/Y/Z` vehicle at all.** Locomotion is a pure block-query walk: `getRelPosAlongRail(anchor, distanceToCover, ...)` repeatedly calls `IRailNTM.getTravelLocation(world, x, y, z, ...)` on whichever rail block currently occupies `getCurrentAnchorPos()` (`posX, posY+0.25, posZ`), asking that block "if I'm here facing this way and want to travel N more blocks, where do I end up and how much distance is left/what's my new heading" — the rail block itself decides curve/slope/switch routing; the entity just walks the answer it's given, up to 30 loop iterations per call (a runaway-loop guard, not a distance cap). The full-precision position (`renderX/Y/Z`, distinct from `posX/Y/Z`) is derived from **two separate rail queries** per car (front-axle position at `+lengthSpan`, rear-axle position at `-lengthSpan`), averaged — this is how the entity's visual position/pitch reflects it riding *up and down slopes and around curves* smoothly even though its logical "anchor" position only exists at one point. `derail()` flips `isOnRail=false`, after which the *only* place `motionX/Y/Z`-style physics appears in this class kicks in: a simple `move(MoverType.SELF, ...)` coast-and-decay (`cachedSpeed *= 0.95` per tick) fallback for a car that's fallen off the rails. Coupling (`TrainCoupling.FRONT/BACK`, a per-instance `coupledFront`/`coupledBack` reference pair) is player-driven via `ModItems.coupling_tool` on `processInitialInteract`, snapping the two nearest compatible coupling points within 1 block. |
| `LogicalTrainUnit` (static nested class inside `EntityRailCarBase`) | ~250 of the 782 | **The consist/train-physics core** — a `Set`-built chain of coupled cars (`generateTrain`, a simple "walk `coupledFront`/`coupledBack` links until null" traversal, re-run any time a car's coupling state changes or it has no `ltu` yet), whose `getTotalSpeed()` sums every car's own `getCurrentSpeed()` (sign-flipped per car depending on which way it's coupled into the chain relative to the lead car), clamped to the **slowest** car's `getMaxRailSpeed()` — i.e. **one underpowered trailer caps the whole train's speed**, a real, intentional design constraint, not a bug. `moveTrainByApproach`/`moveWagonTo`/`combineWagons`/`collideTrain` handle multi-car choreography: each non-lead car is positioned relative to the car ahead of it via its coupling-point *distance* (not by literally following the same rail path), with a soft nonlinear compression formula (`len = len / (0.5/(len*len) + 1)`) that lets a train "concertina" slightly when decelerating rather than instantly snapping to exact coupling spacing; `collideTrain` applies a push-force impulse (not real physics, a simple corrective displacement) when two independently-moving consists' bounding boxes overlap. This is genuinely the single most complex pure-logic surface in this entire report — closer in spirit to RBMK's neutron-flux math or the gun framework's spread/damage formulas than to typical entity boilerplate, and equally worth isolating into unit-testable pure functions per the same PORT_SPEC risk note both of those reports already flagged for their own domains. |
| `EntityRailCarCargo` (abstract, `extends EntityRailCarBase implements IInventory`) | 202, read in full | Adds a plain NBT-backed `NonNullList<ItemStack>` inventory (identical shape to `EntityMinecartContainerBase` below) plus a synced `OCCUPIED_SLOTS` int for client-side "how full" rendering. No movement content of its own. |
| `EntityRailCarRidable` (abstract, `extends EntityRailCarCargo`) | 287, read in full | **The only rider-carrying vehicle in this entire report.** `getCurrentSpeed()` is genuinely player-driven: while a `EntityPlayer` is the `getControllingPassenger()` (vanilla `getPassengers().get(0)`), reading that player's own `moveForward` (forward/back WASD axis) each tick to accelerate/decelerate an internal `engineSpeed` accumulator (`+= getPoweredAcceleration()` forward, `-=` backward, decaying by `getPassivBrake()` when neither key is held and the subclass's `shouldUseEngineBrake` says to coast rather than idle-burn fuel), clamped to `±getMaxPoweredSpeed()`, plus a constant `getGravitySpeed()` term (unused by CE's one concrete subclass, always 0, but present for a hypothetical downhill-freewheeling car). **Multi-seat passengers use a second layer of dummy entities**: `SeatDummyEntity` (a nested `Entity` subclass, one spawned per occupied non-driver seat) is what the rider actually "rides" — `getNearestSeat(player)` picks either the driver's seat (`-1`, ridden directly on the `EntityRailCarRidable` itself) or the closest empty passenger seat by raw look-vector distance (an 180-unit cutoff, not blocks — this is a squared-or-otherwise-scaled distance value, not a literal block radius, worth confirming exactly at implementation time rather than assumed to be "180 blocks"), each `SeatDummyEntity` re-positioning itself every tick relative to the parent car's `renderX/Y/Z` and yaw/pitch via the same coupling-style vector rotation used throughout this package. Losing its own passenger despawns the seat dummy the same tick. |
| `EntityRailCarElectric` (abstract, `extends EntityRailCarRidable`) | 72, read in full | Adds a synced `POWER` int (an energy buffer, not vanilla FE/RF — drained by `getPowerConsumption()`, charged from an `IBatteryItem`-implementing item in a subclass-chosen inventory slot, or instantly maxed by `ModItems.battery_creative`). `canAccelerate()` is hardcoded `true` (no fuel-empty stall state at this layer — a concrete subclass could still gate acceleration on `getPower() > 0` itself, but `TrainCargoTram`, the one concrete subclass, does not). |
| `TrainCargoTram` (concrete, `@AutoRegister`, `extends EntityRailCarElectric implements IGUIProvider`) | 188, read in full | **CE's only concrete rideable/powered rail car.** 5×2 hitbox, standard gauge, 2 passenger seats plus the driver's own seat, a 29-slot cargo inventory (slot 28 reserved for a battery/charge item), `getMaxPoweredSpeed()=0.5`, `getMaxRailSpeed()=1` (i.e. the *rail-speed* ceiling exceeds the *powered* one — coasting/gravity-assisted speed can, in principle, exceed what the motor alone provides), a full `Container`/`GuiScreen` pair (`ContainerTrainCargoTram`/`GUITrainCargoTram`) built directly on vanilla `Container`/`GuiContainer` (**not** this port's own `MenuBase`/`GuiInfoContainer` — see Key design decisions for why that matters). |
| `TrainCargoTramTrailer` (concrete, `@AutoRegister`, `extends EntityRailCarCargo implements IGUIProvider`) | 158, read in full | The non-powered half of the same 2-car set — `getCurrentSpeed()` hardcoded to `0` (it never drives itself, only ever gets towed via coupling to a `TrainCargoTram`), 45-slot cargo inventory, its own `Container`/`GuiScreen` pair opened via ordinary right-click (`processInitialInteract` → `player.openGui(...)`) rather than the seat-picking logic `EntityRailCarRidable` uses. |
| `IRailNTM` (interface, in `com.hbm.blocks.rail`) | 48, read in full | The block-side contract every rail block (Phase 1/2's already-scoped `blocks/rail`, 13 files) must implement: `getSnappingPos`, `getTravelLocation` (the method `EntityRailCarBase` calls every tick — takes a full 3D position/heading/speed and returns a new position plus overshoot/pos-anchor/yaw via the mutable `RailContext`/`MoveContext` helper objects), and `getGauge` (`STANDARD`/`NARROW`, a hard mismatch check — a `NARROW`-gauge car cannot enter a `STANDARD` rail block or vice versa). |

**Confirmed by CE's own `ItemTrain.EnumTrainType`** (this port's own already-committed
`src/main/java/com/hbm/items/special/ItemTrain.java`, itself a direct, faithful port of CE's enum):
CE ships **exactly 2** concrete rail-car types in the whole mod (`CARGO_TRAM`, `CARGO_TRAM_TRAILER`)
— confirming the same "narrow but deep core, thin content" shape RBMK's and the gun framework's own
reports found in their domains. The abstract hierarchy above (4 layers) is deeper than the concrete
content it currently serves; do not expect more rail-car variety to show up once ported.

### The scripted-aircraft family (`com.hbm.entity.logic`, 3 files, 674 lines)

| Class | Lines | Movement/behavior |
|---|---|---|
| `EntityPlaneBase` (abstract, `extends Entity implements IChunkLoader`) | 241, read in full | **No gravity, no drag, no `travel()` override at all while healthy** — `onUpdate()`'s server branch is literally `setPosition(posX + motionX, posY + motionY, posZ + motionZ)` once a tick, with `motionY` forced to exactly `0` every tick while `health > 0` (a plane flies dead level forever until shot down; there is no altitude AI at all — whatever `motionX/Z` a subclass's `fac(...)` factory method set at spawn time is the plane's course for its entire ~200-tick (`getLifetime()`) existence, no waypoints, no steering). Only once `health <= 0` does `motionY -= 0.025` kick in (a slow, constant-acceleration nose-down) and the plane starts spawning trailing "gas flame" particles, crashing into an `ExplosionVNT` blast the instant it either hits a non-air block or its Y drops below 0. Implements this port's own already-real `com.hbm.entity.logic.IChunkLoader` contract (see Key design decisions) rather than CE's raw `ForgeChunkManager.Ticket` plumbing — CE's version force-loads only the single chunk column the plane currently occupies, re-forcing on every chunk-boundary crossing (`loadNeighboringChunks`, called unconditionally every tick, not gated on actually having crossed a boundary — a minor inefficiency worth optionally tightening at port time using this port's own `IChunkLoader.updateChunkTicket`'s already-built diff-and-swap logic instead of re-forcing the identical chunk every tick). Client-side interpolation (`turnProgress`/`syncPosX/Y/Z`/`syncYaw/Pitch`) is the same `setPositionAndRotationDirect`-driven lerp pattern found throughout this survey (UFO's `LogicalTrainUnit`, the rail cars, the drone family) — see Key design decisions for the confirmed 1.21.1 replacement. |
| `EntityBomber` (`@AutoRegister`, `extends EntityPlaneBase implements IConstantRenderer`) | 332, read in full | 8 static `statFacX(world, x, y, z)` spawn-factory methods (Carpet/Napalm/Chlorine/Orange/ABomb/Stinger/Boxcar/PC — one per "air-strike type" a caller can request), each just setting a different `bombStart`/`bombStop`/`bombRate`/`type` tuple and a random cosmetic `STYLE` byte before calling the shared `fac(...)` position/heading setup (spawn 100 blocks upwind of the target at Y+50, aim inward). The actual per-tick payload drop (`onUpdate`, gated by `ticksExisted` falling inside the `[bombStart, bombStop)` window on a `% bombRate` cadence) branches on `type`: types 0/1/2/4/5 spawn an `EntityBombletZeta` submunition (a separate projectile entity, out of this report's scope — see Deferred scope); type 3/7 call `ExplosionChaos.spawnChlorine(...)` directly (no submunition entity at all — an unowned Phase 3/4 explosion-engine dependency, see Deferred scope); type 6 spawns an `EntityBoxcar` "rocket" entity instead of a bomblet. **Every drop is gated behind `CompatibilityConfig.isWarDim(world)`**, a per-dimension flag this port's own `CompatibilityConfig.java` has *already, explicitly* decided not to port (see Key design decisions) — meaning this class's actual bomb-dropping is currently blocked on a deliberately-dropped CE mechanic, not a missing-entity gap. `EntityBomber` is spawned by exactly one caller in all of CE: `com.hbm.items.tool.ItemBombCaller` (not read in this survey, not yet ported — an item that calls in a scripted airstrike at the use location). |
| `EntityC130` (`@AutoRegister`, `extends EntityPlaneBase`) | 101, read in full | Simpler single-purpose subclass — at exactly the halfway point of its lifetime (`ticksExisted == getLifetime()/2`), spawns one `EntityParachuteCrate` loaded with items drawn from `ItemPoolsC130`'s `POOL_SUPPLIES` (5 rolls) or `POOL_WEAPONS`+`POOL_AMMO` (1-2 + 6 rolls) depending on its `C130PayloadType` (`SUPPLIES`/`WEAPONS`/a third joke-named constant, `A_FUCKING_FUEL_TRUCK`, that this survey found **zero code path ever sets** — confirmed by grep, dead enum value, not a missing feature). Spawned by exactly one caller in CE: `XFactory40mm`'s `g26_flare_supply`/`g26_flare_weapon` ammo types' `onUpdate` lambda (a called-in supply-drop flare round in the modern Sedna gun framework, already a documented forward reference in this port's own committed code — see Headline finding #3). |

### Minecart family (`com.hbm.entity.cart`, 7 files, 779 lines) — thin vanilla wrappers, no custom physics

| Class | Lines | What it adds over vanilla `EntityMinecart` |
|---|---|---|
| `EntityMinecartNTM` (abstract, `extends EntityMinecart`) | 93, read in full | Adds a synced `CART_BASE` int (`EnumCartBase` — a purely cosmetic "which vanilla-cart wood/metal skin" selector, not a gameplay stat) and overrides `killMinecart` to drop `getCartItem()` (each subclass's own item form, preserving a custom name if set) instead of vanilla's default drop. **No movement override anywhere** — confirmed by reading the full file; rail-following physics is 100% inherited from vanilla `EntityMinecart`. |
| `EntityMinecartContainerBase` (abstract, `extends EntityMinecartNTM implements IInventory`) | 154, read in full | Same NBT-backed `NonNullList<ItemStack>` inventory shape as `EntityRailCarCargo` above — a repeated pattern across this whole report worth a single shared mixin/base at port time rather than 3 independent copies (`EntityRailCarCargo`, `EntityMinecartContainerBase`, and `EntityDeliveryDrone`'s `ItemStackHandler`-backed variant all reimplement the same `IInventory` boilerplate). |
| `EntityMinecartCrate` (`@AutoRegister`, `extends EntityMinecartContainerBase implements IGUIProvider`) | 202, read in full | 54-slot crate cart, restores its contents from the *placed item's* NBT at construction time (an item-to-entity NBT round-trip, not a fresh empty inventory). `killMinecart`'s NBT-size safety valve (**this is the actual "explosive minecart" CE has** — see Headline finding #3) triggers a `world.newExplosion(..., 2F, true, true)` blast plus drops a second empty crate-cart item if the serialized inventory NBT exceeds 6,000 bytes, alongside always dropping the (now-truncated-to-nothing) item form. |
| `EntityMinecartDestroyer` (`@AutoRegister`, `extends EntityMinecartContainerBase implements IGUIProvider`) | 225, read in full | An 18-slot "filter" cart — every 5 ticks, scans a fixed 5×3.5×5 AABB around itself for `EntityItem` drops matching either slots 0-8 (exact item+damage match) or 9-17 (item-only match) and despawns matches on contact, functioning as a rolling item-filter/voider along a conveyor-adjacent rail line. `isItemValidForSlot` always returns `false` (its 18 slots are a read-only filter template, not a real inventory a hopper could insert into). |
| `EntityMinecartOre` / `EntityMinecartPowder` (`@AutoRegister`, `extends EntityMinecartNTM`) | 27 / 38, read in full | Trivial — only differ by `getCartItem()`'s target `EnumMinecart` constant and (Powder only) a client-only `renderSpecialContent` texture swap. No inventory, no gameplay logic beyond being a themed vanilla minecart. |
| `EntityMinecartSemtex` (`@AutoRegister`, `extends EntityMinecartNTM`) | 40, read in full | **Confirmed purely cosmetic** (Headline finding #3) — same shape as Ore/Powder, a different crafted-item source (`semtex` block + empty cart) and a different render texture. No explosion, no fuze, no unique behavior of any kind. |

### `EntityBoatRubber` — trivial vanilla-`Boat` reskin

26 lines, read in full: `extends EntityBoat`, overrides exactly one method (`getItemBoat()`) to return
a different item. Zero novel mechanics; port as a one-line `Boat` subclass once this port has its own
`Boat`/`ChestBoat`-equivalent registration pattern (not yet established anywhere in this port per a
grep — flagged, not a blocker, since vanilla boats are unmodified content).

### Logistics-drone entity family (`com.hbm.entity.item`, 4 files, 793 lines) — entity-movement half only

| Class | Lines | Movement/behavior |
|---|---|---|
| `EntityDroneBase` (`extends Entity`) | 166, read in full | **Straight-line homing, no gravity, no vanilla travel hook.** Server-side `onUpdate()` zeroes `motionX/Y/Z`, then — only while `targetY != -1` (a sentinel "no target set" value, not a null check) — computes a unit vector toward `(targetX,targetY,targetZ)` scaled by `min(getSpeed(), remainingDistance)` (i.e. it decelerates to an exact stop at the target rather than overshooting) and calls `move(MoverType.SELF, motionX, motionY, motionZ)` directly. A crude obstacle-avoidance hack: `if(collidedHorizontally) motionY += 1` — hitting a wall makes the drone lurch upward by a full block next tick rather than pathfinding around it. `setAppearance(int)` (0=empty/1=crate/2=barrel) is a purely cosmetic synced byte with no gameplay effect at this base-class layer. Base `getSpeed()` is `0.125` blocks/tick. |
| `EntityDeliveryDrone` (`@AutoRegister`, `extends EntityDroneBase implements IInventory, IChunkLoader`) | 266, read in full | The "patrol"/logistics-hauler variant — an `ItemStackHandler`-backed 18-slot inventory plus an optional carried `FluidStack`, `getSpeed()` tripled (`0.375*3`) when its synced `IS_EXPRESS` flag is set (the "express" `ItemDrone.DroneType` variant from `ItemDrone`'s already-committed enum). Optionally force-loads a full 3×3 chunk neighborhood centered on both its current position *and* a one-chunk lookahead along its current heading (`loadNeighboringChunks`) via raw `ForgeChunkManager` calls — CE's own most elaborate chunk-loading footprint in this survey (every other chunk-loading entity here force-loads just its own current chunk). `hitByEntity` from a player instantly destroys it, drops its full cargo plus a `drone` item encoding both its express-ness and chunk-loading-ness back into 4 metadata values — a real "player can shoot down a delivery drone for its cargo" mechanic. |
| `EntityRequestDrone` (`@AutoRegister`, `extends EntityDroneBase`, `@Spaghetti("onUpdate needs to be cleaned up")` in CE's own source) | 285, read in full | The "on-demand fetch" variant — carries a `List<Object> program` (a small mixed-type instruction queue: a `BlockPos` waypoint, an `AStack` "go pick up this item type" instruction, or the `UNLOAD`/`DOCK` terminal actions), executed one instruction at a time only once the drone's current motion magnitude drops below `0.01` (i.e. "arrived and stopped," gating the next instruction pop). Each pickup/dropoff/dock action raytraces straight down 4 blocks from its own position to find the `TileEntityDroneProvider`/`Requester`/`Dock` block below it — **this is the one place the entity half and the not-yet-scoped block-network half of this subsystem meet directly** (see Deferred scope). Self-destructs (dropping any held item plus its own `drone` item) if its program empties with nothing left to do — CE's own annotation flags this method as needing cleanup; treat as confirmed-by-reading but not necessarily confirmed-elegant, matching the same caveat `docs/phase4/entities_bosses.md` gave `EntityHunterChopper`. |
| `EntityParachuteCrate` (`@AutoRegister`, `extends Entity`) | 76, read in full | A simple falling-with-terminal-velocity physics object — `motionY` decays toward a `-0.2` blocks/tick cap (a parachute-drag terminal velocity, not free-fall acceleration) rather than accelerating unboundedly, clamped to never fall below world Y=600 on spawn (a safety clamp for high-altitude C130 drops, not gameplay-relevant at normal play heights). On touching any non-air block, replaces itself with a `ModBlocks.crate_supply` block one Y above the impact point and hands its carried `items` list to that block's already-real, already-ported `BlockSupplyCrate.SupplyCrateBlockEntity` (confirmed zero missing dependency — see Key design decisions). |

## Deferred scope

Real dependencies of *this specific* subsystem that belong to other packages/phases/research passes,
matching the "which package, which phase" format the ground rules ask for:

- **`com.hbm.blocks.rail` (13 files) + `com.hbm.tileentity.rail.TileEntityRailSwitch`** — the
  block-side half of the train system, every file of which extends `BlockDummyable` and is therefore
  gated on the Phase 2 multiblock framework (`MultiblockHandlerXR`), per `docs/phase1/
  blocks_network_rail.md`'s own already-thorough 13-file survey (read for cross-reference, not
  re-derived here). That report explicitly could not assign this package a clean phase home and
  recommended treating `blocks/rail` + `entity/train` + `tileentity/rail` as **one dedicated work
  package**, scheduled alongside or after this report's own vehicle-entity work. This report supplies
  the "entity/train" third of that recommendation in full; the other two-thirds (`blocks/rail`,
  `tileentity/rail`) still need their own implementation pass gated on Phase 2's multiblock framework,
  and the explicit phase-boundary call `docs/phase1/blocks_network_rail.md` asked for should be made
  before implementation starts, not silently defaulted.
- **`EntityUFO`/`EntityHunterChopper`'s health, attack patterns, death/crash sequences, loot, and
  spawn mechanisms** — already fully owned by `docs/phase4/entities_bosses.md` (Headline finding #2).
  Do not re-derive; this report's UFO/Chopper rows above are additive (movement only), not a
  replacement summary.
- **`EntityCyberCrab`/`EntityTaintCrab`/`EntityTeslaCrab`'s AI, spawn conditions, and loot** —
  confirmed not vehicle content (Headline finding #1); belongs with whichever Phase 4 area researches
  general hostile-mob AI. This report's one-line table entries are sufficient to rule them out of
  vehicle scope, not a starting point for that future research.
- **`EntityFBIDrone`'s combat behavior and `EntityFBI`'s ground-trooper AI/raid mechanics** — this
  report read `EntityFBIDrone` in full only to confirm it shares `EntityUFOBase`'s waypoint-hover
  movement model (it does, near-identically — see `EntityUFOBase`'s own row) and to note it adds a
  grenade-drop attack on top; the full FBI-raid mechanic (spawn triggers, `BossSpawnHandler`
  integration, `EntityFBI`'s ground AI) is already correctly deferred by `docs/phase4/
  entities_bosses.md`'s own Deferred scope to "whichever Phase 4 area researches general
  hostile-mob content" — this report does not re-claim it, and confirms `EntityUFOBase` itself
  (207 lines, read in full above) is the one piece of that cluster that *is* genuinely
  vehicle-movement-shaped, in case that future report wants to hand this one class back here instead
  of re-deriving its movement math independently.
- **`com.hbm.tileentity.network.{TileEntityDroneDock,TileEntityDroneProvider,TileEntityDroneRequester}`,
  `IDroneLinkable`, and the 4 `GUIDrone*`/`ContainerDrone*` pairs** — the block/GUI half of the
  logistics-drone network. Already explicitly recommended as its own dedicated Phase 2 package by
  `docs/phase2/items_tool_machine_coupling_and_recipe_system.md`, and still not written as of this
  report (confirmed by a `docs/phase2/` directory listing). This report supplies only the entity
  **movement** half (`EntityDroneBase`/`EntityDeliveryDrone`/`EntityRequestDrone`); the block-network
  half remains that still-open Phase 2 package's job, and `EntityRequestDrone`'s program-execution
  logic (which directly downcasts to those tile-entity types) cannot be wired end-to-end until it
  lands — flagged so this report's entity-movement work isn't mistaken for the whole subsystem.
- **`EntityBombletZeta`, `EntityBoxcar`** (`EntityBomber`'s two submunition entity types) and
  **`ExplosionChaos.spawnChlorine`** (`EntityBomber`'s type-3/7 direct chlorine-gas call) — none read
  in this survey; all are Phase 3/4 explosion-engine/projectile content, not vehicle content.
  `ExplosionChaos` is separately already confirmed **unowned by any Phase 1-3 package** per this
  task's own framing (needed independently by `ItemDrop`'s singularity/xen items) — this report adds
  one more confirmed real caller (`EntityBomber`) to that class's list of dependents, without
  resolving it.
- **`com.hbm.items.tool.ItemBombCaller`** (`EntityBomber`'s sole spawn-trigger item) — not read in
  this survey, not yet ported. A straightforward "use item, call in an airstrike at this location"
  item; needed before `EntityBomber` has any live in-game spawn path, but not itself vehicle content.
- **`com.hbm.itempool.ItemPoolsC130`** (59 lines; header + `POOL_SUPPLIES` read in full,
  `POOL_WEAPONS`/`POOL_AMMO` signature-confirmed only) — the loot table `EntityC130` needs. The
  `ItemPool` **framework** itself (the registry, `getStack`, the pool-entry POJO shape) is already
  fully researched and scoped by `docs/phase4/satellites_followup_and_loot_pools.md` (read for
  cross-reference), which explicitly named `ItemPoolsC130` as one of the "6 sibling pool files" it
  did not itself port, deferring each to "whichever report needs them." This report is that report,
  for this one file — recommend porting `ItemPoolsC130` on top of that report's already-scoped
  `ItemPool` base once `EntityC130` itself is implemented, rather than re-deriving the pool framework
  here.
- **`CompatibilityConfig.isWarDim`** — CE's per-dimension "is this the war dimension" gate on every
  `EntityBomber` bomb-drop. This port's own `config/CompatibilityConfig.java` has *already, explicitly*
  decided not to port any of CE's ~60 dimension-ID-keyed tables (including this one), documenting the
  reason in full (1.12 numeric dimension IDs have no 1.21 equivalent) and naming "whichever phase owns
  world generation" as the eventual owner if dimension-keyed behavior is ever reintroduced. This
  report does not resolve that gap — see Key design decisions for the "drop the gate, default
  always-true" pattern this port's own Phase 3 explosion code already established for the identical
  situation, which `EntityBomber` should most likely follow too.
- **`docs/phase3/turret_system.md`'s own Deferred item #7** (`IRadarDetectableNT`-gated turret target
  visibility) and its `TurretBaseBlockEntity.java` TODO naming `EntityBomber`/`EntityMissileBaseNT`/
  `EntityMissileCustom` as machine-class targets turrets should recognize once those entity classes
  exist — a one-line follow-up (`implements IRadarDetectableNT` on the relevant classes, or adding an
  `instanceof EntityBomber` branch matching the missile classes' pattern) for whoever wires up turret
  targeting once this report's aircraft land, not resolved here.
- **`EntityMinecartTest`** (`extends EntityMinecartTNT`, 26 lines, read in full) — registered
  (`@AutoRegister`) but confirmed by repo-wide grep to have **zero other references anywhere in CE**
  (never constructed, never spawned by any block/item/structure). Genuinely dead content; safe to
  skip porting entirely rather than treating it as a missed vehicle.
- **`com.hbm.entity.mob.IFlyingCreature`** (10 lines, read in full) and its one implementer,
  `EntityPigeon` — a walking/flying animation-state toggle for an ambient decorative bird, not a
  vehicle. Confirmed out of scope; belongs with whichever area covers ambient/decorative mobs.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own already-committed code
and Neo Edition's parallel `PlaneBase`/`Bomber` port for NeoForge 1.21.1 API shape — no NeoForge API
below is invented):

- **`IChunkLoader`/`ServerLevel#setChunkForced` is already real, already-committed, and independently
  confirmed twice over.** This port's own `com.hbm.entity.logic.IChunkLoader` (read in full) already
  re-expresses CE's `ForgeChunkManager.Ticket` API as `ServerLevel#setChunkForced(int,int,boolean)`
  called from `onAddedToLevel`/`onRemovedFromLevel`/`updateChunkTicket` default methods, already used
  by this port's own `EntityExplosionChunkloading` (the nuke/balefire chunk-loading base, read in
  full). **Neo Edition's own independently-written `PlaneBase.java` confirms the identical shape**
  (`onAddedToLevel`/`onRemovedFromLevel` overrides calling `serverLevel.setChunkForced(...)` directly,
  plus an inline `setChunkForced` swap on every chunk-boundary crossing inside `tick()`) — two
  independent ports of two different CE subsystems converged on the same real API. This report's
  `EntityPlaneBase`/`EntityBomber`/`EntityC130` (and `EntityDeliveryDrone`, which also chunk-loads)
  should implement this port's own `IChunkLoader` interface directly rather than re-deriving chunk-
  loading from scratch — this is a solved dependency, not an open one.
- **Neo Edition has already ported `EntityPlaneBase`→`PlaneBase` and `EntityBomber`→`Bomber` in
  full**, confirming several other real 1.21.1 API shapes at once: `Entity#lerpTo(x,y,z,yRot,xRot,
  steps)`/`Entity#lerpMotion(x,y,z)`/`Entity#lerpTargetX()`/`lerpTargetY()`/`lerpTargetZ()`/
  `lerpTargetXRot()`/`lerpTargetYRot()` are the confirmed real replacements for CE's hand-rolled
  `setPositionAndRotationDirect`/`setVelocity` client-interpolation fields (`turnProgress`/`syncPosX/
  Y/Z`/`syncYaw/Pitch`) — **this exact pattern recurs 4 times in this report's own CE source**
  (`EntityPlaneBase`, `EntityRailCarBase`, `EntityRailCarRidable.SeatDummyEntity`,
  `EntityDroneBase`), so all four should be re-expressed on vanilla `Entity`'s own lerp-target
  methods rather than 4 independent hand-rolled interpolation reimplementations. `Entity#tick()`
  replaces `onUpdate()`; `LivingEntity`/`Entity#hurt(DamageSource,float)` replaces
  `attackEntityFrom`; `SynchedEntityData.Builder`/`defineSynchedData` replaces `entityInit`/
  `EntityDataManager.createKey`; `readAdditionalSaveData`/`addAdditionalSaveData` replace
  `readEntityFromNBT`/`writeEntityToNBT`. None of this is invented — it is read directly from Neo
  Edition's own compiling port of the exact same two classes this report covers.
- **Neo Edition's `Bomber` port is itself incomplete (3 of CE's 8 `statFac*` variants only:
  Carpet/Napalm/ABomb) — do not treat it as a finished reference for content, only for API shape.**
  Confirmed by direct comparison against CE's `EntityBomber` (8 variants, read in full above): Neo
  Edition is missing Chlorine/Orange/Stinger/Boxcar/PC, consistent with `EntityBoxcar`/
  `ExplosionChaos.spawnChlorine` being unported dependencies on *both* sides of this port effort, not
  a gap unique to this repo. Per this task's ground rules, do not port Neo Edition's subset as if it
  were CE's complete behavior — this report's own 8-variant table above is the source of truth.
- **`EntityFlying`'s 1.21.1 rename and `travel()` shape is well-established Mojang-mapping knowledge,
  not verified against a compiled jar or Neo Edition in this survey** (Neo Edition has ported no
  flying mob at all — its only aircraft, `PlaneBase`, extends bare `Entity`, matching CE). 1.12's
  `net.minecraft.entity.EntityFlying` (used by CE's `EntityUFO`/`EntityHunterChopper`/`EntityUFOBase`)
  is expected to correspond to 1.21.1's `net.minecraft.world.entity.FlyingMob` (used by vanilla's own
  `Ghast`/`Phantom`), whose `travel(Vec3)` override is expected to apply `getDeltaMovement()` directly
  to position each tick with a friction/drag factor and no gravity — the same "motion field is
  velocity, travel() just applies drag+moves" contract CE's `EntityFlying` already provides. **Flag
  this explicitly as unconfirmed** rather than asserting it: whoever implements UFO/Chopper/UFOBase
  should do a throwaway compile-check against a real dependency jar before assuming this shape,
  exactly as `docs/phase4/entities_bosses.md` already flagged for its own (separately unconfirmed)
  boss-bar API guess.
- **`EntityRailCarRidable`'s player-driven throttle (`getControllingPassenger().moveForward`) maps
  onto 1.21.1's renamed `Player#zza` (or the modern `Input`-record-based forward-axis accessor)** —
  well-established Mojang-mapping knowledge for how vanilla boats/horses already read rider input,
  not verified against a jar in this sandbox (no reference-repo boat/horse-analogue port exists to
  cross-check). `Entity#getControllingPassenger()`/`getPassengers()`/`startRiding(Entity)` themselves
  are unchanged, stable vanilla API across versions and need no reinterpretation — only the specific
  "read the rider's forward-input axis" accessor name needs confirming at implementation time.
- **`MenuBase<T extends MachineBaseBlockEntity>` (this port's own already-committed menu framework,
  read in full) cannot back any of this report's cargo-vehicle GUIs as-is.** `TrainCargoTram`,
  `TrainCargoTramTrailer`, `EntityMinecartCrate`, and `EntityMinecartDestroyer` are all
  `IInventory`-implementing **entities**, not `BlockEntity`s, but `MenuBase`'s generic bound and its
  `isUseableByPlayer` plumbing are both hard-wired to `MachineBaseBlockEntity`. This is a real,
  previously-unflagged gap: whoever implements this report's cargo-vehicle GUIs needs either (a) a
  small parallel `Entity`-backed `AbstractContainerMenu` base (vanilla precedent: chested-horse-style
  inventories opened via `player.openMenu(MenuProvider)` where the `MenuProvider` is the entity
  itself — well-established Mojang-mapping knowledge, not verified against a jar here), or (b)
  generalizing `MenuBase`'s bound to a shared `Container`+"is usable by player" interface both
  `MachineBaseBlockEntity` and these entities could implement. Not resolved here — flagged as a real
  design decision blocking 4 of this report's GUIs, not a simple reuse.
- **The repeated `IInventory` NBT-list boilerplate** (`EntityRailCarCargo`, `EntityMinecartContainerBase`,
  and `EntityDeliveryDrone`'s `ItemStackHandler` variant all independently reimplement the same
  "slot array + read/write NBT list" pattern CE itself duplicates 3 times) is a good candidate for one
  shared helper/base in the port, matching the general "collapse CE's copy-pasted boilerplate, don't
  preserve the duplication" instinct both prior Phase 2/3 reports already applied to their own
  domains — flagged as a recommendation, not a behavior change.
- **`BlockSupplyCrate.SupplyCrateBlockEntity` (this port's own already-real, already-ported block
  entity, confirmed by direct read) is a zero-blocker dependency for `EntityParachuteCrate`** — its
  `items` field and NBT round-trip already match exactly what CE's `EntityParachuteCrate.onUpdate()`
  needs to hand off on landing. No new API surface needed here at all.

## Open questions / risks

- **`LogicalTrainUnit`'s multi-car consist math (`moveTrainByApproach`/`moveWagonTo`/`collideTrain`/
  `combineWagons`) is the single largest pure-logic surface in this report** (~250 of
  `EntityRailCarBase`'s 782 lines) and was read in full but not exhaustively hand-traced against edge
  cases (a 3+ car train reversing direction mid-motion, two independently-moving single-car "trains"
  colliding while one is mid-derail, etc.). Recommend the same "isolate as pure functions, write unit
  tests" treatment PORT_SPEC's own risk note asked for RBMK's neutron math and the gun framework's
  spread/damage formulas — this is arguably a third, equally deserving candidate that neither of
  those reports' own scope covered.
- **Whether `entity/train` (this report) + `blocks/rail` + `tileentity/rail` (Phase 1/2-flagged,
  still unresolved) should ship as one combined work package or be split across two phases** is
  exactly the explicit call `docs/phase1/blocks_network_rail.md` asked planners to make rather than
  silently default — this report does not make that call either, only reconfirms the gap still exists
  and supplies the entity-side research that package will need either way.
- **The 4 duplicated `isCourseTraversable`-shaped AABB-sweep helpers** (`EntityUFO`,
  `EntityHunterChopper`, `EntityUFOBase`, and implicitly `EntityRailCarBase`'s very differently-shaped
  but conceptually similar rail-traversability check) were confirmed near-identical by direct
  comparison but not verified byte-for-byte identical in every numeric edge case — collapsing them
  into one shared helper (recommended, Headline finding #4) should be done carefully enough to
  preserve each caller's exact behavior, not assumed safe by inspection alone.
- **`EntityRailCarRidable.getNearestSeat`'s "180" distance cutoff is unit-ambiguous** — read directly
  from CE's source as a bare `if(nearestDist > 180) return -2` comparison against a `Vec3d.length()`
  value, which reads as blocks but is unusually large for a seat-picking interaction range; not
  cross-checked against any other CE seat-interaction code in this survey. Flag for confirmation
  before assuming "any click within 180 blocks of the car" is the intended behavior versus a typo'd
  constant.
- **`EntityDeliveryDrone`'s look-ahead chunk-force-loading (current chunk + a heading-projected
  one-chunk lookahead, both as full 3×3 neighborhoods) is meaningfully more aggressive than every
  other chunk-loading entity in this survey** (which force-load only their own current chunk) — worth
  a deliberate decision (preserve exactly vs. simplify to match this report's other chunk-loaders) at
  implementation time rather than assumed to need CE's exact footprint, given `docs/phase4/
  chunk_radiation_system.md`/other Phase 4 areas' own general chunk-load-cost sensitivity.
- **CE's own `@Spaghetti("onUpdate needs to be cleaned up")` annotation on `EntityRequestDrone`** and
  the top-of-file "Drillgon200: this whole thing is messed up and janky" comment
  `docs/phase4/entities_bosses.md` already quoted for `EntityHunterChopper` are two separate CE
  maintainer self-assessments of fragile code in this same overall survey — both should get extra
  test coverage relative to the rest of this report's content, not treated as equally battle-tested
  as e.g. the minecart family's trivial vanilla wrappers.
- **`EntityBomber`'s `CompatibilityConfig.isWarDim` gate**: this report recommends following this
  port's own established "drop the gate, default to always-true" precedent (`ExplosionLarge`,
  `ExplosionNukeAdvanced`, `EntityCloudFleija` all already do this for the identical CE mechanic per
  a repo-wide grep), but that is a recommendation, not a decision already made for this specific
  class — confirm explicitly rather than assuming silently, since an always-true bomber changes
  observable behavior (bombs always drop) versus CE's dimension-gated original.
