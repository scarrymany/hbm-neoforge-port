# Melee weapons — Phase 3 research

Scope: the melee-weapon content deferred out of Phase 1's `items/tool` and `items/gear` surveys —
`ItemSwordAbility`/`ItemSwordAbilityPower`/`ItemSwordMeteorite`, the `ItemMultitoolTool`/
`ItemMultitoolPassive` upgrade ladder, `ItemBoltgun`, and `WeaponSpecial`'s four still-deferred
branches.

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/{ItemSwordAbility,ItemSwordAbilityPower,
  ItemSwordMeteorite,ItemMultitoolTool,ItemMultitoolPassive,ItemBoltgun}.java`
- `upstream/hbm-ce/src/main/java/com/hbm/handler/ability/IWeaponAbility.java` (all 8 concrete
  abilities + `NONE`)
- `upstream/hbm-ce/src/main/java/com/hbm/util/EntityDamageUtil.java` (for
  `attackEntityFromIgnoreIFrame`'s exact semantics, the one helper `ItemBoltgun` actually calls)
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityRubble.java` (header +
  `onImpact`) and the relevant slices of `com.hbm.explosion.ExplosionChaos`
  (`levelDown`/`decontaminate`, ~90 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/main/AdvancementManager.java` (header/field-list, to
  confirm its shape as a static `Advancement`-handle registry + `grantAchievement` trigger wrapper)
- Grepped `upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java` for every
  `new ItemSwordAbility(...)`/`ItemSwordAbilityPower(...)`/`ItemSwordMeteorite(...)` call site and
  its chained `.addAbility(...)` calls (23 sites total), plus the single `ItemBoltgun` site
- This port's own `src/main/java/com/hbm/{handler/ability/*, items/tool/{ToolItems,
  ItemMultitoolTool,ItemToolAbilityPower,ToolTiers}.java, api/energymk2/IBatteryItem.java,
  damage/{ModDamageTypes,tags/ModDamageTypeTags}.java, packet/HbmNetwork.java,
  capability/HbmLivingAttachment.java, entity/ConveyorEntityTypes.java,
  hazard/type/HazardTypeUnstable.java, items/gear/WeaponSpecial.java,
  config/CompatibilityConfig.java, lib/Library.java}` — all read in full or grepped for the
  specific symbols this area's dependencies name
- `docs/phase1/{items_tool.md, items_food_gear.md}` (both already named these exact files as
  Phase 3 scope; this report re-derives the actual dependency shape rather than repeating the
  one-line mentions), `docs/phase2/rbmk_reactor.md` (structural model + the "pre-existing compile
  break" framing this report reuses in its own headline finding #5)
- `upstream/neo-edition/src/main/java/com/hbm/items/tools/{SwordAbilityItem,SpecialSwordItem}.java`
  — cross-referenced for confirmed real 1.21.1 attribute-modifier/`hurtEnemy` API shape only, not
  for behavior (Neo Edition's `SwordAbilityItem` has no on-hit ability content at all — CE is the
  sole source of truth for what the abilities themselves do)

## Headline findings

1. **The ability-hook sword framework itself is not the blocker — the 8 concrete abilities are.**
   `com.hbm.handler.ability.{IBaseAbility,IWeaponAbility,AvailableAbilities}` already exist in this
   port (ported during the Phase 1 mining-tool-ability work) and are correct and complete for
   weapon use: `AvailableAbilities.getWeaponAbilities()`, `appendHoverText`, and the
   `IWeaponAbility` contract all already work exactly as CE's swords need. The *only* thing missing
   is the 8 concrete `IWeaponAbility` singletons (`RADIATION`, `VAMPIRE`, `STUN`, `PHOSPHORUS`,
   `FIRE`, `CHAINSAW`, `BEHEADER`, `BOBBLE`) — the ported `IWeaponAbility.java`'s own javadoc
   already says as much. Read individually (not read in Phase 1, read here for the first time),
   these 8 are **not one monolithic blocker**: 4 of them (`STUN`, `BEHEADER`, `VAMPIRE`, and `FIRE`)
   have **zero dependency on anything unported** — pure vanilla `MobEffect`/item-drop/`setFire`
   logic — and can be added to `IWeaponAbility` today. Only `RADIATION` (needs the
   `ContaminationUtil`-equivalent facade), `PHOSPHORUS` (needs a custom `HbmPotion` `phosphorus`
   `MobEffect`), `CHAINSAW` (needs `ModItems.nitra_small` plus a particle-burst network packet),
   and `BOBBLE` (needs `ModBlocks.bobblehead`, itself unported) are genuinely blocked, and each on a
   different, narrow thing.
2. **23 sword registry entries across 3 classes, zero metadata-flattening needed** (each is already
   its own registry object in CE, matching Phase 1's `items_tool.md` finding for the mining-tool
   side of the same file family). `ItemSwordAbility` direct instances: `titanium_sword`,
   `steel_sword`, `alloy_sword` (`@Deprecated`, still instantiated), `desh_sword`, `cobalt_sword`,
   `cobalt_decorated_sword`, `starmetal_sword`, `cmb_sword`, `schrabidium_sword`, `mese_gavel`,
   `dnt_sword` (11). `ItemSwordAbilityPower`: `elec_sword` (1). `ItemSwordMeteorite`: 11 tiers
   (`meteorite_sword`, `_seared`, `_reforged`, `_hardened`, `_alloyed`, `_machined`, `_treated`,
   `_etched`, `_bred`, `_irradiated`, `_fused`, `_baleful`). Total: **23**.
3. **The multitool ladder is 8 independent small features sharing one `Item` subclass and one
   sneak-click state machine, not one Phase 3 chunk.** Phase 1 ported rungs 1–2
   (`multitool_dig`/`multitool_silk`, `ItemMultitoolTool extends TieredItem`, correctly
   scoped alone since CE's own class boundary is real: `ItemMultitoolTool extends ItemTool` while
   `ItemMultitoolPassive extends Item` directly — **no shared material/tier concept at all** past
   rung 2). The remaining 8 rungs (`multitool_ext` → `multitool_decon`) span: a vanilla-furnace-
   recipe instant-smelt tool (no CE-specific dependency), two projectile-entity shooters
   (`EntityMinerBeam`/`EntityLaserBeam`, neither ported), a pure-combat-stats melee weapon with zero
   external dependency, an AoE-lightning item with zero external dependency, a dimension-gated
   terrain-leveling explosion utility, a raycast block-to-rubble utility, and a single-block
   wasteland-decontamination utility. Each rung's dependency is independent — this should be
   implemented and reviewed rung-by-rung, not as one unit.
4. **`ItemBoltgun` is not a ranged/projectile weapon.** Phase 1's `items_tool.md` correctly
   corrected the "nail gun" mislabel but its own replacement label ("ranged weapon with a
   construction-tool skin") is itself imprecise once the actual method is read: the 10-armor-piercing-
   damage attack fires from `onLeftClickEntity` — vanilla's melee/adjacent-attack hook, the same one
   `ItemSword` uses — with **no projectile, no travel, no raycast of its own**. It only fires when
   the player left-clicks (attacks) an entity already within normal melee reach. The correct
   classification is: **a melee-range, ammo-consuming, armor-piercing power-attack item** with a
   VFX-only "ranged weapon" *feel* (explosion-burst particle on hit), not an actual ranged weapon.
   Its `onItemUse` (right-click on a block) is genuinely the construction-tool skin — `IToolable`/
   `NTMToolHandler` bolt-driving conversions — and that half is correctly already bucketed as Phase
   2 machine coupling by `items_tool.md`'s own sibling entry for `ItemTooling`. **Only
   `onLeftClickEntity` is this area's scope**; nothing about `onItemUse` needs re-deciding here.
5. **A live, pre-existing compile break shares this area's exact two entity dependencies.**
   `src/main/java/com/hbm/hazard/type/HazardTypeUnstable.java` already `import`s and calls
   `com.hbm.entity.logic.EntityNukeExplosionMK5.statFac(...)` and
   `com.hbm.entity.effect.EntityNukeTorex.statFac(...)` (four call sites) — but **neither class
   exists anywhere in this port**, confirmed by `Glob`/`Grep` across `src/main/java`. This is the
   exact same "already-committed forward reference now blocking real code" pattern
   `docs/phase2/rbmk_reactor.md` flagged for `ItemRBMKRod`. `WeaponSpecial`'s `memespoon` branch
   (deferred per `docs/phase1/items_food_gear.md`) names these same two classes as its own blocker
   — so whoever ports `EntityNukeExplosionMK5`/`EntityNukeTorex` for either reason fixes both at
   once. Flagging loudly per this project's "no silent punting" convention.
6. **`WeaponSpecial` is already fully ported except exactly the 4 branches Phase 1 named.** Grepped
   the port's own `items/gear/WeaponSpecial.java`: the self-contained branches (`bottle_opener`,
   `wrench`/`wrench_flipped`, `stopsign`, `wood_gavel`/`lead_gavel` minus its potion effect,
   `diamond_gavel`) are live code today, and the 4 deferred branches
   (`memespoon`'s big-fall-nuke, `shimmer_sledge`'s rubble spawn, `onUpdate`'s advancement grants,
   `lead_gavel`'s `HbmPotion.lead` effect) are already marked with in-code TODO comments naming
   their exact blockers. This is **targeted completion work on an existing file**, not new item
   registration — confirmed each named blocker (`EntityNukeExplosionMK5`, `EntityNukeTorex`,
   `EntityRubble`, `AdvancementManager`, `HbmPotion`) is still absent from the port.
7. **Real 1.21.1 attribute-modifier and on-hit-hook API shapes are already proven in this port**,
   both by this port's own committed `ItemToolAbility`/`ItemToolAbilityPower`/`ToolItems` pattern
   (`Item.Properties.attributes(ItemAttributeModifiers...)` baked at construction, confirmed against
   `PickaxeItem`/`AxeItem`/`ShovelItem.createAttributes`) and independently by Neo Edition's own
   `SwordAbilityItem`/`SpecialSwordItem` (`ItemAttributeModifiers.builder().add(Attributes.
  ATTACK_DAMAGE, new AttributeModifier(id, value, Operation.ADD_VALUE), EquipmentSlotGroup.
  MAINHAND)`, and `hurtEnemy(ItemStack, LivingEntity, LivingEntity)` as the modern name for CE's
  `hitEntity`). No API needs to be invented for the sword classes — see Key design/API decisions.

## Phase-3-safe scope

### A. Ability-hook swords — `ItemSwordAbility`, `ItemSwordAbilityPower`, `ItemSwordMeteorite` (23 registry entries)

The class shapes themselves are Phase-3-safe today; every constructor argument
(`damage`/`attackSpeed`/`movement`/`ToolMaterial`→`Tier`/name) is either a plain number or already
solved by `ToolTiers`. Concretely:

| Class | Instances | Notes |
|---|---|---|
| `ItemSwordAbility` | `titanium_sword`, `steel_sword`, `alloy_sword`, `desh_sword`, `cobalt_sword`, `cobalt_decorated_sword`, `starmetal_sword`, `cmb_sword`, `schrabidium_sword` (`Rarity.RARE`), `mese_gavel`, `dnt_sword` | `mese_gavel` needs a new `Tier` (`matMeseGavel` in CE — not one of `ToolTiers`' existing constants) and its "hacky hack" special-case sound on hit (`this == ModItems.mese_gavel` → play `whack` sound) — a CE-authored identity-check special case, port as-is per this port's established `this ==`-avoidance conventions if any exist, otherwise keep the identity check (CE itself does this the same way `ItemSwordMeteorite`'s tooltip does, see below) |
| `ItemSwordAbilityPower` | `elec_sword` (500,000 max charge / 1,000 rate / 100 consumption per hit — same three-long constructor shape as `ItemToolAbilityPower`) | |
| `ItemSwordMeteorite` | 11 tiers, damage 9→75, all `movement = 0`, all `setMaxDamage(0)` (CE: unbreakable) | Tooltip is 11 `this == ModItems.meteorite_sword*` identity branches, each two lines of flavor text — trivial to port as translation-keyed lines, same modernization Phase 1's `items_food_gear.md` already recommended for `ModSword`'s tooltip special-casing |

Every instance's `.addAbility(...)` chain from `ModItems.java` (confirmed by grep, reproduced here
so the next implementer doesn't have to re-derive it):

- `steel_sword`/`alloy_sword`/`desh_sword`: `STUN` level 0
- `elec_sword`: `STUN` level 2
- `cobalt_decorated_sword`: `BOBBLE` level 0
- `starmetal_sword`: `BEHEADER` 0, `STUN` 1, `BOBBLE` 0
- `cmb_sword`: `STUN` 0, `VAMPIRE` 0
- `schrabidium_sword`: `RADIATION` 1, `VAMPIRE` 0 (+ `Rarity.RARE`)
- `mese_gavel`: `PHOSPHORUS` 0, `RADIATION` 2, `STUN` 3, `VAMPIRE` 4, `BEHEADER` 0
- `titanium_sword`/`cobalt_sword`/`meteorite_sword*`/`dnt_sword`: no abilities added (plain weapon
  stats only)

**None of these 23 items use `FIRE` or `CHAINSAW`** — confirmed by the grep above. Those two
abilities are chainsaw-only (already flagged as dropped-for-now in this port's own `ToolItems.java`
comment on `CHAINSAW`). This means the swords collectively need only **6 of the 8** concrete
abilities (`STUN`, `BOBBLE`, `BEHEADER`, `VAMPIRE`, `RADIATION`, `PHOSPHORUS`) to be fully weapon-complete.

### B. The 8 concrete `IWeaponAbility` singletons — read in full from CE, dependency-scoped individually

| Ability | CE behavior | Phase-3-safe today? |
|---|---|---|
| `STUN` | `MobEffectInstance(SLOWNESS, dur*20, 4)` + `(WEAKNESS, dur*20, 4)`, `dur ∈ {2,3,5,10,15}` (5 levels) | **Yes** — pure vanilla `MobEffects` |
| `BEHEADER` | On kill, `instanceof`-dispatches on victim type to drop a specific vanilla item (skeleton→skull, wither skeleton→skull(1/20)/coal, zombie→skull, creeper→skull, magma cube→magma cream, slime→slime ball, player→named skull via NBT, else→rotten flesh+bone) | **Yes** — all vanilla items; the 1.12 numeric-skull-damage-value scheme (`Items.SKULL` meta 0-4) needs mapping onto 1.21's separate `Items.SKELETON_SKULL`/`WITHER_SKELETON_SKULL`/`ZOMBIE_HEAD`/`PLAYER_HEAD`/`CREEPER_HEAD` items — a mechanical 1:1 remap, not a design decision |
| `VAMPIRE` | Heals attacker / damages victim by `{2,3,5,10,50}` (5 levels); if victim dies, `onDeath(DamageSource.MAGIC)` | **Yes** — `livingEntity.hurt`/`heal` are vanilla; `DamageSource.MAGIC` → `damageSources().magic()` (vanilla, already a real 1.21 source) |
| `FIRE` | `victim.setFire(dur)`, `dur ∈ {5,10}` (2 levels) | **Yes**, trivial — only needed once `CHAINSAW` (the ability) is worked, since no sword uses it |
| `BOBBLE` | On kill of an `EntityMob`/hostile with `>20` max health uses a 1/750 drop chance else 1/1000, drops a random `ModBlocks.bobblehead` (`BlockBobble.BobbleType`) variant | Blocked — see Deferred scope |
| `RADIATION` | `ContaminationUtil.contaminate(victim, RADIATION, CREATIVE, {15,50,500})` (3 levels) | Blocked — see Deferred scope |
| `PHOSPHORUS` | `MobEffectInstance(HbmPotion.phosphorus, dur*20, 4)`, `dur ∈ {60,90}` | Blocked — see Deferred scope |
| `CHAINSAW` | On kill, if `divider={15,10}` levels: drops `ceil(maxHealth/divider)` (capped 250) `nitra_small` items + matching XP orbs, plays a sound, and (server) sends a block-dust particle-burst packet | Blocked — see Deferred scope. Only consumer is the already-ported `chainsaw` item (`ToolItems.CHAINSAW`), which today has neither `CHAINSAW` nor `BEHEADER` added (per that class's own javadoc) — `BEHEADER` should be added to the chainsaw's `.addAbility(...)` chain as soon as `BEHEADER` itself lands, independent of `CHAINSAW`'s own separate blockers |

### C. Multitool ladder rungs 3–10 — `ItemMultitoolPassive` (8 registry entries: `multitool_ext`, `_miner`, `_hit`, `_beam`, `_sky`, `_mega`, `_joule`, `_decon`)

Per-rung dependency (CE behavior read in full above; not re-derived here):

| Rung | Sneak-click upgrade target | Non-sneak action | Attack damage | Dependency |
|---|---|---|---|---|
| `multitool_ext` | → `multitool_miner` (+ Fire Aspect III) | Right-click block: vanilla furnace-recipe lookup, instant-smelt block into inventory | 7 | **Phase-3-safe** — vanilla `RecipeManager`/smelting `RecipeType` lookup, no CE-specific system |
| `multitool_miner` | → `multitool_hit` (+ Looting III, Knockback III) | Spawns `EntityMinerBeam` projectile | 8 | Needs `EntityMinerBeam` (unported projectile entity) |
| `multitool_hit` | → `multitool_beam` | none (pass-through) | 16 (highest in the ladder) | **Phase-3-safe** — pure combat stats, zero other dependency |
| `multitool_beam` | → `multitool_sky` | Spawns `EntityLaserBeam` projectile | 8 | Needs `EntityLaserBeam` (unported projectile entity) |
| `multitool_sky` | → `multitool_mega` (+ Knockback V) | Spawns 15 `LightningBolt` entities in a 31×31 area around the player | 5 | **Phase-3-safe** — vanilla `EntityType.LIGHTNING_BOLT`, zero CE-specific dependency |
| `multitool_mega` | → `multitool_joule` (+ Knockback III) | Right-click block: `ExplosionChaos.levelDown` (see below) | 12 | Needs `ExplosionChaos.levelDown` ported **and** `CompatibilityConfig.isWarDim` — see Deferred scope, this is a real design fork, not a small gap |
| `multitool_joule` | → `multitool_decon` | Right-click block: raycasts 9 fanned lines of sight (25 blocks each) via `Library.getBlockPosInPath`, converts any block with hardness `∈(0,6000)` into a flung `EntityRubble` | 12 | Needs `Library.getBlockPosInPath` (confirmed **not yet ported** — grepped this port's own `com.hbm.lib.Library`, no such method exists) **and** `EntityRubble` |
| `multitool_decon` | → `multitool_dig` (+ Looting III, Fortune III — closes the loop back to the already-ported rung 1) | Right-click block: `ExplosionChaos.decontaminate`, a single-block `this ==`-dispatch table swapping ~10 `ModBlocks.waste_*` wasteland blocks for their clean vanilla equivalents | 5 | Needs the specific `ModBlocks.waste_*` set (unported wasteland world-gen blocks) |

**Class-shape decision, not a content decision**: CE's `ItemMultitoolPassive extends Item` directly
— it has no `Tier`/material concept at all (unlike `ItemMultitoolTool extends ItemTool`, which is
why Phase 1's port made it a `TieredItem`). Port rungs 3–10 as their own plain `Item` subclass
(`Item.Properties.durability(5000)`, no `Tier`), matching CE's real class boundary, rather than
retrofitting them onto `ItemMultitoolTool`/`TieredItem`. The per-rung `AttributeModifier` dispatch
(`this == ModItems.multitool_x` in CE) becomes per-instance `Item.Properties.attributes(...)` baked
at construction (see Key design/API decisions), not a runtime `this ==` branch.

### D. `ItemBoltgun` — 1 registry entry (`boltgun`), `onLeftClickEntity` only

Port target: on left-click of a living, alive entity, scan the player's inventory for the first
matching bolt ammo item from `{Mats.MAT_STEEL.make(bolt), Mats.MAT_TUNGSTEN.make(bolt),
Mats.MAT_DURA.make(bolt)}` (steel checked first — this is a fixed priority order, not
"best available"), consume one, deal 10 armor-piercing damage via a damage-bypasses-armor
`DamageSource`, ignore the target's invulnerability window (`EntityDamageUtil
.attackEntityFromIgnoreIFrame`, read in full — a simple retry-with-added-`lastDamage` trick, not
CE's heavier SEDNA damage pipeline), and fire an explosion-burst particle at the target plus the
boltgun-fire sound. `onItemUse`/`processNetwork`/`getAnimation` (bolt-driving into `IToolable`
blocks, the bus-animation recoil) are **out of this area's scope** — already correctly Phase 2
per `items_tool.md`'s sibling `ItemTooling` entry.

### E. `WeaponSpecial` completion — 4 named branches on an already-ported file, no new registry entries

Confirmed via grep of this port's own `items/gear/WeaponSpecial.java`: everything except these 4
branches is live. Blockers, confirmed absent from the port today:

| Branch | Behavior | Blocker(s) |
|---|---|---|
| `memespoon` big-fall-nuke | On a sufficiently large fall, spawns `EntityNukeExplosionMK5` + config-gated `EntityNukeTorex` | `EntityNukeExplosionMK5`, `EntityNukeTorex` (both absent — see headline finding #5) |
| `shimmer_sledge` block destruction | Converts the struck/used block into a flying `EntityRubble` carrying the block's identity | `EntityRubble` (absent) |
| `onUpdate` advancement grants | `ArmorUtil.checkForFiend`/`checkForFiend2` → `AdvancementManager.grantAchievement(achFiend/achFiend2)` | `ArmorUtil` (Phase 3 armor system, not yet built), `AdvancementManager` (absent — see Deferred scope) |
| `lead_gavel` potion effect | 15s `HbmPotion.lead` at amplifier 4 on hit | `HbmPotion` (absent — same custom-`MobEffect`-registry gap `docs/phase1/items_food_gear.md` already flagged for `ItemPill`/`ItemEnergy`) |

## Deferred scope

Real dependencies of *specific* melee-weapon branches, grouped by what actually blocks them:

- **`ContaminationUtil`-equivalent radiation facade** — blocks `RADIATION` (weapon ability). This is
  *not* a storage gap: `com.hbm.capability.HbmLivingAttachment` already carries the raw fields this
  facade would write to (`rads`, `digamma`, even `phosphorus`/`fire` fields already exist on the
  attachment, confirmed by reading the class), but the config-driven radiation-math/contamination
  game-logic layer on top of it is explicitly out of that capability's own scope per its javadoc
  ("a later status-effect system phase"). Whoever builds it should coordinate with whoever ports
  `ItemPill`/`ItemEnergy` (`docs/phase1/items_food_gear.md`), since both wait on the same facade.
- **`HbmPotion` custom `MobEffect` registry** — blocks `PHOSPHORUS` (weapon ability) and
  `lead_gavel`'s `HbmPotion.lead`. Same registry `docs/phase1/items_food_gear.md` already flagged
  for `ItemPill`, `ItemEnergy`, and `ItemAppleSchrabidium`'s `apple_lead` branch — a shared,
  cross-phase prerequisite, not specific to melee weapons. Recommend whoever picks this up builds
  the whole registry once (CE's `HbmPotion` has 6 named effects: `lead`, `radiation`, `radx`,
  `death`, `stability`, `potionsickness`, plus `phosphorus` confirmed here as a 7th referenced by
  `IWeaponAbility`) rather than adding effects piecemeal per consumer.
- **`ModBlocks.bobblehead`/`BlockBobble`** — blocks `BOBBLE` (weapon ability). A single decorative
  loot block with an enum of variants (`BobbleType`); narrow, no further dependency found.
- **`ModItems.nitra_small` + a particle-burst network packet** — blocks `CHAINSAW` (weapon
  ability). The item itself is likely trivial once whichever area owns crafting-material items
  registers it; the packet is the bigger piece — see the VFX note below.
- **`EntityMinerBeam`, `EntityLaserBeam`** — block `multitool_miner`/`multitool_beam`. Both are
  projectile entities in CE's `com.hbm.entity.projectile` package (not read in detail here, out of
  this melee-weapons survey's scope) — likely worth sequencing alongside whatever Phase 3 package
  ports this mod's other ranged/energy-weapon projectiles, since they are architecturally the same
  kind of entity, not melee-specific.
- **`EntityRubble`** — blocks `multitool_joule`, `multitool_mega` (via `levelDown`), and
  `WeaponSpecial`'s `shimmer_sledge`. Read in full: a small (88-line CE) thrown-block-visual entity
  extending CE's own `EntityThrowableNT` base (itself unported — check whether whichever area ports
  `EntityRubble` needs to port a throwable-entity base class too, or can adapt directly onto
  vanilla `ThrowableItemProjectile`/`Projectile`). Three independent consumers across this one
  report alone — port it once, shared.
- **`Library.getBlockPosInPath`** — blocks `multitool_joule`. Confirmed **not present** in this
  port's own `com.hbm.lib.Library` (grepped; the method does not exist). A raycast-fan utility
  (9 lines-of-sight, yaw-rotated by fixed increments, each walked out to a fixed range) — narrow,
  self-contained, no further dependency found in the slice read.
- **`CompatibilityConfig.isWarDim` / a "war dimension" concept** — blocks `multitool_mega`'s
  `levelDown` from ever actually firing. This is not a missing-file gap so much as an **already-made
  decision working against this feature**: `CompatibilityConfig`'s own doc comment says CE's
  per-dimension gamemode/spawn-rate maps and `isWarDim(World)` were **deliberately not ported**,
  since CE's numeric Forge dimension IDs (often referring to other mods' dimensions, e.g.
  Galacticraft) don't map onto 1.21's `ResourceKey<Level>` scheme. Whoever implements
  `multitool_mega` needs an explicit call here (see Open questions), not a silent stub.
- **`ModBlocks.waste_*` wasteland blocks** — block `multitool_decon`'s `decontaminate` (single-block
  scope only — the function itself, read in full, is a ~10-branch `this ==` block swap, not an
  area-effect; the *item* can be registered and its right-click wired now, it will just no-op until
  these blocks exist). World-gen content, likely Phase 4 territory alongside other biome/wasteland
  systems.
- **`EntityNukeExplosionMK5`, `EntityNukeTorex`** — block `WeaponSpecial`'s `memespoon` branch, and
  are **already imported and called by `com.hbm.hazard.type.HazardTypeUnstable`** today (a
  pre-existing compile break independent of this report — see headline finding #5). Whoever ports
  these two entities should treat `HazardTypeUnstable` as a second, already-blocked consumer to
  verify against, not just `WeaponSpecial`.
- **`AdvancementManager`** — blocks `WeaponSpecial`'s `onUpdate` grants. Read CE's version in full
  header: a ~35-field static `Advancement`-handle registry resolved by `ResourceLocation` lookup at
  world load, plus a `grantAchievement(player, advancement)` trigger wrapper. 1.21's real
  replacement is data-driven advancement JSON plus a custom `CriterionTrigger` the mod fires
  programmatically (or `ServerPlayer.getAdvancements().award(...)` directly) — this is a general,
  cross-cutting achievements facade needed by many files across many phases (per its ~35 fields),
  not something to design narrowly for one `WeaponSpecial` branch. Out of this report's scope to
  design; flagged as a real, named dependency.
- **`ArmorUtil.checkForFiend`/`checkForFiend2`** — also blocks `WeaponSpecial`'s `onUpdate` grants.
  Phase 3 armor-system territory (checking the player's currently worn full armor set identity) —
  sequence after whichever package builds the armor-slot system `docs/phase1/items_food_gear.md`
  already deferred wholesale to Phase 3.
- **A particle-burst / VFX network packet** — `ItemBoltgun`'s hit VFX, `CHAINSAW`'s block-dust
  burst, and `multitool_miner`/`multitool_beam`'s projectile-fire sounds all want CE's
  `AuxParticlePacketNT`/`HbmEffectNT` particle system. **`com.hbm.packet.HbmNetwork` registers zero
  content payloads today** (only the Phase 2 block-entity sync `BufPacket`) — confirmed by reading
  the file in full. This is a shared, non-trivial client-VFX system that essentially every ranged/
  explosive Phase 3 weapon will also want; recommend whoever implements this area's first weapon
  needing it (likely not the melee swords, which need no VFX packet at all) builds it once as a
  proper shared system rather than a boltgun-specific one-off.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior, this port's own committed code plus
Neo Edition's reference implementation for 1.21.1 API shape — no API invented below):

- **Sword attribute modifiers**: `Item.Properties.attributes(ItemAttributeModifiers.builder()
  .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(id, damage, Operation.ADD_VALUE),
  EquipmentSlotGroup.MAINHAND).add(Attributes.ATTACK_SPEED, ..., EquipmentSlotGroup.MAINHAND)
  .build())`, baked at item construction — confirmed identically by Neo Edition's own
  `SwordAbilityItem` and by this port's own already-committed `ToolItems`/`ItemToolAbility`
  pattern (`AxeItem`/`PickaxeItem`/`ShovelItem.createAttributes`, the same idiom for a
  `SwordItem`). There is no runtime `getItemAttributeModifiers(EquipmentSlot)` override needed or
  available the way CE's `ItemSwordAbility` used one — do not try to port that override shape.
  CE's separate `movement` constructor argument (a movement-speed debuff/buff, nonzero only for
  `mese_gavel` at `+1.5`) needs the same `Attributes.MOVEMENT_SPEED` treatment as CE's own
  `SharedMonsterAttributes.MOVEMENT_SPEED` multimap entry — `ToolItems.java`'s own precedent
  (dropping a similar tool-side `movement` value entirely, since Neo Edition's parallel port also
  drops it) does **not** directly apply here since `mese_gavel`'s value is much larger and clearly
  load-bearing flavor for that specific top-tier weapon — recommend porting it for the swords
  rather than reusing the tools' drop-it precedent, but flag as a call for whoever implements this.
- **On-hit hook**: 1.21's `SwordItem`/`Item` exposes `hurtEnemy(ItemStack stack, LivingEntity
  target, LivingEntity attacker)` as the direct modern equivalent of CE's `hitEntity(ItemStack,
  EntityLivingBase target, EntityLivingBase attacker)` — confirmed by Neo Edition's
  `SwordAbilityItem`/`SpecialSwordItem`, both of which override exactly this method with the same
  `(stack, target, attacker)` shape and dispatch ability logic from inside it, guarded the same way
  CE does (`!attacker.level().isClientSide && attacker instanceof Player`). **Open question**: Neo
  Edition's `SwordAbilityItem.hurtEnemy` does not itself call `stack.hurtAndBreak(...)` (durability
  loss) or `super.hurtEnemy(...)`, whereas CE's `hitEntity` explicitly calls
  `stack.damageItem(1, attacker)` before `return super.hitEntity(...)`. Verify at implementation
  time whether vanilla's per-attack durability loss happens elsewhere in the modern attack pipeline
  (outside `Item#hurtEnemy`) before assuming Neo Edition's reference omission is intentional and
  safe to copy — getting this wrong would silently make every ability sword unbreakable.
- **`ItemSwordAbilityPower`'s battery storage**: follow this port's own already-established pattern
  from `ItemToolAbilityPower` exactly — implement `IBatteryItem` (already ported,
  `com.hbm.api.energymk2.IBatteryItem`, confirmed real: `Supplier<DataComponentType<Long>>
  getChargeComponent()` + default `charge`/`discharge`/`get`/`setCharge` methods operating on a
  `DataComponentType<Long>`, not raw NBT) backed by a new package-local charge component (mirroring
  `ToolDataComponents.TOOL_CHARGE`, e.g. a `WeaponDataComponents.SWORD_CHARGE` or similar — no
  shared `hbm:battery_charge` component exists yet anywhere in the tree, per `IBatteryItem`'s own
  javadoc, so don't wait for one). `canOperate`/`applyWear`-equivalent hooks map onto
  `ItemToolAbilityPower.canOperate`/`applyWear` 1:1.
- **`ItemMultitoolPassive`'s class shape**: a plain `Item` subclass (not `TieredItem`/`ItemTool`),
  `Item.Properties.durability(5000)`, per-instance `Item.Properties.attributes(...)` baked at
  construction for the flat attack-damage-only modifier (CE's `getAttributeModifiers` override
  becomes 8 separate baked-in component values, one per registered item, rather than a runtime
  `this ==` dispatch) — see Phase-3-safe scope section C for the full per-rung numbers.
- **Entity registration** for any of this area's new entities (`EntityRubble`, `EntityMinerBeam`,
  `EntityLaserBeam`, and — shared with `HazardTypeUnstable` — `EntityNukeExplosionMK5`/
  `EntityNukeTorex`): follow the one confirmed real pattern already in this port,
  `com.hbm.entity.ConveyorEntityTypes` — its own `DeferredRegister<EntityType<?>>` targeting
  `BuiltInRegistries.ENTITY_TYPE`, `EntityType.Builder.of(ctor, MobCategory.MISC).noSummon()
  .sized(w, h).setTrackingRange(n).build(name)`. That class's own javadoc explicitly leaves open
  whether later entity families keep this "one `DeferredRegister` per family" shape or consolidate
  into a single `ModEntityTypes` — a real open call for whoever lands the first of this area's
  entities, not resolved here.
- **`ItemBoltgun`'s armor-piercing damage**: CE builds this from a mutable-flag `DamageSource`
  (`DamageSource.causePlayerDamage(player).setDamageBypassesArmor()`), not a distinctly-named
  `ModDamageSource` singleton — confirmed by reading the file; grepping this port's own
  `ModDamageTypes` finds no boltgun-specific entry (nor should there be one solely for this). The
  1.21.1 mechanism is already proven in this port: define a `DamageType` (either reuse vanilla's
  `damageSources().playerAttack(player)` if a plain "player attack, but bypasses armor" reads as
  close enough, or register a small dedicated `hbm:boltgun` `DamageType` via the same
  `ModDamageTypes`/`ModDamageTypeTagsProvider` pattern already built) and put it in vanilla's own
  `DamageTypeTags.BYPASSES_ARMOR` (not one of this port's custom `ModDamageTypeTags`, since that tag
  already exists in vanilla and CE's flag was semantically exactly that, not CE's own separate
  `ABSOLUTE`/`setDamageIsAbsolute` concept — do not conflate the two, they're different CE flags on
  different call sites).
- **Ignoring invulnerability frames**: `EntityDamageUtil.attackEntityFromIgnoreIFrame` (the one
  helper `ItemBoltgun` actually calls, read in full) is a simple retry: call
  `victim.attackEntityFrom(src, damage)`, and if blocked (still within the i-frame window), retry
  once with `damage + victim.lastDamage` added — it does **not** invoke CE's heavier SEDNA damage
  pipeline (`attackEntityFromNT`/`DamageResistanceHandler`) at all for this item. No 1.21 port of
  either `EntityDamageUtil` or a 1.21-native "ignore i-frames" helper exists yet in this port
  (confirmed by grep) — whoever implements the boltgun (likely alongside other Phase 3 weapons that
  will want the same trick) needs to either replicate this exact retry-with-added-`lastDamage`
  approach against vanilla's `invulnerableTime`/`lastHurt` fields, or find/build a NeoForge-native
  equivalent. Flag this as a shared utility gap, not specific to the boltgun.

## Open questions / risks

- **Should `multitool_mega`'s `levelDown` be a permanent no-op in this port, or does it need a new
  dimension-gating mechanism?** `CompatibilityConfig.isWarDim` was *deliberately* not ported (see
  Deferred scope) because CE's numeric dimension-ID scheme doesn't map onto 1.21's
  `ResourceKey<Level>`. If nothing gates it, `levelDown`'s naive per-block double loop over a
  `radius²` area (default called with `radius=2`, small, but CE's own multiblock/explosion code
  elsewhere uses much larger radii — worth checking whether any other Phase 3 caller of a similar
  utility uses a bigger number) becomes reachable in every dimension. Either explicitly decide it's
  permanently unreachable (register the item, its ability description stays accurate to CE, but the
  right-click branch always no-ops, mirroring CE's own early-return when `isWarDim` is false in a
  world with no war dimension) or design a replacement gate — but make the call explicitly, don't
  let it silently become "works everywhere now" or "silently dead code" by accident.
- **Explosion/mass-block-removal performance is directly relevant here, not just to the bomb/nuke
  weapons this phase will also cover.** `multitool_mega`'s `levelDown` and `multitool_joule`'s
  9-ray block-to-rubble sweep are both small-scale versions of the same "loop over many blocks,
  call `setBlockState`/`Level#setBlock` per block, spawn an entity per removed block" pattern
  PORT_SPEC flags as needing batched `LevelChunk` section writes + deferred lighting. These two
  items are a good, low-risk place to establish that batching pattern (CE's actual constants here
  are small — a handful to a few hundred blocks) before the much larger nuke/bomb explosion code
  (hundreds to thousands of blocks) needs the same infrastructure — worth sequencing this area's
  implementation before or alongside that heavier explosion work rather than after, so the pattern
  exists once and both consumers share it.
- **`BEHEADER`'s skull-item remap** (1.12's single `Items.SKULL` + meta 0-4 → 1.21's 5 separate
  skull/head items) is mechanical but easy to get subtly wrong (e.g. swapping which head goes to
  wither skeleton vs skeleton) — worth a dedicated unit test given how cheap one would be to write.
- **`EntityRubble`'s CE base class (`EntityThrowableNT`) is itself unported.** Before porting
  `EntityRubble`, decide whether it's worth porting a small `EntityThrowableNT`-equivalent base (if
  other unported CE throwable entities would also want it) or adapting `EntityRubble` directly onto
  vanilla's `ThrowableItemProjectile`/`Projectile` — not resolved in this report, flagged since it
  affects 3 separate consumers found here (`multitool_joule`, `multitool_mega`'s `levelDown`,
  `WeaponSpecial`'s `shimmer_sledge`).
- **`HbmPotion`'s effect list has now been independently confirmed at 7 named effects across two
  research reports** (`lead`, `radiation`, `radx`, `death`, `stability`, `potionsickness` per
  `docs/phase1/items_food_gear.md`; `phosphorus` confirmed here). Recommend whoever builds this
  registry cross-check both reports before finalizing the list, since a third consumer area
  surfacing a name neither report caught is a real risk given how many files reference this one
  unported class.
- **`mese_gavel`'s movement-speed modifier and `this == ModItems.mese_gavel` on-hit sound
  special-case** are both single-item CE identity-check patterns preserved from 1.12 code style —
  neither is wrong to port as-is (CE's own `ItemSwordMeteorite` tooltip does the same `this ==`
  pattern for its 11 tiers), but confirm this port has no established preference against
  `this ==` identity dispatch in item classes before assuming it's fine to keep verbatim.
- **No sword or multitool item in this area needs a new `DamageType`, `DataComponent` beyond the
  battery-charge one already discussed, or a `Menu`/`Screen`.** None of `ItemSwordAbility`'s family,
  the multitool ladder, or `ItemBoltgun`'s `onLeftClickEntity` opens a GUI or defines a custom
  damage source of its own (boltgun reuses/extends a vanilla-shaped source per the design decision
  above) — worth stating explicitly so no implementer over-builds infrastructure this area doesn't
  actually need, in contrast to `WeaponSpecial`'s `onUpdate` branch and the boltgun's *right-click*
  half, both of which are out of this report's scope entirely.
