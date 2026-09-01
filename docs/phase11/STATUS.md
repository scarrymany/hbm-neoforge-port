# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.2% weighted / 104.1% unweighted** (8246 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: **OreEnum drops + assembler fluid crafts + pile_rod_mk2 anvil**.
  Reachability **63.3% (1649 / 2607) → 63.4% (1656 / 2612)**. Items **2607 → 2612**
  (`lignite` + `fuel()` helper now counted).
- **Anvil unique**: CE **200** Mod* `new ItemStack` outs vs port **122**
  `stack("id")` (was 118). Honest overlap incl. `plate()`/`out()` flatten **168 / 200**.
  Leftover **32**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_deuterium_tower` fluid AStack TODO(CE: AnvilRecipes.java:453-462), flatten holders
  (`circuit`/`shell`/`pipe`/`wire_fine`/`plate_welded`/`gear_large`/`battery_sc`/`pile_rod`/`mold`).
  Do not invent mold meta 16–28. TODO(CE: AnvilRecipes.java:626-635).
- Live machines (CE has TE, **no GUI**): `pump_steam`/`pump_electric` Dummyable+BE,
  `chimney_brick`/`chimney_industrial` Dummyable+BE, `machine_thresher` 1×1 TE,
  `bm_power_box` redstone TE, `fluid_duct_exhaust` live exhaust duct
  (kept `fluid_duct_box_exhaust` drift).
- `wings_limp`/`wings_murk` = CE `WingsMurk` flight. Client model
  TODO(CE: WingsMurk.java:27-42).
- Vanilla **1898 / 97.3%**. Machine census **1924 / 95.8%** (regex; anvil is Java table).
- Assembler skip **3** (nitra / digimemer / 50bmgbypass). `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (5.375s)** / 4051 recipes (anvil is Java table, not JSON), port 25566.
- No tag (reachability still ~63%). `master` untouched.
