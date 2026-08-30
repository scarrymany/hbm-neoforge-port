# Digest of 10 Phase 1 Research Reports

Each section below covers one file, in the order requested: Phase-1-safe scope, Deferred scope, Key design/API decisions, Open questions/risks.

---

## blocks_generic.md
*(`com.hbm.blocks.generic`, 154 files surveyed)*

### Phase-1-safe scope (95 of 154 files)
Grouped by theme in the report; port these now as plain `Block` classes (no TE, or a trivial one):

- **Structural/building materials**: `BlockGenericSlab`, `BlockGenericStairs` (BlockBakeBase-adjacent, `IDynamicModels`), `BlockRedBrick`, `BlockForgottenBrick`, `BlockForgottenLock`, `FragileBrick`, `BlockRailing`, `BlockChain`, `BlockGrate`, `BlockMetalFence`, `BlockPlatemetal` (meta xN), `BlockRotatablePillar`, `BlockRadResistantPillar`, `BlockRBMKSlab`, `BlockGenericPWR` (BlockBakeBase), `BlockWoodStructure`, `BlockLayering` (BlockBakeBase), `BlockUberConcrete` (meta xN), `BlockConcreteColored` (meta xN, 16 values), `BlockConcreteColoredExt` (meta xN), `BlockSandbags` (BlockBakeBase), `BlockScaffold` (BlockBakeBase, meta xN)
- **Doors/trapdoors/ladders**: `BlockModDoor`, `BlockNTMTrapdoor`, `BlockNTMLadder`
- **Glass**: `BlockNTMGlass`, `BlockNTMGlassPane` (radiation-shielding hook inert until Phase 2, block itself fine)
- **Ore/mineral (non-hazardous)**: `BlockOreMeta` (meta xN), `BlockOreBasalt` (meta xN), `BlockMeteorOre` (meta xN), `BlockResourceStone` (meta xN), `BlockStalagmite` (meta xN), `BlockBedrockOre` (not `BlockBedrockOreTE`), `BlockDepth`, `BlockDepthOre`, `BlockBiomeStone`, `BlockPinkLog`
- **Plants/vegetation**: `BlockDeadPlant` (meta xN), `BlockNTMFlower` (meta xN), `BlockTallPlant` (meta xN), `BlockHangingVine`, `BlockMush`, `BlockMushHuge`, `BlockReeds`, `BlockNTMDirt`, `BlockDirt` (port `TomSaveData` alongside), `BlockGlyph`, `BlockGlyphid` (meta xN via `IBlockMulti`), `Guide`
- **Fallout/wasteland reskins** (self-contained, no `ChunkRadiationManager` call): `WasteEarth`, `WasteMycelium`, `WasteGrassTall`, `WasteSand`, `WasteIce`, `WasteLeaves`, `WasteLog`
- **Hazard-adjacent but self-contained**: `BlockClorine`, `BlockHydroreactive`, `BlockSmolder`, `ReinforcedLamp`, `BlockRadResistant`, `Spikes` (uses `ModDamageSource`), `BarbedWire`, `BlockNoDrop`, `BlockClean`
- **Crates/barrels/loot**: `BaseBarrel`, `BlockCrate`, `BlockAmmoCrate`, `BlockCanCrate`, `BlockJungleCrate`, `BlockLoot`, `BlockSupplyCrate`, `BlockSkeletonHolder`
- **Deco**: `DecoBlock`, `DecoBlockAlt`, `DecoPoleSatelliteReceiver`, `DecoPoleTop`, `DecoSteelPoles`, `DecoTapeRecorder`, `BlockDecoModel` (meta xN, custom OBJ), `BlockDecoCRT` (meta xN), `BlockDecoToaster` (meta xN), `BlockBakedLayered`, `BlockBarrier` (BlockBakeBase), `BlockBakeOld` (BlockBakeBase), `BlockFallingBaked` (BlockBakeBase), `BlockBeaconable`, `BlockWriting`, `HEVBattery` (BlockBakeBase, custom OBJ), `BlockWand` (BlockBakeBase)
- **Speed/tool-interaction**: `BlockSpeedy` (BlockBakeBase), `BlockSpeedyStairs`, `BlockToolConversion`, `BlockPipe`
- **Metadata-only decoration**: `BlockCap` (meta xN), `BlockCoke` (meta xN), `BlockLightstone` (meta xN), `BlockFlammable` (meta xN)

Note: `BlockPorous` fits the "no TE" shape but actually calls `ChunkRadiationManager` - treat as **deferred**, not safe, despite appearing structurally simple.

### Deferred scope (58 files, by blocking system)
1. **Radiation** (`ChunkRadiationManager`/`RadiationSystemNT`, 12 files): `BlockAbsorber`, `BlockCluster`, `BlockFallout`, `BlockHazard`, `BlockHazardFalling`, `BlockHazardMeta`, `BlockNTMOre`, `BlockNetherCoal`, `BlockNuclearWaste`, `BlockOutgas`, `BlockPorous`, `YellowBarrel`
2. **Multiblock framework** (`BlockDummyable`, 3 files): `BlockDoorGeneric`, `BlockLantern`, `BlockLanternBehemoth`
3. **Control-panel event network** (8 files): `BMPowerBox`, `BlockControlPanel`, `BlockWandJigsaw`, `BlockWandLogic`, `BlockWandLoot`, `BlockWandStructure`, `BlockWandTandem`
4. **World-gen logic-block system** (2): `LogicBlock`, `LogicBlockInvis`
5. **GUI screen framework** (2): `BlockBobble`, `BlockSnowglobe`
6. **Inventory/lock container TE framework** (4): `BlockStorageCrate`, `BlockStorageCrateRadResistant`, `BlockDecoContainer`, `BlockClorineSeal`
7. **Fluid-network TE** (4): `BlockRebar` (+`RebarFillRenderer`), `BlockFissure`, `BlockBedrockOreTE`
8. **Particle/FX + threaded-packet system** (4): `BlockEmitter`, `PartEmitter`, `BlockVent`, `BlockGeysir`
9. **Mob/entity system** (4): `BlockGlyphidSpawner`, `DungeonSpawner`, `BlockBallsSpawner`, `TrappedBrick`
10. **Gun/weapon interaction** (1): `BlockPedestal`
11. **Legacy loot-pool + advancement** (2): `BlockKeyhole`, `BlockRedBrickKeyhole`
12. **Custom variant-blend rendering** (3): `BlockSellafield`, `BlockSellafieldOre`, `BlockSellafieldSlaked`
13. **Bomb/explosion system** (1): `RedBarrel`

**Dead weight to drop**: `BlockControlPanelFront.java` (100% commented out). Also strip `BlockDoorGeneric`'s dead Galacticraft `IPartialSealableBlock` implementation when it's eventually ported.

### Key design/API decisions
- **26 files need metadata-flattening** (one CE registry entry -> N port entries): `BlockMeta` subclasses (`BlockFlammable`, `BlockHazardMeta`, `BlockOreMeta`, `BlockSellafield`, `BlockUberConcrete`); `BlockEnumMeta<E>` subclasses (`BlockAbsorber` 4 values BASE/RED/GREEN/PINK, `BlockCap`, `BlockConcreteColored` 16 values one per `EnumDyeColor`, `BlockConcreteColoredExt`, `BlockCoke`, `BlockLightstone`, `BlockMeteorOre`, `BlockOreBasalt`, `BlockResourceStone`, `BlockStalagmite`, `BlockDecoCRT`, `BlockDecoToaster`); `BlockPlantEnumMeta<E>` subclasses (`BlockDeadPlant`, `BlockNTMFlower`, `BlockTallPlant`); `IBlockMulti`-marked (`BlockAbsorber`, `BlockGlyphid`, `BlockGlyphidSpawner`, `BlockPlushie`, `BlockScaffold`, `BlockWandStructure`); hand-rolled `PropertyInteger` (`BlockPlatemetal`).
- **`BlockBakeBase` family must not be ported as runtime baking** - replace with datagen-generated `cube_all`/`cube_column`/custom models. Direct extenders needing this treatment: `BlockBakeOld`, `BlockBakedLayered`, `BlockBarrier`, `BlockFallingBaked`, `BlockGenericPWR`, `BlockLayering`, `BlockSandbags`, `BlockScaffold`, `BlockSpeedy`, `BlockWand`, `HEVBattery`.
- **`BlockSandbags`** uses Forge's `IUnlistedProperty` (`UnlistedPropertyBoolean`) - no NeoForge 1.21 equivalent; needs rework as ordinary `BooleanProperty` blockstate properties (16 combinations) or a `MultipartBakedModel`.
- **Custom `.obj`-model blocks** (via `HFRWavefrontObject`, need a NeoForge geometry-loader equivalent or hand-authored baked model): `HEVBattery`, `BlockSkeletonHolder`, `BlockDecoModel`/`BlockDecoCRT`/`BlockDecoToaster` (via `BlockDecoBakedModel`), `BlockScaffold`. `BlockReeds`/`BlockSandbags` use bespoke `IBakedModel` classes (`BlockReedsBakedModel`, `BlockSandbagsBakedModel`).
- **`INBTBlockTransformable`/`INBTTileEntityTransformable`** (structure-rotation NBT hook) appears on `BlockPipe`, `BlockModDoor`, `BlockDecoModel` family - can ship now with the interface stubbed/unimplemented; only affects correctness under structure rotation, not basic placement.
- **Creative tab placement**: category (a) content goes to `ModCreativeTabs.BLOCKS`; plain ore/mineral variants (`BlockOreMeta`, `BlockOreBasalt`, `BlockMeteorOre`, `BlockResourceStone`, `BlockStalagmite`, `BlockBiomeStone`, `BlockDepth`/`BlockDepthOre`) go to `ModCreativeTabs.RESOURCE`.

### Open questions / risks
- `IRadResistantBlock` and `IDrillInteraction` are self-contained marker interfaces - don't miscount blocks implementing them as Phase-2-blocked; only the caller side is inert.
- `IBomb` is a trivial one-method interface - doesn't itself pull in the explosion system.
- Watch the naming collision: `BlockBedrockOre` (safe, no TE) vs `BlockBedrockOreTE` (deferred, has TE) - easy to conflate.
- `BlockOreMeta`/`BlockOreBasalt` are **not** hazard-coupled despite the "ore" name - verified by import inspection.

---

## items_food_gear.md
*(`com.hbm.items.food` 16 files, `com.hbm.items.gear` 28 files)*

### Phase-1-safe scope
**Food (7 clean)**: `ItemFoodSoup`, `ItemLemon`, `ItemMuchoMango`, `ItemNugget`, `ItemAppleEuphemium`, `ItemBDCL`, `ItemFlask` (de-generify to plain item, only 1 enum variant used).

**Food needing metadata flattening** (52 registry entries from 4 classes): `ItemConserve` (27 `EnumFoodType` values -> `hbm:canned_<name>`), `ItemCrayon` (16 `EnumChemDye` colors -> `hbm:crayon_<color>`), `ItemAppleSchrabidium` (2 base x 3 tiers = 6, naming undecided), `ItemTemFlakes` (3 tiers, naming undecided).

**Food registrable now but with TODO-flagged behavior** (ship item, defer potion-sickness branches): `ItemEnergy` (~24 instances), `ItemPill` (~13 instances), `ItemCanteen` (3 instances - cooldown via damage value, not a variant).

**Gear - Tools/weapons (13 files, none touch armor slots)**: `ModAxe`, `ModHoe` (verify `ItemAttributeModifiers` shape vs old `getItemAttributeModifiers`), `ModPickaxe`, `ModSpade`, `ModSword`, `AxeSchrabidium`, `HoeSchrabidium`, `PickaxeSchrabidium`, `SpadeSchrabidium`, `SwordSchrabidium`, `BigSword`, `RedstoneSword` (drop `IHasCustomModel`), `WeaponSpecial` (port shell + self-contained hit branches: `bottle_opener`, `wrench`/`wrench_flipped`, `diamond_gavel`, `wood_gavel`, `stopsign`; flag `memespoon`/`shimmer_sledge`/advancement-grant branches as follow-ups).

### Deferred scope
- **Phase 3 (armor system)**: all 15 `items/gear` armor-slot files - `ModArmor`, `ArmorModel`, `ArmorAsbestos`, `ArmorEuphemium`, `ArmorFSB`, `ArmorGasMask`, `ArmorHazmat`, `ArmorHazmatMask`, `ArmorSchrabidium`, `MaskOfInfamy`, `JetpackBooster`, `JetpackBreak`, `JetpackGlider`, `JetpackRegular`, `JetpackVectorized`. Blocked on NeoForge's `Equippable`/data-component armor system replacing `ItemArmor` + `ISpecialArmor`.
- `ItemPancake` (food) - deferred, couples to `ArmorFSB` battery-charging (Phase 3).
- `bomb_waffle` branch of `ItemFoodBase` and `memespoon`/`shimmer_sledge`/advancement branches of `WeaponSpecial` - deferred pending `EntityNukeExplosionMK5`, `EntityNukeTorex`, `EntityRubble`, `AdvancementManager`.

### Key design/API decisions
- **Blocking facade for food**: `com.hbm.potion.HbmPotion` (custom `MobEffect` registry: `lead`, `radiation`, `radx`, `death`, `stability`, `potionsickness`) is confirmed **not yet registered anywhere** in the port. Also blocking: `HbmLivingProps`/`ContaminationUtil`/`VersatileConfig.applyPotionSickness` - `VersatileConfig.java` has a doc comment confirming these were deliberately skipped.
- Phase 0's `HbmPlayerAttachment` already covers `getShield`/`setShield`/`getMaxShield` (used by `ItemFlask`) and `isJetpackActive()` (used by `Jetpack*`) - these calls are NOT blocked.
- Drop all CE model-registration plumbing (`IDynamicModels`, `IClaimedModelLocation`, `bakeModel`, `ModelResourceLocation`) - superseded by datagen; only constructor args and eat/use behavior matter.
- Drop all `setCreativeTab(MainRegistry.xTab)` calls from constructors - tab membership handled via `BuildCreativeModeTabContentsEvent` elsewhere.
- **Data Component conversions needed**: `ItemCanteen`'s damage-as-cooldown -> custom int component (not vanilla damage). `JetpackGlider`'s NBT-serialized `FluidTankNTM` under `"fuelTank"` key -> purpose-built fluid-type+amount component, coordinated with whatever the fluid-holding-item convention ends up being elsewhere.
- Vanilla `MobEffects` confirmed safe to use directly (no new registry): `STRENGTH`, `RESISTANCE`, `REGENERATION`, `FIRE_RESISTANCE`, `HASTE`, `SPEED`, `WITHER`, `POISON`, `WEAKNESS`, `BLINDNESS`, `NAUSEA`, `HUNGER`, `JUMP_BOOST`, `HEALTH_BOOST`, `ABSORPTION`, `SATURATION`, `NIGHT_VISION`, `WATER_BREATHING`, `MINING_FATIGUE`.

### Open questions / risks
- Naming decision needed for tiered items with no CE tier names: `ItemAppleSchrabidium` (suggests `low/medium/high` or `weak/potent/apex`), `ItemTemFlakes` (3 tiers, same issue) - flagged as needing sign-off from "whoever owns naming conventions."
- `ItemHoe`'s attribute-modifier API shape needs verification against the Neo Edition reference before implementation - explicitly flagged as "don't guess."
- Recommended sequencing given: (1) register clean Phase-1-safe items, (2) flatten the 4 metadata-multi items, (3) register `ItemEnergy`/`ItemPill`/`ItemCanteen` now but leave potion branches as explicit TODOs (not silent no-ops), (4) defer all 15 armor files + `ItemPancake` to Phase 3, (5) defer nuke/rubble/advancement branches with named follow-ups.

---

## items_special.md
*(`com.hbm.items.special` 42 files + `special.weapon.GunB92` = 43)*

### Phase-1-safe scope
**P1 (register now, 19)**: `ItemAMSCore`, `ItemBedrockOreBase`, `ItemCell` (see design decision below), `ItemConsumable`, `ItemCustomLore`, `ItemDemonCore`, `ItemDigamma`, `ItemFuel`, `ItemGlitch` (registration only), `ItemHot`, `ItemModRecord` (verify jukebox API), `ItemNuclearWaste`, `ItemPolaroid`, `ItemRag`, `ItemSchraranium`, `ItemSimpleConsumable`, `ItemStarterKit` (registration only), `ItemBook`*/`ItemBookLore`*/`ItemClayTablet`*/`ItemHolotapeImage`* (register shells now, defer only the menu-open interaction - see below).

**P1-flatten (metadata -> N registry entries)**: `ItemBedrockOreNew` (156 combos: 6 `BedrockOreType` x 26 `BedrockOreGrade` - needs its own explicit design task, largest single expansion in package), `ItemHolotapeImage` (`EnumHoloImage`, 18), `ItemPlasticScrap` (`ScrapType`, 21), `ItemSoyuz` (`SoyuzSkinType`, 3, but flag per Deferred below), `ItemTrain` (`EnumTrainType`, 2, but flag per Deferred below), `ItemWasteLong` (`WasteClass`, 5), `ItemWasteShort` (`WasteClass`, 8), `ItemDepletedFuel` (2 raw meta values), `ItemSiegeCoin` (count driven by `SiegeTier.getLength()`, confirm before flattening), `ItemChopper` (4 hardcoded variants, already effectively separate instances - just confirm each stays distinct).

**Do NOT port**: `ItemAutogen` - superseded entirely by Phase 0's `MaterialShapes`/`Mats.orderedList` generation convention; build material items fresh from that instead of translating this class.

### Deferred scope
- **GUI/Menu framework** (blocks the *interaction*, not registration): `ItemBook`, `ItemBookLore`, `ItemClayTablet`, `ItemHolotapeImage`.
- **-> Phase 2 (machine-interaction)**: `ItemDoorSkin`, `ItemFusionShield` (borderline), `ItemPotatos` (extends `items.machine.ItemBattery`), `ItemTeleLink`.
- **-> Phase 3 (weapon/bomb/armor)**: `ItemDrop`'s detonator half (`detonator_deadman`/`detonator_de`, split out from the harmless singularity-drop half), `ItemUnstable` (pocket nuke), `ItemLootCrate` (shell now, population waits on `ItemMissile`), `weapon/GunB92` (full ranged weapon, misfiled).
- **Unconfirmed vehicle subsystem** (rail/train entity system + rocket multiblock, not scoped in Phase 0): `ItemTrain`, `ItemSoyuz`.

### Key design/API decisions
- **`ItemCell`**: explicit recommendation to keep as **one** `hbm:cell` item with fluid identity in a data component (bucket/bundle-style), NOT flattened per-fluid - called out as a deliberate exception to the flattening rule since fluid registry is open-ended.
- **NBT -> Data Component inventory** (table with exact keys): `ItemBedrockOreBase` (one double per `BedrockOreType.suffix`, 6 keys); `ItemBookLore` (`k` String, `p` short, `cov_col`/`tit_col` int, `p1..pN` nested compounds with `a1..aN` strings - needs list-of-records shape); `ItemFusionShield` (`damage` long); `ItemKitCustom`/`ItemKitNBT` (arbitrary `ItemStack[]` via `ItemStackUtil`, `color1`/`color2` int - maps to `ItemContainerContents`-style component + two int/color components); `ItemDrop` detonator (`x`/`y`/`z` int -> `BlockPos` component); `ItemTeleLink` (`x`/`y`/`z` int); `ItemClayTablet` (`tabletSeed` long); `ItemHot`/`ItemHotDusted` (`heat` int, drives both gameplay and render overlay); `ItemPotatos` (`timer` int); `ItemUnstable` (`timer` int); `GunB92` (`animation` int, `energy` int).
- **Legacy dynamic/baked-model system needs full redesign, not port**: affects `ItemAutogen`, `ItemBedrockOreNew`, `ItemBookLore`, `ItemHot`/`ItemHotDusted`, `ItemEnumMulti` family. `ItemHot`'s alpha-blended glow overlay specifically needs a modern equivalent, most likely an item model "range dispatch"/component-predicate driven by the `heat` data component. `ItemSchraranium`'s `addPropertyOverride` (removed 1.12 API) has the same problem at smaller scale.
- **`ItemModRecord`**: verify against modern jukebox API before implementing - 1.19.3+ replaced direct `SoundEvent` carrying with a `JukeboxSong` datapack registry entry + `JukeboxPlayable` data component. Do not assume the old constructor shape.
- **`ItemEnumMulti<E>`** base class pattern (5 files extend it in this package) must be flattened to N `DeferredItem` entries per the ground rules.

### Open questions / risks
- Baubles has no NeoForge 1.21 build; Curios API is the community successor - **decide once, up front** whether hazard/diagnostic items become Curios-slot accessories or plain items (affects `ItemDosimeter`, `ItemGeigerCounter`, `ItemDigammaDiagnostic`, `ItemLungDiagnostic` here and more across the mod).
- `ItemPollutionDetector` depends on `PollutionHandler`, a standalone world-pollution simulation not currently listed anywhere in Phase 0/1 scope - flagged as a gap for whoever owns cross-cutting systems.
- `ItemSoyuz`/`ItemTrain` - recommend confirming what these actually place (rocket multiblock? train entity?) before committing to a design; don't guess blind.
- `ItemSiegeCoin`'s variant count is read from an external `SiegeTier` list, not a fixed enum - confirm size before flattening.

---

## items_tool.md
*(`com.hbm.items.tool`, 97 files: 95 classes + 2 marker interfaces)*

### Phase-1-safe scope
**(a) Genuine Phase 1 utility/mining tools, ~46 files**, but with an important caveat (see Key decisions):
- Mining-tool framework: `ItemToolAbility` (base for ~40+ material-tiered pickaxe/axe/shovel instances in `ModItems.java`), `ItemToolAbilityFueled` (adds `IFillableItem` fluid tank, used by chainsaws), `ItemToolAbilityPower` (adds `IBatteryItem`, used by `elec_*`/`drax`), `ItemChainsaw` (needs `BusAnimation`/`HbmAnimations`/`IAnimatedItem`), `ItemMultitoolTool` (only `multitool_dig`/`multitool_silk` - but see multitool call below).
- Standalone simple items: `ItemColtanCompass`, `ItemCraftingDegradation`, `ItemCouplingTool` (0 lines of logic, verify no dead external references), `ItemMS`, `ItemDiscord`, `ItemMatch`, `ItemBalefireMatch`, `ItemModDoor`, `ItemModMinecart` (metadata-multi, needs 5 registry entries: Crate/Destroyer/Ore/Powder/Semtex), `ItemFertilizer`, `ItemRepairKit`, `ItemCrateCaller`, `ItemFusionCore` (cross-package dep on `items/armor`), `ItemPeas` (cross-package dep on `EntityQuackos`).
- Container/bag items: `ItemAmmoBag`, `ItemCasingBag`, `ItemPlasticBag`, `ItemLeadBox`, `ItemToolBox`, `ItemCanister`, `ItemGasCanister`, `ItemFluidContainerInfinite`, `ItemPipette`, `ItemFilter` (cross-package dep on `items/armor`).
- Detector/diagnostic items: `ItemDosimeter`, `ItemGeigerCounter`, `ItemDigammaDiagnostic`, `ItemLungDiagnostic` (all Baubles - Curios decision needed), `ItemOilDetector`, `ItemOreDensityScanner`, `ItemSurveyScanner` (soft dep on a world-gen feature), `ItemPollutionDetector` (depends on unbuilt `PollutionHandler` system).
- Self-contained GUI/reference items: `ItemGuideBook`, `ItemCatalog`, `ItemBookLemegeton`.

### Deferred scope
- **(b) Phase 3 melee/ranged/military, ~24 files**: `ItemSwordAbility`, `ItemSwordAbilityPower`, `ItemSwordMeteorite`, `ItemMultitoolTool`+`ItemMultitoolPassive` (whole 10-entry chain, see decision below), `ItemDetonator`, `ItemMultiDetonator`, `ItemLaserDetonator`, `ItemDefuser`, `ItemAmatExtractor`, `ItemRTTYPager`, `ItemDesignator`, `ItemDesignatorManual`, `ItemDesignatorRange`, `ItemDesignatorArtyRange`, `ItemSatDesignator`, `ItemSatInterface`, `ItemRadarLinker`(+`ItemCoordinateBase`), `ItemRangefinder`, `ItemTurretMobFilter`, `ItemBoltgun`.
- **(c) Phase 2 machine coupling, ~19 files**: `ItemTooling` (base for `screwdriver`/`hand_drill`), `ItemToolingWeapon`, `ItemWrench`, `ItemBlowtorch`, `ItemAnalyzer`, `ItemAnalysisTool`, `ItemMirrorTool`, `ItemPowerNetTool`, `ItemConveyorWand`, `ItemRebarPlacer`, `ItemWiring`, `ItemSettingsTool`, `ItemAnchorRemote`, `ItemKeyPin`/`ItemKey`/`ItemLock`/`ItemCounterfeitKeys` (port as one unit), `ItemRBMKTool`, `ItemDyatlov`, `ItemDrone`(metadata-multi, `EnumDroneType`, 5 entries)/`ItemDroneLinker`.
- **(d) Internal dev/debug tooling, not player content, 11 files**: `ItemStructureTool`+`ItemStructureSolid`/`RandomOnized`/`Single`/`Pattern`/`Randomly`, `ItemCMStructure`, `ItemWand`/`ItemWandS`/`ItemWandD`, `ItemMeteorRemote`. Recommend excluding from Phase 1 counts entirely; treat as a standalone low-priority task later if kept at all.

### Key design/API decisions
- **Headline scope risk**: porting "simple pickaxes" for real parity requires porting the entire ability/preset/GUI/keybind/HUD framework `com.hbm.handler.ability.*` (`IBaseAbility`, `IToolAreaAbility`, `IToolHarvestAbility`, `AvailableAbilities`, `ToolPreset`), plus `GUIScreenToolAbility`, `IKeybindReceiver`/`HbmKeybinds`, `IItemHUD`, `IDepthRockTool`. **Recommendation: treat "port the mining tool ability framework" as an explicit Phase 1 prerequisite work item**, not something that falls out of porting `ItemToolAbility` itself.
- **Multitool chain decision**: port `ItemMultitoolTool` + `ItemMultitoolPassive` (10 registry entries total) together in **Phase 3**, not split - the sneak-click upgrade ladder ends in AoE lightning/mass terrain deletion/16-damage combat stats, so splitting would ship a dead-end Phase 1 item.
- **NBT -> Data Component notes**: coordinate-store pattern (`x`/`y`/`z`, `posX/Y/Z`, `anchorX/Y/Z`) repeated across `ItemWrench`, `ItemDetonator`, `ItemCoordinateBase`, `ItemWand`, `ItemStructureTool`, `ItemBoltgun` - recommend one shared `BlockPos`-holding component type. `ability`/`abilityPresets` (`ItemToolAbility.Configuration`) needs a proper structured component. `pins` (`ItemKeyPin`) - trivial int component. `building` (`ItemWandS`) - trivial int, low priority.
- `IItemAbility` (single method `breakExtraBlock`) may be dead/legacy - the actual logic in `ItemToolAbility` is a same-named instance method, not an override; double-check at implementation time. `IToolNTM` is a trivial one-method default-method marker.

### Open questions / risks
- Baubles -> Curios decision affects 4+ files here (`ItemDosimeter` etc.) and more across the mod - flagged as needing one up-front decision.
- `ItemPollutionDetector`'s `PollutionHandler` system is not currently scoped anywhere in Phase 0/1 - worth flagging to whoever owns cross-cutting systems.
- `ItemBoltgun` is classified by actual behavior (ranged weapon dealing 10 armor-piercing damage) not its "nail gun" flavor text - a reminder that naming can mislead classification throughout this package.
- File count reconciles exactly: (a) ~46 + (b) ~24 + (c) ~19 + (d) 11 + 2 marker interfaces = 97/95+2.

---

## items_machine.md
*(`com.hbm.items.machine`, 53 files: 52 items + 1 interface)*

### Phase-1-safe scope (43 "port now")
Full list with metadata notes: `ItemArcElectrode` (4 variants), `ItemArcElectrodeBurnt` (4), `ItemBattery` (no variants, `@Deprecated` in CE but keep for exact `ModItems` names), `ItemBatteryCreative`, `ItemBatteryPack` (12 variants: 6 batteries + 6 capacitors), `ItemBatterySC` (10), `ItemBlades` (per-instance), `ItemBlueprintFolder` (3 variants), `ItemBlueprints` (no flattening - `pool` string -> data component), `ItemBreedingRod` (17 variants), `ItemCapacitor`, `ItemCassette` (do not flatten, open registry - see decision below), `ItemCatalyst`, `ItemChemicalDye` (16 variants x2 base items), `ItemDrillbit` (N variants per `EnumDrillType`), `ItemFELCrystal`, `ItemFluidIcon` (do not flatten - fluid-container note), `ItemFluidTank` (do not flatten), `ItemFluidTankV2` (do not flatten, uses Forge fluid-handler item capability -> maps to NeoForge's `IFluidHandlerItem`), `ItemFuelRod` (base class), `ItemGear` (2 variants), `ItemICFPellet` (2 enum selections in NBT -> data component), `ItemLens`, `ItemMachineUpgrade`, `ItemMold` (~20 shapes, coordinate with material-shape pipeline; drop `moldId` as single int, not per-material), `ItemPACoil` (4 variants), `ItemPWRFuel` (15 variants), `ItemPileRod` (base), `ItemPileRodMK2` (9 variants), `ItemPistons` (4 variants), `ItemPlateFuel`, `ItemRBMKPellet` (do not flatten depletion dimension - see decision below), `ItemRTGPellet`, `ItemSatChip` (9 named instances), `ItemSatellite` (14 variants), `ItemScraps` (extends `ItemAutogen` - coordinate with material/shape pipeline owner), `ItemStamp`, `ItemStampBook` (8 variants), `ItemTurretBiometry` (NBT name list -> data component), `ItemTurretChip`, `ItemWatzPellet` (12 variants x 2 base = 24), `ItemZirnoxRod` (11 variants), `ItemZirnoxRodDepleted` (9 variants).

### Deferred scope (9, all Phase 2)
- `IItemFluidIdentifier` (trivial itself, but only implementor `ItemFluidIDMulti` needs the pipe network)
- `ItemFFFluidDuct` - places `ModBlocks.fluid_duct_neo`, casts TE to `TileEntityPipeBaseNT`
- `ItemFluidIDMulti` - GUI + flood-fill against `TileEntityPipeBaseNT`
- `ItemFluidSiphon` - requires `IFluidStandardReceiverMK2` machine TE
- `ItemMuffler` - flips `TileEntityLoadedBase.muffled` on a machine TE
- `ItemPWRPrinter` - flood-fills `BlockPWR`/`TileEntityPWRController`/`TileEntityBlockPWR`
- `ItemRBMKLid` - mutates `RBMKBase`/`TileEntityRBMKBase`
- `ItemReactorSensor` - only works on `ModBlocks.reactor_research`
- `ItemRBMKRod` - **partial/flagged**: item's own physics (`burn`, `updateHeat`, `provideHeat`, xenon/depletion math) is pure NBT-over-ItemStack with zero TE refs, but imports `RBMKDials`/`IRBMKFluxReceiver.NType` from the RBMK package so it can't compile standalone. Report recommends deferring the whole class alongside RBMK (option a) rather than splitting into a Phase-1 core + Phase-2 extension (option b), since it's one class and splitting adds complexity for a single file.

**Not actually an item (1)**: `ItemDrive` - bare static nested enum `EnumDriveType` (13 values), no fields/registration - port the enum, find its real consumer elsewhere (likely `items.special`).

### Key design/API decisions
- **Fluid-backed items should NOT be flattened per-fluid** (`ItemFluidTank`, `ItemFluidTankV2`, `ItemFluidIcon`) - recommend single `fluid_tank_<size>`/`fluid_icon` item per container variant with fluid type+amount(+pressure) as a data component, mirroring `ItemFluidTankV2`'s existing Forge fluid-handler-item capability mapping onto NeoForge's `IFluidHandlerItem`. Explicit exception to the flattening rule since the fluid registry is open-ended (100+ entries, growing).
- **`ItemCassette`'s `TrackType`** is a dynamically-registered pseudo-registry (`TrackType.register(...)`, `AtomicInteger`-assigned ids) - cannot be flattened into a fixed set. Recommend single `cassette` item + data component holding a track reference.
- **`ItemRBMKPellet`** mixes two dimensions in one metadata value: fuel type (few instances) and 0-9 depletion/xenon stage (`meta % 5`/`meta >= 5`). Recommend flattening only the fuel-type dimension into distinct items; keep depletion/xenon as a data component (byte) per fuel-type item.
- **`ItemScraps`/`ItemMold`** are really part of the material/shape item-generation pipeline, not bespoke machine content - coordinate with whichever area owns generic per-material item registration.
- **NBT -> Data Component keys list**: `charge` (batteries), `dura`/`durability` (capacitor, arc electrode), `life`/`depletion` (fuel rods, pile rods, Zirnox rods, ICF pellets - **same key name `depletion` reused across 3+ unrelated classes with 3 different meanings, keep as separate component types**), `yield`/`xenon`/`core`/`hull` (RBMK rod), `pool` (blueprints), `x`/`y`/`z` (reactor sensor, natural `BlockPos` component), `playercount`/`player_N` (turret biometry, list-typed component), `fluid1`/`fluid2` (fluid identifier), `fill`/`pressure` (fluid icon), `type1`/`type2`/`muon` (ICF pellet), `PELLET_DEPLETION` (RTG pellet).

### Open questions / risks
- `ItemMachineUpgrade`'s `IUpgradeInfoProvider` tooltip lookup is defensive/null-checked and optional - item is fully functional without any machine present, don't over-defer it.
- `ItemTurretChip` has an author-left `//FIXME...?` marking an unclear split from its parent `ItemTurretBiometry` - worth a second look but nothing here requires a TE.
- Package name "machine" is misleading - the report's headline finding is that the vast majority of these are self-contained data/NBT items despite the package name, and this should inform how work packages are described to avoid over-deferring them.

---

## items_block_fluid_gas.md
*(`items/block` 4 files, `blocks/fluid` 14 files, `blocks/gas` 10 files = 28 total)*

### Phase-1-safe scope
**`blocks/gas` - 2 files, confirmed safe now**: `BlockGasFlammable` (pure vanilla fire-source detection/combustion propagation, only extra dep is `GeneralConfig.enableFlammableGas` which exists), `BlockGasExplosive` (extends `BlockGasFlammable`, only extra dep is `GeneralConfig.enableExplosiveGas`, pure vanilla explosion/flood-fill logic). Also port `BlockGasBase` (abstract parent, shape-wise safe; one minor cosmetic feature - `ArmorUtil`-gated particle effect - can ship without it and backfill).

Nothing in `items/block` or `blocks/fluid` is Phase-1-safe (all deferred, see below).

### Deferred scope
- **`items/block` (all 4, ALL DEFER)**: `ItemBlockBase.java` (blocked on `IPersistentNBT`/`IBlockMulti`/`BlockMetalFence` consumers - no single caller yet); `ItemBlockSpecialAABB.java` (blocked on any block implementing `IBlockSpecialPlacementAABB`, none exist yet); `ItemBlockStorageCrate.java` (blocked on `BlockStorageCrate`/`TileEntityCrate`/`HandHeldTileEntityCrate`/`IGUIProvider`, and on Forge's `FMLNetworkHandler.openGui` which has zero NeoForge 1.21 equivalent); `ItemCustomMachine.java` (blocked on `CustomMachineConfigJSON`, a JSON-datapack machine registry - genuinely metadata-multi in spirit, `getSubItems` over `CustomMachineConfigJSON.niceList`).
- **`blocks/fluid` (all 14, ALL DEFER on one shared blocker)**: single blocker is **no NeoForge world-fluid framework exists in the port** (`BlockFluidClassic`/`BlockFluidFinite` have zero 1.21 equivalent; `com.hbm.inventory.fluid.*` is a different HBM-internal tank abstraction, not the world-fluid system). Files: `ModFluids.java` (registers 8-9 fluids), `FluidNTM.java`, `IFluidFog.java` (trivial, ports mechanically), `FluidFogHandler.java` (NeoForge renamed the fog events - `ViewportEvent.RenderFog`/`ComputeFogColor`, `Camera` replaces `ActiveRenderInfo` - verify exact names), `GenericFluidBlock.java` (+missing `AdvancementManager`, secondary), `AcidBlock`/`MudBlock`/`ToxicBlock`/`SchrabidicBlock` (need `ContaminationUtil`/`ArmorUtil`), `VolcanicBlock`/`RadBlock`/`CoriumBlock`/`CoriumFinite` (Phase 2/3-scale, depend on many not-yet-ported sibling blocks), `GenericFiniteFluid.java`.
- **`blocks/gas` (7 of 10, DEFER as a group)**: `BlockGasAsbestos` (`ContaminationUtil.applyAsbestos`), `BlockGasCoal` (`ContaminationUtil.applyCoal`), `BlockGasMonoxide` (`ArmorUtil.damageGasMaskFilter` missing; damage side `ModDamageTypes.MONOXIDE` already exists), `BlockGasRadon`/`BlockGasRadonDense` (`ArmorUtil.damageGasMaskFilter` + `ContaminationUtil.contaminate`), `BlockGasRadonTomb` (same + `HbmPotion.radaway`/`.radx`), `BlockGasMeltdown` (`ContaminationUtil.contaminate`, `HbmPotion.radiation`, `ArmorUtil`, `HbmLivingProps.incrementAsbestos` - check if Phase 0's `HbmLivingAttachment` covers this under a new name).

### Key design/API decisions
- **Two independent, non-overlapping blockers gate this entire triage area**: (1) a real NeoForge world-fluid framework (blocks 12/14 fluid files outright), (2) a contamination/armor-hazard utility layer (`ContaminationUtil`/`ArmorUtil` equivalents - blocks 7/10 gas files and 6/14 fluid files). Recommendation: sequence one Phase 1 area to build each utility before the dependent areas are attempted, since both are shared prerequisites other areas need too.
- Confirmed dependency-exists table (verified by grep against the port, not assumed): `IBlockMulti`/`IPersistentInfoProvider`/`ITooltipProvider`/`IBlockSpecialPlacementAABB` exist; `ModDamageSource`->`ModDamageTypes` exists (`ACID`, `RADIATION`, `MUD_POISONING`, `MONOXIDE`, `ASBESTOS`); `GeneralConfig`/`ArmorRegistry` exist; `com.hbm.inventory.fluid.*` exists. Does NOT exist: world-fluid framework, `BlockMetalFence`, `IPersistentNBT`, `BlockStorageCrate`, `CustomMachineConfigJSON`, `TileEntityCrate`, `IGUIProvider`, `ContaminationUtil`, `ChunkRadiationManager`, `ArmorUtil`, `HbmPotion`, `AdvancementManager`.
- **Neo Edition reference confirms target shapes**: has 1:1-ported all 10 `blocks/gas` classes using plain vanilla `Block`/`BlockState.scheduleTick` (confirmed real pattern to copy), and has a working `com.hbm.fluids` package (`NtmFluids`, `NtmFluidTypes`) plus 2 of 14 fluid blocks (`VolcanicLiquidBlock`, `RadLiquidBlock`) as a real 1.21 API-shape model, though only 2/8 fluids are covered so far.
- **`ItemCustomMachine`** needs a design call from whoever picks it up: either (a) N real registry entries generated from JSON config at datagen/registration time, or (b) one item + a data component holding the machine config's resource id with `CreativeModeTab.Builder` populate callback instead of `getSubItems`. Since config is user-supplied runtime JSON, likely needs id-in-component design, not static datagen.
- `ItemBlockSpecialAABB`'s `onItemUse` hand-reimplements vanilla placement logic - check whether `BlockItem.getPlacementState`/`place` extension points cover it in 1.21 instead of a full manual reimplementation (genuinely ambiguous, decide when porting the first AABB block).

### Open questions / risks
- No metadata-driven-multi items in this whole triage area except `ItemCustomMachine` (a runtime-JSON-driven case, not a fixed material list - materially different problem).
- `IPersistentNBT.NBT_PERSISTENT_KEY` needs Data Component treatment whenever `ItemBlockBase`/`ItemBlockStorageCrate` get picked up.
- `blocks/gas`'s hazard/contamination blocker overlaps `blocks/fluid`'s - whoever builds the contamination/armor utility unblocks both packages at once; flagged explicitly as a cross-package dependency worth sequencing deliberately.

---

## blocks_network_rail.md
*(`blocks/network` 76 files, `blocks/rail` 13 files)*

### Phase-1-safe scope
**None.** The report's headline verdict: zero Phase-1-safe blocks in either package.

### Deferred scope
- **`blocks/network` (74 real blocks + 1 interface + 1 render-utility, confirmed Phase 2 per PORT_SPEC.md's own text, "logistics: cables + energy net, fluid ducts, item conveyors/crane inserters")**. Breakdown by coupling:
  - `extends BlockContainer`/`BlockContainerBakeable`/`BlockBakeBase` (needs a TE), ~55 files: `PneumoTube`, `FluidDuctBase` + 8 subclasses, `energy/BlockCable`, `energy/CableDiode`, `DroneCrate`, `DroneDock`, `RadioTorchBase` hierarchy.
  - `extends BlockDummyable` (multiblock framework), 8 files: `MachineBatteryREDD`, `MachineBatterySocket`, `PylonMedium`, `RadioAUTOCAL`, `RadioTelex`, `energy/PylonLarge`, `energy/Substation`, `CraneSplitter`.
  - `extends Block` directly but coupled to custom `Entity` (`EntityMovingItem`), 8 files: `BlockConveyor`, `BlockConveyorBase`/`Bendable`/`Chute`/`Double`/`Express`/`Lift`/`Triple` - the "near miss" (structurally simplest, but blocked on the unbuilt `EntityMovingItem` entity, which is out of Phase 1 scope per PORT_SPEC's entity/Phase-4 boundary). Recommended as the **lightest-weight slice within Phase 2**, right after `EntityMovingItem`/`EntityMovingConveyorObject` are ported.
  - Not blocks at all: `IBlockFluidDuct` (marker interface), `SimpleUnlistedProperty<T>` (Forge-1.12 `IUnlistedProperty`, no NeoForge equivalent - exclude from the 76 count).
  - 1 file: `energy/PowerCableBox` (`BlockContainer implements ITileEntityProvider`).
  - 5 files additionally need a Phase 2 menu/screen (`IGUIProvider`): `FluidPump`, `RadioTorchController`, `RadioTorchCounter`, `RadioTorchLogic`, `RadioTorchReader`, plus `energy/CableDiode`.
  - Companion evidence: 54 TileEntity classes in `com.hbm.tileentity.network` back this package, none ported yet.
- **`blocks/rail` (13 files) - not cleanly assigned to any existing phase, needs an explicit decision**: every file extends `BlockDummyable` (`IRailNTM.java` interface; `BlockRailWaypointSystem.java`; `RailNarrowCurve`/`Straight`; `RailStandardCurveBase`/`Wide7`/`Wide9`/`StraightShort`/`Straight`/`Buffer`/`Ramp`; `RailStandardSwitch`/`SwitchFlipped`). All also need a train entity subsystem (`com.hbm.entity.train.EntityRailCarBase`/`EntityRailCarCargo`/`EntityRailCarElectric`/`EntityRailCarRidable`/`TrainCargoTram`/`TrainCargoTramTrailer`) not scoped anywhere in Phase 0. **Recommendation: treat `blocks/rail` + `entity/train` + `tileentity/rail` as one dedicated work package**, gated on the Phase 2 multiblock framework, scheduled alongside or after Phase 4's custom-entity/vehicle work - do not silently fold into Phase 1 or assume "Phase 4 leftovers."

### Key design/API decisions
- The item-metadata-flattening ground rule **does not apply** to this package - blockstate properties (`PropertyDirection FACING`, `BlockConveyorChute.TYPE`) are a live, supported modern-Minecraft mechanism (`DirectionProperty`, `IntegerProperty`), not the pre-1.13 damage-value pattern. Confirmed via `ModBlocks.java`: network classes instantiate ~1:1 with their file, no hidden per-material explosion.
- `IConveyorBelt` and `IToolable` interfaces are **already ported** in Phase 0 (`com.hbm.api.conveyor.IConveyorBelt`, `com.hbm.api.block.IToolable`) - that part of the dependency graph is closed already.
- No ItemStack NBT of note in this package (mostly Block/TileEntity logic) - flag `FluidDuctPaintable`, `FluidDuctPaintableBlockExhaust`, `energy/BlockCablePaintable`, `PneumoTubePaintableBlock`, `BlockOpenComputersCablePaintable` (`IFacade`/paint state) for cross-reference with whoever covers `com.hbm.items.util` paint tools - unresolved whether that's TE NBT (no componentization needed) or ItemStack NBT (would need it).

### Open questions / risks
- **Explicit call needed**: `blocks/rail`'s phase ownership is a genuine gap in PORT_SPEC.md - neither Phase 1 nor cleanly Phase 4 fits (rail blocks are structural/multiblock = Phase 2 flavor; trains are "vehicles" = Phase 4 flavor). The report insists this be raised explicitly during Phase 2-vs-4 planning, not silently defaulted.
- Do not schedule any file from either package into Phase 1 - zero exceptions, per the report's own summary.

---

## datagen_framework.md
*(infrastructure research - no CE equivalent exists to survey; designed from the Neo Edition reference + Phase 0 precedent)*

### Phase-1-safe scope
This report is pure infrastructure design, not a file-by-file CE triage. What Phase 1 must **create** (from the summary section):
1. `com.hbm.items.datagen.ModItemModelProvider extends ItemModelProvider`
2. `com.hbm.blocks.datagen.ModBlockStateProvider extends BlockStateProvider`
3. `com.hbm.blocks.datagen.ModBlockTagProvider extends BlockTagsProvider`
4. `com.hbm.items.datagen.ModItemTagProvider extends ItemTagsProvider`
5. `com.hbm.blocks.datagen.ModBlockLootTableProvider extends BlockLootSubProvider`
6. `com.hbm.datagen.ModLanguageProvider extends LanguageProvider`
7. `com.hbm.datagen.ModDataGenerators` (the `GatherDataEvent` subscriber wiring all the above, plus plugging in the already-existing but never-wired-up `com.hbm.damage.datagen.ModDamageTypeTagsProvider`)
8. Port CE's `IModelRegister` marker interface into an `ICustomItemModelRegister`-shaped hook; add a block equivalent `ICustomBlockModelRegister`.

No `build.gradle` change needed - the `data` run config is already correctly configured (confirmed at lines ~33-37, `data { data(); programArguments.addAll '--mod', project.mod_id, '--all', '--output', file('src/generated/resources/')...` and `sourceSets.main.resources { srcDir 'src/generated/resources' }`).

### Deferred scope
- `ModFluidTagsProvider` - Phase 0's fluid area's concern, not Phase 1.
- `ModSoundDefinitionsProvider` - belongs to whoever owns `com.hbm.sound`.
- `ModRecipeProvider` - its own large area (CE's crafting-recipe corpus), out of scope for this report.
- Custom model loaders (`CustomLoaderBuilder` subclasses, `NtmGeometry`) - Phase 2+ concern tied to a not-yet-ported custom geometry/baked-model system.
- Advancement generation - out of scope for Phase 1 entirely.

### Key design/API decisions
- **Confirmed constructor signatures** (from Neo Edition reference, real code):
```java
public NtmItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper)
public NtmBlockStateProvider(PackOutput output, ExistingFileHelper helper)
public NtmBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, ExistingFileHelper existingFileHelper)
public NtmItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper helper)
public NtmFluidTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, ExistingFileHelper helper)
public NtmDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> provider, @Nullable ExistingFileHelper helper)
protected NtmBlockLootTableProvider(HolderLookup.Provider registries)   // only via LootTableProvider.SubProviderEntry
public NtmLanguageProvider(PackOutput output)                          // hardcoded "en_us"
protected NtmSoundDefinitionsProvider(PackOutput output, ExistingFileHelper helper)
public NtmRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries)
```
- **Package placement follows the already-committed precedent** set by Phase 0's damage_types fix pass (moved from flat `com.hbm.datagen` to `com.hbm.damage.datagen` specifically to avoid multi-area collision on one shared package): `ModItemModelProvider`/`ModItemTagProvider` -> `com.hbm.items.datagen`; `ModBlockStateProvider`/`ModBlockTagProvider`/`ModBlockLootTableProvider` -> `com.hbm.blocks.datagen`; `ModLanguageProvider` and `ModDataGenerators` stay centralized in `com.hbm.datagen` (lang is inherently cross-cutting - splitting it would cause file-path collisions in `LanguageProvider.run()` since instances sharing a locale+modid overwrite each other's output).
- **`basicItem(Item)`** (ItemModelProvider) covers the overwhelming majority of Phase 1's flat 2D material items (ingots, nuggets, dusts, plates, wires, gems). `simpleCubeAllBlock(DeferredBlock<? extends Block>)` (a small helper Phase 1 should port, not framework-provided under that name - wraps `simpleBlockWithItem(block.get(), cubeAll(block.get()))`) covers plain ore/decorative/storage blocks. `cubeBottomTopBlock`/`cubeTop`/`logBlock`/`slabBlock`/`stairsBlock` are confirmed real API for distinct-texture/log/slab/stair blocks. `getVariantBuilder(block).forAllStates(...)` for blockstate-property-driven variants.
- **`ICustomItemModelRegister`/`ICustomBlockModelRegister` marker-interface pattern confirmed real and actively used** in the reference - lets an item/block class own its own model-registration logic instead of the provider growing a giant if/else. Confirmed code:
```java
NtmBlocks.BLOCKS.getEntries().forEach(holder -> {
    Block block = holder.get();
    if (block instanceof ICustomBlockModelRegister icbmr) {
        ResourceLocation loc = Objects.requireNonNull(BuiltInRegistries.BLOCK.getKey(block));
        icbmr.registerModel(this, loc);
    }
});
```
- **`MaterialShapes.commonTag(NTMMaterial mat)`** already exists (confirmed at `MaterialShapes.java:135-141`) and is exactly what `ModItemTagProvider` should call: `this.tag(shape.commonTag(mat)).add(theGeneratedItem)` produces `c:ingots/iron` etc. `BLOCK`-shape materials need the block-side equivalent through `ModBlockTagProvider`.
- **`ModDataGenerators` skeleton given in full** (see report body) - registers a `DatapackBuiltinEntriesProvider` for damage types, then client providers (item model, blockstate, language) gated on `event.includeClient()`, then server providers (block tags, item tags needing `blockTagsProvider.contentsGetter()`, damage type tags, block loot via `LootTableProvider.SubProviderEntry`) gated on `event.includeServer()`.
- `@EventBusSubscriber(modid = MainRegistry.MODID)` + static `@SubscribeEvent void gatherData(GatherDataEvent event)` is the confirmed self-registering pattern, consistent with Phase 0's `packet` area convention.

### Open questions / risks
- **Single vs. split item-model/tag provider classes**: report recommends starting with one class per provider type, splitting only if a later phase's addition would create excessive merge-conflict risk - explicitly flagged as the report's own judgment call, not settled convention.
- **Whether to introduce `ICustomItemModelRegister`/`ICustomBlockModelRegister` in Phase 1 at all** given Phase 1 may have zero content needing non-`basicItem`/non-`simpleCubeAllBlock` generation - report recommends introducing them anyway to avoid a later retrofit, but flags this as a judgment call too.
- Exact set of Phase 1 items/blocks needing `MINEABLE_WITH_PICKAXE`/tool tags/common-tag membership depends on the CE-source-mapping areas' output, not resolved by this report.
- A `mergeWithSources` jar cached on the research machine was for a **different, newer, unrelated MC version** and was correctly excluded - noted as a "don't guess API" trap that was caught, not fallen into.

---

## hazard_bindings_plan.md
*(`HazardRegistry.registerItems()`/`registerContaminatingDrops()`, currently empty no-ops)*

### Phase-1-safe scope
**Phase-1-appropriate now**: Pattern A entries whose backing item is a plain `Item`/`BlockItem` with no custom logic (dynamite sticks, waste blocks/ingots/billets/nuggets, ore fragments, crystals, nuke core/propellant items, debris items, sellafield-tier items once expanded, yellowcake, fallout, asbestos brick, etc.), and all of Pattern B (material-shape fuel/waste families, ~40 calls) and Pattern C (tag/ore-dict registration, ~5 calls + ore blacklist).

Concrete example call shape once an item exists:
```java
HazardSystem.register(ModItems.NUCLEAR_WASTE_LONG.get(), makeData(RADIATION, level));
```

Zero new hazard-system plumbing needed - `HazardSystem.register(...)` (7 overloads: `TagKey<Item>`, `String` tagName, `Item`, `Block`, `ResourceLocation`, `ItemStack`, `ComparableStack`, `Object`), `HazardData`/`HazardEntry`/`IHazardModifier`, and the 4 modifier classes already exist in the port, confirmed by reading the source.

### Deferred scope
**Depends on a later phase's Item subclasses existing first** (Pattern D exact-variant bindings + Pattern E parametric helpers, ~35 + ~90 calls): `ItemRTGPellet`, `ItemRBMKRod`, `ItemRBMKPellet`, `ItemZirnoxRod`/`ItemZirnoxRodDepleted`, `ItemPWRFuel`, `ItemBreedingRod`, `ItemPileRodMK2`, `ItemWatzPellet` - all stateful machine-fuel items belonging to whichever phase ports RBMK/RTG/PWR/breeder machine content. **Recommendation**: write the Pattern E helper method bodies now (`registerOtherFuel`, `registerRTGPellet`, `registerRBMKRod`/`Pellet`/base `registerRBMK`, `registerBreedingRodRadiation`, `registerPWRFuel`, `registerOtherWaste`, `registerOtherWasteContaminating`, `registerRadSourceWaste`) since they have no forward dependency, but leave the actual call sites out of Phase 1's first `registerItems()` pass, adding them incrementally as each machine-item family is ported into the *same* method (no need to split into multiple methods).

### Key design/API decisions
- **Pattern C (tag-string registration)**: CE's bare ore-dict strings (`"dustCoal"`) must NOT be passed to the port's `register(String tagName, ...)` overload as-is - it calls `ResourceLocation.parse(tagName)`, and a bare name with no colon parses wrong (`minecraft:dustcoal`). Correct port shape:
```java
HazardSystem.register(MaterialShapes.DUST.commonTag(Mats.MAT_COAL), makeData(COAL, powder));
```
Recommendation: **register against the tag, not a specific item** - correctly extends hazard coverage to any mod's compatible dust, matching CE's original cross-mod ore-dict intent (more faithful than an item-only binding).
- **Ore blacklist**: `MaterialShapes.ORE` is `.noAutogen()` with `registryName = null`, so `commonTag()` throws `IllegalStateException` for it by design - the blacklist call `HazardSystem.blacklist(MaterialShapes.ORE.commonTag(Mats.MAT_THORIUM))` as naively written **will not work**; needs whatever tag the ore-block item area actually assigns (likely a vanilla-convention `c:ores/uranium` tag) - flagged as unresolved.
- **Pattern D**: once metadata variants are flattened, all 6 affected item families (`sellafield` 6 variants, `rod_zirnox_depleted` 9 via `EnumZirnoxTypeDepleted`, `pellet_rtg_depleted` 1 via `EnumDepletedRTGMaterial`, `ModItems.pile_rod` 8 via `EnumPileRod`, `ModItems.watz_pellet` 10 via `EnumWatzType`, `ModItems.pwr_fuel*` 14 via `EnumPWRFuel`, `ModItems.rod*` 14 via `BreedingRodType`, plus `holotape_image`'s `HOLO_RESTORED` variant) fold back into simple Pattern A calls, one per expanded variant - **the single biggest external dependency this plan has on the items area**.
- **Pattern F (custom hazard type)**: `HazardData.addEntry(IHazardType)` only exists for a bare type (level defaults to 1.0); to inject a custom-typed entry at non-default level, use `HazardData.addEntry(HazardEntry)` directly. Full worked translation given for the demon-core case, including confirmed 1.21 API renames: `EntityItem`->`ItemEntity`, `.world`->`.level()`, `.isRemote`->`.isClientSide()`, `.posX/Y/Z`->`.getX()/getY()/getZ()`, `world.spawnEntity(...)`->`level().addFreshEntity(...)`.
- **`registerContaminatingDrops()`**: port's `HazardSystem` has no `oreMap` equivalent to CE's - use the public `HazardSystem.tagMap` (`Map<TagKey<Item>, HazardData>`, confirmed public) via `computeIfPresent`, plus `itemMap.computeIfPresent(...)` for direct items (e.g. `ModItems.POWDER_BALEFIRE.get()`). **Must run strictly after `registerItems()`** - `computeIfPresent` is a silent no-op if the key isn't present yet; this ordering is already the documented Phase 0 handoff intent.
- Modifier classes with confirmed constructors, already in the port: `HazardModifierFuelRadiation(double target)`, `HazardModifierRTGRadiation(double target)`, `HazardModifierRBMKRadiation(double target, boolean linear)`, `HazardModifierRBMKHot()`, `HazardTypeDangerousDrop(ObjDoubleConsumer<ItemEntity> onDrop)`.

### Open questions / risks
- Several `OreDictManager` constants CE's `registerContaminatingDrops()` uses don't have an obviously-matching `Mats.java` name: `TCALLOY`, `SBD`, `TS` (only `MAT_RADIUM`, `MAT_ACTINIUM`, `MAT_SCHRABIDIUM`, `MAT_STRONTIUM` were confirmed to exist) - flagged explicitly as needing re-verification against the finished `Mats.java`, not to be trusted from the illustrative names given.
- Whether "fuel" material variants (uranium_fuel, plutonium_fuel, etc., ~9 families x ~4 shapes = ~36 items) become their own `NTMMaterial` constants in `Mats.java` or stay bespoke `ModItems` fields is an open items-area decision this plan does not resolve but flags as a blocking dependency for Pattern B binding.
- Ore blacklist tag identity (see above) needs to be resolved when ore blocks are actually registered - explicitly "don't guess it now."

---

## creative_tabs_plan.md
*(Creative tab population plan matching CE ordering)*

### Phase-1-safe scope
This is a mechanism-design report, not a file triage, but its concrete deliverables for Phase 1 are:
1. Keep Phase 0's `ModCreativeTabs.java` tab set/order/`withTabsBefore` chain exactly as-is - already matches CE's `MainRegistry.getNextID()` sequence: `partsTab, controlTab, templateTab, resourceTab, blockTab, machineTab, nukeTab, missileTab, weaponTab, consumableTab` = port's `PARTS, CONTROL, TEMPLATE, RESOURCE, BLOCKS, MACHINE, NUKE, MISSILE, WEAPON, CONSUMABLE`.
2. Add a `CreativeTabContents` holder class in `com.hbm.creativetabs` (full code given, see below).
3. Wire the `Mats x MaterialShapes` bulk generation loop to call `CreativeTabContents.add(tabFor(shape), registeredSupplier)` - covers the majority of CE's 1484 (`ModItems`) + 1084 (`ModBlocks`) `setCreativeTab` call sites automatically, with zero further manual work as materials grow.
4. For hand-authored items/blocks, add one `output.accept(ModItems.X.get())` line inside the relevant tab's `displayItems` lambda in `ModCreativeTabs.java`, authored at the same time as the item - keeps `ModItems.java`/`ModBlocks.java` free of tab-shaped code per Phase 0's existing stated boundary.
5. Implement CONTROL's battery full/empty split and MISSILE's 9 curated showcase stacks as bespoke extra code in those two tabs' lambdas (see below).
6. Wire TEMPLATE's `.withSearchBar()` and TEMPLATE/NUKE's `.backgroundTexture(...)` once the textures exist.

### Deferred scope
Not phase-gated in the traditional sense (this is infrastructure), but explicitly deferred/rejected:
- **Do not copy Neo Edition's `NtmCreativeTabs.java` file structure** - it only implements 8 of CE's 10 tabs (silently drops `TEMPLATE` and `WEAPON` entirely, stubbed `/** SKIP */`), and silently merges CE's `RESOURCE` tab into `BLOCKS` (contradicting confirmed CE source where `ore_uranium.setCreativeTab(resourceTab)` differs from `block_steel.setCreativeTab(blockTab)`). Phase 0's ground rules call for 99% parity across all 10 tabs with the RESOURCE/BLOCKS split preserved.
- The NeoForge convenience overload `Builder.displayItems(Collection<? extends Holder<? extends ItemLike>>)` (for dumping a whole `DeferredRegister` into one tab) was **evaluated and rejected** - only works with one dedicated `DeferredRegister` per tab, but Phase 0 already committed to one shared `ModItems.ITEMS`/`ModBlocks.BLOCKS` register across all ten tabs.

### Key design/API decisions
- **Confirmed real `CreativeModeTab.Builder` API** (extracted directly from this machine's NeoGradle cache for the exact 1.21.1+NeoForge 21.1.228 toolchain, cross-checked by timestamp against a mismatched jar that was correctly rejected):
```java
CreativeModeTab.builder()
    .icon(Supplier<ItemStack>)
    .title(Component)
    .withTabsBefore(ResourceLocation...)   // or ResourceKey<CreativeModeTab>...
    .backgroundTexture(ResourceLocation)
    .withSearchBar()                        // also .withSearchBar(int width)
    .displayItems(CreativeModeTab.DisplayItemsGenerator)
    .build()

interface DisplayItemsGenerator { void accept(ItemDisplayParameters parameters, Output output); }
interface Output {
    void accept(ItemStack stack, TabVisibility visibility);
    default void accept(ItemStack stack);                    // PARENT_AND_SEARCH_TABS
    default void accept(ItemLike item, TabVisibility visibility);
    default void accept(ItemLike item);
    default void acceptAll(Collection<ItemStack> stacks, TabVisibility visibility);
    default void acceptAll(Collection<ItemStack> stacks);
}
enum TabVisibility { PARENT_AND_SEARCH_TABS, PARENT_TAB_ONLY, SEARCH_TAB_ONLY }
```
`buildContents` runs via `BuildCreativeModeTabContentsEvent`, safely after all `DeferredRegister`s have fired, so lambda bodies referencing `.get()` are always safe regardless of static-init order.
- **`CreativeTabContents` shape (given in full)**:
```java
package com.hbm.creativetabs;
final class CreativeTabContents {
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> BY_TAB = new HashMap<>();
    static void add(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        BY_TAB.computeIfAbsent(tab, k -> new ArrayList<>()).add(item);
    }
    static void flush(ResourceKey<CreativeModeTab> tab, CreativeModeTab.Output output) {
        BY_TAB.getOrDefault(tab, List.of()).forEach(supplier -> output.accept(supplier.get()));
    }
    private CreativeTabContents() {}
}
```
Works because `DeferredItem<Item>`/`DeferredBlock<Block>` extend `DeferredHolder<R,T>` which implements `Supplier<T>`, and `Item`/`Block` implement `ItemLike`.
- **Confirmed shape-to-tab mapping table** (spot-checked in `ModItems.java`/`ModBlocks.java`): `ingot_*`/`plate_*`/nugget/wire/billet/gem/crystal/dust/dense_wire/shell/pipe -> PARTS; `block_*` (metal storage) -> BLOCKS; `ore_*` -> RESOURCE. Maps 1:1 onto Phase 0's `MaterialShapes` constants (`INGOT`, `PLATE`, `NUGGET`, `WIRE`, `BILLET`, `GEM`, `CRYSTAL`, `DUST`, `DENSEWIRE`, `SHELL`, `PIPE`, `BLOCK`).
- **CONTROL tab bespoke logic**: CE's `ControlTab.displayAllRelevantItems` finds every `IBatteryItem` stack, replaces with a full-charge copy plus (only if `getChargeRate(stack) > 0`, i.e. not SU-only) a preceding empty-charge copy. Port equivalent: after flushing CONTROL's generic list, walk the battery items again and `output.accept(...)` full/empty `ItemStack`s using `ItemStack.set(componentType, chargeValue)` once the battery data-component design lands.
- **MISSILE tab bespoke logic**: CE's `MissileTab` adds 9 hand-built showcase stacks via `ItemCustomMissile.buildMissile(chip, warhead, fuselage, stability, thruster)` with custom colored names ("Lil Bub", "Uncle Kim", etc.) - port as 9 explicit `output.accept(...)` calls or one `addShowcaseMissile(...)` helper, not a candidate for the generic list mechanism.
- **TEMPLATE/NUKE**: `.withSearchBar()` for TEMPLATE (defaults background to vanilla `item_search` unless overridden); `.backgroundTexture(...)` pointed at ported `item_search.png`/`nuke.png` respectively.

### Open questions / risks
- Explicitly flagged tension between the task brief's suggested "shared list appended at registration" mechanism and Phase 0's already-shipped `ModItems.java` doc comment stating creative-tab placement is "deliberately NOT part of this class" and items should not have a `setCreativeTab`-shaped API bolted back on. Report resolves this by splitting by provenance (generated material-shape items get automatic list population inside the generation loop; hand-authored items get one line added to `ModCreativeTabs.java` at authoring time) - flagged explicitly as "not silently picking a side" per the ground rules, but is the report's own reasoned recommendation, not a pre-existing decision.
- Exact interleaving order of generated-vs-hand-authored items within a tab is acknowledged as an approximation ("generated materials first, then hand-authored") rather than a guaranteed line-for-line CE match - flagged as unlikely to matter in practice but not proven equivalent.
- `ModCreativeTabs.java` may need to be split into a sibling class (e.g. `CreativeTabContents`) if it grows too large, given Neo Edition's equivalent file is 1325 lines - not decided, just flagged as a real concern.agentId: a0c29ddad3757ad56 (use SendMessage with to: 'a0c29ddad3757ad56', summary: '<5-10 word recap>' to continue this agent)
<usage>subagent_tokens: 187161
tool_uses: 11
duration_ms: 253922</usage>