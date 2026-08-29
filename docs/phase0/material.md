# Phase 0 Research: Material System (Mats / NTMMaterial)

Area key: `material`
Scope: `com.hbm.inventory.material.{Mats, NTMMaterial, MatDistribution, MaterialShapes}` (CE, 1.12.2)

## 1. Class inventory (CE source)

| File | Purpose |
|---|---|
| `NTMMaterial.java` (129 lines) | The material definition object itself: id, ore-dict name aliases (`names[]`), which shapes it auto-generates items for, metal/nonmetal trait, smelting behavior, solid/molten colors, and smelting conversion (e.g. coal -> carbon at a ratio). One instance per real-world/fictional material (iron, titanium, schrabidium, polymer, ...). |
| `MaterialShapes.java` (107 lines) | An enum-like set of `public static final` singletons (not a real Java `enum`, a class with static instances) describing every physical form a material can take (nugget, ingot, plate, dust, wire, block, gun parts, ...). Each shape carries a base quantity in "quanta" (1 ingot = 72 quanta = 9 nuggets = 8 quanta/nugget) and the ore-dictionary prefix string(s) that name it (e.g. `"ingot"`, `"plateTriple"`). Also carries a GregTech-compat block (`registerCompatShapes`) that only runs `if (Loader.isModLoaded("gregtech"))`. |
| `Mats.java` (279 lines) | The actual catalog: ~90 `public static final NTMMaterial MAT_*` constants (vanilla-like, radioactive, base metals, alloys, "extension"/polymer materials), plus the global lookup tables (`matById`, `matByName`, `orderedList`), the `MaterialStack` value type, and the crucible-facing query API `getMaterialsFromItem` / `getSmeltingMaterialsFromItem` that reverse-maps an arbitrary `ItemStack` (by exact stack or by ore-dict prefix+material-name string) to its constituent materials. |
| `MatDistribution.java` (204 lines) | A `SerializableRecipe` (JSON-backed, `hbmCrucibleSmelting.json`) that populates `Mats.materialEntries` (exact-`ItemStack` -> materials, e.g. `Blocks.RAIL` -> 6/16 iron ingot) and `Mats.materialOreEntries` (ore-dict-name -> materials, e.g. `"oreIron"` -> 2 iron ingot + 3 titanium nugget + 1 quartz). This is the data-driven "what does melting/smelting this item yield" table used by the crucible machine. |

Total: 719 lines across 4 files, all read in full.

## 2. Key responsibilities

- **Identity**: every material has a numeric `id` (CE encodes it as "atomic number * 100 + last two digits of mass number", with reserved ranges: vanilla space 0-29, alloy space 30-99, extension space >= 20000) and one or more string names (`names[]`, sourced from `OreDictManager.DictFrame`, used both as the ore-dict tag suffix and as the `hbmmat.<name>` localization key).
- **Shape/quantity model**: `MaterialShapes` defines a shared unit ("quantum") so that all shapes across all materials are proportionally comparable (nugget = 8q, ingot = 72q = 9 nuggets, block = 9 ingots, plate = 1 ingot, cast/welded/dense variants = 3x/6x/etc.). `NTMMaterial.autogen` is the set of shapes that item-generation should actually produce for that specific material (not every material gets every shape - e.g. wood only gets STOCK/GRIP, arsenic only gets NUGGET).
- **Traits**: `MatTraits.METAL` / `NONMETAL` drive which machines (e.g. arc furnace vs. not) can process the material; set via convenience chain methods `.m()` / `.n()`.
- **Smelting/crucible behavior**: `SmeltingBehavior` (`NOT_SMELTABLE`, `VAPORIZES`, `BREAKS`, `SMELTABLE`, `ADDITIVE`) plus an optional `smeltsInto` + `convIn`/`convOut` ratio (e.g. coal -> 2 coal make 1 carbon-equivalent). This is distinct from the Minecraft furnace; it is HBM's own crucible/blast-furnace mechanic.
- **Visuals**: `solidColorLight`/`solidColorDark` (for ingot/dust texture tinting) and `moltenColor` (for fluid/lava-like rendering of the melted material) - plain ints, no NBT/GUI coupling.
- **Reverse lookup for the crucible**: `Mats.getMaterialsFromItem(ItemStack)` inspects the ore-dictionary names of an arbitrary stack, matches known shape prefixes against `matByName`, and falls back to two explicit tables (`materialEntries` by exact stack, `materialOreEntries` by ore-dict string) that `MatDistribution` populates. This is how "smelt this random modded ingot" resolves to "X ingots of copper" even for items HBM didn't register itself.
- **JSON data-driven overrides**: `MatDistribution` also serializes/deserializes the two tables to `hbmCrucibleSmelting.json` via `SerializableRecipe`, so server owners can add/replace crucible yields without recompiling.

## 3. The one CE mechanic that cannot be ported as-is: metadata-item identity

In CE, `NTMMaterial.make(Item item, int amount)` returns `new ItemStack(item, amount, this.id)` - i.e. **the material's numeric id is literally the ItemStack damage/metadata value**. A single registered `Item` (e.g. "ingot") represents dozens of different materials purely via that metadata slot. `MaterialShapes.make(NTMMaterial)` builds the parallel ore-dictionary string (`"ingot" + "Iron"`) used for OreDictionary registration and lookup.

Minecraft 1.21.1 / NeoForge has **no metadata/damage-value item variants at all** - `ItemStack` no longer carries a numeric "meta" field; item identity is the registered `Item` itself, and per-stack data lives in typed Data Components. There is also no `OreDictionary` - its modern replacement is `TagKey<Item>` (NeoForge/Fabric "Common Conventions" tags, e.g. `c:ingots/iron`).

I checked how the Neo Edition reference project (partial 1.21.1 port) handled this, since it already ported these exact 4 files:

- `neo-edition/.../material/NTMMaterial.java`, `MaterialShapes.java`, `Mats.java` exist but are **skeletons only** - the file is headed `// todo everything???`, the CE constructor is entirely commented out, and `Mats.java` has zero `MAT_*` definitions (no iron, no titanium, nothing). `MaterialShapes` shape list and quanta math were ported faithfully, but the ore-dict prefix strings were already renamed to plural/common-tag style (`"ingots"`, `"dusts"`, `"fine_wires"`, `"dense_wires"`) which is a real signal for how they intend to bridge to NeoForge Common tags.
- Neo Edition's `NTMMaterial.make()` was changed to `MetaHelper.newStack(item, amount, this.id)`, and `MetaHelper` (`com.hbm.inventory.MetaHelper.java`) is a genuine, working NeoForge 21.1 shim that emulates 1.12-style metadata using a **registered `DataComponentType<Integer>`**:

```java
// com.hbm.items.component.NtmDataComponents (Neo Edition, confirmed real API usage)
DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
    DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, NuclearTechMod.MODID);

DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> META =
    DATA_COMPONENT_TYPES.register("meta", () -> DataComponentType.<Integer>builder()
        .persistent(ExtraCodecs.NON_NEGATIVE_INT)
        .networkSynchronized(ByteBufCodecs.INT)
        .build());
```

This confirms the `DataComponentType.builder().persistent(codec).networkSynchronized(streamCodec).build()` + `DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, modid)` shape is real and works on NeoForge 21.1.228.

**However, this "meta" component is exactly the kind of legacy-metadata shim the project's own hard rules steer away from** ("ItemStack NBT must become Data Components, never raw NBT reads/writes" - the spirit of that rule is to stop treating ItemStack as a bag of loosely-typed legacy state). Since Neo Edition is explicitly "reference only for API/registration/rendering patterns, never for content or business logic," I am not adopting its meta-emulation design, only using it to confirm the DataComponentType registration API is real.

## 4. Recommended port plan

Keep this package (`com.hbm.inventory.material`) **pure data** - no `Item`/`ItemStack` construction inside it at all. Concretely:

1. **`MaterialShapes`** - port near-verbatim as a plain data class of static singletons (quantum math is version-agnostic arithmetic, nothing NeoForge-specific). Two changes:
   - Drop the `net.minecraftforge.fml.common.Loader` GregTech-compat block (`registerCompatShapes`) entirely - no GTCEu integration in Phase 0/1 scope; note it as intentionally dropped rather than stubbed.
   - Add, alongside the existing ore-dict-style `prefixes[]` (kept for documentation/back-reference to CE), a canonical lowercase snake_case **registry name token** per shape (e.g. `INGOT -> "ingot"`, `DENSEWIRE -> "dense_wire"`, `CASTPLATE -> "cast_plate"`) and a **plural Common-tag folder name** (e.g. `"ingots"`, `"dense_wires"`) since NeoForge/Fabric Common tags use plural folders (`c:ingots/iron`) while a sane per-item registry id reads better singular (`hbm:iron_ingot`). This is the field Phase 1's item-generator will consume to mint both the `DeferredRegister` item id and its tag membership - document it precisely so all Phase-1 agents use the same convention.
2. **`NTMMaterial`** - port the data fields as-is (id, names, autogen set, traits, smeltable behavior, conversion, colors). Changes:
   - Replace `getTranslationKey()`/`getLocalizedName()` (CE, uses `I18nUtil`) with `getDescriptionId()` returning `"hbmmat." + names[0].toLowerCase(Locale.ROOT)` and a `getName()` returning `Component.translatable(...)` (`MutableComponent`) - this matches both the Neo Edition reference and standard 1.21 idiom, no custom i18n utility needed.
   - **Remove `make(Item, int)` / `make(Item)` entirely.** There is no single "ingot item + meta" to construct against in the port. Once Phase 1 registers one real `Item` per (shape, material) combination it actually needs, stack construction becomes a plain `new ItemStack(MyItems.IRON_INGOT.get(), amount)` at the call site - no helper on `NTMMaterial` is needed for that. If a convenience lookup is wanted later, it belongs in the Phase-1 item registry class (which knows about `DeferredHolder<Item, Item>`), not in this data-only package.
   - Keep the constructor's dependency on `OreDictManager.DictFrame` for `names[]` only if `OreDictManager` gets ported with an equivalent `DictFrame`-like holder of alias strings; otherwise accept `String... names` directly. This is a cross-area decision - flagged below.
3. **`Mats`** - port the ~90 `MAT_*` catalog constants and the three factory helpers (`make`, `makeSmeltable`, `makeAdditive`, `makeNonSmeltable`) verbatim in content (same ids, same names, same colors, same autogen sets, same traits) - this is exactly the "central registry" Phase 1 depends on, and preserving it 1:1 is the highest-value part of this port. Changes:
   - `getMaterialsFromItem(ItemStack)` must be reimplemented against **`TagKey<Item>`** lookups instead of ore-dictionary name strings: for a given stack, walk its item tags (`stack.getItem().builtInRegistryHolder().tags()` or a NeoForge-provided helper), match tag paths against the shape's Common-tag folder + material name, and fall back to `materialEntries`/`materialOreEntries` exactly as CE does. This method's *contract* (arbitrary item -> `List<MaterialStack>`) is unchanged; only the matching mechanism changes. This directly depends on how `OreDictManager` (a separate area) is ported to tags - flagged as a cross-area risk below.
   - `getSmeltingMaterialsFromItem` needs no change beyond compiling against the above.
   - `ItemScraps.getMats(stack)` special-case call stays, contingent on `ItemScraps` existing in the ported `com.hbm.items.machine` package (another area's scope).
4. **`MatDistribution`** - port the *data* (every `registerEntry`/`registerOre` call, and the `hbmCrucibleSmelting.json` file name/format/comment) 1:1. Structural changes only:
   - Drop the `Loader.isModLoaded("gregtech")` log line and the commented-out GTCEu conditional - no GTCEu compat in this port.
   - `registerEntry` keys can be `Item`, `Block`, or `ItemStack` today; in the port this becomes `Item`/`Block`/`ItemStack` from the NeoForge-registered equivalents (`ModItems`/`ModBlocks` in the port project, owned by other areas) - the `ComparableStack` wrapper type itself is a different area's concern (`com.hbm.inventory.RecipesCommon`), just confirm it's ported before this compiles.
   - `DictFrame.fromOne(Item, Enum)` (meta-based enum-variant stack builder) needs the same treatment as `NTMMaterial.make` - once `ModItems.casing` becomes several distinct registered items (one per `EnumCasingType` value) instead of one meta item, these calls become direct references to the correct registered item, no helper needed.
   - `SerializableRecipe` base class (JSON load/save plumbing) is out of this area's scope; confirm with whichever area owns `com.hbm.inventory.recipes.loader` before wiring the JSON I/O back in - it is not blocking for the in-memory catalog itself.

## 5. NBT -> Data Component mapping

This area's 4 files do not read or write any ItemStack NBT directly - `NTMMaterial`/`Mats`/`MaterialShapes` are pure in-memory data, and `MatDistribution`'s only serialization is its own crucible-recipe JSON config file (not ItemStack NBT). **No NBT keys were found in this scope requiring a Data Component mapping.** The one relevant per-stack identity concern - "which material does this stack represent" - is deliberately *not* solved with a data component in my recommended plan (see section 3/4): it should be solved by registering a distinct `Item` per (material, shape) combination instead of tagging a shared item with an integer, which is both more idiomatic NeoForge 1.21 and avoids reintroducing a metadata-shaped component. If a future phase decides the item count is too large and reverts to a shared-item + component design after all, the correct data component would be an `Item`/material key component (e.g. `DataComponentType<ResourceLocation>` or `DataComponentType<Holder<NTMMaterialType>>`), not a bare integer meta value like Neo Edition's shim - flagged as an open question, not a decision I made unilaterally since it affects every Phase 1 item-generation agent.

## 6. Cross-area dependencies

- **`OreDictManager` / `DictFrame`** (not in this area's scope): every `Mats.MAT_*` constant is constructed from a `DictFrame` carrying alias strings, and `MatDistribution.registerOre` keys off ore-dict prefix strings (`OreDictManager.IRON.ore()`, `OreDictManager.TH232.all(...)`, etc.). This is the single biggest cross-area coupling - whoever ports `OreDictManager` needs to agree with this area on whether materials keep a `DictFrame`-shaped alias holder or move straight to `String... names` plus NeoForge `TagKey<Item>` for the ore-equivalent lookups.
- **`com.hbm.items.ModItems` / `com.hbm.items.machine.ItemScraps`**: `Mats.getMaterialsFromItem` special-cases `ModItems.scraps`; `MatDistribution` references a dozen-plus `ModItems.*` and `ModBlocks.*` constants and enum-variant items (`EnumCasingType`, `EnumAshType`, `EnumStoneType`). None of these are portable until the corresponding item/block areas exist.
- **`com.hbm.inventory.RecipesCommon`** (`ComparableStack`, `AStack`, `OreDictStack`): used throughout `Mats`/`MatDistribution` for stack-keyed maps; needs its own 1.21 port (no metadata field to key off of - `ComparableStack` will need to compare `Item` + Data Components instead of `Item` + damage).
- **`com.hbm.inventory.recipes.loader.SerializableRecipe`**: the JSON load/save base class `MatDistribution` extends; out of scope here.
- **Phase 1 item-generation agents** (the stated primary consumer of this area): they will need, per material, its `autogen` shape set and the shape's quantity/registry-name-token to decide exactly which `Item`s to register and how much of that material each represents (recipes, crucible yields, etc.) - this is why getting `MaterialShapes`'s naming/quantity model right now matters so much.

## 7. Risks / open questions

1. **Item-count explosion**: ~90 materials x up to ~20 autogen shapes each is on the order of 700-900 distinct `Item` registrations if every (material, shape) pair in `autogen` becomes its own registered item, which is what my recommended plan implies. This is normal for tech mods (justified by CE's own scope) but Phase 1 should confirm this is acceptable before generating that many entries, and should generate them via a loop over `Mats.orderedList` x `NTMMaterial.autogen`, not by hand.
2. **Naming convention not yet fixed**: I recommend `shape_token` + `material_name` -> singular snake_case item id (`hbm:iron_ingot`) and plural Common-tag folder (`c:ingots/iron`), but this is a naming decision that ripples into every Phase 1 item file, every recipe JSON, and every lang file. It should be locked down once, here, before Phase 1 starts generating content - flagging it as the most important open decision from this research pass.
3. **`getMaterialsFromItem`'s tag-based rewrite** depends entirely on how the ore-dictionary equivalent is designed by whoever owns `OreDictManager`; until that area's plan exists, this method's implementation is a placeholder shape only (contract is clear, mechanism is not).
4. **GTCEu (GregTech) compatibility is dropped**, not stubbed, per the "no half-finished implementations" rule - `MaterialShapes.registerCompatShapes()` and the crucible's commented-out GTCEu conditional are excluded from Phase 0/1 scope entirely.
5. **`id` field retention**: CE's numeric material id existed almost entirely to serve as ItemStack metadata. Since `MatDistribution`'s JSON serialization already keys off material *name* strings (not ids), and the port drops meta-based `ItemStack` construction, the numeric `id` is no longer load-bearing for anything found in this scope. I recommend keeping it anyway (unique stable identity, useful for sort order/"vanilla space"/"alloy space"/"extension space" bookkeeping comments, and any future world-gen or network-sync use), but it should not be treated as a droppable field just because its original purpose (metadata) disappeared.
