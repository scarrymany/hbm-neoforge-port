# Legacy (pre-Sedna) bullet system & its mob/AI consumers — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemGunBase.java` (763 lines), `ItemGunShotty.java`
  (215 lines), `ItemGunVortex.java` (241 lines) — the legacy gun-item family
- `upstream/hbm-ce/src/main/java/com/hbm/handler/guncfg/{BulletConfigFactory,Gun12GaugeFactory,
  GunCannonFactory,GunDGKFactory,GunEnergyFactory,GunNPCFactory,GunRocketFactory}.java` (7 files, 998
  lines — every factory method, not a signature survey)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/{GunConfiguration,BulletConfiguration,
  BulletConfigSyncingUtil}.java` (92 + 239 + 285 = 616 lines — the legacy "DNA" data model these
  factories build; not named in this task's file list but load-bearing enough to require a full read,
  since `ItemGunBase` and every factory method construct/consume these three classes directly)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/{EntityBulletBase,EntityBullet}.java` (684 +
  809 = 1,493 lines, both in full — the two independent, unrelated concrete projectile entities;
  neither subclasses the other or shares a common abstract parent beyond vanilla `Entity`)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/ai/{EntityAIMaskmanLasergun,
  EntityAIMaskmanMinigun}.java` (113 + 58 lines, both in full)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/botprime/EntityBOTPrimeBase.java` (95 lines, full —
  read only for its `laserAttack()` bullet-firing method per this task's own scope note; the worm-boss
  movement/AI/health machinery is `docs/phase4/entities_bosses.md`'s subject, not re-derived here)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/{EntityUFO,EntityUFOBase,EntityHunterChopper,
  EntityCyberCrab}.java` (467 + 207 + 442 + 111 = 1,227 lines, all in full)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/logic/EntityDeathBlast.java` (93 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/turret/{TileEntityTurretHoward,
  TileEntityTurretHowardDamaged}.java` (233 + 99 lines, both in full — read specifically to resolve
  this task's own claim that these are "the legacy turret model, distinct from
  `TileEntityTurretBaseNT`," see Headline finding)
- `upstream/hbm-ce/src/main/java/com/hbm/lib/ModDamageSource.java` (partial, ~155 of its ~700+ lines:
  every field/method this report's in-scope classes actually call —
  `causeBulletDamage`/`causeTauDamage`/`causeDisplacementDamage`/`causeCombineDamage`/`shrapnel`/
  `radiation`/`s_bullet`/`getIsTau`/`getIsSubatomic`; the other ~200 `DamageSource` singletons and
  `causeXxxDamage` factories belong to whichever areas own those other weapons/hazards, not re-derived)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/deco/TileEntityTrappedBrick.java` (grep +
  targeted read of its `POISON_DART` case, ~15 of 185 lines — a real, additional `EntityBulletBase`
  consumer this task's file list didn't name, found via exhaustive-consumer grep, see Headline finding)
- Repo-wide greps to map every real edge exhaustively: `new EntityBulletBase(`, `new EntityBullet(`,
  `extends EntityBulletBase\b`, `extends EntityBullet\b`, `extends EntityUFOBase\b`,
  `configSet.put`, `loadConfigsForSync`, every `BulletConfigSyncingUtil.<ID>\b` constant referenced
  outside its own declaration file (run individually for `SHELL_NORMAL/EXPLOSIVE/AP/DU/W9`,
  `DGK_NORMAL`, `TEST_CONFIG`, `ZOMG_BOLT`, `getGustavConfig`, `setToGuided()/setToHoming(`), `new
  ItemGunBase(`/`new ItemGunShotty(`/`new ItemGunVortex(`, and `gun_supershotgun`/`gun_vortex` — used
  to trace the *entire* `BulletConfigSyncingUtil` registry to ground truth (see Headline finding #1)
- This port's own `src/main/java/com/hbm/{interfaces/IBullet{Update,Hurt,Hit,Ricochet,Impact}Behavior.java,
  damage/ModDamageTypes.java, util/{EntityDamageUtil.java (full), ContaminationUtil.java (signatures)},
  handler/ArmorUtil.java (signatures), config/WeaponConfig.java, interfaces/IRadiationImmune.java}`
  (read/grepped to confirm exactly what this area's real dependencies already resolve to, see Key
  design decisions) and `docs/phase3/gun_framework.md` (read in full — this report's explicit
  starting point) and `docs/phase4/{entities_bosses.md, hbm_potion_system.md}` (read in full —
  both are sibling reports with direct, load-bearing overlap on this exact package, see Headline
  finding #4 and Deferred scope)
- `upstream/neo-edition/src/main/java/com/hbm/entity/projectile/{BulletBaseMK4,BulletBeamBase}.java`
  (header-checked only) — Neo Edition has **not** ported any part of the legacy bullet system (no
  `BulletBase`/`Bullet`/`GunBase` file exists under its tree), confirmed by directory listing; every
  API-shape claim below is therefore sourced from this port's own already-committed code and from
  Phase 3's already-confirmed decisions for the *sibling* Sedna system, never invented and never
  Neo-Edition-sourced for this specific package

## Headline finding

`docs/phase3/gun_framework.md`'s headline finding #1 correctly identified this system's file
boundaries, but four things in this task's own framing need correcting once the *content* behind
those files is actually traced to ground truth, and a fifth is a direct scope-overlap with an
existing sibling report that needs reconciling rather than re-litigating:

1. **Both of the "still-live" legacy guns are dead code for the one behavior that defines them — they
   cannot fire a bullet on CE HEAD today.** This is not a port gap; it is CE's own current state,
   traced exactly: `ItemGunBase.fire()`/`reload2()`/`tryShoot()`/`canReload()`/`getBeltType()` all
   route ammo lookups through `BulletConfigSyncingUtil.pullConfig(int id)`, which reads a `HashMap`
   populated **only** by `BulletConfigSyncingUtil.loadConfigsForSync()` (called once, from
   `MainRegistry.java:240`). That method registers exactly 15 of the ~155 `int` ids the class
   declares — `TEST_CONFIG`, `SHELL_NORMAL/EXPLOSIVE/AP/DU/W9`, `DGK_NORMAL`, `ZOMG_BOLT`,
   `MASKMAN_BULLET/ORB/BOLT/ROCKET/TRACER/METEOR`, `WORM_BOLT/LASER`, `UFO_ROCKET` — and
   `Gun12GaugeFactory.getShottyConfig()` (`gun_supershotgun`'s `GunConfiguration`) references
   `G12_NORMAL/INCENDIARY/SHRAPNEL/DU/AM/SLEEK`, while `GunEnergyFactory.getVortexConfig()`
   (`gun_vortex`'s) references `R556_STAR` — **none of which are ever registered**. Concretely: for
   `gun_supershotgun` (`reloadType = RELOAD_NONE`), `tryShoot()` calls
   `getBeltSize(player, getBeltType(...))`; `getBeltType()` calls `pullConfig(G12_NORMAL)`, gets
   `null`, and returns `null` immediately; `getBeltSize(player, null)` then compares every inventory
   stack's `getItem()` against `null` (never true for a real `ItemStack`) and returns `0` — so
   `tryShoot()` is `false` on every call, and `fire()` is never even invoked. For `gun_vortex`
   (`reloadType = RELOAD_FULL`), `tryShoot()` checks `getMag(stack) > 0`; the mag can only ever be
   filled by `reload2()`, which itself calls `pullConfig(R556_STAR)`, gets `null`, and `return`s
   immediately without touching the mag NBT — so the mag starts at (NBT-default) `0` and can never
   increase, and `tryShoot()` is permanently `false` here too. **`ItemGunVortex.spawnProjectile()`'s
   entire hitscan-railgun override (`Library.rayTraceEntitiesOnLine` + flat 100 radiation damage) and
   `onFireClient`'s full particle-beam VFX chain are therefore unreachable code in CE HEAD today** —
   not merely "decorative-only," but never invoked by any player action. This strongly *validates*,
   rather than merely permits, Phase 3's decision to port these two items as decorative-only shells:
   nothing observable is lost, because CE's own current build has already lost it. The only
   observably-live behavior either item has today is `ItemGunShotty`'s right-click meathook grapple
   (`onItemRightClick`/`updateClient`/`updateServer`'s hooked-entity swing logic — entirely independent
   of the ammo system) and `ItemGunVortex`'s crosshair-HUD render (`renderHUD`, independent of firing).
   A third, unrelated consumer of the *same* dead-registry pattern was found via exhaustive grep:
   `TileEntityTrappedBrick`'s `POISON_DART` trap case (`entity_bullet_mk2` via
   `BulletConfigSyncingUtil.G20_CAUSTIC`, also never registered) spawns an `EntityBulletBase` that is
   `setDead()` inside its own constructor before ever being positioned — the "poison dart" decorative
   trap has never fired a working dart either, on the exact same evidence trail. This pattern (an
   ammo `int` id declared, its `BulletConfiguration` factory method written, but never wired into
   `loadConfigsForSync()`) is CE's own accumulated dead weight, not something this port needs to
   preserve as working behavior anywhere it appears.
2. **The 998-line `com.hbm.handler.guncfg` package is, by the same registry trace, roughly 90% dead
   weight at the "does any live code path ever call this" level.** `GunCannonFactory`'s 5
   `getShellXxxConfig()` methods and `GunDGKFactory.getDGKConfig()` (`@Deprecated` in CE's own source)
   populate `configSet` under `SHELL_*`/`DGK_NORMAL`, but grep across the entire CE tree finds **no
   `GunConfiguration.config.add(...)` call and no `new EntityBulletBase(world, BulletConfigSyncingUtil.SHELL_*` /
   `DGK_NORMAL` call anywhere** — these five `BulletConfiguration`s exist solely as unread `HashMap`
   entries. `GunRocketFactory.getGustavConfig()` (a "Carl Gustav Recoilless Rifle" `GunConfiguration`)
   has **zero callers anywhere**, not even a registered `int` id — no `ItemGunBase` is ever built from
   it. `BulletConfiguration.setToGuided()`/`setToHoming(ItemStack)` (which would pull in
   `BulletConfigFactory.getLaserSteering()`/`getHomingBehavior()`) have zero callers. The **only**
   `guncfg` content any live game code ever reaches is: `GunNPCFactory.java` in full (344 lines — the
   8 mob/boss ammo definitions listed in point 1), plus the three `BulletConfigFactory` builder
   methods `GunNPCFactory` actually calls as a base template (`standardBulletConfig()`,
   `standardGrenadeConfig()`, and `standardRocketConfig()` transitively via
   `GunRocketFactory.getRocketConfig()`, ~75 combined lines), plus `GunDGKFactory`'s `CASINGDGK`
   `SpentCasing` constant (cosmetic, see point 3). That is roughly 420 of 998 lines — the rest is
   either unregistered or registered-but-uncalled. This matters directly for the "is
   `BulletConfigFactory`'s config shape meaningfully different from Sedna's, or portable with a
   compatibility shim" question this task asks: there is no need to design a shim for ~150 ammo
   definitions when only 8 are real.
3. **`TileEntityTurretHoward`/`TileEntityTurretHowardDamaged` are not "the legacy turret model,
   distinct from `TileEntityTurretBaseNT`" as this task's own framing states — they are concrete
   subclasses of `TileEntityTurretBaseNT` itself**, read in full to confirm:
   `public class TileEntityTurretHoward extends TileEntityTurretBaseNT implements IGUIProvider`,
   loading ammo via `this.getFirstConfigLoaded()` returning a `com.hbm.items.weapon.sedna.BulletConfig`
   (`XFactoryTurret.dgk_normal.id`) — the *modern*, already-Phase-3-ported ammo system, not
   `BulletConfiguration`/`EntityBulletBase` at all. Neither `TileEntityTurretHoward` nor
   `TileEntityTurretHowardDamaged` spawns any projectile entity in its own source — the actual bullet
   spawn happens inside the inherited `TileEntityTurretBaseNT` machinery (already
   `docs/phase3/turret_system.md`'s scope, which already lists "Howard/HowardDamaged" by name in its
   own concrete-TE roster and flags the `HowardDamaged` subclass specifically as "not read in full").
   Howard's **only** two touchpoints with anything this report's file set covers are both purely
   cosmetic and entirely self-contained: `GunDGKFactory.CASINGDGK` (a `SpentCasing` shell-ejection
   particle/model constant — not a `BulletConfiguration`, never routes through
   `BulletConfigSyncingUtil`), and a hand-rolled CIWS-style "shrapnel" tick
   (`WeaponConfig.ciwsHitrate` config chance → `EntityDamageUtil.attackEntityFromIgnoreIFrame(target,
   ModDamageSource.shrapnel, 2F + rand.nextInt(2))`) that bypasses the bullet-entity system entirely —
   it is a flat instant-damage roll, not a projectile. **Recommendation: remove
   `TileEntityTurretHoward`/`HowardDamaged` from this report's scope entirely** — they need zero work
   from whichever team implements the legacy bullet system, and their two loose ends (the `CASINGDGK`
   constant, and confirming the CIWS tick once `EntityDamageUtil.attackEntityFromIgnoreIFrame` and
   `WeaponConfig.ciwsHitrate` exist — both already do, see Key design decisions) belong entirely to
   `docs/phase3/turret_system.md`'s existing scope. This correction narrows this task's own framing,
   it does not contradict any existing report.
4. **This report's actual subject materially overlaps `docs/phase4/entities_bosses.md`, which already
   read `EntityBulletBase` in full (684 lines) independently and made an explicit ownership
   recommendation** ("the team implementing these bosses should also implement `EntityBulletBase` in
   full... treat both as a small, shared 'legacy ballistics core' package... reusable by
   `EntityDeathBlast`/`ExplosionChaos`/`EntityCyberCrab`/`ItemGunBase`... do not duplicate the
   ballistics core per-consumer"). That report read `EntityBulletBase` in full and `EntityBullet`/
   `BulletConfigSyncingUtil`/`GunNPCFactory` only partially (150/809, 90/285, 50/344 lines
   respectively), and explicitly deferred `EntityHunterChopper`'s full depth, `EntityCyberCrab`,
   `ItemGunBase`, `TileEntityTrappedBrick`, and `ExplosionChaos` to "whichever report" does the full
   job. **This report is that full-depth pass** — it independently re-confirms `EntityBulletBase`'s
   684-line read (no discrepancy found between the two reports' readings of the same file), completes
   `EntityBullet`/`BulletConfigSyncingUtil`/`GunNPCFactory` to 100%, and additionally covers
   `ItemGunBase`+`ItemGunShotty`+`ItemGunVortex`+the full `guncfg` package (none of which
   `entities_bosses.md` touched) plus `EntityHunterChopper`/`EntityCyberCrab` in full. **Concrete
   reconciliation, not a re-decision**: build `EntityBulletBase`, `EntityBullet`,
   `BulletConfiguration`, and the trimmed ~15-id `BulletConfigSyncingUtil` **once**, as shared
   infrastructure, exactly as `entities_bosses.md` already recommended — consumed by that report's
   bosses (Worm/BOTPrime, MaskMan, UFO), by this report's own mob/vehicle consumers (Hunter Chopper,
   Cyber Crab), and left as a documented forward reference for `EntityDeathBlast`'s and
   `ExplosionChaos`'s narrower slices (see Deferred scope). Whichever implementation wave picks this
   up first should treat both research reports as jointly authoritative for this one shared class
   pair, not pick one and ignore the other.
5. **The real "compatibility shim vs. second system" question has a concrete, evidence-based answer:
   no second ballistics system is needed.** All 8 live `GunNPCFactory` ammo definitions
   (`MASKMAN_ORB/BOLT/BULLET/TRACER/ROCKET/METEOR`, `WORM_BOLT/LASER`, `UFO_ROCKET`) are semantically
   simple projectiles — flat damage range, optional gravity, optional homing/guided `bUpdate`, optional
   incendiary/explosive impact, no armor-piercing math, no headshot multiplier — and every one of
   those fields already has a direct, already-shipped equivalent on Sedna's `BulletConfig`, which is
   *strictly more general* (it already supports per-config `onImpact` lambdas, penetration, ricochet,
   and `DamageType`-keyed damage sourcing that this legacy system entirely lacks). None of legacy
   `BulletConfiguration`'s bespoke hardcoded impact-effect flags
   (`nuke`/`rainbow`/`emp`/`jolt`/`shrapnel`/`chlorine`, all baked directly into
   `EntityBulletBase.onBlockImpact`) are used by any of the 8 live configs — those flags exist only on
   the dead `SHELL_*`/`ZOMG_BOLT`/Gustav entries this report's registry trace already excludes. The one
   real gap is constructor shape: `EntityBulletBase`'s mob-firing constructor
   `(World, int, EntityLivingBase shooter, EntityLivingBase target, float motion, float deviation)`
   computes an aim vector at a target entity directly (skeleton-arrow-style `atan2` trig); Sedna's
   `EntityBulletBaseMK4` (per `docs/phase3/gun_framework.md`'s own table) has a turret-facing
   constructor that takes raw yaw/pitch instead. Adding one narrow, purely-additive
   "aim at a target `LivingEntity`" constructor overload to `EntityBulletBaseMK4` (relocating the same
   ~10 lines of trig `EntityBulletBase` already has) closes that gap without touching any signature
   `TileEntityTurretBaseNT` (Phase 3, already shipping) depends on. See Key design decisions for the
   one real behavioral risk this retarget introduces (penetration semantics), which is a stated
   decision, not a silent side effect.

## Phase-4-safe scope

All class/line counts below are from the CE files actually read in full this session.

| Class | Lines | Live consumers (traced to ground truth) | Portability |
|---|---|---|---|
| `EntityBulletBase` ("MK2", `@AutoRegister(name = "entity_bullet_mk2")`) | 684 | `GunNPCFactory`'s own `bUpdate`/`bImpact` lambdas (self-spawning sub-munitions), `EntityAIMaskmanLasergun`/`Minigun`, `EntityBOTPrimeBase.laserAttack`, `EntityUFO.laserAttack`/`rocketAttack`, `EntityDeathBlast`'s 100-bolt ring burst — plus 2 confirmed-dead consumers (`ItemGunBase.getBulletEntity`, `TileEntityTrappedBrick`'s dart trap) | Single-tick full block+entity raytrace (no per-projectile drag/motion-integration class — everything lives inline in one `onUpdate()`), always resolves to the single *nearest* AABB-intersecting entity per tick (never a same-tick multi-entity fan-out, see point 5 above), 5 **instance** `IBulletXxxBehavior` fields (not shared static lambdas like Sedna's `LAMBDA_STANDARD_*`) set per-`BulletConfiguration` as anonymous classes. Reuses already-Phase-3-shipped explosion/effect infrastructure directly by class reference (`EntityNukeExplosionMK3/MK5`, `EntityCloudFleijaRainbow`, `EntityEMPBlast`, `EntityNukeTorex`, `ExplosionLarge.{jolt,spawnShrapnels}`, `ExplosionChaos.spawnChlorine`, `ExplosionNukeGeneric.empBlast`) — none of this is a forward reference, it all already exists and compiles. Damage is a flat `rand.nextFloat()*(dmgMax-dmgMin)+dmgMin` roll via `ModDamageSource.causeBulletDamage(this, shooter)`, with a bespoke anti-invulnerability-window retry (`if (!victim.attackEntityFrom(...)) { retry with damage + victim.lastDamage; }`, wrapped in a bare `catch(Exception ignored)`) — a genuinely CE-specific hack, not present anywhere in the Sedna system, see Open questions. Ricochet uses two independently-rolled percentages (`HBRC` — unconditional bounce chance; `LBRC` — angle-gated bounce chance) that read as inverted at a glance; verify against CE's exact branch order when porting, not against intuition. |
| `EntityBullet` ("MK1"/oldest, `@AutoRegister(name = "entity_bullet")`) | 809 | `EntityHunterChopper` (the `"chopper"`-tagged constructor), `EntityCyberCrab` (the target-aimed constructor) — plus 1 out-of-scope consumer, `ExplosionChaos`'s `"tauDay"`/`"eyyOk"` fragments (the unowned antimatter/xen `ItemDrop` area, see Deferred scope) | A direct fork of vanilla's `EntityArrow` (still carries obfuscated vanilla field names — `field_145791_d` etc. — verbatim from CE's own decompile), **not** `BulletConfiguration`-driven at all: damage/instakill/rad/fire/antidote/knockback are raw fields the caller sets directly on the instance after construction. Has real vanilla-arrow mechanics `EntityBulletBase` entirely lacks — `inGround`/`arrowShake`/block-embedding/player pickup (`onCollideWithPlayer`) — **but both of this report's own 2 live consumers set `canBePickedUp = 1` only when `shooter instanceof EntityPlayer`, which is never true for `EntityHunterChopper`/`EntityCyberCrab`'s own firing code (`this`, the mob itself, is always the shooter)** — so the entire stick-in-ground/pickup branch is already dead-in-practice for both of this report's consumers, a real, evidence-based simplification opportunity for the port (see Key design decisions). Damage-source selection is a 3-way dispatch on two synced booleans (`Critical`/`Tau`/`Chopper`) → `ModDamageSource.causeBulletDamage`/`causeTauDamage`/`causeDisplacementDamage` respectively — all 3 already have live `ResourceKey<DamageType>` entries in this port's own `ModDamageTypes` (`REVOLVER_BULLET`/`TAU`/`CHOPPER_BULLET`, see Key design decisions). A `"critical"` flag freezes drag/gravity entirely (holds `prevMotion` fixed each tick) — used by `EntityCyberCrab`'s bullets, not by `EntityHunterChopper`'s (chopper explicitly sets `isCritical = false`). |
| `BulletConfiguration` | 239 | All of the above | Plain mutable POJO (~90 fields) + a fluent-ish `getDamage()` builder + `clone()`. **This port already has all 5 of its behavior-interface types ported verbatim** — `com.hbm.interfaces.{IBulletUpdateBehavior,IBulletHurtBehavior,IBulletHitBehavior,IBulletRicochetBehavior,IBulletImpactBehavior}` (read in full; already migrated to Mojang-mapped `net.minecraft.world.entity.Entity`, comments preserved verbatim from CE) — meaning the interface contract this class's fields need already exists and compiles; only the concrete `BulletConfiguration` class and its ~90 fields remain to write. `DamageSource getDamage(...)` cannot be ported as-is (constructs `new EntityDamageSourceIndirect(...)`/`new DamageSource(...)` directly) — see Key design decisions for the 1.21.1 replacement, already established by `docs/phase3/gun_framework.md` for the sibling Sedna system and directly reusable here. |
| `BulletConfigSyncingUtil` | 285 | Every `EntityBulletBase(World, int, ...)` constructor and every `ItemGunBase`/mob-AI call site | The append-only static-`int` id registry, traced end-to-end this session (not just pattern-confirmed): declares ~155 ids, `loadConfigsForSync()` populates exactly 15 of them, and of those 15 only 8 are ever passed to a live constructor call anywhere in CE (`TEST_CONFIG`, `SHELL_*`, `DGK_NORMAL`, `ZOMG_BOLT` are registered-but-never-fired dead weight, confirmed above). **Recommend porting only the 8 live ids**, as either simple named constants or (preferably, matching `docs/phase3/gun_framework.md`'s and `docs/phase4/entities_bosses.md`'s identical recommendation for the structurally-identical Sedna `BulletConfig.configs`/this same registry) a `ResourceLocation`-keyed registry rather than preserving CE's fragile construction-order-dependent `int` scheme — there is no synced-`DataParameter<Integer>` wire-format constraint forcing the old scheme's preservation once this is rebuilt from scratch. |
| `GunNPCFactory` | 344 | `EntityAIMaskmanLasergun` (`ORB`/`MISSILE`/`SPLASH`), `EntityAIMaskmanMinigun`, `EntityBOTPrimeBase.laserAttack`, `EntityUFO.laserAttack`/`rocketAttack`, `EntityDeathBlast` | The one genuinely load-bearing file in the entire `guncfg` package (see Headline finding #2), read here in full for the first time across this port's research (the sibling bosses report read only 3 of its 8 methods). All 8 methods are simple `BulletConfiguration` builders; the 3 with custom behavior (`getMaskmanOrb`'s self-spawning bolt fan, `getMaskmanTracer`'s meteor-on-impact, `getRocketUFOConfig`'s homing-toward-nearest-visible-target `bUpdate`) are the only non-trivial logic in the file — all 3 map directly onto Sedna `BulletConfig`'s existing `onImpact`/`onUpdate`-equivalent lambda slots. |
| `BulletConfigFactory` (3 live methods only) | ~75 of 295 | `GunNPCFactory`'s `standardBulletConfig()`/`standardGrenadeConfig()` calls, `GunRocketFactory.getRocketConfig()`'s `standardRocketConfig()` call | Trivial builder templates (velocity/spread/wear/ricochet defaults). `getLaserSteering()`/`getHomingBehavior()`/`nuclearExplosion()`/`getTestConfig()`/`standardShellConfig()` are all confirmed dead (zero live callers, see Headline finding #2) and do not need porting. |
| `EntityAIMaskmanLasergun` | 113 | MaskMan (per `docs/phase4/entities_bosses.md`) | A 3-mode rotating `EntityAIBase` goal (`ORB`/`MISSILE`/`SPLASH`, cycling after each mode's own repeat count), fully self-contained — no state beyond a timer/attack-mode enum. Generic over `EntityCreature`, not MaskMan-specific in its own type signature. |
| `EntityAIMaskmanMinigun` | 58 | MaskMan | A single fixed-delay `EntityBulletBase`/`MASKMAN_BULLET` stream goal, the simplest file in this report's set. |
| `EntityBOTPrimeBase.laserAttack` (95-line class; only the bullet-firing method is this report's scope) | ~20 of 95 | Worm boss head+body (per `docs/phase4/entities_bosses.md`) | `head=true`: 5 staggered `WORM_LASER` shots at increasing deviation; `head=false`: 1 `WORM_BOLT` shot. The rest of `EntityBOTPrimeBase` (health/AI/movement) is `entities_bosses.md`'s subject, not re-derived here per this task's own instruction. |
| `EntityUFO.laserAttack`/`rocketAttack` (467-line class; only these 2 methods are this report's scope) | ~30 of 467 | UFO boss (per `docs/phase4/entities_bosses.md`) | `laserAttack`: single `WORM_LASER` shot from a randomized-yaw pivot 10 blocks off-target. `rocketAttack`: single `UFO_ROCKET` shot with `homingTarget` NBT seeded on the bullet entity for `GunNPCFactory.getRocketUFOConfig`'s `bUpdate` to pick up next tick. **`EntityUFO` does not extend `EntityUFOBase`** — it directly extends `EntityFlying implements IMob, IRadiationImmune` and reimplements its own waypoint/course-following logic; `EntityUFOBase` (207 lines, read in full to confirm) is a separate abstract class backing only `EntityFBIDrone` (an unrelated FBI-raid mob, `docs/phase4/entities_bosses.md`'s own Deferred scope already names this split correctly) — flagging so a future agent doesn't assume `EntityUFOBase` is dead code or that `EntityUFO` needs it. |
| `EntityDeathBlast` (93-line class, entirely in this report's scope — it is a pure logic/VFX entity, not a boss) | 93 | SatelliteLaser's payload (`docs/phase3/satellites_followup_and_loot_pools.md`'s named forward reference, per this task's own background) | On a 60-tick timer, spawns a nuke explosion (`EntityNukeExplosionMK5.statFacNoRad`, already Phase-3-shipped) plus a 100-bolt ring of `MASKMAN_BOLT`-configured `EntityBulletBase`s fired radially outward at ground-grazing angle (`motionY = -0.01`) — a pure decorative/damage burst, not aimed at any specific target. Trivial to port once `EntityBulletBase` exists; needs nothing else new. |
| `EntityHunterChopper` (442 lines, full) | 442 | Boss-tier flying mob (already independently read in full by `docs/phase4/entities_bosses.md` too — both reports' findings agree: no discrepancy found) | Fires `EntityBullet` via the `"chopper"` isTau-string constructor on a `attackCounter`-gated cadence (every 2 ticks once the counter crosses 120 within a 0–200 rolling window), flat 3–8 damage. **A 90%-damage-reduction rule gates almost the entire class**: `attackEntityFrom` multiplies incoming `amount` by `0.1F` unless the source is `shrapnel`/`nuclearBlast`/`blackhole`/`isExplosion()`/tau/subatomic — i.e. this boss takes full damage only from heavy ordnance and 10% damage from everything else, a load-bearing balance rule easy to lose in a port that treats damage handling generically. Also drops up to 5 `EntityChopperMine` proximity mines (a separate, unread-in-this-survey entity — `docs/phase4/entities_bosses.md` already flags it for whoever implements this mob to read in full; it is a mine, not a bullet, genuinely out of this report's "bullet-firing behavior" mandate). |
| `EntityCyberCrab` (111 lines, full) | 111 | A non-boss hostile mob (`extends EntityMob implements IRangedAttackMob, IRadiationImmune`) — **not** the unrelated `TileEntityCyberCrab` machine block entity (different class, different package, flagging explicitly to prevent confusion from the shared name) | Uses vanilla `EntityAIAttackRanged` (60–80 tick delay, 15-block range) → `attackEntityWithRangedAttack` spawns one target-aimed `EntityBullet`, tagged `Critical=true, Tau=true`, flat `damage=2`. The mob's own `attackEntityFrom` rejects all tau damage (`ModDamageSource.getIsTau(source)` short-circuits to `false`) — crabs are immune to their own weapon type. A sibling class, `EntityTaintCrab` (84 lines, not read in full — referenced only via `instanceof` checks in `EntityCyberCrab` for a different death-explosion size and a disabled panic AI task), extends this class with no bullet-behavior changes; flagged for whoever implements the crab family to read on its own, out of this report's bullet-specific mandate. |
| `ItemGunBase`/`ItemGunShotty`/`ItemGunVortex` | 763 + 215 + 241 | `gun_supershotgun`, `gun_vortex` (both confirmed dead-fire-path, Headline finding #1) | **Recommend porting only the two items' non-bullet-dependent presentation and behavior**: name/model/recipe (Phase 5/1 concerns), `ItemGunShotty`'s meathook grapple state machine (`setHookedEntity`/`hasHookedEntity`/the swing-physics in `updateClient`/`updateServer`, entirely `ItemStack`-NBT + `Entity`-lookup based, no `BulletConfiguration` involved), and `ItemGunVortex`'s crosshair HUD (`renderHud`) and dry idle-hold appearance. **Do not port `ItemGunBase.fire()`/`reload2()`/`tryShoot()`/`useUpAmmo()`/the mag/reload NBT state machine, `handler.guncfg.{Gun12GaugeFactory,GunEnergyFactory}.get{Shotty,Vortex}Config()`, or wire either item to a working `BulletConfigSyncingUtil` entry** — doing so would give these 2 items *more* functional bullet-firing behavior than they have in CE today, which is a scope decision (arguably a bug-fix) this report flags but does not make unilaterally; see Open questions. If a future decision *does* want `gun_vortex`'s railgun-hitscan or `gun_supershotgun`'s pellet spread to actually fire, re-implement that firing behavior on `ItemGunBaseNT`/`Receiver`/`BulletConfig` (Sedna) per `docs/phase3/gun_framework.md`'s own recommendation, not on this legacy base class. |

### Damage-source and interface forward references already resolved

Confirmed by reading this port's own committed code, **not** assumed:

- `com.hbm.damage.ModDamageTypes` (read in full) already declares live `ResourceKey<DamageType>`
  entries for every legacy damage id this report's in-scope classes construct:
  `REVOLVER_BULLET` (`ModDamageSource.s_bullet`/`causeBulletDamage`), `CHOPPER_BULLET`
  (`causeDisplacementDamage`), `TAU` (`causeTauDamage`), `SHRAPNEL` (`ModDamageSource.shrapnel`, the
  Howard CIWS tick — out of this report's own scope per point 3 above, but confirmed available),
  `RADIATION` (`ModDamageSource.radiation`, `ItemGunVortex`'s dead hitscan path). Unlike
  `docs/phase3/gun_framework.md`'s `SEDNA_PLASMA` gap for the *Sedna* system, **this legacy system has
  no missing `ModDamageTypes` entry** for anything its 8 live ammo configs or 2 `EntityBullet`
  consumers actually construct.
- `com.hbm.interfaces.{IBulletUpdateBehavior,IBulletHurtBehavior,IBulletHitBehavior,
  IBulletRicochetBehavior,IBulletImpactBehavior}` (all 5 read in full) are **already ported**,
  Mojang-mapped, and byte-for-byte structurally identical to CE's originals down to the comments —
  someone anticipated this exact package during an earlier foundational pass. Only the concrete
  `EntityBulletBase`/`EntityBullet`/`BulletConfiguration` classes these interfaces parametrize remain
  unwritten.
- `com.hbm.util.EntityDamageUtil.attackEntityFromIgnoreIFrame(Entity, DamageSource, float)` (read in
  full) and `com.hbm.config.WeaponConfig.ciwsHitrate` both already exist with the exact signature/field
  CE's Howard turret calls — relevant to point 3 above (Howard needs nothing new), not to this report's
  own live ammo consumers.
- `com.hbm.util.ContaminationUtil.radiate(Level, double, double, double, double, float, float, float)`
  (the 8-parameter overload, confirmed present) matches `GunNPCFactory.getRocketUFOConfig`'s CE call
  shape (`world, x, y, z, 50, 0, 0, 500`) parameter-for-parameter once `World`→`Level` is substituted.
- `com.hbm.handler.ArmorUtil.{damageSuit(LivingEntity, EquipmentSlot, int), checkForHazmat(LivingEntity)}`
  (confirmed present) already cover `EntityBulletBase.onEntityHurt`'s `config.caustic` 4-slot armor-damage
  loop and `EntityBullet`'s hazmat-immunity check for its radiation-mutation branch — note the port has
  **already modernized** CE's raw `int` armor-slot index (0–3) to a typed `EquipmentSlot`, a real,
  already-made API decision to follow rather than re-derive.
- `com.hbm.interfaces.IRadiationImmune` (confirmed present, already implemented by `EntityUFO`/
  `EntityHunterChopper`/`EntityCyberCrab` in CE) is ready to consume as-is.

## Deferred scope

Real dependencies of *this specific* subsystem that belong to other packages/phases/reports, matching
the "which package, which phase" format the ground rules ask for:

- **`docs/phase4/entities_bosses.md`** — the direct, load-bearing overlap this report's Headline
  finding #4 already reconciles in detail. Do not implement `EntityBulletBase`/`EntityBullet`/
  `BulletConfiguration`/the trimmed `BulletConfigSyncingUtil` twice; build once as shared
  infrastructure per that report's own explicit recommendation, which this report independently
  confirms and completes rather than overrides.
- **`docs/phase3/turret_system.md`** — owns `TileEntityTurretHoward`/`HowardDamaged` in full (Headline
  finding #3). This report found no new dependency for that pair beyond what `EntityDamageUtil`/
  `WeaponConfig` already resolve (see above); no coordination needed beyond that report's own existing
  "not read in full" flag on the `HowardDamaged` subclass.
- **`docs/phase4/hbm_potion_system.md`** — already fully researched `HbmPotion.lead` (confirmed "fully
  portable, no gaps at all" in that report's own effect table), the one potion effect this report's
  live scope actually needs (`EntityBulletBase.onEntityHurt`'s `config.leadChance` branch, live via
  `GunNPCFactory.getMaskmanBullet()`'s `leadChance = 15`). `HbmPotion` itself does not exist in this
  port yet (confirmed by grep — no `com.hbm.potion.HbmPotion.java` file), but it is already fully
  scoped elsewhere; this report does not need to re-derive it, only to consume `HbmPotion.lead` once
  that report's package lands, and can implement `EntityBulletBase` with that one call left as a
  documented forward reference in the meantime.
- **`com.hbm.entity.mob.EntityCreeperNuclear`** — confirmed still absent from this port (grep, no
  file). `EntityBullet`'s `rad` branch turns a hit `EntityCreeper` into one on a successful radiation
  hit; this is the same forward reference `com.hbm.util.ContaminationUtil`'s own TODOs already name
  (per this task's background) for `isRadImmune`. Belongs to whichever Phase 4 area covers
  mutated/nuclear mob content generally — not re-derived here, this report's `EntityBullet` consumers
  (`EntityHunterChopper`, `EntityCyberCrab`) never set `rad = true` themselves, so this branch is dead
  weight for *this report's own* live consumers regardless (only reachable via other, out-of-scope
  `EntityBullet` constructor call sites this survey did not find any of).
- **`com.hbm.main.AdvancementManager`** — confirmed absent (also already named by
  `docs/phase4/entities_bosses.md` and `docs/phase3/satellites_followup_and_loot_pools.md`). Not
  needed by any class in this report's own scope directly (`EntityUFO`'s `AdvancementManager.
  grantAchievement` call is inside `onDeathUpdate`, which belongs to `entities_bosses.md`'s boss-health
  subject, not this report's bullet-firing one) — mentioned only to avoid double-flagging.
- **`com.hbm.explosion.ExplosionChaos`, the antimatter/xen `ItemDrop` half** — confirmed unowned by any
  Phase 1–3 package per this task's own background, and independently confirmed here as a real,
  additional `EntityBullet` consumer (`ExplosionChaos.java:643,646`, the `"tauDay"`/`"eyyOk"` fragment
  spawns). Not researched further here — this report only names the exact call sites so whichever
  future pass researches `ExplosionChaos`/`EntityVortex`/`EntityBlackHole`/`EntityRagingVortex` knows
  it will need `EntityBullet` (built by this report or `entities_bosses.md`) as a prerequisite, not the
  reverse.
- **`com.hbm.entity.projectile.EntityChopperMine`** — Hunter Chopper's dropped proximity mine, not a
  bullet. Already flagged unread by `docs/phase4/entities_bosses.md` for whoever implements Hunter
  Chopper; this report does not re-read it either, for the same reason (out of "bullet-firing behavior"
  scope by the task's own framing).
- **`com.hbm.entity.mob.EntityTaintCrab`** — a small (84-line) `EntityCyberCrab` subclass with no
  bullet-behavior changes of its own (different death-explosion size, disabled panic AI). Flagged for
  whoever implements the crab family generally to read directly; not re-derived here since it changes
  nothing about the bullet-firing behavior this report covers.
- **`com.hbm.entity.mob.{EntityUFOBase, EntityFBIDrone}`** — confirmed a separate, unrelated
  abstract-mob pair (FBI-raid drone waypoint AI) that happens to share the "UFO" name prefix with this
  report's actual `EntityUFO` boss subject. `docs/phase4/entities_bosses.md` already names this split
  correctly in its own Deferred scope; not re-derived here beyond the one-line confirmation in the
  scope table above.

## Key design/API decisions

Confirmed from real code read this session (CE for behavior; this port's own already-committed code
for confirmed real API shape — no NeoForge API is invented, and none of this specific area has a Neo
Edition port to cross-check against, confirmed by directory listing):

- **Recommend retargeting all 8 live `GunNPCFactory` ammo definitions and both of this report's
  `EntityBullet` mob consumers onto Sedna's already-shipped `BulletConfig`/`EntityBulletBaseMK4`
  rather than building a second, parallel ballistics entity/config pair.** Headline finding #5 above
  lays out the evidence (every legacy field these 8 configs actually use has a direct, strictly-more-
  general Sedna equivalent; none of legacy's bespoke hardcoded impact-effect flags are used by any live
  config). Concretely: add one new, purely-additive `EntityBulletBaseMK4` constructor overload that
  takes `(Level, BulletConfig, LivingEntity shooter, LivingEntity target, float motion, float
  deviation)` and computes the same aim-at-target trig `EntityBulletBase`'s equivalent constructor
  already does; extend whatever `DamageClass`-equivalent enum Sedna's `BulletConfig.getDamage` switches
  on with entries that resolve to the already-present `REVOLVER_BULLET`/`TAU`/`CHOPPER_BULLET`
  `ModDamageTypes` keys (no new registry entries needed, only new switch-case mappings). **Do this only
  as new, additive surface** — never modify an existing `EntityBulletBaseMK4`/`BulletConfig`
  constructor or field that `TileEntityTurretBaseNT` (Phase 3, already shipping) or any already-ported
  Sedna gun depends on; confirm no signature collision before landing this, since two mutually
  incompatible changes to the same already-shipped class would be a real regression risk, not a
  hypothetical one.
- **One real behavioral risk this retarget introduces, stated explicitly rather than silently
  absorbed**: legacy `EntityBulletBase`'s `doesPenetrate` flag (set `true` by `standardBulletConfig()`,
  inherited by 5 of the 8 live configs — every `MASKMAN_BOLT/BULLET/TRACER` and `WORM_BOLT/LASER`)
  only controls whether the bullet *survives* hitting an entity (it always detects and reacts to only
  the single *nearest* AABB-intersecting entity per tick, then may keep flying to hit a *different*
  entity on a *later* tick). Sedna's `doesPenetrate` (per `docs/phase3/gun_framework.md`'s own reading)
  is a same-tick fan-out that calls `onImpact` once per every AABB-intersecting entity in one pass.
  Naively mapping legacy's `doesPenetrate = true` onto Sedna's flag of the same name would change these
  5 live ammo types from "passes through a target and can hit a second one further down the line on a
  later tick" to "instantly hits every entity currently in its swept box." For rapid mob-fired bolts
  this is very likely an invisible change in practice, but it is a real semantic difference between the
  two systems' *same-named* flag, and should be a stated implementation decision (map legacy
  `doesPenetrate` to Sedna's non-penetrating nearest-hit path instead, or accept the behavior change
  explicitly) rather than an assumption that reusing the flag name is free.
- **`EntityBullet`'s vanilla-arrow-derived "stuck in ground, player-pickup" mechanic can be dropped
  entirely for this report's own 2 live consumers with zero observable behavior change** — both
  `EntityHunterChopper` and `EntityCyberCrab` fire their own bullets with `shootingEntity = this` (the
  mob itself), and `canBePickedUp` is only ever set to `1` when the shooter `instanceof EntityPlayer`,
  which never happens on either call site. This is a genuine simplification opportunity confirmed by
  reading both consumers' exact constructor call sites, not merely assumed low-risk.
- **The `EntityUFO`/`EntityHunterChopper`'s 90%-damage-reduction/heavy-ordnance-bypass rule
  (`EntityHunterChopper.attackEntityFrom`) and the `EntityCyberCrab`/tau-immunity rule
  (`EntityCyberCrab.attackEntityFrom`) are ordinary `LivingEntity#hurt` overrides**, no different in
  shape from any other CE mob's custom damage-resistance override — no new NeoForge API surface needed,
  just an override on whatever this port's mob base class already provides for that hook.
- **`EntityBulletBase`/`EntityBullet` should extend vanilla `Projectile`** (1.21.1's real base class),
  following exactly the same already-confirmed decision `docs/phase3/gun_framework.md` made for the
  sibling `EntityThrowableNT`/`EntityBulletBaseMK4` pair — dropping CE's 1.12-era
  `thrower`/`throwerName`-by-string-lookup workaround in favor of vanilla `Projectile`'s real
  `setOwner`/`getOwner` UUID-based ownership, for the same reasons that report already gives in full
  (not re-derived here). Confirm this report's classes and Package A of `gun_framework.md` do not
  independently invent two different "extends `Projectile`" base shapes for what should be one
  shared pattern.
- **Mob/boss-fired bullets need no `ItemGunBaseNT`/`GunConfig`/`Receiver`/mag state at all** — every
  consumer in this report's scope (`EntityAIMaskmanLasergun`/`Minigun`, `EntityBOTPrimeBase`,
  `EntityUFO`, `EntityDeathBlast`, `EntityHunterChopper`, `EntityCyberCrab`) spawns a projectile
  entity directly from an `EntityAIBase` goal or an entity's own attack method, with no held
  `ItemStack` and no player involved — confirming the same "ballistics core has no dependency on the
  held-weapon state machine" shape `docs/phase3/gun_framework.md`'s Headline finding #2 already
  established for the Sedna system's Package A/B split. This report's whole scope is Package-A-shaped,
  never Package B.

## Open questions / risks

- **Whether to fix or faithfully preserve `gun_supershotgun`/`gun_vortex`'s dead-fire-path bug is a
  real product decision this report flags but does not make.** Headline finding #1 traces the exact
  mechanism by which both items' bullet-firing is unreachable in CE HEAD; this report recommends
  porting only their non-bullet presentation (meathook, HUD) as the behavior-preserving default, but a
  reviewer could reasonably decide the "intended" behavior (a working shotgun / working railgun) is
  worth restoring on the Sedna base per `docs/phase3/gun_framework.md`'s own suggestion — that is a
  scope call for whoever plans the implementation wave, not resolved here.
- **The retarget-onto-Sedna recommendation (Headline finding #5 / Key design decisions) is a
  recommendation, not a settled decision** — it changes the work-package shape materially (no second
  `EntityBulletBase`/`EntityBullet`/`BulletConfiguration` class family to write and maintain at all,
  only two small additive changes to the already-shipped Sedna classes) versus a faithful port (writing
  and maintaining ~1,700 new lines across `EntityBulletBase`+`EntityBullet`+`BulletConfiguration`+
  `BulletConfigSyncingUtil` as a second, permanent parallel system). Given `docs/phase4/
  entities_bosses.md` already recommended (independently, before this report existed) building
  `EntityBulletBase` as faithful new infrastructure, **this is a real disagreement between two research
  passes that needs an explicit resolution before implementation starts**, not a difference to paper
  over. This report's own position, on the fuller evidence gathered here (the complete
  `BulletConfigSyncingUtil` trace, the full `EntityBullet`/`GunNPCFactory` reads `entities_bosses.md`
  did not have), is that retargeting is the lower-maintenance, equally-faithful option — but the two
  reports should be reconciled explicitly by whoever plans the implementation wave, not silently
  decided by whichever gets implemented first.
- **The anti-invulnerability-window retry hack in `EntityBulletBase.onUpdate`**
  (`if (!victim.attackEntityFrom(...)) { retry with damage + victim.lastDamage; }`, wrapped in a bare
  `catch(Exception ignored)`) is unusual enough to flag explicitly — it is CE's own attempt to punch
  through a target's brief post-hit invulnerability window by re-attacking with a boosted damage value
  derived from the target's last-recorded damage, silently swallowing any exception the retry itself
  throws. Preserve-vs-fix is a real fork here (the swallowed exception in particular reads as
  defensive-but-fragile 1.12-era code, not a deliberate design), not resolved in this report.
- **`EntityBulletBaseMK4`'s constructor surface is a shared resource across three research reports now**
  (`docs/phase3/gun_framework.md`'s Package A, `docs/phase3/turret_system.md`'s turret-facing
  constructor, and this report's proposed target-aim overload) — worth an explicit compatibility check
  across all three once any one of them is implemented, to confirm the eventual constructor set doesn't
  collide or duplicate.
- **`EntityTaintCrab`/`EntityChopperMine` were not read in this survey**, consistent with this report's
  explicit "bullet-firing behavior" mandate (neither is a bullet) — flagged so whoever implements the
  crab family or Hunter Chopper's mine-drop behavior reads them fresh rather than assuming either is a
  trivial clone of something already covered here.
