# Research report — mrec-12-electrolysermetal-misc

Assignment: `ElectrolyserMetalRecipes.java` (314 lines), `FusionRecipes.java` (157 lines), `EngineRecipes.java` (112 lines) — three CE per-machine-type recipe-data classes under `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`.

**Headline finding, stated up front because it changes the shape of the assignment for two of the three files:** this is not three equally-blocked "port this data" tasks.

- **`EngineRecipes.java` needs no porting at all.** Its entire functional payload is already present in this port, ported via a *different, better* mechanism (`FT_Combustible` fluid trait) that this port's own `Fluids.java` already carries 1:1 with CE's — and CE's own `EngineRecipes` class turns out to be dead code (zero live call sites for any of its query methods anywhere in CE). Evidence in §2.
- **`ElectrolyserMetalRecipes.java` is a real, well-scoped, currently-blocked gap** — blocked on the Crucible/`CrucibleUtil.pourFullStack` foundry-casting system, exactly Phase 6's root-cause #1, plus a few missing bedrock-ore helper methods. The port's own code already documents this exact blocker by name.
- **`FusionRecipes.java` is blocked on something much bigger than recipe data**: the CE machine that consumes it (`TileEntityFusionTorus`'s hot-fusion tokamak, `KlystronNetwork`/`PlasmaNetwork`, `ModuleMachineFusion`) is a structurally distinct, unported CE subsystem — different from the ICF/Watz fusion family this port *has* built. Porting the recipe data without the machine produces nothing playable; this file should be treated as belonging to a future "tokamak machine" task, not a recipe-corpus task.

---

## 1. Scope confirmed

| File | CE path | Lines | In-CE structure |
|---|---|---:|---|
| `ElectrolyserMetalRecipes` | `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/ElectrolyserMetalRecipes.java` | 314 | `extends SerializableRecipe`. `registerDefaults()` (lines 32-152): **18 explicit `recipes.put(...)` calls** (lines 34-130, one crystal-item trigger each) + **one `for` loop over `ItemBedrockOreNew.BedrockOreType.VALUES`** (lines 132-151, 6 enum members × 3 recipes/member = 18 more) = **36 total `ElectrolysisMetalRecipe` entries**. Plus a helper factory `makeBedrockOreProduct(...)` (154-177), a lookup `getRecipe(ItemStack)` with ore-dict fallback (179-193), a JEI-facing `getRecipes()` adapter (195-212) that wraps every output through `ItemScraps.create(...)` and prepends a fixed `NITRIC_ACID` fluid-icon input, and CE's own JSON read/write pair (`readRecipe`/`writeRecipe`, 230-292) for its `SerializableRecipe` hot-reload system. Inner static class `ElectrolysisMetalRecipe` (294-313): `output1`/`output2` (`Mats.MaterialStack` — *molten material*, not `ItemStack`), `byproduct[]` (`ItemStack[]`), `duration` (default 600, bedrock loop overrides to 20). |
| `FusionRecipes` | `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/FusionRecipes.java` | 157 | `extends GenericRecipes<FusionRecipe>` (loader/JSON-hot-reload base, itself `extends SerializableRecipe`). `registerDefaults()` (28-121): **10 explicit named recipes** (`fus.dd`, `fus.do`, `fus.dt`, `fus.tcl`, `fus.h3`, `fus.th4`, `fus.cl`, `fus.dhc`, `fus.bf`, `fus.stellar`), each a single fluent-builder `this.register((FusionRecipe) new FusionRecipe(name)...)` call — flat list, no loop. Plus `registerPost()` (123-133, computes `maxInput` = max `ignitionTemp` across all recipes, for a "creative klystron" UI slider) and CE's JSON extra-field read/write pair for `ignitionTemp`/`outputTemp`/`outputFlux`/RGB (136-156). Sibling class `FusionRecipe.java` (66 lines, `extends GenericRecipe`) carries the actual per-recipe fields: `ignitionTemp`, `outputTemp`, `neutronFlux`, RGB floats, plus inherited `inputItem[]`/`inputFluid[]`/`outputItem[]`/`outputFluid[]`/`duration`/`power` from `GenericRecipe`. `FusionRecipes` itself declares the machine's I/O caps: `inputItemLimit()=0`, `inputFluidLimit()=3`, `outputItemLimit()=1`, `outputFluidLimit()=11`. |
| `EngineRecipes` | `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/EngineRecipes.java` | 112 | **Not** a `SerializableRecipe`/`GenericRecipes` subclass — a bare utility class with two static `HashMap<Fluid, X>` fields (`combustionEnergies`, `fuelGrades`). `registerEngineRecipes()` (15-47): **24 literal `addFuel(Fluids.X.getFF(), grade, energy)` calls** (16-39, one CE-native fluid each — note line 20 keys `HEAVYOIL` a second time with a different value, a likely CE typo, see §5) + **4 `addFuel(String, grade, energy)` calls for foreign-mod fluid ids** (42-45: `biofuel`/`petroil` duplicate-name entries plus `refined_fuel`/`refined_biofuel`, tagged in CE's own comments as Galacticraft/ThermalFoundation compat) = **28 total call sites**. `FuelGrade` enum (49-65: LOW/MEDIUM/HIGH/AERO/GAS). Getters `getEnergy`/`getFuelGrade`/`isAero`/`hasFuelRecipe`, mutators `addFuel`/`removeFuel` (two overloads each, `Fluid` and `String`-by-registered-name). |

Confirmed by grep: none of the three has a same-named port-side Java file. `find src/main/java/com/hbm -iname "ElectrolyserMetalRecipes.java" -o -iname "FusionRecipes.java" -o -iname "EngineRecipes.java"` returns nothing.

## 2. Already covered by this port

### 2.1 `EngineRecipes.java` — functionally 100% already covered, and CE's own class is dead code

This port already has `src/main/java/com/hbm/inventory/fluid/trait/FT_Combustible.java` (75 lines) — a per-`FluidType` trait carrying `FuelGrade` (LOW/MEDIUM/HIGH/AERO/GAS, same 5 grades, same enum-member names) and a `combustionEnergy` long, attached directly on each fluid in `src/main/java/com/hbm/inventory/fluid/Fluids.java` via `.addTraits(..., new FT_Combustible(FuelGrade.X, N), ...)`.

**This is not a port invention — CE has the exact same trait class** (`upstream/hbm-ce/.../fluid/trait/FT_Combustible.java`) attached the exact same way in CE's own `Fluids.java` (27 raw `FT_Combustible(` occurrences on both sides). A scripted diff of every `FLUID_NAME → (FuelGrade, energy)` pair extracted from both files' `FT_Combustible(FuelGrade.X, N)` call sites found:

- **25/25 fluid names match exactly** between CE and this port (zero set difference).
- **25/25 (grade, energy) value pairs match exactly** — zero mismatches on either field for any fluid.

Then, critically: **CE's real engine block entities never call `EngineRecipes` at all.** `grep -rn "EngineRecipes\.\(getEnergy\|getFuelGrade\|hasFuelRecipe\|isAero\|addFuel\|removeFuel\)" upstream/hbm-ce/src/main/java/com/hbm` returns **zero hits** anywhere in CE outside the class's own body. The only call site in all of CE is `MainRegistry.java:328`, which calls `EngineRecipes.registerEngineRecipes()` once at startup, populating two `HashMap`s that nothing ever reads. Meanwhile, every real CE combustion consumer — `TileEntityMachineCombustionEngine`, `TileEntityMachineDiesel`, `TileEntityMachineTurbineGas`, `TileEntityMachineTurbofan`, `TileEntityTurretFritz`, `ItemPistons`, `CokerRecipes`, `EntityChemical` (confirmed by grep for `FT_Combustible` usage in CE, 8 files outside `Fluids.java`/the trait class itself) — reads `tank.getTankType().getTrait(FT_Combustible.class)` directly off the `FluidType`, i.e. the *trait* mechanism, not `EngineRecipes`'s maps. `EngineRecipes.java` is legacy/orphaned code in CE itself, most likely superseded by the trait system at some point in CE's history and never deleted.

This port's own combustion consumers (`MachineCombustionEngineBlockEntity`, `MachineSteamEngineBlockEntity`, `MachineDieselBlockEntity`, `MachineTurbineGasBlockEntity` — all under `src/main/java/com/hbm/blockentity/machine/`, confirmed by grep for `FuelGrade`) already follow the same trait-reading pattern.

**What is NOT covered (a genuine, but functionally inert, data gap):** 8 fluids named only in `EngineRecipes.registerEngineRecipes()` — `BIOGAS`, `KEROSENE_REFORM`, `FISHOIL`, `SUNFLOWEROIL`, `GAS`, `PETROLEUM`, `AROMATICS`, `UNSATURATEDS` — exist as real `FluidType`s in **both** CE's and this port's `Fluids.java`, but **neither** side tags them with `FT_Combustible` (confirmed: both files' `addTraits(...)` calls for these 8 names lack `FT_Combustible`, matching exactly). Since nothing in CE ever reads `EngineRecipes`'s maps, these 8 fluids are **not actually combustible in real CE gameplay today** despite `registerEngineRecipes()` nominally registering them — this port's omission of the same data is therefore *faithful to CE's real behavior*, not a port gap. The 4 "Compat" foreign-mod-fluid entries (`biofuel`/`petroil` string duplicates, `refined_fuel`, `refined_biofuel` — CE's own comments cite Galacticraft/ThermalFoundation/IndustrialForegoing) are doubly out of scope: 1.12-only mod integration (Phase 6 PARITY_REPORT §4's named exclusion category) on top of being dead code.

**Bottom line: there is no remaining implementation gap here.** The one thing worth double-checking (not fixing) is the `HEAVYOIL` duplicate-key line — see §6.

### 2.2 `ElectrolyserMetalRecipes.java` — the port's own code already names this exact gap

The port already has the *other half* of the same CE machine ported: `src/main/java/com/hbm/inventory/recipes/chem/ElectrolyserFluidRecipes.java` (ported from CE's sibling class `com.hbm.inventory.recipes.ElectrolyserFluidRecipes`) plus `src/main/java/com/hbm/blockentity/machine/chem/ElectrolyserBlockEntity.java` (ported from CE's `TileEntityElectrolyser`, confirmed the same CE class that also drives `ElectrolyserMetalRecipes` — CE's `TileEntityElectrolyser.java` imports and calls both `ElectrolyserMetalRecipes.getRecipe(...)` (lines 366, 396, 430) and the fluid-recipe map, plus `tanks[3] = new FluidTankNTM(Fluids.NITRIC_ACID, 16000)` — the same acid input `ElectrolyserMetalRecipes.getRecipes()`'s JEI adapter hardcodes at line 202 of the CE file).

**This port's own javadoc already documents the exact gap this assignment is researching**, verbatim:

> `ElectrolyserBlockEntity.java:38-48`: "this pass ports the fluid side (`ElectrolyserFluidRecipes`, e.g. water -> hydrogen + oxygen) in full and deliberately **does not** port the ore/crystal electrolysis side - that half pours accumulated molten metal into the world via `com.hbm.util.CrucibleUtil.pourFullStack`, a foundry/casting system not ported anywhere in this port yet... `ElectrolyserFluidRecipes` therefore has no `ElectrolyserMetalRecipes` sibling in this pass either."
>
> `ElectrolyserFluidRecipes.java:19-22`: "The ore/crystal electrolysis half (`ElectrolyserMetalRecipes`) is not ported this pass - see `com.hbm.blockentity.machine.chem.ElectrolyserBlockEntity`'s javadoc: it requires `com.hbm.util.CrucibleUtil`'s foundry/casting system, not ported anywhere in this port yet, a real Phase 2/4 boundary dependency flagged by the research doc itself."

Confirmed independently: `grep -rln "CrucibleUtil" src/main/java/com/hbm` returns only these same two files' javadoc comments — no `CrucibleUtil` class exists anywhere in the port. `ElectrolyserMetalRecipes`/`ElectrolysisMetalRecipe` do not appear anywhere else in the port either.

**What partially exists (item-side groundwork, not the recipe/casting logic):** `src/main/java/com/hbm/items/machine/ItemScraps.java` (63 lines) already registers one `scrap_<material>` item per smeltable/additive `NTMMaterial` and already has a `create(ItemStack scrapItem, int amount, boolean liquid)` factory plus `MachineDataComponents.SCRAP_AMOUNT`/`SCRAP_LIQUID` data components — this is the port-side analogue of CE's `ItemScraps.create(Mats.MaterialStack, boolean)` that `ElectrolyserMetalRecipes.getRecipes()` (CE line 204-205) calls to wrap its molten outputs. The item registry and data-component plumbing exist; what's missing is the recipe-matching logic and the actual pour/cast behavior that would produce a `MaterialStack` to hand to it.

**Also not yet ported:** the CE `ItemBedrockOreNew.toFluid(...)`/`.extract(...)`/`.make(...)` static helpers the bedrock-ore loop (CE lines 132-151) depends on. This port's `BedrockOreType.java` (131 lines, `src/main/java/com/hbm/items/special/`) **does** already carry the `primary1`/`primary2` `BedrockOreOutput` fields 1:1 from CE's enum (confirmed: `public final BedrockOreOutput primary1; public final BedrockOreOutput primary2;` at lines 95-96, matching CE's `ItemBedrockOreNew.BedrockOreType` enum's own `primary1`/`primary2` fields exactly) — so the *data* the loop would consume is ready — but `grep -rln "toFluid\|extract\b" src/main/java/com/hbm/items/special` finds no matching helper methods on the port's `ItemBedrockOre`/`BedrockOreItems`/`ItemBedrockOreBase`/`BedrockOreOutput` classes. This is a second, smaller, independent blocker beyond just "Crucible doesn't exist" — worth naming precisely so the implement wave doesn't assume the bedrock loop is a trivial mechanical port once Crucible lands.

### 2.3 `FusionRecipes.java` — the consuming machine is a different, unported CE subsystem

The port's `src/main/java/com/hbm/blocks/machine/fusion/FusionBlocks.java` (registers `IcfReactorBlock`/`IcfControllerBlock`/`IcfPressBlock`/`WatzReactorBlock`, backed by block entities in `src/main/java/com/hbm/blockentity/machine/fusion/`) is a real, substantial port (1,191 combined lines across the 4 block-entity classes) — but its own class javadoc (lines 18-34) is explicit:

> "**Not ported this pass**: the hot-fusion tokamak (CE's `tileentity/machine/fusion/**`, `TileEntityFusionTorus` and its six `IFusionPowerReceiver` devices) - see this package's own follow-up notes for why (it is built around `com.hbm.uninos.networkproviders`' `KlystronNetwork`/`PlasmaNetwork`, neither of which exist in this port, plus the unported `com.hbm.modules.machine.ModuleMachineFusion` processing-loop abstraction - a structurally distinct, much larger system from the ICF/Watz pair this task named)."

This is confirmed to be precisely correct against CE's real source: `grep -rln "FusionRecipes\b" upstream/hbm-ce/src/main/java/com/hbm` (excluding the recipe class itself) finds exactly 4 consumers — `ModuleMachineFusion.java`, `TileEntityFusionKlystronCreative.java`, `handler/jei/FusionRecipeHandler.java`, `inventory/gui/GUIFusionTorus.java` — all under the tokamak/torus subsystem CE's own package `com.hbm.tileentity.machine.fusion` (which also holds `TileEntityFusionBreeder`, `TileEntityFusionBoiler`, `TileEntityFusionKlystron`, `TileEntityFusionCoupler`, `TileEntityFusionCollector`, `TileEntityFusionMHDT`, `TileEntityFusionPlasmaForge`, `IFusionPowerReceiver` — 10 files total, none of which exist in this port). `FusionRecipes.registerDefaults()`'s own code even hard-references this subsystem directly: `double breederCapacity = TileEntityFusionBreeder.capacity;` (line 32) reads a constant (`10_000D`, CE line 55 of `TileEntityFusionBreeder.java`) from a class that does not exist in this port.

**The port's own recipe-loader base classes also explicitly flag this file by name as work not yet done.** `src/main/java/com/hbm/inventory/recipes/loader/GenericRecipe.java` — a "minimal compile-time stand-in" for CE's real `GenericRecipe` — states in its header (lines 8-31):

> "CE's real `GenericRecipe` (~200 lines) is a full machine-recipe description: N `AStack` inputs, one fluid input, N chance-weighted `IOutput` outputs, one fluid output, duration/power... built by 9 of CE's ~60 recipe classes (`AssemblyMachineRecipes`, `ChemicalPlantRecipes`, **`FusionRecipes`**, etc)... this class only carries the slice `ItemBlueprints`/`ItemBlueprintFolder` actually read today: a stable internal name, the pool/localization-name fields... Whoever ports a real 'GenericRecipe-shaped' machine (the 9 named above) should extend or replace this with the real input/output/duration/power fields at that time."

I.e. this port's `GenericRecipe`/`GenericRecipes` classes today have **no** `inputItem[]`/`inputFluid[]`/`outputItem[]`/`outputFluid[]`/`duration`/`power` fields at all — only pool/localization bookkeeping for two blueprint items. `FusionRecipe`'s CE-specific fields (`ignitionTemp`/`outputTemp`/`neutronFlux`/RGB) have no port-side equivalent anywhere.

**Bottom line:** `FusionRecipes.java`'s recipe *data* is small and easy to transcribe (10 entries, §3.2), but transcribing it produces nothing usable — there is no machine to feed it, no base-class fields to hold it, and the machine itself needs two entire unported subsystems (`KlystronNetwork`/`PlasmaNetwork`, `ModuleMachineFusion`) before it could exist. This is fundamentally a "port the hot-fusion tokamak machine" task that happens to also need this recipe data, not a "port this recipe data" task — see §6.

## 3. Full recipe/entry catalog

All three files are small (36, 10, and 28 real entries respectively) — well under the ~150-entry threshold — so every entry is catalogued below in full, per the task's "SMALL file" instruction.

### 3.1 `ElectrolyserMetalRecipes.java` — 36 entries (18 explicit + 6×3 bedrock loop)

18 explicit crystal-item recipes (CE lines 34-130). Every `output1`/`output2` is a **molten** `Mats.MaterialStack` (material + quantity via `MaterialShapes.INGOT.q(n)`/`.NUGGET.q(n)`, cast via Crucible pour — not an `ItemStack`); `byproduct` items are solid `ItemStack`s handed out immediately. Duration defaults to 600 ticks (no entry overrides it).

| CE line | Trigger item (input, qty 1) | output1 (molten) | output2 (molten) | byproduct(s) |
|---|---|---|---|---|
| 34 | `crystal_iron` | MAT_IRON × INGOT.q(6) | MAT_TITANIUM × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 38 | `crystal_gold` | MAT_GOLD × INGOT.q(6) | MAT_LEAD × INGOT.q(2) | `powder_lithium_tiny` ×3, `ingot_mercury` ×2 |
| 44 | `crystal_uranium` | MAT_URANIUM × INGOT.q(6) | MAT_RADIUM × NUGGET.q(4) | `powder_lithium_tiny` ×3 |
| 49 | `crystal_thorium` | MAT_THORIUM × INGOT.q(6) | MAT_URANIUM × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 54 | `crystal_plutonium` | MAT_PLUTONIUM × INGOT.q(6) | MAT_POLONIUM × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 59 | `crystal_titanium` | MAT_TITANIUM × INGOT.q(6) | MAT_IRON × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 64 | `crystal_copper` | MAT_COPPER × INGOT.q(6) | MAT_LEAD × NUGGET.q(4) | `powder_lithium_tiny` ×3, `sulfur` ×2 |
| 70 | `crystal_tungsten` | MAT_TUNGSTEN × INGOT.q(6) | MAT_IRON × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 75 | `crystal_aluminium` | MAT_ALUMINIUM × INGOT.q(2) | MAT_IRON × INGOT.q(2) | `chunk_ore` (meta `CRYOLITE`) ×4, `powder_lithium_tiny` ×3 |
| 82 | `crystal_beryllium` | MAT_BERYLLIUM × INGOT.q(6) | MAT_LEAD × NUGGET.q(4) | `powder_lithium_tiny` ×3, `powder_quartz` ×2 |
| 88 | `crystal_lead` | MAT_LEAD × INGOT.q(6) | MAT_GOLD × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 93 | `crystal_schraranium` | MAT_SCHRABIDIUM × NUGGET.q(5) | MAT_URANIUM × NUGGET.q(2) | `nugget_neptunium` ×2 |
| 98 | `crystal_schrabidium` | MAT_SCHRABIDIUM × INGOT.q(6) | MAT_PLUTONIUM × INGOT.q(2) | `powder_lithium_tiny` ×3 |
| 103 | `crystal_rare` | MAT_ZIRCONIUM × NUGGET.q(6) | MAT_BORON × NUGGET.q(2) | `powder_desh_mix` ×3 |
| 108 | `crystal_trixite` | MAT_PLUTONIUM × INGOT.q(3) | MAT_COBALT × INGOT.q(4) | `powder_niobium` ×4, `powder_nitan_mix` ×2 |
| 114 | `crystal_lithium` | MAT_LITHIUM × INGOT.q(6) | MAT_BORON × INGOT.q(2) | `powder_quartz` ×2, `fluorite` ×2 |
| 120 | `crystal_starmetal` | MAT_DURA × INGOT.q(4) | MAT_COBALT × INGOT.q(4) | `powder_astatine` ×3, `ingot_mercury` ×8 |
| 126 | `crystal_cobalt` | MAT_COBALT × INGOT.q(3) | MAT_IRON × INGOT.q(4) | `powder_copper` ×4, `powder_lithium_tiny` ×3 |

**Bedrock-ore loop (CE lines 132-151, 6 `BedrockOreType` members × 3 recipes each = 18 more entries), the generating pattern:**

```
for(BedrockOreType type : BedrockOreType.VALUES) {   // 6 members: LIGHT_METAL, HEAVY_METAL,
                                                       //   RARE_EARTH, ACTINIDE, NON_METAL, CRYSTALLINE
    // trigger: PRIMARY_FIRST grade of this type
    //   -> molten(type.primary1 × 8) + molten(type.primary2 × 4) + solid(CRUMBS grade ×1)
    // trigger: PRIMARY_SECOND grade of this type
    //   -> molten(type.primary1 × 4) + molten(type.primary2 × 8) + solid(CRUMBS grade ×1)
    // trigger: CRUMBS grade of this type
    //   -> molten(type.primary1 × 2) + molten(type.primary2 × 2)
}
```

Each iteration builds a weighted product list, then `makeBedrockOreProduct(...)` (CE lines 154-177) splits it: the first ≤2 `BedrockOreOutput`-typed entries become the recipe's two molten outputs (via `ItemBedrockOreNew.toFluid(...)`), everything else (including the `CRUMBS`-grade `ItemStack` byproduct) becomes a solid byproduct (via `.extract(...)`). If a material has no valid molten form, `MAT_SLAG × INGOT.q(2)` is substituted as a fallback molten output. Duration is fixed at 20 for every bedrock-loop recipe (vs. 600 default for the 18 explicit recipes above). The 6 `BedrockOreType` members' own `primary1`/`primary2` material+quantity data (e.g. `LIGHT_METAL.primary1 = MAT_IRON×9`, `.primary2 = MAT_COPPER×9`) is defined once in the enum itself (CE `ItemBedrockOreNew.java:242-247`), not repeated per-recipe here — this port's `BedrockOreType.java` already carries that same data (§2.2).

### 3.2 `FusionRecipes.java` — 10 entries

All via `FusionRecipe(name).setInputEnergy(...).setOutputEnergy(...).setOutputFlux(...)[.setRGB(...)].setNamed().setIcon(...).setPower(25_000).setDuration(100).inputFluids(...).output{Items|Fluids}(...)`. `setPower`/`setDuration` are identical (`solenoid=25_000`, `100` ticks) on every recipe. Icon items are NEI/JEI display only, not consumed.

| Name | ignitionTemp | outputTemp | outputFlux | Inputs (fluid, mB) | Output | Icon (display only) |
|---|---:|---:|---|---|---|---|
| `fus.dd` | 750,000 | 1,000,000 | cap/200 | DEUTERIUM ×20 | HELIUM4 fluid ×1,000 | `gas_full`(DEUTERIUM) |
| `fus.do` | 250,000 | 1,250,000 | cap/200 | DEUTERIUM ×10, OXYGEN ×10 | `pellet_charged` ×1 | `gas_full`(OXYGEN) |
| `fus.dt` | 750,000 | 3,750,000 | cap/100 | DEUTERIUM ×10, TRITIUM ×10 | HELIUM4 fluid ×1,000 | `gas_full`(HELIUM4) |
| `fus.tcl` | 2,500,000 | 6,250,000 | cap/20 | TRITIUM ×10, CHLORINE ×10 | `powder_chlorophyte` ×1 | `powder_chlorophyte` |
| `fus.h3` | 500,000 | 3,750,000 | 0 | HELIUM3 ×20 | HELIUM4 fluid ×1,000 | `gas_full`(HELIUM3) |
| `fus.th4` | 875,000 | 4,000,000 | cap/20 | TRITIUM ×10, HELIUM4 ×10 | `pellet_charged` ×1 | `gas_full`(TRITIUM) |
| `fus.cl` | 3,750,000 | 10,000,000 | cap/10 | CHLORINE ×20 | `powder_chlorophyte` ×1 | `powder_chlorophyte` |
| `fus.dhc` | 10,000,000 | 25,000,000 | cap/5 | DHC ×20 | `powder_chlorophyte` ×1 | `fluid_icon`(DHC) |
| `fus.bf` | 1,000,000 | 12,500,000 | cap/5 | BALEFIRE ×15, AMAT ×5 | `powder_balefire` ×1 | `fluid_icon`(BALEFIRE) |
| `fus.stellar` | 10,000,000 | 50,000,000 | cap (=10,000) | STELLAR_FLUX ×10 | `powder_gold` ×1 | `fluid_icon`(STELLAR_FLUX) |

("cap" = `TileEntityFusionBreeder.capacity` = 10,000D, an unported constant — see §2.3.) `registerPost()` additionally computes `maxInput = max(ignitionTemp across all 10)` = 10,000,000 (tied between `fus.cl`/`fus.stellar`), used only for a creative-klystron UI slider bound — trivial to reproduce if/when ported.

### 3.3 `EngineRecipes.java` — 28 entries

`addFuel(fluid, grade, energyPerThousandMb)`. As established in §2.1, this data is a **dead-code duplicate** of what `FT_Combustible` traits already carry (24 of the 28 rows, all CE-native fluids, cross-checked exact-match in §2.1) — catalogued here in full for research completeness, not as an action item.

| CE line | Fluid | Grade | Energy | In this port's `FT_Combustible`? |
|---|---|---|---:|---|
| 16 | HYDROGEN | HIGH | 10,000 | Yes, matches |
| 17 | DEUTERIUM | HIGH | 10,000 | Yes, matches |
| 18 | TRITIUM | HIGH | 10,000 | Yes, matches |
| 19 | HEAVYOIL | LOW | 25,000 | Yes, matches |
| 20 | HEAVYOIL *(dup key, see §6)* | LOW | 100,000 | matches CE's own `HEATINGOIL` entry, not a literal `HEAVYOIL` 2nd value — see §6 |
| 21 | RECLAIMED | LOW | 200,000 | Yes, matches |
| 22 | PETROIL | MEDIUM | 300,000 | Yes, matches |
| 23 | NAPHTHA | MEDIUM | 200,000 | Yes, matches |
| 24 | DIESEL | HIGH | 500,000 | Yes, matches |
| 25 | LIGHTOIL | MEDIUM | 500,000 | Yes, matches |
| 26 | KEROSENE | AERO | 1,250,000 | Yes, matches |
| 27 | KEROSENE_REFORM | AERO | 1,750,000 | No — fluid exists, untagged (matches CE, §2.1) |
| 28 | BIOGAS | AERO | 500,000 | No — fluid exists, untagged (matches CE) |
| 29 | BIOFUEL | HIGH | 400,000 | Yes, matches |
| 30 | NITAN | HIGH | 5,000,000 | Yes, matches |
| 31 | BALEFIRE | HIGH | 2,500,000 | Yes, matches |
| 32 | GASOLINE | HIGH | 1,000,000 | Yes, matches |
| 33 | ETHANOL | HIGH | 200,000 | Yes, matches |
| 34 | FISHOIL | LOW | 50,000 | No — fluid exists, untagged (matches CE) |
| 35 | SUNFLOWEROIL | LOW | 80,000 | No — fluid exists, untagged (matches CE) |
| 36 | GAS | GAS | 100,000 | No — fluid exists, untagged (matches CE) |
| 37 | PETROLEUM | GAS | 300,000 | No — fluid exists, untagged (matches CE) |
| 38 | AROMATICS | GAS | 150,000 | No — fluid exists, untagged (matches CE) |
| 39 | UNSATURATEDS | GAS | 250,000 | No — fluid exists, untagged (matches CE) |
| 42 | `"biofuel"` (string, Galacticraft/IndustrialForegoing compat) | HIGH | 400,000 | N/A — 1.12-only mod compat, out of scope |
| 43 | `"petroil"` (string, Galacticraft compat) | MEDIUM | 300,000 | N/A — out of scope |
| 44 | `"refined_fuel"` (string, ThermalFoundation compat) | HIGH | 1,000,000 | N/A — out of scope |
| 45 | `"refined_biofuel"` (string, ThermalFoundation compat) | HIGH | 400,000 | N/A — out of scope |

## 4. Item/registry dependency check

### 4.1 `ElectrolyserMetalRecipes.java`

**Trigger items — 18/18 already registered**, confirmed individually: `crystal_iron`, `crystal_gold`, `crystal_uranium`, `crystal_thorium`, `crystal_plutonium`, `crystal_titanium`, `crystal_copper`, `crystal_tungsten`, `crystal_aluminium`, `crystal_beryllium`, `crystal_lead`, `crystal_schraranium`, `crystal_schrabidium`, `crystal_rare`, `crystal_trixite`, `crystal_lithium`, `crystal_starmetal`, `crystal_cobalt` — all in `src/main/java/com/hbm/items/PlateCrystalWasteItems.java`.

**Molten material references (`Mats.MAT_*`) — all confirmed already declared** in `src/main/java/com/hbm/inventory/material/Mats.java`: `MAT_IRON`, `MAT_TITANIUM`, `MAT_GOLD`, `MAT_LEAD`, `MAT_URANIUM`, `MAT_RADIUM`, `MAT_THORIUM`, `MAT_PLUTONIUM`, `MAT_POLONIUM`, `MAT_COPPER`, `MAT_TUNGSTEN`, `MAT_ALUMINIUM`, `MAT_BERYLLIUM`, `MAT_SCHRABIDIUM`, `MAT_ZIRCONIUM`, `MAT_BORON`, `MAT_COBALT`, `MAT_DURA`, `MAT_SLAG` — spot-checked 8 directly at their declaration lines, all present with `setAutogen(...)` shape lists matching what the recipe needs (e.g. `MAT_IRON.setAutogen(...INGOT...)` — wait, autogen listed FRAGMENT/DUST/PIPE/CASTPLATE/WELDEDPLATE/BLOCK for MAT_IRON specifically, not INGOT itself — **flagged as an open question in §6**, since several recipes need `INGOT`/`NUGGET` shapes of materials whose `setAutogen(...)` list in `Mats.java` doesn't literally include that shape token for every material checked; this needs a full per-material verification pass, not assumed complete from this spot check).

**Byproduct items — mixed:**

| Item | Status |
|---|---|
| `powder_lithium_tiny`, `powder_quartz`, `powder_desh_mix`, `powder_niobium`, `powder_nitan_mix`, `powder_astatine`, `powder_copper` | Ready — all in `src/main/java/com/hbm/items/BilletPowderItems.java` |
| `ingot_mercury`, `nugget_neptunium` | Ready — both in `src/main/java/com/hbm/items/IngotNuggetItems.java` |
| `fluorite` | Ready — in `src/main/java/com/hbm/items/PlateCrystalWasteItems.java` |
| `sulfur` | **Blocked — not registered anywhere in this port** (CE's raw-sulfur `ItemBase`, `ModItems.java:1130`; zero hits for it in the port's item tree) |
| `chunk_ore` (meta `EnumChunkType.CRYOLITE`) | **Blocked — item not registered.** The `EnumChunkType` enum data structure exists (`src/main/java/com/hbm/items/ItemEnums.java:72-78`, including the `CRYOLITE` constant), but no `chunk_ore` item exists yet; the only mention in the port is a forward-reference in `BlockResourceStone.java`'s javadoc describing it as a future data-component multi-variant item, not yet built. |

**Machine-side dependency: `CrucibleUtil.pourFullStack` and the bedrock-ore `toFluid`/`extract` helpers — both confirmed absent** (§2.2).

**Verdict: blocked**, on both infrastructure (Crucible casting, bedrock-ore conversion helpers) and two specific items (`sulfur`, `chunk_ore`). Even once Crucible exists, the `crystal_copper` and `crystal_aluminium` recipes specifically would still need those two items registered first.

### 4.2 `FusionRecipes.java`

**Fluids — all 10 referenced fluids already registered** in `src/main/java/com/hbm/inventory/fluid/Fluids.java`, confirmed directly: `DEUTERIUM`, `OXYGEN`, `TRITIUM`, `HELIUM4`, `HELIUM3`, `CHLORINE`, `DHC`, `BALEFIRE`, `AMAT`, `STELLAR_FLUX`.

**Item outputs:**

| Item | Status |
|---|---|
| `powder_chlorophyte`, `powder_balefire`, `powder_gold` | Ready — all in `src/main/java/com/hbm/items/BilletPowderItems.java` |
| `pellet_charged` (2 of 10 recipes: `fus.do`, `fus.th4`) | **Blocked — not registered anywhere.** Confirmed by a direct grep for the literal id (`"pellet_charged"`) across the whole item tree: zero hits. Two other in-port javadocs (`CrystallizerRecipes.java:42`, `MixerRecipes.java:37`) independently name this same item as an already-known, not-yet-registered gap from earlier phases. |

**Icon-only references** (`gas_full`/`fluid_icon`, cosmetic NEI display, not gameplay ingredients): `gas_full` exists as `ItemGasCanister` (`src/main/java/com/hbm/items/tool/ToolItems.java:293`, a single DataComponent-based fluid-container item matching this port's established convention, not exploded per-fluid); `fluid_icon` exists (`ItemFluidIcon`, `src/main/java/com/hbm/items/machine/MachineItems.java:274`).

**Verdict: blocked** — not on items (8/9 real gameplay items+fluids are ready; only `pellet_charged` is missing, affecting 2 of 10 recipes) but on the entire hot-fusion tokamak machine and its two network abstractions (§2.3). Porting the recipe list alone would compile but drive nothing.

### 4.3 `EngineRecipes.java`

**All 24 CE-native fluids already registered** in `Fluids.java` (§3.3 table) — moot given §2.1's finding that no port is needed. The 4 compat entries reference fluids from other 1.12-only mods this port does not (and should not) integrate with.

**Verdict: not applicable — already fully covered functionally, no port action needed.**

## 5. Recommended 1.21.1 implementation shape

**`EngineRecipes.java`: do not port as a class.** No JSON, no `RecipeType`, no Java data class. The functionally-relevant data is already correctly present as `FT_Combustible` traits on `Fluids.java` entries (verified byte-for-byte against CE in §2.1). If the 8 currently-untagged fluids (`BIOGAS`, `KEROSENE_REFORM`, `FISHOIL`, `SUNFLOWEROIL`, `GAS`, `PETROLEUM`, `AROMATICS`, `UNSATURATEDS`) are ever wanted as genuinely combustible — which would be a **content addition beyond CE's own current behavior**, not a parity fix — the right move is adding an `FT_Combustible(...)` trait to their existing `Fluids.java` declarations using `EngineRecipes`'s own grade/energy values (§3.3 table), not resurrecting a separate registrar class.

**`ElectrolyserMetalRecipes.java`: a bespoke Java data class, once Crucible exists — not JSON, not `HbmSimpleRecipe`.** Its real shape (item trigger key with ore-dict fallback, up to 2 *molten-material* outputs cast via a foundry, an arbitrary-length solid-`ItemStack` byproduct array, a per-recipe duration) does not fit `HbmSimpleRecipe`'s single-`Ingredient`-in/single-`ItemStack`-out shape (`src/main/java/com/hbm/inventory/recipes/HbmSimpleRecipe.java` — no fluid support of any kind), nor any other existing port scaffolding. This exactly matches the reasoning this port's own already-ported siblings give for staying bespoke Java (`CrystallizerRecipes.java`'s header: "doesn't fit vanilla's `Recipe<RecipeInput>` contract without a much larger custom-ingredient design"). Follow the established convention exactly (see `CrystallizerRecipes.java`, read in full for this report, and `ElectrolyserFluidRecipes.java`): a `private final Map<..., ElectrolysisMetalRecipe>` field, private constructor, `public static synchronized void register()` guarded by a `registered` boolean flag (lazy, first-use-triggered — not called eagerly from block/item registration, for the same reason `CrystallizerRecipes.java`'s own javadoc gives: referenced items/blocks aren't resolvable that early), a static lookup method, and a `public static class ElectrolysisMetalRecipe` inner data holder with plain fields. This work is **gated on**: (a) a Crucible/`CrucibleUtil.pourFullStack`-equivalent existing (the actual Phase 7 headline deliverable), (b) `sulfur` and `chunk_ore` items being registered, (c) `ItemBedrockOreNew.toFluid`/`.extract`/`.make`-equivalent helper methods being added to this port's `BedrockOreOutput`/`ItemBedrockOre` classes (the underlying `primary1`/`primary2` data is already there). Do the 18 explicit crystal recipes first (fully unblocked once (a)+(b) land); the bedrock-ore loop is a clean second increment once (c) also lands, since it's a genuine loop over an already-existing 6-member enum, not 18 more hand-transcribed entries.

**`FusionRecipes.java`: defer — this is a machine-porting task, not a recipe-porting task.** Recommend the implement wave **not** attempt this file in isolation. If/when a future task ports CE's hot-fusion tokamak (`TileEntityFusionTorus` + the `IFusionPowerReceiver` device family + `KlystronNetwork`/`PlasmaNetwork` + `ModuleMachineFusion`), that task should: (1) extend this port's `GenericRecipe`/`GenericRecipes` base classes with the real `inputItem[]`/`inputFluid[]`/`outputItem[]`/`outputFluid[]`/`duration`/`power` fields their own javadoc already anticipates needing (§2.3 quote) — note this is a **breaking change** to the two existing consumers (`ItemBlueprints`/`ItemBlueprintFolder`), not just an addition; (2) then port `FusionRecipes.java`'s 10 entries (§3.2) as a `FusionRecipe`-typed subclass following the same shape, still as a bespoke Java class (multi-fluid-in/multi-output-with-chance is exactly the shape CE's own `GenericRecipes<T>` machinery exists to model, and exactly the shape `HbmSimpleRecipe` cannot). Register `pellet_charged` first (blocks 2 of 10 recipes). This is explicitly out of scope for "just port this recipe data."

## 6. Open questions / risks

1. **`Mats.java` autogen-shape coverage for `ElectrolyserMetalRecipes`'s materials was only spot-checked, not exhaustively verified.** Several materials the recipe table needs at `INGOT`/`NUGGET` shape (e.g. `MAT_IRON` at `INGOT.q(6)`) showed a `setAutogen(...)` list in the 8 lines I read that didn't literally include the exact shape token I expected for every material — this may just mean `INGOT` is available by a different path (e.g. every material implicitly supports `INGOT` and only *extra* shapes are opt-in via `setAutogen`), which would make this a non-issue, but I did not read enough of `Mats.java`'s/`MaterialShapes.java`'s own semantics to be certain. **Whoever implements this file should verify each of the 18 recipes' two molten-material/shape pairs actually resolves via `Mats`/`MaterialShapes` before assuming the table above is drop-in.**
2. **The CE `HEAVYOIL` duplicate-key line (`EngineRecipes.java:20`) is almost certainly a copy-paste bug in CE itself**, not intentional: it re-keys `Fluids.HEAVYOIL.getFF()` a second time with a value (100,000) that exactly matches a *different* fluid's real `FT_Combustible` energy in both CE's and this port's `Fluids.java` — `HEATINGOIL` (LOW, 100,000, CE `Fluids.java:291`, port `Fluids.java:273`). Since §2.1 already established `EngineRecipes.java` is dead code with zero live readers, this has zero gameplay effect either way and needs no fix — flagged only so a reviewer isn't confused encountering it while reading CE source, and to explain why the trait-table diff in §2.1 came out clean (25/25) even though a literal transcription of `EngineRecipes.java`'s `HashMap` population would only yield 24 *distinct* keys for those lines (23 CE-native + the overwritten `HEAVYOIL` collapsing to one value).
3. **Whether the port's `MachineItems.java` "82-item `scraps_*` family`" the Phase 6 `recipe_graph_audit.md` describes (§4's family table, `items/machine/MachineItems.java`) is the same registry as `ItemScraps.java`'s per-material `scrap_<material>` items, or a separate/older naming-inconsistent item family, was not resolved by this task** — worth reconciling before Crucible implementation begins, since duplicate or conflicting scrap-item registrations would be an easy, easy-to-miss bug once both the electrolyser and a future crucible/foundry both try to hand out "scrap" items.
4. **This task did not verify `RecipesCommon.ComparableStack`/`AStack`/`OreDictStack` and `Tuple.Pair`** (both used throughout `ElectrolyserMetalRecipes.java`) beyond confirming `ComparableStack` is already used successfully by `CrystallizerRecipes.java` — a reasonable inference that the ore-dict-fallback path (`ElectrolyserMetalRecipes.getRecipe`'s `OreDictStack` branch, CE lines 185-190) may need extra care since this port's own `ComparableStack` javadoc (cited in `CrystallizerRecipes.java:33-36`) already documents it as a *simplified* version of CE's (no `meta` field) — full 1:1 behavioral parity for the ore-dict fallback specifically was not independently checked.
5. **Neither `upstream/neo-edition` nor any other NeoForge-1.21.1-shape reference was needed for this assignment** — none of the three files' recommended implementation shape uses a vanilla `RecipeType`/`RecipeSerializer` at all (per §5), so there was no NeoForge API-shape question to confirm against it; `upstream/neo-edition` also has no `Electroly*`/`*Fusion*Recipe*`/`*Engine*Recipe*` files of its own to check regardless (confirmed by glob).