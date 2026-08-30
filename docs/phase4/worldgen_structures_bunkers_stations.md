# World-gen structures: bunkers, radio stations, crashed vertibird — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/world/Antenna.java` (238 lines) — the small phased "radio
  antenna" structure, read in full as the simplest example of the legacy schematic system.
- `upstream/hbm-ce/src/main/java/com/hbm/world/phased/{AbstractPhasedStructure,PhasedStructureRegistry,
  PhasedConstants}.java` (348/296/83 lines) — CE's own homegrown, already-modernized (records, `var`,
  pattern-matching `switch`) world-gen scheduler that every legacy structure in this report (`Bunker`,
  `Radio01`/`Radio02`, `Antenna`) actually runs on. **Not** vanilla 1.12 `StructureComponent`/
  `MapGenStructure` — see Headline finding.
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/ProceduralStructureStart.java` (246 lines) — the
  actual vanilla-`StructureStart`-based procedural-room engine (weighted component graph) that
  `BunkerComponents` (the *other*, jigsaw-registered "bunker") is built on.
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/component/Component.java` (1,213 lines total —
  package imports, class declaration/constructors (~first 90 lines), and the `ProceduralComponent`/
  `Weight`/`instantiateStructure` inner-type definitions read; the remaining ~1,100 lines are per-block
  placement helpers (`ConcreteBricks`, `MeteorBricks`, door/bed/lever placement utilities) shared by
  every `Component` subclass mod-wide, not re-read block-by-block here) — confirmed
  `abstract class Component extends net.minecraft.world.gen.structure.StructureComponent` (real vanilla
  1.12 API, the classic procedural piece, **not** the `template.TemplateManager` NBT-loading kind).
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/component/BunkerComponents.java` (1,012 lines) —
  read in full at the structural level: `BunkerStart` (the `ProceduralStructureStart` entry point),
  `registerComponents()` (`MapGenStructureIO` registration), and all 7 room classes' class
  declarations/imports/loot calls (`StartingHub`, `Corridor`, `BedroomL`, `FunJunction`, `BathroomL`,
  `Laboratory`, `PowerRoom`); the block-by-block bodies of each room (the bulk of the 1,012 lines) were
  not read line-by-line — they are pure `setBlockState` placement, structurally identical to the
  `Bunker.java`/`Antenna.java`/`Radio01.java` placement code detailed below.
- `upstream/hbm-ce/src/main/java/com/hbm/world/Bunker.java` (1,565 lines — header/imports, both
  `AbstractPhasedStructure` overrides (`checkSpawningConditions`, `isValidSpawnBlock`,
  `getHeightPoints`, `isCacheable`), and every `ItemPool`/loot-chest call site read via targeted grep +
  spot-read; the placement body itself is one `generate_r0` method of literal `setBlockState` calls, not
  read line-by-line beyond confirming it is a single fixed schematic with no rotation variants).
- `upstream/hbm-ce/src/main/java/com/hbm/world/Radio01.java` (5,101 lines) and `Radio02.java`
  (2,181 lines) — header/imports/class-shape and every `ItemPool` loot call site read (grep-confirmed:
  exactly 5 chest instances across both files — 4× `POOL_GENERIC`, 1× `POOL_ANTENNA` — no mob spawners,
  no missile items, no unique machine/reactor content anywhere in either file); confirmed this is **one
  monolithic static building** split across 6 sequential methods (`Radio01.generate_r0`/`r02`/`r03`/
  `r04` → `Radio02.generate_r00` → `Radio02.generate_r01`) purely because a single Java method cannot
  hold this many literal block-placement statements, not 6 structure variants.
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/NTMWorldGenerator.java` (430 lines, full) — the
  modern registration site: every `NBTStructure.registerStructure(...)`/`SpawnCondition` entry for
  `vertibird`, `crashed_vertibird`, `bunker` (yes, a *second*, unrelated "bunker"), `radio` (a *second*,
  unrelated "radio"), plus ~25 other `.nbt`-file structures and the full `meteor_dungeon` multi-pool
  jigsaw example.
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/nbt/{SpawnCondition,JigsawPiece,JigsawPool}.java`
  (114/42/63 lines, all full) — the complete "modern" (post-CE-rewrite) piece-selection API surface.
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/nbt/NBTStructure.java` (1,679 lines — registration/
  `MapGenStructureIO` hookup (confirmed `extends net.minecraft.world.gen.structure.MapGenStructure`,
  real vanilla base class), the height-resolution branch (`conformToTerrain` vs.
  `MathHelper.clamp(averageHeight, minHeight, maxHeight)`), and `GenStructure`'s entry points read
  directly; the ~1,400-line middle section — jigsaw-graph piece selection/rotation/bounding-box-overlap
  resolution — surveyed by grep for `loot`/height/rotation keywords only, not read function-by-function,
  since none of it is specific to bunker/radio/vertibird — it is the shared placement engine for all
  ~28 `.nbt` structures CE registers).
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/BlockWandLoot.java` (434 lines, full) — the
  dev-authoring-time "loot marker" block/`BlockEntity` (`TileEntityWandLoot`) that resolves a **string**
  pool name baked directly into a `.nbt` file into a real chest with real rolled loot at structure-paste
  time. This is the load-bearing mechanism answering "what loot does each structure spawn with" for
  every `.nbt`-based structure (see Headline finding).
- `upstream/hbm-ce/src/main/java/com/hbm/itempool/ItemPoolsLegacy.java` (238 lines, full) — all 7 pools
  used by the legacy schematic system, including `POOL_VERTIBIRD` (read in full, itemized below) and
  `POOL_ANTENNA`/`POOL_GENERIC`/`POOL_EXPENSIVE` (used by `Bunker`/`Radio01`/`Antenna`).
- `upstream/hbm-ce/src/main/java/com/hbm/itempool/ItemPool.java` (104 lines, full) — the
  `Map<String, ItemPool>` registry `TileEntityWandLoot` calls into by name, with a hardcoded
  backup pool for a missing/misspelled name.
- **The compiled `.nbt` structure assets themselves**, read directly (gzip-decompressed + ASCII-string
  extraction, not a Java source file): `src/main/resources/assets/hbm/structures/{vertibird.nbt,
  crashed-vertibird.nbt, radio_house.nbt}`. Confirmed real vanilla `StructureTemplate` NBT format
  (`size`/`blocks`/`entities`/`palette`/`state` tags) and, critically, the literal loot-pool-name and
  block-name strings baked into each file (see Headline finding — this directly answers "does the
  crashed vertibird contain missile salvage").
- `upstream/hbm-ce/src/main/java/com/hbm/lib/HbmWorldGen.java` (755 lines total — `generateStructures`/
  `generateAStructure`/`generateSellafieldPool` (~140 lines) read in full: the exact biome-gated dispatch
  rules for `Bunker.INSTANCE`/`Radio01.INSTANCE`/`Antenna.INSTANCE`; the ore/plant/mine generation
  methods above and below this block were skimmed only, out of this report's scope).
- `upstream/hbm-ce/src/main/java/com/hbm/config/{StructureConfig,CompatibilityConfig,GeneralConfig,
  WorldConfig}.java` (grepped for every field this report's structures actually read — confirmed real
  field names/defaults: `bunkerSpawnWeight=6`, `radioSpawnWeight=25`, `vertibirdSpawnWeight=6`,
  `vertibirdCrashedSpawnWeight=10`, `enableStructures=2`, `debugStructures=false`,
  `plainsNullWeight=4`/`oceanNullWeight=15` (per-`GenStructure` "spawn nothing" weight),
  `CompatibilityConfig.{radioStructure,antennaStructure,bunkerStructure}` = per-dimension
  `"0:1000"`/`"0:750"`/`"0:1000"` chunk-roll-chance maps, `GeneralConfig.enableDungeons=true`).
- `upstream/hbm-ce/src/main/java/com/hbm/main/StructureManager.java` (99 lines — header + the
  `vertibird`/`crashed_vertibird` `ResourceLocation` declarations read directly; the other ~40
  declarations for unrelated structures skimmed only) — confirms every `.nbt` asset lives under
  `assets/hbm/structures/...` (CE's own custom asset path, **not** vanilla's datapack
  `data/<ns>/structures/` convention).
- `upstream/hbm-ce/src/main/java/com/hbm/itempool/ItemPoolsComponent.java` (209 lines — grepped for pool
  names only, confirmed `POOL_MACHINE_PARTS`/`POOL_OFFICE_TRASH`/`POOL_FILING_CABINET`/`POOL_AMMO` live
  here, matching the strings found baked into `radio_house.nbt`; not read pool-entry-by-entry, since
  `radio_house` is confirmed a *different* structure from the task's "radio station," see Headline
  finding).
- `docs/phase3/missile_launch_infra.md`'s `SiloComponent` research (per this task's own pointer) —
  confirmed as the model for "a 1,408-line legacy `StructureComponent`," and re-confirmed here that
  `SiloComponent` is the **only** real consumer of `ItemPoolsLegacy.POOL_VERTIBIRD` in the entire
  codebase (grep-verified) — the pool named after vertibirds is actually the *silo's* loot pool, not the
  vertibird structures' (see Headline finding).
- **This port's own state**: `upstream/neo-edition/src/main/java/com/hbm/world/gen/{NtmPlacedFeatures,
  NtmConfiguredFeatures,NtmBiomeModifiers}.java` and `.../world/MeteoriteStructure.java` grepped/read —
  confirmed Neo Edition has ported **zero** natural pre-built structures (no `Structure`/`StructureType`
  registration exists anywhere in it); its only worldgen work is ore/vegetation `PlacedFeature`/
  `ConfiguredFeature`/`BiomeModifier` registration (unrelated to this report) and a from-scratch
  synchronous reimplementation of the *meteor impact* shape-carver (triggered at runtime by a landing
  meteor entity, not chunk-population-time structure gen — not a usable precedent for this report's
  question). This report's 1.21.1 `Structure`/`StructureType`/jigsaw claims are therefore **general
  Mojang-mapping knowledge, not verified against any real compiled 1.21.1 jar or a working example in
  this port** — flagged explicitly per this task's own environment constraints.

## Headline finding

The task frames this as one question — "how do CE's pre-built bunker/radio-station/crashed-vertibird
structures work, and what's the 1.21.1 replacement shape" — but the actual codebase answer is that
**"bunker" and "radio station" are each two unrelated, independently-configured structures that both
currently spawn in CE side by side**, and the vertibird question is answered by data no Java source
file contains at all:

1. **CE is mid-migration between two totally different world-gen engines, and both halves are live.**
   The **legacy half** (`Bunker`, `Radio01`+`Radio02`, `Antenna`, `Satellite`, `Spaceship`, ~10 more) is
   one giant fixed Java schematic per structure (literally thousands of `world.setBlockState(...)`
   calls), dispatched by a flat per-chunk dice roll in `HbmWorldGen.generateStructures` (biome check +
   `rand.nextInt(CompatibilityConfig.xStructure.get(dimID)) == 0`, no spacing/grid at all beyond "once
   per N chunks on average") and placed through CE's own **already-modernized** `AbstractPhasedStructure`
   scheduler (`PhasedStructureGenerator`/`DynamicStructureDispatcher` — an async, chunk-boundary-safe
   layout cache using `Long2ObjectOpenHashMap`s, Java records, and pattern-matching `switch`; this is
   **not** vanilla 1.12 `StructureComponent`/`MapGenStructure` at all, despite `Bunker extends
   AbstractPhasedStructure` looking superficially similar). The **modern half**
   (`vertibird`/`crashed_vertibird`/`radio_house`/`bunker` (again!)/~24 more) is a from-scratch,
   CE-authored jigsaw-alike (`NBTStructure`/`JigsawPiece`/`JigsawPool`/`SpawnCondition`) that loads real
   `.nbt` `StructureTemplate`-format files from `assets/hbm/structures/`, resolves height via
   `conformToTerrain`/`alignToTerrain`/a clamped average, and picks one structure per grid region by
   *weighted random plus an explicit "spawn nothing" null-weight* — functionally a hand-rolled precursor
   to vanilla's own post-1.14 jigsaw system, sitting on top of real vanilla `MapGenStructure`/
   `StructureStart`/`StructureComponent`/`MapGenStructureIO` for persistence. **Both engines are wired
   into world generation simultaneously** (`HbmWorld.java` registers both `new HbmWorldGen()` and `new
   NTMWorldGenerator()`), so today's game world can generate a legacy `Bunker.INSTANCE` schematic *and* a
   jigsaw `BunkerComponents`-procedural bunker *and* an `.nbt` `radio_house` office building *and* a
   giant legacy `Radio01` tower, all as unrelated content that happens to share an English name.
2. **"Does the crashed vertibird contain missile salvage?" is answered directly by the compiled `.nbt`
   asset, not by any Java file** — and the answer is **no**. Decompressing
   `crashed-vertibird.nbt`/`vertibird.nbt` and extracting their embedded ASCII strings shows both are
   real vanilla-format structure templates whose only "special" tile entity is
   `hbm:tileentity_wand_loot` (`BlockWandLoot`), CE's own dev-authoring loot marker: at structure-paste
   time it swaps itself for `minecraft:chest` and rolls loot from a **pool name baked into the file as a
   plain string** — `POOL_VERTIBIRD` and `POOL_EXPENSIVE` for both vertibird variants. `POOL_VERTIBIRD`
   (read in full from `ItemPoolsLegacy.java`) is T-51/T-45 power armor pieces, fusion cores, revolvers/
   ammo, uranium fuel, and nuke grenades — **Fallout-style wasteland salvage, zero `ItemMissile`-family
   items**. (A further, non-obvious twist: grepping the whole codebase for `POOL_VERTIBIRD`'s *only
   other* real usage finds it applied to `SiloComponent`'s loot chest, not the vertibird structures at
   all — the pool is named after vertibirds but is actually the *silo's* chest content by Java-source
   reference; the vertibird structures reach it only through the string embedded in their `.nbt` files.)
   The rest of both files' block palette (`hbm:deco_steel/titanium/tungsten`, `hbm:reinforced_glass`,
   `hbm:machine_battery`, `hbm:block_electrical_scrap`) is purely decorative wreckage dressing; no
   `EntityVertibird`/vehicle class exists anywhere in CE — "vertibird" is a set-dressing point-of-interest
   name, not a drivable aircraft.
3. **The task's own framing of the modern replacement ("jigsaw or single-piece `Structure` subclass +
   `.nbt` files, not 1.12 `StructureComponent`") is correct for the vertibird pieces specifically, but
   wrong if applied uniformly** — `Bunker`/`Radio01`/`Antenna` are not `StructureComponent`s of any kind
   (1.12 legacy or otherwise); they are literal 1,500–5,100-line sequences of hardcoded block placement
   with **no structure-template asset backing them at all**, meaning there is no `.nbt` file to simply
   re-point at a modern `StructureTemplateManager` — the actual 1.21.1 port work for these three is "run
   this schematic in a dev world once, save it with a structure block, ship the resulting `.nbt`," not a
   pure code translation (see Key design/API decisions).

## Phase-4-safe scope

All line counts are from the CE files actually read (see Sources).

### Vertibird / crashed vertibird (2 `.nbt` structures + shared plumbing) — cleanest of the three, ready to model directly on vanilla's own jigsaw/`RuinedPortal`-style pattern

| Structure | Registration (`NTMWorldGenerator`) | Biome gate | Height | Loot |
|---|---|---|---|---|
| `vertibird` | `JigsawPiece("vertibird", StructureManager.vertibird, heightOffset=-3)`, `spawnWeight = StructureConfig.vertibirdSpawnWeight` (default **6**) | `!isInvalidBiome(biome) && BiomeDictionary.hasType(biome, Type.SANDY)` (desert/beach/mesa-family biomes) | No `minHeight`/`maxHeight` override (defaults 1–128) → `NBTStructure`'s clamped-average-terrain-height path, then shifted **3 blocks down** by `heightOffset` | `POOL_VERTIBIRD` + `POOL_EXPENSIVE` via one `hbm:tileentity_wand_loot` block each (confirmed from the decompiled `.nbt`) |
| `crashed_vertibird` | `JigsawPiece("crashed_vertibird", StructureManager.crashed_vertibird, heightOffset=-10)`, `spawnWeight = StructureConfig.vertibirdCrashedSpawnWeight` (default **10**) | identical `SANDY` gate | identical average-terrain path, shifted **10 blocks down** (embedded in an impact-crater-style dip) | identical pool pair |

Both are single-piece (`SpawnCondition.structure` set, not `pools`/`startPool` — per `SpawnCondition`'s
own doc comment, "if defined, will spawn a single jigsaw piece, for single nbt structures") — there is
no piece-to-piece jigsaw graph here at all, just one fixed `.nbt` template with a vertical offset and a
biome/weight gate. This is the simplest of the three structure families to reason about and the
cleanest match to a modern vanilla mechanism (see Key design/API decisions).

### Radio stations — two unrelated structures sharing one English name (~7,300 lines + 1 `.nbt` file)

| Name in CE | Real identity | Mechanism | Biome gate / rarity | Loot |
|---|---|---|---|---|
| **"radio" (giant tower)** — what the task almost certainly means | `Radio01`(5,101 lines)+`Radio02`(2,181 lines), `AbstractPhasedStructure`, dispatched by `HbmWorldGen` | One monolithic static schematic, split into 6 sequential methods purely for javac method-size limits (`Radio01.generate_r0/r02/r03/r04` → `Radio02.generate_r00/r01`); non-cacheable (`isCacheable()=false`, "due to `Library.getRandomConcrete(rand)`" — its concrete-brick palette is randomized per spawn) | `biome.getDefaultTemperature() >= 0.8F && biome.getRainfall() > 0.7F` (warm+wet, e.g. jungle-ish), gated by `GeneralConfig.enableDungeons` + world's "generate structures" flag; **flat per-chunk roll**, `CompatibilityConfig.radioStructure` default `"0:1000"` (dimension 0 only, ~1-in-1000 chunks) | 5 chests total across both files: 4×`POOL_GENERIC`, 1×`POOL_ANTENNA` — no mob spawners, no reactors, no unique machines, no missile items anywhere in either file (grep-confirmed) |
| **"radio" (small office building)** — `NTMWorldGenerator`'s `"radio"` key | `radio_house.nbt`, a real `.nbt` `StructureTemplate` | `JigsawPiece("radio_house", StructureManager.radio_house, heightOffset=-6)`, `spawnWeight = StructureConfig.radioSpawnWeight` (default **25**) | `NTMWorldGenerator::isFlatBiome` (plains/desert/ice-plains-family, low height variation) | `POOL_AMMO`/`POOL_FILING_CABINET`/`POOL_MACHINE_PARTS`/`POOL_OFFICE_TRASH` (all `ItemPoolsComponent`, the *modern* pool set) via `hbm:tileentity_wand_loot`; also contains `hbm:mine_ap`/`hbm:mine_he` (armed booby-trap landmines), a bobblehead collectible, filing cabinets, a satellite-dish receiver TE, a tape recorder — office/civilian-scavenger dressing, not a military radio complex |

`Antenna` (238 lines, read in full) is a third, much smaller phased structure — a 3×3×21 steel radio
mast with one `POOL_ANTENNA` chest — spawned independently again (`biome.getDefaultTemperature() >= 0.4F
&& biome.getRainfall() <= 0.6F`, `CompatibilityConfig.antennaStructure` default `"0:750"`). It shares
`POOL_ANTENNA` with `Radio01` (both use `ItemPoolsLegacy`) but is registered, dispatched, and spawned
completely independently of both "radio" structures above.

### Bunker — two unrelated structures again (~2,800 lines across both)

| Name in CE | Real identity | Mechanism | Biome gate / rarity | Loot |
|---|---|---|---|---|
| **`Bunker.INSTANCE`** (`com.hbm.world.Bunker`, 1,565 lines) | `AbstractPhasedStructure`, dispatched by `HbmWorldGen` | Single fixed schematic (one `generate_r0` method, no rotation variants), placed ~25 blocks underground (`y += 1` then all placement at `y + j - 25`), `isCacheable()=false`, requires all 4 footprint corners to sit on `GRASS`/`DIRT`/`STONE`/`SAND`/`SANDSTONE` (`checkSpawningConditions`) | **No biome restriction at all** — spawns in any biome `HbmWorldGen`'s dungeon-gate allows; flat per-chunk roll, `CompatibilityConfig.bunkerStructure` default `"0:1000"` | `POOL_EXPENSIVE` (main stash chest), 3×`POOL_GENERIC`, 1×`POOL_ANTENNA` (unexplained reuse — likely a copy-paste from `Antenna`'s authoring), plus a placed `ModBlocks.geiger` (Geiger counter) block next to the main stash — the only structure of the three with explicit radiation-hazard set-dressing |
| **`BunkerComponents.BunkerStart`** (`com.hbm.world.gen.component`, 1,012 lines) | `ProceduralStructureStart` (real vanilla `StructureStart` subclass) + 7 `Component` (real vanilla `StructureComponent` subclass) room types: `StartingHub`, `Corridor`, `BedroomL`, `FunJunction`, `BathroomL`, `Laboratory`, `PowerRoom` | Weighted random-walk room graph (`ProceduralStructureStart.buildStart`/`getNextValidComponent`): starts from `StartingHub`, then randomly attaches rooms by weight (`Corridor` 6/limit 3, `BedroomL` 5/4, `FunJunction` 10/3, `BathroomL` 5/2, `Laboratory` 7/2, `PowerRoom` 5/1) until `sizeLimit` (7+`rand.nextInt(6)` components) or `distanceLimit` (40 blocks) is hit; registered with vanilla `MapGenStructureIO.registerStructureComponent` for NBT persistence | Registered as `NTMWorldGenerator`'s `"bunker"` `SpawnCondition`: `canSpawn = !isInvalidBiome(biome)` (i.e. **also no real restriction**, just excludes ocean/river/void), `spawnWeight = StructureConfig.bunkerSpawnWeight` (default **6**), competing in the same weighted-per-region-cell lottery as ~25 other `.nbt` structures (vertibird, aircraft carrier, factory, etc.) | `ItemPoolsComponent`/`ItemPoolsLegacy.POOL_ANTENNA` (one room's chest, flagged `/*TODO change */` in CE's own source — CE's own authors consider this a placeholder) via `Component`'s `generateInvContents` helper, a different loot API than the `.nbt` wand-loot path since this bunker is placed by literal Java code, not a pasted template |

Both bunkers can independently spawn in the same world with **zero coordination or exclusion between
them** — CE's own comment `//TODO more rooms for more variety` on `BunkerComponents.registerComponents()`
suggests the room-based bunker is the newer, actively-being-expanded one, while `Bunker.INSTANCE` reads
as the older, frozen-in-place original.

## Deferred scope

- **`PhasedStructureGenerator`/`DynamicStructureDispatcher`** (the async scheduler `AbstractPhasedStructure.generate()`
  hands off to) — not read function-by-function here (confirmed it resolves real placement Y from each
  structure's `getHeightPoints()` corner list via a cached-min-height mechanism, and defers actual block
  writes past the originating chunk-population call to avoid vanilla's classic chunk-boundary corruption
  bug). This is shared plumbing behind **every** phased structure CE has (`Satellite`, `Spaceship`,
  `Sellafield`, `DesertAtom001-3`, `Dud`, `Geyser`, `OilBubble`, `GlyphidHive`, dungeon family, etc. —
  ~25 total per `PhasedStructureRegistry`'s static registration block), not specific to
  bunker/radio/vertibird. Whoever ports the *scheduler itself* (as opposed to individual structure
  content) should treat it as one shared prerequisite, not re-derive it per structure-report.
- **`NBTStructure`'s ~1,400-line jigsaw-graph piece-selection/rotation/overlap-resolution core** — read
  only for the height-resolution branch and registration hookup in this report; the general
  multi-piece pool-walking algorithm (relevant to `meteor_dungeon`'s 9-pool jigsaw graph, `aircraft_carrier`,
  `factory`, etc., none of which this report covers) is out of scope here since `vertibird`/
  `crashed_vertibird`/`radio_house` are all single-piece, not pool-based.
- **`INBTBlockTransformable`/`INBTTileEntityTransformable`** (314/13 lines, not read line-by-line) — the
  per-blockstate and per-`TileEntity` rotation-transform interfaces every rotatable block/TE in a `.nbt`
  piece must implement (confirmed `BlockWandLoot.TileEntityWandLoot implements INBTTileEntityTransformable`,
  used to re-derive `placedRotation` after the structure is rotated at paste time). Needed by whoever
  ports the general `.nbt`-piece placement pipeline; not specific to this report's three structures.
- **`CivilianFeatures.java`/`OfficeFeatures.java`/`MapGenNTMFeatures.java`** (1,092/576/147 lines, not
  read) — these back the `"features"` `SpawnCondition` (a *fourth*, unrelated small-decoration structure
  family — crashed cars, mailboxes, etc.), which the task did not name and this report does not claim.
  Flagged only because `BunkerComponents.java` imports `Component.ConcreteBricks` from the same package
  family, so an implementer skimming imports could mistake them for bunker-relevant.
- **`ItemPoolsComponent`/`ItemPoolsSingle`/`ItemPoolsRedRoom`/`ItemPoolsSatellite`/`ItemPoolsPile`/
  `ItemPoolsC130`/`ItemPoolsVendingMachine`** (the other 7 of 8 `ItemPool.initialize()` calls) — only
  `ItemPoolsLegacy` (full) and `ItemPoolsComponent` (pool names only) were read here, since those are
  the two actually referenced by this report's three structure families. Porting the **full item catalog**
  behind every pool (hundreds of `ModItems`/`ModBlocks` entries) is itself gated on Phase 1-3's item/
  block registries already existing — treat as content work layered on top of the structure-placement
  mechanism this report scopes, not blocking it.
- **`CraterBiomeBase`/`BedrockOilDeposit`/`BedrockOre`/world-gen ore and biome features** — already
  flagged as Phase 4 TODOs elsewhere (`OilChainBlocks`, `BlockFissureBomb` per this task's own framing)
  and confirmed unrelated to bunker/radio/vertibird specifically; not re-derived here.
- **`ItemLock`/`TileEntityLockableBase` integration in `BlockWandLoot`** (the lock-code/lockpick-chance
  fields a wand-loot block can bake into its resulting chest) — this port's existing lock/security
  system (used elsewhere already per Phase 2/3 reports) is the real dependency; not detailed further
  here since none of the three `.nbt` files inspected in this report actually use a locked chest.

## Key design/API decisions

Everything below is this report's own synthesis for the 1.21.1 target — **explicitly flagged as
inferred from well-established Mojang-mapping knowledge, not verified against a real compiled jar or
against any working example in this port**, per this task's environment constraints (Neo Edition has
ported zero natural structures to check against, see Sources).

- **`vertibird`/`crashed_vertibird` map cleanly onto a single-piece, non-jigsaw `Structure` subclass in
  the style of vanilla's own `RuinedPortalStructure`/`RuinedPortalPiece`** — vanilla already ships a
  precedent for "pick one of several `.nbt` templates, apply a biome/rarity gate, and place it at a
  vertically-offset/terrain-following position via a small enum of placement modes
  (`VerticalPlacement`: `ON_LAND_SURFACE`, `PARTLY_BURIED`, `UNDERGROUND`, etc.)" — which is a closer
  match to CE's `heightOffset`/`conformToTerrain`/`alignToTerrain` trio than a full jigsaw-pool `Structure`
  would be, since both CE structures are single fixed pieces, not multi-piece graphs. This needs a
  registered `StructureType`/`Structure` (data-driven via a `structure_set`/`structure` datapack pair)
  and the `.nbt` assets re-saved as real `StructureTemplate`s (they already decompress as valid
  vanilla-format NBT, so the raw geometry likely ports with only a block-ID remap, not a re-author).
- **CE's own per-region weighted-random-plus-null-weight piece lottery (`NBTStructure.GenStructure`,
  `plainsNullWeight`/`oceanNullWeight`) is functionally the same idea as vanilla's post-1.16
  `structure_set` JSON (`RandomSpreadStructurePlacement` with `spacing`/`separation`/`frequency`/`salt`)**
  — recommend representing each of the ~28 `.nbt` structures (including `vertibird`/`crashed_vertibird`/
  `radio_house`) as one vanilla `structure_set` entry with real spacing/separation/frequency values
  chosen to approximate CE's `spawnWeight`-relative-to-total-pool ratios, rather than hand-porting CE's
  own `GenStructure` grid algorithm into NeoForge Java code — this is the one place a straight "port the
  Java 1:1" instinct is likely wrong, since the datapack-driven vanilla mechanism is strictly more
  native and more moddable (`StructureConfig`'s existing per-structure config ints could still drive a
  `Codec`-loaded override, matching this port's Phase 0 config-porting precedent, without reinventing
  the placement algorithm itself).
- **`BlockWandLoot`'s dynamic string-keyed `ItemPool` resolution (`.nbt` file stores `poolName` as a
  plain string, resolved at paste time via `ItemPool.pools.get(name)`) has two legitimate 1.21.1
  translations, and this report does not pick one**: (a) keep the exact CE mechanism — a real ported
  `BlockEntity` that self-replaces on paste and rolls a named weighted pool (straightforward, preserves
  CE's "designer places a marker block in a dev world" authoring workflow, needs an
  `INBTTileEntityTransformable`-equivalent hook wired into whatever pastes the `StructureTemplate`); or
  (b) drop the marker block entirely and bake a real vanilla `LootTable` resource-location + seed
  directly onto the chest's saved `BlockEntity` NBT in the template (`minecraft:loot_table`/
  `LootTableSeed`, vanilla's own native mechanism since 1.9, confirmed already present as a concept via
  this report's own `ItemPool.writeLootTable`/`ILootContainerModifiable` finding — CE apparently already
  has *a* code path for real vanilla loot tables, just unused by these three structures). Recommend (b)
  for new/re-authored `.nbt` assets (simpler, zero custom `BlockEntity`, matches how vanilla structures
  ship loot) but (a) is a faster, more literal port if minimizing re-authoring work is the priority —
  this is a product/effort trade-off, not something this report can resolve unilaterally.
- **`Bunker`/`Radio01`+`Radio02`/`Antenna` have no `.nbt` asset to re-target at all** — the correct port
  path is almost certainly "run CE's original 1.12 code once in a debug world (or read off CE's own
  compiled JAR/dev environment), capture the result with a vanilla structure block, ship it as a `.nbt`
  file," **not** transliterating ~7,300 combined lines of `setBlockState` calls into NeoForge Java. This
  is an asset-authoring task, not an API-shape decision, and needs a build-time or one-off tool step
  this report does not scope further — flagged clearly so it isn't mistaken for "just port the Java
  class" the way `Antenna`'s small 238-line size might suggest is feasible by hand.
- **The bunker/radio naming collision is a real product decision, not a technical one**: does the port
  ship both `Bunker.INSTANCE`'s fixed schematic *and* `BunkerComponents`' procedural room-graph as two
  named structures (matching CE's current, seemingly-unintentional, duplication), retire one in favor of
  the other (CE's own `//TODO more rooms for more variety` comment suggests `BunkerComponents` is the
  actively-favored direction), or merge them (e.g. use the procedural room-graph's `PowerRoom`/
  `Laboratory` variety but the fixed schematic's underground Geiger-counter/`POOL_EXPENSIVE` stash room
  as one guaranteed component)? Same question for `Radio01`'s tower vs. `radio_house`'s office building
  — they don't even share a loot-pool family (`ItemPoolsLegacy` vs. `ItemPoolsComponent`), so they read
  as CE simply never having decided to deprecate the old ones once the `.nbt` pipeline existed. Flagged
  exactly like `missile_launch_infra.md`'s Soyuz-launch-incompleteness case: a genuine CE-parity
  ambiguity, not a research gap this report can close.
- **Entity registration/loot-table Codec plumbing needed for any of this is already established
  elsewhere in this port** (per this task's own framing: `ChunkRadiationManager`'s ambient-radiation
  query, `HbmPotion`, `ContaminationUtil` are Phase 3/4 foundations already real) — nothing in this
  report's three structures needs a new capability or attachment; they are pure world-gen placement +
  item-pool loot, no runtime entity-attachment interaction at spawn time (confirmed: no
  `HbmLivingProps`/contamination/rad-immunity call anywhere in `Bunker`/`Radio01`/`Radio02`/`Antenna`/
  `BunkerComponents`, grep-verified against the class list read).

## Open questions / risks

- **No compiled 1.21.1 Minecraft/NeoForge jar exists in this sandbox, and Neo Edition has ported zero
  natural structures** — every claim in this report about the real `Structure`/`StructureType`/
  `structure_set`/`StructureTemplateManager`/`RuinedPortalStructure` API shape is well-established
  Mojang-mapping knowledge from general familiarity with the modern structure-gen system, **not**
  verified against real code anywhere in this repository. Whoever implements this should confirm the
  exact `Codec`/registration shape against a real 1.21.1 environment before committing to the
  `structure_set` design recommended above.
- **Whether both "bunker" structures and both "radio" structures should ship is unresolved** (see Key
  design/API decisions) — this is the single biggest scope-sizing risk in this report: shipping all four
  legacy-plus-modern pairs means porting ~10,000 combined lines of largely-redundant content (two
  bunkers, two radios, `Antenna`) versus picking one representative per name roughly halving that.
- **`Bunker`/`Radio01`/`Radio02`/`Antenna` have zero rotation support** (single `generate_r0`/fixed-facing
  methods, no `EnumFacing`-parameterized variants) — confirmed by reading their `buildStructure`
  overrides, which each call exactly one `generate_rN(..., 0, 0, 0)` with no facing parameter. Every
  instance of these four structures in a live CE world faces the same fixed cardinal direction
  regardless of terrain — worth deciding whether the port preserves this (simplicity, exact parity) or
  adds real rotation once the content is re-captured as a `.nbt` template (trivial to add for free at
  that point, since vanilla's own template placement natively supports `Rotation`/`Mirror`).
  `BunkerComponents`' room graph, by contrast, already rotates per-component
  (`this.setCoordBaseMode(EnumFacing.byIndex(rand.nextInt(4)))` in `Component`'s constructor) — another
  asymmetry between the two "bunker" implementations.
- **`POOL_ANTENNA`'s reuse inside `Bunker.java` (line 781, a bunker chest rolling antenna-flavored loot)
  and `BunkerComponents`' own explicit `/*TODO change */` comment on its `POOL_ANTENNA` chest** both read
  as CE's own authors flagging unfinished loot-pool assignment, not deliberate design. Faithful-parity
  porting means carrying this apparent copy-paste forward as-is; "fixing" it (giving bunkers a real
  bunker-flavored pool) would be going beyond CE parity — flagged so it's a deliberate choice either way,
  matching this report's house style of surfacing CE's own unfinished edges rather than silently
  resolving them.
- **The exact Y at which `PhasedStructureGenerator` finalizes placement (surface-follow via
  `getHeightPoints`'s cached-min-height, vs. the fixed-offset arithmetic each structure's
  `buildStructure` does internally, e.g. `Bunker`'s `y + j - 25`) was not traced end-to-end in this
  report** (deferred — shared scheduler plumbing, see Deferred scope). Whoever ports the scheduler should
  confirm whether "surface height at the structure's footprint corners" and "structure's own internal
  y-25 offset" compose the way CE intends (i.e., does `Bunker` end up buried under ~25 blocks of terrain
  reliably, or could shallow/uneven terrain expose part of it?) — a real gameplay-visible edge case this
  report flags but does not resolve.
