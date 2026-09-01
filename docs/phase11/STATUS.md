# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.2% weighted / 104.1% unweighted** (8248 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: Dud + Barrel CE structures. `linker` / landmine / NITAN stay
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
- Cited leftover (no generator): hive 256 `GlyphidHive`; desert-atom 0:500
  `!canRain && temp>=2`; satellite 0:500 `temp<1 || temp>1.8`;
  spaceship 0:1000 (`Spaceship`+`Spaceship2`, 1108+452). Next, not skip-forever.
  TODO(CE: HbmWorldGen.java:347-377).
- Sellafield crater `radfreq` 1/5000 + ore veins stay accepted.
- **Anvil unique**: CE **200** vs port **122** `stack("id")`. Honest **168 / 200**.
  Leftover **32**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_deuterium_tower` TODO(CE: AnvilRecipes.java:453-462), flatten holders,
  mold 16–28 TODO(CE: AnvilRecipes.java:626-635).
- Live machines (CE has TE, **no GUI**): pumps/chimneys Dummyable+BE, thresher,
  `bm_power_box`, `fluid_duct_exhaust`. WingsMurk flight.
- Vanilla **1899 / 97.4%**. Machine census **1924 / 95.8%**.
- Assembler skip **3** (nitra / digimemer / 50bmgbypass). `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (5.785s)** / 4052 recipes, port 25566.
- Honest E2E: MCA 841 chunks. Default Dud miss; forced Dud 70 + Barrel 7+. No client.
- No tag (reachability still ~63%). `master` untouched.
