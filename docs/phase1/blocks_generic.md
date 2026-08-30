# Triage: `blocks/generic` (154 files)

Research area key: `blocks_generic`. Read-only survey, no port files written. Every one of the 154
files under `com.hbm.blocks.generic` was inspected (superclass, interfaces, `com.hbm.*` imports, and
inner `TileEntity` classes); files with an external `TileEntity` were followed into
`com.hbm.tileentity.*` to check whether that TE is trivial or a real system dependency.

## Verdict up front

- **95 of 154 files (62%) are Phase-1-safe** as pure `Block` classes: no tile entity, or a genuinely
  trivial one (render-distance override, single `ItemStack` holder, or a small self-contained ticker
  with no external system calls). These can be ported now.
- **58 of 154 files (38%) need a Phase-2+ system** that doesn't exist yet - grouped below by which
  system, per the task's own examples (`ChunkRadiationManager`, multiblock framework, etc.).
- **1 file is dead weight**: `BlockControlPanelFront.java` is 100% commented out.
- **A large fraction of the "safe" pile still needs the metadata-flattening treatment**: 26 files
  extend `BlockMeta`, `BlockEnumMeta<E>`, or `BlockPlantEnumMeta<E>` (CE's `PropertyInteger`/
  `PropertyEnum`-driven multi-variant base classes), or hand-roll the same `PropertyInteger` idiom
  (`BlockPlatemetal`). Every one of these must become N distinct block registry entries in the port.
  See the dedicated section below - this is the single most important structural finding for this
  package.
- **CE's runtime model-baking system (`BlockBakeBase`/`IDynamicModels`, `ModelBakeEvent`) has no
  1.21/NeoForge equivalent and must be replaced by datagen-generated block models/blockstates.** 23
  files extend `BlockBakeBase` (or its "old"/"layered"/"falling" siblings) purely to auto-generate a
  cube/column model from a texture at bake time - this is exactly the kind of hand-rolled model
  generation the port's datagen ground rule replaces. The blocks themselves are trivial; only the
  *mechanism* for producing their models changes.

## How the 58 Phase-2+ files break down, by system

| System (doesn't exist yet) | Count | Files |
|---|---|---|
| Radiation propagation (`ChunkRadiationManager` / `RadiationSystemNT` driving real gameplay behavior, not just a marker interface) | 12 | `BlockAbsorber`, `BlockCluster`, `BlockFallout`, `BlockHazard`, `BlockHazardFalling`, `BlockHazardMeta`, `BlockNTMOre`, `BlockNetherCoal`, `BlockNuclearWaste`, `BlockOutgas`, `BlockPorous` (marked `@Untested` in CE), `YellowBarrel` |
| Multiblock master/dummy framework (`BlockDummyable`, ~945 lines in CE; same framework the `blocks_network_rail` survey flagged as `MultiblockHandlerXR`-adjacent) | 3 | `BlockDoorGeneric` (also see dead-weight note below), `BlockLantern`, `BlockLanternBehemoth` |
| Control-panel event network (`ControlEventSystem`/`IControllable`/`IControlReceiver`, `com.hbm.inventory.control_panel.*`) | 8 | `BMPowerBox`, `BlockControlPanel`, `BlockWandJigsaw`, `BlockWandLogic`, `BlockWandLoot`, `BlockWandStructure`, `BlockWandTandem`, plus the world-gen logic-block system below shares its GUI/event plumbing |
| World-gen logic-block system (`LogicBlockActions`/`Conditions`/`Interactions`) | 2 | `LogicBlock`, `LogicBlockInvis` |
| GUI screen framework (`IGUIProvider` + a real `GuiScreen`, beyond the wand tools already counted above) | 2 | `BlockBobble`, `BlockSnowglobe` |
| Inventory/lock container TE framework (`TileEntityCrateIron/Steel/Tungsten/Desh/Safe`, `TileEntityLockableBase`, `ItemLock`) | 4 | `BlockStorageCrate`, `BlockStorageCrateRadResistant`, `BlockDecoContainer`, `BlockClorineSeal` (control-adjacent lock/seal TE) |
| Fluid-network TE (`IFluidReceiverMK2`, `IFluidStandardSender`, `FluidStack`-bearing TE) | 4 | `BlockRebar` (+ its dedicated `RebarFillRenderer`, counted separately below), `BlockFissure`, `BlockBedrockOreTE` |
| Particle/FX + threaded-packet system (`HbmEffectNT`, `AuxParticlePacketNT`, `PacketThreading`, `ParticleUtil`) | 4 | `BlockEmitter`, `PartEmitter`, `BlockVent`, `BlockGeysir` |
| Mob/entity system (a specific `Entity` subclass that doesn't exist in the port) | 4 | `BlockGlyphidSpawner` (Glyphid family), `DungeonSpawner` (`EntityUndeadSoldier`), `BlockBallsSpawner` (`EntityBOTPrimeHead` boss), `TrappedBrick` (`EntityBulletBase`/`EntityRubble` projectiles, i.e. the gun/bullet-config system) |
| Gun/weapon interaction (`IGunClickable`, `PedestalRecipes`) | 1 | `BlockPedestal` |
| Legacy loot-pool + advancement system (`ItemPool`/`ItemPoolsRedRoom`, `WeightedRandomChestContentFrom1710`, `AdvancementManager`) | 2 | `BlockKeyhole`, `BlockRedBrickKeyhole` |
| Custom variant-blend rendering (`VariantBakedModel`, `PropertyRandomVariant`, `TextureAtlasSpriteMutatable`) - a heavier, bespoke sibling of the `BlockBakeBase` system, not a simple retexture | 3 | `BlockSellafield`, `BlockSellafieldOre`, `BlockSellafieldSlaked` |
| Client-only custom chunk-mesh renderer tied 1:1 to a fluid TE (obsolete rendering approach, needs a `BlockEntityRenderer` rewrite) | 1 | `RebarFillRenderer` (companion to `BlockRebar`, already counted above; listed again here because it is its own file with no `Block` class at all) |
| Bomb/explosion system (`ExplosionThermo`, `ExplosionNukeGeneric`, `BlockTaint`) | 1 | `RedBarrel` (also depends on `BaseBarrel`, which is itself Phase-1-safe) |

Total distinct files in the table: 58 (BlockRebar and RebarFillRenderer are two separate files, both
counted; YellowBarrel appears only in the radiation row even though it also uses
`ExplosionNukeGeneric`, to avoid double-counting - treat both barrels as needing the explosion system
too).

### Notes on borderline calls in that table

- **`BlockBedrockOre` (no TE) vs `BlockBedrockOreTE` (has one) are different files with the same
  name pattern - don't conflate them.** `BlockBedrockOre` implements `IDrillInteraction` directly and
  has no tile entity; the interface's methods (`canBreak`/`extractResource`/`getRelativeHardness`)
  are self-contained and only ever get *called* by a future drill machine, so the block itself is
  Phase-1-safe (see below). `BlockBedrockOreTE` is a different, TE-backed variant whose
  `TileEntityBedrockOre` stores a `FluidStack acidRequirement` for an acid-leaching drill and is
  genuinely Phase-2.
- **`IRadResistantBlock` and `IDrillInteraction` are self-contained marker/callback interfaces**, not
  system couplings by themselves - a block implementing them compiles and functions as a plain block
  with no TE; only the *payoff* behavior (radiation shielding, drill interaction) is inert until the
  Phase-2 caller (`ChunkRadiationManager`/`RadiationSystemNT`, a drill machine) exists. I have **not**
  counted `BlockNTMGlass`, `BlockNTMGlassPane`, `BlockRadResistant`, `BlockRadResistantPillar`,
  `ReinforcedLamp`, or `BlockBedrockOre` as Phase-2-blocked for this reason - they're listed in the
  Phase-1-safe table below with an explicit "radiation hook is a no-op until Phase 2" note. Contrast
  this with the radiation row in the table above, where the block's *core mechanic* (an ore that
  poisons on touch, a barrel that explodes into fallout) is unusable without
  `ChunkRadiationManager`/`HazardSystem` actually running.
- **`IBomb` is likewise a trivial, self-contained interface** (one method, `explode(...)` returning an
  enum) - it does not pull in an explosion system by itself. `BlockDoorGeneric` is deferred for its
  `BlockDummyable` multiblock coupling, not for `IBomb`.
- **`BlockDoorGeneric` also carries genuinely dead 1.12-only weight**: it implements
  `micdoodle8.mods.galacticraft.api.block.IPartialSealableBlock`, an `@Optional.Interface` soft
  dependency on the Galacticraft mod (space-station atmosphere sealing). Galacticraft has no
  NeoForge-1.21-compatible release in this mod's ecosystem at present; this optional cross-mod
  integration should be dropped from the port rather than carried forward, independent of the
  `BlockDummyable` deferral.
- **`BlockOreMeta` and `BlockOreBasalt` do *not* pull in `HazardSystem`/`ChunkRadiationManager`**
  despite the "ore" name - I checked their imports directly and they are plain `BlockMeta`/
  `BlockEnumMeta` decorative-ore variant sets with custom multi-pass texture rendering, not
  radioactive. Don't lump them in with `BlockNTMOre` and its hazard-driven subclasses.

## Metadata-flattening: every block that needs N-entry expansion

Per the port's flattening ground rule, every file below represents **one CE registry entry that must
become several** in the port (one per enum constant / `PropertyInteger` value it currently switches
on). This list is independent of the Phase-1-safe/Phase-2 split above - some of these are otherwise
simple (port now, just as N entries) and some are already deferred for other reasons (listed with a
"(deferred)" tag; expand later when the blocking system lands).

**Base classes carrying the pattern** (not content themselves, but every subclass inherits the
N-entries-per-class shape): `BlockMeta.java` (`PropertyInteger META`, 0-15, one `BlockBakeFrame` per
value), `BlockPlantEnumMeta.java` (extends `BlockEnumMeta<E>`, adds `IPlantable`). `BlockEnumMeta<E>`
itself lives one package up (`com.hbm.blocks.BlockEnumMeta`, out of this survey's 154-file scope) but
is the root of the whole family.

**`BlockMeta` subclasses** (need expansion; check each enum/`META_COUNT` for the exact variant list):
`BlockFlammable`, `BlockHazardMeta` (deferred - hazard), `BlockOreMeta`, `BlockSellafield` (deferred -
custom rendering), `BlockUberConcrete`.

**`BlockEnumMeta<E>` subclasses** (need expansion, one entry per enum constant): `BlockAbsorber`
(deferred - radiation; 4 values: `BASE`/`RED`/`GREEN`/`PINK`), `BlockCap`, `BlockConcreteColored` (16
values, one per `EnumDyeColor`), `BlockConcreteColoredExt`, `BlockCoke`, `BlockLightstone`,
`BlockMeteorOre`, `BlockOreBasalt`, `BlockResourceStone`, `BlockStalagmite`, `BlockDecoCRT` (via
`BlockDecoModel<E>`), `BlockDecoToaster` (via `BlockDecoModel<E>`).

**`BlockPlantEnumMeta<E>` subclasses**: `BlockDeadPlant`, `BlockNTMFlower`, `BlockTallPlant`.

**`IBlockMulti`-marked blocks** (CE's *other* metadata-multi marker - same expansion requirement,
different base class): `BlockAbsorber` (also `BlockEnumMeta`, see above), `BlockGlyphid`,
`BlockGlyphidSpawner` (deferred - mob system), `BlockPlushie` (deferred - GUI/transform system),
`BlockScaffold`, `BlockWandStructure` (deferred - control-panel system).

**Hand-rolled `PropertyInteger` metadata blocks that don't use `BlockMeta`/`BlockEnumMeta` at all**
(same expansion need, just implemented bespoke): `BlockPlatemetal` (`PropertyInteger META` sized off
`PlatemetalType.VALUES.length`).

Total: **26 files** need this treatment (counting each concrete subclass once; the two abstract base
classes are architecture, not content).

## The `BlockBakeBase` runtime-model-baking family (datagen migration note)

`BlockBakeBase.java` is CE's helper for skipping hand-written block-model JSON: its constructors take
a texture name (or a `BlockBakeFrame` describing a cube/column layout) and register a
`ModelBakeEvent` listener that retextures a base model at bake time
(`ModelLoaderRegistry.getModel(...).retexture(...).bake(...)`). NeoForge 1.21's model-baking pipeline
is a different shape entirely (no `ModelBakeEvent`/`IModel`/`ModelLoaderRegistry` in that form), and
regardless of the API, **the port's ground rules call for datagen-generated block models/blockstates,
not runtime baking** - so this whole mechanism should simply not be ported; every subclass just needs
a datagen-emitted `cube_all`/`cube_column`/custom model referencing its texture(s) instead. The
blocks themselves have no TE and no other Phase-2 coupling, so they are all Phase-1-safe; I've flagged
each one with "(BlockBakeBase)" in the table below as a reminder to route it through datagen instead
of a runtime baker. Direct extenders: `BlockBakeOld`, `BlockBakedLayered`, `BlockBarrier`,
`BlockFallingBaked`, `BlockGenericPWR`, `BlockLayering`, `BlockSandbags` (also uses a bespoke
`UnlistedPropertyBoolean` - see next paragraph), `BlockScaffold` (also `IBlockMulti`, see above),
`BlockSpeedy`, `BlockWand`, `HEVBattery` (uses a `.obj` model via `HFRWavefrontObject`, not a simple
retexture - see custom-model note below).

A few files use `net.minecraftforge.common.property.IUnlistedProperty` (`BlockSandbags` via
`UnlistedPropertyBoolean`) - the same Forge-1.12 "unlisted blockstate property" mechanism the
`blocks_network_rail` survey already flagged as having no NeoForge 1.21 equivalent
(`SimpleUnlistedProperty`). `BlockSandbags` needs its per-side "filled" flags reworked as ordinary
`BooleanProperty` blockstate properties (16 combinations, or a `MultipartBakedModel`/model-condition
approach) rather than an unlisted property.

**Custom `.obj`-model blocks** (via `HFRWavefrontObject`, a CE-specific Wavefront OBJ loader, not a
simple cube retexture - needs a NeoForge geometry-loader equivalent or a baked model authored some
other way): `HEVBattery`, `BlockSkeletonHolder`, `BlockDecoModel`/`BlockDecoCRT`/`BlockDecoToaster`
(all three via `BlockDecoBakedModel` wrapping an OBJ). `BlockScaffold` also loads an OBJ.
`BlockReeds` and `BlockSandbags` each use a different bespoke `IBakedModel` implementation
(`BlockReedsBakedModel`, `BlockSandbagsBakedModel`) rather than OBJ, but face the same "needs a
hand-authored NeoForge baked model, not a datagen JSON model" note.

## Category (a): Phase-1-safe simple blocks (95 files)

No tile entity, or a genuinely trivial one; no multiblock/machine-network coupling. Grouped by theme.
"(meta xN)" flags files that also need the N-entry expansion treatment described above - see that
section for the exact variant list per file. "(BlockBakeBase)" flags files needing the datagen model
migration described above.

### Structural / building materials

| File | Purpose | Notes |
|---|---|---|
| `BlockGenericSlab` | Slab variant of a mod material (`BlockSlab` subclass) | Custom `BlockItem` |
| `BlockGenericStairs` | Stairs variant of a mod material (`BlockStairs` subclass) | (BlockBakeBase-adjacent, `IDynamicModels`) |
| `BlockRedBrick` | Decorative brick block | Suggested tab: `BLOCKS` |
| `BlockForgottenBrick` | Dungeon-ruin decorative brick | Custom `BlockItem`; suggested tab: `BLOCKS` |
| `BlockForgottenLock` | Dungeon-ruin decorative lock prop | Custom `BlockItem` |
| `FragileBrick` | Brick that breaks unusually easily | |
| `BlockRailing` | Decorative railing | |
| `BlockChain` | Decorative hanging chain | |
| `BlockGrate` | Decorative metal grate | Has a tooltip (`ITooltipProvider`) |
| `BlockMetalFence` | Metal fence/pane (`BlockPane` subclass) | Custom `BlockItem` |
| `BlockPlatemetal` (meta xN) | Metal plate-panel decorative block | Hand-rolled `PropertyInteger`, see expansion section |
| `BlockRotatablePillar` | Rotatable decorative pillar (`BlockRotatedPillar` subclass) | |
| `BlockRadResistantPillar` | Rad-shielding pillar (`BlockRotatedPillar` + `IRadResistantBlock`) | Radiation-shielding hook is inert until `RadiationSystemNT` exists (Phase 2); block itself is a plain pillar today |
| `BlockRBMKSlab` | RBMK reactor core visual slab | No TE; decorative shell for the future RBMK multiblock (Phase 2 wires it up later) |
| `BlockGenericPWR` (BlockBakeBase) | PWR reactor vessel visual shell | No TE; same "wire up later" note as RBMK slab |
| `BlockWoodStructure` | Decorative wood-structure block variant | Custom `BlockItem`, `EnumUtil`-driven variants (check whether these are metadata or separate fields before porting) |
| `BlockLayering` (BlockBakeBase) | Reactor-meltdown-themed layered decorative block | References `ZirnoxDestroyed`/`RBMKDebris` only as sibling decorative block classes, not as a TE coupling |
| `BlockUberConcrete` (meta xN) | Reinforced/"uber" concrete variant set | extends `BlockMeta` |
| `BlockConcreteColored` (meta xN) | Colored concrete, one per `EnumDyeColor` (16) | extends `BlockEnumMeta` |
| `BlockConcreteColoredExt` (meta xN) | Extended colored-concrete variant set | extends `BlockEnumMeta` |
| `BlockSandbags` (BlockBakeBase) | Sandbag wall/pile block | Bespoke `IUnlistedProperty` for per-side fill state - needs blockstate rework, see note above |
| `BlockScaffold` (BlockBakeBase, meta xN via `IBlockMulti`) | Scaffold/construction block | Custom `.obj` model |
| `BlockPorous` | Porous/absorbent block | Uses `ChunkRadiationManager` - **actually belongs in the Phase-2 radiation group above**, listed here only as a cross-reference; do not port until that system exists |

*(Note: `BlockPorous` is intentionally listed in both places above - it's structurally simple with no
TE, but its one piece of functionality is a `ChunkRadiationManager` call, so treat it as deferred, not
Phase-1-safe, despite fitting this table's "no TE" criterion.)*

### Doors, trapdoors, ladders

| File | Purpose | Notes |
|---|---|---|
| `BlockModDoor` | Modded door variant (`BlockDoor` subclass) | `INBTBlockTransformable` (light structure-rotation hook, see below) |
| `BlockNTMTrapdoor` | Modded trapdoor (`BlockTrapDoor` subclass) | `IDynamicModels` |
| `BlockNTMLadder` | Modded ladder (`BlockLadder` subclass) | |

### Glass

| File | Purpose | Notes |
|---|---|---|
| `BlockNTMGlass` | Rad-resistant glass (`BlockBreakable` subclass) | `RadiationSystemNT` shielding hook inert until Phase 2 |
| `BlockNTMGlassPane` | Rad-resistant glass pane (`BlockPane` subclass) | Same radiation-hook note |

### Ore / mineral (non-hazardous)

| File | Purpose | Notes |
|---|---|---|
| `BlockOreMeta` (meta xN) | Metadata-driven ore-variant set | Custom multi-pass texture rendering (`TextureAtlasSpriteMultipass`); **not** hazard-coupled despite the name |
| `BlockOreBasalt` (meta xN) | Basalt-hosted ore-type variant set | Not hazard-coupled |
| `BlockMeteorOre` (meta xN) | Meteorite ore-type variant set | |
| `BlockResourceStone` (meta xN) | Generic resource-stone variant set | |
| `BlockStalagmite` (meta xN) | Cave stalagmite decoration | |
| `BlockBedrockOre` | Bedrock-tier ore, `IDrillInteraction` | Interface is self-contained/inert until a Phase-2 drill machine calls into it; no TE |
| `BlockDepth` | Deep-rock block requiring a special tool (`IDepthRockTool` marker) | Self-contained tool-requirement check |
| `BlockDepthOre` | Deep-rock ore variant (extends `BlockDepth`) | |
| `BlockBiomeStone` | Biome-variant decorative stone | Custom `BlockItem` |
| `BlockPinkLog` | Decorative wood-log variant (`BlockLog` subclass) | |

### Plants / vegetation / terrain

| File | Purpose | Notes |
|---|---|---|
| `BlockDeadPlant` (meta xN) | Dead-plant decoration set | extends `BlockPlantEnumMeta` |
| `BlockNTMFlower` (meta xN) | Modded flower set, `IGrowable` | extends `BlockPlantEnumMeta` |
| `BlockTallPlant` (meta xN) | Tall-plant set, `IGrowable` | extends `BlockPlantEnumMeta`; references `OreDictManager` for tag-equivalent lookups |
| `BlockHangingVine` | Hanging vine, `IShearable` | |
| `BlockMush` | Small mushroom (`BlockBush`, `IGrowable`) | Uses `HugeMush` world-gen feature class for growth, self-contained |
| `BlockMushHuge` | Huge mushroom cap/stem | |
| `BlockReeds` | Custom-modeled reed/cane block | Bespoke `BlockReedsBakedModel`, see custom-model note |
| `BlockNTMDirt` | Modded dirt variant (extends `BlockDirt`) | |
| `BlockDirt` | Base dirt-spread block | Uses `TomSaveData`, a small custom per-world `WorldSavedData` class for the spread simulation - port that data class alongside, it is not a machine/TE dependency |
| `BlockGlyph` | Decorative dungeon glyph wall | No TE |
| `BlockGlyphid` (meta xN via `IBlockMulti`) | Decorative Glyphid-themed wall texture set | No TE, no entity import - purely cosmetic despite the name |
| `Guide` | Simple guide/waypoint block | |

### Fallout / wasteland terrain (vanilla-block reskins)

| File | Purpose | Notes |
|---|---|---|
| `WasteEarth` | Contaminated dirt; potion effect on walk | Self-contained `ContaminationUtil.isRadImmune` check, no `ChunkRadiationManager` call |
| `WasteMycelium` | Contaminated mycelium (extends `WasteEarth`) | |
| `WasteGrassTall` | Contaminated tall grass (`BlockBush`) | |
| `WasteSand` | Contaminated sand (`BlockFalling`) | |
| `WasteIce` | Contaminated ice (`BlockIce`) | |
| `WasteLeaves` | Contaminated leaves (`BlockOldLeaf`) | |
| `WasteLog` | Contaminated log (`BlockRotatedPillar`) | |

### Hazard-adjacent but self-contained (no radiation-system call)

| File | Purpose | Notes |
|---|---|---|
| `BlockClorine` | Chlorine gas cloud | Self-contained potion effects on touch; gas-mask check via `ArmorRegistry.HazardClass.GAS_LUNG` (ported alongside hazmat armor items, an items-area concern, not a block blocker) |
| `BlockHydroreactive` | Reacts to water contact | Fully self-contained, no hazard/radiation import |
| `BlockSmolder` | Smoldering ground | Self-contained |
| `ReinforcedLamp` | Rad-resistant light source | `RadiationSystemNT` shielding hook inert until Phase 2; no TE |
| `BlockRadResistant` | Generic rad-resistant block | Same hook note |
| `Spikes` | Damage-on-touch spikes | Uses `ModDamageSource` (already a known Phase-0 `damage_types` dependency) |
| `BarbedWire` | Damage-on-touch fence | Same `ModDamageSource` dependency |
| `BlockNoDrop` | Marker block that drops nothing | |
| `BlockClean` | "Clean zone" marker block | Reads `RadiationConfig` only, no radiation-system call |

### Crates, barrels, loot containers

| File | Purpose | Notes |
|---|---|---|
| `BaseBarrel` | Generic barrel shell (base class for Red/Yellow) | Zero `com.hbm` imports beyond `ModBlocks`; fully generic |
| `BlockCrate` | Falling loot crate (`BlockFalling`) | Self-contained loot-list building against `ModItems`/`ItemCell`; needs those items to exist for full loot tables (items-team concern) |
| `BlockAmmoCrate` | Ammo loot crate | Same pattern, references `GunFactory` only for loot-list building |
| `BlockCanCrate` | Canned-food loot crate | |
| `BlockJungleCrate` | Jungle-biome loot crate variant | |
| `BlockLoot` | Loot container (`TileEntityLoot`, no ticking) | Chest-like; needs a basic open/loot GUI, no machine/multiblock coupling |
| `BlockSupplyCrate` | Supply-drop loot container (`TileEntitySupplyCrate`, no ticking) | Same chest-like pattern |
| `BlockSkeletonHolder` | Dungeon prop holding one `ItemStack` | `TileEntitySkeletonHolder` only stores/syncs one `ItemStack`, no ticking, no external systems; custom `.obj` model (`HFRWavefrontObject`) |

### Deco (visual props)

| File | Purpose | Notes |
|---|---|---|
| `DecoBlock` | Generic deco display block | `TileEntityDecoBlock` is empty except a render-distance override - trivial TE |
| `DecoBlockAlt` | Deco block with a small area-effect aura | `TileEntityDecoBlockAlt` is a small self-contained ticker (hardcoded to one specific statue block, AABB potion-effect pulse), no external systems |
| `DecoPoleSatelliteReceiver` | Decorative satellite-dish pole | Trivial empty TE, same pattern as `DecoBlock` |
| `DecoPoleTop` | Decorative pole cap | No TE |
| `DecoSteelPoles` | Decorative steel pole | No TE |
| `DecoTapeRecorder` | Decorative tape recorder prop | `TileEntityDecoTapeRecorder` is 35 lines, no ticking, no dependencies - trivial TE |
| `BlockDecoModel` (meta xN, custom OBJ model) | Base class for enum-driven OBJ-modeled deco props | `INBTBlockTransformable` (light structure-rotation hook, see below), no TE |
| `BlockDecoCRT` (meta xN, custom OBJ model) | Decorative CRT monitor prop | extends `BlockDecoModel` |
| `BlockDecoToaster` (meta xN, custom OBJ model) | Decorative toaster prop | extends `BlockDecoModel` |
| `BlockBakedLayered` (BlockBakeBase-adjacent) | Layered deco block | |
| `BlockBarrier` (BlockBakeBase) | Invisible/barrier-style deco block | |
| `BlockBakeOld` (BlockBakeBase) | Old-style baked deco block | |
| `BlockFallingBaked` (BlockBakeBase) | Falling variant of the baked-block family | |
| `BlockBeaconable` | Block that can host a beacon base | |
| `BlockWriting` | Decorative "writing"/sign-like prop | |
| `HEVBattery` (BlockBakeBase, custom OBJ model) | Wall-mounted armor recharge station | No TE; right-click interaction reads/writes `IBatteryItem` charge on the player's worn armor (Phase-0 HE API) and on `ArmorFSB`/`ArmorFSBPowered` gear items (items-area content) |
| `BlockWand` (BlockBakeBase) | Plain creative "wand" marker block | Zero extra `com.hbm` imports; distinct from the TE-backed structure/logic wands below (which are deferred) |

### Speed / tool-interaction blocks

| File | Purpose | Notes |
|---|---|---|
| `BlockSpeedy` (BlockBakeBase) | Speed-boost floor block | `IStepTickReceiver`, self-contained |
| `BlockSpeedyStairs` | Stairs variant of `BlockSpeedy` | Same |
| `BlockToolConversion` | Right-click-with-tool block converter | Self-contained lookup via `NTMToolHandler`/`RecipesCommon`, no TE |
| `BlockPipe` | Decorative pipe segment | `INBTBlockTransformable` (light structure-rotation hook), no TE |

### Metadata decoration blocks with no other coupling

| File | Purpose | Notes |
|---|---|---|
| `BlockCap` (meta xN) | Decorative "cap" block | |
| `BlockCoke` (meta xN) | Coke-type decorative block | |
| `BlockLightstone` (meta xN) | Glowing decorative stone | |
| `BlockFlammable` (meta xN) | Fuel/flammable-type block set | extends `BlockMeta`; self-contained fire mechanics |

## Category (b): needs Phase 2+ systems (58 files)

See the grouped table near the top of this document for the full file-by-system breakdown. Summary of
systems, for cross-referencing against other Phase-2 research areas:

1. Radiation propagation - `ChunkRadiationManager` / `RadiationSystemNT` (12 files)
2. Multiblock master/dummy framework - `BlockDummyable` (3 files)
3. Control-panel event network - `ControlEventSystem`/`IControllable`/`IControlReceiver` (8 files)
4. World-gen logic-block system - `LogicBlockActions`/`Conditions`/`Interactions` (2 files)
5. GUI screen framework - `IGUIProvider` (2 files, beyond the wand tools already in #3)
6. Inventory/lock container TE framework (4 files)
7. Fluid-network TE - `IFluidReceiverMK2`/`IFluidStandardSender` (4 files, incl. `RebarFillRenderer`)
8. Particle/FX + threaded-packet system (4 files)
9. Mob/entity system - specific `Entity` subclasses not yet ported (4 files)
10. Gun/weapon interaction - `IGunClickable`/`PedestalRecipes` (1 file)
11. Legacy loot-pool + advancement system (2 files)
12. Custom variant-blend rendering - Sellafield family (3 files)
13. Bomb/explosion system (1 file, `RedBarrel`; `YellowBarrel` counted under radiation but also needs this)

## Category (c): dead / 1.12-only weight to drop (1 file)

- **`BlockControlPanelFront.java`** - the entire file is commented out (every line after the package
  statement is a `//`). It is 100% inert and should simply not be ported. (Its intended functionality
  is superseded by/duplicated in the live `BlockControlPanel.java`, which is deferred to the
  control-panel-network Phase-2 group above.)

Additionally noted but not a whole-file drop: `BlockDoorGeneric`'s `IPartialSealableBlock` interface
implementation is a dead Galacticraft soft-dependency (see the borderline-calls note above) - strip
that one interface/import when the file is eventually ported for its `BlockDummyable` multiblock
work, rather than carrying forward a reference to a mod with no NeoForge-1.21 build in this
ecosystem.

## Cross-cutting notes for whoever picks this package up next

- **`INBTBlockTransformable`/`INBTTileEntityTransformable`** (`com.hbm.world.gen.nbt.*`) show up on
  several otherwise-simple blocks (`BlockPipe`, `BlockModDoor`, `BlockDecoModel` family, and the
  deferred `BlockBobble`/`BlockPlushie`/`BlockSnowglobe`/`BlockWandJigsaw`/`BlockWandTandem`). This is
  a light "rotate my NBT data when this structure/schematic is mirrored or rotated during world-gen
  placement" hook, distinct from and much smaller than the multiblock/GUI systems. It's not implemented
  yet in the port; the Phase-1-safe blocks that reference it (`BlockPipe`, `BlockModDoor`,
  `BlockDecoModel`/`BlockDecoCRT`/`BlockDecoToaster`) can still be ported now with the interface
  either stubbed or simply not implemented until the structure-transform system exists - none of them
  need it to function as a placed block, only for correct behavior under structure rotation.
- **No ItemStack-NBT-to-Data-Component findings specific to this package** beyond what's already
  covered by the metadata-flattening section (the flattening itself is a blockstate/registry concern,
  not stack NBT). `BlockSkeletonHolder`'s TE stores one `ItemStack` via TE NBT (not stack NBT), which
  is a tile-entity persistence question for the `tileentity` research area, not this one.
- **Hazard/creative-tab placement**: none of the Phase-1-safe blocks in this package need a
  `HazardRegistry` binding themselves (that registry binds *items*, e.g. hazardous dusts/ingots, per
  Phase 0's design) - the block-level hazard mechanic here is always the separate
  `ChunkRadiationManager`/`RadiationSystemNT` system, which is why the true hazard/radiation blocks
  (ore, waste, absorber, barrels) all landed in category (b) rather than (a). For creative-tab
  placement, essentially everything in category (a) is construction/decoration content that belongs
  in the existing `ModCreativeTabs.BLOCKS` tab (CE's `BlockTab`); the plain ore/mineral variants
  (`BlockOreMeta`, `BlockOreBasalt`, `BlockMeteorOre`, `BlockResourceStone`, `BlockStalagmite`,
  `BlockBiomeStone`, `BlockDepth`/`BlockDepthOre`) fit `ModCreativeTabs.RESOURCE` instead, matching
  that tab's documented CE icon (`ModBlocks.ore_uranium`).
