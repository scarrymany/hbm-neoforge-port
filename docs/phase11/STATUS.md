# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **104.2% weighted / 103.1% unweighted** (8096 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- This wave: CE Electrolyser GUI pngs copied + blit-wired (not gray-box). Fluid-id /
  canister leftover (`setType`/`loadTank`/`unloadTank` slots 3-10). AmmoPress leftover
  family (88 `RECIPES.add`; NUKE_BALEFIRE BlockItem collision). SuperComputer dropdown still TODO
  (no ModuleMachineBase). DRX stays cited skip.
- Vanilla **1898 / 97.3%**. Machine **1809 / 90.0%** (AmmoPress 88; NUKE_BALEFIRE id collides
  with `nuke_balefire` BlockItem). ElectrolyserMetal 21/23. Blocks **1007 / 86.1%**.
- Reachability **60.0%** (1552 / 2585).
- Items census **2585** (+p45×5 +nuke×5 +`assembly_nuke`; nuke_balefire BlockItem already counted).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99% is a gate. Not content-complete.
- Verified: `compileJava` 0, `runServer` **Done (5.608s)** / 3946 recipes, port 25566.
- No new tag: CE pngs in jar + AmmoPress solids playable, but client blit not opened and
  NUKE_BALEFIRE ammo still cited. `v0.0.1-rc2` stays.
- `master` untouched.
