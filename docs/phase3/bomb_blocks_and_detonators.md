# Bomb blocks & detonator items — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/bomb/**/*.java` — all 48 files (every class:
  `Balefire`, `BlockC4`, `BlockChargeBase`, `BlockChargeC4`, `BlockChargeDynamite`,
  `BlockChargeMiner`, `BlockChargeSemtex`, `BlockCloudResidue`, `BlockCrashedBomb`,
  `BlockDetonatable`, `BlockDynamite`, `BlockFireworks`* , `BlockFissureBomb`,
  `BlockPlasticExplosive`* , `BlockSemtex`, `BlockTNT`, `BlockTNTBase`, `BlockTaint`, `BlockVolcano`
  (signature-level), `BombFlameWar`, `BombFloat`, `BombMulti`, `BombThermo`, `CheaterVirus`,
  `CheaterVirusSeed`, `CompactLauncher`, `CrystalPulsar`, `CrystalVirus`, `DetCord`, `DetMiner`,
  `DigammaFlame`, `DigammaMatter`, `Landmine`, `LaunchPad`, `LaunchPadLarge`, `LaunchPadRusted`,
  `LaunchTable`, `NukeBalefire`, `NukeBoy`, `NukeCustom`, `NukeFleija`, `NukeGadget`, `NukeMan`,
  `NukeMike`, `NukeN2`, `NukePrototype`, `NukeSolinium`, `NukeTsar`, `RailgunPlasma`).
  (*`BlockFireworks`/`BlockPlasticExplosive` are cosmetic/crafting-material blocks with no `IBomb`
  or detonator involvement — confirmed by class list only, not read line-by-line; they carry no
  bearing on the detonation protocol this report maps.)
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/bomb/**/*.java` — all 23 files, every one paired
  to a block above: `TileEntityCharge`, `TileEntityLandmine`, `TileEntityCrashedBomb`,
  `TileEntityBombMulti`, `TileEntityNukeBoy/Gadget/Man/Mike/N2/Prototype/Solinium/Fleija/Tsar/Custom/
  Balefire`, `TileEntityCompactLauncher`, `TileEntityRailgun`, `TileEntityLaunchPad`,
  `TileEntityLaunchPadBase`, `TileEntityLaunchPadLarge`, `TileEntityLaunchPadRusted`,
  `TileEntityLaunchTable`, `TileEntityFireworks`* (not read, matches the excluded block above).
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/{ItemDetonator,ItemMultiDetonator,
  ItemLaserDetonator,ItemDefuser}.java` (full) + their real dependency chain, also read in full:
  `com.hbm.interfaces.IBomb`, `com.hbm.api.block.{IToolable,IExploder}`,
  `com.hbm.items.tool.ItemTooling`, `com.hbm.items.armor.ItemModDefuser`, `com.hbm.main.ModContext`.
- `com.hbm.explosion.vanillant.**` (full): `ExplosionVNT`; all 10 interfaces (`IBlockAllocator`,
  `IBlockProcessor`, `IEntityProcessor`, `IPlayerProcessor`, `IExplosionSFX`,
  `ICustomDamageHandler`, `IDropChanceMutator`, `IFortuneMutator`, `IBlockMutator`,
  `IEntityRangeMutator`); `standard/BlockAllocatorStandard`, `standard/BlockProcessorStandard` (the
  two classes that actually allocate/remove blocks — read in full to ground the explosion-performance
  finding below). The other `standard/*` classes (entity/player processors, SFX, damage handlers)
  were surveyed by signature, not read line-by-line — they are pure per-entity-damage/particle glue
  with no bearing on the detonation protocol or block-removal performance question this report
  targets.
- `com.hbm.entity.item.EntityTNTPrimedBase` (full, 165 lines) — the one entity every conventional
  explosive block in this package depends on. `com.hbm.explosion.ExplosionNukeRayBatched` (partial,
  ~90 lines) — read only to confirm CE's own per-chunk batching shape, cited in the
  design-decisions section below, not itself in this package's scope.
- This port's own already-committed code (confirmed real, not inferred): `com.hbm.interfaces.IBomb`,
  `com.hbm.api.block.{IToolable,IExploder}`, `com.hbm.items.tool.ItemTooling`,
  `com.hbm.items.tool.ToolDataComponents`, `com.hbm.config.BombConfig`,
  `com.hbm.damage.ModDamageTypes`, `com.hbm.packet.HbmNetwork`,
  `com.hbm.inventory.container.MenuBase`, `com.hbm.inventory.gui.GuiInfoContainer`,
  `com.hbm.blockentity.{LoadedBaseBlockEntity,MachineBaseBlockEntity}`,
  `com.hbm.entity.ConveyorEntityTypes` (the one existing `EntityType` registration in this port),
  `com.hbm.blocks.BlockDummyable`, `com.hbm.handler.MultiblockHandlerXR`.
- `docs/phase1/items_tool.md` bucket (b) (the section that named these four detonator/defuser files
  as Phase 3 scope), `docs/phase2/rbmk_reactor.md` (structural model for this report, and the source
  of the "`com.hbm.entity` package is entirely absent" finding this report re-confirms and narrows to
  the specific entities the nuke casings need).
- `upstream/neo-edition/src/main/java/com/hbm/items/tools/{DetonatorItem,LaserDetonatorItem}.java`
  and `com.hbm.explosion.ExplosionNukeRayBatched` (Neo Edition's version) — cross-referenced **only**
  for confirmed real NeoForge 1.21.1 `Item#useOn`/`Item#use` shapes and per-chunk batching precedent,
  per this project's standing rule that Neo Edition is not a behavior source.

## Headline finding

Three things worth stating up front, because they reframe what "map the remote-detonation network
protocol" and "the defuse mechanic" actually mean once the real code is read, rather than assumed:

1. **There is no network protocol to map, in the packet sense.** CE's detonators do not send any
   custom packet, do not register a listener on placed bombs, and do not maintain any registry of
   "known bombs." The entire mechanism is a same-tick, server-side `Item` callback
   (`onItemUse`/`onItemRightClick`, already routed to the server by vanilla's own interaction
   packets) that (a) writes/reads a `BlockPos` to/from the detonator `ItemStack`'s own NBT, and (b)
   does a direct `world.getBlockState(pos).getBlock() instanceof IBomb` check followed by
   `((IBomb)block).explode(world, pos, player)`. `ItemLaserDetonator` skips the NBT step entirely and
   raytraces live. This port's `com.hbm.packet.HbmNetwork` (the `CustomPacketPayload`/
   `RegisterPayloadHandlersEvent` registry) needs **zero new payloads** for the detonator-trigger path
   itself — the only reason a real packet would ever be needed is a cosmetic one (Neo Edition's
   `LaserDetonatorItem` adds a client-side spark-trail particle effect that isn't present in CE and
   is not required for parity).
2. **"Defuse" is two unrelated mechanics that happen to share an item name.** (a) The actual
   "safely remove a placed bomb without it exploding" mechanic is generic `IToolable.ToolType.
   DEFUSER` dispatch through `ItemTooling` (already fully working in this port — see Key design/API
   decisions) — every bomb block that supports it implements its own `onScrew(..., ToolType.
   DEFUSER)` branch. `ItemDefuser` itself contributes **zero** lines to that path; it inherits it
   for free from the `ItemTooling` superclass. (b) `ItemDefuser.itemInteractionForEntity` — the only
   code actually written inside `ItemDefuser.java` — is a completely different, entity-targeted
   mechanic with nothing to do with placed bombs: right-click a `EntityCreeper` to pacify it
   (delegates to `ItemModDefuser.defuse`, a class in `items/armor`, out of this survey's scope), or
   right-click an `EntityGlyphidNuclear` mob to trigger a small controlled kill-explosion and drop
   loot. Two `Landmine`/`BlockCrashedBomb` blocks bypass the generic `IToolable` path entirely and
   instead check `player.getHeldItem(hand).getItem() == ModItems.defuser` directly inside their own
   `onBlockActivated` — a second, narrower defuse dispatch mechanism CE never unified with the first
   in ~9 years of development (see Open questions).
3. **The nuke-casing content (9 concrete casings + 1 modular custom bomb + crashed-bomb duds) is
   blocked on an entity-package prerequisite this report can locate precisely.** Every one of them
   ultimately calls `world.spawnEntity(...)` on one of `EntityNukeExplosionMK5`,
   `EntityNukeExplosionMK3`, `EntityBalefire`, `EntityNukeTorex`, `EntityCloudFleija`,
   `EntityCloudSolinium`, `EntityFallingNuke`, or `EntityEMPBlast` — all under `com.hbm.entity.
   {logic,effect,projectile}`. **None of these exist in the port.** A repo-wide check confirms
   `com.hbm.entity` in this port currently has exactly 3 files (`ConveyorEntityTypes`,
   `EntityMovingItem`, `EntityMovingConveyorObject`), all Phase 2 conveyor-belt content — CE's
   `com.hbm.entity` tree has ~198 files across 11 subpackages. This is the same absence
   `docs/phase2/rbmk_reactor.md` already flagged for `EntityRBMKDebris`/`EntitySpear`; this report
   narrows it to the specific ~8 entities the nuke-casing family needs. By contrast, the
   conventional-explosive family (TNT/dynamite/semtex/C4/charges/mines/det-cord) depends on exactly
   **one** small, self-contained entity — `EntityTNTPrimedBase` (165 lines) — making it a
   realistic, demonstrable early Phase 3 milestone independent of the nuke-casing blocker.

## Phase-3-safe scope

All line counts are from the CE files actually read. "Portable now" means buildable against classes
already committed in this port plus, at most, one narrowly-scoped new entity/util class (called out
per item).

### A. Conventional explosive blocks + TE (blocked on one entity: `EntityTNTPrimedBase`)

| Class | Lines | Notes |
|---|---|---|
| `BlockDetonatable` (abstract) | 82 | Implements `IExploder`. `onBlockExploded`/`onExplosionDestroy` spawn `EntityTNTPrimedBase` with a randomized "pop fuse" instead of instantly removing the block — this is what makes TNT-family blocks throw a live primed entity when hit by another explosion rather than vanishing silently. `canDropFromExplosion` is hardcoded `false` (no item drop from chain-detonation). |
| `BlockTNTBase` (abstract) | 139 | `extends BlockDetonatable implements IToolable`. Redstone power and burning-arrow collision both call a private `prime(...)` that spawns the primed entity; flint-and-steel right-click also primes. `onScrew`: `ToolType.SCREWDRIVER` toggles a `META`-backed "ignite on break" flag (with a chat message), `ToolType.DEFUSER` unconditionally destroys the block and re-drops a clean (`META=0`) copy — this is the generic defuse path, already free once `ItemTooling`/`IToolable` dispatch it (both already ported). |
| `BlockTNT`, `BlockDynamite`, `BlockSemtex`, `BlockC4` | 21/20/20/20 | Each is nothing but a constructor (texture frames) + one-line `explodeEntity` override calling vanilla `world.createExplosion(entity, x, y, z, <10/8/12/15>F, true)`. Fully self-contained once `BlockTNTBase`/`EntityTNTPrimedBase` exist. |
| `BlockFissureBomb` | 50 | `extends BlockTNTBase`. `explodeEntity` calls `ExplosionNukeSmall.explode(..., PARAMS_MEDIUM)` then converts a 5-block-radius sphere of `ModBlocks.ore_bedrock_block`/`ore_bedrock_oil` into `ore_volcano`/`Blocks.BEDROCK`, keyed off whether the biome is a `BiomeGenCraterBase`. **Cross-package dependency on Phase 4 world-gen** (the crater biome + bedrock-ore feature) — the block itself is otherwise trivially portable. |
| `BlockChargeBase` (abstract) | 264 | `implements IBomb, IToolable, IExploder`. Directional (6-facing) wall/ceiling/floor-mountable charge with its own render/collision AABBs per facing. Right-click (not sneaking) cycles a timer through `{0,100,200,300,600,1200,3600,6000}` ticks; sneak-click arms it (`TileEntityCharge.started = true`) if a nonzero timer is set. `onScrew(DEFUSER)`: if armed, first click **disarms** (`started = false`, plays a "safe" sound) rather than removing — a second use is needed to actually dismantle-and-drop via `dismantle()`. `breakBlock` explodes unless the static `safe` flag is set (set true only around the defuser's own controlled removal / the `explode()` call's own `setBlockToAir`, to avoid re-triggering itself) — a CE-standard "explosion re-entrancy guard" pattern worth preserving exactly. |
| `TileEntityCharge` | 107 | `extends TileEntityLoadedBase implements ITickable`. Countdown ticks down once `started`; at `timer<=0` it resolves the detonating `Entity` as `ModContext.DETONATOR_CONTEXT.get()` if set, else looks up `placerID` via `world.getMinecraftServer().getPlayerList().getPlayerByUUID(...)` (the entity may be offline — a null detonator is valid downstream), then calls `((BlockChargeBase)block).explode(world, pos, detonator)`. `serialize`/`deserialize` (2 fields) is the exact `networkPackNT`-style sync shape this port's `LoadedBaseBlockEntity` already implements. |
| `BlockChargeC4`, `BlockChargeSemtex` | 61, 55 | `explode()` builds an `ExplosionVNT` (`BlockAllocatorStandard(32)` + `BlockProcessorStandard` (`.setNoDrop()` for C4 / `.setAllDrop().setFortune(3)` for Semtex)) at 15F/10F size, plus a small VFX composite. Concrete demonstration of the vanillant framework's real intended usage shape (see Section D). |
| `BlockChargeDynamite`, `BlockChargeMiner` | 38, 49 | `explode()` builds a plain `ExplosionNT` (the older, non-vanillant explosion class — **not itself read in this survey**, flagged for whoever ports it) at 4F, `BlockChargeMiner` additionally sets `ExAttrib.{NOHURT,ALLDROP}` (mining charge: breaks/drops blocks, does no entity damage). |
| `Landmine` | 270 | Not `IToolable` — `onBlockActivated` does a **direct identity check** (`heldMain/heldOff == ModItems.defuser || == ModItems.defuser_desh`) to safely pop the block into a dropped `EntityItem` instead of exploding; this bypasses the generic `onScrew` path entirely (see Headline finding #2). `explode()` switches on the block's own registry-name string (`mine_ap`/`mine_he`/`mine_shrap`/`mine_fat`/`mine_naval`) to pick a completely different `ExplosionVNT` configuration per mine type (piercing bullet-style damage for AP, block-destroying HE, shrapnel-shower for the shrap mine, `EntityNukeExplosionMK5` for the nuclear "fat" mine, water-only `BlockAllocatorWater` for the naval mine). `mine_fat` needs the same `com.hbm.entity` blocker as Section B. |
| `TileEntityLandmine` | 111 | `ITickable`, no GUI, no inventory. Two-phase proximity trigger: `waitingForPlayer` mode uses a wide (25-block) box that only clears on player presence (a "safety" un-prime, used when the mine is placed with someone standing on it already — not fully wired here, flagged as-is); otherwise a `range`/`height`-sized AABB (block-type-specific, from the paired `Landmine` instance's fields) scans for any `EntityLivingBase` (bats excluded) — first pass without `isPrimed` uses a doubled box to "prime" (play a click sound, no trigger), a later pass with `isPrimed` calls `landmine.explode(world, pos, entity)` with the triggering entity itself as the detonator. |
| `DetCord`, `DetMiner` | 111, 64 | Neither is item-detonated at all — `DetCord` is a wire block that explodes on `neighborChanged`/redstone or on being caught in another explosion (`onExplosionDestroy`), branching by registry identity (`det_cord`→small vanilla explosion, `det_charge`→`ExplosionLarge`, `det_n2`/`det_nuke`→`EntityNukeExplosionMK5` (blocked, Section B dependency), `det_bale`→`EntityBalefire` (same blocker)). `DetMiner` is a redstone-triggered all-drop/no-hurt `ExplosionNT` charge, same shape as `BlockChargeMiner`. Both are simple once their respective explosion/entity dependencies exist. |
| `BombFlameWar`, `BombFloat`, `BombThermo` | 40, 59, 67 | Small standalone `IBomb` blocks, redstone- or detonator-triggered, not tied to any TE. `BombFlameWar`→`ExplosionChaos` (not read, flagged). `BombFloat`→`ExplosionChaos.floater`/`move` or (for the `emp_bomb` variant) `ExplosionNukeGeneric.empBlast` + spawns `EntityEMPBlast` (Section B blocker). `BombThermo`→`ExplosionThermo.freeze`/`scorch` (endo/exo variants) + a plain vanilla explosion. All three are cheap once their named `Explosion*` static-helper classes exist (none of the three were in this survey's read set; `ExplosionThermo`/`ExplosionChaos`/`ExplosionNukeGeneric` are siblings of the `com.hbm.explosion` package, not `vanillant`, and were only encountered by reference here). |

### B. Nuke casings — block/TE/GUI plumbing is portable now; the actual detonation is blocked

All 9 concrete casings (`NukeBoy`, `NukeGadget`, `NukeMan`, `NukeMike`, `NukeTsar`, `NukeN2`,
`NukePrototype`, `NukeFleija`, `NukeBalefire`) plus the modular `NukeCustom` and the crashed-ordnance
`BlockCrashedBomb` share one shape, confirmed by reading every one of them in full:

- A `BlockContainer`/`BlockMachineBase`-backed casing block, 6-directional (`BlockHorizontal.FACING`,
  or a custom 4-value `PropertyInteger` for the two large multi-block-shaped casings
  `NukeFleija`/`NukeMan`), implementing `IBomb`.
- A paired TE (`TileEntityNuke*`) that is a plain `TileEntity implements IGUIProvider` (not
  `ITickable`, except `NukeBalefire`/`NukeTsar` which have their own countdown/resize logic) owning
  an `ItemStackHandler` (5–27 slots depending on casing complexity) exposed via
  `CapabilityItemHandler.ITEM_HANDLER_CAPABILITY`, plus a `placerID: UUID` NBT field used as the
  detonator fallback when the block is redstone-triggered rather than detonator-triggered.
- An `isReady()` (and, for the two-stage casings `NukeMike`/`NukeTsar`, an additional `isFilled()`)
  slot-content check that is a flat `getStackInSlot(n).getItem() == ModItems.<specific item>` chain
  for 8 of the 9 casings — trivial once the referenced `ModItems` fields exist (all of them are
  established Phase 1/2 item registry entries per `docs/phase1/items_machine.md`'s scope, not new
  content this survey needs to introduce).
- `neighborChanged` (redstone) and `explode()` (`IBomb`, detonator-triggered) both funnel into one
  shared `igniteTestBomb(world, detonator, x, y, z[, radius])` helper per class that (a) clears the
  slots, (b) plays the generic explosion sound, (c) spawns one of the blocked entities (Section B's
  dependency), passing through `detonator` if non-null or falling back to `placerID`'s looked-up
  player entity.
- `BombConfig.<name>Radius` (already ported, confirmed, every one of the 9 radii present) supplies
  the blast size; no new config work needed.
- `TileEntityNukeCustom` (399 lines total incl. block+TE) is the one outlier: its 27-slot inventory
  is validated by a real crafting-recipe lookup (`RecipesCommon.ComparableStack`/
  `NbtComparableStack`), not flat item-identity — meaningfully more work than the other 8, and its
  own `explodeCustom(...)` static helper is a tiered priority chain (euphemium > solinium >
  schrabidium > antimatter/balefire > hydrogen > nuclear > non-nuclear) each additively pulling in
  the tier below it's yield, spawning a different entity per tier. Also spawns `EntityFallingNuke`
  when the casing is in "falling bomb" mode (dropped from an aircraft rather than placed) — one more
  entity dependency specific to this class.
- `BlockCrashedBomb`/`TileEntityCrashedBomb` (174 + 62 lines) is dud/salvage content, not a live
  detonator target in the normal sense: right-clicking it with a defuser (direct `ModItems.defuser`/
  `defuser_desh` identity check, same bypass pattern as `Landmine`) salvages materials by
  `EnumDudType` (`BALEFIRE`/`CONVENTIONAL`/`NUKE`/`SALTED`); its own `IBomb.explode()` (usable via a
  regular detonator too) instead detonates it for real, same entity dependency as the live casings.
  Its TE passively irradiates nearby `EntityLivingBase`s via `ContaminationUtil.contaminate(...)`
  every 2 ticks — **`ContaminationUtil` is confirmed absent from this port** (see Deferred scope).
- GUI framework mapping (confirmed against this port's already-shipped classes): every
  `TileEntityNuke*`'s `provideContainer`/`provideGUI` (CE's `IGUIProvider`) maps onto one concrete
  `Menu extends MenuBase<T>` + `Screen extends GuiInfoContainer<T>` pair per casing, exactly like
  Phase 2's RBMK/chem-plant precedent — no new GUI abstraction needed, just the same
  one-Menu-one-Screen-per-machine pattern this port already established. The TE itself maps onto
  `MachineBaseBlockEntity` (has an inventory) for the 8 flat-check casings, or a hand-rolled subclass
  of it for `NukeCustom`'s recipe-driven slots.

### C. Detonator / defuser items — the network protocol and defuse mechanic (this report's core ask)

| Class | Lines | Confirmed behavior |
|---|---|---|
| `ItemDetonator` | 102 | **Store**: `onItemUse` (right-click a block), only if `player.isSneaking()` — writes `x`/`y`/`z` (plain ints) into the stack's `NBTTagCompound`, plays a UI-boop sound, chat-confirms client-side. **Fire**: `onItemRightClick` (right-click in air, or on nothing) — if no NBT, error message; else reads `x`/`y`/`z` back, builds a `BlockPos`, checks `world.getBlockState(pos).getBlock() instanceof IBomb`; if true, plays a UI-bleep sound and (server-only) calls `explode(world, pos, player)`, logs (if `GeneralConfig.enableExtendedLogging`), chat-confirms "detonated"; if false, chat-errors "too far" (a slightly misleading message — the real condition checked is "not a bomb", not distance — see Open questions). No range, no line-of-sight, no dimension check anywhere in this class. |
| `ItemMultiDetonator` | 159 | Same idea, generalized to a list: **Store** appends one more `(x,y,z)` triple to three parallel `int[]` NBT arrays (`xValues`/`yValues`/`zValues`, via `ArrayUtils.add`) on sneak-right-click-on-block. **Fire**: plain right-click (not sneaking) iterates every stored triple, detonates every one that resolves to an `IBomb` (each such detonation gets its own log line + sound), then reports "`succ`/`total`" in chat. **Clear**: sneak-right-click **not on a block** (i.e. `onItemRightClick`, which fires when `onItemUse` didn't consume the interaction) wipes all three arrays to empty and chat-confirms. |
| `ItemLaserDetonator` | 67 | `implements IHoldableWeapon`. No stored state at all. Every right-click does `Library.rayTrace(player, 500, 1)` (a 500-block raycast) and detonates whatever `IBomb` is at the hit `BlockPos`, else plays the same bleep sound and chat-errors. `getCrosshair()` returns a custom crosshair icon (`L_ARROWS`) via `IHoldableWeapon` — a client-side HUD hook, no networking. |
| `ItemDefuser` | 55 | `extends ItemTooling(ToolType.DEFUSER, durability, name)`. **Contributes no code to the placed-bomb defuse path** — that entire mechanic is inherited from `ItemTooling.onItemUse`'s existing `IToolable`/`onScrew` dispatch (already ported and working in this port, confirmed). The only logic actually written here is `itemInteractionForEntity` (right-click-on-living-entity): `EntityCreeper` → `ItemModDefuser.defuse(creeper, player, true)` (pacify: clears creeper "ignited" state, removes its swell AI task, drops `safety_fuse`, deals 1 damage, applies Weakness — this is `items/armor`'s `ItemModDefuser.defuse`, a static helper also called by that class's own passive aura effect, out of this survey's scope but read for completeness); `EntityGlyphidNuclear` (only if `deathTicks > 0`, i.e. already dying) → kills it and detonates a small `ExplosionVNT` (piercing cross-processor, weapon SFX) in its place, gibs it (`ConfettiUtil.gib`), drops nuke-demolition ammo. Registered twice in `ModItems`: `defuser` (100 durability) and `defuser_desh` (`-1` = infinite durability). |
| `ModItems.defuser_gold` = `ItemModDefuser` | — | A **third, unrelated** item also named "defuser" — lives in `items/armor`, is a wearable armor-mod trinket whose passive effect auto-pacifies every creeper within 5 blocks every second. Not part of the detonator/defuser item family this task named; flagged only so it isn't confused with `ItemDefuser` during implementation. Out of scope for this report (belongs to `items/armor`'s own survey). |

Confirmed-already-ported prerequisites this whole section builds on (no new work needed for these):
- `com.hbm.interfaces.IBomb` — byte-identical to CE's shape (`BombReturnCode explode(Level, BlockPos,
  Entity)`, all 7 enum constants), already using `Level`/`BlockPos`/`Entity` (Mojang mappings).
- `com.hbm.api.block.IToolable` — `ToolType` enum already includes `DEFUSER`, `onScrew` signature
  matches CE's (`int x,y,z` overload + a `BlockPos` default-method overload), backed by
  `RecipesCommon.ComparableStack` (already exists, confirmed).
- `com.hbm.api.block.IExploder` — matches CE's shape exactly, **but its own `explodeEntity` overload
  already imports `com.hbm.entity.item.EntityTNTPrimedBase`, which does not exist yet** — this
  interface is already a "documented forward reference" in this port's own committed code, same
  pattern as the RBMK report's `HazardModifierRBMKRadiation` finding.
- `com.hbm.items.tool.ItemTooling` — already dispatches `useOn(UseOnContext)` → `IToolable.onScrew`
  → conditional `stack.hurtAndBreak(1, ...)`, i.e. the entire generic defuse-dispatch mechanism
  `ItemDefuser` will ride on for free.
- `com.hbm.config.BombConfig` — every nuke-radius `IntValue` this section's blocks need
  (`GADGET_RADIUS` through `N2_RADIUS`, plus the `MAX_CUSTOM_*` family for `NukeCustom`) already
  exists.

Not yet ported, small, and specific to this section:
- `com.hbm.main.ModContext` (CE: 12 lines) — a single `ThreadLocal<Entity> DETONATOR_CONTEXT`, used
  by `TileEntityNukeBalefire.explode()`, `NukeBalefire.explode()`, `LaunchPadLarge.explode()`, and
  `LaunchTable.explode()` to smuggle the triggering `Entity` through an internal `launch()`/
  `explode()` call that has no detonator parameter of its own. Every call site wraps set/clear in a
  `try/finally` — preserve that discipline exactly (a leaked ThreadLocal on a server thread pool would
  misattribute a later, unrelated detonation to the wrong player).

## Deferred scope

- **`com.hbm.entity.{logic,effect,projectile}` — the dominant blocker.** Specifically:
  `EntityNukeExplosionMK5` (234 lines, used by 6 of the 9 casings + `DetCord`+`Landmine`'s `mine_fat`),
  `EntityNukeExplosionMK3` (402 lines, used by `NukeFleija`/`NukePrototype`/`NukeSolinium`/
  `NukeCustom`'s solinium/schrabidium tiers), `EntityBalefire` (106 lines, `NukeBalefire`/`DetCord`/
  `NukeCustom`'s antimatter tier), `EntityNukeTorex` (616 lines — the mushroom-cloud visual effect
  spawned alongside almost every nuke detonation, gated by `BombConfig.enableNukeClouds`),
  `EntityCloudFleija`/`EntityCloudSolinium` (96/97 lines, companion cloud VFX for the MK3-family
  detonations), `EntityFallingNuke` (119 lines, `NukeCustom`'s air-dropped mode), `EntityEMPBlast`
  (89 lines, `BombFloat`'s `emp_bomb` variant). None exist in the port. Recommend this become its own
  named work package (`com.hbm.entity` core-entity prerequisite), scoped narrowly to these ~8 classes
  plus whatever base `Entity`/projectile pattern they share — not the full ~198-file CE `com.hbm.
  entity` tree, most of which (mobs, trains, carts, missiles, siege) is unrelated to this package.
- **`EntityTNTPrimedBase`** (165 lines) — much smaller, narrower, and higher-value: unblocks the
  entire Section A conventional-explosive family by itself. Its 1.12 `SynchedEntityData`-based
  block-identity encoding (registry-name string + `byte` meta) has no direct 1.21 analogue (no more
  meta ints on `BlockState`) and needs a real re-encoding decision — see Open questions.
- **`com.hbm.util.ContaminationUtil`** — confirmed absent from this port (only its consumer-side
  target, `com.hbm.capability.HbmLivingAttachment`, exists — already checked per this task's ground
  rules). Needed by `TileEntityCrashedBomb`'s passive-irradiation tick and `DigammaFlame` (a
  world-hazard block in the same CE package, see below). Narrow, single-call-site dependency per
  site; likely belongs with whichever package finishes the general hazard/contamination exposure
  pipeline (Phase 4-flavored per this port's own prior findings), not this bomb-blocks package.
- **`com.hbm.explosion.{ExplosionNT,ExplosionLarge,ExplosionChaos,ExplosionThermo,
  ExplosionNukeGeneric,ExplosionNukeSmall}`** (the non-`vanillant` explosion helper classes) — used by
  `BlockChargeDynamite`/`Miner`, `DetCord`/`DetMiner`, `BombFlameWar`/`Float`/`Thermo`,
  `BlockFissureBomb`. None were read in this survey (out of the `vanillant` scope this task named);
  flagged as real forward references for whoever implements Section A's non-`vanillant`-based blocks.
- **`BiomeGenCraterBase` / bedrock-ore world-gen feature** — `BlockFissureBomb`'s only real
  dependency beyond `BlockTNTBase`; a Phase 4 world-gen concern, already flagged in Phase 1's own
  survey per this task's ground rules.
- **`com.hbm.inventory.RecipesCommon`/`OreDictManager`** — `TileEntityNukeCustom`'s recipe-driven
  slot validation needs `RecipesCommon.ComparableStack`/`NbtComparableStack`; `ItemDefuser`'s glyphid
  branch imports `com.hbm.inventory.OreDictManager.DictFrame` and
  `com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo` — the latter two are a **gun/weapon-package
  dependency**, not a bomb-package one; recommend porting `ItemDefuser`'s `IToolable`/creeper-pacify
  behavior first and leaving the `EntityGlyphidNuclear` branch as a documented forward reference
  until the Sedna gun-factory and glyphid-mob packages exist.
- **Multiblock missile-launch content implementing `IBomb`**: `LaunchPad`, `LaunchPadLarge`,
  `LaunchPadRusted`, `LaunchTable`, `CompactLauncher`, `RailgunPlasma` (the last does not even
  implement `IBomb` — it's a plain machine block, included here only because it lives in the same CE
  package). All six are `BlockDummyable`-family multiblocks (`BlockDummyable`/`MultiblockHandlerXR`
  are both already ported per this port's own Phase 2 work, so the multiblock shape itself is not a
  blocker) whose real payload is missile/artillery content — per `docs/phase1/items_tool.md`'s bucket
  (b), the designator/rangefinder/radar items that actually arm these launchers are a separate
  "military C2 equipment" work package, not detonator/defuser content. `LaunchPadLarge`/`LaunchTable`
  both use the `ModContext.DETONATOR_CONTEXT` thread-local (see Key design/API decisions) when fired
  by a detonator rather than a designator — that's the one place this section's protocol and the
  launch-pad package's own eventual survey overlap; flagged so neither package re-derives it.
- **World-hazard blocks in the same CE package that are not detonator-related at all**: `CheaterVirus`
  /`CheaterVirusSeed`/`CrystalVirus`/`CrystalPulsar` (self-propagating "virus" terrain blocks, no
  `IBomb`, gated by `GeneralConfig.enableVirus`), `DigammaFlame`/`DigammaMatter` (world VFX/hazard
  blocks from the balefire/digamma family, no `IBomb`), `BlockTaint`/`Balefire` (self-spreading
  world-hazard fire/taint blocks — `Balefire extends BlockFire`, not `IBomb`), `BlockCloudResidue`
  (a fallout-cloud residue block), `BlockVolcano` (a `BlockDummyable`-family volcano-eruption
  multiblock, `ICustomBlockItem`, no `IBomb`). None of these seven implement `IBomb`, none interact
  with a detonator/defuser item in any way — CE's own package layout groups them under `blocks/bomb`
  by theme ("things that came out of an explosion or nuclear event"), not by mechanism. They belong
  with Phase 4's world-simulation/hazard survey, not this report's scope; listed here only so their
  absence from the "Phase-3-safe scope" section above isn't mistaken for an oversight.

## Key design/API decisions

Confirmed from real code — CE for behavior, this port's own already-committed code (and, narrowly,
Neo Edition) for NeoForge 1.21.1 API shape. No API below is invented.

- **`IBomb.explode` keeps the `Entity detonator` parameter.** This port's already-committed
  `com.hbm.interfaces.IBomb` correctly preserves CE's 3-argument shape
  (`explode(Level world, BlockPos pos, Entity detonator)`). Neo Edition's own `IBomb` **dropped the
  detonator parameter** (`explode(Level, BlockPos)` only) — noted here explicitly as a place *not*
  to follow Neo Edition, since CE's detonator items, `Landmine`'s proximity trigger, and every
  `ModContext.DETONATOR_CONTEXT` call site all depend on that third argument for correct
  kill-attribution. This port's existing `IBomb.java` is already right; do not "fix" it to match Neo
  Edition.
- **The detonator "protocol" needs zero new `CustomPacketPayload`s.** Confirmed by reading
  `com.hbm.packet.HbmNetwork` (this port's `RegisterPayloadHandlersEvent` registrar, currently
  registering only `BufPacket` from Phase 2) and both CE detonator items: the entire store/fire/clear
  cycle is a same-tick server-side `Item` callback, already dispatched by vanilla's own interaction
  packets. Real 1.21.1 `Item` override shapes, confirmed both by this port's already-ported
  `ItemTooling` (`useOn(UseOnContext)`) and cross-referenced against Neo Edition's real, compiling
  `DetonatorItem`/`LaserDetonatorItem` (API shape only, not behavior — Neo Edition's versions drop
  the sneak/store/multi-position semantics CE has, and are not treated as a behavioral reference):
  `useOn(UseOnContext) → InteractionResult` for the on-block store interaction, and
  `use(Level, Player, InteractionHand) → InteractionResultHolder<ItemStack>` for the in-hand fire
  interaction (CE's `onItemUse`/`onItemRightClick` pair maps onto these directly, one-to-one).
- **NBT → Data Component**: `ItemDetonator`'s `x`/`y`/`z` ints and `ItemMultiDetonator`'s
  `xValues`/`yValues`/`zValues` parallel arrays are exactly the "coordinate-store pattern" already
  flagged by `docs/phase1/items_tool.md` as needing one shared component type rather than a bespoke
  component per item. This port's `com.hbm.items.tool.ToolDataComponents` (confirmed real, already
  registers 4 `DataComponentType`s via `DeferredRegister<DataComponentType<?>>`) is the established
  place to add two more: a `DataComponentType<BlockPos>` for `ItemDetonator` (vanilla `BlockPos`
  already ships its own `Codec`/`StreamCodec`, so this needs no custom codec) and a
  `DataComponentType<List<BlockPos>>` for `ItemMultiDetonator`. Neo Edition's alternative (a generic
  `TagsUtil` helper wrapping the stock `DataComponents.CUSTOM_DATA` `CompoundTag` component) is a
  viable lower-effort fallback if a dedicated component type is judged not worth the extra
  registration — noted as an option, not a mandate, since CE's own behavior doesn't care which
  encoding is chosen as long as round-tripping is correct.
- **GUI framework**: every nuke casing's `IGUIProvider` pair maps onto this port's
  `MenuBase<T extends MachineBaseBlockEntity>` (confirmed, read in full) +
  `GuiInfoContainer<T extends AbstractContainerMenu>` (confirmed, read in full) — one concrete
  `Menu`/`Screen` pair and one `BlockEntityType` registration per casing, exactly like Phase 2's
  established machine pattern; no second GUI framework needed, none invented here.
- **Block-entity base mapping**: `TileEntityCharge`/`TileEntityLandmine`/`TileEntityCrashedBomb`/
  `TileEntityBombMulti` (no `IGUIProvider` need beyond `BombMulti`, which does have one) map onto
  plain `LoadedBaseBlockEntity` + the `ITickableBE` marker (per `docs/phase2/blockentity_base.md`'s
  already-established convention); the 9 nuke-casing TEs map onto `MachineBaseBlockEntity` (they all
  own an `ItemStackHandler`).
- **Entity registration**: this port's one existing precedent,
  `com.hbm.entity.ConveyorEntityTypes` (confirmed real, `DeferredRegister<EntityType<?>>` +
  `EntityType.Builder.of(...).sized(...).setTrackingRange(...).build(...)`), is the pattern the
  `EntityTNTPrimedBase`/nuke-explosion-entity work package should follow — no new registration shape
  needed, and per that file's own javadoc, a family of related entities is free to own its own
  `DeferredRegister` rather than centralizing into one shared `ModEntityTypes` class.
- **Damage types**: no new `DamageType` entries are needed for this package's Phase-3-safe subset.
  `com.hbm.damage.ModDamageTypes` (confirmed real, already bootstraps every entry) already has
  `BLAST`, `NUCLEAR_BLAST`, `RUBBLE`, and `SHRAPNEL` — exactly what `vanillant`'s
  `EntityProcessorStandard`/`EntityProcessorCrossSmooth` and the eventual nuke-explosion entities
  will dispatch through `Level#damageSources().source(key, ...)`, per that file's own stated
  ownership split.
- **Explosion-performance concern is real and precisely located** (PORT_SPEC's explicit callout,
  confirmed by reading the actual removal loop): `BlockAllocatorStandard.allocate` ray-marches a
  16–32-resolution sphere surface (CE's own vanilla-TNT-derived algorithm) into a flat
  `HashSet<BlockPos>` that can hold **thousands** of positions for a large-radius nuke. `Block
  ProcessorStandard.process` then iterates that set and calls vanilla `Block#onBlockExploded` **once
  per position** — under 1.12 semantics this already meant one `world.setBlockState`/neighbor-update
  call per block, but under 1.21.1's more expensive per-block-change lighting engine, a literal port
  of this loop (`Level#setBlock` called thousands of times per explosion) would visibly stall the
  server on every large detonation. This is not a hypothetical: CE's own biggest-radius algorithm,
  `ExplosionNukeRayBatched` (read partially, for this comparison only — it is not itself in this
  package's scope), **already** groups every affected position into a `HashMap<ChunkPos, BitSet>`
  before writing, i.e. CE itself already thinks in per-chunk batches once radii get large enough to
  matter. The port's obligation, per PORT_SPEC, is to extend that same per-chunk grouping into an
  actual batched `LevelChunkSection` write (direct palette/state manipulation while holding one
  section, skipping vanilla `Level#setBlock`'s per-call neighbor/light-update overhead) followed by
  one deferred lighting-engine recalculation pass per touched chunk column — not to invent per-chunk
  grouping from nothing. `BlockProcessorStandard`'s pluggable-mutator shape (`IDropChanceMutator`/
  `IFortuneMutator`/`IBlockMutator`) can be preserved as-is on top of a batched removal pass; only the
  final `world.setBlockState`/`onBlockExploded` call site needs to change, not the allocation or
  drop-chance logic surrounding it.

## Open questions / risks

- **`com.hbm.entity` absence is the dominant risk for the nuke-casing half of this package.** 10 of
  the 13 TE pairs surveyed (9 casings + `NukeCustom`) cannot complete their `explode()`/
  `igniteTestBomb()` call until at least `EntityNukeExplosionMK5` exists (needed by 6 of them
  directly, a 7th — `Landmine`'s `mine_fat` — indirectly). Recommend explicit sub-phase ordering:
  land Section A (conventional explosives, blocked only on the much smaller `EntityTNTPrimedBase`)
  as an early, fully-functional, demonstrable Phase 3 milestone before attempting any nuke casing.
- **`EntityTNTPrimedBase`'s block-identity encoding has no direct 1.21 analogue.** CE synchronizes
  "which block state primed this entity" as a `SynchedEntityData` pair (`registry-name string` +
  `byte meta`), reconstructed via `Block.getStateFromMeta(meta)`. 1.21's `BlockState` has no meta
  ints. Before implementing, check whether `getBomb()`'s only real consumer
  (`IExploder.explodeEntity`) ever actually needs the *specific* `BlockState` variant that primed it,
  or only needs "which `Block`" — if only the latter, a `ResourceLocation`/registry-id sync field is
  sufmicient and much simpler than round-tripping a full `BlockState` codec over the wire.
- **`ItemDetonator`/`ItemMultiDetonator` have zero distance/dimension/loaded-chunk validation in
  CE**, confirmed by reading both in full — a stored position in an unloaded chunk or a different
  dimension silently falls into the "not a bomb" `else` branch (a `chat.postoofarerror`/
  `chat.posbadrror` message that is worded like a range error but is actually just "the block there
  isn't `IBomb`", since `getBlockState` on an unloaded position still returns a valid, non-`IBomb`
  air/default state rather than throwing). Preserve this exactly for CE parity — it is CE's real
  behavior, not a bug this port should silently "fix" — but flag it, since a QA pass unfamiliar with
  CE's source may read the misleading error text as a defect.
- **`Landmine` and `BlockCrashedBomb` bypass the generic `IToolable`/`onScrew` defuse path with a
  direct `getItem() == ModItems.defuser` identity check inside their own `onBlockActivated`.**
  Confirm at implementation time whether to preserve this exact two-mechanism asymmetry (CE itself
  never unified it) or fold it into `IToolable` for consistency — preserving it is the lower-risk,
  parity-safe default, but it means a future "add a third defuser item" change would need to update
  three call sites (the `IToolable` dispatch plus these two direct checks) instead of one.
- **`TileEntityNukeCustom`'s recipe-driven slot validation is meaningfully bigger than the other 8
  casings** (needs `RecipesCommon.ComparableStack`/`NbtComparableStack`, a real recipe-lookup map,
  and its own tiered `explodeCustom` priority chain across 8 yield categories) — scope it as its own
  sized sub-task rather than assuming it's "one more flat-item-check casing" like its siblings.
- **`ItemDefuser`'s `EntityGlyphidNuclear` branch pulls in Sedna gun-factory and glyphid-mob
  dependencies** (`ConfettiUtil`, `GunFactory.EnumAmmo`, `OreDictManager.DictFrame`) that are clearly
  outside "bomb blocks and detonator items" in scope. Recommend implementing `ItemDefuser`'s
  `IToolable` inheritance + creeper-pacify branch first, leaving the glyphid branch as a documented
  forward reference (exactly like every other cross-phase gap already accepted in this port) until
  whichever package owns Sedna weapons and glyphid mobs lands.
- **Neo Edition already has a complete, running implementation of this entire feature family**
  (`DetonatorItem`/`MultiDetonatorItem`/`LaserDetonatorItem`/`BombCallerItem`, `NukeBaseBlockEntity` +
  9 concrete subclasses, its own `IBomb`, `ExplosionNukeRayBatched`/`Parallelized`). Used above **only**
  for two confirmed API-shape facts (the `Item#useOn`/`use` method shapes, and the per-chunk `BitSet`
  batching precedent) per this project's standing rule that Neo Edition is not a behavior source.
  Explicitly do **not** use its dropped detonator parameter, its slot/recipe layouts, or its blast
  yields/damage numbers as ground truth — CE remains the sole source for all of those.
