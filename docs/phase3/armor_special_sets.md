# Special-behavior armor sets (asbestos/euphemium/gas-mask/hazmat/schrabidium/mask-of-infamy) — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/gear/{ArmorAsbestos,ArmorEuphemium,ArmorGasMask,
  ArmorHazmat,ArmorHazmatMask,ArmorSchrabidium,MaskOfInfamy}.java` — the 7 files this task names
  (confirmed by `Glob` that CE actually keeps this content in `com.hbm.items.gear`, not
  `com.hbm.items.armor` — the parallel `armor_framework` research task's package name is a project
  label for the *system*, not a CE source-tree path; `com.hbm.items.armor` in CE holds a completely
  different, unrelated set of ~35 armor files (`ArmorFSB`-descended combat/power-armor sets,
  `ArmorHat`, `ArmorRPA`, jetpacks, cosmetics) that are out of this task's scope)
- `upstream/hbm-ce/src/main/java/com/hbm/items/gear/ArmorFSB.java` (full, 541 lines) — not one of
  the 7 named files, but load-bearing context: see Headline finding #1 below
- `upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java` (grepped for every field declaration
  of the 26 concrete items these 7 classes produce, to get real registry names, materials, and —
  critically — which class each item actually instantiates today)
- `upstream/hbm-ce/src/main/java/com/hbm/handler/ArmorUtil.java` (full, 267 lines — the
  `IGasMask`/hazard-class/full-set-check facade every one of the 7 files calls into)
- `upstream/hbm-ce/src/main/java/com/hbm/util/ContaminationUtil.java` (`applyAsbestos`, read to
  confirm what actually gates asbestos-lung accumulation — see Headline finding #2)
- This port's own already-committed code, read in full: `src/main/java/com/hbm/api/item/IGasMask.java`,
  `src/main/java/com/hbm/util/ArmorRegistry.java` (both already ported by the parallel
  `hazmat_protection_integration` task), `src/main/java/com/hbm/capability/HbmLivingAttachment.java`,
  `src/main/java/com/hbm/items/gear/GearItems.java` + `GearTiers.java` (Phase 1's sibling file in the
  same package), `src/main/java/com/hbm/packet/HbmNetwork.java` + `packet/toclient/BufPacket.java`,
  `src/main/java/com/hbm/inventory/container/MenuBase.java`, `src/main/java/com/hbm/damage/ModDamageTypes.java`,
  `src/main/java/com/hbm/hazard/HazardSystem.java`
- Neo Edition reference, for confirmed real NeoForge 1.21.1 API shape only (never for behavior):
  `upstream/neo-edition/src/main/java/com/hbm/items/armor/{ArmorNo9,ItemArmorMod}.java`,
  `upstream/neo-edition/src/main/java/com/hbm/util/DamageResistanceHandler.java`
- `docs/phase1/items_food_gear.md` (this task's own starting point — its "Armor-slot items - defer
  to Phase 3" table names these exact 7 files plus `ModArmor`/`ArmorModel`/`ArmorFSB`/4 jetpacks,
  one line each), `docs/phase1/items_tool.md` (structural model for this report), and
  `docs/phase2/rbmk_reactor.md` (structural + depth model for this report, per task instructions)

## Headline findings

1. **Two of the seven named classes are dead code in current CE — `ArmorAsbestos` and
   `ArmorSchrabidium` are never instantiated anywhere.** Grepping `ModItems.java` for
   `new ArmorAsbestos(` / `new ArmorSchrabidium(` returns zero matches. The four `asbestos_*` items
   and four `schrabidium_*` items are all actually built with `new ArmorFSB(...)` today (see e.g.
   `ModItems.java:478-481` and `:534-541`) — CE migrated both sets onto the newer `ArmorFSB`
   fluent-builder base class (full-set-bonus potion effects, step/jump/fall sounds, hazard-class
   registration via `.setHazardClass(...)`, rad-resist via `.setRadResist(...)`) at some point after
   these two leaf classes were written, and never deleted the leaf classes or their surviving
   `getProperties`/`onArmorTick` logic. **This means the real current CE behavior for asbestos and
   schrabidium armor is NOT what `ArmorAsbestos.java`/`ArmorSchrabidium.java` describe** — it's
   whatever `ArmorFSB` + the specific `.set*(...)` builder calls at each item's `ModItems.java`
   declaration site configure. Porting these two files' logic (fire immunity via `ISpecialArmor`,
   the schrabidium per-piece potion-effect-on-tick table) as if it were live behavior would silently
   reintroduce old/removed behavior. This is flagged loudly per the ground rules' "no silent
   punting" instruction — the correct Phase 3 action for asbestos and schrabidium armor is to treat
   `ArmorFSB` (a **different**, larger, already-identified-but-not-yet-researched Phase 3 file per
   `items_food_gear.md`'s own table) as the real spec, and treat `ArmorAsbestos.java`/
   `ArmorSchrabidium.java` as historical reference only — see Deferred scope. The other 5 files
   (`ArmorEuphemium`, `ArmorGasMask`, `ArmorHazmat`, `ArmorHazmatMask`, `MaskOfInfamy`) **are** live:
   every item field they produce is still constructed with `new Armor<That Class>(...)` in
   `ModItems.java` today.
2. **`ArmorAsbestos`'s own fire-immunity/extinguish behavior is real and portable regardless of
   finding #1** (whether it ends up being `ArmorFSB`-hosted or kept as a standalone
   `ISpecialArmor`-equivalent implementation is an open question below), but the thing most players
   would call "asbestos suit protection" — not accumulating asbestos-lung disease from ambient dust —
   is **not** granted by wearing the suit at all in current CE. `ContaminationUtil.applyAsbestos`
   gates lung-dust accumulation on `ArmorRegistry.hasProtection(entity, HEAD, HazardClass.PARTICLE_FINE)`,
   and `ArmorUtil.register()` only assigns `PARTICLE_FINE` to gas-mask filter items
   (`gas_mask_filter`, `gas_mask_filter_mono`, `gas_mask_filter_combo`) — `asbestos_helmet` is
   registered with `SAND`/`LIGHT` only, never `PARTICLE_FINE`. A correctly-filtered gas mask protects
   against asbestos-lung buildup; the asbestos suit itself does not, in current CE. This is a
   genuine CE-side naming/design quirk (not a bug for this port to fix), but worth flagging so the
   implementer doesn't "improve" it by adding `PARTICLE_FINE` to the asbestos helmet's hazard-class
   registration under the assumption that's an oversight — that would be a behavior change from CE,
   not a faithful port.
3. **All 7 files sit on top of infrastructure that is split between "already ported" and "not yet
   ported," and the split runs *inside* `ArmorUtil`, not across it.** The parallel
   `hazmat_protection_integration` research/implementation task has already landed
   `com.hbm.api.item.IGasMask` (data-component-based, confirmed real by reading it — already updated
   for 1.21 Data Components per its own javadoc) and `com.hbm.util.ArmorRegistry` (with the exact
   `HazardClass` enum CE uses: `GAS_LUNG`, `GAS_MONOXIDE`, `GAS_INERT`, `PARTICLE_COARSE`,
   `PARTICLE_FINE`, `BACTERIA`, `NERVE_AGENT`, `GAS_BLISTERING`, `SAND`, `LIGHT` — all present,
   matching CE 1:1). **`com.hbm.handler.ArmorUtil` itself does not exist in this port yet**, nor does
   `com.hbm.handler.ArmorModHandler` (the "armor mod slot" pry/insert system `ArmorRegistry`'s own
   `getProtectionFromItem` already calls into via `ArmorModHandler.hasMods`/`pryMods`). Every one of
   the 5 live files calls at least one `ArmorUtil` static method
   (`addGasMaskTooltip`/`getGasMaskFilter`/`installGasMaskFilter`/`damageGasMaskFilter`/`removeFilter`/
   `checkArmor`). This is this task's single biggest concrete dependency, and it belongs to the
   `hazmat_protection_integration` package's own remaining scope, not to this one — noted here only
   as a named blocking dependency, not re-researched (per task instructions).
4. **`ISpecialArmor` (the 1.12 Forge interface `ArmorAsbestos`/`ArmorEuphemium`/`ArmorSchrabidium`
   all implement for their damage-absorption behavior) has no NeoForge 1.21.1 successor interface —
   the replacement is an event, not an item override**, confirmed by reading
   `upstream/neo-edition/src/main/java/com/hbm/util/DamageResistanceHandler.java` in full: it
   subscribes to `net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent` with
   `@SubscribeEvent`, reads `event.getSource()`/`event.getEntity()`/`event.getOriginalAmount()`,
   and either `event.setCanceled(true)` (full absorption) or `event.setAmount(reducedAmount)`
   (partial reduction) — this is a confirmed-real, currently-compiling pattern in the sibling
   reference project, not a guess. See Key design/API decisions for how this maps onto each of the
   3 `ISpecialArmor` files here.
5. **`MaskOfInfamy` is genuinely trivial** — a single-item, texture-only `ItemArmor` subclass with no
   overridden behavior beyond `getArmorTexture`. It is the only one of the 7 files with zero
   dependency on `ArmorUtil`/`ArmorRegistry`/`ISpecialArmor`, and is Phase-3-safe today in the literal
   sense that nothing blocks it once the equipment-slot item shape itself exists.

## Per-file findings

### `ArmorAsbestos.java` — **dead code in current CE** (see Headline #1); fire-immune `ISpecialArmor`

- 4 items in CE's `ModItems.java` share this class's *name* conceptually (`asbestos_helmet/_plate/
  _legs/_boots`) but are actually built with `ArmorFSB`, not this class, today.
- What this class itself specifies, if it were live: `getProperties` returns full absorption
  (`ArmorProperties(1, 1, 999999999)`) only when `source.isFireDamage()` is true, otherwise no
  absorption at all — i.e. immune to fire damage, ordinary armor value (from `ArmorMaterial
  aMatAsbestos`, not researched here) for everything else. `onArmorTick` calls `player.extinguish()`
  every tick regardless of which piece is worn (bug-for-bug: even boots-only extinguishes the
  player, since `onArmorTick` is called per-piece with no slot check). `getArmorDisplay` returns
  a fixed per-slot armor-icon count (3/8/6/3) — 1.12-era GUI icon count, no modern equivalent needed
  (vanilla `ArmorItem` derives this from the material's defense value).
- `damageArmor` multiplies incoming durability damage by `1` (i.e. no-op multiplier, plain vanilla
  durability loss) — functionally identical to not overriding it at all.
- Client-only `renderHelmetOverlay` draws a full-screen alpha-blended overlay texture
  (`overlay_asbestos.png`) when the helmet is worn — this exact overlay mechanism is already solved
  generically by `ArmorFSB.setOverlay(String)`/`renderHelmetOverlay` (confirmed: `asbestos_helmet`'s
  real `ModItems.java` declaration calls `.setOverlay(Tags.MODID + ":textures/misc/overlay_asbestos.png")`
  on its `ArmorFSB` instance), which is further evidence the class actually governing this item today
  is `ArmorFSB`, not this file.

### `ArmorEuphemium.java` — live; full-set-bonus `ISpecialArmor`, damage-immune

- 4 items, all still built with `new ArmorEuphemium(...)`: `euphemium_helmet/_plate/_legs/_boots`
  (`ModItems.java:482-485`), all `EnumRarity.EPIC` (`getRarity` override).
- `getProperties`: full absorption (`ArmorProperties(1, 1, 999999999)`) **only when the full 4-piece
  set is worn**, checked via `ArmorUtil.checkArmor(player, euphemium_helmet, euphemium_plate,
  euphemium_legs, euphemium_boots)` (exact-item-per-slot equality, not material/tag matching) — i.e.
  the wearer becomes damage-immune to everything, but only in the complete set; any single missing
  piece removes all absorption.
- `onArmorTick`, gated behind the same full-set check: applies `REGENERATION`/`RESISTANCE`/
  `FIRE_RESISTANCE`/`SATURATION` at amplifier 127 for 5 ticks each (`ambient=true`,
  `showParticles=false`) every tick (a "keep-alive" refresh pattern, not a one-shot grant — this is
  the same idiom `ArmorFSB.handleTick` uses for its own effects list), and clamps fall speed to
  `motionY >= -0.25` with `fallDistance = 0` (soft-landing / no fall damage), also full-set-gated.
- `damageArmor` multiplies by `0` — the armor **never loses durability**, confirmed distinct from
  Asbestos's `x1` no-op. `setDamage`/`getDamage`/`getMaxDamage` are also overridden to hardcode
  0 damage / `Integer.MAX_VALUE` max — belt-and-suspenders indestructibility on top of the
  `damageArmor` override.
- No custom render overlay, no custom armor model — texture dispatch only (2 texture variants:
  helmet/chest/boots share `euphemium_1.png`, legs use `euphemium_2.png`, same split pattern as
  Asbestos/Hazmat/Schrabidium below).

### `ArmorGasMask.java` — live; the concrete `IGasMask` implementation for 4 mask items, custom model

- 4 items, all still built with `new ArmorGasMask(ArmorMaterial.IRON, ...)`: `gas_mask`,
  `gas_mask_m65`, `gas_mask_mono`, `gas_mask_olde` (`ModItems.java:724-727`), all `HEAD`-slot only
  (`isValidArmor` hardcodes `armorType == HEAD`, meaning these 4 items are declared as generic
  `ArmorMaterial.IRON` items but are never meant to be built with plate/legs/boots variants — the
  constructor is reused 4 times with the same equipment slot every time).
- Custom armor model dispatch (client-only, `getArmorModel`): `gas_mask` gets `ModelGasMask`;
  `gas_mask_m65`/`gas_mask_mono`/`gas_mask_olde` all share one `ModelM65` instance (identity-checked
  by `this ==`, i.e. per-item-not-per-instance dispatch since all 4 are separate item instances of
  the same class) — this is the 1.12 `ItemArmor.getArmorModel(EntityLivingBase, ItemStack,
  EntityEquipmentSlot, ModelBiped)` hook; confirmed real 1.21.1 replacement is
  `IClientItemExtensions.getGenericArmorModel(LivingEntity, ItemStack, EquipmentSlot,
  HumanoidModel<?>)` registered via `Item#initializeClient(Consumer<IClientItemExtensions>)` (see
  Key design/API decisions — confirmed by reading Neo Edition's `ArmorNo9.java`, a currently-real,
  compiling use of exactly this pattern for a different single-item helmet).
- `getArmorTexture` dispatches 4 distinct texture paths by exact item identity (falls through to a
  bogus `CapeUnknown.png` default that should never actually be hit given the 4-way `if` chain
  covers every constructed instance — dead default branch, not a bug to preserve meaningfully).
- Client-only `renderHelmetOverlay`: `gas_mask_m65` and `gas_mask` each get a **6-stage damage-scaled
  overlay** — `(int)(itemDamage / maxDamage * 6D)` selects 1 of 6 pre-rendered blur textures
  (`goggleBlur0..5` for m65, `gasmaskBlur0..5` for the plain mask), i.e. the mask visually fogs up
  more as its own durability decreases. `gas_mask_mono`/`gas_mask_olde` get no overlay at all
  (the `if(this != gas_mask && this != gas_mask_m65) return;` guard excludes them). This
  durability-driven visual state is the one piece of this file that is genuinely NBT/Data-Component
  shaped: `stack.getItemDamage()`/`getMaxDamage()` — in 1.21 this maps directly onto
  `DataComponents.DAMAGE`/`DataComponents.MAX_DAMAGE` (vanilla damage bar), no custom component
  needed, since this mask's own wear level (not the *filter's* wear level, a separate concern below)
  really is ordinary item damage.
- `IGasMask` implementation: `getBlacklist` differs per item (`gas_mask_mono` blocks
  `GAS_LUNG`/`GAS_BLISTERING`/`NERVE_AGENT`/`BACTERIA` — full-face-only hazards a half-mask can't
  stop; the other 3 block only `GAS_BLISTERING`/`NERVE_AGENT`). `getFilter`/`installFilter`/
  `damageFilter` all delegate straight to `ArmorUtil` (unported, see Headline #3) — this is the
  **filter storage concern the already-ported `IGasMask.java`'s own javadoc explicitly calls out**:
  "register a `DataComponentType<ItemStack>` (e.g. `hbm:gas_mask_filter`) ... fold filter damage into
  ... a companion `DataComponentType<Integer>`" — i.e. the target shape is already decided by the
  sibling task, this file's job is just to implement `IGasMask` against that already-specified
  component contract once `ArmorUtil`'s NBT-based `installGasMaskFilter`/`getGasMaskFilter` are
  reimplemented in terms of it (not this task's scope to reimplement `ArmorUtil` itself).
- `addInformation` (tooltip) calls `ArmorUtil.addGasMaskTooltip` (unported) then separately lists
  this mask's own hazard blacklist via `I18nUtil.resolveKey` — the blacklist-listing half is
  self-contained and portable today; the filter-status half is blocked.
- `onItemRightClick`: sneak-right-click ejects the installed filter into the player's inventory
  (or drops it if inventory is full) via `ArmorUtil.getFilter`/`removeFilter` — blocked on the same
  `ArmorUtil` gap.

### `ArmorHazmat.java` — live; plain hazmat suit (no special armor properties), 5 material variants

- **12 items** across 3 color variants + 1 material variant (not 4 — this is the widest item count
  of the 7 files): `hazmat_plate/_legs/_boots` (base, `aMatHaz`), `hazmat_plate_red/_legs_red/
  _boots_red` (`aMatHaz2`), `hazmat_plate_grey/_legs_grey/_boots_grey` (`aMatHaz3`),
  `hazmat_paa_plate/_legs/_boots` (`aMatPaa`) — 12 chest/leg/boot pieces total, confirmed at
  `ModItems.java:448-470`. (The matching 4 helmets — `hazmat_helmet`, `hazmat_helmet_red`,
  `hazmat_helmet_grey`, `hazmat_paa_helmet` — are built with the subclass `ArmorHazmatMask` instead,
  covered separately below, bringing the full hazmat-family item count to 16.)
- **No `ISpecialArmor`, no potion effects, no full-set check** — this is the simplest of the 5 live
  files behaviorally. All it does beyond vanilla `ItemArmor`: per-color/per-material texture
  dispatch (8 branches, 2 textures per family: chest+boots share one texture, legs get a second),
  and a client-only helmet-overlay blur (`overlay_hazmat.png`) gated to fire only when the currently
  rendering item *is* `hazmat_helmet` or `hazmat_paa_helmet` — note this guard checks `this !=
  hazmat_helmet && this != hazmat_paa_helmet`, i.e. it deliberately does **not** fire for
  `hazmat_helmet_red`/`_grey` (those get their own render path via the `ArmorHazmatMask` subclass's
  custom model instead, not this overlay).
- The real "hazmat protection" (radiation/gas hazard-class registration, rad-resist multipliers) is
  entirely external to this class — it lives in `ArmorUtil.register()`'s
  `ArmorRegistry.registerHazard(hazmat_helmet, SAND)` etc. calls (helmet-only hazard classes,
  confirmed: none of `hazmat_plate/_legs/_boots` get any hazard-class registration at all — only the
  helmet pieces do, meaning chest/legs/boots contribute zero hazard protection on their own in CE)
  and in `HazmatRegistry`'s `radResist`/`helmet`/`chest`/`legs`/`boots` multiplier table, which in
  turn is fed by `ArmorFSB.setRadResist(...)` for FSB-based armor — **not** by anything in
  `ArmorHazmat.java`/`ArmorHazmatMask.java` themselves, which register no rad-resist value of their
  own. This confirms plain hazmat armor's real protective value in CE comes almost entirely from the
  hazard-class system (`hazmat_protection_integration`'s scope), not from anything in this file.

### `ArmorHazmatMask.java` — live; hazmat helmet variant, extends `ArmorHazmat`, adds `IGasMask`

- **4 items**: `hazmat_helmet` (`aMatHaz`), `hazmat_helmet_red` (`aMatHaz2`), `hazmat_helmet_grey`
  (`aMatHaz3`), `hazmat_paa_helmet` (`aMatPaa`) — all `ModItems.java:448-467`.
- Custom armor model **only** for `hazmat_helmet_red`/`_grey` (`ModelM65`, same model class
  `ArmorGasMask` uses for its own masks) — `hazmat_helmet` and `hazmat_paa_helmet` get no custom
  model, just texture swaps (`super.getArmorTexture`/base-class texture dispatch handles those 2).
- `IGasMask`: `getBlacklist` returns an **empty list** for every item in this class (full protection,
  no hazard the mask itself refuses to filter) — contrast with sibling `ArmorGasMask`, where 3 of 4
  items have a non-empty blacklist. `getFilter`/`installFilter`/`damageFilter`/`onItemRightClick`
  all delegate to the same unported `ArmorUtil` methods as `ArmorGasMask` — identical dependency,
  not a separate one.
- `addInformation` calls `super.addInformation` (parent's texture-only tooltip — parent has none
  beyond vanilla) then `ArmorUtil.addGasMaskTooltip` (blocked).

### `ArmorSchrabidium.java` — **dead code in current CE** (see Headline #1); per-piece potion-effect `ISpecialArmor`

- 4 items share this class's name conceptually (`schrabidium_helmet/_plate/_legs/_boots`) but are
  actually built with `new ArmorFSB(...)` in `ModItems.java:534-541` today, with `_plate/_legs/_boots`
  each calling `.cloneStats((ArmorFSB) schrabidium_helmet)` to copy the helmet's `ArmorFSB` config —
  same migration pattern as Asbestos.
- What this class itself specifies, if it were live: `getRarity` = `RARE`. `getProperties`: always
  returns absorption `(1, 1, 2000)` (i.e. a capped, not infinite, absorb-point pool unlike
  Asbestos/Euphemium's `999999999`), and additionally inflicts 1 self-damage
  (`player.setHealth(player.getHealth() - 1F)`) as a side effect whenever a single hit's `damage >=
  20` — a "still take a little damage on big hits" mechanic distinct from both Asbestos (conditional
  full immunity) and Euphemium (unconditional full immunity). `onArmorTick` grants a different
  vanilla potion per slot every tick (helmet: `NIGHT_VISION` + `WATER_BREATHING`; chest:
  `FIRE_RESISTANCE`; legs: `JUMP_BOOST` amplifier 2; boots: `SPEED` amplifier 2) — **per-piece
  independently**, not full-set-gated (contrast Euphemium, which requires the complete set).
  `damageArmor` multiplies by `1` (ordinary durability loss, same as Asbestos).
- Same caveat as Asbestos applies: this behavior table should be treated as historical reference,
  not live spec, unless whoever researches `ArmorFSB` confirms the schrabidium `ArmorFSB` builder
  call site actually reproduces it (the visible `.cloneStats(...)` calls only copy `ArmorFSB`'s own
  effect-list/step-sound/rad-resist fields between the 4 schrabidium pieces — they say nothing about
  whether `ArmorFSB`'s generic mechanism reproduces this class's *specific* per-slot potion table or
  its capped-absorption/self-damage-on-big-hit behavior at all).

### `MaskOfInfamy.java` — live; trivial cosmetic mask, no dependency beyond the equipment-slot system

- 1 item: `mask_of_infamy` (`ModItems.java:1710`, `ArmorMaterial.IRON`, `HEAD` slot).
- Zero overrides beyond `getArmorTexture` (one hardcoded texture path, `MaskOfInfamy.png`). No
  potion effects, no `ISpecialArmor`, no `IGasMask`, no hazard-class registration found for this item
  anywhere in `ArmorUtil.register()`. This is the one file in this survey that is genuinely
  Phase-3-safe *today* with no named blocking dependency beyond the equipment-slot item shape itself
  (see Phase-3-safe scope).

## Phase-3-safe scope

What's portable now, with exact CE class/item counts:

- **`MaskOfInfamy` → `hbm:mask_of_infamy`, 1 item.** No dependency beyond a working
  `ArmorItem`/`Equippable` registration (the equipment-slot item system itself, which this whole
  Phase 3 armor area is jointly responsible for standing up — see `armor_framework`'s own scope).
  Texture-only; can be built and datagen'd as soon as that base plumbing exists.
- **The registration shells (item construction, materials, rarity, registry names) for all 26 live
  items across `ArmorEuphemium` (4), `ArmorGasMask` (4), `ArmorHazmat` (12), `ArmorHazmatMask` (4) —
  30 items total** can be built as soon as the equipment-slot base is up, independent of whether
  `ArmorUtil`/`ArmorModHandler` exist yet: constructing the `ArmorItem` and registering it does not
  itself require the tooltip/filter/hazard-class machinery to compile, only for the specific
  overridden methods that call into it to have *something* to call (even a temporary
  not-yet-implemented stub, tracked as a TODO, same idiom `docs/phase1` reports used repeatedly for
  this exact "register now, wire behavior later" situation).
- **Euphemium's full-set potion-effect/fall-damage/damage-immunity behavior** is self-contained
  once `ArmorUtil.checkArmor` (a 4-line exact-item-equality helper, not a system) exists — this is
  the cheapest of the "real" special-armor behaviors to finish, and does not depend on
  `hazmat_protection_integration`'s hazard-class work at all (Euphemium's own hazard-class
  registration in `ArmorUtil.register()` is additive flavor, not load-bearing for its
  `ISpecialArmor` behavior).
- **`ArmorHazmat`'s plain texture-dispatch and helmet-overlay logic** (no potion effects, no special
  armor, as established above) — fully portable today, no dependency beyond textures/models existing
  as assets.
- **`ArmorGasMask`/`ArmorHazmatMask`'s hazard-blacklist reporting (`getBlacklist`) and custom armor
  model dispatch** — both self-contained; `getBlacklist` returns a static `List<HazardClass>` per
  item (no `ArmorUtil` call), and the model dispatch is pure client rendering plumbing.

## Deferred scope

What needs another package/phase first, and which one:

- **`com.hbm.handler.ArmorUtil` and `com.hbm.handler.ArmorModHandler` (not yet ported) — blocks the
  filter install/eject/damage/tooltip half of `ArmorGasMask` and `ArmorHazmatMask`, and
  `ArmorEuphemium`/`ArmorHazmat`'s `checkArmor`/`checkForHazmatOnly`-family full-set helpers.** This
  is this task's single largest named dependency. Per the task framing, this belongs to the
  `hazmat_protection_integration` package (already landed `IGasMask`+`ArmorRegistry`, the natural
  next piece of that same system) — not re-researched here, only named as a blocker. Concretely
  blocked by it: `ArmorGasMask.getFilter/installFilter/damageFilter/onItemRightClick/addInformation`,
  `ArmorHazmatMask`'s equivalents, and any full-set check anywhere in this file set once someone
  wants it real (the raw `checkArmor` helper itself is trivial — 4 lines — but living inside the
  unported class).
- **`ArmorAsbestos`'s and `ArmorSchrabidium`'s real current behavior lives in `ArmorFSB`
  (`com.hbm.items.gear.ArmorFSB`, 541 lines), not in the two files this task read.** `ArmorFSB` is
  itself a separate, larger, not-yet-researched Phase 3 file already named in
  `docs/phase1/items_food_gear.md`'s own deferred table ("the big one - full suit bonus fluent-builder
  armor base ... used by dozens of CE armor sets"). Whoever researches `ArmorFSB` needs to treat
  `asbestos_helmet`/`schrabidium_helmet` (and their per-piece `.cloneStats`/`.setOverlay`/
  `.setRadResist` builder calls at each `ModItems.java` declaration site) as part of *that* file's
  scope, not this one's. This report's own read of `ArmorFSB` (undertaken only to confirm Headline
  finding #1, not as a full survey) additionally surfaces that `ArmorFSB` itself depends on:
  `com.hbm.config.PotionConfig` (a config gate on jump-boost specifically), `com.hbm.handler.HazmatRegistry`
  (rad-resist multiplier table, not yet ported), `com.hbm.handler.radiation.ChunkRadiationManager`
  (a Phase 4 system per `docs/phase2/rbmk_reactor.md`'s own headline finding #4 — same cross-phase
  coupling recurring here independently), `com.hbm.util.ContaminationUtil.getActualPlayerRads`
  (hazmat_protection_integration / Phase 4 territory), and `com.hbm.items.armor.IArmorDisableModel`
  (a small interface in the *actual* `com.hbm.items.armor` package, for hiding player model parts —
  e.g. hair/beard under a full hazmat helmet — worth reading alongside `ArmorFSB` itself).
- **Schrabidium's specific per-slot potion-effect table and capped-absorption/self-damage-on-big-hit
  mechanic (as literally written in the dead `ArmorSchrabidium.java`) needs confirmation against
  whatever `ArmorFSB` config the live `schrabidium_helmet`/`_plate`/`_legs`/`_boots` actually use**
  before assuming it's still current behavior — flagged as an explicit open question below rather
  than assumed either way.
- **Asbestos's world-ambient-dust protection is not this file's concern at all** (see Headline #2) —
  it is entirely a `HazardClass.PARTICLE_FINE` registration + `ContaminationUtil.applyAsbestos`
  gate, both squarely `hazmat_protection_integration`/Phase 4 territory. Nothing in
  `ArmorAsbestos.java` (dead or not) needs to change to support or break this; it is a fully separate
  code path this task's 7 files never touch.
- **`HazmatRegistry`'s `radResist`/per-slot-multiplier table** (`helmet=0.2`, `chest=0.4`,
  `legs=0.3`, `boots=0.1` in CE) is referenced by `ArmorFSB.setRadResist` and is the actual mechanism
  behind any "rad resist" stat these armor sets display — not ported yet, not this task's file set,
  named here only because `ArmorHazmat`'s own class contributes zero rad-resist on its own (see
  per-file finding above) and a reader might otherwise assume this file owns that number.
- **Texture/model assets** (`ModelGasMask`, `ModelM65`, all `overlay_*.png`/armor texture PNGs) are
  out of scope for this research report entirely — noted only where they affect behavior dispatch
  (e.g. which items share a model instance).

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior, this port's own committed code and
the Neo Edition reference for NeoForge API shape — no NeoForge API is invented below):

- **`ItemArmor` → `ArmorItem`, constructed with a datapack-backed material, not an enum.** Every
  live 1.21.1 armor item in the Neo Edition reference (`ArmorNo9`) constructs with
  `super(Holder<ArmorMaterial> material, ArmorItem.Type type, Item.Properties properties)`. CE's
  7 constructors here instead take `(ArmorMaterial materialIn, int renderIndexIn,
  EntityEquipmentSlot equipmentSlotIn, String s)` — the `renderIndexIn` int parameter (a 1.12-era
  render-layer index) has no 1.21.1 equivalent and should simply be dropped; `equipmentSlotIn` maps
  onto `ArmorItem.Type` (an enum: `HELMET`/`CHESTPLATE`/`LEGGINGS`/`BOOTS`, not the general
  `EquipmentSlot`); CE's `ArmorMaterial` (a per-mod-material 1.12 class with per-slot defense values
  baked in) needs to become a `Holder<ArmorMaterial>` sourced from a datapack registry entry this
  port defines per CE material (`aMatHaz`, `aMatHaz2`, `aMatHaz3`, `aMatPaa`, `aMatEuph`) — this is
  the same "1.12 enum-like constant → 1.21 datapack registry entry" pattern this port has already
  applied elsewhere (e.g. `com.hbm.damage.ModDamageTypes` for `DamageSource`/`DamageType`); whoever
  owns the `armor_framework` package's overall material registration should settle the exact
  `ResourceKey<ArmorMaterial>` naming convention once, and every armor file (this one included)
  follows it rather than inventing its own.
- **`ISpecialArmor.getProperties`/`damageArmor` (used by `ArmorAsbestos`, `ArmorEuphemium`,
  `ArmorSchrabidium`) has no direct 1.21.1 successor interface. The confirmed-real replacement
  pattern is a central `@SubscribeEvent` listener on `LivingIncomingDamageEvent`**, not a per-item
  override — read in full in Neo Edition's `DamageResistanceHandler.onEntityAttacked`: it inspects
  `event.getSource()`/`getEntity()`/`getOriginalAmount()`, computes a damage-threshold (`dt`) and
  damage-ratio (`dr`) pair by checking the entity's currently-worn armor set/pieces against a
  registry (`itemStats`/`setStats`, keyed by `Item`/4-tuple-of-`Item`), and either
  `event.setCanceled(true)` (dt/dr fully absorbs) or `event.setAmount(reducedAmount)`. This maps
  directly onto all 3 `ISpecialArmor` files here: Asbestos's "immune to fire damage" becomes "check
  `event.getSource().is(DamageTypeTags.IS_FIRE)` (or the specific `ModDamageTypes` fire-adjacent
  entries, if any apply) and cancel"; Euphemium's "immune to everything while full set worn" becomes
  "check the 4-piece exact-item match, cancel unconditionally"; Schrabidium's capped-2000-absorption-
  plus-1-self-damage-on-big-hits becomes a stateful per-item running-total tracked wherever this
  port ends up storing it (a capability field, most likely on the same `HbmLivingAttachment` this
  port already has, or a small dedicated one — not decided here, flagged as open question). This is
  necessarily a **shared, central listener across every `ISpecialArmor`-equivalent set in the whole
  mod** (not just these 3 files), so its actual home almost certainly belongs to whichever package
  ends up owning `ArmorFSB`/the general armor framework, with these 3 sets as three of its many
  registered entries — do not build 3 separate per-item damage-event listeners.
- **Custom per-item armor models (`getArmorModel` in `ArmorGasMask`/`ArmorHazmatMask`) map onto
  `Item#initializeClient(Consumer<IClientItemExtensions>)` overriding
  `IClientItemExtensions#getGenericArmorModel(LivingEntity, ItemStack, EquipmentSlot,
  HumanoidModel<?>)`**, confirmed real and currently used for exactly this purpose by Neo Edition's
  `ArmorNo9` (a single custom-modeled helmet, read in full). The old per-slot `ModelBiped`
  (`ModelGasMask`/`ModelM65`) needs porting to a modern `HumanoidModel<?>` subclass — not attempted
  here, this is rendering-layer work, but the *hook point* is confirmed.
- **`onArmorTick` (Asbestos's `extinguish()`, Euphemium's/Schrabidium's per-tick potion refresh) has
  a confirmed-real modern replacement: `Item#inventoryTick(ItemStack, Level, Entity, int slotId,
  boolean isSelected)`**, checked against `entity.getItemBySlot(EquipmentSlot.X) == stack` to gate
  per-slot logic — confirmed by reading Neo Edition's `ArmorNo9.inventoryTick`, which does exactly
  this pattern (checks `player.getItemBySlot(EquipmentSlot.HEAD) != stack` and returns early
  otherwise) for its own per-tick armor logic. This is a plain `Item` override, not anything
  armor-specific, and requires no new API beyond what vanilla already provides.
- **Filter storage on gas masks is a Data Component, not NBT — already specified, not just
  recommended, by this port's own `IGasMask.java` javadoc**: a `DataComponentType<ItemStack>`
  (e.g. `hbm:gas_mask_filter`) plus a companion `DataComponentType<Integer>` for filter damage,
  replacing CE's `"hfrFilter"` NBT-compound-holding-a-serialized-ItemStack pattern in `ArmorUtil`.
  This report does not re-decide that shape — it is already decided by the sibling task — but flags
  that `ArmorUtil.installGasMaskFilter`/`getGasMaskFilter`/`damageGasMaskFilter` all need to be
  rewritten against it, not translated line-for-line from their current NBT-compound-copy
  implementation.
- **Networking**: none of these 7 files send or receive any packet in CE (no `PacketDispatcher` call
  found in any of them). If the eventual `ArmorFSB`/general-armor-framework research determines a
  packet is needed (e.g. for client-side geiger-sound/HUD state, which `ArmorFSB` does reference),
  it follows `com.hbm.packet.HbmNetwork`'s already-confirmed-real registration shape (one
  `registrar.playToClient(...)`/`playToServer(...)` line per payload, `CustomPacketPayload` +
  `StreamCodec`, registered inside `RegisterPayloadHandlersEvent`) exactly as `BufPacket` already
  does — no new networking pattern needed for anything in this file set specifically.
- **GUI/Menu**: none of these 7 files open a container or menu. `MenuBase`/`GuiInfoContainer` are not
  relevant to this file set at all — noted only to confirm this task's ground-rule instruction to
  check for them was followed, not because they apply here.
- **Explosion/block-removal batching**: not applicable to any of these 7 files — none of them trigger
  world-block removal or explosions. Noted only to confirm the ground rule was checked; this
  performance concern belongs entirely to whichever Phase 3 package owns actual explosive
  content.

## Open questions / risks

- **Does `ArmorFSB`'s generic mechanism actually reproduce `ArmorAsbestos`'s fire-immunity and
  `ArmorSchrabidium`'s per-slot-potion/capped-absorption/self-damage behavior, or did CE's migration
  to `ArmorFSB` silently drop some of it?** This report did not do a full `ArmorFSB` survey (out of
  scope, belongs to that file's own research pass) and could not fully answer this from the builder
  calls visible at the `ModItems.java` declaration sites alone (`.setOverlay(...)`,
  `.cloneStats(...)`, and whatever `.addEffect(...)`/`.setRadResist(...)` calls exist at those same
  declaration sites but weren't transcribed here since they belong to `ArmorFSB`'s own scope). The
  single highest-value thing whoever researches `ArmorFSB` next can do for this task's sake is
  answer this directly: is fire immunity and the capped-schrabidium-absorption mechanic present in
  `ArmorFSB`'s current feature set, or does current CE `asbestos_helmet`/`schrabidium_*` actually
  behave as *plain* full-set-bonus armor now (potion effects + sounds only, no damage
  absorption at all)? Recommend not assuming either answer — read the actual `ModItems.java`
  declaration-site builder-call chains for these 8 items in full as the first step of that research.
- **Where does Schrabidium's running per-hit absorption pool (capped at 2000, if still real) live as
  state?** `ISpecialArmor.ArmorProperties`'s third field (`AbsorbMax`) in 1.12 was managed by vanilla
  Forge's own armor-absorption bookkeeping, not by the mod. There is no equivalent automatic
  bookkeeping in the `LivingIncomingDamageEvent`-based replacement pattern — whoever builds the
  shared special-armor-damage listener (see Key design/API decisions) needs to decide explicitly
  where a per-item or per-player "absorption remaining" counter is tracked (a capability field is
  the natural fit, but this report does not decide whether it belongs on `HbmLivingAttachment` or a
  new dedicated one).
- **Is `MaskOfInfamy` actually obtainable/craftable in CE, or is it debug/admin-only content?** No
  recipe or creative-tab call was found for it in the files read for this survey (its constructor
  never calls `setCreativeTab`, unlike every other file here). Worth a quick recipe-file grep before
  assuming it needs a crafting recipe at all — it may be a give-command-only novelty item, in which
  case the port should preserve that (no recipe), not add one.
- **`ArmorHazmat`'s and `ArmorHazmatMask`'s texture-dispatch `if(stack.getItem().equals(...))` chains
  will need a real replacement strategy once these become datagen'd items** (this port's own
  ground rules call for datagen, not runtime dispatch, for anything that's really just "which model
  variant does this registry entry get"). Since every branch here is keyed to a *distinct registry
  item* (not a metadata/damage-value variant — these are already fully flattened, one `DeferredItem`
  each), the datagen answer is straightforward (one model JSON per item, no dispatch code needed at
  all) — flagged only so the implementer doesn't reflexively port the `if`-chain as executable logic
  when it's actually pure per-item model-selection that datagen removes entirely.
- **Cross-check needed with the parallel `armor_framework` task's final report** (not visible to this
  research pass, per this task's own framing) for whether it already answers any of the above,
  particularly the `ArmorMaterial`/`Holder<ArmorMaterial>` naming convention and the shared
  special-armor-damage-listener's eventual home — this report makes its own confirmed-real API
  findings (Key design/API decisions above) independently in case there's no overlap, but they
  should be reconciled, not duplicated, once both reports exist.
