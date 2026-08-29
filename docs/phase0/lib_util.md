# Phase 0 research report: Core lib & util (`com.hbm.lib`, `com.hbm.util`)

Scope covered: `hbm-ce/src/main/java/com/hbm/lib/**` (excluding `ModDamageSource.java`, owned by
another area) and `hbm-ce/src/main/java/com/hbm/util/**`. This is a read-only research pass; ~100
files total. Given the volume, files were sampled broadly (full read for small/load-bearing files,
header + import scan + targeted reads for the rest) rather than read end-to-end. The partial Neo
Edition port (`neo-edition/src/main/java/com/hbm/{lib,util}`) already has real, working NeoForge
21.1 equivalents for a good fraction of these classes and was used throughout to confirm API shapes
- those findings are called out explicitly below.

## 1. Class inventory

### `com.hbm.lib` (root package, 17 files)

| File | Purpose | Port verdict |
|---|---|---|
| `Library.java` | ~2500-line "god class" of static helpers: ray tracing (block/entity/cone), AABB math, chest-loot rolling, NBT-compatibility comparison, energy transfer helpers, string/number formatting (`getShortNumber`, `getColoredMbPercent`), block-placement helpers, and more. Marked `@Spaghetti("this whole class")` by the original authors. | **Split, don't port 1:1.** Genuinely load-bearing (imported everywhere), but it mixes at least 6 unrelated concerns and depends on `ForgeDirection`, `IEnergyConnectorMK2`/`IFluidConnectorMK2` (other areas' APIs), NBT, and client-only texture lookup (`getColorFromItemStack`/`getColorFromResourceLocation` call `Minecraft.getMinecraft()`, must move to a client-only helper or be dropped in favor of Mojang's texture atlas API). Recommend porting the pure-math/ray-trace/number-formatting parts verbatim into `Library`, and moving the client-texture and NBT-comparison pieces out as callers are ported in later phases. |
| `ForgeDirection.java` | 1.7.10-era 7-value direction enum (adds `UNKNOWN` to the 6 Minecraft directions) kept around because 1.12's `EnumFacing` lacks it. Provides opposite/rotation/degree helpers. | **Drop.** `net.minecraft.core.Direction` in 1.21 is functionally sufficient for everything this mod uses it for; Neo Edition confirms this - it has no `ForgeDirection` equivalent at all and just uses `Direction` directly (see `util/fauxpointtwelve/DirPos.java`, which takes a `Direction` parameter). Every CE call site that consumes `ForgeDirection` should be re-pointed at `Direction` during content porting. |
| `DirPos.java` | Tiny value holder pairing a `BlockPos` with a `ForgeDirection` (used for "check the block N blocks away in direction D" machine-multiblock logic). | **Port**, using `Direction` instead of `ForgeDirection`. Neo Edition already has this exact class at `util/fauxpointtwelve/DirPos.java`, extending a `BlockPosNT` value type and using `net.minecraft.core.Direction` - follow that shape (see note on `Vec3NT`/`BlockPosNT` below). |
| `CapabilityContextProvider.java` | Thread-local `BlockPos` holder used to let a Forge capability implementation know "which accessor position asked for me" (ports 1.7.10 `IConditionalInvAccess`). | **Port as-is.** Pure Java, no Minecraft/Forge imports beyond `BlockPos`. NeoForge capability provider lambdas can still consult a thread-local exactly the same way. |
| `HBMSoundHandler.java` | Static registry of ~800 lines of `SoundEvent` fields plus one `Object2ObjectLinkedOpenHashMap<ResourceLocation, SoundEvent>`, and (per a grep of usages) the registration entry point. | **Port, converted to `DeferredRegister<SoundEvent>`.** This is purely a big field list + a registration call; mechanical to port. Marked `//TODO: rename to NTMSounds` upstream - not our call to rename, preserve the name. |
| `HbmChestContents.java` | Static factory helpers wrapping `WeightedRandomChestContentFrom1710` construction, plus an `ItemBookLore` "office book" generator. | **Partially in scope.** The `weighted(...)` factories are trivial to port once `WeightedRandomChestContentFrom1710` (owned by `com.hbm.handler`, another area) is ported. Actual loot-table wiring depends on 1.21's loot table system vs. this mod's custom weighted-chest system - flag as a cross-area decision (see Risks). |
| `HbmCollection.java` | Static `enum` constants for flavor data (gun manufacturers, etc.) used purely for translation-key lookups. | **Port as-is.** Zero Minecraft dependency. |
| `HbmWorld.java` | 50-line entry point registering world generators (`MapGenStructureIO`, `IWorldGenerator`) and delegating to `HbmWorldGen`. | **Out of scope for lib_util's literal port, but the registration *pattern* matters.** World generation is a distinct system (owned by whichever area gets `com.hbm.world.*`); flagging the dependency rather than porting the 1.12 `IWorldGenerator`/`MapGenStructureIO` APIs (both gone in 1.21, replaced by data-driven `ConfiguredFeature`/`PlacedFeature`/structure JSONs). |
| `HbmWorldGen.java` | 755 lines of world-gen tuning constants and ore/structure spawn-rate config glue. | Same as `HbmWorld.java` - cross-area (worldgen), not ported here. |
| `InventoryHelper.java` | `dropInventoryItems(World, BlockPos, ICapabilityProvider)` - spills an `IItemHandler`'s contents as `EntityItem`s when a block breaks. | **Port**, retargeted to NeoForge's `Capabilities.ItemHandler.BLOCK` capability lookup and `ItemEntity`. Small, single-purpose, genuinely load-bearing (every inventory-holding tile entity calls this on break). |
| `ItemStackHandlerWrapper.java` | `IItemHandlerModifiable` wrapper around a plain `ItemStackHandler` that restricts external access to a whitelist of slot indices (`validSlots`). | **Port as-is**, using NeoForge's `ItemStackHandler`/`IItemHandlerModifiable` (same package names survive from Forge to NeoForge for the capability-adjacent inventory helper classes - confirm exact package during implementation via the Neo Edition reference, since it's not present there today). |
| `NTMBlockContainer.java` | Thin `BlockContainer` subclass base class in 1.12's block-with-NBT model. | **Drop.** `BlockContainer`/tile-entity-having-block distinction doesn't exist the same way in 1.21 (`EntityBlock` interface is mixed into a normal `Block` subclass); this base class has no 1.21 equivalent shape and must be redesigned per-block by whichever area ports the block hierarchy, not mechanically translated. |
| `ObjObjDoubleConsumer.java` | `(T, U, double) -> void` functional interface. | **Port as-is.** Pure Java. |
| `ObjectDoubleFunction.java` | `Function<T, Double>` specialization avoiding boxing on the call side. | **Port as-is.** Pure Java. |
| `RecoilHandler.java` | Client-only camera-shake handler consuming `EntityViewRenderEvent.CameraSetup`. | **Port, retargeted to NeoForge's `ViewportEvent.ComputeCameraAngles`** (the 1.21 NeoForge successor event - verify exact name against the Neo Edition client event-bus usage before implementing, not present in the sampled Neo Edition `lib`/`util` trees). Small and self-contained. |
| `TLPool.java` | Generic thread-local + shared JCTools `MpmcArrayQueue`-backed object pool. | **Port as-is.** Pure Java, depends only on `org.jctools` (already a CE dependency; confirm it's on the port project's classpath). |
| `TriFunction.java` | `(T, U, V) -> R` functional interface (JDK has no 3-arg `Function`). | **Port as-is.** Pure Java. |

### `com.hbm.lib.internal` (8 files) - Unsafe/MethodHandle plumbing

`AbstractUnsafe`, `InternalUnsafeWrapper`, `SunUnsafeWrapper`, `UnsafeHolder`, `InternalUtil`,
`MethodHandleHelper`, `TrustedLookupAccessor`, `package-info.java`.

Pure-Java reflection/`sun.misc.Unsafe`/`jdk.internal.misc.Unsafe` abstraction layer with zero
Minecraft/Forge imports (only `com.hbm.core.HbmCorePlugin` and `com.hbm.interfaces.*`, both this
mod's own code). Used to back the off-heap bitsets and non-blocking maps below with fast field
access.

**Port as-is, with one real risk**: this code does version-sniffing and reflective access to JDK
internals (`jdk.internal.misc.Unsafe`, module-open tricks in `InternalUtil`) written against
whatever JDK the mod targeted historically. Java 21's module system is the same as Java 17-20 in
this respect, so nothing new should break, but this subpackage is exactly the kind of code that
silently depends on JVM implementation details - it must be smoke-tested standalone (a tiny
`main()` calling `UnsafeHolder.U`) before other code relies on it, not just assumed to work.

### `com.hbm.lib.maps` (7 files) - lock-free collections

`NonBlockingHashMap`, `NonBlockingHashMapLong`, `NonBlockingHashSetLong`,
`NonBlockingLong2LongHashMap`, `ConcurrentAutoTable`, `AbstractEntry`, `RangeUtil`, plus 3 tiny
functional interfaces (`LongObjectBiFunction`, `LongObjectConsumer`, `LongObjectRefConsumer`).

Cliff Click's classic non-blocking hash map family (Apache-2.0 licensed, third-party-origin code
vendored into the mod), built on `com.hbm.lib.internal`'s Unsafe wrapper for CAS operations on
array slots. Zero Minecraft/Forge dependency.

**Port as-is.** These are pure concurrency data structures; nothing about Minecraft 1.21 or
NeoForge affects them. Load-bearing wherever the mod needs a highly-concurrent map keyed by chunk
position or block position (long-packed keys), which is common in this mod's world-tick-heavy
systems (radiation, cable/pipe networks).

### `com.hbm.lib.queues` (4 files) - lock-free long queues

`MpUnboundedXaddArrayLongQueue` (package-private base), `MpmcUnboundedXaddArrayLongQueue`,
`MpscLinkedAtomicLongQueue`, `MpscUnboundedXaddArrayLongQueue`.

JCTools-style MPMC/MPSC unbounded queues specialized for primitive `long` (avoids boxing).
Depends on `com.hbm.lib.Library` (for `SPIN_WAITER`) and `com.hbm.lib.internal.UnsafeHolder`.

**Port as-is** once `Library` and the `internal`/`maps` packages are ported. Zero direct
Minecraft/Forge dependency.

### `com.hbm.util` (69 files)

A grep for `net.minecraft`/`net.minecraftforge` imports across all 69 files classified them into
two buckets (full listing below); this is the fast triage the CLAUDE.md file's "sample broadly"
instruction calls for, followed by close reads of the files most likely to be load-bearing or to
carry NBT-on-ItemStack logic (the hard rule this scope report must call out).

**No Minecraft/Forge import at all (14 files, port essentially unchanged):**
`AdjacencyGraph.java`, `Calculator.java`, `ConcurrentBitSet.java`, `DecodeException.java`,
`Either.java`, `ExponentialMovingAverage.java`, `MpscCollector.java`,
`MpscIntArrayListCollector.java`, `ObjectIntPair.java`, `ObjectPool.java`, `OffHeapBitSet.java`,
`RandomPool.java`, `ReferenceIntTuple.java`, `SectionKeyHash.java`, `ShadyUtil.java`,
`SwappedHashSet.java`, `Tuple.java`, `GuiUtil.java` (7-line mouse-boundary-check helper, actually
pure Java despite living in `util` next to GUI code).

Note: `ShadyUtil.java` and `Tuple.java` are already ported unchanged in Neo Edition
(`util/ShadyUtil.java`, `util/Tuple.java`), confirming these need no adaptation.

**Minecraft/Forge-dependent (55 files) - representative samples read closely:**

| File | Purpose | Port verdict |
|---|---|---|
| `ItemStackUtil.java` | `carefulCopy*` family, `addTooltipToStack` (writes a `display.Lore` NBT tag), `addNBTFromString` (parses a JSON string straight onto the stack's NBT tag compound), `addStacksToNBT`/`readStacksFromNBT` (packs an `ItemStack[]` into a custom `"items"` NBT list keyed by slot byte), ore-dict helpers, `ComparableStack` factories. | **Port, rewritten around Data Components - see the NBT->component mapping table below.** Neo Edition already has a full rewrite of this exact class (`neo-edition/util/ItemStackUtil.java`) that is the concrete confirmation of the pattern: `addTooltipToStack` now goes through a `TagsUtil.getCustomData(stack)` helper backed by `DataComponents.CUSTOM_DATA` / `CustomData.of(tag)`, and `addStacksToNBT`/`readStacksFromNBT` now take a `HolderLookup.Provider`/`RegistryAccess` parameter (`ItemStack.save`/`ItemStack.parse` require registry access in 1.21, unlike 1.12's raw NBT constructor). `areStacksCompatible` becomes `ItemStack.isSameItemSameComponents(a, b)` (a real, existing 1.21 API). |
| `EnumUtil.java` | `grabEnumSafely` (index-mod-length safe enum lookup), plus cached `EnumHand`/`EntityEquipmentSlot` value arrays. | **Port**, dropping the pre-cached `EntityEquipmentSlot` array field (1.21's equivalent is `EquipmentSlot`, and Neo Edition's `EnumUtil.java` shows the ported class keeps only the generic `grabEnumSafely` helper, not per-enum caches - the caching was working around 1.12 API overhead that no longer applies the same way). |
| `FacingUtil.java` | `getPitch`/`getYaw` for `EnumFacing` -> radians, used for entity-facing render math. | **Port**, `EnumFacing` -> `Direction`. Trivial. |
| `Keypad.java` / `KeypadClient.java` | Numeric-keypad puzzle/lock minigame state machine (used by vault-door-style blocks): per-tile-entity button state, network sync via a hand-rolled `ThreadedPacket`. | **In scope for structure, out of scope for the packet system.** `Keypad`'s own state (button cooldowns, `storedCode`, `code[]`) is plain Java and ports unchanged; its `update()` method's packet-send call must be re-pointed at whatever this port's networking layer becomes (NeoForge's `PayloadRegistrar`/custom `CustomPacketPayload`), which is another area's responsibility - note the dependency, don't invent the networking API here. |
| `ContaminationUtil.java` | Radiation-resistance calculation reading a `"hbmradmultiplier"` float and `"ntmNeutron"` marker off `entity.getEntityData()` (Forge's per-entity persistent NBT tag), plus a hard-coded immune-entity-class list. | **NBT key is on the *entity*, not an `ItemStack` - out of the DataComponentType rule's scope**, but still needs an NBT replacement: 1.21/NeoForge's answer for "arbitrary persistent per-entity data" is `AttachmentType` (`net.neoforged.neoforge.attachment`), not raw NBT. Recommend a `RAD_MULT` and `NTM_NEUTRON` `AttachmentType<Float>`/`AttachmentType<Boolean>` pair. Neo Edition has already ported `ContaminationUtil.java` in full; that file should be read closely (not just sampled) before implementing this class, since it is one of the largest and most cross-cutting files in the whole `util` package (676 lines in CE, pulls in hazard/radiation/potion/entity systems from many other areas). |
| `DamageResistanceHandler.java`, `EntityDamageUtil.java` | Custom damage-type/resistance math layered on top of vanilla `DamageSource`. | **Cross-area heavy** (touches armor, hazard, and entity systems owned elsewhere) but Neo Edition has already ported both (`neo-edition/util/DamageResistanceHandler.java`, `EntityDamageUtil.java`) - use those as the concrete guide for how 1.12's `DamageSource` static factories map onto 1.21's `Holder<DamageType>`/`DamageSources` registry-driven model, which is a real, non-trivial API shape change (1.21 damage types are data-driven registry entries, not static fields). |
| `CompatFluidRegistry.java`, `CompatDynamicTrees.java`, `CompatExternal.java`, `CompatBlockReplacer.java`, `Compat.java` | Soft-dependency glue for other mods (Dynamic Trees) and this mod's own `FluidType`/`Fluids` -> Forge `Fluid` bridging. | **`CompatFluidRegistry` is high-risk and cross-area**: it calls `Fluids.setupForgeFluidCompat(...)`, bridging this mod's own custom fluid system onto Forge's `Fluid`/`FluidStack`. NeoForge 1.21's fluid system is substantially different (data-driven `FluidType` registry entries that unfortunately share a class name with this mod's own `com.hbm.inventory.fluid.FluidType` - a naming collision to flag loudly to whoever owns the fluid-system area). Not portable without that area's design decided first; `CompatDynamicTrees`/mod-compat classes should likely be dropped for Phase 0 (no confirmed Dynamic Trees 1.21 NeoForge build to compat against) and revisited later. |
| `UnlistedPropertyBoolean.java`, `UnlistedPropertyInteger.java` | 1.12 Forge "unlisted block state property" (`IUnlistedProperty`) implementations, used to pass non-rendering-relevant state to a block's model without it being part of the real `IBlockState`. | **Drop.** The unlisted-property mechanism doesn't exist in 1.21's `BlockState`/`BlockStateProperties` model at all; any block that used these needs a redesign (real `BlockState` property, a `BlockEntity`-held value read at render time, or a render-data component) decided per-block by the blocks area, not a mechanical port. |
| `RenderUtil.java`, `ShaderHelper.java`, `ParticleUtil.java`, `FontRendererUtil.java`, `OptifineHooks.java` | Client-only rendering helpers (`GlStateManager`-era immediate-mode GL calls, shader program binding, particle spawning, font-rendering wrappers, OptiFine shader-pack compatibility hooks). | **Out of scope for Phase 0's core-lib port.** 1.21's rendering pipeline (`GuiGraphics`, `VertexConsumer`/`RenderType`, no more `GlStateManager` immediate mode, no OptiFine-as-a-jar-hook since Iris/Sodium's NeoForge compat works differently) makes these unportable line-by-line; they need a from-scratch rewrite once the rendering area's foundations exist. Flagging as dead weight for *this* phase, not deleting the requirement - a later phase must redo them. |
| `Vec3NT.java` | Mutable double-precision 3D vector type (1.12 predates a good mutable `Vec3d`). | **Port, but check against `Vec3` (immutable) and consider whether the mutability is still needed.** Neo Edition ported this (`neo-edition/util/Vec3NT.java`) and also introduced a `BlockPosNT`/`fauxpointtwelve` package for "1.12-shaped position helper types that 1.21 doesn't provide the same way" - follow that same package convention for any similarly-shaped helper (see `DirPos` above, which now lives under `util/fauxpointtwelve` in Neo Edition, not directly under `lib`). |
| `ChatBuilder.java`, `Clock.java`, `I18nUtil.java` | Chat/component text-building helper, a tick-based stopwatch, translation-key formatting. | **Port**, all three already exist in Neo Edition in ported form (`ChatBuilder.java`, `Clock.java` at top level; `I18nUtil.java` moved under a new `util/i18n` package split into `I18nClient`/`I18nServer`/`ITranslate` for client/server-safe translation - `net.minecraft.util.text.TextComponentTranslation` from 1.12 is gone, replaced by `Component.translatable(...)`, and 1.21's server side cannot resolve player-locale translations client-side the way 1.12 pretended to, hence the client/server split). Follow the Neo Edition i18n package split rather than a flat `I18nUtil` port. |
| Remaining ~35 files (`ArmorRegistry`, `BobMathUtil`, `BufferUtil`, `ChunkShapeHelper`, `ChunkSpanAccumulator`, `ChunkUtil`, `ColorUtil`, `CrashHelper`, `CrucibleUtil`, `DelayedTick`, `EnchantmentUtil`, `InventoryUtil`, `LootGenerator`, `MobUtil`, `MutableVec3d`, `NetworkUtil`, `NoteBuilder`, `RTGUtil`, `SoundUtil`, `TrackerUtil`, `Vec3dUtil`, `WeightedRandomGeneric`, `WeightedRandomObject`) | Assorted single-purpose helpers: armor hazard-class registry, geometry/math helpers, chunk-shape/section helpers, color math, crash-report enrichment, crucible-recipe lookup, delayed-tick scheduling, enchantment queries, inventory scanning, loot generation, mob targeting, note-block sound building, RTG decay math, sound-event dispatch, entity/chunk tracking, weighted-random pickers. | **Mechanical per-file port**, moderate risk overall: each is individually small (17-880 lines) and single-purpose, but collectively they are the long tail that touches almost every 1.12-only API surface (`WeightedRandom`, `EnumFacing`, `IBlockState`, `NBTTagCompound`, Forge's old capability/oredict APIs). Neo Edition already has working ports for at least `ArmorRegistry`, `BobMathUtil`, `InventoryUtil`, `WeightedRandom` (renamed from `WeightedRandomObject`/`WeightedRandomGeneric`), `SoundUtils` (renamed from `SoundUtil`) - diff against those before writing each one from scratch. |

## 2. Key responsibilities

- **Concurrency primitives** (`lib.internal`, `lib.maps`, `lib.queues`, plus `util.ConcurrentBitSet`,
  `util.OffHeapBitSet`, `util.MpscCollector*`) - the mod's own vendored lock-free data structures,
  used to make hot per-tick systems (radiation propagation, cable/pipe networks) scale across
  threads without heavyweight locking. Zero Minecraft coupling; the biggest, most self-contained,
  and lowest-risk chunk of this area's file count.
- **Direction/position value types** (`ForgeDirection`, `DirPos`, `Vec3NT`, `MutableVec3d`,
  `Vec3dUtil`) - 1.12-era stand-ins for geometry types that 1.21 now provides natively
  (`Direction`, `Vec3`) or provides better-shaped equivalents for. Small in code volume but
  referenced from essentially every tile entity and block in the mod, so getting the replacement
  types right early matters more than the file count suggests.
- **Item/inventory helpers** (`ItemStackUtil`, `InventoryHelper`, `ItemStackHandlerWrapper`,
  `InventoryUtil`) - the mod's abstraction over Forge's `IItemHandler` capability system, and the
  one place this scope directly intersects the hard "no raw ItemStack NBT" rule.
- **The `Library` god class** - genuinely load-bearing (ray tracing, chest loot, energy-transfer
  glue, number formatting) but violates single-responsibility badly enough that a literal 1:1 port
  would just relocate the design problem into the new codebase.
- **Client rendering helpers** (`RenderUtil`, `ShaderHelper`, `ParticleUtil`, `FontRendererUtil`,
  `OptifineHooks`, `RecoilHandler`) - explicitly out of scope for a mechanical Phase 0 port; the
  rendering pipeline changed too much between 1.12 and 1.21 for these to survive translation.
- **Sound registry** (`HBMSoundHandler`) - mechanical `DeferredRegister<SoundEvent>` conversion.
- **World-gen glue** (`HbmWorld`, `HbmWorldGen`) - not portable as-is (1.12's `IWorldGenerator`/
  `MapGenStructureIO` have no 1.21 equivalent shape at all - worldgen is now entirely data-driven),
  and belongs to whichever area owns `com.hbm.world.*` content, not this area.

## 3. Cross-area dependencies

- `Library.java` imports `com.hbm.api.conveyor`, `com.hbm.api.energymk2`, `com.hbm.api.fluidmk2`,
  `com.hbm.capability.*`, `com.hbm.entity.*`, `com.hbm.blocks.ModBlocks`, `com.hbm.items.ModItems`,
  `com.hbm.tileentity.TileEntityMachineBase` - it cannot be fully ported (compiled) in isolation;
  only its self-contained math/formatting methods can be verified standalone in Phase 0, the rest
  needs those other areas' registries to exist first.
- `HbmChestContents.java` depends on `com.hbm.handler.WeightedRandomChestContentFrom1710` (handler
  area) and `com.hbm.items.special.ItemBookLore` (items area).
- `Keypad.java`/`KeypadClient.java` depend on `com.hbm.handler.threading.PacketThreading` and
  `com.hbm.packet.*` (networking area) for their sync mechanism.
- `ContaminationUtil.java`, `DamageResistanceHandler.java`, `EntityDamageUtil.java` depend heavily
  on `com.hbm.capability.HbmLivingCapability`, `com.hbm.hazard.*`, `com.hbm.potion.HbmPotion`,
  `com.hbm.entity.*` (capability, hazard, and entity areas).
- `CompatFluidRegistry.java` depends on `com.hbm.inventory.fluid.FluidType`/`Fluids` (fluid-system
  area) and surfaces a real naming collision against NeoForge's own `FluidType` type that the fluid
  area needs to resolve (rename one of the two, or fully qualify every usage).
- `lib.queues.*` depend on `lib.Library` (for `SPIN_WAITER`) and `lib.internal.UnsafeHolder` -
  purely intra-area, no external risk.
- `HbmWorld.java`/`HbmWorldGen.java` depend on `com.hbm.world.gen.*`, `com.hbm.saveddata.*`,
  `com.hbm.tileentity.bomb.TileEntityLandmine` (world-gen and tile-entity areas).

## 4. Recommended NeoForge/Java 21 port plan

1. **Port the zero-dependency tiers first, in this order**: `lib.internal` -> `lib.maps` ->
   `lib.queues` -> the 14 no-import `util` files -> `ObjObjDoubleConsumer`/`ObjectDoubleFunction`/
   `TriFunction`. These compile standalone today (only need `org.jctools`, `it.unimi.dsi.fastutil`
   on the classpath, both already CE dependencies) and give immediate regression-testable value.
2. **Port the geometry value types next**: `Direction`-based `DirPos` (drop `ForgeDirection`
   entirely, per Neo Edition's confirmed precedent), decide on `Vec3NT`/`MutableVec3d`'s fate
   against 1.21's `Vec3` (Neo Edition kept a ported `Vec3NT`, suggesting the mutability is still
   valued somewhere - confirm against actual call sites once other areas port their tile entities).
3. **Port `ItemStackUtil` around Data Components**, following the Neo Edition file as the concrete
   template, with the NBT-key mapping table below. This is the one file in this whole scope that
   the hard "Data Components, not raw NBT" rule bites hardest on.
4. **Port `HBMSoundHandler`** as a `DeferredRegister<SoundEvent>` holder class exposing a public
   static `register(IEventBus modEventBus)` method for the integration step to call from
   `MainRegistry`.
5. **Triage `Library.java` by method, not as a monolith**: port the pure math/ray-trace/formatting
   methods (roughly the first third of the file - `getColor*`, `getShortNumber`, `roundFloat`,
   `rayTrace*`, `isBoxCollidingCone`, `smoothstep`, `getEuler`) into a ported `Library` class now;
   leave a documented TODO list (not stub methods - per the hard rules, no half-finished
   implementations) of the remaining methods that need other areas' registries, to be finished once
   those areas land.
6. **Defer the client-rendering cluster** (`RenderUtil`, `ShaderHelper`, `ParticleUtil`,
   `FontRendererUtil`, `OptifineHooks`, `RecoilHandler`) and **the world-gen cluster** (`HbmWorld`,
   `HbmWorldGen`) entirely - they are not mechanically portable and belong to later phases /
   different areas.
7. **Drop outright** (no 1.21 equivalent shape, no salvageable logic): `ForgeDirection`,
   `NTMBlockContainer`, `UnlistedPropertyBoolean`, `UnlistedPropertyInteger`.
8. For every remaining `util` file (the ~35-file "mechanical per-file port" tail), **diff against
   the Neo Edition file of the same name before writing it**, since a meaningful fraction already
   have a working, real port to confirm the exact API surface against - only fall back to reasoning
   from CE + general NeoForge 21.1 conventions where no Neo Edition file exists.

### NBT key -> Data Component mapping (from `ItemStackUtil`, the only file in this scope that
writes NBT directly onto an `ItemStack`)

| CE NBT key / structure | Current shape | Recommended 1.21 component |
|---|---|---|
| `display.Lore` (a `NBTTagList` of `NBTTagString`, written by `addTooltipToStack`) | Free-form tooltip lines | Prefer vanilla `DataComponents.LORE` (`ItemLore`, a real Mojang component built exactly for this) over Neo Edition's `CUSTOM_DATA` fallback - it is more idiomatic and gets automatic tooltip rendering for free. Neo Edition's own `TagsUtil`-based `CUSTOM_DATA` approach works but is a pragmatic shortcut, not the ideal target; flag this as worth revisiting even in the reference project. |
| `items` (an `NBTTagList` of `{slot: byte, <itemstack fields>}` compounds, read/written by `addStacksToNBT`/`readStacksFromNBT`) | A fixed-size, slot-indexed array of stacks packed onto a *different* item's NBT (e.g. a multi-block "blueprint" or construction-preview item referencing several stacks) | No single vanilla component fits (vanilla's `ItemContainerContents` doesn't preserve arbitrary/sparse slot indices the way this code does). Needs a bespoke `DataComponentType<SlottedItemList>` with a `Codec`/`StreamCodec` pair, where `SlottedItemList` is a small record wrapping `Map<Integer, ItemStack>` or a fixed-size `ItemStack[]`. This is new code, not a mechanical translation - flag for explicit design review before implementation. |
| Arbitrary caller-supplied JSON via `addNBTFromString` | Unsafe passthrough onto the stack's whole NBT tag | This method's entire premise (blind raw-NBT injection) conflicts with the data-components model. Neo Edition kept it as a `CUSTOM_DATA`-backed escape hatch for debug/creative tooling; recommend keeping it narrowly scoped the same way (never called from real gameplay logic) rather than porting it as a general-purpose API. |
| `hbmradmultiplier` (float, entity persistent data, `ContaminationUtil`) | Entity-level, not item-level | Not a Data Component candidate at all - it's entity data. Migrate to a NeoForge `AttachmentType<Float>` on the entity. Noted here because it's easy to conflate with the ItemStack rule, but it's a different mechanism. |
| `ntmNeutron` (marker, entity persistent data, `ContaminationUtil`) | Entity-level | Same as above - `AttachmentType<Boolean>` (or fold into a single richer attachment record alongside the rad multiplier). |

## 5. Risks and open questions

- **`Library.java` is a 2500-line god class with `@Spaghetti("this whole class")` self-annotation.**
  A literal port would import the design problem wholesale into the new codebase, violating the
  "can it be simpler / is this abstraction needed" architecture questions from the project's
  engineering standards. Recommend the method-by-method split described in the port plan, decided
  with whoever ends up owning the bulk of `Library`'s call sites (likely the energy/fluid/machine
  areas), not unilaterally by this area.
- **`FluidType` naming collision**: this mod's own `com.hbm.inventory.fluid.FluidType` and
  NeoForge's own `net.neoforged.neoforge.fluids.FluidType` share a simple name. `CompatFluidRegistry`
  sits exactly at that collision point. Needs a decision from the fluid-system area before any
  fluid-adjacent util code can be finished; flagging now so it isn't discovered late.
- **`sun.misc.Unsafe`/`jdk.internal.misc.Unsafe` reflection in `lib.internal`** is exactly the kind
  of code that can silently break on a JDK/module-system detail that differs from whatever JDK the
  original authors tested against. Recommend an isolated smoke test (a throwaway `main()` exercising
  `UnsafeHolder.U`'s core operations) as part of implementation, not just "it compiles."
- **World generation (`HbmWorld`/`HbmWorldGen`) has no mechanical 1.21 translation** - 1.12's
  `IWorldGenerator`/`MapGenStructureIO` model is gone, replaced by fully data-driven
  `ConfiguredFeature`/`PlacedFeature`/JSON structure templates. This is a from-scratch redesign for
  whichever area/phase owns world generation, not a port; called out here only because the CE files
  physically live under this area's assigned scope.
- **Client rendering helpers (`RenderUtil`, `ShaderHelper`, `ParticleUtil`, `FontRendererUtil`,
  `OptifineHooks`) have no mechanical translation either** - 1.21's rendering pipeline
  (`GuiGraphics`/`VertexConsumer`/`RenderType`) is unrelated in shape to 1.12's `GlStateManager`
  immediate-mode calls. Same treatment: flagged as out-of-scope dead weight for Phase 0, not
  silently dropped from the project's requirements.
- **`Keypad`/`KeypadClient`'s networking dependency** can't be finished until the networking area
  decides on its `CustomPacketPayload` design; the state-machine logic itself is ready to port now.
- **The 55-file "Minecraft/Forge-dependent" `util` tail was triaged by import-scan plus targeted
  reads of ~15 representative files, not a full read of every file.** The recommendation to diff
  each remaining file against its Neo Edition counterpart (where one exists) is meant to catch any
  additional NBT-on-ItemStack usages or 1.12-only API calls that this sampling pass didn't
  individually surface - implementers should re-check for raw ItemStack NBT reads/writes in each
  file as they port it, not assume this report's single NBT-mapping table (section 4) is
  exhaustive for the whole 100-file scope.
