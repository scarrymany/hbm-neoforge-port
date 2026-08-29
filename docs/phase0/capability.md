# Phase 0 Research: Capabilities Framework (`com.hbm.capability`)

## Scope

CE source: `hbm-ce/src/main/java/com/hbm/capability/*.java` (8 files). Neo Edition reference checked for real
NeoForge 21.1.228 API shapes; it has no equivalent capability-wrapper code yet (only `AttachmentType`
scaffolding in `com.hbm.lib.ModAttachments`), so the item/fluid-handler port plan below is derived from
well-established NeoForge 21.1 conventions rather than copied from a working Neo Edition example.

## Class inventory

| File | Purpose |
|---|---|
| `HbmCapability.java` | Forge `Capability<IHBMData>` attached to every `Entity` (really used on `EntityPlayer`). Holds per-player transient/persistent state: keybind press state, shield HP/max, jetpack/HUD/magnet toggles, dash cooldown/count, stamina, "plink" SFX cooldown, tutorial-book-received flag, reputation. Provides NBT `IStorage`, a `ICapabilitySerializable` provider, a netty `ByteBuf` serialize/deserialize pair for network sync, and a static `getData(Entity)` / `plink(...)` helper. |
| `HbmLivingCapability.java` | Forge `Capability<IEntityHbmProps>` attached to every `EntityLivingBase`. Holds radiation/contamination state: rads, neutron rads, environmental rads, rad buffer, digamma, asbestos/blacklung/bomb-timer/contagion/oil/phosphorus/fire/balefire counters, grenade-deployment ticks, and a `List<ContaminationEffect>`. Provides NBT save/load (with a versioned `fmt` tag for double-vs-float legacy migration), `ByteBuf` serialize/deserialize, `IStorage`, `ICapabilitySerializable` provider. |
| `HbmLivingProps.java` | Static facade over `HbmLivingCapability` - all game logic (radiation increment/decrement, digamma health-modifier application + instant death via `ModDamageSource.digamma`, asbestos/blacklung disease damage, advancement grants, network packets on affliction) lives here, not in the capability class itself. Also defines the `ContaminationEffect` value type (rad/time/ignoreArmor triple) with its own NBT and `ByteBuf` (de)serialization. |
| `NTMBatteryCapabilityHandler.java` | `AttachCapabilitiesEvent<ItemStack>` listener that attaches a Forge `CapabilityEnergy.ENERGY` (`IEnergyStorage`) wrapper to any `ItemStack` whose `Item` implements `IBatteryItem`. The wrapper converts HBM's internal HE (Heat Energy / Nuclear-Tech energy unit) charge to/from Forge RF using `GeneralConfig.conversionRateHeToRF`. |
| `NTMCableEnergyCapabilityWrapper.java` | `IEnergyStorage` adapter over `PowerNetMK2` (an HE cable network), for exposing a cable segment as an FE-compatible capability. Converts through the same HE<->RF rate; capacity is unbounded (`Integer.MAX_VALUE`) since a network has no fixed size. |
| `NTMEnergyCapabilityWrapper.java` | `IEnergyStorage` adapter over a `TileEntity` implementing `IEnergyHandlerMK2`/`IEnergyReceiverMK2`/`IEnergyProviderMK2` (HBM's own tile-entity energy API). Supports an optional "accessor" `BlockPos` used to push/pop thread-local context via `CapabilityContextProvider` so multi-block proxies report correctly. |
| `NTMFluidCapabilityHandler.java` | `AttachCapabilitiesEvent<ItemStack>` listener + static registry glue that (a) builds a `Fluid`-name -> HBM `FluidType` lookup table, (b) tracks the set of items that are known NTM fluid containers (full/empty), and (c) attaches a Forge `IFluidHandlerItem` wrapper (`Wrapper`) to those item stacks so vanilla/other-mod fluid tools (buckets, tanks) can fill/drain HBM's custom fluid container items via `FluidContainerRegistry`. |
| `NTMFluidHandlerWrapper.java` | `IFluidHandler` adapter over a `TileEntity` implementing `IFluidReceiverMK2`/`IFluidProviderMK2`/`IFluidUserMK2` (HBM's own tile-entity fluid API, tanks are `FluidTankNTM`). Same accessor/thread-local-context pattern as `NTMEnergyCapabilityWrapper`; also handles NTM's pressure-tiered tank matching (`getReceivingPressureRange`/`getProvidingPressureRange`) which plain Forge `FluidStack` has no field for. |

## Key responsibilities, summarized

1. **Entity-attached persistent/synced data** (`HbmCapability`, `HbmLivingCapability`, `HbmLivingProps`): per-player
   UI/ability state and per-living-entity radiation/contamination state. This is data storage + game logic, not
   inter-mod interop - nothing external ever queries these capabilities, they exist purely so HBM's own systems can
   read/write structured entity state that persists across death/respawn boundaries where relevant.
2. **Forge-Energy (RF) interop adapters** (`NTMBatteryCapabilityHandler`, `NTMCableEnergyCapabilityWrapper`,
   `NTMEnergyCapabilityWrapper`): translate HBM's internal HE energy model (battery items, cable networks, and
   tile-entity energy handlers) into the vanilla-Forge `IEnergyStorage` capability so other tech mods (or vanilla
   hoppers-of-energy equivalents) can interoperate, at a lossy fixed conversion rate.
3. **Forge-Fluid interop adapters** (`NTMFluidCapabilityHandler`, `NTMFluidHandlerWrapper`): translate HBM's
   internal `FluidType`/`FluidTankNTM` model (fluid container items and tile-entity tanks, including pressure
   tiers that Forge fluids have no concept of) into the vanilla-Forge `IFluidHandler`/`IFluidHandlerItem`
   capability.

## Cross-area dependencies (out of this area's scope, needed by later phases)

- `com.hbm.api.energymk2.*` (`IBatteryItem`, `IEnergyHandlerMK2`, `IEnergyReceiverMK2`, `IEnergyProviderMK2`,
  `PowerNetMK2`) - HBM's own energy API, owned by whichever area ports the energy system.
- `com.hbm.api.fluidmk2.*` (`IFluidProviderMK2`, `IFluidReceiverMK2`, `IFluidUserMK2`) and
  `com.hbm.inventory.fluid.*` (`FluidType`, `Fluids`, `FluidTankNTM`, `FluidContainerRegistry`) - HBM's own
  fluid API, owned by the inventory/fluid area.
- `com.hbm.lib.CapabilityContextProvider` - thread-local accessor-position plumbing shared by both energy and
  fluid tile wrappers, used for multi-block "proxy tile reports as if it were the core tile" scenarios. Not in
  this file scope (`com.hbm.lib`) but tightly coupled - whichever area ports `com.hbm.lib` needs to preserve this
  exact contract (push/pop around every capability call that can re-enter tile logic).
- `com.hbm.config.GeneralConfig` (`conversionRateHeToRF`), `com.hbm.config.ServerConfig` (`ENABLE_MKU`),
  `com.hbm.config.RadiationConfig` (`enableContamination`) - config area.
- `com.hbm.handler.ArmorModHandler`, `com.hbm.items.armor.ItemModShield`, `com.hbm.handler.HbmKeybinds` - armor/
  items/input areas, referenced only by `HbmCapability`'s shield-cap default method.
- `com.hbm.main.AdvancementManager`, `com.hbm.packet.*`, `com.hbm.particle.helper.HbmEffectNT`,
  `com.hbm.lib.ModDamageSource` - referenced by `HbmLivingProps` game-logic methods (advancements, network sync,
  particle FX, custom damage source for digamma/asbestos/blacklung deaths). These calls belong to the radiation/
  status-effect *system*, which Phase 0 capability infrastructure does not implement - only the data storage
  (attachment) side is this area's job.

## Recommended NeoForge / Java 21 port plan

The eight CE files actually split into two very different NeoForge idioms - do not port them uniformly under
`RegisterCapabilitiesEvent`.

### 1. Entity-attached data -> `AttachmentType`, not capabilities

`HbmCapability` and `HbmLivingCapability` are pure per-entity data storage with NBT persistence and network sync -
exactly what NeoForge's `AttachmentType` (`net.neoforged.neoforge.attachment.AttachmentType`) replaced Forge's
entity-capability pattern for. Neo Edition already confirms this is the intended replacement
(`com.hbm.lib.ModAttachments`, `com.hbm.extprop.HbmLivingAttachments` / `HbmPlayerAttachments`), and Phase 0 should
follow the same shape:

- Two plain data-holder classes (not interfaces with anonymous DUMMY instances - `AttachmentType.builder(Ctor::new)`
  supplies the default instance, so the Forge `IHBMData`/`IEntityHbmProps` interface + DUMMY-instance pattern goes
  away entirely), e.g. `HbmPlayerAttachment` (was `HbmCapability.HBMData`) and `HbmLivingAttachment` (was
  `HbmLivingCapability.EntityHbmProps`).
- Each needs a `Codec<T>` (NBT persistence, replaces `IStorage`) and a `StreamCodec<RegistryFriendlyByteBuf, T>`
  (network sync, replaces the hand-rolled `ByteBuf serialize/deserialize` default methods) - built with
  `RecordCodecBuilder`/`StreamCodec.composite` over the same fields CE already tracks.
  `HbmLivingCapability`'s versioned NBT format (`fmt` = "v1" doubles vs. legacy floats) has no equivalent concern
  under a fresh `AttachmentType` (no pre-existing save data to migrate in a from-scratch port), so the codec can
  simply always encode doubles - flag this to whoever owns save-compat policy if CE saves must ever be imported.
- A `DeferredRegister<AttachmentType<?>>` in a new `com.hbm.capability.ModAttachments` (mirrors Neo Edition's
  `com.hbm.lib.ModAttachments` naming one level down, since this is this area's own registry, not a shared `lib`
  one) registering `PLAYER_ATTACHMENT` (player-only: jetpack/HUD/magnet/shield/keybinds/reputation/book-flag) and
  `LIVING_ATTACHMENT` (all `LivingEntity`: radiation/contamination/status timers). Use `.copyOnDeath()` only where
  CE's semantics actually persist through death (reputation, book-received flag do; shield/rads arguably should
  reset - this needs a product decision, flagged below).
- Static accessor facades replacing `HbmCapability.getData(Entity)` / `HbmLivingProps.getData(EntityLivingBase)`:
  `entity.getData(ModAttachments.PLAYER_ATTACHMENT.get())` per NeoForge's attachment API - no capability-injection
  static field, no `hasCapability` check needed since attachments always have a default instance.
- `HbmLivingProps`'s static game-logic methods (radiation increment, digamma death, advancement grants, packet
  sends) port essentially unchanged in *shape*, just retargeted to read/write through the attachment accessor
  instead of the capability accessor - but that logic depends on areas out of scope here (advancements, packets,
  damage sources), so this area should port only the attachment classes themselves and leave `HbmLivingProps`'s
  logic methods to the status-effect/radiation system's own phase, stubless (not invented here).
- `ContaminationEffect` becomes a simple `record` with its own NBT `Codec` and `StreamCodec`, referenced from the
  living attachment's codec as a list field.

### 2. Item/fluid capability interop -> `RegisterCapabilitiesEvent`

`NTMBatteryCapabilityHandler` and `NTMFluidCapabilityHandler` are genuinely "generic item/fluid handler capability
plumbing" per this area's brief - they are the infrastructure other phases' items/tile-entities plug into. Port
as:

- A `com.hbm.capability.ModCapabilities` class (or similarly named) with a single `public static void
  register(RegisterCapabilitiesEvent event)` method - registered from the mod's `@SubscribeEvent` on the mod bus
  by the integration step (this area does not touch `MainRegistry.java`; the wiring instruction is: call
  `ModCapabilities.register(event)` from a `@SubscribeEvent` handler for `RegisterCapabilitiesEvent` on the mod
  event bus).
- Forge's `CapabilityEnergy.ENERGY` / `CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY` (both
  `AttachCapabilitiesEvent`-based) become NeoForge's `Capabilities.ItemHandler.ENERGY` equivalent -
  `net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM` and
  `net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM` - registered per-item via
  `event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> ..., itemsImplementingIBatteryItem...)`
  and the fluid equivalent with `Capabilities.FluidHandler.ITEM`. This replaces the
  `AttachCapabilitiesEvent<ItemStack>` + `ICapabilityProvider` wrapper-object pattern entirely - NeoForge
  capability registration is a direct `(stack, context) -> handler` factory keyed by item, no provider object
  needed. I could not find a working NeoForge 21.1 usage of `Capabilities.EnergyStorage`/`Capabilities.FluidHandler`
  in the Neo Edition reference to confirm the exact registration call signature - this must be verified against
  the actual NeoForge 21.1.228 API (javadoc/source) before the implementation stage writes the registration call,
  per the "never invent APIs" rule.
- `Wrapper implements IEnergyStorage` (battery) and `Wrapper implements IFluidHandlerItem` (fluid) port with
  essentially unchanged logic (HE<->RF conversion math, `FluidContainerRegistry` fill/drain matching) - `IEnergyStorage`
  and `IFluidHandler`/`IFluidHandlerItem` are unchanged NeoForge interfaces (still under
  `net.neoforged.neoforge.energy` / `net.neoforged.neoforge.fluids.capability`), only the *registration* mechanism
  changed. `IFluidTankProperties`/`FluidTankProperties` were removed from modern Forge/NeoForge fluid API in favor
  of `IFluidHandler` alone exposing tank count/contents/capacity directly (`getTanks()`, `getFluidInTank(int)`,
  `getTankCapacity(int)`) - `NTMFluidCapabilityHandler.Wrapper` and `NTMFluidHandlerWrapper` both need their
  `getTankProperties()` method reshaped into that newer `IFluidHandler` tank-index API; this is a real, larger
  rewrite than a mechanical port and should be flagged to whoever implements this area's fluid file.
- `NTMCableEnergyCapabilityWrapper` and `NTMEnergyCapabilityWrapper` are plain `IEnergyStorage` adapters over
  HBM's own tile-entity/network energy API (no `AttachCapabilitiesEvent` involved in CE - they're presumably
  constructed on demand wherever a tile entity exposes its capability, likely in the tile-entity base class
  outside this file scope). Port unchanged as plain classes implementing NeoForge's `IEnergyStorage`; the actual
  `registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ...)` wiring belongs to whichever area owns the tile
  entities that construct these wrappers (out of scope here - each tile entity implementing the HE API registers
  its own wrapper instance in `RegisterCapabilitiesEvent`, using this class as the adapter).
- `NTMFluidHandlerWrapper` similarly is a plain adapter, ported unchanged as a class, with block-entity capability
  registration left to the tile-entity-owning area.
- `CapabilityContextProvider`'s thread-local push/pop pattern is unaffected by the capability-registration
  mechanism change and ports as-is (it lives in `com.hbm.lib`, out of this area's file scope, but both energy/fluid
  wrapper classes depend on its exact contract being preserved).

### No Data Component / NBT-on-ItemStack concerns

None of these 8 files read or write raw NBT on an `ItemStack` directly - `HbmCapability`'s and
`HbmLivingCapability`'s NBT handling is entity-capability NBT (now attachment codecs, per above), not item NBT.
The item-side files (`NTMBatteryCapabilityHandler`, `NTMFluidCapabilityHandler`) only read charge/fluid-content
values through their respective item APIs (`IBatteryItem`, `FluidContainerRegistry`) - whatever storage mechanism
those interfaces use (likely Data Components once ported) is the responsibility of the items/inventory areas that
own `IBatteryItem` and `FluidContainerRegistry`, not this area. No NBT key -> Data Component mapping is needed
from this area's files.

## Risks / open questions

1. **Capabilities.EnergyStorage/FluidHandler exact API not confirmed.** Neo Edition has zero working usage of
   NeoForge's modern capability-registration API (`RegisterCapabilitiesEvent`, `Capabilities.EnergyStorage`,
   `Capabilities.FluidHandler`) to copy from. The implementation stage must consult the actual NeoForge 21.1.228
   source/javadoc for `net.neoforged.neoforge.capabilities.Capabilities` and `RegisterCapabilitiesEvent`'s
   `registerItem`/`registerBlockEntity`/`registerBlock` overloads before writing code - do not guess method
   signatures.
2. **IFluidHandler tank-properties API has changed shape** between 1.12.2 Forge (`IFluidTankProperties[]`) and
   modern NeoForge (`IFluidHandler` exposing `getTanks()`/`getFluidInTank(int)`/`getTankCapacity(int)`/
   `isFluidValid(int, FluidStack)` directly, no separate properties object). `NTMFluidCapabilityHandler.Wrapper`
   and `NTMFluidHandlerWrapper` both need a real rewrite of their tank-reporting methods, not a mechanical
   line-for-line port. This is more implementation risk than the rest of this area.
3. **copyOnDeath semantics need a product decision.** CE's Forge capabilities did not auto-migrate across the
   death/respawn entity-swap; whatever custom event-handler logic CE used elsewhere to carry `HBMData` across
   death (not present in these 8 files - likely in a player-clone event handler outside this scope) needs to be
   located and matched against NeoForge's `AttachmentType.Builder.copyOnDeath()` per-field, rather than assumed.
4. **Versioned NBT migration (`fmt` v1 vs legacy float) has no target** in a from-scratch NeoForge attachment
   codec unless old-world-save import is a project goal. Flagged for whoever owns save-compatibility policy.
5. **HE<->RF conversion correctness under Data Components for `IBatteryItem`.** Not this area's file scope, but
   both wrapper classes assume `IBatteryItem.getCharge/getMaxCharge/chargeBattery/dischargeBattery` exist on the
   ported item API - the items area must expose that exact contract (backed by Data Components) for this area's
   `Wrapper` class to compile against.
6. **This area does not include `HbmLivingProps`'s game-logic methods** (radiation math, digamma death, advancement
   grants, contamination effect ticking) - only the attachment/capability data-holder infrastructure. Porting that
   logic is explicitly deferred to whatever phase owns the radiation/status-effect system, per the "infrastructure
   only" instruction for this area.
