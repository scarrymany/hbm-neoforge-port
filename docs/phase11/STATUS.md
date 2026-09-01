# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.2% weighted / 104.1% unweighted** (8248 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: GlyphidHive 1/256 + DesertAtom 0:500. Dud/Barrel/Spaceship/Satellite stay
  accepted. Reachability **63.4% (1657 / 2613)**.
- **`linker`**: `ItemTeleLink`. stacksTo(1), CONSUMABLE. `DETONATOR_POS` (CE NBT
  x/y/z). Click = set; sneak on teleporter = `target`+`linked=true`, clear.
  CE craft `ToolRecipes.java:107` `I I/ICI/GGG`. Existing png+lang. No invent.
- **`machine_teleporter`**: linker live. Overlay. HE 1e9 / 1e8 per tp.
- **Landmine**: `enableDungeons`+`enableMines`, `minefreq` overworld **64**,
  `mine_ap` + `waitingForPlayer`. Step `TOP_LAYER_MODIFICATION` (CE post-decorate).
  E2E: **2** `hbm:mine_ap` in 841 spawn chunks after wipe.
- **NITAN**: `enableNITAN` only. 8 coords y=250, `POOL_POWDER`×29. Not in spawn.
- **Dud** (`Dud.java` / `HbmWorldGen.java:379`): `enableDungeons`,
  `dudStructure` **0:500**, no biome. Random `crashed_bomb_*` flags `2|16`.
  Sandstone spawn. Default 1/500: **0/841** (λ≈1.7 miss). Forced 1/1:
  **70** duds (23 nuke / 19 conv / 17 salted / 11 balefire).
- **Barrel** (`Barrel.java` / `:370-371`): `enableDungeons`,
  `barrelStructure` **0:5000**, `temp>1.8` only. Schematic 289 cells,
  `POOL_EXPENSIVE`×16, `toxic_block` registered (still + CE tex + walk rad;
  flow/fog TODO(CE: ToxicBlock.java:26-105)). Default spawn plains: miss.
  Forced 1/1: **7** `crate_steel` / **9** `toxic_block` / **11** sellafield
  chunks. Height = in-chunk column (neighbor min-Y is 0 during Feature).
- **Spaceship** (`Spaceship.java`+`Spaceship2` / `:377`): `enableDungeons`,
  `spaceshipStructure` **0:1000**, no biome. Corners 13×24 sandstone, `y+=1`.
  Schematic 1419 cells. `POOL_SPACESHIP`×12 ×4 + `POOL_EXPENSIVE`×12 1/10
  `gun_vortex`. `hadron_coil_alloy` + `machine_generator` registered with CE
  assets. `fusion_core` stays the battery item; block id is `fusion_core_block`.
  Forced 1/1 plains: schematic load + **14** `deco_tungsten` / **8** `pwr_fuelrod`
  / **1** `hadron_coil_alloy` (write-radius 0 clips overflow cells).
- **Satellite dish** (`Satellite.java` / `:373-374`, not satlink):
  `enableDungeons`, **0:500**, `temp<1 || temp>1.8`. Corners 25×31 sandstone.
  Schematic 2474 cells. Forced 1/1: schematic load + **8** `deco_titanium` /
  **6** `deco_beryllium` / **1** `tape_recorder`.
- FEATURES write-radius 0: skip cells outside the generating chunk (do not
  `ServerLevel.setBlock` — cascades). `ensureCanWrite` itself logs far-chunk.
  Same skip as Spaceship/Satellite 1.21 — no invented ServerLevel cascade.
- **GlyphidHive** (`GlyphidHive.java` / `HbmWorldGen.java:347-358`):
  `enableDungeons`+`enableHives`, overworld, `hiveSpawn` **256**. `y =
  getTopSolidOrLiquidBlock+1`, `k=3..-1` first full cube. 1/10 infected,
  worldgen loot=true. Schematic 11×5×11. `glyphid_spawner` + TE swarm
  (all 9 glyphid entity types already registered). Piles
  `POOL_PILE_BONES` / `POOL_PILE_HIVE`.
- **DesertAtom** (`DesertAtom001-3` / `:367-368`): `enableDungeons`,
  `atomStructure` **0:500**, `!hasPrecipitation && temp>=2`. Height/spawn
  at offset `(20,0,16)` + sandstone + terracotta. Schematic 5162 cells.
- Sellafield crater `radfreq` 1/5000 + ore veins stay accepted.
- **Anvil unique**: CE **200** vs port **122** `stack("id")`. Honest **168 / 200**.
  Leftover **32**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_deuterium_tower` TODO(CE: AnvilRecipes.java:453-462), flatten holders,
  mold 16–28 TODO(CE: AnvilRecipes.java:626-635).
- This Dummyable wave: `turret_arty` / `turret_himars` `{1,0,2,1,2,1}` offset 1,
  hardness 5/600. Live BE aim+fire. Arty 100k HE, V0 50/20, delay 300/40,
  modes artillery/cannon/manual. HIMARS 1e6 HE, crane reload, 40t, V0 25.
  Ammo = existing flattened `ammo_arty_*` / `ammo_himars_*` (no invent).
  Projectiles replace Phase9 `EntityThrownTail` stubs. GUI CE png + bind.
  OBJ TESR cited skip. Fusion/watz skipped. Factories stay accepted.
- Prior Dummyable: `reactor_research` / `reactor_zirnox` stay accepted.
- Live machines (CE has TE, **no GUI**): pumps/chimneys Dummyable+BE, thresher,
  `bm_power_box`, `fluid_duct_exhaust`. WingsMurk flight.
- Vanilla **1899 / 97.4%**. Machine census **1924 / 95.8%**.
- Assembler skip **3** (nitra / digimemer / 50bmgbypass). `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (1.915s)** / 4052 recipes, port 25566.
  Dummyable E2E = registry/caps/GUI bind (no client, no physical place).
  `machine_assembly_factory` / `machine_chemical_factory` boot with no Exception/ERROR.
- Honest E2E: MCA 841 chunks. Forced `hiveSpawn=1` + `atomStructure=1` on
  desert seed `1833280291927865410`: hive **57** `glyphid_base` / **56**
  `glyphid_spawner` / **56** `deco_loot` / **53** wither skull / **5**
  infested (~1/10). Atom **48** `reinforced_sand` / **48** `barbed_wire` /
  **10** `yellow_barrel` / **7** `lead_block` / **3** `ore_nether_plutonium`.
  `nuke_man`/`waste_earth`/`uranium_block` = 0 (write-radius 0 clip).
  Defaults stay **256** / **500**. No hive/atom far-chunk logs (4 leftover
  `oil_bubble`). No client.
- No tag (reachability still ~63%). `master` untouched.
