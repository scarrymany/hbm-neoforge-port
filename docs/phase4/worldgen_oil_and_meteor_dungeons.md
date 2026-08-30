# Oil-deposit world-gen & meteor dungeons — Phase 4 research

## Headline finding

The task frames this as roughly two comparably-sized features ("oil-deposit world-gen" and "meteor
dungeon structures"). **Reading the real CE source shows they are wildly different in scope, and
there are actually *three* meteor-related systems in CE, not two:**

1. **Oil-deposit world-gen** (`OilBubble`/`OilSandBubble`/`BedrockOilDeposit`, ~370 CE lines combined)
   is genuinely small. There is **no abstract "richness" or per-chunk data record at all** — CE bakes
   real `ModBlocks.ore_oil`/`ore_bedrock_oil` block states directly into terrain in an ellipsoid/diamond
   blob, and every other system (the derrick's BFS search, `ItemOilDetector`'s scan, the fracking
   tower's `canSuckBlock`) queries that placement by **plain `Block` identity comparison against a real
   `BlockPos`** — nothing more abstract exists to port. This part of the task's framing needs a small
   correction (there is no separate "richness value" to design), but the underlying feature is close to
   what was expected.
2. **`com.hbm.world.MeteoriteStructure` / `com.hbm.world.Meteorite`** (50 + 625 CE lines) — the class
   the task's own instruction ("search for 'Meteor'-named structure/component classes") most naturally
   finds first — is **not a dungeon**. It is a small, passive, ambient world-gen feature (1-in-200
   chunks in dim 0) that carves a solid geometric ore/rock blob (sphere/star/box shapes filled with
   `block_meteor*`/`ore_meteor` blocks) into terrain wherever it lands. No loot chests, no mobs, no
   rooms — just a mineable resource formation, functionally a cousin of `OilBubble`.
3. **A real, separate, and much larger "meteor dungeon"** (`com.hbm.world.gen.nbt.NBTStructure` +
   `JigsawPiece`/`JigsawPool`/`SpawnCondition` + `com.hbm.main.StructureManager` +
   `com.hbm.world.gen.component.Component`, ~3,540 CE lines combined, registered as
   `"meteor_dungeon"` in `com.hbm.world.gen.NTMWorldGenerator`, 430 lines) **is exactly the
   "pre-built meteor crash site with loot" structure the task hypothesized might exist** — a custom,
   fully modular NBT-schematic jigsaw dungeon with 38 real `.nbt` room files, a dragon boss room, four
   treasure-vault variants, 16 small loot-vault variants, and monster-spawner blocks that spawn
   `EntityCyberCrab` (the same legacy-gun-projectile mob Phase 3's `gun_framework.md` already flagged as
   still-live). It shares **zero code** with `MeteoriteStructure`/`Meteorite` and is **not the same
   engine** as oil-deposit world-gen either — it rides on a completely different, much bigger custom
   jigsaw-structure substrate that also generates ~30 unrelated CE buildings (bunkers, ruins, an oil
   rig, a lighthouse, crashed planes, factories, radio houses, ...). This is the real find the task was
   fishing for, and it is dramatically bigger and more entangled with other content than "port two
   world-gen features" suggested.
4. **The crater biome (`BiomeGenCraterBase`) is a fourth, wholly separate system** — not oil, not
   either meteor system. It is three `Biome` subclasses that are never placed by ordinary chunk
   decoration at all; they only get **dynamically painted onto already-generated terrain** by
   `EntityFalloutRain.getBiomeChange()` (already researched and deferred by
   `docs/phase4/fallout_rain_and_effects.md`) after a nuke strike. `BlockFissureBomb` only *reads*
   `world.getBiome(...) instanceof BiomeGenCraterBase` as a boolean flag; it does not create the biome.

Finally, **`upstream/neo-edition` already has real, compiling, 1.21.1 NeoForge implementations of the
oil-deposit half of this area** (`OilBubbleFeature`, `BedrockOilDepositFeature`, plus the full
`Feature`→`ConfiguredFeature`→`PlacedFeature`→`BiomeModifier` registration chain) — not for behavior
(per this task's rules, never copied), but as **directly confirmed, load-bearing evidence of the real
1.21.1 API shape**, cited throughout this report. Tellingly, neo-edition's own `MeteoriteStructure.java`
ports only `Meteorite`'s block-shape *logic* as a bare utility class — it is **not** registered as a
`Feature` anywhere in neo-edition's own world-gen files, meaning even the reference port has not wired
ambient passive meteorite placement into 1.21 chunk generation. And neo-edition has **no meteor-dungeon
equivalent at all** — no jigsaw engine, no `.nbt` room files ported, nothing.

## CE source actually read (files and exact line counts)

| File | Lines | Role |
|---|---|---|
| `world/OilBubble.java` | 197 | Underground oil-deposit blob + surface staining, phased structure |
| `world/OilSandBubble.java` | 89 | Desert-surface `ore_oil_sand` patch, phased structure |
| `world/feature/BedrockOilDeposit.java` | 80 | Bedrock-layer `ore_bedrock_oil` diamond region, phased structure |
| `world/feature/OilSpot.java` | 118 | Surface staining helper (**already ported** in this port, Phase 2) |
| `items/tool/ItemOilDetector.java` | 103 | Player-facing oil scanner: raw block-identity column scan |
| `tileentity/machine/oil/TileEntityOilDrillBase.java` | 334 | Abstract derrick/pumpjack/fracking-tower base: BFS suck + drill-pipe logic |
| `tileentity/machine/oil/TileEntityMachineOilWell.java` | 201 | Derrick concrete class |
| `tileentity/machine/oil/TileEntityMachinePumpjack.java` | 239 | Pumpjack concrete class |
| `tileentity/machine/oil/TileEntityMachineFrackingTower.java` | 246 | Fracking tower concrete class (adds bedrock-oil tapping) |
| `world/phased/AbstractPhasedStructure.java` | 347 | Base class for oil bubble/bedrock-oil/meteorite/bedrock-ore/many other small features (full read) |
| `world/phased/{PhasedStructureRegistry,DynamicStructureDispatcher,PhasedStructureGenerator,PhasedEventHandler,PhasedConstants,DimensionState,IPhasedStructure}.java` | 2,673 (skim/grep, not full read) | Chunk-scheduling substrate the above all depend on |
| `world/feature/BedrockOre.java` | 194 | Sibling (not oil) bedrock ore-vein feature — confirms `ore_bedrock_block`'s real placement site |
| `world/MeteoriteStructure.java` | 50 | Ambient meteorite world-gen entry point (phased structure) |
| `world/Meteorite.java` | 625 | Shared block-shape generator, called by both `MeteoriteStructure` (passive) and `EntityMeteor` (live strike, other agent's scope) |
| `blocks/generic/BlockMeteorOre` / `ModBlocks.java` meteor block lines | — (grepped) | `block_meteor*`/`ore_meteor` block registrations referenced by `Meteorite.java` |
| `world/biome/BiomeGenCraterBase.java` | 111 | The 3 crater biome subclasses |
| `blocks/bomb/BlockFissureBomb.java` | 49 | CE original — confirmed cross-dependency on `ore_bedrock_oil` (this report's area) and `BiomeGenCraterBase` |
| `blocks/generic/BlockFissure.java` | 105 | `ore_volcano`'s real class — a ticking lava-emitting machine block, not a plain decorative block |
| `world/gen/NTMWorldGenerator.java` | 430 | Registers all ~30 CE NBT-jigsaw structures, including `"meteor_dungeon"` (full read) |
| `world/gen/nbt/SpawnCondition.java` | 114 | Per-structure spawn-rule DSL (full read) |
| `world/gen/nbt/{JigsawPiece,JigsawPool}.java` | 42 + 63 | Jigsaw piece/pool data classes (full read) |
| `world/gen/nbt/NBTStructure.java` | 1,679 | The actual `.nbt`-schematic loader/jigsaw walker/chunk-populate hook (sized, not fully read) |
| `world/gen/nbt/INBTBlockTransformable.java` | 314 | Block-transform hooks for schematic placement (sized, not fully read) |
| `world/gen/component/Component.java` | 1,213 | `BlockSelector` implementations incl. `MeteorBricks`/`SupplyCrates`/`CrabSpawners`/`GreenOoze` (read the 4 meteor-relevant inner classes in full) |
| `main/StructureManager.java` | 99 | `.nbt` resource-location registry for every jigsaw piece, incl. all 33 `meteor_*` entries |
| `config/CompatibilityConfig.java` (CE) | grepped | `oilBubbleSpawn` (default `0:100`), `meteoriteSpawn` (default `0:200`), `bedrockOilSpawn` (default `0:200`/`-6:200`) |
| `config/StructureConfig.java` (CE) | grepped | `enableStructures` (tri-state, default `"flag"`), `meteorDungeonSpawnWeight` (default `1`) |
| `config/WorldConfig.java` (CE) | grepped | `craterBiomeRad`/`craterBiomeInnerRad`/`craterBiomeOuterRad`/`craterBiomeWaterMult` — cross-checked, **already ported** (see below) |

Port-side files read: `src/main/java/com/hbm/blocks/machine/OilChainBlocks.java` (full),
`src/main/java/com/hbm/world/feature/OilSpot.java` (full, already ported),
`src/main/java/com/hbm/blocks/bomb/BlockFissureBomb.java` (full, current stub),
`src/main/java/com/hbm/config/CompatibilityConfig.java` (full), `StructureConfig.java` (grepped),
`GeneralConfig.java` (grepped), `docs/phase2/STATUS.md`, `docs/phase2/oil_production_chain.md`
(grepped), `docs/phase4/fallout_rain_and_effects.md` (read in full for crater-biome cross-reference).

`upstream/neo-edition` files read for **API-shape confirmation only** (never as a behavior source):
`world/feature/OilBubbleFeature.java` (186), `world/feature/BedrockOilDepositFeature.java` (48),
`world/feature/CrashedBombFeature.java` (69), `world/feature/NtmFeatures.java` (24),
`world/gen/NtmConfiguredFeatures.java` (41), `world/gen/NtmPlacedFeatures.java` (42),
`world/gen/NtmBiomeModifiers.java` (41), `world/MeteoriteStructure.java` (300).

## Part 1 — Oil-deposit world-gen

### The exact data shape (correcting the task's framing)

There is **no per-chunk richness/pocket-size record anywhere in CE**. Oil deposits are:

- **`OilBubble`** (underground pocket): once per chunk-generation pass, if
  `rand.nextInt(dimOilSpawn) == 0` (dim-0 default `dimOilSpawn = 100`, i.e. ~1% of chunks; halved
  further to ~1/33 in hot-and-dry biomes by dividing the roll by 3 first), CE picks one random point
  `(x, y∈[0,25), z)` inside that chunk and calls `OilBubble.spawnOil` with `radius = 10 + rand.nextInt(7)`
  (10–16). That method is a **flattened-ellipsoid stamp** — the exact test is
  `xx² + yy²·3 + zz² < (radius²)/2` — that overwrites every `Blocks.STONE` inside the shape with
  `ModBlocks.ore_oil`. It also calls `addSurfaceSpot` (150 Gaussian-scattered surface columns turned
  into `dirt_oily`/`dirt_dead`/`sand_dirty(_red)`/`stone_cracked`/`plant_dead`, plus an `oil_spill`
  surface puddle in one cardinal direction) — purely cosmetic surface dressing above the pocket, not
  part of the extractable deposit itself.
- **`BedrockOilDeposit`** (deep tap-only reserve): independently, once per chunk at 1/200 odds
  (`bedrockOilSpawn` default `0:200`), converts every `Blocks.BEDROCK` block within a Manhattan-distance
  (`|dx|+|y|+|dz| ≤ 6`) diamond region of a random point (`y ∈ [0,4]`, `|dx|,|dz| ≤ 4`) into
  `ModBlocks.ore_bedrock_oil`, then also calls `OilSpot.generateOilSpot` and a nearby porous-stone ore
  vein (`DungeonToolbox.generateOre(..., ModBlocks.stone_porous)`, not otherwise relevant here).
- **`OilSandBubble`** (desert fracking feedstock): in dry, hot, non-raining biomes only, at 1/600 odds,
  a second ellipsoid stamp (radius 15–45) converts existing `Blocks.SAND` into `ModBlocks.ore_oil_sand`.
  This is mined and **liquefied via `LiquefactionRecipes`/`ChemicalPlantRecipes`/`SolidificationRecipes`
  as an ordinary block-drop item, not through the derrick/pumpjack BFS mechanic at all** — a separate,
  already-in-scope-elsewhere machine-recipe path, not part of this report's extraction contract.

**There is no depletion tracking data structure.** "Depletion" is exactly one thing: each successful
extraction has a small, config-driven chance (`drainChance`) to flip that *specific* `ore_oil` block
in-place to `ore_oil_empty` (a plain, still-BFS-traversable "the ore here is spent" marker block, but
no longer itself yielding fluid). `ore_bedrock_oil` (bedrock-tap fracking-only resource) never depletes
— it has no drain-chance branch in `TileEntityMachineFrackingTower.onSuck`.

### The exact contract `OilChainBlocks`' block entities need

`TileEntityOilDrillBase` (334 CE lines, the shared derrick/pumpjack/fracking-tower base) defines it
precisely, and it is **pure `Level#getBlockState`/`Block` identity comparison — no capability, no data
attachment, no lookup table**:

1. Every `getDelayEff()` ticks (derrick 50, pumpjack 25, fracking tower 20, each further modified by
   speed/power/overdrive upgrade levels), walk straight down the column under the machine from
   `pos.getY() - 1` to `getDrillDepth()` (5 for derrick/pumpjack, 0 for the fracking tower) looking for
   the first non-`oil_pipe` block.
2. At that block, call `trySuck(y)` first: `canSuckBlock(Block)` (derrick/pumpjack: `ore_oil` or
   `ore_oil_empty`; fracking tower additionally allows `ore_bedrock_oil`) gates a **breadth-first flood
   fill** (`ArrayDeque`, capped at 256 visited nodes, `BobMathUtil.getShuffledDirs()` for direction
   order) through contiguous `ore_oil_empty` blocks looking for a real `ore_oil`/`ore_bedrock_oil` hit —
   this is exactly how one derrick can keep draining a whole connected pocket without needing to be
   re-positioned, and how the pocket "runs dry" once every reachable node has flipped to
   `ore_oil_empty`.
3. If `trySuck` fails (no oil reachable), fall back to `tryDrill(y)`: convert that column block into
   `ModBlocks.oil_pipe` (unless its explosion resistance is ≥1000, in which case set `indicator = 2`,
   "can't drill through this") and extend the shaft one block deeper next cycle.
4. On a successful suck, `onSuck(pos)` (each machine's own override) adds a **fixed fluid amount per
   tick-cycle** — not per "unit of ore" — to its tanks: derrick 500 mB oil + 100–500 mB gas (random);
   pumpjack 750 mB oil + 50–250 mB gas; fracking tower 1,000 mB oil + 100–500 mB gas from `ore_oil`, or
   100 mB oil + 10–50 mB gas from `ore_bedrock_oil` (consuming `solutionRequired=10` mB of `FRACKSOL`
   fluid per successful tap via `canPump()`), then rolls `drainChance` (derrick 5%, pumpjack 2.5%,
   fracking tower 2% — `ore_oil` only) to flip that block to `ore_oil_empty`.
5. `ItemOilDetector` (the player-facing scanner) performs an **entirely separate, redundant scan** —
   a straight-down column check at the player's feet plus 50 Gaussian-scattered sample columns within a
   25-block radius — checking the exact same `ore_oil`/`ore_bedrock_oil` block identities directly. It
   shares no code or cache with the derrick's BFS; it is a simple "is this literal block present"
   probe.

All of §1–5 above have **zero blocked dependencies** on anything not already in this port —
`OilChainBlocks` (Phase 2) already registers every block name this needs
(`ore_oil_empty`/`ore_bedrock_oil`/`ore_oil_sand`/`sand_dirty`/`sand_dirty_red`/`stone_cracked`) and
`OilSpot` is already ported. The only real gap is that **nothing places `ore_oil`/`ore_bedrock_oil`
in the world yet** — which is this report's actual deliverable.

### Real 1.21.1 API shape (confirmed via neo-edition, not copied for behavior)

`upstream/neo-edition/src/main/java/com/hbm/world/feature/OilBubbleFeature.java` (186 lines) and
`BedrockOilDepositFeature.java` (48 lines) are **real, already-written, compiling NeoForge 1.21.1
implementations of exactly this half of the area** (this port must still author its own version from
CE's numbers per this task's rules — never copy neo-edition's logic — but their *shape* is directly
confirmed, load-bearing evidence, not a guess):

- Both extend `net.minecraft.world.level.levelgen.feature.Feature<NoneFeatureConfiguration>`, overriding
  `boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)`, reading
  `context.level()` (a `WorldGenLevel`), `context.origin()` (`BlockPos`), `context.random()`
  (`RandomSource`).
- `OilBubbleFeature` reimplements CE's own once-per-chunk roll **inside `place()` itself** (a
  `getSpawnRate` helper returning `100`, divided by 3 in hot/dry biomes via
  `Holder<Biome>`/`biome.getPrecipitationAt(pos) == Biome.Precipitation.NONE`, then
  `random.nextInt(spawnRate) != 0 → return false`) rather than relying on a placement-modifier rarity —
  this is the correct real-API pattern for porting CE's *config-driven, per-dimension* frequency
  faithfully, since a `RarityFilter` placement modifier is baked into static datapack JSON and cannot
  read a live mutable config value at generation time.
- Full real registration chain, confirmed by `NtmFeatures.java`/`NtmConfiguredFeatures.java`/
  `NtmPlacedFeatures.java`/`NtmBiomeModifiers.java`: `DeferredRegister<Feature<?>>` (code) →
  `BootstrapContext<ConfiguredFeature<?,?>>` (datapack-registry bootstrap, `NoneFeatureConfiguration`)
  → `BootstrapContext<PlacedFeature>` (bootstrap with `PlacementModifier`s —
  `InSquarePlacement.spread()`, `HeightRangePlacement.uniform(VerticalAnchor.absolute(0),
  VerticalAnchor.absolute(24))` for the oil bubble's `y∈[0,25)` CE range, `HeightmapPlacement`/
  `RarityFilter.onAverageOnceEvery(n)` for features that *don't* need a live config value) →
  `BootstrapContext<BiomeModifier>` using NeoForge's own `AddFeaturesBiomeModifier`, gated by
  `BiomeTags.IS_OVERWORLD` and a `GenerationStep.Decoration` phase (`UNDERGROUND_ORES` for both oil
  features). This four-stage registry-bootstrap pipeline is the real, confirmed 1.21.1 NeoForge shape
  for "a custom world-gen feature, wired into biomes, with datapack-level placement rules."
- Neither neo-edition feature reads any live config value for its base spawn rate or radius —
  `BUBBLE_BASE_SPAWN_RATE = 100`/`BUBBLE_MIN_RADIUS = 10`/`BUBBLE_RADIUS_VARIATION = 7` are hardcoded
  Java constants (CE's own numeric defaults, just not re-exposed as tunables). This is a real,
  already-made design choice this port should explicitly confirm or reject (see Open questions).

### Deferred scope (Part 1)

- **`com.hbm.world.phased.*`** (`AbstractPhasedStructure` 347 lines + `PhasedStructureRegistry`/
  `DynamicStructureDispatcher`/`PhasedStructureGenerator`/`PhasedEventHandler`/`PhasedConstants`/
  `DimensionState`, 2,673 more lines) — CE's generic "wait for enough neighboring chunks to load, then
  commit a multi-chunk block layout" scheduler that `OilBubble`/`OilSandBubble`/`BedrockOilDeposit`
  (and `MeteoriteStructure`, and many unrelated CE structures — radio/antenna/atom/barrel/satellite/
  spaceship/bunker/dud/landmine/sellafield-pool) all build on via `useDynamicScheduler()`/
  `getWatchedChunkOffsets()`. **Zero files under this package exist in the port.** Given neo-edition's
  own working precedent (a plain `Feature<NoneFeatureConfiguration>` writing directly via
  `WorldGenLevel#setBlock`, no chunk-wait scheduler at all, accepted because vanilla's `WorldGenRegion`
  already tolerates a feature writing a small buffer beyond its origin chunk), this port does **not**
  need to port the phased-structure substrate to deliver oil-deposit parity — see Key design decisions.
- **`CompatibilityConfig`'s per-dimension spawn-rate maps** (`oilBubbleSpawn`, `bedrockOilSpawn`) — this
  port's own `CompatibilityConfig.java` javadoc (lines 12–25, read in full) already documents that CE's
  ~60 int-dimension-ID-keyed maps were deliberately **not** ported pending "whichever phase owns world
  generation, once it knows the real set of dimensions" — that phase is this one. Whoever implements
  this must add re-keyed (`ResourceKey<Level>`-based) config entries for these two maps specifically,
  reusing `ConfigUtil.toIntMap`'s existing `"key:value"` string-list encoding pattern. (Confirmed:
  neo-edition sidestepped this entirely by hardcoding CE's default numbers — a real option, but a loss
  of CE's per-dimension configurability that should be a conscious choice, not an oversight.)
- **`ModBlocks.oil_pipe`, `dirt_oily`, `dirt_dead`, `ore_oil`** — per `OilChainBlocks.java`'s own
  javadoc, already registered elsewhere in this port (Phase 0/1); not this report's concern.

## Part 2 — Meteor world-gen: three distinct systems

### 2a. `MeteoriteStructure`/`Meteorite` — ambient ore-blob feature (small, oil-deposit-sized)

`MeteoriteStructure` (50 lines) is a phased structure exactly like `OilBubble`: 1-in-200-chunk roll
(`CompatibilityConfig.meteoriteSpawn`, default `0:200`), finds a surface Y (`world.getHeight(x,z) -
rand.nextInt(10)`), bails if the ground two blocks down is air/liquid, then calls
`new Meteorite().generate(world, rand, x, y, z, safe=false, allowSpecials=false, damagingImpact=false)`.

`Meteorite.generate` (625 lines) is the **shared** shape-stamping engine also called by
`EntityMeteor.java:142` (the live falling-meteor entity, `safe, allowSpecials=true,
damagingImpact=true` — that entity's own strike-impact behavior, explicitly out of this report's scope
per the task's framing). With `allowSpecials=false` (the passive world-gen path), it always falls
through to one of three size tiers (`generateLarge`/`generateMedium`/`generateSmall`, equal weight),
each of which randomly composes concentric sphere/star/box shells from `block_meteor_molten` (glowing
hazard rock)/`block_meteor_cobble`/`block_meteor_broken`/`block_meteor_treasure`
(all `BlockNTMOre`-style plain ore blocks, not chests) around a core of `getRandomOre()` — one
`ModBlocks.ore_meteor` block per `BlockEnums.EnumMeteorType` value (iron/cobalt/aluminium/copper/rare
tiers, confirmed via `OreDictManager.DictFrame`). `generateReplacables()` restricts what pre-existing
terrain the shape is allowed to overwrite when `safeMode` is on (irrelevant here since world-gen always
passes `safe=false`). **No loot table, no tile entity, no mob spawner anywhere in this class** — it is
a mineable rock/ore formation, nothing more.

**Deferred scope (2a):** identical to Part 1 — this rides on the same unported
`com.hbm.world.phased.*` substrate, and `CompatibilityConfig.meteoriteSpawn` needs the same
dimension-ID re-keying work as `oilBubbleSpawn`. neo-edition's own `MeteoriteStructure.java` (300 lines,
read in full) ports `Meteorite`'s shape logic faithfully as a bare helper class but registers it as
**no `Feature` anywhere** — confirming that even the reference port has not closed this specific gap
yet; whoever does this in this port gets no shortcut here, only the `OilBubbleFeature` pattern to
follow structurally.

### 2b. The real meteor dungeon — `"meteor_dungeon"` in `NTMWorldGenerator`

This is what the task was actually asking about, and it is **not part of either system above.** CE's
custom NBT-jigsaw structure engine (`com.hbm.world.gen.nbt.{NBTStructure,JigsawPiece,JigsawPool,
SpawnCondition}` + `com.hbm.main.StructureManager` + `com.hbm.world.gen.component.Component`, ~3,540
combined CE lines) is a **fully custom, pre-vanilla-jigsaw** (1.12 has no Mojang jigsaw system)
multi-room dungeon generator, and it is the placement mechanism for **~30 unrelated CE structures**
registered in one 430-line constructor (`NTMWorldGenerator`) — spire, bunker, vertibird, crashed
vertibird, aircraft carrier, **oil rig** (a surface ocean platform — cosmetically oil-themed but
architecturally unrelated to Part 1's underground deposits), lighthouse, beached patrol boat, dish,
forest chem lab, two crashed planes, three desert shacks, laboratory, forest post, factory, crane,
broadcaster tower, ten numbered "NTM ruins," a radio house, and `"meteor_dungeon"`.

`"meteor_dungeon"`'s `SpawnCondition` (from `NTMWorldGenerator.java:260-374`): fixed spawn height
`minHeight = maxHeight = 32`, `sizeLimit = 128`, `canSpawn = biome -> biome.getBaseHeight() >= 0.0F`
(land biomes only), weight from `StructureConfig.meteorDungeonSpawnWeight` (CE default `1`; **this
port's own `StructureConfig.java` already has a ported `METEOR_DUNGEON_SPAWN_WEIGHT` field waiting,
unused** — confirmed by reading it). Content is a graph of named `JigsawPool`s (jigsaw-piece groups
with weighted random selection and named fallbacks):

- `"start"` → `meteor_core` (the single entry piece).
- `"default"` → corridor pieces `meteor_corner`/`meteor_t`/`meteor_stairs`/`meteor_room_base_thru`/
  `meteor_room_base_end` (weights 2/3/1/3/4), falling back to `"fallback"` (`meteor_fallback`, a dead-end
  cap) when the jigsaw walk can't place a neighbor.
- `"10room"` → 8 unique room variants at equal weight — `basic`/`balcony`/`dragon`/`ladder`/`ooze`/
  `split`/`stairs`/`triple` — falling back to `"roomback"` (`meteor_room_fallback`). `meteor_room_dragon`
  is the boss room.
- `"3x3loot"` → 16 small self-contained loot-vault variants at equal weight (`bale`/`blank`/`block`/
  `crab`/`crab_tesla`/`crate`/`dirt`/`lead`/`ooze`/`pillar`/`star`/`tesla`/`book`/`mku`/`statue`/`glow`),
  self-fallback.
- `"headloot"` → 4 treasure-room variants (`dragon_chest`/`dragon_tesla`/`dragon_trap`/
  `dragon_crate_crab`) at equal weight, falling back to `"headback"` (`meteor_loot_fallback`).

Each piece also carries a **block-transform table** (`blockTable`, a `Map<Block,
StructureComponent.BlockSelector>`) applied at placement time by `NBTStructure`'s schematic loader —
placeholder blocks baked into the raw `.nbt` file get randomly re-rolled per-instance:
- `bricks = {meteor_brick → Component.MeteorBricks}`: 40% plain brick / 30% mossy / 30% cracked (CE
  `Component.java:1147-1160`, read in full).
- `crates = {meteor_brick → MeteorBricks, crate → Component.SupplyCrates, meteor_spawner →
  Component.CrabSpawners}`: crate contents 60% air / 20% `crate_ammo` / 10% `crate_can` / 10% plain
  `crate` (`:1164-1179`); spawner density 80% plain brick / 20% real `meteor_spawner` block — confirmed
  a `BlockCybercrab` instance (`ModBlocks.java:404`), i.e. it spawns `EntityCyberCrab`, matching Phase
  3's `gun_framework.md` finding that this mob is still live and still needs the legacy
  `ItemGunBase`/`EntityBulletBase` projectile system (`:1183-1196`).
- `ooze = {meteor_brick → MeteorBricks, concrete_colored → Component.GreenOoze}`: 80% `toxic_block` /
  20% `meteor_polished` (`:1198-1211`).

Crate loot itself resolves through the ordinary vanilla-shaped `TileEntityCrate` lazy `LootTable` field
(confirmed present, not read in full — out of this report's scope, already Phase 2/3 territory) — the
dungeon's own job is only to place the crate blocks; loot-table content is a separate, already-general
system.

**All 38 of the referenced `.nbt` schematic files are real, present assets**
(`assets/hbm/structures/meteor/**/*.nbt`, counted directly) — this is not missing content, it is
missing *engine and asset-conversion* work. The room geometry/loot layout does not need to be
re-authored from scratch, but 1.12.2's structure-NBT block palette (numeric IDs+metadata) does not
match 1.21.1's block-state palette, so the files need a conversion pass before they can load through
whatever placement mechanism replaces `NBTStructure`.

### Deferred scope (2b) — this is the big one

- **The entire custom jigsaw-structure substrate**
  (`com.hbm.world.gen.nbt.{NBTStructure (1,679 lines), JigsawPiece, JigsawPool, SpawnCondition,
  INBTBlockTransformable (314 lines), INBTTileEntityTransformable}` +
  `com.hbm.main.StructureManager` + `com.hbm.world.gen.component.Component` (1,213 lines) +
  `com.hbm.world.gen.NTMWorldGenerator` itself, ~3,970 combined CE lines) — **zero files exist in the
  port.** This is not separable per-structure: `"meteor_dungeon"` is registered in the same
  constructor, using the same `NBTStructure.GenStructure` `PopulateChunkEvent.Pre`/`IWorldGenerator`
  dual hook, as ~29 other unrelated buildings. Porting "just the meteor dungeon" in practice means
  standing up this entire engine (or a from-scratch 1.21.1-shaped jigsaw-piece replacement using
  vanilla's own `StructureTemplate`/jigsaw-adjacent APIs) — a project on the scale of Phase 3's already
  large single areas, not a small addendum to oil-deposit world-gen. **Recommend this become its own
  named Phase 4 (or later) research/implementation area** ("structure/dungeon placement engine" or
  similar) covering all ~30 CE buildings at once, not something folded into this report's two
  originally-scoped features.
- **38 `.nbt` schematic assets** (`assets/hbm/structures/meteor/**`) exist and are usable content but
  need a block-palette conversion pass (1.12.2 numeric IDs → 1.21.1 block states) before any 1.21.1
  structure-template loader can read them.
- **`BlockCybercrab`/`EntityCyberCrab`** (the `meteor_spawner` block's mob) — already named as a live
  legacy-gun-system dependency by Phase 3's `gun_framework.md`; not re-scoped here, just re-confirmed as
  a real call site inside this dungeon.
- **`StructureConfig.enableStructures`/`meteorDungeonSpawnWeight`** — already fully ported in this
  port's `StructureConfig.java` (confirmed by direct read); no work needed on the config side.

## Part 3 — The crater biome: confirmed a third, separate system

Direct answer to the task's explicit question: **`BiomeGenCraterBase` is neither Part 1 (oil) nor
Part 2 (meteor) — it is its own, fourth system**, and it is not really "world generation" in the
chunk-decoration sense at all.

- `BiomeGenCraterBase` (111 lines, read in full) is three `Biome` subclasses (`craterBiome`/
  `craterInnerBiome`/`craterOuterBiome`) with only cosmetic overrides (grass/foliage/sky/water color) —
  no vanilla `BiomeProvider` ever places them as a "natural" biome anywhere on the map. Grepping the
  entire CE tree for `craterBiome`/`craterInnerBiome`/`craterOuterBiome` usage outside the definition
  file turns up exactly three real consumers, none of which are oil or meteor code:
  1. **`EntityFalloutRain.getBiomeChange()`** — the actual, and only, code that *paints* these biomes
     onto already-generated terrain, based on nuke-strike scale/distance, gated by
     `WorldConfig.enableCraterBiomes`. This is **already fully researched and explicitly deferred** by
     the sibling report `docs/phase4/fallout_rain_and_effects.md` (its own Deferred-scope section names
     `BiomeGenCraterBase`/crater world-gen as a named forward reference for "whoever implements it" —
     i.e., confirms this exact question is expected to be answered by an area like this one).
  2. **`com.hbm.handler.EntityEffectHandler.onUpdate`** (759 lines, not ported, out of scope) — the
     general per-tick living-entity handler that turns "standing in a crater biome" into an actual
     `ContaminationUtil.contaminate(...)` radiation tick, using the already-ported
     `WorldConfig.CRATER_BIOME_RAD`/`CRATER_BIOME_INNER_RAD`/`CRATER_BIOME_OUTER_RAD`/
     `CRATER_BIOME_WATER_MULT` values (confirmed present in this port's `WorldConfig.java`, no new
     config work needed).
  3. **`BlockFissureBomb.explodeEntity`** (49 CE lines, read in full; port stub already exists at
     `src/main/java/com/hbm/blocks/bomb/BlockFissureBomb.java`, confirmed) — only ever *reads*
     `world.getBiome(pos) instanceof BiomeGenCraterBase` as a single boolean, to decide whether the
     `ore_bedrock_block → ore_volcano` conversion it performs sets `BlockFissure.CRATER = true` (making
     the resulting volcano vent emit `rad_lava_block` instead of `volcanic_lava_block` once it's
     registered and this bomb's stub is filled in). **It never creates or places the biome itself.**
- **A genuine new cross-dependency this report can confirm that wasn't previously spelled out**:
  `BlockFissureBomb.explodeEntity` also does a second, independent thing inside the exact same
  5-block-radius sweep — it converts any `ModBlocks.ore_bedrock_oil` found there back into plain
  `Blocks.BEDROCK`. That means **this fissure bomb detonation directly destroys any Part-1 bedrock-oil
  deposit within its blast radius** — a real, small, concrete integration point between Phase 3's
  `BlockFissureBomb` stub and this report's oil-deposit block once both exist. `ore_bedrock_oil` itself
  is already registered by `OilChainBlocks` (Phase 2), so this half of `BlockFissureBomb`'s stub is
  actually unblockable *today* — only the `ore_bedrock_block`/`ore_volcano`/`BiomeGenCraterBase`-equiv
  half is still blocked.
- `ore_bedrock_block` (the *other* thing `BlockFissureBomb` needs, `BlockBedrockOreTE`-backed, "not
  yet registered" per the port's own stub comment) is placed by a **fifth, still-different** phased
  structure, `com.hbm.world.feature.BedrockOre` (194 lines, read in full) — a generic, tiered
  (1–4), Perlin-density-gated bedrock resource-vein feature used by the (also unresearched, unowned)
  Excavator machine for glowstone/quartz/powder-fire (Nether) and a generic
  `bedrock_ore_base`+acid-requirement resource (Overworld). This is **not oil-related** and **not
  meteor-related** — flagging it here only because `BlockFissureBomb` needs it and it would otherwise
  be easy to conflate with `BedrockOilDeposit` (similar name, same `AbstractPhasedStructure` base,
  same "bedrock" vocabulary, completely different block/purpose). Whoever owns the Excavator machine
  should pick this up; it is out of scope for both this report's features.
- `ore_volcano` (`BlockFissure`, 105 lines, read in full) is **not a plain decorative block** — it's a
  ticking `BlockContainer` with its own `TileEntityFissure` that pipes 1,000 mB/tick of lava upward
  through a fluid-standard-sender interface, and its `updateTick` places `rad_lava_block` or
  `volcanic_lava_block` above itself depending on the `CRATER` boolean property. Whoever eventually
  implements `BlockFissureBomb`'s deferred half needs to port this small machine block too, not just a
  static block state.

## Phase-4-safe scope

| Component | CE source (lines read) | Port status | What Phase 4 should do |
|---|---|---|---|
| `OilBubble` | `world/OilBubble.java` (197, full) | Not ported | Port as a `Feature<NoneFeatureConfiguration>` per the neo-edition-confirmed shape; reads `CompatibilityConfig`'s (to-be-added) `oilBubbleSpawn` map at generation time, not a baked `RarityFilter` |
| `OilSandBubble` | `world/OilSandBubble.java` (89, full) | Not ported | Same `Feature` shape, desert-biome-gated |
| `BedrockOilDeposit` | `world/feature/BedrockOilDeposit.java` (80, full) | Not ported | Same `Feature` shape; also calls the already-ported `OilSpot.generateOilSpot` |
| `ItemOilDetector` | `items/tool/ItemOilDetector.java` (103, full) | Not researched/ported here | Trivial: plain block-identity column scan against `ModBlocks.ore_oil`/`ore_bedrock_oil`, no dependency on this report's other findings beyond those two block fields (already registered) |
| `TileEntityOilDrillBase`+3 concretes' BFS suck/drill logic | 4 files, 1,120 lines combined, full | Framework (derrick/pumpjack/fracking-tower TEs) presumably exists per Phase 2 (not re-verified here — out of this report's remit) | Confirm the BFS/`canSuckBlock`/`onSuck`/`drainChance` contract above matches whatever Phase 2 already built; this report found no gap in it |
| `MeteoriteStructure`/`Meteorite` | `world/MeteoriteStructure.java` (50) + `world/Meteorite.java` (625), full | Not ported (port-side); neo-edition ported `Meteorite`'s body only, as an unregistered helper | Port `Meteorite`'s shape-generation body faithfully; register a passive `Feature<NoneFeatureConfiguration>` following the exact same shape as `OilBubbleFeature` |
| `BlockMeteorOre`/`EnumMeteorType`, `block_meteor*` | `blocks/ModBlocks.java` (grepped) | Not confirmed in port (not checked here) | Verify against whichever area owns meteor *blocks* (likely Phase 1/2 content, not this report) |
| `"meteor_dungeon"` (NBT jigsaw dungeon) | `world/gen/NTMWorldGenerator.java` (430) + `nbt/*` (2,212) + `main/StructureManager.java` (99) + `world/gen/component/Component.java` (1,213), all read/sized | Not ported at all; not a small feature | **Do not fold into this report's implementation pass.** Recommend spinning off a dedicated "structure/dungeon placement engine" Phase 4/5 area covering all ~30 CE NBT-jigsaw structures together |
| `BiomeGenCraterBase` (registration only — 3 `Biome` instances, biome-dictionary tags) | `world/biome/BiomeGenCraterBase.java` (111, full) | Not ported | Small, standalone: register 3 biomes (1.21.1 datapack-driven `Biome` bootstrap, not Java subclassing — see Open questions) and their `BiomeTags`. Does **not** need any placement/world-gen logic of its own — it is only ever assigned dynamically |
| `WorldConfig` crater tunables | `config/WorldConfig.java:25-28` (CE) | **Already ported** (confirmed via `docs/phase4/fallout_rain_and_effects.md`'s own read) as `CRATER_BIOME`/`CRATER_BIOME_RAD`/`CRATER_BIOME_INNER_RAD`/`CRATER_BIOME_OUTER_RAD`/`CRATER_BIOME_WATER_MULT` | No work needed |
| `StructureConfig.meteorDungeonSpawnWeight`/`enableStructures` | `config/StructureConfig.java` (CE, grepped) | **Already ported** (confirmed by direct read of port's `StructureConfig.java`) | No work needed |
| `BlockFissureBomb`'s `ore_bedrock_oil → BEDROCK` half | `blocks/bomb/BlockFissureBomb.java:42-44` (CE) | Port stub exists, this half unblocked once `ore_bedrock_oil`-placing world-gen (this report) lands | Note for Phase 3/bomb owner: this half can be filled in independently of the `ore_bedrock_block`/`ore_volcano`/crater-biome half |

## Deferred scope (consolidated)

- **`com.hbm.world.phased.*`** (2,673+347 CE lines) — the chunk-scheduling substrate for
  oil/bedrock-oil/meteorite/bedrock-ore and several unrelated small structures. Not required for
  Part 1/2a per neo-edition's real working precedent (plain `Feature` writes); flag as a real
  architecture decision this report cannot make unilaterally (see Open questions).
- **The full NBT-jigsaw structure engine** (`world/gen/nbt/*`, `main/StructureManager`,
  `world/gen/component/Component`, `world/gen/NTMWorldGenerator`, ~3,970 CE lines) — owns
  `"meteor_dungeon"` and ~29 unrelated CE buildings. Recommend a dedicated future Phase 4/5 research +
  implementation area, not this report's scope.
- **`CompatibilityConfig.oilBubbleSpawn`/`bedrockOilSpawn`/`meteoriteSpawn`** — need dimension-ID
  re-keying; already flagged as blocked by `CompatibilityConfig.java`'s own javadoc, named here as the
  specific two-plus-one map entries this area needs filled in.
- **`com.hbm.world.feature.BedrockOre`** (194 CE lines) — generic tiered bedrock-ore-vein feature for
  the Excavator machine; needed by `BlockFissureBomb`'s other half (`ore_bedrock_block`), not by oil or
  meteor content. Belongs to whichever area owns the Excavator.
- **`com.hbm.blocks.generic.BlockFissure`/`ore_volcano`** (105 CE lines) — a small lava-emitting
  machine block, the other missing piece of `BlockFissureBomb`'s deferred half.
- **`com.hbm.handler.EntityEffectHandler`** (759 CE lines) — already named by
  `fallout_rain_and_effects.md`; the actual "standing in a crater biome hurts you" tick logic. Not this
  report's job; re-confirmed here only as the third real consumer of `BiomeGenCraterBase`.
- **`BlockCybercrab`/`EntityCyberCrab`** — already named by Phase 3's `gun_framework.md` as a live
  legacy-gun-system dependency; re-confirmed here as the meteor dungeon's spawner-block mob.
- **38 `.nbt` schematic files** under `assets/hbm/structures/meteor/**` — real content, needs a
  1.12.2→1.21.1 block-palette conversion pass whenever the jigsaw-engine replacement is built.

## Key design/API decisions confirmed against real code

1. **Oil deposits have no abstract data model** — behavior is 100% "real block placed in the world,
   queried by identity," confirmed by `TileEntityOilDrillBase`, `ItemOilDetector`, and both fracking
   tower overrides all doing the same raw `Block ==` comparisons against live `BlockPos`s. Any
   Phase 4 implementation should resist the temptation to invent a chunk-capability/data-attachment
   layer for this — CE itself never has one.
2. **Depletion is a single per-block state flip, not a countdown/counter.** `ore_oil → ore_oil_empty`
   on a `drainChance` roll, checked independently by each `onSuck` call. `ore_bedrock_oil` never
   depletes.
3. **`Feature<NoneFeatureConfiguration>` + the 4-stage `ConfiguredFeature`→`PlacedFeature`→
   `BiomeModifier` datapack-registry bootstrap is the confirmed real 1.21.1 NeoForge shape** for
   porting these features — directly evidenced by neo-edition's own compiling `OilBubbleFeature`/
   `BedrockOilDepositFeature`/`NtmConfiguredFeatures`/`NtmPlacedFeatures`/`NtmBiomeModifiers`, not
   inferred from general Mojang-mapping knowledge alone.
4. **A config-driven (not baked-`RarityFilter`) spawn-rate check belongs inside the `Feature.place()`
   method itself**, reading a live config value each call — confirmed as the correct pattern by
   `OilBubbleFeature.getSpawnRate()`'s real, working implementation of exactly CE's own hot/dry-biome
   divide-by-3 rule.
5. **The crater biome is registration-only work for this report; its placement logic belongs to
   `fallout_rain_and_effects.md`'s area.** This report should not attempt to design a "natural crater
   biome spawn" — CE has none.
6. **`BlockFissureBomb`'s two halves are independently unblockable** — the `ore_bedrock_oil → BEDROCK`
   conversion needs only this report's oil-deposit block (already registered); the
   `ore_bedrock_block → ore_volcano`+crater-flag conversion needs `BedrockOre`'s feature,
   `BlockFissure`'s machine block, and this report's crater-biome registration — three separate,
   differently-owned dependencies bundled in one CE method.

## Open questions / risks

- **Port the `com.hbm.world.phased.*` chunk-scheduling substrate faithfully, or follow neo-edition's
  simpler "write directly in `Feature.place()`" precedent?** CE built the phased scheduler because a
  naive single-pass 1.12 `IWorldGenerator` writing into not-yet-generated neighbor chunks silently loses
  those edits. Modern vanilla's `WorldGenLevel`/`WorldGenRegion` already buffers a feature's writes
  within a bounded neighborhood specifically to solve this same class of problem, and neo-edition's real
  `OilBubbleFeature` (radius up to 16, i.e. spanning up to 2 chunks) and `BedrockOilDepositFeature`
  (radius 4) both write directly with no scheduler and are treated as working. This report cannot
  confirm from source reading alone whether vanilla's write-buffer is large enough for `OilBubble`'s
  full radius-16 case without clipping at extreme chunk-border rolls — this needs either a real
  in-engine test (not possible in this sandbox, no compiled jar) or an explicit decision to accept
  occasional clipped/truncated pockets at chunk borders as a acceptable parity loss, matching
  neo-edition's own already-accepted tradeoff.
- **Does this port want to preserve CE's per-dimension configurable spawn frequency at all?**
  neo-edition did not (hardcoded constants). If yes, `CompatibilityConfig` needs new
  `ResourceKey<Level>`-keyed maps for `oilBubbleSpawn`/`bedrockOilSpawn`/`meteoriteSpawn` — real,
  buildable work using `ConfigUtil.toIntMap`, but a scope decision, not a technical blocker.
- **1.21.1 custom-`Biome` registration is fully datapack/registry-driven** (a `Biome.BiomeBuilder`
  registered via a `BootstrapContext<Biome>`, not a Java subclass with overridable per-position color
  methods the way CE's `BiomeGenCraterBase` does it) — this is well-established Mojang-mapping
  knowledge, **not verified against a compiled jar or against any neo-edition precedent** (grepped
  neo-edition for biome registration and found none related to craters). CE's exact per-position noise
  grass-color trick (`GRASS_COLOR_NOISE.getValue(...)`) has no direct 1.21.1 analog in the same shape;
  a fixed `BiomeSpecialEffects` color (losing the noise variation) is the likely pragmatic substitute,
  but this is a real open design question, not something this report can resolve from CE source alone.
- **Should the "meteor dungeon" be built at all before the general NBT-jigsaw engine exists?** Given
  it shares 100% of its placement machinery with ~29 unrelated CE buildings, implementing it in
  isolation (e.g., a bespoke one-off multi-piece placer just for this one dungeon) would likely mean
  redoing the work again later when that larger engine gets built. Recommend explicitly sequencing:
  engine first (separate area), `"meteor_dungeon"`'s specific pools/pieces second, using this report's
  §2b findings directly.
- **`meteor_spawner`'s exact spawn-trigger/cooldown behavior** (`BlockCybercrab`, referenced but not
  read in full — out of this report's scope) is unverified; whoever picks up the meteor dungeon should
  read that class directly rather than assuming vanilla mob-spawner semantics.
