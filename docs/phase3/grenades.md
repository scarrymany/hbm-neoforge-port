# CE grenade items & entities — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/grenade/*.java` (8 files, 956 lines —
  `EntityGrenadeBase`, `EntityGrenadeBouncyBase`, `EntityGrenadeBouncyGeneric`,
  `EntityGrenadeImpactGeneric`, `EntityGrenadeUniversal`, `EntityDisperserCanister`,
  `EntityWastePearl`, `IGenericGrenade`)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/grenade/*.java` (5 files, 830 lines —
  `ItemGrenadeShell`, `ItemGrenadeFilling`, `ItemGrenadeFuze`, `ItemGrenadeExtra`,
  `ItemGrenadeUniversal`)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/{ItemGenericGrenade,ItemGrenadeDynamite,
  ItemGrenadeFishing,ItemDisperser,GrenadeDispenserRegistry}.java` (5 files, 266 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/api/entity/EntityGrenadeFactory.java` (9 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/effect/{EntityMist,EntityFireLingering}.java`
  (480 lines — the actual payload entities the disperser and the INC/WP fillings spawn; not
  themselves "grenades" but load-bearing for what a grenade does on detonation)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityThrowableInterp.java` (84
  lines — `EntityGrenadeUniversal`'s direct base; its own base `EntityThrowableNT` is *not*
  re-read here, see below)
- `upstream/hbm-ce/src/main/java/com/hbm/crafting/handlers/GrenadeCraftingHandler.java` (87
  lines) and the grenade section of `upstream/hbm-ce/src/main/java/com/hbm/crafting/
  WeaponRecipes.java` (grepped, lines 259–297)
- `upstream/hbm-ce/src/main/java/com/hbm/items/{ItemEnumMulti,IAnimatedItem,
  IEquipReceiver}.java` (201 lines — the shared bases `ItemGrenadeShell`/`Filling`/`Fuze`/
  `Extra` and `ItemGrenadeUniversal` build on)
- `upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java` (grepped) — confirms every
  concrete registered grenade-family item and its constructor arguments
- This port's own `src/main/java/com/hbm/{capability/HbmLivingAttachment.java,
  entity/ConveyorEntityTypes.java, packet/HbmNetwork.java}` and repo-wide greps for
  `CustomRecipe`/`SimpleCraftingRecipeSerializer`/`RecipeSerializer` (zero hits, including in
  `upstream/neo-edition`)
- `docs/phase3/{explosion_engine.md, gun_framework.md, weapon_animation_hooks.md,
  bomb_blocks_and_detonators.md, missile_launch_infra.md}` — all five are load-bearing
  prerequisites for this report (the vanillant explosion framework, `EntityThrowableNT`/
  `BulletConfig`/`EntityBulletBaseMK4`, the animation-trigger payload pattern, and the nuke/
  satellite entity families are each already fully researched there and are treated as
  authoritative, not re-derived, below) and `docs/phase1/{items_special.md, items_tool.md,
  items_food_gear.md}` per the task's own instruction to check what those reports already
  flagged as Phase 3 grenade scope

## Headline finding

Three independent things in CE answer to "grenade," and they should **not** be designed as one
class hierarchy:

1. **The modern modular "Universal Grenade"** (`com.hbm.items.weapon.grenade.*`, 5 items + 1
   entity) — a shell/filling/fuze/(optional extra) component system. Four `ItemEnumMulti`
   metadata items (`grenade_shell`, `grenade_filling`, `grenade_fuze`, `grenade_extra`) are
   combined by a dynamic crafting recipe into one `grenade_universal` stack carrying 3–4 NBT
   ints; one entity (`EntityGrenadeUniversal`) reads those ints back off its carried `ItemStack`
   and dispatches to whichever `Consumer`/`BiConsumer` lambda each enum constant carries. This is
   the actively-developed system — 13 fillings, 5 fuzes, 4 extras, 4 shells — and everything
   PORT_SPEC likely means by "grenade items."
2. **Two legacy single-purpose grenades** (`ItemGrenadeDynamite`/`stick_dynamite`,
   `ItemGrenadeFishing`/`stick_dynamite_fishing`) predating the modular system, sharing their own
   hand-rolled bounce-physics entity family (`EntityGrenadeBouncyBase`/`Generic`) that
   reimplements `Entity#move` with velocity-inversion bounce instead of subclassing
   `EntityThrowable`. These call vanilla's own `world.newExplosion(...)` directly — **no CE
   explosion framework involvement at all**, genuinely the "simpler bespoke area-effect" case the
   task asked to distinguish.
3. **A fluid-payload canister family** (`ItemDisperser`: `disperser_canister`, `glyphid_gland`)
   that looks like a grenade (thrown, arcs, uses the legacy `EntityGrenadeBase` throw-launch math)
   but never explodes at all — it spawns `EntityMist`, a pure area-effect-cloud entity with ~10
   `FluidTrait`-driven per-entity effect branches (boil, freeze, corrosive, poison, radiation
   vent, pheromone buff, ...) and zero block destruction. This is the *other* "simpler bespoke
   area effect" case.

**Detonation is not a separate, simpler system from Phase 3's explosion engine for the cases that
matter most.** Every one of the 13 `EnumGrenadeFilling` explode lambdas that isn't a legacy item
calls directly into `ExplosionVNT` (`com.hbm.explosion.vanillant`) — exactly the strategy-object
framework `docs/phase3/explosion_engine.md` already fully documented as CE's shared "normal
explosion" primitive with 65 consumers; grenade fillings are a chunk of those 65, not a
parallel system. Three fillings additionally reach into other already-scoped Phase 3 packages:
`CLUSTER`/`CLUSTER_HEAVY`/`LASER` spawn `EntityBulletBaseMK4`/`EntityBulletBeamBase` submunitions
(`docs/phase3/gun_framework.md`'s Package A), and `SCHRAB` spawns `EntityNukeExplosionMK3` +
`EntityCloudFleija` (`docs/phase3/bomb_blocks_and_detonators.md`'s territory). Only the two
legacy items and the disperser are genuinely self-contained.

The registered-item footprint is small: **9 registered `Item`s** (`grenade_shell`,
`grenade_filling`, `grenade_fuze`, `grenade_extra`, `grenade_universal`, `stick_dynamite`,
`stick_dynamite_fishing`, `disperser_canister`, `glyphid_gland` — plus two trivial container
items, `disperser_canister_empty`/`glyphid_gland_empty`, which are ordinary `ItemBase`s with no
grenade logic of their own) across **9 concrete/abstract classes**, and **9 entity-family
classes** (`EntityGrenadeBase`, `EntityGrenadeBouncyBase`, `EntityGrenadeBouncyGeneric`,
`EntityGrenadeImpactGeneric`, `EntityGrenadeUniversal`, `EntityDisperserCanister`,
`EntityWastePearl`, plus the `IGenericGrenade` marker interface and the unused
`EntityGrenadeFactory` API surface) plus 2 payload effect entities the fillings/disperser spawn
(`EntityMist`, `EntityFireLingering`).

## Phase-3-safe scope

### 1. The Universal Grenade component items (5 classes, all `ItemEnumMulti<E>`/`ItemBase`)

| Class | Role |
|---|---|
| `ItemGrenadeShell` (57 lines) | 4-value enum (`FRAG`, `STICK`, `TECH`, `NUKE`), each carrying a stack limit (4/4/2/1), draw duration (30–43 ticks), bounce modifier, and throw force ("yeet force"). Pure data card, no world access. |
| `ItemGrenadeFilling` (339 lines) | 13-value enum, each carrying a `Consumer<EntityGrenadeUniversal> explode` lambda plus body/label colors and a `Set<EnumGrenadeShell> compatibleShells` (which shells this filling may be crafted into — enforced by `GrenadeCraftingHandler`, not the item itself). This is where all the actual detonation logic lives (see table below). |
| `ItemGrenadeFuze` (58 lines) | 5-value enum, each carrying either an `updateTick` `Consumer` (the 3 timed fuzes) or an `onImpact` `BiConsumer<..., RayTraceResult>` (impact/airburst), plus a color used only for the item's tooltip/texture band. |
| `ItemGrenadeExtra` (88 lines) | 4-value enum (`GLUE`, `PROXY_FUZE`, `FRAG_SLEEVE`, `TRIPLEX`), each optionally carrying `updateTick`/`onImpact`/`onExplode` hooks — this is the only component that's optional (a grenade may have zero extras). |
| `ItemGrenadeUniversal` (289 lines) | The crafted, thrown item. `setHasSubtypes(false)` — one registry item, all variance in NBT (`KEY_SHELL`/`KEY_FILLING`/`KEY_FUZE`/`KEY_EXTRA`, 3–4 ints). `getItemStackLimit` defers to the shell's stack limit. `onItemRightClick` gates on `HbmLivingProps.getData(player).getGrenadeDeployment() >= shell.getDrawDuration()` (an equip-hold-and-release charge timer, not a cooldown) before spawning `EntityGrenadeUniversal` and shrinking the stack. `onUpdate` drives that same deployment counter up by 1 each tick the item is actually held (tracked per-hand via `ItemGunBaseNT.getIsEquipped`/a local "am I the deployment owner" check so off-hand doesn't double-count), and fires shell-specific cue sounds at specific tick thresholds (revolver-cock click for FRAG at tick 18, bolt-open clacks for STICK at 16/25, etc. — cosmetic, not gameplay-affecting). `getSubItems` (client, creative tab) enumerates every valid shell×filling×fuze×(no-extra/extra) combination via `filling.compatibleShells.contains(shell)` — the same compatibility check `GrenadeCraftingHandler` uses. |

### 2. `EntityGrenadeUniversal` (199 lines) — the modern grenade's flight entity

Extends `EntityThrowableInterp` (client-position-interpolating; itself extends
`EntityThrowableNT`, **already fully documented in `docs/phase3/gun_framework.md`**'s Package A
table — the same base bullets use, complete with `getStuck`/penetration/motion-integration
semantics). Carries one synced `DataParameter<ItemStack>` (the grenade being thrown, so the
entity is self-describing without a second lookup table) plus `bounces`/`trail` ints.

- **Launch**: constructor computes a hand-offset spawn position (±0.25 lateral, rotated by
  `-thrower.rotationYaw + 180`) and calls `shoot(lookVec, shell.getYeetForce(), 0)` — `STICK`/
  `NUKE` shells (`1.5F` yeet force) throw noticeably farther than `FRAG`/`TECH` (`1.0F`).
- **Flight**: no override of the inherited ballistic integration — gravity/drag come from
  `EntityThrowableNT`'s already-documented per-tick `motion.y -= gravity` scheme, not a custom
  arc.
- **Per-tick hooks**: `onUpdate` calls `fuze.updateTick` and `extra.updateTick` unconditionally
  every tick (both may be `null`, both checked before invoking) — this is the *entire* dispatch
  mechanism for timed fuzes and the proxy-fuze extra. `getTimer()` (`ticksInAir + ticksInGround`,
  both vanilla `EntityThrowable` fields) is what every timed fuze/proxy-fuze compares against.
- **Bounce on block impact** (`onImpact`, `RayTraceResult.Type.BLOCK` branch): nudges position
  0.05 blocks off the hit face, plays `HBMSoundHandler.grenadeBounce` if incoming speed > 0.2,
  inverts and scales the motion component perpendicular to the hit face by
  `-shell.getBounce()` (the other two axes get flat `0.8` damping), sends a
  `TrackerUtil.sendTeleport` resync (a real 1.12-client-authoritative-physics workaround —
  confirm this port's equivalent networking need still exists in 1.21.1's server-authoritative
  movement, see Open questions), and increments `bounces`. **Fuze/extra `onImpact` hooks run
  first** (`fuze.onImpact`/`extra.onImpact`, both `BiConsumer<EntityGrenadeUniversal,
  RayTraceResult>`), and if either already killed the grenade (`isDead`) the bounce logic is
  skipped entirely — the same "custom hook can short-circuit the standard path" ordering
  `gun_framework.md` documented for `EntityBulletBaseMK4.onImpact`.
- **`explode()`**: `setDead()`, then `filling.explode.accept(this)`, then
  `extra.onExplode.accept(this)` (if present) — filling always runs before the extra's own
  bonus explosion effect (matters for `TRIPLEX`, which needs the parent's fragmentation to have
  already happened before it spawns 3 child grenades).
- **Client-only**: `spin`/`prevSpin` (visual roll rate — bounces spin faster than free flight)
  and a `TRAIL_TRIPLET` flag (spawns a flame particle trail each tick, used only by `TRIPLEX`'s
  sub-grenades) are pure render state — Phase 5, noted here only because they live in the same
  `onUpdate()` method as the server logic.

### 3. The 5 fuzes (`EnumGrenadeFuze`, all pure `EntityGrenadeUniversal`-in lambdas)

| Fuze | Trigger |
|---|---|
| `S3`/`S7`/`S15` | `updateTick`: explode once `getTimer() >= 60/140/300` (3s/7s/15s at 20 ticks/s) |
| `IMPACT` | `onImpact`: only if `getTimer() >= 10` (0.5s arm delay after throw — a live grenade can't detonate on its own thrower's hand or an immediate wall 3 blocks away), snaps position to the exact hit point, explodes |
| `AIRBURST` | `updateTick`: at `getTimer() >= 30` (1.5s), raytraces straight down 10 blocks; if that ray hits a block, explodes in midair above it (the "safety" isn't a timer floor, it's "don't check until 1.5s in, then explode the instant a floor is under you within 10 blocks") |

### 4. The 13 fillings (`EnumGrenadeFilling`) — detonation effect table

All non-legacy detonation goes through `ExplosionVNT` via three small static helpers in
`ItemGrenadeFilling` itself: `standardExplode(range, damage[, dt, dr])` (allocator=standard,
entity processor=`EntityProcessorCrossSmooth(1, damage)`, player processor=standard, SFX=
`ExplosionEffectWeapon(10, 2.5F, 1F)`), `tinyExplode(...)` (same shape but `SFX=
ExplosionEffectTiny()`, knockback capped at 0.25), and `explodeStandardEnergy(damage, range,
damageClass, r,g,b, scale)` (no block allocator/processor at all — pure entity damage +
`PlasmaBlast` particle broadcast, i.e. genuinely non-destructive "energy" weapons). Every symbol
below (`ExplosionVNT`, `BlockAllocatorStandard`, `EntityProcessorCrossSmooth`,
`PlayerProcessorStandard`, `ExplosionEffectWeapon/Tiny`, `BlockMutatorFire`) is already documented
in `docs/phase3/explosion_engine.md`'s vanillant-framework section — not re-derived here.

| Filling | Shells | Effect |
|---|---|---|
| `POWDER` | FRAG/STICK | `standardExplode(5, 10)` — weakest HE tier |
| `HE` | FRAG/STICK | `standardExplode(7.5, 25, dt=0.1)` — the "default" grenade |
| `DEMO` | FRAG/STICK | Hand-assembled `ExplosionVNT` (not the `standardExplode` helper): `BlockAllocatorStandard` + `BlockProcessorStandard` + `EntityProcessorCrossSmooth(1, 10)` + `PlayerProcessorStandard` + `ExplosionEffectWeapon` at range 5 — same shape as `standardExplode` but spelled out, likely because CE evolved it before the helper existed; port as the equivalent `standardExplode`-style call unless byte-for-byte lambda identity matters |
| `INC` | FRAG/STICK | `standardExplode(3, 10)` + spawns `EntityFireLingering` (6×2 area, 200-tick duration, `TYPE_DIESEL`) + `igniteAround` (sets vanilla fire on any air block adjacent to a flammable block within a 2-block cube) |
| `WP` | FRAG/STICK | `standardExplode(3, 10)` + `EntityFireLingering` (`TYPE_PHOSPHORUS`, 600 ticks — 3× INC's burn duration) + `igniteAround` radius 3 + 3× `Haze` particle-cloud broadcast packets |
| `CLUSTER` | FRAG/STICK | `standardExplode(7.5, 15, dt=0.1)` + spawns 30 (×1.25 if shell==FRAG) `EntityBulletBaseMK4` "pellet" submunitions with random spread, `onImpact = LAMBDA_TINY_EXPLODE` (each pellet does its own small explosion on landing) — **depends on `gun_framework.md`'s Package A** |
| `CLUSTER_HEAVY` | NUKE | Same shape, 15 heavier pellets, `onImpact = LAMBDA_EXPLODE` (bigger per-pellet blast) — **same Package A dependency** |
| `EMP` | TECH | `explodeStandardEnergy(30 dmg, 3 range, DamageClass.ELECTRIC, cyan tint)` — no block destruction, pure entity/electronics damage + VFX |
| `PLASMA` | TECH | `explodeStandardEnergy(50, 5, DamageClass.PLASMA, green tint)` — CE's own code comment: `// TODO: unique effect because this sucks` (i.e. CE itself considers this filling's VFX a placeholder — do not over-invest polishing it beyond parity) |
| `LASER` | TECH | `tinyExplode(2, 5)` (small blast) + finds up to all `EntityLivingBase` within 15 blocks and fires one `EntityBulletBeamBase` hitscan sub-beam at each (shuffled order, thrower excluded) — **depends on `gun_framework.md`'s `EntityBulletBeamBase`** |
| `CLUSTER_HEAVY`/`NUCLEAR`/`NUCLEAR_DEMO`/`SCHRAB` | NUKE only | see below |
| `NUCLEAR` | NUKE | `ExplosionVNT` at range 10 with only `EntityProcessorCrossSmooth(2, 100).withRangeMod(1.5)` + player processor set — **no block allocator/processor at all**, i.e. this is purely an AoE-damage nuke-lite with no crater (CE's actual crater-digging nukes are the `EntityNukeExplosionMK3`/`MK5` family from `explosion_engine.md`, not this) — plus `incrementRad` (radiation) and `spawnMush` (mushroom-cloud VFX broadcast + `SatelliteDetector` ping) |
| `NUCLEAR_DEMO` | NUKE | Same but adds `BlockAllocatorStandard(64)` + `BlockProcessorStandard().withBlockEffect(new BlockMutatorFire())` — i.e. this variant *does* dig a crater and light it on fire, `NUCLEAR` alone does not |
| `SCHRAB` | NUKE | Does not use `ExplosionVNT` at all — calls `EntityNukeExplosionMK3.statFacFleija(...)` and spawns an `EntityCloudFleija` — **entirely `bomb_blocks_and_detonators.md`/`explosion_engine.md`'s territory**, this filling is just a spawn-site |

### 5. The 4 extras (`EnumGrenadeExtra`)

| Extra | Hook | Effect |
|---|---|---|
| `GLUE` | `onImpact` | Snaps position to the hit point, calls `grenade.getStuck(pos, side)` — the exact method `EntityThrowableNT` (`gun_framework.md`'s base) already exposes for bullets embedding in blocks; no new "stuck" mechanic to design |
| `PROXY_FUZE` | `updateTick` | Every 3rd tick starting at tick 10, scans a 20×20×20 AABB for any `EntityLivingBase` (excluding the thrower) within 10 blocks and explodes immediately if found — a proximity mine behavior layered on top of whatever fuze is also installed |
| `FRAG_SLEEVE` | `onExplode` | Calls `standardFragmentation(grenade, 25)` — spawns 25 (×1.5 if shell==FRAG) pure-kinetic `EntityBulletBaseMK4` "fragmentation" pellets (`BulletConfig.fragmentation`: 3-tick life, ricochet ×2 @ 90°) radiating outward — **Package A dependency again** |
| `TRIPLEX` | `onExplode` | Builds a fresh `ItemStack` via `ItemGrenadeUniversal.make(shell, filling, S3_fuze)` (always re-fuzes the children to a 3s timer regardless of the parent's own fuze) and spawns 3 `EntityGrenadeUniversal` children 120° apart with `TRAIL_TRIPLET` set, each inheriting the parent's shell+filling — "the big one," CE's own in-code label |

### 6. Legacy grenades (`ItemGenericGrenade` family, 3 item classes + 4 entity classes)

- `ItemGenericGrenade` (62 lines, abstract) — `fuse` field in seconds (`* 20` for ticks),
  `maxStackSize = 16`, `explode(Entity, EntityLivingBase, World, x,y,z)` is the per-subclass
  override point. `onItemRightClick` always shrinks the stack and plays a bow-shoot-style sound,
  then spawns either `EntityGrenadeImpactGeneric` (if `fuse == -1`) or `EntityGrenadeBouncyGeneric`
  (otherwise).
- `ItemGrenadeDynamite`/`stick_dynamite` (17 lines) — `fuse=3`. `explode` is one line:
  `world.newExplosion(grenade, x, y+0.25, z, 3F, false, false)` — **vanilla explosion API
  directly, zero CE framework**.
- `ItemGrenadeFishing`/`stick_dynamite_fishing` (76 lines) — also `fuse=3` (constructed with `3`
  in `ModItems`, though it separately overrides `getMaxTimer()`→`60` ticks, a harmless redundant
  duplicate of the same 3s value). `explode` does the same `world.newExplosion(null, ..., 3F,
  false, false)` (note: `null` exploder here, `grenade` for dynamite — a real CE asymmetry, not a
  bug to "fix"), then scatters up to 15 loot items into any water blocks within a 15×15×15 volume
  around the blast, rolled from vanilla's `LootTableList.GAMEPLAY_FISHING`/`LootContext.Builder`
  and floated via `EntityItemBuoyant` (a buoyant item-entity outside this survey's scope). **1.21.1
  loot table API shape not resolved here** (see Open questions).
- `EntityGrenadeBase` (106 lines, abstract `EntityThrowable`) — shared hand-offset throw-launch
  math (near-identical to `EntityGrenadeBouncyBase`'s and `EntityGrenadeUniversal`'s own launch
  code — three independent copies of the same "offset from eye height by hand, aim along look
  vector, shoot at fixed force" logic exist in this survey; not unified in CE, and this report does
  not recommend unifying them in the port either beyond what's naturally shared by a common
  superclass, since the three call sites diverge in exactly which force/offset constants they use).
  `onImpact` calls `attackEntityFrom(DamageSource.causeThrownDamage(...), 0)` if an entity was hit
  (0 damage — vanilla-shaped source used purely for hit-detection bookkeeping, not real damage),
  then always calls the abstract `explode()`. Used directly by `EntityDisperserCanister` and
  `EntityWastePearl` (not just as a legacy-grenade-only base).
- `EntityGrenadeBouncyBase` (365 lines, abstract `Entity implements IProjectile`) — **does not
  extend `EntityThrowable` at all**; reimplements `Entity#move` wholesale (`moveBounce`) with a
  full copy of vanilla's AABB-sweep collision code plus an inserted "if collided on any axis,
  invert that axis's motion component and scale all three by `bounceMod * 1.5`" bounce step. This
  is the one piece of real, non-trivial physics-porting work in the legacy family — it is not a
  simple override, it is a fork of vanilla's own movement algorithm, and needs to be re-derived
  against 1.21.1's `Entity#move`/`Entity#collide` (which has changed shape since 1.12 — see Open
  questions) rather than transliterated line-by-line.
- `EntityGrenadeBouncyGeneric` (71 lines) / `EntityGrenadeImpactGeneric` (62 lines) — thin
  `IGenericGrenade` wrappers storing the originating `Item`'s numeric id in a synced
  `DataParameter<Integer>` (1.12's mutable-integer-id item registry — **must become an item
  `ResourceLocation`/`Holder<Item>` reference in the port**, not a raw int) so `explode()` can look
  the item back up and call its `explode(...)` override. **`EntityGrenadeImpactGeneric`'s code
  path is currently unreachable**: no `ModItems` grenade is constructed with `fuse == -1` (both
  `stick_dynamite` and `stick_dynamite_fishing` use `fuse=3`, routing through
  `EntityGrenadeBouncyGeneric` instead) — confirmed by grepping every `ItemGenericGrenade`
  subclass's constructor call in `ModItems.java`. Keep the class for parity/forward-compatibility
  if the port's registry-id-preservation principle demands it; it has zero observable behavior
  today.

### 7. Disperser/canister family (2 items, 2 entities, 1 payload entity)

- `ItemDisperser` (67 lines, extends `ItemFluidTank`) — two registered instances,
  `disperser_canister` (2000 mb capacity) and `glyphid_gland` (4000 mb), differing only in
  capacity/creative-tab filtering (`glyphid_gland` only offers `PHEROMONE`/`SULFURIC_ACID`;
  `disperser_canister` offers every `FluidType` flagged `isDispersable()`). `onItemRightClick`
  spawns `EntityDisperserCanister` carrying the item's registry id and the stack's fluid-metadata
  damage value, shrinks the stack, plays a snowball-throw sound.
- `EntityDisperserCanister` (93 lines, extends legacy `EntityGrenadeBase`) — reuses the shared
  throw-launch physics; `explode()` spawns one `EntityMist` (10×5 area, 80-tick duration, typed by
  the carried fluid) and self-destructs. **No explosion, no block interaction at all.**
- `EntityMist` (368 lines, read in full) — the actual area-effect payload, worth summarizing since
  it's this family's real "detonation" logic even though it isn't itself thrown: per-tick,
  server-side, iterates every non-spectator/non-creative entity in its (negative-width, i.e.
  inverted/grown) bounding box and applies whichever of ~10 independent `FluidTrait`-keyed effects
  the fluid type carries (boiling damage + afterburn above 100°/500°, freezing damage + slowness/
  mining-fatigue below -20°, healing for `DELICIOUS`-tagged fluids, oil-dousing for flammable
  liquids, extinguishing for cold non-flammables, corrosive armor damage, radiation contamination
  via `ContaminationUtil`, poison/wither potion effects, `FT_Toxin`'s own `affect()` callout,
  Enderjuice teleport-victim, and a glyphid/player pheromone buff pair) — client side just spawns
  `Tower` particle-effect broadcasts. If the fluid is both flammable and currently on fire, it
  instead triggers one vanilla `world.createExplosion` scaled by remaining lifetime and
  self-destructs — the *only* explosion-adjacent code path in this entire family, and it's vanilla's
  own API, not CE's.

### 8. Orphaned/dead surface (confirmed by grep — verify before porting, don't assume live)

- **`EntityGrenadeFactory`** (`com.hbm.api.entity`, 9-line `@FunctionalInterface`) — zero
  implementations or call sites found anywhere in `hbm-ce` (grepped the whole tree). Unused public
  API surface; recommend skipping unless a future content pass finds a real consumer.
- **`GrenadeDispenserRegistry.registerDispenserBehaviors()`** — empty method body, does nothing.
  The class's only functional method, `registerDispenserBehaviorFertilizer`, is unrelated to
  grenades (fertilizer dispensing) despite the class's name. Nothing to port here.
- **`EntityWastePearl`** (52 lines, extends legacy `EntityGrenadeBase`) — real, working throw
  physics and a real `explode()` (scatters `ModBlocks.fallout`/`gas_radon`/`gas_radon_dense`
  blocks through a 7×7×7 volume around impact), but **zero items in `ModItems` construct or throw
  it** (grepped the whole tree for `EntityWastePearl` — the only match is its own file). It is
  reachable only via `/summon` or a future content addition. It also depends on `ModBlocks.fallout`
  and the radon gas blocks, which are Phase 4 (radiation/world-sim) content per
  `explosion_engine.md`'s own already-established fallout-system deferral. Recommend deferring
  entirely rather than porting an orphan into a package that doesn't otherwise need Phase 4 blocks.

### 9. Crafting (data-driven, low risk, one exception)

The 5 component items plus `stick_dynamite`/`stick_dynamite_fishing`/`disperser_canister_empty`
all have ordinary shaped/shapeless recipes in `WeaponRecipes.java` — standard JSON recipe-provider
work, no special code, exactly like every other Phase 1/2 item's recipes. The one exception:
**`GrenadeCraftingHandler`** (87 lines) is a dynamic `IRecipe` (not a static shape) that matches
"the 3×3 grid contains only grenade-component items, exactly one shell, one compatible filling,
one fuze, and at most one extra" and produces the combined `grenade_universal` stack via
`ItemGrenadeUniversal.make(...)`. This needs a real custom-match crafting-table recipe in 1.21.1
terms (see Key design decisions).

### Already available — no new work needed

- **`HbmLivingAttachment.getGrenadeDeployment()`/`setGrenadeDeployment(int)`** — confirmed present
  in this port's own already-committed `src/main/java/com/hbm/capability/HbmLivingAttachment.java`
  (with a doc comment explicitly noting it's deliberately *not* persisted to NBT, matching CE's
  `EntityHbmProps`, since it's pure runtime UI-hold state). `ItemGrenadeUniversal`'s equip-charge
  timer needs nothing new here — call the existing getter/setter.
- **`EntityThrowableNT`/`EntityThrowableInterp`** (the base `EntityGrenadeUniversal` builds on) —
  already fully documented in `docs/phase3/gun_framework.md`'s Package A table (motion
  integration, `getStuck`, penetration semantics). Not re-derived in this report.
- **Damage sources** — no new `DamageType` needed for any grenade path; see Key design decisions.

## Deferred scope

- **`CLUSTER`/`CLUSTER_HEAVY`/`LASER` fillings and `FRAG_SLEEVE`** — all four spawn
  `EntityBulletBaseMK4` or `EntityBulletBeamBase` submunitions built from a `BulletConfig`.
  **Hard sequencing dependency on `docs/phase3/gun_framework.md`'s Package A** (ballistics/ammo
  core) landing first — these four Consumer/Consumer lambdas cannot compile, let alone be tested,
  until `BulletConfig`/`EntityBulletBaseMK4`/`EntityBulletBeamBase` exist in this port.
- **`NUCLEAR`/`NUCLEAR_DEMO` fillings' `incrementRad(...)` call and `SCHRAB`/`NUCLEAR`'s
  `spawnMush(...)`'s `SatelliteDetector.reportEvent(...)` ping** — both target systems are
  confirmed **completely unstarted** in this port (grepped: no `ChunkRadiationManager`,
  `ContaminationUtil`, or `SatelliteDetector`/`SatelliteSavedData` file exists under `src/`).
  `docs/phase3/explosion_engine.md` already made the identical finding for `EntityFalloutRain`
  and recommended treating the fallout/radiation *system* as Phase 4; the same call applies here.
  `docs/phase3/missile_launch_infra.md` owns the satellite-addressing protocol `SatelliteDetector`
  is part of. Recommend stubbing/no-op-ing these two call sites for a first Phase 3 pass — the
  `ExplosionVNT` call in the same lambda is fully Phase-3-safe on its own.
- **`SCHRAB` filling's `EntityNukeExplosionMK3.statFacFleija(...)`/`EntityCloudFleija` spawn** —
  depends on those two entity types' registration, which `docs/phase3/bomb_blocks_and_detonators.md`
  and `docs/phase3/explosion_engine.md` already own. This filling is a one-line spawn-site once
  those land; nothing grenade-specific to design.
- **`EntityWastePearl`** — Phase 4 (`ModBlocks.fallout`/radon gas blocks), and currently has zero
  consumers in CE anyway (see above); lowest priority in this entire report.
- **All client rendering/animation**: `RenderGrenadeUniversal`, `ModelGrenade`,
  `ItemRenderGrenade`, `ItemGrenadeUniversal.getAnimation()`'s four `BusAnimation` sequences
  (per-shell equip-bob/ring-spin choreography), the `HbmEffectNT.Anim`-mode-"generic" dispatch
  `onEquip`/`sendEquipAnimation` trigger, and `EntityGrenadeUniversal`'s client-only
  `spin`/`TRAIL_TRIPLET` particle logic. `docs/phase3/weapon_animation_hooks.md` already
  identified this exact `IAnimatedItem`+`HbmEffectNT` trigger path (citing `ItemChainsaw` as its
  example) and recommends building one shared `ToolAnimationType`-style trigger payload rather
  than reviving CE's full ~50-branch `HbmEffectNT` dispatch table; `ItemGrenadeUniversal`'s equip
  trigger is exactly the second consumer that report's design already anticipates. The trigger
  *call site* (`onEquip`) is Phase-3-safe item logic; the payload's client handler and all visual
  playback are Phase 5.
- **`ItemGrenadeFishing`'s loot roll** — `LootTableList.GAMEPLAY_FISHING`/`LootContext.Builder` is
  1.12-era vanilla API with no direct 1.21.1 equivalent (loot tables are now
  `ResourceKey<LootTable>`-addressed through a registry/`LootDataManager`, and `LootParams.Builder`
  replaces `LootContext.Builder`). This is a narrow, single-item concern better resolved by
  whichever Phase 3/4 work already touches loot tables generally rather than invented ad hoc here.
- **The `Haze`/`PlasmaBlast`/`Muke`/`Tower` `AuxParticlePacketNT` broadcasts** used by WP, the
  energy fillings, `spawnMush`, and `EntityMist`'s client tick — all fold into the same generic
  `HbmEffectNT`/`AuxParticlePacketNT` VFX dispatch table `weapon_animation_hooks.md` already
  deferred to Phase 5 client-VFX-system work, not something to solve per-filling here.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code —
`ConveyorEntityTypes`, `HbmLivingAttachment`, `HbmNetwork` — for NeoForge API shape; Neo Edition
consulted read-only for entity-registration shape only, per the task's own instruction; no API
invented below):

- **Entity registration**: follow `ConveyorEntityTypes`'s already-committed pattern exactly —
  `DeferredRegister<EntityType<?>>` on `BuiltInRegistries.ENTITY_TYPE`,
  `EntityType.Builder.of(ctor, MobCategory.MISC).noSummon().sized(w, h)
  .setTrackingRange(n).build(name)` — for `EntityGrenadeUniversal` (`sized(0.25F, 0.25F)`,
  `trackingRange 64` per its `@AutoRegister`), `EntityDisperserCanister` (same size, unspecified
  tracking range in CE → default), `EntityMist` (`trackingRange 1000` per its own
  `@AutoRegister`), and, if the legacy family is ported for parity,
  `EntityGrenadeBouncyGeneric`/`EntityGrenadeImpactGeneric` (both `sized(0.25F, 0.25F)`, no
  explicit tracking range in CE).
- **Zero new `DamageType` entries needed anywhere in this package.** The vanilla-flavored
  `DamageSource.causeThrownDamage(...)` used by legacy `EntityGrenadeBase.onImpact` (0-damage,
  hit-detection bookkeeping only) maps onto 1.21.1's `level.damageSources().thrown(projectile,
  owner)`. Every real grenade-filling damage path routes through `ExplosionVNT`'s
  `EntityProcessorCrossSmooth`, which `docs/phase3/explosion_engine.md` already confirmed needs no
  grenade-specific source — either the existing `ModDamageTypes.BLAST`/`NUCLEAR_BLAST` keys or
  vanilla's own `level.damageSources().explosion(...)` family. The `EMP`/`PLASMA` fillings'
  `EntityProcessorCrossSmooth.setDamageClass(DamageClass.ELECTRIC/PLASMA)` calls are the one place
  this package touches `docs/phase3/gun_framework.md`'s `DamageClass`→`DamageType` mapping table —
  that report already flagged `ModDamageTypes` as **missing a `SEDNA_PLASMA` entry**; this is the
  same gap, not a new one, and should be fixed once, by whoever lands `DamageClass` mapping, not
  twice.
- **NBT → Data Components**: `ItemGrenadeUniversal`'s 4 NBT ints
  (`KEY_SHELL`/`KEY_FILLING`/`KEY_FUZE`/`KEY_EXTRA`) become one `DataComponentType` record (e.g.
  `GrenadeLoadout(EnumGrenadeShell shell, EnumGrenadeFilling filling, EnumGrenadeFuze fuze,
  Optional<EnumGrenadeExtra> extra)`) with a `Codec`/`StreamCodec` pair, replacing the
  `getShell`/`getFilling`/`getFuze`/`getExtra` static NBT-int-reading helpers 1:1 — each helper's
  existing "missing tag → sensible default" fallback (`FRAG`/`HE`/`S3`/`null`) should be preserved
  as the component's absence-fallback. **Recommend a name-keyed (`StringRepresentable`-style)
  `Codec` over an ordinal-keyed one** for the four enum types, even though CE itself stores raw
  ordinals — these enums are small (4/13/5/4 values) and reordering-safety is worth more than the
  few saved wire bytes an ordinal buys, unlike `BulletConfig.configs`' legitimately
  wire-size-sensitive append-only id scheme documented in `gun_framework.md`.
- **The 4 metadata-multi component items** (`ItemGrenadeShell`/`Filling`/`Fuze`/`Extra`, all
  `ItemEnumMulti<E>` with `setHasSubtypes(true)`, damage-value-keyed) should follow whatever
  convention this port's Phase 1 metadata-driven-multi item work already settled on (flagged by
  `docs/phase1/items_tool.md` for `ItemModMinecart`/`ItemDrone` as "needs expansion" into N
  separate registry items) — this report does not re-litigate that convention, it only flags that
  these four items are members of the same category and must land on the same answer, since
  `ItemGrenadeUniversal.addInformation`'s tooltip lookup
  (`ModItems.grenade_shell.getTranslationKey() + "." + shell.name().toLowerCase() + ".name"`)
  depends on whichever translation-key shape that convention produces.
- **Charge-up/equip-draw timer**: use `HbmLivingAttachment.getGrenadeDeployment()`/
  `setGrenadeDeployment(int)` directly, already committed — no new attachment field, no new
  capability.
- **`GLUE` extra's "stuck in block" behavior reuses `EntityThrowableNT.getStuck(BlockPos, int
  side)`** — the identical method bullets use to embed in blocks (`gun_framework.md`'s
  already-documented base). No grenade-specific sticky mechanic to design or re-derive.
- **The Universal Grenade's equip-bob animation trigger** should call into
  `docs/phase3/weapon_animation_hooks.md`'s recommended shared `ToolAnimationType`/
  `HbmAnimationType` payload mechanism once that's built (its own Key design decisions section
  gives the exact `CustomPacketPayload`+`StreamCodec`+`registrar.playToClient` shape,
  `HbmNetwork.registerPackets`-appended) rather than inventing a second grenade-specific animation
  packet.
- **No GUI/Menu touch point exists anywhere in this package.** None of the ~28 files read
  reference `MenuBase`/`GuiInfoContainer`/any container class — grenades are pure item+entity+
  explosion-call logic, matching `explosion_engine.md`'s identical finding for its own package.
  Nothing here should introduce a screen.
- **The dynamic crafting recipe (`GrenadeCraftingHandler`'s replacement) needs a pattern this port
  has not exercised yet.** Vanilla's own `CustomRecipe`/`SimpleCraftingRecipeSerializer` shape
  (the mechanism behind vanilla's own match-predicate-plus-custom-result table recipes, e.g. tool
  repair/banner duplication) is the well-known NeoForge-era answer for "match a predicate over the
  3×3 grid, produce a computed result" recipes with no fixed shape — but grepping this repo
  (including `upstream/neo-edition`) for `CustomRecipe`/`SimpleCraftingRecipeSerializer`/
  `RecipeSerializer` returns **zero hits**. Treat the exact class/method shape as unconfirmed until
  verified against the real 1.21.1 recipe API at implementation time — this report deliberately
  does not invent method signatures for it.
- **`EntityGrenadeBouncyGeneric`/`EntityGrenadeImpactGeneric`'s item-lookup `DataParameter<Integer>`
  must become an item reference (`ResourceLocation` or a synced item-holder id), not a raw int** —
  1.12's `Item.getIdFromItem`/`getItemById` numeric-id registry has no equivalent in 1.21.1's
  registry model.

## Open questions / risks

- **`TrackerUtil.sendTeleport`'s role after a grenade bounce** — CE calls this because 1.12's
  client movement is client-authoritative and a server-side velocity flip needs an explicit resync
  packet to avoid client/server position drift. 1.21.1's movement/physics authority model is
  different in ways this report did not verify (server-authoritative entity motion sync is largely
  automatic via the entity tracker for non-player entities). Recommend whoever ports
  `EntityGrenadeUniversal`'s bounce logic first check whether `TrackerUtil`'s equivalent (if this
  port has one, or vanilla's own entity-tracker resync) is even necessary in 1.21.1's networking
  model before porting the call — this is very plausibly dead weight, not a requirement, but this
  report did not confirm which.
- **`EntityGrenadeBouncyBase.moveBounce`'s reimplementation of `Entity#move`** is a genuine fork of
  vanilla movement/collision code from 1.12, and vanilla's own `Entity#move`/collision-resolution
  internals have changed non-trivially since (block-shape voxel collision, `Entity#collide`,
  `MoverType`). This needs to be **re-derived against 1.21.1's actual movement API at
  implementation time**, not transliterated field-for-field — the *intent* (invert+damp motion on
  any-axis collision) is what must be preserved, not the exact 1.12 AABB-sweep mechanics this
  report read.
- **`EntityFireLingering`'s `@AutoRegister(..., sendVelocityUpdates = false)` flag** — no
  equivalent parameter appears in this port's own confirmed `EntityType.Builder` usage
  (`ConveyorEntityTypes` only exercises `.noSummon().sized(...).setTrackingRange(...)`). Unclear
  whether 1.21.1's `EntityType.Builder` still distinguishes velocity-update suppression, or whether
  this was an FML-1.12-only bandwidth micro-optimization with no modern equivalent — if the latter,
  dropping the flag is a faithful-enough translation (it has no gameplay effect, only a wire-size
  one), but this report did not confirm which case applies.
- **`ItemGrenadeFishing`'s exact 1.21.1 loot-table call shape** was deliberately not resolved here
  (see Deferred scope) — flagged so it isn't silently invented at implementation time by whoever
  picks up this one item.
- **Sequencing risk, spelled out explicitly**: of the 13 fillings, 8 (`POWDER`, `HE`, `DEMO`,
  `INC`, `WP`, `EMP`, `PLASMA`, and the block-allocator-only half of `NUCLEAR`) need nothing beyond
  `ExplosionVNT` (already fully scoped in `explosion_engine.md`) and can be built as soon as that
  package lands. The other 5 (`CLUSTER`, `CLUSTER_HEAVY`, `LASER`, `NUCLEAR_DEMO`'s incidental
  submunition-free crater, `SCHRAB`) either need `gun_framework.md`'s Package A or
  `bomb_blocks_and_detonators.md`'s nuke-entity registration first. Recommend building the
  Universal Grenade item/entity/shell/fuze/extra scaffold and the 8 unblocked fillings as the first
  grenade implementation pass, and gating the remaining 5 filling lambdas behind their respective
  upstream packages rather than blocking the whole grenade system on gun-framework/nuke-entity
  work landing first.
- **Whether to preserve the two demonstrably-dead code paths** (`EntityGrenadeImpactGeneric`'s
  `fuse == -1` branch, `EntityWastePearl` entirely) for registry-id/class-layout parity, or drop
  them as genuinely unreachable in current CE — PORT_SPEC's "preserve `com.hbm.*` package layout/
  registry ids" principle argues for keeping the classes even unused; this report surfaces the
  question rather than deciding it, since it's a project-wide policy call, not a grenade-specific
  one.
- **The three independent copies of "offset spawn position from thrower's eye by hand, aim along
  look vector, apply a fixed launch force" throw-launch math** (`EntityGrenadeBase`,
  `EntityGrenadeBouncyBase`, `EntityGrenadeUniversal`, each with slightly different constants) —
  this report does not recommend unifying them beyond whatever a common superclass already
  provides, since CE itself never unified them and the three call sites' constants genuinely
  differ; flagged only so a future reviewer doesn't mistake the duplication for an oversight this
  port introduced.
