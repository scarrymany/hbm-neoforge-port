# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **104.3% weighted / 103.2% unweighted** (8104 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- This wave: Crystallizer leftover rows live in existing machine. Registered
  `coal_infernal` (CE `ItemFuel` 4800, existing png/lang). GUI
  `gui_crystallizer_alt.png` blit-wired (176×204). Fluid-id slots stay cited trim.
- Unique Crystallizer: CE ~309 (57 flat + 222 bedrock + 18 dye + 12 post; 3 OreDict
  compat gated). Port **303**. Skips: `LI.ore`, malachite scrap, mustardwillow,
  AE2 / P_WHITE.dust / CINNABAR.dust.
- Vanilla **1898 / 97.3%**. Machine census **1815 / 90.3%** (helper `register(` not
  counted — do not chase). Reachability **60.3%** (1559 / 2586).
- Centrifuge **75/78** accepted. AE2 stays cited. AmmoPress fluid-slot stays cited.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0. `runServer` pending this revision.
- No tag. `master` untouched.
