# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **105.7% weighted / 103.8% unweighted** (8213 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- Shredder unique + Cyclotron **42=42** accepted. Cited skips stay.
- This wave: **reachability** 60.5% (1567 / 2590) → **62.4%** (1615 / 2590).
  Honest: census now counts live Java `ItemPools*` (`addHbm` / `ItemPoolLookups.add`);
  satellite leftovers `fluorite` / `gravel_diamond` / `moon_turf` (I/O exists);
  Anvil GUI + smithing/construction rows whose I/O exists. No fake ids. No
  invented assembler SKIP7. No self-drop loot padding.
- **Anvil** next family by unique CE vs port (not regex): CE **200 unique**
  ModItems/Blocks outs / **236** add-sites (42 smithing + 194 construction).
  Port **67 unique** outputs with registered I/O (54 `stack("id")` + 13 plates)
  + live GUI (`ContainerAnvil` / `GUIAnvil` / `AnvilCraftPacket` / CE texture).
  Cited skips: hot/mold/cyanide/rename TODO(CE: AnvilRecipes.java:75-130),
  `machine_boiler` unregistered, shell/pipe/sawblade/recycling I/O AIR.
- Liquefaction leftovers landed: `oil_tar_crude`/`oil_tar_crack` → BITUMEN,
  `lignite` gem, `lead_block` → LEAD 900. coal/wood tar remaps untouched.
- Vanilla **1898 / 97.3%**. Machine census **1920 / 95.6%** (regex).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0. `runServer` this wave after push.
- No tag (reachability still ~62%, not a jump that justifies a playtest jar).
  `master` untouched.
