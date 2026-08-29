# Phase 0 Research: Damage Types

Area key: `damage_types`. Read-only research pass, no source files were written.

## Class inventory

| File | Purpose |
|---|---|
| `com.hbm.lib.ModDamageSource` (CE) | Central registry of ~41 static `DamageSource` singleton instances plus a family of `causeXxxDamage(...)` factory methods that build `EntityDamageSourceIndirect` instances from a hardcoded String "damage type" id. Also owns 13 `s_*` string constants used as those ids, and two predicate helpers (`getIsTau`, `getIsSubatomic`) that pattern-match on the source's `damageType` string. |
| `com.hbm.items.weapon.sedna.DamageSourceSednaNoAttacker` (CE) | `DamageSource` subclass for the Sedna weapon-config framework. Lowercases its `type` id and overrides `getDeathMessage` to build a translation key `"death.sedna." + damageType`. No attacker/projectile entity attached. |
| `com.hbm.items.weapon.sedna.DamageSourceSednaWithAttacker` (CE) | Extends the above, adds `projectile`/`shooter` entities, overrides `getImmediateSource`/`getTrueSource`, and overrides `getDeathMessage` to build `"death.sedna." + damageType + ".attacker"` with the shooter's display name (or an obfuscated "Unknown" placeholder if the shooter despawned). |
| `com.hbm.registry.NtmDamageTypes` (Neo Edition reference, read-only) | Shows the real 1.21.1/NeoForge shape: an interface of `ResourceKey<DamageType>` constants built via `ResourceKey.create(Registries.DAMAGE_TYPE, ...)`, plus a `static void bootstrap(BootstrapContext<DamageType> context)` that registers each key with `new DamageType(msgId, exhaustion)`. |
| `com.hbm.registry.tags.NtmDamageTypeTags` (Neo Edition reference) | Custom `TagKey<DamageType>` constants (`IS_ENERGY`, `ABSOLUTE`) for behavior groupings that vanilla has no built-in tag for. |
| `com.hbm.datagen.NtmDamageTypeTagsProvider` (Neo Edition reference) | `DamageTypeTagsProvider` subclass; datagen-time wiring of each `ResourceKey<DamageType>` into vanilla tags (`IS_EXPLOSION`, `BYPASSES_ARMOR`, `BYPASSES_INVULNERABILITY`, `IS_PROJECTILE`) and the custom mod tags. |
| `com.hbm.datagen.NtmDataGenerators` (Neo Edition reference) | Shows how the damage-type registry set and its tag provider are hooked into `GatherDataEvent` via `RegistrySetBuilder.add(Registries.DAMAGE_TYPE, NtmDamageTypes::bootstrap)` and `DatapackBuiltinEntriesProvider`. |
| `com.hbm.util.EntityDamageUtil` (Neo Edition reference) | Confirms real 1.21.1 damage-application API: `entity.hurt(DamageSource, float)`, `level.damageSources()`, `DamageSource.is(TagKey<DamageType>)`. |
| `com.hbm.items.weapon.sedna.BulletConfig` (Neo Edition reference) | Shows the Sedna death-message subclasses were entirely replaced: a `DamageClass` enum switches to one of 8 generic `ResourceKey<DamageType>` constants, and the source is built with `level.damageSources().source(damageType, shooter, entity)` - no custom `DamageSource` subclass or `getDeathMessage` override needed. |

## Key responsibilities

- `ModDamageSource` is a pure data table: every other CE subsystem (explosions, radiation ticking, projectiles, block damage, hazard effects, digamma/creative-piercing damage) pulls its `DamageSource` from here rather than constructing one inline. Behavior differences between sources are expressed entirely through the fluent `DamageSource` builder flags: `setExplosion()`, `setDamageBypassesArmor()`, `setDamageIsAbsolute()`, `setDamageAllowedInCreativeMode()`, `setProjectile()`, `setFireDamage()`.
- The `causeXxxDamage` factory methods build per-event `EntityDamageSourceIndirect` instances (immediate + true source entities) rather than reusing a singleton, because those sources need entity context for `getTrueSource()`/death messages/knockback direction.
- The Sedna `DamageSourceSednaNoAttacker`/`WithAttacker` pair exists solely to produce distinct death messages per weapon "type" string (e.g. `death.sedna.physical`) with or without attribution to a shooter entity.

## Cross-area dependencies

- `com.hbm.entity.projectile.*` (imported by `ModDamageSource`): `EntityBulletBase`, `EntityRainbow`, `EntityDischarge`, `EntityFire`, `EntityPlasmaBeam`, `EntityLN2`, `EntityLaserBeam`, `EntityMinerBeam` are all owned by the entities/projectiles area, not this one. This area only depends on their type signatures for the factory method parameters.
- Every other Phase-0+ area that inflicts custom damage (explosions, hazards/radiation, weapons, mobs, blocks like the meteorite/boxcar/turbofan) is a consumer of whatever `com.hbm.damage.ModDamageTypes` this area produces. This is a foundational, widely-depended-on area - it should land early since many other areas' `entity.hurt(...)` calls need these keys to compile.
- The Sedna weapon-config framework (`com.hbm.items.weapon.sedna.*`, owned by a weapons/items area) depends on whatever generic damage-class keys this area defines for Sedna.

## Recommended NeoForge/Java 21 port plan

1PORT and Neo Edition both confirm 1.21.1 removed subclassable `DamageSource`; it is now `final` and data-driven via `DamageType` datapack entries plus `ResourceKey<DamageType>`. So:

1. **`com.hbm.damage.ModDamageTypes`** (interface, mirroring `NtmDamageTypes`'s real, verified shape): one `ResourceKey<DamageType> key(String name)` helper plus one constant per CE damage-source id (see mapping table below), and a `static void bootstrap(BootstrapContext<DamageType> context)` registering each with `new DamageType(msgId, exhaustion)`. Exhaustion `0.1F` matches the Neo Edition reference and vanilla's typical non-mob-attack value; CE's `DamageSource` did not expose a configurable exhaustion value so there is no CE-side signal to differ per type - flagged as an open question below.
2. **`com.hbm.damage.tags.ModDamageTypeTags`**: mirror `NtmDamageTypeTags` - custom `TagKey<DamageType>` for CE flag combinations vanilla has no tag for (an `ABSOLUTE` tag standing in for `setDamageIsAbsolute()`, since that CE flag has no single vanilla equivalent).
3. **`com.hbm.datagen.ModDamageTypeTagsProvider extends DamageTypeTagsProvider`**: translate every CE builder-flag combination into the confirmed real vanilla tags:
   - `setExplosion()` -> `DamageTypeTags.IS_EXPLOSION`
   - `setProjectile()` -> `DamageTypeTags.IS_PROJECTILE`
   - `setFireDamage()` -> `DamageTypeTags.IS_FIRE`
   - `setDamageBypassesArmor()` -> `DamageTypeTags.BYPASSES_ARMOR`
   - `setDamageAllowedInCreativeMode()` -> `DamageTypeTags.BYPASSES_INVULNERABILITY` (confirmed by Neo Edition's `DIGAMMA` mapping, which is CE's only other `setDamageAllowedInCreativeMode()` source alongside `ams` and `nitan`)
   - `setDamageIsAbsolute()` -> the custom `ABSOLUTE` tag, itself composed (via `.addTags(...)`) of `DamageTypeTags.BYPASSES_EFFECTS` + `DamageTypeTags.BYPASSES_RESISTANCE`, exactly as Neo Edition does it.
4. **Wire the bootstrap into datagen**: this area does not own `NtmDataGenerators`'s equivalent (a shared gather-data entrypoint is presumably integration-owned or lands with a later "datagen scaffolding" area). Document here rather than edit: whatever central `GatherDataEvent` subscriber exists must call `builder.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap)` on its `RegistrySetBuilder`, add a `DatapackBuiltinEntriesProvider`, and register `new ModDamageTypeTagsProvider(output, lookup, helper)` as a server data provider.
5. **Death messages become pure lang-file data**: 1.21's `CombatTracker`/`DamageSources` compute death messages from the `DamageType`'s message id automatically via `death.attack.<msgId>` / `death.attack.<msgId>.player` translation keys (confirmed live in Neo Edition's `NtmLanguageProvider.addDamage`/`addDamagePlayer` helpers, used for e.g. `sednaPhysical`). This means **`DamageSourceSednaNoAttacker` and `DamageSourceSednaWithAttacker` are not ported as classes at all** - they have no NeoForge-21.1 equivalent because `DamageSource` can no longer be subclassed. Whatever area owns the Sedna weapon system should instead: define a small `DamageClass` enum (`PHYSICAL, FIRE, EXPLOSION, ELECTRIC, LASER, MICROWAVE, SUBATOMIC, OTHER` - exactly Neo Edition's `BulletConfig.DamageClass`), map each to one of 8 generic `ResourceKey<DamageType>` constants in `ModDamageTypes` (`sednaPhysical`, `sednaFire`, ... msgIds), and build the source per-hit via `level.damageSources().source(damageType, shooter, projectile)`. That switch/enum and its lang entries belong to the weapons area, not here; this area's report only needs to supply the 8 keys.
6. **`causeXxxDamage` indirect factories**: 1.21's `DamageSources.source(ResourceKey<DamageType>, Entity directEntity, Entity causingEntity)` (three-arg overload, confirmed via `BulletConfig.getDamage`) directly replaces `new EntityDamageSourceIndirect(type, base, ent)`. Each CE factory method's String type id becomes one `ResourceKey<DamageType>` constant in `ModDamageTypes`; the methods themselves (taking `Level`/`Entity` args and calling `level.damageSources().source(KEY, causing, direct)`) belong in whichever area consumes them (projectiles), since `ModDamageSource`'s CE factory methods take projectile-entity types (`EntityBulletBase`, `EntityRainbow`, etc.) owned by the entities area. This area's deliverable is only the `ResourceKey<DamageType>` constants those call sites will need; the convenience wrapper methods should be re-homed to `com.hbm.entity` (or wherever the projectiles land) to avoid this area importing entity classes it does not own.
7. **`getIsTau`/`getIsSubatomic` predicates**: port as `DamageSource.is(TagKey<DamageType>)` checks against two small custom tags (`IS_TAU`, `IS_SUBATOMIC`) in `ModDamageTypeTags`, rather than string comparison - this is more idiomatic 1.21 and avoids re-deriving a "damage type string" that no longer exists on `DamageSource`.

## Full CE id -> ResourceKey<DamageType> mapping

All keys use `ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("hbm", "<id>"))`, i.e. registry id `hbm:<id>`.

### Direct singleton sources (from the 41 static fields)

| CE field name | CE string id | Recommended key id | Tags to apply |
|---|---|---|---|
| `nuclearBlast` | `nuclearBlast` | `nuclear_blast` | IS_EXPLOSION |
| `blast` | `blast` | `blast` | IS_EXPLOSION |
| `mudPoisoning` | `mudPoisoning` | `mud_poisoning` | BYPASSES_ARMOR |
| `acid` | `acid` | `acid` | (none) |
| `euthanizedSelf` | `euthanizedSelf` | `euthanized_self` | BYPASSES_ARMOR |
| `euthanizedSelf2` | `euthanizedSelf2` | `euthanized_self_2` | BYPASSES_ARMOR |
| `tauBlast` | `tauBlast` | `tau_blast` | BYPASSES_ARMOR, IS_EXPLOSION |
| `digamma` | `digamma` | `digamma` | ABSOLUTE, BYPASSES_ARMOR, BYPASSES_INVULNERABILITY |
| `radiation` | `radiation` | `radiation` | BYPASSES_ARMOR |
| `suicide` | `suicide` | `suicide` | IS_PROJECTILE |
| `rubble` | `rubble` | `rubble` | IS_PROJECTILE |
| `shrapnel` | `shrapnel` | `shrapnel` | IS_PROJECTILE |
| `blackhole` | `blackhole` | `black_hole` | ABSOLUTE, BYPASSES_ARMOR |
| `turbofan` | `blender` | `blender` | ABSOLUTE, BYPASSES_ARMOR |
| `meteorite` | `meteorite` | `meteorite` | ABSOLUTE, BYPASSES_ARMOR |
| `boxcar` | `boxcar` | `boxcar` | ABSOLUTE, BYPASSES_ARMOR |
| `boat` | `boat` | `boat` | ABSOLUTE, BYPASSES_ARMOR |
| `building` | `building` | `building` | ABSOLUTE, BYPASSES_ARMOR |
| `taint` | `taint` | `taint` | ABSOLUTE, BYPASSES_ARMOR |
| `ams` | `ams` | `ams` | ABSOLUTE, BYPASSES_ARMOR, BYPASSES_INVULNERABILITY |
| `amsCore` | `amsCore` | `ams_core` | ABSOLUTE, BYPASSES_ARMOR |
| `broadcast` | `broadcast` | `broadcast` | ABSOLUTE, BYPASSES_ARMOR |
| `bang` | `bang` | `bang` | ABSOLUTE, BYPASSES_ARMOR |
| `pc` | `pc` | `pc` | ABSOLUTE, BYPASSES_ARMOR |
| `cloud` | `cloud` | `cloud` | ABSOLUTE, BYPASSES_ARMOR |
| `lead` | `lead` | `lead` | ABSOLUTE, BYPASSES_ARMOR |
| `enervation` | `enervation` | `enervation` | ABSOLUTE, BYPASSES_ARMOR |
| `electricity` | `electricity` | `electricity` | ABSOLUTE, BYPASSES_ARMOR |
| `exhaust` | `exhaust` | `exhaust` | ABSOLUTE, BYPASSES_ARMOR |
| `spikes` | `spikes` | `spikes` | BYPASSES_ARMOR |
| `lunar` | `lunar` | `lunar` | ABSOLUTE, BYPASSES_ARMOR |
| `slicer` | `slicer` | `slicer` | ABSOLUTE, BYPASSES_ARMOR |
| `crucible` | `crucible` | `crucible` | ABSOLUTE, BYPASSES_ARMOR |
| `monoxide` | `monoxide` | `monoxide` | ABSOLUTE, BYPASSES_ARMOR |
| `asbestos` | `asbestos` | `asbestos` | ABSOLUTE, BYPASSES_ARMOR |
| `blacklung` | `blacklung` | `blacklung` | ABSOLUTE, BYPASSES_ARMOR |
| `mku` | `mku` | `mku` | ABSOLUTE, BYPASSES_ARMOR |
| `vacuum` | `vacuum` | `vacuum` | ABSOLUTE, BYPASSES_ARMOR |
| `overdose` | `overdose` | `overdose` | ABSOLUTE, BYPASSES_ARMOR |
| `microwave` | `microwave` | `microwave` | ABSOLUTE, BYPASSES_ARMOR |
| `nitan` | `nitan` | `nitan` | ABSOLUTE, BYPASSES_ARMOR, BYPASSES_INVULNERABILITY |

### Indirect (attacker-attributed) sources, from the `causeXxxDamage` factories and `s_*` string constants

| CE string id | Recommended key id | Tags to apply | Notes |
|---|---|---|---|
| `revolverBullet` (`s_bullet`) | `revolver_bullet` | IS_PROJECTILE | `causeBulletDamage` |
| `gunGib` | `gun_gib` | IS_PROJECTILE | `causeBulletGibDamage` |
| `chopperBullet` (`s_emplacer`) | `chopper_bullet` | IS_PROJECTILE | `causeDisplacementDamage` |
| `tau` (`s_tau`) | `tau` | IS_PROJECTILE, BYPASSES_ARMOR, ABSOLUTE | `causeTauDamage` |
| `cmb` (`s_combineball`) | `cmb` | IS_PROJECTILE, BYPASSES_ARMOR | `causeCombineDamage` |
| `subAtomic1`..`subAtomic5` (`s_zomg_prefix` + 1..5) | `subatomic_1` .. `subatomic_5` | IS_PROJECTILE, BYPASSES_ARMOR, IS_ENERGY | `causeSubatomicDamage[2-5]`; also target of `getIsSubatomic` |
| `euthanized` (`s_euthanized`) | `euthanized` | BYPASSES_ARMOR | `euthanized(...)` |
| `electrified` (`s_emp`) | `electrified` | BYPASSES_ARMOR, IS_ENERGY | `causeDischargeDamage` |
| `flamethrower` (`s_flamethrower`) | `flamethrower` | BYPASSES_ARMOR, IS_FIRE | `causeFireDamage` |
| `plasma` (`s_immolator`) | `plasma` | BYPASSES_ARMOR | `causePlasmaDamage` |
| `ice` (`s_cryolator`) | `ice` | BYPASSES_ARMOR | `causeIceDamage` |
| `laser` (`s_laser`) | `laser` | BYPASSES_ARMOR, IS_ENERGY | `causeLaserDamage` (both overloads); also target of `getIsTau`-style tag check pattern |

Declared but unused in `ModDamageSource` itself (`s_boil` = `"boil"`, `s_acid` = `"acidPlayer"`): not registered here since no factory method in this file consumes them; flagged below as an open question for whoever owns their real call sites.

### Sedna generic damage classes (replaces `DamageSourceSednaNoAttacker`/`WithAttacker`)

Following the confirmed, already-working Neo Edition mapping (`NtmDamageTypes` + `BulletConfig.DamageClass`) exactly, since this is a general "damage class" concept, not a CE per-weapon string:

| `DamageClass` enum value | Key id | msgId (lang key base) |
|---|---|---|
| `PHYSICAL` | `physical` | `sednaPhysical` |
| `FIRE` | `fire` | `sednaFire` |
| `EXPLOSION` | `explosion` | `sednaExplosion` |
| `ELECTRIC` | `electric` | `sednaElectric` |
| `LASER` | `laser_sedna` (must not collide with the `laser` key above - see risk below) | `sednaLaser` |
| `MICROWAVE` | `microwave_sedna` (collision risk, see below) | `sednaMicrowave` |
| `SUBATOMIC` | `subatomic` | `sednaSubatomic` |
| `OTHER` | `other` | `sednaOther` |

## Risks / open questions

- **Two independent systems both want the ids `laser` and `microwave`.** `ModDamageSource.causeLaserDamage`/nothing named `microwave` directly collide only on `laser` (CE has both a `laser` singleton-style indirect source and, separately, Sedna's `LASER` damage class) - and `ModDamageSource.microwave` (a direct singleton) versus Sedna's `MICROWAVE` damage class. Since registry ids must be unique per `ResourceKey`, the weapons-area owner porting Sedna must pick disambiguated ids (this report proposes `laser_sedna`/`microwave_sedna`, but the actual final names should be agreed with whichever area ports Sedna, since Neo Edition's own reference just calls its Sedna key `LASER`/`MICROWAVE` with id `laser`/`microwave` and apparently never carried forward CE's non-Sedna `laser`/`microwave` sources at all - worth confirming those two CE sources are still needed before Phase 0 locks the id list).
- **`euthanizedSelf` vs `euthanizedSelf2`**: CE declares two nearly identical sources (same flags, different id strings) with no visible difference in intent from this file alone; Neo Edition's reference comments them out entirely (`//public static final ResourceKey<DamageType> euthanizedSelf = ...`). Recommend confirming with whoever owns the euthanization mechanic (likely a mob/entity area) whether both are still needed before finalizing.
- **Per-type exhaustion values**: CE's `DamageSource` never exposed a tunable hunger-exhaustion value (that field did not exist on 1.12.2's `DamageSource`), so there is no CE signal for what value each `DamageType` should use. This report follows the Neo Edition reference's uniform `0.1F` for every entry. If a designer wants different food-exhaustion behavior per damage type (e.g. suicide/self-inflicted sources historically exhaust 0 in vanilla), that is a design decision to make explicitly, not a mechanical port fact.
- **Unused `s_boil`/`s_acid` constants**: declared in `ModDamageSource` but never consumed by a factory method in this file. Likely used by string-literal comparison elsewhere in CE outside this area's scope (e.g. a hazard/potion-effect area) - whichever area finds those call sites should request the corresponding `ResourceKey<DamageType>` be added here (`boil`, `acid_player`) rather than reintroducing a raw string comparison.
- **`getIsTau`/`getIsSubatomic` call sites are outside this area's scope.** Porting the predicates themselves to tag checks is straightforward, but every call site that currently does `if (ModDamageSource.getIsTau(source))` needs updating by whichever area owns that logic (likely combat/entity-effects) once `ModDamageTypes`/`ModDamageTypeTags` exist.
- **`DamageSource` is `final` in 1.21.1/NeoForge** (confirmed by the total absence of any `extends DamageSource` in the Neo Edition reference tree, and by `BulletConfig.getDamage` building sources purely via `damageSources().source(...)`), so there is no direct migration path for `DamageSourceSednaNoAttacker`/`WithAttacker` as classes - this is a hard, unavoidable architecture change, not a stylistic choice. Flagging clearly so the weapons-area implementer doesn't attempt to preserve the class hierarchy.
- **Central datagen entrypoint ownership is outside this area.** This report assumes some other Phase 0 area (or the integration step) owns a `ModDataGenerators` equivalent to `NtmDataGenerators` that a `GatherDataEvent` subscriber lives in; this area's `ModDamageTypeTagsProvider` and `ModDamageTypes::bootstrap` need to be registered there. No such class currently exists in our port project as of this research pass (only `MainRegistry.java` exists, which this area is barred from editing) - the next stage should confirm with the integration owner where that hookup belongs.
