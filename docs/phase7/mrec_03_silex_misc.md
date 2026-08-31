# mrec-03-silex-misc — SILEX, Mixer, Heat, FluidCombustion recipe-data research

## Scope confirmed

Four CE machine-recipe registrar classes read in full, all under
`upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/`:

| CE file | Lines | Structure |
|---|---:|---|
| `SILEXRecipes.java` | 821 | One `register()` method: 13 hand-written static `recipes.put(...)` calls, then a single `for(int i=0;i<5;i++)` loop containing **51** `recipes.put(...)` call sites per iteration (255 total), then 27 more hand-written static calls after the loop. **295 total recipe entries** in the `LinkedHashMap<Object, SILEXRecipe> recipes` map (counted by hand, every `recipes.put(` call site, loop-corrected ×5). Plus a small `itemTranslation`/`dictTranslation` alias-map layer (6 + 2 entries) and a `tinyWasteTranslation` fallback-derivation map (4 entries) that are not separate recipes but affect lookup. |
| `MixerRecipes.java` | 220 | One `registerDefaults()` method (CE's `SerializableRecipe` subclass): **40** distinct `register(Fluids.X, ...)` call sites (flat list, no loop), 7 of which pass 2-4 competing `MixerRecipe` array elements instead of 1, for **50 total `MixerRecipe` objects**. Keyed by *output* `FluidType`, `HashMap<FluidType, MixerRecipe[]>`. |
| `HeatRecipes.java` | 129 | One `registerDefaults()` method (also a `SerializableRecipe` subclass): **7** `addBoilAndCoolRecipe`/`addCoolRecipe` calls (flat list), each populating both a `boilRecipes` and a `coolRecipes` `HashMap<FluidType, HeatRecipe>` (so 7 boil + 8 cool = 15 total map entries, since one call is cool-only). |
| `FluidCombustionRecipes.java` | 92 | One `registerFluidCombustionRecipes()` method: **24** `addBurnableFluid(FluidType, int)` calls against this port's own `Fluids` catalog, plus **14** `addBurnableFluid(String, int)` calls against lowercase compat-mod fluid-id strings (`"liquidhydrogen"`, `"biodiesel"`, etc. — other 1.12 mods' fluid registry names, not CE's own `Fluids` constants; each is self-guarded by `if(Fluids.fromName(fluid) != Fluids.NONE)` and, given CE's own `Fluids` constant names are uppercase, almost certainly resolves to `NONE`/no-op even inside CE itself). `HashMap<FluidType, Integer>`. |

None of these four files touch `Mats`/`MaterialShapes`/the crucible seam at all — they operate on
discrete `ItemStack`/`FluidStack` keys directly, so **none of them is blocked on the not-yet-ported
Crucible/`MatDistribution` system** that dominates the rest of Phase 7's scope. This is good news for
sequencing: this group can be ported in parallel with, not after, the Crucible work.

## Already covered by this port

### SILEX — real gap, not "~7 entries", it's 6

`src/main/java/com/hbm/inventory/recipes/chem/SILEXRecipes.java` (115 lines) has **6**
`RECIPES.put(...)` calls (lines 49, 54, 59, 64, 70, 76), not ~7:

| Port entry | CE source line | Status |
|---|---|---|
| `ingot_uranium` → nugget_u235×1/nugget_u238×11 | SILEXRecipes.java:33 | Ported verbatim |
| `ingot_pu_mix` → nugget_pu239×6/nugget_pu240×3 | :38 | Ported verbatim |
| `ingot_am_mix` → nugget_am241×3/nugget_am242×6 | :43 | Ported verbatim |
| `ingot_schraranium` → nugget_schrabidium/uranium/neptunium | :56 | Ported verbatim |
| `Items.DIAMOND` → crystal_sulfur/powder_aluminium/powder_cobalt | :90 (CE key was `Items.DYE` meta 4, i.e. lapis) | **Bug**: the port substituted `Items.DIAMOND` as the recipe key for what was CE's **lapis-dye** SILEX recipe. 1.21.1 has a native `Items.LAPIS_LAZULI` (dyes are no longer meta-1 `Items.DYE`) — this substitution looks like a mistaken stand-in rather than a deliberate one; there is no comment explaining it, unlike every other substitution in the same file's javadoc. **Flag for the implement wave to fix to `Items.LAPIS_LAZULI`.** |
| `Items.GRAVEL` → flint/powder_boron/powder_lithium/crystal_fluorite | :657 | Ported verbatim (CE's `fluorite` → `PlateCrystalWasteItems.CRYSTAL_FLUORITE` substitution, documented) |

**Real remaining gap: 289 of CE's 295 entries (98%)** — everything else: `ingot_australium`,
`crystal_schraranium`, `ore_tikite`, `crystal_trixite`, the vitriol/death/redmud/UF6/PUF6/fullerene
fluid-keyed entries (6), the entire 255-entry RBMK-pellet reprocessing loop, and all 27 post-loop
nuclear-waste/fallout entries. See catalog below.

### Mixer — the assignment's premise is stale; real state is much better than "~1 entry"

`src/main/java/com/hbm/inventory/recipes/MixerRecipes.java` (218 lines) — **not** in a `chem` subpackage
(it sits directly under `com.hbm.inventory.recipes`, matching CE's own package) — already has **11**
distinct `register(Fluids.X, ...)` call sites (`CRYOGEL`, `NITAN`, `FISHOIL`, `SUNFLOWEROIL`,
`THORIUM_SALT`, `CHLOROCALCITE_SOLUTION`, `DIESEL_REFORM`, `SYNGAS`, `LUBRICANT`, `NITROGLYCERIN`,
`OXYHYDROGEN`), 3 of which carry the real CE 2-recipe competing array, for **14 total `MixerRecipe`
objects** — i.e. **11/40 = 27.5%** of CE's fluid-output keys, **14/50 = 28%** of CE's recipe objects.
The class's own javadoc documents every substitution and every intentional trim (e.g. `Items.FISH`
meta-wildcard → `Items.COD`, `Blocks.DOUBLE_PLANT` meta 0 → `Blocks.SUNFLOWER`), and is already wired
live into `MachineMixerBlockEntity` via `MixerRecipes.findMatch(...)`
(`src/main/java/com/hbm/blockentity/machine/MachineMixerBlockEntity.java:156`). **Correction for the
task brief: this is not "~1 entry" — whoever wrote that count likely grepped for a literal
`.put(` call site (CE's own `MixerRecipes.java` uses `.put(`, but this port's version calls a private
`register(...)` helper instead, which a naive `.put(` grep would miss entirely).**

**Real remaining gap: 29 of CE's 40 fluid-output keys (72.5%)** — see the dependency-check table below;
crucially, **not all 29 are actually blocked** — several were left out only because the port's own
javadoc cites a missing plain "sulfur/fluorite/niter dust" item, but this exact codebase has since
established `PlateCrystalWasteItems.CRYSTAL_SULFUR`/`CRYSTAL_FLUORITE`/`CRYSTAL_NITER` as the standing
substitution for that exact gap (used in this port's own `SILEXRecipes.java` and `RefineryRecipes.java`)
— so several "blocked" entries in the class's own comments are now portable using a precedent already
in the codebase. See "Item/registry dependency check" below.

### HeatRecipes / FluidCombustionRecipes — confirmed untouched

`grep -rl "HeatRecipes"` and `grep -rl "FluidCombustionRecipes"` over `src/main/java/com/hbm` both
return **zero hits** — neither class exists anywhere in the port, not even as an empty stub. Confirmed.

## Full recipe/entry catalog OR representative pattern

### SILEX (LARGE — 295 entries, representative sample + generating pattern)

**Total: 295 recipe entries** (13 pre-loop + 255 loop-generated + 27 post-loop), computed by hand-counting every `recipes.put(` call site with loop correction (see Scope section). CE source: `SILEXRecipes.java:29-666`.

**Representative sample (pre-loop static entries, 13 of 13 — all of them, since this sub-group is small):**

| Key (input) | fluidProduced | fluidConsumed | laserStrength | Outputs (item ×weight) | CE line |
|---|---:|---:|---|---|---|
| `ingot_uranium` (also: `U.dust()`→`U.ingot()` dict alias) | 900 | 100 | VISIBLE | nugget_u235×1, nugget_u238×11 | 31-36 |
| `ingot_pu_mix` | 900 | 100 | ordinal 2 (VISIBLE) | nugget_pu239×6, nugget_pu240×3 | 38-41 |
| `ingot_am_mix` | 900 | 100 | ordinal 2 | nugget_am241×3, nugget_am242×6 | 43-46 |
| `PU.ingot()` (dict alias, → `ingot_plutonium`) | 900 | 100 | ordinal 2 | nugget_pu238×3, nugget_pu239×4, nugget_pu240×2 | 48-54 |
| `ingot_schraranium` | 900 | 100 | ordinal 2 | nugget_schrabidium×4, nugget_uranium×3, nugget_neptunium×2 | 56-60 |
| `ingot_australium` (translation: powder_australium→this) | 900 | 100 | ordinal 2 | nugget_australium_lesser×5, nugget_australium_greater×1 | 62-66 |
| `crystal_schraranium` | 900 | 100 | ordinal 3 (UV) | nugget_schrabidium×5, nugget_uranium×2, nugget_neptunium×2 | 68-72 |
| `ore_tikite` (Block) | 900 | 100 | UV | powder_plutonium×2, powder_cobalt×3, powder_niobium×3, powder_nitan_mix×2 | 74-79 |
| `crystal_trixite` | 1200 | 100 | UV | powder_plutonium×2, powder_cobalt×3, powder_niobium×3, powder_nitan_mix×1, powder_spark_mix×1 | 81-87 |
| `Items.DYE` meta 4 (lapis; translation: powder_lapis→this) | 100 | 100 | ordinal 1 (IR) | sulfur×4, powder_aluminium×3, powder_cobalt×3 | 89-94 |
| `fluid_icon`(DEATH) | 1000 | 1000 | ordinal 4 (GAMMA) | powder_impure_osmiridium×1 | 96-98 |
| `fluid_icon`(VITRIOL) | 1000 | 300 | IR | powder_bromine×5, powder_iodine×5, powder_iron×5, sulfur×15 | 100-105 |
| `fluid_icon`(REDMUD) | 300 | 50 | VISIBLE | powder_aluminium×10, powder_neodymium_tiny×3(stack)×5(wt), powder_boron_tiny×3×5, nugget_zirconium×5, powder_iron×20, powder_titanium×15, powder_sodium×10 | 107-115 |

**The 255-entry RBMK-pellet loop — exact generating pattern** (CE lines 117-472, `for(int i=0;i<5;i++)`):

For each of **~15 pellet-family groups** (UEU, MEU, HEU233, HEU235, UZH, TH232/thmeu, LEP, MEP, HEP239,
HEP241, MEN, HEN, MOX, LEAUS, HEAUS, LES, MES, HES, BALEFIRE, FLASHGOLD, FLASHLEAD, POBE, PUBE, RABE,
DRX, ZFB-BI, ZFB-PU241, ZFB-RG-AM — 28 families, not all producing both a `i` and `i+5` pair), the loop
registers 1 or 2 `recipes.put(new ComparableStack(ModItems.rbmk_pellet_X, 1, i), ...)` /
`...(rbmk_pellet_X, 1, i+5), ...)` calls per iteration. **`i` (0-4) is the "fresh→depleted" burnup
stage; `i+5` (5-9) is the same stage but with an extra `powder_xe135_tiny×1` output** (xenon-135
poisoning byproduct) prepended, and the base-material yield is reduced by 1 in most `i+5` variants
relative to the `i` sibling at the same `i`. All 15 two-sided families use `i` and `i+5`; 6 families
(BALEFIRE, FLASHGOLD, FLASHLEAD, POBE, RABE, and one PUBE side) register **only `i`** (no xenon variant)
— an intentional asymmetry, not an omission, per CE's own inline `//TODO: Readd xenon processing...`
comments on the LES/MES/HES families explaining a 6-output NEI display cap as the reason some families
were trimmed to skip xenon.

Representative sample of loop entries (i=0, first iteration; a table-driven port would generate all 5×51=255 from a formula table like this):

| Pellet key (meta) | fluidProduced | consumed | laser | Outputs @ i=0 | Yield formula (general i) |
|---|---:|---:|---|---|---|
| `rbmk_pellet_ueu`(0) | 600 | 100 | ordinal 1 | nugget_uranium×86, nugget_pu239×10, waste_long_tiny(U235)×2, waste_short_tiny(U235)×2 | uranium=`86-11i`; pu=`i<2?pu239:pu_mix`, wt=`10+3i`; long=`2+3i`; short=`2+5i` |
| `rbmk_pellet_ueu`(5) | 600 | 100 | ordinal 1 | + powder_xe135_tiny×1, nugget_uranium×86, nugget_pu239×10, waste_long_tiny×2, waste_short_tiny×1 | same as above but short=`1+5i`, xe135 flat 1 |
| `rbmk_pellet_meu`(0) | 600 | 100 | 1 | nugget_uranium_fuel×84, nugget_pu239×6, waste_long×4, waste_short×6 | fuel=`84-16i`; pu wt=`i<1?pu239:pu_mix`, `6+4i`; long=`4+5i`; short=`6+7i` |
| `rbmk_pellet_hep241`(0) | 600 | 100 | ordinal 2 | nugget_pu241×85, waste_short_tiny(PU241)×15 | pu241=`85-20i`; waste=`15+20i` |
| `rbmk_pellet_drx`(0 and 5) | 600 | 100 | ordinal 4 | 6× `ModItems.undefined`×1 (joke/mystery-box recipe) | flat, no `i` dependence |
| `rbmk_pellet_zfb_bismuth`(0) | 600 | 100 | 2 | nugget_uranium×50, nugget_pu241×50, nugget_bismuth×50, nugget_zirconium×150 | uranium/pu241=`50-10i`; bismuth=`50+20i`; zirc flat 150 |
| `rbmk_pellet_pu238be`(0) | 600 | 100 | 1 | nugget_pu238×45, nugget_beryllium×45, nugget_lead×3, waste_tiny×2, powder_coal_tiny×5 | pu238/be=`45-10i`; lead=`3+5i`; waste=`2+5i`; coal=`5+10i` |

A table-driven implement-wave loop would look like:
```
record RbmkPelletRow(String pelletId, boolean hasXenonVariant, BiFunction<Integer,Boolean,SILEXRecipe> recipeFn) { ... }
static final RbmkPelletRow[] RBMK_PELLET_ROWS = { ... 28 rows ... };
for (RbmkPelletRow row : RBMK_PELLET_ROWS) {
    for (int i = 0; i < 5; i++) {
        RECIPES.put(pelletKey(row.pelletId(), i), row.recipeFn().apply(i, false));
        if (row.hasXenonVariant()) RECIPES.put(pelletKey(row.pelletId(), i + 5), row.recipeFn().apply(i, true));
    }
}
```
— but note the per-family yield **formulas differ enough** (some linear in `i` with different
slopes/intercepts per output, a few with `i<1`/`i<2` conditional branches for which plutonium isotope
is emitted) that a single generic formula-table won't cover it cleanly; the pragmatic approach is a
`Function<Integer, SILEXRecipe>` per family (28 small lambdas) rather than trying to force one numeric
table to fit all 28 — CE's own source is already effectively that shape (28 hand-written closures
inline in one loop body), so a straight structural transcription (not a re-derivation) is lowest-risk.

**The 27 post-loop static entries** (CE lines 474-665): 24 nuclear-waste-reprocessing recipes (one
`recipes.put` per `{long,long_depleted,short,short_depleted}` × `{URANIUM235, URANIUM233, PLUTONIUM239,
PLUTONIUM240, PLUTONIUM241, THORIUM, NEPTUNIUM, SCHRABIDIUM}` combination that CE actually populates —
not a full 4×8=32 cross product, some combinations are skipped, e.g. THORIUM/PLUTONIUM239/240/241 have
no `long`/`short` counterpart in both directions) + `fallout` (1) + `Blocks.GRAVEL` (1, already ported)
+ `fluid_icon`(FULLERENE) (1). Representative rows:

| Key | Outputs |
|---|---|
| `nuclear_waste_long`(URANIUM235) | nugget_neptunium×20, nugget_pu239×45, nugget_pu240×20, nugget_technetium×15 |
| `nuclear_waste_short_depleted`(URANIUM235) | nugget_zirconium×10, dust_tiny×32, nugget_lead×22, nugget_u238×5, nugget_bismuth×15, waste_tiny×16 |
| `nuclear_waste_long`(SCHRABIDIUM) | nugget_solinium×25, nugget_euphemium×18, nugget_gh336×16, nugget_tantalium×8, powder_neodymium_tiny×8, waste_tiny×25 |
| `fallout` | dust_tiny×90, nugget_co60×2, powder_sr90_tiny×3, powder_i131_tiny×1, powder_cs137_tiny×3, nugget_au198×1 |
| `fluid_icon`(FULLERENE) | `DictFrame.fromOne(powder_ash, EnumAshType.FULLERENE)`×1 |

### Mixer (SMALL — 40 entries, full catalog)

Every `register(Fluids.X, ...)` call in CE's `MixerRecipes.java`, marked **PORTED** if already in this
port's file:

| Output fluid | Recipe(s): stack1 / stack2 / solid | Status |
|---|---|---|
| COOLANT | WATER 1800 / — / `KNO.dust()`(niter) | Missing — ready (→ `CRYSTAL_NITER`) |
| CRYOGEL | COOLANT 1800 / — / powder_ice | **PORTED** |
| NITAN | KEROSENE 600 / MERCURY 200 / powder_nitan_mix | **PORTED** |
| FRACKSOL | (a) SULFURIC_ACID 900/PETROLEUM 100; (b) WATER 1000/PETROLEUM 100/`S.dust()` | Missing — **ready** (both sides; `S.dust()`→`CRYSTAL_SULFUR`, contrary to the port's own javadoc claiming it's blocked — see Open Questions) |
| ENDERJUICE | XPJUICE 500 / — / `DIAMOND.dust()` | Missing — ready (→ `Items.DIAMOND`) |
| SALIENT | SEEDSLURRY 500 / BLOOD 500 / — | Missing — ready |
| COLLOID | WATER 500 / — / `ModItems.dust` | Missing — **blocked** (generic `dust` item not registered) |
| PHOSGENE | UNSATURATEDS 500 / CHLORINE 500 / — | Missing — ready |
| MUSTARDGAS | REFORMGAS 750 / CHLORINE 250 / `S.dust()` | Missing — ready (→ `CRYSTAL_SULFUR`) |
| IONGEL | WATER 1000 / HYDROGEN 200 / `pellet_charged` | Missing — **blocked** (item not registered) |
| EGG | RADIOSOLVENT 500 / — / `Items.EGG` | Missing — ready |
| FISHOIL | — / — / `Items.FISH` (wildcard) | **PORTED** (→ `Items.COD`) |
| SUNFLOWEROIL | — / — / `Blocks.DOUBLE_PLANT` meta 0 | **PORTED** (→ `Blocks.SUNFLOWER`) |
| FULLERENE | RADIOSOLVENT 500 / — / `powder_ash`(SOOT) | Missing — **blocked** (`powder_ash` family not registered) |
| SOLVENT | 4 competing: {NAPHTHA,NAPHTHA_CRACK,NAPHTHA_DS,NAPHTHA_COKER} 500 / AROMATICS 500 | Missing — ready (all 4 fluids exist) |
| SULFURIC_ACID | PEROXIDE 800 / — / `S.dust()` | Missing — ready (→ `CRYSTAL_SULFUR`) |
| NITRIC_ACID | SULFURIC_ACID 500 / — / `KNO.dust()` | Missing — ready (→ `CRYSTAL_NITER`) |
| RADIOSOLVENT | REFORMGAS 750 / CHLORINE 250 / — | Missing — ready |
| SCHRABIDIC | SAS3 8000 / PEROXIDE 6000 / `pellet_charged` | Missing — **blocked** (item not registered) |
| PETROIL | RECLAIMED 800 / LUBRICANT 200 / — | Missing — ready |
| LUBRICANT | (a) HEATINGOIL 500/UNSATURATEDS 500; (b) FISHOIL 800/ETHANOL 200; (c) SUNFLOWEROIL 800/ETHANOL 200 | **PORTED** (a,b only — CE's (c) dropped, documented as "redundant with FISHOIL for this trimmed set") |
| BIOFUEL | (a) FISHOIL 500/WOODOIL 500; (b) SUNFLOWEROIL 500/WOODOIL 500 | Missing — ready (both) |
| NITROGLYCERIN | (a) PETROLEUM 1000/NITRIC_ACID 1000; (b) FISHOIL 500/NITRIC_ACID 500 | **PORTED** (both) |
| THORIUM_SALT | CHLORINE 1000 / — / `TH232.dust()` | **PORTED** (→ `POWDER_THORIUM`) |
| SYNGAS | COALOIL 500 / STEAM 500 / — | **PORTED** |
| OXYHYDROGEN | (a) HYDROGEN 500/AIR 2000; (b) HYDROGEN 500/OXYGEN 500 | **PORTED** (both) |
| PETROIL_LEADED | PETROIL 10000 / — / `fuel_additive` | Missing — **blocked** (item not registered) |
| GASOLINE_LEADED | GASOLINE 10000 / — / `fuel_additive` | Missing — **blocked** (item not registered) |
| COALGAS_LEADED | COALGAS 10000 / — / `fuel_additive` | Missing — **blocked** (item not registered) |
| DIESEL_REFORM | DIESEL 900 / REFORMATE 100 / — | **PORTED** |
| DIESEL_CRACK_REFORM | DIESEL_CRACK 900 / REFORMATE 100 / — | Missing — ready |
| KEROSENE_REFORM | KEROSENE 900 / REFORMATE 100 / — | Missing — ready |
| CHLOROCALCITE_SOLUTION | WATER 250 / NITRIC_ACID 250 / `CHLOROCALCITE.dust()` | **PORTED** (→ `POWDER_CHLOROCALCITE`) |
| CHLOROCALCITE_MIX | CHLOROCALCITE_SOLUTION 500 / SULFURIC_ACID 500 / `powder_flux` | Missing — **ready** (`POWDER_FLUX` exists) |
| PHEROMONE_M | PHEROMONE 1500 / BLOOD 500 / `pill_herbal` | Missing — **ready** (`ItemPill`/`FoodItems` has `pill_herbal`) |
| BAUXITE_SOLUTION | LYE 50 / — / `stone_resource`(BAUXITE) | Missing — **ready** (`BlockResourceStone`/`EnumStoneType.BAUXITE` exists) |
| LYE | WATER 100 / — / `powder_ash`(WOOD) | Missing — **blocked** (`powder_ash` family not registered) |
| ALUMINA | (a) SODIUM_ALUMINATE 150/`F.dust()`×3; (b) SODIUM_ALUMINATE 150/`chunk_ore`(CRYOLITE) | Missing — **ready** (both: → `CRYSTAL_FLUORITE`; `chunk_ore`/`EnumChunkType` exists) |
| PERFLUOROMETHYL | PETROLEUM 1000 / UNSATURATEDS 500 / `F.dust()` | Missing — ready (→ `CRYSTAL_FLUORITE`) |
| BITUMEN | — / — / `ANY_TAR.any()` (ore-dict tag) | Missing — **blocked** (`oil_tar`/`EnumTarType` not registered; confirmed by `RefineryRecipes.java`'s own `TODO(items-followup)`) |

### HeatRecipes (SMALL — 7 pairs, full catalog)

| Cold fluid | Hot fluid | Heat units | Note |
|---|---|---:|---|
| WATER (1) | STEAM (100) | 100 | boil+cool both directions |
| STEAM (100) | SPENTSTEAM (1) | 100 | **cool-only** (`addCoolRecipe`, not `addBoilAndCoolRecipe`) — no reverse boil path in CE either |
| STEAM (10) | HOTSTEAM (1) | 15 | boil+cool |
| HOTSTEAM (10) | SUPERHOTSTEAM (1) | 30 | boil+cool |
| SUPERHOTSTEAM (10) | ULTRAHOTSTEAM (1) | 120 | boil+cool |
| OIL (1) | HOTOIL (1) | 300 | boil+cool |
| CRACKOIL (1) | HOTCRACKOIL (1) | 300 | boil+cool |
| COOLANT (1) | COOLANT_HOT (1) | 500 | boil+cool |

**Important finding: this mechanism is essentially dead code in CE itself.** A repo-wide grep for
`HeatRecipes\.` in CE finds exactly one consumer outside the class's own file:
`integration/groovy/script/Heat.java` (a KubeJS/GroovyScript-style datapack-customization binding, not
game logic). CE's real boiler (`TileEntityHeatBoiler.java`) hardcodes `Fluids.WATER`→`Fluids.STEAM`
directly (confirmed: `tanks[0] = new FluidTankNTM(Fluids.WATER,...)`, `tanks[1] = ...STEAM...`, no
`HeatRecipes` reference anywhere in the file) — and this port's own `SolarBoilerBlockEntity.java`
already replicates that exact hardcoded behavior (confirmed: comment cites "1 heat unit : ~50 water :
100 steam", `Fluids.WATER`/`Fluids.STEAM` tanks, no recipe-class lookup). **`HeatRecipes` has no live
gameplay consumer in CE today** — porting it would only matter for parity-count purposes or a future
Groovy/KubeJS-equivalent scripting layer (not in scope anywhere in this project). Low priority.

### FluidCombustionRecipes (SMALL — 24 real + 14 compat-string entries, full catalog)

| Fluid | TU/1000mB | | Fluid | TU/1000mB |
|---|---:|---|---|---:|
| HYDROGEN | 5 | | HEATINGOIL | 150 |
| DEUTERIUM | 5 | | BIOFUEL | 150 |
| TRITIUM | 5 | | DIESEL | 200 |
| OIL | 10 | | LIGHTOIL | 200 |
| HOTOIL | 10 | | KEROSENE | 300 |
| CRACKOIL | 10 | | GASOLINE | 800 |
| HOTCRACKOIL | 10 | | UNSATURATEDS | 1,000 |
| GAS | 10 | | NITAN | 2,000 |
| FISHOIL | 15 | | BALEFIRE | 10,000 |
| LUBRICANT | 20 | | | |
| AROMATICS | 25 | | | |
| PETROLEUM | 25 | | | |
| BIOGAS | 25 | | | |
| BITUMEN | 35 | | | |
| HEAVYOIL | 50 | | | |
| SMEAR | 50 | | | |
| ETHANOL | 75 | | | |
| RECLAIMED | 100 | | | |
| PETROIL | 125 | | | |
| NAPHTHA | 125 | | | |

Plus 14 lowercase-string calls (`"liquidhydrogen"`, `"liquiddeuterium"`, `"liquidtritium"`, `"crude_oil"`,
`"oilgc"`, `"fuel"`, `"refined_biofuel"`, `"pyrotheum"`, `"ethanol"`, `"plantoil"`, `"acetaldehyde"`,
`"biodiesel"`) — other 1.12 mods' fluid registry names (GregTech/Forestry/etc. naming convention),
self-guarded by `if(Fluids.fromName(fluid) != Fluids.NONE)`; since CE's own `Fluids` constant names are
all-caps and these strings are lowercase, `Fluids.fromName(...)` almost certainly returns `NONE` for
every one of them even inside CE, making this a no-op leftover block. **Not worth porting.**

**Important finding: also has essentially no real gameplay consumer.** Repo-wide grep for
`FluidCombustionRecipes\.`/`getFlameEnergy` in CE finds exactly two hits outside its own file:
`main/MainRegistry.java:329` (the `registerFluidCombustionRecipes()` bootstrap call) and
`inventory/gui/GUIMachineGasFlare.java:86` — used **only** to decide whether to render a cosmetic flame
icon on the Gas Flare machine's GUI (`if(...&& FluidCombustionRecipes.hasFuelRecipe(flare.tank.getTankType()))`).
The actual burn/no-burn gameplay logic in `TileEntityMachineGasFlare.java` checks the fluid's
`FT_Flammable` **trait**, not `FluidCombustionRecipes`'s TU value at all. This class's TU numbers are
**not consumed by any engine/generator/power calculation anywhere in CE** as far as this task's grep
found — it is cosmetic-GUI-only. Low priority, and moot until the Gas Flare machine itself is built
(see next section — it doesn't exist in this port yet).

## Item/registry dependency check

### SILEX ingredient families — spot-checked against this port's item registry

| Family | Port status |
|---|---|
| `rbmk_pellet_*` (all 30 pellet-type items CE's SILEX file references) | **All registered** — `MachineItems.java:418-449`, `registerRbmkPellet(...)`. **But the burnup-stage dimension (CE's `meta` 0-9) is now a data component**, not a separate item id — see Open Questions, this changes the recipe-key shape required. |
| `nugget_*`, `ingot_*` (uranium/plutonium/americium/schrabidium/zirconium/lead/bismuth/mercury/beryllium/polonium/technetium/gold198/pb209/ra226/gh336/solinium/euphemium/tantalium/australium/co60/*_fuel/etc.) | **All registered** — `IngotNuggetItems.java` (spot-checked ~50 names used across the SILEX file, all present). |
| `powder_*` (aluminium, cobalt, niobium, sr90/i131/cs137/xe135 + `_tiny` variants, bromine, iodine, iron, sodium, titanium, neodymium_tiny, boron_tiny, coal_tiny, balefire, cerium_tiny, lanthanium_tiny, impure_osmiridium, lithium, plutonium) | **All registered** — `BilletPowderItems.java`. |
| `crystal_*` (schraranium, trixite, sulfur, fluorite) | **All registered** — `PlateCrystalWasteItems.java`. |
| `nuclear_waste_long`/`nuclear_waste_short` base + `_tiny` variants (used as recipe **key** in the post-loop section) | **Registered** as flattened per-`WasteClass` ids (`nuclear_waste_long_uranium235`, etc.) — `SpecialItems.java:276-303`. |
| `nuclear_waste_long_depleted`/`_short_depleted` (12 of the 27 post-loop entries key on these) **and** `nuclear_waste_long_tiny`/`_short_tiny`/`_depleted_tiny` (used as **output** in every single one of the 255 loop entries) | **NOT registered.** `SpecialItems.java:270-274`'s own comment confirms: "CE's six sibling fields (`_tiny`, `_depleted`, `_depleted_tiny` for each) are a distinct open question." **This is the single biggest item-registry blocker for this file** — it blocks essentially the entire 255-entry RBMK loop (every recipe outputs `nuclear_waste_*_tiny`) and 12 of the 27 post-loop waste-reprocessing entries. |
| `dust`, `dust_tiny` (CE's generic unspecified-material filler item, used as output in ~15 post-loop waste entries) | **NOT registered** (0 hits for `"dust_tiny"`/plain `"dust"` anywhere in `items/`). |
| `undefined` (CE's `ItemCustomLore` joke placeholder, DRX pellet's 6-output "mystery" recipe) | **NOT registered** (0 hits). Low priority — it's a deliberately-silly troll recipe (`ChatFormatting.OBFUSCATED + "can't you hear..."` pellet name), safe to skip or stub. |
| `fluid_icon` (UF6/PUF6/DEATH/VITRIOL/REDMUD/FULLERENE — 6 entries keyed on this) | **Registered and structurally equivalent to CE** — `MachineItems.java:274`, `ItemFluidIcon` + `MachineDataComponents.FLUID_ID` stores the fluid id as a data component exactly the way CE stored it as item metadata. **But see Open Questions — the machine-side lookup mechanism these 6 entries need is not yet built.** |
| `ore_tikite` (Block) | **Registered** — `OreBlocks.java:131`, plus world-gen placement in `OreWorldGenFeatures.java:158`. |
| `Items.DYE` meta 4 / lapis | Should be `Items.LAPIS_LAZULI` in 1.21.1 — vanilla item, trivially available, just needs the port's existing mis-substitution (`Items.DIAMOND`) corrected. |

**Ready to port now** (no item blocker): the 13 pre-loop static entries (6 already done, 7 more ready:
`ingot_australium`, `crystal_schraranium`, `ore_tikite`, `crystal_trixite`, lapis-fix, `fluid_icon`
DEATH/VITRIOL/REDMUD if the fluid-tank lookup path is built) + all 24 nugget/ingot-keyed post-loop
waste entries that key on the **base** (non-depleted) waste items (12 of the 24).

**Blocked** on item registration: the entire 255-entry RBMK loop (needs `nuclear_waste_long_tiny` /
`nuclear_waste_short_tiny`), the 12 `_depleted`-keyed post-loop entries (needs
`nuclear_waste_*_depleted`), the ~15 entries outputting `dust`/`dust_tiny`, and the 2 DRX entries
(needs `undefined`, or can be trivially reskinned/dropped).

### Mixer ingredient families

Covered inline in the full catalog table above (Ready/Blocked column per row). Summary: **20 of 29
missing entries are ready to port now** using the codebase's own established `crystal_*` substitution
precedent (sulfur/fluorite/niter); **9 are genuinely blocked** on unregistered items: `ModItems.dust`
(COLLOID), `pellet_charged` (IONGEL, SCHRABIDIC), `powder_ash` family (FULLERENE, LYE),
`fuel_additive` (PETROIL_LEADED, GASOLINE_LEADED, COALGAS_LEADED), and `oil_tar`/tar tag (BITUMEN).

### HeatRecipes / FluidCombustionRecipes ingredient families

All fluids referenced by both (STEAM, SPENTSTEAM, HOTSTEAM, SUPERHOTSTEAM, ULTRAHOTSTEAM, OIL, HOTOIL,
CRACKOIL, HOTCRACKOIL, COOLANT, COOLANT_HOT + all 24 combustion fluids) are **already registered** in
`Fluids.java` (spot-checked every name). **Fully ready to port now, zero item/fluid blockers** — the
only reason to deprioritize them is that neither has a live gameplay consumer yet (see above).

## Machine block/block-entity correspondence

| CE recipe class | CE consumer(s) | Port-side machine | Verdict |
|---|---|---|---|
| `SILEXRecipes` | `TileEntitySILEX.java` | `SilexBlockEntity.java` + `SilexBlock.java` + `SilexMenu`/`SilexScreen` + JEI `SilexCategory` — **fully built and wired** | Machine exists; **only recipe data is missing** for the item-slot path. The fluid-tank path needs block-entity logic added too (see Open Questions). |
| `MixerRecipes` | `TileEntityMachineMixer.java` | `MachineMixerBlockEntity.java` + `MachineMixerBlock.java` + `MachineMixerMenu`/`MachineMixerScreen` + JEI `MixerCategory` — **fully built and wired** | Machine exists; **only recipe data is missing.** No machine-logic changes needed — `MixerRecipes.findMatch(...)` already consumes the map generically. |
| `HeatRecipes` | Nominally none in real CE gameplay (see above) | `SolarBoilerBlockEntity`, `RBMKBoilerBlockEntity`, etc. already exist and hardcode their own conversions | No machine work needed either way — this recipe class has no real consumer to wire up even in CE. |
| `FluidCombustionRecipes` | `TileEntityMachineGasFlare.java` (GUI-cosmetic only) | **Does not exist** — `grep -rli "gasflare\|gas_flare"` over `src/main/java/com/hbm` returns zero hits | The Gas Flare machine itself (block, block entity, GUI) is **not built** in this port at all. Porting this recipe class alone has no effect until that machine exists — and even then it's purely a flame-icon cosmetic. |

## Recommended 1.21.1 implementation shape

- **SILEX — continue as a custom Java data class, not JSON.** Multi-output weighted-random selection,
  a laser-wavelength-gated progress-rate formula, and (for the RBMK-pellet section) a data-component-
  keyed match rather than plain item identity are all outside vanilla `Recipe<RecipeInput>`'s shape —
  exactly the precedent this port's own 6-entry file already establishes. Two concrete changes needed
  beyond just adding entries:
  1. **Widen the map key type.** The port's `RECIPES` field is currently
     `Map<ComparableStack, SILEXRecipe>`. The RBMK-pellet loop's key needs the pellet's
     `MachineDataComponents.RBMK_PELLET_STAGE` component (0-9) folded in — this port's own
     `RecipesCommon.ComparableStack` deliberately does **not** carry a meta/damage dimension anymore
     (see that class's own header comment), so the correct tool is
     `RecipesCommon.NbtComparableStack` (exact-component match via
     `ItemStack.isSameItemSameComponents`), already used elsewhere in this codebase for exactly this
     kind of component-bearing-item recipe key. Change `RECIPES` to `Map<AStack, SILEXRecipe>` (or
     `Map<Object, SILEXRecipe>`, matching CE's own type) so both `ComparableStack` (plain-item entries)
     and `NbtComparableStack` (pellet-stage entries) can coexist as keys, same as CE's original.
  2. **Extend `SilexBlockEntity` for the 6 fluid-tank-keyed entries** (UF6/PUF6/DEATH/VITRIOL/REDMUD/
     FULLERENE). This is genuinely machine-logic work, not pure data — `SilexBlockEntity`'s own javadoc
     already documents this exact gap ("No direct-fluid-input reprocessing path... none of this pass's
     ported SILEXRecipes entries need the fluid-direct path anyway" — that caveat stops being true once
     these 6 entries are added). The pattern to replicate from CE's `TileEntitySILEX.java:186-198`:
     each tick, synthesize `ItemFluidIcon.make(MachineItems.FLUID_ICON.get(), tank.getTankType(), 1)`,
     wrap in `NbtComparableStack`, and feed it through the same `SILEXRecipes.getOutput(...)` lookup —
     alongside, not instead of, the existing item-slot path.
- **Mixer — continue exactly the established pattern**, no design changes: it's already a plain Java
  `Map<FluidType, MixerRecipe[]>` populated by a private `register(...)` helper. Adding the 20 "ready"
  entries is purely mechanical (copy CE's constructor calls, substitute the already-established
  `CRYSTAL_SULFUR`/`CRYSTAL_FLUORITE`/`CRYSTAL_NITER` items where CE used a plain ore-dict dust).
- **HeatRecipes — if ported, a trivial plain-Java `Map<FluidType, HeatRecipe>` class** (two maps,
  `boilRecipes`/`coolRecipes`), no JSON layer, no machine wiring needed (nothing in this port's boiler
  block entities would consume it, matching CE's own behavior). **Recommend deprioritizing** relative to
  every other item in this port's Phase 7 backlog — it has no live CE gameplay consumer to port
  faithfully *to*.
- **FluidCombustionRecipes — if ported, a trivial `Map<FluidType, Integer>` class.** Recommend
  deprioritizing further than HeatRecipes: it additionally requires building an entire new machine (Gas
  Flare block + block entity + GUI) from scratch before the recipe data has any effect at all, and even
  then only drives a cosmetic flame icon, not real fuel-value gameplay.

## Open questions / risks

1. **RBMK pellet burnup-stage key shape.** Confirmed CE's `EnumWavelengths` ordinals match this port's
   `ItemFELCrystal.EnumWavelengths` 1:1 (`NULL=0, IR=1, VISIBLE=2, UV=3, GAMMA=4, DRX=5` on both sides —
   diffed both enum bodies directly), so the `int laserStrength` constructor overload's raw integers in
   CE's source can be transcribed unchanged. But the RBMK-pellet recipe *keys* need the
   `NbtComparableStack`/data-component approach described above — this is the one place in this file
   group where a literal 1:1 transcription of CE's `new ComparableStack(item, 1, meta)` calls will not
   compile as-is and needs the substitution documented above. Flagging explicitly so the implement wave
   doesn't hit this as a surprise mid-port.
2. **`Items.DIAMOND` mis-substitution already in the port's shipped `SILEXRecipes.java`.** Line 70-74 —
   looks like an unintentional placeholder for CE's lapis-dye recipe (`Items.DYE` meta 4), not a
   documented deliberate substitution (unlike every other substitution in the same file, which all carry
   an inline comment). Should be `Items.LAPIS_LAZULI`. Flagged as a fix-while-touching item, not a
   blocker.
3. **`MixerRecipes.java`'s own javadoc reasoning for excluding `FRACKSOL` is now stale.** It says
   FRACKSOL's second recipe "needs a sulfur-dust item this port hasn't registered yet" — but
   `PlateCrystalWasteItems.CRYSTAL_SULFUR` exists and is already the standing substitute for exactly
   this gap elsewhere in this same codebase (this port's own `SILEXRecipes.java`'s `Items.DIAMOND`
   recipe uses it). The same is true for `SULFURIC_ACID`/`MUSTARDGAS`, which the class's javadoc doesn't
   even mention as candidates. Recommend the implement wave treat "needs sulfur/fluorite/niter dust" as
   resolved by the `CRYSTAL_*` substitution across this whole file, not just the entries already using
   it.
4. **CE's `itemTranslation`/`dictTranslation` alias layer in `SILEXRecipes.java`** (6 + 2 entries — e.g.
   `powder_lapis`→lapis-dye-recipe-key, `U.dust()`→`U.ingot()` dict-string alias) is an OreDictionary-
   string-based indirection this port's tag-based model doesn't have a 1:1 analog for. This port's
   `OreDictStack` (tag-keyed) could substitute for the `dictTranslation` half, but the implement wave
   should treat this as a design decision to make explicitly, not transliterate blindly — it wasn't
   independently re-derived in this pass.
5. **`tinyWasteTranslation` fallback-derivation mechanic** (CE lines 668-724): when a `_tiny` waste item
   has no direct recipe entry, CE derives one on the fly from the full-size waste item's recipe
   (`fluidProduced/900*100`, same outputs). This is a nice, faithful mechanic worth preserving once the
   underlying `_tiny` items exist — but it depends entirely on item #6 above (`nuclear_waste_*_tiny`)
   being registered first, so it's blocked transitively, not directly.
6. **`WeightedRandom`/`WeightedRandomObject` parity** (`src/main/java/com/hbm/util/WeightedRandom.java`,
   `WeightedRandomObject.java`) was not independently re-verified byte-for-byte against CE's version in
   this pass — it's already load-bearing in the 6 shipped `SILEXRecipes` entries and the `MixerRecipes`
   competing-array mechanism without apparent issue, so risk is judged low, but flagged as unconfirmed.
7. **FluidCombustionRecipes' 14 compat-string entries** are almost certainly dead/no-op even in CE
   itself (case-mismatch against `Fluids.fromName`'s uppercase constant names) — recommend skipping
   them entirely rather than spending effort transcribing dead code, unless a future task independently
   confirms one of those lowercase strings does resolve to a real registered fluid name in this port
   (not checked exhaustively here, only spot-checked `"ethanol"` vs. `ETHANOL`, which is exactly this
   case-mismatch pattern).

## Files read (for citation)

- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/SILEXRecipes.java` (821 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/MixerRecipes.java` (220 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/HeatRecipes.java` (129 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/FluidCombustionRecipes.java` (92 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntitySILEX.java` (partial, targeted greps)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityHeatBoiler.java` (targeted greps)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/oil/TileEntityMachineGasFlare.java` (targeted greps)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUIMachineGasFlare.java` (targeted greps)
- `upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java` (targeted greps for `dust`/`dust_tiny`/`undefined`/`fluid_icon`)
- `src/main/java/com/hbm/inventory/recipes/chem/SILEXRecipes.java` (115 lines, full)
- `src/main/java/com/hbm/inventory/recipes/MixerRecipes.java` (218 lines, full)
- `src/main/java/com/hbm/blockentity/machine/chem/SilexBlockEntity.java` (291 lines, full)
- `src/main/java/com/hbm/blockentity/machine/MachineMixerBlockEntity.java` (targeted greps)
- `src/main/java/com/hbm/blockentity/machine/SolarBoilerBlockEntity.java` (targeted greps)
- `src/main/java/com/hbm/inventory/RecipesCommon.java` (~340 lines, full `AStack`/`ComparableStack`/`NbtComparableStack` section)
- `src/main/java/com/hbm/inventory/material/Mats.java` (327 lines, full)
- `src/main/java/com/hbm/items/machine/ItemRBMKPellet.java` (73 lines, full)
- `src/main/java/com/hbm/items/machine/MachineItems.java` (targeted greps: rbmk_pellet_*, fluid_icon)
- `src/main/java/com/hbm/items/machine/ItemFluidIcon.java` (targeted read, ~85 lines)
- `src/main/java/com/hbm/items/machine/ItemFELCrystal.java` (targeted greps: EnumWavelengths)
- `src/main/java/com/hbm/items/IngotNuggetItems.java`, `BilletPowderItems.java`, `PlateCrystalWasteItems.java`, `SpecialItems.java` (targeted greps + a 70-line full read of `SpecialItems.java`'s waste-item section)
- `src/main/java/com/hbm/inventory/fluid/Fluids.java` (targeted greps, ~30 fluid-name spot checks)
- `src/main/java/com/hbm/datagen/ModRecipeProvider.java` (targeted greps: `BILLET_SETS`/table-loop convention)
- `docs/phase6/PARITY_REPORT.md`, `docs/phase6/recipe_graph_audit.md` (read in full, both ~380-420 lines)
