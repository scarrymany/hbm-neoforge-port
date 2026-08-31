# Phase 5 research: RBMK reactor visual feedback & explosion/mushroom-cloud VFX

**Area:** `reactor_and_explosion_visual_effects` — two CE systems that both render escalating danger
state with custom Java code: (1) RBMK reactor visual feedback (control-rod insertion depth, the
fuel-column Cherenkov-glow effect, the in-world console heatmap, the autoloader piston); (2) nuclear/
antimatter/thermonuclear detonation visual effects (`EntityNukeTorex`'s mushroom cloud, the
`EntityCloud{Fleija,Solinium,Tom}` companion shell VFX, `EntityEMPBlast`'s expanding ring, and CE's
"camera shake" — which turns out not to be camera shake at all, see Headline finding 6).

## Sources read

**CE (`upstream/hbm-ce`), in full unless noted:**
- `com/hbm/entity/effect/EntityNukeTorex.java` (617 lines, full) — the mushroom-cloud simulation entity:
  field/constant declarations, `onUpdate()`'s entire client-side cloudlet-spawn logic (standard/shock/
  ring/condensation sub-clouds), the full `Cloudlet` inner class (motion/color/scale/alpha math for
  all 4 `TorexType` values), `setScale`/`getSimulationSpeed`/`getAlpha`/`getInterpColor`, both
  `statFac`/`statFacBale` factories.
- `com/hbm/render/entity/effect/RenderTorex.java` (478 lines, full) — the mushroom-cloud renderer:
  `doRender` (162–213, the exact HUD-flash/HUD-shake/fake-hurt-time trigger site — see Headline finding
  6), `cloudletWrapper`/`cloudletWrapperInstanced` (raw-GL instanced-vs-immediate cloudlet quad paths),
  `flareWrapper`/`flashWrapper`/`renderFlash` (the core flare + white detonation-flash effects),
  `sortCloudlets` (per-frame back-to-front cloudlet sort).
- `com/hbm/entity/effect/{EntityCloudFleija,EntityEMPBlast}.java` (96/89 lines, full, personally
  re-read in this session even though this port's own committed ports of both already carry a "read in
  full" javadoc citation from the prior Phase-3/4 pass) — confirms `getBrightnessForRender()` returning
  `15728880` (`0xF000F0`, vanilla's full-bright lightmap packing) on both, a detail neither port class
  currently replicates (see Key design decisions).
- `com/hbm/render/entity/{RenderCloudFleija,RenderCloudSolinium,RenderEMPBlast}.java` (61/65/54 lines,
  full) and `com/hbm/render/entity/effect/RenderCloudTom.java` (117 lines, full) — the four simple
  "expanding OBJ shell" renderers (sphere/ring/cylinder meshes scaled by `entity.age`).
- `com/hbm/render/tileentity/{RenderRBMKControlRod,RenderRBMKLid,RenderRBMKAutoloader,
  RenderRBMKConsole}.java` (67/58/61/250 lines — first three full, `RenderRBMKConsole` read for its
  `render()` method and heat→color mapping, ~120 of 250 lines, the remainder is 3D screen-frame
  geometry not relevant to this report) — the four RBMK renderers whose block entities already exist,
  real and networked, in this port today (see Headline finding 3).
- `com/hbm/render/tileentity/{RenderRBMKGauge,RenderRBMKCraneConsole}.java` (113/188 lines — signature/
  header only, ~20 lines each) — confirmed to exist and to be genuinely blocked (see Deferred scope),
  not read further since their block entities don't exist in this port at all.
- `com/hbm/main/ModEventHandlerClient.java` (1,533 lines total; ~90 lines read closely: `onOverlayRender`
  768–796 [nuke-flash quad] and 952–958 [nuke-shake HUD translate], `renderWorld` 421–446 [the
  `ClientProxy.renderingConstant` constant-render sweep — resolves an open question the sibling
  `docs/phase5/boss_and_vehicle_entity_renderers.md` explicitly left unanswered, see Headline finding 5],
  `flashDuration`/`shakeDuration` constants at 139–140) plus `grep` confirming `flashTimestamp`/
  `shakeTimestamp` have exactly the 2 write sites (`RenderTorex.doRender`) and 2 read sites
  (`onOverlayRender`) in the whole CE codebase.
- `com/hbm/main/ClientProxy.java` (grepped, 1 line read: `renderingConstant` field declaration at 137).
- `com/hbm/config/ClientConfig.java` (grepped: `NUKE_HUD_FLASH`/`NUKE_HUD_SHAKE` field + registration
  lines 28–29, 56–57).
- `com/hbm/tileentity/machine/rbmk/{TileEntityRBMKRod,TileEntityRBMKBase}.java` — `TileEntityRBMKRod`'s
  `serialize`/`deserialize` (268–292, full) and NBT read/write (245–262) to confirm the exact
  server→client field-repurposing CE performs for the Cherenkov-glow trigger (see Headline finding 4);
  `moveHeat`/`meltdown`/`standardMelt` already fully covered by `docs/phase2/rbmk_reactor.md`, not
  re-derived here, only cited.
- `com/hbm/render/tileentity/RenderRBMKAutoloader.java`, `RenderRBMKConsole.java` (see above) plus
  grep of `com/hbm/render/tileentity/` for every `RBMK*`-named file (15 total: Console, CraneConsole,
  ControlRod, Terminal, Lever, Autoloader, Gauge, Display, Indicator, Graph, Lid, Numitron, KeyPad, plus
  `entity/projectile/RenderRBMKDebris`) to build the complete inventory in Headline finding 3's table.

**This port's own `src/main/java`, in full unless noted:**
- `entity/effect/{EntityNukeTorex,EntityCloudFleija,EntityCloudSolinium,EntityCloudTom,
  EntityEMPBlast,EffectEntityTypes}.java` (132/91/81/74/82/102 lines, full) — **all six already real,
  committed, compiling classes** — see Headline finding 1. This is the single biggest correction to the
  task's own framing: the task describes `EntityNukeTorex` as "already a real Phase 3 entity" but the
  other four companion-cloud entities are equally real and already registered in the same file, a fact
  the task brief doesn't mention.
- `blockentity/machine/rbmk/{RBMKBaseBlockEntity,RBMKRodBlockEntity,RBMKControlBlockEntity,
  RBMKControlManualBlockEntity,RBMKConsoleBlockEntity,RBMKAutoloaderBlockEntity}.java` (300/290/195/
  154/191/174 lines — first five full, `RBMKAutoloaderBlockEntity` grepped for `piston`/`lastPiston`
  only) — confirms every field the CE renderers above read (`heat`, `extraction`/`lastExtraction`,
  `lastFluxQuantity`, `hasRod`, `rodColor`, `columns[]`, `piston`) is **already real and already
  networked** in this port (see Headline finding 3).
- `interfaces/IConstantRenderer.java` (7 lines, full, both this port's and CE's copies — byte-identical)
  and a whole-tree grep confirming 5 already-committed consumers (`EntityTom`, `EntityCloudTom`,
  `EntityNukeTorex`, `EntityOrbitalLaser`, `EntityDeathBlast`).
- `docs/phase2/rbmk_reactor.md` (the 481-line Phase 2 research report; §"Deferred scope" and
  §"`TileEntityRBMKBase`'s heat/meltdown state machine" read in full) and `docs/phase4/
  entities_orbital_and_beam_payloads.md` (§"Key design/API decisions", the `EffectEntityTypes`
  registration note) — both already-landed reports this one builds on rather than re-derives.
- `docs/phase3/explosion_engine.md` (the 537-line Phase 3 report; its own `EntityNukeTorex`
  finding at lines 321–327, 387–388 read — already correctly deferred this entity to Phase 5).
- `docs/phase5/{particle_engine_and_generic_vfx,hud_overlays_geiger_armor_gun,
  renderer_framework_and_obj_models,boss_and_vehicle_entity_renderers,gui_screens_survey_machines_
  processing}.md` — grepped for `Torex`/`mushroom`/`CloudFleija`/`CloudSolinium`/`CloudTom`/
  `shockwave`/`renderingConstant`/`RBMK` to establish this report's exact scope boundary against 5
  sibling Phase 5 reports already on disk (see "Scope boundary against sibling reports" below); the
  `RenderGuiEvent`/`GuiGraphics.pose()` API-shape citations in `hud_overlays_geiger_armor_gun.md` and
  the `RenderLevelStageEvent` citation in `particle_engine_and_generic_vfx.md` are reused, not
  re-derived, for this report's own HUD-flash/constant-render-sweep design (see Key design decisions).

**Neo Edition (`upstream/neo-edition`)**: grepped for `Torex`/`CloudFleija`/`RBMKConsole`/
`renderingConstant`/`IConstantRenderer` — **zero hits for any of them.** Neo Edition has not attempted
either half of this report's area at all; every NeoForge-1.21.1 API-shape claim below is instead
cross-confirmed via the sibling Phase 5 reports' own already-verified Neo Edition citations (named
inline wherever used) rather than a fresh Neo Edition read, since this specific system has no Neo
Edition precedent to check against.

This sandbox cannot run `./gradlew` (network policy blocks `maven.neoforged.net`) and cannot launch a
client, so nothing below is a screenshot-verified visual confirmation. Every 1.21.1 API-shape claim is
either (a) reused from a sibling Phase 5 report's own Neo-Edition-confirmed citation, or (b) explicitly
flagged **"well-established knowledge, NOT verified against a real jar."**

## Headline findings

1. **Every explosion-VFX entity this task names is already a real, committed, compiling class in this
   port — not just `EntityNukeTorex`.** `EntityNukeTorex`, `EntityCloudFleija`, `EntityCloudSolinium`,
   `EntityCloudTom`, and `EntityEMPBlast` are all already ported (`src/main/java/com/hbm/entity/effect/`,
   6 files including the shared `EffectEntityTypes` registry) as **server-tickable, fully-registered,
   zero-render-logic entities** — each one's javadoc explicitly documents "the ~N-line client-only
   render simulation is a Phase 5 TODO, not ported this wave." This confirms the task's framing exactly
   for `EntityNukeTorex` but the task brief undersells the scope: 4 more sibling entities in the same
   file need the identical treatment, plus a 6th (`EntityCloudFleijaRainbow`, an Easter-egg variant used
   by 2 weapon items) that isn't registered at all yet (see Deferred scope).

2. **CE's mushroom-cloud rendering technology (`RenderTorex`, the biggest file in this report) is raw
   hand-rolled OpenGL immediate-mode/instanced-VBO particle rendering with zero 1.21.1 equivalent to
   translate line-for-line — but this port's sibling `docs/phase5/particle_engine_and_generic_vfx.md`
   already solved the exact same problem for CE's *other* particle engine, and the identical
   replacement design applies here.** `RenderTorex.cloudletWrapper`/`cloudletWrapperInstanced` write
   raw `ByteBuffer`s via `NTMBufferBuilder`/`NTMImmediate`/`InstancedBillboardBatch` — the same
   1.12-only, `vertexAttribDivisor`-based hardware-instancing machinery that report's Finding 1 already
   confirmed **has no 1.21.1 equivalent to preserve at all** and is replaced by a lightweight
   non-vanilla-`Particle` object list (`ParticleNT`-shaped) hooked into
   `net.neoforged.neoforge.client.event.RenderLevelStageEvent`, batched through vanilla's own
   `MultiBufferSource.BufferSource`/`RenderType`. `EntityNukeTorex.cloudlets` (an `ArrayList<Cloudlet>`,
   up to `maxCloudlets = 20_000` per cloud) is structurally a bespoke, entity-owned particle system —
   the correct 1.21.1 design is to treat the mushroom cloud as **one `ParticleNT`-family batch owned by
   the `EntityNukeTorex` client-side render state** (not as 20,000 real Minecraft particles, and not as
   a hand-rolled second VBO pipeline), reusing the exact rendering primitives that sibling report already
   designed rather than inventing a third rendering technology for this one entity.

3. **A large, concrete slice of RBMK's visual-feedback work needs zero new server-side plumbing today** —
   every field CE's renderers read is already real, synced, and sitting in this port's already-committed
   RBMK block entities:

   | CE renderer | What it needs | Already real in this port |
   |---|---|---|
   | `RenderRBMKControlRod` (rod-insertion depth) | `level`/`lastLevel` (0–1 extraction fraction) | `RBMKControlBlockEntity.extraction`/`.lastExtraction` — networked via `serialize`/`deserialize` (`RBMKControlBlockEntity.java:181-182,190-191`) |
   | `RenderRBMKLid` (fuel-column Cherenkov glow) | `fluxQuantity > 5`, `rodColor`, `hasRod` | `RBMKRodBlockEntity.lastFluxQuantity`/`.rodColor`/`.hasRod` — networked (`RBMKRodBlockEntity.java:274-289`) — **note the field-name subtlety in Headline finding 4** |
   | `RenderRBMKConsole` (in-world 15×15 heatmap screen) | `te.columns[]`, each with `.heat`/`.maxHeat`/(for control columns)`.color` | `RBMKConsoleBlockEntity.columns` (`RBMKColumn[225]`), fully networked (`RBMKConsoleBlockEntity.java:169,189`), populated every 10 ticks from each column's already-real `getConsoleData()` |
   | `RenderRBMKAutoloader` (piston animation) | `lastPiston`/`renderPiston` (0–1 travel fraction) | `RBMKAutoloaderBlockEntity.piston`/`.lastPiston`, `piston` networked (`RBMKAutoloaderBlockEntity.java:33-34,166-172`) |

   All four are **pure client-rendering work today** — the exact "safe to build now" claim the task's
   ground rules ask this report to make explicit, not a hopeful assumption.

4. **CE's own client/server `fluxQuantity` field is deliberately repurposed across the network
   boundary, and this port's already-committed field split resolves the ambiguity correctly — but only
   if the future renderer reads the right field.** `TileEntityRBMKRod.fluxQuantity` is a
   *server-side-only transient accumulator*: it fills during the tick via `receiveFlux()` calls from
   neighbors, then CE's own `update()` unconditionally zeroes it every tick
   (`TileEntityRBMKRod.java:178`) after copying it into `lastFluxQuantity` (line 175). Yet
   `TileEntityRBMKRod.serialize(ByteBuf)` writes `this.lastFluxQuantity` onto the wire (line 270), and
   the *receiving* `deserialize(ByteBuf)` reads that value back into the client's own `this.fluxQuantity`
   field (line 288) — i.e., CE intentionally sends `lastFluxQuantity` but relabels it as `fluxQuantity`
   on arrival, so the client-side `RenderRBMKLid`'s `te.fluxQuantity > 5` check (its only read of this
   field) is really testing "did last tick's completed flux exceed 5," never a genuinely mid-tick value.
   This port's already-committed `RBMKRodBlockEntity` keeps the two fields honestly separate on both
   sides instead (`serialize` sends `lastFluxQuantity` labeled as `lastFluxQuantity`,
   `RBMKRodBlockEntity.java:276`) — meaning **the future 1.21.1 renderer must read
   `RBMKRodBlockEntity.lastFluxQuantity`, not `.fluxQuantity`** (which stays 0 on a client-side instance,
   since it is never sent) to reproduce CE's exact real behavior. A naive "translate CE's field name
   literally" port would silently read the wrong (always-zero) field and the Cherenkov glow would never
   render — flagged here precisely so that mistake isn't made at implementation time.

5. **`ClientProxy.renderingConstant` — a mechanism `docs/phase5/boss_and_vehicle_entity_renderers.md`
   named but explicitly left uninvestigated ("CE's own purpose for this flag... was not traced back to
   its origin") — is fully resolved here, and it is a hard behavioral dependency for `RenderTorex` and
   `RenderCloudTom`, not an optional debug toggle.** `ModEventHandlerClient.renderWorld`
   (`@SubscribeEvent(priority = EventPriority.LOWEST)` on `RenderWorldLastEvent`, lines 421–446) runs
   *after* vanilla's normal frustum-culled per-chunk entity render pass, sets
   `ClientProxy.renderingConstant = true`, manually iterates **every entity in
   `mc.world.loadedEntityList`** (not just visible ones), and for each one implementing
   `IConstantRenderer`, force-calls `Render.doRender(...)` a second time with hand-computed
   interpolated position/yaw — then sets the flag back to `false`. Every `IConstantRenderer` renderer
   (`RenderTorex`, `RenderCloudTom`, plus the boss/vehicle report's own `RenderBlackHole`/`RenderQuasar`/
   `RenderBomber`/`RenderDeathBlast`/`RenderOrbitalLaser`/`RenderFOEQ`/`RenderMirv`/`RenderBombletZeta`)
   early-returns (`if (!ClientProxy.renderingConstant) return;`) during vanilla's normal pass and only
   actually draws during this explicit second sweep — this is the load-bearing reason these entities
   also set `ignoreFrustumCheck = true`/override `isInRangeToRenderDist` to always return `true`: CE
   deliberately bypasses vanilla's visible-chunk culling entirely for anything whose visual footprint
   (a mushroom cloud, a black hole) can legitimately be far larger than any single chunk's render
   distance would otherwise allow. The 1.21.1-equivalent hook is
   `net.neoforged.neoforge.client.event.RenderLevelStageEvent`, already confirmed real and already the
   chosen hook for this exact "runs once per frame, after/alongside the normal entity pass" role by
   `docs/phase5/particle_engine_and_generic_vfx.md`'s own `EngineHandler` design (`Stage.AFTER_WEATHER`)
   — this report recommends the constant-render sweep register on the same event family (a distinct
   `Stage`, e.g. `AFTER_ENTITIES`, still to be picked at implementation time) rather than inventing a
   second per-frame hook, since **6 already-committed entity classes across 2 Phase 5 reports both
   need this exact mechanism to exist once, generically** (this report does not claim ownership of
   building it — see Deferred scope — only resolves what it does and confirms the correct 1.21.1 event
   family to build it on).

6. **CE's "nuclear detonation camera shake" is not camera shake at all — it is a 2D HUD-element
   translation plus abuse of vanilla's own damage-flash system, and this port already has the
   config keys wired for it.** `RenderTorex.doRender` (lines 197–207) is the *only* place either static
   timestamp gets written: `ModEventHandlerClient.flashTimestamp`/`.shakeTimestamp` are stamped once,
   throttled to at most once per 1,000ms real-world-time, purely as a side effect of the mushroom-cloud
   entity being rendered (not from any explosion/damage event). Two independent consumers, both gated by
   already-ported config booleans:
   - **Flash** (`onOverlayRender`, `ElementType.CROSSHAIRS`, `ClientConfig.NUKE_HUD_FLASH`): a
     full-screen white translucent quad (`GL11.GL_QUADS`/`POSITION_COLOR`) drawn over the crosshair HUD
     layer, alpha linearly fading from 1→0 across `flashDuration = 5_000`ms of **wall-clock time**
     (`System.currentTimeMillis()`, not ticks) — this is a real detail worth preserving exactly, since a
     tick-based reimplementation would desync from CE's real fade curve under lag/tick-freeze.
   - **"Shake"** (`onOverlayRender`, `ElementType.HOTBAR`, `ClientConfig.NUKE_HUD_SHAKE`): a
     `GlStateManager.translate` applied only to the HOTBAR HUD element (not the 3D camera, not the
     world), driven by two independent sine waves (`shakeDuration = 1_500`ms, horizontal amplitude 15px,
     vertical amplitude 3px, decaying linearly to 0 over the duration). **Separately**, `RenderTorex`
     itself (not the HUD code) fakes a genuine vanilla damage hit by directly writing
     `player.hurtTime`/`.maxHurtTime`/`.attackedAtYaw` (lines 202–206, scaled by distance-to-cloud) —
     this is what produces the familiar red damage-vignette-and-tilt sensation players associate with
     "the nuke shook the screen," and it is 100% vanilla's own existing hurt-animation system being
     invoked with no real damage dealt, not a bespoke camera-shake implementation. **This port's
     `ClientConfig.java` already has both `NUKE_HUD_FLASH`/`NUKE_HUD_SHAKE` (`nukeHudFlash`/
     `nukeHudShake`, both default `true`) ported and waiting for a consumer** — zero new config work
     needed. The 1.21.1 API shape for both (`GuiGraphics.pose().pushPose()/translate()/popPose()` for
     the HUD-element translate, a colored-quad `Tesselator`/`BufferBuilder` draw for the flash) is
     already confirmed real by `docs/phase5/hud_overlays_geiger_armor_gun.md` Finding 2, which explicitly
     cites *this exact* nuke-shake/nuke-flash code as its own worked example of the API shape — this
     report is the one that actually needs to build the feature that example was drawn from.

7. **RBMK's "8 more CE GUI classes with no block entity at all" gap (already found by
   `docs/phase5/gui_screens_survey_machines_processing.md`) is the same root blocker for this report's
   8 corresponding 3D panel-block TESRs — named once here so it isn't rediscovered as a fresh gap.**
   `RenderRBMKLever`/`Gauge`/`Terminal`/`KeyPad`/`Display`/`Indicator`/`Graph`/`Numitron` (and
   `RenderRBMKCraneConsole`) all exist in CE and all render real block-mounted 3D readouts, but their
   backing block entities (`RBMKLever`/`Gauge`/`Terminal`/etc.) do not exist anywhere in this port —
   confirmed by the same `RBMKBlockEntities.java` field list the GUI report already read. This report
   does not re-solve that gap (it belongs to whoever picks up the remaining RBMK block-entity work,
   Phase 2/3 scope per that report's own framing) — it only confirms the renderer side inherits the
   identical blocker, so 9 of RBMK's 15 CE TESR classes are blocked on the same named prerequisite
   while the 4 in Headline finding 3 above are not.

## Scope boundary against sibling reports

To avoid duplicating work already claimed elsewhere in this Phase 5 wave:

- **The generic particle-object rendering technology** (how a `ParticleNT`-shaped batch actually gets
  drawn every frame via `RenderLevelStageEvent`/`MultiBufferSource`) is `docs/phase5/
  particle_engine_and_generic_vfx.md`'s territory — reused by reference above (Headline finding 2), not
  re-derived.
- **`ExplosionLarge.spawnShock`/`spawnParticlesRadial`/the generic `AuxParticlePacketNT`/`HbmEffectNT`
  broadcast dispatch** (including the `"Muke"` mushroom-cloud dispatch entry that same report's own
  dispatch table names at `GrenadeFillingActions.java:246`) is that same report's territory. **This is a
  distinct mechanism from `EntityNukeTorex.TorexType.SHOCK`** (the mushroom cloud's own internal
  "shockwave ring" cloudlet sub-simulation, lines 150–170 of `EntityNukeTorex.java`, spawned directly by
  the cloud entity itself, not via any packet) — the two both produce a visual "shockwave" but are
  unrelated code paths; this report owns only the latter (it's inside the entity this report is
  scoped to), not the former.
- **The HUD-flash/HUD-shake API primitives** (`GuiGraphics.pose()` transforms, colored-quad drawing,
  `RenderGuiEvent`/`RenderGuiLayerEvent`) are confirmed by `docs/phase5/hud_overlays_geiger_armor_gun.md`
  — reused by reference (Headline finding 6), not re-derived; that report does not itself claim to
  *build* the nuke-flash/shake feature (it only uses it as a worked API-shape example), so there is no
  ownership conflict, only a citation.
- **RBMK's GUI/Menu screens** (`RBMKConsoleScreen`, `RBMKRodScreen`, etc. — the flat 2D `Screen`
  windows opened by right-clicking a block) are `docs/phase5/gui_screens_survey_machines_processing.md`'s
  territory in full, including that report's own finding about the console's flux-graph feature loss.
  This report covers only the **3D in-world TESR/BER renderers** (what you see standing next to the
  block), never the 2D `Screen` GUI.
- **OBJ-model loading mechanics** (`HFRWavefrontObject` → `IGeometryLoader`/`renderPart(String)`-style
  group lookup) are `docs/phase5/renderer_framework_and_obj_models.md`'s territory. This report names
  which specific OBJ assets each renderer needs (`rbmk_rods_vbo`/`rbmk_element_rods_vbo`/
  `rbmk_autoloader` for RBMK; `Sphere.obj`/`Ring.obj`/a cylinder mesh for the explosion clouds) but does
  not re-derive the loading pipeline itself.
- **Boss/vehicle entity renderers** (`RenderBlackHole`, `RenderMirv`, etc.) are `docs/phase5/
  boss_and_vehicle_entity_renderers.md`'s territory — referenced only for the shared
  `IConstantRenderer`/`renderingConstant` mechanism both reports' entities depend on (Headline finding 5).

## Phase-5-safe scope (buildable today, zero new server-side plumbing)

**RBMK (4 of 15 TESRs, all backing block entities already real and networked — see Headline finding
3's table):**
1. **Control-rod insertion `BlockEntityRenderer`** for `RBMKControlBlockEntity` (both `Manual`/`Auto`
   subclasses, matching CE's single `RenderRBMKControlRod` backing both `TileEntityRBMKControlManual`/
   `Auto`): translate a shared rod-mesh "Lid" part vertically by
   `Mth.lerp(partialTick, extraction_last, extraction)` (this port's field names — CE:
   `lastLevel`/`level`), texture selected by `RBMKControlManualBlockEntity.color` (5 color variants) vs.
   the plain/auto texture. CE borrows lighting from the block above the reactor stack
   (`world.getCombinedLight(pos.up(offset+1), 0)`) rather than the rod's own position — worth
   preserving, since the rod shaft itself sits inside an otherwise-dark column.
2. **Fuel-column Cherenkov-glow `BlockEntityRenderer`** for `RBMKRodBlockEntity`: a `hasRod`-gated
   stack of `Rods`-part meshes (one per column-height unit, tinted by `rodColor`), plus — when
   `lastFluxQuantity > 5` (see Headline finding 4 for the exact field to read) — a translucent
   additive-blended stack of horizontal quads (CE's "Cherenkov effect," cyan-blue `r=0.4,g=0.9,b=1.0`)
   spanning the column height, layered every 0.25 blocks.
3. **In-world console heatmap `BlockEntityRenderer`** for `RBMKConsoleBlockEntity`: a 15×15 grid of
   colored quads on the console's screen face, one per `columns[i]` entry (`null` = no column loaded at
   that grid cell), heat-to-color mapping already known exactly (`r = colorValue + (1-colorValue) *
   (heat/maxHeat)`, `colorValue` a faint per-row alternating base of 0.65/0.70; control columns instead
   solid-color by `((ControlColumn)col).color` when non-negative). This is a genuinely rich "reactor
   danger escalates visually" feature and needs zero new data — `columns[]` is already a fully populated,
   fully networked 225-element array today.
4. **Autoloader piston `BlockEntityRenderer`** for `RBMKAutoloaderBlockEntity`: translate a "Piston"
   mesh part by `Mth.lerp(partialTick, lastPiston, piston)` — the simplest of the four.

**Explosion/mushroom-cloud (entity-registration and server-lifetime already done for all 5; client
particle sim/renderer is the entire remaining scope):**
5. **`EntityCloudFleija`/`EntityCloudSolinium`/`EntityCloudTom` `EntityRenderer`s**: each is a single
   expanding OBJ mesh (sphere/ring-of-spheres/tapered-cylinder respectively) scaled by
   `age + partialTick` with a flat or gradient color tint — the simplest render work in this report,
   ~60–120 CE lines each, no particle system, no per-frame allocation. `EntityCloudFleija`/`Solinium`
   additionally need their `getBrightnessForRender`-equivalent full-bright override (see Key design
   decisions) so they read as glowing rather than shaded by ambient light.
6. **`EntityEMPBlast` `EntityRenderer`**: an expanding ring-mesh, same shape as above, ~50 CE lines.
7. **`EntityNukeTorex` `EntityRenderer`/client tick**: the large remaining item — port the ~500-line
   client-only cloudlet simulation this port's own javadoc already documents as the deferred TODO
   (`EntityNukeTorex.java:82-85`), following the `ParticleNT`-batch design in Headline finding 2 rather
   than CE's raw-GL VBO path, plus the flare/flash quad effects and the HUD-flash/shake trigger
   (Headline finding 6) — Phase-5-safe once the constant-render-sweep mechanism (Headline finding 5) and
   the generic particle rendering primitives (`particle_engine_and_generic_vfx.md`) both land, since
   this entity depends on both.
8. **The HUD nuke-flash/nuke-shake overlay** (Headline finding 6): a `RenderGuiEvent.Pre` handler
   reading 2 static timestamps + the 2 already-ported `ClientConfig` booleans, no new data needed.

## Deferred scope

- **The generic constant-render-sweep mechanism** (`ClientProxy.renderingConstant`'s 1.21.1 replacement,
  Headline finding 5) — a genuine shared cross-cutting prerequisite for 2 of this report's own
  renderers (`RenderTorex`/`RenderCloudTom`) and 8+ of `docs/phase5/boss_and_vehicle_entity_renderers.md`'s.
  This report resolves *what it does* and *which 1.21.1 event family to build it on*, but does not claim
  ownership of building it — whichever report/implementer lands the first `IConstantRenderer`-consuming
  renderer should build it once (both reports independently make this same recommendation).
- **RBMK meltdown's real byproduct-block conversion** (`pribris`/`pribris_burning` rubble blocks) — still
  **completely absent from this port** (confirmed by grep, zero hits for either name under
  `src/main/java/com/hbm/blocks/`), exactly as `docs/phase2/rbmk_reactor.md` found and Phase 3/4 left
  unresolved. `RBMKBaseBlockEntity.standardMelt(int)` (`RBMKBaseBlockEntity.java:236-240`) is still the
  documented no-op it always was. **Partial exception**: `PWRBlocks.CORIUM_BLOCK` now exists (registered
  by the PWR package as a plain, non-fluid solid block — `docs/phase2/rbmk_reactor.md`'s own suggested
  fallback for CE's finite-spreading `CoriumFinite` fluid) and `RBMKRodBlockEntity.onMelt` already
  places it for the fuel-rod-column case (`RBMKRodBlockEntity.java:193-200`) — so **corium rendering
  (a plain static block, standard baked model, no TESR needed) is not blocked**, but everything else in
  CE's meltdown visual escalation (non-fuel-column rubble conversion, `EntityRBMKDebris` flying-loot
  particles, the overpressure/pipe-destruction pass) still is. This is a real, named, narrow gap this
  report's own area does not itself need to fix — the future RBMK renderer work has real corium to
  render, but nothing to render for a partial (non-fuel-column) meltdown yet.
- **`com.hbm.entity.projectile.EntityRBMKDebris`, `com.hbm.entity.effect.EntitySpear`** — confirmed
  still entirely absent from this port (the Phase 2 report's finding that "the entire `com.hbm.entity`
  package tree is absent" is now stale — Phase 3/4 built hundreds of entity files — but these two
  specific classes were never among them, confirmed by `find`). Owner: whoever completes RBMK's
  meltdown-byproduct work above; this report's `RenderRBMKDebris` (49 lines in CE, an entity renderer
  for flying rubble/graphite/fuel-element loot) is trivially portable once the entity exists but cannot
  land before it.
- **9 of 15 RBMK TESRs** (`RenderRBMKLever`/`Gauge`/`Terminal`/`KeyPad`/`Display`/`Indicator`/`Graph`/
  `Numitron`/`CraneConsole`) — blocked on the same missing-block-entity gap
  `docs/phase5/gui_screens_survey_machines_processing.md` already named (Headline finding 7). Not this
  report's to fix.
- **`EntityCloudFleijaRainbow`** (CE, 102 lines, an Easter-egg antimatter-cloud variant used by
  `ItemGunB92`/`WeaponSpecial` per `docs/phase3/scattered_military_items.md`'s own finding) — not yet
  registered anywhere in this port's `EffectEntityTypes`. Low priority (a joke variant, not core
  content); CE has **no dedicated renderer for it at all** (grep of `render/entity*` for the name
  returns zero hits) — it likely reuses `RenderCloudFleija` by rendering the same entity type under a
  different registration, or is simply visually absent in CE too. Worth a 10-minute confirmation at
  implementation time rather than assuming a renderer needs to be written from scratch.
- **`EntityNukeTorex`'s `didPlaySound`/`didShake` fields** (CE has both; this port's already-committed
  stub has neither) — both need to be (re-)added as part of porting the deferred client-tick simulation
  in Phase-5-safe item 7 above. `didShake` in CE is unusually written *by the renderer*, not the entity
  (`RenderTorex.doRender` line 201: `cloud.didShake = true;`) — a renderer mutating its target entity's
  field, a legal-but-unusual pattern in vanilla's own `EntityRenderer` API too; flagged as a design
  choice to preserve deliberately (see Key design decisions) rather than "fixed" into a separate
  client-side map keyed by entity ID, unless a concrete reason to change it turns up at implementation
  time.
- **Sounds** (`HBMSoundHandler.nuclearExplosion`, played from `EntityNukeTorex`'s own deferred
  client-tick block, `didPlaySound`-gated) belong to `docs/phase5/sound_wiring_and_assets.md`'s asset
  pipeline once that report exists/is checked — not investigated by this report (out of scope; this
  report only names the trigger condition, distance-gated at `< (ticksExisted*1.5+1)*1.5` blocks).
- **Texture/model assets**: none of `rbmk_control_{red,yellow,green,blue,purple}.png`,
  `rbmk_control{,_auto}.png`, `rbmk_element_fuel.png`, `BlastFleija.png`, `EMPBlast.png`, `tomblast`
  (whatever its real filename is), `Sphere.obj`, `Ring.obj`, `rbmk_rods_vbo`'s backing OBJ, or
  `rbmk_autoloader`'s backing OBJ exist anywhere in this port's `src/main/resources` yet — consistent
  with `docs/phase5/hud_overlays_geiger_armor_gun.md`'s own finding that this port's resources tree has
  **no `textures/` directory at all yet**. Asset-copying is real, necessary, and not this report's job
  to perform, only to flag (matches that sibling report's own framing of the identical gap).

## Key design/API decisions

- **RBMK's 4 buildable renderers are all `BlockEntityRenderer<T>` (BER), matching the framework
  `docs/phase5/renderer_framework_and_obj_models.md` already established** (`BlockEntityRenderer`/
  `BlockEntityRendererProvider`/`BlockEntityRenderers.register` — CE's `TileEntitySpecialRenderer<T>`
  → 1.21.1 `BlockEntityRenderer<T>` is that report's own confirmed 1:1 mapping). No new registration
  pattern is needed here.
- **The 6 explosion-VFX entities are `EntityRenderer<T>`, matching the pattern
  `docs/phase5/boss_and_vehicle_entity_renderers.md` already established**
  (`net.minecraft.client.renderer.entity.EntityRenderers.register(EntityType<T>,
  EntityRendererProvider<T>)`, called from a `ClientProxy.registerEntityRenderers()`-style
  `FMLClientSetupEvent.enqueueWork` block). No new registration pattern needed here either.
- **`EntityCloudFleija`/`Solinium`/`EMPBlast`'s CE-side full-bright override
  (`getBrightnessForRender() → 15728880`) has no direct field-level equivalent to port onto this port's
  already-committed entity classes** — 1.21.1's `Entity` base class has no equivalent method; the
  correct hook is `EntityRenderer<T>.getPackedLight(T entity, float partialTick)`, overridden per-renderer
  to return the vanilla constant `net.minecraft.client.renderer.LightTexture.FULL_BRIGHT`
  (**well-established Minecraft-modding knowledge — the literal value `0xF000F0` = `15728880` is the
  same lightmap-packing convention CE's own magic number already encodes, but this specific 1.21.1
  constant name/location is NOT verified against a real jar in this sandbox**) rather than needing any
  change to the already-committed entity classes themselves.
- **The constant-render-sweep's 1.21.1 home should be `RenderLevelStageEvent`, on a distinct `Stage`
  from `particle_engine_and_generic_vfx.md`'s own `AFTER_WEATHER` particle-draw stage** (both need to
  run once per frame outside vanilla's normal per-chunk entity culling, but they are logically distinct
  passes — one draws hand-rolled particle batches, the other force-renders whole
  `IConstantRenderer`-tagged entities a second time) — a design recommendation, not yet built by either
  report, so the exact `Stage` value is an implementation-time choice, not resolved here.
- **CE's wall-clock-time-based flash/shake durations (`System.currentTimeMillis()`, not tick count)
  should be preserved as wall-clock in the port too**, not converted to a tick-counter — CE's own choice
  here already protects the effect's fade curve from tick-rate hiccups/lag, a property worth keeping
  rather than "simplifying" into `ClientTickEvent`-counted ticks.
- **RBMK's shared multi-part OBJ meshes (`rbmk_rods_vbo`, `rbmk_element_rods_vbo`, `rbmk_autoloader`)
  are each loaded once and referenced by named part (`renderPart("Lid")`/`"Rods"`/`"Piston"`/`"Base"`)
  across many call sites/renderers** — confirms `docs/phase5/renderer_framework_and_obj_models.md`'s own
  finding that CE's OBJ loader groups faces by named `g`/`o` group and that the 1.21.1 replacement needs
  the same "load once, render named sub-group many times" capability, not a one-mesh-per-renderer model.

## Open questions / risks

- **Whether the mushroom-cloud's `Cloudlet` particle count (up to 20,000 live objects per cloud,
  `maxCloudlets`) is performance-safe under the `ParticleNT`-batch design this report recommends
  (Headline finding 2) was not load-tested here** (this sandbox cannot run a client). CE itself gates
  the raw-GL instanced path behind `GeneralConfig.instancedParticles`/`ShaderHelper.areShadersActive()`
  specifically because 20,000 cloudlets is expensive even on CE's own hardware-instanced fast path,
  falling back to a slower immediate-mode path otherwise (`RenderTorex.doRender`, lines 178–187) —
  worth a deliberate perf pass (possibly a lower default cloudlet cap, or genuine GPU instancing via
  vanilla's own newer rendering primitives if 1.21.1 exposes one) rather than assuming the generic
  `ParticleNT` design scales to this entity's particle count unchanged.
- **`RenderRBMKConsole`'s remaining ~130 unread lines** (of 250 total) are 3D screen-frame/bezel
  geometry and the item-in-hand renderer (`IItemRendererProvider`) — not load-bearing for the heatmap
  feature itself (confirmed by reading the `render()` method's structure and the heat-color math in
  full) but not verified line-by-line; a full read is recommended before implementation, not assumed
  sufficient from this report's partial read.
- **The exact real-CE meaning of `RBMKColumn.ControlColumn.color`'s "≥0 = colored" convention** (used by
  both `RenderRBMKConsole`'s heatmap and `RenderRBMKControlRod`'s texture selection) was confirmed
  structurally (an `ordinal()`-or-`-1` byte, matching `RBMKControlManualBlockEntity.color`'s own
  `Optional`-shaped nullable-enum pattern already in this port) but the exact 5-color CE palette
  (`0x?????? `per `RBMKColor.{RED,YELLOW,GREEN,BLUE,PURPLE}`) was read only from CE's *texture filenames*
  (`rbmk_control_red.png` etc.), not CE's `RenderRBMKConsole` RGB literals for the *console* variant of
  those same 5 colors (lines 89-95 read literals for indices 0–4 but this report did not cross-check
  whether they match the rod-texture red/yellow/green/blue/purple naming 1:1 in hue — worth a quick
  side-by-side check at implementation time, low risk either way since it only affects a cosmetic tint).
- **Whether CE's "borrow light from the block above the reactor stack" trick
  (`RenderRBMKControlRod`'s `world.getCombinedLight(pos.up(offset+1), 0)`)** has a clean 1.21.1
  equivalent inside a `BlockEntityRenderer` (which normally receives packed light as a `doRender`
  parameter computed from the block entity's own position) was not investigated — likely needs a manual
  `level.getBrightness(LightLayer, BlockPos)`/`LevelRenderer.getLightColor` call at a hand-picked
  position rather than the renderer's supplied `packedLight` parameter, flagged as
  well-established-but-unverified Minecraft-modding knowledge, not confirmed against a real jar.
