# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **102.8% weighted / 102.4% unweighted** (7983 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- This wave: CE recipe tables — StorageDrum / SILEX depleted+fallout / SuperComputer. ChemPlant
  already 72/72 live (census CE 145 is double-count). See `docs/CE_PARITY_ADDENDUM.md`.
- I/O registered (CE assets): waste siblings (long/short × tiny/depleted/depleted_tiny),
  `bottle_mercury`, `dust_tiny`, `cinnabar`, `powder_ash_*` (6), `drive_*` (11).
- Vanilla **1898 / 97.3%**. Machine **1707 / 85.0%** (was 1682 / 83.7%). Blocks **1007 / 86.1%**.
- Reachability **52.4%** (1348 / 2574) — JSON/loot only; machine tables not in that graph.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99% is a gate. Not content-complete.
- Verified: `compileJava` 0, `runServer` **Done (5.303s)** / 3946 recipes, port 25566. No parse errors.
- No new Release unless ChemPlant table actually runs (it already does — no tag).
- `master` untouched.
