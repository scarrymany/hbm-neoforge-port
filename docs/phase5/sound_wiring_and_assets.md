# Phase 5 research: Sound wiring and assets

## TL;DR

The task's framing ("does every registered SoundEvent have a real .ogg asset") turned out to
be the *smaller* problem. The real headline finding is upstream of that question:

1. **This port has TWO parallel, fully-duplicated `SoundEvent` registries** —
   `com.hbm.lib.HBMSoundHandler` (458 lines) and `com.hbm.sound.ModSounds` (455 lines) — each
   independently porting all 379 of CE's sound ids, each wired into `MainRegistry`'s constructor,
   using **two different, incompatible lowercasing conventions**. **159 of the 379 ids collide as
   byte-identical `ResourceLocation`s** between the two registries, meaning both `DeferredRegister`
   instances attempt to register the exact same `hbm:<id>` twice into the same underlying vanilla
   registry. This is a real, currently-committed bug, not a hypothetical — see Finding 1.
2. **Zero `.ogg` files and no `sounds.json` exist anywhere in this port's `src/main/resources`.**
   The entire audio-asset tree is unstarted. CE ships 548 real `.ogg` files (542 actually
   referenced by its `sounds.json`, 379 of those keyed to a registered `SoundEvent`). See Finding 2.
3. **`com/hbm/blocks/ModSoundTypes.java` currently fails to compile** — a genuine, narrow,
   2-line-fix bug, unrelated to the registry duplication. See Finding 3.
4. The call-site audit the task asked for (spot-check `HBMSoundHandler`/`ModSounds` references
   for "used a name that was never registered") **found no such bug anywhere in the codebase** —
   see Finding 4 for why that specific bug class cannot occur here, and what the systematic scan
   actually found instead.

## Files read (verbatim, in full, this session)

| File | Lines | Why |
|---|---|---|
| `upstream/hbm-ce/src/main/java/com/hbm/lib/HBMSoundHandler.java` | 789 | CE's real sound registry — source of truth for ids and count |
| `src/main/java/com/hbm/lib/HBMSoundHandler.java` | 458 | Port's registry #1 (task-named) |
| `src/main/java/com/hbm/sound/ModSounds.java` | 455 | Port's registry #2 (task-named) |
| `src/main/java/com/hbm/main/MainRegistry.java` | 125 | Confirms both registries are wired into the mod-bus |
| `src/main/java/com/hbm/blocks/ModSoundTypes.java` | 22 | First real consumer of the registry; found broken |
| `src/main/java/com/hbm/blocks/ModSoundType.java` | 157 | Confirms `ModSoundTypes.java`'s bug is a hard type mismatch |
| `src/main/java/com/hbm/util/SoundUtil.java` | 20 | Correctly-wired reference example |
| `src/main/java/com/hbm/items/machine/ItemCassette.java` | 112 | `TrackType` — confirms a false-positive from the scripted scan |
| `src/main/java/com/hbm/items/weapon/grenade/ItemGrenadeUniversal.java` | 203 | `playCue` — confirms another false-positive |
| `upstream/hbm-ce/src/main/resources/assets/hbm/sounds.json` | 440 (392 JSON keys) | Asset manifest, source of truth |
| `upstream/hbm-ce/src/main/java/com/hbm/config/CassetteJsonConfig.java` | 92 | Confirms `getOrCreate`'s only real caller and the `fm.*`/dynamic-id orphans |
| `docs/phase0/sound.md` | 142 | Phase 0's own research for this exact area — explains how the duplication happened |
| `docs/phase0/lib_util.md` | 239 | The *other* Phase 0 area that also ported the registry |
| `docs/phase0/DIGEST.md` | 518 | Journal digest — confirms both areas' implementation notes independently |
| `docs/phase3/guns_and_ammo.md` | 619 | Confirms `GunConfiguration` is dead weight, correcting a red herring in the task brief |

Plus targeted `grep`/`find`/small Python scripts over the full `src/main/java` tree (111 files
import one of the two sound classes) and CE's `assets/hbm/sounds/**` (548 files across 12
subdirectories) — exact commands and counts are inlined next to each finding below so they're
reproducible.

---

## Finding 1 — Two duplicate `SoundEvent` registries, both live, 159 colliding ids

### The two classes

CE has exactly one registry: `com.hbm.lib.HBMSoundHandler` (789 lines, `//TODO: rename to
NTMSounds` still present at line 9), a bare `Object2ObjectLinkedOpenHashMap<ResourceLocation,
SoundEvent>` populated by 379 `register(String)` calls inside a single `init()` method, flushed
into the vanilla registry by `com.hbm.main.ModEventHandler#soundRegistering` (a
`RegistryEvent.Register<SoundEvent>` listener, confirmed by `docs/phase0/sound.md:18`).

This port has **two** classes, both converted to `DeferredRegister<SoundEvent>` (the correct
1.21.1/NeoForge idiom — confirmed against `upstream/neo-edition/src/main/java/com/hbm/registry/
NtmSoundEvents.java`, which does the identical `DeferredRegister.create(BuiltInRegistries.
SOUND_EVENT, MODID)` / `SoundEvent.createVariableRangeEvent(...)` / `SOUND_EVENTS.register
(modEventBus)` dance — that shape is real, working NeoForge API, not a guess):

- `com.hbm.lib.HBMSoundHandler` (458 lines) — 379 `DeferredHolder<SoundEvent,SoundEvent>` fields,
  camelCase Java field names (`alarmAMSSiren`), ids **mechanically converted to snake_case**
  (e.g. `"alarm.amsSiren"` → `"alarm.ams_siren"` — insert `_` before each original uppercase
  letter, then lowercase). Its own docstring (lines 21-29) says this transformation matches "the
  same transformation Neo Edition applied by hand."
- `com.hbm.sound.ModSounds` (455 lines) — the same 379 sounds, `UPPER_SNAKE_CASE` Java field
  names (`ALARM_AMSSIREN`), ids **flattened to lowercase with no restructuring**
  (`"alarm.amsSiren"` → `"alarm.amssiren"` — just `.toLowerCase()`, no underscore insertion).

Both are `DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, MainRegistry.MODID)` — i.e. both
target the **same** underlying registry and the **same** `"hbm"` namespace.

### Both are wired in

`src/main/java/com/hbm/main/MainRegistry.java:75-76`, inside the mod constructor:

```java
HBMSoundHandler.register(modEventBus);
ModSounds.register(modEventBus);
```

Neither call is dead code or conditional. Both `DeferredRegister`s fire on the same
`RegisterEvent` for `SOUND_EVENT`.

### The collision, measured exactly

```
HBMSoundHandler unique ids: 379
ModSounds unique ids:       379
EXACT STRING COLLISIONS:    159   (e.g. block.boiler, weapon.bang, item.geiger1, turret.sentry_fire, ...)
```

Every id that contained **no** uppercase letter in CE's original spelling collapses to the
identical string under both conventions (`"block.boiler"` has nothing to lowercase or
snake-case, so both classes register `hbm:block.boiler`). The other 220 ids (**exactly** matching
`docs/phase0/DIGEST.md:190`'s own count, "220 of 379 sound ids contained uppercase letters") did
contain uppercase and so diverge (`alarm.ams_siren` vs `alarm.amssiren`), which is why the
registries don't 100%-collide — but the 159 that do collide are two live `DeferredRegister`
instances both trying to claim `hbm:<same-string>`.

**What this does at runtime is not independently verifiable in this sandbox** (no `./gradlew`,
no launchable client — see the task's own stated constraint). What I can say with confidence,
flagged explicitly as *well-established Minecraft/NeoForge knowledge, not confirmed against a
real jar here*: vanilla's `MappedRegistry#register` detects a duplicate `ResourceLocation` key
and calls `Util.pauseInIde(new IllegalStateException("Adding duplicate key '...' to registry"))`
— which **logs an error and continues** in a production/runtime environment (it only actually
halts execution under `SharedConstants.IS_RUNNING_IN_IDE`). The practical, most-likely symptom is
therefore **not** a hard crash but: 159 "Adding duplicate key" errors spammed into the log at
startup, plus an ambiguous/corrupted registry state for those 159 entries (two distinct
`SoundEvent` Java objects registered under the same name, with whichever one the internal maps
end up returning for lookups by name being implementation-defined). Given I cannot run the real
game here, the next phase that touches this area **must** attempt an actual client launch and
confirm the real behavior before treating this as merely cosmetic.

### Which one is actually used

```
grep -l 'import com.hbm.sound.ModSounds'      → 3 files  (MainRegistry.java + 2 real call sites)
grep -l 'import com.hbm.lib.HBMSoundHandler'  → 109 files (real call sites across every phase)
```

`HBMSoundHandler` is the de facto live registry — 109 files' worth of Phase 2/3/4 gameplay code
(weapons, entities, blocks, tools, turrets) already calls it. `ModSounds` has exactly two real
(non-`MainRegistry`) call sites, and both look like accidental drift rather than a deliberate
choice:

- `src/main/java/com/hbm/potion/BangEffect.java:56` — `ModSounds.LASER_BANG.get()`
- `src/main/java/com/hbm/entity/projectile/EntityRubble.java:140` — `ModSounds.BLOCK_DEBRIS.get()`

Both `LASER_BANG`/`BLOCK_DEBRIS` also exist on `HBMSoundHandler` as `laserBang`/`blockDebris` —
these two files are the only ones in the entire tree that reach for the "wrong" (near-dead)
registry for a sound that's available on the dominant one.

### The naming-convention split is itself a project-history bug, not a style choice

This is the more interesting part. `docs/phase0/sound.md` (the Phase 0 research report for the
`com.hbm.sound` package) is the area that **designed** `ModSounds` — its own recommended plan
(lines 85-98) explicitly says, quoting CE's actual asset-naming convention and this project's
hard "preserve CE ids" rule:

> "Important deviation to avoid copying from Neo Edition: Neo Edition renamed a number of ids
> while porting... Per this project's hard rule to preserve CE's registry ids exactly, our port
> must keep CE's original camelCase ids... rather than following Neo Edition's renames."

and DIGEST.md's own summary of what actually got built confirms `ModSounds.java` followed that:
"lowercased with no other restructuring... not snake_cased" (`docs/phase0/DIGEST.md:190`).

But `docs/phase0/lib_util.md` — a **different**, independently-run Phase 0 research area, scoped
to `com/hbm/lib/**`, which also happened to contain `HBMSoundHandler.java` — separately ported
the *same* registry under its own scope, and **that** implementation (line 174, "Port
`HBMSoundHandler` as a `DeferredRegister<SoundEvent>` holder") ended up snake-casing the ids
instead, matching Neo Edition's renaming scheme — the exact thing the sibling `sound` area's
research explicitly warned against copying. `docs/phase0/DIGEST.md:33` documents this
independently: "All 379 sound paths mechanically lowercased/snake_cased."

Net effect: **the registry class that actually won (109 real call sites) is the one that
violates this project's own documented naming rule**, and the registry that followed the rule
correctly (`ModSounds`) is nearly unused. Both were produced by isolated, parallel Phase 0 agents
who had no visibility into each other's output, and the wiring/integration step applied both
areas' `MainRegistry` instructions verbatim without noticing the overlap.

### Why this matters for the (not-yet-started) asset pipeline

Whichever registry survives determines the **`sounds.json` key set** the future asset-copy phase
must author (the JSON top-level key must exactly equal the registered `SoundEvent`'s
`ResourceLocation` path — the `"name"`/file-path values inside each entry are independent and do
not need to match). CE's own `.ogg` files are already all-lowercase with no separators (CE's own
comment in `HBMSoundHandler.java:409` warns "there must be an `assembleroperate.ogg`... `
assemblerOperate.ogg` won't work!") — so **no `.ogg` file needs renaming either way**, only the
`sounds.json` keys need to match whichever id scheme wins. This is a decision that has to be made
*before* `sounds.json` can be written, which is why it's this area's job to flag now rather than
leave for whoever picks up assets next.

**Recommendation (not applied — read-only research):** delete `com.hbm.sound.ModSounds`
entirely, migrate its 2 real call sites (`BangEffect.java`, `EntityRubble.java`) to
`HBMSoundHandler`, and remove the `ModSounds.register(modEventBus)` line from `MainRegistry`.
Keeping `HBMSoundHandler` (not `ModSounds`) is the pragmatic call despite it being the
rule-violating one, purely because it's the one 109 files already depend on — rewriting 109
files' worth of field references to switch to `ModSounds`'s naming is far more churn than fixing
2 files and eating the "ids look like Neo Edition's, not CE's" deviation (which should be
explicitly logged as a known, permanent deviation from the project's stated convention, since
undoing it now would mean re-deriving a 379-entry `sounds.json` from scratch either way).

---

## Finding 2 — Zero audio assets exist in the port yet; CE's asset tree audited

```
find src/main/resources -iname "*.ogg"        → 0 files
find src/main/resources -iname "sounds.json"  → 0 files
```

Confirmed empty. `src/main/resources/assets/hbm/` currently only contains `multiblock_bounds/`.
This is squarely "blocked on the asset-copy phase" — not something this research area can fix
(binary assets aren't something an agent authors), but here is the exact scope that phase needs:

### CE's real asset census

```
find upstream/hbm-ce/.../sounds -iname "*.ogg" | wc -l   → 548 files, 12 subdirectories:
  229 weapon/   116 block/   49 entity/   37 tool/   32 screm/   24 alarm/
   14 footsteps/ 12 turret/  12 player/    8 potatos/  8 misc/    4 music/
```

Cross-checked `sounds.json` against both the Java registry and the files on disk:

```
sounds.json top-level keys:                392
HBMSoundHandler.java unique register() ids: 379   (100% — every one has a sounds.json entry)
distinct .ogg refs inside sounds.json:      542   (all 542 resolve to a real file — 0 missing)
.ogg files on disk not referenced by any entry: 6 (dead/unused extras, harmless)
sounds.json keys with NO matching register() call: 13
```

CE itself is internally 100% consistent in the direction that matters — **every registered
`SoundEvent` has a real backing asset**; there is no "registered-but-assetless" gap in CE. The 13
sounds.json entries with no matching `register()` call are not a port problem; I traced each:

- `weapon.fire.vstar` — used directly via `new SoundEvent(new ResourceLocation("hbm:weapon.fire.
  vstar"))` in `render/item/weapon/sedna/ItemRenderFolly.java:112`, bypassing the registry
  entirely (an ad-hoc unregistered `SoundEvent`, CE's own equivalent of `getOrCreate`).
- `fm.clap` / `fm.mug` / `fm.sample` — almost certainly consumed dynamically via
  `HBMSoundHandler.getOrCreate(ResourceLocation)` from `config/CassetteJsonConfig.java:58`
  (reads a `"sound"` string out of a user-editable JSON config file, not a Java literal — no
  static grep can find the actual id strings since they come from data, not code).
- `block.boilerGroan`, `block.dieselOperate`, `block.missileAssembly`, `block.vaultScrape`,
  `block.vaultThud`, `weapon.osiprAltFire`, `weapon.reload.closeClick`, `weapon.reload.
  insertGrenade`, `weapon.reload.insertLarge` — grepped for each by name across all of CE's
  `.java`; found no consumer at all. These look like genuinely stale/orphaned CE sounds.json
  entries (most have an obvious registered near-namesake, e.g. `boilerGroan0/1/2` exist and are
  used instead of the un-suffixed `boilerGroan`). **Not a port concern** — nothing needs these,
  and CE itself doesn't play them either.

### What the future asset-copy phase actually needs to do

1. Copy the 542 referenced `.ogg` files from `upstream/hbm-ce/src/main/resources/assets/hbm/
   sounds/**` verbatim (no renaming — CE's filenames are already registry-legal lowercase).
2. Author a new `sounds.json` at `src/main/resources/assets/hbm/sounds.json`, one entry per
   surviving registry id (379, once Finding 1 is resolved), each `"name"` pointing at the
   corresponding already-lowercase CE `.ogg` path.
3. This is entirely blocked on Finding 1's registry-consolidation decision being made first,
   since the JSON keys must match whichever registry's exact path strings survive.

---

## Finding 3 — `com/hbm/blocks/ModSoundTypes.java` does not compile (confirmed, 2-line fix)

This is unrelated to Finding 1 and is a hard, unambiguous bug readable directly from this
project's own source (not a matter of guessing an external vanilla API shape).

`src/main/java/com/hbm/blocks/ModSoundTypes.java` (22 lines total):

```java
public static final ModSoundType grate = ModSoundType.customStep(SoundType.STONE, HBMSoundHandler.metalBlock, 0.5F, 1.0F);
public static final ModSoundType pipe = ModSoundType.customDig(SoundType.METAL, HBMSoundHandler.pipePlaced, 0.85F, 0.85F)...
```

`HBMSoundHandler.metalBlock` / `.pipePlaced` are `DeferredHolder<SoundEvent,SoundEvent>` fields.
`ModSoundType.customStep`/`customDig` (`src/main/java/com/hbm/blocks/ModSoundType.java:51,55,69,
73`) declare their sound parameter as a plain `SoundEvent` — there is no `Holder<SoundEvent>` or
`Supplier<SoundEvent>` overload anywhere in that 157-line file. `DeferredHolder<SoundEvent,
SoundEvent>` is not itself a `SoundEvent` (it's a wrapper implementing `Holder<T>`/`Supplier<T>`,
not `T`), so this is a genuine type mismatch — `javac` will reject it.

The file's own header comment gives away exactly how this happened: *"Depends on
`com.hbm.lib.HBMSoundHandler`... which does not exist yet in this port tree; this class will not
compile until that class lands."* This was written speculatively, before `HBMSoundHandler`
existed as a `DeferredHolder`-based class. `HBMSoundHandler` has since landed, but this file was
never revisited to add the two missing `.get()` calls.

`ModSoundTypes.grate`/`.pipe` are not referenced anywhere else in the tree yet (`grep`
confirmed 0 hits) — but that does not matter: Gradle's `compileJava` compiles every `.java` file
in the source set regardless of whether anything calls it, so **this file alone is enough to
fail the whole module's build** the moment someone runs `./gradlew compileJava` (which this
sandbox cannot do — flagging as the most concrete, self-contained, immediately-fixable item in
this whole report). Fix is exactly two `.get()` insertions:

```java
ModSoundType.customStep(SoundType.STONE, HBMSoundHandler.metalBlock.get(), 0.5F, 1.0F);
ModSoundType.customDig(SoundType.METAL, HBMSoundHandler.pipePlaced.get(), 0.85F, 0.85F)
```

---

## Finding 4 — The call-site audit the task asked for

### Why "used a name that was never registered" can't happen the way the task expects

Every real call site references `HBMSoundHandler.<field>` (or `ModSounds.<FIELD>`) as a
**Java static field**, not a string-keyed registry lookup. A reference to a field that was never
declared is a compile error, not a runtime landmine — `javac` catches it unconditionally, for
every file, on every build. So the specific "sounds-right-but-never-registered" bug class the
task hypothesized structurally cannot exist as a *silent* runtime bug here; it can only exist as
a build-breaker (which is exactly what Finding 3 turned out to be, just via a type mismatch
rather than a missing identifier).

To confirm this is exhaustive and not just true for the files I happened to look at, I scripted
a full-tree scan (`src/main/java/**/*.java`, excluding the registries themselves) collecting
every `HBMSoundHandler.<identifier>` reference and diffing against the 379 declared field names
plus the 4 real static methods (`SOUND_EVENTS`, `register`, `getOrCreate`, `geigerSounds()`/
`voiceSounds()`/`boilerGroanSounds()`):

```
References to HBMSoundHandler.<name> where <name> is not a declared field/method: 0 real hits
```

(Two apparent hits, `get` and `xxx`, were false positives from my own regex matching prose
inside javadoc comments in `GunPistolItems.java`/`XFactory*.java` — e.g. `{@code HBMSoundHandler.
xxx.get()}` used as a placeholder in a comment explaining why gun-sound resolution must be
deferred past `RegisterEvent` time. Not real code, verified by reading each file.)

### 15+ real call sites spot-checked against CE for correctness (not just compile-safety)

Beyond "does the field exist," I cross-checked a sample spanning Phase 2/3/4 content against
CE's actual sound choice for the same behavior, to confirm the port didn't just compile but
picked the *right* sound:

| Port call site | Sound used | CE source confirms |
|---|---|---|
| `entity/logic/EntityDeathBlast.java:122` | `mukeExplosion` | matches `EntityMeteor`-family CE usage pattern |
| `entity/logic/EntityBomber.java:192` | `missileTakeoff` | — |
| `entity/logic/EntityPlaneBase.java:111,162` | `planeShotDown`, `planeCrash` | — |
| `entity/projectile/EntityChopperMine.java:101` | `nullMine` | ✅ `ItemChopperMine.java:124` (CE) — exact match |
| `entity/projectile/EntityMeteor.java:185,197` | `oldExplosion`, `meteoriteFallingLoop` | ✅ `EntityMeteor.java:163` (CE) — exact match |
| `entity/mob/EntityUFO.java:290,363,372` | `ufoBeam`, `ballsLaser`, `richard_fire` | ✅ `EntityUFO.java:346` (CE) `ballsLaser` — exact match |
| `entity/mob/EntityBOTPrimeBase.java:109,113` | `ballsLaser` | — |
| `entity/mob/EntityCyberCrab.java:146`, `EntityTaintCrab.java:97` | `sawShoot` | — |
| `entity/mob/EntityHunterChopper.java:176,251,256,299,360,368` | `nullChopper`, `osiprShoot`, `chopperCharge`, `chopperDrop`, `nullCrashing`, `chopperDamage` | — |
| `entity/mob/ai/EntityAIMaskmanMinigun.java:63` | `calShoot` | — |
| `items/tool/ItemBoltgun.java:86` | `boltgun` | ✅ `ItemBoltgun.java:74,136` (CE) — exact match |
| `items/tool/ItemDosimeter.java:51`, `ItemGeigerCounter.java:78`, `ItemLungDiagnostic.java:38` | `techBoop` | — |
| `items/tool/ItemDesignatorRange.java:70`, `ItemOilDetector.java:49` | `techBleep` | — |
| `explosion/ExplosionNukeSmall.java:34` | `mukeExplosion` | — |
| `blocks/generic/BlockSupplyCrate.java:112` | `crateBreak` | — |

All 15+ resolve to real, correctly-declared `HBMSoundHandler` fields; the 3 explicitly
cross-checked against CE line-for-line (`nullMine`, `meteoriteFallingLoop`, `ballsLaser`,
`boltgun`) match CE's own sound choice exactly.

### A second class of false positive I chased down and want to document

A broader scripted scan for `HBMSoundHandler.<field>` used **without** a following `.get()`
(i.e. passing the raw `DeferredHolder` where something might expect an unwrapped `SoundEvent`)
turned up ~28 sites across 9 files: `hazard/type/HazardTypeUnstable.java`, `items/
ItemInventory.java`, `items/machine/ItemCassette.java` (20 of the 28 — every `TrackType`
constant), `items/weapon/grenade/GrenadeFillingActions.java`, `items/weapon/grenade/
ItemGrenadeUniversal.java`, `blocks/ModSoundTypes.java` (Finding 3, above), `explosion/vanillant/
standard/ExplosionEffectTiny.java`, `entity/grenade/EntityGrenadeUniversal.java`, `entity/logic/
EntityNukeExplosionMK3.java`.

I individually verified every target signature rather than assume the pattern was uniformly
buggy:

- `ItemCassette.TrackType`'s constructor takes `Supplier<SoundEvent>` (confirmed by reading
  `ItemCassette.java:96`, `this.location.get()`) — `DeferredHolder` implements `Supplier<T>`, so
  these 20 sites are correct as written, not bugs.
- `ItemGrenadeUniversal.playCue` takes `Holder<SoundEvent>` (confirmed at
  `ItemGrenadeUniversal.java:162`) — also correct as written.
- The remaining sites (`HazardTypeUnstable`, `ItemInventory`, `GrenadeFillingActions`,
  `ExplosionEffectTiny`, `EntityGrenadeUniversal`, `EntityNukeExplosionMK3`) all go through
  vanilla `Level#playSound(...)` directly with the bare `DeferredHolder`. **Flagged explicitly as
  well-established Minecraft/NeoForge knowledge, not independently verified by a compiler in this
  sandbox**: `Level#playSound` has taken a `Holder<SoundEvent>`-typed overload since the 1.19.3
  sound-registry refactor (matching vanilla `net.minecraft.sounds.SoundEvents` fields also being
  `Holder<SoundEvent>`), and `DeferredHolder<SoundEvent,SoundEvent>` satisfies that directly with
  no `.get()` needed. This is corroborated by real, already-compiling reference code:
  `upstream/neo-edition/src/main/java/com/hbm/blockentity/machine/GeigerBlockEntity.java:62-74`
  passes both its own `NtmSoundEvents.GEIGERn` (a `DeferredHolder`) *and* vanilla
  `SoundEvents.FURNACE_FIRE_CRACKLE`/`FIRECHARGE_USE` directly into `level.playSound(...)` with
  no `.get()`, in the same idiom, in already-committed code. Only `ModSoundTypes.java` (Finding
  3) is a confirmed bug — its target method is this project's own, explicitly `SoundEvent`-typed,
  with no `Holder`/`Supplier` overload to rescue it.

**Net result of the call-site audit: one confirmed bug (Finding 3), zero "phantom field" bugs,
and the registry-duplication issue in Finding 1 is the real structural problem — not scattered
call-site typos.**

---

## Corrections to the task's own framing

1. **"registered-but-assetless sounds" is not the interesting question.** CE's own registry and
   `sounds.json` are already 100% mutually consistent (Finding 2). The port hasn't shipped any
   assets yet at all, so there's no partial/inconsistent state to audit there — it's simply
   0-of-379, cleanly, and the interesting content is *what the future asset phase needs to know*
   (Finding 2's final section), not "which sounds are missing."
2. **The "used a name that sounds right but was never added" bug class the task expected doesn't
   manifest as a silent runtime bug** — it can only be a compile error in this codebase, since
   every real call site is a static-field reference, not a string lookup (Finding 4). The actual,
   much bigger latent bug is architectural (Finding 1: two whole registries), not scattered typos.
3. **`HBMSoundHandler.java`'s own docstring flags a "dropped wiring" item** (assigning
   `GunConfiguration.RSOUND_LAUNCHER` etc. once `com.hbm.handler.GunConfiguration` exists) as
   something "whichever area ports `GunConfiguration` must" finish. I checked: `docs/phase3/
   guns_and_ammo.md` (Phase 3's own research for the guns area) already investigated this and
   explicitly **recommends never porting `GunConfiguration`/`ItemGunBase`/
   `BulletConfigSyncingUtil` at all** — confirmed dead framework, used only by two already-broken
   legendary items (`gun_supershotgun`, `gun_vortex`) even in real CE. `com.hbm.handler.
   GunConfiguration` still does not exist anywhere in this port (`find` confirms 0 files, 0 other
   references). **This "blocker" is very likely permanently moot** — it should not be treated as
   live outstanding work for a future phase to pick up.

---

## Safe to build now

- **Fixing `ModSoundTypes.java`** (Finding 3) — self-contained, 2-line change, no cross-area
  dependency, unblocks the whole module's compilation regardless of anything else in this report.
- **Consolidating the two registries into one** (Finding 1's recommendation: delete
  `com.hbm.sound.ModSounds`, keep `com.hbm.lib.HBMSoundHandler`, migrate `BangEffect.java` and
  `EntityRubble.java`'s two call sites, drop the `ModSounds.register(modEventBus)` line in
  `MainRegistry.java`) — self-contained, no cross-area dependency, and should happen *before* any
  asset work so the eventual `sounds.json` only has to target one id scheme.
- Everything client-side that only *calls into* `HBMSoundHandler.<field>.get()` (i.e. the other
  109 files) is unaffected either way and needs no changes.

## Blocked

- **Actually copying `.ogg` assets and authoring `sounds.json`** — not something a code-editing
  agent produces; needs the 542 real files from `upstream/hbm-ce/src/main/resources/assets/hbm/
  sounds/**` copied in verbatim, keyed against whichever registry id scheme Finding 1 leaves
  standing. Owner: whichever future pass is scoped to asset-copying (not yet named in any phase
  doc I found — worth the project explicitly assigning an owner, since it's currently nobody's).
- **`HBMSoundHandler.getOrCreate(ResourceLocation)`'s dynamic-registration design gap** — CE's
  `CassetteJsonConfig` (siren-cassette JSON config, not yet ported — confirmed absent from this
  port's tree) relies on registering brand-new `SoundEvent`s at runtime from user-edited JSON,
  which `DeferredRegister` cannot do after `RegisterEvent` fires. `getOrCreate` exists in the
  port already (`HBMSoundHandler.java:451-453`, functionally correct as a `ConcurrentHashMap`-
  backed ad-hoc-`SoundEvent` factory, ports correctly to `SoundEvent.createVariableRangeEvent`
  outside the registry) but is currently **unused dead code**, since `CassetteJsonConfig` and
  `ItemCassette`'s siren-track config loading haven't been ported yet. Owner: whichever future
  area ports `com.hbm.config.CassetteJsonConfig` (currently unassigned in any phase doc found).
  This is a genuine, correctly-scoped, still-open gap — not a correction to the task, unlike
  the `GunConfiguration` item above.
- **Confirming Finding 1's actual runtime behavior** (log-and-corrupt vs hard crash) needs a real
  `./gradlew runClient`/build, which this sandbox cannot do (network policy blocks
  `maven.neoforged.net`). Flagged as the single most important thing for whoever picks this up
  next to verify first, before deciding how urgently to apply the Finding 1 fix.

## Key risks

- **Finding 1 is the dominant risk of this whole area.** Until it's resolved, the actual
  in-registry behavior for 159 sound ids is unverified and asset authoring can't safely start
  (the `sounds.json` key set isn't stable yet).
- **Finding 3 currently blocks `./gradlew compileJava` for the entire module**, independent of
  anything else — any other Phase 5 work (renderers, particles, HUD) that also touches
  `src/main/java` will inherit this failure until it's fixed, since Gradle compiles the whole
  source set as one unit.
- The `Level#playSound(Holder<SoundEvent>, ...)` overload claim in Finding 4 is presented with
  high confidence (corroborated by neo-edition's own compiling code) but is explicitly *not*
  independently verified against a real compiled jar in this sandbox — worth a fast confirming
  check (a one-file `javac` smoke test against the real NeoForge dependency, once network access
  to `maven.neoforged.net` is available) before anyone treats it as settled.

## Open questions

1. Who owns "copy CE's audio assets + author `sounds.json`"? Not named in any phase doc found —
   recommend the project explicitly assign this before Phase 5 implementation starts, since
   Finding 1 must land first and someone needs to be responsible for the follow-up.
2. Who owns porting `com.hbm.config.CassetteJsonConfig` (unblocks `getOrCreate`'s only real
   caller)? Also unassigned.
3. Does the project want to formally log "the surviving sound-id scheme deviates from CE's
   original camelCase ids, matching Neo Edition's renaming instead of this project's own stated
   convention" as an accepted, permanent deviation (per Finding 1's recommendation to keep
   `HBMSoundHandler` rather than rewrite 109 files), or does someone want to instead migrate the
   109 real call sites onto `ModSounds`'s ids (more churn, but stays rule-compliant)? This is a
   real decision for a human/lead, not something this research pass should silently resolve.
