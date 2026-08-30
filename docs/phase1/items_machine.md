# items/machine triage - Phase 1 vs Phase 2

Scope: `com.hbm.items.machine` in hbm-ce (1.12.2), 53 files (52 item classes + 1 interface).
All 53 files were read in full. This directory's name is misleading: despite living in a
package called "machine", the overwhelming majority of these classes are self-contained
data/NBT items with zero compile-time or functional dependency on any tile entity - the
mod's own convention here is "items carry state, TEs read/write it via static helpers",
which is exactly the kind of item that can and should be ported in Phase 1 as a standalone
item even though the machine that eventually *consumes* it is Phase 2 content.

Result: **43 port-now, 9 defer-to-Phase-2, 1 not-actually-an-item**.

## Legend
- **Port now** - compiles and is functionally complete (tooltips, NBT/durability math,
  right-click behavior that doesn't touch a TE) with zero Phase-2 machine content. The
  eventual machine will just start reading/writing the same static helpers once it exists.
- **Defer** - the class directly references a not-yet-ported TE class, block class, or
  machine-only capability interface, so it cannot function (and in most cases cannot even
  compile) until that Phase 2 content exists.

## Port now (43)

| Class | Metadata->flattening | One-line reason |
|---|---|---|
| ItemArcElectrode | 4 variants (GRAPHITE/LANTHANIUM/DESH/SATURNITE) | Pure NBT durability counter; arc furnace (Phase 2) only ever reads/writes via the static helpers already on this class. |
| ItemArcElectrodeBurnt | 4 variants (shares enum) | Inert byproduct item, zero logic beyond the shared enum. |
| ItemBattery | none (each of battery_potato/battery_potatos/fusion_core/energy_core/battery_schrabidium is its own instantiation, not a metadata variant) | Self-contained charge/discharge NBT + IBatteryItem impl; `@Deprecated` in CE (superseded by ItemBatteryPack) - keep porting it since ModItems still needs those exact registry names, but don't model new content after it. |
| ItemBatteryCreative | none | Trivial infinite-charge IBatteryItem stub, no state at all. |
| ItemBatteryPack | 12 variants (6 batteries + 6 capacitors) | Charge NBT + IBatteryItem, IMetaItemTesr is a render detail (TESR-on-item), not a TE dependency. |
| ItemBatterySC | 10 variants | Fixed-power radioisotope battery, no mutable state, no TE reference. |
| ItemBlades | none (per-instance, e.g. blade_titanium/blade_tungsten) | Plain durability tool item (extends ItemCustomLore); verify ItemCustomLore lands in whatever item-base sweep covers `items.special`. |
| ItemBlueprintFolder | 3 variants (base/discover/secret) | Right-click "loot box" that rolls `GenericRecipes.blueprintPools` - a data/recipe registry, not a TE; 1.12 IModel/IBakedModel baking code is dead weight to drop, not port. |
| ItemBlueprints | none - use a data component, not flattening | Stores a `pool` string in NBT; reads `GenericRecipes`/`GenericRecipe`, no TE coupling. NBT key `pool` -> data component. |
| ItemBreedingRod | 17 variants | Pure marker item, zero logic in this class; consumed by a not-yet-built breeder block but exists independently. |
| ItemCapacitor | none (identity-checked, effectively just `redcoil_capacitor`) | Self-contained lightning/explosion right-click behavior, no TE. |
| ItemCassette | **do not flatten - open/extensible registry, see note below** | Pure data item read by a (Phase 2) siren TE via `getType()`; item itself has no TE reference. |
| ItemCatalyst | none (per-instance) | Plain stat-carrier item (color/power/heat/fuel mods), no TE reference. |
| ItemChemicalDye | 16 variants, x2 base items (`chemical_dye`, `crayon`) | Client-side color tinting only, no TE. |
| ItemDrillbit | N variants per `ItemEnums.EnumDrillType` | Tooltip-only stat item; actual drill tool logic lives elsewhere (items.tool), not a machine TE. |
| ItemFELCrystal | none (per-instance, one class per wavelength) | Tooltip-only descriptor item for a laser machine that doesn't exist yet; item itself has no TE reference. |
| ItemFluidIcon | **do not flatten - see fluid-container note below** | Pure display/GUI helper item (fluid + fill + pressure via NBT), never touches a TE. |
| ItemFluidTank | **do not flatten - see fluid-container note below** | Fill/empty resolved through `FluidContainerRegistry` (a data registry), no TE reference in this class. |
| ItemFluidTankV2 | **do not flatten - see fluid-container note below** | Already uses a Forge fluid-handler item capability; maps naturally onto NeoForge's item fluid-handler capability, still no TE reference. |
| ItemFuelRod | none (base class, per-instance subclassing/instantiation) | Life-counter NBT base class; the classic reactor TE that eventually burns it is Phase 2, but the item stands alone. |
| ItemGear | 2 variants (bronze/steel, hardcoded index) | Plain crafting ingredient, IMetaItemTesr is a render detail only. |
| ItemICFPellet | none - two enum selections in NBT, use a data component | `type1`/`type2`/`muon` NBT triplet identifies a fusion fuel combo; all math (`react`, `getFusingDifficulty`, `getMaxDepletion`) is static over the ItemStack, no TE reference. |
| ItemLens | none (per-instance) | Damage-tracked NBT item for a laser machine that doesn't exist yet; no TE reference in file. |
| ItemMachineUpgrade | none (already one registry entry per upgrade) | Core "insert into any machine" upgrade chip; the `IUpgradeInfoProvider` tooltip lookup is a defensive, null-checked *optional* enhancement (falls back to static text), not a hard requirement - item is fully functional without any machine present. |
| ItemMold | ~20 shape entries | Selector item for a (Phase 2) foundry TE, but the item's own `getOutput()` resolution runs entirely against `Mats`/`MaterialShapes`/`OreDictionary` - exactly the material-shape system Phase 0 built for this purpose. Recommend keeping "moldId" as a single int (data component or the existing metadata-style index), independent from material - the foundry TE picks the material at insert-time. Drop the 1.12 IModel baking code. |
| ItemPACoil | 4 variants | Tooltip-only stat item for a particle accelerator that doesn't exist yet; no TE reference. |
| ItemPWRFuel | 15 variants | Tooltip/data item using a `Function` math helper; no TE reference in file. |
| ItemPileRod | none (base, per-instance) | Tooltip-only base class for the classic "pile" reactor; no TE reference. |
| ItemPileRodMK2 | 9 variants | Depletion-NBT item with a static `react()` helper; no TE reference. |
| ItemPistons | 4 variants | Tooltip-only efficiency-table item for a combustion-engine-type machine; no TE reference (only touches `FT_Combustible.FuelGrade`, a fluid trait). |
| ItemPlateFuel | none (extends ItemFuelRod, per-instance + `setFunction()`) | Adds reactivity math on top of ItemFuelRod; no TE reference. |
| ItemRBMKPellet | **do not flatten across depletion stage - see note below** | "Pellet for recycling" byproduct; tooltip/creative-tab logic only, no RBMK TE class referenced in this file. |
| ItemRTGPellet | none (per-instance) | Self-contained half-life/decay math entirely over ItemStack NBT; references `ModItems.pellet_rtg_depleted` (another plain item), never a TE. |
| ItemSatChip | none (per-instance: sat_foeq, sat_gerald, sat_laser, sat_mapper, sat_miner, sat_lunar_miner, sat_radar, sat_resonator, sat_scanner) | Tooltip-only descriptor item, `ISatChip` interface has no TE dependency here. |
| ItemSatellite | 14 variants | Same as ItemSatChip, just enum-driven instead of one-class-per-item. |
| ItemScraps | 1 per smeltable/additive material (driven by `Mats.orderedList`, could be 60-150+) | `extends ItemAutogen` - this is really a per-material auto-generated "shape" (like ingot/plate) rather than a distinct machine item; coordinate with whatever research area owns the material/shape item-generation pipeline rather than treating it as bespoke machine content. No TE reference. |
| ItemStamp | none (per-instance + durability) | Die item for a (Phase 2) press machine; only self-registers into a static `HashMap<StampType, List<ItemStack>>` lookup table for that future machine to query - no TE reference. |
| ItemStampBook | 8 variants | Subclass of ItemStamp covering the 8 "printing" stamp types; same reasoning. |
| ItemTurretBiometry | none - NBT name list -> data component (list of strings/UUIDs) | Self-contained "add my name to this chip" item; a future turret TE will *read* the list, but the item's write path needs no TE. |
| ItemTurretChip | none | Trivial subclass of ItemTurretBiometry that no-ops `onItemUse`; author left a `//FIXME...?` marking the split from its parent as unclear, but nothing here requires a TE either. |
| ItemWatzPellet | 12 variants x 2 base items (fresh `watz_pellet` / depleted) = 24 | Same shape as ItemPWRFuel/ItemRBMKRod: complex enrichment math, but entirely static-over-ItemStack, no TE reference. |
| ItemZirnoxRod | 11 variants | Life-counter NBT item, no TE reference. |
| ItemZirnoxRodDepleted | 9 variants | Inert byproduct marker item, no TE reference. |

## Defer to Phase 2 (9)

| Class | Blocking dependency |
|---|---|
| IItemFluidIdentifier | Interface itself is trivial (one method), but its only implementor in this package (ItemFluidIDMulti) is pipe-network content; porting it in isolation buys nothing until the fluid pipe network (Phase 2) exists. |
| ItemFFFluidDuct | `onItemUse` places `ModBlocks.fluid_duct_neo` and casts its TE to `TileEntityPipeBaseNT` to set fluid type - is literally a placer for a not-yet-ported pipe-network block. |
| ItemFluidIDMulti | GUI (`GUIScreenFluid`) and flood-fill (`spreadType`) operate directly against `TileEntityPipeBaseNT` pipe network tiles. |
| ItemFluidSiphon | `onItemUse` requires a `IFluidStandardReceiverMK2` machine TE to drain from; does nothing without one. |
| ItemMuffler | `onItemUse` flips `TileEntityLoadedBase.muffled` on whatever machine TE it's used on - needs that TE base (and a concrete machine built on it) to exist. |
| ItemPWRPrinter | Flood-fills `BlockPWR`/`TileEntityPWRController`/`TileEntityBlockPWR` to print a multiblock construction diagram - entirely PWR-reactor-multiblock content. |
| ItemRBMKLid | Directly mutates `RBMKBase` block state and `TileEntityRBMKBase` NBT to install a lid on an RBMK column - fully RBMK-system content. |
| ItemReactorSensor | `onItemUse` only does anything when clicked on `ModBlocks.reactor_research`, a specific not-yet-ported machine block. |
| ItemRBMKRod | **Partial/flagged case** - all of the item's own physics (`burn`, `updateHeat`, `provideHeat`, xenon/depletion math) is pure NBT-over-ItemStack with zero TE references. However the file imports `RBMKDials` and `IRBMKFluxReceiver.NType` from `com.hbm.tileentity.machine.rbmk` (the RBMK package), so it cannot compile until at least that config/type surface is ported. Two real options: (a) defer the whole class alongside RBMK (simplest, matches the package-level dependency), or (b) split the pure-data parts (enums, NBT getters/setters, tooltip) into a Phase-1-portable core and defer only the two dial-reading methods (`burn`, `updateHeat`) to a Phase-2 extension. Given it's one class and the split adds complexity for a single file, recommend (a): defer with RBMK, but call out that the "hard" part is genuinely reusable once RBMK starts. |

## Not actually an item (1)

| Class | Note |
|---|---|
| ItemDrive | Not an `Item` subclass at all - just a bare static nested enum `EnumDriveType` (13 values: FLASH_EMPTY, DISK_EMPTY, ... KLAUS) with no fields, no registration call, and no other members. It exists purely as a shared enum namespace for whatever real item class (elsewhere, likely `items.special`) implements the actual satellite/data-drive item. Porting the enum itself is trivial and has no Phase 2 coupling - just don't count it as a 43rd/44th standalone item; find and check its real consumer when that package is triaged. |

## Cross-cutting design flags (apply the "no metadata after flattening" rule with judgment, not blindly)

1. **Fluid-backed items should NOT be flattened per-fluid.** `ItemFluidTank`, `ItemFluidTankV2`,
   and `ItemFluidIcon` are all `metadata = FluidType.getID()`. The CE fluid registry (Phase 0's
   `Fluids`/`FluidType`) can easily have 100+ entries, and it keeps growing - literally
   flattening these into "one registry item per fluid x per tank size" would produce hundreds
   of items that only exist to carry `(fluidId, amount[, pressure])`. This is precisely the
   shape Data Components were designed for: recommend a single `fluid_tank_<size>` /
   `fluid_icon` item per container variant, with the fluid type + amount (+ pressure for
   `fluid_icon`) stored as a data component referencing the fluid registry by id, mirroring
   `ItemFluidTankV2`'s existing Forge fluid-handler-item capability (which NeoForge's
   `IFluidHandlerItem` capability maps onto directly). This is a deliberate exception to the
   "expand metadata into N registry items" rule from the ground rules, justified because the
   variant dimension here is an open, Phase-0-owned data registry rather than a small fixed
   CE-authored enum.
2. **ItemCassette's `TrackType` is an open, dynamically-registered pseudo-registry**
   (`TrackType.register(...)` lets other code add tracks at runtime, with an `AtomicInteger`
   handing out ids), not a fixed CE enum. It cannot be flattened into a fixed set of registry
   items at all. Recommend a single `cassette` item with a data component holding a track
   reference (ideally the id of an actual NeoForge registry entry if the siren-track table
   itself gets promoted to a registry in the port, otherwise a string key into a runtime map
   as CE already does).
3. **ItemRBMKPellet mixes two independent dimensions in one metadata value**: which RBMK fuel
   type the pellet came from (a handful of `ItemRBMKPellet` instances, one per `fullName`) and
   a 0-9 depletion/xenon stage (`meta % 5` for stage, `meta >= 5` for xenon flag) layered on
   top per instance. Flattening the depletion dimension into more registry items multiplies
   badly for no benefit (it's runtime-computed decay, not a fixed craftable variant) -
   recommend keeping depletion/xenon as a data component (or just a component-stored byte) on
   one item per fuel-type, and only flattening the fuel-type dimension into distinct registry
   entries.
4. **ItemScraps and ItemMold are really part of the material/shape item-generation pipeline**,
   not bespoke "machine" content - `ItemScraps` enumerates every smeltable/additive
   `NTMMaterial`, and `ItemMold`'s per-material resolution runs through `MaterialShapes`/
   `Mats`/`OreDictionary` exactly like the ingot/plate/nugget items Phase 0 built the
   `buildRegistryName()` convention for. Recommend coordinating their final item shape with
   whichever Phase 1 research area owns the generic per-material item registration, rather
   than hand-rolling a one-off design here.
5. **NBT keys seen in this package that need Data Component migration**: `charge` (batteries),
   `dura`/`durability` (capacitor, arc electrode), `life`/`depletion` (fuel rods, pile rods,
   Zirnox rods, ICF pellets - key name `depletion` is reused across at least 3 unrelated
   classes with 3 different meanings, keep them as separate component types), `yield`/`xenon`/
   `core`/`hull` (RBMK rod), `pool` (blueprints), `x`/`y`/`z` (reactor sensor position - this
   one is a natural `BlockPos`-typed component), `playercount`/`player_N` (turret biometry -
   natural fit for a list-typed component), `fluid1`/`fluid2` (fluid identifier), `fill`/
   `pressure` (fluid icon), `type1`/`type2`/`muon` (ICF pellet), `PELLET_DEPLETION` (RTG
   pellet).

## Summary counts
- Port now (Phase-1-safe standalone items): **43**
- Defer to Phase 2 (hard TE/block/multiblock coupling): **9**
- Not an item (bare enum, verify real owner elsewhere): **1**
- Total files surveyed: **53** (52 item classes + `IItemFluidIdentifier`)
