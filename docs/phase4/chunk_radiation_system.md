# Chunk radiation system (`RadiationSystemNT`) — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/handler/radiation/ChunkRadiationManager.java` (68 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/radiation/RadiationWorldHandler.java` (109 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/radiation/RadiationSystemNT.java` (3,946 lines total
  — ~2,900 lines read directly, covering every architecturally distinct subsystem: all public/
  `@ServerThread` entry points (`incrementRad`/`decrementRad`/`setRadForCoord`/`getRadForCoord`/
  `markSectionForRebuild`/`markSectionsForRebuild`/`jettisonData`), every Forge event hook
  (`onWorldLoad`/`onWorldUnload`/`onChunkLoad`/`onChunkUnload`/`onChunkDataLoad`/`onChunkDataSave`/
  `onWorldUpdate`/`onServerTickLast`), the full `scanResistantMask` block-palette walk (all three
  `IBlockStatePalette` implementations), `floodFillPockets`, `remapPocketMass`, the full tick pipeline
  (`processWorldSimulation` → `rebuildDirtySections` → `runExactExchangeSweeps` →
  `postSweepDecayAndEffects`), `rebuildChunkPocketsLoaded` (the UNI/SINGLE/MULTI kind decision),
  `tryEncodePayload`/`readPayload` (the on-disk binary format), `spawnFog`, `PostSweepTask` (decay +
  world-destruction queueing), `DiffuseXTask`/`DiffuseZTask`/`DiffuseYTask` (the 3-axis sweep), one
  full pairwise-exchange formula (`SingleMaskedSectionRef.exchangeWithSingle`/`exchangeWithUniform`),
  and `DirtyChunkTracker` (the dirty-section bitmap). Not read line-by-line: `MultiSectionRef`'s
  remaining face-linking bookkeeping (~450 lines), `LinkCanonicalKeysTask`/`RebuildDirtyChunkBatchTask`
  (~180 lines), and `EditTable`'s open-addressing internals (~125 lines) — surveyed by signature only
  (see method list captured below); these are concurrency/bookkeeping plumbing around already-verified
  algorithms (the same flood-fill/exchange/decay math documented below), not new behavior.
- `upstream/hbm-ce/src/main/java/com/hbm/handler/radiation/RadVisOverlay.java` (1,867 lines — header,
  quad-packing helpers, and the client-tick/data-access entry points read; the GL immediate-mode mesh
  generation itself skimmed, it is pure debug-rendering plumbing over data already understood from
  `RadiationSystemNT`)
- `upstream/hbm-ce/src/main/java/com/hbm/util/ContaminationUtil.java` (`printGeigerData`,
  `printDosimeterData`, `calculateRadiationMod`, immune-entity list — read in full for this area;
  the rest of this 800+-line file is Phase 3/other-Phase-4-area scope, not re-read here)
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemGeigerCounter.java` (110 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/config/{RadiationConfig,GeneralConfig,CompatibilityConfig}.java`
  (radiation-relevant fields and their `loadFromConfig` definitions)
- `upstream/hbm-ce/src/main/java/com/hbm/interfaces/IRadResistantBlock.java` (full, 15 lines)
- `upstream/neo-edition/src/main/java/com/hbm/{lib/ModAttachments.java, handler/radiation/
  {ChunkRadiationManager,ChunkRadiationHandler,ChunkRadiationHandlerSimple}.java}` (full — consulted
  **only** for real NeoForge 1.21 API shape, per the ground rules; its actual radiation algorithm is a
  single-float-per-chunk stub, not CE's real system, and is not used as a design source anywhere below)
- This port's own already-committed code: `src/main/java/com/hbm/capability/ModAttachments.java` (full,
  the entity-attachment registration template), `src/main/java/com/hbm/config/{RadiationConfig,
  GeneralConfig,CompatibilityConfig}.java` (full), `src/main/java/com/hbm/lib/Library.java` (full, 309
  lines), `src/main/java/com/hbm/interfaces/IRadResistantBlock.java` (full), `src/main/java/com/hbm/
  packet/toclient/ExplosionEffectSyncPacket.java` + `src/main/java/com/hbm/explosion/vanillant/standard/
  ExplosionEffectStandard.java` (full — the confirmed real `CustomPacketPayload` +
  `PacketDistributor.sendToPlayersNear` pattern), plus repo-wide greps for
  `ChunkRadiationManager`/`RadiationSystemNT`/`markSectionForRebuild`/`markSectionsForRebuild`/
  `IRadResistantBlock`/`AuxParticlePacketNT`/`PacketThreading` across the whole `src/main/java` tree
  (20+ already-committed call sites found, enumerated below), and `docs/phase0/STATUS.md`,
  `PORT_SPEC.md` §2 and §3.

## Headline finding

This task's own framing asked whether storage is "a byte/float array per subchunk section? per-block?
per-chunk-column scalar?" and whether there's "a packet shape" for Geiger client feedback. **Neither
premise survives contact with the real code**, and both corrections matter for scoping the rest of
Phase 4:

1. **Storage is none of the three guesses — it is per-connected-open-space "pocket" inside each
   16×16×16 section**, discovered by a live flood fill over the section's real block palette (any
   block implementing `IRadResistantBlock` with `isRadResistant(...) == true` is a wall), up to 2048
   pockets per section (`MAX_POCKETS`). A section collapses to one of three cheap representations
   depending on what the flood fill finds: **UNI** (no resistant blocks at all — one `double` for the
   whole 4096-block volume, no object allocated), **SINGLE** (resistant blocks exist but only one
   connected open pocket — one `double` plus a lazily-built face/volume/centroid descriptor for
   diffusion), or **MULTI** (≥2 disconnected pockets — a `double[pocketCount*2]` of
   density+inverse-volume pairs, one entry per pocket). This is a genuine pseudo-fluid simulation:
   radiation "mass" (density × volume) is conserved when pocket topology changes (breaking a wall
   between two rooms merges and redistributes their radiation proportionally — see `remapPocketMass`),
   not a simple decaying scalar grid. Whichever storage shape Phase 4 lands on for the NeoForge port
   must be able to represent this — a flat `float[]`-per-section or single-scalar-per-chunk model
   would be a real behavior regression, not just an implementation detail change.
2. **There is no "Geiger packet."** `ContaminationUtil.printGeigerData`/`printDosimeterData` run
   entirely server-side (triggered by `ItemGeigerCounter.onItemRightClick`'s `!world.isRemote` guard),
   compute every number server-side including the raw `ChunkRadiationManager.proxy.getRadiation(...)`
   query, and deliver the result as ordinary chat `TextComponentTranslation`/`TextComponentString`
   messages via `EntityPlayer.sendMessage(...)` — standard vanilla chat-packet plumbing, zero custom
   network payload. The Geiger *clicking sound* (`ItemGeigerCounter.playGeiger`) is likewise
   server-computed and server-broadcast via `World.playSound(null, ...)`, and it's keyed off the
   player's own **accumulated dose** (`ContaminationUtil.getActualPlayerRads`, already a
   `HbmLivingProps`-tracked value per Phase 3), not the raw chunk-radiation field. The **only** actual
   custom network payload anywhere in this system is a decorative particle effect: when a pocket's
   density exceeds `RadiationConfig.fogRad`, `WorldRadiationData.spawnFog` broadcasts a `RadFog`
   `AuxParticlePacketNT` to nearby players via `PacketThreading.createAllAroundThreadedPacket(...,
   new TargetPoint(dim, x, y, z, 100))` — a generic "spawn this named particle here" payload that
   carries no numeric radiation data at all. The one thing that *is* a genuine live numeric radiation
   visualization — `RadVisOverlay`, the pocket-boundary debug renderer — **only works via
   `Minecraft.getIntegratedServer()`**: it reads `RadiationSystemNT.worldMap` directly, in-process,
   because client and server share a JVM in singleplayer. It has **zero multiplayer/dedicated-server
   support in CE** and is a developer debug tool, not a player-facing feature.
3. **This exact API surface is already load-bearing across ~20 already-committed Phase 0-3 files.**
   `grep -rn "ChunkRadiationManager\|RadiationSystemNT\|markSectionForRebuild"` across this port's
   `src/main/java` today finds real, compiling-except-for-this-gap call sites in
   `ContaminationUtil.calculateRadiationMod`/`printGeigerData` (stubbed to 0), `HazardTypeContaminating`
   (`proxy.incrementRad(world, pos, level)`, live), `FT_VentRadiation`
   (`proxy.incrementRad(level, pos, overflowAmount * radPerMB)`, live), `SiloHatchBlockEntity`,
   `RBMKRodBlockEntity`, `RBMKNeutronHandler` (×3, commented-out pending this class), `ArmorFSB`
   (geiger-tick sound cue), `XFactoryEnergy`, `GrenadeFillingActions`, `ExplosionNukeSmall`, and
   documented-forward-reference javadoc on `IRBMKMeltdownHandler`, `BlockHazard`,
   `BlockHazardFalling`, `BlockNTMOre`, `BlockNTMGlass`, `BlockRadResistant`,
   `BlockRadResistantPillar`, `ReinforcedLamp`, `CrateBlock`. **8 real blocks already implement
   `IRadResistantBlock`** (`BlockNTMGlass`, `BlockNTMGlassPane`, `BlockRadResistantPillar`,
   `ReinforcedLamp`, `BlockSiloHatch`, `DummyBlockSiloHatch`, `CrateBlock`, `BlockMush`) waiting on the
   flood-fill scan that gives that interface meaning. Phase 4 is not free to redesign this call
   shape — `ChunkRadiationManager.proxy.{getRadiation,incrementRad×2-overloads,decrementRad,
   setRadiation,clearSystem}` and `RadiationSystemNT.{markSectionForRebuild,markSectionsForRebuild}`
   must exist with those exact names and (Level/BlockPos/float-or-double) signatures for this code to
   compile as already written.
4. **PORT_SPEC.md's own directive to use "Data Attachments on chunks" is, as far as this survey could
   establish, unverified against real code anywhere in either tree.** `ModAttachments.java` (this port)
   and its neo-edition counterpart both register `AttachmentType`s only on `Entity`-family holders
   (`HbmLivingAttachment`/`HbmPlayerAttachment`/`HbmLivingAttachments`); neither this port nor
   neo-edition has a single `ChunkAccess`/`LevelChunk` `AttachmentType` example, and neo-edition's own
   chunk-radiation code (`ChunkRadiationHandlerSimple`) does **not** use attachments at all — it falls
   back to CE's original technique of reading/writing a value straight into the `ChunkDataEvent.Load`/
   `Save` NBT compound. This sandbox has no NeoForge jar to check `IAttachmentHolder`'s implementor
   list directly. See Open questions/risks — this is the single highest-risk unconfirmed claim in this
   report.

## Phase-4-safe scope

Everything in this table is buildable today against already-confirmed-real infrastructure with no
further cross-phase blocker:

| Piece | Depends on (already real) | Notes |
|---|---|---|
| `ChunkRadiationManager` facade class + `proxy` field | Nothing beyond `RadiationSystemNT` itself | CE's own `ChunkRadiationManager` (68 lines) is a thin, intentionally-CE-authored indirection ("We only have one radiation system, unlike upstream this proxy is made to make porting easier" — CE's own class javadoc). Port it as a thin wrapper exactly as CE does; do not fold it into `RadiationSystemNT` even though there is only one real implementation, since ~20 call sites already assume `ChunkRadiationManager.proxy.X(...)` as the entry point. |
| The flood-fill pocket engine (`floodFillPockets`, `scanResistantMask`, the UNI/SINGLE/MULTI kind decision in `rebuildChunkPocketsLoaded`) | `IRadResistantBlock` (already ported, matches CE 1:1), a `BlockState`↔registry-id palette walk (NeoForge equivalent of `Block.BLOCK_STATE_IDS`/`IBlockStatePalette` — standard vanilla `PalettedContainer` API, not mod-specific) | This is pure per-chunk-section logic with no other-phase dependency. The CE code's three-palette-format branch (`REGISTRY_BASED_PALETTE`/`BlockStatePaletteLinear`/`BlockStatePaletteHashMap`) is a 1.12.2-specific optimization detail (Forge's palette had 3 storage tiers by distinct-block-count); 1.21's `PalettedContainer` has an analogous but not identical tiered-palette design — re-verify tier count/API against real NeoForge source before porting this bit-twiddling verbatim (see Open questions). |
| The diffusion/decay tick engine (`processWorldSimulation`, the X/Z/Y exact-exchange sweeps, `remapPocketMass`, `retentionDt` half-life decay) | Nothing but pure math + the storage engine above | Fully self-contained. `RadiationConfig.RAD_TICK_RATE`/`RAD_HALF_LIFE_SECONDS`/`RAD_DIFFUSIVITY` are **already ported** in this repo's `RadiationConfig.java` with matching CE default values (1, 120.0, 10.0) — confirmed by reading both files side by side. |
| Dirty-section marking (`DirtyChunkTracker`, `markSectionForRebuild`/`markSectionsForRebuild`) | Nothing new | Needed immediately by the 8 already-committed `IRadResistantBlock` implementors above, once ported — each must call this on placement/removal exactly as CE's own interface javadoc instructs ("must override onBlockAdded/onPlace and neighborChanged/onRemove and call ... markChunkForRebuild or it won't work" — same comment already present verbatim in this port's `IRadResistantBlock.java`). |
| Chunk-column binary persistence format (`tryEncodePayload`/`readPayload`, magic `"NTX"` + format byte 7, `hbmRadDataNT`) | A chunk-scoped storage hook (see Open questions for attachment-vs-event choice) | The payload itself is trivial: a 2-byte entry count then repeated `(2-byte sy<<11\|pocketIndex, 8-byte double density)` tuples, only non-zero pockets written. This is genuinely small and format-stable — whichever chunk storage mechanism Phase 4 picks, the *bytes* to store are already fully specified by CE. |
| World-destruction block-decay sweep (`RadiationWorldHandler.decayBlock`, grass/leaves/sand → wasteland conversion) | `PlantBlocks.{WASTE_EARTH,WASTE_MYCELIUM,WASTE_TRINITITE,WASTE_TRINITITE_RED,WASTE_LEAVES,WASTE_GRASS_TALL}` | **All already registered** in this port (`com.hbm.blocks.generic.PlantBlocks`, confirmed by direct read) — this sweep has zero remaining block-content dependency and can be ported in full immediately. `RadiationConfig.WORLD_RAD_EFFECTS` (CE: `worldRadEffects`) already exists and gates it correctly. |
| Server-only Geiger/dosimeter chat readout + sound cue (`ContaminationUtil.printGeigerData`/`printDosimeterData`, `ItemGeigerCounter.playGeiger`) | `HbmLivingProps.getRadiation`, `ContaminationUtil.calculateRadiationMod`/`getActualPlayerRads` (Phase 3 foundation, already real) | No networking to design at all — this is vanilla `Player.sendSystemMessage`/`Level.playSound` once the underlying `getRadiation` query exists. The port's `ContaminationUtil.java` already has this exact call site stubbed (`double rads = 0D; // TODO(ChunkRadiationManager, Phase 4)`) — filling that one line back in (plus the matching TODO in `RBMKRodBlockEntity`/`RBMKNeutronHandler`/`ArmorFSB`) is most of the integration work once the engine exists. |
| `RadFog` particle broadcast on high-radiation pockets | This port's confirmed `CustomPacketPayload` + `PacketDistributor.sendToPlayersNear` pattern (`ExplosionEffectSyncPacket`/`ExplosionEffectStandard`, already real and used) | Direct 1:1 replacement for CE's `PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(...), new TargetPoint(dim, x, y, z, 100))`. `AuxParticlePacketNT`/`HbmEffectNT` themselves are a separate, already-documented cross-cutting gap (confirmed absent, referenced by `GrenadeFillingActions`' own javadoc as "confirmed not ported anywhere") shared with several Phase 3 VFX call sites — Phase 4 either builds a minimal single-purpose `RadFogSyncPacket` scoped to this feature, or coordinates with whoever ends up owning the generic named-particle-broadcast packet. |

## The storage model in detail

CE's four section "kinds" (`KIND_NONE`/`KIND_UNI`/`KIND_SINGLE`/`KIND_MULTI`, packed 2 bits per
subchunk into a `long chunkMeta[chunkId]`, 16 sections per chunk column):

- **`KIND_NONE` (0)** — section not yet (re)built since last touched; either never scanned or marked
  dirty. `getRadForCoord` returns `0` immediately and enqueues a rebuild; any `incrementRad`/
  `decrementRad`/`setRadForCoord` call against a `NONE` (or currently-dirty) section instead buffers
  the write into a per-chunk `EditTable` (keyed by packed `(sectionKey, localBlockIndex)`, with
  separate "add" accumulation and "last write wins by sequence number" set semantics) to be replayed
  once the section's real pocket layout is known.
- **`KIND_UNI` (1)** — the flood fill found **zero** resistant blocks in the section (typical for open
  sky/underground air, or a section made entirely of non-`IRadResistantBlock` blocks). Represented by
  exactly one `double` in a flat `uniformRads[chunkId*16 + sectionY]` array — no `SectionRef` object is
  even allocated. This is the fast, overwhelmingly common case; the whole diffusion sweep has a
  branch-free fast path (`kinds == 0x55555555`, i.e. every section in the column is UNI) that skips
  all pocket-aware bookkeeping.
- **`KIND_SINGLE` (2)** — resistant blocks exist, but the flood fill still finds only **one** connected
  open pocket (e.g. a hollow shell with a doorway, or a single room with `IRadResistantBlock` walls).
  Still one `double` (reuses the same `uniformRads` slot as UNI), plus a `SingleMaskedSectionRef`
  holding the pocket's true `volume` (< 4096), a packed per-face open-cell count
  (`packedFaceCounts`, 9 bits × 6 faces) and a centroid (`cx,cy,cz`) used for the diffusion-area/
  distance terms below.
- **`KIND_MULTI` (3)** — ≥2 disconnected pockets in one section (two separate rooms sharing a subchunk).
  A `MultiSectionRef` holds `short[4096] pocketData` (per-block pocket-index lookup, `NO_POCKET=-1` for
  resistant cells), and `double[pocketCount*2]` interleaved `(density, 1/volume)` pairs, plus a
  per-pocket `float[pocketCount*6]` face-distance table for the same diffusion formula.

Kind selection is **fully re-derived** every time a section is rebuilt (`rebuildChunkPocketsLoaded`):
`computePocketMappingForRebuild` calls `scanResistantMask` (a hand-unrolled walk of the section's real
`ExtendedBlockStorage`/`BlockStateContainer`, branching on which of Forge 1.12's three palette
implementations is in play, with a per-thread `STATE_CLASS` cache mapping global block-state id →
"is `IRadResistantBlock`?" to avoid repeated `instanceof` checks) to build a 4096-bit resistant mask,
then `floodFillPockets` (a plain 6-connected BFS using a `LINEAR_OFFSETS`/`BOUNDARY_MASKS` table to
reject cross-section-boundary neighbors) assigns pocket indices. **Radiation mass is conserved across a
rebuild**: `remapPocketMass` computes the old kind's total mass (density × volume, summed per old
pocket), joins old-pocket → new-pocket by per-block overlap count, and redistributes proportionally —
this is what makes "break the wall between two irradiated rooms" merge their radiation correctly
instead of discarding or duplicating it.

## The tick algorithm

Gated on `GeneralConfig.ENABLE_RADIATION` (CE: `enableRads`) **and**
`GeneralConfig.ENABLE_ADVANCED_RADIATION` (CE: `advancedRadiation`) — both already ported in this repo
with matching semantics and CE-original comments. When `advancedRadiation` is off, none of
`RadiationSystemNT`'s event hooks or tick logic run at all (no persistence, no simulation); the
`incrementRad`/`getRadForCoord`/etc. entry points are *not* separately gated, so on-demand queries
still lazily allocate a `WorldRadiationData` via `worldMap.computeIfAbsent`, they just never decay or
diffuse without the tick loop running.

Once every `RadiationConfig.RAD_TICK_RATE` ticks (`onServerTickLast`, `Phase.END`), across **all**
loaded dimensions in parallel (one `ForkJoinTask` per `WorldServer` on the common `ForkJoinPool`,
`RadiationSystemNT.RAD_POOL`), `WorldRadiationData.processWorldSimulation()` runs:

1. **`rebuildDirtySections()`** — drains the tick's `DirtyChunkTracker` (an open-addressing
   `long chunkKey → (chunkId, 16-bit per-sectionY dirty mask)` hash map, reset by epoch counter rather
   than cleared each tick) and re-runs the flood-fill/kind-selection above for every dirty section, in
   parallel batches (`RebuildDirtyChunkBatchTask`).
2. **`clearQueuedWrites()`** then **`runExactExchangeSweeps()`** — the actual diffusion step. Three
   axis sweeps (X between east/west-neighbor chunk pairs, Z between north/south pairs, Y between
   vertically-adjacent sections in the same column), each parallelized across 4 "buckets" via
   `ForkJoinTask`, with **parity partitioning** so no chunk/section participates in two simultaneous
   exchanges in the same axis (this is the concurrency invariant documented in the class's own
   comment block: server-thread-exclusive mutation never overlaps the async sweep). The **order** of
   X/Z/Y and a same-axis parity flip both rotate every tick (`workEpoch % 6`, keyed off tick count) to
   avoid a fixed sweep-order bias. The actual exchange, for two uniform sections, is a closed-form
   2-node diffusion step (the class's own javadoc): `Δρ = (ρ_eq − ρ) × (1 − e^(−kΔt))`, i.e.
   `r* = (ρ_A/V_B + ρ_B/V_A)/(1/V_A+1/V_B)` (volume-weighted equilibrium), each side relaxing toward
   `r*` by `e^(-k·Δt)` where `k` folds in shared contact area (`packedFaceCounts` for masked/pocket
   faces, a full 256-cell face for uniform-uniform) and inter-centroid distance, and `Δt` derived from
   `RadiationConfig.RAD_DIFFUSIVITY` and `RAD_TICK_RATE` (`diffusionDt = radDiffusivity * (tickRate/20)`,
   `UU_E = e^(-diffusionDt/128)` cached for the common full-face uniform-uniform case).
3. **`postSweepDecayAndEffects()`** — for every section flagged "active" (non-zero radiation), applies
   exponential half-life decay: `next = density * retentionDt`, where
   `retentionDt = e^(ln(0.5) * (tickRate/20) / RAD_HALF_LIFE_SECONDS)` (recomputed once in
   `onLoadComplete()` from config). Then, per pocket: if density exceeds `RadiationConfig.FOG_THRESHOLD`
   (CE: `fogRad`), a per-tick hashed-probability check (derived from `RadiationConfig.FOG_CHANCE`, CE:
   `fogCh`, "1:n chance ... every second") spawns the `RadFog` particle broadcast; if density ≥ `5.0`,
   a fixed 1% per-tick chance queues that exact pocket for `RadiationWorldHandler.handleWorldDestruction`
   (the grass→wasteland/leaves→waste_leaves/sand→trinitite block-decay sweep), one pocket destroyed per
   world per tick when `RAD_TICK_RATE == 1` (a single `pocketToDestroy` slot) or drained from a
   bounded MPSC queue otherwise.
4. **`cleanupAndLog`** — periodic destruction-queue trim and (debug-mode only) profiling.

Chunk load/unload/save all run through ordinary Forge world/chunk events
(`WorldEvent.Load/Unload`, `ChunkEvent.Load/Unload`, `ChunkDataEvent.Load/Save`) — confirmed by
neo-edition's own `ChunkRadiationManager` to have direct 1:1 successors in NeoForge 1.21
(`net.neoforged.neoforge.event.level.{LevelEvent,ChunkEvent,ChunkDataEvent}`,
`net.neoforged.neoforge.event.tick.ServerTickEvent`, subscribed via
`net.neoforged.fml.common.EventBusSubscriber`/`@SubscribeEvent`) — this is the one part of
neo-edition's radiation code this report *does* rely on, purely for confirming those four event
classes/packages still exist with matching semantics, not for any radiation logic.

## Querying ambient radiation (the API every other Phase 4 area calls)

`ChunkRadiationManager.proxy` (CE: an inner `ProxyClass` singleton on the 68-line facade) is the entry
point every non-radiation system already calls, confirmed by the ~20 already-committed call sites
above:

```
double getRadiation(World world, BlockPos pos)                      // read-only; 0 on client or unloaded chunk/section
void    setRadiation(World world, BlockPos pos, double rad)
void    incrementRad(World world, BlockPos pos, double rad)          // uncapped (@DoNotCall — CE marks this "unless you know what you are doing")
void    incrementRad(World world, BlockPos pos, double rad, double max)  // capped variant — this is the one RBMK/hazard/fluid-trait call sites actually use
void    decrementRad(World world, BlockPos pos, double rad)
void    clearSystem(World world)
```

All six delegate straight to `RadiationSystemNT` static methods of nearly the same name
(`getRadForCoord`/`setRadForCoord`/`incrementRad`/`decrementRad`/`jettisonData`), each internally
resolving `BlockPos → section key → owning chunk → section kind → pocket index` and short-circuiting
to `0`/no-op for unloaded chunks, out-of-world-height positions (`isOutsideWorld`, a bit-trick range
check), or positions on a block implementing `IRadResistantBlock` with `isRadResistant(...) == true`
(`isResistantAt`). Separately, `RadiationSystemNT.markSectionForRebuild(World, BlockPos)` /
`markSectionsForRebuild(World, LongIterable)` are the two methods every `IRadResistantBlock`
implementor (and, per this port's own already-committed `SiloHatchBlockEntity`/
`BlockRadResistantPillar`/`ReinforcedLamp`/`CrateBlock` javadoc, several already-ported blocks) must
call on placement/removal — they just add the affected section(s) to the same `DirtyChunkTracker` the
tick loop drains.

## The section bit-packing helpers this needs (not yet ported)

`RadiationSystemNT` leans heavily on a family of `long`/`int` bit-packing helpers living in CE's
`com.hbm.lib.Library` (`blockPosToSectionLong`, `sectionToChunkLong`, `sectionToLong`, `setSectionY`,
`getSectionX`/`Y`/`Z`, `blockPosToLocal`, `getLocalX`/`Y`/`Z`, `blockPosToLong`) — a "section key" is a
packed `long` encoding chunk X/Z and subchunk Y, and a "local index" packs a block's position within
its 4096-block section. **None of these exist yet** in this port's own `com.hbm.lib.Library.java`
(309 lines, 19 static methods currently, confirmed by direct read/grep — zero matches for any of the
above names). This is real net-new infrastructure Phase 4 must add (either into `Library` directly, to
keep the 1:1 package-layout convention other reports have followed, or a small dedicated helper class)
before any of the flood-fill/diffusion code above can be ported. The supporting collection/pool classes
the algorithm also depends on are **already ported and reusable**: `com.hbm.util.SectionKeyHash`,
`com.hbm.lib.TLPool`, `com.hbm.util.ObjectPool`, `com.hbm.util.DecodeException` all exist in this repo
today (confirmed by direct read/find), and `fastutil` (the `Int2LongOpenHashMap`/`Long2IntOpenHashMap`/
etc. collections used throughout) is already a working transitive dependency (14 files in this port
already import it, despite no explicit `build.gradle` line — it ships bundled with vanilla
Minecraft/NeoForge). Only `com.hbm.lib.queues.MpscUnboundedXaddArrayLongQueue` (used solely for the
`RAD_TICK_RATE != 1` batched-destruction-queue path, a minor feature) is confirmed absent and low
priority — the `RAD_TICK_RATE == 1` default path uses a single `long` field instead and needs no queue
at all.

## Deferred scope

Real dependencies and adjacent systems this report does **not** own, even though this system is their
common blocker:

- **`CompatibilityConfig.dimensionRad`** (CE: a `HashMap<Integer,Float>` of per-dimension baseline/
  floor radiation, e.g. permanently-irradiated modded dimensions) feeds directly into
  `WorldRadiationData.minBound` (the per-world floor every `sanitize()` call clamps density to — CE
  reads it once per `WorldRadiationData` construction via `CompatibilityConfig.dimensionRad.get(dim)`).
  **This port's own `CompatibilityConfig.java` explicitly does not port this map** — its class javadoc
  states CE's ~60 dimension-ID-keyed tables (`dimensionRad` named explicitly) are deferred to "whichever
  phase owns world generation, once it knows the real set of dimensions," since Forge integer dimension
  IDs have no 1.21 equivalent. **This is a real, already-decided, named blocking dependency**: until
  world-gen/dimension-registry work lands a `ResourceKey<Level> → float` remapping, this system's
  `minBound` must default to `0.0` for every dimension (CE's own fallback when the map has no entry for
  a dimension), which is a safe, harmless simplification, not a correctness bug — just flag it as
  intentionally deferred, not forgotten.
- **`HbmPotion.mutation`** (immunity short-circuit in CE's `calculateRadiationMod`) — already
  documented as absent by this port's own `ContaminationUtil.java` TODO (a `MobEffect`-registration
  area). Not this report's area; the chunk-radiation engine itself has no dependency on it, only the
  entity-resistance math one layer up in `ContaminationUtil` does.
- **`AuxParticlePacketNT`/`HbmEffectNT`** (generic named-particle-broadcast packet) — confirmed absent
  repo-wide (`GrenadeFillingActions`'s own javadoc: "confirmed not ported anywhere"). Shared with
  several Phase 3 VFX call sites (haze/plasma/mushroom-cloud broadcasts); the `RadFog` effect is one
  more consumer of whatever generic solution eventually lands, or Phase 4 can ship a narrow
  single-purpose payload using the already-confirmed `CustomPacketPayload`/`PacketDistributor.
  sendToPlayersNear` pattern (see Phase-4-safe scope) without waiting on the generic system.
- **`RadVisOverlay`** (the pocket-boundary debug renderer) is a genuine dev/debug tool, not a
  player-facing feature, and per the Headline finding has zero real multiplayer story in CE itself
  (integrated-server-only, direct in-process object access). Recommend treating it as optional/
  out-of-scope for Phase 4's player-facing deliverable, or reimplementing as a genuinely new debug
  command (e.g. a server-side `/radiation query` style command that answers with the same kind/pocket/
  density breakdown via chat, matching the "no packet needed" pattern the rest of this system already
  uses) rather than porting CE's client-reads-server-memory shortcut, which has no legal 1.21 dedicated-
  server equivalent.
- **Whichever area owns pollution/fallout-rain/contamination-effects** (named explicitly in this
  phase's own scope line in `PORT_SPEC.md` §3: "Chunk radiation system (storage, spread, decay, entity
  irradiation, Geiger feedback), **pollution system, fallout rain/effects, contamination effects**
  (`potion` port)...") depends on this system's `getRadiation`/`incrementRad` query surface exactly as
  much as the already-committed Phase 0-3 call sites do, but is out of this document's scope — this
  report only covers the engine itself (`ChunkRadiationManager`/`RadiationSystemNT`/
  `RadiationWorldHandler`), not its downstream consumers beyond the already-existing forward references
  enumerated in the Headline finding.

## Key design/API decisions

Confirmed from real code read in this survey — no NeoForge API invented below without an explicit flag:

- **`ChunkRadiationManager` stays a thin static-proxy facade**, matching CE's own documented intent
  ("this proxy is made to make porting easier... always call `RadiationSystemNT` directly when
  possible" — CE's own class javadoc) and this port's ~20 already-committed call sites that assume
  exactly that shape. Do not merge it into `RadiationSystemNT`.
- **Entity-attachment registration is already a confirmed, working pattern** (`ModAttachments.java`,
  this port): `AttachmentType.builder(Supplier)` + `.serialize(Codec)` + `.sync(StreamCodec)` +
  optionally `.copyOnDeath()`, registered on a `DeferredRegister<AttachmentType<?>>` keyed off
  `NeoForgeRegistries.Keys.ATTACHMENT_TYPES`. **This exact shape for a `ChunkAccess`/`LevelChunk`
  holder is not demonstrated anywhere in this port or in neo-edition** — treat the "attach the compact
  per-chunk byte-payload via a chunk `AttachmentType`" plan as the intended approach per `PORT_SPEC.md`
  §2, but explicitly unverified (see Open questions) with a confirmed-real fallback available.
- **Only the compact binary snapshot needs a chunk-scoped storage hook — not the live simulation
  state.** `WorldRadiationData` (the pocket graph, diffusion scratch buffers, connectivity links) is
  pure per-`WorldServer` runtime memory in CE (`ConcurrentHashMap<WorldServer, WorldRadiationData>
  worldMap`), fully reconstructed on load from (a) the tiny persisted per-pocket-density byte array and
  (b) a fresh flood-fill re-scan of the chunk's actual blocks. **The 1.21 port needs the identical
  split**: a plain `Map<ServerLevel, WorldRadiationData>`-equivalent runtime singleton (no NeoForge
  API needed for this half at all — it is not itself saved, attached, or capability-backed, exactly
  like CE), plus a chunk-scoped attachment/NBT hook holding only the small serialized payload. Do not
  try to model pockets/connectivity themselves inside a `Codec` — CE never persists them either.
- **The confirmed real S2C particle/effect-broadcast pattern for `RadFog`** is
  `record ... implements CustomPacketPayload` + `Type<T>`/`StreamCodec<RegistryFriendlyByteBuf,T>` +
  `@OnlyIn(Dist.CLIENT) static void handleClient(...)`, dispatched server-side via
  `PacketDistributor.sendToPlayersNear(ServerLevel, excludedPlayer, x, y, z, radius, payload)` — this
  port's own `ExplosionEffectSyncPacket`/`ExplosionEffectStandard` (Phase 3, already real and used) is
  the direct 1:1 successor to CE's `PacketThreading.createAllAroundThreadedPacket(payload, new
  TargetPoint(dim, x, y, z, radius))`, confirmed by reading both.
- **CE's three-tier 1.12 block-state palette walk (`REGISTRY_BASED_PALETTE`/
  `BlockStatePaletteLinear`/`BlockStatePaletteHashMap`) does not need to be reproduced 1:1.** It exists
  purely to avoid a per-block `instanceof IRadResistantBlock` check by caching classification at the
  palette-entry level (far fewer palette entries than blocks in a section). 1.21's `PalettedContainer`
  has its own (differently-shaped, not yet verified in this sandbox) tiered palette; the *goal*
  (classify once per distinct palette entry, not once per of the 4096 blocks) is worth preserving for
  performance, but the exact bit-unpacking loop is 1.12-specific plumbing, not behavior, and should be
  re-derived against real 1.21 `PalettedContainer` internals rather than transliterated blind.

## Open questions / risks

- **Does `ChunkAccess`/`LevelChunk` actually implement `IAttachmentHolder` in NeoForge 21.1.228, and
  if so, is `.serialize(Codec)` sufficient for it to round-trip through normal chunk save/load without
  an explicit `ChunkDataEvent` hook (the way `Entity`/`BlockEntity` attachments piggyback on their own
  save/load automatically)?** This sandbox has no NeoForge jar or javadoc to check directly, and
  neither this port nor neo-edition has a working example to confirm from. Based on general
  (unverified-in-this-sandbox) NeoForge 1.21.x knowledge, `ChunkAccess` was added as an
  `IAttachmentHolder` implementor as part of NeoForge's broader "attachments replace Forge
  capabilities everywhere" push, but this claim should be checked against real NeoForge source/javadoc
  (or a compiled jar, once `gradlew` is available) before committing to it. **A confirmed-real fallback
  exists regardless**: CE's own technique (and neo-edition's, independently) of reading/writing the
  payload straight into the `ChunkDataEvent.Load`/`Save` NBT compound (`e.getData().setByteArray(...)`)
  works today with zero new API risk, at the cost of not following `PORT_SPEC.md` §2's stated
  preference literally. Recommend whoever implements this area spend the first hour confirming the
  attachment path against real source before writing the rest of the port around an assumption.
- **1.21's `PalettedContainer` tier count/API shape** for the resistant-mask scan (see Key design
  decisions) is unverified in this sandbox — needs checking against real NeoForge/vanilla source once
  available, not transliterated from CE's 1.12-specific three-tier palette.
- **Performance envelope**: this is a considerably heavier system than a naive port might assume — a
  full `ForkJoinPool`-parallelized, section-flood-fill-based pseudo-fluid simulation running every
  `RAD_TICK_RATE` ticks across every loaded dimension. CE's own code carries an elaborate built-in
  profiler (`GeneralConfig.enableDebugMode`-gated per-step/lifetime timing with percentile buckets,
  read in full above) specifically because this system's cost was clearly a known concern for CE's own
  authors. Phase 4 should preserve at least a coarse version of that profiling hook rather than
  discovering performance problems blind, and should benchmark against a worst-case "many dirty
  sections at once" scenario (e.g. a large explosion) before considering this area done — this overlaps
  with `PORT_SPEC.md` §4's cross-cutting "explosion performance on 1.21 chunk system" risk, since a
  nuke both dirties large numbers of sections at once (via `IRadResistantBlock` removal) and is a major
  `incrementRad` producer.
- **Whether `RadVisOverlay` should be ported at all**, and if so, how — CE's own version has no
  legitimate dedicated-server design (see Deferred scope). This is a product decision (is a
  radiation-debug tool worth new client-server protocol design?) more than a technical blocker, flagged
  here rather than decided.
- **Exact numeric type for the public proxy API** — CE's `ChunkRadiationManager.ProxyClass` uses
  `double` throughout, but several already-committed call sites in this port pass `float` literals
  (`FT_VentRadiation`'s `overflowAmount * radPerMB` is a `float` expression; CE's own neo-edition
  reference also declares its (much simpler) equivalent interface as all-`float`). Implicit
  widening makes this a non-issue at call sites, but worth deciding `double` (matches CE exactly,
  matches `RadiationSystemNT`'s own internal type) vs `float` (matches neo-edition's shallower
  reference and a couple of this port's own call-site literals) once, rather than per-overload.
