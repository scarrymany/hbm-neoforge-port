# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.2% weighted / 104.1% unweighted** (8246 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: leftover Dummyable/CE-TE machines (not cubes). Mining laser stays
  accepted. Reachability **63.4% (1656 / 2612)** unchanged (census is recipe/loot).
- **`machine_strand_caster`**: Dummyable `{0,0,6,0,1,0}` offset 0 + extra
  `{2,0,1,0,1,0}`. Live pour, 9-cast / 200-tick flush. GUI
  `gui_strand_caster.png` 176×214 + `SafeMenuScreens.bind`. Water/spentsteam
  64000. Cited ProxyCombo molten / TESR `RenderStrandCaster.java:22`.
- **`machine_forcefield`**: not Dummyable (1×1). Live bounce. GUI `gui_field.png`
  176×168 + bind. HE 1e6, r16, HP100. Cited IConfigurableMachine /
  TESR `RenderMachineForceField.java:20`.
- **`machine_chungus`**: Dummyable `{3,0,0,3,2,2}` offset 3. TurbineBase 1e9/1e9,
  eff 0.85, consume 100%. Lever densify. Overlay only (CE has no GUI). Cited
  TESR / client audio / OC / IConfigurableMachine / ProxyCombo.
- **`machine_satlink`**: Dummyable `{6,0,1,0,1,0}` offset 0. ISatChip + sky +
  IROR + overlay. No GUI. Cited TESR / OC / ProxyCombo.
- **`machine_teleporter`**: 1×1. HE 1e9 / 1e8 per tp. Overlay. Target null —
  `ItemTeleLink` not ported `TODO(CE: ItemTeleLink.java:38-45)`.
- Sellafield crater `radfreq` 1/5000 + ore veins stay accepted.
- Hive/barrel/satellite/spaceship/dud/landmine/NITAN stay later dungeon wave.
- **Anvil unique**: CE **200** vs port **122** `stack("id")`. Honest **168 / 200**.
  Leftover **32**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_deuterium_tower` TODO(CE: AnvilRecipes.java:453-462), flatten holders,
  mold 16–28 TODO(CE: AnvilRecipes.java:626-635).
- Live machines (CE has TE, **no GUI**): pumps/chimneys Dummyable+BE, thresher,
  `bm_power_box`, `fluid_duct_exhaust`. WingsMurk flight.
- Vanilla **1898 / 97.3%**. Machine census **1924 / 95.8%**.
- Assembler skip **3** (nitra / digimemer / 50bmgbypass). `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (5.927s)** / 4051 recipes, port 25566.
- No tag (reachability still ~63%). `master` untouched.
