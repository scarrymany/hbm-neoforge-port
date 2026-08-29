# Phase 0 research: Sound events registry (`com.hbm.sound`)

## Scope note

The assigned scope (`com/hbm/sound/**/*.java`, ~20 files) contains the *sound instance*
classes (client-side `MovingSound`/`PositionedSound` wrappers), not the actual
`SoundEvent` registry. In CE the registry itself lives one package over, in
`com.hbm.lib.HBMSoundHandler`. I read it anyway because it is the real subject of
"sound events registry" and every file in `com.hbm.sound` depends on it. I did not
modify or claim ownership of `com.hbm.lib.*` - I only report on it here as a
cross-area dependency and as the source of the id list the task asked me to port.

## Class inventory

| File | Purpose |
|---|---|
| `com.hbm.lib.HBMSoundHandler` (outside assigned scope, read for context) | The actual registry. ~350 `public static SoundEvent` fields + one big `init()` that calls a `register(String name)` helper for each, building a `ResourceLocation(Tags.MODID, name)` and storing it in `Object2ObjectLinkedOpenHashMap<ResourceLocation, SoundEvent> ALL_SOUNDS`. A handful of fields (`alarmHatch`, `metalStep`, `lambdaCore`, etc., ~30 total) are registered directly at field-init time instead of inside `init()`. Also exposes grouped arrays (`geigerSounds`, `voiceSounds`, `boilerGroanSounds`) built from the individual fields, and a `getOrCreate(ResourceLocation)` fallback. |
| `com.hbm.main.ModEventHandler` (outside scope, read for context) | Line ~226: `@SubscribeEvent soundRegistering(RegistryEvent.Register<SoundEvent> evt)` iterates `HBMSoundHandler.ALL_SOUNDS.values()` and calls `evt.getRegistry().register(e)` for each - this is the actual Forge registration hook that fires the whole map into the vanilla `SoundEvent` registry. |
| `sound/AudioWrapper.java` | Empty no-op base class (all methods do nothing / return defaults) used as the server-safe stand-in so calling code doesn't need `@SideOnly(CLIENT)` checks everywhere. Defines the wrapper API: `attachTo`, `updatePosition/Volume/Range/Pitch`, `startSound`, `stopSound`, `isPlaying`, `setKeepAlive`, `keepAlive`. |
| `sound/AudioWrapperClient.java` | `@SideOnly(CLIENT)` real implementation of `AudioWrapper`; wraps an `AudioDynamic` instance and forwards all calls to it. |
| `sound/AudioWrapperClientStartStop.java` | Extends `AudioWrapperClient`; additionally plays a one-shot "start" sound via `world.playSound` when the loop starts and a "stop" sound when it stops (e.g. spin-up/spin-down machine sounds). |
| `sound/AudioDynamic.java` | `@SideOnly(CLIENT)`, extends vanilla `MovingSound`. The core looping/attenuating sound instance: tracks position, volume falloff by distance (linear `func`), pitch, an optional `parentEntity` to follow, and a `keepAlive` tick counter that auto-stops the sound if not refreshed (used for sounds driven by tick-rate game logic that may skip ticks). `start()`/`stop()` go through `Minecraft.getSoundHandler()`, with a guard against Forge's known "value already present" `HashBiMap` crash when re-entering `playSound` too early. |
| `sound/MovingSoundBomber.java` | Client `MovingSound` bound to `EntityBomber` (cross-dependency on `com.hbm.entity.logic`). Global static registry `globalSoundList` so external code can find/stop bomber engine loops. |
| `sound/MovingSoundChopper.java`, `MovingSoundCrashing.java`, `MovingSoundChopperMine.java`, `MovingSoundXVL1456.java` | All extend `MovingSoundPlayerLoop` (a class that itself lives in `com.hbm.sound` per the file list, but wasn't in the 20-file grep output as a top-level `public class` line - it may be an inner/abstract base declared inside one of `SoundLoopPlayer.java`/similar, needs confirmation at implementation time). `MovingSoundChopper`/`MovingSoundCrashing` depend on `com.hbm.entity.mob.EntityHunterChopper`; `MovingSoundXVL1456` depends on `com.hbm.items.ModItems`. |
| `sound/SoundLoopMachine.java` | Extends vanilla `PositionedSound implements ITickableSound`. Generic TileEntity-bound loop: stops itself (`donePlaying = true`) once its `TileEntity` becomes invalid. This is the base class most machine ambient loops build on. |
| `sound/SoundLoopCentrifuge.java`, `SoundLoopBroadcaster.java`, `SoundLoopHeatBoilerIndustrial.java`, `SoundLoopTurbofan.java`, `SoundLoopSiren.java` | All extend `SoundLoopMachine`, each bound to its specific TileEntity type (`TileEntityMachineCentrifuge`/`TileEntityMachineGasCent`, `TileEntityBroadcaster`, `TileEntityMachineTurbofan`, `TileEntityMachineSiren`) to read live state (e.g. siren pitch/volume mode) each tick. |
| `sound/SoundLoopCrucible.java` | Extends vanilla `MovingSound` directly (not `SoundLoopMachine`), tied to `ItemCrucible`/`ModItems`, references `HBMSoundHandler` directly for its sound ids. |
| `sound/SoundLoopGunEgonFire.java` | Extends vanilla `MovingSound` directly; looping fire sound for a specific weapon (egon-style beam gun), not TileEntity-bound. |
| `sound/SoundLoopPlayer.java` | Extends vanilla `MovingSound`; driven by `com.hbm.packet.toclient.PlayerSoundPacket.SoundType` - i.e. the server picks a sound-driven state and a packet tells the client which loop to play. Cross-dependency on the network/packet layer. |

## Key responsibilities

1. **Registry** (`HBMSoundHandler`, technically out of my assigned package but the actual subject): declares every `hbm:<id>` `SoundEvent` used by the mod and registers them into the vanilla registry via a `RegistryEvent.Register<SoundEvent>` handler in `ModEventHandler`.
2. **Sound-instance layer** (`com.hbm.sound`, my assigned scope): client-side, tick-driven sound instances that wrap a `SoundEvent` with runtime behavior - looping, distance-based volume falloff, pitch control, entity/tile-entity attachment, and lifecycle (start/stop/keep-alive/expire). This layer is what tile entities, guns, and entities actually instantiate and drive every tick; it never defines new sound *ids*, it only consumes ids from `HBMSoundHandler`.

## Cross-area dependencies

- `com.hbm.lib.HBMSoundHandler` / `Tags.MODID` - the id source; also referenced directly by name from `sound/SoundLoopCrucible.java`.
- `com.hbm.entity.logic.EntityBomber`, `com.hbm.entity.mob.EntityHunterChopper` - entity area (`MovingSoundBomber`, `MovingSoundChopper`, `MovingSoundCrashing`).
- `com.hbm.items.ModItems`, `com.hbm.items.weapon.ItemCrucible`, `com.hbm.items.machine.ItemCassette.SoundType` - items area.
- `com.hbm.tileentity.machine.*` (Centrifuge, GasCent, Broadcaster, Turbofan, Siren) - tile entity area.
- `com.hbm.packet.toclient.PlayerSoundPacket` - networking area.
- `com.hbm.handler.JetpackHandler` (`MovingSoundJetpack`, listed in the file glob but not read in depth here) - handler area.
- `com.hbm.main.ModEventHandler` - the actual Forge registration call site for the map `HBMSoundHandler.ALL_SOUNDS` builds.
- `com.hbm.blocks.ModSoundType` - separate concern (footstep/block `SoundType`, not `SoundEvent`); confirmed to exist in both CE and Neo Edition as a distinct class from the `SoundEvent` registry. Out of this area's scope but worth flagging so whoever owns `ModSoundType` porting doesn't duplicate `SoundEvent` ids.

## Neo Edition reference confirms real 1.21.1 API shapes

`com.hbm.registry.NtmSoundEvents` (Neo Edition, `hbmsntm`) already does almost exactly
what this area needs to become, confirming these are real, working NeoForge 21.1.228 /
MC 1.21.1 APIs:

```java
public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
    DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, NuclearTechMod.MODID);

public static final DeferredHolder<SoundEvent, SoundEvent> SOME_SOUND = reg("some.id");

private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
    return SOUND_EVENTS.register(name,
        () -> SoundEvent.createVariableRangeEvent(NuclearTechMod.withDefaultNamespace(name)));
}

public static void register(IEventBus eventBus) {
    SOUND_EVENTS.register(eventBus);
}
```

`com.hbm.sound.AudioDynamic` in Neo Edition confirms the client sound-instance
migration path: `MovingSound` -> `net.minecraft.client.resources.sounds.AbstractTickableSoundInstance`,
with `update()` -> `tick()`, `attenuationType` -> `attenuation` (`Attenuation.NONE`),
`repeat` -> `looping`, and playback going through
`Minecraft.getInstance().getSoundManager().play(this)` / `.stop(this)` / `.isActive(this)`
instead of `Minecraft.getMinecraft().getSoundHandler()`.

**Important deviation to avoid copying from Neo Edition:** Neo Edition renamed a
number of ids while porting (e.g. CE's `weapon.reload.revolverCock` became
`weapon.reload.revolver_cock`, `entity.oldExplosion` became `entity.old_explosion`,
and at least one, `block.crateOpen`, was moved to a root-level `crate_open` with no
`block.` prefix). Per this project's hard rule to preserve CE's registry ids exactly,
our port must keep CE's original camelCase ids (`weapon.reload.revolverCock`,
`entity.oldExplosion`, `block.crateOpen`, etc.) rather than following Neo Edition's
renames. Neo Edition is reference for *API shape* only, never for content/ids.

## Recommended NeoForge/Java 21 port plan

1. **New class `com.hbm.sound.ModSounds`** (or `HBMSounds` - name to be finalized with
   whoever owns the entrypoint wiring; avoid `HBMSoundHandler` since CE's own
   `//TODO: rename to NTMSounds` comment shows they consider the name stale).
   - `public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "hbm");`
   - One `public static final DeferredHolder<SoundEvent, SoundEvent>` constant per CE
     `SoundEvent` field, in the same declared order as CE for easy diffing, using a
     private `reg(String name)` helper identical in shape to Neo Edition's, but with
     `NuclearTechMod.withDefaultNamespace` replaced by our own `ResourceLocation.fromNamespaceAndPath("hbm", name)` (or equivalent constant-based helper this project already uses elsewhere - I did not find our project's own `ResourceLocation` helper convention yet since `MainRegistry.java` is off-limits to me).
   - `public static void register(IEventBus modEventBus) { SOUND_EVENTS.register(modEventBus); }` - other agents/the integration step wire this into `MainRegistry`.
   - Keep the grouped arrays (`GEIGER_SOUNDS`, `VOICE_SOUNDS`, `BOILER_GROAN_SOUNDS`) as `SoundEvent[]` (unwrap `DeferredHolder.get()` lazily, e.g. as a `Supplier<SoundEvent[]>` or built once after registry population) since other areas (geiger counter, voice easter eggs, boiler ambience) index into them randomly.
   - This satisfies the task's instruction: "Port into com.hbm.sound as a `DeferredRegister<SoundEvent>`-based registry class other phases will add entries to."
   - **All ~350 CE ids should be ported in this pass** since they are fully enumerated in `HBMSoundHandler` with no external data-driven source - there is nothing "later phase" about the id list itself (only the `.ogg` assets and the `sounds.json` definitions are deferred to the asset-copy phase per the task brief).
2. **Port the sound-instance layer 1:1 per class**, since none of it defines new
   registry content, only playback behavior:
   - `AudioWrapper` -> unchanged in spirit (plain Java, no MC types touched at all - can port verbatim).
   - `AudioWrapperClient`, `AudioWrapperClientStartStop`, `AudioDynamic` -> follow the Neo Edition `AudioDynamic` pattern shown above (`AbstractTickableSoundInstance`, `SoundManager.play/stop/isActive`, `tick()` instead of `update()`). Neo Edition's version is a good structural template; re-derive rather than copy since CE's version has extra features Neo Edition's doesn't (the `nonLegacy`/nonLegacy-vs-legacy volume math branch, `setAttenuation`, the `HashBiMap` crash-guard comment in `start()`) that must be preserved per "preserve business logic" rules.
   - `MovingSoundBomber`, `MovingSoundChopper`, `MovingSoundCrashing`, `MovingSoundChopperMine`, `MovingSoundXVL1456`, `MovingSoundJetpack`, `MovingSoundPlayerLoop` -> port to `AbstractTickableSoundInstance` subclasses once their respective entity/handler classes exist in the target project; these have real cross-package compile dependencies (`EntityBomber`, `EntityHunterChopper`, `JetpackHandler`, `ModItems`) that are owned by other Phase-0 areas, so this sound-instance porting is blocked on those, not on the registry.
   - `SoundLoopMachine` and its TileEntity-bound subclasses (`SoundLoopCentrifuge`, `SoundLoopBroadcaster`, `SoundLoopHeatBoilerIndustrial`, `SoundLoopTurbofan`, `SoundLoopSiren`) -> port `PositionedSound implements ITickableSound` to `AbstractTickableSoundInstance` (MC 1.21.1 merges these concepts); blocked on the corresponding NeoForge block-entity classes existing.
   - `SoundLoopCrucible`, `SoundLoopGunEgonFire`, `SoundLoopPlayer` -> same treatment, blocked on `ItemCrucible`/gun item classes and the `PlayerSoundPacket` networking rewrite respectively.
3. Registration call site: whoever owns `MainRegistry.java` needs to call
   `ModSounds.register(modEventBus)` from the mod constructor, mirroring how
   `NtmSoundEvents.register(eventBus)` is called in Neo Edition's `NuclearTechMod`.

## Data components note

No NBT-on-ItemStack usage was found in any file under `com.hbm.sound`. This area has
no NBT -> Data Component mapping to report; the sound registry deals in `SoundEvent`
ids only.

## Risks / open questions

- **`MovingSoundPlayerLoop`** is referenced as a superclass by four sibling files
  (`MovingSoundChopper`, `MovingSoundCrashing`, `MovingSoundChopperMine`,
  `MovingSoundXVL1456`) but did not appear as its own `public class` declaration in
  the file list for `com.hbm.sound`. It likely exists as a class in one of the files
  I didn't open in full (possibly nested/package-private, or the glob missed a
  filename mismatch). The next stage's implementer must locate and read this class
  before porting the four subclasses above.
- The exact registry-id helper convention for this port project (equivalent to Neo
  Edition's `NuclearTechMod.withDefaultNamespace`) is not yet visible to me since
  `MainRegistry.java` is off-limits; the implementer should check what helper (if
  any) already exists in `com.hbm.main` or `com.hbm.lib` in *our* port project before
  hardcoding `ResourceLocation.fromNamespaceAndPath("hbm", name)` everywhere.
- CE keeps `ALL_SOUNDS` as an explicit `Map<ResourceLocation, SoundEvent>` and a
  `getOrCreate` fallback used somewhere to synthesize sound ids that were not
  pre-declared as static fields (dynamic ids, e.g. dynamically-named sounds). I did
  not find a caller of `getOrCreate` inside `com.hbm.sound` itself; if a caller
  exists elsewhere in CE, `DeferredRegister` cannot register a dynamic/runtime id
  after mod loading closes, unlike Forge's mutable registry-event map. This needs a
  cross-check by whoever else greps CE broadly for `HBMSoundHandler.getOrCreate` -
  it is a potential NeoForge-porting blocker (dynamic registry entries are not
  legal post-bake in NeoForge) but is outside my assigned file scope to resolve.
- Sound category (`SoundCategory`/`SoundSource`) is decided at *playback* call sites
  (`world.playSound(..., cat, ...)`), not at registration time - this is unchanged
  in 1.21.1 (`SoundSource` enum), so no action needed in the registry class itself,
  just confirming it for the implementer.
