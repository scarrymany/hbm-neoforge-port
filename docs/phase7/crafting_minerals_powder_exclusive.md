# Phase 7 Research — crafting-minerals-powder-exclusive

Assignment: CE `com/hbm/crafting/MineralRecipes.java` (514 lines), `PowderRecipes.java` (100 lines), `ExclusiveRecipes.java` (29 lines). All three read in full (MineralRecipes across two reads, offset 412, since the first Read call truncated at line 412 of 515).

## Scope confirmed

| File | Real lines | `register()` structure |
|---|---:|---|
| `com/hbm/crafting/MineralRecipes.java` | 514 (code 27–514; `register()` body 29–420; 8 static helper methods 422–514) | A single flat, non-table-driven sequence of ~290 direct static-method call statements inside one `register()` method, calling 8 private static helper methods declared in the same file (`add1To9Pair` × 3 overloads, `add1To9PairSameMeta`, `addMineralSet`, `add9To1` × 2 overloads, `addBillet` × 4 overloads, `addBilletToIngot`) plus raw `CraftingManager.addRecipeAuto`/`addShapelessAuto`/`addRecipeAutoOreShapeless` calls interspersed throughout. Two (and only two) real `for` loops exist: line 57 (`EnumCokeType`, 3 iterations) and lines 93/98 (`ItemWasteLong.WasteClass`/`ItemWasteShort.WasteClass`, 5 and 8 iterations). Everything else is a literal, individually-typed-out call — this is **not** a material-table loop the way the port's own `ModRecipeProvider` tables are; CE itself never built a `String[][]` table for this file. **This task's own line-by-line tally of every call site (cross-checked against grep call-site counts as a sanity bound, not a substitute) totals ≈610–625 individual recipe registrations** — precise breakdown by region is in the catalog section below. |
| `com/hbm/crafting/PowderRecipes.java` | 100 (register() 22–99) | Flat sequence of 56 call sites (55 `CraftingManager.addShapelessAuto`, 1 `addRecipeAuto`), grouped by the file's own comments into 7 clusters (Explosives, Other, Gunpowder, Blends, Metal powders, Flux, Fertilizer, an `if(GeneralConfig.enableLBSM...)`-gated cluster, and a dye/crayon cluster). One `for(i<16)` loop (line 97, over all 16 `EnumChemDye` colors) multiplies its single call site into 16 runtime recipes. **Total: 71 individual recipe registrations** (55 non-loop + 16 loop-generated). |
| `com/hbm/crafting/ExclusiveRecipes.java` | 29 (register() 16–18, one private helper `registerShielding()` 20–28) | Trivial: `register()` calls exactly one private helper, which contains 6 flat calls (4 `addRecipeAuto`, 2 `addShapelessAuto`), no loops, no tables. **Total: 6 individual recipe registrations.** |

## Already covered by this port

Confirmed by reading `src/main/java/com/hbm/datagen/ModRecipeProvider.java` (695 lines) in full, cross-checked line-by-line against CE's actual `MineralRecipes.java` content (not assumed from the port's own javadoc claims, several of which turned out to be slightly imprecise — see below).

**`ModRecipeProvider.mineralRecipes()` covers exactly these CE `MineralRecipes.java` mechanisms and nothing else:**

| Port table | Rows | Generates (per row) | Maps to CE region |
|---|---:|---|---|
| `BILLET_SETS` | **40** (the class javadoc says "39" — this task counted the literal array rows at ModRecipeProvider.java:317–356 and got 40; a one-off discrepancy in the javadoc, not a functional bug, worth a one-line fix) | 4 recipes: 6 nugget↔1 billet, 3 billet↔2 ingot | CE's 3-arg `addBillet(billet, ingot, nugget, ...ore)` calls, MineralRecipes.java:109–151 (ore-dict vararg dropped) |
| `BILLET_NUGGET_ONLY` | 2 | 2 recipes: 6 nugget↔1 billet only | CE's 2-arg `addBillet(billet, nugget)`, MineralRecipes.java:145–146 (australium_greater/lesser) |
| `MINERAL_SETS` | 6 | 2 recipes: 1 ingot↔9 nugget (block leg dropped) | CE's `addMineralSet(nugget, ingot, block)`, MineralRecipes.java:61–66 (block leg intentionally not reproduced) |
| `ONE_TO_NINE_PAIRS` | 19 | 2 recipes: 1 one↔9 many | CE's `add1To9Pair` calls at MineralRecipes.java:70–74 (5 tiny-powder pairs) + the manual powder pairs at 370–387 (9 more) + 3 more one-off pairs (mercury, silicon, osmiridium — lines 33/68/401, with a documented CE field-name-vs-registry-id remap for mercury, confirmed correct by this task) |

Sum: 40×4 + 2×2 + 6×2 + 19×2 = 160+4+12+38 = **214 recipes**, exactly matching both `ModRecipeProvider`'s own javadoc claim and Phase 5/6's independently-published "214 mineral-conversion-cluster recipes" figure — this task's re-derivation is fully consistent with prior work, no correction needed there.

**Of CE's 43 `addBillet(...)` call sites in `MineralRecipes.java`, 42 are already reflected** in `BILLET_SETS`+`BILLET_NUGGET_ONLY` (40 three/four-arg + 2 two-arg). **The one CE `addBillet` call not covered is `billet_nuclear_waste`** (line 147, `addBillet(billet_nuclear_waste, nuclear_waste, nuclear_waste_tiny)` — CE reuses the plain `nuclear_waste`/`nuclear_waste_tiny` items as the "ingot"/"nugget" legs instead of dedicated `ingot_`/`nugget_` names). This is a genuine gap (see catalog below) — and it is additionally blocked on a missing item (`nuclear_waste_tiny` does not exist in this port at all; `nuclear_waste` itself does).

**The genuine remaining gap is real, large, and falls into distinct sub-categories** (not previously broken out this precisely anywhere in this project's docs):

1. **A whole parallel "direct ingot↔nugget 1:9" recipe family CE provides *in addition to* the billet path**, for 38 materials — `billetSet()`'s helper body only produces nugget↔billet↔ingot conversions, never a direct 1-ingot-for-9-nugget shortcut, but CE's `MineralRecipes.java` separately hand-writes exactly that shortcut at lines 76–91 (14 materials) and lines 326–369 (22 materials) plus two more one-offs (solinium, lines 388–389; euphemium, lines 406–407). **This entire 38-material, 76-recipe cluster is currently unported** — not because the materials are missing (37 of 38 have both items already registered — see item-dependency section) but simply because `ModRecipeProvider`'s helper methods never generate this specific recipe shape.
2. **The entire block↔ingot/nugget compression grid is unported**, matching the parity report's known gap #4 — but **`ModRecipeProvider`'s own javadoc claim that this port's material storage blocks "were not found under any name this class could confirm exists" is not fully accurate and should be corrected**: `com.hbm.blocks.MaterialBlockGenerator.java` (read directly) *does* generate one storage block per material tagged `.setAutogen(..., BLOCK, ...)` in `Mats.java` (57 materials, per that class's own javadoc), it was just registered under a **suffix-first id** (`titanium_block`, `uranium_block`) rather than CE's prefix-first `block_titanium`/`block_uranium` — a documented, deliberate rename (`MaterialBlockGenerator.java`'s own javadoc cites `docs/phase1/modblocks_generative.md`). `ModRecipeProvider` was evidently written without checking this class. **This means a meaningful fraction of CE's block-compression targets are already reachable under a different id and are ready to wire up now** — this task spot-checked this precisely (see catalog below) rather than assuming it either way.
3. **CE's fuel-blend billet-alloy recipes are unported** (9 outputs: thorium_fuel, uranium_fuel, plutonium_fuel, pu_mix, americium_fuel, am_mix, neptunium_fuel, mox_fuel, schrabidium_fuel — lines 153–178). All 9 have a portable, item-only (no ore-dict) primary recipe; CE's 2 additional ore-dict variants per output are correctly out of scope per the established convention.
4. **The Be-alloy billet family** (`billet_po210be`/`billet_pu238be`/`billet_ra226be`, lines 180–188) and **the RTG pellet family** (10 outputs, lines 202–211) are both fully unported despite every ingredient/output item already existing in this port.
5. **The RTG-pellet-depleted "recycling" recipes** (lines 213–226) are unported. In CE these match on an `EnumDepletedRTGMaterial` metadata subtype (1.12 damage-value matching); **this port already flattened `pellet_rtg_depleted` into 6 separate items** (`pellet_rtg_depleted_<material>`, one per enum constant — confirmed in `MachineItems.java`), so this cluster needs **no custom component-matching logic at all** in 1.21 — it collapses to plain shapeless recipes, one per already-distinct item.
6. **Several small, self-contained clusters are unported and independently blocked on missing items** (nitra family, bottle_mercury, ball_fireclay, coke item, block_fallout/`fallout` item, block_scrap/`scrap` item, ZFB billet family, arsenic ingot/nugget, part_generic/glass_polarized) — see the dependency table below for the exact missing id per cluster.
7. **Egg-balefire family (4 recipes) is fully unported despite every item already existing** — a pure oversight-class gap, cheapest to close.

**`PowderRecipes.java` and `ExclusiveRecipes.java` are confirmed untouched by `ModRecipeProvider`** — grepped for `ballistite`, `hazmat_cloth`, `chemical_dye`, `fluid_duct_solid`, `powder_flux`, `crayon`, and every other distinctive identifier from both files; zero hits anywhere in `ModRecipeProvider.java`. Both are cataloged fully below as 100% remaining gap (with per-entry item-readiness noted).

## Full recipe/entry catalog

### `MineralRecipes.java` — representative sample + generating pattern (≈620 total entries; too large to transcribe in full per the task's own >150-entry threshold)

CE line-region breakdown (this task's own count, methodology: manual read of every line, cross-checked against grep call-site totals — `add1To9Pair(` ×50 raw hits, `addBillet(` ×49, `CraftingManager.addRecipeAuto(` ×181, `addShapelessAuto(` ×19, `addRecipeAutoOreShapeless(` ×55 — consistent order-of-magnitude, not independently exact since several hits are inside helper-method *definitions* rather than call sites, and 2 regions are loop-multiplied):

| CE region | Lines | Recipes | Status |
|---|---|---:|---|
| Generic dust/mercury pairs | 31–33 | 6 | 2/3 covered (coal, mercury via id-remap); `dust`/`dust_tiny` blocked — no such items exist |
| Block↔ingot direct pairs (19 materials) | 35–54 | 38 | **Unported** — mixed item-readiness, see below |
| block_slag↔ingot_raw (meta) | 55 | 2 | Unported — component/meta-aware, needs check |
| Coke block↔item (×3, loop) | 57–59 | 6 | Unported — `coke` item confirmed absent |
| `addMineralSet` (6 covered + vitrified) | 61–66 | 24 (4 covered × 6) | Nugget↔ingot leg covered; block leg + vitrified unported |
| Silicon ingot/nugget | 68 | 2 | **Covered** (`ONE_TO_NINE_PAIRS`) |
| Tiny-powder pairs (×5) | 70–74 | 10 | **Covered** |
| Direct ingot↔nugget shortcuts (×8) | 76–83 | 16 | **Unported** (7/8 ready, arsenic blocked) |
| Direct ingot↔nugget shortcuts (×5) | 85–89 | 10 | **Unported** — all 5 ready |
| gh336 shortcut | 91 | 2 | **Unported** — ready |
| Waste-long tiny/depleted (loop×5) | 93–96 | 20 | **Unported** — blocked, `_tiny`/`_depleted` items don't exist |
| Waste-short tiny/depleted (loop×8) | 98–101 | 32 | **Unported** — blocked, same reason |
| block_fallout↔fallout + odd 2-shape | 103–104 | 3 | **Unported** — blocked, both items absent |
| pu_mix mineral set + neptunium_fuel pair | 106–107 | 6 | Mineral-set leg covered; pair unported (ready) |
| **`addBillet` family** | 109–151 | 195 | **42/43 covered** (only `billet_nuclear_waste` gap, itself blocked) |
| Fuel-blend billet alloys (×9, +ore-dict alts dropped) | 153–178 | 26 | **Unported** — all 9 primary recipes ready |
| Be-alloy billets (ore-dict ×3 + item ×6) | 180–188 | 9 | **Unported** — item-based 6 ready, ore-dict 3 correctly dropped |
| ZFB billets (×3, nugget+billet tiers) | 190–195 | 6 | **Unported** — blocked, `billet_zfb_*` don't exist |
| billet_uranium alt craft | 198–200 | 3 | 1 ready (item-based), 2 correctly dropped (ore-dict) |
| RTG pellet family (×10) | 202–211 | 10 | **Unported** — all 10 ready |
| RTG-depleted recycling (×5 + 1 ore-dict) | 213–226 | 6 | **Unported** — all 5 ready (now plain per-item recipes, no component matching needed), 1 ore-dict-conditional correctly droppable |
| Block-from-9-ingots compression | 228–277 | 50 | **Unported** — mixed readiness, see below |
| Ingot-from-1-block decompression | 279–324 | 46 | **Unported** — same mixed readiness (mirror of above) |
| Manual ingot↔nugget shortcuts (×22) | 326–369 | 44 | **Unported** — 21/22 ready (all but `lead`... wait `lead` IS ready; all 22 ready — see dependency table) |
| Manual powder pairs (×9) | 370–387 | 18 | **Covered** (`ONE_TO_NINE_PAIRS`) |
| Solinium shortcut | 388–389 | 2 | **Unported** — ready |
| nuclear_waste shortcut | 390–391 | 2 | **Unported** — blocked (`nuclear_waste_tiny` missing) |
| bottle_mercury↔ingot_mercury (8:1) | 392–393 | 2 | **Unported** — blocked, `bottle_mercury` absent |
| egg_balefire↔shard (1:9) | 394–395 | 2 | **Unported** — ready, all items exist |
| Nitra family (×3) | 396–398 | 3 | **Unported** — blocked, whole family absent |
| glass_polarized ← part_generic | 399 | 1 | **Unported** — blocked, `part_generic` absent (block itself exists) |
| Paleogenite/osmiridium pairs | 400–401 | 4 | **Covered** (`ONE_TO_NINE_PAIRS`) |
| egg_balefire_shard cluster (×2) | 403–404 | 2 | **Unported** — ready, all items exist |
| Euphemium shortcut | 406–407 | 2 | **Unported** — ready |
| Ore-dict-only alt alloy recipes (×7) | 409–415 | 7 | Correctly out of scope (pure ore-dict, no item-based equivalent in CE itself) |
| ball_fireclay (×3 alternates) | 417–419 | 3 | **Unported** — blocked, `ball_fireclay` absent |

**Representative sample (30 entries spanning every sub-pattern above), verbatim from CE with this task's readiness verdict:**

| # | CE call (abbreviated) | Pattern | Readiness |
|---|---|---|---|
| 1 | `add1To9Pair(dust, dust_tiny)` L31 | generic 1:9 pair | Blocked — `dust`/`dust_tiny` not registered |
| 2 | `add1To9Pair(block_aluminium, ingot_aluminium)` L35 | block↔ingot 1:9 | Ready — `aluminium_block` exists (MAT_ALUMINIUM has BLOCK autogen) |
| 3 | `add1To9Pair(block_tcalloy, ingot_tcalloy)` L51 | block↔ingot 1:9 | Blocked — MAT_TCALLOY has no BLOCK autogen in this port |
| 4 | `for EnumCokeType: add1To9PairSameMeta(block_coke, coke, i)` L57-59 | meta-loop, 3 iter | Blocked — `coke` item absent (block_coke_X blocks exist) |
| 5 | `addMineralSet(nugget_niobium, ingot_niobium, block_niobium)` L61 | nugget/ingot/block triple | Ingot leg covered; block leg unported (niobium has BLOCK? — MAT_NIOBIUM has no BLOCK autogen, so blocked either way) |
| 6 | `add1To9Pair(ingot_technetium, nugget_technetium)` L76 | direct shortcut | Ready — both items exist, billet path also exists separately |
| 7 | `add1To9Pair(ingot_arsenic, nugget_arsenic)` L83 | direct shortcut | Blocked — neither item registered anywhere (Mats.MAT_ARSENIC exists but its NUGGET-shape autogen isn't wired to any generator) |
| 8 | `for WasteClass(5): add1To9PairSameMeta(nuclear_waste_long, _tiny, i)` L93-96 | meta-loop, 5 iter ×2 | Blocked — only base `nuclear_waste_long_<class>` items exist; `_tiny`/`_depleted`/`_depleted_tiny` do not (confirmed by this port's own `SpecialItems.java:270-274` code comment) |
| 9 | `addBillet(billet_uranium, ingot_uranium, nugget_uranium, U.all(NUGGET))` L112 | 3-arg + ore vararg | Covered by `BILLET_SETS` (ore vararg correctly dropped) |
| 10 | `addBillet(billet_nuclear_waste, nuclear_waste, nuclear_waste_tiny)` L147 | 3-arg, non-standard leg names | Blocked — `nuclear_waste_tiny` absent; the ONE addBillet call not in `BILLET_SETS` |
| 11 | `addShapelessAuto(billet_thorium_fuel×6, billet_th232×5, billet_u233×1)` L153 | shapeless blend | Ready — both billets exist |
| 12 | `addRecipeAutoOreShapeless(billet_po210be×2, billet_polonium, billet_beryllium)` L183 | shapeless blend | Ready — `billet_po210be` exists |
| 13 | `addRecipeAutoOreShapeless(billet_zfb_bismuth, ZR.nugget()×3, U.nugget(), PU241.nugget(), BI.nugget())` L190 | shapeless, Mats-accessor ingredients | Blocked — `billet_zfb_bismuth` output not registered |
| 14 | `addRecipeAutoOreShapeless(pellet_rtg, billet_pu238×3, IRON.plate())` L202 | shapeless | Ready — `pellet_rtg` + `plate_iron` (via IRON accessor) exist |
| 15 | `addShapelessAuto(billet_bismuth×3, pellet_rtg_depleted[BISMUTH])` L214 | meta-subtype input | Ready as plain item — port has `pellet_rtg_depleted_bismuth` as a distinct item |
| 16 | `if(OreDictionary.doesOreNameExist("ingotNickel"))...` L219-226 | ore-dict conditional | Correctly out of scope — no ore-dict system in NeoForge |
| 17 | `addRecipeAuto(block_copper×1, "###×3", ingot_copper)` L228 | 9-ingot→1-block | Ready — `copper_block` exists |
| 18 | `addRecipeAuto(block_scrap×1, "###×3", dust)` L242 | 9-ingot→1-block | Blocked — `scrap`/`dust` absent (this port explicitly has no generic "scrap" item, per `MachineShredderBlockEntity.java`'s own comment) |
| 19 | `addRecipeAuto(block_schrabidium_cluster, "#S#/SXS/#S#", ingot_schrabidium,'S' ingot_starmetal,'X' ingot_schrabidate)` L245 | special 3×3 shaped, mixed materials | Ready — all 3 ingots exist, though `block_schrabidium_cluster`'s port equivalent not independently confirmed |
| 20 | `addRecipeAuto(ingot_copper×9, "#", block_copper)` L279 | 1-block→9-ingot | Ready — mirror of #17 |
| 21 | `addRecipeAuto(ingot_plutonium×1, "###×3", nugget_plutonium)` L326 | manual ingot↔nugget | Ready — this is the same "shortcut" family as #6, just for a billet-covered material |
| 22 | `addRecipeAuto(nugget_lead×9,'#', ingot_lead)` L349 | manual ingot↔nugget, no billet family exists for lead at all | Ready — both items exist |
| 23 | `addRecipeAuto(ingot_solinium×1, "###×3", nugget_solinium)` L388 | manual shortcut | Ready |
| 24 | `addRecipeAuto(bottle_mercury, "###/#B#/###", ingot_mercury,'B' GLASS_BOTTLE)` L392 | 8:1 non-standard ratio | Blocked — `bottle_mercury` absent |
| 25 | `addRecipeAuto(nitra×1, "##/##", nitra_small)` L396 | 4:1 non-standard ratio | Blocked — whole nitra family absent (confirmed by this port's own `IWeaponAbility.java:39` comment) |
| 26 | `addRecipeAuto(glass_polarized×4, "##/##", DictFrame.fromOne(part_generic, GLASS_POLARIZED))` L399 | metadata-subtype ingredient | Blocked — `part_generic` item absent (the `glass_polarized` block output does exist) |
| 27 | `add9To1(cell_balefire, egg_balefire_shard)` L404 | 9:1 only, no reverse | Ready — both items exist |
| 28 | `addRecipeAuto(ingot_euphemium×1, "###×3", nugget_euphemium)` L406 | manual shortcut | Ready |
| 29 | `addRecipeAutoOreShapeless(ingot_pu_mix, "nuggetPlutonium239"×6, "nuggetPluonium240"×3)` L412 | pure ore-dict, no item form | Correctly out of scope |
| 30 | `addRecipeAutoOreShapeless(ball_fireclay×4, CLAY_BALL×3, stone_resource[LIMESTONE], KEY_SAND)` L419 | mixed vanilla+subtype ingredients | Blocked — `ball_fireclay` absent |

**Generating pattern for an implement-wave Java loop**, matching this port's own `ModRecipeProvider` `String[][]` table convention:

```java
// New table, same shape as ONE_TO_NINE_PAIRS but for the 37 currently-unported direct shortcuts
// (36 confirmed-ready + americium_fuel/lead/solinium/euphemium already spot-checked ready; arsenic excluded — blocked):
private static final String[][] DIRECT_INGOT_NUGGET_SHORTCUTS = {
    {"technetium", "ingot_technetium", "nugget_technetium"},
    {"co60", "ingot_co60", "nugget_co60"},
    // ... 35 more rows, one per material in the 38-material list above minus arsenic
};
// reuse the EXISTING onePair(output, path, one, many) helper verbatim - it already does exactly
// this shape (1 <-> 9 via a shapeless-9 + a shaped-3x3), just needs a new table and a new
// mineralRecipes() loop calling it with a distinct id namespace so it doesn't collide with the
// billetSet()-generated recipes for the same 36 materials that already share the ingot/nugget items.
```

For the block-compression grid, the loop should be driven directly off `Mats.orderedList` (not a hand-typed table): `for (NTMMaterial mat : Mats.orderedList) if (mat.getAutogen().contains(MaterialShapes.BLOCK)) { /* emit block<->ingot pair using MaterialShapes.BLOCK.buildRegistryName(mat) and MaterialShapes.INGOT.buildRegistryName(mat) */ }` — this automatically stays correct as `Mats.java` gains more `BLOCK`-tagged materials over time, unlike a hand-typed table.

### `PowderRecipes.java` — full catalog (71 entries, under the 150-entry threshold)

| Cluster | CE entries (output → inputs) | Readiness |
|---|---|---|
| Explosives (L25-33, 9 recipes) | `ballistite`←GUNPOWDER+KNO.dust()+SUGAR; `ball_dynamite`←KNO.dust()+SUGAR+SAND+chemset(tool); `ball_tnt`←AROMATICS(fluid)+KNO.dust()+chemset; `ingot_c4`←UNSATURATEDS(fluid)+KNO.dust()+chemset; `powder_semtex_mix`×2 (solid_fuel+cordite/ballistite+KNO.dust()); `CLAY_BALL`×2 (from sand+dust+water, and from vanilla clay block); `powder_cement`←LIMESTONE.dust()+3×CLAY_BALL | **Mostly blocked**: `ballistite`, `ball_dynamite`, `ball_tnt`, `solid_fuel`, `cordite` all confirmed absent (zero hits repo-wide); `dust` generic absent. `ingot_c4` and `powder_semtex_mix`/`powder_cement` outputs DO exist but their recipes are individually blocked on the missing co-ingredients above, plus all 3 "chemset"/fluid-container recipes need a custom Ingredient (see implementation-shape section) |
| Other (L36-37, 2) | `ingot_steel_dusted`←STEEL.ingot()+COAL.dust(); `powder_bakelite`←2×AROMATICS+PETROLEUM(fluids)+chemset | Partial — `ingot_steel_dusted` exists as an indexed series (`INGOT_STEEL_DUSTED`, `registerIngotSeries`, not a single flat id — needs the exact sub-id CE's plain field maps to, flagged as open question); `powder_bakelite` item exists but recipe needs the fluid-container Ingredient |
| Gunpowder (L40-43, 4 calls = 2 distinct recipes ×2 near-dupes) | vanilla GUNPOWDER×3 ← S.dust()+KNO.dust()+COAL.gem() \| S.dust()+KNO.dust()+COAL(meta1=charcoal) | **Ready** — sulfur/saltpeter dust tags exist (`MaterialShapes.DUST.commonTag(Mats.MAT_SULFUR/MAT_KNO)`), `Items.COAL`/`Items.CHARCOAL` are vanilla |
| Blends (L46-55, 8) | `powder_power`←glowstone+DIAMOND.dust()+MAGTUNG.dust(); `powder_nitan_mix`×2 (6-material blends); `powder_spark_mix`←DESH.dust()+EUPH.dust()+powder_power; `powder_meteorite`←4 dusts; `powder_thermite`←3×IRON.dust()+AL.dust(); `powder_desh_mix`×2 (tiny-dust and dust blends, 9 ingredients each); `powder_desh_ready`←powder_desh_mix+2×ingot_mercury+COAL.dust() | **All output items confirmed registered** (`powder_power`, `powder_nitan_mix`, `powder_spark_mix`, `powder_thermite`, `powder_desh_mix`, `powder_desh_ready`, `powder_meteorite` all exist in `BilletPowderItems.java`) — ready modulo confirming `EUPH`/`DESH` dust-tag materials individually (not independently re-verified for every one of the ~9 dust ingredients in this pass) |
| Metal powders / scraps (L58-62, 4) | `ItemScraps.create(MaterialStack(MAT_MINGRADE, INGOT.q(2)))`←CU.dust()+REDSTONE.dust(); similar for MAT_MAGTUNG, MAT_TCALLOY, MAT_STEEL×2 | **Class exists** (`com.hbm.items.machine.ItemScraps`, confirmed) but its exact factory-method signature (does it have a `create(MaterialStack)`-equivalent static method?) was **not verified in this pass** — flagged as open question |
| Flux (L64-70, 7) | `powder_flux` at 5 different yields ← COAL(meta1)/COAL.dust()/F.dust()/PB.dust()+S.dust()/LIMESTONE.dust()/CA.dust()/BORAX.dust(), all + KEY_SAND (sand tag) | **Ready** — `powder_flux` item exists; all Mats materials referenced (F=fluorine?, PB=lead, S=sulfur, CA=calcium, BORAX) exist in `Mats.java` with DUST autogen except fluorine (`MAT_FLUORITE` exists but is fluorite, not elemental fluorine — flag as open question whether CE's `F` here is the same material) |
| Fertilizer (L72-73, 2) | `powder_fertilizer`←CA.dust()+P_RED.dust()+KNO.dust()+S.dust(); alt with ANY_ASH.any() | **Ready** for the first variant (all Mats-backed); second variant uses an ore-dict "any" wildcard tag, correctly droppable |
| LBSM-gated (L75-82, 6, config-gated `if(GeneralConfig.enableLBSM && ...enableLBSMSimpleCrafting)`) | `powder_red_copper`, `powder_dura_steel`×4 alt recipes, `ingot_firebrick` | CE's own default has this config **off** — matches the established precedent `ModRecipeProvider` already set for `dieselsuit`/starmetal-tool LBSM-gated recipes (skip entirely, faithful to CE's real default, not a scope cut). `ingot_firebrick` item does exist if this ever gets revisited. |
| Dye blends (L85-95, 10) | 10 shapeless 2-color→1-color-x2 blends across `EnumChemDye`'s 16 colors | **Ready, but needs per-color id confirmation** — this port already flattens `chemical_dye` into 16 separate items (`ItemChemicalDye.java`'s own class javadoc confirms this explicitly: "Each (base item, color) pair is now its own registered item"); the exact registered ids (presumably `chemical_dye_<color>`) were not individually grepped in this pass |
| Crayon (L97, 1 call ×16 loop) | `crayon`(16 colors)←chemical_dye[color]+ANY_TAR.any()+PAPER | **Blocked** — `crayon`/`ItemCrayon` confirmed not ported anywhere (explicit comments in both `FoodItems.java:236` and `MachineItems.java:230`) |

### `ExclusiveRecipes.java` — full catalog (6 entries)

| # | CE recipe | Readiness |
|---|---|---|
| 1 | `hazmat` block ×8 ← 8× `hazmat_cloth` (hollow 3×3) | **Ready** — both `hazmat` (block, `GenericBlocks.java:489`) and `hazmat_cloth` (repair-tag material, `MaterialRegistry.java:56`) confirmed registered |
| 2 | `hazmat_cloth` ×1 ← 1× `hazmat` block | **Ready** — same items, reverse direction |
| 3 | `block_niter_reinforced` ← `TCALLOY.ingot()` + `concrete` (block) + `KNO.block()` (3×3 shaped, T-C-T/C-N-C/T-C-T) | **Ready if `concrete` block exists** — `block_niter_reinforced` confirmed (`GenericBlocks.java:485`), `ingot_tcalloy` confirmed, `KNO.block()`→ `niter_block`-equivalent needs the same `Mats.MAT_KNO` naming-mismatch caveat flagged in the block-compression section (registry name is `saltpeter`, not `niter` — `MaterialShapes.BLOCK.buildRegistryName(Mats.MAT_KNO)` resolves it correctly regardless of what CE called it); plain "`concrete`" block presence not independently grepped in this pass (the port's `GenericBlocks.java` is known to have many `concrete_*` variants per the Phase 6 parity report, but the base "`concrete`" id itself is an open question) |
| 4 | `red_wire_sealed` ← `red_wire_coated` + `brick_compound` (shapeless) | **Blocked** — neither `red_wire_sealed` nor `red_wire_coated` found registered anywhere; `brick_compound` block does exist |
| 5 | `fluid_duct_solid` ×8 ← `ingot_steel`+`plate_aluminium`+`ducttape` (hollow 3×3, S-A-S/A-D-A/S-A-S) | **Blocked** — `fluid_duct_solid` and `ducttape` both confirmed absent; `ingot_steel`/`plate_aluminium` (via `AL.plate()`) exist |
| 6 | `fluid_duct_solid_sealed` ← `fluid_duct_solid` + `brick_compound` (shapeless) | **Blocked** — transitively blocked on #5's output |

## Item/registry dependency check

Every distinct ingredient/output family referenced across all three files, checked via direct `grep` against `src/main/java/com/hbm` (not assumed from CE names) — a full ledger, not a sample, since the assignment specifically calls this out as "often the actual blocker."

**Confirmed registered (ready as ingredients/outputs today):**
`ingot_c4`, `powder_semtex_mix`, `powder_cement` (repurposed as food item), `ingot_steel_dusted` (indexed series), `powder_bakelite`, `powder_flux`, `powder_fertilizer`, `ingot_firebrick`, `powder_power`, `powder_nitan_mix`, `powder_spark_mix`, `powder_thermite`, `powder_desh_mix`, `powder_desh_ready`, `chemical_dye` (16 flattened color items), `ItemScraps` class (factory method unverified), `hazmat`/`hazmat_cloth`, `block_niter_reinforced`, `brick_compound`, `ingot_tcalloy`, essentially the entire `IngotNuggetItems.java`/`BilletPowderItems.java` corpus referenced by `MineralRecipes.java`'s direct-shortcut and billet clusters (`ingot_lead`/`nugget_lead`, `ingot_americium_fuel`/`nugget_americium_fuel`, `ingot_solinium`/`nugget_solinium`, `ingot_euphemium`/`nugget_euphemium`, `nuclear_waste` (but not `_tiny`), `billet_po210be`/`billet_ra226be`/`billet_pu238be`, `egg_balefire`/`egg_balefire_shard`/`cell_balefire`/`powder_balefire`, `pellet_rtg` and its 9 siblings, `pellet_rtg_depleted_<material>` ×6), most base-metal `MaterialItemGenerator`/`MaterialBlockGenerator` shapes (`copper_block`, `titanium_block`, `steel_block`, `lead_block`, `cobalt_block`, etc. — 57 materials total per `MaterialBlockGenerator`'s own javadoc, exact per-material overlap with CE's compression targets not 100% cross-matched in this pass, see Open Questions).

**Confirmed blocked (item/family genuinely does not exist yet — this is the actual blocker, not the recipe logic):**
- `dust`/`dust_tiny` (generic, non-material-specific dust item CE uses in a handful of recipes)
- `ingot_arsenic`/`nugget_arsenic` (the one BILLET_SETS-adjacent material with no port item at all; `Mats.MAT_ARSENIC` exists but its `NUGGET` autogen shape isn't wired to any item generator)
- `nuclear_waste_tiny` (blocks both the `billet_nuclear_waste` triple and the direct 1:9 shortcut — `nuclear_waste` itself exists)
- `nuclear_waste_long_tiny`/`_depleted`/`_depleted_tiny` and the `_short` equivalents (6 sibling fields per class CE has; port has only the 5+8 base fields — confirmed by the port's own `SpecialItems.java:270-274` code comment naming this exact gap as "a distinct open question")
- `coke` (plain item; `block_coke_<coal/lignite/petroleum>` blocks exist)
- `fallout` item and `block_fallout` block (both confirmed absent — multiple class javadocs in `blocks/generic/` explicitly distinguish this port's `BlockFallout`/`FalloutBlocks` from CE's real `block_fallout`, which is not ported)
- `scrap`/`block_scrap` (confirmed absent — explicit comment in `MachineShredderBlockEntity.java:41`: "this port has no equivalent generic 'scrap' item")
- `trinitite`, `nuclear_waste_vitrified`/`_tiny` (both zero hits repo-wide; the latter is also explicitly named as skipped in `ONE_TO_NINE_PAIRS`'s own javadoc)
- `bottle_mercury`
- `nitra`/`nitra_small`/`ammo_container` (whole family; explicitly confirmed absent by `IWeaponAbility.java:39`'s own comment)
- `part_generic` (blocks the `glass_polarized` crafting recipe even though the `glass_polarized` block itself exists)
- `ball_fireclay`
- `billet_zfb_bismuth`/`billet_zfb_pu241`/`billet_zfb_am_mix` (whole ZFB family)
- `ballistite`, `ball_dynamite`, `ball_tnt`, `solid_fuel`, `cordite` (whole conventional-explosives sub-family — `ball_tnt`'s absence is independently confirmed by `CrashedBombBlock.java`'s own class javadoc, which names it as "confirmed absent" and lists 3 TODOs against it)
- `crayon`/`ItemCrayon` (confirmed absent, 2 separate code comments)
- `red_wire_sealed`, `red_wire_coated`, `fluid_duct_solid`, `fluid_duct_solid_sealed`, `ducttape` (all 5 ExclusiveRecipes-specific ids)

## Recommended 1.21.1 implementation shape

**The overwhelming majority of the genuine gap is plain shaped/shapeless JSON recipes**, matching `ModRecipeProvider`'s existing `RecipeProvider`/`ShapedRecipeBuilder`/`ShapelessRecipeBuilder` convention — no new `RecipeType`/`RecipeSerializer` needed for:
- The 38-material direct ingot↔nugget shortcut family (extend `mineralRecipes()` with a new table + a call to the existing `onePair()` helper, verbatim)
- The block-compression grid, once driven off `Mats.orderedList`'s `BLOCK` autogen set rather than a hand-typed table (new helper, same `ShapedRecipeBuilder` 3×3/1×1 pattern `add9To1`/`add1To9` already establish)
- The 9 fuel-blend billet alloys, the Be-alloy billet family, the RTG pellet family, and the RTG-depleted recycling recipes (all plain shapeless, all ingredients already distinct items — no component/NBT matching needed, contrary to what the 1.12 metadata source might suggest, precisely *because* this port already flattened the metadata subtypes into separate items)
- Essentially all of `PowderRecipes.java`'s Blends/Flux/Fertilizer/Dye clusters and `ExclusiveRecipes.java` in full (once the blocked ingredient items exist)

**A small number of entries genuinely need something other than a plain vanilla recipe:**
- **Fluid-as-crafting-ingredient recipes** (`ball_tnt`, `ingot_c4`, `powder_bakelite` — CE's `Fluids.AROMATICS.getDict(1000)` pattern, "a full 1000mB container of fluid X consumed as a crafting-table ingredient"): NeoForge/vanilla shaped/shapeless recipes have no native "any filled fluid container" ingredient matcher. This needs either (a) a custom `Ingredient` type built on this port's existing `FluidContainerRegistry` (confirmed present, not read in depth this pass), or (b) redesigning these as filled-cell-item ingredients if this port represents "a full drum of X" as a distinct DataComponents-tagged item rather than a generic container + NBT fluid tag. Flagged as the one place in this whole assignment that plausibly needs custom `Ingredient`/predicate logic, not just missing items.
- **`ComplexOreIngredient(KEY_TOOL_CHEMISTRYSET)`** (3 explosives recipes): CE's "consume this recipe only if a chemistry-set tool is present but don't consume the tool" pattern. 1.21's closest native analogue is a recipe with a `Ingredient` that never gets consumed — needs a small custom `CraftingRecipe`/`Ingredient` wrapper (same complexity class the port's own javadoc already flags for the 7 `com.hbm.crafting.handlers.*` classes) or can be dropped if this port decides not to reproduce the tool-preservation mechanic and just requires the chemistry set as a consumed ingredient instead (a legitimate, cheaper simplification worth flagging to whoever implements this).
- **`DictFrame.fromOne(item, ENUM_CONSTANT)`** (glass_polarized ← part_generic[GLASS_POLARIZED]; ball_fireclay ← stone_resource[LIMESTONE]): in CE this selects one metadata subtype of a multi-variant item. This port's established convention already flattens such subtypes into separate item ids elsewhere (confirmed for `EnumChemDye`, `pellet_rtg_depleted`, `WasteClass`) — so once `part_generic`/`stone_resource` get ported under this port's own flattening convention, these become plain single-item ingredients, no custom matching needed.
- **`block_slag`↔`ingot_raw`(meta `MAT_SLAG.id`)** (line 55): CE encodes the *specific alloy* as a damage value on a shared "raw" ingot item. Needs checking whether this port's `MaterialItemGenerator`/`IngotNuggetItems` already flattened `ingot_raw`+slag-meta into a distinct `slag_ingot`-style id (not checked in this pass) before deciding whether this is a plain-JSON case or needs a fallback.

## Open questions / risks

1. **`NTMMaterial.getRegistryName()` returns `names[0].toLowerCase()`, not CE's registry-string** — confirmed by reading `NTMMaterial.java:57-58`. This means several `Mats.java` constants resolve to ids that don't match CE's naming intuition at all: `MAT_KNO` (CE: `niter`) → this port's real id is **`saltpeter_block`**/`saltpeter_ingot`, not `niter_*`; `MAT_DESH` (CE: `desh`) → **`workersalloy_*`**, not `desh_*`; `MAT_PHOSPHORUS` (CE: white phosphorus vs red phosphorus, two different CE items) → **`redphosphorus_*`** for both, ambiguous. **Whoever implements the block-compression grid (or any `MaterialShapes.X.buildRegistryName(mat)` call against these three materials specifically) must resolve the real id programmatically, never by guessing from CE's field name** — this task flags it precisely but did not exhaustively re-derive every one of the 57 `MaterialBlockGenerator` materials' first-name-array entry against every one of CE's ~65 block-compression targets; that full cross-match (cheap, mechanical, ~30 min of work reading `Mats.java`'s `n(...)` calls against the block lists cataloged above) should be the first step of implementation, not assumed from this report's spot-checks.
2. **Exact overlap between the 57 `MaterialBlockGenerator` materials and CE's ~65 distinct block-compression targets across `MineralRecipes.java`'s two block regions (lines 35-54 and 228-324) was spot-checked (≈20 materials individually verified in this pass, cataloged above) but not fully enumerated for all ~65** — this task's read of `Mats.java` (all ~90 `MAT_*` constants, in full) supports doing this exhaustively without any further CE reading, purely by cross-referencing the two lists already in this report.
3. **`ItemScraps`'s exact factory-method signature was not verified** — confirms the class exists (`items/machine/ItemScraps.java`) but not whether it exposes a `create(Mats.MaterialStack)`-equivalent static method the way CE's does; a 1-file read would resolve this.
4. **`FluidContainerRegistry.java` (confirmed present, referenced by `FoodItems.java` etc.) was not read** — this is the class that would need to back any fluid-as-crafting-ingredient solution (explosives cluster); its actual API shape is unknown to this report.
5. **The 22-material "manual ingot↔nugget shortcut" list (CE lines 326-369) includes `lead`, which has no billet family in CE's `MineralRecipes.java` at all** — worth double-checking against CE's `CraftingManager.java`/other crafting classes (out of this task's assigned files) in case `billet_lead` is registered elsewhere in CE and this port, which would change whether `lead` belongs in a `DIRECT_INGOT_NUGGET_SHORTCUTS`-style table or a `BILLET_SETS`-style one.
6. **`concrete` (plain block, needed by `ExclusiveRecipes.java`'s `block_niter_reinforced` recipe) was not independently grepped for exact existence** — the Phase 6 parity report confirms this port's `GenericBlocks.java` has ≈234 blocks including many `concrete_*` variants, making a bare `concrete` base block plausible but not confirmed in this pass.
7. **`ingot_steel_dusted`'s exact registered id was not resolved** — the port registers it as `registerIngotSeries("ingot_steel_dusted", STEEL_DUSTED_VARIANT_COUNT)` (an indexed series, per the parity report's own "ingot_steel_dusted_0..9" mention), while CE's `PowderRecipes.java:36` references a single flat `ModItems.ingot_steel_dusted` with no index — whoever implements this recipe needs to decide whether it targets index 0, all 10 indices, or whether CE's single field was itself metadata-indexed in a way this port's series already correctly reproduces (most likely) — a one-file read of `IngotNuggetItems.java`'s surrounding context would settle it.
8. **This task's own MineralRecipes.java total (≈610-625, presented as ≈620) is a hand-tally cross-checked against grep call-site sums, not a machine-verified exact count** — flagged with the same confidence-tier discipline this project's own `PARITY_REPORT.md`/`recipe_graph_audit.md` already establish; treat as accurate to within a handful of entries, not a guaranteed-exact integer.
9. **This report did not re-verify CE's own `CraftingManager.java` (1,602 lines) or the other 6 `com.hbm.crafting.*` sub-registrar classes** (`ToolRecipes`, `ArmorRecipes`, `RodRecipes`, `WeaponRecipes`, `ConsumableRecipes`, and `CraftingManager.addCrafting()`'s own ~1,200-line inline dispatcher) — those are explicitly out of this task's assigned scope (3 named files only) and are covered by sibling Phase 7 research tasks per the orchestrating prompt.

## Files read (for the record)

- `docs/phase6/PARITY_REPORT.md` (416 lines, full)
- `docs/phase6/recipe_graph_audit.md` (377 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/crafting/MineralRecipes.java` (514 lines, full, 2 reads)
- `upstream/hbm-ce/src/main/java/com/hbm/crafting/PowderRecipes.java` (100 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/crafting/ExclusiveRecipes.java` (29 lines, full)
- `src/main/java/com/hbm/datagen/ModRecipeProvider.java` (695 lines, full)
- `src/main/java/com/hbm/inventory/material/Mats.java` (327 lines, full)
- `src/main/java/com/hbm/inventory/material/MaterialShapes.java` (142 lines, full)
- `src/main/java/com/hbm/items/special/SpecialItems.java` (partial, lines 260-304, the WasteClass-driven registration region)
- `src/main/java/com/hbm/items/machine/ItemChemicalDye.java` (partial, lines 1-50)
- `src/main/java/com/hbm/blocks/MaterialBlockGenerator.java` (partial, lines 1-60, class javadoc + naming section)
- `src/main/java/com/hbm/items/MaterialItemGenerator.java` (full, ~55 lines)
- `src/main/java/com/hbm/inventory/material/NTMMaterial.java` (partial, `getRegistryName()` region only)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/ItemWasteLong.java` / `ItemWasteShort.java` (partial, `WasteClass` enum bodies)
- `upstream/hbm-ce/src/main/java/com/hbm/items/ItemEnums.java` (partial, `EnumCokeType`/`EnumTarType` region)
- Dozens of targeted `grep`/`Grep` calls across `src/main/java/com/hbm` establishing item-existence for every id named in the dependency-check section above (individual file citations given inline throughout the report rather than listed separately here).
