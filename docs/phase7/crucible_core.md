
# Crucible-core research report (Phase 7, `crucible-core`)

Scope: CE's Crucible **machine** (block/tile-entity/GUI/container/item/recipe-logic), *not* the
MatDistribution material-yield data (separate task `crucible-matdistribution` — noted only at its
integration point below, not duplicated).

## Scope confirmed

Files read in full, with exact CE line counts:

| File | Lines | In-CE structure |
|---|---:|---|
| `upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityCrucible.java` | 675 | One concrete tile-entity class, no sub-methods delegated to a table — `update()` (main tick loop, ~140 lines) calls `tryPullHeat()`, `trySmelt()`, `tryRecipe()`, then two near-identical inline pour blocks (waste, then recipe-stack). Everything else (`isItemSmeltable`, `canAcceptPartialPour`/`pour` for `ICrucibleAcceptor`, NBT/network (de)serialize, `getQuantaFromType` overloads) is flat, linear Java — no loop-over-table codegen anywhere in this file. |
| `upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/MachineCrucible.java` | 199 | One `BlockDummyable` subclass. 5 hand-authored `AxisAlignedBB`s define the basin+rim hitbox (not generated). `createNewTileEntity` branches on `meta >= 12` (core vs. dummy) — same convention this port's own `BlockDummyable` subclasses already use (see below). |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerCrucible.java` | 89 | One `Container` subclass + one nested `SlotOneItem` (stack-limit-1 `Slot` subclass). Plain slot-add loop (3×3), no recipe logic. |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUICrucible.java` | 144 | One `GuiInfoContainer` subclass. Rendering only — no recipe/mechanic logic, just reads `crucible.recipeStack`/`wasteStack`/`progress`/`heat` and CE's generic `GUIScreenRecipeSelector` for the recipe-picker popup. |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CrucibleRecipe.java` | 65 | One data class extending CE's `GenericRecipe`: `MaterialStack[] input`, `MaterialStack[] output`, `int frequency`, builder-style setters, a `print()` for tooltip/GUI text. |
| `upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CrucibleRecipes.java` | 275 | One `GenericRecipes<CrucibleRecipe>` singleton. **`registerDefaults()` is a flat list of 13 real `this.register(new CrucibleRecipe(...)...)` calls** (not a loop over a material table — see catalog below; 4 more calls are commented out, and one more `this.register(...)` at line 161 is inside `readRecipe`, the JSON-loader path, not a static default). Plus JSON read/write (`readRecipe`/`writeRecipe`, for CE's external `hbmCrucible.json` recipe-override file), a JEI-only `getSmeltingRecipes()` helper, and a JEI-only `registerMoldsForJEI()`/`getMoldRecipes()` pair (cross-product of every SMELTABLE material × every `ItemMold.Mold` — this is display data for the *separate* Foundry mold-casting block family, not consumed by `TileEntityCrucible` itself; see "Open questions"). |
| `upstream/hbm-ce/src/main/java/com/hbm/util/CrucibleUtil.java` | 167 | Static utility class, 5 methods: `pourSingleStack`/`pourFullStack` (hitscan-based pour dispatch), `tryPourStack`, `getPouringTarget` (raytrace + `ICrucibleAcceptor` cast), `spill` (2 overloads, the "no valid target" fallback). No state, no loop-over-table. |
| `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemCrucible.java` | 207 | One `ItemSwordCutter` subclass — **a hand melee weapon, unrelated to the machine** (see below). |
| `upstream/hbm-ce/src/main/java/com/hbm/api/block/ICrucibleAcceptor.java` (read for completeness, referenced by every file above) | 26 | Interface: `canAcceptPartialPour`/`pour` (vertical pouring contact) + `canAcceptPartialFlow`/`flow` (horizontal channel transfer, unused by the Crucible itself — `MachineCrucible`/`TileEntityCrucible` both hardcode `flow`→`null`/`false`). |

**A discrepancy from the assignment brief worth flagging up front**: the brief says "catalog every
one of its ~24 recipe entries in full." A direct read of `CrucibleRecipes.registerDefaults()` finds
**13 real, active `CrucibleRecipe` registrations**, not 24 (grep for
`this.register(new CrucibleRecipe` returns 17 hits total, but 4 are commented-out GregTech-compat
variants — `crucible.steelWrought`/`steelPig`/`steelMeteoric`, guarded by a commented-out
`Compat.isModLoaded(Compat.MOD_GT6)` — and 1 more is the generic JSON-loader's `readRecipe` template
call, not a static entry). §"Full recipe catalog" below catalogs all 13 real entries plus the 4
commented-out ones for completeness. The 275-line file count and the file itself match the
assignment exactly; only the entry count differs from the brief's estimate.

### CE mechanics walkthrough (heat, melt queue, casting, GUI) — read in full from `TileEntityCrucible.java`/`MachineCrucible.java`/`GUICrucible.java`/`CrucibleUtil.java`

**Block shape**: `MachineCrucible extends BlockDummyable`. `getDimensions()` returns `{1,0,1,1,1,1}`
(`[UP, DOWN, FORWARD, BACKWARD, LEFT, RIGHT]` dummy-block counts per this port's own
`BlockDummyable#getDimensions` javadoc — i.e. a 3×2×3 footprint: core + 1 dummy up, 0 down, 1 each
forward/backward/left/right), `getOffset()` returns `1`. Only the meta-≥12 "core" position gets a
real tile entity (`TileEntityCrucible`); every other cell in the footprint gets CE's generic
`TileEntityProxyCombo` (this port's already-established convention for the same case — see
`ElectrolyserBlock.newBlockEntity` below — is simpler: `newBlockEntity` returns `null` for
non-core positions, no proxy TE at all). 5 hand-authored `AxisAlignedBB`s form the hitbox: a 3×3
basin floor (Y 0→0.5) plus 4 raised rim-wall segments (Y 0.5→1.5) on each side, open at the center —
a literal cauldron/basin shape.

**Heat** (`tryPullHeat()`, called every server tick): if `heat >= maxHeat` (default 100,000), no-op.
Otherwise reads the block directly below the core; if it implements `IHeatSource`
(`getHeatStored()`/`useUpHeat(int)`), pulls `min(source.heat - this.heat, maxHeat - this.heat)`
scaled by `diffusion` (default 0.25) from it each tick. **If nothing below implements
`IHeatSource`, or the source has less heat than the crucible, heat instead passively decays**:
`heat -= max(heat/1000, 1)` per tick (slow cooldown, floor 0). This port already has the
`IHeatSource` interface (`src/main/java/com/hbm/api/tile/IHeatSource.java`, 12 lines, unchanged
2-method contract) but **zero implementers anywhere in this port** — no heater/firebox/electric-heat
block entity exists yet (CE's own implementers: `TileEntityHeaterOilburner`,
`TileEntityFireboxBase`, `TileEntityHeaterHeatex`, `TileEntityHeaterElectric`, none ported). A ported
Crucible will compile and sit at heat 0 (fully inert) until *some* `IHeatSource` block entity exists
to place beneath it — a real, separate follow-on dependency, not a blocker to porting the Crucible
class itself.

**Smelting items into MaterialStacks** (`trySmelt()`): gated on `heat >= maxHeat/2` (half-heat
floor). Finds the first non-empty smeltable item among input slots 1-9 (slot 0 of the TE's 10-slot
inventory is never exposed by the container — dead slot). Processing rate scales with heat:
`delta = (heat - maxHeat/2) * 0.05`; `progress += delta; heat -= delta` — so smelting *consumes*
heat as it goes, and higher heat above the half-threshold smelts faster. When
`progress >= processTime` (config default 20,000), resets progress, calls
`Mats.getSmeltingMaterialsFromItem(stack)` — **this exact call is already implemented and wired in
this port's `Mats.java`** (see "Already covered" below) — converting the item into a
`List<MaterialStack>`. Each resulting material is routed to `recipeStack` if a recipe is loaded and
that material is part of the loaded recipe's *input or output* list (letting players pour recipe
output back in without wasting it), otherwise to `wasteStack`; with no recipe loaded, everything
goes to `recipeStack` unless `ServerConfig.LEGACY_CRUCIBLE_RULES` is true (in which case it all goes
to waste). **`ServerConfig.LEGACY_CRUCIBLE_RULES` already exists in this port**, same name, same
comment, same default `false` (`src/main/java/com/hbm/config/ServerConfig.java:31,52`) — a ready-made
hook. The consumed item shrinks by 1.

**Alloying/conversion** (`tryRecipe()`, runs every tick but only *fires* once every
`recipe.frequency` world-ticks — CE's data uses 2-20, i.e. roughly 0.1-1s cadence, a steady-state
throughput rate, not a one-shot craft): requires the *entire* recipe input simultaneously present in
`recipeStack` (any shortfall on any input material skips the whole tick, no partial consumption).
When satisfied, subtracts each input's exact amount from the matching `recipeStack` entries and adds
each output's exact amount (creating a new stack entry if not already present). Because this fires
repeatedly as long as enough input remains, a single "batch" keeps converting continuously — this is
architecturally a small rate-limited reactor loop, not a discrete crafting-table transform.

**Casting / how output items actually form**: **there is no mold slot inside the Crucible TE
itself.** Material becomes a real obtainable item only by being *poured out* into the world, where a
separate `ICrucibleAcceptor` block below (CE's Foundry mold/basin block family — see "Open
questions") intercepts the stream and performs mold-based casting. Every server tick, if
`wasteStack` is non-empty, `CrucibleUtil.pourFullStack` hitscans straight down (range 6, `safe=true`)
in the direction *opposite* the block's placement facing, quanta-capped at `NUGGET.q(3)` per tick,
firing a "Foundry" particle effect tinted by the poured material's `moltenColor` on a hit. If
`recipeStack` is non-empty, the same mechanism fires in the block's *own* facing direction — but
filtered: if a recipe is loaded, only materials matching that recipe's *output* list are eligible to
pour (unconsumed input material stays put); with no recipe loaded, everything pours freely.
**`safe=true` means a miss (no `ICrucibleAcceptor` below) silently retains the material rather than
losing or crashing it** — the graceful-degradation path this port would inherit for free even before
any Foundry block exists downstream. Every pour attempt (successful or not) also increments SOOT
pollution via `PollutionHandler.incrementPollution` (**`PollutionHandler` with the exact
`SOOT_PER_SECOND` constant and `incrementPollution` method already exists in this port**,
`src/main/java/com/hbm/handler/pollution/PollutionHandler.java`).

**Overflow hazard**: every tick, `(recipeStack+wasteStack total mass) / (recipeZCapacity +
wasteZCapacity)` scaled by 0.875 defines a "lava column" height above the core; any
`EntityLivingBase` standing in that column (1-block XZ radius) takes 5 `DamageSource.LAVA` damage +
5s fire — a real environmental hazard tied to how full/actively-pouring the crucible is, worth
preserving.

**Shovel-scoop interaction**: sneak + right-click with any tool tagged `"shovel"` on any part of the
multiblock dumps *both* `recipeStack` and `wasteStack` into the player's inventory (or drops as
`EntityItem`) as `ItemScraps.create(new MaterialStack(...))` stacks, then clears both stacks. Same
behavior fires on block break (`breakBlock`).

**GUI/slot layout** (canvas 176×214, `GUICrucible`/`ContainerCrucible`): 9 **stack-limit-1**
(`SlotOneItem`) slots, 3×3 grid at `(107+18c, 18+18r)`, backing TE inventory slots 1-9. Player
inventory: 3 rows at `y=132+18r`, hotbar at `y=190`. Two hover-tooltip zones — waste breakdown at
`(16,17)` and recipe/input-stack breakdown at `(61,17)`, both 36×81, listing each `MaterialStack`'s
translated name + `Mats.formatAmount` (ingots/nuggets/quanta, or raw mB with shift held — **this
exact formatter already exists**, `Mats.formatAmount`, ported verbatim). An 18×18 clickable zone at
`(106,80)` opens CE's generic `GUIScreenRecipeSelector` (331 lines, shared by every
`GenericRecipes`-based machine, not ported anywhere in this port yet) to pick/clear the loaded
recipe by name, sent server-side via `receiveControl`/`IControlReceiver` (index 0 = recipe-name
string) — **this port already has `IControlReceiver`**
(`src/main/java/com/hbm/interfaces/IControlReceiver.java`) with a real precedent implementer
(`MachineMixerBlockEntity`). Hovering (not clicking) the same zone instead shows the loaded recipe's
full input/output printout via `CrucibleRecipe.print()`, or a "click to set recipe" hint. Progress
bar: 33px horizontal fill at `(126,82)`. Heat bar: 33px horizontal fill at `(126,91)`. Two vertical
"tank" fill bars drawn directly on the machine's own texture (not a generic tank widget) at
`(62,97)`/`(17,97)` for recipeStack/wasteStack, 34px wide × up to 79px tall, stacked bottom-up per
material layer with a distinct texture offset for ADDITIVE-behavior materials (u=210) vs. normal
(u=176).

### `ItemCrucible` — confirmed a distinct weapon, unrelated to the machine mechanic

Per the assignment's explicit question: **`ItemCrucible` (`items/weapon/ItemCrucible.java`) is a
melee weapon item — a charge-up "blender" sword** (`ItemSwordCutter` subclass), not any part of the
smelting/casting machine. It shares only the "crucible" name, a config namespace
(`GeneralConfig.crucibleMaxCharges`), and some particle/sound flavor (lightning particles, a
"blender" burst on a charged kill) with the machine — zero code-level connection (no reference to
`TileEntityCrucible`, `CrucibleRecipe(s)`, `Mats`, or `ICrucibleAcceptor` anywhere in the CE file).
Mechanically: full attack damage + normal movement speed while charged (`charges > 0`, max 16),
degrades to flat 5 damage + 0.8× movement once fully discharged (1 charge lost per successful hit);
recharged externally by CE's own crafting recipe (out of this class's own scope either way).
**This item is already fully ported in this port**, at
`src/main/java/com/hbm/items/weapon/ItemCrucible.java` (206 lines) — see "Already covered" below.

## Already covered by this port

This is the single most consequential finding of this task: **substantially more of the Crucible
ecosystem's *scaffolding* is already built than the phase docs suggest, even though the machine
itself is 100% unported.** Concretely, already committed and confirmed correct:

1. **`ItemCrucible` (the hand weapon) is fully ported**, `src/main/java/com/hbm/items/weapon/ItemCrucible.java`
   (206 lines) — charge-dependent attack/movement via live `AttributeInstance` modifiers (a
   documented, deliberate mechanism change from CE's per-query API, same net behavior), on-kill
   redstone-dust burst, durability-bar charge display, tooltip charge pips. **Nothing further to do
   here** — do not re-port or duplicate this item; it is orthogonal to the machine.
2. **`Mats.java`'s own javadoc already documents deferring the Crucible/MatDistribution port**
   (lines 29-41, quoted by both phase6 docs): `materialEntries`/`materialOreEntries` are "simply
   empty" pending a future MatDistribution pass; `registerEntry`/`registerOre` exist as "the stable
   seam a future... port calls into." **This task confirms that seam is not just declared but
   already wired end-to-end on the consuming side**: `Mats.getMaterialsFromItem(ItemStack)` (lines
   252-280) already checks `materialOreEntries` (by tag path), a tag→material reverse index built at
   class-init from every autogen shape's common tag, `materialEntries` (by exact `Item`), and a
   `specialCaseResolvers` extension list — and `Mats.getSmeltingMaterialsFromItem(ItemStack)` (the
   exact method `TileEntityCrucible.trySmelt()` needs to call) already exists and correctly applies
   each material's `smeltsInto`/`convIn`/`convOut` conversion. **The Crucible TE's smelting-input
   call site (`Mats.getSmeltingMaterialsFromItem(stack)`) can be ported completely unchanged** — it
   will simply return an empty list for every item until `crucible-matdistribution` populates the two
   maps, which is the correct, already-designed-for degradation (matches CE's own "no materials
   found → don't smelt" `isItemSmeltable` early return).
3. **`ServerConfig.LEGACY_CRUCIBLE_RULES`** (boolean, default false, same key/comment as CE) and
   **`GeneralConfig.CRUCIBLE_MAX_CHARGES`** (int, default 16, same CE config-key comment) already
   exist — both config surfaces `TileEntityCrucible`/`ItemCrucible` need are pre-wired.
4. **`Mats.MaterialStack`, `NTMMaterial`, `MaterialShapes`** — the entire material/shape data model
   `CrucibleRecipe`/`CrucibleRecipes`/`TileEntityCrucible` are built on is a confirmed near-verbatim
   port of CE's own (`MaterialShapes.java` javadoc states this explicitly), with all ~90 `MAT_*`
   constants, all autogen shapes (`NUGGET.q(1)=8`, `INGOT.q(1)=72`, etc.), and
   `Mats.formatAmount(int, boolean)` (the exact GUI amount-formatter CE's `GUICrucible`/
   `CrucibleRecipe.print()` use) already present and matching CE 1:1.
5. **`BlockDummyable`** (482 lines) is a complete, faithful port of CE's multiblock base — same
   `META` 0-15 encoding, `getDimensions()`/`getOffset()` contract, `findCore`, orphan-cascade,
   `checkRequirement`/`fillSpace` via `MultiblockHandlerXR`, `standardOpenBehavior` helper. CE's
   `MachineCrucible extends BlockDummyable` maps onto this directly with zero new multiblock
   machinery needed.
6. **`ItemScraps`/`ItemMold`** (the two items CE's Crucible pour/shovel/mold-JEI logic touches) are
   both already ported, though with a **documented, deliberate shape change from CE**: CE modeled
   both as one shared multi-item keyed by metadata; this port registers one distinct
   `scraps_<material>` item per smeltable/additive material (82 of them, `MachineItems.java:515-520`,
   confirmed via `Mats.orderedList` + `ItemScraps.isScrappable`) and one shared `mold` item with the
   selected mold shape as a data component. **Consequence for the port**: CE's
   `ItemScraps.create(MaterialStack)` (single call, material baked as metadata) has no direct
   equivalent — the port's own `ItemScraps.create(ItemStack scrapItem, int amount, boolean liquid)`
   needs the *specific* `scraps_<material>` item resolved first. `ItemMold.MoldEntry.getOutput`
   (`items/machine/ItemMold.java:101-110`) already demonstrates the exact resolution pattern to
   reuse: `BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(MODID,
   "scraps_" + material.getRegistryName()))`.
7. **`PollutionHandler`** (with `SOOT_PER_SECOND` and `incrementPollution`) already exists and
   matches the exact call shape `TileEntityCrucible.update()` needs for its pour-side SOOT emission.
8. **`IHeatSource`** (the 2-method interface `tryPullHeat()` needs) already exists verbatim — but
   **zero implementers exist in this port** (see mechanics section above); this is a real, separate
   gap, not covered.
9. **`IControlReceiver`** already exists with a real precedent implementer (`MachineMixerBlockEntity`)
   for the exact "GUI button sends a control packet that mutates a named-recipe-selection field on
   the block entity" pattern `GUICrucible`'s recipe-selector button needs.
10. **The established machine block-entity/menu/screen convention** (`MachineBaseBlockEntity` →
    `MenuBase<T>` → `GuiInfoContainer<Menu>`, `SlotNonRetarded`/`SlotTakeOnly`, per-machine
    `*BlockEntities.java` `DeferredRegister` holder classes) is fully proven across ~30 already-ported
    machines (Shredder, Assembler, Electrolyser, Mixer, etc. all read as precedent — see
    "Recommended shape" below) and is directly reusable, unchanged, for the Crucible.

**What is NOT covered (the real remaining gap)**: every file this task was assigned — `TileEntityCrucible`,
`MachineCrucible`, `ContainerCrucible`, `GUICrucible`, `CrucibleRecipe`, `CrucibleRecipes`,
`CrucibleUtil`, `ICrucibleAcceptor` — has **zero port-side equivalent**. Confirmed by exhaustive grep
(`grep -rln "Crucible" src/main/java/com/hbm`): the only 11 hits are the weapon item, its data
component/tier/config wiring, and 3 forward-referencing javadoc comments in already-ported machines
(`ElectrolyserBlockEntity`, `ElectrolyserFluidRecipes`, `Mats.java`) that explicitly *name* the
Crucible/CrucibleUtil as a not-yet-existing downstream dependency. No `Crucible` block, tile entity,
container, screen, recipe class, or `ICrucibleAcceptor` interface exists anywhere in
`src/main/java/com/hbm` today.

## Full recipe/entry catalog (small file — all 13 active entries + 4 commented-out)

`n` = `MaterialShapes.NUGGET.q(1)` = 8 quanta; `i` = `MaterialShapes.INGOT.q(1)` = 72 quanta (both
constants already present and matching CE 1:1 in this port's `MaterialShapes.java`).

| Internal name | Freq (ticks) | Icon (this port's item, confirmed present) | Inputs | Outputs |
|---|---:|---|---|---|
| `crucible.steel` | 20 | `ingot_steel` | IRON ×16, CARBON ×24, FLUX ×8 | STEEL ×16 |
| `crucible.hematite` | 6 | `stone_resource_hematite` (block) | HEMATITE ×144, FLUX ×16 | IRON ×72, SLAG ×24 |
| `crucible.malachite` | 6 | `stone_resource_malachite` (block) | MALACHITE ×144, FLUX ×16 | COPPER ×72, SLAG ×24 |
| `crucible.redcopper` | 2 | `ingot_red_copper` | COPPER ×8, REDSTONE ×8 | MINGRADE ×16 |
| `crucible.hss` | 9 | `ingot_dura_steel` | STEEL ×40, TUNGSTEN ×24, COBALT ×8 | DURA ×72 |
| `crucible.ferro` | 3 | `ingot_ferrouranium` | STEEL ×16, U238 ×8 | FERRO ×24 |
| `crucible.tcalloy` | 9 | `ingot_tcalloy` | STEEL ×64, TECHNETIUM ×8 | TCALLOY ×72 |
| `crucible.cdalloy` | 9 | `ingot_cdalloy` | STEEL ×64, CADMIUM ×8 | CDALLOY ×72 |
| `crucible.bbronze` | 9 | `ingot_bismuth_bronze` | COPPER ×64, BISMUTH ×8, FLUX ×24 | BBRONZE ×72, SLAG ×24 |
| `crucible.abronze` | 9 | `ingot_arsenic_bronze` | COPPER ×64, ARSENIC ×8, FLUX ×24 | ABRONZE ×72, SLAG ×24 |
| `crucible.cmb` | 3 | `ingot_combine_steel` | MAGTUNG ×48, MUD ×24 | CMB ×72 |
| `crucible.magtung` | 3 | `ingot_magnetized_tungsten` | TUNGSTEN ×72, SCHRABIDIUM ×8 | MAGTUNG ×72 |
| `crucible.bscco` | 3 | `ingot_bscco` | BISMUTH ×16, STRONTIUM ×16, CALCIUM ×16, COPPER ×24 | BSCCO ×72 |

Commented-out in CE source (`registerDefaults()` lines 63-73, gated behind
`Compat.isModLoaded(Compat.MOD_GT6)`, GregTech-6 compat — **out of scope per PORT_SPEC.md's own
GregTech exclusion example**, do not port): `crucible.steelWrought` (WROUGHTIRON×16 + CARBON×8 →
STEEL×16), `crucible.steelPig` (PIGIRON×16 + CARBON×8 → STEEL×16), `crucible.steelMeteoric`
(METEORICIRON×16 + CARBON×8 → STEEL×16).

Not a `CrucibleRecipe` at all, but generated in the same file (`registerMoldsForJEI()`,
lines 252-274) and worth naming precisely since it's easy to conflate: a **JEI-only display list**,
one entry per (SMELTABLE material) × (non-empty-output `ItemMold.Mold`) pair — `scrap-cost + mold
item + basin item (foundry_mold/foundry_basin/vanilla fire, by `mold.size`) + cast output item`.
This documents the *separate* Foundry mold-casting mechanic (CE's `FoundryMold`/`FoundryBasin`
blocks, an `ICrucibleAcceptor` implementer downstream of the Crucible, not part of
`TileEntityCrucible` itself) — see "Open questions."

## Item/registry dependency check

**Every material constant and every icon item CE's 13 real `CrucibleRecipe` entries reference
already exists in this port** — spot-checked individually, not assumed:

- All 21 distinct `MAT_*` constants used (`MAT_IRON`, `MAT_CARBON`, `MAT_FLUX`, `MAT_HEMATITE`,
  `MAT_MALACHITE`, `MAT_COPPER`, `MAT_REDSTONE`, `MAT_MINGRADE`, `MAT_STEEL`, `MAT_TUNGSTEN`,
  `MAT_COBALT`, `MAT_DURA`, `MAT_U238`, `MAT_FERRO`, `MAT_TECHNETIUM`, `MAT_TCALLOY`, `MAT_CADMIUM`,
  `MAT_CDALLOY`, `MAT_BISMUTH`, `MAT_ARSENIC`, `MAT_BBRONZE`, `MAT_ABRONZE`, `MAT_MAGTUNG`,
  `MAT_MUD`, `MAT_CMB`, `MAT_SCHRABIDIUM`, `MAT_STRONTIUM`, `MAT_CALCIUM`, `MAT_BSCCO`, `MAT_SLAG`)
  are confirmed present in `src/main/java/com/hbm/inventory/material/Mats.java` (all 30, exact name
  match, read directly).
- All 11 distinct icon items (`ingot_steel`, `ingot_red_copper`, `ingot_dura_steel`,
  `ingot_ferrouranium`, `ingot_tcalloy`, `ingot_cdalloy`, `ingot_bismuth_bronze`,
  `ingot_arsenic_bronze`, `ingot_combine_steel`, `ingot_magnetized_tungsten`, `ingot_bscco`) are
  confirmed registered in `src/main/java/com/hbm/items/IngotNuggetItems.java` under the identical
  literal id CE uses.
- The 2 icon blocks (`stone_resource_hematite`, `stone_resource_malachite`) are confirmed registered
  in `src/main/java/com/hbm/blocks/generic/GenericBlocks.java` via a loop over
  `BlockEnums.EnumStoneType`, which includes both `HEMATITE` and `MALACHITE` members.

**Verdict: the entire 13-entry recipe corpus is "ready to port now" from an item-dependency
standpoint** — porting `CrucibleRecipe`/`CrucibleRecipes.registerDefaults()` requires inventing zero
new items. The blocker is entirely the *machine mechanism* (block/TE/menu/screen/interface classes),
not missing content.

**Blocked / partially-blocked adjacent pieces** (not required to land a functioning Crucible, but
needed for it to be *useful* in survival):

- **No `IHeatSource` implementer exists** — the Crucible will sit at heat 0 until a heater/firebox
  block entity is also ported (out of this task's assigned file list; CE's implementers are
  `TileEntityHeaterOilburner`/`TileEntityFireboxBase`/`TileEntityHeaterHeatex`/
  `TileEntityHeaterElectric`, none in this port).
- **No `ICrucibleAcceptor` implementer exists anywhere** (confirmed: grep for `ICrucibleAcceptor` in
  this port returns 0 hits) — CE's Foundry block family (`FoundryBasin`, `FoundryMold`,
  `FoundryChannel`, `FoundryOutlet`, `FoundrySlagtap`, `FoundryCastingBase`, `FoundryTank`, none
  ported, confirmed via `find` in CE) is the primary mold-casting sink for poured material. Without
  at least one `ICrucibleAcceptor` block, a ported Crucible pours safely into nothing (per CE's own
  `safe=true` no-op behavior — not a crash, just inert) and its material never becomes a finished
  item via the mold-casting path (only the shovel-scoop-to-scrap path works).
- **`Mats.materialEntries`/`materialOreEntries` are empty** until `crucible-matdistribution` lands —
  the Crucible's smelting-input side (`isItemSmeltable`/`trySmelt`) will accept nothing until that
  companion task populates them. This is the explicitly-scoped-out integration point, not a defect
  in this task's own recommended shape.
- **CE's `GUIScreenRecipeSelector`** (331 lines, the generic named-recipe picker `GUICrucible` opens)
  is not ported anywhere in this port yet.

## Recommended 1.21.1 implementation shape

**Not a JSON/vanilla-`Recipe<Input>` shape — a bespoke data class + registry, matching this port's
own already-established convention for exactly this situation.** CE's `CrucibleRecipe` is
fundamentally incompatible with vanilla's `Recipe<RecipeInput>` contract: it converts a *pool* of
`Mats.MaterialStack` entries (not discrete `ItemStack` inputs) at a *rate* (`frequency`, repeated
indefinitely) rather than performing a one-shot craft, and its selection is a player-driven named
choice stored on the block entity, not ingredient-pattern matching. This port has already solved the
identical problem three times (`CrystallizerRecipes`, `MixerRecipes`, `RefineryRecipes`, each with
its own javadoc explaining the same "doesn't fit `Recipe<RecipeInput>`" reasoning) — **follow that
exact precedent**:

1. **`CrucibleRecipe`**: a plain Java class (not extending anything from
   `com.hbm.inventory.recipes.loader` — that package's `GenericRecipe`/`GenericRecipes` stand-in is
   explicitly a "minimal compile-time stand-in" whose own javadoc invites a real machine to "extend
   or replace this with the real input/output/duration/power fields at that time"; CE's
   `CrucibleRecipe` fields don't match that generic shape closely enough to be worth forcing through
   it — a fresh, purpose-built class mirroring CE's fields 1:1 (`String name`, `Mats.MaterialStack[]
   input`, `Mats.MaterialStack[] output`, `int frequency`, `ItemStack icon`) is simpler and clearer).
2. **`CrucibleRecipes`**: a `final` utility class with a `Map<String, CrucibleRecipe>` (CE's
   `recipeNameMap`), a `registerDefaults()` populated with the 13 literal calls from the catalog
   above (or lazily on first lookup, matching `MixerRecipes`/`CrystallizerRecipes`'s established
   "lazy registration" precedent so item-dependent icons don't race registry population), and a
   `getRecipe(String name)` lookup. No JSON loader is needed for this task's scope (CE's
   `hbmCrucible.json` external-override read/write is a data-driven-tuning nicety, not required for
   functional parity) — flag as a deferred nicety, not a blocker.
3. **Block**: `MachineCrucibleBlock extends BlockDummyable`, `getDimensions()` → `{1,0,1,1,1,1}`,
   `getOffset()` → `1`, `newBlockEntity` returning `null` for non-core `META` (matching
   `ElectrolyserBlock`'s exact pattern, simpler than CE's `TileEntityProxyCombo`), hand-authored
   bounding boxes copied from CE's 5 `AxisAlignedBB`s, `implements ICrucibleAcceptor` delegating to
   the core TE exactly like `MachineCrucible.java` does today.
4. **Block entity**: `MachineCrucibleBlockEntity extends MachineBaseBlockEntity implements
   ITickableBE, MenuProvider, ICrucibleAcceptor, IControlReceiver` — the same base class every other
   ported machine (Shredder, Electrolyser, Mixer) already extends. 10-slot inventory
   (`super(type, pos, state, 10, false, false)` — no fluid/energy wrapper needed, matching CE: the
   Crucible has neither a `FluidTankNTM` nor an `IEnergyReceiverMK2` capability). `heat`/`progress`
   plain `int` fields (+`recipe` `String`, +`recipeStack`/`wasteStack` `List<Mats.MaterialStack>`)
   written in `saveAdditional`/read in `loadAdditional`, layered into `serialize`/`deserialize` for
   live sync — exactly the pattern this port's `MenuBase` javadoc documents as the established
   convention (no `ContainerData`). `tryPullHeat`/`trySmelt`/`tryRecipe`/pour logic port essentially
   line-for-line from CE, substituting: `Mats.getSmeltingMaterialsFromItem` (already present,
   unchanged call), `ItemMold.MoldEntry`-style `BuiltInRegistries.ITEM.getOptional(...)` lookup in
   place of CE's `ItemScraps.create(MaterialStack)` (see "Already covered" #6), and `IHeatSource`
   (already present, unchanged interface). `CrucibleUtil` ports near-verbatim (raytrace + spill
   logic is Minecraft-version-shape-stable; swap `RayTraceResult`→`BlockHitResult`,
   `world.rayTraceBlocks`→`level.clip`, `EnumFacing`/`ForgeDirection`→`Direction` per this port's
   already-established conversion elsewhere).
5. **Menu**: `MachineCrucibleMenu extends MenuBase<MachineCrucibleBlockEntity>` — 9 slots via a new
   `SlotOneItem extends SlotNonRetarded` (stack-limit-1 override, ~4 lines, mirrors
   `ContainerCrucible.SlotOneItem` exactly) in a 3×3 loop at CE's coordinates, `playerInv(...)` at
   CE's y-offsets.
6. **Screen**: `MachineCrucibleScreen extends GuiInfoContainer<MachineCrucibleMenu>` — follow
   `MachineShredderScreen`'s established "plain-panel, hand-blit bars" convention (no texture asset
   exists yet for any Phase 2 machine screen per that class's own javadoc precedent) for progress/heat
   bars; the two vertical material-stack fill bars and the recipe-select hover/click zone are new
   rendering work but mechanically the same "read fields off `getMenu().be`" pattern every other
   screen already uses.
7. **Recipe-selector UI**: recommend porting a *minimal* Crucible-specific version now (a scrollable
   button list cycling through `CrucibleRecipes`' ~13 names, sent via the already-present
   `IControlReceiver` contract) rather than the full generic 331-line `GUIScreenRecipeSelector` —
   narrower scope, unblocks the Crucible without taking on a shared-widget redesign. Flag the generic
   widget as a legitimate future generalization once a second `GenericRecipe`-shaped machine needs
   the same picker (CE has several: Assembler, Fusion, ChemPlant, etc.).
8. **Registration wiring**: follow `ProcessingBlockEntities.java`'s established
   `ModBlocks.BLOCK_ENTITY_TYPES.register("machine_crucible", () -> BlockEntityType.Builder.of(...))`
   pattern; a new `MenuType` following `ProcessingMenus`'s precedent; block registration following
   `ModBlocks`'s existing `registerBlock(...)` helper (pairs a `BlockItem` automatically, per the
   parity report's own confirmed convention).

## Open questions / risks

1. **The "~24 recipe entries" figure in the assignment brief does not match a direct read of CE's
   source (13 active entries)** — flagged explicitly in "Scope confirmed" above rather than silently
   reconciled; whoever picks this up should treat 13 as the ground truth (this task read the file
   directly and grepped to confirm) rather than re-deriving 24 from a different counting method.
2. **The Foundry mold-casting block family (`FoundryBasin`/`FoundryMold`/`FoundryChannel`/
   `FoundryOutlet`/`FoundrySlagtap`/`FoundryCastingBase`/`FoundryTank`, 7 CE block classes) is a
   second, separate `ICrucibleAcceptor` implementer this task did not scope or catalog** — it is the
   *only* way poured material becomes a finished mold-cast item in CE's real design, since the
   Crucible itself has no mold slot. Recommend flagging this explicitly to whoever plans the next
   wave of tasks: a Crucible with no Foundry counterpart is mechanically complete but has no
   "payoff" path for cast items (only the shovel-scoop-to-scrap fallback works). Not part of this
   task's assigned file list, so not researched in depth here — genuinely open whether it becomes
   its own follow-on task or folds into this one's implementation pass.
3. **No `IHeatSource` implementer exists in this port at all.** Same shape of gap as #2 — the
   Crucible will be inert (heat always decays to 0) until at least one heater block entity is ported.
   Worth deciding whether a minimal heat source (e.g. a coal-burning firebox) rides along with this
   task's implementation pass or is deferred.
4. **CE's `hbmCrucible.json` external recipe-override file** (`readRecipe`/`writeRecipe`,
   `CrucibleRecipes.java` lines 138-193) is a data-driven config layer this task recommends deferring
   (not required for functional parity — the 13 hardcoded defaults cover 100% of CE's real recipe
   surface); confirm this is an acceptable trim before implementation, since PORT_SPEC.md's general
   preference is JSON-first where practical, and this is a case where the port's own precedent
   (Crystallizer/Mixer/Refinery) already established that plain-Java registration is the accepted
   shape for this exact recipe family.
5. **`GUIScreenRecipeSelector` (generic, 331 lines) vs. a Crucible-specific minimal picker**: this
   report recommends the narrower Crucible-specific build (see "Recommended shape" #7) to avoid
   scope creep into a shared-widget redesign, but this is a judgment call — a reviewer preferring
   long-term reuse across multiple future `GenericRecipe`-shaped machines (Assembler dropdown,
   ChemPlant, Fusion, etc. — all still unported) might reasonably prefer investing in the generic
   widget now instead.
6. **`Mats.MaterialStack` network serialization**: CE's TE `serialize`/`deserialize` encode
   `MaterialStack` lists as raw `material.id` (an `int`) + `amount` pairs over the wire, looked up via
   `Mats.matById`. This port's `Mats.matById` map already exists (confirmed present, same shape,
   populated in `NTMMaterial`'s constructor) — the exact same encoding should port unchanged, just
   swap `ByteBuf`/`ByteBufUtils` for this port's `RegistryFriendlyByteBuf` conventions (already
   established across every other ported block entity's `serialize`/`deserialize` pair).
7. **Not independently verified**: whether `MultiblockHandlerXR.checkSpace`/`fillSpace` (the port's
   already-ported multiblock placement helper `BlockDummyable` delegates to) correctly handles the
   Crucible's specific `{1,0,1,1,1,1}` dimension array end-to-end (e.g., placement validation against
   existing terrain) — this task read `BlockDummyable` itself in full but not
   `MultiblockHandlerXR`'s own implementation, since every other already-ported `BlockDummyable`
   subclass (Electrolyser, Cyclotron, GasCentrifuge, Centrifuge, ChemPlant) is a working, real
   precedent for the exact same contract, making this a low-risk gap rather than an unknown one.

## Files referenced (absolute paths)

CE source read in full:
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/tileentity/machine/TileEntityCrucible.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/blocks/machine/MachineCrucible.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerCrucible.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/GUICrucible.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CrucibleRecipe.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/inventory/recipes/CrucibleRecipes.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/util/CrucibleUtil.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemCrucible.java`
- `/home/user/hbm-neoforge-port/upstream/hbm-ce/src/main/java/com/hbm/api/block/ICrucibleAcceptor.java`

This port's source read in full (for "already covered" / convention-matching):
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/material/Mats.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/material/MaterialShapes.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/material/NTMMaterial.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/items/weapon/ItemCrucible.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/api/tile/IHeatSource.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/items/machine/ItemScraps.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/items/machine/ItemMold.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/blocks/BlockDummyable.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/blockentity/machine/MachineShredderBlockEntity.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/blocks/machine/MachineShredderBlock.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/container/machine/MachineShredderMenu.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/gui/machine/MachineShredderScreen.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/blockentity/machine/DummyBlockEntity.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/blocks/machine/chem/ElectrolyserBlock.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/container/MenuBase.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/recipes/loader/GenericRecipe.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/recipes/loader/GenericRecipes.java`
- `/home/user/hbm-neoforge-port/src/main/java/com/hbm/inventory/recipes/MixerRecipes.java`
- Spot-read (excerpts): `MachineBaseBlockEntity.java`, `MachineDataComponents.java`,
  `ElectrolyserBlockEntity.java`, `ServerConfig.java`, `GeneralConfig.java`, `MaterialRegistry.java`,
  `PollutionHandler.java`, `IngotNuggetItems.java`, `GenericBlocks.java`, `BlockEnums.java`,
  `MachineItems.java`, `ProcessingRecipes.java`, `ProcessingBlockEntities.java`, `AssemblerRecipe.java`
- Also read: `docs/phase6/PARITY_REPORT.md`, `docs/phase6/recipe_graph_audit.md`
