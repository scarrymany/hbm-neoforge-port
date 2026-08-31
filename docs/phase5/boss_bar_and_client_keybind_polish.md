# Boss bar client rendering + keybind infrastructure audit (Phase 5)

**Area:** `boss_bar_and_client_keybind_polish` — two narrow, related client-UX questions: (1) does
Phase 4's `ServerBossEvent`/`startSeenByPlayer`/`stopSeenByPlayer` wiring on the 4-or-5 boss entities
actually render a client-side boss bar with zero further client code, and (2) does this port's already-
real keybind infrastructure (`HbmKeybinds`/`HbmKeybindInputEvents`) cover every CE keybind, and does the
whole client→server pipeline those keybinds ride on actually work end to end.

Per the task's own instruction, this report is deliberately narrower than the other Phase 5 waves and
spends its budget re-verifying its two claims rather than expanding scope.

## Sources read

**This port's own `src/main/java` (already-committed, already-compiling code) — read in full or grepped
to exhaustion, since both sub-questions are about *this port's* wiring, not new CE behavior to port:**
- `com/hbm/entity/mob/{EntityMaskMan (217), EntityUFO (404), EntityHunterChopper (410),
  EntityBOTPrimeHead (215), EntityQuackos (155), EntityBOTPrimeBody (143)}.java` — all 6 read in full or
  grepped exhaustively for every `bossEvent`/`ServerBossEvent`/`startSeenByPlayer`/`stopSeenByPlayer`/
  `remove(Entity.RemovalReason` occurrence (see Headline finding #2)
- `com/hbm/handler/{HbmKeybinds.java (117), HbmKeybindInputEvents.java (96)}` — both read in full
- `com/hbm/main/{ClientProxy.java (43), ServerProxy.java (23)}` — both read in full (the
  `getIsKeyPressed(EnumKeybind)` switch that is CE's `ClientProxy.getIsKeyPressed` equivalent)
- `com/hbm/capability/HbmPlayerAttachment.java` (258 lines, read in full — CE's `HbmCapability`/
  `IHBMData` equivalent; the origin of Headline finding #3)
- `com/hbm/packet/toserver/KeybindPacket.java` (80 lines, read in full) and its registration line in
  `com/hbm/packet/HbmNetwork.java` (grepped, confirmed registered — `registrar.playToServer(KeybindPacket
  .TYPE, ...)`)
- `com/hbm/items/gear/{JetpackRegular.java (60), JetpackVectorized.java (73), JetpackBooster.java (74),
  JetpackBreak.java (82)}` and `com/hbm/items/armor/JetpackFueledBase.java` (94) — all 4 concrete fueled
  jetpacks read in full/grepped for `isJetpackActive`/`getKeyPressed(EnumKeybind.JETPACK)` (Headline
  finding #3's blast radius)
- `com/hbm/blockentity/network/PipeBaseBlockEntity.java` (219, grepped for `EnumKeybind.TOOL_CTRL`) and
  `com/hbm/items/tool/ItemSettingsTool.java` (93, read in full — confirms CE's `TOOL_ALT`-driven
  display-paging was deliberately simplified away, a documented Phase-2-era decision, not this report's
  finding)
- Repo-wide greps: `EnumKeybind\.` across `com.hbm` (every consumer of every keybind, both ported and
  CE), `ServerBossEvent`/`BossEvent`/`startSeenByPlayer`/`stopSeenByPlayer` across `com.hbm.entity.mob`
  and `com.hbm.handler`, `setKeyPressed`/`getKeyPressed` across the whole tree, `RemovalReason` across
  `com.hbm.entity` (to establish this port's own established precedent for that override, see Headline
  finding #2), `Crane` across `src/main/java/com/hbm` (to confirm no RBMK crane console exists under any
  name)

**CE (`upstream/hbm-ce`) — read for ground truth:**
- `com/hbm/handler/{HbmKeybinds.java (248, full), HbmKeybindsServer.java (56, full)}` and
  `com/hbm/main/ClientProxy.java` (629 lines total; the `getIsKeyPressed(EnumKeybind)` switch at
  lines 412-436 read in full, rest grepped) — the complete real keybind list and its client/server
  dispatch, the direct source of truth for sub-question 2
- `com/hbm/items/weapon/sedna/ItemGunBaseNT.java` (line 252-274, `handleKeybind`'s `EnumKeybind` dispatch
  table), `com/hbm/items/gear/JetpackVectorized.java` (81, full), `com/hbm/items/tool/{ItemSettingsTool
  .java (190, grepped), ItemToolAbility.java (grepped for `ABILITY_CYCLE`)}`,
  `com/hbm/tileentity/network/TileEntityPipeBaseNT.java` (214, grepped for `TOOL_CTRL`),
  `com/hbm/tileentity/machine/rbmk/TileEntityRBMKCraneConsole.java` (740 lines, header read — confirms
  its `com.hbm.inventory.control_panel` dependency) — every real consumer of every `EnumKeybind` value,
  found by grepping `EnumKeybind\.` across all of `upstream/hbm-ce/src/main/java/com/hbm`
- Greps for `throttle`, `dismount`/`Dismount`, `fireMode`/`FireMode` across all of
  `upstream/hbm-ce/src/main/java/com/hbm` — specifically to check the task prompt's own speculative
  examples ("jetpack-throttle key", "gun fire-mode-switch key", "vehicle-dismount key") against real CE
  code (see Headline finding #1)
- `com/hbm/entity/mob/{botprime/*, EntityMaskMan, EntityUFO, EntityHunterChopper, EntityQuackos}.java`
  not independently re-read in full here — already fully read by `docs/phase4/entities_bosses.md`
  (cited below); this report's own contribution is auditing *this port's* resulting code, not
  re-deriving CE's boss behavior a second time

**Neo Edition (`upstream/neo-edition`) — checked strictly for API-shape corroboration, per this task's
ground rules:**
- Grep for `ServerBossEvent|BossEvent|BossBarColor|startSeenByPlayer|stopSeenByPlayer|
  ClientboundBossEventPacket` across the whole tree — **zero hits**. Neo Edition has ported no boss
  content (confirmed independently by `docs/phase4/entities_bosses.md`); this report's boss-bar claim
  has no Neo-Edition corroboration and none was found, consistent with every prior report that touched
  this API surface
- `com/hbm/handler/HbmKeybinds.java` — grepped for `handleOverlap`; confirms Neo Edition's own port also
  carries CE's overlap-suppression hack commented-out and unused (this port's own `HbmKeybinds.java`
  javadoc claims this Neo-Edition precedent; verified true rather than taken on faith)

**Prior research/status this report builds on rather than re-deriving:**
- `docs/phase4/entities_bosses.md` (already read in full for a sibling report this session; re-cited
  here, not re-read line-by-line) — its Key design decisions section is the origin of the
  `ServerBossEvent`/`startSeenByPlayer` shape this port's boss classes actually implement, and it
  already explicitly flagged that shape as unverified against a compiled jar
- `docs/phase5/hud_overlays_geiger_armor_gun.md` (already published this wave) and
  `docs/phase5/boss_and_vehicle_entity_renderers.md` (already published this wave) — both independently
  re-confirm "boss bars are 100% vanilla plumbing, zero custom rendering" from their own angle (HUD
  overlay survey and entity-renderer survey respectively); this report is a **third** independent
  confirmation, from the boss classes' own source, not a copy of either
- `docs/phase4/STATUS.md` (line 174, grepped) — already flags "the boss-bar `ServerBossEvent` surface
  used by 4 bosses" as one of the specific NeoForge API shapes this sandbox could not jar-verify; see
  Headline finding #4 for why "4" is itself slightly wrong
- `docs/phase2/rbmk_reactor.md` ("Package B", read for a sibling report this session) — already named
  `TileEntityRBMKCraneConsole` as blocked on the unported `com.hbm.inventory.control_panel` network;
  this report adds the specific detail that it is *also* the sole real consumer of 5 of this port's 18
  registered keybinds

## Headline findings

1. **The task's own speculative keybind examples don't exist in CE — confirmed by grep, not assumed.**
   The task asks whether CE has "a jetpack-throttle key, a gun fire-mode-switch key, a vehicle-dismount
   key for the new Phase 4 rail cars" with no 1.21.1 equivalent. None of the three exist in CE:
   - No keybind or `EnumKeybind` value relates to jetpack throttle in any way — CE's jetpacks are
     binary (thrust while `JETPACK`/vanilla-jump is held, or not), confirmed by reading every
     `Jetpack*`/`JetpackVectorized` file; the 3 "throttle" hits in all of `upstream/hbm-ce` are
     `TileEntityMachineCombustionEngine`/`TileEntityMachineTurbineGas`/their GUI — unrelated machine
     RPM settings, not a player keybind.
   - CE has no gun fire-mode-switch key. `EnumKeybind.ABILITY_CYCLE` is real and does cycle something,
     but its only consumer (confirmed by grep) is `ItemToolAbility` — mining-tool ability presets
     (hammer/recursion/explosion excavation modes), not guns. Guns instead have three fixed,
     independently-bound roles (`GUN_PRIMARY`/`GUN_SECONDARY`/`GUN_TERTIARY`, all three read directly by
     `ItemGunBaseNT.handleKeybind`'s dispatch table, CE lines 252-274) whose *meaning* per shot is
     defined by each gun's own `GunConfig`, not chosen by a fire-mode key at all.
   - CE has no vehicle-dismount keybind. Every `dismountRidingEntity()` call in CE (`SatelliteResonator`,
     `ItemAnchorRemote`, `ItemDiscord` — 3 hits total, all grepped) is triggered by using an *item*, not
     a keybind; ordinary vehicle dismounting relies on vanilla's built-in sneak-to-dismount, which needs
     no HBM-specific keybind in CE or in this port.

   This port's own `HbmKeybinds.EnumKeybind` already has all 18 real CE values (`JETPACK`,
   `TOGGLE_JETPACK`, `TOGGLE_HEAD`, `TOGGLE_MAGNET`, `RELOAD`, `DASH`, `CRANE_UP/DOWN/LEFT/RIGHT/LOAD`,
   `ABILITY_CYCLE`, `ABILITY_ALT`, `TOOL_ALT`, `TOOL_CTRL`, `GUN_PRIMARY/SECONDARY/TERTIARY`), in the
   same order (load-bearing, since ordinal is the wire format), with the same default physical key for
   every single one — including CE's two non-obvious mouse-button defaults (`abilityCycle` and
   `gunSecondaryKey` both default to CE's raw `-99`, which is Forge-1.12's `-100 - buttonIndex` encoding
   for right-mouse-button-1; this port's equivalents both correctly default to
   `InputConstants.MOUSE_BUTTON_RIGHT`) and CE's deliberate `abilityCycle`/vanilla-"use item" key
   collision (both default to right-click; CE's own comment calls this "a shitty hack" it needs
   `postClientTick` to work around, and this port's `HbmKeybindInputEvents.onClientTick` reproduces the
   exact same end-of-tick collision check). **The keybind *list* itself needed no port work — it is
   already 100% complete and correct.** The one real, intentional deviation (`GUN_PRIMARY` gets an
   actual bound `KeyMapping` here instead of CE's raw `Mouse.isButtonDown(0)` poll) is already documented
   in this port's own class javadoc and does not change default behavior.

2. **Only 1 of the 5 already-committed boss entities clears its boss bar on removal — the other 4 are a
   real, narrow, mechanical bug already sitting in committed Phase 4 code.** Every one of
   `EntityMaskMan`/`EntityUFO`/`EntityHunterChopper`/`EntityBOTPrimeHead`/`EntityQuackos` constructs an
   identical `private final ServerBossEvent bossEvent = new ServerBossEvent(...)` field and correctly
   overrides `startSeenByPlayer`/`stopSeenByPlayer` to add/remove players — confirmed by grep, byte-for-
   byte identical pattern across all 5 files. But only `EntityMaskMan` (lines 212-216) additionally
   overrides `remove(Entity.RemovalReason reason)` to call `this.bossEvent.removeAllPlayers()` — a
   defensive cleanup this port's own javadoc on that override correctly explains is needed because
   `ServerBossEvent` is not itself tied to the entity's lifecycle, and correctly cites it as matching
   vanilla `EnderDragon`/`WitherBoss`'s own identical override. `EntityUFO`, `EntityHunterChopper`,
   `EntityBOTPrimeHead`, and `EntityQuackos` have **no such override** (confirmed by grep — zero hits for
   `remove(` or `removeAllPlayers` in any of the 4 files), even though 3 of the 4 explicitly call
   `this.discard()` themselves on death (`EntityUFO:163`, `EntityHunterChopper:171,356`,
   `EntityQuackos:127`) and `EntityBOTPrimeHead` never calls `discard()`/overrides removal at all despite
   being the actual "win condition" entity of a 75-segment fight that is explicitly designed to end with
   this exact entity dying. This is not a hypothetical: this port's own codebase already has **6** other
   entity classes using this exact `remove(RemovalReason reason)` override for cleanup
   (`EntityFalloutRain`, `EntityBalefire`, `EntityNukeExplosionMK3`, `EntityNukeExplosionMK5`,
   `EntityTomBlast`, and `EntityMaskMan` itself) — so the override shape is well-established and low-risk
   within this port's own conventions, it was simply not applied to 4 of the 5 boss files. **Concrete,
   narrow fix**: add the same 4-line override MaskMan already has to the other 4 classes. Left unfixed,
   the practical risk is a boss bar that survives on a player's screen after the boss it belonged to is
   long gone (or a `ServerBossEvent` instance whose internal player set is never cleared), on whichever
   removal paths don't happen to route through vanilla's own per-player untracking cleanup for every
   currently-registered player — genuinely worth fixing at zero design risk, not merely a style nit.

3. **A real, verified, systemic gap in the keybind *pipeline* (not the keybind list) makes every
   fueled jetpack's thrust completely non-functional today, plus 3 more consumers, all traced to one
   missing line.** CE's server-side keybind handler, `HbmKeybindsServer.onPressedServer` (56 lines, read
   in full), does two independent things on every keybind press/release it receives: (a) calls
   `props.setKeyPressed(key, state)` on the player's capability — the *only* place CE ever writes the
   server-side "is this key currently held" state, and, for `TOGGLE_JETPACK`/`TOGGLE_HEAD`/
   `TOGGLE_MAGNET` specifically, the only place that actually flips `enableBackpack`/`enableHUD`/
   `enableMagnet` (the toggle logic itself lives inside `setKeyPressed`'s own body, gated on a
   press-edge check); then (b) dispatches to the held item's `IKeybindReceiver.handleKeybind` if
   applicable. This port's `KeybindPacket.handleServer` (`com/hbm/packet/toserver/KeybindPacket.java`,
   80 lines, read in full — a genuinely new file this port had to write, since CE's packet class had
   never actually been created anywhere in this tree before, confirmed by that file's own javadoc)
   **only does (b)**. It never calls `HbmPlayerAttachment.setKeyPressed` — confirmed by grep across the
   entire `src/main/java` tree: `setKeyPressed` is *defined* once, in `HbmPlayerAttachment.java:97`
   (whose own javadoc explicitly says it "keeps CE's original key-down-edge toggle behavior," i.e. the
   intent to call it was preserved), but is **called from nowhere in this port at all**. Since
   `getKeyPressed`/`isJetpackActive()` always read the `keysPressed[]` array's Java-default `false`, this
   silently breaks, end to end, in already-committed code:
   - **All 4 fueled jetpacks** (`JetpackRegular`, `JetpackBooster`, `JetpackBreak`, `JetpackVectorized` —
     every concrete subclass of `JetpackFueledBase`, confirmed by reading/grepping all 4) gate their
     entire `onArmorTick` thrust logic on `props.isJetpackActive()` → `enableBackpack &&
     getKeyPressed(EnumKeybind.JETPACK)`. With `getKeyPressed` always `false`, **no fueled jetpack can
     ever thrust**, regardless of fuel level or the jump key — a core survival-gameplay mechanic that
     looks fully ported (all 4 items exist, all pass Phase 3 review, all read correctly against CE) but
     is silently inert.
   - `TOGGLE_JETPACK`/`TOGGLE_HEAD`/`TOGGLE_MAGNET` can never toggle anything at all (their sole effect
     is inside the never-called `setKeyPressed`), so even a player who *could* thrust would have no way
     to turn the backpack mechanic off, and the HUD/magnet toggles are permanently stuck at their
     compiled-in default (`true` for all three, so this specific consequence is currently invisible —
     everything defaults to "on" — but the toggle itself is dead).
   - `PipeBaseBlockEntity`'s `TOOL_CTRL`-gated branch (line 212, the pipe tool's alt-mode check) can
     never fire server-side either, for the same reason.
   - The 5 `CRANE_*` keys would have the identical problem the moment their one real consumer exists
     (see Headline finding #5) — this bug isn't specific to jetpacks, it silently breaks *every* current
     and future `getKeyPressed` consumer on the server.

   **Concrete, one-line fix**, in `KeybindPacket.handleServer`, alongside the existing `IKeybindReceiver`
   dispatch: `HbmPlayerAttachment.getData(context.player()).setKeyPressed(packet.keybind(),
   packet.state());`. This is this report's single highest-value finding: a narrow, mechanical,
   zero-design-risk fix that unblocks real, already-reviewed, already-"complete" gameplay content that
   currently silently does nothing in actual play.

4. **The task's (and this port's own `docs/phase4/STATUS.md`'s) "4 boss classes" framing is off by one
   — there are 5 already-committed, already-identical `ServerBossEvent` boss classes, not 4.** The task
   names "worm, MaskMan, UFO, Hunter Chopper." `docs/phase4/entities_bosses.md` (already published)
   correctly found and documented a 5th — `EntityQuackos`, the invulnerable joke/pseudo-boss duck mount
   — and this report independently re-confirms by grep that `EntityQuackos` carries the exact same
   `ServerBossEvent bossEvent`/`startSeenByPlayer`/`stopSeenByPlayer` wiring as the other 4, already
   committed. `docs/phase4/STATUS.md` itself (line 174) also says "4 bosses," so this undercount has
   already propagated from the task prompt into this port's own status documentation — worth a small
   correction wherever that line is next touched, though it does not change any of this report's
   findings (Quackos's wiring needs the exact same Headline-finding-#2 fix as the other non-MaskMan
   bosses, and is otherwise fine).

5. **The 5 `CRANE_*` keybinds are fully registered and pipeline-ready but have zero real consumer
   anywhere in this port — not a keybind gap, a block-entity gap already named by Phase 2.** Grepping
   CE for every `EnumKeybind.CRANE_*` use finds exactly one consumer: `TileEntityRBMKCraneConsole` (740
   lines), the manual fuel-rod-loading crane console for RBMK reactors. This port registers all 5 crane
   `KeyMapping`s (`craneUpKey`/`craneDownKey`/`craneLeftKey`/`craneRightKey`/`craneLoadKey`) and routes
   them correctly through the full client-diff→packet→(once Headline finding #3 is fixed)
   server-capability pipeline — but no `RBMKCraneConsole`-equivalent block/block-entity exists anywhere
   in `src/main/java/com/hbm` under any name (confirmed by grep for `Crane` across the whole tree; the
   only crane-named class that does exist, `CraneSplitterBlockEntity`, is an unrelated item-conveyor
   splitter, not the RBMK console). This is not a new finding — `docs/phase2/rbmk_reactor.md` already
   named `TileEntityRBMKCraneConsole` as "Package B," blocked on the entirely-unported
   `com.hbm.inventory.control_panel` network (29 CE files, 5,349 lines, zero ported) — this report's only
   addition is confirming that the crane keybinds themselves are not the blocker: they are ready and
   waiting on their block, exactly like every other already-registered-but-unconsumed piece of
   infrastructure this port has shipped ahead of its consumer in prior phases.

6. **A narrower, real, previously-unflagged UX gap: dropping CE's `handleOverlap` also dropped its one
   concrete behavioral effect, not just dead reflection code.** This port's `HbmKeybinds.java` javadoc
   correctly documents dropping CE's `handleOverlap` (a Forge-1.12-internal-reflection keybind-priority
   hack with no NeoForge equivalent), and correctly notes Neo Edition's own port also carries it
   commented-out/unused (independently re-confirmed by this report's own grep of
   `upstream/neo-edition`). But `handleOverlap`'s "GUN HANDLING" branch (CE `HbmKeybinds.java:188-214`)
   did one real, intentional thing beyond overlap-priority bookkeeping: while a gun is held and any gun
   key fires, it zeroes vanilla's own attack keybind's `pressed`/`pressTime` state, explicitly commented
   "Shoot in favor of attacking." Since this port's `gunPrimaryKey` defaults to the same physical key as
   vanilla's own attack key (left mouse button, matching CE's raw `Mouse.isButtonDown(0)` default), and
   CE's own `ItemGunBaseNT.onLeftClickEntity`/`onBlockStartBreak` overrides both intentionally return
   `true` ("don't change vanilla's default behavior," confirmed by this port's own javadoc quoting that
   exact CE fact), nothing in the current pipeline suppresses vanilla's own attack/block-break input
   while a gun fires at melee range. Practical effect: holding left-click to fire a gun point-blank at an
   entity or block will also throw a normal vanilla punch/mining hit on every click, exactly the
   collision CE's dropped code existed to prevent. This is real but narrow (irrelevant at any range
   beyond ~4.5 blocks, where vanilla's own reach check finds no target) and not previously flagged by any
   sibling Phase 5 report — noted here as a small, self-contained follow-up (e.g. cancel the vanilla
   attack `InputEvent`/suppress `AttackEntityEvent` while `GUN_PRIMARY` is down and an
   `ItemGunBaseNT` is held), not something this narrow report should redesign in place.

## Answering the task's two questions directly

**(1) Is the boss-bar approach really "free" client-side, or is a client registration step missing?**
Confirmed free, with the one caveat above. The shape every boss uses —
`ServerBossEvent`/`BossEvent.BossBarColor`/`BossEvent.BossBarOverlay.PROGRESS` field,
`bossEvent.setProgress(...)` per tick, `startSeenByPlayer`/`stopSeenByPlayer` calling
`addPlayer`/`removePlayer` — is a pure server-side construct. Vanilla's client automatically renders any
boss bar it has received a `ClientboundBossEventPacket` for, via its own always-present
`BossHealthOverlay`; there is no mod-side renderer, GUI-overlay registration, or texture asset needed
for any of the 5 boss colors this port uses (`PURPLE`×3, `RED`×1, `GREEN`×1 — all standard vanilla enum
values with built-in vanilla textures) or for the single overlay style used (`PROGRESS`, also built-in).
**This is well-established, long-stable Mojang-mapping knowledge, not verified against a compiled 1.21.1
jar in this sandbox** — flagged per the task's own ground rules, exactly as `docs/phase4/
entities_bosses.md` and both sibling Phase 5 reports already flag it. What *is* newly confirmed by this
report, from real code rather than repeated assertion: three independent research passes (that report,
two sibling Phase 5 reports, and this one) plus this port's own internal precedent (the identical
`remove(RemovalReason)` cleanup pattern already used 6 times elsewhere) all converge on the same shape
with zero contradicting evidence found anywhere in either reference repo. The one real gap is not a
missing *registration* step — it's the missing *cleanup* override on 4 of the 5 boss classes (Headline
finding #2), which is a bug in already-committed code, not a hole in the design.

**(2) Does the keybind infrastructure cover every CE keybind, and are there CE keybinds with no
1.21.1 equivalent?** No CE keybind is missing — the list is already 100% complete and correct (Headline
finding #1), and none of the task's own three speculative examples exist in CE to be missing in the
first place. The real gap this audit found is not in the keybind *list* but in the *pipeline* those
keybinds ride on: the server-side half of the press/release sync silently drops the one call that makes
`getKeyPressed`/`isJetpackActive()` mean anything (Headline finding #3), and 5 of the 18 registered
keybinds have no live consumer yet because their sole real consumer block was never ported (Headline
finding #5, already known from Phase 2).

## Phase-5-safe scope (buildable now, no blockers)

All of the following are self-contained, single-file-or-few-line fixes inside this port's own already-
committed code, need no new dependency, and are not blocked on any other phase or area:

- **Add `remove(Entity.RemovalReason reason)` overrides to `EntityUFO`, `EntityHunterChopper`,
  `EntityBOTPrimeHead`, `EntityQuackos`**, each calling `this.bossEvent.removeAllPlayers()` after
  `super.remove(reason)`, matching `EntityMaskMan`'s existing override verbatim (Headline finding #2).
  4 files, ~4 lines each, zero design risk, zero new imports (all 4 already import `ServerBossEvent`).
- **Add the missing `HbmPlayerAttachment.getData(context.player()).setKeyPressed(packet.keybind(),
  packet.state())` call to `KeybindPacket.handleServer`** (Headline finding #3). 1 file, 1 line, unblocks
  4 already-committed jetpack items plus `PipeBaseBlockEntity`'s `TOOL_CTRL` branch with no other code
  change required anywhere.
- **Correct "4 bosses" to "5 bosses" in `docs/phase4/STATUS.md`** (Headline finding #4) — a one-line doc
  fix, not a code change, whenever that file is next touched.
- Both fixes above are independent of every other Phase 5 area (renderer framework, HUD overlays,
  particles, GUI, JEI, sound, lang) and independent of each other — either can land alone.

## Deferred / blocked scope (named blocker, not this report's job)

- **`TileEntityRBMKCraneConsole` and its 5 `CRANE_*` keybind consumers** — blocked on
  `com.hbm.inventory.control_panel` (29 CE files, 5,349 lines, zero ported), already named as "Package B"
  by `docs/phase2/rbmk_reactor.md`. Not a Phase 5 blocker or task; the keybinds themselves need no
  further Phase 5 work (Headline finding #5).
- **`handleOverlap`'s dropped gun/vanilla-attack-key suppression** (Headline finding #6) — a real, small,
  self-contained gap, but a design decision (how to suppress vanilla attack input while a gun is
  active in 1.21.1's input model) that this narrow report should flag, not resolve. Owner: whichever
  Phase 5 area finishes gun input handling, or a follow-up on this same file.
- **CE's `TOOL_ALT`-driven display-info paging** — already deliberately simplified away by
  `ItemSettingsTool`'s own Phase-2-era javadoc (a documented, accepted scope reduction, not a bug); not
  re-opened here.

## Key design/API decisions

- **`ServerBossEvent`/`BossEvent.BossBarColor`/`BossEvent.BossBarOverlay`/`Entity#startSeenByPlayer`/
  `Entity#stopSeenByPlayer`/`Entity#remove(Entity.RemovalReason)` are all treated as confirmed-real
  1.21.1 API by this report**, on the same "well-established Mojang-mapping knowledge, cross-checked
  against every available reference, not jar-verified" basis as `docs/phase4/entities_bosses.md`
  originally flagged them — strengthened here by this port's own 6 independent uses of the
  `remove(RemovalReason)` shape and by finding zero contradicting evidence anywhere in
  `upstream/neo-edition` (which has none of this code to contradict it, confirmed by grep) or CE.
- **`KeybindPacket`'s existing dispatch-to-`IKeybindReceiver` logic needs no change** — only an addition
  alongside it. This preserves the file's own stated intent (fixing the "packet class never existed"
  bug) without touching the part that already works.
- **No new `EnumKeybind` value, `KeyMapping`, or registration call is needed anywhere** — this report's
  entire keybind-side scope is a one-line pipeline fix, not new infrastructure.

## Open questions / risks

- **Whether every removal path for `EntityHunterChopper`'s two-stage crash-then-explode death (line
  171's mid-air `discard()` and line 356's final-impact `discard()`) and every other boss's removal path
  reliably fires `stopSeenByPlayer` for *every* currently-tracking player through vanilla's own
  chunk-tracking cleanup, independent of the `remove(RemovalReason)` override, was not verified against
  a running server in this sandbox** (no client/server launch is possible here). The recommended fix
  (Headline finding #2) is the same defensive belt-and-suspenders vanilla itself uses for exactly this
  uncertainty, so it is correct to add regardless of the answer — flagged as the one piece of this
  report's boss-bar claim that a real playtest, not further static reading, should confirm.
- **Whether any other `getKeyPressed` consumer besides the 4 named in Headline finding #3 exists
  somewhere this report's greps missed** — the audit was a full-tree grep for `EnumKeybind\.` and
  `getKeyPressed`/`setKeyPressed`, which should be exhaustive, but this report did not independently
  re-derive every consumer's *correctness* beyond confirming the dependency chain to `setKeyPressed`;
  worth a second look once the one-line fix lands and jetpacks can actually be tested in play.
- **The exact 1.21.1 replacement for suppressing vanilla's attack input while a custom item is "in use"**
  (Headline finding #6) was not researched here beyond confirming the gap exists — this needs its own
  small design pass (candidates include cancelling the relevant `InputEvent` client-side while
  `GUN_PRIMARY` is down and a gun is held, or handling it entity-side via `AttackEntityEvent`/
  `PlayerInteractEvent` cancellation) rather than a guess in this narrow report.
