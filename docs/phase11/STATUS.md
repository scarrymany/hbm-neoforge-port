# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.2% weighted / 104.1% unweighted** (8246 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: **Sellafield hotspot worldgen**. Veins/nether/depth/oil already
  CE-numbered in biome modifiers. Reachability **63.4% (1656 / 2612)** unchanged
  (census is recipe/loot, not worldgen).
- **Anvil unique**: CE **200** vs port **122** `stack("id")`. Honest **168 / 200**.
  Leftover **32**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_deuterium_tower` TODO(CE: AnvilRecipes.java:453-462), flatten holders,
  mold 16–28 TODO(CE: AnvilRecipes.java:626-635).
- Live machines (CE has TE, **no GUI**): pumps/chimneys Dummyable+BE, thresher,
  `bm_power_box`, `fluid_duct_exhaust`. WingsMurk flight.
- Vanilla **1898 / 97.3%**. Machine census **1924 / 95.8%**.
- Assembler skip **3** (nitra / digimemer / 50bmgbypass). `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (5.081s)** / 4051 recipes, port 25566.
- No tag (reachability still ~63%). `master` untouched.
