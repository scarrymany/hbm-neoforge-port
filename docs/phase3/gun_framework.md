# CE gun framework (`ItemGunBaseNT` family) — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/{ItemGunBaseNT,GunConfig,Receiver,
  BulletConfig,DamageSourceSednaNoAttacker,DamageSourceSednaWithAttacker}.java` (6 files, ~1,367
  lines — the live gun/ammo "DNA" data model)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/factory/{Lego,GunStateDecider}.java`
  (2 files, 543 lines — the behavior-lambda "verbs" and the state-machine engine that drives them)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/mods/{XWeaponModManager,IWeaponMod,
  WeaponModBase}.java` (3 files, 462 lines — the weapon-attachment eval chain; the ~23 concrete
  `WeaponMod*` effect classes were signature-surveyed, not read in full, see below)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/mags/{IMagazine,MagazineSingleTypeBase,
  MagazineFullReload,MagazineSingleReload,MagazineInfinite,MagazineBelt}.java` (6 of 8 files, 566 of
  693 lines — `MagazineFluid`/`MagazineEnergy` not read in full, signature-checked only, see below)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/{EntityThrowableNT,EntityThrowableInterp,
  EntityBulletBaseMK4,EntityBulletBeamBase,IBulletBase}.java` (5 of 6 files, ~1,127 of 1,234 lines —
  `EntityBulletBaseMK4CL` sized/diffed against `EntityBulletBaseMK4` but not read line-by-line, see
  below) — this is the actual ballistics/hit-detection simulation
- `upstream/hbm-ce/src/main/java/com/hbm/util/EntityDamageUtil.java` (partial, ~155 of 421 lines:
  `attackEntityFromNT`, `getMouseOver`) and `.../util/DamageResistanceHandler.java` (partial: the
  `DamageClass` enum and the `setup`/`reset`/`calculateDamage`/`getDTDR` signatures, not the full
  608-line resistance-table implementation, which is a general combat system out of this package's
  own scope — see Deferred scope)
- `upstream/hbm-ce/src/main/java/com/hbm/items/{IKeybindReceiver,IEquipReceiver}.java` and
  `.../interfaces/{IItemHUD,IHoldableWeapon}.java` (4 trivial marker/callback interfaces, full)
- Repo-wide greps to map the framework's edges: `extends ItemGunBase(NT|Sedna)?\b`,
  `new (ItemGunBaseNT|EntityBulletBase(MK4)?|EntityBullet)\(`, `setupRecoil(`, `GunCannonFactory\.|
  GunDGKFactory\.|GunNPCFactory\.|GunRocketFactory\.`, and directory listings/`wc -l` over
  `items/weapon/**`, `entity/projectile/*Bullet*`, `handler/guncfg/*` to size every bucket below
  exactly (114 files / 18,432 lines under `items/weapon/**` alone)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/turret/TileEntityTurretBaseNT.java` (grep only,
  confirming its direct `BulletConfig`/`EntityBulletBaseMK4` reuse — full survey already owned by
  `docs/phase3/turret_system.md`, not re-derived here) and `.../entity/mob/ai/EntityAIFireGun.java`
  (grep only, confirming the state machine is `EntityLivingBase`-generic, not `EntityPlayer`-only)
- This port's own `src/main/java/com/hbm/{packet/HbmNetwork.java, damage/ModDamageTypes.java,
  inventory/container/MenuBase.java, entity/ConveyorEntityTypes.java}` (read in full) and
  `docs/phase1/{items_tool.md, items_special.md, items_food_gear.md}`, `docs/phase3/{turret_system.md
  (read in full — the sibling report whose own "Deferred scope" item #1 names this exact package),
  missile_framework.md, bomb_blocks_and_detonators.md, explosion_engine.md}` (headlines/scope
  sections skimmed for cross-references, not re-derived)
- `upstream/neo-edition/src/main/java/com/hbm/{items/weapon/sedna/GunBaseNTItem.java (496 lines,
  read in full), entity/projectile/BulletBaseMK4.java (header read), entity/NtmEntityTypes.java
  (registration lines grepped)}` — **cross-referenced for confirmed NeoForge 1.21.1 API shape only**,
  per this task's ground rules; every behavioral claim below is sourced from CE, never from Neo
  Edition

## Headline finding

PORT_SPEC is right that this is one of the deepest logic packages, but three premises need
correcting before the split makes sense, in the same way RBMK's own research had to correct the
"big multiblock" framing:

1. **There is exactly one live, actively-content-populated gun base class, and it is not
   `ItemGunBase`.** A grep for `abstract class.*Gun`/`ItemGunBase` turns up *three* candidate base
   classes, but only one is real:
   - **`com.hbm.items.weapon.sedna.ItemGunBaseNT extends Item`** (496 lines) — instantiated directly
     (never subclassed, except by 5 special-case guns + 1 factory class, see below) **63 times**
     across `GunFactory.java` + 23 `XFactory*.java` files, covering essentially every gun in the mod
     (revolvers, rifles, shotguns, SMGs, miniguns, rocket/missile launchers, laser weapons, the drill
     "gun", flamethrowers, chemthrowers, tesla cannons — the full CE arsenal). This is the class
     PORT_SPEC means by "the CE gun framework" and the sole subject of this report's Phase-3-safe
     scope.
   - **`com.hbm.items.weapon.sedna.ItemGunBaseSedna extends ItemBakedBase`** (859 lines) is **dead
     code** — grep finds zero `new ItemGunBaseSedna(` and zero `extends ItemGunBaseSedna` anywhere in
     CE. It is referenced only twice, both as `instanceof` checks on a held stack
     (`ModEventHandlerClient.java:891`, `EntityBulletBaseNT.java:135`) that can never fire. Read in
     full to confirm this rather than assumed from the name; **do not port it** — it is CE's own
     abandoned first draft of the system `ItemGunBaseNT` superseded, kept in the tree only for those
     two now-dead `instanceof` branches. Flagging so a future agent doesn't burn a work package
     porting 859 lines of unreachable code because the class name looks authoritative.
   - **`com.hbm.items.weapon.ItemGunBase extends Item`** (763 lines, in the *non*-`sedna` package) is
     a **legacy, parallel system** still alive for exactly 2 guns (`gun_supershotgun` via
     `ItemGunShotty`, `gun_vortex` via `ItemGunVortex`), backed by its own `com.hbm.handler.guncfg.*`
     config factories (7 files, 998 lines: `Gun12GaugeFactory`, `GunEnergyFactory`,
     `GunCannonFactory`, `GunDGKFactory`, `GunNPCFactory`, `GunRocketFactory`,
     `BulletConfigFactory`) and its own older projectile entities (`EntityBulletBase`, 684 lines, and
     `EntityBullet`, 809 lines — pre-MK4 ballistics, not the same classes `ItemGunBaseNT` fires).
     **This is not dead code** — those two legacy entity classes are also the active projectile
     system for a cluster of Phase-4 mob/boss content (`EntityAIMaskmanLasergun`,
     `EntityAIMaskmanMinigun`, `EntityBOTPrimeBase` boss, `EntityUFO`, `EntityDeathBlast`,
     `EntityHunterChopper`, `EntityCyberCrab`) plus one legacy turret model
     (`TileEntityTurretHoward`/`HowardDamaged`, distinct from the main `TileEntityTurretBaseNT`
     `docs/phase3/turret_system.md` already scoped). It is real, parallel, still-referenced content —
     just not what PORT_SPEC's "gun framework" phrase is pointing at, and small enough (2 held items)
     that it does not justify porting a second full weapon-item base class. **Recommendation**: port
     the 2 legacy guns' *presentation* (name, model, recipe) but re-implement their firing behavior on
     `ItemGunBaseNT`/`GunConfig`/`BulletConfig` rather than porting `ItemGunBase` and
     `handler.guncfg.*` verbatim — this halves the base-class surface this phase has to design against
     with no player-visible behavior change, since both legacy guns are ordinary semi-auto/shotgun
     patterns the modern system already expresses natively. The legacy *entity* classes
     (`EntityBulletBase`/`EntityBullet`) still need porting regardless, but as Phase 4 mob/boss
     content, not as part of this package — flagged for that phase's own research, not resolved here.
2. **The "ballistics core" is more fundamental than the gun item, and turrets already depend on it.**
   `BulletConfig` + `EntityBulletBaseMK4`/`EntityBulletBaseMK4CL`/`EntityBulletBeamBase` (the ammo
   definition and the projectile/hitscan entities) are consumed directly by
   `TileEntityTurretBaseNT.spawnBullet()` (`docs/phase3/turret_system.md`, already confirmed and
   already deferring this exact package) via a dedicated turret-facing `EntityBulletBaseMK4`
   constructor that takes raw yaw/pitch instead of a shooter entity — **no `ItemGunBaseNT`,
   `GunConfig`, `Receiver`, or held `ItemStack` involved at all**. This means the real dependency
   graph is three tiers, not two: (a) a **ballistics/ammo core** (`BulletConfig` +
   `EntityThrowableNT`/`Interp` + the three concrete projectile/beam entities) with no dependency on
   anything above it, consumed independently by both guns and turrets; (b) a **held-weapon state
   machine** (`ItemGunBaseNT` + `GunConfig` + `Receiver` + `Lego`/`GunStateDecider` + the magazine and
   weapon-mod subsystems) that sits on top of (a) and is unique to player/mob-held guns; (c) **content**
   (the 63 `XFactory*`-declared guns, the ~23 concrete `WeaponMod*` effect classes, the HUD/animation/
   sound tables). This matches the shape RBMK's own report found (a narrow, genuinely-novel core
   underneath a much larger "just data" surface) and should drive the same kind of package split.
3. **"Recoil" and "spread" are two unrelated systems in CE, not one buildup-and-decay mechanic.**
   PORT_SPEC's phrasing ("recoil/spread-buildup-and-decay state machine") implies a single system;
   CE has no such thing. What exists instead:
   - **Spread** (`Lego.calcSpread`) is a **pure, memoryless sum** of four independent terms recomputed
     fresh on every shot — the gun's innate spread, the loaded ammo's spread × the gun's ammo-spread
     modifier, a flat hip-fire penalty (zeroed while aiming), and a **wear-based** term that only
     turns on past 50% of the weapon's durability budget and scales linearly to 100% wear. There is no
     tick-by-tick "heat" that climbs while firing and cools while idle — the only thing that
     accumulates over a gun's lifetime is wear (see below), and wear only ever increases (short of a
     repair item resetting it externally). **This is the actual "ballistics math" pure-logic core.**
   - **Recoil** (`ItemGunBaseNT.recoilVertical/Horizontal/Decay/Rebound`, set via the static
     `setupRecoil(...)` helper called from inside a gun's `fire()` lambda, e.g. every `XFactory*.java`
     gun config) is a **client-only camera-kick effect** — static mutable floats consumed by
     client-side camera-pitch code (outside this survey's file set; not found in any file read here)
     that presumably decay each render/client tick by `recoilDecay` and spring back by
     `recoilRebound`. It has **zero effect on where the bullet actually goes** — the projectile's
     `shoot()` call already happened using `calcSpread`'s value before `setupRecoil` is invoked in the
     same `fire()` lambda. Getting this distinction right matters for the port: the gameplay-affecting
     "accuracy" math (spread) is a small, pure, unit-testable function; the visually-affecting
     "kick" (recoil) is a Phase-5-flavored client camera effect with no bearing on hit resolution and
     should not be entangled with the ballistics core's tests.

## Suggested Phase 3 gun-framework work-package split

Following the same three-tier shape as the headline finding, in build order:

### Package A — Ballistics/ammo core (build first; this is PORT_SPEC's actual unit-test target)
`BulletConfig`, `DamageSourceSednaNoAttacker`/`WithAttacker`, `EntityThrowableNT`, `EntityThrowableInterp`,
`EntityBulletBaseMK4`, `EntityBulletBaseMK4CL`, `EntityBulletBeamBase`, `IBulletBase`. This is the
projectile/hitscan simulation and the ammo data model — the same substrate `docs/phase3/
turret_system.md` is already waiting on (its own Deferred scope item #1). It needs only: an
`EntityType`/`DeferredRegister` registration (pattern already confirmed live in this port, see Key
design decisions), a `DamageType` per `DamageResistanceHandler.DamageClass` value (Phase 0 already
did 8 of the 9 needed — see Open questions), and `EntityDamageUtil.attackEntityFromNT`'s armor-piercing
math (a narrow slice of the much larger `DamageResistanceHandler`, see Deferred scope). It has **no
dependency on `ItemGunBaseNT`, `GunConfig`, or any held-item state** — this is the package to write
the unit tests PORT_SPEC asks for against.

### Package B — Held-weapon state machine (sequence after Package A)
`ItemGunBaseNT`, `GunConfig`, `Receiver`, `Lego` (server-relevant portion), `GunStateDecider`, the
`mags` package (8 files), `IKeybindReceiver`/`IEquipReceiver`/`IItemHUD`/`IHoldableWeapon`. This is
the reload/fire/jam/draw state machine and the mag-refill inventory scan. It depends on Package A only
for "what does `fire()` actually spawn" (`BulletConfig` + the projectile constructors) — the state
machine itself never touches a projectile entity directly, only through the `Receiver.onFire`
lambda's body (which is itself just a call into `Lego.doStandardFire`, Package A's consumer, not
Package B's own logic).

### Package C — Weapon-mod (attachment) eval chain (sequence after Package B)
`XWeaponModManager`, `IWeaponMod`, `WeaponModBase`, and the ~23 concrete `WeaponMod*` effect classes
(silencer, scope, sawed-off, choke, caliber-swap, drill bits, engine swaps, etc. — signature-surveyed,
not read in full, since each is a small, mechanically-similar `IWeaponMod.eval()` override; see
Deferred scope for the exact list). Needed for full parity (guns are meaningfully different with mods
installed) but not for a gun to fire correctly with its default loadout — Package B's state machine
runs unmodified `GunConfig`/`Receiver` DNA values if the mod-eval layer isn't wired yet, since
`XWeaponModManager.eval()` is a pure pass-through when a stack has no mod NBT.

### Package D — Content (the 63 `XFactory*`-declared guns + client-only files, largest but shallowest)
`GunFactory`, all 23 `XFactory*.java` files (4,977 lines), `Orchestras` (1,694 lines — almost entirely
per-gun reload sound-cue timing tables), `ConfettiUtil`, the `hud` package (3 files, `IHUDComponent`/
`HUDComponentAmmoCounter`/`HUDComponentDurabilityBar`), `LegoClient`/`GunFactoryClient` (client
rendering/registration glue). This is "just data" once Packages A–C exist — each `XFactory*.init()`
call is a sequence of `new ItemGunBaseNT(quality, "name", new GunConfig()...)` builder chains with no
novel logic, matching the volume-not-depth shape RBMK's own report found for its "Package B" fluid
columns. Recommend splitting across several implementation-wave agents purely by line count, not by
any structural boundary — there is none to find.

### Not part of this package (own research already exists, or explicitly out of scope)
- **Turrets** (`TileEntityTurretBaseNT` + 13 concrete TE + 14 blocks) — `docs/phase3/
  turret_system.md`, which already correctly treats Package A above as its own prerequisite. Do not
  re-scope turrets here.
- **Grenades** (`com.hbm.items.weapon.grenade.*`, `ItemGenericGrenade`, `ItemGrenadeDynamite`,
  `ItemGrenadeFishing`, `ItemDisperser`) — PORT_SPEC lists these as a separate Phase 3 bucket
  alongside "the CE gun framework"; `ItemGrenadeUniversal.java` (289 lines, sized but not read in
  full here) is a `ItemThrowable`-style class with its own fuze/explosion-radius state, structurally
  unrelated to `ItemGunBaseNT`/`BulletConfig`. Recommend a dedicated grenade research pass rather than
  folding it into this report on the strength of both being "Phase 3 weapons."
- **Missiles/artillery, bombs/detonators, the explosion engine** — already have their own Phase 3
  research reports (`missile_framework.md`, `missile_launch_infra.md`,
  `bomb_blocks_and_detonators.md`, `explosion_engine.md`) on disk; not re-derived here even where a
  gun (`gun_missile_launcher`, per `XFactoryRocket.java`) fires a submunition that eventually explodes
  — the explosion itself is those reports' scope, the gun's fire-state-machine and its projectile's
  flight are this one's.
- **Armor / FSB / hazmat integration** — PORT_SPEC's Phase 3 bullet point, but `ArmorFSB`/
  `ArmorNCRPARanged`/`DamageResistanceHandler`'s full resistance-table system is a general combat
  system consumed by far more than guns (melee, explosions, environmental damage) — see Deferred
  scope. Needs its own research pass; this report only names the one narrow slice
  (`EntityDamageUtil.attackEntityFromNT`'s pierce parameters) that guns specifically feed into it.

## Phase-3-safe scope (Package A + B detail)

All class/line counts below are from the CE files actually read or precisely `wc -l`'d.

### Package A — ballistics/ammo core

| Class | Lines | Portability |
|---|---|---|
| `BulletConfig` | 470 | The ammo/projectile "stat card" — a plain mutable POJO with a fluent builder (`setVel`/`setSpread`/`setGrav`/`setDamage`/`setArmorPiercing`/`setRicochetAngle`/... — 30+ setters, all pure) plus 5 static `BiConsumer<Entity, RayTraceResult>` lambda fields (`LAMBDA_STANDARD_RICOCHET`/`LAMBDA_STANDARD_ENTITY_HIT`/`LAMBDA_STANDARD_BEAM_HIT`/`LAMBDA_BEAM_HIT`) that are the **real** hit-resolution logic and are *not* pure — they call `bullet.world.destroyBlock(...)`, `bullet.world.playSound(...)`, `EntityDamageUtil.attackEntityFromNT(...)`, i.e. full World/Entity access. The config *fields* (velocity, spread, gravity, expires, armor-piercing percent/threshold-negation, headshot multiplier, ricochet angle/count, penetration flags) are the pure ballistics constants; the *lambda bodies* are the glue layer. `BulletConfig.configs` is a static append-only `List` with `id = configs.size()` at construction — every `BulletConfig` instance is a permanent, never-removed global registry entry (mirrors `RBMKColumn`'s registry-by-construction pattern from Phase 2), referenced by raw `int` id over the network via a synced `DataParameter<Integer>` on the projectile entity — **this id is order-dependent and must be assigned in the exact same sequence CE's static-init order produces**, or saved games / synced ids drift (see Open questions). |
| `EntityThrowableNT` (abstract) | 370 | **The real per-tick ballistics physics**, read in full. `onUpdate()`: computes a swept ray from `pos` to `pos + motion*motionMult()` using `Library.rayTraceBlocks` (blocks) and, if `!doesPenetrate()`, `Library.rayTraceEntities` (nearest entity along the same segment) — else a manual AABB-sweep loop over `getEntitiesWithinAABBExcludingEntity` calling `onImpact` once per intersecting entity (this is CE's actual **penetration** implementation: not a damage-falloff formula on a single hit, but literally running the impact callback once per entity whose *expanded* hitbox the segment crosses, in whatever order `getEntitiesWithinAABB` returns them — order is not distance-sorted for the penetrating branch, only the non-penetrating branch tracks "nearest"). Motion integration is `pos += motion*motionMult(); motion *= drag; motion.y -= gravity` — **CE subtracts a flat gravity constant every tick rather than accumulating one, i.e. this is a discrete-time "drop rate" that increases exactly linearly per tick, not real projectile-motion acceleration** (`motionMult()` for `EntityBulletBaseMK4` is `config.velocity + accel`, a per-tick *distance* traveled, not a velocity in blocks/tick², so the whole scheme is "add a fixed 3D step every tick, then bend that step's Y component down by a fixed amount every tick" — algebraically a parabola in the small-angle case, exactly like vanilla arrow physics). `motionMult()`/`getGravityVelocity()`/`getAirDrag()`/`getWaterDrag()`/`doesImpactEntities()`/`doesPenetrate()`/`isSpectral()`/`selfDamageDelay()` are the 8 override points every concrete bullet type customizes — all of them pure `BulletConfig` field reads on `EntityBulletBaseMK4`. **This method needs `World` for the raytrace calls themselves, so it is not literally pure, but every number that goes *into* the raytrace (the step vector, the drag/gravity constants) is computed from pure `BulletConfig` data** — the clean split point for a testable "given this start pos/motion and these constants, where does one tick end up" function is factoring the position/motion update out of the raytrace calls, which this report recommends explicitly (see Key design decisions). |
| `EntityThrowableInterp` (abstract) | 84 | Pure client-side render interpolation (`turnProgress` countdown lerp toward a synced `syncPos`/`syncYaw`/`syncPitch`) — a rewrite of vanilla `EntityThrowable`'s smoothing, no gameplay logic. Trivially portable, or replaceable with 1.21.1's own interpolation delegate (see Key design decisions — Neo Edition's `ProjectileLerping` base class is the confirmed real-API equivalent). |
| `EntityBulletBaseMK4` | 288 | The concrete "physical bullet" entity — 4 constructors (submunition, standard gun-fired, turret-fired by raw yaw/pitch, and the base no-arg for deserialization), a lockon-homing branch in `onUpdate()` (linear-interpolates motion toward a tracked target's position, capped by a `0.005 * ticksExisted` turn-rate ramp, re-normalized to preserve speed — this is CE's entire "guided munition" logic, reused by nothing else in this survey but worth flagging as a second, smaller pure-math nugget alongside spread/damage), and the 8 `EntityThrowableNT` override points listed above, all one-line `BulletConfig` field reads. `onImpact` dispatches, **in this exact fixed order**, `config.onImpact` (custom per-ammo hook, e.g. explosive rounds), then — only if the bullet is still alive — `config.onRicochet` (`BulletConfig.LAMBDA_STANDARD_RICOCHET` by default), then `config.onEntityHit` (`LAMBDA_STANDARD_ENTITY_HIT` by default). **Getting this order right matters**: an ammo type with a custom `onImpact` that kills the bullet (e.g. an explosive round) never reaches ricochet/entity-hit; the standard ammo path runs all three unconditionally in sequence since the standard `onImpact` is `null`. |
| `EntityBulletBaseMK4CL` | 107 | Sized/diffed against `EntityBulletBaseMK4` but not read line-by-line — the `CL` suffix is CE's own naming convention for "chunk-loading" variants elsewhere in the mod (confirmed by `BulletConfig.ProjectileType.BULLET_CHUNKLOADING`), i.e. this is the same physics with a forced chunk-load ticket so long-range projectiles (artillery-fired bullets crossing unloaded chunks) don't despawn — recommend reading in full at implementation time rather than assuming line-for-line duplication, but the *ballistics* content is not expected to differ from `EntityBulletBaseMK4`. |
| `EntityBulletBeamBase` | 373 | The **hitscan** counterpart — read in full. Unlike the two bullet classes, this is not a per-tick-integrated flight: `performHitscan()` (called once, from the constructor, for a player-fired beam — `performHitscanExternal(range)` exists for a non-constructor-time call, e.g. a turret or beam that needs to re-fire) computes a single `headingX/Y/Z` vector scaled to a fixed `range` (250 blocks for the standard constructor), raytraces blocks once, then— for entity hits — either (non-penetrating) finds the single nearest intersecting entity via a AABB-sweep loop identical in shape to `EntityThrowableNT`'s penetrating-bullet branch, or (penetrating) calls `onImpact` once per intersecting entity closer than any `EntityCoin` hit (a curious special case: `EntityCoin` — a themed target/currency entity outside this survey's scope — always blocks non-coin hits behind it and triggers its own nearest-entity-fan-out logic for a "coin flip" gameplay mechanic, not detailed further here as out of the gun-framework's own scope). **No gravity, no `onUpdate` flight loop** — `onUpdate()` only exists to expire the entity after `config.expires` ticks (for its tracer-render lifetime) and to call `config.onUpdate`. This confirms the answer to the "raytrace vs. projectile entity" question the task asked for explicitly: **CE does both, split cleanly by weapon archetype** — physical ammunition (`ProjectileType.BULLET`/`BULLET_CHUNKLOADING`) is a real flying entity with gravity/drag/travel-time; energy weapons (`ProjectileType.BEAM`) are an entity that exists only to carry the hitscan's visual/lifetime state, with all the actual hit resolution happening in one constructor-time raytrace. |
| `IBulletBase` | 12 | Trivial marker interface, not read in detail — one or two getters mirrored across the bullet/beam hierarchy. |
| `DamageSourceSednaNoAttacker` / `WithAttacker` | 23 / 44 | CE's 1.12-era custom `DamageSource` subclasses, keyed by a lowercased `DamageClass` name string. `WithAttacker` overrides `getImmediateSource()`→projectile, `getTrueSource()`→shooter, and builds a `death.sedna.<type>.attacker` translation key death message. **Cannot be ported as subclasses** — 1.21.1's `DamageSource` is a `final` record-like class; see Key design decisions for the confirmed real replacement shape. |

### Package B — held-weapon state machine

| Class | Lines | Portability |
|---|---|---|
| `ItemGunBaseNT` | 496 | The item class itself — read in full. Holds an array of `GunConfig` ("DNA," one per receiver-group — almost every gun has exactly 1, a few dual-purpose weapons like grenade-launcher-underbarrels would use 2, though no 2-config gun was found in this survey's `XFactory*` grep pass), all runtime state stored as flat NBT primitives under per-index string keys (`state_0`, `timer_0`, `wear_0`, `mode_0`, `mouse1_0`, ... — see NBT table below), and the `onUpdate`/`inventoryTick` per-tick driver: on the server, advance each config's `timer` down by 1, and when it hits `≤1`, invoke that config's `decider` lambda (`GunStateDecider.LAMBDA_STANDARD_DECIDER` for every gun surveyed — no gun was found overriding this). `handleKeybind` is the input-edge-triggered half (press/release of primary/secondary/tertiary/reload map to `GunConfig`'s `onPress*`/`onRelease*` lambda slots) — **and it takes `EntityLivingBase`+`IInventory`, not `EntityPlayer`, confirmed by `EntityAIFireGun` calling `gun.handleKeybind(host, null, stack, keybind, state)` with a null inventory for mob-held guns** — the whole state machine is entity-agnostic by design, a real, load-bearing design constraint for the port (see Key design decisions). |
| `GunConfig` | 168 | Per-gun-mode "DNA" — receivers array, durability, draw/inspect durations, crosshair choice, and ~12 `BiConsumer<ItemStack,LambdaContext>` lambda slots (press/release ×4 buttons, decider, smoke handler, orchestra) plus 1 `BiFunction` (animation resolver, client-only). Every getter routes through `XWeaponModManager.eval(...)` before returning — i.e. **every single config value on every gun is mod-overridable**, not just a curated subset. All setters are a trivial fluent builder. |
| `Receiver` | 167 | Per-barrel/chamber "DNA" nested one level under `GunConfig` — base damage, rounds-per-cycle, split-projectiles (for e.g. shotguns firing N pellets per "round"), 4 independent spread terms (see Headline finding #3), auto/dry-fire/reload-on-empty booleans, the full reload-duration 5-tuple (`pre`/`begin`/`cycle`/`end`/`post-cock`), jam duration, fire sound, the `IMagazine` instance, projectile spawn offset (2 variants: hip and aimed-down-sights), and the 3 core behavior lambdas (`canFire: BiFunction<ItemStack,LambdaContext,Boolean>`, `onFire`/`onRecoil: BiConsumer<...>`). Same mod-eval-wrapped-getter pattern as `GunConfig`. |
| `Lego` (server-relevant portion, ~290 of 387 lines — the remainder is one client-only smoke-particle helper) | ~290 | **The actual verb library**, read in full. `clickReceiver` (backs `LAMBDA_STANDARD_CLICK_PRIMARY`): if `IDLE` and `canFire()` passes, `onFire()` fires once, then `roundsPerCycle - 1` additional unconditional-`canFire`-gated fires in the same tick (this is CE's burst-fire primitive — every "3-round burst" or similar gun is just `Receiver.rounds(3)` on top of the standard click handler, no separate burst state machine), then transitions to `COOLDOWN`; on fire-attempt-with-no-ammo, plays a dry-fire animation and transitions to either `COOLDOWN` (if `refireAfterDry`) or `DRAWING`. **`doStandardFire`/`calcDamage`/`calcSpread` are the pure ballistics-math core this task asked to identify explicitly**: `calcDamage = baseDamage * (wear<75%? 1 : 1-(wear%-0.75)*2)`; `calcSpread = innateSpread + ammoSpread*ammoSpreadMult + (aiming?0:hipfireSpread) + (wear<50%? 0 : (wear%-0.5)*2)*durabilitySpread` — **both are plain `float`-in/`float`-out functions once the `GunConfig`/`Receiver`/`BulletConfig` values and the wear percentage are pulled out as parameters**, exactly the shape PORT_SPEC's "unit tests on the pure-logic cores" wants. `doStandardFire` itself is the glue that reads aim state, computes projectile count (`config.projectilesMin` + random up to `projectilesMax`, both scaled by `Receiver.getSplitProjectiles`), and spawns one `EntityBulletBaseMK4`/`MK4CL`/`EntityBulletBeamBase` per projectile via `world.addScheduledTask` on a `WorldServer` (a real Forge-1.12 idiom for deferring entity spawns off the current tick's synchronous callback stack — see Open questions for its 1.21.1 equivalent) — not pure, but a thin, easily-isolated dispatcher once `calcDamage`/`calcSpread` are factored out as the report recommends. `standardExplode`/`tinyExplode`/`resolveImpactFacing` (explosive-round impact handlers, `EnumFacing`-from-motion-vector fallback math) are used by a subset of ammo types' `onImpact` and are themselves a mostly-pure geometry function (`resolveImpactFacing`) wrapping a non-pure explosion-spawn call — out of core gun-framework scope, belongs with whichever package owns `ExplosionVNT`/`docs/phase3/explosion_engine.md`. |
| `GunStateDecider` | 156 | **The state-machine engine**, read in full — `LAMBDA_STANDARD_DECIDER` composes 4 sub-transitions every gun surveyed uses unmodified: `deciderStandardFinishDraw` (`DRAWING`→`IDLE`), `deciderStandardClearJam` (`JAMMED`→`IDLE`, i.e. jams self-clear once their duration elapses, no player action needed), `deciderStandardReload` (drives the multi-cycle reload loop, detailed below), `deciderAutoRefire` (drives held-trigger full-auto fire and the reload-on-empty fallback). `getStandardJamChance(wear%) = wear% < 66% ? 0 : min((wear%-0.66)*4, 1)` — a third pure wear-derived formula, evaluated once per reload completion (not per shot), meaning **jamming is a reload-time risk, not a firing-time risk** — a worn gun does not jam mid-burst, it jams when you finish reloading it. `deciderStandardReload`'s tube-magazine support (`MagazineSingleReload`/`Belt`) is implicit in its own control flow: after one `reloadAction`, if the mag `canReload` again and the operator didn't cancel, it loops back into another `RELOADING` cycle rather than ending — full-reload magazines (`MagazineFullReload`) simply report `canReload() == false` immediately after one action since they load to capacity in a single cycle, so the same decider code handles both without a type check. |
| `mags/IMagazine<T>` | 73 | The reload contract — `getType`/`setType` (ammo type, generic over the mag's chosen "ammo unit," `BulletConfig` for every concrete mag but generic to allow other unit types), `getAmount`/`setAmount`/`getCapacity`, `useUpAmmo` (fire-time deduction), `canReload`/`initNewType`/`reloadAction` (the reload-cycle contract `GunStateDecider` drives), `getAmountBeforeReload`/`getAmountAfterReload` (animation-timing bookkeeping — reload animations key off these rather than the live count because "NBT sync likely arrives after animation packets," per CE's own doc comment, a real client/server race the port must preserve the same workaround for), plus the icon/HUD-text pair. Two static helpers on the interface (`handleAmmoBag`, casing-return bookkeeping into `ItemCasingBag`; `shouldUseUpTrenchie`, a 2-in-3 ammo-conservation roll gated on a specific armor set) are cross-package hooks, not core mag logic. |
| `MagazineSingleTypeBase` (abstract, backs `FullReload`/`SingleReload`) | 246 | The ammo-scanning reload implementation — `standardReload(stack, inventory, loadLimit)` walks the firer's inventory slot-by-slot (plus, recursively, one level into any `ItemAmmoBag`/`ammo_bag_infinite` found in a slot) matching `BulletConfig.ammo` (a `RecipesCommon.ComparableStack` recipe-style matcher) against `acceptedBullets`, loading up to `loadLimit` rounds converted via each config's own `ammoReloadCount` (how many mag-rounds one inventory item is worth — e.g. a single shell item might load 1 round, or a battery cell might load 20). **This method needs a live `IInventory` and cannot run standalone** — passing `inventory == null` (the `EntityAIFireGun`/turret path) short-circuits to "fill to capacity instantly with whatever `acceptedBullets.get(0)` is," i.e. mobs/turrets never actually consume ammo items, only players do. `getFirstConfig`/`getMagType`/`getMagCount` (NBT ints, same per-index key pattern as `ItemGunBaseNT`) are pure once the inventory scan's result is known. |
| `MagazineFullReload` / `MagazineSingleReload` | 18 / 19 | One-line subclasses — the entire semantic difference between "load the whole mag at once" and "load one shell per reload cycle" (revolvers, break-actions, tube-fed shotguns) is the single `loadLimit` argument passed to `standardReload`. |
| `MagazineInfinite` | 39 | A no-op `IMagazine` — fixed 9999 capacity/amount, every mutator a no-op, `canReload` always `false`. Used for debug/creative-only guns; trivially portable. |
| `MagazineBelt` | 171 | Belt-fed weapons (miniguns) — no persisted "loaded count" at all; `getAmount`/`useUpAmmo` scan the firer's inventory live every call (belt guns have no magazine capacity, they consume ammo items directly from inventory each shot, falling through to an `ItemAmmoBag` the same way `MagazineSingleTypeBase` does). `getAmount(stack, null) == 1` is a deliberate `EntityAIFireGun` special case ("mobs always have exactly 1 round available," sidestepping the inventory scan entirely for non-player firers). |
| `MagazineFluid` / `MagazineEnergy` | 73 / 54 | Not read in full — sized and skimmed; these back the chemthrower/flamethrower (fluid-tank-backed ammo) and the tesla cannon/laser weapons (an `IBatteryItem`-backed charge pool) respectively. Both are expected to follow the same `IMagazine` contract shape as the bullet-backed mags but source their "ammo" from this port's already-ported `com.hbm.api.fluidmk2`/HE-energy capabilities instead of an inventory scan — flagged as needing a full read at implementation time, not assumed identical to `MagazineSingleTypeBase`. |
| `IKeybindReceiver` / `IEquipReceiver` / `IItemHUD` / `IHoldableWeapon` | ~11/11/12/19 | Trivial callback/marker interfaces, read in full — `IKeybindReceiver.canHandleKeybind`/`handleKeybind` (+ a client-only default no-op `handleKeybindClient`), `IEquipReceiver.onEquip` (two overloads, hand-based and stack-based, both default no-ops), `IItemHUD.renderHUD` (client-only crosshair/HUD hook), `IHoldableWeapon` (not read past its 19-line signature — a marker for third-person holding-pose selection). All four already have confirmed real analogues in this port's own ground rules (events replace Forge's item-hold hooks) or are simple enough to port as-is. |

### NBT keys → Data Component notes (Package B)

Every one of `ItemGunBaseNT`'s ~20 per-config-index state keys (`state_N`, `timer_N`, `mode_N`,
`wear_N`, `mouse1_N`/`mouse2_N`/`mouse3_N`/`reload_N`, `lastanim_N`, `animtimer_N`, plus the 4 global
keys `drawn`/`aiming`/`lockontarget`/`lockedon`/`cancel`/`eqipped`) is a flat NBT primitive
(int/float/byte/bool) read/written through 4 tiny generic helper pairs
(`getValueInt`/`setValueInt`/... operating on `stack.getTagCompound()`), plus `MagazineSingleTypeBase`
adds 4 more per-mag-index keys (`magcount_N`/`magtype_N`/`magprev_N`/`magafter_N`) and
`XWeaponModManager` adds one int-array key per config index (`KEY_MOD_LIST_N`, the installed mod-id
list). This is the single most Data-Component-friendly NBT shape encountered in this port so far —
every value is a primitive with no nesting — but the *per-config-index* naming convention (string
concatenation, `"state_" + index`) does not map cleanly onto typed Data Components (which are keyed by
a fixed `DataComponentType`, not a runtime-computed string) unless every field is redesigned as
**one component holding a `List`/array indexed by config number** rather than N independently-typed
components. Recommend one `GunStateComponent` (a small record: `state`, `timer`, `mode`, `wear`,
button-bitset, last-anim, anim-timer) **per config slot**, i.e. a `List<GunStateComponent>` component
on the stack sized to `configs_DNA.length`, plus a separate `List<MagState>` component for the mags —
this preserves the "one config = one state bundle" shape CE's naming convention encodes without
inventing per-index component types. Document this concretely in `docs/nbt-components.md` per Ground
Rule 2's requirement once Package B is implemented — this report only identifies the shape, not the
final codec.

## Deferred scope

Real dependencies of *this specific* subsystem that belong to other packages/phases, matching the
"which package, which phase" format the ground rules ask for:

- **`com.hbm.util.DamageResistanceHandler`** (608 lines, partial read: enum + method signatures only)
  — a general armor/resistance-table system consumed by every damage path in the mod, not just guns
  (melee, explosions, environmental hazards all route through `EntityDamageUtil.attackEntityFromNT`
  too). Guns only ever call it through 2 parameters (`armorThresholdNegation`/`armorPiercingPercent`
  on `BulletConfig`, threaded through `DamageResistanceHandler.setup(pierceDT, pierce)` as **shared
  mutable static state** set immediately before, and reset immediately after, each
  `attackEntityFromNT` call — the same shared-static-state shape RBMK's own report flagged as a
  latent-bug risk for `RBMKNeutronHandler`, and worth the same explicit preserve-vs-fix decision here,
  see Open questions). This needs its own research pass (likely paired with the "armor sets + FSB
  armor modifier system" bullet in PORT_SPEC's own Phase 3 description) — this report only names the
  narrow interface surface guns need from it.
- **`com.hbm.items.armor.{ArmorFSB,ArmorNCRPARanged,ArmorNCRPAMelee,ArmorRPAMelee,IPARanged,
  IPAMelee}`** — power-armor/force-shield-belt integration referenced by name in
  `docs/phase1/items_special.md`'s bucket (a) as an `ItemFusionCore` cross-package dependency, and by
  PORT_SPEC's own Phase 3 line item. Not touched by this report; guns interact with armor only through
  the generic `DamageResistanceHandler` pierce parameters above, nothing gun-specific.
- **`com.hbm.items.weapon.grenade.*`, `ItemGenericGrenade`, `ItemDisperser`** — a structurally
  separate throwable-explosive item family (see Headline finding's "not part of this package" list).
  Own research pass recommended.
- **The legacy `ItemGunBase`/`ItemGunShotty`/`ItemGunVortex`/`handler.guncfg.*` system's 2 live guns**
  — recommend re-implementing on `ItemGunBaseNT` rather than porting the legacy base class (see
  Headline finding #1); if that call is rejected, `handler.guncfg.*` (7 files, 998 lines) and
  `ItemGunBase` (763 lines) become a 4th, parallel work package this report does not scope in detail.
- **The legacy `EntityBulletBase`(684 lines)/`EntityBullet`(809 lines) projectile system** — still
  live for Phase 4 mob/boss content (`EntityBOTPrimeBase`, `EntityUFO`, `EntityDeathBlast`,
  `EntityHunterChopper`, `EntityCyberCrab`, the two `EntityAIMaskman*` AI classes) and one legacy
  turret model. `docs/phase0/STATUS.md`-style phase ownership: this belongs with whichever Phase 4
  package researches those mobs, not here — flagged loudly (per Headline finding #1) because it is
  easy to assume "the gun framework report already covered bullets."
- **`com.hbm.explosion.vanillant.*` (`ExplosionVNT`, `EntityProcessorCrossSmooth`,
  `ExplosionEffectWeapon`/`Tiny`, `PlayerProcessorStandard`)** — consumed by `Lego.standardExplode`/
  `tinyExplode` for explosive ammo's impact effect and by `BulletConfig.LAMBDA_STANDARD_RICOCHET`'s
  red-barrel/`BlockDetonatable` interactions. Already fully owned by `docs/phase3/explosion_engine.md`
  — not re-derived here, guns are simply one more caller of that engine.
- **`com.hbm.entity.item.EntityCoin`** — referenced inside `EntityBulletBeamBase.performHitscan()`'s
  special-case "coin flip" branch (a themed target/currency mechanic). Out of gun-framework scope;
  whichever package owns that entity should confirm the beam's special-case interaction once it
  exists. Until then this branch degrades gracefully (a beam simply never encounters an `EntityCoin`
  if that entity doesn't exist yet, exactly like every other documented forward reference in this
  port).
- **~23 concrete `WeaponMod*` effect classes** (`WeaponModScope`, `WeaponModSilencer`,
  `WeaponModChoke`, `WeaponModCaliber`, `WeaponModDrill`/`DrillFortune`, `WeaponModEngine`,
  `WeaponModSlowdown`/`MinigunSpeedup`/`ShredderSpeedup`, `WeaponModSawedOff` +3 gun-specific sawed-off
  variants, `WeaponModPolymerFurniture`, `WeaponModBayonet` ×2, `WeaponModStackMag`,
  `WeaponModUziSaturnite`, `WeaponModNickel`, `WeaponModLas{Shotgun,Capacitor,Auto}`,
  `WeaponModGreasegun`, `WeaponModGeneric{Damage,Durability}`, `WeaponModOverride`, 3
  `WeaponModTest*` debug mods) — signature-surveyed only (each is a small `IWeaponMod.eval()`
  override per `mods/` package total of 1,548 lines across all files). This is Package C content per
  the work-split above; recommend one implementation-wave agent per 4-6 mods rather than a full
  individual read-through here, since the interface contract (`IWeaponMod.eval`) fully determines
  their shape.
- **Client-only files**: `LegoClient` (702 lines), `GunFactoryClient` (255 lines), `Orchestras`
  (1,694 lines — per-gun reload/foley sound-cue timing tables, `GunConfig.orchestra` consumers),
  `ConfettiUtil` (84 lines, death-effect spawning), the `hud` package (3 files, 134 lines,
  `IHUDComponent`/ammo-counter/durability-bar HUD widgets), and the `render.item.weapon.sedna.*`
  package (54 `ItemRender*` classes found by this report's initial grep, not sized or read) — all
  Phase 5 ("Client & UX," per PORT_SPEC's own phase description covering "armor/gun HUD" and "all
  ~100+ GUI screens' visual parity") rather than Phase 3 logic, even though they physically live under
  `items/weapon/sedna/**` in CE's package layout.
- **`com.hbm.render.anim.sedna.{AnimationEnums,BusAnimationSedna,BusAnimationSequenceSedna}`** and
  the `animloader`-driven bus-animation system PORT_SPEC names explicitly ("reload/recoil/animation
  via `animloader`") — referenced constantly by `GunConfig.animations_DNA`/`Lego.LAMBDA_DEBUG_ANIMS`
  but not read in this survey; this is the client-side rig/animation-timeline system and belongs with
  Phase 5's rendering rewrite, not this report's state-machine scope. The *state machine*'s only
  contract with it is calling `ItemGunBaseNT.playAnimation(player, stack, animEnum, index)` at the
  right transition points (already fully catalogued in the Package B table above) and networking a
  `GunAnimationPacketSedna`/`HbmAnimation`-shaped payload — the payload itself is trivial (an
  animation-enum ordinal + a sub-index), see Key design decisions.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and
Neo Edition's parallel gun-item port for NeoForge API shape — no NeoForge API is invented below):

- **`Item#onUpdate` → `Item#inventoryTick(ItemStack, Level, Entity, int slotId, boolean isSelected)`,
  confirmed real** by Neo Edition's `GunBaseNTItem.inventoryTick` override (1.21.1's actual method
  name/signature, cross-checked, not guessed). The `isSelected`/`isHeld` boolean and the client-vs-
  server branch CE's `onUpdate` already has map onto this 1:1 — the "reset state when not
  held"/"advance timer and run decider when held" logic in `ItemGunBaseNT.onUpdate` needs no
  restructuring, only a signature and level-check update (`level instanceof ServerLevel` replaces
  `!world.isRemote`).
- **`Item#addInformation` → `Item#appendHoverText(ItemStack, TooltipContext, List<Component>,
  TooltipFlag)`, confirmed real** by the same Neo Edition file — `List<String>` tooltip lines become
  `List<Component>`, and every hand-built tooltip string in `ItemGunBaseNT.addInformation` (ammo
  state, base damage, condition %, quality banner) becomes a `Component.translatable(...)` +
  `.withStyle(ChatFormatting...)` chain instead of raw `EnumChatFormatting` string concatenation —
  meaning **every tooltip line CE hardcodes in English needs a lang-file key**, not a straight string
  port, consistent with Ground Rule 2's "no placeholder assets" mandate.
- **Gun animation packets follow this port's already-confirmed `CustomPacketPayload` +
  `StreamCodec` + `RegisterPayloadHandlersEvent` shape** (`com.hbm.packet.HbmNetwork`, read in full —
  currently registers exactly one live payload, `BufPacket`, from Phase 2's block-entity sync). CE's
  `GunAnimationPacketSedna`/`MuzzleFlashPacket`/`GunFXPacket` (server→client) and
  `SetGunAnimPacket`/`GunButtonPacket` (client→server) each become one `record ... implements
  CustomPacketPayload` with a `Type<...>` constant and a `StreamCodec`, added to `HbmNetwork`'s
  `registerPackets` via `registrar.playToClient(...)`/`playToServer(...)` — following the exact
  template `HbmNetwork`'s own doc comment already lays out for future phases. Neo Edition's
  `PacketDistributor.sendToPlayer(serverPlayer, payload)` call (confirmed real, used in
  `GunBaseNTItem.playAnimation`) is the send-side API to use in place of CE's
  `PacketDispatcher.wrapper.sendTo(...)`. The keybind-edge packets (`GunButtonPacket`/
  `SetGunAnimPacket`) are the client→server direction of `IKeybindReceiver.handleKeybind` — this
  port's confirmed keybind-relay pattern (if one already exists from an earlier phase) should be
  reused rather than re-invented; if none exists yet, this is a small (2-field: keybind enum ordinal +
  boolean state) payload with no gun-specific complexity.
- **`DamageSource` cannot be subclassed in 1.21.1** (it is effectively a final data holder over a
  `Holder<DamageType>` + optional direct/causing entities) — CE's `DamageSourceSednaNoAttacker`/
  `WithAttacker` become **`level.damageSources().source(ResourceKey<DamageType>, @Nullable Entity
  directEntity, @Nullable Entity causingEntity)`** calls at the point of damage (inside whatever
  replaces `BulletConfig.getDamage`), not a class hierarchy. **This port's `com.hbm.damage.
  ModDamageTypes` (read in full) already anticipated this exact need**: it pre-registers 8
  `SEDNA_*` `ResourceKey<DamageType>` constants (`SEDNA_PHYSICAL`/`FIRE`/`EXPLOSION`/`ELECTRIC`/
  `LASER`/`MICROWAVE`/`SUBATOMIC`/`OTHER`) with a comment explicitly calling them out as "generic
  Sedna weapon-config damage categories ... mirror the Neo Edition reference 1:1." **This is a real,
  already-committed forward reference this package should consume, not reinvent** — `BulletConfig`'s
  `dmgClass` field (currently `DamageResistanceHandler.DamageClass`, a 9-value enum:
  `PHYSICAL/FIRE/EXPLOSIVE/ELECTRIC/PLASMA/LASER/MICROWAVE/SUBATOMIC/OTHER`, confirmed by reading the
  enum directly) should map its damage-source construction onto these `SEDNA_*` keys. **See Open
  questions for a real gap this cross-check found**: `ModDamageTypes` has no `SEDNA_PLASMA` entry,
  even though `DamageClass.PLASMA` is a real, distinct enum value CE's switch statement on
  `dmgClass` explicitly does not group into any other case (only `ELECTRIC, LASER, SUBATOMIC` share a
  fallthrough branch in `BulletConfig.getDamage`'s switch; `PLASMA` was simply never given its own
  `setXxxDamage()` marker call there, but the enum value itself is very much live and used elsewhere
  in the mod's own damage-class checks per `DamageResistanceHandler`).
- **The "physical bullet vs. hitscan beam" split maps directly onto two `EntityType`s**, following
  this port's own already-confirmed `EntityType.Builder`/`DeferredRegister<EntityType<?>>` pattern
  (`com.hbm.entity.ConveyorEntityTypes`, read in full — the port's first and so-far-only entity
  registration, itself modeled on Neo Edition's confirmed-real `NtmEntityTypes`, whose own
  `BULLET_MK4`/`BULLET_BEAM` entries this port's own doc comment names explicitly as the pattern to
  follow: `EntityType.Builder.of(ctor, MobCategory.MISC).sized(w,h).setTrackingRange(n).build(name)`).
  Recommend a `GunEntityTypes` (or similarly-scoped, per-family) class exactly mirroring
  `ConveyorEntityTypes`'s shape for `BULLET_MK4`/`BULLET_MK4CL`/`BULLET_BEAM`, registered once
  Package A lands — `ConveyorEntityTypes`'s own doc comment explicitly leaves this "own registry vs.
  fold into one shared class" choice open for whoever lands next, so this is not a re-litigation of a
  settled decision.
- **Vanilla `Projectile`'s owner-tracking (`setOwner`/`getOwner`) is the confirmed real 1.21.1
  replacement for CE's hand-rolled `thrower`/`throwerName` fields** — Neo Edition's `BulletBaseMK4`
  (header read) calls `this.setOwner(living)` rather than reimplementing CE's
  `EntityThrowableNT.throwerName`-based re-resolution-after-reload workaround (CE's own comment flags
  this as a 1.12-era limitation: entities couldn't reliably persist a UUID reference across a save
  round-trip, so it stored a player *name* string and re-looked-up the entity by name on load — 1.21.1
  has no such limitation). Recommend the port's `EntityThrowableNT`-equivalent extend vanilla
  `Projectile` (or this port's own future projectile base, if one exists by the time this package
  lands) and drop the name-based thrower-recovery workaround entirely rather than porting it — it
  solves a problem 1.21.1 doesn't have. Confirm no other package has already made this same "extend
  `Projectile`" decision for a different projectile before Package A does, since two mutually-
  incompatible NT-projectile base classes would be a real problem.
- **This port's Menu/Screen framework (`MenuBase`, `GuiInfoContainer`, read in full) is not needed by
  Package A or B at all.** Neither `ItemGunBaseNT` nor `BulletConfig` opens an `AbstractContainerMenu`
  — the only GUI CE's gun system touches is `GUIWeaponTable`/`ContainerWeaponTable` (the weapon-mod
  install bench, referenced only by `ItemGunBaseNT.addInformation`'s `Minecraft.getMinecraft().
  currentScreen instanceof GUIWeaponTable` check), which is Package C/D territory (an ordinary
  block-opened container menu, not part of the gun item's own state machine) and should use
  `MenuBase`/`GuiInfoContainer` like every other Phase 2/3 machine GUI once it's built — no new GUI
  framework needed, consistent with the ground rules' explicit instruction.
- **`WorldServer#addScheduledTask` (CE's mechanism for deferring `Lego.doStandardFire`'s entity
  spawns off the firing tick) has no like-for-like 1.21.1 equivalent found in this port's own code or
  in Neo Edition** — 1.21.1's `ServerLevel` has no public "schedule a task for later this tick"
  API matching Forge-1.12's `addScheduledTask`. This needs a real decision at implementation time
  (spawn the projectile entity synchronously inside the keybind/decider call instead, which is very
  likely simply correct and safe in 1.21.1's single-threaded server tick model — CE's own use of
  `addScheduledTask` here reads as defensive 1.12-era caution against re-entrant world mutation during
  an item-tick callback, not a load-bearing ordering requirement) — flagged rather than resolved,
  since asserting "just spawn it synchronously" as fact would be inventing behavior rather than
  reading it.

## Open questions / risks

- **`BulletConfig.configs`' append-only static list assigns ids by construction order** — CE's own
  static-initializer sequence (`GunFactory.init()` calling each `XFactory*.init()` in a fixed textual
  order) is what determines every `BulletConfig`'s numeric id, which is then the *wire format* for
  which ammo type a projectile entity is carrying (`DataParameter<Integer> BULLET_CONFIG_ID`). The
  port must either (a) preserve the exact same construction-order sequence across every `XFactory*`
  file so ids line up byte-for-byte with CE (fragile — a single reordered `init()` call silently
  desyncs every synced bullet-config id), or (b) replace the raw int id with a `ResourceLocation`-keyed
  registry lookup (heavier per-packet payload, but immune to reordering). This is exactly the kind of
  "preserve CE's fragile implicit ordering vs. fix it properly" fork RBMK's report flagged for
  `RBMKNeutronHandler`'s shared static state — recommend deciding explicitly rather than defaulting to
  whichever is easier to write first, and recommend (b) given this port's own general preference for
  explicit `DeferredRegister`-backed ids everywhere else.
- **`ModDamageTypes` is missing a `SEDNA_PLASMA` entry** for `DamageResistanceHandler.DamageClass.
  PLASMA` — confirmed by reading the 9-value enum directly and cross-checking against
  `ModDamageTypes`'s 8 `SEDNA_*` keys. This needs either a 9th key added to `ModDamageTypes` (and its
  bootstrap + datapack registration) before Package A can build a `DamageSource` for any
  plasma-classed ammo, or a confirmed decision that no live CE gun actually uses `PLASMA` (not
  verified in this survey — the `dmgClass` field defaults to `PHYSICAL` and no `XFactory*` file was
  grepped for `setupDamageClass(DamageResistanceHandler.DamageClass.PLASMA)` specifically). Flag and
  check before Package A locks in its `DamageClass`→`DamageType` mapping table.
- **`WorldServer#addScheduledTask`'s 1.21.1 replacement** (see Key design decisions) — needs an
  explicit implementation-time decision, not resolved here.
- **`EntityThrowableNT`'s discrete "subtract a flat gravity constant every tick" motion model is not
  physically accurate projectile motion** (see Package A table) — this is CE's actual, intentional
  behavior (and produces the same drop curve vanilla arrows use), not a bug, but worth a dedicated
  note in whatever unit tests Package A ships so a future contributor doesn't "fix" it into real
  acceleration-based physics and silently change every gun's effective range/drop curve.
- **The penetrating-bullet impact-order is not distance-sorted** (`EntityThrowableNT`'s
  `doesPenetrate()` branch calls `onImpact` for every intersecting entity in whatever order
  `getEntitiesWithinAABBExcludingEntity` returns, not nearest-first) while the **non-penetrating**
  branch explicitly tracks and picks the nearest hit. This asymmetry is easy to lose during a port
  (a natural refactoring instinct is to unify both branches into one distance-sorted loop) but would
  be an observable behavior change for any penetrating ammo that deals falloff damage per body
  penetrated in a stack of entities — flag for an explicit unit test (3+ overlapping hitboxes, assert
  impact-callback order matches CE's raw iteration order, not sorted order) rather than "obviously
  correct" distance-sorting.
- **The shared-static-state pattern in `DamageResistanceHandler.setup(pierceDT, pierce)`/`reset()`**
  (plain static fields, not `ThreadLocal`, set immediately before and reset immediately after each
  `attackEntityFromNT` call) is reentrancy-unsafe by construction — if any damage-event side effect
  (an armor enchantment hook, a `LivingHurtEvent` listener, anything NeoForge fires synchronously
  during `living.attackEntityFrom`/`hurt`) itself triggers a second `attackEntityFromNT` call before
  the first one's `finally` block runs, the second call's pierce values would leak into the first
  call's `calculateDamage`. Not confirmed as an active bug in CE (no such reentrant call site was
  found in this survey), but the same "preserve vs. fix" decision RBMK's report asked for
  `RBMKNeutronHandler`'s static state applies here too — flagged, not resolved, since this system
  belongs to whichever package researches `DamageResistanceHandler` in full (see Deferred scope).
- **Legacy vs. modern gun-base-class scope call** (Headline finding #1) — this report recommends
  re-implementing the 2 legacy guns on `ItemGunBaseNT` rather than porting `ItemGunBase`/
  `handler.guncfg.*` verbatim, but that is a recommendation, not a settled decision; whoever plans the
  Phase 3 implementation wave should confirm it explicitly (it changes the work-package count: 3
  packages if accepted, 4 if `ItemGunBase` needs its own literal port).
- **`MagazineFluid`/`MagazineEnergy` were not read in full** (see Package B table) — flagged
  explicitly so a future agent reads them before assuming they follow `MagazineSingleTypeBase`'s
  inventory-scan shape; fluid/energy ammo sourcing is very likely closer to this port's own
  `com.hbm.api.fluidmk2`/HE-energy capability read pattern than to an `IInventory` scan, which is a
  real difference worth confirming rather than assuming.
- **`EntityBulletBaseMK4CL`'s exact chunk-loading mechanism was not read line-by-line** (see Package A
  table) — flagged so it isn't assumed byte-identical to `EntityBulletBaseMK4` at implementation time;
  1.21.1's forced-chunk-loading API (`ChunkTicketManager`/`Level#setChunkForced` or NeoForge's own
  ticket API) is a different shape than whatever Forge-1.12 mechanism CE used here, and needs its own
  confirmed-real-API check when this file is actually read in full.
