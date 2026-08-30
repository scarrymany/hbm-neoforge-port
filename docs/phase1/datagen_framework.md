# Phase 1 Research: Datagen Framework

Area key: `datagen_framework`. Read-only research pass, no source files were written. This is
infrastructure research, not a CE-source-mapping task: CE (1.12.2) has no equivalent of this
system at all (see section 0), so everything here is designed from the confirmed real NeoForge
21.1 APIs in the Neo Edition reference plus the one concrete precedent Phase 0 already set
(`com.hbm.damage.datagen.ModDamageTypeTagsProvider`).

## 0. CE has nothing to port here

`C:\...\upstream\hbm-ce\src\main\java\com\hbm\datagen\` exists, but it is unrelated: it is a
1.12.2-only DSL (`AdvGen.java` + `dsl/AdvancementDSL.java`) that hand-generates *advancement* JSON
via a `doGen` boolean flag you flip and run once client-side. Minecraft 1.12.2 predates the
1.13 flattening, so there was never a per-item/per-block model, blockstate, tag, or loot-table
datagen system to port - those simply did not exist as a Mojang/Forge concept yet; CE ships all of
that as hand-authored JSON under `src/main/resources`. Nothing from CE's `datagen` package
carries over. Advancement generation itself is out of scope for this report (and for Phase 1) -
note it for whichever later area owns advancements.

## 1. Confirmed real NeoForge 21.1 datagen provider classes

All confirmed by reading working code in the Neo Edition reference
(`C:\...\upstream\neo-edition\src\main\java\com\hbm\datagen\`), not invented. Ten files:

| File (Neo Edition) | Extends | Package of base class |
|---|---|---|
| `NtmDataGenerators.java` | (event subscriber, no base class) | - |
| `NtmItemModelProvider.java` | `ItemModelProvider` | `net.neoforged.neoforge.client.model.generators` |
| `NtmBlockStateProvider.java` | `BlockStateProvider` | `net.neoforged.neoforge.client.model.generators` |
| `NtmBlockLootTableProvider.java` | `BlockLootSubProvider` | `net.minecraft.data.loot` |
| `NtmBlockTagProvider.java` | `BlockTagsProvider` | `net.neoforged.neoforge.common.data` |
| `NtmItemTagProvider.java` | `ItemTagsProvider` | `net.minecraft.data.tags` |
| `NtmFluidTagsProvider.java` | `FluidTagsProvider` | `net.minecraft.data.tags` |
| `NtmDamageTypeTagsProvider.java` | `DamageTypeTagsProvider` | `net.minecraft.data.tags` |
| `NtmLanguageProvider.java` | `LanguageProvider` | `net.neoforged.neoforge.common.data` |
| `NtmSoundDefinitionsProvider.java` | `SoundDefinitionsProvider` | `net.neoforged.neoforge.common.data` |
| `NtmRecipeProvider.java` | `RecipeProvider` | `net.minecraft.data.recipes` |

Constructor shapes confirmed by reading each file's actual constructor (all take a
`PackOutput` plus some combination of `CompletableFuture<HolderLookup.Provider>` and
`ExistingFileHelper`, matching each base class's own signature):

```java
// client-side model/blockstate providers - take modid string directly, no HolderLookup
public NtmItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
public NtmBlockStateProvider(PackOutput output, ExistingFileHelper helper)

// tag providers - vanilla base classes patched by NeoForge to add modid + ExistingFileHelper params
public NtmBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper)
public NtmItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper)
public NtmFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper helper)
public NtmDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper helper)

// loot - protected constructor, only usable via LootTableProvider.SubProviderEntry factory
protected NtmBlockLootTableProvider(HolderLookup.Provider registries)

// lang / sounds - no HolderLookup needed
public NtmLanguageProvider(PackOutput output)               // + hardcoded "en_us" locale
protected NtmSoundDefinitionsProvider(PackOutput output, ExistingFileHelper helper)

// recipes
public NtmRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
```

All ten are wired into one `GatherDataEvent` subscriber, `NtmDataGenerators`
(`@EventBusSubscriber(modid = NuclearTechMod.MODID)`, `@SubscribeEvent static void gatherData(GatherDataEvent event)`),
which pulls `generator`, `output`, `helper`, and `lookup` off the event once and passes them to
each provider constructor, gating client-only providers behind `event.includeClient()` and
server-only ones behind `event.includeServer()`.

## 2. `build.gradle` is already ready

`C:\Users\Sergo127\Desktop\hbms\build.gradle` already defines the `data` run config (confirmed,
lines ~33-37):

```groovy
data {
    data()
    programArguments.addAll '--mod', project.mod_id, '--all', '--output', file('src/generated/resources/').getAbsolutePath(), '--existing', file('src/main/resources/').getAbsolutePath()
}
```

and `sourceSets.main.resources { srcDir 'src/generated/resources' }` is already present, so
generated JSON under `src/generated/resources` is picked up as a resource root automatically.
**No `build.gradle` change is needed for Phase 1** - the moment a `GatherDataEvent` subscriber
class exists and is on the classpath, running the `data` Gradle run config (or
`./gradlew runData` if that task alias exists; otherwise the IDE "data" run configuration NeoGradle
generates) will produce output under `src/generated/resources/`, existing-file-checked against
`src/main/resources/`.

## 3. Naming/package precedent already set in this port (not invented - read from disk)

Phase 0's `damage_types` area originally wrote its tag provider at
`com.hbm.datagen.ModDamageTypeTagsProvider` (mirroring Neo Edition's flat `com.hbm.datagen`
package), then a fix pass relocated it to `com.hbm.damage.datagen.ModDamageTypeTagsProvider`
specifically because a shared `com.hbm.datagen` package with no collision-coordination mechanism
was flagged as a defect for a port worked on by multiple concurrent areas/phases (see
`docs/phase0/DIGEST.md` lines 475-487). The file that exists on disk today is:

- `C:\Users\Sergo127\Desktop\hbms\src\main\java\com\hbm\damage\datagen\ModDamageTypeTagsProvider.java`
  - package `com.hbm.damage.datagen`, extends `net.minecraft.data.tags.DamageTypeTagsProvider`,
    constructor `(PackOutput, CompletableFuture<HolderLookup.Provider>, @Nullable ExistingFileHelper)`
    calling `super(output, provider, MainRegistry.MODID, helper)`.
  - Companion custom tag class at `com.hbm.damage.tags.ModDamageTypeTags` (`TagKey<DamageType>`
    constants `IS_TAU`, `IS_SUBATOMIC`, `IS_ENERGY`, `ABSOLUTE`).

This is a real, already-committed design decision, not a hypothetical - **Phase 1's datagen
classes must follow the same per-domain-package convention**, not Neo Edition's flat
`com.hbm.datagen` bucket. `com.hbm.util.TagsUtil` also already exists (a `CustomData` component
helper, unrelated to datagen but confirms `com.hbm.util` is the existing general-helper package).

**My recommendation for where each Phase 1 provider lives**, applying that precedent plus the
practical constraint that ItemModelProvider/BlockStateProvider/ItemTagsProvider/BlockTagsProvider
each independently accumulate a map of "resource location -> generated file" and write it out in
their own `run()` call - meaning **multiple provider instances can coexist and be registered
side by side to the same `GatherDataEvent` as long as they target disjoint entries**, which is the
real fix for the multi-area collision problem the damage_types fix pass was reacting to (better
than a package rename alone, since two areas both editing one 900-line provider class is still a
merge-conflict magnet regardless of which package it sits in):

| Provider | Recommended package | Rationale |
|---|---|---|
| `ModItemModelProvider` (autogenerated material items: ingots/plates/dusts/etc.) | `com.hbm.items.datagen` | Owned by whichever area builds `ModItems`' material-shape entries; content-only, mirrors `damage.datagen` precedent. |
| `ModItemModelProvider` (hand-authored/special items - tools, machines, blueprints) | Same class, different `registerModels()` section, OR a second `ModItemModelProviderMisc` instance registered alongside the first if the item list grows large enough across phases to become a merge-conflict risk. Start with one class; split only when a later phase's addition would collide. | Matches how Neo Edition's own ~950-line file grew - but that file was authored by one continuous stream of work, not concurrent phase-agents, so treat the single-class version as the Phase 1 starting point and flag the split as a later-phase call, not a Phase 1 requirement. |
| `ModBlockStateProvider` | `com.hbm.blocks.datagen` | Blocks area equivalent of the above. |
| `ModBlockTagProvider` | `com.hbm.blocks.datagen` | `BlockTagsProvider` is inherently block-domain. |
| `ModItemTagProvider` | `com.hbm.items.datagen` | `ItemTagsProvider` needs a `TagLookup<Block>` from the block tag provider (see 4.3) but is otherwise item-domain. |
| `ModBlockLootTableProvider` | `com.hbm.blocks.datagen` | Loot tables are keyed by block, not item. |
| `ModLanguageProvider` | `com.hbm.datagen` (stays centralized/flat) | Translations are inherently cross-cutting - every area's items, blocks, damage types, and future GUIs all need entries in the *same* `en_us.json`, and Neo Edition itself keeps this one centralized despite everything else being content-heavy. Splitting it would mean either N separate `LanguageProvider` instances all writing to the same `assets/hbm/lang/en_us.json` path (which DOES collide - `LanguageProvider.run()` writes one file per instance's locale+modid, and instances sharing a locale/modid pair will overwrite each other's output on the given `PackOutput`, since the file path collides even though instances are otherwise independent) or one shared class. Keep it one shared class and expect merge conflicts as a known cost, exactly as CE's own `en_US.lang` was one big shared file. |
| `ModDataGenerators` (the `GatherDataEvent` subscriber) | `com.hbm.datagen` | Matches the task's own naming (`com.hbm.datagen.ModDataGenerators`); this class only imports and wires the others, so it carries near-zero collision risk regardless of how many areas touch it (each area adds one `generator.addProvider(...)` line). |

This recommendation is my own architectural judgment built on the confirmed precedent and
confirmed API shapes - it is not itself lifted from either reference repo, and should be treated
as a proposal for whoever runs the actual implement stage to confirm or override, not a settled
fact.

## 4. Design for each provider, mapped to Phase 1's actual needs

### 4.1 `ModItemModelProvider extends net.neoforged.neoforge.client.model.generators.ItemModelProvider`

```java
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MainRegistry.MODID, existingFileHelper);
    }
    @Override
    protected void registerModels() {
        // one call per item; for Phase 1's flat material items this is just:
        this.basicItem(ModItems.IRON_INGOT.get());
        // ...
    }
}
```

- `basicItem(Item)` generates `item/generated` parent + single `layer0` texture pointing at
  `item/<path>` by convention - this covers essentially all of Phase 1's flat 2D material items
  (ingots, nuggets, dusts, plates, wires, gems...), which is the overwhelming majority of what
  `MaterialShapes`-driven autogen produces.
- Neo Edition's `layeredItem`/`basicCustomLayerItem`/`entityItem`/`handheldItem` helper methods
  (confirmed at the bottom of `NtmItemModelProvider.java`, lines ~934-947) are private
  conveniences wrapping the same `getBuilder(...).parent(...).texture(...)` calls with different
  parent models (`item/generated` two-layer, `item/handheld`, `builtin/entity`). None of these are
  framework APIs - they are just repeated boilerplate the reference file factored out, and Phase 1
  should do the same once item variety beyond flat 2D textures shows up (tools needing
  `item/handheld`, anything rendered by a `BlockEntityWithoutLevelRenderer` needing
  `builtin/entity`).
- **`ICustomItemModelRegister` pattern (confirmed real, actively used):** Neo Edition's provider
  opens by iterating `NtmItems.ITEMS.getEntries()` and calling `icimr.registerItemModel(this, loc)`
  on any `Item` instance implementing a small `ICustomItemModelRegister` marker interface, letting
  an item class own its own model-registration logic instead of every special case being
  hardcoded inline in the provider. This is the item-side counterpart of the already-confirmed
  `ICustomBlockModelRegister` pattern (section 4.2). Phase 0's `base_items.md` findings note CE's
  legacy `IModelRegister` marker interface should port 1:1 with updated types - **that ported
  interface is exactly what should back this hook**, so Phase 1 does not need to invent a new
  interface, only confirm `IModelRegister`'s method signature lines up with
  `registerItemModel(ItemModelProvider, ResourceLocation)` (or adapt it to that shape if the
  Phase 0 port kept the CE signature verbatim).

### 4.2 `ModBlockStateProvider extends net.neoforged.neoforge.client.model.generators.BlockStateProvider`

```java
public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, MainRegistry.MODID, helper);
    }
    @Override
    protected void registerStatesAndModels() {
        this.simpleCubeAllBlock(ModBlocks.SOME_ORE);
        // ...
    }
}
```

- For Phase 1's "simple blocks" (ores, decorative blocks, plain storage blocks with no
  blockstate properties): `simpleCubeAllBlock(DeferredBlock<? extends Block>)` is the workhorse -
  confirmed as a small private helper in the reference wrapping
  `this.simpleBlockWithItem(block.get(), this.cubeAll(block.get()))`, i.e. one cube-all model
  shared between the block and its `BlockItem`. Phase 1 should port this exact helper (it is
  trivial and not framework-provided under that name - `cubeAll`/`simpleBlockWithItem` are the
  real `BlockStateProvider` API methods it composes).
- `cubeBottomTopBlock`/`cubeTop`/`logBlock`/`slabBlock`/`stairsBlock` are real `BlockStateProvider`
  API methods (confirmed via their use with vanilla-shaped arguments in the reference) for blocks
  with distinct top/bottom/side textures, log-style column blocks, and vanilla slab/stair
  variants respectively.
- Blocks with real blockstate properties (multiple variants selected by an `IntegerProperty`/
  `EnumProperty`, e.g. the reference's `OreBasaltBlock` 6-way subtype or `SellafieldSlakedBlock`'s
  tinted variants) use `getVariantBuilder(block).forAllStates(state -> ...)` /
  `forAllStatesExcept(...)`, both confirmed real `BlockStateProvider` API. Whether Phase 1 needs
  this at all depends on which CE blocks in scope actually carry a metadata-driven visual variant
  vs. plain single-state blocks - that's a question for whichever area maps the actual CE block
  list, not this infrastructure report.
- **`ICustomBlockModelRegister` pattern (confirmed real, actively used):** the reference provider
  opens with exactly this loop:
  ```java
  NtmBlocks.BLOCKS.getEntries().forEach(holder -> {
      Block block = holder.get();
      if (block instanceof ICustomBlockModelRegister icbmr) {
          ResourceLocation loc = Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block));
          icbmr.registerModel(this, loc);
      }
  });
  ```
  letting a `Block` subclass own arbitrarily complex model-generation logic (custom loaders,
  multipart builders, per-state textures) via a marker interface instead of the provider needing a
  giant per-block `if/else`. Phase 1 should port an equivalent `ICustomBlockModelRegister`
  interface into whichever package owns block marker interfaces (likely `com.hbm.blocks`,
  alongside wherever `ModBlocks` itself lives) so any block class complex enough to need custom
  model logic can implement it directly, matching the confirmed real pattern rather than growing
  an ever-larger provider method.
- Custom model loaders (`CustomLoaderBuilder` subclasses like the reference's
  `BlockModelBuilderBase`/`DuctBlockLoaderBuilder`/`BarrelBlockModelBuilder`/etc., which back a
  custom Minecraft model-loader identified by `NuclearTechMod.withDefaultNamespace("ntm_geometry_loader")`)
  are a Phase 2+ concern tied to a not-yet-ported custom geometry/baked-model system
  (`com.hbm.render.model.loader.NtmGeometry`), not Phase 1 "simple blocks" - noted here only so
  the pattern is not mistaken for something Phase 1 must reproduce.

### 4.3 `ModItemTagProvider extends net.minecraft.data.tags.ItemTagsProvider` and `ModBlockTagProvider extends net.neoforged.neoforge.common.data.BlockTagsProvider`

This is where Phase 0's `MaterialShapes`/`NTMMaterial` naming convention plugs in directly.
`MaterialShapes.commonTag(NTMMaterial mat)` (confirmed, `MaterialShapes.java` lines 135-141)
already builds the exact `TagKey<Item>` Phase 1 needs to emit:

```java
public TagKey<Item> commonTag(NTMMaterial mat) {
    return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", tagFolder + "/" + mat.getRegistryName()));
}
```

so the item tag provider's Phase-1-specific job is mechanical, not a design problem: for every
`(material, shape)` pair that has a real registered item (`shape.registryName != null`), call

```java
this.tag(shape.commonTag(mat)).add(theGeneratedItem);
```

This produces `c:ingots/iron`, `c:nuggets/iron`, `c:plates/steel`, etc. - the Fabric/NeoForge
common convention (`c:` namespace, plural folder) both `MaterialShapes`'s own doc comment and
Phase 0's design already anticipated. `BLOCK`-shape materials additionally need the block-side
equivalent (`c:storage_blocks/<material>`, per `MaterialShapes.BLOCK`'s `tagFolder`) registered
through `ModBlockTagProvider`, not the item provider - confirmed real precedent for exactly this
split is Neo Edition's `NtmItemTagProvider`/`NtmBlockTagProvider` both existing side by side with
`ItemTagsProvider`'s constructor taking a `CompletableFuture<TagLookup<Block>> blockTags` sourced
from `blockTagsProvider.contentsGetter()` (confirmed in `NtmDataGenerators`, line 62) precisely so
item tags can be generated from (e.g.) a block's own tag membership when relevant - Phase 1 likely
does not need that cross-reference immediately, but the constructor shape requires wiring it
through regardless (see 4.7).

Beyond material-shape tags, Phase 1's item tag provider should also seed the vanilla tool tags
(`ItemTags.PICKAXES`/`AXES`/`SHOVELS`/`HOES`, `minecraft:enchantable/*`) for any Phase-1-scope
tool items, following the confirmed pattern in `NtmItemTagProvider.java` lines 55-108 - though
whether tools are in Phase 1's "items and simple blocks" scope or a later phase is for the
CE-source-mapping areas to decide, not this report.

`ModBlockTagProvider` should seed `BlockTags.MINEABLE_WITH_PICKAXE` (and `_SHOVEL`/`_AXE` where
relevant) for every Phase 1 ore/decorative block, per the confirmed real pattern in
`NtmBlockTagProvider.java` lines 85+. Blocks with no explicit mineable tag fall back to
hand-breaking-only in vanilla, which is almost certainly wrong for ores/stone-adjacent blocks.

### 4.4 `ModBlockLootTableProvider extends net.minecraft.data.loot.BlockLootSubProvider`

```java
public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }
    @Override
    protected void generate() {
        this.dropSelf(ModBlocks.SOME_DECO_BLOCK.get());
        this.add(ModBlocks.SOME_ORE.get(), block -> this.oreDrop(block, ModItems.SOME_CHUNK.get()));
    }
}
```

- `dropSelf(Block)` is the trivial case (plain storage/decorative blocks that drop themselves).
- `oreDrop(Block, Item)` / `oreDrop(Block, Item, int min, int max)` (fortune-scaled random-count
  drop) and `oreDropNoFortune(Block, Item)` are confirmed real `BlockLootSubProvider` API methods,
  covering essentially every Phase 1 ore block that drops a raw-material item rather than itself.
- This provider is **not** registered directly via `generator.addProvider` - it is wrapped in a
  `LootTableProvider.SubProviderEntry` and handed to a `LootTableProvider` (see 4.7), because
  vanilla's loot datagen is validated as one batch across all loot-table-producing sub-providers
  (block loot, entity loot, chest loot, etc.) rather than provider-by-provider.
- The constructor is `protected`, only reachable through the `SubProviderEntry`'s factory
  reference (`ModBlockLootTableProvider::new`) - confirmed real, not an oversight to work around.

### 4.5 `ModLanguageProvider extends net.neoforged.neoforge.common.data.LanguageProvider`

```java
public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, MainRegistry.MODID, "en_us");
    }
    @Override
    protected void addTranslations() {
        this.add(ModItems.IRON_INGOT, "Iron Ingot");
        // ...
    }
}
```

`add(DeferredItem<?>/DeferredBlock<?>, String)` overloads (confirmed real, used throughout the
reference) resolve the item/block's own registry name to build the `item.hbm.<path>` /
`block.hbm.<path>` translation key automatically - Phase 1 does not need to hand-build lang keys.
Note the reference also demonstrates a `.desc` / `.desc.p11` suffix convention for CE's old
tooltip-description system (`this.add(NtmItems.INGOT_NEPTUNIUM, DESC, "...")` where
`DESC = ".desc"`) - whether Phase 1's items carry an equivalent tooltip system is a question for
the base_items/tooltip area, not this report; the mechanism (arbitrary suffix + `add` overload) is
confirmed available either way.

### 4.6 Fluid tags, damage type tags, sounds - already owned or out of scope

- `ModFluidTagsProvider` (mirroring `NtmFluidTagsProvider`) is Phase 0's fluid area's concern, not
  Phase 1's - noted here for completeness of "what other tag providers this port will eventually
  need" but out of this report's scope since Phase 1 is items/simple blocks.
- The damage-type tag provider already exists (`com.hbm.damage.datagen.ModDamageTypeTagsProvider`,
  see section 3) and only needs to be **plugged into** `ModDataGenerators` (section 5), not
  written.
- `ModSoundDefinitionsProvider` (mirroring `NtmSoundDefinitionsProvider`) belongs to whichever
  area owns `com.hbm.sound`/the sound registry (Phase 0's `sound` area) - out of scope here.
- `ModRecipeProvider` (mirroring `NtmRecipeProvider`, extends `net.minecraft.data.recipes.RecipeProvider`,
  uses `ShapedRecipeBuilder`/`ShapelessRecipeBuilder` with `RecipeCategory` enum values) is
  confirmed real but is its own large area (CE's crafting-recipe corpus) - out of scope for this
  report, only noted so `ModDataGenerators`'s design in section 5 has a slot for it.

### 4.7 `ModDataGenerators` - the `GatherDataEvent` subscriber (Phase 0 anticipated, did not create)

Confirmed anticipated but explicitly not created in Phase 0 (`docs/phase0/damage_types.md` line
36-37, `docs/phase0/DIGEST.md` lines 477-479, 494, 511-512 all point at this exact gap). Design,
in `com.hbm.datagen.ModDataGenerators` per the task's own naming and section 3's package table:

```java
package com.hbm.datagen;

@EventBusSubscriber(modid = MainRegistry.MODID)
public class ModDataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        // Datapack-registry content (damage types today; biomes/features if/when those areas need it)
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.DAMAGE_TYPE, ModDamageTypes::bootstrap);
        DatapackBuiltinEntriesProvider datapackProvider =
                new DatapackBuiltinEntriesProvider(output, lookup, builder, Set.of(MainRegistry.MODID));
        generator.addProvider(event.includeServer(), datapackProvider);

        // Client-side
        generator.addProvider(event.includeClient(), new ModItemModelProvider(output, helper));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(output, helper));
        generator.addProvider(event.includeClient(), new ModLanguageProvider(output));

        // Server-side
        BlockTagsProvider blockTagsProvider = new ModBlockTagProvider(output, lookup, helper);
        generator.addProvider(event.includeServer(), blockTagsProvider);
        generator.addProvider(event.includeServer(),
                new ModItemTagProvider(output, lookup, blockTagsProvider.contentsGetter(), helper));
        generator.addProvider(event.includeServer(),
                new ModDamageTypeTagsProvider(output, lookup, helper));

        LootTableProvider.SubProviderEntry blockLootSubProvider =
                new LootTableProvider.SubProviderEntry(ModBlockLootTableProvider::new, LootContextParamSets.BLOCK);
        generator.addProvider(event.includeServer(),
                (DataProvider.Factory<LootTableProvider>) lootOutput ->
                        new LootTableProvider(lootOutput, Collections.emptySet(), List.of(blockLootSubProvider), lookup));
    }
}
```

Notes on this design, all pinned to confirmed details:

- `@EventBusSubscriber(modid = ...)` + a `static` `@SubscribeEvent` method is the confirmed real
  self-registering pattern (matches Phase 0's `packet` area, which the DIGEST already notes
  "self-registers via `@EventBusSubscriber`" - no manual `MainRegistry` call needed, consistent
  with `docs/phase0/DIGEST.md` line 514 listing `damage_types`' *content* class as needing no
  `MainRegistry` call, though its *provider* still needs to be added to this subscriber method).
- `event.includeClient()` / `event.includeServer()` are confirmed real `GatherDataEvent` gates -
  passing them as the first argument to `generator.addProvider` is how NeoForge's datagen CLI
  flags (`--client`/`--server`, or `--all` as `build.gradle`'s `data` run config already passes)
  decide which providers actually run.
- The block tag provider's `contentsGetter()` must be captured into a local variable and reused
  for the item tag provider's `blockTags` constructor argument - confirmed required by
  `ItemTagsProvider`'s real constructor shape (section 1), not optional plumbing.
- Item models, block states, and item/block tags for Phase 1's actual content are additive calls
  inside each provider's own `register*`/`addTags` override - `ModDataGenerators` itself never
  needs to change as Phase 1's item/block count grows, only the individual provider classes do.
- `ModRecipeProvider` and `ModSoundDefinitionsProvider` slots are intentionally left out of this
  skeleton since neither is Phase 1 scope (section 4.6) - whichever area owns them adds one more
  `generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup))` /
  `generator.addProvider(event.includeClient(), new ModSoundDefinitionsProvider(output, helper))`
  line each, following the same additive pattern.

## 5. Summary: what Phase 1 must create

1. `com.hbm.items.datagen.ModItemModelProvider extends ItemModelProvider`
2. `com.hbm.blocks.datagen.ModBlockStateProvider extends BlockStateProvider`
3. `com.hbm.blocks.datagen.ModBlockTagProvider extends BlockTagsProvider`
4. `com.hbm.items.datagen.ModItemTagProvider extends ItemTagsProvider`
5. `com.hbm.blocks.datagen.ModBlockLootTableProvider extends BlockLootSubProvider`
6. `com.hbm.datagen.ModLanguageProvider extends LanguageProvider`
7. `com.hbm.datagen.ModDataGenerators` - the `GatherDataEvent` subscriber wiring all of the above
   together, plus plugging in the already-existing
   `com.hbm.damage.datagen.ModDamageTypeTagsProvider` that Phase 0 built but never wired up.
8. Port CE's `IModelRegister` (already flagged in `docs/phase0/base_items.md` #9 as a marker
   interface to port 1:1) into the `ICustomItemModelRegister`-shaped hook item classes need to
   own complex model logic, confirmed real via the Neo Edition reference (section 4.1). A block
   equivalent, `ICustomBlockModelRegister` (section 4.2), also needs to exist wherever `ModBlocks`
   marker interfaces live if any Phase 1 block needs custom (non-cube-all) model generation.
9. No `build.gradle` change needed - the `data` run config is already correctly configured
   (section 2).

## 6. Open questions / ambiguities for the implement stage

- **Single vs. split item-model/tag provider classes.** I recommend starting with one class per
  provider type for Phase 1 (section 3's table) and only splitting into multiple co-registered
  instances if a later phase's addition would otherwise create a large merge-conflict-prone diff
  against an already-huge file. This is my judgment call, not a settled convention - flag it for
  confirmation before Phase 2+ content lands on top of whatever Phase 1 builds.
- **Whether `ICustomItemModelRegister`/`ICustomBlockModelRegister` should be introduced in Phase 1
  at all**, versus deferring them until the first item/block that actually needs non-`basicItem`/
  non-`simpleCubeAllBlock` model generation shows up. Given Phase 1 is scoped to "items and simple
  blocks," it is plausible zero Phase 1 content needs the hook yet - but the interfaces are cheap
  to add now and every later phase (machines, weapons, multiblocks) will need them, so introducing
  them alongside the first datagen provider avoids a retrofit later. Recommend introducing them in
  Phase 1 even if no Phase 1 item/block uses them yet, but this is a judgment call for whoever
  scopes the actual implement stage, not a hard requirement.
- **Exact set of Phase 1 items/blocks needing `MINEABLE_WITH_PICKAXE`/tool tags/common-tag
  membership** depends entirely on the CE-source-mapping areas' output (which CE files are in
  Phase 1 scope), not on this infrastructure report - the mechanisms above cover whatever that
  list turns out to be.
