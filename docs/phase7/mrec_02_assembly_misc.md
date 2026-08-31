# Research report — mrec-02-assembly-misc (CE `AssemblyMachineRecipes` / `PedestalRecipes` / `ElectrolyserFluidRecipes` / `CompressorRecipes`)

Phase 7 research task. Covers 4 of CE's per-machine-type recipe-data classes under
`upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/` (not the sibling `com/hbm/crafting/`
classes — those are other tasks' scope). Read in full: `AssemblyMachineRecipes.java` (1,132 lines),
`PedestalRecipes.java` (222 lines), `ElectrolyserFluidRecipes.java` (134 lines),
`CompressorRecipes.java` (98 lines). Cross-checked against this port's committed source: 13
`data/hbm/recipe/assembler/*.json` files, `com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes`,
`com.hbm.inventory.recipes.AssemblerRecipe`/`ProcessingRecipes`/`HbmRecipes`,
`com.hbm.inventory.material.Mats`/`MaterialShapes`, and `com.hbm.datagen.ModRecipeProvider`'s
`BILLET_SETS` table-loop convention (per this task's own instructions).

## Scope confirmed

| File | Lines | In-CE structure |
|---|---:|---|
| `AssemblyMachineRecipes.java` | 1,132 | A single `registerDefaults()` method: **357 literal `this.register(new GenericRecipe(...))` call sites**, organized under **35 plain `//` comment-header sections** (no sub-methods, no material-table loop for the hand-written bulk — each entry is individually authored), **plus one terminal `for` loop** (lines ~1090-1097) iterating `Fluids.getInNiceOrder()` that mechanically generates 2 more recipes (fluid-pack / fluid-unpack) per registered `FluidType` that has a container (`!hasNoContainer()`) — see §"Full catalog" below for the exact count math. Every entry chains `.setup(duration, power)`, `.outputItems(...)`, `.inputItems(...)`, optionally `.inputItemsEx(...)` (an **alternate** ingredient set for the same output — 87 of the 357 entries have one), `.inputFluids(...)`/`.outputFluids(...)`/`.inputFluidsEx(...)` (rare — 1 entry), and `.setPools(...)`/`.setPools528(...)`/`.setGroup(...)` (a CE-specific "blueprint pool" gate — 77 entries carry one, see Open Questions). |
| `PedestalRecipes.java` | 222 | A single `registerDefaults()` with **17 literal `register(new PedestalRecipe(...))` calls**, each a fixed 3×3-grid ring pattern (8 surrounding `AStack` ingredient slots + implicit center = the item the player interacts with) plus an optional `.extra(PedestalExtraCondition)` (moon phase / sun / karma) and `.set(int)` (a "recipe set" bucket, used by `recipeSets[2]` — purpose not fully traced, low risk). Not a loop over any table — 17 fully individual, hand-authored ritual recipes. |
| `ElectrolyserFluidRecipes.java` | 134 | A single `registerDefaults()` with **8 literal `recipes.put(FluidType, new ElectrolysisRecipe(...))` calls**, keyed by input `FluidType` in a `HashMap<FluidType, ElectrolysisRecipe>`. Each entry: `amount` (mL consumed), up to 2 output `FluidStack`s, optional duration, optional varargs `ItemStack...` byproducts. Flat list, no loop. |
| `CompressorRecipes.java` | 98 | A single `registerDefaults()` with **5 literal `recipes.put(Pair<FluidType,Integer>, new CompressorRecipe(...))` calls**, keyed by `(FluidType, pressure-tier)` pair in a `HashMap`. Each entry: input amount, one output `FluidStack` (itself carrying a `pressure` field — CE's fluid model has a pressure/tier dimension), optional duration. Flat list, no loop. |

## Already covered by this port

### AssemblyMachineRecipes.java → `data/hbm/recipe/assembler/*.json` (13 files)

This port's 13 assembler JSON files are **exactly** the single-ingredient→single-plate subset of
CE's "plates and ingots" comment-section (CE lines 50-72, 17 entries total): `plate_iron`,
`plate_gold`, `plate_titanium`, `plate_aluminium`, `plate_steel`, `plate_lead`, `plate_copper`,
`plate_schrabidium`, `plate_gunmetal`, `plate_weaponsteel`, `plate_saturnite`, `plate_dura_steel`,
`plate_dalekanium`. Confirmed by direct diff: each port JSON (e.g.
`src/main/resources/data/hbm/recipe/assembler/plate_iron.json`) matches CE's
`ass.plateiron`/etc. 1:1 on ingredient, output, duration(60)/power(100).

**Not ported, even within that same 17-entry section**: `ass.platecmb` (→`plate_combine_steel`,
2-ingredient input, CE line 60), `ass.platemixed` (3-ingredient, line 65-66), `ass.dalekanium`
(single-`ComparableStack(block_meteor)` input — actually simple, but not JSON'd, line 67-68),
`ass.platedesh` (3-ingredient, line 69-70), `ass.platebismuth` (3-ingredient, line 71-72) — these
were skipped because the port's `AssemblerRecipe` JSON shape (see below) only handles the
single-`Ingredient`+`count`-list pattern each entry already uses, so a multi-ingredient entry needed
no *new* mechanism, just more JSON authoring; they simply weren't reached.

**The real remaining gap is everything else**: **344 of the 357 literal CE entries (96.4%)** are
completely unported — every one of the other 34 comment-header sections (expensive parts, cloth,
crafting parts, machine parts, powders, bunker blocks, blocks, nuclear door mod, decoration,
**machines** [53 entries — this is the section that crafts most of CE's other machine *blocks*,
i.e., meta-recursive "build machine X using the assembler"], generators, condensers, batteries,
fluid tanks, accelerators, reactors, PWR, fusion reactor, watz, upgrades, rancid-shit mob spawners,
weapon parts, bombs, bomb parts, Tsar Core, turrets, missile parts, custom missile
thrusters/fuselages/warheads, ammo, tools, space) — **plus the entire fluid-pack/unpack loop
(~320 more generated recipes, see below) — has zero port-side JSON or Java equivalent.** No
`inputItemsEx` (alternate-recipe) coverage exists at all. No fluid-input/fluid-output assembler
recipe exists at all (0 of the CE entries that use `.inputFluids(...)`/`.outputFluids(...)` are
ported — e.g. `ass.excircuit`, `ass.exleadplating`, `ass.bronzetubes`, `ass.hpcondenser`,
`ass.warheadthermonuke`, `ass.mikedeut`, all 9 `ass.package*`/`ass.unpackage*`-shaped loop entries).

### ElectrolyserFluidRecipes.java → `com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes`

Port has **6 of CE's 8** `FluidType`-keyed entries: `WATER`, `HEAVYWATER`, `VITRIOL` (**partial** —
see below), `POTASSIUM_CHLORIDE`, `CALCIUM_CHLORIDE`, `ALUMINA` (**partial** — see below). Fluid
amounts, secondary output fluids, and durations all match CE exactly for these 6.

**Real remaining gap, precisely**:
1. **`SLOP` entry — missing entirely.** CE: `Fluids.SLOP → MERCURY 250mL, LYE 50mL — wait, that's
   `REDMUD`; SLOP's real CE recipe is `Fluids.SLOP → MERCURY 250mL, NONE, byproducts niter×2,
   powder_limestone×2, sulfur×1` (CE line 29). Not present in the port's `RECIPES` map at all.
2. **`REDMUD` entry — missing entirely.** CE: `Fluids.REDMUD (450mL) → MERCURY 150mL, LYE 50mL,
   byproducts powder_titanium×3, powder_iron×3, powder_aluminium×2` (CE line 30). Not present.
3. **`VITRIOL`'s byproduct is trimmed.** CE: byproducts are `powder_iron` **and** `ingot_mercury`
   (CE line 28). Port (`ElectrolyserFluidRecipes.java:39-40`) keeps only `powder_iron` — its own
   class javadoc documents this explicitly as a scope trim ("CE's VITRIOL recipe drops an
   ingot_mercury byproduct this port has not registered yet"), **but `ingot_mercury` IS now a
   registered item** (`src/main/java/com/hbm/items/IngotNuggetItems.java`) — this trim is stale and
   trivially fixable.
4. **`ALUMINA`'s byproduct is trimmed.** CE: byproducts are `powder_aluminium×7` **and**
   `fluorite×2` (CE line 31). Port (`ElectrolyserFluidRecipes.java:44-45`) keeps only
   `powder_aluminium×7`. `fluorite` (the bare CE item, distinct from the already-ported
   `crystal_fluorite`) is **not** registered anywhere in this port (see Item dependency check below)
   — this one genuinely needs a new item first, unlike #3.

All fluids these 2 missing entries need (`SLOP`, `REDMUD`, `LYE`, `MERCURY`) are **already
registered** in this port's `Fluids.java` (confirmed by grep — `SLOP` line 187, `REDMUD` line 165,
`LYE` line 188, `MERCURY` line 99), so the missing-entries gap is pure recipe-data authoring, not a
fluid-registry gap.

### PedestalRecipes.java and CompressorRecipes.java

**Confirmed by grep: zero coverage in this port, of any kind.** `grep -rli pedestal
src/main/java/com/hbm` and the equivalent for `compressor` (recipe class, not the unrelated
`TurbineBaseBlockEntity`/`MachineIndustrialTurbineBlockEntity`/`RBMKBoilerBlockEntity` substring
false-positives, which have nothing to do with CE's Compressor machine) both return nothing;
`data/hbm/recipe/` has no `pedestal` or `compressor` subdirectory. Neither the recipe data **nor**
the machine/block mechanism itself exists for either — see the item/registry dependency section.

## Full recipe/entry catalog OR representative pattern

### AssemblyMachineRecipes.java — LARGE file (357 literal entries + a generating loop)

**Exact total**: 357 literal `this.register(new GenericRecipe(...))` call sites (script-counted,
verified against a per-section sum that reconciles exactly: 17+9+3+4+3+9+5+4+2+18+6+53+12+1+5+3+12+
11+11+11+3+3+2+2+15+13+9+18+21+19+14+12+5+1+21 = 357). **Plus** a terminal loop (CE lines ~1090-1097)
not counted in that 357 (it contributes 2 more literal call sites, already included above, but each
executes once per fluid at runtime):
```java
FluidType[] order = Fluids.getInNiceOrder();
for (int i = 1; i < order.length; ++i) {
    FluidType type = order[i];
    if (type.hasNoContainer()) continue;
    register(...package...);    // fluid_pack_empty + N -> fluid_pack_full(type)
    register(...unpackage...);  // fluid_pack_full(type) -> fluid_pack_empty + N
}
```
CE has 158 `new FluidType(...)` declarations in `Fluids.java` and only **1** carries the
`FT_NoContainer` trait, so this loop generates **≈2 × 156-160 ≈ 310-320 additional real recipes**
(index 0 is skipped too — likely `Fluids.NONE`). **True total recipe count for this one CE class is
therefore ≈665-675**, not 357 — the 357 figure is call-site count, the loop multiplies it.

**Per-section breakdown** (all 35 headers, exact per-section literal-entry count):

| Section (CE comment) | Lines | Entries | Ported? |
|---|---:|---:|---|
| plates and ingots | 50-72 | 17 | **13/17 as JSON** (see above) |
| expensive parts | 73-99 | 9 | 0 |
| cloth | 100-107 | 3 | 0 |
| crafting parts (+ alternates) | 108-127 | 4+3=7 | 0 |
| machine parts | 128-148 | 9 | 0 |
| powders | 149-161 | 5 | 0 |
| bunker blocks | 162-171 | 4 | 0 |
| blocks | 172-177 | 2 | 0 |
| nuclear door mod | 178-215 | 18 | 0 |
| decoration | 216-229 | 6 | 0 |
| **machines** | 230-386 | **53** | 0 (this is the section that crafts CE's *other* machine blocks) |
| generators | 387-420 | 12 | 0 |
| condensers | 421-425 | 1 | 0 |
| batteries | 426-446 | 5 | 0 |
| fluid tanks | 447-457 | 3 | 0 |
| accelerators | 458-494 | 12 | 0 |
| reactors | 495-527 | 11 | 0 |
| PWR | 528-563 | 11 | 0 |
| fusion reactor | 564-606 | 11 | 0 |
| watz | 607-620 | 3 | 0 |
| upgrades | 621-630 | 3 | 0 |
| rancid shit mob spawners | 631-636 | 2 | 0 |
| weapon parts | 637-642 | 2 | 0 |
| bombs | 643-685 | 15 | 0 |
| bomb parts | 686-713 | 13 | 0 |
| Tsar Core | 714-735 | 9 | 0 |
| turrets | 736-783 | 18 | 0 |
| missile parts | 784-830 | 21 | 0 |
| custom missile thrusters | 831-870 | 19 | 0 |
| custom missile fuselages | 871-900 | 14 | 0 |
| custom missile warheads | 901-931 | 12 | 0 |
| ammo | 932-948 | 5 | 0 |
| tools | 949-952 | 1 | 0 |
| space (incl. the fluid-pack loop) | 953-1132 | 21 literal + ~320 generated | 0 |

**Representative sample (30 entries spanning sub-patterns, verbatim ingredient/output/duration/power)**:

| Recipe id | Output | Duration | Power | Inputs (item, count) | Alt (`Ex`)? | Fluid I/O |
|---|---|---:|---:|---|---|---|
| `ass.plateiron` | `plate_iron` ×1 | 60 | 100 | `IRON.ingot()` ×1 (tag) | no | no |
| `ass.platemixed` | `plate_mixed` ×4 | 50 | 100 | `CU.plate()`×2, reflector×1, `BIGMT.plate()`×1 | no | no |
| `ass.dalekanium` | `plate_dalekanium` ×1 | 200 | 100 | `ModBlocks.block_meteor` ×1 | no | no |
| `ass.excircuit` | `item_expensive`(CIRCUIT) | 400 | 4,000 | `circuit`(BASIC)×12, `circuit`(CAPACITOR)×8, `RUBBER.ingot()`×4 | no | `SULFURIC_ACID` 1,000mL in |
| `ass.chip` | `circuit`(CHIP) | 50 | 250 | `plate_polymer`×1, `circuit`(SILICON)×1, `GOLD.wireFine()`×1 | no | no (gated: `!enable528`) |
| `ass.centrifugetower` | `centrifuge_element` ×1 | 100 | 100 | `DURA.plate()`×4, `TI.plate()`×4, `motor`×1 | no | no |
| `ass.shredder` | `machine_shredder` (block) | 100 | 100 | `STEEL.plate()`×8, `CU.plate()`×4, `motor`×2 | **yes** | no |
| `ass.electrolyzer` | `machine_electrolyser` (block) | 200 | 100 | `STEEL.plateCast()`×8, `CU.plate()`×16, `TI.shell()`×3, `RUBBER.ingot()`×8, `ingot_firebrick`×16, `coil_copper`×16, `circuit`(BASIC)×8 | **yes** | no |
| `ass.pilepabe` | `pile_rod`(RA226BE) ×1 | 40 | 200 | `billet_ra226be`×3 | no | no; `setGroup("autoswitch.pilerod")` |
| `ass.watzrod` | `watz_element` (block) ×3 | 200 | 100 | `STEEL.plateCast()`×2, `ZR.ingot()`×2, `BIGMT.ingot()`×2, `ANY_HARDPLASTIC.ingot()`×4 | **yes** | no; `setPools528(...)` |
| `ass.tsarcore` | `tsar_core` ×1 | 1,200 | 100 | `mike_deut`×1, `LI.ingot()`×9, `cell`(TRITIUM)×1, `PU239.nugget()`×1, `BE.nugget()`×1, `PB.plate()`×12, `TI.shell()`×2 | no | no |
| `ass.turretchekhov` | `turret_chekhov` (block) | 200 | 100 | `battery_pack`(BATTERY_LEAD)×1, `STEEL.ingot()`×16, `motor`×3, `circuit`(ADVANCED)×1, `STEEL.pipe()`×3, `GUNMETAL.mechanism()`×3, `crate_iron`×1, `crt_display`×1 | no | `setPools528("bmg")` |
| `ass.warheadhe1` | `warhead_generic_small` ×1 | 100 | 100 | `TI.plate()`×4, `ball_dynamite`×2, `circuit`(CHIP)×1 | no | no |
| `ass.mpt10kero` | `mp_thruster_10_kerosene` ×1 | 100 | 100 | `seg_10`×1, `STEEL.pipe()`×1, `W.ingot()`×4, `STEEL.plate()`×4 | no | no |
| `ass.mpf10kero` | `mp_fuselage_10_kerosene` ×1 | 100 | 100 | `seg_10`×2, `AL.plate()`×12, `STEEL.plate()`×3 | no | no |
| `ass.mpw10he` | `mp_warhead_10_he` ×1 | 100 | 100 | `seg_10`×1, `STEEL.plate()`×6, `ANY_HIGHEXPLOSIVE.ingot()`×3, `circuit`(BASIC)×1 | no | no |
| `ass.50bmgsm` | `ammo_standard`(BMG50_SM) ×6 | 100 | 100 | `casing`(LARGE_STEEL)×1, `ANY_SMOKELESS.dust()`×6, `STAR.ingot()`×3 | no | `setPools("silverstorm")` |
| `ass.shellchlorine` | `ammo_arty`(index 9) ×1 | 100 | 1,000 | `ammo_arty`(0)×1, `ANY_PLASTIC.ingot()`×1 | no | `CHLORINE` 4,000mL in |
| `ass.multitool` | `multitool_hit` ×1 | 100 | 100 | `ANY_RESISTANTALLOY.ingot()`×4, `STEEL.plate()`×4, `GOLD.wireFine()`×12, `motor`×4, `circuit`(CAPACITOR_BOARD)×16 | no | no |
| `ass.soyuz` | `missile_soyuz` ×1 | 6,000 | 100 | `TI.shell()`×32, `RUBBER.ingot()`×64, `rocket_fuel`×64, `thruster_small`×12, `thruster_medium`×12, `circuit`(CONTROLLER)×4, `part_generic`(LDE)×32 | no | `setPools("soyuz")` |
| `ass.satellitebase` | `sat_base` ×1 | 600 | 100 | `RUBBER.ingot()`×12, `TI.shell()`×3, `thruster_medium`×1, `part_generic`(LDE)×8, `plate_desh`×4, `fluid_barrel_full`(KEROSENE)×1, `photo_panel`×24, `circuit`(BASIC)×12, `battery_pack`(BATTERY_LITHIUM)×1 | no | no |
| `ass.capnuka` | `block_cap`(NUKA) ×varies | 10 | 100 | `cap_nuka`×64 (×2, sink pattern) | no | no |
| `ass.hpcondenser` | `machine_condenser_powered` (block) | 600 | 100 | `STEEL.plateWelded()`×8, `ANY_RESISTANTALLOY.plateWelded()`×4, `CU.plate528()`×16, `motor_desh`×3, `STEEL.pipe()`×24, `LUBRICANT.getDict(1000)`×4 | **yes** | no |
| `ass.fusionmhdt` | `fusion_mhdt` (block) | 1,200 | 100 | `ANY_RESISTANTALLOY.plateWelded()`×16, `CU.plateWelded()`×64, `ANY_BISMOIDBRONZE.plateCast()`×16, `SBD.wireDense()`×64, `circuit`(QUANTUM)×4 | **yes** | no; `setPools528("chlorophyte")` |
| `ass.overdrive1` | `upgrade_overdrive_1` ×1 | 200 | 100 | `upgrade_speed_3`×1, `upgrade_effect_3`×1, `BIGMT.ingot()`×16, `ANY_HARDPLASTIC.ingot()`×16, `circuit`(ADVANCED)×16 | no | no; gated `if(no528)` |
| `ass.chopper` | `spawn_chopper` ×1 | 1,200 | 100 | `CMB.plateCast()`×24, `STEEL.plate()`×32, `MAGTUNG.wireFine()`×48, `motor_desh`×5, `circuit`(CONTROLLER_ADVANCED)×1 | no | no |
| `ass.explosivelenses1` | `early_explosive_lenses` ×1 | 400 | 100 | `AL.plate()`×8, `det_cord`×8, `BIGMT.plate()`×2, `ANY_HIGHEXPLOSIVE.ingot()`×20, `ANY_PLASTIC.ingot()`×4 | no | no |
| `ass.package<fluid>` (loop) | `fluid_pack_full`(type) ×1 | 40 | 100 | `fluid_pack_empty`×1 | no | `type` 32,000mL in |
| `ass.unpackage<fluid>` (loop) | `fluid_pack_empty` ×1 | 40 | 100 | `fluid_pack_full`(type)×1 | no | `type` 32,000mL out |

**Generating pattern for an implement-wave loop** (mirroring `ModRecipeProvider`'s `BILLET_SETS`
convention): CE's own source is **not** table-driven for the 357 hand entries (each is an individual,
differently-shaped call) — a mechanical `String[][]` table only works well for the loop-generated
tail (fluid packs). For the 357: the cleanest **port-side** authoring shape is a `record
AssemblerEntry(String name, String outputId, int count, int duration, long power, Entry[] inputs,
Entry[] inputsEx)` array (one row per CE entry, hand-transcribed from the table above's pattern),
consumed by a single loop that calls `AssemblerRecipe.of(...)` per row (main) and again per row with
`inputsEx` when non-null — this reproduces CE's "two valid recipes, same output" shape without a
second JSON file per alternate. The **fluid-pack loop specifically** should be authored exactly as
CE does it — a runtime loop over `Fluids.ALL`/`getInNiceOrder()`-equivalent filtering
`!hasNoContainer()`, generating `AssemblerRecipe`-with-fluid instances (once that class supports
fluid I/O, see below) rather than JSON per fluid (162 JSON files would be needlessly generated
boilerplate for a purely mechanical rule).

### PedestalRecipes.java — SMALL file, cataloged in full (17 entries)

| Recipe (output) | Extra condition | Set | Ring ingredients (8 slots, clockwise from NW) | Center item held |
|---|---|---:|---|---|
| `gun_light_revolver_dani` | none | 0 | null, `PB.plate()`, null / `GOLD.plate()`, —, `GOLD.plate()` / null, `PB.plate()`, null | `gun_light_revolver` |
| `gun_maresleg_broken` | none | 0 | `barbed_wire`×4 corners, `WEAPONSTEEL.plate()`×4 edges | `gun_maresleg` |
| `gun_heavy_revolver_lilmac` | none | 0 | null, `weapon_mod_special`(SCOPE), null / `powder_magic`, —, `WEAPONSTEEL.plate()` / null, `BONE.grip()`, `APPLE`×3 | `gun_heavy_revolver` |
| `gun_heavy_revolver_protege` | none | 0 | `chain`×16 (corners), `CINNABAR.gem()`/`scrap_nuclear` (edges) | `gun_heavy_revolver` |
| `gun_amat_subtlety` | none | 0 | `STAR.ingot()` corners, `AL.plateCast()` edges | `gun_amat` |
| `gun_amat_penance` | none | 0 | `STAR.ingot()` corners, `DURA.plateCast()`/`weapon_mod_special`(SILENCER,FURNITURE_BLACK) edges | `gun_amat` |
| `gun_flamer_daybreaker` | **SUN** | 0 | `GOLD.plateCast()` corners, `canned_conserve`(JIZZ)/`P_WHITE.ingot()`/`stick_dynamite` edges | `gun_flamer` |
| `gun_autoshotgun_sexy` | none | 0 | `bolt_spike`×16 corners, `wild_p`/`card_qos`/`card_aos`/`STAR.ingot()`×16 edges | `gun_autoshotgun` |
| `gun_minigun_lacunae` | **FULL_MOON** | 0 | null corners, `powder_magic`×4/`item_secret`(SELENIUM_STEEL)×4 edges | `gun_minigun` |
| `gun_laser_pistol_morning_glory` | none | 0 | null corners, `morning_glory`/`item_secret`(SELENIUM_STEEL)×2/`EMERALD.gem()`×16 edges | `gun_laser_pistol` |
| `gun_folly` | **FULL_MOON** | **1** | `item_secret`(FOLLY)×4 corners, `item_secret`(CONTROLLER)×2/`BSCCO.ingot()`×16 edges | `STAR.block()`×64 |
| `gun_aberrator` | none | **1** | null corners, `item_secret`(ABERRATOR) edges | `BIGMT.mechanism()`×4 |
| `gun_aberrator_eott` | **GOOD_KARMA** | **1** | `item_secret`(ABERRATOR) all 8 slots | `BIGMT.mechanism()`×16 |
| `ammo_secret`(FOLLY_SM) ×1 | **FULL_MOON** | **1** | `STAR.ingot()` corners, `powder_magic` edges | `chunk_ore`(MOONSTONE) |
| `ammo_secret`(FOLLY_NUKE) ×1 | **FULL_MOON** | **1** | `STAR.ingot()` corners, `powder_magic` edges | `ammo_standard`(NUKE_HIGH)×4 |
| `ammo_secret`(P35_800) ×5 | none | **1** | all null except center | `item_secret`(ABERRATOR)×1 |
| `ammo_secret`(P35_800_BL) ×10 | none | **1** | all null except center | `item_secret`(ABERRATOR)×3 |

**Mechanism** (from `BlockPedestal.java`): a center `TileEntityPedestal` (simple single-`ItemStack`
holder, right-click to place/swap/retrieve) scans 8 more `TileEntityPedestal`s **3 blocks out** in
each of the 4 cardinal + 4 diagonal directions on the same Y level (`BlockPedestal.java:144-156`),
builds a 9-element array `[nw,n,ne,w,center,e,sw,s,se]`, and on a trigger (neighbor/redstone update)
iterates `PedestalRecipes.recipes` checking: (a) any `extra` condition (`world.getCelestialAngle`
for FULL_MOON/NEW_MOON/SUN, `world.provider.getMoonPhase`, or `HbmCapability.getData(player)
.getReputation()` ≥/≤ ±10 for GOOD_KARMA/BAD_KARMA scanned against nearby players within a
20-block AABB), then (b) exact per-slot `AStack.matchesRecipe` + count match across all 9 positions
(`BlockPedestal.java:188-196`). First full match consumes all 9 items and spawns the output.

### ElectrolyserFluidRecipes.java — SMALL file, cataloged in full (8 entries)

| Input fluid (key) | Amount consumed | Output 1 | Output 2 | Duration | Byproducts | Port status |
|---|---:|---|---|---:|---|---|
| `WATER` | 2,000 | `HYDROGEN` 200 | `OXYGEN` 200 | 10 | — | ✅ ported |
| `HEAVYWATER` | 2,000 | `DEUTERIUM` 200 | `OXYGEN` 200 | 10 | — | ✅ ported |
| `VITRIOL` | 1,000 | `SULFURIC_ACID` 500 | `CHLORINE` 500 | 20 | `powder_iron`×1, `ingot_mercury`×1 | ⚠️ ported minus `ingot_mercury` |
| `SLOP` | 1,000 | `MERCURY` 250 | `NONE` | 20 | `niter`×2, `powder_limestone`×2, `sulfur`×1 | ❌ missing entirely |
| `REDMUD` | 450 | `MERCURY` 150 | `LYE` 50 | 20 | `powder_titanium`×3, `powder_iron`×3, `powder_aluminium`×2 | ❌ missing entirely |
| `ALUMINA` | 200 | `CARBONDIOXIDE` 100 | `NONE` | 40 | `powder_aluminium`×7, `fluorite`×2 | ⚠️ ported minus `fluorite` |
| `POTASSIUM_CHLORIDE` | 250 | `CHLORINE` 125 | `NONE` | 20(default) | — | ✅ ported |
| `CALCIUM_CHLORIDE` | 250 | `CHLORINE` 125 | `CALCIUM_SOLUTION` 125 | 20(default) | — | ✅ ported |

### CompressorRecipes.java — SMALL file, cataloged in full (5 entries)

| Input (fluid, pressure-tier) | Amount | Output fluid (amount, pressure-tier) | Duration |
|---|---:|---|---:|
| `(PETROLEUM, 0)` | 2,000 | `PETROLEUM` 2,000 @ tier 1 | 20 |
| `(PETROLEUM, 1)` | 2,000 | `LPG` 1,000 @ tier 0 | 20 |
| `(BLOOD, 3)` | 1,000 | `HEAVYOIL` 250 @ tier 0 | 200 |
| `(PERFLUOROMETHYL, 0)` | 1,000 | `PERFLUOROMETHYL` 1,000 @ tier 1 | 50 |
| `(PERFLUOROMETHYL, 1)` | 1,000 | `PERFLUOROMETHYL_COLD` 1,000 @ tier 0 | 50 |

CE's Compressor is a **pressure-tier fluid transformer**: it reads a fluid at a given pressure tier
(the `Integer` half of the map key) and outputs the same or a different fluid at a different tier —
e.g. raw petroleum (tier 0) compresses into pressurized petroleum (tier 1), which a *second* pass
through the same machine cracks into LPG (tier 0). `BLOOD` at tier 3 → `HEAVYOIL` is CE's odd one out
(likely a blood-magic-adjacent cross-mod joke recipe, not a mainline progression step — worth a
`Skip / low priority` flag for the implement wave rather than treating it as core content).

## Item/registry dependency check

### AssemblyMachineRecipes.java

**Raw-material ingredients (plates/ingots/shells/pipes/wires/bolts via `Mats`/`MaterialShapes`) are
overwhelmingly already registered.** Confirmed: `MaterialShapes.AUTOGEN_SHAPES` (in this port's
`MaterialItemGenerator.java`) already covers `SHELL`, `PIPE`, `CASTPLATE`("plateCast"),
`WELDEDPLATE`("plateWelded"), `WIRE`("wireFine"), `DENSEWIRE`("wireDense"), `BOLT`, and more, and
`Mats.java`'s `MAT_STEEL`/`MAT_COPPER`/`MAT_TITANIUM`/`MAT_LEAD`/`MAT_GOLD` etc. already declare
these shapes in their `setAutogen(...)` sets — so e.g. `hbm:steel_plate_triple` (CE's
`STEEL.plateCast()`), `hbm:steel_pipe`, `hbm:steel_bolt` **already exist as registered items**, even
though (per `docs/phase6/recipe_graph_audit.md` root cause 1) they are not yet **obtainable** in
survival because the Crucible isn't ported — that's a separate, already-tracked gap, not a blocker
for *authoring* these assembler recipes.

**Output items/blocks are the real blocker, and are missing at scale.** Programmatic extraction of
every distinct `outputItems(new ItemStack(ModBlocks.X...`/`ModItems.X...` in the file (287 distinct
identifiers: 141 `ModBlocks` + 147 `ModItems`, minus 1 dedup) cross-checked against this port's
registered ids:

- **`ModBlocks` outputs: 84 of 141 (60%) missing.** Representative missing: `machine_purex`,
  `machine_precass`, `machine_supercomputer`, `machine_arc_furnace`, `machine_compressor`,
  `machine_compressor_compact`, `machine_coker`, `machine_hydrotreater`, `machine_pyrooven`,
  `machine_liquefactor`, `machine_solidifier`, `machine_radar`, `machine_radar_large`,
  `machine_radgen`, `machine_forcefield`, `machine_teleporter`, `machine_strand_caster`,
  `machine_hephaestus`, `machine_turbofan`, `machine_turbinegas`, `machine_chungus`,
  `machine_orbus`, `machine_exposure_chamber`, `machine_fel`, `machine_condenser_powered`,
  `machine_fluidtank`, `machine_bigasstank`, `machine_flare`, `machine_mining_laser`,
  `machine_excavator`, `machine_ore_slopper`, `machine_satlink`, `pwr_*` (all 11), `fusion_*` (all
  8), `pa_*` accelerator blocks (all 6), the entire door/hatch/vault family (`vault_door`,
  `blast_door`, `fire_door`, `sliding_blast_door`(+legacy), `round_airlock_door`,
  `secure_access_door`, `sliding_seal_door`, `qe_containment`, `qe_sliding_door`, `water_door`,
  `large_vehicle_door`, `cargo_door`, `silo_hatch_large`), `reactor_research`, `reactor_zirnox`,
  `turret_arty`, `turret_himars`, `nuke_solinium`, `nuke_fstbmb`, `vitrified_barrel`,
  `yellow_barrel`, `cmb_brick`, `pile_brick`, `seal_frame`/`seal_controller`, `transition_seal`,
  `watz_element`, `watz_cooler`. **Confirmed already-present (57/141)**: `machine_shredder`,
  `machine_assembly_machine`, `machine_electrolyser`, `machine_centrifuge`, `machine_gascent`,
  `machine_cyclotron`, `machine_silex`, `machine_crystallizer`, `machine_refinery`, `machine_well`,
  `machine_pumpjack`, `machine_diesel`, `machine_combustion_engine`, `machine_industrial_turbine`,
  `machine_reactor_breeding`, `turret_chekhov`, `turret_friendly`, `turret_fritz`, `turret_howard`,
  `turret_jeremy`, `turret_maxwell`, `turret_richard`, `turret_tauon`, `nuke_boy`, `nuke_man`,
  `nuke_mike`, `nuke_tsar`, `nuke_gadget`, `nuke_n2`, `nuke_custom`, `nuke_prototype`, `nuke_fleija`,
  `mine_naval`, `float_bomb`, `therm_endo`, `therm_exo`, `launch_pad`(+`_large`), and several more
  (spot-checked, not individually re-verified for all 57).
- **`ModItems` outputs: 54 of 86 non-plate items (63%) missing.** Representative missing:
  `item_expensive` (the entire `EnumExpensiveType` intermediate-part family — blocks most of the
  "machines"/"generators"/etc. sections' `inputItemsEx` alt-recipe too, since it's consumed there),
  `circuit` (all `EnumCircuitType` variants — same story), `battery_pack`, `centrifuge_element`,
  `thermo_element`, `reactor_core`, `magnetron`, `drill_titanium`, `drillbit`, `entanglement_kit`,
  `rtg_unit`, `dysfunctional_reactor`, `pile_rod`, `part_lithium`/`part_beryllium`/`part_carbon`/
  `part_copper`/`part_plutonium`, `pellet_cluster`/`pellet_buckshot`, `filter_coal`,
  `fluid_pack_empty`/`fluid_pack_full`, all `warhead_*` (13), all `mp_thruster_*`/`mp_fuselage_*`/
  `mp_warhead_*` (custom-missile parts, ~40 combined — 0/40 present), `missile_assembly`,
  `missile_soyuz_lander`, `sat_base`/`sat_head_*`/`satellite`, `ammo_arty`/`ammo_himars`/
  `ammo_secret`/`ammo_standard`, `spawn_chopper`, `solinium_igniter`/`solinium_propellant`. This
  matches — and is directly explained by — `ModRecipeProvider`'s own already-committed javadoc,
  which independently names `circuit`, `motor`, `motor_desh`, `tank_steel`, `thruster_small` as
  "do not exist in this port at all yet."

**Practical implication**: only the ~13 already-ported plate entries plus a modest number of
"machine parts"/"powders"/"cloth" section entries whose output is a plain, already-registered item
(e.g. `centrifuge_element` is actually **missing**, but `thermo_element` and `magnetron` are also
missing — spot-checking found the "machine parts" section's outputs are *all* currently-unregistered
items) are genuinely "ready to port now" purely as recipe JSON. **The large majority of this file's
value is gated behind Phase 8+/other-phase item and block registration work first** — this is a
recipe-*data* task whose real bottleneck is upstream item/block scope, exactly per this task's ground
rule #4.

### PedestalRecipes.java

**Blocked at the mechanism level, not just items.** Zero of the 8 `gun_*`/`ammo_secret` outputs this
file crafts exist in this port (`gun_light_revolver`/`_dani`, `gun_maresleg`/`_broken`,
`gun_heavy_revolver`/`_lilmac`/`_protege`, `gun_amat`/`_subtlety`/`_penance`, `gun_flamer`/
`_daybreaker`, `gun_autoshotgun`/`_sexy`, `gun_minigun`/`_lacunae`, `gun_laser_pistol`/
`_morning_glory`, `gun_folly`, `gun_aberrator`/`_eott`, `ammo_secret` — none found via grep; this
port's weapon/gun item family is a separate, not-yet-cross-checked scope). `item_secret` (the
`EnumSecretType`-keyed family several ring ingredients need, e.g. `SELENIUM_STEEL`, `ABERRATOR`,
`FOLLY`, `CONTROLLER`) is also unconfirmed. **Verdict: fully blocked** — needs (1) the `BlockPedestal`
+ `TileEntityPedestal` mechanism (net-new, no NeoForge precedent in this port), (2) the reputation/
karma capability (`HbmCapability.getData(player).getReputation()` — check whether this port's own
player-capability system already has an equivalent before assuming it needs porting too), and (3)
essentially the entire gun/secret-item family this recipe class's outputs depend on. **Not
"ready to port now" in any meaningful sense** — recommend deferring this file until the gun-item
family lands, unless the implement wave wants to stub the mechanism with zero working recipes.

### ElectrolyserFluidRecipes.java

- **`SLOP` entry — blocked on 2 of 3 byproduct items.** `niter` and `sulfur` (CE's *plain* bare-name
  items, `ModItems.niter`/`ModItems.sulfur` in `upstream/hbm-ce/src/main/java/com/hbm/items/
  ModItems.java:1130,1133`) are **not registered** in this port under those exact ids — confirmed by
  grep (only `crystal_niter`/`crystal_sulfur`, a **different**, already-ported item family from
  `PlateCrystalWasteItems.java:201-202`, both of which CE itself also keeps as distinct, separate
  items — this is not a naming mismatch to fix, it's a genuinely un-ported item pair).
  `powder_limestone` already exists (`BilletPowderItems.java`). **Blocked** until `niter`/`sulfur`
  are registered.
- **`REDMUD` entry — ready to port now.** All 3 byproducts (`powder_titanium`, `powder_iron`,
  `powder_aluminium`) already exist in `BilletPowderItems.java`, and all 3 fluids (`REDMUD`,
  `MERCURY`, `LYE`) already exist in `Fluids.java`. **No blockers — pure recipe-data authoring.**
- **`VITRIOL`'s missing `ingot_mercury` byproduct — ready to port now.** `ingot_mercury` already
  exists (`IngotNuggetItems.java`). This is a one-line fix to the port's own already-committed
  `ElectrolyserFluidRecipes.java:39-40`.
- **`ALUMINA`'s missing `fluorite` byproduct — blocked**, same root cause as `SLOP` above (CE's
  bare `fluorite` item, `ModItems.java:1134`, not registered; only `crystal_fluorite` exists).

### CompressorRecipes.java

**Item/fluid dependency: fully ready.** All 6 fluids referenced (`PETROLEUM`, `LPG`, `BLOOD`,
`HEAVYOIL`, `PERFLUOROMETHYL`, `PERFLUOROMETHYL_COLD`) already exist in `Fluids.java`, and this
port's `FluidStack.java` **already carries the `pressure` field** (`int pressure`, line 13,
constructor `FluidStack(FluidType, int, int)`) CE's compressor model needs — confirmed via direct
read, this was ported structurally already even though nothing consumes it yet.
**Blocked purely on the machine itself**: no `machine_compressor`/`machine_compressor_compact`
block, no block entity, no recipe-registration scaffolding exists anywhere in this port. This is a
"build the machine, then the recipes are nearly free" case, the inverse of `AssemblyMachineRecipes`'s
situation (machine exists, recipes don't).

## Recommended 1.21.1 implementation shape

**AssemblyMachineRecipes.java → extend the existing `AssemblerRecipe`/`hbm:assembler` JSON
`Recipe<?>` type, do not build a new mechanism.** The machine (`MachineAssemblyMachineBlockEntity`,
`machine_assembly_machine` block) and the recipe class (`com.hbm.inventory.recipes.AssemblerRecipe`,
`com.hbm.inventory.recipes.ProcessingRecipes`) both already exist and already handle the dominant
shape (N-item-in → 1-item-out with per-recipe duration/power, arbitrary ingredient count up to CE's
own `inputItemLimit()=12`). Two concrete, scoped extensions are needed before the bulk of the 357
entries can be authored as JSON:
1. **Add fluid input/output support to `AssemblerRecipe`.** Its own class javadoc currently says
   this is blocked on `FluidStack` having no `Codec`/`StreamCodec` — **that claim is now stale**:
   `com.hbm.inventory.fluid.FluidStack.java` already has both (`CODEC` field, line 37;
   `STREAM_CODEC`, line 50), added since that javadoc was written. Wiring an optional
   `List<FluidStack> inputFluids`/`outputFluids` into `AssemblerRecipe`'s record/codec/`Entry` shape
   is now a straightforward mechanical addition, not a data-model blocker — flag this explicitly to
   the implement wave as a corrected/outdated comment to fix while touching the class anyway.
2. **Add an alternate-ingredient-set (`inputsEx`) field**, matching CE's `inputItemsEx` (87 of 357
   entries use it) — either as a second optional `List<Entry>` on the same `AssemblerRecipe` (one
   JSON, two valid ways to satisfy it) or, more simply for a first pass, as **two separate JSON
   files per such entry** (e.g. `machine_purex.json` + `machine_purex_alt.json`, both producing the
   same output) — the latter needs no Java change at all and matches this port's existing
   one-file-per-recipe convention; recommend that route unless the implement wave wants tighter
   parity with CE's single-recipe-two-paths model.
3. **`setPools`/`setPools528`/`setGroup` (CE's blueprint-pool/config-gate metadata, 77+ entries)
   should be dropped for now**, exactly as `AssemblerRecipe`'s own javadoc already documents doing
   for the ported-13 — this port has no blueprint-unlock system, and `GeneralConfig.enable528` is a
   CE-side legacy-balance toggle with no port-side equivalent flagged anywhere yet. Not a blocker,
   just means every recipe is unconditionally available once its ingredients exist (already the
   port's stated simplification).
4. **The fluid-pack/unpack loop tail** should be authored as a **runtime-generated set** (a static
   loop in Java over the fluid registry, same shape as CE's), not 300+ hand-written JSON files —
   this is the one part of the file that genuinely is a mechanical table-loop in CE's own structure,
   and matches `ModRecipeProvider`'s own precedent of generating recipes at datagen time rather than
   hand-authoring each.
   Once fluid I/O (item 1) lands, this becomes a small, self-contained addition.
5. **Prioritize by what's actually reachable**: since 84/141 block outputs and 54/86 item outputs
   don't exist yet, the highest-value slice to port *first* is the subset whose output already
   exists (the 57 present `ModBlocks` + 32 present `ModItems`, cross-referenced against each
   section above) — porting recipe JSON for a machine block that doesn't exist yet produces a
   dead/unreachable recipe, no different in kind from the Crucible-blocked material items Phase 6
   already flagged.

**PedestalRecipes.java → a genuinely new custom mechanism, not a `Recipe<?>` JSON type.** CE's own
matching logic (fixed 9-slot ring positions, moon-phase/karma world-state conditions, first-match
scan across a `List<PedestalRecipe>` rather than a per-block `RecipeManager` lookup) does not fit
vanilla's `Recipe<RecipeInput>` contract at all — there is no single "crafting station" block whose
inventory *is* the input; the ring is 8 **separate block entities** 3 blocks apart, and the "input" is a
snapshot read across all 9 positions at trigger time. Recommend following this port's own established
precedent for exactly this situation: `CrystallizerRecipes`/`MixerRecipes`/`RefineryRecipes` (bespoke
plain-Java `Map`/`List`-backed data classes, not JSON `Recipe<?>`), which this port's own
`ProcessingRecipes.java` javadoc explicitly documents as the pattern for recipe shapes that "doesn't
fit vanilla's `Recipe<RecipeInput>` contract." A `PedestalRecipes` Java class holding `List<Entry>`
(mirroring CE's `PedestalRecipe` record 1:1 — 8 nullable `Ingredient`+count slots, an output
`ItemStack`, an `enum PedestalExtraCondition`) plus a new `BlockPedestal`/block-entity pair (single
`ItemStack` slot, right-click place/swap/retrieve, ring-scan-and-match on neighbor update) is the
right shape — but this whole file should be **sequenced after** the gun-item family it depends on
(§Item dependency check), since 0 of its 8 output guns exist yet.

**ElectrolyserFluidRecipes.java → keep extending the existing bespoke Java class, no new mechanism
needed.** `com.hbm.inventory.recipes.chem.ElectrolyserFluidRecipes` already exists, already uses the
right shape (a `Map<FluidType, ElectrolysisRecipe>`, matching CE's own map-keyed structure exactly —
no `Recipe<?>` conversion was ever attempted or needed here, consistent with this port's established
pattern of keeping fluid-keyed machine recipes as plain Java). The work here is purely: (a) fix the
2 stale byproduct trims (`ingot_mercury`, ready now; `fluorite`, blocked on a new item), (b) add the
`REDMUD` entry (ready now, zero blockers), (c) add the `SLOP` entry once `niter`/`sulfur` exist.

**CompressorRecipes.java → new bespoke Java class, same shape as `ElectrolyserFluidRecipes`/
`CompressorRecipes` itself in CE**, i.e. a `Map<Pair<FluidType,Integer>, CompressorRecipe>` (this
port likely already has or can trivially add a `Pair`/`Tuple` utility — CE's own
`com.hbm.util.Tuple.Pair` is a 2-line record-equivalent, no exotic dependency). **This is a "build the
machine first" file** — recommend sequencing: (1) `machine_compressor`/`machine_compressor_compact`
block + block entity (a straightforward single-fluid-in/single-fluid-out processing block, similar
shape to the already-built Electrolyser/Refinery/Centrifuge block entities this port has working
precedent for), (2) then the 5-entry recipe class is nearly free — every fluid and the `pressure`
data-model field it needs are already in place.

## Open questions / risks

1. **CE's blueprint-pool system (`setPools`/`setPools528`/`POOL_PREFIX_*`, 77 of 357
   `AssemblyMachineRecipes` entries) is a real CE progression-gating mechanic** (recipes only usable
   once a matching "blueprint" item is discovered/crafted, per `AssemblerRecipe`'s own javadoc
   referencing a not-yet-built "blueprint-pool system... once it lands"). Porting the recipe data
   without this gate means every recipe is trivially available from the start, a genuine behavior
   change from CE, not just a missing "nice to have." Whether this matters is a game-design call this
   research task cannot make — flagging it explicitly so the implement wave doesn't silently drop
   real CE progression structure without someone deciding that's acceptable.
2. **CE's `GeneralConfig.enable528` toggle** gates several recipe pairs in `AssemblyMachineRecipes`
   (e.g. `ass.chip`/`ass.chipBismoid`/etc. only registered `if(!GeneralConfig.enable528)`; several
   `setPools528(...)` alt-paths exist specifically for the `enable528`-on case). This is CE's
   internal "528 rebalance" config flag from its own version history — whether this port should adopt
   one config state, both, or a merged always-on set was not resolved by this research and needs an
   explicit decision (recommend: default to the `!enable528` / classic path, since that's what CE
   ships with by default, and treat the `528` alt-paths as a stretch goal).
3. **`recipeSet`/`recipeSets[2]` in `PedestalRecipes.java`** (`.set(1)` used on 9 of the 17 entries)
   was not fully traced to its consumer beyond the `recipeSets[Math.abs(set) % 2]` bucketing in
   `register()` — its actual gameplay purpose (two independent ring "channels"? an
   anti-conflict/registration-order concern?) is unclear from the file alone and would need a read
   of `BlockPedestal.java`'s full trigger logic (only partially read here) or CE's `Pedestal.java`
   groovy-integration script to resolve with confidence. Low risk either way since it's metadata, not
   ingredient logic.
4. **`HbmCapability.getData(player).getReputation()`** (used by `PedestalRecipes`' GOOD_KARMA/
   BAD_KARMA conditions) — this research did not check whether this port already has an equivalent
   player-reputation/karma capability from an earlier phase (Phase 3/4 weapon or faction systems
   might have built one). If it doesn't exist, that's a third dependency (beyond the block mechanism
   and the gun-item family) blocking 3 of the 17 Pedestal entries specifically.
5. **The exact fluid-pack loop multiplier (~310-320) is an estimate**, not a script-verified exact
   count — it depends on the precise runtime size of `Fluids.getInNiceOrder()` (this port's own
   `Fluids.java` field count, ≈158 per Phase 6's `PARITY_REPORT.md` §3.3, was used as the basis) and
   how many carry `hasNoContainer()`(1 confirmed in CE). Treat as ±10%, not exact, consistent with
   this project's own established confidence-tier convention.
6. **This task did not verify CE's gun-item family status at all** (needed to unblock
   `PedestalRecipes`) — that's squarely another task's scope (weapon/gun crafting, likely a sibling
   `mrec-*` or `crafting/*` task), flagged here only because it's this file's hard dependency, not
   independently researched.

## File/line references for the implement wave

- CE source (read in full): `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/
  AssemblyMachineRecipes.java` (1,132 lines), `PedestalRecipes.java` (222 lines),
  `ElectrolyserFluidRecipes.java` (134 lines), `CompressorRecipes.java` (98 lines);
  `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/BlockPedestal.java` (lines 60-135, 224+
  read for the ring-scan/trigger mechanism); `upstream/hbm-ce/src/main/java/com/hbm/inventory/
  fluid/Fluids.java` (lines 1071-1090 for `getInNiceOrder()`; field declarations for fluid-count
  cross-check); `upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java` (lines 1130-1134,
  1816-1821 for the bare `sulfur`/`niter`/`fluorite` vs. `crystal_*` distinction).
- Port source (read in full or targeted): `src/main/java/com/hbm/inventory/recipes/
  AssemblerRecipe.java` (218 lines, full), `ProcessingRecipes.java` (lines 1-60), `HbmRecipes.java`
  (lines 1-40); `src/main/java/com/hbm/inventory/recipes/chem/ElectrolyserFluidRecipes.java` (68
  lines, full); `src/main/java/com/hbm/inventory/fluid/FluidStack.java` (56 lines, full — confirms
  `pressure` field + `CODEC`/`STREAM_CODEC` already exist); `src/main/java/com/hbm/inventory/
  material/Mats.java` (head + `MAT_STEEL`/`MAT_TITANIUM`/`MAT_COPPER`/`MAT_LEAD`/`MAT_GOLD`
  declarations) and `MaterialShapes.java` (142 lines, full); `src/main/java/com/hbm/items/
  MaterialItemGenerator.java` (60 lines, full); `src/main/java/com/hbm/items/
  PlateCrystalWasteItems.java` (lines 201-206); `src/main/java/com/hbm/datagen/
  ModRecipeProvider.java` (lines 1-90, 300-430, for the `BILLET_SETS` table-loop convention);
  `src/main/resources/data/hbm/recipe/assembler/*.json` (13 files, all read/diffed).
