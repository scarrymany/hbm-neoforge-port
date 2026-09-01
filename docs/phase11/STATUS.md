# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **103.7% weighted / 102.8% unweighted** (8057 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- This wave: Electrolyser metal half in-world (21 slots, nitric acid, pour via
  `CrucibleUtil.pourFullStack`, metal menu + `sgf`/`sgm` toggle). Crystal aluminium +
  bedrock PRIMARY_FIRST/SECOND/CRUMBS loop (3 put sites). `chunk_ore_*` flatten
  (`ModItems.java:1273`). Mixer COLLOID/FULLERENE/LYE/BITUMEN already complete.
  SuperComputer dropdown still TODO (no ModuleMachineBase). DRX stays cited skip.
- Vanilla **1898 / 97.3%**. Machine **1781 / 88.7%** (ElectrolyserMetal 21/23). Blocks **1007 / 86.1%**.
- Reachability **59.4%** (1529 / 2574) — JSON/loot + machine-table outputs.
- Items census **2574** (chunk_ore loop not unrolled in extract_all_ids; runtime +4).
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99% is a gate. Not content-complete.
- Verified: `compileJava` 0. runServer pending this revision.
- No new tag: metal half is playable (pour/slots), but CE GUI pngs are still missing;
  no Release this wave unless asked.
- `master` untouched.
