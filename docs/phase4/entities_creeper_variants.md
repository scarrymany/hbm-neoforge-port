# CE creeper-variant mobs — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/EntityCreeperNuclear.java` (140 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/EntityCreeperGold.java` (60 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/EntityCreeperPhosgene.java` (48 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/EntityCreeperTainted.java` (96 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/EntityCreeperVolatile.java` (59 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/mob/EntityQuackos.java` (186 lines — read in full
  specifically to settle whether it belongs in this report; see Headline finding)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/EntityMappings.java` (54 lines — the actual natural
  biome-spawn-weight registration for 3 of the 5 variants)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/bomb/BlockTaint.java` (172 lines — the taint block's
  `onEntityCollision`, which is `EntityCreeperTainted`'s *only* spawn/creation path)
- `upstream/hbm-ce/src/main/java/com/hbm/interfaces/AutoRegister.java` (processor-module copy, ~140
  lines) plus a grep of `upstream/hbm-ce/processor/src/main/java/com/hbm/processor/
  AutoRegisterProcessor.java` (the `eggColors`/`EntityInfo`/`registerModEntity` codegen this port has
  no equivalent for and must replace with explicit `DeferredRegister` + `SpawnEggItem` calls)
- Partial reads (methods/sections directly touching the 5 variants, not the whole file):
  `upstream/hbm-ce/src/main/java/com/hbm/util/ContaminationUtil.java` (lines 55-85 `immuneEntities`
  array + `calculateRadiationMod` header; lines 370-397 `checkConfigEntityImmunity`/`isRadImmune`,
  both full methods), `.../handler/EntityEffectHandler.java` (lines 200-270 of 759 —
  `handleRadiationEffect`, the radiation-mutation ladder that is `EntityCreeperNuclear`'s *other*
  creation path), `.../entity/projectile/EntityBullet.java` (lines 420-480 — the legacy "rad" bullet's
  Creeper→Nuclear-Creeper mutation-on-hit branch), `.../potion/HbmPotion.java` (lines 60-115 —
  `registerPotions` + `performEffect`'s `taint`/`radiation`/`radaway` branches, including the
  `EntityCreeperTainted` self-immunity exclusion), `.../tileentity/machine/TileEntityTesla.java`
  (lines 100-150 — the entity-zap loop's `EntityCreeper`/`EntityCreeperNuclear` branches),
  `.../main/ModEventHandler.java` (lines 1020-1060 — the `LivingDeathEvent` handler's
  `EntityCreeperTainted`+`ModDamageSource.boxcar` hidden-achievement branch), `.../util/
  DamageResistanceHandler.java` (line 124 only — the `EntityCreeperNuclear` explosion-resistance
  table entry), `.../entity/mob/EntityCyberCrab.java` (line 26 only — a targeting-exclusion grep;
  full survey belongs to whichever report covers `EntityCyberCrab` itself), `.../entity/mob/
  EntityDuck.java` (class signature only, confirming `EntityQuackos`'s real base class)
- Repo-wide greps across `upstream/hbm-ce/src/main/java` for `EntityCreeperNuclear`,
  `EntityCreeperGold`, `EntityCreeperPhosgene`, `EntityCreeperTainted`, `EntityCreeperVolatile`, and
  `immuneEntities` to find every real call site touching these 5 classes (11 files total; every hit
  is accounted for above or explicitly named as out of this report's scope)
- `upstream/neo-edition/src/main/java/com/hbm/entity/mob/CreeperNuclear.java` (131 lines, read in
  full — the only creeper variant Neo Edition has ported; consulted for real 1.21.1 API shape only,
  per this task's ground rules) and `.../entity/NtmEntityTypes.java` (registration lines for
  `CREEPER_NUCLEAR`/`DUCK` read) and `.../main/CommonEvents.java` (grepped:
  `onEntityAttributeCreation`'s `event.put(NtmEntityTypes.CREEPER_NUCLEAR.get(), ...)` line)
- This port's own already-committed, confirmed-compiling code, read in full: `src/main/java/com/hbm/
  entity/GunEntityTypes.java` (77 lines — the `DeferredRegister<EntityType<?>>` template),
  `src/main/java/com/hbm/interfaces/IRadiationImmune.java` (3 lines), `src/main/java/com/hbm/util/
  DamageResistanceHandler.java` (40 lines — confirms this is a *stub*, not the full resistance-table
  port), `src/main/java/com/hbm/explosion/vanillant/standard/{BlockAllocatorBulkie,
  BlockMutatorBulkie}.java` (~55 and ~58 lines — confirms both are already reusable as-is for the
  Gold/Volatile creepers' explosions); grepped: `src/main/java/com/hbm/util/ContaminationUtil.java`
  (lines 330-372, 654-678), `src/main/java/com/hbm/config/{ServerConfig,CompatibilityConfig}.java`
  (`TAINT_TRAILS`, the `isWarDim` scope-note comment), `src/main/java/com/hbm/damage/
  ModDamageTypes.java` (`RADIATION`/`MUD_POISONING`/`BOXCAR`/`TAINT` all present), `src/main/java/
  com/hbm/explosion/ExplosionNukeSmall.java` (`PARAMS_SAFE`/`PARAMS_MEDIUM` present), `src/main/java/
  com/hbm/saveddata/satellites/SatelliteRelay.java` (line 80 — the `Level.OVERWORLD` dimension-check
  idiom already used elsewhere in this port)
- Non-existence confirmed by `find`/`grep` sweeps of `src/main/java` (each cited again in Deferred
  scope with its owning report where one exists): `EntityMist`, `BlockTaint`/`ModBlocks.taint`,
  `AdvancementManager`, `OreDictManager`/`DictFrame`, `HbmPotion`, `EntityEffectHandler`,
  `coin_creeper`, `block_slag`, `crystal_gold`, `stick_tnt`, and any existing `SpawnEggItem`/
  `SpawnPlacements`/`MobCategory.MONSTER` precedent (none — this would be this port's first natural-
  spawning hostile mob)
- Cross-referenced sibling Phase 4 reports (sections read, not re-derived): `docs/phase4/
  hbm_potion_system.md` (lines 160-200 — its own Deferred scope explicitly forwards
  `EntityCreeperTainted`/`EntityQuackos` to "whichever phase/area researches those mobs", i.e. this
  report), `docs/phase4/worldgen_oil_and_meteor_dungeons.md` (lines 350-380 — crater-biome ownership),
  `docs/phase3/grenades.md` (`EntityMist` sections — already fully researched there),
  `docs/phase3/bomb_blocks_and_detonators.md` (line 263 — `BlockTaint` ownership statement),
  `docs/phase1/blocks_generic.md` (`BlockTaint` mention), `docs/phase3/STATUS.md` (grepped for
  grenade-area completion status)

## Headline finding

**Only 4 of the 5 `EntityCreeper*` classes — plus one wrong assumption to correct — matter here, and
none of them are naturally-spawning "monster in a biome" mobs the way the task's framing implies for
all five.** Two corrections to make before scoping:

1. **`EntityQuackos` is not a creeper, and does not belong in this report.** It's named right next to
   `EntityCreeperNuclear` in `ContaminationUtil`'s hardcoded `immuneEntities` array (and in this
   port's own `isRadImmune` TODO comment, and in `hbm_potion_system.md`'s Deferred scope) purely
   because CE's original author bundled two unrelated "instant-kill-immune" mobs into one array. Read
   in full, `EntityQuackos extends EntityDuck` (which itself `extends EntityChicken`) — it's "the
   Elder One" / "BOW" (CE's own doc comments, verbatim), a giant (`0.3F*25` × `0.7F*25` hitbox,
   i.e. 7.5×17.5 blocks), server-boss-bar-carrying, permanently-invulnerable
   (`getIsInvulnerable() = true`, `setHealth` refuses any decrease), non-despawning duck that players
   can ride, tracked at `trackingRange = 1000`. It has zero creeper genealogy, zero explosion, and
   zero spawn-egg-summonable presence implied by normal mob content (its `@AutoRegister` name is
   `entity_elder_one`, its egg colors are cosmetic leftovers). It belongs with whichever sibling
   report covers CE's boss-tier mobs (alongside `EntityUFOBase`/`EntityBOTPrimeBase`-family content
   the task description already assigns elsewhere), not here. This report only resolves it far enough
   to confirm that fact and to note (again) that once it's ported, `ContaminationUtil.isRadImmune`
   needs an `e instanceof EntityQuackos` (or, better, `implements IRadiationImmune`) branch exactly
   like the one this report adds for `EntityCreeperNuclear` — see Key design decisions.
2. **Of the 4 real creeper variants, only 3 have a natural biome spawn weight at all, and the other 2
   are never placed by world-gen — they're *mutations* of a vanilla `EntityCreeper` triggered by
   gameplay events.** `EntityMappings.writeSpawns()` (CE's actual, only, biome-spawn-list
   registration site — read in full) registers exactly `EntityCreeperPhosgene` (weight 5),
   `EntityCreeperVolatile` (weight 10), and `EntityCreeperGold` (weight 1, further gated to
   `posY <= 40 && dimension == 0` by its own `getCanSpawnHere()` override) across every biome except
   mushroom islands. **`EntityCreeperNuclear` and `EntityCreeperTainted` never appear in this list, or
   in any other spawn-list/mob-spawner code anywhere in CE.** Their only two creation paths are, for
   Nuclear: (a) `EntityEffectHandler.handleRadiationEffect` mutating a vanilla `EntityCreeper` that
   has accumulated ≥200 rad (1-in-3 chance per check, else the creeper just takes 100 radiation
   damage instead), or (b) the legacy `EntityBullet`'s "rad" ammo flag instantly replacing any
   `EntityCreeper` it hits with an `EntityCreeperNuclear`; and for Tainted: `BlockTaint`'s
   `onEntityCollision` instantly replacing any `EntityCreeper` (that isn't already one) that touches a
   taint block. Treating "spawn conditions/rarity" as a single biome-spawn-weight question for all 5,
   as the task framing invites, would miss this entirely — Nuclear and Tainted have no rarity weight
   to port at all, only mutation *trigger conditions*.

## Phase-4-safe scope

The 4 real creeper variants, everything confirmed real and portable now:

| Class | Base attrs vs. vanilla Creeper (20 HP / 0.25 speed / 30-tick fuse) | Radiation immune? | Explosion (already-ported Phase 3 machinery used) | Natural spawn |
|---|---|---|---|---|
| `EntityCreeperGold` | unchanged | Yes — `implements IRadiationImmune` | `ExplosionVNT` r=14(powered)/7, `BlockAllocatorBulkie(60, 32/16)` + `BlockProcessorStandard`+`BlockMutatorBulkie(Blocks.GOLD_ORE)` (turns stone into gold ore, doesn't destroy it) + `EntityProcessorStandard(rangeMod 0.5)` + `PlayerProcessorStandard` + `ExplosionEffectStandard` — **all 6 of these classes already exist in this port**, reusable as-is | Weight 1 (rarest of the 3), all biomes, **plus** `getCanSpawnHere()` restricts to `posY<=40 && dimension==Overworld` |
| `EntityCreeperVolatile` | unchanged | No (plain `EntityCreeper`, not in `immuneEntities`, no interface) | Same `ExplosionVNT` shape as Gold, r=14/7, but `BlockMutatorBulkie(ModBlocks.block_slag, 1)` (turns stone into slag) | Weight 10 (most common of the 3), all biomes |
| `EntityCreeperPhosgene` | `fuseTime = 20` (faster fuse; vanilla is 30) | No | Vanilla-flavored `world.createExplosion(2F, no fire)` (trivial, portable today) **+** spawns an `EntityMist` set to `Fluids.PHOSGENE`, 10×5 area, 150-tick duration (blocked — see Deferred scope #1) | Weight 5, all biomes |
| `EntityCreeperTainted` | 15 HP (lower), 0.35 speed (faster); regen 1 HP/10 ticks while alive; also takes 4 less damage from any non-absolute/non-unblockable source (same reduction pattern as Phosgene, see below) | Yes — `implements IRadiationImmune`; **also** separately excluded from `HbmPotion.taint`'s own 1-in-80-per-tick self-damage check (an *independent* immunity axis, not via `isRadImmune`) | `world.newExplosion(5F, no fire, respects mobGriefing)` (portable today) **+** a taint-terrain-conversion sweep — powered: 255 random samples in a 15×15×15 cube around the creeper, each turning any non-air/non-bedrock solid block into `ModBlocks.taint` at taintage 5-7 (or 0-2 if `ServerConfig.TAINT_TRAILS`); unpowered: 85 samples in a 7×7×7 cube, taintage 10-15 (or 4-6 with `TAINT_TRAILS`) — **blocked, see Deferred scope #2** | **None — mutation-only, see Headline finding #2 and Deferred scope #5** |

(`EntityCreeperPhosgene`'s damage-reduction override reads `if(!source.isDamageAbsolute() && !source.isUnblockable()) amount -= 4F; if(amount<0) return false;` — flat 4-damage reduction against any non-absolute/non-unblockable source, before calling `super.attackEntityFrom`. In 1.21.1 this maps onto checking `DamageSource.is(DamageTypeTags...)` / the type's `DamageEffects`/`DamageScaling` rather than the removed `isDamageAbsolute()`/`isUnblockable()` booleans — flag for the implementer to confirm the closest 1.21.1 tag-based equivalent once a real jar is available; this report cannot verify that mapping without one.)

Drops (all fully portable mechanics; only the *item identities* are gapped, see Deferred scope #7):
- **Gold**: `getDropItem()` → `Items.GUNPOWDER` (vanilla, already available); `dropFewItems` — player-killed: `5 + rand(6 + 2*looting)` × `crystal_gold`; otherwise: flat 3 × `crystal_gold`.
- **Volatile**: `getDropItem()` → `Items.GUNPOWDER`; `dropFewItems` — flat `sulfur × (2+rand(3))` + `stick_tnt × (1+rand(2))`, no looting scaling, no player-kill distinction.
- **Phosgene**: no overrides at all — uses vanilla `EntityCreeper`'s own loot table (plain gunpowder, including the vanilla skeleton-arrow → music-disc mechanic, unmodified).
- **Tainted**: `getDropItem()` → `Items.TNT` block item; `dropFewItems` not overridden (falls through to vanilla `EntityCreeper`'s own drop-count logic using this overridden `getDropItem()`).

`applyEntityAttributes`, `getLootTable()` (nulled to force the `dropFewItems` path instead of a
datapack loot table — the 1.21.1 equivalent is simply not assigning `RegistryKey<LootTable>` and
relying on `dropCustomDeathLoot`), and `getCanSpawnHere`/spawn-placement restrictions are all
plain, mechanically-portable overrides with a confirmed real 1.21.1 shape (see Key design decisions).
None of the 4 variants override AI (`initEntityAI`/`applyEntityAI`/`tasks` in CE) at all — every one
inherits vanilla `Creeper`'s `GoalSelector` wholesale, confirmed by reading all 4 files in full and by
Neo Edition's own `CreeperNuclear` never touching `registerGoals()` either.

## `EntityCreeperNuclear` (the deepest of the 4 — detailed separately)

Read in full (140 lines). Differences from vanilla Creeper:
- 50 HP (2.5× vanilla), 0.3 speed (1.2×), `fuseTime = 75` (2.5× vanilla's 30 — a much longer fuse,
  consistent with giving players time to flee a nuke-tier blast).
- `attackEntityFrom` special-cases `ModDamageSource.radiation`/`ModDamageSource.mudPoisoning`: instead
  of taking damage, it **heals** by the incoming amount and returns `false` (fully immune to and fed
  by radiation/mud damage specifically — both damage types are already registered in this port's
  `ModDamageTypes` as `RADIATION`/`MUD_POISONING`). Also guards against `isDead` re-entrancy (CE's own
  comment: prevents the death animation replaying if damaged again after `setDead()`).
- `onUpdate`: every tick, server-side, contaminates every living entity within a 5-block AABB grow
  with `HazardType.RADIATION`/`ContaminationType.CREATIVE`, amount 0.25 (i.e. it's a walking radiation
  source — reuses `ContaminationUtil.contaminate`, already real and compiling in this port). Also
  regenerates 1 HP every 10 ticks while below max HP (same regen pattern as `EntityCreeperTainted`).
- `explode()` branches on `getPowered()` (vanilla's lightning-charged state, inherited unmodified —
  `TileEntityTesla`'s zap logic explicitly calls `creeperNuclear.setPowered(true)` instead of the
  lightning-bolt trick it uses on plain `EntityCreeper`s, confirming powered-Nuclear-Creeper is a real,
  reachable in-game state via Tesla coils, not just via natural lightning):
  - **Powered**: on `mobGriefing` — `EntityNukeExplosionMK5.statFac(world, 50, x, y, z).setDetonator(this)` (a
    real 50-yield nuke, already-ported Phase 3 class); off `mobGriefing` —
    `ExplosionNukeGeneric.dealDamage(world, x, y+0.5, z, 100)` (damage-only, no terrain destruction;
    already-ported).
  - **Unpowered**: `ExplosionNukeSmall.explode(..., PARAMS_MEDIUM)` on `mobGriefing`, or `PARAMS_SAFE`
    off — both `MukeParams` constants already exist in this port's `ExplosionNukeSmall.java`.
  - Also plays `HBMSoundHandler.mukeExplosion` and broadcasts a `Muke`-type `AuxParticlePacketNT`
    within 250 blocks before either branch (VFX — Phase 5 territory, not reproduced here).
- `onDeath`: grants `AdvancementManager.bossCreeper` to every player within 50 blocks (blocked, see
  Deferred scope #6); if killed by an `EntitySkeleton` or by a no-owner `EntityArrow` (i.e. dispenser-
  fired, matching vanilla's own "who gets the music disc" check), additionally drops one
  `OreDictManager.DictFrame.fromOne(ModItems.ammo_standard, GunFactory.EnumAmmo.NUKE_STANDARD)` — this
  replaces vanilla's skeleton-kill music-disc mechanic with a "Nuke Standard" ammo drop. `ammo_standard`
  and `GunFactory.EnumAmmo` are already real (Sedna gun framework, Phase 3); `OreDictManager.DictFrame`
  is not (blocked, see Deferred scope #7 — a cosmetic-only gap, doesn't block the mob existing).
- `dropFewItems`/`getDropItem`: `Items.TNT`, plus a 1-in-3 chance of one `coin_creeper` (blocked, see
  Deferred scope #8 — likewise cosmetic-only).
- `DamageResistanceHandler` (CE, 608-line real system) separately grants `EntityCreeperNuclear` 5
  armor / 0.35 damage-multiplier against `CATEGORY_EXPLOSION` damage — **this port's
  `DamageResistanceHandler.java` is confirmed to be only a minimal stub** (the `DamageClass` enum +
  pierce-state contract, explicitly not the entity-resistance-table system; see that file's own
  javadoc), so this specific resistance bonus has nowhere to live yet. Documented here for whoever
  does that full resistance-table pass (already named as its own deferred item by
  `docs/phase3/gun_framework.md`); not a gap this report can or should close.
- `EntityCyberCrab`'s targeting predicate explicitly excludes `EntityCreeperNuclear` (and plain
  `EntityCreeper`) from its valid-target set — a one-line cross-reference in a boss/mob file this
  report doesn't own; noted so whoever ports `EntityCyberCrab` knows to preserve it.

## Deferred scope

Numbered to match the inline references above:

1. **`com.hbm.entity.effect.EntityMist`** (368 CE lines) — `EntityCreeperPhosgene`'s gas-cloud payload.
   Already fully researched and placed in `docs/phase3/grenades.md`'s own Phase-3-safe scope (it's the
   disperser/grenade family's shared area-effect entity, not creeper-specific) — confirmed **not yet
   implemented** in this port's `src/` tree as of this session. This report does not re-derive its
   ~10 `FluidTrait`-keyed effect branches; once Phase 3's own scope item lands, wiring
   `EntityCreeperPhosgene.explode()` to spawn one typed `Fluids.PHOSGENE`, 10×5, 150 ticks is a
   one-line call, no new research needed.
2. **`com.hbm.blocks.bomb.BlockTaint` / `ModBlocks.taint`** — does not exist in this port yet.
   `docs/phase3/bomb_blocks_and_detonators.md` explicitly assigns it to "Phase 4's world-simulation/
   hazard survey" but, as far as this session's cross-reference sweep of `docs/phase4/*.md` found, no
   specific Phase 4 report has yet claimed the block itself (only its *consumers* — e.g.
   `hbm_potion_system.md`'s `taint` potion effect, `fallout_rain_and_effects.md`'s `isWarDim` gate —
   are covered elsewhere). This blocks **all** of `EntityCreeperTainted`'s taint-terrain-conversion
   explosion sweep, and — more importantly — its *entire spawn/creation path*, since
   `BlockTaint.onEntityCollision` is CE's only place that ever instantiates `EntityCreeperTainted`.
   Until it lands, `EntityCreeperTainted` can exist as a summonable/spawn-egg-only mob with fully
   correct combat/drop/explosion-radius behavior, but with zero real in-game creation path and a
   permanently-inert taint-conversion branch (safe no-op, matches the "block doesn't exist yet"
   reality). This report supplies the exact mutation mechanic (100% chance on any non-Tainted
   `EntityCreeper` colliding with the block) so whoever eventually ports `BlockTaint` doesn't have to
   re-derive it from CE.
3. **`CompatibilityConfig.isWarDim(Level)`** — confirmed intentionally not ported (this port's own
   `CompatibilityConfig.java` doc comment says so explicitly, being keyed by CE's now-meaningless
   integer Forge dimension IDs). Same forward reference already named by `hbm_potion_system.md` and
   `fallout_rain_and_effects.md`; relevant here only because `BlockTaint`'s spread mechanic and the
   wider "War Dim" taint content it belongs to are gated by it. Not a new dependency this report
   introduces, just confirming it's the same one.
4. **`com.hbm.potion.HbmPotion`** (the whole class) — confirmed not ported. Two independent touch
   points: (a) `HbmPotion.mutation` is the other half of `ContaminationUtil.isRadImmune`'s own
   already-documented TODO (`isPotionActive(HbmPotion.mutation)` — orthogonal to the two mob checks
   this report resolves, not this report's job); (b) `HbmPotion.taint`'s `performEffect` excludes
   `EntityCreeperTainted` from its own 1-in-80 self-damage tick — a one-`instanceof`-line integration
   this report documents (see the table above) for whoever assembles `HbmPotion.performEffect` per
   `docs/phase4/hbm_potion_system.md` (which itself, circularly but resolvably, forwarded this exact
   mob back to this report — see Headline finding). Net effect: neither report is blocked on the
   other for its own core deliverable; only this one small mutual cross-reference needs whichever
   lands second to wire in.
5. **`com.hbm.handler.EntityEffectHandler`** (759 CE lines) — confirmed not ported; already named as
   an unclaimed class shell by three other Phase 4 reports (`pollution_system.md`,
   `fallout_rain_and_effects.md`, `worldgen_oil_and_meteor_dungeons.md`). Its
   `handleRadiationEffect` method (read in full for this report, lines 233-270ish) is
   `EntityCreeperNuclear`'s *primary* real-world creation path: any vanilla `EntityCreeper` that
   accumulates ≥200 rad (via `HbmLivingProps`, already real) has a 1-in-3 chance per periodic check of
   being replaced by an `EntityCreeperNuclear` at the same position (else it takes 100 radiation
   damage instead — the same method also handles Cow→Mooshroom at ≥50 rad, Villager→ZombieVillager at
   ≥500, Blaze→RADBeast at ≥700, and a 4th ≥800 branch for horses, all out of this report's scope).
   This report documents the exact threshold/formula; wiring a real per-tick call site into a
   `LivingTickEvent`-equivalent belongs to whichever area assembles `EntityEffectHandler`'s shell —
   this report's own deliverable (the `EntityCreeperNuclear` class itself, spawnable via spawn egg or
   `/summon` immediately) is not blocked by that wiring not existing yet.
6. **`com.hbm.main.AdvancementManager`** — confirmed not ported (already named by the task framing as
   shared with Phase 3's satellite work). Blocks `EntityCreeperNuclear.onDeath`'s `bossCreeper`
   achievement grant and `ModEventHandler`'s `EntityCreeperTainted`+`ModDamageSource.boxcar` hidden
   `bobHidden` achievement (the latter also needs `ModEventHandler`'s own `LivingDeathEvent` handler
   assembled, itself a large unowned class outside this report's scope — `ModDamageSource.boxcar` is
   already registered as `ModDamageTypes.BOXCAR` in this port, so only the achievement-grant call site
   and `AdvancementManager` itself are missing). Neither achievement blocks the mobs' own core
   behavior from existing and functioning correctly.
7. **`com.hbm.inventory.OreDictManager`/`DictFrame`** — confirmed not ported (already a known,
   frequently-cited gap: `ItemDefuser.java`, `BlockResourceStone.java`, and several `inventory/
   recipes/*.java` files in this port already carry the same forward-reference comment). Blocks only
   `EntityCreeperNuclear`'s bonus "Nuke Standard ammo on skeleton/arrow kill" drop — a cosmetic
   flourish, not the mob's core loot.
8. **Small unregistered `ModItems`/`ModBlocks` entries, no dedicated report claims any of them yet**:
   `coin_creeper` (an `ItemCustomLore`-based collectible — that base class is already ported at
   `src/main/java/com/hbm/items/special/ItemCustomLore.java`, only the specific item instance is
   missing; `EntityCreeperNuclear`'s bonus drop), `crystal_gold` (`EntityCreeperGold`'s main drop),
   `stick_tnt` (`EntityCreeperVolatile`'s drop — `sulfur` itself is already a known, differently-cited
   gap per `SILEXRecipes.java`/`MixerRecipes.java`/`CrystallizerRecipes.java` comments, not new to this
   report), and `block_slag` (`EntityCreeperVolatile`'s explosion ore-mutation target — CE's call
   passes a metadata index `1`, which the already-ported `BlockMutatorBulkie` has no
   `(Block, int)` overload for; only its `(BlockState)` overload, so whoever registers `block_slag`
   needs to pass the correct metadata-1-equivalent `BlockState` directly). None of these block a
   variant's core existence or combat behavior — only specific drop/terrain-mutation flourishes.
9. **Client-side rendering** — `RenderCreeperUniversal` (CE, a shared multi-skin `RenderCreeper`
   subclass for all `EntityCreeper` variants including these) is out of scope per the project's phase
   split (Phase 5, "Client & UX"). Neo Edition already has a real, compiling reference shape for this
   (`upstream/neo-edition/src/main/java/com/hbm/render/entity/mob/CreeperNuclearRenderer.java`),
   confirmed to exist but not read in depth here — that's Phase 5's source to consult, not this
   report's.

## Key design/API decisions

Confirmed from real code (CE for behavior; this port's own committed code and Neo Edition for
1.21.1 API shape — never Neo Edition for behavior):

- **Entity registration follows `com.hbm.entity.GunEntityTypes`'s exact established pattern** (a
  dedicated `DeferredRegister<EntityType<?>>`, since no shared `ModEntityTypes` registry exists yet in
  this port) — confirmed additionally by Neo Edition's real, compiling
  `EntityType.Builder.of(CreeperNuclear::new, MobCategory.MONSTER).sized(0.6F, 1.7F).build(...)` call
  for its own `CreeperNuclear`, which matches vanilla `Creeper`'s own hitbox exactly (all 4 of this
  report's variants should use the same 0.6×1.7 sizing — none of the 4 CE classes override
  `setSize`). Given no shared "mob entity types" registry exists yet either, and this report's 4
  classes are a natural family, a dedicated `CreeperVariantEntityTypes` (or similar) following the
  `GunEntityTypes` shape is the consistent choice — left for the implementer, this report does not
  presume to name it.
- **Attributes go through `EntityAttributeCreationEvent`, not a per-entity constructor call** —
  confirmed by Neo Edition's real `CommonEvents.onEntityAttributeCreation`:
  `event.put(NtmEntityTypes.CREEPER_NUCLEAR.get(), CreeperNuclear.createAttributes().build())`, with
  `createAttributes()` a `public static AttributeSupplier.Builder` returning
  `Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50.0D).add(Attributes.MOVEMENT_SPEED,
  0.3D)` — directly reusable as the exact shape for all 4 variants' attribute overrides (Gold/
  Phosgene/Volatile just call `Monster.createMonsterAttributes()` with no `.add(...)` overrides at
  all, since none of the three touch `applyEntityAttributes` in CE).
- **`dropFewItems`/`getDropItem` → `dropCustomDeathLoot(ServerLevel, DamageSource, boolean)`** —
  confirmed by Neo Edition's real override signature and its `this.spawnAtLocation(Items.TNT)` body;
  this is the correct 1.21.1 override point for all 4 variants' drop-table overrides, and
  `getLootTable()`-nulling (CE's mechanism to force the `dropFewItems` code path instead of a real
  loot table) simply becomes "don't assign this entity a loot table resource key at all."
- **`IRadiationImmune` stays the right mechanism for `EntityCreeperGold`/`EntityCreeperTainted`**
  (both already `implements IRadiationImmune` in CE — zero new work needed in `ContaminationUtil`
  once they're ported, since `isRadImmune` already checks `e instanceof IRadiationImmune`).
  **Recommend `EntityCreeperNuclear` also `implements IRadiationImmune`** in the port, even though CE
  itself hardcodes it into the `immuneEntities` array instead of using the interface both
  `EntityCreeperGold`/`EntityCreeperTainted` already use — this is a deliberate, behavior-preserving
  departure from CE's exact class structure (isRadImmune still returns `true` for it either way),
  consistent with this port's own already-stated design rationale for having `IRadiationImmune` exist
  at all (`ContaminationUtil`'s own javadoc: "Phase 0's whole reason to define it rather than hardcode
  a list"). The alternative — extending `isRadImmune`'s `instanceof` chain with a fourth hardcoded
  check — works identically but re-introduces exactly the pattern the port's own interface was created
  to avoid. Either resolves the TODO; the interface is the more consistent choice. (`EntityQuackos`,
  once ported by its own sibling report, should get the same treatment for the same reason.)
- **Spawn-egg items need their own explicit registration — CE's `@AutoRegister(eggColors=...)` had no
  1.21.1 equivalent to consult.** Confirmed by reading the CE annotation processor
  (`AutoRegisterProcessor.java`): in 1.12.2, egg colors fold directly into the `EntityEntryBuilder`
  that `EntityRegistry.registerModEntity` builds, and Forge auto-derives a spawn-egg item from that —
  no separate item class ever existed in CE for these. In 1.21.1, `SpawnEggItem` is a real, distinct
  `Item` that must be registered explicitly per entity (general, well-established NeoForge/Mojang-
  mapping knowledge — **not verified against a real jar or any precedent in this port or Neo Edition**,
  since neither has registered a spawn egg for anything yet; this would be the first). The 5 egg-color
  pairs to carry over verbatim from CE's `@AutoRegister` annotations: Nuclear `0x204131`/`0x75CE00`,
  Gold `0xECC136`/`0x9E8B3E`, Phosgene `0xE3D398`/`0xB8A06B`, Tainted `0x813b9b`/`0xd71fdd`, Volatile
  `0x000020`/`0x2D2D72`.
- **Natural biome spawning needs a mechanism this port has never used before.** CE's `EntityMappings.
  writeSpawns()` mutates every `Biome`'s Java `SpawnListEntry` list directly at runtime — a pattern
  1.21.1 replaces with either data-driven `neoforge:add_spawns` biome-modifier JSON (the idiomatic
  modern approach, general knowledge, not verified against a real jar) or an equivalent
  `SpawnPlacements.register(...)` + biome-tag-based JSON pairing for the surface/light-level
  conditions Creeper-family mobs need. This port has zero existing precedent for either approach (no
  `SpawnPlacements` call, no `biome_modifier` datapack file anywhere in `src/main/resources`) — this
  report can confirm the *data* (weights 1/5/10, all-biomes-except-mushroom-island, Gold's extra
  `posY<=40 && Overworld` restriction) but not a verified 1.21.1 call shape; flagged as a real open
  question below, not invented here.
- **`dimension == 0` → `level.dimension() == Level.OVERWORLD`** for `EntityCreeperGold`'s
  `getCanSpawnHere()` restriction — confirmed as this port's own established idiom (already used at
  `SatelliteRelay.java:80`), a direct, low-risk translation.
- **`setPowered` needs its own public method on the ported class, same as CE** — CE's
  `EntityCreeperNuclear.setPowered(boolean)` writes directly to the `POWERED` `DataParameter` it
  inherits from `EntityCreeper` (protected, not otherwise publicly settable — vanilla only ever flips
  it via a lightning strike), because `TileEntityTesla`'s zap loop needs to charge a Nuclear Creeper
  without an actual lightning bolt. The ported class needs the equivalent: a public method writing to
  vanilla `Creeper`'s own (likely `DATA_IS_POWERED`-named, Mojang mappings — general knowledge, not
  jar-verified) synced accessor, accessible from a subclass regardless of package.

## Open questions / risks

- **Neo Edition's `CreeperNuclear` hooks `Creeper#ignite()` to perform the *entire* explosion
  immediately (`super.ignite(); this.dead = true; this.nuclearExplode(); ...; this.discard();`),
  bypassing whatever swell-timer mechanism drives vanilla's own explosion.** General Mojang-mapping
  knowledge (not verified against a real jar in this sandbox — no decompiled 1.21.1 source or
  compiled jar is available to check) suggests `ignite()` traditionally only *starts* the swell-up
  countdown (what CE's own `fuseTime` field controls the length of), with the actual
  `level.explode(...)` call happening later, inside a separate tick-driven method once the swell
  counter reaches its max. If that's correct, Neo Edition's approach silently skips CE's own
  `fuseTime = 75` tuning entirely (an instant detonation on ignite rather than a played-out ~3.75s
  fuse at 20 ticks/sec) — a real behavioral divergence from CE that this task's "no unverified CE
  behavior from Neo Edition" ground rule would flag as unsafe to copy uncritically. **Recommend the
  implementer verify the real override point (`ignite()` vs. a separate `explodeCreeper()`-equivalent)
  against an actual compiled 1.21.1 jar/decompile before committing to Neo Edition's shape** — getting
  this wrong changes a tuned, deliberate CE balance value (a 2.5×-longer-than-vanilla fuse, clearly
  chosen to let players run from a nuke) into a no-warning instant kill.
- **Natural biome spawning has no established pattern anywhere in this port yet** (see Key design
  decisions) — the implementer will need to make a first-of-its-kind call between a
  `neoforge:add_spawns` biome-modifier datapack file and a programmatic
  `SpawnPlacements.register(...)` + tag-scoped JSON pairing, neither of which this report can verify
  against a real jar. Low risk to the mobs' own correctness (a spawn-egg-only creature is still fully
  functional), but real risk to matching CE's "these wander the whole overworld at these weights"
  rarity feel if deferred indefinitely.
- **`EntityCreeperPhosgene`'s flat 4-damage reduction** (`if(!source.isDamageAbsolute() &&
  !source.isUnblockable()) amount -= 4F`) has no obvious 1-for-1 1.21.1 replacement, since
  `isDamageAbsolute()`/`isUnblockable()` were 1.12.2 `DamageSource` booleans removed in favor of
  `DamageType`-keyed tag/effect data. This report flags the exact CE semantics (reduce, don't
  outright block, any damage that isn't "absolute" or "unblockable") for the implementer to re-derive
  against 1.21.1's real `DamageType`/`DamageEffects` model rather than guessing a tag mapping here
  without a jar to check it against.
- **`BlockTaint`/War-Dim taint content has no confirmed owner among the Phase 4 reports read this
  session** (see Deferred scope #2) — worth flagging to whoever runs the Phase 4 planning pass, since
  at least 3 sibling reports (`hbm_potion_system.md`, `fallout_rain_and_effects.md`, this one) all
  depend on it existing and none claims to build it.
- **`crystal_gold`/`stick_tnt`/`block_slag`/`coin_creeper` are small, easy, unclaimed registrations**
  — low risk, but worth someone explicitly picking up rather than leaving as scattered forward
  references across multiple reports (this one included).
