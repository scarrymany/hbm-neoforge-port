# Fallout rain & effects (`EntityFalloutRain` / `BlockFallout` / `FalloutConfigJSON`) — Phase 4 research

## Headline finding

The task's own framing asked whether `EntityFalloutRain` is "a lingering-area-effect entity" that
"contaminate[s] players/mobs under it over time via `ContaminationUtil`." **Neither premise survives
reading the file (1065 lines, read in full).** `EntityFalloutRain` contains **zero references** to
`ContaminationUtil`, `HbmLivingProps`, `HazardType`, or any per-entity radiation call anywhere in it
(grepped the file directly for `Contamination|contaminate|radiate\(` — no hits). It never iterates
living entities at all. What it actually is:

1. **A server-only, `ForkJoinPool`-driven, off-heap/`Unsafe`-based asynchronous terrain-mutation
   engine.** Each tick it drains a time-budget (`BombConfig.falloutDelay`, port name
   `FALLOUT_DELAY`, default 30ms) of pending chunk work from lock-free MPMC/MPSC queues, dispatching
   worker tasks onto a shared `BombForkJoinPool` that scan every one of the ~256 columns in a chunk,
   evaluate `FalloutConfigJSON`'s block-transform rule table against the topmost few blocks of each
   column, and stage the resulting block/biome/falling-block changes to be committed back on the main
   thread. This runs across a huge ring of chunks (an outer rim at exactly `getScale()` blocks out, an
   inner disk sampled every 8 blocks along ~18+ angular spokes) for as long as it takes to finish, then
   the entity despawns itself (`secondPassAndFinish` → `setDead()`). This is corroborated, at arm's
   length, by Phase 3's own `docs/phase3/explosion_engine.md` ("Fallout trigger hook" section), which
   deliberately stopped reading at the import list and flagged from that alone that this "is itself a
   ForkJoinPool/off-heap/`Unsafe`-based system on the same scale as `ExplosionNukeRayParallelized`." A
   full read confirms that guess exactly and sharpens it: it needs `BombForkJoinPool` (165 CE lines,
   not ported), `com.hbm.util.ChunkUtil` (879 CE lines — an off-heap `NonBlockingHashMapLong<Chunk>`
   "loaded chunk mirror" plus CAS-based `ExtendedBlockStorage` copy/carve/modify helpers, not ported),
   and `com.hbm.lib.queues.{MpmcUnboundedXaddArrayLongQueue,MpscUnboundedXaddArrayLongQueue}` (not
   ported). Two of its four low-level primitives (`UnsafeHolder`, `NonBlockingHashMapLong` /
   `NonBlockingLong2LongHashMap`) already landed as Phase 3 foundation infra at
   `src/main/java/com/hbm/lib/{internal,maps}/` — a real head start — but the ForkJoinPool wrapper, the
   chunk-mirror/EBS-CAS utility layer, and the lock-free queue classes are still fully unported and are
   the actual hard blocker to a *faithfully concurrent* port (see Deferred scope).
2. **Player/mob contamination-over-time is not this entity's job at all — it happens through two
   separate downstream systems this entity merely feeds by changing blocks and biomes**, neither of
   which exists in the port yet:
   - `com.hbm.blocks.generic.BlockFallout` (131 lines, read in full) — the "ash" block the terrain
     engine scatters onto exposed surfaces near ground zero. Its `updateTick()` (fires every 10-40
     ticks via `scheduleUpdate`) calls `ChunkRadiationManager.proxy.incrementRad(worldIn, pos, 1,
     100)` — a **4-argument overload** of the API `docs/phase4/chunk_radiation_system.md` already
     names as load-bearing across ~20 other call sites (that report found only 2 overloads in use so
     far; this is a third, newly-confirmed shape for that report to pick up). Its `onEntityWalk()`
     directly slaps `new PotionEffect(HbmPotion.radiation, 2*60*20, 14)` (2400 ticks, amplifier 14) on
     any `EntityLivingBase` that steps on it — **not gated by armor/hazmat at all** at the block level;
     the actual `ContaminationUtil.contaminate(entity, RADIATION, CREATIVE, (level+1)*0.05F)` math (and
     therefore all the armor/creative/hazmat checks) lives inside `HbmPotion.radiation`'s own
     `performEffect` (CE `com.hbm.potion.HbmPotion.java:109-111`), a completely separate, still-unported
     class. So the real chain is: touch fallout block → potion effect applied unconditionally → potion
     effect's own periodic tick calls `ContaminationUtil.contaminate` (which then applies all the usual
     resistance checks). `BlockFallout` itself never calls `ContaminationUtil`.
   - Crater biomes (`BiomeGenCraterBase.craterInnerBiome/craterBiome/craterOuterBiome`), which this
     entity's own `getBiomeChange()` can paint onto the terrain it processes (gated by
     `WorldConfig.enableCraterBiomes`, already ported as `WorldConfig.CRATER_BIOME` +
     `CRATER_BIOME_RAD/CRATER_BIOME_INNER_RAD/CRATER_BIOME_OUTER_RAD/CRATER_BIOME_WATER_MULT`) are
     turned into actual ambient radiation by a wholly separate, much larger (759-line)
     `com.hbm.handler.EntityEffectHandler.onUpdate(EntityLivingBase)` — a general per-living-entity,
     per-tick handler (radiation, shields, fire, several mob-specific branches) that is **not part of
     this area** and does not exist in the port. Its crater-biome branch (CE lines 81-94) is the actual
     `ContaminationUtil.contaminate(entity, RADIATION, CREATIVE, radiation/20D)` call site for standing
     in a crater biome — confirming `EntityFalloutRain` really is two steps removed from any
     `ContaminationUtil` call.
3. **A second corrective point, resolving something Phase 3's report flagged as unresolved**: CE's two
   real spawn call sites use different constructors — `new EntityFalloutRain(world, int)` (MK3) vs.
   `new EntityFalloutRain(world)` (MK5) — which `docs/phase3/explosion_engine.md` called "a genuine CE
   inconsistency... worth preserving faithfully... since this report did not read `EntityFalloutRain`'s
   constructors closely enough to know which one is more correct." Having now read them: there is only
   one real constructor. `public EntityFalloutRain(World worldIn, int ignored) { this(worldIn); }` —
   the 2-arg overload silently discards its `int` argument and delegates to the 1-arg one. There is no
   missing "correct" behavior to resolve; the "asymmetry" is cosmetic dead-parameter code, not two
   different intended APIs. Still worth preserving the two-constructor shape faithfully (in case some
   other unread CE caller uses the 2-arg form), but nothing behavioral hinges on it.
4. **A third corrective point, directly relevant to the task's own "grep for Fallout" instruction**:
   `com.hbm.entity.effect.EntityFallout.java` (462 lines, read in full, an abstract "base class for all
   fallout entities, only aim for behavioral upstream parity" per its own javadoc) has **zero
   subclasses anywhere in CE** — `grep -rn "extends EntityFallout\b"` across the entire `hbm-ce` tree
   returns nothing. `EntityFalloutRain` does **not** extend it (it extends
   `EntityExplosionChunkloading`, already ported, see Key design decisions). `EntityFallout` is
   vestigial/orphaned code sharing a package and a similar name with the real system, containing its
   own separate (and also unused) sellafield/waste-block conversion logic
   (`processBlock`/`placeBlockFromDist`/`calculateS`) plus a `ForgeChunkManager.Ticket`-based
   3×3-chunk-loading scheme. It is not required for parity and should not be ported as part of this
   area; documented here only because the task explicitly asked to grep "Fallout" broadly and this is
   a real hit that a naive read could mistake for load-bearing.

## Suggested Phase 4 work-package split

### Package A — `BlockFallout` + item/block registration + `FalloutConfigJSON` (do this first; small,
self-contained, and the only part of this area with an existing, working NeoForge analog to build on)
Port `BlockFallout` (131 lines) against the already-ported `BlockHazardFalling`/`HazardSystem`/
`ContaminationUtil`/`ArmorUtil` infra, `ModItems.fallout` (a plain item), and `ModBlocks.block_fallout`
(`BlockHazardFalling` already names `block_fallout` as one of its two real CE uses in its own javadoc —
this is a drop-in instantiation, not new logic). Port `FalloutConfigJSON` (1107 lines) as a
self-contained JSON-driven block-transform rule engine: its own `initialize()`/Gson read-or-write-
default flow (against `MainRegistry.configHbmDir`, already real port infra — see Key design decisions),
its `FalloutEntry` builder/`eval()` matcher (distance-gated block/state/material/ore-dict matching with
a weighted-random primary/secondary output list and a Gaussian falloff near `maxDist`), and its 39
default entries. This package has exactly one external content gap (see Deferred scope's "wasteland
block set") but the *engine* itself — JSON I/O, matcher evaluation, `LookupResult` caching — is fully
portable today with no blocked dependency.

### Package B — `EntityFalloutRain`'s terrain-mutation sweep, single-threaded MVP (sequence after A)
Reimplement the actual per-column transform logic (`stompColumnToUpdates`, `gatherChunks`'s
spoke-and-disk chunk-set sampling, the falling-block/fire/biome side effects) **without** the
ForkJoinPool/off-heap concurrency layer: a plain iterator over the same chunk set, applying
`FalloutEntry.eval()` results directly via normal `Level#setBlockState` calls, budgeted across ticks by
the same `BombConfig.FALLOUT_DELAY`/`FALLOUT_CHUNK_SPEED` config values (process N chunks or Xms of
work per tick, whichever CE's own tuning implies is closer to intent). This produces the *exact same
block-transform outcome* as CE (same rules, same distances, same randomness inputs modulo RNG stream
differences) without needing `BombForkJoinPool`/`ChunkUtil`/the lock-free queues at all. This is the
pragmatic "make nukes leave scarred, irradiated craters" deliverable.

### Package C — Full concurrency parity (defer until/unless profiling shows Package B is too slow)
Port `BombForkJoinPool`, `ChunkUtil`'s off-heap mirror-map/CAS-EBS layer, and the two `com.hbm.lib.
queues` classes this area actually uses, to match CE's real multithreaded chunk-processing
architecture. This is the *same* substrate Phase 3's `explosion_engine.md` already deferred for
`ExplosionNukeRayParallelized` — do this once, shared, if and when that Phase 3 follow-up work happens,
rather than building two independent concurrent chunk-mutation engines.

### Not part of this package (real dependents/blockers that belong to other phases or other Phase 4 areas)
- `ChunkRadiationManager`/`RadiationSystemNT` — `docs/phase4/chunk_radiation_system.md`, an existing
  sibling Phase 4 research report; `BlockFallout.updateTick()`'s `incrementRad(World, BlockPos, int,
  int)` call is a new, real call site for that report to add to its list.
- `HbmPotion` (registration + `radiation`/`mutation`/`stability` effects) — not researched by name in
  any existing Phase 0-3 report as its own area; already named as a blocked dependency by
  `ContaminationUtil.calculateRadiationMod`'s and `ArmorUtil`'s own TODOs (per this task's framing). A
  future "status effects" Phase 4/5 area's job.
- `EntityEffectHandler` (759 lines) — a large, general per-tick living-entity handler unrelated in
  scope to fallout specifically; whichever area owns general ambient-radiation/player-tick effects.
- `BiomeGenCraterBase`/crater world-gen — already a documented Phase 4 TODO named by
  `BlockFissureBomb`'s own stub (per this task's framing); `EntityFalloutRain.getBiomeChange()` is a
  second real consumer for whoever implements it.
- `CompatibilityConfig.isWarDim`/`peaceDimensions` — see Deferred scope; explicitly, intentionally not
  ported yet by CompatibilityConfig's own javadoc, for reasons unrelated to fallout (dimension-ID
  re-keying to 1.21's `ResourceKey<Level>`).

## Phase-4-safe scope

| Component | CE source (lines read) | Port status | What Phase 4 should do |
|---|---|---|---|
| `BlockFallout` | `blocks/generic/BlockFallout.java` (131, full) | Not ported | Port directly; only its `HbmPotion.radiation` and `ChunkRadiationManager.incrementRad` calls are blocked (stub both as named forward references) |
| `ModBlocks.fallout` / `ModBlocks.block_fallout` / `ModItems.fallout` | `blocks/ModBlocks.java:608-609` | Not registered anywhere in port's `ModBlocks.java` (0 hits) | Add a new domain file (e.g. `FalloutBlocks.java`) following the `OreBlocks`/`GenericBlocks` `DeferredRegister` delegation pattern `ModBlocks.register()` already uses |
| `FalloutConfigJSON` (engine: I/O, `FalloutEntry`, `eval()`, `LookupResult`) | `config/FalloutConfigJSON.java` (1107, read in full) | Not ported | Fully portable today — no blocked dependency for the engine itself |
| `FalloutConfigJSON`'s 39 default entries | same file, `initDefault()` (lines 104-403) | Not ported | Portable, but ~24 of their *output* blocks don't exist yet (see Deferred scope) |
| `EntityFalloutRain`'s per-column transform math (`stompColumnToUpdates`) | `entity/effect/EntityFalloutRain.java:681-783` | Not ported | Portable as single-threaded logic (Package B); faithfully reproduces sellafield/waste conversion, fire spread, structural-collapse falling blocks |
| `EntityFalloutRain`'s chunk-set sampling (`gatherChunks`) | same file, `862-911` | Not ported | Portable directly — pure geometry, no concurrency dependency |
| `EntityFalloutRain`'s concurrency layer (ForkJoinPool/off-heap queues/CAS chunk mutation) | same file, throughout | Not ported | Deferred to Package C (needs `BombForkJoinPool`+`ChunkUtil`+queue classes, none ported) |
| `BombConfig` fallout tunables | `config/BombConfig.java:34-46` (CE), already ported | **Already ported** as `FALLOUT_RANGE`/`FALLOUT_CHUNK_SPEED`/`FALLOUT_DELAY`/`SAFE_COMMIT`/`MK5_BLAST_TIME` | No work needed — reuse as-is |
| `WorldConfig` crater-biome tunables | `config/WorldConfig.java:25-28` (CE), already ported | **Already ported** as `CRATER_BIOME`/`CRATER_BIOME_RAD`/`CRATER_BIOME_INNER_RAD`/`CRATER_BIOME_OUTER_RAD`/`CRATER_BIOME_WATER_MULT` | No work needed |
| Spawn call sites in `EntityNukeExplosionMK5`/`MK3` | port `entity/logic/EntityNukeExplosionMK5.java:147-153`, `EntityNukeExplosionMK3.java:229-234` | **Stub comments already committed**, exact shape recorded | Fill in the two named call sites once `EntityFalloutRain` exists; comments already specify position/scale/detonator per call site |
| `EntityFalloutRain`'s `EntityType` registration | CE `@AutoRegister(name = "entity_fallout_rain", trackingRange = 1000)` | Not registered | Add to `entity/logic/NukeEntityTypes.java` (the exact precedent for MK5/MK3/Balefire) — note CE *does* call `setSize(4.0F, 20.0F)` in its constructor (unlike MK5/MK3/Balefire, which take the "no explicit size, pick a small nominal one" fallback that file's javadoc documents), so use `.sized(4.0F, 20.0F)` here, a real CE value, not a guess |
| `RenderFallout` (client rain-streak particle renderer) | `render/entity/RenderFallout.java` (232 lines, `GlStateManager`/`Tessellator`/`Render<T>`-era API) | Not ported | Phase 5 (Client & UX) — 1.12-era immediate-mode rendering has no 1.21.1 equivalent shape |
| `EntityFallout` (abstract base, unused) | `entity/effect/EntityFallout.java` (462, full) | N/A | Do not port — zero subclasses in CE, vestigial |

## Deferred scope

- **`HbmPotion`** (registration + `radiation`/`mutation`/`stability` effects, CE 178 lines) — blocks
  `BlockFallout.onEntityWalk`'s potion application and (per `ContaminationUtil`'s and `ArmorUtil`'s own
  already-documented TODOs) several immunity/resistance short-circuits. Stub the `onEntityWalk` call
  site with a named forward reference the same way other Phase 3 TODOs do.
- **`ChunkRadiationManager`/`RadiationSystemNT`** — already a full, separate Phase 4 research area
  (`docs/phase4/chunk_radiation_system.md`, read for cross-reference). `BlockFallout.updateTick()`'s
  `incrementRad(World, BlockPos, int, int)` overload is a real, newly-confirmed call site that report's
  future implementation pass should add to its list of ~20+ call sites.
- **`EntityEffectHandler`** (CE 759 lines, not ported) — the actual class that converts standing in a
  crater biome into a `ContaminationUtil.contaminate` call (CE lines 81-94). General-purpose, far
  larger than fallout alone (fire, shields, several mob branches); not this area's job to port, but
  worth naming precisely since it's the missing link between "fallout rain paints a crater biome" and
  "standing in that biome now hurts you."
- **`BiomeGenCraterBase` / crater biome world-gen`** — already a documented Phase 4 TODO per this
  task's own framing (named by `BlockFissureBomb`'s stub). `EntityFalloutRain.getBiomeChange()`
  (`scale >= 150 && distPercent < 15` → inner, `scale >= 100 && distPercent < 55` → mid, `scale >= 25`
  → outer) is a second real consumer of whatever biome registration that area lands, gated by the
  already-ported `WorldConfig.CRATER_BIOME` toggle.
- **`BombForkJoinPool`** (CE 165 lines), **`com.hbm.util.ChunkUtil`** (CE 879 lines), and
  **`com.hbm.lib.queues.{MpmcUnboundedXaddArrayLongQueue,MpscUnboundedXaddArrayLongQueue}`** (CE, ~32KB
  combined across those 2 of the package's 4 files) — the concurrency substrate for a faithfully
  multithreaded port (Package C above). Shared scope with Phase 3's already-deferred
  `ExplosionNukeRayParallelized`; do not build two separate implementations of the same off-heap
  chunk-mutation machinery independently.
- **The wasteland/mutated-terrain block content set** — `FalloutConfigJSON`'s 39 default entries and
  `EntityFalloutRain.stompColumnToUpdates`'s own hardcoded `ModBlocks.volcano_core`→`volcano_rad_core`
  branch together target roughly two dozen distinct output blocks: `sellafield`/`sellafield_slaked`/
  `sellafield_bedrock` (irradiated-stone decay tiers), `waste_earth`/`waste_grass_tall`/
  `waste_mycelium`/`waste_trinitite`/`waste_trinitite_red`/`waste_leaves`/`waste_log`/`waste_planks`
  (dead-vegetation/scorched-wood set), `toxic_block`, `mush`/`mush_block`/`mush_block_stem`
  (mushroom-biome mutation), `ore_uranium_scorched`/`ore_schrabidium`/`ore_nether_uranium_scorched`/
  `ore_nether_schrabidium`/`ore_gneiss_uranium_scorched`/`ore_gneiss_schrabidium` (ore-mutation tiers),
  `volcano_rad_core`, and `brick_concrete_broken`. **Verified zero of these exist anywhere in the
  port's `ModBlocks.java`** (grepped each by name). No other Phase 0-3 report or already-completed area
  claims ownership of this set — it is a pure content gap, not a design blocker, and per the port's own
  established pattern ("whichever area first needs a block registers it in a domain file and appends
  its `registerAll()` call to `ModBlocks.register()`") this area can supply it directly (Package A/C in
  the work-package split) rather than waiting on another phase.
- **`CompatibilityConfig.isWarDim`/`peaceDimensions`** — `EntityFalloutRain.onUpdate()`'s very first
  real check is `if (!CompatibilityConfig.isWarDim(world)) { setDead(); }`. The port's own
  `CompatibilityConfig.java` javadoc **explicitly and intentionally** excludes this: CE's ~60
  per-dimension tables (including `peaceDimensions`) are keyed by an integer Forge dimension ID that no
  longer exists in 1.21 (`ResourceKey<Level>` now), and re-keying them on guessed mod-compat targets
  "would be worse than not porting them" — that javadoc defers the whole family to "whichever phase
  owns world generation." Until that lands, stub `isWarDim` as always-`true` (this exactly matches
  CE's own default behavior: `peaceDimensionsIsWhitelist=true` with an empty default `peaceDimensions`
  set means `isWarDim` returns `true` in every dimension out of the box), with a named
  `// TODO(CompatibilityConfig.isWarDim, Phase 4 world-gen/dimension-config)` comment.
- **`CompatDynamicTrees`** (Dynamic Trees mod soft-compat, referenced in `doNotifyOnMain` for
  `isTreePart`/`destroyOrphanedNeighbors`) — no port file exists; this is third-party mod
  interoperability, not core CE behavior. Safe to omit entirely (no-op the two call sites) rather than
  treat as a blocker — consistent with how other soft mod-compat hooks are expected to be handled
  elsewhere in this port.
- **`AdvancementManager`** — not referenced by this area directly, named here only because it recurs
  across every other Phase 3/4 forward-reference list; not this area's concern.
- **`RenderFallout`** (client rain-streak particle rendering, `GlStateManager`/`Tessellator`/`Render<T>`
  1.12-era API) — Phase 5 (Client & UX); the rendering pipeline itself has no direct 1.21.1 analog to
  port line-for-line, it needs a from-scratch `EntityRenderer`/`PoseStack` rewrite.

## Key design/API decisions

- **`ContaminationUtil.contaminate`'s exact live signature** (already real, compiling infra):
  `public static boolean contaminate(LivingEntity entity, HazardType hazard, ContaminationType cont,
  double amount)` at `src/main/java/com/hbm/util/ContaminationUtil.java:630`, with `HazardType`/
  `ContaminationType` as nested enums at lines 600/607. Already used identically elsewhere in the port
  (`EntityNukeExplosionMK5`'s `radiate()`: `ContaminationUtil.contaminate(e,
  ContaminationUtil.HazardType.RADIATION, ContaminationUtil.ContaminationType.RAD_BYPASS, eRads)`).
  Fallout's own future dependents (`HbmPotion.radiation`, `EntityEffectHandler`) should call this exact
  shape with `ContaminationType.CREATIVE`, not `RAD_BYPASS` (CE uses `CREATIVE` for both crater biomes
  and the radiation potion, `RAD_BYPASS` only for the nuke blast's own direct line-of-sight radiation).
- **Exact spawn-parameter shape at both real call sites** (already recorded as committed stub comments
  in the port, cross-checked against CE line-for-line):
  - MK3 (`EntityNukeExplosionMK3.onUpdate`, "waste" path, once per detonation): position = entity's own
    `posX/posY/posZ`; `fallout.detonator = detonator` (MK3 **does** propagate the detonator); scale =
    `(int)(destructionRange * 1.8)`; constructed via the 2-arg
    `new EntityFalloutRain(world, (int)(destructionRange * 1.8) * 10)` (the `int` arg is discarded, see
    Headline finding #3).
  - MK5 (`EntityNukeExplosionMK5.onUpdate`, once `explosion.isComplete()` and the `fallout` flag is
    still set — cleared by `statFacNoRad`/`BombConfig.DISABLE_NUCLEAR`): position = entity's own
    `posX/posY/posZ`; **no detonator propagated** (real CE asymmetry, faithfully preserve); scale =
    `(int)(radius * 2.5 + falloutAdd) * BombConfig.falloutRange / 100` (MK5 bakes the
    `FALLOUT_RANGE` percent-scale config directly into the spawn call; MK3 does not — also a real,
    faithfully-preservable asymmetry); constructed via the 1-arg `new EntityFalloutRain(world)`.
- **`FalloutConfigJSON`'s config-file plumbing has a direct, already-real precedent to reuse**:
  `MainRegistry.configHbmDir` already exists in the port (`src/main/java/com/hbm/main/MainRegistry.java:
  45,53-54`) and is already used by another class for exactly this "write a bespoke config file into
  the mod's config subfolder" pattern (`ExplosionNukeGeneric.java:296`, `solinium.cfg`). CE's own
  `hbmFallout.json` (config) + `_hbmFallout.json` (regenerated template, written whenever the real
  config is missing) read/write flow via `com.google.gson.Gson`/`JsonWriter` can be ported verbatim
  against `MainRegistry.configHbmDir` with no NeoForge-specific config-system involvement (this is a
  raw file, not a `ModConfigSpec`).
- **`FalloutEntry.eval()`'s exact matcher/output algorithm** (CE `config/FalloutConfigJSON.java:
  883-938`, read in full): reject if `dist` outside `[minDist, maxDist]`; reject on block-state/material/
  ore-dict/opaque mismatch; apply a Gaussian-noise falloff once `dist > maxDist * falloffStart`
  (default 0.9) that probabilistically drops the conversion the closer to `maxDist` it gets; pick a
  weighted-random output from `primaryBlocks` (or `secondaryBlocks` if a `primaryChance` roll fails);
  three CE-specific guard clauses prevent sellafield/bedrock tiers from ever "downgrading" a
  more-decayed block back to a less-decayed one, and prevent `y==0` (true bedrock layer) from ever
  becoming anything but `sellafield_bedrock`. `LookupResult` (a small per-`IBlockState` cache of
  block/material/opaque/meta/ore-ids) exists purely to avoid recomputing `getMaterial()`/
  `isOpaqueCube()`/ore-dict lookups per rule-list entry per block — a straightforward memoization, not
  behavior-affecting, portable as-is or dropped entirely in the Package B single-threaded MVP (the
  underlying data it caches, `IBlockState`, still resolves the same either way, just recomputed).
- **`EntityExplosionChunkloading` (already ported, `entity/logic/EntityExplosionChunkloading.java`, 117
  lines, read in full) is compatible with `EntityFalloutRain` with no changes needed**, despite its own
  javadoc noting it was written only for MK3/MK5/Balefire's "never move, no synced data" usage pattern.
  Two things to verify hold: (1) `EntityFalloutRain` registers one synced `SCALE` `DataParameter<Integer>`
  in `entityInit()` (CE) — the port's current `defineSynchedData` override is an empty no-op, but a
  concrete `EntityFalloutRain` subclass can simply override it again to add its own field, exactly as
  any subclass may re-override a parent's method; and (2) `EntityFalloutRain` never actually calls
  `loadChunk()`/relies on the force-loading half of `IChunkLoader` at all in CE — its own chunk-loading
  model is `ChunkUtil.acquireMirrorMap`'s off-heap mirror snapshot plus plain `world.getChunk(cx, cz)`
  calls scheduled onto the main thread (`loadMissingChunksUntil`) when a needed chunk isn't already
  resident, which is a normal (not "forced"/persistent) chunk load — so the base class's simplified
  "force-load once, never re-force" model is simply unused by this entity, not violated by it.
- **`BlockHazardFalling` (already ported) already anticipates `block_fallout` by name** — its own
  javadoc reads "CE's `BlockHazardFalling` (`block_fallout`, `block_yellowcake`): a falling, sand-like
  hazardous block," confirming this area's `ModBlocks.block_fallout` instantiation is a drop-in
  `new BlockHazardFalling(properties)` plus a `HazardSystem.register(...)` call using the
  already-ported `HazardRegistry.fo` (10F) and `HazardRegistry.block` (10.0F) constants (CE:
  `HazardSystem.register(block_fallout, makeData(RADIATION, yc * block * powder_mult))`), following the
  exact `HazardSystem.register(item.get(), new HazardData().addEntry(...))` pattern already live
  throughout `HazardRegistry.java`.
- **`RadiationConfig.ENABLE_CONTAMINATION_ON_GROUND`** (already ported, referenced from
  `HazardData.java:37`) is the existing config gate for "hazard items sitting on the ground irradiate
  the area" — worth checking whether CE's fallout-block/crater-biome radiation is meant to respect the
  same toggle or is unconditional; not confirmed either way by this read (see Open questions).

## Open questions / risks

- **Package B's single-threaded MVP changes CE's real-time performance profile.** CE's concurrent
  design exists because a large detonation (e.g. Tsar Bomba, `BombConfig.TSAR_RADIUS` default 500 →
  fallout scale `(500*2.5)*100/100 = 1250` blocks) sweeps a multi-thousand-block-radius ring across
  potentially thousands of chunks. A naive single-threaded per-tick budget loop should still converge
  (CE's own `BombConfig.FALLOUT_DELAY`/`FALLOUT_CHUNK_SPEED` budget the *legacy* single-threaded MK5
  ray algorithm the exact same way), but has not been measured against CE's actual completion-time
  characteristics — worth a real playtest at a large radius before considering Package B "done."
- **Whether `FalloutConfigJSON`'s default entries should be trimmed, stubbed, or left pointing at
  not-yet-existent blocks** until the wasteland block-content package lands. Three options: (a) skip
  the ~24 blocked default entries entirely (silently incomplete transform table, but compiles and
  behaves correctly for whatever subset of rules *does* have real target blocks — e.g. the
  `hbm:brick_concrete` → `brick_concrete_broken` entry, or the trinitite-sand entry, would simply not
  fire); (b) implement the missing blocks alongside this area (recommended, see Deferred scope); (c)
  temporarily point conversions at close vanilla analogs (loses visual/gameplay parity, not
  recommended). This report recommends (b) given the port's own established "whoever needs a block
  registers it" pattern, but the actual call is an implementation-phase decision once scope/time is
  known.
- **Does `RadiationConfig.ENABLE_CONTAMINATION_ON_GROUND` gate `BlockFallout`/crater-biome radiation
  the same way it gates dropped-hazard-item radiation?** Not confirmed either way from the files read
  for this report — `BlockFallout.updateTick()`'s `incrementRad` call and `HbmPotion.radiation`'s
  `contaminate` call are both unconditional in the CE source read here. Worth a targeted check of
  `RadiationConfig`'s full CE source (out of this report's scope) before assuming either respects or
  ignores that toggle.
- **`isWarDim`'s stub-to-`true` recommendation assumes CE's *default* config**, which is safe for
  parity out of the box, but a server operator who configured `peaceDimensions` in CE to protect a
  lobby/PvP-arena dimension would see that protection silently vanish until the real
  `CompatibilityConfig` dimension-ID re-keying work lands — worth flagging to whoever tracks
  config-migration risk for this port as a real (if narrow) behavior gap, not just a "TODO, fine for
  now."
- **`EntityFalloutRain`'s bounding box (`setSize(4.0F, 20.0F)`) and `ignoreFrustumCheck = true`/
  `isImmuneToFire = true`** were read directly from the constructor but this report did not chase why
  CE picked exactly `4×20` (likely tuned for the now-defunct 1.12 rain-particle renderer's visual
  column, a Phase 5 concern) — confirm with Phase 5's own research before assuming this exact size
  still matters once `RenderFallout` is rewritten from scratch, since nothing server-side (world
  mutation) depends on the entity's actual hitbox size.
- **This report did not read `HazardTypeContaminating.java`, `ItemPollutionDetector.java`,
  `NukeCustom.java`, `EntityMissileTier4.java`, `BlockCrashedBomb.java`, `DetCord.java`, or
  `Landmine.java` beyond the single grepped context window shown by the task's "grep for Fallout"
  instruction** — all six were confirmed to be incidental hits (tooltip strings mentioning "[Fallout]"
  radius, or `BombConfig.falloutRange` reads for damage-radius display) with no additional
  `EntityFalloutRain`/`BlockFallout` logic, except `NukeCustom.java` which contains an explicit CE
  author comment ("mlbv: replaced EntityFalloutRain with this... Credit: Leafia") confirming a
  *third*, deliberately-avoided fallout call site was intentionally replaced with
  `ExplosionNukeGeneric.waste(...)` instead — corroborating that exactly 2 real
  `new EntityFalloutRain(...)` call sites exist anywhere in CE (verified directly via
  `grep -rn "new EntityFalloutRain" upstream/hbm-ce/src/main/java/` → exactly 2 hits, both already
  named by this task).
