# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **104.2% weighted / 103.1% unweighted** (8097 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- This wave: CE Electrolyser GUI pngs copied + blit-wired (not gray-box). Fluid-id /
  canister leftover (`setType`/`loadTank`/`unloadTank` slots 3-10). AmmoPress leftover
  family (89 `RECIPES.add` = CE `registerDefaults`). SuperComputer dropdown still TODO
  (no ModuleMachineBase). DRX stays cited skip.
- Vanilla **1898 / 97.3%**. Machine **1810 / 90.1%** (AmmoPress 89, ElectrolyserMetal 21/23).
  Blocks **1007 / 86.1%**.
- Reachability **60.0%** (1552 / 2585).
- Items census **2585** (+p45×5 +nuke×6 +`assembly_nuke`).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99% is a gate. Not content-complete.
- Verified: `compileJava` 0. `runServer` pending this commit.
- No new tag this wave unless runServer Done **and** AmmoPress solids playable (they are).
  User gate: CE png in jar (yes) + another major table playable (AmmoPress solids yes).
  Still no Release unless that gate is confirmed post-runServer.
- `master` untouched.
