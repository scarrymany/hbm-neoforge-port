# Phase 1 triage: items/block (4), blocks/fluid (14), blocks/gas (10)

Scope: `com.hbm.items.block.*`, `com.hbm.blocks.fluid.*`, `com.hbm.blocks.gas.*` in hbm-ce. All 28 files
read in full. None of these packages contain metadata-driven-multi items in the "one class covers N
materials via damage value" sense that Phase 1's material/shape expansion work targets - every class here
is a single, unique block/item behavior class. The triage question for this area is not "how many
variants does this expand into" but "does the plumbing this class needs already exist in the port".

Verified against the port project (`C:\Users\Sergo127\Desktop\hbms`) by grepping for the actual dependency
class/interface names, not assumed:

| CE dependency | Exists in port? |
|---|---|
| `IBlockMulti`, `IPersistentInfoProvider`, `ITooltipProvider`, `IBlockSpecialPlacementAABB` | Yes (Phase 0 API/interfaces) |
| `ModDamageSource` (CE) -> `ModDamageTypes` (port) | Yes, data-driven `DamageType` registry, includes `ACID`, `RADIATION`, `MUD_POISONING`, `MONOXIDE`, `ASBESTOS` |
| `GeneralConfig`, `ArmorRegistry` | Yes |
| `com.hbm.inventory.fluid.*` (FluidType/Fluids/traits - HBM's own tank/machine abstraction) | Yes (Phase 0) |
| Real Minecraft/NeoForge world-fluid framework (`Fluid`/`FluidType`(NeoForge)/`BaseFlowingFluid`/`LiquidBlock` equivalents, i.e. a `com.hbm.fluids` package) | **No** - does not exist in the port at all |
| `BlockMetalFence`, `IPersistentNBT`, `BlockStorageCrate`, `CustomMachineConfigJSON`, `TileEntityCrate`, `IGUIProvider` | **No** |
| `ContaminationUtil`, `ChunkRadiationManager`, `ArmorUtil` (handler), `HbmPotion`, `AdvancementManager` | **No** - referenced by name in already-written Phase 0 hazard code but the classes themselves are not yet written anywhere in the port |

The Neo Edition reference (hbmsntm) is a useful cross-check here: it has already 1:1-ported all 10
`blocks/gas` classes (`com.hbm.blocks.gas.Gas*Block`) and has a working `com.hbm.fluids` package
(`NtmFluids`, `NtmFluidTypes`) plus 2 of the 14 fluid block classes (`VolcanicLiquidBlock`,
`RadLiquidBlock`), confirming real, working 1.21 API shapes for both. It has nothing under
`items/block` equivalent to CE's `ItemBlockStorageCrate`/`ItemCustomMachine` because its crate/custom-machine
systems are structured differently; that difference doesn't change what this port needs.

## items/block (4 files) - ALL DEFER

None of these are simple items. All four are `ItemBlock`/`BlockItem` subclasses whose entire reason to
exist is a companion block/GUI/config system that has not been ported yet. There is no version of any of
them that is "Phase-1-safe now, refine later" - without their dependency, the class does not compile.

- **ItemBlockBase.java** - DEFER. Blocked on: `IBlockMulti`/`BlockMetalFence` consumers, and
  `IPersistentNBT` (stack-carries-a-tile-entity's-contents marker key, `NBT_PERSISTENT_KEY`). The
  `IPersistentNBT` NBT key needs to become a Data Component when this is picked up - note for whoever
  owns it later, not a Phase 1 item. `IBlockMulti`/`IPersistentInfoProvider`/`ITooltipProvider` already
  exist in the port (Phase 0), but this class is the generic "any placed block wants special item
  behavior" wrapper and has no single caller to justify porting standalone; port it alongside the first
  multi-block or persistent-NBT block that needs it.
- **ItemBlockSpecialAABB.java** - DEFER. Blocked on: any block implementing
  `IBlockSpecialPlacementAABB` (the interface exists in the port, but zero blocks implement it yet since
  no block content has been ported). Also worth flagging for its eventual owner: `onItemUse` reimplements
  vanilla block-placement logic by hand (mirroring 1.12.2's `ItemBlock.onItemUse`) rather than delegating
  to `BlockItem.useOn`; in 1.21 NeoForge, check whether `BlockItem.getPlacementState`/`place` extension
  points cover this instead of a full manual reimplementation - genuinely ambiguous without the concrete
  block in hand, so decide when porting the first AABB-placement block, not now.
- **ItemBlockStorageCrate.java** - DEFER. Blocked on: `BlockStorageCrate`, `TileEntityCrate`,
  `HandHeldTileEntityCrate`, `IGUIProvider`, `IPersistentNBT`, `ServerConfig.CRATE_OPEN_HELD` - a whole
  "hand-held crate with a dummy tile entity for its GUI" subsystem, none of which exists. Also uses
  `FMLNetworkHandler.openGui` (Forge 1.12 menu-opening API) which has no NeoForge 1.21 equivalent at all
  (`MenuProvider`/`ItemStack`-opened menus work differently) - flag for whoever ports this that the GUI-open
  call site needs a full rewrite, not a mechanical translation.
- **ItemCustomMachine.java** - DEFER. Blocked on: `CustomMachineConfigJSON` (a JSON-datapack-defined
  machine registry). Note for its future owner: this is a genuine metadata-driven-multi item in spirit -
  `getSubItems` enumerates `CustomMachineConfigJSON.niceList` and creates one `ItemStack` per config entry
  via `new ItemStack(this, 1, i + 100)` (damage value offset by 100 selects which JSON-defined machine).
  Post-flattening this cannot be one registry item with N damage variants; it needs either (a) N real
  registry entries generated from the JSON config at datagen/registration time (mirrors the
  material/shape item-generation approach Phase 1 already uses elsewhere), or (b) one item + a Data
  Component holding the machine config's resource id, with creative-tab population using a
  `CreativeModeTab.Builder` populate callback instead of `getSubItems`. Since the config is user-supplied
  JSON read at runtime, this likely needs an id-in-a-component design rather than static datagen; call
  this out explicitly to whoever picks up the custom-machine system.

## blocks/fluid (14 files) - ALL DEFER, single blocker: no world-fluid framework

Every one of these 14 files extends `net.minecraftforge.fluids.BlockFluidClassic` or `BlockFluidFinite`,
or supports fluid blocks that do. That base class does not exist in NeoForge 1.21 at all - 1.21's fluid
system is a different shape entirely (`Fluid`/`FlowingFluid`, a separate `net.neoforged.neoforge.fluids.FluidType`
for render/interaction properties, `LiquidBlock`, and registration of a still+flowing pair per fluid).
None of that framework exists yet in the port (`com.hbm.inventory.fluid.*` is a different, HBM-internal
tank/machine abstraction from Phase 0 and does not provide any of it). This is a single shared blocker
for the whole package, not per-file:

**Blocker: a `com.hbm.fluids` (or similarly-named) world-fluid registration package - source/flowing
`Fluid` pair registration, a NeoForge `FluidType` per fluid (for fog/color/still-flowing textures), and a
`LiquidBlock`-based block base class - needs to exist before any of these 14 files can be ported.** The
Neo Edition reference confirms the target shape (`com.hbm.fluids.NtmFluids` / `NtmFluidTypes`,
`blocks.fluids.VolcanicLiquidBlock`/`RadLiquidBlock`) and is a reasonable model to follow, though it only
covers 2 of CE's 8 world fluids so far.

Per-file detail (all DEFER on the above blocker; noting additional secondary blockers and porting
concerns for whoever picks this up):

- **ModFluids.java** - registers 8 fluids (`liquid_concrete`, `toxic_fluid`, `mud_fluid`, `acid_fluid`,
  `schrabidic_fluid`, `corium_fluid`, `volcanic_lava_fluid`, `rad_lava_fluid`) plus a 9th,
  `sulfuric_acid_fluid`, that is not fed to a dedicated block subclass (uses generic `GenericFluidBlock`).
  This is the natural place to enumerate the DeferredRegister entries once the framework exists.
- **FluidNTM.java** - thin `Fluid` subclass carrying an optional link back to `com.hbm.inventory.fluid.FluidType`
  for localization. Once the port has real NeoForge `Fluid`/`FluidType` registration, this becomes either
  redundant or a thin adapter; note for its owner that the "does this fluid also have an inventory
  FluidType" linkage needs a replacement now that `com.hbm.inventory.fluid.FluidType` already exists in
  the port (Phase 0) with its own identity.
- **IFluidFog.java** - trivial 2-method interface (`getFogDensity`, `getFogColor`), no NeoForge-specific
  API surface; ports mechanically once its implementors do.
- **FluidFogHandler.java** - a Forge event-bus listener on `EntityViewRenderEvent.FogDensity`/`FogColors`
  reading `ActiveRenderInfo.getBlockStateAtEntityViewpoint`. NeoForge 1.21 has renamed/restructured fog
  events (`ViewportEvent.RenderFog`/`ComputeFogColor` under `net.neoforged.neoforge.client.event`, and
  `ActiveRenderInfo` is now `Camera`) - verify the exact event names against NeoForge 21.1 when this is
  picked up; flagging as an API-shape unknown rather than guessing.
- **GenericFluidBlock.java** - generic damage-on-contact fluid block (used for `sulfuric_acid_block`).
  Straightforward once the base class exists; `AdvancementManager` (grants an achievement when a slime
  ball is destroyed in the fluid) does not exist in the port yet - secondary blocker, but small.
- **AcidBlock.java**, **MudBlock.java**, **ToxicBlock.java**, **SchrabidicBlock.java** - four near-identical
  `BlockFluidClassic` subclasses (contact damage/contamination + neighbor-reaction-to-air/other-blocks
  logic). All reference `ContaminationUtil` (Toxic/Schrabidic) or `ModDamageSource`->`ModDamageTypes`
  (Acid/Mud, already available) and `ArmorUtil` (Mud). `ContaminationUtil` does not exist in the port -
  secondary blocker shared with the gas package (see below).
- **VolcanicBlock.java**, **RadBlock.java** (extends VolcanicBlock), **CoriumBlock.java**,
  **CoriumFinite.java** (finite variant of Corium) - the "molten rock that solidifies into ore variants"
  family. Depends on many not-yet-ported sibling blocks (`ModBlocks.basalt`, `basalt_ore`,
  `sellafield_slaked`, `ore_sellafield_*`, `waste_log`, `waste_planks`) and `ForgeDirection` (CE's own
  compat enum for 6 directions - trivially replaced by vanilla `Direction` per the Neo reference's
  `RadLiquidBlock`/`VolcanicLiquidBlock`). These are Phase 2/3-scale ore/terrain content, not Phase 1 -
  defer for both the fluid-framework blocker and the missing sibling blocks.
- **GenericFiniteFluid.java** - generic finite-fluid base (used nowhere in this file set for a concrete
  instance beyond Corium, but is a reusable base). Blocked on the framework only.

## blocks/gas (10 files) - shape is Phase-1-simple; DEFER on a small, well-defined secondary-system blocker

Structurally this package is exactly what Phase 1 is for: 10 small, single-purpose, invisible/non-solid
"gas cell" blocks, each a concrete leaf under one abstract base (`BlockGasBase`), no metadata variants, no
BlockItem at all (confirmed by grep: none of the 10 CE `ModBlocks` fields for these blocks - `gas_radon`,
`gas_radon_dense`, `gas_radon_tomb`, `gas_meltdown`, `gas_monoxide`, `gas_asbestos`, `gas_coal`,
`gas_flammable`, `gas_explosive` - register an `ItemBlock`; they are world-only, spawned by machinery, and
never held as items). The Neo Edition reference has already ported this exact package 1:1
(`GasBaseBlock` + 9 leaf subclasses) using plain vanilla `Block`/`Block.Properties` and `BlockState`
scheduled ticks (`level.scheduleTick`) in place of CE's `updateTick`/`ForgeDirection` movement logic - a
confirmed, working, low-risk API shape with no fluid-framework dependency at all (no `Fluid`/`FluidType`
involved anywhere in this package; "gas" here is just an invisible, physics-only solid `Block`, unrelated
to the fluid system in `blocks/fluid`).

The blocker is not the block class shape - it's that most of these blocks' *behavior bodies* call into
hazard/radiation/contamination/potion systems that do not exist in the port yet:

- **BlockGasBase.java** (abstract) - Phase-1-safe shape-wise. Uses `ForgeDirection` (trivial ->
  `Direction`), `ArmorUtil`/`ModItems.ashglasses`/`MainRegistry.proxy.effectNT` for a clientside particle
  effect gated on wearing a specific armor item - `ArmorUtil` (handler, distinct from the port's existing
  `util.ArmorRegistry`) does not exist yet. Minor/cosmetic feature; could ship without it and backfill.
- **BlockGasAsbestos.java** - DEFER. Calls `ContaminationUtil.applyAsbestos` (does not exist).
- **BlockGasCoal.java** - DEFER. Calls `ContaminationUtil.applyCoal` (does not exist).
- **BlockGasMonoxide.java** - DEFER. Calls `ArmorRegistry.hasAllProtection` (exists) then either
  `ArmorUtil.damageGasMaskFilter` (does not exist) or `entityLiving.attackEntityFrom(ModDamageSource.monoxide, 1)`
  -> straightforward `ModDamageTypes.MONOXIDE` (exists) on the damage side; only `ArmorUtil` is missing.
- **BlockGasRadon.java**, **BlockGasRadonDense.java** - DEFER. Both call `ArmorUtil.damageGasMaskFilter`
  and `ContaminationUtil.contaminate` (neither exists); RadonDense additionally references
  `ModBlocks.waste_earth`/`ModBlocks.fallout` (not yet ported terrain-hazard blocks - secondary, small).
- **BlockGasRadonTomb.java** - DEFER. Same `ArmorUtil`/`ContaminationUtil` blocker, plus
  `HbmPotion.radaway`/`HbmPotion.radx` (potion registry, does not exist) and `ModBlocks.waste_earth`.
- **BlockGasMeltdown.java** - DEFER. Calls `ContaminationUtil.contaminate`, `HbmPotion.radiation`,
  `ArmorRegistry`+`ArmorUtil`, and `HbmLivingProps.incrementAsbestos` (capability helper - check whether
  the port's Phase 0 `capability.HbmLivingAttachment`/`ContaminationEffect` already covers this method
  under a new name; worth a quick look for whoever picks this up rather than assuming it's fully missing).
  Also references `ModBlocks.gas_radon_dense` (in-package, fine once both exist).
- **BlockGasExplosive.java** (extends BlockGasFlammable) - **Phase-1-safe now.** Only dependency beyond
  vanilla is `GeneralConfig.enableExplosiveGas` (exists) and `BlockGasFlammable` (see next). No
  hazard/contamination/potion calls at all - pure vanilla fire-propagation + explosion logic
  (`world.newExplosion`, flood-fill via `ArrayDeque`/`HashSet` over neighbor `BlockGasExplosive` cells).
  Genuinely portable in Phase 1 once `BlockGasFlammable` and `BlockGasBase` land.
- **BlockGasFlammable.java** - **Phase-1-safe now.** Only dependency beyond vanilla is
  `GeneralConfig.enableFlammableGas` (exists). Pure vanilla fire-source detection and combustion
  propagation, no hazard/contamination system calls. Genuinely portable in Phase 1.

**Recommendation for this package**: port `BlockGasBase`, `BlockGasFlammable`, and `BlockGasExplosive`
in Phase 1 proper (they have zero missing dependencies beyond what Phase 0 already built) using the Neo
Edition reference's `GasBaseBlock` shape as the confirmed 1.21 pattern (vanilla `Block`, `scheduleTick`
instead of always-on `updateTick`, `RenderShape.INVISIBLE`, empty `VoxelShape`). Defer the other 7 leaf
classes as a group behind one real blocker: a contamination/radiation-application utility
(`ContaminationUtil` equivalent) and an armor-hazard-interaction helper (`ArmorUtil` equivalent) - both
of which are also the exact blocker for `blocks/fluid`'s Acid/Mud/Toxic/Schrabidic/Volcanic/Rad/Corium
classes, so whichever Phase 1 area builds that utility unblocks both packages at once. `HbmPotion` (radon
tomb, meltdown) is a smaller, separate blocker only those two leaf classes need.

## Cross-cutting notes for the eventual port

- **No metadata-driven-multi items anywhere in this triage area.** Every class surveyed is single/unique;
  the flattening concern from the task brief does not apply here except for `ItemCustomMachine` (see
  above), which is metadata-multi over a *runtime JSON config*, not a fixed material list - a materially
  different problem from the material/shape expansion Phase 1 is set up for.
- **No ItemStack NBT needing Data Component treatment in blocks/fluid or blocks/gas** - neither package
  stores anything in item NBT (they have no items at all). The two NBT keys worth flagging for whoever
  eventually owns `items/block` are `IPersistentNBT.NBT_PERSISTENT_KEY` (crate/multi-block stack contents,
  `ItemBlockBase`/`ItemBlockStorageCrate`) and the implicit "damage value selects JSON machine config" in
  `ItemCustomMachine`, both noted above.
- **Two independent, non-overlapping blockers gate this whole triage area**: (1) a real NeoForge
  world-fluid framework (blocks 12 of 14 fluid files outright, all of `blocks/fluid`), and (2) a
  contamination/armor-hazard utility layer (blocks 7 of 10 gas files and 6 of 14 fluid files). Neither
  blocker is specific to this area - both are shared prerequisites other Phase 1/2 areas will also need,
  so whoever schedules Phase 1 work should sequence one area to build each before the dependent areas
  (including this one) are attempted.
- **items/block's 4 files are blocked on three separate, unrelated systems** (multi-block/AABB-placement,
  crate+GUI+tile-entity, custom-machine JSON+creative-tab), each Phase 2+-scale; there is no shared fix
  that unblocks more than one of them at a time, unlike the fluid/gas packages above.
