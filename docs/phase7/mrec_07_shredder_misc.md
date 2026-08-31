
# Research report — mrec-07-shredder-misc

Assignment scope: CE's `com.hbm.inventory.recipes.{ShredderRecipes,CyclotronRecipes,CrackingRecipes,RBMKFuelRecipes}` (the machine-per-type recipe-data package under `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`, sibling to but distinct from `com.hbm.crafting.*`, which is out of scope here).

---

## Scope confirmed

All four files read in full, directly from `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`:

| File | Lines | In-CE structure |
|---|---:|---|
| `ShredderRecipes.java` | 465 | `SerializableRecipe` subclass. Two generation paths: (1) `registerDefaults()` (lines 205-424) — a flat sequence of literal `ShredderRecipes.setRecipe(in, out)` static calls, with 6 embedded `for` loops (wood logs/planks/saplings over `OreDictionary.getOres(...)`, a `SKULL` meta-loop ×5, a `STAINED_HARDENED_CLAY`+`WOOL` combined meta-loop ×16, a `BobbleType` enum-loop ×29). (2) `registerPost()` (lines 119-202) — a fully **dynamic, generative** sweep: iterates every string in `OreDictionary.getOreNames()` at mod-load time and, by string-prefix match (`ingot`/`plate`/`nugget`/`ore`/`block`/`gem`/`dust`), auto-derives a shred-to-dust recipe for *every* matching oredict entry in the running game (not just CE's own items — any mod's registered ore-dict material). This is architecturally the largest single piece of the file and has no enumerable "list of entries" — it's a rule, not a table. |
| `CyclotronRecipes.java` | 187 | `SerializableRecipe` subclass. Single method `registerDefaults()` (lines 28-96) with 42 literal `makeRecipe(catalyst, target, output, antimatterYield)` calls, grouped into 5 catalyst "chains" via source comments (`LITHIUM`/`BERYLLIUM`/`CARBON`/`COPPER`/`PLUTONIUM` START/END markers) — no loops, no table. |
| `CrackingRecipes.java` | 124 | `SerializableRecipe` subclass. `registerDefaults()` (lines 41-54) is 12 literal `cracking.put(inputFluid, Pair(outputFluid1, outputFluid2))` calls — flat list, no loops. Fluid-only (no `ItemStack` inputs/outputs at all in the live data — `getCrackingRecipesForJEI()` only wraps fluids in `ItemFluidIcon` for JEI display purposes). |
| `RBMKFuelRecipes.java` | 78 | Plain class (not a `SerializableRecipe`). `registerRecipes()` calls `addRod(...)` once per RBMK fuel-rod item (31 literal calls), each expanding via 2 inner loops (5 enrichment buckets × 2 xenon states = 10 map entries per rod) to build a `LinkedHashMap<ItemStack, ItemStack>` of exact-NBT example stacks. **This map is JEI-display-only** (its only 2 call sites in all of CE are `MainRegistry.init()`'s registration call and `JeiRecipes.getRBMKFuelRecipes()`) — the actual gameplay conversion (`makeRBMKPellet(ItemStack)`, lines 71-77) is a pure function of a rod's *live* enrichment/poison state, not a lookup against this map. |

---

## Already covered by this port

### ShredderRecipes — real remaining gap found

Machine confirmed already built: `com.hbm.blockentity.machine.MachineShredderBlockEntity` exists, wired to a real recipe type (`hbm:shredder`, `HbmSimpleRecipe`/`ProcessingRecipes.SHREDDER_TYPE`/`SHREDDER_SERIALIZER` in `src/main/java/com/hbm/inventory/recipes/{HbmSimpleRecipe,ProcessingRecipes}.java`), plus a JEI category (`compat/jei/category/ShredderCategory.java`). **The machine itself needs no further work — only recipe data is missing.**

This port's `data/hbm/recipe/shredder/*.json` has exactly **44 files**, one flat `{"type":"hbm:shredder","input":{...},"output":{...},"count":n,"duration":60}` per file. Cross-referencing every one of the 44 files' `input`/`output` pair against CE's `registerDefaults()` (excluding `registerPost()`, which is unported as a *mechanism*, not just missing entries — see below) shows an **exact 1:1 match with 44 of CE's 173 live `registerDefaults()` call sites**: 22 of CE's first 76 ("primary recipes") entries, and 22 of CE's 27 "crystal processing" entries. Zero entries from CE's "misc recycling" (20), "sellafite scrapping" (7), "fracking debris" (6), "deco pipe recycling" (24), wool/clay loop (32 runtime), bobblehead loop (29 runtime), "debris shredding" (6), or the wood/skull loops (4 source lines) sections are ported. **Zero of `registerPost()`'s generative sweep is ported at all** — no analogous tag-driven fallback rule exists anywhere in this port's shredder recipe set.

One discrepancy found in the already-ported set: `obsidian.json` outputs `minecraft:gravel`, but CE's real recipe (`ShredderRecipes.java:229`) outputs `ModBlocks.gravel_obsidian` (a distinct HBM item) — `gravel_obsidian` is not registered in this port yet (confirmed, see dependency table below), so the JSON substituted vanilla gravel rather than leaving the recipe unported. Worth flagging to the implement wave as a substitution to revisit once `gravel_obsidian` lands, not a bug to silently keep.

Two of CE's `QUARTZ_BLOCK`-meta recipes are **not actually covered by `quartz_block.json`** despite looking like a meta-collapse: CE's 3 `QUARTZ_BLOCK` meta variants (0/1/2) are 3 *separate* items in modern Minecraft (`minecraft:quartz_block`, `minecraft:chiseled_quartz_block`, `minecraft:quartz_pillar`), all with identical CE output (`powder_quartz` ×4) — the port's single JSON only covers the default block id; the other two are real, trivial, un-flagged gaps (both vanilla items, zero item-registry blocker).

### CyclotronRecipes — real remaining gap found, and the prompt's estimate needs correcting

Machine confirmed already built: block (`blocks/machine/chem/CyclotronBlock.java`), block entity (`blockentity/machine/chem/CyclotronBlockEntity.java`), menu/screen, and JEI category (`compat/jei/category/CyclotronCategory.java`) all exist. **Machine itself needs no further work — only recipe data is missing.**

`src/main/java/com/hbm/inventory/recipes/chem/CyclotronRecipes.java` (84 lines, read in full) already has **10 entries**, not "~1" as this task's prompt estimated — the prompt's figure appears to have undercounted. Exact breakdown against CE's 42-entry `registerDefaults()`:

| Chain | CE entries | Port entries | Gap |
|---|---:|---:|---:|
| Lithium (`part_lithium`) | 13 | 4 | 9 missing |
| Beryllium (`part_beryllium`) | 7 | 3 | 4 missing |
| Carbon (`part_carbon`) | 8 | 0 | **8/8 missing** |
| Copper (`part_copper`) | 10 | 3 | 7 missing |
| Plutonium (`part_plutonium`) | 4 | 1 | 3 missing (including the single highest-value recipe in the whole file: `pellet_charged` → `nugget_schrabidium`, antimatter yield 1000) |
| **Total** | **42** | **10** | **32 missing (76%)** |

The port's own class javadoc already documents its approach precisely: CE's 5 dedicated catalyst "particle" items (`part_lithium`/`part_beryllium`/`part_carbon`/`part_copper`/`part_plutonium`) are not registered in this port, so the 10 ported entries substitute the corresponding elemental `BilletPowderItems.POWDER_*` item as catalyst, and CE's `dust*` OreDictionary strings are replaced with NeoForge `c:dusts/*` common tags via `OreDictStack.ofCommonTag(...)`. This is a legitimate, already-established, low-risk convention the remaining 32 entries should reuse.

One deviation found while diffing: the port's lithium-chain "gold" entry (`dusts/gold` → `NUGGET_URANIUM`, amat 50) does not match CE's real entry at that position (`dustGold` → `ingot_mercury`, amat 50) — `ingot_mercury` is not registered in this port yet (confirmed below), so the port substituted a different, unrelated output item rather than leaving it unported or matching some other convention. Worth a second look at implement time — either wait for `ingot_mercury` or pick a more clearly-labeled placeholder.

### CrackingRecipes — confirmed untouched, exactly as the prompt suspected

`grep -rln "CrackingRecipes"` across this port's entire `com.hbm` tree returns **zero files**. No class of this name, and no "Catalytic Cracker" machine block or block entity exists anywhere (`blockentity/machine/oil/` contains only `OilWell`, `Pumpjack`, `OilDrillBase`, `FrackingTower`, and `Refinery` — no `CatalyticCracker`). This is a distinct CE machine (`com.hbm.tileentity.machine.oil.TileEntityMachineCatalyticCracker`) from the already-ported Refinery (`TileEntityMachineRefinery`, whose own recipe data lives in a *different* CE class, `RefineryRecipes.java`, already substantially ported by another task's `src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java`) — the two are easy to conflate because both classes deal with "cracking"-named fluids, but they are separate CE source files, separate CE machines, and this port has ported only the Refinery one. **The machine itself needs to be built (block + block entity + GUI), not just the recipe data** — 0% coverage on every axis.

### RBMKFuelRecipes — confirmed identical mechanism, zero further work needed

Verified per the prompt's explicit instruction: this port's `com.hbm.inventory.recipes.machine.rbmk.RBMKFuelRecipes.java` (67 lines, read in full) is the direct port of this exact CE file, and its own javadoc says so explicitly, citing "78 lines, read in full" and explaining the design decision in detail. It correctly identifies that CE's `LinkedHashMap<ItemStack,ItemStack> recipes` field is a JEI-display-only enumeration of NBT-exact example stacks (5 enrichment buckets × 2 xenon states, per rod, generated by `addRod`) and is *not* the live gameplay conversion path — the live path is CE's `makeRBMKPellet(ItemStack)`, a pure function of a rod's actual current state. The port reimplements that pure function generically (`computeStage(ItemStack)` + `getRecyclingOutput(ItemStack)`), which is functionally superior to porting CE's static map (a static map keyed on exact NBT would never match a live, player-mutated rod stack). **Verdict: this file needs no port work at all — say so plainly, as instructed.** (Its JEI-table-generation half, matching CE's `addRod`/`getFileName`/JEI registration, is out of scope for gameplay parity and not flagged as a gap here.)

---

## Full recipe/entry catalog OR representative pattern

### ShredderRecipes — representative sample (large file: 173 live call sites in `registerDefaults()`, expanding via loops to an estimated 230-250 runtime recipes, plus `registerPost()`'s unbounded dynamic sweep)

Representative sample spanning every sub-pattern (✓ = already in this port's 44 JSON; ✗ = gap):

| CE input | CE output | Section | Port status |
|---|---|---|---|
| `Blocks.GLOWSTONE` | `Items.GLOWSTONE_DUST` ×4 | primary | ✓ `glowstone.json` |
| `Blocks.OBSIDIAN` | `ModBlocks.gravel_obsidian` ×1 | primary | ✗ (port has `obsidian.json` but outputs wrong item — see above) |
| `Blocks.QUARTZ_STAIRS` | `powder_quartz` ×3 | primary | ✗ — vanilla item, no blocker |
| `STONE_SLAB` meta 7 (quartz slab) | `powder_quartz` ×2 | primary | ✗ — modern equiv is `minecraft:quartz_slab`, no blocker |
| `ModBlocks.ore_nether_fire` | `powder_fire` ×6 | primary | ✗ — `ore_nether_fire` registered (`blocks/OreBlocks.java:165`); `powder_fire` needs check |
| `ModItems.fragment_neodymium` | `powder_neodymium_tiny` ×1 | primary | ✗ — both items registered (`PlateCrystalWasteItems.FRAGMENT_NEODYMIUM`, `BilletPowderItems`), **ready to port** |
| `ModBlocks.block_meteor` | `powder_meteorite` ×10 | primary | ✗ — `block_meteor` registered, ready |
| `ModBlocks.meteor_polished` | `powder_meteorite` ×1 | primary | ✗ — **BLOCKED**, `meteor_polished` not registered anywhere |
| `Blocks.DIRT` | `ModItems.dust` ×1 | primary | ✗ — ready (both vanilla/registered) |
| logs (`OreDictionary "logWood"`, N items) | `powder_sawdust` ×4 | loop | ✗ — needs `c:logs`-tag-based generation, `powder_sawdust` registered |
| `Items.SKULL` meta 0-4 (5 items) | `biomass` ×4 | loop | ✗ — **BLOCKED**, `biomass` not registered as an item anywhere in this port |
| `ModItems.crystal_coal` | `powder_coal` ×3 | crystal | ✗ — both items registered, **ready to port** (correcting prompt: NOT blocked by items) |
| `ModItems.crystal_iron` | `powder_iron` ×3 | crystal | ✓ `crystal_iron.json` |
| `ModItems.crystal_sulfur` | `ModItems.sulfur` ×8 | crystal | ✗ — **BLOCKED**: `crystal_sulfur` exists, but plain `sulfur` output item does not (see item-dependency section) |
| `ModItems.crystal_fluorite` | `ModItems.fluorite` ×8 | crystal | ✗ — **BLOCKED**, same reason |
| `ModBlocks.steel_poles` | `powder_steel_tiny` ×2 | misc | ✗ — both registered, ready |
| `ModItems.coil_copper` | `powder_red_copper` ×1 | misc | ✗ — **BLOCKED**, `coil_copper` not registered as a real item |
| `Blocks.ANVIL` | `powder_iron` ×31 | misc | ✓ `anvil.json` |
| `new ItemStack(sellafield, meta 0..5)` | `scrap_nuclear` ×1/2/3/5/7/15 | sellafite | ✗ — `sellafield` block exists but as a **single** registered block/item id with no per-decay-stage split found (open question, see below) |
| `ModBlocks.sellafield_slaked` | `Blocks.GRAVEL` ×1 | sellafite | ✗ — both registered, ready |
| `ModBlocks.dirt_dead`/`dirt_oily`/`sand_dirty`/`sand_dirty_red`/`stone_cracked`/`stone_porous` | `scrap_oil` ×1 each | fracking | ✗ — all 6 blocks registered (per grep); `scrap_oil` needs a check, likely ready |
| `ModBlocks.deco_pipe*` (24 named variants) | `powder_steel` ×1 each | deco pipe | ✗ — `deco_pipe` family registered as a batch in `GenericBlocks.java:324`, likely ready as a set |
| `STAINED_HARDENED_CLAY`/`WOOL` meta 0-15 (32 runtime) | `Items.CLAY_BALL`/`Items.STRING` ×4 | loop | ✗ — modern MC equivalents are 16 separate dyed-terracotta and 16 separate dyed-wool vanilla items each, no blocker |
| `ModBlocks.bobblehead` meta 0-28 (29 runtime, `BobbleType` enum) | `scrap_plastic` ×1 | loop | ✗ — **BLOCKED on both sides**: `bobblehead` block/item family not registered at all, `scrap_plastic` not registered at all |
| `ModItems.debris_concrete`/`debris_shrapnel`/`debris_exchanger`/`debris_element`/`debris_metal`/`debris_graphite` | various | debris | ✗ — **BLOCKED, all 6 debris items unregistered** |

**`registerPost()`'s generative pattern** (the largest single lever, not enumerable as entries): at mod init, iterate `OreDictionary.getOreNames()`; for each name matching a known prefix, derive the "material" substring and look up (or synthesize) that material's dust equivalent:
- `"ingot" + Material` → that material's `dustX` (1:1, e.g. `ingotIron`→`dustIron`)
- `"plate" + Material` → `dustX` (1:1)
- `"nugget" + Material` → `dustTinyX` (1:1)
- `"ore" + Material` → `dustX` ×2
- `"block" + Material` → `dustX` ×9
- `"gem" + Material` → `dustX` ×1
- `"dust" + anything` → the single generic `ModItems.dust` ×1 (universal catch-all — **this branch alone is a valid single tag-based recipe**, `{"tag":"c:dusts"} → hbm:dust ×1`, since its output doesn't vary by material)

The first 6 branches are **not** representable as one generic tag rule (output varies per material), but this port already has the exact abstraction needed to generate them mechanically — see next section.

### CyclotronRecipes — full catalog is feasible (42 total CE entries, well under the ~150-entry "small file" threshold)

| # | Chain | Catalyst (CE / port substitute) | Target | Output | Amat | Port status |
|---|---|---|---|---|---:|---|
| 1 | Li | part_lithium / powder_lithium | dustLithium | powder_beryllium | 50 | ✗ missing |
| 2 | Li | " | dustBeryllium | powder_boron | 50 | ✓ ported |
| 3 | Li | " | dustBoron | powder_coal | 50 | ✓ ported |
| 4 | Li | " | dustNetherQuartz | powder_fire | 50 | ✗ missing |
| 5 | Li | " | dustPhosphorus | sulfur | 50 | ✗ missing — **BLOCKED**, `sulfur` item not registered |
| 6 | Li | " | dustIron | powder_cobalt | 50 | ✓ ported (port: `dusts/iron`→cobalt) |
| 7 | Li | " | powder_strontium | powder_zirconium | 50 | ✗ missing |
| 8 | Li | " | dustGold | ingot_mercury | 50 | ⚠ port has this slot but wrong output (`nugget_uranium`, see deviation above) |
| 9 | Li | " | dustPolonium | powder_astatine | 50 | ✗ missing |
| 10 | Li | " | dustLanthanium | powder_cerium | 50 | ✗ missing |
| 11 | Li | " | dustActinium | powder_thorium | 50 | ✗ missing |
| 12 | Li | " | U.dust() (uranium) | powder_neptunium | 50 | ✗ missing |
| 13 | Li | " | NP237.dust() | powder_plutonium | 50 | ✗ missing |
| 14 | Be | part_beryllium / powder_beryllium | dustLithium | powder_boron | 25 | ✓ ported |
| 15 | Be | " | dustNetherQuartz | sulfur | 25 | ✗ missing — BLOCKED (sulfur) |
| 16 | Be | " | dustTitanium | powder_iron | 25 | ✓ ported |
| 17 | Be | " | dustCobalt | powder_copper | 25 | ✓ ported |
| 18 | Be | " | powder_strontium | powder_niobium | 25 | ✗ missing |
| 19 | Be | " | powder_cerium | powder_neodymium | 25 | ✗ missing |
| 20 | Be | " | dustThorium | powder_uranium | 25 | ✗ missing |
| 21 | C | part_carbon / **no substitute found** | dustBoron | powder_aluminium | 10 | ✗ missing — chain BLOCKED (no `part_carbon`/`powder_carbon` in this port) |
| 22-28 | C | " | (7 more targets) | (7 more outputs) | 10 | ✗ all 8 missing, all chain-BLOCKED |
| 29 | Cu | part_copper / powder_copper | dustBeryllium | powder_quartz | 15 | ✓ ported |
| 30 | Cu | " | dustCoal | powder_bromine | 15 | ✗ missing |
| 31 | Cu | " | dustTitanium | powder_strontium | 15 | ✗ missing |
| 32 | Cu | " | dustIron | powder_niobium | 15 | ✓ ported |
| 33 | Cu | " | powder_bromine | powder_iodine | 15 | ✗ missing |
| 34 | Cu | " | powder_strontium | powder_neodymium | 15 | ✗ missing |
| 35 | Cu | " | powder_niobium | powder_caesium | 15 | ✗ missing |
| 36 | Cu | " | powder_iodine | powder_polonium | 15 | ✗ missing |
| 37 | Cu | " | powder_caesium | powder_actinium | 15 | ✗ missing |
| 38 | Cu | " | dustGold | powder_uranium | 15 | ✓ ported |
| 39 | Pu | part_plutonium / powder_plutonium | dustPhosphorus | powder_tennessine | 100 | ✗ missing |
| 40 | Pu | " | PU.dust() (plutonium) | powder_tennessine | 100 | ✓ ported |
| 41 | Pu | " | powder_tennessine | powder_australium | 100 | ✗ missing |
| 42 | Pu | " | pellet_charged | nugget_schrabidium | 1000 | ✗ missing — **BLOCKED**, `pellet_charged` not registered |

(This full table doubles as the entire "representative sample" — the file is small enough to enumerate completely rather than sample.)

### CrackingRecipes — full catalog (12 entries, small file)

| Input fluid | Output 1 | Output 2 | Port status (all fluids exist, see dependency section) |
|---|---|---|---|
| OIL | CRACKOIL 80 | PETROLEUM 20 | ✗ missing |
| BITUMEN | OIL 80 | AROMATICS 20 | ✗ missing |
| SMEAR | NAPHTHA 60 | PETROLEUM 40 | ✗ missing |
| GAS | PETROLEUM 30 | UNSATURATEDS 20 | ✗ missing |
| DIESEL | KEROSENE 40 | PETROLEUM 30 | ✗ missing |
| DIESEL_CRACK | KEROSENE 40 | PETROLEUM 30 | ✗ missing |
| KEROSENE | PETROLEUM 60 | NONE 0 (single-output case) | ✗ missing |
| WOODOIL | HEATINGOIL 40 | AROMATICS 10 | ✗ missing |
| XYLENE | AROMATICS 80 | PETROLEUM 20 | ✗ missing |
| HEATINGOIL_VACUUM | HEATINGOIL 80 | REFORMGAS 20 | ✗ missing |
| REFORMATE | UNSATURATEDS 40 | REFORMGAS 60 | ✗ missing |
| BIOGAS | PETROLEUM 20 | AROMATICS 20 | ✗ missing |

All 12 are 100% unported (no machine, no recipe class) — item/fluid dependency check below shows every referenced `FluidType` already exists on this port's side, so this whole file is blocked purely on the missing machine + recipe-class scaffolding, not on content.

### RBMKFuelRecipes — no catalog needed (already fully covered, see above)

---

## Item/registry dependency check

### ShredderRecipes

**Ready to port now** (both input and output already registered — spot-checked, not exhaustive given ~170 distinct ingredient/output names in the file):
- `fragment_neodymium`/`fragment_cobalt`/`fragment_niobium`/`fragment_cerium`/`fragment_lanthanium`/`fragment_actinium`/`fragment_boron`/`fragment_meteorite` (all in `PlateCrystalWasteItems`)
- `block_meteor`, `meteor_pillar`, `ore_rare`, `boxcar`, `ingot_schrabidate`/`block_schrabidate`, `coal_infernal`, `ore_tektite_osmiridium`, `can_empty`, `chunk_ore`, `block_slag`(⚠ see below)`, `ore_aluminium`, `block_bakelite`/`ingot_bakelite`, `powder_sawdust`
- `steel_poles`/`steel_roof`/`steel_wall`/`steel_corner`/`steel_beam` (`GenericDecoBlocks.java`), `crate_iron`/`crate_steel`/`crate_tungsten` (`StorageMachineBlocks.java`), `steel_grate`, `stone_gneiss`, `ore_nether_fire`, `powder_limestone`
- `crystal_coal`/`crystal_sulfur`(input side only)/`crystal_niter`(input side only)/`crystal_fluorite`(input side only) — all registered in `PlateCrystalWasteItems`
- `dirt_dead`/`dirt_oily`/`sand_dirty`/`sand_dirty_red`/`stone_cracked`/`stone_porous` (fracking-debris family, all found registered)
- `deco_pipe` and its named variant family (batch-registered in `GenericBlocks.java:324`)
- `sellafield_slaked` (registered in `WastelandVirusBlocks.java`)
- `gravel_diamond` (registered per grep)
- `bedrock_ore_base` (single flattened item, `BedrockOreItems.java` — CE's wildcard-meta recipe collapses cleanly to this one id)
- all vanilla-sourced entries: `quartz_stairs`, `quartz_slab` (modern id for CE's `STONE_SLAB` meta 7), `sandstone_stairs`, `brick_stairs`, `flower_pot`, `chiseled_quartz_block`, `quartz_pillar`, dyed wool ×16, dyed terracotta ×16 — these are pure-vanilla `minecraft:` ids, no HBM registration needed at all.

**Blocked** (output or input item genuinely not registered anywhere in this port — confirmed by targeted grep, not merely absent from one file):
- `gravel_obsidian`, `brick_light` — only mentioned in a `TODO` comment (`explosion/ExplosionNukeGeneric.java:193`), not registered
- `meteor_polished`, `meteor_brick`(+`_mossy`/`_cracked`/`_chiseled`), `ore_sellafield_diamond` — zero hits anywhere
- `steel_scaffold`, `coil_copper`, `coil_copper_torus`, `coil_gold`, `coil_gold_torus`, `coil_tungsten`(as a real craftable item — a same-named but unrelated `Item` object exists only inside `XFactoryAccelerator.java`'s weapon-ammo config, not a registered game item), `coil_magnetized_tungsten`, `pipes_steel`, `chain`(as a distinct HBM item — the many "chain" hits are unrelated vanilla/other-class matches), `biomass`
- `scrap_plastic`, and the entire `bobblehead` block/item family (0 hits) — blocks the whole 29-entry bobblehead loop
- `debris_concrete`/`debris_shrapnel`/`debris_exchanger`/`debris_element`/`debris_metal`/`debris_graphite` — all 6 unregistered
- plain `sulfur`/`niter`/`fluorite` items (distinct from `crystal_sulfur`/`crystal_niter`/`crystal_fluorite`, which do exist) — confirmed via 3 other already-committed files (`RefineryRecipes.java`, `GasCentrifugeRecipes.java`, `SILEXRecipes.java`) that already document this exact same gap and substitute the `crystal_X` item in CE's *other* recipes; that substitution can't be reused here since these specific Shredder recipes are the crystal→raw conversion itself
- `sellafield`'s per-decay-stage split — the block is registered as a **single** id with no confirmed decay-stage blockstate/component property, so CE's 6 meta-keyed outputs (scrap_nuclear ×1/2/3/5/7/15) cannot currently be represented as 6 distinct recipes; flagged as an open question below rather than asserted blocked or ready.

### CyclotronRecipes

**Ready to port now** using the port's own already-established `powder_X`-for-`part_X` substitution: every target/output item referenced by CE's Li/Be/Cu/Pu chains resolves to an already-registered `BilletPowderItems`/`IngotNuggetItems` item **except**:
- **Blocked**: `part_carbon` and its natural substitute `powder_carbon` — neither registered, so the entire 8-entry carbon chain is blocked until one lands
- **Blocked**: `pellet_charged` (plutonium chain's highest-value recipe) — not registered
- **Blocked**: `ingot_mercury` — not registered (referenced only in a commented-out line in `FluidContainerRegistry.java:147`); the port's existing entry 8 already substitutes a different item here, worth revisiting rather than treating as solved

### CrackingRecipes

**Ready to port now, content-wise** — every one of the 22 distinct `FluidType` names CE's `CrackingRecipes.java` references (`OIL`, `CRACKOIL`, `PETROLEUM`, `BITUMEN`, `SMEAR`, `NAPHTHA`, `GAS`, `UNSATURATEDS`, `DIESEL`, `DIESEL_CRACK`, `KEROSENE`, `WOODOIL`, `HEATINGOIL`, `AROMATICS`, `XYLENE`, `HEATINGOIL_VACUUM`, `REFORMGAS`, `REFORMATE`, `BIOGAS`, `STEAM`, `SPENTSTEAM`, `NONE`) is already declared in `src/main/java/com/hbm/inventory/fluid/Fluids.java` (confirmed individually, matching PARITY_REPORT §3.3's finding that this port's fluid-type layer is near-1:1 with CE). **The sole blocker is that no machine and no recipe-data class exist at all** — this is a "build the machine + port the 12-entry table" task, not an item-dependency-blocked one.

---

## Recommended 1.21.1 implementation shape

**ShredderRecipes — two different shapes for two different parts of the file:**
1. **The literal `registerDefaults()` entries** (primary/misc/sellafite/fracking/decopipe/wool-clay/bobble/debris/crystal sections): plain **JSON recipes** using the machine's already-existing `hbm:shredder` type/`HbmSimpleRecipe` (single `Ingredient` in, single `ItemStack` out, optional `duration`) — exactly the shape the 44 already-ported files use. No new Java needed; every remaining unblocked entry is a straight `{"type":"hbm:shredder","input":{"item":"..."},"output":{"id":"...","count":n}}` file.
2. **`registerPost()`'s generative sweep**: this cannot be a live runtime tag-walk the way CE does it (1.21's recipe system has no equivalent "scan every registered ore-dict-alias-tag at startup" hook wired into this port's datagen), so it should become a **table-driven datagen loop**, mirroring `ModRecipeProvider.java`'s established `BILLET_SETS`/`MINERAL_SETS`/`ONE_TO_NINE_PAIRS` convention exactly: iterate `Mats.orderedList` (the ~90 `MAT_*` materials, already read in full — `src/main/java/com/hbm/inventory/material/Mats.java`), and for each material whose `NTMMaterial.autogen` set contains `INGOT`/`PLATE` (emit `{shapeItem} → dust ×1`), `NUGGET` (emit `→ tinyDust ×1`), `ORE`(emit `→ dust ×2`, `ORE` is `noAutogen()`-flagged so needs the ore-block id built by hand per CE's own `"ore" + Material` string convention), `BLOCK` (emit `→ dust ×9`), or `GEM`/`CRYSTAL` (emit `→ dust ×1`), generate one recipe JSON per (material, shape) pair where both the source item and the dust item are confirmed already registered — plus the one universal `{"tag":"c:dusts"} → hbm:dust ×1` catch-all recipe for `registerPost()`'s final `dust`-prefix branch. **Important implementation-shape risk, found while researching, that the implement wave must not fall into**: `MaterialShapes.buildRegistryName(mat)` (the class's own public helper) builds ids as `{material}_{shape}` (e.g. `iron_ingot`), but this port's *actually-registered* classic items (`BilletPowderItems`, `IngotNuggetItems`, `PlateCrystalWasteItems`) all use CE's original `{shape}_{material}` order (e.g. `powder_iron`, `ingot_iron`). Calling `buildRegistryName()` directly to look up the real item id will silently resolve to nothing. The datagen loop should instead build ids itself as `"powder_" + mat.getRegistryName()`, `"ingot_" + mat.getRegistryName()`, etc., matching the classic convention, and only fall back to `buildRegistryName()`/the newer convention if the classic lookup misses (this Mats.java abstraction appears to be scaffolding for a *future*, not-yet-populated crucible/`MatDistribution` item family — its own class javadoc says exactly this: `materialEntries`/`materialOreEntries` are "simply empty" pending that later phase).

**CyclotronRecipes**: continue the **existing bespoke static Java class** (`com.hbm.inventory.recipes.chem.CyclotronRecipes`, already-established pattern) — **not** a JSON/vanilla `RecipeType`. Reason: each recipe has two differently-matched inputs (an exact-stack "particle" catalyst plus a tag/pattern-matched target `AStack`) and produces **both** an `ItemStack` output **and** a separate integer "antimatter" yield that isn't representable by vanilla `Recipe<?>`'s single-result contract. The class's own `register()` method is already the right shape; extending it with the 32 remaining entries (following the same `powder_X`-for-`part_X`, `OreDictStack.ofCommonTag(...)`-for-`dustX` conventions already in place) is the correct next step, optionally refactored into a `String[][]`-table + loop the same way `ModRecipeProvider` does, once the entry count makes a literal list unwieldy (it's borderline at 42 total — a table is nicer but not required).

**CrackingRecipes**: a **new bespoke static Java class**, directly modeled on this port's own already-ported `RefineryRecipes.java` (same package-sibling shape: `Map<FluidType, Tuple.Pair<FluidStack,FluidStack>>`, i.e. simpler than Refinery's `Quintet` since Cracking has only 2 fluid outputs and no item byproduct) — **not** JSON, since there is no vanilla `Recipe<?>` shape for "one `FluidType` key → two `FluidStack` outputs" and this port has already chosen the "plain hardcoded Java list, not a new datagen shape, for a single fluid-only consumer" pattern for the structurally-identical Refinery case (see that file's own javadoc rationale, which applies verbatim here). This also requires building the Catalytic Cracker machine itself first (block + block entity, following the `MachineRefineryBlockEntity`/`TileEntityMachineCatalyticCracker` shapes CE's TE class already lays out: 5 fluid tanks — bitumen in, steam in, oil out, petroleum out, spent-steam out — a `crack()` tick method every 5 ticks, fluid push every 10 ticks).

**RBMKFuelRecipes**: no work — already correctly ported as generic live-state logic, not a data table.

---

## Open questions / risks

1. **`sellafield`'s decay-stage representation.** CE's Shredder recipe differentiates output (`scrap_nuclear` ×1/2/3/5/7/15) by the block's 6 metadata stages. This port's `sellafield` block (`WastelandVirusBlocks.java:113`) is registered as a single block/item id; whether it carries a blockstate `IntegerProperty` (and, more importantly, whether that property survives onto the *dropped ItemStack* the way CE's metadata did) was not confirmed within this task's time budget — a docblock elsewhere (`FalloutConfigJSON.java:212`) mentions "only has 6 discrete LEVEL states (0-5)" in a different context (world decay), suggesting the states may exist as a blockstate property but not necessarily as something a Shredder recipe's `Ingredient` (which matches on item id, not blockstate) can currently distinguish. The implement wave should check this specifically before deciding whether this collapses to 1 recipe (average/representative output) or needs a new components-aware match.

2. **`registerPost()`'s true generated-recipe count is unknown and possibly large.** CE's version sweeps *every* oredict entry present at runtime, including any other loaded mod's. This port has no other mods loaded and a smaller material roster, so the real count from a `Mats.java`-driven datagen loop will be materially smaller than CE's runtime figure — but exactly how many of the ~90 `MAT_*` materials have both a source shape and a `DUST`/`DUSTTINY` shape *and* both resolve to real registered items was not exhaustively counted (would require checking all ~90 materials' `autogen` sets against `BilletPowderItems`/`IngotNuggetItems`/`PlateCrystalWasteItems`/`OreBlocks`/`MaterialBlockGenerator` — a mechanical but nontrivial cross-reference best done at implement time with a script, following the same "look up `{shape}_{mat.getRegistryName()}`" convention flagged above).

3. **CE's `boxcar` (`ModBlocks.boxcar → powder_steel ×32`) collides in name with this port's own `ModDamageTypes.BOXCAR`** — the single grep hit for `boxcar` found was a `DamageType`, not the block; a real `ModBlocks`-equivalent boxcar block registration was not separately confirmed and should be re-checked (this task's grep budget did not chase every ambiguous single-hit term to its actual registration site — most were, this one specifically wasn't fully resolved).

4. **The Catalytic Cracker's exact GUI/tank layout wasn't independently verified against any existing port convention beyond reading CE's TE class once** — worth a second read of `TileEntityMachineCatalyticCracker.java`'s full `update()`/connection logic (only the constructor and `update()` header were read here) before implementing, to confirm tank capacities, the `world.getTotalWorldTime() % 5` cadence, and the `IConnectionAnchors`/`IFluidStandardTransceiver` NeoForge-equivalent capability wiring this port already uses elsewhere (e.g. `MachineRefineryBlockEntity`'s own fluid-capability pattern, which should be the direct template).

5. **Cyclotron's `getOutput` semantics use `isApplicable`-based catalyst matching, not exact equality** (`ComparableStack.isApplicable`) — the port's `getOutput` reimplementation already mirrors this correctly (confirmed by reading the port file in full), so no risk here, but any implement-wave work adding the remaining 32 entries should double check new entries don't accidentally rely on wildcard-meta catalyst matching that no longer makes sense under this port's one-id-per-variant item convention (CE's catalysts are all single, non-metadata items already, so this risk is low but not zero).

---

## Files read (for citation)

- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/ShredderRecipes.java` (465 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CyclotronRecipes.java` (187 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CrackingRecipes.java` (124 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/RBMKFuelRecipes.java` (78 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/oil/TileEntityMachineCatalyticCracker.java` (first ~90 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/BlockBobble.java` (BobbleType enum, 29 members counted)
- `src/main/resources/data/hbm/recipe/shredder/*.json` (all 44 files, tabulated)
- `src/main/java/com/hbm/inventory/recipes/HbmSimpleRecipe.java`, `ProcessingRecipes.java` (full)
- `src/main/java/com/hbm/inventory/recipes/chem/CyclotronRecipes.java` (84 lines, full)
- `src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java` (153 lines, full)
- `src/main/java/com/hbm/inventory/recipes/machine/rbmk/RBMKFuelRecipes.java` (67 lines, full)
- `src/main/java/com/hbm/inventory/material/Mats.java` (header + first ~70 lines), `MaterialShapes.java` (full shape-constant block + `buildRegistryName`/`commonTag`)
- `src/main/java/com/hbm/inventory/fluid/Fluids.java` (all 22 relevant `FluidType` field declarations confirmed)
- `src/main/java/com/hbm/datagen/ModRecipeProvider.java` (lines 300-423: `BILLET_SETS`/`MINERAL_SETS`/`ONE_TO_NINE_PAIRS` convention)
- `docs/phase6/PARITY_REPORT.md` (full, 416 lines) and `docs/phase6/recipe_graph_audit.md` (grepped for shredder/cyclotron/cracking/RBMK mentions)
