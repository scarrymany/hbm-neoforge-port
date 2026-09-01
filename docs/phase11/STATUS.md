# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **102.4% weighted / 102.2% unweighted** (7955 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- Prior HEAD `0ebf2ff6`: **102.3%** (7943 / 7767), holes blocks 168 / machine 327 / vanilla 52.
- This wave: 1×1 live CE GUI/I/O — `machine_telelinker` (biometry name copy 0→1 / clear slot 2 +
  `SafeMenuScreens.bind`), `soyuz_capsule` (19-slot + `RUSTY` + entity landing writes TE),
  `filing_cabinet` (8-slot, no hopper). Leftover cubes `block_red_phosphorus` / `block_corium` /
  `block_corium_cobble` (not Mats aliases). Skip zirnox / soyuz_launcher / fusion_torus / rails / doors.
- ChemPlant / SILEX / StorageDrum / SuperComputer leftover I/O still absent — not invented.
- Vanilla **1898 / 97.3%**. Machine **1682 / 83.7%**. Blocks **1007 / 86.1%**.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99%. Category holes remain (blocks 162, machine 327, vanilla 52). Not content-complete.
- Verified: `compileJava` 0. `runServer` pending this commit.
- No new Release (Dummyable wave, not a closed hole family). `v0.0.1-rc2` stays.
- `master` untouched.
