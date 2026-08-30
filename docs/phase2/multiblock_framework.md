# Multiblock framework prerequisite (blocks.dummyable + handler.MultiblockHandlerXR + tileentity.IPersistentNBT)

Source read in full: `hbm-ce/src/main/java/com/hbm/handler/MultiblockHandlerXR.java`,
`hbm-ce/.../handler/MultiblockBBHandler.java`, `hbm-ce/.../blocks/BlockDummyable.java`,
`hbm-ce/.../tileentity/IPersistentNBT.java`; every CE file matching
`extends BlockDummyable\b` / `extends BlockDummyableMBB\b` (grep, listed below); the port's own
`src/main/java/com/hbm/blocks/BlockDummyable.java` and `BlockDummyableMBB.java` (already-written,
currently-broken skeletons); `docs/phase0/STATUS.md`, `docs/phase1/items_tool.md` (structure
example); and, for confirmed NeoForge 1.21.1 API shapes only, `neo-edition/.../handler/MultiblockHandlerXR.java`,
`neo-edition/.../blockentity/IPersistentNBT.java`, and the port's own already-working
`com.hbm.blocks.ModBlocks` / `GenericCrateBlocks` / `GenericDecoBlocks` / `ModCapabilities` block-entity
and capability registration code.

## Headline finding

This package is exactly as load-bearing as PORT_SPEC.md says. `grep -rl "extends BlockDummyable\b"`
across CE returns **149 files** (concrete machine casings plus a handful of abstract intermediate
bases: `TurretBaseNT`, `RailStandardCurveBase`, `RBMKBase`, `LaunchPad`), and a further **1 file**
(`MachineFENSU`) extends `BlockDummyableMBB`. That is 150 classes — most of NTM's multi-block
machines, turrets, launch pads, and the narrow/standard rail network — that cannot compile, place,
break, or render a highlight box without this package. `IPersistentNBT` has a smaller but still
real blast radius: 34 CE files touch it, spanning `com.hbm.tileentity`, `com.hbm.tileentity.machine`
(+`.oil`, `.storage`), `com.hbm.blocks`, `com.hbm.blocks.generic`, `com.hbm.blocks.machine`,
`com.hbm.blocks.network`, `com.hbm.items.block`, `com.hbm.render.tileentity`, and `com.hbm.core`.

The port already has hand-written, CE-faithful skeletons for `BlockDummyable.java` and
`BlockDummyableMBB.java` — they are not placeholders, they are essentially finished — and they
already **encode** the package/API decisions this research package was asked to make: they import
`com.hbm.tileentity.IPersistentNBT` (old path) but call `IPersistentNBT.restoreData(level, corePos, stack)`
(Neo Edition's method name, not CE's `onBlockPlacedBy`). That is a half-made decision sitting in the
tree today. This report finishes making it explicitly (see "Key design/API decisions" below) and
specifies the missing pieces byte-for-byte against what the existing skeleton already calls, so no
further redesign of `BlockDummyable`/`BlockDummyableMBB` is needed — only the classes they depend on.

## Phase-2-safe scope

Everything below is self-contained infrastructure with no dependency on any concrete machine,
recipe, GUI, or energy/fluid content, and unblocks all 150 `BlockDummyable`/`BlockDummyableMBB`
subclasses at once. This is the one package PORT_SPEC.md means by "multiblock framework first":

| New/fixed file | Role |
|---|---|
| `com.hbm.handler.MultiblockHandlerXR` (new) | `checkSpace`/`fillSpace`/`rotate`, rewritten for NeoForge 1.21 — signatures must match what `BlockDummyable.java` already calls (see below). Currently missing; this is the literal reason the port's `BlockDummyable.java` doesn't compile today. |
| `com.hbm.handler.MultiblockBBHandler` (new) | CE's binary `.mbb` bounding-box loader + `REGISTRY` map, rewritten for `AABB`/`ResourceLocation`. Needed by `BlockDummyableMBB.java`, which already calls `MultiblockBBHandler.REGISTRY.get(this)`. |
| `com.hbm.lib.Library.checkForPlayerEyePositions(Level, AABB)` (new method on existing stub) | Ported from CE's `Library`; used by `MultiblockHandlerXR.checkSpace` (CE behavior) and already called directly by the port's own `BlockDummyableMBB.checkRequirement`. Currently missing — a second, smaller reason `BlockDummyableMBB.java` doesn't compile. |
| `com.hbm.blockentity.IPersistentNBT` (new package + file — see package-naming decision) | Full CE contract (`writeNBT`/`readNBT`/`shouldDrop`/`setDestroyedByCreativePlayer`/`isDestroyedByCreativePlayer`/`carriesContents`/`breakBlock`/`onBlockHarvested`) reshaped to 1.21 types, plus a `restoreData(Level, BlockPos, ItemStack)` static method matching the name the port's `BlockDummyable.java` already calls (CE calls this method `onBlockPlacedBy`; Neo Edition renamed it `restoreData` — the port skeleton already committed to the Neo Edition name, so this report keeps it rather than reintroducing a third name). |
| `com.hbm.blocks.BlockDummyable` (fix) | Already written and CE-faithful (rotation/orphan-cascade/core-search/hitbox/highlight/`ICopiable`/`INBTBlockTransformable` all ported correctly per manual review). Needs no logic changes — just the two dependencies above to exist. |
| `com.hbm.blocks.BlockDummyableMBB` (fix) | Same: already written, footprint rasterization logic already correct, just needs `MultiblockBBHandler` + `Library.checkForPlayerEyePositions` to exist. |
| `assets/hbm/multiblock_bounds/bb_fensu0.mbb` (copy) | Binary resource CE ships alongside `MultiblockBBHandler`; confirmed present in `hbm-ce/src/main/resources/...` but **not yet copied** into this port's resources. Without it, `MachineFENSU`'s multiblock init throws at runtime (not a compile error) the day that one block is ported. In-scope for this package since `MultiblockBBHandler.init()` is what loads it. |

None of the above references a concrete machine casing, a GUI, an energy/fluid capability, or a
recipe type. All 150 `extends BlockDummyable`/`BlockDummyableMBB` CE classes become portable the
moment this package lands — they are the "machines fan out" PORT_SPEC.md refers to, and are
explicitly *not* part of this package's own scope (see Deferred below).

## Deferred scope

- **The 150 concrete `BlockDummyable`/`BlockDummyableMBB` subclasses themselves** (casings under
  `com.hbm.blocks.machine`, `.machine.rbmk`, `.machine.fusion`, `.machine.albion`, `.network`,
  `.network.energy`, `.turret`, `.rail`, `.bomb`, plus a few in `.generic`) and their **407** paired
  CE `com.hbm.tileentity.**` classes (`machine`, `machine.oil`, `machine.storage`, `network`, `bomb`,
  `deco`, `rail`, `turret` subpackages) — these are the actual "machines fan out" work and belong to
  whichever Phase 2 sub-packages the orchestrating session slices next (e.g. one package per
  `tileentity.machine.oil.*` refinery chain, one for RBMK, one for turrets, etc.). Do not let any of
  those packages re-decide the `com.hbm.blockentity` naming call below — it is made here, once, for
  all of Phase 2.
- **A base "machine" `BlockEntity` class** (inventory + tick + energy/fluid coupling conventions
  shared across dozens of concrete machine TEs) — CE has no single common base beyond `TileEntity`
  itself (each machine TE hand-rolls its own fields), so there is nothing to port 1:1 here, but
  whichever package ports the first real machine TE should decide a shared convention once rather
  than 150 times. Out of scope for this package: `BlockDummyable`'s own contract (`findCore`,
  `findCoreBlockEntity`) is deliberately BlockEntity-shape-agnostic — it only requires the core's
  `BlockEntity` to optionally implement `ICopiable`/`MenuProvider`/`IPersistentNBT`, all satisfied here.
- **`com.hbm.inventory.fluid.tank.FluidTankNTM`** — confirmed absent from the port (also called out
  in `docs/phase0/STATUS.md`'s compile-error triage). Needed by fluid-holding machine TEs (refinery
  chain, boilers, tanks), not by the multiblock framework itself — `BlockDummyable`/
  `MultiblockHandlerXR`/`MultiblockBBHandler` never reference fluid types. Flag as a prerequisite for
  whichever Phase 2 package ports the first fluid machine.
- **`com.hbm.inventory.RecipesCommon` / `com.hbm.inventory.recipes.loader.GenericRecipe(s)`** —
  confirmed absent (flagged in `docs/phase1/STATUS.md` already). Zero coupling from this package
  (verified: `BlockDummyable`, `BlockDummyableMBB`, `MultiblockHandlerXR`, `MultiblockBBHandler`,
  `IPersistentNBT` contain no recipe references in CE). Every concrete machine that processes items
  will need it; not re-solved here, just confirmed out of this package's scope.
- **`AbstractContainerMenu`/`Screen` GUI framework** — confirmed absent from the port (`grep -rl
  "AbstractContainerMenu"` / `"extends Screen"` under `src/main/java` returns nothing). Not a
  dependency of this package: `BlockDummyable.standardOpenBehavior` is written generically against
  vanilla `MenuProvider`/`SimpleMenuProvider` (confirmed working NeoForge 1.21 API, see below) and
  does not require a bespoke menu/screen base class to exist. It is, however, a hard prerequisite for
  every concrete machine that wants a working GUI, so this is flagged here as a **shared Phase 2
  prerequisite package**, per the task's own instruction to surface it rather than silently assume it.
- **Block-entity capability registration for HE energy / fluid on machine TEs** (the NeoForge
  `RegisterCapabilitiesEvent#registerBlockEntity(...)` counterpart to the item-side
  `registerItem(...)` already used in `com.hbm.capability.ModCapabilities`) — not exercised anywhere
  in this port or in Neo Edition today (grepped both; Neo Edition's energy system does not appear to
  route through NeoForge capabilities for blocks at all). Left as an **open question** below rather
  than an invented API shape, since instructions require every API claim to come from real usage.

## Key design/API decisions

### 1. `com.hbm.tileentity` → `com.hbm.blockentity` (the flagged package-naming call)

**Decision: rename the top-level package to `com.hbm.blockentity`, preserving every subpackage
verbatim** (`com.hbm.blockentity.machine`, `.machine.oil`, `.machine.storage`, `.network`, `.bomb`,
`.deco`, `.rail`, `.turret`, matching CE's `com.hbm.tileentity.*` layout 1:1 apart from the renamed
root). `IPersistentNBT` moves to `com.hbm.blockentity.IPersistentNBT`.

Reasoning:
- **Neo Edition already made this exact call** (an independent NeoForge 1.21 port of the same mod)
  and applied it consistently: `com.hbm.blockentity` holds 88 files including `IPersistentNBT`,
  `MachineBaseBlockEntity`, `NtmBlockEntityTypes`, etc.; there is no `com.hbm.tileentity` package
  anywhere in that tree. Per the task's own ground rules, Neo Edition is a legitimate source for
  *confirmed API/package shape*, and this is exactly that kind of decision (not game content/logic).
- **The type itself is gone.** Every one of these classes extends `net.minecraft.world.level.block.entity.BlockEntity`
  in 1.21 — there is no `TileEntity` type left to name the package after. Keeping the literal string
  `tileentity` would describe a 1.12-only concept that no longer exists in the code it contains.
- **PORT_SPEC.md's "preserve com.hbm.\* package layout... where legal" is satisfied in spirit, not
  violated.** The intent of that rule (recognizable structure, stable registry ids for save
  compatibility) is fully preserved: every subpackage under the renamed root stays identical, and
  Java package names have **no effect on registry ids** — those come from the string literals passed
  to `DeferredRegister.register("name", ...)` / `setRegistryName`, which are completely unaffected by
  this rename. This is a pure Java-side rename with zero save-compatibility or datapack impact.
- **The port's own broken skeleton already leans this way.** `BlockDummyable.java`'s import is still
  the old `com.hbm.tileentity.IPersistentNBT` path (stale — literally why part of it doesn't
  compile), but its *call site* already uses `IPersistentNBT.restoreData(...)`, Neo Edition's method
  name, not CE's `onBlockPlacedBy`. Whoever wrote that skeleton was already reaching for the Neo
  Edition shape. This report finishes that decision consistently instead of leaving one file half
  migrated.
- Action item for implementation: fix `BlockDummyable.java`'s import line from
  `com.hbm.tileentity.IPersistentNBT` to `com.hbm.blockentity.IPersistentNBT` when this package
  lands — everything else in that file is already correct.

**What is *not* adopted from Neo Edition**: Neo Edition also redesigned the dummy-block encoding
itself away from CE's single 0-15 `meta` int toward a `FACING` + `DummyBlockType` (`TYPE`) pair of
blockstate properties (see its `MultiblockHandlerXR.fillSpace`, which sets
`.setValue(DummyableBlock.FACING, ...).setValue(DummyableBlock.TYPE, DummyBlockType.DUMMY)`). That is
a game-logic/data-shape redesign, not an API-shape fact, and per the ground rules Neo Edition is not
authoritative for that. The port's own `BlockDummyable.java`/`BlockDummyableMBB.java` already made
the correct call here independently: they preserve CE's exact 0-15 `META` `IntegerProperty` encoding
bit-for-bit (see that file's own header javadoc), which keeps every subclass's hand-computed
offset/bitmask math (`getOffset()`, `getMetaForCore()`, extra-flag checks, etc.) portable verbatim
from CE source. This report's `MultiblockHandlerXR`/`MultiblockBBHandler` designs below match that
existing, correct choice — they write `META`, not `FACING`/`TYPE`.

### 2. `MultiblockHandlerXR` signatures — dictated by the existing `BlockDummyable.java` call sites, confirmed against Neo Edition

The port's own `BlockDummyable.checkRequirement`/`fillSpace` already call:
```java
MultiblockHandlerXR.checkSpace(level, placedPos.relative(dir, placementOffset), getDimensions(), placedPos, dir)
MultiblockHandlerXR.fillSpace(level, placedPos.relative(dir, placementOffset), getDimensions(), this, dir)
```
i.e. `checkSpace(Level, BlockPos corePos, int[] dim, BlockPos placedPos, Direction dir) -> boolean`
and `fillSpace(Level, BlockPos corePos, int[] dim, Block, Direction dir) -> void`. This is
**confirmed as a real, working NeoForge 1.21 shape** by Neo Edition's own `MultiblockHandlerXR`,
which uses the identical parameter list and order (`checkSpace(Level level, BlockPos corePos, int[]
dim, BlockPos placedPos, Direction dir)`) — the two 1.12 overloads (`int x,int y,int z` vs. a
`ForgeDirection` vs. `EnumFacing` pair) collapse into one `BlockPos`/`Direction` signature on both
independent ports, because `BlockPos` already carries what CE needed three ints for. The body,
however, must diverge from Neo Edition's (see decision 1): `fillSpace` sets
`this.defaultBlockState().setValue(BlockDummyable.META, dir.get3DDataValue())` matching CE/the port's
`META` encoding, not Neo Edition's `FACING`/`TYPE` pair. `checkSpace` must also retain CE's
player-eye-position safety check (`Library.checkForPlayerEyePositions`) that Neo Edition's version
drops — per the ground rules CE is the sole source of truth for behavior, and Neo Edition is known
to be incomplete/sometimes-wrong; dropping a safety check that prevents suffocating players inside a
newly-placed multiblock is exactly the kind of behavior regression the ground rules warn against
copying. `rotate(int[], Direction)` is pure coordinate-swap arithmetic, identical in CE and Neo
Edition, and copies over verbatim (`Direction.from3DDataValue`/`get3DDataValue` enumerate DOWN, UP,
NORTH, SOUTH, WEST, EAST in the same 0-5 order 1.12's `ForgeDirection` did for those indices — this
was already verified and relied on inside the port's existing `BlockDummyable.java`).

`Level.getBlockState(pos).canBeReplaced()` (used by Neo Edition's `checkSpace` in place of CE's
`Block#isReplaceable`) is confirmed real 1.21 API — it's the same call the port's own
`BlockDummyableMBB.checkRequirement` already uses today.

### 3. `Library.checkForPlayerEyePositions` — ported to 1.21, confirmed call site already exists

CE's version (`hbm-ce/.../lib/Library.java:116`) takes a `World`/`AxisAlignedBB`, gathers
`EntityPlayer`s in the box, and for each non-creative/non-spectator player whose eye position falls
inside the box, checks whether the block above their head has a non-empty collision box or the space
above is also inside the box — if so the space can't be placed on top of them. The 1.21 shape (all
confirmed vanilla/NeoForge API, no invention): `Level#getEntitiesOfClass(Player.class, AABB)`,
`Player#isCreative()`/`isSpectator()` (still exist, unchanged names), `Entity#getEyeY()` in place of
`posY + eyeHeight`, `AABB#contains(Vec3)`, and `BlockState#getCollisionShape(BlockGetter,
BlockPos)#isEmpty()` in place of CE's `getCollisionBoundingBox(...) != Block.NULL_AABB`. The call
site already exists and is already correct: `BlockDummyableMBB.checkRequirement` calls
`Library.checkForPlayerEyePositions(level, span)` today — it just has nothing to call yet.

### 4. `IPersistentNBT` — CE's fuller contract, Neo Edition's method name and package, 1.21 types

Confirmed from reading both real implementations (not invented): CE's interface (`writeNBT`,
`readNBT`, `shouldDrop`, `setDestroyedByCreativePlayer`, `isDestroyedByCreativePlayer`,
`carriesContents`, static `breakBlock`/`onBlockPlacedBy`/`onBlockHarvested`) is the fuller, more
game-accurate contract — it preserves creative-mode no-drop behavior and the custom-display-name
round-trip via the port's own already-existing `com.hbm.api.tile.IWorldRenameable` (confirmed
present, already 1.21-shaped: `Component`-based `setCustomName`, extends `Nameable`). Neo Edition's
version is a simplified subset (only `writeNBT`/`readNBT` + a `getDrops`/`restoreData` pair) that
drops the creative-no-drop and custom-name behavior entirely — that is a behavior loss, not just an
API-shape difference, so per the ground rules this report does **not** adopt Neo Edition's reduced
contract. What *is* adopted from Neo Edition, because it is purely a naming/package-shape fact and
the port's own skeleton already committed to it: the package (`com.hbm.blockentity`, see decision 1)
and the static restore-on-place method's name, `restoreData(Level, BlockPos, ItemStack)` (CE calls
this `onBlockPlacedBy`, which would otherwise collide/read confusingly next to `Block#onBlockPlacedBy` —
Neo Edition's rename is the better name and the port already calls it that).
`net.minecraft.nbt.CompoundTag` replaces `NBTTagCompound`; `IWorldNameable` (checked via
`hasCustomName()`/`getName()` in CE) is replaced by the port's own `Nameable`-based
`IWorldRenameable`, already 1.21-correct with no changes needed.

### 5. `BlockEntityType` / `DeferredRegister` registration shape — confirmed from this port's own working code

Not invented: `com.hbm.blocks.ModBlocks` already declares
`DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID)`,
and `GenericCrateBlocks`/`GenericDecoBlocks` already register concrete block entities with
`ModBlocks.BLOCK_ENTITY_TYPES.register("name", () -> BlockEntityType.Builder.of(MyBlockEntity::new,
blockSupplier.get()).build(null))`. This is the confirmed, already-working pattern every future
Phase 2 machine `BlockEntity` should follow; this package does not need to add any new concrete
`BlockEntityType`s itself (`BlockDummyable` is abstract and has no `BlockEntity` of its own — only
concrete casings' core variant does).

### 6. `MenuProvider`/`SimpleMenuProvider` open-GUI contract — confirmed from the existing skeleton

`BlockDummyable.standardOpenBehavior` (already written) calls
`player.openMenu(new SimpleMenuProvider(menu, menu.getDisplayName()), corePos)` when
`level.getBlockEntity(corePos) instanceof MenuProvider menu`. This is real, unmodified vanilla 1.21
API and is the correct, minimal contract for the multiblock framework: it only requires a concrete
core `BlockEntity` to implement `MenuProvider` to get a working right-click-open, with zero
dependency on any bespoke GUI framework. This confirms the "GUI framework" gap flagged in Deferred
scope is a real prerequisite for *machines*, not for this package.

## Open questions / risks

- **Block-entity-side capability registration for HE energy/fluid** (`RegisterCapabilitiesEvent
  #registerBlockEntity` or equivalent) has no confirmed usage anywhere in this port or in Neo Edition
  — only item-side `registerItem` is confirmed (`ModCapabilities`). The first Phase 2 package that
  wires a machine `BlockEntity` into `PowerNetMK2`/`IEnergyConductorMK2` or the fluid tank
  abstraction will need to establish this pattern for real (by reading the actual NeoForge
  `RegisterCapabilitiesEvent` class once that package is worked, not by guessing here). Flagging now
  so it isn't silently assumed later.
- **`bb_fensu0.mbb` binary asset** exists in `hbm-ce/src/main/resources` but has not been copied into
  this port's resources yet. It is a binary blob (bounding-box dump), not hand-authored content, so
  copying it verbatim is safe and lossless, but it needs to actually happen before `MachineFENSU`
  (the one `BlockDummyableMBB` subclass) works at runtime — a `gradlew` compile check wouldn't catch
  a missing resource anyway, and this sandbox can't run `gradlew` to verify it either way.
  Flagging as a task-list item, not something this research package can self-verify further.
  Recommend keeping `MultiblockBBHandler` as a small, separate class from `MultiblockHandlerXR`
  exactly as CE does — only `MachineFENSU` needs it, and it should not become an assumed dependency
  for the other 149 `BlockDummyable` casings.
- **Orphan-cascade + `neighborChanged` interaction with piston pushes.** The port's `BlockDummyable`
  already overrides `neighborChanged(..., boolean movedByPiston)` and ignores the flag (matching CE,
  which has no piston-awareness at all here). This is a faithful CE-parity choice already baked into
  the existing skeleton, not something introduced by this package — noting it only because a piston
  pushing a multiblock casing was never well-defined in CE either, so no fix is expected here.
  Downstream reviewers should not flag this as a Phase 2 regression; it's pre-existing CE behavior.
  Not actionable, informational only.
  Verified by reading the already-written `BlockDummyable.java`; no new risk introduced by this
  package's changes.
- **`ItemAnalyzer`/`ItemAnalysisTool`, `ItemSettingsTool`, `ItemPowerNetTool`, etc.** (flagged in
  `docs/phase1/items_tool.md`'s bucket (c) as "Phase 2 machine coupling") all reach into
  `BlockDummyable.findCore`/`ICopiable`/`getSettings`/`infoForDisplay` — all of which are already
  correctly implemented in the port's existing `BlockDummyable.java`. Once this package lands, those
  Phase-1-deferred items become portable too; worth relaying back to whichever package owns
  `items/tool` follow-up.
- **`MultiblockBBHandler`'s unused `blocks` field.** CE's `MultiblockBounds` carries both a flat
  `boxes` array and a `Map<BlockPos, AABB[]> blocks` (a precomputed per-position box map), but the
  port's own `BlockDummyableMBB.rasterizeFootprint` only ever reads `.boxes` and recomputes the
  per-position rasterization itself every call. This report's `MultiblockBBHandler` design keeps
  parsing both (the `.mbb` binary format contains both regardless), but only `boxes` is
  load-bearing for this port — confirmed by reading the only consumer. Not a bug to fix, just a note
  so a future reviewer doesn't assume the unused field is a missed wiring point.
