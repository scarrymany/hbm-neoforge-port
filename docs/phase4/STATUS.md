# Phase 4 status

Phase 4 (World & simulation) followed the same research -> implement -> review -> fix methodology as
Phases 0-3, scaled to this phase's own unusually large size: a 15-agent research wave (`docs/phase4/*.md`,
~6,100 lines), an 8-agent foundation implement wave (status-effect system, pollution engine, chunk
radiation engine, advancement registry, legacy-ballistics retarget, loot-pool framework, small
block/config prerequisites), a 15-agent content implement wave (fallout, world-gen, meteor events,
creeper variants, satellite payloads, orbital/beam payloads, the gravity-well family + `ExplosionChaos`,
vehicles, and the full boss roster), and an 11-agent review/fix wave (8 per-subsystem reviews plus 3
cross-cutting audits). One container restart interrupted the content wave mid-flight; it was resumed via
the workflow tool's cached-replay mechanism with zero lost work (7 of 15 agents' output was already
checkpoint-committed before the restart, confirmed by re-inspecting the workflow's journal before
resuming). Full per-package detail lives in `docs/phase4/*.md` (research) and the git log - five commits
cover this phase area-by-area: `3bfcf3c` (research), `9b3ee0b` (foundation), `a422ae2`+`60350be` (content
wave 1, split by the restart), `57efb09` (review/fix).

This is the largest phase by research volume so far - the 15 reports collectively found roughly 3x the
"there's a whole unowned subsystem here" surface area that Phase 3's did (the legacy-bullet-system
retarget decision, the NBT-jigsaw structure engine, the Glyphid/FBI-raid mob families, the full
`ExplosionChaos` grab-bag, and the rail/train vehicle system were all discovered as much bigger than
their names suggested). This STATUS.md is correspondingly explicit about what got built end-to-end versus
what was deliberately scoped out as its own future work item.

## What's implemented (by package)

**Foundation** (lands first, everything else builds on it):
- `com.hbm.potion.*` - the full `HbmPotion` status-effect port: 8 real `MobEffect`s (taint, radiation,
  bang, lead, radaway, telekinesis, phosphorus, potion sickness) with CE-exact tick cadences/formulas/
  colors, plus a shared no-op class for the 4 marker-only fields (mutation, radx, stability, death), plus
  `HbmDetox`. Wired into ~11 already-committed Phase 1-3 forward references (`ContaminationUtil`,
  `ArmorUtil`, `HazmatRegistry`, `VersatileConfig`, `ItemPill`, `PoweredArmorItems`, `FoodItems`,
  `WeaponSpecial`, `WeaponMeleeItems`, `IWeaponAbility`).
- `com.hbm.handler.pollution.*` - `PollutionHandler`/`PollutionSavedData`, closing a *live* pre-existing
  compile break (`Fluids.java`/`FT_Polluting.java` already imported this class by name).
- `com.hbm.handler.radiation.*` - `ChunkRadiationManager`/`RadiationSystemNT`/`RadiationWorldHandler`,
  the real flood-fill pocket-storage radiation engine (UNI/SINGLE/MULTI section kinds, mass-conserving
  rebuild, diffusion/decay tick, `ChunkDataEvent`-based persistence - independently confirmed real
  against live NeoForge 1.21.1 source during review, since it was the single highest-risk unconfirmed API
  claim in the whole research wave). Wired into ~10 already-committed forward-reference call sites and 7
  `IRadResistantBlock` implementors.
- `com.hbm.main.AdvancementManager` - CE's 65-field advancement registry, `ServerStartingEvent` hook.
  **Ships in a deliberately inert state** - see Known gaps below, this was a review-wave crash fix, not a
  content-complete port.
- Legacy pre-Sedna mob/boss ballistics retargeted onto the already-shipped Sedna `BulletConfig`/
  `EntityBulletBaseMK4` framework rather than building a second ballistics system (`LegacyMobBulletConfigs`,
  11 configs covering every live GunNPCFactory ammo type plus Hunter Chopper/Cyber Crab).
- `com.hbm.itempool.ItemPool` - the loot-pool registry framework satellite/C130 content builds on.
- Small prerequisites: `ore_bedrock_block`+TE, `stone_gneiss`, `ore_nether_coal`, 3 crater biomes, the
  wasteland/reinforced/virus block set, and 39 dimension-keyed ore/oil/meteorite `CompatibilityConfig` maps.

**World simulation content**:
- **Fallout**: `BlockFallout`, `FalloutConfigJSON` (JSON block-transform rule engine), `EntityFalloutRain`
  (single-threaded MVP - see Known gaps), wired into both nuke-explosion entity families.
- **World-gen**: ~61 ordinary ore-vein/bedrock-ore `Feature` instances across every CE shape family
  (ellipsoid veins, chance-gated deposits, the gneiss stratum+vein two-stage mechanic, depth-ore sphere
  blobs, noise-layer ore), oil-deposit and ambient-meteorite features - all on the real 4-stage
  `ConfiguredFeature`/`PlacedFeature`/`BiomeModifier` pipeline, reading live `CompatibilityConfig` values
  rather than baking numbers into static datapack JSON.
- **Meteor events**: `EntityMeteor`, `MeteorStrikeHandler`, `ItemMeteorRemote`, `protection_charm`/
  `meteor_charm`.
- **Creeper variants**: all 5 CE creeper-family mobs (Gold/Volatile/Phosgene/Tainted/Nuclear) with spawn
  eggs, each with CE-faithful HP/speed/fuse/explosion/drop tables.
- **Satellite payloads**: `EntityDeathBlast`, `EntityOrbitalLaser`, the `EntityTom` family (+
  `ExplosionTom`'s full expanding-shell crater algorithm), `ItemPoolsSatellite`.
- **Orbital/beam payloads**: `EntityFireLingering`, `EntityMist` (wired to every real Phase 2/3 fluid
  trait), `EntityCoin`, wired into `EntityBulletBeamBase`'s coin-relay hitscan special case.
- **Gravity wells + `ExplosionChaos`**: `EntityBlackHole`/`EntityVortex`/`EntityRagingVortex`/
  `EntityQuasar` (4 separate classes, matching CE's real structure rather than Neo Edition's fold), and
  all 26 `ExplosionChaos` methods (not just the 2 the task's own framing named) - wired into `ItemDrop`,
  `ItemDigamma`, `ItemConserve`, `ItemGlitch`'s 2 real call sites, and `LegacyChargeWeapons`' mode 4-8.
- **Vehicles**: the full rail/train entity system (`EntityRailCarBase`+`LogicalTrainUnit` consist
  physics, `TrainCargoTram`/`Trailer` with real GUIs), scripted aircraft (`EntityPlaneBase`/`Bomber`/
  `C130` + `ItemPoolsC130`), the 7-class minecart family, and the logistics-drone entity-movement family
  (block/GUI network half explicitly deferred, see below).
- **Bosses**: the full 75-entity BOTPrime worm chain, MaskMan, UFO, Hunter Chopper, the CyberCrab family,
  `EntityQuackos`, `EntityRADBeast`, and `EntityEffectHandler`'s radiation-mutation cascade dispatch table
  (Creeper->Nuclear, Cow->Mooshroom, Villager->ZombieVillager, Blaze->RADBeast, Duck->Quackos, plus the
  crater-biome ambient tick and pollution's ambient poison/lead exposure).

## Real bugs found and fixed beyond each wave's own review stage

- **`AdvancementManager` crash-on-startup** - `Objects.requireNonNull` on every one of ~65 advancement
  lookups would NPE the server on `ServerStartingEvent` under default config (`ENABLE_ADVANCEMENTS`
  defaults `true`), since zero advancement datapack JSON files exist anywhere in this port. Fixed to
  degrade to a safe no-op everywhere instead of crashing - functionality (not just the crash) is a
  documented, tracked gap, see below.
- **All 5 `EntityCreeper*` classes had a compile-breaking bug**: they declared `@Override protected void
  explodeCreeper()`, but real NeoForge/vanilla 1.21.1 `Creeper#explodeCreeper()` is `private` - not a
  legal override point, confirmed via a live decompile check. Rebuilt the explosion trigger via a
  reflective swell-tick accessor (`CreeperVariantSupport`) instead of a (fictional) override point.
- **`OilBubbleFeature`/`BedrockOilDepositFeature` compile error**: both passed a `WorldGenLevel` where
  `OilSpot.generateOilSpot` requires a real `Level` (`WorldGenLevel` is a separate `LevelAccessor`
  interface, not a `Level` subtype) - fixed via the existing `OreShapeUtil.serverLevel(...)` conversion
  helper.
- **`PollutionHandler`'s poison-destruction sweep over-matched CE's real target-block set** - it hit
  vines, dead bushes, and double-tall plants (tall grass/large fern/tall flowers), none of which are
  `Material.plants`/`Material.leaves` in real CE 1.12.2 (verified against decompiled MCP source); fixed
  to match CE's real, narrower set.
- **`ExplosionChaos.spawnExplosion` was missing a real CE `isWarDim` gate** the research report's own
  summary table had marked as absent (a transcription error in the report, not in CE) - restored.
- **`EntityUFO`/`EntityHunterChopper` self-targeting/exclusion bugs**: UFOs could snipe each other (CE
  excludes same-class entities from targeting, the port only excluded literal self); Hunter Chopper's
  retarget logic didn't exclude other choppers or spectator-mode players, matching CE's real
  `Library.getClosestEntityForChopper` exclusion list.
- **`EntityRADBeast`**: missing `xpReward = 30`, and both its water-tick punishment and its "wet" loot
  branch used the narrower `isInWater()` instead of CE's real `isWet()` (water OR rain).
- **`EntityEffectHandler`'s entire radiation-mutation cascade was missing CE's top-level
  `GeneralConfig.ENABLE_RADIATION` gate** - it would keep mutating Creepers/Cows/Villagers/Blazes/Ducks
  even with the mod's own radiation system disabled.
- **4 silently-dead client GUI screens** (`TrainCargoTram`/`Trailer`, minecart crate/destroyer): the
  `MenuType`s and `Menu` classes were fully real and every owning entity already called
  `player.openMenu(...)`, but no `RegisterMenuScreensEvent` subscriber ever bound any of the four to a
  `Screen` - `MenuScreens` silently no-ops on an unregistered menu type rather than crashing, so this was
  invisible without actually opening one of the four GUIs. Fixed with a new self-contained
  `VehicleCargoClientRegistry`.
- **Dead registration code, recurring** (same bug class Phases 2-3 already hit repeatedly): three
  different content-wave agents independently discovered that sibling packages' `*EntityTypes`/
  `*WorldGenFeatures` `DeferredRegister`s were never actually passed to `MainRegistry`'s `modEventBus` -
  consolidated ~25 overlapping wiring-snippet proposals from 15 agents into one clean edit (17 new
  `*EntityTypes.register(modEventBus)`/`*Features.register(modEventBus)` calls, deduplicated rather than
  applied one-by-one) instead of letting the redundant proposals collide.
- **Live NeoForge API verification** (this session's WebFetch-based-GitHub-source technique, reused a
  third time this phase): confirmed `ChunkAccess`/`LevelChunk` do **not** implement `IAttachmentHolder`
  in this NeoForge version (validating `RadiationSystemNT`'s `ChunkDataEvent` fallback as the correct
  choice, not a workaround), and confirmed `ChunkDataEvent.Load`/`Save`/`getChunk`/`getLevel`/`getData`
  are real.

## Known gaps deferred (with rationale, not silently dropped)

- **`AdvancementManager` is functionally inert.** The crash is fixed, but zero `data/hbm/advancement/*.json`
  datapack files exist anywhere in this port - every boss `onDeath`/satellite-orbit `grantAchievement`
  call is now a silent no-op. Porting CE's real ~65 advancement JSON files (1.13+ datapack path,
  1.21.1 predicate-format updates) is real content-authoring work, not a code fix - recommend a
  dedicated Phase 5/6 datapack-content pass.
- **The full NBT-jigsaw structure engine** (`NBTStructure`/`JigsawPiece`/`JigsawPool`/`SpawnCondition` +
  `StructureManager` + `Component`, ~4,000 CE lines) backs ~30 CE structures at once - bunkers, radio
  stations, vertibird wrecks, the meteor dungeon, and more. The research report explicitly recommended
  *not* folding this into Phase 4's own implementation pass (it's its own project-sized area, and needs a
  1.12->1.21.1 `.nbt` asset-conversion pass this sandbox cannot do). **Not built this phase** - recommend
  a dedicated future "structure/dungeon placement engine" research + implementation area.
- **The legacy artillery/rocket entity family** (`EntityRocket`, `EntityMiniNuke`, `EntitySchrab`,
  `EntityRainbow` - `ExplosionChaos`'s `cluster`/`miniMirv`/`schrab`/`zomg` targets) and the **Glyphid mob
  family** (spawner, `EntityGlyphid`/`Scout`/`Digger` - `PollutionHandler`'s own `rampantTargetSetter`/
  `rampantScoutPopulator` are built and ready but have nothing to spawn yet) and the **FBI-raid mob
  family** (`EntityFBI`/`EntityFBIDrone`, `BossSpawnHandler`'s raid roll) are all real, confirmed-unowned
  CE content with no Phase 1-4 package claiming them. Not built this phase - each needs its own
  research+implement pass.
- **`com.hbm.blocks.rail`** - only a minimal `IRailNTM` interface stub exists (built so the rail/train
  entity system could compile against a real contract). The actual rail *blocks* (13 CE files, gated on
  the Phase 2 multiblock framework per `docs/phase1/blocks_network_rail.md`'s own unresolved
  recommendation) are still not built - rail cars exist but have no track to run on yet.
- **The logistics-drone block/GUI network** (`TileEntityDroneDock`/`Provider`/`Requester`,
  `IDroneLinkable`) - `EntityRequestDrone`'s instruction-queue executor is fully built and will dispatch
  against these the moment they exist, but they don't yet (a still-open Phase 2 package per
  `docs/phase2/items_tool_machine_coupling_and_recipe_system.md`'s own original recommendation).
- **`EntityFalloutRain`/`RadiationSystemNT` concurrency**: both ship as single-threaded MVPs. CE's real
  versions use a `ForkJoinPool` (radiation: parity-partitioned concurrent axis sweeps with a write-buffer
  `EditTable`; fallout: the same pool for large-radius scans). Correctness/output is preserved; CE's
  specific concurrency-tuned performance envelope is not - worth a load-test once a real build is
  available, especially for worst-case "large nuke, many dirty sections at once" scenarios.
- **`ExplosionChaos.cluster`/`miniMirv`/`schrab`/`zomg`/`tauMeSinPi`/`spawnChlorine`/`spawnVolley`** have
  no live call site today beyond the entities/particle-VFX infrastructure they need (the legacy artillery
  family above, and Phase 5's particle/networking layer) - method bodies exist and are ready, callers do not.
- **`FalloutConfigJSON`** skips ~9 CE default entries whose target block doesn't exist yet in this port
  (the 5 `ore_sellafield_*` decoration tiers, `sellafield_bedrock`, the irradiated Glyphid spawner
  variant) - logged via runtime warning rather than silently dropped.
- **`HbmDetox.isBlacklisted`** has no wired caller - CE's real consumer is a `PotionApplicableEvent`
  listener whose exact NeoForge 1.21.1 event name/shape could not be confirmed in this sandbox.
- **Client-side rendering for all of this phase's new content** (boss models/animations, vehicle
  models, every new particle effect, GUI screen art beyond the 4 fixed this wave, spawn-egg icons) is
  explicitly Phase 5 ("Client & UX") scope, not attempted here.
- **This sandbox cannot run `gradlew`** (network policy blocks `maven.neoforged.net`) - every claim above
  about compiling correctly is verified by static reading and cross-referencing against already-compiling
  code, not by an actual build. Several NeoForge API shapes (the boss-bar `ServerBossEvent` surface used
  by 4 bosses, `FlyingMob`'s exact `travel()` signature, `Entity#getControllingPassenger()`'s forward-input
  accessor for rail-car driving) remain "well-established Mojang-mapping knowledge, cross-checked against
  Neo Edition's independent port where one exists" rather than jar-verified - flagged inline in the
  relevant classes' javadoc, not silently assumed.
