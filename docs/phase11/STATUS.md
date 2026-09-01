# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **102.3% weighted / 102.1% unweighted** (7943 / 7767).
- Published baseline: **100.2%** (7783 / 7767) @ `91e6763a` / `v0.0.1-rc2`.
- Prior HEAD `a65d9775`: **102.1%** (7933 / 7767), holes blocks 173 / machine 327 / vanilla 52.
- This wave: live Dummyable `machine_conveyor_press` (CE dims {2,0,0,0,0,0} + BE + stamp menu +
  `PressRecipes` on `EntityMovingItem` + `IConveyorBelt` + screwdriver) and 1×1 `mass_storage` ×4
  (wood/iron/desh/default, 3-slot stockpile + output toggle, `SafeMenuScreens.bind`).
  Skip zirnox / soyuz / fusion_torus / rails / doors (too large or already casing).
- ChemPlant / SILEX / StorageDrum / SuperComputer leftover I/O still absent — not invented.
- Vanilla **1898 / 97.3%**. Machine **1682 / 83.7%**. Blocks **1001 / 85.6%**.
- Assembler skip **7**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- Weighted ≥99%. Category holes remain (blocks 168, machine 327, vanilla 52). Not content-complete.
- Verified: `compileJava` 0, `runServer` **Done (5.216s)** / 3946 recipes, port 25566.
- No new Release (Dummyable wave, not a closed hole family). `v0.0.1-rc2` stays.
- `master` untouched.
