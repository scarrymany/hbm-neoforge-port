# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **86.8% weighted / 93.3% unweighted**. Not ≥90%.
- Machine recipes **1409 → 1682 (70.1% → 83.7%)**. Vanilla 1416 / 72.6%. Blocks 790 / 67.6%. Items 2056.
- This session: client `RegisterMenuScreensEvent` NPE fix (`CrucibleBlocks.registerAll` + `SafeMenuScreens`);
  remaining Dummyables (heaters / Stirling / StorageDrum / SuperComputer / Autosaw);
  assembler pack/unpack +272 JSON; leftover vanilla crafts +47 (`CraftingManager.java:724-1090`).
- Assembler skip still **7**. Assembler JSON **628** (356 named + 272 pack/unpack).
- `compileJava` 0. jar **67,390,382 B** (beta-82.1).
- `runServer` **Done (6.716s)** wiped world, **3464 recipes** / 2270 advancements. No parse errors.
- `runClient` passed `RegisterMenuScreensEvent` (atlases including `gui.png-atlas`). No `MACHINE_CRUCIBLE` NPE.
- Reachability **49.1%** (1010 / 2056).
- Prerelease: https://github.com/scarrymany/hbm-neoforge-port/releases/tag/beta-82.1
- No 90% GitHub Release. `master` untouched.
