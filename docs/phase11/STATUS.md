# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **96.4% weighted / 98.2% unweighted** (7490 / 7767).
- Published baseline before this wave: **86.8%** (6741 / 7767), 249 short of 90%.
- Vanilla crafting **1416 → 1867 (72.6% → 95.7%)**. Machine still **1682 / 83.7%**. Blocks **790 / 67.6%**.
- Items **2056 → 2354**: extractor now sees already-registered `registerBillet` / `registerParts` / etc. Not dummy items.
- This wave: CE leftover crafts from `MineralRecipes` / `RodRecipes` / Tool / Armor / Consumable / Exclusive. No invented recipes.
- Assembler skip still **7**. Assembler JSON **628**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- `compileJava` / `runServer` / `runClient` — re-verify this wave (recipe JSON only + census scripts).
- Reachability **51.1%** (1203 / 2354).
- Prerelease playtest still `beta-82.1`. No 90% GitHub Release until compile + runServer Done + runClient past MenuScreens.
- `master` untouched.
