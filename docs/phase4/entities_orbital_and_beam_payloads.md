# Incendiary/coin/mist/airdrop payload entities — Phase 4 research

Four already-named forward references from Phase 3's weapons work: `EntityFireLingering`,
`EntityCoin`, `EntityMist`, `EntityC130`. All four are real, small-to-medium CE entity classes with
no port yet anywhere in this tree (confirmed by grep before reading).

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/effect/EntityFireLingering.java` (112 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/effect/EntityMist.java` (368 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityCoin.java` (49 lines) — **note the
  real package**, see Headline finding #1
- `upstream/hbm-ce/src/main/java/com/hbm/entity/logic/EntityC130.java` (101 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/logic/EntityPlaneBase.java` (241 lines) —
  `EntityC130`'s direct abstract base, a real blocking dependency, read in full to scope it precisely
- `upstream/hbm-ce/src/main/java/com/hbm/entity/item/EntityParachuteCrate.java` (76 lines) — C130's
  payload-drop entity, read in full
- `upstream/hbm-ce/src/main/java/com/hbm/itempool/{ItemPool,ItemPoolsC130}.java` (104 + 59 lines) —
  the loot-pool framework and C130's two concrete pools, read in full
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/impl/ItemGunNI4NI.java` (120 lines) — the
  one gun that actually throws a coin, read in full
- `upstream/hbm-ce/src/main/java/com/hbm/render/entity/projectile/RenderCoin.java` (44 lines, client,
  confirms Phase 5 scope only)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityBulletBeamBase.java` (373 lines) —
  re-read in full specifically for the "coin flip" special case in `performHitscan` (lines 163–303)
- `upstream/hbm-ce/src/main/java/com/hbm/util/TrackerUtil.java` (54 lines, full) and
  `.../world/WorldUtil.java` (`loadAndSpawnEntityInWorld`, partial) — C130's spawn-plumbing helpers
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/sedna/factory/{XFactoryFlamer,XFactoryEnergy,
  XFactoryRocket,XFactory35800,XFactoryAccelerator,XFactory40mm}.java` — every real
  `EntityFireLingering`/`EntityCoin`/`EntityC130` call site grepped and read in context
  (`spawnFire`/`igniteIfPossible`/`spawnPlane`/`LAMBDA_NI4NI_SECONDARY_PRESS`)
- Repo-wide greps for `coin`/`Coin` across `com.hbm.entity.mob.*` to rule out confusion with CE's
  unrelated `ModItems.coin_maskman/coin_creeper/coin_radiation/coin_ufo/coin_worm` trophy-item family
  (see Headline finding #1)

This port's own already-committed code, read in full to confirm every design decision below against
real, compiling infrastructure (never invented):
- `src/main/java/com/hbm/entity/projectile/{EntityThrowableNT (422 lines), EntityThrowableInterp (99
  lines), EntityBulletBeamBase (281 lines)}` — the exact base classes/hitscan loop `EntityCoin` and
  its beam special-case build on
- `src/main/java/com/hbm/entity/logic/IChunkLoader.java` (76 lines) and
  `.../entity/missile/EntityMissileBaseNT.java`/`EntityMissileTier4.java` (chunk-loading/spawn-helper
  usage sites) — the already-solved 1.21.1 answer to CE's `ForgeChunkManager`/`WorldUtil`/
  `TrackerUtil` plumbing `EntityPlaneBase`/`EntityC130` need
- `src/main/java/com/hbm/entity/{ConveyorEntityTypes,GunEntityTypes}.java` and
  `.../entity/effect/EffectEntityTypes.java` — the confirmed real per-family
  `DeferredRegister<EntityType<?>>` registration pattern (ConveyorEntityTypes-pattern)
- `src/main/java/com/hbm/inventory/fluid/trait/{FT_Corrosive,FT_Poison,FT_Toxin,FT_Pheromone,
  FT_VentRadiation,FT_Flammable,FluidTraitSimple}.java`, `.../inventory/fluid/{FluidType,Fluids}.java`
  — cross-checked field-by-field against every branch `EntityMist.affect()` needs
- `src/main/java/com/hbm/damage/ModDamageTypes.java`, `.../util/ContaminationUtil.java`,
  `.../handler/ArmorUtil.java`, `.../capability/HbmLivingAttachment.java` — signatures confirmed for
  every helper `EntityMist`/`EntityFireLingering` call
- `src/main/java/com/hbm/blocks/generic/BlockSupplyCrate.java` (164 lines, full) — the landing target
  `EntityParachuteCrate` hands its cargo to
- `src/main/java/com/hbm/items/weapon/sedna/content/{XFactory40mm,XFactoryAccelerator,XFactoryEnergy,
  XFactoryFlamer,XFactoryRocket,XFactory35800}.java`,
  `.../items/weapon/grenade/GrenadeFillingActions.java`,
  `.../entity/grenade/EntityDisperserCanister.java`,
  `.../blockentity/turret/TurretFritzBlockEntity.java` — every already-committed forward-reference
  TODO/javadoc naming one of these 4 entities (10 files total, see the table below)
- `docs/phase3/{grenades.md, gun_framework.md}` and `docs/phase4/satellites_followup_and_loot_pools.md`
  — cross-referenced for prior research on the same entities/dependencies (see Headline findings #2
  and #4); not re-derived where those reports already settled a question, but independently
  re-verified against the real source rather than taken on faith
- `upstream/neo-edition` — grepped for `EntityType.Builder`/chunk-loading shapes; nothing in Neo
  Edition covers any of these 4 specific entities, so it contributed no direct precedent here beyond
  what this port's own `ConveyorEntityTypes`/`IChunkLoader` already established

## Headline finding

Five corrections to this task's own framing and to assumptions already baked into prior phases'
committed code:

1. **`EntityCoin` is `com.hbm.entity.projectile.EntityCoin`, not `com.hbm.entity.item.EntityCoin`
   — and it isn't a "currency" entity at all.** Both `XFactoryAccelerator.java`'s and
   `EntityBulletBeamBase.java`'s own already-committed forward-reference comments guess the wrong
   package (`com.hbm.entity.item.EntityCoin`); a repo-wide grep of `upstream/hbm-ce` finds exactly one
   `class EntityCoin`, in `com.hbm.entity.projectile`. It is also not a themed trophy/collectible
   item — CE has a wholly separate, unrelated family of those (`ModItems.coin_maskman`/`coin_creeper`/
   `coin_radiation`/`coin_ufo`/`coin_worm`, plain `ItemStack` boss-kill drops from `EntityMaskMan`/
   `EntityCreeperNuclear`/`EntityRADBeast`/`EntityUFO`/`EntityBOTPrimeHead` — none of them construct
   or reference `EntityCoin` in any way). `EntityCoin` itself is a live, physical, thrown *projectile*
   fired by exactly one gun (`gun_n_i_4_n_i`, backed by `ItemGunNI4NI`): a spinning coin with real
   gravity/collision that a beam weapon can strike mid-air to trigger a "coin flip" — the coin is
   destroyed and a *second* beam (1.25× damage) is relayed from the coin's position toward whichever
   entity is nearest, by a fixed priority order (another coin > player > hostile mob > any other
   living entity), crediting the new beam to the *coin's* thrower, not the original beam's shooter.
   This is a trick-shot ricochet mechanic, not a target dummy or a currency drop.
2. **`EntityFireLingering` is real gameplay logic, not "Phase 5 client-VFX scope"** — despite
   `GrenadeFillingActions.java`'s own already-committed javadoc grouping it there. Its client tick
   (`FlameCreator.composeEffectClient`) is indeed decorative, but its **server tick is the actual
   area-denial mechanic**: every tick, for up to 600 ticks, it scans its own (width×height) bounding
   box and sets `HbmLivingProps.setFire(60|300)`/`setBalefire(100)` on every living entity inside —
   real, ongoing burn damage with no dependency on rendering, particles, or any Phase 5 system to
   function. `docs/phase3/grenades.md` itself already researched this correctly (it separately calls
   out the entity's real server logic and only defers the `Haze`/`Tower` *particle broadcasts* to
   Phase 5); the "Phase 5 client-VFX scope" phrase in the later `GrenadeFillingActions`/
   `XFactoryEnergy`/`XFactoryFlamer`/`XFactoryRocket`/`XFactory35800` javadocs over-generalizes that
   into deferring the whole entity. It should be ported now — doing so closes out 7 already-committed
   forward-reference TODOs across 7 files in one pass (table below).
3. **`EntityMist` is already ~95% unblocked** — every `FluidTrait` class its `affect()` method reads
   (`FT_Corrosive`, `FT_Poison`, `FT_Toxin`, `FT_Pheromone`, `FT_VentRadiation`, `FT_Flammable`) is
   already real in this port and API-identical to CE's own (confirmed field-by-field below). Only two
   narrow sub-branches are genuinely blocked, by two *different* systems, not by "world simulation"
   generally: (a) the radiation branch (`FT_VentRadiation.getRadPerMB()` → `ChunkRadiationManager`,
   and the parallel `ContaminationUtil.contaminate(...)` call) needs `ChunkRadiationManager`, which is
   a distinct, already-named sibling Phase 4 area (`docs/phase4/chunk_radiation_system.md`) — and this
   exact forward reference is **already sitting uncommented, live, in this port's own committed
   `FT_VentRadiation.java`** (`ChunkRadiationManager.proxy.incrementRad(level, pos, ...)`), which does
   not compile today (see Open questions); (b) the pheromone trait's glyphid half
   (`instanceof EntityGlyphid`) needs a wholly separate, entirely unstarted 8-class mob line
   (`com.hbm.entity.mob.glyphid.*`) not owned by any named Phase 4 sub-area — the *player*-targeted
   half of the same trait (`instanceof EntityPlayer && pheromone.getType() == 2`, a buff-potion
   effect) has zero missing dependencies and works today.
4. **`EntityC130` cannot be ported alone — it is one of two concrete subclasses of the 241-line
   abstract `EntityPlaneBase`** (the other, `EntityBomber`, is out of this report's scope but becomes
   trivially buildable once `EntityPlaneBase` lands — see Deferred scope). Its payload-drop half also
   needs `EntityParachuteCrate` (76 lines) and the still-unported `com.hbm.itempool` framework's
   `ItemPoolsC130` — which needs `ItemPool`'s exact 3-method contract
   (`getPool`/`getStack`/`pools`) that `docs/phase4/satellites_followup_and_loot_pools.md` **already
   fully specified** (for its own `ItemPoolsSatellite`) but has not yet been implemented. That report
   explicitly left open whether other, unread pool files still need CE's metadata (`meta`) parameter;
   this report's answer, from actually reading `ItemPoolsC130`, is **no** — its ammo pool references
   CE's legacy metadata-subtype `ammo_standard`+`GunFactory.EnumAmmo` item, which this port's own
   Sedna gun-content classes (`XFactory357`, `XFactory44`, `XFactory9mm`, `XFactory762mm`,
   `XFactory12ga`, `XFactoryRocket`) have already fully replaced with one discrete real `Item` per
   round. Every item `ItemPoolsC130` needs — ammo included — is confirmed present in this port by
   name (full list in the scope table); the pool just needs re-keying to those real items, not a
   metadata concept.
5. **C130's two CE spawn-plumbing helpers have no 1.21.1 equivalent, but this port has already solved
   the identical problem for missiles.** `WorldUtil.loadAndSpawnEntityInWorld` (a manual 5×5
   chunk-preload loop) and `TrackerUtil.setTrackingRange` (reaching into 1.12's private
   `EntityTrackerEntry.setMaxRange`) are both 1.12-Forge-only. `EntityMissileTier4`'s own
   already-committed javadoc documents replacing the first with a plain `level.addFreshEntity(...)`
   once the spawned entity self-chunk-loads via the already-real `IChunkLoader` interface (used today
   by `EntityMissileBaseNT`/`EntityMIRV`/`EntityMissileAntiBallistic`) — `EntityC130` should do the
   same. The second has no replacement and can simply be dropped: it only ever *narrows* C130's
   tracking range to 250, which is already smaller than the 1000 its own CE `@AutoRegister` sets at
   registration, so omitting the runtime override leaves C130 strictly *more* visible at range, not
   less — a supersede, not a regression.

## Phase-4-safe scope

| Entity | CE lines | Real blocking deps (all confirmed present in this port unless noted) | Already-committed forward-reference call sites |
|---|---|---|---|
| `EntityFireLingering` | 112 (read in full) | `HbmLivingAttachment.getFire/setFire/getBalefire/setBalefire` (real, exact match) — nothing else needed for the server half | `GrenadeFillingActions` (INC/WP), `XFactory40mm` (`g40_inc`/`g40_phosphorus`), `XFactoryEnergy`, `XFactoryFlamer` (4 lambdas: `LAMBDA_LINGER_DIESEL/NAPALM/BALEFIRE` + 3 more `onImpact` bodies), `XFactoryRocket`, `XFactory35800`, `TurretFritzBlockEntity` — **7 files** |
| `EntityMist` | 368 (read in full) | `FluidType`/`Fluids`/6 `FluidTrait` classes (real, exact match); `ArmorUtil.damageSuit` (real, signature differs — see Key design decisions); `ContaminationUtil.contaminate` (real, exact match); `ChunkRadiationManager` (**not real yet** — sibling Phase 4 area); `EntityGlyphid` (**not real yet** — unclaimed mob line, blocks 1 of ~10 branches only) | `EntityDisperserCanister.explode()` (its only spawn site in this port's tree) |
| `EntityCoin` | 49 (read in full) | `EntityThrowableInterp`/`EntityThrowableNT` (real, exact override-point match) | `EntityBulletBeamBase.performHitscan()` (the "coin flip" TODO), `XFactoryAccelerator`'s `gun_n_i_4_n_i` secondary-press TODO |
| `EntityC130` | 101 (read in full) | `EntityPlaneBase` (**not real yet**, 241 lines, this report's own read) + `IChunkLoader` (real, reuse) for the plane half; `EntityParachuteCrate` (**not real yet**, 76 lines) + `BlockSupplyCrate`/`SupplyCrateBlockEntity` (real, exact `items` field match) for the payload half; `com.hbm.itempool.ItemPool` (**not real yet**, owned by satellites_followup_and_loot_pools.md) + `ItemPoolsC130` (**not real yet**, this report's own read) for the loot half | `XFactory40mm`'s `g26_flare_supply`/`g26_flare_weapon` — via `.setOnUpdate(...)`, **not** `.setOnImpact(...)` (see Key design decisions) |

`EntityFireLingering` and `EntityCoin` are genuinely Phase-4-safe today — every non-CE dependency
either already exists in this port or needs nothing beyond it. `EntityMist` is Phase-4-safe modulo
two narrow, separately-owned branches (stub them, port everything else). `EntityC130` is Phase-4-safe
only as a *bundle* with `EntityPlaneBase`/`EntityParachuteCrate`/`ItemPoolsC130` — none of those three
are one of this task's 4 named entities, but none of them is claimed by any other report either, and
`EntityC130` is inert without them.

### `ItemPoolsC130`'s two pools, re-keyed against this port's real items (all confirmed present)

- `POOL_SUPPLIES` (8 entries): `definitelyfood`, `syringe_metal_stimpak`, `pill_iodine`,
  `canister_full` (Diesel), `machine_diesel`, `geiger_counter`, `med_bag`, `radaway` — every name
  confirmed present in `ModItems`/`ModBlocks`.
- `POOL_WEAPONS` (8 entries): `gun_light_revolver`, `gun_henry`, `gun_maresleg`, `gun_greasegun`,
  `gun_carbine`, `gun_heavy_revolver`, `gun_panzerschreck`, `gun_double_barrel` — all present.
- `POOL_AMMO` (9 entries, CE keys every one off `ammo_standard` + a `GunFactory.EnumAmmo` ordinal):
  re-key to `XFactory357.ITEM_M357_SP`/`ITEM_M357_FMJ`, `XFactory44.ITEM_M44_SP`/`ITEM_M44_FMJ`,
  `XFactory9mm.ITEM_P9_SP`/`ITEM_P9_FMJ`, `XFactory762mm.ITEM_R762_SP`, `XFactory12ga.ITEM_G12_BP`,
  `XFactoryRocket.ITEM_ROCKET_HE` — all 8 distinct items confirmed present; the metadata-subtype
  concept itself (`GunFactory.EnumAmmo` as a class) does not exist in this port and should not be
  reintroduced.

## Deferred scope

- **`com.hbm.handler.radiation.ChunkRadiationManager`** — owned by
  `docs/phase4/chunk_radiation_system.md`. Blocks `EntityMist`'s `FT_VentRadiation` branch (both the
  `onEntityUpdate` ambient-increment call and the `affect()` per-entity `ContaminationUtil.contaminate`
  call, which itself is already real and simply needs `ChunkRadiationManager`'s ambient-radiation
  query per `ContaminationUtil`'s own documented TODO). Recommend porting `EntityMist` with this one
  call left wired exactly as `FT_VentRadiation.java` already has it (see Open questions) rather than
  inventing a second, TODO-stubbed convention for the same not-yet-real class.
- **The Glyphid mob line** (`com.hbm.entity.mob.glyphid.*` — `EntityGlyphid`,
  `EntityGlyphidBehemoth/Digger/Brawler/Nuclear/Scout/Blaster/Brenda/Bombardier`, plus
  `com.hbm.world.feature.GlyphidHive` worldgen; only `BlockGlyphid`/`BlockGlyphidSpawner` are ported so
  far, blocks only) — not owned by any currently-named Phase 4 sub-area. Blocks exactly one `instanceof`
  branch inside `EntityMist.affect()`'s pheromone trait (the glyphid-aggro half); drop that one branch
  (the class literally cannot be referenced without existing) and keep the player-buff half, which has
  no dependency on it.
- **`com.hbm.itempool.ItemPool`** (the 104-line base loot-pool framework: `pools` static map,
  `getPool(name)`, `getStack(pool, rand)`) — owned by `docs/phase4/satellites_followup_and_loot_pools.md`,
  which already fully specified its real 3-method port contract but has not yet implemented it. This
  report's `ItemPoolsC130` needs the exact same contract; sequence `EntityC130`'s loot half after
  (or alongside) that report's own implementation, not as a second parallel design.
- **`ItemGunNI4NI`'s per-stack coin economy** (`getCoinCount`/`setCoinCount`/`getCoinCharge`/
  `setCoinCharge` NBT fields, the `onUpdate` tick-based charge accumulator, and the
  `XWeaponModManager.ID_NI4NI_NICKEL`/`ID_NI4NI_DOUBLOONS` upgrade checks that raise the max coin
  count) — a bespoke `ItemGunBaseNT` subclass that does not exist in this port (`gun_n_i_4_n_i` is
  currently registered as a plain `ItemGunBaseNT` in `XFactoryAccelerator.gun_n_i_4_n_i()`). Porting
  `EntityCoin` (this report) only unblocks the *entity* + *beam-relay* half of the forward reference;
  the "can this gun currently throw a coin at all" half still needs `ItemGunNI4NI` itself, which is
  content/item scope (`XFactoryAccelerator`'s own follow-up), not entity scope.
- **`Block#isFlammable(IBlockAccess, BlockPos, EnumFacing)`'s "ignite an adjacent flammable block"
  branch** (CE's `igniteIfPossible` in `XFactoryFlamer`, `igniteAround` in `GrenadeFillingActions`,
  and the 3×3×3 loop in `XFactory40mm`'s `g40_inc`/`g40_phosphorus` TODO) — no single confirmed 1.21.1
  replacement, per the identical already-documented gap in `GrenadeFillingActions`'s own javadoc and
  `ExplosionNukeGeneric#vaporDest`. Drop per that established precedent; the `EntityFireLingering`
  spawn half of each of these call sites is unaffected and should still be wired.
- **All client rendering/VFX**: `RenderCoin`'s model/texture binding, `FlameCreator.composeEffectClient`
  (`EntityFireLingering`'s client tick), the `Tower`/`Haze`/`VanillaExt_LargeExplode`
  `AuxParticlePacketNT`/`HbmEffectNT` broadcasts (`EntityMist`'s client tick, WP's 3 Haze broadcasts,
  the coin-flip's explosion VFX packet) — all fold into the same generic VFX dispatch table
  `docs/phase3/weapon_animation_hooks.md` and `docs/phase3/grenades.md` already deferred to Phase 5.
- **`EntityBomber`** (`EntityPlaneBase`'s other real CE consumer, a bombing-run plane) — not read in
  detail here (out of this task's named scope), but flagged so it is not lost: once `EntityPlaneBase`
  lands for `EntityC130`, `EntityBomber` becomes a small, mostly-mechanical follow-up for whichever
  future area wants it. Currently unclaimed by any named Phase 4 sub-area.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own already-committed
code for NeoForge API shape; Neo Edition consulted but contributed nothing beyond what this port
already established for these 4 entities specifically):

- **Entity registration** follows the confirmed real `ConveyorEntityTypes` pattern
  (`DeferredRegister<EntityType<?>>` on `BuiltInRegistries.ENTITY_TYPE`,
  `EntityType.Builder.of(ctor, MobCategory.MISC).noSummon().sized(w,h).setTrackingRange(n).build(name)`,
  `.fireImmune()` where CE sets `isImmuneToFire = true` at construction). Concretely:
  - `EntityFireLingering` and `EntityMist` land in the **already-existing**
    `com.hbm.entity.effect.EffectEntityTypes` (currently registering `EntityNukeTorex`/
    `EntityCloudFleija`/`EntityCloudSolinium`/`EntityEMPBlast`) — same package, same file, just two
    more `DeferredHolder` fields and two more `.register(...)` blocks. No new registry class needed.
  - `EntityCoin` is gun-adjacent but not part of `GunEntityTypes`'s own scoped "ballistics core"
    (Package A); either extending `GunEntityTypes` or standing up a small new
    `com.hbm.entity.projectile`-local registry are both valid per `ConveyorEntityTypes`'s own
    "left for whoever lands next" precedent — flagged as an open call, not resolved (see Open
    questions).
  - `EntityC130` (and `EntityPlaneBase`, and eventually `EntityBomber`) belong in
    `com.hbm.entity.logic`, which already has `NukeEntityTypes` — either extend it or add a sibling
    `PlaneEntityTypes`, same open call as above.
- **`EntityCoin` builds directly on this port's already-real `EntityThrowableInterp` →
  `EntityThrowableNT`.** Every CE override it needs already has a confirmed, exact 1.21.1 landing
  spot: `getAirDrag()` (CE: `1F`), `getGravityVelocity()` (CE: `0.02F`), `onImpact(HitResult)` (dies
  only on a block hit — matches `onHitBlock`/`onHitEntity`'s existing dispatch). CE's
  `canBeCollidedWith()` (used both by `EntityCoin` itself and by every hit-sweep that needs to
  recognize it) maps onto vanilla `Entity#isPickable()` — already the exact gate this port's own
  `EntityThrowableNT.tick()` and `EntityBulletBeamBase.performHitscan()` use for every other entity
  (`e.isPickable()`), confirmed by direct read of both methods. Override it to return `true`.
- **The coin-flip special case slots into `EntityBulletBeamBase.performHitscan()` exactly where its
  own TODO comment already sits** (between the block-raycast and the general entity sweep, this
  port's line ~180–190): scan the same `region` this port's loop already builds for any `EntityCoin`,
  pick the nearest one the beam's segment intersects, and — if found — skip the general sweep,
  destroy the coin, and spawn a relay beam. The relay beam itself needs no new machinery: this port's
  own `EntityBulletBeamBase(LivingEntity, BulletConfig, float)` constructor plus its existing
  `performHitscanExternal(double range)` already do exactly what CE's relay-beam construction does
  (aim at a computed target, then re-run the hitscan at a fixed range) — reuse both verbatim.
- **Zero new `DamageType` entries needed for `EntityMist`.** `ModDamageTypes.ACID`, `.BOIL`, and
  `.ICE` are already registered and match CE's `ModDamageSource.acid`/`s_boil`/`s_cryolator` string
  constants 1:1 (`"acid"`/`"boil"`/`"ice"`) — confirmed by direct comparison of both files. Use
  `entity.damageSources().source(ModDamageTypes.BOIL/ICE)` for the boil/freeze branches (matching
  `FT_Toxin.ToxinDirectDamage`'s own already-real call shape) and the pre-built `ModDamageTypes.ACID`
  DamageSource-equivalent for the corrosive branch.
- **`ArmorUtil.damageSuit`'s real signature takes an `EquipmentSlot`, not CE's raw `int` 0–3.** CE's
  corrosive branch loops `for(i = 0; i < 4; i++) ArmorUtil.damageSuit(player, i, ...)`; this port's
  confirmed signature is `damageSuit(LivingEntity, EquipmentSlot, int)`, so the port's equivalent loop
  is over `EquipmentSlot.FEET/LEGS/CHEST/HEAD`, not a bare index range.
- **Every other `FluidTrait` branch `EntityMist.affect()` needs is already a 1:1 API match**: confirmed
  `FT_Corrosive.getRating()`, `FT_Poison.isWithering()`/`getLevel()`, `FT_Toxin.affect(LivingEntity,
  double)`, `FT_Pheromone.getType()`, `FT_VentRadiation.getRadPerMB()`, `FluidType.hasTrait(Class)`/
  `getTrait(Class)`, `Fluids.DELICIOUS`/`ENDERJUICE`/`fromID`/`readType`/`writeType` all exist with
  identical names and semantics to what CE's `EntityMist` calls. No adaptation needed beyond the two
  blocked branches named above.
- **`EntityMist`'s `ENDERJUICE` teleport branch** — CE hand-rolls ~65 lines of copy-pasted
  Enderman-teleport logic (its own comment: "terribly copy-pasted from EntityChemical.class, whose
  method was terribly copy-pasted from EntityEnderman.class"). Recommend replacing it with vanilla's
  own public `Entity#randomTeleport(double x, double y, double z, boolean particleEffects)` (the same
  method `ChorusFruitItem` calls externally) rather than re-deriving CE's manual
  collision/fall-search loop. **Flagged as well-established Mojang-mapping knowledge, not verified
  against a real compiled jar in this sandbox** — confirm the exact signature/visibility before
  relying on it.
- **`EntityC130`'s spawn plumbing** re-derives, not transliterates, CE's helpers:
  `WorldUtil.loadAndSpawnEntityInWorld(c130)` → plain `level.addFreshEntity(c130)`, exactly the
  substitution `EntityMissileTier4`'s own javadoc already documents, relying on `EntityPlaneBase`
  implementing `IChunkLoader` (reusing the already-real interface `EntityMissileBaseNT` uses) instead
  of CE's manual 5×5 chunk-preload loop. `TrackerUtil.setTrackingRange(world, c130, 250)` has no
  1.21.1 equivalent and should be dropped entirely (see Headline finding #5).
- **`spawnPlane`'s trigger is `BulletConfig.setOnUpdate(...)`, not `setOnImpact(...)`.** CE fires the
  C130 spawn from the flare bullet's own per-tick update lambda, gated on
  `entity.ticksExisted == 40`, not from a block/entity impact. This port's `BulletConfig` already
  supports `.setOnUpdate(Consumer<Entity>)` (confirmed real, already used by
  `XFactoryAccelerator.coil_tungsten/coil_ferrouranium`'s `breakInPath` calls) — `g26_flare_supply`/
  `g26_flare_weapon` just need `.setOnUpdate(...)` added, matching this port's own already-committed
  TODO comment on exactly this point.
- **`EntityParachuteCrate`'s landing target, `BlockSupplyCrate`/`SupplyCrateBlockEntity`, is already
  real** in this port with the exact `List<ItemStack> items` field CE's
  `crate.items.addAll(this.items)` call needs — confirmed by direct read of
  `BlockSupplyCrate.SupplyCrateBlockEntity`. No new block/block-entity work needed for the landing
  half, only the falling-crate entity itself.

## Open questions / risks

- **This port's build may already not compile end-to-end, independent of this report.**
  `FT_VentRadiation.java` (committed in an earlier phase) imports and calls
  `com.hbm.handler.radiation.ChunkRadiationManager.proxy.incrementRad(...)` directly, uncommented —
  but no `ChunkRadiationManager` class exists anywhere under `src/` (confirmed by grep). This is not
  this report's bug to fix (`chunk_radiation_system.md` owns that class), but it means `EntityMist`'s
  own identical call is not introducing new debt — it's following an already-established (if
  currently broken) convention. Whoever lands `ChunkRadiationManager` should specifically check
  `FT_VentRadiation.java` is not the only site depending on it.
- **`EntityFireLingering`'s `@AutoRegister(..., sendVelocityUpdates = false)` flag** — `docs/phase3/
  grenades.md` already flagged this identical open question and found no equivalent knob in this
  port's own `EntityType.Builder` usage anywhere (`ConveyorEntityTypes`/`GunEntityTypes`/
  `EffectEntityTypes` only ever call `.noSummon()`/`.sized()`/`.setTrackingRange()`/`.fireImmune()`).
  Still unresolved after this report's own independent check; recommend dropping it as a
  bandwidth-only 1.12 micro-optimization unless a future check of `EntityType.Builder`'s full method
  list (not available in this sandbox — no compiled NeoForge jar) turns up a real equivalent.
- **`TYPE_BLACK` ("black fire") is CE's own unfinished feature, not a porting gap.** It's
  instantiable (`XFactory35800`'s `p35800_bl` legendary sidearm spawns one) but functionally inert:
  `EntityFireLingering.onEntityUpdate`'s affected-entity loop has no `if` branch for
  `TYPE_BLACK` (only `TYPE_DIESEL`/`TYPE_PHOSPHORUS`/`TYPE_BALEFIRE` apply fire/balefire), matching
  the class's own `// TODO implement black fire` comment on the constant's declaration. Preserve as
  inert — do not invent behavior CE itself never shipped for this constant.
- **`EntityCoin`'s and `EntityC130`'s exact registry-family placement is a genuinely open design
  call**, not resolved here — see Key design decisions. Flagging explicitly so a future agent doesn't
  assume one answer was already settled.
- **`ItemPool`'s `meta` parameter should very likely be dropped from the framework's whole signature,
  not just from `ItemPoolsSatellite`'s file.** `docs/phase4/satellites_followup_and_loot_pools.md`
  left this open ("the other 6 unread pool files may still need it, not resolved here"). Having now
  read `ItemPoolsC130` (one of those 6), this report's finding is that CE's `meta` there encodes
  *which discrete ammo item* (via `GunFactory.EnumAmmo.ordinal()`), a concept this port's Sedna
  gun-content already resolved by giving every round its own real `Item` — so the port's equivalent
  pool-entry constructor needs no numeric discriminator at all, just the real item reference. Worth
  confirming against the remaining 4 unread pool files before finalizing `ItemPool`'s new signature,
  but nothing found here contradicts dropping `meta` entirely.
- **The coin-flip's damage/credit attribution is a subtle, real CE behavior worth preserving exactly.**
  The relay beam is constructed with `hitCoin.getThrower()` (falling back to the original beam's
  thrower only if the coin has none) — meaning a coin thrown by player A, later struck by a beam fired
  by player B, credits the *relay* hit to player A, not B. Easy to "simplify away" by accident during
  the port; flagged so it's tested against explicitly rather than assumed to not matter.
- **`EntityBomber` is unclaimed** (Deferred scope) — worth a deliberate decision by whoever plans the
  rest of Phase 4/mob content, not a silent gap.
