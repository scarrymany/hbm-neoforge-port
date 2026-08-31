# Phase 5 research: GUI screens survey — power/reactor/processing machines

**Area:** `gui_screens_survey_machines_processing` — a breadth-first visual-parity survey of every
already-real `AbstractContainerMenu`+`Screen` pair Phase 2 built for power-generation, reactor
(RBMK/PWR/fusion/breeder), and processing (shredder/assembler/chemplant/crystallizer/centrifuge/
SILEX/electrolyser/mixer/cyclotron) machines, plus the oil-chain (well/refinery) family surveyed
alongside them since it shares the exact same Screen/Menu package and placeholder convention. **Not**
a re-derivation of menu/slot logic — that is already real and, per the sample checked below, already
verbatim-correct against CE. The question this report answers: does each Screen paint a real ported CE
background texture, or a placeholder? Are progress bars and fluid gauges wired to real data? Do slot
positions, canvas dimensions, and interactive elements (buttons/sliders) match CE's real GUI class?

## Method

Read directly, with exact paths/line numbers cited inline:
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/*.java` — CE's real `GuiScreen`/`GuiContainer`
  subclasses (canvas `xSize`/`ySize`, texture `ResourceLocation` path, slot-adjacent draw calls),
  cross-referenced against `upstream/hbm-ce/src/main/resources/assets/hbm/textures/gui/**` (493 real
  PNG files, counted and spot-measured with a raw PNG-header read, not assumed).
- This port's own `src/main/java/com/hbm/inventory/gui/**` (30 concrete machine `Screen` classes + 7
  `@EventBusSubscriber` client-registry classes + the shared `GuiInfoContainer` base — every file in
  the power-gen/reactor/processing/oil scope was opened and read in full) and
  `src/main/java/com/hbm/inventory/container/machine/**` (the paired `Menu` classes, spot-checked for
  slot-coordinate fidelity against CE).
- `upstream/neo-edition` — used **only** to independently confirm two real NeoForge 1.21.1 client API
  behaviors this report makes load-bearing claims about (`GuiGraphics#blit`'s short-form-overload
  256×256 UV-divisor convention, and `RegisterMenuScreensEvent`/`@EventBusSubscriber` shape) — never
  as a source of layout, texture path, or behavior; neo-edition's own machine-GUI package
  (`com.hbm.inventory.screens`) uses a different rewritten layout in places (e.g. its
  `ReactorZirnoxScreen`) and was not used as a design reference here.
- `docs/phase2/gui_framework.md` (447 lines, already the authoritative prior-phase survey of CE's 222
  GUI files / the shared `GuiInfoContainer`+`MenuBase` framework this report's classes descend from)
  and `docs/phase2/machines_chemical_isotope.md` (389 lines) for the one already-documented,
  already-correctly-scoped deferral this report re-confirms (Electrolyser's metal-electrolysis half).

Roughly 68 files were opened or grepped-with-cited-lines across both repos for this report (42 in this
port, 26 in CE) — every number, path, and dimension quoted below was read directly, not inferred. This
sandbox cannot run `./gradlew` or launch a client, so **no claim here is a screenshot-verified visual
confirmation** — every claim is either (a) arithmetic run directly against the two repos' own source
(canvas sizes, slot-overflow math), (b) a byte-level PNG dimension read, or (c) cross-checked against
neo-edition's real compiling code at this port's exact pinned `neo_version=21.1.228`. Anything resting
on (c) alone is flagged inline as such.

## Headline findings

1. **Zero real CE GUI textures exist anywhere in this port's resources today.** `find
   src/main/resources/assets/hbm/textures/gui -type f` returns 0 files, against CE's real 493
   (`upstream/hbm-ce/.../textures/gui/**`: 156 `fluids/`, 55 `processing/`, 44 top-level, 37 `machine/`,
   17 `reactors/`, 8 `generators/`, 8 `gauges/`, plus book/hud/jei/satellites/storage/tool/vehicle/weapon
   subfolders not tallied here). Every one of this survey's 30 machine `Screen` classes paints a flat
   `GuiGraphics#fill` placeholder instead of blitting a texture — this is a single, already-documented,
   port-wide gap (`GuiInfoContainer`'s own class javadoc states it explicitly, `.../GuiInfoContainer.java:115-119`),
   not 30 independent gaps to rediscover per-machine.
2. **Slot coordinates are already verbatim-correct against CE where checked.** `MachineAssemblyMachineMenu`
   (`src/main/java/.../MachineAssemblyMachineMenu.java:19-23`) places every slot at the exact pixel CE's
   `ContainerMachineAssemblyMachine` (`upstream/hbm-ce/.../ContainerMachineAssemblyMachine.java:24-32`)
   uses (battery `152,81`; blueprint `35,126`; upgrades from `152,108`; 4×3 input grid from `8,18`;
   output `98,45`) — this port's own class javadoc says as much and the read confirms it. **This is the
   part of "visual parity" Phase 2 already solved correctly**; nothing in this report found a slot
   position that doesn't match CE.
3. **Fluid-tank rendering is fully real, not a placeholder** — `FluidTankNTM.renderTank`
   (`src/main/java/com/hbm/inventory/fluid/tank/FluidTankNTM.java:322-373`) is a complete modern
   `RenderSystem`/`BufferBuilder`/`VertexFormat.POSITION_TEX_COLOR` quad renderer that reads the tank's
   real `FluidType` tint and texture and scales the fill quad by real `fluid`/`maxFluid`, and
   `renderTankTooltip` (`:380-394`) shows the real localized fluid name, exact mB amount, and pressure.
   Every machine with a tank in its Menu (Diesel, Combustion Engine, Large Turbine, Turbine, Turbine
   Gas, Crystallizer, Mixer, ChemPlant, Cyclotron, Electrolyser, SILEX, Refinery, Oil Well, ICF Press,
   ICF Reactor, WATZ, PWR) calls both methods with the tank's real screen coordinates. The only
   remaining gap here is CE's per-fluid **texture** (`textures/fluid/*` sprites — a different, smaller
   asset directory than the 493-file GUI one above, not surveyed in depth by this report) rather than
   anything code-side.
4. **Progress bars are hand-computed from real data but rendered inconsistently, and 5 of 14
   progress-bearing machines render no bar at all (text-only).** Shredder/Assembly/Crystallizer/Mixer/
   Breeder each compute `getProgressScaled(pixelSpan)` off the real block entity and paint a flat
   green/blue rectangle scaled correctly. Centrifuge/GasCentrifuge/Electrolyser/ChemPlant/Cyclotron
   compute the identical real percentage but only print it as text (`"Progress: " + …%`) with **no**
   visual bar — an inconsistency inside this port's own placeholder convention, not something CE does
   (CE always renders a real progress-arrow/bar sprite for all of these). Table below marks each case.
5. **A real, code-only layout bug exists independent of the texture gap: 9 of 30 screens draw a
   background panel shorter than their own Menu's player-hotbar row, verified by direct arithmetic
   (not visual guesswork)** — see "Confirmed layout bug" section. This is fixable today with zero asset
   dependency (bump `imageHeight`, or move the `playerInv(...)` call) and should not wait for the
   texture pass.
6. **Two of CE's real interactive control widgets were redesigned to vanilla `Button`s, not carried
   over as-is**: CE's `GUICombustionEngine`/`GUIPWR` use a free-drag mouse-lock slider (`isMouseLocked`,
   `mouseClicked`/`mouseReleased` override, `upstream/hbm-ce/.../GUICombustionEngine.java:113-164`) for
   throttle/rod-level; this port's `MachineCombustionEngineScreen`/`PWRControllerScreen` instead use
   discrete `+`/`-` vanilla `Button`s wired through `Minecraft.gameMode.handleInventoryButtonClick`.
   Functionally equivalent (both end up setting the same server-side integer), but a real, deliberate
   interaction-model change from CE's continuous drag to stepped clicks — flagged as a genuine parity
   decision for whoever signs off on "visual/UX parity," not a bug.
7. **CE's per-machine status-icon overlays (`drawInfoPanel`'s 12-icon `gui_utility.png` sheet — "no
   fuel," "was on," warning states) are dropped in every screen surveyed except where a machine
   author substituted plain text** (e.g. `MachineOilWellScreen`'s `statusText()` switch, `SilexScreen`'s
   "Charge:" line) — no screen in this survey calls `drawInfoPanel` at all today. This is a real,
   verifiable information-loss versus CE (RTG's heat bar and Diesel's fuel-warning icon are simply
   absent, not just untextured), distinct from "texture doesn't exist yet."
8. **RBMK is missing 2 Screens for block entities that already exist server-side, and CE has 9 more
   RBMK GUI classes for panel-type blocks this port hasn't built the block entity for at all** (a
   Phase 2 gap, not this report's to fix, but named so it isn't rediscovered later) — see the RBMK
   section below.
9. **When this port's texture pass does land, `GuiInfoContainer.drawInfoPanel`'s short-form
   `GuiGraphics#blit(ResourceLocation,x,y,u,v,w,h)` calls will render correctly against CE's real
   `gui_utility.png` unchanged** — independently confirmed by a raw PNG-header read
   (`gui_utility.png` is exactly 256×256) and cross-checked against neo-edition's own real, compiling
   `RenderScreenOverlay` (`upstream/neo-edition/.../RenderScreenOverlay.java:48-56`), which uses the
   identical 6-int-arg overload against its own real 256×256 `overlay_misc.png`. NeoForge 1.21.1's
   short blit overload assumes a 256×256 source texture (the same convention 1.12's
   `drawTexturedModalRect` used) — **as long as CE's PNGs are copied in byte-for-byte, unresized**, this
   code path needs no changes. Flagged because it is the one place in this report resting partly on (c)
   "well-established Mojang convention, cross-checked against neo-edition's real code at this port's
   exact NeoForge build" rather than a jar read.

## Directory survey: what exists today

`src/main/java/com/hbm/inventory/gui/` — 66 files total, of which the power/reactor/processing/oil
scope is:

| Subpackage | Files | Contents |
|---|---|---|
| `gui/` (root) + `GuiInfoContainer` | shared base, not itself a machine screen | `drawElectricityInfo`/`drawCustomInfo`/`drawInfoPanel`/`isHovered`/`click` — ported line-for-line from CE's own 148-line-equivalent base (see headline #9) |
| `gui/machine/` | 11 `Screen`s + `PowerGenClientRegistry` + `ProcessingClientRegistry` + `PWRClientRegistry` | RTG, Diesel, Combustion Engine, Turbine, Large Turbine, Turbine Gas, Shredder, Assembly, Crystallizer, Mixer, Reactor Breeding, PWR Controller |
| `gui/machine/chem/` | 6 `Screen`s + `ChemIsotopeClientRegistry` | Centrifuge, Gas Centrifuge, SILEX, Cyclotron, Chem Plant, Electrolyser |
| `gui/machine/fusion/` | 3 `Screen`s + `FusionClientRegistry` | ICF Reactor, ICF Press, WATZ Reactor |
| `gui/machine/oil/` | 2 `Screen`s + `OilChainClientRegistry` | Oil Well, Refinery |
| `gui/machine/rbmk/` | 7 `Screen`s + `RBMKClientRegistry` | Rod, Control, Control Auto, Storage, Boiler, Console, Autoloader |

**30 concrete machine `Screen` classes total**, each correctly registered in its family's
`@EventBusSubscriber RegisterMenuScreensEvent` handler — all 6 client-registry classes were read in
full and every registration matches an existing `Screen`+`MenuType` pair with **zero orphans found**
(no unregistered Screen, no registration pointing at a missing class). This plumbing layer is
completely solved; nothing here blocks a texture-only follow-up pass.

The remaining files under `gui/` (`bomb/` 9 nuke-assembly screens, `cart/` 2, `train/` 2, `turret/` 2,
`BatteryScreen`/`CrateScreen`/`DesignatorManualScreen`/`FluidTankScreen`/`LaunchPad*`/`SatCoord*`/
`SatInterfaceScreen`) belong to other Phase 5 research areas (weapon/bomb assembly UI, rail, turret,
satellite) and are out of this report's scope.

## The visual-parity table

Every canvas dimension below was read directly from each class's constructor (`this.imageWidth`/
`this.imageHeight` this port, `this.xSize`/`this.ySize` CE) — no dimension is estimated. "Δ" is
this-port minus CE (positive = this port's placeholder canvas is *taller/wider* than CE's real one,
which typically means the panel has extra unused space below/right rather than a slot-overflow risk;
negative Δ combined with a Menu that runs its hotbar close to `imageHeight` is what produces the
overflow bug in the next section).

### Power generation

| Machine | This port `Screen` | Canvas (W×H) | CE class | CE canvas | CE texture | Real texture used? | Fluid gauge | Progress/status indicator |
|---|---|---|---|---|---|---|---|---|
| RTG | `MachineRTGScreen` | 176×187 | `GUIMachineRTG` | 176×176 (Δ+11) | `textures/gui/gui_rtg.png` | No — flat fill | N/A (no tank) | **Missing entirely** — CE renders real heat *and* power vertical bars (`GUIMachineRTG.java:53-62`); this port's `renderLabels` (`MachineRTGScreen.java:34-37`) shows only a power/max tooltip, the heat bar and its own tooltip are gone |
| Diesel Generator | `MachineDieselScreen` | 176×202 | `GUIMachineDiesel` | 176×203 (Δ-1) | `textures/gui/generators/gui_diesel.png` | No — flat fill | Real (`FluidTankNTM`) | No visual bar (power tooltip only); CE's on/off icon-click toggle (`guiLeft+89..105,guiTop+61..75`) and "wasOn"/no-fuel warning icons (`drawInfoPanel`, `GUIMachineDiesel.java:78-84`) replaced by a vanilla On/Off `Button` at a different position (`60,17`) — no equivalent warning icon shown |
| Combustion Engine | `MachineCombustionEngineScreen` | 176×202 | `GUICombustionEngine` | 176×203 (Δ-1) | `textures/gui/generators/gui_combustion.png` | No — flat fill | Real | Text-only "Throttle: X/30"; CE's continuous drag-slider (headline #6) replaced with 3 discrete `+`/`-`/On-Off buttons; CE's piston-type HE/t tooltip (`GUICombustionEngine.java:74-100`) not carried over |
| Turbine | `MachineTurbineScreen` | 176×165 | `GUIMachineTurbine` | 176×168 (Δ-3) | `textures/gui/gui_turbine.png` | No — flat fill | Real (2 tanks) | N/A (passive) |
| Large Turbine | `MachineLargeTurbineScreen` | 176×165 | `GUIMachineLargeTurbine` | 176×168 (Δ-3) | `textures/gui/generators/gui_turbine_large.png` | No — flat fill | Real (2 tanks) | N/A (passive) |
| Turbine (Gas) | `MachineTurbineGasScreen` | 176×222 | `GUIMachineTurbineGas` | 176×223 (Δ-1) | `textures/gui/generators/gui_turbinegas.png` | No — flat fill | Real (4 tanks) | Text-only "RPM/Temp/State"; 4 vanilla buttons (start/stop, auto, throttle ±) replace whatever CE's real widget set is (not read in this pass — out of table depth budget) |

### Reactors (RBMK / PWR / fusion / breeder)

| Machine | This port `Screen` | Canvas (W×H) | CE class | CE canvas | CE texture | Real texture used? | Fluid gauge | Progress/status indicator |
|---|---|---|---|---|---|---|---|---|
| PWR Controller | `PWRControllerScreen` | 176×202 | `GUIPWR` | 176×188 (Δ+14) | `textures/gui/reactors/gui_pwr.png` | No — flat fill | Real (2 tanks) | Text-only rod/heat/flux readout; CE's rod-level drag-slider replaced with `+`/`-` buttons (headline #6) |
| Reactor (Breeder) | `MachineReactorBreedingScreen` | 176×166 | `GUIMachineReactorBreeding` | 176×166 (**exact match**) | `textures/gui/processing/gui_breeder.png` | No — flat fill | N/A | Flat-rect progress bar (one of the 5 that does render one) |
| RBMK Rod (fuel channel) | `RBMKRodScreen` | 176×166 | `GUIRBMKRod` | 176×186 (Δ-20) | `textures/gui/reactors/gui_rbmk_element.png` | No — flat semi-transparent fill (different fill style than `machine/` package, see below) | N/A | Text-only enrichment/xenon/heat readout |
| RBMK Control (manual) | `RBMKControlScreen` | 176×166 | `GUIRBMKControl` | not measured this pass | `textures/gui/reactors/gui_rbmk_control.png` | No | N/A | Text-only extraction %/color |
| RBMK Control (auto) | `RBMKControlAutoScreen` | 176×166 | `GUIRBMKControlAuto` | not measured this pass | `textures/gui/reactors/gui_rbmk_control_auto.png` | No | N/A | Text-only function/heat-range/level-range/target |
| RBMK Storage | `RBMKStorageScreen` | 176×166 | `GUIRBMKStorage` | not measured this pass | `textures/gui/reactors/gui_rbmk_storage.png` | No | N/A | None (pure 3×3 grid) |
| RBMK Boiler | `RBMKBoilerScreen` | 176×166 | `GUIRBMKBoiler` | not measured this pass | `textures/gui/reactors/gui_rbmk_boiler.png` | No | Text-only water/steam mB (not `FluidTankNTM.renderTank` — hand-printed numbers instead, unlike every other machine in this survey) | — |
| RBMK Console | `RBMKConsoleScreen` | 176×166 | `GUIRBMKConsole` | **244×172** (Δ-68 width!) | `textures/gui/reactors/gui_rbmk_console.png` | No | N/A | **Feature loss, not just texture loss**: CE renders a scrolling flux-history graph across its wider 244px canvas; this port's `RBMKConsoleScreen.render` (`RBMKConsoleScreen.java:24-30`) shows only the single latest `fluxBuffer` sample as one line of text — the full history array is already collected server-side (`be.fluxBuffer`) and simply isn't drawn as a graph |
| RBMK Autoloader | `RBMKAutoloaderScreen` | 176×166 | `GUIRBMKAutoloader` | not measured this pass | `textures/gui/machine/...` (not confirmed this pass) | No | N/A | None beyond the 2-slot hopper |
| WATZ Reactor | `WatzReactorScreen` | 176×263 | `GUIWatz` | 176×229 (Δ+34) | `textures/gui/reactors/gui_watz.png` | No — flat fill | Real (3 tanks) | Text-only heat/flux/on-off |
| ICF Reactor | `IcfReactorScreen` | 176×222 | `GUIICF` | **248×222** (Δ-72 width!) | `textures/gui/reactors/gui_icf.png` | No — flat fill | Real (3 tanks) | Text-only heat/laser |
| ICF Press | `IcfPressScreen` | 176×156 | `GUIICFPress` | 176×179 (Δ-23) | `textures/gui/processing/gui_icf_press.png` | No — flat fill | Real (2 tanks) | Text-only muon count |

**RBMK's non-`Rod` screens (Control/ControlAuto/Storage/Boiler/Console/Autoloader) all hardcode the
identical `176×166` canvas regardless of what CE's real class actually is** — confirmed CE's own
`GUIRBMKConsole` is 244×172 (44% wider, presumably to fit the flux graph), so this isn't just "smaller
than CE," it's a fixed placeholder size applied uniformly without checking CE's real per-class
dimensions, unlike the `machine/`-package screens which each picked a distinct (if often slightly off)
size. The RBMK `Boiler` screen also stands out as the one machine in this entire survey with a real
tank field (`feed`/`steam`) that renders as **hand-printed text instead of calling
`FluidTankNTM.renderTank`** — every other tank-bearing screen in this survey uses the real renderer.

### Processing

| Machine | This port `Screen` | Canvas (W×H) | CE class | CE canvas | CE texture | Real texture used? | Fluid gauge | Progress/status indicator |
|---|---|---|---|---|---|---|---|---|
| Shredder | `MachineShredderScreen` | 176×233 | `GUIMachineShredder` | 176×233 (**exact match**) | `textures/gui/processing/gui_shredder.png` | No — flat fill | N/A | Flat-rect progress bar (renders) |
| Assembly Machine | `MachineAssemblyMachineScreen` | 176×202 | `GUIMachineAssemblyMachine` | 176×256 (**Δ-54, largest mismatch in this survey**) | `textures/gui/processing/gui_assembler.png` | No — flat fill | N/A | Flat-rect progress bar (renders); slot coordinates verbatim-correct against CE (headline #2) despite the 54px canvas gap — see the confirmed layout bug this causes, next section |
| Crystallizer | `MachineCrystallizerScreen` | 176×191 | `GUICrystallizer` | 176×204 (Δ-13) | `textures/gui/processing/gui_crystallizer_alt.png` (CE ships an unused `gui_crystallizer.png` sibling too) | No — flat fill | Real | Flat-rect progress bar (renders) |
| Mixer | `MachineMixerScreen` | 176×191 | `GUIMixer` | 176×204 (Δ-13) | `textures/gui/processing/gui_mixer.png` | No — flat fill | Real (3 tanks) | Flat-rect progress bar (renders) |
| Centrifuge | `CentrifugeScreen` | 176×166 | `GUIMachineCentrifuge` | 182×189 (Δ-6/-23) | `textures/gui/processing/gui_centrifuge.png` | No — flat fill | N/A (no tank in this port's Menu) | **Text-only**, no visual bar (headline #4) |
| Gas Centrifuge | `GasCentrifugeScreen` | 176×166 | `GUIMachineGasCent` | 206×204 (Δ-30/-38) | not confirmed this pass | No — flat fill | N/A | Text-only |
| Chemical Plant | `ChemPlantScreen` | 216×200 | `GUIMachineChemicalPlant` | 176×256 (Δ+40/-56) | `textures/gui/processing/gui_chemplant.png` | No — flat fill | Real (6 tanks: 3 in + 3 out) | Text-only recipe name/progress % |
| Cyclotron | `CyclotronScreen` | 220×200 | `GUIMachineCyclotron` | 190×215 (Δ+30/-15) | `textures/gui/machine/gui_cyclotron.png` | No — flat fill | Real (3 tanks) | Text-only progress % |
| Electrolyser | `ElectrolyserScreen` | 176×166 | `GUIElectrolyserFluid` (fluid half only — see below) | 210×204 (Δ-34/-38) | `textures/gui/processing/gui_electrolyser_fluid.png` | No — flat fill | Real (3 tanks) | None |
| SILEX | `SilexScreen` | 232×166 | `GUISILEX` | 176×222 (Δ+56/-56) | `textures/gui/processing/gui_silex.png` | No — flat fill | Real | Text-only laser mode/charge |

### Oil chain (surveyed alongside processing — same package/convention)

| Machine | This port `Screen` | Canvas (W×H) | CE class | CE canvas | CE texture | Real texture used? | Fluid gauge | Progress/status indicator |
|---|---|---|---|---|---|---|---|---|
| Oil Well (derrick/pumpjack/fracking, shared GUI) | `MachineOilWellScreen` | 176×202 | `GUIMachineOilWell` | 184×190 (Δ-8/+12) | `textures/gui/machine/gui_well.png` | No — flat fill | Real (oil + gas tanks) | Text-only drill-state switch (one of the few screens that *does* substitute real text for CE's `drawInfoPanel` icon states) |
| Refinery | `MachineRefineryScreen` | 216×230 | `GUIMachineRefinery` | 210×231 (Δ+6/-1) | `textures/gui/gui_refinery.png` | No — flat fill | Real (5 tanks) | Text-only refining state/sulfur cycle |

## Confirmed layout bug: player-inventory hotbar renders outside the placeholder panel on 9 screens

Independent of any texture question, direct arithmetic against each `Menu`'s own `playerInv(...)` call
(`MenuBase.playerInv`, `src/main/java/com/hbm/inventory/container/MenuBase.java:154-175`: 2-arg form
places the hotbar row at `playerInvY+58`, its own bottom edge at `+58+18=+76`; 3-arg form's hotbar
bottom edge is the given `playerHotbarY+18`) against each paired `Screen`'s `imageHeight` shows the
hotbar's bottom pixel falls **below** the flat-fill background rectangle `renderBg` paints, on 9 of the
30 screens:

| Screen | Menu's `playerInv(...)` call | Hotbar bottom edge | `imageHeight` | Overflow |
|---|---|---|---|---|
| `MachineAssemblyMachineScreen` | `playerInv(8, 174)` (2-arg) | 174+76 = 250 | 202 | **48px** |
| `CentrifugeScreen` | `playerInv(8, 116)` (2-arg) | 116+76 = 192 | 166 | **26px** |
| `GasCentrifugeScreen` | `playerInv(8, 116)` (2-arg) | 192 | 166 | **26px** |
| `ElectrolyserScreen` | `playerInv(8, 116)` (2-arg) | 192 | 166 | **26px** |
| `SilexScreen` | `playerInv(8, 116)` (2-arg) | 192 | 166 | **26px** |
| `IcfPressScreen` | `playerInv(8, 97)` (2-arg) | 97+76 = 173 | 156 | **17px** |
| `MachineCrystallizerScreen` | `playerInv(8, 122, 180)` (3-arg) | 180+18 = 198 | 191 | **7px** |
| `MachineMixerScreen` | `playerInv(8, 122, 180)` (3-arg) | 198 | 191 | **7px** |
| `CyclotronScreen` | `playerInv(8, 130)` (2-arg) | 130+76 = 206 | 200 | **6px** |

In every one of these, `renderBg` fills only `0..imageWidth × 0..imageHeight` — so the bottom edge of
the player's own inventory (and, worst case for Assembly, roughly half the player's main inventory
grid, not just the hotbar) will be drawn with **no background panel behind it at all**, floating over
whatever the game's own blurred-background/vignette shows through. This reproduces even with the
current flat-gray placeholder (it is not a "texture will fix it" problem) and is pure arithmetic, not
a guess — the fix is either raising `imageHeight` to clear the Menu's own `playerInv` call by CE's usual
~6-8px margin, or moving the `playerInv` call up to match the existing `imageHeight`. Recommend fixing
alongside whichever machine's texture eventually gets ported (same file, same method), or as one
standalone before-texture cleanup pass across these 9 files — it needs no new assets.

The other 21 screens' `playerInv` calls all clear their own `imageHeight` with a 4-42px margin (tightest:
`MachineRefineryScreen` at 4px, `MachineTurbineGasScreen`/`MachineTurbineScreen`/
`MachineLargeTurbineScreen` at 5px — all safe, just worth noting as close).

## Two placeholder-panel styles coexist with no shared helper

`gui/machine/**` (11 files) and `gui/machine/{chem,fusion,oil}/**` all use the two-fill "3D bevel" idiom
(`guiGraphics.fill(x,y,x+w,y+h,0xFF8B8B8B)` then an inset `guiGraphics.fill(...,0xFFC6C6C6)`), matching
vanilla's own inventory-screen gray. `gui/machine/rbmk/**` (all 7 files) instead uses a single flat
semi-transparent fill (`0xC0C6C6C6`) with no border/bevel. Neither matches CE (which always blits a
real textured panel), and there is no shared `GuiInfoContainer` helper method for either — each of the
30 screens repeats one of the two 1-2 line fill idioms inline. A one-method addition to
`GuiInfoContainer` (e.g. `drawPlaceholderPanel(guiGraphics, imageWidth, imageHeight)`) would deduplicate
this across all 30 files and make the RBMK family visually consistent with the rest pending the real
texture pass — a genuinely free, zero-risk cleanup, not required for functionality.

## RBMK: 2 screens missing for existing block entities, 8 more CE GUI classes with no block entity at all

CE ships 16 RBMK-related GUI classes (`upstream/hbm-ce/.../gui/GUIRBMK*.java` +
`GUIScreenRBMK{Terminal,Lever,Gauge,Graph,KeyPad,Display,Indicator}.java`). This port has 7 RBMK
Screens. Cross-checking against this port's own `RBMKBlockEntities`
(`src/main/java/com/hbm/blockentity/machine/rbmk/RBMKBlockEntities.java:18-34`, all 16 fields read):

- **`RBMKHeaterBlockEntity` and `RBMKOutgasserBlockEntity` already exist and are registered**
  (`RBMKBlockEntities.java:29,27`) but have **no Menu or Screen at all** — not even a placeholder. CE's
  `GUIRBMKHeater`/`GUIRBMKOutgasser` were never read in this pass (out of budget for a breadth survey)
  but exist and should be a short, cheap follow-up once this area's placeholder convention is settled,
  since the block entities are already real.
- **`RBMKLever`, `RBMKGauge`, `RBMKTerminal`, `RBMKKeyPad`, `RBMKDisplay`, `RBMKIndicator`,
  `RBMKGraph`, `RBMKNumitron`** (CE's redstone-control-panel decorative/logic blocks) have **no block
  entity in this port at all** — confirmed by grep, zero hits anywhere under `src/main/java/com/hbm`.
  This is a **Phase 2 (block entity) gap, not a Phase 5 gap** — there is nothing for a Screen to bind
  to yet. Flagging here only so it isn't mistaken for "Phase 5 forgot these" when someone next audits
  RBMK; the actual owner is whoever picks up the remaining RBMK block-entity work.

## Electrolyser: metal-electrolysis GUI correctly deferred, not forgotten

CE's `TileEntityElectrolyser` backs two independent Container/Screen pairs off one block
(`ContainerElectrolyserFluid`/`GUIElectrolyserFluid` and `ContainerElectrolyserMetal`/
`GUIElectrolyserMetal`, both 210×204 — confirmed by direct read of both CE classes). This port's
`ElectrolyserBlockEntity` javadoc (`src/main/java/com/hbm/blockentity/machine/chem/ElectrolyserBlockEntity.java:41-48`)
explicitly states the ore/metal half is not ported because it depends on
`com.hbm.util.CrucibleUtil.pourFullStack`, a foundry/casting world-interaction system this port has not
built anywhere yet — already documented as a named cross-phase dependency in
`docs/phase2/machines_chemical_isotope.md:194-210,297-300`. This is correctly out of this report's
scope: the blocker is a Phase 2/4 foundry system, not a Phase 5 GUI gap, and re-deriving it here would
just duplicate that report. **Only the fluid half's GUI is in scope for this area, and it is present**
(`ElectrolyserScreen`, table above).

## Phase 5 safe-to-build-now scope

Every item below needs **zero new server-side plumbing** — the data these screens would draw already
exists on the real, already-networked client-side block entity:

1. **Copy CE's real GUI textures in and swap each `renderBg`'s flat-fill for a `GuiGraphics#blit`** —
   this is the overwhelming majority of the remaining work in this area and is purely an asset-and-blit-
   call change, one file at a time, no new fields needed anywhere. Canvas-dimension mismatches (table
   above) should be corrected to CE's real `xSize`/`ySize` in the same pass, since the real texture PNG
   dictates the real canvas size.
2. **Fix the 9-screen hotbar-overflow bug** (see dedicated section) — pure arithmetic fix, no assets,
   can happen before or independent of the texture pass.
3. **Add real progress-bar rendering to Centrifuge/GasCentrifuge/Electrolyser/ChemPlant/Cyclotron** —
   the real percentage is already computed (`getProgressScaled`/`getCentrifugeProgressScaled`), only
   the `guiGraphics.fill` call is missing; trivial to add even before real textures land (as a
   placeholder rect, matching what Shredder/Assembly/Crystallizer/Mixer/Breeder already do).
4. **Restore RTG's heat bar and a generic "insufficient fuel/was on" text substitute for Diesel** —
   both machines' block entities already expose the needed fields (`heat`/`heatMax` per CE's
   `TileEntityMachineRTG`, confirmed present via this port's own `getPower()`/`getMaxPower()` pattern;
   `hasAcceptableFuel()`-equivalent state would need a one-line getter check on `MachineDieselBlockEntity`
   if not already exposed — not confirmed in this pass, flag as a quick check before implementing).
5. **Add a shared `drawPlaceholderPanel` helper to `GuiInfoContainer`** and adopt it in all 30 screens,
   unifying the two competing placeholder styles — free cleanup, no risk.
6. **RBMK Heater/Outgasser Menu+Screen pair** — block entities already exist, this is pure new-file work
   following the exact same pattern as the other 5 RBMK screens in this survey.

## Deferred / blocked scope (not this area's to fix)

- **RBMK Lever/Gauge/Terminal/KeyPad/Display/Indicator/Graph/Numitron block entities** — blocked on
  whoever owns the remaining RBMK block-entity work (Phase 2 scope), not Phase 5. No GUI can exist
  before the block entity does.
- **Electrolyser's metal/ore-electrolysis half** — blocked on a foundry/casting system
  (`CrucibleUtil.pourFullStack`-equivalent) not built anywhere in this port yet. Already documented,
  owner is whoever picks up foundry/casting (cross-references `docs/phase2/machines_chemical_isotope.md`).
- **RBMK Console's scrolling flux-history graph** — the data (`fluxBuffer`) already exists server-side
  and is already synced to the client-side block entity; only the graph-rendering code itself (reading
  the array and drawing a line/bar chart across the wider CE canvas) is missing. This is squarely this
  area's own work once the texture pass gives the console its real 244px-wide canvas — flagged as
  "safe to build now" in spirit but sequenced after the canvas-size fix above, not blocked on anything
  external.
- **Fluid-type textures** (`textures/fluid/*`, distinct from the 493 GUI textures counted above) — not
  surveyed in depth here; `FluidTankNTM.renderTank` already calls `type.getTexture()` correctly, so once
  those sprite files exist, tank rendering needs no further code changes (confirmed by reading the
  renderer, not the asset directory — that directory's own completeness was out of this report's scope).
- **JEI/recipe-viewer parity** for these machines (the recipe-lookup GUIs, not the machine GUIs
  themselves) — explicitly out of this report's scope per the task's own framing and already flagged as
  a separate Phase 5 concern by `docs/phase2/oil_production_chain.md:163`.

## Key risks

- **Canvas-dimension corrections are not always a simple "use CE's number" swap** — 3 screens
  (`MachineAssemblyMachineScreen` -54, `RBMKConsoleScreen` -68 width, `IcfReactorScreen` -72 width) have
  large enough deltas that bumping `imageHeight`/`imageWidth` to CE's real value will visibly shift
  where the panel is centered on screen and may require re-checking whether the *player's own*
  inventory slot positions (currently sized for the smaller placeholder canvas) still read as
  correctly centered once the real, larger CE texture is blitted behind them — worth a visual check
  once a client can actually run, not assumable from source alone.
- **`GuiGraphics#blit`'s implicit-256 convention (headline #9) is confirmed by PNG byte-header +
  neo-edition cross-check, not a jar read** — if any individual CE GUI texture turns out not to be
  exactly a multiple of the assumed canvas (this report spot-checked only `gui_utility.png`, not all
  493 files), that specific file's blit call would need the explicit 8-arg `(...,textureWidth,
  textureHeight)` overload instead. Recommend a quick PNG-dimension check per-file during the texture
  pass rather than assuming all 493 are 256×256.
- **RBMK Boiler's tank fields render as hand-printed text, not `FluidTankNTM.renderTank`** — inconsistent
  with the rest of this survey and worth fixing in the same pass as its texture, since it's the one
  tank-bearing screen that doesn't yet use the already-real shared renderer.
- **Interactive-widget redesigns (headline #6) are a real UX decision, not purely cosmetic** — a
  continuous drag-slider "feels" different from stepped buttons even with identical end-state; if strict
  "visual/UX parity" per `PORT_SPEC.md` is read to include control feel, this is worth an explicit
  product decision rather than silently keeping the button version once textures land.

## Open questions

1. Should the canvas-dimension corrections (bump to CE's real `xSize`/`ySize`) happen in the same pass
   as texture blitting, or as a separate, earlier "layout-only" pass across all 30 files? This report
   recommends same-pass (the real texture dictates the real size), but a separate earlier pass would
   fix the 9-screen hotbar-overflow bug sooner without waiting on any asset work.
2. Does this port want to keep the stepped-button redesign for Combustion Engine's throttle and PWR's
   rod level (headline #6), or reimplement CE's real drag-slider once textures make the slider's own
   visual affordance (a groove/handle sprite) available? Both are legitimate; this report did not find
   a stated decision either way in `docs/phase2/*.md`.
3. `GUIRBMKAutoloader`'s real CE texture path was not confirmed in this pass (grep for its exact
   `ResourceLocation` line was not run) — a 5-minute follow-up before that specific machine's texture
   pass, not a blocker for anything in this report.
4. This report did not open `GUIRBMKControl`/`GUIRBMKControlAuto`/`GUIRBMKStorage`'s CE source for their
   real `xSize`/`ySize` (marked "not measured this pass" in the table) — given RBMK's uniform 176×166
   placeholder is already known-wrong for `Rod` (-20) and `Console` (-68 width), it should be assumed
   wrong for these three too until measured, not assumed close.
