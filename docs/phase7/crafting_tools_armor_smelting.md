# Research report: crafting-tools-armor-smelting (Phase 7 assignment)

Scope: CE `com.hbm.crafting.ToolRecipes` (223 lines), `ArmorRecipes` (216 lines), `SmeltingRecipes`
(173 lines). Cross-referenced against this port's `com.hbm.datagen.ModRecipeProvider` (695 lines,
already committed) and a large number of item-registry grep spot-checks (documented inline per finding
below — every "exists"/"missing" claim in this report was individually grepped against
`src/main/java/com/hbm`, not assumed from CE's field names).

---

## Scope confirmed

| File | Lines | CE package | Structure |
|---|---:|---|---|
| `upstream/hbm-ce/src/main/java/com/hbm/crafting/ToolRecipes.java` | 223 | `com.hbm.crafting` | One `public static void register()` method: a flat sequence of `CraftingManager.addRecipeAuto(...)`/`addShapelessAuto(...)` calls, punctuated by two tiny private static helper methods (`addTool`, and `addSword/addPickaxe/addAxe/addShovel/addHoe` thin wrappers around it) invoked in a short unrolled 5-material list, plus one `for(EnumCartBase base : EnumCartBase.values())` loop (3 iterations) and one `if(GeneralConfig.enableLBSM && ...) {…} else {…}` branch (10 material-tier tool sets, mutually exclusive at runtime). **128 total recipe call sites**: 40 via the `addSword`/`addPickaxe`/`addAxe`/`addShovel`/`addHoe` wrapper family (25 unconditional "regular tools" + 15 inside the `if` branch) + 88 direct `CraftingManager.addRecipeAuto`/`addShapelessAuto` calls (78 unconditional + 10 inside the `else` branch). Not a big data-table loop like `MineralRecipes` — mostly individually hand-written recipes, grouped under section comments (`//Regular tools`, `//Super pickaxes`, `//Utility`, `//Carts`, `//Configged`, etc.).
| `upstream/hbm-ce/src/main/java/com/hbm/crafting/ArmorRecipes.java` | 216 | `com.hbm.crafting` | Same shape: one `register()` method, 120 direct `CraftingManager.addRecipeAuto`/`addShapelessAuto` calls + 23 via the `addHelmet`/`addChest`/`addLegs`/`addBoots` wrapper family (15 unconditional + 8 inside an `if(GeneralConfig.enableLBSM && enableLBSMSimpleArmorRecipes)`/`else` branch). **143 total recipe call sites.** No loops at all (unlike `MineralRecipes`) — every armor piece is its own hand-written shaped/shapeless call.
| `upstream/hbm-ce/src/main/java/com/hbm/crafting/SmeltingRecipes.java` | 173 | `com.hbm.crafting` | One `public static void AddSmeltingRec()` method: **132 active `GameRegistry.addSmelting(...)` call sites** (134 raw text matches minus 2 commented-out dead lines at 117-118), of which 131 are single-recipe calls and 1 is a `for(int i=0;i<10;i++)` loop generating 10 recipes (`ingot_steel_dusted` damage 0-9 self-smelts into its "heated" state) → **141 total individual furnace recipes**. Organized under section comments matching CE's own material families (metal ores→ingots, meteor ore, gneiss ore, briquette→coke, powder→ingot ×41, arc-electrode→ingot, misc singles, gravel/sand/waste/basalt, mineral crystals→base resource, `ItemHot.heatUp` self-smelts, dusted-ingot loop).

All three files were read in full (not sampled).

---

## Already covered by this port

**`ModRecipeProvider.java` (695 lines, already committed) covers a real, verified slice of `ToolRecipes`
and `ArmorRecipes` — cross-checked line-by-line against the CE source above, not assumed from its own
javadoc's summary numbers.**

### ToolRecipes overlap: 29 of 128 CE call sites (≈23%)

`ModRecipeProvider.toolRecipes()` (lines 150-215) covers exactly:
- **20 of the 25 "regular tools" recipes** (ToolRecipes.java:32-56): its `TOOL_MATERIALS` table has
  `{steel, titanium, cobalt, desh}` × `{sword, pickaxe, axe, shovel, hoe}`. CE's 5th material, **CMB**,
  is dropped — confirmed still correct: `ingot_cmb`/`ingot_cmbsteel` do not exist anywhere in this
  port (0 grep hits), even though the 5 CMB tool result items (`cmb_sword`/`_pickaxe`/`_axe`/`_shovel`/
  `_hoe`) themselves are real, registered items with no recipe reaching them.
- **`dwarven_pickaxe`** (ToolRecipes.java:65) — 1:1.
- **4 super pickaxe/axe recipes** (ToolRecipes.java:68-69/75-76): `bismuth_pickaxe`, `volcanic_pickaxe`,
  `bismuth_axe`, `volcanic_axe`.
- **4 chlorophyte-tool recipes** (ToolRecipes.java:70-71/77-78, both alternate-precursor variants each):
  `chlorophyte_pickaxe`×2, `chlorophyte_axe`×2.

Total: 20+1+4+4 = **29**, matching this port's own javadoc claim of "25 tool-family outputs" at the
**result-item** level (29 recipe call sites collapse to 25 distinct output items because the 4
chlorophyte recipes are 2 alternate recipes each for only 2 distinct results) — confirmed consistent,
not a discrepancy.

**Genuinely missed, not excluded on purpose:** `mese_pickaxe`/`mese_axe` (ToolRecipes.java:72/79) are
**not** in `ModRecipeProvider` and are **not** mentioned anywhere in its javadoc's exclusion list — this
looks like an oversight rather than a deliberate scope cut (both result items are real, registered items
in this port; see the item-dependency table below for why they're still blocked today anyway).

### ArmorRecipes overlap: 21 of 143 CE call sites (≈15%)

`ModRecipeProvider.armorRecipes()` (lines 497-654) covers exactly: `euphemium_helmet/_legs/_boots` (3),
`mask_of_infamy` (1), `gas_mask`/`_m65`/`_olde`/`_mono` (4), `dieselsuit_boots` (1),
`envsuit_plate/_legs/_boots` (3), `bismuth_helmet`/`_boots` (2), `dns_plate/_legs/_boots` (3),
`ajro_helmet/_plate/_legs/_boots` (4). 3+1+4+1+3+2+3+4 = **21**, exactly matching this port's own
javadoc's "21 armor-family outputs" claim (verified 1:1 against CE line numbers, not just taken on
faith).

### The real remaining gap (what this task adds)

- **ToolRecipes: 99 of 128 CE call sites are not yet in this port** (128 − 29).
- **ArmorRecipes: 122 of 143 CE call sites are not yet in this port** (143 − 21).
- **SmeltingRecipes: 132 of 132 CE call sites (141 of 141 recipes) are not yet in this port at all** —
  confirmed by repo-wide grep: zero `minecraft:smelting`-type JSON recipes reference any of this file's
  ingredient/result ids, and no Java class in this port's `com.hbm.datagen`/`com.hbm.inventory.recipes`
  tree reproduces any of `SmeltingRecipes.java`'s content. This is a **completely untouched category**,
  not a partial slice like the other two.

`ModRecipeProvider`'s own javadoc (lines 36-107) already explains *why* the covered slice is so small —
two structural gaps confirmed **still accurate today** by this task's own independent re-grepping (not
just re-quoted): (1) this port has no basic armor tiers at all for steel/titanium/CMB/robes/cobalt/
security/dnt/zirconium (`steel_helmet`, `titanium_plate` etc. — 0 real registrations, only the
javadoc's own self-referential mention of those strings), and (2) the circuit-component item family
(`EnumCircuitType` exists only as a bare enum in `ItemEnums.java`; **zero** `"circuit` item registration
string anywhere) plus `motor`, `motor_desh`, `tank_steel`, `thruster_small`, `cladding_lead`, `watch`,
`ring_starmetal` are all still **confirmed absent** (0 grep hits each, re-checked fresh in this task, not
assumed carried over from the earlier report). **Nothing has changed here since `ModRecipeProvider` was
written** — this is the single biggest reason so much of the remaining `ArmorRecipes`/`ToolRecipes` gap
is still blocked, not newly discovered content.

**One correction to `ModRecipeProvider`'s own reasoning, found while researching this task (worth fixing
when this area is next touched):** its comment at lines 525-531 says `rag_damp`/`rag_piss` "are not
separate items, only `ItemRag` *state names* on the single `rag` item." Reading this port's actual
`com/hbm/items/special/ItemRag.java` (68 lines) shows this is subtly wrong: `ItemRag`'s constructor
takes two **companion item ids** (`dampName`/`pissName`) and looks them up via
`BuiltInRegistries.ITEM.getOptional(...)` at runtime, explicitly documented in that class's own javadoc
as items "not owned by this area... no-op gracefully until those items exist." So `rag_damp`/`rag_piss`
(and `mask_damp`/`mask_piss`) are meant to be **real, independently-registered items that simply haven't
been registered by any area yet** — a genuine open item-registration gap, not a metadata/state-flattening
non-issue as the existing comment implies. This affects 2 `SmeltingRecipes` entries (rag_damp→rag,
rag_piss→rag) — see the catalog below, corrected to BLOCKED rather than the "ready" a surface reading of
that comment would suggest.

---

## Full recipe/entry catalog

All three files are small enough by entry count (128 / 143 / 141, all under the ~150 threshold) to
catalog fully rather than sample. To avoid duplicating CE source already quoted verbatim above, the two
partially-covered files are cataloged as **the remaining, not-yet-ported gap only** (per this task's
"Already covered" section); `SmeltingRecipes` — untouched — is cataloged **in full**.

### SmeltingRecipes.java — full catalog (132 call sites → 141 recipes)

Legend: **R** = ready to port today (every ingredient + output already a real item in this port).
**B** = blocked (names the missing item). **X** = needs redesign (items exist, but under a different id
shape than CE's single wildcard call).

| # | CE section (source lines) | Input → Output (×qty), XP | Status | Note |
|---|---|---|---|---|
| 1 | Misc (22) | `glyphid_meat` → `glyphid_meat_grilled`, 1.0 | R | |
| 2-18 | Metal ores (24-40) | `ore_thorium`→`ingot_th232`(3.0), `ore_uranium`→`ingot_uranium`(6.0), `ore_uranium_scorched`→same(6.0), `ore_nether_uranium`→same(12.0), `ore_nether_uranium_scorched`→same(12.0), `ore_nether_plutonium`→`ingot_plutonium`(24.0), `ore_titanium`→`ingot_titanium`(3.0), `ore_copper`→`ingot_copper`(2.5), `ore_tungsten`→`ingot_tungsten`(6.0), `ore_nether_tungsten`→same(12.0), `ore_lead`→`ingot_lead`(3.0), `ore_beryllium`→`ingot_beryllium`(2.0), `ore_schrabidium`→`ingot_schrabidium`(128.0), `ore_nether_schrabidium`→same(256.0), `ore_cobalt`→`ingot_cobalt`(2.0), `ore_nether_cobalt`→same(2.0) | 16×R | all 16 ore blocks + ingot outputs confirmed registered |
| 19 | Metal ores (34) | `ore_aluminium` → `chunk_ore`[CRYOLITE], 2.5 | B | `chunk_ore` item not registered anywhere (confirmed — matches `CentrifugeRecipes`/`CrystallizerRecipes`'s own "not-yet-ported" note) |
| 20-24 | Meteor ore (42-46) | `block_meteor_ore_iron`→`minecraft:iron_ingot`×16(10.0), `block_meteor_ore_copper`→`ingot_copper`×16(10.0), `block_meteor_ore_aluminium`→`chunk_ore`[CRYOLITE]×16(10.0), `block_meteor_ore_rareearth`→`chunk_ore`[RARE]×16(10.0), `block_meteor_ore_cobalt`→`ingot_cobalt`×4(10.0) | 3×R, 2×B | CE's `ore_meteor`+`EnumMeteorType` maps to this port's per-variant `block_meteor_ore_<type>` blocks (all 5 registered — `GenericBlocks.java:391`); the 2 blocked entries are `chunk_ore`-blocked, same as #19 |
| 25-31 | Gneiss ore (48-54) | `ore_gneiss_iron`→iron_ingot(5.0), `_gold`→gold_ingot(5.0), `_uranium`→ingot_uranium(12.0), `_uranium_scorched`→same(12.0), `_copper`→ingot_copper(5.0), `_lithium`→`lithium`(10.0), `_schrabidium`→ingot_schrabidium(256.0) | 7×R | all 7 ore blocks confirmed registered |
| 32-33 | Australium (56-57) | `ore_australium`→`nugget_australium`(2.5), `powder_australium`→`ingot_australium`(5.0) | 2×R | |
| 34-36 | Briquette→coke (59-61) | `briquette`[COAL]→`coke`[COAL](1.0), `briquette`[LIGNITE]→`coke`[LIGNITE](1.0), `briquette`[WOOD]→charcoal(1.0) | 3×B | neither `briquette` nor `coke` registered anywhere in this port |
| 37-77 | Powder→ingot (63-102) | 41 recipes, uniform shape `powder_X → ingot_X` (or vanilla `iron_ingot`/`gold_ingot`/`lithium` for a few): lead, neptunium, polonium, schrabidium(5.0xp), schrabidate(5.0), euphemium(10.0), aluminium, beryllium, copper, gold, iron, titanium, cobalt, tungsten, uranium, thorium, plutonium, combine_steel, magnetized_tungsten, red_copper, steel, lithium, dura_steel, polymer, bakelite, lanthanium, actinium, boron, desh, dineutronium(5.0), asbestos, zirconium, tcalloy, au198, sr90, ra226, tantalium, niobium, bismuth, calcium, cadmium — all at 1.0 XP unless noted | 41×R | **every one of the 41 `powder_*`/`ingot_*` pairs is a confirmed real registered item pair** — this is the single largest ready-now block in the file |
| 78 | Misc (104) | `ball_resin` → `ingot_biorubber`, 0.1 | B | `ball_resin` not registered (`ingot_biorubber` exists) |
| 79-82 | Arc electrode (106-109) | `arc_electrode_burnt`[GRAPHITE/LANTHANIUM/DESH/SATURNITE] → `ingot_graphite`/`ingot_lanthanium`/`ingot_desh`/`ingot_saturnite`, 3.0 each | 4×R* | *this port flattens CE's single 4-metadata field into `arc_electrode_burnt_graphite`/`_lanthanium`/`_desh`/`_saturnite` (`MachineItems.java:100-108`) — use the per-variant id, not the bare `arc_electrode_burnt` name |
| 83-87 | Misc singles (111-115) | `combine_scrap`→`ingot_combine_steel`(1.0), `rag_damp`→`rag`(0.1), `rag_piss`→`rag`(0.1), `plant_flower_tobacco`→`plant_item_tobacco`(0.1), `ball_fireclay`→`ingot_firebrick`(0.1) | 5×B | `combine_scrap`, `rag_damp`, `rag_piss` (see correction above), `plant_item` (any variant), `ball_fireclay` all unregistered |
| 88-98 | Gravel/sand/waste/basalt (119-129) | `minecraft:gravel`→cobblestone(0.0), `gravel_obsidian`→obsidian(0.0), `gravel_diamond`→diamond(3.0), `sand_uranium`→`glass_uranium`(0.25), `sand_polonium`→`glass_polonium`(0.75), `waste_trinitite`→`glass_trinitite`(0.25), `waste_trinitite_red`→same(0.25), `sand_boron`→`glass_boron`(0.25), `sand_lead`→`glass_lead`(0.25), `ash_digamma`→`glass_ash`(10.0), `basalt`→`basalt_smooth`(0.1) | 3×R, 8×B | ready: vanilla gravel, `waste_trinitite`, `waste_trinitite_red` (both real blocks, `PlantBlocks.java:260/263`); blocked: `gravel_obsidian`, `gravel_diamond`, `sand_uranium`, `sand_polonium`, `sand_boron`, `sand_lead`, `ash_digamma`, `basalt`/`basalt_smooth` — none of these 8 decorative/structural blocks exist in this port yet |
| 99 | Schraranium (131) | `ingot_schraranium` → `nugget_schrabidium`, 2.0 | R | |
| 100 | Crystals (133) | `lodestone` → `crystal_iron`, 5.0 | B | `lodestone` not registered |
| 101-126 | Crystals (134-159) | 26 recipes, uniform shape `crystal_X → base resource ×N`: iron→iron_ingot×2, gold→gold_ingot×2, redstone→redstone×6, diamond→diamond×2, uranium→ingot_uranium×2, thorium→ingot_th232×2, plutonium→ingot_plutonium×2, titanium→ingot_titanium×2, sulfur→`sulfur`×6, niter→`niter`×6, copper→ingot_copper×2, tungsten→ingot_tungsten×2, aluminium→ingot_aluminium×2, fluorite→`fluorite`×6, beryllium→ingot_beryllium×2, lead→ingot_lead×2, schraranium→nugget_schrabidium×2, schrabidium→ingot_schrabidium×2, rare→powder_desh_mix, phosphorus→powder_fire×6, lithium→lithium×2, cobalt→ingot_cobalt×2, starmetal→ingot_starmetal×2, trixite→ingot_plutonium×4, cinnabar→`cinnabar`×4, osmiridium→ingot_osmiridium — all 2.0 XP | 22×R, 4×B | blocked: `crystal_sulfur→sulfur`, `crystal_niter→niter`, `crystal_fluorite→fluorite`, `crystal_cinnabar→cinnabar` — the **plain resource items** `sulfur`/`niter`/`fluorite`/`cinnabar` are not registered (only `crystal_sulfur` etc. themselves, and the *ore blocks* `ore_sulfur`/`ore_niter`/`ore_fluorite`/`ore_cinnabar`, exist — confirmed by grep, this is a real distinct gap, not a naming collision) |
| 127-129 | Heat-up self-smelts (161-165) | `ingot_chainsteel`→`ItemHot.heatUp(self)`(0.0), `ingot_meteorite`→same(0.0), `ingot_meteorite_forged`→same(0.0) | 3×R | this port's `ItemHot` class (`items/special/ItemHot.java`) already implements the heat mechanic via a `SpecialItemComponents.HEAT` data component — see Recommended shape below |
| 130-131 | Heat-up self-smelts (164-165) | `blade_meteorite`→heatUp(self)(0.0), `meteorite_sword`→heatUp(`meteorite_sword_seared`)(0.0) | 2×B | neither `blade_meteorite`/`meteorite_sword`/`meteorite_sword_seared` exists (confirmed by `items/weapon/WeaponMeleeItems.java`'s own javadoc: "the 11 `meteorite_sword`* tiers - need `ItemSwordMeteorite`... [not built]") |
| 132 | Plastic scrap (167) | `scrap_plastic`[wildcard] → `ingot_polymer`, 0.1 | X | this port has no single `scrap_plastic` item — it registers 23 per-type `plastic_scrap_<type>` items instead (`SpecialItems.java:246`); needs expansion into up to 23 discrete smelting recipes, all →`ingot_polymer` |
| 133 (loop) | Dusted-ingot loop (169-170) | `ingot_steel_dusted_0`..`_9` → `heatUp(self)` ×10, 1.0 each | R | this port already registers exactly this 10-variant series (`IngotNuggetItems.java:240`, `registerIngotSeries("ingot_steel_dusted", 10)`) — a direct table-driven loop port |

**Totals: 141 individual recipes → 113 ready to port today as-is (≈80%), 27 blocked on a missing item
(each named above), 1 needs restructuring into a per-variant family (not blocked, just reshaped).**

### ToolRecipes.java — remaining gap (99 of 128 call sites not yet ported)

| CE section (lines) | Items | Ready? | Blocker |
|---|---|---|---|
| Regular tools — CMB (32-56, 5 of 25) | `cmb_sword/_pickaxe/_axe/_shovel/_hoe` | B | `ingot_cmb`/`ingot_cmbsteel` not registered (result items exist) |
| Super pickaxe/axe — mese (72, 79) | `mese_pickaxe`, `mese_axe` | B | `shimmer_handle` not registered (both result items exist; genuinely missed by `ModRecipeProvider`, not excluded on purpose — see "Already covered") |
| Misc tools (58-64) | `elec_sword/_pickaxe/_axe/_shovel`, `centri_stick`, `smashing_hammer`, `meteorite_sword` | B (all 7) | `elec_*` need `ModItems.motor` (pickaxe/axe/shovel) or a battery-pack stack; `centri_stick` needs `centrifuge_element` (unregistered); `smashing_hammer` needs `block_steel`/`block_tungsten` (unregistered, same gap `ModRecipeProvider`'s own javadoc already names for the compression grid); `meteorite_sword` needs `blade_meteorite` (unregistered) |
| Chainsaw (82) | `chainsaw` | B | needs `piston_selenium`(exists) but also `ModBlocks.chain`/`canister_empty`(exists) — blocked overall by the tool's own fueled-tool empty-state mechanic not independently re-verified; low priority |
| Misc (85-87) | `crowbar`, `bottle_opener`, vanilla `saddle` override | 2×R, 1×B | `crowbar` (steel ingot only) and `bottle_opener` (`plate_steel` + vanilla planks) are **ready now**; the saddle override needs `plant_item`[ROPE], unregistered |
| Matches (90-91) | `matchstick` ×2 alt recipes | B | needs a sulfur-dust/red-phosphorus-dust item; no `dust_sulfur`/`dust_phosphorus`-equivalent registered (result item exists) |
| Gavels (94-95) | `wood_gavel`, `lead_gavel` | 1×R, 1×B | `wood_gavel` (vanilla slab+log+stick tags) is **ready now**; `lead_gavel` needs `pellet_buckshot`, unregistered |
| Misc weapons (98-99) | `pipe_lead`, `ullapool_caber` | 1×B, 1×R | `pipe_lead` **item itself** is unregistered; `ullapool_caber` (`plate_iron` + vanilla TNT + stick) is **ready now** |
| Utility bucket (102-148, ~33 recipes) | `rangefinder`, `designator`(+`_range`/`_manual`/`_arty_range`), `linker`, `oil_detector`, `turret_chip`, `survey_scanner`, `geiger_counter`, `dosimeter`, `ModBlocks.geiger`, `digamma_diagnostic`, `pollution_detector`, `ore_density_scanner`, `defuser`, `coltan_tool`, `reacher`, `sat_designator`, `sat_relay`, `settings_tool`, `pipette`/`_boron`/`_laboratory`, `siphon`, `boat_rubber`, `mirror_tool`, `rbmk_tool`, `power_net_tool`, `analysis_tool`, `toolbox`, `screwdriver`/`_desh`, `hand_drill`/`_desh`, `chemistry_set`/`_boron`, `blowtorch`, `acetylene_torch`, `boltgun`, `rebar_placer` | ≈8×R, rest B | **Notable finding**: most of these *result items already exist* in this port (only `linker`, `reacher`, `siphon`, `boat_rubber`, `acetylene_torch`, `ducttape`, `pipe_lead`, `part_generic` are genuinely unregistered) — but nearly every recipe needs a `circuit` component (still fully unregistered) as an ingredient. **Confirmed ready without a circuit**: `mirror_tool` (aluminium+iron ingot), `rbmk_tool` (lead+iron ingot), `coltan_tool` (copper ingot + `crystal_cinnabar` + vanilla compass), `screwdriver` (steel+iron ingot), `toolbox` (`plate_copper`+iron ingot), `chemistry_set_boron` (glass_boron block + steel + cobalt ingot), `power_net_tool` (`MINGRADE` wire tag [`MAT_MINGRADE` has `WIRE` in its autogen list] + redstone + iron ingot + `battery_lead` [confirmed real, `ItemBatteryPack.java:101`]), `hand_drill` (`ingot_dura_steel`, a hand-registered item distinct from `MAT_DURA`'s autogen shapes, which has no plain INGOT + stick) — **8 recipes ready now**. `designator_range`/`_arty_range` (shapeless, combining already-existing-but-uncraftable `rangefinder`/`designator` + a plastic ingot) also satisfy the narrow "every ingredient is a real item" bar even though their own inputs have no path yet — flagged separately as a judgment call for the implement wave. |
| Bobmazon (151) | `bobmazon` | R | vanilla book + gold nugget + string + `KEY_BLUE`(dye) — result item exists, all ingredients vanilla |
| Carts (154-164) | cart-base × cart-type matrix (empty/destroyer/powder/semtex + painted variant) | X | **structural mismatch**: CE's combinatorial `EnumCartBase`×`EnumMinecart` matrix (up to 15 items) doesn't exist in this port — `items/tool/CartItems.java` instead registers 5 flat ids (`cart_ntm_ore`, `_powder`, `_semtex`, `_crate`, `_destroyer`), not base-tiered. Needs a redesigned recipe set matching this port's actual item shape, not a 1:1 port. |
| Configged block (167-194, 25 call sites) | `cobalt_decorated_*`, `starmetal_*`, `schrabidium_*` (both `if`/`else` branches) | B (all) | same root cause `ModRecipeProvider`'s javadoc already documents for `ArmorRecipes`' equivalent block: `starmetal_*`/`schrabidium_*` result items don't exist, `ring_starmetal`/`block_schrabidium` don't exist; correctly out of scope either way since `GeneralConfig.enableLBSM` (confirmed to exist, `GeneralConfig.java:489`) defaults false in CE |

**Net: of the 99 remaining ToolRecipes call sites, ≈13 are ready to port today with zero new item work
(crowbar, bottle_opener, ullapool_caber, wood_gavel, bobmazon, mirror_tool, rbmk_tool, coltan_tool,
screwdriver, toolbox, chemistry_set_boron, power_net_tool, hand_drill), ≈2 more (designator_range/
_arty_range) are a judgment call, and the remaining ≈84 are blocked on already-known missing item
families (circuit, motor, block_steel/block_tungsten, shimmer_handle, ingot_cmb, plant_item, and a
handful of one-off unregistered tool items) or need a structural redesign (carts).**

### ArmorRecipes.java — remaining gap (122 of 143 call sites not yet ported)

| CE section (lines) | Items | Ready? | Blocker |
|---|---|---|---|
| Armor mod table (28) | `machine_armor_table` | B | item unregistered; also needs `block_steel`/`block_tungsten` |
| Basic armor tiers (31-59, ~21 recipes) | steel/titanium/cmb/robes/cobalt/security/dnt/zirconium helmet+chest+legs+boots | B (all) | **root item family doesn't exist** — 0 real hits for `steel_helmet`/`titanium_plate`/etc. anywhere (re-confirmed fresh, matches `ModRecipeProvider`'s own already-documented finding) |
| Power armor (62-106, ~33 recipes) | `t51_*`, `ajr_*`(base, `ajro_*` derivative already covered), `bj_*`, `hev_*`, `fau_*`, `dns_helmet`(rest covered), `rpa_*`, `steamsuit_*`, `dieselsuit_helmet/_plate/_legs`(boots covered), `envsuit_helmet`(rest covered) | B (all) | every one needs `motor`/`motor_desh`, `tank_steel`, a `circuit` grade, or `ring_starmetal`/`plate_armor_lunar`-adjacent items still unregistered — same confirmed-still-missing family as ToolRecipes |
| **Hazmat/asbestos suits (127-142, 20 recipes)** | `hazmat_{helmet,plate,legs,boots}`, `_red`, `_grey`, `asbestos_{helmet,plate,legs,boots}` | **R (all 20)** | **new finding, not in `ModRecipeProvider`.** `hazmat_cloth`/`_red`/`_grey` and `asbestos_cloth` all confirmed real registered items; ingredients are the vanilla `c:glass_panes` tag (`ModRecipeProvider` already defines this as `GLASS_PANES` — directly reusable) + `plate_iron` (already used elsewhere in the same class) + `plate_gold` (real, `PlateCrystalWasteItems.java:149`, for `asbestos_helmet`'s `"plateGold"` ore-dict string) |
| **Hazmat PAA (143-146, 4 recipes)** | `hazmat_paa_{helmet,plate,legs,boots}` | **R (all 4)** | **new finding.** `plate_paa` confirmed real (`PlateCrystalWasteItems.java:153`); same glass-pane/iron-plate ingredients as above |
| PAA reflector armor (147-149) | `paa_plate`, `paa_legs`, `paa_boots` | B | result items themselves unregistered, and `OreDictManager.getReflector()`'s 1.21 equivalent was not found under any name |
| Liquidator suit (152-155) | `liquidator_{helmet,plate,legs,boots}` | B | `cladding_lead` unregistered (rubber ingot ingredient is fine, already used elsewhere) |
| Masks (158-166) | `goggles`, `ashglasses`, `mask_rag`, `mask_piss` | B (all 4) | `goggles`/`ashglasses` result items unregistered; `mask_rag`/`mask_piss` blocked by the `rag_damp`/`mask_damp`-family gap documented in "Already covered" above (this is a **correction**, not confirmation, of `ModRecipeProvider`'s existing comment on this exact pair) |
| Capes (169-171) | `cape_radiation`, `cape_gasmask`, `cape_schrabidium` | B (all 3) | none of the 3 result items registered (ingredients would otherwise be fine — `nuclear_waste` and `gas_mask` both exist real for the first two) |
| Bismuth (remaining 2 of 4, 110-111) | `bismuth_plate`, `bismuth_legs` | B | need `ring_starmetal`, unregistered (helmet/boots already covered by `ModRecipeProvider`) |
| Euphemium (remaining 1 of 4, 116) | `euphemium_plate` | B | needs `watch`, unregistered (helmet/legs/boots already covered) |
| Jetpacks (121-124) | `jetpack_fly/_break/_vector/_boost` | B (all 4) | need `circuit`, `tank_steel`, `thruster_small` — all unregistered |
| Configged block (174-192, 16 call sites) | `starmetal_*`/`schrabidium_*` armor (both branches) | B (all) | same root cause as ToolRecipes' configged block — result items/`cobalt_*` precursor family unregistered |

**Net: of the 122 remaining ArmorRecipes call sites, 24 are ready to port today with zero new item work
(the full hazmat/asbestos/hazmat-PAA family — a clean, self-contained, immediately portable 24-recipe
slice this port hasn't touched), and the remaining 98 are blocked on the same already-documented missing
item families (base armor tiers, circuit/motor/tank_steel/thruster_small/cladding_lead/watch/
ring_starmetal) or on individually-unregistered result items (goggles, ashglasses, capes,
machine_armor_table, paa_plate family).**

---

## Item/registry dependency check

Summary of every distinct blocking item family found across all three files (grep-verified against
`src/main/java/com/hbm`, one check per name unless noted):

**Confirmed missing (blocks a real chunk of recipes each):**
- `circuit` (any `EnumCircuitType` grade) — 0 hits; blocks the majority of the ToolRecipes utility
  bucket and nearly all uncovered ArmorRecipes power-armor entries.
- `motor`, `motor_desh`, `tank_steel`, `thruster_small`, `cladding_lead`, `watch`, `ring_starmetal` — all
  0 hits.
- `chunk_ore` (any variant) — 0 hits; blocks 3 SmeltingRecipes entries.
- `block_steel`, `block_tungsten` — 0 hits (already known from `ModRecipeProvider`'s own javadoc,
  re-confirmed); blocks `smashing_hammer`, `machine_armor_table`.
- `briquette`, `coke` — 0 hits each; blocks 3 SmeltingRecipes entries.
- `sulfur`, `niter`, `fluorite`, `cinnabar` (the **plain resource items**, distinct from
  `crystal_sulfur`/etc. and from the `ore_sulfur`/etc. world-gen ore blocks, both of which *do* exist) —
  0 hits; blocks 4 SmeltingRecipes crystal-decomposition entries.
- `rag_damp`, `rag_piss`, `mask_damp`, `mask_piss` — 0 hits (see the `ItemRag.java` correction above);
  blocks 2 SmeltingRecipes + 2 ArmorRecipes entries.
- `plant_item` (any `EnumPlantType` variant, e.g. ROPE/TOBACCO) — 0 hits, even though the corresponding
  block half (`plant_flower_tobacco`) exists; blocks the `saddle` tool override + 1 SmeltingRecipes
  entry.
- One-off missing items: `ingot_cmb`/`ingot_cmbsteel`, `shimmer_handle`, `centrifuge_element`,
  `blade_meteorite`/`meteorite_sword`/`meteorite_sword_seared`, `dust_sulfur`/`dust_phosphorus`-
  equivalent, `pellet_buckshot`, `pipe_lead`, `linker`, `reacher`, `siphon`, `boat_rubber`,
  `acetylene_torch`, `ducttape`, `part_generic`, `ball_resin`, `ball_fireclay`, `combine_scrap`,
  `lodestone`, `gravel_obsidian`, `gravel_diamond`, `sand_uranium`, `sand_polonium`, `sand_boron`,
  `sand_lead`, `ash_digamma`, `basalt`/`basalt_smooth`, `goggles`, `ashglasses`,
  `cape_radiation`/`_gasmask`/`_schrabidium`, `paa_plate`/`_legs`/`_boots`, `OreDictManager.getReflector()`
  equivalent, `machine_armor_table`.

**Confirmed present (the "ready to port now" foundation):**
- All 41 `powder_*`/`ingot_*` pairs, all 30 ore-block→ingot pairs (regular + gneiss + meteor, minus the
  `chunk_ore`-gated 3), all 27 `crystal_*` minus the 5 blocked, `hazmat_cloth`/`_red`/`_grey`,
  `asbestos_cloth`, `plate_paa`, `plate_gold`, `plate_copper`, `plate_iron`, `plate_steel`,
  `ingot_dura_steel`, `battery_lead`, the vanilla `c:glass_panes` tag (already defined in
  `ModRecipeProvider` as `GLASS_PANES`), `waste_trinitite`/`_red`, `ingot_steel_dusted_0`..`_9`,
  `arc_electrode_burnt_<4 types>`, and the ≈13 standalone tool items named above (`mirror_tool`,
  `rbmk_tool`, `coltan_tool`, `screwdriver`, `toolbox`, `chemistry_set_boron`, `power_net_tool`,
  `hand_drill`, `crowbar`, `bottle_opener`, `ullapool_caber`, `wood_gavel`, `bobmazon`).

**Ready to port now (both sides confirmed):** the entire SmeltingRecipes 113-recipe ready set (see
catalog table); the ArmorRecipes hazmat/asbestos/hazmat-PAA 24-recipe slice; the ~13 ToolRecipes
standalone-item recipes listed above.

**Blocked:** everything else, cause named per-entry in the catalogs above — overwhelmingly the same
5-6 root item families (`circuit`, `motor*`, `tank_steel`, base armor tiers, `chunk_ore`,
`block_steel`/`block_tungsten`) recurring across dozens of individual recipes, plus a long tail of
one-off unregistered items each blocking exactly one recipe.

---

## Recommended 1.21.1 implementation shape

**All three files map to plain vanilla-compatible recipe types — none of this content needs a custom
`RecipeType`/`RecipeSerializer`.**

1. **ToolRecipes and ArmorRecipes remaining gap → `ShapedRecipeBuilder`/`ShapelessRecipeBuilder` JSON**,
   generated the exact same way `ModRecipeProvider` already does (same `RecipeOutput`/`RecipeCategory`/
   `item(String)`-resolve-by-id pattern, same `MaterialShapes.X.commonTag(Mats.Y)`/direct-`item(...)`
   ingredient convention). No recipe in the remaining gap needs NBT/component-predicate matching except
   the `rag`/`mask_rag` "wet-state" pair, which is blocked on missing items anyway and so is moot until
   those are registered — at that point it would need a component-predicate `Ingredient` (1.20.5+
   vanilla JSON supports this natively via a `minecraft:components`-shaped ingredient), still not a
   custom `RecipeSerializer`.
   - The **13-entry ToolRecipes ready set** and **24-entry ArmorRecipes hazmat/asbestos ready set** can
     be added as two new private methods in `ModRecipeProvider` (`toolRecipesGapReady`/
     `armorRecipesHazmat` or similar), following its existing per-recipe helper-method style
     (`sword`/`pickaxe`/etc. for ToolRecipes; a straightforward inline shaped-recipe-per-item for the
     20-piece hazmat family, since all 20 share only 2 distinct patterns — a natural candidate for a
     small `String[][] {name, cloth id}` table + one shared helper method, matching this port's own
     `BILLET_SETS`/`ONE_TO_NINE_PAIRS` table-driven-loop convention).
   - The `EnumCartBase`×`EnumMinecart` combinatorial mismatch (ToolRecipes carts) needs a redesigned,
     smaller table matching this port's actual 5-item `cart_ntm_*` family — not a mechanical 1:1 port.

2. **SmeltingRecipes → plain vanilla `minecraft:smelting` JSON via `SimpleCookingRecipeBuilder.smelting(...)`**,
   the single easiest category in this whole research task to close, exactly as this task's own prompt
   anticipated. Recommended as a new `smeltingRecipes(RecipeOutput)` method on `ModRecipeProvider` (or a
   sibling `ModSmeltingRecipeProvider` if the team prefers a separate file), reusing the same
   `item(String)` id-resolve helper.
   - **The `ItemHot.heatUp` "self-smelt" recipes (5 of the 141, or 15 once the ×10 dusted-ingot loop is
     counted) are still plain vanilla recipes, not a custom mechanic** — since 1.20.5, a vanilla recipe
     JSON's `result` block accepts a `components` override (e.g.
     `{"id": "hbm:ingot_chainsteel", "components": {"hbm:heat": 100}}`), which maps directly onto this
     port's already-ported `SpecialItemComponents.HEAT` data component (`items/special/ItemHot.java`).
     No new Java is needed for this sub-family at all.
   - **The 132 → 141 metal-ore/powder/crystal family is the highest-value, lowest-effort win in this
     entire research task**: 113 of 141 recipes (≈80%) are ready today, uniform in shape (`Ingredient →
     ItemStack(qty), float experience`), and can be generated from a small table-driven loop exactly like
     `ModRecipeProvider`'s `BILLET_SETS` convention — e.g. a `{"ore_lead", "ingot_lead", "3.0"}`-shaped
     `String[][]`/record array feeding one shared `oreSmelt(RecipeOutput, String ore, String ingot, float xp)`
     helper, with the ~15 non-uniform entries (crystal→base-resource, heat-ups, dusted loop) handled by
     a small number of additional explicit calls or a second smaller table.
   - The `scrap_plastic`→`ingot_polymer` entry should be expanded into up to 23 discrete recipes (one per
     `plastic_scrap_<type>` id in `SpecialItems.java`), each a trivial one-line smelting recipe — a good
     candidate for its own tiny loop once the exact 23-member type list is confirmed.

---

## Open questions / risks

1. **`ANY_PLASTIC`'s exact 1.21 equivalent was not pinned down in this pass.** No `MAT_PLASTIC` exists in
   `Mats.java`; `ModRecipeProvider` already sidesteps this for rubber by using the concrete item
   `ingot_rubber` directly rather than a tag. The same is likely the right move for plastic
   (`ingot_polymer`, confirmed real) but this task did not verify which of CE's ~6 plastic-family
   ore-dict entries (`ANY_PLASTIC` could mean polymer, bakelite, or a wildcard tag spanning both) each
   blocked recipe actually needs — flag for the implement wave to resolve per-recipe rather than
   guessing one blanket substitution.
2. **`MINGRADE`, `DURA`, and a few other CE `OreDictManager` two/three-letter material shortcuts weren't
   exhaustively cross-checked against `Mats.java`'s real constant names** beyond the ones this task
   specifically needed (`MAT_MINGRADE`, `MAT_DURA` — both checked and resolved). Any further utility-tool
   recipe an implement wave tries to port should re-verify its specific material shortcuts the same way
   `ModRecipeProvider`'s own `item(String)` helper already enforces for direct item ids (that helper only
   catches unresolvable *items*, not unresolvable *tags* — a bad `commonTag(...)` call currently fails
   silently as an empty, always-uncraftable recipe rather than a build-time error, worth flagging to
   whoever writes the implement-wave code).
3. **`ItemArcElectrode.EnumElectrodeType`'s exact 4 member names (`GRAPHITE`/`LANTHANIUM`/`DESH`/
   `SATURNITE`) were inferred from CE's `SmeltingRecipes.java` usage and this port's `MachineItems.java`
   loop reusing "the same enum," not independently opened and read in this pass** — low risk (both files
   clearly share the same type), but worth a 30-second confirmation before wiring the 4 recipes.
4. **The `laser_crystal_bismuth` item (used by CE's `bismuth_plate` recipe, itself blocked on
   `ring_starmetal` regardless) was only grep-confirmed to exist (1 hit), not opened/verified** — doesn't
   change any conclusion in this report since that recipe is blocked either way, noted for completeness.
5. **`GeneralConfig.enableLBSM`/`enableLBSMSimpleToolRecipes`/`enableLBSMSimpleArmorRecipes`'s *default
   values* in this port were not independently re-verified** — this task confirmed the methods/fields
   exist (`GeneralConfig.java:489` etc.) but relied on `ModRecipeProvider`'s own citation
   ("`GeneralConfig.java:107,274`" in CE) for the "defaults to false" claim. If a future config-porting
   pass changes this default, the entire ~41-call-site "Configged" block in both ToolRecipes and
   ArmorRecipes would need re-evaluating (currently correctly treated as unreachable either way, since
   its target items don't exist regardless of which branch is active).
6. **`GLASS_PANES`'s real NeoForge tag name (`c:glass_panes`) was not verified against a live jar in this
   sandbox** — same caveat `ModRecipeProvider`'s own comment already states for its existing use; this
   report's hazmat-family recommendation inherits that same unverified assumption rather than introducing
   a new one.
7. **`chainsaw` (ToolRecipes) was left unresolved as "low priority, not independently confirmed"** —
   its own empty-tool mechanic (`ItemToolAbilityFueled.getEmptyTool`) wasn't traced into this port's
   equivalent class in this pass; flagged rather than guessed.
8. The **SmeltingRecipes XP/cook-time values were transcribed directly from CE's `float` arguments** and
   assumed to map 1:1 onto vanilla `SimpleCookingRecipeBuilder`'s `experience`/`cookingTime` parameters
   (CE's `GameRegistry.addSmelting(input, output, xp)` has no explicit cook-time argument — it always
   used vanilla's default 200 ticks — so the recommended JSON should likewise omit a custom cooking time
   unless a future pass finds evidence CE overrode it elsewhere).
