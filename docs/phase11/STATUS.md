# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.2% weighted / 104.1% unweighted** (8246 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: **`machine_mining_laser`** Dummyable + live CE TE (was BlockBase casing).
  Reachability **63.4% (1656 / 2612)** unchanged (census is recipe/loot, not TE).
- Dummyable `{1,1,1,1,1,1}` offset 0, `heightOffset -1`. HE 100M / 10k cycle.
  EFFECT range, FORTUNE 0–3, **no silk** (CE has none). Menu + `gui_laser_miner.png`
  + `SafeMenuScreens.bind`.
- Cited: UpgradeManagerNT, exclusive processors, nullifier scrapItems, TESR,
  ProxyCombo. CE laser has **no pollution** increment.
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
- Verified: `compileJava` 0, `runServer` **Done (6.138s)** / 4051 recipes, port 25566.
- No tag (reachability still ~63%). `master` untouched.
