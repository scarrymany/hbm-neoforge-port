# Armor / equippable framework — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/gear/{ModArmor,ArmorModel,ArmorAsbestos,
  ArmorEuphemium,ArmorSchrabidium,ArmorFSB,ArmorGasMask,MaskOfInfamy}.java` (8 files — CE's real
  generic armor bases and its only 3 `ISpecialArmor` users live here, in `items.gear`, **not** in
  `items.armor` as the task's own path guess assumed; confirmed by directory listing before reading)
- `upstream/hbm-ce/src/main/java/com/hbm/items/armor/{IDamageHandler,IAttackHandler,
  IArmorDisableModel,IPAMelee,IPARanged,IPAWeaponsProvider,ItemArmorMod,ArmorFSBPowered,ArmorHEV,
  ArmorNCRPA,ArmorNCRPARanged,ArmorNCRPAMelee,ArmorNo9,JetpackBase,JetpackFueledBase}.java` (15 of
  70 files in this package, ~1,250 of its ~5,748 lines — the interfaces in full, plus one file at
  each level of the `ArmorFSB → ArmorFSBPowered → leaf` chain and one full example of the
  power-armor-supplies-weapons pattern; the remaining ~55 files (`Armor{AJR,AJRO,Bismuth,DNT,
  Desh,Diesel,Digamma,Envsuit,FSBFueled,Liquidator,RPA,RPAMelee,T51,Taurun,Trenchmaster,
  AshGlasses,BJ,BJJetpack,Hat},WingsMurk` and ~35 `ItemMod*` armor-insert items) were sized and
  superclass-checked by directory listing + a `grep -m1 "^public.*class"` pass over every file, not
  read line-by-line — see Phase-3-safe scope for what that pass established)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/{ArmorUtil,ArmorModHandler,HazmatRegistry}.java`,
  `.../util/ArmorRegistry.java`, `.../api/item/IGasMask.java` (5 files, full — the actual
  cross-cutting registries this framework is built on)
- `upstream/hbm-ce/src/main/java/com/hbm/main/ModEventHandler.java` (grepped for `IDamageHandler|
  IAttackHandler|LivingHurtEvent|LivingAttackEvent|ISpecialArmor|ArmorProperties`, then read in full
  around both hit methods, ~95 lines — this is the actual central damage-dispatch call site)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerArmorTable.java` (full, 243
  lines) plus a listing confirming `BlockArmorTable`/`GUIArmorTable` exist alongside it
- This port's own (all read in full): `src/main/java/com/hbm/main/MaterialRegistry.java`,
  `src/main/java/com/hbm/util/{ArmorRegistry,TagsUtil}.java`, `src/main/java/com/hbm/api/item/
  IGasMask.java`, `src/main/java/com/hbm/hazard/type/{HazardTypeBlinding,HazardTypeToxic,
  HazardTypeCoal,HazardTypeAsbestos}.java`, `src/main/java/com/hbm/packet/HbmNetwork.java`,
  `src/main/java/com/hbm/inventory/container/MenuBase.java` (header + class-bound section),
  `src/main/java/com/hbm/items/ModItems.java` (header), plus directory listings of
  `src/main/java/com/hbm/{items/gear,capability,hazard,api}` and `docs/phase0/STATUS.md`
- `upstream/neo-edition/src/main/java/com/hbm/{util/DamageResistanceHandler.java,items/armor/
  {ArmorNo9,ItemArmorMod}.java,handler/ArmorModHandler.java}` (4 files, full — **cross-referenced for
  confirmed real NeoForge 1.21.1 API shape only**, per this task's ground rules; every behavioral
  claim below is sourced from CE, never from Neo Edition)
- `docs/phase1/items_food_gear.md` (full — the report that first named the 15-file deferred-to-Phase-3
  bucket this task points at), `docs/phase0/{STATUS.md,hazard.md,api_interfaces.md}` (full),
  `docs/phase3/gun_framework.md` (skimmed for its own cross-reference to this package's classes)

## Headline finding

PORT_SPEC's framing ("armor/equippable framework using `ISpecialArmor`'s 1.21 replacement") needs
two corrections before the design makes sense, in the same way every other Phase 2/3 report in this
repo has had to correct its own starting premise:

1. **`ISpecialArmor` is almost irrelevant to CE's actual armor damage logic.** A repo-wide grep found
   exactly **3** CE classes implementing it: `ArmorAsbestos`, `ArmorEuphemium`, `ArmorSchrabidium` —
   all three are "ignore fire entirely" / "ignore almost everything" full-immunity edge cases, not
   graduated damage-reduction curves. The real, load-bearing mechanism CE uses for per-piece damage
   reduction is a pair of hand-rolled marker interfaces, `IDamageHandler`/`IAttackHandler`
   (`com.hbm.items.armor`), dispatched from **one** central pair of `@SubscribeEvent` methods in
   `ModEventHandler` (`onEntityHurt(LivingHurtEvent)` / `onEntityAttacked(LivingAttackEvent)`) that
   iterate the player's 4 armor slots and call `((IDamageHandler)stack.getItem()).handleDamage(event,
   stack)` on whichever pieces implement it — alongside `ArmorFSB.handleHurt`/`handleAttack` (full-set
   sound/potion base) and `ItemArmorMod.modDamage` (armor-insert-chip hook). This is *exactly* the
   "`LivingHurtEvent`-style listener" the task's own framing anticipated as the likely real answer,
   not `ISpecialArmor` — confirmed concretely by `ArmorNo9.handleDamage`, which just does
   `event.setAmount(event.getAmount() - 0.5F)` (a flat damage-threshold reduction), the same shape
   dozens of pieces across the mod use.
2. **The 1.21.1 replacement API is not a guess this report has to make — it is already confirmed by
   two independent pieces of code already sitting in this repository.** First, this port's own
   `com.hbm.main.MaterialRegistry` (already on disk, pre-dating this research pass) has **already
   registered all ~28 of CE's armor materials** as real `Holder<ArmorMaterial>` entries via
   `DeferredRegister<ArmorMaterial>` against `Registries.ARMOR_MATERIAL`, using the real 7-argument
   `ArmorMaterial` record constructor. Second, Neo Edition's own working `ArmorNo9`/
   `DamageResistanceHandler`/`ArmorModHandler` classes exercise the real `ArmorItem` base, the real
   client-model hook, and the real damage events end-to-end. Section "Key design/API decisions" below
   cites both directly — nothing in this report's API shapes is invented.
3. **The literal 15-file list `docs/phase1/items_food_gear.md` deferred to Phase 3 undercounts the
   real dependency surface by roughly 4x.** Those 15 files (all in `com.hbm.items.gear`) are the
   *generic bases plus the jetpack chestplates*; the ~55 remaining files in the sibling
   `com.hbm.items.armor` package (25 concrete leaf armor-piece classes built on top of `ArmorFSB`,
   6 tiny interfaces, and ~35 `ItemMod*` armor-insert/"chip" items) were never in that report's scope
   at all (it only surveyed `items/food` and `items/gear`) but are every bit as blocked on this same
   framework, and are explicitly named as such by this port's own `docs/phase0/STATUS.md`
   ("`com.hbm.util.{ArmorRegistry,ArmorUtil,ArmorModHandler}` — Phase 3 armor/FSB modifier system")
   and `docs/phase3/gun_framework.md` ("`ArmorFSB`/`ArmorNCRPARanged`/`ArmorNCRPAMelee`/
   `ArmorRPAMelee`/`IPARanged`/`IPAMelee`... power-armor/force-shield-belt integration... Not touched
   by this report"). This report treats the *whole* `items.gear` armor slice + `items.armor` package
   + its three supporting registries as one prerequisite, since none of it is separable in practice —
   every concrete armor set (leaf classes in both packages) extends the same `ArmorFSB` chain.
4. **Two of this framework's three supporting registries are already ported.** `com.hbm.util.
   ArmorRegistry` (hazard-protection lookup) and `com.hbm.api.item.IGasMask` both already exist in
   this repo, already consumed by four already-shipped `HazardType*` classes
   (`HazardTypeBlinding/Toxic/Coal/Asbestos`). `ArmorRegistry` forward-references two classes that do
   **not** exist yet — `com.hbm.handler.ArmorUtil` and `com.hbm.handler.ArmorModHandler` — and those
   two, not `ArmorRegistry` itself, are the actual missing prerequisite this report is about.

## Phase-3-safe scope

**CE file/class inventory (all confirmed real, counted by directory listing + `grep -m1 "^public.*
class"` superclass check over the full `items.armor` package, plus full reads of the 15 `items.gear`
files and 15 representative `items.armor` files):**

| Bucket | Files | Notes |
|---|---|---|
| `com.hbm.items.gear` generic armor bases | `ModArmor`, `ArmorModel` | Plain `ItemArmor` subclasses; texture/model dispatch by `==` comparison against `ModItems` fields. No damage logic at all. |
| `com.hbm.items.gear` `ISpecialArmor` users | `ArmorAsbestos`, `ArmorEuphemium`, `ArmorSchrabidium` | The only 3 CE classes implementing `ISpecialArmor` in the whole mod. All full/near-full immunity (fire-only, everything-but-digamma, everything). |
| `com.hbm.items.gear` FSB core | `ArmorFSB` (540 lines) | The fluent-builder full-set-bonus base every non-trivial armor set in the mod extends, directly or via `ArmorFSBPowered`/`ArmorFSBFueled`. |
| `com.hbm.items.gear` gas-mask family | `ArmorGasMask`, `ArmorHazmat`, `ArmorHazmatMask` | Build on `IGasMask` (already ported) + `ArmorRegistry.HazardClass` (already ported). |
| `com.hbm.items.gear` misc | `MaskOfInfamy` | Trivial cosmetic `ItemArmor`. |
| `com.hbm.items.gear` jetpacks (chestplate-slot) | `JetpackBooster`, `JetpackBreak`, `JetpackGlider`, `JetpackRegular`, `JetpackVectorized` | Build on `ItemArmorMod`/`JetpackFueledBase` (`items.armor`) + `IFillableItem` (already-ported Phase 0 api) for the fuel-tank variant (`JetpackGlider`). |
| `com.hbm.items.armor` power-branch | `ArmorFSBPowered` (+`IBatteryItem`), `ArmorFSBFueled` (+`IFillableItem`) | Battery-charge / fuel-tank gating layered on `ArmorFSB`; `isArmorEnabled()` disables the *entire set's* bonuses when discharged — a real CE mechanic distinct from vanilla durability. |
| `com.hbm.items.armor` concrete leaves (25 classes) | `ArmorAJR`, `ArmorAJRO`, `ArmorAshGlasses`, `ArmorBJ`(→`ArmorBJJetpack`), `ArmorBismuth`, `ArmorDNT`, `ArmorDesh`, `ArmorDiesel`, `ArmorDigamma`, `ArmorEnvsuit`, `ArmorHEV`, `ArmorHat`, `ArmorLiquidator`, `ArmorNCRPA`(+`IPAWeaponsProvider`), `ArmorNo9`(+`IDamageHandler`+`IAttackHandler`), `ArmorRPA`(+`IPAWeaponsProvider`), `ArmorT51`, `ArmorTaurun`, `ArmorTrenchmaster`, `WingsMurk` | All extend `ArmorFSB`/`ArmorFSBPowered`/`ArmorFSBFueled`/`ArmorModel` directly; each adds only its own tick/render/tooltip override once the base chain exists. `ArmorNCRPAMelee`/`ArmorNCRPARanged` are the two `IPAMelee`/`IPARanged` companions to `ArmorNCRPA` — see Deferred scope. |
| `com.hbm.items.armor` interfaces (6) | `IDamageHandler`, `IAttackHandler`, `IArmorDisableModel`, `IPAMelee`, `IPARanged`, `IPAWeaponsProvider` | Tiny, version-agnostic contracts; only their event-type parameters need remapping (see Key decisions). |
| `com.hbm.items.armor` insert base | `ItemArmorMod` | Base class for every "chip" slotted into `ArmorModHandler`'s 9 mod slots. |
| `com.hbm.items.armor` insert leaves (~35) | `ItemMod{Battery,Cladding,Servos,Insert,Card,Charm,...}` | Mostly self-contained data/tooltip items (battery multiplier, rad-cladding value, etc.) — see Open questions #5 for the handful with their own cross-package blockers. |
| Supporting registries | `com.hbm.handler.ArmorUtil`, `com.hbm.handler.ArmorModHandler`, `com.hbm.handler.HazmatRegistry` | **The actual missing prerequisite** — `com.hbm.util.ArmorRegistry` (already ported) forward-references the first two by name; neither exists in this port yet. `HazmatRegistry` (JSON-configurable per-item radiation resistance, additive across worn slots) has no port-side presence either. |
| `ModEventHandler` dispatch | `onEntityHurt`/`onEntityAttacked` (~95 lines) | The central `LivingHurtEvent`/`LivingAttackEvent` listener every `IDamageHandler`/`IAttackHandler`/`ArmorFSB.handleHurt`/`ItemArmorMod.modDamage` piece is invoked from. Its shield-absorb step already maps onto already-ported `HbmPlayerAttachment.getShield/setShield`. |
| Already done, zero work needed | `com.hbm.main.MaterialRegistry` (~28 `Holder<ArmorMaterial>` constants) | Every CE armor material CE's `EnumHelper.addArmorMaterial` calls produce is already registered with the real 1.21.1 API. This package should *consume* these constants, not redesign them. |

Everything in this table is Phase-3-safe **today** in the sense that nothing outside this package's
own scope blocks it — every cross-reference resolves either to an already-ported Phase 0/1/2 piece
(`ArmorRegistry`, `IGasMask`, `IFillableItem`, `TagsUtil`, `HbmPlayerAttachment`, `MaterialRegistry`,
`MenuBase`/`GuiInfoContainer`) or to a named forward reference this report explicitly defers (see
below) rather than silently assumes away.

## Deferred scope

- **`ArmorNCRPA`/`ArmorRPA`'s built-in-weapon firing logic** (`ArmorNCRPAMelee`, `ArmorNCRPARanged`,
  `ArmorRPAMelee`, `IPAMelee`, `IPARanged`, `IPAWeaponsProvider`) directly imports
  `com.hbm.items.weapon.sedna.{ItemGunBaseNT,BulletConfig,MagazineBelt}` and
  `com.hbm.entity.projectile.EntityBulletBaseMK4` — the gun framework, already surveyed and deferred
  in `docs/phase3/gun_framework.md`, which itself names this exact armor-integration slice as
  out of its own scope. **The armor-item shell can be built now** (`ArmorNCRPA extends
  ArmorFSBPowered implements IItemRendererProvider, IPAWeaponsProvider`, with
  `getMeleeComponent`/`getRangedComponent` wired to return the companion objects), but the companion
  objects' `clickPrimary`/`clickSecondary` bodies cannot land until the gun framework lands. Sequence
  this package before or alongside `gun_framework.md`'s package, not after.
- **All client-side rendering**: every leaf's custom `ModelBiped`/`Model` (`ModelArmorHEV`,
  `ModelArmorNCRPA`, `ModelNo9`, `ModelGasMask`, `ModelM65`, `ModelCloak`, `ModelHat`, `ModelJetPack`,
  ...), `IItemRendererProvider`'s first-person/inventory render hooks, and every helmet-overlay-blur
  GL-immediate-mode routine (goggle/gasmask/asbestos overlays) belong to Phase 5 (Client & UX) per
  this port's own phase boundary. This package should expose the *hook point*
  (`Item.initializeClient(Consumer<IClientItemExtensions>)`, confirmed real — see Key decisions) on
  the base classes, and leaf classes should have a well-defined `protected` extension point for it,
  but must not ship placeholder/guessed `Model` subclasses.
- **`ItemPancake`** (named in `docs/phase1/items_food_gear.md` as FSB-battery coupling) unblocks the
  moment `ArmorFSBPowered`/`ArmorFSB.hasFSBArmorIgnoreCharge` exist; it is Phase 1 content that
  should be picked back up as a follow-up once this package lands, not owned by this package.
- **`ChunkRadiationManager`** (Phase 4, world/simulation): `ArmorFSB.onArmorTick`'s geiger-sound
  branch calls `ChunkRadiationManager.proxy.getRadiation(world, pos)` (via the static
  `ArmorFSB.check(World, BlockPos)` helper) and `ContaminationUtil.getActualPlayerRads`. Both must be
  stubbed with an explicit tracked TODO (not silently invented or silently dropped) until Phase 4
  lands `ChunkRadiationManager`; the rest of `ArmorFSB`'s mechanics (potion list, step/jump/fall
  sounds, hazard/rad-resist registration, body-part hiding) have no such dependency and are fully
  portable now.
- **`HbmPotion`** (the unported custom `MobEffect` registry first flagged as a blocker in
  `docs/phase1/items_food_gear.md` finding #2): `ArmorUtil.checkForHazmat`/`checkForDigamma` each end
  with a `player.isPotionActive(HbmPotion.mutation)` / `(HbmPotion.stability)` fallback check. These
  two specific branches must return `false` with a tracked TODO until `HbmPotion` lands — no doc read
  during this research pass assigns `HbmPotion` an owning phase; that gap should be resolved by
  whoever plans Phase 4 (radiation/hazard runtime), since multiple Phase 3/4 packages now depend on
  it (see Open questions #4).
- **Jetpack particle trails / sounds** (`ParticleJetpackTrail`, `MovingSoundJetpack`,
  `JetpackSyncPacket`): need Phase 2's `AuxParticlePacketNT`/`HbmEffectNT` particle system, per
  `docs/phase1/items_food_gear.md`'s own note on the jetpack items. Confirm that system's Phase 2
  landing state before wiring; if not yet present, stub with a tracked TODO rather than guessing a
  particle API.
- **The Armor Table GUI's block-entity mismatch** (see Open questions #1) is a design decision this
  report flags but does not resolve unilaterally — building the GUI is Phase-3-safe in principle
  (nothing outside this package blocks it) but the *shape* of that build depends on a call this
  report isn't positioned to make alone.
- **A handful of individual `ItemMod*` insert leaves** (e.g. `ItemModRadar`, which likely couples to
  the `IRadarDetectableNT` radar system) were not read in this pass and may carry their own
  cross-package blockers the way CE's tool/weapon items did in `docs/phase1/items_food_gear.md` —
  flagged for a follow-up survey rather than asserted safe or unsafe here (see Open questions #5).

## Key design/API decisions

**1. Base class hierarchy** (mirrors CE's real inheritance shape 1:1, `ItemArmor` → `ArmorItem`):

```
ArmorItem (vanilla: Holder<ArmorMaterial>, ArmorItem.Type, Item.Properties)
├─ ModArmor            texture-by-item-identity dispatch only, no damage logic
├─ ArmorModel          custom Model hook (goggles/masks/capes/hat) + overlay + decay tick
├─ ArmorGasMask / ArmorHazmat / ArmorHazmatMask     implements IGasMask (already ported)
├─ ArmorAsbestos / ArmorEuphemium / ArmorSchrabidium   the 3 near-total-immunity pieces —
│                                                        dispatched via LivingIncomingDamageEvent
│                                                        full-cancel, NOT a ported ISpecialArmor
│                                                        (that Forge type has no NeoForge analog)
├─ MaskOfInfamy         trivial cosmetic
└─ ArmorFSB             full-set-bonus base: potion-effect list, step/jump/fall sounds, geiger tick,
   │                    VATS/thermal-sight flags, hazard-class + rad-resist self-registration,
   │                    body-part hiding (IArmorDisableModel), handleAttack/handleHurt dispatch hooks
   ├─ (13 direct leaves: ArmorBismuth, ArmorTaurun, ArmorTrenchmaster, ArmorAJR(O), ArmorHat←ArmorModel, ...)
   ├─ ArmorFSBPowered   + IBatteryItem (battery charge component); isArmorEnabled() gates the WHOLE
   │  │                 set's bonuses on charge > 0 — real CE mechanic, distinct from durability
   │  ├─ ArmorHEV, ArmorAJR, ArmorAJRO, ArmorBJ(→ArmorBJJetpack), ArmorDigamma, ArmorDNT, ArmorEnvsuit,
   │  │  ArmorT51, ArmorLiquidator, ArmorNCRPA(+IPAWeaponsProvider), ArmorRPA(+IPAWeaponsProvider)
   └─ ArmorFSBFueled    + IFillableItem (fuel-tank-in-stack; api already ported)
      └─ ArmorDesh, ArmorDiesel
```

`IDamageHandler`/`IAttackHandler` are **orthogonal** to this tree, not a branch of it — any class
(FSB-based or not, e.g. `ArmorNo9 extends ArmorModel`) can additionally implement them, and
`ModEventHandler`'s dispatch loop finds them by `instanceof`, not by inheritance position.

**2. Damage-reduction hook — two real NeoForge events, not one, confirmed by Neo Edition's own
working code, never guessed:**
- `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent` (confirmed real, used by
  Neo Edition's `DamageResistanceHandler.onEntityAttacked`) fires pre-defense-calc and is fully
  cancelable — this is the correct replacement for `ISpecialArmor.getProperties`'s "override the
  entire damage pipeline" role. Port `ArmorAsbestos`/`ArmorEuphemium`/`ArmorSchrabidium`'s immunity
  logic onto this event (full `event.setCanceled(true)` for the fire-only/near-total cases).
- `net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre` (confirmed real, used by Neo
  Edition's `ItemArmorMod.modDamage(LivingDamageEvent.Pre event, ItemStack armor)`) fires just before
  HP is reduced — this is the correct replacement for `IDamageHandler.handleDamage`/`ItemArmorMod.
  modDamage`/`ArmorFSB.handleHurt`'s "adjust the final amount" role (`event.getAmount()`/
  `setAmount(...)` still work the same way CE's `LivingHurtEvent` did).
- `LivingAttackEvent` still exists unchanged in NeoForge 1.21 (confirmed — CE's own
  `onEntityAttacked` name survives verbatim in the vanilla/NeoForge event hierarchy); keep
  `IAttackHandler`/`ArmorFSB.handleAttack` dispatched from it exactly as CE does.
- Rebuild `ModEventHandler`'s exact order as two listeners: shield-absorb (already-ported
  `HbmPlayerAttachment.getShield/setShield`) + Euphemium full-cancel on `LivingIncomingDamageEvent`;
  armor-mod `modDamage` iteration (slots 2–5, i.e. chest/legs/boots/head per CE's own
  `EnumUtil.ENTITY_EQUIPMENT_SLOTS` indexing) + `ArmorFSB.handleHurt` + `IDamageHandler` iteration on
  `LivingDamageEvent.Pre`.

**3. `ArmorMaterial`: zero new work.** `com.hbm.main.MaterialRegistry` already registers every CE
armor material via `DeferredRegister<ArmorMaterial>` against `Registries.ARMOR_MATERIAL`, using the
confirmed real constructor `new ArmorMaterial(Map<ArmorItem.Type,Integer> defenseByType, int
enchantmentValue, Holder<SoundEvent> equipSound, Supplier<Ingredient> repairIngredient,
List<ArmorMaterial.Layer> layers, float toughness, float knockbackResistance)`. This package should
import the existing `Holder<ArmorMaterial>` constants (`MaterialRegistry.aMatHEV`,
`.enumArmorMaterialT51`, etc.) and call the already-provided `MaterialRegistry.
getDurabilityMultiplier(material)` to compute `ArmorItem.Type.getDurability(multiplier)` for each
leaf item's `Item.Properties.durability(...)`, exactly as that class's own javadoc prescribes — do
not re-derive or duplicate this registry.

**4. Client-side model hook** (Phase 5 will fill in the `Model` subclasses, but the base-class hook
point is this package's job to expose): `Item.initializeClient(Consumer<IClientItemExtensions>)`
overriding `IClientItemExtensions.getGenericArmorModel(LivingEntity, ItemStack, EquipmentSlot,
HumanoidModel<?> original)`, confirmed real via Neo Edition's `ArmorNo9`. This replaces every CE
`getArmorModel(EntityLivingBase, ItemStack, EntityEquipmentSlot, ModelBiped)` override 1:1 in intent.

**5. Armor-tick equivalent**: `Item.inventoryTick(ItemStack, Level, Entity, int, boolean)`, manually
guarded by `player.getItemBySlot(EquipmentSlot.X) == stack` — confirmed via Neo Edition's
`ArmorNo9.inventoryTick`. There is no more a per-slot `onArmorTick` callback distinct from the
regular item tick; every `ArmorFSB`/leaf `onArmorTick` override becomes this, with the equipment-slot
check folded in manually (CE's `ArmorFSB.onArmorTick` already does an analogous
`this.armorType != EntityEquipmentSlot.CHEST` guard, so the shape of the check is unchanged, only its
mechanism is).

**6. Armor-mod (chip/insert) storage**: `com.hbm.util.TagsUtil` (already ported,
`DataComponents.CUSTOM_DATA`-backed: `hasCustomData`/`getCustomData`/`putCustomData`) is the
confirmed real mechanism for `ArmorModHandler`'s 9-slot mod compound — no new `DataComponentType`
needs designing, this exact pattern is already live in this port and mirrors Neo Edition's own
`ArmorModHandler` verbatim (`TagsUtil.getCustomData(armor)` in place of CE's
`stack.getTagCompound()`). Individual mod `ItemStack`s serialize via `ItemStack.parse(level.
registryAccess(), tag)` / `mod.save(level.registryAccess(), tag)` (confirmed, Neo Edition
`ArmorModHandler.pryMods`/`applyMod`), replacing CE's raw `ItemStack.loadFromNBT`/`writeToNBT`.

**7. Two new `DataComponentType`s are needed** (following the exact precedent
`docs/phase0/hazard.md` already established for `hbm:bonus_radiation`/`hbm:unstable_decay_timer` —
same pattern, don't invent a new shape):
- `hbm:armor_charge` (`long`) replacing `ArmorFSBPowered`'s raw `"charge"` NBT tag
  (`Codec.LONG`/`ByteBufCodecs.VAR_LONG`).
- `hbm:jetpack_fuel` (`int`) replacing `JetpackBase`'s raw `"fuel"` NBT tag
  (`Codec.INT`/`ByteBufCodecs.VAR_INT`).
- `JetpackGlider`'s full `FluidTankNTM`-in-NBT (already flagged by `docs/phase1/items_food_gear.md`)
  should reuse whatever fluid-holding-item component shape the fluid system area has already settled
  on elsewhere in this port — this package should not invent its own scheme for it in isolation.

**8. `IGasMask`**: already ported with its Data-Component migration already decided in its own
javadoc — implement it on `ArmorGasMask`/`ArmorHazmatMask` per that javadoc's IMPLEMENTATION NOTE
(`hbm:gas_mask_filter` `ItemStack` component + `hbm:gas_mask_filter_damage` int component). No new
design needed here, just execution.

**9. `ArmorRegistry.HazardClass`**: stays exactly as already ported — armor pieces register into it
at construction time via `ArmorRegistry.registerHazard(item, HazardClass...)`, mirroring CE's
`ArmorUtil.register()` bulk call. This package's job is to write that one bulk-registration method
(`ArmorUtil.register()`, port target) once the concrete items exist, not to redesign the registry
that already exists.

**10. Armor Table GUI**: reuse `MenuBase`/`GuiInfoContainer`'s `Slot`/`SlotItemHandler` conventions
and rendering idioms — do not invent a second GUI framework — but see Open questions #1 for the
real structural mismatch between CE's block-less `ContainerArmorTable` and this port's
block-entity-bound `MenuBase<T extends MachineBaseBlockEntity>`.

**11. Explosion/block-removal batching** (PORT_SPEC's explicit "batched LevelChunk section writes +
deferred lighting" call-out, flagged per this task's own instructions): **does not apply to this
package.** No file read or grep-surveyed in the entire armor/equippable framework (`items.gear`'s
armor slice, `items.armor`, `ArmorUtil`/`ArmorModHandler`/`HazmatRegistry`/`ArmorRegistry`) removes
blocks. If a later phase discovers an armor-adjacent mechanic that does (none found here), it must
use the batched pattern, not a naive per-block `Level#setBlock` loop — noted only so this report
explicitly addresses the instruction rather than silently omitting it.

## Open questions / risks

1. **Armor Table GUI vs. `MenuBase`'s block-entity bound.** CE's `ContainerArmorTable` has *no*
   backing tile entity — a free-floating container built from a bare `InventoryBasic` (9 mod slots)
   + `InventoryCraftResult` (1 armor slot), closer to the vanilla crafting-table pattern than a
   machine GUI. This port's `MenuBase<T extends MachineBaseBlockEntity>` is generically bound to a
   block entity's `getCheckedInventory()`/`isUseableByPlayer`. Two resolution paths: **(a)** give
   `BlockArmorTable` a trivial `MachineBaseBlockEntity` subclass purely to satisfy the generic bound
   — a real (if low-risk) behavior change from CE, since the actual mod data lives on the armor
   `ItemStack`'s Data Component regardless of which container class opens it; or **(b)** write a
   second, bespoke `AbstractContainerMenu` for just this one screen, reusing only `MenuBase`'s
   `Slot`/`SlotItemHandler` conventions without extending it. This report recommends (a) for
   consistency with "use the existing framework, don't invent a second one," but flags rather than
   decides unilaterally — it's a real architectural fork, not a mechanical port choice.
2. **`MaterialRegistry`'s `DeferredRegister<ArmorMaterial>` registration-order interaction with
   `DeferredRegister.Items`** was read but not independently stress-tested against a compiler in
   this research pass (this sandbox cannot run `gradlew`). Phase 0's own review presumably already
   validated `MaterialRegistry.register()`'s call site, but Phase 3 should re-confirm it's actually
   wired into `MainRegistry`'s mod-bus setup before assuming zero risk.
3. **`LivingIncomingDamageEvent` vs. `LivingDamageEvent.Pre`'s firing order relative to vanilla
   armor-point/toughness reduction** was confirmed only via Neo Edition's *usage* of both events, not
   against NeoForge's own event-bus engine source (out of this task's read scope). Specifically:
   whether `LivingIncomingDamageEvent`'s full-cancel happens strictly before vanilla defense-point
   math matters for whether `ArmorAsbestos`/`ArmorEuphemium`/`ArmorSchrabidium`-style full-immunity
   pieces need their own `ArmorItem` defense values forced to 0 (to avoid the vanilla armor bar
   double-dipping visually) or can freely combine both. Needs a targeted check against real NeoForge
   1.21.1 source/javadoc before implementation — not guessed here.
4. **`HbmPotion` has no assigned owning phase in any document read during this research pass**, yet
   it now blocks pieces in at least two areas (`docs/phase1/items_food_gear.md`'s food/pill items,
   and this package's `ArmorUtil.checkForHazmat`/`checkForDigamma` mutation/stability fallback
   checks). Recommend whoever plans Phase 4 explicitly claims it, or flags a dedicated
   cross-cutting pass, rather than each package re-discovering the same gap independently.
5. **Whether the ~35 `ItemMod*` armor-insert leaves belong in this package's implementation pass or
   need their own follow-up survey.** They share exactly one dependency this report covers
   (`ItemArmorMod`/`ArmorModHandler`) but are otherwise independent leaf content (batteries,
   cladding, sensors, radar, knives, night-vision, etc.); at least one (`ItemModRadar`) is suspected
   —but not confirmed in this pass— to couple to the radar system (`IRadarDetectableNT`) the way
   several `items/gear` tools coupled to entity/advancement systems in `docs/phase1/
   items_food_gear.md`. Recommend porting the base class + a handful of simple, clearly
   self-contained leaves (`ItemModBattery`, `ItemModCladding`, `ItemModServos`) alongside this
   package, and doing a dedicated per-file survey of the rest the same way `items_food_gear.md` did
   for food/gear, rather than exhaustively re-deriving all 35 in this report.
6. **Jetpack particle/sound sync** (`com.hbm.packet.JetpackSyncPacket` in CE) will need a
   `CustomPacketPayload` following `HbmNetwork`'s established `record ... implements
   CustomPacketPayload` + `StreamCodec` + `registrar.playToClient(...)` pattern once jetpack tick
   logic is implemented — not designed here since the jetpack items are only Phase-3-safe in shell
   form (their rendering/particle payoff is Phase 5), but flagged so that work doesn't rediscover
   the pattern from scratch.
7. **`ArmorFSB.check(World, BlockPos)` and `ContaminationUtil.getActualPlayerRads`** (the geiger-tick
   branch) are the only two call sites in the entire package that reach into Phase 4's
   `ChunkRadiationManager`/the unported contamination facade. Stub both behind a tracked TODO
   (documented no-op, per this project's established "never silently drop" convention) rather than
   inventing a placeholder radiation number — the rest of `ArmorFSB` has no such dependency and
   should not be blocked by these two lines.
