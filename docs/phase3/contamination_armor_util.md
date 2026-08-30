# Contamination / armor-hazard utility layer — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/util/ContaminationUtil.java` (677 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/ArmorUtil.java` (373 lines — confirmed the only
  `ArmorUtil` in CE; `com.hbm.util.ArmorUtil` does not exist, no namespace duplication. CE's
  `com.hbm.util.ArmorRegistry` is a separate, already-ported class that `ArmorUtil` itself depends on)
- `upstream/hbm-ce/src/main/java/com/hbm/util/ArmorRegistry.java`, `handler/ArmorModHandler.java`,
  `handler/HazmatRegistry.java`, `api/item/IGasMask.java`, `capability/HbmLivingProps.java` (full)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/gas/{BlockGasAsbestos,BlockGasCoal,BlockGasMonoxide,
  BlockGasRadon,BlockGasRadonDense,BlockGasRadonTomb,BlockGasMeltdown}.java` (full — the 7 gas leaf
  blocks this layer unblocks, to confirm exact call shapes)
- This port's own `src/main/java/com/hbm/{capability/HbmLivingAttachment.java,
  capability/ContaminationEffect.java, capability/ModAttachments.java, capability/HbmPlayerAttachment.java
  (relevant section), hazard/HazardSystem.java, hazard/type/{IHazardType,HazardTypeAsbestos,HazardTypeCoal,
  HazardTypeCold,HazardTypeToxic,HazardTypeRadiation,HazardTypeDigamma}.java, hazard/helper/HazardHelper.java,
  util/ArmorRegistry.java, inventory/fluid/trait/FT_Toxin.java, blocks/generic/{BlockClorine,BarbedWire}.java,
  blocks/gas/{BlockGasBase,GasBlocks}.java, items/tool/{ItemDosimeter,ItemGeigerCounter}.java,
  items/HbmDataComponents.java, items/ItemInventory.java, damage/ModDamageTypes.java,
  packet/HbmNetwork.java, config/{CompatibilityConfig,RadiationConfig,GeneralConfig}.java}` (full or
  targeted, as noted inline below)
- `docs/phase0/{STATUS.md,DIGEST.md,hazard.md}`, `docs/phase1/{items_block_fluid_gas.md,items_tool.md,
  items_special.md,items_food_gear.md,STATUS.md}`, `docs/phase2/{STATUS.md,rbmk_reactor.md}` (structural model)
- Cross-checked against `upstream/neo-edition/src/main/java/com/hbm/extprop/HbmLivingAttachments.java`
  and `items/tools/SwordAbilityItem.java` for confirmed real NeoForge 1.21 `AttributeModifier` API shape
  only (content/behavior taken from CE exclusively, per ground rules)
- Repo-wide `grep` for `ArmorUtil`, `ArmorModHandler`, `ContaminationUtil`, `HbmLivingProps`,
  `HazmatRegistry`, `RadiationConfig.disable`, `GeneralConfig.enable528` across `src/main/java` to
  separate live (compiling-today-would-fail) call sites from comment-only forward references

## Headline finding

This area is **not only** "the blocker for 7 of 10 `blocks/gas` files and 6 of 14 `blocks/fluid`
files," as `docs/phase1/items_block_fluid_gas.md` characterized it (correctly, but only counting
not-yet-ported content). Grepping the live tree turns up something Phase 1's own `STATUS.md` didn't
know when it wrote "`ContaminationUtil`, `ArmorUtil` ... remain correctly absent and are not
half-referenced by any Phase 1 code": that claim is true only of Phase-1-*authored* files. Phase 0's
`hazard/type` package (`HazardTypeAsbestos`, `HazardTypeCoal`, `HazardTypeCold`, `HazardTypeToxic`,
`HazardTypeRadiation`, `HazardTypeDigamma`), Phase 0's `util/ArmorRegistry.java`,
`inventory/fluid/trait/FT_Toxin.java`, and `capability/HbmPlayerAttachment.java` — **9 already-committed
files, none of them gas/fluid content** — already `import` and call these classes' exact CE method
signatures today, live and uncommented (not TODO'd). `docs/phase0/STATUS.md` line 26 already flags
`com.hbm.util.{ArmorRegistry,ArmorUtil,ArmorModHandler}`/`com.hbm.handler.ArmorUtil` as "Phase 3" —
this report confirms that flag was correct and quantifies exactly what depends on it. **The port
does not compile today without this area**, independent of whether any gas/fluid/armor content ever
gets ported — this is a live regression sitting in the tree since Phase 0, not a nice-to-have.

Also found, in the same files, three small, independent, pre-existing compile bugs unrelated to the
missing classes (wrong config-accessor syntax) — not this area's assigned scope, but flagged under
Open questions/risks since whoever implements this area will already have these files open.

## Phase-3-safe scope

Everything below is buildable **right now** against classes that already exist in the port
(`HbmLivingAttachment`, `ArmorRegistry`, `ModDamageTypes`, `ModAttachments`, `CompatibilityConfig`,
`IRadiationImmune`, `HbmDataComponents`'s established pattern). Four classes, in dependency order:

### 1. `com.hbm.capability.HbmLivingProps` (new — CE's exact facade, thin wrapper over `HbmLivingAttachment`)

CE splits player/mob hazard state into two layers: `HbmLivingCapability`/`EntityHbmProps` (raw data)
and `HbmLivingProps` (static facade with game-logic side effects — caps, instadeath, health-modifier
application, packet feedback). **This port already ported layer 1** as `HbmLivingAttachment` (its own
javadoc explicitly says the layer-2 facade "belongs to a later status-effect system phase built on top
of this attachment" — that later phase is this one). Layer 2 was never written, but four
already-committed files (`HazardTypeAsbestos.java:32`, `HazardTypeCoal.java:30`,
`ItemDigammaDiagnostic.java`/`ItemLungDiagnostic.java` javadocs) already reference it **by the exact
CE name** `com.hbm.capability.HbmLivingProps`, not `HbmLivingAttachment` directly — so this report
recommends porting the facade under that name (matching CE 1:1 and every existing forward reference)
rather than retrofitting the two call sites to bypass it. Per CE's `HbmLivingProps.java` (full method
list, adapted to `HbmLivingAttachment`'s already-clamped setters):

| CE method | Port shape | Notes |
|---|---|---|
| `getRadiation`/`setRadiation`/`incrementRadiation` | delegate to `HbmLivingAttachment#getRads/setRads/increaseRads`, gated on `RadiationConfig.ENABLE_CONTAMINATION.get()` | `HbmLivingAttachment` already clamps to `MAX_RADS=2500D`; CE's facade cap was 25,000,000 — the port's tighter existing clamp wins, don't loosen it without a design call |
| `getNeutron`/`setNeutron` | `getNeutrons`/`setNeutrons` | direct passthrough |
| `getRadEnv`/`setRadEnv`, `getRadBuf`/`setRadBuf` | `getRadsEnv`/`setRadsEnv`, `getRadBuf`/`setRadBuf` | direct passthrough |
| `getDigamma`/`setDigamma`/`incrementDigamma` | `getDigamma`/`increaseDigamma`/`decreaseDigamma` + **new** health-attribute-modifier + instadeath logic | see API decision #3 below — this is the one method with real new logic, not a passthrough |
| `getAsbestos`/`setAsbestos`/`incrementAsbestos` | `getAsbestos`/`setAsbestos` + instadeath-at-cap (`entity.hurt(ModDamageTypes.ASBESTOS, 1000)`, reset to 0) + `EntityHbmProps.maxAsbestos` cap check | `HbmLivingAttachment.MAX_ASBESTOS` already exists; see risk note on `MAX_BLACKLUNG` below |
| `getBlackLung`/`setBlackLung`/`incrementBlackLung` | same shape as asbestos, using `ModDamageTypes.BLACKLUNG` | ditto |
| `addCont`, `getCont` | `HbmLivingAttachment#getContaminationEffectList()` | already exists verbatim, `ContaminationEffect` already ported |
| `getContagion`/`setContagion` | `HbmLivingAttachment#getContagion/setContagion` | already exists (already gates on `ServerConfig.ENABLE_MKU`) |
| `getOil`/`setOil` | `HbmLivingAttachment#getOil/setOil` | already exists |
| `getTimer`/`setTimer` | `HbmLivingAttachment#getBombTimer/setBombTimer` | already exists |

Every facade method must call `entity.setData(ModAttachments.LIVING_ATTACHMENT, props)` after
mutating — `HbmLivingAttachment`'s own javadoc warns attachments don't auto-sync on in-place mutation,
and the Neo Edition reference's `HbmLivingAttachments.setDigamma` (read for the API shape) does exactly
this. CE's player-feedback packets (`PlayerInformPacketLegacy` on asbestos/blacklung threshold) are
UI polish, not logic — port as a `player.displayClientMessage(...)` call (matching the pattern
`ItemDosimeter`/`ItemGeigerCounter` already use) rather than inventing a new packet; **no new
`CustomPacketPayload` is needed anywhere in this area** (see API decision #6).

### 2. `com.hbm.handler.ArmorModHandler` (new — direct port, no design changes needed)

Pure NBT-bookkeeping engine for the 9-slot armor-mod system (`helmet_only`/`plate_only`/`legs_only`/
`boots_only`/`servos`/`cladding`/`kevlar`/`extra`/`battery` int constants, `isApplicable`/`applyMod`/
`removeMod`/`clearMods`/`hasMods`/`pryMods`/`pryMod`). Already a **live, uncommented** dependency of
`util/ArmorRegistry.java` (`getProtectionFromItem`'s `ArmorModHandler.hasMods`/`pryMods` calls) and
`capability/HbmPlayerAttachment.java:212-213` (`getEffectiveMaxShield`'s `pryMods`/`kevlar` lookup, which
also references a not-yet-ported `ItemModShield` — that one item class is a small, separate follow-up,
not blocking this class). Storage rewrite: CE's `MOD_COMPOUND_KEY`/`MOD_SLOT_KEY_<n>` nested-NBT-tag
scheme becomes a single `DataComponentType<ItemStack[]>`-shaped store — but rather than inventing a new
component type, **reuse vanilla's `DataComponents.CONTAINER` / `ItemContainerContents`**, exactly the
pattern this port's own `ItemInventory` (`items/ItemInventory.java`) already uses for a fixed-size
`ItemStack` array on a stack (`ItemContainerContents.fromItems(...)` / `.copyInto(NonNullList)`), sized
to the 9 `MOD_SLOTS`. This is a stronger, confirmed-in-tree precedent than the `HbmDataComponents`
custom-component route (see API decision #1) precisely because CONTAINER already round-trips an
`ItemStack` list with no new registration.

### 3. `com.hbm.util.ContaminationUtil` (new — direct port against `HbmLivingProps`/`HbmLivingAttachment`)

Already a **live, uncommented** dependency of `hazard/type/HazardTypeRadiation.java:41`
(`ContaminationUtil.contaminate(target, HazardType.RADIATION, ContaminationType.CREATIVE, ...)`) and
`hazard/type/HazardTypeDigamma.java:21` (`ContaminationUtil.applyDigammaData(...)`). Port the full CE
class against the facade above:

- `HazardType`/`ContaminationType` nested enums — port verbatim (4 and 8 values respectively; both
  already imported by name — `ContaminationUtil.ContaminationType`/`.HazardType` — in the two hazard
  files above, so the enum **must** live nested inside `ContaminationUtil`, matching CE, not as
  top-level types).
- `contaminate(LivingEntity, HazardType, ContaminationType, double)` — the central dispatcher. The
  `FARADAY`/`HAZMAT`/`HAZMAT2`/`DIGAMMA` `ContaminationType` cases call into `ArmorUtil` (item 4 below);
  `DIGAMMA2` is a documented CE no-op (`break;`); the `player.capabilities.isCreativeMode` /
  `isSpectator()` early-outs and the `ticksExisted < 200` grace period port mechanically
  (`Player#isSpectator()`/checking the equivalent creative-mode accessor via `Player#getAbilities().instabuild`
  — confirm the exact 1.21 accessor at implementation time, it is a one-line lookup, not an API-shape risk).
- `calculateRadiationMod`/`getConfigEntityRadResistance`/`checkConfigEntityImmunity`/`isRadImmune` —
  **`CompatibilityConfig` (Phase 0) already has everything `getConfigEntityRadResistance`/
  `checkConfigEntityImmunity` need**: `CompatibilityConfig.mobModRadresistance()`/`mobRadresistance()`
  return `Map<String,Float>` (method calls, not the raw public fields CE used — this port already made
  that change), `mobModRadimmune()`/`mobRadimmune()` return `Set<String>`. Entity registry-key lookup
  (CE: `EntityList.getKey(e)`) becomes `BuiltInRegistries.ENTITY_TYPE.getKey(e.getType())` — confirmed
  vanilla `Registry<T>#getKey`, the same registry object the port's own `ConveyorEntityTypes`/Neo
  Edition's `NtmEntityTypes` already register entity types through. `isRadImmune`'s hardcoded CE class
  array (`EntityCreeperNuclear`, `EntityQuackos`, etc.) mixes vanilla mobs (port now, they exist:
  `Zombie`, `Skeleton`, `Ocelot`, `MushroomCow`, `ZombieHorse`, `SkeletonHorse`, `ArmorStand`) with two
  HBM-custom mobs that don't exist in this port yet (see Deferred scope) — port the array with the
  vanilla classes plus the `IRadiationImmune` interface check (already exists, Phase 0's whole reason
  to define that interface as an extensibility point rather than a hardcoded list) and leave the two
  custom-mob entries as a one-line documented TODO for whichever phase ports them.
- `isRadItem`/`getNeutronRads`/`addNeutronRadInfo`/`neutronActivate{Inventory,Item}`/`isContaminated` —
  the neutron-activation float stored on `NTM_NEUTRON_NBT_KEY`. Port as a new
  `DataComponentType<Float>` (`hbm:neutron_activation`), same shape as `HbmDataComponents.SAT_FREQ`
  (`Codec.FLOAT`/`ByteBufCodecs.FLOAT` in place of `Codec.INT`/`ByteBufCodecs.INT`) — register alongside
  the existing `HbmDataComponents` class rather than a new file, per that class's own stated intent to
  hold exactly this kind of item-stack-state migration.
- `getRads`/`getDigamma`/`getPlayerRads`/`getActualPlayerRads`/`getNoNeutronPlayerRads` — thin
  read-through wrappers, port verbatim against `HbmLivingProps`. **Recommend also rewiring
  `ItemDosimeter#getReceivedRads`/`ItemGeigerCounter` to call these** once they exist, since both
  classes' own javadocs already say they duplicated this exact math "since the hazmat resistance
  lookup was not confirmed to exist in this port yet" — it now does (`calculateRadiationMod`), so this
  is a genuine follow-up cleanup opportunity worth calling out to whoever implements this area, not a
  hard requirement.
- `printGeigerData`/`printDosimeterData`/`printDiagnosticData`/`printLungDiagnosticData` — chat-readout
  helpers. All but `printGeigerData` port mechanically (`Component.translatable`/`.literal` in place of
  `TextComponentTranslation`/`TextComponentString`, matching every hazard-type class's already-established
  idiom). `printGeigerData` alone calls `ChunkRadiationManager.proxy.getRadiation(world, pos)` — **defer
  only that one call** (Phase 4, see Deferred scope) with the chunk-radiation line stubbed to `0`/omitted,
  matching how `RBMKNeutronHandler` already handles the same forward reference per `docs/phase2/STATUS.md`.
- `radiate(...)` (the 5-overload blast-radiation/digamma/fire/blast AoE method) — mechanically portable
  now (it only needs `LivingEntity`, `ModDamageTypes.NUCLEAR_BLAST`/`.BLAST` — both already registered —
  and `contaminate(...)` from this same class), **but its real caller is nuclear-explosion content that
  doesn't exist yet** (`EntityNukeExplosionMK5`, `EntityMIRV`, grenade fillings, etc. — all Phase 3
  weapons/explosion scope, not this report's). Port the method's *body* now since it has no missing
  dependency, but expect it to sit unused until the explosion-content area lands; its
  `isExplosionExempt`/`isFireExempt` entity-class exemption lists reference several not-yet-ported
  projectile/nuke entity classes — port the exemption checks against whichever of those classes already
  exist (currently none) and leave the rest as documented forward references, exactly like every other
  cross-package gap in this port.

### 4. `com.hbm.handler.ArmorUtil` (new — direct port against `ArmorRegistry`/`ArmorModHandler`/`HbmLivingProps`)

Already a **live, uncommented** dependency of `util/ArmorRegistry.java:26/35/52` (`checkArmorNull`),
`hazard/type/HazardTypeAsbestos.java:30`, `HazardTypeCoal.java:33`, `HazardTypeCold.java:29`,
`HazardTypeToxic.java:39/42`, and `inventory/fluid/trait/FT_Toxin.java:70/74`. Confirmed exact
signatures needed by those live call sites (all already compile-correct against these shapes once the
class exists — no signature guessing required):

| Method | Confirmed signature | Called by (already in tree) |
|---|---|---|
| `checkArmorNull` | `(LivingEntity, EquipmentSlot) -> boolean` | `ArmorRegistry` (3 call sites) |
| `damageGasMaskFilter` | `(LivingEntity, int) -> void` | `HazardTypeAsbestos`, `HazardTypeCoal`, `FT_Toxin` |
| `checkForHazmat` | `(LivingEntity) -> boolean` | `HazardTypeCold`, `HazardTypeToxic`, `FT_Toxin` |

Port the rest of the class mechanically against what already exists:
- `checkArmor(LivingEntity, Item, Item, Item, Item)`, `checkArmorPiece`, `checkForHaz2`,
  `checkForHazmatOnly`, `checkForAsbestos`, `checkForDigamma`, `checkForFiend`/`checkForFiend2` — all
  pure `EquipmentSlot`-lookup + `Item` identity checks against `ModItems` fields. **All of these
  reference armor/weapon `ModItems` fields that are themselves Phase 3 content not yet ported**
  (`hazmat_helmet`, `fau_helmet`, `jackt`, `shimmer_sledge`, etc. — see Deferred scope) — the *method
  bodies* port now with zero missing API, but will resolve against not-yet-registered fields until the
  armor-items and weapon-items areas land. This is expected and matches how CE itself structured
  `ArmorUtil` as infrastructure sitting above content that arrives incrementally.
- `checkForDigamma`'s `player.isPotionActive(HbmPotion.stability)` fallback and `checkForHazmat`'s
  `isPotionActive(HbmPotion.mutation)` fallback — `HbmPotion` doesn't exist yet (see Deferred scope);
  port the armor-check branches now and stub the potion fallback as `false` with a documented TODO,
  exactly the "ship the compiling majority, flag the exact missing call" pattern every prior phase used.
- `damageGasMaskFilter(ItemStack, int)`, `installGasMaskFilter`, `removeFilter`, `getGasMaskFilter`,
  `getGasMaskFilterRecursively`, `addGasMaskTooltip` — CE's `FILTERK_KEY` nested-NBT filter storage. Port
  as a **new** `DataComponentType<ItemStack>` (`hbm:gas_mask_filter`), following `HbmDataComponents`'s
  exact `WRAPPED_ITEM` precedent (`.persistent(ItemStack.OPTIONAL_CODEC).networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC)`)
  — register it as a new field on `HbmDataComponents` rather than repurposing `WRAPPED_ITEM` (that one is
  documented as `BrokenItem`'s wrapped-item-plus-meta concept, a different semantic; a dedicated
  component keeps the two concerns separate exactly as CE's two different NBT keys did).
- `isFaradayArmor`/`checkForFaraday`/`metals[]` — string-matching against `item.getTranslationKey()` plus
  `HazmatRegistry.getCladding(item)`. The string-matching half ports with zero blockers; the cladding
  half needs `HazmatRegistry` (see Deferred scope) — call `HazmatRegistry.getCladding` once that lands,
  or `0` in the interim (matching CE's own `f != null ? f : 0` null-safety idiom already, so this is a
  true no-behavior-change stub, not a guess).
- `damageSuit(Player, int, int)` — direct armor-slot durability decrement. **This is CE's real fix for
  the acid-contact armor-degradation TODO already left in `blocks/generic/BarbedWire.java:97-99`**
  (`case ACID -> { ... // CE also calls ArmorUtil.damageSuit(player, slot, 1) for each armor slot here;
  that helper has no port equivalent yet`) — implementing this area closes that already-documented gap;
  flag it to whoever lands this area as a one-line wire-up in an already-committed file, not new scope.
  1.21's item-damage API is `ItemStack#hurtAndBreak(int, ServerLevel, LivingEntity, Consumer<Item>)`
  (confirmed by grep across the port's own tool-durability call sites, e.g. `ItemTooling`/`GearItems`
  durability handling) in place of CE's raw `getItemDamage()`/`setItemDamage()`/manual-empty-on-overflow —
  use that vanilla hook rather than reimplementing CE's manual overflow check.
- `resetFlightTime` — `ServerPlayer#connection.aroundCounter`-equivalent (CE:
  `mp.connection.floatingTickCount = 0`; 1.21's `ServerGamePacketListenerImpl` field is renamed —
  confirm the exact 1.21.1 field name at implementation time by decompiling the mapped connection class,
  not by guessing; this is a one-field rename, low risk, but must be confirmed against real mappings
  since Neo Edition does not have an equivalent call to cross-check against).
- `register()` (the big `ArmorRegistry.registerHazard(...)` wiring block) — **defer this one method**
  until the concrete gas-mask/hazmat/schrabidium/euphemium armor items it references exist (all Phase 3
  armor-items scope, see Deferred scope). The registry engine (`ArmorRegistry`, already ported) and every
  other `ArmorUtil` method work today without it; `register()` is pure data wiring that becomes a
  mechanical fill-in once those items land — do not block the rest of the class on it.

## Deferred scope

Systems this area's *consumers* need, but which belong to other phases/areas and are explicitly **not**
part of porting `ArmorUtil`/`ArmorModHandler`/`ContaminationUtil`/`HbmLivingProps` itself:

- **`HazmatRegistry`** (`com.hbm.handler.HazmatRegistry`) — the per-armor-piece radiation-resistance
  coefficient table. Its engine (`registerHazmat`/`getResistance`/`getCladding`) is small and could be
  ported alongside this area with zero new blockers, but its `initDefault()` populates the table by
  calling `registerHazmat` against ~40 `ModItems` armor fields (`hazmat_helmet`, `steel_helmet`,
  `titanium_helmet`, `cobalt_helmet`, `security_helmet`, `starmetal_helmet`, `schrabidium_helmet`,
  `euphemium_helmet`, `paa_plate`, `jackt`, `gas_mask`, ...) — every one of them is in
  `docs/phase1/items_food_gear.md`'s 15-file "Armor-slot items - defer to Phase 3" list. Recommend
  porting the `HazmatRegistry` *engine* now (unblocks `ArmorUtil.isFaradayArmor`'s cladding lookup
  immediately) but leaving `initDefault()`'s content-wiring calls to land alongside the armor items
  themselves, in whichever Phase 3 work package owns `ModArmor`/`ArmorFSB`/`ArmorGasMask`/etc.
- **The 15 armor-slot items** (`ModArmor`, `ArmorFSB`, `ArmorGasMask`, `ArmorHazmat*`,
  `ArmorSchrabidium`, `ArmorEuphemium`, `ArmorAsbestos`, jetpacks, etc. — full list in
  `docs/phase1/items_food_gear.md`) — a separate Phase 3 "armor items" work package. This report's
  four classes are their load-bearing prerequisite (per that report's own per-file blocker column:
  `ArmorFSB`/`ArmorEuphemium`/`ArmorGasMask` name `ArmorRegistry`/`HazmatRegistry`/`ArmorUtil` by name),
  but porting the armor items themselves is out of scope here. Also needs 1.21's `Equippable`
  data-component/armor-trim system in place of `ItemArmor` subclassing + `ISpecialArmor` — confirm that
  shape against the Neo Edition reference when that area starts, per that report's own note; not
  re-derived here since it's orthogonal to the contamination/hazard logic this report covers.
- **`HbmPotion`** (`com.hbm.potion.HbmPotion`) — does not exist anywhere in the port. Needed for:
  `ContaminationUtil.calculateRadiationMod`'s `mutation` check, `isRadImmune`'s `mutation` check,
  `ArmorUtil.checkForDigamma`'s `stability` check, `ArmorUtil.checkForHazmat`'s `mutation` fallback,
  and (downstream) `BlockGasRadonTomb`/`BlockGasMeltdown`'s `radaway`/`radx`/`radiation` potion
  interactions. None of these are hard blockers for this area's own classes (each is a small,
  independently-stubbable branch, documented per-method above) — `HbmPotion` itself is a `MobEffect`
  registration area with no other dependency on this report's classes, so it can land before, after, or
  in parallel with this area. Not scoped here since it's a registration-only system orthogonal to the
  contamination/armor logic.
- **`com.hbm.handler.radiation.ChunkRadiationManager`** — Phase 4 world/simulation scope per
  `docs/phase0/STATUS.md`. Blocks exactly one method in this area (`ContaminationUtil.printGeigerData`'s
  chunk-ambient-radiation line) and, downstream, `BlockGasMeltdown`'s `incrementRad` call and the RBMK
  neutron handler's already-documented forward references (`docs/phase2/rbmk_reactor.md` headline
  finding #4). Stub the one call in `printGeigerData`, as detailed above.
- **The 7 remaining `blocks/gas` leaf classes** (`BlockGasAsbestos`, `BlockGasCoal`, `BlockGasMonoxide`,
  `BlockGasRadon`, `BlockGasRadonDense`, `BlockGasRadonTomb`, `BlockGasMeltdown`) and **6 of the 14
  `blocks/fluid` classes** (`AcidBlock`, `MudBlock`, `ToxicBlock`, `SchrabidicBlock`, plus the two ArmorUtil-
  referencing corium/volcanic siblings) — this area's original stated purpose per
  `docs/phase1/items_block_fluid_gas.md`. Once this report's 4 classes land, the gas blocks are
  immediately portable (their only other dependencies — `ArmorRegistry`, `ModDamageTypes`,
  `GeneralConfig` flags — already exist; `BlockGasRadonTomb`/`BlockGasRadonDense`/`BlockGasMeltdown`
  additionally need `ModBlocks.waste_earth`/`fallout` and (RadonTomb/Meltdown) `HbmPotion`, both
  separately tracked above). The fluid blocks remain additionally blocked on the world-fluid framework
  (`com.hbm.fluids`, `Fluid`/`FluidType`/`LiquidBlock`) per that same report — this area does not remove
  that second blocker.
- **`radiate(...)`'s real callers** — nuclear/grenade explosion entities (`EntityNukeExplosionMK5`,
  `EntityMIRV`, `EntityGrenadeUniversal`, etc.) are separate Phase 3 weapons/explosion-content scope.
  This report ports `radiate`'s body (no blocker on the method itself) but its practical use starts once
  that content exists.

## Key design/API decisions

Confirmed real NeoForge 1.21.1 shapes found by reading actual usage in this port and/or the Neo Edition
reference — nothing below is invented:

1. **Gas-mask filter storage → dedicated `DataComponentType<ItemStack>`.** Confirmed precedent:
   `HbmDataComponents.WRAPPED_ITEM` (`items/HbmDataComponents.java`) is already exactly this shape —
   `DataComponentType.<ItemStack>builder().persistent(ItemStack.OPTIONAL_CODEC).networkSynchronized(ItemStack.OPTIONAL_STREAM_CODEC).build()`.
   Register a sibling `GAS_MASK_FILTER` field on that same class (it already exists for exactly this
   purpose — "backing item-stack state that CE stored as raw NBT keys").
2. **`ArmorModHandler`'s 9 mod slots → vanilla `DataComponents.CONTAINER`/`ItemContainerContents`, not a
   custom component.** Confirmed precedent: `items/ItemInventory.java` already stores a fixed-size
   `ItemStack` array on a stack this exact way (`ItemContainerContents.fromItems`/`.copyInto`). No new
   component registration needed at all for this one — reuse the vanilla component directly.
3. **Digamma's health-scaling attribute modifier → `AttributeModifier(ResourceLocation, double, Operation)`
   + `AttributeInstance#addPermanentModifier`/`removeModifier(ResourceLocation)`.** CE keyed its
   `AttributeModifier` by a `UUID` (`digamma_UUID`) — 1.20.5+ Minecraft replaced UUID-keyed modifiers
   with `ResourceLocation`-keyed ones. Confirmed against the Neo Edition reference's
   `HbmLivingAttachments#setDigamma` (`extprop/HbmLivingAttachments.java:125-137`), which does exactly
   this against `Attributes.MAX_HEALTH`, using `AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL` for
   CE's numeric op-code `2` (`MULTIPLY_TOTAL` in 1.12.2 terms). Reused here only for the confirmed API
   *shape* — the actual health-modifier formula (`Math.pow(0.5, digamma) - 1D`) and the ≥10 digamma
   instadeath threshold are taken from CE's `HbmLivingProps.setDigamma`, not from Neo Edition's numbers.
4. **Attachment mutation requires an explicit `setData` call to sync.** `HbmLivingAttachment`'s own
   javadoc already states this NeoForge contract; every `HbmLivingProps` facade method that mutates
   state must re-call `entity.setData(ModAttachments.LIVING_ATTACHMENT, props)`, matching the Neo
   Edition reference's identical pattern in the same method.
5. **No new `DamageType`s needed.** Every damage this area's classes throw already has a
   `ModDamageTypes` entry: `RADIATION`, `DIGAMMA`, `ASBESTOS`, `BLACKLUNG`, `MONOXIDE`, `ACID`,
   `MUD_POISONING`, `NUCLEAR_BLAST`, `BLAST` are all already registered and bootstrapped
   (`damage/ModDamageTypes.java`). Build `DamageSource` instances via
   `level.damageSources().source(key)` per that class's own documented contract — no datapack work
   required here.
6. **No new networking payload needed.** All of this area's player-facing feedback (geiger/dosimeter
   readouts, asbestos/blacklung warnings) is server-authoritative chat/system messages
   (`player.sendSystemMessage`/`displayClientMessage`), exactly like `ItemDosimeter`/`ItemGeigerCounter`
   already do — no `CustomPacketPayload` round-trip is involved. State that does need to reach the
   client (rads, digamma, asbestos, filter durability) already syncs via the existing
   `HbmLivingAttachment` attachment `STREAM_CODEC` / the gas-mask-filter/mod-slot `ItemStack`
   components' own `networkSynchronized` codecs — `HbmNetwork.java`'s `RegisterPayloadHandlersEvent`
   registrar does not need a new entry for this area.
7. **Entity registry-key lookup:** `BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())` replaces
   CE's `EntityList.getKey(e)` — confirmed vanilla `Registry<T>#getKey`, the same registry object this
   port's `ConveyorEntityTypes`/`DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ...)` already
   registers entity types through.
8. **Item durability → `ItemStack#hurtAndBreak(...)`**, replacing CE's manual
   `getItemDamage()`/`setItemDamage()`/empty-on-overflow pattern in `ArmorUtil.damageSuit` and
   `damageGasMaskFilter`'s overflow branch — this port's existing tool/gear durability call sites
   already use the vanilla hook; follow that established idiom rather than reimplementing CE's manual
   overflow check by hand.
9. **`CompatibilityConfig` is method-based, not field-based**, unlike CE's public static `Map` fields —
   `CompatibilityConfig.mobModRadresistance()`/`mobRadresistance()`/`mobModRadimmune()`/`mobRadimmune()`
   (already ported, Phase 0) are zero-arg accessor methods returning fresh `Map`/`Set` views each call,
   not cached fields — call them as methods, don't declare local `static final` mirrors expecting field
   semantics.

## Open questions / risks

- **`HbmLivingAttachment.MAX_BLACKLUNG` is very likely a pre-existing constant bug.** CE's
  `HbmLivingProps.maxBlacklung = 2 * 60 * 60 * 20` is double `maxAsbestos = 60 * 60 * 20`, but this
  port's `HbmLivingAttachment.MAX_BLACKLUNG = 60 * 60 * 20` (line 56) — same value as `MAX_ASBESTOS`,
  missing the `2×` factor. Since the facade this report designs (`HbmLivingProps.setBlackLung`'s
  instadeath-at-cap check) reads this constant directly, whoever implements this area should either fix
  the constant to match CE (recommended — it's a one-line, low-risk, behavior-restoring fix in an
  already-committed Phase 0 file) or explicitly document the divergence if intentional. Flagging rather
  than silently fixing since it's outside this report's own file set.
- **Three independent, pre-existing compile bugs found in the same already-committed `hazard/type`
  files this area must wire into** — not part of this report's scope to fix, but the implementer will
  have these files open regardless:
  - `RadiationConfig.disableAsbestos`/`.disableCoal`/`.disableCold`/`.disableToxic` are referenced as
    plain fields in `HazardTypeAsbestos.java:27`, `HazardTypeCoal.java:27`, `HazardTypeCold.java:27`,
    `HazardTypeToxic.java:29` — but `RadiationConfig` only declares `DISABLE_ASBESTOS`/`DISABLE_COAL`/
    `DISABLE_COLD`/`DISABLE_TOXIC` as `ModConfigSpec.BooleanValue` fields (needing `.get()`). These four
    call sites will not compile as written.
  - `GeneralConfig.enable528` is referenced as a plain field (no parens) in
    `HazardTypeRadiation.java:35` and `HazardHelper.java:14` — but `GeneralConfig` only exposes it as a
    zero-arg method `enable528()` (confirmed: `config/VersatileConfig.java` calls it correctly with
    parens elsewhere). These two call sites will not compile as written.
  - Both bug classes are simple accessor-syntax fixes (add `.get()` / add `()`), not design questions —
    listed here so they aren't mistaken for new findings by whoever reviews this area's implementation.
- **`HbmLivingProps` vs. calling `HbmLivingAttachment` directly — naming decision already made by
  existing code, not open.** Listed as a "decision" above rather than a true open question because
  `HazardTypeCoal`/`HazardTypeAsbestos` already import `com.hbm.capability.HbmLivingProps` by that exact
  name; the only way to avoid porting that facade class would be editing those two already-committed
  files, which is a larger, riskier change than adding one thin new class. Recommend against relitigating
  this unless the implementer finds a concrete reason the facade layer is actively harmful.
- **`ContaminationUtil.contaminate`'s creative-mode/spectator/warm-up gating** depends on the exact 1.21
  `Player` capability-check accessor (CE: `player.capabilities.isCreativeMode`). This is a single,
  well-known vanilla accessor (`Player#isCreative()` exists directly in 1.21 as a convenience method) —
  low risk, but confirm the exact method name against real Mojang mappings at implementation time rather
  than assuming the exact 1.12.2-era accessor name carries over.
- **`ArmorUtil.resetFlightTime`'s `ServerGamePacketListenerImpl` field rename** (CE:
  `mp.connection.floatingTickCount`) has no cross-checkable precedent in the Neo Edition reference (grep
  found no equivalent call there) — this is the one method in this area whose exact 1.21.1 field name is
  not independently confirmed by a second source. Low-risk (single-field rename, easily verified against
  real mappings when the build toolchain is reachable), but flagged as the one true unknown rather than
  guessed.
- **Scope boundary between this report and the eventual "armor items" Phase 3 area.** This report
  deliberately stops at the four utility classes and does not touch `ModArmor`/`ArmorFSB`/etc. (15 files,
  `docs/phase1/items_food_gear.md`) or `HazmatRegistry.initDefault()`'s content wiring. Whoever schedules
  Phase 3 work should sequence this report's 4 classes first (small, self-contained, unblocks 9
  already-broken files immediately) before either the armor-items area or the remaining 7 gas / 6 fluid
  content classes, matching the dependency direction confirmed throughout this report.
