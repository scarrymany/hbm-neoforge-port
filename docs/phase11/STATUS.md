# Phase 11 status

- Census: `docs/phase11/PARITY_REPORT.md` — **62.8% weighted / 81.1% unweighted**. Not ≥90%.
- Machine recipes **573 → 666 (31.5% → 33.2%)**. Blocks **676 → 680 (57.8% → 58.2%)**. Items **1897 → 1910**.
- This session: leftover assembler (`explosivelenses1`, `fleijacharge`, `fusionplasmaforge`) + chem `uf6`/`meatprocessing`/`hydrogencoke` + AmmoPress/ArcWelder/Soldering/PlasmaForge Java tables + real TE+menu for those four machines + ammo casings / sulfur / niter.
- Census: CE AmmoPress/ArcWelder/Soldering `recipes.add` + PlasmaForge `this.register((PlasmaForgeRecipe)`. Port `RECIPES.add` on those classes only. No global `recipes.add`.
- Leftover CE `@AutoRegister` entities: **none missing**.
- `compileJava` 0. `./gradlew build` / `runServer` numbers pending this revision.
- No GitHub Release. `master` untouched.
