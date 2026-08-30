# Shredder / Assembler / Crystallizer / Mixer machine triage (4 target files, +5 adjacent files read for context)

Source: `hbm-ce/src/main/java/com/hbm/tileentity/machine/{TileEntityMachineShredder,
TileEntityMachineAssemblyMachine, TileEntityMachineCrystallizer, TileEntityMachineMixer}.java`, plus
their paired `Container*`/`GUI*` classes in `com.hbm.inventory.{container,gui}`, their recipe classes
in `com.hbm.inventory.recipes`, and the shared `com.hbm.inventory.RecipesCommon` /
`com.hbm.inventory.recipes.loader.{GenericRecipe,GenericRecipes}` / `com.hbm.modules.machine.{
ModuleMachineBase,ModuleMachineAssembler}` framework the assembler depends on.

Goal: survey CE's four "basic processing machine" TEs for Phase 2, extracting recipe shape, power
draw, and GUI slot layout for each, and flagging exactly what's portable now vs. blocked on
not-yet-ported systems (per `docs/phase1/STATUS.md` and `docs/phase0/STATUS.md`'s own open items).

Directory survey note: `com.hbm.tileentity.machine/` contains 6 files matching an "assembler" search
by name — `TileEntityMachineAssemblyMachine` (the actual single-block assembler this report covers),
`TileEntityMachineAssemblyFactory` (a large multiblock, 764 lines, built on
`TileEntityProxyDyn.IProxyDelegateProvider`), and `TileEntityMachineMissileAssembly` (extends plain
`TileEntity`, not `TileEntityMachineBase`, and its recipe inputs are `ItemMissile`/`ItemCustomMissile`
weapon parts). The task names "assembler" singular; `TileEntityMachineAssemblyMachine` is the one that
matches "assembler" as a basic Phase-2 processing machine. The other two are flagged in Deferred scope
below, not silently dropped.

## Headline finding

All four TEs are structurally simple (single-block, non-multiblock, `TileEntityMachineBase` subclass,
plain `ItemStackHandler` inventory, `IEnergyReceiverMK2` power) and their **TE-side logic itself is
fully portable now** — same shape Neo Edition already confirmed compiles against real NeoForge 1.21.1
(`MachineShredderBlockEntity`/`MachineAssemblyMachineBlockEntity` exist there, class-for-class ports of
CE's own TEs). What blocks all four from being real, compilable Phase 2 content in *this* port is not
their own logic but four shared prerequisites that don't exist here yet, all of which docs/phase0 and
docs/phase1 already flagged as forward references and none of which this report re-solves:

1. **No `com.hbm.tileentity` (or `com.hbm.blockentity`) package or base BlockEntity class exists in
   this port at all.** `docs/phase0/STATUS.md`'s "Open decisions" section explicitly calls this out as
   needing a decision "before Phase 2 block entities land" (CE has it under `com.hbm.tileentity`; Neo
   Edition renamed it to `com.hbm.blockentity`). This report does not resolve that decision — see Open
   questions.
2. **No `AbstractContainerMenu`/`Screen` framework exists in this port.** `docs/phase1/STATUS.md`
   already names this as a recommended "early Phase 2 task since machines need it too" — this survey
   confirms all four machines need it (each pairs a `Container*`/`GUI*` class in CE).
3. **No recipe/pool system exists in this port** (`com.hbm.inventory.RecipesCommon`,
   `com.hbm.inventory.recipes.loader.{GenericRecipe,GenericRecipes}`) — flagged by both
   `docs/phase0/STATUS.md` and `docs/phase1/items_machine.md`/`STATUS.md` as a known gap. The
   assembler depends on it directly (`GenericRecipe`/`AssemblyMachineRecipes.INSTANCE` +
   `com.hbm.modules.machine.{ModuleMachineBase,ModuleMachineAssembler}`); shredder/crystallizer/mixer
   do **not** depend on this system (see per-machine recipe shape below) but do need their own
   simpler, still-nonexistent recipe classes ported.
4. **`com.hbm.inventory.UpgradeManagerNT` does not exist in this port.** All three of
   assembler/crystallizer/mixer (not shredder) use it for their SPEED/POWER/OVERDRIVE/EFFECT upgrade
   slots. `ItemMachineUpgrade` itself already exists in the port (Phase 1's `items_machine` area), but
   the manager class that reads upgrade item levels out of inventory slots does not. This is a new,
   previously-uncounted gap this report is surfacing — not previously named in Phase 0/1 status docs.

None of this means "wait for another phase" in the PORT_SPEC sense — items 2-4 are Phase-2-internal
shared infrastructure (a menu framework, a recipe loader, an upgrade manager), not content that
belongs to a different phase. But they are cross-cutting prerequisites this file set alone cannot
close, and whichever Phase 2 package builds them first unblocks all four machines here (and probably
most of the rest of Phase 2's processing-machine roster).

## Per-machine detail

### 1. Shredder — `TileEntityMachineShredder` (335 lines)

**Shape**: `TileEntityMachineBase(30, false, true)` — 30-slot `ItemStackHandler`, no fluid wrapper,
energy wrapper enabled. Implements `ITickable, IGUIProvider, IEnergyReceiverMK2`. No upgrade slots, no
fluid tanks — the simplest of the four.

**Slots** (30 total):
- 0-8: 9 input slots (any item with a shredder recipe, not an `ItemBlades`).
- 9-26: 18 output slots (take-only, auto-filled by recipe result).
- 27: left blade (`ItemBlades`, degrades 1 durability point per full processing cycle on both blades).
- 28: right blade.
- 29: battery slot (`Library.isBattery`/`chargeTEFromItems` drains held batteries into `power`).

**Recipe shape**: `com.hbm.inventory.recipes.ShredderRecipes` — **not** the `GenericRecipe`/pool
system. A single `HashMap<ComparableStack, ItemStack>` (`shredderRecipes`), one input `ItemStack` ->
one output `ItemStack`, no fluid, no duration/power stored per-recipe (duration/power are hardcoded
constants on the TE itself, see below). `ShredderRecipes.registerDefaults()` hand-registers ~150
explicit recipes; `registerPost()` additionally auto-generates ingot/plate/nugget/ore/block/gem/dust ->
dust recipes by scanning `OreDictionary.getOreNames()` at startup (this port has no ore dictionary
equivalent yet — flagged as a soft dependency, not re-solved here). Lookup: `getShredderResult(stack)`
via `ComparableStack` equality (item + meta, wildcard-meta fallback), defaulting to `ModItems.scrap` on
a miss (not "no recipe" — always something comes out).

**Power draw**: `maxPower = 10_000` HE (constant, `static final`). Drains 5 HE/tick while processing
(`power -= 5` each of the fixed `processingSpeed = 60` ticks/cycle → 300 HE per full item cycle). No
speed/power upgrades apply (no `UpgradeManagerNT` field on this TE at all — CE never gave the shredder
an upgrade path).

**GUI** (`ContainerMachineShredder`/`GUIMachineShredder`, canvas 176x233): input 3x3 grid at
(44,18)-(80,54) step 18; output 6x3 grid at (116,18)-(152,108) step 18; left/right blade slots at
(44,108)/(80,108); battery slot at (8,108); power bar drawn at (guiLeft+8, guiTop+18) 16x88 (vertical
fill); progress bar drawn at (guiLeft+63, guiTop+89) width scaled 0-34px; standard player inventory
starting y=151/209 (3 rows + hotbar).

### 2. Assembler — `TileEntityMachineAssemblyMachine` (544 lines)

**Shape**: `TileEntityMachineBase(0, true, true)` — inventory built manually as a 17-slot
`ItemStackHandler` (fluid + energy wrappers both enabled). Implements a much larger interface set than
the other three: `ITickable, IEnergyReceiverMK2, IFluidStandardTransceiverMK2, IUpgradeInfoProvider,
IControlReceiver, IGUIProvider, IConnectionAnchors, IRORValueProvider, IRORInteractive`. Owns 2
`FluidTankNTM` (input/output, 4000mB each) and a `ModuleMachineAssembler` (see below) that does the
actual recipe/progress bookkeeping — the TE itself is mostly wiring + arm-animation state
(`AssemblerArm[2]`, purely cosmetic, safe to drop/stub without gameplay impact).

**Slots** (17 total): 0 battery; 1 blueprint (`ModItems.blueprints`, selects which `GenericRecipe` this
machine executes — `receiveControl` sets `assemblerModule.recipe` by name from a client GUI packet); 2-3
two `ItemMachineUpgrade` slots; 4-15 twelve recipe-input slots (`itemInput(4)` in the constructor: slots
4 through 4+12-1); 16 one recipe-output slot (`SlotCraftingOutput`-style, take-only).

**Recipe shape**: `com.hbm.inventory.recipes.AssemblyMachineRecipes extends GenericRecipes<GenericRecipe>`
— this machine is the one of the four that genuinely needs the full `RecipesCommon`/`GenericRecipe`/
`GenericRecipes` loader/pool system (item#1 in Headline finding). A `GenericRecipe` here carries: up to
12 `RecipesCommon.AStack[] inputItem` (item OR ore-dict entries, with counts), 1 `FluidStack[]
inputFluid`, 1 `IOutput[] outputItem` (chance-based output wrapper, not a plain `ItemStack`), 1
`FluidStack[] outputFluid`, `int duration`, `long power` (both read by `ModuleMachineAssembler`/
`ModuleMachineBase.canProcess`/`process`), plus pool/localization metadata (`setPools`,
`autoSwitchGroup`) irrelevant to gameplay logic. Recipe selection is by string name
(`assemblerModule.recipe`, a `GenericRecipe.getInternalName()`), looked up in
`AssemblyMachineRecipes.INSTANCE.recipeNameMap` — the player picks the recipe via a client GUI
dropdown/button that fires `IControlReceiver.receiveControl` with an `index`/`selection` NBT payload.
**Cross-check against Neo Edition**: `upstream/neo-edition/src/main/java/com/hbm/inventory/recipes/
AssemblyMachineRecipes.java` (248 lines) still uses the exact same `GenericRecipe`/`GenericRecipes`
class shapes (`.setup(duration, power).outputItems(...).inputItems(new ComparableStack(...))`), just
with NeoForge `DeferredHolder`-based item references substituted for CE's static `ModItems` fields — it
has **not** been rewritten as datapack-JSON `Recipe<?>` despite that being this port's own stated
ground-rule goal. This is a real, confirmed data point: even the more-complete reference port punted on
the JSON-recipe rewrite and kept the hardcoded loader system verbatim. Whoever owns the recipe/pool
package should treat "port `RecipesCommon`+`GenericRecipe`+`GenericRecipes` as plain Java classes
first, JSON `Recipe<?>` conversion later if ever" as the de-risked default, not "must be JSON from day
one" — but that call belongs to whichever Phase 2 package actually owns the recipe system, not this
report.

**Power draw**: `maxPower` starts at `100_000` but is **recipe-driven**: `this.maxPower = recipe.power *
100` when a recipe is selected, floored at `100_000` via `BobMathUtil.max(power, maxPower, 100_000)`.
Actual per-tick consumption/speed is computed by `ModuleMachineBase`/`ModuleMachineAssembler` from
`UpgradeManagerNT` levels (SPEED +33%/level up to +100% speed and -25%/level power via POWER upgrade,
OVERDRIVE stacks +1x speed and +10/3x power per level, both capped at level 3) — this is the general
upgrade-scaling pattern shared with crystallizer/mixer below, all gated on the not-yet-ported
`UpgradeManagerNT` (item #4 in Headline finding).

**GUI** (`ContainerMachineAssemblyMachine`, no fixed canvas size read but standard `ContainerBase`
pattern): battery slot at (152,81); blueprint slot at (35,126); 2 upgrade slots at (152,108)/(152+18,108)
via `addSlots(assembler, 2, 152, 108, 2, 1)`; 12 input slots as a 4-col x 3-row grid from (8,18) via
`addSlots(assembler, 4, 8, 18, 4, 3)`; 1 output slot (`SlotCraftingOutput`) at (98,45); player inventory
at (8,174).

### 3. Crystallizer — `TileEntityMachineCrystallizer` (484 lines)

**Shape**: `TileEntityMachineBase(0, true, true)` — manually built 8-slot `ItemStackHandler`. Implements
`ITickable, IEnergyReceiverMK2, IFluidStandardReceiver, IGUIProvider, IFFtoNTMF, IClimbable,
IUpgradeInfoProvider, IFluidCopiable, IConnectionAnchors`. Owns one `FluidTankNTM tankNew` (8000mB,
default-filled `Fluids.PEROXIDE`) plus a legacy `net.minecraftforge.fluids.FluidTank tank` used only for
a one-time migration path (`converted`/`convertAndSetFluid` — CE's own old-fluid-system -> FluidTankNTM
migration; **not** relevant to this port since there is no legacy state to migrate from, safe to drop
entirely rather than port). `IClimbable`: the crystallizer's tower model doubles as a climbable ladder
in-world (`getLadderAABB`/`isEntityInClimbAABB`) — a real but easy-to-miss gameplay behavior tied to
this specific block's shape, not just decoration.

**Slots** (8 total): 0 item input; 1 battery; 2 item output; 3-4 two fluid-loading slots (`tankNew.
loadTank(3, 4, inventory)` — items that fill/drain the tank, e.g. buckets/cells); 5-6 two upgrade slots;
7 `IItemFluidIdentifier` fluid-type slot (`tankNew.setType(7, inventory)` — an item that tells the tank
what fluid it should currently be tracking, used for empty-tank fluid selection UI).

**Recipe shape**: `com.hbm.inventory.recipes.CrystallizerRecipes` — its own bespoke class, not
`GenericRecipe`. Keyed by `HashMap<Tuple.Pair<Object, FluidType>, CrystallizerRecipe>` where the `Object`
half of the key is either a `RecipesCommon.ComparableStack` or an ore-dict name `String` (see
`registerRecipe` overloads) and the `FluidType` half is the required acid/reagent fluid type in
`tankNew`. `CrystallizerRecipe` itself: `ItemStack output`, `int duration`, `int itemAmount` (input
count required, default 1), `int acidAmount` (fluid consumed, default 500mB, settable via constructor
overload), `float productivity` (0-1, EFFECT-upgrade "free output chance" via `.prod(x)`). Lookup is
`CrystallizerRecipes.getOutput(ItemStack, FluidType)`, returning `null` on no match (unlike shredder,
there is a real "no recipe" state here). ~50 hardcoded recipes registered directly in
`registerDefaults()` (ore -> crystal chains, mostly `+ FluidStack(SULFURIC_ACID, 500)`, plus several
one-off item transmutations).

**Power draw**: `maxPower = 1_000_000` (constant). `demand = 1000` HE/tick base, scaled by upgrades:
`getPowerRequired() = demand + speedLevel*demand + effectLevel*demand*2` (both capped at level 3, so up
to `1000 + 3000 + 6000 = 10_000` HE/tick at max upgrades). `getDuration()` reduces the recipe's base
duration by up to 75% at SPEED 3 (`base * max(1 - 0.25*speed, 0.25)`). `getCycleCount()` (an OVERDRIVE
effect) runs the entire `canProcess()`/`process()` step up to 7 times per world tick, i.e. multiplies
both throughput and power draw and duration-tick-down simultaneously — this compounds with the
duration reduction above in a way worth flagging for balance-preserving parity (not a bug, just a
detail a careless re-derivation could silently drop half of).

**GUI** (`ContainerCrystallizer`, canvas dims not shown in the container itself but `GUICrystallizer`
presumably matches CE's asset — not read in this pass, slot coordinates below are authoritative
regardless): item input at (62,45); battery at (152,72); item output at (113,45); fluid-load slots at
(17,18) and (17,54); 2 upgrade slots at (80,18)/(98,18); fluid-identifier slot at (35,72); player
inventory 3 rows from y=122 + hotbar at y=180.

### 4. Mixer — `TileEntityMachineMixer` (387 lines)

**Shape**: `TileEntityMachineBase(0, true, true)` — manually built 5-slot `ItemStackHandler`. Implements
`IControlReceiver, ITickable, IGUIProvider, IFluidStandardTransceiver, IEnergyReceiverMK2,
IUpgradeInfoProvider, IFluidCopiable, IConnectionAnchors`. Owns 3 `FluidTankNTM`: `tanks[0]`/`tanks[1]`
(16000mB each, the two fluid reagent inputs) and `tanks[2]` (24000mB, the single fluid output, its type
locked by an `IItemFluidIdentifier` item in slot 2).

**Slots** (5 total): 0 battery; 1 solid-ingredient input (optional per-recipe, checked via
`AStack.matchesRecipe`); 2 fluid-output-identifier item slot (`tanks[2].setType(2, inventory)`); 3-4 two
upgrade slots.

**Recipe shape**: `com.hbm.inventory.recipes.MixerRecipes` — its own bespoke class, keyed by
`HashMap<FluidType, MixerRecipe[]>` — **the output fluid type is the map key**, and each output fluid
type maps to an *array* of alternative recipes (see e.g. `Fluids.FRACKSOL` having 2 competing recipes
in the sample read). `MixerRecipe`: `FluidStack input1`, `FluidStack input2` (either may be null —
single-reagent recipes exist, e.g. `Fluids.FISHOIL` needs only a solid input), `AStack solidInput`
(may be null — pure-fluid recipes exist), `int processTime`, `int output` (mB of the keyed fluid
produced). When multiple recipes share an output fluid, `TileEntityMachineMixer.recipeIndex` (a
player-cyclable index, advanced by `IControlReceiver.receiveControl`'s `"toggle"` field) selects which
one is currently "loaded" — `canProcess()` re-derives `tanks[0]`/`tanks[1]`'s expected type from the
currently-selected recipe every tick (`tanks[0].setTankType(recipe.input1.type)`), so switching the
recipe index effectively reconfigures what the two input tanks will accept.

**Power draw**: `maxPower = 10_000` (constant). `consumption` (per-tick, recomputed every tick, not
cached across ticks like the other three): base `50`, `+= speedLevel * 150`, then
`-= consumption * powerLevel * 0.25` (POWER upgrade discount applied *after* the speed surcharge, so
it discounts the speed-inflated total, not just the base), then `*= (overLevel * 3 + 1)` (OVERDRIVE is
a multiplier applied last, after both other adjustments) — order of operations here is load-bearing for
parity and easy to get subtly wrong in a naive re-derivation. `processTime` similarly gets
speed-reduced (`-= processTime * speedLevel / 4`) then overdrive-divided (`/= (overLevel + 1)`), floored
at 1 tick.

**GUI** (`ContainerMixer`, canvas dims not read but layout is authoritative): battery at (23,77); item
input at (43,77); fluid-identifier slot at (117,77); 2 upgrade slots at (137,24)/(137,42) — stacked
vertically, not side by side like the other three machines; player inventory 3 rows from y=122 + hotbar
at y=180.

## Phase-2-safe scope

Fully portable now, in the sense of "no further design decisions needed, only the 4 shared
prerequisites in Headline finding above":

- **4 TE classes**: the shredder/assembler/crystallizer/mixer tick/process/serialize logic itself,
  confirmed by a real prior port (Neo Edition's `MachineShredderBlockEntity`/
  `MachineAssemblyMachineBlockEntity`, read for API shape only, never for behavior — CE remains the
  sole behavior source and every number/formula above was extracted from CE, not Neo Edition).
- **`IEnergyReceiverMK2`/`IEnergyHandlerMK2` usage**: all four already implement interfaces that exist
  unchanged in this port's `com.hbm.api.energymk2` package (`IEnergyReceiverMK2`,
  `IEnergyHandlerMK2.getPower/setPower/getMaxPower`) — zero new energy-API work needed, this is a
  "just call it" case, not a design gap.
- **`FluidTankNTM`-based tanks** (assembler 2, crystallizer 1, mixer 3): the port's own
  `com.hbm.inventory.fluid.{FluidType,FluidStack,Fluids}` + `trait/FT_*` classes already exist, but
  `com.hbm.inventory.fluid.tank.FluidTankNTM` itself does **not** exist yet in this port (confirmed by
  directory listing — only `FluidStack.java`/`FluidType.java`/`Fluids.java`/`trait/` are present under
  `com.hbm.inventory.fluid/`, no `tank/` subpackage). This is the same gap `docs/phase0/STATUS.md`'s
  build-verification section already names (`com.hbm.inventory.fluid.tank.FluidTankNTM` in its 100-error
  triage list) — not new, but directly relevant here since 3 of these 4 machines need it.
- **Shredder/Crystallizer/Mixer recipe classes**: `ShredderRecipes`/`CrystallizerRecipes`/
  `MixerRecipes` are each small, self-contained, and do **not** depend on the big `GenericRecipe`/
  `GenericRecipes` loader system — they can be ported as plain static-HashMap classes independently of
  whoever solves the assembler's `GenericRecipe` dependency. `ShredderRecipes.registerPost()`'s ore-dict
  auto-generation is the one piece worth flagging: this port has no confirmed ore-dictionary equivalent
  yet (out of scope for this report to solve; note only).
- **GUI slot layouts** (all 4, exact coordinates above): pure data, portable into whatever `Slot`/
  `AbstractContainerMenu` subclass Phase 2's menu-framework package produces, once that framework
  exists.

## Deferred scope

- **The 4 shared prerequisites in Headline finding** — no BlockEntity base/package convention, no
  Menu/Screen framework, no recipe/pool system (`RecipesCommon`/`GenericRecipe`/`GenericRecipes`,
  needed by the assembler specifically), no `UpgradeManagerNT` (needed by
  assembler/crystallizer/mixer, not shredder). These block *compiling* all four TEs, not just
  polishing them — whichever Phase 2 package builds these should probably be scheduled before or
  alongside this file set's own implementation, not after.
- **`FluidTankNTM`** — needed by 3 of 4 machines, already a named Phase 0 gap, not re-solved here.
- **`TileEntityMachineAssemblyFactory`** (764 lines, multiblock, `TileEntityProxyDyn`) — excluded from
  this report's scope per the task's own "assembler" naming (see Directory survey note above). Belongs
  with whichever Phase 2 package owns multiblock/proxy machines, since it needs
  `com.hbm.handler.MultiblockHandlerXR`/`MultiblockBBHandler` (both already flagged missing by
  `docs/phase1/STATUS.md`/`docs/phase0/STATUS.md`) in addition to everything this file needs.
- **`TileEntityMachineMissileAssembly`** — excluded for the same naming reason; also weapon-content
  (`ItemMissile`/`ItemCustomMissile` parts), so belongs with Phase 3 weapons regardless of naming.
- **`ModuleMachineAssembler`/`ModuleMachineBase`** (the assembler's recipe-processing helper,
  `com.hbm.modules.machine`) — depends on `GenericRecipe`/`GenericRecipes`, so blocked on the same
  recipe-system prerequisite as the assembler TE itself; not separately blocked on anything new.
- **Assembler's arm-animation state** (`AssemblerArm[2]`, ring rotation, striker sound timing) — purely
  cosmetic client-side flourish, zero gameplay effect (`didProcess` gates it, nothing reads it back).
  Safe to defer to a client/rendering-focused pass without blocking the machine's actual function;
  noted so it isn't mistaken for load-bearing logic when someone reads the 544-line source file.
  Belongs with whichever Phase 2 or Phase 5 package handles block-entity renderers
  (`BlockEntityRenderer`), since CE's version is tied to a custom OBJ-model rig, not a data value the
  TE itself needs to reproduce for correctness.
- **Crystallizer's legacy `FluidTank`/`converted` migration path** — CE-specific save-upgrade shim with
  no equivalent prior state in this port; recommend dropping entirely rather than porting (noted above,
  repeating here for the deferred-scope list's completeness).
- **Ore-dictionary auto-generation** (`ShredderRecipes.registerPost()`) — depends on an ore-dict
  equivalent this port doesn't appear to have; the ~150 explicit `registerDefaults()` recipes do not
  depend on it and are fully portable standalone.
- **JEI/recipe-viewer integration** (`getShredderRecipes()`, `printNEIExtras()`, `jeiCrystalRecipes`) —
  1.12-era NEI/JEI display hooks, not gameplay logic; out of scope for a NeoForge 1.21.1 port unless a
  later phase decides to add a REI/JEI-equivalent recipe-viewer mod integration, which is a project-wide
  decision, not something to resolve per-machine.

## Key design/API decisions

Confirmed by reading real NeoForge 1.21.1 usage (Neo Edition reference, content/behavior *never* taken
from it) and this port's own existing scaffold:

- **BlockEntity registration already has a home**: `com.hbm.blocks.ModBlocks.BLOCK_ENTITY_TYPES` is a
  `DeferredRegister<BlockEntityType<?>> = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE,
  MainRegistry.MODID)`, added in Phase 0 and currently empty (0 entries). Confirmed real shape from Neo
  Edition's `NtmBlockEntityTypes`: `BLOCK_ENTITY_TYPES.register("machine_shredder", () ->
  BlockEntityType.Builder.of(MachineShredderBlockEntity::new, NtmBlocks.MACHINE_SHREDDER.get())
  .build(null))` — i.e. register by string id, factory reference + the block(s) it attaches to,
  `.build(null)` (null datafixer type, the standard no-DFU-type pattern). This port's 4 machines should
  register into the *existing* `ModBlocks.BLOCK_ENTITY_TYPES` field, not a new registry class.
- **No `com.hbm.tileentity`/`com.hbm.blockentity` package or base class exists in this port yet** —
  confirmed by directory listing (`find src -path "*com/hbm/tileentity*"` and
  `-iname "*blockentity*"` both came back empty except 4 unrelated simple blocks in
  `com.hbm.blocks.generic` that each inline their own tiny `BlockEntity` subclass with no shared base).
  This is exactly the gap `docs/phase0/STATUS.md`'s "Open decisions" section names as needing
  resolution "before Phase 2 block entities land" — **this report does not resolve it**, per the task's
  own instruction; see Open questions. Whatever the decision, these 4 machines are a representative
  test case for it: all four need a `TileEntityMachineBase`-equivalent shared base (inventory,
  power-charge-from-battery-slot helper, `networkPackNT`-equivalent sync, `IEnergyReceiverMK2`
  wiring) — Neo Edition's own answer (`com.hbm.blockentity.MachineBaseBlockEntity extends
  LoadedBaseBlockEntity implements WorldlyContainer, Nameable, MenuProvider, ITickable`, confirmed by
  reading the file) is a reasonable API-shape reference for what such a base class's surface looks like
  under real Mojang mappings (`WorldlyContainer` = the 1.21 name for CE's `ISidedInventory`-equivalent
  slot-access contract; `MenuProvider` supplies `createMenu`), but the package name/placement decision
  itself is explicitly out of this report's scope.
- **No `AbstractContainerMenu`/`Screen` framework exists in this port** — confirmed by grep (`extends
  AbstractContainerMenu` matches nothing in `src/`; the only 3 hits for the string `AbstractContainerMenu`
  are javadoc comments on `ItemBook`/`ItemGuideBook`/`IContainerOpenEventListener` explicitly
  documenting its absence as a known Phase 1 gap). This blocks all 4 machines' GUIs, not just these
  four — matches `docs/phase1/STATUS.md`'s own recommendation to build this "early" in Phase 2.
  Confirmed real API shape from Neo Edition (again, shape only): `MenuType<T>` registered via
  `DeferredRegister<MenuType<?>> = DeferredRegister.create(Registries.MENU, MODID)`, each entry as
  `MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory))` where `factory` is an
  `IContainerFactory<T>` (`(id, inventory, extraData) -> new FooMenu(id, inventory, extraData)`,
  reading a `BlockPos` off `extraData` to locate the block entity — the standard NeoForge "menu opened
  from a block" pattern) — and a shared `MenuBase<T extends Container>` superclass carrying
  `playerInv(...)`/`addSlots(...)`/`quickMoveStack` helpers that a from-scratch menu framework for this
  port would likely want an equivalent of, given all 4 of this report's machines (and presumably most
  of Phase 2) repeat the same "3 rows + hotbar player inventory" and "N slots in a rows x cols grid"
  boilerplate CE's own `ContainerBase`/manual loops already show.
- **Custom HE energy is confirmed to stay a plain interface, never a NeoForge `Capability<T>`** — grep
  for `RegisterCapabilitiesEvent` across the entire Neo Edition reference returns zero matches. All 4
  machines here implement `IEnergyReceiverMK2` directly on the `BlockEntity` and are queried by
  `instanceof` (see e.g. CE's own `IEnergyProviderMK2.tryProvide`: `if(targetBe instanceof
  IEnergyReceiverMK2 rec ...)`), never through a `Capability` lookup. This matches the port's ground
  rule and this port's own existing `com.hbm.api.energymk2` interfaces exactly as-is — no adaptation
  needed for these 4 machines beyond implementing the interfaces, which they already do in CE.
- **Recipe system**: confirmed (Neo Edition cross-check, `AssemblyMachineRecipes.java`) that even a
  more-complete reference port kept CE's hardcoded `GenericRecipe`/`GenericRecipes` Java-class loader
  verbatim rather than converting to datapack JSON `Recipe<?>`, despite PORT_SPEC's stated goal of JSON
  recipes. Recorded here as evidence for whoever makes that call, not a decision this report is making.
- **Fluid handling**: none of these 4 machines touch a world fluid block — `FluidTankNTM` is purely an
  internal TE-owned buffer moved machine-to-machine via `IFluidStandardTransceiverMK2`/
  `trySubscribe`/`sendFluid` network calls (the port's own fluid pipe-network abstraction, distinct from
  vanilla `Fluid`/`FluidState` world blocks). Confirmed no dependency on the not-yet-existing
  world-fluid-block system Phase 1 already flagged as absent.

## Open questions / risks

- **`com.hbm.tileentity` vs `com.hbm.blockentity` package-naming decision** (docs/phase0/STATUS.md
  "Open decisions") is a direct, blocking prerequisite for all 4 machines in this report — they cannot
  be given real class files until this is decided, since the decision determines both the package they
  live in and (if the port follows Neo Edition's split) whether a shared `MachineBaseBlockEntity`-style
  base class needs authoring from scratch or already has one to extend. This report surfaces the
  urgency (4 concrete files blocked on it right now) but does not make the call.
- **Menu/Screen framework**: same situation — a cross-cutting Phase 2 prerequisite, not something to
  build 4 separate one-off times for these machines. Recommend whichever package builds it takes these
  4 GUIs' slot layouts (fully documented above) as an early real-world test of the framework's
  "N slots in a grid" / "player inventory" helpers, mirroring Neo Edition's `MenuBase` shape.
- **`UpgradeManagerNT` port**: not flagged anywhere in Phase 0/1 status docs before this report. It's a
  small, self-contained class (reads `ItemMachineUpgrade.UpgradeType` + level out of 1-2 inventory
  slots) with no further hidden dependencies observed in the 3 files that use it — recommend treating
  it as a quick, low-risk win to close alongside `FluidTankNTM`, not as a phase-blocking risk in its own
  right.
- **Mixer's per-output-fluid recipe *array*** (`MixerRecipe[]`, player-cyclable via `recipeIndex`) is
  an easy detail to flatten incorrectly into a single-recipe-per-fluid model during porting — worth a
  reviewer's explicit attention when this machine is implemented, since CE's own `Fluids.FRACKSOL`
  entry (2 competing recipes) is the only one in the sampled recipe list that would silently break if
  simplified.
- **Crystallizer's `getCycleCount()` x `getDuration()` interaction** (OVERDRIVE upgrade running the
  entire process step up to 7x per tick, compounding with SPEED's separate duration reduction) is the
  kind of formula that's easy to re-derive "cleaner" and accidentally change balance — flagged above,
  repeating here as a reviewer risk, not a design gap.
- **Ore-dictionary equivalent**: `ShredderRecipes.registerPost()`'s auto-generated ingot/plate/nugget/
  ore/block/gem/dust recipes depend on `OreDictionary.getOreNames()`. Whether this port has (or intends
  to build) any ore-dict-equivalent tag-based lookup wasn't confirmed in this pass — worth a follow-up
  check by whoever implements `ShredderRecipes`, since without it roughly half of CE's real shredder
  recipe coverage (every modded-ore-compatible entry) would silently not exist.
