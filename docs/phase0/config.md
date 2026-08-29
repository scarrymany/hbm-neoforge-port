# Phase 0 Research Report: Config System (`com.hbm.config`)

Scope: `C:\Users\Sergo127\Desktop\hbms\upstream\hbm-ce\src\main\java\com\hbm\config\**\*.java` (23 files). Read-only
research stage. No files were written or edited in the port project.

## 1. Class inventory

| File | Purpose |
|---|---|
| `GeneralConfig.java` | The largest config class. ~90 static fields covering rendering toggles, world-gen toggles, gas/hazard toggles, misc gameplay switches, plus two entire alternate "mode" blocks: **528 mode** (`enable528*`, category `528`) and **LBSM / Less Bullshit Mode** (`enableLBSM*`, category `LESS BULLSHIT MODE`). Also owns a `Set<String> leadSafeForgeContainerWhitelist` parsed from `modid:item:meta` strings, with validation that throws `IllegalArgumentException` on malformed entries. Has post-load cross-field validation (disabling shader-dependent effects when OpenGL 3.3 unsupported, enforcing `enable528 -> !enableLBSM`, etc). |
| `CommonConfig.java` | Not a config category itself; a **helper/registry class**. Declares all `CATEGORY_*` string constants (`01_general` .. `18_weapons`, plus `528` and `LESS BULLSHIT MODE`) used by every other Forge-`Configuration`-based class, and generic helper methods (`createConfigBool/Int/Double/String/StringList/IntList/HashMap/HashSet`) that wrap `Configuration.get(...)` + `Property.setComment(...)`. Also has `setDef`/`setDefZero` (validate positive-int config values, log+revert to default on violation) and `parseStructureFlag` (tri-state true/false/"respect world flag" parser). |
| `BombConfig.java` | Nuke/explosion tunables: per-weapon blast radii (Gadget, Little Boy, Fat Man, Ivy Mike, Tsar Bomba, Fleija, N2 mine, MIRV, custom-nuke radius caps, etc.), explosion engine tunables (`blastSpeed`, `explosionAlgorithm`, `maxThreads`, `safeCommit`), fallout timing (`falloutRange`, `fChunkSpeed`, `falloutDelay`). Categories: `CATEGORY_NUKES`, `CATEGORY_EXPLOSIONS`. |
| `CompatibilityConfig.java` | Ore/structure/mob spawn-rate tables. Dozens of `HashMap<Integer,Integer>` (dimension id -> spawn rate/frequency) for every ore and structure type, `HashMap<Integer,Float> dimensionRad` (per-dimension background radiation), mod-compat mob radiation resistance/immunity maps (`HashMap<String,Float>`, `HashSet<String>`), `bedrockOreBlacklist`, peace-dimension whitelist/blacklist set + `isWarDim(...)` helper. Very large (~250 lines of config keys), all built via `CommonConfig.createConfigHashMap/HashSet`. |
| `JsonConfig.java` | Base helper for the mod's **hand-rolled JSON config format** (distinct from Forge `Configuration`/TOML). Resolves file path under `<world/instance data dir>/config/hbm/<filename>`, provides `startWriting`/`stopWriting` (raw `JsonWriter`) and `startReading` (raw `JsonObject` via Gson) primitives that every `*JsonConfig`/`*ConfigJSON` class in this package builds on. |
| `RunningConfig.java` | Base class for **runtime-editable, dynamically-typed** configs (`ClientConfig`, `ServerConfig`). Defines the generic `ConfigWrapper<T>` (holds a value, `get/set/update(String)` with manual `instanceof` dispatch for String/Float/Double/Integer/Boolean), plus static `readConfig`/`writeConfig` that (de)serialize a `HashMap<String,ConfigWrapper>` to a JSON file with an `"info"` comment field, sorted keys. Designed to be edited live via an in-game command (`/ntmclient`, `/ntmserver`) — values are looked up/mutated at runtime, not just at boot. |
| `ClientConfig.java` extends `RunningConfig` | ~24 client-only display/rendering runtime knobs (HUD offsets, gun animation toggles, heliostat beam limit, block-meta overlay, badges/healthbar HUD, etc). Reads/writes `hbmClient.json`. |
| `ServerConfig.java` extends `RunningConfig` | ~15 server-authoritative gameplay runtime knobs (damage-compat mode, per-mine-type damage floats, crate behavior, conveyor cram limits, autocal max clock). Reads/writes `hbmServer.json`. |
| `MachineConfig.java` | Small: RTG power scaling/decay toggle, `disableMachines` kill-switch, `holdDoorRedstone`, `crateByteSize` (max compressed NBT bytes for crates/lead boxes), and a `HashMap<String, IDoor.Mode> doorConf` parsed from a `String[]` of `modid:door_name:MODE` or `ALL:MODE` entries (custom mini-DSL parsing with `IDoor.Mode.valueOf`, silently ignoring bad entries). |
| `MachineDynConfig.java` | **Reflection-driven JSON config generator.** Instantiates one dummy instance per class implementing `IConfigurableMachine` (registry populated elsewhere in `AutoRegistry`), groups them by `getConfigName()` so multiple TE subclasses sharing a name collapse into one JSON section, reads existing `hbmMachines.json` into each machine via `readIfPresent(JsonObject)`, then rewrites the file with the merged/defaulted values via `writeConfig(JsonWriter)`. Depends on `com.hbm.tileentity.IConfigurableMachine` and `com.hbm.main.AutoRegistry` (out of this package's scope). |
| `MobConfig.java` | Large mob/spawn tuning class: Maskman, FBI raids, radiation elementals (delay/chance/amount/distance triples), duck button, mob gear/weapon toggles, and an extensive **Glyphid ("Deep Rock" style bug) spawn system**: hive spawn rate, soot-based spawn-chance int-triples per glyphid type (`int[3]`: base chance, soot modifier, min-soot), swarm scaling formula constants, and a whole "Rampant Mode" sub-toggle block that mutates several of the above fields when enabled (cross-field logic at end of `loadFromConfig`, including a `RadiationConfig.sootFogThreshold *= pollutionMult` cross-class write). |
| `PotionConfig.java` | Tiny: hazmat potion effect blacklist (`HashSet<String>`), jump-boost toggle, and a string-config-driven 3-state enum-ish `potionSickness` (0=off/1=normal/2=terraria) parsed from a free-text string. |
| `RadiationConfig.java` | Radiation/pollution tunables: fallout rain duration/intensity, fog thresholds, world-radiation block-modification toggles/thresholds, contamination toggles, railgun stats (damage/buffer/consumption - comment admits these are misplaced here), neutron activation, digamma GUI position, per-hazard-type disable flags (asbestos/blinding/coal/explosive/hydro/hot/cold/toxic), pollution/soot-fog/lead-poisoning toggles and thresholds. |
| `StructureConfig.java` | World-structure generation tunables: master `enableStructures` tri-state flag (parsed via `CommonConfig.parseStructureFlag`), min/max chunk spacing (with a validated correction if min>max), loot amount factor, and ~30 individual per-structure spawn *weight* ints (ruins A-J, planes, desert shacks, laboratory, lighthouse, oil rig, bunker, crane, spires, etc). |
| `ToolConfig.java` | Small: veinminer recursion depth/stone/netherrack toggles, and a fixed set of tool "ability" enable flags (hammer AoE, vein, luck, silk touch, auto-furnace, auto-shredder, auto-centrifuge, auto-crystallizer, mercury touch, explosion). |
| `VersatileConfig.java` | Not itself Forge-config-backed - a small **derived-logic helper** that reads other config classes (`GeneralConfig`, `PotionConfig`, `MachineConfig`) to compute composite runtime values: schrabidium ore chance (mode-dependent), potion-sickness application (adds a `PotionEffect` for `HbmPotion.potionsickness`), RTG decay eligibility, and decay-time constants that vary by 528/LBSM mode. |
| `WeaponConfig.java` | Small: radar range/buffer/altitude, CIWS accuracy modifier, missile-parts-drop toggle, and a handful of "drop causes explosion/effect" toggles (antimatter cell, singularity, rigged star, xen crystal, dead man's explosive) under `CATEGORY_DROPS`. |
| `WorldConfig.java` | World-gen tunables: bedrock-ore master toggle plus per-type bedrock spawn weights (glowstone/phosphorus/quartz), limestone spawn count, hematite/malachite/bauxite/sulfur-cave/asbestos-cave toggles, meteor system toggles+chances+duration, and crater-biome radiation constants (float, unlike the `double` versions in `RadiationConfig`/Neo reference — worth flagging). |
| `BedrockOreJsonConfig.java` | JSON-config (not Forge `Configuration`) for **per-dimension bedrock ore whitelist/blacklist**: `dimOres` (dim -> set of oredict names), `dimWhiteList` (dim -> whitelist/blacklist flag), `dimOreRarity` (dim -> rarity int). Ships hardcoded defaults for overworld (0), nether (-1), and a modded "Mining Dim" (-6). Writes `hbm_bedrock_ores.json` on first run via `JsonConfig`. |
| `CassetteJsonConfig.java` | JSON-config for **custom siren cassette tracks** (name/sound/type/color/volume), registers each into `ItemCassette.TrackType`. Depends on `com.hbm.items.machine.ItemCassette` and `com.hbm.lib.HBMSoundHandler` (both out of scope — item/sound registration, not config). Writes empty `hbm_siren_cassettes.json` template if absent. |
| `CustomMachineConfigJSON.java` | JSON-config allowing **server-defined custom multiblock machines** entirely from JSON: recipe key, localized name + per-locale localization map, fluid/item I/O slot counts and capacities, generator-mode flag, pollution cap, power/heat caps, a crafting-grid recipe (via `SerializableRecipe`), and a 3D grid of block "components" (relative x/y/z + block + allowed metadata) that the multiblock structure must match. Writes an example (`paperPress`) default. Deeply coupled to `com.hbm.blocks.ModBlocks`, `com.hbm.items.ModItems`, `com.hbm.inventory.*`, `com.hbm.main.CraftingManager` — all out of this package's scope but consumed here. |
| `FalloutConfigJSON.java` | The most complex file in the package (~1100 lines). JSON-config for the **fallout block-conversion table** used by nuclear fallout terrain effects: a builder-pattern `FalloutEntry` (immutable once built) describing match criteria (exact `IBlockState`, block-only, `Material`, ore-dictionary membership, opaque-only) plus weighted primary/secondary output block lists, min/max distance-percent window, chance, falloff curve start, and a "solid" flag. `initDefault()` seeds ~50 hardcoded conversion rules (wood->waste_log, leaves->waste_leaves, sand->trinitite, ore tiers -> Sellafield variants at 10 depth bands, etc). Has full JSON (de)serialization (`readEntry`/`write`) with property-copying/coercion logic (`copyProperty`, `coerceValue`) for preserving blockstate properties like `BlockRotatedPillar.AXIS` across conversions, and an ore-dictionary resolution/caching layer (`matchingOreIds`, `hasOreDictMatchers()`) for performance. Heavily `IBlockState`/`Material`/`OreDictionary`-coupled — this is 1.12 blockstate-property machinery, not a simple key-value config. |
| `ItemPoolConfigJSON.java` | JSON-config for **weighted loot pools** (`ItemPool`/`WeightedRandomChestContentFrom1710`), each entry an `[itemstack-array, min, max, weight]` tuple where the itemstack array itself supports NBT via `JsonToNBT.getTagFromJson`. Depends on `com.hbm.itempool.ItemPool` and `com.hbm.items.ModItems` (out of scope). |

## 2. Key responsibilities

The package has three structurally distinct sub-systems that must **not** be flattened into one design:

1. **Boot-time Forge `Configuration`/TOML-style static fields** (`GeneralConfig`, `BombConfig`, `CompatibilityConfig`,
   `MachineConfig`, `MobConfig`, `PotionConfig`, `RadiationConfig`, `StructureConfig`, `ToolConfig`, `WeaponConfig`,
   `WorldConfig`, plus the `CommonConfig` helper/category-registry). All follow the same shape: public static mutable
   fields with 1.12-style defaults, a `loadFromConfig(Configuration config)` method invoked once at mod init, heavy use
   of `CommonConfig.create*` helpers for the repetitive get+comment+return pattern, and occasional cross-field
   validation/derivation at the end of `loadFromConfig`.
2. **Runtime-editable JSON key-value configs** (`RunningConfig` + `ClientConfig`/`ServerConfig`). These are read
   *and rewritten* at arbitrary times via an in-game command, not just at boot — the "current value" is the source of
   truth for gameplay code, and the file is just a persistence mechanism. This is fundamentally different from
   `ModConfigSpec`, which does not support easy multi-type dynamic dispatch or command-driven live mutation the way
   `ConfigWrapper<T>` does.
3. **Ad-hoc JSON "data" configs** (`JsonConfig` + `BedrockOreJsonConfig`, `CassetteJsonConfig`,
   `CustomMachineConfigJSON`, `FalloutConfigJSON`, `ItemPoolConfigJSON`, and `MachineDynConfig`). These are not simple
   scalar options at all — they are structured/variable-length data (recipe-like tables, per-dimension maps, block
   conversion rules with game-object references like `IBlockState`/`Block`/`Material`/`SoundEvent`). They are much
   closer to "data-driven content" than to "mod options" and mostly depend on registries/classes far outside this
   package's scope (blocks, items, recipes, sounds, tile entities).

`VersatileConfig` is a fourth, small category: pure derived-logic helpers that read multiple config classes to
compute a composite value; it holds no config state of its own.

## 3. Cross-area dependencies

Out-of-scope classes referenced by files in this package (all confirmed to belong to other Phase-0 areas, not
ported here):

- `com.hbm.main.MainRegistry` — logger, `configDir`, `configHbmDir`, `proxy.getDataDir()`. **The config system depends
  on `MainRegistry` for its data-directory paths and logger**; this is a real coupling point for whoever wires up
  config loading in the port's `MainRegistry`/mod entrypoint.
- `com.hbm.main.AutoRegistry` (`configurableMachineClasses`) — used by `MachineDynConfig`.
- `com.hbm.main.CraftingManager` — used by `CustomMachineConfigJSON` to register a custom-machine crafting recipe.
- `com.hbm.tileentity.IConfigurableMachine` — the interface `MachineDynConfig` scans for.
- `com.hbm.interfaces.IDoor` (`IDoor.Mode` enum) — used by `MachineConfig.doorConf`.
- `com.hbm.render.GLCompat` — used by `GeneralConfig.loadFromConfig` to detect OpenGL 3.3 support (client-only
  rendering capability check gating several visual-effect configs).
- `com.hbm.potion.HbmPotion` — used by `VersatileConfig` (potion sickness effect).
- `com.hbm.blocks.ModBlocks`, `com.hbm.blocks.generic.BlockGlyphid(Spawner)` — used extensively by
  `FalloutConfigJSON.initDefault()` and by `CustomMachineConfigJSON`'s example writer.
- `com.hbm.items.ModItems`, `com.hbm.items.ItemEnums.EnumCircuitType` — used by `CustomMachineConfigJSON` and
  `ItemPoolConfigJSON`.
- `com.hbm.items.machine.ItemCassette`, `com.hbm.lib.HBMSoundHandler` — used by `CassetteJsonConfig`.
- `com.hbm.itempool.ItemPool`, `com.hbm.handler.WeightedRandomChestContentFrom1710` — used by `ItemPoolConfigJSON`.
- `com.hbm.inventory.OreDictManager`, `com.hbm.inventory.RecipesCommon` (`AStack`/`ComparableStack`/`OreDictStack`),
  `com.hbm.inventory.recipes.loader.SerializableRecipe` — used by `CustomMachineConfigJSON`.
- `com.hbm.inventory.recipes.PrecAssRecipes` — used by `GeneralConfig.trueExp()`.
- `com.hbm.util.ReferenceIntTuple`, `com.hbm.util.CompatDynamicTrees` — used by `FalloutConfigJSON`.

Consumers of this package (not read in detail, but referenced by name across the CE codebase and confirmed by the
Neo Edition reference) are effectively every other subsystem: world generation, tile entities/machines, weapons,
mobs, explosions, rendering. This package is a foundational, widely-depended-upon layer with almost no inbound
dependencies of its own (aside from the items above) — a good sign for porting it early, but it means the boot-time
config classes cannot be *fully* wired into gameplay logic in isolation; they can only be ported as pure data holders
in Phase 0, with call sites elsewhere updated as those other areas get ported.

## 4. Recommended NeoForge / Java 21 port plan

Verified against the Neo Edition reference (`upstream/neo-edition/src/main/java/com/hbm/config/{NtmConfig,CommonConfig,ServerConfig,ClientConfig}.java`),
which already implements this exact pattern against real NeoForge 21.1.228 APIs:

### 4.1 Boot-time scalar configs -> `ModConfigSpec`

- Package target: `com.hbm.config`.
- One `ModConfigSpec.Builder`-consuming class per CE config category, mirroring CE's file boundaries as closely as
  practical (`GeneralConfig`, `BombConfig`, `CompatibilityConfig` (scalars only — see 4.3 for its maps),
  `MachineConfig`, `MobConfig`, `PotionConfig`, `RadiationConfig`, `StructureConfig`, `ToolConfig`, `WeaponConfig`,
  `WorldConfig`). Each class:
  - Declares `public final BooleanValue / IntValue / DoubleValue / ConfigValue<String> / ConfigValue<List<? extends String>>`
    fields (NeoForge's `ModConfigSpec` nested value types — matches the Neo Edition reference exactly).
  - Takes a `ModConfigSpec.Builder` constructor parameter and calls `builder.push("category")` /
    `builder.comment(...).translation(...).define(...)`/`defineInRange(...)` / `builder.pop()`, matching CE's
    `CommonConfig.CATEGORY_*` groupings translated to lowercase snake/plain section names (Neo Edition uses
    `"general"`, `"nukes"`, `"explosion"`, `"tools"`, `"mobs"`, `"radiation"`, `"hazards"`, `"pollution"`, `"biomes"`,
    `"528"`, `"LESS BULLSHIT MODE"` — reuse these exact section names for continuity with the existing Neo port
    rather than inventing new ones).
  - **Preserve every CE option's original key name as the `comment(...)` text or as a trailing note**, per the port
    spec's instruction to keep CE option names as comments — e.g. `builder.comment("CE key: 3.00_gadgetRadius. Radius
    of the Gadget.").define("gadgetRadius", 150)`. The numeric CE prefixes (`3.00_`, `9.99_CE_03_`, etc.) existed only
    to force a fixed ordering in the old `.cfg` file; `ModConfigSpec` does not need them for ordering (TOML preserves
    declaration order) but they should still be recorded as a comment for anyone diffing against CE's `.cfg`.
  - Composite/mode toggles (`GeneralConfig.trueExp()`, `enable528()`/`true528()`, `MobConfig.trueRam()`,
    `VersatileConfig.*`) become plain static helper methods reading `.get()` off the `ModConfigSpec`-backed instance
    fields (see Neo Edition's `NtmConfig.true528()`/`enable528()` for the exact idiom), not part of the spec itself.
  - Register via `container.registerConfig(ModConfig.Type.COMMON, COMMON_SPEC)` (and `SERVER`/`CLIENT` as
    applicable) from a single aggregator class analogous to Neo Edition's `NtmConfig`. **Per the hard rule against
    editing `MainRegistry.java`**, this aggregator's `register(ModContainer container)` (or equivalent) method
    should be described to the integration step rather than wired in directly — recommend a new
    `com.hbm.config.HbmConfig` class exposing `public static void register(ModContainer container)`, called once from
    the mod's `@Mod` constructor.
  - `ModConfig.Type` split: CE's Forge `Configuration` files were monolithic per-mod (`hbm.cfg`), but the port
    should split along CE's already-implicit COMMON vs. CLIENT-only boundary — e.g. `GeneralConfig`'s rendering
    toggles (`useShaders`, `bloom`, `heatDistortion`, `flashlight*`, `depthEffects`, `instancedParticles`,
    `callListModels`) are genuinely client-only and belong in a `ModConfig.Type.CLIENT`-registered spec, while
    everything gameplay-affecting (nuke radii, spawn rates, mob toggles) belongs in `ModConfig.Type.COMMON`. This is
    a deliberate improvement over CE's single `hbm.cfg`, matching how Neo Edition already separates `CommonConfig`/
    `ServerConfig`/`ClientConfig`.
  - `int[]` array fields (`MobConfig.glyphidChance` et al.) have no direct `ModConfigSpec` list-of-int helper;
    port each as three separate `IntValue`s (base chance / soot modifier / min soot) or as a
    `builder.defineList("glyphidChance", List.of(50,-45,0), Integer.class::isInstance)`-style int list — recommend
    three named `IntValue`s per glyphid type for readability and IDE-discoverability, since the three numbers have
    distinct semantic meaning (this matches how the Neo reference favors explicit named fields over generic
    collections wherever the shape is fixed and small).
  - `Set<String>` fields (`leadSafeForgeContainerWhitelist`, `bedrockOreBlacklist`, `mobModRadimmune`,
    `mobRadimmune`, `potionBlacklist`, `peaceDimensions`) map to
    `builder.defineListAllowEmpty("key", List.of(...), String.class::isInstance)` (or the `Integer`-typed overload
    for `peaceDimensions`), read back into a `Set` at use-site via `new HashSet<>(configValue.get())`.
  - `HashMap<Integer,Integer>`/`HashMap<Integer,Float>`/`HashMap<String,Float>` fields
    (`CompatibilityConfig`'s ore/structure spawn-rate tables, `dimensionRad`, `mobModRadresistance`,
    `mobRadresistance`) have **no first-class `ModConfigSpec` map type**. Port these as
    `ConfigValue<List<? extends String>>` of `"key:value"` strings (mirroring CE's own `modid:item:meta` / `dimID:n`
    string-list convention already used for `leadSafeForgeContainerWhitelist` in CE) parsed into a `Map` once after
    `ModConfigEvent.Loading`/`Reloading` fires, exactly the way CE's own `createConfigHashMap` parses `"key:value"`
    string arrays today — this is the lowest-risk translation since the *file format* stays conceptually identical
    for anyone hand-editing it, only the outer container changes from Forge `Configuration` to TOML.
  - `NeoForge does not require validating range bounds manually` the way CE's `setDef`/`setDefZero` did — prefer
    `builder.defineInRange(name, default, min, max)` wherever CE only ever guarded against negative/zero values
    (this is strictly better: invalid values become impossible to set rather than being silently corrected+logged
    after the fact). Keep `CommonConfig.setDef`/`setDefZero`-style logging-based correction only for genuinely
    cross-field invariants that `defineInRange` cannot express (e.g. `StructureConfig`'s
    `structureMinChunks <= structureMaxChunks`, `MachineConfig`'s door-mode parsing) — apply these as post-load
    validation inside a `ModConfigEvent.Loading` listener, matching CE's pattern of validating in `loadFromConfig`.

### 4.2 Runtime-editable configs (`ClientConfig`/`ServerConfig`/`RunningConfig`)

- These are semantically **not** the same thing as the boot-time `ModConfigSpec` configs (see 4.5's open question) —
  CE's `ClientConfig`/`ServerConfig` here are a hand-rolled, command-mutable JSON key-value store, separate from
  Forge's `Configuration`-backed classes despite the confusingly similar names.
- Note the Neo Edition reference **already has classes named `ClientConfig`/`ServerConfig`** but they are the
  `ModConfigSpec`-based boot-time kind (see `CommonConfig.java` excerpt above, and its siblings) — i.e. Neo Edition
  collapsed CE's dual `ClientConfig`(`ModConfigSpec`-worthy options) + `ClientConfig`(`RunningConfig`-based
  in-game-editable options) into a single `ModConfigSpec`-based class per side, **dropping the live in-game-editable
  layer entirely**. This is a real behavior change versus CE (loses the `/ntmclient`/`/ntmserver` in-game editing
  commands), but it is consistent with idiomatic NeoForge (config files are user/pack-editable and reloadable via
  `/reload` or `/neoforge reload confirm` without needing a bespoke command), and it avoids re-implementing a whole
  parallel dynamic-typing config system. **Recommendation: follow the Neo Edition reference's precedent** — fold
  CE's `ClientConfig`/`ServerConfig` scalar options directly into the `ModConfig.Type.CLIENT` / `Type.SERVER`
  `ModConfigSpec` classes from 4.1, dropping `RunningConfig`/`ConfigWrapper<T>` and the custom JSON
  read/write/command-update plumbing entirely. Flag this explicitly to the user/lead as a deliberate scope
  reduction versus CE, not an oversight — the in-game editing UX would need to be reintroduced later (e.g. via
  NeoForge's built-in config screen support) if wanted.
- `ModConfig.Type.SERVER` (server-authoritative, per-world, synced to clients) is the correct target for CE's
  `ServerConfig` fields; `ModConfig.Type.CLIENT` for CE's `ClientConfig` fields — this is a strict improvement over
  CE's manual `hbmClient.json`/`hbmServer.json` split, which achieved the same separation by convention only.

### 4.3 Ad-hoc JSON "data" configs — explicitly OUT OF SCOPE for Phase 0

`JsonConfig`, `BedrockOreJsonConfig`, `CassetteJsonConfig`, `CustomMachineConfigJSON`, `FalloutConfigJSON`,
`ItemPoolConfigJSON`, and `MachineDynConfig` are **not simple mod options** — they are content-defining data tables
deeply coupled to blocks/items/recipes/sounds/tile-entities that do not exist yet in the port project. Per the
task's Phase 0 scope boundaries, these should **not** be ported as stubs in this phase. Recommendation for later
phases:

- `JsonConfig`'s thin file-path/`JsonWriter`/`JsonObject` helper is trivially portable as-is (no Forge/NeoForge API
  surface at all — pure Gson + `java.io.File`), and could be ported early as a shared utility once the port
  project has a `MainRegistry`-equivalent data-directory accessor, but only when the first consumer
  (`BedrockOreJsonConfig` et al.) is actually ported alongside it — porting it alone with no caller is dead code.
- `BedrockOreJsonConfig`, `CassetteJsonConfig`, `ItemPoolConfigJSON` should be ported together with their respective
  world-gen/sound/loot subsystems once those areas are in scope (their content is genuinely data, not config, and
  belongs conceptually with `data/hbm/loot_table`-style datapack content in a NeoForge idiom — worth raising with
  whoever owns loot/worldgen phases: NeoForge's native datapack JSON + `Codec`-based deserialization may be a better
  fit than hand-rolled Gson parsing, but that is a decision for the owning phase, not this one).
- `CustomMachineConfigJSON` and `MachineDynConfig` should be ported together with the tile-entity/machine framework
  phase, since both are inseparable from `IConfigurableMachine`/`AutoRegistry`/`CraftingManager`, none of which
  exist in the port project yet.
- `FalloutConfigJSON` should be ported together with the world-generation/fallout phase; it is entirely
  `IBlockState`/`Material`/`OreDictionary` machinery (1.12-era blockstate API) that needs a full redesign against
  Minecraft 1.21.1's `BlockState`/data-component model anyway, not a mechanical translation.

### 4.4 Data Components note

Per the hard rules, any NBT-on-`ItemStack` pattern in this package's scope must be flagged. Scanning the 23 files:
**no file in `com.hbm.config` itself reads or writes NBT on an `ItemStack` for persistence purposes.** The only NBT
touchpoints are:
- `CustomMachineConfigJSON.readConfig`: sets a `NBTTagCompound` with key `"machineType"` on a freshly-constructed
  `ItemStack` template used purely to register a crafting recipe output — this is recipe-definition data, not
  player-facing item state, and its real NBT-to-Data-Component translation work belongs to whichever phase ports
  `com.hbm.blocks.ModBlocks.custom_machine`/the custom-machine tile entity (Phase where `IConfigurableMachine` is
  ported), not to this config-research report. Flagging the key here for traceability: **`machineType` (String) ->
  likely a custom `DataComponentType<String>` or `DataComponentType<ResourceLocation>` on whatever item represents a
  placed/held custom machine block, once that item exists in the port.**
- `ItemPoolConfigJSON.readItemStack`/`writeItemStack`: parses/serializes NBT (`JsonToNBT.getTagFromJson`,
  `stack.getTagCompound()`) for **loot-pool item templates**, not live player-held items — again, this is
  content-authoring data (what NBT a spawned loot item should carry), not a live-item Data-Component migration
  target for this package. The relevant NBT keys are whatever the loot table entries themselves specify (arbitrary,
  data-driven) — no fixed key set to enumerate here. This work belongs to the loot/itempool phase.

No mapping table entry is needed in this report beyond the two callouts above, since this package does not itself
define or consume any fixed NBT key schema on live `ItemStack`s.

### 4.5 Risks / open questions

1. **`ClientConfig`/`ServerConfig` naming collision with Neo Edition's own classes of the same name but different
   design.** Confirmed above — Neo Edition already uses `ClientConfig`/`ServerConfig` for `ModConfigSpec`-based
   boot-time configs, not for CE's `RunningConfig`-based live-editable ones. The port project should follow Neo
   Edition's naming and design (this is also simpler and avoids maintaining two independent config-loading paths),
   but this needs explicit sign-off from whoever owns "in-game config editing" as a product decision, since it drops
   a real CE feature (`/ntmclient`, `/ntmserver` commands). Recommend raising this to the user/lead explicitly rather
   than deciding unilaterally.
2. **No first-class `ModConfigSpec` support for `Map<K,V>`.** `CompatibilityConfig`'s ~60 dimension-keyed spawn-rate
   maps and `RadiationConfig`-adjacent per-dimension radiation map need a string-list-of-`"key:value"` encoding
   (matching CE's own convention) parsed manually after config load. This is mechanical but tedious — recommend a
   single shared helper (e.g. `ConfigMapCodec.parseIntMap(List<String> raw)`) added to a `com.hbm.config.ConfigUtil`
   class alongside the `ModConfigSpec` classes, playing the same role `CommonConfig.createConfigHashMap` played in
   CE, so this isn't reimplemented ~15 times.
3. **`FalloutConfigJSON`'s `IBlockState`/`Material`/`OreDictionary` API surface is entirely obsolete in 1.21.1.**
   This is by far the largest single file in the package (~1100 lines) and is explicitly deferred to a later phase
   (see 4.3), but it should be flagged loudly now: it is not a "translate the syntax" job, it needs a genuine
   redesign against `BlockState`/registry-tag-based matching, and probably against NeoForge's/Mojang's native
   `Codec`/`MapCodec` (de)serialization instead of hand-written Gson `JsonElement` walking, since 1.21.1 has first-
   class support for that. Whoever picks up world-gen/fallout should budget real design time here, not just
   translation time.
4. **`GeneralConfig`'s `GLCompat.error`-based OpenGL-3.3-capability gating of shader/particle options** relies on a
   client-side rendering-capability probe (`com.hbm.render.GLCompat`) that has no obvious NeoForge/modern-OpenGL
   equivalent researched yet (out of this package's scope) — flagging for whoever owns the rendering-config
   intersection so the equivalent capability check (if still needed at all, given modern GPU/driver baselines) isn't
   silently dropped.
5. **`MobConfig`'s cross-class field mutation** (`RadiationConfig.sootFogThreshold *= pollutionMult` inside
   `MobConfig.loadFromConfig`, when Rampant Mode is enabled) is exactly the kind of imperative one-time mutation that
   does not translate cleanly to `ModConfigSpec`'s declarative `.get()`-based value access — recommend converting
   this into a small derived getter (`static double effectiveSootFogThreshold()`,
   analogous to `VersatileConfig`'s existing style) computed on demand rather than mutating another class's stored
   config value at load time, since `ModConfigSpec` values are meant to be read via `.get()` on demand, not
   overwritten programmatically after load.
6. **CE's numeric key-ordering prefixes (`"3.00_"`, `"9.99_CE_04_"`, etc.) have no equivalent need in TOML** (which
   preserves declaration order), but stripping them entirely loses a diffable link back to the CE `.cfg` for anyone
   maintaining both codebases side by side during the port. Recommendation in 4.1 (record as a comment) resolves
   this, but flagging for the port lead to confirm they want that level of CE-cross-reference detail preserved in
   comments, since it will make every `ModConfigSpec` file noticeably more verbose than the already-established Neo
   Edition reference style (which does not do this).
7. **`WorldConfig`'s crater-biome radiation fields are `float`, while `RadiationConfig`'s (and Neo Edition's
   `CommonConfig.CRATER_RAD` etc.) equivalent-purpose fields are `double`.** `ModConfigSpec.DoubleValue` is the only
   floating-point option NeoForge's `ModConfigSpec` exposes (no `FloatValue`); port all of these as `DoubleValue` and
   cast to `float` only at the point of use if a `float`-typed API downstream still requires it (e.g. biome color/
   rendering code) — do not invent a float-config type.

## 5. Files NOT covered by static `loadFromConfig` (no action needed, informational)

`VersatileConfig` has no `loadFromConfig` — it is pure derived logic and should simply be ported as static helper
methods that call `.get()` on whatever `ModConfigSpec` instance fields the other classes end up exposing, once those
exist. It has no config keys of its own to preserve.
