# Satellite follow-up dependencies: loot pools, payload entities, advancements — Phase 4 research

Scope note up front: the satellite addressing/dispatch protocol itself
(`com.hbm.saveddata.satellites.Satellite`/`SatelliteSavedData` + all 13 concrete `Satellite*`
classes) is already fully ported (Phase 3's `missile_launch_infra` work) and is **not** re-derived
here. This report covers only the 5 real, specifically-named remaining dependencies that Phase 3's
own review pass flagged as documented forward-reference TODOs inside those already-committed files:
`com.hbm.itempool.ItemPoolsSatellite`, `com.hbm.entity.logic.EntityDeathBlast`,
`com.hbm.entity.logic.EntityOrbitalLaser`, `com.hbm.entity.projectile.EntityTom`, and
`com.hbm.main.AdvancementManager`.

## Sources read in full

CE (`upstream/hbm-ce`), sole source of truth for behavior:
- `src/main/java/com/hbm/itempool/{ItemPool,ItemPoolsSatellite}.java` (104 + 59 = 163 lines) — the
  two files this report's assigned scope actually needs. The other 6 files in the package
  (`ItemPoolsC130`/`Component`/`Legacy`/`Pile`/`RedRoom`/`Single`/`VendingMachine`, 923 lines total)
  were sized (`wc -l`) but not read — out of scope, see Deferred scope.
- `src/main/java/com/hbm/entity/logic/EntityDeathBlast.java` (93 lines)
- `src/main/java/com/hbm/entity/logic/EntityOrbitalLaser.java` (69 lines)
- `src/main/java/com/hbm/entity/projectile/EntityTom.java` (83 lines)
- `src/main/java/com/hbm/main/AdvancementManager.java` (195 lines)
- `src/main/java/com/hbm/saveddata/satellites/{SatelliteMiner,SatelliteLunarMiner,SatelliteLaser,
  SatellitePrecisionLaser,SatelliteHorizons}.java` (5 files, all fully read) — the real CE call
  sites that determine each dependency's exact required contract.
- `src/main/java/com/hbm/entity/logic/EntityTomBlast.java` (105 lines) and
  `src/main/java/com/hbm/entity/effect/EntityCloudTom.java` (72 lines), read in full — these are
  *not* part of this report's assigned 5 classes, but `EntityTom.onUpdate()` spawns both directly,
  so they had to be read to determine what `EntityTom`'s own port actually requires downstream (see
  Deferred scope).
- `src/main/java/com/hbm/explosion/ExplosionTom.java` — sized (164 lines) and skimmed (first 40
  lines read) to characterize it as `EntityTomBlast`'s own real payload dependency; not read in
  full, flagged as a further downstream item, see Deferred scope.
- `src/main/java/com/hbm/handler/{WeightedRandomChestContentFrom1710,WeightedRandomFrom1710}.java`
  (141 + 150 = 291 lines, both read in full) — `ItemPool`'s two supporting classes, read to
  determine exactly what new code this report's scope needs versus what this port already has (see
  Headline finding #6).
- `src/main/java/com/hbm/lib/HbmChestContents.java` (partial — the 3-overload `weighted(...)`
  static factory `ItemPoolsSatellite.init()` calls, read in full; rest of file not read, out of
  scope).
- `src/main/java/com/hbm/config/{CompatibilityConfig,GeneralConfig}.java` (`isWarDim`/
  `enableAdvancements` fields and their real values/comments only, not the full files).
- `src/main/java/com/hbm/main/MainRegistry.java` (grep + 15-line read) — confirmed CE's
  `AdvancementManager.init(...)` call site is `FMLServerStartingEvent`, fired once per server start.

This port's own already-committed code (read in full unless noted, to confirm exact call-site
contracts and existing real infrastructure this report's classes must plug into):
- `src/main/java/com/hbm/saveddata/satellites/{SatelliteMiner,SatelliteLunarMiner,SatelliteLaser,
  SatellitePrecisionLaser,SatelliteHorizons,SatelliteRelay,SatelliteMapper,Satellite}.java` (8
  files) — every one of this report's 5 target classes' real, already-written call sites, plus
  `Satellite.java`'s base dispatch contract (`onOrbit`/`onClick`/`onCoordAction`/`markDirty`,
  confirmed real and unrelated to this report's own gaps).
- `src/main/java/com/hbm/util/{WeightedRandom,WeightedRandomObject}.java` — confirmed real,
  already-ported infrastructure directly relevant to `ItemPool`'s own needs (see Headline finding
  #6).
- `src/main/java/com/hbm/explosion/vanillant/ExplosionVNT.java` (constructor/setter signatures) and
  `.../standard/{BlockAllocatorStandard,BlockProcessorStandard,EntityProcessorCrossSmooth,
  PlayerProcessorStandard,ExplosionEffectWeapon}.java` (constructors + `EntityProcessorCrossSmooth`
  read in full, 45 lines) — confirmed real, already-ported infrastructure `EntityOrbitalLaser.explode()`
  needs (see Headline finding #2).
- `src/main/java/com/hbm/entity/logic/{NukeEntityTypes,EntityExplosionChunkloading,
  EntityNukeExplosionMK5}.java` and `.../effect/EffectEntityTypes.java` and
  `.../projectile/EntityRubble.java` — confirmed real `EntityType`/`DeferredRegister` registration
  precedent and vanilla-base-class precedent (`ThrowableProjectile`) this report's 3 new entities
  should follow.
- `src/main/java/com/hbm/util/DamageResistanceHandler.java` (the 9-value `DamageClass` enum only) —
  confirms `DamageClass.LASER` (needed by `EntityOrbitalLaser.explode()`) is a real, already-ported
  value.
- `src/main/java/com/hbm/config/GeneralConfig.java` (grep for `ENABLE_ADVANCEMENTS`, plus a
  repo-wide grep for the `GeneralConfig.<FIELD>.get()` access pattern) and
  `src/main/java/com/hbm/saveddata/TomSaveData.java` (read in full, 85 lines) — confirms two of this
  report's dependencies (`GeneralConfig`'s advancements toggle, and `EntityTomBlast`'s
  `TomSaveData.forWorld(world)` call) are **already real and ready**, not forward references.
- Repo-wide greps for `isWarDim`, `fluorite`, `moon_turf`, `gravel_diamond`, `BulletConfigSyncingUtil`,
  `EntityBulletBase`, `statFacNoRad`, `setDamageClass`, `ServerStartingEvent` to determine exactly
  which of this report's transitive dependencies already exist in the port and which don't.

Neo Edition (`upstream/neo-edition`) — consulted, found to carry **no relevant Java code** for any
of this report's 5 classes: no `itempool` package, no `EntityDeathBlast`/`EntityOrbitalLaser`/
`EntityTom` equivalents, and no `AdvancementManager`/advancement-granting Java code at all (only
datapack JSON under `src/main/resources/data/hbmsntm/advancement/`). This report's NeoForge-API-shape
claims for `EntityType` registration and `ThrowableProjectile` are therefore cross-checked against
this *port's own* already-committed precedent files (listed above) rather than Neo Edition directly;
`AdvancementManager`'s 1.21.1 API shape (`AdvancementHolder`/`ServerAdvancementManager`/
`PlayerAdvancements#award`) could not be verified against any real code in either reference repo and
is flagged explicitly as well-established Mojang-mapping knowledge, not jar-verified, per this task's
ground rules.

## Headline finding

Six corrections/clarifications to this task's own framing, in descending order of how much they
change the work:

1. **`EntityDeathBlast` and `EntityOrbitalLaser` are confirmed genuinely different classes with
   genuinely different payload shapes, not two names for the same mechanic.** Both files exist
   separately in CE and both were read in full. `EntityDeathBlast` (93 lines) is a 60-tick timer
   entity whose payload — gated behind `CompatibilityConfig.isWarDim(world)` — is a full
   `EntityNukeExplosionMK5.statFacNoRad(...)` nuke blast plus 100 legacy `EntityBulletBase` "laser
   bolt" projectiles fired in a circular fan (`BulletConfigSyncingUtil.MASKMAN_BOLT` ammo), with an
   *unconditional* particle/sound broadcast regardless of the `isWarDim` gate.
   `EntityOrbitalLaser` (69 lines) is a 5-tick timer entity with **no `isWarDim` gate at all** —
   its `explode()` unconditionally runs a single `ExplosionVNT`-based laser burst
   (`EntityProcessorCrossSmooth` with `DamageClass.LASER`, 1000 damage, piercing) and nothing else —
   no nuke, no legacy bullets, no war-dimension dependency whatsoever. These are two structurally
   unrelated payloads that happen to share the "orbital superweapon satellite fires a beam" flavor
   text; do not conflate them or try to write one shared base class beyond what `IConstantRenderer`
   (already ported, an empty marker interface) already gives them for free.
2. **`EntityOrbitalLaser` is dramatically closer to portable-today than its sibling.** Every single
   class its `explode()` method calls — `ExplosionVNT`, `BlockAllocatorStandard`,
   `BlockProcessorStandard`, `EntityProcessorCrossSmooth`, `PlayerProcessorStandard`,
   `ExplosionEffectWeapon`, and `DamageResistanceHandler.DamageClass.LASER` — is **already real,
   already committed, and already exposes exactly the constructor/setter signatures CE's own call
   chain uses**, confirmed by reading each one directly (see Sources). The only two things standing
   between "port `EntityOrbitalLaser.java` today" and done are: (a) the 69-line class itself plus an
   `EntityType` registration (a copy of the already-established `NukeEntityTypes`/
   `EffectEntityTypes` pattern), and (b) one missing setter — `EntityProcessorCrossSmooth` does not
   yet have a `setDamageClass(DamageClass)` method or backing field (confirmed absent by grep), even
   though CE's own version of that class has exactly this setter and `EntityOrbitalLaser.explode()`
   calls it. That file's own doc comment currently says the whole Sedna-damage-model integration was
   skipped because "the Sedna weapon system... [does] not exist in this port yet" — **that comment
   is now stale**: Phase 3 has since landed the real `com.hbm.items.weapon.sedna.*` framework
   (`BulletConfig`, `GunConfig`, `Receiver`, `ItemGunBaseNT`, all confirmed present in
   `src/main/java/com/hbm/items/weapon/sedna/`), so `setDamageClass`'s only real blocker (the
   comment's stated reason) no longer applies. Adding this one setter is a small, mechanical,
   low-risk change that unblocks `EntityOrbitalLaser` completely.
3. **`EntityDeathBlast`'s payload depends on the *legacy* projectile system, which is Phase 4
   mob/boss content owned elsewhere, not this report.** `docs/phase3/gun_framework.md`'s own
   headline finding #1 already identified `com.hbm.items.weapon.ItemGunBase` /
   `com.hbm.entity.projectile.{EntityBulletBase,EntityBullet}` as still-live content used by
   `EntityBOTPrimeBase`, `EntityUFO`, `EntityHunterChopper`, `EntityCyberCrab`, and the
   `EntityAIMaskman*` AI classes, and explicitly deferred porting it to "whichever phase researches
   those mobs." `EntityDeathBlast.onUpdate()`'s war-dim branch is a **sixth** consumer of that same
   legacy system (`new EntityBulletBase(world, BulletConfigSyncingUtil.MASKMAN_BOLT)`, confirmed by
   reading the file) — this report inherits that same dependency by association but does not own
   resolving it; see Deferred scope.
4. **This port already has an established, precedent-setting policy for CE's `isWarDim` gates
   (drop the gate, run the guarded branch unconditionally) — but `EntityDeathBlast`/`EntityTom` are
   the first case where that policy's *content*, not just its gate, is itself unported.**
   `ExplosionLarge.java`'s own doc comment states plainly: "CE's `isWarDim` gates on
   `jolt`/`explodeFire`/`buster` are dropped per this port's documented always-true default," and
   `EntityCloudFleija.java`/`ExplosionNukeAdvanced.java` independently confirm the same call. The one
   documented exception is `ItemMultitoolPassive`'s `Rung#MEGA`, where the guarded content itself
   (`ExplosionChaos.levelDown`) is *also* unported, so that rung is left a permanent no-op rather
   than "always true over nothing." `EntityDeathBlast`/`EntityTom` are in that same second bucket —
   applying "always-true" to their gates does not by itself unblock anything, because the
   gated *content* (legacy bullets for `EntityDeathBlast`; `EntityTomBlast`/`ExplosionTom` for
   `EntityTom`) is unported regardless of the gate's resolution. Flagged as a real design-consistency
   question to answer explicitly rather than silently drift from the established policy — see Open
   questions.
5. **`ItemPoolsSatellite`'s two pools do not fully compile against this port's current item/block
   registry, and this port's own already-committed `SatelliteMiner`/`SatelliteLunarMiner` use the
   *wrong* pool-key strings.** Three of `POOL_SAT_MINER`/`POOL_SAT_LUNAR`'s ~32 combined weighted
   entries reference content that is confirmed absent from this port: `ModItems.fluorite` (only the
   higher-tier `crystal_fluorite` exists — already flagged as a gap by an unrelated file,
   `GasCentrifugeRecipes.java`), `ModBlocks.moon_turf`, and `ModBlocks.gravel_diamond` (both
   confirmed by repo-wide grep to have zero registration and, for `gravel_diamond`, only two
   unrelated comments mentioning the name). Separately — and unrelated to those 3 missing items —
   this report's own already-committed `SatelliteMiner.java`/`SatelliteLunarMiner.java` register
   their `CARGO` map with the literal strings `"sat_miner"`/`"sat_lunar"`, but CE's real
   `ItemPoolsSatellite.POOL_SAT_MINER`/`POOL_SAT_LUNAR` constants equal `"POOL_SAT_MINER"`/
   `"POOL_SAT_LUNAR"` (confirmed by reading both CE's `ItemPoolsSatellite.java` and CE's own
   `SatelliteMiner`/`SatelliteLunarMiner`, which call `registerCargo(SatelliteMiner.class,
   ItemPoolsSatellite.POOL_SAT_MINER)` — not a hand-typed literal). Porting `ItemPoolsSatellite`
   verbatim today would silently produce a pool lookup that never matches this port's existing
   `CARGO` keys. This needs an explicit reconciliation (either constant), not an assumption that the
   strings already line up — see Open questions.
6. **The CE loot-pool "framework" (`ItemPool`, `WeightedRandomChestContentFrom1710`,
   `WeightedRandomFrom1710`) is largely redundant with infrastructure this port already has.**
   `WeightedRandomFrom1710` (150 lines, read in full) is a byte-for-byte functional duplicate of
   vanilla's own (1.21-removed) `net.minecraft.util.WeightedRandom` static picker methods — and this
   port already carries a confirmed, real, cross-checked-against-Neo-Edition shim for exactly that
   gap: `com.hbm.util.WeightedRandom` (its own doc comment: "This is a self-contained shim
   reproducing its exact old API... following the shim Neo Edition already vendored for the same
   purpose"), already consumed by `com.hbm.util.WeightedRandomObject` and by unrelated Phase-1/2/3
   code (`SilexBlockEntity`, `SILEXRecipes`). Only `WeightedRandomChestContentFrom1710`'s narrow
   subset actually used by `ItemPool.getStack` (an `ItemStack` + min/max count + weight POJO, plus
   the `generateStacks(rand, source, min, max)` random-count-splitter static method) needs a new
   equivalent class — the container-filling half of that file
   (`generateChestContents`/`func_92080_a`, both unused by `ItemPool`) does not need porting for this
   report's scope at all. Recommend building `ItemPool`'s pool-entry type on top of the existing
   `com.hbm.util.WeightedRandom.Item` base (the same base `WeightedRandomObject` already extends)
   rather than porting a second, parallel weighted-random implementation.

## Phase-4-safe scope

All class/line counts are from the CE files actually read.

| Class | Lines | Portability |
|---|---|---|
| `ItemPool` | 104 | The pool-registry framework. `pools` (a static `HashMap<String, ItemPool>` populated by construction, mirroring `RBMKColumn`'s and `BulletConfig`'s own registry-by-construction pattern from earlier phases), `getPool(name)` (returns a `backupPool` fallback — 4 hardcoded vanilla-bread/stick/scrap/dust entries — if the name is unregistered, never throws), and `getStack(pool, rand)` (rolls one weighted entry, then a random count in `[min, max]`) are the only 3 methods this report's scope needs. `add(...)`/`build()` (a separate fluent-builder path) and `writeLootTable(...)` (vanilla `ILootContainer`/`TileEntityLockableLoot` integration, an unrelated CE loot-table feature) are not used by `ItemPoolsSatellite` and can be ported later alongside whichever of the other 6 pool files needs them. Per Headline finding #6, only a small new pool-entry POJO is needed, not a full port of `WeightedRandomChestContentFrom1710`. |
| `ItemPoolsSatellite` | 59 | Exactly 2 static pool definitions (`POOL_SAT_MINER`, 26 entries; `POOL_SAT_LUNAR`, 7 entries), each a one-time `new ItemPool(name) {{ this.pool = new WeightedRandomChestContentFrom1710[] {...}; }}` double-brace initializer. 23 of `POOL_SAT_MINER`'s 26 entries and all 7 of `POOL_SAT_LUNAR`'s reference items/blocks already confirmed present in this port (spot-checked every distinct item/block name against `BilletPowderItems`/`PlateCrystalWasteItems`/vanilla `Items.REDSTONE`). 3 entries are blocked on missing content (`fluorite`, `moon_turf` ×3 weighted rolls, `gravel_diamond`) — see Headline finding #5 and Open questions. Every call in this file passes `meta = 0` for the `(Item, meta, min, max, weight)` overload — CE's 1.12 metadata-subtype concept is dead weight here and can be dropped from the port's equivalent overload entirely for this specific file (though the other 6 unread pool files may still need it, not resolved here). |
| `EntityDeathBlast` | 93 | `SatelliteLaser`'s payload (`SAT_PANEL`, 5-minute cooldown gate, `onClick`/`onCommandImpl` dual entry point — protocol already real in this port's `SatelliteLaser.java`, only `deathBlast(...)`'s body is a documented no-op stub today). A pure `Entity` (not a projectile) that self-destructs after `maxAge = 60` ticks; on death, server-side only: an unconditional `PacketThreading`-broadcast particle effect (`HbmEffectNT.Muke`) and explosion sound, then — **only if `CompatibilityConfig.isWarDim(world)`** — spawns `EntityNukeExplosionMK5.statFacNoRad(...)` (already real, confirmed by this port's own `DetCord.java`/`NukeN2Block.java`/`NukeCustomBlock.java`/`ExplosionLarge.java` call sites) plus 100 legacy `EntityBulletBase` "laser bolt" projectiles in a circular fan using `BulletConfigSyncingUtil.MASKMAN_BOLT` ammo. The particle/sound/self-destruct half is portable today with zero new dependencies; the nuke+bullet-fan half needs the legacy bullet system (Deferred scope) plus an explicit `isWarDim` policy call (Open questions). `detonator` (`EntityPlayerMP` in CE) threads straight through to `EntityNukeExplosionMK5.setDetonator(Entity)` (confirmed already accepts a generic `Entity`, so a `ServerPlayer` — or `null`, per `SatelliteLaser.CMD_FIRE`'s command-triggered call — needs no adapter). |
| `EntityOrbitalLaser` | 69 | `SatellitePrecisionLaser`'s payload (`SAT_COORD`, entity-tracking, protocol already real in `SatellitePrecisionLaser.java`). A pure `Entity` that self-destructs after `maxAge = 5` ticks; its entire payload is one `explode()` call building an `ExplosionVNT` with a `BlockAllocatorStandard`/`BlockProcessorStandard`/`EntityProcessorCrossSmooth(1, 1000F).setupPiercing(50F, 0.5F).setDamageClass(DamageClass.LASER)`/`PlayerProcessorStandard`/`ExplosionEffectWeapon(15, 3.5F, 1.25F)` pipeline. **This entire pipeline already exists and compiles in this port** except one missing setter (Headline finding #2) — the single most "ready to port today" class of the 5. No `isWarDim` gate exists in CE for this class at all — its payload always fires. |
| `EntityTom` | 83 | `SatelliteHorizons`'s payload (`SAT_COORD`, one-shot "already used" flag, protocol already real in `SatelliteHorizons.java`). CE extends vanilla `EntityThrowable` but **entirely overrides its motion integration by hand** (`onUpdate()` manually sets `lastTickPos*`/advances position/hardcodes `motionY = -0.5` every tick — a straight-down "falling star" descent, not real throw physics) and its own `onImpact(RayTraceResult)` override is empty — CE never uses `EntityThrowable`'s raytrace-driven impact dispatch at all, only its constructor/base-`Entity` plumbing. This port's own `EntityRubble` (`docs/phase3/melee_weapons.md`'s scope) already established the real 1.21.1 precedent for a CE `EntityThrowable` subclass: extend vanilla `net.minecraft.world.entity.projectile.ThrowableProjectile` directly rather than this port's own, differently-scoped `EntityThrowableNT` (the Sedna ballistics-core class) — the same recommendation applies here, though `EntityTom`'s manual motion override means it barely uses `ThrowableProjectile`'s own `tick()`/`onHit()` machinery either, just its `Entity` base plus `setDeltaMovement`-equivalent state. Every 100 ticks it plays a chime sound; when it reaches a non-air block or `posY < 10`, it self-destructs and — **again gated on `CompatibilityConfig.isWarDim(world)`** — spawns `EntityTomBlast` and `EntityCloudTom` (both unported; see Deferred scope). The outer wrapper (spawn, descend, chime, detect-ground, self-destruct) is small and portable with zero blockers beyond the base-class/registration decision; the actual "gerald has landed" payload is 100% behind the `isWarDim` branch and the two unported entities. |
| `AdvancementManager` | 195 | ~90% a flat list of ~65 `public static Advancement` (CE-1.12-era vanilla type) field declarations, resolved once via `load(adv, path)` inside `init(MinecraftServer serv)` (a `Objects.requireNonNull`-guarded `ResourceLocation` lookup against the server's advancement manager, gated on `GeneralConfig.enableAdvancements` — confirmed already ported and real in this port as `GeneralConfig.ENABLE_ADVANCEMENTS` (a `ModConfigSpec.BooleanValue`, confirmed by grep of this port's own `.get()`-call convention used everywhere else, e.g. `ContaminationUtil.java`/`HazardRegistry.java`)). `grantAchievement(EntityPlayerMP, Advancement)` loops `player.getAdvancements().getProgress(a).getRemaningCriteria()` and calls `grantCriterion(a, s)` per remaining criterion (the "grant the whole advancement, not just start progress" idiom); `hasAdvancement(...)` is a one-line `isDone()` check. Only 3 of the ~65 fields are relevant to this report's 2 satellite call sites: `horizonsStart`/`horizonsEnd` (`SatelliteHorizons.onOrbit`/`theHorizons`, both currently documented no-op TODOs in this port) and `achFOEQ` (`SatelliteRelay.onOrbit`, likewise a documented no-op TODO). The class itself is trivial; the real work is confirming the 1.21.1 API replacement (see Key design decisions — not jar-verified in this sandbox). |

## Deferred scope

Real dependencies of *this specific* subsystem that belong to other packages/phases:

- **The legacy `com.hbm.items.weapon.ItemGunBase`/`com.hbm.entity.projectile.EntityBulletBase`
  system** (`EntityDeathBlast`'s war-dim payload calls `new EntityBulletBase(world,
  BulletConfigSyncingUtil.MASKMAN_BOLT)` 100 times per detonation) — already identified and deferred
  by `docs/phase3/gun_framework.md`'s headline finding #1 as live content for
  `EntityBOTPrimeBase`/`EntityUFO`/`EntityHunterChopper`/`EntityCyberCrab`/the two `EntityAIMaskman*`
  AI classes, "whichever phase researches those mobs" — this report is one more, seventh consumer of
  that same deferred dependency, not a new place to resolve it. `BulletConfigSyncingUtil.MASKMAN_BOLT`
  itself (confirmed real in CE, resolving to `GunNPCFactory.getMaskmanBolt()`) was not read in detail
  here — it belongs entirely to that same deferred package.
- **`com.hbm.entity.logic.EntityTomBlast` (105 lines) and `com.hbm.explosion.ExplosionTom` (164
  lines, sized/skimmed only, not read in full)** — `EntityTom`'s own real "impact" payload.
  `EntityTomBlast extends EntityExplosionChunkloading` (already real and ported in this port,
  confirmed by reading it in full — `IChunkLoader`/`setChunkForced` already wired) and calls
  `TomSaveData.forWorld(world)` (**already real and ported** in this port, alongside `BlockDirt`,
  confirmed by reading `src/main/java/com/hbm/saveddata/TomSaveData.java` in full) — those two
  pieces are ready. `ExplosionTom` itself, however, is a genuinely separate, moderately-sized
  expanding-shell block-conversion algorithm (an incremental spiral/shell walk with `n`/`nlimit`/
  `shell`/`leg`/`element` state, per its own NBT field list) that was not read past its field
  declarations in this survey — it needs its own dedicated read before `EntityTomBlast` can be
  ported. This belongs with whichever pass finishes out the explosion-processor family
  `docs/phase3/explosion_engine.md` already scoped (it is a sibling of `ExplosionLarge`/
  `ExplosionNukeAdvanced`/`ExplosionNukeRayBatched`, not a satellite-specific class), not with this
  report.
- **`com.hbm.entity.effect.EntityCloudTom` (72 lines, read in full)** — `EntityTom`'s cosmetic
  mushroom-cloud companion entity, spawned alongside `EntityTomBlast`. Genuinely small and
  low-risk (a synced `MAXAGE` int, a size-only constructor variant, a `setLastLightningBolt(2)`
  lightning-flash visual cue, self-destructs at `maxAge`) — no blockers of its own. Recommend
  bundling its port together with `EntityTomBlast`/`ExplosionTom` rather than with this report's own
  5 classes, since it has no independent value without them (nothing calls it except `EntityTom`).
- **`com.hbm.config.CompatibilityConfig.isWarDim`** — already a settled, documented, **deliberately
  not-ported** concept in this port (`CompatibilityConfig.java`'s own class javadoc: keyed by CE's
  legacy Forge-1.12 integer dimension id, "a concept that no longer exists in 1.21"). This report
  does not re-litigate that decision — see Key design decisions for how the port's existing
  "always-true default" policy applies (or doesn't fully resolve anything) for `EntityDeathBlast`/
  `EntityTom` specifically.
- **`ModItems.fluorite`, `ModBlocks.moon_turf`, `ModBlocks.gravel_diamond`** — 3 missing
  items/blocks blocking full-parity porting of `ItemPoolsSatellite`'s 2 pools (Headline finding #5).
  `fluorite` is Phase 1 item-registration scope (a plain byproduct item; `crystal_fluorite` already
  exists as a documented substitute pattern per `GasCentrifugeRecipes.java`). `moon_turf`/
  `gravel_diamond` are world-generation/decorative-block content with no owning Phase 4 area named
  yet in this port's own status docs — flagged for whoever eventually researches Moon-dimension or
  space-related block content, not resolved here.
- **`com.hbm.handler.pollution.PollutionHandler`** — referenced by `SatelliteMapper`'s
  `CMD_GETSMOG` command (a documented no-op stub in this port's already-committed
  `SatelliteMapper.java`). Explicitly **not** one of this report's assigned 5 dependencies (the task
  brief names only `ItemPoolsSatellite`/`EntityDeathBlast`/`EntityOrbitalLaser`/`EntityTom`/
  `AdvancementManager`) — mentioned here only so it isn't mistaken for something this report resolved.
  No Phase 4 area has been named as its owner yet; flagged for whoever researches a pollution/smog
  simulation system.
- **The other 6 `com.hbm.itempool.ItemPools*` files** (`C130`, `Component`, `Legacy`, `Pile`,
  `RedRoom`, `Single`, `VendingMachine` — 7 files if counting `VendingMachine` separately, 923 lines
  combined, sized but not read) — share the same `ItemPool` framework this report ports, but their
  own content (crashed-C130 loot, generic component-drop tables, legacy dungeon-chest tables,
  Chicago-pile byproducts, red-room dungeon loot, single-item convenience pools, vending-machine
  stock) belongs to whichever content areas eventually need them. Not re-derived here; this report
  only needs `ItemPool` itself plus `ItemPoolsSatellite`.
- **`ItemPool.writeLootTable(...)`** (vanilla `ILootContainer`/`ILootContainerModifiable`/
  `TileEntityLockableLoot` integration) — an unrelated CE feature (assigning a *vanilla* loot table
  to a tile entity) bundled into the same file as the pool framework this report needs, but not
  called by `ItemPoolsSatellite`/`SatelliteMiner`/`SatelliteLunarMiner` at all. Deferred to whichever
  package needs vanilla loot-table assignment on a tile entity, if any.
- **The "asteroid miner delivery mechanism"** that actually calls `SatelliteMiner.getCargo()` and
  resolves it through `ItemPool.getStack(...)` — per this task's own framing and this port's already-
  committed `SatelliteMiner.java` javadoc, this consumer is **not** part of the satellite-addressing
  package and was not located or researched in this pass (a repo-wide grep found no existing
  call site for `getCargo()` outside `SatelliteMiner`/`SatelliteLunarMiner` themselves). Whoever
  implements the actual "satellite drops cargo" world mechanic needs to find/port that CE consumer
  separately.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code for
NeoForge API shape — no NeoForge API is invented below except where explicitly flagged as
Mojang-mapping knowledge not verified against a real jar):

- **New `EntityType`s follow the exact `NukeEntityTypes`/`EffectEntityTypes` per-family
  `DeferredRegister<EntityType<?>>` pattern**, confirmed real and already used 4 times in this port.
  `EntityDeathBlast`/`EntityOrbitalLaser` (both plain `Entity` subclasses in CE, no `setSize` call in
  either, matching `NukeEntityTypes`' own precedent for "nominal, non-colliding size, renders
  nothing of its own") slot into a `com.hbm.entity.logic`-scoped registry file alongside the
  existing `NukeEntityTypes`, or their own small sibling file; `EntityTom` (extends a vanilla
  throwable, `com.hbm.entity.projectile` package) follows `RubbleEntityTypes`'/
  `FallingNukeEntityTypes`' precedent instead. Recommend registering `entity_laser_blast`,
  `entity_orbital_laser`, and `entity_tom_the_moonstone` under those exact CE `@AutoRegister` names
  (per this port's own established policy of dropping the `@AutoRegister` annotation but keeping its
  `name`/`trackingRange` values, confirmed by `docs/phase2/rbmk_reactor.md`'s Key design decisions
  and already followed by `NukeEntityTypes`/`EffectEntityTypes`) — `trackingRange = 1000` for all 3,
  read directly off each CE class's `@AutoRegister` annotation.
- **`EntityTom` should extend vanilla `net.minecraft.world.entity.projectile.ThrowableProjectile`
  directly**, not this port's own `EntityThrowableNT` (a different, Sedna-ballistics-scoped class) —
  the exact same call this port already made for `EntityRubble` (confirmed real, its own javadoc
  citing Neo Edition's `Shrapnel`/`Rocket`/`BombletZeta` as further confirmation this base class is
  the right one for a CE `EntityThrowable` subclass). Unlike `EntityRubble`, `EntityTom`'s own
  `onUpdate()` completely bypasses `ThrowableProjectile`'s stock `tick()`/`onHit()` dispatch (manual
  position update, hardcoded downward drift, empty `onImpact` override) — so the port gets little
  free behavior from the base class beyond `Entity`/`Projectile` plumbing and owner-tracking, but
  there is no reason to invent a different base class either.
- **`EntityProcessorCrossSmooth` needs one small, additive change before `EntityOrbitalLaser` can be
  ported**: a `setDamageClass(DamageClass clazz)` setter plus a backing `protected DamageClass clazz
  = DamageClass.EXPLOSIVE` field (CE's own default), mirroring the `setupPiercing(...)` fluent-setter
  pattern already present in this port's version of the class. This is a small, mechanical addition
  to an already-real file, not a new subsystem — flagged because it's the one concrete missing piece
  keeping `EntityOrbitalLaser.explode()` from compiling verbatim against CE's own call chain (see
  Headline finding #2). The field is presently unused in the port's `attackEntity` override (which
  still uses a fixed `setExplosionSource(...)` damage source per its own documented forward-reference
  comment about the Sedna piercing/confetti model) — wiring `clazz` through to that damage-source
  selection is optional for `EntityOrbitalLaser` to compile and fire correctly (CE's own
  `attackEntity` on this class doesn't consult `clazz` for anything beyond what CE's real
  `EntityDamageUtil.attackEntityFromNT` piercing path would use it for), but doing so properly is
  exactly the kind of "now that Sedna is real, revisit this stale forward reference" cleanup flagged
  in Headline finding #2 — not required for this report's 5 classes, but worth naming so a future
  pass doesn't rediscover it as "surprising".
- **`AdvancementManager`'s 1.21.1 replacement API — flagged as well-established Mojang-mapping
  knowledge, NOT verified against a real compiled jar in this sandbox** (no NeoForge/Minecraft jar
  available, and Neo Edition carries zero Java code touching advancements). Based on well-known
  1.20.2+ Mojang-mapping changes: CE's raw `net.minecraft.advancements.Advancement` fields become
  `net.minecraft.advancements.AdvancementHolder` (a `record(ResourceLocation id, Advancement
  value)`); CE's `serv.getAdvancementManager().getAdvancement(id)` becomes
  `server.getAdvancements().get(ResourceLocation)` (returning `@Nullable AdvancementHolder`, off
  `MinecraftServer#getAdvancements()` → `ServerAdvancementManager`); CE's
  `player.getAdvancements().getProgress(a)` becomes `player.getAdvancements()
  .getOrStartProgress(AdvancementHolder)`; CE's `getRemaningCriteria()` (note CE's own typo) becomes
  the corrected `getRemainingCriteria()`; and CE's `grantCriterion(a, s)` becomes
  `player.getAdvancements().award(AdvancementHolder, String)`. `hasAdvancement`'s
  `getProgress(a).isDone()` shape is expected to carry over unchanged in spirit
  (`getOrStartProgress(holder).isDone()`). **This entire paragraph needs re-confirmation against a
  real compiled 1.21.1/NeoForge jar before implementation** — it is presented as the most likely
  correct shape, not as verified fact, per this task's own ground rules.
- **CE's `AdvancementManager.init(MinecraftServer serv)` hook point (`FMLServerStartingEvent`,
  confirmed by reading `MainRegistry.java`'s real call site) maps onto NeoForge's
  `net.neoforged.neoforge.event.server.ServerStartingEvent`** — a well-established, name-stable event
  across Forge/NeoForge history (not independently verified against a jar in this sandbox, but not a
  contested rename the way `Advancement`→`AdvancementHolder` is). This port has **no existing
  `ServerStartingEvent`/`ServerStartedEvent` listener anywhere yet** (confirmed by repo-wide grep) —
  `AdvancementManager` would be this port's first consumer of that lifecycle hook, a new (small,
  standard) piece of wiring, not a design fork.
- **`GeneralConfig.ENABLE_ADVANCEMENTS.get()` is the confirmed real call this port's `.get()`-on-
  `ModConfigSpec.BooleanValue` convention expects** (grepped and cross-checked against a dozen other
  live call sites, e.g. `ContaminationUtil.java:381`, `LoadedBaseBlockEntity.java:296`) — not a
  cached plain-`boolean` field the way CE's own `GeneralConfig.enableAdvancements` is. Every one of
  `AdvancementManager`'s 3 gate checks (`init`, `grantAchievement`, the `hasAdvancement` doc-comment
  note) should call `.get()` at each check site, matching this port's established pattern rather than
  caching the value once (CE's own `enableAdvancements` field is itself a load-time-cached copy of a
  Forge config value — the `.get()` pattern is this port's already-chosen equivalent, not a new
  design call).
- **`EntityNukeExplosionMK5.setDetonator(Entity)` already accepts a generic `Entity`**, confirmed by
  reading its real signature in this port — `EntityDeathBlast`'s `EntityPlayerMP detonator` field
  (CE) needs no adapter beyond a straight `ServerPlayer`↔`Entity` widening, and CE's own
  `null`-detonator case (`SatelliteLaser.onCommandImpl`'s `CMD_FIRE` branch) passes through
  unchanged since the parameter is nullable in both CE and this port's existing usage.

## Open questions / risks

- **The `isWarDim` "always-true default" policy question is not resolved for `EntityDeathBlast`/
  `EntityTom` specifically**, and per Headline finding #4 the two documented precedents this port
  already set (`ExplosionLarge`'s "always true, guarded content is real" vs.
  `ItemMultitoolPassive`'s "permanent no-op, guarded content is itself unported") point in different
  directions depending on whether the legacy-bullet-fan / `EntityTomBlast` payload is considered
  "worth wiring as a documented forward reference now" or "wait until its own dependencies land."
  Recommend whoever implements these two classes make the choice explicitly (both are individually
  defensible) rather than defaulting silently to whichever this report's own no-op stub style
  implies.
- **The `CARGO` map key mismatch** (this port's committed `"sat_miner"`/`"sat_lunar"` vs. CE's real
  `ItemPoolsSatellite.POOL_SAT_MINER`/`POOL_SAT_LUNAR` = `"POOL_SAT_MINER"`/`"POOL_SAT_LUNAR"`,
  Headline finding #5) needs an explicit fix, not an assumption either side is already correct.
  Recommend changing `ItemPoolsSatellite`'s two constant *values* to match this port's
  already-chosen, already-committed `"sat_miner"`/`"sat_lunar"` strings (simplest fix, zero blast
  radius on other already-written code) rather than editing the two already-committed `Satellite*`
  files — but this is a call for whoever implements it, not settled here.
- **`fluorite`/`moon_turf`/`gravel_diamond` block full-CE-parity porting of `ItemPoolsSatellite`**
  (Headline finding #5). A partial port (register the pool with its other ~29 confirmed-present
  entries now, add the 3 missing ones later once their items/blocks land) is a reasonable
  interim path but changes the pool's effective weight distribution versus CE until they're added —
  flagged as a real, if minor, balance deviation rather than assumed inconsequential.
- **`ExplosionTom` (164 lines) was not read past its field declarations** — it is `EntityTomBlast`'s
  real payload and needs a dedicated read before that class (and therefore `EntityTom`'s full,
  CE-parity impact behavior) can be ported. Flagged explicitly so a future pass doesn't assume it's
  a trivial wrapper the way `EntityCloudTom` is — its own field list (`n`/`nlimit`/`shell`/`leg`/
  `element`, an incremental multi-tick state machine) suggests genuine algorithmic content, not just
  data.
- **`AdvancementManager`'s proposed 1.21.1 API shape is explicitly not jar-verified** (see Key design
  decisions) — this is the single highest-risk item in this report precisely because neither
  reference repo has real code to check it against. Recommend a deliberate, isolated verification
  step (a minimal compile-check against the real NeoForge 21.1.228 jar, once available) before
  writing `AdvancementManager`'s full ~65-field port, rather than discovering an API mismatch only
  after transcribing the whole field list.
- **`EntityProcessorCrossSmooth`'s `attackEntity` override still uses a fixed vanilla explosion
  damage source and ignores `pierceDT`/`pierceDR`/the `clazz` field even after `setDamageClass` is
  added** (Key design decisions) — porting `EntityOrbitalLaser` today would compile and detonate
  correctly, but its damage would not yet carry CE's real armor-piercing/damage-class behavior the
  way CE's own `EntityDamageUtil.attackEntityFromNT` provides. This is a pre-existing, already-
  documented gap in `EntityProcessorCrossSmooth` (not something this report introduces), but worth
  re-flagging now that its stated reason ("Sedna doesn't exist yet") is stale — whoever owns the
  explosion-engine package should decide whether to wire the real piercing model through now that
  Sedna is real, independent of whether `EntityOrbitalLaser` itself gets ported first.
