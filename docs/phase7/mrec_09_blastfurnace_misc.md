
# Research report — mrec-09-blastfurnace-misc

Assignment: CE's `com.hbm.inventory.recipes.{BlastFurnaceRecipes, RotaryFurnaceRecipes, ExposureChamberRecipes, NuclearTransmutationRecipes}` — four small, distinct per-machine recipe-data classes. Sibling crafting-table classes under `com.hbm.crafting/` are explicitly out of scope.

## Scope confirmed

All four files were read in full (not skimmed). Exact line counts and structure:

| CE file | Lines | Structure |
|---|---:|---|
| `upstream/hbm-ce/.../recipes/BlastFurnaceRecipes.java` | 397 | Flat imperative list of `addFuel(...)` calls (21, lines 37-57) + flat list of `addRecipe(in1, in2, out)` calls (15 unconditional, lines 273-288, + 1 config-gated, line 292) inside one `registerDefaults()`. No loops, no tables. Class-level `@Deprecated` (line 29) — see "Open questions" below, this is CE's **legacy** 2-item-input blast/di-furnace, superseded-but-still-live alongside a newer, unrelated `BlastFurnaceRecipesNT` class. Also defines a JSON load/save `SerializableRecipe` shape (CE's own hand-rolled recipe-file loader, not vanilla `Recipe<?>`) and fuel-lookup/matching logic (`getItemPower`, `getOutput`, `getRequiredCounts`) consumed by the block-entity, not just data. |
| `upstream/hbm-ce/.../recipes/RotaryFurnaceRecipes.java` | 177 | Flat list of `recipes.add(new RotaryFurnaceRecipe(...))` calls (12, lines 32-46) inside one `registerDefaults()`. Output type is `Mats.MaterialStack` (material+shape+quantity), not a concrete `ItemStack` — see below, this matters a lot for portability. Also a `getRecipe(ItemStack...)` multiset-matcher and a JSON load/save shape. |
| `upstream/hbm-ce/.../recipes/ExposureChamberRecipes.java` | 120 | Flat list of `recipes.add(new ExposureChamberRecipe(...))` calls (4, lines 55-65, one of the 4 branches on a config flag) inside one `registerDefaults()`. Dual-key match (a "particle" AStack + an "ingredient" AStack) → single `ItemStack` output. |
| `upstream/hbm-ce/.../recipes/NuclearTransmutationRecipes.java` | 74 | Flat list of `addRecipe(input, output, energy)` calls (3, lines 25-27) inside one `registerRecipes()`. Simplest of the four: single AStack input → single ItemStack output → a `long` energy cost. No JSON loader, no `SerializableRecipe` inheritance — a plain static-map class. |

None of the four is table/loop-generated in CE — every entry is a distinct literal call site. Total across all four: **21 fuels + 15 unconditional blast-furnace recipes + 1 conditional + 12 rotary-furnace + 4 exposure-chamber (1 conditional-branch) + 3 nuclear-transmutation = 56 entries**, comfortably a "small file" catalog by this task's own threshold (<150 entries) — every entry is transcribed below, nothing is sampled.

**A file this task's prompt did not name but that changes how you should read `BlastFurnaceRecipes.java`**: CE has a **second, unrelated, newer class** for a **different, newer machine** that happens to share "blast furnace" in its name — `upstream/hbm-ce/.../recipes/BlastFurnaceRecipesNT.java` (105 lines, `extends GenericRecipes<BlastFurnaceRecipe>`, backs block `machine_blast_furnace` / `TileEntityMachineBlastFurnace`). This task's assigned `BlastFurnaceRecipes.java` (no `NT` suffix, `@Deprecated`) backs a **different, older** pair of blocks, `machine_difurnace_on`/`machine_difurnace_off` (`TileEntityDiFurnace`) and `machine_difurnace_rtg_on`/`machine_difurnace_rtg_off` (`TileEntityDiFurnaceRTG`) — confirmed by grepping every CE call site of `BlastFurnaceRecipes.getItemPower`/`getRequiredCounts` (only `TileEntityDiFurnace.java`/`TileEntityDiFurnaceRTG.java` call it) vs. `BlastFurnaceRecipesNT.INSTANCE` (only `TileEntityMachineBlastFurnace.java`/`BlastFurnaceHandler.java`/`BlastFurnaceNT.java` call it). **`BlastFurnaceRecipesNT.java` is not part of this assignment and was not catalogued here** — flagged prominently under "Open questions" since a reader skimming only the class name could conflate the two.

## Already covered by this port

**Nothing in this port currently touches any of the four assigned machine types.** Confirmed by grep across `src/main/java/com/hbm/blockentity` (recursive) and `src/main/java/com/hbm/inventory/recipes` (recursive, listing all files) for `difurnace`, `blast_furnace`/`blastfurnace`, `rotary_furnace`/`rotaryfurnace`, `exposure_chamber`/`exposurechamber`, `transmutation` (case-insensitive) — zero matches in either directory tree. A separate whole-port grep for `rotary_furnace`, `exposure_chamber`, `difurnace`, `blast_furnace` (block/registry-name form) also returned zero. This is a clean, uncontested gap, not a partial-coverage situation — **the "Already covered" section required by this task's format is empty by design**; there is no port-side file to diff against.

The one adjacent thing already on record: `src/main/java/com/hbm/blocks/machine/PowerGenBlocks.java`'s own class javadoc (lines 30-37, Phase 2) explicitly **excludes** `TileEntityDiFurnaceRTG`/`TileEntityRtgFurnace` from that file's power-generation family scope ("RTG-fuel-accelerated *smelting* machines, not power generators... left to whichever area owns RTG-accelerated processing") — i.e. Phase 2 already looked at this territory and explicitly declined it, consistent with this task's own finding that it is still fully unclaimed.

What **is** already committed and directly reusable (not the machines themselves, but the scaffolding all four recipe files need — see "Recommended implementation shape" below):
- `src/main/java/com/hbm/inventory/RecipesCommon.java` — `AStack` (abstract), `ComparableStack`, `NbtComparableStack`, `OreDictStack` (now tag-based: `TagKey<Item>` + `ofCommonTag(String path)`, replacing CE's ore-dict string), all already exist and are already consumed by `CrystallizerRecipes`/`MixerRecipes`.
- `src/main/java/com/hbm/inventory/fluid/{FluidStack,FluidType,Fluids}.java` — `Fluids.java` is 97.5%-ported per `docs/phase6/PARITY_REPORT.md` §3.3; every fluid `RotaryFurnaceRecipes` needs (`LIGHTOIL`, `GAS_COKER`, `REFORMGAS`, `SODIUM_ALUMINATE`) and `BlastFurnaceRecipes` needs (`GASOLINE`, `OIL`) already exists as a `FluidType` field.
- `src/main/java/com/hbm/inventory/material/{Mats,MaterialShapes,NTMMaterial}.java` — the material/shape abstraction this task's prompt named. `Mats.MaterialStack` (the exact class `RotaryFurnaceRecipes`' `output` field needs) already exists verbatim. `MaterialShapes.commonTag(NTMMaterial)` builds the `c:<shape>/<material>` tag CE's `DictFrame`/ore-dict inputs should become.
- `src/main/java/com/hbm/items/{IngotNuggetItems,BilletPowderItems,PlateCrystalWasteItems}.java` and `MaterialItemGenerator.java`/`blocks/MaterialBlockGenerator.java` — cover the overwhelming majority of these four files' ingredient/output items already (see dependency check below).
- `src/main/java/com/hbm/inventory/recipes/{CrystallizerRecipes,MixerRecipes}.java` — **the established convention** to copy for all four assigned files (bespoke lazy-registered plain-Java data class, not JSON) — see "Recommended implementation shape."

## Full recipe/entry catalog

### BlastFurnaceRecipes.java — fuels (`registerFuels()`, lines 37-57, 21 entries)

| # | CE line | Fuel input | Power |
|---:|---:|---|---:|
| 1 | 37 | `COAL.gem()` (coal ore-dict "gem" form) | 200 |
| 2 | 38 | `COAL.dust()` | 220 |
| 3 | 39 | `COAL.block()` | 2000 |
| 4 | 40 | `LIGNITE.gem()` | 150 |
| 5 | 41 | `LIGNITE.dust()` | 150 |
| 6 | 42 | `LIGNITE.block()` | 1500 |
| 7 | 43 | `ModItems.briquette` | 200 |
| 8 | 44 | `"gemCharcoal"` (ore-dict) | 150 |
| 9 | 45 | `"blockCharcoal"` | 1500 |
| 10 | 46 | `"fuelCoke"` | 400 |
| 11 | 47 | `ANY_COKE.gem()` | 400 |
| 12 | 48 | `ANY_COKE.block()` | 4000 |
| 13 | 49 | `Items.LAVA_BUCKET` (vanilla) | 12800 |
| 14 | 50 | `Items.BLAZE_ROD` (vanilla) | 1000 |
| 15 | 51 | `Items.BLAZE_POWDER` (vanilla) | 300 |
| 16 | 52 | `Items.COAL` meta 1 (vanilla, unused 1.12 damage variant) | 200 |
| 17 | 53 | `INFERNAL.gem()` | 300 |
| 18 | 54 | `INFERNAL.block()` | 3000 |
| 19 | 55 | `ModItems.solid_fuel` | 400 |
| 20 | 56 | `ModItems.solid_fuel_presto` | 800 |
| 21 | 57 | `ModItems.solid_fuel_presto_triplet` | 2400 |

### BlastFurnaceRecipes.java — item recipes (`registerDefaults()`, lines 273-292, 16 entries)

Input notation: a bare material name (e.g. `IRON`) means CE's wildcard "DictFrame" match — the raw `Item`/`DictFrame` object is passed to `addRecipe`, which (per `getRecipeStacks`, lines 184-202) expands to **any of that material's ingot/plate/gem/dust ore-dict forms**, not one specific form. A `.method()` call (e.g. `IRON.ore()`) is a single specific ore-dict key.

| # | CE line | Input 1 | Input 2 | Output | Notes |
|---:|---:|---|---|---|---|
| 1 | 273 | `IRON` (wildcard) | `COAL` (wildcard) | `ingot_steel` ×1 | |
| 2 | 274 | `IRON` (wildcard) | `ANY_COKE` (wildcard) | `ingot_steel` ×1 | |
| 3 | 275 | `IRON.ore()` | `COAL` (wildcard) | `ingot_steel` ×2 | |
| 4 | 276 | `IRON.ore()` | `ANY_COKE` (wildcard) | `ingot_steel` ×3 | |
| 5 | 277 | `IRON.ore()` | `ComparableStack(powder_flux)` | `ingot_steel` ×3 | |
| 6 | 279 | `CU` (wildcard) | `REDSTONE` (wildcard) | `ingot_red_copper` ×2 | |
| 7 | 280 | `W` (wildcard) | `COAL` (wildcard) | `neutron_reflector` ×2 | |
| 8 | 281 | `W` (wildcard) | `ANY_COKE` (wildcard) | `neutron_reflector` ×2 | |
| 9 | 282 | `ComparableStack(canister_full, dmg=1, meta=Fluids.GASOLINE.getID())` | `"slimeball"` (ore-dict) | `canister_napalm` ×1 | 1.12 NBT/meta-encoded fluid-filled canister match |
| 10 | 283 | `W` (wildcard) | `SA326.nugget()` = `nugget_schrabidium` | `ingot_magnetized_tungsten` ×1 | `SA326` = "Schrabidium" alias (CE's `OreDictManager` naming, **not** intuitive — verified line-by-line, see Open questions) |
| 11 | 284 | `STEEL` (wildcard) | `TC99.nugget()` = `nugget_technetium` (CE: "Technetium99/Tc99") | `ingot_tcalloy` ×1 | |
| 12 | 285 | `GOLD.plate()` = `plate_gold` | `ModItems.plate_mixed` | `plate_paa` ×2 | |
| 13 | 286 | `BIGMT` (wildcard) = "Saturnite" alias | `ModItems.ingot_meteorite` | `ingot_starmetal` ×2 | |
| 14 | 287 | `CO` (wildcard) = "Cobalt" | `ModItems.powder_meteorite` | `ingot_meteorite` ×1 | |
| 15 | 288 | `ModItems.meteorite_sword_hardened` | `CO` (wildcard) | `meteorite_sword_alloyed` ×1 | Output added to `hiddenRecipes` (line 301) — hidden from NEI/JEI listing |
| 16 | 292 | `ModItems.canister_empty` | `COAL` (wildcard) | `canister_full`, dmg=1, meta=`Fluids.OIL.getID()` | **Conditional**: only if `GeneralConfig.enableLBSM && GeneralConfig.enableLBSMSimpleChemsitry` both true |

Also: an IMC (inter-mod-communication) buffer drain at lines 295-299 (`IMCBlastFurnace.buffer`) — a 1.12-only cross-mod integration hook, not applicable to this port (no other mods exist to send IMC messages), safe to drop entirely.

### RotaryFurnaceRecipes.java — all 12 entries (`registerDefaults()`, lines 32-46)

Constructor shape: `RotaryFurnaceRecipe(MaterialStack output, int duration, int steam, [FluidStack fluid,] AStack... ingredients)`.

| # | CE line | Output (material × INGOT-quanta) | Duration | Steam | Fluid input | Item/AStack inputs |
|---:|---:|---|---:|---:|---|---|
| 1 | 32 | `MAT_STEEL` × 1 ingot | 100 | 100 | — | `OreDictStack(IRON.ingot())`, `OreDictStack(COAL.gem())` |
| 2 | 33 | `MAT_STEEL` × 1 ingot | 100 | 100 | — | `OreDictStack(IRON.ingot())`, `OreDictStack(ANY_COKE.gem())` |
| 3 | 35 | `MAT_STEEL` × 2 ingot | 200 | 25 | — | `OreDictStack(IRON.fragment(), 9)`, `OreDictStack(COAL.gem())` |
| 4 | 36 | `MAT_STEEL` × 3 ingot | 200 | 25 | — | `OreDictStack(IRON.fragment(), 9)`, `OreDictStack(ANY_COKE.gem())` |
| 5 | 37 | `MAT_STEEL` × 4 ingot | 400 | 25 | — | `OreDictStack(IRON.fragment(), 9)`, `OreDictStack(ANY_COKE.gem())`, `ComparableStack(powder_flux)` |
| 6 | 39 | `MAT_DESH` × 1 ingot | 100 | 200 | `LIGHTOIL` ×100mB | `ComparableStack(powder_desh_ready)` |
| 7 | 41 | `MAT_GUNMETAL` × 4 ingot | 200 | 100 | — | `OreDictStack(CU.ingot(), 3)`, `OreDictStack(AL.ingot(), 1)` |
| 8 | 42 | `MAT_WEAPONSTEEL` × 1 ingot | 200 | 400 | `GAS_COKER` ×100mB | `OreDictStack(STEEL.ingot(), 1)`, `ComparableStack(powder_flux, 2)` |
| 9 | 43 | `MAT_SATURN` × 2 ingot | 200 | 400 | `REFORMGAS` ×250mB | `OreDictStack(DURA.dust(), 4)`, `OreDictStack(CU.dust())` |
| 10 | 44 | `MAT_SATURN` × 4 ingot | 200 | 300 | `REFORMGAS` ×250mB | `OreDictStack(DURA.dust(), 4)`, `OreDictStack(CU.dust())`, `OreDictStack(BORAX.dust())` |
| 11 | 45 | `MAT_ALUMINIUM` × 2 ingot | 100 | 400 | `SODIUM_ALUMINATE` ×150mB | (none) |
| 12 | 46 | `MAT_ALUMINIUM` × 3 ingot | 40 | 200 | `SODIUM_ALUMINATE` ×150mB | `ComparableStack(powder_flux, 2)` |

Note: `getRecipes()` (JEI helper, lines 49-66) wraps every `output` through `ItemScraps.create(recipe.output, true)` — CE's generic material-representation item, **not** evidence that the real output item is literally "scraps". The real machine (`TileEntityMachineRotaryFurnace`, not read — out of this task's file scope) is what actually resolves a `MaterialStack` to a concrete dispensed `ItemStack`; the recipe class itself only carries the material+amount abstraction.

### ExposureChamberRecipes.java — all 4 entries (`registerDefaults()`, lines 55-65)

| # | CE line | Particle key | Ingredient | Output |
|---:|---:|---|---|---|
| 1 | 55-56 | `ComparableStack(particle_higgs)` | `OreDictStack(U.ingot())` = `ingot_uranium` | `ingot_schraranium` |
| 2 | 57-58 | `ComparableStack(particle_higgs)` | `OreDictStack(U238.ingot())` = `ingot_u238` | `ingot_schrabidium` |
| 3 | 59-60 | `ComparableStack(particle_dark)` | `OreDictStack(PU.ingot())` = `ingot_plutonium` | `ingot_euphemium` |
| 4a | 62 | `ComparableStack(particle_sparkticle)` | `ComparableStack(item_expensive, dmg=1, meta=DEGENERATE_MATTER)` | `ingot_dineutronium` | **only if** `GeneralConfig.enableExpensiveMode` |
| 4b | 64 | `ComparableStack(particle_sparkticle)` | `OreDictStack(SBD.ingot())` = `ingot_schrabidate` (CE: `SBD` = "Schrabidate", **not** Schrabidium — verified against `OreDictManager.java:132`) | `ingot_dineutronium` | else-branch (default config) |

`getRecipes()` (JEI helper, lines 32-51) wraps outputs to ×8 for display, matching a fixed 8-particle batch — cosmetic, not load-bearing recipe logic.

### NuclearTransmutationRecipes.java — all 3 entries (`registerRecipes()`, lines 25-27)

| # | CE line | Input | Output | Energy (HE) |
|---:|---:|---|---|---:|
| 1 | 25 | `OreDictStack(U.crystal())` = `crystal_uranium` | `crystal_schraranium` ×1 | 5,000,000 |
| 2 | 26 | `OreDictStack(U.ingot())` = `ingot_uranium` | `ingot_schraranium` ×1 | 5,000,000 |
| 3 | 27 | `OreDictStack(U.block())` = `uranium_block` (this port's autogen id, **not** CE's `block_uranium` name — see dependency check) | `ModBlocks.block_schraranium` → this port's `schraranium_block` | 50,000,000 |

## Item/registry dependency check

Methodology note (load-bearing for whoever reads this next): a plain literal-string grep for an item id **only finds hand-declared items** (`IngotNuggetItems.java`, `BilletPowderItems.java`, `PlateCrystalWasteItems.java`, and similar curated files, which use CE's original `<shape>_<material>` prefix convention, e.g. `ingot_steel`). It **will not find** an item generated by `MaterialItemGenerator`/`MaterialBlockGenerator` (`src/main/java/com/hbm/items/MaterialItemGenerator.java`, `src/main/java/com/hbm/blocks/MaterialBlockGenerator.java`), because those build the registry-name string at runtime via `MaterialShapes.buildRegistryName(NTMMaterial)` = `<material>_<shape>` (**suffix** convention, e.g. `uranium_block`, `iron_ore_fragment`) — the opposite naming order from the hand-declared files. Confirming existence for those requires cross-referencing `Mats.java`'s per-material `.setAutogen(...)` list against `MaterialItemGenerator.AUTOGEN_SHAPES`/`MaterialBlockGenerator`'s covered-shape set (both read in full for this task). Every check below used the correct method per item; this distinction is exactly the kind of thing a shallow "grep and declare missing" pass would get wrong, so it is called out explicitly.

**Ready now (every ingredient AND output already exists):**
- **`NuclearTransmutationRecipes` — all 3 of 3 recipes.** `crystal_uranium`, `ingot_uranium`, `uranium_block` (autogen, `MAT_URANIUM.setAutogen(...BLOCK)`, `PlateCrystalWasteItems.java`/`IngotNuggetItems.java`) all exist; `crystal_schraranium`, `ingot_schraranium` (`PlateCrystalWasteItems.java:197`-area / `IngotNuggetItems.java:161`) exist; `schraranium_block` exists (autogen, `MAT_SCHRARANIUM.setAutogen(...BLOCK)`, confirmed in `MaterialBlockGenerator.java` lines 121/147). **This is the single cheapest win in this assignment** — 3 recipes, zero missing items, only needs the machine + recipe-registration code, no item-registration prerequisite work at all.
- **`ExposureChamberRecipes` — the material side of all 4 recipes.** `ingot_uranium`, `ingot_u238`, `ingot_plutonium`, `ingot_schrabidate` (inputs) and `ingot_schraranium`, `ingot_schrabidium`, `ingot_euphemium`, `ingot_dineutronium` (outputs) all already exist. **Blocked purely on the particle-key side** — see below.
- **`RotaryFurnaceRecipes` — 10 of 12 recipes** (all except the 2 `MAT_SATURN` recipes at lines 43-44). Every output (`ingot_steel`, `ingot_gunmetal`, `ingot_weaponsteel`, `ingot_desh` [hand-declared as `ingot_desh` despite `MAT_DESH`'s internal display name `"WorkersAlloy"`], `ingot_saturnite`, `ingot_aluminium`) and every input except `powder_durasteel` already exists: `ingot_uranium`-class inputs n/a here; confirmed present: `powder_flux`, `powder_desh_ready`, `powder_copper` (`POWDER_COPPER`), `ingot_aluminium`, vanilla `IRON_INGOT`/`COAL`/`COPPER_INGOT`, and the 4 fluids (`LIGHTOIL`, `GAS_COKER`, `REFORMGAS`, `SODIUM_ALUMINATE`). `powder_borax` (`BilletPowderItems.java:170`) also exists.
- **`BlastFurnaceRecipes` — roughly half.** Outputs `ingot_steel`, `ingot_red_copper`, `ingot_magnetized_tungsten`, `ingot_tcalloy`, `plate_paa`, `ingot_starmetal`, `ingot_meteorite` all exist, as do inputs `powder_flux`, `plate_gold`, `plate_mixed`, `powder_meteorite`, `nugget_schrabidium`, `nugget_technetium`, and all vanilla items (`IRON`, `COAL`, `GOLD`, `REDSTONE`, `LAVA_BUCKET`, `BLAZE_ROD`/`_POWDER`). Recipes #1-8, #10-14 (12 of 16) in the catalog above have every ingredient present.

**Blocked (missing item, named exactly):**
- **`particle_higgs`, `particle_dark`, `particle_sparkticle`** — confirmed absent from this port by grep across all of `src/main/java/com/hbm/items` (no `particle_*` canister item anywhere). In CE these are plain `ItemBase` items (`ModItems.java:2320`/`2322`/`2324`, part of a `particle_empty`/`particle_*` container family) produced by CE's **Particle Accelerator** (`ParticleAcceleratorRecipes.java`, PARITY_REPORT §5's list of 63 unported machine-recipe classes). **Blocks all 4 `ExposureChamberRecipes` entries** even though every other ingredient/output for all 4 already exists — this is a single missing 4-item family (`particle_empty` + the 3 above, likely more per CE's real `particle_*` set) that unblocks a fully-ready recipe class the moment it lands.
- **`powder_durasteel`** — confirmed absent (`BilletPowderItems.java` has no `POWDER_DURASTEEL`/`registerPowder("powder_durasteel")`, despite `MAT_DURA.setAutogen(...DUST...)` declaring the shape — DUST is a hand-curated shape, not `MaterialItemGenerator`-covered, so this is a genuine "declared in Mats.java but never hand-added to `BilletPowderItems.java`" gap). Blocks `RotaryFurnaceRecipes` #9-10 (the 2 `MAT_SATURN` recipes).
- **`ANY_COKE` family (no port-side "coke" item at all)** — confirmed zero `coke`/`fuel_coke`/`gem_coke`/`coal_coke` items anywhere in `src/main/java/com/hbm`. Blocks `BlastFurnaceRecipes` fuel entries #10-12, recipe entries #2, #4, #8 (3 recipes) — CE's coking-process item family (`CokerRecipes`, another of the 63 unported machine-recipe classes) is a real prerequisite here.
- **`briquette`, `solid_fuel`, `solid_fuel_presto`, `solid_fuel_presto_triplet`** — all 4 confirmed missing (fuel entries #7, #19-21). Likely part of an unported fuel-pellet/processing item family.
- **`INFERNAL` (Nether "infernal coal")** — confirmed no `infernal_coal`/similar item exists. Blocks fuel entries #17-18.
- **`canister_full`, `canister_empty`, `canister_napalm`** — confirmed missing (`ItemCanister.java` exists as a class but registers zero concrete canister items yet; the one hit for `"canister_empty"` is a code **comment**, not a registration). Blocks `BlastFurnaceRecipes` recipe #9 and the conditional #16.
- **`meteorite_sword_hardened`, `meteorite_sword_alloyed`** — confirmed missing (part of CE's unported meteorite-sword weapon-upgrade chain). Blocks `BlastFurnaceRecipes` recipe #15 (already a `hiddenRecipes`-flagged/JEI-suppressed recipe in CE, so low priority regardless).
- **`item_expensive` (`EnumExpensiveType.DEGENERATE_MATTER`)** — confirmed missing, but this only gates `ExposureChamberRecipes`' config-flag alt-path (#4a); the real default-config path (#4b, via `ingot_schrabidate`) is already fully satisfiable, so this specific miss is low-priority.
- **`"gemCharcoal"`/`"blockCharcoal"`/`"fuelCoke"` ore-dict-keyed fuels** (#8-10) — not individually re-verified (these are generic ore-dict tag references CE itself treats as "match anything with this ore-dict key", likely resolvable to vanilla charcoal + the same missing coke family above) — flagged, not confirmed either way.

## Recommended 1.21.1 implementation shape

**None of these four fit a vanilla shaped/shapeless/single-`RecipeType` JSON shape**, and this port already has two proven precedents that settle the question:

1. **Bespoke lazy-registered plain-Java data class — the `CrystallizerRecipes`/`MixerRecipes` pattern (`src/main/java/com/hbm/inventory/recipes/{CrystallizerRecipes,MixerRecipes}.java`, both read in full).** This is the right shape for **all four** assigned files, for the same reason those two already-ported files gave: multi-input matching against a competing/keyed table, no fluid `Codec` yet (`FluidStack` still has none — confirmed still true, same gap `CrystallizerRecipes`' own javadoc names), and (for `RotaryFurnaceRecipes` specifically) an output type (`Mats.MaterialStack`) that is not a fixed `ItemStack` at all. Concretely, each of the four should become its own final class in `com.hbm.inventory.recipes`, following the exact shape already established:
   - A `private static final Map<...> RECIPES` (or `List<...>` where CE's own structure is a list, e.g. `BlastFurnaceRecipes`' triplet list, `RotaryFurnaceRecipes`' list).
   - A `private static boolean registered` guard + `public static synchronized void registerDefaults()`, called lazily from the first real lookup (not eagerly from a block/mod registration bootstrap) — copy `CrystallizerRecipes.registerDefaults()`'s own javadoc verbatim on the registry-not-populated-yet hazard; it applies identically here.
   - Reuse `RecipesCommon.{AStack,ComparableStack,NbtComparableStack,OreDictStack}` as-is — all already exist and already support exactly the match semantics these four files need (`OreDictStack.ofCommonTag(String)` for tag-based multi-form matching, e.g. `IRON.ore()`-equivalent or the "DictFrame wildcard" pattern — build a tiny local helper mirroring CE's own `getRecipeStacks()` DictFrame branch: an `AStack[]` of `OreDictStack.ofCommonTag(...)` over `{ingots,plates,gems,dusts}/<material>` tag folders, using `MaterialShapes.commonTag(NTMMaterial)` to build each).
   - Reuse `Mats.MaterialStack` directly for `RotaryFurnaceRecipes`' output field — it is the exact same class, already committed, no adaptation needed.
   - `NuclearTransmutationRecipes` is simplest of all four — no `SerializableRecipe`/JSON-loader inheritance in CE either, just two parallel maps (`recipesOutput`, `recipesEnergy`) and 3 static lookups; a direct 1:1 port of the class shape, updating only the ore-dict/`ItemStack` types to this port's equivalents.
   - `BlastFurnaceRecipes` additionally needs its **fuel table** (`diFuels`, a separate `Map<AStack,Integer>`, and `getItemPower`/`getAlloyFuels`) ported alongside the item-recipe table — these are two logically-separate registries CE keeps in one class; keep that pairing, it is what the eventual `TileEntityDiFurnace`-equivalent block entity will call into for both "can accept this fuel" and "what does this recipe produce."

2. **Do not attempt a JSON `Recipe<?>`/`RecipeSerializer`** (the `HbmSimpleRecipe`/`AssemblerRecipe` shape in `src/main/java/com/hbm/inventory/recipes/{HbmSimpleRecipe,AssemblerRecipe,ProcessingRecipes}.java`, all read in full) for any of the four. `HbmSimpleRecipe` is single-in/single-out only — none of these four are. `AssemblerRecipe` is the closest existing JSON shape (multiset `Ingredient`+count matching, no fluid) and could in principle cover `BlastFurnaceRecipes`' 2-item-in/1-out shape **if** the "DictFrame wildcard" inputs were pre-expanded into `Ingredient.of(tag)` — but `AssemblerRecipe` has no fuel-table concept and no `hiddenRecipes`/JEI-suppression flag, both of which `BlastFurnaceRecipes` genuinely needs, and CE's own `@Deprecated` marking on this exact class plus the fact that a datapack-JSON recipe collection can't easily also serve as "the fuel-value lookup table a block entity queries every tick" (`getItemPower`) argue for keeping it a plain Java class matching CE's own `getItemPower`/`getOutput`/`getRequiredCounts` API shape 1:1, not fragmenting fuel-table logic away from recipe-table logic into two different subsystems.

3. **The machines themselves still need to be built — none exist in this port today.** Per the CE cross-check done for this task:
   - `BlastFurnaceRecipes` → CE's `MachineDiFurnace`/`MachineDiFurnaceRTG` (`extends BlockContainer`, **simple single-block**, on/off block-id pair) + `TileEntityDiFurnace`/`TileEntityDiFurnaceRTG`. Simplest of the four to build a block entity for.
   - `RotaryFurnaceRecipes` → CE's `MachineRotaryFurnace` (`extends BlockDummyable implements ILookOverlay`, **multiblock** — `meta>=12` gives the real TE, `meta>=6` gives a `TileEntityProxyCombo`) + `TileEntityMachineRotaryFurnace`. Needs multiblock scaffolding, a materially bigger lift than a plain block entity.
   - `ExposureChamberRecipes` → CE's `MachineExposureChamber` (also `extends BlockDummyable`, **multiblock**) + `TileEntityMachineExposureChamber`. Same multiblock caveat.
   - `NuclearTransmutationRecipes` — its consuming machine/block-entity was not identified within this task's file scope (no `NuclearTransmutation`-named block/TE turned up in the greps run for this task); flagged under Open Questions.
   This task is recipe-data research only per its own brief — noted here so the implement wave does not assume "port the recipe class" alone makes any of these four playable; three of four also need a new machine block+block-entity (one of them a multiblock), and the recipe registration work above is necessary-but-not-sufficient on its own.

## Open questions / risks

1. **`BlastFurnaceRecipesNT.java` (105 lines, CE's newer, unrelated "real" blast-furnace machine, `machine_blast_furnace`/`TileEntityMachineBlastFurnace`) is NOT part of this assignment and was not researched here beyond confirming it exists and is a different class/machine.** A reader who sees "blast furnace" in this report's title should not assume it covers CE's actual, currently-primary Blast Furnace machine — it does not. Whoever picks up `BlastFurnaceRecipesNT` should treat it as a wholly separate research/port task (it `extends GenericRecipes<BlastFurnaceRecipe>`, CE's `GenericRecipe`-shaped machine family this port's own `com.hbm.inventory.recipes.loader.GenericRecipe`/`GenericRecipes` stand-in classes explicitly flag as "not the real shape yet, whoever ports a real GenericRecipe-shaped machine should extend or replace this").
2. **CE's `OreDictManager` alias abbreviations are genuinely non-obvious and were verified line-by-line for this report, not assumed** — in particular `SA326` = "Schrabidium" and `SBD` = "Schrabidate" (**not** the reverse, which would be the intuitive-but-wrong reading given "SBD" looks like it should abbreviate "Schrabidium"). Confirmed against `upstream/hbm-ce/.../OreDictManager.java:130` (`SA326`) and `:132` (`SBD`). An implementer working from memory or a quick skim risks swapping these two and mis-wiring `BlastFurnaceRecipes` recipe #10 / `ExposureChamberRecipes` recipe #4b.
3. **`NuclearTransmutationRecipes`' consuming machine/block-entity was not identified.** This is the one file of the four where this task could not confirm which CE block/TE calls `NuclearTransmutationRecipes.getOutput()`/`getEnergy()` (a targeted grep for `NuclearTransmutation` across `com.hbm.blocks`/`com.hbm.tileentity` was not run to completion within this task's time budget — a follow-up grep of CE's tree for `NuclearTransmutationRecipes\.` call sites outside the recipe file itself would resolve this quickly).
4. **CE's ore-dict "DictFrame wildcard" semantics (bare `IRON`/`COAL`/etc. passed to `addRecipe`) are a real behavioral detail, not a simplification to skip.** This task confirmed the mechanism (`getRecipeStacks`, `BlastFurnaceRecipes.java:184-202`) expands a bare material reference to 4 candidate forms (ingot/plate/gem/dust), each independently matchable — i.e. `addRecipe(IRON, COAL, ...)` accepts iron **plate** + coal **dust**, not just iron ingot + coal gem. A naive port that reads `IRON` as "iron ingot specifically" would silently narrow these recipes. `MaterialShapes.commonTag(NTMMaterial)` gives exactly the per-shape tags needed to reproduce this faithfully (see implementation shape above), but the "try all 4 shapes" loop itself still needs writing.
5. **`RotaryFurnaceRecipes`' `MaterialStack`-typed output means the real "what item comes out" answer lives partly outside this recipe file**, in whatever block entity resolves `MaterialStack → ItemStack` (via `MaterialShapes.buildRegistryName`-style lookup, most likely). This task did not read `TileEntityMachineRotaryFurnace.java` (out of scope) to confirm the exact resolution call CE makes at production time — flagged so the implement wave reads that file before assuming the recipe class alone is sufficient to wire up real item output.
6. **Two of the four consuming machines (`RotaryFurnace`, `ExposureChamber`) are CE multiblocks (`BlockDummyable`)**, materially more implementation work than a single-block machine — this changes the effort estimate for "port the machine" relative to what a reader might assume from "these are just small recipe files."
7. **The `"gemCharcoal"`/`"blockCharcoal"`/`"fuelCoke"` bare ore-dict-string fuel entries** (`BlastFurnaceRecipes.java:44-46`) were not individually cross-checked against a specific port-side item beyond the `ANY_COKE`-family miss already reported — plausible they resolve to vanilla charcoal (already exists) once translated to a `c:` tag, but not confirmed.
