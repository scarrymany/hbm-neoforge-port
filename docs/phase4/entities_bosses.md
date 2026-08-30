# CE boss-tier entities — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/botprime/*.java` (7 files, 821 lines —
  `EntityBOTPrimeBase`, `EntityBOTPrimeBody`, `EntityBOTPrimeHead`, `EntityBurrowingNT`,
  `EntityWormBaseNT`, `WormMovementBodyNT`, `WormMovementHeadNT` — the entire "Balls-o-Tron" worm
  boss)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/{EntityMaskMan,EntityQuackos,EntityUFO,
  EntityUFOBase,EntityHunterChopper,EntityRADBeast}.java` (6 files, 1,690 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/ai/{EntityAIMaskmanLasergun,
  EntityAIMaskmanMinigun,EntityAIMaskmanCasualApproach,EntityAINearestAttackableTargetNT}.java`
  (4 files, 393 lines — the boss-specific AI goals; the other 8 files in this package are general
  mob AI out of this report's scope, see Deferred scope)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityBulletBase.java` (684 lines, full —
  the "MK2" legacy bullet, `@AutoRegister(name = "entity_bullet_mk2")`) and
  `.../EntityBullet.java` (partial, ~150 of 809 lines: header, the `chopper`-tagged constructor at
  line 184, and the `CHOPPER`/`TAU`/`CRITICAL` data-parameter fields; `@AutoRegister(name =
  "entity_bullet")`, CE's oldest bullet class)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/BulletConfigSyncingUtil.java` (partial, ~90 of 285
  lines: the append-only id registry pattern and the exact `WORM_BOLT`/`WORM_LASER`/`UFO_ROCKET`/
  `MASKMAN_*` entries) and `.../handler/guncfg/GunNPCFactory.java` (partial, ~50 of 344 lines:
  `getWormBolt`/`getWormHeadBolt`/`getRocketUFOConfig`)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/BossSpawnHandler.java` (250 lines, full — the
  world-tick spawn-roll dispatcher for MaskMan/FBI raids/RAD-Beast "elementals"/meteor strikes)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/BlockBallsSpawner.java` (46 lines, full —
  the worm boss's structure-summon block) and `.../world/generator/{JungleDungeon,
  JungleDungeonStructure}.java` (64 lines combined, full — the world-gen dungeon that places it)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/ItemChopper.java` (162 lines, full — CE's
  summon-item for chopper/worm/UFO/duck) and `.../items/tool/ItemPeas.java` (44 lines, full — the
  Quackos-despawn item)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/EntityEffectHandler.java` (partial, ~45 lines
  around the radiation-mutation dispatch table: the `EntityDuck → EntityQuackos` branch at ≥200 rad)
- `upstream/hbm-ce/src/main/java/com/hbm/main/AdvancementManager.java` (partial: the full
  `Advancement` field list and the `bossMeltdown`/`bossMaskman`/`bossWorm`/`bossUFO`/`bossCreeper`
  `load()` calls)
- Repo-wide greps to confirm scope exhaustiveness: `BossInfo|IBossDisplayData` (finds exactly 5
  hits, see Headline finding), `isNonBoss` (1 override outside render code), `new EntityBulletBase(`/
  `new EntityBullet(` (every consumer, to map shared-dependency edges), `extends EntityUFOBase`,
  `AdvancementManager.grantAchievement`, `addSpawn`/`EntityRegistry` for these mobs (none found —
  see Headline finding on spawn mechanism)
- This port's own `src/main/java/com/hbm/{items/special/ItemChopper.java, items/special/
  SpecialItems.java, util/ContaminationUtil.java, lib/HBMSoundHandler.java, config/MobConfig.java}`
  (read in full/grepped) and `docs/phase3/gun_framework.md` (read in full — its headline finding #1
  and Deferred-scope entry naming `EntityBOTPrimeBase` are this report's starting point, see below)
- `upstream/neo-edition/src/main/java/com/hbm/{entity/mob/CreeperNuclear.java (full),
  entity/NtmEntityTypes.java (registration lines grepped), main/CommonEvents.java
  (EntityAttributeCreationEvent handler, grepped)}` — **cross-referenced for confirmed NeoForge
  1.21.1 mob-registration API shape only**; Neo Edition has ported **zero** boss content (its
  `entity/mob` package contains only `Duck.java`/`CreeperNuclear.java`), so nothing below about
  boss-bar wiring specifically is Neo-Edition-confirmed — flagged explicitly where that matters, per
  this task's own ground rules

## Headline finding

`docs/phase3/gun_framework.md` correctly flagged `EntityBOTPrimeBase` as a real, specific class
still depending on the legacy pre-Sedna bullet system, and correctly named the AI classes
(`EntityAIMaskmanLasergun`/`Minigun`) as forward references — but its own survey (a gun-framework
report, not a mob report) undercounted what those threads actually connect to. Four corrections:

1. **CE has 5 boss-bar-tagged entities, not 1, and they form 3 real fights + 1 "boss-tier elite" +
   1 joke.** A repo-wide grep for `BossInfo|IBossDisplayData` returns exactly 5 hits, all in
   `com.hbm.entity.mob`: `EntityBOTPrimeHead` (green bar — the worm's head is the one boss-bar
   instance among its 75 body segments, see #2), `EntityMaskMan` (purple bar — gun_framework.md
   named this entity's *AI classes* as forward references but never identified `EntityMaskMan`
   itself as a second boss), `EntityUFO` (red bar), `EntityHunterChopper` (purple bar, plus
   `setDarkenSky(true)` and an explicit `isNonBoss() { return false; }` override — vanilla's
   "counts as a boss" marker, the only override of that method found in this survey outside render
   code), and — genuinely unexpected — `EntityQuackos` (purple bar), a secret/joke "boss" that is a
   25×-scaled, **invulnerable** (`getIsInvulnerable()` hardcoded `true`, `setHealth` refuses any
   decrease) mutated duck you can ride, comment-tagged `/** BOW */` throughout by its author and
   despawned by feeding it a `peas` item rather than fighting it. It has no attack of any kind. This
   report treats BOTPrime-worm/MaskMan/UFO as the 3 real boss *fights*, HunterChopper as a boss-tier
   hostile (has a bar and `isNonBoss()=false`, but is really an armed vehicle mob, not a "kill this
   for loot" encounter — see its own row below), and Quackos as a decorative pseudo-boss that exists
   for the joke, not the fight.
2. **The worm "boss" is not one entity — it's 75 independently-full-health entities chained
   together, and killing the wrong one doesn't end the fight.** `EntityBOTPrimeBase.
   applyEntityAttributes` sets `MAX_HEALTH = 15000` unconditionally for *every* subclass —
   `EntityBOTPrimeHead` **and** all 74 `EntityBOTPrimeBody` segments each independently have 15,000
   HP (not 15,000 split across the worm; 15,000 × 75 ≈ 1.1M cumulative HP if you tried to kill every
   segment). The boss bar only ever tracks the head's own HP/maxHP. The real "kill condition" is
   entirely about the head: body segments self-destruct on a decoupling check
   (`EntityBOTPrimeBody.updateAITasks`: once a segment's forward chain-link target dies or goes
   missing, `didCheck` flips true and the segment either explodes at a 1-in-60-per-tick roll or bleeds
   out at 1999 HP/tick) — so the practical fight is "damage the head enough to kill it; the body
   trails along and unravels afterward," not "grind through 74 separate 15,000-HP segments." Get this
   right in the port: naively summing the worm's "boss health" as 75×15,000 would be a real balance
   bug, and treating each segment as independently lethal-to-need-killing would make the fight
   unwinnable by design.
3. **`EntityUFOBase` is a naming trap — it does not back `EntityUFO`.** Exactly like
   `docs/phase3/gun_framework.md` found for `ItemGunBaseSedna` (an abandoned draft superseded by a
   same-package sibling with a different name), `EntityUFOBase` (`abstract ... extends EntityFlying
   implements IMob`, 207 lines, its own waypoint/scan/course-change AI) has exactly one subclass in
   CE — `EntityFBIDrone`, the small reconnaissance quadcopter from FBI raids — and it is **not**
   extended by `EntityUFO`, which extends `EntityFlying` directly and reimplements its own
   independent (and behaviorally different: `DataParameter<BlockPos> WAYPOINT` vs. `EntityUFOBase`'s
   three separate int params, secondary-target list, beam-abduction state, laser/rocket attack
   cycle) waypoint logic from scratch. Do not port `EntityUFOBase` as part of "the UFO boss" — it
   belongs with `EntityFBIDrone`/FBI-raid content instead (see Deferred scope), and porting `EntityUFO`
   needs no shared base class at all.
4. **This report's own bosses are also named, in-repo, forward references Phase 1/3 already left
   for exactly this phase — this is not a hypothetical gap.** Two already-committed, already-compiling
   files in this port name these mobs explicitly and are waiting on them:
   - `src/main/java/com/hbm/items/special/ItemChopper.java`'s own doc comment: *"No entity system
     has been ported through Phase 1 ... `EntityHunterChopper`/`EntityUFO`/`EntityBOTPrimeHead`/
     `EntityDuck` do not exist yet to spawn. Registers as a plain item for now."* — the summon item
     already exists (`spawn_chopper`/`spawn_worm`/`spawn_ufo`/`spawn_duck` in `SpecialItems.java`,
     confirmed registered) with its `use`/`useOn` entity-placement logic stubbed out pending this
     exact report.
   - `src/main/java/com/hbm/util/ContaminationUtil.java` carries two explicit named TODOs:
     `isRadImmune` — *"CE's `immuneEntities` array also lists these two HBM-custom mobs
     (`EntityCreeperNuclear`/`EntityQuackos`); neither exists in this port yet"* — and
     `applyDigammaData` — *"CE also exempts this HBM-custom mob (`EntityQuackos`); doesn't exist in
     this port yet."* Once `EntityQuackos` is ported, these are two literal `instanceof EntityQuackos`
     lines to add back into an already-existing, already-compiling file — nothing else in
     `ContaminationUtil` needs to change. (`EntityMaskMan`/`EntityUFO`/`EntityHunterChopper`/
     `EntityRADBeast` need **no** `ContaminationUtil` change at all: they implement CE's
     `IRadiationImmune` marker interface, which Phase 0/3 already wired into `isRadImmune` as
     `e instanceof IRadiationImmune` — radiation immunity for those four is automatic the moment they
     exist and implement that already-real interface.)
   - Every sound event these five bosses need (`ballsLaser`, `megaquacc`, `nullChopper`,
     `chopperDamage`/`Charge`/`Drop`/`CrashingLoop`, `nullCrashing`, `ufoBeam`, `richard_fire`,
     `osiprShoot`, `hkShoot`, `calShoot`, `teslaShoot`, `bombDet`, `metalStep`, `geigerSounds()`) is
     **already registered** in this port's `com.hbm.lib.HBMSoundHandler` — confirmed by direct grep,
     zero gaps. Audio is a fully solved dependency for this report; only `.ogg` asset files (a Phase 5
     concern) remain outstanding.

## Phase-4-safe scope

### The worm boss ("Balls-o-Tron Prime" / BOTPrime)

| Class | Lines | Portability |
|---|---|---|
| `EntityBurrowingNT` (abstract, `extends EntityCreature`) | 60 | The shared movement base for every worm segment: no fall damage (`fall`/`updateFallState` no-ops), `isOnLadder()` hardcoded false, and a `travel()` override that applies one of two drag constants (`dragInAir`/`dragInGround`, set per-instance in `EntityBOTPrimeBase`'s constructor) depending on whether the segment is inside an opaque block/water/lava — body segments (`!getIsHead()`) get an extra ×0.9 drag multiplier on top. Trivially portable once `Mob`/`PathfinderMob`'s `travel(Vec3)` signature is confirmed (1.21.1 renames `moveRelative`/`move` slightly but the shape is unchanged). |
| `EntityWormBaseNT` (abstract, `extends EntityBurrowingNT`) | 202 | The shared worm-segment contract: `headID`/`partNum` (int fields, not synced — every segment resolves its head via `world.getEntityByID(headID)`, a live-lookup pattern that needs re-checking against 1.21.1's entity-UUID-first idioms, see Open questions), a `wormSelector` static `Predicate<Entity>` (`instanceof EntityWormBaseNT`), and `attackEntityFrom` redirect logic: **damage dealt to any body segment is redirected to `this.targetedEntity` (its forward chain-link, which for the head-most body segment is the head itself)** unless the attacker is itself part of the same worm (checked via `headID` equality) — this is the actual mechanism that makes "hit any segment, damage flows toward the head" work, not a separate aggregation step. `attackEntitiesInList` (melee touch-damage, every 5 ticks, to any non-same-worm `EntityLivingBase` overlapping the segment's hitbox) is the worm's only contact damage source alongside the head's/body's ranged laser attacks. `addVelocity`/`faceEntity` are no-ops (the worm cannot be knocked back and does not turn to face like a normal mob — its facing is driven entirely by `WormMovement{Head,Body}NT`, see below). |
| `EntityBOTPrimeBase` (abstract, `extends EntityWormBaseNT`) | 95 | Sets `MAX_HEALTH = 15000`, `KNOCKBACK_RESISTANCE = 1.0`, fire-immune, `noClip = true`, `isAIDisabled() = false`, never despawns. `canEntityBeSeen` overrides vanilla with a raw block raytrace (ignores vanilla's normal LOS caching). `laserAttack(target, head)` is the **shared attack entry point both Head and Body call**: `head=true` fires 5 staggered `EntityBulletBase` shots (`BulletConfigSyncingUtil.WORM_LASER`, 35–60 damage per CE's `GunNPCFactory.getWormHeadBolt`, `maxAge=100`, no ricochet) at `i*0.05F` deviation increments; `head=false` fires a single `WORM_BOLT` shot (15–25 damage, `maxAge=60`). Both configs' `ammo` field is set to `ModItems.coin_worm` (a `RecipesCommon.ComparableStack` — cosmetic/config-consistency only, these bullets are never magazine-loaded, they're spawned directly). |
| `EntityBOTPrimeHead` | 189 | `@AutoRegister(name = "entity_balls_o_tron", trackingRange = 1000)`. The boss-bar owner (green). On `onInitialSpawn`: spawns **74** `EntityBOTPrimeBody` segments at its own position with sequential `partNumber` 0–73 and shares its own entity id as every segment's `headID` — this is the entire "worm assembly" step, no schematic/structure involved. Self-heals (+1 HP/6 ticks while it has a target, +4 HP/6 ticks while idle and not recently hit) — **a real, intentional regen-while-not-engaged mechanic**, worth preserving exactly (a player who disengages gives the boss time to heal). Fires the 5-shot head laser on a 30-tick cadence once a target is both within 150 blocks and line-of-sight. On death (`onDeathUpdate`, `deathTime==19`): grants `AdvancementManager.bossWorm` to every player within 200 blocks and gives each one `ModItems.coin_worm`. Movement is 100% delegated to `WormMovementHeadNT` (see below); note the file's own comment: `//TODO: clean-room implementation of the movement behavior classes (again)` — CE's own maintainers flag this as reimplemented-from-scratch code, not something to assume is bug-free. |
| `EntityBOTPrimeBody` | 115 | `@AutoRegister(name = "entity_balls_o_tron_seg", trackingRange = 1000)`. No boss bar (only the head has one). `getAttackStrength` = 75% of the *target's current health* (i.e. a body-segment touch always leaves the victim at 25% HP, not a fixed number — a real design choice, not a placeholder value). Fires the single-bolt laser attack on a 10-tick cadence when it can see its own `attackTarget` (inherited from `EntityWormBaseNT`, separate from `targetedEntity`, the chain-following reference — **these are two different fields with overlapping names, easy to conflate during the port**, see Open questions). The self-destruct-on-orphan logic described in Headline finding #2 lives entirely in this class's `updateAITasks`. Writes/reads `partID` to/from NBT (its own index in the chain) on top of the inherited `wormID`. |
| `WormMovementHeadNT` | 90 | Pure movement-composition helper (not an `Entity` itself — held by reference inside `EntityWormBaseNT`/called from `updateAITasks`). Two-mode waypoint AI: wanders within ±30/±10/±30 blocks of `spawnPoint` when idle, or homes toward its `getAttackTarget()` when one exists — with a genuinely interesting **burrow/surface state machine**: `wasNearGround` gates whether the head chases the target directly (when "near ground," it can freely path to the target's exact position) or is forced to approach at a fixed Y=10 "cruising altitude" first (when not near ground), flipping `wasNearGround=true` once it drops below Y=15, and back to `false` at a 1-in-80 roll per tick while above its `surfaceY` (60) and not already inside an opaque block — this is the mechanic that makes the worm alternate between "burrowing through terrain toward you" and "surfacing to strike," not a graphical effect. `courseChangeCooldown` (2–6 ticks between heading updates) and the `isCourseTraversable()`-gated 8× distance penalty for choosing a path through solid blocks are the rest of the steering logic. |
| `WormMovementBodyNT` | 70 | Simpler "follow the entity ahead of you in the chain" logic: every 60 ticks (or on tick 1), re-resolves `targetedEntity`/`followed` by scanning all `EntityWormBaseNT` within `rangeForParts` (70 blocks) for the segment whose `partNumber == this.partNumber - 1` (or the head, for segment 0) — this scan-and-relink, not a stored reference, is what lets the chain self-heal if a segment despawns/reloads out of order. Speed is clamped to `min(distanceToTarget - segmentDistance, maxBodySpeed)`, i.e. **the chain has slack**: a body segment does not move at all once it's within `segmentDistance × 0.895` (≈3.13 blocks) of its forward link, so the worm can compress/bunch up around a stationary head rather than always maintaining exact string-of-pearls spacing. |
| `EntityAINearestAttackableTargetNT` | 58 | Used by both Head (targets players, `range=128`) and Body (targets players via the shared `selector` Predicate that excludes same-worm segments, `range=128`). A thin variant of vanilla's `EntityAINearestAttackableTarget`: adds a configurable search range parameter (vanilla derives it from an attribute) and a custom Guava `Predicate<Entity>` selector; straightforward to reimplement on 1.21.1's `NearestAttackableTargetGoal`. |

**Spawn mechanism (worm)**: **not** a natural/biome spawn, **not** a spawn-egg path — confirmed by
grep (`addSpawn`/`EntityRegistry` return nothing for any of these 5 bosses). Two real routes exist,
both already partially present in this port:
1. **World-gen structure**: `com.hbm.world.generator.JungleDungeon` (`extends CellularDungeon`, a
   procedurally-generated multi-room cave dungeon reusing `brick_jungle`/`brick_jungle_cracked` for
   floor/wall/ceiling) places exactly one `ModBlocks.brick_jungle_circle` block (`BlockBallsSpawner`)
   on its bottom floor. `JungleDungeonStructure extends AbstractPhasedStructure` — **the same
   phased-structure dispatch framework `docs/phase4/worldgen_structures_bunkers_stations.md` already
   confirmed real** for the Radio tower structure — so this is a sibling structure under an
   already-scoped framework, not a new one. This report did not read `CellularDungeon` (the shared
   procedural-dungeon generator, likely used by non-boss dungeon types too) or `HbmWorldGen`'s
   dispatch entry for `JungleDungeonStructure` (biome gate / per-chunk frequency), so the exact spawn
   rarity is **not confirmed** here — flagged in Open questions, not guessed.
2. **Right-click summon**: `BlockBallsSpawner.onBlockActivated` — right-clicking the placed spawner
   block with `ModItems.mech_key` held (a normal crafted item, consumed on use) spawns
   `EntityBOTPrimeHead` at Y=300 above the block with `motionY = -1.0` (a controlled fall-in entrance)
   and turns the spawner block into `brick_jungle_cracked` (single-use, cannot be re-triggered).
   `mech_key` is a normal `AssemblyMachineRecipes`/`CraftingManager` recipe output — not itself a new
   dependency, just an item this report should confirm gets ported alongside the boss (not found
   registered in this port yet).
3. `ItemChopper`'s `spawn_worm` variant (already registered in this port, stubbed pending this
   report — see Headline finding #4) is a **third, unconditional/creative-style** summon path:
   right-click-placing it directly calls `new EntityBOTPrimeHead(world)` with no key/structure
   involved. CE ships all three paths simultaneously; there is no indication one supersedes another.

### MaskMan (purple bar)

| Aspect | Detail |
|---|---|
| Class | `EntityMaskMan extends EntityMob implements IRadiationImmune` (149 lines, `@AutoRegister(name = "entity_mask_man", trackingRange = 1000, eggColors = {0x818572, 0xC7C1B7})`), read in full. |
| Health/attributes | 1000 HP, 15 attack damage (melee — see below), 100-block follow range, full knockback resistance, 0.25 movement speed, fire-immune. |
| Phase mechanic | One real phase transition: `onUpdate` compares `prevHealth`/current health and, the instant health first drops below 50% max, triggers a one-time `world.createExplosion(this, x, y+4, z, 2.5F, true)` (a self-detonation/area-denial burst, not a self-damage attack) and records `prevHealth` so it never re-triggers. This is CE's entire "boss phase" for MaskMan — a single HP-threshold event, not a multi-stage fight. |
| Damage resistances | `attackEntityFrom` override: fire and magic damage are fully negated (`amount = 0`), projectile damage ×0.25, explosion damage ×0.5, and any single hit over 50 is compressed (`50 + (amount-50)*0.25`) — a real diminishing-returns damage cap, not a flat resistance percentage. A **1-in-10 instant-kill vulnerability to eggs** is also present: any `EntityEgg`-sourced indirect damage has a 10% chance to zero MaskMan's health and experience outright regardless of the egg's own (trivial) damage value — an intentional joke weakness, not a bug, worth preserving exactly. |
| Attack pattern | **Purely ranged; there is no active melee attack.** `EntityAIMaskmanCasualApproach` (164 lines, read in full) paths MaskMan to a standoff position 10 blocks from its target (`getApproachPos()`) rather than closing to melee distance — and its own `updateTask()` has the vanilla attack-on-arrival call **commented out** in CE's source (`/*if(d0 <= d1 ...) { ... this.attacker.attackEntityAsMob(...) } */`), confirmed by direct read: MaskMan's 15-damage `ATTACK_DAMAGE` attribute is set but never actually invoked by any AI task. Damage output is entirely `EntityAIMaskmanLasergun` (>10 blocks: a 3-way rotating attack — `ORB` a single lobbed `EntityBulletBase`/`MASKMAN_ORB` shot with upward arc, `MISSILE` a homing-flavored `MASKMAN_ROCKET` shot, `SPLASH` five simultaneous spread `MASKMAN_TRACER` shots — cycling after each attack's own `amount` count of repetitions) and `EntityAIMaskmanMinigun` (5–10 blocks: a steady `MASKMAN_BULLET` stream on a fixed delay, default every 3 ticks per `EntityMaskMan`'s constructor argument). All four `BulletConfigSyncingUtil.MASKMAN_*` ids route through `EntityBulletBase`, the same legacy "MK2" bullet class the worm boss uses. |
| Loot | On death: `gas_mask_m65` (with a filter pre-installed via `ArmorUtil.installGasMaskFilter` — **`gas_mask_m65` and its filter-install API are already ported** in this port's Phase 3 armor work, confirmed), `coin_maskman`, `v1` (`ItemModV1`, not yet ported — a small armor-mod-adjacent trophy item, `com.hbm.items.armor.ItemModV1` in CE), and a vanilla `Items.SKULL`. Grants `AdvancementManager.bossMaskman` to every player within 50 blocks on death. |
| Spawn mechanism | `BossSpawnHandler.rollTheDice` (called every world tick from `ModEventHandler`, confirmed): gated by `MobConfig.enableMaskman` (already-ported config field, confirmed) — every `maskmanDelay` ticks (default 60×60×60 = 216,000, i.e. 3 hours), a `1-in-maskmanChance` (default 3) roll picks a random online player; if that player has ≥`maskmanMinRad` (default 50) rads **and** (per `maskmanUnderground`, default true) is at least 3 blocks below the surface, MaskMan spawns near them (`posX/Z + gaussian*20`, `y = world.getHeight` at that column) and a chat warning fires. This is a real, fully-portable random-encounter system, not a structure or item spawn — and every config field it reads is already ported in `MobConfig` (`ENABLE_MASKMAN`/`MASKMAN_DELAY`/`MASKMAN_CHANCE`/`MASKMAN_MIN_RAD`/`MASKMAN_UNDERGROUND`, confirmed by direct read of this port's own `config/MobConfig.java`). |

### UFO (red bar)

| Aspect | Detail |
|---|---|
| Class | `EntityUFO extends EntityFlying implements IMob, IRadiationImmune` (467 lines, `@AutoRegister(name = "entity_ntm_ufo", trackingRange = 1000)`), read in full. **Not** built on `EntityUFOBase` (Headline finding #3). |
| Health/attributes | 20,000 HP (the single highest boss health value in this survey — 33% above the worm's per-segment 15,000, though the worm's *effective* fight-ending pool is just the head's 15,000). 15×4-block hitbox. Never despawns. A `hurtCooldown` (5 ticks) makes it briefly damage-immune after every hit landed — a real i-frame mechanic, not a resistance multiplier. |
| Phase mechanic | No explicit HP-threshold phase transition (unlike MaskMan). The fight *does* escalate on a timer: `ticksExisted % 300 < 200` → fast dual-laser volleys (every 2 ticks); the remaining 100-tick window per 300-tick cycle → slower rocket volleys (every 10 ticks) — an alternating laser/rocket cadence rather than an HP-gated phase, cycling continuously for the whole fight. |
| Attack pattern | Three distinct attack modes, all via legacy bullets/direct damage, none via the Sedna framework: (1) **laser** — `EntityBulletBase`/`BulletConfigSyncingUtil.WORM_LASER` (the *same* config id the worm boss's head uses — CE reuses one ammo definition across two unrelated bosses), fired from a pivot point 10 blocks off-target with a randomized ±80° yaw offset, at any of up to one "primary" target plus a rotating pool of up to several "secondary" targets (any `EntityLivingBase` within 100 blocks with line-of-sight, re-scanned every 50 ticks); (2) **rocket** — `EntityBulletBase`/`UFO_ROCKET` (built from `GunRocketFactory.getRocketConfig()` with a custom `IBulletUpdateBehavior` giving it mid-flight homing toward whichever entity id is stashed in `homingTarget` NBT); (3) **abduction beam** — not a projectile at all: when within 25 blocks (X+Z) of its target, UFO raycasts straight down to the first non-air block, then deals a flat **1000 damage** plus 5 seconds of fire plus a 5-point `ContaminationUtil.contaminate(..., HazardType.RADIATION, ContaminationType.CREATIVE, 5F)` radiation dose to every entity in that vertical column — **`ContaminationUtil.contaminate` is already ported** (confirmed, Phase 3 foundation), so this specific attack has zero missing-API blockers beyond the entity itself existing. |
| Death sequence | On death (`deathTime == 19`): spawns `EntityNukeTorex.statFac` (mushroom-cloud VFX) and `EntityNukeExplosionMK5.statFacNoRad(..., 25)` — **both already-ported Phase 3 explosion-engine classes**, confirmed real, zero new dependency — then grants `AdvancementManager.bossUFO` and `coin_ufo` to every player within 200 blocks. UFO explodes as a real nuke-tier detonation on death, not a cosmetic effect. |
| Spawn mechanism | No periodic/natural roll (unlike MaskMan) — confirmed by grep, `EntityUFO` is referenced nowhere in `BossSpawnHandler` or any biome/structure spawn list. The **only** spawn path is `ItemChopper`'s `spawn_ufo` variant (already registered in this port, stubbed — Headline finding #4), which additionally pre-sets `scanCooldown = 100` and spawns it 35 blocks above the target position. |

### Hunter Chopper (purple bar, boss-tier hostile — not a discrete "kill for loot" fight)

| Aspect | Detail |
|---|---|
| Class | `EntityHunterChopper extends EntityFlying implements IMob, IRadiationImmune` (442 lines,
  `@AutoRegister(name = "entity_hunter_chopper", trackingRange = 1000, eggColors = {0x000020,
  0x2D2D72})`), read in full. The file's own top-of-class comment, preserved verbatim: *"Drillgon200:
  This whole thing is messed up and janky and I don't know what to about it."* — CE's own maintainer
  flags this class as unusually fragile; treat every behavior below as confirmed-by-reading but not
  necessarily confirmed-sane, and budget extra test time. |
| Health | 750 HP — an order of magnitude below MaskMan/UFO/the worm; this is armed-vehicle-tier, not raid-boss-tier, health. |
| Attack pattern | Fires the **oldest** of the three legacy bullet generations: `new EntityBullet(this.world, this, 3.0F, 35, 45, false, "chopper", EnumHand.MAIN_HAND)` — the `EntityBullet(World, EntityLivingBase, float, int, int, boolean, String, EnumHand)` constructor (CE's own in-file comment on this exact overload, preserved verbatim: *"why the living shit did i make isTau a string? who knows, who cares."*), passing the literal string `"chopper"` rather than a boolean/enum. That string sets **three** boolean data-parameters by string-equality inside the constructor: `setTau(isTau.equals("tauDay"))` (false here), `setChopper(isTau.equals("chopper"))` (true — this is what makes chopper bullets never self-collide with their own `EntityHunterChopper` shooter, checked elsewhere via `!(entityHit instanceof EntityHunterChopper)` at two call sites: `Library.java:1055` and `EntityBullet.java:508`), and `setIsCritical(!isTau.equals("chopper"))` (false here — chopper bullets are explicitly excluded from the "critical hit" visual/behavior flag every other `EntityBullet` shot gets by default). Fired every other tick once `attackCounter` (a 0–200 rolling counter) crosses 120, at 35–45 base damage bumped by 0–4 random. Also drops up to 5 `EntityChopperMine` proximity mines beneath itself once airborne combat has continued past 100 ticks (1-in-15 roll, occasionally dropping a cross-pattern of 4 extra mines) — `EntityChopperMine` is a separate projectile/hazard entity, not read in this survey (see Deferred scope). |
| "Death" sequence | Unlike the other three bosses, HunterChopper does not simply reach 0 HP and despawn: `attackEntityFrom` intercepts any hit that would exceed current health and instead calls `initDeath()` (a 10-block explosion + damage sound) and `setIsDying(true)`, clamping health to 0.1 rather than 0 — this flips `onUpdate` into a **separate falling/crashing state machine** (motion decays, it spins, it periodically re-explodes at 5-block radius, spawns exhaust-trail particles) until it hits the ground, at which point it does a final 15-block explosion, drops its loot table (chopper body-part items, `combine_scrap`, `plate_combine_steel`, `wire_fine` magtung), and only then calls `setDead()`. **No achievement is granted on death** (confirmed — no `AdvancementManager` reference anywhere in this file, unlike the other three). Porting this needs the crash state machine ported as a distinct phase, not folded into ordinary death handling. |
| Spawn mechanism | `getCanSpawnHere()`/`getMaxSpawnedInChunk()` are overridden (1-in-20 roll, cap 1 per chunk) but **no `addSpawn`/biome spawn-list registration exists anywhere in CE** for this class (confirmed by grep) — this natural-spawn path is dead code in CE itself, not something the port needs to wire up. The only live spawn path is `ItemChopper`'s `spawn_chopper` variant (already registered in this port, stubbed). |

### Quackos (purple bar, joke pseudo-boss)

| Aspect | Detail |
|---|---|
| Class | `EntityQuackos extends EntityDuck` (186 lines, `@AutoRegister(name = "entity_elder_one", trackingRange = 1000, eggColors = {0xd0d0d0, 0xFFBF00})`), read in full — `EntityDuck extends EntityChicken` (42 lines, trivial, already read). |
| Behavior | Scaled 25× (0.3×0.7 → 7.5×17.5 hitbox), fully **invulnerable** (`getIsInvulnerable()` hardcoded true; `setHealth` silently refuses any value lower than current, so even direct NBT/command health-setting can't reduce it in normal play), never despawns naturally, and rideable (`processInteract` lets a player mount it if no passenger is present, with a custom `updatePassenger` seat-position offset). It has **no attack code of any kind** — it is a decorative curiosity/mount, not a fight. |
| "Kill"/removal | Cannot be killed. Removed only via `ItemPeas.onItemRightClick` (44 lines, read in full — flavor tooltip: *"He accepts your offering."*), which scans a 50-block radius for `EntityQuackos` instances and calls a custom `despawn()` on each — a 150-particle `HbmEffectNT.BF` burst around the duck followed by `this.isDead = true`, bypassing the normal death event/loot path entirely (no `onDeath`, no drops). |
| Spawn mechanism | Not summonable directly by any item (unlike the other four) — the *only* creation path is CE's radiation-mutation cascade: `com.hbm.handler.EntityEffectHandler` (not ported in this port at all, confirmed absent), on any tick where an `EntityDuck`'s accumulated radiation reaches ≥200 rad, replaces it in-place with a freshly-spawned `EntityQuackos` at the same position/rotation and kills the original duck. Since `ItemChopper`'s `spawn_duck` variant already places a plain `EntityDuck` (once ported, per Headline finding #4), the practical spawn chain is: place a duck → irradiate the area enough → `EntityEffectHandler` promotes it to Quackos. This report's scope is the `EntityQuackos` class itself and the specific mutation branch; the rest of `EntityEffectHandler`'s cascade (nuclear creeper/zombie-villager/zombie-horse/`EntityRADBeast`-from-blaze mutations) is general hazard-effect content out of this report's "bosses" scope — see Deferred scope. |

### RAD Beast — boss-adjacent, explicitly excluded from the "boss" list

`EntityRADBeast extends EntityMob implements IRadiationImmune` (239 lines, read in full,
`@AutoRegister(name = "entity_ntm_radiation_blaze", ...)`) is spawned by the same
`BossSpawnHandler.rollTheDice` as MaskMan (as a themed "radiation elemental" swarm, `MobConfig.
enableMeltdownElementals`/`elementalChance`/`elementalDelay`/`elementalAmount`, all already-ported
config fields) and via `EntityEffectHandler`'s blaze-mutation branch, and it does have a "pack
leader" variant (`makeLeader()`: 360 HP instead of 120, holds+drops `coin_radiation`, and its death
grants `AdvancementManager.bossMeltdown` — the only one of its two HP tiers that does) — but it has
**no `BossInfo`/boss-bar field anywhere in the class**, confirmed by direct read. This report treats
it as boss-*adjacent* elite content (a themed elite mob with an achievement-granting "leader"
variant), not one of the 5 boss-bar bosses in scope here — flagged for whichever Phase 4 area
researches general hostile-mob content, not re-scoped in full here. Its passive area radiation pulse
(`ContaminationUtil.radiate(world, x, y, z, 32, 500)`, called every tick) and its `attackEntityAsMob`
override (a `ChunkRadiationManager.proxy.incrementRad` call at melee range, see Deferred scope) are
its only genuinely new mechanics worth flagging to that future report.

### Legacy bullet system these bosses depend on (shared, multi-consumer — see Deferred scope for the other consumers)

| Class | Lines | Consumed by (this report's bosses) | Also consumed by (outside this report) |
|---|---|---|---|
| `EntityBulletBase` ("MK2", `@AutoRegister(name = "entity_bullet_mk2")`) | 684, read in full | Worm (head+body laser attacks), MaskMan (all 3 AI attack modes), UFO (laser+rocket) | `EntityDeathBlast` (SatelliteLaser's payload — already named as an unowned Phase 3 follow-up dependency), `ItemGunBase` (the legacy 2-gun system `docs/phase3/gun_framework.md` scoped), `TileEntityTrappedBrick` (a decoration/trap block, not otherwise researched) |
| `EntityBullet` ("MK1"/oldest, `@AutoRegister(name = "entity_bullet")`) | 809, ~150 read | Hunter Chopper only | `EntityCyberCrab` (a non-boss legacy mob named in `docs/phase3/gun_framework.md`'s deferred list), `ExplosionChaos` (the antimatter/xen explosion engine — confirmed unowned by any Phase 1-3 package per this task's own framing) |
| `BulletConfiguration` (the legacy per-shot "stat card," structurally the predecessor to Sedna's `BulletConfig`) | 239, signature-read | All of the above | — |
| `com.hbm.handler.guncfg.GunNPCFactory` | 344, ~50 read (3 boss-relevant methods only) | Worm (`getWormBolt`/`getWormHeadBolt`), UFO (`getRocketUFOConfig`, itself built on `GunRocketFactory.getRocketConfig()`) | MaskMan's 4 configs (`getMaskman*`) were not individually read — signature-confirmed present in the same file, not detailed here |
| `BulletConfigSyncingUtil` | 285, ~90 read | The static append-only `int` id registry every `EntityBulletBase(World, int, ...)` constructor keys off — **the exact same "construction-order-dependent synced id" pattern `docs/phase3/gun_framework.md` already flagged for Sedna's `BulletConfig.configs`**, and the same recommendation applies here: prefer a `ResourceLocation`-keyed registry over preserving CE's brittle static-`int`-increment order (see Key design decisions) | — |

This report recommends the team implementing these bosses **also** implement `EntityBulletBase` in
full (684 lines, already read here) and the `EntityBullet` "chopper" constructor path specifically,
since no other Phase 1-3 package has claimed either class and three of this report's four real
fights are unplayable without the first one. Treat both as a small, shared "legacy ballistics core"
package analogous to `docs/phase3/gun_framework.md`'s "Package A," reusable by `EntityDeathBlast`/
`ExplosionChaos`/`EntityCyberCrab`/`ItemGunBase` once those areas are implemented — do not gate this
report's bosses on those other consumers being ready first, and do not duplicate the ballistics core
per-consumer.

### Loot/trophy items to register alongside these bosses

None of the following exist yet in this port (confirmed by grep across `src/main/java/com/hbm/`);
all are simple flavor/decorative items with a directly-reusable pattern already established by this
port's own `ItemSiegeCoin`/`ItemCustomLore` registrations in `SpecialItems.java`:

`coin_worm`, `coin_maskman`, `coin_ufo`, `coin_radiation` (an `ItemCustomLore`-style rarity-flagged
trophy each, exactly like the already-ported siege coins), `mech_key` (a normal crafted item — the
worm-spawner key), `chopper_head`/`chopper_torso`/`chopper_wing`/`chopper_tail`/`chopper_gun`/
`chopper_blades` (6 Hunter Chopper wreckage drops), and `v1` (`com.hbm.items.armor.ItemModV1` in CE
— not read in detail in this survey, a small trophy/mod-adjacent item, flagged for a follow-up read
rather than assumed trivial).

## Deferred scope

Real dependencies of *this specific* subsystem that belong to other packages/phases or other
research passes, matching the "which package, which phase" format the ground rules ask for:

- **`com.hbm.main.AdvancementManager`** — confirmed entirely absent from this port. All 4 real
  bosses (worm, MaskMan, UFO, RAD-Beast-leader) grant a named `Advancement` on death
  (`bossWorm`/`bossMaskman`/`bossUFO`/`bossMeltdown`); Hunter Chopper and Quackos do not. This needs
  its own research/port pass (already named as a blocking dependency by `docs/phase3/
  satellites_followup_and_loot_pools.md`'s Satellite work per this task's own framing) — this report
  only names the exact 4 call sites bosses need from it. Until it exists, the boss entities can be
  fully implemented and fought with the achievement-grant call left as a documented forward
  reference, exactly like every other cross-phase gap in this port.
- **`com.hbm.handler.radiation.ChunkRadiationManager`** — confirmed not yet ported (Phase 4 per
  `docs/phase0/STATUS.md`), real API shape confirmed from CE (`ChunkRadiationManager.proxy.
  incrementRad(World/WorldServer, BlockPos, rad, [max])`). Only `EntityRADBeast` (boss-adjacent, not
  one of the 5 core bosses) calls this directly, at melee-attack time. None of the 5 core bosses in
  this report need it.
- **`com.hbm.handler.EntityEffectHandler`** — confirmed not yet ported. This report's scope is
  narrowly the `EntityDuck → EntityQuackos` mutation branch (≥200 accumulated rad); the rest of this
  class's dispatch table (zombie/zombie-villager/zombie-horse/`EntityRADBeast`-from-blaze mutations)
  is general radiation-hazard mob content, not boss content — recommend whichever Phase 4 area
  covers general hostile-mob/hazard interactions own the full port of this class, with this report's
  Quackos branch as one line item inside it rather than a reason to duplicate the file.
- **`com.hbm.entity.mob.ai.{EntityAIBreaking,EntityAIConditionalWander,EntityAIEatBread,
  EntityAIFireGun,EntityAIStartFlying,EntityAIStopFlying,EntityAISwimmingConditional,
  EntityAI_MLPF}`** — the other 8 files in the same package as this report's 4 boss-specific AI
  classes. `EntityAIFireGun` is already scoped by `docs/phase3/gun_framework.md` (confirmed generic
  over `EntityLivingBase`, not player-only); the remaining 7 are general-purpose mob AI (used by
  `EntityFBI`/`EntityCyberCrab`/other non-boss mobs per this survey's greps) — out of this report's
  boss-specific scope, belongs with whichever Phase 4 area researches "regular" hostile mobs.
- **`EntityUFOBase`, `EntityFBIDrone`, `EntityFBI`, `EntityChopperMine`** — `EntityUFOBase` backs
  the FBI raid drone, not the UFO boss (Headline finding #3); `EntityFBI`/`EntityFBIDrone` are the
  raid-swarm mobs `BossSpawnHandler` also dispatches (config-gated by `MobConfig.enableFBIRaids`/
  `raidDelay`/`raidChance`/`raidAmount`/`raidDrones`/`raidAttackDistance`, all already-ported config
  fields, confirmed) — not boss-tier (no boss bar, ordinary `EntityMob` health), out of this report's
  scope; `BossSpawnHandler.markFBI(player)` is also called from several already-ported-elsewhere
  machine right-click handlers (`ReactorResearch`/`ReactorZirnox`/`RBMKConsole`/`RBMKRod`/
  `MachineRadiolysis`/`SoyuzLauncher` in CE) as an escalation trigger for the raid variant gated by a
  20-minute NBT-timestamp mark — worth the implementing team's awareness that `BossSpawnHandler` is
  not purely a periodic-roll dispatcher, it is also reactively triggered by nuclear-machine use, but
  this is FBI-raid content, not boss content, and is deferred here in full. `EntityChopperMine`
  (Hunter Chopper's dropped proximity mine) was not read in this survey — flagged for whoever
  implements Hunter Chopper to read in full rather than assume it's a trivial TNT-primed clone.
- **`com.hbm.world.generator.CellularDungeon`** and **`HbmWorldGen`'s dispatch entry for
  `JungleDungeonStructure`** — not read in this survey. This report confirms `JungleDungeon extends
  CellularDungeon` and that `JungleDungeonStructure extends AbstractPhasedStructure` (the same
  framework `docs/phase4/worldgen_structures_bunkers_stations.md` already confirmed real for the
  Radio-tower structure), but does **not** confirm the exact biome gate or per-chunk spawn frequency
  for the worm's dungeon — recommend whoever owns world-gen-structure implementation read
  `CellularDungeon` once, since it is very likely shared by other (non-boss) dungeon types this
  report did not enumerate.
- **`com.hbm.potion.HbmPotion.mutation`/`.stability`** — `ContaminationUtil`'s existing TODOs (both
  `isRadImmune` and `applyDigammaData`) also name `HbmPotion` as a separate, not-yet-ported blocking
  dependency unrelated to this report's bosses specifically (it affects *player* immunity, not mob
  immunity) — already flagged by that file's own comments, not re-derived here, just noted so it
  isn't confused with the `EntityQuackos` TODO immediately next to it in the same methods.
- **`EntityDeathBlast`, `ExplosionChaos`, `ItemGunBase`, `EntityCyberCrab`, `TileEntityTrappedBrick`**
  — the other real consumers of `EntityBulletBase`/`EntityBullet` (see the legacy-bullet-system table
  above). Not researched in this report beyond confirming they exist and share the dependency;
  whichever team implements the shared legacy-bullet package should coordinate with (or simply
  precede) each of these.
- **`docs/phase3/gun_framework.md`'s own unresolved items** (the `DamageResistanceHandler` pierce
  parameters, the `SEDNA_PLASMA` `ModDamageTypes` gap, etc.) do not apply to this report's bosses —
  none of the legacy `BulletConfiguration`/`EntityBulletBase` damage path routes through
  `DamageResistanceHandler` or `ModDamageTypes` at all (confirmed by reading `EntityBulletBase`'s
  `onUpdate`/`onEntityHurt` in full: damage is a plain `victim.attackEntityFrom(ModDamageSource.
  causeBulletDamage(...), damage)` call with no armor-piercing math) — flagged only to head off an
  incorrect assumption that this report inherits that report's open items.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and
Neo Edition's parallel entity registration for NeoForge API shape; the boss-bar API specifically is
**not** Neo-Edition-confirmed, see the explicit flag below):

- **Mob `EntityType`/attribute registration is confirmed real and already-used in Neo Edition** —
  `NtmEntityTypes.DUCK`/`CREEPER_NUCLEAR` follow `DeferredRegister<EntityType<?>>.register(name, ()
  -> EntityType.Builder.of(Ctor::new, MobCategory.X).sized(w, h)[.eyeHeight(e)][.clientTrackingRange(n)]
  .build(name))`, and each mob's `SharedMonsterAttributes`-equivalent (`MAX_HEALTH` etc.) is supplied
  via a static `createAttributes()` returning `Monster.createMonsterAttributes().add(Attributes.X,
  v)...` **plus** a live `@SubscribeEvent`-annotated handler on NeoForge's
  `EntityAttributeCreationEvent` (confirmed real, `CommonEvents.onEntityAttributeCreation`, calling
  `event.put(NtmEntityTypes.X.get(), X.createAttributes().build())`) — this is the exact 1.21.1
  replacement for CE's per-entity `applyEntityAttributes()` override pattern every boss in this
  survey uses. Recommend one `BossEntityTypes`-or-similarly-scoped `DeferredRegister<EntityType<?>>`
  class (mirroring `ConveyorEntityTypes`'s already-confirmed shape from Phase 3, per `docs/phase3/
  gun_framework.md`) for `BOTPRIME_HEAD`/`BOTPRIME_BODY`/`MASK_MAN`/`UFO`/`HUNTER_CHOPPER`/`QUACKOS`
  (all 6 are `LivingEntity` subclasses and need the attribute-event registration), plus the same
  `DeferredRegister` (or the shared one `docs/phase3/gun_framework.md` already recommended) for the
  legacy `EntityBulletBase`/`EntityBullet` projectile types (non-living, no attribute event needed).
- **`ServerBossEvent`/`BossEvent.BossBarColor`/`BossEvent.BossBarOverlay` is real 1.21.1 vanilla API
  — but this specific claim is well-established Mojang-mapping knowledge from vanilla's own
  `EnderDragon`/`WitherBoss` boss-bar wiring, not verified against a compiled jar in this sandbox and
  not cross-checked against Neo Edition (which has ported zero boss content, confirmed above).**
  Flagging that distinction explicitly per this task's own ground rules. With that caveat, the shape
  every boss in this report needs is: a `private final ServerBossEvent bossEvent = new
  ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.X, BossEvent.BossBarOverlay.PROGRESS)`
  field (CE's own 5 instances use `PURPLE`×3, `RED`×1, `GREEN`×1 — direct 1:1 color mapping, no enum
  values are missing on either side); a per-tick `bossEvent.setProgress(getHealth() / getMaxHealth())`
  call (CE's `onLivingUpdate` hook maps onto 1.21.1's `Mob#customServerAiStep`/`LivingEntity#tick`);
  and the modern replacement for CE's `addTrackingPlayer(EntityPlayerMP)`/
  `removeTrackingPlayer(EntityPlayerMP)` overrides is overriding `Entity#startSeenByPlayer
  (ServerPlayer)`/`Entity#stopSeenByPlayer(ServerPlayer)` (the method vanilla's own `EnderDragon`/
  `WitherBoss` use for the identical purpose) to call `bossEvent.addPlayer(serverPlayer)`/
  `bossEvent.removePlayer(serverPlayer)`. `setDarkenSky(true)` (Hunter Chopper) is `setDarkenScreen
  (true)` in 1.21.1's renamed `BossEvent` setter. Recommend whoever implements this package's first
  boss (the worm, since it's this report's most-detailed entity) do a throwaway compile-check of this
  exact shape against a real dependency jar before propagating it to all 5 bosses, since — per the
  flag above — none of it is confirmed against an actual compiled artifact in this repo.
- **`@AutoRegister` on all 9 boss/related classes in this survey is CE-only build tooling, not
  something to port** — already confirmed generally by `docs/phase2/rbmk_reactor.md`'s Key design
  decisions (a separate-Gradle-module compile-time annotation processor with no NeoForge equivalent
  need); this report's own `@AutoRegister(name=..., trackingRange=..., eggColors=...)` usages are the
  same pattern applied to mobs instead of tile entities — the `name`/`trackingRange` map onto the
  `EntityType.Builder`/`.setTrackingRange()`(or `.clientTrackingRange()`) calls above, and
  `eggColors` maps onto a `SpawnEggItem` registration (a Phase 1/5 items concern, not read further
  here) — not re-derived, just confirmed to transfer directly.
- **`ContaminationUtil.contaminate`/`.radiate`/`isRadImmune`, `IRadiationImmune`, and
  `HBMSoundHandler`'s boss-relevant sound events are all already real, already-committed, and
  already sufficient for this report's needs with zero further changes required beyond the two
  `EntityQuackos`-specific TODO lines** (Headline finding #4) — this is the strongest "green field"
  signal in this survey; the boss entities themselves are the only missing piece, not their
  supporting infrastructure.
- **The worm's `world.getEntityByID(headID)`-style live entity-id lookup (`EntityWormBaseNT.
  getHead()`) is a real 1.12-era pattern this port should re-examine, not blindly port.** 1.21.1's
  `Level` still exposes `getEntity(int)` (confirmed stable vanilla API across versions, unchanged in
  shape), so this pattern is not actually blocked — but `docs/phase3/gun_framework.md`'s own finding
  about CE's 1.12-era "store a name/id and re-resolve" workarounds being unnecessary in 1.21.1
  (vanilla `Projectile#setOwner`/`getOwner` replacing `EntityThrowableNT`'s thrower-name workaround)
  is a useful parallel worth checking here too: confirm at implementation time whether `headID`
  (a raw `int`, not synced/NBT-persisted across a chunk-unload/reload in a way this survey verified)
  survives a save/reload correctly, or whether a synced `Optional<UUID>`/direct entity reference
  would be more robust for 1.21.1 — flagged as a design question, not resolved here.

## Open questions / risks

- **Exact `JungleDungeonStructure` spawn frequency/biome gate is not confirmed** (see Deferred
  scope) — `CellularDungeon` and `HbmWorldGen`'s dispatch table were not read in this survey. Do not
  guess a rarity value; read those two pieces before finalizing the worm's natural-discovery rate.
- **The worm's 75-simultaneous-entity design is a real performance/tracking-load question for
  1.21.1** that CE's 1.12.2 tick model may have tolerated differently — 75 independently-ticking,
  independently-network-synced `LivingEntity` instances (74 of which have no boss bar and mostly
  idle-follow logic) all alive at once, each with its own attribute map, is worth a load-test once
  ported rather than assumed free. Not confirmed as a problem in this survey — flagged as a risk, not
  a finding.
- **`attackTarget` (vanilla `Mob` field, inherited) vs. `targetedEntity` (CE's own field, the
  chain-follow reference) are two different fields with easily-confused names on the same worm
  classes** (Headline finding, `EntityBOTPrimeBody` row) — recommend an explicit rename during the
  port (e.g. `chainTarget`) rather than preserving both similarly-named fields verbatim, since CE's
  own naming here is a real readability trap, not a case where preserving CE's exact identifier
  matters for behavior.
- **`EntityWormBaseNT.headID`/`partNum` are plain `int` fields, not `DataParameter`-synced** — this
  report did not verify whether client-side rendering/AI code anywhere in CE's `render.entity.
  RenderWormBody`/`RenderWormHead` (out of this report's scope, Phase 5) needs these values on the
  client, which would require adding synchronization the original class doesn't have. Flagged for
  whoever picks up worm rendering to check, not resolved here.
- **The boss-bar API shape above is unverified against any compiled jar or Neo Edition file** (see
  Key design decisions) — the single highest-priority item to confirm before implementation starts,
  since it's the one piece of this report's API surface with zero corroborating source in either
  reference repo.
- **CE's own `EntityBOTPrimeHead` file comment (`//TODO: clean-room implementation of the movement
  behavior classes (again)`) is CE's own maintainers flagging `WormMovement{Head,Body}NT` as
  reimplemented/uncertain code**, not this report's own uncertainty — read and documented faithfully
  above, but worth extra test coverage (waypoint steering, burrow/surface state transitions) rather
  than treating it as a settled, battle-tested reference implementation the way e.g. RBMK's neutron
  engine was.
- **`EntityHunterChopper`'s crash/dying state machine and `EntityChopperMine` were only partially
  characterized** (the mine entity itself was not read at all) — flagged explicitly so the
  implementing team reads `EntityChopperMine` in full rather than assuming it behaves like a generic
  primed-TNT entity.
- **Whether `mech_key`, the 4 boss coins, `v1`, and the 6 chopper wreckage items should be flattened
  onto one shared item base or kept as fully independent registrations** was not decided here (this
  report only confirms none exist yet and that `SpecialItems.java`'s existing `ItemSiegeCoin`
  pattern is a directly reusable template) — a small, low-risk implementation-time choice, not a
  blocking one.
