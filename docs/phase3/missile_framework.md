# Missile item/entity framework and warhead system — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/missile/{EntityMissileBaseNT,EntityMissileCustom,
  EntityMissileAntiBallistic,EntityMissileTier0}.java` (420+320+301+171 lines) and `EntityMIRV.java`
  (229 lines, read through the constructor/`onUpdate`/`killMissile` — the `mirvSplit`-relevant half;
  the render/`onImpact` tail not needed for warhead-dispatch purposes)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/{ItemMissile,ItemMissileStandard,
  ItemCustomMissile}.java` (416+104+119 lines) and `com.hbm.handler.MissileStruct` (172 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/explosion/ExplosionLarge.java` (279 lines) — the one
  explosion-engine file this package calls into directly; read in full to document the exact call
  surface, not to re-scope the engine itself
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityMachineMissileAssembly.java`
  (304 lines) — the real player-facing machine that turns 5 `mp_*` parts into one `missile_custom`
  stack
- `upstream/hbm-ce/src/main/java/com/hbm/creativetabs/MissileTab.java` (full — the 9 showcase builds)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityThrowableInterp.java` (84 lines, the
  CE base `EntityMissileBaseNT` extends) and `com.hbm.tileentity.bomb/TileEntityLaunchTable.java`,
  `TileEntityCompactLauncher.java` (grep-targeted sections only — the two real-world missile-spawn
  call sites and the chip-inaccuracy consumer)
- This port's own `com.hbm.packet.HbmNetwork`, `com.hbm.packet.toclient.BufPacket`,
  `com.hbm.damage.ModDamageTypes`, `com.hbm.inventory.container.MenuBase`,
  `com.hbm.inventory.gui.GuiInfoContainer`, `com.hbm.entity.{ConveyorEntityTypes,
  item.EntityMovingItem, item.EntityMovingConveyorObject}`, plus a grep survey of
  `com.hbm.blockentity.MachineBaseBlockEntity` (`isItemValidForSlot`/`getCheckedInventory`) — all
  read to confirm real, already-shipped API shapes before proposing anything for this package.
- `docs/phase1/{moditems_generative.md §4, creative_tabs_plan.md, items_tool.md, items_special.md}`,
  `docs/phase2/rbmk_reactor.md` (structural/depth model for this report)
- `upstream/neo-edition/src/main/java/com/hbm/entity/missile/MissileBase.java`,
  `com.hbm.items.weapon.MissileItem.java`, `com.hbm.entity.projectile.{ProjectileNT,
  ProjectileLerping}.java`, `com.hbm.entity.logic.IChunkLoader.java` — read in full, for confirmed
  1.21.1 NeoForge API shape only, per the task's Neo Edition caveat (never for behavior)
- Repo-wide greps: `mp_*`/`missile_*` field-declaration breakdown in CE's `ModItems.java`,
  `impactCustom=`/`updateCustom=`/`WarheadType.CUSTOM` usage, `isCluster = true` usage, and a
  confirmation that `com.hbm.explosion`/`com.hbm.entity.missile`/`com.hbm.items.weapon` do not exist
  anywhere in this port's `src/` yet

## Headline finding

CE actually ships **two independent, loosely-coupled missile systems**, sharing one abstract
flight-physics base and one explosion-engine call surface, plus a real crafting machine that bridges
them. Both must be scoped, and they are not the same size of problem:

1. **Preset "standard" missiles** — `ItemMissileStandard` (28 registered items, e.g.
   `missile_nuclear`, `missile_micro`, `missile_shuttle`) each correspond 1:1 to one concrete,
   hand-written `EntityMissileTierN`/`EntityMissileAntiBallistic`/`EntityMissileShuttle`/
   `EntityMissileStealth` Java subclass with its `onMissileImpact` behavior **baked directly into
   Java code** — no runtime configurability, no NBT-driven behavior selection. **Neo Edition already
   ported this half once** (`MissileBase`/`MissileItem`/`MissileTier0-4`/`MissileAntiBallistic`/
   `MissileShuttle`/`MissileStealth` all exist and compile against 1.21.1), so this half has a real,
   confirmed API-shape reference to lean on.
2. **Composable "custom" missiles** — `ItemMissile` (the `mp_*` parts: chip/warhead/fuselage/
   fins/thruster), `ItemCustomMissile.buildMissile(...)`, and `EntityMissileCustom` form a genuine
   data-driven system: parts are combined at runtime (by a player, or by
   `TileEntityMachineMissileAssembly`) into one `missile_custom` `ItemStack` whose NBT holds up to 4
   other items' registry IDs, and `EntityMissileCustom.onMissileImpact` dispatches on a `WarheadType`
   enum read back out of that NBT to decide which explosion-engine entry point to call. **Neo Edition
   has zero reference for this half** — confirmed by directory listing: no `MissileCustom`,
   `MissilePart`, or `ItemCustomMissile`-equivalent exists anywhere in `upstream/neo-edition`. This
   half is real, uncharted 1.21.1-API-design work, not a shape-confirmation exercise.

**Phase 1's mp_*/missile_* counts need a correction, found while verifying them against CE source
directly** (not a criticism of that survey — it was a high-level pass that correctly flagged the area
for a later, deeper look, which is what this report is):

- **`mp_*` is 125 registered items, not 64.** `docs/phase1/moditems_generative.md`'s grep matched
  only direct `new ItemMissile("mp_...")` constructions (64 of those — this is where "64" came from,
  and it correctly counts every *structurally distinct* part). The other **61 are `.copy(name)`
  clones** — e.g. `mp_fuselage_10_kerosene_camo = ((ItemMissile) mp_fuselage_10_kerosene)
  .copy("mp_fuselage_10_kerosene_camo").setRarity(...).setTitle("Camo")` — pure cosmetic reskins
  (same `PartType`/`attributes`/`health`/`mass`, different `title`/`author`/`witty`/`rarity` flavor
  fields) that exist to populate `ItemLootCrate.list10`/`list15`/`listMisc`'s rarity-weighted roll
  tables (the exact mechanism `docs/phase1/items_special.md` flagged as Phase-3-blocked). Real
  breakdown by `PartType`, counting every field (both `new ItemMissile(...)` and `.copy(...)`):
  **5 chip, 19 warhead, 74 fuselage (13 base + 61 cosmetic clones, overwhelmingly on fuselages),
  7 stability, 20 thruster.**
- **`missile_*` is exactly 42 as stated**, but only **28** are real launchable `ItemMissileStandard`
  presets. The other 14: `missile_custom` (1, the composable item itself), `missile_assembly` (1, a
  plain `ItemBase` — the *block-placer item* for `TileEntityMachineMissileAssembly`, not a missile at
  all, tabbed under `partsTab` not `missileTab`), `missile_kit` (1, an `ItemStarterKit` giveaway),
  `missile_soyuz` + `missile_soyuz_lander` (2, `ItemSoyuz`/`ItemCustomLore` — space-capsule content, a
  different subsystem entirely), and **10 `missile_skin_*` `ItemCustomLore` items** (cosmetic lore
  items, not flyable missiles). Worth knowing precisely before Phase 3 item-count/registry planning
  treats "42" as "42 missiles."

## Phase-3-safe scope

All line counts are from the CE files actually read in full.

| Class | Lines | What it is |
|---|---|---|
| `ItemMissile` | 416 | The `mp_*` part factory: `PartType` enum (CHIP/WARHEAD/FUSELAGE/FINS/THRUSTER), `PartSize` enum (physical socket size, `NONE`/`ANY`/`SIZE_10`/`15`/`20`/`25`/`30` — `25`/`30` are space-grade, unused by any real item in this survey), `WarheadType` enum (30 values, `HE`/`INC`/`BUSTER`/`CLUSTER`/`NUCLEAR`/`TX`/`N2`/`BALEFIRE`/`SCHRAB`/`TAINT`/`CLOUD`/`VOLCANO`/`TURBINE`/`MIRV`/`APOLLO`/`SATELLITE`/`CUSTOM0-9`), `FuelType` enum (9 values), `Rarity` enum (6, one is a joke value), and the 5 `make{Chip,Warhead,Fuselage,Stability,Thruster}(...)` builder methods each concrete `mp_*` item calls once at registration. `copy(String)` is the cosmetic-clone mechanism behind the 61 extra fuselage variants above. |
| `ItemMissileStandard` | 104 | The 28-preset item class: `MissileFormFactor` (7 values, each pins a default `MissileFuel`), `MissileTier` (5, TIER0-4), `MissileFuel` (5, display text + a `defaultCap` in mB). Simple tooltip-only item, no NBT of its own beyond `notLaunchable()`'s static field. |
| `ItemCustomMissile` | 119 | `buildMissile(ItemStack×5 → ItemStack)` (writes 4 registry-int NBT keys: `chip`/`warhead`/`fuselage`/`thruster`, plus `stability` only if non-null), `getStruct(ItemStack) → MissileStruct` (re-reads 3 of those 4 back into a transient struct — **does not include `chip`**, see Open questions), and a tooltip renderer that recomputes total HP/inaccuracy from the 5 parts on the fly. |
| `MissileStruct` | 172 | Transient (warhead, fuselage, fins, thruster) holder — **no chip field at all**. Carries its own Forge-1.12 `DataSerializer<MissileStruct>` (for `EntityDataManager` sync) and a `ByteBuf` (de)serialization pair (for `TEMissileMultipartPacket`, the assembly machine's live-preview broadcast). Both need a StreamCodec-based redesign (see Key design/API decisions). |
| `EntityMissileBaseNT` | 420 | Abstract flight-physics base for **every** missile entity except `EntityMissileAntiBallistic`/`EntityMIRV` (which duplicate a near-identical chunk-loader/interp scaffold independently — see Open questions). Owns: target-seeking XZ acceleration + Y deceleration toward a fixed `(targetX, targetZ)` set once at spawn (`accelXZ`/`decelY` are both `1/distance`, `decelY` doubled — a simple parabolic-arc approximation, not real ballistics); a `health` field decremented by `attackEntityFrom` (any damage source) that triggers `killMissile()` at 0; per-tick contrail particle spawning, rotation-facing math, and `ForgeChunkManager`-ticket-based chunk loading (1 chunk, refreshed every tick as the missile moves) so the missile doesn't despawn/desync crossing unloaded chunks. `explodeStandard(strength, resolution, fire)` is a convenience wrapper around the (deferred) `ExplosionVNT` engine, used by at least one Tier variant. |
| `EntityMissileCustom` | 320 | The composable-missile entity. 5 `DataParameter<Integer>` fields (HEALTH/WARHEAD/FUSELAGE/FINS/THRUSTER, all raw `Item.getIdFromItem` registry ints) synced client-side for rendering. `onUpdate()` reads the warhead's `WarheadType.updateCustom` hook every tick (only `MIRV` uses this, calling `EntityMissileCustom::mirvSplit`) and burns `consumption` fuel per tick (`hasPropulsion() = fuel > 0`, matching the base class's ballistic-if-no-fuel fallback). `onMissileImpact` is a 12-arm `switch(WarheadType)` that is the actual warhead→explosion-engine dispatch table (see Key design/API decisions for the exact call list). `spawnContrail` picks a different particle helper per `FuelType`. `mirvSplit()` spawns 7 `EntityMIRV` sub-munitions in a fixed hardcoded XZ dispersion pattern (`MIRV_OFF_X`/`MIRV_OFF_Z`, "1.12.2 exclusive thanks to seven" per CE's own comment) when descending (`motionY < -1D`). |
| `EntityMissileAntiBallistic` | 301 | Point-defense interceptor — **not** an `EntityMissileBaseNT` subclass. Independent predictive-targeting logic (`aimAtTarget`'s lead-prediction: `intercept = distance / (baseSpeed * velocity)`, extrapolates the target's last-tick delta by that many ticks), a 10-tick activation delay before it starts homing, a proximity-fuse detonation (`distance < 15` → area-damage every nearby `EntityMissileBaseNT` for 51 HP, i.e. instantly overkills the default-50-HP base class, then self-destructs) *and* a second, tighter proximity check inside `aimAtTarget` (`distance < 10 && activationTimer >= 40` → also self-destructs) — two independent kill conditions coexist, worth preserving both exactly rather than assuming they're redundant. Duplicates its own copy of the `ForgeChunkManager` ticket scaffold (3×3 chunk area, not 1 chunk like the base class) rather than sharing `EntityMissileBaseNT`'s. |
| `EntityMissileTier0` (+ 5 nested static subclasses, read in full) | 171 | `EntityMissileTest` (mass world-scale radiation-charring effect over a 50-block radius, meta-varying `sellafield_slaked` — **has no matching `ItemMissileStandard` field at all**, a leftover/dev-only entity, flag before assuming a 1:1 item↔entity mapping holds everywhere), `EntityMissileMicro`, `EntityMissileSchrabidium`, `EntityMissileBHole`, `EntityMissileTaint`, `EntityMissileEMP` — each with its own hardcoded `onMissileImpact`. |
| `EntityMissileTier1`/`2`/`3`/`4` | 90/104/112/156 | Line-counted and grep-spot-checked (not read in full) — confirmed at least `EntityMissileCluster`/`EntityMissileClusterStrong`/`EntityMissileRain` set `isCluster = true` in their constructor and override `cluster()` to re-invoke `onMissileImpact(null)` early (mid-flight, when descending past `motionY < -1.5`), calling `ExplosionChaos.cluster(...)` — the only real users of `EntityMissileBaseNT.isCluster`/`cluster()` anywhere in this survey. A full per-class accounting of all ~20+ remaining nested subclasses across these 4 files was not done here — first implementation-time task for whoever picks up this file, not assumed to be a clean "28 items = 28 entity classes" mapping (Tier0 alone already breaks that assumption by 1). |
| `EntityMIRV` | 229 (read through the MIRV-relevant half) | The cluster sub-munition `EntityMissileCustom.mirvSplit()` spawns. Own 25 HP pool, own `killMissile()` calling the same `ExplosionLarge.explode`/`spawnShrapnelShower` pair as the parent class. Extends vanilla `EntityThrowable`, not `EntityMissileBaseNT` — ballistic free-fall only, no target-seeking of its own (it inherits the parent missile's velocity at split time and free-falls from there). |
| `TileEntityMachineMissileAssembly` | 304 | The real player-facing machine (`missile_assembly` block/item) that calls `ItemCustomMissile.buildMissile(...)`. 6-slot `ItemStackHandler` (chip/warhead/fuselage/fins/thruster/output), a redstone-pulsed auto-craft (`update()`: every 20 ticks while powered, calls `construct()` if `canBuild()`), and 5 `xxxState()` validator methods that cross-check part compatibility (`fuselage.top == thruster's expected size`, `warhead.weight <= thruster.thrust`, `fins.top == fuselage.bottom`, etc.) before allowing assembly — this compatibility-checking logic is exactly what a ported `MissileAssemblyBlockEntity.isItemValidForSlot`/`canBuild` pair needs to reproduce. Broadcasts a `TEMissileMultipartPacket` (the loaded-parts preview, for in-progress render) to nearby players every tick via `PacketDispatcher`/`PacketThreading`. |
| `ExplosionLarge` | 279 | Read in full as **the exact integration surface** warhead dispatch calls into (see Key design/API decisions) — not re-scoped as the explosion engine itself. |
| `MissileTab` (9 showcase stacks) | — | Confirmed exact part combinations for "Lil Bub"/"Long Boy"/"Uncle Kim"/"Trotty's Toy Rocket"/"Stealthy Shark"/"Polite Lad"/"NERV's Leftover Missile"/"7 For 1 Package Deal"/"Hightower Missile" — already documented once by `docs/phase1/creative_tabs_plan.md`, re-confirmed here directly against `MissileTab.java` (one, "Hightower Missile", passes `null` for `stability`, exercising `ItemCustomMissile.buildMissile`'s null-stability overload — worth a unit test). |

**Real WarheadType usage, confirmed by grep** (of the 30 enum values, only these appear on any
actually-registered `mp_warhead_*` item — the rest, `SCHRAB`/`APOLLO`/`SATELLITE`/`CUSTOM0-9`, are
either unused by the composable system or belong to unrelated content):
`NUCLEAR` ×3, `TX` ×2, `INC` ×2, `HE` ×2, `VOLCANO` ×1, `TURBINE` ×1, `TAINT` ×1, `N2` ×1, `MIRV` ×1,
`CLOUD` ×1, `BUSTER` ×1, `BALEFIRE` ×1 (17 items across 12 distinct types). `impactCustom`/
`updateCustom`/`labelCustom` hooks are wired **only** on `MIRV` anywhere in CE (confirmed by
repo-wide grep) — the "pluggable custom warhead behavior" mechanism the enum constructor supports is
real but has exactly one live consumer; don't over-build for hypothetical future users of it.

## Deferred scope

- **The explosion engine itself** (`com.hbm.explosion`, 46 files: `ExplosionVNT` + its
  `BlockAllocator*`/`BlockProcessor*`/`BlockMutator*`/`EntityProcessor*`/`PlayerProcessor*`
  strategy-object family, the full nuke chain `ExplosionNukeGeneric`/`ExplosionNukeSmall`/
  `ExplosionNukeAdvanced`/`ExplosionNukeRayBatched`/`ExplosionNukeRayParallelized`, `ExplosionChaos`,
  `ExplosionBalefire`/`ExplosionFleija`/`ExplosionSolinium`/`ExplosionThermo`/`ExplosionTom`/
  `ExplosionDrying`, plus `EntityNukeExplosionMK5`/`EntityNukeTorex`/`EntityBalefire` under
  `com.hbm.entity.logic`/`com.hbm.entity.effect`). **Confirmed absent from this port's `src/`
  entirely** (`com.hbm.explosion` does not exist yet). This report documents only the exact call
  surface warhead dispatch uses (below); the batched-block-removal performance work the ground rules
  flag as mandatory (not optional) belongs entirely to whichever package ports this engine, not to
  the missile/warhead package — this package's own code (confirmed by reading `ExplosionLarge.java`
  in full) does no bulk per-block looping itself, it only calls into vanilla `Level#createExplosion`
  or hands off to the (deferred) engine.
- **Launch infrastructure**: `TileEntityLaunchPad`/`TileEntityLaunchTable`/`TileEntityCompactLauncher`
  (the actual world-placed blocks that call `new EntityMissileCustom(...)` and `world.spawnEntity(...)`
  — confirmed the *only* two call sites anywhere in CE that spawn `EntityMissileCustom`), their
  `LaunchPad` block, and their GUIs/Containers (`GUIMachineLaunchTable`/`GUICompactLauncher`, not
  read). These need liquid-fuel tanks (`solidState()`/`liquidState()`/`oxidizerState()` — a
  `fluidmk2`/tank-capability prerequisite not yet in this port beyond the one already-ported
  `IFluidRegisterListener` interface) and redstone/designator-target coordinate input. Without this
  package, `EntityMissileCustom`/`ItemCustomMissile` are fully portable and testable in isolation
  (spawn one programmatically, verify flight/impact), just not launchable by a player from a block —
  a reasonable Phase-3-internal sequencing split.
- **Designator/targeting items** (`ItemDesignator`, `ItemDesignatorRange`, `ItemDesignatorArtyRange`,
  `ItemSatDesignator`) — already flagged Phase 3 by `docs/phase1/items_tool.md`'s bucket (b); not
  re-derived here, they feed target coordinates into the launch infrastructure above, not into this
  package directly.
- **`com.hbm.entity.projectile.{EntityThrowableNT, EntityThrowableInterp}` chain** (CE's own base
  classes) / **`ProjectileNT`/`ProjectileLerping`** (Neo Edition's confirmed-real 1.21.1
  replacements, read in full above) — a shared "projectile framework" prerequisite used by bullets,
  grenades, *and* missiles alike, not itself missile-specific content. **Confirmed absent from this
  port** (no `com.hbm.entity.projectile` package exists in `src/` yet). Whoever owns the bullet/gun
  package and whoever implements missiles both need this to exist first; recommend it lands once,
  shared, rather than each area hand-rolling its own copy of `lerpTo`/`lerpTargetX/Y/Z` interpolation
  and the vanilla-`ClipContext`-based hit-scan `tick()` loop `ProjectileNT` provides.
- **`com.hbm.entity.logic.IChunkLoader`** — small (52 lines in Neo Edition's confirmed-real version),
  self-contained, `TicketType<UUID>`-based (see Key design/API decisions). Confirmed absent from this
  port. Recommend porting this alongside the missile entities specifically (every single missile
  entity variant surveyed here needs it), even though it is technically a shared
  `com.hbm.entity.logic` utility rather than missile-package-exclusive content — small enough that
  waiting on a separate "entity logic utilities" package to land first would only add friction.
- **`EntitySoyuz`/`EntitySoyuzCapsule`** (208+109 lines, not read), **`EntityBobmazon`** (85 lines),
  **`EntityMinerRocket`** (111 lines), **`EntityBombletSelena`** (93 lines) — all physically live in
  `com.hbm.entity.missile` alongside the real missile classes but are thematically distinct
  (space-capsule reentry, a novelty delivery gimmick, a mining rocket, a cluster bomblet). None were
  read in this survey. Flag for their own scoping pass rather than assuming they belong to "the
  missile package" by directory location alone — `EntitySoyuz`/`EntitySoyuzCapsule` in particular look
  like they belong with whichever future package covers space/orbital content (matches
  `missile_soyuz`/`missile_soyuz_lander`'s classification above as non-missile content).
- **`EntityMissileTier1-4`'s remaining ~20+ nested subclasses**, individually — only Tier0's 5 were
  read in full; the other four files were line-counted and spot-grepped only (confirmed the
  `isCluster`/`cluster()` pattern above, nothing else). A full read is a first implementation-time
  task, not assumed complete by this survey.
- **Rendering**: `com.hbm.render.entity.missile.RenderMissileXxx` (client-only, ~9 files observed by
  directory listing during this survey) and whatever dynamic-model mechanism lets `missile_custom`'s
  in-hand/in-world render vary by its 4 loaded parts (CE-era `IDynamicModels`/baked-model swapping,
  per `docs/phase1/items_tool.md`'s note on the same mechanism used by `ItemModMinecart`). Phase 5
  per every other Phase 1/2 report's convention for render work.
- **Hazard bindings**: confirmed **not needed** — grepped `com.hbm.hazard`/`HazardRegistry` usage
  against every `mp_*`/`missile_*` item and found no bindings; missile parts carry no radiation/heat
  hazard of their own (the *warhead effects* are all delivered via the explosion engine's own,
  separately-scoped radiation/contamination systems, not via `HazardSystem` item bindings).

## Key design/API decisions

Confirmed from real code — CE for behavior, this port's own already-shipped code and Neo Edition
(cross-checked, never invented) for NeoForge 1.21.1 API shape:

- **Entity registration**: `DeferredRegister<EntityType<?>>` + `EntityType.Builder.<T>of(ctor,
  MobCategory.MISC).noSummon().sized(w, h).setTrackingRange(n).build(name)` is the confirmed live
  pattern in this port already (`com.hbm.entity.ConveyorEntityTypes`, Phase 2) and matches CE's own
  per-entity `@AutoRegister(name, trackingRange)` values 1:1 (e.g. `EntityMissileCustom`'s
  `trackingRange = 1000`). Every concrete missile entity (custom + every preset `EntityMissileTierN`
  nested subclass + `EntityMissileAntiBallistic` + `EntityMIRV`) needs its own registration this way —
  CE's own one-Java-class-per-missile-type structure means there is no natural single parametrized
  entity type to collapse them into without a larger redesign; recommend preserving CE's one-class,
  one-`EntityType` shape for parity rather than inventing a new "generic missile entity + payload
  enum" abstraction CE itself doesn't have.
- **Synced fields**: `EntityDataAccessor<T>` via `SynchedEntityData.defineId(EntityMissileCustom.class,
  EntityDataSerializers.X)` replaces CE's `DataParameter<T>`/`EntityDataManager` pair — confirmed
  live in this port (`EntityMovingItem.java`, Phase 2) and in Neo Edition's `MissileBase.ROT` field.
  **`EntityMissileCustom`'s 4 raw-registry-int fields (WARHEAD/FUSELAGE/FINS/THRUSTER) cannot survive
  a literal port** — `Item.getIdFromItem`/`Item.getItemById` (numeric, session-stable-only Forge-1.12
  registry ids) do not exist in 1.21.1 at all. Every read site (the `WarheadType` switch in
  `onMissileImpact`, the `FuelType` switch in `spawnContrail`, the `PartSize` checks in
  `getTranslationKey`/`getBlipLevel`) needs to be re-derived around whatever replacement sync value is
  chosen — recommend syncing a `Holder<Item>` (via `EntityDataSerializers`... note: vanilla ships no
  built-in `Holder<Item>` serializer, so this needs either a custom `EntityDataSerializer` registered
  through NeoForge's `EntityDataSerializers`/`NeoForgeStreamCodecs` extension point, or simplifying to
  sync a `ResourceLocation` (`EntityDataSerializers.STRING` + manual parse, or a registered
  `ResourceLocation` serializer if one already exists in this port — not confirmed either way, check
  before assuming) and resolve it against `BuiltInRegistries.ITEM` on read. This is real, sizeable
  redesign work at every call site, not a mechanical field-type swap.
- **Networking (assembly-machine preview + any client-visible custom-missile part data)**:
  `MissileStruct`'s `DataSerializer<MissileStruct>` (Forge-1.12's `EntityDataManager`-sync mechanism)
  and its separate hand-rolled `ByteBuf` (de)serialization (for `TEMissileMultipartPacket`) both need
  replacing with this port's own confirmed, already-shipped shape: a `record` implementing
  `CustomPacketPayload`, a `Type<T>` keyed by `ResourceLocation.fromNamespaceAndPath(MODID, "...")`,
  a `StreamCodec<RegistryFriendlyByteBuf, T>`, and one `registrar.playToClient(...)`/`playToServer(...)`
  line added to `com.hbm.packet.HbmNetwork.registerPackets` — the exact pattern this port's own
  `com.hbm.packet.toclient.BufPacket` (Phase 2's only registered payload so far) already demonstrates
  end to end, including its `IPayloadContext.enqueueWork(...)` main-thread-hop convention. One new
  payload (e.g. a `MissilePartsPacket` carrying warhead/fuselage/fins/thruster as
  `ResourceLocation`s, or 4 raw item ids resolved via `BuiltInRegistries.ITEM.getId(...)`/`byId(...)`
  if a numeric approach is preferred for wire-size) can serve **both**
  `TileEntityMachineMissileAssembly`'s live in-progress-assembly preview broadcast **and**
  `EntityMissileCustom`'s render-time part lookup, since both are "here are 4 Items, render
  accordingly" problems with the same shape.
- **Data Components**: `ItemCustomMissile`'s `chip`/`warhead`/`fuselage`/`thruster`/`stability` NBT
  int keys (raw registry ids, same "does not survive the port" problem as above) become one custom
  `DataComponentType` — a record holding `Holder<Item>` (or plain `Item`/`ResourceLocation`)
  references for warhead/fuselage/thruster + optional fins/chip — registered the way this port's
  other Phase 1/2 NBT→component conversions already have been (not re-derived here; follow whichever
  concrete `DataComponentType.Builder`/`DataComponentType.builder().persistent(codec)` pattern
  Phase 1's item work already established). **`ItemMissile`'s `title`/`author`/`witty`/`rarity` fields
  are NOT NBT and are NOT data-component candidates at all** — confirmed by reading the class: they
  are plain Java fields set once via builder methods (`.setTitle(...)`, etc.) at static-registration
  time, identical in spirit to `Item.Properties()` calls, not per-`ItemStack` state. Port them as
  constructor/builder fields on the ported `Item` subclass, exactly like CE's own shape, with zero
  component involved.
- **GUI**: `TileEntityMachineMissileAssembly`'s 6-slot, per-`PartType`-validated container maps
  directly onto this port's already-shipped `MenuBase<T extends MachineBaseBlockEntity>` +
  `SlotNonRetarded` (`IItemHandler`-backed slots, matching CE's own `ItemStackHandler`-based
  validation, not vanilla `Container`) + `GuiInfoContainer<T>` (confirmed real, read in full — see
  Phase 2's `docs/phase2/gui_framework.md`, not re-derived here) pattern. No new GUI shape is needed:
  a new `MissileAssemblyBlockEntity extends MachineBaseBlockEntity` overriding `isItemValidForSlot`
  to check `ItemMissile.PartType` per slot (chip=0, warhead=1, fuselage=2, fins=3, thruster=4,
  output=5, exactly CE's slot layout) plus a `canBuild()`/`construct()` pair ported nearly verbatim
  from CE's own logic (the 5 `xxxState()` compatibility checks are pure `ItemMissile.attributes[]`
  reads, no NeoForge-specific rework needed there at all) is the whole of the port work for this
  machine's server-side logic; a matching `MissileAssemblyMenu`/`MissileAssemblyScreen` pair follows
  the same shape every other Phase 2 machine GUI already does.
- **Damage**: confirmed **no new `ModDamageTypes` entries needed for this package**. Every missile
  entity's `attackEntityFrom`/`hurt` override (`EntityMissileBaseNT`, `EntityMissileCustom` via
  inheritance, `EntityMIRV`, `EntityMissileAntiBallistic`) accepts incoming damage from *any* source
  to decrement its own `health` field — it does not itself *deal* a missile-specific damage type to
  anything. The `SHRAPNEL`/`RUBBLE`/`BLAST`/`NUCLEAR_BLAST` entries already present in this port's
  `com.hbm.damage.ModDamageTypes` (Phase 0) are attributed by the (deferred) *explosion engine*'s
  entity-processor classes to blast victims, not by this package.
- **Chunk loading — the single most important "don't invent an API" finding here**: CE's
  `net.minecraftforge.common.ForgeChunkManager` (`Ticket`/`requestTicket`/`forceChunk`/
  `unforceChunk`/`releaseTicket`/`bindEntity`) **does not exist in NeoForge 1.21.1 at all**. The
  confirmed real replacement, cross-checked against Neo Edition's own compiling
  `com.hbm.entity.logic.IChunkLoader` (read in full, reproduced below because it is short and
  load-bearing for every missile entity in this package):
  ```java
  public interface IChunkLoader {
      TicketType<UUID> ENTITY = TicketType.create("entity", Comparator.comparing(UUID::toString));
      void setLoadedChunkPos(ChunkPos pos);
      ChunkPos getLoadedChunkPos();
      default void onAddedToLevel(Entity entity) {
          if (entity.level instanceof ServerLevel serverLevel) {
              this.setLoadedChunkPos(new ChunkPos(entity.blockPosition()));
              serverLevel.setChunkForced(getLoadedChunkPos().x, getLoadedChunkPos().z, true);
          }
      }
      default void onRemovedFromLevel(Entity entity) { /* mirror: setChunkForced(..., false) */ }
      default void updateChunkTicket(Entity entity) { /* diff old/new ChunkPos, force new, unforce old */ }
  }
  ```
  wired through `Entity#onAddedToLevel`/`#onRemovedFromLevel`/`#tick()` overrides. This reduces CE's
  two different chunk-loading shapes (`EntityMissileBaseNT`'s single-chunk reload-every-tick, and
  `EntityMissileAntiBallistic`'s independently-duplicated 3×3-chunk-area version) to repeated calls of
  one shared, much simpler primitive — a real simplification opportunity flagged for whoever
  implements this, not just a mechanical translation. (Neo Edition's own file is annotated
  `@Deprecated`, with no comment explaining why and no newer alternative visible anywhere else in that
  reference tree — flagged in Open questions, not treated as disqualifying.)
- **Explosion-engine call surface** (documented as the exact seam this package hands off across, not
  redesigned): `EntityMissileCustom.onMissileImpact`'s `WarheadType` switch calls, verbatim:
  `ExplosionLarge.explode(level, detonator, x, y, z, strength, cloud, rubble, shrapnel)` (HE — wraps
  vanilla `Level#createExplosion` plus particle/rubble/shrapnel spawns, confirmed no bulk block-loop
  of its own), `.explodeFire(...)` (INC), `.buster(...)` (BUSTER), `.jolt(...)` (paired with HE/INC,
  a "war dimension"-gated rubble-entity spawner, not real block destruction), `.spawnShrapnelShower`/
  `.spawnMissileDebris` (universal, on `killMissile()`); and, bypassing `ExplosionLarge` entirely,
  direct `EntityNukeExplosionMK5.statFac(...)`/`.statFacNoRad(...)` + `EntityNukeTorex.statFac(...)`
  spawns for `NUCLEAR`/`TX`/`MIRV`/`N2`, an `EntityBalefire` spawn for `BALEFIRE`, and
  `ExplosionChaos.spawnChlorine(...)` for `CLOUD`. **None of these targets exist in this port yet**
  (confirmed — `com.hbm.explosion` is entirely absent from `src/`). This is precisely the boundary
  PORT_SPEC's "batched LevelChunk section writes + deferred lighting" performance note applies to: the
  missile/warhead package's own responsibility stops at "call the right static method with the right
  arguments," and the actual hundreds/thousands-of-blocks removal work is the deferred explosion
  engine's problem, not this package's — but every `NUCLEAR`/`TX`/`BALEFIRE`/`N2` warhead is a direct,
  unavoidable trigger of exactly that code path, so the two packages' implementation order matters:
  a naive first cut of the explosion engine will be exercised immediately and heavily by even a
  minimal missile-package smoke test.

## Open questions / risks

- **`ProjectileNT`/`ProjectileLerping`/CE's `EntityThrowableNT`/`EntityThrowableInterp` chain is not
  yet scoped or ported by any prior phase.** This is a real blocking dependency for
  `EntityMissileBaseNT` specifically (its Neo Edition equivalent, `MissileBase`, extends
  `ProjectileLerping extends ProjectileNT extends Projectile`) — or a genuine design fork worth
  deciding explicitly rather than defaulting into: extend vanilla `Projectile`/`Entity` directly in
  the missile package itself and hand-roll the ~40 lines of interp/lerp logic CE needs, versus waiting
  on / co-authoring a shared projectile-framework package first. Flagging per the "no silent punting"
  rule — this is the single largest unresolved sequencing question this survey found.
- **CE bug/design-gap: `WarheadType.VOLCANO` is real and registered** (one `mp_warhead_*` item uses
  it) **but `EntityMissileCustom.onMissileImpact`'s switch has no `case VOLCANO`** — it falls through
  to a no-op `default: break;`. A volcano-type custom missile currently does nothing on impact in CE
  itself. Decide explicitly whether to preserve this exact (apparently unintentional) no-op for
  byte-for-byte parity, or treat it as a bug worth fixing during the port — flagged, not resolved
  here, per this project's own stated preference for explicit calls over silent "fixes."
  `WarheadType.CLUSTER` similarly has an explicit-but-still-no-op `case CLUSTER: break;`, and a
  repo-wide grep found `isCluster` is **never** set to `true` anywhere reachable from
  `EntityMissileCustom` (only the unrelated *preset* `EntityMissileCluster`/`ClusterStrong`/`Rain`
  classes ever set it) — meaning the composable system's "cluster warhead" concept is confirmed
  entirely vestigial today, not merely under-documented.
- **`ItemCustomMissile.getStruct(ItemStack)` never reads back the `chip` NBT key that
  `buildMissile` writes** — `MissileStruct` has no `chip` field at all. This is not obviously a bug:
  grepping the one other real consumer, `TileEntityCompactLauncher.launch()`, shows it re-reads
  `chip` directly off the `ItemStack` NBT itself (bypassing `MissileStruct`) to compute a one-time
  random target-offset (`inaccuracy × fin inaccuracy`) applied **only at launch**, never consulted
  again during flight. `TileEntityLaunchTable.launch()`, by contrast, **never reads chip/inaccuracy at
  all** and always launches dead-on-target. This asymmetry (silo = pinpoint always; mobile/compact
  launcher = randomized by chip+fin quality) is real, confirmed CE behavior — preserve it exactly,
  don't "fix" it into consistency, and don't assume `chip`'s absence from `MissileStruct` is a
  bug worth correcting (it may simply reflect "the entity itself never needs accuracy after launch").
- **`ItemMissileStandard`'s `MissileFormFactor`/`MissileTier`/`MissileFuel` enums are a completely
  separate family from anything `ItemMissile` (the `mp_*` system) uses** (which has `PartSize`/
  `WarheadType`/`FuelType` instead) — confirmed by reading both files fully. The similar names
  (`MissileTier` vs. nothing shared, `MissileFuel` vs. `FuelType`) are a real naming-collision risk
  during the port; flagging explicitly so the two systems' enums are never accidentally merged or
  cross-referenced as if they were one taxonomy.
- **Neo Edition's `IChunkLoader` carries a bare `@Deprecated` annotation with no explanation and no
  visible newer alternative anywhere else in that reference tree.** Since this port is treating Neo
  Edition as an API-shape reference only (not a completeness/correctness one, per every other Phase 1/2
  report's stated policy), recommend using the shape as-is but flagging this specific annotation for a
  second look at implementation time — it may simply reflect Neo Edition's own unfinished internal
  migration rather than a real problem with the API surface itself.
- **A full per-nested-class accounting of `EntityMissileTier1`/`2`/`3`/`4` (only Tier0's 5 subclasses
  were read in full; the rest were line-counted and spot-grepped only) is a real gap in this survey**,
  not an oversight to silently carry forward — `EntityMissileTier0` alone already contains one class
  (`EntityMissileTest`) with no matching `ItemMissileStandard` registry entry, so "28 standard items
  map 1:1 onto 28 concrete entity classes" cannot be assumed true for Tiers 1-4 either without an
  explicit read pass, which should be the first task whoever implements this package undertakes.
- **`EntityBobmazon`/`EntityMinerRocket`/`EntityBombletSelena`/`EntitySoyuz`/`EntitySoyuzCapsule`**
  physically live in `com.hbm.entity.missile` but were not read at all in this survey beyond a
  directory listing and line count — do not assume they are in scope for "the missile package" without
  a dedicated look; the `missile_soyuz*` item-side evidence above suggests at least the Soyuz pair
  belongs elsewhere (space/orbital content).
