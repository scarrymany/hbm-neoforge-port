# Research report — mrec-14-annihilator-misc

Scope: CE `com.hbm.inventory.recipes.{AnnihilatorRecipes,PressRecipes,FractionRecipes}` (three
per-machine-type recipe-data classes under `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`,
sibling to but distinct from the crafting-table classes under `com/hbm/crafting/`, which are other
tasks' scope).

## Scope confirmed

All three files were confirmed present at exactly the line counts the assignment states, and confirmed
**not touched by this port at all** — zero references to any of the three class names, or to their
corresponding CE machine block/tile-entity/block-entity names, anywhere under this port's
`src/main/java/com/hbm` (grepped case-sensitively for `Annihilator`, `MachinePress`, `PressRecipes`,
`FractionTower`, `FractionRecipes` — the only two incidental hits, both in port-side doc comments
referencing these class names as *design precedent*, are called out in "Already covered" below).

| File | Lines | Confirmed by | In-CE structure |
|---|---:|---|---|
| `AnnihilatorRecipes.java` | 277 | `wc -l`, read in full | `SerializableRecipe` subclass. One static `HashMap<Object, AnnihilatorRecipe>` (`recipes`), where `Object` key can be `Item`, `ComparableStack`, `FluidType`, or `String` (ore-dict name) and `AnnihilatorRecipe` is a `List<Pair<BigInteger, ItemStack>>` of "milestones" (cumulative-amount-fed → payout). `registerDefaults()` (lines 57-79) is a **flat list of 13 literal `recipes.put(...)` calls**, entirely inside `if(GeneralConfig.enable528) { ... }` — none run unless 528-mode is enabled (CE default: `enable528 = false`). The remaining ~200 lines are non-recipe-table machinery: `getRecipes()` (JEI flattening), `getHighestPayoutFromKey/Stack/Fluid/Recipe` (the actual runtime "which milestone was just crossed" lookup algorithm), and `readRecipe`/`writeRecipe` (JSON persistence to `hbmAnnihilator.json`, CE's moddable-recipe convention). |
| `PressRecipes.java` | 153 | `wc -l`, read in full | `SerializableRecipe` subclass. One static `HashMap<Pair<AStack, ItemStamp.StampType>, ItemStack>` (`recipes`) — keyed by a **two-part match**: an ore-dict-aware ingredient (`AStack`) *and* which `StampType` the player's inserted stamp item resolves to (`ItemStamp.getStampType(...)`). `getOutput(ingredient, stamp)` (lines 36-53) does a **linear scan** matching stamp-type equality + `AStack.matchesRecipe(...)`, not a direct map lookup (because `AStack` equality isn't structural). `registerDefaults()` (lines 56-106) is **37 literal `makeRecipe(...)` calls in 6 sub-groups** (FLAT/PLATE/casings/CIRCUIT/PRINTING) **plus one `for` loop over `Mats.orderedList`** (lines 90-94, generating WIRE-shape recipes for every material with `WIRE` in its `setAutogen(...)` list AND an existing `INGOT` ore-dict entry — CE's own `Mats.java` has 12 such materials, one (`MAT_ALLOY`) commented out/inactive, so **≈11 loop-generated recipes**, not individually re-verified against the ingot-oredict-exists condition given this task's budget). Total ≈48 entries. |
| `FractionRecipes.java` | 104 | `wc -l`, read in full | `SerializableRecipe` subclass. One static `Map<FluidType, Tuple.Pair<FluidStack, FluidStack>>` (`fractions`) — keyed by input fluid, value is a **fixed-percentage 2-output split** (class's own `getComment()`: "Inputs are always 100mB, set output quantities accordingly"). `registerDefaults()` (lines 23-43) is a **flat list of exactly 18 literal `fractions.put(...)` calls, no loops, no conditions**. Remainder: `getFractions`/`getFractionRecipesForJEI` (JEI display) and `readRecipe`/`writeRecipe` (JSON persistence to `hbmFractions.json`). |

**CE machine correspondence** (grepped `upstream/hbm-ce/src/main/java/com/hbm` for each machine family):

- **Annihilator** → `blocks/machine/MachineAnnihilator.java` (block) + `tileentity/machine/TileEntityMachineAnnihilator.java` (288 lines) + `saveddata/AnnihilatorSavedData.java` (215 lines, `WorldSavedData` — the persistent per-pool cumulative-amount counter this recipe class's milestone table is checked against) + GUI/Container/Render/JEI-handler classes.
- **Press** → `blocks/machine/MachinePress.java` (block) + `tileentity/machine/TileEntityMachinePress.java` + GUI/Container/Render/JEI-handler classes.
- **Fraction Tower** → `blocks/machine/MachineFractionTower.java` + `blocks/machine/FractionSpacer.java` (blocks) + `tileentity/machine/oil/TileEntityMachineFractionTower.java` (lives in the same CE package, `tileentity.machine.oil`, as `TileEntityMachineRefinery` — the refinery's sibling oil-processing machine) + JEI handler (`handler/jei/FractioningRecipeHandler.java`).

**None of these three machine block/block-entity families exist in this port yet** (confirmed by grep — see "Item/registry dependency check" below for exactly what does and doesn't exist). This is a genuine "both the machine and its recipe data are missing" situation for all three, **not** a "machine exists, recipes are the only gap" situation — important context for the implement wave: recipe-data porting alone will not make any of these three machines playable; the block/block-entity/GUI/container layer must also be built (out of this task's scope, which is recipe-data-only, but flagged so the implement wave doesn't assume otherwise).

## Already covered by this port

**No CE recipe content from any of these three files has been ported.** However, this task found
substantially more *supporting infrastructure* already committed than the assignment's "likely NOT
touched by this port at all yet" framing suggested — worth stating precisely so the implement wave
does not re-invent it:

1. **`ItemStamp.java`** (`src/main/java/com/hbm/items/machine/ItemStamp.java`, 65 lines) already ports
   CE's `ItemStamp.StampType` enum **verbatim, all 16 constants** (`FLAT, PLATE, WIRE, CIRCUIT, C357,
   C44, C50, C9, PRINTING1..8`), plus the `STAMPS` static lookup table CE's own `ItemStamp` class uses.
   Its own class javadoc states outright: *"Die item for a (Phase 2) press machine. Only self-registers
   into a static lookup table for that future machine to query — no tile entity reference of its own."*
   This is a documented, deliberate stable seam for exactly the Press machine this task's `PressRecipes`
   feeds — already anticipated, not yet consumed.
2. **`MachineItems.java`** (lines 522-563) already registers **all 40 stamp items** PressRecipes needs
   as inputs: 32 tiered stamps (`stamp_{stone,iron,steel,titanium,obsidian}_{flat,plate,wire,circuit}`
   = 20, `stamp_desh_{flat,plate,wire,circuit,357,44,9,50}` = 8, `stamp_{357,44,9,50}` = 4) plus 8
   `stamp_book_printing{1..8}` (`ItemStampBook`, hidden from creative to match CE) via a `for` loop over
   `StampType.values()`. **The entire "stamp" side of the Press machine's item prerequisites is done.**
3. **`GenericRecipes.java`** (`src/main/java/com/hbm/inventory/recipes/loader/GenericRecipes.java`,
   line 42) already declares `POOL_PREFIX_528 = "528."`, an exact string match to CE's own constant —
   the blueprint-pool-naming convention `AnnihilatorRecipes`'s 13 entries all key off
   (`ItemBlueprints.make(GenericRecipes.POOL_PREFIX_528 + "steel")`, etc.).
4. **`ItemBlueprints.java`** already has the exact `make(Supplier<? extends Item>, String pool)` factory
   AnnihilatorRecipes' payouts call.
5. **`GeneralConfig.java`** already ports `enable528()` (line 379-380 area, `"enable528Mode"`, default
   `false`) — the exact config gate CE wraps 100% of `AnnihilatorRecipes.registerDefaults()`'s body in.
   `VersatileConfig.java` already reads it. This means the implement wave has a ready-made, already-wired
   gate to reuse if it ports the Annihilator's milestone table.
6. **`RefineryRecipes.java`** (`src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java`, 153
   lines, already committed and JEI-wired via `com.hbm.compat.jei.category.RefineryCategory`) is a
   **near-exact structural precedent for `FractionRecipes`**: a bespoke (non-JSON, non-datagen) static
   `Map<FluidType, Tuple.Quintet<...>>` populated once by a `registerX()` method, explicitly justified in
   its own javadoc as *"CE never made this data-driven/moddable... this stays a literal hardcoded Java
   registration list rather than inventing a new `Recipe<?>`/datagen shape for a single consumer, the
   lower-risk option"* — the identical reasoning applies to `FractionRecipes` (see "Recommended shape"
   below). `RefineryRecipes` also demonstrates the fluid-transform idiom already exists end-to-end in
   this port: `Fluids`, `FluidStack`, `Tuple.Pair`, and a working JEI category consuming it.
7. **`HbmSimpleRecipe.java`** (`src/main/java/com/hbm/inventory/recipes/HbmSimpleRecipe.java`) — this
   port's own reusable single-`Ingredient`-in/single-`ItemStack`-out JSON `Recipe<SingleRecipeInput>`
   scaffolding — **names `PressRecipes` by name in its own class javadoc** as one of the "tier 1" CE
   recipe classes (`ShredderRecipes`, `CentrifugeRecipes`, `PressRecipes`) this shape was built to
   eventually serve. This is a real, if partial, design commitment already on record — see the caveat in
   "Recommended shape" below (CE's real `PressRecipes` is actually a **two**-input match, which
   `HbmSimpleRecipe` as currently coded does not support).
8. **`RecipesCommon.java`**'s `AStack`/`ComparableStack`/`OreDictStack` and `com.hbm.util.Tuple.Pair` —
   the exact low-level match/data types both `PressRecipes` and `FractionRecipes` are built on in CE —
   are already ported and NeoForge-native: `OreDictStack` is backed by a real `TagKey<Item>` (with an
   `ofCommonTag(String)` convenience for the `c:` tag namespace), not a 1.12 OreDictionary string. No
   plumbing gap here at all.

**Net "already covered" verdict**: the *recipe data itself* for all three files is a 100% gap (nothing
ported). But three of the four pieces of infrastructure a straightforward port would need — the stamp
item family + enum, the blueprint-pool/528-config plumbing, and the `AStack`/`Tuple`/`FluidStack` match
types — are already done, considerably de-risking the implement wave's job versus what "NOT touched at
all" alone would suggest. The fourth piece — the actual machine blocks/block-entities/GUIs — is the
one genuinely large remaining prerequisite, and it is **not** in this recipe-data task's scope.

## Full recipe/entry catalog

All three files are small (13 + ≈48 + 18 = ≈79 entries combined); every entry is catalogged below.

### AnnihilatorRecipes.java — 13 entries (all gated behind `GeneralConfig.enable528`, CE default `false`)

| # | Key (input) | Milestone amount | Payout (blueprint pool) |
|---|---|---:|---|
| 1 | `STEEL.ingot()` | 256 | `528.steel` |
| 2 | `SI.billet()` | 256 | `528.chip` |
| 3 | `BI.nugget()` | 128 | `528.chip_bismoid` |
| 4 | `ModItems.pellet_charged` | 1,024 | `528.chip_quantum` |
| 5 | `U.billet()` | 256 | `528.gascent` |
| 6 | `ANY_PLASTIC.ingot()` | 512 | `528.plastic` |
| 7 | `RUBBER.ingot()` | 512 | `528.rubber` |
| 8 | `FERRO.ingot()` | 1,024 | `528.ferrouranium` |
| 9 | `SR.dust()` | 256 | `528.strontium` |
| 10 | `ANY_HARDPLASTIC.ingot()` | 1,024 | `528.hardplastic` |
| 11 | `ANY_RESISTANTALLOY.ingot()` | 1,024 | `528.tcalloy` |
| 12 | `ModItems.powder_chlorophyte` | 1,024 | `528.chlorophyte` |
| 13a | `ComparableStack(ammo_standard, BMG50_FMJ)` | 256 | `528.bmg` |
| 13b | `ComparableStack(ammo_arty, meta 0)` | 128 | `528.arty` |
| 13c | `ComparableStack(circuit, CONTROLLER)` | 128 | `528.controller` |

(13 `recipes.put(...)` call sites; the last 3 are individually listed as they carry distinct
`ComparableStack` keys, matching the source's own line-per-entry layout.) Each entry's "milestone" is a
**single** `Pair<BigInteger, ItemStack>` in this default set, though the data structure (`AnnihilatorRecipe.milestones`, a `List`) supports multiple thresholds per key — CE's defaults just don't use that
capacity.

### PressRecipes.java — 37 literal entries + ≈11 loop-generated WIRE entries (≈48 total)

**FLAT group (11):**

| Stamp | Ingredient | Output |
|---|---|---|
| FLAT | `NETHERQUARTZ.dust()` | `Items.QUARTZ` (vanilla) |
| FLAT | `LAPIS.dust()` | `Items.DYE:4` (vanilla lapis dye) |
| FLAT | `DIAMOND.dust()` | `Items.DIAMOND` (vanilla) |
| FLAT | `EMERALD.dust()` | `Items.EMERALD` (vanilla) |
| FLAT | `ModItems.biomass` | `biomass_compressed` |
| FLAT | `ANY_COKE.gem()` | `ingot_graphite` |
| FLAT | `meteorite_sword_reforged` | `meteorite_sword_hardened` |
| FLAT | `Blocks.LOG:3` (jungle log) | `ball_resin` |
| FLAT | `COAL.dust()` | `briquette(COAL)` |
| FLAT | `LIGNITE.dust()` | `briquette(LIGNITE)` |
| FLAT | `powder_sawdust` | `briquette(WOOD)` |

**PLATE group (13, ingot → plate):**

| Ingredient | Output |
|---|---|
| `IRON.ingot()` | `plate_iron` |
| `GOLD.ingot()` | `plate_gold` |
| `TI.ingot()` | `plate_titanium` |
| `AL.ingot()` | `plate_aluminium` |
| `STEEL.ingot()` | `plate_steel` |
| `PB.ingot()` | `plate_lead` |
| `CU.ingot()` | `plate_copper` |
| `SA326.ingot()` | `plate_schrabidium` |
| `CMB.ingot()` | `plate_combine_steel` |
| `GUNMETAL.ingot()` | `plate_gunmetal` |
| `WEAPONSTEEL.ingot()` | `plate_weaponsteel` |
| `BIGMT.ingot()` | `plate_saturnite` |
| `DURA.ingot()` | `plate_dura_steel` |

**Casing group (4):**

| Stamp | Ingredient | Output |
|---|---|---|
| C9 | `GUNMETAL.plate()` | `casing(SMALL, ×4)` |
| C50 | `GUNMETAL.plate()` | `casing(LARGE, ×2)` |
| C9 | `WEAPONSTEEL.plate()` | `casing(SMALL_STEEL, ×4)` |
| C50 | `WEAPONSTEEL.plate()` | `casing(LARGE_STEEL, ×2)` |

**WIRE group (loop, ≈11 entries):** `for (NTMMaterial mat : Mats.orderedList) if (mat.autogen has WIRE
&& INGOT ore-dict exists) → makeRecipe(WIRE, ingot(mat), wire_fine ×8, meta=mat.id)`. CE's `Mats.java`
has **12** materials with `WIRE` in `setAutogen(...)` (`CARBON, GOLD, SCHRABIDIUM, COPPER, TUNGSTEN,
ALUMINIUM, LEAD, ZIRCONIUM, STEEL, MINGRADE, MAGTUNG` — 11 active + `MAT_ALLOY` commented out/inactive
in CE's own source) — the second condition (ingot ore-dict presence) was not individually re-verified
per material given this task's budget, so ≈11 is an upper-bound estimate, not an exact count.

**CIRCUIT group (1):** `SI.billet()` → `circuit(SILICON)`.

**PRINTING group (8):** `Items.PAPER` (vanilla) → `page_of_(PAGE1..8)`, one recipe per `PRINTING1..8`
stamp type, 1:1.

### FractionRecipes.java — 18 entries (complete, exhaustive; "In / OutA:qty / OutB:qty", all quantities
in mB, all inputs fixed at 100mB per the class's own comment)

| Input fluid | Output A | Output B |
|---|---|---|
| `HEAVYOIL` | `BITUMEN` 30 | `SMEAR` 70 |
| `HEAVYOIL_VACUUM` | `SMEAR` 40 | `HEATINGOIL_VACUUM` 60 |
| `SMEAR` | `HEATINGOIL` 60 | `LUBRICANT` 40 |
| `NAPHTHA` | `HEATINGOIL` 40 | `DIESEL` 60 |
| `NAPHTHA_DS` | `XYLENE` 60 | `DIESEL_REFORM` 40 |
| `NAPHTHA_CRACK` | `HEATINGOIL` 30 | `DIESEL_CRACK` 70 |
| `LIGHTOIL` | `DIESEL` 40 | `KEROSENE` 60 |
| `LIGHTOIL_DS` | `DIESEL_REFORM` 60 | `KEROSENE_REFORM` 40 |
| `LIGHTOIL_CRACK` | `KEROSENE` 70 | `PETROLEUM` 30 |
| `COALOIL` | `COALGAS` 30 | `OIL` 70 |
| `COALCREOSOTE` | `COALOIL` 10 | `BITUMEN` 90 |
| `REFORMATE` | `AROMATICS` 40 | `XYLENE` 60 |
| `LIGHTOIL_VACUUM` | `KEROSENE` 70 | `REFORMGAS` 30 |
| `EGG` | `CHOLESTEROL` 50 | `RADIOSOLVENT` 50 |
| `OIL_COKER` | `CRACKOIL` 30 | `HEATINGOIL` 70 |
| `NAPHTHA_COKER` | `NAPHTHA_CRACK` 75 | `LIGHTOIL_CRACK` 25 |
| `GAS_COKER` | `AROMATICS` 25 | `CARBONDIOXIDE` 75 |
| `CHLOROCALCITE_MIX` | `CHLOROCALCITE_CLEANED` 50 | `COLLOID` 50 |
| `BAUXITE_SOLUTION` | `REDMUD` 50 | `SODIUM_ALUMINATE` 50 |

## Item/registry dependency check

### AnnihilatorRecipes (13 entries) — **all 13 blocked**, none ready to port as concrete recipes

| Ingredient/output family | Port status |
|---|---|
| Blueprint-pool infra (`GenericRecipes.POOL_PREFIX_528`, `ItemBlueprints.make`) | **Exists** (see "Already covered" #3-4) |
| `GeneralConfig.enable528()` gate | **Exists** (see "Already covered" #5) |
| `STEEL.ingot()`, `SI.billet()`, `U.billet()`, `SR.dust()`, `powder_chlorophyte` | Underlying `Mats.MAT_STEEL`/`MAT_SILICON`/`MAT_URANIUM`/`MAT_STRONTIUM` all exist in `Mats.java`; `powder_chlorophyte` is registered (`BilletPowderItems.java:163`). Ingredient side likely portable **once** the port's own `OreDictManager`-style short-alias layer (CE's `IRON`/`SI`/`U`/etc. static imports — see below) is added or the recipe is written against `Mats.MAT_*` directly. |
| `BI.nugget()`, `pellet_charged`, `ANY_PLASTIC`/`ANY_HARDPLASTIC`/`ANY_RESISTANTALLOY` (umbrella ore-dict groups), `ammo_standard`/`EnumAmmo.BMG50_FMJ`, `ammo_arty`, `circuit`/`EnumCircuitType.CONTROLLER` | **Not registered anywhere in this port** (grepped, zero hits): no `pellet_charged`, no `ammo_standard`/`ammo_arty` items, no `EnumAmmo` enum at all under `items/weapon` (confirmed — the gun-ammunition item family is not yet ported), no `circuit`-prefixed items despite `EnumCircuitType` existing as a bare enum (see Press's CIRCUIT group below), and no CE `OreDictManager`-equivalent umbrella-material groups (`ANY_PLASTIC` etc.) exist in this port's `Mats.java`. |
| CE's `OreDictManager` short-alias static-import layer (`IRON`, `TI`, `SA326`, `CU`, `SR`, `U`, `SI`, `FERRO`, `RUBBER`, `BI`, `CMB`, `GUNMETAL`, `WEAPONSTEEL`, `BIGMT`, `DURA`, etc.) | **Not ported.** This port's `Mats.java` names its constants `MAT_IRON`, `MAT_TITANIUM`, `MAT_SCHRABIDIUM`, `MAT_COPPER`, etc. directly, with no short-alias convenience class matching CE's `com.hbm.inventory.OreDictManager` (the `import static com.hbm.inventory.OreDictManager.*;` all three CE files rely on). Not a functional blocker (the underlying materials mostly exist under `MAT_*` names) but every one of these three files' CE source is written against that alias layer, so a literal copy-paste port will not compile without either adding the alias layer or rewriting every reference to `Mats.MAT_*`. |
| `AnnihilatorSavedData` (persistent cumulative-amount tracker) | **Not ported** — no `WorldSavedData`/`SavedData` equivalent anywhere in this port for Annihilator. This is the runtime half of the mechanic (this task's file is only the milestone lookup table); without it, the recipe table has nothing to check against. |
| `TileEntityMachineAnnihilator`/`MachineAnnihilator` (the block/block-entity) | **Not ported** at all (grepped, zero hits). |

**Verdict: 0 of 13 entries are "ready to port now" as functioning gameplay recipes** — every entry needs
at least one unregistered item, and the whole mechanism needs a not-yet-built machine + SavedData class
behind it. The milestone *table itself* (pure data, no engine dependency) could be transcribed today with
placeholder/`ItemStack.EMPTY` outputs where an item doesn't exist, mirroring `RefineryRecipes.java`'s own
already-established `ItemStack.EMPTY` + `TODO(items-followup)` pattern for its `oil_tar` byproduct gaps —
but that would not be a functional recipe until the blocking items land.

### PressRecipes (≈48 entries)

| Sub-group | Ingredient status | Output status | Verdict |
|---|---|---|---|
| FLAT: netherquartz/lapis/diamond dust → vanilla gem | No `Mats.MAT_QUARTZ`/`MAT_LAPIS` exist in this port at all (grepped); `Mats.MAT_DIAMOND` exists but its `setAutogen(...)` list is `FRAGMENT` only — **no `DUST` shape**, so `dust_diamond` is not a real registered item id either. | Vanilla items, always available. | **Blocked** (3 of 11 FLAT recipes) |
| FLAT: emerald dust → `Items.EMERALD` | `Mats.MAT_EMERALD` has `DUST` in its autogen list — item likely exists via `MaterialItemGenerator`'s loop (not individually re-verified this pass). | Vanilla. | **Likely ready**, not fully confirmed |
| FLAT: biomass/biomass_compressed | **Neither registered anywhere in this port** (grepped, zero hits for both identifiers). | — | **Blocked** |
| FLAT: `ANY_COKE.gem()` → `ingot_graphite` | No umbrella `ANY_COKE` ore-dict group in this port; `ingot_graphite` itself **is** registered (`IngotNuggetItems.java:186`, backed by `ItemFuel`). | Ready | **Half-blocked** — output exists, ingredient's umbrella-tag concept doesn't |
| FLAT: `meteorite_sword_reforged`/`_hardened` | **Neither registered** (grepped, zero hits). | — | **Blocked** |
| FLAT: `Blocks.LOG:3` → `ball_resin` | Vanilla input ready. | `ball_resin` **not registered** (grepped, zero hits). | **Blocked** |
| FLAT: coal/lignite dust → `briquette(COAL/LIGNITE)`, sawdust → `briquette(WOOD)` | `powder_sawdust` **is** registered (`BilletPowderItems.java`); coal/lignite dust status not individually re-verified. | `EnumBriquetteType` enum **exists** in `ItemEnums.java` but **zero items are registered against it anywhere** (grepped) — the entire `briquette_*` family is enum-only, matching the same "declared but not consumed" pattern found for casing/circuit/pages below. | **Blocked** (all 3) |
| PLATE (13, ingot → plate) | Underlying materials (`MAT_IRON`, `MAT_GOLD`, `MAT_TITANIUM`, `MAT_ALUMINIUM`, `MAT_STEEL`, `MAT_LEAD`, `MAT_COPPER`, `MAT_SCHRABIDIUM`, `MAT_GUNMETAL`, `MAT_WEAPONSTEEL`, `MAT_SATURN`≈BIGMT, `MAT_DURA`) all confirmed present in `Mats.java`; `MAT_CMB` (combine steel) **not found** as a distinct constant. Vanilla iron/gold/copper ingots also available. | **All 13 `plate_*` output items already registered**, confirmed by name in `PlateCrystalWasteItems.java:145-177` (`PLATE_IRON, PLATE_COPPER, PLATE_TITANIUM, PLATE_ALUMINIUM, PLATE_GOLD, PLATE_LEAD, PLATE_STEEL, PLATE_COMBINE_STEEL, PLATE_SATURNITE, PLATE_SCHRABIDIUM, PLATE_DURA_STEEL, PLATE_GUNMETAL, PLATE_WEAPONSTEEL`). | **12 of 13 ready** (all except `plate_combine_steel`'s `CMB` ingot input, not independently confirmed) — **this is the single healthiest sub-group in either file** |
| Casings (4, C9/C50) | `GUNMETAL.plate()`/`WEAPONSTEEL.plate()` map to the just-confirmed `plate_gunmetal`/`plate_weaponsteel` items. | `EnumCasingType` enum **exists** (`ItemEnums.java:122-126`, 7 constants including all 4 this file needs) but **zero items registered against it anywhere** (grepped `EnumCasingType` outside `ItemEnums.java` — only found in unrelated ammo-casing comments/TODOs in the gun-item package, never consumed for a `casing_*` item). | **Blocked** (all 4) — ingredient ready, output family entirely unregistered |
| WIRE (loop, ≈11) | Ingot items for the loop's materials mostly exist per the PLATE row above. | `wire_fine` **not registered anywhere in this port** (grepped — 5 separate `TODO`/javadoc comments across `SpecialItems.java`, `EntityMissileTier0.java`, `EntityHunterChopper.java`, `EntityTaintCrab.java`, `BlockJungleCrate.java` all independently confirm "not yet registered in this port"). | **Blocked** (all ≈11) — single missing item blocks the entire sub-family |
| CIRCUIT (1) | `SI.billet()` → `Mats.MAT_SILICON` exists, has `BILLET` in autogen. | `EnumCircuitType` enum **exists** (`ItemEnums.java:134-157`, all 20 CE constants including `SILICON`) but **zero items registered against it** (grepped — only consumed today as a comment/label inside `ModRecipeProvider.java`'s own "deliberately not ported" javadoc list). | **Blocked** — same enum-only pattern as casings |
| PRINTING (8) | `Items.PAPER` vanilla, always ready. | `EnumPages` enum **exists** (`ItemEnums.java:103-114`, all 8 `PAGE1..8`) but **zero `page_of_*` items registered** (grepped). | **Blocked** (all 8) — same pattern again |
| Stamp items (all groups, secondary input) | **All 40 stamp items already registered** (`MachineItems.java`, see "Already covered" #2) — every `StampType` this file needs has ≥1 real stamp item behind it. | — | **Fully ready** |

**Verdict: of PressRecipes' ≈48 entries, roughly 12-13 (the PLATE group, minus `plate_combine_steel`)
are "ready to port now" pending only the `OreDictManager`-alias/`Mats.MAT_*` rewrite** noted above; the
remaining ≈35 are blocked, and the blocking pattern is strikingly uniform: **`EnumCasingType`,
`EnumCircuitType`, `EnumPages`, and `EnumBriquetteType` are all four already ported as bare enums in
`ItemEnums.java` (matching CE's enum definitions exactly) but none of the four has a single item
registered against it anywhere in this port** — a "declared the type, never consumed it" gap distinct
from (and narrower/more mechanical than) the Crucible-driven `MaterialItemGenerator`/`IngotNuggetItems`
gaps `docs/phase6/recipe_graph_audit.md` already root-causes at length. Registering these four small
item families (casing ×4, circuit ×1 needed here [×20 total enum], pages ×8, briquette ×3) plus
`wire_fine` would unblock the large majority of PressRecipes' currently-blocked entries with no Crucible
dependency at all.

### FractionRecipes (18 entries)

| Family | Status |
|---|---|
| All 40 distinct fluid names referenced (`HEAVYOIL`, `BITUMEN`, `SMEAR`, `HEAVYOIL_VACUUM`, `HEATINGOIL_VACUUM`, `HEATINGOIL`, `LUBRICANT`, `NAPHTHA`, `DIESEL`, `NAPHTHA_DS`, `XYLENE`, `DIESEL_REFORM`, `NAPHTHA_CRACK`, `DIESEL_CRACK`, `LIGHTOIL`, `KEROSENE`, `LIGHTOIL_DS`, `KEROSENE_REFORM`, `LIGHTOIL_CRACK`, `PETROLEUM`, `COALOIL`, `COALGAS`, `OIL`, `COALCREOSOTE`, `REFORMATE`, `AROMATICS`, `LIGHTOIL_VACUUM`, `REFORMGAS`, `EGG`, `CHOLESTEROL`, `RADIOSOLVENT`, `OIL_COKER`, `CRACKOIL`, `NAPHTHA_COKER`, `GAS_COKER`, `CARBONDIOXIDE`, `CHLOROCALCITE_MIX`, `CHLOROCALCITE_CLEANED`, `COLLOID`, `BAUXITE_SOLUTION`, `REDMUD`, `SODIUM_ALUMINATE`) | **All 40/40 confirmed present** as `FluidType` field declarations in this port's own `src/main/java/com/hbm/inventory/fluid/Fluids.java` (individually grepped, one-by-one, 100% hit rate). |
| `FluidStack`, `Tuple.Pair` | Both already exist and already used identically in `RefineryRecipes.java`. |

**Verdict: 18 of 18 entries are "ready to port now" on the ingredient/output side** — every fluid this
file needs already exists in the port's fluid registry, and the data types (`FluidStack`, `Tuple.Pair`)
and structural precedent (`RefineryRecipes.java`) are already committed. **This is the single most
"ready to port" of the three files, and one of the most ready-to-port machine-recipe files found in any
Phase 7 research task** — the only missing piece is the `FractionRecipes` class itself (trivial, ≈40
lines given the precedent) and, separately/out-of-scope, the actual Fraction Tower block/block-entity.

## Recommended 1.21.1 implementation shape

**FractionRecipes → plain bespoke Java data class, structurally a near-twin of the port's own
`RefineryRecipes.java`.** Not a vanilla-JSON `Recipe<?>` at all: CE never makes this data-driven in any
way a JSON schema would improve on (its own `readRecipe`/`writeRecipe` exist only for CE's generic
in-game recipe-override tooling, not for player-facing datapacks), and this port already made — and
justified — the identical call for the structurally-identical `RefineryRecipes` ("the lower-risk option
that does not block on the recipe/datagen cross-cutting work landing first"). Concretely: a
`Map<FluidType, Tuple.Pair<FluidStack, FluidStack>>` populated once by a `registerFractions()` method,
18 literal `.put(...)` calls copied directly from the catalog above (fluid names need no translation —
they're already registered under the same names), plus `getFractions(FluidType)` and an
`getAllFractions()`-style full-collection accessor matching `RefineryRecipes.getAllRefinery()`'s already-
established pattern for JEI consumption. This is the lowest-effort, lowest-risk of the three files by a
wide margin, and has zero item-registry blockers.

**PressRecipes → needs a design decision the implement wave should make explicitly, not default past.**
Two real options, both defensible given what's already committed:
1. **Bespoke Java `Map`** (same shape as CE: `Map<Pair<AStack, ItemStamp.StampType>, ItemStack>`),
   following the `RefineryRecipes`/`FractionRecipes` non-JSON precedent. Fastest, lowest-risk, matches
   the fact that CE's own match logic (`getOutput`, a linear scan over stamp-type + `AStack.matchesRecipe`)
   is not itself vanilla-Ingredient-shaped either.
2. **A genuine custom `Recipe<?>`/`RecipeType`/`RecipeSerializer`**, extending this port's own
   `HbmSimpleRecipe` family — which its own javadoc already earmarks `PressRecipes` for as a "tier 1"
   consumer. **This needs a real fix, not a direct reuse**: `HbmSimpleRecipe` as committed today is
   single-`Ingredient`-in; CE's actual Press recipe is a **two-input match** (material + which stamp item
   was inserted, dispatched by the stamp's `StampType`, not by stamp item identity). The clean way to
   make that JSON-drivable while staying close to `HbmSimpleRecipe`'s existing codec pattern: define a
   small sibling `HbmStampRecipe implements Recipe<TwoSlotRecipeInput-or-equivalent>` with **two**
   `Ingredient` fields (material + stamp), where the "stamp" `Ingredient` is backed by a new per-
   `StampType` item tag (e.g. `#hbm:stamps/flat` containing all 5 tiered flat-stamps + any future
   additions) rather than hardcoding stamp item ids — this reproduces CE's actual "any stamp of this
   *type* works" semantics precisely, using a mechanism (tags) this port's own `OreDictStack` already
   leans on for the material side. Recommend option 2 if the implement wave has bandwidth for the small
   amount of new codec/tag work, since it's what this port's own design docs already commit to and keeps
   Press consistent with the shredder/centrifuge JSON-recipe family; recommend option 1 as a pragmatic
   fallback if time is short, since CE's own match semantics don't map onto option 2 cleanly for the
   WIRE loop's dynamic per-material generation anyway (that sub-family may be easier to keep as a
   generated Java table regardless of which shape the rest of the file takes).
   Either way, **before writing any of this**, register the four small missing item families identified
   above (`wire_fine`, `casing_{small,large,small_steel,large_steel}`, `circuit_silicon` [+ the other 19
   `EnumCircuitType` constants if that family is being completed anyway], `page_of_page{1..8}`,
   `briquette_{coal,lignite,wood}`) — this unblocks the majority of the file's entries independently of
   which recipe-shape decision is made, and does not require the Crucible.

**AnnihilatorRecipes → custom Java data class (milestone table) + a NeoForge `SavedData` counter, not a
`Recipe<?>` of any kind.** This is not a "craft A, get B" transform — it's a cumulative-deposit/threshold-
reward mechanic (feed material into the machine over time; once a running total crosses a `BigInteger`
threshold, grant a one-time blueprint payout). No vanilla or custom `Recipe<?>` abstraction fits this
shape; CE itself backs it with a bespoke `WorldSavedData` (`AnnihilatorSavedData`, 215 lines) tracking
per-pool cumulative amounts, checked against this file's milestone table by
`getHighestPayoutFromRecipe`'s pure-algorithm logic (lines 180-194, no engine API dependency — portable
almost verbatim). **Recommend deprioritizing this file specifically**: every one of its 13 entries is
gated behind CE's own `enable528` flag (default `false` in CE, and this port already carries the same
flag defaulting the same way), so CE itself ships this content off-by-default as opt-in "528 mode"
expansion material; and its only outputs are `ItemBlueprints` research-unlock items, not concrete
craftables, so porting it moves none of the count-parity or reachability numbers `docs/phase6/
PARITY_REPORT.md`/`recipe_graph_audit.md` cite as the reason for this phase. If picked up regardless, the
milestone table itself is cheap (13 lines' worth of data, half already-blocked on the same missing
`ammo_standard`/`pellet_charged`/`circuit` items PressRecipes also needs), but the `SavedData` class and
the still-nonexistent `TileEntityMachineAnnihilator` block-entity are a materially larger lift than either
of the other two files in this assignment.

## Open questions / risks

1. **The `OreDictManager` short-alias layer is a real, if small, compile-blocker for a literal port of
   any of these three files.** All three CE files open with `import static
   com.hbm.inventory.OreDictManager.*;` and reference `IRON`, `TI`, `SA326`, `CU`, `SR`, `U`, `SI`,
   `FERRO`, `RUBBER`, `BI`, `CMB`, `GUNMETAL`, `WEAPONSTEEL`, `BIGMT`, `DURA`, `NETHERQUARTZ`, `LAPIS`,
   `DIAMOND`, `EMERALD`, `COAL`, `LIGNITE`, `ANY_COKE`, `ANY_PLASTIC`, `ANY_HARDPLASTIC`,
   `ANY_RESISTANTALLOY` as bare identifiers. This port's `Mats.java` has no equivalent alias class — the
   implement wave needs to either add one (mirroring CE's `OreDictManager.java`, not read by this task —
   out of scope, flagged for whoever picks this up) or rewrite every reference to the port's own
   `Mats.MAT_*` naming. This affects compile-ability, not design, so it's a small but real known cost not
   otherwise visible from the recipe tables alone.
2. **The WIRE loop's exact recipe count (≈11) was not individually verified against CE's second
   condition** (`OreDictionary.doesOreNameExist(MaterialShapes.INGOT.make(mat))`) for each of the 11
   WIRE-autogen materials — this task counted `setAutogen(...WIRE...)` occurrences directly (exact) but
   did not cross-check each material's ingot ore-dict presence in CE's own registry (would require
   loading CE's live OreDictionary state, not statically derivable from source alone within this task's
   budget). Treat ≈11 as an upper bound; the true count is ≤11.
3. **`plate_combine_steel`'s ingredient (`CMB.ingot()`) status is unconfirmed** — no `MAT_CMB` constant
   was found in this port's `Mats.java` by name, but combine-steel may exist under a different
   constant name (not exhaustively searched given this task's file-scope boundary staying on the three
   named recipe files rather than a full `Mats.java` audit). Worth a quick targeted check before the
   implement wave marks this one PLATE entry blocked vs. ready.
4. **Whether `MaterialItemGenerator` actually emits an item for every shape a material's
   `setAutogen(...)` list names, with zero exceptions, was assumed rather than independently re-verified
   for the specific PLATE-group ingots and the emerald-dust FLAT entry.** The class exists and its
   pattern was read (`AUTOGEN_SHAPES` × `Mats.orderedList` nested loop, gated per-material by that
   material's own `autogen` set) — high confidence this is correct, but not literally executed/compiled
   to confirm.
5. **This task deliberately did not investigate `TileEntityMachineAnnihilator`/`TileEntityMachinePress`/
   `TileEntityMachineFractionTower`'s internal logic** (durability consumption on stamps, power/duration
   costs, GUI slot layout) beyond confirming their existence and line counts — that's machine-block scope,
   a separate task from this recipe-data assignment, and is called out here only so the implement wave
   knows recipe-data alone won't make any of these three machines functional.
6. **`upstream/neo-edition` already has a complete, real NeoForge 1.21.1 Press port** (`MachinePressBlock`,
   `MachinePressBlockEntity`, `MachinePressMenu`, `MachinePressScreen`, `PressRecipeHandler`,
   `RenderPress`, plus its own `PressRecipes.java`) — confirmed to exist by directory listing only, per
   this task's ground rules **not read for content/behavior** (that project is explicitly out of bounds
   for anything beyond confirming API shape). Flagging its existence for the implement wave: it is a
   legitimate, in-bounds reference for **NeoForge 1.21.1 API shape only** (how a `RecipeType`/
   `RecipeSerializer`/menu/screen is wired together in this exact `neo_version`), already cited once in
   this port's own `ITickableBE.java` javadoc for that narrow purpose — but CE (`upstream/hbm-ce`) must
   remain the sole source for what the Press's actual recipes/behavior/content should be.
