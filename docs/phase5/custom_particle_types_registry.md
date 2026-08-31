# Phase 5 research: Custom particle-type registry

**Area:** `custom_particle_types_registry` — does this port register any custom `ParticleType<?>` at
all, what does CE's real particle catalog actually contain, and (evidence-based, not exhaustive) which
particle types are most load-bearing to register first.

**Scope boundary vs. the sibling `particle_engine_and_generic_vfx` report:** that report owns the
*rendering substrate* decision (instanced-batching replacement, the `AuxParticlePacketNT`/`HbmEffectNT`
generic network-broadcast redesign, `ParticleEngineNT`-style custom particle lists) and already confirms
the registry API shape exists in passing (its lines 288-298). This report goes one level deeper on the
registry specifically: exhaustive confirmation there is currently zero registry in this port, the real
CE catalog size/shape, a load-bearing prioritization grounded in actual committed TODO comments (not
CE's full 101-file catalog), the exact client-asset shape a registered type additionally requires, and one
concrete NeoForge event-bus wiring gotcha this port's own prior phase already discovered and neo-edition
appears to have gotten wrong.

## Method

Read directly, with exact paths/line counts cited inline:
- This port's own `src/main/java/com/hbm/**` — repo-wide grep for `DeferredRegister<ParticleType`,
  `ParticleType<`, `ParticleTypes.`, `com/hbm/particle` (package does not exist), plus every TODO comment
  mentioning "particle" (30 hits across ~27 files) to build the load-bearing ranking from real evidence.
- `upstream/hbm-ce/src/main/java/com/hbm/particle/**` (87 files) + `particle_instanced/**` (8 files) +
  `particle/helper/HbmEffectNT.java` (1503 lines, read in full) — CE 1.12.2 source, sole source of truth
  for the effect catalog and which particle class backs which named effect.
- `upstream/hbm-ce/src/main/resources/assets/hbm/textures/particle/` — the real texture catalog (37
  files) that any registered type ultimately needs art for.
- `upstream/neo-edition/src/main/java/com/hbm/particle/NtmParticleTypes.java` (81 lines, read in full)
  and `com/hbm/main/NuclearTechModClient.java` (`onTextureAtlasStitched`/`registerParticles`, lines
  701-748) — used **only** to confirm the real NeoForge 1.21.1 registration API shape, per this project's
  standing rule that neo-edition is never a source of visual/behavioral truth.
- `upstream/neo-edition/src/main/resources/assets/hbmsntm/particles/*.json` (10 files) — confirms the
  extra client-resource shape a registered particle type needs beyond the Java registration.
- This port's own `src/main/java/com/hbm/sound/ModSounds.java` (registration-convention precedent) and
  `src/main/java/com/hbm/main/ClientModRegistry.java` (the confirmed correct client-bootstrap class and
  its own documented `@EventBusSubscriber` bus-default finding) — read in full.

## Headline finding: the registry does not exist, confirmed exhaustively

Repo-wide grep across `src/main/java/com/hbm` for every plausible registration surface:

```
DeferredRegister<ParticleType   → 0 hits
ParticleType<                   → 0 hits
com/hbm/particle (package)      → does not exist (no directory)
```

`ParticleTypes.` (the **vanilla** particle-type enum, not this port's own registry) appears in exactly 8
files, all spawning stock vanilla effects directly and all pre-existing from earlier phases, not this
one:

| File | Vanilla type(s) used |
|---|---|
| `packet/toclient/RadFogPayload.java:69` | `ParticleTypes.CLOUD` — explicit documented stand-in, see below |
| `blocks/generic/BlockSmolder.java:35,38` | `LAVA`, `FLAME` |
| `blocks/generic/WasteEarth.java:68,71` | `FLAME`, `SMOKE` |
| `explosion/vanillant/standard/ExplosionEffectStandard.java:49,51,79` | `EXPLOSION_EMITTER`, `EXPLOSION`, `SMOKE` |
| `entity/item/EntityDroneBase.java:144-147` | `SMOKE` (drone thrusters, x4) |
| `entity/item/EntityTNTPrimedBase.java:132` | `SMOKE` |
| `entity/projectile/EntityThrowableNT.java` | vanilla (grep hit, not read in full — out of scope) |
| `items/tool/ItemDiscord.java` | vanilla (grep hit, not read in full — out of scope) |

`ExplosionLarge.spawnParticles`/`spawnParticlesRadial`/`spawnFoam`/`spawnBurst`/`spawnShock`
(`src/main/java/com/hbm/explosion/ExplosionLarge.java:52-70`) look like they might be live custom-particle
call sites from their names and their 3 live callers (`buster`/`explode`/`explodeFire`, lines 161/182/196)
— **they are documented no-op stub bodies**, each literally just `// TODO(AuxParticlePacketNT, Phase 5)`.
This port's *only* currently-executing particle-spawn code, anywhere, is the 8-file vanilla-`ParticleTypes`
list above. **Zero custom `Particle` subclasses, zero custom `ParticleType`s, zero `ParticleProvider`s
exist in this codebase today.** This confirms and extends `RadFogPayload.java`'s own javadoc claim
("no custom particle-type registry exists at all in this pass — confirmed by repo-wide search") — that
claim is accurate, and this report is the actual repo-wide search backing it up.

## CE's real particle catalog

`upstream/hbm-ce/src/main/java/com/hbm/particle/` contains **87 `.java` files** (including 5 subpackages:
`book/`, `bullet_hit/`, `helper/`, `lightning_test/`, `vortex/`), plus a sibling `particle_instanced/`
package with **8 files** (GPU-instanced variants of 6 of the base-package classes — owned by the sibling
report, not re-litigated here). Every particle class extends `net.minecraft.client.particle.Particle`
directly (confirmed by grepping `class Particle.*extends` across all 60 top-level files — every one
extends `Particle`, `ParticleRotating`, `ParticleFXRotating`, `ParticleFirstPerson`, `ParticleLayerBase`,
or a same-package sibling, never anything registry-shaped). This is expected: **Forge 1.12.2 has no
`ParticleType`/registry concept at all** — that's a Minecraft-1.13+ addition — so CE's whole catalog is
"instantiate a `Particle` subclass directly and hand it to `Minecraft.getMinecraft().effectRenderer`",
dispatched across the network by a hand-rolled enum (see next section), not by any Forge registry.
`com.hbm.particle.EnumHbmParticles` (`upstream/hbm-ce/.../EnumHbmParticles.java`, 5 lines) is a vestigial
single-value enum (`PARTICLES`) with no fields or logic — not a real registry, despite the name; do not
mistake it for one.

**Texture catalog** (`upstream/hbm-ce/src/main/resources/assets/hbm/textures/particle/`, 37 files):
`particle_base.png`, `particles.png`, `particlesmoke.png`, `d_smoke1-8.png` (8 frames), `contrail.png`,
`casings.png`, `debug_fluid.png`, `debug_power.png`, `explosion.png`, `explosion_bf.png`, `flare.png`,
`fog.png`, `hadron.png`, `haze.png`, `meat.png`, `metal.png`, `rbmk_fire.png`, `rbmk_jet_steam.png`,
`rbmk_mush.png`, `shockwave.png`, `skeleton.png`, `skeleton_blood.png`, `skoilet.png`,
`skoilet_blood.png`, `slime.png`, `vortex_beam.png`, `vortex_beam2.png`, `vortex_beam_circle_2.png`,
`vortex_flash.png`, `vortex_hit.png`. **None of these exist anywhere under this port's
`src/main/resources`** (checked: `find src/main/resources -path "*textures/particle*"` → 0 hits) — asset
porting for this area has not started at all, by any phase. This is a hard, separate blocker from the
Java-side registry work (see "Blocked on" below) — owned by whoever does the general asset-porting pass,
not by this report's registry design.

**Named-effect catalog**: `com.hbm.particle.helper.HbmEffectNT` (`upstream/hbm-ce/.../HbmEffectNT.java`,
1503 lines, read in full) is CE's actual *dispatch* enum — **87 named effect constants**
(`CasingNT` through `Giblets`, enum declared lines 60-76), each wired to a client-side handler lambda by
`registerClientHandlers()` that the `AuxParticlePacketNT` network payload invokes by ordinal. This is the
real "particle type catalog" from a gameplay-call-site perspective, and it is the sibling report's
primary subject (the packet/dispatch redesign) — but it's also the ground truth this report uses to rank
which *underlying* `Particle` classes are worth a registry entry first, below.

## Confirmed real NeoForge 1.21.1 registration API shape

**Not independently verified against a real jar or a running client** (this sandbox cannot run
`./gradlew` — network policy blocks `maven.neoforged.net` — and there is no NeoForge sources/jar cached
anywhere on disk: checked `~/.gradle/caches` and found only the `moddev-gradle` plugin jar, no NeoForge
artifact). Everything below is corroborated by **neo-edition's own compiling code** (per this project's
standing rule, permitted for API-shape confirmation) plus this port's own already-established
`DeferredRegister`/`@EventBusSubscriber` conventions, cross-checked against each other for consistency.

**1. The registry itself** — `net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE` is a real
built-in registry; neo-edition registers into it exactly the way this port already registers sounds
(`ModSounds.java` uses `BuiltInRegistries.SOUND_EVENT` the same way):

```java
// upstream/neo-edition/.../particle/NtmParticleTypes.java:21
public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
    DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, NuclearTechMod.MODID);
```

Two registration shapes, both used live in that same file:
- **No-argument particle** (the common case — 21 of 27 entries): `SimpleParticleType`, via
  `PARTICLE_TYPES.register(name, () -> new SimpleParticleType(overrideLimiter))`
  (`NtmParticleTypes.java:53-55`). `overrideLimiter` is CE's rough equivalent of "ignore the
  client's particle-count/distance culling" — set `true` for a handful of always-visible entries
  (`DIGAMMA_SMOKE`, `SKELETON`, both debug types, `SPARK`, `TOM_BLAST`), `false` for everything else.
- **Typed particle with server-sent options** (6 of 27 entries — `MISSILE_CONTRAIL`, `EXHAUST_SOYUZ`,
  `EXHAUST_METEOR`, `SWEAT`, `VOMIT_NORMAL/BLOOD/SMOKE`, `AMAT`, `RBMK_FLAME`, `COOLING_TOWER`,
  `GAS_FLAME`): a custom `ParticleType<T>` anonymous subclass overriding `codec()`/`streamCodec()`,
  backed by a custom `ParticleOptions` implementation (neo-edition's `NbtParticleOptions` — a generic
  NBT-blob-carrying options type, `com.hbm.particle.vanilla.NbtParticleOptions`, used as the catch-all for
  every effect that needs arbitrary per-spawn data the way CE's NBT-keyed `HbmEffectNT` handlers do).

**2. The client-only provider-registration event** — `net.neoforged.neoforge.client.event.
RegisterParticleProvidersEvent`, with two confirmed methods, both used live at
`NuclearTechModClient.java:719-748`:
- `event.registerSpecial(ParticleType<T>, ParticleProvider<T>)` — for a provider that builds its own
  sprite/animation state (used for 21 of 27 entries, e.g. `DIGAMMA_SMOKE`, `DEBRIS`, `SPARK`,
  `RADIATION_FOG`, `AMAT`, `RBMK_FLAME`, `COOLING_TOWER`).
- `event.registerSpriteSet(ParticleType<T>, SpriteSet-consuming-factory)` — for a provider whose visuals
  come from the particle texture atlas via a `SpriteSet` NeoForge builds and hands in (used for `DEAD_LEAF`,
  `AURA`, `POWER_DEBUG`, `FLUID_DEBUG`, `GAS_FLAME`).

**3. Sprite-atlas participation** — a `registerSpriteSet`-shaped type additionally needs its sprites
present in the vanilla particle atlas, obtained from a `TextureAtlasStitchedEvent` handler gated on
`event.getAtlas().location().equals(TextureAtlas.LOCATION_PARTICLES)`
(`NuclearTechModClient.java:701-717`) — confirmed real, this is a `NeoForge` client event, not a mod-bus
one, and neo-edition subscribes it correctly (it fires on the game bus, matching the class's actual
`@EventBusSubscriber(value = Dist.CLIENT)` default — see the bus-default discussion below for why this
one being correct and `registerParticles` likely *not* being correct is not a contradiction).

**4. The extra client-resource JSON** — every registered particle type needs a matching
`assets/<modid>/particles/<name>.json` (confirmed real, 10 example files under
`upstream/neo-edition/src/main/resources/assets/hbmsntm/particles/`), minimally:
```json
{ "textures": [ "hbmsntm:particle_base" ] }
```
(`cooling_tower.json`, read in full — 5 lines). This is a **third** artifact a fully-wired custom
particle type needs, beyond the Java `ParticleType` registration and the `ParticleProvider`: (a) the
Java registry entry, (b) the `assets/<modid>/particles/<name>.json` texture-list file, (c) the actual
PNG(s) it points at under `assets/<modid>/textures/particle/`. Note neo-edition's modid is `hbmsntm` —
**this port's modid is `hbm`** (`MainRegistry.MODID`, confirmed at
`src/main/java/com/hbm/main/MainRegistry.java:56`) — every namespace in any copied-shape file must be
substituted, not copied verbatim.

## Risk: `@EventBusSubscriber` bus default — a gotcha this port's own Phase 0 already caught, that neo-edition appears to have missed

This port's `ClientModRegistry.java` (`src/main/java/com/hbm/main/ClientModRegistry.java:44-49`) carries
a load-bearing comment from Phase 0, worth quoting in full since it is the single most important gotcha
for whoever wires this registry:

> `bus = Bus.MOD` required: both `FMLClientSetupEvent` and `RegisterMenuScreensEvent` implement
> `IModBusEvent` and only fire on the mod bus — `@EventBusSubscriber`'s `bus()` defaults to `Bus.GAME`
> and does not auto-detect `IModBusEvent` (confirmed against real NeoForge 1.21.1 source and
> FancyModLoader's `EventBusSubscriber` javadoc). Without this, no Phase 2+ machine screen would ever
> actually bind to its `MenuType`.

`RegisterParticleProvidersEvent` is the same *category* of event as `RegisterMenuScreensEvent` — a
one-shot client-setup "register your factories" event fired during mod loading, not a per-tick game
event — so by the same rule it almost certainly also implements `IModBusEvent` and needs the same
explicit `bus = EventBusSubscriber.Bus.MOD` override. **Neo-edition's own
`NuclearTechModClient.java:128` is annotated `@EventBusSubscriber(value = Dist.CLIENT)` with no `bus=`
override at all** — checked every `@EventBusSubscriber` usage in neo-edition (13 total, repo-wide grep)
and **none of them ever specify `bus=`**, meaning every one defaults to `Bus.GAME`. If the reasoning
above is right, neo-edition's own `registerParticles(RegisterParticleProvidersEvent event)` handler
(lines 719-748) **never actually fires**, and its entire particle-provider wiring is silently dead code
that only looks correct by reading it — it would need `Minecraft` running to notice the particles never
render. This is flagged as **unverified — this sandbox cannot launch a client to confirm neo-edition's
handler is actually dead** — but it is strongly grounded in this project's own previously-confirmed fact
about the same annotation on the same category of event, not speculation, and it fits the project's
default posture: neo-edition compiles but is "frequently incomplete or diverges" — this would be exactly
that kind of divergence, invisible without running the game, silently reproduced by anyone who copies its
`@EventBusSubscriber` line verbatim instead of following this port's own `ClientModRegistry.java`
precedent. **Recommendation: add the `registerParticles` handler as a new `@SubscribeEvent` static method
directly inside `ClientModRegistry.java`**, which already carries the correct
`@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)`
class-level annotation — do not create a new class copying neo-edition's `NuclearTechModClient` shape for
this.

## Prioritized list: which particle types are most load-bearing to register first

Ranked by how many **already-committed Phase 3/4 TODO comments in this port** name the effect, not by
CE's full catalog size (the task's own instruction — the full catalog is 87 files/37 textures, most of
which nothing in this port references yet). Evidence: repo-wide grep for "TODO.*particle" (27 files) plus
targeted greps per named effect, both run against `src/main/java/com/hbm` this session.

### Tier 0 — needs *zero* new registry entries, unblocks 7 files immediately

**The entire `Jetpack`/`Jetpack_BJ`/`Jetpack_DNS`/`bnuuy` family** (CE's `HbmEffectNT.java:931-1119`,
read in full) turns out to use **exclusively vanilla particle classes** — this is the single biggest
finding of this report, because it means 7 files' outstanding "TODO(particle system)" comments are
*already unblocked* by nothing more than calling `level.addParticle(ParticleTypes.X, ...)` with CE's own
math, no custom registry work required at all:

| Effect | CE particle classes used | Vanilla 1.21.1 equivalent |
|---|---|---|
| `Jetpack` (normal thrust) | `ParticleFlame.Factory` ×2, `ParticleSmokeNormal.Factory` ×2 | `ParticleTypes.FLAME`, `ParticleTypes.SMOKE` |
| `Jetpack_BJ` | `ParticleBlockDust.Factory` (scorch trail), `ParticleRedstone.Factory` ×2 (tinted dust) | `ParticleTypes.BLOCK` (needs `BlockParticleOption`), `ParticleTypes.DUST` (needs `DustParticleOptions`) |
| `Jetpack_DNS` | same shape as `Jetpack_BJ`, different tint | same |
| `bnuuy` (`ArmorDiesel`) | `ParticleSmokeNormal.Factory` ×2 | `ParticleTypes.SMOKE` |

Affected port files, all currently carrying a `// TODO(particle system): CE spawns a HbmEffectNT.*
AuxParticlePacketNT here` comment: `items/gear/JetpackVectorized.java:51`,
`items/gear/JetpackRegular.java:45`, `items/gear/JetpackBreak.java:49`,
`items/gear/JetpackBooster.java:53`, `items/armor/ArmorBJJetpack.java:52`,
`items/armor/ArmorDNT.java:75`, `items/armor/ArmorDiesel.java:69`. **Recommendation: implement these 7
directly with vanilla `ParticleTypes`, independent of and before any custom registry work** — this is
pure "safe to build now," zero blockers, and closes 7 named gaps immediately. (Whether the *networking*
side — client-only local particles vs. a broadcast packet other players see — belongs to this report or
the sibling one: broadcast-vs-local is the sibling report's `AuxParticlePacketNT` redesign; this report
only confirms the particle *types themselves* need no registry.)

### Tier 1 — high call-site count, genuinely needs new custom registry entries

1. **`PlasmaBlast`** (`ParticlePlasmaBlast`, `HbmEffectNT.java:449-454`) — referenced by name in **3
   files, 6 distinct call sites**: `items/weapon/sedna/content/XFactoryEnergy.java:219,322`,
   `items/weapon/sedna/content/XFactory12ga.java:219,283,304`,
   `entity/projectile/LegacyMobBulletConfigs.java:293`. Highest raw reference count of any single named
   effect after the Tier-0 vanilla family. A genuinely custom color-tinted billboard cloud (constructor
   takes `r,g,b,pitch,yaw` — `HbmEffectNT.java:450-451`), needs a real `SimpleParticleType` +
   `ParticleProvider`. Not yet wired in neo-edition either (`PlasmaBlastParticle.java` file exists there
   but is **absent** from `NtmParticleTypes`'s registered list — confirmed by grep, it is not among the 27
   entries) — so this is a genuine gap in both projects, not something to copy from neo-edition.

2. **`Spark`/`Tau`** (`ParticleHbmSpark` + `ParticleHadron`, `HbmEffectNT.java:841-878,1435-1441`) —
   backs the generic muzzle-flash/beam VFX referenced by name (as "particle burst"/"beam effect") in
   **6 turret block-entity files**: `blockentity/turret/TurretFritzBlockEntity.java:141`,
   `TurretTauonBlockEntity.java:105` (explicitly "Tau" beam), `TurretMaxwellBlockEntity.java:128`,
   `TurretHowardBlockEntity.java:148`, `TurretHowardDamagedBlockEntity.java:86`,
   `TurretSentryBlockEntity.java:139`, `TurretChekhovBlockEntity.java:80`,
   `TurretJeremyBlockEntity.java:91` (8 files total — some described generically as "muzzle-flash burst,"
   likely `Spark` given CE's own bullet-impact code reuses `Spark` for exactly this purpose, e.g.
   `HbmEffectNT.java:1252`). `SPARK` **is** already registered in neo-edition's `NtmParticleTypes`
   (`overrideLimiter=true`) with a real `SparkParticle.Provider` wired — this one *is* a safe shape to
   follow from neo-edition. `Hadron` (backs `Tau`'s beam-core flash) is **not** registered in neo-edition
   despite `HadronParticle.java` existing there — same "file exists, never wired" gap as `PlasmaBlast`.

3. **Generic smoke-burst family** (`Smoke_Cloud`/`Smoke_Radial`/`Smoke_RadialDigamma`/`Smoke_Shock`/
   `Smoke_ShockRand`/`Smoke_Wave`, backed by `ParticleExSmoke`/`ParticleDigammaSmoke`,
   `HbmEffectNT.java:165-291`) — the generic "explosion smoke plume" effect, referenced (via the
   catch-all `AuxParticlePacketNT`/"networked particle burst" phrasing rather than by exact constant
   name) across **`explosion/ExplosionLarge.java` (5 stub methods)**, `explosion/ExplosionNukeSmall.java:32`,
   `explosion/ExplosionChaos.java:363,368`, `entity/logic/EntityNukeExplosionMK3.java:249,313` — CE's most
   reused generic smoke shape, backing nearly every explosion in the game. `ParticleExSmoke` has **no**
   neo-edition file at all under the plain (non-`_instanced`) name, though `particle/ParticleExSmoke.java`
   does exist there — check its registration status before assuming it's wired; it was **not** found in
   `NtmParticleTypes`'s 27-entry list either.

### Tier 2 — lower file-count but high visual significance (nuke/mushroom-cloud family)

**`Muke`/`TinyTot`/`BF` family** (`ParticleMukeWave`, `ParticleMukeFlash`, `ParticleMukeCloud`,
`ParticleMukeCloudBF`, `HbmEffectNT.java:392-435,1120-1123`) plus the standalone toroidal-convection
system named directly in `entity/effect/EntityNukeTorex.java:82` ("CE's entire client-side cloudlet
particle simulation — toroidal convection"). Only 2-3 direct file references
(`items/weapon/sedna/content/XFactoryEnergy.java:322` for `Muke`, `EntityNukeTorex.java:82`,
`entity/missile/EntityMissileShuttle.java:32` for the generic "mushroom-cloud particle burst" phrasing),
but this is the mushroom-cloud/nuke-detonation visual — arguably the single most iconic VFX in the whole
mod, worth prioritizing on significance even though the raw TODO-count signal ranks it below Tier 1. None
of the 4 classes are in neo-edition's registered 27, though the `.java` files exist there unwired
(`MukeCloudParticle.java`, `MukeFlashParticle.java`, `MukeWaveParticle.java`, `MukeCloudBFParticle.java`).

### Tier 3 — single/double reference, smaller scope

- **`Tower`** (`ParticleCoolingTower`, `HbmEffectNT.java:911-930`) — cooling-tower steam/lift-plume,
  referenced at `blockentity/machine/LaunchpadSoyuzBlockEntity.java:163` and generically at
  `entity/effect/EntityMist.java:189` ("Tower" broadcast). **Already fully wired in neo-edition**
  (`COOLING_TOWER` registered + `CoolingTowerProvider` in `registerSpecial` — the one custom effect in
  this whole report's evidence set that neo-edition actually finished end-to-end) — safe to copy the
  shape directly, modulo the bus-default gotcha above.
- **`GasFlame`** (`ParticleGasFlame`) — referenced at `explosion/ExplosionLarge.java:65`
  ("`TODO(ParticleUtil.spawnGasFlame, Phase 5)`") — 1 file. Registered in neo-edition
  (`GAS_FLAME`, sprite-set shape).
- **Ground-fire** (`entity/effect/EntityFireLingering.java:144`, CE's
  `FlameCreator.composeEffectClient`) — 1 file, not yet checked against `FlameCreator`'s real
  implementation in this session; likely resolves to vanilla `FLAME`/`SMOKE` like the Jetpack family
  (same helper-class family, `com.hbm.particle.helper.FlameCreator`) but this was not confirmed by
  reading `FlameCreator.java` directly this session — flag as open, not assumed.
- **Legacy bullet trail** (`entity/projectile/LegacyMobBulletConfigs.java:152`, "5-particle Flame
  trail") — 1 file, almost certainly vanilla `FLAME` per CE's own description ("client-side... Flame
  trail" — capital-F Flame strongly implies `ParticleFlame`, the same vanilla-backed class as the
  Tier-0 Jetpack family).
- **`RadFog`** (`ParticleRadiationFog`) — already has a *documented, shipping* vanilla-`CLOUD`
  substitute in `packet/toclient/RadFogPayload.java:34-36,69` with an explicit forward-reference to this
  exact future pass. Lowest urgency of anything in this list: the game is not visually broken without it,
  unlike the Tier 0/1 items which are currently fully silent (no VFX at all, not even a placeholder).
  Registered in neo-edition (`RADIATION_FOG`).

### Not yet surfaced by any committed TODO (informational only, not ranked)

CE's `bullet_hit` subpackage (`ParticleBloodParticle`, `ParticleBulletImpact`, `ParticleHitDebris`,
`ParticleMobGib`, `ParticleSmokeAnim` — backing `HbmEffectNT.BulletImpact`/`Giblets`, a large chunk of
`HbmEffectNT.java:1152-1298,1442+`) has **no committed TODO comment anywhere in this port** referencing
it by name — the one incidental hit (`interfaces/IBulletImpactBehavior.java`) is an unrelated
block-hit-behavior interface, not a particle stub. Absence of a TODO here does not mean the gun-impact
VFX subsystem doesn't matter — CE clearly treats it as a first-class feature (block-material-dependent
debris color/texture, blood, gibs) — it means the port's gun-framework work (Phase 3, per
`docs/phase3/gun_framework.md`/`guns_and_ammo.md`) has not yet reached the point of stubbing its impact
VFX call site the way turrets/explosions/jetpacks already have. Worth a check-in with whoever owns the
gun-framework area before assuming it's lower priority than this report's ranking implies.

## Registration convention this port should follow

This port already has an established, working `DeferredRegister` convention
(`src/main/java/com/hbm/sound/ModSounds.java`, read in full) that a `com.hbm.particle.NtmParticleTypes`-
equivalent (suggest `com.hbm.particle.ModParticleTypes` to match this port's existing `Mod*` naming, e.g.
`ModSounds`, `ModItems`, `ModCreativeTabs` — **not** neo-edition's `Ntm*` prefix, which belongs to
neo-edition's own `NuclearTechMod` class name, not this port's) should copy:
- All-lowercase snake_case ids matching CE's original names where CE's names are already
  lowercase/snake_case, same "id casing" rule `ModSounds.java`'s own javadoc documents.
- A private `register(String)`-shaped helper, a public `register(IEventBus)` static method, called once
  from `MainRegistry`'s mod constructor (same call site as `ModSounds.register(modEventBus)`, confirmed
  at `MainRegistry.java:76`).
- The `RegisterParticleProvidersEvent` handler and the `TextureAtlasStitchedEvent` sprite-capture handler
  both belong as new `@SubscribeEvent` static methods on **this port's own `ClientModRegistry.java`**
  (not a new class), per the bus-default finding above.

## Blocked on / owned elsewhere

- **All 37 CE particle textures + the `assets/hbm/particles/<name>.json` files**: zero exist in this
  port's resources today. This is asset-porting work, not registry-design work — the registry can be
  authored and will compile without them (a `DeferredRegister` entry doesn't need its texture to exist to
  register), but nothing will actually render correctly client-side until the art lands. Owner: whoever
  runs the general asset-porting pass for Phase 5 (not named in this port's docs as a specific person/
  session — flag as an open coordination point).
- **The `AuxParticlePacketNT`/`HbmEffectNT` network-dispatch redesign** (how a server-side call becomes a
  client-visible effect, broadcast radius/threading, the `EffectHandler` functional-interface pattern) is
  explicitly the sibling `particle_engine_and_generic_vfx` report's scope — this report's registry design
  is a dependency of that work (the dispatch handlers need real `ParticleType`s to spawn), not a
  replacement for it.
- **Confirming `RegisterParticleProvidersEvent` is actually `IModBusEvent`** (the bus-default gotcha
  above) cannot be settled without either a real NeoForge sources jar (blocked: no network access to
  `maven.neoforged.net`) or an actual client launch (blocked: no display/client in this sandbox). Treat
  the recommendation to always pass `bus = EventBusSubscriber.Bus.MOD` explicitly as cheap insurance
  regardless — an explicit correct bus on an event that turns out to already default correctly is a no-op,
  while an explicit wrong bus on a class where it doesn't default correctly is a silent, hard-to-diagnose
  dead handler (exactly the outcome this report suspects already happened to neo-edition).

## Key risks

1. **The bus-default gotcha** (above) is the single highest-risk item: if unaddressed, particle
   registration would compile cleanly, look correct in code review, and simply never spawn anything
   client-side — a failure mode invisible to static reading, only catchable by launching a real client.
2. **`overrideLimiter` semantics are inferred from usage pattern, not confirmed from NeoForge docs/source**
   in this sandbox — neo-edition sets it `true` for exactly the "should ignore the vanilla particle
   density/distance culling" cases (debug lines, always-visible sparks, the `TOM_BLAST` climax effect) and
   `false` elsewhere, which lines up with what the constructor parameter name suggests, but this is
   pattern-matching against neo-edition's usage, not a confirmed API contract.
3. **Namespace hygiene**: every neo-edition example uses modid `hbmsntm`; this port is `hbm`. Any copied
   JSON/registration snippet must have its namespace substituted, or it will silently reference textures
   under the wrong mod's asset root and fail to resolve at runtime (not a compile error).
4. **The Tier-1/2 "not yet wired in neo-edition either" items** (`PlasmaBlast`, `Hadron`, `ParticleExSmoke`,
   the whole `Muke` family) have no working reference implementation anywhere to check this port's future
   work against — CE's source is still the sole behavioral truth for them, but the *registration
   plumbing* will need to be authored fresh rather than adapted from a compiling neo-edition example the
   way `COOLING_TOWER`/`GAS_FLAME`/`SPARK`/`RADIATION_FOG` can be.

## Open questions

1. Does `FlameCreator.composeEffectClient` (backing `EntityFireLingering`'s ground-fire TODO) actually
   resolve to pure vanilla particles like the Jetpack family, or does it pull in a custom class? Not read
   this session — worth 10 minutes before assuming it's Tier-0-safe.
2. Which exact `HbmEffectNT` constant backs each of the 8 turrets' generic "muzzle-flash burst" TODO
   phrasing — `Spark`, `CasingNT`/`CasingOld` (shell ejection), or a combination? The turret files
   themselves don't name the constant, only describe the visual in prose; confirming against each
   turret's real CE counterpart class (not read this session) would sharpen the Tier-1 `Spark` estimate.
3. Should this port's gun-framework impact VFX (bullet_hit family, currently absent from any TODO) get a
   TODO stub added now — even before Phase 5 implementation — so it surfaces in the same grep-based
   signal this report and future ones rely on? Worth raising with whoever owns `docs/phase3/
   gun_framework.md` follow-up.
4. Is `RegisterParticleProvidersEvent` actually `IModBusEvent`, definitively? Only answerable with a real
   NeoForge 1.21.1 sources jar or a client launch, neither available in this sandbox — carried forward as
   the report's central unverified claim, with the mitigation (always pass `bus=MOD` explicitly) that
   makes the answer not actually block implementation.
