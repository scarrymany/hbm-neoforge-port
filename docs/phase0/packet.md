# Phase 0 research report: packet infrastructure

Area key: `packet`. Scope: `com.hbm.packet` direct files (dispatcher/base machinery) plus a
survey-only inventory of `com.hbm.packet.toclient` (31 files) and `com.hbm.packet.toserver`
(13 files). This is research only - nothing has been written under the port project yet.

## 1. Class inventory (CE source, 1.12.2 / Forge SimpleNetworkWrapper style)

### Direct files in `com.hbm.packet` (the 4 files in scope for framework porting)

| File | Purpose |
|---|---|
| `PacketDispatcher.java` | Central registration point. Holds the single `NetworkHandler wrapper` instance (channel name = `Tags.MODID`, i.e. `hbm`). `registerPackets()` assigns each packet class a sequential integer discriminator and a `Side` (CLIENT/SERVER) via `wrapper.registerMessage(...)`, in a fixed order that must never change (the int IDs are positional, not stable IDs - CE ports by re-running the whole list every startup, but on the wire discriminator == index of registration call). Also exposes `LISTENERS`, a list of `IPacketRegisterListener` that lets other mod-like extensions append more packets after the core list and receive the next free ID. `sendTo(IMessage, EntityPlayerMP)` is a small convenience static forwarding to `wrapper.sendTo`. |
| `JetpackSyncPacket.java` | Concrete packet extending `PrecompiledPacket` (see below). Carries an `int playerId` and a `JetpackHandler.JetpackInfo` (jetpack fuel/mode state). Registered **both** as SERVER-bound and CLIENT-bound (same class, two discriminators) - client sends its local jetpack state to the server, server periodically syncs it back down to nearby/self clients. Handler dispatches onto the main thread via `addScheduledTask` on both sides (classic 1.12 pattern for avoiding netty-thread state mutation). |
| `KeybindPacket.java` | Plain `IMessage` (not precompiled). Carries a keybind ordinal (`EnumKeybind`) and pressed/released boolean, sent SERVER-bound only. Server-side handler dispatches straight to `HbmKeybindsServer.onPressedServer` - note this handler does **not** hop to the main thread itself, unlike JetpackSyncPacket; that is a latent thread-safety inconsistency inherited from CE that should not be "fixed" silently in the port write-up (call it out, do not silently add scheduling that changes behavior/timing). |
| `PermaSyncHandler.java` | Not a packet class itself - a **shared read/write utility** used by `PermaSyncPacket` (in `toclient`) to serialize a fixed bundle of "sync every tick" state directly onto a raw `ByteBuf`, deliberately bypassing NBT for size/perf. Bundles: Tom-impact-event floats (`TomSaveData`), a "boykissers" set (player IDs with a specific potion effect active, `HbmPotion.death`), a fixed-size pollution float array (`PollutionHandler.PollutionType.VALUES.length` entries) plus a lookup of the pollution at the syncing player's position, a partial view of the satellite registry (id -> satellite type id only, for rendering), and a "riding desync fix" (re-attaches the client's local mount if the client's local entity's `getRidingEntity()` disagrees with the packet). This class has real cross-area dependencies: `handler.ImpactWorldHandler`, `handler.pollution.PollutionHandler`, `potion.HbmPotion`, `saveddata.TomSaveData`, `saveddata.satellites.SatelliteSavedData`. It is a hard example of a packet payload that is a heterogeneous grab-bag rather than a clean single-purpose message - do not attempt to "clean this up" into separate packets in Phase 0; it must be ported byte-for-byte compatible with the systems above once those systems exist, in whichever phase owns `PermaSyncPacket`. |

### Supporting base classes actually in scope by dependency (`com.hbm.packet.threading`, referenced by `JetpackSyncPacket` and `NetworkHandler`)

| File | Purpose |
|---|---|
| `threading/ThreadedPacket.java` | Base class for packets that must be serializable off the main/netty thread ahead of time ("precompiled"). Holds a lazily-built, pooled direct `ByteBuf` (`PooledByteBufAllocator.DEFAULT.directBuffer()`), built once via the packet's own `toBytes`, cached, and explicitly reference-counted (`getCompiledBuffer()` / `releaseBuffer()`). This exists purely to let CE fire off large volumes of particle/FX packets from worker threads without contending on Netty's encode path per-send. |
| `threading/PrecompiledPacket.java` | Empty marker subclass of `ThreadedPacket`; purely a naming/distinction point, no members. `JetpackSyncPacket` extends this. |

Also read for context (not "in scope" but load-bearing for understanding the dispatch framework):

| File | Purpose |
|---|---|
| `main/NetworkHandler.java` | The actual FML `SimpleChannelHandlerWrapper`-based transport underneath `PacketDispatcher`. Implements a **custom netty codec** (`PrecompilingNetworkCodec extends MessageToMessageCodec<FMLProxyPacket, Object>`) that special-cases `ThreadedPacket` instances (reads their precompiled buffer instead of calling `toBytes` again) versus plain `IMessage` (calls `toBytes` normally). Exposes the full FML target-selection surface: `sendTo`, `sendToAll`, `sendToDimension`, `sendToAllAround`, `sendToAllTracking` (by point or by entity), `sendToServer`, each with a `...Direct` variant. Every non-Direct send acquires `PacketThreading.LOCK` (a lock defined in `com.hbm.handler.threading.PacketThreading`, out of this area's scope) before writing, and delegates `ThreadedPacket` sends into `PacketThreading.createSendTo...ThreadedPacket(...)` helper factories instead of writing directly - i.e. `NetworkHandler` intentionally refuses to hand a `ThreadedPacket` straight to Netty from an arbitrary thread; it always routes it through the `PacketThreading` scheduler first. `flushClient`/`flushServer` (and `...Direct` variants) are called once per tick from client/server tick-end events (not in this area) to batch-flush the channel. |
| `api/network/IPacketRegisterListener.java` | One-method SPI interface (`int registerPackets(int nextId)`) used by `PacketDispatcher.LISTENERS` so external code can append packet registrations without editing `PacketDispatcher` itself. |

### `com.hbm.packet.toclient` and `com.hbm.packet.toserver` (survey only - inventory, not ported)

Each entry below is packet name -> one-line payload summary -> owning feature/subsystem -> the phase that should port it (per the 7-phase plan: Phase 2 = core blocks/items/tiles, Phase 3 = machines/power/fluids, Phase 4 = weapons/combat/entities, Phase 5 = world-gen/satellites/endgame systems; adjust to whatever the actual phase-to-feature mapping document says, this is my best read from CE's own package structure).

**toclient (31):**

| Packet | Payload summary | Owning feature | Suggested phase |
|---|---|---|---|
| `AuxLongPacket` | Generic long[] gauge/state sync for machine tiles (`TileEntityCoreEmitter`/`TileEntityCoreReceiver` referenced) | Generic machine GUI sync framework | Phase 3 (machines) |
| `AuxParticlePacket` | Generic particle spawn (position + particle id + args) | Particle/FX system | Phase 4 (combat/FX) or a shared FX phase |
| `AuxParticlePacketNT` | Same as above but carries an NBT payload ("New Technology" variant) | Particle/FX system (extended) | Same as above |
| `BiomeSyncPacket` | Syncs custom biome data to client | World/biome system | Phase 5 (worldgen) |
| `BufPacket` | Generic raw-ByteBuf-to-tile-entity delivery (`IBufPacketReceiver`) | Generic TE sync framework (faster than NBT) | Phase 3 (machines) - this is itself framework-adjacent; Neo Edition already has a ported example (`network.toclient.BufPacket`) worth reusing as a template |
| `ControlPanelUpdatePacket` | Pushes `ControlPanel`/`DataValue` state to `TileEntityControlPanel` | Control panel / SCADA-like machine system | Phase 3 (machines) |
| `EnumParticlePacket` | Newer enum-keyed particle spawn packet | Particle/FX system | Phase 4/shared FX |
| `ExplosionKnockbackPacket` | Applies ExVNT-style explosion knockback to client player | Explosion/ExVNT system | Phase 4 (combat/explosions) |
| `ExplosionVanillaNewTechnologyCompressedAffectedBlockPositionDataForClientEffectsAndParticleHandlingPacket` | Compressed affected-block-position data for vanilla-style explosion client FX (`explosion.vanillant.standard.ExplosionEffectStandard`) | Explosion/ExVNT system | Phase 4 (combat/explosions) |
| `GunAnimationPacket` | Triggers legacy gun animation (`render.anim.HbmAnimations.AnimType`) | Weapon animation system (legacy) | Phase 4 (weapons) |
| `GunAnimationPacketSedna` | Triggers Sedna-framework gun animation (bus animation, receiver-based) | Weapon animation system (Sedna, newer gun framework) | Phase 4 (weapons) |
| `GunFXPacket` | Activates gun particle/animation FX without needing an entity | Weapon FX | Phase 4 (weapons) |
| `HbmPlayerSyncPacket` | Generic per-player custom data sync | Player capability/extended-properties sync | Phase 2 or 3 depending on what data it actually carries - needs a closer read when its owning system is ported |
| `KeypadClientPacket` | Client-side keypad UI state (`IKeypadHandler`) | Keypad/security system | Phase 3 (machines) |
| `MeathookResetStrafePacket` | Resets sideways acceleration when meathook (`ItemGunShotty`) unhooks | Meathook weapon mechanic | Phase 4 (weapons) |
| `MuzzleFlashPacket` | Syncs Sedna gun muzzle flash for other-entity rendering | Weapon FX (Sedna) | Phase 4 (weapons) - Neo Edition already has `network.toclient.MuzzleFlashPacket` as a working template |
| `PacketSpecialDeath` | Custom death animation (e.g. gluon gun disintegration), carries mesh/triangle render data | Weapon/entity death FX | Phase 4 (weapons/combat) |
| `ParticleBurstPacket` | Rubble/debris particle burst | Explosion/rubble FX | Phase 4 (combat) - Neo Edition has `network.toclient.ParticleBurst` as a template |
| `PermaSyncPacket` | Uses `PermaSyncHandler` (see above) - impact data, death-potion player set, pollution, satellite id map, riding fix | Multiple: impact/meteor system, pollution system, satellite system, potion system | Cross-cutting; must be ported alongside whichever of those systems lands last (likely Phase 5), since it depends on all of them. Neo Edition has `network.toclient.PermaSyncPacket` already, confirm its scope matches CE's before reusing. |
| `PlayerInformPacket` | "Toast"/HUD text announcement (e.g. music disc info) | HUD/notification system | Phase 2 (core UI) - Neo Edition has `network.toclient.InformPlayer` as a direct working template (record + `Component` payload) |
| `PlayerInformPacketLegacy` | Older/legacy variant of the above (e.g. lung damage alert) | HUD/notification system (legacy) | Same as above; decide at port time whether to unify with `PlayerInformPacket` or keep both for behavior parity |
| `PlayerSoundPacket` | Plays a sound on the client | Audio system | Phase 2/shared |
| `RailgunCallbackPacket` | Server->client callback after railgun fire | Railgun weapon (`TileEntityRailgun`) | Phase 4 (weapons) or Phase 3 if railgun is a placed machine-weapon hybrid |
| `RailgunFirePacket` | Sets last-fire-time for railgun client state | Railgun weapon | Same as above |
| `SatPanelPacket` | Sends satellite info to players (`ItemSatInterface`, `Satellite`) | Satellite system | Phase 5 |
| `SerializableRecipePacket` | Syncs a `SerializableRecipe` (loaded/data-driven recipe) to client | Recipe/data-loader system | Phase 2 (recipes are usually early infra) or wherever the custom recipe loader lands |
| `SurveyPacket` | Sends chunk radiation survey data (`RBMKDials`) to individual players | Radiation/RBMK reactor system | Phase 3 (machines, specifically RBMK) |
| `TEDoorAnimationPacket` | Generic door open/close animation state (`IAnimatedDoor`/`IDoor`) | Generic animated-door TE framework | Phase 3 (machines/structures) |
| `TEMissileMultipartPacket` | Sends missile multipart layout to TEs (launch table, compact launcher, missile assembly) | Missile system | Phase 4/5 (missiles are typically a late-game endgame system) |
| `TESirenPacket` | Looped siren sound state (`TileEntityMachineSiren`, cassette track/sound type) | Siren machine | Phase 3 (machines) |
| `TETeslaPacket` | Updates entities currently being zapped by a Tesla coil | Tesla coil machine | Phase 3 (machines) |

**toserver (13):**

| Packet | Payload summary | Owning feature | Suggested phase |
|---|---|---|---|
| `AnvilCraftPacket` | Requests an anvil-recipe craft (`AnvilRecipes`, `ContainerAnvil`) | Anvil crafting system | Phase 2/3 (crafting infra) |
| `AuxButtonPacket` | Generic "button pressed" signal for machine GUIs (referenced by launch table, railgun, many `tileentity.machine.*`) | Generic machine GUI framework | Phase 3 (machines) - this is itself a framework-adjacent generic packet, note for the machine-porting phase |
| `GunButtonPacket` | Gun fire/button-press signal from client | Weapon input | Phase 4 (weapons) |
| `ItemBobmazonPacket` | Buys an offer from the "Bobmazon" in-game shop entity | Bobmazon shop system | Phase 4/5 depending on where the shop entity lands |
| `ItemDesignatorPacket` | Sends laser-designator targeting data to server | Designator/guided-weapon system | Phase 4 (weapons) |
| `KeypadServerPacket` | Server-side keypad input (`IKeypadHandler`) | Keypad/security system | Phase 3 (machines) |
| `MeathookJumpPacket` | Unhooks meathook entity on player jump | Meathook weapon mechanic | Phase 4 (weapons) |
| `NBTControlPacket` | Generic NBT-based control packet for `IControlReceiver` tiles/entities | Generic control framework | Phase 3 - **NBT-on-the-wire pattern; when ported this must NOT become raw NBT read/write on an ItemStack (that's the Data Component rule), but this packet's payload is a free-floating NBT blob sent over the network for a tile/entity control interface, not stored on an ItemStack - it stays a `CompoundTag` inside the packet's `StreamCodec`, which is fine.** Neo Edition already has `network.toserver.CompoundTagControl` as a working template. |
| `NBTItemControlPacket` | Generic NBT-based control packet specifically for the **held ItemStack** (`IItemControlReceiver`) | Generic item control framework | Phase 3/4 depending on which items implement it - **this is the one that actually touches ItemStack state and needs the NBT-key -> DataComponent mapping called out per consuming item at port time; the packet transport itself can still carry a `CompoundTag`/StreamCodec payload, but whatever the receiving item does with that data server-side must write it into components, not `ItemStack#getTag()`/`setTagCompound()`.** Neo Edition has `network.toserver.CompoundTagItemControl` as a template - inspect it when this packet's owning items are ported to see what pattern they chose. |
| `PacketMobSlicer` | Tells server a mob was cut by a cutting sword (`ItemSwordCutter`, `ItemCrucible`) | Melee weapon mechanic | Phase 4 (weapons) |
| `SatCoordPacket` | Coordinate-based satellite command from client | Satellite system | Phase 5 |
| `SatLaserPacket` | Requests an orbital-strike/laser action | Satellite system | Phase 5 |
| `SetGunAnimPacket` | Sets gun animation state server-side (no client-side NBT tag available) | Weapon animation system (legacy) | Phase 4 (weapons) |

Two packets outside `toclient`/`toserver` but registered by `PacketDispatcher` from the root package:
`JetpackSyncPacket` (registered both ways) and `KeybindPacket` (server-bound) - both already
inventoried above as in-scope framework files, not deferred content.

## 2. Key responsibilities of the framework (what Phase 0 must actually port)

1. A single mod-wide payload channel/registrar, versioned, analogous to CE's `NetworkHandler(Tags.MODID)`.
2. A registration point analogous to `PacketDispatcher.registerPackets()` where every concrete
   packet's `Type`/`StreamCodec`/handler triple gets wired in one place.
3. A `sendTo`-style convenience surface covering the same target set CE uses: single player,
   dimension, "all around a point", "all tracking a point/entity", "to server", "to all" - because
   later phases (machines, weapons, satellites) will need all of these, not just player-targeted sends.
4. A decision on the `ThreadedPacket`/precompiled-buffer optimization: NeoForge's
   `CustomPacketPayload` + `StreamCodec` pipeline does not expose the same raw Netty codec hook CE's
   `NetworkHandler` used, so this "precompile off-thread" trick cannot be ported mechanically. See risks below.
5. An extension point equivalent to `IPacketRegisterListener` if other Phase-0 areas need to append
   packet registrations without editing the shared registrar file directly - NeoForge's
   `RegisterPayloadHandlersEvent` is itself already broadcast to every subscriber, so this may turn
   out to be unnecessary (each feature area can subscribe its own listener to the vanilla event
   instead of routing through a custom CE-style list). Flagged as an open question below rather than
   decided unilaterally, since it affects how every later phase wires its own packets.

## 3. Cross-area dependencies

- `PermaSyncHandler` touches `handler.ImpactWorldHandler`, `handler.pollution.PollutionHandler`,
  `potion.HbmPotion`, `saveddata.TomSaveData`, `saveddata.satellites.SatelliteSavedData` - none of
  which exist yet in the port project. The framework itself has no compile-time dependency on these
  (it is a pure ByteBuf reader/writer), so it is safe to port the dispatch framework now; only the
  concrete `PermaSyncPacket` payload needs those systems, and that packet is explicitly deferred.
- `JetpackSyncPacket` depends on `handler.JetpackHandler`/`JetpackHandler.JetpackInfo`, not yet ported.
  Its registration in `PacketDispatcher` is data (a `wrapper.registerMessage` call) but the packet
  class itself cannot be ported until `JetpackHandler` exists - this pushes `JetpackSyncPacket`
  itself out of Phase 0 scope even though it lives directly in `com.hbm.packet`. I am treating the
  request to "port the dispatch framework" as covering `PacketDispatcher`'s registration mechanism
  and the `ThreadedPacket`/`PrecompiledPacket` base classes only, not `JetpackSyncPacket` or
  `KeybindPacket` as concrete payloads (those depend on `JetpackHandler` and `HbmKeybindsServer`/
  `HbmKeybinds`, neither ported yet, and are themselves individual packets that belong with the
  jetpack and keybind systems respectively in a later phase).
- `NetworkHandler`'s `PacketThreading.LOCK` / `PacketThreading.createSendTo...ThreadedPacket` calls
  depend on `com.hbm.handler.threading.PacketThreading`, out of this area's scope and not ported yet.

## 4. Recommended NeoForge 21.1 / Java 21 port plan

Confirmed against the Neo Edition reference (`com.hbm.network.NtmNetwork`,
`com.hbm.network.toclient.InformPlayer`/`BufPacket`, `com.hbm.network.toserver.*`, and
`PacketDistributor` call sites in `NuclearTechModClient`/`HbmPlayerAttachments`/entity classes) as
real, working 21.1.228 API shapes - nothing below is guessed:

- **Registrar class**: `com.hbm.packet.HbmNetwork` (package `com.hbm.packet`, matching CE's
  `com.hbm.packet.PacketDispatcher` location one-to-one), annotated
  `@EventBusSubscriber(modid = Tags.MODID)` (confirm `Tags.MODID` constant name/location against
  whatever the integration step already established; CE's own `Tags.MODID` is `"hbm"`), with a
  `@SubscribeEvent public static void registerPackets(RegisterPayloadHandlersEvent event)` method.
  Inside: `PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION)` (a private `static final
  String PROTOCOL_VERSION` constant, bump manually on wire-incompatible changes, mirroring
  `NtmNetwork.PROTOCOL_VERSION`), then one `registrar.playToClient(Type, StreamCodec, handler)` or
  `registrar.playToServer(...)` call per concrete packet, added as each feature phase introduces its
  packets. Phase 0 itself registers zero concrete packets (see section 3) - the file ships with an
  empty (but structurally correct and compiling) `registerPackets` method, ready for later phases to
  extend by adding lines, exactly like CE's own dispatcher started as a list that grew over years.
- **Per-packet shape** (documented here for later phases, not written in Phase 0): a `record
  Foo(...) implements CustomPacketPayload` with `public static final Type<Foo> TYPE = new
  Type<>(ResourceLocation.fromNamespaceAndPath(Tags.MODID, "foo"))` (or whatever the port's own
  `withDefaultNamespace`-style helper ends up being called - Neo Edition uses
  `NuclearTechMod.withDefaultNamespace(...)`, our port's equivalent should live on the main mod
  class, outside this area's edit scope), a `public static final StreamCodec<RegistryFriendlyByteBuf,
  Foo> STREAM_CODEC`, a `handleClient`/`handleServer`/`handleCommon` static method taking
  `(Foo packet, IPayloadContext context)` and doing `context.enqueueWork(() -> { ... })` for any
  main-thread-only work, and an `@Override public Type<Foo> type() { return TYPE; }`.
- **Sending helper**: do not reintroduce a custom `NetworkHandler` class - NeoForge's
  `net.neoforged.neoforge.network.PacketDistributor` already provides the full target surface CE's
  `NetworkHandler` hand-rolled: `sendToPlayer(ServerPlayer, payload)`, `sendToServer(payload)`,
  `sendToPlayersNear(ServerLevel, ServerPlayer exclude, x, y, z, radius, payload)`, and (per NeoForge
  21.1 conventions, not yet observed in a Neo Edition call site but standard API) `sendToAllPlayers`,
  `sendToDimension`/level-wide variants. Recommend deleting the CE-style dispatcher-owns-a-wrapper
  pattern entirely: `PacketDistributor` static methods are the port's `PacketDispatcher.sendTo`
  replacement, called directly from feature code, so `HbmNetwork` only needs the registration method,
  not a parallel send-helper API surface. This is a deliberate simplification versus CE, not a gap -
  confirm with the integration step that no other Phase-0 area expects a `PacketDispatcher.sendTo(...)`
  static method to still exist, since callers should be updated to call `PacketDistributor` directly.
- **Threaded/precompiled packets**: NeoForge's `CustomPacketPayload` pipeline does not expose a raw
  Netty `MessageToMessageCodec` hook the way CE's `NetworkHandler` did, so CE's exact "pre-serialize on
  a worker thread, replay the cached buffer on the Netty thread" trick has no direct 1:1 API
  equivalent to port. Recommend **not** porting `ThreadedPacket`/`PrecompiledPacket` as infrastructure
  in Phase 0: `StreamCodec.encode` already runs off the render/game thread when NeoForge itself
  schedules the write, and the actual reason CE needed manual precompilation (avoiding repeated
  `toBytes()` calls when broadcasting one packet to many recipients via `sendToAllTracking`) is
  largely moot under `StreamCodec`, since NeoForge/Netty already encodes once per payload object
  and reuses the encoded buffer across multiple send targets, not once per recipient. Flagged as a
  risk below for a later phase to revisit only if profiling actually shows this matters (e.g. for the
  `AuxParticle`/particle-burst family, which used `ThreadedPacket` in CE specifically for
  high-frequency FX broadcast).
- **Package layout**: keep `com.hbm.packet` as the registrar/dispatch home, matching CE, rather than
  copying Neo Edition's `com.hbm.network` naming - CE cross-referencing is the priority per the hard
  rules, and CE's own root package for this system is `com.hbm.packet`. `toclient`/`toserver`
  subpackages should be preserved 1:1 as well, so each later phase's packet lands at the same
  relative path it has in CE (e.g. `com.hbm.packet.toclient.PlayerInformPacket`,
  `com.hbm.packet.toserver.AnvilCraftPacket`), rather than Neo Edition's flatter `network.toclient`/
  `network.toserver` naming.

## 5. Risks / open questions

1. **No `IPacketRegisterListener` equivalent decided.** CE's dispatcher lets other systems append
   registrations after the fact via a static listener list. NeoForge's `RegisterPayloadHandlersEvent`
   is itself a mod-bus event any class can subscribe to independently, so each future phase's own
   packet-owning class could subscribe directly instead of routing through `HbmNetwork`. Recommend the
   integration step decide once, for consistency, whether all packet registration funnels through one
   `HbmNetwork.registerPackets` method (single source of truth, matches CE's centralization) or is
   spread across each feature package's own `@SubscribeEvent` method (matches NeoForge idiom, avoids a
   central file every phase must edit). I have written the plan above assuming centralization to match
   CE's structure and the "preserve package layout" rule, but this is worth an explicit decision before
   Phase 2 starts adding to it.
2. **`ThreadedPacket`/precompiled-buffer optimization has no direct port path**, as detailed above.
   Whichever phase ports the particle/FX packets that relied on it (`AuxParticlePacket`,
   `ParticleBurstPacket`, `JetpackSyncPacket`) needs to confirm plain `StreamCodec` throughput is
   acceptable, or design a NeoForge-native batching strategy if not - do not assume this is a solved
   problem.
3. **`KeybindPacket`'s missing main-thread hop** (see inventory table) is a pre-existing CE quirk, not
   a NeoForge migration concern, but it is worth flagging explicitly to whichever phase ports keybinds
   so it is a conscious behavior-preservation decision, not an accidental omission.
4. **`PermaSyncPacket`/`PermaSyncHandler` cross-cuts four unrelated systems** (impact, pollution,
   potion, satellites) in a single ByteBuf layout. Whichever phase ports it must port all four
   dependencies' relevant fields simultaneously, or the wire format silently breaks; it cannot be
   split cleanly per-system without changing the packet's on-wire shape (out of scope for Phase 0 to
   decide, flagged for the phase that picks it up, likely last of the four dependent systems to land).
5. **CE's positional/sequential discriminator IDs are not preserved by NeoForge** - `PayloadRegistrar`
   keys packets by `ResourceLocation`/`Type`, not by registration order, so there is no equivalent
   "int ID must match this exact call order" constraint to carry over. This is a strict simplification,
   not a risk, but noted so nobody tries to preserve CE's `i++` ordering in the new registrar for no
   reason.
6. **Protocol version string**: Neo Edition uses `"3"` for its own, unrelated fork; our port needs its
   own independent `PROTOCOL_VERSION` starting fresh (e.g. `"1"`), not copied from Neo Edition, since
   the two mods do not need wire compatibility with each other.
