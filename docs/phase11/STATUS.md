# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **104.3% weighted / 103.2% unweighted** (8104 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- This wave: Centrifuge leftover rows live in existing machine (`getOutput` already ticks).
  Registered `block_slag` (CE id, not `slag_block`). Wired CE `gui_centrifuge.png` blit + CE
  `ContainerCentrifuge` slot coords (182×189). AE2 certus cited skip. AmmoPress fluid-slot
  leftover stays cited — CE TE has no tank. SuperComputer dropdown still TODO
  (no ModuleMachineBase). DRX stays cited skip.
- Vanilla **1898 / 97.3%**. Machine **1815 / 90.3%** (Centrifuge **75/78**; AE2 + helper/readRecipe
  census). ElectrolyserMetal 21/23. Blocks **1008 / 86.2%**.
- Reachability **60.0%** (1552 / 2586).
- Items census **2586** (+`block_slag` BlockItem).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99% is a gate. Not content-complete.
- Verified: `compileJava` 0. `runServer` pending this revision.
- No new tag: leftover solids live + CE png in jar, client GUI not opened. `v0.0.1-rc2` stays.
- `master` untouched.
