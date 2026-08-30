# Weapon animation/rendering hooks (animloader, BusAnimation, HbmAnimations) — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/animloader/*.java` (6 files — `Animation`, `AnimationWrapper`,
  `AnimationController`, `AnimatedModel`, `Transform`, `ColladaLoader`)
- `upstream/hbm-ce/src/main/java/com/hbm/render/anim/{BusAnimation,BusAnimationSequence,
  BusAnimationKeyframe,HbmAnimations}.java` (4 files)
- `upstream/hbm-ce/src/main/java/com/hbm/render/anim/sedna/*.java` (6 files — `AnimationEnums`,
  `AnimationLoader`, `BusAnimationSedna`, `BusAnimationSequenceSedna`, `BusAnimationKeyframeSedna`,
  `HbmAnimationsSedna`)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemGunBase.java` (full, 763 lines — the
  legacy/mainline gun framework and the only class that actually calls `startAnim`)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/GunConfiguration.java` (full) and
  `handler/guncfg/Gun12GaugeFactory.java` (full, as a worked example of a real `animations` map)
- `upstream/hbm-ce/src/main/java/com/hbm/packet/toclient/{GunAnimationPacket,
  GunAnimationPacketSedna}.java` (full — the actual network trigger)
- `upstream/hbm-ce/src/main/java/com/hbm/items/{weapon/GunB93,special/weapon/GunB92}.java` (full —
  the NBT-frame-counter alternative to the animloader/BusAnimation systems entirely)
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemChainsaw.java` +
  `items/IAnimatedItem.java` (full — the melee/tool trigger path, named explicitly by
  `docs/phase1/items_tool.md`)
- `upstream/hbm-ce/src/main/java/com/hbm/particle/helper/HbmEffectNT.java` lines ~1290-1430 (the
  `Anim`/generic-effect dispatch table that `ItemChainsaw`/`ItemCrucible` route through)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemCrucible.java` (full — a weapon that reads
  animation *state* back out client-side to drive gameplay-adjacent decisions, not just render)
- Repo-wide greps in `upstream/hbm-ce` for `AnimationController|AnimatedModel|setAnim(|
  AnimationWrapper`, `hotbar\[`, `HbmAnimations\.`, `extends ItemGunBase\b`, `\.animations\.put`
- This port's `src/main/java/com/hbm/packet/{HbmNetwork.java, toclient/BufPacket.java}` (the one real
  `CustomPacketPayload` already registered, used here as the confirmed pattern for the new payload)
  and `docs/phase1/{items_tool.md, items_special.md}` (the two docs that flagged this scope)

## Headline finding

CE actually has **two unrelated animation systems** sharing loose terminology, and only one of them
is a Phase 3 dependency at all:

1. **`com.hbm.animloader`** (6 files) is a Collada (`.dae`)-file skeletal-animation renderer:
   `ColladaLoader` parses XML into an `AnimatedModel` tree of named bones, each bone backed by
   `Transform` (quaternion/scale/translation per keyframe with GL matrix math), driven at render time
   by `AnimationController`/`AnimationWrapper`, and rendered via raw `GlStateManager.callList`
   display lists. Every non-trivial method in every one of these 6 files either touches
   `GlStateManager`/`GL11`/`FloatBuffer` GL matrices directly or exists purely to feed something that
   does. **This is 100% Phase 5 client rendering.** Nothing here is reachable from, or a dependency
   of, server-side gun/item logic — a gun item's `fire()`/`reload()` never touches this package.
2. **`com.hbm.render.anim.{BusAnimation,BusAnimationSequence,BusAnimationKeyframe}`** (+ the parallel
   `.sedna` versions) is a completely different, hand-rolled *keyframe-per-named-channel* system with
   **zero GL/client imports** — pure `double[]` math (interpolation curves, easing functions ported
   from Blender's `easing.c`). This is the system `GunConfiguration.animations` (a
   `Map<AnimType, BusAnimation>`) actually stores, and it is what "reload/recoil/animation via
   animloader" in PORT_SPEC really refers to for guns, even though the class name most people
   associate with "CE's animation system" (`animloader`) is the *other*, unrelated package. **This is
   the one that is Phase-3-safe as data**, and its *trigger* (deciding "reload just started, tell the
   client to start playing the RELOAD animation") is core, server-authoritative gun logic that
   `gun_framework_core` cannot function without.

The boundary the task asked for falls exactly on the render read-back: constructing/holding a
`BusAnimation` and deciding *when* to start one is Phase 3; sampling it every frame and feeding the
result into `GlStateManager`/`PoseStack` transforms on a rendered item model is Phase 5.

## The trigger chain (what actually starts an animation)

Two independent trigger call sites in `ItemGunBase`, both server-side, both firing when
`mainConfig.animations` actually has an entry for that `AnimType` — the gun framework treats "no
animation configured" as "don't bother telling the client":

- **Fire cycle** — `ItemGunBase.spawnProjectile()` (called from `fire()`, called from
  `updateServer()`/`startAction()` on every successful shot):
  ```java
  if (this.mainConfig.animations.containsKey(AnimType.CYCLE) && player instanceof EntityPlayerMP)
      PacketDispatcher.wrapper.sendTo(new GunAnimationPacket(AnimType.CYCLE.ordinal(), hand), (EntityPlayerMP) player);
  ```
- **Reload start** — `ItemGunBase.startReloadAction()` (called once when a reload begins, not every
  tick of the reload):
  ```java
  PacketDispatcher.wrapper.sendTo(new GunAnimationPacket(AnimType.RELOAD.ordinal(), hand), (EntityPlayerMP) player);
  ```
  Reload itself is then driven tick-by-tick by a plain NBT countdown (`reload2()`,
  `getReloadCycle`/`setReloadCycle`, decremented once per tick in `updateServer()`) — the *animation*
  only needs to be told once, at the start; the countdown that gates ammo insertion has nothing to do
  with the animation system at all and would exist even with animations disabled.

`AnimType` (`HbmAnimations.AnimType`: `RELOAD, CYCLE, ALT_CYCLE, SPINUP, SPINDOWN, EQUIP`) is a pure
enum — no rendering dependency — and is exactly the vocabulary `gun_framework_core` needs to expose
as its trigger surface.

The packet (`GunAnimationPacket`, S2C, `{int type, EnumHand hand}`) is handled `@SideOnly(Side.CLIENT)`
and does exactly one thing:
```java
AnimType type = AnimType.values()[m.type];
((ItemGunBase) stack.getItem()).startAnim(player, stack, slot, type);
```
which is:
```java
@SideOnly(Side.CLIENT)
public void startAnim(EntityPlayer player, ItemStack stack, int slot, AnimType type){
    BusAnimation animation = mainConfig.animations.get(type);
    if (animation != null)
        HbmAnimations.hotbar[slot] = new Animation(stack.getItem().getTranslationKey(), System.currentTimeMillis(), animation);
}
```
That's the entire "hook surface" on the receiving end: look up the `BusAnimation` for this `AnimType`
in the gun's own config, and stamp a `(key, startMillis, animation)` triple into a 10-slot
(9 hotbar + 1 offhand) client-only array. Nothing here validates ammo, mutates the `ItemStack`, or
runs any gameplay logic — it is pure "remember what time this started playing" bookkeeping for the
renderer to sample later.

### The Sedna (newer) generalization — same boundary, wider vocabulary

CE's newer weapon framework (`ItemGunBaseNT`/`GunConfig`/`Receiver`, under
`items/weapon/sedna/**`, out of scope for this report — it needs its own `gun_framework_core`
sub-package research) already solves the exact "guns and tools share one animation-trigger
mechanism" problem the legacy system doesn't:
- `AnimationEnums.AnimationType` is a marker interface implemented by both
  `AnimationEnums.GunAnimation` (`RELOAD, RELOAD_EMPTY(deprecated), RELOAD_CYCLE, RELOAD_END, CYCLE,
  CYCLE_EMPTY, CYCLE_DRY, ALT_CYCLE, SPINUP, SPINDOWN, EQUIP, INSPECT, JAMMED` — a superset of the
  legacy `AnimType`) and `AnimationEnums.ToolAnimation` (`SWING, EQUIP`).
- `GunAnimationPacketSedna` is the same shape as the legacy packet (`{short type, int receiverIndex,
  int itemIndex}` — generalized for multi-barrel/multi-receiver weapons) and its handler does the
  same lookup-and-stamp: `HbmAnimationsSedna.hotbar[slot][gunIndex] = new Animation(key, startMillis,
  animation, type, holdLastFrame)`.
- One extra wrinkle worth flagging for whoever scopes the Sedna gun framework itself: on `CYCLE`, the
  packet handler also invokes a per-`Receiver` recoil callback
  (`rec.getRecoil(stack).accept(stack, lambdaContext)`) **entirely client-side, inside the
  `@SideOnly(CLIENT)` packet handler** — i.e. CE's "recoil" here is cosmetic view-kick only, not a
  server-authoritative accuracy/spread penalty (spread is computed separately in the shot-firing
  code, not from this callback). Don't assume "recoil" implies a server round-trip; in this codebase
  it doesn't.
- `AnimationLoader` (JSON-based, replacing the `.dae`/`ColladaLoader` pipeline for Sedna weapons)
  calls `Minecraft.getMinecraft().getResourceManager()` directly — client resource-pack loading, so
  it (like `ColladaLoader`) is Phase 5, not Phase 3, even though the JSON it parses is trivial data.

### The non-gun, non-animloader path: `IAnimatedItem` + `HbmEffectNT` (melee/tools)

`docs/phase1/items_tool.md` flagged `ItemChainsaw` as needing "`BusAnimation`, `HbmAnimations`,
`IAnimatedItem`." What it actually does is a *third*, unrelated trigger mechanism, not a variant of
`GunAnimationPacket`:
```java
// ItemChainsaw.onEntitySwing (server side only, guarded by instanceof EntityPlayerMP)
NBTTagCompound data = new NBTTagCompound();
data.setString("mode", "generic");
MainRegistry.proxy.effectNT(HbmEffectNT.Anim, 0, 0, 0, data);
```
`HbmEffectNT.Anim`'s handler (client-side dispatch table in `HbmEffectNT.java`, ~140 lines covering
`"generic"`/`"equip"`/`"crucible"`/`"swing"`/`"cSwing"`/`"sSwing"`/`"lSwing"` string-keyed modes) is
the thing that actually calls `IAnimatedItem.getAnimation(data, stack)` and stamps
`HbmAnimations.hotbar[slot]`, exactly like `startAnim` does for guns. `ItemCrucible.onEntitySwing`
uses the same route (`AuxParticlePacketNT` + `HbmEffectNT.Anim`, mode `"cSwing"`).

**This route is not itself Phase 3 gun-framework scope.** `HbmEffectNT`/`AuxParticlePacketNT` is a
large, cross-cutting generic VFX/effect dispatch table used all over the mod for particles, sounds,
and screen effects — not something specific to weapons — and Phase 2's own research already
confirmed it doesn't exist in this port yet (`MachinePWRControllerBlock.java`'s own comment: "neither
exists in this port (grepped, zero hits)"). Rebuilding CE's full ~50-branch effect-string dispatch
table is Phase 5 client-VFX-system work, not a Phase 3 prerequisite. See Deferred scope and Key
design decisions below for the recommended narrow substitute.

### The NBT-frame-counter alternative (no animloader/BusAnimation at all)

`GunB92`/`GunB93` (both flagged "misfiled, -> Phase 3" by `docs/phase1/items_special.md`) use neither
animation system. They store a raw NBT `int` ("animation") incremented once per tick in `onUpdate()`,
and a hard-coded formula (`getRotationFromAnim`/`getTransFromAnim`, static, keyed purely off the
counter value's numeric range) that render code reads directly:
```java
private static void setAnim(ItemStack stack, int i) { stack.getTagCompound().setInteger("animation", i); }
// onUpdate(): if (j > 0) { setAnim(stack, j < 30 ? j + 1 : 0); ... }
```
This is entirely self-contained per-item state with no network packet and no `BusAnimation` at all —
the counter is plain `ItemStack` NBT, so it round-trips to the client for free via vanilla's own
`ItemStack` sync. **This pattern needs no animation-system hook whatsoever**: porting it is "store an
int in a Data Component, increment it in the item's tick handler," full stop. The *only* Phase 3
responsibility is preserving the counter field (as a Data Component, per the NBT->Data Component
ground rule) and the increment/reset logic; interpreting the counter into a visual rotation/offset is
pure Phase 5 render-time math with no state of its own to manage.

## Phase-3-safe scope

Portable now, as data/logic with no client-rendering dependency (verified: zero
`GlStateManager`/`GL11`/`net.minecraft.client`/`Minecraft.getMinecraft()` imports in every class
listed here except where explicitly noted otherwise):

| Class / concept | Count | What it is |
|---|---|---|
| `render.anim.BusAnimation`, `BusAnimationSequence`, `BusAnimationKeyframe` | 3 files | Pure keyframe/interpolation data model (`double[]` transforms, Blender-derived easing math). No GL, no `Minecraft` reference. `BusAnimationKeyframe`'s one `ClientConfig.GUN_ANIMATION_SPEED` read is a plain static double field with a default (`ConfigWrapper<Double>`, not a Forge-config-system side-restricted value) — confirmed safe to read on the server as well as the client, since `GunConfiguration.animations` maps are constructed once at startup and read from `spawnProjectile()`/`startReloadAction()` on the **server**. |
| `render.anim.sedna.BusAnimationSedna`, `BusAnimationSequenceSedna`, `BusAnimationKeyframeSedna` | 3 files | Same shape, Sedna's evolution. Also zero client imports. Belongs with whichever package researches the Sedna gun framework itself, not duplicated here, but flagged as Phase-3-safe *data* now so that package doesn't need to re-derive this finding. |
| `render.anim.HbmAnimations.AnimType` (the enum only, not the rest of the class) | 1 enum, 6 values | `RELOAD, CYCLE, ALT_CYCLE, SPINUP, SPINDOWN, EQUIP`. This is the vocabulary `GunConfiguration.animations` keys on and the trigger call sites switch over. Pure data. |
| `render.anim.sedna.AnimationEnums` (`AnimationType` marker + `GunAnimation`/`ToolAnimation`) | 1 file, 2 enums (13 + 2 values) | Same role, Sedna's superset vocabulary — also unifies gun and tool animation triggers under one interface, which is the pattern this port should copy (see Key design decisions). |
| `handler.GunConfiguration.animations` (the `Map<AnimType, BusAnimation>` field + everything else on that class) | 1 field on 1 class | Already a `gun_framework_core` data type regardless of this report; the animation map is just one more field on it, no different in kind from `rateOfFire`/`ammoCap`. |
| The trigger call sites: `spawnProjectile()`'s `animations.containsKey(CYCLE)` check + send, `startReloadAction()`'s unconditional send | 2 call sites in `ItemGunBase` (→ `gun_framework_core`'s fire/reload equivalent) | Server-authoritative logic: "did this gun just do X, and is an animation configured for X" — exactly the hook the gun framework depends on existing. This is the `startAnimation(stack, name)`-shaped surface the task asked about. |
| A new S2C `CustomPacketPayload` (`GunAnimationPacket` equivalent) | 1 new payload, registered in `HbmNetwork` | The wire contract only — see Key design decisions for its exact shape and why the *handler body* should still be a Phase 3 no-op stub, not full Phase 5 logic, so the wire format doesn't churn later. |
| The NBT-counter pattern's state (`GunB92`/`GunB93`'s `"animation"`/`"energy"` ints, as Data Components) + the tick-increment logic | 2 classes | No animation-system hook needed at all; just item tick logic + component storage, as described above. |

## Deferred scope

Needs a later phase (named explicitly, per package) before it can be finished, even though the
Phase-3 trigger side is complete without it:

- **`com.hbm.animloader` (all 6 files) → Phase 5.** Collada skeletal rendering
  (`AnimatedModel`/`AnimationController`/`AnimationWrapper`/`Transform`/`ColladaLoader`), 100% GL
  display-list based. Used today by non-gun render classes (`RenderDoorGeneric`,
  `RenderSlidingBlastDoorLegacy`, `RenderSiloHatch`, `ItemRenderJetpackGlider`,
  `WorldSpaceFPRender`) — none of it is gun-specific, and none of it gates anything `gun_framework_core`
  needs to compile or function.
- **`render.anim.HbmAnimations` (the class itself, minus the `AnimType` enum already carved out above)
  → Phase 5.** The `hotbar[]` per-slot client array, `getRelevantAnim`/`getRelevantTransformation`/
  `getTimeDifference` (all read `Minecraft.getMinecraft().player` directly), and the
  `BlenderAnimation` inner class (bridges into the animloader `AnimationWrapper`). This is the
  render-time *consumer* of the state Phase 3's payload produces.
- **`render.anim.sedna.HbmAnimationsSedna` → Phase 5.** Same role for Sedna; additionally
  `applyRelevantTransformation` calls `GlStateManager.translate/rotate/scale` directly, making the
  GL dependency explicit in-class (unlike the legacy version, which keeps GL calls in the separate
  renderer classes).
- **`render.anim.sedna.AnimationLoader` → Phase 5.** JSON keyframe loading via
  `Minecraft.getMinecraft().getResourceManager()` — client resource-pack access, same category as
  `ColladaLoader` even though the format itself is trivial.
- **The `GunAnimationPacket`/`GunAnimationPacketSedna` client-side handler *bodies* (the
  `HbmAnimations.hotbar[slot] = new Animation(...)` assignment and everything downstream of it) →
  Phase 5.** The payload registration and server-side send call are Phase 3; populating and then
  reading the per-slot state array back out for rendering is not.
- **`items/tool/ItemChainsaw`'s actual swing-animation trigger (the `IAnimatedItem` +
  `HbmEffectNT.Anim` route) → partially Phase 5, partially needs a Phase-3-adjacent decision now.**
  The `IAnimatedItem` interface and the `BusAnimation` it returns are Phase-3-safe data; the delivery
  mechanism it currently rides on (`HbmEffectNT`'s ~50-branch generic effect dispatch, confirmed
  unported per Phase 2's own research) is out of scope for `gun_framework_core` to build. See Key
  design decisions for the recommended substitute (reuse the gun payload's shape instead of building
  `HbmEffectNT`).
- **The Sedna gun framework itself (`ItemGunBaseNT`, `GunConfig`, `Receiver`, the recoil-callback
  system) → its own Phase 3 sub-package research**, not covered here beyond the animation-trigger
  boundary already described above. It is a substantially larger surface (per-receiver/per-barrel
  indexing, lambda-based recoil/state callbacks, jam states) than the legacy `ItemGunBase` framework
  and deserves a dedicated pass rather than being folded into this animation-focused report.
- **First-person/third-person item-render wiring** (`ItemRenderWeaponShotty`, `ItemRenderWeaponVortex`,
  `ItemRenderCrucible`, `ItemRenderChainsaw`, `ItemRenderBoltgun`, `ItemRenderGrenade` — the 6
  `render.item.weapon`/`render.item` classes that actually call into `HbmAnimations` to draw a gun
  model) → Phase 5, no exceptions; these are pure `BakedModel`/`ItemStackRenderer`-equivalent
  rendering with no gameplay logic of their own.

## Key design/API decisions

Confirmed from real code already committed in this port (`HbmNetwork`, `BufPacket`) — no NeoForge API
invented below; Neo Edition was not consulted for this report per the task's own instruction that it
is authoritative only for entity/damage/armor/explosion API shapes, none of which apply here:

- **Register one new S2C `CustomPacketPayload` following `BufPacket`'s exact confirmed pattern**
  (`record` + `Type<T>` + `StreamCodec<RegistryFriendlyByteBuf, T>` + a
  `registrar.playToClient(...)` line appended to `HbmNetwork.registerPackets`), carrying the
  equivalent of `GunAnimationPacket`'s `{int type, EnumHand hand}` — in 1.21.1 terms, `{AnimType
  animType, InteractionHand hand}` (or an `int`/`byte` ordinal + a `boolean` for main/off hand, same
  as CE's own wire-thrifty choice — `EnumHand hand > 0 ? OFF : MAIN` — worth keeping for wire-size
  parity since this packet is sent on every single shot of every automatic weapon in the game).
  Register it once, in Phase 3, with a **stub client handler** (`context.enqueueWork(() -> {})` or a
  logged no-op) — this locks the wire contract in now so `gun_framework_core`'s fire/reload logic has
  something real to call, without Phase 3 needing to build any of the client-side state array or
  renderer that will eventually consume it. Phase 5 fills in the handler body; it does not change the
  payload shape.
- **Expose the hook as a method on the gun-framework's `ItemGunBase` equivalent**, matching CE's own
  `startAnim` shape but moving the "is an animation even configured" check to the *sending* side (as
  CE already does for `CYCLE`, just not for `RELOAD` — worth normalizing both to the same guarded
  shape rather than porting the asymmetry verbatim):
  ```java
  protected void triggerGunAnimation(ServerPlayer player, ItemStack stack, InteractionHand hand, AnimType type) {
      if (mainConfig.animations.containsKey(type)) {
          PacketDistributor.sendToPlayer(player, new GunAnimationPayload(type, hand));
      }
  }
  ```
  This is the exact `startAnimation(stack, name)`-shaped surface the task named as the target — call
  sites in `fire()`/`startReloadAction()` become one-line calls to this method, identically to CE.
- **Copy the Sedna framework's `AnimationType` marker-interface pattern, not the legacy single-enum
  `AnimType`, even for the legacy/simple gun path.** Sedna already proved (in CE itself) that guns and
  melee tools can share one animation-trigger vocabulary and one payload shape
  (`GunAnimation`/`ToolAnimation` both implementing `AnimationType`). Designing `gun_framework_core`'s
  trigger enum as `interface HbmAnimationType {}` implemented by a `GunAnimationType` enum now, with
  room for a `ToolAnimationType` enum later (`ItemChainsaw`'s `SWING`/`EQUIP`), means `ItemChainsaw`
  and other melee weapons flagged in `docs/phase1/items_tool.md` can reuse the *same* payload and
  registration Phase 3 already built for guns, instead of Phase 5 needing to invent a second,
  parallel trigger mechanism (or resurrect CE's much larger `HbmEffectNT` generic dispatch just to
  send one enum value). This directly resolves the `ItemChainsaw`/`IAnimatedItem` deferred item above
  without taking on `HbmEffectNT`'s full scope.
- **Do not port `AnimationController`** (`animloader` package) — CE's own in-file comment on it
  (`"I'm pretty sure this class is entirely pointless and just acts as a stupid getter/setter for the
  wrapper... TODO delete"`) is the original authors flagging it as dead weight; treat it as
  Phase-5-optional even within its own already-deferred package, not as something to preserve for
  fidelity.
- **Preserve the "no animation configured -> no packet sent" short-circuit exactly.** Several weapons
  (e.g. base `ItemGunBase` fire cycles with no `CYCLE` entry in their `animations` map) intentionally
  send nothing — this isn't a gap to fill, it's how CE avoids a network packet on every single shot of
  every weapon that has no visual recoil animation at all (many machine-gun-tier weapons rely on pure
  procedural view-kick elsewhere, not a `BusAnimation`).

## Open questions / risks

- **Confirm `GunConfiguration` (and therefore its `animations` map) really is constructed identically
  on dedicated server and client**, not lazily/only-client as a latent CE assumption. This report
  traced the `containsKey`/`get` call sites into `spawnProjectile()`/`startReloadAction()`, both of
  which run under `updateServer()`/server-only `startAction()` — i.e. the map must already exist
  server-side today in CE, which is what makes `BusAnimationKeyframe`'s `ClientConfig` read a real
  (if currently benign) common-code hazard rather than a hypothetical one. Worth an explicit unit
  check once `gun_framework_core`'s config factories are ported, rather than assuming symmetry.
  Whichever config-value system replaces `ConfigWrapper` in the port should keep this same
  server-readable property, not accidentally gate it behind an `@OnlyIn(Dist.CLIENT)` config type.
- **The Sedna recoil-callback finding above (`Receiver.getRecoil` running entirely inside the
  client-only packet handler) should be re-verified by whoever scopes the Sedna gun framework** —
  this report only traced it far enough to confirm it's cosmetic-only in the *legacy* animation-
  trigger sense, not to audit whether any Sedna weapon relies on it for something that should have
  been server-authoritative (e.g. a jam chance or spread change). Flagging as a risk to carry forward,
  not a resolved finding.
- **Wire-format lock-in risk**: because the recommendation above is to register the new payload in
  Phase 3 with a stub handler and fill in the real client logic in Phase 5, the payload's `Type` id
  and `StreamCodec` become a cross-phase contract the moment Phase 3 ships. Any Phase-5 realization
  that the shape needs to change (e.g. adding a `receiverIndex` field once the Sedna framework is
  scoped, mirroring `GunAnimationPacketSedna`) means either a `PROTOCOL_VERSION` bump in `HbmNetwork`
  or a second payload type — worth deciding up front whether the legacy-shaped payload
  (`{type, hand}`) is meant to be gun-framework's *only* animation payload long-term, or whether it
  should be designed from Phase 3 with the Sedna-shaped `{type, receiverIndex, itemIndex}` fields
  present but unused by the legacy path, to avoid a second payload type later.
- **Explosion/block-removal batching is a real Phase 3 concern raised by PORT_SPEC, but it is
  orthogonal to this report's scope.** Noting only because two of the animation-triggering weapons
  surveyed here (`GunB92`'s `EntityExplosiveBeam` swarm, `GunB93`'s overcharge-triggered
  `EntityNukeExplosionMK3`) are exactly the kind of weapon whose *explosion* implementation (not its
  animation) will hit that concern directly — a naive per-block `Level#setBlock` loop on either of
  these firing will not perform acceptably, per PORT_SPEC's explicit call for batched `LevelChunk`
  section writes + deferred lighting. That work belongs with whichever package researches CE's
  explosion engine itself, not with the animation-trigger hook surface documented here.
- **`docs/phase1/items_special.md`'s "misfiled" framing for `GunB92`/`GunB93`** is about their
  *package location* in CE (`items/special/weapon` vs `items/weapon`), not their animation approach —
  confirmed here that both use the same NBT-counter pattern regardless of package, so the misfiling
  note has no bearing on this report's scope beyond the state-shape already documented above.
