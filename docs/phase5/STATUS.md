# Phase 5 (Client & UX) — Status

Phase 5 followed the same research → implement → review → fix methodology as Phases 1-4, scaled to
this phase's size: **15 parallel research reports** (`docs/phase5/*.md`, ~6,740 lines, all read in
full before implementation was designed), then a **35-agent implement wave** (9 Foundation + 16
Content + 10 Review, run as three sequential barrier phases via the `Workflow` tool), then a
coordinator pass applying and de-duplicating every agent's `wiringSnippets` against the shared
aggregator files no individual agent was allowed to touch directly.

Before the implement wave launched, three small, fully-scoped fixes surfaced by the research reports
were applied directly (not worth spending an agent on): the `ModSoundTypes.java` compile bug (missing
`.get()` on two `DeferredHolder<SoundEvent,...>` fields, also converted to lazy factory methods per
this project's standing eager-holder-crash rule), the missing `remove(RemovalReason)` boss-bar cleanup
override on 4 of 5 boss entities (`EntityUFO`/`EntityHunterChopper`/`EntityBOTPrimeHead`/
`EntityQuackos`, matching `EntityMaskMan`'s existing one), and a dead keybind-pipeline bug
(`KeybindPacket.handleServer` never called `HbmPlayerAttachment.setKeyPressed`, silently disabling
every fueled jetpack's thrust, the 3 toggle keybinds, and a pipe tool's alt-mode branch).

**This sandbox cannot run `./gradlew` or launch a Minecraft client** (network policy blocks
`maven.neoforged.net`), so nothing in this phase is compile-verified or visually confirmed — every
claim below is from careful static reading, cross-checked against `upstream/hbm-ce` (behavior/content
ground truth) and `upstream/neo-edition` (API-shape corroboration only, pinned to the same
`neo_version=21.1.228`), plus a dedicated live-source-verification review pass (see below).

## What shipped

**273 files changed, +23,498/-906 lines** (one coordinator commit on top of the 35 agents' own work).

### Foundation (9 agents)

- **Sound registry dedup**: deleted `com.hbm.sound.ModSounds` (a full duplicate of
  `com.hbm.lib.HBMSoundHandler`, 159 colliding registry ids). `HBMSoundHandler` is now the sole
  `SoundEvent` registry; its 2 real non-`MainRegistry` call sites were migrated.
- **`com.hbm.render.loader.HbmObjModel`**: a hand-parsed runtime OBJ mesh loader (CE's
  `HFRWavefrontObject` port) — deliberately *not* built on NeoForge's `IGeometryLoader`/baked-model
  pipeline, since that pipeline targets static items/blocks, not multi-part TESR meshes redrawn every
  frame with per-part transforms; the class's own javadoc documents this judgment call.
- **`com.hbm.client.render.ClientEntityRenderers`**: bulk `EntityRenderers.register` pass across all
  23 `*EntityTypes.java` registries (94 concrete entity types) — previously **zero** entity renderers
  were registered anywhere in this port, a guaranteed client crash the instant any Phase 3/4 entity
  spawned near a client. This was the single highest-priority Phase 5 item.
- **`com.hbm.particle.ModParticleTypes`** + **`com.hbm.particle.HbmEffect`**: a custom `ParticleType`
  registry and the generic named-VFX broadcast/dispatch system (CE's `HbmEffectNT`/
  `AuxParticlePacketNT` equivalent), wiring ~24 already-committed Phase 3/4 particle-broadcast call
  sites that previously did nothing.
- **`com.hbm.client.render.ConstantRenderSweep`**: the `RenderLevelStageEvent`-based replacement for
  CE's `ClientProxy.renderingConstant`/`IConstantRenderer` force-render-every-frame mechanism (used by
  mushroom clouds, black holes, and other far-clipping VFX).
- **JEI**: `build.gradle`/`gradle.properties` dependency (`mezz.jei:jei-1.21.1-neoforge(-api):19.25.0.325`)
  + `com.hbm.compat.jei.HbmJeiPlugin` skeleton.
- **Armor/BEWLR rendering frameworks**: `IClientItemExtensions.getGenericArmorModel` (armor) and
  `.getCustomRenderer()`/`BlockEntityWithoutLevelRenderer` (3D item-in-hand models) registration
  scaffolding.

### Content (16 agents)

- **RBMK reactor** (4 `BlockEntityRenderer`s): control-rod insertion depth, fuel-column Cherenkov
  glow, 15×15 console heatmap, autoloader piston — all reading fields already real and networked since
  Phase 2.
- **Explosion/mushroom-cloud VFX**: `EntityNukeTorex`'s full cloudlet simulation (ported onto the new
  `ParticleEngineNT`/`EngineHandler` batch-rendering primitive rather than CE's raw-GL VBO path) plus
  the HUD nuke-flash/nuke-shake overlay (wall-clock-timed, matching CE's real fade curve, including the
  "fake a real vanilla hurt animation" trick that actually produces the shake sensation); `CloudFleija`/
  `CloudSolinium`/`CloudTom`/`EMPBlast` simple expanding-mesh renderers.
- **~30 boss/vehicle/mob renderers**: gravity-well family (black hole/vortex/raging vortex/quasar,
  one shared `instanceof`-branching class matching CE's own design), MIRV, bomber (dual Dornier/B-29
  airframes on a newly-added synced `STYLE` byte), death blast, orbital laser, MaskMan, UFO (with a
  newly-added synced abduction-beam flag), hunter chopper (47-box hand-transcribed `Model<T>`), BOTPrime
  head/body (worm boss), cyber/taint/tesla crab family, duck/Quackos (vanilla `ChickenRenderer` reskins),
  RAD beast.
- **Weapon gun rendering**: the `BusAnimationSedna` keyframe engine ported in full (all ~30 Blender
  easing-curve functions, line-by-line from CE, not a placeholder), 3 guns (SPAS-12, Uzi, AM-180) fully
  wired end-to-end with real per-part rendering and real CE animation data, all 12 CE animation JSON
  files copied into the resource tree.
- **Armor model rendering**: 15 four-piece OBJ-rigged powered-armor sets (62 items, live per-part pose
  sync so limbs swing with the walk cycle) + 2 hand-transcribed box-model sets (gas mask family) + the
  jetpack-worn cosmetic model.
- **HUD overlays**: Geiger/digamma radiation bars, the HEV suit's full ASCII-art status readout, and the
  gun ammo/durability HUD (via a new generic `ItemHudDispatcher`, since CE had this logic inlined in one
  giant event handler with no reusable dispatch point).
- **JEI**: real `IRecipeCategory` implementations for all 13 of this port's actual recipe types
  (shredder, breeder, assembler, crystallizer, centrifuge, gas centrifuge, cyclotron, SILEX,
  electrolyser, mixer, refinery, chemical plant, RBMK fuel recycling).
- **Sound wiring**: ~14 new real call sites closing confirmed gameplay-vs-silence gaps (a completely
  silent gun, a machine using a vanilla stand-in sound after its real one had since landed).
- **Lang file**: `ModLanguageProvider` grew from 149 to 1,036 lines — all 146 real CE achievement keys
  (verified against what the 65 advancement JSONs actually reference, not just the research estimate),
  161 item + 59 block display names, machine GUI titles.
- **Custom particle content**: real, CE-behavior-transcribed `Particle` subclasses for all 17
  `ModParticleTypes` entries, replacing every placeholder.
- **Advancements**: all 65 CE advancement JSONs ported 1:1 to `data/hbm/advancement/*.json` (singular,
  confirmed-correct 1.21.1 path), schema-translated (icon `item`→`id`, predicate `item`→`items`, no
  metadata subtypes), scripted-diff-verified against the CE originals field-for-field.
- **Recipe datagen**: a real `ModRecipeProvider`, 260 new vanilla-crafting recipes (29 tool + 210
  mineral + 21 armor) — this port's crafting-table recipe count went from 1 to 261 (CE's real corpus is
  ~1,900+; see Known gaps).

### Review (10 agents)

Two dedicated cross-cutting re-scans (the recurring eager-`DeferredHolder`-crash pattern, and
`@EventBusSubscriber` `bus=MOD` correctness) found **zero new instances** across the whole wave — every
Foundation/Content agent had already internalized both standing rules. A live-source API verification
pass (WebFetch against `github.com/neoforged/NeoForge`, branch `1.21.1`) confirmed every previously-
unverified API shape this phase leaned on and found **one real bug**: `RenderLevelStageEvent#getPartialTick()`
returns `DeltaTracker`, not `float`, in `ConstantRenderSweep` — fixed in place. Per-subsystem reviews
found and fixed several "renderer class built but its registration line was never swapped from the
Foundation-wave fallback" bugs directly in `ClientEntityRenderers.java` (11 entities), plus 2 real
missing-synced-field bugs (`EntityUFO.beam`, `EntityBomber.STYLE`) that would have made their new
renderers silently non-functional client-side.

## Coordinator wiring pass

Every agent was blocked from directly editing `MainRegistry.java`, `ClientModRegistry.java`,
`ModItems.java`, `ModBlocks.java`, `HbmNetwork.java`, `ClientProxy.java`, `ModAttachments.java`, and
`build.gradle` (to avoid 35-way merge conflicts), and instead reported the exact edit needed as a
`wiringSnippets` entry. ~40 such entries (heavily overlapping — several tasks and reviewers
independently converged on the same real gaps) were read, cross-checked against the files' actual
current state, de-duplicated, and applied in one commit:

- `MainRegistry.java`: removed the dangling `ModSounds` import/call (a real compile break the review
  wave flagged as CRITICAL — its own fix couldn't reach this file), added `ModParticleTypes.register`.
- `ClientModRegistry.java`: wired `ClientEntityRenderers.registerAll()`, the 6 RBMK
  `BlockEntityRenderers.register(...)` calls, and `HbmEffect.registerHandlers()` into the (previously
  empty) `onClientSetup` lambda — 3 independent tasks had each concluded their own call belonged there.
- `ModItems.java`: registered `AchievementIconItems` (10 decorative achievement-icon items + 1 "nothing"
  item, backing 11 of the 65 advancement JSONs' icons).
- `packet/HbmNetwork.java`: registered `HbmEffectPacket` (S2C) and `LaunchPadRustedControlPacket` (C2S).
- `ClientProxy.java`: added the missing `effectNT` override (CE's `ClientProxy.effectNT` equivalent —
  `ServerProxy`'s no-op base was correct, but nothing overrode it client-side, so already-client-side
  callers like `EntityMist` silently did nothing).
- `ClientEntityRenderers.java`: 3 renderer classes (`MirvRenderer`, `DeathBlastRenderer`,
  `OrbitalLaserRenderer`) were built and reported as wiring snippets but not yet applied when the review
  wave's own similar snippets landed for other entities — wired in during this pass.

## Known gaps / deferred scope

- **CE's Collada skeletal-animation system** (`com.hbm.animloader`) has **no ported equivalent** —
  blocks 12 door TESRs and the jetpack glider's opening animation. `BusAnimationSedna` (a separate,
  non-Collada system CE uses specifically for gun animation) was fully ported instead and is unrelated;
  this remains an open, unresolved project-wide design decision from earlier phases.
- **Bulk texture/model asset migration**: CE ships ~6,965 PNG/OBJ assets; this port now has a handful
  (2 overlay PNGs, 12 gun animation JSONs) plus every renderer written to reference the *correct*
  expected resource path. No Phase 5 area claimed ownership of the bulk copy — every report that found
  this gap (at least 5 of the 15) flagged it as unowned. **This is the single largest blocker to any of
  Phase 5's rendering work being visually verifiable** — every OBJ-model renderer will throw or draw a
  missing-texture checkerboard until it lands.
- **Vanilla-crafting recipe corpus**: CE's real corpus is ~1,900+ recipes across `ToolRecipes`/
  `ArmorRecipes`/`MineralRecipes`/`ConsumableRecipes`/`PowderRecipes`/`WeaponRecipes`/`ExclusiveRecipes`;
  this port now has 261 (up from 1). Explicitly NOT attempted: CE's `RodRecipes` (a different mechanism —
  RBMK/breeding/ZIRNOX fuel-rod crafting already has its own internal recipe system) and the 7
  `com.hbm.crafting.handlers.*` dynamic NBT-aware recipes (need genuine new `RecipeType`/
  `RecipeSerializer` Java classes, scoped as a harder follow-up).
- **68+ registered gun items** still render with vanilla's default flat icon — only SPAS-12/Uzi/AM-180
  are wired through the new BEWLR/animation pipeline. 9 more guns' animation JSON already exists on disk
  (unblocking a future pass with zero further asset work) but their Java renderer classes were not
  written.
- **`EntityBombletZeta`/`EntityBurningFOEQ`**: confirmed genuinely unported anywhere in this repo (not a
  rendering gap — there is no entity class to render). `EntityBomber.java` already carries a
  `TODO(unowned-entity, EntityBombletZeta)` comment. Low priority.
- **RBMK Heater/Outgasser**: both have real, registered block entities but no `MenuType`/Menu/Screen at
  all — confirmed missing both, not just a Screen (Phase 2-shaped architecture work, out of Phase 5's
  Screen-only scope). No current task owns this.
- **Liquidator armor** (4 pieces) and the **jetpack-worn model**: correctly left unregistered/inert.
  CE has no `ModelArmorLiquidator`; the jetpack-worn model is fully built and registered but has no
  live effect until standalone-jetpack `Equippable`/data-component support lands (a pre-existing,
  separately-documented gap this phase didn't own).
- **22 pre-existing dangling imports** (not introduced this phase — predate Phase 5 entirely) were
  found during the wiring-completeness sweep across `com.hbm.interfaces`/`com.hbm.api`/
  `com.hbm.capability`/`com.hbm.handler`/`com.hbm.blocks`/`com.hbm.inventory.fluid`/`com.hbm.items`/
  `com.hbm.lib.internal` — e.g. `IBulletUpdateBehavior` imports a never-ported `EntityBulletBase`. Means
  **the project will not compile end-to-end today even with every Phase 5 gap above closed** — worth a
  dedicated triage pass, recommended for Phase 6.
- **JEI's `RecipeManager`-timing dependency** for the 3 JSON-datapack recipe categories (shredder/
  breeder/assembler) is standard JEI-plugin practice but has zero precedent in either reference repo, so
  is flagged unverified rather than assumed correct.
- Two of RBMK's TESR-panel blocks (`RBMKConsoleHeatmapRenderer`'s bezel/frame geometry and item-in-hand
  variant) and several other narrow items noted inline in individual agents' own `notes` were left as
  documented, narrow, low-risk simplifications rather than exhaustively listed here — see the Phase 5
  research reports and this wave's own git history for full detail.

## Verification caveats

Nothing in this phase was compile-checked (`./gradlew` unavailable) or visually confirmed (no client
launch available). Confidence comes from: careful CE-source cross-referencing (every new class cites
exact CE file/line), a dedicated live-NeoForge-source API verification pass, two clean cross-cutting bug
re-scans, and per-subsystem review passes that read real code rather than trusting prior agents'
self-reports. A real compile pass would very likely surface issues a text-only review cannot (a wrong
import resolving to a class of different arity, a generics mismatch) — the 22 pre-existing dangling
imports above are proof this class of issue exists in this tree already.
