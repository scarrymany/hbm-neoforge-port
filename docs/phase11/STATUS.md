# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.1% weighted / 104.0% unweighted** (8237 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: **live leftover casings + WingsMurk + 2 Anvil rows**.
  Reachability **63.3% (1648 / 2604) → 63.3% (1649 / 2607)**. Items **2604 → 2607**.
- **Anvil unique**: CE **200** Mod* `new ItemStack` outs vs port **118**
  `stack("id")` (was 116). Honest overlap incl. `plate()`/`out()` flatten **168 / 200**
  (was 166). Leftover **32**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_deuterium_tower` fluid AStack TODO(CE: AnvilRecipes.java:453-462), flatten holders
  (`circuit`/`shell`/`pipe`/`wire_fine`/`plate_welded`/`gear_large`/`battery_sc`/`pile_rod`/`mold`).
  Do not invent mold meta 16–28.
- Live machines (CE has TE, **no GUI**): `pump_steam`/`pump_electric` Dummyable+BE,
  `chimney_brick`/`chimney_industrial` Dummyable+BE, `machine_thresher` 1×1 TE,
  `bm_power_box` redstone TE, `fluid_duct_exhaust` live exhaust duct
  (kept `fluid_duct_box_exhaust` drift).
- `wings_limp`/`wings_murk` = CE `WingsMurk` flight. Client model
  TODO(CE: WingsMurk.java:27-42).
- Vanilla **1898 / 97.3%**. Machine census **1920 / 95.6%** (regex; anvil is Java table).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (5.609s)** / 4047 recipes (anvil is Java table, not JSON), port 25566.
- No tag (reachability still ~63%). `master` untouched.
