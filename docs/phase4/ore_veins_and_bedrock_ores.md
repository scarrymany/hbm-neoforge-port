# Ore-vein & bedrock-ore world-gen — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/lib/HbmWorldGen.java` (755 lines — the real, single
  `IWorldGenerator` entry point that actually places every ordinary ore vein; `generateOres` is the
  method this report is centered on)
- `upstream/hbm-ce/src/main/java/com/hbm/world/generator/DungeonToolbox.java` (132 lines — the
  `generateOre(...)` helper every ordinary vein call routes through)
- `upstream/hbm-ce/src/main/java/com/hbm/world/feature/{WorldGenMinableNonCascade,BedrockOre,
  BedrockOilDeposit,DepthDeposit,OreCave,OreLayer3D,SchistStratum}.java` (173+194+80+168+209+137+74 =
  1,035 lines — the actual per-vein/per-deposit shape algorithms)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/{BlockBedrockOre,BlockBedrockOreTE,
  BlockBiomeStone}.java` (54+153+123 lines) and `.../blocks/fluid/VolcanicBlock.java` (193 lines,
  read to line 135 in full plus signatures beyond)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/ItemBedrockOreBase.java` (71 lines, full) and
  `ItemBedrockOreNew.java` (365 lines, the `BedrockOreType`/`BedrockOreGrade`/`ProcessingTrait` enums
  read for their exact shape, not the 156-variant metadata table)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/BedrockOreRegistry.java` (220 lines, partial —
  enough to confirm its role) and `config/BedrockOreJsonConfig.java` (174 lines, sized/skimmed)
- `upstream/hbm-ce/src/main/java/com/hbm/config/CompatibilityConfig.java` (409 lines — every
  `*Spawn`/`*SpawnRate` field's exact default value grepped and cross-checked) and
  `config/WorldConfig.java`/`config/GeneralConfig.java` (grepped for the ore-relevant toggles)
- `upstream/hbm-ce/src/main/java/com/hbm/world/gen/{MapGenNTMFeatures,NTMWorldGenerator}.java`
  (147+430 lines, full — read to confirm these are the **structure** jigsaw engine, not ore veins, and
  do not overlap this report's scope)
- `upstream/hbm-ce/src/main/java/com/hbm/main/MainRegistry.java` (grepped around lines 320-355 — the
  `SchistStratum`/`OreCave`/`OreLayer3D` construction/registration call site)
- Repo-wide greps to settle three open questions precisely: every `ore_schrabidium`/`ore_gneiss_rare`/
  `ore_gneiss_schrabidium`/`ore_nether_schrabidium` reference in CE (to confirm which schrabidium-tier
  ores are placed by ordinary world-gen vs. by explosion/fallout/structure mechanics elsewhere), every
  `basalt_ore`/`stone_biome` reference (to confirm neither is chunk-decoration content), and every
  `ore_nether_coal`/`stone_gneiss`/`stone_porous`/`Library.nextIntDeterministic` reference in this
  port's own `src/` (to confirm exactly which supporting blocks/utilities are and are not registered
  yet)
- This port's own `src/main/java/com/hbm/{blocks/OreBlocks.java (282 lines, full — the exact
  already-registered-but-unplaced ore Block list), blocks/generic/{GenericBlocks.java (grepped for
  `BlockResourceStone`/`stone_depth`/`stone_porous`/`stone_gneiss`), BlockBedrockOre.java (51 lines,
  full)}, config/{WorldConfig.java (100 lines, full), CompatibilityConfig.java (141 lines, full)},
  blocks/machine/OilChainBlocks.java (grepped for `ore_bedrock_oil`), items/tool/{ItemSurveyScanner,
  ItemOreDensityScanner,ItemOilDetector}.java (grepped for their own documented forward-reference
  comments), items/special/{BedrockOreType,BedrockOreAmounts,BedrockOreGrade,ProcessingTrait,
  ItemBedrockOre,BedrockOreComponents,BedrockOreItems,BedrockOreOutput,ItemBedrockOreBase}.java
  (confirmed present, not read line-by-line — see Headline finding 3), datagen/ModDataGenerators.java
  (78 lines, full — the exact `RegistrySetBuilder`+`DatapackBuiltinEntriesProvider` pattern this
  report's own registration plan reuses), damage/ModDamageTypes.java (grepped, the only other
  datapack-registry bootstrap this port has written so far)}`
- `docs/phase0/STATUS.md`, `docs/phase3/bomb_blocks_and_detonators.md` (grepped, the report that first
  named `ore_bedrock_block`/`ore_bedrock_oil` as a forward reference), and the five Phase 4 sibling
  reports already on disk: `docs/phase4/{worldgen_oil_and_meteor_dungeons.md (read in full — the
  report that already fully owns oil-deposit, bedrock-oil, meteorite-ambient, meteor-dungeon and
  crater-biome-registration world-gen; this report treats its findings as authoritative and does not
  re-derive them), worldgen_structures_bunkers_stations.md, fallout_rain_and_effects.md,
  chunk_radiation_system.md, pollution_system.md, hbm_potion_system.md}` (grepped for cross-reference
  confirmation, particularly the schrabidium-ore-mutation and crater-biome ownership questions)
- `upstream/neo-edition/src/main/java/com/hbm/world/gen/{NtmConfiguredFeatures,NtmPlacedFeatures,
  NtmBiomeModifiers}.java` (41+42+41 lines, full) and `world/feature/OilBubbleFeature.java` (header/
  structure, cross-checked against the sibling oil report's own quotes rather than re-read in full)
  — **cross-referenced for confirmed NeoForge 1.21.1 API shape only**, per this task's ground rules;
  every behavioral claim below is sourced from CE, never from Neo Edition

## Headline finding

The task's framing is basically right that this is "ore veins + bedrock ores," but four premises need
correcting before a future implement-wave agent can safely act on this report, in the same
correct-the-framing spirit as this project's other gold-standard reports:

1. **Not every `OreBlocks.java`-registered ore is missing world-gen because Phase 1 outran Phase 4 —
   several are missing because CE itself never gives them ordinary chunk-decoration placement.**
   A repo-wide grep confirms `ore_schrabidium`, `ore_gneiss_schrabidium`, and `ore_nether_schrabidium`
   are never touched by `HbmWorldGen.generateOres` at all. Their real CE placement is three entirely
   different mechanisms, none of which are this report's job: a fixed block list inside the
   `DesertAtom001`/`DesertAtom003` decorative structures (`docs/phase4/
   worldgen_structures_bunkers_stations.md`'s territory), a chance-based "uranium ore → schrabidium
   ore" mutation performed by nuclear explosions (`com.hbm.explosion.ExplosionNukeGeneric`, already
   named in `docs/phase3/explosion_engine.md`), and a similar mutation performed by fallout rain
   (`com.hbm.entity.effect.EntityFallout`, already confirmed owned by `docs/phase4/
   fallout_rain_and_effects.md`, which explicitly lists all three schrabidium block fields under its
   own "ore-mutation tiers"). **`ore_gneiss_rare`, by contrast, has zero placement anywhere in all of
   CE** — not in `HbmWorldGen`, not in an explosion, not in fallout, not in a structure. It is
   registered, orphaned, dead content in CE itself (the same shape as `ore_bedrock_coltan` and
   `stone_biome`, both discussed below); there is nothing to port for it, and inventing world-gen for
   it would be adding content CE never had, not restoring parity. A future agent should not spend a
   work package hunting for "missing" schrabidium-ore or `ore_gneiss_rare` world-gen — three-quarters
   of that hunt belongs to other reports already closed out, and the last quarter doesn't exist.
2. **CE's `HbmWorldGen.generateOres` (755-line file, this method is ~160 lines of it) is the single
   real dispatcher for essentially every ordinary vein** — there is no per-ore `WorldGenerator` class;
   `com.hbm.world.gen.{MapGenNTMFeatures,NTMWorldGenerator}` (both read in full, 577 combined lines)
   are the **structure** jigsaw-dungeon engine (spire/bunker/vertibird/oil-rig/lighthouse/meteor-dungeon
   etc., already fully scoped by the two sibling structure/meteor reports) and contribute nothing to
   ore placement. Every ordinary vein is one `DungeonToolbox.generateOre(...)` call inside
   `HbmWorldGen.generateOres`, which itself is one of two methods NeoForge event-equivalent-registered
   `IWorldGenerator.generate(...)` calls — confirming PORT_SPEC's instinct that this is "one
   parametrized dispatcher driven by a config table," not 30 separate `WorldGenerator` classes.
3. **The "bedrock ore" family is not one mechanic but three, only one of which is genuinely blocked.**
   `BlockBedrockOre` (`ore_bedrock_coltan`, a single CE block) is **already fully ported** in this
   port (`src/main/java/com/hbm/blocks/generic/BlockBedrockOre.java`, 51 lines, a faithful port of
   CE's 54-line original) — but a repo-wide grep of CE confirms `ore_bedrock_coltan` **has no
   world-gen placement anywhere in CE either**; it is dead content exactly like `ore_gneiss_rare`,
   nothing to do here. `BlockBedrockOreTE`/`ore_bedrock_oil` (the fracking-tap resource) is **already
   registered** in this port (`OilChainBlocks.ORE_BEDROCK_OIL`, Phase 2) and its world-gen placement is
   **already fully scoped** by `docs/phase4/worldgen_oil_and_meteor_dungeons.md` Part 1 — not
   re-derived here. The one genuinely open piece is `ModBlocks.ore_bedrock_block`
   (`BlockBedrockOreTE`, 153 CE lines, read in full below) — the tiered, TE-backed *mineral* deposit
   `com.hbm.world.feature.BedrockOre` (194 lines, read in full) actually places, feeding the Excavator
   machine. **Its Block+TileEntity class does not exist anywhere in this port yet** (confirmed absent
   from every Phase 0-3 area grepped), which is a real, narrow, nameable blocking dependency this
   report documents precisely in Deferred scope — not something this report should silently invent a
   substitute for.
4. **`basalt_ore` and `stone_biome_desert`/`stone_biome_woodland` are not chunk-decoration ore-vein
   content at all**, despite living in `OreBlocks.java`'s own registration methods (`registerBasalt`/
   `registerBiomeStone`). `basalt_ore`'s only placement site in all of CE is
   `com.hbm.blocks.fluid.VolcanicBlock.onSolidify()` — a **live, ticking flowing-lava fluid block**
   that randomly solidifies into `basalt`/`basalt_ore` (sulfur/asbestos/fluorite/gem variants, by a
   weighted roll out of 200) as it cools, a volcano-terrain mechanic wholly unrelated to biome
   decoration and not owned by any Phase 4 report surveyed here (see Deferred scope).
   `stone_biome`/`BlockBiomeStone` has **zero references anywhere in CE outside its own class and
   registration** — dead content, most plausibly intended only as a decorative NBT-structure-schematic
   palette block that was never wired up. Neither needs (or should get) ore-vein world-gen designed
   for it by this report.

## Phase-4-safe scope

All numbers below are CE's own real defaults, read from `HbmWorldGen.java`/`CompatibilityConfig.java`/
`DungeonToolbox.java` — none invented. `DungeonToolbox.generateOre(world, rand, chunkMinX, chunkMinZ,
veinCount, amount, minHeight, variance, ore[, target])` runs `veinCount` independent placement
attempts per chunk-generation pass, each starting at a uniform-random `(x∈[chunkMinX,+16),
y∈[minHeight,minHeight+variance), z∈[chunkMinZ,+16))` and calling
`new WorldGenMinableNonCascade(ore, amount[, target]).generate(...)` there.

### Group A — ordinary "N attempts per chunk" ellipsoid veins (the majority)

`WorldGenMinableNonCascade` (173 lines, read in full) is a **near-verbatim port of vanilla's own
ellipsoid-blob ore-vein algorithm** (two random tangent points offset by `size/8` blocks, a
`sin(π·f)`-weighted radius profile swept along the segment between them) — CE's own in-file comment
flags exactly why it exists as a separate class: `// mlbv: vanilla WorldGenMinable DOES cascade` (1.12
vanilla's version overwrites already-placed ore of a different type; CE's checks the target block
identity — `Blocks.STONE` or a caller-supplied `target`, e.g. `stone_gneiss` — via
`isReplaceableOreGen`, so a later vein cannot stomp an earlier one). It also `extends
AbstractPhasedStructure` (the CE chunk-neighbor-wait scheduler already found unnecessary to port by
`docs/phase4/worldgen_oil_and_meteor_dungeons.md`'s own Key design decision — that same conclusion
applies here verbatim, see Key design decisions below) purely so a vein whose ellipsoid crosses a
chunk boundary defers until the neighbor chunk is generated.

| Ore Block(s) | CE config field (dim-0 default) | size | y-range | target |
|---|---|---|---|---|
| `ore_uranium`/`ore_uranium_scorched` (via `outgas`) | `uraniumSpawn` (7) | 5 | [5, 25) | stone |
| `ore_thorium` | `thoriumSpawn` (7) | 5 | [5, 30) | stone |
| `ore_titanium` | `titaniumSpawn` (8) | 6 | [5, 35) | stone |
| `ore_sulfur` | `sulfurSpawn` (5) | 8 | [5, 35) | stone |
| `ore_aluminium` | `aluminiumSpawn` (7) | 6 | [5, 45) | stone |
| `ore_copper` | `copperSpawn` (12) | 6 | [5, 50) | stone |
| `ore_fluorite` | `fluoriteSpawn` (6) | 4 | [5, 50) | stone |
| `ore_niter` | `niterSpawn` (6) | 6 | [5, 35) | stone |
| `ore_tungsten` | `tungstenSpawn` (10) | 8 | [5, 35) | stone |
| `ore_lead` | `leadSpawn` (6) | 9 | [5, 35) | stone |
| `ore_beryllium` | `berylliumSpawn` (6) | 4 | [5, 35) | stone |
| `ore_rare` | `rareSpawn` (6) | 5 | [5, 25) | stone |
| `ore_lignite` | `ligniteSpawn` (2) | 24 | [35, 60) | stone |
| `ore_asbestos` | `asbestosSpawn` (2) | 4 | [16, 32) | stone |
| `ore_cinnabar` | `cinnabarSpawn` (1) | 4 | [8, 24) | stone |
| `ore_cobalt` | `cobaltSpawn` (2) | 4 | [4, 12) | stone |
| `cluster_iron` | `ironClusterSpawn` (4) | 6 | [15, 60) | stone |
| `cluster_titanium` | `titaniumClusterSpawn` (2) | 6 | [15, 45) | stone |
| `cluster_aluminium` | `aluminiumClusterSpawn` (3) | 6 | [15, 50) | stone |
| `cluster_copper` | `copperClusterSpawn` (3) | 6 | [15, 35) | stone |
| `ore_australium` | `australiumSpawn` (**0 for dim 0** — CE's only nonzero default is keyed `-31:1`, an unidentified modded dimension; see below) | 3 | [14, 18) | stone |
| `stone_resource_limestone` | `WorldConfig.limestoneSpawn` (**already ported**, default 1) | 6 | [15, 35) | stone |
| `ore_nether_uranium`/`_scorched` (outgas) | `netherUraniumSpawn` (dim `-1`: 8) | 6 | y∈[0,127) flat | netherrack |
| `ore_nether_tungsten` | `netherTungstenSpawn` (dim -1: 10) | 10 | [0, 127) | netherrack |
| `ore_nether_sulfur` | `netherSulfurSpawn` (dim -1: 26) | 12 | [0, 127) | netherrack |
| `ore_nether_fire` | `netherPhosphorusSpawn` (dim -1: 24) | 6 | [0, 127) | netherrack |
| `ore_nether_coal` | `netherCoalSpawn` (dim -1: 24) | 32 | [16, 96) | netherrack — **block class `BlockNetherCoal` not read/ported anywhere in this port yet, a small additional gap this feature needs, see Deferred scope** |
| `ore_nether_cobalt` | `netherCobaltSpawn` (dim -1: 2) | 6 | fixed 100-126 | netherrack |
| `ore_nether_plutonium` | `netherPlutoniumSpawn` (dim -1: 8), gated by `GeneralConfig.enablePlutoniumOre` (**already ported** as `enablePlutoniumNetherOre`) | 4 | [0, 127) | netherrack |
| `ore_tikite` | `endTixiteSpawn` (dim `1`: 8) | 6 | [0, 127) | end stone |
| `ore_coltan` (ordinary vein, separate from the special deposit below) | gated by `GeneralConfig.enable528ColtanSpawn` (**not yet ported**, CE default `false`) × `GeneralConfig.coltanRate` (**not yet ported**, CE default 2) | 4 | [15, 40) | stone |

Gneiss-family veins are the same mechanic **plus a required substrate layer** — see Group C below.
Gas-bubble/alexandrite/oil/bedrock-oil/coltan-deposit/australium-treasure are the same
`DungeonToolbox`/`WorldGenMinableNonCascade` machinery but gated by a *chance* roll rather than an
unconditional attempt count — see Group B.

### Group B — "1-in-N chunks" chance-gated single deposits

These reuse the exact same placement primitives as Group A but are preceded by
`if (rand.nextInt(N) == 0)` rather than looping `N` times:

| Feature | CE config (dim-0 default) | Shape |
|---|---|---|
| `gas_flammable` bubble | `gasbubbleSpawn` (40), gated by `GeneralConfig.enableFlammableGas` (**already ported**) | 1 attempt, size 32, y∈[30,40) |
| `gas_explosive` bubble | `explosivebubbleSpawn` (80), gated by `GeneralConfig.enableExplosiveGas` (**already ported**) | 1 attempt, size 32, y∈[30,40) |
| `ore_alexandrite` | `alexandriteSpawn` (100) | 1 attempt, size 3, y∈[10,15) |
| Oil bubble / bedrock-oil / oil-sand | `oilBubbleSpawn`(100)/`bedrockOilSpawn`(200) | **Already fully scoped by `docs/phase4/worldgen_oil_and_meteor_dungeons.md` Part 1 — not re-derived** |
| Australium special deposit | unconditional per-chunk `rand.nextInt(4)` attempts | size 50, y∈[15,30), but only actually places inside a **hardcoded fixed world region** `x∈[-450,-350], z∈[-450,-350]` — a single, deliberate one-off "treasure zone" at fixed absolute coordinates, not a per-chunk-random feature; this is why `australiumSpawn`'s dim-0 default is 0 (see Group A) — CE's *real* overworld australium source is this fixed zone, not the ordinary per-chunk vein |
| Coltan rich deposit | gated by `GeneralConfig.enable528ColtanDeposit` (**not yet ported**, CE default `true`) | A Gaussian-scattered hotspot centered at `(1500·N(0,1), 1500·N(0,1))` from a `world.getSeed()+5`-seeded `Random`, 2 outer loop passes × 5 concentric rings (`colRange/r` for r=1..5) each rolling one `WorldGenMinableNonCascade(ore_coltan, 4)` if the current chunk's random point falls inside that ring — a genuinely different mechanic from every other entry in this table (fixed-seed-derived world-coordinate hotspot, not per-chunk-independent) |
| Netherrack "smoldering ore" scatter | unconditional, up to 30 attempts/chunk | **Not an ellipsoid vein at all** — picks a random `(x,z)` and a random depth `d∈[16,112)`, scans the 5-block column `[d-5,d]` for the first air-above-netherrack transition, and replaces that one netherrack block with `ore_nether_smoldering` (**already registered** in `GenericBlocks.java`, Phase 1) — a scattered single-block decoration, needs a small bespoke `Feature`, not `Feature.ORE` |

### Group C — the gneiss stratum + vein two-stage mechanic

Gneiss ores are **not** simple stone-target veins — CE places them in two independent passes that
must both be ported for gneiss ore to generate at all:

1. **`SchistStratum`** (74 lines, read in full), registered once via `MinecraftForge.EVENT_BUS.register(new
   SchistStratum(ModBlocks.stone_gneiss.getDefaultState(), 0.01D, 5, 8, 30))` in `MainRegistry`, listens
   to `DecorateBiomeEvent.Pre` and paints a wavy horizontal band of `stone_gneiss` centered at y=30
   (2D Perlin noise per XZ column determines a per-column thickness around that center), replacing any
   `Material.ROCK` block it finds — dimension 0 only, no config gate at all (always on).
2. Eight ordinary `DungeonToolbox.generateOre(..., ore, ModBlocks.stone_gneiss)` calls (Group A shape,
   `target = stone_gneiss` instead of stone) then place `ore_gneiss_iron`(`gneissIronSpawn`, 25),
   `ore_gneiss_gold`(`gneissGoldSpawn`, 10), `ore_gneiss_uranium`(`uraniumSpawn`×3), `ore_gneiss_copper`
   (`copperSpawn`×3), `ore_gneiss_asbestos`(`asbestosSpawn`×3), `ore_gneiss_lithium`(`lithiumSpawn`, 6),
   and `ore_gneiss_gas`(`gassshaleSpawn`×3) — all size 6 (10 for gas), y∈[30,40) — **inside** whatever
   `stone_gneiss` the stratum pass already laid down that chunk.

**Confirmed CE quirk, not a bug to silently fix**: CE's own 8th gneiss call (`HbmWorldGen.java` line
175) targets `ModBlocks.ore_gneiss_asbestos` **a second time**, gated by `CompatibilityConfig.rareSpawn`
(the *rare-earth-ore* config, default 6) rather than a dedicated field — meaning in real CE, changing
the "rare earth ore" spawn-rate slider *also* changes gneiss-asbestos density as an entangled side
effect, and `ore_gneiss_asbestos` gets placed by two independent config-gated passes while
`ore_gneiss_rare` (a real, separately-registered Block) never gets its own placement call at all. Flag
and preserve rather than "fix" — see Open questions.

**Blocking gap this report must name explicitly**: neither `stone_gneiss` nor `stone_porous` (the
sibling substrate `BedrockOilDeposit` also plants a companion vein into, per the sibling oil report)
is registered as a Block anywhere in this port yet (confirmed by grep of `GenericBlocks.java`/
`OreBlocks.java`). `stone_gneiss` is a trivial `BlockBase`-equivalent in CE (`Material.ROCK`, hardness
1.5 — this port's own `OreBlocks.GNEISS_HARDNESS = 1.5F` constant already anticipates this exact
value, confirming Phase 1 knew this block was coming but didn't register it) — recommend this
report's own implementation wave register it directly (it is small, standalone, and needed by nothing
else), rather than reopening Phase 1. `stone_porous` is `BlockPorous` (67 CE lines, not read in this
survey) — not this report's block to add (it belongs to `BedrockOilDeposit`'s already-scoped feature
in the sibling oil report); flagged here only because it shares this report's "target block doesn't
exist yet" shape.

### Group D — depth-ore family (`DepthDeposit`, distinct sizing math from Group A)

`DepthDeposit` (168 lines, read in full) is a **solid sphere blob with a hollow shell**, not an
ellipsoid vein: for every lattice point within `size` blocks of the origin, if
`√(dx²+dy²+dz²) + rand.nextInt(2) < size·fill` it becomes the ore block; else if `... ≤ size` it
becomes a `filler` block (`stone_depth`/`stone_depth_nether`, **already registered**, Phase 1); the
replaceable-check tests against **both** `Blocks.BEDROCK` and the caller's `genTarget`
(`Blocks.STONE`/`Blocks.NETHERRACK`) — CE's own comment: `//yes you've heard right, bedrock`, meaning
a depth deposit can carve into isolated pre-existing bedrock the same as ordinary stone. Every call is
gated `if (rand.nextInt(chance) != 0) return;` (a "1-in-N chunks" roll, like Group B), then samples one
random point `(x∈chunk, y∈[yMin,yMin+yDev), z∈chunk)`:

| Ore Block(s) | Overworld/Nether | chance | size | fill | y-range | filler |
|---|---|---|---|---|---|---|
| `cluster_depth_iron` | overworld | 24 | 5 | 0.6 | [0,3) | `stone_depth` |
| `cluster_depth_titanium` | overworld | 32 | 5 | 0.6 | [0,3) | `stone_depth` |
| `cluster_depth_tungsten` | overworld | 32 | 5 | 0.6 | [0,3) | `stone_depth` |
| `ore_depth_cinnabar` | overworld | 16 | 5 | 0.8 | [0,3) | `stone_depth` |
| `ore_depth_zirconium` | overworld | 16 | 5 | 0.8 | [0,3) | `stone_depth` |
| `ore_depth_borax` | overworld | 16 | 5 | 0.8 | [0,3) | `stone_depth` |
| `ore_depth_nether_neodymium` | nether (×2 y-bands) | 16 | 7 | 0.6 | [0,3)/[125,128) | `stone_depth_nether` |
| `ore_depth_nether_nitan` | nether (×2 y-bands) | 16 | 7 | 0.6 | [0,3)/[125,128) | `stone_depth_nether` |

`ore_alexandrite` is registered by `OreBlocks.registerDepthOres()` too but is **not** placed via
`DepthDeposit` — it's the ordinary Group B vein (`alexandriteSpawn`) listed above; only the eight rows
above use the sphere-blob mechanic. All eight target/filler blocks are already registered
(`stone_depth`/`stone_depth_nether`, Phase 1) — this group has **zero blocking Block dependencies**,
only the placement code itself is missing.

### Group E — adjacent-but-distinct: `BlockResourceStone` noise-layer family (not `OreBlocks.java`)

`OreCave` (209 lines) and `OreLayer3D` (137 lines), both read in full, are a genuinely separate
mechanic from everything above: 2D/3D Perlin-noise-driven `DecorateBiomeEvent.Pre` listeners that
paint **`BlockResourceStone`** metadata variants (this port's already-flattened `stone_resource_sulfur`/
`_asbestos`/`_hematite`/`_malachite`/`_bauxite`, `GenericBlocks.java`, Phase 1 — **not** an
`OreBlocks.java` block, a different class entirely) rather than discrete veins:

- `OreCave` carves a **cave-like cavity boundary decoration** — for each XZ column, a 2D noise value
  above a threshold defines a vertical range around a fixed `yLevel`; any exposed rock face touching
  air/another cave within that range gets replaced with the ore state (sulfur: `threshold=1.5,
  rangeMult=20, maxRange=20, yLevel=30`, gated `WorldConfig.enableSulfurCave`, **already ported**;
  asbestos: `threshold=1.75`, same ranges, `yLevel=25`, gated `WorldConfig.enableAsbestosCave`,
  **already ported**) — optionally also carving a paired fluid pocket (sulfuric-acid block, for
  sulfur only) plus `stalactite`/`stalagmite` decoration on unrelated air pockets nearby.
- `OreLayer3D` is a genuine 3D noise volume (three independent Perlin fields multiplied together,
  `nX·nY·nZ > threshold`) replacing solid rock in `y∈[6,64]` with hematite (`scaleH=0.04, scaleV=0.25,
  threshold=230`, gated `WorldConfig.enableHematite`)/bauxite(`0.03/0.15/300`,
  `WorldConfig.enableBauxite`)/malachite(`0.1/0.15/275`, `WorldConfig.enableMalachite`) — all three
  gate flags **already ported**.

All five `stone_resource_*` blocks (plus `stone_resource_limestone`, Group A) are registered with zero
placement, exactly the same gap shape as this report's primary ask, sharing the same `WorldConfig`
toggles — flagged here for completeness since a future agent reading only `OreBlocks.java` would miss
that this sibling family exists and needs the same treatment, but this report treats it as secondary
to the literal `OreBlocks.java` ask.

## The bedrock-ore family in detail (fundamentally different placement mechanic — confirmed)

`com.hbm.world.feature.BedrockOre` (194 lines, read in full) confirms the task's hypothesis
completely: bedrock ore is placed **only at/adjacent to `y=0` (the bedrock layer)**, never as an
ordinary underground vein, and by a **deterministic, not per-seed** density function:

- **Placement geometry**: `executeOriginalLogic` scans a 3×3 XZ patch at `y=0`; the center column is
  always converted if it's `Blocks.BEDROCK`, the 8 neighbors each get a 50/50 chance
  (`rand.nextBoolean()`); every converted position becomes `ModBlocks.ore_bedrock_block` with a fresh
  `TileEntityBedrockOre` (resource/color/shape/acid/tier fields, all set from the calling tier). It
  then sweeps a `7×7×6` column above (`y∈[1,7)`) converting any `Blocks.STONE`/`Blocks.BEDROCK` into a
  tier-specific "depth rock" (`stone_depth`, already registered) — the visible "you've struck a rich
  seam" cosmetic shell around the actual resource block.
- **Tier selection is a fixed-seed 2D Perlin density scan, independent of the world seed**:
  `ItemBedrockOreBase.getOreLevel(x, z, type)` (71 lines, read in full) uses `new
  NoiseGeneratorPerlin(new Random(2114043), 4)` and `new NoiseGeneratorPerlin(new Random(2082127 +
  type.ordinal()), 4)` — **literal integer constants, not `world.getSeed()`**. `BedrockOre.generateAuto`
  averages this density across all 6 `BedrockOreType` values (`LIGHT_METAL`/`HEAVY_METAL`/
  `RARE_EARTH`/`ACTINIDE`/`NON_METAL`/`CRYSTALLINE`, **already ported** as items in
  `com.hbm.items.special`) and buckets the result into tier 1 (≤0.75, plain), 2 (>0.75, water-drill-
  required), 3 (>1, sulfuric-acid-required), or 4 (>1.5, solvent-required) — `getTier`/`getBoreFluid`
  read directly. **This means every CE world, regardless of seed, has bedrock-ore density hotspots at
  the exact same X/Z coordinates** — worth confirming as intentional (it lets community-shared
  coordinate maps of "good bedrock ore spots" be universally valid) rather than assuming it's a bug to
  fix during the port; see Open questions.
- **Trigger**: `HbmWorldGen.generateOres` rolls `rand.nextInt(10)==0` per chunk (gated
  `WorldConfig.newBedrockOres`, **already ported** as `NEW_BEDROCK_ORES`, default `true`) and picks one
  near-chunk-center XZ point — the tier is then fully determined by that point's density, not by the
  roll itself.
- **Nether variant is the same mechanic, different resource table**: `BedrockOre.getWeightedNetherOre`
  uses `Library.nextIntDeterministic(world.getSeed(), chunkX, chunkZ, total)` (a *seed-dependent*,
  chunk-keyed deterministic weighted pick — confirmed a genuinely different RNG source from the
  overworld tier scan above) to choose among glowstone dust ×4 (weight
  `WorldConfig.bedrockGlowstoneSpawn`, **already ported**, default 100), `powder_fire` ×4 (weight
  `bedrockPhosphorusSpawn`, **already ported**, default 50), or vanilla quartz ×4 (weight
  `bedrockQuartzSpawn`, **already ported**, default 100), all still going through the exact same
  `executeOriginalLogic`/`ore_bedrock_block` placement code — same trigger roll, same `y=0` geometry.

**Blocking dependency, named precisely**: `ModBlocks.ore_bedrock_block` (`BlockBedrockOreTE`, CE 153
lines, read in full) does not exist anywhere in this port. It is a small `BlockContainer`-equivalent
whose paired `TileEntityBedrockOre` holds exactly 5 fields (`ItemStack resource`, `FluidStack
acidRequirement` nullable, `int tier`, `int color`, `int shape`) with no inventory and no ticking
logic — only NBT read/write (maps onto this port's already-confirmed `BlockEntity`
`saveAdditional`/`loadAdditional` pattern per `docs/phase2/blockentity_base.md`, **not** a Data
Component — this is block-entity state, not `ItemStack` state) plus a walk-on-fire touch effect and an
`ILookOverlay` HUD hint (Phase 5). This report's placement code cannot run without this class existing;
it is small enough (one block class + one ~70-line TE) to be its own tiny prerequisite package, but is
not itself a world-gen concern — flagged here, not designed here. Its downstream consumer, the
Excavator machine (`TileEntityMachineExcavator`, CE class, drives `IMiningDrill`/`IDrillInteraction`
against it — those two interfaces **are** already ported and used by this port's own
`BlockBedrockOre`/`BlockCluster`), is a separate machine concern entirely absent from this port and
not required for `ore_bedrock_block` to simply exist in freshly-generated terrain.

`ore_bedrock_oil`'s placement (`BedrockOilDeposit`, same file family, same `y≈0` geometry, a
Manhattan-distance diamond region rather than a 3×3-plus-column shape) is **already fully scoped** by
`docs/phase4/worldgen_oil_and_meteor_dungeons.md` — not re-derived here; that report's own
"Deferred scope" item already names `com.hbm.world.feature.BedrockOre` (this report's subject) as the
sibling feature confirming `ore_bedrock_block`'s real placement site, and this report in turn does not
re-scope oil.

The much larger `BedrockOreRegistry`(220 lines)/`BedrockOreJsonConfig`(174 lines)/`ItemBedrockOreNew`'s
`BedrockOreGrade`/`ProcessingTrait` machinery (365 lines, `@Spaghetti("everything")` in CE's own
source) is the **ore-refining/processing chain** for whatever a drilled bedrock-ore tile yields (base →
roasted/washed → primary → sulfuric/solvent variants, an oredict-driven, mod-compatibility-aware
multi-step system) — **already ported as items** in `com.hbm.items.special` per this port's own file
list, and entirely orthogonal to world-gen placement; not touched further here.

## Deferred scope

Real dependencies of *this specific* report that belong to other packages/phases/reports:

- **`ModBlocks.ore_bedrock_block` (`BlockBedrockOreTE` + `TileEntityBedrockOre`)** — a small,
  standalone block+TE class (see above), not yet owned by any completed phase. Recommend a narrow
  prerequisite work item (block + TE + registration, no GUI, no ticking) land before or alongside this
  report's own implementation wave; it is not itself a world-gen task.
- **The Excavator machine (`TileEntityMachineExcavator`)** — the real consumer of `ore_bedrock_block`/
  `ore_bedrock_coltan`'s drill interaction. Entirely absent from this port (confirmed by grep). Not
  required for this report's placement code to compile or run; a machine-package concern for whichever
  phase revisits Phase 2-style content (Phase 2 itself is already closed per this project's own
  tracking, so this is a genuine gap needing an explicit owner — flagged loudly rather than silently
  assumed covered).
- **`ore_bedrock_oil` world-gen, `OilBubble`/`OilSandBubble`, the crater biome, `Meteorite`/
  `MeteoriteStructure`** — all already fully owned by `docs/phase4/worldgen_oil_and_meteor_dungeons.md`.
  Do not re-scope any of it here; this report's only touchpoint with that report is the shared
  `com.hbm.world.feature.BedrockOre` file and the shared "no phased-structure substrate needed" design
  call (see Key design decisions).
- **`basalt_ore`'s real placement mechanic (`VolcanicBlock.onSolidify`)** — a live fluid-block/lava
  behavior, not chunk-decoration. `ModBlocks.basalt`/`volcanic_lava_block`/`VolcanicBlock` are not
  confirmed present in this port and no Phase 1-4 report surveyed here claims ownership of the
  "volcano" mechanic explicitly. Flagged as a genuine, currently-unassigned gap — not this report's to
  solve (it is not ore-vein world-gen by any reasonable reading), but worth a future STATUS.md note so
  it isn't silently lost between phases.
- **`stone_biome_desert`/`stone_biome_woodland` (`BlockBiomeStone`)** — dead/unplaced content in CE
  itself; if it has any real consumer, it would be a decorative palette entry inside one of the
  `JigsawPiece`/`.nbt` structure schematics, which is `docs/phase4/
  worldgen_structures_bunkers_stations.md`'s territory, not this report's. No action recommended
  beyond noting it needs no ore-vein world-gen.
- **Schrabidium-tier ore mutation (`ExplosionNukeGeneric`, `EntityFallout`) and the `DesertAtom001`/
  `DesertAtom003` decorative structures** — already confirmed owned by `docs/phase3/
  explosion_engine.md`, `docs/phase4/fallout_rain_and_effects.md`, and `docs/phase4/
  worldgen_structures_bunkers_stations.md` respectively (see Headline finding 1). Not re-derived here.
- **`stone_porous` (`BlockPorous`, 67 CE lines)** — needed by `BedrockOilDeposit`'s companion vein,
  already the sibling oil report's scope; flagged here only as a shared "target block missing" note.
- **`ore_nether_coal` (`BlockNetherCoal`)** — not read in this survey, confirmed absent from this port.
  A small, narrow prerequisite this report's own implementation wave should register alongside
  `stone_gneiss` (both are simple, this-feature-only blocks with no other consumer found).
- **`CompatibilityConfig`'s entire per-dimension ore-frequency map** (`uraniumSpawn`, `titaniumSpawn`,
  ... all ~40 ore-relevant keys used in the tables above) — this port's own `CompatibilityConfig.java`
  javadoc (read in full) already documents this exact deferral: CE's dimension-ID-keyed maps were not
  re-keyed to `ResourceKey<Level>` "pending whichever phase owns world generation, once it knows the
  real set of dimensions" — that phase is this one. This report's own implementation wave must add
  these fields (re-keyed by `ResourceLocation`/`ResourceKey<Level>` string, reusing
  `ConfigUtil.toIntMap`'s existing `"key:value"` encoding, exactly as the sibling oil report already
  recommended for its own two fields) before any of the numbers in the tables above can be
  server-operator-tunable like they are in CE. Until then, hardcoding CE's dim-0/dim-`-1`/dim-`1`
  defaults (as Neo Edition does for its 4 already-ported features) is the fallback, with the same loss
  of per-dimension configurability the sibling report already flagged.
- **`GeneralConfig.enable528ColtanDeposit`/`enable528ColtanSpawn`/`coltanRate`/
  `enable528ColtanSpawn`-adjacent "528 mode" fields** — confirmed not yet ported (grepped this port's
  `GeneralConfig.java` and found no match), gating the coltan rich-deposit and the ordinary coltan vein
  respectively. A small, narrow config addition this report's implementation wave should make.
- **`Library.nextIntDeterministic(long seed, int chunkX, int chunkZ, int bound)`** — CE's 13-line
  splitmix64-derived bounded-uniform chunk-seeded RNG helper (`HashCommon.murmurHash3` + Lemire's
  method), used only by `BedrockOre.getWeightedNetherOre`. Confirmed absent from this port's own
  `Library.java` by grep. Trivial to add; flagged so a future agent doesn't have to re-derive the
  algorithm from scratch.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and Neo
Edition's parallel world-gen port for NeoForge API shape — no NeoForge API is invented below):

- **PORT_SPEC's "ores/features via datapack features + `BiomeModifier`" is a real, already-proven
  mechanism in this exact port, not a hypothetical.** `src/main/java/com/hbm/datagen/
  ModDataGenerators.java` (78 lines, read in full) already runs a `RegistrySetBuilder` +
  `DatapackBuiltinEntriesProvider` for `Registries.DAMAGE_TYPE` (`ModDamageTypes::bootstrap`) — the
  *exact same* API this report's ore/bedrock-ore features need for `Registries.CONFIGURED_FEATURE`,
  `Registries.PLACED_FEATURE`, and `NeoForgeRegistries.Keys.BIOME_MODIFIERS`. A future implement-wave
  agent's registration work is: write `bootstrap(BootstrapContext<...>)` methods analogous to
  `ModDamageTypes.bootstrap`, one per registry tier, and add three more
  `registrySetBuilder.add(Registries.X, MyClass::bootstrap)` lines to `ModDataGenerators.gatherData`
  next to the existing damage-type line — no new datagen plumbing needed, only new bootstrap classes.
  Running `gradlew runData` (blocked in this sandbox, confirmed unavailable per this task's own
  environment notes) is what actually serializes these Java bootstrap calls into the real, on-disk
  `data/hbm/worldgen/{configured_feature,placed_feature}/*.json` and
  `data/hbm/neoforge/biome_modifier/*.json` files the game loads at runtime — **this is genuinely
  datapack-JSON-driven, not code-driven placement**: the shipped mod jar's actual world-gen behavior
  comes from those generated JSON files (indistinguishable at runtime from a hand-authored resource
  pack's datapack), the Java `bootstrap` methods are only how those files get *authored*, exactly
  mirroring how `ModDamageTypes` already produces real `DamageType` JSON entries today.
- **`upstream/neo-edition`'s `NtmConfiguredFeatures`/`NtmPlacedFeatures`/`NtmBiomeModifiers` (41+42+41
  lines, all read in full) confirm the exact four-stage pipeline and real class/method names**:
  `Feature<FC> → context.register(ResourceKey<ConfiguredFeature<?,?>>, new ConfiguredFeature<>(feature,
  config))` → `context.register(ResourceKey<PlacedFeature>, new PlacedFeature(configuredFeatureHolder,
  List<PlacementModifier>))` → `context.register(ResourceKey<BiomeModifier>, new
  AddFeaturesBiomeModifier(biomeHolderSet, placedFeatureHolderSet, GenerationStep.Decoration))`. Real,
  confirmed placement-modifier classes actually used by neo-edition: `InSquarePlacement.spread()`,
  `HeightRangePlacement.uniform(VerticalAnchor.absolute(a), VerticalAnchor.absolute(b))`,
  `HeightmapPlacement.onHeightmap(Heightmap.Types.WORLD_SURFACE_WG)`, `RarityFilter.onAverageOnceEvery(n)`.
  **`CountPlacement`/`Feature.ORE`/`OreConfiguration`/`OreConfiguration.target(RuleTest, BlockState)`
  are NOT used anywhere in neo-edition or this port** (neither has ported an ordinary vein-shaped ore
  yet) — these names are well-established, high-confidence Mojang-mapping knowledge from general
  1.18+ vanilla ore-gen (the vanilla `minecraft:ore` feature type, unchanged in shape since 1.18), but
  **not verified against a real compiled jar in this sandbox**, flagged explicitly per this task's own
  ground rules. A future agent should confirm the exact `OreConfiguration`/`TargetBlockState` field
  names against a real NeoForge 1.21.1 dependency before writing datagen code against them.
- **Ordinary vein ores do not need a custom `Feature` subclass at all — if CE's live per-dimension
  config tunability is *not* preserved.** `WorldGenMinableNonCascade`'s ellipsoid-blob algorithm (this
  report's own read, above) is shape-for-shape the same algorithm vanilla's own `Feature.ORE`/
  `OreConfiguration` implements (both are the same 1.12-era `WorldGenMinable` lineage) — a plain
  `ConfiguredFeature<OreConfiguration, ?>` referencing the vanilla `minecraft:ore` feature, with a
  `PlacedFeature` chaining `CountPlacement.of(n)` (or `RarityFilter` for the "1-in-N-chunks" Group B
  entries) + `InSquarePlacement.spread()` + `HeightRangePlacement.uniform(...)`, reproduces Group A's
  numbers exactly as pure JSON with zero Java `Feature` class — this is the simplest, most
  "datapack-JSON-driven" reading of PORT_SPEC's instruction.
- **However, that simplicity has a real, explicit cost: `CountPlacement`/`RarityFilter` values are
  baked into the generated JSON at datagen time and cannot read a live, server-operator-editable TOML
  config value at world-gen time** — the same tension the sibling oil report already surfaced and
  resolved by following neo-edition's own precedent (`OilBubbleFeature.place()` does its own
  `random.nextInt(liveConfigValue)` roll internally, still registered through the full
  `ConfiguredFeature`/`PlacedFeature`/`BiomeModifier` pipeline, just with a thin custom
  `Feature<NoneFeatureConfiguration>` instead of the vanilla `Feature.ORE`). Since literally every
  number in this report's Group A/B/C/D tables is sourced from a *currently-mutable*
  `ModConfigSpec.IntValue`/`BooleanValue` in this port's own `CompatibilityConfig`/`WorldConfig`/
  `GeneralConfig` (not a compile-time constant), achieving full CE parity — an operator edits the TOML
  file and sees the new frequency without needing new datapack JSON or a restart-with-regenerated-data
  — requires this report's ore/bedrock-ore features to follow neo-edition's thin-custom-`Feature`
  pattern uniformly, even for the otherwise-trivial vanilla-`Feature.ORE`-shaped Group A entries. This
  is a real fork to decide explicitly, not implicitly: **(A)** one thin custom `Feature` per
  ore-family/tier (CE parity, matches the port's own general "preserve every config knob" ethos, matches
  neo-edition's own already-established pattern for its 4 ported features) vs **(B)** bake CE's current
  defaults as pure vanilla `Feature.ORE`/`OreConfiguration` JSON and accept losing live
  per-dimension/per-server tunability (simpler, fewer Java classes, but a real CE-parity regression, and
  the choice neo-edition itself made for its own 4 features — "neither neo-edition feature reads any
  live config value," already flagged as a real, explicit design choice by the sibling oil report).
  Recommend (A) for consistency with this port's own config system and with neo-edition's actual
  precedent, but this is a call for whoever plans the implementation wave to make explicitly.
- **The chunk-neighbor-wait scheduler (`com.hbm.world.phased.*`, `AbstractPhasedStructure`'s base) does
  not need porting for this report's features either** — every Group A/C/D class this report covers
  (`WorldGenMinableNonCascade`, `DepthDeposit`, `BedrockOre`) extends `AbstractPhasedStructure` for the
  same reason `OilBubble`/`BedrockOilDeposit` do (an ellipsoid/sphere/diamond shape can cross a chunk
  boundary before the neighbor chunk exists in 1.12's synchronous single-chunk generation model). The
  sibling oil report's own Key design decision — a plain `Feature<FC>` writing directly via
  `WorldGenLevel#setBlock` needs no such scheduler, because vanilla's own `WorldGenRegion` already
  tolerates a feature writing a small buffer beyond its origin chunk — applies identically here. Do not
  port `com.hbm.world.phased.*` for this report's sake; it would be pure unnecessary complexity.
- **`OreCave`/`OreLayer3D`/`SchistStratum`'s `DecorateBiomeEvent.Pre`-listener shape has no direct
  1.21.1 event equivalent** (Forge 1.12's `DecorateBiomeEvent` was folded into the modern
  `Feature`/`PlacementModifier` pipeline entirely — there is no NeoForge event by that name). These
  three should be re-expressed as ordinary `Feature<NoneFeatureConfiguration>`s using
  `WorldGenLevel#getBlockState`/`setBlock` directly inside `place()`, iterating the feature's own
  origin-chunk XZ range exactly as CE's handler iterated `event.getChunkPos()` — no event registration
  needed at all once expressed this way, consistent with every other feature in this report.

## Open questions / risks

- **Bedrock-ore density being fixed-seed rather than world-seed-derived** (`ItemBedrockOreBase.
  getOreLevel`'s literal `2114043`/`2082127+ordinal` constants) — confirmed real CE behavior by reading
  the method directly, not an artifact of this survey. Worth an explicit decision: preserve exactly
  (every CE-based server/SMP world has bedrock-ore hotspots at literally the same coordinates,
  matching any community-shared "bedrock ore map") vs. quietly seed it from `world.getSeed()` instead
  (a plausible "fix," but a real behavior change from CE, and CE's own nether variant *does* use
  `world.getSeed()` for its type roll, suggesting this asymmetry might not even be intentional in CE).
  Flagged, not resolved — matches this project's own convention of surfacing this kind of CE quirk
  rather than silently normalizing it.
- **`ore_gneiss_asbestos`'s double-placement via two independent config fields**
  (`asbestosSpawn`×3 and, separately, `rareSpawn` unmodified) while its sibling `ore_gneiss_rare` is
  never placed at all (Group C) — confirmed by direct line-by-line read of `HbmWorldGen.java`, not
  inferred. Decide explicitly whether to preserve this exact quirk (byte-for-byte CE parity, including
  its apparent copy-paste error) or treat `ore_gneiss_rare` as the "obviously intended" target of that
  8th call and fix it during the port — either is defensible, but silently doing either without a
  documented decision risks a future reviewer treating the other as the bug.
- **`australiumSpawn`'s dim-0 default of 0** means the *ordinary* per-chunk australium vein is a
  complete no-op on the overworld under CE's own shipped defaults — the real overworld australium
  source is the hardcoded `x∈[-450,-350], z∈[-450,-350]` treasure zone (Group B). Confirm this is
  intentional CE design (a single deliberate "rare zone" easter egg) rather than assuming the ordinary
  vein is the "real" mechanism and the hardcoded zone is legacy cruft to drop — both blocks are real,
  live, currently-shipping CE content and this report recommends porting both exactly as found.
  Whether the fixed coordinate zone should be dimension-relative in some other way in a
  `ResourceKey<Level>` world is an open call for whoever re-keys `CompatibilityConfig`'s maps (see
  Deferred scope) — this report does not resolve it.
- **The `Feature<FC>`-per-config-value-read design (Key design decisions, fork A vs B) is the single
  highest-leverage decision for this whole report** — it determines whether ~40 ore/deposit types each
  need a bespoke Java `Feature` class (fork A, more code, full parity) or a shared vanilla
  `Feature.ORE` config-plus-JSON-only approach (fork B, far less code, loses live tunability). Given
  the sheer count of ore types this report covers (more than any single feature the sibling oil report
  had to decide this for), this decision should be made once, explicitly, before the implementation
  wave starts splitting work packages — not discovered independently by whichever agent happens to
  implement the first ore.
- **`OreConfiguration`/`RuleTest`/`TargetBlockState`'s exact field and method names are unverified
  against a real jar** in this sandbox (no compiled NeoForge/Minecraft dependency available, per this
  task's own environment constraints) — flagged per the task's ground rules as well-established
  Mojang-mapping knowledge, not confirmed compiling code. A future implement-wave agent's first ore
  feature should be treated as the point where this gets confirmed for real (or corrected) before the
  remaining ~40 entries are written against the same assumed shape.
- **`ore_nether_coal`'s `BlockNetherCoal` class was not read in this survey** — flagged so a future
  agent doesn't assume it is a plain `BlockNTMOre`-equivalent; CE's constructor signature
  (`BlockNetherCoal(false, 5, true, "ore_nether_coal")`) takes three unexplained parameters ahead of
  the name, worth reading in full before registering it.
