# ModBlocks.java - Generative / Ore Resource Blocks Research

Scope: `com.hbm.blocks.ModBlocks` (CE, 1617 lines), read end-to-end. Focus area is the ore/resource/material-storage
portion of the file (roughly lines 300-690, plus the meteor/basalt tail at 681 and the `Material Blocks` /
`Deco blocks` sections at 436-513). The remaining ~900 lines (machines, RBMK, fusion, doors, cables, rails, bombs,
turrets, drones, fluids...) are individually hardcoded, TileEntity-and-multiblock-coupled content that belongs to a
different research area (machine casings) - they are mentioned only where needed to draw the Phase 1/Phase 2 line.

## 1. Complete inventory of block families

### 1a. True "one class, many instances" ore families (backing pattern = shared Block subclass, NOT metadata)

None of these use block metadata to encode material - CE already gives every ore/material variant its own
`Block` instance and registry name. The "generative" opportunity here is a **registration-table loop** (data
describing name/hardness/harvest-level/drop), not metadata flattening - there is nothing to flatten, each variant
is already a distinct object.

| Family | Backing class | File | Instances in ModBlocks | Notes |
|---|---|---|---|---|
| Standard ore (`ore_*`) | `BlockNTMOre extends BlockOre` | `blocks/generic/BlockNTMOre.java` | ~30 (`ore_thorium`, `ore_titanium`, `ore_sulfur`, `ore_niter`, `ore_copper`, `ore_tungsten`, `ore_aluminium`, `ore_fluorite`, `ore_lead`, `ore_beryllium`, `ore_lignite`, `ore_asbestos`, `ore_rare`, `ore_cobalt`, `ore_cinnabar`, `ore_coltan`, `ore_australium`, `ore_oil`, `ore_tikite`, `ore_gneiss_*` x8, `ore_nether_cobalt`, `ore_nether_tungsten`, `ore_nether_sulfur`, `ore_nether_fire`, `ore_nether_plutonium`, `ore_nether_schrabidium`, `ore_uranium`/`_scorched` via `BlockOutgas` variant, `waste_planks` (odd reuse, see below)) | Constructor: `(name, IOreType oreEnum, harvestLvl, xp)`. `oreEnum` (nullable) supplies a custom quantity+drop function; when null it falls back to vanilla `getItemDropped`/`damageDropped`. This is the direct analogue of a modern `OreBlock`. |
| Ore clusters (`cluster_*`) | `BlockCluster extends BlockNTMOre` | `blocks/generic/BlockCluster.java` | 4 (`cluster_iron`, `cluster_titanium`, `cluster_aluminium`, `cluster_copper`) | Adds `IDrillInteraction` (mining-drill API) and silent (non-`doTileDrops`-aware) manual `harvestBlock`. Still just `Block`, no TE. |
| Depth-stratum ore (`ore_depth_*`, `cluster_depth_*`) | `BlockDepthOre extends BlockDepth` | `blocks/generic/BlockDepthOre.java` | 8 (`ore_depth_cinnabar`, `ore_depth_zirconium`, `ore_depth_borax`, `ore_alexandrite`, `cluster_depth_iron`, `cluster_depth_titanium`, `cluster_depth_tungsten`, `ore_depth_nether_neodymium`, `ore_depth_nether_nitan`) | `BlockDepth` (plain `Block`, `Material.ROCK`, unbreakable except by `IDepthRockTool`-implementing tools) is itself a distinct "world-context" stone reused for `stone_depth`/`stone_depth_nether`/`depth_brick`/`depth_tiles`/etc. No metadata, no TE. |
| Bedrock-tier ore | `BlockBedrockOre` / `BlockBedrockOreTE` | `blocks/generic/` | 2 (`ore_bedrock_coltan`, `ore_bedrock_block`) | `BlockBedrockOreTE` **does** attach a TileEntity (per name) - flagged below as the one ore-adjacent exception that needs a TE decision. |
| Meteor ore | `BlockNTMOre` (reused) + dedicated `BlockMeteorOre` | n/a | `block_meteor`, `block_meteor_cobble`, `block_meteor_broken`, `block_meteor_treasure` (all `BlockNTMOre` with `OreEnum`), `ore_meteor` (`BlockMeteorOre`, likely a vanilla-ore-in-meteor-biome retexture) | Not metadata-driven either; each is its own registry entry. |
| Basalt volcanic ore | `BlockOreBasalt extends BlockEnumMeta<EnumBasaltOreType>` | `blocks/generic/BlockOreBasalt.java` | **1 CE block, 5 metadata submeta variants** (`SULFUR`, `FLUORITE`, `ASBESTOS`, `GEM`, `MOLYSITE`) | **This is the one genuine metadata-flattening case in the ore family.** `EnumBasaltOreType` (`BlockEnums.java`) drives per-meta texture (`ore_basalt.<name>`) and per-meta drop (`oreType.getDrop()`/`getDropCount()`). Must become **5 distinct registry entries** in the port (e.g. `basalt_ore_sulfur`, `basalt_ore_fluorite`, `basalt_ore_asbestos`, `basalt_ore_gem`, `basalt_ore_molysite`), each a plain ore-style block with its fixed drop hardcoded (or driven by the same `IOreType`/drop-function idea `BlockNTMOre` uses). |
| Sellafield contaminated ore | `BlockSellafieldOre extends BlockSellafieldSlaked` | `blocks/generic/BlockSellafieldOre.java` | 5 (`ore_sellafield_emerald`, `_uranium_scorched`, `_schrabidium`, `_diamond`, `_radgem`) | **Not** a material-metadata block in the flattening sense - the "meta"/`VARIANT` machinery here (`BlockSellafieldSlaked`, `META_COUNT=4`, `PropertyRandomVariant`) is a **position-hashed random texture variant + a 0-15 "shade" contamination property** for visual variety, unrelated to material choice. Each of the 5 ore types is its own hardcoded `Block` instance; the drop is an `if (this == ModBlocks.X)` chain inside the shared class, not enum-driven. Needs its own random-texture-variant + tint rendering solution in 1.21 datagen (weighted model variants + a blockstate `IntegerProperty` for shade), independent of the material/ore work. |
| Terrain "biome stone" | `BlockBiomeStone` | `blocks/generic/BlockBiomeStone.java` | **1 CE block, 2 metadata submeta variants** (`EnumBiomeType.DESERT`, `WOODLAND`) | Second genuine metadata-flattening case, though it's a decorative terrain stone, not literally an ore. Must become 2 distinct blocks (`stone_biome_desert`, `stone_biome_woodland`) plus the "COVERED" `PropertyBool` (a real rendering-only blockstate property, keep as-is per block). |

### 1b. Material-driven storage / ingot-block family (the real generative opportunity)

CE's `//Material Blocks` section (lines ~436-490) is a straight analogue of vanilla `iron_block`/`gold_block`:
one full-cube storage block per crafting material. **This is not metadata-driven in CE** (each is its own hardcoded
`Block` field, e.g. `block_titanium`, `block_uranium`), but Phase 0's `Mats.java` already tags the exact set of
materials that need one, via `.setAutogen(..., MaterialShapes.BLOCK, ...)`:

```
grep -c "setAutogen([^)]*BLOCK[^)]*)" Mats.java  ->  57 materials flagged
```

Examples already flagged in Phase 0: `MAT_IRON`, `MAT_GOLD`, `MAT_URANIUM`, `MAT_U233/235/238`, `MAT_THORIUM`,
`MAT_PLUTONIUM`, `MAT_PU238/239/240/241`, `MAT_NEPTUNIUM`, `MAT_POLONIUM`, `MAT_TECHNETIUM`, `MAT_RADIUM`,
`MAT_SCHRABIDIUM`, `MAT_SOLINIUM`, `MAT_SCHRABIDATE`, `MAT_SCHRARANIUM`, `MAT_GHIORSIUM`, `MAT_TITANIUM`,
`MAT_COPPER`, `MAT_TUNGSTEN`, `MAT_ALUMINIUM`, `MAT_LEAD`, `MAT_BISMUTH`, `MAT_TANTALIUM`, `MAT_NEODYMIUM`,
`MAT_NIOBIUM`, `MAT_BERYLLIUM`, `MAT_EMERALD`, `MAT_COBALT`, `MAT_BORON`, `MAT_LANTHANIUM`, `MAT_ZIRCONIUM`,
`MAT_LITHIUM`, `MAT_SULFUR`, `MAT_KNO` (niter), `MAT_FLUORITE`, `MAT_PHOSPHORUS`, `MAT_ASBESTOS`, `MAT_STEEL`,
`MAT_MINGRADE`, `MAT_DURA` (dura-steel), `MAT_DESH`, `MAT_STAR` (starmetal), `MAT_MAGTUNG`, `MAT_CMB`, `MAT_DNT`
(dineutronium), `MAT_SLAG`, `MAT_SATURN` (saturnite), `MAT_CARBON`, and more - a near 1:1 match against CE's
`block_uranium`, `block_thorium`, `block_titanium`, `block_copper`, `block_tungsten`, `block_aluminium`,
`block_lead`, `block_steel`, `block_cadmium`, `block_bismuth`, `block_coltan`, `block_tantalium`, `block_niobium`,
`block_beryllium`, `block_schraranium`, `block_schrabidium`, `block_schrabidate`, `block_solinium`, `block_au198`,
`block_euphemium`, `block_dineutronium`, `block_combine_steel`, `block_magnetized_tungsten`, `block_desh`,
`block_dura_steel`, `block_saturnite`, `block_boron`, `block_lanthanium`, `block_actinium`, `block_zirconium`,
`block_lithium`, `block_cobalt`, `block_niter`/`block_niter_reinforced`, `block_sulfur`, `block_fluorite`,
`block_asbestos`, `block_white_phosphorus`, `block_starmetal`, etc. (CE has ~55 such blocks once the
uranium/plutonium isotope and fuel variants are counted).

**Important nuance - this is not one uniform Block class.** CE varies the *behavior* class per material even
though the shape/purpose (full-cube storage block) is identical:

| Behavior | Example CE blocks | Class |
|---|---|---|
| Plain cube | `block_titanium`, `block_copper`, `block_tungsten`, `block_aluminium`, `block_steel`, `block_cobalt`, `block_niter`, `block_sulfur`, `block_beryllium`, `block_euphemium`, `block_dineutronium`, `block_starmetal`, `block_combine_steel`, `block_desh`, `block_dura_steel`, `block_saturnite`, `block_fluorite` | `BlockBase` |
| Contact-radiation hazard | `block_thorium`, `block_neptunium`, `block_polonium`, `block_mox_fuel`, `block_plutonium`, `block_pu238/239/240`, `block_uranium`, `block_u233/235/238`, `block_*_fuel`, `block_trinitite`, `block_schraranium`, `block_schrabidium`, `block_schrabidate`, `block_solinium`, `block_au198`, `block_magnetized_tungsten`, `block_ra226` | `BlockHazard` |
| Radiation-resistant / shielding | `block_niter_reinforced`, `block_lead` | `BlockRadResistant` |
| Beacon-compatible | `block_cadmium`, `block_bismuth`, `block_coltan`, `block_tantalium`, `block_niobium`, `block_lanthanium`, `block_actinium`, `block_zirconium`, `block_polymer`, `block_bakelite`, `block_rubber` | `BlockBeaconable` |
| Outgassing (releases a hazard gas block above) | `block_asbestos` | `BlockOutgas` |
| Water-reactive | `block_lithium` | `BlockHydroreactive` |
| Baked/retextured cube | `block_tcalloy`, `block_cdalloy` | `BlockBakeBase` |
| Nuclear waste (multi-behavior) | `block_waste`, `block_waste_painted`, `block_waste_vitrified` | `BlockNuclearWaste` |
| Falling hazard sand-like | `block_fallout`, `block_yellowcake` | `BlockHazardFalling` |

None of these behavior classes touch a TileEntity or `MultiblockHandlerXR`/`BlockDummyable` - they are all render/
physics/hazard behavior on a plain `Block`. This is why the whole storage-block family is Phase-1-safe (see
section 3), but it means the port's registration loop needs a **per-material behavior tag** (a small lookup, not
a single hardcoded class), most naturally piggybacked onto the hazard data Phase 0 already tracks in
`HazardRegistry`/`NTMMaterial` rather than re-invented block-by-block.

**Naming caveat to flag explicitly:** CE's ids are prefix-first (`block_titanium`, `block_uranium`). Phase 0's
`MaterialShapes.BLOCK.buildRegistryName(mat)` (`inventory/material/MaterialShapes.java:129`) produces
`mat.getRegistryName() + "_block"`, i.e. suffix-first (`titanium_block`, `uranium_block`) - consistent with the
already-established item convention (`ingot_iron` -> `iron_ingot`) but a deliberate, already-made departure from
CE's legacy `hbm:block_titanium` ids. This is the same tradeoff Phase 0 already accepted for items; Phase 1 should
simply follow the same policy for consistency rather than special-case blocks, but it is worth one line in the
port's migration/changelog since it changes save-compatible block ids (out of scope for a datafix here).

### 1c. Adjacent hardcoded-but-repeated families (NOT metadata, NOT generative from `Mats`, but worth a shared loop)

| Family | Class | Count | Notes |
|---|---|---|---|
| `deco_*` metal panel blocks | `BlockBase` (reused) | 8 (`deco_titanium`, `deco_red_copper`, `deco_tungsten`, `deco_aluminium`, `deco_steel`, `deco_rusty_steel`, `deco_lead`, `deco_beryllium`) + `deco_asbestos` (`BlockOutgas`) | Purely decorative per-metal panel texture; CE's own comment flags `//Deco blocks TODO: Add CTM` (connected-textures never finished). Not tied to `Mats` autogen shapes; would need its own small name list if ported 1:1, or could be dropped/deferred since CTM was never finished upstream either. |
| `ladder_*` | `BlockNTMLadder` (reused) | 12 (`ladder_sturdy`, `_iron`, `_gold`, `_aluminium`, `_copper`, `_titanium`, `_lead`, `_cobalt`, `_steel`, `_tungsten`, `_red`, `_red_top`) | Cosmetic per-metal ladder retexture, no material-system coupling (not in `Mats` autogen list). Good loop candidate for datagen (blockstate/model only) but is a decorative family, not a resource family - flagged here only because it's the last "one-class-many-material-named-instances" pattern in the file. |
| Custom Machine (`cm_block`, `cm_sheet`, `cm_engine`, `cm_tank`, `cm_circuit`, `cm_port`) | `BlockCM<E>` / `BlockCMGlass<E>` / `BlockCMPort<E>` (`blocks/machine/`) | metadata-driven via `EnumCMMaterials` (4), `EnumCMEngines` (3), `EnumCMCircuit` (5) | **Metadata-flattening case, but it lives in the `machine` package and is part of the player-buildable "Custom Machine" multiblock system** (`cm_anchor` = `BlockCMAnchor`). Out of scope for this ore/resource report; flagging for whoever owns the machine-casing area, and recommending Phase 2 regardless of its metadata status because of the multiblock coupling. |

## 2. OreEnum cross-check (`OreEnumUtil.OreEnum`, `blocks/OreEnumUtil.java`)

`OreEnum` has **29 entries** (task brief estimated "~30+", close). Cross-checked against every ore/cluster block
constructor call in `ModBlocks.java`:

**Wired to a real `BlockNTMOre`/`BlockCluster`/`BlockDepthOre` instance:**
`SULFUR`, `NITER`, `FLUORITE`, `LIGNITE`, `ASBESTOS`, `RARE_EARTHS`, `CLUSTER_IRON`, `CLUSTER_TITANIUM`,
`CLUSTER_ALUMINIUM`, `CLUSTER_COPPER`, `CLUSTER_TUNGSTEN`, `COBALT`, `CINNABAR`, `COLTAN`, `ZIRCON`, `ALEXANDRITE`,
`NEODYMIUM`, `NITAN`, `OIL`, `PHOSPHORUS_NETHER`, `BLOCK_METEOR`, `METEORITE_FRAG`, `METEORITE_TREASURE` (23 of 29).

**Declared but never passed to an ore-block constructor in `ModBlocks.java`:**
- `COAL`, `DIAMOND`, `EMERALD` - only reachable indirectly: `BlockEnums.OreType` (a *different*, smaller enum used
  by `BlockSellafieldOre`) wraps `OreEnum.DIAMOND`/`EMERALD` for its texture-overlay naming, but the actual drop
  logic in `BlockSellafieldOre.getItemDropped` is a hardcoded `if (this == ModBlocks.ore_sellafield_diamond) return
  Items.DIAMOND` chain, not `OreEnum.getDropFunction()`. So these three are effectively dead as far as the
  `BlockNTMOre` drop-function mechanism goes.
- `RAD_GEM` - same story: `OreType.RADGEM` wraps `OreEnum.RAD_GEM` for texture naming only; `ore_sellafield_radgem`'s
  actual drop is hardcoded to `ModItems.gem_rad` in the `if` chain, bypassing `RAD_GEM.getDropFunction()`.
- `WASTE_TRINITE` - not referenced by any block in `ModBlocks.java` at all (looks like dead/unused enum content).
- `COBALT_NETHER` - `ore_nether_cobalt = new BlockNTMOre("ore_nether_cobalt", 3)` passes no `IOreType` (defaults to
  `null`, i.e. plain vanilla-style self-drop), even though a purpose-built `COBALT_NETHER` entry with a richer
  drop amount (`cobaltNetherAmount`) exists and is unused. This looks like an oversight in CE, not an intentional
  design - worth a note but **do not silently "fix" it** in the port; preserve CE's actual (buggy-looking) behavior
  unless the user explicitly wants behavior parity improvements.

Net: of 29 `OreEnum` entries, 23 drive real ore-block drops, 6 are effectively inert for the ore family (`COAL`,
`DIAMOND`, `EMERALD`, `RAD_GEM` are reused only for Sellafield texture naming; `WASTE_TRINITE` and `COBALT_NETHER`
are unused). Port only needs to carry forward the 23 that are load-bearing; the other 6 can be ported as inert
enum entries for parity or dropped, but either choice is a judgment call - flagging rather than deciding.

`BlockEnums.EnumBasaltOreType` (5 entries: `SULFUR`, `FLUORITE`, `ASBESTOS`, `GEM`, `MOLYSITE`) is a **separate**,
smaller enum specific to `BlockOreBasalt`'s metadata variants (see 1a) - it is not part of `OreEnumUtil.OreEnum`
and has its own `getDrop()`/`getDropCount()` methods.

## 3. Dimension-variant handling

**Finding: there is no single parametrized "ore-per-dimension" block class, and no End-dimension ore variants
exist at all** (`grep -i "ore_end\|end_stone"` over `ModBlocks.java` returns nothing). Instead, CE handles each
"world stone context" as its own family of separately-named, separately-instantiated blocks sharing a common base
class:

- **Overworld stone**: `ore_thorium`, `ore_titanium`, `ore_copper`, ... (`BlockNTMOre` on implicit `Material.ROCK`
  ore look)
- **Nether**: `ore_nether_cobalt`, `ore_nether_tungsten`, `ore_nether_sulfur`, `ore_nether_fire`,
  `ore_nether_plutonium`, `ore_nether_schrabidium`, `ore_nether_uranium(_scorched)` - same `BlockNTMOre`/
  `BlockOutgas` classes, just a different registry name/creative-tab-order per instance. No shared "stone type"
  parameter beyond hardness/resistance tuning (Nether copies are consistently lower hardness, e.g. `0.4F` vs
  `5.0F`).
- **"Gneiss" stone** (a CE-specific decorative/ore stone type, not vanilla): `ore_gneiss_iron`, `_gold`,
  `_uranium(_scorched)`, `_copper`, `_asbestos`, `_lithium`, `_schrabidium`, `_rare`, `_gas` - again, independent
  `BlockNTMOre`/`BlockOutgas` instances.
- **"Depth" stratum stone** (a CE-specific deep-world stone requiring `IDepthRockTool`): `ore_depth_*`,
  `cluster_depth_*`, plus non-ore `stone_depth`/`stone_depth_nether`/`depth_brick`/`depth_tiles`/
  `depth_nether_brick`/`depth_nether_tiles`/`depth_dnt` - all `BlockDepth`/`BlockDepthOre`.
- **Meteor rock**: `block_meteor*`, `ore_meteor`, `meteor_*` decorative set - `BlockNTMOre`/`BlockMeteorOre`/
  `BlockBase`.
- **Basalt** (volcanic biome): `basalt`, `basalt_ore` (metadata multi-ore, see 1a), `basalt_smooth`,
  `basalt_brick`, `basalt_polished`, `basalt_tiles`.
- **Sellafield** (irradiated wasteland biome): `sellafield`, `sellafield_slaked`, `sellafield_bedrock`,
  `ore_sellafield_*`.

**Port implication**: there is no existing "dimension" abstraction to preserve or fight against - each stone
context is already a flat list of independently named blocks. The Phase 1 port can therefore choose either to (a)
keep mirroring CE 1:1 (one block per ore per stone-context, generated from a small per-context table: base class +
hardness/resistance + drop), or (b) if the team wants a cleaner long-term design, introduce a genuine "ore family x
stone-context" 2D generation table now that a real material system exists - but that would be a deliberate
improvement beyond parity, not a requirement, and should be called out to the user rather than assumed.

## 4. NBT / Data Components

None of the ore/resource blocks in this area store meaningful `ItemStack` NBT that needs Data Component migration:
- `BlockNTMOre`/`BlockCluster`/`BlockDepthOre`/`BlockOreBasalt` drops are plain `ItemStack`s from item/metadata
  pairs (1.12 metadata damage values on the dropped item, not NBT) - covered by the general
  metadata-to-distinct-item work already planned in `ModItems`, not by a Data Component migration.
- `BlockBedrockOreTE` is the only ore-adjacent block with a TileEntity; whatever persistent state it carries is a
  TileEntity/BlockEntity concern (out of this report's scope - flagged in section 5).
- The storage-block family (section 1b) carries no stack NBT at all - it is a placeable full-cube analogous to
  `minecraft:iron_block`.

## 5. Phase 1 vs Phase 2 boundary recommendation

**Safe for Phase 1 (no TileEntity, no `BlockDummyable`/`MultiblockHandlerXR` coupling) - confirmed by reading
every constructor in this area:**
- All `BlockNTMOre` / `BlockCluster` / `BlockDepthOre` ore and cluster instances (section 1a), including the
  basalt (`BlockOreBasalt`) and biome-stone (`BlockBiomeStone`) metadata-flattening cases once expanded to
  distinct blocks.
- The entire material storage-block family (section 1b, ~55-57 blocks/materials) - every behavior class involved
  (`BlockBase`, `BlockHazard`, `BlockRadResistant`, `BlockBeaconable`, `BlockOutgas`, `BlockHydroreactive`,
  `BlockBakeBase`, `BlockNuclearWaste`, `BlockHazardFalling`) extends plain `Block`/render-behavior mixins; none
  reference `TileEntity`, `BlockDummyable`, `BlockDummyableMBB`, or `MultiblockHandlerXR`. Phase 0's ported
  `BlockDummyable`/`BlockDummyableMBB` base classes are simply **not needed** for this whole area.
- `BlockSellafieldOre`/`BlockSellafieldSlaked` (needs its own random-variant + shade rendering solution, but no TE
  or multiblock coupling either).
- Decorative `deco_*` and `ladder_*` families (section 1c) - plain `BlockBase`/`BlockNTMLadder`, no TE.

**Deferred to Phase 2 (needs machine-tile-entity or multiblock wiring not covered by this report):**
- `BlockBedrockOreTE` (`ore_bedrock_block`) - the one ore-family block that attaches a TileEntity. Everything else
  in the ore family is TE-free; this single block should be scheduled with whatever machine/TE research area picks
  up simple TE-backed blocks, not bundled into the Phase 1 bulk-ore pass.
- Custom Machine family (`cm_block`, `cm_sheet`, `cm_engine`, `cm_tank`, `cm_circuit`, `cm_port`, `cm_anchor`,
  `cm_flux`, `cm_heat`, `custom_machine`) - metadata-driven but multiblock-coupled (`BlockCMAnchor`); belongs with
  the machine-casing research area, not here.
- Everything else in `ModBlocks.java` outside lines ~300-690/436-513 (RBMK, fusion, PWR, hadron collider, doors,
  cables, conveyors, turrets, rails, bombs, drones, radar, missiles, fluid ducts, etc.) is out of this report's
  area entirely and was skimmed only to confirm it contains no additional generative/ore families and no
  `Casing`/`Dummyable`/`MultiblockHandler` references at the `ModBlocks.java` call-site level (the actual
  Dummyable/multiblock wiring lives inside those ~900 lines' individual block classes, one research area's worth
  of work on its own).

## 6. Summary recommendation for Phase 1 `ModBlocks` implementation

1. Add a **table-driven ore registration helper** (name, base class, harvest level, hardness/resistance,
   optional drop-function reference) covering the ~30 `BlockNTMOre` instances, the 4 `BlockCluster` instances, and
   the 8 `BlockDepthOre` instances found in section 1a - this removes ~40 near-identical constructor calls without
   inventing any new mechanic (the `IOreType` drop-function pattern already generalizes cleanly).
2. Expand `BlockOreBasalt` (5 metadata variants) and `BlockBiomeStone` (2 metadata variants) into 7 distinct
   registry entries total - the only genuine 1.12-metadata-flattening work in this area.
3. Drive the ~55-57-block material storage-block family directly off `Mats.ALL` (or whatever the canonical
   iteration point is) filtered by `hasAutogen(MaterialShapes.BLOCK)`, paired with a small per-material behavior
   lookup (plain / hazard / rad-resistant / beaconable / outgas / hydroreactive) rather than one uniform class -
   this is the single highest-leverage generative win in `ModBlocks.java` and it slots directly into the naming
   convention (`MaterialShapes.BLOCK.buildRegistryName`) Phase 0 already built for exactly this purpose.
4. Leave `BlockBedrockOreTE` and the `cm_*` Custom Machine family out of the Phase 1 ore/resource pass; they are
   the only two "resource-adjacent" families in this report's scanned area that carry machine/multiblock coupling.
5. `BlockSellafieldOre`/`BlockSellafieldSlaked`'s random-variant-plus-tint rendering trick needs a specific
   datagen answer (weighted blockstate variants + an `IntegerProperty` shade) independent of the ore/material
   work above - flagging for whoever designs the block datagen pipeline, since it is a rendering pattern, not a
   registration pattern.
