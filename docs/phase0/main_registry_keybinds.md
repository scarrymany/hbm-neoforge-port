# Phase 0 Research: Main Registry / Mod Bootstrap, Proxy Pattern & Keybinds

Area key: `main_registry_keybinds`. Research only, no code written.

## Scope note - important discrepancy

The task brief names 19 files under `com.hbm.main.**` but only describes three of
them in detail (`MainRegistry.java`, the `ClientProxy`/`ServerProxy` pair,
`MaterialRegistry.java`) plus `com.hbm.handler.HbmKeybinds`. The other 15 files in
`com.hbm.main` and `com.hbm.main.client` are enormous, self-contained subsystems that
belong to other Phase 0 areas (item/block auto-registration, crafting recipe wiring,
world/impact event handling, client rendering bootstrap, model/animation loading,
structure NBT loading, custom Netty packet transport). I read all 19 files to confirm
their purpose but only actually port the four called out explicitly. Treating the
other 15 as in-scope would collide with concurrent agents working those areas and
would blow this area's line budget by ~11,000 lines. See the class inventory below for
the explicit in/out-of-scope split.

There is also a real naming collision worth flagging: the task's file description says
*"MaterialRegistry.java which wires Mats into the game"*, but CE's actual
`com.hbm.main.MaterialRegistry` has nothing to do with `com.hbm.inventory.material.Mats`
(that class is a ~500+ entry `NTMMaterial` catalog consumed by `CraftingManager`, an
out-of-scope file). CE's real `MaterialRegistry` only registers `ArmorMaterial` and
`ToolMaterial` instances via `EnumHelper.addArmorMaterial`/`addToolMaterial`. I ported
what the file on disk actually does (armor/tool materials), not a `Mats` wiring - `Mats`
is a huge, separate ore/alloy catalog that belongs in a later item/material phase.

## Class inventory

| File | Purpose | In scope for this area |
|---|---|---|
| `main/MainRegistry.java` | CE's `@Mod` entry point. `@SidedProxy` field, creative tabs, FML lifecycle event handlers (`construction`, `preInit`, `initIMC`, `init`, `postInit`, `serverStarting`, `serverStopping`, `serverStopped`, `loadComplete`). Fans out to ~40 other subsystems' `init()`/`register()` calls. | Yes (report only - do not overwrite the existing port `MainRegistry.java`) |
| `main/ClientProxy.java` | Client-side proxy (`extends ServerProxy`). Registers item/entity renderers, particle dispatch, keybind polling (`getIsKeyPressed`), state mappers, audio wrappers, GL capability checks. | Yes (describe target shape) |
| `main/ServerProxy.java` | Dedicated-server-safe base proxy with no-op/default implementations of every proxy method (render info, particles, keybind polling, audio, tooltips). | Yes (describe target shape) |
| `main/MaterialRegistry.java` | Registers ~30 `ArmorMaterial` and ~22 `ToolMaterial` instances via Forge 1.12 `EnumHelper`, then wires their repair items in `initFixMaterials()`. | Yes - **this file is created new** in the port (`com.hbm.main.MaterialRegistry`) |
| `handler/HbmKeybinds.java` | Declares every `KeyBinding`, registers them, and drives the `EnumKeybind` client-input-to-server-packet pipeline (`handleProps`, `handleOverlap`, `onPressedClient`). | Yes - ported to `com.hbm.handler.HbmKeybinds` |
| `main/AdvancementManager.java` (195 lines) | Server-side advancement/achievement grant helper, reads `Tags`/`GeneralConfig`. | No - belongs to an advancements/achievements area |
| `main/AutoRegistry.java` (110 lines) | Reflection-driven tile entity auto-registration + auxiliary machine config loading. | No - belongs to block/tile-entity registration area |
| `main/CraftingManager.java` (1602 lines) | The mod's entire vanilla-style crafting recipe registration, built on `com.hbm.inventory.material.Mats`/`MaterialShapes`/`OreDictManager`. | No - belongs to a recipes/crafting area |
| `main/ModContext.java` (13 lines) | A single `ThreadLocal<Entity>` used to pass detonator context through explosion code. | No - belongs wherever the explosion/bomb code lands (trivial, but out of this area's file list) |
| `main/ModEventHandler.java` (1690 lines) | Huge Forge event bus subscriber: world tick, capability sync, chunk loading, ladder/inventory events, mob spawn logic. | No - belongs to a world/event-handling area |
| `main/ModEventHandlerClient.java` (1533 lines) | Huge client-side Forge event subscriber: HUD rendering, armor table GUI wiring, Baubles integration, color handlers. | No - belongs to a client-rendering/HUD area |
| `main/ModEventHandlerImpact.java` (297 lines) | Meteor/impact-crater world-generation event handling. | No - belongs to a world-generation area |
| `main/ModEventHandlerRenderer.java` (278 lines) | First-person render tweaks (armor model hiding, item renderer overlays, block highlight). | No - belongs to a rendering area |
| `main/NetworkHandler.java` (345 lines) | Custom Netty `MessageToMessageCodec` wrapping FML's `SimpleNetworkWrapper` with packet "precompilation" and threaded dispatch. | No - belongs to the networking/packets area |
| `main/ResourceManager.java` (1677 lines) | Texture/model/shader resource registration, Collada animation loading, splash-screen hooking. | No - belongs to a rendering/resource-loading area |
| `main/StructureManager.java` (99 lines) | Loads/caches NBT world-generation structure templates. | No - belongs to a world-generation area |
| `main/client/DynamicPlaceholderModelLoader.java` (45 lines) | Forge `ICustomModelLoader` for placeholder block models. | No - rendering area |
| `main/client/NTMClientRegistry.java` (628 lines) | TESR (`TileEntityItemStackRenderer`) binding registry plus block/item render layer wiring. | No - rendering area |
| `main/client/StaticDecoBakedModels.java` (262 lines) | Statically baked decorative block models. | No - rendering area |
| `main/client/StaticTesrBakedModels.java` (864 lines) | Statically baked TESR models. | No - rendering area |

## Key responsibilities of the in-scope files

**MainRegistry (CE)**: single `@Mod` class holding global mutable state (creative
tabs, stat trackers, config dir paths, `polaroidID` random pick) and driving every
other subsystem's init through the five FML lifecycle events, in this order:
`preInit` -> `init` -> `postInit` -> `serverStarting`/`serverStopping`/`serverStopped`
-> `loadComplete`. It also owns the `@SidedProxy` field (`MainRegistry.proxy`) that the
rest of the codebase (including `HbmKeybinds`) calls into for anything client-only.

**ClientProxy/ServerProxy**: classic Forge 1.12 sided-proxy pattern.
`ServerProxy` is the dedicated-server-safe default (mostly no-ops or values pulled
from server-side saved data like `TomSaveData`). `ClientProxy extends ServerProxy`
and overrides every method with real client behavior (particle spawning, keybind
polling via `getIsKeyPressed(EnumKeybind)`, renderer registration, GL capability
probing, audio, tooltip HUD).

**MaterialRegistry (CE)**: two flat lists of static fields (`ArmorMaterial`,
`ToolMaterial`) populated in `init()` via Forge's `EnumHelper.addArmorMaterial`/
`addToolMaterial`, then `initFixMaterials()` (called later, after `ModItems.init()`)
back-fills each material's repair item now that item instances exist.

**HbmKeybinds**: declares every `KeyBinding` as a public static field, a
`register()` method that calls `ClientRegistry.registerKeyBinding` for each, a
`@SubscribeEvent keyEvent(KeyInputEvent)` handler used only for opening the
calculator GUI, a `@SubscribeEvent postClientTick` handler working around an
input-timing bug for the ability-cycle keybind vs. right-click, an `EnumKeybind` enum
that is the wire format sent to the server via `KeybindPacket`, and the
`handleProps`/`handleOverlap`/`onPressedClient` machinery that diffs "was this
enum-keybind pressed last tick vs now" and dispatches a packet + local
`IKeybindReceiver` callback on change.

## Cross-area dependencies

- `MainRegistry.preInit/init/postInit` call into dozens of out-of-scope subsystems
  (`ModItems.init()`, `ModBlocks.init()`, `Fluids.init()`, `PacketDispatcher`,
  `HazardRegistry`, `ControlRegistry`, `AutoRegistry`, `CraftingManager` via other
  entry points, etc). None of that fan-out is this area's job to port; only the
  bootstrap shape (constructor + mod-bus/game-bus event registration) is.
- `HbmKeybinds.onPressedClient` depends on `com.hbm.items.IKeybindReceiver`
  (items area) and sends `com.hbm.packet.KeybindPacket` via
  `PacketDispatcher.wrapper` (networking area) - in the port this becomes a
  NeoForge network payload sent through `PacketDistributor`, which the networking
  area owns; this area only needs to know the send-site shape.
  The Neo Edition reference confirms the real class name:
  `com.hbm.network.toserver.KeybindReceiver` sent via
  `PacketDistributor.sendToServer(...)`.
- `HbmKeybinds.keyEvent` opens `com.hbm.inventory.gui.GUICalculator` (inventory/GUI
  area) - the Neo Edition reference replaces this with a `Screen` subclass
  (`CalculatorScreen`) opened via `Minecraft.getInstance().setScreen(...)`.
- `MaterialRegistry.initFixMaterials()` depends on `com.hbm.items.ModItems`
  (item registration area) for repair-item `ItemStack`s, and on `com.hbm.blocks.ModBlocks`
  for `block_schrabidium`. In the port these become `DeferredHolder<Item, ?>`/
  `DeferredHolder<Block, ?>` references resolved through whatever items/blocks area
  produces (`NtmItems`/`NtmBlocks`-equivalent classes in our package layout, likely
  `com.hbm.items.ModItems`/`com.hbm.blocks.ModBlocks` to match CE 1:1).
- `ClientProxy.registerRenderInfo()` and `preInit()` touch nearly every other
  rendering/model/particle subsystem file listed as out-of-scope above - none of
  that content is ported here.

## NeoForge 21.1 port plan

### Mod bootstrap (`com.hbm.main.MainRegistry`, already exists - human-owned)

Confirmed against the Neo Edition reference's `NuclearTechMod.java`:

- `@Mod(MainRegistry.MODID)` on a plain class (no `@SidedProxy` annotation exists in
  NeoForge - sided proxy selection is done manually in the constructor).
- Constructor signature `MainRegistry(IEventBus modEventBus, ModContainer modContainer)`
  is exactly what NeoForge calls; this already matches the skeleton file.
- Proxy selection becomes explicit:
  `proxy = FMLLoader.getDist().isClient() ? new ClientProxy() : new ServerProxy();`
  (confirmed real usage in `NuclearTechMod` constructor).
- Every "register a DeferredRegister-backed catalog" call other areas produce
  (items, blocks, fluids, entities, data components, sounds, creative tabs, block
  entities, menus, particles, features, attachments) is invoked from this
  constructor as `SomeAreaRegistry.register(modEventBus)`. This area does not own
  any of those catalogs; it only owns wiring `MaterialRegistry.init()` in here (see
  below) and config-dir setup that mirrors CE's `preInit` (`configDir`,
  `configHbmDir`).
- CE's `FMLPreInitializationEvent`/`FMLInitializationEvent`/`FMLPostInitializationEvent`
  three-phase split has no direct NeoForge equivalent; NeoForge registration is
  driven by `RegisterEvent`/`DeferredRegister` (mod bus) plus ordinary constructor
  code for anything that isn't registry-shaped. `FMLCommonSetupEvent` is the closest
  analogue to CE's `init()` for "do work after all registries exist."
  `FMLClientSetupEvent` (client-only, mod bus) is the analogue for anything in CE's
  `ClientProxy.preInit/init/postInit` that isn't itself a registration.
- Server lifecycle hooks (CE's `serverStarting`/`serverStopping`/`serverStopped`/
  `loadComplete`) map to NeoForge's game-bus `ServerStartingEvent`,
  `ServerStoppingEvent`, `ServerStoppedEvent`, and `FMLLoadCompleteEvent`
  respectively - these must be registered on `NeoForge.EVENT_BUS`
  (game bus), not the mod bus, exactly like CE registers `ModEventHandler` etc. on
  `MinecraftForge.EVENT_BUS`.

### Client bootstrap (new class, e.g. `com.hbm.main.ClientModRegistry` or folded into `ClientProxy`)

Confirmed against `NuclearTechModClient.java`:

- A second `@Mod(value = MainRegistry.MODID, dist = Dist.CLIENT)` class is the
  correct pattern for client-only mod-bus subscription, with constructor
  `(ModContainer modContainer)`.
- Pair it with `@EventBusSubscriber(value = Dist.CLIENT)` (or
  `@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)`) on the
  same class or a sibling, so `@SubscribeEvent`-annotated static methods for
  `FMLClientSetupEvent`, `RegisterKeyMappingsEvent`, `RegisterColorHandlersEvent`,
  etc. are picked up automatically without manual `IEventBus.register()` calls.
- I recommend **not** creating this class myself: it is effectively "the client
  half of MainRegistry" and its member list belongs to whichever area owns
  rendering bootstrap (`ModEventHandlerClient`/`ResourceManager`/`NTMClientRegistry`
  equivalents), all explicitly out of scope here. This report documents the shape;
  the integration step (or a rendering-area agent) should create it.

### `com.hbm.main.ClientProxy` / `com.hbm.main.ServerProxy`

- Port as plain classes, not sided-annotated. `ServerProxy` keeps default/no-op
  method bodies exactly as CE does (this area's slice of it: `getIsKeyPressed`
  returning `false`, `me()` returning `null`). `ClientProxy extends ServerProxy` and
  overrides `getIsKeyPressed(EnumKeybind)` and `me()` for real client behavior,
  matching CE's `ClientProxy.getIsKeyPressed`/`me()`.
- `me()` returns `net.minecraft.world.entity.player.Player` in 1.21 (CE's
  `EntityPlayer`); `ClientProxy.me()` becomes
  `Minecraft.getInstance().player` (confirmed pattern: Neo Edition's
  `NuclearTechMod.proxy.me()` is used the same way in `NuclearTechModClient`).
  `ServerProxy.me()` returns `null`, same as CE.
- Every other CE `ServerProxy`/`ClientProxy` method (particles, renderer
  registration, audio, tooltip HUD, GL caps) belongs to the out-of-scope
  rendering/audio/particle areas and should be added to these classes by those
  areas, not invented here. I am only porting the two methods that
  `HbmKeybinds`/keybind flow actually needs (`getIsKeyPressed`, `me`), to keep this
  area's file self-contained and buildable in isolation.

### `com.hbm.main.MaterialRegistry` (new file, ported in full)

- `ArmorMaterial`/`ToolMaterial` (Forge 1.12, via `EnumHelper`) do not exist in
  1.21 NeoForge. 1.21's armor/tool material system is data-driven: `ArmorMaterial`
  is now a record held in a NeoForge/vanilla registry (`Registries.ARMOR_MATERIAL`),
  and tool "materials" were replaced by `Tier` (`net.minecraft.world.item.Tier`,
  typically implemented via `SimpleTier` or a custom `Tier` record) referenced
  directly by tool item constructors - there is no global add-and-store-in-a-field
  step comparable to `EnumHelper.addToolMaterial`.
- Recommended NeoForge 21.1 port shape: register a `DeferredRegister<ArmorMaterial>`
  (registry key `Registries.ARMOR_MATERIAL`) for every CE `aMatXxx` field, and
  expose a set of `public static final Holder<ArmorMaterial>` (or
  `DeferredHolder<ArmorMaterial, ArmorMaterial>`) constants with the same field
  names as CE (`aMatSteel`, `aMatSchrab`, etc.) so downstream armor item code stays
  a near-mechanical port. For tool materials, define `public static final Tier`
  constants (custom `Tier` implementations carrying CE's five numbers: harvest
  level analogue, durability, mining speed, attack damage bonus, enchantability) -
  NeoForge 21.1 tiers are plain objects, not a registry, so no `DeferredRegister` is
  needed for those.
- `initFixMaterials()`'s "repair item" concept: 1.21's `ArmorMaterial` record takes
  a `TagKey<Item>` (`repairIngredient`) at construction time instead of a
  post-hoc mutable setter, and `Tier` similarly takes its repair-ingredient
  `TagKey<Item>`/`Ingredient` supplier at construction. This means CE's two-phase
  "declare in `init()`, back-fill repair item in `initFixMaterials()` once items
  exist" split cannot be preserved as-is: the port must either (a) declare item
  tags for each repair ingredient ahead of item registration and reference the tag
  at material-construction time, or (b) use a `Supplier<Ingredient>` if the specific
  `ArmorMaterial`/`Tier` constructor accepts one lazily. I recommend (a) - define
  `TagKey<Item>` constants in `MaterialRegistry` (e.g. `hbm:repair_schrabidium`)
  and have the items area add each repair item to that tag - since this keeps
  `MaterialRegistry` fully self-contained and registerable before `ModItems` exists,
  matching NeoForge's registration-order constraints (`RegisterEvent` for one
  registry cannot safely depend on `DeferredHolder`s from another registry not yet
  populated).
- This is new code (the class does not yet exist in the port project), so it will
  be written as `com.hbm.main.MaterialRegistry` with a `public static void
  register(IEventBus modEventBus)` method the human integration step calls from
  `MainRegistry`'s constructor.
- Not a NBT/data-component concern: no `ItemStack` NBT keys are involved anywhere
  in this file - it only builds material definition objects, no per-stack state.

### `com.hbm.handler.HbmKeybinds` (ported in full)

Confirmed almost 1:1 against the Neo Edition reference's own
`com.hbm.handler.HbmKeybinds` (same class, same responsibility, real working
NeoForge 21.1 code) - I recommend porting CE's file to match that shape closely
since it is proven to compile and run on this exact NeoForge version:

- `KeyBinding` -> `net.minecraft.client.KeyMapping`. Constructor takes
  `InputConstants.Type` + key code constant (`InputConstants.KEY_N` etc, from
  `com.mojang.blaze3d.platform.InputConstants`) instead of raw LWJGL2
  `Keyboard.KEY_N` ints.
- Registration: no `ClientRegistry.registerKeyBinding` in NeoForge. Use
  `@SubscribeEvent public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)`
  with `event.register(KEY_MAPPING)` per binding, on a class annotated
  `@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)` so it
  self-registers on the mod bus without any manual `IEventBus.register()` call from
  `MainRegistry`.
- CE's `KeyInputEvent`/`TickEvent.ClientTickEvent` split becomes NeoForge's
  `net.neoforged.neoforge.client.event.InputEvent.Key` /
  `InputEvent.MouseButton.Pre` (for raw key/mouse edge detection) and
  `net.neoforged.neoforge.client.event.ClientTickEvent.Post` (end-of-tick, replacing
  CE's `TickEvent.ClientTickEvent` + `Phase.END` check) - all `@SubscribeEvent`
  static methods on the same event-bus-subscriber class, no manual registration.
- CE's reflection-based `hashHandle`/`KeyBindingMap` overlap hack
  (`handleOverlap`, tied to Forge 1.12 internals via `MethodHandleHelper`) has no
  NeoForge equivalent and should **not** be ported: the Neo Edition reference
  confirms this by leaving its own `handleOverlap` entirely commented out/unused.
  I recommend dropping it from the port rather than reinventing a hack against
  1.21 internals; note this as an intentional behavior change in the port (keybind
  conflict overlap between hbm keys and vanilla/other-mod keys sharing the same
  physical key will no longer be silently forced).
- `EnumKeybind` enum: port field-for-field. CE's `TOGGLE_JETPACK`/`TOGGLE_HEAD` name
  order differs slightly from the Neo Edition reference's enum (`TOGGLE_JETPACK`,
  `TOGGLE_MAGNET`, `TOGGLE_HEAD` order swapped, plus extra values `DUCK`, `DASH`,
  `TRAIN`) - **preserve CE's exact enum member set and order** since ordinal values
  are the network wire format (`KeybindPacket`/`KeybindReceiver` serializes by
  ordinal in both codebases); do not reconcile with the Neo Edition reference's enum,
  it is informative-only for API shape, never for game content/ordering per the
  project's hard rules.
- `postClientTick`'s ability-cycle-vs-right-click workaround: port the intent
  (detect `keyBindUseItem`/`options.keyUse` colliding with `abilityCycle`'s bound
  key and suppress the double-fire) using `mc.options.keyUse.getKey()` compared to
  the ability-cycle `KeyMapping.getKey()` - directly mirrored in the Neo Edition
  reference's own `onClientTick`/`handleProps`.
- `IKeybindReceiver.canHandleKeybind`/`handleKeybindClient` interface dependency
  stays the same shape, just retargeted at the ported `com.hbm.items.IKeybindReceiver`
  (items area) and `net.minecraft.world.item.ItemStack`/
  `net.minecraft.world.entity.player.Player`.
- Sending the keybind state to the server: CE uses
  `PacketDispatcher.wrapper.sendToServer(new KeybindPacket(...))`. Port target is
  `net.neoforged.neoforge.network.PacketDistributor.sendToServer(new
  KeybindPacket(key, current))` where `KeybindPacket` becomes a `CustomPacketPayload`
  record (networking area's responsibility to define; this area only needs the
  call-site shape, confirmed real in the Neo Edition reference's
  `PacketDistributor.sendToServer(new KeybindReceiver(...))`).
- `GUICalculator` -> a NeoForge `Screen` subclass (inventory/GUI area's
  responsibility to create); this area's `HbmKeybinds` only needs
  `Minecraft.getInstance().setScreen(new CalculatorScreen())` at the call site,
  confirmed real in the Neo Edition reference.
- No NBT/data-component concerns in this file either - it operates on live
  `ItemStack` type checks (`instanceof IKeybindReceiver`), not persisted NBT data.

## NBT -> Data Component mapping

None. Neither `MaterialRegistry` nor `HbmKeybinds` reads or writes any `ItemStack`
NBT compound in CE. No `DataComponentType` work is required for this area.

## Integration instructions (for the human integration step)

Call these from `MainRegistry`'s constructor and client-setup, in this order:

1. `com.hbm.main.MaterialRegistry.register(IEventBus modEventBus)` - call once from
   `MainRegistry`'s constructor, before any area that references
   `MaterialRegistry`'s `Holder<ArmorMaterial>`/`Tier` constants (i.e. before the
   items area's armor/tool item `DeferredRegister` entries are built, since those
   entries need these constants at registration time).
2. No explicit call is needed to wire up `com.hbm.handler.HbmKeybinds` - it
   self-registers via `@EventBusSubscriber(modid = MainRegistry.MODID, value =
   Dist.CLIENT)` the moment the class is loaded on the client, exactly like the Neo
   Edition reference. The integration step only needs to ensure the class is
   reachable on the classpath (no explicit `Class.forName`/static reference is
   required by NeoForge's `@EventBusSubscriber` scanning, but if the project's mod
   descriptor restricts subscriber scanning to specific packages, confirm
   `com.hbm.handler` is included).
3. `ClientProxy`/`ServerProxy` are plain data/behavior objects, not registered with
   any event bus. The integration step assigns
   `MainRegistry.proxy = FMLLoader.getDist().isClient() ? new ClientProxy() : new
   ServerProxy();` inside `MainRegistry`'s constructor, mirroring the Neo Edition
   reference exactly. `MainRegistry.proxy`'s declared type should be `ServerProxy`
   (the common base), matching CE's own `public static ServerProxy proxy;` field
   typed at the base class even on the client.

## Risks / open questions

1. **`MaterialRegistry`/`Mats` naming collision** (detailed above) - I built the
   report and port plan around the file CE actually ships
   (`com.hbm.main.MaterialRegistry`, armor/tool materials only). If the intent was
   actually to have this area port `com.hbm.inventory.material.Mats` (the ~500-entry
   ore/alloy catalog consumed by `CraftingManager`), that is a much larger, separate
   effort that should be its own area - please confirm which was intended before the
   write stage begins.
2. **1.21 `ArmorMaterial`/`Tier` repair-ingredient timing.** CE's `initFixMaterials()`
   runs after `ModItems.init()` specifically so repair items exist as concrete
   `ItemStack`s. NeoForge's `ArmorMaterial`/`Tier` want their repair ingredient
   (tag or ingredient) at construction time, which happens during mod-bus
   `RegisterEvent` - before items are guaranteed registered. The tag-based approach
   in the port plan above avoids a hard ordering dependency, but it means the items
   area must independently know to add each repair item to the corresponding
   `hbm:repair_*` tag (via a datagen tag provider or static tag registration) - this
   cross-area contract should be written down explicitly wherever the items area's
   report lands.
3. **Keybind overlap hack drop.** Dropping CE's `handleOverlap` reflection hack is a
   deliberate, visible gameplay behavior change (mod keybinds bound to the same key
   as vanilla/other-mod keybinds will behave like normal Minecraft keybind
   collisions instead of CE's forced-priority override). Confirmed intentional by
   the Neo Edition reference doing the same, but flagging since it is a behavior
   change, not a pure syntax port.
4. **Where the "second `@Mod` client class" lives.** I recommend it be created by
   whichever area owns client rendering bootstrap, not this area, since its member
   list (color handlers, particle registration, HUD rendering) is entirely
   out-of-scope content for `main_registry_keybinds`. Flagging so it isn't dropped
   entirely between areas - `RegisterKeyMappingsEvent` itself doesn't need it (this
   area's `HbmKeybinds` self-subscribes), but other Phase-0-relevant client setup
   (e.g. `FMLClientSetupEvent`-driven work) needs a home.
