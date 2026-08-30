# Phase 3 status

Phase 3 (Weapons & destruction) followed the same research -> implement -> review -> fix methodology
as Phases 0-2, scaled up further for the phase's own size: a 15-agent research wave
(`docs/phase3/*.md`), a 7-agent foundation implement wave (contamination/armor utility layer,
explosion vanillant engine, nuke-tier entity family, ballistics/ammo core, armor-equippable
framework, entity/chunk-loader utilities, weapon animation + control-packet infrastructure), a
12-agent content implement wave (conventional bombs, nuke casings, detonator items, turrets, the
gun held-weapon state machine, grenades, melee weapons, armor special sets, power-armor suits,
jetpacks, missile entity core, scattered military items), a 6-agent second content wave (the full
70-gun roster split into 4 caliber batches, the weapon-mod evaluation chain, missile launch
infrastructure), and a 9-agent review/fix wave (one per major subsystem, plus two cross-cutting
audits specifically re-hunting for bug classes already found once). Full per-package detail lives in
`docs/phase3/*.md` (research) and the git log (`git log --oneline` from `1ee56e6` through `de71efe`
covers this phase area-by-area, four large commits: foundation, content wave 1, content wave 2,
review/fix).

## What's implemented (by package)

**Foundation** (lands first, everything else builds on it):
- `com.hbm.capability.HbmLivingProps` + `com.hbm.handler.{ArmorModHandler,ArmorUtil}` +
  `com.hbm.util.ContaminationUtil` - the contamination/hazard/armor-mod utility layer. Closed a
  *live* pre-existing compile regression: 9 already-committed Phase 0/1 files imported these classes
  by name without them existing anywhere in the port.
- `com.hbm.explosion.vanillant.*` (`ExplosionVNT` + 10 interfaces + ~20 `standard/` implementations)
  - the pluggable "normal explosion" strategy-object framework, the single most-consumed system in
  the whole weapons area (65 CE consumers). Built with batched per-chunk block removal from the
  start per PORT_SPEC's explicit performance mandate, not retrofitted later.
- `com.hbm.entity.{logic,effect}.*` + `com.hbm.explosion.{ExplosionLarge,ExplosionNuke*,
  ExplosionBalefire,ExplosionFleija,ExplosionSolinium,ExplosionDrying}` - the nuke-tier explosion
  entities (mk3 column-carving and mk5 ray-batched families). Closed a second live pre-existing
  compile break (`HazardTypeUnstable.java`'s forward references to these exact classes).
- `com.hbm.items.weapon.sedna.BulletConfig` + `com.hbm.entity.projectile.*` - the ballistics/ammo
  core (gun framework Package A), PORT_SPEC's own flagged "unit-testable pure-logic core," shared by
  guns, turrets, and grenades.
- `com.hbm.items.armor.*` + `com.hbm.items.gear.{ArmorFSB,ArmorModel,ModArmor}` +
  `com.hbm.util.{DamageResistanceHandler,EntityDamageUtil}` - the armor-equippable base-class
  hierarchy and the central damage-reduction event dispatch that replaces CE's `ISpecialArmor` (which
  has no NeoForge 1.21.1 equivalent - `ISpecialArmor`-style full-item damage override became a
  shared `LivingIncomingDamageEvent`/`LivingDamageEvent.Pre` listener instead).
- `com.hbm.entity.logic.IChunkLoader` + `com.hbm.main.ModContext` + `com.hbm.entity.projectile.
  EntityRubble` + `com.hbm.entity.item.EntityTNTPrimedBase` - small, high-leverage shared utilities
  several later content packages depend on individually.
- `com.hbm.weapon.anim.*` + `com.hbm.packet.toclient.GunAnimationPayload` +
  `com.hbm.packet.toserver.ItemControlPacket` - the shared animation-trigger vocabulary and the
  port's first two Phase-3-scoped network payloads (stub client handler for the animation payload,
  full server-side dispatch for the generic item-control packet).

**Concrete content**:
- **Bombs**: the full conventional-explosive family (TNT/dynamite/semtex/C4/charges/landmines/
  det-cord/det-miner/flame-war/float/thermo), all 9 nuke casings + `NukeCustom`'s recipe-driven
  variant + crashed-bomb duds, and the 3 detonator items + defuser.
- **Turrets**: the full `TileEntityTurretBaseNT`-family targeting/power/whitelist core, all 14
  blocks, and 11 of 13 concrete turrets wired to fire (artillery/HIMARS turrets excluded per the
  research report's own recommendation - a separate, much larger missile/radar dependency chain).
- **Guns**: the complete 70-gun Sedna roster across every caliber family (black powder/.357/.44/
  9mm/.22LR pistols-revolvers-SMGs, 5.56mm/7.62mm/.50 BMG rifles and machine guns, 12ga/10ga/40mm/
  7.5mm-bolt shotguns and launchers, accelerator/energy/rocket/tool/flamer/drill/power-armor-delegate
  energy weapons), the legacy `gun_supershotgun`/`gun_vortex` decorative shells and the self-contained
  `gun_b92`/`gun_b93` charge weapons, plus the full held-weapon state machine (`ItemGunBaseNT`,
  `GunConfig`, `Receiver`, the pure-math ballistics core, the reload/jam state machine, every
  magazine style) and the weapon-mod evaluation chain (`XWeaponModManager` wired into every
  `GunConfig`/`Receiver` getter, all 28 concrete `WeaponMod*` attachment effects).
- **Grenades, melee, scattered items**: the throwable-grenade family, the ability-sword/chainsaw/
  crucible/multitool melee content, and the scattered military-C2 item cluster (detonator pager,
  radar linker, rangefinder, pocket nuke, loot crates).
- **Armor**: the special-behavior sets (euphemium/gas-mask/hazmat/asbestos-and-schrabidium-via-
  `ArmorFSB`), ~25 concrete power-armor leaf sets, and the 5 jetpack items, all built on the
  foundation's `ArmorFSB`/`ArmorFSBPowered`/`ArmorFSBFueled` hierarchy.
- **Missiles**: the flight-physics base, the composable custom-missile system, a representative tier
  roster, warhead dispatch wired to the foundation's nuke/explosion entities, and the full launch
  infrastructure (small/large/rusted launch pads, the silo blast-door multiblock, the Soyuz launch
  complex preserving CE's own incomplete launch sequence, designator/satellite items, and the
  satellite addressing/dispatch system).

## Real bugs found and fixed beyond each wave's own review stage

- **`@EventBusSubscriber` missing `bus = Bus.MOD` - the single highest-value finding this phase,
  mod-wide in scope, not just Phase 3.** Confirmed against real NeoForge 1.21.1 source (a live
  GitHub read of `RegisterPayloadHandlersEvent`/`RegisterMenuScreensEvent`/`RegisterKeyMappingsEvent`/
  `GatherDataEvent`, all `extends Event implements IModBusEvent`) and FancyModLoader's own
  `EventBusSubscriber` javadoc (`bus()` defaults to `Bus.GAME`, no auto-detection of
  `IModBusEvent`): 10 already-committed classes across every phase had this bug, including
  `com.hbm.packet.HbmNetwork` itself (meaning **every packet in the mod, including all of Phase 2's
  block-entity sync, was dead code**), all 7 machine-family `*ClientRegistry` classes plus
  `ClientModRegistry` (meaning **no Phase 2 machine's GUI screen was ever bound to its MenuType**),
  `com.hbm.handler.HbmKeybinds` (no keybind ever registered), and `com.hbm.datagen.
  ModDataGenerators` (datagen never ran). Fixed by adding `bus = Bus.MOD` to the 10 single-purpose
  classes and splitting the 2 classes that mixed a mod-bus event with a game-bus event
  (`CommonEvents`/`CommonTickEvents`, `HbmKeybinds`/`HbmKeybindInputEvents`) into pairs, since one
  `@EventBusSubscriber` class can only declare one bus.
- **Eager static-field `DeferredHolder` resolution crash, recurring 6+ times.** Any
  `public static final` field whose constructor resolves a `DeferredHolder`/`DeferredItem`/
  `DeferredBlock` via `.get()` (e.g. `Receiver.sound(HBMSoundHandler.xxx.get(), ...)`) throws
  `IllegalStateException` if the containing class loads before the relevant `RegisterEvent` fires -
  which it will, the moment any aggregator's `registerAll()` (itself called from `ModItems.register()`
  /`ModBlocks.register()`, both invoked synchronously from `MainRegistry`'s constructor) references
  the class. First found in the gun-content roster (`XFactory556mm`/`762mm`/`50`, then recurring in
  `XFactory10ga`/`12ga`/`40mm`/`75Bolt` and in two aggregator helper methods that re-introduced the
  same eager evaluation one level up), and independently in two Phase 2 files
  (`ChemIsotopeBlocks`/`OilChainBlocks` eagerly building recipe tables that resolve `DeferredItem`s).
  Fixed everywhere by converting eager fields to lazy static factory methods (or, for the Phase 2
  recipe tables, moving the calls to `FMLCommonSetupEvent`, which fires after every `RegisterEvent`).
- **`LivingAttackEvent` does not exist in NeoForge 1.21.1.** A foundation-wave file's javadoc claimed
  this CE event "survives verbatim into NeoForge 1.21.1" - false, confirmed by a live source search
  of the real `neoforged/NeoForge` repository (zero matches anywhere in the org). This would have
  failed to compile across 5 files (the central `ArmorDamageHandler` dispatcher, `IAttackHandler`,
  `ArmorFSB`, and 2 concrete power-armor sets). Fixed by rerouting the whole attack-cancellation
  dispatch chain onto `LivingIncomingDamageEvent`, which fires early enough in the pipeline to
  reproduce the same "cancel the attack outright" semantics and is already used elsewhere in the same
  file for an unrelated dispatch point (two separate `@SubscribeEvent` methods on the same event type
  is valid and normal).
- **Block-entity leak in both batched-explosion-removal code paths.** Writing directly into a
  `LevelChunkSection` (the PORT_SPEC-mandated performance optimization) bypassed vanilla's
  block-entity bookkeeping entirely - any chest/machine/reactor destroyed by an explosion left a
  stale `BlockEntity` registered at a position the section now reports as air, a real corruption/
  crash risk on the next tick and on world save. Fixed in both `ChunkBatchedBlockRemoval` (the
  vanillant engine, hit by every grenade/warhead/non-nuke bomb) and `NukeChunkBlockRemoval` (the
  nuke-tier path) by removing the block entity before the section write.
- **Registration dead code, recurring again**: 23 `ItemSwordAbility`-family swords and 8
  `ItemMultitoolPassive` rungs were fully built but never registered anywhere (confirmed by grep);
  wired both into `ModItems.java`. `EntityExplosionChunkloading`'s chunk-force-loading hooks were
  stubbed as no-ops behind a stale "no confirmed API" claim, when the same package's own
  `IChunkLoader` (built on the confirmed-real `ServerLevel#setChunkForced`) already solved it.
- **Assorted real behavioral bugs found by the subsystem review agents**: `BlockChargeBase`'s
  support-loss removal suppressed its own explosion cascade (a `safe=true` guard that CE's real code
  never applies there); `LaunchpadSoyuzBlockEntity` dropped its entire client-side interpolation
  branch; `Satellite.register()` passed `null` instead of `EnumSatType.RELAY`, breaking satellite-type
  lookup for the relay satellite specifically; `LaunchPad.explode()` (unlike its `LaunchPadLarge`
  sibling) never wrapped its launch call in `ModContext.DETONATOR_CONTEXT`; legacy `gun_b92`/`gun_b93`
  had their per-charge-level explosion tiers flattened to one generic blast instead of CE's real
  distinct entity/radius progression per charge level.

## Known gaps intentionally deferred to later phases

- **Chunk-batched block removal's write-side API is still not compiler-verified.** Both
  `ChunkBatchedBlockRemoval` and `NukeChunkBlockRemoval` document this explicitly in their own
  javadocs: the read-side `LevelChunkSection`/`Heightmap` APIs are confirmed against this port's own
  Neo Edition reference, but the exact write-side `setBlockState`/`setUnsaved`/light-engine-recompute
  call shapes are well-established Mojang-mapping knowledge, not independently confirmed by a real
  compiler in this sandbox. A real `./gradlew compileJava` pass should treat any error here as the
  first thing to check, not a surprise.
- **Casing items (`EnumCasingType` -> real `Item` per tier) and the Ammo Press recipe consuming
  them do not exist anywhere in this port.** Every one of the ~150 ammo `BulletConfig`/`Item` pairs
  across the 70-gun roster carries a code comment with CE's exact casing type + count for whoever
  builds this shared family - none of them call `.setCasing(...)` yet.
- **`ItemGunBaseNT` has no `defaultAmmo`/`setDefaultAmmo` mechanism.** Starter-ammo-on-craft is a
  real CE feature with no landing spot in this port's foundation code yet; every gun's intended grant
  is preserved as a comment.
- **`HbmPotion` (the `MobEffect` registration area) does not exist anywhere in this port**, blocking
  a recurring, already-cataloged set of small branches across `ArmorUtil`, `ContaminationUtil`,
  several power-armor sets, and a few melee abilities (PHOSPHORUS/BOBBLE) - each individually stubbed
  with a documented TODO, not a structural blocker for anything else.
- **`ChunkRadiationManager`, `SatelliteDetector`'s real payload entities (`EntityDeathBlast`/
  `EntityOrbitalLaser`/`EntityTom`), `ItemPoolsSatellite`, `PollutionHandler`, `AdvancementManager`,
  `EntityVortex`/`EntityRagingVortex`/`EntityBlackHole`, `EntityFireLingering`, and `ExplosionChaos`/
  `ExplosionNT`/`ExplosionThermo`** (the non-vanillant CE explosion-helper family) remain unported,
  each named explicitly at its exact call site rather than silently skipped - most are Phase 4
  world-simulation scope per PORT_SPEC's own phase boundaries.
- **`JetpackGlider`'s real flight engine (hover/thrust physics) is unported** - the item shell and
  fuel-tank contract are complete and correct, but wearing/inserting it currently grants no flight.
  Standalone chest-slot equipping-by-right-click for jetpacks is also unwired (CE's Forge-1.12
  `isValidArmor`/`getArmorModel` hooks have no independently-confirmed 1.21.1 equivalent found this
  session) - jetpacks are fully functional today only as armor-mod-slot inserts into another
  chestplate.
- **`elec_sword` and the 11 `meteorite_sword*` tiers remain unregistered** (need a new
  `ItemSwordAbilityPower`/`ItemSwordMeteorite` class respectively, judged out of scope for a
  review-only pass without compiler access) - a small, clearly-flagged follow-up.
- Texture/model assets: every Phase 3 GUI renders with plain filled panels, matching every prior
  phase's own documented gap - not a Phase 3 defect, tracked once for whichever phase starts asset
  porting.

## On "gradlew build green" as a per-phase gate

Unchanged from Phases 1-2: **this sandbox cannot run `gradlew` at all** (org egress policy blocks
`maven.neoforged.net`). Every package in Phase 3 was verified by static means - reading every
constructor/method signature against its real target class, checking every import resolves, and (for
every implement and review wave) a dedicated adversarial pass against CE source and, where behavior
was genuinely uncertain, a live GitHub source search of the real `neoforged/NeoForge` repository
(used twice this phase to definitively resolve two real API-shape questions: the `EventBusSubscriber`
bus-default behavior, and `LivingAttackEvent`'s nonexistence). This caught an unusually large number
of real, severe bugs for a single phase (see above) but is still not a substitute for a real
compiler; the next session or CI run with real network access should run `gradlew compileJava` as the
first order of business and treat any error not explained by this file or `docs/phase0/STATUS.md`/
`docs/phase1/STATUS.md`/`docs/phase2/STATUS.md` as a real regression, not another expected forward
reference.
