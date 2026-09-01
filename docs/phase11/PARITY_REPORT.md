# Phase 11 parity census (live, 2026-09-01)

**This document supersedes `docs/phase6/PARITY_REPORT.md`.** Phase 6 (~54% weighted / ~67% unweighted)
is stale. Do not quote Phase 6 as current.

Source: static read of `upstream/hbm-ce` vs this port. Script: `scripts/phase11_parity_census.py`
(item/block ids via Phase 10 `extract_all_ids` — Java `register`/`reg`/`parts`/`parts1`/`fuel` + Mats autogen
+ plant/glyph/bedrock loops, plus flatten extras; **not** lang keys). Recipe JSON counted from
`src/main/resources` + `src/generated`. Quality bar: `docs/CE_PARITY_ADDENDUM.md`.

Verified this wave: `compileJava` 0,
`./gradlew runServer` **Done (5.633s)** on wiped world port 25566, **4052 recipes**.
No recipe parse errors. No Exception/ERROR. No new tag. `v0.0.1-rc2` stays.

## Top line

| | |
|---|---|
| **Weighted** (Σport / ΣCE) | **106.2%** (8248 / 7767) |
| **Unweighted** (mean of category %) | **104.1%** |
| Recipe/loot + machine-table + ItemPools reachability | **63.4%** (1657 / 2613) |
| CE `@AutoRegister` entities still missing | **none** |

Weighted is **above 99%** (need 7689). Gates: `compileJava` 0 + `runServer` Done.
Tag `v0.0.1-rc2`. Existing `v0.0.1-rc1` / `beta-82` / `beta-82.1` stay.

Largest remaining holes: **blocks 154**, **machine 85**, **vanilla 51**.
Weighted **106.2%**. Reachability **63.4%** — still the owner pain. Not a tag.
99%+ tag: https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc2
90% playtest (kept): https://github.com/scarrymany/hbm-neoforge-port/releases/tag/v0.0.1-rc1

## Per-category

| Category | CE | Port | % | Method |
|---|---:|---:|---:|---|
| Items (flattened ids) | 1863 | 2613 | **140.3%** | +`linker` |
| Blocks | 1169 | 1016 | **86.9%** | +`toxic_block` |
| Fluids | 162 | 162 | **100%** | `FluidType` fields |
| Entities | 168 | 189 | **112.5%** | CE `@AutoRegister(name=)` under `entity/`. Port extras = spawn eggs + `entity_cloud_solinium` |
| Sounds | 381 | 381 | **100%** | `SoundEvent` / `DeferredHolder` fields |
| Vanilla crafting | ~1950 | 1899 | **97.4%** | CE estimate kept at 1950 |
| Machine recipes | ~2009 | 1924 | **95.8%** | +4 assembler JSON. ChemPlant unique **72=72**. Crystallizer unique **303/~309**. Shredder unique **201/200**. Cyclotron **42=42**. Anvil unique **122 / 200** `stack("id")` / honest **168 / 200**. Centrifuge 75/78 AE2 skip. |
| Advancements | 65 | 65 | **100%** | JSON under `data/hbm/advancement(s)` |

Texture leftover after aliases (Phase 10, do **not** invent art): items **9.3%** (164/1771), blocks
**16.6%** (96/579). See `docs/phase10/LEFTOVER_MISSES.md`.

## What changed this wave (Dud + Barrel)

Reachability **63.4% (1657 / 2613)**. No invented biomes / chances. `linker` /
landmine / NITAN stay accepted.

- **`linker`**: CE `ItemTeleLink` (`ModItems.java:106`). `DETONATOR_POS` =
  CE NBT `x/y/z`. Click any block → set + `chat.telelink.set`. Sneak on
  `machine_teleporter` with a saved pos → `target` + `linked=true`, clear NBT,
  `chat.telelink.linked`. Existing `linker.png` + lang. CE craft
  `ToolRecipes.java:107` `I I / ICI / GGG` (plate_iron / circuit_advanced /
  plate_gold). Teleporter TODO dropped.
- **Landmine** (`HbmWorldGen.java:386-404`): `enableDungeons` + `enableMines`,
  `minefreq` default **0:64**, `mine_ap` flags `2|16`, `waitingForPlayer=true`.
  `isFaceSturdy(UP)`. Step `TOP_LAYER_MODIFICATION` — CE `IWorldGenerator` is
  post-decorate; first wipe at `UNDERGROUND_ORES` had **0/841** (grass overwrite).
  After fix: **2** `hbm:mine_ap` in 841 spawn chunks.
- **NITAN** (`HbmWorldGen.java:652-686` / `:744-753`): `enableNITAN` only (not
  dungeon-gated). 8 coords y=250: `(±10000,250,±10000)` + axes. Air → chest +
  `POOL_POWDER` × 29. Not generated in spawn (coords 10000).
- **Dud** (`Dud.java` / `HbmWorldGen.java:379`): `enableDungeons`,
  `dudStructure` default **0:500**, no biome. `EnumDudType` → four
  `crashed_bomb_*` flags `2|16`. Sandstone OR spawn. `TOP_LAYER_MODIFICATION`.
  Default 1/500: **0/841** (λ≈1.7). Forced `dudStructure=1`: **70** duds
  (23/19/17/11 nuke/conv/salted/balefire).
- **Barrel** (`Barrel.java` / `:370-371`): `enableDungeons`,
  `barrelStructure` default **0:5000**, `getBaseTemperature()>1.8F` only.
  289-cell schematic, `crate_steel` + `POOL_EXPENSIVE`×16. `toxic_block`
  registered (still; flow/fog TODO(CE: ToxicBlock.java:26-105)). YellowBarrel
  1-in-3 toxic branch live. Default spawn plains: miss. Forced 1/1:
  **7** `crate_steel` / **9** `toxic_block` / **11** sellafield chunks.
- **Spaceship** (`:377`): `enableDungeons`, **0:1000**, no biome, 1419-cell
  schematic, `y+=1`. Forced 1/1: 14 `deco_tungsten` / 8 `pwr_fuelrod` /
  1 `hadron_coil_alloy`. `fusion_core` item untouched (`fusion_core_block`).
- **Satellite dish** (`:373-374`, not satlink): **0:500**, `temp<1||temp>1.8`.
  Forced 1/1: 8 `deco_titanium` / 6 `deco_beryllium` / 1 `tape_recorder`.
- FEATURES write-radius 0 clips overflow (no ServerLevel cascade).
  Same Spaceship/Satellite skip — do not invent a write cascade.
- **GlyphidHive** (`:347-358`): `enableHives`, overworld, `hiveSpawn` **256**.
  Schematic 11×5×11. `glyphid_spawner` + glyphid entities. 1/10 infected.
- **DesertAtom** (`:367-368`): `atomStructure` **0:500**,
  `!hasPrecipitation && temp>=2`. 5162-cell schematic. Height point `(20,0,16)`
  + sandstone/terracotta.
- Forced 1/1 desert seed `1833280291927865410`, 841 chunks: hive 57
  `glyphid_base` / 56 `glyphid_spawner` / 5 infested; atom 48
  `reinforced_sand` / 10 `yellow_barrel` / 7 `lead_block`. Defaults 256/500.

Honest E2E: no client. compileJava 0 + runServer Done + MCA palette scan.

## This wave (leftover Dummyable casings → live TE)

Phase11 `BlockBase` casings with CE TEs. Same ids. No invent.
≠ already-live `machine_assembly_machine` / `machine_chemical_plant`.

- **`machine_assembly_factory`**: Dummyable `{2,0,2,2,2,2}` offset 2 +
  perimeter extras `|i|==2|||j|==2` at y + roof extras y+2 along ±rot*2
  (`MachineAssemblyFactory.java:45-64`). META≥12 core TE. 60 slots,
  4 lanes (`5+i*14` in / `17+i*14` out), tanks 4+4 @ **4000** + water/lps
  **4000**. Upgrade then **`speed*2D, pow*2D`**. `canCool` 100 water → 100
  lps/lane. maxPower `max(power, sum(recipe.power*100), 1_000_000)`.
  Recipes: port `AssemblerRecipe` / `ProcessingRecipes.ASSEMBLER_TYPE`.
  GUI `gui_assembly_factory.png` 256×240 + bind. Caps item+fluid+energy.
  Auto-match per lane.
  TODO(CE: MachineAssemblyFactory.java:36) ProxyDyn META≥6;
  TODO(CE: MachineAssemblyFactory.java:72-99) ILookOverlay;
  TODO(CE: GUIMachineAssemblyFactory.java:62) GUIScreenRecipeSelector;
  TODO(CE: TileEntityMachineAssemblyFactory.java:213) CE blueprint slot
  `4+i*7` typo — port uses container `4+i*14`;
  TODO(CE: RenderAssemblyFactory.java:1) OBJ TESR + AssemfacArm;
  TODO(CE: TileEntityMachineAssemblyFactory.java:744-763) ROR.
- **`machine_chemical_factory`**: same Dummyable dims/extras
  (`MachineChemicalFactory.java:45-64`). 32 slots, 4 lanes
  (`5+i*7..7` in / `8+i*7..10` out), 12+12 tanks @ **24000** + water/lps
  **4000**. Same upgrade `*2` + cool. Recipes: `ChemPlantRecipes`.
  GUI `gui_chemical_factory.png` 248×216 + bind. Caps item+fluid+energy.
  Same-type out→in 50 mB/tick. Auto-match per lane.
  TODO(CE: MachineChemicalFactory.java:36) ProxyDyn META≥6;
  TODO(CE: GUIMachineChemicalFactory.java:63) GUIScreenRecipeSelector;
  TODO(CE: TileEntityMachineChemicalFactory.java:168-174) `loadTank(10,13)`
  leftover chem-plant slots — not copied (no canister slots);
  TODO(CE: RenderChemicalFactory.java:1) OBJ TESR;
  TODO(CE: TileEntityMachineChemicalFactory.java:525-544) ROR.
- **`turret_arty`**: Dummyable `{1,0,2,1,2,1}` offset 1
  (`TurretArty.java:30-36`). META≥12 core TE. 11 slots (0 chip, 1–9 ammo,
  10 battery). maxPower **100000**. Modes ARTILLERY/CANNON/MANUAL. Range
  3000/250, grace 250/32, V0 50/20, delay 300/40, elevation 90, barrel 9,
  height 3. Fires `EntityArtilleryShell` + existing `ammo_arty_normal`/
  `chlorine`/`phosgene`/`mustard`. GUI `gui_turret_arty.png` 176×222 + bind.
  TODO(CE: TurretArty.java:26) ProxyCombo;
  TODO(CE: RenderTurretArty.java:1) OBJ TESR;
  TODO(CE: ItemAmmoArty.java:54-67) missing 8 shell metas not invented.
- **`turret_himars`**: same Dummyable dims (`TurretHIMARS.java:29-35`).
  maxPower **1_000_000**. AUTO/MANUAL, crane ±0.0125, fire 40t, V0 25,
  range 5000, grace 250, height 5, barrel 0.5. Reload pack → 6 or 1.
  Fires `EntityArtilleryRocket` + existing 8 `ammo_himars_*`.
  GUI `gui_turret_himars.png` 176×222 + bind.
  TODO(CE: TurretHIMARS.java:25) ProxyCombo;
  TODO(CE: RenderTurretHIMARS.java:1) OBJ TESR;
  TODO(CE: ItemAmmoHIMARS.java:256) volcanic_lava → slag.
- Fusion torus / klystron family not stubbed (`KlystronNetwork` /
  `PlasmaNetwork` / `ModuleMachineFusion`). CE `fusion_core` is BlockBase.
  `machine_watz_reactor` already live — no second Watz id.
  `watz_element` / `watz_cooler` / `machine_transformer*` stay BlockBase
  (CE ModBlocks.java:979-982 / :watz element-cooler).
  Leftover later: fusion peripherals / `struct_torus_core` (no stub).

## This wave (nuke_solinium / nuke_fstbmb / vitrified_barrel)

- **`nuke_solinium`**: `NukeSolinium` + `TileEntityNukeSolinium`
  (`tileentity_nuke_solinium`). 9 slots. MK3 `extType=1`, `waste=false`,
  `coefficient=1.0`, radius **150** (`BombConfig.SOLINIUM_RADIUS`).
  `EntityCloudSolinium`. GUI 176×222 CE slot coords.
  TODO(CE: GUINukeSolinium.java:16) soliniumSchematic.png missing in tree.
- **`nuke_fstbmb`**: CE `NukeBalefire` id (`ModBlocks.java:711`). Shares
  live TE `nuke_balefire` + timer 18000 + `EntityBalefire` **250**.
  `nuke_balefire` flatten stays. No invented yield.
- **`vitrified_barrel`**: `YellowBarrel`, 0.5/2.5. Idle rad **0.5/5**
  (yellow **5/75**). Explode toxic 1/3 else 18.0F, waste 35, rad 35/1500.
  No TE (CE none).

Honest E2E: compileJava 0 + runServer **Done (5.633s)** / 4052 / *:25566.
RCON place + BE Size 9 / timer 18000 / no TE on vitrified.
`/item replace` not vanilla Container. No r=150/250 detonation. No GUI.
No Exception/ERROR.

## Prior wave (seal_* / pile_brick)

- **`seal_controller`**: `BlockSeal` (`BlockSeal.java`). FACING + ACTIVATED.
  Hardness **10 / 100** (`ModBlocks.java:885`). No own TE. Click / rising
  RS / `IBomb.explode` toggle frame size **1–6**. Interior = `seal_hatch`.
- **`seal_hatch`**: `BlockHatch` + `TileEntityHatch` (`tileentity_seal_lid`).
  Hardness/resistance ∞, creative tab null. Tick: missing controller or
  `getFrameSize==0` → air.
- **`seal_frame`**: BlockBase, hardness **10 / 100** (`ModBlocks.java:884`).
- **`pile_brick`**: `BlockPileBrick` IToolable HAND_DRILL, MIN/MAX **5/15**,
  flammable 30/5, hardness **5 / 10**. Size scan + CE error strings.
  TODO(CE: BlockPileBrick.java:94-108) conversion to `pile_block` +
  `TileEntityPileCore` / `TileEntityPileBaseMK2` — do not stub.

Honest E2E: compileJava 0 + runServer **Done (5.827s)** / 4052 recipes /
*:25566. RCON size-1 SOUTH: close → hatch + BE controller long; falling
RS keeps hatch; second pulse → air; break frame → hatch air. `pile_brick`
places, no TE. No client — click/`IBomb`/HAND_DRILL not physically used.
No Exception/ERROR.

## Prior wave (reactor_research / reactor_zirnox)

- **`reactor_research`**: Dummyable `{2,0,0,0,0,0}` offset 0
  (`ReactorResearch.java:87-93`). META≥12 core TE. 12 plate slots,
  maxHeat **50000**, rod speed **0.04**, water cool `heat*0.07*water/12`,
  explode **18.0F** + deco_steel/corium/deco_steel, rad
  `heat/max*50` / 25000 (unshielded). Fuel map 7 plates → `_hot` waste
  (CE meta 1). GUI `gui_research_reactor.png` 176×222 + bind. Caps item.
  Breeding `getInteractions` reads `totalFlux`. Sensor marks core pos.
  TODO(CE: ReactorResearch.java:36) ProxyCombo;
  TODO(CE: RenderSmallReactor.java:16) TESR;
  TODO(CE: TileEntityReactorResearch.java:420-478) OC;
  TODO(CE: GUIReactorResearch.java:29) NumberDisplay 7-seg;
  TODO(CE: TileEntityReactorResearch.java:232) block_lead/desh.
- **`reactor_zirnox`**: Dummyable `{1,0,2,2,2,2}` offset 2 + XR
  `{4,-2,1,1,1,1}` / `{4,-2,0,0,2,-2}` / `{4,-2,0,0,-2,2}` + 5 extras
  (`ReactorZirnox.java:58-89`). 28 slots, tanks SHS **8000** / CO2
  **16000** / water **32000**, maxHeat/Pressure **100000**. Steam gen
  `(heat-10256)/maxHeat * min(CO2/14000,1) * 25 * 7.5`. Vent −1000 CO2.
  Redstone forces on / falling edge off. GUI `gui_zirnox.png` 203×256.
  Caps item+fluid. Meltdown: explode 12F + rad 50/15000.
  TODO(CE: ReactorZirnox.java:33) ProxyCombo;
  TODO(CE: StaticTesrBakedModels.java:315) OBJ TESR;
  TODO(CE: TileEntityReactorZirnox.java:229) tilt / IConnectionAnchors;
  TODO(CE: TileEntityReactorZirnox.java:354-431) debris / zirnox_destroyed / waste / ach;
  TODO(CE: TileEntityReactorZirnox.java:508-656) OC + ROR;
  TODO(CE: GUIReactorZirnox.java:99-104) GUIElements gauges.
- Fusion / watz skipped (huge MB). Transformers stay BlockBase.

Honest E2E: Dummyable `setPlacedBy` needs a Player — no client. compileJava 0
+ runServer **Done (1.692s)** / 4052 recipes / *:25566 + registry/caps/GUI bind.
No physical place. No Exception/ERROR.

## Prior wave (fluidtank / bigasstank / compact / satlinker)

- **`machine_fluidtank`**: Dummyable `{2,0,1,1,2,2}` offset 1 + extras
  `placed-dir ±1,±1` (`MachineFluidTank.java:79-143`). META≥12 core TE.
  6 slots, tank **256000**, mode 0=in / 1=both / 2=out / 3=off.
  `loadTank(2,3)` / `setType(0,1)` / `unloadTank(4,5)`. Antimatter →
  explode 5F + `hasExploded`; highly corrosive → `hasExploded`.
  GUI `gui_tank.png` 176×166 + mode blit 151,34 + bind. Caps item+fluid.
  Comparator. Sneak+identifier sets type.
  TODO(CE: TileEntityMachineFluidTank.java:198-235) UniNodespace pipe node;
  TODO(CE: TileEntityMachineFluidTank.java:70) OC / IControllable / IClimbable / IRepairable;
  TODO(CE: TileEntityMachineFluidTank.java:253-256) ExplosionVNT.makeAmat;
  TODO(CE: TileEntityMachineFluidTank.java:263-370) leak/fire/pollute;
  TODO(CE: MachineFluidTank.java:173-197) onBlockExploded + BombletZeta;
  TODO(CE: RenderFluidTank.java:1) TESR.
- **`machine_bigasstank`**: Dummyable `{5,0,4,4,4,4}` offset 6 + 6 XR fills
  (`MachineBigAssTank.java:48-76`). ≠ `machine_bat9000`. Extends barrel:
  6 slots, **16_000_000**, mode, `getReceiverSpeed`/`getProviderSpeed`
  `max(50000,(max-fill)/100)` / `max(50000,fill/100)`. Antimatter destroy
  + explode 10. ConPos `±dir*7`. Barrel GUI `gui_barrel.png` 176×166
  (`guiID_barrel`). Caps item+fluid. Comparator. Sneak+identifier.
  TODO(CE: TileEntityBarrel.java:247-286) UniNodespace buffer;
  TODO(CE: TileEntityBarrel.java) tilt / floor pollute;
  TODO(CE: MachineBigAssTank.java:44) ProxyCombo;
  TODO(CE: RenderBigAssTank.java:1) TESR.
- **`machine_compressor_compact`**: Dummyable `{2,0,1,1,3,3}` offset 1 + 6
  extras y+1 (`MachineCompressorCompact.java:30-52`). Subclass of compressor
  BE; only `getConPos` (6 ports) + client `fanSpin`. Reuses
  `CompressorMenu`/`CompressorScreen` (CE same GUI). Caps item+fluid+energy.
  Hardness 10 / 20 (`ModBlocks.java:1087`).
  TODO(CE: TileEntityMachineCompressorCompact.java:18-29) TESR fan;
  TODO(CE: MachineCompressorCompact.java:26) ProxyCombo.
- **`machine_satlinker`**: 1×1 MODEL (`MachineSatLinker.java:19`). ≠
  `machine_satlink`. 3 slots: copy freq 0→1 if both `ISatChip`; slot 2
  `rand(100000)` if `!SatelliteSavedData.isFreqTaken`. GUI
  `gui_sat_linker.png` 176×186 + bind. Missile tab. Caps item.

Honest E2E: Dummyable `setPlacedBy` needs a Player — no client. compileJava 0
+ runServer **Done (5.751s)** / 4052 recipes / *:25566 + registry/caps/GUI bind.
No physical place. No Exception/ERROR.

## Prior wave (leftover Dummyable + CE TE)

Casings → live BEs. Same ids. Mining laser stays accepted.

- **`machine_strand_caster`**: Dummyable `{0,0,6,0,1,0}` offset 0 + extra
  `{2,0,1,0,1,0}` (`MachineStrandCaster.java:48-81`). Live pour via
  `ICrucibleAcceptor` on extras (META≥6) + `getMetalPourPos`. Flush at 9 casts
  or 200 ticks. Tanks water/spentsteam 64000. Mold slot 0, out 1–6. GUI
  `gui_strand_caster.png` 176×214 + `SafeMenuScreens.bind`.
  TODO(CE: MachineStrandCaster.java:60) ProxyCombo.moltenMetal();
  TODO(CE: RenderStrandCaster.java:22) TESR.
- **`machine_forcefield`**: not Dummyable (`MachineForceField.java:24`). 1×1,
  hardness 5 / resistance 100, missile tab. Live bounce (exclude Player+
  ItemEntity), HE 1_000_000, baseCon 1000, r16, HP100, `isOn` button 142,34.
  GUI `gui_field.png` 176×168 + bind. Caps item+energy.
  TODO(CE: TileEntityForceField.java:436-458) IConfigurableMachine;
  TODO(CE: RenderMachineForceField.java:20) TESR.
- **`machine_chungus`**: Dummyable `{3,0,0,3,2,2}` offset 3 + extra fills
  (`MachineChungus.java:87-108`). TurbineBase 1e9/1e9, eff 0.85,
  `consumptionPercent()=1D`. Lever on compressor extras when `!operational`.
  Overlay only — CE has no GUI. Caps fluid+energy.
  TODO(CE: RenderChungus.java:16) TESR;
  TODO(CE: TileEntityChungus.java:115-163) client audio/particles;
  TODO(CE: TileEntityChungus.java:222-280) OC;
  TODO(CE: TileEntityChungus.java:69-86) IConfigurableMachine;
  TODO(CE: MachineChungus.java:40) ProxyCombo.
- **`machine_satlink`**: Dummyable `{6,0,1,0,1,0}` offset 0 + 3 extras.
  ISatChip sets freq, sky `WORLD_SURFACE<=y`, overlay freq/connected/sat info,
  IROR setfreq/tx. No GUI. Missile tab. ≠ `machine_satlinker`.
  TODO(CE: RenderSatLink.java:16) TESR;
  TODO(CE: TileEntityMachineSatLink.java:201-270) OC;
  TODO(CE: MachineSatLink.java:41) ProxyCombo.
- **`machine_teleporter`**: 1×1 MODEL (`MachineTeleporter.java:49`). HE
  1_000_000_000 / 100_000_000 per teleport, subscribe all sides. Overlay.
  (This wave: `ItemTeleLink` now live — see above.)

Honest E2E: Dummyable `setPlacedBy` needs a Player — no client. 1×1 also not
physically placed. compileJava 0 + runServer Done + registry/caps/GUI bind.

## Prior wave (`machine_mining_laser`)

- Casing `BlockBase` → Dummyable `{1,1,1,1,1,1}` offset 0, `heightOffset -1`,
  CE hardness 5 / resistance 100 (`ModBlocks.java:1177`).
- Live TE: 100_000_000 HE, 10_000/cycle, scan/break/fortune, oil 64000,
  redstone stop, `isOn` button, suck drops, `IDrillInteraction`, sandbags dam.
  SPEED/EFFECT/FORTUNE/POWER/OVERDRIVE/SCREAM via slot-scan (UpgradeManagerNT
  not ported). **No silk** — CE has none.
- Menu + CE `gui_laser_miner.png` (cmp match) + `SafeMenuScreens.bind`.
- Cited: TODO(CE: TileEntityMachineMiningLaser.java:70) UpgradeManagerNT;
  TODO(CE: TileEntityMachineMiningLaser.java:305-342) exclusive processors;
  TODO(CE: TileEntityMachineMiningLaser.java:372-388) nullifier scrapItems;
  TODO(CE: RenderLaserMiner.java:18) TESR;
  TODO(CE: MachineMiningLaser.java:35-39) ProxyEnergy/ProxyCombo.
  CE TE has no pollution increment.

## Prior wave (Sellafield worldgen)

- Reachability **63.4% (1656 / 2612)** — census does not see worldgen/`getDrops`.
- Ordinary veins / nether / depth / oil / meteor **already** CE-numbered in
  `add_overworld_ores` / nether / `add_oil_meteor_worldgen`. No invented biomes.
- Landed CE **Sellafield** crater (`HbmWorldGen.java:321-334` / `:382-384`):
  `radfreq` overworld **1-in-5000**, r=`rand(15)+10` (1/50 → 50), depth `r*0.35`,
  rings LEVEL 4→0 + slaked, core LEVEL 5. Temp ≥ 1.0, not ocean.
  Gates `ENABLE_DUNGEON_SPAWN` + `ENABLE_RAD_HOTSPOT_SPAWN`.
  Oil-sand now dungeon-gated (CE `enableDungeons`).
- Cited: phased chunk-wait TODO(CE: Sellafield.java:20-45); TE never in CE
  TODO(CE: Sellafield.java:149-155). Landmine/NITAN/dud/barrel/spaceship/
  satellite dish landed; hive/atom still TODO(CE: HbmWorldGen.java:347-368).
  Basalt ores stay volcanic-fluid (CE has no chunk vein).

## Prior wave (OreEnum drops + assembler fluids + pile_rod_mk2)

- Reachability **63.3% → 63.4%** (1649 / 2607 → 1656 / 2612). Items **2607 → 2612**.
- Flatten-holders **already flattened** — no dummy `circuit`/`shell`/`pipe`/`wire_fine`/
  `plate_welded`/`gear_large`/`battery_sc`/`pile_rod`/`mold` ids. No mold meta 16–28
  TODO(CE: AnvilRecipes.java:626-635). Anvil construct `pile_rod_mk2_{ra226be,po210be,zr,nu}`
  CE `:880-895`. Unique **118 → 122**. Honest **168 / 200**.
- Assembler SKIP7: **4 landed** (`hpcondenser` / `himarssmalltb` / `himarslargetb` /
  `mpw10taint`) via live `input_fluids`. **3 cited**:
  `ass.nitra` ChanceOutput TODO(CE: AssemblyMachineRecipes.java:1073-1087),
  `ass.digimemer` commented Mekanism TODO(CE: AssemblyMachineRecipes.java:1100-1113),
  `ass.50bmgbypass` `black_diamond` ItemModHealth TODO(CE: AssemblyMachineRecipes.java:936-938).
- CE `OreEnum` / `BlockOreBasalt` drops wired (items exist): sulfur / niter / fluorite /
  lignite / asbestos / chunk_ore_rare / cinnabar / oil_tar_crude / nugget_zirconium /
  phosphorus-nether (`ingot_phosphorus` 1/10 else `powder_fire`). Matching loot JSON
  (census). `getDrops` is the live path; fortune stays `0` like cobalt/coltan.
  `ore_nether_cobalt` / `ore_gneiss_rare` stay CE-null.
- `lignite` = CE `ItemFuel` 1200 (`ModItems.java:1339`). Model copied; **no CE png**
  (do not invent art). Lang already in tree.
- Cited stays: hot/mold/cyanide/rename, deuterium tower fluid AStack, chimney ashpit,
  thresher arm/shred, BMPowerBox control panel, wings client model.

## Prior wave (live casings + WingsMurk + 2 Anvil rows)

- Reachability **63.3% → 63.3%** (1648 / 2604 → 1649 / 2607). Items **2604 → 2607**.
- Distinct leftover I/O with CE assets: `blade_titanium`/`blade_tungsten` ItemBase,
  `blade_meteorite` ItemHot(200) (`ModItems.java:1305/:1307/:887`). MatDistribution
  `#8/#9` reconnected. Flatten holders **not** faked. No mold meta 16–28.
- Anvil rows now live: smith `:94` `flask_infusion` (flattened SHIELD), construct
  `:552-558` `missile_doomsday`. Unique **116 → 118**. Honest overlap **166 → 168 / 200**.
  Hot/mold/cyanide/rename still `TODO(CE: AnvilRecipes.java:75-130)`.
  `machine_deuterium_tower` fluid AStack `TODO(CE: AnvilRecipes.java:453-462)`.
- Casings → live machines (CE has TE, **no Container/GUI** — ILookOverlay / redstone):
  - `pump_steam` / `pump_electric` — Dummyable `{3,0,1,1,1,1}` offset 1 + BE + fluid/energy
  - `chimney_brick` / `chimney_industrial` — Dummyable + BE, smoke → pollution.
    Ashpit fly-ash feed `TODO(CE: TileEntityChimneyBase.java:46-54)`.
  - `machine_thresher` — 1×1 TE, WOODOIL, harvest mature crops.
    Arm/shred/tall-plant `TODO(CE: TileEntityMachineThresher.java:101-204)`.
  - `bm_power_box` — 1×1 redstone 15, debounce 12 ticks.
    Control panel `TODO(CE: TileEntityBMPowerBox.java:52-83)`.
  - `fluid_duct_exhaust` — live `FluidDuctBoxExhaustBlock` (kept `fluid_duct_box_exhaust` drift).
- `wings_limp` / `wings_murk` — CE `WingsMurk` flight tick, COMBAT tab.
  Client model `TODO(CE: WingsMurk.java:27-42)`. Standalone chest Equippable is the
  known JetpackBase blocker; mod-slot works.
- Assembler SKIP7 not invented. No self-drop loot padding. No invented GUI.

## Prior wave (Anvil leftover I/O)

- Reachability **62.4% → 63.3%** (1615 / 2590 → 1648 / 2604). Items **2590 → 2604**.
- Registered leftover anvil I/O **with existing CE png/json/lang**: items
  `sawblade`, `wings_limp`, `mold_base`, `wings_murk`, `deuterium_filter`,
  `egg_glyphid`, `flame_pony`; `fusion_core` as `ItemBattery(2500000)` (CE
  `ItemFusionCore`); casings `pump_steam`/`pump_electric`/`machine_thresher`/
  `fluid_duct_exhaust`/`chimney_brick`/`chimney_industrial`/`bm_power_box`.
  Chimney models parent CE `brick_concrete`/`concrete_smooth` (CE only had `.obj`).
- Anvil rows for shells/pipes/stamps + leftover machines (`heat_boiler` =
  CE `machine_boiler` field) + recycling (deco/firebox/RBMK/pile/toaster/CRT)
  whose I/O exists. Flatten holders stay flattened (`circuit_*`, `{mat}_shell`).
- Unique anvil: CE **200** vs port **116** `stack("id")` (was 67). Honest
  `stack`+`out`+`plate` overlap **166 / 200**. Remaining 34 = hot/mold/flask/
  flatten holders / `machine_deuterium_tower` fluid /
  TODO(CE: AnvilRecipes.java:75-130).
- Anvil GUI + smithing + construction E2E stayed from prior wave. New casings
  were placeable + anvil-craftable, **not** full CE Dummyable TE/GUI (this wave).
  `wings_murk` was `ItemBase` stacksTo(1), not CE flight armor (this wave).
- Assembler SKIP7 not invented. No self-drop loot padding.

## Prior wave (reachability + Anvil GUI)

- Reachability **60.5% → 62.4%** (1567 → 1615 / 2590). Census `loot_outputs()` now
  scans live `ItemPools*` (`addHbm` / `ItemPoolLookups.add`) — those pools already
  fire from structures / satellite miners / vending. Not JSON self-drop padding.
- Sat leftovers whose I/O now exists: `fluorite` 4/4/15, `gravel_diamond` 1/1/3,
  `moon_turf` 48/48/5 + 32/32/7 + 16/16/5. Stale "absent" javadoc fixed.
- **Anvil E2E**: `NTMAnvil.useWithoutItem` → `AnvilMenu` (2+1, no BE) +
  `AnvilScreen` blit CE `gui_anvil.png` + `AnvilCraftPacket` construction from
  player inv. Smithing consume on take. Recipes: upgrades + gunmetal + plates +
  deco + coils/motors + machines/armor/fuel plates with registered I/O.
  TODO(CE: AnvilRecipes.java:75-130) hot/mold/cyanide/rename.
- Liquefaction: `oil_tar_crude` BITUMEN 75, `oil_tar_crack` BITUMEN 100,
  `lignite` COALOIL 150, `lead_block` LEAD 900. coal/wood tar remaps stay.
- Assembler SKIP7 not invented.

## Prior wave (Shredder + Cyclotron start)

- Family: **Shredder**. Leftover CE `registerDefaults` rows → JSON `data/hbm/recipe/shredder/`
  (TE already queries `ProcessingRecipes.SHREDDER_TYPE`). **201** files, **200** unique
  inputs (`quartz.json` + `quartz_item.json` both key `minecraft:quartz` — not triplicated).
- Registered I/O with existing CE png/lang/models: `scrap`, `scrap_nuclear`, `scrap_oil`,
  `scrap_plastic` (CE tab=null → hidden), `pipes_steel`, `debris_{concrete,shrapnel,exchanger,element,metal,graphite}`.
- Fixed `obsidian.json` output to `hbm:gravel_obsidian` (CE `:229`; was vanilla gravel).
- Wood OreDict loops → 3 tag recipes (`minecraft:logs`/`planks`/`saplings`), not per-wood files.
- `schrabidate_block` is Mats BLOCK autogen of CE `block_schrabidate` (not a second id).
- Sellafield flatten: one BlockItem → `scrap_nuclear`×1 (CE `:352` meta 0).
- Cited skips: `TODO(CE: ShredderRecipes.java:119-201)` registerPost,
  `:103-115` miss→scrap fallback (scrap exists; TE still rejects no-match),
  `:246` other `dustLapis`, `:348` old `bedrock_ore`, `:353-357` sellafield LEVEL 1-5,
  `:400-402` bobbleheads, `:412-423` GC/AR (commented in CE).
- Cyclotron: **42** unique CE rows now in `CyclotronRecipes` (was 12). Catalysts
  `part_*` (already registered). Li+gold → `nugget_mercury`. `dustPhosphorus` live
  member = `powder_fire`.

## Prior wave (ChemPlant verify + Crystallizer)

- ChemPlant: **72 unique `chem.*` names in CE = 72 in port**. Census 145 = `this.register` +
  `.register`. No rows added.
- Family: **Crystallizer** leftover live (`MachineCrystallizerBlockEntity.getOutput`). Unique
  **303 / ~309**. Bedrock wash loop 222, dye loop 18, leftover ores/utilities. Registered
  `coal_infernal` (CE fuel 4800, existing png/lang).
- Skips cited: `TODO(CE: CrystallizerRecipes.java:75)` LI.ore, `:79` malachite scrap,
  `:103` mustardwillow, `:199-216` AE2 / P_WHITE.dust / CINNABAR.dust.
- CE `gui_crystallizer_alt.png` blit-wired 176×204. Fluid-id slots stay
  `TODO(CE: ContainerCrystallizer.java:38-42)`.
- Centrifuge 75/78 + AE2 skip unchanged.

## Prior wave (104.2% → 104.3%, 8104 / 7767)

- Family: **Centrifuge** leftover rows now in the existing machine table (BE already calls
  `CentrifugeRecipes.getOutput`). Added `chunk_ore_rare`, `ore_aluminium`, `ore_nether_plutonium`,
  `block_slag`, `powder_ash_coal`, `crystal_aluminium`. Mercury outputs restored on redstone /
  `crystal_gold` / `crystal_redstone` (CE `ingot_mercury` id = `nugget_mercury`).
- Registered `block_slag` with CE texture/model (not `slag_block`, not orphan `block_slag_0`).
  Chunk-ore item models point at existing CE `chunk_ore.*` pngs.
- CE `gui_centrifuge.png` already in jar; screen blit-wired (power 37px + 4 progress columns).
  Menu slots match `ContainerCentrifuge` (44/57 in, 8/57 batt, 70/90/110/130 out, 156 upgrades,
  player 11/107). Gray-box gone.
- AE2 `oreCertusQuartz` skip: `TODO(CE: CentrifugeRecipes.java:243-254)`.
- AmmoPress fluid-slot leftover stays cited: `TODO(CE: AmmoPressRecipes.java:936+)`. CE TE has no tank.
- SuperComputer dropdown still skip — `ModuleMachineBase` class missing.
- SILEX DRX stays cited skip.

## Prior wave (103.7% → 104.2%, 8096 / 7767)

- CE `gui_electrolyser_fluid.png` / `gui_electrolyser_metal.png` already in jar; screens blit CE UVs
  (power / progress / molten tint / power-ok). Gray-box gone.
- Electrolyser fluid-id + canister I/O: `FluidTankNTM.setType`/`loadTank`/`unloadTank` + loaders
  (`FluidLoaderStandard` / `FillableItem` / `Infinite`). Slots 3-10 CE coords.
- Family: **AmmoPress** (next after Electrolyser/Mixer). 28 leftover `registerDefaults` rows
  (NUKE_BALEFIRE skipped — item id collides with bomb BlockItem). Registered `p45_*`,
  `nuke_standard/demo/high/tots/hive`, `assembly_nuke` with existing CE textures/lang. No invent.

## Prior waves (86.8% → 96.4%, 7489 / 7767)

Previous published snapshot: weighted **86.8%** / unweighted **93.3%**, vanilla **1416 / 1950
(72.6%)**, machine **1682 / 83.7%**, items **2056**, blocks **790**. Short of 90% by **249**.

### Extractor: already-registered helpers

`scripts/phase10_remap_v3.py` HELPERS now matches `registerBillet` / `registerPowder` /
`registerFuelPowder` / `registerParts` / `registerWaste` / `registerRtgPellet` /
`registerResource`. Those items were already in `BilletPowderItems` / `PlateCrystalWasteItems` /
etc. Census items **2056 → 2354**. Not dummy registration.

### Vanilla leftover JSON (CE-faithful, no invented rows)

- `scripts/phase11_wave12_minerals.py` — CE `MineralRecipes.java` `add1To9Pair` / `addMineralSet` /
  `addBillet` + explicit 9-pack. Skips unregistered I/O and banned results.
- `scripts/phase11_wave12_rods.py` — leftover empty-rod / RBMK / RTG rows whose ids are visible.
  Pile/PWR fuels already in datagen (`src/generated/.../rod/`); not re-emitted under new filenames.
- `scripts/phase11_wave13_leftover.py` — leftover Tool / Armor / Consumable / Exclusive crafts
  with registered I/O only.

Vanilla **1416 → 1866**. Recipe JSON total **2174 → 2624**. Dropped `gas_mask_filter` (DataComponent id, not an item).

Dropped (not invented): armor sets whose pieces are unregistered (`steel_helmet` / hazmat /
`gas_mask_m65`), `ring_starmetal`, `block_schrabidium`, SEDNA `part_stock_*` / `part_grip_*`,
`insert_*` / `cladding_*` / `pads_*` / `servo_set`, `block_steel` (Mats id is `steel_block`),
foods (`pancake` / `quesadilla` / …), pile/PWR concat ids already owned by datagen.

Banned results still skipped: `powder_sawdust` / `gem_tantalium` / `coil_tungsten`.

### Unchanged this wave

- Machine **1781 → 1809 / 90.0%**. AmmoPress 60→88. CE `registerDefaults` 89; NUKE_BALEFIRE
  skipped (`nuke_balefire` BlockItem collision, no invented id). Census CE 91 includes
  JSON `recipes.add` helper. ElectrolyserMetal 21/23.
  Cited leftovers: SILEX DRX `:417-431` (`undefined`), SuperComputer dropdown.
- Blocks **790 / 67.6%**. No dummy blocks.
- Assembler skip **3** (fluid-dict four landed this wave). `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Client NPE fix (`CrucibleBlocks.registerAll` + `SafeMenuScreens`) from prior HEAD.

## Exclusion list (only CE-lacks or deliberate skips)

- Double-slab flatten (1.21 has no double-slab block)
- CE `entity_clound_solinium` typo → `entity_cloud_solinium`
- `GlyphidHive` is a structure, not an entity
- Soyuz pad `LAUNCHING` is TBI **in CE itself**
- Projectile tails = fallback renderer
- Assembler expensive-mode `inputItemsEx` legs — dropped (same as prior assembler ports)
- PA recipe `#10` SBD.ingot() — no schrabidate INGOT autogen
- Texture misses with no CE file — documented, no invented art
- ElectrolyserMetal 21/23. CE GUI pngs copied + blit-wired (fluid + metal).
  Fluid-id / canister slots 3-10 live (`setType`/`loadTank`/`unloadTank`).
  `chunk_ore_*` registered; CE has no `chunk_ore*.png` — no invented art.
- PUREX chance-output / ICF / vitrification / naquadria — missing I/O
- Full Albion beam physics — detector runs the recipe table locally
- AmmoPress fluid-slot recipes (FLAME_*) — stored, not consumed (no tank on the press)
- PlasmaForge late-game recipes with 11–12 item stacks — counted, TE still 6 slots
- Assembler leftover skip **3**: `ass.nitra` ChanceOutput, `ass.digimemer` (commented Mekanism in CE), `ass.50bmgbypass`
  (`black_diamond` is `ItemModHealth`, not a dummy). Fluid-dict four now have JSON.
- Combination coke/briquette/powder_ash item flatten — put sites present, AIR filtered
- Blast `meteorite_sword_*` — items missing, row dropped after register
- RockMill AE2 module / blueprint cycling — auto-detect instead
- Annihilator 528 gate — table registered unconditionally (same CE lines)
- SILEX DRX pellet — TODO(CE: SILEXRecipes.java:417-431) `ModItems.undefined` not registered
- Press meteorite sword / briquette / page_of — AIR filtered
- Rotary `ModuleBurnTime` heat mods — vanilla burn/2
- WasteDrum RBMK rod heat path — `ItemRBMKRod.updateHeat` not in this port
- Fraction CE chat inspect — replaced with a real tank menu (not a stub)
- Compressor upgrades (`UpgradeManagerNT`) — slots exist, levels unused (manager not ported)
- Compressor compact — still a cube casing (assembler output only)
- Cracker CE overlay — replaced with a real tank menu (not a stub)
- Reformer / hydrotreater canister `loadTank`/`unloadTank` — FluidTankNTM item-canister subsystem
  not ported (same skip as diesel / crystallizer / PWR)
- Coker pollution soot increment — PollutionHandler tick not wired on this TE
- Vacuum distill canister unload — FluidTankNTM item-canister subsystem not ported
- Radiolysis sterilize (`ntmContagion` NBT) — not ported; slots exist
- Flare pollution / particles / entity fire / tilt — skipped
- Flare / EPress `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier instead
- PyroOven tar→soot — `powder_ash_*` now registered; row still skipped pending wire-up
- PyroOven / arc furnace pollution / audio / particles / lid animation — skipped
- Arc furnace vanilla furnace autogen — RecipeManager not available at commonSetup
- Arc furnace `sand_quartz` → `glass_quartz` — `sand_quartz` unregistered
- Exposure expensive-mode DEGENERATE_MATTER — cheap path only
- Pyro / arc / exposure `UpgradeManagerNT` — slot scan by `ItemMachineUpgrade` type/tier
- Slopper animation / entity shred — skipped
- Turbofan pollution / particles — skipped
- Radgen depleted/tiny leftovers — items unregistered
- Hephaestus `volcanic_lava_block` — unregistered; CE overlay-only (port has a real menu)
- Wood burner `powder_ash_*` registered; ash emit + `loadTank` canister path not ported
- Wood burner / slopper / turbofan `UpgradeManagerNT` — slot scan by type/tier instead
- Iron / steel furnace pollution + particles; steel ore bonus
- Firebox `ModuleBurnTime` fuel-class mods + ashpit + door anim
- Oilburner `loadTank` / pollution
- Sawmill blade / entity shred / overspeed (`sawblade` unregistered → blade assumed)
- Heaters / steel furnace / sawmill CE crafts are **anvil**, not vanilla
- SuperComputer CE recipe dropdown (`IControlReceiver` / ModuleMachineBase) — auto-match instead
  TODO(CE: TileEntityMachineSuperComputer.java:186-194)
- Autosaw entity shred skipped
- `catalytic_converter` `ANY_BISMOID.ingot()` → `ingot_bismuth` (no `any_bismoid` tag)
- wave11 drops: `powder_fire` / `key` / `gear_large` unregistered (`sawblade`/`mold_base` now registered)
- wave13 drops: unregistered armor pieces / SEDNA parts / inserts / claddings / foods / `block_steel`
- fluid-NBT / `OreDictionary.WILDCARD` / LBSM-gated / commented CE crafts skipped

## Recipe-graph reachability (cheap)

**63.3%** (1648 / 2604). JSON loot + Java machine-table **outputs** + live `ItemPools*`
(`addHbm` / `ItemPoolLookups.add`). Inputs not counted. Self-drop block loot not added.

Before this wave: **62.4%** (1615 / 2590). +27 items / +6 blocks from leftover anvil I/O
+ matching construction/recycle rows (not self-drop padding).

## Next single gap

**Anvil** unique leftover: CE **200** vs port **122** `stack("id")` / honest **168 / 200**.
Remaining 32 wait on hot/mold (`AnvilSmithingHotRecipe` / `AnvilSmithingMold` + mold meta
16–28), `machine_deuterium_tower` fluid AStack, flatten holders.
TODO(CE: AnvilRecipes.java:75-130) / TODO(CE: AnvilRecipes.java:626-635). Do not invent.

Next other family by unique CE vs port (not regex 145-style): Press **41/38**,
Combination ~done, Exposure done, Liquefaction leftovers landed, Solidification live.
Assembler skip 3. Leftover CE dungeons (hive/desert-atom).
Reachability **63.4%** still the owner pain (census misses worldgen). `v0.0.1-rc2` stays.
Anvil E2E: smithing consume/produce **yes** (menu slots). Construction **yes**
(`AnvilCraftPacket` + player inv). GUI blit CE `gui_anvil.png`, not gray-box.

## Entities (Phase 9 leftovers)

All leftover classes spawn-wired at prior HEAD. No other CE `@AutoRegister` entity missing.
Cites: `docs/phase9/ENTITY_CENSUS.md`.
