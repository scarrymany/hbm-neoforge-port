# Scripted playtest scenarios (Phase 6, `sy2-playtest-scenarios`)

The 5 scenarios PORT_SPEC.md's Phase 6 section names verbatim: (1) full progression chain
(ore -> steel -> assembler -> chemplant -> RBMK -> nuke), (2) radiation lifecycle, (3) one full
missile launch, (4) gun firing, (5) explosion perf benchmark (Tsar-scale under N seconds without
watchdog kill).

## 0. Verification status — read this first

**Nothing in this document has been executed.** This sandbox cannot run `./gradlew` (the build's
own dependency resolution hits `maven.neoforged.net`/`maven.blamejared.com`, both returning HTTP
403 from this session's egress proxy — a genuine network-policy denial, confirmed at task start,
not a fixable misconfiguration) and cannot launch a Minecraft client or dedicated server. **These
are test *scripts* written for a future human (or CI runner with real client/server access) to
carry out — not a report of results, not a claim that any of this "works" or "passes."** Every
step below is a *prediction* derived from careful, direct reading of this port's real committed
Java/JSON source (never assumed, never copied from CE without independently confirming the
matching id/mechanic exists in this port); §6 lists every place that reading surfaced a genuine,
confirmed gap that will make a specific step fail as designed, not by tester error.

**How each fact below was verified**, so a reader can judge how much to trust it:
- Every block/item/entity registry id quoted is copied verbatim from a real
  `DeferredBlock`/`DeferredItem`/`EntityType` registration call this port's `src/main/java`
  currently contains (file:line cited inline or in §7's reference table) — grepped and read
  directly, never guessed from the CE name.
- Every numeric constant (radii, power thresholds, tick durations, damage values) is copied from
  this port's own `config/*.java` `defineInRange(...)` defaults or the relevant class's field
  initializers — not CE's 1.12 numbers unless this port's code demonstrably still uses the same
  number.
- Every mechanic ("right-click opens X", "redstone triggers Y") is traced to the actual method
  that implements it in this port, cited inline. Where a mechanic could not be confirmed this way
  (e.g. the exact redstone-subscription geometry a launch pad listens on), the script says so and
  gives the general, code-confirmed trigger condition instead of an unverified specific one.
- Where reading turned up a mechanic that is **provably dead code in this port today** (e.g.
  `RBMKBaseBlockEntity.hasLid()` is never overridden anywhere), the affected scenario step is
  marked **EXPECTED FAIL** with the exact reason, rather than writing a script that pretends the
  path works.

## 1. Before you start

- All commands assume a single-player world or a dedicated server with the operator running them
  as an OP (`/give`, `/effect give`, `/summon`, `/gamerule`, `/data` all need OP level 2+).
- Build a flat or superflat creative test world away from spawn protection. Scenarios 1, 3, and 5
  involve explosions up to 500-block radius (§5) — use a fully expendable test world, not a saved
  world you care about.
- `hbm:battery_creative` (`com.hbm.items.machine.ItemBatteryCreative`, an infinite-charge
  creative-only battery — `getCharge` returns `Long.MAX_VALUE/2`, every discharge/charge call is a
  no-op) is the one power source every machine below needs. It is accepted directly in a battery
  *item slot* on every machine used in these scripts (confirmed generic check:
  `com.hbm.lib.Library.isBattery(ItemStack)` tests `instanceof IBatteryItem`, and
  `Library.chargeTEFromItems` is called from each machine's own `updateEntity()`/tick method against
  that slot) — no cable network or external generator needs to be built for any scenario here.
  `/give @s hbm:battery_creative 4` once at the start covers every machine below.
- Server log location: default `logs/latest.log` under the run directory (`run/` for a Gradle
  `runServer`/`runClient` task, per this project's `build.gradle`). Scenario 5's pass/fail leans on
  reading this log for watchdog warnings.
- `neoforge.enabledGameTestNamespaces` is already set to `hbm` for both the `client` and `server`
  Gradle runs (`build.gradle:28,34`), and §8's two GameTest classes need nothing further to be
  discovered — running `./gradlew runGameTestServer` (or the equivalent CI task) once the sandbox
  restriction above is lifted will pick them up automatically.

## 2. Scenario 1 — Full progression chain (ore → steel → assembler → chemplant → RBMK → nuke)

PORT_SPEC names this as one continuous chain. Reading this port's real recipe data shows CE's
actual "ore → steel" step (`upstream/hbm-ce/.../BlastFurnaceRecipes.java:273-277`: iron ore/ingot +
coal/coke in a Blast Furnace → `ingot_steel`) **has no equivalent anywhere in this port yet** — no
`BlastFurnace`/`Crucible` block, block entity, or recipe exists under `src/main/java/com/hbm` or
`src/main/resources/data/hbm/recipe` (repo-wide grep, zero hits beyond the word appearing in one
unrelated advancement-holder field name and this port's own hazard/`Mats.java` javadoc, which
already documents the same gap independently — see `docs/phase6/recipe_graph_audit.md` §2 finding
#1 for the sibling audit that found the same thing). `hbm:ingot_steel` itself is real and fully
functional as a *crafting ingredient* (`items/IngotNuggetItems.java:85`) — it is only the "obtain
it from ore" step that is currently a dead end. Step 1 below is written both ways: the CE-faithful
step that is **expected to fail** today, and the `/give` workaround that lets the rest of the chain
still be verified.

### Step 1 — ore → steel (EXPECTED FAIL, documented gap)

1. `/give @s minecraft:iron_ore 8` and `/give @s minecraft:coal 8`, then look for a way to smelt
   them into `hbm:ingot_steel` (a Blast Furnace multiblock, per CE).
   **PASS/FAIL**: **FAIL, by design** — no such block exists to place. Confirms the gap above is
   real and not a script error. Do not spend time hunting for it.
2. Workaround so the rest of this scenario is still testable: `/give @s hbm:ingot_steel 4`.

### Step 2 — steel → assembler (`hbm:ingot_steel` → `hbm:plate_steel`)

Real JSON recipe, confirmed at `src/main/resources/data/hbm/recipe/assembler/plate_steel.json`:
1 `hbm:ingot_steel` in, 1 `hbm:plate_steel` out, `duration: 60` ticks (3s), `power: 100`/tick.

1. `/give @s hbm:machine_assembly_machine 1` (registry id confirmed at
   `blocks/machine/ProcessingBlocks.java:47`) and place it.
2. Right-click the block to open its GUI (`inventory/gui/machine/MachineAssemblyMachineScreen.java`).
3. Put 1 `hbm:battery_creative` in **slot 0** (battery slot —
   `MachineAssemblyMachineBlockEntity.java:74`), leave the blueprint slot (slot 1) empty (confirmed
   decorative-only in this port: that class's own javadoc states the item is "accepted but not"
   consequential — no recipe here needs it), and put the `hbm:ingot_steel` in any of the input
   slots (slots 4-15).
4. Watch the GUI: a green progress bar should begin filling
   (`MachineAssemblyMachineScreen.java:26-27`, 0→24px over the recipe's 60-tick duration).

**PASS**: within ~3-4 seconds the green bar fills, the input `hbm:ingot_steel` is consumed, and 1
`hbm:plate_steel` appears in the output slot (slot 16) — with no exception in the server log.
**FAIL**: bar never appears/moves (power not registering — re-check the battery is in slot 0, not
elsewhere), or a server-side exception appears in the log the moment the block is placed or the
GUI is opened.

### Step 3 — chem plant (proves the machine functions; not fed by the same steel)

CE's Chemical Plant handles gas/liquid chemistry, not metal fabrication — there is no real CE
recipe (and hence no real recipe in this port) that consumes `plate_steel`/`ingot_steel` in a chem
plant. This step verifies the chem plant machine itself works, using one of its real ported
recipes, rather than inventing a steel-consuming recipe that doesn't exist in either codebase.
Recipe confirmed at `inventory/recipes/chem/ChemPlantRecipes.java:66-70` (`"chem.ethanol"`): 10×
`minecraft:sugar` → 1000mB of the mod's `Fluids.ETHANOL`, `duration: 100` ticks (5s), no fluid
input needed (the only fluid-input-free recipe in this port's chem plant table, chosen specifically
so this step needs no pipe network).

1. `/give @s hbm:machine_chemical_plant 1` (id confirmed at
   `blocks/machine/chem/ChemIsotopeBlocks.java:56`) and place it.
2. Right-click to open (`inventory/gui/machine/chem/ChemPlantScreen.java`).
3. Put 1 `hbm:battery_creative` in the battery slot (slot 6, confirmed
   `blockentity/machine/chem/ChemPlantBlockEntity.java:55`).
4. `/give @s minecraft:sugar 10` and place all 10 in any item-input slot.
5. Watch the GUI's status line (`ChemPlantScreen.java:34-37`): it should read
   `Processing: chem.ethanol` with a `Progress: N%` that climbs from 0 to 100, and the rightmost
   input-side tank's fill level should stay empty while the leftmost **output** tank gauge visibly
   fills (`ChemPlantScreen.java:26-29`, the `outputTanks[0]` gauge).

**PASS**: sugar is consumed, `Progress:` climbs to 100% over ~5 seconds, output tank gauge fills to
1000mB, `Processing:` reverts to `Idle`. No server exception in the log.
**FAIL**: status line never leaves `Idle` (battery slot wrong, or sugar not recognized — re-check
it's vanilla `minecraft:sugar`, not `hbm:sugar`), or an exception appears.

### Step 4 — RBMK reactor (minimal single-column proof)

CE's real RBMK is a multi-column lattice; this step uses the smallest configuration that still
exercises the real fuel-burn/heat simulation end to end — a single, unshielded fuel-rod column
loaded with a self-igniting ("neutron source") fuel, which generates heat with zero external flux
input by design (`ItemRBMKRod.burn()` step 1: `inFlux += selfRate`; confirmed
`rbmk_fuel_po210be`'s registration sets `selfRate=50`,
`EnumBurnFunc.PASSIVE` — `reactivityFunc` for `PASSIVE` is literally `selfRate * enrichment`, so
this rod produces nonzero output flux, and therefore heat, from the very first tick it's loaded,
alone, with no neighbors — `items/machine/rbmk/RBMKRods.java:170-173`).

1. `/give @s hbm:rbmk_rod 1` (id confirmed `blocks/machine/rbmk/RBMKBlocks.java:61`) and place it
   on flat ground with 4+ blocks of clearance above (it auto-fills a vertical multiblock column —
   default height 4 blocks including the core, `config/RBMKConfig.java:81-82`,
   `RBMKConfig.columnHeight` default `4`).
2. `/give @s hbm:rbmk_fuel_po210be 1` (id confirmed `items/machine/rbmk/RBMKRods.java:170`).
3. Right-click the column (non-sneak — sneaking passes through, confirmed
   `blocks/machine/rbmk/RBMKBaseBlock.java:114-115`) to open its fuel-channel GUI
   (`RBMKRodScreen.java`).
4. Insert the `hbm:rbmk_fuel_po210be` into the rod's single item slot.
5. Leave the GUI open (or re-open it every ~15s) and watch the `"Core/Hull: X / Y C"` line
   (`inventory/gui/machine/rbmk/RBMKRodScreen.java:38-40`).

**PASS**: the Core value (`X`) rises monotonically from its starting value over the next 60 real
seconds, with no server crash or exception in the log. (Numeric rate is deliberately not asserted
here — `heat=0.1` per the fuel's registration and the column's own passive-cooling/heat-diffusion
math combine into a rate this document does not independently re-derive; "rising" is the concrete,
checkable claim.)
**FAIL**: Core value stays flat or `NaN` appears, or a server exception/crash occurs.

### Step 5 — nuke (Fat Man, `hbm:nuke_man`)

Confirmed 6-slot two-stage casing, `blockentity/bomb/NukeManBlockEntity.java`: slot 0 igniter,
slots 1-4 explosive lenses (shared item with the Gadget), slot 5 core.
`BombConfig.MAN_RADIUS` default `175` blocks (`config/BombConfig.java:61-62`).

1. `/give @s hbm:nuke_man 1` (id confirmed `blocks/bomb/NukeCasingBlocks.java:61`) and place it in
   the open (nothing within ~200 blocks you mind losing).
2. `/give @s hbm:man_igniter 1`, `/give @s hbm:early_explosive_lenses 4`,
   `/give @s hbm:man_core 1` (all ids confirmed `items/bomb/NukeCasingItems.java:93-98`).
3. Right-click the casing to open its GUI; place the igniter in slot 0, the 4 lenses in slots 1-4,
   and the core in slot 5.
4. Place a lever directly against the casing and flip it on (redstone signal). Confirmed trigger:
   `NukeCasingBlockBase.java:103-107`, `neighborChanged` calls `explode(level, pos, null)` whenever
   `level.hasNeighborSignal(pos)` is true.

**PASS**: the casing block disappears, an explosion sound plays, and a large-radius crater/
destruction effect (`EntityNukeExplosionMK5`, spread across many ticks — see §5's perf notes,
same underlying algorithm at a smaller radius) resolves over the following seconds with no server
crash and no watchdog-kill message in the log (see §5 for the exact log text to search for).
**FAIL**: casing does not disappear/no explosion entity spawns (`BombReturnCode` other than
`DETONATED` — re-check all 6 slots hold the exact items above), or a crash/watchdog kill occurs.

## 3. Scenario 2 — Radiation lifecycle

**Read this before running**: this port's `hbm:geiger_counter` and `hbm:dosimeter` items compile
and are fully interactable, but their RAD/s readouts are **confirmed dead code** — both read
`HbmLivingAttachment.radBuf`/`.neutrons` (`items/tool/ItemGeigerCounter.java:80`,
`items/tool/ItemDosimeter.java:44`), and a repo-wide grep for `setRadBuf(`/`incrementNeutrons(`/
`setNeutrons(` outside `com.hbm.capability` itself returns **zero hits** — nothing in this port's
currently-committed code ever writes to either field. Both tools will print `"...RAD/s: 0.000"`
(or silence, for the ambient click sound) regardless of real exposure. This is a real,
already-present gap, not a scenario-authoring mistake — flagged here rather than written around
silently. The player's actual contamination total is a *separate*, correctly-wired field
(`HbmLivingAttachment.rads`, read/written via `com.hbm.capability.HbmLivingProps.getRadiation`/
`incrementRadiation` — confirmed live via `com.hbm.util.ContaminationUtil.contaminate(...,
HazardType.RADIATION, ...)`, itself called from real hazard/potion/explosion code, see below) — it
is only the two *display* items that are broken. This scenario therefore verifies the underlying
mechanic through an effect that **is** live-wired, rather than through the broken UI.

### Part A — exposure and its confirmed knock-on effect

`hbm:fallout` (id confirmed `blocks/generic/FalloutBlocks.java:30`) is a real, placeable,
self-perpetuating radiation-source block: on placement and every 10-40 ticks after, it calls
`ChunkRadiationManager.proxy.incrementRad(level, pos, 1, 100)` (chunk radiation +1/tick-ish, capped
at 100 — `blocks/generic/BlockFallout.java:80-84`), and unconditionally applies the
`hbm:radiation` potion effect (2400 ticks, amplifier 14) to any entity that steps on it
(`BlockFallout.java`'s `stepOn` override). That potion effect (`potion/RadiationEffect.java`) ticks
*every* tick it's active, each tick calling `ContaminationUtil.contaminate(entity, RADIATION,
CREATIVE, (14+1)*0.05 = 0.75)` — a real, live, per-tick contamination increment (confirmed chain:
`RadiationEffect.applyEffectTick` → `ContaminationUtil.contaminate` → `HbmLivingProps
.incrementRadiation` → `HbmLivingAttachment.increaseRads`).

1. `/give @s hbm:fallout 8` and lay down a small 2×2 patch on the ground.
2. `/summon minecraft:cow ~2 ~ ~` next to the patch, then push/lead it onto the fallout blocks (or
   place the fallout blocks under a cow already there).
3. Wait up to ~10-15 real seconds once the cow is standing on the fallout blocks.

**PASS**: the cow transforms into a `minecraft:mooshroom`. This is a deterministic, code-confirmed
side effect once the cow's own accumulated contamination crosses 50
(`handler/EntityEffectHandler.java:171-176`: `if (entity instanceof Cow cow ... && eRad >= 50)` →
mutate to `MushroomCow`, no additional random roll on this branch) — at +0.75/tick from the potion
effect alone, 50 is crossed in well under 15 seconds. This is independent of, and does not rely on,
the broken Geiger/Dosimeter readouts above.
**FAIL**: no mutation after 30 seconds, or a server exception appears — re-check the cow is
actually standing on (not just near) the fallout blocks (the effect applies via `stepOn`, not
proximity).

4. Now stand on the same patch yourself for ~15-20 seconds (as a non-creative/non-spectator
   player — the mutation-cascade gate that excludes those modes only applies to the mutation
   check, not to `incrementRadiation` itself, but your own accumulated contamination has no other
   currently-wired in-game display — see the callout above).
5. Right-click with `hbm:geiger_counter` in hand.

**PASS/FAIL is informational, not blocking**: the chat message `"Ambient dose: 0.000 RAD/s"` is
**expected** here per the confirmed gap above — this is not a failure of *this* step, it is
independent confirmation the gap is real and reproducible in a live client, worth relaying back to
whichever pass owns `com.hbm.capability`/`com.hbm.items.tool` next. If a future fix wires
`radBuf`/`neutrons`, this exact repro (stand on `hbm:fallout`, right-click Geiger counter) is the
regression check for it.

### Part B — held-item contamination (secondary check)

1. `/give @s hbm:pellet_rtg_lead 1` (id confirmed `items/machine/MachineItems.java:472`, one of the
   RTG pellets registered with a real radiation-hazard entry via `HazardRegistry.registerRTGPellet`
   — heat rating 600, the strongest of the ten registered RTG pellets) and keep it in your
   inventory (not necessarily your hand — `HazardTypeRadiation.onUpdate` is driven by a
   per-inventory-slot hazard scan, matching CE).
2. Repeat the cow test above (step onto a fresh `hbm:fallout`-free patch is not needed here —
   this is testing the *held item* path specifically) is not directly observable the same way
   (a held item irradiates *you*, not nearby mobs) — so this part's pass condition is narrower:

**PASS (narrow)**: no server exception occurs over 60 seconds of carrying the pellet, and — if a
`hbm:hev_helmet`+`hbm:hev_plate` (`items/armor/PoweredArmorItems.java:336-338`) suit is worn
instead — the HEV suit's own from-scratch HUD overlay (`items/armor/ArmorHEV.java`, confirmed to
read `HbmLivingAttachment.getData(player).getRads()` directly, **not** the broken `radBuf`/
`neutrons` fields) shows a nonzero, rising `RAD/s` delta line. This is this port's one confirmed
*working* live radiation-total display; note it requires the HEV suit specifically, not the plain
Geiger counter/Dosimeter.
**FAIL**: server exception, or (with the HEV suit) the delta line stays at `<1`/`0` for the full 60
seconds despite carrying the pellet.

### Part C — curing exposure

This port registers the `hbm:radaway` `MobEffect` (`potion/HbmPotionEffects.java:63-64`,
`potion/RadawayEffect.java` — confirmed to call `HbmLivingAttachment.decreaseRads`, the only
call site anywhere in this port's `src/main/java` that ever reduces a living entity's contamination
total; there is no passive decay-over-time otherwise) but a repo-wide grep found **no item
anywhere** in `items/food` or elsewhere that actually applies it to a player (a real, confirmed
content gap — CE has a Radaway consumable; this port has the effect class but nothing that grants
it). Use the vanilla effect command as the only currently-available way to test the cure path:

1. `/effect give @s hbm:radaway 30 4`
2. If wearing the HEV suit from Part B, watch its overlay's delta line fall back toward `0`/`<1`
   over the following ~10-15 seconds while the effect is active.

**PASS**: no exception; HEV overlay (if worn) shows the delta trending down while `hbm:radaway` is
active.
**FAIL**: exception, or (with HEV worn) no visible downward trend.

## 4. Scenario 3 — One full missile launch

Uses `hbm:missile_micro` (`MissileFormFactor.MICRO` → `MissileFuel.SOLID`, `fuelCap=0` — confirmed
`items/weapon/ItemMissileStandard.java:67-75`, `items/weapon/MissileItems.java:178`) specifically
because its **solid fuel needs no fluid tanks filled** (`LaunchPadBaseBlockEntity.hasFuel()`:
`tanks[0].getFill() < missile.fuelCap` is `0 < 0 = false` for a solid-fuel missile, so the tank
check trivially passes) — this keeps the script to power + designator + missile, with no fluid-pipe
network required.

1. `/give @s hbm:launch_pad 1` (id confirmed `blocks/bomb/BombBlocks.java:125`) and place it with
   open sky above.
2. `/give @s hbm:missile_micro 1` (id confirmed `items/weapon/MissileItems.java:178`).
3. `/give @s hbm:designator_manual 1` (id confirmed `items/tool/LaunchInfraItems.java:47`).
4. Right-click with the designator manual **in hand, before placing it** — this opens a
   client-side GUI (`inventory/gui/DesignatorManualScreen.java`, confirmed
   `items/tool/ItemDesignatorManual.java:57-58`) to type target X/Z coordinates. Enter coordinates
   roughly 50-100 blocks away from the pad (far enough to see travel, close enough to watch the
   whole flight) and save. The item's tooltip should now read `"Target: X: .. Z: .."`
   (`ItemDesignatorManual.java:47-49`).
5. `/give @s hbm:battery_creative 1` if you haven't already.
6. Right-click the launch pad to open its GUI (`inventory/gui/LaunchPadScreen.java`). Place:
   - slot 0: `hbm:missile_micro` (confirmed `LaunchPadBaseBlockEntity.java:196`,
     `isItemValidForSlot(0, ...)`)
   - slot 1: the pre-configured `hbm:designator_manual`
   - slot 2: `hbm:battery_creative` (confirmed `LaunchPadBaseBlockEntity.java:220`,
     `chargeTEFromItems(inventory, 2, ...)`)
7. Wait until the pad's `power` reaches its firing threshold — `hasFuel()` requires `power >=
   75_000` (`LaunchPadBaseBlockEntity.java:369`) out of a `MAX_POWER` of `100_000`
   (`LaunchPadBaseBlockEntity.java:159`); with the infinite-charge creative battery this should be
   effectively instant (well under one second — `chargeTEFromItems` uses the battery's own
   `getChargeRate`, which for `ItemBatteryCreative` is `Long.MAX_VALUE/100`).
8. Place a lever against the pad and flip it on. Confirmed trigger:
   `LaunchPadBaseBlockEntity.updateEntity()` (lines 214-217): a redstone-signal **rising edge**
   (`redstonePower > 0 && prevRedstonePower <= 0`) calls `launchFromDesignator()`.

**PASS**: a missile entity visibly launches from the pad, flies toward the configured target
coordinates, and detonates at/near them (small explosion — `missile_micro` is a Tier0/`MICRO`
warhead, deliberately the smallest launchable missile in this port's roster, chosen to keep this
scenario's blast radius modest and separate from Scenario 5's stress test). No server crash or
exception in the log at any point from step 8 through detonation.
**FAIL**: no entity spawns (re-check all 3 slots and that `power` actually reached 75,000 — the
GUI's power bar, `drawElectricityInfo`, should read near-full), the missile does not move toward
the target (designator coordinates not actually saved — re-check the tooltip before inserting it),
or a crash/exception occurs.

## 5. Scenario 4 — Gun firing

Uses `hbm:gun_light_revolver` (confirmed `items/weapon/sedna/content/GunPistolItems.java:88`, via
`XFactory357.gun_light_revolver()`) with `hbm:m357_fmj` ammunition (one of six .357 rounds this
specific gun accepts — confirmed `items/weapon/sedna/content/XFactory357.java:90`,
`.mag(new MagazineFullReload(0, 6).addConfigs(m357_bp, m357_sp, m357_fmj, m357_jhp, m357_ap,
m357_express))`). Real stats from that same `Receiver` definition
(`XFactory357.java:88-92`): `dmg(7.5F)` damage/hit, `delay(16)` ticks between shots, a 6-round
cylinder (`MagazineFullReload(0, 6)`), `reload(55)` ticks to reload.

Firing is bound to a real, registered keybind — **not** vanilla left-click-to-attack. Confirmed
`items/weapon/sedna/ItemGunBaseNT.java:246-250` (`canHandleKeybind` accepts
`HbmKeybinds.EnumKeybind.GUN_PRIMARY`/`GUN_SECONDARY`/`GUN_TERTIARY`/`RELOAD`) and
`handler/HbmKeybinds.java`: `gunPrimaryKey` defaults to the **left mouse button**
(`InputConstants.MOUSE_BUTTON_LEFT`), and `reloadKey` defaults to the **R key**
(`InputConstants.KEY_R`) — both distinct `KeyMapping`s from vanilla attack/use, registered on the
mod bus via `RegisterKeyMappingsEvent`.

1. `/give @s hbm:gun_light_revolver 1`.
2. `/give @s hbm:m357_fmj 12` (two full reloads' worth — `IMagazine` reloading scans the player's
   inventory for matching ammo, no manual "load into gun" step is needed beyond having the ammo in
   inventory).
3. Hold the revolver in your main hand, aim at open air or a target dummy/mob.
4. Hold down the left mouse button (default `GUN_PRIMARY` bind) for one shot, release, repeat 6
   times (one full cylinder). Watch/listen for a fire sound and any hit-marker/damage on a target.
5. After the 6th shot, hold the left mouse button again (or wait for the gun's own state machine to
   request it) and confirm it does **not** fire — the cylinder should be empty.
6. Press and hold **R** to reload.
7. Once reloading completes, verify the gun fires again on left-click.

**PASS**: each of the first 6 left-click presses produces one shot at roughly `delay=16`-tick
(~0.8s) minimum spacing (rapid double-taps faster than that should not double-fire), each shot
consumes 1 `hbm:m357_fmj` from inventory, a 7th press before reloading produces no shot, R-holding
visibly reloads over roughly `reload=55` ticks (~2.75s) and restores the ability to fire, and no
server exception occurs at any point. A gun jamming mid-test (a real, wear-linked mechanic —
`items/weapon/sedna/factory/GunStateDecider.java:104`, jam probability ramps from 0% at <66% max
wear to 100% at ≥91% wear, evaluated once per reload — not expected in this short a test since
`dura(300)` gives far more than 12 shots of headroom, but not a bug if it does occur) is expected
CE-parity behavior, not a failure, as long as it self-clears after its configured
`jam(45)`-tick duration without further input.
**FAIL**: no fire sound/no ammo consumed on left-click (keybind not registered — check
`key.categories.hbm.gunPrimary` appears in the controls menu), ammo other than the 6 listed .357
rounds is silently accepted or `m357_fmj` is rejected, reload never completes, or a server
exception/crash occurs.

## 6. Scenario 5 — Explosion perf benchmark (Tsar-scale)

Uses `hbm:nuke_tsar` (id confirmed `blocks/bomb/NukeCasingBlocks.java:63`), the largest
player-buildable explosive in this port. `BombConfig.TSAR_RADIUS` default `500` blocks
(`config/BombConfig.java:65-66`). Confirmed 6-slot casing
(`blockentity/bomb/NukeTsarBlockEntity.java`): slots 0-3 = `hbm:explosive_lenses` ×4 (**not** the
`early_explosive_lenses` Fat-Man uses a different item, confirmed by direct read of that class's
`isReady()`), slot 4 = `hbm:man_core`, slot 5 = `hbm:tsar_core` (the second-stage item that
upgrades the yield from `MAN_RADIUS` to the full `TSAR_RADIUS` — confirmed
`blocks/bomb/NukeTsarBlock.java:60`: `filled ? BombConfig.TSAR_RADIUS.get() :
BombConfig.MAN_RADIUS.get()`).

**Why this is a real perf question, not just a big number**: this port's own committed code
confirms only the *legacy, single-threaded, tick-spread* explosion algorithm
(`com.hbm.explosion.ExplosionNukeRayBatched`) is wired up — its own class javadoc states "every mk5
detonation currently runs the single-threaded batched algorithm regardless of the configured value"
(`entity/logic/EntityNukeExplosionMK5.java:35-40`), because the fully-threaded default algorithm
CE ships (`ExplosionNukeRayParallelized`) is explicitly out of scope for this port so far
(`docs/phase3/explosion_engine.md`'s "Deferred scope"). The batched algorithm *is* confirmed to
spread its work across many ticks by design (`explosion/ExplosionNukeRayBatched.java:20-31`: a
2-phase `cacheChunksTick(int)`/`destructionTick(int)` state machine, each phase draining a bounded
amount of work per call, and PORT_SPEC's own batched-`LevelChunk`-section-write mandate is
confirmed followed at that file's lines ~40-50) — but "spread across many ticks" still means a
500-radius detonation could plausibly take a *long* wall-clock time to fully resolve, or (if the
per-tick budget for either phase is too large) a *single* tick could still run long enough to trip
the vanilla/NeoForge watchdog. Both are real, distinct risks this benchmark is designed to catch.

1. Set up a dedicated server (this test is specifically about *server* tick health — a
   single-player integrated server works but a real dedicated server plus a spectating client is
   closer to the intended target). Note your `server.properties` `max-tick-time` (vanilla default
   `60000`, i.e. 60 seconds — the watchdog force-kills the server if one tick exceeds this).
2. `/give @s hbm:nuke_tsar 1` and place it somewhere with at least ~550 blocks of clearance in
   every direction (check the world border / how far you are from spawn chunks you don't want
   destroyed).
3. `/give @s hbm:explosive_lenses 4`, `/give @s hbm:man_core 1`, `/give @s hbm:tsar_core 1` (ids
   confirmed `items/bomb/NukeCasingItems.java:98-108`).
4. Right-click the casing to open its GUI and place all 6 items per the slot layout above.
5. **Before** triggering: note the wall-clock time, and tail the server log
   (`tail -f logs/latest.log` or the client's dev-console equivalent) watching specifically for the
   string `"Considering it to be crashed, server will forcibly shutdown"` (the vanilla watchdog's
   exact log message when a single tick exceeds `max-tick-time`) and for the standard "Running X ms
   behind, skipping Y tick(s)" server-lag warning frequency.
6. Place a lever against the casing and flip it on (same `hasNeighborSignal` trigger as Scenario
   1's nuke — `blocks/bomb/NukeCasingBlockBase.java:103-107`).
7. From the moment the casing block disappears, measure wall-clock time until the destruction
   visibly stops expanding/settles (the `EntityNukeExplosionMK5` entity removing itself is the
   authoritative "done" signal if you have log/debug access to entity lifecycle; visually, when the
   crater stops growing and dust/fire settles).

**PASS** (suggested default `N`; adjust to your hardware and state the `N` you actually used when
reporting results): the whole destruction sequence completes within **5 minutes** of wall-clock
time, the watchdog-kill log line above never appears, and the server keeps accepting player input
(walk around, run `/list`) *throughout* the sequence, not just before/after it — i.e. no single
tick freezes the server even momentarily. Zero server-side exceptions in the log.
**FAIL**: the watchdog-kill message appears (server force-restarts) at any point, the server
becomes unresponsive to commands for longer than a few seconds at a stretch, an exception/stack
trace appears in the log, or the destruction sequence never completes/visibly hangs indefinitely.
**Also record** (informational, not pass/fail by itself, but valuable data for whoever reads this
report): the total wall-clock duration, and whether `Running Xms behind` warnings cluster tightly
around detonation or are spread evenly — clustering suggests the per-tick work budget in
`ExplosionNukeRayBatched` is too large for this radius and is a concrete tuning target for a future
pass, even if nothing here technically "fails."

## 7. Registry-id quick reference

Every id below was confirmed against a real registration call in this port's `src/main/java`
(file:line cited) at the time this document was written — re-grep before relying on this table if
the source has moved since.

| Purpose | Registry id | Source |
|---|---|---|
| Infinite creative battery | `hbm:battery_creative` | `items/machine/MachineItems.java:144` |
| Steel ingot (input-only, see §2 Step 1) | `hbm:ingot_steel` | `items/IngotNuggetItems.java:85` |
| Steel plate (assembler output) | `hbm:plate_steel` | `src/main/resources/data/hbm/recipe/assembler/plate_steel.json` |
| Assembler block | `hbm:machine_assembly_machine` | `blocks/machine/ProcessingBlocks.java:47` |
| Chemical plant block | `hbm:machine_chemical_plant` | `blocks/machine/chem/ChemIsotopeBlocks.java:56` |
| RBMK fuel-rod column | `hbm:rbmk_rod` | `blocks/machine/rbmk/RBMKBlocks.java:61` |
| RBMK neutron-source fuel | `hbm:rbmk_fuel_po210be` | `items/machine/rbmk/RBMKRods.java:170` |
| Fat Man casing | `hbm:nuke_man` | `blocks/bomb/NukeCasingBlocks.java:61` |
| Fat Man igniter/lenses/core | `hbm:man_igniter`, `hbm:early_explosive_lenses`, `hbm:man_core` | `items/bomb/NukeCasingItems.java:93-98` |
| Tsar Bomba casing | `hbm:nuke_tsar` | `blocks/bomb/NukeCasingBlocks.java:63` |
| Tsar lenses/core/tsar-core | `hbm:explosive_lenses`, `hbm:man_core`, `hbm:tsar_core` | `items/bomb/NukeCasingItems.java:98-108` |
| Fallout ash (radiation source block) | `hbm:fallout` | `blocks/generic/FalloutBlocks.java:30` |
| RTG pellet (radioactive held item) | `hbm:pellet_rtg_lead` | `items/machine/MachineItems.java:472` |
| Geiger counter (readout is dead, see §3) | `hbm:geiger_counter` | `items/tool/ToolItems.java:273` |
| HEV powered armor (live radiation HUD) | `hbm:hev_helmet`, `hbm:hev_plate` | `items/armor/PoweredArmorItems.java:336-338` |
| `radaway` mob effect (no item grants it — see §3) | `hbm:radaway` | `potion/HbmPotionEffects.java:63-64` |
| Small launch pad | `hbm:launch_pad` | `blocks/bomb/BombBlocks.java:125` |
| Manual-target designator | `hbm:designator_manual` | `items/tool/LaunchInfraItems.java:47` |
| Smallest launchable missile (solid fuel) | `hbm:missile_micro` | `items/weapon/MissileItems.java:178` |
| .357 revolver | `hbm:gun_light_revolver` | `items/weapon/sedna/content/GunPistolItems.java:88` |
| .357 FMJ ammo | `hbm:m357_fmj` | `items/weapon/sedna/content/GunPistolItems.java` (`registerAmmo`) |

## 8. GameTest coverage (bonus, server-side/scriptable subset)

Two real (best-effort, **unverified — cannot be compiled or run in this sandbox**) NeoForge
`GameTest` classes under `src/main/java/com/hbm/gametest/` automate the parts of Scenarios 1 and 5
that are purely server-side state machines with no real player input (place block → insert items →
tick → assert a numeric/state change) — exactly the two PORT_SPEC's own Phase 6 wording calls out
as fitting the framework. Scenarios 2-4 are **not** forced into GameTest: 2 leans on a client-side
HUD readout and a broken-vs-working display distinction that needs a human's eyes, 3 needs the
designator's client-only `Screen`, and 4 is fundamentally about held-item input timing feel — a
precise manual script (as written above) is the right and sufficient deliverable for each, per this
task's own instruction not to force GUI/feel-dependent scenarios into automated tests.

- `com.hbm.gametest.ProgressionChainGameTests` — three `@GameTest` methods covering §2 Steps 2-4
  (assembler produces `plate_steel`, chem plant produces ethanol, RBMK rod's core heat rises),
  each driving the real `BlockEntity` classes directly (`setBlock`, fetch the `BlockEntity`,
  insert `ItemStack`s into its `IItemHandler` inventory, `runAtTickTime`/`succeedWhen` to poll for
  the expected state change, `helper.succeed()`/`helper.fail(...)` per vanilla `GameTestHelper`
  convention) rather than simulating player right-clicks.
- `com.hbm.gametest.ExplosionPerfGameTests` — one `@GameTest` method covering §6: places and fills
  a `hbm:nuke_tsar` casing, triggers `explode(...)` directly (bypassing the redstone-lever step,
  since `GameTestHelper` has no player to place one), then polls once per tick recording
  `System.nanoTime()` deltas, failing the test if any single recorded gap exceeds a configurable
  millisecond threshold (a programmatic proxy for "would this have tripped the watchdog") or if the
  detonation entity has not self-removed within the test's `timeoutTicks`.

**Uncertainty flagged explicitly, per this task's own instruction to say what is/isn't verified**:
the exact shape of NeoForge's `@GameTestHolder`/`@PrefixGameTestTemplate`/`EmptyTemplate`
annotations (used below so these tests need no companion structure-NBT asset file) was not
cross-checked against a real compiled NeoForge 1.21.1 jar or against `upstream/neo-edition` — a
repo-wide grep of `upstream/neo-edition` for `GameTest` returned **zero hits**, so there is no
in-repo precedent to cross-check against for this specific piece of API surface, unlike every id in
§7. The vanilla `net.minecraft.gametest.framework.GameTest`/`GameTestHelper` calls these classes
otherwise use are long-stable, well-documented vanilla API and carry much higher confidence. If
`EmptyTemplate`'s exact field names differ from what's written, the fix is mechanical (adjust the
annotation's arguments to match the real signature) and does not change either test's actual logic.
