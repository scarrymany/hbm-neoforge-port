# Phase 0 Port Journal — Full Digest (15 Areas)

Source: `wf_8afa9dfc-e74/journal.jsonl` (112 lines / 56 agent results). Mapped each area's research→implement→review→fix line numbers, pulled implement `filesWritten`/`integrationInstructions`/`deviationsFromPlan`/`knownIssues`, review `parityOk`/`findings`, and fix `filesChanged`. All file paths below are relative to `C:\Users\Sergo127\Desktop\hbms\` unless shown absolute. Spot-checked on disk: `com/hbm/main/{MainRegistry,MaterialRegistry,ServerProxy,ClientProxy}.java` exist; `com/hbm/damage/datagen/ModDamageTypeTagsProvider.java` exists (package `com.hbm.damage.datagen`) and the old `com/hbm/datagen/` directory is gone — confirms the fix-stage rename described below.

Note on a workflow quirk you should know before reading further: several areas got a **fix pass even though `parityOk: true`** (fluid, hazard, sound, config, base_blocks, api_interfaces, damage_types) — apparently any non-empty findings list triggered a fix regardless of the boolean. Only energy, packet, base_items, creativetabs had zero findings and no fix stage.

---

## lib_util

**Files on disk** (`com/hbm/lib/**` excl. ModDamageSource, `com/hbm/util/**`), 64 files written, 5 later edited by fix:
- `com/hbm/lib/{CapabilityContextProvider,DirPos,HBMSoundHandler,HbmCollection,InventoryHelper,ItemStackHandlerWrapper,Library,ObjObjDoubleConsumer,ObjectDoubleFunction,RecoilHandler,TLPool,TriFunction}.java` — **TLPool.java fixed**
- `com/hbm/lib/internal/{AbstractUnsafe,InternalUnsafeWrapper,InternalUtil,MethodHandleHelper,SunUnsafeWrapper,TrustedLookupAccessor,UnsafeHolder,package-info}.java`
- `com/hbm/lib/maps/{AbstractEntry,ConcurrentAutoTable,LongObjectBiFunction,LongObjectConsumer,LongObjectRefConsumer,NonBlockingHashMap,NonBlockingHashMapLong,NonBlockingHashSetLong,NonBlockingLong2LongHashMap,RangeUtil}.java`
- `com/hbm/lib/queues/MpscLinkedAtomicLongQueue.java`
- `com/hbm/util/{AdjacencyGraph,ArmorRegistry,Calculator,ChatBuilder,Clock,ConcurrentBitSet,DecodeException,Either,EnumUtil,ExponentialMovingAverage,FacingUtil,GuiUtil,ItemStackUtil,MpscCollector,MpscIntArrayListCollector,ObjectIntPair,ObjectPool,OffHeapBitSet,RandomPool,ReferenceIntTuple,SectionKeyHash,ShadyUtil,SoundUtil,SwappedHashSet,TagsUtil,Tuple,Vec3dUtil,WeightedRandom,WeightedRandomGeneric,WeightedRandomObject}.java` — **ItemStackUtil.java, ObjectPool.java fixed**
- `com/hbm/util/i18n/{I18nClient,I18nServer,I18nUtil,ITranslate}.java` — **I18nServer.java fixed**
- `com/hbm/lib/InventoryHelper.java` — **fixed**

**Integration instructions:** In `MainRegistry`'s constructor (mod-bus setup), call `com.hbm.lib.HBMSoundHandler.register(modEventBus)` — this fires `HBMSoundHandler.SOUND_EVENTS.register(modEventBus)`; without it every `HBMSoundHandler.xxx.get()` throws. Nothing else in this area needs a call — `RecoilHandler` self-registers via `@EventBusSubscriber(Dist.CLIENT, modid = MainRegistry.MODID)`. Flagged (not applied) build.gradle dependency: `org.jctools:jctools-core:4.0.5` needed for the dropped queue classes and to restore `TLPool`'s original `MpmcArrayQueue`.

**Review outcome:** `parityOk: false`. Findings:
- **critical** — `TLPool.java`: `catch (RuntimeException _)` uses JDK 22+ unnamed-variable syntax (preview-only on this project's JDK 21, no `--enable-preview`) → compile failure.
- **critical** — `util/ObjectPool.java`: same unnamed-catch-variable issue.
- **minor** — `InventoryHelper.java`: claimed cross-reference to a working `Capabilities.ItemHandler.BLOCK` usage in the Neo Edition reference doesn't actually exist there (verification-methodology gap, not necessarily wrong code).
- **minor** — `ItemStackUtil.java`: `readStacksFromNBT` loop bound changed from CE's `count` to `list.size()`, an undocumented behavior change.
- **minor** — `i18n/I18nServer.java`: server-side `resolveKey` now always returns the literal `"I18N CALL SERVERSIDE - GREAT JOB"` instead of CE's raw-key fallback, undocumented.
Fix ran and touched exactly the 5 files flagged (`TLPool.java`, `ObjectPool.java`, `ItemStackUtil.java`, `InventoryHelper.java`, `I18nServer.java`).

**Known issues / deviations (not fixed, still true):**
- `jctools-core` is NOT on the classpath (build.gradle has no dependencies block, out of this area's authority) — blocks the `lib/queues` family except `MpscLinkedAtomicLongQueue`; `TLPool` was reimplemented on `ArrayBlockingQueue` instead of `MpmcArrayQueue` (behaviorally equivalent, not lock-free).
- `HBMSoundHandler` uses `DeferredHolder<SoundEvent,SoundEvent>` fields — every call site must add `.get()`.
- All 379 sound paths mechanically lowercased/snake_cased (ResourceLocation path rules) — asset pipeline must rename `.ogg`/`sounds.json` to match.
- `Library.java` ported as a ~20-line stub (only `onSpinWait()`) — the other ~2500 lines deferred whole (too many undefined cross-area types to port safely).
- Entirely dropped (no 1.21 shape): `lib/ForgeDirection.java`, `lib/NTMBlockContainer.java`, `util/UnlistedPropertyBoolean/Integer.java`, the 3 jctools-dependent queue classes.
- Deferred whole (not stubbed): `lib/HbmWorld.java`, `lib/HbmWorldGen.java`, `lib/HbmChestContents.java`, `util/{BobMathUtil,InventoryUtil,ContaminationUtil,DamageResistanceHandler,EntityDamageUtil,Keypad,KeypadClient,Vec3NT,MutableVec3d,Compat,CompatBlockReplacer,CompatDynamicTrees,CompatExternal,CompatFluidRegistry,ChunkShapeHelper,ChunkSpanAccumulator,ChunkUtil,CrashHelper,CrucibleUtil,DelayedTick,EnchantmentUtil,LootGenerator,MobUtil,NetworkUtil,NoteBuilder,RTGUtil,TrackerUtil,ColorUtil}.java` and all client-rendering GL1 utilities.
- Files ported but will not compile standalone until other areas land: `util/ArmorRegistry.java` (needs `com.hbm.api.item.IGasMask`, `com.hbm.handler.{ArmorUtil,ArmorModHandler}`), `util/ConcurrentBitSet.java`/`OffHeapBitSet.java` (need `com.hbm.interfaces.BitMask`), `util/Tuple.java` (needs `com.hbm.interfaces.Spaghetti`).
- One real bug fix vs. CE/Neo: added the missing `TagsUtil.putCustomData(stack, tag)` write-back in `ItemStackUtil.addTooltipToStack`/`addStacksToNBT`.
- Not build-verified; the `sun.misc.Unsafe`/`jdk.internal.misc.Unsafe` reflection path in `lib/internal` is flagged as unverified.

---

## material

**Files on disk:**
- `src/main/java/com/hbm/inventory/material/NTMMaterial.java`
- `src/main/java/com/hbm/inventory/material/MaterialShapes.java` — **fixed**
- `src/main/java/com/hbm/inventory/material/Mats.java` — **fixed**

**Integration instructions:** None needed for Phase 0 — pure static data, no `DeferredRegister`, no `register(IEventBus)`. Referencing `Mats.MAT_IRON` etc. triggers class-load/static population. Optional non-required hint: touch `Mats.orderedList.size()` during common setup if deterministic early population is ever wanted.

**Review outcome:** `parityOk: false`. Findings:
- **major** — `Mats.java`: `materialOreEntries` (populated by `registerOre`, the seam kept for a future `MatDistribution` port) is **never read** by `getMaterialsFromItem`/`getSmeltingMaterialsFromItem` — only the tag-based map and `materialEntries` are consulted. Once `MatDistribution` is ported and calls `registerOre(...)`, those lookups will silently return empty forever.
- **minor** — `MaterialShapes.java`: `TINY` has a non-null `registryName` ("tiny") while being `.noAutogen()`, contradicting the file's own documented invariant that non-backing shapes get `null`.
Fix ran, touched `Mats.java` and `MaterialShapes.java`.

**Known issues / deviations:**
- `MatDistribution.java` was **NOT ported** (was in the plan's target list) — needs `ModItems`, `ModBlocks`, `OreDictManager`, `RecipesCommon`, `SerializableRecipe`, none of which exist yet. `materialEntries`/`materialOreEntries` exist but start empty; `Mats.registerEntry(Item, Object...)`/`registerOre(String, Object...)` kept as the exact seam for a future mechanical port of CE's ~40 registration calls.
- `NTMMaterial` constructor now takes `String... names` directly (dropped `OreDictManager.DictFrame` dependency).
- `Mats.getMaterialsFromItem`'s tag lookup is real but returns empty results until Phase 1 registers items with `c:<tagFolder>/<material>` tags — expected, not a bug.
- `Mats.formatAmount` return type changed `String` → `MutableComponent` (dropped `I18nUtil` dependency).
- CE's `ItemScraps.getMats()` special case replaced with an empty `List<Function<ItemStack,MaterialStack>> specialCaseResolvers` extension point — items area should register a resolver into it.
- New `registryName`/`tagFolder`/`buildRegistryName()`/`commonTag()` naming convention added to `MaterialShapes` — **needs explicit Phase 1 sign-off** before item generation starts (singular snake_case item id + plural common tag folder, e.g. `hbm:iron_ingot` / `c:ingots/iron`).
- Not build-verified.

---

## fluid

**Files on disk:**
- `src/main/java/com/hbm/inventory/fluid/FluidStack.java`
- `src/main/java/com/hbm/inventory/fluid/FluidType.java`
- `src/main/java/com/hbm/inventory/fluid/Fluids.java`
- `src/main/java/com/hbm/inventory/fluid/trait/FluidTrait.java`
- `src/main/java/com/hbm/inventory/fluid/trait/FluidTraitSimple.java`
- `src/main/java/com/hbm/inventory/fluid/trait/FT_{Corrosive,Combustible,Flammable,Heatable,Coolable,Pheromone,Poison,Polluting,PWRModerator,Rocket,VentRadiation}.java`
- `src/main/java/com/hbm/inventory/fluid/trait/FT_Toxin.java` — **fixed**
- `src/main/java/com/hbm/api/fluidmk2/IFluidRegisterListener.java`

**Integration instructions:** Pure-data classes — no DeferredRegister/event-bus wiring. Two things needed in `com.hbm.main.MainRegistry` (not touched by this area):
1. Add `public static File configHbmDir` field, initialized via `new File(FMLPaths.CONFIGDIR.get().toFile(), "hbmConfig")` (create dir if missing) before anything calls `Fluids.init()`.
2. Call `com.hbm.inventory.fluid.Fluids.init()` exactly once after `configHbmDir` is set (e.g. end of `MainRegistry` constructor, or a `FMLCommonSetupEvent` handler). `Fluids.reloadFluids()` may optionally be called again at a later lifecycle point.
Note: CE's `com.hbm.Tags.MODID` doesn't exist to port (ForgeGradle-generated) — `Fluids.java` already uses `MainRegistry.MODID` instead.

**Review outcome:** `parityOk: true` but findings recorded (fix still ran):
- **minor** (tooltip-fidelity) — `FT_Toxin.java`: `ToxinEffects.addInfo()` lost CE's roman-numeral/duration-formatted tooltip, now shows raw numbers (e.g. "2 4s" instead of "II 0:04").
- **minor** (unverified-api) — `FT_Toxin.java`: `deserializeJSON` calls `mobEffect.builtInRegistryHolder()` on a plain `MobEffect`, a pattern the implementer itself flagged as unconfirmed (only Item/Block usages of that method were found in the reference).
Fix ran, touched `FT_Toxin.java` only.

**Known issues / deviations:**
- Dropped entire Forge-Fluid-compat bridge (`getFF`/`ffBan`/etc.) per plan and Neo Edition precedent.
- Did **not** create any `net.neoforged.neoforge.fluids.FluidType`/`Fluid` DeferredRegister — CE's `FluidType` is a distinct custom domain class, not NeoForge's. That DeferredRegister pair is owed by whichever phase ports actual placeable-fluid blocks.
- Dropped `com.hbm.util.I18nUtil` dependency — tooltips use `Component.translatable` directly, kept CE's original lang keys.
- `FT_Toxin.ToxinDirectDamage` now stores `ResourceKey<DamageType>` resolved via `damageSources().source(key)` at hurt-time instead of an eagerly built `DamageSource`.
- Intentionally did **not** wire the 5-fluid `FT_Toxin` block from `Fluids.init()` (CHLORINE/PHOSGENE/MUSTARDGAS/ESTRADIOL/REDMUD) — needs `com.hbm.lib.ModDamageSource`, `com.hbm.potion.HbmPotion`, `com.hbm.util.ArmorRegistry`/`ArmorUtil`, none ported yet. ~5-line addition once those land.
- Only ported `com.hbm.api.fluidmk2.IFluidRegisterListener` from api/fluid scope — did NOT port `FluidNetMK2`, `FluidNode`, `IFluidConnectorMK2/BlockMK2`, `IFluidPipeMK2`, `IFluidProviderMK2/ReceiverMK2/StandardReceiver/Sender/TransceiverMK2`, `IFluidUserMK2`, `IFillableItem` — the pipe/network layer is explicitly out of scope (shares base with energymk2).
- Forward-references not yet resolvable: `FluidTankNTM` (tank phase), `FluidNetMK2`/`INetworkProvider`/`NodeNet` (pipe-network phase), `com.hbm.main.MainRegistry.configHbmDir` (must be added per above), `com.hbm.render.misc.EnumSymbol`, `com.hbm.handler.pollution.PollutionHandler`, `com.hbm.util.BobMathUtil.getShortNumber`, `com.hbm.handler.radiation.ChunkRadiationManager.proxy.incrementRad`, `com.hbm.util.ArmorRegistry`/`com.hbm.handler.ArmorUtil`.
- Not build-verified.

---

## energy (energymk2)

**Files on disk** (no fix stage ran):
- `src/main/java/com/hbm/api/energymk2/IEnergyConnectorMK2.java`
- `src/main/java/com/hbm/api/energymk2/IEnergyConnectorBlock.java`
- `src/main/java/com/hbm/api/energymk2/IEnergyConductorMK2.java`
- `src/main/java/com/hbm/api/energymk2/IEnergyHandlerMK2.java`
- `src/main/java/com/hbm/api/energymk2/IEnergyProviderMK2.java`
- `src/main/java/com/hbm/api/energymk2/IEnergyReceiverMK2.java`
- `src/main/java/com/hbm/api/energymk2/Nodespace.java`
- `src/main/java/com/hbm/api/energymk2/PowerNetMK2.java`
- `src/main/java/com/hbm/api/energymk2/IBatteryItem.java`

**Integration instructions:** **No MainRegistry hook needed at all.** This is plain interfaces + the `PowerNetMK2` graph class, consumed via `instanceof` by whichever area writes conductor/provider/receiver `BlockEntity` implementations (confirmed against the Neo Edition reference — no `RegisterCapabilitiesEvent`/`BlockCapability` used for HE). Hard compile dependencies that must land with exact contracts:
1. `com.hbm.uninos.{NodeNet,GenNode,INetworkProvider,UniNodespace}` — must use CE's real generic signature `NodeNet<R, P, L extends GenNode<N>, N extends NodeNet<R,P,L,N>>` with fastutil `Object2LongOpenHashMap` fields exposing `.object2LongEntrySet().fastIterator()` (NOT Neo Edition's simplified plain-HashMap version) or `PowerNetMK2` won't compile. Whoever owns `uninos` also owns wiring `UniNodespace`'s tick loop into NeoForge.
2. `com.hbm.api.tile.ILoadedTile` — needs `boolean isLoaded();`.
3. `com.hbm.lib.DirPos` — needs `DirPos(int x,int y,int z, Direction dir)`, `getPos()`, `getDir()`.
4. `com.hbm.util.Compat` — needs `static BlockEntity getBlockEntityStandard(Level, BlockPos)`.
5. `com.hbm.util.Tuple.ObjectLongPair<T>` — needs `(T key, long value)` + `getKey()`/`getValue()`.
6. A `hbm:battery_charge` `DataComponentType<Long>` must be registered by the item/data-component registry area (this area only defines `IBatteryItem.getChargeComponent()`).

**Review outcome:** `parityOk: true`, findings empty. **No fix stage.**

**Known issues / deviations:**
- Dropped the Forge/NeoForge Energy interop bridge (`CapabilityEnergy.ENERGY`/`GeneralConfig.conversionRateHeToRF`) and the `particleDebug` packet-sending branches — both allowed to be deferred per the task brief.
- `IBatteryItem`'s charge accessors (`chargeBattery`/`setCharge`/`dischargeBattery`/`getCharge`) became **default methods** backed by the shared `hbm:battery_charge` component, replacing CE's per-item raw-NBT boilerplate — a deliberate improvement.
- `PowerNetMK2` ported byte-for-byte from CE's algorithm (BigInteger overflow guard, round-robin fairness, `ReentrantLock`), **not** Neo Edition's simplified version — this is only correct if `uninos` area keeps the fastutil-backed map types; if it ports the simpler version instead, `PowerNetMK2.update()`/`sendPowerDiode()`/`extractPowerDiode()` need a non-trivial rework.
- Package will not compile in isolation until `com.hbm.uninos.*`, `ILoadedTile`, `DirPos`, `Compat`, `Tuple` exist (expected under the wave model).

---

## hazard

**Files on disk:**
- `src/main/java/com/hbm/hazard/HazardData.java` — **fixed**
- `src/main/java/com/hbm/hazard/HazardEntry.java`
- `src/main/java/com/hbm/hazard/HazardComponents.java`
- `src/main/java/com/hbm/hazard/HazardSystem.java`
- `src/main/java/com/hbm/hazard/HazardRegistry.java`
- `src/main/java/com/hbm/hazard/helper/HazardHelper.java`
- `src/main/java/com/hbm/hazard/modifier/{IHazardModifier,HazardModifierFuelRadiation,HazardModifierRTGRadiation,HazardModifierRBMKRadiation,HazardModifierRBMKHot}.java`
- `src/main/java/com/hbm/hazard/transformer/{IHazardTransformer,HazardTransformerRadiationNBT,HazardTransformerForgeFluid,HazardTransformerRadiationME,HazardTransformerPostCustom}.java`
- `src/main/java/com/hbm/hazard/transformer/HazardTransformerRadiationContainer.java` — **fixed**
- `src/main/java/com/hbm/hazard/type/{IHazardType,HazardTypeRadiation,HazardTypeContaminating,HazardTypeDigamma,HazardTypeHot,HazardTypeBlinding,HazardTypeAsbestos,HazardTypeCoal,HazardTypeCold,HazardTypeToxic,HazardTypeHydroactive,HazardTypeExplosive,HazardTypeDangerousDrop,HazardTypeUnstable}.java`

**Integration instructions:**
1. In `MainRegistry`'s constructor: `com.hbm.hazard.HazardComponents.register(modEventBus);` (registers `bonus_radiation`, `unstable_decay_timer` data components).
2. During common setup (inside `event.enqueueWork(...)` of whatever `FMLCommonSetupEvent` handler exists, e.g. a future `com.hbm.main.CommonEvents.commonSetup`), call in order: `HazardRegistry.registerTrafos();` then `HazardRegistry.registerItems();` (currently no-op) then `HazardRegistry.registerContaminatingDrops();` (currently no-op).
3. In the shared entity-tick subscriber: `if (entity instanceof Player p) HazardSystem.updatePlayerInventory(p);` / `if (entity instanceof ItemEntity ie) HazardSystem.updateDroppedItem(ie);` / `if (entity instanceof LivingEntity le) HazardSystem.updateLivingInventory(le);` — these self-throttle, call unconditionally every tick, do not add an extra rate gate.
4. For tooltips: `HazardSystem.addHazardInfo(stack, player, tooltipComponents)` (client-only).

**Review outcome:** `parityOk: true` but findings recorded (fix ran):
- **major** — `HazardData.java`: `addEntry(IHazardType, double)` dropped CE's guard that skips adding a `CONTAMINATING` entry when `RadiationConfig.enableContaminationOnGround` is false — a silent, undocumented behavior divergence affecting anything reading `CONTAMINATING` levels directly.
- **minor** — `HazardTransformerRadiationContainer.java`: dropped CE's second `if(!isCrate && !isBox) return;` guard, which in CE made the `isBag` branch permanently dead code. Port now actually applies (doubled) containment radiation to plastic bags — a real, undocumented functional change.
Fix ran, touched `HazardData.java` and `HazardTransformerRadiationContainer.java`.

**Known issues / deviations:**
- Scope-narrowed `HazardSystem`: did **not** port CE's threaded/cached/volatility-protected per-player scan pipeline (`PlayerHazardData`, `CompletableFuture` scan, Guava cache) — replaced with uncached `updatePlayerInventory`/`updateLivingInventory`/`updateDroppedItem`, matching the partial Neo Edition reference's own (also uncached) shape.
- Corrected CE's stale 1.12 `MobEffects` field names (`MINING_FATIGUE`→`DIG_SLOWDOWN`, `SLOWNESS`→`MOVEMENT_SLOWDOWN`, `INSTANT_DAMAGE`→`HARM`).
- `HazardModifierFuelRadiation`: replaced removed `getDurabilityForDisplay` with `stack.getDamageValue()/(double)stack.getMaxDamage()`.
- `HazardTransformerRadiationME` left as a load-detection-only stub — AE2's 1.21 storage API (AEKey/MEStorage) not available to reference; real scraping logic deferred.
- New file `HazardComponents.java` (not in original plan) hosts the two data components.
- Unverified API: `HazardTransformerForgeFluid`'s `Capabilities.FluidHandler.ITEM` usage — no live confirming example found in the reference, flagged for a build-time check.
- `HazardTypeUnstable` assumes `com.hbm.lib.ModDamageSource.nuclearBlast(Level)` will exist with a `Level` parameter (CE had a plain static field) — the lib-area owner must provide this exact method/shape.
- Cross-area contract expectations documented but unconfirmed: `BlockStorageCrate.CRATE_RAD_KEY` as a `DataComponentType<Double>`; `ItemStackUtil.readStacksFromNBT` returning `ItemStack[]`; `ItemRTGPellet.getDurabilityForDisplay(ItemStack)`; `ItemRBMKPellet.rectify(ItemStack)`/`hasXenon(ItemStack)`; `ModItems.reacher`/`containment_box`/`plastic_bag` as `DeferredItem<Item>`.
- `HazardRegistry.registerItems()`/`registerContaminatingDrops()` are intentionally empty no-ops — Phase 1 work (~458 items).
- Not build-verified.

---

## sound

**Files on disk:**
- `src/main/java/com/hbm/sound/ModSounds.java` — **fixed**
- `src/main/java/com/hbm/sound/AudioWrapper.java`
- `src/main/java/com/hbm/sound/AudioWrapperClient.java`
- `src/main/java/com/hbm/sound/AudioWrapperClientStartStop.java` — **fixed**
- `src/main/java/com/hbm/sound/AudioDynamic.java`
- `src/main/java/com/hbm/sound/SoundLoopMachine.java`

**Integration instructions:** In `MainRegistry`'s constructor, add `com.hbm.sound.ModSounds.register(modEventBus);` (same pattern as `ModCreativeTabs.register`). No other wiring — the wrapper/dynamic/loop classes are plain client-side utilities instantiated directly by later phases (e.g. `new AudioWrapperClient(ModSounds.SOME_SOUND.get(), SoundSource.BLOCKS, true)`).

**Review outcome:** `parityOk: true` but findings recorded (fix ran):
- **minor** — `AudioWrapperClientStartStop.java`: constructor now assigns `this.cat = cat;`, silently fixing a latent CE bug (CE's `cat` field was always null there) without documenting it as a deviation.
- **minor** — `ModSounds.java`: CE's `HBMSoundHandler.getOrCreate(ResourceLocation)` (used by `CassetteJsonConfig` to dynamically register runtime SoundEvents) has no equivalent — undocumented design gap; `DeferredRegister` can't add entries after the registry event fires, so a real replacement design is needed later.
Fix ran, touched `AudioWrapperClientStartStop.java` and `ModSounds.java`.

**Known issues / deviations:**
- 220 of 379 sound ids contained uppercase letters (illegal in 1.21 `ResourceLocation`) — lowercased with no other restructuring (e.g. `"alarm.amsSiren"` → `"alarm.amssiren"`, not snake_cased) — must be documented for the future asset-port phase (`sounds.json`/`.ogg` must match exactly).
- CE field names mechanically converted to `UPPER_SNAKE_CASE` (one manual fix: `potatOSRandom` → `POTATOS_RANDOM` instead of the naive `POTAT_OSRANDOM`).
- `AudioDynamic.stop()` renamed to `doneStopWhatever()`, `SoundLoopMachine.stop()` renamed to `requestStop()` — forced by `AbstractTickableSoundInstance` declaring `stop()` as `final` in 1.21.1 (independently confirmed via Neo Edition using the identical rename).
- Dropped CE's `HashBiMap` re-entry guard in `AudioDynamic.start()` (depended on private 1.12 Paulscode-engine internals with no 1.21 equivalent) — matches Neo Edition, which also lacks it. Flagged as a risk if double-play crashes are observed later.
- Only the 6 plan-listed files ported; ~14 other `com.hbm.sound` files (SoundLoopCentrifuge, SoundLoopBroadcaster, MovingSoundBomber, etc.) intentionally not ported — each is bound to not-yet-existing tile-entity/entity/item/packet classes.
- `SoundInstance.Attenuation.LINEAR` used but not independently grep-confirmed in the reference (only `NONE` is) — flagged, high confidence.
- Note: found an unrelated decompiled MC source tree at `C:\Users\Sergo127\AppData\Local\Temp\mc26-src` that is actually MC 26.2 (future/unrelated version) — flagged so no other phase mistakes it for a valid 1.21.1 reference.
- Not build-verified.

---

## config

**Files on disk:**
- `src/main/java/com/hbm/config/ConfigUtil.java`
- `src/main/java/com/hbm/config/GeneralConfig.java`
- `src/main/java/com/hbm/config/BombConfig.java`
- `src/main/java/com/hbm/config/CompatibilityConfig.java`
- `src/main/java/com/hbm/config/MachineConfig.java`
- `src/main/java/com/hbm/config/MobConfig.java`
- `src/main/java/com/hbm/config/PotionConfig.java`
- `src/main/java/com/hbm/config/RadiationConfig.java` — **fixed**
- `src/main/java/com/hbm/config/StructureConfig.java`
- `src/main/java/com/hbm/config/ToolConfig.java`
- `src/main/java/com/hbm/config/WeaponConfig.java`
- `src/main/java/com/hbm/config/WorldConfig.java`
- `src/main/java/com/hbm/config/ClientConfig.java` — **fixed**
- `src/main/java/com/hbm/config/ServerConfig.java`
- `src/main/java/com/hbm/config/VersatileConfig.java`
- `src/main/java/com/hbm/config/HbmConfig.java`

**Integration instructions:** Call `com.hbm.config.HbmConfig.register(modContainer)` exactly once from `MainRegistry`'s constructor `MainRegistry(IEventBus modEventBus, ModContainer modContainer)`, using the `ModContainer` param it already receives. No other wiring — every value is read directly via public static fields/accessors (e.g. `GeneralConfig.ENABLE_GUNS.get()`, `MobConfig.glyphidChance()`, `RadiationConfig.sootFogThreshold()`).

**Review outcome:** `parityOk: true` but findings recorded (fix ran):
- **minor** — `RadiationConfig.java`: `FOG_CHANCE` default set to 20, but CE's actual fresh-install default is 50 (the 20 in CE is only a corrupted-value fallback via `setDef()`).
- **minor** — `ClientConfig.java`: `GUN_ANIMATION_SPEED`'s lower bound set to `Double.MIN_VALUE` (smallest *positive* double, ~4.9E-324) instead of a real floor — a classic Java pitfall that effectively forbids exactly `0.0`, while CE had no bound at all.
Fix ran, touched `RadiationConfig.java` and `ClientConfig.java`.

**Known issues / deviations:**
- Verified every `ModConfigSpec.Builder` method against the real decompiled NeoForge 21.1.228 sources jar; found and fixed a real push/pop nesting bug in `MobConfig` during that verification (a "rampant" subsection under "mobs" was only popped once instead of twice).
- Architecture: static fields + package-private `static void init(Builder)` per class (not the plan's instance-field pattern) to preserve CE's static-field idiom.
- `CompatibilityConfig`: only ported fields NOT keyed by Forge integer dimension IDs; deferred ~60 per-dimension ore/structure/meteor/geyser maps, `dimensionRad`, `peaceDimensions`/`isWarDim`, `fillCraterWithWater` to the world-gen phase (Forge dimension IDs don't exist in 1.21).
- `GeneralConfig`: dropped `trueExp()` (needs `PrecAssRecipes`, out of scope) and GL 3.3 capability gating; dropped `leadSafeForgeContainerWhitelist` entirely (meaningless post-metadata format — string format changed to plain registry-id, flagged for whoever populates it).
- `MachineConfig.doorConf()` returns `Map<String,String>` of raw mode names, not `IDoor.Mode` (not ported yet) — machine/door area must add the `IDoor.Mode.valueOf(...)` conversion.
- `VersatileConfig`: dropped `applyPotionSickness`/`hasPotionSickness` (need `HbmPotion`, out of scope).
- `ClientConfig`/`ServerConfig`: CE's live `/ntmclient`/`/ntmserver` in-game editing commands dropped in favor of standard TOML — a genuine feature reduction, flagged for lead sign-off.
- `ToolConfig`: fixed a CE copy-paste bug (veinminer toggles mislabeled `"11.01_recursionDepth"`) with clearer keys, CE key preserved in comment.
- `CompatibilityConfig`'s ~40 third-party mod-compat entity ids trimmed to a vanilla-only baseline.
- None of the JSON "data" configs (`JsonConfig`, `BedrockOreJsonConfig`, `CassetteJsonConfig`, `CustomMachineConfigJSON`, `FalloutConfigJSON`, `ItemPoolConfigJSON`, `MachineDynConfig`) were touched — inseparable from not-yet-ported blocks/items/recipes/tile-entities.
- Any code reading `CompatibilityConfig.uraniumSpawn`/`.radioStructure`/`.isWarDim(...)` etc. will find no equivalent.
- Not build-verified (though checked against the real sources jar, described as the strongest verification short of compiling).

---

## packet

**Files on disk** (no fix stage ran):
- `src/main/java/com/hbm/packet/HbmNetwork.java`

**Integration instructions:** **No call from MainRegistry.java needed.** `HbmNetwork` is annotated `@EventBusSubscriber(modid = MainRegistry.MODID)`; NeoForge auto-invokes `HbmNetwork.registerPackets(RegisterPayloadHandlersEvent)`. For every future packet: add the record under `com.hbm.packet.toclient`/`toserver` (matching CE's exact path), then add one line inside `HbmNetwork.registerPackets()`: `registrar.playToClient(YourPacket.TYPE, YourPacket.STREAM_CODEC, YourPacket::handleClient)` or the `playToServer` equivalent. `PROTOCOL_VERSION = "1"` (this port's own value, independent of Neo Edition's "3"). No `sendTo`/`sendToAll` helper provided — use `net.neoforged.neoforge.network.PacketDistributor` directly.

**Review outcome:** `parityOk: true`, findings empty. **No fix stage.**

**Known issues / deviations:**
- Open decision (unresolved, needs team call before Phase 2): keep all future `playTo*` registrations centralized in `HbmNetwork.registerPackets()` (CE-style) vs. let each feature package self-subscribe with its own `@EventBusSubscriber` — both compatible with the current empty scaffold.
- The `registrar` local variable is currently unused (benign compiler warning, no `-Werror`/`-Xlint` configured).
- `NBTItemControlPacket` (toserver) mutates a held `ItemStack`'s NBT and **falls under the Data-Component migration hard rule** — a concrete NBT-key→component mapping needs a full survey of every `IItemControlReceiver` implementer before Phase 0's networking successor designs it. `NBTControlPacket` targets tile entities, not ItemStacks, so the component rule doesn't apply to it.
- `PermaSyncPacket`/`PermaSyncHandler` (toclient) cross-cuts 4 unrelated future systems (impact world data, death-potion player set, pollution floats, satellite id map) plus a riding-desync fix, packed in one raw ByteBuf — cannot be split cleanly, must be ported together once `ImpactWorldHandler`, `PollutionHandler`, `HbmPotion`, `SatelliteSavedData` all exist.
- `KeybindPacket`'s CE handler dispatches to `HbmKeybindsServer.onPressedServer` with no main-thread hop (a pre-existing CE quirk); Neo Edition's equivalent adds `context.enqueueWork(...)` — future keybind-porting phase must consciously choose which behavior to keep.
- All 44 concrete toclient/toserver packet files (plus `JetpackSyncPacket`, `KeybindPacket`) are explicitly **not ported** — only the dispatch framework. Full inventory with owning feature domain was captured in the implement result (machines/weapons/satellites/particles-FX/player-UI/sound/world-radiation/recipes/vendor-items) — worth pulling from the raw journal line 49 if a future phase needs the complete file-by-file mapping.

---

## capability

**Files on disk:**
- `src/main/java/com/hbm/capability/ModAttachments.java`
- `src/main/java/com/hbm/capability/HbmPlayerAttachment.java` — **fixed**
- `src/main/java/com/hbm/capability/HbmLivingAttachment.java` — **fixed**
- `src/main/java/com/hbm/capability/ContaminationEffect.java` — **fixed**
- `src/main/java/com/hbm/capability/ModCapabilities.java`
- `src/main/java/com/hbm/capability/NTMFluidCapabilityHandler.java`
- `src/main/java/com/hbm/capability/NTMBatteryEnergyWrapper.java`
- `src/main/java/com/hbm/capability/NTMFluidContainerWrapper.java`
- `src/main/java/com/hbm/capability/NTMCableEnergyCapabilityWrapper.java`
- `src/main/java/com/hbm/capability/NTMEnergyCapabilityWrapper.java`
- `src/main/java/com/hbm/capability/NTMFluidHandlerWrapper.java`

**Integration instructions:** Two calls needed in `MainRegistry` (not edited here):
1. `com.hbm.capability.ModAttachments.register(modEventBus)` — alongside other `DeferredRegister.register(modEventBus)` calls during mod construction.
2. `modEventBus.addListener(com.hbm.capability.ModCapabilities::register)` — a method-reference listener for `RegisterCapabilitiesEvent` (NOT a direct call from the constructor body — the event fires later).
Whoever builds status-effect/input logic on top of the attachments must remember: mutating a fetched `HbmLivingAttachment`/`HbmPlayerAttachment` instance in place does not trigger a client resync — must call `entity.setData(ModAttachments.LIVING_ATTACHMENT, props)` / `player.setData(ModAttachments.PLAYER_ATTACHMENT, data)` with the same instance afterward.

**Review outcome:** `parityOk: false`. Findings:
- **critical** — `HbmPlayerAttachment.java` line 242: `loadNBTData()` calls `CompoundTag.getBoolean/getFloat/getInt` as if primitive-returning, but this project's pinned 1.21.1 `CompoundTag` returns `Optional<Boolean>/Optional<Float>/Optional<Integer>` from those single-arg getters (primitive variants are `getBooleanOr`/`getFloatOr`/`getIntOr(name, default)`) — **file does not compile as written**, blocking `PLAYER_ATTACHMENT` registration entirely.
- **critical** — `HbmLivingAttachment.java` line 240: identical `Optional<T>` vs. primitive misuse (`getDouble`/`getInt`) — blocks `LIVING_ATTACHMENT` registration.
- **critical** — `ContaminationEffect.java` line 49: same Optional-getter misuse, plus `tag.get(key)` replacing CE's null-safe `getCompoundTag(key)` — returns `null` on a missing key (real Mojang API), risking an NPE on corrupted/edited saves where CE degraded gracefully.
- **minor** (parity), line 159 — `HbmLivingAttachment.java`: `getContagion()`/`saveNBTData()` dropped CE's `ServerConfig.ENABLE_MKU` gate that CE applies at the capability-data-class level.
- **minor** (parity), line 230 — `HbmLivingAttachment.java`: `grenadeDeployment` now serialized/network-synced, whereas CE deliberately never serializes it (pure runtime state in CE).
- **minor** (completeness), line 35 — `HbmPlayerAttachment.java`: CE's public `dashCooldownLength = 5` constant dropped with no replacement.
Fix ran, touched `HbmPlayerAttachment.java`, `HbmLivingAttachment.java`, `ContaminationEffect.java` (the 3 files with the critical compile-blocking findings — presumably the 3 minor parity/completeness findings were left unaddressed; the journal only records which files fix touched, not which findings it resolved).

**Known issues / deviations:**
- Everything is compile-blocked on other areas: `IBatteryItem`, `PowerNetMK2`, `IEnergyHandlerMK2`/`IEnergyReceiverMK2`/`IEnergyProviderMK2`, `IFluidProviderMK2`/`IFluidReceiverMK2`/`IFluidUserMK2`, `FluidType`/`Fluids`/`FluidTankNTM`/`FluidContainerRegistry`, `CapabilityContextProvider`, `GeneralConfig`, `HbmKeybinds.EnumKeybind`, `ArmorModHandler`, `ItemModShield` — none exist yet in the target tree.
- `NTMFluidCapabilityHandler.initialize()`/`ModCapabilities.register()` assume `FluidContainerRegistry.allContainers`/`Fluids.getAll()` are populated eagerly at mod-construction time — if the fluid area populates lazily instead, the bulk item-capability registration will be incomplete.
- `NTMFluidContainerWrapper`/`NTMFluidHandlerWrapper` assume `FluidTankNTM` will expose a modern `IFluidTank`-shaped surface (`getFluid()`, `getCapacity()`, `isFluidValid(FluidStack)`, `drain(int, FluidAction)`) — an assumption on a class this area doesn't own.
- Dropped CE's versioned NBT migration ("fmt": "v1") — no legacy CE saves to migrate in this port.
- `GeneralConfig.leadSafeForgeContainerWhitelist` format changed to plain registry-id strings (no metadata).
- `copyOnDeath()` applied only to the player attachment, matching Neo Edition precedent (not independently verified against CE, which has no `PlayerEvent.Clone` handler in this area's 8-file scope to check against).
- Not compiled/build-verified.

---

## base_items

**Files on disk** (no fix stage ran):
- `src/main/java/com/hbm/items/ItemBase.java`
- `src/main/java/com/hbm/items/EffectItem.java`
- `src/main/java/com/hbm/items/ItemEnums.java`
- `src/main/java/com/hbm/items/ItemAmmoEnums.java`
- `src/main/java/com/hbm/items/IAnimatedItem.java`
- `src/main/java/com/hbm/items/ICustomizable.java`
- `src/main/java/com/hbm/items/IEquipReceiver.java`
- `src/main/java/com/hbm/items/IItemControlReceiver.java`
- `src/main/java/com/hbm/items/IKeybindReceiver.java`
- `src/main/java/com/hbm/items/IModelRegister.java`
- `src/main/java/com/hbm/items/ISatChip.java`
- `src/main/java/com/hbm/items/ItemInventory.java`
- `src/main/java/com/hbm/items/BrokenItem.java`
- `src/main/java/com/hbm/items/HbmDataComponents.java`
- `src/main/java/com/hbm/items/ModItems.java`

**Integration instructions:** In `MainRegistry`'s constructor, call:
```
com.hbm.items.ModItems.register(modEventBus);
com.hbm.items.HbmDataComponents.register(modEventBus);
```
Order between the two doesn't matter; both must run during mod construction. No client-setup-time registration needed (data-driven item models replace the old baking system).

**Review outcome:** `parityOk: true`, findings empty. **No fix stage.**

**Known issues / deviations:**
- Added `com.hbm.items.HbmDataComponents.java` (not in original target list) — needed for `ISatChip`/`BrokenItem`; exposes `WRAPPED_ITEM` (`DataComponentType<ItemStack>`, `hbm:wrapped_item`) and `SAT_FREQ` (`DataComponentType<Integer>`, `hbm:sat_freq`).
- `IItemControlReceiver` kept CE's original 2-arg `receiveControl(ItemStack, CompoundTag)` rather than Neo Edition's 3-arg version (deliberate CE-fidelity choice).
- `BrokenItem.make(...)`: kept only `make(ItemStack)`, `make(Item)`, `make(ItemStack, int stackSize)` — dropped CE's two meta-taking overloads (metadata doesn't exist post-flattening). **`BrokenItem` compiles only once `ModItems.BROKEN_ITEM` exists (Phase 1) — that exact field name is required.**
- `ItemAmmoEnums.java` compiles only once Phase 1's `com.hbm.items.weapon.ItemAmmo.AmmoItemTrait` exists.
- Deliberately NOT ported: `ItemBakedBase`, `ItemEnumMulti`, `ItemEnumMultiFood`, `IDynamicModels`, `IDynamicSprites`, `IModelLocationOwner`, `IClaimedModelLocation`, `ClaimedModelLocationRegistry`, `BrokenItem`'s inner model classes — all superseded by 1.21's data-driven item model JSON.
- Two APIs used with only moderate confidence (not grep-confirmed in the Neo Edition reference, flagged for a build-time spot check): `net.neoforged.neoforge.items.ItemStackHandler` (used by `ItemInventory`), and `net.minecraft.world.item.component.ItemContainerContents.copyInto`/`fromItems` + `DataComponents.CONTAINER`.
- CE's 6000-byte gzip-NBT eject-to-world safety valve in `ItemInventory` was dropped, not reimplemented.
- Open design question (not resolved here, explicitly deferred): how CE's ~440 metadata-variant item families (formerly `ItemEnumMulti`/`ItemEnumMultiFood`) become N distinct `DeferredItem<Item>` entries in Phase 1 — needs ratification before `ModItems` gets populated.
- `ModItems` currently has zero entries — ready for Phase 1's ~440 fields.

---

## base_blocks

**Files on disk:**
- `src/main/java/com/hbm/blocks/ModBlocks.java`
- `src/main/java/com/hbm/blocks/BlockBase.java` — **fixed**
- `src/main/java/com/hbm/blocks/BlockFallingBase.java`
- `src/main/java/com/hbm/blocks/BlockDummyable.java`
- `src/main/java/com/hbm/blocks/BlockDummyableMBB.java`
- `src/main/java/com/hbm/blocks/BlockControlPanelType.java`
- `src/main/java/com/hbm/blocks/BlockEnums.java`
- `src/main/java/com/hbm/blocks/PlantEnums.java`
- `src/main/java/com/hbm/blocks/IAnalyzable.java`
- `src/main/java/com/hbm/blocks/IBlockMulti.java` — **fixed**
- `src/main/java/com/hbm/blocks/IOreType.java`
- `src/main/java/com/hbm/blocks/IPersistentInfoProvider.java`
- `src/main/java/com/hbm/blocks/ISpotlight.java`
- `src/main/java/com/hbm/blocks/IStepTickReceiver.java`
- `src/main/java/com/hbm/blocks/ITooltipProvider.java`
- `src/main/java/com/hbm/blocks/ICustomBlockHighlight.java`
- `src/main/java/com/hbm/blocks/ILookOverlay.java` — **fixed**
- `src/main/java/com/hbm/blocks/ModSoundType.java`
- `src/main/java/com/hbm/blocks/ModSoundTypes.java`
- `src/main/java/com/hbm/blocks/OreEnumUtil.java`

**Integration instructions:** Call `com.hbm.blocks.ModBlocks.register(modEventBus)` from the mod constructor (same pattern as `ModCreativeTabs.register`). Currently a no-op (zero registry entries) but should be wired now so Phase 1/2 don't need another `MainRegistry` edit. No client-setup call needed.

**Review outcome:** `parityOk: true` but findings recorded (fix ran):
- **minor** — `BlockBase.java`: ported `appendHoverText` silently drops CE's `meteor_battery`-specific Tesla-coil tooltip line, undocumented (unlike the analogous, documented `gravel_diamond`/`sand_boron` omission in `BlockFallingBase`).
- **minor** — `IBlockMulti.java`: `rectify(int)` changed from CE's `Math.abs(meta % getSubCount())` to `Math.floorMod(index, getSubCount())` — not behaviorally equivalent for negative inputs (e.g. subCount=5, index=-1: CE gives 1, port gives 4), undocumented.
- **minor** — `ILookOverlay.java`: unused import `net.minecraft.client.Minecraft` left in the file.
Fix ran, touched `BlockBase.java`, `IBlockMulti.java`, `ILookOverlay.java`.

**Known issues / deviations:**
- `BlockDummyable`/`BlockDummyableMBB` keep CE's exact single `IntegerProperty META` (0-15) encoding rather than Neo Edition's FACING+TYPE split, per explicit "port faithfully" instruction.
- `IBlockMulti` reduced to `getSubCount()`+`rectify(int)` only (metadata-driven naming defaults dropped).
- `IOreType` declares its own nested `TriFunction` instead of depending on out-of-scope `com.hbm.lib.TriFunction`.
- `OreEnumUtil.OreEnum` (30+ entries) NOT ported — only dependency-free quantity-math helpers kept; ore content is Phase 1 work once `ModItems` exists.
- `IPersistentInfoProvider`'s method renamed `addInformation`→`addPersistentInfo` to avoid an override collision.
- `ModBlocks` intentionally has no convenience block+BlockItem registration helper (would need not-yet-existing `ModItems`).
- Package cannot compile in isolation — hard dependencies on `com.hbm.handler.MultiblockHandlerXR` (expected contract documented: `checkSpace(Level, BlockPos corePos, int[] dims, BlockPos placedPos, Direction dir):boolean` / `fillSpace(...)`), `com.hbm.handler.MultiblockBBHandler`+`MultiblockBounds` (with public `boxes: List<AABB>`), `com.hbm.interfaces.ICopiable`, `com.hbm.tileentity.IPersistentNBT.restoreData(Level, BlockPos, ItemStack)` (flagged package-layout conflict: Neo Edition renamed this to `com.hbm.blockentity` — whoever owns it should reconcile), `com.hbm.world.gen.nbt.INBTBlockTransformable`, `com.hbm.lib.Library.checkForPlayerEyePositions`, `com.hbm.lib.HBMSoundHandler.metalBlock`/`.pipePlaced`, `com.hbm.items.ModItems` fields.
- Not ported at all: `BlockContainerBakeableNormal`, `BlockEnumMeta`, `ICustomBlockItem`, `MaterialGas` — superseded by modern datagen/DataComponent/Properties.
- Not build-verified.

---

## creativetabs

**Files on disk** (no fix stage ran):
- `src/main/java/com/hbm/creativetabs/ModCreativeTabs.java`

**Integration instructions:** In `MainRegistry`'s constructor (already has `IEventBus modEventBus`), add: `com.hbm.creativetabs.ModCreativeTabs.register(modEventBus);` — mirrors Neo Edition's `NuclearTechMod` constructor calling `NtmCreativeTabs.register(eventBus)`. No other wiring needed; the class deliberately avoids referencing not-yet-existing `ModItems`/`ModBlocks` (uses `Items.BARRIER` for all 10 tabs).

**Review outcome:** `parityOk: true`, findings empty. **No fix stage.**

**Known issues / deviations:**
- Consolidated CE's 10 per-tab classes into one `com.hbm.creativetabs.ModCreativeTabs` with `DeferredRegister<CreativeModeTab>`, exposing `Supplier<CreativeModeTab>` fields `PARTS, CONTROL, TEMPLATE, RESOURCE, BLOCKS, MACHINE, NUKE, MISSILE, WEAPON, CONSUMABLE` and `public static void register(IEventBus)`.
- Registry ids (`hbm:<id>`): `tab_parts, tab_control, tab_template, tab_resource, tab_blocks, tab_machine, tab_nuke, tab_missile, tab_weapon, tab_consumable`, ordered via `.withTabsBefore(ResourceLocation)`.
- All 10 tabs currently **empty** (no `displayItems` call) with `Items.BARRIER` icons — each has a code comment naming the real CE icon (e.g. "CE icon: ModItems.ingot_uranium") for Phase 1 to swap in.
- Deliberately not ported in Phase 0: `ControlTab`'s battery full/empty split display logic (needs `IBatteryItem` + battery items), `MissileTab`'s 9 curated `ItemCustomMissile.buildMissile(...)` showcase stacks, `TemplateTab`'s `hasSearchBar()`/`item_search.png`, `NukeTab`'s `nuke.png` background.
- `CreativeModeTab.Builder#backgroundTexture(ResourceLocation)` **is confirmed real** (verified in Neo Edition's `NtmCreativeTabs.java` line 830) — resolved from "open question" to "confirmed, just needs assets"; `TemplateTab`'s `hasSearchBar()` equivalent remains genuinely unresolved.
- Two CE copy/paste bugs found (not reproduced, noted for Phase 1): `BlockTab.createIcon()` null-guards on `ore_uranium` but returns `brick_concrete`; `NukeTab.createIcon()` null-guards on `float_bomb` but returns `nuke_man`.
- No lang file entries added — integration/localization owner must source real English tab titles from CE assets under `itemGroup.hbm.tab_xxx` keys, not invent them.

---

## api_interfaces

**Files on disk** (37 files, one fixed):
- `src/main/java/com/hbm/api/block/{IBlockSideRotation,IBlowable,ICrucibleAcceptor,IDrillInteraction,IExploder,IInsertable,IMiningDrill,IPileNeutronReceiver,IToolable}.java`
- `src/main/java/com/hbm/api/conveyor/{IConveyorBelt,IConveyorItem,IConveyorPackage,IEnterableBlock}.java`
- `src/main/java/com/hbm/api/entity/{EntityGrenadeFactory,IRadarDetectable,IRadarDetectableNT,IResistanceProvider,IThrowable,RadarEntry}.java`
- `src/main/java/com/hbm/api/item/{IDepthRockTool,IDesignatorItem,IGasMask}.java`
- `src/main/java/com/hbm/api/network/IPacketRegisterListener.java`
- `src/main/java/com/hbm/api/ntl/{IPneumaticConnector,ISlotMonitorProvider,SlotMonitor,StackCache}.java`
- `src/main/java/com/hbm/api/recipe/IRecipeRegisterListener.java`
- `src/main/java/com/hbm/api/redstoneoverradio/{IRORInfo,IRORInteractive,IRORValueProvider,RORFunctionException}.java`
- `src/main/java/com/hbm/api/tile/{IHeatSource,ILoadedTile,ILootContainerModifiable,IWorldRenameable}.java` — **ILootContainerModifiable.java fixed**
- `src/main/java/com/hbm/interfaces/{BitMask,IAnimatedDoor,IArmorModDash,IBlockSpecialPlacementAABB,IBomb,IBulletHitBehavior,IBulletHurtBehavior,IBulletImpactBehavior,IBulletRicochetBehavior,IBulletUpdateBehavior,IClimbable,IConstantRenderer,IContainerOpenEventListener,IControlReceiver,ICopiable,ICustomSelectionBox,IDoor,IDummy,IExplosionRay,IGunClickable,IHasCustomModel,IHoldableWeapon,IItemHUD,IKeypadHandler,ILaserable,IMultiBlock,IOrderedEnum,IPostRender,IRadResistantBlock,IRadiationImmune,NotableComments,ServerThread,Spaghetti,ThreadSafeMethod,Untested}.java`

**Integration instructions:** **No MainRegistry.java wiring needed** — everything here is interfaces/annotations/DTOs plus two passive helper classes; nothing registers with NeoForge. One actionable item for other areas: `com.hbm.api.network.IPacketRegisterListener` was redesigned to take a `net.neoforged.neoforge.network.registration.PayloadRegistrar` instead of CE's `int nextId`. Whoever builds the networking area's `RegisterPayloadHandlersEvent` handler should, after registering its own built-in payloads, iterate registered `IPacketRegisterListener` instances and call `registerPackets(registrar)` on each. Also: `ISlotMonitorProvider`/`SlotMonitor`/`StackCache` reference `com.hbm.uninos.networkproviders.PneumaticNetwork` (doesn't exist yet) which must expose an `accessors: Collection<StackCache>` field for these to compile.

**Review outcome:** `parityOk: true` but a finding recorded (fix ran):
- **minor** — `ILootContainerModifiable.java`: `setLootTable(ResourceKey<LootTable>, long seed)` keeps CE's old 2-arg signature verbatim, but the real `net.minecraft.world.RandomizableContainer` it now extends splits this into separate `setLootTable(ResourceKey<LootTable>)` and `setLootTableSeed(long)` — this adds an unrelated overload rather than fulfilling the interface's actual abstract methods; an implementor wiring only the 2-arg method leaves `RandomizableContainer`'s real abstract methods unimplemented/stale.
Fix ran, touched `ILootContainerModifiable.java`.

**Known issues / deviations:**
- Dropped `IFFtoNTMF` and `IMixinFMLProxyPacket` as dead 1.12-only artifacts.
- Confirmed API redesigns: `IPacketRegisterListener` (see above); `IHoldableWeapon`/`IItemHUD` rebuilt on `GuiGraphics`/`RenderGuiLayerEvent.Pre` (Forge's overlay event model is gone — genuine redesign, rendering area should review); `ILootContainerModifiable` now extends `RandomizableContainer`; `IWorldRenameable` now extends `Nameable` (`setCustomName(String)`→`setCustomName(Component)`); `EntityGrenadeFactory` uses `net.minecraft.core.Position`/`Projectile`.
- `IBomb` **deliberately diverges from Neo Edition's own already-shipped IBomb** — kept CE's 3-arg `explode(Level, BlockPos, Entity detonator)` signature since CE is ground truth; flagged for whoever wants a unified IBomb across both codebases.
- `api.ntl` (`SlotMonitor`/`StackCache`/`ISlotMonitorProvider`) identity model changed from `(Item, meta, NBTTagCompound)` to `(Item, DataComponentPatch)`.
- `IGasMask`: javadoc documents required NBT→component mapping for future implementers: CE's `"Filter"` compound → `DataComponentType<ItemStack>` (e.g. `hbm:gas_mask_filter`), CE's `"FilterDamage"` int → `DataComponentType<Integer>` (e.g. `hbm:gas_mask_filter_damage`).
- `RadarEntry`'s wire serialization (`fromBytes`/`toBytes`) intentionally dropped — replacement is a networking-area `StreamCodec<RegistryFriendlyByteBuf, RadarEntry>` concern; DTO fields/constructors preserved.
- Numerous forward-referenced, not-yet-existing cross-area classes (expected under the wave model): `com.hbm.inventory.material.Mats`, `com.hbm.inventory.RecipesCommon`, `com.hbm.lib.ForgeDirection`, `com.hbm.config.MachineConfig`, `com.hbm.util.ArmorRegistry`/`Keypad`, `com.hbm.handler.ClimbableRegistry`, `com.hbm.entity.item.EntityTNTPrimedBase`, `com.hbm.entity.projectile.EntityBulletBase`, `com.hbm.render.misc.RenderScreenOverlay.Crosshair`, `com.hbm.uninos.networkproviders.PneumaticNetwork`.
- Not build-verified.

---

## main_registry_keybinds

**Files on disk:**
- `src/main/java/com/hbm/main/MaterialRegistry.java`
- `src/main/java/com/hbm/main/ServerProxy.java`
- `src/main/java/com/hbm/main/ClientProxy.java`
- `src/main/java/com/hbm/handler/HbmKeybinds.java` — **fixed**

**Integration instructions:** `MainRegistry.java` (human-owned) needs two additions in its constructor `MainRegistry(IEventBus modEventBus, ModContainer modContainer)`:
1. Add field `public static ServerProxy proxy;` and assign: `proxy = FMLLoader.getDist().isClient() ? new ClientProxy() : new ServerProxy();` (confirmed against Neo Edition's `NuclearTechMod` constructor). **The project will not compile without this field** — `HbmKeybinds.handleProps`/`onClientTick` call `MainRegistry.proxy.getIsKeyPressed(key)`.
2. Call `com.hbm.main.MaterialRegistry.register(modEventBus);` from the constructor.
`HbmKeybinds` itself needs no explicit call — `@EventBusSubscriber(modid = MainRegistry.MODID, value = Dist.CLIENT)` auto-registers it. Forward-referenced dependencies for later phases: `com.hbm.items.IKeybindReceiver`, `com.hbm.packet.KeybindPacket` (must be a `CustomPacketPayload` record `KeybindPacket(EnumKeybind key, boolean state)`), `com.hbm.inventory.gui.CalculatorScreen` (a `Screen` subclass with a no-arg constructor), and 20 `hbm:repair/<name>` item tags that need real items added.

**Review outcome:** `parityOk: false`. Finding:
- **major** — `HbmKeybinds.java`: `onClientTick` opens a **brand-new `CalculatorScreen` every single client tick** while `calculatorKey` is held, instead of once per key press (CE used `isPressed()`; Neo Edition guards with `mc.screen != null`).
Fix ran, touched `HbmKeybinds.java` only.

**Known issues / deviations:**
- `MaterialRegistry` ports all 31 CE armor materials and 23 tool materials under their exact CE field names (`enumArmorMaterialT51`, `aMatBJ`, `aMatHaz`, `enumToolMaterialSchrabidium`, `matHF`, etc.), keyed on `Registries.ARMOR_MATERIAL`/`SimpleTier`. Repair items wired through `hbm:repair/<name>` item tags (20 tags) rather than concrete items — materials with no CE repair item get an empty `NO_REPAIR` Ingredient.
- `MaterialRegistry` is **unrelated** to `com.hbm.inventory.material.Mats` (the ~500-entry crafting-material catalog) despite the similar name — flagged as a real naming/scope mismatch inherited from CE itself, ported faithfully.
- CE's `matHS`/`matHF` tool tiers are byte-for-byte duplicates of `matCrucible` (a CE copy-paste artifact) — preserved verbatim, not deduplicated, per "change only what's required."
- CE's reflection-based keybind-overlap hack (`handleOverlap`, using Forge-1.12-internal `MethodHandleHelper`) dropped entirely with no replacement — **a visible, intentional gameplay behavior change worth confirming with a human**: two HBM keybinds sharing a physical key with a vanilla binding will no longer suppress the vanilla action.
- CE never gave `GUN_PRIMARY` its own `KeyMapping` (polled `Mouse.isButtonDown(0)` directly) — the port added a real `gunPrimaryKey` `KeyMapping` bound to left-mouse by default; default behavior unchanged, but it's now a real rebindable controls-menu entry (a deviation, not a bug).
- No owner yet exists for a client-only bootstrap class mirroring Neo Edition's `NuclearTechModClient` (`@Mod(dist=Dist.CLIENT)` + `FMLClientSetupEvent` handling) — flagged as an open gap for whichever area does client setup.
- Armor material texture names carried as lowercased `ResourceLocation` metadata (e.g. `hbm:hbm_blackjack`) — no actual textures exist yet.
- Verified against decompiled Mojang-mapped NeoForge 21.1 sources plus the Neo Edition reference; not build-verified.

---

## damage_types

**Files on disk** (note: one file was **moved/renamed** during the fix pass):
- `src/main/java/com/hbm/damage/ModDamageTypes.java` — **fixed**
- `src/main/java/com/hbm/damage/tags/ModDamageTypeTags.java` — **fixed**
- `src/main/java/com/hbm/damage/datagen/ModDamageTypeTagsProvider.java` — **originally written as `src/main/java/com/hbm/datagen/ModDamageTypeTagsProvider.java`; the fix stage moved it to `com/hbm/damage/datagen/` (package changed `com.hbm.datagen` → `com.hbm.damage.datagen`) and deleted the old file.** Confirmed on disk: `com/hbm/datagen/` no longer exists; the file now lives at `com/hbm/damage/datagen/ModDamageTypeTagsProvider.java` with `package com.hbm.damage.datagen;`.

**Integration instructions:** No `MainRegistry.java` change needed/made. Once the integration/datagen owner creates a `GatherDataEvent` subscriber (Neo Edition's equivalent is `com.hbm.datagen.NtmDataGenerators` — none exists in this port yet):
1. In the `RegistrySetBuilder` for `DatapackBuiltinEntriesProvider`: `builder.add(Registries.DAMAGE_TYPE, com.hbm.damage.ModDamageTypes::bootstrap);`
2. Register the tag provider: `generator.addProvider(event.includeServer(), new com.hbm.damage.datagen.ModDamageTypeTagsProvider(output, lookup, helper));` **— note the class moved packages post-fix; use `com.hbm.damage.datagen.ModDamageTypeTagsProvider`, not the `com.hbm.datagen` path stated in the original implement-stage text.**
3. The client/datagen area must emit `death.attack.<msgId>` and `death.attack.<msgId>.player` lang keys for every msgId in the mapping table (67 total CE damage-source ids, e.g. `NUCLEAR_BLAST`→`hbm:nuclear_blast`/msgId `nuclearBlast`, `LASER`→`hbm:laser`/`laser`, 8 new `SEDNA_*` categories, etc. — full 67-entry table is in the raw journal at result line 55 if needed verbatim).
Any code that needs an actual `DamageSource` must call `level.damageSources().source(ModDamageTypes.XXX, ...)` (2- or 3-arg overload) — **`DamageSource` is `final` in NeoForge 21.1**, so CE's `causeXxxDamage(...)` factories and the two Sedna `DamageSource` subclasses (`DamageSourceSednaNoAttacker`/`WithAttacker`) have zero migration path as classes; that logic must be rewritten by whichever area owns entities/projectiles.

**Review outcome:** `parityOk: true` but findings recorded (fix ran):
- **minor** — `ModDamageTypes.java`: `DEFAULT_EXHAUSTION = 0.1F` copied from Neo Edition rather than vanilla 1.12.2's actual default (0.3F), which CE silently inherits.
- **minor** — `ModDamageTypeTagsProvider.java`: `ModDamageTypeTags.ABSOLUTE` composed from real vanilla `DamageTypeTags.BYPASSES_EFFECTS`/`BYPASSES_RESISTANCE`, which also grants immunity to the vanilla Resistance potion effect — a vanilla mechanic CE's original `isDamageAbsolute` never touched.
- **minor** — `ModDamageTypeTagsProvider.java`: placed in shared `com.hbm.datagen` package (outside this area's own namespace) with no collision-coordination mechanism against other concurrent areas.
Fix ran — result: the file was relocated to `com.hbm.damage.datagen` (addressing the third finding directly; presumably intended to reduce collision risk by moving it into this area's own namespace).

**Known issues / deviations:**
- Ported 67 `ResourceKey<DamageType>` constants total: CE's ~41 `ModDamageSource` static fields (NUCLEAR_BLAST, BLAST, ACID, RADIATION, MKU, NITAN, etc.), plus indirect-factory string ids owed to the entities/projectiles area (REVOLVER_BULLET, TAU, COMBINE_BALL, SUBATOMIC_1..5, LASER, PLASMA, ICE, etc.), plus 8 new `SEDNA_*` generic categories for the Sedna weapon-config `DamageClass` replacement (prefixed to avoid Java-constant clashes with CE-native LASER/MICROWAVE).
- Three custom `TagKey<DamageType>` added with no vanilla equivalent: `ABSOLUTE`, `IS_TAU`, `IS_SUBATOMIC`, plus `IS_ENERGY` (mirrors Neo Edition's own energy-weapon grouping, populated with the 4 generic Sedna energy categories — no CE precedent).
- Ported both `EUTHANIZED_SELF` and `EUTHANIZED_SELF_2` despite Neo Edition commenting both out — flagged as an open question (confirm with mob/entity area whether one is dead CE code) rather than unilaterally dropping one.
- `getIsTau`/`getIsSubatomic` call sites elsewhere in CE must be rewritten to check `source.is(ModDamageTypeTags.IS_TAU)`/`IS_SUBATOMIC)` — out of this area's scope.
- No `GatherDataEvent`/`ModDataGenerators`-equivalent class exists yet — this area was not permitted to create one.
- Not build-verified.

---

## Cross-cutting notes for wiring MainRegistry.java

Areas that need an explicit call added to `MainRegistry`'s constructor `MainRegistry(IEventBus modEventBus, ModContainer modContainer)`:
1. `com.hbm.lib.HBMSoundHandler.register(modEventBus);` (lib_util)
2. `com.hbm.inventory.fluid.Fluids.init();` plus adding a `public static File configHbmDir` field first (fluid)
3. `com.hbm.hazard.HazardComponents.register(modEventBus);` + later, from common setup: `HazardRegistry.registerTrafos()/registerItems()/registerContaminatingDrops()` (hazard)
4. `com.hbm.sound.ModSounds.register(modEventBus);` (sound)
5. `com.hbm.config.HbmConfig.register(modContainer);` (config)
6. `com.hbm.capability.ModAttachments.register(modEventBus);` and `modEventBus.addListener(com.hbm.capability.ModCapabilities::register);` (capability)
7. `com.hbm.items.ModItems.register(modEventBus);` and `com.hbm.items.HbmDataComponents.register(modEventBus);` (base_items)
8. `com.hbm.blocks.ModBlocks.register(modEventBus);` (base_blocks)
9. `com.hbm.creativetabs.ModCreativeTabs.register(modEventBus);` (creativetabs)
10. Add `public static ServerProxy proxy;` field + assignment, and `com.hbm.main.MaterialRegistry.register(modEventBus);` (main_registry_keybinds)
11. Damage types need a future `GatherDataEvent` subscriber (not `MainRegistry` itself) calling `ModDamageTypes::bootstrap` and constructing `com.hbm.damage.datagen.ModDamageTypeTagsProvider` (damage_types)

Areas needing **no** MainRegistry call: material, energy, packet (self-registers via `@EventBusSubscriber`), api_interfaces.

**Compile-blocking gaps that will hit a full Gradle build immediately** (beyond the individually-noted cross-area forward references): the entire `com.hbm.uninos.*` package (NodeNet/GenNode/INetworkProvider/UniNodespace) is referenced by energy and capability but was not part of this Phase 0 wave at all — nothing in this journal shows it being written. Same for `com.hbm.handler.{MultiblockHandlerXR,MultiblockBBHandler,ArmorUtil,ArmorModHandler,ClimbableRegistry,pollution.PollutionHandler,radiation.ChunkRadiationManager}`, `com.hbm.tileentity.IPersistentNBT` (or its Neo-Edition-renamed `com.hbm.blockentity` equivalent — package-layout decision needed), and `com.hbm.world.gen.nbt.INBTBlockTransformable`. None of these are in the 15 Phase 0 areas, so a full build will fail on them until a later wave lands.agentId: a8b15b3db9a6fddfe (use SendMessage with to: 'a8b15b3db9a6fddfe', summary: '<5-10 word recap>' to continue this agent)
<usage>subagent_tokens: 198586
tool_uses: 18
duration_ms: 390980</usage>