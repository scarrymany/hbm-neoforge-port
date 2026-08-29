# Phase 0 Research Report: HBM API & Interop Interfaces (`api_interfaces`)

Scope covered: `com.hbm.api.**` excluding `api.fluid`, `api.fluidmk2`, `api.energymk2` (owned by other areas),
and `com.hbm.interfaces.**` (37 files). All 60 CE source files in scope were read in full. Neo Edition's
partial port of the `interfaces` package (8 files) was cross-checked to confirm real, working NeoForge
21.1.228 / Minecraft 1.21.1 Mojang-mapped API usage.

## 1. Class inventory

### `com.hbm.api.block` (9 files)

| File | Purpose |
|---|---|
| `IBlockSideRotation.java` | Block asks for its facing/rotation index given world+pos+side; carries two `static` helper methods (`topToBottom`, `isOpposite`) that are pure integer lookup tables, not tied to 1.12 types. |
| `IBlowable.java` | Marker for blocks affected by fan/wind push (`applyFan`). |
| `ICrucibleAcceptor.java` | Contract for blocks that accept molten metal "pours" (from above) and "flows" (lateral, block-to-block), each with a `canAccept*` check + a mutating action returning the leftover `Mats.MaterialStack`. |
| `IDrillInteraction.java` | Contract between ore/rock blocks and drills: `canBreak`, `extractResource`, `getRelativeHardness`, all parameterized by an `IMiningDrill`. |
| `IExploder.java` | Callback for TNT-primed-entity-triggered explosions at a position; has a `default` overload converting `BlockPos` to raw doubles. |
| `IInsertable.java` | Simple item-insertion contract at a directional face. |
| `IMiningDrill.java` | Drill "identity" contract: tier enum (`PRIMITIVE/INDUSTRIAL/HITECH`) + numeric rating, consumed by `IDrillInteraction`. |
| `IPileNeutronReceiver.java` | Single-method marker for reactor-pile components that absorb neutron flux. |
| `IToolable.java` | Contract for blocks interactable with screwdriver/hand-drill/defuser/wrench/torch/bolt tools; nests a `ToolType` enum that self-registers ItemStacks and does a lazy reverse-lookup map keyed by a `ComparableStack` from `RecipesCommon`. |

### `com.hbm.api.conveyor` (4 files)

| File | Purpose |
|---|---|
| `IConveyorBelt.java` | Belt-surface physics contract: whether an item should stay on the belt, where it travels to, and belt-center snapping. |
| `IConveyorItem.java` | Wraps a single `ItemStack` for entities riding a conveyor. |
| `IConveyorPackage.java` | Wraps an `ItemStack[]` bundle (multi-item package) riding a conveyor. |
| `IEnterableBlock.java` | Contract for blocks a conveyor item/package can enter (can-enter check + on-enter callback, for both single items and packages). |

### `com.hbm.api.entity` (6 files)

| File | Purpose |
|---|---|
| `EntityGrenadeFactory.java` | `@FunctionalInterface` factory for spawning `IProjectile` grenades from a world+position, used by throwable-weapon registration tables. |
| `IRadarDetectable.java` | Older/legacy radar contract; nests a large `RadarTargetType` enum (missiles by tier, custom-missile sizes, mirvlet, player, artillery) each carrying a display name string. |
| `IRadarDetectableNT.java` | Newer radar contract superseding the above; uses raw `int` tier constants instead of an enum, adds `paramsApplicable`/`suppliesRedstone` gating via a static nested `RadarScanParams` (mutable flag bag: scanMissiles/scanShells/scanPlayers/smartMode). |
| `IResistanceProvider.java` | Lets custom entities supply damage-type/threshold/resistance/piercing values directly instead of going through the separate `DamageResistanceHandler`; also gets an `onDamageDealt` callback. |
| `IThrowable.java` | Sets the throwing `EntityLivingBase` on a projectile. Comment explicitly documents it must NOT depend on obfuscated Forge/Minecraft classes to remain usable pre-remap — this constraint is void in an official-mappings NeoForge project and can be dropped, but the interface method itself stays. |
| `RadarEntry.java` | Concrete DTO class (not an interface) carrying one radar blip's serializable state (name, blip level, xyz, dim, entityID, redstone flag) plus manual `ByteBuf` `fromBytes`/`toBytes` (Forge's legacy `ByteBufUtils`) and three convenience constructors from `IRadarDetectableNT`, `IRadarDetectable`, or a player. |

### `com.hbm.api.item` (3 files)

| File | Purpose |
|---|---|
| `IDepthRockTool.java` | Whether a tool ItemStack can break "depthrock" blocks, gated by world/player/block/position. |
| `IDesignatorItem.java` | Target-designator item contract (e.g., laser designator for missile launch pads): readiness check + coordinate resolution, both keyed off launch-pad position. |
| `IGasMask.java` | Gas mask filter contract: hazard-class blacklist, get/install/damage a filter `ItemStack` stored in the mask's data. **NBT->component migration target** (see section 4). |

### `com.hbm.api.network` (1 file)

| File | Purpose |
|---|---|
| `IPacketRegisterListener.java` | Callback invoked at the end of `PacketDispatcher.registerPackets()`, returns the next free packet id. This entire packet-id-counter pattern is a Forge-1.12 `SimpleNetworkWrapper` idiom; NeoForge 1.21 uses the payload-registration API (`PayloadRegistrar` / `IPayloadHandler` keyed by `ResourceLocation`+version, no integer ids). This interface's *contract* (a listener hook fired during packet registration) is portable, but its `int nextId` return value is not — the replacement should thread through a `PayloadRegistrar` reference instead. Flagged as a cross-area concern for whichever area owns networking/`PacketDispatcher`. |

### `com.hbm.api.ntl` (4 files)

| File | Purpose |
|---|---|
| `IPneumaticConnector.java` | Marker + one `default` method deciding if a given `ForgeDirection` face can connect to the pneumatic network (rejects `UNKNOWN`). |
| `ISlotMonitorProvider.java` | Storage tile-entity contract exposing its slots as `SlotMonitor[]` to the "access terminal"/pneumatic network system; also mediates item add/remove/type-set operations using `long` amounts (supports int64-scale storage beyond `ItemStack`'s `int` count) and reachability to a given `StackCache`. Has several `default` no-op methods for storages that don't support type-setting. |
| `SlotMonitor.java` | Concrete class: per-slot change-detection node. Tracks `item/meta/nbt/stacksize`, diff-checks against the live inventory each tick (`checkUpdate`), and pushes deltas into every `CacheSlot` (from `StackCache`) that currently "views" it, joining/leaving cache slots as the underlying network's `PneumaticNetwork.accessors` set changes. Uses `net.minecraft.nbt.NBTTagCompound` and `Item`/`ItemStack` (1.12) directly for identity comparison. |
| `StackCache.java` | Concrete class: per-endpoint (e.g. access terminal) aggregation of all `SlotMonitor`s reachable from it, keyed by a computed `long` stack identity (`getStackIdentity`, a hand-rolled hash over item id, meta, and NBT string form — collision-prone but presumably deemed good enough). Nests `CacheSlot`, an inner class representing one item-type's combined virtual stack across all backing monitors, with add/consume-and-return-quantity helpers. |

### `com.hbm.api.recipe` (1 file)

| File | Purpose |
|---|---|
| `IRecipeRegisterListener.java` | Callback fired once per `SerializableRecipe` type during that recipe system's `initialize()`, letting listeners inject/patch recipes of a named type before the template is finalized. |

### `com.hbm.api.redstoneoverradio` (4 files)

| File | Purpose |
|---|---|
| `IRORInfo.java` | Base contract for "Redstone-over-Radio" components: exposes `getFunctionInfo()` plus two constant string prefixes (`VAL:`/`FUN:`) used by the wire-format parser. |
| `IRORInteractive.java extends IRORInfo` | Adds `runRORFunction(name, params)` plus `static` parsing helpers (`getCommand`, `getParams`, `parseInt` with range clamping) that throw `RORFunctionException` on malformed input. Pure string-protocol logic, no Minecraft types at all — trivially portable verbatim. |
| `IRORValueProvider.java extends IRORInfo` | Read-only counterpart: `provideRORValue(name)` for querying a value without side effects. |
| `RORFunctionException.java` | Plain `RuntimeException` subclass used by the parsing helpers above. |

### `com.hbm.api.tile` (4 files)

| File | Purpose |
|---|---|
| `IHeatSource.java` | Reactor/machine heat-pool contract: query stored heat, consume some (implementation must clamp at zero). |
| `ILoadedTile.java` | Single-method `isLoaded()` marker, presumably for tiles that need to distinguish "constructed" from "chunk-loaded and ticking" state. |
| `ILootContainerModifiable.java extends ILootContainer` | Lets code assign/refresh a loot table (by `ResourceLocation` + seed) and force-fill a container for a given player; `ILootContainer` is a vanilla 1.12 interface (`net.minecraft.world.storage.loot`) that no longer exists by that name/package in 1.21 — see risk section. |
| `IWorldRenameable.java extends IWorldNameable` | Adds a mutator (`setCustomName`) that vanilla's `IWorldNameable` lacks; comment lampshades that Mojang should have included it. `IWorldNameable` was renamed in later Minecraft versions (see risks). |

### `com.hbm.interfaces` (37 files: 33 interfaces, 1 concrete class, 4 annotations)

| File | Purpose |
|---|---|
| `BitMask.java` | Abstract bitset contract (get/set/getAndSet, next/previous set/clear bit, cardinality, length, size, `toLongArray`, optional `free()`); no Minecraft types, pure data-structure API. Backing implementation(s) are presumably elsewhere in `com.hbm.util` (out of this area's scope) but the contract itself belongs here. |
| `IAnimatedDoor.java extends IDoor` | Client-side door animation hook (`handleNewState`) plus a `static` helper computing a fresh animation-start timestamp on stationary->moving transitions. |
| `IArmorModDash.java` | Single-method marker returning a dash count for armor with a dash/dodge mod. |
| `IBlockSpecialPlacementAABB.java` | Lets a block override its placement-time collision AABB based on the placing `ItemStack`. |
| `IBomb.java` | Central "something can be detonated/triggered" contract; nests `BombReturnCode` enum (DETONATED/TRIGGERED/LAUNCHED success cases, three error cases) each carrying an unlocalized message key + success flag. **Already ported in Neo Edition** (see section 3) — signature changed from `(World, BlockPos, Entity detonator)` to `(Level, BlockPos)`, dropping the detonator entity parameter. |
| `IBulletHitBehavior.java` | Called when a bullet entity (`EntityBulletBase`) hits and kills an entity. |
| `IBulletHurtBehavior.java` | Called when a bullet hits and damages (but doesn't necessarily kill/consume) an entity. |
| `IBulletImpactBehavior.java` | Called when a bullet hits a block at integer coords (also called with `-1,-1,-1` on entity hits per the comment — a smell to preserve/document, not silently fix). |
| `IBulletRicochetBehavior.java` | Called when a bullet ricochets off a block. |
| `IBulletUpdateBehavior.java` | Called every tick a bullet is alive, for homing/steering logic. |
| `IClimbable.java` | TE (or other) climbable-AABB contract with an explicit lifecycle contract in the javadoc (register in `onLoad`, not `validate`; unregister in `invalidate`/`onChunkUnload`; call `ClimbableRegistry.refresh` on AABB change) and an explicit warning against implementing `world()`/`pos()` by naming them so `TileEntity` "accidentally" satisfies them (throws `AbstractMethodError` in obfuscated runtime) — this specific obfuscation pitfall is moot under Mojang mappings, but the cast-based default-method pattern itself is still the intended idiom and should be preserved (now casting to `BlockEntity`). |
| `IConstantRenderer.java` | Marker only: entities needing a fallback world-entity sweep when normal chunk-visibility culling would skip them. |
| `IContainerOpenEventListener.java` | Hook contract to be wired manually into an overridden `Container#onContainerClosed`. |
| `IControlReceiver.java` | Client->server complex control-data channel over NBT; permission check + two receive overloads (legacy player-less, and player-aware `default`). **Already ported in Neo Edition.** |
| `ICopiable.java` | Settings copy/paste contract ("clone tool" pattern) for TEs/blocks, using an `Either<TileEntity, Block>` to resolve a display/translation key regardless of whether the source is currently a live TE or a bare block. **Already ported in Neo Edition** — note the ported version's `getSettingsSourceDisplay` collapsed to `getDescriptionId()` for both id and display (1.12's `getLocalizedName()` has no direct 1.21 equivalent; likely intentional simplification by the Neo Edition author, not something to silently "fix" back). |
| `ICustomSelectionBox.java` | Lets a block render a custom selection/outline box instead of the default cube. |
| `IDoor.java` | Central door contract: open/close/toggle, `DoorState` enum (CLOSED/OPEN/CLOSING/OPENING) with `isStationaryState`/`isMovingState` helpers, texture-state hooks (`default`, opt-in), redstone-only mode with a config-driven `Mode` enum (DEFAULT/TOOLABLE/REDSTONE) resolved via `MachineConfig.doorConf` keyed by block registry name string. |
| `IDummy.java` | Empty marker interface. |
| `IExplosionRay.java` | Procedural/ray-based explosion contract: incremental `update(processTimeMs)` (explicitly designed to allow heavy work off the main thread), cancel, completion/containment queries, detonator UUID, and manual NBT read/write for persistence across save/load — this is a *saved-entity-like* object, not a simple event object. |
| `IFFtoNTMF.java` | `@Deprecated` one-shot migration shim converting old Forge `Fluid`/`FluidTank` state into the mod's own `FluidType`/`FluidTankNTM` via reflection into `Fluids` constants. This is explicitly legacy-data-migration code tied to a specific historical mod version (comment: "delete after 2.0.3"). **Recommendation: do not port.** It exists purely to migrate saves from an old CE version forward; a from-scratch NeoForge port has no such legacy save format to migrate from, and it depends on `api.fluid`/`api.fluidmk2` types that are out of this area's scope anyway. Flagging as an explicit out-of-scope/drop decision rather than silently omitting it. |
| `IGunClickable.java` | Empty marker: block is clickable by guns. |
| `IHasCustomModel.java` | Returns a `ModelResourceLocation` for blocks/items with special model resolution. 1.12-specific type; 1.21 model identification is `ResourceLocation`/`ModelResourceLocation` still exists in 1.21 client code but the whole custom-model-loading pipeline changed substantially (baked model registration via `ModelEvent.RegisterAdditional` etc.) — contract intent (let content classes declare an extra/custom model to load) is portable, exact type/wiring is a client-rendering-area concern, not this area's. |
| `IHoldableWeapon.java` | Held-weapon HUD contract: crosshair type + optional custom HUD render hook, `@SideOnly(Side.CLIENT)` (NeoForge equivalent: `@OnlyIn(Dist.CLIENT)` from `net.neoforged.api.distmarker`, confirmed as the real annotation used broadly across NeoForge mods; verify against Neo Edition renderer code in the rendering area's pass since it wasn't yet ported here). |
| `IItemHUD.java` | Held-item HUD render hook, parameterized by `RenderGameOverlayEvent.Pre`/`ElementType` — both Forge-1.12-specific event types; 1.21 NeoForge overlay rendering uses `IGuiOverlay`/`RegisterGuiOverlaysEvent` and `RenderGuiEvent`, a materially different API. Contract intent portable, exact signature is a rendering-area concern. |
| `IKeypadHandler.java` | Keypad-lock contract: exposes a `Keypad` object plus two opt-in lifecycle callbacks (activated, password-set). |
| `ILaserable.java` | Energy-delivery-via-laser contract; carries a `@Deprecated` legacy overload converting raw `int`/`ForgeDirection` to `BlockPos`/`EnumFacing` — in the port, drop the deprecated overload entirely (its own javadoc says "use BlockPos") rather than reintroducing a legacy shim with no legacy callers left. |
| `IMixinFMLProxyPacket.java` | Mixin-injected accessor interface pulling S3F (custom-payload) packets out of an `FMLProxyPacket`. This is entirely a Forge-1.12 mixin/ASM artifact — `FMLProxyPacket` does not exist in NeoForge 1.21 (custom payloads are now `CustomPacketPayload`). **Recommendation: do not port.** No 1.21 equivalent construct exists; this was working around a 1.12-specific packet-wrapping problem NeoForge's payload API doesn't have. |
| `IMultiBlock.java` | Empty marker ("Another dummy interface" per its own comment). |
| `IOrderedEnum.java` | Generic `<T extends Enum<T>>` contract returning a custom display/iteration order array for an enum. Pure Java, no Minecraft types — trivially portable verbatim. |
| `IPostRender.java` | Empty marker. |
| `IRadResistantBlock.java` | `default`-true radiation-resistance marker with explicit implementer obligations documented in a comment (must override `onBlockAdded`/`breakBlock` and call `RadiationSystemNT.markChunkForRebuild`) and an `@implNote` that the check itself must be side-effect-free. |
| `IRadiationImmune.java` | Empty marker (distinct from `IRadResistantBlock` — immune vs. merely resistant). |
| `NotableComments.java` | Source-retention marker annotation (`@Target(TYPE)`) flagging classes with noteworthy comments; team-culture/documentation tool, zero runtime behavior. |
| `ServerThread.java` | Source-retention marker annotation (`@Target(METHOD)`) documenting that a method must run on the server thread. |
| `Spaghetti.java` | Source-retention annotation with a required `String value()` documenting known-bad code, for hover-tooltip visibility in an IDE. |
| `ThreadSafeMethod.java` | Source-retention marker annotation (`@Target(METHOD)`) documenting thread-safety of a method. |
| `Untested.java` | Source-retention annotation with an optional `String value()` flagging untested code. |

## 2. Key responsibilities (by cluster)

- **Block/tile behavior contracts** (`api.block`, `api.tile`, most of `interfaces`): dozens of Phase 1-4 blocks and block entities will implement these as mixins-of-behavior (tool interaction, drilling, heat, explosion, radiation, doors, climbable surfaces, custom hitboxes). None of these carry implementation logic themselves beyond a handful of `default`/`static` pure-Java or pure-math helper methods — they are the connective tissue between otherwise-unrelated content classes and the mod's shared systems (`ClimbableRegistry`, `RadiationSystemNT`, `MachineConfig`, `DamageResistanceHandler`).
- **Conveyor subsystem contract** (`api.conveyor`): a small, self-contained 4-interface protocol between belt blocks and the items/packages riding them.
- **Pneumatic storage-network contract** (`api.ntl`): the two concrete classes (`SlotMonitor`, `StackCache`) plus their supporting interface (`ISlotMonitorProvider`) implement the diff-detection and aggregation engine behind the mod's "access terminal" style item storage/retrieval network. This is meaningfully stateful logic, not just a marker interface, and depends on `PneumaticNetwork` (in `com.hbm.uninos.networkproviders`, outside this area).
- **Redstone-over-Radio protocol** (`api.redstoneoverradio`): a tiny, fully self-contained string-based RPC protocol with real (if minimal) parsing logic in `IRORInteractive`'s static methods. Zero engine dependency — this is the easiest, lowest-risk piece in the whole area to port unchanged.
- **Bullet behavior strategy interfaces** (4 `IBullet*Behavior` files in `interfaces`): a classic strategy-pattern decomposition of what happens to a bullet on hit/hurt/impact/ricochet/update, all keyed on `EntityBulletBase` (outside this area, in `com.hbm.entity.projectile`).
- **Documentation/lint annotations** (`NotableComments`, `ServerThread`, `Spaghetti`, `ThreadSafeMethod`, `Untested`): zero runtime behavior, pure `SOURCE`-retention markers. Trivial 1:1 port.
- **Radar target classification** (`api.entity`): two competing/overlapping contracts (`IRadarDetectable` legacy enum-based, `IRadarDetectableNT` newer int-constant-based) plus the `RadarEntry` DTO used to serialize a blip over the network.
- **Legacy/dead weight to explicitly drop**: `IFFtoNTMF` (save-migration shim, `@Deprecated` in CE itself) and `IMixinFMLProxyPacket` (Forge-1.12 mixin artifact with no NeoForge 1.21 equivalent construct). Both are flagged rather than silently ported as stubs.

## 3. Cross-area dependencies

This area is almost entirely upstream of everything else — it defines contracts other areas' content classes will implement — but a few files reach *out* to types owned by other areas:

- `api.block.ICrucibleAcceptor` -> `com.hbm.inventory.material.Mats.MaterialStack` (inventory/material area) and `com.hbm.lib.ForgeDirection` (needs a NeoForge-native replacement, likely `net.minecraft.core.Direction` directly — `ForgeDirection` was CE's own 1.12-era wrapper and should probably not be re-created 1:1 in the port; flagging for whoever owns `com.hbm.lib`).
- `api.block.IDrillInteraction` -> `com.hbm.inventory.RecipesCommon` (via `IToolable.ToolType`) and itself references `IMiningDrill` (in-area).
- `api.block.IExploder` -> `com.hbm.entity.item.EntityTNTPrimedBase` (entity area).
- `api.item.IGasMask` -> `com.hbm.util.ArmorRegistry.HazardClass` (util/armor area).
- `api.ntl.ISlotMonitorProvider` / `SlotMonitor` / `StackCache` -> `com.hbm.uninos.networkproviders.PneumaticNetwork` (uninos/network area) — this is the area's single largest external coupling; whoever owns `uninos` needs `PneumaticNetwork.accessors` to expose a compatible iterable of `StackCache`.
- `api.network.IPacketRegisterListener` -> `com.hbm.packet.PacketDispatcher` (networking area) — as noted above, the `int nextId` contract itself needs to be redesigned jointly with the networking area around NeoForge's `PayloadRegistrar`, not ported literally.
- `interfaces.IClimbable` -> `com.hbm.handler.ClimbableRegistry` (handler area).
- `interfaces.IDoor` -> `com.hbm.config.MachineConfig` (config area).
- `interfaces.IRadResistantBlock` -> `RadiationSystemNT` (radiation area, referenced only in a comment, not an import).
- `interfaces.IKeypadHandler` -> `com.hbm.util.Keypad`.
- `interfaces.ICopiable` / others -> `com.hbm.util.Either` (small util class; confirmed still present and used as-is in Neo Edition's port).
- `interfaces.IHoldableWeapon` / `IItemHUD` -> rendering-area event types that changed shape entirely between Forge 1.12 and NeoForge 1.21 (see risks).
- The four `IBullet*Behavior` interfaces -> `com.hbm.entity.projectile.EntityBulletBase` (entity/projectile area).
- `interfaces.IExplosionRay` -> no direct dependency on other named classes but is clearly meant to be driven by whatever "explosion manager" ticks it (out of scope here).

Nothing in `api.fluid`, `api.fluidmk2`, or `api.energymk2` was read or touched, per the exclusion.

## 4. NBT -> Data Component migration notes

Per the hard rule on ItemStack NBT, the following in-scope files store or mutate state directly on an `ItemStack`'s NBT and will need a `DataComponentType` in the actual port:

- **`api.item.IGasMask`**: `getFilter(ItemStack)` / `installFilter(ItemStack, ItemStack)` / `damageFilter(ItemStack, int)`. CE stores an installed filter `ItemStack` (and presumably its remaining durability/charge) directly in the mask's NBT compound. Recommended component: a single `DataComponentType<GasMaskFilterState>` (or two components: one `ItemStack`-holding component for the filter item, one `int` component for filter damage/charge) registered wherever gas mask items are registered (Phase 1+ content area, not this area) — this interface's *contract* doesn't change, only the concrete implementing item classes (out of scope here) need the codec/stream-codec work.
- **`api.ntl.SlotMonitor` / `StackCache`**: these read `NBTTagCompound` off `ItemStack`s purely for **identity comparison** (detecting whether a slot's contents changed type) via `stack.getTagCompound()`/`nbt.equals(...)` and a hash in `getStackIdentity`. This is not persistent data owned by these classes — it's inspecting whatever data components an item stack from elsewhere happens to carry. In 1.21 this becomes comparing `ItemStack#getComponents()` (a `DataComponentMap`) for equality/hash instead of `NBTTagCompound` equality — `ItemStack.isSameItemSameComponents` and `ItemStack#getComponents().hashCode()` are the natural replacements. This is a mechanical adaptation of the *comparison*, not a case of this area needing to define a new component type itself.
- **`interfaces.IExplosionRay`**: `readEntityFromNBT`/`writeEntityToNBT` — this is entity/blockentity save data, not ItemStack data, so it stays as `CompoundTag` read/write (the "Data Components" rule targets `ItemStack`, not entities/BEs); no migration needed here.
- No other in-scope file reads or writes `ItemStack` NBT directly. Interfaces that pass `NBTTagCompound`/`CompoundTag` around (`ICopiable`, `IControlReceiver`) do so for TE/control-data payloads, not item stacks, and are unaffected by the component rule.

## 5. Recommended NeoForge / Java 21 port plan

1. **Package layout**: preserve `com.hbm.api.block`, `com.hbm.api.conveyor`, `com.hbm.api.entity`, `com.hbm.api.item`, `com.hbm.api.network`, `com.hbm.api.ntl`, `com.hbm.api.recipe`, `com.hbm.api.redstoneoverradio`, `com.hbm.api.tile`, and `com.hbm.interfaces` 1:1, one file each, as CE has them.
2. **Mechanical type substitutions** (confirmed real by grepping Neo Edition's own ported files and well-established NeoForge 21.1 conventions):
   - `net.minecraft.world.World` -> `net.minecraft.world.level.Level`
   - `net.minecraft.util.math.BlockPos` -> `net.minecraft.core.BlockPos`
   - `net.minecraft.tileentity.TileEntity` -> `net.minecraft.world.level.block.entity.BlockEntity`
   - `net.minecraft.entity.player.EntityPlayer` -> `net.minecraft.world.entity.player.Player`
   - `net.minecraft.entity.EntityLivingBase` -> `net.minecraft.world.entity.LivingEntity`
   - `net.minecraft.entity.Entity` -> `net.minecraft.world.entity.Entity`
   - `net.minecraft.nbt.NBTTagCompound` -> `net.minecraft.nbt.CompoundTag`
   - `net.minecraft.util.EnumFacing` -> `net.minecraft.core.Direction`
   - `net.minecraft.util.math.Vec3d` -> `net.minecraft.world.phys.Vec3`
   - `net.minecraft.util.math.AxisAlignedBB` -> `net.minecraft.world.phys.AABB`
   - `net.minecraft.block.state.IBlockState` -> `net.minecraft.world.level.block.state.BlockState`
   - `net.minecraft.block.Block` -> `net.minecraft.world.level.block.Block`
   - `net.minecraft.item.ItemStack` -> `net.minecraft.world.item.ItemStack`
   - `net.minecraft.item.Item` -> `net.minecraft.world.item.Item`
   - `Block.getTranslationKey()`/`getLocalizedName()` -> `Block.getDescriptionId()` (confirmed in Neo Edition's ported `ICopiable`, which collapsed both CE methods onto this single 1.21 method)
   - `@SideOnly(Side.CLIENT)` -> `@OnlyIn(Dist.CLIENT)` (`net.neoforged.api.distmarker.OnlyIn`/`Dist`)
   - `com.hbm.lib.ForgeDirection` -> use `net.minecraft.core.Direction` directly wherever this area references it (`ICrucibleAcceptor`, `IBlowable`, `IPneumaticConnector`, the deprecated `ILaserable` overload which should simply be dropped) — CE's own `ForgeDirection` was a compatibility wrapper for a Forge concept that no longer needs wrapping; final call belongs to whoever owns `com.hbm.lib`, but recommend not reintroducing it for these interfaces specifically.
   - `world.storage.loot.ILootContainer` (`ILootContainerModifiable`'s supertype) has no 1:1 1.21 name — modern loot tables use `RandomizableContainer`/`RandomizableContainerBlockEntity` machinery (`net.minecraft.world.RandomizableContainer` is the closest analog exposing `getLootTable()`/`setLootTable()` already). Recommend `ILootContainerModifiable` extend `net.minecraft.world.RandomizableContainer` instead of trying to recreate `ILootContainer`, since Mojang's own interface already covers most of the same ground; confirm exact member names against the actual 1.21.1 Minecraft sources before implementing (not found in Neo Edition reference, so this is the one place in this area not yet confirmed against a *working* NeoForge 21.1 usage — flagged as an open question, not guessed).
   - `net.minecraft.world.IWorldNameable` (`IWorldRenameable`'s supertype) was renamed by Mojang; current name in modern MC is `net.minecraft.world.Nameable`. Not found in Neo Edition reference either — same caveat as above, verify before implementing.
   - Forge's legacy `ByteBuf`/`ByteBufUtils` in `RadarEntry` -> NeoForge 1.21 networking is payload/`RegistryFriendlyByteBuf`-based (`net.minecraft.network.RegistryFriendlyByteBuf`, `StreamCodec`); `RadarEntry`'s manual `fromBytes`/`toBytes` should become a `StreamCodec<RegistryFriendlyByteBuf, RadarEntry>` when its owning packet is ported (owning packet type is outside this area — this area only needs to keep `RadarEntry` itself as a plain data-holder class and note the required codec for whoever ports the actual radar packet).
3. **`IPacketRegisterListener`**: do not port the `int nextId` contract literally. Recommend redesigning jointly with the networking-area owner as a listener that receives a `PayloadRegistrar` (or equivalent registration context) instead of returning/consuming an integer id, since NeoForge 1.21 payload channels are keyed by `ResourceLocation` + protocol version, not sequential integers.
4. **Drop, do not port**: `interfaces.IFFtoNTMF` (dead save-migration shim, already `@Deprecated` in CE, has no legacy save data to migrate from in a from-scratch port) and `interfaces.IMixinFMLProxyPacket` (Forge-1.12-only mixin artifact; NeoForge 1.21's `CustomPacketPayload` model has no equivalent problem to solve). Both should be documented as intentionally omitted, not silently forgotten.
5. **`interfaces.IHoldableWeapon` / `IItemHUD`**: port the *contract shape* (crosshair/HUD hook methods) but the parameter types (`RenderGameOverlayEvent.Pre`, `ElementType`, `GuiIngame`, `ScaledResolution`) must be re-derived against NeoForge 1.21's actual overlay API (`IGuiOverlay`, `RegisterGuiOverlaysEvent`, `RenderGuiEvent.Pre/Post`, `GuiGraphics`) by whoever owns the rendering area — not guessed here since Neo Edition's reference tree doesn't yet have these two ported. Recommend this area publish the two interfaces with `Object`-erased or deferred signatures is NOT an option (would violate "preserve interface contracts exactly" for Phase 1-4 implementors); instead, recommend this area's port pass for these two specific files be scheduled jointly with (or immediately after) the rendering area's Phase 0 pass, once real overlay method signatures are confirmed there.
6. **`interfaces.IHasCustomModel`**: same situation — `ModelResourceLocation` still exists in 1.21 client code, but confirm the exact intended usage pattern against the rendering area's model-registration approach before finalizing; low risk since the type itself is unchanged, only the *pipeline* around it differs.
7. **Everything else in this area has a direct, low-risk, already-confirmed mechanical translation** using the substitutions above — the bulk of the 60 files (all pure markers, simple data contracts, or the fully engine-agnostic `BitMask`/`IOrderedEnum`/`IRORInteractive` static-helper logic) can be ported with essentially find-and-replace-level changes plus the `@OnlyIn` swap where `@SideOnly` appears.
8. **Concrete (non-interface) classes** (`RadarEntry`, `SlotMonitor`, `StackCache`, `RORFunctionException`) port as ordinary Java classes; `SlotMonitor`/`StackCache` need their `NBTTagCompound`-based identity comparison updated per section 4, and their `PneumaticNetwork` dependency resolved once the `uninos` area's Phase 0 report is available.

## 6. Risks and open questions

- **`ILootContainerModifiable`'s supertype and `IWorldRenameable`'s supertype** could not be confirmed against a real, working NeoForge 21.1 usage (absent from the Neo Edition reference tree). Recommend the implementing agent verify `net.minecraft.world.RandomizableContainer` and `net.minecraft.world.Nameable` (or whatever the actual 1.21.1 names are) directly against the Minecraft/NeoForge sources before writing these two files, per the "do not invent APIs" rule.
- **`api.network.IPacketRegisterListener`** cannot be ported as a pure 1:1 translation because its entire reason to exist (a sequential integer packet-id allocator) doesn't map onto NeoForge 1.21's `ResourceLocation`-keyed payload registration. This needs a joint decision with the networking area owner before Phase 1 classes start implementing it — flagging now so it doesn't become a Phase 1 surprise.
- **`interfaces.IHoldableWeapon` and `IItemHUD`** depend on Forge-1.12 GUI overlay event types with no 1:1 NeoForge 1.21 equivalent. Exact replacement signatures should be confirmed by/with the rendering area before this area's Phase 1 port pass, to avoid the interface contract needing a breaking change later after dozens of weapon classes have already implemented it.
- **`ForgeDirection` (CE's own wrapper class, in `com.hbm.lib`, out of this area's scope)** is referenced by several in-scope interfaces (`ICrucibleAcceptor`, `IBlowable`, `IPneumaticConnector`, `ILaserable`'s deprecated overload). Recommend against recreating it and using `net.minecraft.core.Direction` directly in this area's ported files, but this should be confirmed against whatever `com.hbm.lib`'s owning area decides, since a mismatch would force a rewrite of every implementor.
- **`api.ntl` (`ISlotMonitorProvider`/`SlotMonitor`/`StackCache`) is real, non-trivial stateful logic**, not just a marker interface — it is effectively a small inventory-diffing engine. Porting it correctly (especially the `getStackIdentity` hash and the `PneumaticNetwork.accessors` join point) requires close coordination with whoever owns `com.hbm.uninos`. Recommend this trio be scheduled as one atomic unit with that area rather than ported in isolation.
- **`IRadarDetectable` vs `IRadarDetectableNT`**: CE itself carries two overlapping/competing radar contracts (an older enum-based one and a newer int-constant-based one). This report preserves both as found; whether Phase 1+ should continue supporting both or consolidate onto `IRadarDetectableNT` is a content-area decision, not one to make unilaterally in this API-preservation pass.
- **`BitMask`'s concrete implementation(s)** were not found anywhere in this area's scope (only the interface exists here) — whoever implements `BitMask` in a later phase should be pointed at `com.hbm.util` in CE to locate the concrete class(es), which are out of this area's scope.
- File count note: the `com.hbm.interfaces` package contains 37 files total: 32 plain interfaces (including `BitMask`, `IDoor`, `IBomb`, etc.) plus 5 source-retention annotation types (`NotableComments`, `ServerThread`, `Spaghetti`, `ThreadSafeMethod`, `Untested`). It has no concrete classes. The 4 concrete classes in this area's overall scope (`RadarEntry`, `SlotMonitor`, `StackCache`, `RORFunctionException`) all live in `com.hbm.api.*` subpackages, not in `com.hbm.interfaces`.
