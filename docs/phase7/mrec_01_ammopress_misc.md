# Research report — mrec-01-ammopress-misc (AmmoPress / PyroOven / Refinery / Magic machine recipes)

## Scope confirmed

Files read in full (CE = `upstream/hbm-ce`, port = this repo):

| File | Lines (CE) | Structure |
|---|---:|---|
| `com/hbm/inventory/recipes/AmmoPressRecipes.java` | 1242 | Single `registerDefaults()` method, **flat list of 89 literal `recipes.add(new AmmoPressRecipe(...))` calls** — no loop, no table. Each call is `new AmmoPressRecipe(output, AStack[9])`, positional (comment: "Input array describes slots from left to right, top to bottom"). Plus `SerializableRecipe`-style JSON read/write boilerplate (not relevant to porting the data) and a static `getRecipes()` that flattens the 9-slot array to a filtered `AStack[]` for the JEI/serialization view only. |
| `com/hbm/inventory/recipes/PyroOvenRecipes.java` | 228 | `registerDefaults()` = 26 one-line `registerSFAuto(FLUID)` calls + 1 three-arg `registerSFAuto(BALEFIRE, ...)` call (solid-fuel family, 27 recipes total, each auto-computing an mB quantity from the fluid's heat trait) **+ one `for(BedrockOreType : VALUES)` loop with 5 literal `recipes.add()` calls per iteration** (bedrock-ore roasting, 6 types × 5 = 30 recipes) **+ 14 further literal `recipes.add()` calls** (syngas/heavyoil/coalgas/refgas/hydrogen reaction chemistry). **71 total recipes.** |
| `com/hbm/inventory/recipes/RefineryRecipes.java` | 137 | Not a `SerializableRecipe`/JEI-file class at all — a plain `LinkedHashMap<FluidType, Tuple.Quintet<...>>` (`refinery`, 4 `.put()` calls) + `Map<FluidType, Tuple.Quartet<...>>` (`vacuum`, 2 `.put()` calls), populated once by `registerRefinery()`. **6 total entries** (4 refinery + 2 vacuum). |
| `com/hbm/inventory/recipes/MagicRecipes.java` | 100 | Flat list, **8 literal `recipes.add(new MagicRecipe(output, AStack...))` calls** inside `register()`. Matching (`MagicRecipe.matches`) is a custom order-sensitive comparison over up to 4 **compacted** (empty slots skipped) `ComparableStack`s built from an `InventoryCrafting` — used by `ContainerBook` (CE's held-item "Book of Shadows"/Lemegeton GUI), not a machine block. |

Line counts match the assignment prompt exactly (1242/228/136≈137/99≈100), confirming file identity.

## Already covered by this port

**Only `RefineryRecipes.java` has prior port coverage** (`src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java`, 153 lines) — grep-confirmed the other three (`AmmoPressRecipes`, `PyroOvenRecipes`, `MagicRecipes`) have **zero port-side file or reference** anywhere under `src/main/java/com/hbm` (the only two hits for those class names are two doc-comment mentions in `IngotNuggetItems.java` citing CE usage, not a port).

### RefineryRecipes — precise diff (mandatory per assignment)

The port's `RefineryRecipes.java` is a **line-for-line data port of all 6 CE entries**, already verified field-by-field:

| Entry | CE (upstream) | Port | Match |
|---|---|---|---|
| `refinery.put(HOTOIL, ...)` | heavy/naph/light/petro fractions (50/25/15/10) + `ModItems.sulfur` byproduct | Same 4 fractions (`OIL_FRAC_*` constants, identical values) + `PlateCrystalWasteItems.CRYSTAL_SULFUR` substituted for the byproduct | **Fractions identical.** Byproduct is a **substitution**, not the real CE item (see gap below). |
| `refinery.put(HOTCRACKOIL, ...)` | naph/light/aroma/unsat (40/30/15/15) + `oil_tar`/`EnumTarType.CRACK` | Same 4 fractions (`CRACK_FRAC_*`) + `ItemStack.EMPTY` (TODO-commented) | Fractions identical; byproduct **dropped to EMPTY**, not substituted. |
| `refinery.put(HOTOIL_DS, ...)` | heavy/naph/light/unsat (30/35/20/15) + `oil_tar`/`PARAFFIN` | Same 4 fractions (`OILDS_FRAC_*`) + `ItemStack.EMPTY` | Fractions identical; byproduct dropped. |
| `refinery.put(HOTCRACKOIL_DS, ...)` | naph/light/aroma/unsat (35/35/15/15) + `oil_tar`/`PARAFFIN` | Same 4 fractions (`CRACKDS_FRAC_*`) + `ItemStack.EMPTY` | Fractions identical; byproduct dropped. |
| `vacuum.put(OIL, ...)` | heavy/reform/light/sour (40/25/20/15) | Same 4 fractions (`VAC_FRAC_*`) | **Identical, no byproduct in either.** |
| `vacuum.put(OIL_DS, ...)` | heavy/reform/light/sour, but 4th is `REFORMGAS` not `SOURGAS` | Same, 4th is `REFORMGAS` | **Identical.** |

**The real remaining gap is narrow and precisely two missing items**, both already flagged by the port class's own javadoc/TODO comments (`src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java:39-47`, `92-113`):
1. `ModItems.sulfur` (CE `ModItems.java:1130`, plain `ItemBase`) — not registered in this port (grep-confirmed zero hits for a standalone `sulfur` item anywhere under `items/`). The port currently substitutes `PlateCrystalWasteItems.CRYSTAL_SULFUR` for the `HOTOIL` byproduct, which is a documented stand-in, not the same item.
2. `ModItems.oil_tar` (CE `ModItems.java:1325`, `ItemEnumMulti<EnumTarType>`, 6 variants: `CRUDE, CRACK, COAL, WOOD, WAX, PARAFFIN`) — the **enum is already ported 1:1** (`items/ItemEnums.java:18-27`, identical member list/order to CE's `ItemEnums.java:13-22`), but no `oil_tar_*` items exist yet. Per this port's DataComponents convention this becomes 6 separate item ids (`oil_tar_crude`, `oil_tar_crack`, ... `oil_tar_paraffin`); only `oil_tar_crack` and `oil_tar_paraffin` are actually needed by these 3 recipes' byproducts (3 call sites currently `ItemStack.EMPTY`, all TODO-commented at `RefineryRecipes.java:99,106,113`).

**Not a recipe-porting task — a 2-item registration task** (`sulfur`, and the `oil_tar` 6-way split via `MaterialShapes`/`IngotNuggetItems`-style hand registration since it doesn't fit the `Mats.setAutogen` shape system). Once those exist, this port's `RefineryRecipes.java` needs only 4 one-line edits (swap `ItemStack.EMPTY`/`CRYSTAL_SULFUR` for the real items) — **no new recipe logic, no machine work**. The machine itself is already fully built: `blockentity/machine/oil/MachineRefineryBlockEntity.java`, `blocks/machine/MachineRefineryBlock.java`, `inventory/container/machine/oil/MachineRefineryMenu.java`, `inventory/gui/machine/oil/MachineRefineryScreen.java`, and `compat/jei/category/RefineryCategory.java` (JEI) all exist and already consume `RefineryRecipes.getAllRefinery()`. **Not ported at all** (also explicitly noted by the port class's own javadoc, and not part of this gap): CE's `getRefineryRecipe()`/`getVacuumRecipe()` JEI-table builders — superseded by the port's own `RefineryCategory` + `getAllRefinery()`, so this is a non-issue, not a gap.

## Full recipe/entry catalog OR representative pattern

### AmmoPressRecipes (89 entries — full catalog, CE is a flat list with no generating loop)

Every recipe's 9-slot `AStack[]` input is populated in only a small, consistent set of positions across the whole file: **pos0** ("coat", usually empty), **pos1** ("core"), **pos4** ("agent"/propellant-or-casing), **pos7** ("casing"/propellant) — positions 2,3,5,6,8 are always `null`. A few outliers use only pos1+pos4 (nukes) or only pos4 (coilgun ammo). All ×N quantities below use CE's own `.copy(n)` multipliers; "—" means that position is `null`.

**M357 (casing family: `casing_small`/`casing_small_steel`)**

| CE id (`EnumAmmo`) | Qty | pos0 (coat) | pos1 (core) | pos4 (agent) | pos7 (casing) |
|---|---:|---|---|---|---|
| M357_BP | 16 | — | lead ×2 | gunpowder (vanilla) | casing_small |
| M357_SP | 8 | — | lead | smokeless powder | casing_small |
| M357_FMJ | 8 | — | steel | smokeless powder | casing_small |
| M357_JHP | 8 | plastic | copper | smokeless powder | casing_small |
| M357_AP | 8 | — | weaponsteel | smokeless powder ×2 | casing_small_steel |
| M357_EXPRESS | 8 | — | steel | smokeless powder ×3 | casing_small |

**M44 (identical shape to M357, different quantities: 12/6/6/6/6/6)** — BP, SP, FMJ, JHP, AP, EXPRESS, same ingredient pattern as M357 row-for-row.

**P22 (qty 24, no EXPRESS variant, 4 entries)** — SP, FMJ, JHP, AP, same pattern as M357/M44 (SP/FMJ/JHP → `casing_small`; AP → `casing_small_steel`).

**P9 (qty 12, 4 entries)** — SP, FMJ, JHP, AP — same pattern as P22.

**P45 (qty 8, 5 entries — adds a DU variant)** — SP, FMJ, JHP, AP (same pattern) + **P45_DU**: pos1 = uranium, pos4 = smokeless ×2, pos7 = casing_small_steel.

**R556 (qty 16, 4 entries, doubled ingredient counts vs pistol calibers)** — SP: core lead×2/agent smokeless×2/casing small×2; FMJ: steel×2/smokeless×2/small×2; JHP: coat plastic, core copper×2/smokeless×2/small×2; AP: weaponsteel×2/smokeless×4/small_steel×2.

**R762 (qty 12, 6 entries, adds DU + HE)** — SP/FMJ/JHP/AP same doubled pattern as R556; **DU**: uranium×2/smokeless×4/small_steel×2; **HE**: coat he, core ferrouranium/smokeless×4/small_steel×2.

**BMG50 (qty 12, `casing_large`/`casing_large_steel`, 6 entries)** — SP: lead×2/smokeless×3/large; FMJ: steel×2/smokeless×3/large; JHP: coat plastic, copper×2/smokeless×3/large; AP: weaponsteel×2/smokeless×6/large_steel; DU: uranium×2/smokeless×6/large_steel; HE: coat he, ferrouranium/smokeless×6/large_steel.

**G12 shotgun family (qty 6, `casing_shotshell`/`casing_buckshot`/`casing_buckshot_advanced`, 9 entries)**

| id | pos1 | pos4 | pos7 |
|---|---|---|---|
| G12_BP | lead nugget ×6 | gunpowder | casing_shotshell |
| G12_BP_MAGNUM | lead nugget ×8 | gunpowder | casing_shotshell |
| G12_BP_SLUG | lead | gunpowder | casing_shotshell |
| G12 | lead nugget ×6 | smokeless | casing_buckshot |
| G12_SLUG | lead | smokeless | casing_buckshot |
| G12_FLECHETTE | lead bolt ×12 | smokeless | casing_buckshot |
| G12_MAGNUM | lead nugget ×8 | smokeless | casing_buckshot_advanced |
| G12_EXPLOSIVE | high explosive | smokeless | casing_buckshot_advanced |
| G12_PHOSPHORUS | white phosphorus | smokeless | casing_buckshot_advanced |

**G10 (qty 4, `casing_buckshot_advanced` always, 5 entries)** — G10: nugget×8/smokeless×2/adv; G10_SHRAPNEL: coat plastic, nugget×8/smokeless×2/adv; G10_DU: uranium/smokeless×2/adv; G10_SLUG: lead/smokeless×2/adv; G10_EXPLOSIVE: coat he, ferrouranium/smokeless×2/adv.

**G26_FLARE (qty 4, 1 entry)** — pos1 = red phosphorus dust, pos4 = smokeless, pos7 = casing_large.

**G40 grenade-launcher (qty 4, `casing_large` always, 5 entries)** — HE: coat —, dynamite/smokeless/large; HEAT: coat copper plate, high-explosive/smokeless/large; DEMO: —, he×2/smokeless/large; INC: coat diesel(fluid), dynamite/smokeless/large; PHOSPHORUS: coat white-phosphorus, he/smokeless/large.

**ROCKET family (qty 2, 5 tokens ×2 interchangeable-propellant variants = 10 entries)** — pos4 holds `casing_large` (consumed as an ingredient, not produced), pos7 holds *either* smokeless×3 *or* `rocket_fuel` (two separate recipes per token, same output):

| id | pos1 (core) |
|---|---|
| ROCKET_HE ×2 | dynamite |
| ROCKET_HEAT ×2 | coat copper plate, core high-explosive |
| ROCKET_DEMO ×2 | high-explosive ×2 |
| ROCKET_INC ×2 | coat diesel(fluid), core dynamite |
| ROCKET_PHOSPHORUS ×2 | coat white-phosphorus, core high-explosive |

**FLAME family (qty 1, 4 entries)** — pos1 = steel plate, pos7 = steel plate, pos4 = the fuel: FLAME_DIESEL→diesel(fluid), FLAME_NAPALM→`canister_napalm`, FLAME_GAS→gas(fluid), FLAME_BALEFIRE→balefire(fluid).

**CAPACITOR family (qty 4, `plastic` on both pos1/pos7, 3 entries)** — CAPACITOR: pos4 silicon billet×4; CAPACITOR_OVERCHARGE: silicon billet×6; CAPACITOR_IR: niobium ingot.

**TAU_URANIUM (qty 16, 1 entry)** — pos1 = lead plate, pos4 = uranium ingot, pos7 = lead plate.

**COIL family (qty 4, pos4-only, 2 entries)** — COIL_TUNGSTEN: tungsten ingot; COIL_FERROURANIUM: ferrouranium ingot.

**NUKE family (qty 1, pos1+pos4 only except TOTS/HIVE, 6 entries)** — STANDARD: Pu239 nugget/`assembly_nuke`; DEMO: Pu239 nugget×2/`assembly_nuke`; HIGH: Pu239 nugget×4/`assembly_nuke`; TOTS: Pu239 nugget×2/`ball_tatb`×2/steel plate×4(pos7); HIVE: high-explosive×8/casing_large_steel×2/steel plate×4(pos7); BALEFIRE: `egg_balefire_shard`/`assembly_nuke`.

**CT family (qty 16/4, 2 entries)** — CT_HOOK: steel/steel pipe/smokeless; CT_MORTAR: he×4/steel pipe/smokeless.

**Generating-loop note**: none exists — the whole file is a literal 89-call list, so an implement-wave agent cannot table-drive this the way `ModRecipeProvider`'s `BILLET_SETS` loops do; it must be transcribed entry-by-entry (or table-driven off the compact table above, which already captures 100% of the source data).

### PyroOvenRecipes (71 entries — 41 catalogued individually, 30 are one precise loop)

**Solid-fuel family (27 entries, no loop — 26 one-arg + 1 three-arg literal calls)**: `registerSFAuto(fluid)` auto-computes an integer mB quantity of `solid_fuel` from `fluid.getTrait(FT_Flammable.class).getHeatEnergy()` at 0.5 bonus efficiency (rounded down per the tiering in `registerSFAuto`'s body), for: `SMEAR, HEATINGOIL, HEATINGOIL_VACUUM, RECLAIMED, PETROIL, NAPHTHA, NAPHTHA_CRACK, DIESEL, DIESEL_REFORM, DIESEL_CRACK, DIESEL_CRACK_REFORM, LIGHTOIL, LIGHTOIL_CRACK, LIGHTOIL_VACUUM, KEROSENE, KEROSENE_REFORM, SOURGAS, REFORMGAS, SYNGAS, PETROLEUM, LPG, BIOFUEL, AROMATICS, UNSATURATEDS, REFORMATE, XYLENE` (all → `solid_fuel`) + `BALEFIRE` → hardcoded 24,000,000 TU worth of `solid_fuel_bf` (a distinct item, not `solid_fuel`).

**Bedrock-ore roasting (30 entries = exact loop, `for(BedrockOreType type : VALUES)`, 6 types × 5 grade-pairs)**: every iteration emits 5 identical-shape recipes — duration 10, input = 1 ore item at a given `BedrockOreGrade`, output = 50 mB `VITRIOL` fluid + the same ore at the paired "roasted" grade:
`BASE→BASE_ROASTED`, `PRIMARY→PRIMARY_ROASTED`, `SULFURIC_BYPRODUCT→SULFURIC_ROASTED`, `SOLVENT_BYPRODUCT→SOLVENT_ROASTED`, `RAD_BYPRODUCT→RAD_ROASTED`. Types: `LIGHT_METAL, HEAVY_METAL, RARE_EARTH, ACTINIDE, NON_METAL, CRYSTALLINE`.

**Misc reaction chemistry (14 literal entries)**:

| Input | Output | Duration |
|---|---|---|
| 500mB STEAM + coal gem | 1000mB SYNGAS | 100 |
| 500mB STEAM + coal dust | 1000mB SYNGAS | 100 |
| 2000mB SYNGAS + tungsten dust | 1000mB SPENTSTEAM + `ingot_tungsten_carbide` | 300 |
| 250mB STEAM + coke gem | 1000mB SYNGAS | 100 |
| biomass ×4 | 1000mB SYNGAS + vanilla coal (damage 1 = charcoal) | 100 |
| any-tar ×4 | 250mB HYDROGEN + 1000mB CARBONDIOXIDE + `powder_ash`/SOOT | 40 |
| 500mB HYDROGEN + coal gem | 1000mB HEAVYOIL | 100 |
| 500mB HYDROGEN + coal dust | 1000mB HEAVYOIL | 100 |
| 250mB HYDROGEN + coke gem | 1000mB HEAVYOIL | 100 |
| 500mB HEAVYOIL + coal gem | 1000mB COALGAS | 50 |
| 500mB HEAVYOIL + coal dust | 1000mB COALGAS | 50 |
| 500mB HEAVYOIL + coke gem | 1000mB COALGAS | 50 |
| 4000mB GAS_COKER | 100mB REFORMGAS | 60 |
| 12000mB GAS | 8000mB HYDROGEN + 1 `ingot_graphite` | 60 |

### MagicRecipes (8 entries — full catalog, small file)

| # | Output | Ingredients (order matters — compacted, non-empty GUI slots only) |
|---|---|---|
| 1 | `ingot_u238m2` | `ingot_u238m2`(_elements variant, dmg1), `ingot_u238m2`(_arsenic, dmg2), `ingot_u238m2`(_vault, dmg3) |
| 2 | `rod_of_discord` | vanilla `ender_pearl`, `nugget_euphemium`, vanilla `blaze_rod` |
| 3 | `balefire_and_steel` | steel ingot (tag/OreDictStack), `egg_balefire_shard` |
| 4 | `mysteryshovel` | vanilla `iron_shovel`, vanilla `bone`, `ingot_starmetal`, `ducttape` |
| 5 | `ingot_electronium` | `pellet_charged` ×2, dineutronium ingot (tag) ×2 |
| 6 | `diamond_gavel` | `gravel_diamond` (block) ×3, `lead_gavel` |
| 7 | `mese_gavel` | `shimmer_handle`, `powder_dineutronium`, `blades_desh`, `diamond_gavel` |
| 8 | `hadron_coil_mese` (block) | `hadron_coil_chlorophyte` (block), `powder_dineutronium`, `plate_desh`, gold ingot (tag) |

No loop, no generating table — CE hand-wrote all 8.

## Item/registry dependency check

### AmmoPressRecipes

**Ready (already registered, confirmed by grep):** `ingot_lead`, `nugget_lead`, `ingot_steel`, `ingot_copper`, `ingot_weaponsteel`, `ingot_u238` (uranium-238), `ingot_ferrouranium`, `ingot_niobium`, `ingot_tungsten`, `nugget_pu239`, `billet_silicon`, `plate_copper`, `plate_lead`, `plate_steel`, `lead_bolt`* (autogen — MAT_LEAD has `BOLT` in `setAutogen`, and `BOLT` is one of `MaterialItemGenerator.AUTOGEN_SHAPES`), `steel_pipe`* (autogen, same reasoning), all needed fluids (`Fluids.DIESEL`, `Fluids.GAS`, `Fluids.BALEFIRE` all exist in `inventory/fluid/Fluids.java`), vanilla `gunpowder`/`iron_shovel`/`bone`/`blaze_rod`, `egg_balefire_shard` (`NukeCasingItems.EGG_BALEFIRE_SHARD`). **All 79 ammo *output* items whose id was spot-checked** (M357/M44/P22/P9/BMG50/G10/G12/G26/G40/ROCKET/FLAME/CAPACITOR/COIL/NUKE/CT/TAU_URANIUM families) **are already registered**, each as its own discrete `DeferredItem<Item>` via a `registerAmmo("<lowercased_enum_name>", ...)` call in the matching `Gun*Items.java`/`XFactory*.java` pair (e.g. `GunPistolItems.M357_BP = registerAmmo("m357_bp", XFactory357.ITEM_M357_BP)`) — this port's item-flattening convention happens to reproduce CE's `EnumAmmo` constant names verbatim, lowercased, as separate ids. **This is the single most important finding for this file**: the ~80-item *output* family that would normally be assumed "blocked" is in fact already fully registered and simply uncraftable (0% reachable per `recipe_graph_audit.md`'s "Gun families... 166 combined items... 0 reachable" line) — porting `AmmoPressRecipes` is almost entirely a recipe-*logic* task, not an item-registration task, for the ammo side.

**Blocked:**
- **`casing` (7-variant `EnumCasingType` family: `casing_small`, `casing_large`, `casing_small_steel`, `casing_large_steel`, `casing_shotshell`, `casing_buckshot`, `casing_buckshot_advanced`)** — the enum is already ported 1:1 (`items/ItemEnums.java:122-126`), but **no item exists**, self-documented by this port's own comment (`items/weapon/sedna/content/XFactory556mm.java`: "no shared casing-item family exists yet... race against whichever other concurrent 'guns' package first defines e.g. casing_small"). This single family gates **~85 of the 89 recipes** (every one except `COIL_TUNGSTEN`/`COIL_FERROURANIUM`/two `NUKE_*` entries that don't consume a casing) — **by far the highest-leverage single item to register** for this file.
- **P45 (.45 ACP) ammo output family, 5 recipes** — self-documented as **not ported at all**, not a forward reference: `items/weapon/sedna/mods/XWeaponModManager.java`'s own javadoc: "CE: `XFactory45.p45_sp/p45_fmj/p45_jhp/p45_ap/p45_du`... genuinely missing ammo content."
- **`ANY_PLASTIC` ingot tag** (CE `DictGroup` = polymer OR bakelite) — `MAT_POLYMER`/`MAT_BAKELITE` exist in `Mats.java` but neither has `INGOT` in its own `setAutogen(...)` (only `STOCK, GRIP`), so no `polymer_ingot`/`bakelite_ingot` item exists — blocks the "coat" (pos0) ingredient on 8 recipes (JHP variants + `G10_SHRAPNEL`).
- **`P_WHITE` (white phosphorus)** — no `Mats.java` material exists for it at all (only `MAT_PHOSPHORUS` = *red* phosphorus is ported) — blocks `G12_PHOSPHORUS`, `G40_PHOSPHORUS`, `ROCKET_PHOSPHORUS`×2 (4 recipes).
- **`ANY_SMOKELESS`/`ANY_HIGHEXPLOSIVE`** — these are CE `DictFrame`s (bare ore-dict aliases), not `Mats.java` materials; the concrete smokeless-powder and high-explosive items they tag live as bespoke fields scattered across CE's weapon/ammo item classes (not in `ModItems.java`'s obvious names), and no equivalent has been identified as registered in this port. This is the **single biggest ingredient family by recipe count** — `smokeless` alone appears in ~55 of the 89 recipes — and needs a dedicated item-registry search this task did not have budget to complete exhaustively; flagged as an open question below rather than asserted either way.
- **`P_RED.dust()` (red phosphorus dust)** — `ingot_phosphorus`/`crystal_phosphorus` exist, but no `powder_phosphorus`/dust variant was found — blocks `G26_FLARE`.
- **`rocket_fuel`, `ball_dynamite`, `canister_napalm`, `assembly_nuke`, `ball_tatb`** — all confirmed **not registered**, and all already explicitly named as TODO/missing in this port's own comments (`blockentity/bomb/NukeCustomBlockEntity.java` lines 100-102, `items/bomb/NukeCasingItems.java:28`). Blocks all `G40_HE/INC`, both `ROCKET_*` variants (rocket_fuel is only one of two interchangeable propellants there, so those recipes are only *half*-blocked), `G40_HEAT`'s `he` (exists) but the whole `NUKE_*` family (6 recipes) needs `assembly_nuke` and/or `ball_tatb`.

### PyroOvenRecipes

**Ready:** all named fluids exist (`STEAM, SYNGAS, HYDROGEN, CARBONDIOXIDE, HEAVYOIL, COALGAS, REFORMGAS, GAS_COKER, GAS, VITRIOL`, plus every `registerSFAuto` fluid target — spot-checked `DIESEL`, `NAPHTHA`, `LIGHTOIL` all exist), `ingot_graphite`, `ingot_tungsten_carbide`, `solid_fuel` item, vanilla `coal`/`charcoal`. `BedrockOreType`/`BedrockOreGrade` are both ported with **identical constant names and order** to CE (`items/special/BedrockOreType.java`, `items/special/BedrockOreGrade.java` — spot-checked `BASE, BASE_ROASTED, PRIMARY, PRIMARY_ROASTED, SULFURIC_BYPRODUCT, SULFURIC_ROASTED, SOLVENT_BYPRODUCT, SOLVENT_ROASTED, RAD_BYPRODUCT, RAD_ROASTED` all present), so the bedrock-roasting loop's 30 recipes have all their item/enum dependencies satisfied.

**Blocked:**
- **`solid_fuel_bf`** (the BALEFIRE-specific solid-fuel variant item) — not found; only plain `solid_fuel` exists. Blocks 1 of the 27 solid-fuel recipes.
- **`biomass`** — confirmed absent (zero hits anywhere in `items/`). Blocks the "syngas from biomass" recipe.
- **`ANY_TAR`/`oil_tar`** (needed as an *input* here, for the "soot from tar" recipe) — same gap as `RefineryRecipes`' `oil_tar` output gap above; not registered.
- **`powder_ash`/`EnumAshType.SOOT`** — explicitly confirmed **not registered** by this port's own comments in two places (`items/BilletPowderItems.java`'s class javadoc: "powder_ash... intentionally excluded"; `entity/missile/EntityMissileStealth.java`: "TODO(ModItems.powder_ash / EnumAshType.MISC, not yet registered)"). Blocks the "soot from tar" recipe's output (same recipe as above — doubly blocked).
- **`W.dust()` (tungsten dust)** — only `ingot_tungsten` was spot-checked; a `tungsten_dust`/`powder_tungsten` item was not directly confirmed present or absent (MAT_TUNGSTEN does list `DUST` in its own `setAutogen`, but `DUST` is *not* in `MaterialItemGenerator.AUTOGEN_SHAPES`, so it would need to come from the same "different pass" as `powder_phosphorus` above — not independently verified either way; flagged as open question).

**Overall PyroOven is much closer to ready than AmmoPress** — 41 of 41 non-loop recipes and all 30 loop recipes have their *item* dependencies mostly satisfied; only ~3-4 items block ~3 of 71 recipes outright (the rest of the 71 need no new items at all, only the recipe/machine logic itself).

### RefineryRecipes

Already covered above — **2 items** (`sulfur`, `oil_tar`) fully describe the remaining gap; everything else (all 4 fraction-percentage constants, all fluid references, the machine block/BE/menu/screen/JEI category) is already done.

### MagicRecipes

**Ready:** `ingot_u238m2` (+ `_elements`/`_arsenic`/`_vault` variants), `rod_of_discord`, `nugget_euphemium`, `balefire_and_steel`, `egg_balefire_shard`, steel ingot (tag), `ingot_starmetal`, `ingot_electronium`, `ingot_dineutronium` (tag), `diamond_gavel`, `lead_gavel`, `mese_gavel`, `powder_dineutronium`, `blades_desh`, `plate_desh`, gold ingot (vanilla/tag).

**Blocked:** `ducttape` (recipe 4 — explicitly TODO'd in `entity/missile/EntityMissileTier0.java`: "wire_fine/shell/ducttape, not yet registered"), `pellet_charged` (recipe 5 — named as a known-deferred item in both `CrystallizerRecipes.java` and `MixerRecipes.java`'s own javadocs), `gravel_diamond` block (recipe 6 — confirmed absent, named 4 separate times in this port's own comments, e.g. `itempool/ItemPoolsSatellite.java:26`: "confirmed absent from this port's registry"), `shimmer_handle` (recipe 7 — zero hits anywhere, not even a TODO comment), `hadron_coil_mese`/`hadron_coil_chlorophyte` blocks (recipe 8 — zero hits anywhere).

**Net: 5 of 8 MagicRecipes are blocked by a missing ingredient/output item; 3 (#1, #2, #3) are fully ready today.**

## Recommended 1.21.1 implementation shape

- **AmmoPressRecipes → a new custom `RecipeType`/`RecipeSerializer` (JSON-datagen-backed)**, structurally similar to this port's own `AssemblerRecipe.java` (`inventory/recipes/AssemblerRecipe.java`) — a `Recipe<Input>` implementation with a `MapCodec`/`StreamCodec` pair, JSON files under `data/hbm/recipe/ammo_press/*.json`, following the exact `ModRecipeProvider`/datagen convention this port already uses. **Key divergence from `AssemblerRecipe`'s existing shape**: CE's Ammo Press matches **strictly positionally** (`TileEntityMachineAmmoPress.hasIngredients`: `recipe.input[i].matchesRecipe(inventory.getStackInSlot(i), ...)` for a fixed 9-slot grid, with the player manually picking `selectedRecipe` from a list rather than any auto-detect) — not `AssemblerRecipe`'s unordered "bag of items" multiset match. Recommend a `List<@Nullable Entry> inputs` sized exactly 9 (reusing `AssemblerRecipe.Entry`'s `(Ingredient, count)` record shape) matched index-by-index, **not** the assembler's bag-match — but this needs an explicit design call from the implement wave (see open questions) since this port's own `AssemblerRecipe` javadoc already documents *choosing* to diverge from CE's manual-select-recipe UI in favor of auto-detect for its own machine family; the same simplification (auto-detect, ignore CE's positional/manual-select mechanic entirely and treat it as a 9-item bag) is a defensible, lower-risk option consistent with precedent, at the cost of losing CE's per-slot-shape flavor (a shotgun recipe visually "looking like" a shotgun shell in the GUI).
- **PyroOvenRecipes → a bespoke hardcoded Java data class**, matching the precedent already established by this port's own `CrystallizerRecipes`/`MixerRecipes`/`RefineryRecipes` (plain `List`/`Map` populated by a `register()` method, **not** a JSON `Recipe<?>`). Reason, stated directly by this port's own `AssemblerRecipe.java` javadoc: **`FluidStack` has no `Codec`/`StreamCodec` of its own yet** — every PyroOven recipe needs fluid input and/or output (only a handful of the 71 are pure item→item), so a JSON-driven `RecipeSerializer` genuinely cannot be built today without first adding a `FluidStack` codec (a separate, cross-cutting prerequisite, likely belonging to whoever owns the fluid/recipe-infrastructure area, not this task). The bedrock-roasting 30-entry loop should be reproduced as a literal Java `for` loop over `BedrockOreType.VALUES` exactly mirroring CE's structure (5 `add()` calls per iteration) rather than transcribed by hand.
- **RefineryRecipes → no recipe-shape work needed at all.** Only item registration (`sulfur`, `oil_tar`) plus 4 one-line substitutions in the already-complete port file.
- **MagicRecipes → a bespoke hardcoded Java data class** (same precedent as PyroOven/Refinery/Crystallizer), *not* a vanilla `Recipe<CraftingInput>`: CE's own matching semantics (compacted, order-sensitive, `ComparableStack`/`OreDictStack`-only, up to 4 slots, consumed by a **held-item GUI** not a machine block) don't map onto vanilla crafting-table `Recipe` machinery at all, and 8 entries is far too small to justify inventing a new `RecipeType` for. **This one has an additional, larger prerequisite**: `ItemBookLemegeton.java` (`items/tool/ItemBookLemegeton.java`) is currently a bare tooltip-only stub with its `use()` override explicitly not implemented ("Menu-opening interaction deferred... No use() override until a MenuProvider/Screen equivalent of CE's GUILemegeton/ContainerLemegeton exists") — porting `MagicRecipes`' *data* is cheap, but it is inert without a real 4-slot `MenuProvider`/`Screen` pair for the book GUI, which is a genuinely separate, larger GUI-framework task, not a recipe-porting task.

## Open questions / risks

1. **`ANY_SMOKELESS`/`ANY_HIGHEXPLOSIVE` item identity is unresolved.** These are CE ore-dict aliases (not `Mats.java` materials), and this task could not locate their concrete backing item(s) in either CE's `ModItems.java` (searched, no match) or this port. Given `smokeless` alone gates ~55 of AmmoPress's 89 recipes, resolving this (likely requires reading CE's actual weapon/ammo-component item files, e.g. wherever CE registers "gunpowder_smokeless"/"cordite"/"ballistite"-type items and calls `.setOreDict(ANY_SMOKELESS, ...)`) should be the **first** follow-up before an implement wave starts on AmmoPress, since it is the highest-leverage single blocker after `casing`.
2. **Two-naming-convention risk for material shapes.** This port has two parallel, differently-named item-registration passes for the same conceptual "material + shape" grid: `IngotNuggetItems`/`PlateCrystalWasteItems`/`BilletPowderItems` use a CE-inherited **prefix** convention (`ingot_lead`, `plate_lead`, `billet_pu239`), while `MaterialItemGenerator`'s loop (driven by `Mats.setAutogen`) uses a **suffix** convention via `MaterialShapes.buildRegistryName` (`lead_bolt`, `steel_pipe`). A handful of materials (`MAT_REDSTONE`, `MAT_BORAX`, `MAT_SODIUM`, `MAT_STRONTIUM`, `MAT_SLAG`, `MAT_NEODYMIUM`) declare `INGOT` in their own `setAutogen(...)` *and* are separately hand-registered in `IngotNuggetItems.java` — meaning e.g. both `ingot_redstone` (if hand-registered) and `redstone_ingot` (autogen) could exist as two different ids for "an ingot of redstone." Not directly relevant to these 4 files (none of their ingredients hit this overlap), but worth flagging since any implement wave writing AmmoPress/PyroOven JSON recipes must pick the *correct* one of two possible ids per material+shape pair, not assume a single canonical id exists.
3. **AmmoPress positional-vs-bag matching is a real design decision, not a detail** (see Recommended shape above) — CE's `isItemValidForSlot` also uses position to gate which items the GUI will even accept per slot, a UX nicety this port would lose entirely under a bag-match simplification. Flagging for the implement wave to decide explicitly rather than silently picking one.
4. **`W.dust()` (tungsten dust)** and a couple of other single-shape lookups (noted inline above) were not exhaustively confirmed present/absent — this task's grep budget prioritized the ~30 highest-recipe-count families; a implement-wave agent should re-verify the handful of "not independently confirmed" items called out inline before writing JSON.
5. **CE's own `TileEntityMachineAmmoPress.java` (277 lines) and `GUIMachineAmmoPress.java` were read only for the matching-logic excerpt needed to resolve the positional-vs-bag question** — the full block entity (power draw, animation state machine, `selectedRecipe` persistence/NBT) was not read in full and is out of this recipe-focused task's scope; the implement wave building the actual Ammo Press machine block should read it directly rather than relying on this report's partial excerpt.
6. **No port-side machine block/block-entity exists for AmmoPress or PyroOven at all** (confirmed by exhaustive grep — zero files anywhere under `src/main/java/com/hbm` mention `AmmoPress` or `PyroOven`). This means both of those two files need **the machine itself built** (block, block entity, container/menu, screen) in addition to the recipe data — a materially larger scope than Refinery/Magic, where the machine (Refinery) or at least the held item (Magic's `ItemBookLemegeton`) already exists in some form.