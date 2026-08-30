# CE's gravity-well/singularity entity family (`EntityVortex`/`EntityRagingVortex`/`EntityBlackHole`/`EntityQuasar`) + `ExplosionChaos` — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/effect/{EntityBlackHole,EntityVortex,EntityRagingVortex,EntityQuasar}.java`
  (220 + 50 + 65 + 23 = 358 lines — the entire gravity-well entity family; `EntityQuasar` is a 4th
  real family member this task's own framing did not name, see Headline finding)
- `upstream/hbm-ce/src/main/java/com/hbm/explosion/ExplosionChaos.java` (915 lines — the full
  method surface: `forEachBlockInSphere`/`destruction`(private) + 24 public entry points across
  26 total methods, see Phase-4-safe scope table B for every one)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/{ItemDrop,ItemDigamma}.java` (243 + 67
  lines — the two real "drop a gravity-well entity" item classes; `ItemDrop` is the one this task's
  framing named, `ItemDigamma` is the second one this research found)
- `src/main/java/com/hbm/items/special/{ScatteredMilitaryItems,ItemDigamma}.java`,
  `src/main/java/com/hbm/items/food/ItemConserve.java`, `src/main/java/com/hbm/items/special/
  ItemGlitch.java`, `src/main/java/com/hbm/items/weapon/legacy/{LegacyChargeWeapons,ItemGunB92,
  ItemGunB93}.java`, `src/main/java/com/hbm/items/tool/ItemMultitoolPassive.java`,
  `src/main/java/com/hbm/blocks/bomb/{BombFlameWar,BombFloat,NukeCustomBlock}.java`,
  `src/main/java/com/hbm/entity/missile/{EntityMissileTier1,EntityMissileTier2,EntityMissileTier3,
  EntityMissileCustom}.java` (this port's own already-committed code, read in full or targeted —
  every one of these files has a real, live, already-documented forward-reference TODO naming this
  exact package as its blocker; see Headline finding and Deferred scope)
- `src/main/java/com/hbm/config/CompatibilityConfig.java` (141 lines, read in full — confirms
  `isWarDim`/`peaceDimensions` are **deliberately not ported**, with a documented reason), `src/main/java/
  com/hbm/damage/ModDamageTypes.java` (197 lines, read in full — confirms `BLACK_HOLE`/`RUBBLE`/`PC`/
  `CLOUD` already registered) and `src/main/java/com/hbm/damage/datagen/ModDamageTypeTagsProvider.java`
  (grepped — confirms `BLACK_HOLE` already carries `DamageTypeTags.BYPASSES_ARMOR`)
- `src/main/java/com/hbm/entity/projectile/EntityRubble.java` (166 lines, read in full — already
  fully ported, real dependency of `EntityBlackHole`'s block-destruction loop and `ExplosionChaos.
  levelDown`) diffed against `upstream/hbm-ce/.../EntityRubble.java` (88 lines, read in full) to
  confirm the real API-shape change (`setMetaBasedOnBlock(Block,int)` → `setBlockState(BlockState)`)
- `src/main/java/com/hbm/entity/effect/EffectEntityTypes.java` (73 lines, read in full — the
  established per-family `DeferredRegister<EntityType<?>>` pattern this port already uses for the
  sibling `entity.effect` package, the template this family's own registration should follow)
- `src/main/java/com/hbm/handler/ArmorUtil.java` (378 lines total, `damageSuit`/`checkForHazmat`/
  `damageGasMaskFilter` signatures read) and `src/main/java/com/hbm/util/ArmorRegistry.java` (120
  lines, read in full — `hasProtection`/`hasAllProtection`/`HazardClass` enum) — confirms
  `ExplosionChaos.c`/`pc`/`poison`'s entire armor/hazmat-interaction surface is already real and ready
- `src/main/java/com/hbm/config/WeaponConfig.java` (56 lines, read in full — `DROP_CELL`/
  `DROP_SINGULARITY`/`DROP_CRYSTAL`/`DROP_DEAD_MANS_EXPLOSIVE` all confirmed already wired with
  CE-config-key comments matching `ItemDrop`'s/`ItemDigamma`'s real gates)
- `docs/phase3/scattered_military_items.md` (full read — the report whose own Deferred scope this
  task's framing quotes; confirmed its "harmless singularity/xen half" section and cross-checked
  every field name it lists against `ItemDrop.java`'s real source), `docs/phase3/explosion_engine.md`
  (full read — confirmed its own "signature-surveyed only" flag on `ExplosionChaos`), `docs/phase3/
  gun_framework.md` (full read — confirms `EntityBulletBase`/`EntityBullet`'s status as the "still-live
  legacy projectile system," relevant to `ExplosionChaos.tauMeSinPi`'s dependency below)
- `docs/phase4/{fallout_rain_and_effects,hbm_potion_system,entities_creeper_variants,
  entities_vehicles_aircraft,satellites_followup_and_loot_pools,entities_legacy_bullet_system}.md`
  (grepped for `isWarDim`/`EntityRocket`/`EntityMiniNuke`/`EntitySchrab`/`EntityRainbow`/`EntityBullet`/
  `Quasar` to confirm this session's own sibling-report landscape before claiming or deferring
  anything — see Headline finding #3 and Deferred scope)
- `upstream/neo-edition/src/main/java/com/hbm/entity/effect/{BlackHole,Vortex,RagingVortex}.java`
  (149 + 60 + 65 lines, read in full) and `.../entity/NtmEntityTypes.java` (registration lines for
  all 4 — yes, 4, see Headline finding #2 — `EntityType`s, read) — **cross-referenced for confirmed
  NeoForge 1.21.1 API shape only**, per this task's ground rules; every behavioral number/formula
  below is sourced from CE, and every place Neo Edition's own port *deviates* from CE's real behavior
  is called out explicitly as "do not copy," not silently followed
- Repo-wide greps: `ExplosionChaos\.` (66 call sites across 26 CE files, and every mention inside this
  port's own `src/`), `new EntityVortex\(`/`setShrinkRate`/`EntityQuasar`/`ItemDigamma` (to find every
  real spawn site beyond `ItemDrop`), `isWarDim` (across CE, this port, and every Phase 4 sibling
  report), `waste_earth|sellafield|crystal_virus|reinforced_brick` etc. against `ModBlocks.java` (to
  confirm the wasteland/wall-block set `ExplosionChaos.decontaminate`/`hardenVirus`/`explode` need is
  not yet registered), and `EntityRocket|EntityMiniNuke|EntitySchrab|EntityRainbow` against CE (to size
  the unowned legacy-artillery family `ExplosionChaos.cluster`/`miniMirv`/`schrab`/`zomg` depend on)

## Headline finding

The task's own framing is directionally right (this family is genuinely unowned, and `ExplosionChaos`
genuinely needed a dedicated follow-up read) but understates the shape of what a full read actually
found in five separate ways:

1. **Of the 8 `ItemDrop` fields named in the task, only 4 actually spawn a gravity-well entity, and
   `pellet_antimatter` does not spawn anything at all.** Reading `ItemDrop.onEntityItemUpdate` in full
   (lines 91–162) gives the exact wiring:
   - `singularity` → `EntityVortex(1.5F)`; `singularity_counter_resonant` → `EntityVortex(2.5F)`;
     `singularity_super_heated` → `EntityVortex(2.5F)` (identical size to the counter-resonant one —
     CE gives these two items the same drop effect despite different tooltips/flavor text);
     `singularity_spark` → `EntityRagingVortex(3.5F)`; `black_hole` → `EntityBlackHole(1.5F)`. All 5
     are gated by `WeaponConfig.dropSing` (this port's already-real `DROP_SINGULARITY`).
   - `capsule_xen`/`crystal_xen` spawn **no entity at all** — they call `ExplosionChaos.floater(...)`
     then `ExplosionChaos.move(...)` directly (capsule: radius 3/height 8; crystal: radius 25/height
     75), gated by `WeaponConfig.dropCrys` (`DROP_CRYSTAL`). This is why the task correctly grouped
     them with the entity family in spirit (same "unowned, needs the same follow-up" bucket) even
     though mechanically they're pure-`ExplosionChaos` callers, not entity spawners.
   - `pellet_antimatter`'s drop effect is `ExplosionLarge.explodeFire(world, thrower, x, y, z, 100,
     true, true, true)`, gated by `WeaponConfig.dropCell` (`DROP_CELL`) — **already-real, already-
     ported infrastructure** (`ExplosionLarge` is committed Phase 3 code, see Key design decisions).
     Its actual relationship to the gravity-well family is a *different* real mechanic living inside
     `EntityBlackHole.onUpdate()` itself, not inside `ItemDrop`: when a dropped `pellet_antimatter` (or
     the flavor item `flame_pony`) drifts within a black hole's consumption radius, the black hole
     self-destructs and is replaced by a real `world.createExplosion(null, x, y, z, 5.0F, true)` —
     this is the concrete mechanic behind the item's own tooltip line, "Gets rid of black holes." So
     `pellet_antimatter` is a real, load-bearing part of this family's behavior, just not as a spawner.
2. **The family has a 4th real, currently-unnamed member — `EntityQuasar` — and its own real,
   already-committed, already-blocked consumer in this port today.** `com.hbm.entity.effect.
   EntityQuasar` (23 lines, read in full) is a trivial `EntityBlackHole` subclass (`@AutoRegister(name
   = "entity_digamma_quasar")`, two constructors mirroring `EntityVortex`'s shape, an `onUpdate()`
   override whose entire body is `super.onUpdate()` — i.e. zero behavioral difference from its parent
   beyond the registry name/renderer) spawned by `ItemDigamma.onEntityItemUpdate` at a fixed size of
   `5F`, gated by the same `WeaponConfig.dropSing`. Neither `docs/phase1/items_special.md` nor any
   Phase 4 sibling report (checked by grep across this session's own `docs/phase4/*.md`) names
   `EntityQuasar` anywhere. But **`ItemDigamma` is already ported in this repo**
   (`src/main/java/com/hbm/items/special/ItemDigamma.java`, from Phase 1/3's implementation wave) with
   its `onEntityItemUpdate` override already present and already carrying the exact stub this package
   needs to unblock: `// EntityQuasar spawn-on-drop deferred - see class javadoc`, returning `false`
   unconditionally. This is a real, already-compiling, already-committed call site waiting on this
   exact package — not a hypothetical future consumer.
3. **`CompatibilityConfig.isWarDim` — which gates most of `EntityBlackHole`'s own `onUpdate()` (and
   therefore every subclass: `EntityVortex`, `EntityRagingVortex`, `EntityQuasar`) plus roughly
   two-thirds of `ExplosionChaos`'s methods — is not a fresh open question for this report to raise.
   It is a settled, port-wide precedent, already applied by real committed code and independently
   re-derived by five separate Phase 4 sibling reports written this same research wave.**
   `CompatibilityConfig.java`'s own class javadoc states the reason explicitly: CE's ~60 dimension-ID-
   keyed tables (including `peaceDimensions`, which `isWarDim` reads) are keyed by a Forge-1.12
   integer dimension id, "a concept that no longer exists in 1.21," and re-keying them on guessed
   mod-compatibility targets "would be worse than not porting them" — deferred wholesale to "whichever
   phase owns world generation." Already-committed Phase 3 code has already made the call for what to
   do about every *consumer* of that gate in the meantime: `src/main/java/com/hbm/explosion/
   ExplosionLarge.java`'s own class javadoc states outright, "CE's `isWarDim` gates on `jolt`/
   `explodeFire`/`buster` are dropped per this port's documented always-true default." Five sibling
   Phase 4 reports from this same wave (`fallout_rain_and_effects.md`, `hbm_potion_system.md`,
   `entities_creeper_variants.md`, `entities_vehicles_aircraft.md`,
   `satellites_followup_and_loot_pools.md`) all independently re-derive and confirm the identical
   policy, several citing the exact same CE-config-default reasoning: `peaceDimensionsIsWhitelist`
   defaults to `true` with an **empty** default `peaceDimensions` set, so `isWarDim` returns `true` in
   every dimension out of the box in real CE — stubbing it to `true` (not `false`) is the CE-faithful
   default, not a safety fallback. This report adopts the identical policy for `EntityBlackHole`'s
   family and `ExplosionChaos` rather than re-litigating it (see Key design decisions and Deferred
   scope for the one wrinkle specific to this package: the gate is **not applied uniformly** across
   every `ExplosionChaos` method — see finding 4).
4. **`ExplosionChaos` really is a 26-method grab-bag spanning at least five unrelated concerns, and
   only 2 of its methods (`floater`/`move`) are what the xen items in this task's framing actually
   call.** The other 24 span: block-sphere destruction (`explode`, `explodeZOMG`, `pulse`,
   `levelDown`), fire-setting (`flameDeath`, `burn`), gas/poison-cloud area effects tied to CE's armor/
   hazmat system (`c`, `pc`, `poison`), particle-only VFX broadcasts (`spawnChlorine`, `spawnVolley`),
   a legacy artillery/rocket/boss-projectile spawner family entirely unrelated to gravity wells
   (`cluster`, `miniMirv`, `frag`, `schrab`, `tauMeSinPi`, `zomg`), block-relocation/entity-shove
   (`floater`, `move` — the two the xen items use), and a corruption-terrain reversal table
   (`decontaminate`, `hardenVirus`, `spreadVirus`). CE's own in-file comment ("this whole class looks
   outdated as fuck") and `explosion_engine.md`'s "misc grenade/warhead effect grab-bag"
   characterization both undersell just how unrelated these sub-groups are to each other — this is not
   one cohesive "exotic effects" module with a common data model, it is 26 independent static methods
   sharing only a source file and a private sphere-iteration helper (`forEachBlockInSphere`). See
   Phase-4-safe scope table B for the full per-method survey. **A second, more subtle finding inside
   this one**: the `isWarDim` gate from finding 3 is applied **inconsistently across these methods** —
   `floater` has it, `move` (used by the exact same two xen items, in the exact same `ItemDrop` call
   sequence) does not; `explode`/`flameDeath`/`burn`/`c`/`pc`/`poison`/`spawnChlorine`/`spawnVolley`/
   `explodeZOMG`/`pulse`/`hardenVirus`/`spreadVirus` all have it, while `spawnExplosion`/`cluster`/
   `miniMirv`/`frag`/`schrab`/`tauMeSinPi`/`zomg`/`levelDown`/`decontaminate` do not. This asymmetry
   must be preserved per-method under finding 3's "drop the gate, run unconditionally" policy — it
   produces real, CE-faithful behavior (a dropped `crystal_xen` outside the war dimension still shoves
   nearby entities and renames them Dinnerbone/`jeb_` via the ungated `move`, but does not lift terrain
   via the gated `floater`), not a bug to unify away by adding a matching gate to `move` or removing one
   from `floater`.
5. **This family's real footprint in the mod is far wider than `ItemDrop`, and three of the other four
   consumer files are *already ported and already blocked* in this port today** — most notably,
   **half of the `gun_b93` legendary weapon's entire charge-escalation identity is currently flattened
   pending this exact package**:
   - `ItemDigamma` (finding 2, already covered above).
   - `src/main/java/com/hbm/items/weapon/legacy/LegacyChargeWeapons.java` (already ported and
     registered, backing the real, already-committed `ItemGunB92`/`ItemGunB93` items) is CE's
     `gun_b93`'s real 10-tier charge-escalation explosion (`EntityModBeam#explode()`'s `mode` 0–9
     switch), reimplemented directly on this port's own `EntityBulletBaseMK4`/`BulletConfig` rather
     than porting the CE-only `EntityModBeam` class. Its own class javadoc says explicitly: "modes
     4/5 (`EntityVortex`), 6/7 (`EntityRagingVortex`), and 8 (`EntityBlackHole`) all need entity
     classes that do not exist anywhere in this port yet ... those 5 tiers fall back to mode 3's real,
     already-portable tier ... rather than a silent no-op or a crash." Reading CE's real
     `EntityModBeam.java` (grepped, not read in full — out of this report's own scope, the class is
     confirmed CE-only content this port has already decided not to reimplement 1:1) gives the exact
     parameters that fallback is standing in for: **mode 4 → `EntityVortex(1F)`, mode 5 →
     `EntityVortex(2.5F)`, mode 6 → `EntityRagingVortex(2.5F)`, mode 7 → `EntityRagingVortex(5F)`,
     mode 8 → `EntityBlackHole(2F)`** — a genuine, real gap this report can now hand back with exact
     numbers rather than leaving `LegacyChargeWeapons.java`'s implementer to re-derive them from
     `EntityModBeam.java` from scratch.
   - `src/main/java/com/hbm/items/food/ItemConserve.java` (already ported) has its `FoodType.BHOLE`
     branch already stubbed: `// CE spawns an EntityVortex (0.5 size, 0.01 shrink rate, no-break) at
     the eater's position here`. This is also the **only real CE call site that ever calls
     `EntityVortex.setShrinkRate(...)`** (see Open questions for why that call is silently a no-op in
     CE's own real behavior today).
   - `src/main/java/com/hbm/items/special/ItemGlitch.java` (registered, effect table intentionally
     deferred wholesale per Phase 1's own explicit guidance — "implement/port the effect table
     incrementally... spanning every future phase") has a real case (CE's switch-case 27) that spawns
     `EntityVortex(2.5F)` 15 blocks below the player, and a separate case that calls
     `ExplosionChaos.burn(world, null, pos, 5)`. This report does not claim `ItemGlitch`'s 31-case
     table (correctly out of scope per Phase 1's own framing, a whole-item cross-phase job), but flags
     both real call sites for whoever eventually completes it.

## Phase-4-safe scope

### Table A — the entity family (`com.hbm.entity.effect`, all 4 read in full)

| Class | Lines | Real CE behavior | Portability |
|---|---|---|---|
| `EntityBlackHole` | 220 | The base class and the one with all the real logic (`EntityVortex`/`EntityRagingVortex`/`EntityQuasar` all extend it and add only a shrink/pulse/rename curve on top). `onUpdate()`: (1) bail immediately if `!CompatibilityConfig.isWarDim(world)` (see Headline finding 3); (2) if `breaksBlocks` (default `true`, toggled off only via `.noBreak()`, which no real CE call site this survey found ever calls) and server-side: fire `size*2` random rays from a uniform-sphere direction sample (`phi`/`costheta`/`theta` spherical coordinates — this is a genuine uniform-on-sphere sampler, not a naive independent-axis random, so its exact math should be preserved rather than replaced with a simpler approximation), walk up to `ceil(size*15)` blocks along each ray, and on the **first** solid block hit: convert liquids to air in-place and keep walking (liquids don't count as "the first solid block" and don't stop the ray), then on the first genuinely solid block spawn an `EntityRubble` at that position (carrying the destroyed block's identity) and `break` out of the per-ray loop — i.e. each ray destroys **at most one** block; (3) build one AABB of side `size*15*2` centered on the black hole and scan every entity in it (`getEntitiesWithinAABBExcludingEntity`, i.e. excluding itself); for each entity: skip creative-mode players outright; if it's a mid-air `EntityFallingBlock` older than 1 tick, kill it and replace it with an `EntityRubble` carrying its identity and velocity (falling blocks don't survive a black hole, they become debris); compute the normalized pull vector toward the black hole's center, and — for every entity that is *not* an `EntityItem` — rotate that vector 15° around the yaw axis before applying it (this is the visual "orbit/swirl" effect: non-item entities corkscrew inward, dropped items beeline straight to center), then *add* `(vec * 0.1, vec.y * 0.2, vec * 0.1)` to the entity's existing motion every tick (unbounded, no damping on the pull itself — only the black hole's own motion is damped, see below); skip further processing for other `EntityBlackHole`s (so black holes don't consume each other); if the entity is within the much smaller `size*1.5` "consumption" radius, deal `1000.0F` `ModDamageSource.blackhole` damage (`.setDamageIsAbsolute().setDamageBypassesArmor()`), outright kill anything that isn't an `EntityLivingBase`, and — **only** for a consumed `EntityItem` carrying `pellet_antimatter` or `flame_pony` — self-destruct the black hole and replace it with a real `world.createExplosion(null, x, y, z, 5.0F, true)`; (4) finally, move the black hole itself by its own (usually-zero, since no real spawn site gives it nonzero initial motion) accumulated velocity and damp that velocity ×0.99/tick. `isImmuneToExplosions()` returns `true` (black holes can't be blown up by anything else). Client-only: `isInRangeToRenderDist` (25000 blocks — always renders when loaded) and a full-bright `getBrightnessForRender`/`getBrightness` (Phase 5 concern, not reproduced in detail here). |
| `EntityVortex` | 50 | `extends EntityBlackHole`. Adds one field (`shrinkRate`, default `0.0025F`, public `setShrinkRate`/persisted to NBT) and overrides `onUpdate()` to shrink `SIZE` by a **hardcoded literal `0.0025F` every tick** (not `this.shrinkRate` — see Open questions for why this makes `setShrinkRate` a real, reachable, but silently-ignored CE bug) before delegating to `super.onUpdate()`; once `SIZE <= 0` the vortex self-destructs. This is the "temporary, self-extinguishing" variant of the black hole — every real spawn size this survey found (0.5F–3.5F) fully decays within a few hundred to ~1400 ticks at the real 0.0025F/tick rate. |
| `EntityRagingVortex` | 65 | `extends EntityBlackHole`. Adds a `timer` field and overrides `onUpdate()` with its **own separate, redundant** `!CompatibilityConfig.isWarDim(world)` re-check (already checked once by the `super.onUpdate()` call this method makes at its end — CE checks the gate twice per tick for this one entity, an inconsistency worth preserving faithfully rather than "cleaning up" into a single check, since removing the redundant check is itself a small behavior change once the always-true stub is applied — though a harmless one). Each tick: increments `timer` (with a dead branch — `if(timer <= 20) timer -= 20;` can never fire meaningfully once `timer` starts at 0 and only increments, so this line is effectively a no-op after the very first tick; confirmed by reading, not assumed), computes a sinusoidal "pulse" shrink term (`sin(timer) * π/20 * 0.35`), and — on a flat 1-in-100 roll per tick — an additional `0.1F` shrink plus a real `world.createExplosion(null, x, y, z, 10F, false)` (radius 10, **no block damage** — the `false` flag). `SIZE` shrinks by `pulse + dec` every tick (so it can occasionally *grow* slightly when `pulse` goes negative on the sine wave's downswing, before the next 1-in-100 explosion-and-shrink event) until it hits 0. This is the "occasionally detonates while alive" variant. |
| `EntityQuasar` | 23 | `extends EntityBlackHole`. Zero behavioral override beyond the two constructors matching `EntityVortex`'s shape (`(World)` flavor-flag constructor, `(World, float size)` sized constructor) and an `onUpdate()` whose entire body is `super.onUpdate()` — i.e., this class exists purely to give the `ItemDigamma`-flavored black hole its own `@AutoRegister` name (`entity_digamma_quasar`) and its own client renderer (`RenderQuasar`, not read — Phase 5), not to add any new mechanic. Functionally indistinguishable from a plain `EntityBlackHole` at the fixed size `5F` `ItemDigamma` always constructs it at. |

### Table B — `ExplosionChaos`'s full real method surface (26 methods, all read in full)

`forEachBlockInSphere` (private, 26 lines) is the shared engine behind roughly half these methods —
an optimized sphere-iteration loop (compute a shrinking `xz` bound per `y` layer from the sphere
equation, rather than iterating a full cube and rejecting points outside the radius) that clamps `y`
to `[0,255]` (a hardcoded 1.12 world-height limit — needs a 1.21 world-height-aware replacement, see
Key design decisions) and calls a `Consumer<MutableBlockPos>` per in-sphere column position. This
exact algorithm (not a naive cube-scan) should be preserved for any method built on it, since several
callers pass very large radii (`explodeZOMG`'s "ZOMG" name suggests deliberately huge blasts).

| Method | `isWarDim`-gated? | What it does | Real dependency this report found |
|---|---|---|---|
| `explode(world, detonator, x, y, z, bombStartStrength)` | Yes | Sphere block-clear, skipping an indestructible list (`Blocks.BEDROCK`, 4 named `ModBlocks.reinforced_*`, or resistance > 2,000,000). | `ModBlocks.reinforced_brick`/`reinforced_sand`/`reinforced_glass`/`reinforced_lamp_on`/`reinforced_lamp_off` — confirmed **not yet registered** in this port's `ModBlocks.java` (see Deferred scope). Already a real, already-committed consumer: `BombFlameWar.explode` (`explode(world, detonator, x, y, z, 15)`). |
| `spawnExplosion(world, detonator, x, y, z, bound)` | **No** | 25×8 = 200 random-position vanilla `world.createExplosion(..., 10.0F, true)` calls scattered inside a `bound`-sized cube around the point — a "carpet bomb a volume with real vanilla explosions" effect, not a custom sphere-clear. | None beyond vanilla `Level#explode`. Already a real, already-committed consumer: `BombFlameWar.explode` (`spawnExplosion(world, detonator, x, y, z, 75)`). |
| `c(world, x, y, z, bombStartStrength)` (CE's own name — "cloudPoisoning" per an inline dev comment) | Yes | AABB entity scan (radius `2×bombStartStrength`); damages all 4 armor slots 5pts each unless `GAS_BLISTERING`-protected; then, unless the entity is hazmat-protected, either converts an active `taint` potion into a fresh 1-hour `mutation` effect, or (no `taint` active) damages the gas-mask filter if `BACTERIA`-protected, else deals 3 `ModDamageSource.cloud` damage. | `HbmPotion.taint`/`HbmPotion.mutation` — confirmed **not yet ported** (only `PotionConfig.java`, the config-value file, exists; no `com.hbm.potion` package). `ArmorRegistry.hasProtection`/`ArmorUtil.damageSuit`/`checkForHazmat`/`damageGasMaskFilter` — all confirmed **already real and ready** (see Key design decisions). `ModDamageTypes.CLOUD` — already registered. |
| `flameDeath(world, detonator, pos, bound)` | Yes | Sphere scan: place fire above any block flammable-facing-up whose above-block is air. | None new. Already real, already-committed consumers: `EntityMissileTier2`/`EntityMissileTier3` (both TODO'd, "CE ignites a 25-block-radius area here"). |
| `burn(world, detonator, pos, bound)` | Yes | Sphere scan: place fire above any non-air block whose above-block is air or snow layer (broader trigger condition than `flameDeath` — doesn't require flammability). | None new. Already real, already-committed consumers: `EntityMissileTier3` (TODO'd alongside `flameDeath`), `ItemGlitch` (`burn(world, null, pos, 5)`). |
| `spawnChlorine(world, x, y, z, count, speed, type)` | Yes | Pure particle/VFX broadcast — networked `AuxParticlePacketNT` plus a client-side `EntityModFXShadow` spawn, 4 color/type variants (chlorine/cloud/pink-cloud/orange), Gaussian-scattered per-particle motion. | `EntityModFXShadow`, `AuxParticlePacketNT`, `HbmEffectNT`, `PacketThreading` — all confirmed **not yet ported** (Phase 5 particle/networking layer). Already a real, already-committed consumer: `EntityMissileCustom` (`spawnChlorine(level, ..., 750, 2.5, 2)` TODO'd). |
| `pc(world, x, y, z, bombStartStrength)` (CE's own name — "pinkCloudPoisoning") | Yes | Same AABB-scan shape as `c`, but heavier armor damage (25pts/slot vs 5), a different gate combo (`BACTERIA`+`SAND` both required to avoid the mask-filter-damage branch), and a different damage source (`ModDamageTypes.PC`, 5 damage) with no `taint`→`mutation` branch at all. | Same armor/hazmat surface as `c`, already real. `ModDamageTypes.PC` — already registered. No known real call site found in this survey's own file set (not `ItemDrop`/`ItemDigamma`-adjacent). |
| `poison(world, x, y, z, bombStartStrength)` (CE's own comment: "used by grenades and Chlorine seal gas blocks") | Yes | AABB scan restricted to `EntityLivingBase`; unless `NERVE_AGENT`-hazmat-protected (then just damages the mask filter), stacks 5 vanilla potion effects (Blindness/Poison/Wither/Slowness/Mining Fatigue). | `ArmorRegistry.hasAllProtection`/`ArmorUtil.damageGasMaskFilter` — already real. Vanilla `MobEffects` — already real. No call site found in this survey's own file set. |
| `cluster(world, x, y, z, count, gravity)` | **No** | Spawns `count` `EntityRocket` submunitions with randomized (and occasionally negated) direction components. | `EntityRocket` (542 lines) — confirmed **not ported**, not named by any Phase 4 sibling report (see Deferred scope). Already real, already-committed consumers: `EntityMissileTier1`/`Tier2`/`Tier3` (all TODO'd, "CE scatters N sub-munitions here"). |
| `miniMirv(world, x, y, z)` | **No** | Spawns exactly 8 `EntityMiniNuke`s in a fixed geometric pattern — 4 along the cardinal-ish axes, 4 along the diagonals (`zeta = √2/2`), each with a shared downward-biased random `motionY`. Note the method name ("mini-MIRV") vs. the entity it actually spawns (`EntityMiniNuke`, not `EntityMiniMIRV` — CE has a *separate* `EntityMiniMIRV.java` class this method does not use, confirmed by grep; do not conflate the two when this method is eventually implemented). | `EntityMiniNuke` (585 lines) — confirmed **not ported**. No known real call site in this survey's own file set (a signature-surveyed, standalone method). |
| `explodeZOMG(world, x, y, z, bombStartStrength)` | Yes | Sphere block-clear, but with a different indestructible test than `explode` (resistance > 2,000,000 **and** `y <= 0`, i.e. at `y > 0` even very hard blocks get cleared — presumably to guarantee a bottomless pit rather than leaving a bedrock floor at the world's low build limit). | None beyond world-height-limit handling (see `forEachBlockInSphere`'s note above). No known real call site in this survey's own file set. |
| `frag(world, x, y, z, count, flame, shooter)` | **No** | Spawns `count` vanilla `EntityTippedArrow`s, critical-flagged, optionally set on fire for 1000 ticks, fixed 2.5 damage. | Vanilla `EntityArrow`/`EntityTippedArrow` only — trivially portable whenever this method is implemented, no forward reference. |
| `schrab(world, x, y, z, count, gravity)` | **No** | Spawns `count` `EntitySchrab` fragments, same randomized-direction shape as `cluster`. | `EntitySchrab` (586 lines) — confirmed **not ported**, unnamed by any Phase 4 sibling report. |
| `pulse(world, x, y, z, bombStartStrength)` | Yes | Sphere scan; any block with resistance ≤ 70 gets passed to `pDestruction`. | See `pDestruction` below (its only caller). |
| `pDestruction(world, x, y, z)` (private helper `pulse` alone calls) | n/a | Spawns a vanilla `EntityFallingBlock` at the position, carrying that position's **current** `IBlockState` — but **never removes the original block from the world**. | This looks like a genuine CE bug (block duplication: the original solid block stays in place while an identical falling-block entity spawns on top of it) rather than intentional design — flagged, not silently "fixed," see Open questions. No dependency beyond vanilla. |
| `tauMeSinPi(world, x, y, z, count, shooter, tau)` | **No** | Player-shooter-only: spawns `count` legacy `EntityBullet`s (85% chance "eyyOk" flavor, 35–45 damage; 15% chance "tauDay" flavor, 100–400 damage), randomized direction, critical-flagged. | `EntityBullet` — already the subject of `docs/phase4/entities_legacy_bullet_system.md` (build it there or in `entities_bosses.md`, per that report's own framing); that report **already names this exact call site** (`ExplosionChaos.java:643,646`) as one of `EntityBullet`'s real consumers. Do not re-derive `EntityBullet` here — just confirm this method is one more consumer once it exists. |
| `zomg(world, x, y, z, count, shooter, zomg)` | **No** | Spawns `count` `EntityRainbow` projectiles anchored at a given entity's position/rotation (or the raw coordinates if no anchor entity), playing a dedicated `zomgShoot` sound per shot. | `EntityRainbow` (515 lines) — confirmed **not ported**, unnamed by any Phase 4 sibling report. Already a real, already-committed consumer: `NukeCustomBlock.explodeCustom`'s euphemium tier (`zomg(world, x, y, z, (int)(100*euph), detonator, null)`, already TODO'd in that already-committed file). |
| `spawnVolley(world, x, y, z, count, speed)` | Yes | Same particle/VFX-only shape as `spawnChlorine` (single hardcoded orange type, no color-type switch), heavier vertical speed bias (`×7.5` on the Y Gaussian term). | Same VFX dependency as `spawnChlorine`, confirmed absent. No known real call site in this survey's own file set. |
| `floater(BlockPos overload)` / `floater(world, detonator, x, y, z, radi, height)` | Yes | Sphere scan: for every non-air block, remove it from its current position and re-place the identical `IBlockState` `height` blocks higher — a "lift a hemisphere of terrain straight up" effect. | None beyond vanilla block API. **Real, already-committed consumer**: `ItemDrop`'s `capsule_xen`/`crystal_xen` branches (see Headline finding 1) and `BombFloat`'s `float_bomb` variant (already TODO'd: `floater(world, detonator, pos, 15, 50)`). |
| `move(BlockPos overload)` / `move(world, x, y, z, radius, a, b, c)` | **No** | AABB entity scan (radius `2×radius`, note the parameter is doubled internally same as `c`/`pc`/`poison`'s `bombStartStrength`); for any `EntityLiving` that isn't a sheep, a coin-flip rename to `"Dinnerbone"` or `"Grumm"` (the vanilla upside-down-mob easter egg); for any `EntitySheep`, unconditional rename to `"jeb_"` (the vanilla rainbow-sheep easter egg); then, for any entity within `radius` of the point, translate its position by the fixed offset `(a, b, c)`. **This rename/translate logic runs on every matching entity in range regardless of whether they're actually being "moved" by a large offset** — it's really "shove nearby mobs a fixed distance and, as a side effect, always re-roll their Dinnerbone/Grumm/jeb_ name tag," a CE easter egg baked into a terrain-manipulation utility. | None beyond vanilla entity API. **Real, already-committed consumer**: `ItemDrop`'s `capsule_xen`/`crystal_xen` branches (paired with `floater`, always called together) and `BombFloat`'s `float_bomb` variant (`move(world, pos, 15, 0, 50, 0)`, already TODO'd). |
| `levelDown(world, x, y, z, radius)` | **No** | Square-column scan (not a sphere — a flat `x`/`z` grid at fixed `y`): any block with `0 < hardness < 6000` becomes an `EntityRubble` (given a small upward launch velocity, `0.025*10+0.15 = 0.4`) and is cleared to air. | `EntityRubble` — **already fully ported** (see Table A's dependency notes and Key design decisions for its exact modern-API call shape). Real, already-committed consumer: `ItemMultitoolPassive`'s `Rung#MEGA`, already documented as "a permanent no-op until a replacement dimension gate is designed" pending `CompatibilityConfig.isWarDim` (see Headline finding 3 — this port's own established precedent resolves that specific wrinkle: since `levelDown` has **no** `isWarDim` gate of its own in CE, `ItemMultitoolPassive`'s own blocker is really just "`ExplosionChaos.levelDown` doesn't exist yet," not the dimension gate its comment focuses on — worth a quick correction pass on that file once this package lands). |
| `decontaminate(world, pos)` | **No** | Single-block corruption-reversal table: 11 `else if` branches converting CE's "wasteland" decoration blocks back toward vanilla equivalents at various random-chance thresholds (`waste_earth`→grass, `waste_grass_tall`→tallgrass, `waste_mycelium`→mycelium, `waste_leaves`→leaves, `waste_trinitite`(_red)→(red_)sand, `waste_log`→log, `waste_planks`→planks, `block_trinitite`/`block_waste`→lead block, and a 5-step `sellafield` meta decay chain terminating at `sellafield_slaked`→`stone`). | All 10+ named `ModBlocks.*` fields — confirmed **not yet registered anywhere** in this port's `ModBlocks.java` (see Deferred scope). No known real call site in this survey's own file set (a standalone utility, presumably called from a decontamination item/block this survey did not scope). |
| `hardenVirus(world, x, y, z, bombStartStrength)` | Yes | Sphere scan: `ModBlocks.crystal_virus` → `ModBlocks.crystal_hardened`. | `ModBlocks.crystal_virus`/`crystal_hardened` — confirmed **not yet registered**. |
| `spreadVirus(world, x, y, z, bombStartStrength)` | Yes | Sphere scan: 1-in-15 chance to convert any non-air block into `ModBlocks.cheater_virus_seed`. | `ModBlocks.cheater_virus_seed` — confirmed **not yet registered**. |

### `ItemDrop`'s exact xen/singularity wiring (the 4 fields the task named that this report resolves in full)

Already detailed in Headline finding 1 — restated here as the compact summary a future implementer
needs: `singularity`/`singularity_counter_resonant`/`singularity_super_heated`/`singularity_spark`/
`black_hole` spawn Table A's entities directly (sizes above); `capsule_xen`/`crystal_xen` call Table
B's `floater`+`move` directly with no entity involved; `pellet_antimatter` calls neither — it's a
`ExplosionLarge.explodeFire` caller (already real) whose only tie to this family is being consumed by
a nearby `EntityBlackHole`'s own logic. All six real branches trigger only `if(entityItem.onGround ||
entityItem.isBurning())` — i.e., a thrown/dropped item sitting on the ground or currently on fire,
not the instant it's dropped — and `entityItem.setDead()` unconditionally afterward, matching CE's
real "one-shot on landing" semantics, not a per-tick repeat.

## Deferred scope

Real dependencies of *this specific* package that belong to other phases/areas, or that this report
found are real but chose not to claim:

- **`com.hbm.config.CompatibilityConfig.isWarDim`/`peaceDimensions`** — per Headline finding 3, this
  is a **settled port-wide precedent** (stub always-`true`, drop the gate, apply per-method exactly as
  CE gates it — see finding 4's asymmetry note), not an unresolved blocker. The actual dimension-
  ID-to-`ResourceKey<Level>` re-keying work belongs to "whichever phase owns world generation" per
  `CompatibilityConfig.java`'s own javadoc — this package does not need to wait for that, only to apply
  the already-established stub.
- **`com.hbm.potion.HbmPotion`** (specifically `taint`/`mutation`) — confirmed absent from this port
  (only `com.hbm.config.PotionConfig`, the config-value file, exists; no `com.hbm.potion` package at
  all). Blocks only `ExplosionChaos.c`'s taint→mutation conversion branch — every other line of `c`,
  and all of `pc`/`poison`, are fully portable today against this port's already-real `ArmorUtil`/
  `ArmorRegistry`. `docs/phase4/hbm_potion_system.md` (already written this wave) is the owner of
  `HbmPotion` itself — this report does not re-scope it, only names the one call site waiting on it.
- **The wasteland/corruption `ModBlocks.*` set** (`waste_earth`, `waste_grass_tall`, `waste_mycelium`,
  `waste_leaves`, `waste_trinitite`(_red), `waste_log`, `waste_planks`, `block_trinitite`,
  `block_waste`, `sellafield`(_slaked), `crystal_virus`, `crystal_hardened`, `cheater_virus_seed`) and
  the indestructible-block set (`reinforced_brick`/`reinforced_sand`/`reinforced_glass`/
  `reinforced_lamp_on`/`reinforced_lamp_off`) — confirmed **not registered anywhere** in this port's
  `ModBlocks.java` today (grepped directly, zero matches). Blocks `ExplosionChaos.decontaminate`/
  `hardenVirus`/`spreadVirus` entirely and narrows `explode`'s indestructibility check to just
  `Blocks.BEDROCK` + the resistance threshold until the `reinforced_*` set exists. Whichever Phase 4
  (or Phase 2 leftover) area owns world-gen decoration/hazard blocks should supply these; not named by
  any sibling report checked this wave, flagged as a real gap.
- **The legacy artillery/rocket/boss-projectile entity family** (`EntityRocket` 542 lines,
  `EntityMiniNuke` 585 lines, `EntitySchrab` 586 lines, `EntityRainbow` 515 lines — all `extends Entity
  implements IProjectile` in CE, none read beyond their class declaration/`extends` line in this
  survey, all confirmed **not ported** and **not named by any Phase 4 sibling report checked this
  wave**) — real dependencies of `ExplosionChaos.cluster`/`miniMirv`/`schrab`/`zomg` respectively.
  These four are unrelated to the gravity-well family and to each other beyond sharing the
  `IProjectile` marker interface; recommend a dedicated future entity-family research pass (this
  report's own scope was the vortex/black-hole family plus `ExplosionChaos`'s full method survey, not
  a fifth entity-family deep-dive) rather than either this report or `entities_legacy_bullet_system.md`
  silently absorbing them by proximity, the same caution `scattered_military_items.md` raised for this
  very family before this report existed.
- **`com.hbm.entity.projectile.EntityBullet`** — already the explicit subject of
  `docs/phase4/entities_legacy_bullet_system.md`, which already names `ExplosionChaos.java:643,646`
  (this report's `tauMeSinPi` method) as one of its own real consumers. Not re-scoped here; this
  report only confirms the one additional call site.
- **`com.hbm.entity.particle.EntityModFXShadow`, `com.hbm.packet.toclient.AuxParticlePacketNT`,
  `com.hbm.particle.helper.HbmEffectNT`, `com.hbm.handler.threading.PacketThreading`** — confirmed
  absent from this port (no matching files found anywhere in `src/`). Block only
  `ExplosionChaos.spawnChlorine`/`spawnVolley`'s particle/VFX halves — both methods have zero gameplay
  effect beyond visuals, so they degrade to safe no-ops until Phase 5's particle/custom-packet layer
  exists, matching the precedent already established for `ExplosionLarge`'s own particle helpers (see
  Key design decisions).
- **`com.hbm.util.Vec3NT`/`MutableVec3d`** (CE utility classes, 483 and ~200+ lines respectively, not
  read in full — only the specific methods `EntityBlackHole` actually calls were traced:
  `createVectorHelper`, `normalize`, `length`, `rotateYawSelf`) — not a real blocker. `rotateYawSelf`'s
  full formula was extracted directly (see Key design decisions) and is four lines of vanilla-`Vec3`-
  compatible math; porting the entire 483-line utility class for one rotation call is unnecessary
  scope creep this report explicitly declines to recommend.
- **`ItemGlitch`'s full 31-case effect table** — already explicitly deferred wholesale by
  `docs/phase1/items_special.md` ("implement/port the effect table incrementally... spanning every
  future phase"); this report only names the 2 real calls into this package's own scope (case 27's
  `EntityVortex(2.5F)` spawn, and a `burn(world, null, pos, 5)` call) as data for whoever eventually
  works through that table, not a claim on the item itself.
- **`EntityModBeam`** (the CE class backing `gun_b93`'s real 10-mode explosion switch) — this port has
  **already decided not to port this CE class 1:1** (`LegacyChargeWeapons.java`'s own javadoc: reused
  `EntityBulletBaseMK4`/`BulletConfig` instead of inventing a bespoke entity). This report does not
  reverse that decision; it only supplies the exact mode-4-through-8 spawn parameters (Headline
  finding 5) that whoever eventually finishes `LegacyChargeWeapons.explodeB93`'s mode branches needs
  once `EntityVortex`/`EntityRagingVortex`/`EntityBlackHole` exist.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and
Neo Edition's parallel effect-entity port for NeoForge API shape — no NeoForge API is invented below):

- **`DataParameter<Float>`/`EntityDataManager.createKey` → `EntityDataAccessor<Float>`/
  `SynchedEntityData.defineId`, confirmed real** by this port's own `EntityRubble.java` (already
  committed) and independently by Neo Edition's `BlackHole.java`/`Vortex.java` (both read in full).
  `defineSynchedData(SynchedEntityData.Builder)` replaces `entityInit()`; `entityData.get(...)`/
  `.set(...)` replace `dataManager.get(...)`/`.set(...)`. `SIZE` (all 4 entities) is the one accessor
  needed; `EntityVortex`'s own `shrinkRate` field does **not** need to be a synced accessor at all
  (CE never reads it client-side — it's read only inside `onUpdate`'s server-relevant shrink
  calculation, though see Open questions for why that read is currently dead) but Neo Edition's
  `Vortex.java` makes it one anyway (`SHRINK_RATE` as a second `EntityDataAccessor<Float>`) — a
  reasonable, low-cost simplification (one fewer NBT-only field to hand-roll) worth following for API
  consistency, not because CE requires network sync here.
- **`this.noClip = true` + `this.ignoreFrustumCheck = true` → `this.noPhysics = true` +
  `this.noCulling = true`, confirmed real** by Neo Edition's `BlackHole` constructor (both fields set
  identically to how this port's own `EntityMovingConveyorObject.java` already uses `noPhysics = true`
  elsewhere). `this.isImmuneToFire = true` on the CE side is Neo Edition's `.fireImmune()` call at
  `EntityType.Builder` registration time instead of an instance field (confirmed by
  `NtmEntityTypes.java`'s registration block for all 4 of its equivalent types) — this port's own
  `EffectEntityTypes.java` (already committed, read in full) uses the identical `.fireImmune()`
  builder call for its sibling `entity.effect` registrations (`EntityNukeTorex`, `EntityCloudFleija`,
  etc.), so this family's own registration should follow that exact file's template, not invent a new
  one.
- **`Entity#isImmuneToExplosions()` (1.12) → `Entity#ignoreExplosion(Explosion)` (1.21.1), confirmed
  real** by Neo Edition's `RagingVortex.java` (`@Override public boolean ignoreExplosion(Explosion
  explosion) { return true; }`). **CE puts this override on the base `EntityBlackHole` class**, so
  `EntityVortex`/`EntityRagingVortex`/`EntityQuasar` all inherit it — **Neo Edition's own port only
  applies it to `RagingVortex`, not to the base `BlackHole` class**, which looks like an oversight in
  that parallel port (per this task's ground rules, Neo Edition is not a source of behavior — this is
  exactly the kind of place to follow CE's real placement, not copy Neo Edition's gap). Port this
  override onto the base class matching CE, not onto only one subclass.
- **`EntityDamageSource`'s "absolute damage, bypasses armor" flags are not per-instance `DamageSource`
  method calls in 1.21 — they're datapack tags**, and this port's `ModDamageTypes.BLACK_HOLE` **already
  carries the correct tag**: confirmed by reading `src/main/java/com/hbm/damage/datagen/
  ModDamageTypeTagsProvider.java`, `BLACK_HOLE` is already listed under `this.tag(DamageTypeTags.
  BYPASSES_ARMOR)`. `entity.attackEntityFrom(ModDamageSource.blackhole, 1000.0F)` becomes
  `entity.hurt(level.damageSources().source(ModDamageTypes.BLACK_HOLE), 1000.0F)` — **use CE's real
  `1000.0F` literal**, not Neo Edition's `Float.MAX_VALUE` substitution (`BlackHole.java` line 111) —
  that is an undocumented behavior change in the parallel port, not an API-shape difference, and this
  task's ground rules require CE be the sole source of the actual number.
- **`world.createExplosion(entity, x, y, z, power, boolean blockDamage)` → `level.explode(entity, x,
  y, z, power, Level.ExplosionInteraction)`, confirmed real** by Neo Edition's `BlackHole.java`
  (`level.explode(null, ..., 5F, Level.ExplosionInteraction.BLOCK)`) and `RagingVortex.java`
  (`Level.ExplosionInteraction.BLOCK` for the 1-in-100 pulse explosion too — note CE's own
  `EntityRagingVortex` call passes `false` for block damage on that one, i.e. **CE's real behavior is
  `Level.ExplosionInteraction.NONE` for the raging vortex's periodic explosion**, not `.BLOCK` as Neo
  Edition wrote it — another place to follow CE's real flag, not Neo Edition's substitution). The
  antimatter-consuming black-hole-destruction explosion (CE: `true` for block damage) correctly maps
  to `.BLOCK`.
- **`Vec3d.rotateYawSelf(float yaw)`'s exact formula** (from CE's `MutableVec3d`, the base class
  `Vec3NT` extends, read directly rather than assumed): `nx = x*cos(yaw) + z*sin(yaw); nz =
  z*cos(yaw) - x*sin(yaw); y unchanged` — a standard rotation-about-the-Y-axis. This is trivially
  reproducible against vanilla `net.minecraft.world.phys.Vec3` (which has its own `.yRot(float)`
  method doing the same rotation — confirmed by Neo Edition's `BlackHole.java` using
  `toEntity.yRot((float) Math.toRadians(15))` directly) — **use vanilla `Vec3#yRot`, not a hand-rolled
  copy of CE's formula**, since they are mathematically identical and `yRot` is already a real,
  available vanilla method.
- **`getEntitiesWithinAABBExcludingEntity(this, aabb)` → `Level#getEntities(this, aabb)` (the
  `Entity`-overload, which excludes the passed entity itself), not `Level#getEntities(null, aabb)`
  plus a manual self-filter.** Neo Edition's `BlackHole.java` uses the `null`-plus-manual-`instanceof
  BlackHole`-skip pattern; while functionally equivalent for this specific case (the black hole is
  always also an instance of the class being filtered), passing `this` directly is the more literal
  translation of CE's actual API call and avoids relying on the coincidence that "skip other black
  holes" happens to also skip self.
- **This family's 4 entities need 4 separate `EntityType` registrations**, one per concrete class
  (`EntityBlackHole`, `EntityVortex`, `EntityRagingVortex`, `EntityQuasar`), even though 3 of the 4
  add zero or minimal behavioral difference over the base class — confirmed as the right call by
  Neo Edition's own `NtmEntityTypes.java`, which registers all 4 as fully independent
  `DeferredHolder<EntityType<?>, EntityType<...>>` entries (in fact 4 distinct type *names* even
  though `BLACK_HOLE` and `DIGAMMA_QUASAR` both construct the literal same `BlackHole` class — Neo
  Edition folded `EntityQuasar` into `BlackHole` rather than keeping it a separate class; this report
  does **not** recommend copying that fold, since CE keeps `EntityQuasar` as its own class with its
  own registry name for a reason — save-file entity-type identifiers are exactly the kind of thing
  this port's own established convention, per every other `*EntityTypes.java` file surveyed
  elsewhere in this port, treats as "one class = one registered type," and a data-driven digamma-tier
  variant folded into `EntityBlackHole` itself, if desired, could be expressed as a constructor
  parameter without touching the registry shape at all). Recommend a new `com.hbm.entity.effect.
  GravityWellEntityTypes` (or folding into the existing `EffectEntityTypes.java` if that file's owner
  prefers one shared registration file per package, which its own javadoc leaves open) following
  `EffectEntityTypes.java`'s exact template: `EntityType.Builder.of(Ctor::new,
  MobCategory.MISC).noSummon().fireImmune().sized(1F, 1F).setTrackingRange(1000)` (CE's own
  `@AutoRegister(trackingRange = 1000)` on all 4 classes, matching `EffectEntityTypes.java`'s own
  1000-tracking-range siblings, not Neo Edition's smaller `250`).
- **`ExplosionChaos.forEachBlockInSphere`'s hardcoded `y ∈ [0, 255]` clamp needs a 1.21-world-height-
  aware replacement** (`level.getMinBuildHeight()`/`getMaxBuildHeight()`, or the modern
  `Level#isOutsideBuildHeight(BlockPos)` check) rather than the literal `0`/`255` CE hardcodes — this
  port's own established convention elsewhere (not verified in this specific survey's file set, but
  consistent with how every other sphere/AABB-scanning method this port has already ported handles
  world height) should be followed rather than reproducing CE's Overworld-only assumption verbatim.
- **`EntityRubble`'s already-real constructor/setter shape is the one to call from both
  `EntityBlackHole`'s block-destruction loop and `ExplosionChaos.levelDown`**: `new
  EntityRubble(level, x, y, z)` (position-only constructor, already exists) followed by
  `.setBlockState(state)` (not CE's `setMetaBasedOnBlock(Block, int)` — this port's `EntityRubble`
  already made the metadata-flattening call, storing a full `BlockState` via a single synced int
  palette id, see that class's own javadoc). No new `EntityRubble` API needed for either call site.

## Open questions / risks

- **`EntityVortex.setShrinkRate(float)` is real, reachable, public API with exactly one real CE call
  site (`ItemConserve`'s `FoodType.BHOLE` branch: `.setShrinkRate(0.01F)`) — but `EntityVortex.
  onUpdate()`'s actual shrink line reads a hardcoded `0.0025F` literal, never `this.shrinkRate`.**
  Confirmed by reading both files directly, not assumed. This means CE's real behavior today is: every
  vortex, however it's spawned and whatever `setShrinkRate` value it's given, shrinks at the exact
  same fixed rate — the field is set, persisted to NBT across save/load, and exposed via a public
  fluent setter, but has **zero** effect on gameplay. This is the same "preserve CE's real, even
  buggy, behavior vs. fix it" fork RBMK's and the gun-framework's own reports flagged for their
  respective shared-state quirks — flagged here rather than resolved. Recommend preserving the bug
  (hardcode `0.0025F` in the port too) for byte-for-byte parity, with a code comment pointing at this
  report, unless the port's overall philosophy has since shifted toward fixing confirmed CE bugs
  outright — that call belongs to whoever implements this package, not this research pass.
- **`ExplosionChaos.pDestruction` (the `pulse` method's only helper) appears to have a genuine block-
  duplication bug**: it spawns a real `EntityFallingBlock` carrying the target position's current
  `BlockState` but never clears that position to air, unlike every other block-destruction method in
  this same file (`explode`/`explodeZOMG`/`EntityBlackHole`'s own loop/`levelDown` all explicitly
  clear the source block after spawning their debris entity). Not confirmed as observable in actual
  CE gameplay (this survey did not run the game), and `pulse`/`pDestruction` are not consumed by any
  real call site this survey found (see Table B) — low real-world stakes today, but flagged loudly so
  a future implementer doesn't assume the omission was deliberate and copy it without checking whether
  it produces visible block duplication once `pulse` actually gets a caller.
- **`EntityRagingVortex.onUpdate()`'s double `isWarDim` check** (once via its own explicit re-check,
  once via the `super.onUpdate()` call it makes at the end of its own method body) — confirmed by
  reading both methods directly. Harmless once the always-true stub from Headline finding 3 is
  applied (both checks trivially pass), but worth noting so a future refactor doesn't "simplify" it
  into a single check and then need to re-verify nothing depended on the redundant one running twice
  (nothing does, per this survey's read — the second check inside `super.onUpdate()` runs
  unconditionally regardless of what the first one decided, since the first one doesn't return early
  on the *true* branch, only on the *false* one).
- **`isWarDim`'s per-method asymmetry across `ExplosionChaos` (Headline finding 4) must be preserved
  exactly as CE has it, method by method** — the temptation to "clean up" this file by applying one
  uniform gate (or removing all gates uniformly, reasoning "they're all always-true anyway now") would
  silently change real behavior the moment a server operator ever does configure a peace dimension in
  the future dimension-re-keying work this defers to. This report recommends implementing each
  method's gate (present or absent) exactly as Table B lists it, not as a single class-wide policy.
- **This report found 5 real, non-`ExplosionChaos` consumer files for the entity family
  (`ItemDrop`, `ItemDigamma`, `ItemConserve`, `ItemGlitch`, `LegacyChargeWeapons`/`EntityModBeam`),
  three of which are already-ported, already-compiling code in this repo with a live, documented
  stub waiting on this exact package.** The task's own framing named only `ItemDrop`. Recommend
  whoever implements this package explicitly re-reads and updates all three already-committed stub
  sites (`ItemDigamma.onEntityItemUpdate`, `ItemConserve.finishUsingItem`'s `BHOLE` case,
  `LegacyChargeWeapons.explodeB93`'s mode 4–8 branches) rather than treating `ItemDrop` as the only
  wiring job — this report supplies the exact spawn parameters for all of them (Headline finding 5,
  Table A).
- **`EntityQuasar`'s design value is genuinely marginal** (Open question, not a defect): its entire
  class body beyond the two constructors is `onUpdate() { super.onUpdate(); }` — a no-op override.
  Whether to actually port it as a fourth distinct `Entity` subclass (matching CE's real class
  structure and this port's own "one CE class = one port class" convention) or fold it into
  `EntityBlackHole` as a named factory/constant (`EntityBlackHole.spawnQuasar(level, x, y, z)`)
  is a real implementation-time design call this report flags but does not resolve — Key design
  decisions above recommends the former (matching CE structure, distinct registry entry) for save-
  compatibility and convention-consistency reasons, but the latter is not unreasonable given the
  class's near-total lack of independent behavior.
