# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **106.0% weighted / 104.0% unweighted** (8234 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: **Anvil leftover I/O**. Reachability **62.4% (1615 / 2590) → 63.3% (1648 / 2604)**.
  Registered CE-asset I/O (`sawblade`/`wings_limp`/`mold_base`/`wings_murk`/
  `deuterium_filter`/`egg_glyphid`/`flame_pony`/`fusion_core` + casings
  `pump_*`/`machine_thresher`/`fluid_duct_exhaust`/`chimney_*`/`bm_power_box`)
  then matching AnvilRecipes rows. No fake ids. No invented assembler SKIP7.
- **Anvil unique**: CE **200** Mod* `new ItemStack` outs vs port **116**
  `stack("id")` (was 67). Honest overlap incl. `plate()`/`out()` flatten **166 / 200**.
  Leftover **34**: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  flask infusion meta, `machine_deuterium_tower` fluid AStack, flatten holders
  (`circuit`/`shell`/`pipe`/`wire_fine`/`plate_welded`/`gear_large`/`battery_sc`/`pile_rod`/`mold`).
- Vanilla **1898 / 97.3%**. Machine census **1920 / 95.6%** (regex; anvil is Java table).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0, `runServer` **Done (5.072s)** / 4047 recipes (anvil is Java table, not JSON), port 25566.
- No tag (reachability still ~63%). `master` untouched.
