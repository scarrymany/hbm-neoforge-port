# Chemical plant / centrifuge / gas centrifuge / cyclotron / SILEX / electrolyser triage

Source: `hbm-ce/src/main/java/com/hbm/tileentity/machine/` (surveyed via grep for
`ChemPlant`/`Chemical`, `Centrifuge`, `Cyclotron`, `SILEX`/`Laser`, `Electrolyser`/`Electrolysis`),
plus their recipe classes in `com.hbm.inventory.recipes.*`, the shared module base in
`com.hbm.modules.machine.*`, and the fluid tank/energy abstractions in `com.hbm.inventory.fluid.tank`
and `com.hbm.api.energymk2`.

8 tile-entity classes matched the survey (the "Centrifuge" name covers two distinct machines in CE —
an item-only centrifuge and a fluid-based gas centrifuge — which PORT_SPEC's naming collapses into
one line item but which are functionally unrelated):

| Class | Role |
|---|---|
| `TileEntityMachineCentrifuge` | Item→item[] separator (ore washing/breakdown), no fluids |
| `TileEntityMachineGasCent` | **Gas centrifuge** — UF6/PUF6/WATZ isotope-enrichment cascade, the real "centrifuge" of the task |
| `TileEntitySILEX` | Laser isotope/element separation (SILEX = Separation of Isotopes by Laser EXcitation) |
| `TileEntityMachineCyclotron` | Particle-accelerator transmutation (item + catalyst → item + antimatter fluid) |
| `TileEntityMachineChemicalPlant` | 1-recipe-slot chemical processing, JSON-recipe-driven |
| `TileEntityMachineChemicalFactory` | 4× chemical-plant modules sharing one multiblock + coolant loop |
| `TileEntityElectrolyser` | Two independent sub-machines in one TE: fluid electrolysis + ore/crystal electrolysis (foundry casting) |
| `TileEntityMachineMiningLaser` | World-block-breaking quarry laser (not chemistry, but matched the SILEX/Laser grep and lives in this package) |

Also read for context (not in the TE package, but load-bearing for the above): `GasCentrifugeRecipes`,
`SILEXRecipes`, `CyclotronRecipes`, `CentrifugeRecipes`, `ElectrolyserFluidRecipes`,
`ElectrolyserMetalRecipes`, `ChemicalPlantRecipes` (shared by plant+factory),
`com.hbm.modules.machine.{ModuleMachineBase,ModuleMachineChemplant}`,
`com.hbm.inventory.recipes.loader.{GenericRecipe,GenericRecipes,SerializableRecipe}`,
`com.hbm.inventory.fluid.tank.FluidTankNTM`, `com.hbm.api.fluid.{IFluidStandardReceiver,
IFluidStandardSender,IFluidStandardTransceiver}`, `com.hbm.api.energymk2.*`,
`com.hbm.tileentity.TileEntityMachineBase`/`TileEntityLoadedBase`. ~22 files surveyed in total.

## Headline finding

**None of these 8 machines can be started yet** — every one of them extends
`com.hbm.tileentity.TileEntityMachineBase` (itself extending `TileEntityLoadedBase`), implements
`com.hbm.tileentity.IGUIProvider`, and uses `com.hbm.inventory.UpgradeManagerNT` +
`com.hbm.items.machine.ItemMachineUpgrade.UpgradeType` for the speed/power/overdrive/effect upgrade
math that is common to literally every one of them. **None of these four exist in the port yet**
(confirmed by directory search — no `com.hbm.tileentity` or `com.hbm.blockentity` package, no
`TileEntityMachineBase`, no `IGUIProvider`, no `UpgradeManagerNT` anywhere under
`/home/user/hbm-neoforge-port/src`). This is the single shared prerequisite for this entire area, not
specific to any one machine — see "Deferred scope" and Phase 0's open decision on the
`com.hbm.tileentity` vs `com.hbm.blockentity` package name (below).

The fluid tank abstraction itself — `com.hbm.inventory.fluid.tank.FluidTankNTM`, used by 6 of the 8
machines here — is **also not yet ported**, even though it's a Phase-0-flavored leaf class with no
machine-specific logic. It was already flagged as missing in `docs/phase0/STATUS.md`'s compile-error
list. Every consumer of it that already exists in this port (`FluidType.onTankBroken/onTankUpdate/
onFluidRelease`, `com.hbm.api.fluidmk2.IFluidRegisterListener`) already expects its Mojang-mapped/
NeoForge-native shape (`BlockEntity` params, not 1.12 `TileEntity`), so porting `FluidTankNTM` itself
should be a fairly mechanical Phase 2 task — it has no block-entity-lifecycle logic of its own beyond
`withOwner(TileEntity)` for one broken-tank callback.

By contrast, the recipe/config layer above the tank (`GenericRecipe`/`GenericRecipes`/
`SerializableRecipe`, `com.hbm.inventory.RecipesCommon`) is the same cross-cutting gap
`docs/phase1/STATUS.md` already flags — **not re-solved here**, just documented per-machine below.

## Phase-2-safe scope

The following is portable now (once the shared prerequisites below land) with no further upstream
research needed — CE's math is fully read and can be transcribed exactly:

### `TileEntityMachineCentrifuge` — no fluids, simplest of the 8
- 8-slot `ItemStackHandler`: slot 0 = item input, slot 1 = battery (`IBatteryItem`), slots 2-5 = up
  to 4 item outputs, slots 6-7 = upgrade slots.
- Recipe: `CentrifugeRecipes.getOutput(ItemStack) -> ItemStack[]` (up to 4 outputs), a flat
  `HashMap<AStack, ItemStack[]>`-style table (item), no fluids at all.
- Progress/power: `progress` (int, ticks) vs. configurable `processingSpeed` (default 200 ticks);
  `power` (long) vs. `maxPower` (default 100 000 HE). `baseConsumption` = 200 HE/tick, modified by
  `UpgradeManagerNT` (`SPEED` level increases speed and consumption 1:1; `OVERDRIVE` level multiplies
  speed by `1 + level*5` and adds a much larger consumption penalty; `POWER` level divides
  consumption by `1 + level`). All three config numbers (`maxPower`, `processingSpeed`,
  `baseConsumption`) are also **JSON-configurable per-instance-of-class** via
  `IConfigurableMachine.readIfPresent`/`writeConfig` (a config-file override system, not the
  JSON-recipe system) — worth preserving as its own small interface.
- GUI: single progress bar + power bar, `ContainerCentrifuge`/`GUIMachineCentrifuge`.

### `TileEntityMachineGasCent` — the real isotope-separation math
This is the "centrifuge" the task is really asking about. It models a **physical gas-centrifuge
cascade**: each block is one enrichment *stage*, stages are chained by placing centrifuges in a line
(the block's facing direction plus `BlockDummyable.offset`-derived metadata determines which
neighbor is upstream/downstream), and enriched product physically flows from one block's output tank
into the next block's input tank (`attemptTransfer`, polled every 10 ticks).

**The enrichment math** (`GasCentrifugeRecipes.PseudoFluidType`, a *pseudo*-fluid — not a real
`FluidType`, tracked in a lightweight inner `PseudoFluidTank` class, not `FluidTankNTM`):
- A real feed fluid (`Fluids.UF6`, `Fluids.PUF6`, `Fluids.WATZ` — actual `FluidTankNTM`-held fluids)
  is converted 1:1 into a pseudo-fluid via `GasCentrifugeRecipes.fluidConversions` the moment it's
  loaded into the machine's real `tank` (2000 mB capacity) — `attemptConversion()` drains the real
  tank into the `inputTank` pseudo-fluid at whatever rate room allows.
- Each `PseudoFluidType` stage has 4 numbers: `fluidConsumed`, `fluidProduced` (always
  `fluidProduced < fluidConsumed` — the delta represents tailings/depleted material physically
  skimmed off as items, not lost), `outputFluid` (the next stage up), and an item `output[]` (the
  tailings byproduct dropped into the item output slots every enrich cycle).
- The **cascade for uranium**: `NUF6` (natural, consumed 400/produced 300, byproduct 1×`nugget_u238`)
  → `LEUF6` (consumed 300/produced 200, byproduct `nugget_u238`+`fluorite`) → `MEUF6` (consumed
  200/produced 100, byproduct `nugget_u238`) → `HEUF6` (**terminal**, `outputFluid = NONE`,
  `isHighSpeed = true`, consumed 300/produced 0, byproduct 2×`nugget_u238` + 1×`nugget_u235` +
  `fluorite` — the actual enriched-uranium payout). The plutonium (`PF6`) and irradiated-water
  (`MUD`→`MUD_HEAVY`) chains follow the same consumed/produced/output shape but with only 1-2 stages.
  **This 4-number-per-stage table is the exact game-balance data to preserve verbatim** — it is the
  entire enrichment yield/cost curve for the mod's nuclear fuel cycle.
- `isHighSpeed` gates the terminal `HEUF6` stage behind `ModItems.upgrade_gc_speed` in the upgrade
  slot — i.e. the final enrichment step literally cannot run without the speed upgrade installed,
  independent of the speed upgrade's normal effect (which is: `processingSpeed` (150 ticks) − 70 =
  80 ticks, and power draw 300/tick instead of 200/tick).
- `enrich()` runs when `progress >= getProcessingSpeed()`: drains `fluidConsumed` from `inputTank`,
  adds `fluidProduced` to `outputTank` (whose type is fixed to `inputTank.getTankType().getOutputType()`),
  and unconditionally drops the byproduct `ItemStack[]` into slots 0-3.
- A separate, unrelated hardcoded conversion runs every 10 ticks: `LEUF6` output ≥ 600 mB (and no
  upstream centrifuge to hand off to) converts directly into `6×nugget_uranium_fuel + fluorite` —
  i.e. a centrifuge with nowhere left to cascade to can "settle" for low-enriched reactor fuel instead
  of continuing the chain. Preserve this fallback exactly; it's an intentional escape hatch, not dead
  code.
- 7-slot inventory: 0-3 = item output, 4 = battery, 5 = fluid-identifier item (sets `tank`'s type via
  `IItemFluidIdentifier`), 6 = upgrade slot.
- The `GasCentrifugeRecipes` registration table (`gasCent` map) additionally records, per full feed
  `FluidStack`, a "# of centrifuges" integer and a boolean `isHighSpeed` flag purely as **display
  metadata** for JEI (`GasCentrifugeRecipeHandler`) — not consumed by the TE's own logic, which reads
  `PseudoFluidType` fields directly. Don't confuse the two data sources when porting recipes.

### `TileEntitySILEX` — laser-gated, weighted-random single-output separation
A different isotope/element-separation model from the gas centrifuge: instead of a cascade, SILEX
does single-shot laser ablation of a solid feedstock, gated by an externally-supplied laser
wavelength.
- `mode: EnumWavelengths` (from `ItemFELCrystal.EnumWavelengths` — `NULL, IR, VISIBLE, UV, ...`,
  ordinal-ordered by increasing "strength") is **reset to `NULL` every tick** and must be re-set
  *from outside* by a separate Free-Electron Laser block (`TileEntityFEL`, not in this survey's file
  list — `silex.mode = this.mode` is the only writer found via grep) aiming a beam at the SILEX block
  that tick. This is a cross-block coupling dependency: SILEX alone cannot process anything without a
  working FEL neighbor. Flag as a soft dependency on the FEL TE for functional testing, even though
  SILEX's own class is self-contained.
- `SILEXRecipes.SILEXRecipe`: `fluidConsumed`, `fluidProduced` (an internal `currentFill` counter,
  0-16000, not a real `FluidTankNTM` — items and UF6/PUF6/fluid-icon inputs are all pre-converted
  into this one linear "material charge" via `ComparableStack`-keyed `fluidConversion`/direct lookup),
  `laserStrength: EnumWavelengths` (minimum required wavelength), and a **weighted-random** output
  pool (`outputs: WeightedRandomObject` list, resolved via `WeightedRandom.getRandomItem` — one
  random item stack per completed cycle, not all outputs deterministically like the centrifuge/plant).
  ~140 registered recipes cover uranium/plutonium/RBMK-pellet reprocessing, americium, australium,
  schrabidium, and various ore/waste breakdowns — a very large recipe table, all straightforward
  input-item → weighted-output-pool data once the recipe-loader gap is closed.
- **The core isotope-separation formula to preserve exactly**:
  `progress += Math.pow(2, mode.ordinal() - recipe.laserStrength.ordinal() + 1) / 2`. Every wavelength
  tier above the recipe's minimum required strength **doubles** the process-speed increment per tick
  (an exact power-of-two speed multiplier, not a flat bonus) — using a weaker-than-required laser
  yields a non-positive exponent and therefore <1 progress/tick effectively never finishing (bounded
  by the earlier `laserStrength.ordinal() <= mode.ordinal()` guard, which actually already refuses to
  process at all if underpowered — the pow() line only scales speed *above* the minimum).
- 11-slot inventory: slot 0 = item input, slots 2-3 = fluid-container item I/O for the tank (16000 mB,
  starts as `Fluids.PEROXIDE`), slot 4 = process output landing slot, slots 5-10 = a 6-slot output
  queue/buffer that `dequeue()` drains item 4 into (stacking into matching queue slots first).
- GUI: `ContainerSILEX`/`GUISILEX`, no upgrade slots at all (only machine in this set with none).

### `TileEntityMachineCyclotron` — transmutation, not isotope separation, but adjacent
3 parallel recipe "lanes" (slots 0-2 = target item, 3-5 = catalyst item, 6-8 = output item), each
independently checked against one shared `CyclotronRecipes.recipes: HashMap<Pair<ComparableStack,
AStack>, Pair<ItemStack, Integer>>` table (target×catalyst → output item + antimatter mB amount).
Every successful transmutation across all 3 lanes adds its antimatter yield into `tanks[2]` (AMAT,
8000 mB cap). Two more tanks: `tanks[0]` (water, 32000, coolant in) and `tanks[1]` (spent steam,
32000, coolant out) — `getCoolantConsumption() = 500/(effect_upgrade+1) * getSpeed()` mB/tick
converted from water to spent steam every tick processing runs, independent of whether any lane is
actually mid-recipe. `duration` = 690 ticks per completed item per lane; `consumption` = 1 000 000
HE/tick baseline (`maxPower` = 100 000 000 HE — by far the largest single-tick draw of the 8
machines). Upgrade slots 10-11; `IFluidCopiable` (settings-paste tool support). This is a 6-block-tall,
multi-block-footprint machine (`getConPos()` reaches ±3 blocks on X/Z) — confirm `BlockDummyable`
multiblock wiring (already ported in Phase 1) covers this footprint shape before assuming the block
side is trivial.

### `TileEntityMachineChemicalPlant` / `TileEntityMachineChemicalFactory`
Both delegate all actual recipe logic to `com.hbm.modules.machine.ModuleMachineChemplant` (a thin
subclass of `ModuleMachineBase` that just wires 3 item-in/3 item-out slots and 3
`FluidTankNTM` in/3 out per module instance) and `ChemicalPlantRecipes.INSTANCE` (a
`GenericRecipes<GenericRecipe>` singleton keyed by recipe *name*, not by matching input, with
player-selectable recipes via a `receiveControl`/`IControlReceiver` GUI dropdown, not automatic
recognition). Plant = 1 module (22-slot inventory, 3 fluid in + 3 fluid out tanks @ 24 000 mB each,
`maxPower` dynamically set from the active `GenericRecipe.power * 100`, floored at 100 000 HE).
Factory = 4 identical modules sharing one 32-slot inventory, 12+12 fluid tanks, plus a
water→spent-steam coolant loop shared across all 4 lanes (100 mB water/lps per successful process,
independent of recipe) and inter-tank auto-balancing (any output tank of one lane top up any
compatible-type input tank of another lane, ≤50 mB/tick, up to 144 pair-checks/tick — cheap because
almost all pairs are `Fluids.NONE`). Both implement `IRORValueProvider`/`IRORInteractive`
(Redstone-over-Radio — read-only telemetry + a remote `setrecipe` function), a separate
cross-cutting automation/networking system not part of this survey's scope; note it as a soft
dependency if RoR parity is wanted for these two blocks specifically.
The Factory additionally implements `TileEntityProxyDyn.IProxyDelegateProvider`: a dummy/proxy TE at
specific relative offsets (`coolantLine`, computed from facing) delegates capability queries back to
an inner `DelegateChemicalFactoy` object that exposes *only* the water/lps tanks — i.e. neighbors
plugged into the "coolant port" positions see a completely different capability surface (water in,
spent steam out) than neighbors plugged into the main recipe-fluid ports on the rest of the
multiblock shell. This proxy-delegation pattern is worth generalizing (it likely recurs in other
Phase 2 multiblocks) rather than reinventing per-machine.

### `TileEntityElectrolyser` — two independent recipe systems sharing one power/upgrade state
Genuinely two machines glued into one TE with a `lastSelectedGUI` toggle deciding which
`Container`/`Screen` opens (`provideContainer`/`provideGUI` take an `ID` of `-1` meaning "whatever was
last selected" — a real, reusable "dual-GUI-block" pattern to carry into the NeoForge menu framework):
1. **Fluid electrolysis** (`ElectrolyserFluidRecipes`, keyed by single input `FluidType` →
   `ElectrolysisRecipe{amount, output1: FluidStack, output2: FluidStack, duration, byproduct:
   ItemStack[]}`, e.g. water 2000 mB/2000 ticks → 200 mB hydrogen + 200 mB oxygen). 4 tanks: input
   (water, 16000), hydrogen out, oxygen out, and a separate nitric-acid tank (16000, gates the *ore*
   side below, not the fluid side).
2. **Ore/crystal electrolysis** (`ElectrolyserMetalRecipes`, keyed by `AStack` item →
   `ElectrolysisMetalRecipe{output1: Mats.MaterialStack, output2: Mats.MaterialStack, byproduct:
   ItemStack[], duration}`) consumes the item in slot 14 plus 100 mB nitric acid per cycle and
   accumulates molten `Mats.MaterialStack` in `leftStack`/`rightStack` (two independent output
   streams, e.g. iron ore → 6 ingots-worth of iron + 2 ingots-worth of titanium), which are then
   **poured into the world as molten metal via `com.hbm.util.CrucibleUtil.pourFullStack`** (a foundry
   casting system, not in this port at all yet — flag as a real Phase 2/4 boundary dependency: the
   electrolyser's ore side cannot function without a foundry/casting target existing downstream).
- Both sides share `power`/`maxPower` (20 000 000 HE, largest steady-state cap here besides the
  cyclotron) and one `UpgradeManagerNT`. `getCycleCount() = min(1 + overdrive*2, 7)` runs the whole
  process-check-and-advance loop up to 7× per tick under Overdrive — note this multiplies *both*
  sub-machines' progress per game tick, not just one.
- GUI: `ContainerElectrolyserFluid`/`GUIElectrolyserFluid` and `ContainerElectrolyserMetal`/
  `GUIElectrolyserMetal` — two full Container/Screen pairs for one block.

### `TileEntityMachineMiningLaser` (matched the Laser grep; not chemistry, flagged for completeness)
A world-quarry block: reads `CentrifugeRecipes`/`CrystallizerRecipes`/`ShredderRecipes` (all
item-drop-table recipe registries, not fluid-based) to decide what a mined block yields, breaks
blocks in the world via a scanning/targeting beam (`targetX/Y/Z`), and separately holds an
`FluidTankNTM tankNew` (oil, 64000) *and* a raw Forge `FluidTank tank` side-by-side — the latter looks
like leftover/dead code from a not-yet-completed migration to `FluidTankNTM` in CE itself and should
be diffed carefully rather than ported literally; verify against the latest CE `main` before treating
both fields as intentional. Upgrade math (`SPEED`, `OVERDRIVE`, `EFFECT`→range, `FORTUNE`) mirrors the
other machines. Its real complexity is world-breaking/quarry mechanics (dam-building over liquids,
block-break-progress network packets, furnace-recipe fallback via `net.minecraft.item.crafting.
FurnaceRecipes`) which is much closer to Phase 4 (world/simulation) territory than to the chemical-
processing machines above — **recommend implementing its TE scaffold/GUI/upgrade math alongside the
other 7 in Phase 2, but treating its actual world-breaking loop as needing the same care Phase 4's
world-interaction code gets**, not a plain "port the recipe lookup" task like the others.

## Deferred scope

Everything below blocks *all 8* machines above, not just one — porting any single machine class in
isolation will not compile.

1. **Block-entity base-class layer** (`com.hbm.tileentity.TileEntityMachineBase` →
   `TileEntityLoadedBase` → NeoForge `BlockEntity`). Nothing under this name or under a renamed
   `com.hbm.blockentity` package exists in the port yet. **This is exactly the open decision flagged
   in `docs/phase0/STATUS.md`** ("`IPersistentNBT` package: CE has it under `com.hbm.tileentity`;
   Neo Edition renamed the whole tile-entity layer to `com.hbm.blockentity`... needs one explicit
   decision before Phase 2 block entities land") — it needs resolving *before* any of these 8 classes
   can be written, since every one of them is a block entity. This report does not resolve it; it
   only confirms via `grep`/`find` that (a) no `com.hbm.tileentity` or `com.hbm.blockentity` package
   exists in `/home/user/hbm-neoforge-port/src` at all, and (b) several already-ported Phase 0/1
   classes (`FluidType.onTankBroken(BlockEntity be, ...)`, `NTMEnergyCapabilityWrapper`,
   `NTMFluidHandlerWrapper`, `IEnergyConductorMK2.createNode()`) already assume the target type is
   NeoForge's `net.minecraft.world.level.block.entity.BlockEntity`, which at least answers "does the
   eventual base class extend `BlockEntity` and not something else" even though the *package name*
   for it is still undecided.
2. **GUI/menu framework** (`AbstractContainerMenu` + `Screen` pairs, `MenuType` registration). Zero
   `AbstractContainerMenu`/`MenuType`/machine `Screen` classes exist in the port (confirmed by
   search — the only `Menu`/`Screen` hits in `com.hbm` are unrelated: `IContainerOpenEventListener`,
   `ItemBook`/`ItemGuideBook`'s own item-owned GUI, and an unrelated `ItemEnums` field literally named
   `Screen`). Every one of the 8 machines here implements `IGUIProvider` and needs a
   `Container`→`Screen` pair (some need two, see Electrolyser above). This is the same "GUIs need
   AbstractContainerMenu+Screen pairs... it likely doesn't [exist]" gap flagged in the task brief,
   confirmed true by this survey — **shared prerequisite across all of Phase 2**, not specific to
   this document's machines.
3. **`com.hbm.inventory.fluid.tank.FluidTankNTM`** — confirmed missing (see Headline finding above).
   Low-risk mechanical port once the block-entity base class exists (its only BE-typed method,
   `withOwner`, just stashes a reference for one broken-tank callback).
4. **`UpgradeManagerNT` + `ItemMachineUpgrade.UpgradeType`'s consumer side** — `ItemMachineUpgrade`
   itself already exists in the port (`com.hbm.items.machine.ItemMachineUpgrade.java`, presumably
   from Phase 1's items survey), but `UpgradeManagerNT` (the per-TE upgrade-slot-scanning/level-
   caching object every one of these 8 machines instantiates) is not ported. Since every machine's
   speed/power/overdrive math routes through it identically, porting it once unblocks all 8 at once —
   worth doing as its own small task ahead of any individual machine.
5. **Fluid I/O interface family** (`com.hbm.api.fluid.{IFluidStandardReceiver,IFluidStandardSender,
   IFluidStandardTransceiver}` and their non-deprecated backing interfaces
   `com.hbm.api.fluidmk2.{IFluidStandardReceiverMK2,IFluidStandardSenderMK2}`). Only
   `com.hbm.api.fluidmk2.IFluidRegisterListener` exists in the port so far. Note that in CE itself the
   `com.hbm.api.fluid.*` versions are already `@Deprecated` thin wrappers over the MK2 versions — the
   same "MK2 is the real API, legacy name is a compat shim" shape this port already followed for
   energy (`IEnergyProviderMK2`/`PowerNetMK2` vs. no plain `IEnergyProvider`). **Recommend porting
   straight to the fluidmk2 interfaces as the primary API**, matching the energy precedent, rather
   than porting the deprecated non-MK2 names as if they were current. The underlying network-graph
   plumbing they ride on (`com.hbm.uninos.{GenNode,NodeNet,UniNodespace}`) is *already ported*
   (per Phase 0's STATUS.md gap-fill note), so this is mostly interface-and-glue work, not a new
   subsystem.
6. **Recipe/config JSON layer**: `com.hbm.inventory.RecipesCommon`, `com.hbm.inventory.recipes.
   loader.{GenericRecipe,GenericRecipes,SerializableRecipe}`. Already flagged missing in
   `docs/phase1/STATUS.md`; **not re-solved here**. This report's dependency on it: the Chemical
   Plant/Factory's *entire* recipe system (`ChemicalPlantRecipes.INSTANCE`, a `GenericRecipes`
   singleton with player-selectable named recipes) cannot function at all without it — those two
   machines are the most exposed to this gap of the 8. The other 6 machines (Centrifuge, GasCent,
   SILEX, Cyclotron, Electrolyser×2) use simpler ad-hoc `HashMap`-based recipe classes
   (`CentrifugeRecipes`, `GasCentrifugeRecipes`, `SILEXRecipes`, `CyclotronRecipes`,
   `ElectrolyserFluidRecipes`, `ElectrolyserMetalRecipes`) that all extend `SerializableRecipe` for
   JSON-config-override support but do *not* depend on the named/pooled `GenericRecipe` machinery —
   these could in principle be ported as plain static tables sooner, with JSON-override support
   layered on later, if the team wants partial progress on this area before the generic-recipe loader
   lands. `AStack`/`ComparableStack`/`OreDictStack` (from `com.hbm.inventory.RecipesCommon`) are used
   pervasively by these simpler recipe classes too, though, so they are not fully independent of the
   gap either — only the *pooled/named* `GenericRecipe` layer is unique to the Plant/Factory.
7. **Foundry/casting world-interaction** (`com.hbm.util.CrucibleUtil.pourFullStack`) — needed only by
   the Electrolyser's ore/crystal side. Not ported; not researched in depth here since it's a
   world-effects/particle system outside this survey's fluid/recipe/isotope focus. Flag as a
   dependency of the Electrolyser specifically, not the other 7 machines.
8. **Redstone-over-Radio** (`com.hbm.api.redstoneoverradio.{IRORValueProvider,IRORInteractive}`) —
   needed only by Chemical Plant/Factory's telemetry+remote-control surface. Treat as optional/
   deferrable per-machine polish, not a hard blocker — both machines function without it (it only
   adds an external readout/remote-set-recipe capability).
9. **Mining Laser's world-breaking loop specifically** (dam-building over liquids, block-break-
   progress network packets, furnace-recipe fallback) — recommend treating this one machine's core
   loop as Phase 4-adjacent even though its TE scaffold/GUI/upgrades belong in Phase 2 alongside the
   other 7 (see per-machine note above).
10. **`TileEntityFEL`** (Free-Electron Laser) — not in this survey's file list, but SILEX cannot
    receive a non-`NULL` `mode` without it. Needed for *functional* SILEX testing, not for compiling
    SILEX's own class.

## Key design/API decisions

- **BlockEntity base class target confirmed, package name not confirmed.** Every already-ported
  consumer in this codebase (`FluidType`, `NTMEnergyCapabilityWrapper`, `NTMFluidHandlerWrapper`,
  `IEnergyConductorMK2`) imports `net.minecraft.world.level.block.entity.BlockEntity` (Mojang
  mappings, NeoForge 1.21.1) as the eventual machine base type — confirmed by reading those files
  directly in this port, not inferred. What's still open is only the **package** the new
  `TileEntityMachineBase`-equivalent lands in (`com.hbm.tileentity` per CE's layout and per several
  already-ported Phase 0 interfaces that reference that path, vs. `com.hbm.blockentity` per the Neo
  Edition reference) — this is Phase 0 STATUS.md's flagged open decision, unresolved, and this report
  does not pick a side; it just confirms all 8 machines in this document need it settled first.
- **Energy stays HE/`IEnergyReceiverMK2`, confirmed compatible with all 8 machines as-is.** Every
  machine implements `IEnergyReceiverMK2` (already ported, `com.hbm.api.energymk2.*`) with a
  `long power`/`getMaxPower()`/`setPower()` triple charged via `Library.chargeTEFromItems(inventory,
  slot, power, maxPower)` (a static helper in `com.hbm.lib.Library` — check its port status
  separately; not covered by this survey). No machine here touches NeoForge Energy (FE) at all. This
  matches the ground rule as-is with zero new decisions needed.
- **Fluid API: port to the MK2 interfaces, not the deprecated legacy names** (see Deferred-scope
  item 5) — this is a recommendation based on reading CE's own `@Deprecated` annotations and the
  precedent already set by the energy API in this port, not an invented API.
- **Dual-GUI-per-block pattern confirmed real** (Electrolyser): `provideContainer`/`provideGUI` take
  an `int ID` where `-1` means "reopen whatever was last selected," persisted as `lastSelectedGUI` in
  NBT/network sync. When the NeoForge `MenuType`/`AbstractContainerMenu` framework is designed
  (shared Phase 2 prerequisite), it should accommodate a single `BlockEntity` opening one of several
  `MenuType`s based on interaction context (e.g. which face was clicked, or an explicit "switch mode"
  button) rather than assuming one `BlockEntity` ⇒ one `MenuType`.
- **Proxy/delegate capability surfaces confirmed real** (Chemical Factory's `IProxyDelegateProvider`
  + `TileEntityProxyDyn`, and separately the Electrolyser/Cyclotron/Chemical Plant/Factory's shared
  pattern of `DirPos[]`-enumerated fixed I/O ports around a multiblock shell computed from block
  facing). Neither `TileEntityProxyDyn` nor `IProxyDelegateProvider` were read in depth in this pass
  (out of scope: they belong to the generic multiblock-dummy-block system, not to any one machine's
  chemistry/recipe logic) — flagging their existence so whoever designs the shared multiblock/BE
  framework for Phase 2 is aware at least one machine (Chemical Factory) needs per-position capability
  delegation, not just per-position accessible-slot filtering.
- **Recipe registration entry point**: all recipe classes here (`CentrifugeRecipes`,
  `GasCentrifugeRecipes`, `SILEXRecipes`, `CyclotronRecipes`, `ElectrolyserFluidRecipes`,
  `ElectrolyserMetalRecipes`) extend `com.hbm.inventory.recipes.loader.SerializableRecipe` and
  populate a static table in a `registerDefaults()`/`register()` method called once at mod init —
  this is the same shape Phase 1 already found for other CE recipe registries, confirming it's a
  mod-wide convention, not something specific to this area.

## Open questions / risks

- **The `com.hbm.tileentity`/`com.hbm.blockentity` package decision genuinely cannot be deferred
  further once Phase 2 machine work starts** — this document adds 8 more concrete classes (plus their
  Containers/Screens/recipe classes) that all need it settled. Recommend resolving it as the very
  first Phase 2 implementation task, before writing any of these 8 TEs, since retrofitting a package
  rename across 8+ freshly-written classes (and their eventual JSON recipe/datagen references) is
  pure waste.
- **`TileEntityMachineMiningLaser`'s dual fluid-tank field** (`FluidTankNTM tankNew` alongside a raw
  Forge `FluidTank tank`) looks like an in-progress migration left mid-flight in CE itself. Confirm
  against upstream CE's latest commit (not just the snapshot in this repo's `upstream/hbm-ce`) before
  porting literally — porting a half-migrated field pair verbatim would bake in what may be CE's own
  dead code.
- **JEI-only "# of centrifuges" metadata in `GasCentrifugeRecipes.gasCent`** is never read by the TE
  logic itself (confirmed by reading `TileEntityMachineGasCent` end to end — it only reads
  `PseudoFluidType` fields). Since this port has no JEI/REI integration decision made yet (out of
  scope for this document), it's unclear whether that display-only data needs porting at all in
  Phase 2, or should wait for whatever this port's item/fluid tooltip or recipe-viewer strategy ends
  up being. Flagging rather than deciding.
- **Chemical Plant/Factory's `IRORValueProvider`/`IRORInteractive` (Redstone-over-Radio)** was not
  traced further upstream (what block sends/receives RoR signals, whether any of that system is
  ported or planned) — if RoR parity for these two blocks specifically matters, that needs its own
  research pass; this document only confirms the two machines *implement* those interfaces and what
  each interface call does locally (read-only telemetry strings + one `setrecipe` remote function).
- **`Library.chargeTEFromItems`** (used by all 8 machines to charge from a battery-item slot) was not
  independently verified as ported in this pass — it's a `com.hbm.lib.Library` static helper, and
  `com.hbm.lib.Library`/`ForgeDirection`/`DirPos` were treated as assumed-ported utility classes per
  the task's framing ("already-ported tank abstraction" implies adjacent `com.hbm.lib` helpers are in
  similar shape), but this was not independently confirmed by directory search the way the tank/BE/
  GUI/upgrade gaps above were. Worth a quick check before Phase 2 implementation assumes it's there.
- **Fluid constants: verified, not a risk.** Every named fluid these 8 machines reference
  (`WATER`, `SPENTSTEAM`, `AMAT`, `HYDROGEN`, `OXYGEN`, `NITRIC_ACID`, `UF6`, `PUF6`, `WATZ`,
  `PEROXIDE`, `DEATH`, `HEAVYWATER`, `DEUTERIUM`, `VITRIOL`, `SULFURIC_ACID`, `CHLORINE`, `SLOP`,
  `MERCURY`, `REDMUD`, `LYE`, `ALUMINA`, `CARBONDIOXIDE`, `POTASSIUM_CHLORIDE`, `CALCIUM_CHLORIDE`,
  `CALCIUM_SOLUTION`, `OIL`, `FULLERENE`) was individually confirmed present in this port's
  `com.hbm.inventory.fluid.Fluids.java` by direct grep. No fluid-registry gap blocks this area.
