
# Research report — mrec-11-centrifuge-misc (CentrifugeRecipes / CokerRecipes / SuperComputerRecipes / RadiolysisRecipes)

Scope: CE's `com.hbm.inventory.recipes.{CentrifugeRecipes,CokerRecipes,SuperComputerRecipes,RadiolysisRecipes}` (four per-machine recipe-data classes under `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`, NOT the sibling `com.hbm.crafting.*` classes). This is a **research-only** deliverable — no implementation code was written.

## Scope confirmed

| CE file | Lines | Read in full? |
|---|---:|---|
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CentrifugeRecipes.java` | 373 | Yes |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CokerRecipes.java` | 159 | Yes |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/SuperComputerRecipes.java` | 113 | Yes |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/RadiolysisRecipes.java` | 67 | Yes |

Also read for context (CE): `ItemBedrockOreNew.java` (`BedrockOreType`/`BedrockOreGrade`/`extract()`, lines 240-310), `CrackingRecipes.java` (124 lines, in full — the class `RadiolysisRecipes.registerRadiolysis()` calls into).

Also read for context (port side, `src/main/java/com/hbm`): `inventory/recipes/chem/CentrifugeRecipes.java` (180 lines, in full), `inventory/recipes/RefineryRecipes.java` (152 lines, header + relevant sections), `blockentity/machine/chem/CentrifugeBlockEntity.java` (recipe-consumption call sites), `items/special/{BedrockOreType,BedrockOreGrade,BedrockOreOutput,BedrockOreItems,ItemBedrockOre,ProcessingTrait}.java` (all in full), `items/MaterialItemGenerator.java` (in full), `items/machine/ItemDrive.java` (in full), `items/machine/ItemBlueprintFolder.java` (partial), `inventory/recipes/loader/{GenericRecipe,GenericRecipes}.java` (header/class doc), `inventory/material/Mats.java` (header + spot checks), `items/{BilletPowderItems,IngotNuggetItems,PlateCrystalWasteItems}.java` (grep-driven full-constant extraction), `blocks/OreBlocks.java` (spot checks), `inventory/fluid/Fluids.java` (spot checks, all 39 Coker-relevant fluid types individually confirmed).

**In-CE structure, per file:**

1. **`CentrifugeRecipes.java`** — a flat `HashMap<AStack, ItemStack[]>` (`recipes`), populated by `registerDefaults()` as **77 literal `recipes.put(` source lines**, of which:
   - **57 are unconditional single literal entries** (one `OreDictStack`/`ComparableStack` key → up to 4 `ItemStack` outputs each), covering vanilla/CE ore-washing (19 ores/blocks), one certus-quartz AE2-conditional entry, `BLAZE_ROD`, `ingot_schraranium`, and **26 crystal-breakdown recipes** (`crystal_coal` … `crystal_cobalt`).
   - **16 lines sit inside a `for(BedrockOreType type : BedrockOreType.VALUES)` loop** (lines 220-241) — CE's `BedrockOreType` enum has **6 members**, so this loop generates **16 × 6 = 96 real recipe entries** at runtime (not 16).
   - **1 line is gated by a runtime `OreDictionary.getOres("crystalCertusQuartz")` non-empty check** (AE2-integration-conditional, lines 243-254) — fires only if Applied Energistics 2 is loaded.
   - **True total: ≈154 recipe entries** (57 + 96 + 1 conditional), not 77 — this is the loop-multiplication caveat `recipe_graph_audit.md` §3.7 already flags generically ("a call site inside a `for` loop... counted once per source line rather than once per runtime iteration").
   - Also has a `registerPost()` IMC-mod-integration hook (out of scope — no CE mods to integrate with) and standard `SerializableRecipe` JSON read/write plumbing (`hbmCentrifuge.json`).

2. **`CokerRecipes.java`** — a flat `HashMap<FluidType, Tuple.Triplet<Integer, ItemStack, FluidStack>>` (`recipes`), keyed by **input fluid type**, each value = `{fuel-quantity-consumed, ItemStack byproduct, FluidStack byproduct}`. `registerDefaults()` makes **33 literal calls**: 24× `registerAuto(fluid, outputType)` (each internally computes a coke-fuel-quantity from the input fluid's combustion/flammability heat value and calls `registerSFAuto`→`registerRecipe`), 1× `registerSFAuto(WOODOIL, ...)` (explicit wood-coal fuel override), 8× `registerRecipe(...)` directly (miscellaneous non-coke-fueled processing: `WATZ`, `REDMUD`, `BITUMEN`, `LUBRICANT`, `CALCIUM_SOLUTION`, `SOURGAS`, `SLOP`, `VITRIOL`). No loops — flat, table-driven-by-hand, one map entry per literal call, **33 total** (confirmed by grep: 38 `register*(` matches minus 3 method declarations minus 2 internal delegation calls = 33).

3. **`SuperComputerRecipes.java`** — extends CE's `GenericRecipes<GenericRecipe>` (a `SerializableRecipe` subclass with item/fluid input/output limits, chance-weighted multi-output). `registerDefaults()` makes **10 top-level calls** that expand to **18 total `GenericRecipe` registrations**: 2× `registerSimulation(...)` (each → 1× `registerTriplet` → 3 sub-recipes = 6 total), 2× `registerTriplet(...)` directly (6 total), 3× `registerCopy(...)` (1 recipe each = 3), 3× standalone `this.register(...)` (`com.blueprints`, `com.beigeprints`, `com.klaus`). Every recipe uses `ChanceOutputMulti`/`ChanceOutput` (percentage-weighted multi-output — e.g. a drive either upgrades to the processed variant at X% or breaks at (100-X)%) and most also gate on a required input fluid (water/PFM-coolant/helium) consumed alongside the item input. This is CE's data-drive/satellite-processing machine.

4. **`RadiolysisRecipes.java`** — a flat `Map<FluidType, Pair<FluidStack, FluidStack>>` (`radiolysis`), **not** a `SerializableRecipe`/JSON-loader subclass (plain class, no CE data-driven-recipe machinery at all). `registerRadiolysis()` puts exactly **1 explicit entry** (`WATER → {80 PEROXIDE, 20 HYDROGEN}`) then calls `CrackingRecipes.getCrackingRecipes()` and `putAll()`s the result — CE's `CrackingRecipes.java` (124 lines, read in full for this task) has **11 literal `cracking.put(` entries** (`OIL`, `BITUMEN`, `SMEAR`, `GAS`, `DIESEL`, `DIESEL_CRACK`, `KEROSENE`, `WOODOIL`, `XYLENE`, `HEATINGOIL_VACUUM`, `REFORMATE`, `BIOGAS` — 12 keys, but `DIESEL` and `DIESEL_CRACK` map to the identical output pair so effectively 11 distinct output-pair definitions across 12 `.put()` calls plus one more parameterized `registerCracking`-style helper at line 113 that this count already includes). **True total for `RadiolysisRecipes`: 1 + 11 = 12 recipe entries**, but this is a **hard cross-class dependency**: `RadiolysisRecipes` cannot be ported without `CrackingRecipes` (CE's own code even `throw`s `IllegalStateException` if the cracking map is empty at radiolysis-registration time — "load order is broken" guard). `CrackingRecipes.java` is a fifth CE file this task's assignment does not name but that a correct port of `RadiolysisRecipes` structurally requires; flagging it here rather than silently absorbing it.

## Already covered by this port

**Only `CentrifugeRecipes` has partial coverage; the other three have zero.**

### CentrifugeRecipes — port has `src/main/java/com/hbm/inventory/recipes/chem/CentrifugeRecipes.java` (180 lines, read in full)

Port-side class is a `LinkedHashMap<AStack, ItemStack[]>` (`RECIPES`), wired live into `blockentity/machine/chem/CentrifugeBlockEntity.java` (confirmed: `CentrifugeRecipes.getOutput(itemStack)` is called at lines 83/119/130 for slot-validity checks and process execution) — **the machine itself is fully built and already recipe-data-driven**; closing this gap is a pure recipe-data-addition task, no block/block-entity/GUI/menu work required.

**Port currently has exactly 20 entries** (10 ore-washing + 10 crystal-breakdown), matching the task prompt's "~20" estimate. Line-by-line diff against CE's 154:

| Port entry | CE match? | Notes |
|---|---|---|
| `ores/iron` → 3×POWDER_IRON+GRAVEL | ✅ exact | matches CE `IRON.ore()` |
| `ores/gold` → 3×POWDER_GOLD+GRAVEL | ✅ exact (CE non-LBS-config default) | CE's `GOLD.ore()` has an `enableLBSMSimpleCentrifuge` config branch this port correctly omits (defaults off) |
| `ores/copper` → 2×POWDER_COPPER+POWDER_GOLD+GRAVEL | ✅ exact | matches CE `CU.ore()` non-LBS |
| `ores/lead` → 2×POWDER_LEAD+POWDER_GOLD+GRAVEL | ✅ exact | matches CE `PB.ore()` non-LBS |
| `ores/diamond` → 3×POWDER_DIAMOND+GRAVEL | ✅ exact | matches CE `DIAMOND.ore()` |
| `ores/emerald` → 3×POWDER_EMERALD+GRAVEL | ✅ exact | matches CE `EMERALD.ore()` |
| `ores/uranium` → 2×POWDER_URANIUM+NUGGET_RA226+GRAVEL | ✅ exact (CE non-LBS default) | matches CE `U.ore()` non-LBS |
| `ores/lapis` → 6×POWDER_LAPIS+POWDER_COBALT_TINY+GEM_SODALITE+GRAVEL | ✅ exact | matches CE `LAPIS.ore()` |
| `ores/redstone` → 2×REDSTONE(3)+**POWDER_COBALT**+GRAVEL | ⚠️ **deviation** | CE's `REDSTONE.ore()` 3rd output is `ingot_mercury` (1, non-LBS), **not** `powder_cobalt`. This port's `NUGGET_MERCURY` (confirmed registered, `IngotNuggetItems.java:259`) is the correct CE-equivalent item and was available — this looks like an unexplained substitution, not a documented one (no comment in the port file explains it). **Flag for correction**, not just addition. |
| `ores/coal` → 3×POWDER_COAL+GRAVEL | ✅ exact | matches CE `COAL.ore()` |
| `crystal_coal` → 3×3×POWDER_COAL+POWDER_LITHIUM_TINY | ✅ exact | |
| `crystal_iron` → 2×POWDER_IRON+POWDER_TITANIUM+POWDER_LITHIUM_TINY | ✅ exact | |
| `crystal_gold` → 2×POWDER_GOLD+POWDER_LITHIUM_TINY (**3 outputs**) | ⚠️ **incomplete** | CE has a **4th output**, `ingot_mercury×1` — dropped entirely (not substituted, just missing). `NUGGET_MERCURY` was available. |
| `crystal_redstone` → 3×3×REDSTONE (**3 outputs**) | ⚠️ **incomplete** | CE has a **4th output**, `ingot_mercury×3` — dropped entirely, same as above. |
| `crystal_lapis` → 4×POWDER_LAPIS+POWDER_COBALT+2×GEM_SODALITE | ✅ exact | |
| `crystal_diamond` → 4×POWDER_DIAMOND | ✅ exact | |
| `crystal_uranium` → 2×POWDER_URANIUM+2×NUGGET_RA226+POWDER_LITHIUM_TINY | ✅ exact | |
| `crystal_copper` → 2×POWDER_COPPER+**CRYSTAL_SULFUR**+POWDER_COBALT_TINY | ⚠️ **substitution** | CE uses plain `ModItems.sulfur` (1); port substitutes `PlateCrystalWasteItems.CRYSTAL_SULFUR` because plain "sulfur" doesn't exist yet in this port (confirmed, see Item dependency check below) — **this substitution is undocumented in the port file** (no comment), unlike the identical, well-documented precedent in `RefineryRecipes.java`'s own javadoc for the same sulfur gap. Worth a doc comment at minimum; worth reconsidering once plain `sulfur` exists. |
| `crystal_lead` → 2×POWDER_LEAD+POWDER_GOLD+POWDER_LITHIUM_TINY | ✅ exact | |
| `crystal_niter` → POWDER_LITHIUM_TINY only (**1 output**) | ⚠️ **heavily incomplete** | CE has **4 outputs**: 3×`niter`, 3×`niter`, 3×`niter`, 1×`powder_lithium_tiny`. Port keeps only the 4th (lithium_tiny) and drops the 3 niter outputs — consistent with plain "niter" not existing yet (confirmed missing, see below), but this reduces a 4-output recipe to a 1-output recipe rather than flagging it as blocked/deferred. |

**Real remaining gap for CentrifugeRecipes: ≈134 of ≈154 CE entries (≈87%) still unported**, not merely "some entries" — specifically:
- **96 bedrock-ore-processing entries** (the entire `BedrockOreType × BedrockOreGrade` loop) — **0 ported**, despite this port already having 100% of the underlying item infrastructure built (see dependency check below) — this is the single largest slice of the gap and is now genuinely "ready to port."
- **15 more crystal-breakdown recipes** not yet touched at all (`crystal_thorium`, `crystal_plutonium`, `crystal_titanium`, `crystal_sulfur`, `crystal_tungsten`, `crystal_aluminium`, `crystal_fluorite`, `crystal_beryllium`, `crystal_schraranium`, `crystal_schrabidium`, `crystal_rare`, `crystal_phosphorus`, `crystal_trixite`, `crystal_lithium`, `crystal_starmetal`, `crystal_cobalt` — that's actually 16; `crystal_niter` above is already partially present) — **all 16 target items already exist** in `PlateCrystalWasteItems.java` (confirmed by full constant list), so this is also low-friction.
- **9 more ore-washing entries** (`LIGNITE`, `TI`(titanium), `NETHERQUARTZ`, `W`(tungsten), `AL`(aluminium), `SA326`(schrabidium), `"oreRareEarth"`, `PU`(plutonium), `TH232`(thorium), `BE`(beryllium), `F`(fluorite), `CO`(cobalt) — 12 actually, some blocked by missing items, see below) not yet ported.
- **6 misc single entries**: `chunk_ore RARE` (blocked, item missing), `ModBlocks.ore_tikite` (item-ready), `ModBlocks.block_euphemium_cluster` (item-ready), `ModBlocks.ore_nether_fire` (item-ready), `ModItems.powder_tektite` breakdown (item-ready), `ModBlocks.block_slag` (blocked, block missing), `ModItems.powder_ash` COAL variant (blocked, item family missing), `Items.BLAZE_ROD` breakdown (item-ready, all vanilla+existing items), `ModItems.ingot_schraranium` breakdown (item-ready).
- The 1 AE2-conditional certus-quartz entry (low priority — depends on a mod this project has no compat plan for).

### CokerRecipes / SuperComputerRecipes / RadiolysisRecipes — confirmed NOT touched by this port at all

Grep evidence:
- No file matching `*Coker*` under `src/main/java/com/hbm` other than `blocks/generic/BlockCoke.java` (an unrelated coke-block class, not the Coker machine or its recipes).
- No file matching `*SuperComputer*` anywhere under `src/main/java/com/hbm`.
- No file matching `*Radiolysis*` anywhere under `src/main/java/com/hbm`.
- No file matching `*Cracking*` anywhere under `src/main/java/com/hbm` (the class `RadiolysisRecipes` structurally depends on — also 0% ported).

Zero recipe data, zero machine block/block-entity/GUI for any of these three.

## Full recipe/entry catalog OR representative pattern

### CentrifugeRecipes — representative catalog (30 entries spanning every sub-pattern)

| # | Input (AStack) | Outputs (ItemStack×qty) | Pattern |
|---|---|---|---|
| 1 | `chunk_ore` meta RARE | 2×powder_cobalt_tiny, 2×powder_boron_tiny, 2×powder_niobium_tiny, 3×nugget_zirconium | single literal, **item-blocked** (chunk_ore missing) |
| 2 | `LIGNITE.ore()` (oredict tag) | 2×powder_lignite ×3, 1×gravel | single literal, ore-washing |
| 3 | `TI.ore()` | 1×powder_titanium ×2 (LBS-cond), 1×powder_iron, 1×gravel | single literal, ore-washing, has LBS config branch |
| 4 | `NETHERQUARTZ.ore()` | 1×powder_quartz ×2, 1×powder_lithium_tiny, 1×netherrack | single literal, non-gravel byproduct |
| 5 | `SA326.ore()` (schrabidium) | 1×powder_schrabidium ×2, 1×nugget_solinium, 1×gravel | single literal, **item-ready** |
| 6 | `"oreRareEarth"` (raw OreDict string) | 1×powder_desh_mix, 2×nugget_zirconium, 1×gravel | single literal, **tag-mapping unclear** (no NeoForge common-tag equivalent) |
| 7 | `PU.ore()` (plutonium) | 2×powder_plutonium, 3×nugget_polonium, 1×gravel | single literal, **item-ready** |
| 8 | `ModBlocks.ore_tikite` | 1×powder_plutonium, 2×powder_cobalt, 2×powder_niobium, 1×end_stone | single literal (block-keyed, not oredict), **item-ready** |
| 9 | `ModBlocks.block_euphemium_cluster` | 7×nugget_euphemium, 4×powder_schrabidium, 2×ingot_starmetal, 2×nugget_solinium | single literal (block-keyed), **item-ready** |
| 10 | `ModBlocks.ore_nether_fire` | 2×blaze_powder(vanilla), 2×powder_fire, 1×ingot_phosphorus, 1×netherrack | single literal, **item-ready** |
| 11 | `ModItems.powder_tektite` | 1×powder_meteorite_tiny, 1×powder_paleogenite_tiny, 1×powder_meteorite_tiny, 6×dust | single literal (item-keyed breakdown), **item-ready except plain "dust"** |
| 12 | `ModBlocks.block_slag` | 1×gravel, 1×powder_fire, 1×powder_calcium, 1×dust | single literal, **blocked** (block_slag not registered) |
| 13 | `powder_ash` meta COAL | 2×powder_coal_tiny, 1×powder_boron_tiny, 6×dust_tiny | single literal (metadata-keyed), **blocked** (powder_ash family unregistered) |
| 14 (loop) | `bedrock_ore_new_base_<type>` | `bedrock_ore_new_primary_<type>` + 1×gravel | **loop entry 1/16**, iterated ×6 types |
| 15 (loop) | `bedrock_ore_new_primary_sulfuric_<type>` | 2×`primary_nosulfuric_<type>`, 2×`sulfuric_byproduct_<type>` | **loop entry 4/16** |
| 16 (loop) | `bedrock_ore_new_primary_first_<type>` | 2×`extract(primary1)`, 1×`extract(primary2)`, 1×`crumbs_<type>` | **loop entry 11/16**, 4 outputs |
| 17 (loop) | `bedrock_ore_new_sulfuric_washed_<type>` | `extract(byproductAcid1..3)`, `crumbs_<type>` | **loop entry 14/16** |
| 18 | `Items.BLAZE_ROD` (vanilla) | 2×blaze_powder, 2×powder_fire | single literal, **item-ready** (all-vanilla+existing) |
| 19 | `ModItems.ingot_schraranium` | 3×nugget_schrabidium (2+1), 3×nugget_uranium, 2×nugget_neptunium | single literal, **item-ready** |
| 20 | `crystal_coal` | 3×3×powder_coal, 1×powder_lithium_tiny | crystal-breakdown, **already ported** |
| 21 | `crystal_thorium` | 2×powder_thorium, 1×powder_uranium, 1×nugget_ra226 | crystal-breakdown, **not ported, item-ready** |
| 22 | `crystal_plutonium` | 2×powder_plutonium, 1×powder_polonium, 1×powder_lithium_tiny | crystal-breakdown, **not ported, item-ready** |
| 23 | `crystal_titanium` | 2×powder_titanium, 1×powder_iron, 1×powder_lithium_tiny | crystal-breakdown, **not ported, item-ready** |
| 24 | `crystal_sulfur` | 2×4×sulfur(plain), 1×powder_iron, 1×ingot_mercury | crystal-breakdown, **not ported, blocked (plain sulfur)** |
| 25 | `crystal_niter` | 3×3×niter(plain), 1×powder_lithium_tiny | crystal-breakdown, **partially ported (1/4 outputs), blocked on plain niter for the other 3** |
| 26 | `crystal_tungsten` | 2×powder_tungsten, 1×powder_iron, 1×powder_lithium_tiny | crystal-breakdown, **not ported, item-ready** |
| 27 | `crystal_aluminium` | 3×chunk_ore(CRYOLITE), 1×powder_titanium, 1×powder_iron, 1×powder_lithium_tiny | crystal-breakdown, **not ported, blocked (chunk_ore)** |
| 28 | `crystal_fluorite` | 2×4×fluorite(plain), 2×gem_sodalite, 1×powder_lithium_tiny | crystal-breakdown, **not ported, blocked (plain fluorite)** |
| 29 | `crystal_starmetal` | 3×powder_dura_steel, 3×powder_cobalt, 2×powder_astatine, 5×nugget_mercury | crystal-breakdown, **not ported, item-ready** |
| 30 | `crystal_cobalt` | 2×powder_cobalt, 3×powder_iron, 3×powder_copper, 1×powder_lithium_tiny | crystal-breakdown, **not ported, item-ready** |

**Generating pattern for the bedrock-ore loop (96 of 154 entries)** — precise enough to write mechanically:

```
for (BedrockOreType type : BedrockOreType.VALUES) {           // 6 types, port-side items/special/BedrockOreType.java
    // 16 fixed (inputGrade -> outputGrade[]) transitions, per-type, e.g.:
    put(BedrockOreItems.get(type, BASE),              new[]{ BedrockOreItems.get(type, PRIMARY), gravel(1) });
    put(BedrockOreItems.get(type, BASE_ROASTED),       new[]{ BedrockOreItems.get(type, PRIMARY), gravel(1) });
    put(BedrockOreItems.get(type, BASE_WASHED),        new[]{ BedrockOreItems.get(type, PRIMARY)×2, gravel(1) });
    put(BedrockOreItems.get(type, PRIMARY_SULFURIC),   new[]{ BedrockOreItems.get(type, PRIMARY_NOSULFURIC)×2, BedrockOreItems.get(type, SULFURIC_BYPRODUCT)×2 });
    // ... 12 more fixed transitions, see CE lines 222-240 for the complete literal list (grade names only — no material lookup needed for these 6) ...
    // The remaining transitions call extract(type.primary1/2, byproductAcidN/SolventN/RadN) -> need a
    // materialToItem(BedrockOreOutput o, double mult) helper equivalent to CE's ItemBedrockOreNew.extract(),
    // i.e. resolve o.material() via MaterialItemGenerator's (material, FRAGMENT) pair, amount = ceil(o.amount()*mult).
}
```
This is a **direct** table-driven port (not this port's own `BILLET_SETS`-style table, since CE's transitions are 16 *fixed, non-uniform* grade→grade rules rather than a uniform shape×material cross product) — the loop body itself is the "table," identical in shape to CE's own loop; an implement-wave agent should transcribe CE lines 222-240 into a Java method taking `BedrockOreType type` and calling `BedrockOreItems.get(type, grade)` in place of CE's `ItemBedrockOreNew.make(grade, type)`, and a new `extract(BedrockOreOutput, double)` helper in place of CE's static method (see Open Questions — this helper doesn't exist in the port yet and needs a `(material, shape) -> DeferredItem` lookup that `MaterialItemGenerator` also doesn't currently expose publicly).

### CokerRecipes — full catalog (33 entries; small enough to list, but grouped by pattern for compactness)

**Pattern A — 24 `registerAuto(fluid, outputByproductType)` entries** (all consume the same `coke` item, meta PETROLEUM, quantity auto-computed from the input fluid's `FT_Flammable`/`FT_Combustible` heat trait — CE's own comment: "3200 burntime × 1.25 burntime bonus × 200 TU/t + 20000TU per operation"):

| Input fluid | Byproduct fluid type | Input fluid | Byproduct fluid type |
|---|---|---|---|
| HEAVYOIL | OIL_COKER | DIESEL_CRACK_REFORM | GAS_COKER |
| HEAVYOIL_VACUUM | REFORMATE | LIGHTOIL | GAS_COKER |
| COALCREOSOTE | NAPHTHA_COKER | LIGHTOIL_DS | GAS_COKER |
| SMEAR | OIL_COKER | LIGHTOIL_CRACK | GAS_COKER |
| HEATINGOIL | OIL_COKER | LIGHTOIL_VACUUM | GAS_COKER |
| HEATINGOIL_VACUUM | OIL_COKER | BIOFUEL | GAS_COKER |
| RECLAIMED | NAPHTHA_COKER | AROMATICS | GAS_COKER |
| NAPHTHA | NAPHTHA_COKER | REFORMATE | GAS_COKER |
| NAPHTHA_DS | NAPHTHA_COKER | XYLENE | GAS_COKER |
| NAPHTHA_CRACK | NAPHTHA_COKER | FISHOIL | MERCURY(!) |
| DIESEL | NAPHTHA_COKER | SUNFLOWEROIL | GAS_COKER |
| DIESEL_REFORM | NAPHTHA_COKER | DIESEL_CRACK | GAS_COKER |

**Pattern B — 1 special-fuel override**: `registerSFAuto(WOODOIL, 340_000L, new ItemStack(Items.COAL, 1, 1), GAS_COKER)` — uses vanilla charcoal (`Items.COAL` meta 1) as the coke-fuel stand-in instead of `ModItems.coke`, at a fixed TU-per-charcoal figure rather than the auto-computed one.

**Pattern C — 8 explicit `registerRecipe(fluid, quantity, output, byproduct)` entries** (no coke fuel consumed at all — direct fluid→item conversions):

| Input fluid | Quantity | Item output | Fluid byproduct |
|---|---:|---|---|
| WATZ | 4,000 | 4×ingot_mud | none |
| REDMUD | 450 | 1×iron_ingot(vanilla) | 50 MERCURY |
| BITUMEN | 16,000 | 1×coke(meta PETROLEUM) | 1,600 OIL_COKER |
| LUBRICANT | 12,000 | 1×coke(meta PETROLEUM) | 1,200 OIL_COKER |
| CALCIUM_SOLUTION | 125 | 1×powder_calcium | 100 SPENTSTEAM |
| SOURGAS | 1,000 | 1×sulfur(plain) | 150 GAS_COKER |
| SLOP | 1,000 | 1×powder_limestone | 250 COLLOID |
| VITRIOL | 4,000 | 1×powder_iron | 500 SULFURIC_ACID |

**Generating pattern**: a single `Map<FluidType, {quantity, ItemStack, FluidStack}>` built with 3 helper overloads (`registerAuto`/`registerSFAuto`/`registerRecipe`), all delegating to one `registerRecipe`. This is small and irregular enough (heat-trait-derived quantities, mixed coke/non-coke fuel patterns) that it should be **hand-transcribed as literal calls to the same 3-helper pattern**, not further table-compressed — CE's own file already is the "table."

### SuperComputerRecipes — full catalog (18 entries)

| Recipe name | Input item(s) | Input fluid | Output (chance) | Time (ticks) |
|---|---|---|---|---:|
| com.flightcalc_water | 1×drive(FLASH_EMPTY) | 16,000 WATER→SPENTSTEAM | 95% drive(FLASH_FLIGHTSIM) / 5% drive(FLASH_BROKEN) | 900 |
| com.flightcalc_pfm | 1×drive(FLASH_EMPTY) | 16,000 PFM_COLD→PFM | 50% / 50% | 150 |
| com.flightcalc_helium | 1×drive(FLASH_EMPTY) | 16,000 HELIUM4 (consumed) | 25% / 75% | 60 |
| com.particlecalc_water/pfm/helium | (same shape, FLASH_PARTICLESIM) | " | 95/5, 50/50, 25/75 | 900/150/60 |
| com.processflight_water/pfm/helium | 1×drive(DISK_FLIGHTDATA) | " | 99/95/90 vs DISK_BROKEN | 36000/18000/6000 |
| com.processorbit_water/pfm/helium | 1×drive(DISK_ORBITDATA) | " | 75/65/50 vs DISK_BROKEN | 72000/36000/18000 |
| com.copyflightcalc | 1×drive(FLASH_FLIGHTSIM)+1×drive(FLASH_EMPTY) | none | 95% 2×drive(FLASH_FLIGHTSIM) / 5% 2×drive(FLASH_BROKEN) | 18000 |
| com.copyparticlecalc | (FLASH_PARTICLESIM variant) | none | 95/5 | 18000 |
| com.copyfligthdata | (DISK_FLIGHTDATA_PROCESSED variant) | none | 75/25 | 18000 |
| com.blueprints | 16×paper + 16×(oredict KEY_BLUE) | none | 20% blueprint_folder(meta0) / 80% 16×paper | 18000 |
| com.beigeprints | 24×paper + 24×(oredict CINNABAR.gem()) | none | 10% blueprint_folder(meta1) / 90% 24×paper | 18000 |
| com.klaus | 3×64×drive(DISK_EMPTY) | 1,000,000 WATER (consumed) | 1×drive(KLAUS) | 72000, outputs 1,000 SLOP |

**Generating pattern**: two parameterized helper methods (`registerTriplet` for the water/PFM/helium 3-tier chance-processing shape, `registerCopy` for the duplication shape) called with literal name/time/chance arguments — small enough (18 entries) to hand-transcribe once the target framework exists; no loop-table compression needed.

### RadiolysisRecipes — full catalog (12 entries)

| Input fluid | Output 1 | Output 2 |
|---|---|---|
| WATER | 80 PEROXIDE | 20 HYDROGEN |
| OIL | CRACKOIL (`oil_crack_oil` amount) | PETROLEUM (`oil_crack_petro` amount) |
| BITUMEN | OIL (`bitumen_crack_oil`) | AROMATICS (`bitumen_crack_aroma`) |
| SMEAR | NAPHTHA (`smear_crack_napht`) | PETROLEUM (`smear_crack_petro`) |
| GAS | PETROLEUM (`gas_crack_petro`) | UNSATURATEDS (`gas_crack_unsat`) |
| DIESEL | KEROSENE (`diesel_crack_kero`) | PETROLEUM (`diesel_crack_petro`) |
| DIESEL_CRACK | KEROSENE (same as above) | PETROLEUM (same as above) |
| KEROSENE | PETROLEUM (`kero_crack_petro`) | NONE (0) |
| WOODOIL | HEATINGOIL (`wood_crack_heat`) | AROMATICS (`wood_crack_aroma`) |
| XYLENE | AROMATICS (`xyl_crack_aroma`) | PETROLEUM (`xyl_crack_petro`) |
| HEATINGOIL_VACUUM | HEATINGOIL (80) | REFORMGAS (20) |
| REFORMATE | UNSATURATEDS (40) | REFORMGAS (60) |
| BIOGAS | PETROLEUM (20) | AROMATICS (20) |

(The named `_oil_crack_oil`-style constants are CE's own tunable int fields declared elsewhere in `CrackingRecipes.java`, not read individually here — flagged for the implement wave to pull the literal values from CE's real file at port time.)

**Generating pattern**: `RadiolysisRecipes` itself is trivial (1 literal `put` + a `putAll` of another class's map) — the actual content-bearing table lives in `CrackingRecipes.java`, a small (11-entry) flat map with no loop.

## Item/registry dependency check

### CentrifugeRecipes

**Ready to port now** (item/block dependencies already fully registered, confirmed by direct grep):
- The entire **96-entry bedrock-ore loop** — `BedrockOreType`/`BedrockOreGrade`/`BedrockOreOutput`/`ProcessingTrait`/`ItemBedrockOre`/`ItemBedrockOreBase`/`BedrockOreItems` (`src/main/java/com/hbm/items/special/`) already implement the full 6×26=156-item flattened grid with a `BedrockOreItems.get(type, grade)` lookup equivalent to CE's `ItemBedrockOreNew.make()`. The only missing piece is a small helper equivalent to CE's `extract(BedrockOreOutput, double)` (see Open Questions).
- 16 crystal-breakdown recipes not yet ported (`crystal_thorium`, `crystal_plutonium`, `crystal_titanium`, `crystal_tungsten`, `crystal_beryllium`, `crystal_schraranium`, `crystal_schrabidium`, `crystal_rare`, `crystal_phosphorus`, `crystal_trixite`, `crystal_lithium`, `crystal_starmetal`, `crystal_cobalt`, plus the already-substituted `crystal_copper`/`crystal_niter` needing their real ingredients once those exist) — every referenced `POWDER_*`/`NUGGET_*`/`INGOT_*`/`CRYSTAL_*` constant confirmed present via full grep of `BilletPowderItems.java`/`IngotNuggetItems.java`/`PlateCrystalWasteItems.java` (389 constants extracted; every one of ~32 spot-checked names, including `POWDER_PLUTONIUM`, `NUGGET_EUPHEMIUM`, `INGOT_STARMETAL`, `POWDER_ASTATINE`, `POWDER_DURA_STEEL`, `INGOT_SCHRARANIUM`, was `FOUND`).
- 6 misc single entries: `ore_tikite` (block registered, `blocks/OreBlocks.java:131`), `block_euphemium_cluster` (block registered, `blocks/generic/GenericBlocks.java:169`), `ore_nether_fire` (block registered, `blocks/OreBlocks.java:165`), `powder_tektite` breakdown (`BilletPowderItems.java:237`), `BLAZE_ROD` breakdown (all-vanilla + `POWDER_FIRE`), `ingot_schraranium` breakdown (all ingredients confirmed).
- 2 more ore-washing entries: `SA326.ore()` (schrabidium — `POWDER_SCHRABIDIUM`/`NUGGET_SOLINIUM` both found), `PU.ore()` (plutonium — `POWDER_PLUTONIUM`/`NUGGET_POLONIUM` both found), `CO.ore()` (cobalt — `ore_cobalt` block registered at `blocks/OreBlocks.java:123`, `MAT_COBALT` fully autogen'd).

**Blocked** (need an item/block that doesn't exist yet — name exactly which):
- `chunk_ore` (CE's `ItemEnumMulti<EnumChunkType>`, 4 variants: RARE/MALACHITE/CRYOLITE/MOONSTONE) — **enum exists** (`items/ItemEnums.java:71-77`) **but no item is registered** for it anywhere in the port (confirmed: zero `chunk_ore`/`CHUNK_ORE` registration hits repo-wide). Blocks recipe #1 (`chunk_ore RARE`) and `crystal_aluminium`'s input reference.
- Plain **`sulfur`** item (distinct from `PlateCrystalWasteItems.CRYSTAL_SULFUR`) — confirmed absent (0 hits for `"sulfur"` as a registered item name; `OreBlocks.java` itself documents `ore_sulfur` as a `null`-drop/self-drop-fallback block in its own javadoc, and `RefineryRecipes.java`'s javadoc independently confirms "`ModItems.sulfur`... not yet ported by any Phase 1 items area"). Blocks `crystal_sulfur` and the `SOURGAS` Coker recipe (see below).
- Plain **`niter`** item (distinct from `CRYSTAL_NITER`) — same pattern, confirmed absent, `ore_niter` self-drops. Blocks 3 of `crystal_niter`'s 4 outputs (already partially ported without them).
- Plain **`fluorite`** item (distinct from `CRYSTAL_FLUORITE`) — confirmed absent, `ore_fluorite`-equivalent not found registered with a real drop. Blocks `F.ore()` and `crystal_fluorite`, `crystal_lithium`'s ingredient reference.
- **`dust`/`dust_tiny`** plain generic waste items — confirmed absent (0 hits under any registrar). Blocks `powder_tektite`'s 4th output, `block_slag`'s 4th output, `powder_ash`'s 3rd output.
- **`powder_ash`** (CE's `ItemEnumMulti<EnumAshType>`, ~6 variants: WOOD/COAL/MISC/…) — enum exists, item explicitly **not registered**, per `BilletPowderItems.java`'s own comment: "CE registers powder_ash (ItemEnumMulti<EnumAshType>) here; intentionally excluded, see class javadoc." Blocks the `powder_ash COAL` recipe entirely.
- **`block_slag`** — confirmed **not registered** as a block anywhere (the only repo hits are documentation comments in `blocks/MaterialBlockGenerator.java` explicitly explaining it's excluded — CE's is a multi-variant `BlockMeta` with a distinct "broken" sub-state, flagged there as needing its own port pass). Blocks the `block_slag` breakdown recipe.
- `"oreRareEarth"` — a raw CE `OreDictionary` string with **no confirmed NeoForge tag or block equivalent** in this port (this port's `CentrifugeRecipes.java` uses `OreDictStack.ofCommonTag("ores/*")` only for vanilla-tagged ores; rare-earth has no vanilla common tag). Needs either a custom port-defined tag or direct block-keyed matching once a rare-earth ore block exists.
- Ore-washing entries for `LIGNITE`, `TI`(titanium), `NETHERQUARTZ`, `W`(tungsten), `AL`(aluminium), `TH232`(thorium), `BE`(beryllium) are **not item-blocked** (all output materials confirmed registered) but their **input ore blocks/tags were not individually re-verified in this pass** — `AL.ore()`'s output additionally needs `chunk_ore` (blocked, see above).
- 1 AE2-conditional entry (certus quartz) — low priority, no AE2 compat plan exists in this project.

### CokerRecipes

**Ready to port now**: all **39 distinct `FluidType` constants** referenced (`HEAVYOIL`, `OIL_COKER`, `HEAVYOIL_VACUUM`, `REFORMATE`, `COALCREOSOTE`, `NAPHTHA_COKER`, `SMEAR`, `HEATINGOIL`, `HEATINGOIL_VACUUM`, `RECLAIMED`, `NAPHTHA`, `NAPHTHA_DS`, `NAPHTHA_CRACK`, `DIESEL`, `DIESEL_REFORM`, `DIESEL_CRACK`, `DIESEL_CRACK_REFORM`, `GAS_COKER`, `LIGHTOIL`, `LIGHTOIL_DS`, `LIGHTOIL_CRACK`, `LIGHTOIL_VACUUM`, `BIOFUEL`, `AROMATICS`, `XYLENE`, `FISHOIL`, `MERCURY`, `SUNFLOWEROIL`, `WOODOIL`, `WATZ`, `REDMUD`, `BITUMEN`, `LUBRICANT`, `CALCIUM_SOLUTION`, `SOURGAS`, `SLOP`, `COLLOID`, `VITRIOL`, `SULFURIC_ACID`, `SPENTSTEAM`) **individually confirmed registered** in this port's `inventory/fluid/Fluids.java` — a complete win, this port's fluid parity is genuinely strong here.
- `ingot_mud` (WATZ output) — not individually re-verified in this pass (low risk, plausible name match to existing convention).
- `powder_calcium`, `powder_limestone`, `powder_iron` (Pattern C outputs) — all confirmed present (spot-checked via the same constant extraction used for CentrifugeRecipes).

**Blocked**:
- **`coke`** item (CE's `ItemEnumMulti<EnumCokeType>`, 3 variants COAL/LIGNITE/PETROLEUM) — enum exists (`items/ItemEnums.java:10-16`) but **no item registered** (confirmed: only `blocks/generic/BlockCoke.java`, an unrelated block, exists under any "Coke" name). This blocks **all 24 `registerAuto` + both explicit `BITUMEN`/`LUBRICANT` recipes = 26 of 33 entries** (every recipe whose fuel input or item output is `coke`).
- Plain **`sulfur`** (same gap as CentrifugeRecipes) — blocks the `SOURGAS` recipe's output.
- The **Coker machine itself** (block/block-entity/GUI/menu/`CokerRecipes.java` port-side class) — **0% built**, confirmed via repo-wide grep (only unrelated `BlockCoke` match). This is a from-scratch machine, not just missing recipe data.

### SuperComputerRecipes

**Blocked — the entire class is blocked**, not just individual entries:
- The **`drive` item family** (CE's `ModItems.drive`, `ItemEnumMulti<EnumDriveType>`, 11 variants: `FLASH_EMPTY`, `DISK_EMPTY`, `FLASH_BROKEN`, `DISK_BROKEN`, `FLASH_FLIGHTSIM`, `FLASH_PARTICLESIM`, `DISK_FLIGHTDATA`, `DISK_FLIGHTDATA_PROCESSED`, `DISK_ORBITDATA`, `DISK_ORBITDATA_PROCESSED`, `KLAUS`) is **not registered as an item at all**. This port's `items/machine/ItemDrive.java` is explicitly documented as "Not an item - a bare enum namespace" whose class javadoc says the real item class "should reference `EnumDriveType` from here rather than duplicating it... when it lands in `items.special`" — i.e. this is a known, self-documented, not-yet-done dependency. Every one of the 18 recipes references `ModItems.drive`, so **0 of 18 recipes can be ported** until this item family exists. This also blocks the entity-reachability finding in `recipe_graph_audit.md` — the drive family isn't even in that audit's census, meaning it's missing at the item-registration level, one layer more fundamental than "unreachable."
- **`blueprint_folder`** — this port has `blueprint_folder_base`/`_discover`/`_secret` (`ItemBlueprintFolder.Kind` = `BASE`/`DISCOVER`/`SECRET`, mapped to `GenericRecipes.POOL_PREFIX_{ALT,DISCOVER,SECRET}`), which is **structurally different** from CE's 2-meta `blueprint`/`beigeprint` split the `com.blueprints`/`com.beigeprints` recipes reference — **not confirmed equivalent**, flagged as an open question below rather than asserted blocked or ready.
- The **SuperComputer machine itself** — 0% built (no block/block-entity/GUI/menu, confirmed by repo-wide grep).
- CE's base class `GenericRecipes<GenericRecipe>` (the chance-weighted multi-I/O recipe framework `SuperComputerRecipes` extends) has **no real port-side equivalent** — this port's own `inventory/recipes/loader/GenericRecipes.java` is explicitly documented as "a minimal compile-time stand-in" that intentionally does **not** implement the generic recipe-registry shape, keeping only unrelated blueprint-pool-name bookkeeping, and its javadoc explicitly directs future machine recipes to use vanilla `Recipe<?>`/`RecipeSerializer` scaffolding instead of extending it.

### RadiolysisRecipes

**Blocked**:
- The **Radiolysis machine itself** — 0% built (no block/block-entity/GUI, confirmed by grep).
- **`CrackingRecipes`** — the CE class `RadiolysisRecipes` structurally depends on (`putAll(CrackingRecipes.getCrackingRecipes())`) is **also 0% ported** (confirmed, no `*Cracking*` file anywhere in `src/main/java/com/hbm`) — this is a second, un-named-in-this-assignment CE file that must be ported alongside `RadiolysisRecipes` for the dependency to resolve; CE's own code throws `IllegalStateException` if this map is empty at registration time, so a literal transcription must either inline `CrackingRecipes`'s 11 entries directly or port that class too.

**Ready / not blocked**: all 6 fluid types the `WATER` entry needs (`WATER`, `PEROXIDE`, `HYDROGEN`) and all 9 additional fluid types `CrackingRecipes`'s 11 entries need (`OIL`, `CRACKOIL`, `PETROLEUM`, `BITUMEN`, `AROMATICS`, `SMEAR`, `NAPHTHA`, `GAS`, `UNSATURATEDS`, `DIESEL`, `DIESEL_CRACK`, `KEROSENE`, `WOODOIL`, `HEATINGOIL`, `XYLENE`, `HEATINGOIL_VACUUM`, `REFORMGAS`, `REFORMATE`, `BIOGAS`) are all plausible given this port's confirmed-strong fluid-type parity (97.5% per `PARITY_REPORT.md` §3.3) — not individually re-verified in this pass given the class's small size, but low risk.

## Recommended 1.21.1 implementation shape

- **CentrifugeRecipes**: **not JSON, not a new RecipeType** — this port's existing `com.hbm.inventory.recipes.chem.CentrifugeRecipes` (a plain Java `LinkedHashMap<AStack, ItemStack[]>`, already wired into `CentrifugeBlockEntity`) is the correct, already-proven shape and should simply be **extended in place** with more `RECIPES.put(...)` calls. No machine/block-entity/menu changes needed — confirmed the machine already calls `getOutput()` generically. For the bedrock-ore loop specifically, write it as an actual `for (BedrockOreType type : BedrockOreType.VALUES)` Java loop (matching CE's own shape 1:1, not `ModRecipeProvider`'s `BILLET_SETS`-style declarative table, since the 16 per-type transitions are non-uniform hand-written rules, not a uniform shape×material cross product) inside a new private helper method, e.g. `registerBedrockOreChain()`, called from the class's existing registration entry point.
- **CokerRecipes**: **custom Java data class, not JSON** — same reasoning as this port's own `RefineryRecipes.java` precedent (cited in that file's own javadoc: "a bespoke... recipe shape... CE never made this data-driven/moddable... this stays a literal hardcoded Java registration list rather than inventing a new `Recipe<?>`/datagen shape for a single consumer"). Port `CokerRecipes.java` to `com.hbm.inventory.recipes` (or a `.chem`/`.oil` subpackage matching `RefineryRecipes`'s neighbors) as a `Map<FluidType, {int, ItemStack, FluidStack}>` with the same 3-helper (`registerAuto`/`registerSFAuto`/`registerRecipe`) structure CE uses, once the `coke` item and the Coker machine block/block-entity exist (the machine itself is new work, outside a pure-recipe-data task's scope — flag to the implement wave as "needs a machine, not just data").
- **SuperComputerRecipes**: **needs a genuine custom `RecipeType`/`RecipeSerializer`/machine-recipe-data-class** — this is the one file in this assignment that cannot be a simple ported-Java-map, for two independent reasons CE's own class encodes: (1) **chance-weighted multi-output** (`ChanceOutputMulti`/`ChanceOutput`, e.g. 95%-success/5%-break) has no vanilla `Recipe<?>` equivalent and needs a bespoke data shape (a `List<{ItemStack, int weight}>` output slot, evaluated at craft time); (2) **combined item + fluid input/output with per-recipe limits** (`inputItemLimit()`/`inputFluidLimit()`/`outputItemLimit()`/`outputFluidLimit()`) also has no vanilla equivalent. This port's own `inventory/recipes/loader/GenericRecipes.java` javadoc already anticipates this and explicitly says future machine recipes should build on "the vanilla `Recipe<?>`/`RecipeSerializer` scaffolding (see `com.hbm.inventory.recipes.HbmRecipes`/`HbmSimpleRecipe`)" rather than extend the stand-in class — an implement-wave agent should locate and reuse whatever `HbmRecipes`/`HbmSimpleRecipe` machinery already exists port-side (not read in this pass — flagged as a lead to follow, referenced by name in that javadoc) as the base for a new chance-output-capable recipe type, rather than inventing a fourth pattern from scratch. This is also blocked on the `drive` item family and the SuperComputer machine itself, both 0% built — a full implementation here is a 3-part task (items, machine, recipe-data-shape), not a 1-part recipe-only task the way Centrifuge/Coker are.
- **RadiolysisRecipes**: **custom Java data class, not JSON** — trivially small (12 entries total across both classes), same `Map<FluidType, Pair<FluidStack,FluidStack>>` shape as CE, no chance/multi-input complexity. Port `CrackingRecipes.java` first (or inline its 11 entries directly into `RadiolysisRecipes` if the implement wave judges the separate class isn't worth preserving as its own file) then `RadiolysisRecipes.java` on top. Also needs the Radiolysis machine block/block-entity, 0% built — same "needs a machine, not just data" flag as Coker.

## Open questions / risks

1. **`MaterialItemGenerator.get(material, shape)` lookup does not exist yet.** The class registers all (material, shape) item pairs in a loop but exposes no public accessor to retrieve a specific one by `(NTMMaterial, MaterialShapes)` key afterward (confirmed — the class has no `Map` field, `get()` method, or any lookup surface; `registerAll()` is the only public method besides the class itself). CE's `ItemBedrockOreNew.extract()` (needed for ~48 of the 96 bedrock-loop entries — the "primary"/"crumbs"/"washed" transitions that call `extract(type.primaryN, amount)`) fundamentally needs this lookup. **This is a small, mechanical prerequisite** (add a `Map<NTMMaterial, Map<MaterialShapes, DeferredItem<Item>>>` populated during `registerAll()`, plus a `get(material, shape)` accessor) that blocks porting the "extract"-based half of the bedrock loop specifically — the implement wave should add this to `MaterialItemGenerator` (or find it already added by a sibling Phase 7 task, since other assignments in this same wave may need the identical accessor) before attempting those entries.
2. **`ItemBlueprintFolder`'s `Kind` enum (`BASE`/`DISCOVER`/`SECRET`) vs. CE's `blueprint_folder` meta 0/1 (`blueprint`/`beigeprint`) — not confirmed equivalent.** This task did not read CE's `ItemBlueprintFolder`/`ModItems.blueprint_folder` source in full to determine whether CE's 2 meta values are actually "which recipe pool this folder unlocks" (matching this port's 3-way `Kind` split, just with a different member count) or a genuinely different axis (e.g. a cosmetic/tier distinction unrelated to pool). If they're the same axis, `com.blueprints`/`com.beigeprints`'s outputs might map to `blueprint_folder_discover`/`blueprint_folder_secret` (or similar) rather than needing new items; if not, 2 more items are needed. **Flagged for the implement wave to resolve by reading CE's full class before writing these 2 recipes**, not assumed either way here.
3. **`ModItems.ingot_mud` (Coker's `WATZ` recipe output) was not individually re-verified.** Low risk (plausible existing-convention name) but not confirmed the way the ~40 other constants in this report were.
4. **Ore-block-vs-tag matching strategy is inconsistent across CE's own oredict tags and should be decided once, not per-recipe.** CE mixes `OreDictStack(TAG.ore())` (works for anything with an `OreDictManager` tag, including CE's own custom non-vanilla tags like `TI`/`SA326`/`PU`) and `ComparableStack(ModBlocks.specific_block)` (for one-off blocks with no tag, like `ore_tikite`). This port's ported subset so far only demonstrates the vanilla-common-tag case (`OreDictStack.ofCommonTag("ores/iron")` etc.) — for CE's non-vanilla ore tags (titanium, schrabidium, plutonium, thorium, beryllium, cobalt — all of which map to real port-side `OreBlocks` entries per the dependency check above) the implement wave will need to decide whether to mint new port-specific tags (e.g. `hbm:ores/titanium`) or match directly on `ComparableStack(OreBlocks.ORE_TITANIUM.get())` the way CE does for its untagged blocks. Not a blocker, but a design decision worth making consistently once rather than ad hoc per recipe.
5. **`"oreRareEarth"` has no clear modern equivalent identified in this pass.** CE's `OreDictManager` bundles several distinct materials under one loose oredict string; this port's `Mats.java`-based system doesn't obviously have a matching multi-material tag. Likely needs either a small custom tag spanning whichever "rare earth" ore blocks exist, or should simply be deferred (it's 1 of ~154 entries, low leverage).
6. **CE's `EnumAshType` member count was not individually confirmed** (this task's read of `items/ItemEnums.java` stopped at line 90; the `EnumAshType` block itself, mentioned in a `BilletPowderItems.java` comment as "6 metadata variants: WOOD, COAL, MISC, ..." was not re-verified against CE's real enum for an exact count) — low-impact since `powder_ash` is already established as blocked regardless of its exact variant count.
7. **This report did not verify CE's `EnumChunkType`/`EnumCokeType` values are exhaustive against CE's real source** for the 2 enums reused here (`RARE`/`MALACHITE`/`CRYOLITE`/`MOONSTONE` for chunk, `COAL`/`LIGNITE`/`PETROLEUM` for coke) — both read directly from this port's own `ItemEnums.java`, which its own class javadoc states is "Ported verbatim" from CE, so treated as reliable, but not independently cross-checked against CE's `ItemEnums.java` in this pass.
