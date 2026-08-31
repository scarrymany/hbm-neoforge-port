# HUD overlays: Geiger, armor, and gun readouts — Phase 5 research

**Area:** `hud_overlays_geiger_armor_gun` — CE's 2D screen-space HUD layer: the Geiger/digamma
radiation gauges, the HEV suit's built-in radiation readout, gun ammo-counter/durability-bar/crosshair
HUD components, the two distinct jetpack fuel gauges, the shield/dash bars and armor-mod bars-list, the
perk badge row, and the look-at-block overlay dispatcher. Boss health bars and the gas-mask-filter
"durability" question are investigated and explicitly ruled **out** of this area (see Headline findings
6–7) with the real answer documented either way, per the task's own instruction to correct wrong framing
rather than force a fit.

## Sources read

**CE (`upstream/hbm-ce`), in full unless noted:**
- `com/hbm/render/misc/RenderScreenOverlay.java` (532 lines, full) — the central HUD-drawing class:
  `renderRadCounter`/`renderDigCounter` (lines 47–162), `renderCustomCrosshairs` (170–187),
  `renderStingerLockon` (189–200), `renderAmmo`/`renderAmmoAlt` (202–256), `renderDashBar` (259–366),
  `renderShieldBar` (369–412), `renderScope` (414–453), `renderBadges` (455–497), and the `Crosshair`
  enum (499–532, already ported verbatim as a stub — see below).
- `com/hbm/main/ModEventHandlerClient.java` (1,533 lines total; ~350 lines read closely: `onOverlayRender`
  768–973, `onRenderHUD`/badges 975–983, `onHUDRenderShield` 985–996, `getBars` 998–1015,
  `onHUDRenderBar` 1017–1114, plus the thermal-sight/`RenderOverhead` dispatch at 578–599 and 710–730
  read to establish this area's boundary — see Headline finding 8) — the actual `RenderGameOverlayEvent`
  wiring that calls every method above.
- `com/hbm/interfaces/IItemHUD.java` (12 lines, full), `com/hbm/blocks/ILookOverlay.java` (56 lines, full).
- `com/hbm/items/weapon/sedna/hud/{IHUDComponent,HUDComponentAmmoCounter,HUDComponentDurabilityBar}.java`
  (11+71+52 = 134 lines, full).
- `com/hbm/items/weapon/sedna/{GunConfig,ItemGunBaseNT}.java` — `GunConfig`'s `hudComponents_DNA`/
  `getHUDComponents`/`hud(...)` slot (lines 89, 120, 157) and `ItemGunBaseNT.renderHUD` (458–484, full),
  including the CE `bottomOffset`-never-accumulates quirk (see Headline finding 9).
- `com/hbm/items/weapon/ItemGunBase.java` (763 lines total; `renderHUD` 716–762 read in full) — the
  legacy (non-Sedna) gun's ammo-counter + crosshair/`IHoldableWeapon` dispatch, cross-checked against
  `docs/phase4/entities_legacy_bullet_system.md`'s own scoping of which 2 legacy guns actually survive.
- `com/hbm/items/gear/ArmorFSB.java` (540 lines, full) — `customGeiger`/`geigerSound` fields (69–70),
  `onArmorTick`'s click-sound cue (280–306), the base (no-op) `handleOverlay` hook (316–319),
  `setHasCustomGeiger` (331–334), `addInformation`'s `armor.geigerHUD` tooltip line (445).
- `com/hbm/items/armor/ArmorHEV.java` (218 lines, full) — the one real override of `handleOverlay`
  anywhere in CE (confirmed by `grep -rln "void handleOverlay"` — exactly 2 hits, `ArmorFSB.java`'s
  no-op base and this one override): `handleOverlay` (57–71) cancels vanilla's `ARMOR`/`HEALTH` HUD
  elements and substitutes `renderOverlay` (77–180), a from-scratch ASCII-art health/charge/radiation
  readout.
- `com/hbm/handler/JetpackHandler.java` (1,074 lines total; header/fields 1–100 and `renderHUD` 429–570
  read in full) — the `JetpackGlider`'s bespoke rotary-dial fuel/thrust gauge, distinct from the
  bars-list mechanism below.
- `com/hbm/handler/ArmorUtil.java` (372 lines; gas-mask-filter section 214–372 read in full) —
  `damageGasMaskFilter`/`installGasMaskFilter`/`getGasMaskFilter`/`describeFilter` — confirms the real
  answer to the task's "gas-mask-filter durability HUD" question (see Headline finding 7).
- `com/hbm/capability/HbmCapability.java` (472 lines; interface/field declarations skimmed, ~60 lines) —
  CE's shield/stamina/dash/`enableHUD` capability, confirmed distinct from `HbmLivingProps`
  (radiation/digamma) — two separate capabilities in CE, already split identically in this port (see
  below).
- `com/hbm/render/util/RenderOverhead.java` (520 lines; header/class declaration only, ~20 lines) —
  the VATS-overhead-health-bar + thermal-sight world-space renderer, confirmed real and confirmed
  **not** covered by any existing Phase 5 report (see Headline finding 8) — flagged, not solved, here.
- `com/hbm/entity/mob/{EntityBOTPrimeHead,EntityMaskMan,EntityUFO,EntityHunterChopper,
  EntityQuackos}.java` — grepped only (`BossInfo`), to confirm boss health bars are 100% vanilla
  plumbing already fully scoped by `docs/phase4/entities_bosses.md` (see Headline finding 6).

**Neo Edition (`upstream/neo-edition`), in full unless noted — confirmed real, apparently-compiling
NeoForge 1.21.1 code at the exact same `neo_version=21.1.228` this port targets, used strictly for API
shape per the task's ground rules, never for behavior/layout:**
- `com/hbm/render/util/RenderScreenOverlay.java` (91 lines, full) — the real 1.21.1 port of CE's
  Geiger-bar/crosshair renderer.
- `com/hbm/render/util/RenderInfoSystem.java` (139 lines, full) — an unrelated CE feature (transient
  on-screen info toast queue) kept only for its confirmed `RenderGuiEvent.Pre` + `GuiGraphics` +
  `Tesselator`/`BufferBuilder`/`GameRenderer::getPositionColorShader` colored-quad pattern.
- `com/hbm/items/IHUDItem.java` (11 lines, full), `com/hbm/items/weapon/sedna/hud/
  {IHUDComponent,HUDComponentAmmoCounter,HUDComponentDurabilityBar}.java` (11+55+46 = 112 lines, full).
- `com/hbm/items/weapon/sedna/GunBaseNTItem.java` (renderHUD at 446–475, read in full) — confirms
  neo-edition independently **fixed** CE's `bottomOffset` bug (see Headline finding 9).
- `com/hbm/main/NuclearTechModClient.java` (only the GUI-event section, lines 1–60 imports + 250–360,
  ~170 lines read) — `onRenderGuiPre(RenderGuiLayerEvent.Pre)` (262–267), `onRenderGuiPre(RenderGuiEvent.Pre)`
  (269–296), `onRenderGuiPost(RenderGuiEvent.Post)` (300+, including the confirmed Geiger-bar call site
  at 333–338 and `checkForGeiger` at 347–358).
- Whole-file greps: `RenderGuiLayerEvent|RegisterGuiLayersEvent|RenderGuiEvent` across neo-edition's
  entire `src/main/java` (29 hits, enumerated and triaged — **zero** hits for `RegisterGuiLayersEvent`,
  see Headline finding 5); `JetpackHandler|renderShieldBar|renderDashBar|getShield()|renderBadges|
  customGeiger|ArmorFSB\b` (zero hits — none of these CE features exist in neo-edition at all, so no
  cross-check is available for them beyond the primitives already confirmed by the files above).

**This port's own already-committed code (`src/main/java`), all read in full:**
- `interfaces/IItemHUD.java` (19 lines) — already ported against `RenderGuiLayerEvent.Pre`.
- `interfaces/IHoldableWeapon.java` (25 lines) — already ported against `GuiGraphics`, with its own
  javadoc already naming the confirmed neo-edition precedent this report also independently verified.
- `blocks/ILookOverlay.java` (65 lines) — already ported against `RenderGuiEvent.Pre`, `printGeneric`
  already implemented, `printHook` dispatch not yet wired (see Deferred scope).
- `render/misc/RenderScreenOverlay.java` (59 lines) — a narrow stub, `Crosshair` enum only, verbatim
  values, explicitly documented as waiting on this area's package.
- `capability/{HbmLivingProps,HbmLivingAttachment,HbmPlayerAttachment,ModAttachments}.java`
  (288+~250(unread body)+259+55 lines) — confirms every radiation/digamma/shield/stamina/dash field this
  area needs is real, already `.sync(...)`-attached, and already synced client-side (see Headline
  finding 3).
- `items/tool/{ItemGeigerCounter,ItemDosimeter,ItemDigammaDiagnostic}.java` (86+61+50 lines) and
  `util/ContaminationUtil.java` (684 lines; `getActualPlayerRads`/`getPlayerRads`/`getRads`/
  `calculateRadiationMod` at 74–83, 202–214, 319–323 read closely) — confirms the exact two distinct
  radiation quantities this area must not conflate (see Key risks 1).
- `items/gear/ArmorFSB.java` (486 lines), `items/armor/{ArmorHEV,ArmorFSBPowered,JetpackFueledBase,
  ArmorFSBFueled}.java` (21+144+94+132 lines) — confirms every server-side field the armor/jetpack HUD
  needs (`customGeiger`, `getCharge`/`getMaxCharge`, `getFuel`/`maxFuel`, `isBarVisible`/`getBarWidth`)
  is already real, and confirms `ArmorHEV`'s and `JetpackGlider`'s renderers are the only pieces missing.
- `items/weapon/sedna/{GunConfig,ItemGunBaseNT}.java` (180+487 lines) — confirms every field the gun
  HUD needs (`crosshair_DNA`/`getCrosshair`, `aimingProgress`, `getWear`, `lastShot`/`shotRand`) already
  exists, and pins the exact stub location (`ItemGunBaseNT.java:475–487`) this report's implementer
  fills in.
- `items/weapon/sedna/mags/IMagazine.java` (`getIconForHUD`/`reportAmmoStateForHUD` signatures, lines
  53/55) — confirms zero missing data for the ammo-counter component.
- `items/tool/ItemLaserDetonator.java` (`getCrosshair` at line 88) — confirms a real, already-registered
  `IHoldableWeapon` implementor with nothing yet calling its crosshair.
- `config/{ClientConfig,RadiationConfig}.java` — confirms `GEIGER_OFFSET_HORIZONTAL/VERTICAL`,
  `DIGAMMA_X/Y`, `BADGES_HUD`, `SHOW_BLOCK_META_OVERLAY`, `DODD_RBMK_DIAGNOSTIC`, `HEALTHBAR_HUD` are
  **already ported**, 1:1 with CE's config keys.
- Repo-wide greps: `RenderGameOverlayEvent` in CE (110 hits across 100+ files — this area's own
  ModEventHandlerClient/RenderScreenOverlay/IItemHUD/ILookOverlay/ArmorFSB/ArmorHEV/hud-package plus ~90
  other files whose `renderHUD`/`printHook` overrides are individual items/blocks scattered across every
  other phase, out of this report's scope but confirming the dispatcher pattern is universal); `Curios|
  curios|Baubles|baubles` in this port (4 hits, all in the detector-item javadocs documenting the
  already-made Phase 1 decision to drop Baubles/accessory-slot support); `InventoryUtil` in this port
  (no `hasItem`-style helper class exists yet — a trivial, named gap, see Deferred scope).
- `docs/phase3/{gun_framework.md,fsb_armor_and_jetpacks.md,weapon_animation_hooks.md,
  armor_equippable_framework.md}.md`, `docs/phase4/{entities_bosses.md,entities_legacy_bullet_system.md,
  chunk_radiation_system.md}.md`, `docs/phase5/{weapon_gun_rendering_animloader.md,
  renderer_framework_and_obj_models.md,armor_humanoidmodel_rendering.md}.md` — grepped and the relevant
  sections read closely to establish this report's exact boundary against 7 sibling reports (3 already
  published in this Phase 5 wave, 4 from Phase 3/4) without duplicating any of them.

**File count this report is based on:** 26 files read in full or to closure on their relevant sections,
plus 9 sibling/predecessor reports cross-checked for boundary and non-duplication.

## Headline findings

1. **This port has already built essentially all of this area's server-side data layer.** Every field
   the HUD needs to read is already real, already committed, and already network-synced: radiation/
   digamma (`HbmLivingAttachment`, `.sync(HbmLivingAttachment.STREAM_CODEC)`, confirmed in
   `capability/ModAttachments.java:43–49`), shield/stamina/dash (`HbmPlayerAttachment`,
   `.sync(HbmPlayerAttachment.STREAM_CODEC)`, `ModAttachments.java:29–36`), gun wear/aim state
   (`GunDataComponents.GUN_STATES`, `.networkSynchronized(...)`, per
   `docs/phase5/weapon_gun_rendering_animloader.md`), armor charge/jetpack fuel
   (`ArmorDataComponents.ARMOR_CHARGE`/`JETPACK_FUEL`, per `docs/phase5/armor_humanoidmodel_rendering.md`
   finding 6), and the magazine HUD helpers (`IMagazine.getIconForHUD`/`reportAmmoStateForHUD`, already
   implemented). **This report's entire remaining scope is pure client-rendering code** (plus two small,
   named data-only additions: `GunConfig.hudComponents_DNA`/`getHUDComponents`/`hud(...)`, and wiring
   `ArmorFSB.handleOverlay`'s dispatch) **and asset copying** (see finding 4) — there is no
   missing-data blocker anywhere in this area, confirming the task's own framing.

2. **The real 1.21.1 API for this whole area is independently cross-confirmed by two sources, not
   guessed**: this port's own already-committed `IItemHUD`/`IHoldableWeapon`/`ILookOverlay` interfaces
   (which already picked `RenderGuiLayerEvent.Pre`/`RenderGuiEvent.Pre`/`GuiGraphics` before this report
   started) and neo-edition's compiling `RenderScreenOverlay`/`RenderInfoSystem`/`HUDComponentAmmoCounter`/
   `HUDComponentDurabilityBar`/`NuclearTechModClient` — read together, they give a complete, load-bearing
   answer for every rendering primitive this area needs: `GuiGraphics.blit(ResourceLocation, x, y, u, v,
   width, height)` (256×256-atlas-assumed overload, same UV convention as CE's own `overlay_misc.png`),
   `GuiGraphics.drawString(Font, ...)`, `GuiGraphics.renderItem(ItemStack, x, y)`,
   `guiGraphics.pose().pushPose()/translate/mulPose(Axis.ZP.rotationDegrees(...))/popPose()` for rotated
   elements (confirmed real via neo-edition's nuke-shake `pose().translate(...)` and
   `renderCustomCrosshairs`'s push/pop), and — for anything needing colored (non-textured) quads, like
   CE's shield/dash/armor-mod bars — `Tesselator.begin(VertexFormat.Mode.QUADS,
   DefaultVertexFormat.POSITION_COLOR)` + `BufferBuilder.addVertex(matrix, x, y, z).setColor(...)` +
   `BufferUploader.drawWithShader(buf.buildOrThrow())` under `RenderSystem.setShader(GameRenderer::
   getPositionColorShader)` (confirmed real via `RenderInfoSystem.onOverlayRender`, lines 89–97 — a
   structural 1:1 replacement for CE's `Tessellator`/`BufferBuilder.begin(GL11.GL_QUADS,
   DefaultVertexFormats.POSITION_COLOR)`/`buffer.pos(...).color(...).endVertex()`/`tessellator.draw()`
   idiom used throughout `ModEventHandlerClient.onOverlayRender`'s nuke-flash quad and `onHUDRenderBar`'s
   bar quads).

3. **Two distinct event families exist and this area needs both — but `RegisterGuiLayersEvent` (new
   custom layers) is used by neither CE nor either 1.21 codebase for anything in this area.** A
   whole-tree grep of neo-edition for `RenderGuiLayerEvent|RegisterGuiLayersEvent|RenderGuiEvent` returns
   29 hits and **zero** are `RegisterGuiLayersEvent`. The real pattern, confirmed end-to-end: (a)
   `RenderGuiLayerEvent.Pre`, filtered by `event.getName().equals(VanillaGuiLayers.HOTBAR)` /
   `.CROSSHAIR`, for anything that replaces or augments one specific named vanilla layer (ammo counter,
   durability bar, custom crosshair — these `event.setCanceled(true)` the vanilla crosshair exactly like
   CE's `RenderGameOverlayEvent.Pre` + `ElementType.CROSSHAIRS` + `event.setCanceled(true)` did); (b)
   `RenderGuiEvent.Pre`/`.Post` (whole-frame, fires once regardless of layer), for anything drawn at an
   absolute screen position independent of any single vanilla layer — neo-edition's own ported Geiger bar
   is drawn this way (`onRenderGuiPost`, unconditional), not layer-gated, which is the safer, simpler
   choice this report recommends for the Geiger/digamma bars, HEV overlay, shield bar, dash bar, and
   badges row (all of which CE itself draws at hardcoded absolute HUD-corner coordinates, never relative
   to another mod's layer). `VanillaGuiLayers.ARMOR_LEVEL`/`PLAYER_HEALTH` (the layers CE's own
   `ElementType.ARMOR`/`ElementType.HEALTH` map to) are **not** demonstrated in either codebase read here
   — flagged as well-established-but-unverified-in-this-sandbox (see Open questions); the `RenderGuiEvent.
   Post` fallback above sidesteps needing to know their exact names at all.

4. **HUD texture assets have not been copied into this port yet — a real, concrete, but trivial gap.**
   `find` confirms `assets/hbm/textures/misc/{overlay_misc,overlay_digamma}.png` and
   `assets/hbm/textures/gui/hud/jetpack_hud_{small,large,small_text}.png` exist in CE's resources but
   this port's `src/main/resources` has **no `textures/` tree at all yet** (confirmed: the whole
   `assets/hbm` tree under this port today is just `multiblock_bounds/`). This is not unique to this
   area — it is a project-wide "no textures ported yet" state — but is named here because every single
   HUD element this report describes (all of `RenderScreenOverlay`'s methods, both jetpack HUD variants)
   is a `png` blit and literally cannot render, even once the Java code is written, until these 5 files
   (plus whatever `armor_hev`/`hev_*` textures `ArmorHEV`'s in-world model needs, out of this area's
   scope) are copied over. Flagging so implementation doesn't stall rediscovering this mid-task.

5. **The Geiger-counter HUD bar is real, exists in CE, and is not the same thing as `RadVisOverlay` —
   making `docs/phase4/chunk_radiation_system.md`'s deferred RadVisOverlay recommendation concrete.**
   That report asked whether CE has "any other real player-facing radiation HUD element besides
   RadVisOverlay" — it does, and there are **three**, all real, all already fully supported by real
   server-side data, and **none** of them touch `RadiationSystemNT.worldMap` or need
   `Minecraft.getIntegratedServer()` the way `RadVisOverlay` does: (a) `RenderScreenOverlay.
   renderRadCounter`/`renderDigCounter` — a persistent hotbar-corner gauge bar + color-coded warning icon
   + "N RAD/s" text, gated on carrying a `geiger_counter`/`digamma_diagnostic` item anywhere in inventory,
   reading the player's own accumulated dose (`HbmLivingProps.getRadiation`/`getDigamma`, both real,
   already synced); (b) `ArmorHEV.renderOverlay` — the HEV suit's built-in ASCII-art gauge, which
   **replaces** (not augments) the vanilla health/armor HUD entirely while worn, reading the same
   accumulated-dose field; (c) the already-fully-ported chat/sound feedback (Phase 4). **All three read
   only the player's own already-synced attachment data — nothing here needs `RadiationSystemNT`
   internals, in-process or otherwise.** This directly supports Phase 4's "skip `RadVisOverlay`" option:
   the real player-facing radiation-feedback feature set this game needs is fully covered by (a)+(b)+(c)
   above, all buildable today, all multiplayer-correct by construction; `RadVisOverlay`'s pocket-boundary
   debug visualization has zero bearing on it and can be dropped or replaced by a debug command with no
   loss to the actual player experience.

6. **CE has no HUD element for gas-mask-filter durability at all — the task's framing here does not
   survive contact with the real code.** `ArmorUtil.describeFilter` (this port, 372 lines; CE's
   equivalent read in full) exposes installed-filter remaining durability **exclusively as tooltip text**
   (`"§6installed filter: <name> (<damage>/<max>)"`-shaped lines, shown on hover), plus whatever vanilla's
   automatic per-`ItemStack`-damage durability bar draws under the filter's own inventory slot icon (a
   stock vanilla mechanism, not a CE HUD element) — there is no persistent on-screen filter-charge gauge
   anywhere in CE, confirmed by a full read of `ArmorUtil.java`'s gas-mask section and a whole-CE-tree
   grep for `filter` finding no `RenderGameOverlayEvent`/`IItemHUD` consumer of it. This port's own
   `ArmorUtil.java` (already ported, tooltip-only) already matches this exactly — **zero HUD work is
   needed for gas-mask filters**, and none should be added (it would be new-content invention, not a
   port). Armor's actual durability-bar HUD mechanic in CE is the vanilla `showDurabilityBar`/
   `getDurabilityForDisplay` override family (1.21: `isBarVisible`/`getBarWidth`), already ported on
   `ArmorFSBPowered` (this port, confirmed at lines 117–125) for the charge-as-durability-bar look — that
   one **is** real "armor durability HUD" work, already done, one layer below this report's own scope.

7. **Boss health bars are not this area's job and are already fully resolved elsewhere.**
   `docs/phase4/entities_bosses.md` (already published) confirms all 5 of CE's boss-bar-tagged entities
   use plain vanilla `BossInfo`/`IBossDisplayData` (1.21: `ServerBossEvent`/`BossEvent.BossBarColor`), a
   server-only construct whose client-side bar is drawn automatically by vanilla's own boss-bar overlay
   with **zero custom rendering code needed anywhere**. This report's own re-grep of the 5 boss classes
   confirms the same finding independently. Nothing in this area needs to (or should) touch boss health
   bars; the task's "any boss-health ... overlay" phrase is answered by "already fully scoped, zero
   Phase-5-rendering work, see the cited report" rather than anything new here.

8. **A real, sizeable, currently-unclaimed HUD system was discovered in the course of drawing this
   area's boundary: `RenderOverhead.java` (520 lines) — VATS overhead entity health bars + a
   world-space thermal-sight overlay.** Gated by `ArmorFSB.vats`/`ArmorFSB.thermal` (both already-ported
   fields on this port's `ArmorFSB.java`) and `ClientConfig.HEALTHBAR_HUD` (already ported), triggered
   from `ModEventHandlerClient`'s `preRenderEvent`/`RenderPlayerEvent.Post` hooks (CE lines 578–599,
   710–730) — i.e. a `RenderLivingEvent`-family hook drawing floating health-bar segments over nearby
   mobs, plus a full-viewport thermal-vision color-remap effect keyed off `ItemGunBaseNT.hasThermalSights`
   and worn-armor `thermal` flags. **This is not named in this report's task brief** (which lists Geiger/
   armor-durability/gun-ammo/jetpack/boss-health/radiation-exposure, not VATS/thermal-sight) and was not
   found claimed by any of the 3 other already-published Phase 5 reports checked
   (`armor_humanoidmodel_rendering.md` only confirms the *server-side* `vats`/`thermal` flags are already
   real, not the renderer). Flagged here as a genuine, real, unclaimed gap for whoever scopes it next —
   not investigated further, per this report's own boundary, beyond confirming its existence, size, and
   real data dependencies.

9. **CE has a real bug in its multi-HUD-component vertical stacking that this port should not
   reproduce — neo-edition already fixed it, independently, and this report recommends following the
   fix.** `ItemGunBaseNT.renderHUD` (CE, 458–484) declares `int bottomOffset = 0;` **inside** the
   per-component loop (line 477, re-declared every iteration), so every `IHUDComponent` in a gun's
   `hud(...)` list always renders at `bottomOffset = 0` — the accumulation (`bottomOffset +=
   component.getComponentHeight(...)`, line 479) has no effect on the next iteration. In practice CE's
   own 2 shipped components (`HUDComponentAmmoCounter`, which uses `bottomOffset`;
   `HUDComponentDurabilityBar`, which ignores it and hardcodes its own fixed Y) don't visibly collide, so
   this reads as latent dead code rather than a visible bug — but it means a 3rd component, or two
   `HUDComponentAmmoCounter`s on the same gun (a dual-magazine weapon), would overlap. Neo Edition's
   `GunBaseNTItem.renderHUD` (446–475) declares `bottomOffset` **outside** the loop (line 468), correctly
   accumulating — confirmed by direct read. Recommend this port's `ItemGunBaseNT.renderHUD`
   implementation (filling the stub at `ItemGunBaseNT.java:475–487`) copy neo-edition's corrected
   version, not CE's.

10. **Two entirely separate jetpack HUD mechanisms exist in CE, not one, and this port has already
    built the server-side item shells for both, in both cases sans any renderer.** (a) `JetpackGlider`
    (`items/gear/JetpackGlider.java`, this port, already ported per `docs/phase3/fsb_armor_and_jetpacks.md`)
    uses CE's bespoke `JetpackHandler.renderHUD` (429–570) — two independently-rotated needle-style gauge
    quads (thrust + fuel) drawn over a dial-face texture, in a "compact" (50×N px) or "large" (80×80px)
    variant selected by `JetpackInfo.useCompactHUD`, plus a 4-state status-icon swap
    (idle/active/hovering/failure) — entirely bespoke, entirely absent from the generic bars-list
    mechanism. (b) `JetpackFueledBase` (this port's `items/armor/JetpackFueledBase.java`, already ported,
    confirmed `getFuel`/`maxFuel` real) is one of the ~13 armor-mod items `ModEventHandlerClient.getBars`
    (CE, 998–1015) feeds into the **same generic horizontal bars-list mechanism** `ArmorFSBPowered`'s
    charge readout uses (`onHUDRenderBar`, 1017–1114) — a small colored fill-bar drawn next to the vanilla
    armor icons, not a rotary gauge at all. **These are not interchangeable and not the same feature**:
    (b) is by far the cheaper build (reuses the shield/charge-bar quad-drawing code this report already
    has to write for the shield bar) and should be built first; (a) is a genuinely bespoke, higher-effort
    asset-heavy widget (3 dedicated textures, rotation math, 4-state icon logic) that can be deferred
    independently without blocking (b).

## Confirmed 1.21.1 API surface

All of the following are read directly from real, compiling source (this port's own already-committed
interfaces, or neo-edition at the exact `neo_version` this port targets) — nothing here is invented:

| CE 1.12.2 | Confirmed 1.21.1 replacement | Source |
|---|---|---|
| `RenderGameOverlayEvent.Pre` + `ElementType` enum | `RenderGuiLayerEvent.Pre` (per-named-layer, `event.getName()` is a `ResourceLocation` matched against `VanillaGuiLayers.HOTBAR`/`.CROSSHAIR`) **or** `RenderGuiEvent.Pre`/`.Post` (whole-frame, no layer filter) | This port's `IItemHUD.java`/`ILookOverlay.java` (already committed); neo-edition `NuclearTechModClient.java:262,269-296,300-358`, `HUDComponent*.java` |
| `ScaledResolution` | `Minecraft.getInstance().getWindow()` (`Window.getGuiScaledWidth/Height()`), or `GuiGraphics.guiWidth()/guiHeight()` | neo-edition `RenderScreenOverlay.java:46`, `HUDComponentAmmoCounter.java:44` |
| `Gui.drawTexturedModalRect(x, y, u, v, w, h)` | `GuiGraphics.blit(ResourceLocation, x, y, u, v, width, height)` — same 256×256-atlas-assumed pixel-space UV convention as CE, confirmed by neo-edition reusing CE's own `overlay_misc.png` unmodified | neo-edition `RenderScreenOverlay.java:48-56`, `HUDComponentDurabilityBar.java:43-44` |
| `FontRenderer.drawString`/`drawStringWithShadow` | `GuiGraphics.drawString(Font, String\|Component, x, y, color[, dropShadow])` | neo-edition `RenderScreenOverlay.java:62-66`, this port's `ILookOverlay.java:57-61` |
| `RenderItem.renderItemAndEffectIntoGUI(EntityPlayer, ItemStack, x, y)` | `GuiGraphics.renderItem(ItemStack, x, y)` (no lighting-state push/pop needed — folded in) | neo-edition `HUDComponentAmmoCounter.java:53` |
| `GlStateManager.pushMatrix/translate/rotate/popMatrix` (for a rotated/offset quad) | `guiGraphics.pose().pushPose()/translate(x,y,z)/mulPose(Axis.ZP.rotationDegrees(deg))/popPose()`, applied **before** the `blit`/`drawString` call (GuiGraphics's own pose stack transforms subsequent draws) | neo-edition `NuclearTechModClient.java:281` (nuke-shake translate), `RenderScreenOverlay.java:81-89` (crosshair blend push/pop) |
| `Tessellator`/`BufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR)` (untextured colored quads — CE's shield/dash/armor-mod bars) | `RenderSystem.setShader(GameRenderer::getPositionColorShader)` + `Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR)` + `BufferBuilder.addVertex(matrix, x, y, z).setColor(r,g,b,a)` + `BufferUploader.drawWithShader(buf.buildOrThrow())`, matrix from `guiGraphics.pose().last().pose()` | neo-edition `RenderInfoSystem.java:89-97` (a live, compiling example of exactly this quad-drawing idiom, for an unrelated feature) |
| `GlStateManager.enableBlend()`/`tryBlendFuncSeparate(...)` | `RenderSystem.enableBlend()`/`blendFuncSeparate(GlStateManager.SourceFactor.X, GlStateManager.DestFactor.Y, ...)` — same enum names, `com.mojang.blaze3d.platform.GlStateManager` now holds only the factor enums, not the whole state-machine API | neo-edition `RenderScreenOverlay.java:82-87` |
| `net.minecraftforge.client.GuiIngameForge.left_height`/`right_height` (a shared, community-respected static stacking counter other Forge HUD mods increment so multiple mods' hotbar-companion bars don't overlap) | **No equivalent found in either codebase.** NeoForge's `LayeredDraw`/`VanillaGuiLayers` system has no public mutable per-side stacking counter that other mods are expected to cooperate on. | Not found in neo-edition (searched: no `left_height`/`right_height`/similar identifier anywhere) — see Key risks 2 |
| `IItemHUD`/`ILookOverlay` custom interfaces | Unchanged in shape, only the event type swapped — already ported 1:1 in this port | This port's own `interfaces/IItemHUD.java`, `blocks/ILookOverlay.java` |

**Not found anywhere, in either tree, for this area**: `RegisterGuiLayersEvent` (registering a brand
new named `LayeredDraw.Layer`). Every single HUD element in scope here — CE's and neo-edition's alike —
hooks an *existing* vanilla layer or the whole-frame event instead. Recommend this report's
implementation do the same rather than reaching for `RegisterGuiLayersEvent` on the (unverified)
assumption it's needed; nothing read here needs it.

## Area write-ups

### A. Geiger / digamma persistent HUD bars

CE: `RenderScreenOverlay.renderRadCounter`/`renderDigCounter` (47–162), dispatched from
`ModEventHandlerClient.onOverlayRender`'s `ElementType.HOTBAR` branch (818–836), gated on
`Library.hasInventoryItem(player.inventory, ModItems.geiger_counter) || hasBauble(...)` (the Baubles
half is a dead branch for this port per the already-made Phase 1 no-Curios decision — see Sources).
Reads `Library.getEntRadCap(player).getRads()` — **the accumulated total-body dose pool**
(`HbmLivingProps.getRadiation`/this port's `HbmLivingAttachment.getRads()`), sampled once per 1000ms and
displayed as the *delta* since the last sample (`lastRadResult - prevRadResult`), i.e. it turns an
accumulator into an approximate rate display. **This is not the same field as the Geiger click-sound or
Dosimeter/Digamma-diagnostic readouts**, which read `ContaminationUtil.getActualPlayerRads`/`getPlayerRads`
(`radBuf` + neutrons×20, this port's `ItemDosimeter.getReceivedRads`/`ContaminationUtil.getPlayerRads`) —
see Key risks 1 for why this distinction matters and is easy to get backwards. Also gated (radiation-only)
on `!(ArmorFSB.hasFSBArmorHelmet(player) && helmet.customGeiger)` — i.e. suppressed while wearing the HEV
helmet (area B below takes over). The digamma variant is ungated by that check (no armor suppresses it)
and reads a separate accumulated field (`HbmLivingProps.getDigamma`), driven by `digamma_diagnostic` item
presence and `RadiationConfig.DIGAMMA_X/Y` position config (already ported).

### B. HEV suit built-in radiation gauge (`customGeiger`)

CE: `ArmorHEV.handleOverlay` (57–71) + `renderOverlay` (77–180) — the **only** real override of
`ArmorFSB`'s no-op `handleOverlay` hook anywhere in CE. While worn (`hasFSBArmorIgnoreCharge`), cancels
vanilla's `ElementType.ARMOR` HUD entirely and, on `ElementType.HEALTH`, cancels that too and substitutes
a from-scratch text readout: current health×5 ("+N"), average armor-piece charge percentage ("‖N"), and
a 10-segment ASCII radiation bar (`☢ [.. || ...]`) built from the same accumulated-dose field as area A,
plus the same 1-second-sampled RAD/s delta text. This is the single feature that makes `ArmorFSB.
customGeiger`/`setHasCustomGeiger(true)` (set on the HEV helmet, `ModItems.java:643` — this port's
equivalent registration not grepped here, out of item-registration scope) meaningful: it's not a
*different* Geiger mechanism, it's "this specific suit's own screen replaces the normal HUD/Geiger
readout." **Dispatch requirement**: `ArmorFSB.handleOverlay` needs a real call site — CE calls it twice,
redundantly, from `ModEventHandlerClient.onOverlayRender` (lines 948-950 and 962-964 in CE, both
identical `if (helmet.getItem() instanceof ArmorFSB) ((ArmorFSB) helmet).handleOverlay(event, player)`)
against the equipped **helmet** slot specifically — this port's `ArmorFSB.java` already has the `customGeiger`
field and tooltip text (confirmed) but **no `handleOverlay` method or dispatch call exists yet** — this is
new, small, real work this report claims.

### C. Gun ammo counter / durability bar HUD components (Sedna)

Covered in full by CE's `HUDComponentAmmoCounter`/`HUDComponentDurabilityBar` (71+52 lines) and neo-edition's
1.21.1 port of the same (55+46 lines, confirmed real). `GunConfig.getHUDComponents(stack)` returns a
per-gun-config `IHUDComponent[]` (empty/null for most guns — only a handful of CE guns call `.hud(...)`
at all, e.g. dual-mag weapons wanting two `HUDComponentAmmoCounter(receiver)` calls with `.mirror()` on
one side); `ItemGunBaseNT.renderHUD` (this port's stub, `ItemGunBaseNT.java:475–487`) iterates
`getConfigCount()` configs × each config's components, calling `renderHUDComponent` (filter:
`event.getName().equals(VanillaGuiLayers.HOTBAR)`, matching CE's `ElementType.HOTBAR` check) and
`getComponentHeight` per component — see Headline finding 9 for the accumulation-bug fix to apply while
porting. The crosshair half of the same method (`event.getName().equals(VanillaGuiLayers.CROSSHAIR)` →
`event.setCanceled(true)` → `RenderScreenOverlay.renderCustomCrosshairs(guiGraphics,
config.getCrosshair(stack))`, gated on `!(config.getHideCrosshair(stack) && aimingProgress >= 1F)`) needs
no new data at all — `crosshair_DNA`/`getCrosshair`/`hideCrosshair_DNA`/`getHideCrosshair` and the static
`aimingProgress`/`prevAimingProgress` fields all already exist on this port's `GunConfig`/`ItemGunBaseNT`
(confirmed by direct read). **One small data-layer addition remains, shared with
`docs/phase5/weapon_gun_rendering_animloader.md`'s own Phase-5-safe scope item**: `GunConfig.
hudComponents_DNA`/`getHUDComponents`/`hud(...)` needs adding back (currently absent, documented in this
port's `GunConfig.java`'s own class javadoc as deliberately deferred pending this exact package) — either
report's implementer can add it; it's a ~5-line mechanical addition following the already-present
`crosshair_DNA` field's exact shape, not a design decision.

### D. Legacy gun (`ItemGunBase`) crosshair + `IHoldableWeapon` dispatch

CE's non-Sedna `ItemGunBase.renderHUD` (716–762) has the same two-part shape (HOTBAR ammo counter via
`RenderScreenOverlay.renderAmmo`/`renderAmmoAlt`; CROSSHAIRS via `IHoldableWeapon.getCrosshair()`/
`hasCustomHudElement()`/`renderHud(...)`). Per `docs/phase4/entities_legacy_bullet_system.md`'s own
scoping (the only 2 legacy guns kept in this port at all, `gun_supershotgun`/`gun_vortex`, are
explicitly recommended to **not** get live ammo/mag/reload wiring), the HOTBAR ammo-counter half of this
dispatch is effectively moot for this port today — nothing will ever feed it real mag/belt data. The
CROSSHAIRS half is **not** moot: `IHoldableWeapon` (already ported, `interfaces/IHoldableWeapon.java`)
has a real, already-registered implementor (`ItemLaserDetonator`, confirmed `getCrosshair()` override at
line 88) with **no dispatcher calling it anywhere yet** — this is real, small, in-scope work: a
`RenderGuiLayerEvent.Pre` listener (or a shared per-item dispatch loop, matching CE's own "check main
hand, then off hand, for `instanceof IItemHUD`" pattern at `ModEventHandlerClient.java:812-816`) that
finds the held `IHoldableWeapon`/`IItemHUD` item and calls its render hook. `RenderScreenOverlay.
renderAmmo`/`renderAmmoAlt` (CE, 202–256) are still worth porting as general-purpose helper methods (any
future non-Sedna weapon item, or `ItemGunStinger`'s `renderStingerLockon` sibling — `ItemGunStinger`
itself does not exist in this port yet, confirmed by `find`) even though no current call site needs them
urgently.

### E. Jetpack HUD (two systems — see Headline finding 10)

**`JetpackGlider`** (bespoke rotary gauge): `JetpackHandler.renderHUD` (CE, 429–570) is entirely
self-contained rendering logic over already-real data (`JetpackInfo.thrust`/`prevThrust`,
`getTank(player).getFluidAmount()`/`getCapacity()`, `info.failureTicks`, `jetpackActive(p)`/
`isHovering(p)`) — this port's `items/gear/JetpackGlider.java` item shell already exists (confirmed,
Phase 3) with its renderer explicitly deferred to Phase 5 by its own javadoc. Two size variants
(`useCompactHUD` true/false) select between `jetpack_hud_small`(+`_text`)/`jetpack_hud_large` textures
(not yet copied into this port, see Headline finding 4); both use the confirmed
`pose().translate/mulPose(rotationDegrees)/blit` idiom (Headline finding 2/API table) for the two
independently-rotating needle quads (thrust angle: `clamp(thrust*100-27, -27, 200)`; fuel angle:
`(fluidAmount/capacity)*227-27`).

**`JetpackFueledBase`/electric jetpacks** (generic bars-list): `ModEventHandlerClient.getBars` (CE,
998–1015) — `if (stack.getItem() instanceof JetpackFueledBase jetpack) { float fuel = (float)
JetpackFueledBase.getFuel(stack) / jetpack.maxFuel; bars.add(Pair(fuel, jetpack.fuel.getColor())); }` —
feeds the exact same bar-drawing code path as `ArmorFSBPowered`'s charge readout (area F below). This
port's `items/armor/JetpackFueledBase.java` already has `getFuel`/`maxFuel` real (confirmed by direct
read) — building this half requires zero new server-side work and should be built alongside/as part of
the shield-bar/bars-list mechanism, not the rotary-gauge mechanism.

### F. Shield bar / dash bar / armor-mod bars-list

`RenderScreenOverlay.renderShieldBar` (369–412, CE) draws a single labeled bar (`props.getShield()`/
`getEffectiveMaxShield(player)`) positioned relative to `GuiIngameForge.left_height` (no 1.21 equivalent,
see Headline finding/API table — hardcode a fixed offset instead) on `ElementType.ARMOR`, gated on
`getEffectiveMaxShield(player) > 0`. This port's `HbmPlayerAttachment.getShield()`/`getEffectiveMaxShield
(Player)` (both confirmed real, lines 144–158 and 208–218) are exact 1:1 ports already, including the
`ItemModShield` armor-mod bonus lookup. `renderDashBar` (259–366, CE, marked `@Spaghetti` in CE's own
source — a real CE-authored annotation meaning "known-messy code, port carefully") draws a
row-of-3-per-row dash-charge indicator from `props.getStamina()`/`getDashCount()` (this port's
`HbmPlayerAttachment.getStamina()`/`getDashCount()`, both confirmed real) gated on `getDashCount() > 0`
on `ElementType.HOTBAR`. `onHUDRenderBar`'s generic bars-list (1017–1114, CE) iterates all 4 armor slots
+ their installed mods, collecting `(fraction, color)` pairs from `getBars` (charge readout +
`JetpackFueledBase` fuel, per area E) and draws them as a set of small colored fill-bars via the raw
colored-quad idiom (API table row 6) positioned under the vanilla armor icons on `ElementType.ARMOR`
`Post`. All three read only already-real, already-synced data; all three are pure rendering work.

### G. Badges HUD

`RenderScreenOverlay.renderBadges` (455–497, CE) — a small fixed-position row of up to 4 perk icons
(`GeneralConfig.true528()`/`trueExp()`/`MobConfig.trueRam()`/composite `true328`), gated on
`ClientConfig.BADGES_HUD` (already ported), drawn once on `ElementType.TEXT` via a dedicated
`@SubscribeEvent(priority = HIGHEST)` listener (`onRenderHUD`, 975–983) — CE's own comment there flags
`ElementType.TEXT` as a deliberate, slightly-hacky choice to avoid interfering with Xaero's minimap mod;
worth re-verifying that specific interaction concern is still meaningful in a NeoForge 1.21 mod-compat
context (probably yes if this pack still targets Xaero's-minimap-adjacent audiences, but not verified
here — out of this report's scope to decide). The 3-4 boolean gate methods (`true528`/`trueExp`/`trueRam`)
were not traced to their definitions in this pass — small, self-contained, low-risk lookup for whoever
implements this piece.

### H. Look-overlay (`ILookOverlay`) dispatch

This port's `blocks/ILookOverlay.java` (65 lines) already has `printGeneric` fully implemented (confirmed,
ported from neo-edition's own real version per its own javadoc) but, per its own javadoc, **nothing calls
`printHook` yet** — CE's dispatcher (`ModEventHandlerClient.onOverlayRender`, 839-866) raytraces the
player's look target on `ElementType.CROSSHAIRS` (gated `ClientConfig.DODD_RBMK_DIAGNOSTIC`, already
ported) and calls `printHook` on whichever of (held item, looked-at block) implements `ILookOverlay`,
plus a debug block-meta overlay (`ClientConfig.SHOW_BLOCK_META_OVERLAY`, already ported) and
`TileEntityRBMKBase.diagnosticPrintHook` (RBMK-specific, out of this report's scope — belongs with
whichever area owns RBMK block entities). Neo-edition's confirmed equivalent (`NuclearTechModClient.
onRenderGuiPre(RenderGuiEvent.Pre)`, 284-295) uses `mc.hitResult instanceof BlockHitResult` +
`InventoryUtil.getItemsFromBothHands(player)` — this port has no `InventoryUtil` helper class yet
(confirmed, `find`/`grep` both empty) — a small, named, trivial gap (a 2-line loop over main+off hand
`ItemStack`s) rather than a real blocker. This dispatcher is genuinely this report's scope (it's the
generic "look-at overlay" plumbing every `ILookOverlay` implementor across the whole mod needs, and this
area already owns the sibling generic dispatchers for `IItemHUD`/`IHoldableWeapon`), even though the
concrete implementors that will eventually use it (`CraneSplitterBlock`, per `blocks/network/
CraneSplitter.java`'s CE original) belong to other content areas.

## Phase-5-safe scope (buildable now, zero missing-data blocker)

| Item | Basis |
|---|---|
| `RenderScreenOverlay` (this port's own, replacing the 59-line `Crosshair`-only stub) — `renderRadCounter`/`renderDigCounter`/`renderCustomCrosshairs`/`renderAmmo`/`renderAmmoAlt`/`renderScope`/`renderShieldBar`/`renderDashBar`/`renderBadges`, all against `GuiGraphics` | Both CE (532 lines) and a partial neo-edition port (91 lines, radiation bar + crosshair only) read in full; every data source (`HbmLivingAttachment`, `HbmPlayerAttachment`, `ContaminationUtil`) already real and synced |
| `ArmorFSB.handleOverlay` real dispatch (currently a no-op) + `ArmorHEV.handleOverlay`/`renderOverlay` override | CE read in full (57-180); `customGeiger`/`hasFSBArmorIgnoreCharge`/`getCharge`/`getMaxCharge` all already real in this port |
| `ItemGunBaseNT.renderHUD` body (fills the stub at `ItemGunBaseNT.java:475-487`) — crosshair + `IHUDComponent` loop, using neo-edition's corrected (non-buggy) `bottomOffset` accumulation | CE + neo-edition both read in full; every field read (`crosshair_DNA`, `aimingProgress`, `getHUDComponents`) already real once the `GunConfig.hud(...)` slot (below) is added |
| `GunConfig.hudComponents_DNA`/`getHUDComponents`/`hud(...)` slot | Mechanical ~5-line addition matching the already-present `crosshair_DNA` shape; shared touchpoint with `docs/phase5/weapon_gun_rendering_animloader.md`, either report's implementer can add it |
| `com.hbm.items.weapon.sedna.hud.{IHUDComponent,HUDComponentAmmoCounter,HUDComponentDurabilityBar}` | Both CE and neo-edition read in full; `IMagazine.getIconForHUD`/`reportAmmoStateForHUD` and `ItemGunBaseNT.getWear`/`getConfig` already real |
| `IHoldableWeapon`/`IItemHUD` dispatch loop (held-item HOTBAR/CROSSHAIRS render hook, matching CE's "check main hand then off hand" pattern) | CE read in full (`ModEventHandlerClient.java:812-816`); `IItemHUD`/`IHoldableWeapon` already ported, `ItemLaserDetonator` already a real implementor waiting on this |
| `ILookOverlay` raytrace dispatcher (the missing half of an already-implemented interface) | CE (839-866) + neo-edition (284-295) both read; `printGeneric` already implemented in this port |
| `JetpackFueledBase` fuel bar via the generic bars-list mechanism (`getBars`/`onHUDRenderBar`) | `getFuel`/`maxFuel` already real; shares code with the shield-bar/charge-bar quad-drawing this report already builds |
| Copying the 5 confirmed HUD texture assets (`overlay_misc.png`, `overlay_digamma.png`, `jetpack_hud_{small,large,small_text}.png`) from CE's resources into this port's `assets/hbm/textures/{misc,gui/hud}/` | Files already exist in CE, byte-identical copy, no format conversion needed |
| A trivial `InventoryUtil.hasItem(Player, Item)` / `getItemsFromBothHands(Player)` helper (or inlining the 2-3 lines at each call site) | Needed by the Geiger-bar gating check and the look-overlay dispatcher; neo-edition's shape confirmed (`checkForGeiger`, `NuclearTechModClient.java:347-358`) |

## Deferred / blocked scope (named blocker, not guessed)

| Item | Blocked on |
|---|---|
| `JetpackGlider`'s rotary-dial HUD (`JetpackHandler.renderHUD` port) | Not blocked on missing data (all real) — deferred only by effort/priority relative to the cheaper `JetpackFueledBase` bars-list variant (Headline finding 10); genuinely optional to ship in a first Phase 5 pass |
| `RenderOverhead`'s VATS overhead health bars + thermal-sight overlay | **Newly-discovered, unclaimed scope** (Headline finding 8) — not blocked on any missing data (`vats`/`thermal`/`HEALTHBAR_HUD` all already real) but is a genuinely separate ~500-line system this report does not claim; needs its own owner/research pass, or an explicit decision to fold it into this area later |
| `RenderScreenOverlay.renderScope`'s `GunConfig.scopeTexture_DNA`/`getScopeTexture` field | Same shape as the already-noted-absent `O_SCOPETEXTURE` key in this port's `GunConfig.java` (its own javadoc already documents this gap) — a ~5-line mechanical addition, not a real blocker, but not yet done; `renderScope` itself (a full-screen textured quad, CE 414-453) has zero other dependency |
| Full parity for `renderBadges`'s `ElementType.TEXT`-vs-Xaero's-minimap interaction concern | Needs a real decision on whether this pack still targets minimap-mod compatibility in its 1.21 form — a product question, not a technical one, not resolved here |
| `TileEntityRBMKBase.diagnosticPrintHook`'s share of the look-overlay dispatcher | Belongs with whichever area owns RBMK block entities (Phase 2, already built server-side per multiple already-published reports) — this report's dispatcher should call it if/when that class exposes an equivalent hook, but does not itself design that hook |
| `CraneSplitterBlock`'s `ILookOverlay` implementation and any other concrete implementor's actual on-screen content | Belongs to whichever content area owns each concrete block/item; this report only owns the generic dispatch plumbing they call into |

## Key risks

1. **Two different "player radiation number" quantities exist and are extremely easy to swap by
   accident — this is the single highest-value correctness risk in this report.** The persistent HUD
   gauges (area A: `renderRadCounter`, `ArmorHEV.renderOverlay`) read the **accumulated total-body dose
   pool** (`HbmLivingProps.getRadiation`/`ContaminationUtil.getRads(Entity)`/`HbmLivingAttachment.
   getRads()`) sampled as a derivative. The click-sound cue and Dosimeter/Digamma-diagnostic on-demand
   readouts (already fully ported, Phase 3/4) read a **different** field: `ContaminationUtil.
   getActualPlayerRads`/`getPlayerRads` (`radBuf` + neutrons×20, resistance-multiplied). Both are real
   and already exist; using the wrong one for a given UI element will silently compile and silently show
   plausible-looking-but-wrong numbers (e.g. a HUD bar that never matches what the Dosimeter reports for
   the same instant) rather than crashing — verify against this report's exact citations (Area A/B
   write-ups) per element, not by pattern-matching "radiation" to any available getter.
2. **No NeoForge equivalent to Forge's `GuiIngameForge.left_height`/`right_height` shared stacking
   counter was found** (searched neo-edition's full source, confirmed absent). CE's shield bar and
   `onHUDRenderBar`'s armor-mod bars-list both assumed this convention so multiple Forge HUD-mod
   authors' extra hotbar-companion elements would stack without manual coordinate math. Without an
   equivalent, this port's shield/armor-mod-bars vertical position must be a fixed hardcoded offset
   (fine in isolation, but means this mod's HUD will not automatically avoid overlapping some other,
   unrelated NeoForge mod's own hotbar-companion HUD element the way two well-behaved Forge mods once
   would have) — a real, if minor, architecture regression versus CE's ecosystem behavior, not a bug in
   this report's own logic.
3. **`RenderOverhead` (Headline finding 8) is easy to accidentally half-build while implementing this
   area**, since its trigger conditions (`ArmorFSB.thermal`/`.vats`) live on the exact same `ArmorFSB`
   class this report already touches for `handleOverlay`/`customGeiger`. Implementers should resist the
   temptation to "just also wire up VATS/thermal while I'm in here" without a deliberate scope decision —
   it is a genuinely separate ~500-line rendering system (world-space, not 2D HUD-space) that deserves
   its own pass rather than an ad-hoc partial implementation bolted onto this report's work.
4. **Every element in this report is a `png` blit with no texture assets ported yet** (Headline finding
   4). This is a real, unavoidable prerequisite — Java code alone will compile and run but render nothing
   (or a missing-texture checkerboard) until the 5 named files are copied. Not a design risk, but a
   real sequencing dependency worth naming explicitly so it isn't discovered as a surprise mid-review.
5. **`VanillaGuiLayers.ARMOR_LEVEL`/`PLAYER_HEALTH` constant names/existence are well-established
   Minecraft/NeoForge 1.21.x knowledge, NOT verified against a real jar or a working example in this
   sandbox** (no cached NeoForge sources jar found; neither CE nor neo-edition demonstrates hooking
   these two specific layers). This report's own recommendation (area F: use `RenderGuiEvent.Post`
   instead, which **is** fully confirmed real) sidesteps needing to resolve this before implementation
   can start, but whoever implements the shield bar/armor-mod bars-list should know the layer-specific
   approach was not chosen for lack of confidence, not because it's wrong — either would work if the
   constant names are confirmed first.

## Open questions

- Should `RenderOverhead`'s VATS/thermal-sight system (Headline finding 8) be folded into this report's
  area on a future pass, or does it deserve its own dedicated Phase 5 research report? This report
  recommends the latter (it is large enough and different enough in kind — 3D world-space, not 2D
  screen-space — to warrant separate scoping) but does not decide it unilaterally.
- Are `VanillaGuiLayers.ARMOR_LEVEL`/`PLAYER_HEALTH` (or whatever the real constant names turn out to be)
  worth using for the shield bar/armor-mod bars-list once a real jar is available, instead of this
  report's recommended `RenderGuiEvent.Post` fallback? Either produces the same visual result for this
  mod's own elements; the layer-specific approach would only matter if better interop with *other* mods'
  armor/health-adjacent HUD elements is a goal.
- Does this pack still target Xaero's-minimap-style compatibility in its NeoForge 1.21 form (relevant to
  `renderBadges`'s CE-era `ElementType.TEXT` workaround, area G)? Not a technical question — flagged for
  whoever owns overall mod-compat decisions for this port.
- Where should the small `InventoryUtil.hasItem`/`getItemsFromBothHands` helper live — a new
  `com.hbm.util.InventoryUtil` class (matching neo-edition's naming), or inlined at each of this report's
  2-3 call sites? Low-stakes, not resolved here.
