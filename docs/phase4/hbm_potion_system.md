# CE `HbmPotion` status-effect system — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/potion/{HbmPotion,HbmDetox}.java` (2 files, 178 + 24 =
  202 lines — this is the **entire** CE `com.hbm.potion` package; a directory listing confirms no
  other file exists there)
- Full-repo greps of `upstream/hbm-ce/src/main/java` for every `HbmPotion.<name>` constant
  individually (`taint`/`radiation`/`bang`/`mutation`/`radx`/`lead`/`radaway`/`telekinesis`/
  `phosphorus`/`stability`/`potionsickness`/`death`) to build the real, complete call-site map (48
  matches across 27 files) rather than trusting the 3-4 names already known from this port's own
  TODOs
- Full read of every CE call site found relevant to a real, already-committed dependency of this
  port: `util/ContaminationUtil.java` (`calculateRadiationMod`/`isRadImmune`/`applyDigammaData`,
  ~30 lines around each), `handler/ArmorUtil.java` (`checkForHazmat`/`checkForDigamma`, full
  methods), `handler/HazmatRegistry.java` (`getResistance(EntityLivingBase)`, full method),
  `handler/ability/IWeaponAbility.java` (`PHOSPHORUS` ability, full anonymous class),
  `items/food/ItemPill.java` (full `onFoodEaten`, ~150 lines — every pill's HbmPotion branch),
  `config/VersatileConfig.java` (full, 45 lines — `applyPotionSickness`/`hasPotionSickness`),
  `packet/PermaSyncHandler.java` (the `HbmPotion.death`-synced "boykissers" list, full context) and
  `mixin/MixinRenderPlayerManly.java` (the client render consumer of that list, confirming `death`'s
  real gameplay effect is purely cosmetic), `blocks/gas/{BlockGasRadonTomb,BlockGasMeltdown}.java`
  (full `onEntityCollision`, confirming the exact interactions `docs/phase3/
  contamination_armor_util.md` and `docs/phase1/items_block_fluid_gas.md` already named as blocked
  on this class), `main/ModEventHandler.java` (`potionCheck`/`onFoodEaten` `HbmPotion.death`
  trigger, ~20 lines each), `handler/EntityEffectHandler.java` (`handlePollution`'s lead-poisoning
  tiers, full method — cross-checked against `docs/phase4/pollution_system.md`'s own citation of
  the same lines), `items/armor/PoweredArmorItems`-equivalent CE source (`ModItems.java:647-663`,
  the BJ armor's `radx` grant) and `entity/mob/EntityTaintCrab.java` (confirms it applies taint to
  *victims* only, with no self-immunity override)
- Every already-committed reference to `HbmPotion` in **this port's own tree**, found via
  `grep -rln HbmPotion src/main/java` (19 files, 34 lines total — every one read with surrounding
  context, not just the grep line): `blockentity/turret/TurretMaxwellBlockEntity.java`,
  `blocks/gas/GasBlocks.java`, `blocks/generic/{BarbedWire,PlantBlocks,WasteEarth,WasteSand}.java`,
  `config/VersatileConfig.java`, `handler/{ArmorUtil,HazmatRegistry}.java`,
  `handler/ability/IWeaponAbility.java`, `inventory/fluid/Fluids.java`,
  `items/armor/PoweredArmorItems.java`, `items/food/{FoodItems,ItemAppleSchrabidium,ItemEnergy,
  ItemPill}.java`, `items/gear/WeaponSpecial.java`, `items/weapon/WeaponMeleeItems.java`,
  `util/ContaminationUtil.java`
- This port's own `src/main/java/com/hbm/{capability/HbmLivingProps.java (radiation accessors,
  full), config/{PotionConfig,CompatibilityConfig,GeneralConfig,RadiationConfig}.java (full),
  damage/ModDamageTypes.java (full, 198 lines), sound/ModSounds.java (header + registration
  pattern), main/MainRegistry.java (constructor registration-call list), util/ArmorRegistry.java
  (`HazardClass`/`hasProtection` signatures), blockentity/turret/TurretBaseBlockEntity.java
  (confirmed real `LivingEntity#hasEffect` call site), util/EntityDamageUtil.java (method list)}`
- `upstream/neo-edition/src/main/java/com/hbm/{lib/ModEffect.java, lib/effects/{GenericEffect,
  RadiationEffect,RadawayEffect,TaintEffect,BangEffect,LeadEffect,PhosphorusEffect}.java}` (8
  files, full — Neo Edition's own parallel `MobEffect` port) plus `util/ContaminationUtil.java` and
  `handler/HazmatRegistry.java` (grepped for `ModEffect.`/`hasEffect` call sites only) — **consulted
  only for the real NeoForge 1.21.1 API shape, exactly as instructed; every behavioral/numeric claim
  below is sourced from CE, and every place Neo Edition's own numbers silently diverge from CE is
  called out explicitly so a future agent does not copy them by accident**

## Headline finding

The task's own framing is right that this is the single most cross-referenced missing dependency in
the port so far, but three of its premises need correcting:

1. **`HbmPotion` is not really "a potion system" — it is one `Potion` subclass with 12 singleton
   instances, and 4 of those 12 do nothing at all.** CE's `performEffect`/`isReady` methods are a
   single `if (this == X) ... else if (this == Y) ...` chain covering only 8 of the 12 fields
   (`taint`, `radiation`, `bang`, `lead`, `telekinesis`, `phosphorus`, `potionsickness`, `radaway`).
   The other 4 — **`mutation`, `radx`, `stability`, `death`** — have **no branch in either method**:
   `performEffect` never touches them and `isReady` falls through to `return false` for them, so they
   **never tick and never do anything on their own**. They exist purely as boolean flags other systems
   read via `entity.isPotionActive(HbmPotion.X)` — `mutation` gates full radiation immunity
   (`ContaminationUtil.calculateRadiationMod`/`isRadImmune`), `stability` gates digamma immunity
   (`ContaminationUtil.applyDigammaData`) and hazmat-suit-equivalent status
   (`ArmorUtil.checkForDigamma`), `radx` grants a flat `+0.2F` radiation-resistance bonus
   (`HazmatRegistry.getResistance`), and `death` is a **purely cosmetic joke effect** — CE's own
   constructor call flags it `isBad = false` (i.e. CE itself does not consider it a debuff), and its
   only real consumer, `MixinRenderPlayerManly`, uses a synced "boykissers" id set
   (`PermaSyncHandler`) to swap the affected player's arm-rendering model client-side. It is triggered
   by eating a food item tagged `"ntmRedPill"`, drinking `ModItems.pill_red`, or being hit by the
   Maxwell turret's ungated "5G" upgrade tier — there is no lethal or debuffing consequence baked into
   the effect itself. Porting `HbmPotion` is therefore two much smaller jobs than "port a status-effect
   engine": (a) 8 small `MobEffect` tick behaviors, most under 10 lines each, and (b) 4 no-op
   `MobEffect`s that exist solely so `LivingEntity#hasEffect(...)` calls elsewhere have something to
   check.

2. **The task's own guessed call-site list is partly wrong, and the real list is much longer.**
   `com.hbm.items.gear.ArmorFSB` — named in the task as an expected call site — references
   **nothing** in either CE or this port related to `HbmPotion`; that guess does not hold up. What the
   task did *not* name and a full grep surfaced instead: `config/VersatileConfig.java` (potion
   sickness on every pill, config-gated), `items/food/ItemPill.java` (6 distinct pill branches:
   `pill_iodine` removes `radiation`, `pill_red` grants `death`, `radx`-item grants `radx`,
   `pill_herbal` grants a milk-immune `potionsickness`, `five_htp` grants `stability`),
   `items/armor/PoweredArmorItems.java` (the BJ powered-armor set's `radx` grant),
   `items/food/{FoodItems,ItemAppleSchrabidium}.java` (the `apple_lead_low`/`_mid` items'
   `lead` grant), `items/gear/WeaponSpecial.java` (`lead_gavel`'s `lead` grant on hit),
   `items/weapon/WeaponMeleeItems.java` + `handler/ability/IWeaponAbility.java` (the `mese_gavel`
   sword's `PHOSPHORUS` on-hit ability), and five block-registration files
   (`blocks/gas/GasBlocks.java`, `blocks/generic/{BarbedWire,PlantBlocks,WasteEarth,WasteSand}.java`)
   whose CE originals apply `radiation` on contact and were trimmed to a vanilla stand-in (or nothing)
   pending this exact class. 19 files in this port's own tree already carry a `TODO(HbmPotion)` or
   equivalent comment; none of them is `ArmorFSB`.

3. **The real 1.21.1 `MobEffect` registration API is already confirmed compiling *inside this
   project's reference material* — twice, independently — and this port's own `ModSounds.java`
   already establishes the exact idiom to follow.** Neo Edition's `com.hbm.lib.ModEffect` registers
   all 11 of its ported effects via `DeferredRegister<MobEffect> MOB_EFFECTS =
   DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MODID)`, and this port's own
   `com.hbm.sound.ModSounds` (Phase 0/1, already committed) registers `SoundEvent` — another
   vanilla/built-in (not datapack) registry — via the identical `DeferredRegister.create(
   BuiltInRegistries.SOUND_EVENT, MainRegistry.MODID)` shape, with a `register(IEventBus)` static
   method called once from `MainRegistry`'s constructor. `MobEffect` is a built-in registry exactly
   like `Item`/`Block`/`SoundEvent` (not a datapack registry like `DamageType` — **do not model this
   on `ModDamageTypes`'s `ResourceKey`+`BootstrapContext` shape**, that pattern is for a different
   kind of registry entirely). This is about as low-risk an API-shape confirmation as this port has
   had for any area so far.

4. **Neo Edition's parallel `MobEffect` port is a real, useful API-shape reference but gets CE's own
   numbers wrong in several concrete, checkable ways — a textbook case of why the ground rules say
   "never as a source of behavior."** Full comparison table and worked examples are in Key design/API
   decisions below; the short version: Neo Edition's `LeadEffect` ticks **every** game tick instead of
   CE's real once-per-60-ticks, its `BangEffect` unconditionally instant-kills on the very first tick
   instead of CE's dimension-gated delayed-fuse-in-the-last-10-ticks design, its `RadawayEffect`
   removes a flat `-(amplifier+1)` rads/tick instead of CE's `-(amplifier+1)*0.05F`, four of its
   effect colors don't match CE's real hex values, `potionsickness` is mapped to `NEUTRAL` and
   `death` to `HARMFUL` even though CE's own source flags both `isBad = false`, and CE's 12th potion
   (`telekinesis`) is missing from its roster entirely (though see Open questions — this one has a
   defensible reason).

## Phase-4-safe scope

All 12 fields plus `HbmDetox` are safely portable as this report's own package — nothing here needs a
forward reference to any class this port doesn't already have, except the small, clearly-scoped
`CompatibilityConfig.isWarDim` gap named in Deferred scope (which only gates 3 of the 8 active
effects, and degrades safely if stubbed per the Key design section).

| CE field | `isBad` | Color (hex) | Real tick behavior (CE `performEffect`/`isReady`) | Portability |
|---|---|---|---|---|
| `taint` | harmful | `0x800080` | Every other tick (`duration % 2 == 0`), 1-in-80 chance per check to deal `(amp+1)` `ModDamageTypes.TAINT` damage (already registered) — skipped for `EntityCreeperTainted` (doesn't exist in this port yet, forward-reference TODO only). If `ServerConfig.TAINT_TRAILS` (already ported) is on **and** `CompatibilityConfig.isWarDim` (not ported, see Deferred), spreads `ModBlocks.taint` onto the solid block below. | Fully portable; taint-trail spread branch degrades to "never spreads" until `isWarDim` lands (safe — see Key design). |
| `radiation` | harmful | `0x84C128` | Every tick: `ContaminationUtil.contaminate(entity, RADIATION, CREATIVE, (amp+1)*0.05F)` — already-ported call, one line. | Fully portable today, no gaps. |
| `bang` | harmful | `0x111111` | Only in the last ≤10 ticks of duration (`duration <= 10`). If `isWarDim`: deal `10000*(amp+1)` `ModDamageTypes.BANG` damage (registered) via the *normal* damage path (respects player invulnerability/creative), and for non-players *additionally* force `onDeath`+`setHealth(0)`. Sound + 10-particle burst play **unconditionally**, regardless of dimension. | Fully portable; the lethal branch degrades to "never fires" until `isWarDim` lands (see Key design on why the *default* stub value matters here). |
| `mutation` | beneficial | `0xFF8132` | **No tick behavior at all** (not in either `if` chain). Pure marker: `ContaminationUtil.calculateRadiationMod` returns `0D` immediately if active (full rad-damage immunity), `ContaminationUtil.isRadImmune` also returns `true` if active, `ArmorUtil.checkForHazmat` returns `true` as a fallback if active. | Fully portable as a no-op `MobEffect`; the 3 marker checks are one-line additions to already-committed, already-read methods (exact insertion points identified above). |
| `radx` | beneficial | `0x225900` | **No tick behavior.** Pure marker: `HazmatRegistry.getResistance(EntityLivingBase)` adds a flat `+0.2F` if active. | Same as `mutation` — one-line addition to an already-read, already-committed method. |
| `lead` | harmful | `0x767682` | Every 60th tick (`duration % 60 == 0`): deal `(amp+1)` `ModDamageTypes.LEAD` damage (registered). No dimension gate. | Fully portable, no gaps at all. |
| `radaway` | beneficial | `0xFFE400` | Every tick: `HbmLivingProps`-equivalent `decreaseRads((amp+1)*0.05F)` — this port's `HbmLivingProps.incrementRadiation(entity, -(amp+1)*0.05F)` is the exact already-committed equivalent (verified: it clamps to `[0, MAX_RADS]` the same way CE's `decreaseRads` clamped to `[0, 2500]`). | Fully portable today, no gaps. |
| `telekinesis` | harmful | `0x00F3FF` | Every tick while >1 tick remains: adds a small random `(rand-0.5)*(amp+1)*0.5` impulse to all 3 motion axes. | Fully portable — trivial vanilla motion math, no dependency at all. **Zero real CE call sites exist anywhere in the CE source tree** (verified by grep) — dead/vestigial in CE itself, not just unported (see Open questions). |
| `phosphorus` | harmful | `0xFF3A00` | Every tick, if **not** remote and `isWarDim`: `entity.setFire(amp+1)`. Also the payload of `IWeaponAbility.PHOSPHORUS` (60/90-tick duration by level, amplifier 4) applied by melee weapons with that ability (e.g. `mese_gavel`). | Fully portable; the `isWarDim`-gated fire branch degrades to "never sets fire" until that lands (safe default, see Key design — unlike `bang`/`taint`, getting this stub wrong is low-stakes: worst case is a missing cosmetic fire tick). |
| `stability` | beneficial | `0xD0D0D0` | **No tick behavior.** Pure marker: `ContaminationUtil.applyDigammaData` returns early (no digamma gain) if active; `ArmorUtil.checkForDigamma` returns `true` as a fallback if active. | Same shape as `mutation`/`radx` — one-line addition to two already-committed methods. |
| `potionsickness` | beneficial | `0xFF8080` | Every other tick (`duration % 2 == 0`): 1-in-128 chance to grant vanilla `MobEffects.NAUSEA` for 8s. Also the payload of `VersatileConfig.applyPotionSickness`/`hasPotionSickness` (currently stubbed out in this port pending exactly this class, per its own doc comment). | Fully portable; unblocks `VersatileConfig`'s two stubbed methods immediately (see below). |
| `death` | **beneficial** (`isBad=false` in CE's own source — do not mark this `HARMFUL`) | `0x111111` | **No tick behavior.** Pure marker, purely cosmetic: no damage, no debuff of any kind. Its only real consumer (`MixinRenderPlayerManly` + `PermaSyncHandler`'s synced id set) is a client-side player-model swap — Phase 5 (Client & UX) scope, not needed for this report's own correctness. | Fully portable as a no-op `MobEffect`; the render-swap consumer is out of scope here (see Deferred scope). |
| `HbmDetox` (not a `Potion`, a bare utility class) | n/a | n/a | `blacklistedPotions`: a `HashSet<Potion>` built once from `PotionConfig.potionBlacklist()` (**already ported**, default value `["srparasites:coth", "srparasites:viral"]` — a *modded*-disease-compat blacklist, not a vanilla-potion one). `isBlacklisted(Potion)` is a pure set-membership check. CE's only consumer is `ModEventHandler.potionCheck`, a `PotionApplicableEvent` listener that vetoes applying a blacklisted potion when the target is wearing a full hazmat set (`ArmorUtil.checkForHazmat`) **and** has `HazardClass.BACTERIA` head protection (`ArmorRegistry.hasProtection`, already ported), damaging the gas-mask filter by 10 as a side effect (`ArmorUtil.damageGasMaskFilter`, already ported). | **Fully portable today** — every dependency (`PotionConfig`, `ArmorUtil.checkForHazmat`/`damageGasMaskFilter`, `ArmorRegistry.hasProtection`+`HazardClass.BACTERIA`) already exists and compiles in this port. Only the listener event name needs confirming (see Open questions). |

**Already-committed consumers this report's classes directly unblock** (all read above, all get a
small, mechanical follow-up edit once `HbmPotion`'s equivalent lands — none of these files are this
report's own responsibility to edit, listed here only so the implementer knows exactly where the 3
one-line insertions and 2 method-body fill-ins go):

| File | Exact TODO | Fix once ported |
|---|---|---|
| `util/ContaminationUtil.java:63-64,359-360` | `calculateRadiationMod`/`isRadImmune` `mutation` short-circuit | `if (entity instanceof LivingEntity le && le.hasEffect(HbmPotionEffects.MUTATION)) return 0D;` (and `return true;` in `isRadImmune`) |
| `util/ContaminationUtil.java:431-432` | `applyDigammaData` `stability` short-circuit | `if (entity.hasEffect(HbmPotionEffects.STABILITY)) return;` |
| `handler/ArmorUtil.java:236-237,324-325` | `checkForHazmat`/`checkForDigamma` fallbacks | `return entity.hasEffect(HbmPotionEffects.MUTATION);` / `return player.hasEffect(HbmPotionEffects.STABILITY);` |
| `handler/HazmatRegistry.java:122-124` | `getResistance` `radx` bonus | `if (entity.hasEffect(HbmPotionEffects.RADX)) res += 0.2F;` |
| `config/VersatileConfig.java` | Whole class doc comment says "not ported" | Add back `applyPotionSickness(LivingEntity, int)`/`hasPotionSickness(LivingEntity)` exactly as CE has them, reading `PotionConfig.potionSicknessMode()` (already ported) for the `×12` Terraria-mode multiplier |
| `handler/ability/IWeaponAbility.java` | `PHOSPHORUS` ability entirely missing | Add the singleton per CE's exact shape (60/90-tick-by-level duration table, amplifier 4), register in `abilities[]` |
| `items/food/ItemPill.java` | 6 TODO comments, one per pill branch | Wire in `radiation`-removal, `death`/`radx`/`stability` grants, and the milk-immune `potionsickness` grant exactly as tabulated above |
| `items/armor/PoweredArmorItems.java` (BJ set) | 1 TODO | Add `.addEffect(...)`-equivalent `radx` grant per CE `ModItems.java:647-663` |
| `items/food/FoodItems.java` + `ItemAppleSchrabidium.java` | 2 TODOs | `apple_lead_low`/`_mid` grant `lead` (15×20 ticks amp 2 / 60×20 ticks amp 4) |
| `items/gear/WeaponSpecial.java` | 1 TODO | `lead_gavel` grants `lead` (15×20 ticks amp 4) on hit |
| `items/weapon/WeaponMeleeItems.java` | 1 TODO | `mese_gavel` adds `IWeaponAbility.PHOSPHORUS` at level 0 |

## Deferred scope

- **`com.hbm.config.CompatibilityConfig.isWarDim(Level)`** — **not ported** (confirmed: this port's
  own `CompatibilityConfig.java` doc comment explicitly says so, since it's keyed by CE's
  now-meaningless integer Forge dimension ID). This gates 3 of the 8 active effects: `taint`'s
  trail-spread branch, `bang`'s entire lethal branch, and `phosphorus`'s fire-setting branch. This is
  the one genuine forward reference in this whole area, and it belongs to whichever Phase 4 sub-area
  ends up owning dimension/world-gen registration (the same area `docs/phase0/STATUS.md` and
  `docs/phase4/chunk_radiation_system.md` already point dimension-keyed config at). **Read CE's real
  default carefully before stubbing this** — see Key design decisions; the safe default is the
  opposite of what every other forward-reference stub in this port defaults to.
- **`EntityCreeperTainted`/`EntityQuackos`** — two HBM-custom mobs CE's `taint` performEffect and
  `ContaminationUtil.isRadImmune` check for by `instanceof`; neither exists in this port yet (already
  a documented forward reference in `ContaminationUtil.java`'s own TODO for the second one). Belongs
  to whichever phase/area researches those mobs (Phase 4's own mob-content areas, per the task's
  framing of legacy-gun-consuming mobs living in this same phase). Until then, `taint`'s "don't damage
  the tainted creeper with its own aura" exclusion simply never triggers (harmless — the mob doesn't
  exist to be excluded from anything yet).
- **`PermaSyncHandler`/`MixinRenderPlayerManly`** (`death`'s only real consumer) — a synced
  per-player id set plus a client-side player-render-model mixin swap. This is Phase 5 ("Client &
  UX") scope per the project's phase split (rendering/mixin work, not world-simulation logic); `death`
  itself needs zero server-side logic beyond existing as a registered no-op `MobEffect`, so nothing in
  this report is blocked by that consumer not existing yet.
- **`ModEventHandler`'s `HarvestDropsEvent` lead-poisoning-on-ore-break listener** and
  **`EntityEffectHandler.handlePollution`'s ambient `lead` tiers** — both already fully researched by
  `docs/phase4/pollution_system.md` (its own Deferred scope section names `HbmPotion.lead` as the one
  thing blocking both hooks, with exact amplifier tiers cited). That report's own package (pollution)
  owns wiring those two call sites once this report's `lead` effect exists; this report does not need
  to touch `EntityEffectHandler.java` or `ModEventHandler.java` itself.
- **`blocks/gas/{BlockGasRadonTomb,BlockGasMeltdown}.java`** (and the other 5 unregistered
  `blocks/gas` leaves) — already named as blocked on this exact class by both
  `docs/phase1/items_block_fluid_gas.md` and `docs/phase3/contamination_armor_util.md`. Read in full
  here to confirm the exact interaction: `BlockGasRadonTomb` **removes** `radaway`/`radx` on contact
  without hazmat protection ("get fucked" — CE's own comment) while still contaminating the entity;
  `BlockGasMeltdown` **grants** `radiation` (60×20 ticks, amp 2) plus asbestos on contact. Whichever
  area registers those two blocks (the gas-blocks/fluid area, not this one) owns wiring these calls in
  once this report's `radaway`/`radx`/`radiation` effects exist — no action needed here beyond having
  those effects registered.
- **`blocks/generic/{BarbedWire,PlantBlocks,WasteEarth,WasteSand}.java`, `blocks/gas/GasBlocks.java`**
  — 5 already-committed Phase 1/2/3 files whose CE originals apply `HbmPotion.radiation` on contact
  (exact tiers found by the full CE-wide grep: `BarbedWire`'s `ULTRADEATH` wire = 5×20 ticks amp 9,
  `WasteEarth`'s `waste_mycelium` = 30×20 ticks amp 29, `WasteSand`'s `waste_trinitite` = 1×20 tick
  amp 49) and were trimmed to a vanilla stand-in or dropped entirely, each with its own inline
  comment naming `HbmPotion` as the reason. This report supplies the class; **going back and
  restoring the exact CE radiation-potion call in each of those 5 already-merged files is a small
  follow-up edit for whichever area owns them**, not part of this report's own deliverable.
- **`ConsumableHandler`/`EntityEffectHandler`/`GunEnergyFactory`/`ItemModAuto`/`BlockTaint`/
  `Balefire`/`BlockHazard`/`BlockSellafield`/`BlockFallout`/`ExplosionChaos`/`GlyphidStats`/
  `EntityBulletBase(NT)`/`EntityModBeam`/`ItemAmmo{HIMARS,Arty}`** — 14 further CE files reference
  `HbmPotion` constants (found by the full CE-wide grep) but **do not exist anywhere in this port
  yet**; each belongs to whichever phase/area first ports that specific class (consumables, taint
  bomb, balefire, hazard/sellafield/fallout blocks, the Xen explosion system, Glyphid mobs, the
  legacy bullet system, HIMARS/artillery ammo). None of them are a blocker on this report — they are
  simply future consumers, listed here so a future agent searching for "who else needs HbmPotion"
  finds this table instead of re-running the grep.
- **`com.hbm.inventory.fluid.Fluids.java`'s `ESTRADIOL` toxin trait** — CE wires `HbmPotion.death`
  (60×60×20 ticks, amp 0) onto this fluid's `FT_Toxin` trait; this port's `Fluids.java` already has an
  inline comment naming `HbmPotion` (among 2 other classes) as the reason that wiring is deferred.
  Small follow-up for whichever area owns the fluid-trait assignment pass once this report's effects
  exist.
- **Client-side potion icon rendering** (CE's `HbmPotion.getStatusIconIndex()` override, which binds a
  custom `textures/gui/potions.png` atlas) — 1.12.2-era `Potion` rendering API with no attempt made
  here to find its exact 1.21.1 replacement (modern `MobEffect` icons are ordinary
  `textures/mob_effect/<id>.png` sprites, a data/asset concern). Phase 5 ("Client & UX") scope, not
  needed for any of this report's server-side behavior.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and
Neo Edition's parallel effect port for NeoForge API shape only):

- **Registration shape, doubly confirmed real**: `DeferredRegister<MobEffect> MOB_EFFECTS =
  DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, MainRegistry.MODID);`, one
  `DeferredHolder<MobEffect, ?>` per effect via `MOB_EFFECTS.register("name", () -> new
  XxxEffect(MobEffectCategory, colorInt))`, and a `register(IEventBus)` static method called once
  from `MainRegistry`'s constructor (following the exact `HBMSoundHandler.register(modEventBus);
  ModSounds.register(modEventBus);` pattern already in that constructor's call list — this report's
  registrar is one more line in that same list). Confirmed twice independently: Neo Edition's
  `com.hbm.lib.ModEffect` (a different project, same target API) and this port's own already-merged
  `com.hbm.sound.ModSounds` (a different registry, same `BuiltInRegistries.X`/`DeferredRegister`
  shape, same "why not `ModDamageTypes`'s pattern" distinction already documented in that file).
  `MobEffect`'s constructor takes exactly `(MobEffectCategory, int color)`, confirmed by both Neo
  Edition source files read and by the fact CE's own `Potion(boolean isBad, int color)` constructor
  is the direct analogue (`isBad=true` → `MobEffectCategory.HARMFUL`, `isBad=false` →
  `MobEffectCategory.BENEFICIAL` — CE's model is strictly binary, `NEUTRAL` has no CE equivalent and
  should not be introduced without a real reason; see the `potionsickness` divergence below for why
  Neo Edition's choice there should not be copied uncritically).
- **`LivingEntity#hasEffect(Holder<MobEffect>)` / `#getEffect(Holder<MobEffect>)` /
  `#addEffect(MobEffectInstance)` / `#removeEffect(Holder<MobEffect>)` is the confirmed real 1.21.1
  replacement for CE's `EntityLivingBase#isPotionActive`/`getActivePotionEffect`/`addPotionEffect`/
  `removePotionEffect`.** Confirmed **three separate ways**: this port's own already-committed
  `TurretBaseBlockEntity.java:591` (`living.hasEffect(MobEffects.INVISIBILITY)`), this port's own
  `ItemPill.java`/`ArmorFSB.java`/`FT_Toxin.java` (`addEffect(new MobEffectInstance(...))`,
  `removeEffect(MobEffects.X)`, `getEffect(effect.getEffect())`), and Neo Edition's own
  `ContaminationUtil.java`/`HazmatRegistry.java` (`living.hasEffect(ModEffect.MUTATION)`,
  `player.hasEffect(ModEffect.RADX)`) — passing a `DeferredHolder<MobEffect, ?>` **directly**, with no
  `.get()` call, exactly the way vanilla's own `MobEffects.X` constants (themselves `Holder<MobEffect>`)
  are already used everywhere in this port. This means every one of the "one-line insertion" fixes in
  the Phase-4-safe-scope table above is exactly as simple as it looks: `entity.hasEffect(HbmPotionEffects.MUTATION)`.
- **One class with `this == X` branching (CE's real shape) vs. one `MobEffect` subclass per effect
  (Neo Edition's shape) — recommend the latter, and this port has already made the equivalent
  decision elsewhere.** CE's `HbmPotion` is a single class serving all 12 singletons, dispatching on
  reference equality inside one shared `performEffect`/`isReady`. Neo Edition instead gave each active
  effect its own small class (`TaintEffect`, `RadiationEffect`, `BangEffect`, `LeadEffect`,
  `RadawayEffect`, `PhosphorusEffect`, plus a shared `GenericEffect` no-op for the 4 markers) —
  confirmed to compile against the real 1.21.1 `MobEffect` API (`applyEffectTick`/
  `shouldApplyEffectTickThisTick` are per-instance overridable methods, not a shared dispatch point
  the way CE's `Potion.performEffect(EntityLivingBase, int)` was, so the "one class" shape is not even
  a more direct translation — it would need its own internal `this == X` re-derivation just to know
  which constant it is). This port's own `items/armor/PoweredArmorItems.java` (read in this survey)
  already made the identical call for a structurally similar CE original — CE's own armor classes here
  are less unified than the potion case, but the general precedent ("prefer one small class per
  concept over one union class with identity checks," visible throughout this port's `Armor{HEV,BJ,
  RPA,...}` split) points the same direction. Recommend: `com.hbm.potion.HbmPotionEffects` (registrar,
  in the `Mod*`-registry naming family alongside `ModSounds`/`ModDamageTypes`, but the package stays
  CE's real `com.hbm.potion`, not Neo Edition's invented `com.hbm.lib`/`com.hbm.lib.effects`) holding
  8 small effect classes + 1 shared no-op class for the 4 markers.
- **`CompatibilityConfig.isWarDim`'s CE default is "true for every dimension," not "false" — get the
  stub direction right.** CE's real implementation: `peaceDimensionsIsWhitelist` defaults to `true`
  and `peaceDimensions` defaults to an **empty** set, so `isWarDim` = `!peaceDimensions.contains(dimID)`
  = `true` for literally every dimension unless a server operator explicitly opts one out. This is the
  opposite of the safe-default pattern this port has used for every other forward-reference stub so
  far (e.g. `ChunkRadiationManager`'s ambient-radiation query stubs to `0`, the "nothing happens yet"
  direction). Stubbing `isWarDim` to `false` — the instinctive "safe until real" choice — would
  silently disable `taint`'s trail-spread, `bang`'s entire lethal payload, and `phosphorus`'s
  fire-setting in every world, which is **not** CE's real default behavior. The CE-faithful stub is
  `true` (i.e. "assume every dimension is a war dimension until real per-dimension config exists"),
  matching this port's own general principle of reproducing CE's default runtime behavior rather than
  the most conservative one.
- **`radaway`'s decrement wraps this port's already-committed `HbmLivingProps.incrementRadiation`,
  verified clamp-compatible.** CE's `EntityHbmProps#decreaseRads(double)` clamps to `[0, 2500]`; this
  port's `HbmLivingAttachment#increaseRads`/`HbmLivingProps.incrementRadiation` (read in full) already
  clamps every write to `[0, MAX_RADS]` (that class's own constant, replacing CE's hardcoded `2500`
  literal), so calling `HbmLivingProps.incrementRadiation(entity, -(amp+1)*0.05F)` reproduces CE's
  `decreaseRads((amp+1)*0.05F)` exactly, clamp included, with no new code needed in that class.
- **Neo Edition numeric/behavioral divergences — confirmed by direct comparison against CE's real
  source, do not carry any of these into this port:**
  - `LeadEffect.applyEffectTick` fires every game tick (`shouldApplyEffectTickThisTick` always
    `true`) — CE's real cadence is once every 60 ticks (`duration % 60 == 0`). Copying Neo Edition's
    cadence here would make lead poisoning **60× more frequent** than real CE balance.
  - `BangEffect.applyEffectTick` unconditionally deals `Float.MAX_VALUE` damage and calls
    `entity.kill()` on the very first tick, with no dimension gate and no player/non-player
    distinction. CE's real design only becomes lethal in the **last ≤10 ticks** of the effect's
    duration (a delayed-fuse "you have a few seconds' warning" mechanic), only inside a war
    dimension, deals a large-but-finite `10000*(amp+1)` through the normal (invulnerability-respecting)
    damage path for everyone, and only *additionally* force-kills non-player entities via
    `onDeath`+`setHealth(0)`. `Entity#kill()` in modern Minecraft bypasses invulnerability/creative
    checks entirely, so Neo Edition's version would even instant-kill a creative-mode player — a
    behavior CE never has.
  - `RadawayEffect` removes a flat `-(amplifier+1)` rads per tick; CE's real formula is
    `-(amplifier+1)*0.05F` per tick (matching `radiation`'s own `+(amplifier+1)*0.05F` grant formula
    exactly — the two are meant to be symmetric). Copying Neo Edition's numbers here would make
    `radaway` **20× stronger** than real CE balance.
  - `PhosphorusEffect.applyEffectTick` sets fire unconditionally every tick with no `isWarDim` gate;
    CE requires the dimension check (see the `isWarDim` decision above — lower-stakes than the
    other two since the CE-real default is "war dim everywhere anyway," but still a real gap once a
    server actually configures a peace dimension).
  - `TaintEffect` uses a `1/40` roll on **every** tick; CE uses a `1/80` roll only on every *other*
    tick (net effect: CE's real expected damage rate is **4× lower** than Neo Edition's).
  - Color mismatches vs. CE's real hex values (verified by direct decimal→hex conversion of CE's own
    `preinit()` literals): Neo Edition's `mutation` (`0x800080`, CE is `0xFF8132`), `radx`
    (`0xBB4B00`, CE is `0x225900`), `radaway` (`0xBB4B00`, CE is `0xFFE400`), `phosphorus`
    (`0xFFFF00`, CE is `0xFF3A00`) all diverge; `taint`/`radiation`/`bang`/`lead`/`stability`/
    `potionsickness`/`death` all happen to match CE exactly.
  - `MobEffectCategory` mismatches vs. CE's own `isBad` flag: Neo Edition maps `potionsickness` to
    `NEUTRAL` (CE flags it `isBad=false`, i.e. `BENEFICIAL` under CE's strict binary model — `NEUTRAL`
    is a defensible *interpretation* given nausea is a mild debuff, but is not what CE's own source
    says) and, more clearly wrong, maps `death` to `HARMFUL` when **CE's own constructor call passes
    `isBad=false`** for it — CE itself does not consider this a harmful effect (consistent with it
    being a pure cosmetic joke, not a debuff). Recommend `BENEFICIAL` for `death` to match CE's real
    flag exactly.
  - `telekinesis` is entirely absent from Neo Edition's 11-effect roster (CE has 12). See Open
    questions for whether this is actually the right call to make here too.
- **Neo Edition's own `ContaminationUtil.calculateRadiationMod` is a simplified, non-parity
  reimplementation — do not use it as a cross-check for this port's own (already more faithful)
  version.** Neo Edition's version only applies resistance to `Player` instances (returns a flat `1`
  — no resistance at all — for every other `LivingEntity`, silently dropping CE's real per-mob-type
  resistance) and omits `getConfigEntityRadResistance` entirely. This port's own already-committed
  `ContaminationUtil.calculateRadiationMod(LivingEntity)` (read in full in this survey) is already
  closer to CE's real formula than Neo Edition's own file is — it just needs the one `mutation`
  short-circuit line added, per the table above.

## Open questions / risks

- **Is `telekinesis` worth registering at all, given it has zero real CE call sites?** Verified by
  grepping the entire CE source tree for `HbmPotion.telekinesis` outside the definition file itself —
  nothing calls it anywhere in CE. It reads like dead/reserved content (possibly a leftover from a
  removed mechanic, or a debug/command-only effect never wired to a real trigger). Neo Edition
  dropped it, and for once that omission looks like a reasonable call rather than an oversight.
  Recommend porting it anyway for roster completeness (CE's own `registerPotions` registers all 12
  unconditionally, and this port's general practice elsewhere has been to preserve full CE inventories
  even for content with no current consumer, e.g. registered-but-unused items), but flagging this
  explicitly rather than silently matching Neo Edition's smaller roster without comment.
- **The exact NeoForge 1.21.1 event name/shape for CE's `PotionApplicableEvent`
  (`HbmDetox`/`ModEventHandler.potionCheck`'s veto-a-potion-application hook) is not confirmed against
  a real jar or against either reference repo** — neither this port nor Neo Edition has ported that
  listener yet. The likely real name, based on well-established NeoForge API knowledge **not verified
  in this sandbox** (no compiled jar available, network blocked), is
  `net.neoforged.neoforge.event.entity.living.MobEffectEvent.Applicable`, fired on the game event bus
  and cancellable/settable similarly to Forge 1.12's `Result`-based `PotionApplicableEvent`. This
  port's own `@EventBusSubscriber(modid = MainRegistry.MODID)` idiom (confirmed real, defaults to
  `Bus.GAME`, already used by `handler.neutron.NeutronHandler`/`handler.ArmorDamageHandler`, both read
  in this survey) is the right shape for the listener class itself once the event name is confirmed —
  this is the one piece of this report's design that should be double-checked against a real compiled
  NeoForge jar before implementation, rather than trusted outright.
- **CE's per-`PotionEffect`-instance `setCurativeItems(List<ItemStack>)` override (used exactly twice:
  `BlockTaint`'s taint application and `ItemPill`'s `pill_herbal` potionsickness grant, both making the
  effect un-milk-curable) has no confirmed 1.21.1 equivalent in this survey.** Modern
  `MobEffectInstance`/NeoForge expose curability via `EffectCure` and a `MobEffect#fillEffectCures`
  override (seen once, as an empty override, in Neo Edition's `TaintEffect.java`) rather than a mutable
  per-instance item list — but whether a *specific application* can override the *class-level* cure set
  (as CE's per-instance call did) versus only the whole effect class always being cure-immune is not
  confirmed here. This only affects 2 of the ~15 CE call sites this report's classes eventually need
  to support, both outside this report's own committed-file scope (`BlockTaint`, `ItemPill` — the
  latter is this port's own file and already has a TODO for this exact branch), so it does not block
  registering the effects themselves, only those two specific curative-immunity behaviors.
- **Neo Edition's `TaintEffect` adds an extra `EntityTaintCrab` self-immunity check
  (`!(entity instanceof EntityTaintCrab)`) that has no basis in CE's real source** — CE's own
  `EntityTaintCrab.java` (read in full) only ever *applies* taint to other entities via
  `e.addPotionEffect(...)`, it never receives the effect itself, so no self-exemption is needed or
  present in CE. This is a small, harmless-looking addition in Neo Edition's file, but it is still an
  invented behavior not present in the source of truth — another concrete instance of "don't copy Neo
  Edition's logic," even where the addition looks intuitively reasonable.
- **`isWarDim`'s eventual real implementation is out of this report's scope, but this report's `taint`/
  `bang`/`phosphorus` effects need to call *something* named that in the meantime.** Recommend a
  single `private static boolean isWarDim(Level level) { return true; }` local helper inside whichever
  effect classes need it (matching the CE-faithful default established above), with a `TODO` pointing
  at `CompatibilityConfig` for whoever ports the real dimension-keyed version — rather than 3 separate
  inline `true` literals, so there is exactly one place to swap in the real implementation later.
