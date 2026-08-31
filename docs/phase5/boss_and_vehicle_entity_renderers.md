# Boss & vehicle entity renderers (Phase 5)

**Area:** `boss_and_vehicle_entity_renderers` — CE's real `com.hbm.render.entity.*` renderer classes for
every Phase-4-built boss/vehicle/vehicle-adjacent entity that currently has zero client-side rendering
in this port, what model technology each one actually uses, and what's genuinely hard to port.

## Sources read

**CE (`upstream/hbm-ce`), in full unless noted:**
- `com/hbm/render/entity/{RenderWormHead,RenderWormBody,RenderHunterChopper,RenderCyberCrab,
  RenderBlackHole}.java` (51+50+56+29+460 = 646 lines)
- `com/hbm/render/entity/mob/{RenderMaskMan,RenderUFO,RenderQuacc,RenderTaintCrab,RenderTeslaCrab,
  RenderRADBeast}.java` (32+82+53+54+54+80 = 355 lines)
- `com/hbm/render/entity/train/{RenderTrainCargoTram,RenderTrainCargoTramTrailer}.java` (65+96 = 161
  lines)
- `com/hbm/render/entity/item/{RenderC130,RenderCartCrate,RenderNeoCart,RenderDeliveryDrone,
  RenderParachuteCrate,RenderBoatRubber}.java` (75+19+104+59+60+28 = 345 lines)
- `com/hbm/render/entity/RenderBomber.java` (85 lines) and `com/hbm/render/entity/effect/
  RenderQuasar.java` (76 lines)
- `com/hbm/render/model/{ModelMaskMan,ModelTaintCrab}.java` (78 read ~40, 36 full) and
  `ModelHunterChopper.java`/`ModelCrab.java`/`ModelM65Blaze.java`/`ModelWormHead.java` (492/172/150/19
  lines — header + `grep`-confirmed field/import shape, not full line-by-line, sufficient to classify
  "vanilla `ModelRenderer` cuboids" vs. "OBJ `renderPart()` wrapper," which is this report's only use
  for them)
- `com/hbm/render/misc/BeamPronter.java` (166 lines, header read — the tether-beam helper shared by
  `RenderUFO`/`RenderTaintCrab`/`RenderTeslaCrab`/`RenderRADBeast`)
- `com/hbm/entity/cart/EntityMinecartCrate.java` (grepped for a display-tile/custom-render override —
  none found, confirms `RenderCartCrate`'s bare vanilla-passthrough is CE's real, intentional behavior,
  not an oversight this survey should "fix")
- Directory listing of `com/hbm/render/entity/**` (full, 128 files) to confirm no renderer in this
  report's scope was missed
- `docs/phase4/{entities_bosses.md,entities_vehicles_aircraft.md,entities_vortex_gravity_wells.md}`
  (all 3 read in full — the entity-behavior reports this one is the rendering supplement to; not
  re-derived, cited by section throughout)
- `docs/phase5/{renderer_framework_and_obj_models.md,weapon_gun_rendering_animloader.md}` (both read
  in full — the sibling Phase 5 reports this one's model-technology classification and NeoForge
  API-shape claims build directly on, per this task's own instruction to search for named forward
  references rather than re-derive them)

**This port's own `src/main/java` (already-committed, already-compiling Phase 4 code), read in full or
targeted:**
- `com/hbm/main/ClientProxy.java` (44 lines, full — confirms **zero** renderer registration exists
  anywhere in this port today, matching `renderer_framework_and_obj_models.md`'s own finding, and
  confirms the exact `ServerProxy`/`ClientProxy` override-method shape Neo Edition's
  `registerEntityRenderers()` pattern should be added to)
- `com/hbm/entity/mob/{WormEntityTypes,Phase4BossEntityTypes2,MaskmanEntityTypes,
  RadBeastEntityTypes}.java`, `com/hbm/entity/train/TrainEntityTypes.java`, `com/hbm/entity/cart/
  CartEntityTypes.java`, `com/hbm/entity/effect/GravityWellEntityTypes.java`, `com/hbm/entity/logic/
  PlaneEntityTypes.java`, `com/hbm/entity/item/{DroneEntityTypes,ParachuteCrateEntityTypes}.java` (all
  9 files, full — the already-registered `EntityType`/`DeferredHolder` handles every renderer in this
  report needs to target; see Headline finding #1)
- `com/hbm/entity/mob/EntityUFO.java` (grepped for `beam`/`SynchedEntityData` — confirms Headline
  finding #5), `com/hbm/entity/mob/EntityHunterChopper.java` (grepped for `isDying`),
  `com/hbm/entity/mob/EntityBOTPrimeBody.java` (grepped for `DATA_SHIELD`), `com/hbm/entity/effect/
  EntityBlackHole.java` (grepped for the `SIZE` accessor), `com/hbm/entity/mob/{EntityQuackos,
  EntityDuck}.java` (grepped for `extends`), `com/hbm/entity/cart/EntityMinecartNTM.java` (header read
  — confirms `extends AbstractMinecart`, Headline finding #6), `com/hbm/entity/train/
  {EntityRailCarBase,EntityRailCarCargo,EntityRailCarRidable}.java` (grepped for `renderX`/`renderY`/
  `renderZ`/`OCCUPIED_SLOTS`/`SeatDummyEntity` — confirms the exact fields CE's train renderers need
  already exist, field-for-field)
- Whole-tree check: zero `.obj`/entity-texture assets exist anywhere under `src/main/resources` for any
  entity in this report's scope (confirmed by `find`) — a second, asset-level blocker layered on top of
  the code-level OBJ-pipeline gap for the entities that need one.

**Neo Edition (`upstream/neo-edition`), used strictly for confirmed NeoForge 1.21.1 API shape, never for
behavior, per this task's ground rules:**
- `com/hbm/render/entity/mob/{DuckRenderer,CreeperNuclearRenderer}.java` (21+37 lines, full) — real,
  compiling `ChickenRenderer`/`CreeperRenderer` subclasses; the closest living analogue to this report's
  `EntityQuackos`/`EntityDuck` renderers
- `com/hbm/render/entity/effect/{RenderBlackHole,RenderBomber}.java` (396+80 lines, full) — real,
  compiling ports of two of this report's own CE classes, used to confirm the exact
  `PoseStack`/`VertexConsumer`/`RenderType` translation (Headline finding #4)
- `com/hbm/render/entity/EmptyEntityRenderer.java` (18 lines, full) — the confirmed "this entity has no
  visual, but NeoForge still requires a registered renderer" pattern
- `com/hbm/render/entity/rocket/RenderMissileGeneric.java` (56 lines, full) and `com/hbm/render/util/
  RenderContext.java` (61 lines, full) — confirms the `ThreadLocal<PoseStack>`-wrapping shim Neo Edition
  uses to translate CE's `GlStateManager.translate/rotate/scale` call sequences almost mechanically
- `com/hbm/render/util/BeamPronter.java` (60 of ~150 lines read) — confirms CE's tether-beam helper has
  already been ported to a real `RenderType`/`VertexConsumer` shape
- `com/hbm/entity/{NtmEntityTypes,mob/CreeperNuclear}.java` (registration lines / header, grepped) and
  `com/hbm/main/ClientProxy.java` (grepped for `EntityRenderers.register`/`registerEntityRenderers`) —
  confirms the exact call-site pattern (`ClientProxy.registerEntityRenderers()`, invoked from
  `FMLClientSetupEvent.enqueueWork`, holding a flat list of `EntityRenderers.register(TYPE.get(),
  Factory::new)` calls) this port's own `ClientProxy` should extend

**File count this report is based on:** 24 CE files read in full + 5 read partially/header-only, 9 of
this port's own already-committed files read in full + 6 grepped, 3 Phase 4 predecessor reports read in
full, 2 Phase 5 sibling reports read in full, 8 Neo Edition files read in full/substantially.

## Headline findings

1. **Every entity in this report's scope already exists server-side in this port with a registered,
   working `EntityType` — the *only* missing piece anywhere in this report is the client `Render`/
   `EntityRenderer` class and its registration call.** This is a materially better starting position
   than the task's own framing ("Phase 4 built a large roster of custom entities with zero client-side
   rendering") suggested by itself — it's not just that rendering is missing, it's that *nothing else*
   is missing. Confirmed by reading all 9 of this port's `*EntityTypes.java` registration files in full:
   `WormEntityTypes.{BOTPRIME_HEAD,BOTPRIME_BODY}`, `MaskmanEntityTypes.MASK_MAN`,
   `Phase4BossEntityTypes2.{UFO,HUNTER_CHOPPER,CYBER_CRAB,TAINT_CRAB,TESLA_CRAB,DUCK,QUACKOS}`,
   `RadBeastEntityTypes.RAD_BEAST`, `TrainEntityTypes.{BOUNDING_DUMMY,TRAIN_SEAT,CARGO_TRAM,
   CARGO_TRAM_TRAILER}`, `CartEntityTypes.{CART_ORE,CART_POWDER,CART_SEMTEX,CART_CRATE,
   CART_DESTROYER}`, `PlaneEntityTypes.{C130,BOMBER}`, `DroneEntityTypes.{DELIVERY_DRONE,
   REQUEST_DRONE}`, `ParachuteCrateEntityTypes.PARACHUTE_CRATE`, `GravityWellEntityTypes.{BLACK_HOLE,
   VORTEX,RAGING_VORTEX,QUASAR}` — **29 already-registered `DeferredHolder<EntityType<?>, EntityType<X>>`
   handles** this report's renderers can target directly, by name, today. The one exception is
   `EntityBoatRubber`, which does not exist anywhere in this port yet (confirmed by `find`) — see the
   dedicated row below.
2. **Because NeoForge requires a registered renderer for every `EntityType` before that entity can ever
   be added to the client render dispatch, this gap is not merely cosmetic — it is a standing crash
   risk against every one of Phase 4's own already-shipped mechanics.** Confirmed by this port's own
   `ClientProxy.java` (44 lines, read in full): it has no `registerEntityRenderers()` override at all,
   and `NuclearTechMod`/`ServerProxy`'s own default is a no-op. The instant a MaskMan/UFO/worm/cart/
   drone/gravity-well entity is spawned while any client is loaded nearby — including from this port's
   own already-wired `BossSpawnHandler`/`ItemChopper`/`ItemDrop` spawn paths, which are themselves
   Phase-4-complete per `docs/phase4/entities_bosses.md`/`entities_vortex_gravity_wells.md` — the client
   throws (well-established `EntityRenderDispatcher` behavior across every Forge/NeoForge version; not
   verified against a real launched client in this sandbox, but not a guess either — it is the
   documented consequence of an unregistered `EntityType` reaching `EntityRenderDispatcher.render`).
   This means even a trivial placeholder renderer (an `EmptyEntityRenderer`-shaped stub, see Headline
   finding #3) for every one of the 29 types above is a **correctness requirement for safe Phase 4
   playtesting**, not a "nice-to-have visual polish" backlog item — prioritize registering *something*
   for all 29 before investing further effort into any single entity's visual fidelity.
3. **CE uses two unrelated model technologies across this report's roster, and which one an entity uses
   does *not* track "boss" vs. "vehicle" the way a surface reading suggests.** Two of this report's most
   visually prominent entities — **Hunter Chopper** (`ModelHunterChopper.java`, 492 lines) and **RAD
   Beast**'s mask overlay (`ModelM65Blaze.java`, 150 lines, `extends ModelBiped`) — are plain
   vanilla-style box-cuboid `ModelRenderer` rigs with **zero OBJ dependency**, confirmed by reading their
   field declarations directly (dozens of `ModelRenderer` fields, no `ResourceManager`/`renderPart`
   calls anywhere in either file). These port today via a straightforward `EntityModel<T>`/`ModelPart`
   translation, the same well-trodden path as any vanilla mob model — no blocker beyond copying the box
   dimensions and the one texture PNG each needs. By contrast, **MaskMan**, the **worm** (head+body),
   the **UFO**, **TaintCrab/TeslaCrab**, both **train cars**, both **scripted aircraft**, 4 of the 5
   **minecarts**, and the **delivery/request drones** all render through CE's custom OBJ pipeline
   (`HFRWavefrontObject`/`ResourceManager.xxx.renderPart("PartName")`), which — per
   `docs/phase5/renderer_framework_and_obj_models.md`'s own already-confirmed survey — has **zero**
   corresponding code anywhere in this port today. Do not assume an entity's category predicts which
   bucket it falls in; the per-entity tables below classify each one individually.
4. **The gravity-well family's renderer is *not* actually blocked on the OBJ pipeline, despite CE
   itself loading a real `.obj` file (`Sphere.obj`) for it — Neo Edition already proves the fix, and it
   is a real, compiling, line-for-line-traceable translation of CE's own math, not a guess.**
   `upstream/neo-edition/.../render/entity/effect/RenderBlackHole.java` (396 lines, read in full)
   replaces the OBJ sphere mesh with a **procedurally-generated UV-sphere triangle mesh** built directly
   against `Tesselator.begin(TRIANGLES, POSITION_COLOR)` at render time (`renderSphere`, 16×16
   stacks/slices, `Math.sin`/`Math.cos` spherical-coordinate generation) — a small, sensible
   simplification (a sphere is trivial to generate in code; porting the entire OBJ loader just for one
   mesh would be substantial overkill) this port should copy rather than wait on. The much larger
   remainder of both CE's and Neo Edition's renderer code — the "accretion disc," "swirl," and "jets"
   effects that make up the actual visual identity of a black hole/vortex — was **already** pure
   procedural immediate-mode geometry in CE (never OBJ), and Neo Edition's version is close to a
   line-for-line mechanical translation of CE's exact trigonometry and color-ramp math into
   `PoseStack.mulPose(Axis.YP.rotationDegrees(...))`/`VertexConsumer.addVertex(matrix,
   x,y,z).setUv(...).setColor(...)` calls, confirmed by direct side-by-side comparison of both files.
   This makes the gravity-well family the single most fully-specified, lowest-risk renderer in this
   entire report — every number, blend mode, and rotation is confirmed from real, compiling code on
   both sides — and it should not be gated behind the OBJ-pipeline work the way MaskMan/worm/UFO/trains/
   aircraft/drones genuinely are.
5. **At least one entity's client-visible state needed by CE's own renderer is missing from this port's
   server-side implementation today, confirmed by direct read, not inferred.** CE's `RenderUFO.doRender`
   reads `ufo.getBeam()` every frame to decide whether to draw the three nested abduction-beam segments
   (Headline finding in the source table below). This port's own `EntityUFO.java` has the underlying
   mechanic fully ported (`private int beamTimer; private boolean beam;`, driving the real 1000-damage/
   radiation abduction logic per `docs/phase4/entities_bosses.md`) — but `beam` is a **plain private
   field with no getter and no `SynchedEntityData`/`EntityDataAccessor` registration anywhere in the
   class** (confirmed: zero `defineSynchedData` override exists in `EntityUFO.java` at all). The beam
   visual cannot be written correctly until this becomes a real synced boolean with a public accessor —
   a small, easy, but genuinely non-optional server-side follow-up this report surfaces for whoever
   implements the UFO renderer, exactly the same shape of gap `renderer_framework_and_obj_models.md`
   already found for `MachineLargeTurbineBlockEntity.lastRotor`.
6. **This port's minecart family made a real, deliberate architecture change from CE that changes the
   rendering question but does not resolve it.** `EntityMinecartNTM extends AbstractMinecart` (confirmed
   by direct read) — CE's own `EntityMinecartNTM extends net.minecraft.entity.item.EntityMinecart`, the
   1.12 equivalent, so this is not itself a deviation. What *is* worth flagging: CE renders 4 of its 5
   concrete minecarts (Ore/Powder/Semtex/Destroyer) through a **fully custom, non-vanilla**
   `Render<EntityMinecartNTM>` (`RenderNeoCart`, 104 lines) with its own OBJ "Carriage"/"Bucket" rig plus
   a per-subclass texture-swap overlay ("Powder"/"SemtexTop"+"SemtexSide"/full-retexture-for-Destroyer)
   — none of which is vanilla's built-in minecart-plus-carried-block display mechanism. Only the 5th,
   `EntityMinecartCrate`, is genuinely renderable with zero custom code: CE's own `RenderCartCrate` is a
   bare `extends RenderMinecart<EntityMinecartCrate>` with **no body override at all** (confirmed by
   reading both the renderer, 19 lines, and the entity class, grepped for any display-tile override —
   none exists), meaning CE itself renders this one cart as an undecorated stock minecart. This port's
   choice to build on `AbstractMinecart` means `EntityMinecartCrate` can use vanilla's stock
   `MinecartRenderer<T>` directly and be visually CE-faithful with zero new code — but the other 4
   still need the same OBJ-rig work as everything else in the OBJ bucket to match CE's real look; simply
   registering vanilla's `MinecartRenderer` for all 5 (the fastest way to stop the crash from Headline
   finding #2) would under-deliver on 4 of them relative to CE, not just look "simpler."

## Method / API-shape cross-check (all confirmed via Neo Edition's real, compiling 1.21.1 code)

- **Registration**: `net.minecraft.client.renderer.entity.EntityRenderers.register(EntityType<T>,
  EntityRendererProvider<T>)`, called from inside a `ClientProxy.registerEntityRenderers()`-style method
  invoked from `FMLClientSetupEvent.enqueueWork` — confirmed identical in shape to the already-confirmed
  `BlockEntityRenderers.register` pattern `renderer_framework_and_obj_models.md` established for block
  entities, and directly matching this port's own existing `ServerProxy`/`ClientProxy` split (just needs
  the one new override method added).
- **Renderer base classes**: `net.minecraft.client.renderer.entity.EntityRenderer<T>` (one method,
  `render(T, float entityYaw, float partialTick, PoseStack, MultiBufferSource, int packedLight)`) for
  anything with fully custom geometry (worm, UFO, chopper, trains, aircraft, gravity wells, custom
  minecarts, drones, parachute crate) — confirmed by every Neo Edition file read above. For the two
  entities that are thin reskins of a vanilla mob (`EntityDuck`/`EntityQuackos` → vanilla `Chicken`),
  `net.minecraft.client.renderer.entity.ChickenRenderer` is the correct base to extend (confirmed real
  and already used, `DuckRenderer.java`), overriding only `getTextureLocation` (and, for Quackos, the
  25× scale — see its own row). For `EntityCyberCrab`/`RAD Beast` (vanilla-box models on custom mob
  bodies), the correct base is `net.minecraft.client.renderer.entity.MobRenderer<T, M extends
  EntityModel<T>>` (the modern successor to CE's `RenderLiving<T>`) — not independently confirmed by a
  Neo Edition file in this survey (no equivalent custom-box-model mob renderer exists in that repo to
  cross-check), but well-established, unchanged-in-shape vanilla API across versions; flagged as
  **NOT independently jar-verified** per this task's ground rules.
- **Entities with no independent visual at all** (this report's two dummy-rider entities,
  `EntityRailCarBase.BoundingBoxDummyEntity` and `EntityRailCarRidable.SeatDummyEntity`) should register
  Neo Edition's exact confirmed pattern: `EmptyEntityRenderer<T extends Entity>` (18 lines, `render()`
  is a no-op, `getTextureLocation()` returns `null`) — a real, compiling, already-used-elsewhere-in-Neo-
  Edition (for CE's own invisible EMP/nuke-payload marker entities) pattern for exactly this situation.
- **Old-style `GlStateManager.translate/rotate/scale` + `renderPart(String)` sequences translate almost
  mechanically**: Neo Edition's `com.hbm.render.util.RenderContext` (a `ThreadLocal`-held `PoseStack`
  wrapper, 61 lines, read in full) exposes `translate`/`scale`/`mulPose` static methods with the same
  call shape CE's `GlStateManager` had, letting `ResourceManager.xxx.renderPart("PartName")`-style OBJ
  calls (once the OBJ pipeline itself exists) sit inside what is otherwise a near-verbatim port of CE's
  own transform sequence — confirmed directly against `RenderMissileGeneric.java`'s and (effect)
  `RenderBomber.java`'s real, compiling bodies, both of which are structurally identical to this report's
  own `RenderC130`/`RenderBomber`/`RenderHunterChopper`/`RenderWormHead`/`RenderWormBody` CE sources.
- **Procedural immediate-mode quads/triangles** (the black-hole disc/swirl/jets, `BeamPronter`'s tether
  beams) translate to `Tesselator.getInstance().begin(VertexFormat.Mode.X, DefaultVertexFormat.Y)` →
  `BufferBuilder.addVertex(matrix, x, y, z).setUv(u,v).setColor(...)` → `BufferUploader.drawWithShader
  (buffer.buildOrThrow())`, with `RenderSystem.setShader(GameRenderer::getPositionTexColorShader)` (or
  `getPositionColorShader` for untextured quads) selecting the fixed-function-equivalent shader —
  confirmed directly from Neo Edition's `RenderBlackHole.java` (`renderDisc`/`renderSwirl`/`renderJets`)
  and `render/util/BeamPronter.java`, both real and apparently-compiling.
- **`entity.ticksExisted`/`entity.getEntityId()` → `entity.tickCount`/`entity.getId()`** (renamed fields,
  confirmed by direct 1:1 comparison of every CE/Neo-Edition pair read above) — a mechanical rename with
  no behavior change, needed by `RenderBlackHole`'s per-instance rotation-offset math (`entity.getId() %
  90 - 45`, preserved verbatim by Neo Edition) and `RenderUFO`'s beam-cycle animation.

## Per-entity/family survey

### A — The BOTPrime worm (head + body)

| CE class | Lines | Model tech | Live per-frame data | This port's `EntityType` |
|---|---|---|---|---|
| `RenderWormHead` | 51, full | OBJ (`models/mobs/bot_prime_head.obj` via `HFRWavefrontObject.renderAll()`) — single static mesh, no named-part transforms | Just `rotationYaw`/`rotationPitch` interpolated against `prevRotation{Yaw,Pitch}` (both vanilla `Entity` fields, always present, no port-side gap) | `WormEntityTypes.BOTPRIME_HEAD` |
| `RenderWormBody` | 50, full | Same OBJ tech, separate mesh (`bot_prime_body.obj`), same single-`renderAll()` shape | Same rotation-only need | `WormEntityTypes.BOTPRIME_BODY` |

Both are about as simple as a custom OBJ renderer gets — a translate, two rotates, one `renderAll()`
call, one texture bind, `GL_SMOOTH` shading with culling briefly disabled (head only — CE's own code,
not something to "fix" for consistency). The only genuinely hard part of this pair is not the renderer
itself but the **75-simultaneous-instance load** already flagged as an open risk by
`docs/phase4/entities_bosses.md` (74 body segments + 1 head, each independently ticking/rendering) — not
a rendering-technique problem, a rendering-*volume* problem, worth a real playtest once the OBJ pipeline
and this renderer both exist. `ModelWormHead.java` (a separate, `ModelBase`-extending class, 19 lines,
confirmed to exist by grep) is **dead code in CE itself** — `RenderWormHead` never references it; do not
port it. Blocked on: the OBJ pipeline (`docs/phase5/renderer_framework_and_obj_models.md`) + the two
`.obj` assets + one texture PNG each, none of which exist in this port yet.

### B — MaskMan (purple boss bar)

`RenderMaskMan` (32 lines, `extends RenderLiving<EntityMaskMan>`) is a thin wrapper — all the real
work is in `ModelMaskMan` (78 lines, `extends ModelBase`, ~40 lines read), whose `render()` override
does its own `GlStateManager` push/rotate dance and then calls `ResourceManager.maskman.renderPart
("Torso")` (an OBJ part-render, not a box-cuboid draw) plus at least one nested child part not read in
full here (the model's own comments show a torso-swing calculation feeding a nested nested
`pushMatrix`/child-part sequence, consistent with a multi-part rigged OBJ humanoid, not a simple single-
mesh renderAll). **Model tech: OBJ, not vanilla box-cuboid** — despite `ModelMaskMan extends ModelBase`,
the same shape used by classic vanilla box models, its actual body is a multi-part `renderPart(...)`
call sequence. Live per-frame data: limb-swing-derived torso lean (computed from the same
`limbSwing`/`limbSwingAmount` parameters every `ModelBase.render()` call already receives — no
port-side gap) plus rotation. `EntityType`: `MaskmanEntityTypes.MASK_MAN`. Blocked on the OBJ pipeline +
`models/mobs/maskman.obj`-family assets + `ResourceManager.maskman_tex`, none ported yet.

### C — UFO (red boss bar)

`RenderUFO` (82 lines, `extends Render<EntityUFO>`) is the most mechanically interesting renderer in
this report short of the black hole: a spinning-saucer OBJ body (`ResourceManager.ufo.renderAll()`,
continuous Y-axis spin at a flat `5°/tick`, independent of the entity's own rotation — the saucer always
spins regardless of which way it's facing/flying) plus, conditionally, the **abduction beam**: a downward
raycast to the first solid block below the UFO, then three nested calls into `BeamPronter.prontBeam`
(one thick solid spiral core, two thinner faster-spinning random-jitter outer layers) forming the
visual "tractor beam" column. Model tech: OBJ for the saucer body; the beam itself is pure procedural
geometry via the shared `BeamPronter` helper (already confirmed ported to a real `RenderType`/
`VertexConsumer` shape in Neo Edition — see Method section). Live per-frame data: `ufo.getBeam()`
(**currently missing from this port's server-side `EntityUFO` — see Headline finding #5, a real
blocker**), `ufo.posY` plus a downward block scan for beam length (client can do this scan itself against
its own loaded chunk data — no new server sync needed for that part), `ufo.isEntityAlive()`/`deathTime`
for the death-tumble tilt. `EntityType`: `Phase4BossEntityTypes2.UFO`. Blocked on: the OBJ pipeline for
the saucer body + the beam-flag sync fix (small, independent, and worth doing regardless of OBJ-pipeline
timing since it's a one-line server change) + `models/mobs/ufo.obj` + `ResourceManager.ufo_tex`.

### D — Hunter Chopper (purple boss bar, boss-tier hostile)

**Correction to a surface assumption**: `RenderHunterChopper` (56 lines, `extends Render<
EntityHunterChopper>`) delegates its body entirely to `ModelHunterChopper` (492 lines) — and that model
is a **plain vanilla box-cuboid rig** (dozens of `ModelRenderer` fields — `RotorPivotStem`,
`TorsoBaseCenter`, `WingLeftPlate`, `TailRotorBlades`, etc. — confirmed by header/field-declaration read),
**not** an OBJ mesh, despite CE's own commented-out reference to an alternate `ProtoCopter` renderer in
the same file (`//mine = new ProtoCopter();`, dead code, never instantiated — confirmed by the live
`mine2 = new ModelHunterChopper()` line actually used). This makes Hunter Chopper's body **fully
portable today via a straightforward `EntityModel<T>`/`ModelPart` translation, no OBJ blocker at all** —
a genuinely valuable correction, since this is one of the most visually prominent entities in the whole
report and the task's own framing groups it with the OBJ-heavy vehicles. Live per-frame data: rotation
only (the renderer's own commented-out `setGunRotations(...)` call — CE disabling its own turret-tracking
feature, not something to re-enable speculatively). `EntityType`: `Phase4BossEntityTypes2.HUNTER_CHOPPER`.
Blocked on: nothing but the model/texture asset-porting work itself (the ~30 box definitions in
`ModelHunterChopper.java` plus one texture PNG, `chopper.png`) — this is a Phase-5-safe-now item.

### E — Cyber Crab family (`EntityCyberCrab`, `EntityTaintCrab`, `EntityTeslaCrab`)

| CE class | Lines | Model tech | Notes |
|---|---|---|---|
| `RenderCyberCrab` | 29, full | Vanilla box-cuboid (`ModelCrab`, 172 lines, 20 `ModelRenderer[]` boxes, confirmed by field/import read) | Plain `RenderLiving`-shaped wrapper, no per-frame extras — **fully portable today**, no OBJ dependency |
| `RenderTaintCrab` | 54, full | Same box-cuboid tech (`ModelTaintCrab`... wait, see correction below) | See note |
| `RenderTeslaCrab` | 54, full | Same shape as TaintCrab | See note |

**Correction within this family**: `ModelTaintCrab`/`ModelTeslaCrab` (36/37 lines each, `ModelTaintCrab`
read in full) are **not** box-cuboid like their sibling `ModelCrab` — both call
`ResourceManager.taintcrab.renderPart("Body"/"Legs1"/"Legs2")`/`ResourceManager.teslacrab.renderPart(...)`
respectively, i.e. **OBJ-rigged**, despite extending the same `ModelBase` base class as the vanilla-box
`ModelCrab`. So within one 3-class family, the base (`EntityCyberCrab`) is OBJ-free and portable today,
while both subclasses (`EntityTaintCrab`/`EntityTeslaCrab`) need the OBJ pipeline — do not assume family
membership predicts model tech even within a single inheritance chain. Both `RenderTaintCrab`/
`RenderTeslaCrab` additionally draw a `BeamPronter`-based electric tether toward each entry in their own
`targets` list (a `List<double[]>` of nearby zap-target coordinates, read directly off the entity —
already real per `docs/phase4/entities_vehicles_aircraft.md`'s crab-family confirmation) — same shared
procedural-beam dependency as UFO/RAD Beast, no new asset work beyond the OBJ body mesh itself.
`EntityType`s: `Phase4BossEntityTypes2.{CYBER_CRAB,TAINT_CRAB,TESLA_CRAB}`. `CyberCrab` is Phase-5-safe
now; `TaintCrab`/`TeslaCrab` are OBJ-blocked (`models/mobs/taintcrab.obj`/`teslacrab.obj` + textures).

### F — Quackos (joke pseudo-boss) + Duck (its prerequisite base mob)

`RenderQuacc` (53 lines, `extends RenderChicken`, its every line comment-tagged `/** BOW */` in CE's own
source, preserved faithfully by this survey's read) is the simplest renderer in this entire report: a
single texture override (`textures/entity/duck.png`) plus one `preRenderCallback` override that scales
the model **25×** (`GlStateManager.scale(25,25,25)`) — matching `EntityQuackos`'s own 25×-scaled hitbox
per `docs/phase4/entities_bosses.md`. **Fully confirmed portable today**: Neo Edition's own
`DuckRenderer.java` (21 lines, read in full) is a real, compiling `ChickenRenderer` subclass doing
exactly the texture-override half of this (Neo Edition has not ported Quackos itself, so the 25×-scale
override is not independently cross-checked, but `CreeperNuclearRenderer`'s own confirmed `scale(T,
PoseStack, float)` override pattern — read in full above — is the exact hook point a Quackos-scale
override needs: `poseStack.scale(25F, 25F, 25F)` inside an overridden `scale(...)` method, the modern
1.21.1 successor to `preRenderCallback`). Both `EntityDuck` (`DUCK`) and `EntityQuackos` (`QUACKOS`) need
their own `EntityRenderers.register` call even though Quackos reuses the same renderer class shape as
Duck plus one override, since they are two separate registered `EntityType`s
(`Phase4BossEntityTypes2.{DUCK,QUACKOS}`) — Duck gets a plain `ChickenRenderer` subclass with the texture
override only (no scale change; CE's `EntityDuck` is not scaled, only `EntityQuackos` is). Zero OBJ
dependency for either — Phase-5-safe now, needs only one texture PNG (`duck.png`, shared by both).

### G — RAD Beast

`RenderRADBeast` (80 lines, `extends RenderLiving<EntityRADBeast>`, base model `net.minecraft.client.
model.ModelBlaze` — **vanilla's own stock Blaze model**, reused as-is, not reimplemented) layers a
second box-cuboid model (`ModelM65Blaze`, 150 lines, `extends ModelBiped`, confirmed box-cuboid by field
read — a gas-mask overlay, texture `textures/armor/ModelM65Blaze.png`) on top via an overridden
`renderModel(...)` call, plus — when the entity has an active "unfortunate soul" grab target (a real,
already-ported mechanic per `docs/phase4/entities_bosses.md`'s boss-adjacent section) — a `BeamPronter`
tether identical in shape to the crab family's, from the beast to its victim, with special-cased Y-offset
handling for when the victim is the local player (a first-person camera-height correction, `tY -= 1.5`
only for `Minecraft.getMinecraft().player`). **Fully portable today, zero OBJ dependency at all** — the
base body is 100% vanilla (`net.minecraft.client.model.BlazeModel` in 1.21.1, unchanged in shape across
versions, well-established but not independently jar-verified in this sandbox), the mask overlay is a
plain box-cuboid `ModelBiped`-equivalent (`HumanoidModel<T>` in 1.21.1), and the tether reuses the
already-confirmed-portable `BeamPronter`. `EntityType`: `RadBeastEntityTypes.RAD_BEAST`. Needs only two
texture PNGs (`radbeast.png`, `ModelM65Blaze.png`) — no mesh assets at all.

### H — Rail/train vehicles (`TrainCargoTram`, `TrainCargoTramTrailer`, + 2 dummy rider entities)

| CE class | Lines | Model tech | Live per-frame data | Port field confirmed present |
|---|---|---|---|---|
| `RenderTrainCargoTram` | 65, full | OBJ (`ResourceManager.train_cargo_tram.renderAll()`, single mesh, no named parts) | Dual position interpolation: **both** the entity's raw tick position (`prevPosX`/`posX`-style) **and** its separately-tracked "rendered along the rail" position (`lastRenderX`/`renderX`-style), diffed against each other to keep the visible model glued to curved/sloped rail geometry even though the logical hitbox only exists at one anchor point (per `docs/phase4/entities_vehicles_aircraft.md`'s `EntityRailCarBase` finding) | `lastRenderX/Y/Z`/`renderX/Y/Z` **all confirmed present, field-for-field, in this port's `EntityRailCarBase.java`** (grepped directly) — zero server-side gap |
| `RenderTrainCargoTramTrailer` | 96, full | Same OBJ tech, plus a **recursive entity-render call**: for 1-9+ occupied cargo slots, spawns a throwaway client-only `EntityItem` wrapping `ModBlocks.crate` and calls `renderManager.renderEntity(dummy, offset...)` once per position in a hardcoded `CRATE_STACKS[tier]` offset table (9 tiers, up to 9 crates stacked in a fixed visual arrangement) — a real, deliberately-designed "more cargo = visibly more crates piled on the flatbed" effect, not a simple counter/HUD number | Same position-interpolation need as the tram, plus `train.getOccupiedSlots()` | `OCCUPIED_SLOTS` **confirmed present as a real `EntityDataAccessor<Integer>` with a public `getOccupiedSlots()` getter** in this port's `EntityRailCarCargo.java` (grepped directly) — zero server-side gap |
| (dummy) `EntityRailCarBase.BoundingBoxDummyEntity` | — | None — invisible helper entity | — | `TrainEntityTypes.BOUNDING_DUMMY` — register `EmptyEntityRenderer` |
| (dummy) `EntityRailCarRidable.SeatDummyEntity` | — | None — invisible rider-seat helper | — | `TrainEntityTypes.TRAIN_SEAT` — register `EmptyEntityRenderer` |

Both concrete renderers are genuinely well-specified once the OBJ pipeline exists — every field they read
is already present in this port's server-side code with matching names, a rare "zero surprises" case in
this report. The two dummy entities need nothing but the trivial `EmptyEntityRenderer` registration from
Headline finding #2/the Method section — this is the cheapest, fastest fix in the entire report and
should be done immediately regardless of OBJ-pipeline timing, since both types are `.noSummon()`'d
internal helpers that will otherwise crash the instant any player rides or spawns a `TrainCargoTram`.
Blocked on (for the 2 real renderers): the OBJ pipeline + `models/*/train_cargo_tram*.obj` + textures
(`tram_tex`/`tram_trailer_tex`).

### I — Minecart family (5 concrete classes, `com.hbm.entity.cart`)

| CE class | Lines | Model tech | Notes |
|---|---|---|---|
| `RenderCartCrate` | 19, full | **None** — bare `extends RenderMinecart<EntityMinecartCrate>`, zero override | Confirmed CE itself never gives this cart a custom look (no display-tile override on the entity side either) — vanilla's stock `MinecartRenderer<T>` is CE-faithful here. **Fully portable today**, zero new code beyond one `EntityRenderers.register` line |
| `RenderNeoCart` | 104, full | OBJ (`ResourceManager.cart.renderPart("Carriage"/"Bucket")`), plus a full re-derivation of on-rail visual position/roll/damage-shake from the vanilla minecart's own interpolation fields (`getPos`/`getPosOffset`, `getRollingAmplitude`, `getDamage`) — a near-total reimplementation of vanilla's own `RenderMinecart.doRender`, not a thin wrapper | Used by `EntityMinecartOre`/`Powder`/`Semtex`/`Destroyer` via each subclass's own `renderSpecialContent(RenderNeoCart)` override (a texture-swap-plus-`renderPart` overlay: Powder binds `cart_powder_tex`+"Powder"; Semtex binds two more textures for "SemtexTop"/"SemtexSide"; Destroyer just retextures+`renderAll()`s the whole cart; Ore adds nothing extra at all) |

Since this port's `EntityMinecartNTM extends AbstractMinecart` (Headline finding #6), a **fast, safe
interim fix exists that CE itself doesn't fully match visually**: register vanilla's stock
`MinecartRenderer<T>` for all 5 `CartEntityTypes` handles immediately (stops the Headline-finding-#2
crash risk for the whole family in one line each), then replace 4 of the 5 (`CART_ORE`, `CART_POWDER`,
`CART_SEMTEX`, `CART_DESTROYER`) with a real `RenderNeoCart`-equivalent once the OBJ pipeline lands —
`CART_CRATE` needs no follow-up at all, since it's CE-faithful either way. `EntityType`s:
`CartEntityTypes.{CART_ORE,CART_POWDER,CART_SEMTEX,CART_CRATE,CART_DESTROYER}`.

### J — Scripted aircraft (`EntityC130`, `EntityBomber`)

| CE class | Lines | Model tech | Notes |
|---|---|---|---|
| `RenderC130` | 75, full | OBJ, **named-part**: `ResourceManager.c130.renderPart("Plane")` for the fuselage plus 4 independently-spinning propeller parts (`"Prop1"`-`"Prop4"`), each wrapped in its own `pushMatrix`/translate-to-hub/rotate/translate-back sequence driven by a shared `System.currentTimeMillis() * 15D % 360D` spin value (**wall-clock-driven, not tick-driven** — a real CE quirk worth preserving exactly rather than "fixing" to a tick-based spin, since it means propeller speed is framerate-independent but not deterministic/replay-safe, matching this survey's general instruction to preserve CE's real behavior even where it's an odd choice) | No entity-state reads at all beyond rotation — the spin is a pure client-clock function |
| `RenderBomber` | 85, full | OBJ, **two alternate full airframes** selected by a synced `STYLE` byte (`EntityBomber.STYLE`, an `EntityDataAccessor`-equivalent, already real per `docs/phase4/entities_vehicles_aircraft.md`): styles 0-4 render a small Dornier-style plane (`ResourceManager.dornier`, 5× scale) with 5 texture variants; styles 5-8 render a much larger B-29-style plane (`ResourceManager.b29`, ~9.7× scale) with 4 texture variants — a real, deliberate "two visually distinct plane families sharing one entity class" design, not a simple recolor | `entity.getDataManager().get(EntityBomber.STYLE)` — confirmed already a real synced field per the vehicles report, no server-side gap for this one (unlike UFO's beam flag) |

Both gate their render entirely behind `ClientProxy.renderingConstant`, CE's own global "constant
renderer" master-switch flag (used elsewhere for the same purpose per `docs/phase4/
entities_vortex_gravity_wells.md`'s `RenderQuasar`/`RenderBlackHole` — see Deferred scope for where that
flag itself needs to land). `EntityType`s: `PlaneEntityTypes.{C130,BOMBER}`. Both OBJ-blocked
(`models/weapons/c130.obj`, `models/*/dornier.obj`+`b29.obj`, 9 total texture variants between them).

### K — Logistics drones (`EntityDeliveryDrone`, `EntityRequestDrone`) + `EntityParachuteCrate`

`RenderDeliveryDrone` (59 lines, `extends Render<EntityDroneBase>`) is a single renderer class shared by
**both** concrete drone `EntityType`s via two separate `@AutoRegister` factory fields in CE
(`FACTORY_REQUEST`/`FACTORY_DELIVERY`, both constructing the identical `RenderDeliveryDrone` class) —
port this as one renderer class registered twice, not two classes. Model tech: OBJ, named-part
(`ResourceManager.delivery_drone.renderPart("Drone")` always, plus conditionally `"Crate"` or `"Barrel"`
depending on the drone's own `getAppearance()` value — a real, already-ported cosmetic-only synced byte
per `docs/phase4/entities_vehicles_aircraft.md`). Texture selection branches three ways: request drones
get their own texture unconditionally; delivery drones check their own synced `IS_EXPRESS` flag
(confirmed already real, same report) for an express-vs-normal skin. `RenderParachuteCrate` (60 lines,
`extends Render<EntityParachuteCrate>`) is simpler — a sine-wave pendulum-sway rotation (two out-of-phase
sine terms driving X/Z tilt, a pure decorative "swinging under a parachute" effect with no entity-state
dependency beyond `world.getTotalWorldTime()`) around two separate OBJ pieces: the crate body
(`ResourceManager.conservecrate`, reusing the same mesh as an unrelated CE item) and the parachute canopy
itself (`ResourceManager.soyuz_lander.renderPart("Chute")` — reusing a part from the **Soyuz capsule's**
model, a real CE asset-reuse worth preserving rather than authoring a duplicate chute mesh). `EntityType`s:
`DroneEntityTypes.{DELIVERY_DRONE,REQUEST_DRONE}`, `ParachuteCrateEntityTypes.PARACHUTE_CRATE`. All
three OBJ-blocked.

### L — Gravity-well family (`EntityBlackHole`, `EntityVortex`, `EntityRagingVortex`, `EntityQuasar`)

Already fully detailed in Headline finding #4 — restated compactly here per this report's own per-entity
table format. All 4 share one renderer class in CE (`RenderBlackHole`, 460 lines, full — `RenderQuasar`,
76 lines, is a thin `extends RenderBlackHole` overriding only the disc texture/color-ramp/step-count for
a purple-tinted variant), and this report recommends the same one-class-plus-thin-subclass shape,
following Neo Edition's own confirmed structure exactly (`RenderBlackHole<T extends BlackHole>` generic
over the shared supertype, `RenderQuasar extends RenderBlackHole` overriding the same 3 hook methods
CE's own subclass does). Branch logic inside `doRender`/`render`: `EntityVortex` gets a swirl effect only;
`EntityRagingVortex` gets swirl + jets; the base `EntityBlackHole` (and `EntityQuasar`, since it adds no
override) gets a disc + jets. Live per-frame data: the synced `SIZE` float accessor — **confirmed already
present and correctly wired** in this port's `EntityBlackHole.java` (`SIZE` accessor with getter/setter
and NBT round-trip, grepped directly) — plus `entity.getId()`/`entity.tickCount` (both plain vanilla
`Entity` fields, always available, no port-side gap of any kind). `EntityType`s:
`GravityWellEntityTypes.{BLACK_HOLE,VORTEX,RAGING_VORTEX,QUASAR}`. **This is the one full family in this
report ready to implement immediately, with a real, compiling, line-by-line-traceable reference already
in this sandbox** (Neo Edition's own version) — recommend building this one first as a template/proof of
the `RenderType`/`VertexConsumer` translation pattern before tackling the OBJ-blocked entities, since it
needs no external asset-pipeline work at all (only two small texture PNGs — `bholeDisc`/`bhole`-swirl —
plus one purple variant for the Quasar's disc, `bholeD.png`; the central sphere needs no texture asset
at all once procedurally generated per Neo Edition's approach, since it renders from
`DefaultVertexFormat.POSITION_COLOR`, no UV).

### M — `EntityBoatRubber` (blocked on a different layer — the entity itself)

`RenderBoatRubber` (28 lines, `extends RenderBoat`, one texture-override method) is trivial — but
**`EntityBoatRubber` does not exist anywhere in this port yet**, confirmed by `find` across the whole
`src/main/java` tree, exactly as `docs/phase4/entities_vehicles_aircraft.md` already flagged (a "zero
novel mechanics" one-line `Boat` reskin that report deferred pending "this port's own `Boat`/`ChestBoat`-
equivalent registration pattern," which per that same report does not exist anywhere in this port either).
This is the one entity in this whole report where the blocker is **not** rendering technology at all —
there is no `EntityType` to register a renderer against yet. Owner: whoever ports the entity itself
first (a trivial `Boat` subclass per that report's own framing); this report's job here is complete once
that class exists (one `getTextureLocation` override on vanilla's own `BoatRenderer`, no OBJ, no new
mesh work — `EntityBoatRubber`'s CE texture is a plain 2D boat-skin PNG like every vanilla boat variant).

## Phase-5-safe scope (buildable right now, no external blocker)

1. **Register `EmptyEntityRenderer`-equivalents for all 29 `EntityType`s in this report** (Headline
   finding #2) — the single highest-priority, lowest-effort item here, since it is the only thing
   standing between Phase 4's already-shipped mechanics and a client crash the moment any of them spawns
   near a player. Every one of the 29 `DeferredHolder` handles is already named above by exact field.
2. **The full gravity-well family** (`EntityBlackHole`/`Vortex`/`RagingVortex`/`Quasar`) — a real,
   compiling, line-by-line-traceable reference (Neo Edition's own port) already exists; needs only 2-3
   small texture PNGs, no mesh assets, no OBJ pipeline. Recommend implementing this family first as the
   template for this report's `RenderType`/`VertexConsumer`/`PoseStack` translation pattern.
3. **Hunter Chopper's full body model** — confirmed vanilla box-cuboid (`ModelHunterChopper`, 492
   lines), zero OBJ dependency; needs only the box-dimension porting work (mechanical, ~30 `ModelRenderer`
   fields) plus one texture PNG (`chopper.png`).
4. **RAD Beast's full renderer** — vanilla `BlazeModel` base + a box-cuboid `ModelBiped`-style mask
   overlay + the already-portable `BeamPronter` tether; needs two texture PNGs, zero mesh assets.
5. **`EntityCyberCrab`'s renderer** (base class only, not `TaintCrab`/`TeslaCrab`) — vanilla box-cuboid
   `ModelCrab`, zero OBJ dependency; one texture PNG.
6. **Duck + Quackos renderers** — thin `ChickenRenderer` subclasses (Neo Edition's `DuckRenderer`
   confirms the exact shape); Quackos needs one additional `scale(...)` override for its 25× size. One
   shared texture PNG for both.
7. **`EntityMinecartCrate`'s renderer** — vanilla `MinecartRenderer<T>` registered directly, zero new
   code; CE itself never gives this cart a custom look either.
8. **An interim stock-`MinecartRenderer` registration for all 5 cart types** (item 1 already covers the
   crash-safety half of this; doing it with the *real* `MinecartRenderer` instead of `EmptyEntityRenderer`
   for the cart family specifically costs nothing extra and gets a visible, if not CE-faithful, cart
   immediately for the 4 OBJ-blocked ones too) — a reasonable staged rollout, not a substitute for item
   10 below.
9. **`BeamPronter`'s port** (the shared tether-beam helper `RenderUFO`/`RenderTaintCrab`/
   `RenderTeslaCrab`/`RenderRADBeast` all depend on) — already confirmed real and compiling in Neo
   Edition at the exact `RenderType`/`VertexConsumer` shape needed; a small, self-contained, one-time
   port that unblocks 4 renderers at once.
10. **The one-line server-side fix for `EntityUFO.beam`** (make it a real `SynchedEntityData` boolean
    with a getter) — independent of OBJ-pipeline timing, should happen regardless of when the UFO's
    visual renderer itself gets built.

## Blocked / deferred scope (named blocker, owner)

| Item | Blocked on |
|---|---|
| Worm head/body, MaskMan, UFO's saucer body, TaintCrab/TeslaCrab bodies, both train-car bodies, both aircraft, 4 of 5 minecarts (Ore/Powder/Semtex/Destroyer), both drone types, the parachute-crate body | The shared OBJ mesh-loading/named-part-VBO pipeline (`HFRWavefrontObject`/`ResourceManager`-equivalent) — confirmed **zero corresponding code exists anywhere in this port today** by `docs/phase5/renderer_framework_and_obj_models.md`'s own already-completed survey; this report does not re-scope that work, only re-confirms the dependency and names every consumer in this report's own scope that needs it |
| Every `.obj` mesh and texture PNG this report's OBJ-blocked entities need (`bot_prime_head.obj`/`bot_prime_body.obj`/`ufo.obj`/`maskman.obj`(-family)/`taintcrab.obj`/`teslacrab.obj`/`train_cargo_tram*.obj`/`c130.obj`/`dornier.obj`/`b29.obj`/`cart.obj`(+ variant textures)/`delivery_drone.obj`/`conservecrate.obj` — confirmed by `find`, zero present) | A dedicated asset-migration pass (copying files from `upstream/hbm-ce/src/main/resources/assets/hbm/{models,textures}/**` into this port's own resource tree) — mechanical work, not a design decision, but real volume; not resolved here |
| `EntityBoatRubber`'s renderer | `EntityBoatRubber` itself does not exist in this port yet — owner is whoever ports the entity (a trivial `Boat` subclass per `docs/phase4/entities_vehicles_aircraft.md`), not this report |
| `EntityUFO.beam`'s visual (the abduction-beam column) | The one-line synced-boolean fix named in Phase-5-safe item #10 — small, but a real precondition, not resolved by this report itself |
| `ClientProxy.renderingConstant` (the CE-style global "constant renderer" master switch both `RenderBomber` and, per `docs/phase4/entities_vortex_gravity_wells.md`, the gravity-well family's own CE renderers gate on) | Confirmed **not yet ported anywhere in this port** (grepped — no `renderingConstant` field exists in this port's `ClientProxy.java`, read in full above). A small, standalone client-config flag; whoever implements the first renderer that needs it (Bomber or the gravity-well family) should add it once, not duplicate it per-renderer |
| Fully CE-faithful visuals for 4 of the 5 minecarts | Same OBJ-pipeline blocker; the interim vanilla-`MinecartRenderer` fallback (Phase-5-safe item #8) is a real, working stopgap but not a substitute |

## Key design/API decisions

- **One renderer class can and should back multiple `EntityType`s where CE does the same** —
  `RenderDeliveryDrone` (2 types), `RenderBlackHole`/`RenderQuasar` (4 types via 1 base + 1 thin
  subclass), `RenderNeoCart` (4 of the 5 minecart types) all follow this shape in CE itself; port the
  same class-sharing structure rather than one renderer file per `EntityType` out of habit.
- **`EmptyEntityRenderer<T extends Entity>` (Neo Edition's real, compiling pattern) is the correct,
  minimal answer for this report's 2 dummy-rider entities** and should be reached for immediately given
  Headline finding #2's crash risk — do not wait for a "real" visual design decision for entities that
  were never meant to be seen in CE either.
- **`ClientProxy.registerEntityRenderers()` should be added to this port's own already-existing
  `ServerProxy`/`ClientProxy` split** (confirmed present, `ClientProxy.java` read in full), invoked from
  `FMLClientSetupEvent.enqueueWork` — matching both Neo Edition's confirmed call-site shape and this
  port's own already-established `ClientProxy`-override convention (the same class already carries
  `getIsKeyPressed`/`me()` overrides from Phase 0 keybind work).
- **Prefer this port's own already-real field names over re-deriving CE's** wherever this report found
  them already ported (`lastRenderX/Y/Z`/`renderX/Y/Z` on `EntityRailCarBase`, `OCCUPIED_SLOTS` on
  `EntityRailCarCargo`, `SIZE` on `EntityBlackHole`, `STYLE` on `EntityBomber`) — every renderer's field
  reads should target these exact, already-committed accessors, not reimplement parallel ones.
- **Preserve CE's real, even odd, per-entity quirks rather than "cleaning them up"**: `RenderC130`'s
  wall-clock-driven (not tick-driven) propeller spin, `RenderUFO`'s spin-independent-of-facing saucer
  rotation, and `RenderQuackos`'s 25× scale are all confirmed-real CE behaviors read directly from
  source, not artifacts of an outdated engine that should be normalized away.

## Open questions / risks

- **The worm's 75-simultaneous-instance render load** (Headline finding, section A) — a real performance
  question for 1.21.1 that this report does not resolve, already flagged as an open risk by
  `docs/phase4/entities_bosses.md`; re-flagged here specifically as a *rendering* load question (75
  simultaneous OBJ `renderAll()` calls, not just 75 ticking server entities) once this pair of renderers
  exists.
- **`net.minecraft.client.renderer.entity.MobRenderer<T, M extends EntityModel<T>>` as the correct base
  for `EntityCyberCrab`/`RAD Beast`-style custom-box-model mobs is well-established vanilla API knowledge,
  not independently cross-checked against a compiling Neo Edition file in this survey** (no equivalent
  custom-box-model mob renderer exists anywhere in that repo) — flagged per this task's own ground rules;
  low risk (this is stable, unchanged-in-shape vanilla API across many versions) but not jar-verified in
  this sandbox.
- **Whether to build the interim vanilla-`MinecartRenderer` stopgap (Phase-5-safe item #8) for all 5
  cart types, or go straight to `EmptyEntityRenderer` for the 4 OBJ-blocked ones and skip the interim
  step entirely**, is a real, low-stakes implementation-time choice this report flags but does not
  resolve — either unblocks Phase 4 playtesting equally well; the vanilla-renderer option is marginally
  more visually informative during that interim period at zero extra cost.
- **`ClientProxy.renderingConstant`'s exact semantics were not investigated in this survey beyond
  confirming its name and that CE gates `RenderBomber`/the gravity-well renderers on it** — CE's own
  purpose for this flag (a debug/performance toggle? a settings-menu option?) was not traced back to its
  origin; whoever adds it should confirm what actually flips it in CE before assuming a fixed always-true
  default is safe, unlike the `isWarDim`-style dimension gates other Phase 4 reports found and
  deliberately defaulted to always-true.
- **Whether `ModelHunterChopper`'s 492 lines and `ModelM65Blaze`'s 150 lines were read closely enough to
  guarantee every box's position/rotation/texture-offset was captured** — this report classified both as
  "vanilla box-cuboid, no OBJ dependency" from header/field-declaration reads sufficient for that
  classification, but did not hand-trace every one of the ~30-40 individual box definitions in either
  file; whoever implements these two models should do a full line-by-line port rather than treating this
  report's classification as a substitute for reading the complete source.
