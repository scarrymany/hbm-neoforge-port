# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **96.4% weighted / 98.2% unweighted** (7489 / 7767).
- Published baseline before this wave: **86.8%** (6741 / 7767), 249 short of 90%.
- Vanilla crafting **1416 → 1866 (72.6% → 95.7%)**. Machine still **1682 / 83.7%**. Blocks **790 / 67.6%**.
- Items **2056 → 2354**: extractor now sees already-registered `registerBillet` / `registerParts` / etc. Not dummy items.
- This wave: CE leftover crafts from `MineralRecipes` / `RodRecipes` / Tool / Armor / Consumable / Exclusive. No invented recipes.
- Assembler skip still **7**. Assembler JSON **628**. `SafeMenuScreens.bind` stays. `modId` stays `hbm`.
- `compileJava` 0 (UP-TO-DATE). jar **68,697,039 B**.
- `runServer` **Done (5.037s)** wiped world, port **25566**, **3914 recipes** / 2270 advancements. No parse errors (dropped `gas_mask_filter` DataComponent false-id).
- `runClient` passed MenuScreens (reached `gui.png-atlas`). No `MACHINE_CRUCIBLE` NPE. OpenAL/ALSA fail is env-only.
- Reachability **51.1%** (1203 / 2354).
- Prerelease playtest still `beta-82.1`. Gates for a 90% Release all passed this HEAD; tag not cut (ask if you want it).
- `master` untouched.
