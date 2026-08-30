# Menu/Screen GUI framework prerequisite (survey of 222 CE GUI files + 172 CE containers)

Source: `hbm-ce/src/main/java/com/hbm/inventory/gui/**/*.java` (222 files),
`hbm-ce/src/main/java/com/hbm/inventory/container/**/*.java` (172 files, single-level package,
no subdirectories). Cross-checked against `upstream/neo-edition/src/main/java/com/hbm/inventory/menus/**`
(40 files) and `.../screens/**` for confirmed real NeoForge 1.21.1 API shapes only - Neo Edition is
never used as a source of behavior, only of "does this API exist and what does it look like."

Goal: Phase 1's own research (`docs/phase1/items_tool.md` finding on `ItemToolAbility`, and inline
comments already left in `com.hbm.items.special.ItemBook` / `com.hbm.items.tool.ItemGuideBook`)
confirmed no `AbstractContainerMenu`/`Screen` pair exists anywhere in this port yet, and several
Phase 1 items already have GUI-opening interactions stubbed out waiting on it. Every Phase 2 machine
block entity needs a Menu+Screen pair too, so this report designs (does not implement) the shared
base classes, the `MenuType<?>` registration convention, and the progress-bar/fluid-tank/energy-bar
widget approach, grounded in CE's actual visual conventions and cross-checked against Neo Edition's
confirmed-real (if sometimes off-spec) NeoForge 1.21.1 usage.

## Headline finding

CE's GUI layer is not one framework but a large, fairly consistent *pattern* applied ad hoc per
machine: a `Container` subclass (172 of them, all in one flat `com.hbm.inventory.container` package)
paired 1:1 with a `GuiScreen`/`GuiContainer` subclass (222 files under `com.hbm.inventory.gui`,
including 56 `GUIMachine*` machine GUIs, 172-ish 1:1-paired container-backed GUIs, plus ~50 screen-only
classes with no container - `GUIScreenToolAbility`, the RBMK/turret/nuke non-inventory panels, item-owned
GUIs like `GUIBook`/`GUICalculator`). Two genuinely shared base classes exist and both are thin:
`ContainerBase` (slot-adding helpers + `TransferStrategy`-based shift-click) and `GuiInfoContainer`
(hover-tooltip helpers, the small numbered-icon `gui_utility.png` overlay, and `drawElectricityInfo`).
There is **no shared progress-bar or fluid-tank widget class** - progress bars are hand-blitted per
GUI (`drawTexturedModalRect` against each machine's own texture, scaled by `progress * width /
maxProgress`), and fluid tanks render via **a method on the tank object itself**
(`FluidTankNTM.renderTank`/`renderTankInfo`), not a separate widget class. Energy bars are the same
hand-blit pattern as progress bars, just vertical. This is the shape Phase 2's shared base classes
should preserve: thin base classes for boilerplate (slots, tooltips, background/foreground layering),
plus each machine's own `renderBg`/foreground override doing its own texture math, plus tanks that
render themselves.

Neo Edition (client-only shape reference, not content) confirms every piece of this survives the
1.12->1.21.1 jump close to verbatim: `MenuBase<T extends Container>` mirrors `ContainerBase` exactly
(same slot-helper method names), `InfoScreen<T>` mirrors `GuiInfoContainer` exactly (same
`drawElectricityInfo`/`drawInfoPanel` signatures translated to `GuiGraphics`), and its
`MachineFluidTankBlockEntity.tank.renderTank(...)` / `renderTankTooltip(...)` calls confirm the
"tank renders itself" convention is unchanged. This gives high confidence the design below is not
speculative - it is the same shape CE already used, expressed in confirmed 1.21.1 APIs.

## Phase-2-safe scope

The following can be designed and built now, independent of any specific Phase 2 machine, and none
of it depends on world-fluid-blocks, `RecipesCommon`/JSON recipes, or the multiblock framework:

- **`MenuBase<T extends BlockEntity & Container>` / equivalent shared abstract `AbstractContainerMenu`
  subclass** under `com.hbm.inventory.menu` (see naming note below), directly modeled on CE's
  `ContainerBase` (`upstream/hbm-ce/.../container/ContainerBase.java`, 122 lines): a `stillValid`
  delegating to the block entity, a `quickMoveStack` reference implementation identical in shape to
  CE's `transferStackInSlot`, and the same slot-batch helper methods CE has
  (`playerInv(Inventory, x, y[, hotbarY])`, `addSlots`, `addOutputSlots`, `addTakeOnlySlots`). CE's own
  `TransferStrategy`-based generic shift-click (`com.hbm.inventory.TransferStrategy`,
  `com.hbm.util.InventoryUtil.transferStack`) is itself Phase-2-safe to port alongside this - it has no
  machine-specific dependency, it is used identically by every one of the 172 containers.
- **A shared `Screen` base** (`GuiScreenBase`/`GuiInfoContainer` -> one `AbstractContainerScreen<T>`
  subclass), porting: `drawElectricityInfo` (hover tooltip showing `power/maxPower` formatted via
  `Library.getShortNumber`, confirmed still `long`-typed on both sides - see Key design decisions),
  `drawCustomInfoStat`/`drawCustomInfo` (generic hover-tooltip-on-AABB helper, used by nearly every
  machine GUI for upgrade-slot tooltips), and `drawInfoPanel` (the 12-icon `gui_utility.png` sprite
  sheet blit - small/large blue/green/red/yellow/grey I/!/* icons used for machine status hints).
  Neo Edition's `InfoScreen` (`upstream/neo-edition/.../screens/InfoScreen.java`) is a line-for-line
  confirmation this translates cleanly to `GuiGraphics.blit`/`GuiGraphics.renderComponentTooltip`.
- **`MenuType<?>` `DeferredRegister` convention**: one `DeferredRegister<MenuType<?>>` created via
  `DeferredRegister.create(Registries.MENU, "hbm")`, one `DeferredHolder<MenuType<?>, MenuType<X>>`
  entry per machine registered through `IMenuTypeExtension.create(factory)` (NOT `MenuType::new`
  directly - see Key design decisions for why), plus one `@SubscribeEvent` static method handling
  `RegisterMenuScreensEvent` that calls `event.register(SOME_MENU_TYPE.get(), SomeScreen::new)` per
  entry. Both pieces are confirmed real, working NeoForge 1.21.1 API verbatim from
  `upstream/neo-edition/src/main/java/com/hbm/inventory/{NtmMenuTypes.java,../main/CommonEvents.java}`.
- **The progress-bar and energy-bar *convention*** (not a shared widget class - see headline finding):
  document the "scale an int/long by a pixel height/width, blit a sub-rect of the same background
  texture shifted by that many pixels" pattern so every Phase 2 machine author follows it consistently,
  the way CE's own 56 `GUIMachine*` classes all do (`TileEntityMachineElectricFurnace.getProgressScaled`/
  `getPowerScaled`, `MachineCentrifugeBlockEntity.getCentrifugeProgressScaled`/
  `getPowerRemainingScaled` in Neo Edition - same helper-method naming convention on both sides).
- **Base fluid-tank client render helper**: once Phase 2's own tank class exists (see Deferred scope -
  it is not yet ported), giving it `renderTank(...)`/`renderTankTooltip(...)` methods matching
  `FluidTankNTM`'s shape is Phase-2-safe to specify now as a contract, since it does not depend on
  which specific machine owns the tank.
- **Menu-opening call-site convention**: `ServerPlayer.openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)`
  writing the block's `BlockPos` (confirmed pattern used identically across every block-opened menu in
  Neo Edition: `CrateBlock`, `BarrelBlock`, `MachineSatLinkerBlock`, `MachineShredderBlock`,
  `DummyableBlock`, all doing `player.openMenu(new SimpleMenuProvider(be, be.getDisplayName()), pos)`
  where the block entity itself implements `MenuProvider`), and the matching client-side menu
  constructor overload `(int id, Inventory inv, FriendlyByteBuf extraData)` that reads the `BlockPos`
  back and resolves the block entity (`CompatExternal.getCoreFromPos` in Neo Edition; this port has no
  such helper yet and would need its own, trivial, equivalent).
- **Slot classes**: `SlotNonRetarded` (plain `IItemHandler`-backed slot with saner defaults),
  `SlotTakeOnly`/`SlotFiltered.takeOnly` (output-only slots), `SlotCraftingOutput` (XP-awarding output
  slot). All three exist in both CE (`com.hbm.inventory.slot`) and Neo Edition
  (flattened to `com.hbm.inventory`), have zero machine-specific logic, and are presently entirely
  absent from this port's own `com.hbm.inventory` tree (confirmed by grep - no `Slot*.java` files
  exist under `src/main/java/com/hbm/inventory` yet). Porting these alongside the Menu/Screen base is
  Phase-2-safe and is a hard prerequisite for `ContainerBase`'s slot helpers to compile.

None of the above requires deciding what any individual Phase 2 machine's recipe, fluid, or block
entity looks like - it is pure GUI-plumbing that every machine will plug into the same way CE's own
172 containers plug into `ContainerBase`.

## Deferred scope

- **Every concrete machine Menu+Screen pair** (56 `GUIMachine*` + matching containers, plus the
  non-"Machine"-prefixed ones like `GUIElectrolyserMetal`/`Fluid`, `GUICompressor`, `GUIMixer`,
  `GUICrucible`, furnace variants, RBMK consoles, turret GUIs, nuke assembly GUIs, etc. - the full 222
  minus the handful that are item-owned, not block-owned) - each waits on its own block entity's
  fields (progress/power/heat/tanks) existing first, which is the rest of Phase 2's per-machine work,
  not this prerequisite package.
- **The fluid-tank class itself** (`com.hbm.inventory.fluid.tank.FluidTankNTM` in CE, ~500 lines:
  `IFluidHandler`/`IFluidTank` implementation, NBT/network (de)serialization, `loadTank`/`unloadTank`
  item-interaction helpers, `renderTank`/`renderTankInfo` client rendering). This port's own
  `com.hbm.inventory.fluid` package (Phase 0/1 work, confirmed present: `FluidStack`, `FluidType`,
  `Fluids`, the `FT_*` trait classes) has the fluid *type registry* but not yet the tank
  *container-of-fluid* abstraction - `FluidTankNTM` and its render methods are themselves Phase 2
  scope, not this GUI-framework prerequisite. The Menu/Screen base above only specifies the *contract*
  (`renderTank`/`renderTankTooltip` method shape) the eventual tank class should satisfy.
- **World-fluid-block rendering** (a tank block rendering a visible fluid column in the world, as
  opposed to a GUI widget) - per Phase 1's own research this port has no world-fluid-block system at
  all yet. Out of scope for this GUI package entirely; flagging only so nobody conflates "fluid tank
  GUI widget" (in scope, described above) with "fluid block in the world" (does not exist, not this
  package's job).
- **`TransferStrategy`/`InventoryUtil.transferStack`** are named above as Phase-2-safe to port, but
  the actual per-machine `TransferStrategy` configurations (which slot ranges count as "machine
  input", "output", "battery", "upgrade" for shift-click purposes) are inherently per-machine and
  deferred to each machine's own implementation pass.
- **`RecipesCommon`/JSON `Recipe<?>` types**: already flagged cross-cutting in
  `docs/phase1/STATUS.md` (`com.hbm.inventory.RecipesCommon`,
  `com.hbm.inventory.recipes.loader.GenericRecipe(s)` both missing). This GUI package does not
  re-solve that gap, but two GUI-adjacent things depend on it and should be noted for whoever owns
  it: (1) `IToolable.ToolType.getType(ItemStack)` (CE's screwdriver/wrench/blowtorch dispatch,
  `upstream/hbm-ce/.../api/block/IToolable.java`) keys off `RecipesCommon.ComparableStack`, and (2)
  `GUIMachineCustom`/`TileEntityCustomMachine` - CE's fully data-driven "custom machine" GUI whose
  slot/tank counts, max power/heat, and localized name all come from a `config` object populated by
  the recipe/JSON loader - is a *generic* machine GUI in the sense that it needs zero new Screen code
  once the Menu/Screen base above exists, but it cannot be ported at all until `RecipesCommon`-style
  config loading exists. Worth remembering as a highly-reusable machine GUI once that gap closes.
- **`IToolable` machine-interaction system** (screwdriver/wrench/blowtorch/hand-drill right-click
  behavior on placed machine blocks, changing redstone mode, opening alternate configuration state,
  etc.) - this is a block-interaction system adjacent to but independent of the Menu/Screen GUI
  framework (it does not open a `Screen` itself; CE's `ItemTooling`/`ItemWrench`/etc. call `onScrew`
  directly on the block). Already tracked as Phase 2 machine-coupling in
  `docs/phase1/items_tool.md` bucket (c); not part of this GUI-framework package, only mentioned
  because the task's own framing groups it with "GUI interactions."
- **Block entity base class / package-naming decision** (`com.hbm.tileentity` vs
  `com.hbm.blockentity`): this is a prerequisite for literally every Phase 2 machine block entity,
  including the ones that will own a Menu/Screen pair designed here, but it is explicitly called out
  in `docs/phase0/STATUS.md` as a cross-cutting decision to be made once, not per-package. Flagging
  its relevance here (every Menu constructor takes a `T extends BlockEntity`-shaped block entity
  parameter) without re-deciding it - see Open questions below.

## Key design/API decisions

All of the following are confirmed by reading real NeoForge 1.21.1 usage in
`upstream/neo-edition` (API shape only, never behavior) and/or NeoForge/Minecraft classes it imports.
None are invented.

1. **`MenuType<?>` registration must go through `IMenuTypeExtension.create(factory)`, not
   `new MenuType<>(factory, FeatureFlags...)` directly.** Confirmed in
   `NtmMenuTypes.reg(String, IContainerFactory<T>)`:
   `MENU_TYPES.register(name, () -> IMenuTypeExtension.create(factory))`. This NeoForge extension
   point exists specifically so the client-side `IContainerFactory<T>` signature
   `(int windowId, Inventory inv, RegistryFriendlyByteBuf/FriendlyByteBuf extraData)` can be used
   without also having to hand-roll a vanilla `MenuType.MenuSupplier` datagen entry. Every Phase 2
   machine's `MenuType` registration should follow this exact `reg(name, Ctor::new)` helper pattern.
2. **Menu opening is `ServerPlayer#openMenu(MenuProvider, Consumer<RegistryFriendlyByteBuf>)`**, with
   the block entity itself implementing `MenuProvider` (its `createMenu(int, Inventory, Player)`
   returns `new XyzMenu(id, inv, this)`), wrapped by the block's `useWithoutItem`/`useItemOn` in a
   `SimpleMenuProvider(be, be.getDisplayName())` when a display name needs to be supplied separately.
   Confirmed identically in `CrateBlock`, `BarrelBlock`, `MachineSatLinkerBlock`,
   `MachineShredderBlock`, `DummyableBlock`, `NukeBaseBlock`, `MachineFluidTankBlock`. The
   extra-data writer is always `buf -> buf.writeBlockPos(blockEntity.getBlockPos())`, matched on the
   client by the Menu's `(int, Inventory, FriendlyByteBuf)` constructor overload reading
   `extraData.readBlockPos()` and resolving the block entity from the client's own level. This port
   will need its own small block-entity-from-pos resolution helper (Neo Edition's
   `CompatExternal.getCoreFromPos` is a one-line `level.getBlockEntity(pos)` cast wrapper - trivial to
   write fresh, not worth cross-referencing further).
3. **Machine state (progress, HE power, heat, tank contents) syncs to the client via the block
   entity's own update packet, not via vanilla `ContainerData`.** This is the most consequential
   confirmed decision. Checked directly: CE's `TileEntityMachineElectricFurnace.power` is a `long`
   (`public static final long maxPower = 100000; public long power;`), and vanilla `ContainerData`
   (and CE's own 1.12-era `Container` "windowProperty" mechanism it stands in for) is `int`-only -
   CE never uses it for these fields at all. Instead CE's TE base
   (`com.hbm.tileentity.TileEntityLoadedBase`) overrides `getUpdateTag()`/`getUpdatePacket()`/
   `onDataPacket()` to push the *entire* TE NBT to the client on every chunk-load and on relevant
   changes, so `furnace.power`/`furnace.progress` are simply already correct, live fields on the
   client-side TE instance by the time the Screen reads them
   (`GUIMachineElectricFurnace.drawGuiContainerBackgroundLayer` reads `furnace.power`/
   `furnace.getProgressScaled(28)` directly, no menu-field lookup at all). Neo Edition's
   `MachineCentrifugeScreen` confirms the exact same shape survives verbatim into 1.21.1:
   `this.be.getPower()`/`this.be.getMaxPower()`/`this.be.getCentrifugeProgressScaled(145)` read
   straight off the client-side `MachineCentrifugeBlockEntity`, no `ContainerData` involved anywhere
   in the Menu. **Design consequence for the shared base**: `MenuBase`/the shared Screen base should
   not attempt to wire a `ContainerData` int-array sync path at all (it cannot carry `long` HE values
   without splitting them into two ints, which neither CE nor Neo Edition bothers with) - the
   contract is simply "the block entity's own NBT sync already keeps client-side fields correct;
   Screens read the block entity's fields/getters directly," exactly as both CE and Neo Edition do.
   `ContainerData` should only be reached for if some future machine has a field that genuinely
   doesn't belong in the TE's own persisted/synced state, which none of the surveyed machines do.
4. **`BlockEntity` sync API confirmed already in live use in this port** (not just Neo Edition):
   `com.hbm.blocks.generic.BlockLoot.LootBlockEntity` (already ported, Phase 1) overrides
   `saveAdditional(CompoundTag, HolderLookup.Provider)`, `loadAdditional(...)`,
   `getUpdateTag(HolderLookup.Provider)`, and `getUpdatePacket()` returning
   `ClientboundBlockEntityDataPacket.create(this)`. This is the exact mechanism point 3 above relies
   on, and it is already proven to compile and follow the real 1.21.1 signature in this codebase, not
   just in Neo Edition - Phase 2 machine block entities should follow this identical override set for
   `getUpdateTag`/`getUpdatePacket`, layering `progress`/`power`/tank NBT into `saveAdditional` the
   same way CE layers them into `writeToNBT`.
5. **Fluid tanks render themselves; there is no generic "TankWidget" class in either CE or Neo
   Edition.** `FluidTankNTM.renderTank(x, y, z, width, height[, orientation])` binds the fluid type's
   own texture (`type.getTexture()`), tints it via `GL11.glColor3d` from `type.getTint()`, and draws a
   single scaled quad via raw `Tessellator`/`BufferBuilder` (`POSITION_TEX` format, UV cropped to the
   fill ratio) - not a `drawTexturedModalRect` sub-blit like solid-color progress bars, because the
   fill amount changes the *texture crop*, not just a rect position, and the same 16x16 fluid sprite is
   reused (tinted) across every fluid type. `renderTankInfo(GuiInfoContainer, ...)` is the paired
   hover-tooltip method (name + `fluid/maxFluid mB` + pressure + trait info lines). Neo Edition's
   `MachineFluidTankBlockEntity.tank.renderTank(...)`/`renderTankTooltip(...)`, called directly from
   `MachineFluidTankScreen.renderBg`/`render`, confirms this exact "tank owns its render method"
   shape carries forward, just re-expressed against `GuiGraphics`/modern `RenderSystem` instead of
   raw `GL11`/`Tessellator` calls. **Design consequence**: don't design a separate `TankWidget` class
   for Phase 2 - design the tank abstraction itself (deferred, see above) to carry these two render
   methods, matching this confirmed contract.
6. **Progress/energy bars are a hand-blit convention, not a widget class, everywhere surveyed**:
   every one of `GUIMachineElectricFurnace`, `GUIMachineCustom`, and Neo Edition's
   `MachineCentrifugeScreen` computes `scaled = value * pixelSpan / max` (or a getter that already
   does so, e.g. `getProgressScaled(28)`/`getCentrifugeProgressScaled(145)`) and blits a
   sub-rectangle of the *same* background texture shifted/cropped by that many pixels - background
   textures carry both the "empty" and "full" bar art side by side in the sprite sheet, and the Screen
   just picks how much of the "full" region to reveal. This is cheap to replicate with
   `GuiGraphics.blit` and should be documented as the house convention rather than built as a reusable
   component, since CE itself never abstracted it in ~9 years of the mod's history despite 56+
   machines using the exact same math.
7. **Package naming**: CE uses `com.hbm.inventory.container` + `com.hbm.inventory.gui` (flat, no
   subpackages). Neo Edition renamed to `com.hbm.inventory.menus` + `com.hbm.inventory.screens` (both
   plural, matching vanilla/NeoForge's own `AbstractContainerMenu`/`Screen` terminology) with one
   `element` subpackage for shared render-math helpers (`ScreenElements` - circular/linear "gauge"
   shaders, used for analog-needle-style widgets, distinct from the blit-based progress bars above).
   Since PORT_SPEC.md's ground rule is "preserve `com.hbm.*` package layout... where legal," and
   `container`/`gui` are legal, portable package names under NeoForge (nothing about NeoForge forces
   the `menus`/`screens` renaming - it's a Neo Edition stylistic choice, not an API requirement),
   **recommend keeping CE's own `com.hbm.inventory.container` / `com.hbm.inventory.gui` names** for
   this port rather than adopting Neo Edition's renaming, consistent with the "preserve package
   layout" rule and with how Phase 0/1 have already treated other Neo-Edition-renamed packages.

## Open questions / risks

- **Block entity package-naming decision is a hard blocker for this package's own base class
  signatures**, not just for individual machines. `MenuBase<T extends ??? & Container>`'s type bound
  needs a concrete block-entity-inventory contract (in CE, `IItemHandler`; in Neo Edition, the block
  entity itself implements `Container`/`net.minecraft.world.Container`). Whatever the eventual
  `com.hbm.tileentity` vs `com.hbm.blockentity` (or an all-new name) decision lands on, it needs to
  resolve *before* this package's `MenuBase` can be written for real, not just designed. Repeating
  `docs/phase0/STATUS.md`'s own framing: this is a decision needed "before Phase 2 block entities
  land," and this GUI package is one of the first things that will hit it.
- **`IItemHandler`-backed slots (CE's `SlotItemHandler`/`SlotNonRetarded`) vs. block entity directly
  implementing `net.minecraft.world.Container`**: CE's `ContainerMachineElectricFurnace` mixes both
  (`new SlotItemHandler(tedf.inventory, ...)` for most slots, but a bespoke `SlotSmelting`/
  `SlotUpgrade`); Neo Edition's `MachineCentrifugeMenu` uses only `SlotNonRetarded`/`SlotTakeOnly`
  against a block entity that itself is a `Container`. This port hasn't decided whether Phase 2 block
  entities expose their inventory as a NeoForge `IItemHandler` capability (matching this port's
  Phase 0 capability-framework investment) or as a raw vanilla `Container` (matching Neo Edition's
  simpler-but-less-capability-integrated style, and CE's mixed pattern). This changes which `Slot`
  subclass shape the shared `MenuBase` slot-helpers should be written against, and should be decided
  alongside (or as part of) the block-entity package-naming decision above, not independently by
  whichever machine happens to be ported first.
- **HE power sync via full-TE-NBT push (decision 3 above) means large/frequent tank or heat-array
  state gets pushed to every client in view on every change**, same as CE already does - not a new
  cost this port introduces, but worth flagging since NeoForge's own `ContainerData` mechanism exists
  specifically to make *cheap, throttled* int-field sync easy, and this design explicitly declines to
  use it for the common case. If a future machine has a genuinely expensive-to-serialize or
  high-frequency-changing piece of purely-cosmetic GUI state, `ContainerData` remains available as an
  escape hatch - just not the default, per the confirmed CE/Neo-Edition convention.
- **`GUIMachineCustom`/`TileEntityCustomMachine`'s fully data-driven GUI is very attractive as a
  "build once, most simple machines don't need a bespoke Screen at all" shortcut**, but it hard-depends
  on the `RecipesCommon`/JSON recipe-loading gap (already tracked, not re-litigated here). Recommend
  whoever plans machine-by-machine Phase 2 ordering treat "port `GUIMachineCustom` once
  `RecipesCommon`-equivalent config loading exists" as a high-value follow-up that could retroactively
  reduce how many bespoke Screen classes the rest of Phase 2 needs to hand-write - not a blocker on
  this package, but a reason to sequence recipe-loading work early within Phase 2 rather than late.
- **`FluidTankNTM`'s raw `Tessellator`/`BufferBuilder`/`GL11` tank-fill rendering (CE) needs a real,
  tested translation to NeoForge 1.21.1's `GuiGraphics`/`RenderSystem`/`BufferBuilder` APIs** before
  any machine with a fluid tank can ship its Screen. Neo Edition's call sites
  (`be.tank.renderTank(...)`) confirm the *call shape* survives, but this report did not read Neo
  Edition's tank-render *implementation* itself (out of scope - Neo Edition's fluid-tank abstraction
  is content/behavior, not just API shape, per the ground rule to cross-check API shape only). The
  actual crop-by-fill-ratio tessellation code will need to be written and verified against 1.21.1's
  modern rendering pipeline (likely a custom `RenderType`/`VertexConsumer` sequence within
  `GuiGraphics`'s pose stack) when the tank class itself is ported - flagging as a real implementation
  risk for that later Phase 2 sub-package, not something this survey resolves.
- **Exact final count of Menu+Screen pairs Phase 2 will need** cannot be pinned down from this report
  alone: 56 `GUIMachine*` classes is a lower bound for "obviously machine-shaped" GUIs, but several
  more of the 222 CE GUI classes not prefixed `GUIMachine` (furnaces, mixers, compressors,
  electrolysers, RBMK components, turret bases) are also Phase-2-owned block GUIs by content, while
  others (nuke assembly, nuke devices, nested RBMK sub-panels, nested launch/soyuz GUIs) likely belong
  to later phases (explosives/rocketry) per PORT_SPEC's own phase boundaries even though they share
  this exact Menu/Screen shape. Recommend the machine-by-machine Phase 2 inventory (whatever survey
  enumerates actual machine block entities) cross-reference this GUI file list rather than
  re-deriving GUI scope from scratch.
