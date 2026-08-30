# Turret system (`TileEntityTurretBaseNT` family) — Phase 3 research

Sources read in full:
- `upstream/hbm-ce/src/main/java/com/hbm/tileentity/turret/*.java` (15 files, ~3,100 lines total —
  `TileEntityTurretBaseNT` (1,301 lines, the shared AI/targeting/power/whitelist core) and
  `TileEntityTurretBaseArtillery` read in full; the 13 concrete subclasses read in full for their
  `updateFiringTick`/`getAmmoList`/constructor-tuning overrides — `Sentry`, `SentryDamaged`,
  `Chekhov`, `Friendly`, `Fritz`, `Howard`, `HowardDamaged`, `Jeremy`, `Maxwell`, `Richard`, `Tauon`,
  `Arty`, `HIMARS`)
- `upstream/hbm-ce/src/main/java/com/hbm/blocks/turret/*.java` (14 files — `TurretBaseNT` read in
  full, the 13 concrete blocks surveyed by signature/override grep — all are thin `BlockDummyable`
  subclasses carrying only material/registry-name/bounding-box overrides)
- `upstream/hbm-ce/src/main/java/com/hbm/inventory/container/ContainerTurretBase.java` (full — the
  one shared `Container` for all 13 concrete turret TEs) and
  `upstream/hbm-ce/src/main/java/com/hbm/inventory/gui/{GUITurretBase,GUITurretMobFilter}.java` (both
  full; the 11 remaining concrete `GUITurret*` classes surveyed by grep — cosmetic texture/position
  overrides only, no new behavior)
- `upstream/hbm-ce/src/main/java/com/hbm/items/machine/{ItemTurretBiometry,ItemTurretChip}.java`
  (full, CE) cross-checked against this port's own already-shipped
  `src/main/java/com/hbm/items/machine/{ItemTurretBiometry,ItemTurretChip,MachineDataComponents,MachineItems}.java`
- `upstream/hbm-ce/src/main/java/com/hbm/items/tool/ItemTurretMobFilter.java` (full — Phase 3 scope)
- Dependency surface read to characterize scope, not to invent behavior: `BulletConfig.java` (header
  + `LAMBDA_STANDARD_RICOCHET`), `com.hbm.entity.projectile/*` directory listing (54 files),
  `com.hbm.items.weapon/**` directory listing (sedna framework, ~90+ files across
  `mags/hud/impl/mods/factory`), `TileEntityTurretBaseArtillery.java` (full, for
  `IRadarCommandReceiver`), `com.hbm.util.CompatExternal.java` (full, 209 lines),
  `com.hbm.inventory.control_panel/**` directory listing (84 files), `com.hbm.lib.ModDamageSource.java`
  (grep for `microwave`/`electricity`)
- This port's own `src/main/java/com/hbm/{blockentity/{LoadedBaseBlockEntity,MachineBaseBlockEntity}.java,
  inventory/container/MenuBase.java, inventory/gui/GuiInfoContainer.java,
  api/energymk2/IEnergyReceiverMK2.java, blocks/BlockDummyable.java, packet/HbmNetwork.java,
  damage/ModDamageTypes.java, entity/ConveyorEntityTypes.java, entity/item/EntityMovingItem.java,
  blockentity/network/FluidCounterValveBlockEntity.java}` (read in full or targeted, to confirm what
  substrate already exists rather than assuming gaps)
- `docs/phase1/{items_tool.md, items_machine.md, DIGEST_REMAINDER.md}`, `docs/phase2/{gui_framework.md,
  blockentity_base.md, multiblock_framework.md, rbmk_reactor.md (structural model)}` — all treated as
  authoritative prerequisites, not re-derived
- `upstream/neo-edition/src/main/java/com/hbm/entity/projectile/BulletBaseMK4.java` and
  `com.hbm.entity.NtmEntityTypes` (signature-only, for the confirmed `EntityType`/registration shape
  a future projectile port would use — **not** consulted for turret content, since Neo Edition has
  **no turret classes at all**, confirmed by `find -iname "*turret*"` returning zero TE/block hits)

## Headline finding

The turret *shell* — targeting AI, line-of-sight, power draw, mob-filter/whitelist state, the
`BlockDummyable` casing, the `Container`/GUI chrome — is portable today: every piece of substrate it
needs (`BlockDummyable`, `MachineBaseBlockEntity`+`LoadedBaseBlockEntity`, `MenuBase`,
`GuiInfoContainer`, `IEnergyReceiverMK2`, `ModDamageTypes`, `HbmNetwork`) already exists from Phase
0–2 and matches CE's own shapes closely enough to be a mechanical port. **What is not portable is
the part that actually makes a turret a weapon**: every one of the 13 concrete turret TEs fires
through `com.hbm.items.weapon.sedna.BulletConfig` + `com.hbm.entity.projectile.EntityBulletBaseMK4`
— the exact same gun/ammo pipeline CE's hand-held guns use (confirmed: `BulletConfig`/
`EntityBulletBaseMK4` are referenced from `items/weapon/sedna/factory/XFactory*mm.java`,
`items/armor/ArmorNCRPARanged.java`, and `items/weapon/grenade/ItemGrenadeFilling.java`, not just
from turret code). **Turrets are not a separate weapon system — they are a consumer of the gun/ammo
framework, driven by an AI instead of a player's mouse click.** None of `BulletConfig`,
`EntityBulletBaseMK4`, or any `XFactory*` config exists in this port yet. Two turrets (Maxwell,
Tauon) sidestep projectile spawning entirely and call `Entity#attackEntityFrom`/damage-source
methods directly — but even they still call `consumeAmmo(BulletConfig.ammo)`, proving CE's own
design already decouples "ammo consumption" from "projectile spawning." The two artillery/command
turrets (Arty, HIMARS) sit on a second, larger deferred dependency: a whole separate
missile/artillery entity family plus a radar command-and-control interface, unrelated to the bullet
system.

## Phase-3-safe scope

### Substrate already in place (confirmed, not assumed)

| CE dependency | Port status | Where |
|---|---|---|
| `TileEntityMachineBase(11, false, true)` constructor shape | Ported as `MachineBaseBlockEntity(type, pos, state, scount, enableFluidWrapper, enableEnergyWrapper)` — identical arity/semantics | `blockentity/MachineBaseBlockEntity.java:135-145` |
| Per-instance `hasCapability`/`getCapability(CapabilityEnergy.ENERGY)` | Replaced by `getEnergyStorageCapability(Direction)`/`hasEnergyStorageCapability()` accessor pair, registered once via `RegisterCapabilitiesEvent` | `blockentity/MachineBaseBlockEntity.java` javadoc + body |
| `IEnergyReceiverMK2` (power/consumption/`trySubscribe`) | Ported 1:1, including `transferPower`/`getReceiverSpeed`/`ConnectionPriority` | `api/energymk2/IEnergyReceiverMK2.java` |
| `BlockDummyable` (2×2 dummy/core casing, `findCore`, `getDimensions`, `getOffset`) | Ported 1:1 | `blocks/BlockDummyable.java` |
| `serialize`/`deserialize`(`ByteBuf`)/`networkPackNT` throttled sync | Ported 1:1 as `RegistryFriendlyByteBuf` overloads | `blockentity/LoadedBaseBlockEntity.java:184-255` |
| `ContainerBase`(shift-click, slot batching) | Ported as `MenuBase<T extends MachineBaseBlockEntity>` | `inventory/container/MenuBase.java` |
| `GuiInfoContainer` (`drawElectricityInfo`/`drawCustomInfoStat`/`drawInfoPanel`) | Ported 1:1 against `GuiGraphics` | `inventory/gui/GuiInfoContainer.java` |
| `ModDamageSource.electricity`/`.microwave` (needed by Tauon/Maxwell) | Both already registered as `DamageType` datapack entries | `damage/ModDamageTypes.java:63,75,151,163` |
| `ItemTurretBiometry`/`ItemTurretChip` NBT name list | Ported as one `\n`-joined `String` data component (`MachineDataComponents.TURRET_NAMES`) | `items/machine/{ItemTurretBiometry,ItemTurretChip,MachineDataComponents}.java` |
| `RegisterPayloadHandlersEvent` packet registration pattern | One real payload (`BufPacket`) already registered; pattern documented in-file for the next packet to add | `packet/HbmNetwork.java` |

### What can be built now (13 TE + 14 block + shared Container/GUI shell)

1. **`TileEntityTurretBaseNT`'s core (1,301 lines) is almost entirely projectile-agnostic** and can
   be ported as an abstract `MachineBaseBlockEntity` subclass today, function-for-function:
   - Targeting math: `entityInLOS`, `entityAcceptableTarget`, `seekNewTarget`, `alignTurret`,
     `turnTowards`/`turnTowardsAngle` (radian-based yaw/pitch stepping with wraparound handling),
     `getTurretPos`/`getEntityPos`/`byHorizontalIndexOffset` — pure geometry over `Vec3`/`Entity`,
     no gun-framework dependency at all.
   - Targeting filters: `targetPlayers`/`targetAnimals`/`targetMobs`/`targetMachines` booleans,
     `isBlacklistMobFilter` + `mobFilter` (`List<String>` of entity registry-id strings),
     `getWhitelist()`/`addName`/`removeName` reading/writing the already-ported
     `MachineDataComponents.TURRET_NAMES` component on the biometry chip in slot 0.
   - Power: `hasPower`/`isOn`/`setPower`/`getPower`/`getConsumption`/`getMaxPower` against the
     already-ported `IEnergyReceiverMK2` contract; `Library.chargeTEFromItems` (battery-slot
     trickle-charge) needs only `Library`'s equivalent helper (confirmed a `Library` class already
     exists in this port from Phase 0, used throughout).
   - The 11-slot `ItemStackHandler` inventory (slot 0 = biometry/chip, 1–9 = ammo, 10 = battery) —
     identical shape to what `MachineBaseBlockEntity`'s `getNewInventory(int, int)` factory already
     supports.
   - `updateConnections()` (HE cable auto-subscribe for the 2×2 footprint) — pure `BlockPos`
     arithmetic against `IEnergyReceiverMK2.trySubscribe`, already-ported.
2. **All 14 turret block classes** (`TurretBaseNT` abstract + 13 concrete) — each is a trivial
   `BlockDummyable` subclass (material + registry name + occasional custom bounding box/offset), the
   same shape as every other multiblock casing already ported in Phase 2.
3. **`ContainerTurretBase` → `TurretMenu extends MenuBase<TurretBaseBlockEntity>`** — same 11-slot
   layout, `SlotItemHandler`-backed (matching CE's own `IItemHandler`-based `ContainerBase`, which
   `docs/phase2/gui_framework.md` already confirmed as this port's chosen slot model).
4. **`GUITurretBase` → `TurretScreen extends GuiInfoContainer<TurretMenu>`** for every part not tied
   to ammo: power gauge (`drawElectricityInfo`), the 4 targeting-toggle icon tooltips
   (`drawCustomInfoStat`), the tally-mark (`stattrak`) counter, and the whitelist add/remove text
   field (needs one small control payload — see decision 6 below).
5. **`ItemTurretMobFilter`** (Phase 3 scope, this task's named target) — the item itself has zero
   gun-framework dependency: it's a shift-right-click-on-`BlockDummyable` item that resolves the core
   TE via `findCore` and opens a screen. Portable now.
6. **8 of the 13 concrete turret TEs need only `BulletConfig`+`EntityBulletBaseMK4` to be
   fire-complete** once that framework lands (no missile/radar dependency): Sentry, SentryDamaged,
   Chekhov, Friendly, Richard, Jeremy, Howard/HowardDamaged (gatling variant — spin-up `loaded`
   counter, otherwise same `consumeAmmo`/`BulletConfig` pattern), Fritz (diesel flamethrower — gates
   firing on its own already-portable `FluidTankNTM` fuel tank, `IFluidStandardReceiver`/
   `IFluidCopiable`, on top of the same bullet call). **2 more need no projectile system at all**:
   Maxwell (`ModDamageSource.microwave` direct-damage turret) and Tauon (`ModDamageSource.electricity`
   direct-damage turret) — both already have their `DamageType` in `ModDamageTypes`.

## Deferred scope

1. **The gun/ammo "sedna" framework** — `com.hbm.items.weapon.sedna/**` (`BulletConfig` +
   `mags/hud/impl/mods/factory` subpackages, factory alone has 21 `XFactory*` files) +
   `com.hbm.entity.projectile.{EntityBulletBaseMK4, EntityBulletBaseMK4CL, EntityBullet,
   EntityBulletBase, EntityBulletBaseNT, EntityBulletBeamBase, IBulletBase}`. **This is the true
   Phase 3 blocker for 10 of 13 turret TEs and should be its own package landed first or in
   parallel** — it is shared by turrets, every hand-held gun, `ArmorNCRPARanged`, and
   `ItemGrenadeFilling`, so porting it once unblocks all of them simultaneously; porting turret TEs
   first just produces classes whose firing methods have nothing to call.
2. **Missile/artillery entity family**, needed only by `TileEntityTurretArty`
   (`EntityArtilleryShell`) and `TileEntityTurretHIMARS` (`EntityArtilleryRocket`): those two entity
   classes plus `com.hbm.entity.missile/**` (16 files) if any missile-guided variant is ever added to
   a turret, plus `IRadarCommandReceiver` (`TileEntityTurretBaseArtillery`'s `sendCommandPosition`/
   `sendCommandEntity`/`targetQueue`) and the radar/designator item family already named as Phase 3
   scope in `docs/phase1/items_tool.md`'s bucket (b) (`ItemDesignator*`, `ItemSatDesignator`,
   `ItemRadarLinker`, `ItemRangefinder`). Recommend scoping Arty/HIMARS **out of the first turret PR**
   given this is a materially larger, mostly-unrelated dependency chain than the other 11 turrets.
3. **Particle/VFX network for muzzle flash, casing ejection, and tracers**:
   `com.hbm.particle.{SpentCasing, helper.HbmEffectNT}`, `com.hbm.handler.CasingEjector`,
   `com.hbm.packet.toclient.AuxParticlePacketNT`, `com.hbm.handler.threading.PacketThreading`. Every
   bullet-firing turret (`usesCasings()`/`spawnCasing()`/`getCasingSpawnPos()`/`getEjector()`) needs
   this, but it is not turret-specific — it's shared VFX substrate every hand-held gun also needs, so
   defer to whichever Phase 3 sub-task owns "gun visual effects," not this one.
4. **Control-panel system** (`com.hbm.inventory.control_panel/**`, 84 files) +
   `ControlEventSystem`/`ControlEvent`/`IControllable`. The turret's `receiveEvent`/`getInEvents`
   (event names `"turret_set_target"`/`"turret_switch"`) opt into this as a remote redstone-like
   automation network, but the package itself is a large, wholly separate cross-cutting feature many
   other machines also hook into — not turret-specific. Recommend the ported turret TE **not**
   implement `IControllable`/`IControlReceiver` yet (leave the two event names unwired) until whatever
   phase owns control panels exists, rather than pulling 84 files into a turret PR to keep two event
   strings functional.
5. **`CompatExternal`'s turret-target extension hooks** (`turretTargetPlayer`/`Friendly`/`Hostile`/
   `Machine`/`Blacklist`/`Condition` — `Set<Class>`/`Map` populated via soft reflection against other
   mods' entity classes, e.g. presumably Chisel/vehicle-mod entities in CE's own multi-mod pack
   context). This port has no other-mod dependency declared anywhere in `build.gradle`/`PORT_SPEC.md`
   — **recommend dropping these hooks entirely** rather than porting inert extension points that can
   never be populated; flag for a PORT_SPEC sign-off since it is a (currently invisible) behavior
   difference from CE if this port is ever run in a modpack with other content mods later.
6. **OpenComputers (`li.cil.oc`) `SimpleComponent`/`@Callback`/`@Optional` integration** — not a
   dependency of this port at all (`opencomputers`/`li.cil.oc` appear nowhere in `build.gradle` or
   `PORT_SPEC.md`). Drop entirely; this is not a "needs another phase" deferral, it's out of scope.
7. **`IRadarDetectableNT`-gated target visibility** (`entityAcceptableTarget`'s
   `e instanceof IRadarDetectableNT && !canBeSeenBy` branch) — the interface is already ported
   (`api/entity/IRadarDetectableNT.java`) but currently has no implementers in this port (missiles,
   aircraft, etc. don't exist yet), so this branch is a correct no-op until Phase 3/4 entities that
   implement it land; no action needed now beyond leaving the check in place.
8. **`SlotBattery`/`TransferStrategy`** — `ContainerTurretBase` uses both and neither exists in this
   port yet (confirmed by file search). `docs/phase2/gui_framework.md`'s own Deferred scope already
   flagged per-machine `TransferStrategy` configuration as deferred; the turret Menu should use
   `MenuBase`'s generic `quickMoveStack` reference implementation for now (as that report recommends
   for any machine without a ported `TransferStrategy`) and a plain `SlotItemHandler` restricted by an
   `IBatteryItem`-style predicate in place of `SlotBattery` until/unless that class is ported
   generally (it is not turret-specific either — every machine with a battery slot needs it).
9. **`TileEntityTurretHowardDamaged`/`TileEntityTurretSentryDamaged`** — not read in full for this
   report (both are small "damaged variant" subclasses of Howard/Sentry, presumably reduced stats for
   structure-generation loot/ruins). Low-risk; extend same-day once the parent TE lands.

## Key design/API decisions

1. **Base class**: `TurretBaseBlockEntity extends MachineBaseBlockEntity`, constructed with
   `super(type, pos, state, 11, false, true)` — matching CE's `super(11, false, true)` exactly (11
   slots, no fluid wrapper, energy wrapper on). Confirmed shape from
   `MachineBaseBlockEntity`'s 6-arg constructor overload.
2. **Energy capability wiring**: register once via
   `event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, TURRET_TYPE.get(), (be, side) ->
   be.getEnergyStorageCapability(side))` in the mod's `RegisterCapabilitiesEvent` handler — this
   replaces CE's per-instance `hasCapability`/`getCapability(CapabilityEnergy.ENERGY)` override, which
   has no NeoForge 1.21.1 equivalent (`BlockEntity` has no such override point in Neo at all; this is
   confirmed by `MachineBaseBlockEntity`'s own javadoc, not inferred here).
3. **Turret state that is TE-owned, not item-owned**: `mobFilter` (`List<String>`), the four
   `targetX` booleans, `isBlacklistMobFilter`, and `stattrak` are plain block-entity fields persisted
   via `saveAdditional`/`loadAdditional` (a `ListTag` of `StringTag`, same shape as CE's
   `NBTTagList`/`NBTTagString`) — **not** data components, since this is mutable TE state, not an
   `ItemStack` property. Only the biometry/chip item's own name list is a data component (already
   shipped as `MachineDataComponents.TURRET_NAMES`); do not conflate the two NBT-shaped lists that
   happen to both be "lists of strings."
4. **Menu**: one shared `TurretMenu extends MenuBase<TurretBaseBlockEntity>`, mirroring CE's own
   single shared `ContainerTurretBase` across all 13 concrete TEs — slot 0 restricted to
   `ItemTurretBiometry` instances, 1–9 general `SlotItemHandler`, 10 battery-restricted (see Deferred
   #8 for what stands in for `SlotBattery` until it's ported). Start from `MenuBase`'s generic
   `quickMoveStack` rather than trying to reconstruct CE's `TransferStrategy`-based routing.
5. **Screen**: `TurretScreen<M extends TurretMenu> extends GuiInfoContainer<M>` using the
   already-shipped `drawElectricityInfo`/`drawCustomInfoStat` for the power gauge and the 4
   targeting-toggle tooltips. CE's ammo-cycling hover tooltip (`GUITurretBase.drawAmmo`, backed by a
   `drawStackText` helper) has **no equivalent in the ported `GuiInfoContainer` yet** — this is a
   small, shared (non-turret-specific) addition to make there once `BulletConfig`-backed ammo lists
   exist to feed it; do not add a one-off copy inside the turret screen.
6. **`ItemTurretMobFilter`'s GUI is architecturally different from every other turret screen**: CE's
   `GUITurretMobFilter` is a bare `GuiScreen` with **no backing `Container` at all** — it mutates the
   target TE purely via a raw NBT packet (`addMobFilter`/`removeMobFilter`/`name`/`del` keys through
   `NBTControlPacket` → `TileEntityTurretBaseNT.receiveControl`). Port this as one new
   `CustomPacketPayload` record (e.g. `TurretControlPacket(BlockPos pos, CompoundTag data)` or a
   more structured sealed variant per action) registered via `registrar.playToServer(...)` in
   `HbmNetwork` — following the exact extension pattern that file's own header comment documents —
   handled server-side by resolving the `BlockEntity` at `pos` and dispatching to the turret's
   `receiveControl`-equivalent method. The client-side screen itself is a plain `Screen` subclass with
   no `AbstractContainerMenu` at all, same as vanilla's own screen-without-a-menu precedent (e.g. the
   sign-edit screen) — do **not** force this into `MenuBase`/`GuiInfoContainer`'s menu-bound shape,
   it genuinely doesn't have one.
7. **Ammo consumption is decoupled from projectile spawning** (proven by Maxwell/Tauon, which
   `consumeAmmo(conf.ammo)` without ever calling `spawnBullet`): once `BulletConfig`/
   `EntityBulletBaseMK4` exist, keep `consumeAmmo`/`getFirstConfigLoaded` as their own methods
   callable independently of `spawnBullet`, exactly matching CE's own separation, rather than folding
   ammo-checking into one "fire" call that assumes a projectile is always spawned.
8. **Damage types**: `ModDamageTypes.ELECTRICITY`/`.MICROWAVE` (Phase 0) already cover Tauon/Maxwell
   — no new `DamageType` datapack entries needed for either. The ballistic-bullet damage path
   (whatever `DamageSource` `EntityBulletBaseMK4`/`EntityDamageUtil` end up using) is out of this
   report's scope since it belongs to the deferred gun/ammo framework — cross-check it against
   `ModDamageTypes`'s existing catalogue when that framework is researched, rather than assuming a new
   entry is needed.
9. **Explosion/impact performance (PORT_SPEC's explicit concern)**: no turret TE itself removes
   blocks — that happens downstream, in bullet-ricochet/impact handling (`BulletConfig`'s
   `LAMBDA_STANDARD_RICOCHET` already shows a single-block `destroyBlock` call, which is fine at
   per-bullet scale) and, more importantly, in `EntityArtilleryShell`/`EntityArtilleryRocket`'s
   eventual explosion. **Flag now for whoever ports the artillery/missile package**: turret-fired
   ordnance is exactly the "hundreds of shells over a play session, each potentially clearing a
   multi-block crater" workload PORT_SPEC's "batched `LevelChunk` section writes + deferred lighting"
   requirement is about — a naive per-block `Level#setBlock` loop per shell impact will not perform
   acceptably at that call frequency, and this needs to reuse whatever batched explosion path Phase
   3's core destruction package builds, not reinvent its own.
10. **Entity registration pattern for the eventual `EntityBulletBaseMK4`/artillery entities** (out of
    this report's own scope to port, recorded for whoever does): this port already has a confirmed,
    working pattern from Phase 2 — `DeferredRegister<EntityType<?>> = DeferredRegister.create(
    BuiltInRegistries.ENTITY_TYPE, MODID)`, then
    `EntityType.Builder.<T>of(T::new, MobCategory.MISC).noSummon().sized(w, h)
    .setTrackingRange(n).build(name)` (see `entity/ConveyorEntityTypes.java`, registering
    `entity/item/EntityMovingItem.java`) — cross-checked and matching Neo Edition's own confirmed-real
    `BulletBaseMK4`/`NtmEntityTypes.BULLET_MK4` shape. No central `com.hbm.entity.ModEntityTypes`
    exists yet; each entity family currently owns its own `DeferredRegister`, and that file's own
    javadoc explicitly leaves "fold them into one shared registry" as an open call for whoever lands
    next — the bullet/artillery family can either follow suit or be the one to consolidate.

## Open questions / risks

1. **Sequencing risk**: landing turret TE shells before `BulletConfig`/`EntityBulletBaseMK4` produces
   classes whose `updateFiringTick()` has nothing to call. Recommend the gun/ammo framework be the
   very first Phase 3 package (or land in the same PR wave), since turrets, hand-held guns, and the
   grenade/flamethrower families (per `docs/phase1/DIGEST_REMAINDER.md` bucket (b)) all sit downstream
   of it.
2. **Scope split for artillery**: recommend explicitly excluding `TileEntityTurretArty`/`HIMARS` from
   the first turret PR — their missile/radar dependency chain is large and mostly unrelated to the
   other 11 turrets' bullet-only dependency. Confirm this split with whoever is sequencing Phase 3
   work packages before implementation starts.
3. **`CompatExternal` drop needs sign-off**: dropping the turret-target reflection hooks (recommended
   above, since no other mod is declared as a dependency anywhere in this repo) is a silent CE
   behavior difference that only matters if this port is ever run in a modpack with other content
   mods. Worth one explicit PORT_SPEC line either confirming the drop or naming a specific compat
   target to keep.
4. **Control-panel integration timing is unresolved**: is remote-automation control of turrets
   (`receiveEvent`/`"turret_set_target"`/`"turret_switch"`) in scope for *this* phase at all, or should
   it wait until whichever phase owns the 84-file `control_panel` package? Recommended default (leave
   `IControllable` unimplemented on the ported turret TE for now) needs confirmation, since it's a
   visible CE feature (remote control panels can already toggle these two turret behaviors) being
   silently dropped for an unspecified number of future phases.
5. **`GUITurretMobFilter`'s living-entity enumeration** (`EntityList.getEntityNameList()` filtered to
   classes assignable to `EntityLiving`, sorted, with live search-filtering) needs a confirmed 1.21.1
   equivalent — likely iterating `BuiltInRegistries.ENTITY_TYPE` and checking
   `EntityType#getCategory()`/`LivingEntity.class.isAssignableFrom(...)` via each type's factory, but
   this was **not** confirmed against a real NeoForge 1.21.1 API read in this pass (out of this
   report's scope — it belongs to whoever actually implements the mob-filter screen). Flag as
   something to verify then, not an API shape this report is asserting.
6. **ROR (redstone-over-radio) dispatch**: the `IRORInteractive`/`IRORValueProvider`/`IRORInfo`
   interfaces are already ported and already implemented by at least one other Phase 2 block entity
   (`FluidCounterValveBlockEntity`), confirming the pattern ("implement the interface methods
   directly, some external ROR system elsewhere calls them") is real and already in use — so wiring
   the turret's existing `runRORFunction`/`getFunctionInfo`/`provideRORValue`-shaped methods back in
   should be low-risk and mechanical, not a genuine open question, but is called out here since the
   central ROR dispatcher/registry itself was not located in this pass (may not need to be — the
   pattern looks purely interface-based, called from outside this package).
7. **`TileEntityTurretHowardDamaged`/`TileEntityTurretSentryDamaged`** were not read in full (small,
   presumed-trivial "damaged variant" subclasses) — confirm they're as trivial as they look before
   marking the Howard/Sentry family fully done.
