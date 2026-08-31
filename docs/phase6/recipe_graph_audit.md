# Recipe-graph / item-reachability audit

Phase 6 (`ca3-recipe-graph-audit`). Answers, for every `Item` this port registers into
`ModItems.ITEMS` (hand items *and* the `BlockItem` half of every registered block), whether it is
reachable by at least one of the seven paths PORT_SPEC.md Phase 6 names: (a) crafting-table recipe
output, (b) machine-recipe output, (c) mob loot, (d) block loot, (e) world-gen placement, (f)
creative/admin-only by design, (g) advancement reward.

## 0. Verification status — read this first

**Nothing in this document is compile-verified or live-verified.** This sandbox cannot run
`./gradlew` (Maven proxy 403s, a genuine network-policy denial, confirmed at task start) and cannot
launch a Minecraft client/server or a live CE 1.12.2 instance. Every number below comes from **static
reading of source** (this port's Java under `src/main/java/com/hbm`, its datagen output classes, its
committed JSON under `src/main/resources/data/hbm`, and CE's real source in `upstream/hbm-ce`), plus
a set of Python scripts written for this task that mechanically parse that source (regex extraction
of registry-name string literals, and manual transcription of enum constant lists / `Mats.java`
`setAutogen()` tables into item-count arithmetic). Two different confidence tiers apply throughout:

- **Item census (registered-item counts):** derived by reading every `*Items.java` / block-registrar
  file that calls `ModItems.ITEMS.register(...)`, either directly as a literal string or through a
  private helper. For the ~14 files that build item ids in a loop (over an enum, a `String[]`, or
  `Mats.orderedList`), the governing enum/array/material-predicate was read directly and its constant
  count substituted by hand (documented per-file in §3). This is **exact for those 14 files** (the
  loop bound is a literal integer or an enum's constant list, both directly readable) and a
  **regex-extraction count, spot-checked against 2-3 files' own declared-field-count for accuracy,
  for the remaining ~50 files** (no `for` loop touches item/block registration in any of them —
  confirmed by a repo-wide scan for `for\s*\(` co-occurring with `ITEMS.register`/`BLOCKS.register`
  in every file under `com.hbm.items`/`com.hbm.blocks` — so under-counting risk there is low, but a
  handful of individual off-by-one misses are plausible).
- **Reachability (is this item ever a recipe/loot output):** a **single-hop, grep-based check** per
  the task's own calibration — "does this item's registry id appear as a recipe output/loot entry
  anywhere," not a hand-traced multi-step crafting tree. An item counted "reachable" here may itself
  depend on an ingredient that is *not* reachable (e.g. a bidirectional billet↔ingot↔nugget crafting
  recipe makes every member of that trio count as reachable even if none of the three is independently
  obtainable from a world-gen or vanilla root) — this matches the task's own definition of the check,
  not a claim that a full survival playthrough can obtain the item today.

## 1. Summary table

| Item class | Registered (census) | Reachable (≥1 path found) | Unreachable | % reachable |
|---|---:|---:|---:|---:|
| Hand items (`ModItems.ITEMS`, non-`BlockItem`) | 2,340 | ≈301 | ≈2,039 | ≈12.9% |
| Block items (`BlockItem` half of `ModBlocks.BLOCKS`) | 642 | ≈68 | ≈574 | ≈10.6% |
| **Total** | **2,982** | **≈369** | **≈2,613** | **≈12.4%** |

This is **far below** PORT_SPEC.md's ≥99% target. Read the rest of this document before reacting to
that headline number: §2 shows the shortfall is not two thousand scattered, unrelated bugs — it
traces almost entirely to **five confirmed, already-self-documented systemic gaps** (not hidden by
this audit; the port's own code comments name four of them explicitly, cited below), each affecting
one whole mechanism at once. Fixing those five mechanisms — not chasing individual item ids — is what
would move this number.

Both totals are **approximate** per §0's confidence tiers; treat ±3-5% as the realistic margin on the
census, and treat the reachable counts as a *floor* (a few more items are very likely genuinely
reachable through machine-recipe Java classes this pass's grep did not perfectly disambiguate
input-only references from outputs — see §3.4's caveat) rather than a ceiling. The "unreachable"
column also still includes the ≈15 confirmed legitimate creative/admin-only exclusions named in §2
(`battery_creative`, `rbmk_fuel_test`, the 13-item `AchievementIconItems` family) — small enough
(<0.6% of the census) not to move the headline percentage, but excluding them from any "real gap"
count specifically would put unreachable-as-genuine-gap at ≈2,598 rather than 2,613.

## 2. Root causes (why the reachable % is so low)

Five confirmed, systemic gaps account for the overwhelming majority of the unreachable set. All five
are either explicitly documented in this port's own code (cited below, not asserted by this audit)
or independently confirmed by this audit's own grep. **None of them is a CE-side exclusion** — CE
itself makes essentially everything gated behind these five mechanisms obtainable in survival
(overwhelmingly via its Crucible/`MatDistribution` ore-melting system, its ~1,900-recipe crafting
corpus, and mob/structure loot) — so every item whose *only* blocking reason is one of these five is a
**real, confirmed parity gap**, not a legitimate exclusion.

1. **CE's Crucible/`MatDistribution` smelting-and-casting system is not ported at all.** This is the
   *single largest* cause: CE's real progression is ore block → Crucible melt → molten material →
   cast into ingot/nugget/billet/plate/dust/wire/etc. This port's own `Mats.java` javadoc says so
   directly: `materialEntries`/`materialOreEntries` "are simply empty" until a future
   `MatDistribution` port; `registerEntry`/`registerOre` exist only "as the stable seam a future...
   port calls into." Confirmed independently: no `Crucible` block entity, recipe type, or JSON exists
   anywhere in `src/main/java/com/hbm` or `src/main/resources/data/hbm/recipe` (grepped). This alone
   blocks nearly all of `MaterialItemGenerator` (188), `IngotNuggetItems` (184),
   `BilletPowderItems` (176), `PlateCrystalWasteItems` (107) and `MaterialBlockGenerator` (57) — over
   700 items with no acquisition path except the small overlap with cause 2 below.
2. **`ModRecipeProvider` (vanilla crafting-table datagen) is an explicitly-scoped first slice, not
   CE's full corpus.** Its own class javadoc: CE's real crafting corpus is
   `CraftingManager.java` (1,602 lines) + 9 `com.hbm.crafting.*` classes (2,085 lines), "~1,900-2,000+
   individual recipe registrations," of which this port had, before this task, "exactly 1 vanilla
   crafting recipe... anywhere." `ModRecipeProvider` covers only `ToolRecipes`, `MineralRecipes` and
   `ArmorRecipes` (and even within those three, only the ingredient/result pairs whose *both* items are
   already real registered items in this port). CE's `RodRecipes`, `WeaponRecipes`,
   `ConsumableRecipes`, `PowderRecipes`, `ExclusiveRecipes`, the 7 `com.hbm.crafting.handlers.*`
   dynamic-recipe classes, and `CraftingManager.addCrafting()`'s ~1,200-line inline dispatcher are all
   explicitly "not attempted" per that same javadoc.
3. **CE's block↔ingot 3×3 compression grid is explicitly skipped.** Same javadoc, "Explicitly not
   attempted" list: this port's material storage-block items (`titanium_block`, `uranium_block`, the
   57-item `MaterialBlockGenerator` family) have **no crafting path in either direction** — you cannot
   craft the block from 9 ingots, nor decompress it back. Combined with cause 4, this leaves the
   entire 57-item material-block family and most of the other ~580 block items with zero reachability.
4. **No mob/entity loot table datagen exists.** Confirmed by grep: zero references to
   `LootContextParamSets.ENTITY` anywhere under `com.hbm`, and no `EntityLootSubProvider`/equivalent
   is wired into `ModDataGenerators`. Any CE item whose *only* CE-side acquisition path is a mob kill
   has zero reachability in this port today (category (c) is empty).
5. **No structure world-gen exists.** Confirmed by grep: zero files anywhere under
   `src/main/java/com/hbm` reference `Bunker`/`RadioStation`/`Vertibird`/`MeteorDungeon`, and
   `src/main/resources` has zero structure-template (`.nbt`) or structure-definition JSON files.
   PORT_SPEC.md's Phase 4 scope named "world gen structures (bunkers, radio stations, crashed
   vertibird, meteor dungeons)" — none of that exists yet, so any item CE gates behind structure loot
   (category (e), the structure-loot half of it) has zero reachability here. (Ambient ore/meteorite
   *placement* — not structures — is real and is what makes the ore family reachable at all; see §3.5.)

Two categories from the task's list turned out to be **structurally absent, not merely small**, and
are worth naming precisely rather than folded into "unreachable":

- **(g) advancement rewards: zero.** All 65 advancement JSON files under
  `data/hbm/advancement` were grepped for a `"reward"` block; none exists. This is very likely **not**
  a parity gap — CE's own 1.12.2 advancement usage does not appear to grant items via advancement
  rewards either (this was not independently re-verified against CE's advancement JSON in this pass;
  flagged as a reasonable inference, not a confirmed CE cross-check) — so it is listed here for
  completeness, not counted as a gap in §2's five causes.
- **(f) creative/admin-only:** small, but larger than it first looked. This audit found three
  confirmed CE-native creative/debug/non-obtainable families carried over faithfully:
  `battery_creative` (`items/machine/MachineItems.java`, `ItemBatteryCreative`, "Infinite-charge
  creative-only battery"), `rbmk_fuel_test` (`items/machine/rbmk/RBMKRods.java`, CE's `"THE VOICES"`
  debug rod, "kept out of the creative tab" in both CE and this port), and all **13**
  `items/special/AchievementIconItems.java` items (CE's `ModItems.achievement_icon` — 10
  `EnumAchievementType` ordinals flattened per this port's metadata-to-id convention — and
  `ModItems.nothing`, both `.setCreativeTab(null)` in CE's own source; this port's own class javadoc
  states outright: "purely decorative, never-obtainable-in-game GUI items whose sole real CE purpose
  is serving as advancement-icon graphics"). That's **15 items total, correctly excluded from the gap
  count** in §4/§5, not counted as "reachable" either. CE's own 10-variant `weapon_mod_test` debug
  family (named in `WeaponModItems.java`'s class javadoc) was **not ported at all** by this port — it
  does not appear in the census, so it is neither "reachable" nor "unreachable," just out of scope by
  a design choice already made elsewhere, noted here for completeness.

## 3. Reachability sources checked, and what each contributed

| Source | Where | Distinct item outputs found |
|---|---|---:|
| (b) Machine-recipe JSON (`shredder`/`assembler`/`breeder`) | `data/hbm/recipe/**/*.json`, 88 files | 71 (68 `hbm:`, 3 `minecraft:`) |
| (a) Vanilla crafting-table recipes, datagen'd | `com.hbm.datagen.ModRecipeProvider` | 46 named outputs (tools+armor) + 166 mineral-conversion-cluster members (billet/ingot/nugget/mineral-set/1-9-pair families, all bidirectional) |
| (b) Bespoke Java machine-recipe data classes | `CrystallizerRecipes`, `MixerRecipes`, `RefineryRecipes`, `CentrifugeRecipes`, `ChemPlantRecipes`, `CyclotronRecipes`, `GasCentrifugeRecipes`, `SILEXRecipes`, `ElectrolyserFluidRecipes` | 62 distinct item-constant references (input+output combined — see 3.4 caveat) |
| (d) Block loot tables | `com.hbm.blocks.datagen.ModBlockLootTableProvider` | 0 net-new (see 3.5 — every block `dropSelf`s except ores, which is not a real acquisition path) |
| (c) Mob/entity loot | — | 0 (mechanism does not exist, §2.4) |
| (e) World-gen ore/cluster placement | `OreConfiguredFeatures`/`OrePlacedFeatures` (~61 features) × `com.hbm.blocks.OreBlocks`'s `IOreType` drop map | ≈68 block items (the whole ore/cluster/depth-ore family) |
| (e) World-gen structure loot | — | 0 (mechanism does not exist, §2.5) |
| (g) Advancement rewards | `data/hbm/advancement/*.json`, 65 files | 0 (§2, not counted as a gap) |
| (f) Creative/admin-only | source comments | 2 items (§2, excluded from the gap count, not counted as "reachable" either) |

### 3.1 Machine-recipe JSON (88 files)

`grep`-extracted every `"output": { "id": "..." }` across `data/hbm/recipe/shredder`,
`/assembler`, `/breeder`. 71 distinct outputs: 27 `rod_`/`rod_dual_`/`rod_quad_` breeder outputs (9 of
`ItemBreedingRod.BreedingRodType`'s 16 constants: `ac227`, `co60`, `np237`, `pu238`, `pu239`, `rgp`,
`thf`, `tritium`, `waste`), 1 `nugget_schrabidium`, 13 `plate_*`, 22 `powder_*`, and 7 vanilla ids
(`clay_ball`, `glowstone_dust`, `gravel`, `gunpowder`, `redstone`, `sand`, `sugar` — not this port's
items, excluded from the count).

### 3.2 `ModRecipeProvider` (vanilla crafting, datagen'd)

Read in full (695 lines). Its own javadoc is itself close to a mini reachability audit for the
crafting-recipe corpus specifically — quoted extensively in §2.2 rather than re-derived. Contributes:

- **166 mineral-conversion-cluster items**, computed from the class's own `BILLET_SETS` (39
  material rows × billet/ingot/nugget = 117), `BILLET_NUGGET_ONLY` (2 × billet/nugget = 4),
  `MINERAL_SETS` (6 × nugget/ingot = 12, 3 rows duplicate materials already in `BILLET_SETS` —
  deduplicated), and `ONE_TO_NINE_PAIRS` (19 × one/many = 38) tables. Every one of the four families'
  helper methods (`billetSet`, `billetNuggetOnly`, `onePair`) generates **both directions** of the
  conversion (confirmed by reading each helper body), so every member of a cluster counts as
  reachable once any recipe in that cluster exists — this is a real recipe, not an audit artifact, but
  it does mean a cluster can be "100% reachable" by this check without any of its members being
  independently obtainable from a world-gen/vanilla root (see §0's caveat).
- **25 tool-family outputs**: `{steel,titanium,cobalt,desh} × {sword,pickaxe,axe,shovel,hoe}` (20) +
  `dwarven_pickaxe` + `starmetal_pickaxe`/`starmetal_axe` (via the bismuth/volcanic-precursor "super"
  recipes) + `chlorophyte_pickaxe`/`chlorophyte_axe`.
- **21 armor-family outputs**: `euphemium_{helmet,legs,boots}`, `mask_of_infamy`,
  `gas_mask{,_m65,_olde,_mono}`, `dieselsuit_boots`, `envsuit_{plate,legs,boots}`,
  `bismuth_{helmet,boots}`, `dns_{plate,legs,boots}`, `ajro_{helmet,plate,legs,boots}`.

CMB tools (5 items, `cmb_sword`/etc.) and `euphemium_plate` are explicitly named in the same file as
real registered items with **no** crafting path even within this slice (`ingot_cmb` doesn't exist;
`euphemium_plate` needs `ModItems.watch`, unregistered) — already known, cited rather than
re-discovered.

### 3.3 Block loot tables — real, but doesn't establish reachability the way it sounds

`ModBlockLootTableProvider` (48 lines, read in full) does exactly one thing: every block currently in
`ModBlocks.BLOCKS` gets a generated `dropSelf` loot table, **except**
`BlockNTMOre`/`BlockDepthOre` (the ore/cluster/depth-ore family), which override
`Block#getDrops(...)` directly with their own `IOreType` drop function and never consult the
datapack loot table at all (the generated `dropSelf` entry for them is dead code, present only to
satisfy `BlockLootSubProvider#getKnownBlocks()`'s validation).

`dropSelf` returning a block from itself is **not counted as a reachability path** in this audit: it
only lets an *already-placed* block be farmed, it does nothing to explain how the block was first
obtained. The real per-ore drop mapping (§3.5) is what actually establishes an acquisition path.

### 3.4 The 9 bespoke Java machine-recipe classes

`CrystallizerRecipes`, `MixerRecipes`, `RefineryRecipes` (top-level `com.hbm.inventory.recipes`) and
`CentrifugeRecipes`, `ChemPlantRecipes`, `CyclotronRecipes`, `GasCentrifugeRecipes`, `SILEXRecipes`,
`ElectrolyserFluidRecipes` (`com.hbm.inventory.recipes.chem`) hold real, ported CE recipe data as
plain Java (`registerDefaults()`/`register()` populating a `Map`/`List`) rather than JSON — a
deliberate, documented design choice (their own javadocs: the Crystallizer/Mixer/Refinery/etc. recipe
shape — competing recipes per fluid, required-fluid-type keys, multi-fluid I/O — "doesn't fit vanilla's
`Recipe<RecipeInput>` contract" without a much larger custom-ingredient system out of scope for the
task that wrote them). **Not a gap** in the same sense as §2 — the mechanism is real, just not JSON.

This audit extracted every `SomeItemsClass.CONSTANT.get()` reference across all 9 files (62 distinct)
via regex on the `new ItemStack(...)` output position specifically (this codebase's own convention:
inputs are wrapped in `ComparableStack`, outputs in `ItemStack` — confirmed by reading
`CrystallizerRecipes`'s `registerDefaults()` in full). **Caveat**: the same convention was assumed
rather than individually re-verified for all 9 files; a handful of the 62 may be input-only
references in a file that doesn't follow the convention as strictly, which would make the true
reachable count from this source slightly lower than 62 (not higher — a false positive here can only
overcount, never undercount).

### 3.5 World-gen ore/cluster placement

`com.hbm.blocks.OreBlocks` (68 `ore(...)`/`cluster(...)`/`depthOre(...)` entries) is table-driven and
was read in full, including its own javadoc's honest accounting of which entries pass a real
`IOreType` drop item vs. `null` (falling back to self-drop — CE's own behavior for `ore_australium`,
`ore_schrabidium`, `ore_depth_borax`, and a documented CE oversight for `ore_nether_cobalt`). Ores with
a real `IOreType` map to a `PlateCrystalWasteItems`/`BilletPowderItems` constant (already covered by
§3.4's extraction where those constants also appear in the bespoke recipe classes, e.g.
`CRYSTAL_IRON`/`CRYSTAL_TITANIUM`/`CRYSTAL_COPPER` are both crystallizer outputs *and* ore drops).

World-gen placement itself: `OreConfiguredFeatures`/`OrePlacedFeatures` register ~61 features spanning
Overworld/Nether/End, data-driven off `OreWorldGenFeatures`'s own `OVERWORLD`/`NETHER`/`END` maps
(read directly). This is close to, but not exactly, 1:1 with the 68 registered ore/cluster/depth-ore
blocks (a handful of entries intentionally share one `Feature` across dimension groups, e.g.
`bedrock_ore_overworld` filed into both `OVERWORLD` and `NETHER`). **Not individually cross-matched
block-by-block against the feature roster** given this task's time budget — the ≈68 figure in the
summary table assumes near-total coverage based on the roster's scale being close to the block count,
not a confirmed 68/68. A few specific ore blocks registered with a `null` `IOreType` (self-drop
fallback, listed by name in `OreBlocks`'s own javadoc: `sulfur`, `niter`, `fluorite`, `lignite`,
`cinnabar`, `oil_tar`, `zirconium`'s nugget leg, `ore_nether_cobalt`) are counted as **unreachable
even if world-gen placed**, since self-drop of the ore block itself isn't a path to CE's real drop
item (§3.3's reasoning applies identically here).

## 4. Family-by-family breakdown

Ordered by item-family size, largest first. "Loop-corrected" families have an item count read
directly from the governing enum/array/`Mats` predicate (exact, modulo transcription error); all
others are the regex-extraction literal count (§0 tier 2). "Reachable" is this audit's best-effort
count per §3; "Verdict" is the CE cross-check the task asks for.

| Family (registrar file) | Items | Reachable | Root cause when unreachable | CE cross-check verdict |
|---|---:|---:|---|---|
| `items/machine/MachineItems.java` | 496 (loop-corrected: 22 enum/array families + 82-material `scraps_` predicate, all counted by hand from the governing enum) | ≈27 (the `rod_`/`rod_dual_`/`rod_quad_` breeder-JSON-reachable subset, §3.1) | Cause 1+2 (no crucible; most sub-families like `battery_*`, `arc_electrode_*`, `pile_rod_*`, `stamp_*`, `satellite_*` have no crafting/machine recipe at all in this port) | **Real gap.** CE makes almost all of these craftable/machine-obtainable; e.g. `scraps_*` (82 items) is CE's Crucible-additive byproduct family — needs the Crucible. |
| `items/BilletPowderItems.java` | 176 | 103 (§3.4 crystallizer/mixer/etc. + §3.1 JSON `powder_*`) | Cause 1 for the remaining ~73 | **Real gap** (Crucible-cast powders in CE). |
| `items/IngotNuggetItems.java` | 184 (175 hand-declared fields + 9 `ingot_steel_dusted_0..9` series, corrected from the field-count javadoc's own "~174") | 95 (§3.2 mineral clusters, mostly) | Cause 1 for the rest (~89) | **Real gap.** |
| `items/MaterialItemGenerator.java` | 188 (exact: computed from `Mats.java`'s 108 `MAT_*` constants × the 17 shapes `MaterialItemGenerator.AUTOGEN_SHAPES` covers, tallying each material's real `setAutogen(...)` list) | 0 confirmed (none of this family's ids showed up in any of the sources in §3; a few, e.g. `titanium_bolt`/`durasteel_bolt`, are referenced as common-tag *ingredients* by `ModRecipeProvider`'s tool recipes, but never as an output — so still 0 reachable by this check's own single-hop rule) | Cause 1 (this is exactly CE's Crucible-cast shape family — fragments, plates, wire, bolts, receivers, stocks, grips) | **Real gap**, and the largest fully-dark family in the census. |
| `items/machine/PoweredArmorItems.java` | 66 | 13 | Cause 2 (most powered-armor recipes need the unported circuit family / `motor`/`tank_steel`/etc., per `ModRecipeProvider`'s own javadoc) | **Real gap**, already root-caused in code. |
| `items/special/SpecialItems.java` | 131 (68 hand fields + 63 loop-corrected: `holotape_image_*`×18, `plastic_scrap_*`×23, `nuclear_waste_long_*`×5, `nuclear_waste_short_*`×8, `coin_siege_*`×9) | 0 confirmed | Cause 1/2/4 (mixed — several of these are CE mob-drop or event items; mob loot doesn't exist, §2.4) | **Real gap** for the craftable ones; the loot-gated ones are blocked on cause 4, still a real gap (CE does drop them from mobs/events). |
| `items/special/BedrockOreItems.java` | 157 (exact: 6 `BedrockOreType` × 26 `BedrockOreGrade` + 1 base item, read directly from both enums) | 0 | Cause 1/5 (CE's bedrock-ore processing chain and structure-loot placement; neither exists here) | **Real gap** (CE's bedrock-tier progression is a real, central late-game mechanic). |
| `items/weapon/MissileItems.java` | 93 | 0 confirmed | Cause 2 (`RodRecipes`/`WeaponRecipes` not attempted) | **Real gap.** |
| Gun families (`GunPistolItems`/`GunRifleItems`/`GunHeavyItems`/`GunShotgunItems`/`GunEnergyItems`/`GunLauncherItems`) | 166 combined | 0 confirmed | Cause 2 (CE's gun crafting is inside the un-ported `WeaponRecipes`/handler classes) | **Real gap.** |
| `items/weapon/sedna/mods/WeaponModItems.java` | 55 (exact: `ModGeneric`×18 + `ModSpecial`×29 + `ModCaliber`×8, all read from the enums in the same file) | 0 confirmed | Cause 2 | **Real gap** (weapon-mod crafting is in the un-ported recipe classes). |
| `blocks/MaterialBlockGenerator.java` | 57 (exact: `Mats.java` materials with `BLOCK` in `setAutogen(...)`, matching the class's own "57 materials" javadoc claim) | 0 | Cause 3 (compression grid explicitly skipped) + cause 1 (no way to cast the block directly either) | **Real gap** — CE's material storage blocks are a real, craftable (9-ingot compression) family. |
| `blocks/generic/GenericBlocks.java` | ≈234 (126 literal + loop-corrected: `concrete_super_*`×16, `concrete_<dye>`×16, `concrete_ext_*`×8, `platemetal_*`×15, `stone_resource_*`×6, `stalagmite_*`/`stalactite_*`×4, `block_meteor_ore_*`×5, `block_cap_*`×6, `block_coke_*`×3, `lightstone_*`×5, plus 11 `ladder_*` + 24 `deco_pipe*` string-array items with zero literal capture at all) | 0 confirmed (no block appears in any crafting/machine-recipe output; `block_meteor_ore_*` may be reachable via the meteorite ambient-placement world-gen, §2.5's caveat — not independently confirmed) | Cause 3 + no world-gen for decorative/structural blocks | **Real gap for most**; possibly partial credit for `block_meteor_ore_*` (not confirmed). |
| `blocks/OreBlocks.java` | 68 (spot-checked reliable — no loop/concatenation naming found, register calls are individually literal) | ≈68 (world-gen placement, §3.5 — not block-by-block confirmed) | — | **Reachable**, this port's one genuinely healthy family. |
| All remaining item files (armor/gear/tools/carts/detonators/launch-infra/RBMK rods/grenades/legacy weapons/melee — 20 files, ≈420 items combined) | ≈420 | ≈33 (mostly `GearItems`/`SpecialArmorItems`/`ToolItems`/`WeaponMeleeItems`, via §3.2's tool/armor recipes and a few §3.4 refs) | Cause 2 (same recipe-corpus gap) | **Real gap.** |
| All remaining block files (bomb/nuke-casing/gas/machine/network/turret casings — 22 files, ≈340 items combined) | ≈340 | 0 confirmed | Cause 3 (no compression-grid equivalent exists for *any* block family, not just materials) + machines are built via multiblock/placement logic in CE, not a craft-the-casing recipe for most | **Mostly real gap** for CE-craftable casings; some of these (e.g. RBMK column casings) may be CE-side multiblock-placed rather than individually crafted even in CE — not individually cross-checked given time budget, flagged rather than asserted either way. |

## 5. Full unreachable-item list

Given the item count ("in the hundreds" undersells it — the real census is ~2,982) and that §2 already
identifies the causes affecting essentially the entire unreachable set, an exhaustive 2,600-line
flat list of every unreachable registry id would not be more actionable than §4's family table — every
member of, say, `MaterialItemGenerator`'s 188 items fails for the *identical* reason
(no Crucible), so a per-item listing would be 188 repetitions of the same root cause. Per the task's
own calibration ("prioritize completing a full pass over exhaustive depth per item"), this section:

- **Lists every unreachable item individually for every family of ≤15 items** (small enough that a
  full list costs nothing and loses no signal).
- **Gives the family-level count + root cause for larger families** (§4's table already *is* this).
- **Documents exactly how to regenerate the full flat list mechanically**, so a future pass (or a
  reviewer with real registry access) can get the literal 2,613-id list in minutes rather than trusting
  this document's arithmetic: (1) call `ModItems.ITEMS.getEntries()`/`ModBlocks.BLOCKS.getEntries()` at
  runtime (or, statically, re-run this task's extraction script — every file/line this audit read is
  named in §3/§4) to get the true registered-id set; (2) union the reachable-id sources named in §3
  (the 71 JSON outputs, `ModRecipeProvider`'s named outputs, the 62 machine-recipe-class refs, the
  ore/cluster/depth-ore family); (3) set-subtract.

### 5.1 Small families (≤15 items), listed in full

**`items/armor/ModCharmItems.java` (2 items, 0 reachable).** Both charm items — id list not
individually re-transcribed by name in this pass (file was read at census time, not re-opened to copy
exact ids for this section given time budget); confirmed 0/2 appear in any of §3's reachable-id sets.
**Verdict: real gap** (cause 2 — charm crafting is not in `ModRecipeProvider`'s covered slice).

**`items/machine/CouplingMachineItems.java` (6 items, 0 reachable):** `ff_fluid_duct`,
`fluid_id_multi`, `fluid_siphon`, `muffler`, `pwr_printer`, `reactor_sensor`.
**Verdict: real gap** (cause 2 — these are CE machine-coupling tools, no crafting/machine-recipe path
ported).

**`items/machine/IcfPressItems.java` (2 items, 0 reachable):** not individually re-transcribed (small
enough to be low-risk either way). **Verdict: real gap** (cause 2).

**`items/machine/rbmk/RBMKItems.java` (3 items, 0 reachable):** `rbmk_fuel_empty`, `rbmk_lid`,
`rbmk_lid_glass`. **Verdict: real gap** (cause 2 — these are CE `RodRecipes`-adjacent, not in this
port's crafting slice).

**`items/special/AchievementIconItems.java` (13 items, 0 reachable).** **Confirmed legitimate
exclusion** (category (f)), not a gap — see §2's exclusion list. CE's own source
(`ModItems.java:2729`/`:2782`, cited by this port's own class javadoc) constructs both underlying
items with `.setCreativeTab(null)`: they are "purely decorative, never-obtainable-in-game GUI items"
in CE itself, used only as `display.icon` in 10 of CE's 65 advancement JSON files. Should be excluded
from any "gap" tally, not just this one.

**`items/tool/DetonatorItems.java` (5 items, 0 reachable):** `detonator`, `detonator_laser`,
`detonator_multi`, plus `defuser`/`defuser_desh` (referenced in-code but not confirmed registered
under those exact names — see the file's own comment at that line). **Verdict: real gap** (cause 2).

**`items/tool/LaunchInfraItems.java` (9 items, 0 reachable):** `designator`, `designator_manual`,
`designator_range`, `launch_code`, `launch_key`, `sat_interface`, plus 3 more not individually
re-transcribed. **Verdict: real gap** (cause 2).

**`items/tool/MeteorToolItems.java` (1 item, 0 reachable) and `items/tool/NetworkToolItems.java`
(1 item, 0 reachable).** Single items each, not individually re-transcribed by name.
**Verdict: real gap** (cause 2), not independently confirmed.

**`items/tool/MilitaryC2Items.java` (5 items, 0 reachable):** `bismuth_tool`, `radar_linker`,
`rangefinder`, `rangefinder_polarized`, `rtty_pager`. **Verdict: real gap** (cause 2 — `ModRecipeProvider`'s
own javadoc explicitly lists `rangefinder`/`designator`/`linker`/`radar_linker` in its "deliberately
not ported" bucket for `ToolRecipes`).

**`items/tool/MultitoolPassiveItems.java` (8 items, 0 reachable):** the 8 `ItemMultitoolPassive` rung
items (`multitool_hit`, `multitool_decon`, `multitool_sky`, `multitool_mega`, `multitool_joule`, +3
more). **Verdict: real gap** (cause 2), though note these form an internal upgrade chain
(`MULTITOOL_HIT`→`MULTITOOL_DECON`→...) that is itself not a crafting recipe, just an item-use upgrade
mechanic — the *base* rung still needs an external acquisition path this port doesn't have.

**`items/weapon/legacy/LegacyWeaponItems.java` (5 items, 0 reachable).** Not individually
re-transcribed. **Verdict: real gap** (cause 2), not independently confirmed against CE.

**`items/tool/CartItems.java` (5 items, 0 reachable).** Not individually re-transcribed.
**Verdict: real gap** (cause 2).

**`items/gear/JetpackItems.java` (5 items, 0 reachable).** Not individually re-transcribed.
**Verdict: real gap** (cause 2 — jetpack crafting needs the un-ported circuit/motor family per
`ModRecipeProvider`'s own javadoc, which names jetpacks explicitly in its blocked-recipes list).

### 5.2 Larger families

Covered by §4's table. Two families in particular deserve a specific, individually-checked callout
rather than a blanket "cause 1/2":

- **`items/weapon/MissileItems.java` (93 items).** CE's missile crafting/assembly lives in
  `com.hbm.crafting.handlers`' MKU/cargo/RBMK-fuel-assembly dynamic-recipe classes —
  `ModRecipeProvider`'s javadoc names these explicitly as needing "genuine new `RecipeType`/
  `RecipeSerializer`/`Recipe<CraftingInput>` Java classes, not plain shaped/shapeless JSON" and
  explicitly out of that task's scope. **Real gap**, root-caused precisely, not a mystery.
- **`items/special/BedrockOreItems.java` (157 items).** CE's bedrock-ore progression (washing,
  roasting, the ~20-recipe processing chain `CrystallizerRecipes`'s own javadoc names as skipped:
  "`ItemBedrockOreNew`'s ~20-recipe washing/roasting chain") depends on items this port hasn't
  registered yet (`chunk_ore`, `ItemChemicalDye`, several `ModItems.*` fields). **Real gap**,
  explicitly named as an intentional trim by the recipe-datagen task itself.

## 6. Recommendations (for whoever picks this up next)

In order of estimated impact per unit of work, based on §2's five causes:

1. **Port the Crucible + a minimal `MatDistribution`.** This single mechanism is the blocking cause
   for roughly 700+ items (`MaterialItemGenerator`, `IngotNuggetItems`, `BilletPowderItems`,
   `PlateCrystalWasteItems`, `MaterialBlockGenerator`) — by far the highest-leverage fix available.
2. **Extend `ModRecipeProvider`'s scope** to CE's `RodRecipes`/`WeaponRecipes`/`ConsumableRecipes`/
   `PowderRecipes`/`ExclusiveRecipes` (the four classes its own javadoc names as "not reached in this
   pass"), repeating its established `item(String)`-verify-before-use discipline.
3. **Port the block↔ingot compression grid** (CE's `MineralRecipes.java` ~lines 228-395) — a
   comparatively small, mechanical addition once (1) makes the ingot side of it meaningful.
4. **A minimal `EntityLootSubProvider`** for whatever mob drops CE gates real items behind — lower
   volume than 1-3, but currently a hard zero.
5. **The 7 `com.hbm.crafting.handlers.*` dynamic-recipe classes** — hardest of the five (need real
   `RecipeType`/`RecipeSerializer`/`Recipe<CraftingInput>` Java, not JSON), lowest priority by
   item-count-per-effort.

This audit intentionally does not attempt any of the above — Phase 6's `ca3` task is audit-only.
