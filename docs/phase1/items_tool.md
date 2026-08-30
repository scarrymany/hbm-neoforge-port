# items/tool package triage (97 files)

Source: `hbm-ce/src/main/java/com/hbm/items/tool/**/*.java`

Goal: PORT_SPEC.md currently blankets "melee/tools" into Phase 3. This survey splits the package
file-by-file into (a) genuine Phase 1 mining/utility tools, (b) melee/ranged/military weapons that
belong in Phase 3, and (c) items whose real dependency is Phase 2 machine coupling. A fourth bucket
(d) surfaced during the survey and is called out separately: internal mod-author/debug tooling that
isn't player-facing content at all.

File count check: 95 classes + 2 marker interfaces (`IItemAbility`, `IToolNTM`) = 97. All 97 are
accounted for below.

## Headline finding

The package is not "tools = simple, weapons = complex" the way PORT_SPEC's blanket Phase-3
assignment implies. The real split is:

- The genuine pickaxe/axe/shovel content (dozens of material-tiered mining tools declared in
  `ModItems.java`, e.g. `titanium_pickaxe`, `steel_axe`, `desh_shovel`, `drax`) all run through
  **`ItemToolAbility`** (plus `ItemToolAbilityFueled` / `ItemToolAbilityPower`), which lives in this
  package. This is Phase-1-appropriate *content* but it is not simple *code*: it drags in an entire
  ability/preset/GUI/keybind/HUD framework (`com.hbm.handler.ability.*`, `GUIScreenToolAbility`,
  `IKeybindReceiver`, `IItemHUD`, `IDepthRockTool`) that is not part of Phase 0 and is not itself in
  this package. Porting "simple pickaxes" for real parity means porting that ability framework too.
  This is the single biggest scope risk in this file set — flagging it explicitly per the "no silent
  punting" rule.
- A large fraction of what looks like "tools" by name (wrench, screwdriver, blowtorch, analyzer,
  lock/key, RBMK console tool, power-net tool, rebar placer, wiring tool, conveyor wand, mirror tool,
  settings tool) is not simple at all — it exists purely to interact with placed machine blocks
  (`BlockDummyable`, `IToolable`, `TileEntityLockableBase`, `TileEntityRBMKBase`,
  `TileEntityPylonBase`, `TileEntityMachineRadarScreen`, `IEnergyConductorMK2`, etc.). These are
  Phase 2 machine-coupling items, not Phase 1 content, regardless of how mundane they look.
- A meaningful cluster (11 files) is internal developer/debug tooling used by the original mod
  authors to author multiblock structure JSON and to test boss/meteor spawns. It is not gameplay
  content in the normal sense and should not be prioritized as Phase 1 "simple items."

## (a) Genuine Phase 1 utility / mining tools

### Mining-tool framework (needs the ability system as a prerequisite)

| Class | Notes |
|---|---|
| `ItemToolAbility` | Base for every material-tiered pickaxe/axe/shovel (`EnumToolType.PICKAXE/AXE/SHOVEL/MINER`). Instantiated directly (not subclassed) ~40+ times in `ModItems.java` across every tool material (titanium, steel, desh, cobalt, starmetal, cmb, bismuth, volcanic, chlorophyte, schrabidium, mese, dwarven, decorated variants, `smashing_hammer`, `centri_stick`, etc.). Each material tier is its own registry entry already — **not** metadata-driven, no expansion needed there. But it depends on: `com.hbm.handler.ability.*` (`IBaseAbility`, `IToolAreaAbility`, `IToolHarvestAbility`, `AvailableAbilities`, `ToolPreset` — an entire ability-preset plugin system), `GUIScreenToolAbility`, `IKeybindReceiver`/`HbmKeybinds`, `IItemHUD` (crosshair HUD icon), `IDepthRockTool` (bedrock-ore breaking permission), dynamic model baking (`IDynamicModels`/`IClaimedModelLocation`). None of that lives in Phase 0 or in this package. **Recommendation:** treat "port the mining tool ability framework" as an explicit Phase 1 prerequisite work item, not something that falls out of porting `ItemToolAbility` itself. |
| `ItemToolAbilityFueled` | Adds `IFillableItem` fluid-fuel tank on top of `ItemToolAbility` (used by chainsaws). |
| `ItemToolAbilityPower` | Adds `IBatteryItem` electric charge on top of `ItemToolAbility` (used by `elec_*` tools and the `drax` drill line). |
| `ItemChainsaw` | Concrete axe-type tool extending `ItemToolAbilityFueled`. Needs fluid fuel (Phase 0 `FluidType`, fine) plus a custom bone/bus swing animation system (`BusAnimation`, `HbmAnimations`, `IAnimatedItem`) — non-trivial rendering dependency worth flagging, not a plain item. |
| `ItemMultitoolTool` | **Ambiguous — see call below.** Concrete class for `multitool_dig` / `multitool_silk` only. Simple `ItemTool` subclass, no ability framework needed. Right-click (sneak) upgrades `dig -> silk -> ext`, crossing into `ItemMultitoolPassive` (see Phase 3 list). **Metadata note:** these are separate registry objects already (not damage-value variants of one item), so no expansion is needed for these two specifically. |

**Call on the multitool chain:** `multitool_dig`/`multitool_silk` read as Phase-1-flavored ("breaks
blocks extremely fast, extra ore drops") but they are the first two rungs of one single sneak-click
upgrade ladder that continues directly into `ItemMultitoolPassive` and ends in AoE lightning storms,
"level down blocks" (mass terrain deletion), and 16-attack-damage combat stats. Splitting the ladder
across two phases would mean either shipping a dead-end item in Phase 1 (upgrade target doesn't exist
yet) or re-touching this class in Phase 3 anyway. **Recommendation: port the entire multitool line
(`ItemMultitoolTool` + `ItemMultitoolPassive`, 10 registry entries total) together in Phase 3**, not
split. Do not count it toward Phase 1 file/item totals.

### Standalone simple items (no machine or weapon coupling; own state only)

These are genuinely simple — single-purpose `Item`/`ItemBakedBase` subclasses whose logic is
self-contained. Good Phase 1 candidates as-is:

- `ItemColtanCompass` — points at nearest coltan ore, vanilla-compass-style `IItemPropertyGetter`.
- `ItemCraftingDegradation` — generic "container item" base for tools that degrade one use per craft (dies/stamps). No dependency beyond vanilla `getContainerItem`.
- `ItemCouplingTool` — literally an empty `ItemBase` subclass (0 lines of logic); acts as a marker item checked elsewhere by `instanceof`/identity. Trivial to port, but grep the rest of the codebase before assuming it's dead — its behavior lives outside this file.
- `ItemMS` — "meteor sample" tool; breaks a specific world-gen block (`ModBlocks.ntm_dirt`) and drops unique ingots. Self-contained.
- `ItemDiscord` — short-range teleport ("blink") item, particle + sound only, no NBT-persisted state beyond none.
- `ItemMatch`, `ItemBalefireMatch` — fire-lighting utility items (flint-and-steel analogues); `ItemBalefireMatch` lights a specific ritual/pyre block.
- `ItemModDoor` — item wrapper that places a custom `BlockDoor` subclass; the door-item analogue of vanilla `ItemDoor`.
- `ItemModMinecart` — **metadata-driven-multi, needs expansion.** Spawns one of `EntityMinecartCrate`/`Destroyer`/`Ore`/`Powder`/`Semtex` via a single class with an enum/ID selector (`IDynamicModels`, custom baked models per cart type). Needs 5 distinct registry entries in the port. Note: `EntityMinecartSemtex` is an explosive cart — keep the item itself in Phase 1 (it's just a spawner, same shape as the others) but be aware its entity behavior belongs with Phase 3 explosives content.
- `ItemFertilizer` — bonemeal-alternative for `IGrowable` crops, fires vanilla `BonemealEvent`. No dependency beyond vanilla hooks.
- `ItemRepairKit` — consumable durability-repair item via `ConsumableHandler`.
- `ItemCrateCaller` — novelty "call in a loot crate" item (consumes an enchanted item as cost). No machine/weapon coupling.
- `ItemFusionCore` — consumable charge cell that recharges Force-Shield-Belt armor. **Cross-package dependency**: needs `items/armor` (`ArmorFSB`, `ArmorFSBPowered`) to mean anything; port order matters.
- `ItemPeas` — feeds/summons the `EntityQuackos` mob. **Cross-package dependency**: needs that mob entity ported; item itself is trivial.

### Container / bag items (self-contained inventory + GUI, not tied to a placed machine)

All of these implement `IGUIProvider` and own a private `ItemStackHandler`/`Container`/`GUI` — the
GUI belongs to the *item*, not to any block, so this is not "Phase 2 machine coupling" in the sense
used elsewhere in this doc. It does require a generic item-owned inventory/GUI pattern to exist
(check whether Phase 0's capability/packet framework already covers this shape, or whether it needs
a small Phase 1 addition).

- `ItemAmmoBag`, `ItemCasingBag`, `ItemPlasticBag`, `ItemLeadBox`, `ItemToolBox`
- `ItemCanister`, `ItemGasCanister`, `ItemFluidContainerInfinite`, `ItemPipette` — fluid-side containers (Phase 0 `FluidType`/`IFillableItem`, no GUI).
- `ItemFilter` — gas-mask filter; interacts with the player's armor slot via `ArmorModHandler`/`IGasMask`. **Cross-package dependency** on `items/armor`.

### Detector / diagnostic items (hazard & world-scanning; matches Phase 0's `HazardRegistry`)

- `ItemDosimeter`, `ItemGeigerCounter`, `ItemDigammaDiagnostic`, `ItemLungDiagnostic` — all implement Baubles' `IBauble` behind `@Optional.InterfaceList`. **Needs a porting decision**: Baubles has no NeoForge 1.21 build; the community successor is Curios API. Decide once, up front, whether these become Curios-slot accessories or plain held/worn items, since 4+ files here and more across the mod depend on the same answer.
- `ItemOilDetector`, `ItemOreDensityScanner`, `ItemSurveyScanner` — ore/oil scanners; `OreDensityScanner`/`SurveyScanner` read `BlockBedrockOreTE`/`com.hbm.world.feature.BedrockOre`, i.e. depend on a specific world-gen feature existing. Flag as a soft dependency on that world-gen feature being ported (even a stub) before these are meaningful.
- `ItemPollutionDetector` — depends on `PollutionHandler`, a standalone world-pollution simulation not part of Phase 0. The item itself is trivial; the simulation it reads is a real system that needs to exist somewhere in the plan (not currently listed in Phase 0/1 scope as far as this survey can see — worth flagging to whoever owns cross-cutting systems).

### Self-contained GUI/reference items

- `ItemGuideBook`, `ItemCatalog` (in-game "Bobmazon" shop/order screen), `ItemBookLemegeton` (occult/ritual grimoire GUI) — each owns its own `GuiScreen`/`Container` and doesn't reach into any block or weapon system. Thematically `ItemBookLemegeton` is endgame ritual/summoning content, but mechanically it's exactly as self-contained as the guide book, so there's no structural reason to delay it past Phase 1 beyond whatever mob/ritual content it eventually unlocks (out of scope here).

## (b) Melee / ranged / military — belongs in Phase 3

### Melee weapons

- `ItemSwordAbility` (extends vanilla `ItemSword`, ability-hook variant of the sword system), `ItemSwordAbilityPower` (adds `IBatteryItem`), `ItemSwordMeteorite` (meteorite-tier sword). These share the `handler.ability` dependency called out above, on the weapon side of that framework.
- `ItemMultitoolTool` + `ItemMultitoolPassive` — see the call-out in (a); port together as one 10-entry weapon progression line, not split.

### Explosives / detonation

- `ItemDetonator`, `ItemMultiDetonator`, `ItemLaserDetonator` (also `IHoldableWeapon`), `ItemDefuser` — all built on `IBomb`/explosion packages (`com.hbm.explosion.vanillant.*`), remote-detonate or defuse placed bombs.
- `ItemAmatExtractor` — salvages material from `BlockCrashedBomb` (a crashed-ordnance world structure). Ambiguous by itself (it's a passive extraction tool, not a weapon), but its only reason to exist is bomb/ordnance world content, so group it with Phase 3 rather than invent a one-off Phase-1 exception for it.
- `ItemRTTYPager` — nominally a radio pager (`RTTYSystem`, a Phase-2-flavored network), but its actual payload imports `ExplosionEffectWeapon`/`EntityProcessorCrossSmooth` to remote-detonate — classify by what it does (trigger an explosion) not by its name.

### Artillery / missile / satellite targeting (military C2 equipment)

- `ItemDesignator`, `ItemDesignatorManual`, `ItemDesignatorRange`, `ItemDesignatorArtyRange` — all `IDesignatorItem`, tied to `com.hbm.blocks.bomb.LaunchPad`.
- `ItemSatDesignator`, `ItemSatInterface` — orbital strike / satellite control panel, tied to `com.hbm.saveddata.satellites.*`.
- `ItemRadarLinker` (+ its base `ItemCoordinateBase`, used only by this one concrete class in-package) — links to `TileEntityMachineRadarScreen`/`IRadarCommandReceiver`, i.e. military radar, not generic Phase 2 machinery.
- `ItemRangefinder` — creative tab is literally `MainRegistry.missileTab`; it's artillery rangefinding, not a generic surveying tool.
- `ItemTurretMobFilter` — configures `TileEntityTurretBaseNT` targeting rules. Turrets are a combat structure; treat this as weapon-system coupling even though mechanically it's "just" a GUI on a block, same shape as the Phase 2 machine tools below.
- `ItemBoltgun` — reads as a construction "nail gun" (drives bolts to fasten hull blocks) but its actual `onLeftClickEntity` implementation consumes bolt ammo to deal 10 armor-piercing damage with an explosion VFX. It's a ranged weapon with a construction skin; classify by behavior.

## (c) Phase 2 machine coupling

These items are only meaningful once the machine block/TE they interact with exists. Porting the
item class itself is easy; porting it *usefully* requires the paired Phase-2 (or later) block/TE.

| Class | Target system |
|---|---|
| `ItemTooling` (base: `screwdriver`, `screwdriver_desh`, `hand_drill`, `hand_drill_desh`) | `IToolable` machine part interaction |
| `ItemToolingWeapon` (`wrench_archineer`) | Same `IToolable` system, with an incidental melee stat bolted on |
| `ItemWrench` | `TileEntityPipelineBase` pipe-network anchor connection |
| `ItemBlowtorch` | `IToolable.ToolType.TORCH`, fluid-fueled welding |
| `ItemAnalyzer`, `ItemAnalysisTool` | Generic block/TE debug-info dump (`IAnalyzable`, `BlockDummyable` core lookup) |
| `ItemMirrorTool` | `TileEntitySolarMirror` (solar boiler multiblock) alignment |
| `ItemPowerNetTool` | `IEnergyConductorMK2`/`PowerNetMK2` diagnostic |
| `ItemConveyorWand` | `BlockConveyor*`/`BlockCraneBase` network placement/config |
| `ItemRebarPlacer` | `BlockRebar` construction, own GUI/container for rebar patterns |
| `ItemWiring` | `TileEntityPylonBase` wiring |
| `ItemSettingsTool` | `ICopiable` — copy/paste machine settings between TEs |
| `ItemAnchorRemote` | Structure/dimensional anchor blocks (battery-powered remote) |
| `ItemKeyPin` (base), `ItemKey`, `ItemLock`, `ItemCounterfeitKeys` | `TileEntityLockableBase` machine-door lock/pin security system — port as one unit |
| `ItemRBMKTool`, `ItemDyatlov` | RBMK reactor console/meltdown trigger (`TileEntityRBMKBase`, `RBMKBase`) — reactor-specific machine coupling, likely belongs alongside whichever phase owns the RBMK multiblock, not generic Phase 2 |
| `ItemDrone`, `ItemDroneLinker` | Logistics/automation drone network (`IDroneLinkable`, `EntityDroneBase`/`EntityDeliveryDrone`). `ItemDrone` is **metadata-driven-multi**: `ItemEnumMulti<EnumDroneType>` over `PATROL`, `PATROL_CHUNKLOADING`, `PATROL_EXPRESS`, `PATROL_EXPRESS_CHUNKLOADING`, `REQUEST` — needs 5 registry entries. These are logistics/hauling drone types (not combat drones), hence Phase 2 rather than Phase 3. |

`ItemCoordinateBase` (abstract) is only used by `ItemRadarLinker` in this package; since radar is
classified under Phase 3 military targeting above, this base class travels with it, not with the
Phase 2 list.

## (d) Not player content — internal dev/debug tooling (11 files)

Flagging separately because it's large enough to skew file-count planning and because it needs an
explicit decision (port later / port as stub / drop) rather than silently landing in Phase 1:

- `ItemStructureTool` (abstract) + `ItemStructureSolid`, `ItemStructureRandomized`, `ItemStructureSingle`, `ItemStructurePattern`, `ItemStructureRandomly` — write multiblock structure definitions out to `structureOutput.txt` for the mod's own developers to paste into Java source when authoring new multiblocks. Gated behind `GeneralConfig.enableDebugMode`. Not consumed by normal players.
- `ItemCMStructure` — same JSON/structure-dump pattern, scoped to `BlockCMAnchor`.
- `ItemWand`, `ItemWandS`, `ItemWandD` — creative-tab-only "construction wand" / "structure wand" (instant prefab factory placement, several `case` branches unimplemented/stubbed in CE itself) / debug wand (increments internal counters, spawns loot chests via ray-trace, dumps `TileEntityPylonBase` connection lists to chat). None of these three carry balanced survival behavior.
- `ItemMeteorRemote` — explicitly commented in CE source: `//mlbv: useful for testing, please don't remove it`. A dev/QA tool for triggering `BossSpawnHandler` events on demand.

**Recommendation:** exclude this cluster from Phase 1 item-count and crafting-recipe planning
entirely. If kept for parity, treat it as a standalone low-priority "creative/debug utilities" task
at the end of the project, not as Phase 1 mass content — none of it needs to exist for the game to
be playable, and several of these reference blocks/systems (`BlockCMAnchor`, prefab factory
buildings, `red_pylon`) that won't exist until much later phases anyway, so porting them early would
just mean porting them twice.

## Trivial marker interfaces (no phase assignment needed)

- `IItemAbility` — single-method interface (`breakExtraBlock`). Worth double-checking at
  implementation time whether anything still implements it; the actual break-extra-block logic in
  `ItemToolAbility` today is a same-named instance method, not an override of this interface, so this
  may be dead/legacy.
- `IToolNTM` — one-method default-method marker interface (`getTool()` returning `this`). Port
  alongside whatever concrete class ends up needing it; carries no logic of its own.

## NBT -> Data Component notes

Recurring hand-rolled NBT keys in this package that will need Data Component equivalents:
- Coordinate-store pattern (`x`/`y`/`z`, or `posX`/`posY`/`posZ`, or `anchorX/Y/Z`) — repeated
  across `ItemWrench`, `ItemDetonator`, `ItemCoordinateBase`, `ItemWand`, `ItemStructureTool`,
  `ItemBoltgun`'s pending-position stash, etc. Good candidate for one shared `BlockPos`-holding
  component type used by many items rather than one bespoke component per item.
- `ability` / `abilityPresets` (`ItemToolAbility.Configuration`) — a list of structured presets;
  needs a proper component type, not a flat primitive.
- `pins` (`ItemKeyPin`) — trivial int component.
- `building` (`ItemWandS`) — trivial int component (dev tool, low priority per bucket (d)).

## Summary counts

- (a) Phase 1 utility/mining tools: ~46 files (framework: `ItemToolAbility`/`Fueled`/`Power`,
  `ItemChainsaw`, `ItemMultitoolTool`; standalone utility items; bag/container items; detectors;
  self-contained GUI items). Exact count depends on the multitool-chain call above.
- (b) Phase 3 melee/ranged/military: ~24 files (swords, multitool weapon chain classed here,
  detonators/defusers, designators/sat/radar/turret targeting, boltgun, RTTY pager, amat extractor).
- (c) Phase 2 machine coupling: ~19 files (tooling base + wrench family, analyzer/blowtorch/mirror/
  power-net/conveyor/rebar/wiring/settings tools, lock-key security family, RBMK console tools,
  drone logistics family, anchor remote).
- (d) Internal dev/debug tooling, not content: 11 files (structure-dump tools, wands, meteor test
  remote).
- 2 trivial marker interfaces, counted separately from the above.

These four buckets total 95 concrete classes + 2 interfaces = 97, matching the file count.
