# Phase 1 research: items/food (16) and items/gear (28) - triage and port plan

Scope: `com.hbm.items.food` (16 files) and `com.hbm.items.gear` (28 files) in the CE source
(`upstream/hbm-ce`). Read in full (all 44 files). Cross-checked against Phase 0's capability
attachments (`com.hbm.capability.HbmPlayerAttachment` / `HbmLivingAttachment`), `ModItems.java`,
`HazardRegistry.java`, and the Neo Edition reference's `NtmFoods` for confirmed 1.21/NeoForge API
shapes.

## Headline findings

1. **`items/gear` is overwhelmingly armor-slot content, not "backpacks/utility gear."** 15 of 28
   files (54%) extend `ItemArmor` / implement `ISpecialArmor` / extend the jetpack-chestplate base
   class - i.e. they are Phase 3 (armor system) by the project's own phase boundary, not Phase 1.
   Only the 13 plain tool/weapon classes (axes, hoes, pickaxes, spades, swords, and the
   Schrabidium/special-weapon variants) are actually Phase-1-safe.
2. **The single biggest blocker for `items/food` is `com.hbm.potion.HbmPotion`** (CE's custom
   `MobEffect` registry: `radiation`, `lead`, `radx`, `death`, `stability`, `potionsickness`,
   `digamma`-adjacent effects) and the game-logic facade CE calls `HbmLivingProps` /
   `ContaminationUtil`. Phase 0 explicitly did **not** port these - `VersatileConfig.java` in the
   new port has a doc comment stating `applyPotionSickness`/`hasPotionSickness` were deliberately
   skipped pending `HbmPotion`, and `HbmLivingAttachment`'s javadoc says the radiation-math/contamination
   facade built on top of it is "a later status-effect system phase." Roughly half the food items
   in this package call into one of these three things from `onFoodEaten`/`onItemUseFinish`.
3. Phase 0's capability attachments already cover two things food/gear code needs:
   `HbmPlayerAttachment` has `getShield`/`setShield`/`getMaxShield` (used by `ItemFlask`) and
   `isJetpackActive()` (used by all `Jetpack*` classes), so those specific calls are **not**
   blocked - only the potion/radiation-side calls are.
4. All CE model-registration plumbing on these items (`IDynamicModels`, `IClaimedModelLocation`,
   `ClaimedModelLocationRegistry`, `bakeModel`/`registerModel`/`registerSprite`,
   `ModelResourceLocation`) is 1.12 Forge-model-loader machinery with no 1.21 equivalent - it is
   superseded wholesale by datagen (item model JSON generation) per the port's ground rules. Do not
   port any of it; only the constructor arguments (food values, translation/registry name) and the
   eating/use behavior matter.
5. `setCreativeTab(MainRegistry.xTab)` calls embedcded in every constructor are likewise dropped -
   Phase 0's `ModItems.java` deliberately excludes creative-tab wiring from item construction;
   tab membership is assigned by a `BuildCreativeModeTabContentsEvent` listener elsewhere
   (`ModCreativeTabs.java`).

---

## items/food (16 files)

| File | Purpose | Phase-1-safe? | Blocking dependency |
|---|---|---|---|
| `ItemFoodBase.java` | Base class for simple named foods with a dynamic-model hook; 3 special-cased instances (`bomb_waffle`, `cotton_candy`, `schnitzel_vegan`) get hardcoded potion-effect bundles + one triggers a full nuke explosion on eating | Partially | `bomb_waffle`'s `onFoodEaten` spawns `EntityNukeExplosionMK5`/`EntityNukeTorex` (Phase 2 entities) and reads `BombConfig`; the other two variants are plain vanilla `MobEffect`s and are Phase-1-safe today |
| `ItemConserve.java` | "Canned conserve" - **metadata-driven multi-item**, 27 `EnumFoodType` variants (beef, tuna, bhole, recursion, fist, napalm, ...) sharing one `ItemStack` via damage value, each with its own food/saturation values | Needs expansion | Not a system dependency - a **flattening problem**: must become 27 distinct registry entries (`hbm:canned_beef`, `hbm:canned_tuna`, ... `hbm:canned_bhole`, etc., matching the `canned_<name>` id shape the class already derives). `BHOLE` variant spawns `EntityVortex` (Phase 2 entity); `FIST` variant deals magic damage (fine); `RECURSION` duplicates itself 90% of the time (fine, self-contained) |
| `ItemEnergy.java` | Cans/bottles (drinks) - one class, ~24 separate single-item instances (not metadata-multi despite one shared class) each with its own hardcoded potion-effect branch in `onItemUseFinish`, container-swap-to-empty-can/bottle logic, and criteria trigger | Partially | Several branches (`bottle_cherry`, `bottle_nuka`, `bottle_sparkle`, `bottle_quantum`, `bottle_rad`, `coffee_radium`) call `ContaminationUtil.contaminate(...)` or `HbmLivingProps.incrementRadiation(...)` - both unported facades (see headline #2). Every branch also calls `VersatileConfig.applyPotionSickness` (unported, see #2). The plain-potion-effect branches (`can_smart`, `can_creature`, `can_redbomb`, `can_mrsugar`, `can_overcharge`, `can_luna`, `can_bepis`, `can_breen`, `can_mug`, `bottle2_*`, `coffee`) are Phase-1-safe once `applyPotionSickness` is stubbed out or made a no-op |
| `ItemCanteen.java` | Reusable canteen with a cooldown (uses item damage as a cooldown timer, not a variant axis) - 3 instances (`canteen_13`, `canteen_vodka`, `canteen_fab`) | Mostly | Calls `VersatileConfig.applyPotionSickness`/`hasPotionSickness` (unported). Damage-as-cooldown is a Data Component concern (see NBT/DC notes below), not a metadata-variant concern - do **not** treat this as needing per-item expansion |
| `ItemCrayon.java` | Chemical-dye-colored crayon - **metadata-driven multi-item**, one instance keyed off `ItemChemicalDye.EnumChemDye` (16 colors) | Needs expansion | Flattening problem: 16 distinct items (`hbm:crayon_black` ... `hbm:crayon_white`). Cross-package dependency: `ItemChemicalDye.EnumChemDye` lives in `items/machine` (a different Phase 1 survey area) - the enum itself has no registration dependency, just needs that file read to confirm color names/order when this item is built |
| `ItemEnergy.java` (see above) | | | |
| `ItemFlask.java` | Shield-infusion drink - technically uses the `ItemEnumMulti` metadata-multi machinery but only declares **one** enum value (`SHIELD`) | Phase-1-safe today | None blocking - `HbmCapability.getData(player).setMaxShield/setShield` maps directly onto the already-ported `HbmPlayerAttachment.getMaxShield/setMaxShield/getShield/setShield`. Since there's only one variant, this should just become a single plain item, not a generified multi-item class |
| `ItemFoodSoup.java` | Thin wrapper around vanilla `ItemSoup` (mushroom-stew-style, returns empty bowl) - 3 instances (`glowing_stew`, `balefire_scrambled`, `balefire_and_ham`), no custom `onFoodEaten` | Phase-1-safe | None - purely a food-value + bowl-return wrapper |
| `ItemLemon.java` | Misused/overloaded generic food base actually reused for ~25 unrelated single foods (`lemon`, `med_ipecac`, `loops`, `twinkie`, `pudding`, `ingot_semtex`(!), `marshmallow`, `cheese`, `glyphid_meat`, etc.) - each a distinct instance, not metadata-multi | Mostly | Plain `PotionEffect` branches (`med_ipecac`/`med_ptsd` hunger, `loop_stew` regen/speed/strength) are Phase-1-safe. `loop_stew`'s bowl-return via `onItemUseFinish` override is Phase-1-safe (vanilla `Items.BOWL` equivalent). No potion-sickness or radiation calls in this file - genuinely clean |
| `ItemMuchoMango.java` | Long-duration-use giant can (`mucho_mango`), grants Speed on finish | Phase-1-safe | None |
| `ItemNugget.java` | Thin `ItemFood` wrapper, only one real instance (`gun_moist_nugget`), tooltip joke only | Phase-1-safe | None |
| `ItemPancake.java` | "Space pancake" - eating it doesn't restore hunger, it recharges every battery-backed item in the player's armor slots (`Library.chargeBatteryIfValid`); right-click is gated behind `ArmorFSB.hasFSBArmorIgnoreCharge` + a specific helmet check | Deferred | Directly couples to `ArmorFSB` (Phase 3 armor) and the "battery-backed armor" mechanic (`bj_helmet`); cannot be meaningfully ported until that armor class exists. The food-value/registration shell is trivial but the entire point of the item is the armor coupling |
| `ItemPill.java` | ~13 separate pill/medicine instances (`radx`, `siox`, `pill_herbal`, `xanax`, `fmn`, `five_htp`, `pill_iodine`, `plan_c`, `pill_red`, `chocolate`) sharing one class, each with a hardcoded effect branch; not metadata-multi (each is `new ItemPill(0, "name")`, single damage value) | Deferred (mostly) | Nearly every branch reads/writes `HbmLivingAttachment`-equivalent radiation/digamma/asbestos/blacklung state via the CE facade (`HbmLivingProps.setAsbestos`, `.setBlackLung`, `.incrementRadiation`, `.getDigamma`/`.setDigamma`) or applies `HbmPotion.lead`/`.death`/`.radx`/`.stability` (unported registry, see #2), or calls `VersatileConfig.applyPotionSickness`. `pill_iodine`'s "remove negative effects" branch and `chocolate`'s vanilla-only branch (haste/speed/jump + 1-in-25 self-damage via `ModDamageSource.overdose`, itself unported) are the only near-Phase-1-safe pieces, and even those need `applyPotionSickness` stubbed |
| `ItemAppleEuphemium.java` | Golden-apple-style infinite-duration buff apple, single item, `EnumRarity.EPIC` | Phase-1-safe | None - plain vanilla `MobEffect`s (`RESISTANCE`, `FIRE_RESISTANCE`, `SATURATION`) at max duration/amplifier |
| `ItemAppleSchrabidium.java` | **Metadata-driven multi-item**: 2 instances (`apple_schrabidium`, `apple_lead`) x 3 damage-value tiers each = effectively 6 distinct behaviors, extends `ItemFoodBase` and adds `setHasSubtypes(true)` | Needs expansion + partially blocked | Flattening problem: needs 6 distinct items (e.g. `apple_schrabidium_low/mid/high`, `apple_lead_low/mid/high` - naming needs a decision, CE has no separate names per tier today). The `apple_schrabidium` tiers are plain vanilla `MobEffect`s (Phase-1-safe); the `apple_lead` tiers use `HbmPotion.lead` and `ModDamageSource.lead` (both unported, see #2) |
| `ItemBDCL.java` | "Drink slowly" item extending `ItemBakedBase` (a base class in `items/`, not `items/food` - out of this survey's file list but a direct dependency); ticks down over `onUsingTick`, plays gulp/groan sounds | Phase-1-safe | None found in this file itself - depends on `ItemBakedBase` (survey that base class as part of whichever area covers `items/*.java` directly) and `HBMSoundHandler.gulp`/`.groan` (Phase 0 sound registry - already built, just needs the specific sound events registered) |
| `ItemTemFlakes.java` | **Metadata-driven multi-item**: 1 instance, 3 damage-value tiers (cheap/regular/expensive), all identical behavior (heal 2 HP), only tooltip text differs | Needs expansion | Flattening problem: 3 distinct items (e.g. `hbm:tem_flakes`, `hbm:tem_flakes_premium`, `hbm:tem_flakes_deluxe` - naming needs a decision, CE only names the base). No system dependency otherwise - `player.heal(2.0F)` is vanilla |

### Food summary counts
- Phase-1-safe today (no blocking system, may still need metadata expansion): `ItemFoodSoup`,
  `ItemLemon`, `ItemMuchoMango`, `ItemNugget`, `ItemAppleEuphemium`, `ItemBDCL`, `ItemFlask`
  (7 files clean).
- Needs metadata-to-distinct-items expansion (flattening): `ItemConserve` (27 items),
  `ItemCrayon` (16 items), `ItemAppleSchrabidium` (6 items, partially blocked too),
  `ItemTemFlakes` (3 items). `ItemFlask` technically uses the same base class but only has 1
  variant, so it should be de-generified into a plain item rather than kept as a 1-variant
  metadata-multi item.
- Blocked in whole or in large part on the unported `HbmPotion` / `HbmLivingProps` /
  `ContaminationUtil` facade: `ItemEnergy`, `ItemCanteen`, `ItemPill`, half of
  `ItemAppleSchrabidium`, the `bomb_waffle` branch of `ItemFoodBase`.
- Blocked on Phase 3 armor coupling: `ItemPancake` (FSB armor battery charging).

---

## items/gear (28 files)

### Armor-slot items - defer to Phase 3 (15 files)

All of these extend `ItemArmor` (or, for the jetpacks, the armor-slot `ItemArmorMod`/
`JetpackFueledBase` chestplate base in `items/armor`, a package outside this survey). Phase 3 owns
the equipment-slot item system in 1.21 (NeoForge replaced `ItemArmor` subclassing + per-item
`getArmorTexture`/`getArmorModel` overrides with the data-component-driven `Equippable`/armor
trim system - confirm the exact 1.21.1 shape against the Neo Edition reference's armor items when
Phase 3 starts, do not guess it here).

| File | Purpose | Blocking dependency |
|---|---|---|
| `ModArmor.java` | Generic armor base; texture dispatch by `==` comparison against ~20 `ModItems` fields (steel/titanium/alloy/cmb/paa/asbestos/security sets) | Phase 3: armor system + all those material sets need to exist as items first |
| `ArmorModel.java` | Armor with a custom `ModelBiped` (goggles, gas-damp/piss masks, capes, hat) + helmet-overlay blur rendering + random rag-drop-on-decay tick | Phase 3: custom armor models are a rendering-layer concern on top of the armor system |
| `ArmorAsbestos.java` | `ISpecialArmor` fire-immune asbestos suit, extinguishes wearer every tick | Phase 3: `ISpecialArmor`'s 1.21 equivalent (damage-reduction hook) doesn't exist yet in the port |
| `ArmorEuphemium.java` | `ISpecialArmor` full-set-bonus armor (regen/resist/fire-immune/saturation + no-fall-damage), damage-immune, `ArmorUtil.checkArmor` full-set check | Phase 3: same `ISpecialArmor` gap + `ArmorUtil` (unported) |
| `ArmorFSB.java` | The big one - "full suit bonus" fluent-builder armor base (effects list, step/jump/fall sounds, geiger integration, VATS/thermal-sight flags, part-hiding, hazard-class blacklist, rad-resist registration into `HazmatRegistry`) used by dozens of CE armor sets | Phase 3: this is armor-system core infrastructure, not a leaf item - needs the whole `ArmorRegistry`/`HazmatRegistry`/`ArmorUtil` stack |
| `ArmorGasMask.java` | `IGasMask` armor (goggles/M65/mono/olde gas masks), filter install/damage, hazard blacklist | Phase 3: `IGasMask` interface + `ArmorUtil.addGasMaskTooltip`/`getGasMaskFilter` (unported) |
| `ArmorHazmat.java` | Plain hazmat suit set (base + red/grey variants), helmet blur overlay | Phase 3 |
| `ArmorHazmatMask.java` | Hazmat helmet variant implementing `IGasMask` on top of `ArmorHazmat` | Phase 3 |
| `ArmorSchrabidium.java` | `ISpecialArmor` near-immune armor with per-piece potion effects on tick | Phase 3 |
| `MaskOfInfamy.java` | Single cosmetic mask armor item, texture only | Phase 3 (still an `ItemArmor`, trivial once the slot system exists) |
| `JetpackBooster.java` | Chest-slot jetpack: vectorized boost movement, particle packet, fuel burn | Phase 3 (armor-slot item) + Phase 2 (`AuxParticlePacketNT`/`HbmEffectNT` particle system) |
| `JetpackBreak.java` | Chest-slot jetpack: hover-mode variant | Phase 3 + Phase 2 (same particle system) |
| `JetpackGlider.java` | Chest-slot jetpack: `IFillableItem` fuel-tank-in-NBT item (fill/drain by fluid type, NBT-stored `FluidTankNTM`) | Phase 3 (armor-slot `ItemArmorMod` base) + fluid tank system; **NBT-heavy**, see Data Component notes below |
| `JetpackRegular.java` | Chest-slot jetpack: basic upward-thrust variant | Phase 3 + Phase 2 particles |
| `JetpackVectorized.java` | Chest-slot jetpack: directional-look-vector thrust variant, keybind-gated | Phase 3 + Phase 2 particles + `HbmKeybinds` (client input, likely a separate area) |

### Tools/weapons - Phase-1-safe (13 files)

None of these touch `EntityEquipmentSlot`/`ItemArmor`. They are plain `ItemTool`-family
(`ItemAxe`/`ItemHoe`/`ItemPickaxe`/`ItemSpade`/`ItemSword`) subclasses with a `ToolMaterial` +
registry name, which is exactly the shape 1.21's `Tiers`/`ToolMaterial`-equivalent + `AxeItem`/
`HoeItem`/`PickaxeItem`/`ShovelItem`/`SwordItem` still supports.

| File | Purpose | Phase-1-safe? | Notes |
|---|---|---|---|
| `ModAxe.java` | Generic axe base, two constructors (default damage/speed, or explicit override) | Yes | None |
| `ModHoe.java` | Generic hoe base, overrides attribute modifiers (adds attack damage/speed to an otherwise damage-less vanilla hoe) | Yes | `getItemAttributeModifiers` is 1.12-era; 1.21 uses `ItemAttributeModifiers` data components on `Item.Properties` - verify the modern equivalent against the Neo Edition reference before porting, don't guess the exact builder shape here |
| `ModPickaxe.java` | Generic pickaxe base | Yes | None |
| `ModSpade.java` | Generic shovel base | Yes | None |
| `ModSword.java` | Generic sword base, tooltip-only special-casing for ~6 named swords (`saw`, `bat`, `bat_nail`, `golf_club`, `pipe_rusty`, `pipe_lead`, `reer_graar`) | Yes | Tooltips are trivial to port (translation keys instead of hardcoded `==` checks + `list.add`, per the Data Component / lang-key modernization every text-bearing item needs) |
| `AxeSchrabidium.java` | Rare-rarity single-stack axe | Yes | None |
| `HoeSchrabidium.java` | Rare-rarity hoe | Yes | None |
| `PickaxeSchrabidium.java` | Rare-rarity pickaxe | Yes | None |
| `SpadeSchrabidium.java` | Rare-rarity shovel | Yes | None |
| `SwordSchrabidium.java` | Rare-rarity sword | Yes | None |
| `BigSword.java` | Trivial `ItemSword` subclass, no overrides at all beyond ctor | Yes | None - almost pointless as a distinct class; could arguably just be `ModSword` |
| `RedstoneSword.java` | "Very first NTM item" - on right-click-block places `Blocks.REDSTONE_WIRE` in the adjacent air block and damages itself 14 per use, breaks after enough uses | Mostly yes | Core logic (place redstone wire, damage on use) is Phase-1-safe vanilla logic. Implements `IHasCustomModel` (1.12 rendering interface, drop - datagen handles this) |
| `WeaponSpecial.java` | ~13 named special melee weapons sharing one class (`schrabidium_hammer` one-shot-kill, `bottle_opener` random-debuff-on-hit, `shimmer_sledge`/`shimmer_axe` block-destruction-on-hit-or-use, `wrench`/`wrench_flipped` knockback + slow-move attribute, `ullapool_caber` self-damaging explosive throw, `memespoon` fall-damage-execute + nuke-on-huge-fall, `stopsign`/`sopsign`/`wood_gavel`/`lead_gavel`/`diamond_gavel` sound/effect-on-hit gavels) | Mostly yes, with named exceptions | The item class/registration/rarity/attribute-modifier logic is Phase-1-safe. Specific behaviors are blocked on not-yet-ported systems: `memespoon`'s big-fall-nuke branch needs `EntityNukeExplosionMK5`/`EntityNukeTorex` (Phase 2 entities) and `BombConfig`; `shimmer_sledge`'s block-to-item logic needs `EntityRubble` (Phase 2 entity); `onUpdate`'s advancement grants need `AdvancementManager`/`ArmorUtil.checkForFiend` (unported); `lead_gavel` applies `HbmPotion.lead` (unported, see food finding #2). Recommend porting the item shell + the self-contained branches (`bottle_opener`, `wrench`/`wrench_flipped`, `diamond_gavel`, `wood_gavel`, `stopsign`) now and flagging the entity/advancement/potion branches as follow-up TODOs tied to their owning phases |

---

## Metadata/flattening summary (post-1.13 damage-value removal)

Definite N-to-N expansion needed (each becomes a distinct registry entry, no shared class instance
with subtypes):

| CE class | Variant axis | Count | Suggested id shape |
|---|---|---|---|
| `ItemConserve` | `EnumFoodType` (27 values: beef, tuna, mystery, pashtet, cheese, jizz, milk, ass, pizza, tube, tomato, asbestos, bhole, hotdogs, leftovers, yogurt, stew, chinese, oil, fist, spam, fried, napalm, diesel, kerosene, recursion, bark) | 27 | `hbm:canned_<name>` (matches the class's own `getPrefix()+getSeparator()+name` derivation) |
| `ItemCrayon` | `ItemChemicalDye.EnumChemDye` (16 colors) | 16 | `hbm:crayon_<color>` |
| `ItemAppleSchrabidium` | 2 base instances x 3 damage tiers | 6 | needs a naming decision - CE has no tier names, only meta 0/1/2; recommend something like `low/medium/high` or `weak/potent/apex` suffixes, to be finalized with whoever owns naming conventions |
| `ItemTemFlakes` | 3 damage tiers, single instance, tooltip-only difference | 3 | same naming-decision note as above (`hbm:tem_flakes`, `hbm:tem_flakes_<tier2>`, `hbm:tem_flakes_<tier3>`) |

Already effectively single/unique despite a shared class (do **not** expand, just instantiate once
per CE call site as today): `ItemFoodBase`, `ItemEnergy`, `ItemCanteen`, `ItemLemon`,
`ItemMuchoMango`, `ItemNugget`, `ItemPancake`, `ItemPill`, `ModArmor`/`ArmorModel`/etc.,
`ModAxe`/`ModHoe`/`ModPickaxe`/`ModSpade`/`ModSword` and their Schrabidium variants, `BigSword`,
`RedstoneSword`, `WeaponSpecial`. `ItemFlask` uses the metadata-multi machinery but only declares
one enum value - recommend de-generifying it to a plain item rather than keeping a 1-variant
generic class.

`ItemCanteen`'s damage value is a **cooldown timer**, not a content variant - unrelated to the
flattening problem, must survive as mutable per-stack state (a Data Component, see below), not as
multiple registry entries.

## NBT -> Data Component notes

- **`ItemCanteen`**: `stack.getItemDamage()` used purely as a per-stack cooldown counter, ticked
  down in `onUpdate`. Needs a custom Data Component (e.g. an int) rather than vanilla damage, since
  1.21 damage bars are tied to `DataComponents.DAMAGE`/`MAX_DAMAGE` and this isn't really "damage."
- **`JetpackGlider`**: stores an entire `FluidTankNTM` serialized into a `NBTTagCompound` under a
  `"fuelTank"` key on the stack (`stack.getTagCompound().getCompoundTag("fuelTank")`). This is the
  most NBT-heavy item in either package - needs a purpose-built Data Component wrapping fluid
  type + amount (whatever shape the fluid system area settles on for other fluid-holding items;
  this should not invent its own scheme in isolation).
- **`ItemAppleSchrabidium`, `ItemTemFlakes`, `ItemConserve`, `ItemCrayon`**: currently read tier/
  variant from `stack.getItemDamage()`. Once flattened into distinct items (see table above), this
  need disappears entirely - no Data Component required, the variant becomes "which item is it."
- Everything else in both packages reads/writes ordinary mutable entity/player state (potions,
  capability attachments), not stack NBT - no Data Component design needed for those.

## Food-effect (MobEffect) inventory

Vanilla `MobEffects` used directly (Phase-1-safe, no new registry needed): `STRENGTH`,
`RESISTANCE`, `REGENERATION`, `FIRE_RESISTANCE`, `HASTE`, `SPEED`, `WITHER`, `POISON`, `WEAKNESS`,
`BLINDNESS`, `NAUSEA`, `HUNGER`, `JUMP_BOOST`, `HEALTH_BOOST`, `ABSORPTION`, `SATURATION`,
`NIGHT_VISION`, `WATER_BREATHING`, `MINING_FATIGUE`.

Custom `HbmPotion` effects referenced but **not yet registered** anywhere in the new port
(confirmed via Glob - no `HbmPotion`/`*MobEffect*` file exists yet under `src/main/java/com/hbm`):
`lead`, `radiation`, `radx`, `death`, `stability`, `potionsickness`. These block: `ItemPill`
(`radx`, `pill_red`->`death`, `five_htp`->`stability`), `ItemAppleSchrabidium`'s `apple_lead`
branch (`lead`), and indirectly every item that calls `VersatileConfig.applyPotionSickness`
(`ItemEnergy`, `ItemCanteen`, `ItemPill`). Recommend flagging `HbmPotion`/custom-MobEffect
registration as a prerequisite research/implementation area before these specific items can be
completed - the item *registration* can still happen in Phase 1, but the eating behavior for the
affected branches will need a follow-up pass once that MobEffect registry exists.

## Recommended Phase 1 sequencing for this area

1. Register all Phase-1-safe leaf items now: `ItemFoodSoup`, `ItemLemon`, `ItemMuchoMango`,
   `ItemNugget`, `ItemAppleEuphemium`, `ItemBDCL`, `ItemFlask` (as a plain item), plus every plain
   tool/weapon in gear (`ModAxe`/`ModHoe`/`ModPickaxe`/`ModSpade`/`ModSword` and Schrabidium
   variants, `BigSword`, `RedstoneSword`, and `WeaponSpecial`'s self-contained hit branches).
2. Flatten and register the four metadata-multi items (`ItemConserve` x27, `ItemCrayon` x16,
   `ItemAppleSchrabidium` x6, `ItemTemFlakes` x3) - 52 registry entries from 4 CE classes, the
   largest concrete item count in this whole survey area.
3. Register `ItemEnergy`'s and `ItemPill`'s and `ItemCanteen`'s items now (so hazard bindings,
   creative tab contents, and recipes elsewhere in Phase 1 aren't blocked on them existing), but
   leave their potion-sickness/radiation/custom-MobEffect branches as explicit TODOs pointing at
   the `HbmPotion` gap - do not silently drop the behavior or silently stub it to a no-op without
   flagging it.
4. Defer entirely to Phase 3: all 15 armor-slot files, plus `ItemPancake` (FSB armor coupling).
5. Defer the nuke/rubble/advancement-trigger branches inside `ItemFoodBase` (`bomb_waffle`) and
   `WeaponSpecial` (`memespoon`, `shimmer_sledge`, the `onUpdate` advancement grants) to whichever
   phase ports `EntityNukeExplosionMK5`, `EntityNukeTorex`, `EntityRubble`, and
   `AdvancementManager` - track them as named follow-ups on the otherwise-portable item shells.
