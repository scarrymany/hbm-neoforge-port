# Oil production chain (derrick, pumpjack, refinery, fracking) — Phase 2 research

Sources:
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/oil/{TileEntityOilDrillBase,
  TileEntityMachineOilWell,TileEntityMachinePumpjack,TileEntityMachineFrackingTower,
  TileEntityMachineRefinery}.java` (read in full)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/{MachineOilWell,MachinePumpjack,
  MachineFrackingTower,MachineRefinery}.java` (read in full)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/RefineryRecipes.java` (read in full)
- `upstream/hbm-ce/src/main/java/com/hbm/world/feature/{OilSpot,BedrockOilDeposit}.java` (read in
  full — world-gen boundary check only, this area is Phase 4)
- `upstream/hbm-ce/src/main/java/com/hbm/world/{OilBubble,OilSandBubble}.java` (located, not read —
  named for the record, out of this task's boundary, see Deferred scope)
- This port's `src/main/java/com/hbm/blocks/BlockDummyable.java`, `src/main/java/com/hbm/blocks/
  generic/BlockSupplyCrate.java` (existing `extends BlockEntity` example), `src/main/java/com/hbm/
  api/energymk2/*.java`, `src/main/java/com/hbm/inventory/fluid/{Fluids,FluidType,FluidStack}.java`,
  `src/main/java/com/hbm/blocks/{OreBlocks,generic/PlantBlocks,generic/GenericBlocks}.java`,
  `src/main/java/com/hbm/items/machine/ItemMachineUpgrade.java` — grepped/read to establish exactly
  what already exists in the port vs. what this area still needs
- `docs/phase0/STATUS.md`, `docs/phase1/items_tool.md` (structural model), `docs/phase1/STATUS.md`
- **This area's three sibling Phase 2 prerequisite reports, read in full and treated as
  authoritative for their own scope**: `docs/phase2/multiblock_framework.md`,
  `docs/phase2/blockentity_base.md`, `docs/phase2/gui_framework.md`. This report does not re-derive
  any of their decisions — it documents what the oil chain specifically needs from each and flags
  one real conflict between two of them (see Open questions / risks).
- `upstream/neo-edition/src/main/java/com/hbm/blockentity/machine/oil/{OilDrillBaseBlockEntity,
  MachineOilWellBlockEntity}.java`, `.../blocks/machine/MachineOilWellBlock.java`,
  `.../inventory/menus/MachineRefineryMenu.java` — cross-checked for confirmed NeoForge 1.21.1 API
  shape only, never for behavior (per ground rules; Neo Edition is incomplete/sometimes-wrong)

## Headline finding

The task's own framing calls this area "derrick, pumpjack, refinery, fracking" as if the derrick
were a simple, non-multiblock extractor. **It is not.** All four blocks
(`MachineOilWell` — CE's internal config name for it is literally `"derrick"`, `MachinePumpjack`,
`MachineFrackingTower`, `MachineRefinery`) extend `BlockDummyable` and are built with
`MultiblockHandlerXR.checkSpace`/`fillSpace`, exactly like the Refinery. There is no "simple,
single-block derrick" shortcut available — this entire area is 100% gated on
`docs/phase2/multiblock_framework.md` landing first, not just the Refinery half of it.

The four machines form a clean two-tier hierarchy in CE, which carries over directly:
`TileEntityOilDrillBase` (abstract, 334 lines) is the shared extraction engine for the three
extractors; `TileEntityMachineOilWell` (201 lines, "derrick"), `TileEntityMachinePumpjack` (239
lines), and `TileEntityMachineFrackingTower` (246 lines) each override a handful of tuning
constants and hooks. `TileEntityMachineRefinery` (469 lines) is unrelated code — a standalone
multiblock with its own tank/power/recipe/explosion state machine — grouped with the other three
only because it consumes their output fluids downstream.

## Phase-2-safe scope

**Exact CE inventory for this area: 5 TileEntity files, 4 Block files, 1 recipe-data file, 2
Container files, 2 GUI files = 14 CE files.** (JEI's `RefineryRecipeHandler` and the world-gen
files are named in Deferred scope, not counted here.)

| CE class | Lines | Role |
|---|---|---|
| `TileEntityOilDrillBase` (abstract) | 334 | Shared engine: 2-tank (`OIL`,`GAS`) `FluidTankNTM[]`, `IEnergyReceiverMK2` power buffer, `UpgradeManagerNT` (SPEED/POWER/OVERDRIVE/AFTERBURN, 3 slots), the drill-down/suck-oil tick loop, `DirPos[]`-based fluid-push connections |
| `TileEntityMachineOilWell` | 201 | "Derrick" (`getConfigName()` returns literally `"derrick"`). Cheapest/simplest concrete extractor: 4 cardinal `DirPos` (no rotation), no proxy dummy TE variant, `getDimensions()` = `{9,0,1,1,1,1}` |
| `TileEntityMachinePumpjack` | 239 | Adds `ForgeDirection` rotation math to `getConPos()` (facing-dependent connector points), a purely cosmetic client-side rotating-rod animation (`rot`/`prevRot`/`speed`), `getDimensions()` = `{3,0,0,0,0,6}` |
| `TileEntityMachineFrackingTower` | 246 | Adds a 3rd tank (`FRACKSOL` — fracking solution, consumed as an input), `getDrillDepth()` overridden to `0` (drills through bedrock too), can additionally suck `ore_bedrock_oil`, and every successful suck calls `OilSpot.generateOilSpot(...)` as a gameplay side-effect (see below). By far the largest/most error-prone `checkRequirement`/`fillSpace` footprint of the four (7 distinct offset shapes) |
| `TileEntityMachineRefinery` | 469 | Independent multiblock: 5-tank chain (`HOTOIL` in → `HEAVYOIL`/`NAPHTHA`/`LIGHTOIL`/`PETROLEUM` out + sulfur item byproduct), `IOverpressurable`/`IRepairable` explosion-and-repair state machine, a looped audio effect tied to `isOn`, and a **self-correcting placement quirk**: if its block metadata is still in the 0-11 "dummy" range on first tick (i.e. it was loaded from a save/placed before its rotation was finalized), it removes and replaces itself as the core (meta 12-15) and re-fills the multiblock footprint — this is a real CE behavior, not dead code, and must not be "simplified away" when ported |
| `MachineOilWell`, `MachinePumpjack`, `MachineFrackingTower`, `MachineRefinery` (blocks) | — | All `extends BlockDummyable`, all use `MultiblockHandlerXR.checkSpace`/`fillSpace`, all delegate `onBlockHarvested`/`breakBlock` to `IPersistentNBT`, all implement `IPersistentInfoProvider.addInformation` (renders saved tank/power state on the item tooltip while off-world) |
| `RefineryRecipes` | 137 | Bespoke recipe data (see Key design decisions — this is not `RecipesCommon`/`GenericRecipe`) |
| `ContainerMachineOilWell` / `GUIMachineOilWell` | — | **One GUI/Container pair shared by all three extractors** (derrick, pumpjack, fracking tower each call `new ContainerMachineOilWell(...)`/`new GUIMachineOilWell(...)` from their own TE) |
| `ContainerMachineRefinery` / `GUIMachineRefinery` | — | Refinery's own GUI: battery slot + 5 input/output tank slot-pairs + sulfur output + fluid-ID slot (13-slot inventory) |

**What is genuinely unblocked once the shared prerequisites below land** (no oil-specific blocker
beyond them):
- The tank/energy/upgrade wiring is the same shape every other Phase 2 machine needs — no
  oil-specific novelty. `com.hbm.api.energymk2.IEnergyReceiverMK2` (the interface all 4 TEs
  implement) is already present in the port unchanged, confirming HE-energy stays a distinct
  capability per ground rules.
- `com.hbm.inventory.fluid.Fluids` already declares **every fluid this area touches**, already
  ported in Phase 0: `OIL`, `GAS`, `FRACKSOL`, `HOTOIL`, `HEAVYOIL`, `NAPHTHA`, `LIGHTOIL`,
  `PETROLEUM`, plus every `RefineryRecipes` output (`HOTCRACKOIL`, `HOTOIL_DS`, `HOTCRACKOIL_DS`,
  `NAPHTHA_CRACK`/`_DS`, `LIGHTOIL_CRACK`/`_DS`, `AROMATICS`, `UNSATURATEDS`,
  `HEAVYOIL_VACUUM`/`REFORMATE`/`LIGHTOIL_VACUUM`/`SOURGAS`/`REFORMGAS`). No fluid-type work needed.
- `ModBlocks.ore_oil` and `ModBlocks.oil_pipe` (the block the drill loop replaces stone with as it
  digs down) are **already registered** in this port (`OreBlocks.java` line 122, `GenericBlocks.java`
  line 413). `dirt_oily`/`dirt_dead` (used by fracking's `OilSpot.generateOilSpot` surface
  decoration) are also already registered (`PlantBlocks.java`).
- Fracking's actual block-mutation mechanic (`OilSpot.generateOilSpot`, called from the fracking
  tower's `onSuck`) is **plain per-block `world.setBlockState` calls in an ordinary Java loop** —
  no chunk/section-level API, no `ChunkGenerator`/heightmap manipulation of any kind. It scans
  `count` randomly-offset columns within Gaussian `width` of a center point and, for up to 4 blocks
  below the surface, swaps grass/dirt/sand/stone/leaves for oil-stained decorative variants and
  kills nearby plants. In NeoForge 1.21.1 this is a 1:1 rename to `Level#setBlock(BlockPos,
  BlockState, int flags)` inside the same loop shape — confirmed by reading the CE source directly,
  there is nothing chunk/section-API-shaped to redesign here. This mechanic's *code shape* is
  Phase-2-safe; what is *not* in scope (see Deferred scope) is the natural world-gen placement of
  the oil ore deposits this mechanic is triggered from finding.

## Deferred scope

**Everything in this list blocks all four machines equally — this is not "the Refinery is hard,
the others are easy."**

1. **`com.hbm.handler.MultiblockHandlerXR` / the multiblock framework** (owned by
   `docs/phase2/multiblock_framework.md`, not re-derived here). Confirmed by direct source reading:
   100% of this area's 4 blocks extend `BlockDummyable` and call `MultiblockHandlerXR.checkSpace`/
   `fillSpace` in their `checkRequirement`/`fillSpace` overrides — including the "derrick", which
   this task's own framing might suggest is a simple standalone block. It is not; its dimensions
   are `{9,0,1,1,1,1}` plus 5 extra `checkSpace`/`fillSpace` shapes for the corner support beams.
   Nothing in this area can be placed, broken, or rendered until that package lands.
2. **The shared machine `BlockEntity` base class** (owned by `docs/phase2/blockentity_base.md`, not
   re-derived here). All 5 TE classes in this area need essentially everything that report
   scopes: NBT round-trip via `saveAdditional`/`loadAdditional`, the sync/`networkPackNT` throttled
   packet pattern, `ItemStackHandler`-based inventory, and capability exposure for
   `IEnergyReceiverMK2`/fluid transceiver. Concretely missing from the port today (grepped, zero
   hits outside CE/neo-edition upstream): `TileEntityMachineBase`(-equivalent),
   `TileEntityProxyCombo` (the two extractor multiblocks and the refinery all use this for their
   non-core "orphan-safe" dummy positions — `if(meta >= 6) return new TileEntityProxyCombo(...)`),
   `IGUIProvider`, `IConfigurableMachine` (per-machine JSON tuning — `derrick`/`pumpjack`/
   `frackingtower` each have a `getConfigName()`/`readIfPresent`/`writeConfig` triple; no
   equivalent per-machine JSON config system exists in the port yet, separate from the mod-wide
   TOML config Phase 0 already built), `IConnectionAnchors`, `IFluidCopiable`, `IRepairable`
   (Refinery only), `IOverpressurable` (Refinery only), `AutoRegister` (CE's TE-registration
   annotation — all 4 concrete extraction TEs and the Refinery carry it), and
   `com.hbm.inventory.fluid.tank.FluidTankNTM` itself (the actual tank class every TE here
   instantiates 2-5 times — the port has `FluidType`/`FluidStack`/`Fluids` from Phase 0 but not the
   stateful tank container type; this is the same gap `docs/phase2/blockentity_base.md` already
   names as a fluid-machine prerequisite, confirmed here as directly needed by this area).
3. **`UpgradeManagerNT`** — not yet ported. Every TE in this area (all 4 extraction machines) uses
   it for the SPEED/POWER/OVERDRIVE/AFTERBURN upgrade-slot system. `ItemMachineUpgrade` (the item
   side, with a matching `UpgradeType` enum: `SPEED`, `POWER`, `AFTERBURN`, `OVERDRIVE`) already
   exists from Phase 1 — only the machine-side manager class that reads those slots is missing.
4. **Menu/Screen (`AbstractContainerMenu`+`Screen`) framework** (owned by
   `docs/phase2/gui_framework.md`, not re-derived here, and confirmed still absent from the port —
   grepped for `*Menu*`/`*Screen*` under `com.hbm`, zero hits besides an unrelated enum). This area
   needs exactly **two** GUI pairs, not four: one shared by all three extractors
   (`ContainerMachineOilWell`/`GUIMachineOilWell`), one for the Refinery.
5. **Recipe/datagen cross-cutting gap** (`com.hbm.inventory.RecipesCommon` /
   `com.hbm.inventory.recipes.loader.GenericRecipe(s)`, already flagged missing in
   `docs/phase1/STATUS.md` — not re-solved here). `RefineryRecipes` is a **third, refinery-specific
   shape**, distinct from `RecipesCommon`/`GenericRecipe` — see Key design decisions. Whoever owns
   the recipe/datagen cross-cutting work needs to know this area's recipe shape does not fit either
   of the two already-flagged missing types; it needs its own JSON `Recipe<?>` (or a deliberate
   decision to keep it a hardcoded Java registration list, since CE itself never made it
   data-driven either).
6. **World-gen (explicitly this task's flagged Phase 4 boundary)**: `BedrockOilDeposit` (extends
   `AbstractPhasedStructure`, places `ore_bedrock_oil` in a bedrock-adjacent blob plus a porous-stone
   vein plus one `OilSpot.generateOilSpot` call, all keyed to natural chunk generation) and the
   two-file `OilBubble`/`OilSandBubble` system (located, not read — same Phase 4 category) own *how
   and where* oil ore naturally spawns. **The boundary is precise**: Phase 2 needs `ore_oil` /
   `ore_bedrock_oil` / `ore_oil_empty` to exist as registered `Block`s so the extraction TEs'
   `block == ModBlocks.X` identity checks compile and behave correctly; Phase 4 owns the feature
   registration that decides where those blocks appear in freshly generated terrain. Registering
   the blocks themselves is cheap and is listed as a small Phase-2-adjacent gap below, not a Phase 4
   dependency.
7. **Small block-registration gap, not a structural blocker**: grepped and confirmed **absent**
   from the port today: `ore_bedrock_oil`, `ore_oil_empty`, `ore_oil_sand`, `gas_radon_dense`,
   `gas_asbestos`, `stone_cracked`, `stone_porous`, `sand_dirty`, `sand_dirty_red`. (`ore_oil`,
   `oil_pipe`, `dirt_oily`, `dirt_dead` already exist, see Phase-2-safe scope.) None of these need
   new behavior beyond what similar already-ported ore/decorative blocks already do — they're
   cheap to add — but the derrick/pumpjack's suck-search (`canSuckBlock` checks `ore_oil_empty`)
   and the fracking tower's bedrock-suck path (`ore_bedrock_oil`) will silently no-op without them.
   Two Phase 1 items (`ItemOilDetector`, `ItemSurveyScanner`, per their own source comments in
   `ToolItems.java`/`ItemOilDetector.java`) are *also* stubbed pending exactly `ore_oil`/
   `ore_bedrock_oil` — registering `ore_bedrock_oil` for this area's sake would unblock those two
   Phase 1 stubs as a side effect, worth doing once rather than twice.
8. **JEI integration** (`RefineryRecipeHandler`) — CE-specific mod-integration glue with no
   NeoForge-1.21 JEI/REI decision made anywhere in this port. Out of this task's boundary; probably
   belongs with whichever later phase owns recipe-viewer parity (Phase 5 "Client & UX" per the
   project's own phase list).

## Key design/API decisions

- **All four blocks follow `docs/phase2/multiblock_framework.md`'s already-decided
  `BlockDummyable` contract** — the port's own `BlockDummyable.java` (already written) preserves
  CE's packed 0-15 `META` `IntegerProperty` encoding bit-for-bit rather than Neo Edition's
  `FACING`+`DummyBlockType` redesign, and `multiblock_framework.md` confirms `MultiblockHandlerXR`
  must be written against that same `META` encoding, not Neo Edition's. Every concrete oil-chain
  block subclass should therefore call `getDimensions()`/`getOffset()`/`checkRequirement`/
  `fillSpace` exactly as CE's four block classes already do — this report found no oil-specific
  reason to deviate from that already-established convention.
- **`TileEntityMachinePumpjack`/`TileEntityMachineFrackingTower`/`TileEntityMachineRefinery` all
  use a non-core "proxy" `TileEntity` for their non-core dummy positions**
  (`if(meta >= 6) return new TileEntityProxyCombo(false, true, true);`), while
  `TileEntityMachineOilWell` ("derrick") does not — its dummy positions get no `TileEntity` at all
  (`return null`). This is a real, confirmed-by-source asymmetry, not an oversight to normalize
  away: the derrick is the one machine in this area simple enough that its dummy blocks need no
  runtime state.
- **`RefineryRecipes` is a bespoke, refinery-only recipe shape** — a
  `LinkedHashMap<FluidType, Tuple.Quintet<FluidStack,FluidStack,FluidStack,FluidStack,ItemStack>>`
  populated once by a hardcoded `registerRefinery()` call (4 refinery entries + 2 "vacuum" entries,
  confirmed by reading the full 137-line file — CE never made this moddable/data-driven, it is not
  a JEI-exposed "recipe system" the way `RecipesCommon` is elsewhere in the mod). It is keyed by
  *input* `FluidType` (`HOTOIL`, `HOTCRACKOIL`, `HOTOIL_DS`, `HOTCRACKOIL_DS`) and produces up to 4
  fixed-percentage output fluids plus one optional item byproduct, gated by a `power`/`sulfur`
  accumulator inside `TileEntityMachineRefinery.refine()` (5 power + 100 fill of input per tick,
  1 sulfur-counter tick per cycle, item byproduct emitted every `maxSulfur` (100) cycles). No
  vanilla or already-designed NeoForge `Recipe<?>` shape in this codebase fits "1 fluid in → up to
  4 fluids + 1 item out at fixed ratios" — this needs either a bespoke `RecipeType`/
  `RecipeSerializer` or a decision to keep porting it as a literal hardcoded Java registration list
  (the latter is the lower-risk, more literal option and does not block on datagen landing first).
- **Fluid handling stays this port's own `com.hbm.inventory.fluid.*` tank abstraction, confirmed
  not a world-fluid-block system** — every tank in this area (`OIL`/`GAS`/`FRACKSOL` on the
  extractors, `HOTOIL`/`HEAVYOIL`/`NAPHTHA`/`LIGHTOIL`/`PETROLEUM` on the refinery) is a
  `FluidTankNTM` instance pushed between machines via `DirPos`-addressed `IFluidStandardTransceiver`
  connections, never a placed fluid block in the world. Confirmed consistent with Phase 1's own
  finding that no world-fluid-block system exists in this port — none is needed here either.
- **Confirmed NeoForge 1.21.1 `BlockEntity`/`Block` API shapes, cross-checked against Neo Edition
  for syntax only** (behavior stays CE's): `BlockEntity newBlockEntity(BlockPos, BlockState)` +
  `EntityBlock#getTicker(Level, BlockState, BlockEntityType<T>)` returning a ticker lambda (ticking
  is opt-in per `Block`, not automatic per `BlockEntity` — matches what
  `docs/phase2/blockentity_base.md` already documents); every concrete `Block` subclass needs its
  own `MapCodec<T>` via `simpleCodec(...)` and a `codec()` override (`BlockDummyable` itself is
  abstract and doesn't show this — each of this area's 4 concrete block classes will need to add
  it); menus are opened via `player.openMenu(new SimpleMenuProvider(menuProvider,
  menuProvider.getDisplayName()), corePos)`, already wired into the port's own
  `BlockDummyable.standardOpenBehavior` — confirmed by `docs/phase2/multiblock_framework.md` and
  independently re-confirmed here by reading that method in full, so the oil-chain blocks only need
  their core `BlockEntity` to implement `MenuProvider`, nothing bespoke. `RegistryFriendlyByteBuf`
  (not `FriendlyByteBuf`) is the confirmed sync-packet type for `serialize`/`deserialize`-style
  payloads (seen in Neo Edition's `OilDrillBaseBlockEntity.serialize`), consistent with vanilla
  1.21.1's split between the two buffer types for registry-object-bearing vs. plain payloads.
- **HE energy stays `com.hbm.api.energymk2.*`, unchanged, per ground rules** — confirmed already
  present and untouched in the port; all 4 extraction TEs plus the Refinery implement
  `IEnergyReceiverMK2` directly in CE and should continue to.

## Open questions / risks

- **Real, currently-unresolved conflict between this area's two hard prerequisites, surfaced by
  reading both in full — flag for the orchestrating session before implementing anything here.**
  `docs/phase2/multiblock_framework.md` states under "Key design/API decisions" a firm **Decision:
  rename `com.hbm.tileentity` → `com.hbm.blockentity`** (option B) and proceeds to design
  `IPersistentNBT`/`MultiblockHandlerXR` against that package. `docs/phase2/blockentity_base.md`,
  covering the base `BlockEntity` class hierarchy that sits right next to it, reaches its own
  recommendation the *opposite* way — **preserve `com.hbm.tileentity`** (option A) — explicitly
  labeling it "offered for the record but not self-authorized" and "the single highest-priority
  open decision blocking this package's implementation." Since this oil-chain area depends on
  *both* packages (the multiblock framework for `BlockDummyable`/`MultiblockHandlerXR`, the base
  block-entity package for `TileEntityMachineBase`-equivalent/`IGUIProvider`/etc.), it cannot be
  implemented under two different package roots at once. This needs one explicit sign-off from
  whoever owns cross-package Phase 2 decisions — not a third independent guess made inside this
  report, and not something either sibling report can resolve alone since they already disagree.
- **`MultiblockHandlerXR`'s per-axis `int[]` dimension encoding is easy to transcribe wrong.** This
  area alone has 4 blocks with a combined 10+ distinct `checkSpace`/`fillSpace` call sites, each
  passing a 6-element `int[]` whose axis semantics are documented only by CE's call-site convention
  (`docs/phase2/multiblock_framework.md` already flags this as a general risk for the framework
  itself) — the Fracking Tower's `checkRequirement` alone chains 7 different shapes. Recommend
  porting `MultiblockHandlerXR` itself by literal translation first (already scoped by the sibling
  report), then transcribing each of this area's call sites value-for-value from CE source rather
  than re-deriving what the numbers "should" mean.
- **`TileEntityMachineRefinery`'s audio loop was not independently verified against the port's
  current state.** It calls `MainRegistry.proxy.getLoopedSound(...)` returning an `AudioWrapper`,
  tied to the `isOn` flag with a 20-tick keep-alive window — this pass located the CE source but
  did not check whether `AudioWrapper`/`getLoopedSound` already exist in this port (Phase 0's sound
  registry area may or may not cover looped sounds). Flag as an open verification item for whoever
  implements the Refinery specifically — it is the only TE in this area with continuous audio tied
  to machine state.
- **CE's `TileEntityMachineOilWell`/`TileEntityMachinePumpjack.onDrill` uranium/asbestos
  side-effects reference `gas_radon_dense`/`gas_asbestos`**, two blocks confirmed absent from the
  port (see Deferred scope #7). These are minor flavor side-effects (placing a dense-radon-gas
  block near a drilled uranium vein), not core to the extraction loop — safe to stub as a no-op
  until those two blocks exist, rather than blocking the whole TE port on them.
- **Not verified in this pass, named for the record**: `OilBubble.java` (197 lines) and
  `OilSandBubble.java` (89 lines) were located under `com.hbm.world` but not read — they are
  additional oil-deposit world-gen (Phase 4 per this task's own boundary), flagged here only so
  whoever picks up Phase 4's oil world-gen doesn't have to rediscover their existence.
