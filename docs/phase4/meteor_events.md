# Meteor-strike event system — Phase 4 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/entity/projectile/EntityMeteor.java` (225 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemMeteorRemote.java` (75 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/items/armor/ItemModCharm.java` (53 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemSwordMeteorite.java` (82 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/generic/BlockMeteorOre.java` (13 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/world/Meteorite.java` (625 lines) and
  `MeteoriteStructure.java` (50 lines) — read in full to establish the two real call sites into the
  shared shape-stamping engine, but that engine itself is **owned by the sibling report**
  (`docs/phase4/worldgen_oil_and_meteor_dungeons.md`) and is not re-derived here.
- `upstream/hbm-ce/src/main/java/com/hbm/handler/BossSpawnHandler.java` (250 lines, full — the
  meteor-relevant ~70 lines are `meteorUpdate`/`spawnMeteorAtPlayer`/the two static fields/the
  `WorldConfig.enableMeteorStrikes` gate at line 163; the rest of the file, maskman/raid/elemental
  mob-spawn rolls, is read for context only and is **not** this report's scope)
- `upstream/hbm-ce/src/main/java/com/hbm/main/ModEventHandler.java` — full `worldTick(WorldTickEvent)`
  method (lines 662-690+) read to confirm `rollTheDice`'s real call site, phase, and side gating
- `upstream/hbm-ce/src/main/java/com/hbm/lib/ModDamageSource.java` (153 lines, grepped for
  `meteorite`/`broadcast` DamageSource definitions)
- `upstream/hbm-ce/src/main/java/com/hbm/config/WorldConfig.java` (grepped, meteor section: fields at
  lines 17-23, config wiring at 60-66)
- `upstream/hbm-ce/src/main/java/com/hbm/command/CommandHbm.java` (382 lines; `doGenCommand`'s
  `"meteorite"` case at line 280 read in context, confirming the only admin command touching meteors
  is the sibling report's ambient-structure debug spawner, not a live-strike trigger)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/BlockEnums.java` (`EnumMeteorType` — 5 values: IRON,
  COPPER, ALUMINIUM, RAREEARTH, COBALT), `OreEnumUtil.java` (`METEORITE_FRAG`/`BLOCK_METEOR` OreEnum
  entries + `blockMeteorDrop()` body), `ModBlocks.java` (grepped meteor block registration lines)
- `upstream/hbm-ce/src/main/java/com/hbm/crafting/{SmeltingRecipes,MineralRecipes,ToolRecipes,
  PowderRecipes,ConsumableRecipes}.java` and `inventory/recipes/{BlastFurnaceRecipes,
  BlastFurnaceRecipesNT,AssemblyMachineRecipes}.java` + `inventory/recipes/anvil/AnvilRecipes.java`
  (grepped, tracing the full `fragment_meteorite`/`powder_meteorite`/`ingot_meteorite`/
  `ingot_meteorite_forged`/`blade_meteorite`/`meteorite_sword` material chain end to end)
- `upstream/hbm-ce/src/main/java/com/hbm/items/ModItems.java` (grepped: `protection_charm`,
  `meteor_charm`, `meteor_remote`, `ingot_meteorite`, `blade_meteorite`, `powder_meteorite`,
  `fragment_meteorite` field declarations)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/ItemGlitch.java` and
  `items/tool/ItemGuideBook.java` (grepped, incidental meteor references — an Easter-egg loot roll and
  a guide-book texture path, neither part of this system)

Neo Edition — read only to confirm real NeoForge 1.21.1 API **shapes**, never for behavior or balance
(this cluster is fully ported there, unlike the sibling ambient-structure system):
- `upstream/neo-edition/src/main/java/com/hbm/world/MeteorStrikeSystem.java` (107 lines, full)
- `upstream/neo-edition/src/main/java/com/hbm/entity/projectile/Meteor.java` (194 lines, full)
- `upstream/neo-edition/src/main/java/com/hbm/items/tools/MeteorRemoteItem.java` (45 lines, full)
- `upstream/neo-edition/src/main/java/com/hbm/main/NtmEventHandler.java` (85 lines, full)

This port's own already-real, already-compiling code, read to confirm what already exists and what
integration pattern to follow:
- `src/main/java/com/hbm/config/WorldConfig.java` (100 lines; meteor block at lines 29-35, 85-96 —
  **all 7 CE config fields already ported**)
- `src/main/java/com/hbm/blocks/OreBlocks.java` (282 lines, `registerMeteorOres()` at lines 162-174
  plus its class-level javadoc's exclusion list, both read in full)
- `src/main/java/com/hbm/blocks/generic/GenericBlocks.java` (grepped: `ore_meteor`'s
  per-`EnumMeteorType`-variant registration)
- `src/main/java/com/hbm/damage/ModDamageTypes.java` (grepped: `METEORITE` damage type, already
  registered) and `src/main/java/com/hbm/sound/ModSounds.java` (grepped: `METEORITE_FALLING_LOOP`,
  already registered)
- `src/main/java/com/hbm/handler/ArmorModHandler.java` (184 lines; `helmet_only`/`hasMods`/`pryMods`
  read at lines 33, 145-158 — this port's own real, single-argument signature)
- `src/main/java/com/hbm/handler/neutron/NeutronHandler.java` (62 lines, full — the established
  self-subscribing `ServerTickEvent.Pre` precedent for exactly this class of problem)
- `src/main/java/com/hbm/entity/projectile/RubbleEntityTypes.java` (40 lines, full — the established
  per-family `DeferredRegister<EntityType<?>>` precedent)
- `src/main/java/com/hbm/main/CommonTickEvents.java` (35 lines, full — the other existing
  `@EventBusSubscriber`/game-bus tick-dispatch class, for contrast: per-entity, not per-server-tick)
- `src/main/java/com/hbm/explosion/ExplosionLarge.java` (200 lines; `spawnRubble`/`spawnParticles`
  signatures read at lines 60, 74 — already real and already called this way elsewhere)
- `src/main/java/com/hbm/items/weapon/WeaponMeleeItems.java` (grepped: its own javadoc already
  documents `meteorite_sword`'s 11 tiers as a real, still-open, named gap)
- `docs/phase3/melee_weapons.md` and `docs/phase4/worldgen_oil_and_meteor_dungeons.md` (sibling
  reports, cross-referenced rather than re-derived)

## Headline finding

This task's own framing turns out to be exactly right about *where the boundary is* — CE really does
have a live, tick-driven, random meteor-fall event, genuinely distinct from the pre-built dungeon — but
three things need correcting or sharpening before scoping the work:

1. **The two meteor systems are not "the same system," but they share one real piece of code, and that
   piece belongs to the sibling report, not this one.** `com.hbm.world.Meteorite.generate(...)` (625
   lines) is the block-shape stamping engine that both the ambient `MeteoriteStructure` world-gen
   feature *and* this report's `EntityMeteor` call at the moment of impact. `docs/phase4/
   worldgen_oil_and_meteor_dungeons.md` already reads `Meteorite.java` in full and explicitly names
   `EntityMeteor.java:142`'s call as "the live falling-meteor entity, other agent's scope" — so there
   is no double-research here, only one shared dependency to name on both sides. This report does not
   re-derive `Meteorite.generate`'s 20-branch special-meteor switch, its `genL1..genL5`/`genM1..genM6`
   shape variants, or its `ModBlocks.block_meteor*` registrations — see that report for all of it.
   The only two facts this report needs from it: `MeteoriteStructure`'s ambient call passes
   `(safe=false, allowSpecials=false, damagingImpact=false)`, while `EntityMeteor`'s impact call passes
   `(safe, allowSpecials=true, damagingImpact=true)` — the live event is a strictly *richer* trigger of
   the same generator (it can roll every special-meteor case, including the Star Blaster gun drop and
   the 1000-damage insta-kill AoE; the ambient structure never does either).
2. **The live-strike system is small and has almost no blocking cross-phase dependencies** — a rare
   property among Phase 4 areas. One 225-line `Entity` subclass, one ~70-line slice of a handler method
   (the rest of that method's file is unrelated mob-spawn rolls, see Deferred scope), one debug item,
   and two small `ItemArmorMod` leaf items are the entire new-code surface. Everything else it touches
   — all 7 `WorldConfig` meteor fields, the `METEORITE` damage type, the `METEORITE_FALLING_LOOP` sound
   event, and every `block_meteor*`/`ore_meteor` block the impact can place — **is already ported and
   already real** in this port (confirmed by direct read, see Sources above). This is a genuinely
   narrow, low-risk implementation slice once the sibling's `Meteorite.generate` port lands.
3. **Correcting this task's own hypothesis about `ItemSwordMeteorite`: meteor strikes are *not* that
   family's real gameplay gate.** Tracing the full crafting chain end to end (`SmeltingRecipes.java`,
   `PowderRecipes.java`, `BlastFurnaceRecipes(NT).java`, `AnvilRecipes.java`, `ToolRecipes.java`):
   `meteorite_sword` needs `blade_meteorite` ← anvil-forged from two `ingot_meteorite_forged` ← anvil-
   forged from two `ingot_meteorite` ← blast-furnaced from `powder_meteorite` (+ a cobalt ingot
   catalyst) ← **`powder_meteorite` is a shapeless craft from ordinary iron/copper/lithium/netherquartz
   dust** (`PowderRecipes.java:50`), with **zero meteor-block input anywhere in that chain**. Mining
   `ore_meteor` only ever yields ordinary metals (iron/copper/aluminium/rare-earth/cobalt, its 5
   `EnumMeteorType` values); mining the meteor *hull* blocks (`block_meteor_cobble`/`_broken`) only
   ever yields `fragment_meteorite`, which is consumed exclusively by `protection_charm`/`meteor_charm`
   and a purely decorative reverse-craft — **never** by the ingot/blade/sword chain. A player can build
   the entire `meteorite_sword` family without a single meteor ever striking the world. The one place
   meteor events do gate real content: `fragment_meteorite` (→ the two armor-mod charms below) and the
   1-in-10 `plate_dalekanium` "jackpot" drop from mining `block_meteor` itself (`OreEnumUtil.
   blockMeteorDrop`, already faithfully re-implemented in this port's own `OreBlocks.java:163-167`).
   `docs/phase3/melee_weapons.md` independently confirms the sword family's 11 items exist in CE but
   that `ItemSwordMeteorite` + a `matMeteorite`-equivalent `Tier` are **still an open, unbuilt gap** in
   this port — not something this report needs to touch.
4. **Neo Edition has already built this entire cluster** (`MeteorStrikeSystem`, `Meteor`,
   `MeteorRemoteItem`, wired into `NtmEventHandler`) — useful for confirming real NeoForge 1.21.1 API
   shapes, but it contains at least three confirmed **behavior divergences from CE** that must not be
   copied (per this project's own stated rule that Neo Edition is never a source of behavior): it drops
   CE's Overworld-only gate entirely, it "fixes" a real CE timer bug by keying meteor showers per
   dimension instead of with one shared static counter, and its `Meteor.tick()` has a genuine structural
   bug where disabling meteor tails would leave a landed meteor entity looping forever. All three are
   detailed in Key design decisions and Open questions below.

## Phase-4-safe scope

| Component | CE source | Status in this port | Recommended treatment |
|---|---|---|---|
| `EntityMeteor` (falling entity, path-clearing, impact) | `entity/projectile/EntityMeteor.java`, 225 | Not ported | New `Entity` subclass in `com.hbm.entity.projectile`; own `EntityType` registration file (e.g. `MeteorEntityTypes.java`), mirroring the already-real `RubbleEntityTypes.java`/`FallingNukeEntityTypes.java` per-family `DeferredRegister` pattern — `trackingRange=1000`, `sized(4F, 4F)` taken directly from CE's `@AutoRegister(name="entity_meteor", trackingRange=1000)` + the constructor's `setSize(4F, 4F)` |
| Meteor-strike tick trigger (roll + spawn + shower bookkeeping) | `handler/BossSpawnHandler.java` — static fields at line 37/185, `meteorUpdate`/`spawnMeteorAtPlayer` at lines 186-248, gate at line 163 | Not ported | New small self-subscribing handler class (e.g. `com.hbm.handler.MeteorStrikeHandler`) following the already-real `NeutronHandler.java` pattern: `@EventBusSubscriber(modid = MainRegistry.MODID)` + `@SubscribeEvent public static void onServerTick(ServerTickEvent.Pre event)`, iterating `event.getServer().getAllLevels()` |
| `ItemMeteorRemote` (debug/testing spawn item) | `items/tool/ItemMeteorRemote.java`, 75 | Not ported | New `Item`, creative-tab-only (confirmed: no crafting recipe anywhere in CE), calls the new handler's `spawnMeteorAtPlayer` directly, `stacksTo(1).durability(2)` |
| `ItemModCharm` — `protection_charm`/`meteor_charm` (helmet-slot mitigation) | `items/armor/ItemModCharm.java`, 53 + `ModItems.java:245-246` | Not ported | New `Item` extending this port's already-real `ItemArmorMod`; needed to exercise 2 of the handler's 3 branches (repel / no-strike) — recommend building both here since they're ~2 lines of real logic each and otherwise the mitigation paths are untestable |
| `ModDamageSource.meteorite` (1000 absolute, armor-bypassing impact AoE) | `lib/ModDamageSource.java:23` | **Already ported** as `ModDamageTypes.METEORITE` | Reuse, no work needed |
| Meteor config (7 fields: strikes/showers/tails/specials toggles + 3 chance/duration ints) | `config/WorldConfig.java:17-23,60-66` | **Already ported**, matching CE defaults exactly | Reuse, no work needed |
| `block_meteor`/`_cobble`/`_broken`/`_treasure` + `ore_meteor` (5 `EnumMeteorType` variants) | `ModBlocks.java` (grepped) | **Already ported** (`OreBlocks.java:162-174`, `GenericBlocks.java:387`), including the faithful 1-in-10 `plate_dalekanium` jackpot drop | Reuse, no work needed |
| `HBMSoundHandler.meteoriteFallingLoop` | `lib/HBMSoundHandler.java` (grepped) | **Already ported** as `ModSounds.METEORITE_FALLING_LOOP` | Reuse, no work needed |
| `HBMSoundHandler.techBleep`/`oldExplosion` (used by `ItemMeteorRemote`/`EntityMeteor`) | same file | Not confirmed in this pass | Verify against `ModSounds.java`'s full symbol list before wiring — not checked here since it wasn't this report's focus |
| Shared block-shape stamping (`Meteorite.generate`, all impact variety/loot) | `world/Meteorite.java`, 625 | **Owned by** `docs/phase4/worldgen_oil_and_meteor_dungeons.md` (not yet ported per that report) | Do not re-implement here — `EntityMeteor`'s impact is simply a second call site into that same class once it exists, passing `(safe, true, true)` instead of the ambient feature's `(false, false, false)` |
| `ExplosionLarge.spawnRubble`/`spawnParticles` (cosmetic post-impact rubble/particle burst) | N/A (this port's own class) | **Already ported and real** (`com.hbm.explosion.ExplosionLarge:60,74`) | Reuse, call exactly as CE's `EntityMeteor.onUpdate()` does |

## Deferred scope

Real dependencies of *this specific* subsystem that belong to other packages/phases:

- **`Meteorite.generate`'s full 625-line shape-stamping engine, its 20-branch special-meteor switch,
  and the `MeteoriteStructure` ambient world-gen feature it also serves** — fully owned by
  `docs/phase4/worldgen_oil_and_meteor_dungeons.md`. This report only needs that engine to exist with
  its real signature (`generate(World/Level, Random, x, y, z, boolean safe, boolean allowSpecials,
  boolean damagingImpact)`); do not duplicate that report's Part 2a research here.
- **The `"meteor_dungeon"` NBT-jigsaw pre-built structure** — same sibling report, Part 2b. Confirmed
  structurally unrelated to this event system (different package, different trigger — chunk-population
  time vs. runtime entity), per that report's own explicit finding.
- **`BossSpawnHandler`'s other three roll types** (maskman/FBI-raid/RAD-beast-elemental spawns), which
  live in the *same* CE method (`rollTheDice`) and the *same* file as the meteor roll, sharing only the
  outer per-tick call site — not the meteor logic itself. Belongs to whichever Phase 3/4 area researches
  `EntityMaskMan`/`EntityFBI`/`EntityFBIDrone`/`EntityRADBeast` periodic hostile-event spawning. This
  report's recommended handler class intentionally isolates only the meteor-specific ~70 lines, matching
  this port's own established convention (per `NeutronHandler`'s javadoc) of one small self-contained
  class per system rather than reproducing CE's grab-bag method.
- **`ItemSwordMeteorite`'s 11-tier item family and its `matMeteorite`-equivalent `Tier`** — confirmed by
  this report's own material-chain trace (Headline finding #3) to have no real dependency on meteor
  events, and already tracked as a real, still-open, named gap in `docs/phase3/melee_weapons.md`. Not
  touched by this report.
- **The `fragment_meteorite → powder_meteorite → ingot_meteorite → ingot_meteorite_forged →
  blade_meteorite` material-progression recipes** (`PowderRecipes`, `BlastFurnaceRecipes(NT)`,
  `AnvilRecipes`) — mostly independent of meteor events (see Headline finding #3); belongs to whichever
  Phase 1/2 area is doing blast-furnace/anvil/assembly-machine recipe-content parity. Only the ore-drop
  half (`fragment_meteorite` itself, from mining `block_meteor_cobble`/`_broken`) is confirmed already
  real in this port, via `PlateCrystalWasteItems.FRAGMENT_METEORITE`.
- **`ModDamageSource.broadcast` and `TileEntityBroadcaster`** — the *other*, unrelated function of
  `protection_charm`/`meteor_charm` (halving/negating radio-broadcaster-tower damage, `ItemModCharm.
  modDamage`). Confirmed to have nothing to do with meteors; belongs to whichever phase/area researches
  `TileEntityBroadcaster`. When porting the two charm items for this report's own scope, their
  `modDamage` broadcast-mitigation branch can be ported verbatim (it's a two-line `if`) without pulling
  in the broadcaster tower itself.
- **`block_meteor_molten`'s "stateful tick-driven molten-to-cobble transition" behavior** — referenced
  only as a hull option inside `Meteorite.generate` (sibling report's territory) and explicitly excluded
  by this port's own `OreBlocks.java` class javadoc as "not a resource-drop block." It is a passive
  block tick-behavior, not part of the live-strike *event* trigger this report covers — flagged here
  only because the block name surfaced during this research; belongs to whichever Phase 1/2 area does
  block-tick-behavior parity.
- **Client-side rendering and audio**: `RenderMeteor` (falling-entity model/tail render, both CE's and
  Neo Edition's versions exist but were not read in depth here), `ItemRendererMeteorSword`, the
  `HbmEffectNT.Exhaust_Meteor` particle trail, and the guide-book's `guide_meteor_sword` texture pages
  (`ItemGuideBook.java:225`) — all Phase 5 ("Client & UX").
- **Armor/hazard interaction**: none found. `ModDamageSource.meteorite` sets both
  `setDamageIsAbsolute()` and `setDamageBypassesArmor()` in CE, so the 1000-damage impact AoE is
  intentionally un-mitigable by any Phase 3 armor system — confirmed not a hidden `ArmorUtil`/
  `HazmatRegistry` dependency.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior; this port's own committed code and Neo
Edition's parallel, already-built port for NeoForge API shape — no NeoForge API is invented below):

- **Entity lifecycle**: CE's `onUpdate()` → NeoForge `tick()`; `setDead()` → `discard()`;
  `readEntityFromNBT`/`writeEntityToNBT(NBTTagCompound)` → `readAdditionalSaveData`/
  `addAdditionalSaveData(CompoundTag)`; the empty `entityInit()` → an empty
  `defineSynchedData(SynchedEntityData.Builder)` (CE's `EntityMeteor` has no synced fields beyond NBT
  persistence of `safe`, and neither does Neo Edition's port — confirmed no new synced-data plumbing is
  needed). Confirmed via Neo Edition's `Meteor.java`, which is a direct mechanical translation of CE's
  `EntityMeteor.java` (identical field names, identical fall/clear-path/impact math) rather than a
  reimagining — safe to mirror its *structure* even though its exact numbers must still be checked
  against CE, not it.
- **`EntityType` registration**: follow this port's own established per-family
  `DeferredRegister<EntityType<?>>` pattern (`RubbleEntityTypes.java`/`FallingNukeEntityTypes.java`,
  both already real and compiling) rather than CE's `@AutoRegister` annotation or a shared
  `ModEntityTypes` god-class:
  `EntityType.Builder.<EntityMeteor>of(EntityMeteor::new, MobCategory.MISC).noSummon()
  .sized(4F, 4F).setTrackingRange(1000).build("entity_meteor")` — size and tracking range read
  directly off CE's `@AutoRegister(name = "entity_meteor", trackingRange = 1000)` plus the constructor's
  `this.setSize(4F, 4F)`.
- **Vanilla explosion call**: CE's `world.createExplosion(entity, x, y, z, power, doesBlockDamage)` →
  NeoForge `level.explode(entity, x, y, z, power, Level.ExplosionInteraction.BLOCK_or_NONE)`. The 6-arg,
  no-fire-flag overload is already in live use elsewhere in this exact port (`ItemCapacitor.java`,
  `IToolAreaAbility.java`) — confirmed real, not invented. Use `!safe ? BLOCK : NONE`, matching CE's own
  `!safe` boolean flag.
- **Tick-hook placement**: this port already has an established, documented convention for exactly this
  shape of problem — `NeutronHandler.java`'s javadoc explicitly explains why it "self-subscribes instead
  of modifying a shared, multi-package event-handler aggregator file." Recommend the identical pattern
  for the meteor roll: a small new class (e.g. `com.hbm.handler.MeteorStrikeHandler`), `@EventBusSubscriber
  (modid = MainRegistry.MODID)`, `@SubscribeEvent public static void onServerTick(ServerTickEvent.Pre
  event)`, iterating `event.getServer().getAllLevels()` — the exact real `MinecraftServer` method Neo
  Edition's own `MeteorStrikeSystem.update()` already uses to reach every loaded `ServerLevel`.
- **Preserve CE's real dimension gate.** CE's `spawnMeteorAtPlayer` roll only ever succeeds for
  `p.dimension == 0` (Overworld). The NeoForge equivalent is checking the `ServerLevel`'s dimension key
  against `Level.OVERWORLD` before rolling that level's player pool. **Neo Edition's own port silently
  drops this gate** (`MeteorStrikeSystem.meteorUpdate` iterates every loaded level with no dimension
  filter at all) — a confirmed behavior divergence, not to be copied; see Open questions for the
  shower-timer implication.
- **`ArmorModHandler` integration**: use this port's own already-real, single-argument
  `ArmorModHandler.pryMods(ItemStack)` / `hasMods(ItemStack)` / `helmet_only` int constant (confirmed at
  `src/main/java/com/hbm/handler/ArmorModHandler.java:33,145,156`) — **not** Neo Edition's differently-
  shaped two-argument `pryMods(Level, ItemStack)` with a capitalized `HELMET_ONLY` constant. This port's
  own signature is already real, already compiling, and needs no `Level` parameter.
- **`ItemMeteorRemote` API shape**: `Item.Properties.stacksTo(1).durability(2)` constructor;
  `use(Level, Player, InteractionHand)` returning `InteractionResultHolder<ItemStack>`;
  `ItemStack.hurtAndBreak(int, LivingEntity, EquipmentSlot)` (slot from
  `LivingEntity.getSlotForHand(hand)`); `appendHoverText(ItemStack, TooltipContext, List<Component>,
  TooltipFlag)`. All confirmed via Neo Edition's `MeteorRemoteItem.java`, which is a faithful,
  unremarkable mechanical translation (no balance changes to flag), so mirroring its structure carries
  low risk even under this project's "never copy Neo Edition behavior" rule.
- **Reuse `ExplosionLarge.spawnRubble(Level, double, double, double, int)` /
  `spawnParticles(Level, double, double, double, int)`** — already real in this port with exactly the
  signature CE's `EntityMeteor.onUpdate()` needs for its cosmetic post-impact rubble/particle burst.

## Open questions / risks

- **CE's own multi-dimension meteor-shower-timer quirk.** `rollTheDice(World world)` is called once per
  loaded `World` per `WorldTickEvent` (`ModEventHandler.java:672`, one Forge event per loaded dimension
  per tick in 1.12), and `meteorUpdate`'s `meteorShower` counter is a single **shared static** `int`
  (`BossSpawnHandler.java:185`), not per-dimension. With N dimensions loaded, the shower duration decays
  N times per game tick instead of once, and the shower-start roll (`meteorStrikeChance * 100`) is
  independently re-rolled N times per tick too — even though the `p.dimension == 0` gate means the
  shower can only ever actually manifest in the Overworld. Confirmed by direct read of
  `BossSpawnHandler.java:37,185-225` and `ModEventHandler.java:662-672`. Neo Edition already "fixed"
  this by keying showers in a `Map<String, Integer>` per dimension — a real behavior change from CE, not
  a faithful port. **Decide explicitly**: replicate CE's shared-counter quirk faithfully, or adopt the
  per-dimension fix? Either is defensible; silently picking one without flagging it would be an
  unreviewed behavior change either way.
- **The "repelled" meteor still carries the instant-kill AoE.** When `protection_charm` sets
  `meteor.safe = true`, CE's `EntityMeteor.onUpdate()` still calls `Meteorite.generate(..., safe, true,
  true)` — the `damagingImpact` argument is a literal `true`, not gated by `safe` — so the 1000-damage,
  armor-bypassing `ModDamageSource.meteorite` AoE (±7.5 blocks) still fires at the meteor's new
  (redirected) landing spot. The item's own tooltip claims only "meteors no longer destroy blocks,"
  which is accurate for the explosion/path-clearing, but not for this AoE. Confirmed by direct read of
  `EntityMeteor.java:142` and `Meteorite.java:41-47`. Flag for behavior-parity sign-off rather than
  silently changing it either way.
- **Neo Edition's `Meteor.tick()` has a genuine structural bug worth knowing about (and not copying).**
  Its impact branch nests the `meteorite.generate(...)`/`clearMeteorPath`/sound/`discard()` calls
  *inside* `if (NtmConfig.COMMON.ENABLE_METEOR_TAILS.get())`, unlike CE (and its own earlier code),
  where only the cosmetic rubble/particle spawn is gated by that flag and the generate/discard calls are
  unconditional. If a server disables meteor tails, Neo Edition's ported meteor would explode via
  `level.explode(...)` on first touching ground, then never discard itself, and re-run that explosion
  check every subsequent tick indefinitely. This is exactly the kind of Neo-Edition-only logic this
  project's rules say never to copy — noted here so a future implementer recognizes the CE-faithful
  shape (unconditional generate + discard, tails flag only gates the cosmetic burst) rather than
  mechanically transcribing Neo Edition's file.
- **`techBleep`/`oldExplosion` sound-event port status** — this pass confirmed only
  `METEORITE_FALLING_LOOP` already exists in this port's `ModSounds.java`; the other two sounds
  `EntityMeteor`/`ItemMeteorRemote` reference were not checked against the full symbol list here.
  Verify before wiring the item/entity's `playSound` calls.
- **`ItemMeteorRemote`'s durability call happens before the `isRemote` branch** in CE
  (`stack.damageItem(1, player)` runs unconditionally, then server vs. client diverges). Confirm this
  port's `Item#use` is invoked in the same dual-logical-side pattern NeoForge normally uses (client
  predicts, server authoritative) before mechanically porting the exact call order, since a naive port
  could double-damage the item stack under some NeoForge dispatch timing.
- **Whether to build `protection_charm`/`meteor_charm` here or defer them.** They are small (each is a
  constructor call into the already-real `ItemArmorMod` plus ~10 lines of tooltip/`modDamage` logic),
  and without them this area's "repel" and "no-strike" branches are structurally present but
  unreachable/untestable in-game. This report recommends building both as part of this area's own
  implementation pass (see Phase-4-safe scope) rather than waiting on a separate armor-mod sweep, but
  flags the choice explicitly since `ModItems.java`/leaf-item registration is nominally a different
  area's territory in this port's phase split.
