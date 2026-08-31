# GUI screens survey: weapons/turrets, storage/logistics, satellites, vehicle cargo — Phase 5 research

Sibling report: `docs/phase5/gui_screens_survey_machines_processing.md` (power/reactor/processing
GUIs under `com.hbm.inventory.gui.machine.**`) — not present on disk at the time this report was
written (parallel research wave), so the directory-structure survey below is derived independently
rather than copied from it. If that report lands with a different account of the shared directory
layout, its account should win for anything under `com.hbm.inventory.gui.machine.**`; everything
below `com.hbm.inventory.gui` **outside** that package is this report's exclusive scope and does not
overlap it.

## Scope and directory structure (verified by `find`)

This port organizes GUI `Screen` classes under `src/main/java/com/hbm/inventory/gui/`:

```
com.hbm.inventory.gui/            9 screens: BatteryScreen, CrateScreen, DesignatorManualScreen,
                                   FluidTankScreen, LaunchPadScreen, LaunchPadRustedScreen,
                                   LaunchpadSoyuzScreen, SatCoordScreen, SatInterfaceScreen
com.hbm.inventory.gui.bomb/       9 screens: NukeBalefire/Boy/Custom/Fleija/Gadget/Man/Mike/N2/
                                   PrototypeScreen, NukeTsarScreen (+ NukeCasingClientRegistry)
com.hbm.inventory.gui.cart/       2 screens: MinecartCrateScreen, MinecartDestroyerScreen
com.hbm.inventory.gui.train/      2 screens: TrainCargoTramScreen, TrainCargoTramTrailerScreen
com.hbm.inventory.gui.turret/     2 screens: TurretScreen, TurretMobFilterScreen
                                   (+ TurretClientRegistry)
com.hbm.inventory.gui.machine/**  21 screens — sibling report's scope, not read in depth here
```

That is **24 screens in this report's scope** (9 + 9 + 2 + 2 + 2), all confirmed present and
compiling (each has a registered `MenuScreens`/`RegisterMenuScreensEvent` binding — see the
`*ClientRegistry` classes cited throughout). CE's own `com.hbm.inventory.gui` package (225 files,
`find upstream/hbm-ce/src/main/java/com/hbm/inventory/gui -iname "*.java" | wc -l`) is **not** a
1:1 map onto this port's 55-screen total — CE has far more concrete GUI classes than this port has
built screens, because (a) CE gives every one of its ~50 machine variants its own top-level
`GUIMachine*` file where this port consolidates several into one shared parametrized screen (see
Crate/Battery below), and (b) CE has entire GUI families (crane, pneumo-tube storage, drone network,
ammo/tool/casing/lead bags, file cabinet, tape drive) that this port has **not started at all**
server-side yet — no `BlockEntity`/`Menu` exists for them, so there is nothing to build a `Screen`
against (see Deferred scope). This report only surveys the 24 screens that exist; it does not attempt
a byte-for-byte census of everything CE has that this port lacks outside its named area.

**Corpus-wide asset fact, verified once and applicable to every finding below**: this entire port has
**zero** PNG textures anywhere in `src/main/resources` (`find src/main/resources -ipath
"*textures*" -iname "*.png"` → 0 hits, vs. CE's 495 under `textures/gui/**` alone). This is **not**
a Phase-5-specific or GUI-specific gap — block and item textures are equally absent (0 hits under
`textures/item`/`textures/block` too) — it is one, port-wide, not-yet-run asset-copy pass that
`PORT_SPEC.md:20` already commits to ("copy textures/sounds/lang from CE verbatim"). Every screen
in this report that already references a *correct* CE-matching `ResourceLocation` (Crate/Battery/
FluidTank/most bomb-family screens once fixed) will render correctly the moment that pass runs and
needs **no further screen-side code change**. Screens that reference a **wrong** path (see Headline
finding 2) need a one-line fix regardless of when the asset pass happens. This report distinguishes
the two everywhere below rather than lumping every screen under one generic "no textures yet" note.

## Sources read in full (this port)

- `src/main/java/com/hbm/inventory/gui/turret/TurretScreen.java` (73 lines), `TurretMobFilterScreen.java`
  (228 lines), `TurretClientRegistry.java` (29 lines)
- `src/main/java/com/hbm/blockentity/turret/TurretBaseBlockEntity.java` (grepped/read in sections:
  lines 1-140, 290-330, 450-500, 620-660 of ~700 total — button dispatch, mob-filter, biometric-chip
  whitelist, targeting logic), `TurretFritzBlockEntity.java` (grepped for its `FluidTankNTM` field)
- `src/main/java/com/hbm/items/machine/ItemTurretBiometry.java` (full, ~65 lines), `ItemTurretChip.java`
  (existence-confirmed via grep)
- `src/main/java/com/hbm/inventory/gui/CrateScreen.java` (43 lines), `BatteryScreen.java` (55 lines),
  `FluidTankScreen.java` (52 lines)
- `src/main/java/com/hbm/blockentity/machine/CrateBlockEntity.java` (lines 1-155 of the file, full
  `CrateType` enum with all 5 constants read)
- `src/main/java/com/hbm/inventory/gui/SatCoordScreen.java` (104 lines, full), `SatInterfaceScreen.java`
  (116 lines, full), `SatPanelClientState.java` (18 lines, full)
- `src/main/java/com/hbm/packet/toclient/SatPanelPayload.java` (121 lines, full), `src/main/java/com/hbm/
  packet/toserver/SatPanelActionPayload.java` (67 lines, full)
- `src/main/java/com/hbm/inventory/gui/LaunchPadScreen.java` (63 lines, full), `LaunchPadRustedScreen.java`
  (43 lines, full), `LaunchpadSoyuzScreen.java` (57 lines, full)
- `src/main/java/com/hbm/inventory/gui/DesignatorManualScreen.java` (117 lines, full)
- `src/main/java/com/hbm/inventory/gui/bomb/NukeBoyScreen.java` (26 lines, full), `NukeCustomScreen.java`
  (32 lines, full), `NukeCasingClientRegistry.java` (34 lines, full) — the other 7 bomb screens
  confirmed structurally identical by grep (see Bomb-family section)
- `src/main/java/com/hbm/inventory/gui/cart/MinecartCrateScreen.java` (37 lines, full),
  `MinecartDestroyerScreen.java` (35 lines, full)
- `src/main/java/com/hbm/inventory/gui/train/TrainCargoTramScreen.java` (46 lines, full),
  `TrainCargoTramTrailerScreen.java` (37 lines, full)
- `src/main/java/com/hbm/main/VehicleCargoClientRegistry.java` (55 lines, full)
- `src/main/java/com/hbm/inventory/container/turret/TurretMenu.java` (grepped for `BUTTON_*`
  constants and `clickMenuButton`), `src/main/java/com/hbm/inventory/gui/GuiInfoContainer.java`
  (lines 1-100 of the file — shared `drawElectricityInfo`/`drawCustomInfo` helpers every screen above
  calls)
- `src/main/java/com/hbm/inventory/fluid/tank/FluidTankNTM.java` (grepped: `renderTank`/
  `renderTankTooltip` signatures at lines 318-380, confirming the shared tank-widget convention)
- Existence/absence sweeps (`grep -rl`, `find`) confirming: no `Drone*` GUI/Menu, no `Crane*` GUI/Menu
  beyond `CraneSplitterBlock(Entity)`, no `BaseBarrel`/waste-drum/storage-drum `BlockEntity`, no
  `WeaponTable`/`Railgun`/`CompactLauncher`/`SoyuzCapsule`/`SoyuzLauncher` server-side classes, no
  `BedrockOreRegistry`, no `SatLaserPacket` equivalent name, no GUI texture PNGs anywhere in
  `src/main/resources`

## Sources read in full (CE, `upstream/hbm-ce`)

- `GUITurretBase.java` (347 lines, full) and all 11 concrete `GUITurret{Arty,Chekhov,Friendly,Fritz,
  HIMARS,Howard,Jeremy,Maxwell,Richard,Sentry,Tauon}.java` (full, 15-90 lines each)
- `GUITurretMobFilter.java` (219 lines, full)
- `GUICrateBase.java` (52 lines, full), `TileEntityCrate.java` (lines 1-100, 220-270 — constructor +
  all getters), `TileEntityCrate{Iron,Steel,Tungsten,Desh}.java` + `TileEntitySafe.java` (constructor
  lines, all 5)
- `GUIMachineBattery.java` (lines 1-100 of ~150)
- `GUIMachineFluidTank.java` (~60 lines, full)
- `GUIScreenSatCoord.java` (189 lines, full), `GUIScreenSatInterface.java` (288 lines, full)
- `GUILaunchPadLarge.java` (151 lines, full), `GUILaunchPadRusted.java` (126 lines, full),
  `GUILaunchpadSoyuz.java` (41 lines, full)
- `GUIScreenDesignator.java` (213 lines, full)
- `GUINukeBoy.java` (64 lines, full); texture-constant lines read for `GUINuke{Balefire,Custom,
  Fleija,Gadget,Man,Mike,N2,Prototype,Tsar,Solinium}.java` (all 10)
- `EntityMinecartCrate.java` (202 lines, full, including inner `ContainerCartCrate`/`GUICartCrate`),
  `EntityMinecartDestroyer.java` (lines 1-60 + inner-class signatures, ~225 total)
- `TrainCargoTram.java` (188 lines, full, including inner `ContainerTrainCargoTram`/its GUI class),
  `TrainCargoTramTrailer.java` (158 lines, full, same shape)
- `find`/`grep` sweeps of `upstream/hbm-ce/src/main/resources/assets/hbm/textures/gui/**` for every
  texture path cited below, and of `upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/*.java` for
  the full 225-file listing

---

## Headline finding 1 — the turret-family javadoc's own "cosmetic only" claim is wrong for 2 of 11 variants, and a fully-wired server feature has no UI at all

`TurretScreen.java`'s class javadoc claims CE's 11 concrete `GUITurret*` subclasses are "cosmetic
texture/position overrides only, no new behavior." Reading all 11 in full shows that is true for 8
of them (Chekhov/Friendly/Howard/Jeremy/Maxwell/Richard/Sentry/Tauon: texture swap + font-color
override, nothing else), but **false for 3**:

- **`GUITurretArty`** and **`GUITurretHIMARS`** each add a real interactive control absent from
  `GUITurretBase`: an 18×18 firing-mode toggle button at `(guiLeft+151, guiTop+16)` that cycles
  artillery/cannon/manual (Arty) or auto/manual (HIMARS) via `AuxButtonPacket(..., 0, 5)`, with a
  mode-indicator icon drawn from the shared texture atlas.
- **`GUITurretFritz`** adds a `FluidTankNTM` fuel-gauge widget (`tank.renderTankInfo`/`renderTank` at
  `guiLeft+134`) — the flame turret's 16,000 mB diesel tank is a real, fully-implemented field on
  this port's own `TurretFritzBlockEntity.java:54,59` (confirmed by reading it), but **`TurretScreen`
  — the single shared screen this port uses for every turret variant including Fritz — never renders
  it.** This is a genuine, currently-invisible-to-the-player gap, not a texture-only cosmetic one:
  the fuel level cannot be read from the GUI at all today. It is a small, well-scoped fix: the exact
  `renderTank`/`renderTankTooltip` calls already exist and are used identically by 5+ machine screens
  (`FluidTankNTM.java:318-380`; e.g. `MachineDieselScreen.java:40,47`) — this just needs
  `TurretScreen` to branch on `be instanceof TurretFritzBlockEntity` and call them.
- Arty/HIMARS's own gap is **not** a screen bug — this port has no `TurretArty`/`TurretHIMARS`
  `BlockEntity` at all yet (confirmed: `TurretBlockEntities`/`blockentity/turret/*.java` lists 10
  variants — Chekhov/Friendly/Fritz/Howard/HowardDamaged/Jeremy/Maxwell/Richard/Sentry/
  SentryDamaged/Tauon — with no Arty or HIMARS entry). That mode-toggle control is **blocked on**
  those two turret types landing server-side first (owner: whichever report/pass covers the
  remaining turret variants, a Phase 3-shaped gap surfacing here because their GUI is the only
  turret-family artifact this report happens to have read in full).

**The single biggest concrete, safe-to-build-now gap in the turret family**, however, is a different
one the javadoc doesn't mention at all: **CE's `GUITurretBase` has a second targeting mechanism this
port's `TurretScreen` never exposes.** `GUITurretBase.java:52-58,168-200` embeds a name-based
biometric whitelist editor directly in the base turret GUI — a text field plus cycle/add/delete
buttons that read/write player names on an `ItemTurretBiometry` "AI chip" sitting in inventory slot 0
(distinct from, and older than, the separate entity-type `mobFilter` that `GUITurretMobFilter`
edits). This port has **already fully ported the server-side mechanic** — `TurretBaseBlockEntity.java`
has `getWhitelist()` (line 454, reads the chip via `ItemTurretBiometry.getNames`), `addName(String)`
(line 470), `removeName(int)` (line 478), and `handleServer`'s dispatch already special-cases
`data.contains("del")`/`data.contains("name")` (lines 303-305) to call them — matching CE's own
`NBTControlPacket` contract byte-for-byte. **`TurretScreen` has no field, no buttons, and no display
for any of this.** The entire server-side plumbing sits unused. This is the cleanest "safe to build
now" item in this whole report: no new server code, no new packet, no textures required to be
*functional* (only to look like CE) — just an `EditBox` + a cycle-index + two buttons wired to the
already-working `TurretControlPacket` NBT keys `"name"`/`"del"`, the same pattern
`TurretMobFilterScreen` already established for the sibling mob-type filter.

Two smaller, confirmed-real turret gaps, both cosmetic/texture-dependent so lower priority:
CE's `stattrak` kill-tally bar (`GUITurretBase.java:296-315`, a 0-63-kill progress bar drawn in 5-kill
steps) is tracked server-side (`TurretBaseBlockEntity.java:136,234,268` — the field increments
correctly) but never rendered by `TurretScreen`; and CE's per-category target toggles draw small
10×10 lit/unlit icon overlays this port's screen has no equivalent for (it uses plain vanilla
`Button`s instead, which is a reasonable, already-documented simplification, not a bug).

## Headline finding 2 — 7 of the 24 screens reference a texture path that will *never* resolve, independent of when the asset-copy pass runs

Distinct from the corpus-wide "no PNGs exist yet" fact above: several screens' `ResourceLocation`
constants point at a path CE's own asset tree does not use anywhere, so copying CE's textures
"verbatim" (`PORT_SPEC.md:20`) will **not** fill them in without also fixing the constant. Verified
by diffing every screen's texture constant against a `find`/grep of the real file location in
`upstream/hbm-ce/src/main/resources`:

| Screen | This port's path | Real CE path | Problem |
|---|---|---|---|
| `LaunchPadScreen` | `textures/gui/bomb/gui_launch_pad.png` | `textures/gui/weapon/gui_launch_pad_large.png` | wrong folder **and** wrong filename |
| `LaunchPadRustedScreen` | `textures/gui/bomb/gui_launch_pad_rusted.png` | `textures/gui/weapon/gui_launch_pad_rusted.png` | wrong folder only |
| `LaunchpadSoyuzScreen` | `textures/gui/machine/gui_launchpad_soyuz.png` | `textures/gui/machine/gui_soyuz.png` | right folder, wrong filename |
| `TrainCargoTramScreen` | `textures/gui/train/gui_cargo_tram.png` | `textures/gui/vehicles/gui_cargo_tram.png` | wrong folder only |
| `TrainCargoTramTrailerScreen` | `textures/gui/train/gui_cargo_tram_trailer.png` | `textures/gui/vehicles/gui_cargo_tram_trailer.png` | wrong folder only |
| `MinecartDestroyerScreen` | `textures/gui/cart/gui_cart_destroyer.png` | `textures/gui/cart/gui_destroyer.png` | right folder, wrong filename |
| `MinecartCrateScreen` | `textures/gui/cart/gui_cart_crate.png` | **no dedicated file exists** — CE's real `EntityMinecartCrate.GUICartCrate` (its inner GUI class, confirmed by reading `EntityMinecartCrate.java:176-201`) reuses `textures/gui/storage/gui_crate_steel.png` directly | this port invented a path for an asset CE never made |

All 9 bomb-family screens (`NukeBoyScreen` etc.) and `TurretScreen`/`TurretMobFilterScreen` reference
**no texture constant at all** (pure flat-color panels) — a step behind the screens above, which at
least reference *a* path, right or wrong.

`MinecartCrateScreen` is the highest-value, lowest-effort fix in this entire report: point its
constant at `textures/gui/storage/gui_crate_steel.png` (the exact same file `CrateScreen`'s `STEEL`
`CrateType` already needs, per `CrateBlockEntity.java:59,97`) and it renders correctly for free the
moment the storage-texture subset of the asset-copy pass lands — no new PNG has to be sourced or
drawn for it specifically, because CE itself never drew one.

## Headline finding 3 — `SatInterfaceScreen` is a placeholder standing in for a real client-side live-map/scan/radar minigame, and the gap is much larger than its own javadoc says

`SatInterfaceScreen`'s javadoc is honest that it is "a reasonable, documented reconstruction ... not
a line-for-line port" because `GUIScreenSatInterface.java` was not in the file set read when it was
written. Having now read all 288 lines of the real file, the actual gap is substantially bigger than
a reconstruction caveat suggests:

- **Real dimensions are 216×216**, not this port's 200×150.
- CE's GUI is a **live, pannable, per-pixel-scanned 200×200 map** rendered directly from the
  **client's own already-loaded `Level`** (`player.world.getBlockState(...)`/`getHeight(...)`) —
  it is not, and does not need to be, driven by any server payload for the pixel data itself. Three
  distinct modes selected by `Satellite.InterfaceActions` flags: `HAS_MAP` (top-down terrain-color
  scan, one column of 200 pixels revealed roughly every 15ms via a `scanPos` sweep), `HAS_ORES` (same
  sweep, but samples the topmost non-air block per column via `OreDictionary`/`BedrockOreRegistry.
  getOreScanColor` plus a hardcoded `ore_bedrock_block` check), and `HAS_RADAR` (scans nearby entities
  in a ±100-block AABB every frame and draws them as 8×8 texture-indexed blips — mob=6, player=7,
  everything else=5; a `EntityMissileBaseAdvanced` branch is commented out/`TODO` in CE itself, so
  this port need not reproduce it). **WASD keys pan the tracked view center by 50 blocks** and reset
  the scan buffer — a real, currently entirely-unbuilt navigation mechanic.
- Clicking inside the map (when `InterfaceActions.CAN_CLICK`) sends `SatLaserPacket(x, z, freq)` —
  an **orbital laser-strike command**, not a generic "click" event. This port's rebuilt equivalent
  (`SatPanelActionPayload` with `satClickX`/`satClickZ` keys, dispatched server-side to
  `Satellite#onClick` — `SatPanelActionPayload.java:52`) is architecturally sound and already
  reaches the correct real CE dispatch target (`Satellite.onClick`, confirmed present and called
  identically), so **the wiring is not the gap** — only the click *surface* (a real scanned map) and
  its accompanying `techBleep` sound feedback (present in CE, absent here) are.
- A hover tooltip showing raw world X/Z under the cursor (`InterfaceActions.SHOW_COORDS`) exists in
  CE and has no equivalent here.

This is a genuine correction to the task's own framing: "SatPanelPayload-driven" accurately describes
how satellite **metadata** (type, color, interface-action flags, target coordinate, status text) and
**action dispatch** reach the screen and the server — that half is real, tested-against-CE-contract,
and already correct. It does **not** describe how the **map/scan/radar pixels** are produced in CE —
those come from the client sampling its own already-loaded world state directly, the same way a
vanilla F3+G chunk-border overlay would, with no additional network payload needed. Building this for
real needs: (a) `BedrockOreRegistry`-equivalent ore-scan-color data, confirmed **not ported** anywhere
in this port (`grep -rl BedrockOreRegistry src/main/java` → 0 hits) — a real, named, non-Phase-5
blocker for the ore-scan mode specifically (owner: whichever report covers `BedrockOreRegistry`/
ore-scanning, not found by this survey); (b) nothing new for the terrain-map or radar modes beyond
screen code, since client-side world/entity access is already trivially available
(`Minecraft.getInstance().level`, already used elsewhere in this port's own bare screens). `SatCoordScreen`
is much closer to CE already (see the smaller finding below) and does not share this gap.

## Headline finding 4 — `LaunchPadScreen` never draws a power-level fill bar at all, and is missing CE's status icons and 3D missile preview

Reading `GUILaunchPadLarge.java` in full (151 lines) against this port's `LaunchPadScreen.java`
(63 lines) surfaces gaps beyond the texture-path bug already listed in Finding 2:

- **Real `ySize` is 236**, this port's `imageHeight` is 222 — a 14px mismatch, the largest of any
  screen surveyed (see the vehicle-cargo off-by-2 pattern below for a much more consistent, smaller
  version of the same class of bug).
- CE draws a **vertical 16×52 fill bar** for reactor-style power display
  (`drawTexturedModalRect(guiLeft+107, guiTop+88-power, 176, 52-power, 16, power)`, `guiTop+88` is
  the bar's bottom). This port's `render()` only registers a hover-tooltip hitbox at
  `(leftPos+107, topPos+90, 18, 18)` via `drawElectricityInfo` — **wrong shape (18×18 square instead
  of a 16×52 vertical bar matching the two adjacent fuel/oxidizer tanks) and `renderBg` never draws
  any fill rectangle at all**, so today the power level is invisible in the GUI except via a
  mis-shaped, mis-positioned tooltip hitbox. This is a real bug independent of missing textures — a
  flat-color fallback fill rectangle would still be visibly wrong-shaped even before real art lands.
- Missing entirely: fuel-present/oxidizer-present/launch-ready 6×8 status icons at `(130,23)`/
  `(148,23)`/`(112,23)` (`launchpad.getFuelState()`/`getOxidizerState()`/`isMissileValid()` — gameplay
  status the player currently has no GUI-visible way to check at all), and a hover-cycling tooltip
  over the empty designator slot showing the 3 compatible designator item types.
- Missing entirely: CE renders a **live 3D preview of the loaded missile standing on the pad**
  inside the GUI (`ItemRenderMissileGeneric.renderers`, scaled per `ItemMissileStandard.formFactor`).
  This is **not** something `LaunchPadScreen` alone can add — it depends on whichever Phase 5 area
  builds the missile/weapon item-renderer registry (`docs/phase3/weapon_animation_hooks.md` already
  flagged the whole `ItemRenderMissileGeneric`-equivalent class as Phase 5 scope, not yet read by
  that report either). Flagged here as a cross-reference so whoever builds that renderer knows this
  GUI is a real, additional consumer of it, not just in-world missile item rendering.

`GUILaunchPadRusted.java` (126 lines, also read in full) has the equivalent status icons (hasCodes/
hasKey), an 8-digit pseudo-random "launch codes" readout keyed off the block's own position
(`new Random(pos.getX()*131071L + pos.getZ())` — a real, deterministic-per-block CE mechanic, not
randomized-per-render), a "Release Missile" click zone with a full warning tooltip, and the same 3D
missile-preview rendering (fixed to the doomsday-rusted missile item specifically). None of the
above (status icons, launch-codes readout, release button, tooltip, 3D preview) exist in this port's
`LaunchPadRustedScreen` — it is presently button-for-button and icon-for-icon a bare textured panel
plus vanilla slots only. `LaunchpadSoyuzScreen`'s own javadoc already candidly flags the same missing
3D-preview class of gap for the Soyuz pad and additionally has a smaller, CE-confirmed dimension
mismatch (this port's `imageHeight=240` vs. CE's real `ySize=244`, and a texture filename mismatch:
`gui_launchpad_soyuz.png` here vs. real `gui_soyuz.png`).

## Headline finding 5 — all 4 vehicle-cargo screens share one systematic off-by-2 height bug, plus 2 of them have folder-only texture-path bugs

The 4 screens the task specifically calls out (`TrainCargoTramScreen`, `TrainCargoTramTrailerScreen`,
`MinecartCrateScreen`, `MinecartDestroyerScreen` — all "just fixed this session" in the sense that a
`Screen` now exists and is registered where none existed before, per `VehicleCargoClientRegistry`'s
own javadoc) were cross-checked against CE's real inner-class GUI definitions
(`EntityMinecartCrate.GUICartCrate`, `EntityMinecartDestroyer.GUICartDestroyer`,
`TrainCargoTram`'s and `TrainCargoTramTrailer`'s own inner GUI classes — none of these are top-level
`GUI*.java` files in CE, which is why a naive top-level-directory grep for e.g. "GuiCartCrate" turns
up nothing; they live as `public static class` members inside the entity file itself). All four
slot-layout geometries (grid dimensions, player-inventory Y offset, hotbar Y offset) match CE
**exactly** — this port's own in-code comments citing them are accurate. Only the panel height and
the texture path are off, and the height error is the same +2 in all four cases:

| Screen | This port's `imageHeight` | Real CE `ySize` | Texture path issue |
|---|---|---|---|
| `TrainCargoTramScreen` | 206 | **204** | folder: `train/` should be `vehicles/` |
| `TrainCargoTramTrailerScreen` | 224 | **222** | folder: `train/` should be `vehicles/` |
| `MinecartCrateScreen` | 224 | **222** | invented path — should reuse `storage/gui_crate_steel.png` (Finding 2) |
| `MinecartDestroyerScreen` | 168 | **166** | filename: `gui_cart_destroyer.png` should be `gui_destroyer.png` |

`MinecartDestroyerScreen`'s underlying `MinecartDestroyerMenu` slot layout (2×9 "filter/pattern"
slots plus vanilla player inventory) matches CE's `ContainerCartDestroyer`, which uses CE's own
`SlotPattern` class (`com.hbm.inventory.SlotPattern`/its modernized `com.hbm.inventory.slot.
SlotPattern extends SlotItemHandler` sibling, both confirmed present in CE) rather than a plain
`Slot` — whether this port's `MinecartDestroyerMenu` uses an equivalent restricted slot type (to
correctly block manual item placement into what CE treats as a landmine-pattern template bank rather
than free storage) was **not verified in this pass** (this report focused on the `Screen`/render
side per its assigned area) and is flagged as an open question below for whoever owns
`MinecartDestroyerMenu` itself.

## Storage/logistics: `CrateScreen`/`BatteryScreen`/`FluidTankScreen` are essentially fully and
correctly ported already

Unlike every category above, this trio needed **no behavioral correction**. Full verification:

- `CrateBlockEntity.CrateType`'s 5 enum constants (`IRON`/`STEEL`/`TUNGSTEN`/`DESH`/`SAFE`) were
  diffed field-by-field against CE's 5 real `TileEntityCrate` subclasses' constructor calls
  (`TileEntityCrateIron/Steel/Tungsten/Desh.java` + `TileEntitySafe.java`) — every one of `slots`,
  `columns`, `rows`, `crateX/Y`, `playerInventoryX/Y`, `hotbarY`, `guiWidth/Height`,
  `inventoryLabelX`, `titleColor`, `inventoryLabelColor` matches exactly (CE's raw decimal color
  literals like `4210752` are confirmed equal to this port's `0x404040` hex literals — same value,
  different radix). `CrateScreen.renderBg` already calls `guiGraphics.blit(crate.getCrateType()
  .texture, ...)` with the exact right per-type texture path (`textures/gui/storage/gui_crate_*.png`,
  all 6 of which exist as real files in CE's asset tree, including the `SAFE`→`gui_safe.png` mapping
  which was spot-checked). **Nothing to fix here besides the pending asset-copy pass.**
- `BatteryScreen`: dimensions (176×166), texture path (`textures/gui/storage/gui_battery.png`), and
  the power-bar geometry (`(71, 69-52, 34, 52)`) all match `GUIMachineBattery.java` exactly. The one
  documented, deliberate drop (redstone-mode/priority buttons — no server-bound GUI-button packet
  infrastructure for this specific field yet, per this port's own javadoc) is real and honestly
  flagged. **Not previously documented**: CE's tooltip also shows a live charge/discharge rate
  (`battery.delta`, formatted "+NHE/s"/"-NHE/s" in green/red) alongside power/maxPower — this port's
  `drawElectricityInfo` call only shows the static power/maxPower fraction, dropping the rate
  readout. Minor, but worth naming since the existing javadoc's "dropped" list doesn't mention it.
- `FluidTankScreen`: dimensions (176×166), texture path (`textures/gui/storage/gui_tank.png`), tank
  widget position (`71, 69, 34, 52`), and the mode-icon blit (`151, 34/35, 18×18`, `tank.getMode()*18`
  V-offset) all match `GUIMachineFluidTank.java` exactly, including the mode-toggle click zone this
  screen already wires through `AuxButtonPacket`-equivalent dispatch.

The real gap in this category is **not** anything wrong with these 3 screens — it's that CE has
**4 separate fluid-storage GUI families** (`GUIMachineFluidTank`, `GUIStorageDrum`, `GUIWasteDrum`,
`GUIBarrel`) and this port has ported only the first. `BaseBarrel.java` exists as a `Block` only — no
`BlockEntity`, `Menu`, or `Screen` anywhere for barrel/waste-drum/storage-drum. **Blocked on**: those
3 fluid-storage `BlockEntity` types being built server-side first (a retroactive Phase 2/3-shaped gap
this report surfaces because it happened to be adjacent, not something Phase 5 screen work can
address on its own — there is no `BlockEntity` to build a `Menu`/`Screen` against yet).

## Satellite/designator bare-`Screen` pattern: architecturally sound, missing polish uniformly

`SatCoordScreen`, `SatInterfaceScreen`, `DesignatorManualScreen`, and `TurretMobFilterScreen` all
correctly follow the same "no backing `AbstractContainerMenu`" pattern CE itself uses (`GuiScreen`
subclasses opened directly via `Minecraft.setScreen`/CE's `player.openGui` for a client-only,
item-driven panel with no server-side slot inventory) — this is a legitimate, CE-matching
architectural choice, not a shortcut, and `TurretControlPacket`/`ItemControlPacket`/
`SatPanelActionPayload` all correctly reach the real CE-equivalent server dispatch targets. Comparing
`SatCoordScreen` and `DesignatorManualScreen` against their real CE originals (`GUIScreenSatCoord.
java`, `GUIScreenDesignator.java`, both read in full) surfaces the same small set of missing details
in **both**, suggesting a pattern worth fixing once and reapplying:

- **No sound feedback.** CE plays `HBMSoundHandler.buttonYes` on every flip/here-style button and
  `techBleep` on save/send in both screens. This port's equivalents play no sound at all on any
  button.
- **No input validation before send.** CE's `GUIScreenSatCoord` uses `NumberUtils.isCreatable(...)`
  and refuses to send if a field isn't a valid number. This port's `parse()` helpers silently fall
  back to `0` and always send.
- **Dimension mismatches**: `SatCoordScreen` is 176×110 vs. CE's real 176×126 (16px short);
  `DesignatorManualScreen` is 176×100 vs. CE's real 176×126 (26px short, the largest gap of the pair).
- **`SatCoordScreen` doesn't default its fields to the player's current position.** CE's
  `GUIScreenSatCoord.initGui()` pre-fills X/Y/Z from `player.posX/Y/Z` so the common case ("target
  where I'm standing") needs no typing. This port defaults to literal `"0"`/`"0"`/`"-1"` unless a
  `SatPanelPayload` already arrived with a matching target — an easy fix (`Minecraft.getInstance()
  .player` is already trivially reachable from a client-only `Screen`, same as `useCurrentPos()`
  already does in `DesignatorManualScreen`).
- **Auto-close-on-send behavior is inconsistent with CE, in both directions.** CE's `GUIScreenSatCoord`
  closes the screen after a valid send (`mc.player.closeScreen()`); this port's `SatCoordScreen.send()`
  does not close. CE's `GUIScreenDesignator` does **not** close after Save (it shows a 20-tick
  highlighted-button flash and stays open for repeat adjustments); this port's `DesignatorManualScreen
  .save()` calls `this.onClose()`, closing immediately. Neither direction is "more correct" absent a
  product decision — flagged as a real, verified discrepancy either way, not a guess.
- Hover tooltips per-button/per-field (CE has 5 in `GUIScreenDesignator` alone: flip-X, flip-Z, here,
  save, distance-field) are present in CE and absent from both of this port's reconstructions.

None of the above require any new server-side work — every fix is confined to the `Screen` class
itself, using widgets/APIs already used elsewhere in this same file set (`GuiInfoContainer`'s
`drawCustomInfo`, vanilla `SimpleSoundInstance`/`SoundEvents`, `Minecraft.getInstance().player`).

## Bomb-assembly screens: 9 present, 1 confirmed missing, all pre-texture flat panels

All 9 of this port's `bomb/Nuke*Screen.java` files (`Balefire/Boy/Custom/Fleija/Gadget/Man/Mike/N2/
Prototype/Tsar`) follow one consistent, simple pattern verified by reading `NukeBoyScreen.java` and
`NukeCustomScreen.java` in full and grepping the rest: a flat gray/light-gray panel (no texture
constant referenced at all — a step behind even the wrong-path screens in Finding 2) plus, for
`NukeCustom` specifically, a live text readout of 8 computed yield categories
(`tnt`/`nuke`/`hydro`/`bale`/`dirty`/`schrab`/`sol`/`euph`/`isFalling()`) pulled straight off the menu's
block entity — a reasonable, honest placeholder given no schematic art exists yet. `NukeCasingClientRegistry
.java` (34 lines, read in full) correctly registers all 9 against their respective `MenuType`s with
the right `bus = Bus.MOD` fix already applied (confirmed against this port's own established pattern
elsewhere, and consistent with what a real NeoForge 1.21.1 `RegisterMenuScreensEvent` — an
`IModBusEvent` — requires).

**CE has a 10th concrete bomb-casing GUI this port has not built at all: `GUINukeSolinium`**
(confirmed by reading its texture-constant line: `textures/gui/soliniumSchematic.png`). This is not
a screen-only gap — `grep -rln Solinium src/main/java` finds `ExplosionSolinium` (the solinium
detonation *effect*, already fully implemented per Phase 3/4 explosion-engine work) and several
particle/entity classes (`EntityCloudSolinium`, etc.) but **no** `NukeSoliniumBlock`,
`NukeSoliniumBlockEntity`, `NukeSoliniumMenu`, or anything else representing the physical,
assemblable solinium-bomb device itself. **Blocked on**: that device's `Block`/`BlockEntity`/`Menu`
triad landing first (owner: whichever pass covers `bomb`-package block entities — this is a
retroactive gap in bomb-device coverage, not a Phase 5 screen problem; the *explosion* half of
solinium is already done, only the *craftable device* half is missing).

Real CE texture paths for the 9 built casings are inconsistent **within CE itself** (some directly
under `textures/gui/`, e.g. `n2Schematic.png`/`gunBombSchematic.png`; others under `textures/gui/
weapon/`, e.g. `fatManSchematic.png`/`ivyMikeSchematic.png`/`tsarBombaSchematic.png`
/`fleijaSchematic.png`/`gadgetSchematic.png`/`fstbmbSchematic.png`) — this is CE's own real,
un-tidy layout, not a mistake to "fix" when the asset-copy pass eventually adds texture constants to
these 9 screens; each one's path should be copied verbatim from its real CE location (cited above),
not normalized into one folder the way this port's launch-pad/vehicle-cargo screens mistakenly did
(Finding 2).

## Weapons/missile-infra items confirmed genuinely absent server-side (not a screen gap)

Cross-checked against `com.hbm.inventory.gui`'s full CE listing and this port's own source tree —
none of the following have any server-side representation in this port yet, so there is no `Menu`/
`BlockEntity` for a Phase 5 screen to be built against: `GUIWeaponTable` (crafting-adjacent gun
customization bench), `GUIRailgun`, `GUICompactLauncher`, `GUISoyuzCapsule` (the Soyuz astronaut-cargo
capsule — distinct from the launch **pad**, which this port has via `LaunchpadSoyuzBlockEntity`/
`LaunchpadSoyuzScreen`), and `GUISoyuzLauncher` (the launcher-tower structure, also distinct from the
pad). All confirmed by `find`/`grep` turning up zero matches for their obvious class-name analogues
anywhere in `src/main/java`. These are named, real gaps but are **not owned by this report** — they
require server-side design decisions (block shapes, multiblock structure, recipe/crafting contract)
this report's ground rules explicitly place out of scope for a client/UX survey.

## Safe to build now (no server-side or cross-report dependency)

1. **Turret biometric-chip whitelist UI in `TurretScreen`** (Headline 1) — server side is 100% done
   and already dispatches correctly; needs an `EditBox` + cycle-index + 2 buttons only.
2. **`TurretScreen`'s Fritz-variant fuel-tank gauge** (Headline 1) — the exact render calls needed
   already exist and are used identically elsewhere (`FluidTankNTM.renderTank`/`renderTankTooltip`).
3. **All 7 wrong-path texture constants** (Headline 2) — one-line `ResourceLocation` string fixes,
   independent of whether/when the PNGs themselves land; `MinecartCrateScreen`'s fix additionally
   needs zero new art since it should reuse an asset another screen already requires.
4. **All 4 vehicle-cargo screens' `imageHeight` off-by-2** (Headline 5) — mechanical, verified-exact
   numeric fix (206→204, 224→222 ×2, 168→166).
5. **`LaunchPadScreen`'s missing power-fill bar and status icons**, and **`LaunchPadRustedScreen`'s
   missing status icons/launch-codes readout/release button/tooltip** (Headline 4) — all pure
   `GuiGraphics`/menu-field wiring, no new packets or server fields needed (`getFuelState()`/
   `getOxidizerState()`/`isMissileValid()`/`missileLoaded`-equivalent fields already exist server-side
   per the CE cross-reference — **confirm the exact field names on this port's `LaunchPadBaseBlockEntity
   `/`LaunchPadRustedBlockEntity` before wiring**, as this report read the CE originals in depth but
   only skimmed this port's block-entity side for these two).
6. **Sound feedback, input validation, correct default field values, and hover tooltips** across
   `SatCoordScreen`/`DesignatorManualScreen` (and by the same pattern, likely `SatInterfaceScreen`'s
   click handler) — no new infrastructure, same fix shape in all three.
7. **`SatCoordScreen`'s and `DesignatorManualScreen`'s dimension mismatches** — numeric-only fixes
   (110→126, 100→126).
8. **`LaunchpadSoyuzScreen`'s dimension (240→244) and texture-filename fix**
   (`gui_launchpad_soyuz.png`→`gui_soyuz.png`).
9. **`SatInterfaceScreen`'s terrain-map and radar modes** (Headline 3, the two sub-features that do
   **not** depend on `BedrockOreRegistry`) — genuinely buildable now since they only need the
   client's own already-loaded `Level`/entity list, which every other bare `Screen` in this report
   already demonstrates is trivially reachable. This is real, substantial new-render-code work (not a
   one-liner like the items above), not merely a texture/dimension fix.

## Blocked / deferred, with named owner

- **`SatInterfaceScreen`'s ore-scan mode** — blocked on a `BedrockOreRegistry`-equivalent
  ore-scan-color table, confirmed not ported anywhere in this port. Owner: whichever report covers
  ore-dictionary/bedrock-ore registration (not identified by this survey; `docs/phase4/
  ore_veins_and_bedrock_ores.md` covers world-gen placement of bedrock ores but this report did not
  re-open it to check for a scan-color table specifically).
- **`GUITurretArty`/`GUITurretHIMARS`'s mode-toggle control** — blocked on those 2 turret
  `BlockEntity` types not existing server-side at all yet. Owner: whichever pass covers the remaining
  turret variants (Phase 3-shaped gap).
- **`GUINukeSolinium`** — blocked on the solinium bomb device's `Block`/`BlockEntity`/`Menu` not
  existing (the *explosion effect* is already done). Owner: whichever pass covers bomb-device block
  entities.
- **`GUIWeaponTable`/`GUIRailgun`/`GUICompactLauncher`/`GUISoyuzCapsule`/`GUISoyuzLauncher`** —
  blocked on their respective server-side block/entity/recipe design not existing at all. Not owned
  by this report; needs a design decision, not just a screen.
- **CE's crane family** (`GUICrane{Boxer,Extractor,Grabber,Inserter,Router,Unboxer}`) — blocked on 5
  of 6 `BlockEntity`s not existing (only `CraneSplitterBlockEntity` exists, and per `docs/phase2/
  blocks_network_conveyor_crane.md:117-170,242-248` that report already flagged "no menu/screen
  framework exists at all" as a Phase 2 blocker — that framework now exists (this whole report is
  built on it), so the remaining blocker is purely the 5 missing crane `BlockEntity`s themselves).
- **Drone-network GUIs** (`GUIDrone{Crate,Dock,Provider,Requester}`) — blocked on drone-network
  server logic beyond the entity/item shells that already exist (`EntityDroneBase`/`EntityDeliveryDrone
  `/`EntityRequestDrone`/`ItemDrone`/`ItemDroneLinker` all present; no drone-crate/dock/provider/
  requester `BlockEntity`, `Menu`, or storage-matching logic found). Matches the task's own framing
  ("drone network GUIs once they land") exactly — confirmed not landed.
- **Fluid storage drum/barrel/waste-drum GUIs** — blocked on their `BlockEntity`s not existing
  (`BaseBarrel` is a `Block`-only stub). See Storage section above.
- **Bag/box item GUIs** (`ItemAmmoBag`/`ItemCasingBag`/`ItemLeadBox`/`ItemPlasticBag`/`ItemToolBox`)
  — items exist server-side (confirmed in `src/main/java/com/hbm/items/tool/`) but per `docs/phase1/
  STATUS.md:124` and `docs/phase1/items_tool.md:86` these were already explicitly deferred to "Phase
  5 territory" for their GUI without a menu/screen framework existing at the time. That framework now
  exists (per every screen in this report), so these 5 items' GUIs are now **unblocked** and could be
  picked up by this report's area or a follow-up — not attempted here since they weren't named in
  this report's assigned scope and their CE originals (`GUIAmmoBag`/`GUICasingBag`/etc.) were not
  read in this pass.
- **`MinecartDestroyerMenu`'s slot type** — whether it uses a `SlotItemHandler`-restricted "pattern"
  slot matching CE's `SlotPattern`, or a plain unrestricted slot, was not verified (out of this
  report's `Screen`-focused reading). Flagged as an open question, not a confirmed bug.

## Key risks

- **The texture-path bugs in Finding 2 are silent.** A screen with a wrong `ResourceLocation` renders
  identically (NeoForge's missing-texture checkerboard) to a screen with a *correct* path before
  assets exist — there is no compile error or visible difference today that would catch this. If the
  eventual asset-copy pass copies files into CE's real locations (the `PORT_SPEC.md`-mandated
  "verbatim" approach) without also re-auditing every screen's constant against this report's table,
  7 screens will silently keep rendering as missing-texture forever after the copy, with no obvious
  symptom pointing at the screen code as the cause.
- **The vehicle-cargo and launch-pad dimension mismatches are exact but currently invisible**, for the
  same reason (no texture to reveal panel-edge misalignment yet). They will become visible layout bugs
  (background texture painted at the wrong size relative to slot positions, which are separately
  defined in each `Menu` class and were confirmed correct) the moment textures land, unless fixed
  proactively using this report's numbers.
- **`SatInterfaceScreen`'s real scope (Headline 3) is large enough that if left as today's stub, it
  will look like "the satellite panel doesn't work" to any playtester**, not "the satellite panel is
  missing polish" — CE's real interface is a core, distinctive feature (a live in-game radar/map), not
  a decorative status readout. This is the one item in this report where a partial visual pass could
  reasonably be mistaken for "done" by anyone who hasn't read the real 288-line CE source.
- **This report's own numeric findings were checked against CE source, not against a running client**
  (per this task's standing sandbox constraint — no `./gradlew`, no launchable Minecraft client). All
  pixel offsets and dimensions are transcribed directly from CE's `GuiContainer` field values and
  `drawTexturedModalRect` call arguments, which is the same coordinate system this port's
  `AbstractContainerScreen`-based screens use (`leftPos`/`topPos` replacing `guiLeft`/`guiTop` 1:1,
  confirmed by every screen already in this report using that exact substitution) — but this has not
  and cannot be visually confirmed by this sandbox.

## Open questions

1. Does `MinecartDestroyerMenu` use a `SlotItemHandler`-restricted slot matching CE's `SlotPattern`
   semantics, or plain unrestricted slots? (Not verified — out of this report's `Screen`-focused
   reading; owner of `MinecartDestroyerMenu` should confirm.)
2. Should `SatCoordScreen`/`DesignatorManualScreen`'s auto-close-on-send/save behavior match CE
   exactly (close on coord-send, stay open with a flash on designator-save) or is the current,
   opposite-of-both-in-different-ways behavior an intentional UX call? Flagged as a real discrepancy
   in Headline finding under Satellite/designator, not resolved here.
3. Who owns building the 5 now-unblocked bag/box item GUIs (`ItemAmmoBag`/`ItemCasingBag`/
   `ItemLeadBox`/`ItemPlasticBag`/`ItemToolBox`)? They fit this report's "storage/logistics" framing
   but were not in its explicitly named scope, and their CE originals were not read here.
4. Is there a `BedrockOreRegistry`-equivalent ore-scan-color table anywhere in this port under a
   different name, or in a report this survey didn't cross-reference? A repo-wide grep for
   `BedrockOreRegistry` found nothing, but the underlying ore-dictionary/scan-color *data* (as opposed
   to that exact class name) was not separately searched for.
5. What are the exact field/method names on this port's `LaunchPadBaseBlockEntity`/
   `LaunchPadRustedBlockEntity` corresponding to CE's `getFuelState()`/`getOxidizerState()`/
   `isMissileValid()`/`missileLoaded`? This report confirmed the mechanics exist in CE and read this
   port's `LaunchPadScreen`/`LaunchPadRustedScreen` in full, but did not open the block-entity classes
   themselves to confirm the exact server-side accessor names to wire against.
