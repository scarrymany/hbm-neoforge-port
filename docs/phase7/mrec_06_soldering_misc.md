# mrec-06-soldering-misc — CE machine-recipe registrars: Soldering, Combination, Breeder, Storage Drum

Assignment scope: 4 CE per-machine recipe-data classes under `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/` — `SolderingRecipes.java` (465 ln), `CombinationRecipes.java` (192 ln), `BreederRecipes.java` (125 ln), `StorageDrumRecipes.java` (78 ln). All four read in full. Companion docs `docs/phase6/PARITY_REPORT.md` and `docs/phase6/recipe_graph_audit.md` read in full first, as instructed.

---

## Scope confirmed

| CE file | Lines | Structure |
|---|---:|---|
| `SolderingRecipes.java` | 465 (`wc -l`) | One `registerDefaults()` method: 6 unconditional `recipes.add(new SolderingRecipe(...))` calls (circuit family) + 3 more gated behind `if (no528)` (controller circuits) + 7 unconditional upgrade-tier-1 adds + 10 more gated behind the same `if (no528)` (5× `addFirstUpgrade`, 5× `addSecondUpgrade` — helper methods, not a loop over a table). **Flat list of individually-written static calls, not a loop over a material/shape table.** 26 real `SolderingRecipe` entries under CE's own default config (`GeneralConfig.enable528 = false` by default, confirmed at `upstream/hbm-ce/.../config/GeneralConfig.java:89`, so `no528 = true` and every conditional block runs). Two nested `static class SolderingRecipe` (7-field record-like holder) and a `getRecipe(ItemStack[])`/`getRecipes()` pair plus full `SerializableRecipe` JSON read/write override (CE's own on-disk-override system, not relevant to a 1.21 port). Six input roles per recipe: 3 "toppings" slots (`AStack[]`), 2 "pcb" slots (`AStack[]`), 1 "solder" slot (`AStack[]`), one optional `FluidStack`, one `ItemStack output`, `int duration`, `long consumption`.
| `CombinationRecipes.java` | 192 (`wc -l`) | One `registerDefaults()` method: 23 individually-written `recipes.put(key, new Pair<>(output, fluid))` calls (a `HashMap<Object, Pair<ItemStack,FluidStack>>` keyed by either a `ComparableStack` or a legacy ore-dict `String`) **plus one small `for (BedrockOreType type : BedrockOreType.VALUES)` loop** (6 iterations × 5 `recipes.put(...)` calls per iteration = 30 more entries) at the end of the method. **Mixed: mostly a flat list, with one genuine table-loop tail.** 23 + 30 = 53 total entries. Single-item-key lookup (`getOutput(ItemStack)`), dual optional output (`ItemStack` and/or `FluidStack`), no duration field of its own (the consuming block entity supplies its own heat/time curve, see below).
| `BreederRecipes.java` | 125 (`wc -l`) | One `registerDefaults()` method: 10 calls to a private helper `setRecipe(BreedingRodType in, BreedingRodType out, int flux)` (each expands to 3 `recipes.put(...)` calls — single/`rod_dual`/`rod_quad`, flux ×1/×2/×3) **= a genuine small table-via-helper pattern**, plus 1 more standalone `recipes.put(...)` for `meteorite_sword_etched → meteorite_sword_bred`. 10×3 + 1 = 31 real recipe entries in CE. Single-item-key lookup (`getOutput(ItemStack)`), single `ItemStack output` + `int flux` (a resource cost checked against the reactor's per-tick flux income, not a tick-duration timer).
| `StorageDrumRecipes.java` | 78 (`wc -l`) | One `registerRecipes()` method: `for (int i = 0; i < ItemWasteLong.WasteClass.VALUES.length; i++)` (5 iterations × 2 `addRecipe(...)` calls — normal + `_tiny`) + `for (int i = 0; i < ItemWasteShort.WasteClass.VALUES.length; i++)` (8 iterations × 2 calls) + 2 standalone `addRecipe(...)` calls at the end (au198 ingot/nugget → mercury). **A genuine loop-over-enum-table pattern for 26 of its 28 entries, plus 2 hand-written.** 5×2 + 8×2 + 2 = 28 total entries. Single-item-key lookup (`getOutput`/`getWaste`), output `ItemStack` + `int[]{chance, wasteLiquid, wasteGas}` — consulted by a **passive per-tick random-chance decay** loop in the consuming block entity's `update()`, not a player-invoked craft at all (see below).

---

## Already covered by this port

**`BreederRecipes` — the prompt's "PARTIAL COVERAGE" framing does not hold up: this port already has full, exact coverage of everything in CE's file that is currently portable.** Diffed directly against CE's real source, not assumed:

- Port-side: `src/main/java/com/hbm/inventory/recipes/machine/BreederRecipe.java` (127 ln, JSON `Recipe<SingleRecipeInput>` + Codec/StreamCodec) + `.../machine/BreederRecipes.java` (43 ln, `RecipeType`/`RecipeSerializer` registration) + 30 JSON files under `src/main/resources/data/hbm/recipe/breeder/` (`{co_co60,lithium_tritium,np237_pu238,pu238_pu239,ra226_ac227,rgp_waste,th232_thf,u235_np237,u238_rgp,uranium_rgp} × {single,dual,quad}`).
- Verified every one of the 30 JSON files individually against CE's `setRecipe(...)` table (`BreederRecipes.java:33-42`): all 10 base transmutation pairs present (LITHIUM→TRITIUM, CO→CO60, RA226→AC227, TH232→THF, U235→NP237, NP237→PU238, PU238→PU239, U238→RGP, URANIUM→RGP, RGP→WASTE), each with `single`/`dual`/`quad` variants and the exact CE flux values (e.g. `co_co60_single.json`: `flux: 100`; `co_co60_dual.json`: `flux: 200` = CE's `flux*2`; quad would be `flux*3=300`). Item ids match this port's already-flattened `hbm:rod_<type>` / `hbm:rod_dual_<type>` / `hbm:rod_quad_<type>` convention (`ItemBreedingRod.java:34-54`'s `BreedingRodType` enum has all 15 members CE has, including `LITHIUM`/`TRITIUM`/`CO`/`CO60`/`RA226`/`AC227`/`TH232`/`THF`/`U235`/`NP237`/`U238`/`PU238`/`PU239`/`RGP`/`WASTE`/`URANIUM`).
- **The real remaining gap is exactly 1 CE entry**, not the "~30 vs some larger CE total" the prompt anticipated: `meteorite_sword_etched → meteorite_sword_bred` (1000 flux, `BreederRecipes.java:44`). Confirmed by grep: neither `meteorite_sword_etched` nor `meteorite_sword_bred` exists anywhere in this port's `src/main/java/com/hbm` (0 hits) — this port's own `BreederRecipe.java:38-40` javadoc already documents this exact omission and its exact reason ("neither item exists anywhere in this port yet... a genuinely out-of-scope weapon/tool item, not a data-porting gap").
- **Verdict for the implement wave: no action needed on `BreederRecipes` unless/until `meteorite_sword_etched`/`meteorite_sword_bred` are registered by a weapons-area task** — at which point porting the 31st recipe is a 1-file JSON addition following the exact pattern of the 30 that already exist. Nothing else to do here.

**`SolderingRecipes`, `CombinationRecipes`, `StorageDrumRecipes` — confirmed NOT touched by this port at all, on both axes (recipe data and machine):**

- Recipe data: `grep -rn "SolderingRecipes\|CombinationRecipes\|StorageDrumRecipes" src/main` → 0 hits anywhere in `src/main/java` or `src/main/resources`.
- Machine block/block-entity: `grep -rli "solder\|combination\|storagedrum\|storage_drum\|furnacecombination"` across `src/main/java/com/hbm` → 0 hits (the 2 false-positive "combination" hits were English-word occurrences in unrelated stair/slab-block javadoc comments, not this machine). **None of the three machines — Soldering Station, Combination Furnace, Storage Drum — exist in this port at all, block or block-entity.** This means the implement wave for these three is not "add recipe JSON to an existing machine" but "build the machine block-entity too," a materially bigger task than Breeder's was.

---

## Full recipe/entry catalog

All four files are well under the ~150-entry "small" threshold (26 / 53 / 31 / 28 real entries), so every entry is cataloged below.

### SolderingRecipes (26 entries, CE `upstream/hbm-ce/.../SolderingRecipes.java`)

Quantities shown are the non-LBSM (`GeneralConfig.enableLBSM=false`) default path; where CE's ternary picks a different quantity under LBSM, the LBSM value is noted in parens. Both `no528` blocks run by default (`enable528=false`).

| # | Output | Dur. | Consumption | Fluid | Toppings (×3 slots) | PCB (×2 slots) | Solder (×1 slot) | CE line |
|---:|---|---:|---:|---|---|---|---|---:|
| 1 | `circuit:ANALOG` | 100 | 100 | — | `circuit:VACUUM_TUBE`×3, `circuit:CAPACITOR`×2 | `circuit:PCB`×4 | `PB.wireFine()`×4 | 36-46 |
| 2 | `circuit:BASIC` | 200 | 250 | — | `circuit:CHIP`×4 | `circuit:PCB`×4 | `PB.wireFine()`×4 | 48-55 |
| 3 | `circuit:ADVANCED` | 300 | 1,000 | Sulfuric Acid 1000mB | `circuit:CHIP`×4(16), `circuit:CAPACITOR`×4 | `circuit:PCB`×8, `RUBBER.ingot()`×2 | `PB.wireFine()`×8 | 57-71 |
| 4 | `circuit:CAPACITOR_BOARD` | 200 | 300 | Peroxide 250mB | `circuit:CAPACITOR_TANTALIUM`×3 | `circuit:PCB`×1 | `PB.wireFine()`×3 | 73-82 |
| 5 | `circuit:BISMOID` | 400 | 10,000 | Solvent 1000mB | `circuit:CHIP_BISMOID`×4, `circuit:CHIP`×4(16), `circuit:CAPACITOR`×8(24) | `circuit:PCB`×12, `ANY_HARDPLASTIC.ingot()`×2 | `PB.wireFine()`×12 | 84-100 |
| 6 | `circuit:QUANTUM` | 400 | 100,000 | Helium-4 1000mB | `circuit:CHIP_QUANTUM`×4, `circuit:CHIP_BISMOID`×4(16), `circuit:ATOMIC_CLOCK`×1(4) | `circuit:PCB`×16, `ANY_HARDPLASTIC.ingot()`×4 | `PB.wireFine()`×16 | 102-119 |
| 7 | `circuit:CONTROLLER` (gated `no528`) | 400 | 15,000 | Perfluoromethyl 1000mB | `circuit:CHIP`×8(32), `circuit:CAPACITOR`×8(32), `circuit:CAPACITOR_TANTALIUM`×8(16) | `circuit:CONTROLLER_CHASSIS`×1, `upgrade_speed_1`×1 | `PB.wireFine()`×16 | 128-146 |
| 8 | `circuit:CONTROLLER_ADVANCED` (gated `no528`) | 600 | 25,000 | Perfluoromethyl 4000mB | `circuit:CHIP_BISMOID`×8(16), `circuit:CAPACITOR_TANTALIUM`×16(48), `circuit:ATOMIC_CLOCK`×1 | `circuit:CONTROLLER_CHASSIS`×1, `upgrade_speed_3`×1 | `PB.wireFine()`×24 | 147-165 |
| 9 | `circuit:CONTROLLER_QUANTUM` (gated `no528`) | 600 | 250,000 | Perfluoromethyl-Cold 6000mB | `circuit:CHIP_QUANTUM`×8(16), `circuit:CHIP_BISMOID`×16(48), `circuit:ATOMIC_CLOCK`×1(8) | `circuit:CONTROLLER_ADVANCED`×2, `upgrade_overdrive_1`×1 | `PB.wireFine()`×32 | 166-185 |
| 10 | `upgrade_speed_1` | 200 | 1,000 | — | `circuit:VACUUM_TUBE`×4, `circuit:CAPACITOR`×1 | `upgrade_template`×1, `MINGRADE.dust()`×4 | — | 192-204 |
| 11 | `upgrade_effect_1` | 200 | 1,000 | — | `circuit:VACUUM_TUBE`×4, `circuit:CAPACITOR`×1 | `upgrade_template`×1, `EMERALD.dust()`×4 | — | 205-217 |
| 12 | `upgrade_power_1` | 200 | 1,000 | — | `circuit:VACUUM_TUBE`×4, `circuit:CAPACITOR`×1 | `upgrade_template`×1, `GOLD.dust()`×4 | — | 218-230 |
| 13 | `upgrade_fortune_1` | 200 | 1,000 | — | `circuit:VACUUM_TUBE`×4, `circuit:CAPACITOR`×1 | `upgrade_template`×1, `NB.dust()`×4 | — | 231-243 |
| 14 | `upgrade_afterburn_1` | 200 | 1,000 | — | `circuit:VACUUM_TUBE`×4, `circuit:CAPACITOR`×1 | `upgrade_template`×1, `W.dust()`×4 | — | 244-256 |
| 15 | `upgrade_radius` | 200 | 1,000 | — | `circuit:CHIP`×4, `circuit:CAPACITOR`×4 | `upgrade_template`×1, `"dustGlowstone"`×4 | — | 257-269 |
| 16 | `upgrade_health` | 200 | 1,000 | — | `circuit:CHIP`×4, `circuit:CAPACITOR`×4 | `upgrade_template`×1, `LI.dust()`×4 | — | 270-282 |
| 17-21 | `upgrade_{speed,effect,power,fortune,afterburn}_2` (`addFirstUpgrade`, gated `no528`) | 300 | 10,000 | — | `circuit:CHIP`×8(4), `circuit:CAPACITOR`×4(2) | tier-1 item ×1, `ANY_PLASTIC.ingot()`×4 | — | 285-293, 298-312 |
| 22-26 | `upgrade_{speed,effect,power,fortune,afterburn}_3` (`addSecondUpgrade`, gated `no528`) | 400 | 25,000 | Solvent 500mB | `circuit:CHIP`×16(6), `circuit:CAPACITOR`×16(4) | tier-2 item ×1, `RUBBER.ingot()`×4 | — | 286-294, 314-329 |

### CombinationRecipes (53 entries: 23 individual + 30 loop-generated)

**Individual entries (`registerDefaults()` lines 86-121):**

| # | Input | Output item | Output fluid | CE line |
|---:|---|---|---|---:|
| 1 | `COAL.gem()` | `coke:COAL` | Coalcreosote 100mB | 86 |
| 2 | `COAL.dust()` | `coke:COAL` | Coalcreosote 100mB | 87 |
| 3 | `briquette:COAL` | `coke:COAL` | Coalcreosote 150mB | 88-89 |
| 4 | `LIGNITE.gem()` | `coke:LIGNITE` | Coalcreosote 50mB | 91 |
| 5 | `LIGNITE.dust()` | `coke:LIGNITE` | Coalcreosote 50mB | 92 |
| 6 | `briquette:LIGNITE` | `coke:LIGNITE` | Coalcreosote 100mB | 93-94 |
| 7 | `CHLOROCALCITE.dust()` | `powder_calcium` | Chlorine 250mB | 96 |
| 8 | `MOLYSITE.dust()` | vanilla `iron_ingot` | Chlorine 250mB | 97 |
| 9 | `CINNABAR.crystal()` | `sulfur` | Mercury 100mB | 98 |
| 10 | vanilla `glowstone_dust` | `sulfur` | Chlorine 100mB | 99 |
| 11 | `SODALITE.gem()` | `powder_sodium` | Chlorine 100mB | 100 |
| 12 | `chunk_ore:CRYOLITE` | `powder_aluminium` | Lye 150mB | 101-102 |
| 13 | `NA.dust()` | — (none) | Sodium 100mB | 103 |
| 14 | `LIMESTONE.dust()` | `powder_calcium` | Carbon Dioxide 50mB | 104 |
| 15 | `KEY_LOG` (any log, oredict wildcard) | vanilla `coal` meta 1 (= charcoal) | Wood Oil 250mB | 106 |
| 16 | `KEY_SAPLING` (any sapling) | `powder_ash:WOOD` | Wood Oil 50mB | 107 |
| 17 | `briquette:WOOD` | vanilla charcoal | Wood Oil 500mB | 108-109 |
| 18 | `oil_tar:CRUDE` | `coke:PETROLEUM` | — | 111-112 |
| 19 | `oil_tar:CRACK` | `coke:PETROLEUM` | — | 113-114 |
| 20 | `oil_tar:COAL` | `coke:COAL` | — | 115-116 |
| 21 | `oil_tar:WOOD` | `coke:COAL` | — | 117-118 |
| 22 | vanilla `reeds` (sugar cane) | vanilla `sugar`×2 | Ethanol 50mB | 120 |
| 23 | vanilla `clay` (block) | vanilla `brick_block` | — | 121 |

**Loop-generated entries (`for (BedrockOreType type : VALUES)`, lines 123-134 — 6 types × 5 grade-pairs = 30 entries, all fluid output = Vitriol 50mB, no item side-effect beyond the grade transition):**

| Grade transition | Applies to all 6 `BedrockOreType` values (`LIGHT_METAL`,`HEAVY_METAL`,`RARE_EARTH`,`ACTINIDE`,`NON_METAL`,`CRYSTALLINE`) |
|---|---|
| `BASE` → `BASE_ROASTED` | ×6 |
| `PRIMARY` → `PRIMARY_ROASTED` | ×6 |
| `SULFURIC_BYPRODUCT` → `SULFURIC_ROASTED` | ×6 |
| `SOLVENT_BYPRODUCT` → `SOLVENT_ROASTED` | ×6 |
| `RAD_BYPRODUCT` → `RAD_ROASTED` | ×6 |

Generating pattern an implement-wave agent can reproduce as a Java loop exactly like `ModRecipeProvider`'s `BILLET_SETS` convention: `for (BedrockOreType type : BedrockOreType.VALUES) for (String[] gradePair : GRADE_PAIRS) addCombinationRecipe(BedrockOreItems.get(type, gradePair[0]), BedrockOreItems.get(type, gradePair[1]), Fluids.VITRIOL, 50)` where `GRADE_PAIRS = {{"BASE","BASE_ROASTED"},{"PRIMARY","PRIMARY_ROASTED"},{"SULFURIC_BYPRODUCT","SULFURIC_ROASTED"},{"SOLVENT_BYPRODUCT","SOLVENT_ROASTED"},{"RAD_BYPRODUCT","RAD_ROASTED"}}`.

### StorageDrumRecipes (28 entries)

`decayChance` default (no LBSM/528) = `VersatileConfig.getLongDecayChance()` = `VersatileConfig.getShortDecayChance()` = 3 hours = **216,000** ticks (both long and short use the identical formula in CE's default config, confirmed at `VersatileConfig.java:39-44`); tiny variants use `(int)(216000*0.1) = 21,600`.

| WasteClass (long, 5) | Normal: input→output, chance, liquid, gas | Tiny: input→output, chance, liquid, gas |
|---|---|---|
| THORIUM | `nuclear_waste_long:0`→`nuclear_waste_long_depleted:0`, 216000, 0, 0 | `_tiny` variant, 21600, 0, 0 |
| URANIUM233 | →depleted, 216000, 0, 50 | tiny, 21600, 0, 5 |
| URANIUM235 | →depleted, 216000, 0, 0 | tiny, 21600, 0, 0 |
| NEPTUNIUM | →depleted, 216000, 0, 100 | tiny, 21600, 0, 10 |
| SCHRABIDIUM | →depleted, 216000, 0, 250 | tiny, 21600, 0, 25 |

| WasteClass (short, 8) | Normal: chance, liquid, gas | Tiny: chance, liquid, gas |
|---|---|---|
| URANIUM233 | 216000, 50, 100 | 21600, 5, 10 |
| URANIUM235 | 216000, 0, 100 | 21600, 0, 10 |
| NEPTUNIUM | 216000, 150, 500 | 21600, 15, 50 |
| PLUTONIUM239 | 216000, 250, 1000 | 21600, 25, 100 |
| PLUTONIUM240 | 216000, 350, 1000 | 21600, 35, 100 |
| PLUTONIUM241 | 216000, 500, 1000 | 21600, 50, 100 |
| AMERICIUM242 | 216000, 750, 1000 | 21600, 75, 100 |
| SCHRABIDIUM | 216000, 1000, 1000 | 21600, 100, 100 |

Plus 2 standalone entries (line 56-57): `ingot_au198` → `bottle_mercury`, chance `(int)(216000*0.01)=2160`, liquid 500, gas 500; `nugget_au198` → `ingot_mercury` (CE field name; real registry id is `nugget_mercury`, see below), chance `(int)(216000*0.001)=216`, liquid 50, gas 50.

Generating pattern: `for (WasteClass w : ItemWasteLong.WasteClass.VALUES) { addRecipe(long[i], longDepleted[i], baseChance, w.liquid, w.gas); addRecipe(longTiny[i], longDepletedTiny[i], baseChance/10, w.liquid/10, w.gas/10); }` (same shape for short), matching `ModRecipeProvider`'s table-loop convention exactly, table-keyed on the `WasteClass` enum's own `.liquid`/`.gas` fields rather than a separate parallel array.

---

## Item/registry dependency check

Every distinct item family referenced by these 4 files, grepped against `src/main/java/com/hbm` (not assumed):

### SolderingRecipes — item status

| Item family | Port status | Evidence |
|---|---|---|
| `circuit` (`EnumCircuitType`, 19 variants: `VACUUM_TUBE,CAPACITOR,CAPACITOR_TANTALIUM,PCB,SILICON,CHIP,CHIP_BISMOID,ANALOG,BASIC,ADVANCED,CAPACITOR_BOARD,BISMOID,CONTROLLER_CHASSIS,CONTROLLER,CONTROLLER_ADVANCED,QUANTUM,CHIP_QUANTUM,CONTROLLER_QUANTUM,ATOMIC_CLOCK,NUMITRON`) | **NOT REGISTERED.** The enum itself exists (`items/ItemEnums.java:129-152`, verbatim match to CE), but **zero flattened `circuit_*` items exist** — 0 hits for any `circuit_` id anywhere in `src/main/java`. This port's own `ModRecipeProvider.java:58-63` javadoc independently confirms this by name ("CE's electronics family (`EnumCircuitType...`) ... do not exist in this port at all yet"). **This single missing family blocks all 26 entries** (every one uses `circuit` as an ingredient and/or output). |
| `upgrade_template` | **NOT REGISTERED.** 0 hits anywhere. Blocks entries 10-16 (all 7 base-tier upgrades). |
| `upgrade_speed_1/2/3`, `upgrade_effect_1/2/3`, `upgrade_power_1/2/3`, `upgrade_fortune_1/2/3`, `upgrade_afterburn_1/2/3`, `upgrade_radius`, `upgrade_health`, `upgrade_overdrive_1` | **REGISTERED.** All present in `items/machine/MachineItems.java:300-319`, exact id match. |
| `MINGRADE.dust()` → `powder_mingrade` | **NOT REGISTERED.** `MAT_MINGRADE` exists in `Mats.java:156` but no `powder_mingrade` hand item anywhere (grep 0 hits). Blocks entry 10 specifically. |
| `EMERALD.dust()` → `powder_emerald`, `GOLD.dust()` → `powder_gold`, `NB.dust()` → `powder_niobium`, `W.dust()` → `powder_tungsten`, `LI.dust()` → `powder_lithium` | **REGISTERED** — all confirmed in `items/BilletPowderItems.java` (lines 146, 148, 151, 167, 215). |
| `"dustGlowstone"` (vanilla-adjacent) | **REGISTERED** trivially (`minecraft:glowstone_dust`). |
| `PB.wireFine()` → `lead_wire` | **REGISTERED** via `MaterialItemGenerator` (`MAT_LEAD` has `WIRE` in its autogen list, `Mats.java:124`; `WIRE` is one of `MaterialItemGenerator.AUTOGEN_SHAPES`). |
| `RUBBER.ingot()` → `ingot_rubber` | **REGISTERED** as a legacy hand field, `IngotNuggetItems.java:110`. |
| `ANY_HARDPLASTIC.ingot()` / `ANY_PLASTIC.ingot()` | **NOT REGISTERED.** CE's `ANY_PLASTIC`=Polymer+Bakelite, `ANY_HARDPLASTIC`=Polycarbonate+PVC (`OreDictManager.java:368,372`). This port's `MAT_POLYMER`/`MAT_BAKELITE`/`MAT_HARDPLASTIC`/`MAT_PVC` (`Mats.java:178-182`) only autogen `STOCK`/`GRIP` — no `INGOT` shape, and no legacy `ingot_*` hand field exists for any of the four either (grep 0 hits). **No ingot-shaped plastic item of any kind exists in this port today.** Blocks entries 5, 6, 17-21. |

**Net: 0 of 26 `SolderingRecipes` entries are ready to port today** — every single one needs the `circuit` family at minimum, and several need one further missing item (`upgrade_template`, `powder_mingrade`, or a plastic ingot) on top.

### CombinationRecipes — item status

| Item family | Port status | Evidence |
|---|---|---|
| `coke` (`EnumCokeType`: COAL/LIGNITE/PETROLEUM, hand item) | **NOT REGISTERED.** Enum exists (`ItemEnums.java:10-16`), but only a **block** form (`BlockCoke`, `blocks/generic/GenericBlocks.java:539`, `block_coke_*`) exists — no hand `coke` item. Blocks entries 1-6, 18-21. |
| `briquette` (`EnumBriquetteType`: COAL/LIGNITE/WOOD) | **NOT REGISTERED.** Enum exists (`ItemEnums.java:29-34`); 0 hits for a `briquette` item. Blocks entries 3, 6, 17. |
| `powder_calcium`, `powder_sodium`, `powder_aluminium`, `powder_chlorocalcite`, `powder_molysite`, `powder_limestone` | **REGISTERED** — all in `items/BilletPowderItems.java`. |
| `crystal_cinnabar`, `gem_sodalite` | **REGISTERED** — `items/PlateCrystalWasteItems.java:216,226`. |
| `sulfur` (hand item, `ModItems.sulfur` in CE) | **NOT REGISTERED.** Only ore/world-gen block references (`OreWorldGenFeatures.java`), no hand item. Blocks entries 9, 10. |
| `chunk_ore` (`EnumChunkType.CRYOLITE`) | **NOT REGISTERED.** Enum exists (`ItemEnums.java:72-78`); this port's own `BlockResourceStone.java` comment explicitly flags `ModItems.chunk_ore` as a dependency not yet met. Blocks entry 12. |
| `powder_ash` (`EnumAshType.WOOD`) | **NOT REGISTERED.** `BilletPowderItems.java:233` and `entity/missile/EntityMissileStealth.java:53` both carry explicit `TODO(items-followup)` comments confirming this. Blocks entry 16. |
| `oil_tar` (`EnumTarType`: CRUDE/CRACK/COAL/WOOD) | **NOT REGISTERED.** `RefineryRecipes.java:40,99,106,113` (this port's own already-ported Refinery recipe file!) has 3 live `TODO(items-followup)` comments for exactly this item. Blocks entries 18-21. |
| `NA.dust()` (CE `OreDictManager.NA`, likely distinct elemental-sodium dust) | **Not individually verified** — could not confirm whether this resolves to the same `powder_sodium` item or a separate one without reading CE's full `OreDictManager.java` (not read in full, out of this task's budget). Flagged as an open question, not asserted either way. Affects entry 13. |
| `iron_ingot`, `glowstone_dust`, `coal` (charcoal, meta 1), `reeds`, `sugar`, `clay`, `brick_block` (all vanilla) | **REGISTERED** trivially. |
| `KEY_LOG`/`KEY_SAPLING` (CE ore-dict wildcard keys `"logWood"`/`"treeSapling"`) | **Direct 1.21 equivalent exists and is better-typed**: vanilla `ItemTags.LOGS` / `ItemTags.SAPLINGS` — no port-side item registration needed, just an `Ingredient.of(tag)` on the recipe-matching side. |
| `BedrockOreType` (6) × `BedrockOreGrade` (26, incl. `BASE/BASE_ROASTED/PRIMARY/PRIMARY_ROASTED/SULFURIC_BYPRODUCT/SULFURIC_ROASTED/SOLVENT_BYPRODUCT/SOLVENT_ROASTED/RAD_BYPRODUCT/RAD_ROASTED`) | **FULLY REGISTERED**, verified verbatim against CE. `items/special/BedrockOreType.java` (6 values, matching suffixes `light/heavy/rare/actinide/nonmetal/crystal`) and `items/special/BedrockOreGrade.java` (26 values, same names/order as CE's `ItemBedrockOreNew.BedrockOreGrade`) both ported. `items/special/BedrockOreItems.java` registers the full 6×26=156-item grid + 1 base item, with a clean `BedrockOreItems.get(type, grade)` lookup already provided. **All 30 bedrock-roasting loop entries are ready to port right now** — this is the single largest concrete "ready" finding in this file. |

**Net: of `CombinationRecipes`' 53 entries, 37 are ready today** (7 individual: entries 7, 8, 11, 14, 15, 22, 23 + all 30 bedrock-loop entries), **15 are blocked** (entries 1-6, 9, 10, 12, 16-21) on missing `coke`/`briquette`/`sulfur`/`chunk_ore`/`powder_ash`/`oil_tar` item families, and **1 is unresolved** (entry 13, `NA.dust()`).

### StorageDrumRecipes — item status

| Item family | Port status | Evidence |
|---|---|---|
| `nuclear_waste_long` / `nuclear_waste_short` (base, 5+8 variants) | **REGISTERED** — `items/special/SpecialItems.java:276-303`, ids `nuclear_waste_long_<class>` / `nuclear_waste_short_<class>`. |
| `nuclear_waste_long_tiny`, `nuclear_waste_long_depleted`, `nuclear_waste_long_depleted_tiny` (and the `_short_` equivalents) | **NOT REGISTERED.** This port's own `SpecialItems.java:270-274` code comment states this explicitly: only the base fields were flattened; "CE's six sibling fields (`_tiny`, `_depleted`, `_depleted_tiny` for each) are a distinct open question, see this area's final report." **Every one of the 26 loop-generated entries needs at least one of these missing variants** (all need a `_depleted`/`_depleted_tiny` output that doesn't exist; the 5 long-tiny and 8 short-tiny entries also need a `_tiny` input that doesn't exist). |
| `ingot_au198`, `nugget_au198` | **REGISTERED** — `items/IngotNuggetItems.java:218,333`. |
| `bottle_mercury` | **NOT REGISTERED.** This port's own `inventory/FluidContainerRegistry.java:144-146` has an explicit code comment: "needs `ModItems.bottle_mercury`/`ingot_mercury`, neither of which exist in this port." Blocks the au198-ingot entry. |
| CE's `ModItems.ingot_mercury` (the recipe's literal Java reference for the nugget-au198 output) | **Resolves to `nugget_mercury`, which DOES exist** — `IngotNuggetItems.java:254-259` documents that CE's field named `ingot_mercury` is actually registered under the string id `"nugget_mercury"` (a known CE field-name/registry-id mismatch), and this port already ported it under the correct id. **So the nugget-au198→mercury entry is item-ready today** despite the `FluidContainerRegistry.java` comment's blanket "neither exist" phrasing (that comment is about a different, unrelated fluid-container use of the same two CE fields, not about this specific recipe). |

**Net: 0 of 28 `StorageDrumRecipes` entries are fully ready** (all 26 loop entries blocked on the missing `_depleted`/`_tiny` waste-item variants; of the 2 standalone entries, the au198-nugget→mercury one is item-ready but the au198-ingot→`bottle_mercury` one is not) — **but the blocker is narrow and well-scoped**: registering 6 more item variants (`nuclear_waste_{long,short}_{tiny,depleted,depleted_tiny}` — actually the port already has "no tiny/depleted" so it's 3 more sibling families × 2 (long/short) = 6 field-groups, each following the exact same `EnumMap`-over-`WasteClass` pattern `SpecialItems.java:276-303` already establishes) plus `bottle_mercury` would unblock all 28 entries in one pass.

---

## Recommended 1.21.1 implementation shape

**Machine block-entities needed first, for 3 of 4 files** (Breeder's machine already exists): Soldering Station, Combination Furnace, and Storage Drum are not built at all in this port. This assignment's own recipe-data work is necessarily downstream of (or must land alongside) new `BlockEntity` classes for these three — flagging clearly for the implement wave so it isn't scoped as "just add JSON to an existing machine."

- **`SolderingRecipes` → bespoke JSON `Recipe<?>` class, NOT plain shaped/shapeless, and NOT the simple `HbmSimpleRecipe`/`BreederRecipe` one-ingredient shape.** The recipe has 3 *distinct, role-typed* ingredient groups (toppings/pcb/solder, each independently checked, unlike a flat multiset) plus an optional fluid input, duration and consumption. This port's own `AssemblerRecipe.java` (`inventory/recipes/AssemblerRecipe.java`) is the closest existing precedent — a JSON-backed `Recipe<?>` with `List<Entry>` (Ingredient+count pairs) instead of a single `Ingredient`, already proven to compile and Codec-serialize in this port's 1.21.1 tree. Recommend a new `SolderingRecipe implements Recipe<SolderingRecipe.Input>` modeled directly on `AssemblerRecipe`'s structure but with 3 separate `List<Entry>` fields (`toppings`, `pcb`, `solder`) instead of 1, plus an `Optional<FluidStack>` field once `FluidStack` gets a Codec (currently it doesn't — see Open Questions) — or, if that Codec gap is judged blocking, follow the `ChemPlantRecipes`/`RefineryRecipes` plain-Java-static-table precedent instead (`List<SolderingRecipeData> RECIPES` + a `register()` method, `AStack[]`-typed slots, no JSON at all) the same way those two files already did for multi-slot+fluid shapes that "don't fit vanilla's `Recipe<RecipeInput>` contract" per their own javadoc. **Given the item-registry blocker below (0/26 ready), this is a "build the class, ship zero data" task until the `circuit` item family lands from another area — not worth over-investing in the Codec route today.**

- **`CombinationRecipes` → also a bespoke recipe shape, but simpler: single item-key lookup → optional item output + optional fluid output, heat-driven (not tick-duration-driven).** CE's own `TileEntityFurnaceCombination` (`upstream/hbm-ce/.../tileentity/machine/TileEntityFurnaceCombination.java:44,49,64-107`) is a heat-accumulator machine (`processTime=20_000` ticks, `heat` field fed externally, `burn = heat/100` per tick, advances `progress` only while `heat>0`) — not a duration-per-recipe machine the way Soldering is. Recommend a small `CombinationRecipe` data class (`AStack input, ItemStack output, FluidStack outputFluid` — both outputs nullable, matching CE's `Pair<ItemStack,FluidStack>` exactly) registered the same static-table way `StorageDrumRecipes`/`ChemPlantRecipes` are, since there's no per-recipe duration/power field to justify a JSON `Recipe<?>` wrapper (the block entity's own heat curve is a machine-level constant, not part of the recipe data) — a plain `Map<AStack, CombinationRecipe>`-shaped Java class is both truer to CE's own structure and less implementation work than inventing an unused JSON schema. **Given the item-registry blocker (37/53 ready), this file is worth implementing now** — the class + the 30 bedrock-roast entries + the 7 ready individual entries (37 total) can ship immediately once the Combination Furnace block-entity exists, with the remaining 16 entries following once `coke`/`briquette`/`sulfur`/`chunk_ore`/`powder_ash`/`oil_tar` land.

- **`BreederRecipes` → no new work; the established JSON `Recipe<SingleRecipeInput>` shape (already shipped) is correct and complete.** Nothing to recommend beyond "add 1 more JSON file if/when `meteorite_sword_etched`/`_bred` are registered."

- **`StorageDrumRecipes` → NOT a `RecipeType` at all, in CE or here.** CE's own consuming class (`TileEntityStorageDrum.update()`, `upstream/hbm-ce/.../TileEntityStorageDrum.java:53-101`) is a **passive per-tick random-chance decay table** consulted unconditionally every server tick for every occupied slot (`world.rand.nextInt(wasteData[0])==0`), not a player-invoked craft — there is no GUI "craft" button, no progress bar, no recipe *selection* at all. This is architecturally identical to `MatDistribution`/`Mats.materialOreEntries` (a static lookup table consulted by passive world/block-entity logic), not to any of this port's existing `RecipeType` family. Recommend a plain static Java data class (`Map<ComparableStack, StorageDrumRecipe>` where `StorageDrumRecipe{ItemStack output; int chance, liquid, gas;}`), registered via a table-driven loop over `WasteClass.VALUES` exactly matching `ModRecipeProvider`'s `BILLET_SETS`-loop convention, **no JSON, no `RecipeType`/`RecipeSerializer` at all** — JSON-loading this would add real complexity (Codec, data-driven reload) for a mechanic that gains nothing from being data-pack-overridable the way a craftable recipe would. **Given the item-registry blocker (0/28 fully ready — all 26 loop entries need the undecided `_tiny`/`_depleted` waste-item variants), this file's implementation should be sequenced after the waste-item-variant decision below is made**, not before.

---

## Open questions / risks

1. **The `_tiny`/`_depleted`/`_depleted_tiny` nuclear-waste item variants are a named, already-flagged open question in this port's own source** (`SpecialItems.java:270-274`), not something this task discovered — but it is the single hard blocker for all 26 of `StorageDrumRecipes`' loop-generated entries. Whoever picks this up needs to either extend `SpecialItems.java`'s existing `EnumMap`-over-`WasteClass` pattern with 3 more sibling families (×2 for long/short = 6 field-groups) or confirm a different area already owns that decision, before `StorageDrumRecipes` can ship more than the au198-nugget entry.
2. **`bottle_mercury` and a genuinely-`ingot_mercury`-named item may not need to exist as literally-CE-shaped items at all** — this port's `IngotNuggetItems.java:254-259` already documents that CE's own `ingot_mercury` field is a misnomer for `nugget_mercury`. Worth double-checking whether CE's `bottle_mercury` (a filled-glass-bottle consumable, likely `ItemStack(Items.GLASS_BOTTLE)`-shaped with mercury as filled-container metadata in 1.12's oredict-container system) has a natural 1.21 analogue via this port's own fluid-container item system (`FluidContainerRegistry.java`, already referenced above) rather than needing a brand-new standalone item — not resolved by this task.
3. **`NA.dust()` in `CombinationRecipes` entry 13 was not individually resolved** — CE's `OreDictManager.java` (472+ lines) was not read in full; whether CE's `NA` constant resolves to the same item as `SODIUM`/`powder_sodium` (used elsewhere in the same file, entry 11's output) or a distinct elemental-sodium item is unconfirmed. Low-impact (1 of 53 entries) but should be verified against CE's real `OreDictManager.java` before the implement wave marks this entry either ready or blocked.
4. **`FluidStack` has no `Codec`/`StreamCodec` in this port yet** (noted independently by this port's own `AssemblerRecipe.java:39-41` javadoc, a real, separately-scoped gap this task did not verify further) — this affects whether `SolderingRecipes`' fluid-input field and `CombinationRecipes`' fluid-output field can be JSON-loaded at all today, reinforcing the recommendation above to use plain Java static tables for both rather than blocking on that Codec landing first.
5. **CE's `SolderingRecipes` gates 13 of its 26 entries behind `if (GeneralConfig.enable528)`-derived `no528`** — CE's default config has `enable528=false` so `no528=true` and all 26 run by default, which is what this report catalogs and counts. If a future config-porting task adds an equivalent toggle, the implement wave should preserve the same default-on behavior rather than accidentally hard-coding only the 13 unconditional entries.
6. **The `AStack`/`ComparableStack`/`OreDictStack` abstraction this port already carries (`inventory/RecipesCommon.java`) includes `OreDictStack.ofCommonTag(path)`** (`RecipesCommon.java:355-357`) as the documented modern replacement for CE's legacy ore-dict alias strings (e.g. `PB.wireFine()` → `OreDictStack.ofCommonTag("wires/lead")` once/if a common tag is populated for that shape+material) — worth using directly rather than reinventing a tag-matching helper, once the underlying items in question actually exist.
7. **Item-registration blockers dominate over recipe-logic complexity for all 4 files.** None of `SolderingRecipes`/`CombinationRecipes`/`StorageDrumRecipes` is blocked by anything hard about the recipe *shape* — the shapes are all either already-precedented (`AssemblerRecipe`/`ChemPlantRecipes`/`BreederRecipe`) or genuinely simple (`StorageDrumRecipes`' passive lookup table). The real work is elsewhere: building 3 new machine block-entities, and registering ≈8 missing item families (`circuit`, `upgrade_template`, `powder_mingrade`, a plastic-ingot shape, `coke`, `briquette`, `sulfur`, `chunk_ore`, `powder_ash`, `oil_tar`, the 6 nuclear-waste sibling families, `bottle_mercury`) most of which are almost certainly other areas' assigned scope, not this recipe-data area's.