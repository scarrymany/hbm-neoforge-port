# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **105.7% weighted / 103.8% unweighted** (8209 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- ChemPlant **72 unique CE names = 72 unique port names**. Census 145 is
  `this.register` + `.register` double-count. Left ChemPlant alone.
- Crystallizer unique **303 / ~309** accepted (OreDict/AE2 skips cited).
- This wave: **Shredder** leftover `registerDefaults` rows live in JSON
  `hbm:shredder` (TE already ticks `SHREDDER_TYPE`). Unique: CE **177 sites** /
  **~211** expanded skulls+wool+clay. Port **201 JSON / 200 unique inputs**
  (`quartz`+`quartz_item` both `minecraft:quartz`). Cited skips: `registerPost`
  OreDict, old `bedrock_ore`, sellafield LEVEL 1-5, bobbleheads, GC/AR,
  `dustLapis` other mods.
- Cyclotron started: CE **42 unique** `makeRecipe` (regex 43 includes helper) =
  port **42**. Catalysts are real `part_*`. Li+gold → `nugget_mercury` (CE
  `ingot_mercury` field / `nugget_mercury` id).
- Vanilla **1898 / 97.3%**. Machine census **1916 / 95.4%** (regex; helper
  `register(` / `makeRecipe` not counted — do not chase). Reachability
  **60.5%** (1567 / 2590).
- Centrifuge **75/78** accepted. AE2 stays cited. AmmoPress fluid-slot stays cited.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Verified: `compileJava` 0. `runServer` pending this revision.
- No tag. `master` untouched.
