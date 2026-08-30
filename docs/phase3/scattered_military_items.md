# Scattered Phase 3 items: RTTY/radar/rangefinder C2 cluster, drop-detonators, pocket nuke, loot crate — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/{ItemRTTYPager,ItemAmatExtractor,
  ItemCoordinateBase,ItemRadarLinker,ItemRangefinder}.java` (112/58/74/38/63 lines)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/network/RTTYSystem.java` (199 lines — the
  channel-broadcast network `ItemRTTYPager` reads/writes; note the real package is
  `com.hbm.tileentity.network`, not `com.hbm.handler` as this task's framing named it — corrected
  below, not invented)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/bomb/BlockCrashedBomb.java` (174 lines — the real
  package is `com.hbm.blocks.bomb`, not `com.hbm.blocks` as this task's framing named it) and its
  `EnumDudType`/`explode`/`onBlockActivated` surface, `ItemAmatExtractor`'s actual target
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/weapon/GunB92.java` (337 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/items/special/{ItemDrop,ItemUnstable,ItemLootCrate}.java`
  (244/133/83 lines, full)
- `upstream/hbm-ce/src/main/java/com/hbm/items/weapon/ItemMissile.java` (grep-targeted: the
  `Rarity` enum and its 6 values, confirming exactly what `ItemLootCrate.choose` weights against)
- This port's own already-committed code (confirmed real, not inferred): `com.hbm.items.ModItems`
  (registration shape, `DeferredRegister.Items`), `com.hbm.packet.HbmNetwork` +
  `com.hbm.packet.toclient.BufPacket` (full — the only concrete `CustomPacketPayload` in the port so
  far, and confirmation that **no `toserver` packet exists yet anywhere in this port**),
  `com.hbm.items.IItemControlReceiver` (already ported, one existing implementer:
  `com.hbm.items.tool.ItemToolAbility`), `com.hbm.items.HbmDataComponents` (the
  `DataComponentType.builder().persistent(...).networkSynchronized(...)` pattern, plus its own
  javadoc's explicit note that per-package component classes, not one central registry, is this
  port's convention so far), `com.hbm.items.special.ItemCell` (full — confirms the port's `ItemCell`
  has `isEmptyCell(ItemStack)`/`getFullCell(Item, FluidType, int)` but **not** CE's
  player-inventory-scanning `hasEmptyCell(EntityPlayer)`/`consumeEmptyCell(EntityPlayer)` helpers),
  `com.hbm.items.special.ItemCustomLore` (full — `ItemAmatExtractor`'s real, already-ported base
  class), `com.hbm.items.tool.ItemAnalysisTool` (full — confirmed working `useOn(UseOnContext)`
  example against `BlockDummyable`/`findCore`, the exact shape `ItemCoordinateBase`/`ItemRadarLinker`
  need), `com.hbm.capability.HbmLivingAttachment` + `ContaminationEffect` (full — confirms the
  radiation/contamination *data* exists but the CE `ContaminationUtil.contaminate(...)` *facade*
  `ItemAmatExtractor` calls does not, matching the gap `docs/phase1/items_special.md` and
  `docs/phase1/items_food_gear.md` already flagged for other items), `com.hbm.config.WeaponConfig`
  (full — confirms `DROP_CELL`/`DROP_SINGULARITY`/`DROP_CRYSTAL`/`DROP_DEAD_MANS_EXPLOSIVE` all
  already exist with the exact semantics `ItemDrop` needs), `com.hbm.blocks.BlockDummyable`
  (`findCore` signature only), `com.hbm.damage.ModDamageTypes` (confirms `NUCLEAR_BLAST` already
  registered for `ItemUnstable`)
- Repo-wide greps confirming absence: `ItemLootCrate`/`loot_10`/`loot_15`/`loot_misc`,
  `ItemMissile`, `gun_b92`, `detonator_deadman`/`detonator_de`, `ItemUnstable`, `EntityVortex`/
  `EntityBlackHole`/`EntityRagingVortex`, `ExplosionChaos`/`ExplosionLarge`, `ContaminationUtil`,
  `TileEntityMachineRadarScreen`/`IRadarCommandReceiver`, any `com.hbm.packet.toserver` package —
  **none exist anywhere in this port's `src/` yet**
- `docs/phase1/{items_tool.md, items_special.md}` (both name every file in this report's scope as
  "Phase 3" in a single line or short paragraph — this report is the actual scoping pass those lines
  promised), `docs/phase2/rbmk_reactor.md` (structural model for this report, and cross-checked for
  its own treatment of `ControlEventSystem`, a sibling "shared cross-cutting network system" problem
  to this report's `RTTYSystem` finding)
- Skimmed (headline/scope sections only, to confirm boundaries, not re-derived): the sibling Phase 3
  reports from this same wave — `docs/phase3/{gun_framework.md, guns_and_ammo.md,
  bomb_blocks_and_detonators.md, missile_framework.md, missile_launch_infra.md, explosion_engine.md,
  turret_system.md}` — confirming `guns_and_ammo.md` already reads `GunB92.java` in full and
  `missile_framework.md` already fully scopes `ItemMissile`, and confirming **no** sibling report
  this wave touches `ItemDrop`'s singularity half, `EntityVortex`/`EntityBlackHole`/
  `EntityRagingVortex`, or `RTTYSystem`

## Headline finding

This is nine CE files (`ItemRTTYPager`, `ItemAmatExtractor`, `ItemCoordinateBase`, `ItemRadarLinker`,
`ItemRangefinder`, `GunB92`, `ItemDrop`, `ItemUnstable`, `ItemLootCrate`) that fall out of every other
Phase 3 package's scope, and the reason they were left scattered turns out to be real rather than
accidental: each one is a small item whose entire reason for existing is a *one- or two-call reach*
into a system another package owns (a bomb block, a radar screen block entity, a missile item family,
a nuke-explosion entity), not a family of related content with its own internal logic worth a
dedicated package. Five of the nine (`ItemRTTYPager`/`ItemAmatExtractor`/`ItemCoordinateBase`/
`ItemRadarLinker`/`ItemRangefinder`) are genuinely one military-C2 item cluster and are grouped
together below; the other four are unrelated singletons, each with its own narrow dependency.

Two corrections to this task's own framing, both confirmed by reading the real source tree rather
than assumed:

1. **`RTTYSystem` lives at `com.hbm.tileentity.network.RTTYSystem`, not `com.hbm.handler.RTTYSystem`.**
   More importantly, it is not a weapon-package system at all — a repo-wide grep found **19 CE call
   sites** spanning RBMK console/keypad/lever/gauge/graph/numitron/terminal peripherals (Phase 2's own
   `docs/phase2/rbmk_reactor.md` Package B), the satellite save-data system (`Satellite`,
   `SatelliteRelay` — `docs/phase3/missile_launch_infra.md` territory), and a whole
   `com.hbm.tileentity.network` radio-torch/telex/pneumo-exporter redstone-over-radio family that no
   Phase 2 or Phase 3 report this wave has scoped. `ItemRTTYPager` is RTTYSystem's *only* item-level
   consumer, but RTTYSystem itself is cross-cutting shared infrastructure with far more owners than
   this one item — recommend treating it the same way `docs/phase2/rbmk_reactor.md` treated
   `ControlEventSystem`: whoever ports it first (likely whichever package needs it earliest — probably
   RBMK Package B or a dedicated "signals/telex" package, not this one) builds the real thing, and
   this package's own `ItemRTTYPager` scope is written below assuming only `RTTYSystem.listen`/
   `broadcast`'s two-method read/write surface exists, not the whole file.
2. **`BlockCrashedBomb` lives at `com.hbm.blocks.bomb.BlockCrashedBomb`, not `com.hbm.blocks`.** It is
   already fully in scope for `docs/phase3/bomb_blocks_and_detonators.md` (that report's own "Sources
   read in full" list names every file under `com.hbm.blocks.bomb`, including this one). This report
   does not re-scope the block; it only documents `ItemAmatExtractor`'s one-method call into it
   (`((BlockCrashedBomb) ...).explode(world, pos, player)`, and the `EnumDudType` metadata read for
   flavor), which is a strict subset of what `bomb_blocks_and_detonators.md` already owns in full.

Per this task's explicit instruction, `GunB92.java` is read here only to confirm the boundary, not
re-scoped: `docs/phase3/guns_and_ammo.md`'s own "Sources read in full" list already includes
`com.hbm.items.special.weapon.GunB92.java` verbatim, with the same "misfiled — `gun_b92` actually
lives here" observation this report would otherwise make. Nothing below duplicates that work.

## Phase-3-safe scope

### Cluster 1 — Military C2 items (5 CE classes)

| CE class | Lines | What it does | Real dependency |
|---|---|---|---|
| `ItemCoordinateBase` (abstract) | 74 | Generic "shift-right-click a block to save its coordinates onto this item" base. `onItemUse`→`useOn(UseOnContext)`: if `canGrabCoordinateHere` passes, stores `posX`/`posY`(optional)/`posZ` as NBT ints, calls the `onTargetSet` hook. `addInformation` prints the saved coordinates or "No position set!". Two `protected`-shaped extension points (`canGrabCoordinateHere`, `getCoordinates`, `includeY`, `onTargetSet`) are exactly what `ItemRadarLinker` overrides — this port's `docs/phase1/items_tool.md` already noted `ItemCoordinateBase` "is only used by `ItemRadarLinker` in this package" and travels with it. |
| `ItemRadarLinker` | 38 | Concrete `ItemCoordinateBase`: `canGrabCoordinateHere` accepts a clicked block only if `CompatExternal.getCoreFromPos(world, pos)` resolves to an `IRadarCommandReceiver` or `TileEntityMachineRadarScreen`; `getCoordinates` re-resolves to that core's own position (so clicking any dummy face of the multiblock still saves the *core* coordinate, not the clicked face); `onTargetSet` plays `HBMSoundHandler.techBleep`. | `IRadarCommandReceiver`/`TileEntityMachineRadarScreen` — **confirmed absent from this port** (see Deferred scope). `CompatExternal.getCoreFromPos`'s job is already covered by this port's own `BlockDummyable`/`IPersistentNBT` multiblock-core-lookup machinery (confirmed real, see Key design decisions) — no new lookup mechanism needed. |
| `ItemRangefinder` | 63 | Not a `ItemCoordinateBase` subclass (plain `Item`) — right-click ray-traces up to 200 blocks and messages the player the hit distance via a one-shot `PlayerInformPacket`; `META_POLARIZED` (an old damage-value variant, meta `1`) tints both the message and the item's display name light-purple. | None beyond `PlayerInformPacket` (a toclient inform-toast packet — check whether the port's `com.hbm.packet.toclient` package already has an equivalent chat/toast packet before writing a new one; not confirmed present or absent by this survey, flag as a quick check). `META_POLARIZED` needs the standard post-1.13 metadata flattening: two distinct registry entries (e.g. `hbm:rangefinder`, `hbm:rangefinder_polarized`), not a damage-value branch. |
| `ItemRTTYPager` | 112 | Implements both `IItemControlReceiver` (already-ported interface — the pager's channel-selection GUI writes a `chan` string into the stack via `receiveControl`) and `IGUIProvider` (opens `GUIScreenPager` on right-click). Its `onUpdate` polls `RTTYSystem.listen(world, channelFreq)` every tick; if the channel's last-tick signal is the literal string `"selfdestruct"`, it fires a real `ExplosionVNT` (`EntityProcessorCrossSmooth` piercing, `ExplosionEffectWeapon` SFX, radius 5) centered on the holding entity and consumes the pager — this is the one piece of actual weapon behavior in the class, everything else is inert radio-pager flavor (any other signal just prints a HUD toast via `PlayerInformPacketLegacy`). | `RTTYSystem.listen`/`broadcast` (see Headline finding #1 — recommend depending on just this two-method surface, not porting the whole file here); `com.hbm.explosion.vanillant.*` (`ExplosionVNT`, `EntityProcessorCrossSmooth`, `ExplosionEffectWeapon`, `PlayerProcessorStandard`) — **already fully scoped by `docs/phase3/bomb_blocks_and_detonators.md`/`explosion_engine.md`**, consume from there, don't re-derive; the GUI/menu-opening half (`IGUIProvider`→`GUIScreenPager`) needs this port's real Menu/Screen framework (confirmed real, see Key design decisions) plus a **new client→server control packet** (see Key design decisions — this is the one genuinely new infrastructure piece this cluster needs). |
| `ItemAmatExtractor` | 58 | `extends ItemCustomLore` (already ported in this port, confirmed). `onItemUse`→`useOn(UseOnContext)`: if the clicked block is a `BlockCrashedBomb` and the player holds an empty `ItemCell`, rolls a `world.rand.nextFloat()`: 1% chance the bomb detonates in place (`BlockCrashedBomb.explode`, a strict `bomb_blocks_and_detonators.md` call-through), else a weighted split between a rare `cell_balefire` reward (chance ≤ 0.30) and a normal full AMAT cell (`ItemCell.getFullCell(..., Fluids.AMAT)`) — then contaminates the player 50 RAD via `ContaminationUtil.contaminate(...)`. | `ItemCell.hasEmptyCell(EntityPlayer)`/`consumeEmptyCell(EntityPlayer)` — **confirmed missing from the port's `ItemCell`** (it has `isEmptyCell(ItemStack)` and `getFullCell(Item, FluidType, int)` but no player-inventory-scanning variant); this is a small, real, concrete addition this package needs to make to `ItemCell` itself, not a new subsystem. `ContaminationUtil.contaminate(...)` — confirmed absent (see Deferred scope, same gap already flagged by `docs/phase1/items_special.md`/`items_food_gear.md` for other items). `ModItems.cell_balefire`/`BlockCrashedBomb` — real registry-shape dependencies on Phase 1 material items and `bomb_blocks_and_detonators.md` respectively, both expected to exist by the time this ships. |

All five are **item-shell-portable now**: registration, `useOn`/`onItemRightClick` control flow, and
tooltips need nothing this package can't already stand on. The two real blockers
(`IRadarCommandReceiver`/`TileEntityMachineRadarScreen` for the radar linker's *usefulness*, and
`ContaminationUtil` for the amat extractor's *contamination side effect*) are narrow and named, not
structural — see Deferred scope. `ItemRTTYPager`'s "just print a HUD toast" branch (everything except
the `selfdestruct` signal) can ship the moment `RTTYSystem.listen` exists in any form, even a minimal
stub with no real network behind it yet.

### `ItemDrop`'s detonator half (2 of the class's ~9 conditionally-branched CE fields)

Per this task's own framing (and `docs/phase1/items_special.md`'s recommendation to split the class):
reading `ItemDrop.java` in full confirms the exact split is clean along item-identity lines, since
every branch in the class is a `stack.getItem() == ModItems.<field>`/`this == ModItems.<field>` check,
not shared logic:

- **`detonator_deadman`**: `useOn(UseOnContext)` (sneak-right-click a block to save its `x`/`y`/`z`
  as plain NBT ints onto the stack, plus a `techBoop` sound and a chat message) and
  `onEntityItemUpdate` (when dropped: if the saved position holds an `IBomb` block, calls its
  `explode(world, pos, thrower)`; either way, triggers a small vanilla `world.createExplosion` at the
  drop point and despawns). Tooltip branch included.
- **`detonator_de`**: `onEntityItemUpdate` only — when dropped and `WeaponConfig.dropDead` is true
  (confirmed already wired in this port's `WeaponConfig`, see Key design decisions), triggers a
  15-block-radius vanilla explosion at the drop point and despawns. Tooltip branch included.
- **The trivial third case, `beta`**: `onEntityItemUpdate`'s very first check (`this == ModItems.beta`
  → `entityItem.setDead()`, no explosion, no config gate) is neither a detonator nor a singularity
  drop — it's a two-line "this item silently vanishes when dropped" flavor rule with zero weapon
  behavior. It belongs wherever `ModItems.beta` (a flavor material, presumably a beta-particle sample)
  is otherwise registered, and should not block or be blocked by either half of this split; flagging
  it explicitly here only so it isn't lost between the two halves during the port.

Both detonator items are self-contained once `IBomb` (already real, `bomb_blocks_and_detonators.md`'s
own dependency list confirms `com.hbm.interfaces.IBomb` is committed code in this port) and a small
`BlockPos`-shaped data component exist (see Key design decisions — the exact same coordinate-store
pattern `ItemCoordinateBase` needs, `docs/phase1/items_tool.md`'s NBT→component notes already flagged
this as a good shared-component candidate).

### `ItemUnstable` — pocket nuke (1 class, `setHasSubtypes(true)` → needs flattening)

133 lines, `extends ItemBakedBase` in CE (superseded in the port by extending `Item`/`ItemBase`
directly per this port's established convention — CE's dynamic-model plumbing on this base class is
dead weight post-1.13, matching every other `ItemBakedBase` subclass already ported in this repo).
Per-tick countdown item (`onUpdate`/`onEntityItemUpdate`, both held-in-inventory and dropped-on-ground
paths implemented near-identically): once a `timer` NBT counter reaches the constructor-supplied
`timer` value, it consumes the stack (`IItemHandler.extractItem` when held; `setDead()` when a dropped
entity), spawns `EntityNukeExplosionMK5.statFac(...)` at a **stack-count-scaled radius**
(`scaledRadiusForCount`: `radius * cbrt(count)`, so a full stack of pocket nukes is dramatically more
dangerous than one), optionally spawns a mushroom cloud (`BombConfig.enableNukeClouds` — already a
real config in this port), plays `oldExplosion`, and deals 10,000 `ModDamageSource.nuclearBlast`
damage to whoever was holding/threw it. `EnumDudType`-style meta 1/2/3 give special display names
("ELEMENTS"/"ARSENIC"/"VAULT") with **no explosion behavior at all** (the `stack.getItemDamage() != 0`
guard at the top of every method short-circuits everything for those three) — these read as inert
"decoration" variants of the same base item, not additional pocket-nuke tiers.

**Flattening note**: CE constructs `ItemUnstable` multiple times with different `(radius, timer)`
pairs across `ModItems.java` (not surveyed here — that census belongs to whichever area owns
`ModItems.java`'s full field list) — each constructor call is already a distinct registry entry today,
metadata is used only for the 4 damage-value variants of *each* named instance (the live pocket-nuke
behavior at meta 0, plus the three inert reskins at meta 1–3). Recommend the same treatment as
`docs/phase1/items_food_gear.md`'s `ItemTemFlakes` call: flatten each `(base item, meta)` pair into
its own registry entry (e.g. `hbm:<name>`, `hbm:<name>_elements`, `hbm:<name>_arsenic`,
`hbm:<name>_vault`), carrying `radius`/`timer` as constructor-time constants per entry rather than a
runtime field, and drop the `getItemDamage()` runtime branching entirely once each variant is its own
item.

**Real dependencies, both already scoped elsewhere, not re-derived here**: `EntityNukeExplosionMK5`/
`EntityNukeTorex` are `docs/phase3/explosion_engine.md`'s own "Sources read in full" list (confirmed);
`ModDamageSource.nuclearBlast` already exists in this port as `ModDamageTypes.NUCLEAR_BLAST` (confirmed
by direct read — no new `DamageType` entry needed, just the `DamageSource` construction site, which is
this item's own job, not `ModDamageTypes`'s).

### `ItemLootCrate` — shell does **not** yet exist (correcting `docs/phase1/items_special.md`)

`docs/phase1/items_special.md`'s per-file table says: *"Register the shell now; the loot table
population necessarily waits on Phase 3 weapons."* **A repo-wide grep confirms this was a
recommendation that was never carried out**: there is no `ItemLootCrate.java`, and no
`loot_10`/`loot_15`/`loot_misc` reference, anywhere in this port's `src/` today. Flagging this
explicitly since the task asked to confirm it, and the honest answer is "recommended, not done."

The class itself (83 lines) is genuinely trivial and has no dependency beyond what's already real:
3 `Item` instances (`loot_10`/`loot_15`/`loot_misc`), each right-clicking to consume the stack and
grant one random `ItemMissile` drawn from a matching static `List<ItemMissile>`
(`list10`/`list15`/`listMisc`) via a rarity-weighted rejection loop (`ItemMissile.Rarity`: `COMMON`
always accepted, `UNCOMMON` 1-in-5, `RARE` 1-in-10, `EPIC` 1-in-25, `LEGENDARY` 1-in-50, and one joke
tier at 1-in-100 — confirmed by direct read of `ItemMissile.java`'s `Rarity` enum, 6 constants). The
three lists themselves are populated elsewhere in CE (population call sites not surveyed here — out
of scope, since they're keyed off `ItemMissile` instances that don't exist yet either).

**Register the three item shells now** (trivial, zero real dependency) but leave `list10`/`list15`/
`listMisc` empty (or ship the `choose`/`onItemRightClick` logic behind a "list not yet populated, no-op"
guard) until `docs/phase3/missile_framework.md`'s package lands `ItemMissile` and its `Rarity` field —
confirmed by direct read of that report's own "Sources read in full" list, which already includes
`com.hbm.items.weapon.ItemMissile.java` (416 lines) in full. This report does not re-scope
`ItemMissile` itself; it only documents the exact shape `ItemLootCrate` needs from it (a `rarity` field
of the 6-constant enum above) so whoever picks up the loot crate can wire the two together without
re-reading `ItemMissile.java` from scratch.

### `GunB92` — misfiled, boundary only (not re-scoped)

`com.hbm.items.special.weapon.GunB92` (337 lines) is a full charge-up energy rifle: `onPlayerStoppedUsing`
fires 1–10 `EntityExplosiveBeam` projectiles scaled by charge time, `onUpdate` drives a 30-tick fire
animation plus a "prestige" mechanic where 10 stored charges triggers a real `EntityNukeExplosionMK3` +
`EntityCloudFleijaRainbow` easter egg, and it stores `animation`/`energy` as NBT ints (both already
listed in `docs/phase1/items_special.md`'s NBT→component table). **This is fully in scope for
`docs/phase3/guns_and_ammo.md`**, whose own "Sources read in full" list already names this exact file
and already flags the same "misfiled — lives in `special/weapon/`, not `items/weapon/`" observation.
Per this task's explicit instruction, this report does not re-scope the gun itself; it exists here
only to record the boundary decision (this file belongs entirely to the gun-framework-adjacent
content package, zero split) so a future reader doesn't wonder why a "scattered items" report skipped
a file that so obviously matches its theme.

## Deferred scope

- **`RTTYSystem`'s full channel-broadcast network** (`com.hbm.tileentity.network.RTTYSystem`, 199
  lines) — confirmed absent from the port. Real consumer count across CE: 19 files spanning RBMK
  console peripherals (Phase 2's own Package B per `docs/phase2/rbmk_reactor.md`), the satellite
  save-data system (`docs/phase3/missile_launch_infra.md` territory), and an entirely unscoped
  `com.hbm.tileentity.network` radio-torch/telex family. Recommend whoever ports this treats it as
  shared cross-cutting infrastructure (like `ControlEventSystem` in the RBMK report) rather than
  bundling it into this items package; `ItemRTTYPager`'s own scope above only needs the two-method
  `listen`/`broadcast` surface, which could ship as a minimal stub (a plain
  `ConcurrentHashMap<Pair<Level,String>, Object>` with no world-tick melody generator) well ahead of
  the real thing.
- **`IRadarCommandReceiver`/`TileEntityMachineRadarScreen`** — confirmed absent (no `*Radar*` block
  entity anywhere in `src/`, only the unrelated `com.hbm.api.entity.IRadarDetectable(NT)`/`RadarEntry`
  interfaces used by turret/missile targeting, a different "radar" concept entirely). `ItemRadarLinker`
  can register and its `useOn` logic can compile against these two types as forward references, but it
  has literally nothing to link to until whichever package owns the physical radar-screen multiblock
  ships. Not named by any Phase 2 or Phase 3 report surveyed this wave — flag as an unowned gap, likely
  belongs with whichever future package covers long-range detection/C2 machine blocks.
- **`ContaminationUtil` (`com.hbm.util.ContaminationUtil`)** — confirmed absent, same gap already
  named by `docs/phase1/items_special.md` and `docs/phase1/items_food_gear.md` for unrelated items.
  `ItemAmatExtractor`'s core extraction mechanic (roll the reward, consume the empty cell, hand back
  AMAT or `cell_balefire`) does not need it and can ship now; only the 50-RAD contamination side effect
  is blocked, and should be left as a documented `// TODO(ContaminationUtil follow-up)` in the same
  style already established by this port's own `ItemEnergy.java`, not silently dropped.
- **`ItemDrop`'s harmless singularity/xen half** (`pellet_antimatter`, `singularity`,
  `singularity_counter_resonant`, `singularity_super_heated`, `black_hole`, `singularity_spark`,
  `capsule_xen`, `crystal_xen`) — this is the half `docs/phase1/items_special.md` called "P1-safe,
  depend on entity/explosion effect classes existing" and Phase 1 correctly declined to port for
  exactly that reason (confirmed: none of `ItemDrop.java`, `EntityVortex`, `EntityBlackHole`,
  `EntityRagingVortex`, or `ExplosionChaos` exist anywhere in this port today). **This report also
  does not claim it** — a check against every sibling Phase 3 report from this wave found none of
  them scope `EntityVortex`/`EntityBlackHole`/`EntityRagingVortex` (a small "gravity well" entity
  family under `com.hbm.entity.effect`, distinct from the `EntityNukeTorex` cloud entity
  `explosion_engine.md` does cover), and `docs/phase3/explosion_engine.md` explicitly flags
  `ExplosionChaos` (the `capsule_xen`/`crystal_xen` engine) as signature-surveyed only, with "a
  dedicated follow-up read" recommended before anyone implements it. **Net effect: this half of
  `ItemDrop` remains genuinely unowned by any package this wave** — recommend a future entity/effect
  survey (paired with whoever finally reads `ExplosionChaos` in full) pick it up, rather than either
  this package or `explosion_engine.md` silently absorbing it by proximity.
- **`ItemUnstable`'s exact `(radius, timer)` constructor-call census across `ModItems.java`** — not
  surveyed here (that's a `ModItems.java`-wide census job, out of this report's file list), but
  whoever flattens the metadata variants (see Phase-3-safe scope above) will need it to know how many
  total registry entries result.
- **`ItemLootCrate`'s `list10`/`list15`/`listMisc` population call sites in CE** — not surveyed (they
  live outside the 9 files this report was scoped to, and are meaningless before `ItemMissile` exists
  regardless). Whoever finishes wiring the loot crate after `missile_framework.md` lands `ItemMissile`
  should grep CE for `ItemLootCrate.list10.add`/`.list15.add`/`.listMisc.add` at that time.
- **`PlayerInformPacket`/`PlayerInformPacketLegacy`-equivalent toclient toast packet** — needed by
  `ItemRangefinder` (distance readout) and `ItemRTTYPager` (channel HUD toast). Not confirmed present
  or absent by this survey (a `com.hbm.packet.toclient` package exists per `HbmNetwork`'s own
  javadoc, but this report did not enumerate its full contents) — a five-minute check before
  implementation, not a real blocker either way.

## Key design/API decisions

Confirmed from real code read in this survey (CE for behavior, this port's own committed code for
NeoForge API shape — no NeoForge API is invented below):

- **Coordinate-store items use `useOn(UseOnContext)`, not a 1.12-shaped `onItemUse` override.**
  Confirmed against this port's own already-working `com.hbm.items.tool.ItemAnalysisTool` (read in
  full): `Player player = context.getPlayer(); Level level = context.getLevel(); BlockPos pos =
  context.getClickedPos();`, returning `InteractionResult.SUCCESS`/`.PASS`. `ItemCoordinateBase`,
  `ItemRadarLinker`, and `ItemDrop`'s `detonator_deadman` branch all map onto this exact shape
  directly — no new pattern needed, follow `ItemAnalysisTool` line-for-line.
- **`BlockDummyable.findCore(Level, BlockPos)` is the confirmed, already-real replacement for CE's
  `CompatExternal.getCoreFromPos`.** `ItemAnalysisTool` already calls it this exact way
  (`dummy.findCore(level, pos)`); `ItemRadarLinker.canGrabCoordinateHere`/`getCoordinates` should use
  the identical call rather than reinventing a core-lookup helper — this port's own multiblock
  framework already solved this problem once.
- **A shared `DataComponentType<BlockPos>` is the right replacement for the repeated `x`/`y`/`z` (or
  `posX`/`posY`/`posZ`) NBT-int-triplet pattern**, not three separate int components per item.
  `docs/phase1/items_tool.md`'s own NBT→component notes already called this out as "a good candidate
  for one shared BlockPos-holding component type used by many items" across `ItemWrench`,
  `ItemDetonator`, `ItemCoordinateBase`, `ItemWand`, and `ItemBoltgun`'s pending-position stash — this
  report adds `ItemDrop`'s `detonator_deadman` to that same list (identical `x`/`y`/`z` int pattern).
  Confirmed buildable today: vanilla `net.minecraft.core.BlockPos` ships both `BlockPos.CODEC` (for
  the `persistent(...)` half) and a stream codec (for `networkSynchronized(...)`), and this port's own
  `com.hbm.items.HbmDataComponents` (read in full) already establishes the exact registration pattern
  — `DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MODID)` +
  `DataComponentType.builder().persistent(codec).networkSynchronized(streamCodec).build()` — with an
  explicit note in its own javadoc that per-package component classes (not one central registry) are
  this port's convention so far. `ItemCoordinateBase`'s `includeY()` flag (radar linker always saves
  Y; a future item might not) means the shared component should hold a full `BlockPos`, not drop Y at
  the codec level — omit-Y items can simply store `pos.getY()`-zeroed or store a separate flag, a
  small implementation detail left to whoever builds it.
- **`IItemControlReceiver` already exists and is already implemented once** (`ItemToolAbility`,
  confirmed by direct read: `receiveControl(ItemStack stack, CompoundTag data)`). `ItemRTTYPager`
  should implement the same already-real interface, not a new one — this cluster needs zero new
  interface work here, only the packet that calls it (next point).
- **No client→server (`toserver`) packet exists anywhere in this port yet** — confirmed by directory
  listing (`com.hbm.packet` has no `toserver` package at all; `HbmNetwork`'s own javadoc explicitly
  says it registers zero concrete packets beyond the one `toclient` `BufPacket`). CE's own
  `GUIScreenPager` (read in full) sends the pager's channel string to the server via a generic
  `com.hbm.packet.toserver.NBTItemControlPacket(NBTTagCompound)` that dispatches to whatever the
  player's held item's `IItemControlReceiver.receiveControl` implements — this is exactly the shape
  `ItemToolAbility`'s own (not-yet-built) ability-config GUI will eventually need too, so **this is a
  good candidate for one shared, generically-named payload** (e.g. `ItemControlPacket(CompoundTag
  data)`, resolving the sender's held item server-side rather than encoding a slot/hand index that
  could desync) rather than a pager-specific one-off. Concrete shape, following the confirmed
  `RegisterPayloadHandlersEvent`/`StreamCodec` pattern from this port's own `HbmNetwork`+`BufPacket`
  (read in full): a `record ItemControlPacket(CompoundTag data) implements CustomPacketPayload` with
  a `StreamCodec<RegistryFriendlyByteBuf, ItemControlPacket>` built on `ByteBufCodecs.COMPOUND_TAG` (or
  the trusted-NBT-size variant, matching whatever the block-entity sync packet already uses),
  registered via `registrar.playToServer(...)` in `HbmNetwork` (the mirror image of the existing
  `playToClient(BufPacket.TYPE, ...)` line), and a handler that reads `context.player()`'s held item
  in both hands and dispatches to whichever one implements `IItemControlReceiver`. This is this
  cluster's one genuinely new piece of shared infrastructure — small, but real, and worth building
  once rather than once per item.
- **`ItemCell`'s player-inventory helpers are a small, concrete gap, not a design question.** CE's
  `ItemCell.hasEmptyCell(EntityPlayer)`/`consumeEmptyCell(EntityPlayer)` (confirmed present in CE,
  absent from the port's `ItemCell`) simply scan `player.inventory`/the player's `Inventory` for a
  stack satisfying `isEmptyCell`. This is a straightforward addition to the port's existing
  `com.hbm.items.special.ItemCell` (already real, already has the underlying `isEmptyCell(ItemStack)`
  primitive) — recommend `ItemAmatExtractor`'s own package add these two static methods to `ItemCell`
  directly rather than duplicating an inventory scan locally, since any future item needing "does the
  player have a spare cell" will want the same helper.
- **`ModDamageTypes.NUCLEAR_BLAST` already exists and is the confirmed target for
  `ModDamageSource.nuclearBlast`** — `ItemUnstable` needs no new `DamageType` datapack entry, only a
  `level.damageSources().source(ModDamageTypes.NUCLEAR_BLAST, ...)` call site at the point of use,
  per this port's own established damage-source-dispatch convention (`ModDamageTypes`'s own javadoc:
  "Building an actual `DamageSource` instance from one of these keys is the responsibility of the area
  that owns the corresponding attack").
- **`WeaponConfig`'s drop-behavior toggles are already fully wired** — `DROP_CELL`, `DROP_SINGULARITY`,
  `DROP_CRYSTAL`, `DROP_DEAD_MANS_EXPLOSIVE` all exist in this port's `com.hbm.config.WeaponConfig`
  today with matching CE-config-key comments (confirmed by direct read), so `ItemDrop`'s config gates
  need zero new config plumbing, just the `if (WeaponConfig.DROP_DEAD_MANS_EXPLOSIVE.get()) { ... }`
  call sites.
- **`EntityItem.onEntityItemUpdate` is confirmed already re-expressed against the modern signature**
  in this port (`ItemCell.onEntityItemUpdate(ItemStack stack, ItemEntity entity)`, read in full) —
  `ItemDrop` and `ItemUnstable` should follow that exact override shape (instance method on `Item`,
  `ItemStack` + `ItemEntity` parameters, `boolean` return), not CE's 1.12 `EntityItem`-only signature.

## Open questions / risks

- **`RTTYSystem`'s ownership is genuinely unassigned.** This report deliberately does not claim it
  (it's shared infrastructure with 19 CE consumers spanning at least three future packages), but
  nothing surveyed this wave claims it either. Whoever schedules RBMK Package B or the satellite
  system should explicitly decide who ports the base `RTTYSystem` class, since `ItemRTTYPager` (this
  package), the RBMK console peripherals, and the satellite relay system all silently assume it
  exists. A minimal stub (bare `listen`/`broadcast` over a `ConcurrentHashMap`, no melody generator,
  no per-world semantics fixed) is enough to unblock `ItemRTTYPager`'s non-`selfdestruct` path today
  without committing to the real network's final shape.
- **`ItemDrop`'s singularity/xen half has no owner this wave.** Flagged loudly in Deferred scope
  above — repeating here because it's the kind of gap that's easy for a future reader to assume
  "must be covered by explosion_engine.md since it's explosion-adjacent" when it explicitly is not
  (that report's own text says `ExplosionChaos` needs "a dedicated follow-up read" before anyone
  implements it, and `EntityVortex`/`EntityBlackHole`/`EntityRagingVortex` aren't mentioned by name in
  any Phase 2 or Phase 3 report surveyed).
- **`ItemRangefinder`'s `META_POLARIZED` naming.** CE has no name for the polarized variant beyond a
  meta value and a color tint — whoever flattens it needs to pick a registry id (this report suggests
  `hbm:rangefinder_polarized` as the obvious choice, but it's not fixed by any CE source).
- **`ItemUnstable`'s per-instance `(radius, timer)` construction means this report cannot state an
  exact final registry-entry count** without the `ModItems.java`-wide census called out in Deferred
  scope — the *shape* of the flattening (each existing `(base, meta)` pair becomes one entry) is
  confirmed, but the *count* is not.
- **`GUIScreenPager`'s 184×42 texture-based rendering** is 1.12 `GuiScreen`/`drawTexturedModalRect`
  code (read in full for the packet-dispatch pattern only) and will need a full rewrite against this
  port's real `GuiInfoContainer`/`AbstractContainerScreen` framework when the pager's interactive GUI
  is actually implemented — not a blocker for registering the item, but flagged so "port the pager"
  isn't assumed to be a five-minute job once the control packet exists.
- **Whether `ItemRTTYPager`'s GUI needs a real `AbstractContainerMenu` at all is an open call.** CE's
  own `provideContainer` returns `null` unconditionally (the pager's GUI is a bare `GuiScreen`, no
  server-side `Container`/inventory slots) — this maps more naturally onto a menu-less client screen
  opened via item-use than onto this port's `MenuBase`/`GuiInfoContainer` machine-GUI pattern (which
  assumes a real `AbstractContainerMenu` bound to a `MachineBaseBlockEntity`). Recommend confirming
  NeoForge 1.21.1's supported way to open a menu-less item-driven client screen (rather than forcing
  a trivial no-slot `AbstractContainerMenu` just to fit the existing machine-GUI pattern) before
  implementing, since neither `MenuBase` nor `GuiInfoContainer`'s own docs address this no-menu case.
