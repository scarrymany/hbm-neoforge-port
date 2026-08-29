# Phase 0 Research: HE Energy Capability & Network Graph (`energymk2`)

Scope: `com.hbm.api.energymk2` from CE (1.12.2). Target: `com.hbm.api.energymk2` in the NeoForge 21.1.228 / Java 21
port at `C:\Users\Sergo127\Desktop\hbms`.

## 1. Class inventory (CE source)

| File | Purpose |
|---|---|
| `IBatteryItem.java` | Contract for `Item`s that store HE charge in NBT (a "charge" long tag by default). Static helpers `emptyBattery(ItemStack/Item)` and `getChargeTagName(ItemStack)`. Pure item-side API, no world/BE involvement. |
| `IEnergyConnectorMK2.java` | Root marker interface for anything that can be a network endpoint. Single `canConnect(ForgeDirection dir)` default method (side-based connection gate). Extended by `IEnergyHandlerMK2` and directly implemented by conductors via `IEnergyConductorMK2`. |
| `IEnergyConnectorBlock.java` | Separate, TE-less variant of the connect check (`canConnect(IBlockAccess, BlockPos, ForgeDirection)`) for plain `Block`s that want cable-visual-connection without a tile entity. Rendering-only, not part of the network graph itself. |
| `IEnergyConductorMK2.java` | Mixed into cable/conductor `TileEntity`s. Extends `IEnergyConnectorMK2`. Provides `createNode()`, which builds a `Nodespace.PowerNode` with the 6 axis-aligned `DirPos` neighbor connections for this TE's position. Does not extend `IEnergyHandlerMK2` - conductors are not power-holding themselves, they just route the network topology. |
| `IEnergyHandlerMK2.java` | Common ancestor for anything that *holds* power (providers and receivers): `getPower()/setPower(long)/getMaxPower()`, extends `IEnergyConnectorMK2` and `ILoadedTile` (chunk-loaded check, used to evict stale network members). Explicitly documented "DO NOT USE DIRECTLY". Also carries a `particleDebug` static flag and a debug-particle position helper. |
| `IEnergyProviderMK2.java` | Mixed into power-source `TileEntity`s (generators, batteries acting as sources, etc). Adds `usePower(long)`, `getProviderSpeed()` (defaults to `getMaxPower()`), and the important `tryProvide(World, BlockPos/xyz, ForgeDirection)` - a direct-contact push: looks at the neighbor TE, and either (a) registers itself as a provider on the neighbor's power net if the neighbor is a conductor, (b) does a direct HE transfer if the neighbor is a receiver with `allowDirectProvision()==true`, or (c) bridges to Forge Energy (`CapabilityEnergy.ENERGY`) using `GeneralConfig.conversionRateHeToRF` as an HE<->RF exchange rate. Also fires debug particles via `PacketThreading`/`AuxParticlePacketNT`. |
| `IEnergyReceiverMK2.java` | Mixed into power-sink `TileEntity`s. Adds `transferPower(long, boolean simulate)` (accepts up to capacity, returns rejected overflow), `getReceiverSpeed()`, `allowDirectProvision()` (bypass network via touching provider), `trySubscribe(...)` (registers itself onto the neighbor conductor's `PowerNetMK2`), `tryUnsubscribe(...)`, and the `ConnectionPriority` enum (`LOWEST..HIGHEST`) used by the network's demand-weighting pass. Contains an unrelated large historical-trivia comment block (MKUltra) with no code effect - can be dropped in the port. |
| `Nodespace.java` | Thin, explicitly `@Deprecated`-flagged compatibility facade over the shared `com.hbm.uninos.UniNodespace` graph engine (out of this area's scope - owned by whichever agent handles "uninos"). Defines `THE_POWER_PROVIDER` (an `INetworkProvider<PowerNetMK2>` supplying `PowerNetMK2::new`) and the nested `PowerNode extends GenNode<PowerNetMK2>` class used as the per-block node payload for the power graph specifically. `getNode/createNode/destroyNode` simply delegate to `UniNodespace`. |
| `PowerNetMK2.java` | The actual power network graph node-set: extends `com.hbm.uninos.NodeNet<IEnergyReceiverMK2, IEnergyProviderMK2, Nodespace.PowerNode, PowerNetMK2>`. Holds `providerEntries`/`receiverEntries` (inherited, keyed by last-seen timestamp) and implements: `update()` - per-tick priority-weighted, precision-safe (`weightedShare`, `BigInteger` fallback for overflow-prone multiplications) distribution of available provider power to receivers ordered by `ConnectionPriority`, with round-robin remainder cursors so leftover HE doesn't always land on the same node; `sendPowerDiode(long, boolean)` - pushes external power directly into the network's receivers without touching a provider bucket (used by diode-type one-way connectors); `extractPowerDiode(long, boolean)` - pulls power directly out of the network's providers. Uses a `ReentrantLock` around the provider/receiver map scans since network updates can run off the main thread relative to registration calls. |

Two CE dependencies are exercised heavily but live outside this area's scope and must be coordinated with whichever
agent owns them:

- `com.hbm.uninos.*` (`NodeNet`, `GenNode`, `INetworkProvider`, `UniNodespace`) - the generic multi-network graph
  engine (nodes, joining/splitting networks, per-world node storage, `activeNodeNets` tick loop). `PowerNetMK2` and
  `Nodespace` are consumers of this, not owners of it.
- `com.hbm.lib.ForgeDirection`/`DirPos`/`Library` (direction enum and axis-neighbor helpers), `com.hbm.api.tile.ILoadedTile`,
  `com.hbm.config.GeneralConfig` (HE<->RF conversion rate config), `com.hbm.handler.threading.PacketThreading` +
  `com.hbm.packet.toclient.AuxParticlePacketNT` + `com.hbm.particle.helper.HbmEffectNT` (debug particle packet, only
  active when `particleDebug=true`), `com.hbm.util.Compat` (safe TE lookup), `com.hbm.util.Tuple`.

## 2. Key responsibilities

- Define HBM's own "HE" energy unit as a completely separate, custom transport system, deliberately distinct from
  Forge/NeoForge Energy (FE/RF). The only FE interop point is the one-directional bridge in
  `IEnergyProviderMK2.tryProvide` that converts HE into FE when pushing into a plain Forge-Energy-capable neighbor.
- Model cables/conductors as topology-only nodes (`IEnergyConductorMK2` -> `Nodespace.PowerNode`) that get merged
  into `PowerNetMK2` graphs by the shared UNINOS engine based on world-adjacency, independent of chunk loading.
- Model producers/consumers (`IEnergyProviderMK2`/`IEnergyReceiverMK2`) as *registrants* on a neighboring conductor's
  network rather than as network members themselves - they call `tryProvide`/`trySubscribe` against their immediate
  neighbor position each time they want to interact, and the network purges stale/unloaded registrants opportunistically
  (`isBadLink`, timeout-based eviction) rather than requiring explicit unregistration in every code path.
- Perform per-tick priority-weighted power distribution with overflow-safe arithmetic and round-robin fairness across
  ties, in `PowerNetMK2.update()`.
- Provide two "diode" one-way transfer entry points (`sendPowerDiode`, `extractPowerDiode`) for parts of the mod that
  push/pull power without acting as full provider/receiver network members.
- Provide the battery-item contract (`IBatteryItem`) fully independent of the block/TE network graph - this is pure
  ItemStack NBT state today and is the one piece of this area that intersects the mandatory Data Components migration.

## 3. Cross-area dependencies

- **uninos (network graph engine)**: hard dependency, out of scope here. `PowerNetMK2` and `Nodespace` cannot compile
  without a ported `NodeNet`/`GenNode`/`INetworkProvider`/`UniNodespace`. Whichever wave ports `com.hbm.uninos` and
  this area need to land together or `energymk2` needs a temporary local stub during development.
- **lib (ForgeDirection/DirPos/Library)**: `ForgeDirection` no longer exists in modern Minecraft; NeoForge/Vanilla
  code uses `net.minecraft.core.Direction`. Every method signature in this area that takes `ForgeDirection` must be
  re-typed to `Direction`, confirmed live in the Neo Edition reference (see section 5).
- **api.tile.ILoadedTile**: needed by `IEnergyHandlerMK2`. Neo Edition's equivalent is `api.hbm.blockentity.ILoadedBE`.
  Whoever owns block-entity base classes needs to supply this.
- **config.GeneralConfig** (`conversionRateHeToRF`): needed only inside `IEnergyProviderMK2.tryProvide`'s FE bridge.
- **handler.threading.PacketThreading / packet.toclient.AuxParticlePacketNT / particle.helper.HbmEffectNT**: only used
  behind the `particleDebug` compile-time-false flag; can be stubbed/no-op'd first and wired for real once the
  networking area lands, without blocking this area's core port.
- **util.Compat, util.Tuple**: small helpers; `Tuple.ObjectLongPair` is used inside `PowerNetMK2`'s hot loop.
- **Consumers of this API** (batteries, cables, generators, machines throughout the mod) are themselves out of scope,
  but every one of them will need to switch `ForgeDirection` -> `Direction`, `BlockPos`(1.12 `net.minecraft.util.math.BlockPos`)
  -> `net.minecraft.core.BlockPos`, and `TileEntity` -> `BlockEntity` when they implement these interfaces.

## 4. Recommended NeoForge / Java 21 port plan

**Key finding that overrides the task brief's suggested approach:** the already-working Neo Edition reference port
(`hbmsntm`, NeoForge 21.1) does **not** implement this system as a registered NeoForge `BlockCapability`/`ItemCapability`
via `RegisterCapabilitiesEvent`/`Capabilities`-style `DeferredRegister`. It ports `energymk2` as the exact same kind of
plain Java interface contract CE uses (`IEnergyProviderMK2`/`IEnergyReceiverMK2`/`IEnergyConductorMK2` implemented
directly by `BlockEntity` subclasses, discovered via `instanceof` on the neighbor `BlockEntity`, exactly like CE does
with `instanceof` on the neighbor `TileEntity`). There is no `RegisterCapabilitiesEvent`, no `BlockCapability.createVoid`,
no capability `DeferredRegister` anywhere in their `energymk2`-related code. I recommend following that proven, working
shape rather than forcing NeoForge's generic capability-lookup layer onto a system whose whole design already routes
through direct neighbor-TE inspection and its own `PowerNetMK2` graph - NeoForge capabilities exist to let *unrelated*
mods discover a capability generically; HBM's own network graph already is that generic discovery layer for HE, so
wrapping it in a second lookup layer would be a needless abstraction with no consumer. This should be flagged back to
whoever wrote the task brief's suggested target shape before the write stage, since it deviates from "capability
registration via Capabilities-style DeferredRegister."

Concretely, per file:

- **`IEnergyConnectorMK2`**: port 1:1. Replace `ForgeDirection` -> `net.minecraft.core.Direction`; `canConnect`
  default becomes `dir != null` (there is no `Direction.UNKNOWN` in modern MC - `null` is the "no side" sentinel used
  for non-directional interaction elsewhere in NeoForge code, confirmed by the reference file).
- **`IEnergyConnectorBlock`**: port 1:1, `IBlockAccess` -> `net.minecraft.world.level.BlockGetter`, `BlockPos` -> the
  modern package.
- **`IEnergyConductorMK2`**: port 1:1. `TileEntity` -> `BlockEntity`, cast target and `.getPos()` -> `.getBlockPos()`.
  `Library.POS_X` etc. stay as whatever the `lib` area's ported `Library` constants become (confirm those exist before
  finalizing this file, since it references them directly).
- **`IEnergyHandlerMK2`**: port 1:1. `ILoadedTile` -> the ported loaded-check interface (`ILoadedBE` in the reference).
  `Vec3d` -> `net.minecraft.world.phys.Vec3`.
- **`IEnergyProviderMK2`**: port the core logic 1:1 (`usePower`, `getProviderSpeed`, the conductor-registration and
  direct-receiver-transfer branches of `tryProvide`). For the FE bridge branch: NeoForge 21.1 removed
  `net.minecraftforge.energy.CapabilityEnergy`/`IEnergyStorage` in favor of `net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK`
  (a `BlockCapability<IEnergyStorage, Direction>`) together with `net.neoforged.neoforge.energy.IEnergyStorage`, looked
  up via `level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, blockState, blockEntity, side)`. Per the task
  brief this FE bridge is optional/deferrable behind a config flag for a later phase - I recommend deferring the whole
  `else` branch (the Forge/NeoForge Energy interop) out of Phase 0 rather than guessing at the exact capability call
  shape without a confirmed working example of it in the Neo Edition reference (a grep of the reference tree found no
  live `Capabilities.EnergyStorage` usage to confirm against - see Risks). The particle-debug branch should be ported
  using whatever this project's packet/particle area exposes, gated the same way (`particleDebug` compile-constant).
- **`IEnergyReceiverMK2`**: port 1:1, including the `ConnectionPriority` enum and its ordinal-based priority
  weighting contract, which `PowerNetMK2` depends on structurally (do not renumber or reorder the enum). Drop the
  MKUltra comment block; it has no functional bearing. `Compat.getTileStandard` -> use this project's equivalent safe
  BE lookup helper (confirm with whoever owns `util`).
- **`Nodespace`**: port 1:1 once `com.hbm.uninos` is ported. `PowerNode` stays a thin `GenNode<PowerNetMK2>` subtype
  carrying the 6-way `DirPos` connections built by `IEnergyConductorMK2.createNode()`.
- **`PowerNetMK2`**: port the CE version's algorithm faithfully, including the `weightedShare` precision-safe
  BigInteger fallback and the round-robin remainder cursors (`updateReceiverRemainderCursor`, `updateProviderRemainderCursor`,
  `diodeReceiverRemainderCursor`, `diodeProviderRemainderCursor`) and the `ReentrantLock` around the map scans - this
  CE version is more advanced (fairer, more precision-safe) than what shipped in the Neo Edition reference's own
  `PowerNetMK2` (that file still uses the older simple-`double`-weight algorithm without the lock or remainder
  cursors). Do not downgrade to the reference's simpler algorithm; the task explicitly asks to preserve the CE graph
  algorithm faithfully, and the CE version is the more correct one of the two. `Object2LongOpenHashMap` (fastutil) is
  available in NeoForge's dependency set (fastutil ships with Minecraft itself) so it can be kept as-is once `NodeNet`
  (owned by the `uninos` area) exposes it with that map type - confirm the ported `NodeNet`'s `providerEntries`/
  `receiverEntries` field types with that area before finalizing this file, since the Neo Edition reference's own
  `NodeNet` downgraded these to plain `HashMap<K, Long>` instead of fastutil's primitive-specialized map.
- **`IBatteryItem`**: this is the one file in this area that touches ItemStack state directly, and per the hard rules
  it must move off raw NBT reads/writes onto Data Components. Recommended component:
  - `HbmDataComponents.CHARGE` : `DataComponentType<Long>` (or a small record `BatteryCharge(long value)` if a plain
    boxed `Long` component feels too anonymous in the registry) with a `Codec` (`Codec.LONG` or `Codec.LONG.xmap(...)`)
    and a `StreamCodec` (`ByteBufCodecs.VAR_LONG` or `.LONG`) for sync. Maps the CE/`getChargeTagName()` NBT key
    (default `"charge"`, per-item overridable) one-to-one onto this single component: every implementor's charge
    value moves from `stack.getTagCompound().getLong(keyName)` to `stack.get(HbmDataComponents.CHARGE)`similar, and
    `getChargeTagName()`/its per-item override becomes dead once every implementor is migrated (it existed purely to
    key into NBT; a typed component makes the per-item key indirection unnecessary, but that cleanup touches every
    battery item implementor, which is out of this area's scope - only note it here). `chargeBattery`/`setCharge`/
    `dischargeBattery`/`getCharge` become default methods here that read/write the component directly
    (`stack.update(HbmDataComponents.CHARGE, 0L, v -> ...)` / `stack.set(...)`), so implementors no longer need their
    own NBT plumbing at all - this is a net simplification over CE, not just a mechanical swap. `getMaxCharge`,
    `getChargeRate`, `getDischargeRate` stay abstract per-item intrinsic queries, unchanged in shape.
  - `emptyBattery(ItemStack)`/`emptyBattery(Item)` become default/static methods that copy the stack and
    `.set(HbmDataComponents.CHARGE, 0L)` instead of touching an `NBTTagCompound`. The Neo Edition reference file still
    does this the old way (`CompoundTag` + `TagsUtil.putCustomData`, i.e. writing into the vanilla `CUSTOM_DATA`
    component) - that is a legacy-NBT compatibility shim, not the idiomatic Data Component approach the task's hard
    rules require, so it should **not** be copied as-is; treat this as the one place this area intentionally does
    better than the existing reference port.
  - NBT key -> component mapping table (the explicit mapping the task rules require):
    | CE NBT key | New Data Component |
    |---|---|
    | `charge` (or per-item override via `getChargeTagName()`) | `HbmDataComponents.CHARGE` (`DataComponentType<Long>`) |
  - The actual `DataComponentType` registration (its `DeferredRegister<DataComponentType<?>>` entry) belongs in
    whichever class owns the mod's shared data component registry (likely alongside item registration, out of this
    area's file scope) - this area only needs to consume it. I am not creating that registry file since it is not
    among the files listed for this area's scope; the write stage should either find/reuse an existing
    `HbmDataComponents`-style class or coordinate with the item-registration area to add one.

## 5. Confirmed real NeoForge 21.1 API shapes (from the Neo Edition reference, `api.hbm.energymk2` and `com.hbm.uninos`)

- Interfaces are plain Java interfaces mixed into `BlockEntity` subclasses - no capability provider objects, no
  `ICapabilityProvider`, no `RegisterCapabilitiesEvent`.
- `net.minecraft.core.Direction` replaces `ForgeDirection` everywhere; `Direction.getOpposite()`, `.getStepX/Y/Z()`
  exist and are used exactly as in CE's `ForgeDirection`.
- `net.minecraft.core.BlockPos`, `net.minecraft.world.level.Level`, `net.minecraft.world.level.block.entity.BlockEntity`
  replace the 1.12 `net.minecraft.util.math.BlockPos` / `World` / `TileEntity`.
- `net.minecraft.world.phys.Vec3` replaces `Vec3d`.
- Debug particle networking uses `net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear(ServerLevel, Player, x, y, z, range, CustomPacketPayload)`
  rather than `NetworkRegistry.TargetPoint`.
- `net.minecraft.world.item.ItemStack`/`Item` package paths are unchanged in spirit from 1.12 (`net.minecraft.item.*`
  -> `net.minecraft.world.item.*`), `net.minecraft.nbt.NBTTagCompound` -> `net.minecraft.nbt.CompoundTag`.

## 6. Risks / open questions

1. **Capability-vs-plain-interface decision needs sign-off.** The task brief's target description explicitly asks for
   "a NeoForge capability (block/item capability registration via Capabilities-style DeferredRegister)" for this area,
   but the only real, working NeoForge 21.1 reference implementation available does not do that - it keeps the CE
   architecture unchanged (plain interfaces + the existing UNINOS graph as the discovery mechanism). I've written the
   port plan around the proven approach and flagged the deviation here rather than inventing an unverified capability
   registration shape; this should be confirmed with whoever set the target description before the write stage starts.
2. **`com.hbm.uninos` must land alongside this area or be stubbed.** `PowerNetMK2` and `Nodespace` do not compile
   without `NodeNet`/`GenNode`/`INetworkProvider`/`UniNodespace` existing in the port tree first.
3. **Precision-safety divergence between CE and the Neo Edition reference's `PowerNetMK2.update()`.** CE has a
   materially more robust algorithm (BigInteger overflow fallback, round-robin fairness, a lock around concurrent
   registration). The task says preserve the CE graph algorithm faithfully, so the write stage should port CE's
   version, not copy the reference's simpler one, even though the reference is otherwise the trusted API-shape source.
4. **FE bridge exact capability call shape is unconfirmed.** No live `Capabilities.EnergyStorage`/`IEnergyStorage`
   usage was found anywhere in the Neo Edition reference tree to confirm the exact NeoForge 21.1 energy-capability
   lookup call against. Per the task brief this bridge is optional and can be deferred behind a config flag; I
   recommend doing exactly that in Phase 0 rather than guessing the API.
5. **`Object2LongOpenHashMap` vs plain `HashMap<K, Long>` for `providerEntries`/`receiverEntries`.** CE uses fastutil's
   primitive-specialized map (with `.object2LongEntrySet().fastIterator()` and `.removeLong(...)` used directly in
   `PowerNetMK2`); the Neo Edition reference's ported `NodeNet` downgraded to a boxed `HashMap<K, Long>`. Since fastutil
   ships as a transitive Minecraft/NeoForge dependency, I recommend keeping the CE fastutil map type for
   correctness/performance parity, but this is ultimately owned by the `uninos` area and needs to be agreed with them
   since `PowerNetMK2` extends their `NodeNet` and inherits whichever map type they choose.
6. **`DataComponentType<Long>` registration ownership.** This area needs a `DataComponentType` for battery charge but
   does not own any registry-bootstrap file in its scope; the write stage needs to coordinate with the
   item/registry-owning area on where `HbmDataComponents` (or equivalent) lives and how this area's code references it.
7. **`ILoadedTile`/`ILoadedBE` and `Compat.getTileStandard` are unconfirmed in our own port tree** (only confirmed to
   exist in CE and in the Neo Edition reference under different names/packages) - the write stage must locate or
   request the equivalents in our own project rather than assuming the reference's exact names (`ILoadedBE`,
   `BlockEntityAccessCache.getBEOrCache`) carry over unchanged.
