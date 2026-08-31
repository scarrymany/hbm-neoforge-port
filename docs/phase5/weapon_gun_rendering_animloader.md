# Gun rendering & animation system (Phase 5) — first/third-person posing, BusAnimation sampling, muzzle flash/shell eject

## Sources read

**CE (`upstream/hbm-ce`), in full unless noted:**
- `com/hbm/animloader/{AnimatedModel,Animation,AnimationController,AnimationWrapper,Transform,ColladaLoader}.java`
  (6 files, 822 lines) — re-confirms Phase 3's finding: this is a Collada/GL-display-list skeletal
  system used only by `RenderDoorGeneric`/`RenderSiloHatch`/`RenderSlidingBlastDoorLegacy`/
  `ItemRenderJetpackGlider`/`WorldSpaceFPRender` (re-grepped, same 8 usage sites Phase 3 found). **Not
  a gun-rendering dependency at all** — see Headline finding #1.
- `com/hbm/render/anim/{BusAnimation,BusAnimationSequence,BusAnimationKeyframe,HbmAnimations}.java`
  (4 files, 91+169+566+126 = 952 lines) — the legacy (non-Sedna) keyframe animation system.
- `com/hbm/render/anim/sedna/{AnimationEnums,AnimationLoader,HbmAnimationsSedna,
  BusAnimationSequenceSedna}.java` (4 files, 32+209+130+169 = 540 lines) read in full.
  `BusAnimationSedna.java` (100 lines) and `BusAnimationKeyframeSedna.java` (606 lines) **not** read
  line-by-line — inferred structurally identical to the legacy `BusAnimation`/`BusAnimationKeyframe`
  (which were read in full) from `BusAnimationSequenceSedna`'s near-verbatim duplication of
  `BusAnimationSequence`'s code and `AnimationLoader`'s field-for-field parallel parsing of both. Flag
  this inference explicitly rather than presenting it as a verified read.
- `com/hbm/render/item/TEISRBase.java` (134 lines) and
  `com/hbm/render/item/weapon/sedna/ItemRenderWeaponBase.java` (531 lines) — the 1.12.2 base class
  every Sedna gun's renderer extends.
- `com/hbm/render/item/weapon/sedna/ItemRenderUzi.java` (186 lines) — a worked concrete-gun example.
- `com/hbm/render/item/weapon/ItemRenderGunAnim.java` (96 lines) — the `GunB92`/`GunB93` NBT-counter
  alternative render path (no animation system at all).
- `com/hbm/packet/toclient/GunAnimationPacketSedna.java` (124 lines) — the real network trigger.
- `com/hbm/items/weapon/sedna/GunConfig.java` (168 lines, CE) — the config slots this port's own
  `GunConfig.java` currently omits.
- Targeted greps: `AnimatedModel|AnimationController|ColladaLoader` repo-wide;
  `HbmAnimationsSedna.hotbar\[`; `AnimationLoader.load`; `\.anim\(` under `items/weapon/sedna`; the
  `render/item/weapon/**` and `render/item/weapon/sedna/**` directory listings (65 + item-render
  classes total, confirmed by `find`/`wc -l`).
- `com/hbm/main/ModEventHandlerClient.java` lines 830-900 (per-tick animation-array expiry) and
  `com/hbm/particle/helper/HbmEffectNT.java` lines 1360-1440 (melee-swing `BusAnimationSedna`
  construction inline) — line-range reads, not full-file.

**This port's own already-committed code (`src/main/java`), all in full:**
- `com/hbm/packet/toclient/GunAnimationPayload.java` (129 lines) — the Phase-3-stubbed S2C payload
  this report's job is to fill in the client handler for.
- `com/hbm/weapon/anim/{GunAnimationType,HbmAnimationType,ToolAnimationType}.java` (48+32+19 lines).
- `com/hbm/items/weapon/sedna/{ItemGunBaseNT,GunConfig,GunStateComponent,GunDataComponents}.java`
  (487+180+97+120 lines) — confirms the gun state machine **already calls into this report's exact
  scope** (`playAnimation`) at every CE trigger point; see Headline finding #2.
- Grep: `playAnimation\(` (21 call sites across `ItemGunBaseNT`, `Lego.java`, `GunStateDecider.java`,
  6 `XFactory*.java` content files, `WeaponModCarbineBayonet.java`) — confirms the server-side trigger
  chain is already fully wired for every CE animation type this report's vocabulary defines.
- `docs/phase3/weapon_animation_hooks.md` (328 lines, full) and `docs/phase3/gun_framework.md` lines
  295-330 (its own explicit Phase-5 handoff note) — the direct predecessor reports this one continues.

**Neo Edition (`upstream/neo-edition`), in full unless noted — confirmed real, apparently-compiling
NeoForge 1.21.1 code, used strictly for API shape per the task's ground rules, never for behavior:**
- `com/hbm/render/item/weapon/sedna/ItemRenderWeaponBase.java` (309 lines) — a real 1.21.1 port of the
  *exact* CE class above.
- `com/hbm/render/item/weapon/sedna/ItemRenderSPAS12.java` (140 lines) — a worked concrete gun.
- `com/hbm/render/blockentity/IBEWLRProvider.java` (18 lines).
- `com/hbm/util/mixins/{ItemInHandRendererMixin,GameRendererMixin}.java` (25+26 lines) — see Key
  finding on hand-render suppression.
- `com/hbm/items/weapon/sedna/factory/{GunFactoryClient,LegoClient}.java` (91+160 lines).
- `com/hbm/render/anim/{HbmAnimations,AnimationEnums,AnimationLoader}.java` (115+27+197 lines) and
  `com/hbm/network/toclient/HbmAnimation.java` (97 lines, the network payload).
- `com/hbm/items/weapon/sedna/GunConfig.java` (165 lines) — has the `animations_DNA`/`getAnims`/
  `anim(...)` and `hudComponents_DNA`/`getHUDComponents`/`hud(...)` slots this port's own `GunConfig`
  deliberately left out.
- `com/hbm/render/loader/HFRWavefrontObjectVBO.java` (154 lines, full) and `ObjRenderer.java` (first
  ~40 of 86 lines) — model-geometry loading, read only far enough to confirm the `renderPart(String)`
  contract survives into 1.21.1; full OBJ-pipeline research is explicitly out of this report's scope
  (see Adjacent scope below).
- Greps: `flashMap` (7 hits — `MuzzleFlashPacket.java`, `NuclearTechModClient.java:386`,
  4 `ItemRender*` classes, the field declaration) and `\.anim\(|AnimationLoader` under
  `items/weapon/sedna/factory`. `MuzzleFlashPacket.java` and `NuclearTechModClient.java` were **not**
  read in full, only the grepped lines.
- Build config diff: `grep -in mixin` on both repos' `build.gradle` and
  `META-INF/neoforge.mods.toml` / `hbmsntm.mixins.json` — confirmed Neo Edition has Mixin
  infrastructure wired (`[[mixins]] config="hbmsntm.mixins.json"`); this port has **none** (no
  `.mixins.json` anywhere under `src/main/resources`, no `[[mixins]]` block). See Key risks.

**File count this report is based on:** 34 files read in full, 4 read partially/by line-range or
grep-only (all named above), plus the 328-line predecessor report.

## Headline findings

1. **CE's Collada `animloader` package is still not a gun-rendering dependency** — Phase 3's finding
   holds after a full re-read. Every gun renderer (`ItemRenderWeaponBase` and its ~54 concrete
   subclasses, per `docs/phase3/gun_framework.md`'s own count) is driven entirely by the *other*
   system: `com.hbm.render.anim.sedna.{BusAnimationSedna,BusAnimationSequenceSedna,
   BusAnimationKeyframeSedna}` — pure `double[]`-transform keyframe/easing math, zero GL. Do not build
   or port any part of `animloader` for guns; it belongs to whichever Phase 5 package handles doors/
   silo hatches/jetpack gliders.

2. **This port's Phase 3/4 work already crossed the exact boundary this report was asked to research,
   and it did so with a genuinely different architecture than CE's.** `ItemGunBaseNT.playAnimation()`
   (already committed, called from 21 real sites across the gun framework — fire cycles, reloads, jams,
   inspect, equip, dry-fire) already sends `GunAnimationPayload` **and** stamps a `lastAnim`/`animTimer`
   pair into `GunStateComponent`, a genuine **per-`ItemStack` Data Component** (`GunDataComponents
   .GUN_STATES`, a `List<GunStateComponent>` indexed by gun-config-index). CE (and Neo Edition, which
   ported CE's design unchanged) instead keep animation state in a **client-only static array**
   (`HbmAnimationsSedna.hotbar[9 hotbar slots][8 parallel rails]`, matched back to the held stack by
   comparing `stack.getItem().getDescriptionId()` string identity) that a lightweight enum-ordinal
   packet mutates, with **no per-stack state at all** — the animation "belongs" to the hotbar slot, not
   the item instance. These are two different, incompatible designs for the same problem, and Phase 5
   must pick one *consciously*, not by accident. See "Two competing designs" below — this report's
   recommendation is to keep this port's Data-Component design (it is strictly more correct: no
   slot-swap edge cases, no client-only ghost state that desyncs on late join/relog) but **not** to
   trust the synced `animTimer` field as the moment-to-moment interpolation clock, for a concrete,
   named reason (see Key risks #1).

3. **The task's own framing of "ammo-count/reload-state visual switches" as an
   `ItemProperties`/`IItemPropertyGetter`-equivalent predicate-model-swap mechanism is wrong for these
   guns, confirmed by reading both CE's and Neo Edition's actual renderers.** There is no item-model
   predicate system involved anywhere in gun visual state, in either 1.12.2 or the 1.21.1 port. Every
   "ammo-count"/"reload-state" visual (a visible chambered round, a slide position, a magazine being
   present or not) is a plain `if` in the gun's own `renderFirstPerson`/`renderStatic` Java method,
   reading `BusAnimation` transform-array values or `GunConfig`/`Receiver`/`IMagazine` state directly,
   e.g. CE's `if(bullet[0] == 1) ResourceManager.uzi.renderPart("Bullet");` (`ItemRenderUzi.java:93`)
   and Neo Edition's `if(bullets[0] == 0) ResourceManager.hangman.renderPart("Bullets");`
   (`ItemRenderHangman.java:107`) — a boolean is literally encoded as a `0`/`1` value on one channel of
   the same 15-slot transform array everything else reads, sampled every frame, not switched via a
   model-file predicate at load/tick time. **1.21.1 does have a real `IItemPropertyGetter` successor**
   (item model `"minecraft:condition"`/`"minecraft:select"` special model types operating over Data
   Components) for genuinely swapping *baked models* — but CE's guns don't use baked models at all
   (they use a fully custom `BlockEntityWithoutLevelRenderer`), so that mechanism is irrelevant here.
   Whoever else on this project *is* researching baked-item-model predicate swaps (bow pull states,
   compass needles, etc.) should not assume guns are an example of that pattern.

4. **Neo Edition supplies real, load-bearing confirmation of every 1.21.1 API shape this report needs**,
   not just a sketch — it is a substantive, apparently-compiling parallel implementation of this exact
   subsystem (`ItemRenderWeaponBase`, `HbmAnimations`/`BusAnimation`/`AnimationLoader`, the
   `HbmAnimation` network payload, `GunFactoryClient`'s `IClientItemExtensions` registration, and 4
   concrete gun renderers). Per the task's ground rules it is used here strictly as an API-shape
   reference, never as a behavior/design source — CE remains authoritative for what each gun should
   look like and when each animation fires — but where CE's 1.12.2 code doesn't tell you *how* to say
   the same thing in 1.21.1 (custom render-type construction, `IClientItemExtensions` registration,
   `VertexConsumer` vertex emission, the two mixins needed for correct hand suppression), Neo Edition's
   code is a real answer, not a guess, and is cited as such throughout.

## The two competing designs for "where does animation state live"

| | CE / Neo Edition | This port (already committed) |
|---|---|---|
| State location | Client-only static array `HbmAnimationsSedna.hotbar[slot][rail]` (Neo Edition: `HbmAnimations.hotbar[9][8]`, same shape) | Per-`ItemStack` Data Component (`GunDataComponents.GUN_STATES`, a `List<GunStateComponent>`) |
| Identity check | String-compares the held stack's `getDescriptionId()`/translation key against the slot's remembered key every read (`getRelevantAnim`) — a genuine CE quirk: swap to a *different instance of the same gun type* mid-animation and the animation is kept (CE's own code comment: "you can still 'trick' the system") | None needed — the state is *on* the exact stack instance being rendered |
| Timing | `startMillis = System.currentTimeMillis()` set once when the packet arrives; elapsed time computed client-side every frame, needs no further sync during playback | `animTimer` is an `int` **incremented server-side once per server tick** (`ItemGunBaseNT.inventoryTick`, only while the gun is the selected hotbar item) and synced as part of the Data Component |
| Network cost per shot/reload | One `{short type, int rec, int gun}` packet (9 bytes + header) sent once per animation start | Same `GunAnimationPayload` sent once per trigger, **plus** — see Key risks #1 — a component write every tick while any gun is selected |
| Multi-weapon-slot bleed | Possible (documented CE bug, "trick the system") | Impossible — state is stack-bound |
| Late-join / relog | Animation state doesn't exist for a just-connected client — cosmetic-only, no correctness impact in CE either | Correctly reconstructable from the synced Data Component (no worse, and arguably better for spectator/replay-style consumers) |

Recommendation: **keep this port's per-stack Data Component as the source of truth for *what* is
playing (`lastAnim`) and *bookkeeping-durable state* (persisted across reload/relog), but drive the
actual per-frame render-time clock the same way CE and Neo Edition do** — a client-local
`System.currentTimeMillis()` (or this port's own millisecond clock helper, if one exists — Neo Edition
uses a custom `com.hbm.util.Clock.get_ms()`, not read in this report) captured **once, when
`GunAnimationPayload` arrives client-side**, not derived from re-reading `animTimer` every frame. This
sidesteps the network-cost risk below entirely while keeping every other benefit of the Data-Component
design. The client-side handler this report unblocks (`GunAnimationPayload.handleClient`) is exactly
the place to capture that timestamp — mirroring `GunAnimationPacketSedna.Handler.handleSedna`'s
`HbmAnimations.hotbar[slot][gunIndex] = new Animation(key, System.currentTimeMillis(), animation, ...)`
line almost exactly, just keyed by the `ItemStack` identity (or a small per-render-instance client map
keyed by the stack's own `lastAnim`+a change-counter) instead of by hotbar slot.

## Confirmed 1.21.1 API shapes (from Neo Edition's real code)

All of the following are read directly out of compiling Neo Edition source, not invented:

- **Custom item renderer base class**: `BlockEntityWithoutLevelRenderer` (package
  `net.minecraft.client.renderer`) is the 1.21.1 successor to `TileEntityItemStackRenderer`.
  Constructor takes `(BlockEntityRenderDispatcher, EntityModelSet)` — Neo Edition's
  `ItemRenderWeaponBase()` passes `Minecraft.getInstance().getBlockEntityRenderDispatcher()` and
  `Minecraft.getInstance().getEntityModels()`. Override
  `renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
  MultiBufferSource buffer, int packedLight, int packedOverlay)` and `switch` on `displayContext`
  (`FIRST_PERSON_RIGHT_HAND`, `FIRST_PERSON_LEFT_HAND`, `THIRD_PERSON_RIGHT_HAND`,
  `THIRD_PERSON_LEFT_HAND`, `GROUND`, `GUI`, `FIXED`, `HEAD`, `NONE`) — a direct 1:1 replacement for
  CE's `ItemCameraTransforms.TransformType` switch in `ItemRenderWeaponBase.renderByItem` (1.12.2).
- **Registration**: `IClientItemExtensions`, registered via
  `NeoForge.EVENT_BUS`'s `RegisterClientExtensionsEvent` (`event.registerItem(extensions, item)`), not
  a baked-model JSON file. Override:
  - `getCustomRenderer()` → return the `BlockEntityWithoutLevelRenderer` instance (one instance per gun
    type, reused — Neo Edition's `GunFactoryClient.registerGunItemRenderer` lazily assigns it once).
  - `applyForgeHandTransform(PoseStack, LocalPlayer, HumanoidArm, ItemStack, float partialTick, float
    equipProcess, float swingProcess)` → return `true` after doing your own `poseStack` mutation
    (view-bob, sway) to tell NeoForge "I've positioned this, don't apply the default hand transform."
    This is the direct successor to CE's per-item `setupFirstPerson`/view-bob code that used to run
    inline inside `ItemRenderer` itself.
  - `getArmPose(LivingEntity, InteractionHand, ItemStack)` → Neo Edition doesn't actually override the
    pose (falls through to `IClientItemExtensions.super.getArmPose(...)`), it only uses this call as a
    hook to capture *which* `LivingEntity` is holding the item right now (`renderer.setEntity(living)`)
    for third-person-of-other-players rendering. **A gun that wants a custom `HumanoidModel.ArmPose`
    (e.g. `CROSSBOW_HOLD`-style) can return one here** — neither CE nor Neo Edition's 4 ported guns
    need this, since the gun model itself replaces the arm/hand visually, but the hook exists and is
    real; confirm per-weapon in Phase 5 rather than assuming none need it (a bow-like weapon might).
- **Hand-render suppression needs Mixin, not just `IClientItemExtensions`.** This is a genuinely
  non-obvious, load-bearing finding: even with a correct `IClientItemExtensions` + `getCustomRenderer`,
  vanilla's `ItemInHandRenderer.renderHandsWithItems` and `GameRenderer.renderItemInHand` still apply
  their own default swing-rotation/bob math *around* your custom renderer's output unless suppressed.
  Neo Edition ships two `@Mixin` classes specifically for this:
  - `GameRendererMixin` (`@ModifyExpressionValue` on `renderItemInHand`) forces
    `mc.options.getCameraType()`-driven first-person-model visibility off when the main-hand item's
    `getCustomRenderer()` is an `ItemRenderWeaponBase`, preventing vanilla's own held-item render pass
    from double-rendering.
  - `ItemInHandRendererMixin` (`@Redirect` on the `Axis.rotationDegrees` call inside
    `renderHandsWithItems`) replaces vanilla's swing-rotation quaternion with identity
    (`new Quaternionf()`) for the same condition, killing the built-in swing arc so the gun's own
    `BusAnimation`-driven recoil/cycle animation is the only motion visible.
  **This port has zero Mixin infrastructure today** (confirmed: no `.mixins.json` under
  `src/main/resources`, no `[[mixins]]` block in `neoforge.mods.toml`, vs. Neo Edition's
  `hbmsntm.mixins.json` + matching TOML entry) — see Key risks #2.
- **Modern vertex-buffer rendering**: no `GlStateManager`/`Tessellator`/`BufferBuilder.begin(mode,
  format)` immediate-mode calls anywhere in Neo Edition's port. Muzzle flash / gap flash / smoke are
  emitted via a custom `RenderType.create(...)` (built once via `RenderType.CompositeState.builder()`
  with explicit shader/texture/transparency/lightmap/overlay/cull/write-mask state — see
  `ItemRenderWeaponBase.SMOKE`/`FLASH` fields for the exact composite state CE's additive-fullbright
  blend mode maps to) and `MultiBufferSource.getBuffer(renderType)` → `VertexConsumer
  .addVertex(matrix, x, y, z).setColor(...).setUv(...).setLight(packedLight)` chains — a 1:1 structural
  replacement for CE's `BufferBuilder.begin(GL11.GL_QUADS, ...); buf.pos(...).tex(...).endVertex();
  Tessellator.getInstance().draw();`, quad-for-quad (compare CE's `renderMuzzleFlash`
  (`ItemRenderWeaponBase.java:360-398`) against Neo Edition's (`ItemRenderWeaponBase.java:271-308`) —
  same 16 vertices, same UV layout, same `fire`/`width`/`length`/`inset` math, only the emission API
  changed). CE's `beginFullbrightAdditive`/`endFullbrightAdditive` state-push/pop dance (this port's own
  `com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase.java` — wait, **this class does not exist yet
  in this port**, only in CE and Neo Edition — see Phase-5-safe scope) is entirely subsumed by the
  `RenderType`'s declarative composite state in 1.21.1; no manual GL attrib push/pop is needed at all.
- **The per-gun animation registration and sampling pattern**, confirmed end-to-end in Neo Edition:
  1. `ResourceManager` (a static holder, client-init-time) loads the OBJ model
     (`HFRWavefrontObject("...obj").asVBO()`, stored as `IModelCustom spas_12`) and the animation JSON
     (`AnimationLoader.load(ResourceLocation)`, stored as `HashMap<String, BusAnimation> spas_12_anim`)
     as static fields, exactly mirroring CE's own `ResourceManager.spas_12_anim` field.
  2. `GunConfig.anim(BiFunction<ItemStack, GunAnimation, BusAnimation> lambda)` registers a per-gun
     switch, e.g. `case CYCLE -> ResourceManager.spas_12_anim.get("Fire");` — a plain
     `HashMap` lookup by Blender-action-name string, one lambda per gun, assigned from
     `XFactory12ga.java`'s item-registration block. **This exact field/getter/setter (`animations_DNA`/
     `getAnims`/`anim(...)`) is what this port's own `GunConfig.java` explicitly, deliberately omitted**
     (see its own class javadoc: `"Not ported from CE's own field set ... needs
     com.hbm.render.anim.sedna.BusAnimationSedna, unported Phase 5 keyframe-animation data"`) — Neo
     Edition's version (using the plain, non-`Sedna`-suffixed `BusAnimation`, confirming both repos
     converged on dropping the `Sedna`-namespaced class names once there was only one animation
     system left to port) is the exact target shape to add back.
  3. `GunFactoryClient.init(RegisterClientExtensionsEvent)` wires the renderer instance +
     `.hud(...)` components per item, once, at client-setup time.
  4. Per-frame, the concrete renderer (`ItemRenderSPAS12.renderFirstPerson`) calls
     `HbmAnimations.getRelevantTransformation("EQUIP")` (a raw `float[15]` for one-off reads, e.g. a
     single-axis rotation) or `HbmAnimations.applyRelevantTransformation("MainBody")` (mutates the
     current `PoseStack` directly — the 1.21.1 replacement for CE's `GlStateManager.translate/rotate/
     scale`) immediately before `ResourceManager.spas_12.renderPart("MainBody")` — i.e. **the named-bus
     ↔ named-model-part correspondence from CE (bus name usually, but not always, equals the model
     group name) survives 1.21.1 completely unchanged**, just phrased in `PoseStack`/`Axis` terms
     instead of `GlStateManager`/raw GL rotation-axis triples.
- **Multiplayer third-person muzzle-flash correctness — a real bug CE has and Neo Edition fixed.** CE's
  `gun.lastShot[gunIndex]` (and this port's own already-committed, field-for-field-identical
  `ItemGunBaseNT.lastShot`) is a **mutable field on the `Item` instance**, not the `ItemStack` — since
  Minecraft registers exactly one `Item` instance per gun type shared by every player and every stack of
  that gun, `lastShot` is *global, shared, mutable client-side state*. In CE this "works" for
  first-person (only the local player's own fire events are ever processed into it) but is a latent bug
  for third-person: if you are standing near another player holding the *same gun type* and they fire,
  then look at a *third* player also holding that gun type, that third player's gun would incorrectly
  flash too, because all three players' renders read the one shared `lastShot[]` array. **Neo Edition
  does not silently inherit this bug — it added a dedicated `MuzzleFlashPacket` (server broadcasts a
  `{livingEntityId, timestamp}`-shaped payload to nearby players whenever anyone fires) that populates
  a bounded `HashMap<LivingEntity, Long> ItemRenderWeaponBase.flashMap` (evicted every client tick past
  150ms, see `NuclearTechModClient.java:386`), and every renderer branches: `living ==
  Minecraft.getInstance().player` → cosmetic per-Item `lastShot[]`/`shotRand` (fine, only one local
  player exists); anyone else → `flashMap.getOrDefault(living, -1)` (correct per-entity lookup). This
  is a genuine, confirmed design correction from a real sibling implementation, not a guess — Phase 5
  should adopt the same `flashMap`+dedicated-broadcast-packet pattern rather than faithfully porting
  CE's shared-state bug, while still keeping the per-`Item`-instance `lastShot[]`/`shotRand` fields for
  the local-player fast path (this port's `ItemGunBaseNT.java` already declares both fields, unused —
  see Phase-5-safe scope).

## What CE actually renders for "reload/ammo visual state" (confirms Headline finding #3)

Concretely, from `ItemRenderUzi.java` (CE) and `ItemRenderHangman.java`/`ItemRenderSPAS12.java` (Neo
Edition), every "does the model show a chambered round / an inserted magazine / a racked slide" check
is one of exactly two things, both plain per-frame Java, never a model-file predicate:
1. **A boolean encoded as a keyframe-channel value**, sampled through the *same* `BusAnimation` call
   used for every other transform on that gun — e.g. `bullet[0] == 1` reading the `TX` channel of a
   bus literally named `"Bullet"`/`"RELOAD_BULLETS_CON"` that the Blender rig animator set to hold at
   `0` or `1` for the relevant frame ranges (`IType.CONSTANT` keyframes with no interpolation — see
   `BusAnimationKeyframe`'s `CONSTANT` case, `"Teleport"`). This is why `AnimationLoader`'s JSON format
   supports a `CONSTANT` interpolation type distinct from `LINEAR`: it's how the rig encodes "instant,
   not gradual" visibility toggles inside a system that otherwise only knows continuous transforms.
2. **Direct gameplay-state reads with no animation involvement at all** — e.g.
   `gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack).getCasing(...)` (spent-casing
   color) or a magazine's own ammo-count getter, called straight from inside the renderer, once per
   frame, no caching, no predicate, no model swap.

Both patterns are equally trivial to port once `GunConfig.getAnims()`/`Receiver.getMagazine()` exist
client-side and the renderer can read them — there is no separate "ammo-count visual switch subsystem"
to design; it is just more `if` statements inside `renderFirstPerson`/`renderStatic`, exactly like every
other per-gun visual decision.

## Shell ejection — adjacent scope, not this report's core (flagged, not fully resolved)

The **on-model** "chambered shell" visibility (SPAS-12's `"Shell"`/`"ShellFore"` parts, tinted per
`SpentCasing.getColors()`) is squarely part of this report's animation/rendering scope and is described
above. The **physically ejected, flying-away casing** is a different, adjacent system this report did
not fully research: `upstream/hbm-ce/src/main/java/com/hbm/particle/SpentCasing.java` is pure data
(casing color/type), and Neo Edition's `com/hbm/particle/SpentCasingParticle.java` is a genuine vanilla
`Particle` subclass (not read in full here) presumably spawned client-side off the same `CYCLE`
animation trigger. Whoever scopes Phase 5's particle/VFX package should treat "spawn a `SpentCasing`-
typed vanilla particle on `GunAnimationPayload`'s `CYCLE` arrival" as the hook point (the payload already
carries everything needed — the receiver index tells you which barrel ejected), but the particle's own
physics/rendering is out of this report's line-by-line verification.

## Model geometry loading — adjacent scope, not this report's core (flagged, not fully resolved)

Every gun renderer calls `ResourceManager.<gun>.renderPart("<PartName>")` against a named-group OBJ
model object. Neo Edition has **two** competing implementations: `HFRWavefrontObjectVBO` (implements
`IModelCustomNamed`, marked `@Deprecated` in Neo Edition's own source, static VBO-per-group with a
hand-written shader-uniform push) and a newer `ObjRenderer` (a `record` keyed by `Material`, routed
through a `MaterialShaderCache`) — the 4 ported gun renderers still use the **deprecated** one
(`ResourceManager.spas_12 = new HFRWavefrontObject(...).asVBO()`), i.e. even Neo Edition itself hasn't
finished migrating gun models to its own newer pipeline. This is a real, separate research area (OBJ
parsing + named-group VBO/material rendering) that this report deliberately did not chase to the bottom
— treat `renderPart(String)` as a **confirmed, stable contract** guns can be written against, but get a
dedicated pass on *how that contract gets implemented* (or reuse whichever Phase 5 package already
owns block-entity OBJ rendering, if one exists — `IBEWLRProvider`/the `render.blockentity` package
uses the same OBJ loaders for machine models and may already be scoped elsewhere).

## Phase-5-safe scope (buildable now, no blockers)

| Item | Basis |
|---|---|
| Port `BusAnimation`/`BusAnimationSequence`/`BusAnimationKeyframe` (CE, legacy-named) as pure Java data classes — no GL, no `Minecraft` reference except the one `AnimationLoader` resource read | Read in full from CE; Neo Edition's byte-for-byte-structurally-identical port (same class names, `double`→kept as `double` per CE, not `float` — Neo Edition's is `float`, either is fine, this port should pick one and be consistent) confirms it compiles and works in 1.21.1 with zero API changes needed beyond the `AnimationLoader`'s resource-manager call |
| Port `AnimationEnums.{GunAnimation,ToolAnimation}` as the CE-shaped vocabulary — **already effectively done** as this port's own `GunAnimationType`/`ToolAnimationType`/`HbmAnimationType` (Phase 3) | Confirmed compatible; no changes needed, just needs a real consumer |
| Add `GunConfig.animations_DNA`/`getAnims(ItemStack)`/`anim(BiFunction<ItemStack, HbmAnimationType, BusAnimation> lambda)` and `hudComponents_DNA`/`getHUDComponents`/`hud(...)` back into this port's `GunConfig.java`, exactly mirroring CE's/Neo Edition's field shape (adjusted to this port's `HbmAnimationType` marker instead of CE's concrete `GunAnimation` enum, so the same slot serves both gun and tool animation vocabularies per Phase 3's own design intent) | Both CE and Neo Edition read in full; this port's own `GunConfig.java` javadoc explicitly names this as the one thing blocking it, and names the exact class (`BusAnimationSedna`/`BusAnimation`) that unblocks it — now portable per the row above |
| `AnimationLoader` (JSON keyframe parser) — port using `Minecraft.getInstance().getResourceManager().getResource(ResourceLocation).open()` (confirmed real 1.21.1 API from Neo Edition, `Optional<Resource>`-returning, not CE's 1.12.2 `IResource`) | Read in full from both CE and Neo Edition; the JSON format itself is unchanged, needs zero redesign |
| Port the 12 animation JSON assets CE ships (`assets/hbm/models/weapons/animations/{python,cursed,novac,ks23,spas12,supershotty,benelli,congolake,am180,flamethrower,stg77,lag}.json`) into this port's asset tree as plain data-file copies (format is engine-agnostic) | Confirmed present in CE (`find` above); confirmed **not yet present** in this port (`find` on `src/main/resources` returns nothing under any "animations" path) and only 1 of 12 (`spas12.json`) copied so far in Neo Edition |
| `ItemRenderWeaponBase` (this port's own, new file) as a `BlockEntityWithoutLevelRenderer` subclass with the `renderByItem`/`ItemDisplayContext` switch, `setupFirstPerson`/`setupThirdPerson`/`setupInv`/`setupEntity` hook methods, and the `renderMuzzleFlash`/`renderGapFlash`/`renderLaserFlash`/`renderSmokeNodes` static helpers, built on custom `RenderType`s | Both CE (531 lines) and Neo Edition (309 lines) read in full — this is close to a mechanical translation once the `RenderType`/`VertexConsumer` pattern above is applied; **zero corresponding file exists in this port yet** (confirmed by `find`) |
| `GunFactoryClient`-equivalent registration (`RegisterClientExtensionsEvent` + `IClientItemExtensions` per gun item, wiring `getCustomRenderer`/`applyForgeHandTransform`/`getArmPose`) | Read in full from Neo Edition; a NeoForge event-registration pattern, no design decision needed |
| Fill in `GunAnimationPayload.handleClient`'s body: capture a client-local start timestamp keyed by the receiving stack/slot (per "Two competing designs" recommendation above), replacing the current debug-log stub | This report's specific unblock target — the payload shape itself (already shipped) needs no changes |
| Add `ItemRenderWeaponBase.flashMap` (`HashMap<LivingEntity, Long>`) + a small dedicated muzzle-flash-broadcast payload for correct third-person-of-other-players rendering, following Neo Edition's confirmed fix rather than CE's confirmed bug | Read in full from Neo Edition; a deliberate, named design choice this report recommends making explicitly (see Key risks — silently porting CE's bug is also a valid, cheaper choice, but should be a decision, not an accident) |

## Deferred / blocked scope (named blocker, owner)

| Item | Blocked on |
|---|---|
| Any concrete gun's `renderFirstPerson`/`renderStatic` body (the ~54-65 `ItemRender*` classes) | The named-part OBJ model loader (see "Model geometry loading" above) — needs its own research/implementation pass; not something this report resolves. Also needs the actual OBJ + texture assets ported from CE (`upstream/hbm-ce/src/main/resources/assets/hbm/models/obj/weapons/*.obj` + `textures/models/weapons/*.png`, not audited by file count in this report) |
| Full hand-render suppression matching CE's visual fidelity (no vanilla swing-arc bleed-through) | **This port has no Mixin infrastructure at all** (confirmed absent: no `.mixins.json`, no `[[mixins]]` TOML entry, vs. Neo Edition's confirmed-present setup). Owner: whoever sets up this port's build tooling — needs a `.mixins.json` + `[[mixins]]` NeoForge TOML block + (if not already a transitive NeoGradle dependency) the Mixin annotation processor. Until this exists, first-person gun rendering will work but will visibly co-exist with vanilla's default item-swing motion, a real (if minor) visual regression from CE |
| `IHUDComponent`/`HUDComponentAmmoCounter`/`HUDComponentDurabilityBar` (the crosshair/ammo-counter HUD `GunConfig.hud(...)` slot feeds) | A dedicated HUD-rendering package — `ItemGunBaseNT.renderHUD` is already a wired-but-empty stub in this port (confirmed, `ItemGunBaseNT.java:482-486`) explicitly deferred to "whichever Phase 5 package renders the item's actual HUD ammo counter." Not this report's package |
| The ejected-shell particle (`SpentCasingParticle`-equivalent) | A Phase 5 particle/VFX package — this report only confirms the hook point (`GunAnimationPayload`'s `CYCLE` arrival), not the particle's own implementation |
| OBJ model + VBO/shader loading pipeline (`HFRWavefrontObjectVBO`/`ObjRenderer`, `Material`/`MaterialShaderCache`, `NtmRenderTypes`) | A dedicated model-geometry-loading research pass — flagged but not resolved here; may already be scoped under whichever package owns `render.blockentity`/machine-model OBJ rendering, worth checking before duplicating |
| Weapon-mod (`XWeaponModManager`) client-side animation overrides (bayonet/sawed-off/speedloader mods that CE lets rebind `GunConfig.FUN_ANIMNATIONS`) | Same blocker as the base `animations_DNA` field — once that field exists (Phase-5-safe row above), the existing `XWeaponModManager.eval` plumbing (already ported, Package C per `gun_framework.md`) picks it up for free; no separate work needed beyond adding the field |

## Key risks

1. **Network-traffic risk in the already-committed Data Component design (well-established Minecraft
   server-architecture knowledge, *not* verified against a running 1.21.1 server in this sandbox — no
   `./gradlew`, no launchable client).** `ItemGunBaseNT.inventoryTick` calls
   `setAnimTimer(stack, i, getAnimTimer(stack, i) + 1)` **every server tick**, unconditionally, for
   whichever gun config is the player's currently-*selected* hotbar item (confirmed: the "RESET WHEN
   NOT EQUIPPED" branch returns early for non-selected slots, so this is bounded to one gun per player,
   but still 20×/second for the entire time any player has a gun as their active item).
   `GunDataComponents.GUN_STATES` is declared `.networkSynchronized(...)`. Standard vanilla container-
   sync behavior (`AbstractContainerMenu.broadcastChanges`, called every server tick per player against
   their own inventory menu, which is always "open") resends a slot's full `ItemStack` payload whenever
   its components differ from the last-broadcast snapshot — and a `List<GunStateComponent>` record
   whose `animTimer` field changed every tick *is* a different value by Java `record` equality every
   single tick. If this holds as I believe (again: not verified against a real running server in this
   sandbox), **every player with a gun selected causes a continuous 20/sec full-`ItemStack` resync
   packet for their held-item slot**, for as long as they hold it — a genuine, avoidable performance
   regression versus CE's own design (one 9-byte packet per animation *start*, zero packets during
   playback). The "Two competing designs" recommendation above (client-local timestamp captured once on
   payload arrival, `animTimer` treated as non-realtime bookkeeping only) sidesteps this without
   requiring any change to the already-shipped Data Component shape — but confirming the actual
   resync behavior (does `Slot`/`broadcastChanges` really compare deeply enough to trigger every tick,
   or is there a coalescing/dirty-flag mechanism that saves this?) should be an explicit verification
   task early in Phase 5 implementation, not an assumption either way.
2. **No Mixin tooling exists in this port today**, confirmed by direct config diff against Neo Edition
   (which needs and has it for the exact same problem). This blocks *full* visual parity for
   first-person hand suppression specifically — the animation/model-part rendering itself has no such
   dependency and can be built and will work without Mixin, just with the vanilla-swing-bleed-through
   caveat noted above. Flagging so it's a conscious "ship slightly-off first-person feel now, fix later"
   decision rather than a silent gap discovered after the fact.
3. **This port's `ItemGunBaseNT.lastShot`/`shotRand` fields already exist (ported from CE field-for-
   field) but are currently write-only-never-written** (grep confirmed: `lastShot[` has zero writes
   anywhere in this port's `src/main/java`) — `GunAnimationPayload.handleClient`'s stub never touches
   them. Filling in the handler body must remember to populate these (or their `flashMap` replacement,
   per the recommended fix) — they are not automatically kept in sync by anything else.
4. **`BusAnimationSedna.java`/`BusAnimationKeyframeSedna.java` (CE) were not read line-by-line** in this
   report (time-budgeted against `BusAnimationSequenceSedna`/`AnimationLoader`, which *were* read in
   full and structurally confirm the same shape) — treat the easing-function math specifically
   (`BLI_easing_*`, ~30 Blender-ported curve functions in the legacy `BusAnimationKeyframe.java`, read
   in full) as verified only for the legacy class; re-diff against `BusAnimationKeyframeSedna.java`
   before assuming the Sedna version has zero differences, however unlikely given `AnimationLoader`'s
   identical parsing of both.
5. **OBJ asset volume not counted.** This report did not enumerate CE's weapon `.obj`/texture files
   under `models/obj/weapons/`+`textures/models/weapons/` — porting ~54-65 guns' worth of geometry and
   textures (plus the 12 animation JSONs, of which only 1 exists in Neo Edition and 0 in this port) is
   a substantial asset-migration task in its own right, separate from the code described here.

## Open questions

- Does this port have (or plan) a millisecond-clock utility equivalent to Neo Edition's
  `com.hbm.util.Clock.get_ms()`, or should the recommended client-local animation-start timestamp just
  use `System.currentTimeMillis()` directly (as CE itself does)? Not resolved here — `Clock.java` was
  not read.
- Should the `flashMap`/dedicated-muzzle-flash-broadcast fix (recommended above) be built now, or is
  faithfully porting CE's shared-`Item`-instance bug an acceptable, cheaper first pass given it's a
  cosmetic-only, third-person-only, same-gun-type-only edge case? This report recommends the fix but
  flags it as a real scope/cost decision for whoever picks up implementation, not a foregone conclusion.
- Where does OBJ model + VBO/shader loading actually get scoped in this project's Phase 5 breakdown —
  is there already a sibling research package for it (e.g. covering `render.blockentity`/machine
  models), or does one need to be created? This report could not determine that from its own scope.
- Confirm (can't be done in this sandbox — no launchable client) whether `IClientItemExtensions
  .applyForgeHandTransform` returning `true` is sufficient on its own for *third-person* hand
  suppression too, or whether the `GameRendererMixin`/`ItemInHandRendererMixin` pair is strictly
  required even for third-person (Neo Edition's own mixin targets — `renderItemInHand`,
  `renderHandsWithItems` — both read as first-person-specific method names, but this was not verified
  against actual third-person render output).
