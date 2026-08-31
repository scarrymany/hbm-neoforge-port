# Phase 5 research: Particle engine & generic VFX

**Area:** `particle_engine_and_generic_vfx` — CE's `com.hbm.particle`/`com.hbm.particle_instanced`
rendering technologies, and CE's generic named-particle-effect network broadcast system
(`AuxParticlePacketNT`/`HbmEffectNT`) that ~30 already-committed Phase 3/4 classes in this port stub out
pending this research landing.

## Method

Read directly, with exact paths/line counts cited inline:
- `upstream/hbm-ce/src/main/java/com/hbm/particle/**` (72 files) and `com/hbm/particle_instanced/**` (8
  files) — CE 1.12.2 source, the sole source of truth for behavior/visual intent/effect catalog.
- `upstream/hbm-ce/src/main/java/com/hbm/packet/toclient/AuxParticlePacketNT.java` and
  `com/hbm/render/InstancedBillboardBatch.java` — the real network payload and the real (raw-GL)
  hardware-instancing mechanism.
- `upstream/neo-edition` (partial 1.21.1 NeoForge port, `neo_version=21.1.228`, same `minecraft_version`
  as this port) — used **only** to confirm real NeoForge/Blaze3D 1.21.1 client API shapes (event names,
  method signatures, registration call sites), never for visual design or dispatch-catalog completeness.
  It targets Sodium compatibility (`sodium_version=0.8.12-alpha.2+mc1.21.1` in `build.gradle`), which
  turns out to matter (see Headline finding 2).
- This port's own `src/main/java/com/hbm/**` — grepped for `AuxParticlePacketNT`/`HbmEffectNT` (36 files,
  ~30 distinct call sites) and read `ExplosionEffectSyncPacket.java`/`RadFogPayload.java`/
  `ClientProxy.java`/`ServerProxy.java`/`ClientModRegistry.java`/`HbmNetwork.java`/`MainRegistry.java` in
  full for the real, already-established `CustomPacketPayload`/proxy/client-bootstrap conventions this
  design must slot into.
- `docs/phase0-4/*.md` and `docs/phase5/renderer_framework_and_obj_models.md` (a sibling Phase 5 report,
  already landed) for established conventions and cross-references naming this area.

## Headline findings

1. **CE's "instanced particle engine" is literal raw-OpenGL hardware instancing behind a custom GLSL
   shader — not a design pattern that survives into 1.21.1 at all, and neo-edition's real code confirms
   the actual replacement is architecturally simpler, not a "shader-compat fallback."** PORT_SPEC.md's
   framing ("rewritten on modern render pipeline with a shader-compat fallback path") assumes the CE
   mechanism is something to reimplement with a modern-shader equivalent plus a fallback. It isn't: CE's
   `InstancedBillboardBatch` (`upstream/hbm-ce/.../render/InstancedBillboardBatch.java:1-140`) hand-rolls
   a VAO + per-instance VBO with `vertexAttribDivisor` and `glDrawArraysInstanced(GL_QUADS, ...)`
   (lines 111-132, 55), gated behind a custom `lit_particles` GLSL shader
   (`ResourceManager.lit_particles`, `HbmShaderManager2`) that **requires OpenGL 3.3 core** and is
   force-disabled by `GeneralConfig.instancedParticles` whenever `GLCompat.error` is non-empty
   (`upstream/hbm-ce/.../config/GeneralConfig.java:201-206`) or a shaderpack is active (the config
   comment literally says "will break with shaders",
   `GeneralConfig.java:161`). This is real GPU hardware instancing, confirmed by the `vertexAttribDivisor`
   calls — not a figure of speech. **Confirmed real replacement** (neo-edition,
   `particle/engine/ParticleEngineNT.java` 86 lines + `particle/engine/ParticleNT.java` 209 lines +
   `particle/engine/EngineHandler.java` 35 lines, all compiling against `neo_version=21.1.228`): a
   lightweight custom particle-object list (`ParticleNT`, NOT extending vanilla `Particle`) ticked and
   rendered by a singleton (`ParticleEngineNT.INSTANCE`), hooked into rendering via
   `net.neoforged.neoforge.client.event.RenderLevelStageEvent`
   with `Stage.AFTER_WEATHER` (`EngineHandler.java:21-27`), batched by vanilla's own
   `net.minecraft.client.renderer.RenderType` through a `MultiBufferSource.BufferSource` obtained from
   `Minecraft.getInstance().renderBuffers().bufferSource()` — i.e. it reuses Mojang's own
   already-hardware-batched `VertexConsumer`/`BufferBuilder` upload path instead of hand-rolling a second
   one. **No custom shader, no OpenGL-version probing, no fallback path is needed at all** — the
   "instanced" name survives only as a legacy label; the real 1.21.1 mechanism achieves the same
   "many particles rendered cheaply" goal by piggy-backing on vanilla's existing batching, which is
   available unconditionally on every client that can run Minecraft 1.21.1. See "Recommended
   architecture" below for the concrete design this port should copy.

2. **This port's own `GeneralConfig` already ported CE's `instancedParticles` config *key* (including the
   "may break with shaders" comment text) verbatim, and its own class javadoc already flags the exact gap
   this research needs to close** — `src/main/java/com/hbm/config/GeneralConfig.java:63,228-230`
   (`INSTANCED_PARTICLES` / `"instancedParticles"`, default `true`) plus the class javadoc at lines 18-22:
   *"The GL 3.3 capability gating that force-disabled `instancedParticles`... via
   `com.hbm.render.GLCompat`. GL 3.3 is far below Minecraft 1.21's own baseline, so the original safety
   net is obsolete; whoever owns the render pipeline intersection should confirm whether any such gate is
   still needed at all."* **Answer, now confirmed**: no gate is needed, for the reason in Finding 1 — the
   modern replacement has no hardware dependency to probe for in the first place, because it isn't raw GL
   instancing. `GLCompat.java` does not exist anywhere in this port (confirmed, `find` returns nothing) and
   should not be created. Recommendation: keep the `instancedParticles` boolean (for CE-config-key
   parity and because it's a legitimate perf/detail knob) but redefine what it gates — see "Recommended
   architecture."

3. **CE's real generic broadcast system is a single `AuxParticlePacketNT` payload
   (`{HbmEffectNT effect, NBTTagCompound nbt, double x, y, z}`) dispatching through an 87-value enum
   (`HbmEffectNT`), each value carrying its own client-only lambda handler** — not 87 separate packet
   classes, and not the deprecated string-keyed path some call sites and neo-edition itself still use.
   Full detail in "CE's real `AuxParticlePacketNT`/`HbmEffectNT` system" below.

4. **neo-edition only ported the *deprecated legacy* half of this system, not the real one — do not
   copy its dispatch shape, only its confirmed network/rendering API calls.** neo-edition's
   `com.hbm.network.toclient.AuxParticle` (`CustomPacketPayload` record, 53 lines) and
   `NuclearTechModClient.effectNT(CompoundTag)` (`main/NuclearTechModClient.java:751-901+`) are a
   line-for-line port of CE's `@Deprecated AuxParticlePacketNT(NBTTagCompound, x, y, z)` constructor and
   `EffectNTLegacyAdapter`/`ClientProxy.effectNT(NBTTagCompound)` string-keyed compatibility shim — the
   *old* pre-`HbmEffectNT` mechanism CE kept around only so old save-embedded/mod-compat NBT blobs still
   resolve. neo-edition never built the real `HbmEffectNT` enum or its `EffectHandler` per-value lambda
   table at all; it just re-inlined the legacy string `switch` directly into its client bootstrap class.
   This is exactly the kind of "partial/incomplete, diverges from CE" gap the task's ground rules warned
   about. This port has no legacy save data to stay compatible with (it is a from-scratch NeoForge port,
   not an in-place CE upgrade), so the *remapping table* half of the deprecated path (old string spellings
   → newer enum values) is pure historical baggage with no value to port. A narrower piece of it is still
   worth keeping in mind, though: CE itself still has a few live, non-decorative-legacy call sites
   (`BlockEmitter`/`PartEmitter`/`TileEntityMachineThresher`, none built in this port yet) that select the
   effect **by name from block configuration data** rather than a compile-time enum constant — see "CE's
   real `AuxParticlePacketNT`/`HbmEffectNT` system" below for the correction and what, narrowly, is worth
   keeping. Build the clean modern `HbmEffectNT`-equivalent enum first; add a by-name lookup only if/when
   a data-driven emitter block is ported.

5. **This port already has every non-particle-specific piece of plumbing this design needs, already real
   and already in the exact shape to extend** — confirmed by reading the files directly, not inferred:
   - `MainRegistry.proxy` static field (`ServerProxy` or `ClientProxy` instance,
     `MainRegistry.java:59,67`) — the exact CE `MainRegistry.proxy` pattern, ready for an `effectNT(...)`
     override pair (`ServerProxy.effectNT` no-op base, `ClientProxy.effectNT` real client dispatch),
     mirroring CE's own `ServerProxy.java:41,43-44` / `ClientProxy.java:392-394` 1:1.
   - `ExplosionEffectSyncPacket`/`RadFogPayload` (`src/main/java/com/hbm/packet/toclient/*.java`) — two
     already-real `CustomPacketPayload` records with `Type<>` + `StreamCodec<RegistryFriendlyByteBuf,_>`
     + `@OnlyIn(Dist.CLIENT) handleClient(..., IPayloadContext)` + `context.enqueueWork(...)`, registered
     in `HbmNetwork.java` via `registrar.playToClient(TYPE, STREAM_CODEC, ::handleClient)`. This is the
     exact wire-shape template the task asked for and the one this design reuses verbatim.
   - `ClientModRegistry` (`src/main/java/com/hbm/main/ClientModRegistry.java`, `@Mod(dist=Dist.CLIENT)` +
     `@EventBusSubscriber(bus=MOD)`) — the client-only bootstrap class (mirrors neo-edition's
     `NuclearTechModClient`) with an already-empty, already-commented `onClientSetup(FMLClientSetupEvent)`
     slot explicitly waiting for "the first area that needs client-only setup work" — this is where the
     effect-handler-table registration call belongs.
   - `net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(ServerLevel, excluded, x, y, z,
     radius, payload)` and `.sendToPlayer(ServerPlayer, payload)` are both already in live use elsewhere
     in this port (`RadiationSystemNT.java:706`, `ExplosionEffectStandard.java:41`,
     `ItemSatInterface.java:69`) — confirmed real signatures, and independently cross-confirmed by
     neo-edition's own `IParticleCreator.sendPacket` (`particle/helper/IParticleCreator.java:16-24`),
     which uses the identical `PacketDistributor.sendToPlayersNear(ServerLevel,...)` call. CE needs both
     shapes too: `PacketThreading.createAllAroundThreadedPacket` (radius broadcast — the common case) and
     `PacketThreading.createSendToThreadedPacket` (single-player send — used exactly once, by
     `MachinePWRController`'s error marker, `upstream/hbm-ce/.../MachinePWRController.java:220`).

6. **`ExplosionChaos.spawnChlorine`/`spawnVolley`'s TODOs in this port undersell their own blocker — the
   real gap is a missing gameplay entity, not (only) missing VFX.** CE's `spawnChlorine`/`spawnVolley`
   (`upstream/hbm-ce/.../ExplosionChaos.java:279-304,705-722`) each do two independent things per
   iteration: broadcast an `AuxParticlePacketNT` (genuinely Phase 5/this area), **and** spawn a real
   server-side `EntityModFXShadow` (`upstream/hbm-ce/.../entity/particle/EntityModFXShadow.java`, 173
   lines) — an invisible (`trackingRange=0`, never sent to any client), `noClip` physics-simulated
   projectile that flies a ballistic arc and, on colliding with a solid block roughly every 50 ticks,
   calls `ExplosionChaos.poison`/`.c`/`.pc` (real chlorine/cloud/pink-cloud gas damage application),
   converts terrain to `BlockCloudResidue`, places `PinkCloudBroadcaster` blocks, or (ORANGE type)
   transmutes a 3×3×3 block region via `ExplosionNukeGeneric.solinium`. **None of that is client VFX** —
   it is real, gameplay-consequential, server-only simulation that happens to share a helper method with
   a particle broadcast. This port's `ExplosionChaos.java:57-61,361-369` javadoc/TODOs currently file the
   entire method under "Phase 5... particle/networking infra," which will cause the eventual
   implement-wave to think landing this research area alone unblocks `spawnChlorine`/`spawnVolley` — it
   does not. Porting `EntityModFXShadow` (a real Phase 3/4-shaped gameplay-entity task, independent of
   this area) is a **separate prerequisite**, named here as an open question for whoever picks this back
   up (see Open questions).

## CE's real `AuxParticlePacketNT`/`HbmEffectNT` system

**The packet** (`upstream/hbm-ce/src/main/java/com/hbm/packet/toclient/AuxParticlePacketNT.java`, 112
lines): a `ThreadedPacket` (Forge-1.12-era `IMessage`) carrying either the legacy `NBTTagCompound`-only
payload (deprecated) or the modern triple `{HbmEffectNT effect, @Nullable NBTTagCompound nbt, double x,
y, z}`. `toBytes`/`fromBytes` use `PacketBuffer.writeEnumValue(HbmEffectNT)`/`readEnumValue(HbmEffectNT
.class)` — Forge's ordinal-based enum codec. The `Handler` (lines 93-111) runs entirely
`@SideOnly(Side.CLIENT)`, schedules onto the client thread via `Minecraft.addScheduledTask`, resolves an
`I18nUtil` label key if present, then calls `MainRegistry.proxy.effectNT(m.effect, m.posX, m.posY,
m.posZ, m.nbt)` (or the legacy `effectNT(NBTTagCompound)` overload for old-style packets).

**The catalog** (`upstream/hbm-ce/src/main/java/com/hbm/particle/helper/HbmEffectNT.java`, 1503 lines):
a plain Java `enum` with **exactly 87 named constants** (counted directly from the enum body,
lines 60-76) — not a placeholder `EnumHbmParticles.java` (that unrelated file,
`com/hbm/particle/EnumHbmParticles.java`, is a 5-line dead stub with a single `PARTICLES` value and no
callers; ignore it, it is not part of this system). Each `HbmEffectNT` constant carries a private
`Object handler` field set once via `@SideOnly(Side.CLIENT) setHandler(EffectHandler)`
(lines 1473-1487) — `EffectHandler` is a `@FunctionalInterface` with one method,
`summonParticle(World, double x, double y, double z, @NotNull NBTTagCompound data)` (lines 1498-1502).
All 87 handlers are registered once, client-side only, inside the single static method
`registerClientHandlers()` (lines 80-1471) — called from CE's `ClientProxy.preInit`. Dispatch is
`type.summonParticle(...)` from `ClientProxy.effectNT(HbmEffectNT type, x, y, z, nbt)`
(`upstream/hbm-ce/.../main/ClientProxy.java:392-394`); the server-side `ServerProxy.effectNT(...)`
override is a no-op (`upstream/hbm-ce/.../main/ServerProxy.java:41,43-44`) — the whole thing is a
pure client-side effect-dispatch table; the server only ever *sends the packet*, never runs a handler.

**Handler bodies span three real render technologies, not one**, confirmed by reading representative
handlers across the full range:
- **Plain vanilla `Particle` spawn** — most common. E.g. `CasingNT`/`Flamethrower`/`ExplosionSmall`/
  `ExplosionLarge`/`BlackPowder`/`Ashes`/`Skeleton` (lines 81-94) delegate to the small
  `IParticleCreator`-backed registry in `ParticleCreators.java` (7 creators: `CASING`, `FLAME`,
  `EXPLOSION_SMALL`, `EXPLOSION_LARGE`, `BLACK_POWDER`, `ASHES`, `SKELETON` —
  `helper/ParticleCreators.java:8-31`); most others (`WaterSplash`, `CloudFX2`, `LaunchSmoke`, the seven
  `Smoke_*` variants, `Muke`, `Haze`, `Hadron`, `Rift`, etc.) just `new` a `com.hbm.particle.Particle*`
  subclass and call `Minecraft.getMinecraft().effectRenderer.addEffect(fx)` — ordinary vanilla-style
  particles, ~50+ of the 87.
- **Custom-blend-layer batched particles** (`ParticleBatchRenderer`/`ParticleLayerBase`/
  `ParticleRenderLayer`, `com/hbm/particle/ParticleBatchRenderer.java` 123 lines +
  `ParticleLayerBase.java` 13 lines + `ParticleRenderLayer.java` 51 lines) — a hand-rolled "like vanilla's
  particle manager but supports more GL states" system (the class's own comment, line 27), used by the
  most complex handler in the whole file: `BulletImpact` (lines 1152-1298, ~146 lines on its own),
  which branches on hit-material (`Material.IRON`/`ROCK`/`SAND`/`WOOD`/`LEAVES`/entity) to spawn
  material-tinted debris (`ParticleHitDebris`), a bullet-hole decal (`ParticleBulletImpact`), smoke
  (`ParticleSmokeAnim`), blood (`ParticleBloodParticle`, gated on `GeneralConfig.bloodFX`), or (metal
  hits) a nested same-tick re-dispatch to `HbmEffectNT.Spark` via `MainRegistry.proxy.effectNT(Spark, x,
  y, z, nbt)` — confirming `effectNT` is also called *directly, client-side, without a network
  round-trip* whenever the triggering code is already running on the client (this is the shape the port
  should preserve: `MainRegistry.proxy.effectNT(...)` is a general "run this named VFX now" entry point,
  not only a packet-deserialization target).
- **True GPU hardware-instanced particles** (`com.hbm.particle_instanced`, 8 files) — only 6 of the 87
  effects use this path, each with an `if (GeneralConfig.instancedParticles) {...instanced...} else
  {...classic Particle fallback...}` branch at the `HbmEffectNT` call site itself (not inside the
  instanced classes): `RadFog`→`ParticleRadiationFogInstanced`/`ParticleRadiationFog` (lines 141-148),
  `Smoke_Cloud`/`Smoke_Radial`→`ParticleExSmokeInstanced`/`ParticleExSmoke` (lines 165-218),
  `RBMKFlame`→`ParticleRBMKFlameInstanced` (line ~888), `RBMKMush`→`ParticleRBMKMushInstanced`,
  `RBMKSteam`→`ParticleRBMKSteamInstanced`, plus `MissileContrail`/`Exhaust_*`→
  `ParticleRocketFlameInstanced`/`ParticleRocketFlame`. Mechanism confirmed in
  `InstancedParticleRenderer.java` (177 lines): particles queue into a shared `ArrayDeque`
  (max 16384, oldest evicted), a `@SubscribeEvent RenderWorldLastEvent` handler batches by
  `ParticleInstanced.RenderType` (5 render types: `DEFAULT_BLOCK_ATLAS`, `RBMK_FLAME`, `RBMK_STEAM`,
  `RBMK_MUSH`, `RADIATION_FOG`, each its own texture + blend-func + fog/lighting/depth toggles,
  `ParticleInstanced.java:75-156`), then one `glDrawArraysInstanced` call per render type via
  `InstancedBillboardBatch.draw()`.
- **Wildcard/legacy escape hatches**: `Vanilla` (lines 1299-1306) spawns *any* vanilla
  `EnumParticleTypes` by string name from `data.getString("mode")` — a generic pass-through worth keeping
  as a fallback case in the modern enum (mapped onto `BuiltInRegistries.PARTICLE_TYPE` lookup by
  `ResourceLocation`). `Anim` (lines 1307+) drives CE's `BusAnimation`/`HbmAnimations` item-animation-bus
  system — **not particle VFX at all**; this port has already, correctly, chosen not to build it and
  substituted `ToolAnimationType`/`GunAnimationPayload` instead (confirmed by
  `src/main/java/com/hbm/weapon/anim/ToolAnimationType.java:7-12`'s own javadoc) — treat `Anim` as
  "intentionally not ported, already resolved differently," not as a gap this area needs to fill.

**The legacy/deprecated half** (`helper/EffectNTLegacyAdapter.java`, 129 lines): a
`Map<String, Function<NBTTagCompound, HbmEffectNT>>` that converts old string-keyed NBT (`type`,
`posX`/`posY`/`posZ`, and for multi-mode keys like `"smoke"`/`"vanillaburst"` a nested `"mode"` string) to
the same `HbmEffectNT` values. **Correction after a closer check** (do not take the first pass's word for
this): this is not *only* a dead save-compat shim — CE itself still has a handful of **live, uncommented,
non-`@Deprecated`-annotated** call sites using the 4-arg legacy constructor
(`AuxParticlePacketNT(NBTTagCompound, x, y, z)`, i.e. no `HbmEffectNT` argument at all) directly:
`blocks/generic/BlockEmitter.java:219`, `blocks/generic/PartEmitter.java:115`, and
`tileentity/machine/TileEntityMachineThresher.java:201` (confirmed by reading each call site's full
surrounding block, not just the grep hit line). `PartEmitter.java:114` explicitly gates the call on
`data.hasKey("type")` — this is a **decorative, data-driven particle-emitter block** where the emitted
effect's name is configuration data (presumably NBT set by a player/map tool), not a Java compile-time
constant, which is exactly the case a string-keyed lookup is good for and an `enum` constant reference at
the call site is not. **None of `BlockEmitter`/`PartEmitter`/`TileEntityMachineThresher` exist anywhere in
this port yet** (confirmed, no matching files), so this has no bearing on already-committed code today —
but it means the recommendation isn't a flat "never port the string-keyed path": if/when this port ports
a decorative particle-emitter block, it will want a `String`/`ResourceLocation`→`HbmEffect` name lookup
(trivial to add — a `Map<String, HbmEffect>` or `HbmEffect.byName(String)` via `Enum::valueOf` guarded
with a null-safe wrapper — not the full `EffectNTLegacyAdapter` machinery, which exists only to
*additionally* remap old pre-`HbmEffectNT` string spellings like `"radiationfog"`→`RadFog` onto the newer
enum, a remapping concern this port will never have since it has no CE 1.12.2 save data to stay
compatible with). Every other real (non-decorative-emitter) call site in CE already uses the modern
5-arg `AuxParticlePacketNT(HbmEffectNT, nbt, x, y, z)` constructor directly, so the full
`EffectNTLegacyAdapter` remapping table itself is still not worth porting — only the "look an effect up
by name" capability it happens to also provide is worth keeping in mind.

## Recommended architecture for this port

Three independent decisions, each already de-risked by a confirmed real precedent:

**1. The network payload — one packet, matching `ExplosionEffectSyncPacket`'s exact shape.**

```java
public record HbmEffectPacket(HbmEffect effect, double x, double y, double z, CompoundTag data)
        implements CustomPacketPayload {
    public static final Type<HbmEffectPacket> TYPE = ...;
    public static final StreamCodec<RegistryFriendlyByteBuf, HbmEffectPacket> STREAM_CODEC = ...;
    // effect: ByteBufCodecs.VAR_INT.map(ordinal -> EnumUtil.grabEnumSafely(HbmEffect.VALUES, ordinal),
    //   HbmEffect::ordinal) — copy KeybindPacket.java:41-48's already-real pattern verbatim.
    // data: buf.readNbt()/writeNbt() (same as neo-edition's own AuxParticle, confirmed real API).
    @OnlyIn(Dist.CLIENT) public static void handleClient(HbmEffectPacket p, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (Minecraft.getInstance().level == null) return;
            p.effect().summonParticle(Minecraft.getInstance().level, p.x(), p.y(), p.z(), p.data());
        });
    }
    @Override public Type<HbmEffectPacket> type() { return TYPE; }
}
```
Register in `HbmNetwork.java` next to `ExplosionEffectSyncPacket`/`RadFogPayload` with
`registrar.playToClient(HbmEffectPacket.TYPE, HbmEffectPacket.STREAM_CODEC,
HbmEffectPacket::handleClient)`. Server-side send helper (mirrors CE's `IParticleCreator.sendPacket` and
neo-edition's identical real call): a static `HbmEffect.sendPacket(ServerLevel, HbmEffect, x, y, z,
double radius, @Nullable CompoundTag)` wrapping `PacketDistributor.sendToPlayersNear(level, null, x, y,
z, radius, packet)`, plus a `sendPacket(ServerPlayer, HbmEffect, x, y, z, data)` overload wrapping
`PacketDistributor.sendToPlayer(...)` for the one single-player use case (`MachinePWRController`-style
error markers). This is the "single generic `CustomPacketPayload` parametrized by a named effect-type
enum" the task asked for, and it is a direct structural copy of a payload already real in this codebase.

**2. The effect-type enum — port `HbmEffectNT`'s *shape* (enum + per-value client lambda handler), not
its legacy string-dispatch companion.** A Java enum (call it `HbmEffect`, in `com.hbm.particle` or
alongside the packet) with one constant per CE `HbmEffectNT` value actually needed by a real call site in
this port (start from the ~30-call-site list below, not all 87 — CE's own catalog includes effects with
zero surviving callers in this port, e.g. anything gated behind content this port hasn't built), each
holding a `EffectHandler` functional-interface field set via a client-only `registerHandlers()` static
method called once from `ClientModRegistry.onClientSetup` (the exact empty slot already waiting at
`ClientModRegistry.java:56-64`). Dispatch entry point: `MainRegistry.proxy.effectNT(HbmEffect, x, y, z,
CompoundTag)` — a new method pair on `ServerProxy` (no-op base, matching CE's `ServerProxy.java:41,43-44`
1:1) / `ClientProxy` (real dispatch, matching CE's `ClientProxy.java:392-394` 1:1) — both the packet
handler above and any already-client-side caller (e.g. a future client-only hit-detection path,
mirroring CE's `BulletImpact`→`Spark` same-tick re-dispatch) go through this one static call.

**3. The rendering substrate for handler bodies — copy neo-edition's `ParticleEngineNT` pattern (custom
lightweight particle list + vanilla `RenderType`/`VertexConsumer` batching), confirmed real, for the
handful of high-volume/custom-blend effects; use plain registered vanilla `Particle`+`ParticleProvider`
for everything else.** This maps CE's three render technologies onto two modern ones instead of three:
  - Simple one-off particles (the CE "plain vanilla `Particle` spawn" bucket, ~50+ effects): standard
    `net.minecraft.client.particle.Particle` subclass + `ParticleProvider<T>` +
    `SimpleParticleType`/custom `ParticleType<T>` registered via `DeferredRegister<ParticleType<?>>`
    (confirmed real shape, neo-edition's `NtmParticleTypes.java`, 80 lines) and
    `RegisterParticleProvidersEvent.registerSpecial(type, provider)` /
    `.registerSpriteSet(type, factory)` (confirmed real call sites, both methods used live in
    `NuclearTechModClient.java:720-748`).
  - High-volume / custom-blend effects (CE's `ParticleBatchRenderer` bucket **and** its
    `particle_instanced` bucket collapse into the same modern mechanism — there is no reason to keep them
    separate once GPU instancing is gone): a `ParticleNT`-shaped base class (custom physics/AABB, not
    extending vanilla `Particle`) added to a singleton engine (`ParticleEngineNT`-equivalent) that ticks
    every client tick and renders once per frame via `RenderLevelStageEvent(Stage.AFTER_WEATHER)` →
    `Minecraft.getInstance().renderBuffers().bufferSource()` → per-particle `RenderType` grouping →
    `buffer.endBatch()`. A particle can still opt out of the shared buffer and do fully-manual
    `RenderSystem`/`Tesselator`/`BufferUploader.drawWithShader` immediate-mode rendering by returning
    `null` from `getRenderType()` (confirmed real, exact pattern in neo-edition's own
    `RadiationFogParticle.java:58-99,101` — this is how CE's non-standard blend states, like the fog
    puff's `blendFuncSeparate`, survive without a `RenderType.create(...)` builder call). **Bridge
    pattern** (also confirmed real, same file, lines 103-110): register the effect as an ordinary vanilla
    `ParticleType`/`ParticleProvider` so it still gets a `ClientboundLevelParticlesPacket` for free if a
    generic caller wants one, but have `createParticle()` return `null` and instead
    `ParticleEngineNT.INSTANCE.add(new CustomParticleNT(...))` — i.e. vanilla's own particle network path
    can be reused as *free transport* for effects that don't need custom per-call NBT, reserving the new
    `HbmEffectPacket` for effects that do (variable particle count, motion vectors, colors, etc., the way
    CE's own handlers read arbitrary NBT keys per effect).
  - **Redefine, don't remove, `GeneralConfig.INSTANCED_PARTICLES`**: since neither modern path needs GPU
    capability probing, there is no hardware fallback to gate. Recommend repurposing the existing boolean
    as a density/quality knob (e.g. halve per-tick particle-burst counts, or cap the custom-engine list
    size below the CE-derived 16384 ceiling, when off) rather than a code-path fork — this satisfies
    "keep the CE config key working" without requiring two parallel render implementations, and directly
    answers the open question `GeneralConfig.java`'s own javadoc left for this research area.

## This port's own forward-reference comments (full survey — ~30 call sites, not "8+")

The task named this "the single most cross-referenced missing dependency across Phases 3-4" and asked for
"8+" call sites; the real count from a full-tree grep of `AuxParticlePacketNT`/`HbmEffectNT` is **36
files, ~30 distinct blocked call sites** (some files reference it only in javadoc cross-links, not a
blocked call). Grouped by what unblocking them actually requires:

**Already resolved differently (no action needed from this area)**:
- `ItemChainsaw.java:17-23`, `ItemSwordCutter.java:31`, `ItemGrenadeUniversal.java:43`,
  `ToolAnimationType.java:7-12` — all reference `HbmEffectNT.Anim`, which this port has already,
  correctly, replaced with `ToolAnimationType`/`GunAnimationPayload`. Not blocked on this area.
- `ItemBoltgun.java:41-44` — same `HbmEffectNT`-not-built framing, but the actual gameplay (kill
  detection, advancement) is unaffected; only the hit-spark particle burst is deferred here.

**Single named effect, straightforward once the packet+enum exist** (file:line — CE effect —
context):
| Call site | CE effect(s) | Notes |
|---|---|---|
| `RadFogPayload.java` | `RadFog` | **Already shipped** as its own narrow payload (see its own javadoc) — a template for this area, and a candidate to fold into the generic packet later, non-urgent. |
| `EntityNukeExplosionMK3.java:249,313` | `PlasmaBlast` (color r/g/b, scale 7.5) | |
| `EntityDeathBlast.java:36` | `Muke` | |
| `EntityMist.java:189` | `Tower` | 2/tick while active |
| `LegacyMobBulletConfigs.java:294` | `PlasmaBlast` | 3-shot burst |
| `EntityQuackos.java:121` | `BF` | 150-particle burst on despawn |
| `EntityCreeperNuclear.java:75` | `Muke` | CE: `EntityCreeperNuclear.java:117`, range 250 |
| `EntityTaintCrab.java:98` | `Vanilla` | CE: `EntityTaintCrab.java:79`, range 50 — generic vanilla-particle wildcard, not a custom sprite |
| `HbmLivingProps.java:131` | `Sweat` | CE: `HbmLivingProps.java:128`, range 50, pos `(0,0,0)` local-offset (bug-for-bug: CE passes literal 0,0,0 as the packet's own x/y/z and puts real pos in NBT — verify before copying, may be an NBT-relative-position convention worth preserving as-is) |
| `ArmorDiesel.java:69` | `bnuuy` | every 3 ticks while worn |
| `ArmorBJJetpack.java:52` | `Jetpack_BJ` | while thrusting |
| `ArmorDNT.java:75` | `Jetpack_DNS` | while jetting/gliding |
| `JetpackBooster.java:53` | `Jetpack` (mode 1) | while thrusting |
| `JetpackBreak.java:49` | `Jetpack` | while thrusting/gliding |
| `JetpackRegular.java:45` | `Jetpack` | while thrusting |
| `JetpackVectorized.java:51` | `Jetpack` (mode 1) | while thrusting |
| `TurretSentryBlockEntity.java:139` | `VanillaExt_LargeExplode` | muzzle flash |
| `ItemCrucible.java:117` | `VanillaBurst_BlockDust` | on-kill-while-charged burst |
| `XFactoryFolly.java:45` | (custom "growing plasma sphere" tracer — check CE `XFactoryFolly`/Sedna equivalent, not confirmed which `HbmEffectNT` value it maps to) | along beam path |
| `XFactoryEnergy.java:219` | `PlasmaBlast` | + ufoBlast/firework sound pair |
| `XFactoryEnergy.java:322` | `Muke` | |
| `LaunchPadBlockEntity.java:18` | `LaunchSmoke` | on missile-detected-above |
| `LaunchPadLargeBlockEntity.java:29` | `Tower`, `LaunchSmoke` | |
| `IWeaponAbility.java:40` | `VanillaBurst_BlockDust` | |
| `MachinePWRControllerBlock.java:52` | `Marker` | **single-player send**, not radius broadcast (CE: `createSendToThreadedPacket`, not `createAllAroundThreadedPacket`) |
| `ExplosionEffectTiny.java:31`, `ExplosionEffectAmat.java:23` | `VanillaExt_LargeExplode`, `AmatExplosion` | |
| `ExplosionLarge.java:53,57,61,69` | `Smoke_Radial`, `Smoke_FoamSplash`, `Smoke_Cloud`, `Smoke_Shock` | 4 separate helper methods, each a single effect, range 250 in CE |
| `ExplosionNukeSmall.java:32` | `Muke`/`TinyTot` (data-driven via `MukeParams.particle`) | |
| `GrenadeFillingActions.java:123` | `Haze` ×3 | |
| `GrenadeFillingActions.java:169` | `PlasmaBlast` ×3 | |
| `GrenadeFillingActions.java:246` | `Muke` | mushroom-cloud |
| `TurretBaseBlockEntity.java:72,808` | (casing-eject/muzzle-flash VFX substrate, not one `HbmEffectNT` value) | shared gun-VFX plumbing every hand-held gun also needs — bigger than a single effect, see Open questions |

**Needs a real gameplay entity first, not just VFX** (see Headline finding 6):
- `ExplosionChaos.java:363,368` (`spawnChlorine`/`spawnVolley`) — blocked on `EntityModFXShadow`
  (real server entity), not (only) on this area's packet.

## Safe to build now (no external blocker)

Everything in this document is buildable today with zero cross-phase dependency:
1. `HbmEffect` enum + `HbmEffectPacket` `CustomPacketPayload` + `HbmNetwork` registration — pure
   client/network plumbing, no content dependency.
2. `ServerProxy`/`ClientProxy` `effectNT(...)` method pair.
3. `ClientModRegistry.onClientSetup` wiring for `HbmEffect.registerHandlers()`.
4. The `ParticleNT`/`ParticleEngineNT`-equivalent custom engine (tick + render-batch-by-`RenderType`),
   independent of which specific effects use it yet.
5. Redefining `GeneralConfig.INSTANCED_PARTICLES` as a density knob (no `GLCompat`/capability-probe code
   needed — confirmed dead weight, see Finding 2).
6. Any of the ~29 single-named-effect call sites in the table above, once 1-4 exist — each is a small,
   independent, mechanically-identical change (replace a `// TODO` comment with
   `HbmEffect.sendPacket(level, HbmEffect.X, x, y, z, radius, data)` at the existing call site) with no
   further research needed per-site; the actual particle *visuals* for each named effect still need real
   texture/sprite assets pulled from CE's `assets/hbm/textures/particle*` (out of this report's scope —
   asset inventory, not engine design).

## Blocked / deferred (named blocker, not guessed)

- `ExplosionChaos.spawnChlorine`/`spawnVolley` — blocked on `EntityModFXShadow` (a real physics-simulated
  gas/chemical-weapon entity with terrain-mutation side effects), a Phase 3/4-shaped gameplay task, not
  this area's job. Owner: whoever picks up remaining weapons/explosion gaps (Phase 6 QA sweep, most
  likely, since Phase 3/4 are closed).
- `TurretBaseBlockEntity.spawnCasing()` / the broader casing-ejection + muzzle-flash substrate
  (`TurretBaseBlockEntity.java:72,808`) — this needs a shared "spent casing" particle/entity system (CE:
  `com.hbm.particle.SpentCasing`/`CasingEjector`) used by every hand-held gun, not just turrets; this
  report covers the *generic effect broadcast* half (the `CasingNT`/`CasingOld` `HbmEffectNT` values
  route through the same packet design above) but the casing-physics/ejection-timing half is shared
  gun-VFX substrate that arguably belongs with whichever area owns gun rendering
  (`docs/phase5/weapon_gun_rendering_animloader.md`, already landed as a sibling report — cross-check it
  before implementing casings, to avoid the two areas building incompatible casing systems).
- Exact per-effect particle sprite/texture assets — this report designs the *dispatch mechanism*; the
  ~30 catalog entries above (plus however many of the remaining 87−30≈57 CE effects turn out to have
  live callers once asset/texture work starts) each still need their real CE texture path identified
  under `assets/hbm/textures/particle*` and their handler body's exact motion/color/lifetime numbers
  transcribed from `HbmEffectNT.java` — mechanical but real work, not done here (would have meant
  transcribing most of the 1503-line CE file into this report).

## Key risks

1. **Raw-GL-mimicry temptation**: because CE's instanced system is real hardware instancing, a future
   implementer who reads only CE (not neo-edition) could be tempted to hand-roll VAOs/shaders again in
   1.21.1. This is unnecessary (Finding 1) and actively risky: 1.21.1's `RenderSystem`/`GpuBuffer`
   internal state assumptions are not documented as safe to interleave with raw immediate GL calls, and
   the whole reason neo-edition dropped it is very likely Sodium compatibility (inferred from the
   `sodium_version` compile-only dependency declared in `build.gradle:128` — **not confirmed by any code
   comment explaining why**, flagged here as inference, not fact). Sodium replaces large parts of the
   vanilla chunk/entity rendering backend and is well known in the wider modding community for breaking
   mods that poke raw GL/VBO state outside vanilla's own abstractions; a `RenderType`/`VertexConsumer`
   based approach is far more likely to coexist with it. This port has no explicit Sodium-support goal
   documented anywhere yet — worth confirming with the user/spec owner whether that matters here too.
2. **Enum-over-the-wire codec — not a risk, a confirmed already-real precedent to copy**:
   `PacketBuffer.writeEnumValue`/`readEnumValue` (CE's Forge API) has no direct 1:1 name in NeoForge's
   `StreamCodec`, but this port has already solved exactly this problem for a different enum:
   `src/main/java/com/hbm/packet/toserver/KeybindPacket.java:41-48` encodes `HbmKeybinds.EnumKeybind` as
   `ByteBufCodecs.VAR_INT.map(ordinal -> EnumUtil.grabEnumSafely(EnumKeybind.VALUES, ordinal),
   EnumKeybind::ordinal)` inside a `StreamCodec.composite(...)` — a var-int ordinal plus a
   bounds-checked reverse lookup (`EnumUtil.grabEnumSafely`, guards against a stale/out-of-range ordinal
   from a mismatched client/server jar) rather than a raw unchecked `values()[ordinal]`. The new
   `HbmEffect` enum's codec should copy this pattern verbatim rather than invent a new one — no design
   risk here once you know to look at `KeybindPacket` first instead of only the `toclient` packages
   (`ExplosionEffectSyncPacket`/`RadFogPayload`/`GunAnimationPayload`/`SatPanelPayload`, none of which
   carry a bare enum field, which is where an earlier pass of this report incorrectly looked).
3. **87 vs. ~30**: building the full CE catalog (87 values with real handler-body parity) is much bigger
   than unblocking this port's current call sites (~30, several sharing values like `Jetpack`/`PlasmaBlast`
   /`Muke`). Recommend building only the enum values with a real caller in this port first (the table
   above), adding more on demand, rather than transcribing all 87 up front — CE itself accumulated this
   enum incrementally across years of content additions, and several values (`DebugDrone`, `Network`,
   `UFO`, `JustTilt`, `ProperJolt`, `Foundry`, `Fireworks`, `Splash`, `FluidFill`, `RadiationFlash`, the
   12 `VanillaBurst_*`/`VanillaExt_*` values beyond the two call sites found, `SchrabFog`, `Tau`,
   `Giblets`, and more) have **zero confirmed caller anywhere in this port's current source** — building
   them speculatively risks guessing at behavior nobody will exercise yet.
4. **Unverified against a real client**: every claim in this report about `RenderLevelStageEvent`,
   `MultiBufferSource`/`VertexConsumer` batching, `RegisterParticleProvidersEvent.registerSpecial`/
   `.registerSpriteSet`, and `PacketDistributor.sendToPlayersNear`/`.sendToPlayer` is cross-checked
   against neo-edition's own compiling source at the same `neo_version=21.1.228`/`minecraft_version
   =1.21.1` this port targets, and (for the packet-distribution calls) additionally against this port's
   own already-committed, presumably-compiling code — but this sandbox cannot run `./gradlew` (network
   policy blocks `maven.neoforged.net`) or launch a client, so none of it has been visually confirmed to
   actually render correctly, only to compile-shape-match.

## Open questions

1. Does this port have (or want) an explicit Sodium-compatibility goal? If yes, the
   `RenderType`/`VertexConsumer`-batched engine design here is the safer choice and should be treated as
   a hard requirement, not a preference. If no, it's still the recommended default (simpler, less code,
   confirmed real precedent) but the raw-GL route becomes merely "not recommended" rather than "avoid."
2. Who owns porting `EntityModFXShadow`? It blocks `ExplosionChaos.spawnChlorine`/`spawnVolley` and is a
   real gameplay entity, not VFX — needs a home in whichever pass does Phase 3/4 backfill or Phase 6 QA.
3. Should `RadFogPayload` (already shipped as its own narrow payload) be migrated onto the new generic
   `HbmEffectPacket` once it exists, or left standalone? Non-urgent either way (its own javadoc already
   documents the intended future consolidation), but worth a decision so the codebase doesn't end up with
   two parallel "spawn a named particle at a point" payloads long-term.
4. Exact `HbmEffectNT` value for `XFactoryFolly`'s "growing plasma sphere" tracer effect was not
   confirmed against CE's Sedna-package source in this pass (Sedna's `com.hbm.items.weapon.sedna.factory`
   tree is large and wasn't fully read here) — flagged rather than guessed.
5. `HbmLivingProps`'s `Sweat` call passes CE `AuxParticlePacketNT(HbmEffectNT.Sweat, data, 0, 0, 0)` —
   literal zero position with real coordinates presumably inside `data`'s NBT (worth re-reading
   `HbmLivingProps.java:128` and the `Sweat` handler body together before porting, to confirm this isn't
   simply a CE bug being preserved by accident).
