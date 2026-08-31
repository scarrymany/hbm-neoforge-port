# Armor / HumanoidModel rendering — Phase 5 research

**Area:** `armor_humanoidmodel_rendering` — CE's custom armor-model rendering (power-armor/hazmat/FSB
sets rendered over the vanilla biped), the mod-slot ("chip") render path (jetpacks-as-insert,
`ItemModTesla`/`ItemModGasmask`), `IArmorDisableModel`'s body-part hiding, and the jetpack's
particle/glider render trail (`JetpackGlider`'s `JetpackHandler`-hosted flight VFX).

**Scope boundary (per task instructions):** the OBJ parsing/loading pipeline itself
(`HFRWavefrontObject`/`ModelRendererObj`/`IGeometryLoader`) and the general `BlockEntityRenderer`
framework are `docs/phase5/renderer_framework_and_obj_models.md`'s scope, not re-derived here except
where this area's own confirmed API shapes depend on them. First-person gun/weapon-arm rendering,
muzzle flash, and the `BusAnimation` gun-animation system are
`docs/phase5/weapon_gun_rendering_animloader.md`'s scope. `IPAMelee`/`IPARanged`'s built-in-weapon
firing logic is the gun-framework's scope (`docs/phase3/gun_framework.md`) — only the render-hook
shape of the power-armor weapon-arm pose is touched on here.

## Method

Read directly, with exact paths/line counts cited inline:
- `upstream/hbm-ce` (CE 1.12.2 source — sole source of truth for behavior/asset names/visual intent)
- `upstream/neo-edition` (a parallel, partial, **already-compiling-against-the-same-`neo_version`**
  1.21.1 port — used *only* to confirm real NeoForge 1.21.1 client-rendering API shapes; every
  design/behavior/layout choice below comes from CE, never from neo-edition). Confirmed identical
  `neo_version=21.1.228` in both `gradle.properties` files.
- This port's own `src/main/java` tree — every already-committed `com.hbm.items.armor.*`/
  `com.hbm.items.gear.*` class this task named, read for its exact forward-reference javadoc
- `docs/phase3/{armor_equippable_framework,armor_special_sets,fsb_armor_and_jetpacks}.md` (all three
  already on disk, all three explicitly deferred client rendering to this exact area)
- `docs/phase5/{renderer_framework_and_obj_models,weapon_gun_rendering_animloader}.md` (the two
  sibling Phase 5 reports already written this wave, for shared infra and cross-references)

This sandbox cannot run `./gradlew` (network policy blocks `maven.neoforged.net`) and cannot launch a
client or open a real NeoForge jar — confirmed again this pass (`find / -iname '*neoforge*.jar'` and a
whole-filesystem search for `IClientItemExtensions.class` both returned nothing). Every API-shape claim
below is either (a) read directly from a **compiling, exercised call site** in this port's own source or
neo-edition's source at the cited line, (b) read directly from an **unused-but-resolving import** (a
weaker but still real signal — the class exists at that package path in NeoForge 21.1.228, since
neo-edition compiles with it present, but its exact method shape is not demonstrated by a call site), or
(c) explicitly flagged **"well-established knowledge, NOT verified against a real jar or compiling
example in either reference tree."**

## Sources read in full

CE (`upstream/hbm-ce/src/main/java/com/hbm/`):
- `render/model/ModelArmorBase.java` (183 lines) — the abstract 1.12 `ModelBiped`-subclass every
  OBJ-driven armor model extends
- `render/model/ModelArmorHEV.java` (64) — representative concrete OBJ-per-slot leaf
- `render/model/ModelGasMask.java` (138) — representative hand-modeled (no OBJ) mask leaf
- `render/model/ModelJetPack.java` (161) — the jetpack's own worn-model (plain `ModelRenderer` boxes,
  not OBJ)
- `render/model/ModelNo9.java` (69) — the one CE armor model that is *both* OBJ-driven *and* has a
  runtime-toggled emissive sub-part (the lamp)
- `render/entity/layers/LayerArmorMod.java` (52) — **the single central render layer CE uses for every
  mod-slot-insert render, including jetpack-as-insert**
- `main/ModEventHandlerRenderer.java` (278) — **the actual `IArmorDisableModel` consumer**
  (`onRenderPlayerPre`/`onRenderPlayerPost`, `RenderPlayerEvent.Pre`/`.Post`)
- `items/armor/JetpackBase.java` (133, CE) — `modRender`/`getArmorModel`/`isValidArmor`, the dual-mode
  render dispatch
- `items/armor/ItemModTesla.java` (74) — a second, non-jetpack `modRender` example, confirming
  `LayerArmorMod` is general infra, not jetpack-specific

CE, read in targeted full sections (not end-to-end, cited by line range):
- `handler/JetpackHandler.java` (1,074 total) — lines 860–1000 read in full (`JetpackLayer.doRenderLayer`,
  `JetpackInfo`'s client-only fields); the rest already fully read by
  `docs/phase3/fsb_armor_and_jetpacks.md` (cited, not re-read line-for-line here)
- `main/ClientProxy.java` (629 total) — lines 540–565 read in full (`postInit`'s
  `r.addLayer(new JetpackHandler.JetpackLayer())` / `r.addLayer(new LayerArmorMod(r))` registration
  call, the confirmed real registration site for both layers)

CE, grepped/sized only (superclass/field check, not read end-to-end — flagged, not silently treated as
fully surveyed): `render/model/{ModelArmorDesh,ModelArmorDiesel,ModelArmorRPA,ModelArmorAJRO,
ModelArmorDigamma,ModelCloak,ModelHat,ModelM65,ModelM65Blaze}.java` (OBJ-resource-field names only,
to confirm the OBJ-vs-hand-modeled split per set), `items/ModItems.java` (grepped for the `no9` item
declaration line), `assets/hbm/models/armor/*.obj` (directory-listed, 20 files, one `ls`).

Neo Edition (`upstream/neo-edition/src/main/java/com/hbm/`), read in full:
- `items/armor/ArmorNo9.java` (77) — the one real, compiling, currently-used custom-armor-model item
- `render/model/armor/ModelArmorBase.java` (112) — the confirmed real 1.21.1 successor to CE's class of
  the same name
- `render/model/armor/ModelNo9.java` (63) — confirmed real 1.21.1 successor to CE's `ModelNo9`
- `render/loader/ModelRendererObj.java` (101) — confirmed real 1.21.1 per-part OBJ renderer (shared
  infra with `renderer_framework_and_obj_models.md`, re-read here only for the armor-specific call
  shape)
- `items/armor/ItemArmorMod.java` (71, neo-edition) — confirms this port's own port of the same class
  already matches the real 1.21.1 shape; also confirms `net.neoforged.neoforge.client.event.
  RenderPlayerEvent` resolves as an import at this exact NeoForge version (present but **unused** in
  this file's body — no compiling call site demonstrates its actual method shape, see Open questions)

Neo Edition, read in targeted full sections: `main/NuclearTechModClient.java` (1,075 total) — lines
1–130 (full import list + class annotations) and 590–690 (`onRenderLiving`/`registerLayerDefinitions`)
read in full; the rest grepped for `RenderPlayerEvent|RenderLivingEvent|addLayer|AddLayers|
RenderLayerParent` (zero further hits — see Headline finding 3).

This port's own (`src/main/java/com/hbm/`), read in full:
- `items/gear/ArmorModel.java` (96) — the already-committed generic client-model hook base class
- `items/gear/ArmorGasMask.java` (146) — an already-committed concrete `initializeClient` stub
- `items/gear/JetpackGlider.java` (153) — full item-shell javadoc, confirms the Collada/particle
  deferral
- `items/armor/IArmorDisableModel.java` (30) — the port of CE's part-hiding interface
- `items/armor/ItemArmorMod.java` (100) — the port of CE's mod-slot base, including its own
  "Phase 5's job" render-hook forward reference
- `items/armor/JetpackBase.java` (143, this port) — including its own explicit open blocker about
  standalone-wear `Equippable`
- `items/armor/IPAMelee.java` (27) — confirms the "simplified relative to CE" first-person weapon-arm
  scope note
- `items/armor/ArmorDataComponents.java` (76) — confirms all render-relevant state is already a
  synced Data Component
- `items/armor/ArmorHat.java` (31), `items/armor/ArmorBJ.java` (46) — two more already-committed
  `Phase 5` forward references
- `items/armor/PoweredArmorItems.java` (472) — the registrar for all 66 already-committed power-armor
  items this area's models must eventually cover
- `items/armor/ItemModV1.java` (70) — confirms `ItemArmorMod#getModifiers` has zero real consumers yet
  (an adjacent, not-yet-wired mechanic, noted but out of this area's scope)
- `items/gear/JetpackItems.java` (75) — the registrar for all 5 already-committed jetpack items

This port's own, grepped only: `items/gear/ArmorHazmatMask.java` (`initializeClient` stub location),
`items/gear/{SpecialArmorItems,ArmorHazmat}.java` (Phase 5 javadoc cross-reference lines), whole-tree
grep for `Phase 5|Phase5` across `items/armor/` and `items/gear/` (28 files matched — table below),
whole-tree grep for `Equippable|EQUIPPABLE` (3 matches, none in neo-edition), whole-tree grep for
`IClientItemExtensions|getGenericArmorModel|getHumanoidArmorModel|HumanoidModel` in both this port and
neo-edition.

Docs read in full: `docs/phase3/{fsb_armor_and_jetpacks.md (124), armor_equippable_framework.md (343),
armor_special_sets.md (463)}`, `docs/phase5/renderer_framework_and_obj_models.md` (447). Docs read in
targeted section: `docs/phase5/weapon_gun_rendering_animloader.md` (408 total, lines 160–224 read in
full for its confirmed `IClientItemExtensions`/Mixin findings).

## Headline findings

1. **The task's framing conflates two independent CE mechanisms that map onto two different, both-real
   NeoForge 1.21.1 APIs — and this port has already, correctly, split them in already-committed code.**
   - **Path A — a standalone-worn armor piece's own model** (CE: `ItemArmor#getArmorModel(
     EntityLivingBase, ItemStack, EntityEquipmentSlot, ModelBiped)`). The confirmed real 1.21.1
     replacement is **not** `IClientItemExtensions#getHumanoidArmorModel` (the task's guessed name) —
     it is `IClientItemExtensions#getGenericArmorModel(LivingEntity, ItemStack, EquipmentSlot,
     HumanoidModel<?> original)`, returning a plain `net.minecraft.client.model.Model` (not itself a
     `HumanoidModel` subclass), registered via `Item#initializeClient(Consumer<IClientItemExtensions>)`.
     This is not a guess: it is the **exact, already-compiling shape** neo-edition's `ArmorNo9.java`
     uses (lines 39-51), and this port's own `ArmorModel`/`ArmorGasMask`/`ArmorHazmatMask` (Phase 3,
     already committed) already declare the identical override with a `// TODO(Phase 5)` body. **No
     `LivingEntityRenderer#addLayer`/`RenderLayerParent` involvement at all** for this path — vanilla's
     own armor-rendering machinery (not reproduced/verified in this sandbox, see Open questions) is
     presumed to already call `getGenericArmorModel` per equipped slot without any mod-side layer
     registration, exactly as `ArmorNo9`/`ArmorModel` assume.
   - **Path B — a mod-slot ("chip") insert's render, and the jetpack-glider's particle trail.** CE's
     own real mechanism here is a **literal custom player-render layer**, added via `RenderPlayer#
     addLayer` (1.12) — `ClientProxy.java:558-559` registers exactly two:
     `new JetpackHandler.JetpackLayer()` and `new LayerArmorMod(r)`, onto **every player skin variant**
     (`getRenderManager().getSkinMap().forEach(...)`). `LayerArmorMod.doRenderLayer` (52 lines, read in
     full) is the *general* mod-slot render dispatcher: it iterates all 4 armor slots, prys every
     installed mod, and calls `((ItemArmorMod) mod.getItem()).modRender(ctx, armor)` on each one —
     **this is exactly the `LivingEntityRenderer#addLayer`/`RenderLayerParent` mechanism the task's
     framing named**, just for this second path, not the first. `ItemModTesla.modRender` (74 lines,
     read in full — a "backpack tesla coil" mod chip, not a jetpack) confirms this layer is genuinely
     general infra, not a jetpack-only hack. **This port's own `ItemArmorMod` javadoc already flags
     the gap accurately**: "this port's own `ItemArmorMod` does not yet declare a client-render
     mod-slot hook at all, so there is nothing to stub here" — i.e. Path B's hook point does not exist
     in this port yet at all, unlike Path A's (already stubbed).
   - Both paths are real, both are needed, and **neither substitutes for the other**: a standalone-worn
     jetpack chestplate needs Path A (once/if it can be worn at all, see finding 2); a jetpack **or any
     other `ItemArmorMod`** installed into another chestplate's mod slot needs Path B, unconditionally,
     regardless of Path A's status.
   - **`IArmorDisableModel`'s body-part hiding is a third, separate mechanism — an event listener, not
     a layer and not the armor-model hook.** `ModEventHandlerRenderer.java` (278 lines, read in full)
     confirms CE toggles `ModelRenderer.isHidden` on the *vanilla* player model's own body parts
     directly inside `RenderPlayerEvent.Pre`/`.Post` listeners (`onRenderPlayerPre`/`onRenderPlayerPost`,
     lines 151-201), not inside any `LayerRenderer`. The confirmed real 1.21.1 event class this maps
     onto is `net.neoforged.neoforge.client.event.RenderPlayerEvent` — its import **resolves** in
     neo-edition's `ItemArmorMod.java:11` (proving the class exists at that path in NeoForge 21.1.228),
     but no compiling call site anywhere in either reference tree exercises its `.Pre`/`.Post`
     nested-class API, so its exact method signatures are **not** independently confirmed — see Open
     questions 1. The more general `RenderLivingEvent.Pre<? extends LivingEntity, ? extends
     EntityModel<?>>` (used with a real, exercised call site in `NuclearTechModClient.java:630-653` for
     an unrelated arm-pose tweak) is confirmed to exist, resolve, and expose `.getEntity()`/
     `.getRenderer().getModel()` casting to `HumanoidModel<?>` with mutable `ModelPart` fields
     (`.head`/`.body`/`.leftArm`/etc., each presumably carrying a `.visible` boolean per public
     Minecraft/NeoForge knowledge, **not directly demonstrated by a `.visible =` write in either
     reference tree** — flagged, not assumed).
2. **Standalone jetpack wear (Path A actually firing for a jetpack) is blocked on a real, already-named,
   already-flagged-by-this-port's-own-code open question — and it directly gates whether this area's
   Path A work has any consumer for the 5 jetpack items specifically.** This port's own
   `JetpackBase.java` (`items.armor`, 143 lines, read in full) already documents: jetpacks extend
   `ItemArmorMod extends Item` (not `ArmorItem`) because Java single inheritance forbids also extending
   `ArmorItem` (needed for the mod-slot-insert half of dual-mode delivery) — so equipping a jetpack
   standalone via ordinary click/shift-click actions needs a `DataComponents.EQUIPPABLE`/`Equippable`
   builder call this port has **not confirmed the exact API surface of in this sandbox** ("no compiler
   or decompiled source was reachable to confirm the exact `DataComponents.EQUIPPABLE`/`Equippable`
   builder API surface at this specific NeoForge version"). This report's own grep confirms **zero**
   `Equippable`/`EQUIPPABLE` usage anywhere in neo-edition — the sibling reference tree has no
   compiling precedent either, so this is a genuine, still-open, cross-cutting gap, not something this
   report can resolve by reading more CE or neo-edition source. **This does not block Path B at all**
   (jetpack-as-mod-slot-insert delivery is already fully functional server-side per
   `docs/phase3/fsb_armor_and_jetpacks.md`), and it does not block porting the `ModelJetPack`-equivalent
   `Model` class itself (Path A's render body can be written and will simply have no caller for
   standalone wear until `Equippable` is confirmed and added at the item's registration site).
3. **CE has three distinct armor-rendering "content shapes," but all three funnel through the identical
   confirmed `getGenericArmorModel` hook — the fork is authoring effort, not API shape.**
   - **(a) OBJ-driven, full-body-part replacement, one mesh group per `EquipmentSlot`** — the majority
     of "real" power-armor sets (`ModelArmorHEV` read in full: a `type` int of 0/1/2/3 dispatches to
     `ModelRendererObj`-wrapped OBJ groups per helmet/chest/legs/boots, one dedicated texture per
     group). Confirmed same shape in CE for `ModelArmorAJR(O)`, `ModelArmorBJ`, `ModelArmorBismuth`,
     `ModelArmorDNT`, `ModelArmorDesh`, `ModelArmorDiesel`, `ModelArmorDigamma`, `ModelArmorEnvsuit`,
     `ModelArmorNCRPA`, `ModelArmorRPA`, `ModelArmorT51`, `ModelArmorTaurun`, `ModelArmorTrenchmaster`,
     `ModelArmorWings`, `ModelNo9` (grepped/sized, not all read end-to-end — see Sources). 20 `.obj`
     files exist under `assets/hbm/models/armor/` (directory-listed in full this pass:
     `ajr/bismuth/bj/dnt/envsuit/fau/hat/hev/ncrpa/no9/steamsuit/t51/taurun/trenchmaster` +
     `goggles/mod_tesla/murk` + 3 non-armor-set meshes `bnuuy/player_fem/remnant`, likely alternate
     base-model content, not investigated further).
   - **(b) Hand-modeled, plain `ModelBiped`/`ModelRenderer` boxes, no OBJ at all** — `ModelGasMask`
     (138 lines, read in full: 6 hand-placed `ModelRenderer` boxes forming a gas mask, no
     `ModelRendererObj`) and `ModelJetPack` (161 lines, read in full: 8 boxes forming tanks/ducts/
     thrusters, only the parent `JetPack` group's `render()` call is live — the individual
     child-part render calls are commented out in CE's own source, i.e. CE renders the whole rig as
     one recursive parent-child hierarchy, not per-part). `ModelM65`/`ModelM65Blaze`/`ModelCloak`/
     `ModelHat` are plausibly the same shape (sized/superclass-checked only, not read end-to-end).
   - **(c) No custom model at all** — confirmed for `ArmorLiquidator` (no matching `ModelArmorLiquidator`
     file exists anywhere in CE's `render/model/` directory; this port's own `PoweredArmorItems.java`
     javadoc independently confirms Liquidator's only client-render concern is a `renderHelmetOverlay`
     motion-blur GL overlay, "not reproduced here" (Phase 5)) — these sets render with the plain
     vanilla per-material armor texture layer and need no `getGenericArmorModel` override at all.
   All three shapes plug into the identical `IClientItemExtensions#getGenericArmorModel` hook already
   stubbed in this port's committed code — a leaf class in bucket (a) or (b) fills in a real `Model`;
   a leaf in bucket (c) simply never overrides `getGenericArmorModel` at all (inherits `ArmorModel`'s
   `return original` default, or doesn't extend `ArmorModel` in the first place).
4. **`JetpackGlider`'s worn-model animation is doubly blocked, and the block is not this area's to
   resolve — but its *particle trail* is a separable, non-blocked sub-problem this area can build.**
   `JetpackHandler.JetpackLayer.doRenderLayer` (CE, read in full at lines 867-951) draws the glider's
   "activate" opening animation via `ResourceManager.jetpack.renderAnimated(...)` — CE's Collada
   skeletal-animation system (`com.hbm.animloader`, already confirmed by
   `docs/phase5/renderer_framework_and_obj_models.md` finding 1 to have **no confirmed NeoForge 1.21
   equivalent at all** and to be blocked on a separate, not-yet-made project-wide decision — same
   blocker, not re-derived here). **The particle trail is architecturally separate**: the same method
   also spawns/renders `booster_particles`/`brightness_particles`/`distortion_particles`
   (`ParticleRocketPlasma`/`ParticleFakeBrightness`/`ParticleHeatDistortion`) and two
   `ParticleJetpackTrail` instances (spawned in `postRenderPlayer`, line 650-651) — these do **not**
   depend on Collada at all, only on (i) `hasJetpack(player)` (already-portable pure logic per
   `docs/phase3/fsb_armor_and_jetpacks.md` Deferred scope item 1) and (ii) Phase 2's particle-packet/
   `ParticleEngineNT`-equivalent system (confirmed present in neo-edition, `com.hbm.particle.engine.*`,
   imported by `NuclearTechModClient.java`). **A real design change is needed, not a line-for-line
   port**: CE's particle-render call reads back the *raw OpenGL modelview matrix*
   (`GlStateManager.getFloat(GL11.GL_MODELVIEW_MATRIX, ...)`) and replays it later via a
   `ClientProxy.deferredRenderers` callback and `GL11.glLoadMatrix` — a 1.12 immediate-mode technique
   with no equivalent in 1.21.1's retained-mode `PoseStack`/`MultiBufferSource` pipeline (confirmed
   absent from both reference trees per `docs/phase5/weapon_gun_rendering_animloader.md`'s own finding
   that "no `GlStateManager`/`Tessellator`/`BufferBuilder`... calls anywhere in Neo Edition's port").
   The `PoseStack` already threaded into a `RenderLayer.render(PoseStack, ...)` call (per public
   Minecraft/NeoForge API shape, not independently confirmed by a compiling armor/jetpack-layer example
   in this sandbox — see Open questions 2) should carry the transform forward directly; no matrix
   read-back/replay trick should be needed at all in the modern pipeline.
5. **This port's own Phase 3 work already did the hard design thinking for this area — it left compiling
   stubs, not open guesses.** A whole-tree grep of `items/armor/` + `items/gear/` for `Phase 5|Phase5`
   matched **28 files** (full list below). Of those, `ArmorModel`, `ArmorGasMask`, and `ArmorHazmatMask`
   already contain a real, compiling `initializeClient`/`getGenericArmorModel` override with an explicit
   `// TODO(Phase 5): ...` comment describing exactly which CE model class belongs there and why it
   isn't there yet — this area's job for those three specific classes is to **fill in the method body**,
   not design the hook. The other 25 files are javadoc-only forward references (no client-render method
   stub exists yet) on already-committed leaf armor/jetpack items whose `getGenericArmorModel`
   overrides, mod-render hooks, or `disablesPart` render consumer do not exist at all yet.

## This port's own `Phase 5` forward references (full grep result, 28 files)

| File | What it already says |
|---|---|
| `items/armor/ArmorAJR.java`, `ArmorAJRO.java`, `ArmorHEV.java`, `ArmorT51.java`, `ArmorTaurun.java`, `ArmorDigamma.java` | "CE's whole class is client-model/renderer plumbing (Phase 5); no behavior of its own beyond `ArmorFSBPowered`'s [own already-ported mechanics]." Zero non-rendering behavior left to port — purely a `Model` + texture job. |
| `items/armor/ArmorBJ.java`, `ArmorBismuth.java`, `ArmorDNT.java`, `ArmorDesh.java`, `ArmorDiesel.java`, `ArmorEnvsuit.java`, `ArmorLiquidator.java`, `ArmorNCRPA.java`, `ArmorRPA.java`, `ArmorTrenchmaster.java` | "Beyond client-model/renderer plumbing (Phase 5), CE's [remaining] non-rendering mechanic is: ..." — each already ported its own non-rendering mechanic (see `PoweredArmorItems.java`'s per-set builder-chain javadoc), leaving only the model/texture as this area's job. `ArmorLiquidator` additionally names `renderHelmetOverlay` (motion-blur GL overlay) as "Phase 5, not reproduced here." |
| `items/armor/ArmorHat.java` | Extends `ArmorModel` (not `ArmorFSB`) specifically because it needs the custom-hat-model hook and has no full-set bonus of its own. |
| `items/armor/IArmorDisableModel.java` | "consumed by the (Phase 5) player render layer" — this report's finding 1's third mechanism (`RenderPlayerEvent.Pre`/`.Post`, confirmed above), not a `LayerRenderer`. |
| `items/armor/IPAMelee.java` | CE's real interface also carries `setupFirstPerson`/`renderFirstPerson`/`playAnim`/`orchestra` (first-person weapon-arm rendering, `BusAnimationSedna`-driven) — explicitly out of this port's simplified interface, "a Phase 5 pass can widen this interface with a confirmed 1.21.1 first-person-rendering hook once one exists" (see `docs/phase5/weapon_gun_rendering_animloader.md`'s confirmed `getArmPose`/`applyForgeHandTransform` hooks — the natural landing spot, cross-referenced not re-designed here, since it belongs to the gun/weapon-arm rendering area by mechanism). |
| `items/armor/ItemArmorMod.java` | Drops CE's `offset(EntityPlayer,EntityPlayer,float)`/`copyRot(ModelBiped,ModelBiped)` GL-immediate-mode helpers entirely — "Phase 5's job if a jetpack/mod render layer ever needs equivalent math again... inventing a placeholder here would be a guess." This report's finding 1 Path B is exactly that job; the modern replacement is ordinary `PoseStack` translate/rotate inside the new render-layer's `render()` method, not a 1:1 port of these two helpers. |
| `items/armor/JetpackBase.java` (this port) | Full "Not ported" section (client rendering + standalone-equip blocker) — this report's findings 1 and 2 above are the direct continuation of this exact javadoc. |
| `items/gear/ArmorFSB.java` | `disablesPart` "consumed by the (Phase 5) player render layer" — same finding as `IArmorDisableModel` above (`ArmorFSB` is the one concrete implementer read in this port so far). |
| `items/gear/ArmorGasMask.java`, `ArmorHazmatMask.java` | Both already have a compiling `initializeClient` override with a body that returns `original` and a `// TODO(Phase 5)` comment naming the exact CE model class (`ModelGasMask`/shared `ModelM65` instance) that belongs there. |
| `items/gear/ArmorModel.java` | The full hook-point base class (finding 5) — `getGenericArmorModel`'s default returns `original`; `tickDecay` is a second, unrelated Phase-5-adjacent hook (CE's random mask-decay-and-drop, not a rendering concern, already no-op'd correctly and out of this area's scope). |
| `items/gear/ArmorHazmat.java`, `items/gear/SpecialArmorItems.java` | Cross-reference the same `renderHelmetOverlay`/`overlay_*.png` GL-blur mechanism named for Liquidator above — "not reproduced, Phase 5" — confirming this is a recurring, consistent gap across many sets, not a one-off. |
| `items/armor/JetpackBase.java` (CE-comment-mirroring text inside this port's `PoweredArmorItems`/`JetpackItems` registrar javadocs, indirectly) | Not a literal "Phase 5" string match but load-bearing: `JetpackItems.java`'s own javadoc confirms all 5 concrete jetpack items are already registered and waiting, "None of the 5 sets a vanilla durability bar" — i.e. registration is 100% done, this area's only remaining job for these 5 items is the `Model`/`modRender` bodies. |

## Confirmed real NeoForge 1.21.1 API shapes

All three mechanisms below are cited from a real, compiling call site (not invented), with the one
partial exception (`RenderPlayerEvent`'s exact nested-class shape) called out explicitly.

**Path A — per-item standalone armor model** (fully confirmed, already used 3× in this port's own
committed code):
```java
@Override
public void initializeClient(Consumer<IClientItemExtensions> consumer) {
    consumer.accept(new IClientItemExtensions() {
        @Override
        public Model getGenericArmorModel(LivingEntity living, ItemStack stack,
                                           EquipmentSlot slot, HumanoidModel<?> original) {
            // return a Model here — the vanilla model is `original`, not `this`
        }
    });
}
```
`Model` (`net.minecraft.client.model.Model`) is a plain abstract class with one required method,
`renderToBuffer(PoseStack, VertexConsumer, int packedLight, int packedOverlay, int color)` (confirmed
by neo-edition's `ModelArmorBase.renderToBuffer`, read in full). Neo-edition's `ModelArmorBase` (112
lines) is the confirmed real 1.21.1 shape to mirror: it caches `HumanoidModel<? extends LivingEntity>
model` + `EquipmentSlot slot`, exposes 8 `ModelRendererObj` fields (`head`/`body`/`leftArm`/`rightArm`/
`leftLeg`/`rightLeg`/`leftFoot`/`rightFoot` — note the CE-original 8, including the two extra
"foot" parts CE split off the legs for boot rendering), and a `getPropertiesFrom(HumanoidModel)` method
that copies rotation/visibility from the real vanilla model each frame — this is the direct successor
to CE's `ModelArmorBase.copyPropertiesFromBiped` (confirmed 1:1 intent match by reading CE's version in
full). `ModelNo9` (neo-edition, 63 lines) is the one complete, real, end-to-end concrete example: it
caches the replacement instance on the `IClientItemExtensions` object itself (`private ModelNo9
replacement;`), lazily constructs it once, and re-syncs pose every call
(`replacement.getPropertiesFrom(original); replacement.living = living;`) — this exact caching pattern
should be followed for every leaf armor `Model` this area writes, not a fresh allocation per frame.

**Path B — mod-slot insert render** (the general mechanism, `LivingEntityRenderer#addLayer`/
`EntityRenderersEvent.AddLayers`, is well-established public Minecraft/NeoForge API knowledge but has
**zero compiling example in either reference tree** — neither this port nor neo-edition has ever
registered a custom `RenderLayer` on any entity renderer; a whole-tree grep for `addLayer(` /
`AddLayers` / `RenderLayerParent` across neo-edition returned no matches at all). What **is** confirmed:
`net.neoforged.neoforge.client.event.RenderLivingEvent.Pre<? extends LivingEntity, ? extends
EntityModel<?>>` is real, resolves, and is actively exercised
(`NuclearTechModClient.java:630-653`, read in full): `event.getEntity()`, `event.getRenderer().
getModel()` (cast to `HumanoidModel<?>`), and direct mutation of `model.leftArmPose`/
`model.rightArmPose` (an enum field, `HumanoidModel.ArmPose`) all compile and run in the exact same
NeoForge build this port targets. **Recommendation for this area, given the confirmed-vs-unconfirmed
split**: build Path B as a `RenderLivingEvent.Pre<Player, ...>`-listener-based dispatcher (mirroring
CE's own `RenderPlayerEvent.Pre`-based `LayerArmorMod` design almost exactly, on a *confirmed* event
type) rather than committing to the textbook `addLayer`/`RenderLayer<T,M>` class-based pattern before
someone with real-jar access confirms its exact 1.21.1 shape (`RenderLayer`'s package, constructor, and
`render(PoseStack, MultiBufferSource, int, T, float,float,float,float,float,float)` signature are
well-established from public Minecraft modding knowledge but not demonstrated compiling anywhere this
sandbox can read) — see Open questions 2 for the exact decision point.

**Body-part hiding** (the `IArmorDisableModel` consumer): `net.neoforged.neoforge.client.event.
RenderPlayerEvent` is confirmed to **exist and resolve** at this NeoForge version (import compiles in
neo-edition's `ItemArmorMod.java:11`), matching CE's own `RenderPlayerEvent.Pre`/`.Post` mechanism
almost exactly by name — but, as with Path B, no call site anywhere in either reference tree exercises
its nested `.Pre`/`.Post` classes, so their exact constructor/accessor shape is not independently
confirmed. The fallback, `RenderLivingEvent.Pre`/`.Post`, **is** confirmed with a real call site (see
above) and is a strictly more general supertype-of-intent replacement (CE's own player-specific event
narrows what NeoForge's more general living-entity event already covers) — either should work
mechanically; `RenderPlayerEvent` is the closer 1:1 match to CE's own class name and almost certainly
still exists as a `Player`-specific specialization, but pin down its real shape against a jar before
committing (Open questions 1).

**Shared/already-confirmed-by-sibling-report infra this area should reuse, not reinvent:**
- `ModelRendererObj`/`IModelCustom`/`HFRWavefrontObject`(-VBO) — the OBJ parsing/per-part-render
  primitives every bucket-(a) leaf `Model` needs (`docs/phase5/renderer_framework_and_obj_models.md`
  is the owning report; this area only needs the *armor-specific* `ModelArmorBase` wrapper around it,
  confirmed above).
- `IClientItemExtensions.getArmPose(LivingEntity, InteractionHand, ItemStack)` and
  `applyForgeHandTransform(...)` — confirmed real by `docs/phase5/weapon_gun_rendering_animloader.md`
  (lines 174-190, read in full this pass) — the eventual landing spot for `IPAMelee`'s deferred
  first-person weapon-arm pose, once that gun-framework-owned mechanic is ready; not designed here.
- **No Mixin infrastructure exists in this port today** (confirmed by the sibling report, re-confirmed
  by this pass's own grep: no `.mixins.json`/`[[mixins]]` anywhere in this port). This is a **net
  positive finding for this specific area**: unlike first-person hand-render suppression (which needs
  Mixin per the sibling report), every mechanism this area needs — `getGenericArmorModel`,
  `RenderLivingEvent`/`RenderPlayerEvent`, `modRender`'s eventual layer — is a plain event/interface
  hook with no Mixin dependency, confirmed by CE's own design (CE itself used ordinary Forge events for
  all three, never a mixin-equivalent ASM hook) and by neo-edition's confirmed usage. This area can be
  built with zero new build-tooling investment.

## Phase-5-safe scope (buildable now, no external blocker)

1. **The `ModelArmorBase`-equivalent abstract class and its `ModelRendererObj`-per-part shape** — fully
   confirmed (Path A above), zero blocker. This is the single highest-leverage piece of work in this
   area: one abstract class unlocks every bucket-(a) leaf.
2. **Filling in `getGenericArmorModel` for the 3 already-stubbed classes** (`ArmorModel`, `ArmorGasMask`,
   `ArmorHazmatMask`) — the hook already compiles; only the `Model` body and its OBJ/box-mesh content
   are missing. No blocker beyond authoring the actual mesh/texture content (an asset-porting task, not
   an API question — CE's real PNG/OBJ assets already exist under `assets/hbm/`).
3. **Adding `getGenericArmorModel` overrides to the ~20 already-registered `PoweredArmorItems`/
   `JetpackItems` leaves** that don't have one yet (everything in the 28-file table above except the 3
   already stubbed) — same confirmed hook, same "author the `Model`, not the API" situation. All 66
   power-armor items + 5 jetpack items are already registered server-side (`PoweredArmorItems.java`,
   472 lines; `JetpackItems.java`, 75 lines — both read in full) and already carry every non-rendering
   mechanic CE specifies (potion effects, hazard class, rad-resist field, dash count, VATS/thermal-sight
   flags) — this area's job is purely additive, no risk of regressing already-correct server logic.
4. **`ArmorLiquidator`-shape sets (bucket (c), no custom model)** — confirmed to need *zero* work in
   this area at all; they already render correctly via the plain vanilla armor-texture layer today
   (nothing to build, just confirm no leaf in this bucket is mistakenly given a stub `Model` override
   it doesn't need).
5. **The `IArmorDisableModel` render-side consumer**, using the confirmed `RenderLivingEvent.Pre`/`.Post`
   fallback if `RenderPlayerEvent`'s exact shape isn't independently confirmed before implementation
   starts — either event gives this area everything CE's own `ModEventHandlerRenderer.
   onRenderPlayerPre/Post` needs (iterate 4 armor slots, check `instanceof IArmorDisableModel`, toggle
   the corresponding `ModelPart`). `ArmorFSB.disablesPart` is already ported and waiting
   (`items/gear/ArmorFSB.java`, confirmed implementer).
6. **All render-relevant item state is already a synced Data Component** — `ArmorDataComponents.java`
   (76 lines, read in full) confirms `ARMOR_CHARGE`/`JETPACK_FUEL`/`ARMOR_FUEL`/`JETPACK_GLIDER_TANK`
   are all already registered with `.networkSynchronized(...)`, meaning any render code this area
   writes (e.g. a charge-level-driven glow, or `ModelNo9`'s on/off lamp state via `TagsUtil`'s
   `CustomData`, confirmed pattern from neo-edition's own `ArmorNo9.inventoryTick`) can read live
   client-side item state with **zero new packet work** — this was already solved by Phase 3.
7. **`ItemModTesla`/`ItemModGasmask`-style mod-slot leaf renders** (once ported as items — they are not
   yet in this port at all, confirmed by directory listing; only `ItemModCharm`/`ItemModShield`/
   `ItemModV1` exist today) — not blocked by anything in this area once the Path B dispatcher exists;
   flagged as future content, not attempted here since the items themselves aren't registered yet.
8. **The jetpack particle trail's *logic* half** (spawn conditions, particle types/counts/lifetimes,
   which of the 3 lists gets which particle class) is a straightforward transcription of
   `JetpackHandler.java` lines 700-770 (already read in full by `docs/phase3/fsb_armor_and_jetpacks.md`,
   re-confirmed relevant here) once Phase 2's particle-engine equivalent (confirmed present in
   neo-edition, `com.hbm.particle.engine.ParticleEngineNT`) has a home in this port — the only real
   redesign needed is dropping CE's raw-GL-matrix-readback deferred-render trick (finding 4) in favor
   of passing the render layer's own `PoseStack` straight through, which is *simpler* than CE's
   original, not harder.

## Deferred / blocked scope (named blocker, not guessed)

1. **`JetpackGlider`'s worn "activate" opening animation** — blocked on CE's Collada skeletal-animation
   system, already flagged with no confirmed NeoForge 1.21 equivalent by
   `docs/phase5/renderer_framework_and_obj_models.md` (its own Blocked-scope item 1) and by
   `docs/phase3/weapon_animation_hooks.md`. This is a **project-wide decision** (port Collada as-is vs.
   adopt Sedna's JSON-keyframe system), not something this area's report can resolve alone — this
   report only reconfirms that the jetpack's *particle trail* (item 8 above) does not share this
   blocker and can proceed independently.
2. **Standalone jetpack chest-slot equip** (Path A's consumer for the 5 jetpack items specifically) —
   blocked on confirming NeoForge 1.21.1's `DataComponents.EQUIPPABLE`/`Equippable` builder API surface
   against a real jar (finding 2). Already named as an open blocker by this port's own committed
   `JetpackBase.java` javadoc before this research pass began; this report adds only that neo-edition
   has zero precedent either, so it cannot be resolved by reading more reference source — it needs
   either real-jar access or a documented best-effort implementation attempt with a fallback plan.
   **Not a blocker for anything else in this area** — the `Model` bodies, `LayerArmorMod`-equivalent,
   and `IArmorDisableModel` consumer are all independently buildable regardless of this question's
   outcome.
3. **`RenderPlayerEvent`'s and (if chosen) `RenderLayer<T,M>`'s exact 1.21.1 method shapes** — both
   confirmed to *exist* (by resolving imports / public API knowledge) but neither demonstrated by a
   compiling call site in either reference tree. Owner: whoever implements this area should verify
   against a real NeoForge 21.1.228 jar/javadoc before writing the final method signatures (see Open
   questions 1-2) — the design (which mechanism handles which of the three CE render paths) does not
   change based on the answer, only the exact code shape does.
4. **CE's own `no9` item (`ModItems.java:729`, `new ArmorNo9(MaterialRegistry.aMatSteel, 7,
   EntityEquipmentSlot.HEAD, "no9")`) does not exist in this port at all yet** — confirmed by grep: the
   only 3 hits for `no9`/`No9` in this port's whole source tree are javadoc citations of neo-edition's
   `ArmorNo9` (used as an API-shape reference, not the item itself) and an already-reserved advancement
   key (`AdvancementManager.achNo9`) with no backing item. This is a genuine item-content gap (the
   coal-mining lamp helmet with a `+0.5 DT`/black-lung-HUD-toggle mechanic, per neo-edition's real,
   ported `ArmorNo9.java`), not purely a rendering gap — it needs its own item-class pass (most likely
   belonging wherever the rest of `items.gear`'s small cosmetic/utility armor pieces are picked up)
   before this area's `no9`-specific `Model`/OBJ work has an item to attach to. Flagged so it isn't
   silently assumed already covered by the `achNo9` advancement placeholder.
5. **The exact OBJ-mesh-group-name to `ResourceManager` field mapping for `ArmorDesh`/`ArmorDiesel`**
   (Steamsuit/Dieselsuit) is not fully resolved by this pass — both leaf models reference distinct
   `ResourceManager.armor_steamsuit`/`armor_dieselsuit` fields, but only one matching file
   (`steamsuit.obj`) was found in the 20-file `assets/hbm/models/armor/` directory listing; no
   `dieselsuit.obj` appears. Either `dieselsuit` reuses `steamsuit.obj` under CE's own resource-loading
   indirection (plausible — Desh/Diesel are visually similar "fuel-tank" suits per
   `docs/phase3/fsb_armor_and_jetpacks.md`), or the file is named differently and this directory listing
   missed it. Flagged for whoever implements `ArmorDesh`/`ArmorDiesel`'s models to re-check CE's real
   `ResourceManager`/model-loading call sites directly rather than assuming either answer.
6. **`ItemArmorMod#getModifiers`'s dynamic-attribute-application consumer does not exist anywhere in
   this port yet** (confirmed: `ItemModV1.java`'s own javadoc states "nothing yet calls
   `ItemArmorMod#getModifiers(ItemStack)` anywhere... a pre-existing, not-yet-consumed forward
   reference"). This is adjacent to but not part of this area's rendering scope — noted because a
   render-layer implementer might otherwise assume `getModifiers` is already wired and skip it, when
   in fact it is a separate, still-open gap belonging to whichever package owns the general armor-mod
   attribute-application system.
7. **`IPAMelee`/`IPARanged`'s first-person weapon-arm rendering** (`setupFirstPerson`/
   `renderFirstPerson`/`playAnim`/`orchestra` in CE) is explicitly out of this area — it belongs to the
   gun/weapon-arm rendering area (`docs/phase5/weapon_gun_rendering_animloader.md`'s confirmed
   `getArmPose`/`applyForgeHandTransform` hooks are the landing spot). Reconfirmed here only because
   this task's brief mentions `IPAMelee`/`IPARanged` by name — the actual rendering work for them is a
   sibling area's job, not a silent gap.

## Key design/API decisions

- **Three CE render mechanisms map onto three different, all-real NeoForge hooks — do not try to
  collapse them into one.** Path A (`IClientItemExtensions#getGenericArmorModel`, fully confirmed) for
  any item that is itself worn as armor. Path B (a `RenderLivingEvent.Pre<Player,...>`-based dispatcher,
  confirmed-event/unconfirmed-`addLayer`-alternative) for anything rendered from a mod-slot insert,
  including a jetpack installed into someone else's chestplate. A third listener (same confirmed
  `RenderLivingEvent.Pre`/`.Post`, or the less-confirmed `RenderPlayerEvent`) for `IArmorDisableModel`'s
  body-part hiding. This mirrors CE's own three-mechanism design exactly (`getArmorModel` /
  `LayerRenderer.addLayer` / `RenderPlayerEvent.Pre`+`.Post`) — this is a faithful port of CE's actual
  architecture, not an invented simplification, and CE's own code is the reason to keep them separate
  (e.g. a jetpack worn standalone needs Path A; the same jetpack installed as a mod chip needs Path B;
  neither needs the other).
- **One `LayerArmorMod`-equivalent dispatcher, not one per mod-slot item.** CE's own `LayerArmorMod`
  (52 lines) is generic: it doesn't know about jetpacks specifically, it just calls `modRender` on
  whatever `ItemArmorMod` occupies a mod slot (plus a special-cased standalone-jetpack branch, "because
  armor that isn't `ItemArmor` doesn't render at all" — CE's own comment, confirming CE observed that
  1.12 Forge's armor-model hook does not fire for non-`ItemArmor` items even when worn in an armor slot,
  the same underlying constraint the port's `JetpackBase.java` "standalone equip" open question echoes).
  This port should build one dispatcher, add `modRender`-equivalent as an actual method on the
  port's `ItemArmorMod` (it does not exist yet — confirmed, `items/armor/ItemArmorMod.java`'s current
  100 lines have no render hook of any kind), and let leaf classes (jetpacks now, `ItemModTesla`/
  `ItemModGasmask`/etc. later) override it.
- **Cache the replacement `Model` instance per `IClientItemExtensions` object, not per frame.**
  Confirmed pattern from neo-edition's `ArmorNo9`/CE's own `JetpackBase.getArmorModel`
  (`if (model == null) this.model = new ModelX();`) — both cache a single instance and re-sync pose
  data (`getPropertiesFrom`) every call rather than allocating fresh.
- **The `ModelArmorBase`'s 8-part shape (`head`/`body`/`leftArm`/`rightArm`/`leftLeg`/`rightLeg`/
  `leftFoot`/`rightFoot`) should be ported as-is**, including the CE-original split of legs vs. feet
  into two separate renderable groups (confirmed present in both CE's and neo-edition's versions of
  this exact class) — this is deliberate CE content-authoring (boots textured/posed independently from
  the leg mesh), not simplifiable without a visual behavior change.
- **`renderHelmetOverlay`-style motion-blur/durability-scaled GL overlays (Liquidator, gas masks,
  hazmat masks, asbestos) have no confirmed 1.21 client-item-extension equivalent** in either reference
  tree, and this report did not find one — every already-committed leaf's javadoc that mentions this
  mechanism (`ArmorGasMask`, `ArmorHazmat`, `SpecialArmorItems`) already correctly stops short of
  inventing a replacement and instead notes the mask's own durability bar still works via vanilla's
  ordinary damage-bar rendering (no mechanical loss, only the cosmetic fog-up effect is missing). This
  area should continue that pattern — do not invent a full-screen-overlay API that isn't confirmed real.
- **Explosion/block-removal batching** (this task's ground rules ask every report to check): does not
  apply to this area. Nothing read in this pass — `ModelArmorBase`/leaf armor models, `LayerArmorMod`,
  `ModEventHandlerRenderer`, `JetpackHandler`'s render section, `ItemArmorMod` — reads, writes, or
  iterates world blocks in bulk. Noted per the ground rules rather than silently omitted.

## Key risks

- **The single biggest unresolved risk is Path B's exact class shape** (`RenderLayer<T,M>` vs. a plain
  `RenderLivingEvent`-based dispatcher) — this report recommends the confirmed-event-based design over
  the unconfirmed textbook `addLayer` pattern specifically *because* neither this port nor neo-edition
  has ever exercised `addLayer`/`RenderLayerParent`/`EntityRenderersEvent.AddLayers` even once, despite
  both being large, actively-developed codebases with hundreds of other confirmed NeoForge API uses —
  that absence, across two independent codebases, is itself a signal worth taking seriously rather than
  assuming the textbook pattern "obviously still works the same way."
- **The Collada blocker (item 1, Deferred scope) affects only the jetpack glider's own worn-model
  animation, not the majority of this area's work** — a risk of scope confusion, not of actual
  blockage: someone skimming "jetpack rendering is blocked on Collada" could wrongly conclude the
  entire jetpack particle trail and the other 4 jetpack variants' standalone/mod-slot renders are also
  blocked, when only `JetpackGlider`'s specific animated-open sequence is.
- **The `Equippable` open question (item 2, Deferred scope) is shared with at least one other Phase 3
  package** (this port's own `JetpackBase.java` already names it) — resolving it once, centrally, is
  better than each area that touches jetpacks re-discovering the same gap independently, which is
  exactly the failure mode this report is trying to avoid by naming it explicitly here too.
- **No real-jar verification was possible for any claim in the "Confirmed... but not by a compiling
  call site" category** (`RenderPlayerEvent`'s nested classes, `RenderLayer<T,M>`'s exact shape,
  `ModelPart.visible`'s exact name) — this is a sandbox constraint stated up front in this task's own
  ground rules, not an oversight; flagged again here because this area has more such gaps than the two
  sibling Phase 5 reports already written (both of which found fuller end-to-end confirmed examples for
  their own core mechanisms).

## Open questions

1. **What is `RenderPlayerEvent`'s exact 1.21.1 shape** (does it still have `.Pre`/`.Post` nested
   classes; what accessors do they expose — `getEntityPlayer()`/`getRenderer()`/`getPartialTick()`
   equivalents)? Confirmed to exist as an importable class in NeoForge 21.1.228; not confirmed beyond
   that. Recommend checking against a real jar/javadoc before implementation; the confirmed
   `RenderLivingEvent.Pre`/`.Post` fallback works mechanically either way.
2. **Does NeoForge 1.21.1 still expose `LivingEntityRenderer#addLayer`/`RenderLayerParent` and a
   `RenderLayer<T extends LivingEntity, M extends EntityModel<T>>` base class in the shape widely
   documented for modern Minecraft versions?** This report could not find a single compiling example in
   either reference tree despite both being substantial codebases — either (a) neither project has
   needed a custom render layer yet (plausible — coincidence, not evidence of removal) or (b) something
   about the API shape changed enough that neither project attempted it. Recommend the implementer
   check this specifically before committing to the layer-class design over the event-listener design
   recommended above.
3. **Is the CE `no9` item (finding/Deferred-scope item 4) intended to be picked up by this area, or by
   whoever owns the rest of `items.gear`'s small cosmetic pieces?** This report flags its absence but
   does not claim it — recommend explicit ownership assignment rather than either area assuming the
   other covers it.
4. **`ArmorDesh`/`ArmorDiesel`'s exact OBJ file** (Deferred-scope item 5) — needs a direct check against
   CE's real `ResourceManager`/`NTMClientRegistry` model-loading call sites, not resolved by this pass's
   directory listing alone.
5. **Should this area's `LayerArmorMod`-equivalent also become the confirmed home for
   `ItemArmorMod#getModifiers`'s dynamic-attribute application** (Deferred-scope item 6), since both are
   currently-unconsumed forward references on the same base class? This report flags the coincidence but
   does not decide it — attribute application is a gameplay/stat concern, not inherently a rendering
   one, and could equally belong to a non-Phase-5 package.
